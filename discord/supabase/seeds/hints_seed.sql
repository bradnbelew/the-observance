-- The Observance — hints_seed.sql
-- The WHISPER RAIL content (the group's only safety net; OVERHAUL/BUILD-PLAN P0). The `hints` table
-- (0003_discord.sql) ships EMPTY; without bodies, /whisper returns nothing and a group that hits one
-- unsignposted cipher stalls a whole session and quits. This seeds tiered bodies for the SPINE puzzles
-- that survive v2 (the 5 keeper ciphers + the literacy on-ramps + the Iss catch).
--
-- DELIVERY (voice.ts whisperReply): tier 1 is a FIXED ambient nudge in code ("look again at what
-- repeats…") — NOT seeded here. We seed tier 2 (plainer) and tier 3 (near-spells-it, the rescue floor).
-- REGISTER: the Watcher's voice — lowercase, sparse, certain, NEVER "hint: try X". It KNOWS things; it
-- does not give hints. Escalates so the group never hard-locks, but stays in character.
--
-- Run as service_role after 0003. Idempotent (ON CONFLICT (puzzle_key,tier) DO UPDATE).
-- NOTE: these are the spine ciphers only. Author the DIVERSE puzzles' hints (PUZZLES.md §5) as those
-- puzzles are designed; this is the floor that makes a first playtest safe.

begin;

insert into public.hints (puzzle_key, tier, body) values

-- rosetta-ring — the rune-literacy on-ramp (assemble the six ways, in order, off the carved ring).
('rosetta-ring', 2, 'the ring is not decoration. it is a key. the marks around it are the ways, set in the order the record keeps them. read them, and you can read the rest.'),
('rosetta-ring', 3, 'six ways, in the carved order: bow, offering, kept light, deep line, unspoken, sacred beast. learn the mark beside each. that is the alphabet the stones are cut in.'),

-- stone-vaun — Caesar (every letter held back by a fixed amount; his hoarding made literal).
('stone-vaun', 2, 'vaun gave nothing back. even his letters are held back — every one, by the same measure. find the measure and give them back.'),
('stone-vaun', 3, 'each mark stands for a letter shifted a fixed count down the row. try the counts one by one; when the words come clear, that is his.'),

-- stone-mara — book cipher (page/line/word into the lectern shelf she kept).
('stone-mara', 2, 'mara read and did not walk. the numbers on her stone are not the answer — they are where to look. she left the books.'),
('stone-mara', 3, 'three numbers to a word: the page, the line, the word along it. walk her shelf, count to each, and the sentence assembles itself.'),

-- stone-sella — atbash (the mirror; the water gives the face back wrong).
('stone-sella', 2, 'sella speaks only as a reflection now. her marks are the same — read backward. the water would show you how.'),
('stone-sella', 3, 'the first letter is the last, the last is the first, folded at the middle of the row. read her stone as its own mirror.'),

-- stone-orin — substitution (one rune, one letter, kept the same throughout).
('stone-orin', 2, 'orin would not bow, and he would not bend his marks either. each one is a letter, and it is always that letter. find the small words first.'),
('stone-orin', 3, 'a one-for-one swap, steady the whole stone. start where a single mark stands alone — that is "i" or "a" — and let the rest fall in.'),

-- stone-iss-wall — Vigenère keyed on a name (the catch begins here).
('stone-iss-wall', 2, 'iss is warm, and warmth is the wall here. his stone will not open to a plain reading. it wants a key — a word laid over it, again and again.'),
('stone-iss-wall', 3, 'whose wall is this? lay his own name over the marks, letter against letter, and read what comes. the key is the man.'),

-- iss-warm — the warm misreading (true-feeling, leads to the cold dead hearth; the doubt is the point).
('iss-warm', 2, 'the warm reading feels true. it tells you the ways are a wall and you are safe inside. follow it, if you would — but mark where it leads.'),

-- no-wall-catch / iss-doubt — the CATCH (the dead-end pushes you back to re-test his key).
('iss-doubt', 2, 'the warm road was read true and still went nowhere — a cold hearth, a grave, nothing kept. a true road that saves no one. whose road was it?'),
('iss-doubt', 3, 'you trusted his reading once. read his stone again with his own name as the key, the way you should have the first time. the warm wall was never a wall.'),
('no-wall-catch', 2, 'no wall was ever built here. what iss called a wall was the way the reaching was let in. read who carved the comfort, and ask why a man carves a comfort he knows is a lie.'),
('no-wall-catch', 3, 'the warm voice lied. his key turns his own stone to the name they used for him: the one who turned away. the land kept the proof he hoped you would not find.'),

-- a1z26-tick-stave — the numeral-literacy twin (tick marks as counts → letters).
('a1z26-tick-stave', 2, 'these are not words. they are counts. each cluster of ticks is a number, and each number is a letter in its order.'),
('a1z26-tick-stave', 3, 'count the ticks: one is a, two is b, on down the row. read the counts as letters and the stave speaks.'),

-- stone-brann — (when re-authored as the railFence/beacon night cipher, P0-5) the count-the-fires read.
('stone-brann', 2, 'brann kept the watch by the lamps, and counted them twice. the order the lights are read is the order that matters here — not the marks, the sequence.'),
('stone-brann', 3, 'read the lit lamps in their rows, top to bottom, the way a watchman counts down a black moon. the sequence spells what he could not say twice the same.'),

-- stone-brann-cipher — the SIXTH keeper-stone as its real cipher (railFence, read-by-time): the
-- carving rakes visible only by the lit beacon-glow after dark. Rails = the fire-count brann names.
-- Reuses brann''s taught rail literacy; tier 2 points at the night gate + the rails, tier 3 the read.
('stone-brann-cipher', 2, 'brann''s stone keeps nothing in the day. wait for the dark and let the lamps light the marks. the number he counted twice is the number of rails his line is raked along.'),
('stone-brann-cipher', 3, 'count the fires brann kept — that count is the rails. after dark, read the lit marks down each rail in turn, the way you read his lamps, and the raked line comes back together.'),

-- ===========================================================================
-- BACK-HALF SPINE (Movements II→V). The front-half ciphers had a safety net;
-- these did not, and a group stalled here dead-airs with no in-world pointer. Same
-- rail: tier 2 plainer, tier 3 the rescue floor. Same register — the Watcher knows
-- things; it does not give hints. Many of these are not ciphers but ACTS: the whisper
-- points at the thing to DO, never spells a word that opens nothing. (tier 1 stays the
-- fixed ambient nudge in voice.ts.)
-- ===========================================================================

-- ── MOVEMENT II — the false-law thread (found carvings; true readings that keep no door) ──

-- forged-eighth — the added ordinance. A real substitution read that names a way the
-- founders never set; obeying it pays nothing. The lie is that its toll never comes.
('forged-eighth', 2, 'seven were set, and this stone makes eight. read it the same one-for-one way as the others — the marks are honest even when the law is not.'),
('forged-eighth', 3, 'decode it and it names a covering of your own — a way not in the ring the founders carved. keep it as long as you like. the land never once counts it. that silence is the proof.'),

-- prophet-wall-comfort — Iss with a pulpit. Warm promises, each a true substitution
-- solve that opens nothing (a wall keeps nothing out; the record is not outside).
('prophet-wall-comfort', 2, 'the wall speaks softly, in the same swap the keeper stones use. read a line of it plainly. then ask what it is that it keeps outside, and whether the record was ever out there.'),
('prophet-wall-comfort', 3, 'every warm line reads true and gives nothing. keep the ten, be easy, stay inside the wall — decode it and it comforts, and comfort is all it does. no door opens off a wall that keeps out a thing already within.'),

-- prophet-wall-name — the columnar acrostic hidden down the warm rungs (the wall was his).
('prophet-wall-name', 2, 'you have been reading the warm lines across. read them down instead — the first mark of each, top to bottom. a name is standing in the margin.'),
('prophet-wall-name', 3, 'take the first mark of each promise, in order, down the wall. they spell who carved the comfort: the one who turned away. iss built the wall he told you to hide behind.'),

-- ── MOVEMENT III — the Undercroft descent + the Seventh side quest ──

-- undercroft-descent — PERFORM mara's read; do not type it, do the thing it says.
('undercroft-descent', 2, 'mara did not want the sentence copied out. she wanted it done. you have the reading — descend and bow at the unbroken light. the light that never went dark is a place you can stand.'),
('undercroft-descent', 3, 'walk to the one light in the lectern room that is always lit, and go down through the door beneath it. the way opens to the descent itself, not to a word typed into the record.'),

-- undercroft-fog — witness the altar room rebuild wrong (a told set-piece; look, do not decode).
('undercroft-fog', 2, 'nothing here is ciphered. the room is telling you plainly, if you stay in it. one fire is kept and no hand tends it. ask what that means about the ones who kept it.'),
('undercroft-fog', 3, 'the rite was never a trade. they did not leave when it was done — they were kept. one fire, eternal, attended by no one. say what you see: the room rebuilds wrong.'),

-- seventh-shrine — sella''s bearing → count the markers; there is one past the last.
('seventh-shrine', 2, 'sella''s bearing sent you to the cold shrine. count what stands there. count again past where the counting is meant to stop.'),
('seventh-shrine', 3, 'there are six markers, and then there is one more the tally was never meant to reach. there was a seventh. the land kept six and refused it — and a thing that can say no is not a wall.'),

-- seventh-unwriting — chamber two: the rail-fence sealed below the hearth (reuse brann''s rails).
('seventh-unwriting', 2, 'the deep below the cold hearth is sealed, and the seal is a name. it is raked the way brann''s lamps were — along rails you can count on the wall itself.'),
('seventh-unwriting', 3, 'count the rails cut into the wall, then read the marks along them the way you read brann. the unwriting kept the very name it was meant to cut out. name it, and the seal gives.'),

-- seventh-cause — the cause-fragment (the land refused one who broke nothing). Told lore.
('seventh-cause', 2, 'do not look for a broken custom here. look for the absence of one. the seventh kept every way and was cast out anyway.'),
('seventh-cause', 3, 'the fire they let out was never theirs to lose. the land refused a keeper who broke nothing — whether that is mercy, the record does not say. say what it did: it refused the one who broke nothing.'),

-- seventh-choice — restore or erase (detected in-world; the whisper points at the two acts).
('seventh-choice', 2, 'this one is not read. it is chosen. at the unwriting there are two things a hand can do to the seal, and only one can be done.'),
('seventh-choice', 3, 'lay the deposit back and the seventh is restored; strike the seal and it is erased for good. there is no word to type — do the one your hands will stand behind. the record keeps whichever you choose.'),

-- fork-light — draw the kept light up the stair, or bank it and leave the room dark.
('fork-light', 2, 'the eternal flame can be carried, or left. neither is safe and neither is wrong. the leaf you take is permanent — the room does not relight.'),
('fork-light', 3, 'draw the light up the stair and carry it into the close, or bank the flame and let the room stay dark for the rest of it. choose the one you will not want to undo. both are kept.'),

-- ── MOVEMENT IV — the reckoning + the single Iss chain (bound word → gate → coord → walk) ──

-- orin-threshold — finish orin''s broken "i —" in the third hand (atonement, not cipher).
('orin-threshold', 2, 'orin''s line breaks off at "i —". it is not for you to decode. it is for you to finish, in the hand that stands over the threshold now.'),
('orin-threshold', 3, 'complete the sentence he could not: i was not kept, i was counted, and the count was true, and it was not enough. named, then warned, then left at the threshold. ask which of the two is still free to leave.'),

-- haunting-biography — a keeper ties the movement-I dread to a named fate (told dialogue).
('haunting-biography', 2, 'the first dread that found you was not chosen at random. go back to the keeper who will speak of it, and ask whose it was.'),
('haunting-biography', 3, 'the earliest hauntings were one keeper''s fate, re-enacted at your door. the dread had a biography. say that plainly and the telling opens.'),

-- atonement-refrain — honor a broken custom, then return to the keeper who withheld the fragment.
('atonement-refrain', 2, 'a keeper is withholding a piece, and no key will take it. conduct is the lock here, not cleverness. honor a custom you broke, then come back.'),
('atonement-refrain', 3, 'go do right by one of the ways you failed — perform it, do not describe it — and return to the keeper who held the fragment back. the fragment is the key; your conduct is the lock. the keeper''s turn is the turn you make.'),

-- fork-name — carve Iss''s name, or leave it unspoken (the withholding is the kept reading).
('fork-name', 2, 'you know his name now. the choice is whether to cut it into the record or leave the mark empty. the unspoken is one of the ways.'),
('fork-name', 3, 'speak the name and it is carved for good; keep it, and the mark stays uncut. the kept reading is the withheld one — but the choice is yours, and the record keeps whichever you make.'),

-- base-docket-reread — the down-count re-read after the catch (the muster of present hands).
('base-docket-reread', 2, 'you read the down-count as a doom-clock. read it again, after the catch. it was never counting the dark.'),
('base-docket-reread', 3, 'the muster is being read. the count was never of the dark — it was of the hands, and the hands are almost all in. it is a roll call, not a doom clock.'),
-- base-docket-reread-auto — the offline twin (same payoff, whichever the world serves live).
('base-docket-reread-auto', 2, 'you read the down-count as a doom-clock. read it again, after the catch. it was never counting the dark.'),
('base-docket-reread-auto', 3, 'the muster is being read. the count was never of the dark — it was of the hands, and the hands are almost all in. it is a roll call, not a doom clock.'),

-- meta-unkept — the six maker''s-marks, read in fall-order, carry one word.
('meta-unkept', 2, 'there is one small glyph framing each keeper stone — a maker''s mark, one to a stone. six marks, one word. the order is not the order they stand in.'),
('meta-unkept', 3, 'read the six maker''s-marks in the order the keepers fell — vaun, mara, sella, orin, brann, iss — not the order on the ring. the word they spell is the one each of them did not keep: unkept.'),

-- bound-word — the Iss vigenère plaintext IS the coop-gate''s need (the convergence word).
('bound-word', 2, 'the catch re-cut iss''s stone. read it now with his own name laid over it, and it yields a single bound word — the word another gate is waiting to be given.'),
('bound-word', 3, 'lay iss''s name over his stone, letter against letter, and it reads: the one who turned away. that is the bound word. carry it to the gate that needs it.'),

-- m4-three-hands — the cross-surface co-op gate (three acts in one window; not typed).
('m4-three-hands', 2, 'this gate does not open to a word. it opens to three things done at once — a foot, a carve, a word posted here — inside the same short breath.'),
('m4-three-hands', 3, 'one of you stands on the plate, one cuts the mark, one posts here, and all three within the same window. no single hand clears it. do the three together and the threshold opens.'),

-- threshold-coordinate — the threshold carving yields the TRUE coordinate (a road, not an answer).
('threshold-coordinate', 2, 'the opened threshold is carved with a mark that points. it is not an answer to type — it is a direction to walk. follow where it sends you.'),
('threshold-coordinate', 3, 'read the threshold mark as a bearing and walk it to the end. the true coordinate was never a word; it is the road iss hid. what you type is the word waiting for you where it points.'),

-- true-walk-arrive — the true walk endpoint (read the destination word on-site; presence-gated).
('true-walk-arrive', 2, 'the road kept its word. you have to be standing where it ends to read what it left — the answer is carved there, not here.'),
('true-walk-arrive', 3, 'walk the true road to its end and read the leaves placed at the tableau. you were filed here before you came: kept here before you. the road kept its word.'),

-- ── MOVEMENT V — the Accepting (acts and rites; the whisper points at what to do) ──

-- rite-tokens — lay one personal token per slot; the missing tool is you.
('rite-tokens', 2, 'the six slots are not asking for the right object. they are asking for a piece of you — one you cannot read your way out of. bring the thing only you can give.'),
('rite-tokens', 3, 'lay one personal token in each of the six slots, alongside the named components. the tool the rite was always missing is the giver. deeps first, heart, unbroken light, salt of the keepers — and the piece that is yours.'),

-- pressure-glyph-walk — walk the rune the floor names, footstep by footstep (do not decode it).
('pressure-glyph-walk', 2, 'the floor names a rune. do not decode it. this is mara''s lesson made a door — the shape is walked, not read.'),
('pressure-glyph-walk', 3, 'trace the rune the altar floor carries with your own feet, step by step, until the whole shape is walked. it is a second way into the accepting. do not read your way out of it.'),

-- accepting-crouch — everyone present bows as one, at the hour, in the kept light (detected).
('accepting-crouch', 2, 'there is no chosen one here, and no word to type. the rite asks all of you at once. what is the smallest of the ways — the one orin thought too small to matter?'),
('accepting-crouch', 3, 'everyone present bows together, as one, in the kept light, at the hour. the bow is the smallest way and the whole of it. no one is left standing; no one goes first. do it together and the record opens.'),

-- record-receives — the world''s answer to the bow (opaque sentinel; witness it, do not solve it).
('record-receives', 2, 'this one is not yours to open. once the bow is made as one, the record answers on its own. stay, and let it.'),
('record-receives', 3, 'nothing here is to be decoded. the record receives you — the world turns kept, and the change is felt, not typed. you have already done the thing that opens it.'),

-- ===========================================================================
-- THE DIVERSE EXPANSION (design/PUZZLE-DESIGNS.md). Same rail: tier 2 plainer, tier 3
-- the rescue floor. Same register — the Watcher knows things; it does not give hints.
-- Many of these are ACTS, not typed answers: the whisper points at the thing to DO. (tier 1
-- stays the fixed ambient nudge in voice.ts.)
-- ===========================================================================

-- ── VAUN — the hoarder (deposit / count) ──
-- vaun-hoard-sorted — deposit the first-of-the-deep into the empty "given back" chest.
('vaun-hoard-sorted', 2, 'vaun kept a column he never filled. five chests are full and one is labelled and empty. the answer is not read — it is given.'),
('vaun-hoard-sorted', 3, 'take the first of the deep and lay it in the chest marked given back — the offering vaun never made. do the mercy he could not, and the cache opens.'),
-- vaun-bookshelf-tally — reproduce his "all taken, none given" count in the six slots.
('vaun-bookshelf-tally', 2, 'the shelf has six slots and his leaf has six counts. the door does not open to the right count — it opens to his count. read the taken column.'),
('vaun-bookshelf-tally', 3, 'place the books to spell all taken and none given back — the shape of what he did. the redstone completes on his refusal, and the cold click is the door.'),

-- ── MARA — the reader who never walked (turn pages / walk it) ──
-- mara-lectern-lock — turn the five books to the pages she annotated.
('mara-lectern-lock', 2, 'each of mara''s books has one page she marked. turn each lectern to its marked note and watch the lamps — a wrong page is not a wall, only a dark lamp.'),
('mara-lectern-lock', 3, 'set all five books to the pages she annotated; the lamps light one by one as you do. the alcove opens on a single line: walk the rite you have only read.'),
-- mara-walk-the-map — go to the marker row and bow together, all of you.
('mara-walk-the-map', 2, 'mara read the rite and never walked it. do not type her sentence. go where it sends you, and there, do as it says — together, none left at the door.'),
('mara-walk-the-map', 3, 'travel to the marker row and bow, all of you at once, with everyone present. that is the part she could not give: a map is not a road walked.'),

-- ── SELLA — the drowned child (reflect / overlay / stand and see) ──
-- sella-reflection-bearing — the blank stone reads only in the water.
('sella-reflection-bearing', 2, 'sella speaks only as a reflection now. her shore stone is blank in the day. face the water, and read what the surface gives back.'),
('sella-reflection-bearing', 3, 'stand at the shore pool and look at the reflection — the rune reads only inverted, in the water. it is a bearing: the far water, where the reeds fold back.'),
-- sella-overlay-lake — two maps become one place.
('sella-overlay-lake', 2, 'in her chest at the far water are two maps, each a meaningless scatter alone. they are not two places. they are one, twice.'),
('sella-overlay-lake', 3, 'frame the two maps on the same wall grid so one lies over the other. the blue and grey resolve into a shore with an x — the place the child drew, where she went.'),
-- sella-shore-memorial — stand on the one block and see the bird.
('sella-shore-memorial', 2, 'from the ground it is only debris. there is one worn standing-stone above the pool. a mason left it there for a reason.'),
('sella-shore-memorial', 3, 'stand on the worn stone above the pool and look down. the scatter resolves into a bird over water — her deep-bird, the thing she kept. seeing it is the whole of it.'),

-- ── ORIN — the mason who would not bow (bow / find the key / rotate) ──
-- orin-bow-fall-order — bow at the six markers in the order they fell.
('orin-bow-fall-order', 2, 'orin would not bow, and his threshold will not open to a word. six markers stand there, each a keeper''s mark. bow at each — but the order is not the order they stand in.'),
('orin-bow-fall-order', 3, 'crouch at the markers in the order the keepers fell: vaun, mara, sella, orin, brann, iss. a wrong order does no harm, only nothing. bow where he would not, and his stone reads.'),
-- orin-banner-heraldry — the banner sigil is the key to his stone.
('orin-banner-heraldry', 2, 'six banners hang in his hall, one sigil each. you have been grinding his stone the hard way. the mason''s square is not decoration — it is the key.'),
('orin-banner-heraldry', 3, 'read orin''s own banner — the mason''s square — as the substitution alphabet, and lay it over his stone. the plaintext you could not force comes plain. the work was in noticing.'),
-- orin-frame-dials — turn the dials to face as the markers face.
('orin-frame-dials', 2, 'six frames, each a dial with an arrow. you have already walked his markers. the niche echoes them.'),
('orin-frame-dials', 3, 'rotate each dial to point the way its marker faces, in fall-order. the clicks are wrong until they are right; when they are, the niche gives up the one offering he kept.'),

-- ── BRANN — the watchman on the black moon (come at night / go in silence) ──
-- brann-black-moon-toll — the toll rings only in the dark.
('brann-black-moon-toll', 2, 'brann speaks only at night, and this rings only on the black moon. in daylight the tower is silent — you are not stuck, only early. come when it is dark.'),
('brann-black-moon-toll', 3, 'wait for the black moon and listen at his tower. the toll is a rhythm, and the rhythm is a word — the one he could not stop saying: awake. do not close your eyes here.'),
-- brann-silence-corridor — pass the sculk corridor without a sound.
('brann-silence-corridor', 2, 'his watch-walk hears you. the sculk answers every sound — a step run, a block struck, a blow. it does not answer the quiet.'),
('brann-silence-corridor', 3, 'sneak the whole corridor, break nothing, strike nothing, and reach the far door in silence. the shriekers tell you the instant you are too loud. pass unnoticed, as he did in the dark.'),

-- ── ISS — the liar (cross-check / inspect the gift / speak it again) ──
-- iss-which-is-true — the warm account against the cold land.
('iss-which-is-true', 2, 'iss says the ways are a wall against the watching. the cold hearth he sent you to says otherwise. so does the later stone. which does the land itself agree with?'),
('iss-which-is-true', 3, 'his warm reading and the cold facts flatly disagree, and the land keeps the proof. the ways were never a wall — no wall was ever built here. he lied about the wall.'),
-- iss-nbt-falsified-entry — the record he doctored, hidden in a warm gift.
('iss-nbt-falsified-entry', 2, 'the warmest keeper leaves the warmest gift. look inside it, not at it — the keepsake carries more than it shows.'),
('iss-nbt-falsified-entry', 3, 'inspect the lamp''s hidden field; it decodes to a line and a path. it is the entry he doctored — that the seventh was a mercy, spared. go correct it. he counted on no one looking.'),
-- iss-bound-word-callback — speak the earned name again at the deep gate.
('iss-bound-word-callback', 2, 'this is not a new reading. it is one you already earned. the deep gate quotes it back at you and asks you to say it again.'),
('iss-bound-word-callback', 3, 'the bound word you drew from his stone binds the deep. speak again the one who turned away, here, and the gate toward the threshold gives.'),

-- ── CROSS-KEEPER / SPINE (research / co-op / speak / observe) ──
-- spine-recovered-archive — the salvaged folder + the spectrogram name.
('spine-recovered-archive', 2, 'what was recovered is kept off the record. the string on the sign is where. most of what is there is only lore — but one image is not an image.'),
('spine-recovered-archive', 3, 'open the archive the sign points to, and read the waveform image as a spectrogram. a name is written in it — the one the record would not keep.'),
-- spine-threshold-vault — each of you holds a piece; read them aloud together.
('spine-threshold-vault', 2, 'each of you sees runes the others cannot. no one holds the whole combination. the wall is not the puzzle — the difference between what each of you sees is.'),
('spine-threshold-vault', 3, 'read your own runes aloud and combine them with the others'' — the code is only whole when the roster is whole. the assembled combination makes the keys; the keys open the vault.'),
-- spine-spoken-name — say the truth aloud and it is heard.
('spine-spoken-name', 2, 'this one is not typed at all, and it never blocks you. it is only heard — if you say the true thing aloud, where the listening is.'),
('spine-spoken-name', 3, 'say the one who turned away out loud, and wait. what is heard is kept: the words come back, cut where you will pass. you do not solve this. it hears you.'),
-- spine-unkept-acrostic — the six marks, read in fall-order.
('spine-unkept-acrostic', 2, 'there is one small glyph framing each keeper stone — six marks, one word. the order is not the order they stand in. it opens nothing; it only recolors.'),
('spine-unkept-acrostic', 3, 'read the six maker''s-marks in the order the keepers fell — vaun, mara, sella, orin, brann, iss. they spell the word each of them did not keep: unkept.'),
-- spine-cold-hearth-shadow — the one fire let go out.
('spine-cold-hearth-shadow', 2, 'every home in the hold keeps one fire, always. count them if you like — they all burn. all but this one. do not look for a broken custom. look for the absence of a fire.'),
('spine-cold-hearth-shadow', 3, 'this is the false walk — a warm man''s word led you here, and here is a hearth gone cold all through, the only fire in the hold let go out. it answers nothing. it asks: why was this one home not kept.')

on conflict (puzzle_key, tier) do update set body = excluded.body;

commit;
