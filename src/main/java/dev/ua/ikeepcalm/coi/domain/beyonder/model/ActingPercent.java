package dev.ua.ikeepcalm.coi.domain.beyonder.model;

import com.google.gson.JsonObject;
import dev.ua.ikeepcalm.coi.util.JsonRead;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Exact acting progress and threshold-safe display formatting. */
public final class ActingPercent {
    private ActingPercent() {
    }

    /** Uses integer progress when available, with the payload value as a legacy fallback. */
    public static double of(JsonObject node, int acting, int needed) {
        boolean counted = JsonRead.present(node, "acting") && JsonRead.present(node, "needed");
        if (counted && needed > 0 && needed != Integer.MAX_VALUE) {
            return Math.min(100.0, acting * 100.0 / needed);
        }
        return JsonRead.dbl(node, "percent", needed > 0 ? acting * 100.0 / needed : 0);
    }

    /** Truncates a display value so it cannot round up across an advancement threshold. */
    public static double floor(double percent, int decimals) {
        if (!Double.isFinite(percent)) return percent;
        return BigDecimal.valueOf(percent).setScale(decimals, RoundingMode.FLOOR).doubleValue();
    }
}
