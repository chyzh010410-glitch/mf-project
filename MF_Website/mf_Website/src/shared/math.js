export const ease = (x) => x * x * (3 - 2 * x);

export const lerp = (start, end, progress) => start + (end - start) * progress;
