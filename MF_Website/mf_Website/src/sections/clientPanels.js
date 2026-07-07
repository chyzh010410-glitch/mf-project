import gsap from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import { BREAKPOINTS, getAnimationViewportHeight, isViewportAtMost, onWidthChange } from "../shared/viewport.js";

const smoothStep = (progress) => progress * progress * (3 - 2 * progress);
const FLIP_PROGRESS_SPAN = 0.18;
const SPREAD_START_PROGRESS = 0.36;
const SETTLED_CARD_PROGRESS = 0.84;
const FLOAT_START_PROGRESS = 0.04;

export function initClientPanels() {
    const servicesPanel = document.querySelector(".client-services-panel");
    const servicesHeader = document.querySelector(".client-services-header");
    const cards = gsap.utils.toArray(".client-panel-card");
    const cardStates = cards.map((card, index) => ({
        card,
        innerCard: card.querySelector(".client-panel-card-inner"),
        startXPercent: index === 0 ? 100 : index === 1 ? 0 : -100,
        startRotate: index === 0 ? -5 : index === 1 ? 0 : 5,
        delay: index * 0.5,
    }));
    const setHeaderY = servicesHeader ? gsap.quickSetter(servicesHeader, "y") : null;
    let isFloating = false;

    if (!servicesPanel || cards.length === 0) {
        return;
    }

    function setupPanelScroll() {
        const isMobile = isViewportAtMost(BREAKPOINTS.clientPanelsMobile);
        const viewportHeight = getAnimationViewportHeight(isMobile);
        const pinDistance = viewportHeight * 2;
        const cardsDistance = isMobile ? viewportHeight * 1.55 : viewportHeight * 3;

        ScrollTrigger.getById("client-services-pin")?.kill();
        ScrollTrigger.getById("client-services-cards")?.kill();
        servicesPanel.classList.remove("is-floating");
        isFloating = false;

        if (servicesHeader) {
            gsap.set(servicesHeader, { y: "360%" });
        }
        cardStates.forEach((state) => {
            gsap.set(state.card, {
                opacity: 0,
                yPercent: -100,
                xPercent: state.startXPercent,
                rotation: state.startRotate,
                scale: 0.25,
                force3D: true,
            });
            gsap.set(state.innerCard, { rotationY: 0 });
        });

        ScrollTrigger.create({
            id: "client-services-pin",
            trigger: servicesPanel,
            start: "top top",
            end: `+=${pinDistance}`,
            pin: true,
            pinType: "fixed",
            pinSpacing: true,
        });

        ScrollTrigger.create({
            id: "client-services-cards",
            trigger: servicesPanel,
            start: "top 45%",
            end: `+=${cardsDistance}`,
            scrub: 1,
            invalidateOnRefresh: true,
            onUpdate: (self) => {
                const progress = self.progress;
                const shouldFloat = progress >= FLOAT_START_PROGRESS;
                if (shouldFloat !== isFloating) {
                    servicesPanel.classList.toggle("is-floating", shouldFloat);
                    isFloating = shouldFloat;
                }

                if (servicesHeader) {
                    const headerProgress = gsap.utils.clamp(0, 1, progress / 0.9);
                    const headerY = gsap.utils.interpolate("360%", "0%", smoothStep(headerProgress));
                    setHeaderY(headerY);
                }

                cardStates.forEach((state) => {
                    const cardProgress = gsap.utils.clamp(
                        0,
                        1,
                        (progress - state.delay * 0.1) / (0.9 - state.delay * 0.1),
                    );

                    let yPercent;
                    if (cardProgress < 0.4) {
                        yPercent = gsap.utils.interpolate(-100, 50, smoothStep(cardProgress / 0.4));
                    } else if (cardProgress < 0.6) {
                        yPercent = gsap.utils.interpolate(50, 0, smoothStep((cardProgress - 0.4) / 0.2));
                    } else {
                        yPercent = 0;
                    }

                    let scale;
                    if (cardProgress < 0.4) {
                        scale = gsap.utils.interpolate(0.25, 0.75, smoothStep(cardProgress / 0.4));
                    } else if (cardProgress < 0.6) {
                        scale = gsap.utils.interpolate(0.75, 1, smoothStep((cardProgress - 0.4) / 0.2));
                    } else {
                        scale = 1;
                    }

                    const opacity = cardProgress < 0.2 ? smoothStep(cardProgress / 0.2) : 1;

                    const spreadProgress = smoothStep(gsap.utils.clamp(
                        0,
                        1,
                        (cardProgress - SPREAD_START_PROGRESS) / (SETTLED_CARD_PROGRESS - SPREAD_START_PROGRESS),
                    ));
                    let xPercent = gsap.utils.interpolate(state.startXPercent, 0, spreadProgress);
                    let rotate = gsap.utils.interpolate(state.startRotate, 0, spreadProgress);
                    let rotationY;
                    if (cardProgress < 0.6) {
                        rotationY = 0;
                    } else if (cardProgress >= SETTLED_CARD_PROGRESS) {
                        xPercent = 0;
                        rotate = 0;
                        rotationY = 180;
                    } else {
                        const flipProgress = smoothStep(gsap.utils.clamp(
                            0,
                            1,
                            (cardProgress - 0.6) / FLIP_PROGRESS_SPAN,
                        ));
                        rotationY = flipProgress * 180;
                    }

                    gsap.set(state.card, {
                        opacity,
                        yPercent,
                        xPercent,
                        rotation: rotate,
                        scale,
                        force3D: true,
                    });
                    gsap.set(state.innerCard, { rotationY });
                });
            },
        });
    }

    setupPanelScroll();
    onWidthChange(() => {
        setupPanelScroll();
        ScrollTrigger.refresh();
    });
}
