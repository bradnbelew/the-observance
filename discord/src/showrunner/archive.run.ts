/**
 * archive.run.ts — the I/O half of the Recovery Archive materializer (W3a).
 *
 * Reads every `thread_cards` row's (card_key, body_voice_key), resolves each body from voice.archive.ts
 * (the pure `buildArchiveBodies`), and UPSERTS the resolved text into `public.thread_card_bodies` — the
 * store the `v_archive` view joins to reveal cards to anon (0007_v_archive.sql). It writes ALL bodies,
 * not just revealed ones: the REVEAL is enforced in SQL (v_archive's WHERE, over solves + flags), so a
 * body sitting in this service-role-only table is never anon-readable until its card is earned. Writing
 * the full set keeps the materializer stateless — it does not need to know the game's progress, only the
 * authored text — and idempotent (upsert on card_key), so it is safe to run every cron tick or once at
 * deploy.
 *
 * Fault-isolation: this is called inside run.ts's isolated pass block (a throw here can never abort the
 * spine the tick already applied) and also guards its own reads. Bodies are static authored text, so a
 * failed materialize simply leaves the archive at its last-known state (or empty on a fresh DB — the
 * sealed baseline the Record page already degrades to).
 */
import { supabase } from '../db/client.js';
import { buildArchiveBodies, type ArchiveCardKey } from './archive.js';

/**
 * Resolve + upsert every card body into `public.thread_card_bodies`. Returns the count written (0 on
 * any read failure — never throws to the caller's spine when wrapped; the standalone entry surfaces
 * errors via exit code). Idempotent: re-running overwrites the same rows with the same text.
 */
export async function materializeArchive(): Promise<{ materialized: number }> {
  const { data, error } = await supabase
    .from('thread_cards')
    .select('card_key, body_voice_key');

  if (error) throw error;
  const cards = (data ?? []) as ArchiveCardKey[];

  const rows = buildArchiveBodies(cards).map((r) => ({ ...r, updated_at: new Date().toISOString() }));
  if (rows.length === 0) return { materialized: 0 };

  const { error: upsertErr } = await supabase
    .from('thread_card_bodies')
    .upsert(rows, { onConflict: 'card_key' });

  if (upsertErr) throw upsertErr;
  return { materialized: rows.length };
}
