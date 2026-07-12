# THE OBSERVANCE - EXPERIENCE MANIFEST

> Director source of truth, created 2026-07-05. This file describes the player-facing ARG as an
> experience, not as an implementation ledger. When docs, seeds, routes, or comments disagree, use
> this manifest as the editorial map, then update the underlying source to match.

## Director Standard

The Observance is a haunted Minecraft world that pulls players into investigation. It is not a
linear puzzle hallway. A good player experience has four layers running at once:

1. the world notices the group before it explains itself
2. the surface people give human, contradictory accounts
3. the record and archive preserve evidence outside the world
4. the keeper/finale spine turns scattered evidence into a name

Player-facing writing should be certain. Do not use "should," "would," or "accepted answers" in
canonical playthrough text unless the beat is genuinely not built. Production uncertainty belongs in
status notes, not inside John's experience.

## Production Status Terms

- **live** - implemented in code/content and expected in the launch run once ops are applied.
- **placement-required** - built, but needs in-world placement or operator staging.
- **media-required** - canonical planned media; play it as finished in editorial review, but do not plant
  its live lure until the file/route exists.
- **ops-required** - requires deployment, config, migrations, hosting, credentials, or server setup.
- **deferred** - intentionally not part of launch.
- **drift-risk** - current files contain duplicate/stale/spec-only truth that must be reconciled.

## Runtime Ownership

- **Minecraft plugin:** observes real player behavior, detects physical acts, performs in-world theater,
  and posts opaque solves/events. It does not own story canon.
- **Supabase:** stores state, flags, solves, event log rows, archive rows, and content keys. It is the
  shared ledger.
- **Showrunner:** decides when authored content appears. It may select from authored lines, but launch
  canon does not depend on live improvisation.
- **Website and Discord:** display the record, archive, whispers, cards, off-world media, and social
  evidence.

The operator model is: the plugin watches, Supabase remembers, the showrunner answers, and the website
shows the record.

## Canonical Player Path

### 0. Session Zero / Cold Ignition

Status: **placement-required**, **ops-required**

John begins in an ordinary Minecraft survival context. The first strange event is not a formal puzzle. It
is an anomaly in or near the group's own base: a cursed map-frame, a lit marker, or a base journal that
knows a real number it should not know.

What John learns:

- the world is watching before it teaches
- the record reacts to real play, not only submitted answers
- the first hook is a screenshot-worthy wrongness, not an instruction plaque

Required launch prep:

- stage with `/observance placeprologue`
- resource pack hosted before rune-heavy beats are expected
- first anomaly must use a grounded real value, never an invented accusation

### 1. Surface People Before Stones

Status: **live**, **placement-required**

John meets the five surface NPCs before the keeper field dominates the experience.

**Aro** is warm, specific, and wrong. He sells the lie that the painted line is safe, the deep may be nice,
and the old warnings are local superstition.

**Wenna** half-remembers the ways as folk charm. She gives John the soft version of real rules: keep a lamp,
leave a little, do not say the cold's name, stay up on the black moon.

**Coll** cares about trade. He is not mystical, but his business records tell the truth: people who cross
the line stop buying oil.

**Dob** descends with the group. His fear is human and useful. As the world gets stranger, Dob gets quieter.

**Old Pell** is the surface mirror of the Watcher. He remembers conduct, refuses to hand over his deepest
memory, and states the horror plainly: it does not chase; it waits and watches.

What John learns:

- surface testimony is contradictory but not disposable
- Aro's confidence is a trap
- Wenna's folk habits are corrupted survival knowledge
- Coll and Pell give fair warnings
- Dob makes the descent feel social, not empty

Playthrough requirement:

- include first conversations, conduct-colored returns, and at least one NPC quest/reaction before the
  main puzzle spine becomes dominant

### 2. The World Watches

Status: **live**

John receives small, sparse hauntings governed by the drama budget:

- private lines that name measured habits
- a book or page changing when unwitnessed
- a name appearing where it should not
- a torch gutter or warmth loss after a broken way
- a pale/cosmetic drift around already-found sites
- a map mark or world drift that rewards return visits

What John learns:

- the Watcher is precise only when the world has evidence
- silence is part of the experience
- the horror comes from being measured, not spammed

Director rule:

- no haunting may accuse beyond evidence
- no haunting should explain the system
- every scare must either deepen mood, confirm conduct, or point attention back to a real clue

### 3. Rosetta Literacy

Status: **live**, **resource-pack ops-required**

John learns the old script by walking the world. The marks are placed beside what they mean. The teaching
line is:

> read the ring sunwise from the topmost mark.

John reads the ring clockwise from the top and gets:

> bow offering kept light deep line unspoken sacred beast

What John learns:

- runes are readable language, not decoration
- the ways are seven
- the resource pack matters
- future old-script lines are fair game

Drift risk:

- older wording in `learn-them-as-we-learned-them.md` references stale symbol names. Current implemented
  canon is the sequence above.

### 4. The Old Listing And The Record Elsewhere

Status: **live**, **media-required for downstream lure**

John finds the damaged founder note beside the first report. It states:

> the record is kept in more than one place, against the loss of the first.

It warns:

> do not look for the elsewhere in the world. it is not a shrine you can walk to.

It tells him the way is keyed below, in the script, and is not a bearing or coordinate.

John decodes the old-script line with the Rosetta literacy:

> the-record-keeps

The opening Hold copy also teaches a blunter web door:

> front door: SNOIKERZ
> ending: common web
> path: /

John reaches the abandoned listing first. It looks like an old server/map host,
not a clean puzzle menu: the Hold download is there, the Record routes are there,
and the live Paper address appears there only once the operator has configured
`NEXT_PUBLIC_OBSERVANCE_SERVER_ADDRESS`.

The decoded Record route remains the deeper receipt:

> /record/the-record-keeps

The page opens. The website is now an in-universe place, and not only a Record
page: it is the old public face of the server that still keeps files.

What John learns:

- off-world content is canonical
- the record preserves copies outside Minecraft
- the server address is hosted by the old listing, not hidden in the zip
- hyphenated decoded phrases may be route slugs, not coordinates

Launch rule:

- do not plant the live in-world lure until `/`, `/the-hold/the-hold.zip`, and
  `/record/the-record-keeps` work from the deployed site
- do not put the live Paper port inside `the-hold.zip`; configure it on the
  listing with `NEXT_PUBLIC_OBSERVANCE_SERVER_ADDRESS`

### 5. Side Destinations: Longevity and World Truth

Status: **live content**, **placement/ops-dependent**

Side destinations do not gate the spine. They make the spine emotionally and evidentially rich. John is
thorough, so he follows them throughout the run.

Required side beats:

- `dest-warm-stair` - the third lamp cold; kept-light grief
- `dest-empty-cairn` - offering taught by example
- `dest-warm-town` - Aro's warm-town lie contradicted
- `dest-school-stand` - domestic proof they were human
- `dest-bird-coops` - sacred beast and seed-cake
- `dest-far-water` - Sella's mirror/count evidence
- `dest-markers-row` - bowing and seventh surplus
- `dest-cistern-7` - lamp-in-water unease and oil lore
- `dest-third-bay` - Iss breach and deep-line context
- `dest-dead-shrine` - the seventh's place distinct from Iss
- `dest-set-apart` - digit cross-count and warm cold-lamp
- `dest-watch-floor` - Brann's dark-hours watch log
- `dest-deep-market` - ordinary life, trade, warmth
- `dest-ration-table` - hunger, child evidence, humanity
- `dest-undercroft-seal` - Orin seal and crouch-to-read
- `dest-pell-mark` - human record mirrored by Pell
- `dest-way-up` - Iss's forgotten Mouth, true but hollow
- `dest-gutter-lamps` - continuing the rite after failure
- `sq-cold-ignition` - base lure anomaly
- `sq-count-journal` - base journal counting down in Orin's hand

Playthrough requirement:

- side destinations appear in chronological exploration, not as a late appendix
- each one gets observed content, why John follows it, what it changes, and whether it contradicts a rumor

### 6. Recovery Archive Threads

Status: **live**

The archive clusters evidence under five threads:

- **who** - who they were
- **place** - what the Hold was
- **happened** - what broke
- **surface** - what the surface knows or misremembers
- **human** - whether the kept were still people

Archive cards are not summaries. They are evidence. The full playthrough must show card bodies when John
earns them.

Core archive turns:

- Vaun counted and gave nothing back
- Mara read the rite and did not walk it
- Sella counted seven where others counted six
- Orin sealed from inside
- Brann watched through the black moon
- Iss was warm, useful, loved, and wrong
- no wall was ever built here
- the record writes the living into an open column
- the surplus names prove the kept were still human enough to be counted wrongly

### 7. Wren, the Companion

Status: **live**, showrunner consumption should remain verified during playtest

Wren is not optional flavor. He is the intimate version of the record's violence.

Trust:

- he warns John off dangerous routes
- he gives real help
- he asks personal questions that feel like care
- he says "stay close"

Crack:

- John notices the Watcher only knows things said near Wren
- Wren does not fully deny it

Reveal:

- Wren admits he fed the record names, plans, and personal details
- he claims he did it to keep the group safe
- the kept-close tally proves the mechanism

Reckoning:

- condemn, understand, or free
- the choice changes how sharp-quote echoes continue or cease

Director rule:

- Wren must be present early enough to be trusted before he is doubted
- the reveal must be a betrayal of intimacy, not a lore dump

### 8. The Keeper Field

Status: **live**

Each keeper is both a puzzle mode and a wound.

**Vaun**

- mode: count, hoard, return first-of-the-deep, Caesar final fragment
- wound: kept and gave none back
- final letter: A

**Mara**

- mode: books, page-line-word, marked pages, walking the rite she only read
- wound: knew the way and did not walk it
- final letter: V

**Sella**

- mode: reflection, water, Atbash, far-water count
- wound: preserved a count others rejected
- final letter: E

**Orin**

- mode: bow/crouch, fall-order markers, frame dials, sealed undercroft
- wound: built the asking stones and resisted the asking
- final letter: R

**Brann**

- mode: dark hours, silence corridor, black moon, rail-fence/fire count
- wound: watched, misread, and kept watch too long
- final letter: Y

**Iss**

- mode: warm lie, acrostic `no wall`, false Vigenere, NBT/lamp falsification, final acrostic correction
- wound: turned away and wrote mercy over refusal
- final letter: N, correcting the false M

Final name:

> AVERYN

### 9. Conduct and Customs

Status: **live**, with some visual reward polish deferred

The seven ways are detected as conduct, not only answered as text:

- keep the lamp
- respect the deep line
- keep the dark hours
- return the offering
- bow
- keep the unspoken
- spare the sacred beast

The Unlit Deep is the group latch:

- explicit flame acts below the deep line on the taboo moon are recorded
- the break is group-scoped and reversible
- `broken_by` is recorded but not spoken

Director rule:

- customs must feel like survival practices before they feel like rules
- consequences take warmth, certainty, and comfort, not progress

### 10. Planned Media as Canonical Evidence

Status: **media-required**

The found footage, Drive folder, waveform/spectrogram, and `the-hold.zip` are record copies.

Current staging state:

- found footage is produced/local-staged; see `design/MANUAL-MEDIA-STAGING.md`
- `the-hold.zip` is produced and present in `dashboard/public/the-hold/`; see `design/MANUAL-MEDIA-STAGING.md`
- recovered archive/spectrogram is produced/local-staged and hosted on Dropbox; see `design/MANUAL-MEDIA-STAGING.md`
- found footage is hosted on YouTube and HTTP-reachable; see `design/MANUAL-MEDIA-STAGING.md`
- YouTube videos, archive contents, and spectrogram payload are operator-checked; do not flip media-ready flags or plant player-facing trails until the matching story gate is ready

Each media artifact must have:

- one concrete payload that John can use
- one emotional/worldbuilding payload
- one self-confirming return path into Minecraft or the record site

Found footage:

- John watches normally, then scrubs frame by frame
- a frame, audio layer, subtitle, or metadata point reveals a real clue
- the clue confirms a known language: kept, record, wall, seventh, count, light, deep, name
- clip 1 resolves to `ASH-13` and flags `media_prior_base_read`
- clip 2 resolves to `where the reeds fold back` and flags `media_far_water_read`
- clip 3 resolves to `stay awake` and flags `media_black_moon_read`
- clip 4 is late only; after the name is earned it resolves to `six return one is not kept`

Drive folder:

- filenames, order, images, metadata, and audio all matter
- the folder feels recovered, not like a puzzle hub

Spectrogram:

- damaged sound becomes record text or glyphs
- the launch spectrogram phrase is `I WAS NOT KEPT`; it maps to the optional `spine-recovered-archive`
  answer and flags `media_spectrogram_read`
- it must not reveal `AVERYN` before the group earns the name in-world

`the-hold.zip`:

- the readme is not fully trustworthy
- John compares the hold's count against the world
- the final room points to the old listing instead of exposing a server port
- absence and redaction become evidence

### 11. The Seventh Below and the Final Reading

Status: **live**, **placement-required**

John learns:

> i kept every way... seven ways, and i kept all seven... and i was not kept.

And:

> he carved that i was spared. i was not spared. i was refused.

The final reading gathers six keeper fragments into:

> AVERYN

Iss's last correction matters:

> i told you the last of it was m
> take the first mark of each line down
> see what the warm words were laid over
> n is the letter i cut and called m

The answer is N, not M.

### 12. Accepting and Release

Status: **live**, **placement-required**, **ops-required**

The group performs the Accepting together. The finale is physical and collective, not only typed.

Release language:

> you named me...

> you gave me back my name. AVERYN...

Closing:

> the record is closed. thank you for coming down. - AVERYN, the seventh, kept no longer

Director rule:

- the finale is not "enter password, win"
- it is the return of a name the record refused to keep correctly

## Launch Manual Checklist

Required before a real launch run:

- apply migrations and reseed
- host resource pack and set URL/SHA1
- place world sites and deep sites
- spawn townsfolk
- stage cold open
- run `/observance reading`
- run `/observance finale`
- verify deployed website root `/` renders the old listing and configured server address
- verify deployed `the-hold.zip`
- verify hosted found footage, Drive folder, waveform/spectrogram
- rotate exposed credentials
- run vertical slice

## Editorial Gaps To Close Next

1. Rewrite the full John playthrough from this manifest, with no internal uncertainty in the story body.
2. Move spec-only config comments out of live config or label them as non-live in an operator-safe way.
3. Add a coherence check that verifies route/key/card/NPC coverage.
4. Re-audit planned media payloads once actual files exist.
5. Playtest the first hour for feeling: haunted first, puzzle second.

