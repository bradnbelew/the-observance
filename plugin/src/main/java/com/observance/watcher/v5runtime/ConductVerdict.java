package com.observance.watcher.v5runtime;

import java.util.Locale;

public enum ConductVerdict {
    SOLO,
    DIVIDED,
    UNANIMOUS,
    PERSISTENT;

    public String wireValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ConductVerdict fromWireValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("conduct verdict cannot be null");
        }
        return switch (value) {
            case "solo" -> SOLO;
            case "divided" -> DIVIDED;
            case "unanimous" -> UNANIMOUS;
            case "persistent" -> PERSISTENT;
            default -> throw new IllegalArgumentException("unknown conduct verdict " + value);
        };
    }
}
