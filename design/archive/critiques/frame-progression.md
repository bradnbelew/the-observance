# CRITIQUE — the two new ideas: `cursed-map-frame` + `minecraft-progression`

> Ruthless coherence + ARG-craft pass. Grounded against the canonical web (`WEB-MASTER.md`,
> `INTEGRATION-V2.md`, `COHERENCE-AUDIT-V2.md`), the canon spine (`arc/lore/canon-spine.md`), and the
> as-built tree (`dashboard/.../record/[slug]/page.tsx`, `discord/src/voice.ts`,
> `discord/src/showrunner/prologue.ts`, `HeatmapAccumulator.java`, `sites.yml`, `fate.ts`,
> `arc/lore/documents/*`). Every "what is there" claim is quoted from a real artifact; every absence is
> verified by grep, not assumed. Severity-ranked, one concrete resolution each. Must-resolve set at the
> bottom.
>
> **SUPERSEDES the prior draft of this file.** The previous `frame-progression.md` opened with S0
> (BLOCKER): *"`cursed-map-frame.md` does not exist."* That is now FALSE — `design/ideas/cursed-map-frame.md`
> is on disk (a full 6-section treatment). The prior critique therefore never actually critiqued the
> cursed-map idea and its headline finding is stale. This pass covers **both** real artifacts.

---

## 0. GROUND-TRUTH VERIFICATION (what is real, quoted)

Both idea files are unusually disciplined about self-grounding. Verified TRUE against the tree:

- ✅ **The Nether/End anchors are RATIFIED, not self-minted.** `WEB-MASTER §0.4` lists `nether_forge`
  (`observance_nether`), `end_seventh_shrine`/`end_exile_hold` (`observance_end`) with one owner each
  (`minecraft-progression`). So the progression file's header claim ("ratified canon, not self-minted") is
  honest. `canon-spine §3b` does NOT mint a new FACT for either lane — correct (both are delivery bodies).
- ✅ `HeatmapAccumulator.CellKey` is world-keyed; `sites.yml` carries per-site `world:` + `default-world`.
  Dimension-aware tracking with zero new infra is TRUE.
- ✅ `decideFate(inp: FateInput)` is pure/active-only (INV-11). `nether_forge`/`end_*` sites + seed rows do
  NOT yet exist (grep `nether|observance_end|exile_hold` in `sites.yml`/`puzzles_seed.sql` → no match) — the
  doc correctly marks them GO-LIVE/TODO. No hallucination.
- ✅ The cursed-map's cited `[BUILT]` corpus all exists: `the-record-opens.md` (and it really ends *"six are
  named in full, and there is a seventh mark the record will not [...]"* — the `6` plant is REAL canon, line
  74), `the-seventh-not-kept.md` (the *"to be kept and to be cast out are one door"* line is real, lines
  65-67), `kept-in-more-than-one-place.md`, `journals-vaun-mara-sella.md`.
- ✅ The prologue ignition spine is exactly as cursed-map describes: `prologue.ts` flips `prologue_ignited`
  on a DETECTED signal (*"a human read the lectern OR posted in #the-record"*), gates a ONE-SHOT
  `recordOpened` ack with an `acked` guard, and `decide.ts` suppresses the curatorial drip until ignition.
- ✅ `voice.ts` has `recordOpened` (82), `recordElsewhere` (311), `recordReceives` (544),
  `fateKept/CastOut/Refusers/InheritorsCodicil` (455-471). `recordFrameBreak` does **NOT** exist — cursed-map
  correctly marks it `[GAP — one voice key]`.

So both files are sound, build-aware treatments. The defects below are where they nonetheless **collide with
each other**, contradict the frozen web, orphan, double-book, mis-gate, or read too easy.

---

## 1. THE CROSS-IDEA COLLISIONS (the two files vs each other — the audit's whole reason to exist)

### S1 (P0 — HARD COLLISION) — both ideas claim master-ledger rows **24-27** with DIFFERENT plants

This is the exact namespace sin `COHERENCE-AUDIT-V2 ROOT-A` was minted to stop, recurring on the **plant
ledger** (`WEB-MASTER §9`, which today ends at **row 23**). Both files independently append "next free row #24+":

- `cursed-map-frame §5`: row **24** = the `kept: 6` counter; **25** = the README "it does not connect to
  anything"; **26** = the dead uploader `m.kept`; **27** = the vignette's doused hearth + scraped wall.
- `minecraft-progression §5`: row **24** = Brann's "the fire is lent… below the below"; **25** = D11 "to be
  kept and to be cast out are one door"; **26** = D-NETHER-1 "someone who keeps the light better… should
  carry it down"; **27** = the unwriting wall's extra effaced "out past the door" line.

**Two files, eight plants, four integers.** If both ship as written the §9 ledger has duplicate row IDs —
the go-live gate (*"no plant ships without a seeded payoff"*) reads an ambiguous table, and any later
"see ledger #24" reference is undefined.

**Resolution:** synthesis owns `WEB-MASTER §9` numbering exactly as it owns FACT/INV integers. Assign one
contiguous block — e.g. cursed-map = **24-27**, progression = **28-31** (or interleave by movement). Both
idea files must change "next free row #24+" to the **synthesis-assigned** rows and stop self-numbering. Until
then neither ledger block is canon. (Note: progression §5 also says *"status DESIGNED→seed"* while cursed-map
says *"NEW"* — normalize the status column too.)

### S2 (P1) — both lanes pour content into Movement IV, the already-densest stretch, with no shared budget

`WEB-MASTER §1.M4` is explicitly *"the universal hinge — ~six threads pay off here."* The arg-craft critique
(F3) already had to **stagger** payoffs *away* from M4. Now:

- cursed-map's frame-break + the `6` hard-payoff both land at/around M4 (§1.5 "M4 (hard)", §1.7 "M4 The
  Catch": doused hearth + scraped wall + `6` + Mara's hand all re-read cold).
- progression's End way-out, the Seventh shrine, the unwriting-wall extra line, AND the `cast_out`/`refusers`
  re-read all key on `seventh_named`/M4→V.

Each file is internally disciplined about *its own* M4 load, but neither accounts for the **other's** M4 load
landing on the same catch. The M4 cold-re-read is the single most important "oh" in the arc; piling two more
multi-beat cascades onto it risks the same exposition-blur the FACT-15 discipline forbids.

**Resolution:** synthesis must budget the M4 catch as a shared resource. cursed-map's `6`-hard-payoff is a
*single re-read of a number already seen* (cheap, keep at M4). progression's End lane is post-`seventh_named`
and can sit in the M4→V tail (it already does). Confirm in both files that the catch beat itself adds **at
most one** new cold-re-read per file, and the rest trail into V.

### S3 (P2) — both lanes touch the SAME Mara voice/hand; verify one fingerprint, no contradiction

cursed-map §1.4 makes the dead uploader **Mara** ("the Reader," `m.kept`, deferred/citeable hand).
progression §3(a) makes **D-NETHER-1** Mara's hand too ("*i have read it forty times. i have not walked it*").
Both correctly invoke her `WEB-MASTER §6` fingerprint (referential, deferred, cites-not-acts). Good — but they
are now **two new Mara surfaces authored in different files**, and `canon-spine §6.8` ("keeper voices are
inviolable") + `journals-vaun-mara-sella.md` are the single source. A drift between "i copied it as it was
given, page for page" (cursed-map) and "page eleven, third line… i have read it forty times" (progression) is
easy.

**Resolution:** LORE authors **all** new Mara surfaces (the lure-page provenance line AND D-NETHER-1) in one
pass against `journals-vaun-mara-sella.md`, confirming both cite/defer and neither makes her *act*. Note the
thematic rhyme is actually an asset: Mara-who-copies-and-keeps (cursed-map) and Mara-who-reads-but-won't-walk
(progression) are the same character — make it deliberate, not accidental.

---

## 2. `cursed-map-frame` — SEVERITY-RANKED DEFECTS

### S4 (P1) — the route gap is DEEPER than the file states: `record/[slug]/page.tsx` has no `params` at all

cursed-map §1.3/§4 says the route *"already exists as `record/[slug]/page.tsx`"* and *"today the route reads
`v_record` without validating the slug — that is the gap,"* proposing a one-line `slug === 'the-record-keeps'`
validation. **The real gap is bigger.** The as-built file (quoted) is:

```
export default async function RecordPage() {
  const signal = await readSignal();
  const rec = project(signal);
```

`grep -ci 'params|slug'` → **0**. The component takes **no `params` argument** and contains **no
slug-dependent branch whatsoever** — it is a fixed page that happens to live under a `[slug]` segment. So
*every* slug (`/record/anything`) renders the identical base archive. cursed-map's "add slug validation +
a downloads block when slug === the lure slug" is therefore not a one-line addition to an existing read — it
requires (a) adding `params` to the signature, (b) branching the render, (c) a 404/in-voice path for non-lure
slugs (which today simply don't exist as a concept). This is a real, correctly-directed gap, but the file
**under-scopes it** and mis-describes the current behavior ("reads v_record without validating" implies a
validation point exists to fix; there is none).

**Resolution:** restate the §4 task as: *"`record/[slug]/page.tsx` is currently slug-agnostic (no `params`);
add `{ params }`, render the BASE archive for the bare `/record` and the existing slug, the downloads block
ONLY for `the-record-keeps`, and an in-voice 404 for any other slug — preserving `noindex`, no client JS, and
the `v_record`-only read."* Keep it a server component; do not let the downloads block introduce a live
counter (§2c holds — the `6` is static).

### S5 (P1) — PRECISION: the frame-break's conduct-callback FB-2(i) can fire a *false* "you did X in the map"

cursed-map §1.6 FB-2(i): *"if a player repeats in the server the conduct the vignette flatly noted… the line
states the repeat: 'you passed the seventh in the hold too. it was noted there. it is noted here.'"* The file
argues this is *"true by construction (the vignette logged the conduct locally; the server re-observes it)."*
But the vignette is a **downloaded, offline, single-player datapack** (§2f) — its local "log" is **not
transmitted to the server**. The server has NO measured signal that *this* player passed the seventh *in the
map*; it can only observe what they do *on the server*. So "you passed the seventh in the hold too" asserts a
map-side fact the server **cannot have measured** — a `§6.4`/INV-16 precision lie of exactly the kind
*"a wrong 'it knows you' is worse than none"* forbids. The file's own §2d says the named-personal path is
"absent here entirely," yet FB-2(i) smuggles a map-side personal claim back in.

**Resolution:** **cut FB-2(i) as written**, or rebuild it as a *server-only* observation: the line may state
a conduct the server itself re-measured ("you passed the seventh") WITHOUT asserting the map ("…too / it was
noted there"). The count-callback FB-2(ii) — *"six were kept before you"* — is group-facing, true for
everyone, names no one, and the map genuinely could not say it (it was offline/static). Make FB-2(ii) the
**only** frame-break body; the default-safe degrade (fire only `recordOpened`) already covers the rest. This
keeps the category-violation scare (the server stated the `6` the dead page showed) without a fabricated
map-memory.

### S6 (P2) — the "6 downloads" payoff partly DOUBLE-BOOKS the existing `the-record-opens` "six named" plant

The `6` is load-bearing, and its canon source is real (`the-record-opens.md` line 74, the "six are named in
full" line). But that line **already has a seeded payoff**: master-ledger row **2** (*"seventh mark the record
will not keep" → M3 → the Seventh, cast out*). cursed-map's `6` re-reads the SAME canon clause to a DIFFERENT
payoff ("six prior *download* groups"). Two payoffs off one plant is allowed (the web likes ≥2 doors) — but
only if they don't contradict. "Six named keepers" (canon) vs "six prior groups who downloaded the map"
(cursed-map §1.5) are reconciled by the file as *the same six* ("they are the six dead keepers"). That is
elegant, but it means the cursed-map is asserting **the six keepers each = a prior download group** — a
non-trivial cosmological claim (the keepers were *players who got the map*) that leans toward the sealed
keepers-as-you thesis (FACT 14/15) earlier and harder than canon does.

**Resolution:** confirm with LORE that "the six downloads ARE the six keepers" does not over-blurt FACT 15 on
day zero. Safer framing: the `6` is inert/ambiguous on the page (a dead file's tally), and the keeper
identification is only *available* on the M4 cold re-read (the file mostly does this — tighten §1.5 so the M2
"soft" read is "six downloads, six keepers" as pattern-match, NOT "the keepers were download groups like us,"
which is the M4 felt landing). Keep the heavy equivalence sealed.

### S7 (P2) — PATH A: the datapack vignette is sound, but the "say *kept* in #the-record on arrival" ignition double-books the prologue detector cleanly — verify the word

cursed-map §4 routes the frame-break ignition through the BUILT `messageCreate` detector by having the
vignette's closing page tell the group to *"say kept when you are all in."* This is genuinely clever (no new
detector code — Path A and anti-jank both hold; the vignette is an optional client prop, the engine stays
server-side). **But** the prologue detector fires on *any* human post in `#the-record` (`prologue.ts`: *"a
human posted in #the-record"*), not on the specific word "kept." So the "say *kept*" instruction is flavor,
not a gate — which is fine, EXCEPT the file elsewhere (§1.6 FB-2) wants to distinguish "this group came from
the map" (to fire `recordFrameBreak`) from "this group joined cold" (fire only `recordOpened`). The bare
`messageCreate` detector **cannot tell them apart** — both just post in `#the-record`.

**Resolution:** if the frame-break must only fire for map-arrivals, it needs the optional `from_map` flag the
file already files as P2 (§4 "a keener form: a dedicated `from_map` flag set by an arrival beat"). State
plainly: **without `from_map`, the frame-break degrades to the count-callback `recordFrameBreak()` fired for
everyone** (which is safe — the `6` is true regardless of whether they played the map), OR build the P2
`from_map` flag. Do not let the design imply the bare detector distinguishes map-arrivals; it does not.

### S8 (verified AIRTIGHT) — the frame-break category-violation and the Mara/`6` canon ARE sound

Per the task's explicit ask: **verified.**
- The **frame-break** (map behaves like a map → server knows the `6` / re-observes conduct) is a real
  category violation and the file's reveal discipline holds (nothing witnessed mutating; the map is static;
  the server only *says* text in register). **Airtight given S5's FB-2(i) cut.**
- The **dead-uploader = Mara** reconciliation is canon-correct: the Seventh *cannot* be the named uploader
  (the record holds no name for them — `the-seventh-not-kept.md` "no name said back"), so a *named* hand must
  be a kept keeper, and Mara-the-archivist-who-copies is the right one. **Airtight.**
- The **`6` → six keepers** plant is real canon (`the-record-opens.md`). **Airtight given S6's sealing.**

---

## 3. `minecraft-progression` — SEVERITY-RANKED DEFECTS

> The file's header claims a *prior* `frame-progression.md` raised S1-S10 and that it folded each in as
> `[S#-RESOLVED]`. **That prior critique did NOT cover the cursed-map idea at all** (its S0 was "cursed-map
> doesn't exist") and its S1-S10 were all progression-only. The `[S#-RESOLVED]` markers are therefore
> self-certified against a critique whose numbering this file re-used. I re-verify the substantive ones below
> rather than trust the marker; most hold, two need a second look.

### S9 (P1) — `decideFate` input change is a frozen-interface edit the file still half-asserts

progression §1.4 proposes `FateInput.netherForgeFound` and is admirably explicit that it is *"PROPOSED… for
synthesis to ratify — not asserted as wired."* This is the right posture. **But** `WEB-MASTER §5` pins the
selector inputs to a closed list (*"`custom_compliance`… `seventh_found`, `iss_caught`, quorum/refusal. Bond/
Whisper tally is excluded"*) and §8 echoes it. Adding a third positive in-road to `kept` is a canon-surface
edit to `WEB-MASTER §5/§8` + the `FateInput` struct — and the file's §6 dependency table still lists it as a
**P1** build item, which reads as "scheduled," not "pending ratification."

**Resolution (confirm the existing `[S1-RESOLVED]` is honest):** it mostly is — keep the "degrades to a
tinted M5 clause with no selector dependency until synthesis ratifies" fallback (§1.4). Tighten §6 so the row
reads *"BLOCKED on synthesis ratifying the `WEB-MASTER §5/§8` `FateInput` edit; self-test: `kept` fires fully
without it (INV-12)."* Do not let any build step wire `netherForgeFound` into `decideFate` before §5 is
amended.

### S10 (P1) — the `seventh_seen_out` → `cast_out`-legible end-city is the INV-16 reconstruction risk (P0-4) re-surfacing

progression §1.5 makes the re-dressed end-city *"legible as the group's own exile-hold… only if
`seventh_seen_out`."* `cast_out` is keyed (`WEB-MASTER §8`) on *"violation dominates + ≥2 `LEFT_AT`"* — a real
honored/violated spread. INV-16 forbids any surface letting the group *"derive WHICH active player is on the
honored vs violated side."* A vast, walkable, persistent exile-hold labelled "the cast-out went here," shown
to a group whose fate was *measured* `cast_out`, is one inference-step from "we are cast out, and the ≥2 who
left are why." This is precisely the P0-4 risk INV-16 exists to kill. The file's `[S2-RESOLVED]` adds the
right teeth (chorus-only dressing, names no player, P2/cuttable) — **verify they actually bind**: the open End
has long sightlines and a void floor, and a "markers all facing away" vista is hard to keep from
spatially-rhyming the `LEFT_AT` set if any per-player carve exists out there.

**Resolution (the `[S2-RESOLVED]` marker is conditionally honest):** keep it **P2/cuttable** AND make the cut
condition concrete: *no per-player carve, name, or `LEFT_AT`-correlated dressing may exist anywhere in
`observance_end`; the exile-hold dressing rhymes ONLY on the chorus "you only came to look."* If the open End
can't guarantee that, ship the **Seventh shrine alone** (the `[S2]` already says this — make it the default,
not the fallback).

### S11 (P1) — second-bottom cosmology: the FACT-11 "below the below" seal is still UNSEALED in canon

progression §1.1 / `[S3-RESOLVED]` makes the Nether *"the source the Undercroft's one fire was carried up
from… below the below,"* and gates the build on LORE sealing one sentence to FACT 11. **That seal is not yet
in `canon-spine.md`.** FACT 11 (canon-spine line 188) reads only *"One fire never went out through the
abandoned years."* — no source, no "below the below," no furnace. `WEB-MASTER §0.4` lists the `nether_forge`
anchor as *"the deep fire-source (FACT 11 deepened, 'below the below'); a delivery body, not a new bottom (S3
seal)"* — so synthesis has **named** the seal but LORE has not **written** it into the spine. Until it is,
"the Nether IS the source of the kept fire" is a cosmological claim living only in the idea file, and a later
author reading canon-spine alone will not know the Undercroft-vs-Nether "which is the deep" answer.

**Resolution:** the `[S3-RESOLVED]` marker is **premature** — it is RESOLVED-IN-DESIGN, not RESOLVED-IN-CANON.
Before any Nether build, LORE adds the one sentence to `canon-spine` FACT 11 (and `structures.md`): *"the kept
fire was carried up from below the bottom; the Undercroft is the bottom of the Hold, the deep-fire its source
— one direction, not two."* This is a P1 BLOCKER (the file's §6 already lists it as such — good; just don't
mark §1.1 "RESOLVED" until the sentence is in the spine).

### S12 (P2) — walk-budget: the Nether near-pocket is the right P1, but confirm it is not gated behind the Undercroft in a way that hard-gates

progression `[S4-RESOLVED]` correctly scales the Nether P1 to a near-pocket (not a third decoded
coordinate-walk — honoring `coords-to-real-place §2.G` "do not multiply walks"). **Verify the gate
direction:** §1.1.2 finds the bearing-page at the Undercroft *"`requires_flags: [undercroft_open]`."* That is
fine (the Nether DEEPENS the Undercroft, so depending on it is on-theme) AS LONG AS the Nether never gates
anything the core needs. The file says it gates nothing (§1.0, R4) and that checks out against `WEB-MASTER
§2.1` (no spine row depends on `nether_forge_found`). **Confirmed not a mis-gate.**

**Resolution:** none needed beyond a one-line assertion in §6 that `undercroft_open` gates the Nether
*bearing-page* (inbound dependency, fine) and the Nether gates *nothing outbound* (INV-12). Keep the
near-pocket as P1; the bearing-trek stays P2 behind a playtest.

### S13 (P2) — producer boundary: re-dressing OCCUPIED vanilla structures via `SmallStructureBeat` is correctly resolved — verify the build honors it

progression `[S7-RESOLVED]` correctly states that re-dressing an *occupied* vanilla end-city/fortress is NOT a
clear-footprint `SmallStructureBeat` paste (impossible against `footprintClear`, `WEB-MASTER §0.5`) and routes
it to **additive pastes onto verified-clear adjacent air** (lanterns/lecterns/slabs beside vanilla blocks) or
`RevealBeat` block-state flips, never an occupied overwrite, never `RoomSwapBeat` (which is Undercroft-only).
This matches `WEB-MASTER §0.5` / `COHERENCE-AUDIT-V2 BP0-3`. **The design is correct.** The risk is purely at
build time: a careless implementer pastes over occupied end-city blocks.

**Resolution:** none in design; flag for BUILD-MANIFEST that the End/Nether dressing must pass the
`footprintClear` pre-check (additive-on-air only) and bind to the `world_paste_ledger` single-paste owner.

### S14 (P2) — reveal timing in the open End: re-dress an EXISTING structure, do not lazily paste toward a glider

progression `[S6-RESOLVED]` correctly identifies that a player gliding to a fresh outer island is the FIRST
loader of that chunk (no unwitnessed window), so the P1 End core must re-dress an **already-generated**
end-ship/city (force-load → mutate on unwitnessed relog → unload) or pre-generate a bespoke island at
world-build. **This is sound and genuinely sharp** (it is the one truly reveal-safe option in the open End).
The End-has-no-ambient-apparition-lane choice (R3) is also a *positive* canon choice, not a limitation.

**Resolution:** none — `[S6-RESOLVED]` holds. Just confirm the BUILD chooses the re-dressed-existing-structure
path by default and treats the bespoke pre-generated island as the optional P1-alt.

### S15 (LOW) — soul-sand vs the Pale herd: differentiation is correct, keep the one-sentence anchor

progression `[S5-RESOLVED]` differentiates soul sand (*"older than the first keeper… deep-time, before the
Kept"*) from the Pale herd (*this* group's present-tense conversion, `WEB-MASTER §7` plant #14). The §3(b)/(d)
exemplars anchor it explicitly. **Correct** — the two FACT-12 visuals no longer read as the same population.

**Resolution:** none; keep the "older than the first keeper" line load-bearing in D-NETHER-2.

---

## 4. WHAT IS AIRTIGHT (credit, both files)

- **Neither lane hard-gates the core.** Verified against `WEB-MASTER §2.1`: no spine `requires_flags` row
  depends on `nether_forge_found`, `seventh_seen_out`, `prologue_ignited`-via-map, or the lure page. The
  Nether/End are optional deepening (INV-12, INV-19); a portal-skipping, map-skipping group gets a whole
  un-shaded arc. **The Nether/End mis-gating the core is NOT a risk.** This is the single most important
  constraint and both files nail it.
- **INV-14 obedience** (both): the on-site `[a-z]` word is the answer, the coordinate/rune is a pointer.
- **The cursed-map mints nothing** (no FACT/INV/cipher/flag/site) — it is genuinely a new *surface* + *prop*
  for facts/sites/voice keys that already ship. The dead-uploader, the `6`, the rune key (`the-record-keeps`),
  and the frame-break all reuse BUILT canon. This is the discipline that keeps it from being the "disconnected
  gimmick" `arg-leaves §2a` warns about.
- **The cut/scale ledgers** in both files are honest (live counter CUT, in-map personalization CUT, full
  6-room hold scaled to 1-room, End-city binding P2/cuttable, Nether trek P2).
- **De-slop exemplars** pass the anti-slop law in both: cursed-map's *"hands are kept. you are not a file
  here"* and progression's *"i am the part of it that does not go out now"* / *"you only came to look"* are
  cold, declarative, no named emotion, no bow, no thematic tidy.

---

## 5. MUST-RESOLVE FOR SYNTHESIS (the short list)

1. **[P0] Master-ledger row collision (S1).** Both files self-number `WEB-MASTER §9` rows 24-27 with different
   plants. Synthesis must assign one contiguous block (e.g. cursed-map 24-27, progression 28-31) and both
   files must stop self-numbering. Normalize the status column. **This is the only hard blocker and it is the
   classic ROOT-A namespace sin on the plant ledger.**

2. **[P1] Cut the frame-break's FB-2(i) map-conduct callback (S5).** "you passed the seventh in the hold too"
   asserts a map-side fact the offline single-player datapack never transmits to the server — a precision lie
   (§6.4 / INV-16). Make the count-callback FB-2(ii) the only frame-break body; degrade-safe to `recordOpened`.

3. **[P1] Re-scope the cursed-map route task (S4).** `record/[slug]/page.tsx` is slug-AGNOSTIC today (no
   `params`, `grep slug` → 0), not "reads v_record without validating the slug." The downloads block needs
   `params` + a render branch + an in-voice 404 for non-lure slugs, preserving the `v_record`-only read.

4. **[P1] `decideFate` input edit stays PENDING, not scheduled (S9).** `FateInput.netherForgeFound` is a
   frozen-interface edit to `WEB-MASTER §5/§8`; progression §6 must mark it BLOCKED-on-ratification, with the
   "`kept` fires fully without it" self-test. Confirm `seventh_seen_out` is NOT a fate input.

5. **[P1] Seal "below the below" in canon before any Nether build (S11).** `[S3-RESOLVED]` is resolved-in-
   design only; `canon-spine` FACT 11 still has no source clause. LORE adds the one sentence (Undercroft = the
   bottom, the Nether = the fire's source, one direction) to the SPINE, or two authors will disagree on where
   "the deep" is.

6. **[P1] INV-16 teeth on the End-city `cast_out` place (S10).** Keep P2/cuttable; the cut condition is
   concrete — NO per-player carve or `LEFT_AT`-correlated dressing anywhere in `observance_end`, chorus-only
   rhyme. If unguaranteeable in the open End, ship the Seventh shrine alone.

7. **[P2] M4 catch is a shared budget (S2); one Mara author for all new Mara surfaces (S3); seal the `6`→six-
   keepers equivalence so it doesn't over-blurt FACT 15 on day zero (S6); the bare `#the-record` detector
   can't distinguish map-arrivals (S7 — build `from_map` or degrade the frame-break for everyone).**

**Verdict on both:** `minecraft-progression` is a sound KEEP-SCALED treatment; its `[S#-RESOLVED]` markers are
mostly honest, with S3/S11 being RESOLVED-IN-DESIGN-not-canon (the seals aren't written yet) and S10
conditionally-resolved (the cut must actually bind). `cursed-map-frame` is a strong, mints-nothing on-ramp
whose frame-break and dead-uploader/`6` canon are airtight **once FB-2(i) is cut** and the route task is
re-scoped. The defects are scoped build/namespace seams, not kill-shots — except S1, the ledger collision,
which is a real ROOT-A recurrence and must be frozen by synthesis before either §9 block is canon.
