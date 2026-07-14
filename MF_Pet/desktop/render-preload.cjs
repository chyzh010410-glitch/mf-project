const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("mfPetRender", {
  onCursor: (callback) => ipcRenderer.on("mf-pet-render:cursor", (_event, point) => callback(point)),
  onMini: (callback) => ipcRenderer.on("mf-pet-render:mini", (_event, state) => callback(state)),
  onVisual: (callback) => ipcRenderer.on("mf-pet-render:visual", (_event, state) => callback(state)),
});
