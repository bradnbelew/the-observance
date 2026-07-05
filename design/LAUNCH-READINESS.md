# THE OBSERVANCE — LAUNCH READINESS (2026-07-03)

The state after the "build everything left + full first-contact→finale pass" push. Branch
`feat/build-everything-2026-07-01`, all committed, **every surface green** (plugin jar · discord tsc +
9 selftests · dashboard tsc + 2 selftests · datapack JSON). Nothing pushed.

The experience is **code-complete and launch-ready** end to end — from the first thing a player
experiences to the finale close — barring the MANUAL items below (real media + ops that are yours to
make/apply, which the code already degrades around safely).

---

## 1. What shipped this push (all green)

- **W4.2 — Observer Tier-2 LLM archivist.** Opus 4.8 picks the most uncanny REAL quote to echo; pure
  selection (never composes text); fires ≤once/12h; degrades to the deterministic pick on any failure.
- **W5 — the voice tier ("it heard you SAY it").** Discord voice → Whisper → observations → the same
  Observer echo, in a distinct "heard aloud" register. Off by default; optional deps; fault-isolated.
  - Closed the `spine-spoken-name` orphan: speaking "the one who turned away" → the Watcher quotes it back.
- **W8 — cohesion + hardening audit.** Fixed the real blocker (`CoopPlateListener` / `m4-three-hands`
  had no producer → the primary M4 "true walk" chain was unreachable; now a faithful cross-surface coop
  gate). Fixed the `beat_queue` `failed`-status deploy hazard (idempotent migration 0009). Voice-dispatch
  cohesion + plugin polish. Everything else verified sound (normalizer twins, flags, reveal-gate, finale reachable).
- **W9 — journey pass, the two finale BLOCKERS fixed:**
  - **The ending now posts.** Built the fate sentinel — `ending_fate` was never written, so the composed
    divergent close never reached players. Now decided from the active-only compliance spread at the Accepting instant.
  - **The reckoning is now felt.** condemn/free → the "it heard you" echoes cease (announced once); understand
    → they persist as sorrow. (`observer.run.ts` had no reckoning gate.)
  - Rosetta literacy KEY made concrete (rune↔plaintext crib pairs — the "these are letters" turn).
  - Mara (the thinnest keeper) given her 2nd clue.
- **A3 — keeper-record wired (post-W9, Ethan-approved).** The Hold-Book now writes each living player in
  (living row → keeper heading → the keeper's own hand) from their measured habit, re-filling the lectern
  on each tier crossing. Delivers the authored `keeperPage*` pages that were wired nowhere before. Mapping:
  hoards→vaun, reads→mara, wanders→sella, silent→orin, night-walks→brann, spends-words→iss.

---

## 2. MANUAL go-live actions (yours — the code degrades safely until each is done)

**THE RELEASE finale (design/FINALE-THE-RELEASE.md — BUILT 2026-07-03)**
- [ ] `/observance finale` at the Seventh's chamber (`the_unwriting`) — places the 3 finale-rite markers
      (seventh restore · seventh erase · the release). NOTE: this ALSO fixes a latent gap — nothing spawned
      the Seventh-choice markers before either, so the old finale rite was unstageable too. Both fixed.
- [ ] THE SEVENTH READING capstone (design/THE-SEVENTH-READING.md — BUILT): the Seventh's name is
      **AVERYN**, derived by the group from six distributed keeper fragments (one per keeper, each in a
      different technique) and SAID to trigger the finale. Run **`/observance reading`** (after the keeper
      sites are placed) to stamp all six fragment carvings automatically — no hand-carving; then place
      Mara's capstone shelf books (THE-SEVENTH-READING.md §3). Trigger/flag-chain/confirmation/hint-rail/
      carving all wired. (Restore = say AVERYN; erase = the release marker, unnamed.) The `seventh_name`
      setting hook still exists if you ever want to override.
- [ ] (optional) `closing.whitelist-after: true` in config.yml if you want "nothing to come back to"
      enforced (whitelist flips ON after the kick — reversible, NEVER a ban). Default: they can rejoin.
- [ ] `closing.enabled` default true = full auto (world dies + kick). false = the farewell still posts, you
      stage the kick by hand. Tune `closing.theater-seconds` (default 8) to taste.

**Apply / deploy**
- [ ] Apply the Supabase migrations to the live DB (both lineages, in order). Newest this push:
      `discord/0009_observations`, `dashboard/0009_beat_queue_failed_status` (idempotent), and the earlier
      `dashboard/0007_v_archive` + `0008_v_archive_flag_gate` (you said 0007/0008 are already in).
- [ ] Re-seed so the gated rows are lit (progression/metapuzzle) — the Nether/End lanes are `active=true`.
- [ ] `npm run archive:materialize` in `discord/` (or let the cron self-populate the archive bodies).
- [ ] Host the resourcepack (`observance-resourcepack.zip`) and set `config.yml resource-pack.url`+`sha1`.
      **The rune font ships in the pack** — the rosetta cribs + all rune beats only render once it's live.
- [ ] Survey + `placeworld` the sites, incl. the two new coop/lane sites: `coop_plate` (the three-hands
      gate) and the Nether/End lane sites (from their own dimensions). An unplaced site = a safe no-op.
- [ ] Stage the cold open in the group's base before session zero: `/observance placeprologue`.

**Turn on the optional tiers (each OFF by default; all degrade to silence)**
- [ ] Observer capture: flip the `observer_capture` setting on when ready.
- [ ] Observer Tier-2: set `ANTHROPIC_API_KEY` in the **Render cron** env (the showrunner runs there, not Vercel).
- [ ] Voice tier: set `DISCORD_VOICE_CHANNEL_ID` + a Whisper backend (`WHISPER_API_URL`/`_KEY` or `WHISPER_BIN`)
      + flip the `voice_capture` setting, then restart the bot. (Deps auto-install as optionalDependencies.)

**Real media (the hero artifacts — the code has the wiring and safely withholds live lures until files exist)**
- [ ] `dashboard/public/the-hold/the-hold.zip` — the offline "cursed map" the lure page links. Until it's
      hosted, **don't plant the in-world clue to the lure slug** (`/record/the-record-keeps`) — the link 404s otherwise.
- [ ] The found-footage clip + the recovered Drive folder + the waveform/spectrogram image — for
      `spine-recovered-archive` (a wired puzzle awaiting its artifact) and the coords-in-a-frame first-find aid.

**Security / ops**
- [ ] Rotate the previously-exposed `service_role` + Discord bot creds.
- [ ] Run the first vertical-slice playtest.

---

## 3. Deferred enhancements (designed, NOT half-shipped — each flagged honestly)

These are real "could be more" items. None is a blocker; none is shipped in a degraded state (no
half-ready code). Ethan's decisions on the earlier open items are recorded here.

- **REFUSERS ending — DECIDED OUT (Ethan).** Not wanted. The fate sentinel still reads `flags.refusal_signal`
  (nothing sets it), so REFUSERS stays a designed-but-dormant fifth ending; the other three fates
  (kept/cast_out/divided) are the live set. To revive it later, add a plugin refusal-rite that sets the flag.
- **Self-firing cold open — DECIDED: operator-staged (Ethan).** The base anomaly is staged by
  `/observance placeprologue` (you control the base edit). No autonomous base-editing is built (intentional).
- **`keeper.ts` (keeper-rhyme NPC beat).** Separate from the now-wired keeper-record: an in-world keeper NPC
  line that rhymes with a player's habit. Pure + self-tested, still unwired (no beat producer). Lower value
  than keeper-record (which delivers the same rhyme via the Hold-Book); leave until it has a clear surface.
- **Early first-find trailhead for a sky-blocked first site.** The recovery needle is a late (post-Seventh)
  aid; the first-find coords are meant to come from your hero artifact. If a keeper site is sky-blocked (no
  beacon beam) AND no coord artifact is authored, a group could lack a trailhead. Mitigation: ensure the
  first site is beacon-visible or its coords are in the first artifact.
- **Six-prior-groups slow burn / diverse-puzzle archive cards.** The richest slow-burn is 3 cards; a few
  more per-group traces + thread cards on the payoff expansion puzzles would deepen the reconstruction web.
  Pure content adds (thread_cards.sql + voice.archive.ts bodies), no engine change.

---

## 4. Director coherence guardrails (built)

`m4-three-hands` slipped because player-facing continuity was being verified by large manual audits instead
of one repeatable check. The current launch branch now has a director-level check:

```powershell
python tools\check_experience_coherence.py
```

It verifies that Recovery Archive cards resolve to real voice bodies, card references point at real cards,
archive anchors point at real `sites.yml` ids, revealed-by-solve values point at real puzzle keys, side
quests use real thread lanes, and the experience manifest exists with the major lanes represented.

Keep running it beside:

```powershell
python tools\check_namespace_collisions.py
python tools\check_voice_register.py
```

This does not replace the lower-level producer/token checks in the TS/Java pipeline. It closes the recurring
director problem: a player-facing thread exists in one file but silently dangles in another.

---

## 5. 2026-07-03 audit pass — fixed, and what's still open

A from-scratch adversarial audit (not trusting this doc's own "verified sound" claims) plus a manual
playtest trace (cold-open → the M4/Iss chain → finale → companion reckoning). Two real bugs fixed and
committed on this branch; everything re-verified green after each.

**Fixed (commits `bb72be5`, `5a47b35`):**
- **The resource-pack PUSH half was never registered — launch-blocking.** `ResourcePackPusher.java` was
  complete, self-tested, and config-driven, but `ObservancePlugin` only ever registered its RECEIVE
  counterpart (`ResourcePackTracker`). Practical effect: completing the go-live step "host the pack, set
  config.yml url/sha1" (§2) would have done *nothing* — no player is ever prompted, so the rune font (and
  every rosetta/rune beat depending on it) would silently never render. Now registered; inert-and-safe
  until the URL is set, as designed.
- **The curatorial drip pool didn't apply the reveal-gate (`requires_flags`).** `snapshot.ts` fed
  `decide.ts` every `active=true` puzzle row without checking `flagsSatisfied` against `arc_state.flags`
  — unlike the real answer oracle (`getOpenPuzzles`), which does. A flag-gated-but-not-yet-open row (e.g.
  `m4-three-hands` waiting on `bound_word_known`) could be dripped as a hint before it was actually
  solvable (a moon-logic dead end for the group attempting it), and once dripped it is *permanently*
  excluded from future announcements — so the real in-world pointer would never fire once the gate
  genuinely opened. Fixed via a new optional `SnapshotPuzzle.flagsOpen`, back-compat with every existing
  test fixture.
- A minor register-separation violation: `oracle/resolve.ts`'s lore-fallback line was hardcoded inline
  instead of sourced from `voice.ts`. Moved into `voice.oracleLore()`.

**2026-07-05 — Unlit Deep BUILT (Ethan approved the build).** `UnlitDeepListener.java` (BlockPlace +
player-attributed `BlockIgniteEvent` "held-flame edge" detection, Y ≤ `deep-line-y`, taboo moon phase,
GROUP-scoped cooldown via a single shared rate-limiter key — never a per-player tally, matching
`CustomComplianceListener`'s own note on this boundary) + `TrackerConfig.CUSTOM_UNLIT_DEEP` + the
`customs.unlit-deep`/`restraint.enabled` config resolution, registered in `ObservancePlugin`. On a fresh
group-latch break it merges `unlit_deep_broken_at`/`unlit_deep_broken_by` into `arc_state.flags`
(recorded, never spoken/messaged — a downstream discord pass owns the telling). The Discord side
(`unlit-deep.ts` pure policy + `unlit-deep.run.ts` I/O, wired into the showrunner tick beside
`runCustomsPass`) posts the already-authored `voice.tollUnlitDeep()` line to #the-record once per fresh
break, idempotent on a high-water mark (`state.unlit_deep_last_reported_at`). **Not built (honestly
deferred, not half-shipped): the KEPT side** (`voice.keptUnlitDeep()` is authored and ready) — it needs a
per-black-moon-night idempotency signal ("was THIS black moon already reported kept or broken?") that
nothing currently supplies cleanly from the Discord side; a good next step once there's a per-night
marker to key off. **Also not built: the reward's visual payoff** ("the never-doused fire lends its
glow") — the config's own "gated behind the Undercroft fire (FACT 11) existing" note means this is
conditional on a physical light-source structure that hasn't been placed as a site yet; the flag write is
ready for whatever beat eventually reads it. All 4 surfaces verified green after the build.

**Found, NOT fixed — needs your call, not a unilateral build:**
- ✅ **CLOSED (2026-07-05 cohesion pass) — the plugin-side half of the `keeper.ts` gap.**
  `KeeperNpcListener` is now registered in `ObservancePlugin.registerListeners()`, and a real body/tag
  manager (`KeeperNpc.java`, mirrors `WrenNpc.java`) exists — `/observance keeper <spawn|despawn> [node]`
  places him (Citizens2 when present, an armor-stand fallback otherwise) and stamps the `keeper_npc` PDC
  key the listener reads. Right-clicking the presiding Keeper now genuinely fires (writes an `event_log`
  row, type `keeper`/`npc.open`) — this was previously a complete no-op. **Still open, separately:**
  whether a showrunner runner consumes that `npc.open` row and calls the already-pure, already-self-tested
  `resolveKeeperDialogue` (`keeper.ts`) to post a personalized `KeeperNpcBeat` — I did not build or verify
  that runner this pass, so don't assume the full personalized-dialogue path is proven end-to-end yet.
  That's the one remaining leg, and it's a TS-side scoping decision, not a blocker to spawning/clicking the
  Keeper working at all.
- **Two Nether/End `progression_seed.sql` rows reference voice keys that don't exist**
  (`nether.forgeArrive`, `end.shrineArrive`, plus `nether.soulSand`/`end.outsideRecord` named in its own
  header). Harmless today (`active=false`, staged), but will speak nothing the moment you place those
  Nether/End sites and flip them on — worth writing the four lines before that go-live step, not after.
- Smaller cosmetic/config drift (no functional impact, not fixed): `event-window.*` and
  `customs.false-law.enabled` are documented in config.yml but read by no code; `herd.pale-cosmetic-pdc-key`
  is declared but the actual PDC key is hardcoded elsewhere (moot today since nothing writes the tag either
  way — the "cosmetic Pale" producer doesn't exist yet, the same "no producer yet" shape Unlit Deep was
  in before 2026-07-05); `SceneAwareness.java` (util) is unreferenced anywhere.

**2026-07-03 (later) — THE RELEASE built + full-ARG playability audit.** Built the unified finale (see
above / FINALE-THE-RELEASE.md). Then a from-scratch playability pass across ALL content (not just the
spine), John's-POV:
- **Producer-token coverage: CLEAN.** All 14 plugin-produced puzzles (hoard/bookshelf/lectern/group-walk/
  shore/bow/frame-dials/black-moon/silence/vault/accepting/coop/seventh/release) have a registered listener
  posting a token that byte-matches the seed's accepted_answer; the voice-tier `spine-spoken-name` token
  matches too. The m4-three-hands-class bug (producer with no/mismatched token) does NOT recur anywhere.
- **Reachability: CLEAN.** Every one of the 13 `requires_flags` gate keys has a writer (a puzzle set_flags
  or a plugin listener) — no puzzle is unreachable for lack of a flag producer. The new finale chain
  (bowed_as_one → reveal → record_released → ending_fate → the_closing) is fully wired end to end.
- **Finale-marker gap: FIXED** (`/observance finale`) — both the release AND the pre-existing Seventh-choice
  markers were unstageable (nothing spawned them); one command now places all three.
- **Nether/End "phantom voice keys": FALSE ALARM, hardened.** The earlier consistency audit flagged
  `nether.forgeArrive`/`end.shrineArrive` as non-existent keys that would speak nothing. Traced live: both
  rows are `outcome_type: lore`, so the resolver's lore fallback reads their authored `voice_args.fragment`
  verbatim — they always worked. Swapped the phantom keys for the real `oracleLore` key so it's explicit +
  robust to a future edit.
- **Still open (content enrichment, not blockers):** the 20 diverse-expansion puzzles carry NULL thread
  tags (not clustered in the Recovery Archive) — pure content add. (The "seven keepers" doc conflation
  noted here previously was fixed in `125fe56`.)

**Playtest trace (manual, content-quality + retrace-fairness read, not a live human group):** Sampled the
cold-open hook, the full M4/Iss chain (`no-wall-catch` → `bound-word` → `m4-three-hands` → `threshold-
coordinate` → `true-walk-arrive`), the M5 finale composer + all fate/fork/seventh-choice prose, and the
companion reckoning lines. All hold up well: the cold-open lands the haunt correctly (an anomaly that
already knows something about you before you knew it was watching); the M4 chain properly teaches its own
technique via hint tier 3 before requiring it (no moon-logic) and varies mechanically (cipher → coop-
coordination → physical road-following → presence-gated read); the finale/reckoning prose is genuinely
strong and register-disciplined. No additional pacing/fairness bugs surfaced beyond the drip-gate fix
above (which the trace would have hit directly on the M4 chain).
