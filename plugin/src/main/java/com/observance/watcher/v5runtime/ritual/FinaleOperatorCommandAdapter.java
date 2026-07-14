package com.observance.watcher.v5runtime.ritual;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/** Branchless `/obs finale` command delegate; existing root-command wiring remains external. */
public final class FinaleOperatorCommandAdapter {
    private final FinaleRite finale;
    private final FinaleBukkitArmExpiry expiry;
    private final String permission;

    public FinaleOperatorCommandAdapter(
            FinaleRite finale, FinaleBukkitArmExpiry expiry, String permission) {
        this.finale = Objects.requireNonNull(finale, "finale");
        this.expiry = Objects.requireNonNull(expiry, "expiry");
        this.permission = permission == null || permission.isBlank()
                ? "observance.admin" : permission;
    }

    /** Receives only the arguments after `/obs finale`. */
    public boolean handle(CommandSender sender, List<String> arguments) {
        Objects.requireNonNull(sender, "sender");
        arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(Component.text("Missing " + permission, NamedTextColor.RED));
            return true;
        }
        if (arguments.isEmpty()) {
            status(sender);
            return true;
        }
        String action = arguments.get(0).toLowerCase(Locale.ROOT);
        try {
            switch (action) {
                case "status" -> {
                    if (arguments.size() != 1) {
                        usage(sender);
                    } else {
                        status(sender);
                    }
                }
                case "arm" -> arm(sender, arguments);
                case "cancel" -> {
                    if (arguments.size() != 1) {
                        usage(sender);
                    } else {
                        FinaleRite.ArmResult result = finale.cancel();
                        expiry.stop();
                        send(sender, result.status().name() + ": " + result.detail(),
                                result.status() == FinaleRite.ArmStatus.CANCELLED);
                    }
                }
                default -> usage(sender);
            }
        } catch (IOException | RuntimeException failure) {
            sender.sendMessage(Component.text(
                    "Finale command failed closed: " + failure.getMessage(), NamedTextColor.RED));
        }
        return true;
    }

    private void arm(CommandSender sender, List<String> arguments) throws IOException {
        if (arguments.size() > 2) {
            usage(sender);
            return;
        }
        Integer seconds = null;
        if (arguments.size() == 2) {
            try {
                seconds = Integer.valueOf(arguments.get(1));
            } catch (NumberFormatException failure) {
                usage(sender);
                return;
            }
        }
        FinaleRite.ArmResult result = finale.arm(sender.getName(), seconds);
        if (result.status() == FinaleRite.ArmStatus.ARMED) {
            expiry.scheduleFromDurableState();
        }
        send(sender, result.status().name() + ": " + result.detail(),
                result.status() == FinaleRite.ArmStatus.ARMED);
    }

    private void status(CommandSender sender) {
        FinaleStateStore.Snapshot snapshot = finale.snapshot();
        sender.sendMessage(Component.text(
                "phase=" + snapshot.phase().name().toLowerCase(Locale.ROOT)
                        + ", wren=" + dash(snapshot.wrenOutcome())
                        + ", name=" + dash(snapshot.nameTreatment())
                        + ", conduct=" + dash(snapshot.conductVerdict())
                        + ", cutoff=" + snapshot.cancelCutoffAt(),
                NamedTextColor.GRAY));
    }

    private static void usage(CommandSender sender) {
        sender.sendMessage(Component.text(
                "Usage: /obs finale status | arm [15-600 seconds] | cancel",
                NamedTextColor.YELLOW));
    }

    private static void send(CommandSender sender, String message, boolean success) {
        sender.sendMessage(Component.text(message,
                success ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private static String dash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
