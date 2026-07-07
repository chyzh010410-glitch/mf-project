import { normalizePetTheme } from "./petThemeLoader.js";
import { loadPetThemeFromUrl } from "./petThemeJson.js";

export function createPetThemeRegistry(initialThemes = []) {
    const themes = new Map();

    function register(theme) {
        const normalized = normalizePetTheme(theme);
        themes.set(normalized.id, normalized);
        return normalized;
    }

    initialThemes.forEach(register);

    return {
        register,
        async load(id, url, options = {}) {
            const theme = await loadPetThemeFromUrl(url, options);
            const registered = register({
                ...theme,
                id: id || theme.id,
            });
            return registered;
        },
        get(id) {
            return themes.get(id) || null;
        },
        has(id) {
            return themes.has(id);
        },
        list() {
            return [...themes.values()].map((theme) => ({
                id: theme.id,
                name: theme.name,
                version: theme.version,
                capabilities: { ...theme.capabilities },
            }));
        },
    };
}
