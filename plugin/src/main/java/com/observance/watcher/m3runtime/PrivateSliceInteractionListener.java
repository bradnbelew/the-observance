package com.observance.watcher.m3runtime;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.io.IOException;

/** Distinct record-reading and single-ledger filing interactions for authored M3 v5. */
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
        PrivateSliceWorld.ReferenceSurface reference = slice.referenceAt(event.getClickedBlock().getLocation());
        if (evidence == null && submission == null && reference == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        try {
            if (evidence != null) {
                state.commitObservation(evidence.findingId(), evidence.sourceId(), contributor(player));
                if (evidence.presentation() == PrivateSliceWorld.Presentation.NATIVE_BOOK) {
                    slice.openEvidenceBook(player, evidence);
                } else {
                    player.sendActionBar(Component.text("Field record entered in the examiner's working file."));
                }
                return;
            }
            if (reference != null) {
                slice.openReferenceBook(player, reference, state);
                return;
            }
            slice.openFilingLedger(player, state);
        } catch (IOException | IllegalArgumentException | IllegalStateException failure) {
            player.sendActionBar(Component.text("Record desk unavailable: " + safe(failure.getMessage())));
        }
    }

    private static String contributor(Player player) {
        return player.getUniqueId().toString();
    }

    private static String safe(String message) {
        return message == null ? "unknown" : message.replaceAll("[^A-Za-z0-9_.:/=;,' -]", "_");
    }
}
