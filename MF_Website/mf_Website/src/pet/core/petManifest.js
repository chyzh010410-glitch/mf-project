export const PET_ASSET_BASE_URL = "/assets/pet";

export const PET_STATES = {
    hidden: {
        asset: null,
        label: {
            zh: "隐藏",
            en: "Hidden",
        },
    },
    appearing: {
        asset: "move/move.gif",
        label: {
            zh: "出现",
            en: "Appearing",
        },
    },
    idle: {
        asset: "idle/idle.gif",
        label: {
            zh: "待机",
            en: "Idle",
        },
    },
    expanded: {
        asset: "idle/idle.gif",
        label: {
            zh: "展开",
            en: "Expanded",
        },
    },
    working: {
        asset: "work/work.gif",
        label: {
            zh: "工作中",
            en: "Working",
        },
    },
    success: {
        asset: "watering/watering.gif",
        label: {
            zh: "积极反馈",
            en: "Success",
        },
    },
    unclear: {
        asset: "doubt/doubt.gif",
        label: {
            zh: "疑惑",
            en: "Unclear",
        },
    },
    error: {
        asset: "sad/sad.gif",
        label: {
            zh: "错误",
            en: "Error",
        },
    },
    resting: {
        asset: "rest/rest.gif",
        label: {
            zh: "休息",
            en: "Resting",
        },
    },
};

export const PET_TEXT = {
    zh: {
        title: "你好，我是苗丰精灵。",
        subtitle: "想先做什么？",
        toggleLabel: "打开或收起苗丰精灵",
        closeLabel: "收起苗丰精灵面板",
        actions: {
            mall: "进入商城",
            encyclopedia: "查看树木百科",
            ai: "问苗丰精灵",
            merchant: "商家入驻",
        },
    },
    en: {
        title: "Hi, I am the MF Forest Pet.",
        subtitle: "What would you like to do first?",
        toggleLabel: "Open or collapse the MF Forest Pet",
        closeLabel: "Collapse the MF Forest Pet panel",
        actions: {
            mall: "Enter Mall",
            encyclopedia: "Tree Encyclopedia",
            ai: "Ask MF Pet",
            merchant: "Merchant Entry",
        },
    },
};

export const PET_LINKS = {
    mall: "#",
    encyclopedia: "#",
    ai: "#",
    merchant: "#",
};

export const PET_ACTIONS = ["mall", "encyclopedia", "ai", "merchant"];

export const DEFAULT_PET_OPTIONS = {
    assetsBaseUrl: PET_ASSET_BASE_URL,
    lang: "zh",
    links: PET_LINKS,
    storageKey: "mf-forest-pet",
    initialState: "idle",
    restore: true,
    initiallyVisible: false,
    initiallyExpanded: false,
};
