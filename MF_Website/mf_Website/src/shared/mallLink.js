const MALL_URL_ENV_KEY = "VITE_MF_EP_CLIENT_URL";
const mallUrlEnv = import.meta.env.VITE_MF_EP_CLIENT_URL;

function getMallUrl() {
    const value = mallUrlEnv?.trim();

    if (!value) {
        console.error(`[MF Website] Missing ${MALL_URL_ENV_KEY}; mall navigation is unavailable.`);
        return null;
    }

    try {
        const url = new URL(value);
        if (url.protocol === "http:" || url.protocol === "https:") {
            return url.href;
        }
    } catch {
        // The error below gives developers the actionable configuration issue.
    }

    console.error(`[MF Website] Invalid ${MALL_URL_ENV_KEY}: expected an http or https URL.`);
    return null;
}

export function navigateToMall() {
    const mallUrl = getMallUrl();
    if (!mallUrl) return false;

    window.open(mallUrl, "_blank", "noopener,noreferrer");
    return true;
}

export function bindMallEntrypoints() {
    document.querySelectorAll("[data-mall-link]").forEach((entrypoint) => {
        entrypoint.addEventListener("click", (event) => {
            event.preventDefault();
            navigateToMall();
        });
    });
}
