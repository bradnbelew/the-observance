package com.observance.watcher.m3runtime;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Player-facing evidence, filing, catch-up, and accessibility interactions for the authored v2 slice. */
public final class PrivateSliceInteractionListener implements Listener {
    private final PrivateSliceWorld slice;
    private final PrivateSliceState state;

    public PrivateSliceInteractionListener(PrivateSliceWorld slice, PrivateSliceState state) {
        this.slice = slice;
        this.state = state;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        PrivateSliceWorld.EvidenceSurface evidence = slice.evidenceAt(event.getClickedBlock().getLocation());
        PrivateSliceWorld.SubmissionSurface submission = slice.submissionAt(event.getClickedBlock().getLocation());
        if (evidence == null && submission == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        try {
            if (evidence != null) inspect(player, evidence);
            else file(player, submission);
        } catch (IOException | IllegalArgumentException | IllegalStateException failure) {
            player.sendMessage(Component.text("Intake filing refused: " + safe(failure.getMessage())));
        }
    }

    private void inspect(Player player, PrivateSliceWorld.EvidenceSurface evidence) throws IOException {
        state.commitObservation(evidence.findingId(), evidence.sourceId(), contributor(player));
        player.sendMessage(Component.text("[" + evidence.title() + "]"));
        player.sendMessage(Component.text(evidence.body()));
        player.sendMessage(Component.text("Observation retained for " + evidence.findingId()
                + ": " + evidence.sourceId()));
    }

    private void file(Player player, PrivateSliceWorld.SubmissionSurface submission) throws IOException {
        if (submission.findingId() == null) {
            replay(player);
            return;
        }
        if (PrivateSliceState.SYNTHESIS.equals(submission.findingId())) {
            state.commitFinding(PrivateSliceState.SYNTHESIS, PrivateSliceState.BASE_FINDINGS,
                    contributor(player));
            slice.setGate(true);
            player.sendMessage(Component.text("INTAKE FINDING FILED. The controlled gate is open."));
            return;
        }
        Set<String> observed = state.observedSources(submission.findingId());
        if (observed.size() < 2) {
            throw new IllegalStateException("inspect at least two independent authored sources for "
                    + submission.findingId());
        }
        List<String> sources = new ArrayList<>(observed);
        sources.sort(String::compareTo);
        state.commitFinding(submission.findingId(), sources, contributor(player));
        player.sendMessage(Component.text(submission.findingId() + " filed from " + sources.size()
                + " retained source receipts."));
    }

    private void replay(Player player) {
        List<String> committed = new ArrayList<>();
        for (String finding : PrivateSliceState.BASE_FINDINGS) {
            if (state.findingCommitted(finding)) committed.add(finding);
        }
        if (state.findingCommitted(PrivateSliceState.SYNTHESIS)) committed.add(PrivateSliceState.SYNTHESIS);
        player.sendMessage(Component.text("FIELD ARCHIVE — committed: "
                + (committed.isEmpty() ? "none" : String.join(", ", committed))));
        player.sendMessage(Component.text("Changed place: controlled gate " + (state.gateOpen() ? "OPEN" : "CLOSED")));
        player.sendMessage(Component.text("Remaining dispute: compare construction phase, viable capacity, and descent motive."));
        player.sendMessage(Component.text("Accessibility readback — west view: one copied capacity digit appears freshly overwritten."));
        player.sendMessage(Component.text("Accessibility readback — east view: the same digit remains worn and unchanged."));
        player.sendMessage(Component.text("Neither selective view is evidence for a P4 finding."));
    }

    private static String contributor(Player player) {
        return player.getUniqueId().toString();
    }

    private static String safe(String message) {
        return message == null ? "unknown" : message.replaceAll("[^A-Za-z0-9_.:/= -]", "_");
    }
}
