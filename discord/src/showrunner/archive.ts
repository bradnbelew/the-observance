/**
 * archive.ts — the PURE half of the Recovery Archive materializer (W3a).
 *
 * THE GAP THIS CLOSES. The reshape authored 42 place-anchored `thread_cards` (the reconstruction
 * field: who / place / happened / surface / human) with each body held behind a `body_voice_key` in
 * `voice.archive.ts` (INV-1 — the engine never hardcodes story; canon.ts GUARD-9 pins every key to an
 * entry there). The Record website can only read anon-safe SQL views, and the card BODIES are not in
 * the DB — only the keys are. So the archive was authored, voice-covered, self-tested, and invisible.
 *
 * This module resolves each card's `body_voice_key` → its recovered text via `archiveLine` (the single
 * authored source), producing the rows the run half upserts into `public.thread_card_bodies`, which the
 * `v_archive` SECURITY DEFINER view then reveal-gates for anon (0007_v_archive.sql). The reveal lives in
 * SQL (solves + flags); this side only supplies text — so voice.archive.ts stays the one place the
 * English lives, and the DB is merely its realization.
 *
 * PURE + DETERMINISTIC: no DB, no clock, no LLM. Same input → same output, so `archive.selftest.ts` can
 * pin it. The I/O (read the card keys, upsert the bodies) is `archive.run.ts`.
 */
import { archiveLine, npcLine } from '../voice.archive.js';

/** One card's identity + the voice key its body is authored behind (the shape the run half reads). */
export interface ArchiveCardKey {
  card_key: string;
  body_voice_key: string | null;
}

/** A resolved body row, ready to upsert into `public.thread_card_bodies`. */
export interface ArchiveBodyRow {
  card_key: string;
  body: string;
}

/**
 * Resolve each card's authored body from `voice.archive.ts`. The archive spans TWO registers, each with
 * its own resolver (the separation law — `archiveLine` never returns a SET-A line where a Watcher line is
 * expected, and vice-versa): the Watcher-register `archive` (most cards) and the SET-A human `npcLines`
 * (the three found-testimony surface cards — Aro's lie, Wenna's charm, Pell's truth). The materializer is
 * the one place that legitimately writes the WHOLE archive store, so it tries the Watcher resolver first
 * and falls back to the NPC resolver — never blurring them at a Watcher call site, only assembling the DB
 * realization of both. A card whose `body_voice_key` is null or resolves in NEITHER register (should
 * never happen — GUARD-9 enforces coverage) is SKIPPED, never emitted with a placeholder or a leaked
 * identifier: it simply has no body row, so `v_archive`'s inner join will not surface it. Deterministic
 * order (input order preserved) for a stable self-test.
 */
export function buildArchiveBodies(cards: readonly ArchiveCardKey[]): ArchiveBodyRow[] {
  const rows: ArchiveBodyRow[] = [];
  for (const c of cards) {
    if (!c.body_voice_key) continue;
    const body = archiveLine(c.body_voice_key) ?? npcLine(c.body_voice_key);
    if (body == null) continue; // resolves in neither register — skip (spoiler-/slop-safe).
    rows.push({ card_key: c.card_key, body });
  }
  return rows;
}
