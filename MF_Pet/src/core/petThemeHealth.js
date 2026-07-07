import { normalizePetTheme } from "./petThemeLoader.js";
import { joinPetAssetPath } from "./petVisualResolver.js";

function addFile(files, file) {
    if (typeof file === "string" && file) files.add(file);
}

function collectThemeFiles(theme) {
    const files = new Set();
    Object.values(theme.states || {}).forEach((binding) => {
        (binding.files || []).forEach((file) => addFile(files, file));
    });
    (theme.workingTiers || []).forEach((tier) => addFile(files, tier.file));
    (theme.jugglingTiers || []).forEach((tier) => addFile(files, tier.file));
    (theme.idleAnimations || []).forEach((animation) => addFile(files, animation.file));
    Object.values(theme.reactions || {}).forEach((reaction) => {
        addFile(files, reaction.file);
        addFile(files, reaction.fileLeft);
        addFile(files, reaction.fileRight);
        (reaction.files || []).forEach((file) => addFile(files, file));
    });
    Object.values(theme.displayHintMap || {}).forEach((file) => addFile(files, file));
    Object.values(theme.sounds || {}).forEach((file) => addFile(files, file));
    return [...files];
}

export function listPetThemeAssets(theme, options = {}) {
    const normalized = normalizePetTheme(theme);
    const baseUrl = options.assetsBaseUrl || normalized.assetBaseUrl || "";
    return collectThemeFiles(normalized).map((file) => ({
        file,
        src: joinPetAssetPath(baseUrl, file),
    }));
}

export async function checkPetThemeAssets(theme, options = {}) {
    const assets = listPetThemeAssets(theme, options);
    const fetchFn = options.fetch || globalThis.fetch;
    if (typeof fetchFn !== "function") {
        return {
            ok: false,
            checked: 0,
            missing: assets,
            reason: "fetch unavailable",
        };
    }

    const results = await Promise.all(assets.map(async (asset) => {
        try {
            const response = await fetchFn(asset.src, {
                method: options.method || "HEAD",
                cache: options.cache || "no-cache",
            });
            return {
                ...asset,
                ok: !!response.ok,
                status: response.status,
            };
        } catch (error) {
            return {
                ...asset,
                ok: false,
                error: error?.message || "request failed",
            };
        }
    }));
    const missing = results.filter((item) => !item.ok);
    return {
        ok: missing.length === 0,
        checked: results.length,
        missing,
        results,
    };
}
