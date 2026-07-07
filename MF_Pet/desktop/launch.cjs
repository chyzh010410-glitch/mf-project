const path = require("path");
const { spawn } = require("child_process");

function resolveElectron() {
  const candidates = [
    path.join(__dirname, "..", "node_modules", "electron"),
    path.join(__dirname, "..", "clawd-on-desk", "node_modules", "electron"),
  ];

  for (const candidate of candidates) {
    try {
      const electron = require(candidate);
      if (electron) return electron;
    } catch {
      // Try the next local Electron candidate.
    }
  }

  throw new Error("Electron not found. Run npm install, or keep clawd-on-desk/node_modules available.");
}

const electron = resolveElectron();
const child = spawn(electron, [path.join(__dirname, "main.cjs")], {
  cwd: path.join(__dirname, ".."),
  stdio: "inherit",
  windowsHide: false,
});

child.on("exit", (code) => {
  process.exitCode = code || 0;
});
