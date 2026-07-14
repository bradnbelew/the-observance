package com.observance.watcher.v5runtime.mechanics;

import com.observance.watcher.v5runtime.FixtureTransform.LocalOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Immutable snapshot of only the exact site components used for one evaluation. */
public record MechanicObservation(
        UUID actor,
        String siteId,
        Optional<String> answer,
        Set<String> boundComponents,
        Map<String, String> blockTypes,
        Map<String, FrameState> frames,
        Map<String, Map<Integer, MechanicItem>> inventories,
        Map<String, Map<Integer, MechanicItem>> bookshelfSlots,
        Map<String, MechanicItem> books,
        Set<String> openedSources,
        Set<String> openedBooks,
        Set<String> sessionEvents,
        Set<LocalOffset> waterOffsets,
        Map<String, String> selectorValues,
        Set<String> actorOperated,
        Set<String> actorDidNotOperate,
        Optional<String> handleComponent,
        Optional<String> actorViewSide,
        Map<String, List<String>> stringLists,
        Map<String, String> stringFacts,
        Map<String, Integer> integerFacts,
        Map<String, Boolean> booleanFacts) {

    public MechanicObservation {
        Objects.requireNonNull(actor, "actor");
        requireText(siteId, "siteId");
        answer = Objects.requireNonNull(answer, "answer");
        boundComponents = Set.copyOf(boundComponents);
        blockTypes = Map.copyOf(blockTypes);
        frames = Map.copyOf(frames);
        inventories = deepItemMap(inventories);
        bookshelfSlots = deepItemMap(bookshelfSlots);
        books = Map.copyOf(books);
        openedSources = Set.copyOf(openedSources);
        openedBooks = Set.copyOf(openedBooks);
        sessionEvents = Set.copyOf(sessionEvents);
        waterOffsets = Set.copyOf(waterOffsets);
        selectorValues = Map.copyOf(selectorValues);
        actorOperated = Set.copyOf(actorOperated);
        actorDidNotOperate = Set.copyOf(actorDidNotOperate);
        handleComponent = Objects.requireNonNull(handleComponent, "handleComponent");
        actorViewSide = Objects.requireNonNull(actorViewSide, "actorViewSide");
        Map<String, List<String>> immutableLists = new HashMap<>();
        stringLists.forEach((key, value) -> immutableLists.put(key, List.copyOf(value)));
        stringLists = Map.copyOf(immutableLists);
        stringFacts = Map.copyOf(stringFacts);
        integerFacts = Map.copyOf(integerFacts);
        booleanFacts = Map.copyOf(booleanFacts);
    }

    public static Builder builder(UUID actor, String siteId) {
        return new Builder(actor, siteId);
    }

    public boolean booleanFact(String key) {
        return Boolean.TRUE.equals(booleanFacts.get(key));
    }

    public int integerFact(String key) {
        return integerFacts.getOrDefault(key, 0);
    }

    private static Map<String, Map<Integer, MechanicItem>> deepItemMap(
            Map<String, Map<Integer, MechanicItem>> source) {
        Map<String, Map<Integer, MechanicItem>> result = new HashMap<>();
        source.forEach((key, value) -> result.put(key, Map.copyOf(value)));
        return Map.copyOf(result);
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
    }

    public record FrameState(List<MechanicItem> items, List<Integer> rotations) {
        public FrameState {
            items = List.copyOf(items);
            rotations = List.copyOf(rotations);
            if (items.size() != rotations.size()) {
                throw new IllegalArgumentException("frame item/rotation counts differ");
            }
            for (int rotation : rotations) {
                if (rotation < 0 || rotation > 7) {
                    throw new IllegalArgumentException("frame rotation outside 0..7");
                }
            }
        }
    }

    /** Mutable construction helper used by Bukkit capture and the table-driven simulator. */
    public static final class Builder {
        private final UUID actor;
        private final String siteId;
        private String answer;
        private final Set<String> bound = new HashSet<>();
        private final Map<String, String> blockTypes = new HashMap<>();
        private final Map<String, FrameState> frames = new HashMap<>();
        private final Map<String, Map<Integer, MechanicItem>> inventories = new HashMap<>();
        private final Map<String, Map<Integer, MechanicItem>> shelfSlots = new HashMap<>();
        private final Map<String, MechanicItem> books = new HashMap<>();
        private final Set<String> openedSources = new HashSet<>();
        private final Set<String> openedBooks = new HashSet<>();
        private final Set<String> sessionEvents = new HashSet<>();
        private final Set<LocalOffset> water = new HashSet<>();
        private final Map<String, String> selectors = new HashMap<>();
        private final Set<String> operated = new HashSet<>();
        private final Set<String> notOperated = new HashSet<>();
        private String handle;
        private String viewSide;
        private final Map<String, List<String>> stringLists = new HashMap<>();
        private final Map<String, String> stringFacts = new HashMap<>();
        private final Map<String, Integer> integerFacts = new HashMap<>();
        private final Map<String, Boolean> booleanFacts = new HashMap<>();

        private Builder(UUID actor, String siteId) {
            this.actor = Objects.requireNonNull(actor, "actor");
            requireText(siteId, "siteId");
            this.siteId = siteId;
        }

        public Builder answer(String value) { answer = value; return this; }
        public Builder bind(String component, String blockType) {
            bound.add(component); blockTypes.put(component, blockType); return this;
        }
        public Builder bind(String component) { bound.add(component); return this; }
        public Builder frames(String component, List<MechanicItem> items, List<Integer> rotations) {
            bound.add(component); frames.put(component, new FrameState(items, rotations)); return this;
        }
        public Builder inventory(String component, Map<Integer, MechanicItem> slots) {
            bound.add(component); inventories.put(component, new HashMap<>(slots)); return this;
        }
        public Builder bookshelf(String component, Map<Integer, MechanicItem> slots) {
            bound.add(component); shelfSlots.put(component, new HashMap<>(slots)); return this;
        }
        public Builder book(String component, MechanicItem book) {
            bound.add(component); books.put(component, book); return this;
        }
        public Builder openedSource(String source) { openedSources.add(source); return this; }
        public Builder openedBook(String source) { openedBooks.add(source); return this; }
        public Builder sessionEvent(String event) { sessionEvents.add(event); return this; }
        public Builder water(LocalOffset offset) { water.add(offset); return this; }
        public Builder selector(String component, String value) {
            selectors.put(component, value); return this;
        }
        public Builder operated(String component) { operated.add(component); return this; }
        public Builder didNotOperate(String component) { notOperated.add(component); return this; }
        public Builder handle(String component) { handle = component; return this; }
        public Builder viewSide(String value) { viewSide = value; return this; }
        public Builder stringList(String key, List<String> value) {
            stringLists.put(key, new ArrayList<>(value)); return this;
        }
        public Builder stringFact(String key, String value) { stringFacts.put(key, value); return this; }
        public Builder integerFact(String key, int value) { integerFacts.put(key, value); return this; }
        public Builder booleanFact(String key, boolean value) { booleanFacts.put(key, value); return this; }

        public MechanicObservation build() {
            return new MechanicObservation(actor, siteId, Optional.ofNullable(answer), bound,
                    blockTypes, frames, inventories, shelfSlots, books, openedSources, openedBooks,
                    sessionEvents, water, selectors, operated, notOperated,
                    Optional.ofNullable(handle), Optional.ofNullable(viewSide), stringLists,
                    stringFacts, integerFacts, booleanFacts);
        }
    }
}
