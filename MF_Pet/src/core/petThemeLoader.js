import { validatePetTheme } from "./petThemeValidator.js";

const DEFAULT_TIMINGS = {
    minDisplay: {
        appearing: 600,
        working: 1000,
        success: 1800,
        unclear: 1800,
        error: 2500,
    },
    autoReturn: {
        appearing: 700,
        success: 2200,
        unclear: 2600,
        error: 3200,
    },
    restingAfter: 60000,
};

const DEFAULT_STATE_PRIORITY = {
    error: 8,
    unclear: 7,
    notification: 7,
    sweeping: 6,
    success: 6,
    attention: 5,
    carrying: 4,
    juggling: 4,
    working: 4,
    thinking: 3,
    expanded: 3,
    appearing: 2,
    idle: 1,
    resting: 0,
    hidden: -1,
};

const DEFAULT_EVENT_MAP = {
    summon: "appearing",
    show: "appearing",
    expand: "expanded",
    collapse: "idle",
    hide: "hidden",
    idle: "idle",
    rest: "resting",
    "ask-ai": "working",
    "ai-working": "working",
    "ai-success": "success",
    "ai-unclear": "unclear",
    "ai-error": "error",
    notify: "notification",
    attention: "attention",
    sweeping: "sweeping",
    carrying: "carrying",
    juggling: "juggling",
    thinking: "thinking",
    "panel-success": "success",
    SessionStart: "idle",
    SessionEnd: "idle",
    UserPromptSubmit: "thinking",
    PreToolUse: "working",
    PostToolUse: "working",
    PostToolUseFailure: "error",
    Stop: "attention",
    StopFailure: "error",
    SubagentStart: "juggling",
    SubagentStop: "working",
    PreCompact: "sweeping",
    PostCompact: "attention",
    Notification: "notification",
    PermissionRequest: "notification",
    WorktreeCreate: "carrying",
};

function isPlainObject(value) {
    return value && typeof value === "object" && !Array.isArray(value);
}

function normalizeStateBinding(binding) {
    if (Array.isArray(binding)) {
        return { files: [...binding], fallbackTo: null };
    }
    if (isPlainObject(binding)) {
        return {
            files: Array.isArray(binding.files) ? [...binding.files] : [],
            fallbackTo: typeof binding.fallbackTo === "string" ? binding.fallbackTo : null,
        };
    }
    return { files: [], fallbackTo: null };
}

function mergeTimingGroup(defaults, override) {
    return {
        ...defaults,
        ...(isPlainObject(override) ? override : {}),
    };
}

export function normalizePetTheme(theme) {
    if (!isPlainObject(theme)) {
        throw new Error("MF_Pet theme must be an object");
    }
    const validation = validatePetTheme(theme);
    if (validation.errors.length) {
        throw new Error(`MF_Pet theme validation failed: ${validation.errors.join("; ")}`);
    }
    if (theme.schemaVersion !== 1) {
        throw new Error(`MF_Pet theme schemaVersion must be 1, got ${theme.schemaVersion}`);
    }
    if (!theme.id) throw new Error("MF_Pet theme missing id");
    if (!theme.states || !theme.states.idle) throw new Error("MF_Pet theme requires states.idle");

    const states = {};
    Object.entries(theme.states).forEach(([state, binding]) => {
        states[state] = normalizeStateBinding(binding);
    });
    if (isPlainObject(theme.miniMode?.states)) {
        Object.entries(theme.miniMode.states).forEach(([state, binding]) => {
            states[state] = normalizeStateBinding(binding);
        });
    }

    const timings = {
        ...DEFAULT_TIMINGS,
        ...(isPlainObject(theme.timings) ? theme.timings : {}),
        minDisplay: mergeTimingGroup(DEFAULT_TIMINGS.minDisplay, theme.timings?.minDisplay),
        autoReturn: mergeTimingGroup(DEFAULT_TIMINGS.autoReturn, theme.timings?.autoReturn),
    };

    return {
        ...theme,
        states,
        timings,
        statePriority: {
            ...DEFAULT_STATE_PRIORITY,
            ...(isPlainObject(theme.statePriority) ? theme.statePriority : {}),
        },
        eventMap: {
            ...DEFAULT_EVENT_MAP,
            ...(isPlainObject(theme.eventMap) ? theme.eventMap : {}),
        },
        ui: {
            spriteSize: 96,
            panelWidth: 312,
            ...(isPlainObject(theme.ui) ? theme.ui : {}),
        },
        miniMode: {
            supported: false,
            states: {},
            ...(isPlainObject(theme.miniMode) ? theme.miniMode : {}),
        },
        sounds: isPlainObject(theme.sounds) ? theme.sounds : {},
        reactions: isPlainObject(theme.reactions) ? theme.reactions : {},
        displayHintMap: isPlainObject(theme.displayHintMap) ? theme.displayHintMap : {},
        idleAnimations: Array.isArray(theme.idleAnimations) ? theme.idleAnimations : [],
        workingTiers: Array.isArray(theme.workingTiers)
            ? [...theme.workingTiers].sort((a, b) => b.minSessions - a.minSessions)
            : [],
        jugglingTiers: Array.isArray(theme.jugglingTiers)
            ? [...theme.jugglingTiers].sort((a, b) => b.minSessions - a.minSessions)
            : [],
        capabilities: {
            miniMode: !!theme.miniMode?.supported,
            idleAnimations: Array.isArray(theme.idleAnimations) && theme.idleAnimations.length > 0,
            reactions: isPlainObject(theme.reactions) && Object.keys(theme.reactions).length > 0,
            workingTiers: Array.isArray(theme.workingTiers) && theme.workingTiers.length > 0,
            jugglingTiers: Array.isArray(theme.jugglingTiers) && theme.jugglingTiers.length > 0,
        },
    };
}

export { DEFAULT_EVENT_MAP, DEFAULT_STATE_PRIORITY, DEFAULT_TIMINGS };
