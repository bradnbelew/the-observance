package com.observance.watcher.morrow.room04.staticrestore;

import com.observance.watcher.morrow.room04.RecoveryRoom04Manifest;
import com.observance.watcher.morrow.room04.RecoveryRoom04Manifest.Cell;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Collections;

/** Exact M02 world/content authority for the six-cell Static Restore proposal. */
public final class StaticRestoreManifest {
    public static final String INVESTIGATION_ID = "M02";
    public static final String TITLE = "The sixth block";
    public static final String BASELINE_BLOCK = RecoveryRoom04Manifest.AIR;
    public static final String MANIFEST_SHA256 = "870d27b4b21539e496577eff68dc5f26ada71ec321fef9629088e9a8274b28ff";

    public enum Provenance {
        AUTHENTICATED("authenticated", "waxed copper / square mark"),
        INFERRED("inferred", "copper scaffold / diagonal mark"),
        CONFLICTING("conflicting", "split copper / double mark"),
        UNKNOWN("unknown", "no copper / blank mark");

        private final String key;
        private final String accessibleMark;

        Provenance(String key, String accessibleMark) {
            this.key = key;
            this.accessibleMark = accessibleMark;
        }

        public String key() { return key; }
        public String accessibleMark() { return accessibleMark; }
    }

    public enum CandidateId { B01, B02, B03, B04, B05, B06 }

    public record Candidate(
            CandidateId id,
            Cell cell,
            String restoredBlock,
            Provenance factualProvenance,
            String accessibleDescription) {
        public Candidate {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(cell, "cell");
            if (restoredBlock == null
                    || !restoredBlock.matches("minecraft:[a-z0-9_]+(?:\\[[a-z0-9_=,]+])?")) {
                throw new IllegalArgumentException("invalid Static Restore block data");
            }
            Objects.requireNonNull(factualProvenance, "factualProvenance");
            if (accessibleDescription == null || accessibleDescription.isBlank()) {
                throw new IllegalArgumentException("candidate requires accessible description");
            }
        }
    }

    public enum EvidenceId { CURRENT_ROOM, ARCHIVED_SCREENSHOT, BLOCK_MANIFEST, FINCH_PLANNING_POST }

    public record Evidence(
            EvidenceId id,
            String title,
            String custody,
            String content,
            Set<CandidateId> placedCells) {
        public Evidence {
            Objects.requireNonNull(id, "id");
            if (title == null || title.isBlank() || custody == null || custody.isBlank()
                    || content == null || content.isBlank()) {
                throw new IllegalArgumentException("evidence requires authored text and custody");
            }
            placedCells = Set.copyOf(Objects.requireNonNull(placedCells, "placedCells"));
        }
    }

    public record Pass(int number, List<Candidate> candidates) {
        public Pass {
            if (number < 1 || number > 3) throw new IllegalArgumentException("restore pass is out of bounds");
            candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates"));
            if (candidates.size() != 2) throw new IllegalArgumentException("each visible pass restores two cells");
        }
    }

    private final Map<CandidateId, Candidate> candidates;
    private final Map<Cell, Candidate> byCell;
    private final Map<EvidenceId, Evidence> evidence;
    private final List<Pass> passes;
    private final String manifestSha256;

    public StaticRestoreManifest() {
        LinkedHashMap<CandidateId, Candidate> authored = new LinkedHashMap<>();
        add(authored, CandidateId.B01, new Cell(-4, 0, -1), "minecraft:oak_planks",
                Provenance.AUTHENTICATED, "lower west: oak planks");
        add(authored, CandidateId.B02, new Cell(-3, 0, -1), "minecraft:stripped_oak_log[axis=y]",
                Provenance.AUTHENTICATED, "lower center: stripped oak log");
        add(authored, CandidateId.B03, new Cell(-2, 0, -1), "minecraft:oak_planks",
                Provenance.AUTHENTICATED, "lower east: oak planks");
        add(authored, CandidateId.B04, new Cell(-4, 1, -1), "minecraft:oak_planks",
                Provenance.AUTHENTICATED, "upper west: oak planks");
        add(authored, CandidateId.B05, new Cell(-3, 1, -1), "minecraft:glass",
                Provenance.AUTHENTICATED, "upper center: glass");
        add(authored, CandidateId.B06, new Cell(-2, 1, -1), "minecraft:spruce_planks",
                Provenance.INFERRED, "upper east: spruce planks");
        this.candidates = Collections.unmodifiableMap(new LinkedHashMap<>(authored));

        LinkedHashMap<Cell, Candidate> indexed = new LinkedHashMap<>();
        authored.values().forEach(candidate -> indexed.put(candidate.cell(), candidate));
        this.byCell = Collections.unmodifiableMap(new LinkedHashMap<>(indexed));
        this.passes = List.of(
                new Pass(1, List.of(authored.get(CandidateId.B01), authored.get(CandidateId.B02))),
                new Pass(2, List.of(authored.get(CandidateId.B03), authored.get(CandidateId.B04))),
                new Pass(3, List.of(authored.get(CandidateId.B05), authored.get(CandidateId.B06))));

        EnumMap<EvidenceId, Evidence> sources = new EnumMap<>(EvidenceId.class);
        sources.put(EvidenceId.CURRENT_ROOM, new Evidence(
                EvidenceId.CURRENT_ROOM,
                "Current starter-room restore",
                "Minecraft physical state / Recovery Room 04",
                "The bounded wall is a three-by-two grid. B01 oak planks; B02 stripped oak log; "
                        + "B03 oak planks; B04 oak planks; B05 glass; B06 spruce planks.",
                Set.of(CandidateId.values())));
        sources.put(EvidenceId.ARCHIVED_SCREENSHOT, new Evidence(
                EvidenceId.ARCHIVED_SCREENSHOT,
                "Archived coordinate image 04-A",
                "Copperline attachment SHA-256 custody / 2024-06-18",
                "Text alternative: three-by-two wall viewed from the south. Lower row B01 oak, B02 stripped log, "
                        + "B03 oak. Upper row B04 oak, B05 glass, B06 visibly empty. Grid labels are printed in the image.",
                Set.of(CandidateId.B01, CandidateId.B02, CandidateId.B03, CandidateId.B04, CandidateId.B05)));
        sources.put(EvidenceId.BLOCK_MANIFEST, new Evidence(
                EvidenceId.BLOCK_MANIFEST,
                "Authenticated block manifest 04-A",
                "Copperline region extraction / signed before recovery",
                "Placed cells: B01 oak_planks; B02 stripped_oak_log; B03 oak_planks; B04 oak_planks; "
                        + "B05 glass. The manifest ends after B05 and contains no B06 placement record.",
                Set.of(CandidateId.B01, CandidateId.B02, CandidateId.B03, CandidateId.B04, CandidateId.B05)));
        sources.put(EvidenceId.FINCH_PLANNING_POST, new Evidence(
                EvidenceId.FINCH_PLANNING_POST,
                "Finch planning post",
                "Mossfield planning archive / authored before the loss",
                "finchline: 'Could cap upper-east B06 with one spruce plank after the rail budget. "
                        + "Discussing only; I have not placed it.' No later completion post or placement receipt exists.",
                Set.of()));
        this.evidence = Collections.unmodifiableMap(new EnumMap<>(sources));
        validate();
        this.manifestSha256 = hash(canonicalBytes());
        if (!MANIFEST_SHA256.equals(manifestSha256)) {
            throw new IllegalStateException("M02 manifest bytes drifted: " + manifestSha256);
        }
    }

    public Map<CandidateId, Candidate> candidates() { return candidates; }
    public List<Pass> passes() { return passes; }
    public Map<EvidenceId, Evidence> evidence() { return evidence; }
    public String manifestSha256() { return manifestSha256; }
    public Candidate candidate(CandidateId id) { return candidates.get(Objects.requireNonNull(id, "id")); }
    public Candidate candidate(Cell cell) { return byCell.get(Objects.requireNonNull(cell, "cell")); }
    public Evidence evidence(EvidenceId id) { return evidence.get(Objects.requireNonNull(id, "id")); }
    public Set<Cell> mutableCells() { return Set.copyOf(byCell.keySet()); }
    public Candidate factualInferenceError() { return candidate(CandidateId.B06); }

    private static void add(
            Map<CandidateId, Candidate> target,
            CandidateId id,
            Cell cell,
            String block,
            Provenance provenance,
            String description) {
        target.put(id, new Candidate(id, cell, block, provenance, description));
    }

    private void validate() {
        if (candidates.size() != 6 || byCell.size() != 6 || passes.size() != 3) {
            throw new IllegalStateException("M02 requires exactly six unique cells in three passes");
        }
        RecoveryRoom04Manifest room = new RecoveryRoom04Manifest();
        for (Candidate candidate : candidates.values()) {
            if (!RecoveryRoom04Manifest.BOUNDS.contains(candidate.cell())
                    || !BASELINE_BLOCK.equals(room.expected(candidate.cell()))) {
                throw new IllegalStateException("M02 cell is not bounded empty Room 04 space: " + candidate.id());
            }
            if (RecoveryRoom04Manifest.SPAWN_CELL.equals(candidate.cell())
                    || RecoveryRoom04Manifest.EXIT_CELL.equals(candidate.cell())
                    || RecoveryRoom04Manifest.TERMINAL_CELL.equals(candidate.cell())) {
                throw new IllegalStateException("M02 overlaps a protected Room 04 cell");
            }
        }
        long inferred = candidates.values().stream()
                .filter(candidate -> candidate.factualProvenance() == Provenance.INFERRED).count();
        if (inferred != 1 || factualInferenceError().id() != CandidateId.B06) {
            throw new IllegalStateException("B06 must be the sole factual inference error");
        }
        if (evidence.size() != EvidenceId.values().length
                || evidence(EvidenceId.ARCHIVED_SCREENSHOT).placedCells().contains(CandidateId.B06)
                || evidence(EvidenceId.BLOCK_MANIFEST).placedCells().contains(CandidateId.B06)
                || !evidence(EvidenceId.FINCH_PLANNING_POST).content().contains("not placed")) {
            throw new IllegalStateException("M02 evidence authority no longer proves discussed-but-never-placed B06");
        }
    }

    private byte[] canonicalBytes() {
        StringBuilder text = new StringBuilder("morrow-static-restore-m02-v1\n");
        candidates.values().forEach(candidate -> text.append(candidate.id()).append('|')
                .append(candidate.cell().x()).append(',').append(candidate.cell().y()).append(',')
                .append(candidate.cell().z()).append('|').append(candidate.restoredBlock()).append('|')
                .append(candidate.factualProvenance().key()).append('|')
                .append(candidate.accessibleDescription()).append('\n'));
        evidence.values().stream().sorted(java.util.Comparator.comparing(Evidence::id)).forEach(source -> {
            text.append(source.id()).append('|').append(source.title()).append('|')
                    .append(source.custody()).append('|').append(source.content()).append('|');
            source.placedCells().stream().sorted().forEach(id -> text.append(id).append(','));
            text.append('\n');
        });
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String hash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
