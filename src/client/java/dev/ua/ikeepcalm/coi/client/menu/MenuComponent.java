package dev.ua.ikeepcalm.coi.client.menu;

import java.util.List;

/**
 * One item inside a menu section — the vocabulary the server composes screens
 * from.
 * <p>
 * Sealed, so the renderer's switch is exhaustive and a new type cannot be added
 * without deciding how it draws. The <em>wire</em> is deliberately not sealed:
 * {@link MenuParser} skips a {@code type} it has never heard of, because a newer
 * plugin talking to an older client has to degrade to a screen missing one row
 * rather than no screen at all.
 * <p>
 * Every string here arrives already localized. Colours are 0xRRGGBB with
 * {@code 0} meaning "the server did not name one", which is why they are plain
 * ints rather than boxed.
 * <p>
 * Vocabulary v2 added {@link Hero}, {@link Details}, {@link Steps},
 * {@link Chips} and {@link Panels}, plus an {@link MenuIcon} on every row-ish
 * type. It is purely additive: an absent new field means exactly the behaviour
 * v1 had.
 */
public sealed interface MenuComponent {

    enum TextStyle {BODY, MUTED, HEADING, WARN, DANGER, SUCCESS}

    enum ButtonStyle {PRIMARY, SECONDARY, DANGER, SUCCESS, GHOST}

    enum Align {LEFT, CENTER, RIGHT}

    /**
     * How a {@link Stat} spends its fraction: the default bar, a 24px arc for a
     * headline number, or the bar cut into ten notches.
     */
    enum GaugeStyle {BAR, RING, SEGMENTS}

    /**
     * A {@link Hero}'s gauge: a bar under the text, or a 28px arc beside it.
     */
    enum HeroStyle {PLAIN, RING}

    /**
     * A {@link Steps} rail's markers: numbered discs or timeline dots.
     */
    enum StepStyle {NUMBERED, TIMELINE}

    /**
     * A {@link Grid}'s tile edge — 18 / 28 / 40 px. {@code MEDIUM} is what v1
     * always drew.
     */
    enum TileSize {SMALL, MEDIUM, LARGE}

    /**
     * A {@link Check}'s three answers. {@code PENDING} is "not yet", which the
     * old {@code ok} boolean could only spell as "failed".
     */
    enum CheckState {OK, NO, PENDING}

    /**
     * A click the client questions before sending. This is what replaces the
     * plugin's two-step chest confirms: the modal is drawn locally and nothing
     * leaves the client until the player agrees.
     */
    record Confirm(String title, String body, String confirmLabel) {
    }

    /**
     * A small fact as a pill — the replacement for a two-row {@link Kv} that is
     * really just facts. Used standalone by {@link Chips} and along the bottom
     * of a {@link Hero}.
     */
    record Chip(String label, String value, int rgb, MenuIcon icon, String hint) {
    }

    record Text(String text, TextStyle style, Align align) implements MenuComponent {
    }

    /**
     * A callout — the same prose as {@link Text}, boxed and edged in its
     * style's colour so a warning cannot be skimmed past.
     */
    record Note(TextStyle style, String title, String text, MenuIcon icon) implements MenuComponent {
    }

    /**
     * Label, right-aligned value and an optional gauge beneath. {@code hasBar}
     * is separate from {@code fraction} because a genuine 0% is not the same
     * thing as a stat that has no bar at all — and {@code hasCap} is separate
     * from {@code cap} for exactly the same reason.
     * <p>
     * {@code cap} is a ceiling the fill cannot reach, drawn dark above the
     * gauge; it is the character plate's permanent-madness language. {@code
     * delta} is a trend token such as {@code "+12"}, coloured by its sign.
     */
    record Stat(String label, String value, double fraction, boolean hasBar, int rgb, String hint,
                MenuIcon icon, GaugeStyle style, double cap, boolean hasCap,
                String delta) implements MenuComponent {
    }

    record KvRow(String label, String value, int rgb, String hint, MenuIcon icon) {
    }

    record Kv(List<KvRow> rows) implements MenuComponent {
    }

    /**
     * {@code state} is already resolved: the parser folds the wire's older
     * {@code ok} boolean into it, so the renderer never has to ask which of the
     * two the server meant.
     */
    record Check(CheckState state, String label, String detail, MenuIcon icon, int rgb) {
    }

    record Checklist(List<Check> items) implements MenuComponent {
    }

    /**
     * {@code id} is an opaque token the server minted for this document
     * version; the client never invents one. A disabled button keeps its
     * {@code disabledReason} and shows it as a tooltip — a dead control that
     * will not say why is the thing the chest GUIs did worst.
     */
    record Button(String id, String label, String desc, ButtonStyle style, boolean enabled,
                  String disabledReason, Confirm confirm, MenuIcon icon) implements MenuComponent {
    }

    record Buttons(List<Button> buttons, int columns) implements MenuComponent {
    }

    record Toggle(String id, String label, boolean on, boolean enabled, String desc,
                  String onText, String offText, String disabledReason,
                  MenuIcon icon) implements MenuComponent {
    }

    /**
     * One entry of a {@link ListView} or a {@link Grid}. {@code action} is what
     * a click sends; a row without one is inert but still drawn, which is how
     * the server shows a read-only ledger. {@code meta} is a second,
     * right-aligned line under the badge, and {@code fraction} a hair-thin
     * gauge under the subtitle. A disabled row keeps the same contract a button
     * does: it is drawn, and it says why.
     */
    record Row(String id, String title, String subtitle, MenuIcon icon, String badge, int badgeRgb,
               List<String> tooltip, String action, boolean enabled, int rgb,
               double fraction, boolean hasFraction, String meta, String disabledReason) {
    }

    record ListView(String id, List<Row> rows, boolean searchable, int maxVisible,
                    String empty) implements MenuComponent {
    }

    record Grid(List<Row> cells, int columns, TileSize size) implements MenuComponent {
    }

    record Input(String id, String label, String placeholder, String value, int maxLength,
                 String submit, String submitLabel, String hint) implements MenuComponent {
    }

    /**
     * A bare hairline, or — with a {@code label} — a titled separator.
     */
    record Divider(String label) implements MenuComponent {
    }

    record Spacer(int size) implements MenuComponent {
    }

    // --- Vocabulary v2 ---

    /**
     * The screen's identity block: one per screen, first component of the first
     * section. This is what gives each menu a face instead of a title bar and a
     * wall of grey paragraphs.
     */
    record Hero(MenuIcon icon, String title, String subtitle, String badge, int badgeRgb,
                HeroStyle style, double fraction, boolean hasFraction, String fractionLabel,
                int rgb, List<Chip> chips) implements MenuComponent {
    }

    /**
     * A disclosure row, collapsed by default — the wall-of-text killer. The
     * open/closed state is the client's once the player touches it, keyed on
     * {@code id}; {@code open} is only the server's opening suggestion.
     */
    record Details(String id, String summary, List<String> text, TextStyle style, MenuIcon icon,
                   boolean open) implements MenuComponent {
    }

    /**
     * {@code done} is deliberately boxed: {@code null} is "not yet", which is
     * neither done nor failed and draws as a muted hollow marker.
     */
    record Step(String title, String text, Boolean done, MenuIcon icon) {
    }

    record Steps(StepStyle style, List<Step> items) implements MenuComponent {
    }

    record Chips(List<Chip> items) implements MenuComponent {
    }

    /**
     * One mini-card of a {@link Panels} row. Everything a {@link Row} is, plus
     * a headline {@code value} — and the same disabled contract as a button, so
     * a dead cell still says why.
     */
    record PanelCell(String id, MenuIcon icon, String title, String value, String subtitle,
                     int rgb, double fraction, boolean hasFraction, String badge, int badgeRgb,
                     List<String> tooltip, String action, boolean enabled, String disabledReason) {
    }

    /**
     * Side-by-side mini-cards — the one thing the renderer had no answer for at
     * all, since every other component is a full-width band.
     */
    record Panels(int columns, List<PanelCell> cells) implements MenuComponent {
    }

    /**
     * A section heading carrying more than a caption.
     * <p>
     * The wire puts {@code icon} / {@code badge} / {@code collapsed} on the
     * <em>section</em>, but {@code MenuDocument.Section} has only a title, so
     * {@link MenuParser} lifts a decorated heading into the section's component
     * list as this and leaves the section's own title empty. A section with
     * nothing but a title still travels the v1 way, untouched.
     * <p>
     * {@code key} is what the client's disclosure map is keyed on — the
     * section's {@code id}, or {@code "#section<n>"} when it named none.
     */
    record Heading(String key, String title, MenuIcon icon, String badge, int badgeRgb,
                   boolean collapsible, boolean collapsed) implements MenuComponent {
    }
}
