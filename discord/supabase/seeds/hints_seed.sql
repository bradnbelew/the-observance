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
('stone-brann', 3, 'read the lit lamps in their rows, top to bottom, the way a watchman counts down a black moon. the sequence spells what he could not say twice the same.')

on conflict (puzzle_key, tier) do update set body = excluded.body;

commit;
