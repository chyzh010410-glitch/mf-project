import { readPetStorage, writePetStorage } from "./petStorage.js";
import { PetStateMachine } from "./petStateMachine.js";
import { normalizePetTheme } from "./petThemeLoader.js";
import { deriveSessionBadge, normalizeContextUsage, normalizeQuotaUsage, normalizeSessionMeta, pushSessionEvent, shouldPruneSession } from "./petSessionModel.js";
import { joinPetAssetPath, resolveVisualAsset } from "./petVisualResolver.js";

const SESSION_ALIAS_TTL_MS = 7 * 24 * 60 * 60 * 1000;
const MAX_SESSION_ALIAS_LENGTH = 80;
const DEFAULT_NOTIFICATION_AUTO_CLOSE_SECONDS = 6;

function normalizeLang(lang) {
    return lang === "en" ? "en" : "zh";
}

function truncateText(value, max = 120) {
    const text = String(value || "").trim();
    return text.length > max ? `${text.slice(0, max - 1)}…` : text;
}

function firstStringValue(input, names) {
    if (!input || typeof input !== "object") return "";
    for (const name of names) {
        const value = input[name];
        if (typeof value === "string" && value.trim()) return value.trim();
    }
    return "";
}

function normalizeAutoCloseSeconds(value, fallback = 0) {
    const number = Number(value);
    if (!Number.isFinite(number) || number < 0) return fallback;
    return Math.min(3600, Math.trunc(number));
}

function formatToolInputDescription(toolName, input) {
    if (!input || typeof input !== "object") return "";
    const name = String(toolName || "").trim().toLowerCase();
    if (name === "bash" || name === "shell" || name === "run_command") {
        return truncateText(firstStringValue(input, ["CommandLine", "command", "Command", "cmd"]), 160);
    }
    if (["edit", "write", "multiedit", "write_to_file", "replace_file_content", "multi_replace_file_content"].includes(name)) {
        const filePath = firstStringValue(input, ["TargetFile", "AbsolutePath", "file_path", "path", "filePath", "FilePath"]);
        const description = firstStringValue(input, ["Description", "Instruction", "description"]);
        return truncateText(description && filePath ? `${filePath}: ${description}` : filePath || description, 160);
    }
    if (name === "read" || name === "view_file") {
        return truncateText(firstStringValue(input, ["AbsolutePath", "file_path", "path", "filePath", "FilePath"]), 160);
    }
    if (name === "grep" || name === "grep_search") {
        const searchPath = firstStringValue(input, ["SearchPath", "SearchDirectory", "DirectoryPath", "path"]);
        const query = firstStringValue(input, ["Query", "query", "pattern", "Pattern"]);
        return truncateText(query && searchPath ? `${searchPath}: ${query}` : searchPath || query, 160);
    }
    if (name === "ask_permission") {
        const target = firstStringValue(input, ["Target", "target", "Permission", "permission"]);
        const reason = firstStringValue(input, ["Reason", "reason", "Description", "description"]);
        return truncateText(reason && target ? `${target}: ${reason}` : target || reason, 160);
    }
    if (typeof input.description === "string" && input.description.trim()) return truncateText(input.description, 120);
    for (const value of Object.values(input)) {
        if (typeof value === "string" && value.trim()) return truncateText(value, 100);
    }
    try {
        return truncateText(JSON.stringify(input), 120);
    } catch {
        return "";
    }
}

function formatToolDisplayName(toolName) {
    const name = String(toolName || "").trim();
    const parts = name.split("__").filter(Boolean);
    if (parts.length >= 3 && parts[0].toLowerCase() === "mcp") {
        return `${parts[parts.length - 2].toLowerCase()} · ${parts[parts.length - 1].toLowerCase()}`;
    }
    return name;
}

function sanitizeSessionAlias(value) {
    if (typeof value !== "string") return null;
    const cleaned = value.replace(/[\u0000-\u001f\u007f]/g, " ").replace(/\s+/g, " ").trim();
    if (!cleaned) return "";
    return cleaned.length > MAX_SESSION_ALIAS_LENGTH ? cleaned.slice(0, MAX_SESSION_ALIAS_LENGTH) : cleaned;
}

function sessionAliasKey(session = {}) {
    const id = String(session.id || "").trim();
    if (!id) return null;
    const host = String(session.host || "local").trim() || "local";
    const agent = String(session.agentId || "unknown").trim() || "unknown";
    return [host, agent, id].join("|");
}

function normalizeSessionAliases(value = {}, now = Date.now()) {
    if (!value || typeof value !== "object" || Array.isArray(value)) return {};
    const out = {};
    Object.entries(value).forEach(([key, entry]) => {
        if (!entry || typeof entry !== "object") return;
        const title = sanitizeSessionAlias(entry.title);
        const updatedAt = Number(entry.updatedAt);
        if (!key || !title || !Number.isFinite(updatedAt)) return;
        if (now - updatedAt > SESSION_ALIAS_TTL_MS) return;
        out[key] = { title, updatedAt };
    });
    return out;
}

function normalizeFocusResult(result, fallbackStatus = "success") {
    if (result === false) return { status: "failed", reason: "rejected" };
    if (!result || typeof result !== "object") return { status: fallbackStatus, reason: "" };
    const status = ["success", "failed", "unavailable", "pending"].includes(result.status)
        ? result.status
        : fallbackStatus;
    return {
        status,
        reason: typeof result.reason === "string" ? result.reason.slice(0, 120) : "",
        target: result.target && typeof result.target === "object" ? result.target : null,
    };
}

function normalizePermissionSuggestions(payload = {}) {
    const raw = payload.permissionSuggestions || payload.permission_suggestions || payload.suggestions || [];
    if (!Array.isArray(raw)) return [];
    return raw.slice(0, 4).map((item, index) => {
        if (typeof item === "string") {
            return {
                id: item || `suggestion-${index}`,
                label: item,
                behavior: item,
                type: "",
            };
        }
        if (!item || typeof item !== "object") return null;
        const behavior = item.behavior || item.decision || item.mode || item.type || `suggestion-${index}`;
        return {
            id: item.id || behavior || `suggestion-${index}`,
            label: item.label || item.title || item.text || behavior,
            behavior,
            type: item.type || "",
            description: item.description || item.detail || "",
        };
    }).filter(Boolean);
}

export function createPetRuntime(options = {}) {
    let theme = normalizePetTheme(options.theme);
    const storageKey = options.storageKey || "mf-forest-pet";
    const saved = options.restore === false ? null : readPetStorage(storageKey);
    const listeners = new Set();
    const state = {
        lang: normalizeLang(options.lang),
        visible: saved?.visible ?? !!options.initiallyVisible,
        expanded: saved?.expanded ?? !!options.initiallyExpanded,
        chatOpen: saved?.chatOpen ?? false,
        dashboardOpen: false,
        mini: saved?.mini ?? false,
        miniEdge: saved?.miniEdge || "right",
        miniTransition: null,
        dragging: false,
        reaction: null,
        position: saved?.position || null,
        chatMessages: [],
        notifications: [],
        permissionRequests: [],
        muted: saved?.muted ?? !!options.muted,
        lowPower: saved?.lowPower ?? !!options.lowPower,
        doNotDisturb: saved?.doNotDisturb ?? !!options.doNotDisturb,
        hideBubbles: saved?.hideBubbles ?? !!options.hideBubbles,
        permissionBubblesEnabled: saved?.permissionBubblesEnabled ?? options.permissionBubblesEnabled !== false,
        permissionBubbleAutoCloseSeconds: saved?.permissionBubbleAutoCloseSeconds ?? normalizeAutoCloseSeconds(options.permissionBubbleAutoCloseSeconds, 0),
        notificationBubbleAutoCloseSeconds: saved?.notificationBubbleAutoCloseSeconds ?? normalizeAutoCloseSeconds(options.notificationBubbleAutoCloseSeconds, DEFAULT_NOTIFICATION_AUTO_CLOSE_SECONDS),
        sessionHudPinned: saved?.sessionHudPinned ?? options.sessionHudPinned === true,
        sessionHudRevealed: saved?.sessionHudPinned ?? options.sessionHudPinned === true,
        sessionAliases: normalizeSessionAliases(saved?.sessionAliases),
        shellMode: options.shellMode === "desktop" ? "desktop" : "web",
        shellCapabilities: {
            transparentWindow: false,
            alwaysOnTop: false,
            tray: false,
            nativeFocus: false,
            multiScreen: false,
            ...(options.shellCapabilities || {}),
        },
        assetsBaseUrl: options.assetsBaseUrl || theme.assetBaseUrl || "",
        themeTransition: null,
    };
    const sessions = new Map();

    const machine = new PetStateMachine(theme, {
        initialState: saved?.state || options.initialState || "idle",
        onChange: notify,
    });

    let restingTimer = null;
    let idleAnimationTimer = null;
    let miniTransitionTimer = null;
    let staleCleanupTimer = null;
    const permissionAutoCloseTimers = new Map();
    const notificationAutoCloseTimers = new Map();
    let destroyed = false;

    function pruneSessionAliases() {
        const activeKeys = new Set([...sessions.values()].map(sessionAliasKey).filter(Boolean));
        const now = Date.now();
        const next = {};
        Object.entries(state.sessionAliases).forEach(([key, entry]) => {
            if (activeKeys.has(key) || now - entry.updatedAt <= SESSION_ALIAS_TTL_MS) next[key] = entry;
        });
        state.sessionAliases = next;
    }

    function persist() {
        pruneSessionAliases();
        writePetStorage(storageKey, {
            visible: state.visible,
            expanded: state.expanded,
            chatOpen: state.chatOpen,
            state: machine.currentState,
            themeId: theme.id,
            muted: state.muted,
            lowPower: state.lowPower,
            doNotDisturb: state.doNotDisturb,
            hideBubbles: state.hideBubbles,
            permissionBubblesEnabled: state.permissionBubblesEnabled,
            permissionBubbleAutoCloseSeconds: state.permissionBubbleAutoCloseSeconds,
            notificationBubbleAutoCloseSeconds: state.notificationBubbleAutoCloseSeconds,
            sessionHudPinned: state.sessionHudPinned,
            sessionAliases: state.sessionAliases,
            mini: state.mini,
            miniEdge: state.miniEdge,
            position: state.position,
        });
    }

    function sessionUpdatedAt(session) {
        return Number(session.updatedAt || session.createdAt || 0);
    }

    function sessionLastEvent(session) {
        const events = Array.isArray(session?.recentEvents) ? session.recentEvents : [];
        const event = events[events.length - 1];
        if (!event) return null;
        return {
            event: event.event || null,
            state: event.state || session.state || "idle",
            at: Number.isFinite(Number(event.at)) ? Number(event.at) : sessionUpdatedAt(session),
        };
    }

    function buildSessionEntries() {
        return [...sessions.values()].map((session) => {
            const badge = deriveSessionBadge(session);
            return {
                ...session,
                badge,
                lastEvent: sessionLastEvent(session),
                requiresCompletionAck: badge === "done" && (session.acknowledgedAt || 0) < (session.updatedAt || 0),
            };
        });
    }

    function buildSessionSnapshotSummary(entries) {
        const dashboardEntries = entries
            .filter((session) => !session.hiddenFromDashboard)
            .sort((a, b) => {
                const byTime = sessionUpdatedAt(b) - sessionUpdatedAt(a);
                return byTime || String(a.id).localeCompare(String(b.id));
            });
        const orderedSessionIds = dashboardEntries.map((session) => session.id);
        const hudEntries = dashboardEntries.filter((session) => !session.headless && session.state !== "idle");
        const sessionCountsByBadge = dashboardEntries.reduce((counts, session) => {
            const badge = session.badge || "idle";
            counts[badge] = (counts[badge] || 0) + 1;
            return counts;
        }, {});
        const groupMap = new Map();
        dashboardEntries.forEach((session) => {
            const key = session.host || session.agentLabel || session.agentId || session.provider || "local";
            const title = session.host || session.agentLabel || session.agentId || session.provider || "local";
            if (!groupMap.has(key)) {
                groupMap.set(key, {
                    key,
                    title,
                    ids: [],
                });
            }
            groupMap.get(key).ids.push(session.id);
        });
        const groups = [...groupMap.values()].map((group) => ({
            ...group,
            count: group.ids.length,
        }));
        const lastSession = dashboardEntries[0] || null;
        return {
            sessionGroups: groups,
            orderedSessionIds,
            dashboardSessionCount: dashboardEntries.length,
            sessionCountsByBadge,
            hudTotalNonIdle: hudEntries.length,
            hudLastSessionId: hudEntries[0]?.id || null,
            hudLastTitle: hudEntries[0]?.displayTitle || hudEntries[0]?.title || null,
            lastSessionId: lastSession?.id || null,
            lastTitle: lastSession?.displayTitle || lastSession?.title || null,
        };
    }

    function areAllBubblesHidden() {
        return state.hideBubbles || (!state.permissionBubblesEnabled && state.notificationBubbleAutoCloseSeconds === 0);
    }

    function buildBubblePolicySnapshot() {
        return {
            permission: {
                enabled: !state.hideBubbles && state.permissionBubblesEnabled,
                autoCloseMs: state.permissionBubbleAutoCloseSeconds > 0 ? state.permissionBubbleAutoCloseSeconds * 1000 : 0,
            },
            notification: {
                enabled: !state.hideBubbles && state.notificationBubbleAutoCloseSeconds > 0,
                autoCloseMs: state.notificationBubbleAutoCloseSeconds > 0 ? state.notificationBubbleAutoCloseSeconds * 1000 : 0,
            },
            update: {
                enabled: false,
                autoCloseMs: 0,
            },
        };
    }

    function snapshot() {
        const sessionEntries = buildSessionEntries();
        const sessionSummary = buildSessionSnapshotSummary(sessionEntries);
        return {
            theme,
            lang: state.lang,
            visible: state.visible,
            expanded: state.expanded,
            chatOpen: state.chatOpen,
            dashboardOpen: state.dashboardOpen,
            mini: state.mini,
            miniEdge: state.miniEdge,
            miniTransition: state.miniTransition,
            dragging: state.dragging,
            reaction: state.reaction,
            position: state.position,
            chatMessages: [...state.chatMessages],
            notifications: [...state.notifications],
            permissionRequests: [...state.permissionRequests],
            muted: state.muted,
            lowPower: state.lowPower,
            doNotDisturb: state.doNotDisturb,
            hideBubbles: state.hideBubbles,
            allBubblesHidden: areAllBubblesHidden(),
            bubblePolicy: buildBubblePolicySnapshot(),
            permissionBubblesEnabled: state.permissionBubblesEnabled,
            permissionBubbleAutoCloseSeconds: state.permissionBubbleAutoCloseSeconds,
            notificationBubbleAutoCloseSeconds: state.notificationBubbleAutoCloseSeconds,
            sessionHudPinned: state.sessionHudPinned,
            sessionHudRevealed: state.sessionHudRevealed,
            sessionAliases: { ...state.sessionAliases },
            shellMode: state.shellMode,
            shellCapabilities: { ...state.shellCapabilities },
            themeTransition: state.themeTransition,
            sessions: sessionEntries,
            ...sessionSummary,
            sessionCount: countActiveSessions(),
            state: machine.currentState,
            visual: state.reaction
                ? {
                    file: state.reaction,
                    src: joinPetAssetPath(state.assetsBaseUrl || theme.assetBaseUrl, state.reaction),
                }
                : resolveVisualAsset(theme, machine.currentState, {
                    assetsBaseUrl: state.assetsBaseUrl || theme.assetBaseUrl,
                    sessions,
                    sessionCount: countActiveSessions(),
                    jugglingSessionCount: countActiveSessions(new Set(["juggling"])),
                }),
        };
    }

    function notify() {
        persist();
        const next = snapshot();
        listeners.forEach((listener) => listener(next));
        scheduleIdleAnimation();
        resetStaleCleanupTimer();
    }

    function resetRestingTimer() {
        clearTimeout(restingTimer);
        const restingAfter = theme.timings.restingAfter || 0;
        if (!restingAfter) return;
        restingTimer = setTimeout(() => {
            if (state.visible && !state.expanded && !state.chatOpen) {
                machine.setState("resting");
            }
        }, restingAfter);
    }

    function countActiveSessions(states = new Set(["working", "thinking", "juggling"])) {
        let count = 0;
        sessions.forEach((session) => {
            if (!session.headless && !session.hiddenFromDashboard && states.has(session.state)) count += 1;
        });
        return count;
    }

    function getStatePriority(stateName) {
        return theme.statePriority[stateName] ?? 0;
    }

    function resolveDominantSessionState() {
        if (state.doNotDisturb) return "resting";
        let best = null;
        sessions.forEach((session) => {
            if (session.headless || session.hiddenFromDashboard) return;
            if (!best || getStatePriority(session.state) > getStatePriority(best.state)) best = session;
        });
        return best?.state || "idle";
    }

    function mapMiniState(nextState) {
        if (!state.mini || !theme.miniMode.supported) return nextState;
        if (nextState === "notification" || nextState === "error") return "mini-alert";
        if (nextState === "attention" || nextState === "success") return "mini-happy";
        if (["working", "thinking", "juggling", "carrying", "sweeping"].includes(nextState)) return "mini-working";
        if (nextState === "resting") return "mini-sleep";
        return "mini-idle";
    }

    function applyResolvedSessionState(payload = {}) {
        const nextState = mapMiniState(resolveDominantSessionState());
        machine.setState(nextState, payload);
    }

    function upsertSession(id, patch = {}) {
        const sessionId = id || `session-${Date.now()}-${sessions.size}`;
        const normalizedContextUsage = normalizeContextUsage(patch.contextUsage || patch.context_usage);
        const normalizedQuotaUsage = normalizeQuotaUsage(patch.quotaUsage || patch.quota_usage);
        const cleanPatch = { ...patch };
        delete cleanPatch.context_usage;
        delete cleanPatch.quota_usage;
        if (patch.contextUsage || patch.context_usage) {
            delete cleanPatch.contextUsage;
            if (normalizedContextUsage) cleanPatch.contextUsage = normalizedContextUsage;
        }
        if (patch.quotaUsage || patch.quota_usage) {
            delete cleanPatch.quotaUsage;
            if (normalizedQuotaUsage) cleanPatch.quotaUsage = normalizedQuotaUsage;
        }
        const normalizedMeta = normalizeSessionMeta(patch);
        Object.entries(normalizedMeta).forEach(([key, value]) => {
            if (value !== "" && value !== null) cleanPatch[key] = value;
        });
        [
            "agent_id",
            "agent_label",
            "agent_icon",
            "agent_color",
            "agentName",
            "agent_name",
            "icon",
            "hook_source",
            "source_pid",
            "turn_id",
            "workspace",
            "workspacePath",
            "workspace_path",
            "hostname",
            "machine",
            "osPlatform",
            "os_platform",
        ].forEach((key) => delete cleanPatch[key]);
        const existing = sessions.get(sessionId) || {
            id: sessionId,
            state: "idle",
            title: "",
            headless: false,
            recentEvents: [],
            createdAt: Date.now(),
        };
        const next = {
            ...existing,
            ...cleanPatch,
            id: sessionId,
            updatedAt: Date.now(),
        };
        const aliasKey = sessionAliasKey(next);
        const alias = aliasKey ? state.sessionAliases[aliasKey] : null;
        if (alias?.title && !("displayTitle" in cleanPatch)) next.displayTitle = alias.title;
        sessions.set(sessionId, next);
        return next;
    }

    function resetStaleCleanupTimer() {
        if (destroyed) return;
        clearTimeout(staleCleanupTimer);
        if (sessions.size === 0) return;
        staleCleanupTimer = setTimeout(() => {
            if (destroyed) return;
            let changed = false;
            sessions.forEach((session, id) => {
                if (shouldPruneSession(session)) {
                    sessions.delete(id);
                    changed = true;
                }
            });
            if (changed) {
                applyResolvedSessionState({ force: true });
                notify();
            } else {
                resetStaleCleanupTimer();
            }
        }, theme.timings.sessionCleanupInterval || 30000);
    }

    function scheduleIdleAnimation() {
        clearTimeout(idleAnimationTimer);
        const animations = theme.idleAnimations;
        if (!animations.length) return;
        if (state.lowPower) return;
        if (!state.visible || state.expanded || state.chatOpen || state.dragging || state.mini) return;
        if (machine.currentState !== "idle" || state.reaction) return;

        const delay = theme.timings.idleAnimationAfter || 12000;
        idleAnimationTimer = setTimeout(() => {
            if (!state.visible || state.expanded || state.chatOpen || state.dragging || state.mini) return;
            if (machine.currentState !== "idle" || state.reaction) return;
            const animation = animations[Math.floor(Math.random() * animations.length)];
            if (!animation?.file) return;
            state.reaction = animation.file;
            notify();
            setTimeout(() => {
                if (state.reaction === animation.file) {
                    state.reaction = null;
                    notify();
                }
            }, animation.duration || 5000);
        }, delay);
    }

    function settleMiniTransition() {
        clearTimeout(miniTransitionTimer);
        miniTransitionTimer = setTimeout(() => {
            state.miniTransition = null;
            notify();
        }, theme.miniMode.transitionMs || 360);
    }

    function clearNotificationAutoClose(id) {
        const timer = notificationAutoCloseTimers.get(id);
        if (timer) clearTimeout(timer);
        notificationAutoCloseTimers.delete(id);
    }

    function scheduleNotificationAutoClose(notification) {
        clearNotificationAutoClose(notification.id);
        if (!notification.autoCloseMs) return;
        const timer = setTimeout(() => {
            notificationAutoCloseTimers.delete(notification.id);
            state.notifications = state.notifications.filter((item) => item.id !== notification.id);
            notify();
        }, notification.autoCloseMs);
        notificationAutoCloseTimers.set(notification.id, timer);
    }

    function addNotification(message = {}) {
        const seconds = normalizeAutoCloseSeconds(
            message.notificationBubbleAutoCloseSeconds ?? message.notification_bubble_auto_close_seconds,
            state.notificationBubbleAutoCloseSeconds,
        );
        if ((state.doNotDisturb || state.hideBubbles || seconds === 0) && !message.force) return null;
        const autoCloseMs = Number.isFinite(Number(message.ttl))
            ? Math.max(0, Number(message.ttl))
            : seconds > 0
                ? seconds * 1000
                : DEFAULT_NOTIFICATION_AUTO_CLOSE_SECONDS * 1000;
        const notification = {
            id: `${Date.now()}-${state.notifications.length}`,
            type: message.type || "info",
            title: message.title || "",
            text: message.text || "",
            event: message.event || "",
            sessionId: message.sessionId || "",
            autoCloseMs,
            expiresAt: autoCloseMs ? Date.now() + autoCloseMs : null,
            createdAt: Date.now(),
        };
        state.notifications = [notification, ...state.notifications].slice(0, 3);
        const liveIds = new Set(state.notifications.map((item) => item.id));
        [...notificationAutoCloseTimers.keys()].forEach((id) => {
            if (!liveIds.has(id)) clearNotificationAutoClose(id);
        });
        scheduleNotificationAutoClose(notification);
        return notification;
    }

    function playSound(name) {
        if (state.muted || state.lowPower) return;
        const file = theme.sounds?.[name];
        if (!file || typeof Audio === "undefined") return;
        try {
            const audio = new Audio(joinPetAssetPath(state.assetsBaseUrl || theme.assetBaseUrl, file));
            audio.volume = 0.42;
            audio.play?.().catch?.(() => {});
        } catch {
            // Audio can be blocked by browser autoplay policies.
        }
    }

    function addAgentNotification(eventName, payload, mappedState) {
        if (!["notification", "error", "attention", "success"].includes(mappedState)) return;
        addNotification({
            type: mappedState === "error" ? "error" : mappedState === "notification" ? "warning" : "success",
            event: eventName,
            sessionId: payload.sessionId || payload.id || "",
            title: payload.title || eventName,
            text: payload.message || payload.text || "",
        });
    }

    function getPermissionBubblePolicy(payload = {}) {
        const seconds = normalizeAutoCloseSeconds(
            payload.permissionBubbleAutoCloseSeconds ?? payload.permission_bubble_auto_close_seconds,
            state.permissionBubbleAutoCloseSeconds,
        );
        const enabled = !state.hideBubbles
            && state.permissionBubblesEnabled
            && !state.doNotDisturb
            && payload.bubbleVisible !== false;
        return {
            enabled,
            autoCloseMs: seconds > 0 ? seconds * 1000 : 0,
        };
    }

    function clearPermissionAutoClose(id) {
        const timer = permissionAutoCloseTimers.get(id);
        if (timer) clearTimeout(timer);
        permissionAutoCloseTimers.delete(id);
    }

    function schedulePermissionAutoClose(request) {
        clearPermissionAutoClose(request.id);
        if (!request.bubbleVisible || request.status !== "pending" || !request.autoCloseMs) return;
        const timer = setTimeout(() => {
            permissionAutoCloseTimers.delete(request.id);
            const current = state.permissionRequests.find((item) => item.id === request.id);
            if (!current || current.status !== "pending") return;
            current.bubbleVisible = false;
            current.dismissedAt = Date.now();
            notify();
        }, request.autoCloseMs);
        permissionAutoCloseTimers.set(request.id, timer);
    }

    function clearDroppedPermissionTimers() {
        const liveIds = new Set(state.permissionRequests.map((request) => request.id));
        [...permissionAutoCloseTimers.keys()].forEach((id) => {
            if (!liveIds.has(id)) clearPermissionAutoClose(id);
        });
    }

    function addPermissionRequest(eventName, payload = {}) {
        const toolInput = payload.toolInput || payload.tool_input || null;
        const toolName = payload.toolName || payload.tool_name || "";
        const toolInputDescription = payload.toolInputDescription || payload.tool_input_description || formatToolInputDescription(toolName, toolInput);
        const policy = getPermissionBubblePolicy(payload);
        const request = {
            id: payload.permissionId || `${Date.now()}-${state.permissionRequests.length}`,
            sessionId: payload.sessionId || payload.id || "",
            title: payload.title || "权限确认",
            text: payload.message || payload.text || "任务需要你的确认后继续。",
            toolName,
            toolDisplayName: payload.toolDisplayName || payload.tool_display_name || formatToolDisplayName(toolName),
            toolInput,
            toolInputDescription,
            suggestions: normalizePermissionSuggestions(payload),
            event: eventName,
            status: "pending",
            decision: null,
            selectedSuggestion: null,
            bubbleVisible: policy.enabled,
            autoCloseMs: policy.autoCloseMs,
            expiresAt: policy.autoCloseMs ? Date.now() + policy.autoCloseMs : null,
            createdAt: Date.now(),
        };
        clearPermissionAutoClose(request.id);
        state.permissionRequests = [request, ...state.permissionRequests.filter((item) => item.id !== request.id)].slice(0, 3);
        clearDroppedPermissionTimers();
        schedulePermissionAutoClose(request);
        return request;
    }

    const api = {
        theme,
        subscribe(listener) {
            listeners.add(listener);
            listener(snapshot());
            return () => listeners.delete(listener);
        },
        emit(eventName, payload = {}) {
            resetRestingTimer();
            if (eventName === "summon" || eventName === "show") state.visible = true;
            if (eventName === "expand") {
                state.visible = true;
                state.expanded = true;
                state.dashboardOpen = false;
            }
            if (eventName === "collapse") state.expanded = false;
            if (eventName === "hide") {
                state.visible = false;
                state.expanded = false;
                state.chatOpen = false;
                state.dashboardOpen = false;
            }
            if (eventName === "mini-enter") state.mini = true;
            if (eventName === "mini-exit") state.mini = false;
            machine.emit(eventName, payload);
            notify();
            return api;
        },
        emitAgentEvent(eventName, payload = {}) {
            resetRestingTimer();
            const sessionId = payload.sessionId || payload.id;
            const mappedState = theme.eventMap[eventName] || eventName;
            addAgentNotification(eventName, payload, mappedState);
            if (eventName === "PermissionRequest") {
                addPermissionRequest(eventName, payload);
            }

            if (eventName === "SessionEnd") {
                if (sessionId) sessions.delete(sessionId);
                applyResolvedSessionState({ force: true });
                notify();
                return api;
            }

            if (sessionId) {
                const existing = sessions.get(sessionId);
                const nextSession = upsertSession(sessionId, {
                    ...payload,
                    state: mappedState,
                });
                nextSession.recentEvents = pushSessionEvent(existing || nextSession, eventName, mappedState);
                sessions.set(sessionId, nextSession);
                applyResolvedSessionState({ returnTo: "idle" });
            } else {
                machine.setState(mapMiniState(mappedState), { returnTo: "idle" });
            }
            notify();
            return api;
        },
        startSession(id, initialState = "thinking", meta = {}) {
            resetRestingTimer();
            const session = upsertSession(id, {
                ...meta,
                state: initialState,
            });
            session.recentEvents = pushSessionEvent(session, "SessionStart", initialState);
            sessions.set(session.id, session);
            applyResolvedSessionState();
            notify();
            return session.id;
        },
        updateSession(id, patch = {}) {
            if (!id) return api;
            resetRestingTimer();
            const existing = sessions.get(id);
            const session = upsertSession(id, {
                ...patch,
                state: patch.state || existing?.state || "working",
            });
            session.recentEvents = pushSessionEvent(existing || session, patch.event || "SessionUpdate", session.state);
            sessions.set(session.id, session);
            applyResolvedSessionState();
            notify();
            return api;
        },
        endSession(id) {
            if (id) sessions.delete(id);
            applyResolvedSessionState({ force: true });
            notify();
            return api;
        },
        hideSession(id) {
            if (!id || !sessions.has(id)) return api;
            const session = sessions.get(id);
            sessions.set(id, {
                ...session,
                hiddenFromDashboard: true,
                updatedAt: Date.now(),
            });
            applyResolvedSessionState({ force: true });
            notify();
            return api;
        },
        focusSession(id, options = {}) {
            if (!id || !sessions.has(id)) return null;
            const session = sessions.get(id);
            const requestedAt = Date.now();
            const next = {
                ...session,
                lastFocusedAt: requestedAt,
                focusHandoff: {
                    status: session.canFocus === false ? "unavailable" : "pending",
                    source: options.source || "dashboard",
                    requestedAt,
                    completedAt: null,
                    target: session.focusTarget || null,
                    reason: session.canFocus === false ? "focus-unavailable" : "",
                },
                updatedAt: requestedAt,
            };
            sessions.set(id, next);
            notify();
            return {
                ...next,
                badge: deriveSessionBadge(next),
            };
        },
        completeSessionFocus(id, result = {}) {
            if (!id || !sessions.has(id)) return null;
            const session = sessions.get(id);
            const normalized = normalizeFocusResult(result);
            const completedAt = Date.now();
            const next = {
                ...session,
                focusHandoff: {
                    ...(session.focusHandoff || {}),
                    ...normalized,
                    target: normalized.target || session.focusHandoff?.target || session.focusTarget || null,
                    completedAt,
                },
                updatedAt: completedAt,
            };
            sessions.set(id, next);
            notify();
            return {
                ...next,
                badge: deriveSessionBadge(next),
            };
        },
        renameSession(id, displayTitle = "") {
            if (!id || !sessions.has(id)) return null;
            const session = sessions.get(id);
            const title = sanitizeSessionAlias(displayTitle) || "";
            const aliasKey = sessionAliasKey(session);
            if (aliasKey && title) {
                state.sessionAliases[aliasKey] = {
                    title,
                    updatedAt: Date.now(),
                };
            } else if (aliasKey) {
                delete state.sessionAliases[aliasKey];
            }
            const next = {
                ...session,
                displayTitle: title,
                updatedAt: Date.now(),
            };
            sessions.set(id, next);
            notify();
            return {
                ...next,
                badge: deriveSessionBadge(next),
            };
        },
        acknowledgeSession(id) {
            if (!id || !sessions.has(id)) return null;
            const session = sessions.get(id);
            const next = {
                ...session,
                acknowledgedAt: Date.now(),
                updatedAt: Date.now(),
            };
            sessions.set(id, next);
            applyResolvedSessionState({ force: true });
            notify();
            return {
                ...next,
                badge: deriveSessionBadge(next),
            };
        },
        touch() {
            resetRestingTimer();
            return api;
        },
        setState(nextState, payload = {}) {
            resetRestingTimer();
            if (nextState !== "hidden") state.visible = true;
            if (nextState === "hidden") {
                state.visible = false;
                state.expanded = false;
                state.chatOpen = false;
                state.dashboardOpen = false;
            }
            machine.setState(nextState, payload);
            notify();
            return api;
        },
        show({ expand = false } = {}) {
            state.visible = true;
            if (expand) state.expanded = true;
            machine.emit("summon", { returnTo: expand ? "expanded" : "idle" });
            notify();
            return api;
        },
        hide() {
            return api.emit("hide", { force: true });
        },
        expand() {
            return api.emit("expand");
        },
        collapse() {
            return api.emit("collapse");
        },
        toggle() {
            if (!state.visible) return api.show({ expand: true });
            return state.expanded ? api.collapse() : api.expand();
        },
        setLanguage(lang) {
            state.lang = normalizeLang(lang);
            notify();
            return api;
        },
        setTheme(nextTheme, themeOptions = {}) {
            const normalized = normalizePetTheme(nextTheme);
            theme = normalized;
            api.theme = theme;
            machine.theme = theme;
            state.assetsBaseUrl = themeOptions.assetsBaseUrl || normalized.assetBaseUrl || "";
            state.themeTransition = "switching";
            state.reaction = null;
            clearTimeout(idleAnimationTimer);
            if (!theme.states[machine.currentState] && machine.currentState !== "hidden") {
                machine.setState("idle", { force: true });
            }
            if (state.mini && !theme.miniMode.supported) {
                state.mini = false;
                state.miniTransition = null;
            }
            setTimeout(() => {
                if (state.themeTransition === "switching") {
                    state.themeTransition = null;
                    notify();
                }
            }, themeOptions.transitionMs || 260);
            notify();
            return api;
        },
        openChat() {
            state.visible = true;
            state.expanded = state.shellMode !== "desktop";
            state.chatOpen = true;
            state.dashboardOpen = false;
            machine.emit("ask-ai", { returnTo: "expanded" });
            notify();
            return api;
        },
        closeChat() {
            state.chatOpen = false;
            notify();
            return api;
        },
        openDashboard() {
            state.visible = true;
            state.dashboardOpen = true;
            state.expanded = false;
            state.chatOpen = false;
            machine.setState(mapMiniState(resolveDominantSessionState()), { force: true });
            notify();
            return api;
        },
        closeDashboard() {
            state.dashboardOpen = false;
            notify();
            return api;
        },
        toggleDashboard() {
            return state.dashboardOpen ? api.closeDashboard() : api.openDashboard();
        },
        addNotification(message) {
            addNotification(message);
            notify();
            return api;
        },
        dismissNotification(id) {
            clearNotificationAutoClose(id);
            state.notifications = state.notifications.filter((item) => item.id !== id);
            notify();
            return api;
        },
        resolvePermission(id, decision = "approved") {
            const request = state.permissionRequests.find((item) => item.id === id);
            if (!request) return api;
            clearPermissionAutoClose(id);
            const suggestion = request.suggestions.find((item) => item.id === decision || item.behavior === decision);
            const resolvedDecision = suggestion?.behavior || decision;
            request.status = resolvedDecision === "denied" || resolvedDecision === "deny" || resolvedDecision === "rejected"
                ? "denied"
                : "approved";
            request.decision = resolvedDecision;
            request.selectedSuggestion = suggestion || null;
            request.bubbleVisible = false;
            request.resolvedAt = Date.now();
            if (request.sessionId) {
                const nextState = request.status === "approved" ? "working" : "error";
                api.updateSession(request.sessionId, {
                    state: nextState,
                    event: request.status === "approved" ? "PermissionApproved" : "PermissionDenied",
                    permissionDecision: request.decision,
                });
                applyResolvedSessionState({ force: true });
            }
            addNotification({
                type: request.status === "approved" ? "success" : "error",
                title: request.status === "approved" ? "已批准" : "已拒绝",
                text: request.selectedSuggestion?.label || request.toolName || request.title,
                sessionId: request.sessionId,
                force: true,
            });
            playSound(request.status === "approved" ? "confirm" : "error");
            setTimeout(() => {
                state.permissionRequests = state.permissionRequests.filter((item) => item.id !== id);
                notify();
            }, 900);
            notify();
            return api;
        },
        dismissPermission(id) {
            const request = state.permissionRequests.find((item) => item.id === id);
            if (!request) return api;
            clearPermissionAutoClose(id);
            request.bubbleVisible = false;
            request.dismissedAt = Date.now();
            notify();
            return api;
        },
        setMuted(nextMuted) {
            state.muted = !!nextMuted;
            notify();
            return api;
        },
        setLowPower(nextLowPower) {
            state.lowPower = !!nextLowPower;
            if (state.lowPower) {
                clearTimeout(idleAnimationTimer);
                state.reaction = null;
            }
            notify();
            return api;
        },
        setDoNotDisturb(nextEnabled) {
            state.doNotDisturb = !!nextEnabled;
            state.reaction = null;
            clearTimeout(idleAnimationTimer);
            state.permissionRequests = state.permissionRequests.map((request) => ({
                ...request,
                bubbleVisible: !state.hideBubbles && state.permissionBubblesEnabled && !state.doNotDisturb && request.status === "pending",
            }));
            state.permissionRequests.forEach(schedulePermissionAutoClose);
            if (state.doNotDisturb) {
                state.expanded = false;
                state.chatOpen = false;
                state.dashboardOpen = false;
                machine.setState(mapMiniState("resting"), { force: true });
            } else {
                applyResolvedSessionState({ force: true });
            }
            notify();
            return api;
        },
        setPermissionBubblesEnabled(nextEnabled) {
            state.permissionBubblesEnabled = !!nextEnabled;
            state.permissionRequests = state.permissionRequests.map((request) => ({
                ...request,
                bubbleVisible: !state.hideBubbles && state.permissionBubblesEnabled && !state.doNotDisturb && request.status === "pending",
            }));
            state.permissionRequests.forEach(schedulePermissionAutoClose);
            notify();
            return api;
        },
        setBubbleCategoryEnabled(category, enabled) {
            if (category === "permission") return api.setPermissionBubblesEnabled(!!enabled);
            if (category === "notification") return api.setNotificationBubbleAutoCloseSeconds(enabled ? DEFAULT_NOTIFICATION_AUTO_CLOSE_SECONDS : 0);
            if (category === "update") return api;
            return api;
        },
        setShellCapabilities(capabilities = {}) {
            if (capabilities.shellMode === "desktop" || capabilities.shellMode === "web") {
                state.shellMode = capabilities.shellMode;
            }
            state.shellCapabilities = {
                ...state.shellCapabilities,
                ...capabilities,
            };
            notify();
            return api;
        },
        getBubblePolicy(category) {
            const policy = buildBubblePolicySnapshot();
            return category ? policy[category] || null : policy;
        },
        setHideBubbles(nextHidden) {
            state.hideBubbles = !!nextHidden;
            if (state.hideBubbles) {
                notificationAutoCloseTimers.forEach((timer) => clearTimeout(timer));
                notificationAutoCloseTimers.clear();
                state.notifications = [];
            }
            state.permissionRequests = state.permissionRequests.map((request) => ({
                ...request,
                bubbleVisible: !state.hideBubbles && state.permissionBubblesEnabled && !state.doNotDisturb && request.status === "pending",
            }));
            state.permissionRequests.forEach(schedulePermissionAutoClose);
            notify();
            return api;
        },
        setNotificationBubbleAutoCloseSeconds(seconds) {
            state.notificationBubbleAutoCloseSeconds = normalizeAutoCloseSeconds(seconds, DEFAULT_NOTIFICATION_AUTO_CLOSE_SECONDS);
            notificationAutoCloseTimers.forEach((timer) => clearTimeout(timer));
            notificationAutoCloseTimers.clear();
            if (state.notificationBubbleAutoCloseSeconds === 0) {
                state.notifications = [];
            } else {
                const autoCloseMs = state.notificationBubbleAutoCloseSeconds * 1000;
                state.notifications = state.notifications.map((notification) => ({
                    ...notification,
                    autoCloseMs,
                    expiresAt: Date.now() + autoCloseMs,
                }));
                state.notifications.forEach(scheduleNotificationAutoClose);
            }
            notify();
            return api;
        },
        setPermissionBubbleAutoCloseSeconds(seconds) {
            state.permissionBubbleAutoCloseSeconds = normalizeAutoCloseSeconds(seconds, 0);
            state.permissionRequests = state.permissionRequests.map((request) => {
                const autoCloseMs = state.permissionBubbleAutoCloseSeconds > 0 ? state.permissionBubbleAutoCloseSeconds * 1000 : 0;
                return {
                    ...request,
                    autoCloseMs,
                    expiresAt: autoCloseMs && request.bubbleVisible ? Date.now() + autoCloseMs : null,
                };
            });
            state.permissionRequests.forEach(schedulePermissionAutoClose);
            notify();
            return api;
        },
        setSessionHudPinned(nextPinned) {
            state.sessionHudPinned = !!nextPinned;
            state.sessionHudRevealed = state.sessionHudPinned || state.sessionHudRevealed;
            notify();
            return api;
        },
        revealSessionHud() {
            state.sessionHudRevealed = true;
            notify();
            return api;
        },
        hideSessionHud() {
            if (state.sessionHudPinned) return api;
            state.sessionHudRevealed = false;
            notify();
            return api;
        },
        sleep() {
            return api.setDoNotDisturb(true);
        },
        wake() {
            return api.setDoNotDisturb(false);
        },
        enterMini(options = {}) {
            if (!theme.miniMode.supported) return api;
            const edge = options.edge === "left" ? "left" : "right";
            state.mini = true;
            state.miniEdge = edge;
            state.miniTransition = "entering";
            state.expanded = false;
            state.chatOpen = false;
            state.dashboardOpen = false;
            state.visible = true;
            machine.setState("mini-enter", { force: true });
            settleMiniTransition();
            setTimeout(() => {
                if (state.mini) {
                    machine.setState(mapMiniState(resolveDominantSessionState()), { force: true });
                    notify();
                }
            }, theme.miniMode.enterMs || 700);
            notify();
            return api;
        },
        exitMini() {
            if (!state.mini) return api;
            state.miniTransition = "exiting";
            state.mini = false;
            machine.setState(resolveDominantSessionState(), { force: true });
            settleMiniTransition();
            notify();
            return api;
        },
        setDragging(isDragging, payload = {}) {
            state.dragging = !!isDragging;
            state.position = payload.position || state.position;
            if (isDragging && payload.reaction !== false && theme.reactions.drag?.file) {
                state.reaction = payload.direction === "left" && theme.reactions.drag.fileLeft
                    ? theme.reactions.drag.fileLeft
                    : payload.direction === "right" && theme.reactions.drag.fileRight
                        ? theme.reactions.drag.fileRight
                        : theme.reactions.drag.file;
            } else if (!isDragging) {
                state.reaction = null;
            }
            notify();
            return api;
        },
        setPosition(position) {
            state.position = position;
            notify();
            return api;
        },
        playReaction(name) {
            const reaction = theme.reactions[name];
            const file = reaction?.file || reaction?.files?.[0];
            if (!file) return api;
            state.reaction = file;
            notify();
            setTimeout(() => {
                if (state.reaction === file) {
                    state.reaction = null;
                    notify();
                }
            }, reaction.duration || 2400);
            return api;
        },
        addChatMessage(message) {
            state.chatMessages.push({
                id: `${Date.now()}-${state.chatMessages.length}`,
                role: message.role || "assistant",
                text: message.text || "",
                streaming: !!message.streaming,
            });
            notify();
            return state.chatMessages[state.chatMessages.length - 1];
        },
        updateChatMessage(id, patch) {
            const target = state.chatMessages.find((message) => message.id === id);
            if (target) Object.assign(target, patch);
            notify();
            return api;
        },
        clearChat() {
            state.chatMessages = [];
            notify();
            return api;
        },
        getSnapshot: snapshot,
        destroy() {
            destroyed = true;
            clearTimeout(restingTimer);
            clearTimeout(idleAnimationTimer);
            clearTimeout(miniTransitionTimer);
            clearTimeout(staleCleanupTimer);
            permissionAutoCloseTimers.forEach((timer) => clearTimeout(timer));
            permissionAutoCloseTimers.clear();
            notificationAutoCloseTimers.forEach((timer) => clearTimeout(timer));
            notificationAutoCloseTimers.clear();
            machine.destroy();
            listeners.clear();
        },
    };

    resetRestingTimer();
    resetStaleCleanupTimer();
    return api;
}
