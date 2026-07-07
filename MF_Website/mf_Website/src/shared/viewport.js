export const BREAKPOINTS = {
    locationMobile: 800,
    splitCardsMobile: 1000,
    clientPanelsMobile: 700,
};

export function isViewportAtMost(width) {
    return window.innerWidth <= width;
}

export function isViewportBelow(width) {
    return window.innerWidth < width;
}

export function getAnimationViewportHeight(isMobile) {
    return isMobile
        ? Math.round(window.visualViewport?.height || window.innerHeight)
        : window.innerHeight;
}

export function onWidthChange(callback, { threshold = 8, delay = 120 } = {}) {
    let lastViewportWidth = window.innerWidth;
    let resizeTimer;

    window.addEventListener("resize", () => {
        const nextViewportWidth = window.innerWidth;
        if (Math.abs(nextViewportWidth - lastViewportWidth) < threshold) return;

        lastViewportWidth = nextViewportWidth;
        clearTimeout(resizeTimer);
        resizeTimer = window.setTimeout(callback, delay);
    });
}
