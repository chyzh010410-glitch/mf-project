const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("mfPetHit", {
  startDrag: () => ipcRenderer.send("mf-pet-hit:drag-start"),
  endDrag: () => ipcRenderer.send("mf-pet-hit:drag-end"),
  click: () => ipcRenderer.send("mf-pet-hit:click"),
  contextMenu: () => ipcRenderer.send("mf-pet-hit:context-menu"),
  showDashboard: () => ipcRenderer.send("mf-pet-hit:show-dashboard"),
  reaction: (asset, duration) => ipcRenderer.send("mf-pet-hit:reaction", { asset, duration }),
  onState: (callback) => ipcRenderer.on("mf-pet-hit:state", (_event, state) => callback(state)),
});
