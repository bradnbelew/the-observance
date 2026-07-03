/**
 * relief.ts — the PURE policy for the relief/exhale beats (W3e — pacing / Warm-Grief).
 *
 * After a heavy CLIMAX the world EXHALES: the record surfaces a warm MEMORY — the Hold when it was
 * alive, a keeper who was loved — so weeks of dread don't fatigue (OVERHAUL Pillar 2, Warm-Grief). The
 * relief is warm in SUBJECT, never in tone: it reuses an existing recovered-material archive body (the
 * who-cards), so the cold Watcher register holds unbroken; the only warmth is that the record, amid the
 * horror, still remembers these were people. It is honest flavor — a memory, read as a memory — never a
 * puzzle or a gate (the consistency principle: pure relief/lore is welcome when it reads as what it is).
 *
 * PURE + DETERMINISTIC (no DB, no clock, no LLM): given the arc flags, return which relief beats have
 * newly earned their exhale — their climax flag is set AND they have not yet been relieved. The run half
 * (autonomy.run.ts) resolves each bodyKey via voice.archive.ts and posts it ONCE to #the-record, setting
 * the relievedFlag so it never re-fires. Same flags in → same decision out, so relief.selftest can pin it.
 */

export interface ReliefBeat {
  /** the climax flag whose setting earns this exhale. */
  climax: string;
  /** the set-once idempotency flag written after the relief posts (so it never re-fires). */
  relievedFlag: string;
  /** the voice.archive.ts key of the warm-subject recovered body to surface (an existing who-card). */
  bodyKey: string;
  /** a short log label. */
  label: string;
}

/**
 * The climax → exhale map. Deliberately SPARSE + well-spaced (two beats across the whole arc), so relief
 * stays a rare grace, never a habit:
 *   - undercroft_open (you descend into the keepers' dead world) → the deep market ALIVE, so the grief
 *     you are walking into has something to grieve (thread-archive WHO-01's own placement note).
 *   - iss_caught (you catch Iss as the tragic betrayer) → Iss REMEMBERED KINDLY (the best of the young
 *     ones, who mended Mara's bellows and took nothing) — the man, not only the crime.
 */
export const RELIEF_BEATS: readonly ReliefBeat[] = [
  { climax: 'undercroft_open', relievedFlag: 'relieved_market', bodyKey: 'cardWhoDeepMarket', label: 'the market, alive' },
  { climax: 'iss_caught',      relievedFlag: 'relieved_iss',    bodyKey: 'cardWhoIssFriend',  label: 'iss, remembered' },
];

/** Return the relief beats whose climax has fired and that have not yet exhaled. Pure + deterministic. */
export function decideRelief(flags: Record<string, unknown>): ReliefBeat[] {
  return RELIEF_BEATS.filter((r) => flags[r.climax] === true && flags[r.relievedFlag] !== true);
}
