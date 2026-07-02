// record-oracle.selftest.ts — pins the website's normalize + gate replicas to the oracle contract.
//
// The record website is a THIRD answer surface; its normalize/match MUST stay byte-for-byte identical
// to discord/src/oracle (normalize.ts, gate.ts) and the plugin's Java twin, or the closed loop
// desyncs silently. These are the camera-guard for that. Run: `npx tsx src/lib/record/record-oracle.selftest.ts`.

import { normalizeAnswer, MAX_RAW_LEN } from "./normalize";
import { flagsSatisfied, matchPuzzles, type RecordPuzzle } from "./gate";

let failures = 0;
function check(name: string, cond: boolean) {
  if (!cond) {
    failures++;
    console.error(`  ✗ ${name}`);
  } else {
    console.log(`  ✓ ${name}`);
  }
}

console.log("record-oracle.selftest");

// 1. normalize — the ORACLE.md §2 battery (the canonical tricky inputs).
check("bow,at → bow at", normalizeAnswer("BOW,AT") === "bow at");
check("bow at → bow at", normalizeAnswer("bow at") === "bow at");
check("-1280, 64 → 1280 64", normalizeAnswer("-1280, 64") === "1280 64");
check("collapse + trim", normalizeAnswer("  the   OLD   name  ") === "the old name");
check("ligature ﬁre → fire", normalizeAnswer("ﬁre") === "fire");
check("fullwidth ABC123 → abc123", normalizeAnswer("ＡＢＣ１２３") === "abc123");
check("Roman Ⅻ → xii", normalizeAnswer("Ⅻ") === "xii");
check("greek strips to empty", normalizeAnswer("ΣΊΣΥΦΟΣ") === "");
check("emoji becomes a break", normalizeAnswer("a😀b") === "a b");
check("empty stays empty", normalizeAnswer("   ") === "");
check("MAX_RAW_LEN is 512", MAX_RAW_LEN === 512);

// 2. flagsSatisfied — the storylet gate.
check("empty requires → open", flagsSatisfied({}, {}) === true);
check("null requires → open", flagsSatisfied(null, {}) === true);
check("missing flag → closed", flagsSatisfied({ a: true }, {}) === false);
check("falsy flag → closed", flagsSatisfied({ a: true }, { a: false }) === false);
check("truthy flag → open", flagsSatisfied({ a: true }, { a: true }) === true);
check("all-of gate", flagsSatisfied({ a: true, b: true }, { a: true }) === false);

// 3. matchPuzzles — whole-string set-membership only.
const P = (k: string, ans: string[]): RecordPuzzle => ({
  puzzle_key: k,
  accepted_answers: ans,
  outcome_type: "next_clue",
  outcome_payload: {},
  active: true,
  max_attempts: null,
  requires_flags: {},
  thread_key: null,
});
const web = [P("p1", ["bow at"]), P("p2", ["the old name", "old name"]), P("p3", ["bow at"])];
check("empty normalized → no match", matchPuzzles(web, "") .length === 0);
check("exact match → both p1+p3", matchPuzzles(web, "bow at").map((p) => p.puzzle_key).join(",") === "p1,p3");
check("no substring match", matchPuzzles(web, "bow").length === 0);
check("alt answer matches", matchPuzzles(web, "old name").map((p) => p.puzzle_key).join(",") === "p2");

if (failures > 0) {
  console.error(`\nrecord-oracle.selftest: ${failures} FAILED`);
  process.exitCode = 1;
} else {
  console.log("\nrecord-oracle.selftest: all passed");
}
