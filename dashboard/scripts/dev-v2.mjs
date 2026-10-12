// Local preview of the Copperline mirror on port 3040, isolated from any other dev server.
// MORROW_ACT defaults to 3 here so the whole Acts 1-3 slice is reachable.
import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";

const cwd = fileURLToPath(new URL("..", import.meta.url));
const env = { ...process.env, OBSERVANCE_NEXT_DIST_DIR: ".next-v2dev", OBSERVANCE_TSCONFIG: "tsconfig.local-build.json", MORROW_ACT: process.env.MORROW_ACT ?? "3" };
const child = spawn(process.platform === "win32" ? "npx.cmd" : "npx", ["next", "dev", "-p", "3040"], { cwd, env, stdio: "inherit", shell: process.platform === "win32" });
child.on("exit", (code) => process.exit(code ?? 0));
