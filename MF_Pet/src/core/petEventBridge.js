const EVENT_PRESETS = {
    websiteAsk: {
        event: "UserPromptSubmit",
        title: "官网问答",
        state: "thinking",
    },
    websiteAnswered: {
        event: "Stop",
        title: "官网问答完成",
    },
    mallRecommend: {
        event: "PreToolUse",
        title: "商城推荐",
        state: "working",
    },
    mallDone: {
        event: "Stop",
        title: "商城推荐完成",
    },
    aiToolCall: {
        event: "PreToolUse",
        title: "AI 工具调用",
        state: "working",
    },
    aiToolDone: {
        event: "PostToolUse",
        title: "AI 工具完成",
        state: "working",
    },
    aiToolFailed: {
        event: "PostToolUseFailure",
        title: "AI 工具失败",
    },
    aiPermission: {
        event: "PermissionRequest",
        title: "AI 权限确认",
    },
};

export function createPetEventBridge(runtime, options = {}) {
    const namespace = options.namespace || "mf";

    function sessionId(scope, id) {
        return id || `${namespace}:${scope}`;
    }

    function emitPreset(name, payload = {}) {
        const preset = EVENT_PRESETS[name];
        if (!preset) throw new Error(`Unknown MF_Pet event preset: ${name}`);
        const id = sessionId(name, payload.sessionId || payload.id);
        if (preset.state) {
            runtime.startSession(id, preset.state, {
                title: payload.title || preset.title,
                displayHint: payload.displayHint,
                source: payload.source || name,
            });
        }
        runtime.emitAgentEvent(preset.event, {
            ...payload,
            sessionId: id,
            title: payload.title || preset.title,
            message: payload.message,
        });
        return id;
    }

    return {
        emitPreset,
        websiteAsk(payload) {
            return emitPreset("websiteAsk", payload);
        },
        websiteAnswered(payload) {
            return emitPreset("websiteAnswered", payload);
        },
        mallRecommend(payload) {
            return emitPreset("mallRecommend", payload);
        },
        mallDone(payload) {
            return emitPreset("mallDone", payload);
        },
        aiToolCall(payload) {
            return emitPreset("aiToolCall", payload);
        },
        aiToolDone(payload) {
            return emitPreset("aiToolDone", payload);
        },
        aiToolFailed(payload) {
            return emitPreset("aiToolFailed", payload);
        },
        aiPermission(payload) {
            return emitPreset("aiPermission", payload);
        },
    };
}

export { EVENT_PRESETS };
