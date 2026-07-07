export class PetStateMachine {
    constructor(theme, options = {}) {
        this.theme = theme;
        this.currentState = options.initialState || "idle";
        this.previousStableState = this.currentState === "hidden" ? "idle" : this.currentState;
        this.changedAt = Date.now();
        this.pending = null;
        this.pendingTimer = null;
        this.autoReturnTimer = null;
        this.onChange = options.onChange || (() => {});
    }

    destroy() {
        clearTimeout(this.pendingTimer);
        clearTimeout(this.autoReturnTimer);
    }

    emit(eventName, payload = {}) {
        const nextState = this.theme.eventMap[eventName] || eventName;
        return this.setState(nextState, payload);
    }

    setState(nextState, payload = {}) {
        if (!this.theme.states[nextState] && nextState !== "hidden") return this.currentState;

        const now = Date.now();
        const minDisplay = this.theme.timings.minDisplay[this.currentState] || 0;
        const elapsed = now - this.changedAt;
        if (!payload.force && elapsed < minDisplay) {
            clearTimeout(this.pendingTimer);
            this.pending = { nextState, payload };
            this.pendingTimer = setTimeout(() => {
                const pending = this.pending;
                this.pending = null;
                if (pending) this.setState(pending.nextState, { ...pending.payload, force: true });
            }, minDisplay - elapsed);
            return this.currentState;
        }

        this.applyState(nextState, payload);
        return this.currentState;
    }

    applyState(nextState, payload = {}) {
        clearTimeout(this.autoReturnTimer);
        this.currentState = nextState;
        if (!["hidden", "appearing", "success", "unclear", "error"].includes(nextState)) {
            this.previousStableState = nextState;
        }
        this.changedAt = Date.now();
        this.onChange(this.getSnapshot(payload));

        const autoReturnMs = this.theme.timings.autoReturn[nextState] || 0;
        if (autoReturnMs > 0) {
            this.autoReturnTimer = setTimeout(() => {
                const fallback = payload.returnTo || this.previousStableState || "idle";
                this.setState(fallback, { force: true, autoReturn: true });
            }, autoReturnMs);
        }
    }

    getSnapshot(extra = {}) {
        return {
            state: this.currentState,
            previousStableState: this.previousStableState,
            changedAt: this.changedAt,
            ...extra,
        };
    }
}
