package dev.ua.ikeepcalm.coi.client.screen.ability;

import dev.ua.ikeepcalm.coi.client.ability.AbilityInfo;

import java.util.Locale;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/**
 * Every word the ability picker puts on a row or in its tooltip.
 * <p>
 * The kind tag and the block reason are whitelisted rather than interpolated,
 * so a kind or a block the server invents can never leak a raw lang key onto
 * the screen.
 */
final class PickerLabels {

    private PickerLabels() {
    }

    static Component kindTag(AbilityInfo info) {
        if (info == null) return Component.empty();
        if (info.locked()) return Component.translatable("screen.coi.picker_kind_locked");
        if (info.blocked()) return Component.translatable("screen.coi.picker_kind_blocked");
        // Whitelisted, so a kind the server invents never leaks a raw lang key
        String kind = switch (info.kind()) {
            case AbilityInfo.KIND_ACTIVATED -> AbilityInfo.KIND_ACTIVATED;
            case AbilityInfo.KIND_PASSIVE -> AbilityInfo.KIND_PASSIVE;
            default -> AbilityInfo.KIND_ACTIVE;
        };
        return Component.translatable("screen.coi.picker_kind_" + kind);
    }

    static Component cost(AbilityInfo info) {
        if (info == null) return Component.translatable("screen.coi.picker_meta_cost_none");
        if (info.drainPerSecond() > 0) {
            return Component.translatable("screen.coi.picker_meta_drain",
                    String.format(Locale.ROOT, "%.1f", info.drainPerSecond()));
        }
        if (info.cost() > 0) {
            return Component.translatable("screen.coi.picker_meta_cost", info.cost());
        }
        return Component.translatable("screen.coi.picker_meta_cost_none");
    }

    static Component cooldown(AbilityInfo info) {
        if (info == null || info.cooldownSeconds() <= 0) {
            return Component.translatable("screen.coi.picker_meta_cooldown_none");
        }
        return Component.translatable("screen.coi.picker_meta_cooldown", info.cooldownSeconds());
    }

    /**
     * The category as a translated word. The ability list carries the raw key
     * (the localized {@code categoryName} only ever rides on state updates), so
     * anything we have no string for falls back to the key itself — still better
     * on screen than a raw lang path.
     */
    static Component category(AbilityInfo info) {
        String category = info == null || info.category() == null ? "" : info.category().toLowerCase(Locale.ROOT);
        if (category.isEmpty()) category = "uncategorized";
        String key = "screen.coi.ability_cat_" + category;
        return Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(category);
    }

    /** Why the ability cannot be used, or null when it can. */
    static Component blockReason(AbilityInfo info) {
        if (info.locked()) return Component.translatable("screen.coi.ability_tooltip_locked");
        if (!info.blocked()) return null;
        return switch (info.blockedBy()) {
            case "hanged" -> Component.translatable("screen.coi.ability_tooltip_blocked_hanged");
            case "devouring" -> Component.translatable("screen.coi.ability_tooltip_blocked_devouring");
            default -> Component.translatable("screen.coi.ability_tooltip_blocked_contract");
        };
    }
}
