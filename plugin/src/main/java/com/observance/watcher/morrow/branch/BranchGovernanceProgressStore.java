package com.observance.watcher.morrow.branch;

import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Anchor;
import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Choice;
import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Ending;
import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Progress;
import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Rule;
import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Window;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Atomic release-bound M12 rollback, policy, governance, and coda progress. */
public final class BranchGovernanceProgressStore {
    private static final String HEADER = "morrow-branch-governance-progress-v1";
    private final Path path; private final String releaseId;
    public BranchGovernanceProgressStore(Path path, String releaseId) {
        this.path = Objects.requireNonNull(path, "path"); this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
    }
    public synchronized Progress load() throws IOException {
        if (!Files.exists(path)) return Progress.initial();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.size() != 14 || !HEADER.equals(lines.get(0))) throw new IOException("invalid M12 progress record");
        String body = String.join("\n", lines.subList(0, 13)) + "\n";
        if (!lines.get(13).equals("hash=" + sha256(body))) throw new IOException("M12 progress hash mismatch");
        if (!releaseId.equals(value(lines.get(1), "release="))) throw new IOException("M12 progress release mismatch");
        try {
            long revision = Long.parseLong(value(lines.get(2), "revision=")); String run = value(lines.get(3), "window.id=");
            Window window = run.isBlank() ? null : new Window(run, Long.parseLong(value(lines.get(4), "window.started=")),
                    Long.parseLong(value(lines.get(5), "window.expires=")));
            Set<Anchor> anchors = EnumSet.noneOf(Anchor.class); String anchorText = value(lines.get(6), "anchors=");
            if (!anchorText.isBlank()) for (String name : anchorText.split(",", -1)) anchors.add(Anchor.valueOf(name));
            Map<Rule, Choice> rules = new EnumMap<>(Rule.class); String ruleText = value(lines.get(8), "rules=");
            if (!ruleText.isBlank()) for (String pair : ruleText.split(",", -1)) {
                String[] parts = pair.split(":", -1); if (parts.length != 2) throw new IllegalArgumentException("invalid M12 rule pair");
                rules.put(Rule.valueOf(parts[0]), Choice.valueOf(parts[1]));
            }
            String endingText = value(lines.get(9), "ending="); Ending ending = endingText.isBlank() ? null : Ending.valueOf(endingText);
            return new Progress(window, anchors, flag(value(lines.get(7), "anchors-committed=")), rules, ending,
                    flag(value(lines.get(10), "policy-committed=")), flag(value(lines.get(11), "governance-authorized=")),
                    flag(value(lines.get(12), "coda-started=")), revision);
        } catch (IllegalArgumentException failure) { throw new IOException("invalid M12 progress field", failure); }
    }
    public synchronized void save(Progress progress) throws IOException {
        Objects.requireNonNull(progress, "progress");
        String run = progress.window() == null ? "" : progress.window().runId();
        String started = progress.window() == null ? "0" : Long.toString(progress.window().startedTick());
        String expires = progress.window() == null ? "0" : Long.toString(progress.window().expiresTick());
        String anchors = progress.anchors().stream().sorted().map(Anchor::name).reduce((a, b) -> a + "," + b).orElse("");
        String rules = progress.rules().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey().name() + ":" + entry.getValue().name()).reduce((a, b) -> a + "," + b).orElse("");
        String body = HEADER + "\nrelease=" + releaseId + "\nrevision=" + progress.revision()
                + "\nwindow.id=" + run + "\nwindow.started=" + started + "\nwindow.expires=" + expires
                + "\nanchors=" + anchors + "\nanchors-committed=" + flag(progress.anchorsCommitted())
                + "\nrules=" + rules + "\nending=" + (progress.ending() == null ? "" : progress.ending().name())
                + "\npolicy-committed=" + flag(progress.policyCommitted())
                + "\ngovernance-authorized=" + flag(progress.governanceAuthorized())
                + "\ncoda-started=" + flag(progress.codaStarted()) + "\n";
        Path parent = path.toAbsolutePath().getParent(); if (parent == null) throw new IOException("M12 progress has no parent");
        Files.createDirectories(parent); Path temporary = parent.resolve(path.getFileName() + ".tmp");
        Files.writeString(temporary, body + "hash=" + sha256(body) + "\n", StandardCharsets.UTF_8);
        try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.deleteIfExists(temporary); throw new IOException("filesystem does not support atomic M12 progress", unsupported);
        }
        if (!load().equals(progress)) throw new IOException("M12 progress readback mismatch");
    }
    private static String flag(boolean value) { return value ? "1" : "0"; }
    private static boolean flag(String value) throws IOException {
        if ("1".equals(value)) return true; if ("0".equals(value)) return false; throw new IOException("invalid M12 boolean");
    }
    private static String value(String line, String prefix) throws IOException {
        if (!line.startsWith(prefix)) throw new IOException("invalid M12 progress field"); return line.substring(prefix.length());
    }
    private static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
