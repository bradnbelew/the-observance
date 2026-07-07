// record-copy.selftest.ts - guards the public Record terminal against web-form/puzzle-course copy.
//
// The Record can accept answers technically, but the player-facing surface should read like a recovered
// archive lectern, not a normal "submit your answer" website. This intentionally scans only small,
// explicit public strings that have regressed before.
import { readFileSync } from "node:fs";
import { join } from "node:path";

let failures = 0;
function check(label: string, ok: boolean) {
  if (!ok) {
    failures += 1;
    console.error(`  FAIL ${label}`);
  } else {
    console.log(`  ok ${label}`);
  }
}

const root = process.cwd();
const terminalPage = readFileSync(join(root, "src/app/record/terminal/page.tsx"), "utf8");
const inscribeForm = readFileSync(join(root, "src/app/record/terminal/InscribeForm.tsx"), "utf8");

console.log("record-copy.selftest");

check("terminal says marks unkept, not entries unresolved", terminalPage.includes("marks unkept"));
check("terminal does not expose entries unresolved as player copy", !terminalPage.includes("entries unresolved"));
check("inscription body says a hand and a mark", terminalPage.includes("a hand. a mark."));
check("inscription header stays in archive register", terminalPage.includes("// a hand and a mark"));
check("form field label is mark", inscribeForm.includes(">mark</span>"));
check("form field label is not answer", !inscribeForm.includes(">answer</span>"));

if (failures > 0) {
  console.error(`\nrecord-copy.selftest: ${failures} FAILED`);
  process.exit(1);
}

console.log("record-copy.selftest OK");
