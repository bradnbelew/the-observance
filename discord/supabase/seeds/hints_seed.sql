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

-- Retired/non-open puzzle cleanup. Because this seed upserts hints, old live databases
-- need explicit deletes or /whisper can keep teaching dead/non-open mechanisms:
-- stone-sella's Atbash was replaced by sella-reflection-bearing; base-docket-reread
-- is owned by base-docket-reread-auto; record-receives is a world response, not an
-- oracle-open puzzle.
delete from public.hints
where puzzle_key in ('stone-sella', 'base-docket-reread', 'record-receives');

insert into public.hints (puzzle_key, tier, body) values

-- rosetta-ring — the rune-literacy on-ramp (assemble the seven ways, in ring order).
('rosetta-ring', 2, 'these marks are not decoration. each one sits beside the thing it names: bow marker, offering cairn, kept lamp, deep line, shut mouth, living bird. read the ring in the order it gives you.'),
('rosetta-ring', 3, 'the ring is asking for the seven way names, not a sentence. give the names in ring order: bow, offering, kept light, deep line, unspoken, sacred beast. keep the spaces ordinary.'),

-- stone-vaun — Caesar (every letter held back by a fixed amount; his hoarding made literal).
('stone-vaun', 2, 'vaun gave nothing back. even his letters are held back — every one, by the same measure. find the measure and give them back.'),
('stone-vaun', 3, 'each mark stands for a letter shifted a fixed count down the row. try the counts one by one; when the words come clear, that is his.'),

-- stone-mara — book cipher (page/line/word into the lectern shelf she kept).
('stone-mara', 2, 'mara read and did not walk. the numbers on her stone are not the answer — they are where to look. she left the books.'),
('stone-mara', 3, 'three numbers to a word: the page, the line, the word along it. walk her shelf, count to each, and the sentence assembles itself.'),

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
('a1z26-tick-stave', 2, 'these are counts, not words — but you are not meant to be handed the trick. count what is set out in the world in ones, and let it tell you.'),
('a1z26-tick-stave', 3, 'each cluster is a number and each number a letter in its order; you will have proved this to yourself at the counting-stones before you need it here.'),

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

-- seventh-name — THE SEVENTH READING capstone (the six keepers each kept a letter of the name, in their
-- own tongue; read in the order they fell). The whisper points at the READING, never the name itself.
('seventh-name', 2, 'the seal is a name, and no one hand holds it. the six kept a letter of it each, and each could keep it only the one way they knew. go back to them. read each stone the way that stone taught you — vaun''s shift, mara''s shelf, sella''s water, orin''s marks, brann''s rails after dark. the first mark of what each left you is the letter they kept.'),
('seventh-name', 3, 'read the six in the order they fell — vaun, mara, sella, orin, brann, iss — and take the first letter each gives. five will give you their letter true. the sixth is iss, and iss lies once more: read his straight and he tells you the last letter warm and wrong; read the first mark of each of his lines down, the way you read his wall, and he gives you the true one. say the six together, and the seal gives.'),

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

-- base-docket-reread-auto — the offline twin (same payoff, whichever the world serves live).
('base-docket-reread-auto', 2, 'you read the down-count as a doom-clock. read it again, after the catch. it was never counting the dark.'),
('base-docket-reread-auto', 3, 'the muster is being read. the count was never of the dark — it was of the hands, and the hands are almost all in. it is a roll call, not a doom clock.'),

-- meta-unkept — the six maker''s-marks, read in fall-order, carry one word.
('meta-unkept', 2, 'there is one small glyph framing each keeper stone — a maker''s mark, one to a stone. six marks, one word. the order is not the order they stand in.'),
('meta-unkept', 3, 'read the six maker''s-marks in the order the keepers fell — vaun, mara, sella, orin, brann, iss — not the order on the ring. the word they spell is the one each of them did not keep: unkept.'),

-- bound-word — the Iss vigenère plaintext IS the coop-gate''s need (the convergence word).
('bound-word', 2, 'the catch re-cut iss''s stone. read it now with his own name laid over it, and it yields a single bound word — the word another gate is waiting to be given.'),
('bound-word', 3, 'the catch re-cut his stone; the key is still the man. lay his own name over the fresh marks, letter against letter, and read the single word that comes. that word is the bound word — carry what you read to the gate that waits on it.'),

-- m4-three-hands — the cross-surface co-op gate (three acts held together; not typed).
('m4-three-hands', 2, 'this gate does not open to a word alone. it opens to three things held together — a foot, a carve, a word posted here — while the square is awake.'),
('m4-three-hands', 3, 'one of you stands on the plate, one cuts the mark, one posts here, and the square must still be awake when the word is spoken. no single hand clears it. hold the three together and the threshold opens.'),

-- threshold-coordinate — the threshold carving yields the TRUE coordinate (a road, not an answer).
('threshold-coordinate', 2, 'the opened threshold is carved with a mark that points. it is not an answer to type — it is a direction to walk. follow where it sends you.'),
('threshold-coordinate', 3, 'read the threshold mark as a bearing and walk it to the end. the true coordinate was never a word; it is the road iss hid. what you type is the word waiting for you where it points.'),

-- true-walk-arrive — the true walk endpoint (read the destination word on-site; presence-gated).
('true-walk-arrive', 2, 'the road kept its word. you have to be standing where it ends to read what it left — the answer is carved there, not here.'),
('true-walk-arrive', 3, 'walk the true road to its end and read the leaves placed at the tableau. you were filed here before you came: kept here before you. the road kept its word.'),

-- prior-absence — the prior-run roster before the failed camp gate.
('prior-absence', 2, 'do not start inside the camp. read the roster before it. the old group had the stones, the answers, and the tokens. one condition is named by its absence.'),
('prior-absence', 3, 'the roster says six names, six keeper answers, six tokens prepared, then the correction line: no witness. file that condition plainly before you try to enter the old camp.'),

-- prior-camp-refusal — the camp proves why six answers were not enough.
('prior-camp-refusal', 2, 'the camp did not fail because they lacked solutions. read both lecterns and the blank place. the room is separating finished answers from something that can stand outside the finish.'),
('prior-camp-refusal', 3, 'the failed record says the floor took their tokens and returned nothing because no one outside the circle could say what was true. answers are not a witness.'),

-- prior-vaun-correction — repair Vaun's prior-run file.
('prior-vaun-correction', 2, 'vaun''s barrel points back to debt before inventory. compare it with the market and ration evidence; the old file made counting sound like holiness.'),
('prior-vaun-correction', 3, 'the correction line in vaun''s barrel is the answer shape: return first before count. the first thing was already owed before anyone counted it.'),

-- prior-mara-correction — repair Mara's prior-run file.
('prior-mara-correction', 2, 'mara''s barrel points back to the walked route. the old file treated a copied sentence like proof; the living test is whether anyone actually walked it.'),
('prior-mara-correction', 3, 'file the correction as walk it before filing it. a read route changes paper; a walked route changes the walker.'),

-- prior-sella-correction — repair Sella's prior-run file.
('prior-sella-correction', 2, 'sella''s barrel points at later ink, school, water, and the seventh count. the old file called the addition an error because the count looked cleaner without it.'),
('prior-sella-correction', 3, 'file the correction as count the seventh before the six. later ink is still evidence; it is how the absence was added back.'),

-- prior-orin-correction — repair Orin's prior-run file.
('prior-orin-correction', 2, 'orin''s barrel points back to the crouch and the threshold. the old file treated the bow as a price paid to the room. read what the posture actually proves.'),
('prior-orin-correction', 3, 'file the correction as bowing is proof not payment. smallness is how the stone is read without turning it into a possession.'),

-- prior-brann-correction — repair Brann's prior-run file.
('prior-brann-correction', 2, 'brann''s barrel points at the toll and the silence corridor. the old file stopped at hearing the warning; the watch still had to be kept afterward.'),
('prior-brann-correction', 3, 'file the correction as the watch must be kept. a warning heard once is not the same as staying awake through the count.'),

-- prior-iss-correction — repair Iss's prior-run file.
('prior-iss-correction', 2, 'iss''s barrel points back to the warm wall and the cold land. do not ask whether the sentence comforts you; ask whether the world agrees with it.'),
('prior-iss-correction', 3, 'file the correction as test warmth against the land. comfort is not proof; the warm wall was cover over the count.'),

-- prior-witness-before-accepting — synthesize the failed camp before the final floor.
('prior-witness-before-accepting', 2, 'after all six correction files are entered, go to the failed accepting floor below. it is not asking for another keeper answer; it is asking for the condition missing from the old attempt.'),
('prior-witness-before-accepting', 3, 'the failed floor shows six old tokens and one blank relation. name the condition exactly: witness before accepting. then the last warm gate and the real rite can matter.'),

-- ── MOVEMENT V — the Accepting (acts and rites; the whisper points at what to do) ──

-- rite-tokens — lay one personal token per slot; the missing tool is you.
('rite-tokens', 2, 'the floor will not take a token from a thin case. the keepers, copied village, old places, surface kindnesses, and failed accepting correction must all be filed first. this is evidence before rite.'),
('rite-tokens', 3, 'before the slots answer, the record needs the six keeper theories, all eight unlit house recoveries, the thirteen named side proofs, the two surface kindnesses, and the prior witness condition. then lay one personal token in each slot with the named components. the missing tool is the giver.'),

-- pressure-glyph-walk — walk the rune the floor names, footstep by footstep (do not decode it).
('pressure-glyph-walk', 2, 'the floor names a rune. do not decode it. this is mara''s lesson made a door — the shape is walked, not read.'),
('pressure-glyph-walk', 3, 'trace the rune the altar floor carries with your own feet, step by step, until the whole shape is walked. it is a second way into the accepting. do not read your way out of it.'),

-- accepting-crouch — everyone present bows as one, at the hour, in the kept light (detected).
('accepting-crouch', 2, 'there is no chosen one here, and no word to type. the rite asks all of you at once. what is the smallest of the ways — the one orin thought too small to matter?'),
('accepting-crouch', 3, 'everyone present bows together, as one, in the kept light, at the hour. the bow is the smallest way and the whole of it. no one is left standing; no one goes first. do it together and the record opens.'),

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
-- media-prior-base — frame-scrub found footage; map/frame token + count 13.
('media-prior-base', 2, 'the base recording is not asking for a place. scrub the cut frame, then read the named map and the count that keeps repeating after thirteen seconds.'),
('media-prior-base', 3, 'one frame gives ash. the torches and the drift count give thirteen. speak it together: ash thirteen.'),
-- media-far-water — found footage gives a place-bearing phrase, not another count.
('media-far-water', 2, 'the shore clip gives the phrase in pieces. read the map label, the reed frames, and the water-reflected sign as one sentence.'),
('media-far-water', 3, 'the words assemble as a place: where the reeds fold back. it is not a name. it is where the water wants you to return.'),
-- media-black-moon-toll — count the toll groups and the late ninth light.
('media-black-moon-toll', 2, 'do not treat the tolls as mood. count the close group, then the second group, then the late ninth sound after the pause.'),
('media-black-moon-toll', 3, 'four tolls, then five. the watch rule is the phrase: stay awake. the late ninth toll only proves whose night it is.'),
-- media-release-room — late checksum; six visible returns, one absence.
('media-release-room', 2, 'this clip is late because it is a checksum, not a spoiler. count what returns to the room, then name the absent space.'),
('media-release-room', 3, 'six return. one is not kept. that is the whole late-room instruction; it tells you how to approach, not what name to guess.'),
-- spine-recovered-archive — the salvaged folder + the spectrogram sentence.
('spine-recovered-archive', 2, 'what was recovered is kept off the record. the string on the sign is where. most of what is there is only lore — but one sound image is not only a picture.'),
('spine-recovered-archive', 3, 'open the archive the sign points to, and read the waveform image as a spectrogram. the hidden sentence is the answer: i was not kept.'),
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

,

-- MISSING-RESCUE FLOOR (puzzlefairness). These are live lore/dead-end/payoff rows that
-- can still stall, mislead, or feel ignored if the group cannot ask the Watcher for the
-- second and third rail. Kept in one late block so the launch audit has a clear owner.
-- m1-record-opens - the buried base lectern line; lore acknowledgement, not a gate.
('m1-record-opens', 2, 'the first record is not hidden far from you. read the report as if it already knows who stands before it; the line about the living is the one that answers.'),
('m1-record-opens', 3, 'on the base lectern, say the sentence that names what the record does: the record counts the living by name. it is the opening truth, not a door.'),
-- m1-named-habit - the report naming a player habit before the rules are known.
('m1-named-habit', 2, 'when the report names something you do before the rite names it, do not treat that as flavor. it is the dread. the record was measuring before it explained the law.'),
('m1-named-habit', 3, 'answer what happened to you: it named my habit before i knew it was a custom. this keeps no road; it only proves the record was already watching.'),
-- iss-warm - the warm misreading, with the cold acrostic rescue.
('iss-warm', 3, 'read the warm lines two ways. across, they comfort you and send you to the cold hearth. down the first letter of each warm line, they confess the colder thing: no wall.'),
-- m2-rhyme - the keeper fates rhyme with the customs they broke.
('m2-rhyme', 2, 'set two keeper stones beside one another in your head. the words change, but the shape does not: each fate answers the custom that keeper broke.'),
('m2-rhyme', 3, 'vaun kept and was counted, mara read and did not walk, sella reflected and drowned, orin would not bow, brann watched the fire, iss made comfort into a wall. the stones are warnings shaped like you.'),
-- iss-dead-shrine - the false road that works and still saves no one.
('iss-dead-shrine', 2, 'iss did not send you nowhere. that is why the lie holds. the place is real, the hearth is cold, and the fact that it opens nothing is the answer.'),
('iss-dead-shrine', 3, 'stand at the cold hearth and name what it is: the dead shrine, a place that keeps no road on. west and down was true, and that is why the truth is dangerous.'),
-- name-where - the place-filing reread; true, terminal, and personal.
('name-where', 2, 'the new carving is not about strangers who came before you. it files the living by place. read the name and the ground beside it together.'),
('name-where', 3, 'say the shape of the reread: the record files the living by place, not only by name. before you was never about strangers. it opens nothing because the proof is you.'),
-- record-url - the off-world provider directory and Record path, decoded from the founder line and Hold copy.
('record-url', 2, 'the old customer trail has a provider, a common-web company name, and a directory number. read it as a filing reference, not as the live minecraft address.'),
('record-url', 3, 'copperline hosting is the retired provider on the common web. search its public server directory for service 1842, then follow the hand that filed the row to the recovered copy.'),
-- difficulty-mara - the fairness plant; the land''s grip changes with the living.
('difficulty-mara', 2, 'mara left this as a sentence, not a mechanism. read the cruelty she names: the land counts the quick more closely and opens around stumbling hands.'),
('difficulty-mara', 3, 'the line is the point. the record keeps a closer count of the quick; it closes on those who run ahead and opens for those who stumble. this colors the rite; it does not unlock one.'),
-- reckoning-rosetta - the digit-literacy stone; counts become marks, marks become roads.
('reckoning-rosetta', 2, 'you learned the letters from the ring. this stone teaches the other marks: low bar, ticks, breaks. they are counts before they are words.'),
('reckoning-rosetta', 3, 'read the counting marks as digits, not sounds. the low bar marks a minus and the double tick marks a break. once you can count it, later roads stop pretending to be gibberish.'),
-- nether-forge - the deep fire-source payoff; the slab word is the answer.
('nether-forge', 2, 'below the below, the fire is not owned. the slab does not ask for a coordinate. it names the condition of the fire itself.'),
('nether-forge', 3, 'read the word cut at the forge: lent. the fire is lent; you do not own it, you carry it. the kept light upstairs came from here.'),
-- end-seventh-out - the End payoff; the Seventh''s own words outside the record.
('end-seventh-out', 2, 'this is outside the record, so do not ask the record to explain it first. read the shrine in its own voice: the seventh kept the ways and still went out.'),
('end-seventh-out', 3, 'the End shrine says the part the Hold could not keep: the keeping was never the price. you only came to look. exile is the other side of keeping.')

on conflict (puzzle_key, tier) do update set body = excluded.body;

commit;
