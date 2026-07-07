import gsap from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import { ease, lerp } from "../shared/math.js";
import { BREAKPOINTS, getAnimationViewportHeight, isViewportAtMost, onWidthChange } from "../shared/viewport.js";

export function initLocationScroll() {
    const heroContent = document.querySelector(".hero-content");
    const heroImg = document.querySelector(".hero-img");
    const heroMask = document.querySelector(".hero-mask");
    const heroGridOverlay = document.querySelector(".hero-grid-overlay");
    const anchorMarker = document.querySelector(".marker-1");
    const progressBar = document.querySelector(".hero-scroll-progress-bar");
    const lastStyle = {
        maskSize: "",
        overlayOpacity: "",
        gridOpacity: "",
        progress: "",
    };

    function setupHeroScroll() {
        const isMobile = isViewportAtMost(BREAKPOINTS.locationMobile);
        const viewportHeight = getAnimationViewportHeight(isMobile);
        const heroContentMoveDistance = heroContent.offsetHeight - viewportHeight;
        const heroImgMoveDistance = heroImg.offsetHeight - viewportHeight;
        const maskOutsideSize = 240;
        const maskGridSize = 68;
        const mobileMaskOutsideRadius = 86;
        const mobileMaskGridRadius = 18;
        const maskStep = isMobile ? 0.4 : 0.001;
        const maskRadiusStep = 0.25;
        const opacityStep = isMobile ? 0.01 : 0.001;
        const setHeroContentY = gsap.quickSetter(heroContent, "y", "px");
        const setHeroImgY = gsap.quickSetter(heroImg, "y", "px");
        const setGridOpacity = gsap.quickSetter(heroGridOverlay, "opacity");
        const setMarkerOpacity = gsap.quickSetter(anchorMarker, "opacity");

        lastStyle.maskSize = "";
        lastStyle.overlayOpacity = "";
        lastStyle.gridOpacity = "";
        lastStyle.progress = "";

        ScrollTrigger.getById("location-scroll")?.kill();

        ScrollTrigger.create({
            id: "location-scroll",
            trigger: ".location-hero",
            start: "top top",
            end: `+=${viewportHeight * 4}px`,
            pin: true,
            pinType: "fixed",
            pinSpacing: true,
            scrub: 1,
            onUpdate: (self) => {
                const nextProgress = self.progress.toFixed(isMobile ? 3 : 4);
                if (nextProgress !== lastStyle.progress) {
                    progressBar.style.setProperty("--progress", nextProgress);
                    lastStyle.progress = nextProgress;
                }

                setHeroContentY(-self.progress * heroContentMoveDistance);

                let heroImgProgress;
                if (self.progress <= 0.45) {
                    heroImgProgress = ease(self.progress / 0.45) * 0.65;
                } else if (self.progress <= 0.75) {
                    heroImgProgress = 0.65;
                } else {
                    heroImgProgress = 0.65 + ease((self.progress - 0.75) / 0.25) * 0.35;
                }

                setHeroImgY(heroImgProgress * heroImgMoveDistance);

                let heroMaskSize;
                let heroImgOverlayOpacity;

                if (self.progress <= 0.42) {
                    heroMaskSize = maskOutsideSize;
                    heroImgOverlayOpacity = 0.35;
                } else if (self.progress <= 0.56) {
                    const phaseProgress = ease((self.progress - 0.42) / 0.14);
                    heroMaskSize = lerp(maskOutsideSize, maskGridSize, phaseProgress);
                    heroImgOverlayOpacity = 0.35 + phaseProgress * 0.35;
                } else if (self.progress <= 0.72) {
                    heroMaskSize = maskGridSize;
                    heroImgOverlayOpacity = 0.7;
                } else if (self.progress <= 0.88) {
                    const phaseProgress = ease((self.progress - 0.72) / 0.16);
                    heroMaskSize = lerp(maskGridSize, maskOutsideSize, phaseProgress);
                    heroImgOverlayOpacity = 0.7 - phaseProgress * 0.35;
                } else {
                    heroMaskSize = maskOutsideSize;
                    heroImgOverlayOpacity = 0.35;
                }

                if (isMobile) {
                    const maskProgress = gsap.utils.clamp(
                        0,
                        1,
                        (heroMaskSize - maskGridSize) / (maskOutsideSize - maskGridSize),
                    );
                    const rawMaskRadius = gsap.utils.interpolate(mobileMaskGridRadius, mobileMaskOutsideRadius, maskProgress);
                    const roundedMaskRadius = Math.round(rawMaskRadius / maskRadiusStep) * maskRadiusStep;
                    const nextMaskRadius = `${roundedMaskRadius.toFixed(2)}vmax`;
                    if (nextMaskRadius !== lastStyle.maskSize) {
                        heroMask.style.setProperty("--mask-radius", nextMaskRadius);
                        lastStyle.maskSize = nextMaskRadius;
                    }
                } else {
                    const roundedMaskSize = Math.round(heroMaskSize / maskStep) * maskStep;
                    const nextMaskSize = `${roundedMaskSize.toFixed(3)}%`;
                    if (nextMaskSize !== lastStyle.maskSize) {
                        heroMask.style.setProperty("--mask-size", nextMaskSize);
                        lastStyle.maskSize = nextMaskSize;
                    }
                }

                const roundedOverlayOpacity = Math.round(heroImgOverlayOpacity / opacityStep) * opacityStep;
                const nextOverlayOpacity = roundedOverlayOpacity.toFixed(isMobile ? 2 : 3);
                if (nextOverlayOpacity !== lastStyle.overlayOpacity) {
                    heroImg.style.setProperty("--overlay-opacity", nextOverlayOpacity);
                    lastStyle.overlayOpacity = nextOverlayOpacity;
                }

                let heroGridOpacity;
                if (self.progress <= 0.54) {
                    heroGridOpacity = 0;
                } else if (self.progress <= 0.58) {
                    heroGridOpacity = ease((self.progress - 0.54) / 0.04);
                } else if (self.progress <= 0.72) {
                    heroGridOpacity = 1;
                } else if (self.progress <= 0.78) {
                    heroGridOpacity = 1 - ease((self.progress - 0.72) / 0.06);
                } else {
                    heroGridOpacity = 0;
                }

                const roundedGridOpacity = Math.round(heroGridOpacity / opacityStep) * opacityStep;
                const nextGridOpacity = roundedGridOpacity.toFixed(isMobile ? 2 : 3);
                if (nextGridOpacity !== lastStyle.gridOpacity) {
                    setGridOpacity(nextGridOpacity);
                    setMarkerOpacity(nextGridOpacity);
                    anchorMarker.classList.toggle("is-pulsing", heroGridOpacity > 0.01);
                    lastStyle.gridOpacity = nextGridOpacity;
                }
            },
        });
    }

    setupHeroScroll();
    onWidthChange(() => {
        setupHeroScroll();
        ScrollTrigger.refresh();
    });
}
