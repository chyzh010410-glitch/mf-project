import { normalizePetTheme } from "./petThemeLoader.js";
import { validatePetTheme } from "./petThemeValidator.js";

export function parsePetThemeJson(source) {
    const raw = typeof source === "string" ? JSON.parse(source) : source;
    const validation = validatePetTheme(raw);
    if (validation.errors.length) {
        throw new Error(`MF_Pet theme JSON validation failed: ${validation.errors.join("; ")}`);
    }
    return normalizePetTheme(raw);
}

export async function loadPetThemeFromUrl(url, options = {}) {
    const response = await fetch(url, {
        cache: options.cache || "no-cache",
    });
    if (!response.ok) {
        throw new Error(`Failed to load MF_Pet theme JSON: ${response.status} ${response.statusText}`);
    }
    const theme = parsePetThemeJson(await response.json());
    if (options.assetBaseUrl) {
        return {
            ...theme,
            assetBaseUrl: options.assetBaseUrl,
        };
    }
    return theme;
}

export function exportPetThemeJson(theme) {
    const normalized = normalizePetTheme(theme);
    const {
        capabilities,
        statePriority,
        eventMap,
        ...portable
    } = normalized;
    return JSON.stringify(portable, null, 2);
}
