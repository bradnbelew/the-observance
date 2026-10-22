// Local solo playtest of the Copperline mirror on port 3041, against the local Paper test server.
// Unlike dev-v2.mjs there is no act switcher and nothing is released early: the act follows the
// plugin over the bridge, hints keep the real act clock, and Theo answers a few minutes after
// each ticket (MORROW_DESK_MINUTES) instead of the next evening.
import { readFileSync } from "node:fs";
import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";

const cwd = fileURLToPath(new URL("..", import.meta.url));
const pluginConfig = fileURLToPath(new URL("../../build/mrrw-v2-test/plugins/mrrw/config.yml", import.meta.url));
const stopSecret = /^stop-secret:\s*['"]?([^'"\r\n]+)/m.exec(readFileSync(pluginConfig, "utf8"))?.[1]?.trim();
if (!stopSecret || stopSecret === "mrrw-dev-stop-secret") {
  console.error(`Set a non-default stop-secret in ${pluginConfig} first (the bridge refuses the dev default).`);
  process.exit(1);
}
const env = {
  ...process.env,
  OBSERVANCE_NEXT_DIST_DIR: ".next-playtest",
  OBSERVANCE_TSCONFIG: "tsconfig.local-build.json",
  MORROW_ACT: "1",
  MORROW_ACT_STARTED: process.env.MORROW_ACT_STARTED ?? new Date().toISOString(),
  MORROW_DESK_MINUTES: process.env.MORROW_DESK_MINUTES ?? "3",
  MORROW_MC_HOST: process.env.MORROW_MC_HOST ?? "localhost:25599",
  MORROW_PLAYERS: process.env.MORROW_PLAYERS ?? "",
  MORROW_STOP_SECRET: stopSecret,
  MORROW_BRIDGE_URL: process.env.MORROW_BRIDGE_URL ?? "http://127.0.0.1:25588",
};
delete env.MORROW_DEV;
delete env.MORROW_DESK_FAST;
const child = spawn(process.platform === "win32" ? "npx.cmd" : "npx", ["next", "dev", "-p", "3041"], { cwd, env, stdio: "inherit", shell: process.platform === "win32" });
child.on("exit", (code) => process.exit(code ?? 0));
