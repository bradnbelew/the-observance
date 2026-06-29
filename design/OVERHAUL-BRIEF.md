# THE OBSERVANCE — OVERHAUL BRIEF (cold-start handoff)

> For a fresh session with **no memory of the prior conversation.** Read this top-to-bottom,
> then `design/TEARDOWN.md` (the audit) and `design/research/DOSSIER.md` (the research), then
> open the actual code/seeds. Your auto-loaded memory file is `the-observance-overhaul-mandate`.
> **Trust the code and seeds over the design docs** — there are 28 design docs and they have
> drifted from what's actually built.

---

## 0. What this project is
A server-side Minecraft ARG (**Paper 1.21.x / Java 21** plugin + **Discord** bot + **Supabase** +
**Vercel** dashboard) built for a small veteran friend group. North star: **"From The Fog, but it
knows your name"** — autonomous, per-player, soft-pressure horror/mystery, **not scripted.** Path A:
friends install nothing but ONE auto-pushed resource pack. Stack already in play: FastAsyncWorldEdit,
Citizens2, ZNPCsPlus, PacketEvents, Multiverse-Core, a curated beat library, a between-session
showrunner. Repo root: `D:/the-observance` (`bradnbelew/the-observance`).

---

## 1. YOUR MANDATE — full latitude
You are doing **one full step-back overhaul of the whole ARG, day one → finale.** Full agency, no
permission-asking. Ethan (the owner) was emphatic and corrected this several times, so internalize it:

- **The toolbox is everything, not a direction.** add · edit · remove · refine · enhance · expand ·
  rework whole sections · de-linearize the story · cut what's too wide · add new features · invent new
  ways to integrate into Minecraft · **even change the premise entirely if the premise is the problem.**
  His words: *"the whole idea could be not ideal"*, *"you can do 100000+ different things i dont care"*,
  *"don't take these words hyper-literally."* Do NOT collapse this into a single rule like "make it lean"
  or "make it bigger." Decide with judgment.
- **Ethan's taste (a strong preference, NOT a cage):** a **rich, immersive, genuinely difficult,
  time-taking** experience with **deep lore and lots of sidequests.** He leans away from a stripped lean
  core. Weight this heavily — but you may still cut/rework where your judgment says it makes the whole
  thing better.
- **The only hard guardrails:**
  1. **Cohesive** — no mess, no orphaned or contradictory pieces. Rich *standalone* lore/backstory is
     welcome **as honest flavor**; the sin is **inert content that costumes itself as a puzzle/game-thread**,
     and a bad ratio of real-ARG to filler. (See the lore rule, §6.)
  2. **It actually runs** — the current build's core does not (see §2). An ARG that can't run is the one
     thing Ethan can't ship.
  3. **Genuinely great for the friend group** — immersive, difficult, *findable*.
- **Have a mind.** Do your own research, your own auditing, your own ideas — through the lens of someone
  making this incredible for their friends to thoroughly enjoy. **Work holistically: do not build in a
  straight line.** Step back, look at the whole web together, decide the shape, then execute.

---

## 2. THE BRUTAL CURRENT STATE (full detail in `design/TEARDOWN.md`)
- **Nothing has ever run on a real Minecraft server.** `npm run specscheck` is **RED** (11 unclassified
  seed rows). ~108 files are **uncommitted.**
- **The central nerve is a literal no-op** (verified in code): `getOpenPuzzles` (`discord/src/db/repo.ts:361`)
  never reads `requires_flags`, so gated puzzles are solvable from minute zero; migration `0006` doesn't
  exist, so the *other* gated rows are permanently dead; **no flag-producer listeners exist**
  (Ignition/CoopPlate/SeventhChoice/UnlitDeep/Refusal), so nothing ever sets a flag; `voice.recordFrameBreak()`
  doesn't exist, so the cursed-map "it says the 6 back at you" — the single best beat — currently produces
  **silence**; `composer.ts` (the M5 ending assembler) doesn't exist. Every guard fails silent, so "broken"
  looks like "fine."
- **It is over-woven** for a 4–8 person irregular voice group: 24 threads, ~21 facts, 10 invariants, two
  extra dimensions, a filing-system thesis, three different "sevens." The problem is **"too dense to find,"
  not too hard to feel.**
- **The genuinely great core (keep, lean on, grow):** the **six distinct broken keepers**
  (Vaun/Mara/Sella/Orin/Brann/Iss) whose fates mirror player behavior; the **Liar/Iss Vigenère catch**
  (the warm voice lied, and the land kept the proof); the **receive-don't-reward Accepting bow.** The
  `arc/corpus/journals-*.md` voices are the strongest asset in the project.
- **Specific defects to FIX (not necessarily cut):** answer collisions (`the one who turned away` is a
  primary answer on 3 active rows → wrong-puzzle resolution); the rune-literacy gate `rosetta-ring` teaches
  `WARD/COVERING` on the carved ring but accepts different words in the resolver; the Vigenère "duality"
  is faked (a hand-typed phrase, not a real decode); reveal-discipline collapses for a convened group; the
  Whisper hint rail has voice lines but **no mechanism**; the lure `6` carries two incompatible meanings;
  the stego is never actually applied; the coop gate uses a 20s real-time window with a mis-located AND-join.

> **Use the teardown as a DEFECT + COHERENCE list, not a shrink order.** Fix what's broken, make the
> dense web *discoverable* (Watcher points one live thread at a time; a working hint economy; signposted
> entry points), and keep/grow the rich lore. Density is not the enemy; *undiscoverable* density is.

---

## 3. THE RESEARCH THAT SHOULD RESHAPE YOUR APPROACH (`design/research/DOSSIER.md` + the 7 lane notes)
Highest-leverage findings:

- **Model the arc as a salience-based STORYLET engine, not a linear pointer.** Each beat = preconditions
  (over per-player qualities) + content + state-change; the showrunner fires the most *salient legal*
  storylet. `requires_flags` **is** the precondition system — lean into it. This gives order-independence,
  skip-tolerance, and gap-resilience for free, and is the cleanest way to make the story **non-linear yet
  coherent** (exactly what Ethan wants). (Refs: Emily Short / Failbetter storylets, Kreminski salience.)
- **Re-tier the in-game layer: world-build static + per-player PACKET illusion.** Stop relying on global
  "mutate only when unwitnessed" (`Reveal.isHidden` never returns true for a group in one base). Build the
  stones/shrines/graves statically ahead of time; do the "it knows ME" reactivity as **per-player illusion**
  that needs no unwitnessed instant: `player.sendBlockChange(...)`, `display.setVisibleByDefault(false)` +
  `target.showEntity(...)`, EntityLib `WrapperEntity.addViewer(target)`. (Fixes the single biggest
  interaction defect.) Pitfall: don't fake tile-entities (signs/heads) via `sendBlockChange` — use
  TextDisplay/ItemDisplay.
- **Atomic jsonb flag merge** to kill the silent flag-clobber: `update arc_state set flags = flags || :new`
  as ONE server-side statement (Supabase RPC), never SELECT-then-UPDATE. Keep the flags object flat.
- **Solder the nerve:** write `0006_requires_flags.sql`; read `requires_flags` in BOTH `getOpenPuzzles`
  and the Java `OracleResolver.firstMatch`; build the flag producers (start with Ignition + CoopPlate, or
  a temporary `/obs flag set <key>` admin command to prove gating before the listeners are perfect); make
  the Java `applyOutcome` actually apply `set_flags`.
- **Sealed-door + teleport room-swap** instead of unwitnessed live mutation; pin a Watcher line so a change
  reads as intentional the instant it's noticed.
- **Vanilla-first capability map** (no resource pack needed): Display entities (Text/Block/Item) for runes,
  holograms, apparitions; particles; custom fog via **datapack `dimension_type`**; per-player packets. The
  pack (rune font, sounds, models) is an enhancement with a graceful-degrade fallback (`ResourcePackPusher`
  + the Discord `#the-record` mirror), not a hard dependency.
- Plus ARG-craft principles from the case studies (Cicada 3301 / I Love Bees / Petscop): disclose the
  rescue/hint rail once up front; keep one obvious trailhead; never let a true solve dead-end with no
  acknowledgment; TINAG only works if every surface stays in-fiction. And 8 non-obvious ideas in §D of the
  dossier.

---

## 4. THE ARTIFACT MAP (where everything is)
- **Read first:** `design/TEARDOWN.md` (audit) · `design/research/DOSSIER.md` + `design/research/*.md` (research).
- **The current web/story:** `design/WEB-MASTER.md` (spine + §9 plant→payoff ledger) · `design/story-map.json`
  + `design/STORY-WEB.html` (readable lore web) · `design/PLAYTHROUGH-SCRIPT.md` (+ `.html`) — the literal
  day-one→finale script with `[GAP]` markers.
- **Supporting (treat as drifted — verify against code):** `design/INTEGRATION-V2.md`, `BUILD-MANIFEST.md`,
  `COHERENCE-AUDIT-V2.md`, `MINECRAFT-INGEST-PREP.md`, `MASTER-PLAN.md`, `FINAL-REPORT.md`, the 28
  `design/ideas/*.md`. **Consider consolidating this sprawl as part of the overhaul** — there is no single
  source of truth right now and several docs each claim to be one.
- **Lore:** `arc/lore/canon-spine.md`, `arc/lore/LORE-BIBLE.md`, `arc/lore/documents/*`, and the gold:
  `arc/corpus/journals-*.md`.
- **The Java engine:** `plugin/src/main/java/com/observance/watcher/` — `beats/lib/*`, `signal/listener/*`,
  `oracle/*`, `util/Reveal.java`, `util/PerPlayer.java`, `data/SupabaseClient.java`,
  `resources/{plugin.yml,config.yml,sites.yml}`.
- **Discord/showrunner:** `discord/src/forge/*` (ciphers/runes/clue-specs), `oracle/*` (resolve/normalize),
  `showrunner/*`, `voice.ts` + `voice.archive.ts`, `db/repo.ts`, `supabase/migrations/*` + `seeds/*`.
- **Dashboard + website:** `dashboard/src/app/*` (the director console + the `/record` site).

---

## 5. A SUGGESTED STARTING SEQUENCE (advice, not a constraint — you have full latitude)
0. **Commit the baseline** (~108 uncommitted files) so your overhaul reads as a clean diff and is recoverable.
1. **Step back and write what this should BE at its best** — the one central experience — then judge every
   existing part against that, not against the old docs.
2. **Decide the shape:** the storylet reframe; how rich/long/difficult; what story changes, what to
   keep/cut/rework/add; how to make the dense web discoverable.
3. **Solder the nerve so it RUNS** (0006 + read `requires_flags` on both surfaces + flag producers +
   ignition + the atomic merge + Java `set_flags`). Green `specscheck`.
4. **Fix the specific broken puzzles/coherence** (answer collisions, the rune-ring mismatch, the faked
   duality, the hint rail, the `6`, the stego, the coop window).
5. **Re-tier the in-game layer** (static builds + per-player packet illusions; sealed-door room swaps).
6. **Then expand/rework with full latitude** toward the rich, difficult, immersive experience — story,
   sidequests, lore, new Minecraft integration — kept cohesive and *findable*.
7. **Prove the smallest real loop on a server** before scaling everything (nothing has run yet — one proven
   beat is worth more than another design pass).

Throughout: when a change is a **mechanic**, move story + clue + lore + interaction together; when it's
**standalone lore/atmosphere**, that's fine — just make it read honestly as flavor, not as a fake puzzle.
Do a real **copywriting pass** on player-facing wording. Don't bury the real ARG under pretend-ARG.
