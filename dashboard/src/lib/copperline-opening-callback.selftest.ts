import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const read = (path: string) => readFileSync(resolve(path), 'utf8');
const archive = read('src/app/community/archive.php/page.tsx');
const ticket = read('src/app/support/ticket.php/page.tsx');
const packageReview = read('src/app/community/archive/package-review/page.tsx');
const camp = read('src/app/community/archive/ash-camp/page.tsx');

assert.ok(archive.includes("params.locker === undefined"), 'P1 archive route must not require the later locker answer');
assert.ok(archive.includes("eventKey: 'p1.attachment_history_restored'"));
assert.ok(archive.includes("idempotencyKey: 'copperline:p1:service-1842-ticket-9137-history'"));
assert.ok(!archive.includes('copperline:p1:service-1842-ticket-9137-locker-13'));
assert.ok(archive.includes("hasCampaignEvent('p9.company_biographies_restored')"), 'P9 media route must stay earned');
assert.ok(archive.includes("['A06']") && archive.includes("['A07']"));
assert.ok(ticket.includes('/community/archive.php?service=1842&amp;ticket=9137'));
assert.ok(!ticket.includes('/community/archive.php?service=1842&amp;ticket=9137&amp;locker=13'));
assert.ok(packageReview.includes('/community/archive.php?service=1842&amp;ticket=9137'));
assert.ok(!packageReview.includes('locker=13'));
assert.ok(camp.includes('/community/archive.php?service=1842&amp;ticket=9137&amp;locker=13'));

console.log('Copperline opening/P9 callback split: PASS');
