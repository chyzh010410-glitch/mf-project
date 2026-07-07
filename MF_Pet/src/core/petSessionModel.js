const RECENT_EVENT_LIMIT = 8;
const DONE_EVENTS = new Set(["Stop", "PostCompact", "PermissionApproved", "SessionEnd"]);
const BLOCKED_EVENTS = new Set(["PermissionRequest"]);
const ERROR_EVENTS = new Set(["PostToolUseFailure", "StopFailure", "PermissionDenied"]);
const RUNNING_STATES = new Set(["thinking", "working", "juggling", "carrying", "sweeping", "notification"]);

function clampPercent(value) {
    return Math.max(0, Math.min(100, Math.round(value)));
}

export function normalizeContextUsage(input) {
    if (!input || typeof input !== "object") return null;
    const used = Number(input.used ?? input.usedTokens);
    const rawLimit = input.limit ?? input.max ?? input.total;
    const limit = rawLimit === undefined || rawLimit === null ? null : Number(rawLimit);
    const explicitPercent = input.percent === undefined || input.percent === null ? null : Number(input.percent);

    if (!Number.isFinite(used) || used < 0) return null;
    if (limit !== null && (!Number.isFinite(limit) || limit <= 0)) return null;

    const percent = limit
        ? clampPercent((used / limit) * 100)
        : Number.isFinite(explicitPercent)
            ? clampPercent(explicitPercent)
            : null;

    return {
        used: Math.round(used),
        limit: limit ? Math.round(limit) : null,
        percent,
        source: typeof input.source === "string" ? input.source : "",
    };
}

export function normalizeQuotaUsage(input) {
    if (!input || typeof input !== "object") return null;
    const rawPercent = input.usedPercent ?? input.percent;
    const usedPercent = Number(rawPercent);
    const remaining = input.remaining === undefined || input.remaining === null ? null : Number(input.remaining);
    const limit = input.limit === undefined || input.limit === null ? null : Number(input.limit);
    const resetAt = input.resetAt ?? input.reset_at;

    if (!Number.isFinite(usedPercent) && !Number.isFinite(remaining) && !Number.isFinite(limit)) return null;
    if (Number.isFinite(remaining) && remaining < 0) return null;
    if (Number.isFinite(limit) && limit <= 0) return null;

    const derivedPercent = Number.isFinite(usedPercent)
        ? usedPercent
        : Number.isFinite(remaining) && Number.isFinite(limit)
            ? ((limit - remaining) / limit) * 100
            : null;

    return {
        label: firstString(input.label, input.name) || "Quota",
        usedPercent: Number.isFinite(derivedPercent) ? clampPercent(derivedPercent) : null,
        remaining: Number.isFinite(remaining) ? Math.round(remaining) : null,
        limit: Number.isFinite(limit) ? Math.round(limit) : null,
        resetAt: Number.isFinite(Number(resetAt)) ? Number(resetAt) : null,
        source: firstString(input.source),
    };
}

function firstString(...values) {
    const value = values.find((item) => typeof item === "string" && item.trim());
    return value ? value.trim() : "";
}

function firstNumber(...values) {
    const value = values.find((item) => Number.isFinite(Number(item)));
    return value === undefined ? null : Number(value);
}

function firstCssColor(...values) {
    const value = firstString(...values);
    if (/^#[0-9a-f]{3,8}$/i.test(value) || /^rgba?\([^)]+\)$/i.test(value)) return value;
    return "";
}

export function normalizeSessionMeta(input = {}) {
    return {
        agentId: firstString(input.agentId, input.agent_id),
        agentLabel: firstString(input.agentLabel, input.agent_label, input.agentName, input.agent_name),
        agentIcon: firstString(input.agentIcon, input.agent_icon, input.icon),
        agentColor: firstCssColor(input.agentColor, input.agent_color),
        provider: firstString(input.provider),
        model: firstString(input.model),
        cwd: firstString(input.cwd, input.workspace, input.workspacePath, input.workspace_path),
        host: firstString(input.host, input.hostname, input.machine),
        platform: firstString(input.platform, input.osPlatform, input.os_platform),
        editor: firstString(input.editor),
        hookSource: firstString(input.hookSource, input.hook_source),
        sourcePid: firstNumber(input.sourcePid, input.source_pid),
        turnId: firstString(input.turnId, input.turn_id),
    };
}

export function pushSessionEvent(existing, eventName, state, now = Date.now()) {
    const previous = Array.isArray(existing?.recentEvents)
        ? existing.recentEvents.slice(-(RECENT_EVENT_LIMIT - 1))
        : [];
    previous.push({
        at: now,
        event: eventName || null,
        state: state || "idle",
    });
    return previous;
}

export function deriveSessionBadge(session, now = Date.now()) {
    const recent = Array.isArray(session?.recentEvents) ? session.recentEvents : [];
    const last = recent[recent.length - 1];
    const staleMs = Number.isFinite(session?.staleMs) ? session.staleMs : 120000;
    const updatedAt = session?.updatedAt || session?.createdAt || now;
    const acknowledgedAt = session?.acknowledgedAt || 0;

    if (acknowledgedAt && acknowledgedAt >= updatedAt) return "idle";

    if (last && ERROR_EVENTS.has(last.event)) return "blocked";
    if (last && BLOCKED_EVENTS.has(last.event)) return "waiting";
    if (last && DONE_EVENTS.has(last.event)) return "done";
    if (session?.state && RUNNING_STATES.has(session.state)) return "running";
    if (now - updatedAt > staleMs) return "stale";
    return "idle";
}

export function shouldPruneSession(session, now = Date.now()) {
    if (!session) return true;
    if (session.pinned || session.state === "notification") return false;
    const badge = deriveSessionBadge(session, now);
    const pruneAfterMs = Number.isFinite(session.pruneAfterMs) ? session.pruneAfterMs : 90000;
    const updatedAt = session.updatedAt || session.createdAt || now;
    return ["done", "idle", "stale"].includes(badge) && now - updatedAt > pruneAfterMs;
}
