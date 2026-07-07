import { createPetRuntime } from "./core/petRuntime.js";
import { createPetEventBridge } from "./core/petEventBridge.js";
import { mfSproutTheme } from "./themes/mfSproutTheme.js";
import {
    DEFAULT_PET_OPTIONS,
    PET_ACTIONS,
    PET_LINKS,
    PET_TEXT,
} from "./petManifest.js";

const HUD_EXPANDED_SESSION_LIMIT = 3;

function createElement(tag, className, attributes = {}) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    Object.entries(attributes).forEach(([key, value]) => {
        if (value === undefined || value === null) return;
        element.setAttribute(key, value);
    });
    return element;
}

function navigateTo(link) {
    if (!link || link === "#") return;
    window.location.href = link;
}

function normalizeLang(lang) {
    return lang === "en" ? "en" : "zh";
}

function resolveText(lang) {
    return PET_TEXT[normalizeLang(lang)] || PET_TEXT.zh;
}

function formatTokenCount(value) {
    if (!Number.isFinite(value)) return "";
    if (value >= 1000000) return `${(value / 1000000).toFixed(1)}m`;
    if (value >= 1000) return `${(value / 1000).toFixed(1)}k`;
    return String(value);
}

function formatElapsed(ms) {
    const seconds = Math.max(0, Math.floor(Number(ms) / 1000));
    if (!Number.isFinite(seconds) || seconds < 5) return "just now";
    if (seconds < 60) return `${seconds}s ago`;
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    return `${Math.floor(hours / 24)}d ago`;
}

function formatContextUsage(contextUsage, text, compact = true) {
    if (!contextUsage) return null;
    const used = Number(contextUsage.used);
    const limit = Number(contextUsage.limit);
    const percent = Number(contextUsage.percent);
    if (!Number.isFinite(used)) return null;
    if (compact && Number.isFinite(percent)) return `${Math.round(percent)}%`;
    if (Number.isFinite(limit) && limit > 0) {
        const resolvedPercent = Number.isFinite(percent) ? Math.round(percent) : Math.round((used / limit) * 100);
        return `${formatTokenCount(used)} / ${formatTokenCount(limit)} (${resolvedPercent}%)`;
    }
    return text.contextUsage?.tokens
        ? text.contextUsage.tokens.replace("{used}", formatTokenCount(used))
        : `${formatTokenCount(used)} tokens`;
}

function formatResetTime(resetAt) {
    const value = Number(resetAt);
    if (!Number.isFinite(value)) return "";
    const diff = value - Date.now();
    if (diff <= 0) return "";
    const minutes = Math.ceil(diff / 60000);
    if (minutes < 60) return `${minutes}m`;
    const hours = Math.floor(minutes / 60);
    const rest = minutes % 60;
    return rest ? `${hours}h ${rest}m` : `${hours}h`;
}

function formatQuotaUsage(quotaUsage, text) {
    if (!quotaUsage) return null;
    const pieces = [];
    if (Number.isFinite(Number(quotaUsage.usedPercent))) pieces.push(`${Number(quotaUsage.usedPercent)}%`);
    if (Number.isFinite(Number(quotaUsage.remaining))) pieces.push(text.quotaUsage.remaining.replace("{remaining}", formatTokenCount(Number(quotaUsage.remaining))));
    const reset = formatResetTime(quotaUsage.resetAt);
    if (reset) pieces.push(text.quotaUsage.resetIn.replace("{time}", reset));
    return pieces.length ? pieces.join(" · ") : null;
}

function createQuotaUsageNode(quotaUsage, text) {
    const label = formatQuotaUsage(quotaUsage, text);
    if (!label) return null;
    const percent = Number(quotaUsage.usedPercent);
    const item = createElement("div", "mf-pet-dashboard-session__quota");
    const header = createElement("div", "mf-pet-dashboard-session__quota-header");
    const name = createElement("span", "mf-pet-dashboard-session__quota-name");
    const value = createElement("span", "mf-pet-dashboard-session__quota-value");
    const track = createElement("div", "mf-pet-dashboard-session__quota-track");
    const fill = createElement("div", "mf-pet-dashboard-session__quota-fill");
    name.textContent = quotaUsage.label || text.quotaUsage.label;
    value.textContent = label;
    fill.style.width = `${Number.isFinite(percent) ? Math.max(0, Math.min(100, percent)) : 0}%`;
    fill.dataset.warning = String(Number.isFinite(percent) && percent >= 90);
    header.append(name, value);
    track.append(fill);
    item.append(header, track);
    return item;
}

function createSessionTimingNode(session, text) {
    const updatedAt = Number(session.updatedAt);
    const createdAt = Number(session.createdAt);
    const pieces = [];
    if (Number.isFinite(updatedAt)) {
        pieces.push(`${text.dashboard.updated || "Updated"} ${formatElapsed(Date.now() - updatedAt)}`);
    }
    if (Number.isFinite(createdAt)) {
        pieces.push(`${text.dashboard.started || "Started"} ${formatElapsed(Date.now() - createdAt)}`);
    }
    if (!pieces.length) return null;
    const node = createElement("div", "mf-pet-dashboard-session__timing");
    node.textContent = pieces.join(" / ");
    return node;
}

function createContextUsageNode(contextUsage, text) {
    const label = formatContextUsage(contextUsage, text, true);
    if (!label) return null;
    const percent = Number(contextUsage.percent);
    const tier = Number.isFinite(percent) && percent >= 90 ? "hot" : Number.isFinite(percent) && percent >= 75 ? "warm" : "normal";
    const node = createElement("span", `mf-pet-session__usage mf-pet-session__usage--${tier}`);
    node.textContent = label;
    node.title = formatContextUsage(contextUsage, text, false) || label;
    return node;
}

function summarizePath(path) {
    if (!path || typeof path !== "string") return "";
    const parts = path.split(/[\\/]+/).filter(Boolean);
    if (!parts.length) return path;
    return parts.slice(-2).join("/");
}

function sessionDisplayTitle(session) {
    return session?.displayTitle || session?.title || session?.sessionTitle || session?.id || "";
}

function agentFallbackText(session) {
    const source = session?.agentLabel || session?.agentId || session?.provider || "MF";
    const words = source.split(/[\s_-]+/).filter(Boolean);
    if (words.length > 1) return words.slice(0, 2).map((word) => word[0]).join("").toUpperCase();
    return source.slice(0, 2).toUpperCase();
}

function createAgentIdentityNode(session) {
    const node = createElement("div", "mf-pet-dashboard-session__agent");
    const label = session.agentLabel || session.agentId || session.provider || "MF_Pet";
    node.title = label;
    node.textContent = agentFallbackText(session);
    if (session.agentColor) node.style.setProperty("--mf-pet-agent-color", session.agentColor);
    if (session.agentIcon) {
        const icon = createElement("img", "mf-pet-dashboard-session__agent-icon", {
            src: session.agentIcon,
            alt: "",
            loading: "lazy",
        });
        icon.addEventListener("error", () => icon.remove(), { once: true });
        node.prepend(icon);
    }
    return node;
}

function dashboardFilterMatches(session, filter) {
    if (filter === "running") return session.badge === "running";
    if (filter === "waiting") return session.badge === "waiting" || session.badge === "blocked";
    if (filter === "done") return session.badge === "done";
    return true;
}

function createChatMessage(message) {
    const item = createElement("div", `mf-pet-chat__message mf-pet-chat__message--${message.role}`);
    const bubble = createElement("div", "mf-pet-chat__bubble");
    bubble.textContent = message.text;
    bubble.toggleAttribute("data-streaming", !!message.streaming);
    item.dataset.messageId = message.id;
    item.append(bubble);
    return item;
}

function createNotificationNode(notification, runtime) {
    const item = createElement("button", `mf-pet-notice mf-pet-notice--${notification.type}`, {
        type: "button",
        "data-notification-id": notification.id,
    });
    const title = createElement("span", "mf-pet-notice__title");
    const text = createElement("span", "mf-pet-notice__text");
    title.textContent = notification.title || notification.event || "MF_Pet";
    text.textContent = notification.text || notification.sessionId || notification.event || "";
    item.append(title, text);
    item.addEventListener("click", () => runtime.dismissNotification(notification.id));
    return item;
}

function createPermissionNode(request, runtime) {
    const item = createElement("div", `mf-pet-permission mf-pet-permission--${request.status}`, {
        "data-permission-id": request.id,
    });
    const header = createElement("div", "mf-pet-permission__header");
    const title = createElement("div", "mf-pet-permission__title");
    const close = createElement("button", "mf-pet-permission__close", {
        type: "button",
        "aria-label": "关闭权限气泡",
    });
    const text = createElement("div", "mf-pet-permission__text");
    const meta = createElement("div", "mf-pet-permission__meta");
    const actions = createElement("div", "mf-pet-permission__actions");
    const approve = createElement("button", "mf-pet-permission__button", { type: "button" });
    const deny = createElement("button", "mf-pet-permission__button mf-pet-permission__button--deny", { type: "button" });
    title.textContent = request.title || "权限确认";
    close.textContent = "×";
    close.addEventListener("click", (event) => {
        event.stopPropagation();
        runtime.dismissPermission(request.id);
    });
    text.textContent = request.text || request.event || "";
    [
        request.toolDisplayName || request.toolName ? `Tool: ${request.toolDisplayName || request.toolName}` : "",
        request.toolInputDescription || "",
        request.autoCloseMs ? `Bubble auto-hide: ${Math.round(request.autoCloseMs / 1000)}s` : "",
    ].filter(Boolean).forEach((line) => {
        const row = createElement("div", "mf-pet-permission__meta-row");
        row.textContent = line;
        meta.append(row);
    });
    (request.suggestions || []).forEach((suggestion) => {
        const button = createElement("button", "mf-pet-permission__button mf-pet-permission__button--suggestion", { type: "button" });
        button.textContent = suggestion.label || suggestion.behavior || suggestion.id;
        button.title = suggestion.description || "";
        if (suggestion.type) button.dataset.suggestionType = suggestion.type;
        button.addEventListener("click", () => runtime.resolvePermission(request.id, suggestion.id || suggestion.behavior));
        actions.append(button);
    });
    approve.textContent = "批准";
    deny.textContent = "拒绝";
    approve.addEventListener("click", () => runtime.resolvePermission(request.id, "approved"));
    deny.addEventListener("click", () => runtime.resolvePermission(request.id, "denied"));
    actions.append(approve, deny);
    header.append(title, close);
    item.append(header, text);
    if (meta.childElementCount) item.append(meta);
    item.append(actions);
    return item;
}

function createSessionNode(session, text) {
    const item = createElement("div", `mf-pet-session mf-pet-session--${session.state} mf-pet-session--badge-${session.badge || "idle"}`, {
        "data-session-id": session.id,
    });
    const dot = createElement("span", "mf-pet-session__dot");
    const app = createElement("span", "mf-pet-session__app");
    const label = createElement("span", "mf-pet-session__label");
    const state = createElement("span", "mf-pet-session__state");
    const elapsed = createElement("span", "mf-pet-session__elapsed");
    const usage = createContextUsageNode(session.contextUsage, text);
    const unread = createElement("span", "mf-pet-session__unread");
    const focus = createElement("span", "mf-pet-session__focus");
    const pin = createElement("span", "mf-pet-session__pin");
    const hasUnreadCompletion = session.badge === "done" && (session.acknowledgedAt || 0) < (session.updatedAt || 0);
    const canFocus = session.canFocus !== false;
    if (!canFocus) item.classList.add("mf-pet-session--unfocusable");
    app.textContent = agentFallbackText(session);
    app.title = session.agentLabel || session.agentId || session.provider || "";
    if (session.agentColor) app.style.setProperty("--mf-pet-agent-color", session.agentColor);
    label.textContent = sessionDisplayTitle(session);
    state.textContent = text.stateLabels[session.state] || session.state;
    elapsed.textContent = formatElapsed(Date.now() - (session.updatedAt || session.createdAt || Date.now()));
    elapsed.title = text.sessionHud?.elapsed || "Updated";
    unread.textContent = "!";
    unread.title = text.sessionHud?.completionUnread || "Completed";
    unread.hidden = !hasUnreadCompletion;
    focus.textContent = "×";
    focus.title = text.sessionHud?.focusUnavailable || "Focus unavailable";
    focus.hidden = canFocus;
    pin.textContent = "⌖";
    pin.title = text.menu?.sessionHudPin || "Pin session HUD";
    item.append(dot, app, label, state, elapsed);
    if (usage) item.append(usage);
    item.append(unread);
    item.append(focus);
    item.append(pin);
    return item;
}

function createFoldedSessionNode(count, text) {
    const item = createElement("button", "mf-pet-session mf-pet-session--folded", {
        type: "button",
        "data-session-folded": "true",
    });
    const dot = createElement("span", "mf-pet-session__dot");
    const label = createElement("span", "mf-pet-session__label");
    const state = createElement("span", "mf-pet-session__state");
    label.textContent = (text.sessionHud?.otherActive || "{count} other active").replace("{count}", count);
    state.textContent = text.dashboard?.title || "Dashboard";
    item.append(dot, label, state);
    return item;
}

function createSessionDetailNode(session, text) {
    const item = createElement("div", "mf-pet-session-detail");
    const title = createElement("div", "mf-pet-session-detail__title");
    const state = createElement("div", "mf-pet-session-detail__state");
    const events = createElement("div", "mf-pet-session-detail__events");
    title.textContent = sessionDisplayTitle(session);
    state.textContent = `${text.sessionBadges?.[session.badge] || session.badge || "idle"} · ${text.stateLabels[session.state] || session.state}`;
    item.append(title, state);
    const contextUsage = formatContextUsage(session.contextUsage, text, false);
    if (contextUsage) {
        const usage = createElement("div", "mf-pet-session-detail__usage");
        usage.textContent = `${text.contextUsage?.label || "Context"}: ${contextUsage}`;
        item.append(usage);
    }
    (session.recentEvents || []).slice(-4).reverse().forEach((event) => {
        const row = createElement("div", "mf-pet-session-detail__event");
        row.textContent = `${event.event || "event"} -> ${text.stateLabels[event.state] || event.state}`;
        events.append(row);
    });
    item.append(events);
    return item;
}

function createDashboardSessionNode(session, text, runtime, api, config) {
    const item = createElement("article", `mf-pet-dashboard-session mf-pet-dashboard-session--badge-${session.badge || "idle"}`, {
        "data-dashboard-session-id": session.id,
    });
    const header = createElement("div", "mf-pet-dashboard-session__header");
    const agent = createAgentIdentityNode(session);
    const titleWrap = createElement("div", "mf-pet-dashboard-session__title-wrap");
    const title = createElement("input", "mf-pet-dashboard-session__title", {
        type: "text",
    });
    const badge = createElement("div", "mf-pet-dashboard-session__badge");
    const meta = createElement("div", "mf-pet-dashboard-session__meta");
    const quota = createQuotaUsageNode(session.quotaUsage, text);
    const timing = createSessionTimingNode(session, text);
    const source = createElement("div", "mf-pet-dashboard-session__source");
    const events = createElement("div", "mf-pet-dashboard-session__events");
    const actions = createElement("div", "mf-pet-dashboard-session__actions");
    const focus = createElement("button", "mf-pet-dashboard-session__button", { type: "button" });
    const acknowledge = createElement("button", "mf-pet-dashboard-session__button", { type: "button" });
    const hide = createElement("button", "mf-pet-dashboard-session__button", { type: "button" });
    const end = createElement("button", "mf-pet-dashboard-session__end", { type: "button" });
    title.value = sessionDisplayTitle(session);
    title.setAttribute("aria-label", text.dashboard.renameSession);
    title.addEventListener("keydown", (event) => {
        if (event.key === "Enter") title.blur();
        if (event.key === "Escape") {
            title.value = sessionDisplayTitle(session);
            title.blur();
        }
    });
    title.addEventListener("blur", async () => {
        const renamed = runtime.renameSession(session.id, title.value);
        await config.onSessionRename?.({
            session: renamed || session,
            title: title.value.trim(),
            runtime,
            api,
        });
    });
    badge.textContent = text.sessionBadges?.[session.badge] || session.badge || "idle";
    meta.textContent = [
        text.stateLabels[session.state] || session.state,
        formatContextUsage(session.contextUsage, text, false),
    ].filter(Boolean).join(" · ");
    [
        [text.dashboard.agent, session.agentLabel || session.agentId],
        [text.dashboard.model, [session.provider, session.model].filter(Boolean).join(" / ")],
        [text.dashboard.cwd, summarizePath(session.cwd)],
        [text.dashboard.host, session.host],
        [text.dashboard.platform, session.platform],
        [text.dashboard.editor, session.editor],
        [text.dashboard.hook, [session.hookSource, session.sourcePid ? `pid ${session.sourcePid}` : ""].filter(Boolean).join(" · ")],
        [text.dashboard.focusResult, session.focusHandoff?.status ? [session.focusHandoff.status, session.focusHandoff.reason].filter(Boolean).join(" · ") : ""],
        [text.dashboard.headless, session.headless ? text.dashboard.yes : ""],
    ].filter(([, value]) => value).forEach(([labelText, value]) => {
        const row = createElement("div", "mf-pet-dashboard-session__source-row");
        const label = createElement("span", "mf-pet-dashboard-session__source-label");
        const body = createElement("span", "mf-pet-dashboard-session__source-value");
        label.textContent = labelText;
        body.textContent = value;
        if (labelText === text.dashboard.cwd && session.cwd) body.title = session.cwd;
        row.append(label, body);
        source.append(row);
    });
    const dashboardEvents = session.lastEvent
        ? [session.lastEvent]
        : (session.recentEvents || []).slice(-1).reverse();
    dashboardEvents.forEach((event) => {
        const row = createElement("div", "mf-pet-dashboard-session__event");
        const eventAt = Number(event.at);
        const age = Number.isFinite(eventAt) ? ` / ${formatElapsed(Date.now() - eventAt)}` : "";
        row.textContent = `${event.event || "event"} -> ${text.stateLabels[event.state] || event.state}${age}`;
        events.append(row);
    });
    if (!events.childElementCount) {
        const empty = createElement("div", "mf-pet-dashboard-session__event");
        empty.textContent = text.dashboard.noEvents;
        events.append(empty);
    }
    focus.textContent = text.dashboard.focusSession;
    focus.disabled = session.canFocus === false;
    focus.addEventListener("click", async () => {
        const focused = runtime.focusSession(session.id, { source: "dashboard" });
        runtime.acknowledgeSession(session.id);
        try {
            const result = await config.onSessionFocus?.({
                session: focused || session,
                runtime,
                api,
                source: "dashboard",
            });
            runtime.completeSessionFocus(session.id, result || { status: "success" });
        } catch (error) {
            runtime.completeSessionFocus(session.id, {
                status: "failed",
                reason: error?.message || "focus-callback-failed",
            });
        }
    });
    acknowledge.textContent = text.dashboard.acknowledgeSession;
    acknowledge.disabled = session.badge === "idle";
    acknowledge.toggleAttribute("data-attention", !!session.requiresCompletionAck);
    acknowledge.addEventListener("click", () => runtime.acknowledgeSession(session.id));
    hide.textContent = text.dashboard.hideSession;
    hide.addEventListener("click", () => runtime.hideSession(session.id));
    end.textContent = text.dashboard.endSession;
    end.addEventListener("click", () => runtime.endSession(session.id));
    actions.append(focus, acknowledge, hide, end);
    titleWrap.append(title);
    header.append(agent, titleWrap, badge);
    item.append(header, meta);
    if (timing) item.append(timing);
    if (quota) item.append(quota);
    if (source.childElementCount) item.append(source);
    item.append(events, actions);
    return item;
}

function createMenuButton(label, action) {
    const button = createElement("button", "mf-pet-menu__item", { type: "button" });
    button.textContent = label;
    button.addEventListener("click", action);
    return button;
}

function createMenuDivider() {
    return createElement("div", "mf-pet-menu__divider", { role: "separator" });
}

function isSvgAsset(src) {
    return /\.svg(?:[?#].*)?$/i.test(String(src || ""));
}

function setSvgTrackingTransform(svgObject, theme, dx, dy) {
    if (!svgObject || !svgObject.contentDocument || !theme.eyeTracking?.enabled) return;
    const ids = theme.eyeTracking.ids || {};
    const eyes = svgObject.contentDocument.getElementById(ids.eyes || "eyes-js");
    const body = svgObject.contentDocument.getElementById(ids.body || "body-js");
    const shadow = svgObject.contentDocument.getElementById(ids.shadow || "shadow-js");
    const bodyScale = Number(theme.eyeTracking.bodyScale) || 0.33;
    const shadowShift = Number(theme.eyeTracking.shadowShift) || 0.3;
    const shadowStretch = Number(theme.eyeTracking.shadowStretch) || 0.15;
    if (eyes) eyes.setAttribute("transform", `translate(${dx} ${dy})`);
    if (body) body.setAttribute("transform", `translate(${(dx * bodyScale).toFixed(2)} ${(dy * bodyScale).toFixed(2)})`);
    if (shadow) {
        const stretch = 1 + Math.min(0.24, Math.abs(dx) * shadowStretch);
        shadow.setAttribute("transform", `translate(${(dx * shadowShift).toFixed(2)} 0) scale(${stretch.toFixed(3)} 1)`);
    }
}

export function initForestPet(options = {}) {
    const config = {
        ...DEFAULT_PET_OPTIONS,
        ...options,
        theme: options.theme || mfSproutTheme,
        links: {
            ...PET_LINKS,
            ...(options.links || {}),
        },
    };
    const mount = config.mount || document.body;
    const runtime = createPetRuntime(config);
    const bridge = createPetEventBridge(runtime, config.bridgeOptions);

    const root = createElement("div", "mf-pet");
    const notices = createElement("div", "mf-pet-notices", {
        "aria-live": "polite",
    });
    const permissions = createElement("div", "mf-pet-permissions", {
        "aria-live": "polite",
    });
    const sessionHud = createElement("div", "mf-pet-session-hud");
    const sessionDetail = createElement("div", "mf-pet-session-detail-wrap");
    const dashboard = createElement("section", "mf-pet-dashboard", {
        "aria-live": "polite",
    });
    const dashboardHeader = createElement("div", "mf-pet-dashboard__header");
    const dashboardTitle = createElement("div", "mf-pet-dashboard__title");
    const dashboardClose = createElement("button", "mf-pet-dashboard__close", {
        type: "button",
    });
    const dashboardFilters = createElement("div", "mf-pet-dashboard__filters");
    const dashboardBody = createElement("div", "mf-pet-dashboard__body");
    const contextMenu = createElement("div", "mf-pet-menu", {
        role: "menu",
    });
    contextMenu.hidden = true;
    const panel = createElement("section", "mf-pet__panel", {
        "aria-live": "polite",
    });
    const toggle = createElement("button", "mf-pet__sprite-button", {
        type: "button",
    });
    const sprite = createElement("img", "mf-pet__sprite", {
        alt: "",
        draggable: "false",
    });
    const spriteObject = createElement("object", "mf-pet__sprite mf-pet__sprite-object", {
        type: "image/svg+xml",
        "aria-label": "",
        tabindex: "-1",
    });
    const close = createElement("button", "mf-pet__close", {
        type: "button",
    });
    const panelText = createElement("div", "mf-pet__panel-text");
    const title = createElement("p", "mf-pet__title");
    const subtitle = createElement("p", "mf-pet__subtitle");
    const actions = createElement("div", "mf-pet__actions");
    const chat = createElement("div", "mf-pet-chat");
    const chatHeader = createElement("div", "mf-pet-chat__header");
    const chatTitle = createElement("span", "mf-pet-chat__title");
    const chatClose = createElement("button", "mf-pet-chat__close", {
        type: "button",
    });
    const chatMessages = createElement("div", "mf-pet-chat__messages");
    const chatComposer = createElement("form", "mf-pet-chat__composer");
    const chatInput = createElement("input", "mf-pet-chat__input", {
        type: "text",
        autocomplete: "off",
    });
    const chatSend = createElement("button", "mf-pet-chat__send", {
        type: "submit",
    });

    let dragging = false;
    let dragStart = null;
    let clickTimes = [];
    let suppressNextClick = false;
    let sessionHudHideTimer = null;
    let dashboardFilter = "all";
    let nativeDragStart = null;
    let nativeDragging = false;
    let currentEyeMove = { dx: 0, dy: 0 };

    function applyEyeMove(dx, dy) {
        currentEyeMove = { dx, dy };
        setSvgTrackingTransform(spriteObject, runtime.getSnapshot().theme, dx, dy);
    }

    function handleShellCursor(payload) {
        const snapshot = runtime.getSnapshot();
        const eyeTracking = snapshot.theme.eyeTracking;
        if (!eyeTracking?.enabled || !eyeTracking.states?.includes(snapshot.state)) {
            applyEyeMove(0, 0);
            return;
        }
        const point = payload?.point;
        const bounds = payload?.windowBounds;
        if (!point || !bounds) return;
        const rect = toggle.getBoundingClientRect();
        const eyeScreenX = bounds.x + rect.left + rect.width * (eyeTracking.eyeRatioX ?? 0.5);
        const eyeScreenY = bounds.y + rect.top + rect.height * (eyeTracking.eyeRatioY ?? 0.5);
        const relX = point.x - eyeScreenX;
        const relY = point.y - eyeScreenY;
        const distance = Math.sqrt(relX * relX + relY * relY);
        let dx = 0;
        let dy = 0;
        if (distance > 1) {
            const maxOffset = Number(eyeTracking.maxOffset) || 3;
            const scale = Math.min(1, distance / 300);
            dx = (relX / distance) * maxOffset * scale;
            dy = (relY / distance) * maxOffset * scale;
            dx = Math.max(-maxOffset * 0.85, Math.min(maxOffset * 0.85, Math.round(dx * 2) / 2));
            dy = Math.max(-maxOffset * 0.5, Math.min(maxOffset * 0.5, Math.round(dy * 2) / 2));
        }
        applyEyeMove(dx, dy);
    }

    spriteObject.addEventListener("load", () => {
        applyEyeMove(currentEyeMove.dx, currentEyeMove.dy);
    });

    close.textContent = "×";
    dashboardClose.textContent = "×";
    chatClose.textContent = "×";
    chatSend.textContent = "发送";
    spriteObject.hidden = true;
    toggle.append(sprite, spriteObject);
    panelText.append(title, subtitle);
    chatHeader.append(chatTitle, chatClose);
    chatComposer.append(chatInput, chatSend);
    chat.append(chatHeader, chatMessages, chatComposer);
    dashboardHeader.append(dashboardTitle, dashboardClose);
    dashboard.append(dashboardHeader, dashboardFilters, dashboardBody);
    panel.append(close, panelText, actions, chat);
    root.append(notices, permissions, sessionHud, sessionDetail, dashboard, panel, contextMenu, toggle);
    mount.append(root);

    const actionButtons = new Map();
    let selectedSessionId = null;
    let contextMenuOpen = false;
    PET_ACTIONS.forEach((action) => {
        const button = createElement("button", "mf-pet__action", {
            type: "button",
            "data-pet-action": action,
        });
        button.addEventListener("click", async (event) => {
            const payload = {
                action,
                link: config.links[action],
                runtime,
                api,
                event,
            };

            if (action === "ai") {
                api.openChat();
            } else if (action === "encyclopedia") {
                runtime.emit("panel-success");
            }

            const handled = await config.onAction?.(payload);
            if (handled === true) return;

            if (action === "ai") {
                const askResult = await config.onAskAi?.(payload);
                if (typeof askResult === "string") api.streamAssistantMessage(askResult);
                return;
            }

            navigateTo(config.links[action]);
        });
        actionButtons.set(action, button);
        actions.append(button);
    });

    function renderText(snapshot) {
        const text = resolveText(snapshot.lang);
        title.textContent = text.title;
        subtitle.textContent = text.subtitle;
        chatTitle.textContent = text.chat.title;
        dashboardTitle.textContent = text.dashboard.title;
        chatInput.placeholder = text.chat.placeholder;
        chatSend.textContent = text.chat.send;
        toggle.setAttribute("aria-label", text.toggleLabel);
        close.setAttribute("aria-label", text.closeLabel);
        dashboardClose.setAttribute("aria-label", text.dashboard.closeLabel);
        chatClose.setAttribute("aria-label", text.chat.closeLabel);
        PET_ACTIONS.forEach((action) => {
            actionButtons.get(action).textContent = text.actions[action];
        });
    }

    function renderChat(snapshot) {
        chat.hidden = !snapshot.chatOpen;
        const existing = new Map(
            [...chatMessages.querySelectorAll("[data-message-id]")].map((node) => [node.dataset.messageId, node]),
        );
        snapshot.chatMessages.forEach((message) => {
            const current = existing.get(message.id);
            if (current) {
                const bubble = current.querySelector(".mf-pet-chat__bubble");
                bubble.textContent = message.text;
                bubble.toggleAttribute("data-streaming", !!message.streaming);
                existing.delete(message.id);
                return;
            }
            chatMessages.append(createChatMessage(message));
        });
        existing.forEach((node) => node.remove());
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    function renderNotifications(snapshot) {
        const existing = new Map(
            [...notices.querySelectorAll("[data-notification-id]")].map((node) => [node.dataset.notificationId, node]),
        );
        snapshot.notifications.forEach((notification) => {
            if (existing.has(notification.id)) {
                existing.delete(notification.id);
                return;
            }
            notices.append(createNotificationNode(notification, runtime));
        });
        existing.forEach((node) => node.remove());
        notices.hidden = snapshot.notifications.length === 0 || snapshot.expanded;
    }

    function renderPermissions(snapshot) {
        const visibleRequests = snapshot.permissionRequests.filter((request) => request.bubbleVisible !== false);
        const existing = new Map(
            [...permissions.querySelectorAll("[data-permission-id]")].map((node) => [node.dataset.permissionId, node]),
        );
        visibleRequests.forEach((request) => {
            if (existing.has(request.id)) {
                existing.delete(request.id);
                return;
            }
            permissions.append(createPermissionNode(request, runtime));
        });
        existing.forEach((node) => node.remove());
        permissions.hidden = visibleRequests.length === 0 || snapshot.expanded;
    }

    function handleSessionHudClick(sessionId) {
        const session = runtime.getSnapshot().sessions.find((item) => item.id === sessionId);
        if (!session) return;
        if (session.badge === "done") runtime.acknowledgeSession(session.id);
        if (session.canFocus !== false) {
            api.focusSession(session.id);
            return;
        }
        selectedSessionId = selectedSessionId === session.id ? null : session.id;
        renderSessionDetail(runtime.getSnapshot());
    }

    function renderSessionHud(snapshot) {
        const text = resolveText(snapshot.lang);
        const visibleSessions = snapshot.sessions.filter((session) => !session.headless && !session.hiddenFromDashboard && session.state !== "idle");
        sessionHud.dataset.pinned = String(snapshot.sessionHudPinned);
        const existing = new Map(
            [...sessionHud.querySelectorAll("[data-session-id]")].map((node) => [node.dataset.sessionId, node]),
        );
        [...sessionHud.querySelectorAll(".mf-pet-session__pin")].forEach((pin) => {
            pin.title = snapshot.sessionHudPinned ? text.menu.sessionHudUnpin : text.menu.sessionHudPin;
        });
        sessionHud.querySelector("[data-session-folded]")?.remove();
        const expandedSessions = visibleSessions.slice(0, HUD_EXPANDED_SESSION_LIMIT);
        const foldedSessions = visibleSessions.slice(HUD_EXPANDED_SESSION_LIMIT);
        expandedSessions.forEach((session) => {
            if (existing.has(session.id)) {
                const current = existing.get(session.id);
                current.className = `mf-pet-session mf-pet-session--${session.state} mf-pet-session--badge-${session.badge || "idle"}`;
                current.classList.toggle("mf-pet-session--unfocusable", session.canFocus === false);
                const app = current.querySelector(".mf-pet-session__app");
                if (app) {
                    app.textContent = agentFallbackText(session);
                    app.title = session.agentLabel || session.agentId || session.provider || "";
                    if (session.agentColor) app.style.setProperty("--mf-pet-agent-color", session.agentColor);
                }
                current.querySelector(".mf-pet-session__label").textContent = sessionDisplayTitle(session);
                current.querySelector(".mf-pet-session__state").textContent = text.stateLabels[session.state] || session.state;
                const elapsed = current.querySelector(".mf-pet-session__elapsed");
                if (elapsed) {
                    elapsed.textContent = formatElapsed(Date.now() - (session.updatedAt || session.createdAt || Date.now()));
                    elapsed.title = text.sessionHud?.elapsed || "Updated";
                }
                const unread = current.querySelector(".mf-pet-session__unread");
                if (unread) {
                    unread.hidden = !(session.badge === "done" && (session.acknowledgedAt || 0) < (session.updatedAt || 0));
                    unread.title = text.sessionHud?.completionUnread || "Completed";
                }
                const focus = current.querySelector(".mf-pet-session__focus");
                if (focus) {
                    focus.hidden = session.canFocus !== false;
                    focus.title = text.sessionHud?.focusUnavailable || "Focus unavailable";
                }
                current.querySelector(".mf-pet-session__usage")?.remove();
                const usage = createContextUsageNode(session.contextUsage, text);
                if (usage) current.querySelector(".mf-pet-session__pin")?.before(usage);
                existing.delete(session.id);
                return;
            }
            const node = createSessionNode(session, text);
            const pinButton = node.querySelector(".mf-pet-session__pin");
            if (pinButton) pinButton.title = snapshot.sessionHudPinned ? text.menu.sessionHudUnpin : text.menu.sessionHudPin;
            node.querySelector(".mf-pet-session__pin")?.addEventListener("click", (event) => {
                event.stopPropagation();
                runtime.setSessionHudPinned(!runtime.getSnapshot().sessionHudPinned);
            });
            node.addEventListener("click", () => {
                handleSessionHudClick(node.dataset.sessionId);
            });
            sessionHud.append(node);
        });
        existing.forEach((node) => node.remove());
        if (foldedSessions.length) {
            const folded = createFoldedSessionNode(foldedSessions.length, text);
            folded.addEventListener("click", () => runtime.openDashboard());
            sessionHud.append(folded);
        }
        sessionHud.hidden = visibleSessions.length === 0 || snapshot.expanded || snapshot.mini || (!snapshot.sessionHudPinned && !snapshot.sessionHudRevealed);
    }

    function renderSessionDetail(snapshot) {
        const text = resolveText(snapshot.lang);
        sessionDetail.replaceChildren();
        const session = snapshot.sessions.find((item) => item.id === selectedSessionId && !item.hiddenFromDashboard);
        if (!session || snapshot.expanded) {
            sessionDetail.hidden = true;
            return;
        }
        sessionDetail.append(createSessionDetailNode(session, text));
        sessionDetail.hidden = false;
    }

    function renderDashboard(snapshot) {
        const text = resolveText(snapshot.lang);
        dashboard.hidden = !snapshot.dashboardOpen;
        dashboardFilters.replaceChildren();
        dashboardBody.replaceChildren();
        if (!snapshot.dashboardOpen) return;
        const visibleSessions = snapshot.sessions.filter((session) => !session.hiddenFromDashboard);
        const filterLabels = text.dashboard.filters || {};
        ["all", "running", "waiting", "done"].forEach((filter) => {
            const button = createElement("button", "mf-pet-dashboard__filter", { type: "button" });
            const count = visibleSessions.filter((session) => dashboardFilterMatches(session, filter)).length;
            button.textContent = `${filterLabels[filter] || filter} ${count}`;
            button.dataset.active = String(dashboardFilter === filter);
            button.addEventListener("click", () => {
                dashboardFilter = filter;
                renderDashboard(runtime.getSnapshot());
            });
            dashboardFilters.append(button);
        });
        if (!visibleSessions.length) {
            const empty = createElement("div", "mf-pet-dashboard__empty");
            empty.textContent = text.dashboard.empty;
            dashboardBody.append(empty);
            return;
        }
        const filteredSessions = visibleSessions
            .filter((session) => dashboardFilterMatches(session, dashboardFilter))
            .sort((a, b) => (b.updatedAt || b.createdAt || 0) - (a.updatedAt || a.createdAt || 0));
        if (!filteredSessions.length) {
            const empty = createElement("div", "mf-pet-dashboard__empty");
            empty.textContent = text.dashboard.emptyFiltered || text.dashboard.empty;
            dashboardBody.append(empty);
            return;
        }
        const filteredById = new Map(filteredSessions.map((session) => [session.id, session]));
        const groups = Array.isArray(snapshot.sessionGroups) && snapshot.sessionGroups.length
            ? snapshot.sessionGroups
            : [{
                key: "local",
                title: text.dashboard.localGroup,
                ids: filteredSessions.map((session) => session.id),
            }];
        groups.forEach((group) => {
            const groupSessions = (group.ids || []).map((id) => filteredById.get(id)).filter(Boolean);
            if (!groupSessions.length) return;
            const section = createElement("section", "mf-pet-dashboard-group");
            const header = createElement("div", "mf-pet-dashboard-group__title");
            const count = createElement("span", "mf-pet-dashboard-group__count");
            const cards = createElement("div", "mf-pet-dashboard-group__cards");
            header.textContent = group.title || text.dashboard.localGroup;
            count.textContent = String(groupSessions.length);
            header.append(count);
            groupSessions.forEach((session) => {
                cards.append(createDashboardSessionNode(session, text, runtime, api, config));
            });
            section.append(header, cards);
            dashboardBody.append(section);
        });
    }

    function closeContextMenu() {
        contextMenuOpen = false;
        contextMenu.hidden = true;
        root.dataset.menuOpen = "false";
    }

    function renderContextMenu(snapshot) {
        if (!contextMenuOpen) return;
        const text = resolveText(snapshot.lang);
        const menu = text.menu;
        const desktopShell = snapshot.shellMode === "desktop" && window.mfPetShell;
        contextMenu.replaceChildren(
            createMenuButton(snapshot.expanded ? menu.collapse : menu.expand, () => {
                snapshot.expanded ? api.collapse() : api.expand();
                closeContextMenu();
            }),
            createMenuButton(menu.chat, () => {
                api.openChat();
                closeContextMenu();
            }),
            createMenuButton(snapshot.dashboardOpen ? menu.dashboardClose : menu.dashboard, () => {
                if (desktopShell?.openDashboard) desktopShell.openDashboard();
                else api.toggleDashboard();
                closeContextMenu();
            }),
            ...(desktopShell?.openSettings
                ? [createMenuButton(menu.settings || "Settings...", () => {
                    desktopShell.openSettings();
                    closeContextMenu();
                })]
                : []),
            createMenuDivider(),
            createMenuButton(snapshot.sessionHudPinned ? menu.sessionHudUnpin : menu.sessionHudPin, () => {
                api.setSessionHudPinned(!runtime.getSnapshot().sessionHudPinned);
                closeContextMenu();
            }),
            createMenuButton(snapshot.mini ? menu.miniExit : menu.miniEnter, () => {
                snapshot.mini ? api.exitMini() : api.enterMini({ edge: snapshot.miniEdge || "right" });
                closeContextMenu();
            }),
            createMenuButton(snapshot.lowPower ? menu.lowPowerOff : menu.lowPowerOn, () => {
                api.setLowPower(!runtime.getSnapshot().lowPower);
                closeContextMenu();
            }),
            createMenuButton(snapshot.doNotDisturb ? menu.doNotDisturbOff : menu.doNotDisturbOn, () => {
                api.setDoNotDisturb(!runtime.getSnapshot().doNotDisturb);
                closeContextMenu();
            }),
            createMenuDivider(),
            createMenuButton(snapshot.permissionBubblesEnabled ? menu.permissionBubblesOff : menu.permissionBubblesOn, () => {
                api.setPermissionBubblesEnabled(!runtime.getSnapshot().permissionBubblesEnabled);
                closeContextMenu();
            }),
            createMenuButton(snapshot.hideBubbles ? menu.bubblesOn : menu.bubblesOff, () => {
                api.setHideBubbles(!runtime.getSnapshot().hideBubbles);
                closeContextMenu();
            }),
            createMenuButton(snapshot.muted ? menu.muteOff : menu.muteOn, () => {
                api.setMuted(!runtime.getSnapshot().muted);
                closeContextMenu();
            }),
            createMenuDivider(),
            createMenuButton(menu.resetPosition, () => {
                runtime.setPosition(null);
                root.style.left = "";
                root.style.top = "";
                root.style.right = "";
                root.style.bottom = "";
                closeContextMenu();
            }),
            createMenuButton(menu.hide, () => {
                api.hide();
                closeContextMenu();
            }),
        );
        contextMenu.hidden = false;
    }

    function openContextMenu(event) {
        event.preventDefault();
        const snapshot = runtime.getSnapshot();
        const rect = toggle.getBoundingClientRect();
        const edge = rect.left < window.innerWidth / 2 ? "left" : "right";
        const isDesktop = snapshot.shellMode === "desktop";
        const menuWidth = isDesktop ? 294 : 224;
        const menuHeight = isDesktop ? 560 : 390;
        const menuX = edge === "left"
            ? rect.right - 8
            : rect.left - menuWidth + 8;
        const menuY = rect.top + Math.max(10, Math.min(44, rect.height * 0.22));
        runtime.touch();
        contextMenuOpen = true;
        root.dataset.menuOpen = "true";
        root.style.setProperty("--mf-pet-menu-x", `${Math.max(8, Math.min(window.innerWidth - menuWidth - 8, menuX))}px`);
        root.style.setProperty("--mf-pet-menu-y", `${Math.max(8, Math.min(window.innerHeight - menuHeight - 8, menuY))}px`);
        root.dataset.menuEdge = edge;
        renderContextMenu(snapshot);
    }

    function render(snapshot) {
        const text = resolveText(snapshot.lang);
        root.dataset.state = snapshot.state;
        root.dataset.visible = String(snapshot.visible);
        root.dataset.expanded = String(snapshot.expanded);
        root.dataset.dashboardOpen = String(snapshot.dashboardOpen);
        root.dataset.mini = String(snapshot.mini);
        root.dataset.miniEdge = snapshot.miniEdge || "right";
        root.dataset.miniTransition = snapshot.miniTransition || "";
        root.dataset.dragging = String(snapshot.dragging);
        root.dataset.lowPower = String(snapshot.lowPower);
        root.dataset.doNotDisturb = String(snapshot.doNotDisturb);
        root.dataset.hideBubbles = String(snapshot.hideBubbles);
        root.dataset.allBubblesHidden = String(snapshot.allBubblesHidden);
        root.dataset.permissionBubblesEnabled = String(snapshot.permissionBubblesEnabled);
        root.dataset.sessionHudPinned = String(snapshot.sessionHudPinned);
        root.dataset.sessionHudRevealed = String(snapshot.sessionHudRevealed);
        root.dataset.shellMode = snapshot.shellMode || "web";
        root.dataset.muted = String(snapshot.muted);
        root.dataset.themeTransition = snapshot.themeTransition || "";
        root.style.setProperty("--mf-pet-size", `${snapshot.theme.ui.spriteSize}px`);
        root.style.setProperty("--mf-pet-panel-width", `${snapshot.theme.ui.panelWidth}px`);
        if (snapshot.position) {
            root.style.left = `${snapshot.position.x}px`;
            root.style.top = `${snapshot.position.y}px`;
            root.style.right = "auto";
            root.style.bottom = "auto";
        }
        panel.hidden = !snapshot.expanded;
        const visualSrc = snapshot.visual.src;
        if (isSvgAsset(visualSrc)) {
            if (spriteObject.data !== visualSrc) spriteObject.data = visualSrc;
            spriteObject.hidden = false;
            sprite.hidden = true;
        } else {
            sprite.src = visualSrc;
            spriteObject.hidden = true;
            sprite.hidden = false;
        }
        sprite.alt = text.stateLabels[snapshot.state] || snapshot.state;
        spriteObject.setAttribute("aria-label", sprite.alt);
        renderText(snapshot);
        renderChat(snapshot);
        renderNotifications(snapshot);
        renderPermissions(snapshot);
        renderSessionHud(snapshot);
        renderSessionDetail(snapshot);
        renderDashboard(snapshot);
        renderContextMenu(snapshot);
        if (window.mfPetShell?.updateSnapshot) {
            window.mfPetShell.updateSnapshot(snapshot);
        }
    }

    let unsubscribe = () => {};

    const api = {
        root,
        runtime,
        bridge,
        show(options) {
            runtime.show(options);
            return api;
        },
        hide() {
            runtime.hide();
            return api;
        },
        expand() {
            runtime.expand();
            return api;
        },
        collapse() {
            runtime.collapse();
            return api;
        },
        toggle() {
            runtime.toggle();
            return api;
        },
        emit(eventName, payload) {
            runtime.emit(eventName, payload);
            return api;
        },
        emitAgentEvent(eventName, payload) {
            runtime.emitAgentEvent(eventName, payload);
            return api;
        },
        startSession(id, initialState, meta) {
            return runtime.startSession(id, initialState, meta);
        },
        updateSession(id, patch) {
            runtime.updateSession(id, patch);
            return api;
        },
        endSession(id) {
            runtime.endSession(id);
            return api;
        },
        hideSession(id) {
            runtime.hideSession(id);
            return api;
        },
        async focusSession(id, options = {}) {
            const source = options.source || "hud";
            const session = runtime.focusSession(id, { source });
            try {
                const result = await config.onSessionFocus?.({
                    session,
                    runtime,
                    api,
                    source,
                });
                runtime.completeSessionFocus(id, result || { status: "success" });
            } catch (error) {
                runtime.completeSessionFocus(id, {
                    status: "failed",
                    reason: error?.message || "focus-callback-failed",
                });
            }
            return session;
        },
        completeSessionFocus(id, result) {
            return runtime.completeSessionFocus(id, result);
        },
        renameSession(id, displayTitle) {
            const session = runtime.renameSession(id, displayTitle);
            config.onSessionRename?.({
                session,
                title: String(displayTitle || "").trim(),
                runtime,
                api,
            });
            return session;
        },
        acknowledgeSession(id) {
            return runtime.acknowledgeSession(id);
        },
        setState(nextState, payload) {
            runtime.setState(nextState, payload);
            return api;
        },
        setLanguage(lang) {
            runtime.setLanguage(lang);
            return api;
        },
        setTheme(nextTheme, themeOptions) {
            runtime.setTheme(nextTheme, themeOptions);
            return api;
        },
        setMuted(nextMuted) {
            runtime.setMuted(nextMuted);
            return api;
        },
        setLowPower(nextLowPower) {
            runtime.setLowPower(nextLowPower);
            return api;
        },
        setDoNotDisturb(nextEnabled) {
            runtime.setDoNotDisturb(nextEnabled);
            return api;
        },
        setPermissionBubblesEnabled(nextEnabled) {
            runtime.setPermissionBubblesEnabled(nextEnabled);
            return api;
        },
        setBubbleCategoryEnabled(category, enabled) {
            runtime.setBubbleCategoryEnabled(category, enabled);
            return api;
        },
        getBubblePolicy(category) {
            return runtime.getBubblePolicy(category);
        },
        setShellCapabilities(capabilities) {
            runtime.setShellCapabilities(capabilities);
            return api;
        },
        setHideBubbles(nextHidden) {
            runtime.setHideBubbles(nextHidden);
            return api;
        },
        setPermissionBubbleAutoCloseSeconds(seconds) {
            runtime.setPermissionBubbleAutoCloseSeconds(seconds);
            return api;
        },
        setSessionHudPinned(nextPinned) {
            runtime.setSessionHudPinned(nextPinned);
            return api;
        },
        revealSessionHud() {
            runtime.revealSessionHud();
            return api;
        },
        hideSessionHud() {
            runtime.hideSessionHud();
            return api;
        },
        openDashboard() {
            runtime.openDashboard();
            return api;
        },
        closeDashboard() {
            runtime.closeDashboard();
            return api;
        },
        toggleDashboard() {
            runtime.toggleDashboard();
            return api;
        },
        sleep() {
            runtime.sleep();
            return api;
        },
        wake() {
            runtime.wake();
            return api;
        },
        openChat() {
            runtime.openChat();
            if (runtime.getSnapshot().chatMessages.length === 0) {
                const text = resolveText(runtime.getSnapshot().lang);
                runtime.addChatMessage({
                    role: "assistant",
                    text: text.chat.welcome,
                });
            }
            return api;
        },
        closeChat() {
            runtime.closeChat();
            return api;
        },
        addNotification(message) {
            runtime.addNotification(message);
            return api;
        },
        setNotificationBubbleAutoCloseSeconds(seconds) {
            runtime.setNotificationBubbleAutoCloseSeconds(seconds);
            return api;
        },
        resolvePermission(id, decision) {
            runtime.resolvePermission(id, decision);
            return api;
        },
        dismissPermission(id) {
            runtime.dismissPermission(id);
            return api;
        },
        addChatMessage(message) {
            return runtime.addChatMessage(message);
        },
        streamAssistantMessage(text, options = {}) {
            const sessionId = options.sessionId || runtime.startSession(null, "working", {
                title: options.title || "chat",
                displayHint: options.displayHint,
            });
            const message = runtime.addChatMessage({
                role: "assistant",
                text: "",
                streaming: true,
            });
            const chars = [...String(text || "")];
            const interval = options.interval ?? 28;
            let index = 0;
            const timer = window.setInterval(() => {
                index += 1;
                runtime.updateChatMessage(message.id, {
                    text: chars.slice(0, index).join(""),
                    streaming: index < chars.length,
                });
                if (index >= chars.length) {
                    window.clearInterval(timer);
                    if (options.endSession !== false) runtime.endSession(sessionId);
                    runtime.setState("success", { returnTo: "expanded" });
                }
            }, interval);
            return message.id;
        },
        clearChat() {
            runtime.clearChat();
            return api;
        },
        enterMini(options) {
            runtime.enterMini(options);
            return api;
        },
        exitMini() {
            runtime.exitMini();
            return api;
        },
        getSnapshot() {
            return runtime.getSnapshot();
        },
        destroy() {
            unsubscribe();
            document.removeEventListener("pointerdown", handleDocumentPointerDown);
            runtime.destroy();
            root.remove();
        },
    };
    unsubscribe = runtime.subscribe(render);
    if (window.mfPetShell?.onCursorMove) {
        window.mfPetShell.onCursorMove(handleShellCursor);
    }
    if (window.mfPetShell?.onCommand) {
        window.mfPetShell.onCommand((message = {}) => {
            const command = message.command;
            const payload = message.payload || {};
            if (command === "show") api.show(payload);
            if (command === "hide") api.hide();
            if (command === "openChat") api.openChat();
            if (command === "openDashboard") api.openDashboard();
            if (command === "setLowPower") api.setLowPower(payload.enabled);
            if (command === "setDoNotDisturb") api.setDoNotDisturb(payload.enabled);
            if (command === "setPermissionBubblesEnabled") api.setPermissionBubblesEnabled(payload.enabled);
            if (command === "setHideBubbles") api.setHideBubbles(payload.enabled);
            if (command === "setMuted") api.setMuted(payload.enabled);
            if (command === "setSessionHudPinned") api.setSessionHudPinned(payload.enabled);
            if (command === "enterMini") api.enterMini({ edge: payload.edge });
            if (command === "exitMini") api.exitMini();
            if (command === "resetPosition") {
                runtime.setPosition(null);
                root.style.left = "";
                root.style.top = "";
                root.style.right = "";
                root.style.bottom = "";
            }
            if (command === "focusSession") api.focusSession(payload.id, { source: "native-dashboard" });
            if (command === "acknowledgeSession") api.acknowledgeSession(payload.id);
            if (command === "hideSession") api.hideSession(payload.id);
            if (command === "endSession") api.endSession(payload.id);
        });
    }

    close.addEventListener("click", () => api.collapse());
    dashboardClose.addEventListener("click", () => api.closeDashboard());
    chatClose.addEventListener("click", () => api.closeChat());
    chatComposer.addEventListener("submit", async (event) => {
        event.preventDefault();
        const value = chatInput.value.trim();
        if (!value) return;
        chatInput.value = "";
        runtime.addChatMessage({ role: "user", text: value });
        const sessionId = runtime.startSession(null, "thinking", {
            title: value,
            displayHint: "clawd-working-typing.svg",
        });
        const handled = await config.onChatSubmit?.({
            text: value,
            api,
            runtime,
            sessionId,
        });
        if (handled === true) return;
        runtime.updateSession(sessionId, { state: "working" });
        api.streamAssistantMessage("我先记录下你的问题。后续接入苗丰 AI 后，这里会以流式方式返回更完整的种植建议。", {
            sessionId,
        });
    });
    root.addEventListener("pointermove", () => runtime.touch());

    toggle.addEventListener("dblclick", (event) => {
        event.preventDefault();
        runtime.playReaction(event.offsetX < toggle.clientWidth / 2 ? "clickLeft" : "clickRight");
    });
    toggle.addEventListener("click", () => {
        if (suppressNextClick) {
            suppressNextClick = false;
            return;
        }
        const snapshot = runtime.getSnapshot();
        const hasHudSessions = snapshot.sessions.some((session) => !session.headless && !session.hiddenFromDashboard && session.state !== "idle");
        if (hasHudSessions && !snapshot.sessionHudPinned && !snapshot.sessionHudRevealed && !snapshot.expanded) {
            runtime.revealSessionHud();
            return;
        }
        api.toggle();
        const now = Date.now();
        clickTimes = [...clickTimes.filter((time) => now - time < 900), now];
        if (clickTimes.length >= 4) {
            clickTimes = [];
            runtime.playReaction("double");
        }
    });
    toggle.addEventListener("contextmenu", (event) => {
        openContextMenu(event);
    });
    const handleDocumentPointerDown = (event) => {
        if (!contextMenuOpen) return;
        if (!root.contains(event.target)) closeContextMenu();
    };
    document.addEventListener("pointerdown", handleDocumentPointerDown);
    toggle.addEventListener("mouseenter", () => {
        clearTimeout(sessionHudHideTimer);
        const snapshot = runtime.getSnapshot();
        if (snapshot.mini && snapshot.theme.miniMode?.states?.["mini-peek"]) runtime.setState("mini-peek");
    });
    toggle.addEventListener("mouseleave", () => {
        const snapshot = runtime.getSnapshot();
        if (snapshot.mini) runtime.setState("mini-idle");
        if (!snapshot.mini) sessionHudHideTimer = setTimeout(() => runtime.hideSessionHud(), 650);
    });
    sessionHud.addEventListener("mouseenter", () => {
        clearTimeout(sessionHudHideTimer);
        runtime.revealSessionHud();
    });
    sessionHud.addEventListener("mouseleave", () => {
        sessionHudHideTimer = setTimeout(() => runtime.hideSessionHud(), 650);
    });
    toggle.addEventListener("pointerdown", (event) => {
        if (event.button !== 0) return;
        const snapshot = runtime.getSnapshot();
        if (snapshot.shellMode === "desktop" && config.desktopWindowDrag && window.mfPetShell?.moveBy) {
            nativeDragStart = {
                pointerId: event.pointerId,
                x: event.screenX,
                y: event.screenY,
                moved: false,
            };
            toggle.setPointerCapture(event.pointerId);
            runtime.setDragging(true);
            event.preventDefault();
            return;
        }
        dragging = true;
        dragStart = {
            pointerId: event.pointerId,
            x: event.clientX,
            y: event.clientY,
            left: root.getBoundingClientRect().left,
            top: root.getBoundingClientRect().top,
        };
        toggle.setPointerCapture(event.pointerId);
        runtime.setDragging(true);
    });
    toggle.addEventListener("pointermove", (event) => {
        if (nativeDragStart) {
            const dx = event.screenX - nativeDragStart.x;
            const dy = event.screenY - nativeDragStart.y;
            if (Math.abs(dx) > 2 || Math.abs(dy) > 2) {
                nativeDragging = true;
                nativeDragStart.moved = true;
                suppressNextClick = true;
                window.mfPetShell.moveBy({ x: dx, y: dy });
                nativeDragStart.x = event.screenX;
                nativeDragStart.y = event.screenY;
            }
            event.preventDefault();
            return;
        }
        if (!dragging || !dragStart) return;
        const dx = event.clientX - dragStart.x;
        const dy = event.clientY - dragStart.y;
        if (Math.abs(dx) > 4 || Math.abs(dy) > 4) suppressNextClick = true;
        const next = {
            x: Math.max(8, Math.min(window.innerWidth - root.offsetWidth - 8, dragStart.left + dx)),
            y: Math.max(8, Math.min(window.innerHeight - root.offsetHeight - 8, dragStart.top + dy)),
        };
        const direction = dx < -4 ? "left" : dx > 4 ? "right" : null;
        runtime.setDragging(true, { position: next, direction });
    });
    toggle.addEventListener("pointerup", async (event) => {
        if (nativeDragStart) {
            const wasNativeDragging = nativeDragging || nativeDragStart.moved;
            nativeDragStart = null;
            nativeDragging = false;
            toggle.releasePointerCapture(event.pointerId);
            runtime.setDragging(false);
            if (wasNativeDragging && window.mfPetShell?.finishDrag) {
                const result = await window.mfPetShell.finishDrag();
                if (result?.edge) runtime.enterMini({ edge: result.edge });
            }
            event.preventDefault();
            return;
        }
        if (!dragging) return;
        dragging = false;
        dragStart = null;
        toggle.releasePointerCapture(event.pointerId);
        const rect = root.getBoundingClientRect();
        const nearRight = window.innerWidth - rect.right < 28;
        const nearLeft = rect.left < 28;
        runtime.setDragging(false, {
            position: {
                x: rect.left,
                y: rect.top,
            },
        });
        if (nearRight || nearLeft) {
            runtime.setPosition(null);
            root.style.left = "";
            root.style.top = "";
            root.style.right = "";
            root.style.bottom = "";
            runtime.enterMini({ edge: nearLeft ? "left" : "right" });
        }
    });

    return api;
}

export { clawdDevTheme } from "./themes/clawdDevTheme.js";
export { mfSproutTheme } from "./themes/mfSproutTheme.js";
export { createPetRuntime } from "./core/petRuntime.js";
export { createPetEventBridge, EVENT_PRESETS } from "./core/petEventBridge.js";
export { loadPetThemeFromUrl, parsePetThemeJson, exportPetThemeJson } from "./core/petThemeJson.js";
export { checkPetThemeAssets, listPetThemeAssets } from "./core/petThemeHealth.js";
export { createPetThemeRegistry } from "./core/petThemeRegistry.js";
