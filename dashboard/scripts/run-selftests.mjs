import { spawnSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const initDirectory = process.env.INIT_CWD ? resolve(process.env.INIT_CWD) : null;
const dashboardRoot = initDirectory && existsSync(join(initDirectory, 'package.json'))
  ? initDirectory
  : resolve(scriptDirectory, '..');
const repoRoot = resolve(dashboardRoot, '..');
const tsx = join(repoRoot, 'discord', 'node_modules', 'tsx', 'dist', 'cli.mjs');

if (!existsSync(tsx)) {
  console.error('dashboard selftest: missing discord/node_modules/tsx; run npm.cmd ci in ../discord first');
  process.exit(1);
}

const tests = [
  'src/lib/deployment-target.selftest.ts',
  'src/lib/arg-event-ledger.selftest.ts',
  'src/lib/arg-event-policy.selftest.ts',
  'src/lib/campaign-projection.selftest.ts',
  'src/lib/copperline-p4-archive.selftest.ts',
  'src/lib/copperline-p4-route.selftest.ts',
  'src/lib/copperline-p4-restore.selftest.ts',
  'src/lib/copperline-p2-package.selftest.ts',
  'src/lib/copperline-p5-consequence.selftest.ts',
  'src/lib/copperline-p6-consequence.selftest.ts',
  'src/lib/copperline-p7-inquiry.selftest.ts',
  'src/lib/copperline-p8-repair.selftest.ts',
  'src/lib/p8-intervention-plan.selftest.ts',
  'src/lib/copperline-p9-company.selftest.ts',
  'src/lib/copperline-p10-wren.selftest.ts',
  'src/lib/copperline-p11-averyn.selftest.ts',
  'src/lib/copperline-p12-coda.selftest.ts',
  'src/lib/v5-cases.selftest.ts',
  'src/lib/v5-web-node-policy.selftest.ts',
  'src/lib/v5-ls02-docket.selftest.ts',
  'src/lib/v5-hold-archive.selftest.ts',
  'src/proxy.selftest.ts',
  'src/lib/archive-projection.selftest.ts',
  'src/lib/record-projection.selftest.ts',
  'src/lib/record/record-oracle.selftest.ts',
  'src/lib/record/record-terminal.selftest.ts',
  'src/lib/record/record-copy.selftest.ts',
  'src/lib/runes.selftest.ts',
];

for (const relativeTest of tests) {
  const result = spawnSync(process.execPath, [tsx, join(dashboardRoot, relativeTest)], {
    cwd: dashboardRoot,
    env: process.env,
    stdio: 'inherit',
  });
  if (result.error) {
    console.error(`dashboard selftest: could not start ${relativeTest}: ${result.error.message}`);
    process.exit(1);
  }
  if (result.status !== 0) process.exit(result.status ?? 1);
}
