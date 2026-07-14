const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("mfPetSurface", {
  onShow: (callback) => ipcRenderer.on("mf-pet-surface:show", (_event, payload) => callback(payload)),
  openChat: () => ipcRenderer.send("mf-pet-surface:open-chat"),
  openMenu: () => ipcRenderer.send("mf-pet-surface:open-menu"),
  close: () => ipcRenderer.send("mf-pet-surface:close"),
  openSettings: () => ipcRenderer.send("mf-pet-surface:open-settings"),
  openDashboard: () => ipcRenderer.send("mf-pet-surface:open-dashboard"),
  hidePet: () => ipcRenderer.send("mf-pet-surface:hide-pet"),
  agentChat: (request) => ipcRenderer.invoke("mf-pet-surface:agent-chat", request),
});
