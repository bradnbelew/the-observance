import { existsSync, readFileSync } from 'node:fs';
import { join } from 'node:path';

const root = process.cwd();
const terminal = readFileSync(join(root, 'src/app/record/terminal/page.tsx'), 'utf8');
const route = readFileSync(join(root, 'src/app/record/terminal/inscribe/route.ts'), 'utf8');
const retiredForm = join(root, 'src/app/record/terminal/InscribeForm.tsx');

for (const required of ['82', '10 cases', 'filing discipline', 'this docket is read-only']) {
  if (!terminal.includes(required)) throw new Error(`V5 terminal copy missing: ${required}`);
}
if (existsSync(retiredForm)) throw new Error('V5 terminal must not ship the arbitrary-name inscription form');
if (!route.includes('status: 410') || !route.includes("'Cache-Control': 'no-store'")) {
  throw new Error('retired inscription POST must fail closed, permanently and without caching');
}
if (route.includes('resolveInscription') || route.includes('SUPABASE_SERVICE_ROLE_KEY')) {
  throw new Error('retired inscription POST must not reach the service-role answer resolver');
}
for (const stale of ['marks unkept', 'a hand. a mark.', '>answer</span>', 'seventh']) {
  if (`${terminal}\n${route}`.toLowerCase().includes(stale)) throw new Error(`V5 terminal retains stale copy: ${stale}`);
}

console.log('record-copy.selftest OK: read-only V5 docket; arbitrary-name/service-role bypass removed');
