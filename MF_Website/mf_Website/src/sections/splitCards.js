import gsap from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import { BREAKPOINTS, getAnimationViewportHeight, isViewportBelow, onWidthChange } from "../shared/viewport.js";

const EXIT_CARD_MIN_SCALE = 0.35;
const FLOAT_START_PROGRESS = 0.72;

export function initSplitCards() {
    const sticky = document.querySelector(".sticky");
    const cardContainer = document.querySelector(".card-container");
    const photoComposite = document.querySelector(".card-photo-composite");
    const stickyHeader = document.querySelector(".sticky-header h1");
    const progressBar = document.querySelector(".scroll-progress-bar");
    let cardTimeline;
    let exitTimeline;
    let isFloating = false;
    let cardsVisible = false;
    let headerVisible = false;

    function stopFloating() {
        sticky.classList.remove("is-floating");
        isFloating = false;
    }

    function startFloating() {
        sticky.classList.add("is-floating");
        isFloating = true;
    }

    function showCards(showHeader = false) {
        sticky.classList.add("is-card-active");
        gsap.set(cardContainer, {
            autoAlpha: 1,
            pointerEvents: "auto",
        });
        cardsVisible = true;

        if (!showHeader) return;

        gsap.set(stickyHeader, {
            visibility: "visible",
            pointerEvents: "auto",
        });
        headerVisible = true;
    }

    function hideCards() {
        stopFloating();
        sticky.classList.remove("is-card-active");
        gsap.set([cardContainer, stickyHeader], {
            autoAlpha: 0,
            pointerEvents: "none",
        });
        cardsVisible = false;
        headerVisible = false;
    }

    function syncCardFrontBackground(isMobile = isViewportBelow(BREAKPOINTS.splitCardsMobile)) {
        if (isMobile) {
            const mobilePositions = {
                "card-1": "left",
                "card-2": "center",
                "card-3": "right",
            };

            document.querySelectorAll(".card").forEach((card) => {
                const front = card.querySelector(".card-front");
                if (!front) return;

                front.style.setProperty("--card-bg-width", "300%");
                front.style.setProperty("--card-bg-x", mobilePositions[card.id] || "center");
            });
            return;
        }

        const containerWidth = cardContainer.offsetWidth;
        if (!containerWidth) return;

        document.querySelectorAll(".card").forEach((card) => {
            const front = card.querySelector(".card-front");
            if (!front) return;

            front.style.setProperty("--card-bg-width", `${containerWidth}px`);
            front.style.setProperty("--card-bg-x", `${-card.offsetLeft}px`);
        });
    }

    function resetDesktopStyles(isMobile = false) {
        stopFloating();
        gsap.set(stickyHeader, {
            y: isMobile ? 24 : 40,
            autoAlpha: 0,
            pointerEvents: "none",
        });
        gsap.set(cardContainer, {
            width: isMobile ? "92%" : "75%",
            gap: 0,
            autoAlpha: 1,
            pointerEvents: "auto",
            scale: 1,
            transformOrigin: "50% 50%",
        });
        gsap.set(photoComposite, { opacity: 1, visibility: "visible" });
        gsap.set(".card", { rotationY: 0, x: 0, xPercent: 0, y: 0, yPercent: 0, scale: 1, opacity: 0 });
        gsap.set(".card-float", { rotationY: 0, force3D: true });
        gsap.set("#card-1", { rotationZ: 0 });
        gsap.set("#card-2", { rotationZ: 0 });
        gsap.set("#card-3", { rotationZ: 0 });
        gsap.set("#card-1", { borderRadius: "20px 0 0 20px" });
        gsap.set("#card-2", { borderRadius: 0 });
        gsap.set("#card-3", { borderRadius: "0 20px 20px 0" });
        syncCardFrontBackground(isMobile);
    }

    function setupCardScroll() {
        ScrollTrigger.getById("split-cards")?.kill();
        ScrollTrigger.getById("split-cards-exit")?.kill();
        ScrollTrigger.getById("split-cards-visibility")?.kill();
        cardTimeline?.kill();
        exitTimeline?.kill();

        const isMobile = isViewportBelow(BREAKPOINTS.splitCardsMobile);
        const initialContainerWidth = isMobile ? 92 : 75;
        const finalContainerWidth = isMobile ? 84 : 60;
        const mobileContainerScale = finalContainerWidth / initialContainerWidth;
        const splitGap = isMobile ? 8 : 20;
        const fanY = isMobile ? 16 : 30;
        const fanRotation = isMobile ? 11 : 15;
        const scrubAmount = 0.45;
        const flipStagger = isMobile ? 0.02 : 0.03;
        const splitDuration = isMobile ? 0.16 : 0.2;
        const viewportHeight = getAnimationViewportHeight(isMobile);
        const pinDistance = viewportHeight * 2;
        const exitCardScale = isMobile ? 0.38 : EXIT_CARD_MIN_SCALE;
        const exitYPercent = isMobile ? 210 : 250;
        const exitXPercent = isMobile ? 82 : 90;

        resetDesktopStyles(isMobile);
        cardsVisible = true;
        headerVisible = isMobile;

        if (isMobile) {
            ScrollTrigger.create({
                id: "split-cards-visibility",
                trigger: ".sticky",
                start: "top bottom",
                end: "bottom top",
                onEnter: () => showCards(true),
                onEnterBack: () => showCards(true),
                onUpdate: (self) => {
                    if (self.isActive) {
                        showCards(true);
                    }
                },
                onLeaveBack: undefined,
            });
        }

        cardTimeline = gsap.timeline({
            scrollTrigger: {
                id: "split-cards",
                trigger: ".sticky",
                start: "top top",
                end: `+=${pinDistance}`,
                scrub: scrubAmount,
                pin: true,
                pinType: "fixed",
                pinSpacing: true,
                anticipatePin: 1,
                invalidateOnRefresh: true,
                onEnter: () => showCards(isMobile),
                onLeave: undefined,
                onEnterBack: () => showCards(isMobile),
                onLeaveBack: undefined,
                onRefresh: (self) => {
                    syncCardFrontBackground(isMobile);
                    if (self.isActive) {
                        showCards(isMobile);
                    }
                },
                onUpdate: (self) => {
                    if (self.isActive) {
                        showCards(isMobile || self.progress >= 0.1);
                    }
                    if (!isMobile) {
                        syncCardFrontBackground(false);
                    }
                    const shouldFloat = self.progress >= FLOAT_START_PROGRESS;
                    if (shouldFloat !== isFloating) {
                        if (shouldFloat) {
                            startFloating();
                        } else {
                            stopFloating();
                        }
                    }
                    if (!isMobile) {
                        gsap.set(progressBar, {
                            "--progress": self.progress,
                        });
                    }
                },
            },
        });

        if (!isMobile) {
            cardTimeline.to(stickyHeader, { y: 0, autoAlpha: 1, duration: 0.15, ease: "none" }, 0.1);
            cardTimeline.to(stickyHeader, { y: 40, opacity: 0, duration: 0.22, ease: "none" }, 0.58);
        } else {
            cardTimeline.to(stickyHeader, { y: 0, autoAlpha: 1, duration: 0.18, ease: "none" }, 0.08);
            cardTimeline.to(stickyHeader, { y: 24, opacity: 0, duration: 0.22, ease: "none" }, 0.58);
        }
        if (isMobile) {
            cardTimeline.to(cardContainer, { scale: mobileContainerScale, duration: 0.25, ease: "none" }, 0);
        } else {
            cardTimeline.to(cardContainer, { width: `${finalContainerWidth}%`, duration: 0.25, ease: "none" }, 0);
        }
        cardTimeline.set(".card", { opacity: 1 }, 0.34);
        cardTimeline.set(photoComposite, { opacity: 0, visibility: "hidden" }, 0.34);
        cardTimeline.to(cardContainer, { gap: splitGap, duration: splitDuration, ease: "power2.out" }, 0.35);
        cardTimeline.to(["#card-1", "#card-2", "#card-3"], {
            borderRadius: 20,
            duration: splitDuration,
            ease: "power2.out",
        }, 0.35);

        cardTimeline.to(".card-float", {
            rotationY: 180,
            duration: 0.22,
            ease: "power2.inOut",
            stagger: flipStagger,
        }, 0.58);
        cardTimeline.to("#card-1", { y: fanY, rotationZ: -fanRotation, duration: 0.18, ease: "power2.inOut" }, 0.88);
        cardTimeline.to("#card-3", { y: fanY, rotationZ: fanRotation, duration: 0.18, ease: "power2.inOut" }, 0.88);

        exitTimeline = gsap.timeline({
            scrollTrigger: {
                id: "split-cards-exit",
                trigger: ".client-about-panel",
                start: "top 96%",
                end: "top top",
                scrub: scrubAmount,
                invalidateOnRefresh: true,
                onEnter: () => showCards(isMobile),
                onLeave: hideCards,
                onEnterBack: () => showCards(isMobile),
                onLeaveBack: () => {
                    gsap.set(["#card-1", "#card-2", "#card-3"], {
                        xPercent: 0,
                        yPercent: 0,
                        scale: 1,
                    });
                    gsap.set(cardContainer, { opacity: 1 });
                    showCards(isMobile);
                },
            },
        });

        exitTimeline.to(cardContainer, { opacity: 0.5, duration: 1, ease: "none" }, 0);
        exitTimeline.to("#card-1", {
            xPercent: exitXPercent,
            yPercent: exitYPercent,
            scale: exitCardScale,
            duration: 1,
            ease: "none",
        }, 0);
        exitTimeline.to("#card-2", {
            xPercent: 0,
            yPercent: exitYPercent,
            scale: exitCardScale,
            duration: 1,
            ease: "none",
        }, 0);
        exitTimeline.to("#card-3", {
            xPercent: -exitXPercent,
            yPercent: exitYPercent,
            scale: exitCardScale,
            duration: 1,
            ease: "none",
        }, 0);
    }

    setupCardScroll();
    onWidthChange(() => {
        setupCardScroll();
        ScrollTrigger.refresh();
    });
}
