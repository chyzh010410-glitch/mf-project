const path = require("path");
const http = require("http");
const https = require("https");
const { app, BrowserWindow, Menu, Tray, ipcMain, screen } = require("electron");
const {
  createDragSnapshot,
  computeAnchoredDragBounds,
} = require("../clawd-on-desk/src/drag-position");
const hitGeometry = require("../clawd-on-desk/src/hit-geometry");
const clawdTheme = require("../clawd-on-desk/themes/clawd/theme.json");

const DEFAULT_AGENT_CHAT_URL = process.env.MF_AGENT_CHAT_URL || "http://127.0.0.1:8092/api/agent/chat";
const PET_SIZE = { width: 280, height: 280 };

let renderWindow = null;
let hitWindow = null;
let surfaceWindow = null;
let settingsWindow = null;
let dashboardWindow = null;
let tray = null;
let cursorTimer = null;
let topmostTimer = null;
let dragSnapshot = null;
let miniEdge = null;
let currentState = "idle";
let currentAsset = "clawd-idle-follow.svg";
let dragActive = false;
let reactionTimer = null;
const desktopPrefs = {
  doNotDisturb: false,
  lowPower: false,
  muted: false,
  permissionBubblesEnabled: true,
  hideBubbles: false,
  sessionHudPinned: false,
};

function isLive(win) {
  return !!(win && !win.isDestroyed());
}

function getRenderBounds() {
  return isLive(renderWindow) ? renderWindow.getBounds() : null;
}

function getWorkAreaForBounds(bounds) {
  const point = {
    x: Math.round(bounds.x + bounds.width / 2),
    y: Math.round(bounds.y + bounds.height / 2),
  };
  return (screen.getDisplayNearestPoint(point) || screen.getPrimaryDisplay()).workArea;
}

function clampPetBounds(bounds) {
  const area = getWorkAreaForBounds(bounds);
  return {
    ...bounds,
    x: Math.max(area.x, Math.min(bounds.x, area.x + area.width - bounds.width)),
    y: Math.max(area.y, Math.min(bounds.y, area.y + area.height - bounds.height)),
  };
}

function clampPetPosition(x, y, width, height) {
  const clamped = clampPetBounds({ x, y, width, height });
  return { x: clamped.x, y: clamped.y };
}

function setTopmost(win) {
  if (!isLive(win) || !win.isVisible()) return;
  win.setAlwaysOnTop(true, "pop-up-menu");
  win.moveTop();
}

function getHitBox() {
  if (clawdTheme.fileHitBoxes[currentAsset]) return clawdTheme.fileHitBoxes[currentAsset];
  if (clawdTheme.sleepingHitboxFiles.includes(currentAsset)) return clawdTheme.hitBoxes.sleeping;
  if (clawdTheme.wideHitboxFiles.includes(currentAsset)) return clawdTheme.hitBoxes.wide;
  return clawdTheme.hitBoxes.default;
}

function getHitRect(bounds = getRenderBounds()) {
  if (!bounds) return null;
  return hitGeometry.getHitRectScreen(clawdTheme, bounds, currentState, currentAsset, getHitBox());
}

function sendVisual(state, asset, options = {}) {
  currentState = state;
  currentAsset = asset;
  sendToRender("mf-pet-render:visual", { state, asset, ...options });
  syncHitWindow();
}

function syncHitWindow() {
  if (!isLive(hitWindow)) return;
  // Keep pointer capture stable during native dragging, exactly as Clawd does.
  if (dragActive) return;
  const bounds = getRenderBounds();
  if (!bounds) return;
  const area = getWorkAreaForBounds(bounds);
  const hit = getHitRect(bounds);
  if (!hit) return;
  let x = Math.round(hit.left);
  let y = Math.round(hit.top);
  let width = Math.max(1, Math.round(hit.right - hit.left));
  let height = Math.max(1, Math.round(hit.bottom - hit.top));
  if (miniEdge === "left") {
    const right = Math.min(area.x + width, Math.round(hit.right));
    x = area.x;
    width = Math.max(1, right - x);
  }
  if (miniEdge === "right") {
    const right = area.x + area.width;
    x = Math.max(area.x, Math.round(hit.left));
    width = Math.max(1, right - x);
  }
  hitWindow.setBounds({
    x,
    y,
    width,
    height,
  });
  if (typeof hitWindow.setShape === "function") hitWindow.setShape([{ x: 0, y: 0, width, height }]);
  setTopmost(hitWindow);
}

function setPetBounds(bounds) {
  if (!isLive(renderWindow)) return;
  renderWindow.setBounds({ ...bounds, width: PET_SIZE.width, height: PET_SIZE.height });
  syncHitWindow();
  repositionSurface();
}

function exitMiniMode() {
  if (!miniEdge) return;
  const bounds = getRenderBounds();
  if (!bounds) return;
  const area = getWorkAreaForBounds(bounds);
  miniEdge = null;
  setPetBounds({
    ...bounds,
    x: bounds.x < area.x ? area.x + 12 : Math.min(bounds.x, area.x + area.width - bounds.width - 12),
  });
  sendToRender("mf-pet-render:mini", { edge: null });
  if (isLive(hitWindow)) hitWindow.webContents.send("mf-pet-hit:state", { miniMode: false, currentState });
  sendVisual("idle", "clawd-idle-follow.svg");
  broadcastSnapshot();
}

function snapToEdge() {
  const bounds = getRenderBounds();
  if (!bounds) return;
  const area = getWorkAreaForBounds(bounds);
  const threshold = 48;
  if (bounds.x <= area.x + threshold) {
    miniEdge = "left";
    setPetBounds({ ...bounds, x: area.x - Math.round(PET_SIZE.width * clawdTheme.miniMode.offsetRatio) });
  } else if (bounds.x + bounds.width >= area.x + area.width - threshold) {
    miniEdge = "right";
    setPetBounds({ ...bounds, x: area.x + area.width - PET_SIZE.width + Math.round(PET_SIZE.width * clawdTheme.miniMode.offsetRatio) });
  } else {
    miniEdge = null;
    setPetBounds(clampPetBounds(bounds));
  }
  sendToRender("mf-pet-render:mini", { edge: miniEdge });
  sendVisual(miniEdge ? "mini-idle" : "idle", miniEdge ? "clawd-mini-idle.svg" : "clawd-idle-follow.svg");
  if (isLive(hitWindow)) hitWindow.webContents.send("mf-pet-hit:state", { miniMode: !!miniEdge, currentState });
  broadcastSnapshot();
}

function beginDrag() {
  if (miniEdge) {
    exitMiniMode();
    return false;
  }
  const bounds = getRenderBounds();
  if (!bounds) return false;
  dragSnapshot = createDragSnapshot(screen.getCursorScreenPoint(), bounds, PET_SIZE);
  dragActive = true;
  sendVisual("drag", "clawd-react-drag.svg");
  hideSurface();
  return true;
}

function moveDrag() {
  if (!dragSnapshot || !isLive(renderWindow)) return;
  const cursor = screen.getCursorScreenPoint();
  const next = computeAnchoredDragBounds(dragSnapshot, cursor, clampPetPosition);
  if (!next) return;
  setPetBounds(next);
}

function endDrag() {
  if (!dragSnapshot) return;
  dragSnapshot = null;
  dragActive = false;
  snapToEdge();
}

function sendToRender(channel, payload) {
  if (isLive(renderWindow)) renderWindow.webContents.send(channel, payload);
}

function getDesktopSnapshot() {
  return {
    state: "idle",
    mini: !!miniEdge,
    miniEdge,
    ...desktopPrefs,
    theme: { name: "MF_Pet / Clawd", assetBaseUrl: "clawd-on-desk/assets/svg", ui: { spriteSize: PET_SIZE.width } },
    sessions: [],
  };
}

function broadcastSnapshot() {
  const snapshot = getDesktopSnapshot();
  [settingsWindow, dashboardWindow].forEach((win) => {
    if (isLive(win)) win.webContents.send("mf-pet-shell:snapshot", snapshot);
  });
}

function postJson(urlString, body) {
  return new Promise((resolve, reject) => {
    const url = new URL(urlString);
    const data = Buffer.from(JSON.stringify(body || {}), "utf8");
    const client = url.protocol === "https:" ? https : http;
    const request = client.request({
      method: "POST",
      protocol: url.protocol,
      hostname: url.hostname,
      port: url.port,
      path: `${url.pathname}${url.search}`,
      headers: { "Content-Type": "application/json; charset=utf-8", "Content-Length": data.length },
      timeout: 15000,
    }, (response) => {
      const chunks = [];
      response.on("data", (chunk) => chunks.push(chunk));
      response.on("end", () => {
        const text = Buffer.concat(chunks).toString("utf8");
        let parsed = null;
        try { parsed = text ? JSON.parse(text) : null; } catch { parsed = { raw: text }; }
        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject(new Error(`AgentService HTTP ${response.statusCode}`));
          return;
        }
        resolve(parsed);
      });
    });
    request.on("timeout", () => request.destroy(new Error("AgentService request timed out")));
    request.on("error", reject);
    request.end(data);
  });
}

function createPetWindows() {
  const area = screen.getPrimaryDisplay().workArea;
  const initial = {
    width: PET_SIZE.width,
    height: PET_SIZE.height,
    x: area.x + area.width - PET_SIZE.width - 24,
    y: area.y + area.height - PET_SIZE.height - 24,
  };
  const shared = {
    frame: false,
    transparent: true,
    resizable: false,
    skipTaskbar: true,
    hasShadow: false,
    alwaysOnTop: true,
    show: false,
    webPreferences: { contextIsolation: true, nodeIntegration: false, backgroundThrottling: false },
  };
  const initialHit = getHitRect(initial);

  renderWindow = new BrowserWindow({
    ...shared,
    ...initial,
    focusable: false,
    webPreferences: { ...shared.webPreferences, preload: path.join(__dirname, "render-preload.cjs") },
  });
  renderWindow.setIgnoreMouseEvents(true);
  renderWindow.loadFile(path.join(__dirname, "render.html"));
  renderWindow.once("ready-to-show", () => {
    renderWindow.showInactive();
    sendVisual(currentState, currentAsset);
  });

  hitWindow = new BrowserWindow({
    ...shared,
    width: Math.round(initialHit.right - initialHit.left),
    height: Math.round(initialHit.bottom - initialHit.top),
    x: Math.round(initialHit.left),
    y: Math.round(initialHit.top),
    focusable: true,
    webPreferences: { ...shared.webPreferences, preload: path.join(__dirname, "hit-preload.cjs") },
  });
  if (typeof hitWindow.setShape === "function") hitWindow.setShape([{ x: 0, y: 0, width: Math.round(initialHit.right - initialHit.left), height: Math.round(initialHit.bottom - initialHit.top) }]);
  hitWindow.loadFile(path.join(__dirname, "hit.html"));
  hitWindow.once("ready-to-show", () => {
    hitWindow.showInactive();
    hitWindow.webContents.send("mf-pet-hit:state", { miniMode: false, currentState });
  });
  [renderWindow, hitWindow].forEach((win) => {
    win.setVisibleOnAllWorkspaces(true, { visibleOnFullScreen: true });
    setTopmost(win);
  });
}

function getSurfaceBounds(mode) {
  const pet = getRenderBounds();
  const area = getWorkAreaForBounds(pet);
  const width = 368;
  const height = mode === "chat" ? 462 : 330;
  const placeLeft = pet.x + pet.width + width + 16 > area.x + area.width;
  return {
    width,
    height,
    x: placeLeft ? Math.max(area.x + 12, pet.x - width - 12) : Math.min(area.x + area.width - width - 12, pet.x + pet.width + 12),
    y: Math.max(area.y + 12, Math.min(pet.y, area.y + area.height - height - 12)),
  };
}

function repositionSurface() {
  if (!isLive(surfaceWindow) || !surfaceWindow.isVisible()) return;
  const mode = surfaceWindow.__mode || "menu";
  surfaceWindow.setBounds(getSurfaceBounds(mode));
}

function showSurface(mode) {
  if (miniEdge) exitMiniMode();
  if (!isLive(surfaceWindow)) {
    surfaceWindow = new BrowserWindow({
      ...getSurfaceBounds(mode),
      frame: false,
      transparent: true,
      resizable: false,
      skipTaskbar: true,
      hasShadow: true,
      alwaysOnTop: true,
      show: false,
      webPreferences: { preload: path.join(__dirname, "surface-preload.cjs"), contextIsolation: true, nodeIntegration: false },
    });
    surfaceWindow.loadFile(path.join(__dirname, "surface.html"));
    surfaceWindow.once("ready-to-show", () => {
      surfaceWindow.show();
      surfaceWindow.webContents.send("mf-pet-surface:show", { mode });
    });
    surfaceWindow.on("closed", () => { surfaceWindow = null; });
  } else {
    surfaceWindow.setBounds(getSurfaceBounds(mode));
    surfaceWindow.show();
    surfaceWindow.focus();
    surfaceWindow.webContents.send("mf-pet-surface:show", { mode });
  }
  surfaceWindow.__mode = mode;
  setTopmost(surfaceWindow);
}

function hideSurface() {
  if (isLive(surfaceWindow)) surfaceWindow.hide();
}

function createAuxWindow(kind, options) {
  const existing = kind === "settings" ? settingsWindow : dashboardWindow;
  if (isLive(existing)) { existing.show(); existing.focus(); return existing; }
  const area = (screen.getDisplayNearestPoint(screen.getCursorScreenPoint()) || screen.getPrimaryDisplay()).workArea;
  const win = new BrowserWindow({
    width: options.width, height: options.height,
    x: Math.round(area.x + (area.width - options.width) / 2), y: Math.round(area.y + (area.height - options.height) / 2),
    minWidth: options.minWidth, minHeight: options.minHeight, title: options.title, show: false, backgroundColor: "#f6f7f9",
    webPreferences: { preload: path.join(__dirname, "preload.cjs"), contextIsolation: true, nodeIntegration: false },
  });
  win.setMenuBarVisibility(false);
  win.loadFile(path.join(__dirname, options.file));
  win.once("ready-to-show", () => {
    win.show();
    win.webContents.send("mf-pet-shell:snapshot", getDesktopSnapshot());
  });
  win.on("closed", () => { if (kind === "settings") settingsWindow = null; else dashboardWindow = null; });
  if (kind === "settings") settingsWindow = win; else dashboardWindow = win;
  return win;
}

const openSettingsWindow = () => createAuxWindow("settings", { file: "settings.html", title: "MF_Pet Settings", width: 920, height: 640, minWidth: 760, minHeight: 560 });
const openDashboardWindow = () => createAuxWindow("dashboard", { file: "dashboard.html", title: "MF_Pet Dashboard", width: 920, height: 620, minWidth: 760, minHeight: 520 });

function createTray() {
  tray = new Tray(path.join(__dirname, "..", "clawd-on-desk", "assets", "icon.png"));
  tray.setToolTip("MF_Pet Desktop");
  tray.setContextMenu(Menu.buildFromTemplate([
    { label: "Show Pet", click: () => { if (isLive(renderWindow)) renderWindow.showInactive(); if (isLive(hitWindow)) hitWindow.showInactive(); } },
    { label: "Hide Pet", click: () => { if (isLive(renderWindow)) renderWindow.hide(); if (isLive(hitWindow)) hitWindow.hide(); hideSurface(); } },
    { type: "separator" }, { label: "Dashboard", click: openDashboardWindow }, { label: "Settings", click: openSettingsWindow },
    { type: "separator" }, { label: "Quit", click: () => app.quit() },
  ]));
}

ipcMain.on("mf-pet-hit:drag-start", () => beginDrag());
ipcMain.on("mf-pet-hit:drag-end", endDrag);
ipcMain.on("mf-pet-hit:click", () => { if (miniEdge) exitMiniMode(); });
ipcMain.on("mf-pet-hit:context-menu", () => showSurface("menu"));
ipcMain.on("mf-pet-hit:show-dashboard", openDashboardWindow);
ipcMain.on("mf-pet-hit:reaction", (_event, payload = {}) => {
  if (miniEdge || dragActive || !payload.asset) return;
  clearTimeout(reactionTimer);
  sendVisual("reaction", payload.asset);
  reactionTimer = setTimeout(() => {
    reactionTimer = null;
    if (!miniEdge && !dragActive) sendVisual("idle", "clawd-idle-follow.svg");
  }, Math.max(250, Number(payload.duration) || 2500));
});
ipcMain.on("mf-pet-surface:open-chat", () => showSurface("chat"));
ipcMain.on("mf-pet-surface:open-menu", () => showSurface("menu"));
ipcMain.on("mf-pet-surface:close", hideSurface);
ipcMain.on("mf-pet-surface:open-settings", openSettingsWindow);
ipcMain.on("mf-pet-surface:open-dashboard", openDashboardWindow);
ipcMain.on("mf-pet-surface:hide-pet", () => { if (isLive(renderWindow)) renderWindow.hide(); if (isLive(hitWindow)) hitWindow.hide(); hideSurface(); });
ipcMain.handle("mf-pet-surface:agent-chat", (_event, request) => postJson(DEFAULT_AGENT_CHAT_URL, request));
ipcMain.handle("mf-pet-shell:get-capabilities", () => ({
  shellMode: "desktop", transparentWindow: true, alwaysOnTop: true, tray: true, multiScreen: screen.getAllDisplays().length > 1,
}));
ipcMain.handle("mf-pet-shell:get-snapshot", () => getDesktopSnapshot());
ipcMain.on("mf-pet-shell:command", (_event, message = {}) => {
  const { command, payload = {} } = message;
  const preference = command.replace(/^set/, "").replace(/^./, (char) => char.toLowerCase());
  if (Object.prototype.hasOwnProperty.call(desktopPrefs, preference)) {
    desktopPrefs[preference] = !!payload.enabled;
  } else if (command === "openChat") {
    showSurface("chat");
  } else if (command === "show") {
    if (isLive(renderWindow)) renderWindow.showInactive();
    if (isLive(hitWindow)) hitWindow.showInactive();
  } else if (command === "enterMini") {
    const bounds = getRenderBounds();
    if (bounds) {
      const area = getWorkAreaForBounds(bounds);
      miniEdge = payload.edge === "left" ? "left" : "right";
      setPetBounds({
        ...bounds,
        x: miniEdge === "left"
          ? area.x - Math.round(PET_SIZE.width * clawdTheme.miniMode.offsetRatio)
          : area.x + area.width - PET_SIZE.width + Math.round(PET_SIZE.width * clawdTheme.miniMode.offsetRatio),
      });
      sendToRender("mf-pet-render:mini", { edge: miniEdge });
      sendVisual("mini-idle", "clawd-mini-idle.svg");
      if (isLive(hitWindow)) hitWindow.webContents.send("mf-pet-hit:state", { miniMode: true, currentState });
    }
  } else if (command === "exitMini") {
    exitMiniMode();
  } else if (command === "resetPosition") {
    const area = screen.getPrimaryDisplay().workArea;
    miniEdge = null;
    setPetBounds({ x: area.x + area.width - PET_SIZE.width - 24, y: area.y + area.height - PET_SIZE.height - 24 });
    sendToRender("mf-pet-render:mini", { edge: null });
  }
  broadcastSnapshot();
});
ipcMain.on("mf-pet-shell:open-settings", openSettingsWindow);
ipcMain.on("mf-pet-shell:open-dashboard", openDashboardWindow);

app.whenReady().then(() => {
  createPetWindows();
  createTray();
  cursorTimer = setInterval(() => {
    if (dragSnapshot) moveDrag();
    if (isLive(renderWindow)) sendToRender("mf-pet-render:cursor", screen.getCursorScreenPoint());
  }, 16);
  topmostTimer = setInterval(() => [renderWindow, hitWindow, surfaceWindow].forEach(setTopmost), 2000);
  screen.on("display-metrics-changed", () => { const bounds = getRenderBounds(); if (bounds && !miniEdge) setPetBounds(clampPetBounds(bounds)); });
});

app.on("window-all-closed", () => {});
app.on("before-quit", () => {
  clearInterval(cursorTimer);
  clearInterval(topmostTimer);
  if (tray) tray.destroy();
  if (isLive(hitWindow)) hitWindow.destroy();
});
