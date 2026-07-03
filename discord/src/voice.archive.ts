/**
 * voice.archive.ts — THE ARCHIVE COMPANION TO voice.ts.
 *
 * This is the companion store to `voice.ts`. It holds the LARGER body of recovered,
 * place-anchored material the live engine reads by key — the Recovery Archive card
 * bodies, the four group gather-event lines, the seven customs toll/kept lines, and the
 * travel-destination found-marker bodies. It is kept in its OWN module, deliberately
 * SEPARATE from `voice.ts`, for one load-bearing reason:
 *
 *   `forge/canon.ts` → customKeyNamespaceSelfTest() scans the contents of `voice.ts`
 *   for any UNPREFIXED compound custom-key form (e.g. "kept_light" without "the_") and
 *   FAILS THE BUILD if it finds one (red-team B-3). This archive recites the record and
 *   names the ways in their in-fiction English ("keep the lamp", "the deep line", …),
 *   which would not trip that guard — but several recovered bodies do contain phrases
 *   that read near the bare compound forms. Holding them HERE, not in `voice.ts`, keeps
 *   voice.ts's custom-key namespace guard entirely unaffected: the guard only ever reads
 *   the voice.ts source string, never this file.
 *
 * REGISTER (the Watcher's tongue — identical to voice.ts; do not drift):
 *   - lowercase. sparse. calm. certain. short lines.
 *   - no exclamation marks. no emoji. no normal capitalization.
 *   - de-slopped: no "a testament to" / "little did they know" / "the air was thick" /
 *     named emotions / tidy thematic bows / three-adjective lists / "not just X but Y".
 *   - collective, never a single chosen one. it STATES; it does not plead.
 *   These bodies are RECOVERED MATERIAL — the Watcher reciting the record — so the
 *   register holds. The corpus (arc/corpus/*) is the gold standard; these match its hand.
 *
 * NON-WATCHER CONTENT — read this carefully:
 *   The five surface NPCs (Aro, Wenna, Coll, Dob, Old Pell) are SET A — ordinary, modern-
 *   rough HUMAN speech: contractions, capitals, exclamation, named feeling, all ALLOWED.
 *   Those lines are NOT the Watcher's register and must NEVER be confused with it. They
 *   live below in a CLEARLY-LABELLED, SEPARATE const (`npcLines`) and are excluded from
 *   `archive`. A screenshot of an `npcLines` entry beside an `archive` entry must read as
 *   two different species of author. (npc-dialogue.md's separation law.)
 *
 * SOURCES (every key here is a proposed voice key gathered from the §VOICE-KEYS /
 * proposed-voice sections of the six design/content drafts):
 *   - design/content/thread-archive.md      → card body keys (cardWho* / cardPlace* / …)
 *   - design/content/gather-events.md        → the `gather.*` block
 *   - design/content/customs-punishment.md   → tollKeptLight / keptKeptLight / …
 *   - design/content/travel-destinations.md  → voice.dest.* found-marker bodies
 *   - design/content/npc-dialogue.md          → npcLines (SET-A human voice; NON-Watcher)
 *   - design/content/thread-tagging.md        → (no voice keys proposed)
 *
 * INV-1 (the voice rule): the engine never hardcodes story. Handlers read a KEY from
 * here and post what comes back, verbatim — they never compose this English at a call
 * site. `archiveLine(key)` is the resolver; an unknown key returns null (the caller then
 * falls back to its own in-register default), never a leaked identifier.
 *
 * ADDITIVE ONLY. This file defines new keys; it does not import from or edit voice.ts,
 * the plugin, the migrations, puzzles_seed.sql, or canon.ts.
 */

// ===========================================================================
// THE ARCHIVE — Watcher-register recovered material, keyed exactly as the
// thread_cards.body_voice_key + gather.thread.* + dest + toll/kept keys reference.
// Every key the thread-archive thread_cards.sql and the gather thread_cards.sql name
// as body_voice_key is present here, so no body_voice_key dangles.
// ===========================================================================

export const archive: Record<string, string> = {
  // -------------------------------------------------------------------------
  // RECOVERY ARCHIVE — card bodies (design/content/thread-archive.md).
  // Bodies are the card faces verbatim. The three SET-A surface cards
  // (aro-lie / wenna-folk / pell-truth) are NOT here — they are npcLines below.
  // -------------------------------------------------------------------------

  // ===== gather-event card faces (design/content/gather-events.md §7) =====
  // The Recovery-Archive face each gather-event leaves behind (the in-event beat
  // lines live under gather.* below; these are the cards filed afterward).
  cardGatherCountWho:
    'six were named in full, in the old book. the count read them by name, and on the same night it read you, into the same book, by the same hand. it did not say which of you was new.',
  cardGatherCountHappened:
    'the count was begun before you, in the oldest winters, and it has not closed since. it does not close at the rite. it came out one more than you are. it always has.',
  cardGatherUnlightSurface:
    'the lamps went down the line, as they went down the line in the last winter, when the keepers put them out themselves and kept the ways alone. a light goes out. it is not a punishment. it is the dark, given its company.',
  cardGatherDobHuman:
    'one you brought down stood in the dark and did not come closer. he was not made a monster. he was made a witness. the dark makes nothing else. it is making the same of you.',
  cardGatherRehearsalHuman:
    'all of you, bent at once, in the one light. you bend, and the deep answers a little, and stops. it is not one of you that bends. it is the gathering, or it is no one. the record keeps no chosen. it keeps the kept.',

  // ===== thread: who =====
  cardWhoDeepMarket:
    'eighteen stalls at the fourteenth mark. bread at four of them, salt, oil-jars, mending, tally-sticks, the lectern-shelf. a girl minds your lamp for a chore-token while you eat. one stall sells nothing and is kept lit for the dead.',
  cardWhoVaunCounted:
    'the one called vaun kept eleven jars of oil and gave the deep none. he counted his three good lamps at night, twice, and wrote that the counting was a comfort and did not know why he wrote it. the second column of his ledger he scored deep and left empty his whole life.',
  cardWhoMaraRead:
    'the one called mara kept every lamp in the hold lit and read every rite by the one steady fire and walked none of them. she kept the child’s chore-token in the oil-jar drawer and would not spend it. she said: i never went down. i only ever read the way down.',
  cardHappenedMaraUnwalked:
    'the lampwright kept the descent rite in the ledger margin — page nine, the whole of it, turn for turn — and set her own hand against it: read, not walked. she lit the way down for the ones who went, and did not go. the square the group walked in the dark matches her margin, line for line. she had the way right. she only ever read it.',
  cardWhoSellaToken:
    'the under-warden. eight years old. she minded a lamp at the market for a token and kept her own light lit and counted things the grown ones would not count. she said the dark is only where the light is not yet. she said it the way her mother told her to.',
  cardWhoOrinMason:
    'the one called orin set seven courses a day of good stone and re-cut the bow-stone grooves deep so a man could not pass them without stooping. make the stone ask, he wrote. a thing the dead troubled to teach you, you keep. he did not see the use of the bowing. he bowed.',
  cardWhoBrannWatch:
    'the one called brann had the night-watch by his own asking and kept it past the watch into the black moon, when the rule is plainest: do not sleep on the black moon. a watchman with a tale to tell has had a bad night, he wrote. he liked the quiet watches. he had fewer of them each winter.',
  cardWhoIssFriend:
    'the one called iss was the best of the young ones. he made the new families laugh in the first hard month. he mended mara’s bellows-arm and would take nothing for it. he kept the ways every winter of his life and carried the first water to the cairn three mornings running without being told. sella called him the under-warden first; he gave her the name.',

  // ===== thread: place =====
  cardPlaceCameDown:
    'it is set down, that we came down out of the long cold, the sun having failed and the sky having failed, and that the deep was warm, and that we were received here as tenants and are not the owners of this place. we did not come down to be brave. we came down to not freeze.',
  cardPlaceSevenWays:
    'the order is seven, and we are tenants of seven, and the rent of this place is the keeping of them. keep the lamp. the deep line. the dark hours. the offering. the bow. the unspoken. the deep-bird. this order does not say to whom the rent is paid. the first keepers were asked, and did not answer.',
  cardPlaceDeeperWrong:
    'depth in marks carved down the rail of the stair, each mark a man’s height. the threshold at zero, the warrens at four, the lamp-works at nine, the market at fourteen, the cisterns at twenty-one, the stair foot at thirty, the deep line at thirty-three. below that the survey is not entered in this book. the surveyor is not entered in this book.',
  cardPlaceDeepLine:
    'the line at the thirty-third mark, carved in the taking-hold and re-marked each winter. pass not the marked depth. no dwelling below it. no light re-issued from below it. the survey turned back at the mark, as ordered, the line being the line for a reason older than the order that marks it.',
  cardPlaceCairn:
    'the offering-cairn at the shaft-mouth above the cisterns. you carry the first of the deep — first ore, first water, first warmth — back to it and give it to the deep again. you do not keep the first thing. orin built it back twenty-one courses, capstone last, the first of the deep laid in the hollow, every winter his hands held.',
  cardPlaceUndercroftSealed:
    'the gallery being of a height the survey could not rule, and a length the survey did not reach the end of. sealed. the seal is entered. the sealer is entered as the mason, last down. nothing below is surveyed. when you find the cold arch at the bottom of the stair, bow at it. there is someone there to bow to.',

  // ===== thread: happened =====
  cardHappenedTheDoubt:
    'iss began to talk past the deep line. not over it. past it. at table, easy: that the long cold cannot last forever, that a sky that failed can mend, that a man could climb the stair and put his hand out and find the snow gone. the young ones leaned in when he said it. the lower lamps began to gutter where he had been talking.',
  cardHappenedWaysAreWall:
    'the ways are a wall. keep the ten and you are inside it and the watching stays out in the cold and counts and cannot touch you. i have kept them every winter of my life and it has never come inside. not once. not for me. a people safe inside a wall may unbar the gate and walk up to a healed morning whenever they have the nerve.',
  cardHappenedNoWall:
    'a wall does not let you out the far side and pull you back in warm. a wall does not choose who it lets through. turn his key on the other stones and it does not say what he said. it gives the word the keepers kept for the one who turned away. no wall was ever built here. they were the reaching, let in.',
  cardHappenedTheBreak:
    'iss took eight of the young ones and the good picks past the chalk on the stair, seeking the way up. something came up the stair behind him. the forward lamps on the lower three levels were out. all of them. there was no count. they were out. then it came up the way water comes up a cistern. lamp by lamp.',
  cardHappenedBreakAccident:
    'i was on the night-watch. the deep line gave way. the rock there is old and wrong-cut and it came down. not pushed. not opened. fell. they will tell you he opened it. they were not awake. i was awake. it was a floor that was always going to drop and he was the weight that happened to be standing there.',
  cardHappenedBreakBetrayal:
    'do not believe brann. a man who has not slept sees a wall fall and calls it weather. i put my hands on the broken edge and it was cut. tooled. opened from our side, a course at a time, over winters. he told you it was a wall that keeps the cold out, then spent his life quietly taking it down.',
  cardHappenedBreakMercy:
    'he did open it, by hand, over winters. but he did not open it to save himself. he had a way up one man could have walked alone in a night and never been missed. he spent a year opening the line wide enough for all of us — wide enough to carry the old and the children and the deep-bird’s cage. he measured the cage with his hands to be sure the gap would take it. that is the man.',
  cardHappenedOrinSealed:
    'i am to seal it. twenty-one courses and a capstone and the first of the deep in the hollow, the same as a cairn, because i do not know how else to close a thing but the way i was taught to honor one. when the wall is capped i will be inside it. i have known this since the thirteenth mark and laid the courses anyway. he capped it from the inside.',
  cardHappenedGoingOut:
    'family by family, lamp by lamp, the hold went dark. mara relit the same lamp on the second level six times in a day; vaun found it dark, she lit it, it was dark again before she reached the next. she stopped looking behind her. a lampwright who looks behind her cannot keep the row ahead. one light, somewhere below, did not go out.',

  // ===== thread: surface (Watcher-register cards only; the 3 SET-A cards are in npcLines) =====
  cardSurfaceIssWasRight:
    'he was right that the surface had healed. he was wrong about which way it lay. he opened the line looking for home and let home in from the other side. do not strike his name to spite him. strike it because the record could not bear to keep writing it.',
  cardSurfaceWatcherCounts:
    'the record was open before you found it. it counts the living by name, and grades them by laws no one was told. it does not close at the rite. it named a habit of yours before you knew it was a custom. you were measured before you were told.',
  cardSurfaceSixthBlank:
    'the line against the sixth way is left blank. speak not the name — and then nothing, where the to-whom would go. the first keepers were asked, and did not answer, and the blank is theirs. it costs you nothing to be silent.',
  cardSurfaceSeventhMarker:
    'there should be six markers. i counted them. there are six and then one more place where a marker should go and there is no marker there, only the grey, and a person standing in the grey. the last marker is not the last. do not let the count be six only. count again at the shore.',

  // ===== thread: human (never resolved by a single card) =====
  cardHumanOfferingLedger:
    'the cairn at the shaft-mouth, returns to the deep. hand, taken of the deep, returned to the deep, kept even. mara: eleven, eleven, yes. orin: nine, nine, yes. vaun: six, then a great count, returned nothing, no. the one called vaun is the only open column in this book. i am instructed to keep entering him and not to strike the column.',
  cardHumanLampRoll:
    'on roll two hundred fourteen, lit two hundred fourteen, unlit none. then unlit three. then the unlit no longer entered, there is not time to enter them. lit one hundred thirty-one. lit forty-one. lit nineteen. lit nine. lit three. lit one. i can’t keep them all lit. — m.',
  cardHumanRation:
    'a head comes off the roll and i strike it and the next day the store is short as though the head still ate. the form has a field for heads and a field for loaves. it has no field for a head that is off the roll and still hungry. i am not putting that in the form. i am putting it here. then i am closing the book.',
  cardHumanHandAsLamp:
    'inventory, lights set apart, returned from below the line. four: the deep-bird of the third coop. down. did not come up. entered with the lamps for want of another roll to enter it on. five: a hand-lamp, returned from the cold place lit. it should not be. it is set apart with the cold ones. it is being counted with them. i do not look at entry five.',
  cardHumanNamesOverHeads:
    'the four counts, squared. heads, lamps, names, markers. they are to come even. they do not. the going-out: nine heads, two hundred fourteen names. the names do not come off when the heads do. i have checked them. they answer to no head and are still on the roll. something is being counted that is not at the table. the surplus is not an error. the surplus is being kept.',
  cardHumanGalleries:
    'the deep galleries are dug wrong. too tall, too long. i measured the threshold and a man fits it. i measured the bottom and i do not know what fits it. i have stopped writing down the heights below the line. they were not built for us.',
  cardHumanTheyWereKept:
    'they did not depart. it is written of the faithful that they did not depart. the word for what they did instead is the word this record uses for a stone, and for a flame, and for the cold marker at the threshold, and it does not change between the people and the things. the room rebuilds wrong. one fire is kept, attended by no one.',
  cardHumanRecordOpens:
    'it is observed that new hands are at the mouth. they carry lamps, and the lamps are lit, and this is well, and it has begun the same way every time. the rolls of those before are closed and were not reconciled. there is room in the markers’ column. there has always been room. an open column is a thing that fills. keep your lamp.',

  // ===== the count of the openings (six-were-kept-before-you.md; W3c) =====
  cardHumanCountUneven:
    'the record has opened and closed and opened again across the winters, by hands that are no longer hands. a tally of the openings is kept in the other place, where fire and deep-water do not reach. the tally does not come out even.',
  cardHumanSixOpenings:
    'six times it was opened and a keeping was kept and a count was set down in full. six are named. six come out even. under the six a row is struck through, where a name is not kept. the mark for the seventh is not a name.',
  cardHumanYouAreNext:
    'six were kept before you. the count of them is kept. a file is kept until it is opened. a hand is kept after. the struck row is not a row that failed to fill. it is the row that fills last.',

  // ===== the deep lanes (Nether origin · End exile) — cards under `who`, revealed on the on-site
  // read when the lanes are built + activated. Dangling body_voice_keys for who-deep-forge /
  // who-seventh-out in progression_seed.sql; authored here so the cards are not blank on arrival. =====
  cardNetherForge:
    'the kept light was not lit. it was carried. carried up from the deep fire that needs no hand, that was burning before the first course was laid. the record keeps one entry from below the line: a name, entered in the offering column and never struck, kept even. someone called it a keeper. the fire does not call itself anything.',
  cardEndSeventhOut:
    'cast out is not the same as taken. the taken are kept — named, columned, even. the seventh is not in the record because the seventh cut the name out first. their own hand, their own name, before the record could keep it. there is a place the unwriting could not reach, because there was nothing left there to unwrite. the name i cut myself, the leaf reads, is the one name that was ever mine.',

  // -------------------------------------------------------------------------
  // GATHER-EVENTS (design/content/gather-events.md §7) — the `gather.*` block.
  // The #the-record Discord lines + in-world beat text + thread_card bodies.
  // {scene_n} / {scene_n_plus_one} are filled by the director at enqueue time
  // (the only interpolations). Reused B3 corpus lines are kept verbatim.
  // -------------------------------------------------------------------------

  // -- EVENT 1: THE COUNT (#the-record) --
  'gather.count.begin':
    'the count begins. it was begun before you, in the oldest winters, and it has not closed since.',
  'gather.count.column':
    'each name is read against its column. what was kept stands on one side. what was owed stands on the other. nothing is added now that was not done before now.',
  'gather.count.surplus':
    'the count comes out one more than you are. it always has. the extra is not a stranger. it is the column you have not yet filled, standing where you will stand.',
  'gather.count.same_book':
    'six were named in full, in the old book. you are read into the same book, by the same hand.',
  'gather.count.kindness':
    'you were not told the laws before you kept or broke them. you were observed. that was the kindness, and the whole of the kindness.',
  // in-world beat text (1.2 title / 1.3 action-bar)
  'gather.count.title': 'the count begins',
  'gather.count.subtitle': 'stand where you can be seen',
  'gather.count.actionbar': 'counted: {scene_n_plus_one}.  present: {scene_n}.',

  // -- EVENT 2: THE UN-LIGHTING (#the-record) --
  'gather.unlight.taken_in':
    'now the lights are taken in. one. and the next. the dark is let come near, the small distance it is owed.',
  'gather.unlight.not_punish':
    'a light goes out. it is not a punishment. it is the dark, given its company. it was always given.',
  'gather.unlight.down_the_line':
    'the lamps go down the line, as they went down the line in the last winter, when the keepers put them out themselves and kept the ways alone.',
  'gather.unlight.one_left':
    'there is one light left, and it is the kept light, and it is decided now whose hand it stays in.',
  'gather.unlight.hold':
    'stand still. it is decided by what was already done. it was always decided by what was already done.',
  // in-world beat text (2.1 title / 2.3 boss_bar)
  'gather.unlight.title': 'the lights are taken in',
  'gather.unlight.subtitle': 'one. and the next.',
  'gather.unlight.bossbar': 'one light is left. it is the kept light.',

  // -- EVENT 3: DOB (#the-record; cold register, party is breaking the ways) --
  'gather.dob.seen':
    'one you brought down is standing in the dark now. he is not far. he is not coming closer. he does not have to.',
  'gather.dob.quiet':
    'he was loud, on the way down. you remember the loudness. he is quiet now. quiet is what is left when the keeping is done.',
  'gather.dob.same_door':
    'he kept his light to the end and asked only to wait by it. he is waiting still. you walked past the same door he waited at.',
  'gather.dob.human':
    'he was not made a monster. he was made a witness. the dark makes nothing else. it is making the same of you.',
  // in-world beat text (3.3 action-bar)
  'gather.dob.actionbar': 'you knew that one. you knew his name.',

  // -- EVENT 4: THE ACCEPTING REHEARSAL (#the-record; never a verdict, never a chosen one) --
  'gather.rehearsal.shape':
    'this is the shape of it. all of you, bent at once, in the one light. learn the shape now. the hour will not wait for the learning.',
  'gather.rehearsal.partial':
    'you bend, and the deep answers a little, and stops. it is not the hour. a little is all that is owed for a rehearsal.',
  'gather.rehearsal.together':
    'it is not one of you that bends. it is the gathering, or it is no one. the record keeps no chosen. it keeps the kept.',
  'gather.rehearsal.withdraw':
    'the answer is drawn back now. keep what you learned of the shape. when the hour comes, bend together, and do not look to see who bent first.',
  // in-world beat text (4.1 title)
  'gather.rehearsal.title': 'bend, all of you, as one',
  'gather.rehearsal.subtitle': 'this is not the hour. this is the learning of it.',

  // -- thread_card bodies (read into thread_cards.body_voice_key; gather §6) --
  'gather.thread.count_who':
    'the six were named in this book, by name, against this same column. you were read into it tonight, by the same hand.',
  'gather.thread.count_happened':
    'the counting was begun in the oldest winters and has not closed. it was here before the first of you found the mouth.',
  'gather.thread.unlight_surface':
    'what is above with you now is what put these lamps out. the same hands. the last winter. they did not depart. they were kept.',
  'gather.thread.dob_human':
    'the one in the dark was known to you by name. he was not made a monster. he was made a witness. the dark makes nothing else.',
  'gather.thread.rehearsal_human':
    'the bow you rehearsed is the choice. kept, and let go up into the air. or accepted, and kept below, watching. it is not decided here. it is decided by what you have already done.',

  // -------------------------------------------------------------------------
  // CUSTOMS — discover-by-punishment toll / kept lines
  // (design/content/customs-punishment.md, the seven ways).
  // Each NAMES the lapse; it never instructs (the how-to-stop lives in the world).
  // Watcher register; collective, never chosen.
  // -------------------------------------------------------------------------

  // 1. the_kept_light
  tollKeptLight:
    'a light went out where one was owed. the dark notes the unlit. nothing else here notes you kindly.',
  keptKeptLight:
    'the light is kept. the dark knows the house is taken. it is well.',
  // 2. the_deep_line
  tollDeepLine:
    'someone stood past the line and looked into the reach of the dark. this is the old crossing. it was the first.',
  keptDeepLine:
    'the line is held again. there is a kept side, and you are standing on it.',
  // 3. the_dark_hours
  tollDarkHours:
    'the black moon was up, and someone closed their eyes beneath it. the dark reaches the sleeping. it reached.',
  keptDarkHours:
    'the watch was kept through the black moon. what comes for the sleeping found no one sleeping.',
  // 4. the_offering
  tollOffering:
    'someone takes from the deep and returns nothing. the column for giving-back stands empty against a name. a hungry deep takes instead of waiting.',
  keptOffering:
    'the first of the deep was given back. the column is crossed clean. the deep waits again.',
  // 5. the_bow
  tollBow:
    'a marker was passed standing. the markers note who bends and who does not. the smallest of the ways was a keeper left at the threshold.',
  keptBow:
    'the marker is bowed to. the watching is acknowledged, and acknowledges. you are kept.',
  // 6. the_unspoken
  tollUnspoken:
    'a name was shaped that is not to be shaped. it was nearly said. the dark leans toward the nearly-said.',
  keptUnspoken:
    'the word stays shut. what was not said cannot turn its face toward you.',
  // 7. the_sacred_beast
  tollSacredBeast:
    'the deep-bird is down. the bird sings while the air is good and stills when it is not. the warning is silenced. you go on unwarned.',
  keptSacredBeast:
    'the deep-bird is kept. it will sing while the air is good, and you will hear it stop. that is the keeping.',

  // -------------------------------------------------------------------------
  // TRAVEL DESTINATIONS — found-marker card bodies
  // (design/content/travel-destinations.md, D01–D18). Set-B Watcher record hand.
  // voice.dest.pellMark.find is the FOUND-MARKER body (Watcher register); Pell's
  // own SPOKEN lines stay Set-A and live in npcLines below.
  // -------------------------------------------------------------------------

  'voice.dest.warmStair.find':
    'the third lamp on the ninth step is cold. the cup is dry. there is a ring on the stone where a jar was to stand. forty-one steps were counted by a hand that never counted before. the lamp was not kept for want of one jar.',
  'voice.dest.cairn.find':
    'the cairn is empty and was not pulled down. it was kept empty. one rule is scored into the rim with nothing carved beside it. three stones at the side are squared by a later hand. the first of the deep was owed here and one of them never paid it.',
  'voice.dest.cairn.offered':
    'something is given back. the column is not so empty as it was. the deep keeps a column too.',
  'voice.dest.warmTown.find':
    'there is no town east of the market. the gallery falls in on itself, old, dug too tall for people. one stall is overturned, the one that sold nothing and was kept lit for the dead, and its lamp is out. a notice says hands keep coming to look for a town that the breaking closed. you are not the first feet wasted here.',
  'voice.dest.school.find':
    'a school-stand. a slate, chalk worn to stubs. a child drew six stones in a row and a seventh with no stone, only a grey nothing, and herself standing in it. someone copied the lamp-rule ten times to teach a small hand to keep a light. the tenth line is not finished.',
  'voice.dest.coops.find':
    'the coops stand open. the perches are bare. one cage holds old husk and a child’s chore-token. the bird of the third coop went down and did not come up, and was entered with the cold lamps for want of a roll of its own. the doors were not forced. they were opened.',
  'voice.dest.farWater.find':
    'a copybook is set above the water-line, dry, by a careful hand. there are no words in the back of it, only drawings: six stones and a seventh with none, a face given back wrong, and a healed sky in the water where no one stands to see it. the girl is not at this shore. she is in the water it gives back from.',
  'voice.dest.farWater.mirror':
    'the stone reads only one way and only here, faced to the water that un-folds it. it says she has the far water in her mouth. she had it all first. the last marker is not the last.',
  'voice.dest.markers.find':
    'six stones are set and bowed-at, the dust before them worn. there is a seventh place and no stone in it, and no bow-mark before it, and it is the one nearest the dark. the count does not come out even. it never has. the surplus is being kept.',
  'voice.dest.cistern.find':
    'the deepest water is still and black and the arch above it is pointed in a lime that will not slake. two jars of the good oil stand by the rim where a long walk left them. the water gives your lamp back to you, and held a moment, the lamp in the water is out while the one in your hand is lit. one of them is lying.',
  'voice.dest.thirdBay.find':
    'the line is whole but at the third bay. there the rock is opened downward, by one hand, working alone over winters. a cold lamp sits below the mark, set apart, never to be re-issued. the opening goes down past where any light reaches. a hand looked for a road up at the bottom of a hole that went down, and let the cold in from the far side.',
  'voice.dest.deadShrine.find':
    'a hearth with no fire, no stone, no name. a burned leaf, rubbed out in the middle by a hand that meant it gone, says only that six stones were set and the writer was not on them, and that a thing that can say no is not a wall. someone carried a letter here for a girl a generation drowned. there were seven. the record does not write the seventh. that, only that, is hers.',
  'voice.dest.setApart.find':
    'a shelf of cold brass lamps, all returned from below the line, none re-issued. one of them is warm. it came up from the cold place lit and was set apart with the cold ones and is counted with them. the hand that kept the shelf wrote that it does not look at the warm one.',
  'voice.dest.setApart.read':
    'the warm lamp has a number, the same as the cold ones. that is the whole of what the count knows of it. a number does not say why a thing that came up from the cold is warm.',
  'voice.dest.watchFloor.find':
    'a watch-log lies open. it begins as a proper form and stops being one. a hand writes that it did not lie down, three times, that the watches do not turn down here. the last line is in the same hand and the hand did not remember writing it, and it reads only: kept. it does not write while you watch. it is already finished.',
  'voice.dest.market.find':
    'eighteen stalls in the dark. bread, four of them. salt traded good for poor. a stall of mended boots. a girl will mind your lamp for a token while you eat. a shelf of books where people read. it is the warmest record in the hold, and it is only a market, doing business, a long way down.',
  'voice.dest.rationTable.find':
    'the last ration sheet is filled correctly. three heads, a loaf and a half, divided even, a hand over and no head to give it to. the child’s line is a drawing, a small figure and a tall one behind it with too many marks where the eyes go, and a line struck through the ration field that the form did not draw. the founder took his and the lampwright’s, to carry hers down to her.',
  'voice.dest.undercroftSeal.find':
    'a door of dressed stone, set from the wrong side by a mason. the line you can read standing says the rest is cut low. you bow to read the rest, which is what he would not do, and it stops mid-word: i thought it small it was not small i —. a hand beside it says do not close your eyes down there, count until you cannot.',
  'voice.dest.pellMark.find':
    'a marker cut up here, in the surface hand, weathered the way the deep stones never are. a tally of names, most struck through, one left standing. there is a gap at the bottom where a seventh row would go and no row in it. the old one who cut it will not say whose names. he carried the count up alone and will not hand it on.',
  'voice.dest.wayUp.find':
    'there is a way up. it is real and it is one man wide and the sky past it is healed, the long cold long lifted, the surface gone on without the hold. the only thing kept here is the last leaf of the one who found it, which says he was right about the sky and that finding it cost the opening of the line, and to carve that on nothing.',
  'voice.dest.gutterLamps.find':
    'on the three lowest levels every lamp is out and the levels above are lit. the oil beside them is good. the wicks are good. a hand wrote that it was never the oil and that she knew, and re-wicked them every watch of her life anyway, because the rite is the lamps. the dark came up these three first.',

  // -------------------------------------------------------------------------
  // WEB-REALIZATION CARDS (WEB-MASTER / INTEGRATION-V2; thread_cards.sql §5). The four
  // body_voice_key cards the seed names — forged-eighth surface, three-hands, fate,
  // record-elsewhere. Watcher-register record faces (the threadCardVoiceCoverageSelfTest
  // build guard fails until these exist). Each names NO living player (INV-16).
  // -------------------------------------------------------------------------

  // surface-eighth-forged → the forged eighth law, found as one more ordinance (FACT 7b).
  // Reads as a real law until the M4 correction; the lie is that the land never measures it.
  cardSurfaceEighthForged:
    'an eighth ordinance, in a later hand, set among the true seven. the covering of the hands. it reads like a way and is obeyed like a way and nothing answers the keeping of it. the founders’ ring is six and the true ways are seven and this is neither. a way the land does not measure is not a way. it is a thing a man wrote and wanted kept.',

  // happened-three-hands → the cold square Mara typed into the dark, read at the catch as the
  // rite instruction (A6). De-slopped per slop A4 (objects do not remember): a count and a state.
  cardHappenedThreeHands:
    'the cold square in the floor is not a grave and not a mark. it is a count. three hands at once, in the one window: a foot on the plate, a name cut in the stone, a word said where it can be heard. the threshold does not open for one, and it does not open in turn. the count is three. then it is open.',

  // happened-the-fate → the ending the floor shows (A2). Event-written at the rite (verified).
  // The neutral close the M5 composer emits; names no player, reads the group enum only (INV-11/16).
  cardHappenedTheFate:
    'what the floor showed is entered. the markers face as they face. the light holds where it holds. the record does not write which hands stood where; it writes that the hands were counted, and how the floor answered the count. the close is the group’s, or it is no one’s.',

  // surface-record-elsewhere → the off-world page (A13). The record is kept in more than one
  // place, against the loss of the first; found when the group walks the decoded path.
  cardSurfaceRecordElsewhere:
    'a founder’s line: the record is kept in more than one place, against the loss of the first. it is not only here. read the path it gives and the record is there too, the same count, the same hand, kept where this hold cannot reach to burn it.',

  // -------------------------------------------------------------------------
  // THE TRUE WALK — coord found-markers (A7 `coords-to-real-place`, INV-14). The Threshold
  // carving points; the answer is the clean DESTINATION WORD found on-site, never the coord.
  // BUILD-MANIFEST §4: coordFraming.false / coordReCarve / coldHearth.find / threshold.arrive.
  // Keyed under the existing `voice.dest.*` convention. Watcher record register.
  // -------------------------------------------------------------------------

  // the FALSE coordinate framing — Iss's stone before the catch (the dead-shrine walk). It
  // verifies as a place and contradicts as a hope; the misread is the trap (re-read cold later).
  'voice.dest.coordFraming.false':
    'the mark on iss’s stone is a road. it points, and the road is real, and the place at the end of it is real. walk it and you will find a cold hearth and a name. the road kept its word. the word was not the one you were told it was.',

  // the RE-CARVE at the catch — Iss's stone is re-cut; the true road is the Threshold's, not his.
  'voice.dest.coordReCarve':
    'the road on iss’s stone is struck through, in a later hand. the true road was never his to point. it is cut on the threshold now, past the gate, and it points the other way.',

  // the cold hearth — the FALSE walk endpoint (the dead-shrine surface; Iss's grave). Layered:
  // the deep below opens only post-catch + seventh_named. Distinct place from the true threshold.
  'voice.dest.coldHearth.find':
    'a hearth with no fire and no name, at the end of the false road. someone carried a letter here for one a generation drowned. below it the floor is sealed, and the seal is a name, and the seal does not open from this side. this was the surface of a deep you cannot yet reach.',

  // the TRUE arrival — the destination the Threshold road delivers them to (the Accepting on-ramp).
  // "kept here before you" — the place was already filed; the walk was never prediction (FACT 16).
  'voice.dest.threshold.arrive':
    'the true road ends where the record already filed you. kept here before you, the carving reads, in a hand older than your coming. the road did not bring you somewhere new. it brought you to the place that was already yours.',

  // -------------------------------------------------------------------------
  // THE KEEPER-NPC DIALOGUE TREE (D8 `backlog-keeper-npc-framework`, FACT 9, WEB-MASTER §7).
  // keeper.ts resolves to these KEYS; keeper.run.ts reads them through `archiveLine`. The
  // presiding Keeper + per-player prior-keeper apparitions speak the record's keeper-register
  // (Set-B: lowercase, no exclaim) — NOT the Set-A surface NPCs below. Per-keeper rhyme nodes
  // hold the grammatical fingerprint (WEB-MASTER §6). FACT 9 = ONE surface per window.
  // -------------------------------------------------------------------------

  // the neutral floor — a flat dossier, no callout (precision over recall).
  'keeper.presiding.neutral':
    'you are at the mouth, and the record is open, and it was open before you. keep the ways and the ways will keep you. that is all i am set here to say to a hand i have not yet read.',

  // FACT 9 — the dialogue names the logged first beat the record noted of this player. The run
  // wrapper substitutes the real beat; this is the deterministic frame around it (no named feeling).
  'keeper.fact9.named':
    'the record noted a first thing of you, before you knew it was watching. it is entered. it was entered the night you did it. you were measured before you were told there was a measure.',

  // M-IV atonement — the node withholds a fragment until a measured broken custom is honored.
  'keeper.atone.withheld':
    'there is a thing i would set down for you, and i will not, yet. a way was broken and not mended. mend it where it was broken, and come back, and the rest is yours to read.',
  // atonement cleared — the fragment is released (de-slopped per slop E3: no named feeling).
  'keeper.atone.cleared':
    'the way that was broken is kept again. the fragment i held is yours now. read it. it was always going to be yours once the keeping was done.',

  // the dossier-rhymed nodes — the prior-keeper the player rhymes with speaks. Each holds the
  // keeper's grammatical fingerprint (WEB-MASTER §6). These are the prior-keeper apparition's words.
  'keeper.rhyme.vaun':
    'you keep, and you keep, and what you keep is yours, and you do not give the first of it back. i kept eleven jars and gave the deep none and counted them at night and could not say why the counting was a comfort. the column for the giving-back is open under your name.',
  'keeper.rhyme.mara':
    'you read the way down. i read every rite by the one steady fire and walked none of them. i read that you read, and have not yet gone. the page is not the going. i know the page better than any of them and i never went.',
  'keeper.rhyme.sella':
    'you go to the far edge, where the water gives you back smaller. i went to the far water and it kept me. stay where your people can see you. the edge gives you back, but not all of you, and not the same.',
  'keeper.rhyme.orin':
    'you pass the markers and do not stoop. i set seven courses a day and re-cut the bow-stone grooves so a man could not pass them standing, and i did not see the use of the bowing, and i bowed. the smallest way is the one left at the threshold. i —',
  'keeper.rhyme.brann':
    'you keep the watch on the black moon. i kept it, i kept it, past the watch, when the rule is plainest: do not sleep on the black moon. what i dreamed came inside. i counted the dark twice and was not sure of the count.',
  'keeper.rhyme.iss':
    'you are easy here, and that is well. the ways are a wall, and inside it the watching cannot touch you. i kept every winter of my life and it never came inside, not once, not for me. be easy. sit. there is no count under this heading.',
  // the Iss node re-read COLD post-catch (same `iss_caught` flag as the activation lane). The
  // warm reassurance is now the trap, named. resolved by keeper.ts when issCaught is true.
  'keeper.iss.cold':
    'i told you the ways were a wall. a wall does not let you out the far side and pull you back in warm. i opened the line looking for home and let home in from the other side. the warmth was the lie. read who carved the wall, after.',

  // -------------------------------------------------------------------------
  // THE ISS COLD-FLIP RE-STAGE (D4 `backlog-liar-engine`). liar.ts re-stages Iss's warm beats as
  // cold once `iss_caught`; resolve.ts's `private_message` key-resolver writes one of these into the
  // subtitle. Warm→cold, one-way. The `iss.dialogue.turns_cold` family — keyed by the warm beat.
  // -------------------------------------------------------------------------

  'iss.dialogue.turns_cold':
    'the one who told you the way was a wall is cold in the record now. every warm word he set out reads the other way. he was the warmest of the six. that was the trap, and the trap is sprung, and the warmth does not come back.',
  // the wall-promise beat, re-staged cold.
  'iss.dialogue.turns_cold.wall':
    'the wall he promised was a door he was opening, a course at a time, over winters. inside-the-wall was never safe. it was the far side being let in.',
  // the easy-here beat, re-staged cold.
  'iss.dialogue.turns_cold.easy':
    'the ease he offered was the not-counting. a thing told it is kept, and never counted, is a thing being readied to be let go.',

  // -------------------------------------------------------------------------
  // THE EAR — keeper-whisper relevance (D11 `backlog-modeled-mob-and-voice`, FACT 17, P3). The
  // VoiceListener hears a spoken token that rhymes with a way; SpatialVoiceBeat speaks one of
  // these to one player, or the PrivateSoundBeat pack-sound fallback. FACT 17 plants at P2 and
  // degrades to the pack-sound whisper if the voice layer never installs. Defers to apparitionClaim.
  // -------------------------------------------------------------------------

  // the relevance whisper — the record files what is SAID of the ways, not only what is done.
  'keeperWhisper.heard':
    'the record files what is said of the ways, not only what is done. a word was said over this place, and it is entered, against the name and against the ground.',
  // the pack-sound fallback line (when no voice layer) — the same fact, one short whisper.
  'keeperWhisper.fallback':
    'what is said is kept too. the record heard it.',

  // -------------------------------------------------------------------------
  // THE COMPANION — the found "kept close" tally (the-companion.md §6). The ONE
  // in-fiction document Wren authors: not a journal, an inventory of the group.
  // It surfaces ONLY post-reveal (thread_cards `kept-close`, gated on the reveal
  // condition) — the proof no accusation could be. Wren's own SPOKEN lines are
  // Set-A human speech and live in npcLines below; THIS is the record showing the
  // find, so it holds the Watcher record register (lowercase, no exclaim). The
  // Observer's sharp quotes were harvested here — this is where "it knows your
  // name" is earned, not magic (§6 design note). Names no living player (INV-16);
  // the tally IS the group's own words, shown back.
  // -------------------------------------------------------------------------
  cardKeptClose:
    'a tally-book, soft with handling, the hand the same one that drew the safe path on the first night. it is not a journal. it is an inventory. of you. every page dated, the last entries today. the thing you said you would never do is written here. where you said you would go if it ever got bad is written here. the name you use only for each other is written here. he kept you close. this is what close was. the sharp things the dark knew of you were not known by the dark. they were carried down, one page at a time, in this hand.',
};

// ===========================================================================
// NON-WATCHER — SET-A surface NPC lines (design/content/npc-dialogue.md +
// the three SET-A surface cards from thread-archive.md).
//
// *** THIS IS NOT THE WATCHER REGISTER. ***
// These are ordinary HUMAN speech: contractions, capitals, exclamation, named
// feeling — all allowed and intended. They are the five surface NPCs (Aro,
// Wenna, Coll, Dob, Old Pell) talking. They are kept in a SEPARATE const so the
// two registers cannot bleed and `archiveLine()` (Watcher only) never returns
// them. npc-dialogue.md proposes these eventually live in a dedicated
// `npcVoice.ts`; gathered here, labelled, until that integration lands.
// ===========================================================================

export const npcLines: Record<string, string> = {
  // --- SET-A surface cards from thread-archive.md (Aro / Wenna / Pell as found testimony) ---
  cardSurfaceAroLie:
    'the painted line on the big stair? step right over it, friend. that is the locals keeping the soft folk out so they can have the warm to themselves. cross it and keep going. that is where it gets good. sleep wherever you like down there. it is a hole. holes do not have ears.',
  cardSurfaceWennaFolk:
    'gran used to say there were seven somethings you had to mind down there. seven. i only ever remember six and i always forget a different one. light, and the line, and the bird, and the bowing, and the giving, and — see, there is the sixth gone again. you do not say the cold’s name. that one i remember, because she would go white when i tried.',
  cardSurfacePellTruth:
    'i will tell you the only true thing i have. it does not chase. whatever is down there, it does not chase you. it waits, and it watches, and it takes what stops being watched. so be watched. stay where your people can see you. i minded six of the seven. i have thought a long time about what the seventh would have cost me, and it would have cost less than the not-minding has.',

  // --- ARO — the rumor-broker who lies (npc_key: aro) ---
  'aro.greet.neutral':
    'Ah — fresh boots. Sit, sit, you’re letting the cold in. You want the way down, you want the right person, and lucky you, here I am.',
  'aro.greet.again':
    'You’re the lot poking round the old hole, yeah? Course you are. Everyone is, this season. Drink first. The hole’s not going anywhere.',
  'aro.greet.warm':
    'Huh. You went down and you came *back* up, and you came back — quiet. Most don’t. Most come back loud or don’t come back. You’re alright, you. Sit. On the house.',
  'aro.greet.cold':
    '...you don’t look so good. No offence. You’ve got that — that grey on you. Bunch came through last month with that same grey and I, ah. I don’t see ’em anymore. Tab’s closed. Go on.',
  'aro.greet.iss_cold':
    'You found what’s past the line, then. Yeah. I can tell by your faces. Look — I never *been* down there, I just say what sells, that’s all I — don’t. Don’t tell me about it. I don’t want it in my head with the rest of the things I say.',
  'aro.rumor.town':
    'Way I heard it, there’s a whole town down there. Lamps still burning. People who just — stayed. Living fat off the warm while we freeze our backsides up here. That’s why nobody comes back up, see. Not ’cause they died. ’Cause it’s *nice*.',
  'aro.rumor.line':
    'There’s a line painted across the big stair, halfway down. Don’t mean nothing. Old paint. Builders’ mark. People make a whole religion out of a stripe of pitch, I swear. You want to see it, it’s down past the lamp-house, on the Stair.',
  'aro.rumor.bird':
    'They say there’s a bird down there older than the digging. Keeps the air sweet. You find the bird, you find the bottom, and the bottom’s where they kept the good stuff. Coops were up at the Lamp-works, last anyone said.',
  'aro.lie.cross':
    'The painted line? Step right over it, friend. That’s the locals keeping the soft folk out so they can have the warm to themselves. Cross it and keep going. That’s where it gets good.',
  'aro.lie.moon':
    'Sleep wherever you like down there. Black moon, white moon, no moon — rock doesn’t care what the sky’s doing. That’s a tale to sell candles.',
  'aro.bye.warm':
    'Mind how you go. Come tell me what you find — I’ll make a good story of it either way.',
  'aro.bye.cold':
    'I’m out of stories for you. First time in my life. Just — don’t tell anyone where you heard the line was safe to cross, yeah? Don’t put that on me.',

  // --- WENNA — half-remembers the ways as folk-superstition (npc_key: wenna) ---
  'wenna.greet.neutral':
    'Mind the lamp by the door, love, don’t pinch it out. House likes to look lived-in after dark. Gran’s rule, not mine, but I’ve never had cause to break it.',
  'wenna.greet.again':
    'Back again. Good. Take a crust for your pocket — no, I won’t hear it, you take the crust. You leave a little, you get to keep a little. That’s the whole of it, near enough.',
  'wenna.greet.warm':
    'Oh, you minded it all, didn’t you. I can tell. You’ve got the — the *kept* look. Gran would’ve liked you. She’d have given you the good chair.',
  'wenna.greet.cold':
    '...did you leave a little? Down there. Did you give anything back, or did you just — take. You don’t have to answer. I can see you didn’t. Take the crust anyway. Maybe it’s not too late for the crust.',
  'wenna.rumor.seven':
    'Gran used to say there were seven somethings you had to mind down there. Seven. I only ever remember six and I always forget a different one, isn’t that the way. Light, and the line, and the bird, and the bowing, and the giving, and... see, there’s the sixth gone again.',
  'wenna.rumor.name':
    'You don’t say the cold’s name. That one I do remember, ’cause she’d go white when I tried. ‘You don’t *name* it, Wenna.’ Name what, Gran? And she’d just — wouldn’t. So I don’t. Habit now.',
  'wenna.rumor.moon':
    'When the moon goes black you stay up. Stupid, isn’t it. I still do it. Sit up all night with the lamp like a fool. Slept through it once as a girl and had the worst dreams of my life, so.',
  'wenna.truth.bow':
    'Bow at the stones. I don’t know who to, mind. Gran never said who. You just bend your knee going past and you don’t think too hard about it. The ones who don’t bend... she’d just shake her head.',
  'wenna.truth.light':
    'Keep your light. Above all the others, keep your light. That one she said like it mattered more than the rest put together, and she didn’t say things like that twice.',
  'wenna.quest.offer':
    'Do me a kindness while you’re down there. There’s a little shelf-stall, sells nothing, kept lit for the dead — leave the crust there, not in your pocket. Gran’s gran kept that stall. I never can go myself. You’ll do it? Good.',
  'wenna.quest.done':
    'You left it. At the dead-stall. I didn’t tell you where it was and you found it and you left the crust. You don’t know what that — no, you do, I think. I think you know exactly what that was.',
  'wenna.bye':
    'Go on, love. The lamp’ll be lit for you. I mean that the ordinary way *and* the other way, if there is an other way, which I’ve never quite decided.',
  'wenna.bye.cold':
    'I’m going to light a second lamp tonight, I think. After seeing you. No reason. Just feel like the house wants two lit, with you stood there like that.',

  // --- COLL — the trader (npc_key: coll) ---
  'coll.greet.neutral':
    'Torches, oil, rope, three days’ rations, a spare striker ’cause your first one’s already wet. Don’t haggle, I’ve heard your speech, the answer’s the price on the tag.',
  'coll.greet.warm':
    'You came back, you’re spending, you’re not babbling. Model customer. Here — striker’s on me. Don’t tell the others I do that, it ruins the business.',
  'coll.greet.cold':
    'Cash up front from you. No, nothing personal. Last three that came up looking like you settled their tab and then I never saw the coin spend again. It just... sat where they dropped it. So. Up front.',
  'coll.shop':
    'Down or up? Down, you buy light. Up, you sell whatever you found that’s still worth anything. Which is rarely much. People bring up the strangest junk and want gold for it.',
  'coll.rumor.oil':
    'Folk come up babbling about a watcher, a presence, eyes in the dark. You know what I sell to those folk? More oil. Whatever’s down there, it’s never once stopped a man from needing more oil.',
  'coll.rumor.lampworks':
    'Furthest I go’s the lamp-house — the Lamp-works, second level. Good trade there, people coming up are scared and scared pays full price. Past that? Nothing past that’s worth a markup. Past that you don’t come back to spend it.',
  'coll.truth.line':
    'The painted line’s real, if that’s your question. I’ve seen it. I don’t cross it. Not ’cause of stories — ’cause everyone who does stops buying oil from me, and I notice when a customer stops existing.',
  'coll.truth.twolamps':
    'Keep one lamp more than you think you need. That’s not wisdom, that’s stock advice. The man with two lamps comes back to spend. The man with one comes back as a story. I’d rather you came back to spend.',
  'coll.quest.offer':
    'You’re going down past where I go. Fine. Take this sealed jar to the third lamp on the Lamp-works stair — it’s been dark for years, some lampwright’s old stand, number’s worn off. Light it. I’ll knock the rope off your next bill. I don’t like a dark stand on my route, bad for trade.',
  'coll.quest.done':
    'You lit it? The third one? Huh. It’s been dark longer than I’ve sold here. Rope’s free. And — nothing. Just. Good. A lit stand’s a lit stand.',
  'coll.bye':
    'Buy and go. You know where I am. I’m always where the oil is.',
  'coll.bye.cold':
    'I’ll sell you the oil. I’ll always sell you the oil. But I’m not shaking your hand, and I’d thank you to buy and go.',

  // --- DOB — descends with the group (npc_key: dob) ---
  'dob.greet.bravado':
    'Right, I’ve been down to the second level loads of times, loads, so just — stick behind me and we’re golden. Loads of times. Twice. Twice is loads.',
  'dob.greet.alert':
    'I’m not scared, before you ask. I’m *alert*. There’s a difference and my mum says it’s a good quality.',
  'dob.chatter.lampworks':
    'See, this is fine. Lamps, smell of oil, nothing weird. People worked here. Normal job, normal — okay, why’s it so *tall*, the ceiling, down here. Was it always this tall? I don’t remember tall.',
  'dob.chatter.cisterns':
    'Don’t drink the still water, that’s Cistern 7, that one’s gone bad — my uncle said. Or was it 7’s the good one. One of ’em’s good. Let’s not test it. Let’s super not test it.',
  'dob.chatter.line':
    'There’s the line. The painted one. We’re — we’re not crossing that, are we. Tell me we’re stopping at the line. Aro said cross it but Aro’s a liar, everyone knows Aro’s a liar, why’d I even — we’re stopping at the line, right?',
  'dob.truth.lied':
    'Okay — real talk — I’ve never been past the Lamp-works. I lied. Twice was a lie, it was once and I cried on the way up. I just wanted to come ’cause everyone treats me like a kid. I don’t know what’s down there any more than you do.',
  'dob.truth.lamp':
    'I keep my lamp on me. Not letting go of it. You can have my rope, you can have my rations, you cannot have my lamp, I will not be the one whose light goes out, I’ve *heard* what they say about the ones whose light goes out.',
  'dob.react.good':
    'I feel — okay, this is going to sound stupid — I feel better next to you lot. Like the dark’s paying attention, but not to *us*. Not while we’re together. Stay close though, yeah?',
  'dob.react.good.up':
    'We did it the right way, yeah? Bowed and gave the bird its seed and kept the lamps. My gran’d be made up. Let’s go up. Let’s go up while we’re still the kind of people my gran’d be made up about.',
  'dob.react.bad':
    'Why’d you cross it. Why’d you — Aro said it was fine but you *knew* Aro lies, I told you he lies, and you crossed it anyway, so you didn’t do it ’cause you believed him. You did it ’cause you wanted to. That’s worse. Why’s that worse. It feels worse.',
  'dob.react.bad.wait':
    '...I don’t want to go further. I’ll wait here. By the lamp. I’ll just — I’ll keep this lit and I’ll wait. You go on. I’ll be right here. I’ll be right here. I’ll be right here.',

  // --- OLD PELL — won’t descend; remembers your conduct (npc_key: old-pell) ---
  'old-pell.greet.neutral':
    'I won’t go down, so don’t ask. People always ask. They think I’m being dramatic. I went down once. That was the whole of my going-down. You’ll understand or you won’t.',
  'old-pell.greet.again':
    'Sit if you like. Don’t sit if you don’t. I’m not lonely, I’m just old, the two get confused.',
  'old-pell.greet.warm':
    'You. You’ve been down more than once and you come up the same every time. Same eyes. You don’t know what that’s worth. I do. Come here. Good. You’re still in there. Stay that way.',
  'old-pell.greet.cold':
    'I’ve been watching you come and go. I watch everyone. And you — you’ve gone grey at the edges, the way they do, the way *they* did, and I’m not going to pretend I don’t see it to spare your feelings. I’m too old to lie about the grey.',
  'old-pell.greet.iss_cold':
    'So you found the dead shrine. West and down, the cold hearth. I knew a man went looking for a road up at the bottom of a hole, and I knew what came back wearing him. You went where he went. I won’t ask if you came back as you. I’m watching to see.',
  'old-pell.memory.kinds':
    'I knew people who went down keeping every little rule like it was nothing, like a game, and they came up and they were *here*, you understand, all the way here, behind their own eyes. And I knew the other kind. I don’t say what happened to the other kind. You’ll know it if you see it. You’ll wish you didn’t.',
  'old-pell.memory.seventh':
    'There were seven things you minded down there. I minded six of them. Six. I have spent a long time thinking about the seventh, and what it would’ve cost me to mind it, and I think now it would’ve cost me less than the not-minding has.',
  'old-pell.truth.watched':
    'I’ll tell you the only true thing I have. It doesn’t chase. Whatever’s down there, it does not chase you. It waits, and it watches, and it takes what stops being watched. So be watched. Stay where your people can see you. That’s all I’ve got and it’s worth more than every map Aro’s ever sold.',
  'old-pell.react.good':
    'I’ll remember you came back right. That’s not nothing, a person remembering you right. It’s most of what I’ve got left to give.',
  'old-pell.react.bad':
    'I remember the others who went the way you’re going. I remember all of them. That’s my curse, that I remember. And I’ll remember you. Whatever you become down there, some part of you’ll be up here, remembered, by a bitter old man who told you and you didn’t listen.',
  'old-pell.press.refuse':
    'No. You don’t get that one. I carried it up alone so nobody else would have to carry it. Don’t you dare make me hand it to you.',
  'old-pell.bye':
    'Go on. I’ll be here. Where else.',

  // --- WREN — the trusted companion (npc_key: wren) --------------------------
  // the-companion.md. Set-A HUMAN speech, but a DISTINCT fingerprint from the five
  // above: warm, present-tense, contraction-heavy, HEDGED, and deliberately lowercase
  // (§3 — "uncertain and modern" where the keepers are certain and archaic). He
  // under-claims, asks about YOU (the questions are the harvest, legible only in
  // hindsight), and is never present when the Watcher manifests. His verbal tic is
  // "stay close" — care in Trust, control at the Reveal, and the heading of his found
  // tally ("kept close", cardKeptClose in the archive above). These lines are gated in
  // the plugin/dialogue layer by the companion_* flags (companion_introduced →
  // companion_trust → companion_revealed → reckoning_*), the PRODUCERS of which are
  // plugin listeners not in this scope; here is the CONTENT they resolve to.

  // M1–M2 · TRUST I–II (companion_introduced; companion_trust rises) — the one warm
  // voice down here. Warns off a bad path, hands a true gift, asks a lot of gentle
  // questions. The last clause of the care line is the harvest.
  'wren.trust.meet':
    'oh — hey. you’re new. real people, not the dark doing a voice. sorry, you get careful about that down here. i’m no one, honestly — just someone who’s been down here a while. stay close, would you. i lost people going off alone and i’m not doing that again.',
  'wren.trust.warn':
    'not that way. i mean it — not that way. i know it looks like the easy road. the easy road down here is how you lose someone. let me walk it with you, or don’t walk it. those are the two i’d pick.',
  'wren.trust.gift':
    'here. take it, it’s nothing, it’s just a spare — you’ll want it before i will. no, keep it. tell me where you’re headed and i’ll tell you what’s waiting. that’s the trade. it’s a good trade.',
  'wren.trust.ask':
    'so tell me about you lot. the real stuff. what you’d never do, where you’d go if it got bad, the name you only use for each other. no — i like knowing. it’s how i keep track of who to keep close.',
  'wren.trust.absent':
    'i stepped out — sorry — thought i heard something in the dark and went to check and it was nothing, it’s always nothing. you’re alright? good. i hate that i wasn’t just behind you. i’m always just behind you.',

  // M3 · TRUST III + the first hairline crack — every warning is the cautious
  // direction, always with a good reason, and the scares track what they told him.
  'wren.crack.slow':
    'not tonight. it’s a black moon — brann’s night, we wait, you don’t want to be moving under that. i know, i know, it’s always a reason with me. they’re good reasons. that’s the thing about me. the reasons are always good.',
  'wren.crack.notice':
    'you’re looking at me funny. that — okay. no, say it. you noticed it only ever knows the things you said out loud. near me. that’s. that’s a hard thing to have noticed. i’m not going to tell you you’re wrong.',

  // M4 · THE REVEAL (companion_revealed; gated on iss_caught — the same lens that
  // caught Iss turns on Wren). He does not deny the what. He denies the WHY — insists,
  // to the end, that it was protection. The lie is in "safe".
  'wren.reveal.yes':
    'yes. all of it. from the first night. i fed it your names, your plans, the thing you said you’d never do — i fed it you. and i’d do it again. every time i handed it one of you it took me instead of you, for a night, and you’re all still here, aren’t you. still here. i kept you close. i kept you safe.',
  'wren.reveal.tally':
    'you found the book. yeah. that’s my hand. it’s not a journal — i know how it looks. it’s where i kept you. every page. i told myself it was so it would take me slow instead of taking you. that part’s even true. it’s just not the whole of the true.',

  // M5 · THE RECKONING — three lines the group chooses to enter into the record about
  // him (the same correct-the-record verb as the Seventh's quest). All hard: he was
  // scared AND self-serving, and he will not clarify it for them.
  'wren.reckoning.condemn':
    'that’s — fair. write it. write me as what i did, a man who traded you to save himself. at least it’s true. the record can have that one true. stay cl—',
  'wren.reckoning.understand':
    'you’re not going to make it simple. scared and selfish, both, all of it, none of it crossed out. that’s the hardest one to write and you wrote it anyway. i don’t get to be a hero or a monster. i just get to be true. i can hold that. i think that’s the only thing i can hold.',
  'wren.reckoning.free':
    'oh. you’re not going to keep me either. neither one. you’re just going to — let go. i forgot that was a thing you could do to a person. it ends me, you know that, unfed i don’t hold together. but it ends me let go, not taken. thank you. i’m sorry. i wasn’t only lying.',

  // async / dynamic-roster — a late joiner gets a Wren line (the reveal is one group
  // event, quorum-free; §7 invariants).
  'wren.roster.newhand':
    'a new hand — good. more of you is better. stay close, all of you. that’s the only rule i’ve got and it’s the whole of what i know.',
};

// ===========================================================================
// RESOLVERS
// ===========================================================================

/**
 * Resolve a Watcher-register archive key to its recovered text. Mirrors the
 * `customPhrase` / oracle resolver shape in voice.ts: a key in, the bound line out.
 * Returns null for an unknown key so the caller can fall back to its own in-register
 * default — a key typo never leaks a raw identifier to a player, and never errors.
 *
 * NOTE: this resolves ONLY the Watcher-register `archive`. It deliberately does NOT
 * resolve `npcLines` — those are SET-A human speech and must be read through the NPC
 * dialogue path, never returned where a Watcher line is expected (the separation law).
 */
export function archiveLine(key: string): string | null {
  return Object.prototype.hasOwnProperty.call(archive, key) ? archive[key]! : null;
}

/**
 * Resolve a SET-A surface-NPC line (the human register). Kept as a separate function
 * from {@link archiveLine} so the two registers cannot be resolved through the same
 * door. Returns null for an unknown key.
 */
export function npcLine(key: string): string | null {
  return Object.prototype.hasOwnProperty.call(npcLines, key) ? npcLines[key]! : null;
}
