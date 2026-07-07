# SESSION ZERO — the out-of-fiction consent + onboarding (read to the group BEFORE anything starts)

> v2 canon. Required by the grounding+consent invariant (OVERHAUL §4). For a veteran friend group,
> "being watched / surveilled / isolated" can land harder than intended — so the **existence** of the
> personalized surprise is disclosed up front, out of fiction, and opt-out is real and easy. Disclosing
> that "something will watch and seem to know you" does NOT spoil it; the dread survives knowing. (Cicada/
> care-ethics: disclose the existence, never the mechanism.)

## The message (say this plainly, as yourself — not in the Watcher's voice)

> Hey — before we start this thing I built: a heads-up, because I want everyone in by choice.
>
> For the next few weeks there's going to be *something* on the server. It watches. It's meant to feel
> like it knows you — your habits, where you go, things you say. That's the point of it, and it's going
> to be unsettling on purpose. Some of what it "knows" it gets from watching how you play, from this
> Discord, and — if you opt in — from voice chat. It only ever uses **real** things; it doesn't make
> stuff up about you.
>
> Three things:
> 1. **You can opt out or dial it down any time**, no questions, no explaining. Just tell me "dial it
>    back" and the personal stuff goes quiet for you. You stay in the story; it just stops getting
>    personal.
> 2. **Voice tracking is opt-in.** If you'd rather it not listen in VC, say so now or whenever — totally
>    fine, the game works without it.
> 3. It's a horror mystery. It's meant to be tense, not actually distressing. If anything lands wrong,
>    tell me and I'll fix it. When it's over I'll walk you through what was real and what was the game.
>
> That's it. If you're in, don't overthink it — just play, keep an eye out, and let it get under your
> skin. Cool? Then forget I said any of this.

## The handoff into fiction (right after — no more out-of-fiction talk)
- One person gets **the cursed map** (the single-player download). They play the ~15-min vignette; it
  ends on the number **6**.
- They bring the group onto the server. The hook: **the server says the 6 back** — a thing a map cannot
  know. From here, stay in fiction; everything is the world and the record.

## The debrief (mandatory, at the end — out of fiction again)
- Walk through: what was authored vs. what was real observation; that nothing about them was invented;
  the Seventh; what the ending meant. Give the experience a clean door to close. Especially for this
  group, end *with* them, not on a cliff.

## Capture switch rule (operator, before players join)
- Do not enable `observer_capture` until this script has been read and every player's opt-out choice is
  recorded.
- Keep `players.observer_opt_out = true` for anyone who opts out of personal capture, is absent from
  session zero, or has an unclear consent state. Only explicitly clear it to `false` after disclosure.
- Do not enable `voice_capture` unless you have handled voice consent with the group beforehand.
- Record the final `observer_capture`, `voice_capture`, and any `observer_opt_out` state in the rehearsal
  packet's `launch-attestations.md`.

## Practical setup checklist
- [ ] Server is Paper 1.21.x (pinned), plugin + datapack installed, resource-pack auto-push on
      (`force=true`); confirm each friend accepts the pack (decline → degraded clues, see them re: the mirror).
- [ ] The bot/showrunner is hosted and running; Supabase reachable; `0006` applied + seeds re-run.
- [ ] The record website is reachable at its URL (the discover-by-URL hook).
- [ ] Each player has `/link`-ed (or is auto-linked) so the record knows their name.
- [ ] Confirm who opted in/out of observer personalization and voice; set the DB flags and capture switches
      above before the first session.
