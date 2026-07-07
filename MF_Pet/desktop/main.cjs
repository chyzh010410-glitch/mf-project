const path = require("path");
const http = require("http");
const https = require("https");
const { app, BrowserWindow, Menu, Tray, ipcMain, screen } = require("electron");

const DEFAULT_AGENT_CHAT_URL = process.env.MF_AGENT_CHAT_URL || "http://127.0.0.1:8092/api/agent/chat";

let petWindow = null;
let settingsWindow = null;
let dashboardWindow = null;
let tray = null;
let cursorTimer = null;
let topmostTimer = null;
let latestSnapshot = null;

function getWorkAreaForBounds(bounds) {
  const cx = bounds.x + bounds.width / 2;
  const cy = bounds.y + bounds.height / 2;
  const displays = screen.getAllDisplays();
  if (!displays.length) return screen.getPrimaryDisplay().workArea;

  let nearest = displays[0];
  let nearestDistance = Infinity;
  for (const display of displays) {
    const area = display.workArea;
    const dx = Math.max(area.x - cx, 0, cx - (area.x + area.width));
    const dy = Math.max(area.y - cy, 0, cy - (area.y + area.height));
    const distance = dx * dx + dy * dy;
    if (distance < nearestDistance) {
      nearest = display;
      nearestDistance = distance;
    }
  }
  return nearest.workArea;
}

function clampBoundsToWorkArea(bounds, options = {}) {
  const area = getWorkAreaForBounds(bounds);
  const margin = Number.isFinite(options.visibleMargin) ? options.visibleMargin : 64;
  return {
    ...bounds,
    x: Math.max(area.x - bounds.width + margin, Math.min(bounds.x, area.x + area.width - margin)),
    y: Math.max(area.y, Math.min(bounds.y, area.y + area.height - margin)),
  };
}

function snapBoundsToEdge(bounds) {
  const point = screen.getCursorScreenPoint();
  const display = screen.getDisplayNearestPoint(point) || screen.getPrimaryDisplay();
  const area = display.workArea || getWorkAreaForBounds(bounds);
  const threshold = 42;
  if (point.x <= area.x + threshold) {
    return {
      edge: "left",
      bounds: { ...bounds, x: area.x },
    };
  }
  if (point.x >= area.x + area.width - threshold) {
    return {
      edge: "right",
      bounds: { ...bounds, x: area.x + area.width - bounds.width },
    };
  }
  return { edge: null, bounds: clampBoundsToWorkArea(bounds) };
}

function sendToPet(channel, payload) {
  if (!petWindow || petWindow.isDestroyed()) return;
  petWindow.webContents.send(channel, payload);
}

function sendToAuxWindows(channel, payload) {
  [settingsWindow, dashboardWindow].forEach((win) => {
    if (win && !win.isDestroyed()) win.webContents.send(channel, payload);
  });
}

function sendPetCommand(command, payload = {}) {
  sendToPet("mf-pet-shell:command", { command, payload });
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
      headers: {
        "Content-Type": "application/json; charset=utf-8",
        "Content-Length": data.length,
      },
      timeout: 15000,
    }, (response) => {
      const chunks = [];
      response.on("data", (chunk) => chunks.push(chunk));
      response.on("end", () => {
        const text = Buffer.concat(chunks).toString("utf8");
        let parsed = null;
        if (text) {
          try {
            parsed = JSON.parse(text);
          } catch {
            parsed = { raw: text };
          }
        }
        if (response.statusCode < 200 || response.statusCode >= 300) {
          const error = new Error(`AgentService HTTP ${response.statusCode}`);
          error.response = parsed;
          reject(error);
          return;
        }
        resolve(parsed);
      });
    });
    request.on("timeout", () => request.destroy(new Error("AgentService request timed out")));
    request.on("error", reject);
    request.write(data);
    request.end();
  });
}

function assertPetTopmost() {
  if (!petWindow || petWindow.isDestroyed() || !petWindow.isVisible()) return;
  petWindow.setAlwaysOnTop(true, "pop-up-menu");
  petWindow.moveTop();
}

function startShellLoops() {
  clearInterval(cursorTimer);
  clearInterval(topmostTimer);
  cursorTimer = setInterval(() => {
    if (!petWindow || petWindow.isDestroyed()) return;
    const point = screen.getCursorScreenPoint();
    const bounds = petWindow.getBounds();
    sendToPet("mf-pet-shell:cursor", { point, windowBounds: bounds });
  }, 50);
  topmostTimer = setInterval(assertPetTopmost, 2000);
}

function createAuxWindow(kind, options) {
  const existing = kind === "settings" ? settingsWindow : dashboardWindow;
  if (existing && !existing.isDestroyed()) {
    existing.show();
    existing.focus();
    return existing;
  }

  const cursor = screen.getCursorScreenPoint();
  const display = screen.getDisplayNearestPoint(cursor) || screen.getPrimaryDisplay();
  const area = display.workArea;
  const width = options.width;
  const height = options.height;
  const win = new BrowserWindow({
    width,
    height,
    x: Math.round(area.x + (area.width - width) / 2),
    y: Math.round(area.y + (area.height - height) / 2),
    minWidth: options.minWidth || width,
    minHeight: options.minHeight || height,
    title: options.title,
    show: false,
    backgroundColor: "#f6f7f9",
    webPreferences: {
      preload: path.join(__dirname, "preload.cjs"),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });
  win.setMenuBarVisibility(false);
  win.loadFile(path.join(__dirname, options.file));
  win.once("ready-to-show", () => {
    win.show();
    if (latestSnapshot) win.webContents.send("mf-pet-shell:snapshot", latestSnapshot);
  });
  win.on("closed", () => {
    if (kind === "settings") settingsWindow = null;
    if (kind === "dashboard") dashboardWindow = null;
  });
  if (kind === "settings") settingsWindow = win;
  if (kind === "dashboard") dashboardWindow = win;
  return win;
}

function openSettingsWindow() {
  return createAuxWindow("settings", {
    file: "settings.html",
    title: "MF_Pet Settings",
    width: 920,
    height: 640,
    minWidth: 760,
    minHeight: 560,
  });
}

function openDashboardWindow() {
  return createAuxWindow("dashboard", {
    file: "dashboard.html",
    title: "MF_Pet Dashboard",
    width: 920,
    height: 620,
    minWidth: 760,
    minHeight: 520,
  });
}

function createPetWindow() {
  const display = screen.getPrimaryDisplay();
  const width = 560;
  const height = 420;
  const x = display.workArea.x + display.workArea.width - width - 28;
  const y = display.workArea.y + display.workArea.height - height - 28;

  petWindow = new BrowserWindow({
    x,
    y,
    width,
    height,
    frame: false,
    transparent: true,
    resizable: false,
    movable: true,
    show: false,
    skipTaskbar: true,
    hasShadow: false,
    alwaysOnTop: true,
    webPreferences: {
      preload: path.join(__dirname, "preload.cjs"),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });

  petWindow.setAlwaysOnTop(true, "pop-up-menu");
  petWindow.setVisibleOnAllWorkspaces(true, { visibleOnFullScreen: true });
  petWindow.setMenuBarVisibility(false);
  petWindow.loadFile(path.join(__dirname, "index.html"));
  petWindow.once("ready-to-show", () => petWindow.showInactive());
  petWindow.on("closed", () => {
    petWindow = null;
  });
}

function createTray() {
  const icon = path.join(__dirname, "..", "clawd-on-desk", "assets", "icon.png");
  tray = new Tray(icon);
  tray.setToolTip("MF_Pet Desktop");
  tray.setContextMenu(Menu.buildFromTemplate([
    { label: "Show Pet", click: () => petWindow && petWindow.showInactive() },
    { label: "Hide Pet", click: () => petWindow && petWindow.hide() },
    { type: "separator" },
    { label: "Dashboard", click: openDashboardWindow },
    { label: "Settings", click: openSettingsWindow },
    { type: "separator" },
    { label: "Quit", click: () => app.quit() },
  ]));
}

ipcMain.handle("mf-pet-shell:get-capabilities", () => ({
  shellMode: "desktop",
  transparentWindow: true,
  alwaysOnTop: true,
  tray: true,
  nativeFocus: true,
  multiScreen: screen.getAllDisplays().length > 1,
}));

ipcMain.on("mf-pet-shell:move-by", (_event, delta) => {
  if (!petWindow || !delta) return;
  const bounds = petWindow.getBounds();
  petWindow.setBounds(clampBoundsToWorkArea({
    ...bounds,
    x: bounds.x + Math.round(Number(delta.x) || 0),
    y: bounds.y + Math.round(Number(delta.y) || 0),
  }, { visibleMargin: 96 }));
  assertPetTopmost();
});

ipcMain.handle("mf-pet-shell:finish-drag", () => {
  if (!petWindow) return { edge: null };
  const result = snapBoundsToEdge(petWindow.getBounds());
  petWindow.setBounds(result.bounds);
  assertPetTopmost();
  return {
    edge: result.edge,
    bounds: petWindow.getBounds(),
  };
});

ipcMain.on("mf-pet-shell:hide", () => {
  if (petWindow) petWindow.hide();
});

ipcMain.on("mf-pet-shell:snapshot", (_event, snapshot) => {
  latestSnapshot = snapshot || null;
  sendToAuxWindows("mf-pet-shell:snapshot", latestSnapshot);
});

ipcMain.handle("mf-pet-shell:get-snapshot", () => latestSnapshot);

ipcMain.on("mf-pet-shell:command", (_event, message = {}) => {
  sendPetCommand(message.command, message.payload || {});
});

ipcMain.on("mf-pet-shell:open-settings", openSettingsWindow);
ipcMain.on("mf-pet-shell:open-dashboard", openDashboardWindow);

ipcMain.handle("mf-pet-shell:agent-chat", async (_event, request) => {
  return postJson(DEFAULT_AGENT_CHAT_URL, request);
});

app.whenReady().then(() => {
  createPetWindow();
  createTray();
  startShellLoops();
});

app.on("window-all-closed", () => {
  // Keep the tray process alive until the user chooses Quit.
});

app.on("before-quit", () => {
  clearInterval(cursorTimer);
  clearInterval(topmostTimer);
  if (tray) tray.destroy();
});
