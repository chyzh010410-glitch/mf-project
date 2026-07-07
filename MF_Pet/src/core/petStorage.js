const STORAGE_VERSION = 9;

export function readPetStorage(storageKey) {
    try {
        const parsed = JSON.parse(localStorage.getItem(storageKey) || "null");
        return parsed?.version === STORAGE_VERSION ? parsed : null;
    } catch {
        return null;
    }
}

export function writePetStorage(storageKey, data) {
    try {
        localStorage.setItem(
            storageKey,
            JSON.stringify({
                version: STORAGE_VERSION,
                visible: !!data.visible,
                expanded: !!data.expanded,
                chatOpen: !!data.chatOpen,
                state: data.state,
                themeId: data.themeId,
                muted: !!data.muted,
                lowPower: !!data.lowPower,
                doNotDisturb: !!data.doNotDisturb,
                hideBubbles: !!data.hideBubbles,
                permissionBubblesEnabled: data.permissionBubblesEnabled !== false,
                permissionBubbleAutoCloseSeconds: Number.isFinite(Number(data.permissionBubbleAutoCloseSeconds))
                    ? Math.max(0, Math.trunc(Number(data.permissionBubbleAutoCloseSeconds)))
                    : 0,
                notificationBubbleAutoCloseSeconds: Number.isFinite(Number(data.notificationBubbleAutoCloseSeconds))
                    ? Math.max(0, Math.trunc(Number(data.notificationBubbleAutoCloseSeconds)))
                    : 6,
                sessionHudPinned: !!data.sessionHudPinned,
                sessionAliases: data.sessionAliases && typeof data.sessionAliases === "object" && !Array.isArray(data.sessionAliases)
                    ? data.sessionAliases
                    : {},
                mini: !!data.mini,
                miniEdge: data.miniEdge || "right",
                position: data.position || null,
            }),
        );
    } catch {
        // localStorage can be unavailable in private or embedded contexts.
    }
}
