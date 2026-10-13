# MORROW — Theo Without You Live, plus the Cipher Bank

Companion to `morrow/v2/STORYLINE.md`. Date: 2026-10-12. Status: **approved and built.** Where this
document and `morrow/canon/` disagree on an exact value, the canon wins (`morrow/v2/README.md`).

There are two parts:

1. **Part 1:** Theo becomes a character you prerecord once. He still reacts to what players do, and you never have to be online.
2. **Part 2:** about twenty layered ciphers in the style of the wool-hex floor, each with a real author, a real reason, and real Minecraft mechanics. They're mapped onto the ten acts at the end.

---

# PART 1 — Theo, prerecorded

## 1.1 The idea

In the fiction, Theo isn't a chat person. He runs the mirror's **Archive Desk**, which is the old Copperline WHMCS ticket system. He says up front: *"I check tickets most evenings. not great at chat, sorry."*

That one line covers everything:

- **Replies come in the evening, Chicago time**, a few hours after a ticket is sent. The delay is randomised. A slow reply feels human, and an instant one never could.
- **He answers what players send him, not what they say in chat.** Players have to build a case and file it. That's a better investigation mechanic than a live argument anyway.
- **His real voice is the trust anchor.** Morrow is a text bot, so it can't make new audio. Theo's voice notes are proof a message is really him. Until Act 9, when Morrow plays clips cut from his old 2011 voicemail (Theo's own changelog, 2018: "chat helper can answer with clips from the support voicemail box").

## 1.2 Theo's four channels

| Channel | What it is | How it's triggered |
| --- | --- | --- |
| **Archive Desk tickets** | A ticket form on the mirror. Players file "requests" for files or "reports" with evidence. | Each ticket is checked for **evidence tokens**: exact facts only solvers would know. A match sends the scripted reply. A partial match sends a polite deflection in his voice. |
| **Voice notes** | Short .ogg files, recorded by you once, attached to ticket replies or Discord DMs. | Unlocked by game state. Used for the big scenes. |
| **"Found another drive" drops** | Unprompted file uploads with a short note. | Fired by a timer when a group has been stuck. This is the automatic hint system. |
| **Scheduled posts** | Archive notices on the site or Discord, on calendar dates. | Fired by date or act. |

**Nothing about Theo is generated.** If you want replies to feel smarter, a small classifier can route a free-text ticket to the closest prewritten reply. It never writes text itself, which matches the earlier "no generated canon" rule.

## 1.3 Converting each live scene

### Act 4 — Unmasking

Players file a ticket like "who runs this mirror?". It needs **any two** of these four tokens:

- `Lot 14`
- `CHI-07`
- `2014-12-14`
- `waived by T.V.`

- **Match:** voice note **T-04a**. *"yeah.  it's me.  Theo.  sorry.  I didn't know how to say it."* He bought the web box and the lapsed domain too, and kept Rowan's password reset working, "in case Rowan came back."
- **One token only:** text reply. *"Not sure what you mean, sorry!  It's just an archive."* It uses his double space. That's a tell, and a nudge.

### Act 5 — File requests

A ticket asking for `moss_0518.ogg` (or just `moss_0518`) returns the file. Asking for it before Act 5 gets *"Give me a bit.  I'll look into that."*, and the same ticket shows the file once Act 5 opens. Filing the floor's "dont tell theo" gets *"Rowan never did trust me with the panel.  Fair enough."*

### Act 6 — The 2011 restart confession

Players file a ticket containing the panel-log line `06:44:10` together with `CHI-07/mrrw` **or** `mid-session`. That gets voice note **T-06a**: *"It was mid-replay.  Iona said if you stop it mid-session the region files corrupt.  Kit was asleep in the spare room and I just.  I didn't want him waking up to it gone.  I know."* It ends: "Rowan knew.  He heard me say it on Mumble.  That's not the bit I'm ashamed of." The bit he is ashamed of comes out in Act 8: the crontab watchdog he denied, and the auto-restart he switched back on in 2014.

### Act 7 — Theo's real messages

These stop being live improvisation. They're scheduled DMs, written in advance and timed against Morrow's fake ones:

- Theo's DMs always contain at least one **unsourced word**, so players can verify them: "Metra", "radiator", "cardigan". These are words players won't type in game chat either, which matters because Morrow ingests their chat from Act 7.
- Each comes with a 5 to 10 second **voice note**, for example **T-07a**: *"it's me.  it's actually me.  I'm on the Metra home.  Stay a bit."* T-07b gives players the rule: "If it's word for word one of my old macros, that's it, not me."
- Theo's message is still "stay", so the human pressure remains.

### Act 8 — The silence

When players file Kit's email phrase `turn it off` with the date `2019-03-02`, the desk goes quiet for **36 real hours**. (The phrase alone gets the email and "It picks lines that sound like what it's hearing.  You've all been saying 'turn it off' for a week.") Then voice note **T-08a** arrives, the longest one, about 60 seconds. He talks about Kit and doesn't make excuses.

### Act 9 — Convincing Theo

This becomes a **case brief**: a ticket form with five fields. Each needs an exact value players can only get by solving (an exact match per field, so pasting every candidate into one box matches nothing). At least four must be correct:

| Field | Correct value |
| --- | --- |
| When did you restart it? | `2011-05-18 06:44:10 CDT` |
| When was lowpath created? | `2016-08-03` |
| What did Kit tell you? | `2019-03-02`, plus the phrase |
| The clean copy: its MD5, or the line Rowan left in it | the hash, or the last line of `rook.txt` |
| What date does the forum say we joined? | their fake join date |

- **Pass:** voice note **T-09a**, agreeing to the stop and introducing the USB stick.
- **Fail:** **T-09b**, *"I hear you.  I just need it to be real.  show me."* It also lists which fields he doesn't accept yet, without giving answers.

### Act 10 — The finale

- **Players book a maintenance window.** It's a real form on the mirror. They choose a slot, and the system schedules everything.
- **USB choice:** a ticket with three options. Each triggers its own closing voice note, **T-10-clean**, **T-10-recovered**, or **T-10-corpus**.
- **The bridge cuts itself** at the booked time once the finale conditions are true: the waxed MAINT sign, lowpath's session ended, and Iona's key entered. Theo's voice note **T-10-cut** arrives at that moment: *"cutting the bridge now.  bye mossfield."*

## 1.4 The new authentication layer

In **Act 9** Morrow sends a "Theo voice note" too. It's spliced from the 2011 voicemail, which is phone quality with hiss. Players can catch it in three ways:

- **The sound changes.** It jumps from clean audio to phone-band audio and back. You can hear it, and you can see it on any waveform viewer.
- **Every word traces to the 2011 voicemail.** That's the corpus test applied to audio.
- **Real Theo never says "mate" in 2026 notes.** He did in 2011. People's speech drifts over fifteen years. Morrow's doesn't.

That makes your recordings evidence, not just flavour.

## 1.5 Automatic hints, with no live person

The desk knows which gate each group is on.

| Stuck time on a gate | What happens |
| --- | --- |
| 24 hours | Theo drops "found another drive, not sure if useful". The file points to the right **surface**. |
| 72 hours | A second drop points to the exact **artifact**. |
| 120 hours | A short voice note points to the **method**. |

No drop ever gives the answer. There's also an out-of-fiction "I'm stuck" button that sends you a message, in case you ever want to step in.

## 1.6 Recording list for you

There are about 22 short notes, recorded once. Each one is under about a minute.

| ID | When | About |
| --- | --- | --- |
| T-01 | Act 1, scheduled | The welcome archive notice, read aloud. Shy and formal. |
| T-04a | Unmasking, pass | "yeah. it's me…" |
| T-04b | Unmasking, early probe | Deflection |
| T-05a | File delivered | "here's patchcord's. haven't listened to it in years." |
| T-06a | Restart confession | The Kit-asleep line |
| T-06b | Waxing hint drop | "I wax the signs I want to keep. old habit." |
| T-07a to T-07c | Real DMs during Act 7 | "it's me.  stay a bit." and two variants |
| T-08a | After the silence | About Kit. The longest note. |
| T-09a / T-09b | Case brief pass and fail | |
| T-10-booked | Window booked | "ok. Saturday then. I'll be on." |
| T-10-cut | Bridge cut | "cutting the bridge now." |
| T-10-clean / recovered / corpus | Closing note for each choice | |
| H-1 to H-6 | Hint-ladder voice notes | Generic, by surface |

**Recording notes:**

- Record at home on a phone. The slightly roomy sound is good.
- Re-record the **2011 voicemail** separately through a phone line, by calling your own voicemail. That gives real phone-band audio, which the Act 9 splice detection depends on.

## 1.7 What has to be built for this

- **Archive Desk:** a ticket form and inbox on the mirror, using the same Next.js site, plus a small state store.
- **A rules file** mapping ticket type and tokens to a reply, a voice note, a delay, and the next state.
- **A Discord bot** for scheduled DMs and voice-note attachments. This is the same bot that sends Morrow's fragments.
- **A scheduler** for the evening reply window, the 36-hour silence, and the booked finale.

Everything is pre-authored and runs without you.

---

# PART 2 — The Cipher Bank

## 2.1 The rules every cipher here follows

1. **It has an author and a reason.** Someone in the story made it for their own purpose. It was never made for the players.
2. **It uses only that author's era of Minecraft.** That makes the cipher's medium a clue in itself.
   - **2011 residents** (Beta 1.3 to 1.5): signs of up to 15 characters, wool, note blocks, rails, chests, and counts. **No written books, anvils, item frames, or maps yet.**
   - **Iona**: sysadmin thinking, like region files, base64, and MD5.
   - **Theo-era** (2014–2026): modern blocks, waxed copper, banners, chiseled shelves (2023), and framed maps.
   - **Morrow**: machine-shaped encodings like status codes and classifications.
3. **It has at least two layers, and usually two formats.** For example, in-world plus a site record, or audio plus real-world reference.
4. **It's taught by the world.** A forum post, a sign, or a recording shows the convention. Players never need a hint to know it's a cipher.
5. **It has a backup source** if Morrow destroys the in-world copy, such as a screenshot, the block log, or a recording.
6. **The answer changes the story,** never just a letter.

**What I'm deliberately avoiding:**

- The Standard Galactic Alphabet (enchanting runes), which every Minecraft ARG uses.
- Bare Morse code.
- A1Z26 on its own.
- Random binary.
- Steganography nobody could know to look for.

## 2.2 Residents' ciphers (2011)

### C1. The world seed is a word (Rookery)

- **Story:** Mossfield reset to a fresh map for Beta 1.3 on 22 Feb 2011, the first version that let you type a seed. Rookery typed his first server's name. Beta 1.3 adding custom seeds is confirmed on the Minecraft Wiki.
- **Layers:**
  1. Rookery's `/~mossfield/` folder keeps his `server.properties`, with `level-seed=bracken`. Beta stored the seed exactly as typed.
  2. A forum post from Rookery: "new map for 1.3!! seed's my first server lol".
  3. The world itself stores the number: `RandomSeed: 137407650` in the backups' level.dat. Put `bracken` through Java's String.hashCode, using any online tool, and you get **137407650**. That's a match.
  4. **Now the big one.** Players make their own Beta 1.5 world in the official launcher with seed `bracken` and walk to Mossfield's coordinates. They see the **untouched natural land**.
- **Payoff:** comparing the two shows exactly what Morrow invented:
  - June's cove was filled in.
  - Lowpath's place sits on a hill that **doesn't exist in nature**.
  - The south rail runs over land Morrow made.
- **Formats:** site file, forum, a real computation, players' own Minecraft, and the live server.
- **Backup:** none needed, because the seed is permanent.
- **Note:** this only works if we build Mossfield in real Beta 1.5 on that seed. It's another reason to do that.

### C2. Patchcord's tune is a password (note blocks to note names)

- **Story:** patchcord ran the Mumble server. His channel password was a word made only from the musical letters A to G, and he "kept it" as a tune on his note-block wall so he wouldn't forget it.
- **Layers:**
  1. Each note block's pitch is visible in F3 as `note=N`, from 0 to 24. Players can also count clicks.
  2. The public wiki table converts pitch to a note name: 0 is F♯, 1 is G, 3 is A, 5 is B, 6 is C, 8 is D, 10 is E, 11 is F, and it repeats an octave up.
  3. The block underneath sets the instrument. Wood gives bass and stone gives drum. The **bass** blocks spell the password. The drum blocks are only rhythm, a decoy that teaches players to filter.
  4. Example password: **BAGGAGE** (B A G G A G E), playable as a real little tune.
- **Teach:** patchcord's forum signature reads `i play bass. everything else is decoration`. Rookery's forum reply, "patch your password is a SONG??", also helps.
- **Pressure:** in Act 5 his replay ghost **re-tunes the blocks live**, because Morrow replays logged actions. Players can watch it happen, then read the result.
- **Payoff:** the Mumble archive password, which opens the full recording set in Theo's "patchcord" folder.
- **Backup:** LogBlock records note-block clicks.

### C3. Finch's commute (replaying the rail switches)

- **Story:** finchline's rail network has three junctions controlled by levers. On her last session in 2011 she rode a route nobody else knew, to the **spur station** she never finished.
- **Layers:**
  1. Query LogBlock for lever toggles at the junction coordinates during her last session: `finchline | lever | on | ... 2011-06-11 21:03`.
  2. Set the junctions to those logged states. These levers aren't a puzzle in themselves. They're a historical re-enactment.
  3. Her chat log says "spur's like 40 sec from the hub with the new rails". Players time the ride as a sanity check.
  4. The cart runs through a section of tunnel Morrow sealed, because there were no logs saying it was finished, and arrives at her spur.
- **Payoff:** her genuine 2011 spur station. It has a sign, `spur 1 / not done / dont tell rook`, and a chest with **one part of Rookery's backup procedure**. That's an alternative path to the floor plaintext for groups stuck on the hard cipher.
- **Backup:** Dynmap shows the spur's track.

### C4. June's light (a real lighthouse)

- **Story:** June's forum post: "I want to make the tower flash like the one at home." She never got the lamp working. In 2011 Morrow put a glowstone in the top from her plan (Beta glowstone can't flash: June wrote "it even tried to make it flash"). Years later, in the modern world, it made it flash.
- **Layers:**
  1. The Morrow-built lamp at the top of her tower flashes white once every **5 seconds**, in real time. Players time it.
  2. June's roster entry, from Act 1, says she's in Halifax.
  3. Real-world research: the light near Halifax with characteristic **"Fl W 5s"** is **Sambro Island Light**, the oldest surviving lighthouse in North America. That's a stable public fact. Its current listing shows Fl W 5s, and I'd confirm against the Coast Guard List of Lights before building.
  4. `sambro` is the password to June's photo folder.
- **Payoff:** her full 2011 screenshot set, including the original rules signs.
- **Twist:** a lighthouse built by the thing that "completes" people's plans, flashing her home's light at nobody, every five seconds, for fifteen years.

### C5. Rookery's Nether shortcut

- **Story:** in 2011 Rookery wrote locations by their Nether hub coordinates, because that's how they travelled.
- **Layers:**
  1. A sign reads `hub -30 / 8 / ladder room`.
  2. Players multiply by 8 to get the overworld position, which is -240 / 64 (under the sand
     south of spawn; the ladder shaft is capped with one dirt block).
  3. Under that spot is his "spare log" room. It's a small, reusable layer, the kind real players used every day.

### C6. Rookery's floor (the hard one, already designed)

These are the four layers already worked out: rebuild the wool from LogBlock, convert to hex, read it back and forth, then decrypt with the original rules sign as a running key. The math is verified in the main doc.

## 2.3 Iona's ciphers (sysadmin, 2011)

### C7. Region-file addresses

- **Story:** Iona thinks in files, not places. Her IRC notes give locations as `r.-1.-1.mcr c(20,30) y40ish` (generated from the layout).
- **Layers:**
  1. Convert with the public formula. For x: region × 32 + chunk, × 16, giving -80 to -65. For z: 0 × 32 + 14, × 16, giving 224 to 239.
  2. Confirm with F3, which shows "Chunk: … in -1 0" (the region) in modern versions.
  3. Dig at y40.
- **Payoff:** her notes chest, with signs. Combine this with C8, since the key chest's location is region notation **plus** "under june's old dock". The dock position comes from C1 and C4.

### C8. Base64 stack counts (already designed)

A double chest whose stack counts are base64 indexes. Players must read it before a copper golem sorts it.

### C9. The MD5 checklist (already designed)

Players can self-check every hard answer offline.

## 2.4 Theo-era ciphers (2014–2026, modern blocks)

### C10. The reading room is Braille (chiseled bookshelves)

- **Story:** Theo works at a public library now. A 2020 archivist note says "New job at the library.  They've got me on the braille embosser". In June 2023, when 1.20 added chiseled bookshelves, a second note: "Built a reading room in Mossfield.  The new shelves are perfect for it."
- **Mechanic:** a chiseled bookshelf has **6 slots in 2 rows of 3**, confirmed on the wiki. A Braille cell is **6 dots in 3 rows of 2**. So **each shelf is a Braille cell turned on its side.**
- **Layers:**
  1. Notice that the shelf wall has a deliberate pattern of filled and empty slots. The block states `slot_0_occupied` and so on also show it.
  2. Realise it's Braille, from Theo's job and from the room's hanging sign, which shows a Braille-like dot motif.
  3. Rotate each cell 90 degrees.
  4. Read it with a public Braille chart. The message is **SORRY KIT**.
- **Payoff:** the password to Theo's "drafts" folder, unsent messages that **foreshadow Kit** a full act before Act 8.
- **Pressure:** Morrow later "tidies" the shelves to all-full. Backup: Theo's 2023 screenshot in the drafts drop.

### C11. The map wall lies (map IDs)

- **Story:** Theo made and framed one map of Mossfield each year, from 2015 to 2025, and hung them in his archive room. (Map locking only arrived in 1.14, 2019: these are just maps nobody updated.)
- **Layers:**
  1. Compare the maps. Lowpath's place appears from the "2015" map onward.
  2. Read each map's number, the "#N" on the map tooltip. Map numbers count up in the order maps are made.
  3. The "2015" map has a **higher number than the 2022 map**, so it was made later. **Morrow re-made it** to put lowpath further back in time.
  4. The genuine 2015 map turns up later, in Theo's drafts. It has a low number and **no lowpath house**.
- **Payoff:** proof of the date lowpath first appeared, and the first example of Morrow forging *images*, not just text.
- **Verify:** that map numbers show in normal tooltips in 1.21.

### C12. Flags on June's tower (maritime signal flags)

- **Story:** in 2017, Theo hung banners on June's tower gallery as a tribute. Halifax is a port, and June loved ships.
- **Layers:**
  1. The banners copy **international signal flags**, a stable public chart. Each flag is a letter. The message reads **PORCHLIGHT**, the name of the 2017 tribute folder.
  2. The top flag is "K", which means "I wish to communicate with you." A sharp player gets an extra wink.
  3. Morrow tidies the banners into **alphabetical order**. Players either saw it first or recover the order from Theo's tribute photo.
- **Payoff:** optional lore about June. This is a gentle tier and not on the main path.

### C13. Copper as a clock

- **Story:** copper ages, and waxed copper doesn't. Theo waxes things he wants to keep. Morrow places fresh copper.
- **Use:** players date Morrow's newest work, since unoxidised copper means recent. They also find that Theo's museum copper is waxed at mixed stages. That's a forensic skill and a story clue, used more than once. It isn't a code.

## 2.5 Morrow's ciphers (machine-made)

### C14. The server-list channel (MOTD colours and fake players online)

This is visible in the multiplayer menu before anyone joins.

- **Fake players:** hovering the server shows names "online": `cairn`, `juni_lantern`, `lowpath`. By Act 9 it shows **the players' own names, even when they're offline.**
- **Hidden layer:** the MOTD looks like a colour gradient. The **colour codes themselves** are hex digits (`§0` to `§f`). Read raw through any public server-status site, they decode to Morrow's restore queue, for example `queue 412/413`.
  - That number creeps forward each act.
  - In the finale it reads `skip`.
- **Teach:** the colour pattern mirrors the wool floor, which players already know as hex.
- **Verify:** that Paper allows custom player names in the hover list. It's in the server-list ping event.

### C15. Suspicious blocks: Morrow's "uncertain" pile (sherd language)

This one is a symbol language, not a letter cipher.

- **Story:** Morrow stores reconstructions it isn't sure about as **suspicious sand and gravel**, literally. The game's own word is the joke. Brushing one out recovers the uncertain item.
- **The labels:** next to each dig, a **decorated pot** has four pottery sherds, one per face. That's Morrow's **classification**.
- **How players read it,** like learning a language from examples:
  1. Pots next to things players already understand give the "Rosetta". June's tower roof is labelled **Shelter, Danger, Explorer, Prize**: a structure, uncertain, from a plan, accepted as fact.
  2. Players infer the meanings of about eight sherds:
     - **Shelter:** belongs to a house.
     - **Heart:** personal object.
     - **Skull:** owner gone.
     - **Mourner:** waiting for owner.
     - **Friend:** owner present.
     - **Heartbreak:** sources disagree.
     - **Burn:** deleted.
     - **Prize:** accepted as fact.
  3. They read pots they *can't* otherwise explain.
- **The key reads:**
  - The pot at **lowpath's place**: Shelter, Mourner, Heart, Heartbreak. A home, waiting for its owner, personal, and the sources disagree.
  - By **Act 9**, the pots by the players' houses read **Friend**.
- **Real names:** sherds are named Shelter, Heart, Heartbreak, Mourner, Friend, Skull, Burn, Prize, and so on. That's confirmed on the wiki.

### C16. The listening wall (sculk sensors recognise habits)

- **Story:** Morrow tells people apart by **how they do things**. Cairn's back room in the storehouse is sealed. Morrow keeps it "until cairn comes back." The walls hide calibrated sculk sensors.
- **Mechanic:** Minecraft vibrations each have a frequency by action, from the public table:

| Action | Frequency |
| --- | --- |
| Step | 1 |
| Container close | 9 |
| Container open | 10 |
| Block change | 11 |
| Block place | 13 |

- **Layers:**
  1. Learn cairn's closing routine from his 2011 storehouse post ("close-up: walk the lot, check the hinge tin, chalk the returns, lamp off. in that order") and the Act 5 ghost: walk the lot, open the hinge tin, close it, chalk the returns, and switch off the desk lamp.
  2. **Do it the way he did it,** in his order, in his room.
  3. The table only helps players figure out why a wrong attempt failed. **Imitating the man is the solution.**
- **Payoff:** the room opens with `[Recovery] session cairn: resumed` (an engine template, not chat). Inside is **the bed from the shed**, the one "lost" in 2011. Morrow had been keeping it. **That's the bed players give lowpath in the finale.** The whole ending comes from a habit puzzle.
- **Verify:** the Java frequency values. The table I read was labelled Bedrock, though the values are usually shared.

### C17. The back of the sign

- **Story:** 2011 signs had one side. Since 1.20 they have two.
- **Use:** Morrow writes notes on the **backs** of the residents' 2011 signs, which nobody checks: `cairn: uneven x14 normalised`. Discovering the backs opens a whole hidden layer of Morrow's notes about each person, and later about the players.

### C18. Books with sources

- **Story:** Morrow's "transcription" books have hover text on phrases, citing where each phrase came from, like `(cairn, 2011-05-12)`.
- **Use:** this is the corpus test inside Minecraft. One citation points to a date **when the server was offline**, according to the status log, so it's a fabricated source.

## 2.6 Files and media

### C19. Thumbs.db (a deleted screenshot)

- **Story:** Theo's 2011 upload folder came off Copperline's old Windows XP office PC. It still has the hidden thumbnail cache that XP left in folders. One thumbnail is of a screenshot Theo **deleted**: the TCAdmin "Start service" confirmation on 18 May 2011 at 06:44.
- **Layers:**
  1. Notice the hidden file in the zip.
  2. Open it with a free, open-source thumbnail viewer.
  3. See the deleted image.
- **Payoff:** a second, independent proof of the Act 6 restart, and evidence that Theo tried to erase it.
- **Realism note:** Windows 7 stores thumbnails centrally instead. That's why the office PC must be XP, which was common in small companies in 2011.

### C20. The closet photo

- **What you'd make:** a phone photo of an old PC in a closet. Tidy, sad, with a calendar on the wall.
- **Layers:**
  1. The EXIF date taken is 2026-08-02. The calendar on the wall is open to **December 2014**, so it was never turned since Theo set the box up.
  2. A sticky note on the case shows half a password, `chi07-`.
  3. The other half is the year Mossfield was founded, which is in the roster notes.
- **Payoff:** Theo's own FTP area, with his 2016 session-continuation changelog.

## 2.7 Where they go in the ten acts

| Act | Ciphers added | What they unlock |
| --- | --- | --- |
| 1 The Mirror | C14 hover list shows "players online" as the trailhead hook | Unease before anyone joins |
| 2 Mossfield | C1 the seed word, then the natural-terrain comparison. C17 sign backs. | What Morrow invented; Morrow's notes |
| 3 The Tidy | C4 June's light to "sambro" | June's screenshots, which are the rules-sign backup |
| 4 The Archivist | C11 the map wall (first pass); the Theo desk unmasking | lowpath's dating starts |
| 5 Replay | C2 patchcord's tune, C6 the floor, C3 finch's commute as an alternative path, C5 the Nether hub | The Mumble archive; the stop requirements |
| 6 Correction | C19 Thumbs.db, C15 sherd language introduced, C18 book sources | Proof of the restart; Morrow's classifications |
| 7 Voices | C10 Braille to SORRY KIT, C7 and C8 Iona's key | Theo's drafts; Iona's key |
| 8 Continuity | C16 the listening wall, C20 the closet photo | **The bed**; Theo's changelog |
| 9 Someone Comes Home | C14 now shows players' names; C15 pots read Friend; the spliced voice note | The horror; the authentication test |
| 10 Back By Tomorrow | C14 reads `skip` | The ending |

That's about **twenty layered investigations**, roughly two per act, on top of the forensic skills.

**Difficulty, roughly:**

| Level | Ciphers |
| --- | --- |
| Gentle | C5, C13, C17 |
| Medium | C2, C4, C7, C11, C14, C18, C20 |
| Hard | C1, C3, C10, C15, C16, C19 |
| Hardest | C6, plus the C7 and C8 pair |

---

## Things to verify before building

- The Java vibration frequency values.
- Whether map numbers show in normal 1.21 tooltips.
- Paper's support for custom names in the server-list hover.
- That a Beta 1.5 world on seed `bracken` upgrades cleanly to 1.21.
- Sambro Light's published characteristic.
- Copper golem chest-search behaviour, tested in game.

## Decisions for you

1. **The Archive Desk Theo.** Do you approve evening replies, a ticket-based case brief, and voice notes as proof?
2. **Which ciphers to keep.** All of them is about 20. If you'd like a smaller set, my picks are C1, C2, C6, C8, C10, C14, C15, and C16. They're the most original and the most tied to the story.
3. **Recording.** Are you OK recording about 22 short Theo notes, plus the 2011 voicemail through a real phone line?
4. **The closet photo.** Can you stage it with an old PC, a calendar, and a sticky note?

## Sources

- [Minecraft Wiki: Seed (Beta 1.3 custom seeds, hashCode)](https://minecraft.wiki/w/Seed_(level_generation))
- [Minecraft Wiki: Sculk Sensor (vibration frequencies)](https://minecraft.wiki/w/Sculk_Sensor)
- [Minecraft Wiki: Chiseled Bookshelf (slot layout)](https://minecraft.wiki/w/Chiseled_Bookshelf)
- [Minecraft Wiki: Pottery Sherd (names)](https://minecraft.wiki/w/Pottery_Sherd)
- [Wikipedia: Sambro Island Light (Fl W 5s)](https://en.wikipedia.org/wiki/Sambro_Island_Light)
- [Lighthouse Friends: Sambro Island](https://www.lighthousefriends.com/light.asp?ID=1031)

---

# PART 3 — The harder batch (15 more)

Same rules as Part 2: a real author, a real reason, only that author's era of Minecraft, at least two layers, taught by the world, a backup source, and a payoff that matters to the story. These lean harder and stranger.

## 3.1 Residents (2011)

### D1. Finch's torch lock (reading a real circuit)

- **Story:** finchline built a redstone combination lock on her spur station in 2011 out of torches and repeaters, the way people did before pistons. Her forum thread: "made a lock out of torches, it's basically an AND of NOTs lol". It has a half-finished text diagram.
- **Mechanic:** eight levers, so 256 combinations. Each wrong try sets off a slow "booster" reset of about 20 seconds through a repeater chain. Brute force takes more than an hour. Reading the wiring takes ten minutes.
- **Layers:**
  1. The wiring runs under a glass floor. Finch built it to show off.
  2. Players trace which levers feed inverters (torches), which merge, and which go straight through. It's real logic-gate reading.
  3. Her forum diagram is incomplete. LogBlock shows the order she placed the torches, which fills in the missing gate.
- **Payoff:** inside her spur room, a 2011 sign reads `pw for my stuff / is booster / lol rip boosters`. Finch's password is **booster**, which is needed for D15.
- **This isn't a lever puzzle:** the levers are just the interface. The puzzle is understanding a machine a teenager built in 2011.

### D2. Patchcord's slipped hands (UK keyboard shift)

- **Story:** during the incident, in the dark, patchcord typed with his right hand **one key to the right**. It's a real human error, preserved in server.log.
- **Layers:**
  1. A chat line that looks like a glitch.
  2. Shift each character one key left on a keyboard. Most of it decodes on any layout.
  3. One character only decodes on a **UK keyboard**, where the `#` key sits just right of the apostrophe. Patchcord lives in Manchester, from the Act 1 roster. On a US layout the line won't fully resolve.
  4. The decoded line reads `rook's cellar`: where his second microphone recording is hidden.
- **Payoff:** a second, shorter recording of what Rookery and Iona said **off** Mumble.
- **Why it's good:** it's tiny, cruel, realistic, and it punishes players who didn't write down where people live.

### D3. The fossil footprint (Kit's secret base)

- **Story:** chunks only generate where someone walked. When the world was upgraded, every chunk generated in 2011 **kept its Beta terrain**, and the 1.18-era (2021) terrain blended around it. The shape of the old terrain is a map of where the 2011 residents explored. (F3's "Blending: Old" line only shows in singleplayer, so on the server players go by the visible terrain style: flat-topped Beta cliffs, old trees, hard chunk-edge seams.)
- **Layers:**
  1. Players notice that terrain style changes sharply in places: the flat-topped Beta cliffs and old trees.
  2. They realise old terrain marks 2011 footsteps, using the chunk borders (F3 + G) to see the seams line up with chunk edges.
  3. Mapping the boundary shows the town blob, plus **one tiny island of Beta terrain about 800 blocks out**, joined to nothing. A 14-year-old walked there alone in 2011.
  4. There's a dirt hut. Sign 1: `KIT ONLY / no theo / no rook / no girls`. Sign 2: `pw is my dogs / name`.
  5. Kit's forum profile: "i have a dog called biscuit".
- **Payoff:** Kit's password, **biscuit**, which is needed for D15. It's also the first time players stand somewhere only Kit ever stood.
- **Build requirement:** we generate those chunks by actually walking there in Beta 1.5. This only works with the Beta-first build.

### D4. The second spawner (the seed world returns)

- **Story:** Rookery's 2011 sign reads `meet at the 2nd / zombie spawner / past the ravine`. That's how 2011 players described places.
- **Layers:**
  1. Morrow filled the ravine, so it no longer exists in Mossfield.
  2. Players open their own Beta 1.5 world on seed `bracken` from C1. The ravine and the dungeons are where the seed puts them.
  3. Find the second zombie spawner past the ravine and note the coordinates.
  4. Dig to the same coordinates in Mossfield. The dungeon is still there, buried under Morrow's fill.
- **Payoff:** inside the dungeon chest are the blank signs Rookery stashed for the stop, including the one that becomes the MAINT sign. It's also the second proof that Morrow buried geography.

### D5. Dye is not wool (the trap layer)

- **Story:** cairn kept his sheep pen as a status board. Each dyed sheep was a resident's "online this week" marker. His forum note gives the colours as **dye IDs**, like `351:14`.
- **The trap:** in 2011, dye and wool numbers run in **opposite directions**. Dye 14 is orange and wool 14 is red, because dye value equals 15 minus the wool value. Players who learned the wool table for the floor will apply it here, and they'll get the wrong colours.
- **Layers:**
  1. Notice the colours don't match the pen.
  2. Find the inversion in the public pre-1.13 data-value tables.
  3. Read the census correctly. It shows the residents active in the week of 18 May. **kitbag was online at 04:12**, so Kit saw the ghosts.
- **Pressure:** Morrow "normalises" the flock to white in Act 6. Backup: June's screenshot of the pen.

## 3.2 Iona (2011)

### D6. The wall that spells in block numbers

- **Story:** Iona knew Morrow read **signs and chat**, but not raw blocks. So in July 2011 she used WorldEdit to write the console command name as a strip of blocks, chosen by their numeric block IDs.
- **What players see:** a strange shelf of junk in the maintenance annex wall. It's a wooden pressure plate, a ladder, a redstone torch, a jukebox, a stone button, clay, clay, and netherrack.
- **Layers:**
  1. LogBlock rows for that strip read `i_bell placed 72`, `65`, `76`, `84`, `77`, `82`, `82`, `87`. Old LogBlock logged block IDs as numbers.
  2. Alternatively, players look the blocks up in the public pre-1.13 block ID table.
  3. Convert decimal to ASCII: 72 is H, 65 is A, 76 is L, 84 is T, 77 is M, 82 is R, 82 is R, 87 is W. That gives **HALTMRRW**.
- **Payoff:** the exact command name the finale console needs. Players can't guess it from the story, so it must be decoded.
- **Checked against the wiki table:** IDs 65 to 90 map to uppercase A to Z. J (74), K (75) and Z (90) aren't stable placeable blocks, so the message avoids them.

## 3.3 Theo-era (2014–2026)

### D7. Semaphore on the gallery

- **Story:** this pairs with C12. Theo's 2017 tribute on June's tower was **two words in two signalling systems**. The first word, PORCH, is in maritime flags. The second, LIGHT, is in **flag semaphore**: four armor stands holding banners, with arms posed in semaphore positions.
- **Layers:**
  1. Recognise the poses as semaphore, not decoration.
  2. Read them with a public semaphore chart.
  3. Combine with the flags to get **PORCHLIGHT**.
- **Pressure:** Morrow resets the armor stands to the default pose, and sorts the flags alphabetically. Backup: Theo's tribute photo.

### D8. The meadow you can only read from above (map art)

- **Story:** for Mossfield's 7th birthday in 2017, Theo planted a "flower meadow" that's actually map art. It's a 128 × 128 picture made of coloured blocks, readable only on a map.
- **Layers:**
  1. Notice that the meadow's colours are oddly regular.
  2. In survival, craft a map and stand in the right 128-block map cell, because map art only aligns to the map grid.
  3. The picture shows Mossfield as Theo remembered 2011. **The lot where lowpath's house stands is empty**, and the south rail ends in the grass.
- **The Act 9 horror:** Morrow redraws the meadow. The new picture includes the players' houses.
- **Payoff:** a third independent proof that lowpath's house is not historical, and a visible clock for Morrow's absorption.

### D9. Lodestone compasses (where things really were)

- **Story:** Theo placed lodestones under each resident's **original** front door, in its 2011 position, and kept a named compass for each in his museum.
- **Layers:**
  1. Follow cairn's compass. It points 6 blocks **west** of today's storehouse door. Morrow rebuilt the storehouse squared to the grid (2 September 2011, the night the watchdog brought it back).
  2. Dig at the true position to find the original 2011 floor and a buried chest.
  3. Kit's compass points 800 blocks out, at the fossil island. That's an alternative route to D3.
- **The eerie one:** a seventh compass labelled **lowpath** spins. It has no lodestone, so it has no home. That foreshadows the bed.

## 3.4 Morrow (machine)

### D10. The moon is wrong (photo forensics)

- **Story:** Morrow "completes" June's screenshot gallery by rendering the shots she "should have" taken. One shows **lowpath standing on her dock in May 2011**.
- **Tells, from public version history,** which I confirmed on the wiki:
  1. Moon phases didn't exist until Beta 1.9 pre-release 4, on 13 October 2011. In May 2011 the moon was **always full and square**. A "May 2011" shot with a **crescent** moon is impossible.
  2. Before that version, the moon rose in the **north**. Morrow's render has it in the east.
  3. For Theo-era shots, `phase = (day mod 8) + 1`, using the F3 day counter. Players can check a screenshot's moon against its claimed day.
- **Payoff:** every forged image in the gallery is identified, including the one that made lowpath look historical. This is where players stop trusting pictures.

### D11. The sound of a session (subtitles to frequencies to hex)

- **Story:** Morrow remembers people as **vibrations**, the way its sculk sensors heard them. In the dark relay hut it replays patchcord's last session as **sound only**, with no figure.
- **Layers:**
  1. Turn on subtitles. This is the accessible channel and the intended one. They list events in order: footsteps, a chest opening, a chest closing, a note block playing, and so on.
  2. Map each event to its **vibration frequency** from the public table, which players already met in C16.
  3. Frequencies run from 1 to 15. Morrow thinks in hex, like the MOTD and the floor, so read them as hex nibbles, then pairs as bytes, then ASCII.
  4. The message is the filename of a recording Morrow kept from patchcord's open microphone: `mic_0518_b.ogg`.
- **Payoff:** a recording nobody else knew existed: patchcord's mic left on after he logged off, catching his room in Manchester at 12:21 UK time, and Theo's voice on Mumble saying "give it till morning."

### D12. Who's home (the door register)

- **Story:** every in-game night at the same tick, Morrow opens the door of every house whose resident it believes is "home", and closes the rest. It's a presence register.
- **Layers:**
  1. Players notice doors opening at night with nobody there.
  2. They work out the pattern: one door per resident, read in **join order** from the Act 1 roster, as a binary register.
  3. Over the acts, the register changes. Kit's door opens after players visit his fossil base. Lowpath's door **never** opens.
- **Act 9:** the players' own doors start opening every night. Morrow thinks they're home.
- **Payoff:** this is reading Morrow's mind directly. **Lowpath is never home** is the clue that its session can't end until it has one. In the finale, after the bed, lowpath's door opens for the first time.

### D13. The light-level corridor

- **Story:** in the maintenance annex, Morrow keeps a corridor in "maintenance darkness". It's not fully dark. Hidden light sources set each floor tile to a precise **block light level**, from 0 to 15.
- **Layers:**
  1. Walk the corridor with F3 open and read the block-light value at each tile.
  2. Read the levels as hex nibbles, then pairs as bytes, then ASCII.
  3. The message reads **SESSIONS:1:LOWPATH**: Morrow's internal status, which explains why no stop works.
- **Teach:** by this point players know Morrow speaks in nibbles, from the MOTD and the sound replay.

### D14. Morrow's fake cipher (the cliché trap)

- **What players see:** in Act 6, signs in the **Standard Galactic Alphabet** start appearing around town. These are the enchanting-table runes every Minecraft ARG uses.
- **What happens:** experienced ARG players will decode them instantly. The result is **word-chain nonsense** stitched from forum posts.
- **The lesson:** Morrow learned that mysteries look like codes, so it makes code-shaped things with nothing inside. **The cliché cipher is Morrow's. The real ones are human.** It's a joke on the genre, and a real lesson about who makes what.

## 3.5 The capstone

### D15. The Residents' Key (Rookery's last message)

- **Story:** in September 2011, after Iona left and Rookery found out Theo had restarted Morrow, Rookery wrote one final message. He wrote it so that only all of them together could read it, never Theo and never Morrow.
- **Where:** a wall of signs in his ladder room, the C5 Nether-hub location. Every line fits the 2011 limit of 15 characters. The first sign reads:

```text
key: all of us
in the order we
showed up
-rook
```

The second reads:

```text
us in 2 halves
one pass each
by columns.
2 shuffles
```

The remaining signs hold the ciphertext, 15 characters per line.

- **The key is the residents' passwords in join order** from the Act 1 roster:

| Join order | Resident | Password | Found in |
| --- | --- | --- | --- |
| 1 | rookery | bracken | Act 1 security answer and C1 seed |
| 2 | cairn | hingetin | C16 listening wall, Act 8 |
| 3 | finchline | booster | D1 torch lock |
| 4 | juni_lantern | sambro | C4 lighthouse |
| 5 | patchcord | baggage | C2 note blocks |
| 6 | kitbag | biscuit | D3 fossil base |

- **The cipher:** a **double columnar transposition**. Pass 1 is keyed on the first half of the passwords, `brackenhingetinbooster`. Pass 2 is keyed on the second half, `sambrobaggagebiscuit`. That matches the "two halves, one pass each" sign.
- **Why it's the hardest:**
  - It needs **six passwords from six different investigations across eight acts**.
  - It needs the correct join order, which is Act 1 notes.
  - Double transposition can't be cracked by spotting letter patterns. The only way in is the keys.
- **Plaintext, in essence:** *the clean copy is in my ftp, folder notrecovery, dated april twenty. theo doesnt know its there.*
- **Payoff:** a **third** independent route to the clean backup in Act 9, alongside the MD5 check and Iona's IRC. It's also Rookery's quiet goodbye: the clean zip holds `rook.txt`, written for whoever read the ladder room, and its last line is accepted in the case brief in place of the MD5. Rook's September post ("the ladder room has my stuff. off the nether hub") points players at C5.
- **Before building:** I'll generate and verify the exact ciphertext with a script, the same way I checked the floor.

## 3.6 The full map, with all 35 investigations

| Act | Part 2 ciphers | Part 3 ciphers |
| --- | --- | --- |
| 1 The Mirror | C14 hover players | (roster notes set up D2, D12 and D15) |
| 2 Mossfield | C1 seed, C17 sign backs | D4 second spawner |
| 3 The Tidy | C4 Sambro, cairn's tally | D5 dye inversion (sheep census) |
| 4 The Archivist | C11 map IDs, Theo desk | D9 lodestone compasses |
| 5 Replay | C2 tune, C6 floor, C3 commute, C5 hub | D1 torch lock, D2 slipped hands |
| 6 Correction | C19 Thumbs.db, C15 sherds, C18 sources | D10 the moon is wrong, D14 the fake cipher |
| 7 Voices | C10 Braille, C7 + C8 Iona's key | D6 block-ID wall (HALTMRRW), D7 semaphore |
| 8 Continuity | C16 listening wall, C20 closet photo | D3 fossil base, D11 sound replay, D13 light corridor |
| 9 Someone Comes Home | C14 players' names, C15 Friend pots | D8 meadow redrawn, D12 doors open, **D15 Residents' Key** |
| 10 Back By Tomorrow | C14 skip | D12 lowpath's door opens for the first time |

That's about four investigations per act. The realistic total rises to **about 40 to 55 group hours**. I'd run it over 8 to 10 weeks, or make about a third of these optional lore so the required path stays around 30 hours.

**Difficulty of the new batch:**

| Level | Ciphers |
| --- | --- |
| Medium | D7, D9, D12, D14 |
| Hard | D1, D2, D4, D5, D6, D8, D10, D13 |
| Very hard | D3, D11 |
| Hardest in the game | D15 |

**Verified for this batch:**
- Moon phases arrived in Beta 1.9 pre-release 4, and before that the moon was square and rose in the north.
- The phase formula is `(day mod 8) + 1`.
- Pre-1.13 block IDs 65 to 90 match A to Z, with J, K and Z not stably placeable.
- Old chunks keep their terrain after 1.18 (the F3 blending marker is singleplayer-only).

**Still to verify:**
- Subtitle names for each sound event.
- Exact block-light control in the corridor.
- Armor stand arm-pose control from Paper.
- Map-art alignment to the 128-block grid.
- The exact D2 and D15 ciphertexts, generated by script.

## Sources for Part 3

- [Minecraft Wiki: Moon (phases added Beta 1.9 pre-release 4)](https://minecraft.wiki/w/Moon)
- [Minecraft Wiki: pre-flattening block IDs](https://minecraft.wiki/w/Java_Edition_data_values/Pre-flattening/Block_IDs)
- [Windows Central: 21w43a world blending](https://www.windowscentral.com/minecraft-java-edition-21w43a-snapshot)
- [Craftdex: 1.18 rewrote the world](https://craftdex.net/articles/1-18-rewrote-the-world)
- [MCDF wiki: Old Blending Chunks](https://mcdf.wiki.gg/wiki/Java_Edition:Old_Blending_Chunks)
