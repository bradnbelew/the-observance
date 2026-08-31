package com.observance.watcher.morrow.continuity;

import com.observance.watcher.morrow.continuity.AccountContinuityAuthority.Challenge;
import com.observance.watcher.morrow.continuity.AccountContinuityAuthority.DisconnectReceipt;
import com.observance.watcher.morrow.continuity.AccountContinuityAuthority.Progress;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Atomic release-bound M09 challenge, disconnect, echo log, return, and identity receipt. */
public final class AccountContinuityProgressStore {
    private static final String HEADER = "morrow-account-continuity-progress-v1";
    private final Path path; private final String releaseId;
    public AccountContinuityProgressStore(Path path, String releaseId) {
        this.path = Objects.requireNonNull(path, "path"); this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
    }
    public synchronized Progress load() throws IOException {
        if (!Files.exists(path)) return Progress.initial();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.size() != 14 || !HEADER.equals(lines.get(0))) throw new IOException("invalid M09 progress record");
        String body = String.join("\n", lines.subList(0, 13)) + "\n";
        if (!lines.get(13).equals("hash=" + sha256(body))) throw new IOException("M09 progress hash mismatch");
        if (!releaseId.equals(value(lines.get(1), "release="))) throw new IOException("M09 progress release mismatch");
        try {
            long revision = Long.parseLong(value(lines.get(2), "revision="));
            boolean armed = flag(value(lines.get(3), "armed="));
            boolean observed = flag(value(lines.get(4), "continuation-observed="));
            boolean authenticated = flag(value(lines.get(5), "authenticated="));
            String player = value(lines.get(6), "challenge.player=");
            String challengeId = value(lines.get(7), "challenge.id=");
            String secretHash = value(lines.get(8), "challenge.secret-sha256=");
            Challenge challenge = null;
            if (!player.isBlank() || !challengeId.isBlank() || !secretHash.isBlank())
                challenge = new Challenge(UUID.fromString(player), challengeId, secretHash);
            String disconnectTick = value(lines.get(9), "disconnect.tick=");
            String actions = value(lines.get(10), "disconnect.actions=");
            String actionHash = value(lines.get(11), "disconnect.action-sha256=");
            DisconnectReceipt disconnect = null;
            if (!disconnectTick.isBlank() || !actions.isBlank() || !actionHash.isBlank()) {
                List<String> decoded = List.of(new String(Base64.getUrlDecoder().decode(actions),
                        StandardCharsets.UTF_8).split("\u001f", -1));
                disconnect = new DisconnectReceipt(Long.parseLong(disconnectTick), decoded, actionHash);
            }
            String returned = value(lines.get(12), "returned.tick=");
            return new Progress(challenge, armed, disconnect, returned.isBlank() ? null : Long.parseLong(returned),
                    observed, authenticated, revision);
        } catch (IllegalArgumentException failure) { throw new IOException("invalid M09 progress field", failure); }
    }
    public synchronized Progress save(Progress progress) throws IOException {
        Challenge challenge = progress.challenge(); DisconnectReceipt disconnect = progress.disconnect();
        String actions = disconnect == null ? "" : Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.join("\u001f", disconnect.echoActions()).getBytes(StandardCharsets.UTF_8));
        String body = HEADER + "\nrelease=" + releaseId + "\nrevision=" + progress.revision()
                + "\narmed=" + progress.armed() + "\ncontinuation-observed=" + progress.continuationObserved()
                + "\nauthenticated=" + progress.authenticated()
                + "\nchallenge.player=" + (challenge == null ? "" : challenge.playerId())
                + "\nchallenge.id=" + (challenge == null ? "" : challenge.challengeId())
                + "\nchallenge.secret-sha256=" + (challenge == null ? "" : challenge.secretSha256())
                + "\ndisconnect.tick=" + (disconnect == null ? "" : disconnect.serverTick())
                + "\ndisconnect.actions=" + actions
                + "\ndisconnect.action-sha256=" + (disconnect == null ? "" : disconnect.actionSha256())
                + "\nreturned.tick=" + (progress.returnedTick() == null ? "" : progress.returnedTick()) + "\n";
        Path parent = path.getParent(); if (parent != null) Files.createDirectories(parent);
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.write(temporary, (body + "hash=" + sha256(body) + "\n").getBytes(StandardCharsets.UTF_8));
        try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
        Progress verified = load(); if (!verified.equals(progress)) throw new IOException("M09 progress readback mismatch");
        return verified;
    }
    private static boolean flag(String value) throws IOException {
        if ("true".equals(value)) return true; if ("false".equals(value)) return false;
        throw new IOException("invalid M09 boolean");
    }
    private static String value(String line, String prefix) throws IOException {
        if (!line.startsWith(prefix)) throw new IOException("invalid M09 progress field"); return line.substring(prefix.length());
    }
    private static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
