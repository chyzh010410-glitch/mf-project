const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("mfPetShell", {
  getCapabilities: () => ipcRenderer.invoke("mf-pet-shell:get-capabilities"),
  moveBy: (delta) => ipcRenderer.send("mf-pet-shell:move-by", delta),
  beginDrag: (payload) => ipcRenderer.invoke("mf-pet-shell:begin-drag", payload),
  dragMove: (payload) => ipcRenderer.send("mf-pet-shell:drag-move", payload),
  cancelDrag: () => ipcRenderer.send("mf-pet-shell:cancel-drag"),
  finishDrag: () => ipcRenderer.invoke("mf-pet-shell:finish-drag"),
  getSnapshot: () => ipcRenderer.invoke("mf-pet-shell:get-snapshot"),
  updateSnapshot: (snapshot) => ipcRenderer.send("mf-pet-shell:snapshot", snapshot),
  command: (command, payload) => ipcRenderer.send("mf-pet-shell:command", { command, payload }),
  openSettings: () => ipcRenderer.send("mf-pet-shell:open-settings"),
  openDashboard: () => ipcRenderer.send("mf-pet-shell:open-dashboard"),
  agentChat: (request) => ipcRenderer.invoke("mf-pet-shell:agent-chat", request),
  setMousePassthrough: (enabled) => ipcRenderer.send("mf-pet-shell:set-mouse-passthrough", !!enabled),
  onCommand: (callback) => {
    ipcRenderer.on("mf-pet-shell:command", (_event, message) => callback(message));
  },
  onSnapshot: (callback) => {
    ipcRenderer.on("mf-pet-shell:snapshot", (_event, snapshot) => callback(snapshot));
  },
  onCursorMove: (callback) => {
    ipcRenderer.on("mf-pet-shell:cursor", (_event, payload) => callback(payload));
  },
  hide: () => ipcRenderer.send("mf-pet-shell:hide"),
});
