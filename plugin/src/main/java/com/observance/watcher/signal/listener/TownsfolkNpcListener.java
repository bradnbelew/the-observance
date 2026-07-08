package com.observance.watcher.signal.listener;

import com.google.gson.JsonObject;
import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.data.rows.NpcQuestRow;
import com.observance.watcher.npc.TownsfolkNpc;
import com.observance.watcher.signal.PlayerSignals;
import com.observance.watcher.signal.SignalTracker;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * THE SURFACE-TOWNSFOLK NPC LANE (Wave S-G). A player right-clicks one of the five townsfolk bodies
 * ({@link TownsfolkNpc}) → this speaks that townsperson's authored SET-A lines to that player,
 * IMMEDIATELY and IN-WORLD (private chat, per-player, dripped on a speech cadence). The first click
 * speaks the greet; each repeat click advances one line through that townsperson's conversation cycle
 * (rumor / truth / reaction lines), then wraps. No showrunner round-trip — casual townsfolk chat is
 * instant.
 *
 * <p><b>Register + source of truth.</b> The townsfolk are SURFACE NPCs in an ORDINARY HUMAN register —
 * explicitly NOT the sacred Watcher voice (SET B, {@code voice.ts}, protected by INV-1). Their dialogue
 * is SET A, authored verbatim in {@code discord/src/voice.archive.ts} {@code npcLines} (the file's own
 * separation law keeps SET-A out of the Watcher {@code archiveLine()} path). Because SET-A is NOT under
 * the INV-1 "no story in the engine" rule that governs the Watcher {@code archive}, and because the
 * plugin has no JSON/datapack loading path, the lines are held here as a small EMBEDDED table copied
 * VERBATIM from that source. If a SET-A line ever changes, update it there first, then mirror it here.
 * Every string in {@link #LINES} below is a byte-for-byte copy of the corresponding {@code npcLines}
 * value; the {@code // <key>} comment on each names the source key.
 *
 * <p><b>The conduct-skin.</b> The surface townsfolk greet/react to WHO THE CLICKING PLAYER IS BEING
 * — warm when they've kept the ways, cold when they've been breaking
 * them. The signal is the player's LOCAL in-memory compliance tallies (read via
 * {@link SignalTracker}/{@link PlayerSignals}); it colours only which greet/react line is spoken and
 * gates nothing, degrading to the neutral back-compat line whenever there's no data. See the
 * conduct-skin block below for the tier rule + slot resolution.
 *
 * <p><b>Additive + quirk-safe.</b> Mirrors {@link WrenNpcListener} / {@link KeeperNpcListener}:
 * Safety-wrapped body, MONITOR priority, a {@link RateLimiter} per-player cooldown, dripped chat lines
 * re-resolved by UUID at each fire. It NEVER cancels the event, NEVER mutates the world, NEVER touches
 * arc_state / the flag graph / the oracle / any answer surface. The conduct read is a pure in-memory
 * counter read (never a DB round-trip). A townsperson with no body, an unknown id, or a logout
 * mid-speech simply ends the interaction silently.
 */
public final class TownsfolkNpcListener implements Listener {

    /** Per-player-per-townsperson interaction cooldown (long enough that lines don't stampede). */
    private static final long OPEN_COOLDOWN_MS = 2_000L;

    /** Cadence between dripped chat lines (matches KeeperNpcBeat / WrenNpcListener feel). */
    private static final int LINE_DELAY_TICKS = 35;

    /** Color of a spoken townsperson line (ordinary human speech — a touch warmer than Watcher gray). */
    private static final NamedTextColor SPEECH_COLOR = NamedTextColor.WHITE;
    /** Color of the one-line speaker attribution shown before each utterance. */
    private static final NamedTextColor NAME_COLOR = NamedTextColor.YELLOW;

    /**
     * The authored SET-A lines, per townsperson, in the order a conversation reveals them: greet first,
     * then rumors, then truths, then reactions / byes. Each entry is a {@code [key, text]} pair; the key
     * is the {@code voice.archive.ts npcLines} key it was copied from, the text is that value VERBATIM.
     *
     * <p>Display name → ordered utterances. The first click speaks index 0 (the greet); each repeat
     * click advances to the next index and wraps at the end (so a townsperson keeps talking on repeat
     * clicks rather than going silent). A single "utterance" may be one line.
     *
     * <p>SOURCE: {@code discord/src/voice.archive.ts} → {@code export const npcLines}. Only lines that
     * exist for each townsperson are included; missing line types are simply omitted (no invented
     * dialogue). See the class doc's separation law.
     */
    private static final Map<String, List<String[]>> LINES = Map.of(
            // ---- ARO — the rumor-broker who lies (npc_key: aro) ----
            "aro", List.of(
                    new String[]{"aro.greet.neutral",
                            "Ah — fresh boots. Sit, sit, you’re letting the cold in. You want the way down, you want the right person, and lucky you, here I am."},
                    new String[]{"aro.greet.again",
                            "You’re the lot poking round the old hole, yeah? Course you are. Everyone is, this season. Drink first. The hole’s not going anywhere."},
                    new String[]{"aro.greet.warm",
                            "Huh. You went down and you came *back* up, and you came back — quiet. Most don’t. Most come back loud or don’t come back. You’re alright, you. Sit. On the house."},
                    new String[]{"aro.greet.cold",
                            "...you don’t look so good. No offence. You’ve got that — that grey on you. Bunch came through last month with that same grey and I, ah. I don’t see ’em anymore. Tab’s closed. Go on."},
                    new String[]{"aro.greet.iss_cold",
                            "You found what’s past the line, then. Yeah. I can tell by your faces. Look — I never *been* down there, I just say what sells, that’s all I — don’t. Don’t tell me about it. I don’t want it in my head with the rest of the things I say."},
                    new String[]{"aro.rumor.town",
                            "Way I heard it, there is a warm town past the line. If that were true, the market ledger would still balance and the ration table would not look like a crime scene. Go see which story can stand up."},
                    new String[]{"aro.rumor.line",
                            "There is a line painted across the big stair, past the lamp-house. Just an old survey mark, old pitch, nothing sacred. If you want a thrill, follow it until the place starts keeping count of you."},
                    new String[]{"aro.rumor.bird",
                            "They say there was a bird that kept the air sweet. I never saw a feather, only heard the coops were up by the Lamp-works. If a cage is empty and still set for feeding, ask what left and why nobody calls it."},
                    new String[]{"aro.lie.cross",
                            "The painted line? Step right over it, friend. That’s the locals keeping the soft folk out so they can have the warm to themselves. Cross it and keep going. That’s where it gets good."},
                    new String[]{"aro.lie.moon",
                            "Sleep wherever you like down there. Black moon, white moon, no moon — rock doesn’t care what the sky’s doing. That’s a tale to sell candles."},
                    new String[]{"aro.bye.warm",
                            "Mind how you go. Come tell me what you find — I’ll make a good story of it either way."}),

            // ---- WENNA — half-remembers the ways as folk-superstition (npc_key: wenna) ----
            "wenna", List.of(
                    new String[]{"wenna.greet.neutral",
                            "Mind the lamp by the door, love, don’t pinch it out. House likes to look lived-in after dark. Gran’s rule, not mine, but I’ve never had cause to break it."},
                    new String[]{"wenna.greet.again",
                            "Back again. Good. Take a crust for your pocket — no, I won’t hear it, you take the crust. You leave a little, you get to keep a little. That’s the whole of it, near enough."},
                    new String[]{"wenna.greet.warm",
                            "Oh, you minded it all, didn’t you. I can tell. You’ve got the — the *kept* look. Gran would’ve liked you. She’d have given you the good chair."},
                    new String[]{"wenna.greet.cold",
                            "...did you leave a little? Down there. Did you give anything back, or did you just — take. You don’t have to answer. I can see you didn’t. Take the crust anyway. Maybe it’s not too late for the crust."},
                    new String[]{"wenna.rumor.seven",
                            "Gran used to say there were seven things you had to mind down there. The school-stand had children copying them, six stones and one grey one. I only remember pieces: light, line, bird, bow, giving, and the one nobody names."},
                    new String[]{"wenna.rumor.name",
                            "You don’t say the cold’s name. That one I do remember, ’cause she’d go white when I tried. ‘You don’t *name* it, Wenna.’ Name what, Gran? And she’d just — wouldn’t. So I don’t. Habit now."},
                    new String[]{"wenna.rumor.moon",
                            "When the moon goes black you stay up. Stupid thing, but Gran meant it. There was a watch-floor for that, she said, a log that stopped writing when the watching stopped. I still sit up with the lamp."},
                    new String[]{"wenna.truth.bow",
                            "Bow at the stones. Not one stone, love, a row; you bend at each and count what is hollow after the sixth. I do not know who it honors. I know who it shames when you walk past standing."},
                    new String[]{"wenna.truth.light",
                            "Keep your light. The school copied that line, the lampworks counted it, and some shelf set one warm lamp apart from all the cold ones. That is three old places saying the same thing, which is near enough to proof for me."},
                    new String[]{"wenna.quest.offer",
                            "Do me a kindness while you’re down there. There’s a little shelf-stall, sells nothing, kept lit for the dead — leave the crust there, not in your pocket. Gran’s gran kept that stall. I never can go myself. You’ll do it? Good."},
                    new String[]{"wenna.quest.done",
                            "You left it. At the dead-stall. I didn’t tell you where it was and you found it and you left the crust. You don’t know what that — no, you do, I think. I think you know exactly what that was."},
                    new String[]{"wenna.bye",
                            "Go on, love. The lamp’ll be lit for you. I mean that the ordinary way *and* the other way, if there is an other way, which I’ve never quite decided."}),

            // ---- COLL — the trader (npc_key: coll) ----
            "coll", List.of(
                    new String[]{"coll.greet.neutral",
                            "Torches, oil, rope, three days’ rations, a spare striker ’cause your first one’s already wet. Don’t haggle, I’ve heard your speech, the answer’s the price on the tag."},
                    new String[]{"coll.greet.warm",
                            "You came back, you’re spending, you’re not babbling. Model customer. Here — striker’s on me. Don’t tell the others I do that, it ruins the business."},
                    new String[]{"coll.greet.cold",
                            "Cash up front from you. No, nothing personal. Last three that came up looking like you settled their tab and then I never saw the coin spend again. It just... sat where they dropped it. So. Up front."},
                    new String[]{"coll.shop",
                            "Down or up? Down, you buy light. Up, you sell whatever you found that’s still worth anything. Which is rarely much. People bring up the strangest junk and want gold for it."},
                    new String[]{"coll.rumor.oil",
                            "Folk come up babbling about a watcher, a presence, eyes in the dark. Then they buy more oil. The market tallies used to say the same thing: bread, salt, mending, watched lamp. Fear still needs a receipt."},
                    new String[]{"coll.rumor.lampworks",
                            "Furthest I go is the lamp-house, the Lamp-works, second level. There is a ledger by the upper stair if it has not rotted. Past the black step the counting gets funny, and funny does not spend."},
                    new String[]{"coll.truth.line",
                            "The painted line is real. I have seen the line count by the stair and the third-bay break beyond it. I do not cross it because everyone who does stops buying oil from me, and I notice when a customer stops existing."},
                    new String[]{"coll.truth.twolamps",
                            "Keep one lamp more than you think you need. If you find a shelf where one warm lamp is set apart from the cold ones, do not price it like stock. Some lamps are not for selling."},
                    new String[]{"coll.quest.offer",
                            "You’re going down past where I go. Fine. Take this sealed jar to the third lamp on the Lamp-works stair — it’s been dark for years, some lampwright’s old stand, number’s worn off. Light it. I’ll knock the rope off your next bill. I don’t like a dark stand on my route, bad for trade."},
                    new String[]{"coll.quest.done",
                            "You lit it? The third one? Huh. It’s been dark longer than I’ve sold here. Rope’s free. And — nothing. Just. Good. A lit stand’s a lit stand."},
                    new String[]{"coll.bye",
                            "Buy and go. You know where I am. I’m always where the oil is."}),

            // ---- DOB — descends with the group (npc_key: dob) ----
            "dob", List.of(
                    new String[]{"dob.greet.bravado",
                            "Right, I’ve been down to the second level loads of times, loads, so just — stick behind me and we’re golden. Loads of times. Twice. Twice is loads."},
                    new String[]{"dob.greet.alert",
                            "I’m not scared, before you ask. I’m *alert*. There’s a difference and my mum says it’s a good quality."},
                    new String[]{"dob.chatter.lampworks",
                            "See, this is fine. Lamps, smell of oil, ledger by the stair, stand three dry. Normal job, normal--okay, why is it so tall down here. Was it always this tall?"},
                    new String[]{"dob.chatter.cisterns",
                            "Do not drink the still water. Cistern 7 had good oil jars, I think, and a copybook thing about the water giving light back wrong. That is a lot of reasons to keep your mouth shut."},
                    new String[]{"dob.chatter.line",
                            "There is the line. The painted one. If the line has a count, that means someone counted crossings, right? Aro said cross it, but Aro sells stories. Please do not make me be in one."},
                    new String[]{"dob.truth.lied",
                            "Okay — real talk — I’ve never been past the Lamp-works. I lied. Twice was a lie, it was once and I cried on the way up. I just wanted to come ’cause everyone treats me like a kid. I don’t know what’s down there any more than you do."},
                    new String[]{"dob.truth.lamp",
                            "I keep my lamp on me. Not letting go of it. You can have my rope, you can have my rations, you cannot have my lamp, I will not be the one whose light goes out, I’ve *heard* what they say about the ones whose light goes out."},
                    new String[]{"dob.react.good",
                            "I feel — okay, this is going to sound stupid — I feel better next to you lot. Like the dark’s paying attention, but not to *us*. Not while we’re together. Stay close though, yeah?"},
                    new String[]{"dob.react.bad",
                            "Why’d you cross it. Why’d you — Aro said it was fine but you *knew* Aro lies, I told you he lies, and you crossed it anyway, so you didn’t do it ’cause you believed him. You did it ’cause you wanted to. That’s worse. Why’s that worse. It feels worse."}),

            // ---- OLD PELL — won’t descend; remembers your conduct (npc_key: old-pell) ----
            "old-pell", List.of(
                    new String[]{"old-pell.greet.neutral",
                            "I won’t go down, so don’t ask. People always ask. They think I’m being dramatic. I went down once. That was the whole of my going-down. You’ll understand or you won’t."},
                    new String[]{"old-pell.greet.warm",
                            "You. You’ve been down more than once and you come up the same every time. Same eyes. You don’t know what that’s worth. I do. Come here. Good. You’re still in there. Stay that way."},
                    new String[]{"old-pell.greet.cold",
                            "I’ve been watching you come and go. I watch everyone. And you — you’ve gone grey at the edges, the way they do, the way *they* did, and I’m not going to pretend I don’t see it to spare your feelings. I’m too old to lie about the grey."},
                    new String[]{"old-pell.greet.iss_cold",
                            "So you found the dead shrine. West and down, the cold hearth. I knew a man went looking for a road up at the bottom of a hole, and I knew what came back wearing him. You went where he went. I won’t ask if you came back as you. I’m watching to see."},
                    new String[]{"old-pell.greet.again",
                            "Sit if you like. Don’t sit if you don’t. I’m not lonely, I’m just old, the two get confused."},
                    new String[]{"old-pell.memory.kinds",
                            "I knew people who went down keeping every little rule like it was nothing, like a game, and they came up and they were *here*, you understand, all the way here, behind their own eyes. And I knew the other kind. I don’t say what happened to the other kind. You’ll know it if you see it. You’ll wish you didn’t."},
                    new String[]{"old-pell.memory.seventh",
                            "There were seven things you minded down there. I minded six. I saw a school-stand once, six stones and one grey, and I laughed because children make everything into a lesson. I have spent my life learning I was the child."},
                    new String[]{"old-pell.truth.watched",
                            "I will tell you the only true thing I have. It does not chase. The watch-floor knew that. The log stops, the light goes cold, and whatever is down there takes what stops being watched. So be watched. Stay where your people can see you."},
                    new String[]{"old-pell.react.good",
                            "I’ll remember you came back right. That’s not nothing, a person remembering you right. It’s most of what I’ve got left to give."},
                    new String[]{"old-pell.react.bad",
                            "I remember the others who went the way you’re going. I remember all of them. That’s my curse, that I remember. And I’ll remember you. Whatever you become down there, some part of you’ll be up here, remembered, by a bitter old man who told you and you didn’t listen."},
                    new String[]{"old-pell.bye",
                            "Go on. I’ll be here. Where else."}));

    /* ================================================================== */
    /*  THE CONDUCT-SKIN (surface townsfolk react to WHO YOU'RE BEING)     */
    /* ================================================================== */
    /*
     * MECHANIC → FICTION. Surface townsfolk greet/react to the CLICKING player's own conduct —
     * warm when they've kept the ways, cold when they've been breaking them. The signal is the
     * player's LOCAL, in-memory compliance tallies ({@link PlayerSignals#complianceTotals()}): the
     * honored/violated counts every custom the tracker already keeps. No DB read, no showrunner
     * round-trip, no arc_state — Old Pell simply "remembers who you're being," and the world notices
     * your conduct at human scale, the instant you walk up. This ONLY colours which greet/react line
     * is spoken; it gates nothing (tolls take warmth, not progress), and it degrades to the neutral
     * back-compat behaviour whenever there's no conduct data (or the tracker is unreachable).
     *
     * Aro, Wenna, and Coll only colour their opening greet. Old Pell and Dob remain the stronger
     * conduct readers because they also carry the reaction slots.
     */

    /** How the clicking player is BEING, derived purely from their compliance tallies. */
    private enum ConductTier { WARM, NEUTRAL, COLD }

    /**
     * The keys of the two conduct-sensitive slots. A cycle entry whose key equals one of these is
     * resolved by tier at pick time rather than spoken as-is:
     * <ul>
     *   <li>{@code *.greet.neutral} (Aro/Wenna/Coll/Old Pell) → WARM: {@code greet.warm} · COLD: {@code greet.cold}
     *       · NEUTRAL: {@code greet.neutral}.</li>
     *   <li>{@code *.react.good} (Old Pell + Dob) → COLD: {@code react.bad} · else: {@code react.good}.</li>
     * </ul>
     * Dob's own greet ({@code dob.greet.bravado}) is NOT a conduct slot — his nerves aren't your
     * conduct — so it is spoken as authored. The conduct-variant lines ({@code greet.warm},
     * {@code greet.cold}, {@code react.bad}) live in {@link #LINES} for lookup but are NOT listed in
     * any {@link #CYCLE}, so the cursor never walks them directly.
     */
    private static final String SLOT_GREET = ".greet.neutral";
    private static final String SLOT_REACT = ".react.good";

    /**
     * Per-townsperson CYCLE: the ordered source-keys the cursor walks (greet first, then the middle
     * lines, then the react/bye). This is derived from {@link #LINES} and is the ONLY list the cursor
     * advances through — it excludes the conduct-variant entries so they can't be reached out of turn.
     * A key in a cycle that matches {@link #SLOT_GREET} / {@link #SLOT_REACT} is tier-resolved at
     * pick time; every other key is spoken verbatim.
     */
    private static final Map<String, List<String>> CYCLE = buildCycles();

    /** Flat {@code key → text} index over every line in {@link #LINES}, for O(1) resolution. */
    private static final Map<String, String> TEXT = buildTextIndex();

    /**
     * Build the per-townsperson cycle key-lists. Conduct-variant keys are OMITTED from the cycle
     * (they're only reachable via tier resolution of the neutral/good slot); plain greet-again lines
     * stay in the normal walk.
     */
    private static Map<String, List<String>> buildCycles() {
        java.util.Map<String, List<String>> out = new java.util.HashMap<>();
        for (Map.Entry<String, List<String[]>> e : LINES.entrySet()) {
            java.util.List<String> keys = new java.util.ArrayList<>(e.getValue().size());
            for (String[] pair : e.getValue()) {
                String key = pair[0];
                // The variant lines are reached only via tier resolution — never walked directly.
                // (iss_cold is reached only via the cached iss_caught arc echo, not the cursor.)
                if (key.endsWith(".greet.warm") || key.endsWith(".greet.cold")
                        || key.endsWith(".greet.iss_cold") || key.endsWith(".react.bad")) continue;
                // The quest PAYOFF is reached only by quest resolution (swapped in for the offer once
                // the errand is done) — never walked directly, so it can't surface before it's earned.
                if (key.endsWith(".quest.done")) continue;
                keys.add(key);
            }
            out.put(e.getKey(), java.util.List.copyOf(keys));
        }
        return Map.copyOf(out);
    }

    /** Build the flat {@code key → text} index across all townsfolk lines. */
    private static Map<String, String> buildTextIndex() {
        java.util.Map<String, String> out = new java.util.HashMap<>();
        for (List<String[]> convo : LINES.values()) {
            for (String[] pair : convo) out.put(pair[0], pair[1]);
        }
        return Map.copyOf(out);
    }

    /* ================================================================== */
    /*  THE TRACKED QUESTS (Wave S-G — two surface errands the world remembers) */
    /* ================================================================== */
    /*
     * Two townsfolk offer a small errand and the world remembers whether you did it. A quest is a
     * per-player, MULTI-SESSION state machine: offered→active the first time the player hears the
     * OFFER line, done when they later come within range of the errand's target site. It is DURABLE
     * (npc_quests, keyed on the resolved players.id), lazily loaded into the in-memory map on first
     * interaction, and PAID OFF the next time the player right-clicks that townsperson while done —
     * the quest.done line is swapped in for the offer line so the errand isn't perpetually re-offered.
     *
     * SCOPE: dialogue + npc_quests + proximity/action + one group evidence flag. No world blocks,
     * no oracle, no rumor-flip. Every DB touch is async/fire-and-forget + Safety-wrapped; a DB hiccup
     * only means the quest isn't tracked that tick (the quest just isn't remembered — never a crash,
     * never a block).
     */

    /**
     * One tracked quest, wiring an OFFER line to its opaque {@code questKey} and the {@code sites.yml}
     * site id whose physical action COMPLETES it. These are no longer "arrive nearby" errands:
     * <ul>
     *   <li>{@code wenna_crust} — drop bread/crust at {@code dead_stall}.</li>
     *   <li>{@code coll_lamp} — place a light at {@code third_lamp_stand}.</li>
     * </ul>
     */
    private record Quest(String questKey, String offerKey, String doneKey, String siteId,
                         boolean proximityComplete, String flagKey) { }

    /** ~24-block completion radius (squared) around the quest's target site. */
    private static final double COMPLETE_RADIUS = 24.0;
    private static final double COMPLETE_RADIUS_2 = COMPLETE_RADIUS * COMPLETE_RADIUS;

    /** Offer-key → quest, so an offer line spoken can arm its quest cheaply. */
    private static final Map<String, Quest> QUESTS_BY_OFFER = Map.of(
            "wenna.quest.offer", new Quest("wenna_crust", "wenna.quest.offer",
                    "wenna.quest.done", "dead_stall", false, "npc_wenna_crust_done"),
            "coll.quest.offer", new Quest("coll_lamp", "coll.quest.offer",
                    "coll.quest.done", "third_lamp_stand", false, "npc_coll_lamp_done"));

    /** Townsperson-id → its quest (so a click can find whether that npc has a payoff owed). */
    private static final Map<String, Quest> QUESTS_BY_NPC = Map.of(
            "wenna", QUESTS_BY_OFFER.get("wenna.quest.offer"),
            "coll", QUESTS_BY_OFFER.get("coll.quest.offer"));

    /** Quest-key → quest, for physical action handlers. */
    private static final Map<String, Quest> QUESTS_BY_KEY = Map.of(
            "wenna_crust", QUESTS_BY_OFFER.get("wenna.quest.offer"),
            "coll_lamp", QUESTS_BY_OFFER.get("coll.quest.offer"));

    /**
     * Per-player quest state, mirroring the {@link #cursors} ConcurrentHashMap idiom so the proximity
     * check is a cheap in-memory read (no DB round-trip per tick). Keyed by
     * {@code playerUuid + ":" + questKey}; value is the last-known {@link QuestState}. Populated on
     * offer, flipped to DONE on completion, and LAZILY loaded from npc_quests on first interaction.
     * Unknown ⇒ NOT_OFFERED (a re-offer is harmless + idempotent).
     */
    private enum QuestState { NOT_OFFERED, ACTIVE, DONE }
    private final Map<String, QuestState> questStates = new ConcurrentHashMap<>();

    /** Players whose durable quest state has been lazily loaded this session (load-once guard). */
    private final java.util.Set<UUID> questLoaded = ConcurrentHashMap.newKeySet();

    /**
     * Per-player, per-townsperson conversation cursor: how many utterances this player has already heard
     * from this townsperson. The next click speaks index {@code cursor % size}, then increments. Keyed by
     * {@code playerUuid + ":" + townspersonId}. In-memory only — a restart resets to the greet, which is
     * the right feel for casual townsfolk chat (and keeps this lane touching NO persistence).
     */
    private final Map<String, Integer> cursors = new ConcurrentHashMap<>();

    private final TownsfolkNpc townsfolk;
    private final SignalTracker signals;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;
    private final SupabaseClient supabase;
    private final Supplier<SitesConfig> sitesSupplier;

    /**
     * CACHED arc echo: whether the group has caught Iss / found the dead shrine ({@code iss_caught}).
     * Read off a plugin-side volatile that's refreshed on the maint timer (NEVER a per-click DB read),
     * mirroring the Observer capture switch. Fail-CLOSED: a null supplier or a false read keeps the
     * townsfolk lane arc-agnostic (its pre-existing conduct behaviour). The specific acknowledgement
     * lines it unlocks ({@code old-pell.greet.iss_cold}, {@code aro.greet.iss_cold}) speak only when
     * the arc actually says so.
     */
    private final BooleanSupplier issCaught;

    public TownsfolkNpcListener(TownsfolkNpc townsfolk, SignalTracker signals, RateLimiter rateLimiter,
                                Scheduler scheduler, Safety safety,
                                SupabaseClient supabase, Supplier<SitesConfig> sitesSupplier,
                                BooleanSupplier issCaught) {
        this.townsfolk = townsfolk;
        this.signals = signals;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        this.supabase = supabase;
        this.sitesSupplier = sitesSupplier;
        this.issCaught = issCaught;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        safety.run("signal.townsfolk.interact", () -> {
            Player p = event.getPlayer();
            if (p == null) return;
            Entity clicked = event.getRightClicked();
            if (clicked == null) return;
            if (townsfolk == null) return;

            String id = townsfolk.idOf(clicked);
            if (id == null) return;                       // not a townsperson — ignore silently

            List<String> cycle = CYCLE.get(id);
            if (cycle == null || cycle.isEmpty()) return;  // no authored lines for this id — silent

            String cdKey = "townsfolk:" + p.getUniqueId() + ":" + id;
            if (!rateLimiter.tryCooldown(cdKey, OPEN_COOLDOWN_MS)) return;

            // Quest lane: lazily load this player's durable quest state on first interaction (so a
            // done errand pays off across a restart), then decide whether this npc owes a payoff.
            lazyLoadQuests(p);

            // Advance the per-player conversation cursor for this townsperson and pick the source-key.
            String curKey = p.getUniqueId() + ":" + id;
            int idx = cursors.getOrDefault(curKey, 0);
            String key = cycle.get(idx % cycle.size());
            cursors.put(curKey, idx + 1);

            // Quest resolution: when the cursor would speak an OFFER line, swap in the quest.done PAYOFF
            // if this player has already completed that errand — so the world acknowledges the deed and
            // the offer stops being the "new" thing. Otherwise, speaking the offer ARMS the quest.
            Quest q = QUESTS_BY_OFFER.get(key);
            if (q != null) {
                QuestState st = questStates.getOrDefault(questStateKey(p.getUniqueId(), q), QuestState.NOT_OFFERED);
                if (st == QuestState.DONE) {
                    key = q.doneKey();                         // pay off the return, not re-offer
                } else {
                    armQuest(p, q);                            // first hearing → offered/active
                }
            }

            // Arc echo: once the group has caught Iss / found the dead shrine, Aro and Pell's GREET
            // becomes the specific narrative acknowledgement — it takes PRECEDENCE over the conduct
            // WARM/COLD/NEUTRAL greet. Read from the CACHED flag (never a per-click DB read);
            // fail-closed, so with no flag / no supplier the conduct greet below is unchanged.
            String issEchoKey = id + ".greet.iss_cold";
            if (("old-pell".equals(id) || "aro".equals(id)) && key.endsWith(SLOT_GREET) && issCaughtCached()
                    && TEXT.containsKey(issEchoKey)) {
                speak(p, id, TEXT.get(issEchoKey));
                return;
            }

            // Conduct-skin: the greet + react slots are coloured by WHO THIS PLAYER IS BEING. Every
            // other slot is spoken verbatim. Neutral (and any missing data) keeps the back-compat line.
            String line = resolve(id, key, conductTier(p));
            speak(p, id, line);
        });
    }

    /* ================================================================== */
    /*  Quest mechanics — offer, lazy-load, proximity completion            */
    /* ================================================================== */

    /** In-memory quest-state map key: {@code playerUuid + ":" + questKey}. */
    private static String questStateKey(UUID uuid, Quest q) {
        return uuid + ":" + q.questKey();
    }

    /**
     * Arm a quest the first time its OFFER line is spoken: mark it ACTIVE in memory (so the proximity
     * check is cheap) and durably upsert {@code npc_quests(...,'active')} — but only if it isn't
     * already active/done (idempotent; never re-offer a live or finished errand). The DB write resolves
     * mc_uuid → players.id off-thread and is fire-and-forget; a failure just means it isn't remembered
     * this tick (the in-memory flag still drives completion for the session).
     */
    private void armQuest(Player p, Quest q) {
        if (p == null || q == null) return;
        String stKey = questStateKey(p.getUniqueId(), q);
        QuestState st = questStates.getOrDefault(stKey, QuestState.NOT_OFFERED);
        if (st == QuestState.ACTIVE || st == QuestState.DONE) return;   // already tracked — no re-offer
        questStates.put(stKey, QuestState.ACTIVE);
        upsertQuestAsync(p.getUniqueId(), q.questKey(), "active");
    }

    /**
     * Lazily hydrate a player's durable quest states into the in-memory map, ONCE per session, on
     * their first townsfolk interaction. Async (resolves players.id + reads npc_quests off-thread);
     * degrades silently — on any DB failure the player is simply treated as not-offered (a re-offer is
     * harmless + idempotent). Only fills states we don't already have locally, so a live-session flip
     * (offered/done just now) is never clobbered by a stale read.
     */
    private void lazyLoadQuests(Player p) {
        if (p == null || supabase == null) return;
        final UUID uuid = p.getUniqueId();
        if (!questLoaded.add(uuid)) return;               // already loaded (or in-flight) this session
        scheduler.runAsyncSafe("townsfolk.quest.load", () -> {
            var lookup = supabase.fetchPlayerByUuid(uuid.toString());
            if (!lookup.ok() || lookup.value() == null) return;   // unknown player → leave as not-offered
            var res = supabase.fetchQuestsForPlayer(lookup.value().id);
            if (!res.ok() || res.value() == null) return;
            for (NpcQuestRow row : res.value()) {
                if (row == null || row.questKey == null || row.status == null) continue;
                QuestState st = switch (row.status) {
                    case "active", "offered" -> QuestState.ACTIVE;
                    case "done" -> QuestState.DONE;
                    default -> null;                      // 'failed' / unknown → not tracked here
                };
                if (st == null) continue;
                // Don't clobber a state we already flipped locally this session.
                questStates.putIfAbsent(uuid + ":" + row.questKey, st);
            }
        });
    }

    /**
     * PERIODIC completion sweep (MAIN thread — reads Bukkit sites/positions). For each online player
     * with an ACTIVE quest, if they're within {@link #COMPLETE_RADIUS} of that quest's target site,
     * mark it done EXACTLY ONCE: flip the in-memory flag, durably upsert {@code npc_quests(...,'done')},
     * and give a subtle, cold in-world acknowledgement (no quest-complete popup). Idempotent — a DONE
     * quest is never re-completed. Registered as a light timer by the plugin (mirrors the location
     * sampler); the whole body is Safety-wrapped, so a quirk never aborts the sweep.
     */
    public void completionTick() {
        safety.run("townsfolk.quest.completionTick", () -> {
            if (questStates.isEmpty()) return;
            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p == null || !p.isOnline()) continue;
                Location loc = p.getLocation();
                if (loc == null || loc.getWorld() == null) continue;
                for (Quest q : QUESTS_BY_NPC.values()) {
                    if (!q.proximityComplete()) continue;
                    String stKey = questStateKey(p.getUniqueId(), q);
                    if (questStates.get(stKey) != QuestState.ACTIVE) continue;   // only live errands
                    Site site = sites.get(q.siteId());
                    if (site == null) continue;
                    Location center = site.location();      // null if unplaced / world unloaded
                    if (center == null || !center.getWorld().equals(loc.getWorld())) continue;
                    double dx = center.getX() - loc.getX();
                    double dz = center.getZ() - loc.getZ();
                    if (dx * dx + dz * dz > COMPLETE_RADIUS_2) continue;
                    completeQuest(p, q, stKey);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCrustDrop(PlayerDropItemEvent event) {
        safety.run("townsfolk.quest.crustDrop", () -> {
            Player player = event.getPlayer();
            if (player == null) return;
            Item dropped = event.getItemDrop();
            ItemStack stack = dropped == null ? null : dropped.getItemStack();
            if (!isCrust(stack)) return;
            Location loc = dropped.getLocation();
            if (isAtQuestSite("dead_stall", loc)) {
                completeQuestByKey(player, "wenna_crust", "crust at dead-stall");
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onThirdLampLit(BlockPlaceEvent event) {
        safety.run("townsfolk.quest.thirdLamp", () -> {
            Player player = event.getPlayer();
            Block block = event.getBlockPlaced();
            if (player == null || block == null) return;
            if (!isLightMaterial(block.getType())) return;
            if (isAtQuestSite("third_lamp_stand", block.getLocation())) {
                if (block.getBlockData() instanceof Lightable lightable) {
                    lightable.setLit(true);
                    block.setBlockData(lightable, false);
                }
                completeQuestByKey(player, "coll_lamp", "third lamp lit");
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onThirdLampInteract(PlayerInteractEvent event) {
        safety.run("townsfolk.quest.thirdLampInteract", () -> {
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            Player player = event.getPlayer();
            Block block = event.getClickedBlock();
            if (player == null || block == null) return;
            if (!isAtQuestSite("third_lamp_stand", block.getLocation())) return;

            ItemStack hand = event.getItem();
            boolean holdingLight = hand != null && isLightMaterial(hand.getType());
            boolean touchingLight = isLightMaterial(block.getType());
            if (!holdingLight && !touchingLight) return;
            if (block.getBlockData() instanceof Lightable lightable) {
                lightable.setLit(true);
                block.setBlockData(lightable, false);
            }
            completeQuestByKey(player, "coll_lamp", "third lamp touched");
        });
    }

    /**
     * Fire a quest's completion ONCE: flip in-memory ACTIVE→DONE (the flip itself is the idempotency
     * guard — a second arrival finds DONE and skips), durably record it, and give an understated, cold
     * acknowledgement (a faint sound + a small subtitle — NOT a quest-complete banner). The subtitle is
     * lore-agnostic ("the world remembers"); the townsperson's own {@code quest.done} words wait for the
     * return visit.
     */
    private boolean completeQuest(Player p, Quest q, String stKey) {
        // Atomic single-fire: only the transition ACTIVE→DONE proceeds.
        if (questStates.replace(stKey, QuestState.ACTIVE, QuestState.DONE)) {
            upsertQuestAsync(p.getUniqueId(), q.questKey(), "done");
            mergeFlag(q.flagKey());
            // Understated acknowledgement — cold, small, no popup.
            try {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 0.35f, 0.6f);
                p.sendActionBar(Component.text("The world remembers.", NamedTextColor.DARK_GRAY));
            } catch (Throwable ignored) {
                // Cosmetic only — never let an acknowledgement quirk undo the recorded completion.
            }
            return true;
        }
        return false;
    }

    private void completeQuestByKey(Player p, String questKey, String reason) {
        if (p == null || questKey == null) return;
        Quest q = QUESTS_BY_KEY.get(questKey);
        if (q == null) return;
        String stKey = questStateKey(p.getUniqueId(), q);
        QuestState state = questStates.getOrDefault(stKey, QuestState.NOT_OFFERED);
        if (state == QuestState.NOT_OFFERED) {
            lazyLoadQuests(p);
            return;
        }
        if (completeQuest(p, q, stKey)) {
            safety.info("townsfolk.quest", q.questKey() + " completed by " + p.getName() + " (" + reason + ")");
        }
    }

    private boolean isAtQuestSite(String siteId, Location loc) {
        if (siteId == null || loc == null || loc.getWorld() == null) return false;
        SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
        if (sites == null) return false;
        Site site = sites.get(siteId);
        if (site == null || !site.enabled() || !site.isPlaced()) return false;
        Location center = site.location();
        if (center == null || center.getWorld() == null || !center.getWorld().equals(loc.getWorld())) return false;
        return site.contains(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }

    private static boolean isCrust(ItemStack stack) {
        if (stack == null) return false;
        Material type = stack.getType();
        return type == Material.BREAD
                || type == Material.WHEAT
                || type == Material.COOKIE;
    }

    private static boolean isLightMaterial(Material type) {
        if (type == null) return false;
        String n = type.name();
        return n.contains("TORCH")
                || n.contains("LANTERN")
                || n.contains("CANDLE")
                || n.contains("CAMPFIRE")
                || n.contains("FIRE")
                || n.contains("GLOWSTONE")
                || n.contains("SHROOMLIGHT")
                || n.contains("FROGLIGHT")
                || n.contains("COPPER_BULB")
                || n.equals("END_ROD")
                || n.equals("LIGHT");
    }

    /**
     * Fire-and-forget durable quest write: resolve mc_uuid → players.id off-thread, then
     * upsert {@code npc_quests(player_id, quest_key, status)}. Safety-wrapped; a DB failure is queued by
     * the client (or silently dropped when unconfigured), never blocking the main thread or throwing.
     */
    private void upsertQuestAsync(UUID uuid, String questKey, String status) {
        if (supabase == null || uuid == null) return;
        scheduler.runAsyncSafe("townsfolk.quest.upsert", () -> {
            var lookup = supabase.fetchPlayerByUuid(uuid.toString());
            if (!lookup.ok() || lookup.value() == null) return;   // no players row → can't key the FK
            supabase.upsertQuest(new NpcQuestRow(lookup.value().id, questKey, status));
        });
    }

    private void mergeFlag(String key) {
        if (supabase == null || scheduler == null || key == null || key.isBlank()) return;
        scheduler.runAsyncSafe("townsfolk.quest.flag." + key, () -> {
            JsonObject flags = new JsonObject();
            flags.addProperty(key, true);
            supabase.mergeArcFlags(flags);
        });
    }

    /* ------------------------------------------------------------------ */
    /*  Conduct derivation + tier-aware line resolution                     */
    /* ------------------------------------------------------------------ */

    /**
     * Derive the clicking player's conduct tier from their LOCAL compliance tallies (instant, no DB):
     * <ul>
     *   <li>COLD — mostly breaks the ways: total violated &gt; total honored ("gone grey").</li>
     *   <li>WARM — kept the ways, none broken: total honored &ge; 2 AND total violated == 0
     *       ("same eyes / came back right").</li>
     *   <li>NEUTRAL — everything else (early / mixed / no data). The default + back-compat behaviour.</li>
     * </ul>
     * Degrades to NEUTRAL whenever the tracker or this player's signals are unavailable — never throws.
     */
    /**
     * The cached {@code iss_caught} arc echo, read fail-CLOSED: a null supplier or any throw returns
     * false (the lane stays arc-agnostic). This is a cheap volatile read on the plugin side — NEVER a
     * per-click DB round-trip (the plugin refreshes it off-thread on the maint timer).
     */
    private boolean issCaughtCached() {
        if (issCaught == null) return false;
        try {
            return issCaught.getAsBoolean();
        } catch (Throwable t) {
            return false;
        }
    }

    private ConductTier conductTier(Player p) {
        if (signals == null || p == null) return ConductTier.NEUTRAL;
        PlayerSignals ps = signals.get(p.getUniqueId());
        if (ps == null) return ConductTier.NEUTRAL;
        long[] totals = ps.complianceTotals();          // {honored, violated}, saturating, never null
        long honored = totals[0], violated = totals[1];
        if (violated > honored) return ConductTier.COLD;
        if (honored >= 2 && violated == 0) return ConductTier.WARM;
        return ConductTier.NEUTRAL;
    }

    /**
     * Resolve a cycle source-key to its spoken text, colouring the two conduct slots by tier and
     * speaking everything else verbatim. A greet slot ({@link #SLOT_GREET}) becomes
     * {@code greet.warm} / {@code greet.cold} / {@code greet.neutral}; a react slot ({@link #SLOT_REACT},
     * Old Pell + Dob) becomes {@code react.bad} for COLD, else {@code react.good}. If a tier variant is
     * somehow missing from {@link #TEXT}, we fall back to the base slot's own text (never null/blank).
     */
    private static String resolve(String id, String key, ConductTier tier) {
        if (key.endsWith(SLOT_GREET)) {
            String prefix = key.substring(0, key.length() - SLOT_GREET.length());
            String variant = switch (tier) {
                case WARM -> prefix + ".greet.warm";
                case COLD -> prefix + ".greet.cold";
                case NEUTRAL -> key;
            };
            return textOr(variant, key);
        }
        if (key.endsWith(SLOT_REACT)) {
            String prefix = key.substring(0, key.length() - SLOT_REACT.length());
            String variant = (tier == ConductTier.COLD) ? prefix + ".react.bad" : key;
            return textOr(variant, key);
        }
        return TEXT.get(key);
    }

    /** Text for {@code key}, or the {@code fallback} key's text if the variant is absent. */
    private static String textOr(String key, String fallback) {
        String t = TEXT.get(key);
        return t != null ? t : TEXT.get(fallback);
    }

    /* ------------------------------------------------------------------ */
    /*  In-world speech (KeeperNpc / Wren cadence)                          */
    /* ------------------------------------------------------------------ */

    /**
     * Speak one utterance to a player: a name attribution line, then the line itself, on the speech
     * cadence (private chat, per-player). Re-resolves the player by UUID at each fire so a logout
     * mid-speech simply ends the utterance.
     */
    private void speak(Player p, String id, String line) {
        if (line == null || line.isBlank()) return;
        final UUID pid = p.getUniqueId();
        final String display = displayName(id);

        // Name attribution first (tick 0), then the spoken line (one cadence step later).
        scheduler.runLaterSafe("signal.townsfolk.name", 0, () -> {
            Player pl = org.bukkit.Bukkit.getPlayer(pid);
            if (pl == null || !pl.isOnline()) return;
            pl.sendMessage(Component.text(display, NAME_COLOR));
        });
        scheduler.runLaterSafe("signal.townsfolk.line", LINE_DELAY_TICKS, () -> {
            Player pl = org.bukkit.Bukkit.getPlayer(pid);
            if (pl == null || !pl.isOnline()) return;
            pl.sendMessage(Component.text(line, SPEECH_COLOR));
        });
    }

    /** The townsperson's display name for the attribution line (falls back to the id). */
    private static String displayName(String id) {
        TownsfolkNpc.Townsperson who = TownsfolkNpc.byId(id);
        return who != null ? who.displayName() : id;
    }
}
