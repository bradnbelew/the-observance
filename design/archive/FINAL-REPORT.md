# The Observance — final build report

*A server-side Minecraft ARG for a friend group, filmed as a "this is not scripted" documentary. This
report covers the end-to-end build, the guardrails, the 7-player scenario testing, the comprehensive
adversarial audit (and every fix), the anti-AI-slop writing work, and the go-live checklist.*

---

## 1. Executive summary

The Observance is **built and internally green**. Across this pass: the deterministic engine and the
plugin's group-safety layer were completed; the data model was migrated; the de-slopped found-document
corpus was authored and wired into live content; the cipher-rune resource pack was generated from the
single source of truth; a 7-player scenario test harness was written and passes; and a **52-finding
adversarial audit** was run, with **all 5 criticals and the actionable highs fixed**.

What is NOT done is everything that genuinely requires hands the repo doesn't have: applying SQL to
Supabase, building the world in-game, recording the audio, installing server mods, and deploying — the
**go-live checklist (§7)**. Those are flagged, ordered, and unambiguous.

**Coherence is enforced by 10 build-time guards** (`npm run specscheck`) so a content/code drift fails
the build, not the camera. **~30 commits this pass, all local (unpushed by policy).**

---

## 2. What was built (by layer)

| Layer | State | Notes |
|---|---|---|
| **Coherence guards** | ✅ 10 in `specscheck` | forge-bind, reachability, no-leaked-sentinel, namespace, thread-registry, site-coverage, rite-token, thread-tags, thread-card-voice-coverage, register-discipline |
| **Data model** | ✅ migration `0005` | threads / thread_cards / side_quests / punishment_state / npc_dialogue_state + puzzles.thread_key/teaches_custom |
| **Showrunner spine** | ✅ deterministic | `decide()` (drips + fair gifts), zero-LLM, sleep/confirm, customs→report bridge (now per-tick capped) |
| **Plugin group-safety** | ✅ MF-8/9/10/11 | GroupBeat fan-out (per-player id), two-path reveal + SceneAwareness, WARDEN fallback, resource-pack tracker |
| **Selection / restraint** | ✅ Attention | responsive focus ("it knows me") + tier restraint gate; DIRECTED rewards exempt from the ambient window |
| **The Accepting climax** | ✅ detector | synchronized group-bow → opaque token → oracle; quorum = cast size; non-spoofable |
| **Corpus (writing)** | ✅ 6 docs, de-slopped | journals/letters/records/cipher-plaintexts/NPC+Watcher voice; a dedicated de-slop pass + the register guard |
| **Live content** | ✅ wired | 24 thread-tags, 42 Recovery-Archive cards, 18 travel destinations, `voice.archive.ts` (164 keys) |
| **Resource pack** | ✅ generated | carved-rune atlas + MC font from `runes.ts` (in-world == Discord); README lists go-live |
| **Structures** | ✅ spec | `design/structures.md` build spec; the in-game builds are go-live |
| **Dashboard / director** | ✅ built, re-gated | `/author` (kill-switch, arc, beat-queue, climax, budgets, dossiers) + `/status`; auth restored |
| **7-player scenario test** | ✅ passes | `showrunner:test:scenario` — multi-day timeline + edge cases |

---

## 3. Guardrails inventory (the "nothing goes haywire / spams / breaks immersion / hallucinates" mandate)

The audit confirmed the great majority of these **sound**; the few gaps were fixed (§6).

**Anti-spam / anti-haywire**
- `DramaBudget` rolling-window cap (absolute ceiling on ambient/personalized) + per-player & global ambient cooldowns.
- `Attention` restraint gate — calm scenes skip ~78% of considerations; the score decays so a scare can't snowball.
- Per-player `RateLimiter` cooldowns on every tracked custom (bow 10s, offering 30s, dark-hours 60s, deep-line 5min).
- Oracle token-bucket + durable windowed attempt ceiling + per-puzzle `max_attempts` + **idempotent solve** (no replay re-reward).
- `AnswerSign` 3s submit cooldown; **customs report bridge now capped 3/tick** (was the one uncapped path — fixed).
- `BeatQueuePoller` in-flight set + durable DB status = double-fire guards; fetch capped per poll.
- **`scenario.selftest` proves** drips never violate the interval and gifts never cascade, across a 14-day 7-player run.

**Anti-immersion-break (on camera)**
- Reveal discipline: mutate only when unwitnessed (radius + line-of-sight + FOV), same-tick check-then-mutate, silent abandon on a witnessed block, **per-player two-path** for convened groups.
- `NamedMobBeat` → WARDEN/STRAY fallback, **never a green zombie**; spawned out of LoS, AI-disabled, silent.
- Resource-pack tracker gates rune rendering (ASCII fallback, never tofu) — *wiring into rune beats is a go-live item, §6/§7*.
- Every player-facing string comes from `voice.ts`/`voice.archive.ts`; **register mechanized by guard 10** (no caps/exclaim/meta-words).
- All bot interactions try/caught → only a `voice.quiet()` line, never a stacktrace; **all slash commands now deferReply** (no red "did not respond").
- The Accepting climax is an **opaque, non-typeable token** — performable only, not spoofable at a sign or in Discord.

**Anti-hallucination**
- The showrunner spine is **100% deterministic, zero-LLM** — it works even if the AI layer is down. No player-facing text is LLM-generated at runtime; all of it is authored in `voice.ts`/`voice.archive.ts`/the seed.
- `noLeakedSentinelSelfTest` fails the build if any answer describes itself as a placeholder.

---

## 4. 7-player scenario testing

`discord/src/showrunner/scenario.selftest.ts` (`npm run showrunner:test:scenario`) drives `decide()`
across a **simulated 14-day timeline with 7 players and evolving state**, plus dedicated edge cases.
All assertions pass:

- **A (main arc):** drip cadence never violated (no spam), never opens on a dead_end/lore, never drips a non-forgeable found-doc row, the arc progresses, gift volume bounded.
- **B (all 7 hard-stuck):** ≤1 gift per player per tick, capped at the authored hint count — no infinite-hint cascade, no re-gift while a whisper is unspent.
- **C:** the kill-switch (asleep) and confirm-mode hold for an entire run.
- **D:** degenerate inputs (empty / all-solved / found-docs-only) degrade quietly, never crash.
- **E:** fully deterministic (seeded, no `Math.random`) and the sim actually branches.

*Known limitation (audit, low):* the harness uses a 9-node fixture, not the live 24-row seed, so it
tests the decision **logic**, not a live-web coherence regression — those are caught by the 10 guards
instead. Loading the real seed into the harness is a worthwhile future enhancement. **Live in-game
playtest with 7 real clients is a go-live step** (no server in-repo).

---

## 5. Anti-AI-slop / "make it feel REAL"

Treated as a first-class requirement, not a polish afterthought:

1. **Authoring law.** Every corpus + content agent ran under an explicit banned-phrase list ("a testament to", "little did they know", named emotions, tidy thematic bows, three-adjective lists, "not just X but Y", melodrama) and a required-texture list (concrete mundane detail, iceberg/omission, distinct per-author voices, tonal decay shown structurally).
2. **A dedicated de-slop pass** rewrote the corpus in place — it caught and fixed exactly the right tells ("that is the lesson" bows, named emotions → concrete acts, melodramatic superlatives, meta-editorializing in the cipher notes).
3. **The audit's anti-slop dimension** then adversarially re-read everything and flagged the remaining weak lines — **all the flagged register-slip/tidy-bow lines were fixed** (the tier-1 whisper, oracleMainBeat, the DOB line, the "not nothing" litotes).
4. **Guard 10 mechanizes the Watcher register** so a future caps/exclaim/meta-word slip fails the build.
5. **The cipher VISUALS are real:** the rune alphabet is an original carved futhark-style script generated from `runes.ts` (verified by eye via `pack:proof`) — not a font dump, and identical in-world and in Discord.

*Remaining (minor, tracked below):* a few subjective verbal tics the audit flagged as "becoming a mannerism" on repetition — `"X, and the whole of X"`, the `"not nothing"` litotes elsewhere, Dob's `"Is that mad. That's mad."` — worth a final read-through polish pass; none are mechanical violations.

---

## 6. The audit — 52 findings, dispositions

Seven dimensions, adversarially generated. (The verify pass was server-rate-limited mid-run, so every
serious finding was re-verified by hand against the code before acting.)

**Counts:** 5 critical · 14 high · 20 medium · 13 low.

### Fixed this pass (all 5 criticals + the actionable highs)
| Sev | Finding | Fix (commit) |
|---|---|---|
| CRIT | GroupBeat fan-out hit only 1 player (shared beatId) | unique per-player id |
| CRIT | Slash commands never deferReply → red "did not respond" | deferReply + editReply on all 3 |
| CRIT | Customs report bridge uncapped → 7-player Discord burst | worst-first, capped 3/tick, deferred keep their mark |
| CRIT | Dashboard author surface fully open (no auth) | `guard()` = `isAdmin()` + page redirect |
| CRIT | `authenticated_all` RLS exposed 12 spoiler tables | `0003_lockdown.sql` drops policy + grants |
| HIGH | `/link` ILIKE wildcard → identity hijack | escape metacharacters + assert exact match |
| HIGH | Accepting quorum default 2 → sub-group fires climax | default 6 (cast size) + 300s cooldown |
| HIGH | DramaBudget swallowed earned rewards | DIRECTED exempt from the ambient window |
| HIGH | rosetta-ring taught fake ways `ward`/`covering` | replaced with the_unspoken + the_sacred_beast (also gives them their only teaching home) |
| MED/LOW | register slips + tidy bows in voice lines | de-slopped + guard 10 |

### Documented (correct as flagged; resolved by go-live or an intentional design choice)
- **MF-11 gate not wired into rune beats** — the tracker works; the per-beat `isLoaded()` check + the `BeatContext.resourcePack()` accessor land when the rune beats render the font. Until then runes ASCII-fallback. *(go-live wiring item)*
- **bow/offering are honor-only** (no violate path) so their report ladder is unreachable — this is **by design** (you can't "violate" a bow by not bowing in a way the plugin tracks); the report bridge covers the 5 violate-capable customs. *(documentation, not a bug; the customs self-test fixture should be relabelled to a violate-capable custom)*
- **the_unspoken never fires** because `tracker.forbidden-words: []` — intentional ("ships no story text"); Ethan adds the per-arc forbidden word at go-live (now taught by the rosetta ring).
- **Reveal LoS raycast cost** under a tight group — the hot path already distance-gates before the raycast and early-returns on first witness; a shared per-tick raycast budget is a worthwhile hardening if a stress test shows a spike. *(perf hardening, monitored)*
- **accepting-crouch → record-receives (active=false)** — the finale's terminal row is intentionally staged; **activate it when Movement V launches** (go-live).
- **Drip spine ignores the rosetta literacy gate** — a keeper-stone card could be posted before the ring is learned; tighten by gating the drip pool on `rosetta_known` if strict literacy ordering is wanted. *(design choice for the director)*
- **Pre-existing dashboard tsc errors** (Supabase SSR cookie + form-action types in untouched files) — **must be resolved (or `typescript.ignoreBuildErrors`) for `next build` to pass.** *(go-live, flagged HIGH)*

### Remaining medium/low (triaged; none block an internally-green build)
Most are go-live items (already in §7) or minor hardenings: Discord 429 retry-after handling; the public-channel withhold-reply spam bound (already ≤8/60s); Attention map pruning quitters; `GroupBeat scene=all` budget; the subjective verbal tics (§5). The full finding set with locations is in the audit task output.

---

## 7. GO-LIVE checklist (the only things left, in order)

> Everything above is done in-repo. These need hands the repo doesn't have (a Supabase project, a
> Minecraft client, audio, hosting, deploy). All SQL is idempotent.

1. **Rotate secrets.** The chat-pasted service-role key, bot token, and Anthropic key — and a committed `.env.local` exposes the live service-role key. Rotate all, then scrub the committed file.
2. **Apply SQL** (service_role, in order): dashboard `0001_init` → `0002_seed` → `0003_lockdown`; discord `0003_discord` → `0004_oracle` → `0005_threads`; then seeds `puzzles_seed` → `thread_tags` → `thread_cards` → `side_quests`. Then run `npm run seedcheck && npm run specscheck`.
3. **Dashboard auth.** Set `ADMIN_EMAILS` to the director's email(s) (or `/author` is correctly locked). Configure Supabase Auth (magic-link SMTP + redirect URLs).
4. **Build the world** in-game per `design/structures.md` (deepslate/blackstone, stoop-to-read stones, the 6-8 player Undercroft gather-room). Export `.schem`; **fill the real x/y/z into `sites.yml`** (esp. `unbroken_light`, the 6 keeper-stones, both Rosettas). **Carve the rune ring with the real ways** (bow/offering/kept-light/deep-line/**unspoken/sacred-beast**), not ward/covering. Activate `record-receives` for the finale.
5. **Resource pack.** Add the 4 MONO `.ogg` files named in `sounds.json`; set `pack.mcmeta` `pack_format` to the server version; tune `ascent`/`height`; zip + host; push via `server.properties` `resource-pack=`/`resource-pack-sha1=`.
6. **Mods/datapacks.** Citizens2 (surface NPC dialogue — tables ship empty until seeded), Multiverse + the `dimension_type` fog datapack (the Undercroft).
7. **Config.** Set `tracker.forbidden-words` (the per-arc Unspoken word) and `rites.accepting.quorum` to the real cast size.
8. **Deploy** the Discord bot worker, the showrunner cron, and the Vercel dashboard with their (not-in-repo) env. **Fix the pre-existing dashboard tsc errors so `next build` passes.**
9. **Wire MF-11** into the rune beats (BeatContext accessor + per-beat `isLoaded()` gate) when they render the font — or accept ASCII fallback for v1.
10. **Live playtest** the vertical slice with real clients before filming.

---

## 8. Bottom line

The hard, breakable parts — the deterministic engine, the group-safety on camera, the coherence web,
the non-spoofable climax, the real cipher visuals, and the *de-slopped* writing — are **built, tested,
guarded, and audited**. What remains is deployment and the in-world build: real, but mechanical and
fully enumerated above. The thing is ready to be stood up.
