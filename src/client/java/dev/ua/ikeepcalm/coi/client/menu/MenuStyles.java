package dev.ua.ikeepcalm.coi.client.menu;

import com.google.gson.JsonObject;
import java.util.Locale;

/**
 * The enum words on the wire, folded onto this client's enums.
 * <p>
 * An unknown word is always the type's default, never a failure: that is what
 * lets a newer plugin name a style this build has never heard of and still get
 * a component drawn. Synonyms are accepted where the two repos have disagreed
 * in the past ({@code warn}/{@code warning}, {@code center}/{@code centre}).
 */
final class MenuStyles {

    private MenuStyles() {
    }

    /**
     * The style word at {@code key}, lower-cased and length-capped — every
     * lookup below starts here.
     */
    static String word(JsonObject node, String key) {
        return MenuJson.string(node, key, MenuLimits.MAX_WORD).toLowerCase(Locale.ROOT);
    }

    static MenuComponent.TextStyle text(String value) {
        return switch (value) {
            case "muted" -> MenuComponent.TextStyle.MUTED;
            case "heading" -> MenuComponent.TextStyle.HEADING;
            case "warn", "warning" -> MenuComponent.TextStyle.WARN;
            case "danger", "error" -> MenuComponent.TextStyle.DANGER;
            case "success" -> MenuComponent.TextStyle.SUCCESS;
            default -> MenuComponent.TextStyle.BODY;
        };
    }

    static MenuComponent.ButtonStyle button(String value) {
        return switch (value) {
            case "primary" -> MenuComponent.ButtonStyle.PRIMARY;
            case "danger" -> MenuComponent.ButtonStyle.DANGER;
            case "success" -> MenuComponent.ButtonStyle.SUCCESS;
            case "ghost" -> MenuComponent.ButtonStyle.GHOST;
            default -> MenuComponent.ButtonStyle.SECONDARY;
        };
    }

    static MenuComponent.Align align(String value) {
        return switch (value) {
            case "center", "centre" -> MenuComponent.Align.CENTER;
            case "right" -> MenuComponent.Align.RIGHT;
            default -> MenuComponent.Align.LEFT;
        };
    }

    static MenuComponent.GaugeStyle gauge(String value) {
        return switch (value) {
            case "ring" -> MenuComponent.GaugeStyle.RING;
            case "segments", "segmented" -> MenuComponent.GaugeStyle.SEGMENTS;
            default -> MenuComponent.GaugeStyle.BAR;
        };
    }

    static MenuComponent.HeroStyle hero(String value) {
        return "ring".equals(value) ? MenuComponent.HeroStyle.RING : MenuComponent.HeroStyle.PLAIN;
    }

    static MenuComponent.StepStyle step(String value) {
        return "timeline".equals(value) ? MenuComponent.StepStyle.TIMELINE : MenuComponent.StepStyle.NUMBERED;
    }

    static MenuComponent.TileSize tile(String value) {
        return switch (value) {
            case "small" -> MenuComponent.TileSize.SMALL;
            case "large" -> MenuComponent.TileSize.LARGE;
            default -> MenuComponent.TileSize.MEDIUM;
        };
    }

    /**
     * The tri-state, folded down once here. An unknown word is not a third
     * answer: it falls back to the {@code ok} boolean the wire has always
     * carried, which is also what an absent {@code state} does.
     */
    static MenuComponent.CheckState check(JsonObject node) {
        return switch (word(node, "state")) {
            case "ok", "yes", "done" -> MenuComponent.CheckState.OK;
            case "no", "fail", "failed" -> MenuComponent.CheckState.NO;
            case "pending", "wait", "waiting" -> MenuComponent.CheckState.PENDING;
            default -> MenuJson.bool(node, "ok") ? MenuComponent.CheckState.OK : MenuComponent.CheckState.NO;
        };
    }
}
