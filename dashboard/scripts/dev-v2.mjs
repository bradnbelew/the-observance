// Local test preview of the Copperline mirror on port 3040, isolated from any other dev server.
// Testing mode: the act switcher is on (bottom-right), Theo answers immediately, and the Act 10
// console talks to the local Paper test server. None of these settings exist in production.
import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";

const cwd = fileURLToPath(new URL("..", import.meta.url));
const env = {
  ...process.env,
  OBSERVANCE_NEXT_DIST_DIR: ".next-v2dev",
  OBSERVANCE_TSCONFIG: "tsconfig.local-build.json",
  MORROW_DEV: "1",
  MORROW_DESK_FAST: "1",
  MORROW_ACT: process.env.MORROW_ACT ?? "1",
  MORROW_MC_HOST: process.env.MORROW_MC_HOST ?? "localhost:25599",
  MORROW_PLAYERS: process.env.MORROW_PLAYERS ?? "Tester",
  // The mrrw bridge. By default the fake one morrow/world/e2e_site.py runs on 25591 (when it isn't
  // running the site just uses the env above). Point it at a real plugin to test with Paper.
  MORROW_BRIDGE_URL: process.env.MORROW_BRIDGE_URL ?? "http://127.0.0.1:25591",
  MORROW_BRIDGE_TTL_MS: process.env.MORROW_BRIDGE_TTL_MS ?? "0",
};
const child = spawn(process.platform === "win32" ? "npx.cmd" : "npx", ["next", "dev", "-p", "3040"], { cwd, env, stdio: "inherit", shell: process.platform === "win32" });
child.on("exit", (code) => process.exit(code ?? 0));
