import { initForestPet } from "../core/forestPet.js";
import { navigateToMall } from "../../shared/mallLink.js";

const MF_LINKS = {
    mall: null,
    encyclopedia: "#",
    ai: "#",
    merchant: "#",
};

function getCurrentLanguage() {
    return localStorage.getItem("forest-site-lang") === "en" ? "en" : "zh";
}

function bindPetTriggers(pet) {
    document.querySelectorAll("[data-forest-pet-trigger]").forEach((trigger) => {
        trigger.addEventListener("click", (event) => {
            event.preventDefault();
            pet.show({ expand: true });
            trigger.closest("details")?.removeAttribute("open");
        });
    });
}

function bindMobileMenuLinks() {
    document.querySelectorAll(".site-nav__menu-list a").forEach((link) => {
        link.addEventListener("click", () => {
            link.closest("details")?.removeAttribute("open");
        });
    });
}

export function initForestPetAdapter() {
    const pet = initForestPet({
        mount: document.body,
        assetsBaseUrl: "/assets/pet",
        lang: getCurrentLanguage(),
        links: MF_LINKS,
        storageKey: "mf-website-forest-pet",
        initiallyVisible: false,
        initiallyExpanded: false,
        onAction({ action }) {
            if (action !== "mall") return false;

            navigateToMall();
            return true;
        },
        onAskAi({ api }) {
            window.setTimeout(() => {
                api.setState("success");
            }, 650);
            return true;
        },
    });

    bindPetTriggers(pet);
    bindMobileMenuLinks();
    window.mfForestPet = pet;

    return pet;
}

export { MF_LINKS };
