// fate-preview.selftest.ts — pins the dashboard fate-preview to the engine's decideFate (A2).
//
// CONSISTENCY GUARD. `previewFate` is a verbatim mirror of `discord/src/showrunner/fate.ts::decideFate`.
// These are the SAME shared cases the engine's `autonomy.selftest.ts` pins — if the mirror drifts from
// the engine, one of these fails. Cheap, dependency-free (the repo's `.selftest` convention). Run with
// `npx tsx src/app/author/fate-preview.selftest.ts`.

import { previewFate, coerceFate, type FateInput } from "./fate-preview";

let failures = 0;
function check(name: string, cond: boolean) {
  if (!cond) {
    failures++;
    console.error(`  ✗ ${name}`);
  } else {
    console.log(`  ✓ ${name}`);
  }
}

function fateInput(over: Partial<FateInput> = {}): FateInput {
  return {
    honoredActive: 0,
    violatedActive: 0,
    leftAtActive: 0,
    seventhFound: false,
    issCaught: false,
    quorumMet: false,
    refusalSignal: false,
    ...over,
  };
}

console.log("fate-preview.selftest");

// The five shared cases the engine's autonomy.selftest.ts also pins (must match exactly).
check(
  "kept: honored dominates + iss + quorum",
  previewFate(fateInput({ honoredActive: 5, violatedActive: 1, issCaught: true, quorumMet: true })).fate === "kept",
);
check(
  "cast_out: violated dominates + 2 left at",
  previewFate(fateInput({ honoredActive: 1, violatedActive: 5, leftAtActive: 2 })).fate === "cast_out",
);
check(
  "divided: dead-even spread",
  previewFate(fateInput({ honoredActive: 3, violatedActive: 3 })).fate === "divided",
);
check(
  "divided: empty arc earns neither pole (never punishes an absent/slow group)",
  previewFate(fateInput()).fate === "divided",
);
check(
  "refusers: NOT read from slowness (quorum + honored, no defiance signal)",
  previewFate(fateInput({ quorumMet: true, honoredActive: 4, issCaught: true })).fate !== "refusers",
);
check(
  "refusers: only on a positive defiance signal",
  previewFate(fateInput({ quorumMet: true, refusalSignal: true })).fate === "refusers",
);
check(
  "kept requires quorum (no quorum → not kept)",
  previewFate(fateInput({ honoredActive: 5, violatedActive: 1, quorumMet: true })).fate !== "kept",
);

// Determinism (the backstop IS the determinism — no LLM, no fallback needed).
check(
  "deterministic",
  JSON.stringify(previewFate(fateInput({ honoredActive: 4, violatedActive: 1, issCaught: true, quorumMet: true }))) ===
    JSON.stringify(previewFate(fateInput({ honoredActive: 4, violatedActive: 1, issCaught: true, quorumMet: true }))),
);

// coerceFate — the server override re-check rejects everything outside the four-value enum.
check("coerceFate accepts the four fates", ["kept", "cast_out", "divided", "refusers"].every((f) => coerceFate(f) === f));
check("coerceFate rejects a player name", coerceFate("Ethan") === null);
check("coerceFate rejects inheritors (a codicil, not a base fate)", coerceFate("inheritors") === null);
check("coerceFate rejects empty / junk", coerceFate("") === null && coerceFate(null) === null && coerceFate(42) === null);

if (failures > 0) {
  console.error(`\nfate-preview.selftest: ${failures} FAILED`);
  process.exit(1);
}
console.log("\nfate-preview.selftest: all passed");
