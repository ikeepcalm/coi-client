package dev.ua.ikeepcalm.coi.client.screen.ability;

import dev.ua.ikeepcalm.coi.client.ability.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.ability.AbilityRegistry;
import dev.ua.ikeepcalm.coi.client.network.ServerCapabilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The ability picker's list model: what the search leaves standing, how that is
 * flattened into drawable rows, and the pixel arithmetic that follows from rows
 * of unequal height.
 * <p>
 * {@link #filtered()} stays a plain ability list — Enter still picks the first
 * of those — while {@link #rows()} is what actually gets drawn: the same
 * abilities plus Unbind, the group headers and the no-results message.
 */
final class PickerModel {

    enum RowKind {UNBIND, HEADER, SPELL, ABILITY, MESSAGE}

    /**
     * One visual line. Headers and the no-results message are inert: they take up
     * layout space and scroll with everything else, but never answer a click.
     */
    record Row(RowKind kind, String option, String pathway, int sequence) {

        static Row unbind() {
            return new Row(RowKind.UNBIND, null, "", -1);
        }

        static Row message() {
            return new Row(RowKind.MESSAGE, null, "", -1);
        }

        static Row header(String pathway, int sequence) {
            return new Row(RowKind.HEADER, null, pathway, sequence);
        }

        static Row ability(String option, String pathway) {
            return new Row(RowKind.ABILITY, option, pathway, -1);
        }

        boolean clickable() {
            return kind == RowKind.UNBIND || kind == RowKind.ABILITY;
        }
    }

    /** Pathway block, then sequence inside it, then name — the order the headers assume. */
    private static final Comparator<String> LIST_ORDER =
            Comparator.comparing(PickerModel::pathwayOf)
                    .thenComparingInt(PickerModel::sequenceOf)
                    .thenComparing(PickerModel::baseName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(AbilityInfo::extractId)
                    .thenComparingInt(PickerModel::modeOrder);

    /** Matching abilities in display order; Enter still picks the first of these. */
    private final List<String> filtered = new ArrayList<>();
    /** What actually gets drawn: {@link #filtered} plus Unbind, headers and messages. */
    private final List<Row> rows = new ArrayList<>();

    List<String> filtered() {
        return filtered;
    }

    List<Row> rows() {
        return rows;
    }

    /**
     * Search matches the display name, the pathway and the category, so "door",
     * "mobility" and "Traveler" all reach the same row.
     */
    void refilter(String rawQuery) {
        filtered.clear();
        String query = rawQuery.trim().toLowerCase(Locale.ROOT);
        for (String option : AbilityRegistry.getAvailableAbilities()) {
            if (matches(option, query)) filtered.add(option);
        }
        filtered.sort(LIST_ORDER);
        rebuildRows(!rawQuery.isEmpty());
    }

    private static boolean matches(String option, String query) {
        if (query.isEmpty()) return true;
        if (displayNameOf(option).toLowerCase(Locale.ROOT).contains(query)) return true;
        if (pathwayOf(option).contains(query)) return true;
        if (entryLabel(option).toLowerCase(Locale.ROOT).contains(query)) return true;
        AbilityInfo info = infoFor(option);
        return info != null && info.category() != null
                && info.category().toLowerCase(Locale.ROOT).contains(query);
    }

    /**
     * Flattens the sorted list into drawable rows, one header per
     * <em>pathway and sequence</em> — the sequence is the bracket a player
     * actually thinks in, so Fool 5 and Fool 3 are two blocks, not one. The
     * list is already sorted pathway → sequence → name, so a group break is
     * simply "this row's key differs from the last one's".
     */
    private void rebuildRows(boolean searching) {
        rows.clear();

        if (filtered.isEmpty()) {
            if (searching) rows.add(Row.message());
            return;
        }

        String lastPathway = null;
        int lastSequence = Integer.MIN_VALUE;
        String lastId = null;
        for (String option : filtered) {
            String pathway = pathwayOf(option);
            int sequence = sequenceOf(option);
            if (!pathway.equals(lastPathway) || sequence != lastSequence) {
                rows.add(Row.header(pathway, sequence));
                lastPathway = pathway;
                lastSequence = sequence;
            }
            String id = AbilityInfo.extractId(option);
            if (!id.equals(lastId) && hasModes(option))
                rows.add(new Row(RowKind.SPELL, option, pathway, sequence));
            lastId = id;
            rows.add(Row.ability(option, pathway));
        }
    }

    // --- Pixel arithmetic ---

    static int rowHeight(Row row) {
        return switch (row.kind()) {
            case UNBIND -> PickerMetrics.UNBIND_H;
            case HEADER -> PickerMetrics.HEADER_H;
            case SPELL -> 23;
            case MESSAGE -> PickerMetrics.MESSAGE_H;
            case ABILITY -> PickerMetrics.ROW_H;
        };
    }

    int contentHeight() {
        int total = 0;
        for (Row row : rows) total += rowHeight(row);
        return total;
    }

    /**
     * First row index that still leaves the whole tail on screen. Rows have
     * different heights, so this is measured in pixels and answered as an index.
     */
    int maxScroll(int listH) {
        int tail = 0;
        for (int i = rows.size() - 1; i >= 0; i--) {
            tail += rowHeight(rows.get(i));
            if (tail > listH) return i + 1;
        }
        return 0;
    }

    /** Total pixel height of the rows scrolled off the top — the scrollbar's input. */
    int heightAbove(int index) {
        int above = 0;
        for (int i = 0; i < index && i < rows.size(); i++) {
            above += rowHeight(rows.get(i));
        }
        return above;
    }

    // --- Ability lookups ---

    /**
     * Does this server feed ability metadata? The capability reply is the
     * contract, but a list that carries any richer field counts too — that keeps
     * the badges alive in the dev environment, where nothing answers the hello.
     * A protocol-1 list has none of it and would otherwise render a column of
     * em-dashes, which says less than nothing.
     */
    static boolean detectMeta() {
        if (ServerCapabilities.has("ability_meta")) return true;
        for (String option : AbilityRegistry.getAvailableAbilities()) {
            AbilityInfo info = infoFor(option);
            if (info == null) continue;
            if (info.cost() > 0 || info.drainPerSecond() > 0 || info.cooldownSeconds() > 0
                    || !info.description().isEmpty() || info.isToggle()) {
                return true;
            }
        }
        return false;
    }

    static AbilityInfo infoFor(String option) {
        return AbilityRegistry.getAbilityInfo(AbilityInfo.extractId(option));
    }

    /** Protocol 2 names the pathway; otherwise it is the first id segment. */
    static String pathwayOf(String option) {
        AbilityInfo info = infoFor(option);
        String pathway = info != null ? info.pathway() : null;
        return pathway == null || pathway.isEmpty()
                ? AbilityInfo.pathwayOf(AbilityInfo.extractId(option))
                : pathway;
    }

    static int sequenceOf(String option) {
        AbilityInfo info = infoFor(option);
        if (info != null && info.sequence() >= 0) return info.sequence();
        return AbilityInfo.sequenceOf(AbilityInfo.extractId(option));
    }

    static String baseName(String option) {
        AbilityInfo info = infoFor(option);
        return info == null ? AbilityInfo.extractDisplayName(option) : info.localizedName();
    }

    static boolean hasModes(String option) {
        AbilityInfo info = infoFor(option);
        return (info != null && info.hasLeftClick())
                || !dev.ua.ikeepcalm.coi.client.ability.AbilityCategories.get(AbilityInfo.extractId(option)).isEmpty();
    }

    private static int modeOrder(String option) {
        if (!AbilityInfo.extractCategory(option).isEmpty()) return 0;
        if (AbilityInfo.ACTION_LEFT_CLICK.equals(AbilityInfo.extractAction(option))) return 1;
        return dev.ua.ikeepcalm.coi.client.ability.AbilityCategories.get(AbilityInfo.extractId(option)).isEmpty() ? 0 : 2;
    }

    static String entryLabel(String option) {
        var category = dev.ua.ikeepcalm.coi.client.ability.AbilityCategories.find(
                AbilityInfo.extractId(option), AbilityInfo.extractCategory(option));
        if (category != null) return category.name();
        if (AbilityInfo.ACTION_LEFT_CLICK.equals(AbilityInfo.extractAction(option)))
            return net.minecraft.client.resources.language.I18n.get("screen.coi.manual_secondary");
        if (!dev.ua.ikeepcalm.coi.client.ability.AbilityCategories.get(AbilityInfo.extractId(option)).isEmpty())
            return net.minecraft.client.resources.language.I18n.get("screen.coi.manual_current_mode");
        return hasModes(option) ? net.minecraft.client.resources.language.I18n.get("screen.coi.picker_primary") : baseName(option);
    }

    static String displayNameOf(String option) {
        AbilityInfo info = infoFor(option);
        if (info != null) {
            var category = dev.ua.ikeepcalm.coi.client.ability.AbilityCategories.find(info.abilityId(), AbilityInfo.extractCategory(option));
            if (category != null) return info.localizedName() + " · " + category.name();
            if (AbilityInfo.ACTION_LEFT_CLICK.equals(AbilityInfo.extractAction(option)))
                return info.localizedName() + " · " + net.minecraft.client.resources.language.I18n.get("screen.coi.manual_secondary");
            return info.localizedName();
        }
        String name = AbilityInfo.extractDisplayName(option);
        return name == null ? option : name;
    }
}
