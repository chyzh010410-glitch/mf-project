function stripTrailingSlash(value) {
    return String(value || "").replace(/\/$/, "");
}

export function joinPetAssetPath(baseUrl, file) {
    if (!file) return "";
    if (/^(https?:|data:|blob:|file:)/.test(file)) return file;
    return `${stripTrailingSlash(baseUrl)}/${file.replace(/^\//, "")}`;
}

export function pickStateFile(files, random = Math.random) {
    if (!Array.isArray(files) || files.length === 0) return null;
    return files[Math.floor(random() * files.length)];
}

function normalizeSessionsIterable(sessions) {
    if (!sessions) return [];
    if (sessions instanceof Map) return sessions.entries();
    if (typeof sessions[Symbol.iterator] === "function") return sessions;
    return [];
}

function countActiveSessions(sessions, states) {
    let count = 0;
    for (const [, session] of normalizeSessionsIterable(sessions)) {
        if (session?.headless) continue;
        if (states.has(session?.state)) count += 1;
    }
    return count;
}

function selectTieredFile(tiers, count) {
    if (!Array.isArray(tiers)) return null;
    const tier = tiers.find((entry) => count >= entry.minSessions);
    return tier?.file || null;
}

function getWinningDisplayHint(sessions, state, displayHintMap = {}) {
    let winner = null;
    for (const [, session] of normalizeSessionsIterable(sessions)) {
        if (session?.headless || session?.state !== state || !session.displayHint) continue;
        if (!winner || session.updatedAt >= winner.updatedAt) winner = session;
    }
    if (!winner) return null;
    return displayHintMap[winner.displayHint] || null;
}

export function resolveVisualFile(theme, state, options = {}) {
    const sessionCount = options.sessionCount
        ?? countActiveSessions(options.sessions, new Set(["working", "thinking", "juggling"]));
    if (state === "working") {
        const hinted = getWinningDisplayHint(options.sessions, "working", theme.displayHintMap);
        if (hinted) return hinted;
        const tiered = selectTieredFile(theme.workingTiers, sessionCount);
        if (tiered) return tiered;
    }
    if (state === "juggling") {
        const jugglingCount = options.jugglingSessionCount
            ?? countActiveSessions(options.sessions, new Set(["juggling"]));
        const hinted = getWinningDisplayHint(options.sessions, "juggling", theme.displayHintMap);
        if (hinted) return hinted;
        const tiered = selectTieredFile(theme.jugglingTiers, jugglingCount);
        if (tiered) return tiered;
    }
    if (state === "thinking") {
        const hinted = getWinningDisplayHint(options.sessions, "thinking", theme.displayHintMap);
        if (hinted) return hinted;
    }

    let cursor = state;
    const visited = new Set();
    const pickFile = options.pickFile || pickStateFile;

    for (let hops = 0; hops < 5; hops += 1) {
        if (!cursor || visited.has(cursor)) break;
        visited.add(cursor);
        const binding = theme.states[cursor];
        if (binding?.files?.length) return pickFile(binding.files);
        cursor = binding?.fallbackTo;
    }

    return pickFile(theme.states.idle?.files || []);
}

export function resolveVisualAsset(theme, state, options = {}) {
    const file = resolveVisualFile(theme, state, options);
    const baseUrl = options.assetsBaseUrl || theme.assetBaseUrl || "";
    return {
        file,
        src: joinPetAssetPath(baseUrl, file),
    };
}
