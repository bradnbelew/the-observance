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
  console.error('morrow reboot selftest: missing discord/node_modules/tsx; run npm.cmd ci in ../discord first');
  process.exit(1);
}

const tests = [
  'src/lib/morrow-review.selftest.ts',
  'src/lib/morrow-copperline-seed.selftest.ts',
  'src/lib/morrow-full-rehearsal.selftest.ts',
];

for (const relativeTest of tests) {
  const result = spawnSync(process.execPath, [tsx, join(dashboardRoot, relativeTest)], {
    cwd: dashboardRoot,
    env: process.env,
    stdio: 'inherit',
  });
  if (result.error) {
    console.error(`morrow reboot selftest: could not start ${relativeTest}: ${result.error.message}`);
    process.exit(1);
  }
  if (result.status !== 0) process.exit(result.status ?? 1);
}

const readinessResult = spawnSync('python', [join(repoRoot, 'tools', 'check_morrow_production_readiness.py')], {
  cwd: repoRoot,
  env: process.env,
  stdio: 'inherit',
});
if (readinessResult.error) {
  console.error(`morrow reboot selftest: could not start production readiness check: ${readinessResult.error.message}`);
  process.exit(1);
}
if (readinessResult.status !== 0) process.exit(readinessResult.status ?? 1);
