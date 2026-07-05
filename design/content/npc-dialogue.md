# npc-dialogue.md — the five surface NPC dialogue trees (DRAFT / design content)

> **2026-07-05 audit note: the SHIPPED implementation is simpler than this spec, by design
> — read the code, not just this file, before extending it.** `plugin/src/main/java/com/
> observance/watcher/signal/listener/TownsfolkNpcListener.java` is a flat index-cycling
> scheme (no `node_key` state machine), conduct is read from local `PlayerSignals` totals
> (not `punishment_state`), there is no `press` node, and Dob's cursor is not depth-gated.
> All five NPCs' lines were copied byte-for-byte from this file into the listener's
> embedded table (verified), so the TEXT below is still the live source of truth — only
> the delivery MECHANISM described in this front-matter (Citizens2/ZNPCsPlus state
> machine) is aspirational, not what's running. This is a legitimate simpler design the
> listener's own javadoc justifies (no showrunner round-trip, casual chat), not a bug —
> flagging only so a future author doesn't try to debug/extend against a contract that
> isn't the real one.
>
> **DRAFT. Authoring file, not the live seed.** Content for `npc_dialogue_state` +
> `npc_quests` (migration `0005_threads.sql`), to be driven through Citizens2/ZNPCsPlus.
> Grounded line-for-line in `arc/corpus/npc-and-watcher-voice.md` (SET A — the surface
> NPCs) and `arc/WORLD-BIBLE.md`. The five are **Aro** (lies), **Wenna** (half-remembers
> the ways as folk-charm), **Coll** (trader, flat truth), **Dob** (descends with the
> group, honest to a fault), **Old Pell** (won't descend; remembers your conduct).
>
> **Register law (hard):** every line below is HUMAN — modern-rough, contracted,
> capitalised, feelings named out loud where a person would. NONE of it is the Watcher's
> register. A screenshot of any line here next to a `voice.ts` line must read as two
> different species of author. The de-slop law from the corpus still binds: no "a
> testament to" / "little did they know" / "the air was thick" / tidy thematic bows /
> three-adjective lists / "not just X but Y". Show through concrete mundane detail and
> omission. The corpus's SET A is the gold standard; these reuse its lines and extend
> them in the same hand.
>
> **No story in the engine (INV-1, the voice rule).** The plugin/Citizens layer reads a
> `node_key` + the per-player dossier and posts the bound text VERBATIM. The text itself
> lives behind keys — proposed at the foot of this file as an `npcVoice` registry
> (the HUMAN twin of `voice.ts`; integrated under the build guards, never inlined at a
> call site). The trees below give each node its `npcVoice` key.

---

## 0. How a tree runs (the shared contract)

All five NPCs share one branch shape, taken straight from the corpus SET A branch keys
(`greet` / `rumor` / `truth_or_lie` / `react_good` / `react_bad`) plus the conversational
glue (`menu`, `bye`) the runtime needs:

```
node_key        purpose
─────────       ───────
greet           opening line. picks a conduct-skin (neutral / warm / cold) from the dossier.
menu            the choice hub: offer {rumor, truth_or_lie} and any quest line.
rumor           the gossip/lead. ONE rumor points at a real travel destination (a sites.yml
                anchor / a puzzle node), so the leg pays a debt. cycles through a small set.
truth_or_lie    the NPC's defining tell — a LIE (Aro), GARBLED-TRUTH (Wenna), FLAT-FACT
                (Coll), BLURTED-HONESTY (Dob), HARD-TRUTH (Pell). The world later
                corroborates or contradicts, per the corpus.
react_good      shown when the dossier reads KEPT conduct (see §0.2). Conduct is never
                named as "the meter"; the NPC says what a person says (you look rough / you
                came back / you didn't).
react_bad       shown when the dossier reads BROKEN conduct.
bye             exit line, conduct-skinned.
```

### 0.1 State (what `npc_dialogue_state.state` jsonb holds, per (player, npc_key))

The cursor + per-NPC flags. jsonb so trees evolve without a migration (the schema note in
`0005`). The shape every NPC uses:

```jsonc
{
  "cursor": "greet",            // current node_key; the runtime advances it
  "seen_greet": true,           // greet only does its full intro once; later visits short-greet
  "rumors_heard": ["aro_r1"],   // which rumor variants are spent (so they cycle, don't repeat)
  "told_truth_or_lie": false,   // the defining tell fires once; afterward it's referenced, not re-said
  "conduct_skin": "neutral",    // "neutral" | "warm" | "cold" — last computed skin (cached for re-greet)
  "iss_cold": false             // sticky: once an Iss-adjacent NPC turns cold on iss_caught, it stays
}
```

`npc_quests` (separate table) holds the fetch/return quests these trees offer
(`status`: offered → active → done/failed). Only Wenna and Coll carry a quest in this
pass; Dob's "stay with me" beat is a flag, not a fetch-quest.

### 0.2 Reading conduct off the dossier (NOT a new measurement)

The skin (`neutral` / `warm` / `cold`) is computed at `greet` from the SAME state the
engine already keeps — no NPC-specific meter. Resolution order:

```
1. ISS-COLD OVERRIDE (only Aro + Pell):
     if arc_state.flags.iss_caught == true AND this NPC is Iss-adjacent
       → skin = "cold", and set state.iss_cold = true (sticky thereafter).
   (Wenna/Coll/Dob are NOT Iss-adjacent; iss_caught does not flip them.)

2. CONDUCT, from punishment_state for this player, aggregated across the seven ways:
     broken := any custom_key with toll_tier >= 2 (named/warned) AND deciphered == false
     kept   := no way at toll_tier >= 2 (nothing standing warned-and-unlearned)
     if broken  → skin = "cold"
     if kept    → skin = "warm"
     else        → skin = "neutral"   (early game, nothing measured yet)
```

So a player who broke the Bow, was warned, and never learned it reads "cold"; a player
who kept the ways (or learned each way they slipped) reads "warm". `neutral` is the honest
early-game state before anyone has been observed — the NPCs treat a stranger as a stranger.

**Why two cold doors.** `cold` from conduct is *recoverable* (atone / decipher a way and
the next greet can read warm again). `iss_cold` is *sticky* — once `iss_caught` flips, Aro
and Pell know the group walked the liar's road, and that does not un-happen. Both render
the same cold lines; only the recoverability differs.

---

## 1. ARO — the rumor-broker who lies   ·   `npc_key: aro`

Warm, specific, confident, and half-wrong on purpose. Never descends. **Iss-adjacent:** Aro
is the one who sold "cross the painted line, that's where it gets good" — Iss's exact lie,
secondhand. When `iss_caught` flips, the group has learned the line was a grave (the corpus
`iss-dead-shrine` dead end + L06/L08), and Aro can't meet their eyes.

```
TREE: aro
─────────
greet ──▶ menu ──▶ { rumor (cycles aro_r1..r3) , lie (aro_lie, once) }
                      │
                      └── react_good / react_bad chosen at greet, colour the whole convo
bye
```

| node_key | npcVoice key | when shown | text |
|---|---|---|---|
| `greet` (neutral, first) | `aro.greet.neutral` | first meet, no conduct read | "Ah — fresh boots. Sit, sit, you're letting the cold in. You want the way down, you want the right person, and lucky you, here I am." |
| `greet` (neutral, repeat) | `aro.greet.again` | seen before, still neutral | "You're the lot poking round the old hole, yeah? Course you are. Everyone is, this season. Drink first. The hole's not going anywhere." |
| `greet` (warm) | `aro.greet.warm` | `react_good` | "Huh. You went down and you came *back* up, and you came back — quiet. Most don't. Most come back loud or don't come back. You're alright, you. Sit. On the house." |
| `greet` (cold, conduct) | `aro.greet.cold` | `react_bad` | "...you don't look so good. No offence. You've got that — that grey on you. Bunch came through last month with that same grey and I, ah. I don't see 'em anymore. Tab's closed. Go on." |
| `greet` (cold, iss) | `aro.greet.iss_cold` | `iss_caught` sticky | "You found what's past the line, then. Yeah. I can tell by your faces. Look — I never *been* down there, I just say what sells, that's all I — don't. Don't tell me about it. I don't want it in my head with the rest of the things I say." |
| `rumor` 1 | `aro.rumor.town` | menu | "Way I heard it, there's a whole town down there. Lamps still burning. People who just — stayed. Living fat off the warm while we freeze our backsides up here. That's why nobody comes back up, see. Not 'cause they died. 'Cause it's *nice*." |
| `rumor` 2 (→ DEST) | `aro.rumor.line` | menu | "There's a line painted across the big stair, halfway down. Don't mean nothing. Old paint. Builders' mark. People make a whole religion out of a stripe of pitch, I swear. You want to see it, it's down past the lamp-house, on the Stair." |
| `rumor` 3 (→ DEST) | `aro.rumor.bird` | menu | "They say there's a bird down there older than the digging. Keeps the air sweet. You find the bird, you find the bottom, and the bottom's where they kept the good stuff. Coops were up at the Lamp-works, last anyone said." |
| `lie` (the tell) | `aro.lie.cross` | `truth_or_lie`, once | "The painted line? Step right over it, friend. That's the locals keeping the soft folk out so they can have the warm to themselves. Cross it and keep going. That's where it gets good." |
| `lie` (alt) | `aro.lie.moon` | if `cross` spent | "Sleep wherever you like down there. Black moon, white moon, no moon — rock doesn't care what the sky's doing. That's a tale to sell candles." |
| `bye` (neutral/warm) | `aro.bye.warm` | exit | "Mind how you go. Come tell me what you find — I'll make a good story of it either way." |
| `bye` (cold) | `aro.bye.cold` | exit, cold | "I'm out of stories for you. First time in my life. Just — don't tell anyone where you heard the line was safe to cross, yeah? Don't put that on me." |

**Travel destinations seeded:** `aro.rumor.line` points at the Stair / **the Deep Line**
(the `iss-dead-shrine` red-herring lane and `no-wall-catch` catch; sites.yml: the Stair).
`aro.rumor.bird` points at the **Lamp-works** coops (PLACE thread; `stone-brann` /
deep-bird). Aro's `lie.cross` is the SAME lie Iss carved (`stone-iss-wall` / `iss-warm`);
the world contradicts it at `iss-dead-shrine` (a grave, not warmth). That contradiction is
what later makes `iss_caught` land on him.

**Transitions:**
```
greet → (compute skin) → menu
menu  → rumor[next unspent in rumors_heard cycle] → menu   (mark spent; when all spent, menu
         shows only lie + bye)
menu  → lie  (set told_truth_or_lie; first call = aro.lie.cross, later = aro.lie.moon) → menu
menu  → bye  → close (cursor reset to greet; seen_greet stays true → next visit short-greets)
on iss_caught at greet: skin=cold(iss), state.iss_cold=true (sticky); rumor/lie still offered
   but lie now reads as the thing he regrets (Aro deflects: "don't ask me about the line").
```

---

## 2. WENNA — half-remembers the ways as folk-superstition   ·   `npc_key: wenna`

The accidental Rosetta. Everything she does is a softened, corrupted survival rite, and she
half-knows it. Warm, chatty, performing the wise-woman a little. Never descends. **Not
Iss-adjacent** — `iss_caught` does not flip her; she only ever turns "cold" the recoverable
way, and even then it's worry, not coldness. Her `truth_or_lie` is GARBLED-TRUTH: she has
the ways right *under the charm*. She is the human teacher that makes discover-by-punishment
fair (INTEGRATION §3.4 — learn a way from a person, not just a doused torch).

```
TREE: wenna
───────────
greet ──▶ menu ──▶ { rumor (cycles the six-she-remembers) , truth (the real rite under it) , crust_quest }
bye
```

| node_key | npcVoice key | when shown | text |
|---|---|---|---|
| `greet` (neutral, first) | `wenna.greet.neutral` | first meet | "Mind the lamp by the door, love, don't pinch it out. House likes to look lived-in after dark. Gran's rule, not mine, but I've never had cause to break it." |
| `greet` (neutral, repeat) | `wenna.greet.again` | seen before | "Back again. Good. Take a crust for your pocket — no, I won't hear it, you take the crust. You leave a little, you get to keep a little. That's the whole of it, near enough." |
| `greet` (warm) | `wenna.greet.warm` | `react_good` | "Oh, you minded it all, didn't you. I can tell. You've got the — the *kept* look. Gran would've liked you. She'd have given you the good chair." |
| `greet` (cold) | `wenna.greet.cold` | `react_bad` | "...did you leave a little? Down there. Did you give anything back, or did you just — take. You don't have to answer. I can see you didn't. Take the crust anyway. Maybe it's not too late for the crust." |
| `rumor` 1 | `wenna.rumor.seven` | menu | "Gran used to say there were seven somethings you had to mind down there. Seven. I only ever remember six and I always forget a different one, isn't that the way. Light, and the line, and the bird, and the bowing, and the giving, and... see, there's the sixth gone again." |
| `rumor` 2 | `wenna.rumor.name` | menu | "You don't say the cold's name. That one I do remember, 'cause she'd go white when I tried. 'You don't *name* it, Wenna.' Name what, Gran? And she'd just — wouldn't. So I don't. Habit now." |
| `rumor` 3 | `wenna.rumor.moon` | menu | "When the moon goes black you stay up. Stupid, isn't it. I still do it. Sit up all night with the lamp like a fool. Slept through it once as a girl and had the worst dreams of my life, so." |
| `truth` 1 (the rite) | `wenna.truth.bow` | `truth_or_lie` | "Bow at the stones. I don't know who to, mind. Gran never said who. You just bend your knee going past and you don't think too hard about it. The ones who don't bend... she'd just shake her head." |
| `truth` 2 (the rite) | `wenna.truth.light` | `truth_or_lie`, alt | "Keep your light. Above all the others, keep your light. That one she said like it mattered more than the rest put together, and she didn't say things like that twice." |
| `crust_quest` offer | `wenna.quest.offer` | quest, once | "Do me a kindness while you're down there. There's a little shelf-stall, sells nothing, kept lit for the dead — leave the crust there, not in your pocket. Gran's gran kept that stall. I never can go myself. You'll do it? Good." |
| `crust_quest` done | `wenna.quest.done` | on return, quest done | "You left it. At the dead-stall. I didn't tell you where it was and you found it and you left the crust. You don't know what that — no, you do, I think. I think you know exactly what that was." |
| `bye` | `wenna.bye` | exit | "Go on, love. The lamp'll be lit for you. I mean that the ordinary way *and* the other way, if there is an other way, which I've never quite decided." |
| `bye` (cold) | `wenna.bye.cold` | exit, cold | "I'm going to light a second lamp tonight, I think. After seeing you. No reason. Just feel like the house wants two lit, with you stood there like that." |

**Travel destination seeded:** `wenna.quest.offer` points at the **Deep Market**'s
"stall that sells nothing and is kept lit for the dead" (corpus R04 survey, mark 14). The
quest pays the_offering (Give Back to the Deep) as a TAUGHT way — completing it can set the
player's `punishment_state.deciphered=true` for `the_offering` via `teaching_site_id`
(discover-by-punishment, the fair human-teacher path).

**The teaching map (Wenna's whole point).** Each rumor/truth corrupts a real way, so a
player who listens learns it before they break it:

| Wenna line | the way it half-teaches | corpus echo |
|---|---|---|
| `rumor.seven` | that there are SEVEN ways (she forgets one — the seventh-not-kept seed) | L04, R10 (the count won't come even) |
| `rumor.name` | `the_unspoken` (don't say the name) | R07, Sella "i drew it" |
| `rumor.moon` | `the_dark_hours` (don't sleep on the black moon) | Brann / L13 / R12 |
| `truth.bow` | `the_bow` (bend at the markers) | Orin / R08 / L03 |
| `truth.light` | `the_kept_light` (keep your lamp) | Mara / R02 / L03 |
| `quest` | `the_offering` (leave a little, give back) | Vaun's empty column / R03 / L02 |

**Transitions:**
```
greet → (skin) → menu
menu  → rumor[cycle] → menu
menu  → truth[bow|light alternating] → menu  (set told_truth_or_lie once)
menu  → crust_quest (npc_quests: offered→active; on Deep-Market dead-stall offering detected
         → done; fire wenna.quest.done; mark the_offering deciphered for the player) → menu
menu  → bye → close
```

---

## 3. COLL — the trader   ·   `npc_key: coll`

Pure transaction. Descends only to the Lamp-works, never to the Line. Clipped, dry, prices
nothing supernatural in. **Not Iss-adjacent.** His `truth_or_lie` is FLAT-ACCURACY: no
reason to lie about stock or distance, and the truth bores him.

```
TREE: coll
──────────
greet ──▶ menu ──▶ { shop (the goods) , rumor (cycles) , truth (flat fact) , oil_run_quest }
bye
```

| node_key | npcVoice key | when shown | text |
|---|---|---|---|
| `greet` (neutral) | `coll.greet.neutral` | first/repeat (Coll doesn't warm up much) | "Torches, oil, rope, three days' rations, a spare striker 'cause your first one's already wet. Don't haggle, I've heard your speech, the answer's the price on the tag." |
| `greet` (warm) | `coll.greet.warm` | `react_good` | "You came back, you're spending, you're not babbling. Model customer. Here — striker's on me. Don't tell the others I do that, it ruins the business." |
| `greet` (cold) | `coll.greet.cold` | `react_bad` | "Cash up front from you. No, nothing personal. Last three that came up looking like you settled their tab and then I never saw the coin spend again. It just... sat where they dropped it. So. Up front." |
| `shop` | `coll.shop` | menu | "Down or up? Down, you buy light. Up, you sell whatever you found that's still worth anything. Which is rarely much. People bring up the strangest junk and want gold for it." |
| `rumor` 1 | `coll.rumor.oil` | menu | "Folk come up babbling about a watcher, a presence, eyes in the dark. You know what I sell to those folk? More oil. Whatever's down there, it's never once stopped a man from needing more oil." |
| `rumor` 2 (→ DEST) | `coll.rumor.lampworks` | menu | "Furthest I go's the lamp-house — the Lamp-works, second level. Good trade there, people coming up are scared and scared pays full price. Past that? Nothing past that's worth a markup. Past that you don't come back to spend it." |
| `truth` 1 (flat) | `coll.truth.line` | `truth_or_lie` | "The painted line's real, if that's your question. I've seen it. I don't cross it. Not 'cause of stories — 'cause everyone who does stops buying oil from me, and I notice when a customer stops existing." |
| `truth` 2 (flat) | `coll.truth.twolamps` | `truth_or_lie`, alt | "Keep one lamp more than you think you need. That's not wisdom, that's stock advice. The man with two lamps comes back to spend. The man with one comes back as a story. I'd rather you came back to spend." |
| `oil_run_quest` offer | `coll.quest.offer` | quest, once | "You're going down past where I go. Fine. Take this sealed jar to the third lamp on the Lamp-works stair — it's been dark for years, some lampwright's old stand, number's worn off. Light it. I'll knock the rope off your next bill. I don't like a dark stand on my route, bad for trade." |
| `oil_run_quest` done | `coll.quest.done` | on return, quest done | "You lit it? The third one? Huh. It's been dark longer than I've sold here. Rope's free. And — nothing. Just. Good. A lit stand's a lit stand." |
| `bye` | `coll.bye` | exit | "Buy and go. You know where I am. I'm always where the oil is." |
| `bye` (cold) | `coll.bye.cold` | exit, cold | "I'll sell you the oil. I'll always sell you the oil. But I'm not shaking your hand, and I'd thank you to buy and go." |

**Travel destination seeded:** `coll.rumor.lampworks` points at the **Lamp-works** (second
level, mark 9; PLACE). The `oil_run_quest` points at "the third lamp on the Lamp-works
stair" — Mara's L01/L02 third lamp she never got the jar for, now relit by the players. The
quest pays `the_kept_light` as a taught way (the human-teacher path; `teaching_site_id` =
the Lamp-works stand). It's a quiet grief-payload: the players close a debt Vaun left open
four winters.

**Transitions:**
```
greet → (skin) → menu
menu  → shop  → (hand off to the vanilla/Citizens shop trade) → menu
menu  → rumor[cycle] → menu
menu  → truth[line|twolamps alt] → menu
menu  → oil_run_quest (offered→active; on third-stand light detected → done; mark
         the_kept_light deciphered) → menu
menu  → bye → close
```

---

## 4. DOB — descends with the group   ·   `npc_key: dob`

The only Set-A voice that follows the party past the Mouth. Afraid the ordinary way — sweaty,
talky, jumpy — the human counterweight to the Watcher's calm. **Not Iss-adjacent by the
flag**, but he is the one who NAMED Aro a liar, so when the group crosses the line on Aro's
word his fear curdles. His `rumor` slot is `descent_chatter` (running commentary that
deepens by depth); his `truth_or_lie` is HONEST-TO-A-FAULT (he blurts, admits his fear).
**The tell is the quieting** — bravado → chatter → blurted honesty → "i'll be right here."

Dob's tree is DEPTH-DRIVEN, not menu-driven: the runtime advances his cursor as the party
descends (read the deepest site the group has reached). He doesn't offer a fetch-quest;
his "stay with me" is a flag the world can honor (a lamp he won't leave).

```
TREE: dob   (cursor follows DEPTH, set by deepest site reached)
─────────
topside ──▶ lampworks ──▶ cisterns ──▶ stair(line) ──▶ [react_good | react_bad]
   greet      chatter1       chatter2     chatter3        (conduct fork at the Line)
   + truth (blurted) available at any depth from the menu
```

| node_key | npcVoice key | depth / when | text |
|---|---|---|---|
| `greet` (topside) | `dob.greet.bravado` | before descending | "Right, I've been down to the second level loads of times, loads, so just — stick behind me and we're golden. Loads of times. Twice. Twice is loads." |
| `greet` (alt) | `dob.greet.alert` | topside repeat | "I'm not scared, before you ask. I'm *alert*. There's a difference and my mum says it's a good quality." |
| `chatter` 1 | `dob.chatter.lampworks` | at Lamp-works | "See, this is fine. Lamps, smell of oil, nothing weird. People worked here. Normal job, normal — okay, why's it so *tall*, the ceiling, down here. Was it always this tall? I don't remember tall." |
| `chatter` 2 | `dob.chatter.cisterns` | near the Cisterns | "Don't drink the still water, that's Cistern 7, that one's gone bad — my uncle said. Or was it 7's the good one. One of 'em's good. Let's not test it. Let's super not test it." |
| `chatter` 3 | `dob.chatter.line` | approaching the Stair/Line | "There's the line. The painted one. We're — we're not crossing that, are we. Tell me we're stopping at the line. Aro said cross it but Aro's a liar, everyone knows Aro's a liar, why'd I even — we're stopping at the line, right?" |
| `truth` 1 (blurt) | `dob.truth.lied` | menu, any depth, once | "Okay — real talk — I've never been past the Lamp-works. I lied. Twice was a lie, it was once and I cried on the way up. I just wanted to come 'cause everyone treats me like a kid. I don't know what's down there any more than you do." |
| `truth` 2 (blurt) | `dob.truth.lamp` | menu, alt | "I keep my lamp on me. Not letting go of it. You can have my rope, you can have my rations, you cannot have my lamp, I will not be the one whose light goes out, I've *heard* what they say about the ones whose light goes out." |
| `react_good` | `dob.react.good` | at/after the Line, KEPT conduct | "I feel — okay, weirdly, I feel better next to you lot? Like the dark's paying attention but it's not — it's not paying attention to *us*. Is that mad. That's mad. Stay close though." |
| `react_good` (alt) | `dob.react.good.up` | KEPT, late | "We did it the right way, yeah? Bowed and gave the bird its seed and kept the lamps. My gran'd be made up. Let's go up. Let's go up while we're still the kind of people my gran'd be made up about." |
| `react_bad` | `dob.react.bad` | at/after the Line, BROKEN conduct | "Why'd you cross it. Why'd you — Aro said it was fine but you *knew* Aro lies, I told you he lies, and you crossed it anyway, so you didn't do it 'cause you believed him. You did it 'cause you wanted to. That's worse. Why's that worse. It feels worse." |
| `react_bad` (the quiet) | `dob.react.bad.wait` | BROKEN, deepest, the tell | "...I don't want to go further. I'll wait here. By the lamp. I'll just — I'll keep this lit and I'll wait. You go on. I'll be right here. I'll be right here. I'll be right here." |

**Travel destinations seeded:** Dob's chatter IS the leg-by-leg travel narration — it names
the **Lamp-works → Cisterns/Cistern 7 → the Stair/Deep Line** in order (the spine descent,
INTEGRATION §2.1). He never seeds a NEW destination; he colours the ones the party is
already on, and his `chatter.line` pre-discredits Aro's lie at the exact moment the choice
lands (so a group that crosses anyway does it knowingly — the corpus's whole point).

**The conduct fork (no `iss_caught` needed).** Dob doesn't read the `iss_caught` flag; he
reads the same KEPT/BROKEN skin at the LINE. KEPT → `react.good` (he feels safer near them).
BROKEN (they crossed) → `react.bad` → `react.bad.wait` (he stops, won't go on, repeats
"i'll be right here"). The quieting is the horror; no line names a feeling the dark gives —
it names HIS, which is allowed (he's human).

**Transitions:**
```
greet(topside) → [party descends] → chatter advances with deepest_site:
   lampworks → chatter.lampworks ; cisterns → chatter.cisterns ; stair → chatter.line
truth available from menu at any depth (blurts once each; lied then lamp)
at the Line, fork on skin:
   KEPT   → react.good (→ react.good.up when surfacing)
   BROKEN → react.bad  → react.bad.wait (sticky at this depth; he does not advance further)
state flag dob_waiting=true when react.bad.wait fires → the world may leave his lamp lit,
   unmanned, where he stood (a tier-A discoverable-on-return beat; INTEGRATION §3.3).
```

---

## 5. OLD PELL — won't descend; remembers your conduct   ·   `npc_key: old-pell`

The oldest topside. Went down once, came up alone, won't look at the Mouth since. The human
mirror of the Watcher — he keeps a grieving MEMORY-record, fully capitalised and contracted
and alive (the Watcher counts; Pell remembers, and it costs him). **Iss-adjacent:** Pell
remembers the keepers who went the liar's way and what became of them; when `iss_caught`
flips, he knows the group walked that road, and it lands on him hardest. His
`truth_or_lie` is the one HARD-TRUTH of the whole cast: *it does not chase; it waits, and
watches, and takes what stops being watched.*

```
TREE: old-pell
──────────────
greet ──▶ menu ──▶ { memory (his rumor = what he SAW) , truth (the hard one) }
                      │
                      └── react_good / react_bad at greet (he's been watching you come and go)
bye
```

| node_key | npcVoice key | when shown | text |
|---|---|---|---|
| `greet` (neutral, first) | `old-pell.greet.neutral` | first meet | "I won't go down, so don't ask. People always ask. They think I'm being dramatic. I went down once. That was the whole of my going-down. You'll understand or you won't." |
| `greet` (neutral, repeat) | `old-pell.greet.again` | seen before | "Sit if you like. Don't sit if you don't. I'm not lonely, I'm just old, the two get confused." |
| `greet` (warm) | `old-pell.greet.warm` | `react_good` | "You. You've been down more than once and you come up the same every time. Same eyes. You don't know what that's worth. I do. Come here. Good. You're still in there. Stay that way." |
| `greet` (cold, conduct) | `old-pell.greet.cold` | `react_bad` | "I've been watching you come and go. I watch everyone. And you — you've gone grey at the edges, the way they do, the way *they* did, and I'm not going to pretend I don't see it to spare your feelings. I'm too old to lie about the grey." |
| `greet` (cold, iss) | `old-pell.greet.iss_cold` | `iss_caught` sticky | "So you found the dead shrine. West and down, the cold hearth. I knew a man went looking for a road up at the bottom of a hole, and I knew what came back wearing him. You went where he went. I won't ask if you came back as you. I'm watching to see." |
| `memory` 1 | `old-pell.memory.kinds` | menu | "I knew people who went down keeping every little rule like it was nothing, like a game, and they came up and they were *here*, you understand, all the way here, behind their own eyes. And I knew the other kind. I don't say what happened to the other kind. You'll know it if you see it. You'll wish you didn't." |
| `memory` 2 | `old-pell.memory.seventh` | menu | "There were seven things you minded down there. I minded six of them. Six. I have spent a long time thinking about the seventh, and what it would've cost me to mind it, and I think now it would've cost me less than the not-minding has." |
| `truth` (the hard one) | `old-pell.truth.watched` | `truth_or_lie` | "I'll tell you the only true thing I have. It doesn't chase. Whatever's down there, it does not chase you. It waits, and it watches, and it takes what stops being watched. So be watched. Stay where your people can see you. That's all I've got and it's worth more than every map Aro's ever sold." |
| `react_good` (the cost) | `old-pell.react.good` | warm, on leaving | "I'll remember you came back right. That's not nothing, a person remembering you right. It's most of what I've got left to give." |
| `react_bad` (the cost) | `old-pell.react.bad` | cold, on leaving | "I remember the others who went the way you're going. I remember all of them. That's my curse, that I remember. And I'll remember you. Whatever you become down there, some part of you'll be up here, remembered, by a bitter old man who told you and you didn't listen." |
| `press` (if pushed) | `old-pell.press.refuse` | player presses what he saw | "No. You don't get that one. I carried it up alone so nobody else would have to carry it. Don't you dare make me hand it to you." |
| `bye` | `old-pell.bye` | exit | "Go on. I'll be here. Where else." |

**Travel destination seeded:** Pell deals in memory, not maps — his is the ANTI-rumor (he
corrects Aro). `truth.watched` names the survival principle the whole game enacts (stay
witnessed) and explicitly down-ranks Aro's maps ("worth more than every map Aro's ever
sold") — a soft pointer telling players which NPC to trust. `memory.seventh` seeds the
seventh-not-kept thread (L14 / `seventh-shrine` / R10). His `iss_cold` greet names the
`iss-dead-shrine` location ("west and down, the cold hearth") in plain human words — the
human echo of the corpus dead-end coordinate.

**The Watcher-mirror discipline.** Pell must NEVER drift into the Watcher's register. He
remembers in CAPITALS and contractions and grief; the Watcher counts in lowercase and
certainty. Same function (a record of conduct), opposite voice — that contrast is the point.
A Pell line that reads lowercase-sparse-certain is a bug.

**Transitions:**
```
greet → (skin; iss override applies) → menu
menu  → memory[kinds|seventh] → menu
menu  → truth (the hard truth; once) → menu
menu  → press (only offered if player asks what he saw; always refuses) → menu
bye   → close. on warm exit fire react.good; on cold exit fire react.bad.
on iss_caught at greet: skin=cold(iss), iss_cold sticky → greet.iss_cold.
```

---

## 6. ISS-ADJACENCY SUMMARY (the warm→cold flip on `iss_caught`)

Two of the five are Iss-adjacent and flip warm→cold when `arc_state.flags.iss_caught`
becomes true — because both are tied to the liar's road, and the corpus makes that road a
grave (`iss-dead-shrine`, L06/L08/L15):

| NPC | Iss-adjacent? | why | flip node | sticky? |
|---|---|---|---|---|
| **Aro** | YES | sold Iss's "cross the line" lie secondhand; the world proved it a grave | `aro.greet.iss_cold` | yes |
| **Old Pell** | YES | remembers the keepers who went the liar's way and what wore them back | `old-pell.greet.iss_cold` | yes |
| Wenna | no | her ways are folk-charm, not the liar's argument; only conduct-cold (recoverable) | — | — |
| Coll | no | pure transaction; only conduct-cold (a customer who stopped spending) | — | — |
| Dob | no (by flag) | reads KEPT/BROKEN skin at the Line, not `iss_caught`; his fork is conduct | (conduct fork) | dob_waiting |

`iss_caught` is set by the shipped `no-wall-catch` node (puzzles_seed.sql) — PLAYER-driven,
not showrunner-flipped (INTEGRATION risk note "DOC DRIFT (the Liar)"; these trees honor the
shipped player-driven model). The flip is **enacted, never announced** — no NPC says "I'm
cold now"; Aro deflects and won't meet their eyes, Pell names the dead shrine and watches.
Same person, colder. (The corpus separation law, applied to the human register.)

---

## 7. THE PRESIDING KEEPER — the rite-side NPC (`npc_key: keeper`)   ·   the KeeperNpcBeat tree

> **A different register from §1–§5.** The five surface NPCs are SET A (modern-rough,
> human). The Keeper is canon **register 3** (`canon-spine §0`; corpus SET C): the NPC who
> presides over the rite, older than the six prior keepers, nearer the thing the world has
> become. He speaks **second-person to the group** and half-says **we** of the kept dead.
> He is **lowercase like the Watcher** but he is **not** the Watcher — the Watcher is
> third-person and names names; the Keeper addresses *all of you* and names no one (INV-16).
> He may be a touch more human than the Watcher (the *you*, the half-veiled *we*) but he
> obeys the de-slop law absolutely and **NEVER states FACT 15**.
>
> Text source: every line is authored verbatim in `arc/corpus/npc-and-watcher-voice.md`
> **SET C** (and the new-thread Watcher keys in **SET B-NEW**). This section is the
> **tree + branch logic** that drives the **KeeperNpcBeat** — the plugin/showrunner reads
> a `node_key` + the dossier and posts the bound `keeper.*` text. No English in the beat.
>
> **He presides on the rite-side, not the surface** — he appears at `the_threshold` and the
> Undercroft altar, the places the synthesized threads converge, never at the Mouth where
> Set A lives. He is the human-presiding twin of the new Watcher keys: each Keeper node has a
> Watcher key it does not contradict (cross-surface truth law).

### 7.0 How the Keeper tree runs (branch on dossier state)

Same dossier the Watcher and Set A read — **no new measurement**. The Keeper's skin and
which thread-node he opens are computed from `arc_state.flags` + `punishment_state`:

```
node_key             dossier read (existing flags/state)                 voice key
─────────            ──────────────────────────────────                  ─────────
greet                conduct skin: warm | neutral | cold                 keeper.greet.<skin>
falseLaw             arc_state.flags.eighth_seen (forged ordinance read) keeper.falseLaw
seventhChoice/offer  seventh_named == true AND deep open (post-iss_caught) keeper.seventhChoice.offer
seventhChoice/done   arc_state.flags.seventh_choice ∈ {restore|erase}    keeper.seventhChoice.<restored|erased>
becomingKeepers      near the rite (M5 on-ramp) + conduct skin           keeper.becomingKeepers.<neutral|warm|cold>
collectiveRestraint  group_restraint_state.state ∈ {kept|broken}         keeper.collectiveRestraint.<kept|broken>
endings              arc_state.ending_fate (read AFTER the bow fires)    keeper.endings.<kept|castOut|divided|refusers>
deadEndTaunt         solved dead_end node + its kind (voice_args.kind)   keeper.deadEndTaunt(<name|count|place|known|prophet>)
```

Conduct skin resolution is identical to §0.2 (read off `punishment_state` toll_tier/
deciphered; `iss_caught` is **not** a Keeper-skin input — the Keeper is of the record, not
Iss-adjacent; he names the catch in `seventhChoice`/`becomingKeepers` by *flag*, not by a
warm→cold flip). The Keeper has **no `truth_or_lie` tell** — he is the one voice that never
lies and never reassures falsely; that honesty is his whole function against Iss (C7).

### 7.1 The Keeper tree (nodes → voice keys → branch)

```
TREE: keeper   (rite-side; cursor follows arc state, not a menu the player drives)
──────────
greet ──▶ { falseLaw (if eighth_seen) , seventhChoice (if seventh_named & deep open) ,
            collectiveRestraint (if restraint armed) , deadEndTaunt (on a dead-end solve) }
       ──▶ becomingKeepers (M5 on-ramp)
       ──▶ endings (after the bow)
```

| node_key | voice key | branch condition | thread / fact | INV |
|---|---|---|---|---|
| `greet` (neutral) | `keeper.greet.neutral` | early; nothing measured | the presider, half-veiled | — |
| `greet` (warm) | `keeper.greet.warm` | KEPT conduct | warmth-under-dread | §6.3 collective |
| `greet` (cold) | `keeper.greet.cold` | BROKEN conduct | grief not threat; reversible | §6.5, §6.10 |
| `falseLaw` | `keeper.falseLaw` | `eighth_seen` | the forged eighth (FACT 7b) | INV-17 |
| `seventhChoice` (offer) | `keeper.seventhChoice.offer` | `seventh_named` & deep open | the Seventh (FACT 10b) | INV-12 (colors, gates nothing) |
| `seventhChoice` (restored) | `keeper.seventhChoice.restored` | `seventh_choice = restore` | the INHERITORS codicil (FACT 14) | INV-16 |
| `seventhChoice` (erased) | `keeper.seventhChoice.erased` | `seventh_choice = erase` | the blank left | INV-16 |
| `becomingKeepers` (neutral) | `keeper.becomingKeepers.neutral` | M5 on-ramp, no skin | the rite (FACT 13/14); door to 15 | §6.2 never blurt |
| `becomingKeepers` (warm) | `keeper.becomingKeepers.warm` | M5 on-ramp, KEPT | the rite; the keeping made easy | §6.2 never blurt |
| `becomingKeepers` (cold) | `keeper.becomingKeepers.cold` | M5 on-ramp, BROKEN | the rite; grieves, never gates | §6.2 never blurt; reversible-tone |
| `endings` (kept) | `keeper.endings.kept` | `ending_fate = kept` | M5 close, human face | INV-11/16 |
| `endings` (castOut) | `keeper.endings.castOut` | `ending_fate = cast_out` | M5 close | INV-11/16 |
| `endings` (divided) | `keeper.endings.divided` | `ending_fate = divided` | M5 close; split by geometry | INV-16 |
| `endings` (refusers) | `keeper.endings.refusers` | `ending_fate = refusers` | M5 close; positive defiance | INV-11 |
| `collectiveRestraint` (kept) | `keeper.collectiveRestraint.kept` | `group_restraint_state = kept` | the Unlit Deep | INV-17 |
| `collectiveRestraint` (broken) | `keeper.collectiveRestraint.broken` | `group_restraint_state = broken` | the Unlit Deep; `broken_by` never spoken | INV-17 |
| `deadEndTaunt` (kind) | `keeper.deadEndTaunt(kind)` | on a dead-end solve; pass the row's `kind` | the honest counterweight to Iss | precision contract |

### 7.2 The cross-surface-truth map (Keeper node ↔ its non-contradicting Watcher key)

Each Keeper node has a Watcher key on the same thread; they never contradict (one voice
register on every surface). The Keeper says the human-presiding half; the Watcher records
the flat half. **Both feed the same flags** — the Keeper is read text, not a second source
of truth.

| Keeper node | paired Watcher key(s) (SET B-NEW) | shared flag / source |
|---|---|---|
| `keeper.falseLaw` | `cardEighthForged` (plant) → `archiveEighthCorrection` (catch) | `eighth_seen`; FACT 7b |
| `keeper.seventhChoice.*` | `keeperCloseSeventhRestored` / `keeperCloseSeventhErased`; `fateInheritorsCodicil` | `seventh_choice`; FACT 10b |
| `keeper.becomingKeepers.*` | (the rite-side `oracleThreeHands`; the Hold-Book `keeperEnrolled`) | the rite; FACT 13/14 |
| `keeper.endings.*` | `fateKept` / `fateCastOut` / `fateDivided` / `fateRefusers` (the composer base) | `ending_fate`; INV-11 |
| `keeper.collectiveRestraint.*` | `tollUnlitDeep` / `keptUnlitDeep`; `CUSTOM_PHRASES.the_unlit_deep` | `group_restraint_state`; INV-17 |
| `keeper.deadEndTaunt(kind)` | `oracleDeadEnd(kind)` — SAME `kind`, the flat label the Keeper humanizes | the dead-end solve + `voice_args.kind`; precision |
| (place-filing) `keeper.nameWhere` | `clueDrip` place-filing (BN10) | `player_visited_cells`; FACT 16 |

> **`keeper.nameWhere` (the name-where half-veiled M4 line)** — the BUILD-MANIFEST §4
> "name-where: the place-filing clueDrip + Keeper half-veiled line" pair. The Watcher drip
> (`clueDrip`, SET B-NEW BN10) surfaces the place-filing; the Keeper's half-veiled M4 line
> is authored in SET C as `keeper.nameWhere`:
>
> > *"your name is cut where you have not been. the record does not wait for your foot to file
> > you. it files the ground first and the foot after. before you was never strangers. it
> > was you, before you came."*
>
> Obeys INV-16 (chorus, never which-player) and INV-14 (the back-pointer is read, not typed).

### 7.3 The Keeper register discipline (the hard test)

A Keeper line that reads **third-person ledgerlike** is the Watcher's, not the Keeper's
(wrong set). A Keeper line with a **contraction, capital, exclamation, or named feeling**
is Set A's (wrong set). A Keeper line that **finishes the induction thought** ("and so you
become the watching") **states FACT 15** and is a defect — he stops at "we would keep you,
if you would keep the ways" and at "i will not say the rest of it." The half-veiled *we* is
the *only* place the recursion shows, and it only points.

---

## SCHEMA

```yaml
file: design/content/npc-dialogue.md
status: DRAFT (design content; integrated under build guards — NOT the live seed/migration)
purpose: >
  The five surface-NPC dialogue trees as data for npc_dialogue_state + npc_quests
  (migration 0005), driven via Citizens2/ZNPCsPlus. Human register (SET A), grounded
  line-for-line in arc/corpus/npc-and-watcher-voice.md. Each NPC: a conduct-skinned
  greet, a rumor that points at a real travel destination, a defining truth_or_lie tell,
  and react_good/react_bad read off the dossier. Two NPCs (Aro, Pell) flip warm→cold on
  arc_state.flags.iss_caught.
grounded_in:
  - arc/corpus/npc-and-watcher-voice.md   # SET A — the five surface NPCs (verbatim source)
  - arc/WORLD-BIBLE.md                     # the seven ways, the Deep Line, Iss, the places
  - discord/supabase/migrations/0005_threads.sql   # npc_dialogue_state / npc_quests schema
  - discord/supabase/seeds/puzzles_seed.sql        # iss_caught (no-wall-catch), site/lore web
  - discord/src/voice.ts                   # the CONTRAST register — NPCs are NOT this

register: modern_rough_human               # contractions, capitals, exclamation, named feelings ALLOWED
must_not: [lowercase_archaic, watcher_register, slop_phrases, "the ways said with reverence"]
separation_law: >
  No NPC line may be utterable by the Watcher (voice.ts). A screenshot of an NPC line and
  a Watcher line must read as two different authors. Old Pell is the sharpest test — same
  function as the Watcher (a record of conduct), opposite voice; a lowercase-sparse Pell
  line is a bug.

state_jsonb_shape:   # npc_dialogue_state.state, per (player_id, npc_key)
  cursor: text                 # current node_key
  seen_greet: bool             # full intro once; later visits short-greet
  rumors_heard: text[]         # spent rumor variants (so they cycle)
  told_truth_or_lie: bool      # the defining tell fires once
  conduct_skin: enum           # neutral | warm | cold (cached from last greet)
  iss_cold: bool               # sticky once an Iss-adjacent NPC flips on iss_caught

conduct_resolution:            # computed at greet; NO new measurement infra
  iss_override:                # only Aro + Pell
    when: "arc_state.flags.iss_caught == true AND npc.iss_adjacent"
    effect: "skin=cold; state.iss_cold=true (sticky)"
  conduct_from_punishment_state:
    broken: "any custom_key with toll_tier >= 2 AND deciphered == false  → cold"
    kept:   "no custom_key standing warned-and-unlearned                 → warm"
    else:   "neutral (early game; nothing observed yet)"
  recoverability:
    conduct_cold: recoverable   # atone/decipher → next greet may read warm
    iss_cold: sticky            # the liar's road does not un-happen

branch_keys: [greet, menu, rumor, truth_or_lie, react_good, react_bad, bye]
   # plus per-NPC: wenna/coll add a *_quest (npc_quests); dob uses depth-driven chatter
   #   (rumor slot) + a dob_waiting flag instead of a fetch-quest.

npcs:
  - npc_key: aro
    role: rumor-broker
    tell: lie                       # confident pleasant falsehood; world contradicts it
    descends: false
    iss_adjacent: true              # sold Iss's "cross the line" lie
    travel_destinations: [the Deep Line / the Stair (iss-dead-shrine lane), the Lamp-works (deep-bird coops)]
    quest: none
  - npc_key: wenna
    role: half-remembers ways as folk-charm (the accidental Rosetta / human teacher)
    tell: garbled_truth             # corrupted-but-real survival rite under the charm
    descends: false
    iss_adjacent: false
    travel_destinations: [the Deep Market dead-stall (kept lit for the dead)]
    quest: crust_quest              # teaches the_offering (teaching_site_id = the dead-stall)
    teaches: [the_unspoken, the_dark_hours, the_bow, the_kept_light, the_offering, "the seventh (count)"]
  - npc_key: coll
    role: trader
    tell: flat_accuracy             # no reason to lie about stock/distance
    descends_to: the Lamp-works     # never past it
    iss_adjacent: false
    travel_destinations: [the Lamp-works (second level), the third lamp-stand (Mara's L01/L02)]
    quest: oil_run_quest            # teaches the_kept_light (relight the third stand)
  - npc_key: dob
    role: descends with the group (only Set-A voice past the Mouth)
    tell: honest_to_a_fault         # blurts, admits fear
    descends: true
    cursor_driver: depth            # chatter advances with deepest site reached
    iss_adjacent: false             # forks on KEPT/BROKEN skin at the Line, not iss_caught
    decay: bravado -> chatter -> blurted_honesty -> "i'll be right here"   # quieting = the tell
    travel_destinations: [colours the spine descent Lamp-works→Cisterns→the Line; seeds none new]
    flag: dob_waiting               # he stops at the Line on BROKEN; world leaves his lamp lit, unmanned
  - npc_key: old-pell
    role: won't descend; remembers your conduct (human mirror of the Watcher)
    tell: hard_truth                # "it does not chase; it waits and watches and takes what stops being watched"
    descends: false
    iss_adjacent: true              # remembers the keepers who went the liar's way
    travel_destinations: [anti-rumor — corrects Aro; names west-and-down/the cold hearth on iss_cold]
    quest: none
    seeds: ["the seventh — not a custom_key; the seventh-not-kept lore (memory.seventh)", "stay-witnessed (truth.watched)"]

iss_flip:
  flag: arc_state.flags.iss_caught         # set by no-wall-catch (puzzles_seed.sql); PLAYER-driven
  flips: [aro, old-pell]                   # warm→cold, enacted never announced
  cold_nodes: [aro.greet.iss_cold, old-pell.greet.iss_cold]

voice_key_proposal:   # the HUMAN twin of voice.ts; integrated, never inlined at a call site
  registry: npcVoice
  home: "discord/src/npcVoice.ts (NEW, proposed) — parallel to voice.ts; keyed npc.node[.skin]"
  rule: >
    The Citizens/plugin layer posts npcVoice[node_key] VERBATIM, exactly as voice.ts is the
    SOLE source of the Watcher's text. No English in the dialogue handler; a node_key + the
    dossier in, the bound human line out. Keys are listed per row in §1–§5 above.
  note: >
    Kept SEPARATE from the voice object so the two registers cannot bleed (the separation
    law is enforceable: voice.ts stays lowercase-sparse; npcVoice.ts stays human-rough).
```

---

## INTEGRATION NOTES (for the build-guard pass — not done here)

- **New text home:** propose `discord/src/npcVoice.ts` (the human twin of `voice.ts`), a flat
  `Record<string, string>` keyed exactly as the tables above (`aro.greet.neutral`, …). The
  dialogue handler resolves `node_key` → text, mirroring how `resolveAnswer` → `voice.ts`.
  Do NOT add these to the `voice` object — the separation law depends on the two files not
  sharing a register.
- **No new measurement:** the conduct skin reads `punishment_state` (toll_tier/deciphered)
  and `arc_state.flags.iss_caught` — both already seeded by 0005 + the shipped puzzles seed.
  The trees add no signal listeners.
- **Quests:** `wenna.crust_quest` and `coll.oil_run_quest` are the only `npc_quests` rows in
  this pass; both double as discover-by-punishment human-teacher paths (set
  `punishment_state.deciphered=true` for `the_offering` / `the_kept_light` on completion via
  `teaching_site_id`). They GATE NOTHING (breadth invariant; side_quests.gates_progress=false).
- **Dob is depth-driven, not menu-driven:** his cursor is set by the deepest site the party
  has reached, not by player choice; `dob_waiting` is a world-honored flag (his abandoned lit
  lamp is a tier-A discoverable-on-return beat).
- **Seed shape (when integrated):** an additive, parse-clean `npc_dialogue_state` is per-player
  RUNTIME state (not authored rows); the AUTHORED content is the `npcVoice` keys + the
  `npc_quests` definitions. Any quest seed must be `begin/commit` + `on conflict (player_id,
  quest_key) do nothing`, clearly marked DRAFT — but quest ROWS are per-player and created at
  offer-time by the handler, so there is no static quest seed to write here, only the two
  quest_key definitions (`crust_quest`, `oil_run_quest`) and their completion hooks.
```
