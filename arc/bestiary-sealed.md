# ⚠️ SPOILER — SEALED — DO NOT READ IF YOU WANT TO PLAY UNSPOILED ⚠️

This is the twist-bearing design for the bestiary of *The Observance*. It names the
sealed truth in plain words. Ethan intends to play unspoiled — **close this file.**
The showrunner and plugin read it; you do not. The spoiler-free player-facing version
is `design/bestiary.md`.

> Reconciled against: `arc/_SEALED_ARC_BIBLE.md`, `arc/lore/canon-spine.md`,
> `arc/lore/LORE-BIBLE.md`, `arc/lore/found-documents.md`, the 12 documents in
> `arc/lore/documents/`, `DESIGN.md` (anti-jank contract), `design/arg-deepening.md`
> (the six keepers, the mod stack, the Seventh), and the beat library in
> `plugin/src/main/java/com/observance/watcher/beats/lib/`.

---

## 0. THE SEALED TRUTH OF THE BESTIARY

The single sealed spine (`canon-spine.md`): **the presence is the accumulated keepers**
— the land's memory of everyone who ever kept the ways. To be *accepted* is **induction**
into the watching, becoming a marker for whoever comes next. Judgment is **collective**;
there is no chosen one.

The bestiary is the **embodiment of that spine.** Each apparition **is a prior keeper** —
not a monster the keepers fight, but a keeper who was kept, now worn by the land as a
shape. The six creatures are the **six prior keepers** of `canon-spine §1`. The seventh
apparition is the **cast-out Seventh** of `the-seventh-not-kept.md` — the inverse: a keeper
the land *refused*, who is therefore *not* in the watching, an absence rather than a shape.

This is why the bestiary must obey **FACT 15 discipline** (`canon-spine §3`, §6 rule 2):
**no apparition, no name, no beat may state the induction twist in words.** The creatures
*are* the foreshadow — the strongest one in the whole project, because they are literally
the kept keepers standing in front of the group. The group should slowly feel "these
things are people who were here before us… these things are *kept*… and we are being
asked to keep the ways like they did" — and never be *told* what acceptance costs until
the world flips at the Accepting (Movement V).

**The rhyming chorus, made flesh.** `canon-spine §1` and `arg-deepening §2` establish that
each prior keeper's fate **rhymes** with a behavior the tracker already measures in the
current group, *without singling out a living favorite*. The creatures are the sharpest
edge of that: the Watcher-as-Vaun appears most readily to the player whose `hoardedScore`
is highest; the Surface-Walker-as-Sella to the player with the highest `distanceFromGroup`.
The land is showing each player **the shape of who they are becoming** — collectively,
never as a callout. Rule 9 (`canon-spine §6`): the corpus must never say "this keeper is
you." The creature implements that as *gating bias*, never as a named accusation.

> **Sealed reading (2026-07-05 — `canon-spine.md §0`/§1's deeper seal).** "Six prior keepers"
> still names the six creatures correctly and nothing below needs to change: each is a genuinely
> distinct keeper, with a genuinely distinct fate, cipher, and voice. What changes is only what
> stands behind the six — they are not six people the land took separately and now wears as six
> shapes; they are the six the one taken mind wore to survive being the only one down there. A
> creature is not "part of a crowd of former people." Each is a whole way a single long aloneness
> held itself together. This changes no beat, no wiring, no gating bias — the rhyme, the grounding,
> the FACT-15 discipline all hold exactly as written below.

---

## 1. THE SIX KEEPERS AS THE SIX APPARITIONS

Mapping table — creature ↔ keeper ↔ custom ↔ document ↔ tracked rhyme. (Player-facing
names from `design/bestiary.md`; sealed identity here.)

| # | Player-facing | IS (keeper) | Custom embodied | Source document(s) | Cipher rhyme | Tracked behavior it rhymes with |
|---|---|---|---|---|---|---|
| 1 | The Watcher at the Edge | **Vaun, the Hoarder** (also the canonical "the watching" silhouette) | The Offering (failed) | D02 `counted-them-in-the-dark`, D01 `the-record-opens` | Caesar (held back a fixed amount) | high `hoardedScore` + `soloMiningRatio` |
| 2 | The One Who Counts | **Vaun, the Hoarder** (his *intimate* aspect) | The Offering | D02 `counted-them-in-the-dark` | Caesar | `hoardedScore`, Offering `violationRatio` |
| 3 | The Surface-Walker | **Sella, the Drowned** | The Bow / the markers (wandered past them) | D06 `what-the-surface-keeps`, D11 (handoff) | Atbash / mirror (A↔Z) | high `distanceFromGroup` (the wanderer) |
| 4 | The Stoop | **Orin, the Silent** | The Bow (never bowed, until too late) | D07 `i-thought-it-small`, D04 `observed-warned-left-at-threshold` | substitution (plain alphabet, withheld) | Bow `violationRatio` (passes markers standing) |
| 5 | The Sleepless | **Brann, the Night-Walker** | The Dark Hours (slept on the black moon) | D08 `do-not-close-your-eyes-here` | beacon / colour-sequence | sleeping on black-moon phase; Kept-Light lapse |
| 6 | The Quiet Herd (Sacred Beast) | the keepers' kept faith, embodied — **the small life the markers stand for**; tonally **Mara**'s lonely watching | The Sacred Beast / the Bow's object | (the Haunted Herd, `arg-deepening §3`); Mara D05 tonal | book-cipher (Mara) | `mobKills` on the tagged beast |
| 6b | The Pale field (cosmetic) | **the going-out itself** — the taking re-enacted as the herd turns pale, family by family | (the process behind all customs — the conversion) | herd count fragments (Mara, bookCipher) | book-cipher (Mara) | **none** — cosmetic, never tracked (INV-13) |
| 7 | The offline-skin apparition | **a prior keeper's fate**, worn by **an absent friend** (re-skin of 1/3/4) | the custom that keeper broke | the M1 offline-player report; carrier of **FACT 9** | (inherits the worn keeper's) | the offline player canonically rhymed (OFFLINE-only, INV-16) |
| — | Cold Hearth's tenant | **the Seventh, cast out** | the inverse of all customs | D11 `the-seventh-not-kept` | none (unwritten) | optional; non-gating |

> **Note on Vaun doubling (creatures 1+2).** Vaun is both the canonical *watching
> silhouette* (the founding-line keeper who "counts the living," the literal origin of the
> record's gaze — see D02's "the land counts first… i only keep the tally after," and D01's
> "the watching has already watched") **and** the intimate hoard-haunting. We split his two
> registers into two creatures so the marquee silhouette (1) and the per-player chest dread
> (2) can fire independently. Both *are* Vaun. This is deliberate: the first keeper is the
> one most fully merged into "the watching," so he wears two shapes.

> **Note on Mara (creature 6 tonal, not literal).** Mara, the Reader, has no single creature
> of her own — by design. Her fate is *absence of action* (the map, never the tool; she read
> by the Kept Light too long and it outlasted her). A keeper who only ever *watched and read*
> is most honestly embodied as the **watching itself** rather than a discrete monster — she is
> diffused into the reports, the lecterns, the Kept Light that never guttered (D08), and the
> still, observing quality of the whole bestiary. The Quiet Herd carries her *tonal* note
> (lonely, watching, never intervening). If a discrete Mara apparition is ever wanted, build
> it as a figure seen **only at a lectern, reading, never looking up** — see §5 options.

> **Note on Iss (the Liar) — intentionally NOT a creature.** Iss's whole function is
> *dialogue that re-reads* (`canon-spine §4`): warm NPC who lies, caught later, his tree
> flipping warm→cold. That is an **NPC + dialogue-state** payload (ZNPCsPlus / Citizens2),
> not an apparition. Giving Iss a monster shape would telegraph the lie. He stays a voice.
> His "creature," if you insist on one, is the *cold re-read itself* — the same warm figure,
> seen again after the catch, now silent and turned away (a dialogue-state-driven model swap,
> §5).

---

## 2. PER-CREATURE SEALED DESIGN

For each: the keeper's fate (from the documents), how the creature **rhymes** it at the
tracked group, the exact **becoming** discipline (how it foreshadows induction without
blurting), and the FACT-15 seal.

### 2.1 — The Watcher at the Edge / The One Who Counts = VAUN

**Fate (D02).** Vaun kept everything and gave nothing back to the deep. His ledger runs
down one column; the second — *given back* — is "a long rule across… nothing in it." The
deep went dark *for him alone* ("not for the others — for me"). He starved in the light he
hoarded. The Keeper's closing margin: *"he is counted. he counts still. that is not the
same as kept."* Vaun did not die into nothing — he counts **still**. He is the watching's
oldest tally-keeper. That is the seed of the twist: a keeper who "counts still" is a keeper
who **became the counting**.

**Rhyme at the group.** The silhouette (creature 1) fires most readily for the player with
high `hoardedScore` (`InventoryScanner`) and high `soloMiningRatio` — the hoarder watches
the hoarder. The intimate aspect (creature 2) appears at that player's chest cluster
(`BaseDetector`) and tolls *their* light (`TorchGutterBeat`) — "the deep goes dark for
them," exactly Vaun's seventh-winter fate (D02), exactly the Offering escalation
(`_SEALED_ARC_BIBLE.md` custom table: "the deep goes dark for them"). Grounded: it only
fires on a hoard the tracker measured (anti-jank #6, `canon-spine §6` rule 4).

**Becoming discipline.** The figure is *counting* — the same posture as the record. It is
never named "Vaun" to the player until/unless a Movement-II keeper-stone vision attaches the
name (the keeper apparition at Vaun's stone, `arg-deepening §1.2 step 5, §2`). The dread is:
*the thing that watches my hoard is shaped like a man who hoarded, and he is still here,
counting.* Step toward induction, never name it.

**FACT-15 seal.** Never let any line say Vaun "became the watching." The world says only
that he *counts still* (D02 margin) and lets the group infer. The silhouette's only "speech"
is the report and the stone-vision, both grounded.

**Beat wiring.** `NamedMobBeat` (`entity:"mythicmob:watcher"` / fallback `WARDEN`,
`no_ai_drift`, `silent`, `invulnerable`, out-of-LoS via `findSpawn`); `ChestArrangeBeat` +
`FakeBlockBeat` (per-player "looked-at" stash); `TorchGutterBeat` + `PrivateSoundBeat` (the
cold rhyme). Gated by `DramaBudget` + Offering `complianceFor("offering").violationRatio()`.

### 2.2 — The Surface-Walker = SELLA, the Drowned

**Fate (D06, D11).** Sella learned the markers and did not keep them; she walked past the
last stone, where the bow was owed, to a far water and **did not come back**. "The surface
keeps everything… you look down to be sure of yourself and it has already taken the looking
and folded it and given it back wrong." She speaks now **only as a reflection** (her
inviolable voice, `canon-spine §6` rule 8). The margin: *"do not follow me. the others were
kept. i was [...]."* The lake set her seventh stone for her.

**Rhyme at the group.** Surfaces for the player with high `distanceFromGroup` (the wanderer,
the one who walks off alone) at water cells the heatmap shows the group visiting. Seen
**only in reflection**, per-player — the social-horror of one player insisting on a second
face. Her warning ("do not follow me") rhymes mechanically with the soft-pressure rule:
wandering off alone makes the thread *quieter and stranger*, never louder.

**Becoming discipline.** She is the clearest "kept" foreshadow: *"the others were kept. i
was —"* (the sentence breaks). The group should feel that being "kept" is something that
*happens to you at the far water*, ambiguous between rescue and capture (D06, D11's "to be
kept and to be cast out are one door"). Never resolve it. The reflection-only rendering is
itself the foreshadow: she is a keeper the surface *keeps* — present only as the land's
held image of her.

**FACT-15 seal.** Her reflection never says what "kept" means. The Atbash/mirror cipher she
characterizes (`canon-spine §1`) reinforces "read the other side" — the inverse-reading that
the whole arc uses to walk up to the twist (Iss's lie is the inverse of the truth, §3 of the
spine). She points at the Seventh (D06→D11 margin handoff) and stops.

**Beat wiring.** `NamedMobBeat` / ZNPCsPlus packet apparition at a water anchor, out of LoS,
`silent`, short `despawn_seconds`; `PrivateParticleBeat` (surface disturbance, one player);
`PrivateSoundBeat` (lap/breath behind). Gated on `distanceFromGroup` + drama budget.
**Cross-tie:** shares its surface with the Atbash reflection-puzzle (D06 clue, `arg-deepening
§1.2 Stone 3`), so apparition and cipher live on the same lake.

### 2.3 — The Stoop = ORIN, the Silent

**Fate (D07, D04).** Orin would not bow. He "thought it small. it was not small. i —" — the
sentence breaks; the record finishes it (D04): *"i was not kept. i was counted, and the
count was true, and it was not enough."* He was **observed, warned, left at the threshold**
while the rest were kept. He bowed at last to no one. He speaks **only when the reader is
crouched** (his inviolable voice).

**Rhyme at the group.** Tied to the **Bow** custom and `PlayerToggleSneakEvent` at markers
(`CustomComplianceListener`). For the player with a high Bow `violationRatio` (passes markers
standing), the marker "gains a name and watches" — the exact escalation in `FLOW §1` and the
`_SEALED_ARC_BIBLE.md` custom table ("markers watch the offender"). The kneeling figure is
revealed only by *your* crouch: the creature literally enforces the bow as the condition for
seeing it, the same way D07 is legible only from a crouch.

**Becoming discipline.** Orin is the **kept/left binary made visible** (FACT 6): the others
were kept, Orin was left. The Stoop foreshadows the cast-out ending (a player can be "left at
the threshold") **collectively-safe** — it never names a living player as Orin, only biases
the marker-watching toward measured non-compliance. D04's foot-margin is the key seal: *"ask
yourself, before you envy them, which of the two is still free to leave"* — the nearest the
corpus comes to "kept = captured," delivered as grief, never explained.

**FACT-15 seal.** The Stoop never says bowing inducts you. It only makes the smallest custom
feel *counted*, and lets D04's "kept… as the stones are kept" do the quiet work.

**Beat wiring.** `NamedMobBeat` at a marker anchor, crouch-gated reveal (reuse Bow detection
in `CustomComplianceListener`); the watching-marker escalation = `NamedMobBeat` with a `name`
+ `glowing:true`; `PrivateSoundBeat` (low breath on bow). Gated on
`complianceFor("bow").violationRatio()`.

### 2.4 — The Sleepless = BRANN, the Night-Walker

**Fate (D08).** Brann slept when told not to, on the black moon, "and what i dreamed did not
stay a dream — it came inside, and it has not gone back out, and it walks when i walk." He
walks the nights now and will not lie down. He carries **FACT 11** (the one home-fire that
"would not be doused," that "outlasted her reading," that will "burn on in the empty dark
with no one to feed it") and **FACT 12** ("of the faithful before me it is not written that
they left. it is written that *they were kept.* as a stone is kept. as the light is kept.").
His voice: **only at night / black moon.**

**Rhyme at the group.** Time-gated to night / black moon (`fullTime/24000 % 8`, the Dark
Hours check). At a detected base after dusk with **no kept light** (Kept Light custom), the
"it comes inside" beat fires (the custom table's escalation). If a player **sleeps on the
black moon**, the nightmare beat fires and the bed denies — exactly Brann's transgression
re-enacted at them.

**Becoming discipline.** Brann is the document that comes **closest to the twist in feeling**
without stating it: FACT 12 is "the strongest foreshadow" (`LORE-BIBLE §3`). His whole
warning — *"it does keep you. that is not the comfort you take it for. ask who [the fire] is
being kept for"* — is induction-shaped and induction-silent. The creature must carry that
tone: the cold at home is *almost* tender, "we would keep you" (`canon-spine §6` rule 5,
warmth under dread). **F9 hook:** D08's "what came inside… walks when i walk" is the seed for
FACT 9 (the first hauntings were a keeper's fate re-enacted) — see §4.

**FACT-15 seal.** Brann never explains who the fire is kept *for*. He asks the question and
leaves it (D08's last lines). The Sleepless apparition asks it the same way: by guttering a
light and never saying why.

**Beat wiring.** `NamedMobBeat` surfaced only at night/black moon; `TorchGutterBeat` +
`DoorOpenBeat` + `PrivateSoundBeat` (cold comes inside); nightmare = `PrivateSoundBeat` +
title beat + `PrivateDarknessBeat`. Gated on Dark Hours + Kept Light tallies + moon phase.

### 2.5 — The Quiet Herd (Sacred Beast) = the kept small life (Mara's tonal note)

**Fate / role.** The Sacred Beast is the **Haunted Herd** side-mystery (`arg-deepening §3`):
a pale tagged animal the herd watches; killing it is a tracked transgression, protecting it
earns a quiet boon at the Accepting. It is not a keeper who *fell* — it is the **object of a
custom**, the small kept life. Tonally it carries **Mara** (the Reader): the one who only
ever watched and never acted, lonely under her precision (D05). The herd that stands and
watches, never intervening, is Mara's posture made into many bodies.

**Rhyme at the group.** Pure conduct. Tracked via `mobKills` + the `sacred_beast` PDC tag
(`SacredAnimalBeat` + `DeathListener`). Protect across the run → boon at the ending; kill →
named transgression in the reports. This is the gentlest "what kind of tenant are you" test —
keeping a small life, not just a law.

**Becoming discipline.** The herd "watching" is the **bestiary's thesis in miniature**: the
ordinary world turning to observe you. It foreshadows the twist by making *being watched by
the kept* feel mundane and total — the same gaze the markers, the reports, and the Watcher
carry, but worn by cows. It must never be explained. (The boon it grants at the Accepting is
sealed in the ending design.)

**FACT-15 seal.** Nothing here names the twist. The herd just watches, and remembers.

**Beat wiring.** `SacredAnimalBeat` (`match_type`, `glow:true`, PDC `sacred_beast`,
persistent, silent, idempotent); kill tracked via `DeathListener` PDC check; collective-gaze
pass kept cheap/vanilla.

### 2.5b — The slow herd conversion (the Pale field) = the going-out, made cosmetic

**What it is.** The Sacred Beast (§2.5) is the one glowing, tracked, kept small life. The **Pale
field** is the other half: a slow drift of cosmetic pale animals (`pale_cosmetic` PDC, never
glowing, never tracked, never a violation — INV-13) that climbs between sessions, the herd turning
pale a few head at a time, all of them facing one way. The conversion is never witnessed happening
(reveal discipline): the group only ever finds *more of them pale than there were*, and finds them
already turned to watch.

**The keeper-enactment (sealed).** The Pale field is not weather and not decoration — it is **the
going-out, re-enacted in miniature** (timeline §4 step 5: "family by family, lamp by lamp, the
Kept were taken"). The grey herd is the settlement before the breaking; each head that turns pale
is a family taken; the pale ones facing one way are the taken, *watching*, exactly as the Kept
became watchers. The land is not showing the group a haunted herd — it is showing them **the
conversion itself**, at the same patient cadence it once happened to the Kept, in the one register
that needs no blood and breaks no anti-jank rule: animals, quietly, turning to look. The prior-
keeper count fragment ("nine grey, one white" → later "they were grey when i shut the door") is
the same event in a keeper's hand: a herd that was grey when a door closed and is not grey now.
A second, later hand keeps the same count in `keeper-tally.md` (M2→M4, three degrading leaves) —
the two documents are the same watch kept by two keepers, in two eras, of the one going-out.

This is the bestiary's thesis stated in the gentlest possible material. Every other creature is
*one* kept keeper worn as a shape; the Pale field is the **process** that makes them — the slow,
total, mundane turning of the ordinary world into the watching, shown not as a single shape but as
a *spread*. It is the FACT-15 visual: by Movement V the field stands fully pale, all facing the
group, and the group has watched (without ever catching it) the same thing happen to a herd that
happened to a colony — and is being asked to let it not happen to them.

**Becoming discipline.** It must never be explained, never announced ("the herd grows" is
forbidden — no step-ladder, no count). The marquee leans on **orientation** (the collective gaze,
the formation) and never on number. The conversion is deniable at every step ("weren't there one
of those?") until it is total and undeniable and still unspoken. No line ties the pale herd to the
Kept; the rhyme is left for the group to feel when the timeline and the field stand side by side.

**FACT-15 seal.** Nothing names the twist. The grey turns pale and faces one way, and the group
infers — when it is far too late to un-see — that they have been watching a taking the whole time.

**Beat wiring.** `SacredAnimalBeat` `mode:"spread"` + `target` + `pale_cosmetic` PDC (distinct
from `sacred_beast`, **never glowing**, capped 16, one-per-pass, unwitnessed, babies not
auto-pale); `DeathListener` ignores `paleCosmeticKey` for conduct (the precision guard — a pale is
never a violation); `paleTarget` deterministic lookup in `snapshot.ts`; the collective-gaze facing
pass (the one new code dep). Reuses `tollSacredBeast`/`keptSacredBeast` (no new voice key). The
glowing Sacred Beast and the Pale field never collide: the fork-arming beast is always the glowing
one, so a group that wants to spare the tracked life can always tell it from the cosmetic herd
(INV-13).

### 2.7 — The apparition wearing an offline player's skin = a keeper's fate, worn by one of you

**What it is.** A rare re-skin of the existing keeper apparitions (§2.1–2.5) — the Watcher, the
Surface-Walker, the Stoop — wearing the **skin of a friend who is logged off**. Reflection / edge
/ crouch-revealed only; no walking, no following, no pathfinding, no chat (all cut). Name-tag is
OFF by default; exactly **one** human-approved, group-witnessed, named glimpse is allowed, at M4
(FACT 9 spoken). It despawns, reveal-disciplined, the moment the worn player rejoins.

**The keeper-enactment (sealed).** This is the carrier of **FACT 9** — "the first hauntings were a
specific keeper's fate re-enacted at the group" — and it closes the LORE-BIBLE TODO-3 gap by
design (§4). Each keeper apparition already *is* a keeper enacting their own fate at the rhyming
player (§2). The offline-skin tightens that one turn further: the fate is enacted **wearing one of
the group's own**, the absent friend. Canon (timeline §5): the going-out took the Kept "family by
family, lamp by lamp," and the ones most easily taken were the ones who had **stopped coming to
the light** — who logged off, in the old sense: stopped keeping their lamp, stopped being witnessed.
The land wears the offline friend because, in the world's grammar, the friend who is not here is
the friend the going-out reaches first. The horror is not that a monster looks like your friend;
it is that the land has begun to **file your absent friend the way it filed the Kept** — wearing
their shape because they are, for now, one of the un-witnessed.

**The precision law (sealed restatement).** The worn skin is the offline player **canonically
rhymed** to the keeper shape, never a callout (INV-16, P2-5). Skin-wearing is **OFFLINE-only**;
the name-carve (`name-where-never-been`) is **ACTIVE-only**; the two may never co-locate the same
player's name and worn skin at the same keeper stone in the same window. The shape worn is the
keeper apparition whose fate the *group* (collectively) is closest to — never a per-player
accusation of the absent friend. A wrong wearing is worse than none: it fires only on a real
offline player the tracker logged as absent.

**Becoming discipline.** The M1 plant (the offline-player report — "brann… not here to see it
noted") reads, at first, as "the record watches you even logged off." Its true meaning (III→IV):
the land had begun to *wear* the friend from the night they stopped coming — the un-witnessed are
the first taken. The first glimpse (M3) is the most deniable: a reflection only, no name-tag, gone
before anyone is sure. The single M4 named glimpse, human-approved, is where FACT 9 is *spoken* by
a keeper NPC tying a logged M1 beat to the keeper who enacted it: *the dread had a biography, and
the body it borrowed was one of yours.* Then it stops — one glimpse, never a recurring jump.

**FACT-15 seal.** The apparition never says what being worn *means*, never says the friend is
"taken," never names induction. It shows a kept keeper's fate, worn by an absent friend, and lets
the group feel the distance between *here, witnessed* and *gone, worn* — which is the whole of the
ways stated in one image, and never in a sentence.

**Beat wiring.** `NamedMobBeat` with `skin_player` + `offline_only` payload + `applyWornSkin`
(cache-first, silhouette fallback if the skin won't load); `PresenceListener.onJoin` (new small
hook — the assumed listener does not exist; `TerritoryListener` is the location source) →
`despawnApparitionsWearing(uuid)`; per-worn-player one-shot budget; the M4 FACT-9 line via the
M4 keeper-NPC dialogue (reads the haunting log, references the *actual* early beat that fired at
that player — grounded, true). Reflection/edge/crouch reveal reuses the §2.1–2.5 apparition
vocabulary; no new monster, no new model.

### 2.6 — The Cold Hearth's tenant = THE SEVENTH, cast out

**Fate (D11, D06, D08).** The Seventh was **not kept** — cast out *before* the threshold, the
looking *unwritten*: "no stone. no name said back. the fire out, and the door shut, and the
way grown over." Theirs is the only shrine where the Kept Light went **fully dark** (the
inverse of FACT 11's one eternal fire). They prove **FACT 10**: acceptance is a choice the
land makes, and it can refuse. *"it kept them. it did not keep the seventh. it has not yet
said which we are."*

**Sealed identity vs. the bestiary.** Critically, the Seventh is **the one keeper who did NOT
become a shape** — because the land refused them. So the "creature" is an **anti-creature**:
an absence where an apparition should be, a cold hearth where a kept fire (and a kept keeper)
should be. If anything is glimpsed, it is *retreating* — a thing leaving, never arriving,
never named. This is the cleanest possible expression of the twist's inverse: the kept become
shapes; the cast-out become *nothing the record will say*.

**Becoming discipline.** The Seventh foreshadows the **cast-out ending** (the group's own
possible fate) and the kept/left binary **without touching induction** (`canon-spine §5`).
It answers "can the land say no?" (yes) and leaves "what does *yes* cost?" for Movement V. The
canon ambiguity with Iss is preserved (distinct, but a sharp group may wonder; never collapse
them before M4).

**FACT-15 seal.** The cold shrine says only that the light was *let* go out, and asks "which
are we." It never says what being kept *is*. The scratched late hand — *"whatever it costs to
be kept — the seventh was spared it. i do not know yet whether that is mercy"* — is the
sharpest the corpus gets, and it is a *question*, not the answer.

**Beat wiring.** `SmallStructureBeat` (footprint-checked ruined shrine, out of LoS), doused
hearth (`TorchGutterBeat`/no-light), and at most a single retreating `NamedMobBeat` with a
very short `despawn_seconds`, seen leaving. Off-path, non-gating; finding it earns Whisper
budget (`FLOW §3`).

### 2.8 — The keeper-enactment law (the offline-skin and the herd, read as one)

The two creatures the task names — the offline-skin apparition (§2.7) and the slow herd
conversion (§2.5b) — are not two unrelated spooks. They are the **two scales of the same
sealed mechanism**, and naming the relationship here keeps either from drifting into a
standalone effect with no narrative home (the orphan check, consistency law).

**The shared law: every apparition is a keeper enacting a fate, never a monster performing a
scare.** §2 established it per-keeper: each shape *is* a prior keeper, worn by the land, doing
again at the rhyming player the thing that took them. The offline-skin and the herd extend that
law in two directions:

- **The herd-conversion is the enactment at the scale of the *settlement*** — the taking shown
  not as one keeper but as the going-out itself (timeline §4 step 5), a colony turning to the
  watching family by family, transposed to grey turning pale head by head. It is the *process*
  that makes every other creature: the verb behind all the nouns. It wears no single keeper
  because it is what *happened to all of them*.
- **The offline-skin is the enactment at the scale of the *one***, and specifically the one who
  is, for now, **un-witnessed** — the friend logged off, who in the world's grammar is the
  friend the going-out reaches first (the un-lit, the un-kept-light). The land wears them because
  the un-witnessed is what the taking is *for*. Where the herd is the process at large, the
  offline-skin is the process arriving at a name the group knows.

**Why they must be read as a pair (the sealed rhyme).** The herd makes "the ordinary world is
turning to watch you" *mundane and total*; the offline-skin makes it *personal and singular*.
Separately, the herd risks reading as ambiance and the skin as a single jump. Together they are
the bestiary's whole thesis in two registers: a taking is happening, at the cadence of a colony
and at the address of a friend, and the group is inside it. Neither is ever explained; the rhyme
is left for the group to feel when the pale field and the worn friend stand in the same dark.

**The enactment discipline both inherit (sealed restatement, no new mechanic):**
1. **Reveal, never witness.** Neither is ever seen *becoming* — the herd is only ever found
   *more pale than it was*, the skin only ever glimpsed (reflection / edge / crouch), never
   walking, never approached as it spawns (`DESIGN §3` reveal discipline).
2. **Grounded enactment only.** The herd's pale count climbs on a deterministic, capped cadence
   (never a measured callout — it is the *process*, group-wide, INV-13); the skin fires only on a
   real offline player the tracker logged as absent, worn as the keeper shape the *group* is
   collectively nearest (never a per-player accusation of the absent friend, INV-16). A wrong
   wearing is worse than none.
3. **Single-arbiter restraint.** Both defer to the conductor's `apparitionClaim` (INV-18): at
   most one ambient figure or turning per drama window. The taking is patient; it does not crowd
   the frame. (The herd's *standing* pale field persists as world-dressing, not as a per-window
   appearing — only its turnings and additions are claimed.)
4. **The seal holds.** Neither names the twist. The herd turns pale and faces one way; the skin
   shows a kept keeper's fate worn by an absent friend. Both let the group infer, far too late to
   un-see, that what they have been watching the whole time was a taking — and never say so in a
   sentence (FACT 15).

**No new wiring.** §2.8 adds no beat, listener, site, flag, or voice key — it is the canon tie
between §2.5b's `SacredAnimalBeat mode:"spread"`/`pale_cosmetic` field and §2.7's `NamedMobBeat
skin_player`/`offline_only` apparition. The enactment law is a reading discipline over mechanics
already specified in `BUILD-MANIFEST §6`, not a mechanism of its own.

---

## 3. THE LIAR (Iss) — why no creature, and his cold re-read

Iss is **dialogue, not a monster** (§1 note). His mechanic is the warm→cold dialogue-state
flip (`canon-spine §4`): met as the warmest of the six (D09, "the ways are a wall"), caught
via the Vigenère key = his own name decoding to "the one who turned away" (D09 answer), then
his whole tree re-reads cold (D10 catch). His rhyme: the player who leaned hardest on Whispers
(the bond tally) — the one who most wanted to be *told the comforting answer*. Collective,
never a callout.

**If a visual is wanted at the catch:** reuse the **same ZNPCsPlus model**, post-catch, now
**silent and turned away** — a dialogue-state-driven swap, not a new creature. The horror is
that the friend you trusted is the same shape, changed. No new model; no telegraph.

---

## 4. FACT 9 — the hauntings had a biography (closing the LORE-BIBLE gap)

`LORE-BIBLE §6 TODO-3` flags **FACT 9** ("the first hauntings were a specific keeper's fate
re-enacted at the group") as the one fact with **no document home** — it must be carried by
M4 dialogue. **The bestiary is the natural carrier, and this closes the gap by design:**

Because each creature **IS** a keeper enacting **their own fate** at the rhyming player (§2),
the Movement-I/II hauntings literally *are* keepers' biographies replayed. The M4 payoff
(via a keeper NPC or the exposed Iss, per `canon-spine §3` F9) is to **name the connection**:
*"the cold that took your light in the first winter — that was Vaun's deep going dark. the
face in your water — that was Sella's far shore. the dread had a biography."* Authoring action
(satisfies TODO-3 option b): **confirm FACT 9 is delivered by the M4 keeper-NPC dialogue that
ties a logged Movement-I beat to the creature/keeper that enacted it.** The plugin already
logs every beat (`EventLogRow`, beat ids); the showrunner can read the haunting log and have
the M4 NPC reference the *actual* early beat that fired at that player — grounded, true, and
the strongest possible FACT-9 reveal. Recommend recording this here as the canonical home for
F9 so the web-rule is satisfied (creature-as-enactment is door 1; the M4 dialogue naming it is
door 2).

---

## 5. OPTIONAL / RESERVED CREATURES (do not build by default)

- **A discrete Mara apparition.** A figure at a lectern, **reading, never looking up**,
  seen only by a player who lingers at the Kept-Light lectern. `NamedMobBeat` at a lectern
  anchor, `no_ai_drift`, never faces the player (inverse of the Watcher's stare). Embodies
  "the map, never the tool" (D05). Skip unless the group wants a sixth literal keeper-shape;
  her diffusion into the watching is more honest.
- **Iss post-catch swap** (§3) — dialogue-state, not a new creature.
- **The herd-en-masse turn** as a marquee — a coordinated gaze of a whole herd at one player,
  paced rarely. Cheap, vanilla, very effective; promote from §2.5 if wanted.

All optional builds inherit the same non-negotiables (§6).

---

## 6. NON-NEGOTIABLES (sealed restatement)

1. **No creature, name, beat, sound, or toast states the induction twist (FACT 15).** The
   creatures *are* the foreshadow; the world flip at the Accepting is the only "reveal," and
   it is felt, not spoken (`canon-spine §3` F15, §6 rule 2).
2. **The rhyme stays soft and collective.** A creature biases toward a player by *measured
   signal* (`hoardedScore`, `distanceFromGroup`, custom `violationRatio`) — never a named
   accusation, never "this is you" (`canon-spine §6` rules 3, 9).
3. **Grounded only.** A creature fires for a behavior the tracker actually measured. A wrong
   apparition is worse than none (anti-jank #6, `canon-spine §6` rule 4).
4. **Warmth under dread.** Every cold beat carries *we would keep you, if you would keep the
   ways* (`canon-spine §6` rule 5). Never gratuitous.
5. **Custom 3D is garnish.** ModelEngine/MythicMobs give the look; the vanilla fallback entity
   carries the beat if the pack fails (`DESIGN.md §3.5`, anti-jank #5).
6. **All `DESIGN.md §3` anti-jank rules hold:** reveal discipline, reversible tolls, idempotent
   persistent beats, the kill-switch.
7. **Keeper voices are inviolable** in any creature dialogue/vision: Vaun only of what he kept,
   Sella only as reflection, Orin only when crouched, Brann only at night, Mara only in
   page-refs, Iss plainly-and-falsely (`canon-spine §6` rule 8).
8. **The Seventh is an absence, not a shape** — the one keeper the land refused, so the one
   keeper that is *not* a creature. Preserve the Iss/Seventh ambiguity until M4 (`canon-spine
   §5`).
