# TEARDOWN — STORY / LORE / MYSTERY COHERENCE

Adversarial critic pass on the *story* dimension of The Observance. Judged from the
director's real problem: a veteran friend group, playing live, who will MISS most
subtlety. The lore prose is genuinely strong — these are not "the writing is bad"
findings. The problems are **over-weaving, contradictions the guides hide from
themselves, and a central mystery whose own number doesn't add up.** Worst first.

Ground rule I held to: the seeds + corpus are truth; the design guides over-claim. Where
WEB-MASTER says "≥2 doors, self-correcting, lands hard," I checked the actual `puzzles_seed.sql`
row and the actual `arc/lore/documents/*.md` text.

---

## THE 12 WORST (prioritized)

### 1. [FIX] The central number is broken: `six-were-kept-before-you.md` vs `the-seventh-not-kept.md` — the "6" cannot mean both "six prior keepers" AND "six kept of seven."
This is the spine's keystone plant (ledger #24, FACT 14/15, the day-zero `kept: 6`) and it
carries **two incompatible meanings in two canon documents.**
- `six-were-kept-before-you.md`: "*six times it was opened… six are named… you are the
  seventh.*" → the 6 = **six prior keeper-GENERATIONS/openings**, the group is the 7th group.
- `the-seventh-not-kept.md` + `the-record-opens` (D01 fragment): "*vaun, mara, sella, orin,
  brann, iss. that is six… there was a seventh*" → the 6 = **six kept keepers of one
  generation**, and the 7th is **the cast-out Seventh keeper.**
These are different sevens. The cursed-map `kept: 6` is supposed to pay off as BOTH (the
six prior groups AND the struck-seventh = the present group) — but the in-world stones say
"six" means Vaun…Iss, and the *struck seventh* in that frame is the **Seventh keeper**, not
the group. WEB-MASTER §0.3 carefully separates "fall-order six" from "ring six" but **never
addresses that the cursed-map `6` and the keeper-roster `6` are a THIRD collision** — the
one the players actually start the game on. A friend group will conflate "you are the
seventh" (the count) with "the Seventh, cast out for nothing" (the keeper) instantly, and
the game will feel like it's calling them the doomed one.
**Fix:** pick ONE referent for the lure `6`. Cleanest: the lure `6` = the six prior keeper
generations (groups), and rename the in-generation roster count so the stones never say a
bare "six" that reads as the same number. Add a canon-spine §8.6 "the THREE sixes" and make
`the-record-opens` say "six generations" not "six are named," or the keystone lands as confusion.

### 2. [CUT] The Nether lane (`the-fire-is-lent.md`, threads #28/#30, FACT 11-deepened) — a fully-authored optional dimension that pays off a fact already paid off, gated behind an unbuilt sentence.
The Nether lane's entire job is to deepen FACT 11 ("the keeping was always a carrying")
and tint the M5 close with `nether_forge_found`. But: (a) FACT 11 is ALREADY delivered by
`undercroft-fog` ("one fire, no one tends it") and `the-fire-is-lent` is readable on the
Undercroft shelf WITHOUT the Nether trek; (b) WEB-MASTER §1.M3 admits the lane is **blocked
on LORE sealing one sentence into canon-spine FACT 11 that does not exist yet** ("`[S3-RESOLVED]`
is resolved-in-design, not in-canon"); (c) the payoff is a single flag that adds ONE tinted
clause to a close already capped at ≤2 clauses, so it usually gets out-prioritized and shows
nothing. A remote friend group will not org a second-dimension expedition for an un-shaded
tint they'll never see. This is the single heaviest "noise the players will never connect."
**Fix:** CUT the Nether build. Keep `the-fire-is-lent.md` as an Undercroft shelf page (it's
beautiful and it carries the carrying-not-owning theme for free). Reclaim the budget.

### 3. [CUT] The End lane (threads #29/#31, `seventh_seen_out`, the exile-hold) — same disease as the Nether, plus an INV-16 self-destruct clause.
The End lane makes the cast-out fate "a place." But by its own spec (WEB-MASTER §1.M5, S10)
the exile-hold "**names no living player and encodes no per-player side… if the open End
cannot guarantee that, the binding is CUT and the End ships as the Seventh shrine alone (the
default).**" The design has already written its own cut. It's an optional dimension whose
only non-default payload is conditionally forbidden. `seventh_seen_out` is explicitly NOT a
fate input — it only licenses a re-read clause that **replaces** the neutral clause, i.e.
adds zero net information. Two custom dimensions (Nether + End) for two tint clauses that a
live group sees neither of.
**Fix:** CUT the End build to its declared default (Seventh shrine only, already in the
Overworld via `the_unwriting`). Keep D11's "one door, two sides" line — it's the best line
in the corpus and needs no dimension to land.

### 4. [REDESIGN] FACT 17 / "the Ear" (thread `the-record-axis-of-word`, `against-each-ground-what-was-said.md`) — a whole canonical fact + filing axis that no friend group will ever detect, riding on an unbuilt P3 voice layer.
FACT 17 ("the record files what is *said*") is the third filing axis: name/place/word. Its
"≥2 doors" are (a) a single Archivist clause buried as the second half of FACT 16's fragment
("against each ground, what was said over it") and (b) the Ear's `SpatialVoiceBeat` — a P3
Simple-Voice-Chat layer that **degrades to a pack-sound whisper if it never installs.** So
the realistic delivery is: one comma-clause in one document, that says "the record logged
your talk." Nobody infers a *filing axis* from that. FACT 17 is the clearest case of canon
manufactured to fill a namespace slot (synthesis even admits it was "not self-minted by the
P3 garnish file… assigned here continuing the sequence"). It deepens nothing the players
will perceive.
**Fix:** demote FACT 17 from a numbered fact to a one-line flourish on the Archivist register
(keep the clause, drop the axis). If the Ear ships, it's a cool *texture*, not a *fact*.

### 5. [FIX] WORLD-BIBLE vs canon-spine — the Sacred Beast is a "deep-bird/canary" in `arc/WORLD-BIBLE.md` and a generic "glowing animal" everywhere in the synthesis layer; Sella's custom contradicts her fate.
`arc/WORLD-BIBLE.md` L55/L106 canonizes the Sacred Beast as **the Deep-Bird (a canary,
early-warning)** and assigns it to **Sella** ("the deep-bird (Sacred Beast)… taken early").
But canon-spine §1 and story-map assign Sella to **the Bow/markers (wandered past, the far
water kept her)** — drowning, not a bird. And the synthesis/herd thread (`the-sacred-beast-first-thing`,
INV-13) makes the Sacred Beast a **glowing Pale herd animal tied to Vaun's "first thing,"**
not a warning-canary. Three different Sacred Beasts. The Watcher's own B2 report
(`the_sacred_beast`) says "the deep-bird sings while the air is good" — i.e. the *built voice*
sides with the canary, while the *herd thread* and *fork A* side with the glowing animal.
**Fix:** pick one. The herd/glowing-animal version is the one with mechanics (INV-13, the
permanence fork); make WORLD-BIBLE and the B2 `the_sacred_beast` report match it, or the
custom the players are graded on is described two ways.

### 6. [SIMPLIFY] The Iss thread is over-instrumented: `stone-iss-wall` / `iss-warm` / `iss-doubt` / `no-wall-catch` / `prophet-wall-comfort` / `prophet-wall-name` / `bound-word` — SEVEN puzzle rows for one reveal ("the warm guy lied").
The catch is the emotional engine (correctly). But the seed splits it across two warm-vs-
skeptical readings of the same stone (`iss-warm` sets `iss_trusted`→dead shrine;
`stone-iss-wall` sets `iss_key_turned`→`iss-doubt`), PLUS a two-row prophet's wall
(`-comfort` opens nothing, `-name` is a columnar acrostic of "the one who turned away"),
PLUS the bound word that is ALSO "the one who turned away," PLUS the false walk. A live group
will not distinguish "the warm reading" from "the skeptical name-as-key reading" of one
stone — they'll type whatever they got and the resolver branches them invisibly. The
prophet-wall columnar acrostic (`prophet-wall-name`) spelling Iss's name is god-tier-attention
content that re-reads at the catch — the kind of thing the brief explicitly flags as "never
noticed."
**Fix:** keep `stone-iss-wall → iss-doubt → no-wall-catch` and the false walk. CUT
`prophet-wall-name` (the columnar acrostic) — it's a third encoding of "the one who turned
away" that nobody decodes. SIMPLIFY `prophet-wall-comfort` to a single found carving, not a
puzzle row.

### 7. [CUT] The `UNKEPT` meta-acrostic (`meta-unkept`, thread `the-unkept-acrostic`, §8.5) — gates nothing, requires reading six maker's-mark framing glyphs across six scattered stones in a specific fall-order, and is the textbook "players will never assemble this."
By the design's own admission it "gates nothing — pure recontextualizing texture." To get
it, a player must: notice a tiny framing glyph on each of six stones (not in the cipher
text), record all six, learn the fall-order is Vaun/Mara/Sella/Orin/Brann/iss (a key dropped
by the cold Iss at the catch), and anagram. The "self-correcting in ring-order" cleverness is
invisible if nobody tries either order. This is the single least-likely-to-land payoff in the
whole arc, and it costs six in-world art assets + a staged activation + a voice key.
**Fix:** CUT to a single Watcher line at the catch ("six marks, one word, the word is the one
they did not keep — UNKEPT") delivered as told lore. Lose the six-glyph hunt; keep the chill.

### 8. [REDESIGN] The three-hands coop gate (`m4-three-hands`, thread `the-coop-gate`) — "foot on a plate + a carve + a Discord post in the same ~20s window" is the one genuinely un-fakeable group beat, and it's the most likely hard-lock in the game for a remote/async group.
The premise of the entire project (per MEMORY) is a **scattered, remote, async** friend group
whose "only reason to gather" is the cursed map. Then the spine's M4→V hinge **requires three
simultaneous real-time acts within 20 seconds** — exactly the synchronization the group's
async nature makes hardest. Mara's "i typed into the dark, alone… the threshold counts three"
is a lovely plant, but the payoff is the single point where an async group stalls indefinitely
with no fallback (unlike `requires_flags` lore rows, a timing gate has no deterministic
backstop). The brief's "will it work in-game" bar fails here for *this specific group*.
**Fix:** keep the *idea* (the threshold needs the group together) but widen the window to
minutes and make the three acts sequential-within-a-session, not simultaneous-within-20s.
Or make it 2 acts. The theme ("you cannot do this alone") survives a generous window.

### 9. [FIX] The Seventh vs Iss ambiguity is canon-fragile: `the-seventh-not-kept.md` and `the-ways-are-a-wall` are cross-linked such that a sharp group WILL collapse them, against canon §5's "keep distinct."
canon-spine §5 says: default is the Seventh and Iss are **distinct**, "keep a thin ambiguous
line… author no fragment that collapses them before M4." But `the-seventh-not-kept.md` quotes
Iss's wall-lie *inside the Seventh's own map-note* ("iss told the keepers-after that the ways
are a wall… i wanted to [believe him]"), and the doused-hearth/cold-shrine is BOTH Iss's
false-walk endpoint AND the Seventh's deep (the §0.4 "two distinct places, one cold hearth").
Sharing a physical anchor between the liar's herring and the cast-out keeper's grave is asking
a group that misses subtlety to read "Iss = the Seventh." The distinction (Iss broke faith and
stayed in the record; the Seventh broke nothing and was erased) is precisely the kind of fine
line this group will flatten.
**Fix:** either commit to distinct and physically separate the dead-shrine from the
seventh-deep (different sites), or commit to "the group is allowed to wonder" and stop
spending canon notes defending a distinction the level geometry undermines.

### 10. [SIMPLIFY] FACT inflation: 15 base facts + 6 namespace-children (16, 2b, 7b, 13b, 10b, 17) + 4 fates + 2 codicils + 10 invariants. The "web" is so dense that the seven load-bearing foreshadows of FACT 15 are buried.
canon-spine §3's web-check claims FACT 15 is "supported by ≥7 independent foreshadows
(3,4,6,11,12,13,14)." That's the real spine and it's good. But it's now surrounded by 2b/7b/
13b/16/17/10b — children that exist mostly to satisfy the namespace pass, each with its own
"≥2 doors" obligation that forces more surfaces into the world. The result is a corpus where
the *strong* foreshadows (the kept did not depart / one fire never out / the rite wants a piece
of you) compete for attention with *manufactured* ones (the record files by word). Players have
finite attention; every child-fact spends some.
**Fix:** treat 2b/7b/13b/16 as *texture under* their parent facts, not numbered canon. Keep
10b (it's the Seventh's whole point) and FACT 13b (the grave-as-appointment is a genuine
top-tier payoff). Drop the numbering ceremony for the rest.

### 11. [FIX] The difficulty engine (`difficulty-mara`, FACT 2b, `deepTightens`/`deepIsPatient`) — "the land was grading your MASTERY all along" is a fact the players cannot perceive and may resent.
The dynamic-difficulty reveal asks the group to realize, retroactively, that the drip pacing
they experienced was a diegetic judgment of how fast they solved. But hint-withholding is
indistinguishable from "the game was being stingy" — there's no in-world *signal* a group can
point to and say "ah, it sped up when we slowed." Mara's "closer count of the quick" line is
nice, but the *mechanic* it foreshadows is invisible-by-design (it only "colors pacing"). A
fact whose entire payoff is "the thing you couldn't feel was on purpose" doesn't land; it just
risks reading as the game having been unfair.
**Fix:** keep the difficulty engine as an INVISIBLE pacing tool (good UX), but CUT FACT 2b as
a *revealed* fact. Don't promise the players a reveal they have no surface to perceive.

### 12. [SIMPLIFY] The cursed-map prologue (M0, `the-mara-uploader`, `the-readme-lie`, `the-doused-hearth-prop`) — four plants (#24-27) carried by an offline single-player vignette that the design admits "carries zero spoilers, a cracked world file reveals only what the player already saw."
The M0 vignette is a 6-room (1-room-first) single-player download whose payoffs are all
deferred to M4. By its own spec it must contain no FACT past 1/2, no keeper name past the
Archivist, no cipher key, no server flag. So it is a melancholy walk that plants four things
(`m.kept` uploader, "doesn't connect to anything," doused hearth, scraped seventh) — all of
which are ALSO present in-server. The `m.kept` = Mara payoff (#26) requires the group to
remember a sign-off on a web page from before they joined, then connect it at M4 to a keeper
voice. For a friend group, the realistic outcome is: one person plays it, posts "kept," nobody
remembers the uploader's name two weeks later.
**Fix:** keep the lure page + the `6` + the "post kept to join" ignition (those are load-bearing
on-ramp). SIMPLIFY the vignette to the 1-room form permanently. CUT the `m.kept`→Mara payoff as
a required connection — let it be a bonus nobody is graded on noticing.

---

## SECONDARY (still specific, still real)

### 13. [FIX] The forged eighth law (`forged-eighth`, FACT 7b) collides with the real eighth (`the_unlit_deep`) by design's own worry — and the "proof of the lie is nothing happens" is unfalsifiable to players.
WEB-MASTER §3.3 spends a whole section defending why "two eighths" don't collide. The fact
that it NEEDS that defense is the smell. Worse, the falsification ("obey the Covering and
nothing pays") is identical to the player experience of obeying a custom they're keeping
correctly (also "nothing happens, you just stay clean"). A group cannot distinguish "this law
is fake because no toll fires" from "I'm keeping this law so no toll fires." The forgery is
legible only to someone who decodes the substitution signature to "cover one's own" AND notices
its absence from the ring — high-attention again.
**Fix:** keep the forged law as a found document and the M4 `archiveEighthCorrection`
collapse (that line is great). CUT the expectation that players *deduce* the forgery from
toll-absence; let the Keeper/record just tell them at the catch.

### 14. [KEEP] The keeper voices (Vaun accumulates / Mara cites / Sella mirrors / Orin breaks off / Brann doubles / Iss reassures) — this is the strongest, most legible part of the story and it earns its place.
`journals-*.md` + the grammatical-fingerprint table (WEB-MASTER §6) actually deliver six
distinguishable people. `the-fire-is-lent.md` (Mara citing p.11 l.3, then the one personal
margin she "would not write as a sentence") is genuinely moving and reads exactly as Mara.
Orin's "i —" doubling as both grief and the E of UNKEPT is the one acrostic touch worth keeping.
Do not cut the keepers. If anything, lean HARDER on the chorus and let the over-built ARG
machinery fall away around it.

### 15. [FIX] Voice mannerism — the Watcher's "X. it is true. it opens nothing." / "kept… in the other sense of kept" tic is becoming a verbal crutch.
The Set-B register is a real cold voice and mostly holds. But the *contrast-with-prior-belief*
construction the slop pass supposedly banned is still all over the new keys:
- `oracleDeadEnd`: "it is a true name. it keeps no door." / "it is the right place for the
  wrong thing." / "it is still true. knowing it twice opens nothing new."
- the warm/cold flip: "you will be received… and kept — in the other sense of kept, the sense
  you will learn last." This "the other sense of kept" appears in `light_taken`
  ("kept elsewhere, in the other sense of kept"), the cold flip, and BN keys — it's now a
  signature move, which makes the Watcher *mannered* rather than flat.
The construction "the count was never of the dark. it was of the hands" (docketReread) is
exactly the "contrast with prior belief" the slop note A-list says to cut, and it survived.
**Fix:** ration "in the other sense of kept" to ONCE in the whole arc (the M5 flip). Audit
every "it was never X, it was Y" — that's the rhetorical mirror the Watcher is forbidden.

### 16. [SIMPLIFY] `name-where-never-been` (FACT 16, `name-where`) — "your name carved where you've never been" is a great scare; "it means the record files by PLACE as a third index" is over-explaining a good chill into a filing-system lecture.
The scare (your name at a cell you've never visited) lands on its own. Promoting it to "FACT
16, the place-filing axis, child of FACT 1" and pairing it with FACT 17's word-axis turns a
ghost into a database schema. The `keeper.nameWhere` line ("it files the ground first and the
foot after") is the lecture.
**Fix:** keep the carve + the chill. Drop the "filing axis" framing; the player doesn't need
the index theory to feel watched.

### 17. [FIX] `the-record-keeps` URL door (#11, `record-url`) — the fourth-wall break is good, but the decode chain (founder margin → cipher → URL path → typing it → recognizing it's the address you already used) is a four-hop inference no one completes live.
The intended "click" is that the in-game decoded phrase `the-record-keeps` matches the
`/record/the-record-keeps` lure-page slug the group already visited. That requires remembering
the M0 URL precisely. It's a lovely idea that depends on perfect recall across weeks.
**Fix:** keep the website as live texture (it un-redacts as stones are read — that's felt). CUT
the requirement that players *derive* the slug; let the recognition be a bonus, and have the
website link surface in Discord so the loop closes for everyone, not just the one who memorized a URL.

---

## IF I COULD CHANGE ONE THING

**Cut the arc in half and let the six keepers carry it.** The strongest story asset by a wide
margin is the rhyming chorus — six distinct, broken people whose fates mirror the players'
tracked behavior, culminating in Iss's warm lie and its cold catch, the Seventh who broke
nothing, and the rite that *receives* instead of rewards. That spine (FACT 1→2→6→8→10/10b→11→
12→13→14→15) is coherent, legible, and lands. Everything bolted around it — the Nether and End
dimensions, FACT 17/the Ear, the UNKEPT six-glyph acrostic, the prophet-wall columnar name, the
difficulty-as-revealed-fact, the place-filing axis, the dual cursed-map vignette — is
namespace-driven over-weaving that a real friend group will never connect, each item spending
world surfaces and attention the core needs. The project's stated north star is "From The Fog,
but it knows your name" — intimate and reactive. Right now it reads as "From The Fog, but it has
24 threads, 21 facts, 10 invariants, two extra dimensions, and a filing-system thesis." The
mystery will land *harder* with two-thirds of its cleverness removed and all of its keepers intact.
