/**
 * seventh-reading.selftest.ts — standalone runner for the capstone integrity guard (THE SEVENTH READING).
 * Proves every fragment round-trips under its keeper's real forge cipher, Iss's acrostic corrects his
 * lie, and the six letters in fall-order spell AVERYN (== the `seventh-name` seed answer). Run:
 *   npx tsx src/forge/seventh-reading.selftest.ts
 */
import { readingSelfTest, assembledName, SEVENTH_NAME } from './seventh-reading.js';

const r = readingSelfTest();
console.log(`seventh-reading.selftest: OK (${r.passed}) — the six keepers spell "${assembledName()}"`);
for (const c of r.cases) console.log('  ✓ ' + c);
if (assembledName().toLowerCase() !== SEVENTH_NAME) {
  console.error('FAIL: assembled name != SEVENTH_NAME');
  process.exit(1);
}
