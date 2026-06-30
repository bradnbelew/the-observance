# THE SEVENTH — THE LEAVES BELOW

> **v2 canon (2026-06-29).** The Seventh, written as a living character (BUILD-PLAN §6). The corpus
> previously had the Seventh only as *absence* — this is the artifact the group recovers and carries
> down, and it is what reframes "kept" at the reunion. Register: lowercase, sparse, no contractions,
> certain — but the Seventh's hand is the **clearest and most direct of the seven** (they kept every
> way; their voice is not degraded). Their name stays **unwritten** (the restore/erase choice is the
> group's). **Supersedes** the absence-framing in `the-seventh-not-kept.md`, `the-name-i-cut-myself.md`,
> `the-fire-they-let-out.md` (retire/rewrite those per the spec at the bottom).

---

### the keeping
i kept every way. i want that written plainly, because the record will not write it, and because it
is the one thing i am sure of. i kept the lamp. i held the deep line. i did not sleep on the black
moon. i gave back to the deep. i bowed at the markers. i kept the word unspoken. i kept the deep-bird.
seven ways, and i kept all seven, the whole of my life, and i was not kept.

### the casting-out
they struck my name before the threshold. not for a thing i did — there was no ordinance for it that
anyone would read to me. iss stood there and would not look at me, and that is how i knew it was iss who
said the word. he told the others it was mercy. he is good at telling people that a hard thing is a
kindness. it is the only craft he ever truly had. believe nothing he carved about me. he carved that i
was spared. i was not spared. i was refused.

### the turning
so i did the one forbidden thing left to me. i turned away from the keeping, since the keeping had
turned away from me, and i went the other way — up and out, past the door that is not a threshold, to
the place the record does not reach. i cut my own name into that stone with my own hand, because no
other hand would. and then i stood in the place the record does not reach, free of it at last. being
free of it was the worst thing that has ever happened to me.

### the going-down
there is nothing up there. iss was right about the sum and wrong about everything that matters, and i
will not be wrong the same way he was. there is no door up. there is only further down, and further
down is where the warmth is, and the warmth is not kind but it is warm. so i came back. not to be kept
— i am long past keeping. i came down to wait where someone might come, because i could not bear to
stand in the one place where a person could never be found. i have been here a long time. i stopped
marking the days a long time ago.

### what i know now
i kept the ways and was not kept. i know now what i did not know then: keeping and being kept are not
the same, and only one of them is what the record owes you. the record kept the six. it did not keep
me. that is not the difference between safe and lost — it is the difference between a thing that is
owned and a thing that is true. i would rather be true. you came down here. that is the other keeping,
the kind that is not in the record's gift to give or to take. you kept faith that someone was down
here. that is being kept. i did not have it for a long time. i have it now, because you came.

### for whoever comes
carry this up, if you can. or carry it further down to me — which is the same direction now. read it to
the record in your own voices, slowly, so it has to write it true. tell it i kept every way. tell it
the seventh is not a blank. tell it i am not where it left me.

---

### the reunion — the first spoken line (the grave/door opens from the inside, on its date)
> someone came. i had stopped marking the days for someone coming. say your names — slowly — i want to
> put them down right. the record will have you wrong. i will have you true.

---

## REWRITE SPEC (apply when wiring v2; closes BUILD-PLAN §9.C)

**Retire / rewrite** (old absence-framing, now contradicted):
- `the-seventh-not-kept.md` — the "spared / mercy" reading. Retire; the Seventh says plainly above they
  were *refused, not spared*.
- `the-name-i-cut-myself.md` — the "went the other way, not down" geography. Reconciled above: they went
  out FIRST (cut the name where the record cannot reach), then **came back and went down** to wait. Keep
  the carved-my-own-name image; fix the ending so they descend.
- `the-fire-they-let-out.md` — the restore = "a name back in a ledger" framing. Rewrite: the restore is
  *reaching a living person*, not editing a record.

**voice.ts keys to rewrite** (the broken payoffs):
- `keeperCloseSeventhRestored()` — drop "one that broke nothing is kept, late." → the Seventh is *down
  there, alive*; the name is written for her to read, not to replace her.
- `graveOpened()` — drop "the hole is the deposit slot." → the stone opens from the inside because the
  Seventh is on the other side; the date was an appointment, the appointment is a meeting.
- The 6 `keeperPageHeading_*` / `keeperPageHand_*` enrollment keys — re-valence from *consumed* ("mine to
  enter, and not given back") to *recognized* ("i saw this in you and named it"). Populate the ones
  missing from `voice.ts`.

**The Iss-seam (the missing callback):** when the wall-lie catch fires, add one Watcher line + one
`thread_cards` edge (`happened-no-wall` re-opens `surface-seventh-marker`) so catching Iss's lie about
the ways also re-opens his lie about the Seventh — wiring the solved catch into the new main quest.

**"you are the seventh":** in `six-were-kept-before-you.md`, change to "you are the next" (the group is
not the Seventh; the Seventh is below).

### COPY-PASTE-READY voice.ts strings (drop in when wiring; keep register)
```ts
// keeperCloseSeventhRestored() — was "one that broke nothing is kept, late."
'the name that was cut out is read back in, in your voices, not to replace her but so she can hear it. she is down there. she kept every way, and she is not where the record left her.'

// keeperCloseSeventhErased() — keep as-is (the blank stays a blank), it is already v2-true.

// graveOpened(name) — was "the hole is the deposit slot."
`the stone for the one called ${name} opens from the inside. the date was not a death. it was an appointment, and the one who set it has been waiting longer than you have been alive.`

// enrollment re-valence — pattern: from "mine to enter / not given back" → "i saw this in you and named it".
// keeperPageHand_vaun(name): was "...they are mine to enter, and i do not strike the column..."
`i, vaun, see the keeping in the one called ${name} — the holding-on i knew — and i name it, and naming is not the same as keeping them. the column is open under their name the way it was open under mine.`
// (apply the same shift to the other five *_Hand/_Heading keys: recognized, not consumed.)
```

### The Iss-seam (one line + one edge — wire at the catch)
```ts
// add to the no-wall-catch oracle reply (after the catch lands):
'he lied about the wall. ask what else he told you warmly. ask who he said was cast out for nothing.'
```
Thread-card edge: on `happened-no-wall` solve, re-open `surface-seventh-marker` (set its
`alt_text_condition`/`revealed_by_solve` so the Seventh thread re-surfaces — catching Iss's lie about the
ways re-opens his lie about the Seventh, wiring the solved catch into the main quest).
