package com.observance.watcher.v5runtime.ritual;

import com.observance.watcher.v5runtime.ConductVerdict;
import java.util.Locale;

/** Closed V5 branch vocabularies. There is deliberately no operator-defined branch value. */
public final class RitualChoices {
    private RitualChoices() {
    }

    public enum WrenTopic {
        BRIDGE_REVISION,
        NAMES_AND_FEARS,
        PRIOR_COMPANY_DISAPPEARANCE
    }

    public enum ClosingChoice {
        YOU_CHOSE_TO_SEND_THEM
    }

    public enum ClosingReply {
        ADMISSION_AND_FEAR_WITHOUT_COERCION_EXCUSE
    }

    public enum WrenOutcome {
        CONDEMN,
        UNDERSTAND,
        FREE;

        public String wireValue() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static WrenOutcome fromWireValue(String value) {
            return valueOf(value.toUpperCase(Locale.ROOT));
        }
    }

    public enum NameTreatment {
        PUBLISH,
        RELEASE_UNNAMED;

        public String wireValue() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static NameTreatment fromWireValue(String value) {
            return valueOf(value.toUpperCase(Locale.ROOT));
        }
    }

    public record EndingDimensions(
            WrenOutcome wrenOutcome,
            NameTreatment nameTreatment,
            ConductVerdict conductVerdict) {
        public EndingDimensions {
            if (wrenOutcome == null || nameTreatment == null || conductVerdict == null) {
                throw new IllegalArgumentException("all ending dimensions are required");
            }
        }
    }
}
