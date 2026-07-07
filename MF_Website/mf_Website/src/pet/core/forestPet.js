import {
    DEFAULT_PET_OPTIONS,
    PET_ACTIONS,
    PET_LINKS,
    PET_STATES,
    PET_TEXT,
} from "./petManifest.js";

const STORAGE_VERSION = 1;

function normalizeLang(lang) {
    return lang === "en" ? "en" : "zh";
}

function joinAssetPath(baseUrl, asset) {
    if (!asset) return "";
    return `${baseUrl.replace(/\/$/, "")}/${asset}`;
}

function readStorage(storageKey) {
    try {
        const parsed = JSON.parse(localStorage.getItem(storageKey) || "null");
        return parsed?.version === STORAGE_VERSION ? parsed : null;
    } catch {
        return null;
    }
}

function writeStorage(storageKey, data) {
    try {
        localStorage.setItem(
            storageKey,
            JSON.stringify({
                version: STORAGE_VERSION,
                visible: data.visible,
                expanded: data.expanded,
                state: data.state,
            }),
        );
    } catch {
        // localStorage can be unavailable in private or embedded contexts.
    }
}

function navigateTo(link) {
    if (!link || link === "#") return;
    window.location.href = link;
}

function createElement(tag, className, attributes = {}) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    Object.entries(attributes).forEach(([key, value]) => {
        if (value === undefined || value === null) return;
        element.setAttribute(key, value);
    });
    return element;
}

export function initForestPet(options = {}) {
    const config = {
        ...DEFAULT_PET_OPTIONS,
        ...options,
        links: {
            ...PET_LINKS,
            ...(options.links || {}),
        },
    };

    const mount = config.mount || document.body;
    const saved = config.restore ? readStorage(config.storageKey) : null;
    const state = {
        lang: normalizeLang(config.lang),
        visible: saved?.visible ?? config.initiallyVisible,
        expanded: saved?.expanded ?? config.initiallyExpanded,
        petState: saved?.state || config.initialState,
    };

    const root = createElement("div", "mf-pet", {
        "data-state": state.petState,
        "data-visible": String(state.visible),
        "data-expanded": String(state.expanded),
    });
    const toggle = createElement("button", "mf-pet__sprite-button", {
        type: "button",
    });
    const sprite = createElement("img", "mf-pet__sprite", {
        alt: "",
        draggable: "false",
    });
    const panel = createElement("section", "mf-pet__panel", {
        "aria-live": "polite",
    });
    const panelText = createElement("div", "mf-pet__panel-text");
    const title = createElement("p", "mf-pet__title");
    const subtitle = createElement("p", "mf-pet__subtitle");
    const actions = createElement("div", "mf-pet__actions");
    const close = createElement("button", "mf-pet__close", {
        type: "button",
    });

    close.textContent = "×";
    panelText.append(title, subtitle);
    panel.append(close, panelText, actions);
    toggle.append(sprite);
    root.append(toggle, panel);
    mount.append(root);

    const actionButtons = new Map();
    PET_ACTIONS.forEach((action) => {
        const button = createElement("button", "mf-pet__action", {
            type: "button",
            "data-pet-action": action,
        });
        button.addEventListener("click", (event) => {
            if (action === "ai") {
                api.setState("working");
            } else if (action === "encyclopedia") {
                api.setState("success");
            }

            const payload = {
                action,
                link: config.links[action],
                api,
                event,
            };
            const handled = config.onAction?.(payload);
            if (handled === true) return;

            if (action === "ai" && config.onAskAi) {
                config.onAskAi(payload);
                return;
            }

            navigateTo(config.links[action]);
        });
        actionButtons.set(action, button);
        actions.append(button);
    });

    function persist() {
        writeStorage(config.storageKey, {
            visible: state.visible,
            expanded: state.expanded,
            state: state.petState,
        });
    }

    function renderText() {
        const text = PET_TEXT[state.lang];
        title.textContent = text.title;
        subtitle.textContent = text.subtitle;
        toggle.setAttribute("aria-label", text.toggleLabel);
        close.setAttribute("aria-label", text.closeLabel);
        PET_ACTIONS.forEach((action) => {
            actionButtons.get(action).textContent = text.actions[action];
        });
    }

    function renderState() {
        const currentState = PET_STATES[state.petState] || PET_STATES.idle;
        root.dataset.state = state.petState;
        root.dataset.visible = String(state.visible);
        root.dataset.expanded = String(state.expanded);
        panel.hidden = !state.expanded;
        sprite.src = joinAssetPath(config.assetsBaseUrl, currentState.asset);
        sprite.alt = currentState.label[state.lang];
    }

    function render() {
        renderText();
        renderState();
        persist();
    }

    const api = {
        root,
        show({ expand = false } = {}) {
            state.visible = true;
            state.petState = "appearing";
            if (expand) state.expanded = true;
            render();
            window.setTimeout(() => {
                if (state.petState === "appearing") {
                    state.petState = state.expanded ? "expanded" : "idle";
                    render();
                }
            }, 420);
            return api;
        },
        hide() {
            state.visible = false;
            state.expanded = false;
            state.petState = "hidden";
            render();
            return api;
        },
        expand() {
            state.visible = true;
            state.expanded = true;
            state.petState = "expanded";
            render();
            return api;
        },
        collapse() {
            state.expanded = false;
            state.petState = "idle";
            render();
            return api;
        },
        toggle() {
            if (!state.visible) return api.show({ expand: true });
            return state.expanded ? api.collapse() : api.expand();
        },
        setState(nextState) {
            if (!PET_STATES[nextState]) return api;
            state.petState = nextState;
            if (nextState !== "hidden") state.visible = true;
            if (nextState === "expanded") state.expanded = true;
            if (nextState === "hidden") state.expanded = false;
            render();
            return api;
        },
        setLanguage(nextLang) {
            state.lang = normalizeLang(nextLang);
            render();
            return api;
        },
        getSnapshot() {
            return {
                visible: state.visible,
                expanded: state.expanded,
                state: state.petState,
                lang: state.lang,
            };
        },
        destroy() {
            root.remove();
        },
    };

    toggle.addEventListener("click", () => {
        api.toggle();
    });
    close.addEventListener("click", () => {
        api.collapse();
    });

    render();
    return api;
}

export { PET_ACTIONS, PET_LINKS, PET_STATES, PET_TEXT };
