# Director's pass (2026-11-05)

Branch `morrow-v2/storyline`, on top of 0fdbbbe. Four reviewers ran in parallel (plugin hardening +
admin, site/bot hardening + control room, stale-code sweep, puzzle design), plus a writing pass; every
finding was traced in code before it was fixed. Nothing is committed yet.

**Proof on the combined tree:** `canon.verify` all passed · `world.selftest` all passed ·
`gradlew selftest build` all passed · `e2e_site` all site checks passed (preview 3040) · `tsc` clean ·
`discord-v2 run dry` passed (groups of 1 and 10) · `livecheck world` and `livecheck acts` passed on
the Paper test server (the "six figures walking" check is flaky: it counts entities with no player
online, so a figure in an unloaded chunk isn't counted; the rerun passed) · live RCON: `/mrrw help`,
snapshots, and a rollback from act 9 to act 1. A desk simulator (a Python port of `desk.ts` evaluate) routed 26 sample tickets correctly.

## Verdict

It plays end to end with no known way to strand a group, and an operator can now fix anything from
`/backstage` or `/mrrw` without touching files. The two things that can still end the game early are
outside the code: **the repo is public with the answer key** (your call, kept), and
**copperlinehosting.com still serves the old site** until this branch is merged and deployed.

## What changed

**Skips and stalls closed**
- Act 1: the Community nav link went straight to the forum (skipped robots.txt/page source). Removed.
- Act 5: MAINT ON typed for fun in Acts 2-4 jumped the group past Rook's floor. Now reverted at dawn, no advance.
- Act 8: a quiet group might never hear Kit's line. Morrow says it at the first two Act 8 dusks.
- Act 9: the case brief accepted the MD5 printed in Iona's checklist; it now needs Rowan's line. rook.txt
  (dated after Iona left) sat inside the zip whose MD5 she logged; it now sits beside the zip.
- Act 9: lowpath only walked at night, so a group that always sleeps never saw the bed scene. It walks once by day after a skipped night.
- Gates 6->7 and 8->9: desk tokens accept natural wording ("you restarted it at 6:44"); "how do we turn it
  off?" no longer gives away Kit's email; the Marked denial no longer fires on "I marked the spot";
  curly apostrophes from phones match.

**Sessions, restarts, player counts (plugin)**
- Every save file is written atomically; a corrupt file is kept aside, not silently emptied.
- A restart mid-finale finishes the same stop (stop log, `stopped.flag`); Theo's Act 10 ask and the last dawn survive restarts.
- Early desk refs are retried after `/mrrw act`, auto-on, and every advance.
- Ops are never residents (no figure, pot, profile, corpus lines, or quorum seat) unless you add them.
- USB quorum = residents seen in the last 14 days (min 1), override with `/mrrw residents quorum N`. Every count rule works for 1 and for 10 (table in the plugin section of the runbook).
- The plugin warns (startup, status, journal) if `pause-when-empty-seconds` isn't -1.

**Free play (things players could destroy)**
- Protected now: Iona's notes, finch's levers, cairn's room, Kit's hut, the south-rail end, the top of June's tower, and Rook's Nether hub sign (the old check confused Nether coordinates with Mossfield).
- Pistons, water and lava can't move or wash away build blocks.
- Lectern books can't be taken. The Braille shelves are locked. Iona's chest is view-only for non-ops and merges no sooner than an hour after it is first opened.
- The only genuine 2015 map is refilled at dawn. `/mrrw restore signs` puts back any build sign's original text.

**Admin control**
- `/mrrw`: help, journal, flags/flag, repair, restore, desk, handin, auto, residents, snapshot/snapshots/rollback (automatic snapshot before every act move), tp, figures, say. All are tab-complete, journalled, and callable over the bridge.
- `/backstage` is a full control room. It shows act and site fallback (in red), residents, the USB vote and the journal, and has a button for every command, with confirmation on destructive ones.
- Runbook: command table, "When something goes wrong", "When a group is stuck" (gate → which hint to release), and what Morrow protects.

**Immersion (site and bot)**
- No framework error pages. A malformed URL could 500 the whole site; fixed.
- Apache-style 404/401 pages. No env names, no `X-Powered-By`. Dev and test switches are forced off on production.
- Logins last 90 days. A session-secret rotation keeps old links working (`MORROW_SESSION_SECRET_PREVIOUS`).
- Bot: no duplicate posts after a restart, retries when Discord refuses a post, no doubled Act 7 DMs, late joiners get their own schedule.
- Voice notes go out as `voice-message.*` (the fake was literally named `T-09-fake.wav`). A voice player shows only when the recording exists.
- Released hints stay on the desk, dated.
- Players can't see "ARG runtime" via `/version`; `/plugins` and `/version` are op-only.
- 212 dead CSS classes and the unused Supabase packages removed; favicon added.
- Site facts agree everywhere: founded 2008, plan names, Chicago plus leased Dallas/Atlanta nodes, snapshot dated 01 Dec 2014.

**Writing**
- Every hint rewritten as Theo noticing something in his own files: no imperatives, no aphorisms. Act 6 gained the missing "ask me about the log" hint.
- Cut the four-way "It's just Mossfield" refrain and the trailer lines. Theo no longer gives directions.
- Eight new act-keyed desk replies (are you okay, was that you, how do we stop it, Kit, Iona, lowpath in Acts 6 and 8, the bridge), so Acts 7-9 stop getting "Thanks! I'm not sure I have anything".
- T-09a names the cost of telling Theo. The booking reply closes off the restart toggle players would look for.
- Lore fixes:
  - Iona's key line is now a logged `/tell`, not open chat after "not on here".
  - Kit at Theo's on a school night is explained in the IRC log.
  - Rook was seventeen, not sixteen (before SMP existed).
  - Hint 6.4 now says Paint.NET, not a phone.
  - Theo's tell words are Metra/Ogilvie/cereal (all checked absent from Morrow's corpus).
  - Theme words are out of characters' mouths: rook.txt, cairn's post, the invoice note.
- Morrow's two out-of-corpus finale lines are replaced: one picked from the corpus at runtime, the other turned into silence plus a relay click.
- Operator-facing: the trailhead line no longer says "find the key"; the panel no longer says "where Iona wrote its name".
- `RECORDING-SCRIPTS.md` matches every changed line: **record from it**.

## Deferred on purpose (improvements, not breakages; each needs a Beta world re-inject)

- **D13 dust corridor:** cut it (it repeats the console's Act 7 message): `ciphers.LIGHT_CORRIDOR_MESSAGE`/`build_light_corridor`, `layout` `dust_corridor`, `ModernLayer` `install.dust_corridor`, the corridor parts of `Fixtures` (+ its dawn half), the verify/solutions/runbook/TEST-NOW lines.
- **D15 Residents' Key:** reduce to one columnar pass and state the repeated-letter rule on the preface sign. Online solvers default to left-first, so it is solvable today.
- **D11 sound replay:** add a comparator/dust readout beside the subtitles.
- **Kit's hut signs** ("KIT ONLY... no rook" / "pw is my dogs name", `ciphers.py:69-70`) are left as is. Rook knowing the dog's name is plausible.
- **Torch lock:** it brute-forces in about 15 minutes. It is a low-stakes side lock.
- **Optional "never told Theo" ending:** stop in Act 9, the bridge keeps talking for a week. About half a day of work.

## Still yours before friends play (HUMAN-TODO)

- Recordings: all 26 voice notes are now listed in §4; the Mumble night, the voicemail, MIC-5-18-B.
- Merge and deploy this branch; set Vercel prod env; finish the Railway bot env and accept-deploy.
- Server: `pause-when-empty-seconds=-1` (the plugin now warns if it isn't).
- Already-installed worlds keep cairn's old sign until `/mrrw repair listening_room`.
- Not testable without a real client: non-op command hiding, the piston/water/lava/lectern blocks, the daytime lowpath walk, Kit's line at dusk. One 20-minute pass with a second, non-op account covers them.
