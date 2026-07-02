package com.observance.watcher.signal.listener;

import com.observance.watcher.npc.TownsfolkNpc;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
 * <p><b>Additive + quirk-safe.</b> Mirrors {@link WrenNpcListener} / {@link KeeperNpcListener}:
 * Safety-wrapped body, MONITOR priority, a {@link RateLimiter} per-player cooldown, dripped chat lines
 * re-resolved by UUID at each fire. It NEVER cancels the event, NEVER mutates the world, NEVER touches
 * arc_state / the flag graph / the oracle / any answer surface. A townsperson with no body, an unknown
 * id, or a logout mid-speech simply ends the interaction silently.
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
                    new String[]{"aro.rumor.town",
                            "Way I heard it, there’s a whole town down there. Lamps still burning. People who just — stayed. Living fat off the warm while we freeze our backsides up here. That’s why nobody comes back up, see. Not ’cause they died. ’Cause it’s *nice*."},
                    new String[]{"aro.rumor.line",
                            "There’s a line painted across the big stair, halfway down. Don’t mean nothing. Old paint. Builders’ mark. People make a whole religion out of a stripe of pitch, I swear. You want to see it, it’s down past the lamp-house, on the Stair."},
                    new String[]{"aro.rumor.bird",
                            "They say there’s a bird down there older than the digging. Keeps the air sweet. You find the bird, you find the bottom, and the bottom’s where they kept the good stuff. Coops were up at the Lamp-works, last anyone said."},
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
                    new String[]{"wenna.rumor.seven",
                            "Gran used to say there were seven somethings you had to mind down there. Seven. I only ever remember six and I always forget a different one, isn’t that the way. Light, and the line, and the bird, and the bowing, and the giving, and... see, there’s the sixth gone again."},
                    new String[]{"wenna.rumor.name",
                            "You don’t say the cold’s name. That one I do remember, ’cause she’d go white when I tried. ‘You don’t *name* it, Wenna.’ Name what, Gran? And she’d just — wouldn’t. So I don’t. Habit now."},
                    new String[]{"wenna.rumor.moon",
                            "When the moon goes black you stay up. Stupid, isn’t it. I still do it. Sit up all night with the lamp like a fool. Slept through it once as a girl and had the worst dreams of my life, so."},
                    new String[]{"wenna.truth.bow",
                            "Bow at the stones. I don’t know who to, mind. Gran never said who. You just bend your knee going past and you don’t think too hard about it. The ones who don’t bend... she’d just shake her head."},
                    new String[]{"wenna.truth.light",
                            "Keep your light. Above all the others, keep your light. That one she said like it mattered more than the rest put together, and she didn’t say things like that twice."},
                    new String[]{"wenna.quest.offer",
                            "Do me a kindness while you’re down there. There’s a little shelf-stall, sells nothing, kept lit for the dead — leave the crust there, not in your pocket. Gran’s gran kept that stall. I never can go myself. You’ll do it? Good."},
                    new String[]{"wenna.bye",
                            "Go on, love. The lamp’ll be lit for you. I mean that the ordinary way *and* the other way, if there is an other way, which I’ve never quite decided."}),

            // ---- COLL — the trader (npc_key: coll) ----
            "coll", List.of(
                    new String[]{"coll.greet.neutral",
                            "Torches, oil, rope, three days’ rations, a spare striker ’cause your first one’s already wet. Don’t haggle, I’ve heard your speech, the answer’s the price on the tag."},
                    new String[]{"coll.shop",
                            "Down or up? Down, you buy light. Up, you sell whatever you found that’s still worth anything. Which is rarely much. People bring up the strangest junk and want gold for it."},
                    new String[]{"coll.rumor.oil",
                            "Folk come up babbling about a watcher, a presence, eyes in the dark. You know what I sell to those folk? More oil. Whatever’s down there, it’s never once stopped a man from needing more oil."},
                    new String[]{"coll.rumor.lampworks",
                            "Furthest I go’s the lamp-house — the Lamp-works, second level. Good trade there, people coming up are scared and scared pays full price. Past that? Nothing past that’s worth a markup. Past that you don’t come back to spend it."},
                    new String[]{"coll.truth.line",
                            "The painted line’s real, if that’s your question. I’ve seen it. I don’t cross it. Not ’cause of stories — ’cause everyone who does stops buying oil from me, and I notice when a customer stops existing."},
                    new String[]{"coll.truth.twolamps",
                            "Keep one lamp more than you think you need. That’s not wisdom, that’s stock advice. The man with two lamps comes back to spend. The man with one comes back as a story. I’d rather you came back to spend."},
                    new String[]{"coll.quest.offer",
                            "You’re going down past where I go. Fine. Take this sealed jar to the third lamp on the Lamp-works stair — it’s been dark for years, some lampwright’s old stand, number’s worn off. Light it. I’ll knock the rope off your next bill. I don’t like a dark stand on my route, bad for trade."},
                    new String[]{"coll.bye",
                            "Buy and go. You know where I am. I’m always where the oil is."}),

            // ---- DOB — descends with the group (npc_key: dob) ----
            "dob", List.of(
                    new String[]{"dob.greet.bravado",
                            "Right, I’ve been down to the second level loads of times, loads, so just — stick behind me and we’re golden. Loads of times. Twice. Twice is loads."},
                    new String[]{"dob.greet.alert",
                            "I’m not scared, before you ask. I’m *alert*. There’s a difference and my mum says it’s a good quality."},
                    new String[]{"dob.chatter.lampworks",
                            "See, this is fine. Lamps, smell of oil, nothing weird. People worked here. Normal job, normal — okay, why’s it so *tall*, the ceiling, down here. Was it always this tall? I don’t remember tall."},
                    new String[]{"dob.chatter.cisterns",
                            "Don’t drink the still water, that’s Cistern 7, that one’s gone bad — my uncle said. Or was it 7’s the good one. One of ’em’s good. Let’s not test it. Let’s super not test it."},
                    new String[]{"dob.chatter.line",
                            "There’s the line. The painted one. We’re — we’re not crossing that, are we. Tell me we’re stopping at the line. Aro said cross it but Aro’s a liar, everyone knows Aro’s a liar, why’d I even — we’re stopping at the line, right?"},
                    new String[]{"dob.truth.lied",
                            "Okay — real talk — I’ve never been past the Lamp-works. I lied. Twice was a lie, it was once and I cried on the way up. I just wanted to come ’cause everyone treats me like a kid. I don’t know what’s down there any more than you do."},
                    new String[]{"dob.truth.lamp",
                            "I keep my lamp on me. Not letting go of it. You can have my rope, you can have my rations, you cannot have my lamp, I will not be the one whose light goes out, I’ve *heard* what they say about the ones whose light goes out."},
                    new String[]{"dob.react.good",
                            "I feel — okay, this is going to sound stupid — I feel better next to you lot. Like the dark’s paying attention, but not to *us*. Not while we’re together. Stay close though, yeah?"}),

            // ---- OLD PELL — won’t descend; remembers your conduct (npc_key: old-pell) ----
            "old-pell", List.of(
                    new String[]{"old-pell.greet.neutral",
                            "I won’t go down, so don’t ask. People always ask. They think I’m being dramatic. I went down once. That was the whole of my going-down. You’ll understand or you won’t."},
                    new String[]{"old-pell.greet.again",
                            "Sit if you like. Don’t sit if you don’t. I’m not lonely, I’m just old, the two get confused."},
                    new String[]{"old-pell.memory.kinds",
                            "I knew people who went down keeping every little rule like it was nothing, like a game, and they came up and they were *here*, you understand, all the way here, behind their own eyes. And I knew the other kind. I don’t say what happened to the other kind. You’ll know it if you see it. You’ll wish you didn’t."},
                    new String[]{"old-pell.memory.seventh",
                            "There were seven things you minded down there. I minded six of them. Six. I have spent a long time thinking about the seventh, and what it would’ve cost me to mind it, and I think now it would’ve cost me less than the not-minding has."},
                    new String[]{"old-pell.truth.watched",
                            "I’ll tell you the only true thing I have. It doesn’t chase. Whatever’s down there, it does not chase you. It waits, and it watches, and it takes what stops being watched. So be watched. Stay where your people can see you. That’s all I’ve got and it’s worth more than every map Aro’s ever sold."},
                    new String[]{"old-pell.react.good",
                            "I’ll remember you came back right. That’s not nothing, a person remembering you right. It’s most of what I’ve got left to give."},
                    new String[]{"old-pell.bye",
                            "Go on. I’ll be here. Where else."}));

    /**
     * Per-player, per-townsperson conversation cursor: how many utterances this player has already heard
     * from this townsperson. The next click speaks index {@code cursor % size}, then increments. Keyed by
     * {@code playerUuid + ":" + townspersonId}. In-memory only — a restart resets to the greet, which is
     * the right feel for casual townsfolk chat (and keeps this lane touching NO persistence).
     */
    private final Map<String, Integer> cursors = new ConcurrentHashMap<>();

    private final TownsfolkNpc townsfolk;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;

    public TownsfolkNpcListener(TownsfolkNpc townsfolk, RateLimiter rateLimiter,
                                Scheduler scheduler, Safety safety) {
        this.townsfolk = townsfolk;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
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

            List<String[]> convo = LINES.get(id);
            if (convo == null || convo.isEmpty()) return;  // no authored lines for this id — silent

            String cdKey = "townsfolk:" + p.getUniqueId() + ":" + id;
            if (!rateLimiter.tryCooldown(cdKey, OPEN_COOLDOWN_MS)) return;

            // Advance the per-player conversation cursor for this townsperson and pick the utterance.
            String curKey = p.getUniqueId() + ":" + id;
            int idx = cursors.getOrDefault(curKey, 0);
            String[] utterance = convo.get(idx % convo.size());
            cursors.put(curKey, idx + 1);

            speak(p, id, utterance[1]);
        });
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
