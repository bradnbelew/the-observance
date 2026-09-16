import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import {
  antiPuzzleRoomRules,
  directorControls,
  discoveries,
  evidenceFormats,
  handmadeMedia,
  investigationTrails,
  mysteryRules,
  morrowVoice,
  recurringMotifs,
  reviewActs,
  reviewGates,
  scares,
  websiteInvestigations,
  weekScript,
} from './morrow-review';
import { MORROW_MEDIA_CATALOG, MORROW_MEDIA_REVIEW_CATALOG } from './morrow-media-catalog';

assert.equal(reviewActs.length, 5);
assert.equal(reviewGates.length, 15);
assert.equal(new Set(reviewGates.map((gate) => gate.id)).size, 15);
assert.deepEqual(reviewGates.map((gate) => gate.id), Array.from({ length: 15 }, (_, i) => `G${String(i + 1).padStart(2, '0')}`));
assert.equal(discoveries.length, 24);
assert.equal(discoveries.filter((entry) => entry[2] === false).length, 9);
assert.equal(morrowVoice.length, 9);
assert.equal(morrowVoice.at(-1)?.[0], 'SILENT');
assert.equal(scares.length, 12);
assert.equal(mysteryRules.length, 6);
assert.equal(antiPuzzleRoomRules.length, 5);
assert.equal(recurringMotifs.length, 5);
assert.equal(investigationTrails.length, 6);
assert.equal(websiteInvestigations.length, 8);
assert.equal(handmadeMedia.length, 12);
assert.equal(evidenceFormats.length, 21);
assert.equal(weekScript.length, 7);
assert.equal(directorControls.length, 9);

const page = readFileSync(resolve('src/app/review/morrow/page.tsx'), 'utf8');
for (const required of [
  'Morrow is the phenomenon',
  'Deepen the wrongness, not the lock count',
  'Anti-puzzle-room firewall',
  'Six questions pursued through every kind of trace',
  'Twelve artifacts that feel handled, recorded, damaged, and retained',
  'Evidence format atlas',
  'Fifteen gates',
  '24 meaningful discoveries',
  'Week-long launch script',
  'Director dashboard concept',
  'PRODUCTION LOCKED',
  'No Supabase, Discord, Minecraft, or production mutation',
]) assert.ok(page.includes(required), `review page lacks: ${required}`);
for (const forbidden of [
  'createAdminClient',
  'createClient(',
  'SUPABASE_SERVICE_ROLE_KEY',
  'fetch(',
  "'use client'",
]) assert.equal(page.includes(forbidden), false, `static review page contains forbidden runtime dependency: ${forbidden}`);

const fixture = JSON.parse(readFileSync(resolve('../morrow/review/review-fixtures.json'), 'utf8')) as {
  deployable: boolean;
  production_mutations_allowed: boolean;
  minecraft_locations: unknown[];
  dread_score: Array<{ id: string; maximum_seconds: number; required_for_progress: boolean }>;
  media: unknown[];
  director_fixture: { controls_enabled: boolean; arbitrary_command_allowed: boolean; arbitrary_message_allowed: boolean };
};
assert.equal(fixture.deployable, false);
assert.equal(fixture.production_mutations_allowed, false);
assert.equal(fixture.minecraft_locations.length, 8);
assert.equal(fixture.dread_score.length, 12);
assert.equal(fixture.media.length, 12);
assert.equal(new Set(fixture.dread_score.map((beat) => beat.id)).size, 12);
assert.ok(fixture.dread_score.every((beat) => beat.maximum_seconds <= 300));
assert.equal(fixture.director_fixture.controls_enabled, false);
assert.equal(fixture.director_fixture.arbitrary_command_allowed, false);
assert.equal(fixture.director_fixture.arbitrary_message_allowed, false);

assert.equal(MORROW_MEDIA_CATALOG.length, 0,
  'design-only v2 media must not enter the historical earned-media delivery path');
assert.equal(MORROW_MEDIA_REVIEW_CATALOG.length, 12);
assert.ok(MORROW_MEDIA_REVIEW_CATALOG.every((asset) => asset.required_hash_before_release));

console.log('MORROW REDESIGN REVIEW: PASS acts/gates/discoveries/mystery/investigation/web/media-formats/anti-room/voice/scares/week/director/static-boundary/media-fail-closed');
