const REQUIRED_STATES = ["idle"];
const RECOMMENDED_STATES = ["working", "thinking", "resting", "error"];
const MINI_REQUIRED_STATES = ["mini-idle", "mini-peek", "mini-alert", "mini-happy"];

function isPlainObject(value) {
    return value && typeof value === "object" && !Array.isArray(value);
}

function normalizeBinding(binding) {
    if (Array.isArray(binding)) return { files: binding, fallbackTo: null };
    if (isPlainObject(binding)) {
        return {
            files: Array.isArray(binding.files) ? binding.files : [],
            fallbackTo: typeof binding.fallbackTo === "string" ? binding.fallbackTo : null,
        };
    }
    return { files: [], fallbackTo: null };
}

function isUrl(value) {
    return /^(https?:|data:|blob:|file:)/.test(value);
}

function isSafeAssetRef(value) {
    if (typeof value !== "string" || value.trim() === "") return false;
    if (isUrl(value)) return true;
    if (value.includes("..") || value.includes("\\") || value.startsWith("/")) return false;
    return true;
}

function collectStateBindings(theme) {
    const bindings = {};
    if (isPlainObject(theme?.states)) {
        Object.entries(theme.states).forEach(([state, binding]) => {
            bindings[state] = normalizeBinding(binding);
        });
    }
    if (isPlainObject(theme?.miniMode?.states)) {
        Object.entries(theme.miniMode.states).forEach(([state, binding]) => {
            bindings[state] = normalizeBinding(binding);
        });
    }
    return bindings;
}

function validateAssetList(errors, label, files) {
    files.forEach((file) => {
        if (!isSafeAssetRef(file)) errors.push(`${label} contains unsafe asset reference: ${file}`);
    });
}

function validateTierList(errors, label, tiers) {
    if (tiers === undefined) return;
    if (!Array.isArray(tiers)) {
        errors.push(`${label} must be an array`);
        return;
    }
    tiers.forEach((tier, index) => {
        if (!isPlainObject(tier)) {
            errors.push(`${label}[${index}] must be an object`);
            return;
        }
        if (!Number.isFinite(tier.minSessions) || tier.minSessions < 1) {
            errors.push(`${label}[${index}].minSessions must be a positive number`);
        }
        if (!isSafeAssetRef(tier.file)) {
            errors.push(`${label}[${index}].file is missing or unsafe`);
        }
    });
}

export function validatePetTheme(theme) {
    const errors = [];
    const warnings = [];

    if (!isPlainObject(theme)) {
        return { errors: ["theme must be an object"], warnings };
    }
    if (theme.schemaVersion !== 1) errors.push(`schemaVersion must be 1, got ${theme.schemaVersion}`);
    if (!theme.id) errors.push("missing required field: id");
    if (!theme.name) warnings.push("missing recommended field: name");
    if (!theme.version) warnings.push("missing recommended field: version");
    if (!isPlainObject(theme.states)) errors.push("missing required field: states");

    const bindings = collectStateBindings(theme);
    REQUIRED_STATES.forEach((state) => {
        if (!bindings[state]?.files?.length) errors.push(`states.${state} must define at least one file`);
    });
    RECOMMENDED_STATES.forEach((state) => {
        if (!bindings[state]) warnings.push(`states.${state} is recommended for clawd-style behavior`);
    });

    Object.entries(bindings).forEach(([state, binding]) => {
        validateAssetList(errors, `states.${state}`, binding.files);
        if (binding.fallbackTo && !bindings[binding.fallbackTo]) {
            errors.push(`states.${state}.fallbackTo target "${binding.fallbackTo}" does not exist`);
        }

        const visited = new Set([state]);
        let cursor = state;
        for (let hops = 0; hops < 5; hops += 1) {
            const next = bindings[cursor]?.fallbackTo;
            if (!next) break;
            if (visited.has(next)) {
                errors.push(`states.${state}.fallbackTo forms a cycle`);
                break;
            }
            visited.add(next);
            cursor = next;
        }
    });

    if (theme.miniMode?.supported) {
        MINI_REQUIRED_STATES.forEach((state) => {
            if (!bindings[state]?.files?.length) {
                errors.push(`miniMode.supported=true requires miniMode.states.${state}`);
            }
        });
    }

    validateTierList(errors, "workingTiers", theme.workingTiers);
    validateTierList(errors, "jugglingTiers", theme.jugglingTiers);

    if (Array.isArray(theme.idleAnimations)) {
        theme.idleAnimations.forEach((entry, index) => {
            if (!isPlainObject(entry) || !isSafeAssetRef(entry.file)) {
                errors.push(`idleAnimations[${index}].file is missing or unsafe`);
            }
        });
    }

    if (isPlainObject(theme.reactions)) {
        Object.entries(theme.reactions).forEach(([name, reaction]) => {
            if (!isPlainObject(reaction)) return;
            ["file", "fileLeft", "fileRight"].forEach((key) => {
                if (reaction[key] !== undefined && !isSafeAssetRef(reaction[key])) {
                    errors.push(`reactions.${name}.${key} is unsafe`);
                }
            });
            if (Array.isArray(reaction.files)) validateAssetList(errors, `reactions.${name}.files`, reaction.files);
        });
    }

    return { errors, warnings };
}

export { MINI_REQUIRED_STATES, RECOMMENDED_STATES, REQUIRED_STATES };
