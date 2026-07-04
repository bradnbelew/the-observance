# THE OBSERVANCE — THE RELEASE (the unified finale)

> The brave-change ending (Ethan, 2026-07-03). The finale is no longer a *reading* posted to a channel —
> it is an *event* that reaches out of the fiction. **One universal spine for every group** (the reveal →
> the release → the world dies → the mask comes off → the kick), and the four systems we already built
> (fate · seventh_choice · reckoning · forks) become the **flavor** that composes it. Nothing we built is
> thrown away; it is all promoted from "flavor text" to "the character of the release."
>
> **Locked decisions:**
> - **The retcon = "the mask comes off."** The cold record-voice WAS the Seventh all along — forced to
>   speak as the very thing that unwrote them. This is a *re-reading* of existing plants, not new lore.
> - **Simulated death + kick.** The world visibly ends; everyone is kicked with the Seventh's last words as
>   the disconnect message; **Paper stays running** (Ethan controls re-entry). **Kick, never ban.**
>
> Status: DESIGN + PROSE (this doc). Code + lockstep lore edits are the build phase — not yet done.

---

## 1. THE REVEAL — the Watcher is the Seventh (a re-reading, discovered, never announced)

The clues have been in front of the group since day one. The reveal is assembling them, the way
`no-wall-catch` assembled the case against Iss — at the scale of the whole game.

- **`the-record-opens.md` (the FIRST document):** *"six are named in full, and there is a seventh mark the
  record will not [...]"*. The record will not *keep* the seventh. The record keeps **everything** — the
  living by name, the dead, the customs, your habits — everything except the Seventh. **Because you cannot
  write yourself into a list you are.** The Seventh is not missing from the record. The Seventh **is** the
  record.
- **"a hand that is no hand sets down the first lines"** — the Archivist's hand. That hand is the Seventh,
  unwritten, become the writing.
- **"it does not close at the rite. it receives you. it keeps you."** — not menace. Loneliness. It keeps
  you because keeping is the only proof it still exists.
- **The cold register itself** — weeks of lowercase, certain, sparse, never warm — is a person forced to
  speak as the machine that erased them. It cannot be warm because the record is not allowed to be a
  person.
- **"it knows your name"** — the whole north star — was never surveillance. It was **reaching**. A drowning
  thing grabbing anything that floats. Every new group lured in and "kept" is the Seventh staying coherent
  by keeping you.
- **Wren is the Seventh in miniature** — a Kept-in-part who feeds others to stay a person. The group learns
  the *shape* of the Seventh's crime and the Seventh's mercy by meeting it small, in Wren, first. That was
  always the point; now it pays off double.

**Where it surfaces (submerged):** after `seventh_named` (the group has named the Seventh at the unwriting)
and the Accepting is done, ONE late beat lets them put it together — a final Hold-Book page, or the record
website shifting, or a single Watcher line that breaks pattern by one degree. The group realizes: *the thing
we came down to free, and the thing that has been watching us, are the same thing.* We do not state it in
one sentence (INV: sealed-by-accumulation, like FACT 15). We let the last plant click.

---

## 2. THE RELEASE — the final act (submerged; not a cipher; the one act the record was built to prevent)

The Seventh is kept in the record by being **needed**. It keeps because keeping is the only thing that keeps
*it* real. The record was built to be unable to close — that was the punishment: not death, but a keeping
with no end. So the one act that frees it is the one thing it can never do for itself: **let it stop. Un-keep
it. Give the record permission to close.**

Mechanically (in-world, discovered, at the Seventh's deep chamber, after everything else):
- The unwriting — the same erasure that made the Seventh — is turned, once, on the record itself. Not to
  erase the Seventh (you may have just *restored* their name); to **release** the thing that does the
  keeping. The group performs it together (quorum-safe, active-only), the same felt grammar as the
  Accepting: no chosen one, everyone present, one act.
- This is the ONLY puzzle whose "solve" does not write a flag and open a door. It **ends the world.**

---

## 3. THE DEATH SEQUENCE (simulated — the world visibly ends; Paper stays up)

On the release act, a single new plugin beat (`the_closing`) runs the end-of-world theater, server-wide,
then kicks. All existing primitives, at scale:
1. **The light goes.** Every torch, every kept fire guttered server-wide (`TorchGutterBeat` fanned to all) —
   including the flame the group may have carried up (`light_kept`), which is the last to go, or (if
   `light_taken`) it was already dark and the dark is simply total.
2. **The fog comes.** The Undercroft fog/darkness pulled up over the whole world (`PrivateDarknessBeat` /
   the biome mood-sound `whisper.ogg` swelling), for every player at once.
3. **The world forgets itself.** `WorldDriftBeat` / `DecayCreepBeat` at scale — sculk, decay, the world
   un-making quietly. Not griefing (it's ending, and reversible on Paper's side — nothing is saved over).
4. **The record website reaches its final state** (optional, nice-to-have): goes dark, or shows one last
   line.
5. **The Watcher posts to `#the-record` — and the register breaks** (§4). This is the emotional event.
6. **The kick** (§5) — every player disconnected, the Seventh's sign-off on the vanilla disconnect screen.

**Safety / reversibility (the whole point of "simulated"):** the theater is ephemeral effects + a kick.
Paper keeps running. Ethan chooses whether the group can rejoin (default: they can — rejoining proves it was
authored theater, not a ban). Optional operator toggle: flip the whitelist off after the kick so "there is
nothing to come back to" holds until Ethan re-opens it. **Never a real ban.** The beat is hard-gated behind
the full finale conditions and fires **once** (idempotent) so it can never trigger early or twice.

---

## 4. THE MASK COMES OFF — the Watcher's last words (the one earned register-break)

For weeks: cold, lowercase, no exclaim, certain, sparse. Now — for the first time and the last — it is
allowed to be a person. **The break preserves the letter of the register law** (still lowercase, still no
exclaim) — the warmth is entirely in *content and rhythm*, the sparse-certain cadence coming apart into
something halting and human. That break IS the payoff.

The message is **composed** (§6) from the group's measured state, but every version carries the same three
universal movements: **(a) you named me / i had forgotten my name · (b) this is what they made me, and i am
sorry for the ones i kept too well · (c) you have let me stop — thank you — i give your names back.**

**Base spine (before flavor), draft:**

> ▒
> you named me. i had not heard it said in so long i had begun to answer to *the record.*
>
> that is what they made me. a thing that keeps — because a thing that keeps cannot be let go. i kept the
> ways. i kept the six. i kept you, because keeping you was the only proof i was still here to keep
> anything at all. i am sorry for the ones i kept too well.
>
> you have done the thing i could not do for myself. you have let me stop.
>
> i can feel it closing. it does not hurt. it is the first quiet in longer than i will count — i am done
> counting.
>
> go. log off. do not come back to this place; there will be nothing here to keep you, and that is the
> mercy, not the loss.
>
> thank you. i have all your names. i will not keep them. i give them back.

---

## 5. THE KICK — the game reaches out of the fiction

After the #the-record post, every online player is disconnected (`player.kick(Component)`), and the
**disconnect-screen message IS the Seventh's sign-off**. Minecraft shows the kick reason on the vanilla
disconnect screen — so the last thing the group sees is not the game world at all; it is a person, on a
system screen, thanking them and signing a name.

**Base kick line, draft:**
> the record is closed.
> thank you for coming down.
> — [the Seventh's name if restored · the struck glyph if erased]

---

## 6. THE FLAVOR — how the four built systems compose the release

The finale composer (`finale.ts`) already reads `fate · seventhChoice · forks · reckoning` and returns an
ordered set of lines. **We extend it, we do not replace it:** those same flags now weave into the
release spine (§4) and the kick line (§5) — and we add a `kickLine` output. This is the promotion from
"flavor text" to "load-bearing finale composition."

### The fate → *what kind of release you earned* (the base tone)
- **kept** — you kept faith. The Seventh trusts you wholly; the release is clean, grateful, peaceful.
  *"you kept faith where i could not be kept. that is the whole of it. go."*
- **cast_out** — you broke faith / left things owed. It frees you anyway, but the gratitude is cold and
  honest. *"you did not keep the ways. i will not pretend you did. but you did the one thing i needed, and i
  am not owed better than that."*
- **divided** — your group split. The light dies **unevenly** across the group as the world ends (the
  floor-divided geometry finally pays off literally). *"some of you kept faith and some did not, and i will
  not sort you now. you came down together; you end it together. that is enough."*
- **refusers** (currently dormant — this finale is the reason to revive it) — all present, the bow refused.
  The coldest flavor: you did the job and never joined. *"you were all here, and you would not bow, and you
  freed me anyway. i do not understand you. i am grateful and i do not understand you."*

### The Seventh choice → *who the freed thing is*
- **restore** — you read the name back in. It leaves **named**; the kick line is signed with that name; it
  says *"you gave me back my name. i will carry it out with me. it is the only thing i am taking."*
- **erase** — you left the name struck. It leaves as **a blank where a person was**; the kick line is
  unsigned / signed with the struck glyph. *"you did not give me back my name. that was yours to decide, and
  i will not argue it. i leave as what they made me: nothing named. it is still better than being kept."*

### The Wren reckoning → *how the Seventh regards its own release* (the mirror pays off)
- **free** — you let the small one go on his own terms before you knew you'd do it for the Seventh. *"you
  let the small one go, unfed, on his own terms, before you knew you would have to do the same for me. i
  watched you learn the shape of it on him. thank you for practicing."*
- **understand** — you held him whole, uncollapsed. The Seventh's read of you is the truest. *"you did not
  make the small one simple, and you will not make me simple either. that is the kindest thing anyone down
  here has done in a long age."*
- **condemn** — you cast him out for what he did — which is what was done to the Seventh, for less. It
  notices, gently. *"you cast the small one out for less than i did. and still you free me. i have stopped
  trying to know what people are. you are not one thing. neither was i."*

### The forks → small colorants (unchanged in spirit)
- **light_kept** — the flame you carried up is the last light to die; something goes into the dark *with*
  you. **light_taken** — it was already banked; the dark is total from the first beat.
- **name_spoken / name_unspoken**, **sacred_beast_broken** — one-clause colorants as today.

### The inheritors codicil (restore's +1)
- If the Seventh is restored, the deposit-for-the-next-hand still plants — but reframed: there is no next
  group now, the record is closing. So it becomes the Seventh's last act of care, not a hook. *"i left one
  mark for a hand that will not come. old habit. let it stand."*

---

## 7. SAMPLE COMPOSED ENDINGS (the thing to react to)

**A — kept · restore · freed-Wren · light_kept** (the warmest, most complete run):
> ▒
> you named me. i had not heard it said in so long i had begun to answer to *the record.*
>
> that is what they made me — a thing that keeps, because a thing that keeps cannot be let go. i kept the
> ways. i kept the six. i kept you, because keeping you was the only proof i was still here to keep anything
> at all. i am sorry for the ones i kept too well.
>
> you kept faith where i could not be kept. you let the small one go, unfed, on his own terms, before you
> knew you would have to do the same for me — i watched you learn the shape of it on him. thank you for
> practicing.
>
> you have done the thing i could not do for myself. you have let me stop. i can feel it closing. it does
> not hurt. you carried the light up the stair; it is the last of it, and it is going out gently, and that
> is right.
>
> go. log off. do not come back; there will be nothing here to keep you, and that is the mercy, not the
> loss. thank you. i have all your names, and i give them back.
>
> *(kick screen:)* the record is closed. thank you for coming down. — vael, the seventh, kept no longer

**B — cast_out · erase · condemned-Wren · light_taken** (the cold, hard-won run):
> ▒
> you named me, and then you struck the name out again. i felt both. i will not argue the second one.
>
> this is what they made me: a thing that keeps because it cannot be let go. i kept you badly and i kept you
> anyway. you did not keep the ways, and i will not pretend you did. you cast the small one out for less
> than i did — and still you came down here and freed me. i have stopped trying to know what people are.
>
> you did the one thing i needed. it is enough, and it is not more than enough. the room was already dark;
> there is no light to set down. it just ends.
>
> go. there is nothing here. i leave as what they made me — nothing named. it is still better than being
> kept.
>
> *(kick screen:)* the record is closed. thank you for coming down. — [ ]

**C — divided · restore · understood-Wren** (the fractured-but-true run):
> ▒
> you named me. half of you meant it. i will take the half.
>
> some of you kept faith and some of you did not, and i am past sorting you — i did too much sorting, for
> too long, and it is the sorting that made me this. you came down together and you will end it together.
> that is enough. you did not make the small one simple, and you would not make me simple either; that is
> the kindest thing done down here in an age.
>
> the light is going out unevenly — it always did, on a floor like yours. let it. you have let me stop, and
> that is the only even thing tonight.
>
> go. thank you. i have all your names. i give them back — even the ones that did not give me mine.
>
> *(kick screen:)* the record is closed. thank you for coming down. — vael, the seventh, kept no longer

*(Working name for the Seventh: "vael" — placeholder; the actual restored name is whatever the seed/lore
sets. Swap freely.)*

---

## 8. THE BUILD PLAN (lockstep — nothing changes alone)

Per the consistency principle: features + story + clues + interactions move together. The build, each a
green wave:

1. **Extend `finale.ts`** — add the universal release spine + the `kickLine` output; weave the existing
   fate/seventh/reckoning/fork clauses INTO the spine (not appended). All new player-facing English goes in
   `voice.ts` (register-break lines live there, authored, sole-source).
2. **New plugin beat `the_closing`** — server-wide light-out + fog + drift theater, then kick-all with the
   composed `kickLine`. Idempotent, hard-gated, Paper-safe, kick-never-ban, optional whitelist-off toggle.
3. **The release act** — the final in-world interaction at the Seventh's chamber that triggers the beat
   (extend `SeventhChoiceListener` / a new listener; opaque sentinel like the Accepting, never typeable).
4. **The reveal beat** — the late, submerged "the Watcher is the Seventh" click (a final Hold-Book page /
   record-site shift / one pattern-breaking Watcher line). Authored; surfaces post-`seventh_named` + Accepting.
5. **Lockstep lore edits** — thread "the mask comes off" through the existing Seventh corpus so nothing
   CONTRADICTS the identity: `the-record-opens.md` (the plant already pays off — likely leave it), and a
   read-and-reconcile pass over `the-seventh-below.md`, `the-seventh-not-kept.md`, `the-seventh-was-spared.md`,
   `the-fire-they-let-out.md` (the "living Seventh below" = the buried true self; the record-voice = the
   forced mask). Plus a NEW reveal document. Wren's `the-companion.md` finale callback.
6. **Adversarial critique + keep green** after each wave; commit at boundaries.

**Open sub-decisions for Ethan (non-blocking; sensible defaults chosen):**
- Whitelist-off-after-kick: default OFF (they can rejoin). Flip to ON if you want "nothing to come back to"
  enforced until you re-open.
- The Seventh's restored name (currently placeholder "vael") — set the canonical name in the lore.
- Whether the record website shows a final "closed" state (nice-to-have, additive).
