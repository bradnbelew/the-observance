---
id: the-companion
title: the one who came to meet you (design brief + artifacts)
kind: character
author: design (with in-fiction artifacts drafted inline)
recipient: —
era: present tense (the group's own descent)
movement: 1–5 (trust I–III · reveal IV · reckoning V)
clue_bearing: false
reveals: []
foreshadows:
  - "the Watcher knows real things because a friend has been feeding it (the Observer's in-fiction channel)"
  - "kept and keeping and being-kept — a fourth face: kept-in-part, paying to stay a person"
  - "the record can be coerced; correcting HIS record is part of correcting the Seventh's"
links_to:
  - the-ways-are-a-wall            # Iss — the historical warm liar; the companion is his present-tense mirror
  - the-seventh-below              # the Seventh's return — the companion steers toward it as his own escape
  - the-name-i-cut-myself          # exile/return — the companion fears being cast out more than being kept
---

# THE COMPANION — the present-tense betrayal (design brief)

> **What this is.** The full character for the trusted companion NPC (manifest A1 / decision D3).
> A DESIGN brief with draft in-fiction lines inline. His dialogue wires into `npcVoice.ts`/`voice.ts`
> at the integration pass; his one FOUND artifact (§6) wires into the seeds as a post-reveal document.
> **Working name: WREN** (adjustable — a small watchful bird; folklore's trickster "king of birds"
> who won by hiding. Reads as a warm peer, fits the colony's short old names without claiming keeper
> rank). If Ethan wants another name, it changes only the string, not the design.

---

## 1. THE ONE-LINE TRUTH (never say it straight in-game)

Wren is a **Kept who was never finished** — caught partway into the Watcher and held there. He stays a
person by **feeding it others**: every name, every plan, every fear he hands over buys him a little more
time with a face and a voice. He found the group, and he has been feeding them to it since the first
night. He tells them — and himself — that he slows them and marks them *to keep them safe from being
taken the way Iss's breaking took everyone*. Some of that is true enough to say out loud. Underneath it
is the thing he won't look at: **they are how he is paying to stay himself**, and he is steering them
toward the Seventh because he believes reaching the record's wound might buy him *out* — and if some of
them are taken on the road down, that is a price he has decided, quietly, he can live with.

He is **Iss's mirror, one turn in.** Iss lied from **pride**, long ago, to bury a crime — and the land
kept the proof. Wren lies from **shame and fear**, right now, in front of you — and *you* are the proof,
if you ever look. The group learns to distrust Iss's warm account of the Seventh through the land. Then
they have to do it again, to their friend, in the present, with no land to do it for them.

## 2. WHY HE CAN WALK AND TALK (the canon check — do not break this)

The Kept are hollowed into the Watcher (WORLD-BIBLE §1). A *finished* Kept could not be a companion.
Wren is the exception the fiction allows: **kept-in-part, arrested mid-taking, and paying to stay that
way.** This is a *fourth* face of the word alongside the three the project already holds
(kept = absorbed/horror · kept = the light "keeps" · kept = recorded-true): **kept-in-part — a person
holding his own edges together with other people's names.** It costs the story nothing and earns the
Observer everything: Wren is the one node of the Watcher that still has a face, *because* he keeps
buying it. When the world quotes your words back, it is because he carried them down.

## 3. VOICE / FINGERPRINT (distinct from Iss and the six)

- **Warm like Iss, but *present* — he asks, he doesn't preach.** Iss's warmth is a written sermon ("the
  ways are a wall"). Wren's is conversational, second-person, full of small kindnesses and questions
  about *you*. (Those questions are also how he harvests — legible only in hindsight.)
- **He under-claims.** Never "I am a keeper," never authority. "I'm just someone who's been down here a
  while." "I don't know much more than you, honestly." (He knows far more.)
- **Present-tense, contraction-heavy, hedged.** "I think—" "maybe don't—" "let's not, tonight." Where the
  keepers' journals are certain and archaic, Wren is uncertain and modern. That grammar *is* the tell:
  the hedging is him steering while seeming to defer.
- **One repeated verbal tic to plant early and pay off:** he says **"stay close"** — as care in Trust,
  as control at the Reveal, and the found record (§6) is headed, in his hand, *"kept close."*

## 4. THE ARC (movements; trust is earned, then it curdles)

- **M1–M2 · TRUST I–II (`companion_introduced` → `companion_trust` rises).** He finds them early —
  first real night. Helps: warns them off a genuinely bad path, hands a small true gift, is the one warm
  voice in a place with none. He asks a lot of gentle questions. He is *never present when the Watcher
  manifests* (he "steps out," "checks the dark," "was just behind you") — because he is the manifestation's
  mouth. Draft line (care): *"stay close. i lost people down here going off alone, and i'm not doing that
  again. tell me where you're headed and i'll tell you what's waiting."* (The last clause is the harvest.)
- **M3 · TRUST III + the first hairline crack.** His steering starts to have a shape: he slows them at
  every threshold ("not tonight — it's a black moon, Brann's night, we wait"), always with a good reason.
  A sharp group notices his warnings are *always* the cautious direction, and that the Watcher's precise
  scares track exactly what they told *him*. Nothing proven. Just a hairline.
- **M4 · THE REVEAL (`companion_revealed`).** Tied to the Iss catch, not a calendar (async-safe): once
  the group has caught Iss's lie — learned that a warm, plausible account can be false and the truth is
  in the record — the same lens turns on Wren. The trigger is *theirs* (they accuse him, or they find the
  record §6, or they set a plan they never speak aloud and watch it not reach the Watcher). He does not
  deny the *what*. He denies the *why* — he insists, to the end, that it was protection. Draft (reveal):
  *"yes. all of it. and i'd do it again, because every time i fed it one of you it took me instead of you
  for a night, and you're all still here, aren't you? still here. i kept you close. i kept you safe."*
  — The lie is in "safe." The truth he won't say is "i kept you *fed to it*, and i kept you *for the road
  down*, because you're my way out."
- **M5 · THE RECKONING (`reckoning_condemn | understand | free`).** The group decides what the record
  says of him — this is a *correct-the-record* act, the same verb as the Seventh's whole quest, turned on
  a living man whose motives are genuinely mixed. Three branches (§5).

## 5. THE THREE RECKONING BRANCHES (feed the finale; dynamic-roster, group-scoped)

Each is a line the group chooses to enter into the record about Wren. All are *hard*, because the truth
is ambiguous — he was both scared and self-serving, and he will not clarify it for them.

- **CONDEMN (`reckoning_condemn`).** They write him as what his acts were: a man who traded them to save
  himself. The record takes him — fully, finally; the face goes out. *Warning built into the beat:* to
  cast him out for what he did is to do to him exactly what was done to the Seventh (the-name-i-cut-
  myself). The world notes the rhyme. Clean justice, and it costs them a piece of the moral high ground
  they came down to claim. Draft (his last, taken): *"that's — fair. write it. at least it's true. stay
  cl—"* (cut off; the record closes over the word).
- **UNDERSTAND (`reckoning_understand`).** They write him whole — scared *and* selfish, protective *and*
  self-serving, all of it, uncollapsed. He is not freed and not cast out; he stays kept-in-part, a face
  in the dark that will not resolve to hero or villain. The hardest to write and the truest. This is the
  branch that best rhymes with the Seventh's thesis (*"the record will have you wrong; i will have you
  true"*) — mercy as *accuracy*, not absolution.
- **FREE (`reckoning_free`).** They find they can correct *his* record too — the tally he kept was itself
  coerced (a Kept feeds or dissolves), so it is not fully his crime. Releasing him from the deal ends his
  feeding — which means it ends *him* as a held-together person, but on his own terms, unfed, let go
  rather than taken. It **costs the group** (a real price at the finale — a bargain the Seventh names).
  Draft (freed): *"oh. you're not going to keep me either. neither one. you're just going to — let go.
  i forgot that was a thing you could do to a person. thank you. i'm sorry. i wasn't only lying."*

## 6. THE FOUND ARTIFACT — "kept close" (the post-reveal gut-punch)

The one in-fiction document Wren authors. The group **cannot** find it during Trust (he carries it).
It surfaces **only post-`companion_revealed`** (dropped at the reveal, or hidden at a site the reveal
unlocks). It is a **tally in his own hand** — every one of *their* real names, plans, fears, inside
jokes, dates, the things they said in the dark — the record he kept *for the Watcher*, so it would take
him slow. Seeing their own true words in his handwriting is the proof no accusation could be. It is
headed, in the same warm hand as every kindness he ever did them:

> ` kept close `
>
> [ a tally-book, soft with handling, the hand the same one that drew you the safe path on night one.
> it is not a journal. it is an inventory. of you. every page dated. the last entries are today. ]
>
> *— he wrote down the thing you said you'd never do. he wrote down where you said you'd go if it ever
> got bad. he wrote the name you only use for each other. he kept you close. this is what close was.*

**Design note:** this artifact is where the Observer Engine's grounded observations become *diegetic
proof* — the sharp quotes the world threw at you were harvested *here*. It retroactively explains the
whole "it knows your name" north star and makes the Observer's precision feel earned, not magic (D6).

## 7. WIRING NOTES (for the integration pass — every touchpoint)

- **Observer / two-register (T1):** Wren is the in-fiction channel for the **sharp, quoted** scares
  (Tier 1/2). The **ambient** "it noticed you" (Tier 0, behavior) stays the *land itself* — needs no
  Wren. **Post-reckoning the sharp quotes must change** (condemn/free → they go quiet or shift source;
  understand → they persist but read differently). Sequence: harvest ramps with `companion_trust`.
- **Iss differentiation (must hold everywhere both appear):** pride/past/buried-crime (Iss) vs
  shame-fear/present/you-are-the-proof (Wren). The M4 reveal explicitly *reuses the Iss-catch lens* —
  that reuse is the point, not a repeat.
- **Relief split (T10):** Wren is a *source* of Warm-Grief relief that **curdles** at the reveal; keeper
  memories carry the *untainted* relief that survives the reckoning. Do not route all warmth through him.
- **Dynamic roster / async (invariants):** one group-scoped NPC; trust is a group flag; late joiners get
  a Wren line ("a new hand — good; stay close, all of you"). The reveal is one group event, quorum-free.
- **NPC framework (D4):** Wren is Citizens2 (must pass as a real person). His "never present at a
  manifestation" tell requires the beat system to despawn/relocate him during Watcher beats — a listener.
- **Reckoning → finale:** the three flags branch the ending; the FREE branch's cost is named by the
  Seventh at the reunion (dovetails L1/L6). His record (§6) may be one of the testimonies the group can
  carry down to correct.
- **Seeds/flags:** `companion_introduced`, `companion_trust` (int), `companion_tells_seeded`,
  `companion_revealed`, `reckoning_condemn|understand|free`. Gate the reveal behind `iss_caught`.
- **Cohesion gate:** nothing here is a fake puzzle — Wren is character + honest reactive drama; his one
  artifact is forensic proof, not a decode. Passes §4 cohesion.
