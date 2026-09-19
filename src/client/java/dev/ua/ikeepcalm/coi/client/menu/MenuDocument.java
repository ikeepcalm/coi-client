package dev.ua.ikeepcalm.coi.client.menu;

import java.util.List;

/**
 * A whole server-authored screen, as it arrived on {@code coi-client:menu}.
 * <p>
 * The client renders this and nothing else: it never branches on
 * {@link #screen()}, which exists only so a document can be recognised as "the
 * same screen refreshed" (scroll position and search text survive) rather than
 * a new one. Navigation, gating and side effects all stay on the server, which
 * answers every click with the next document.
 */
public record MenuDocument(String session, int version, String screen,
                           String title, String subtitle, int accentRgb, MenuIcon icon,
                           boolean back, boolean closable, Toast toast,
                           List<Section> sections, List<MenuComponent.Button> footer,
                           boolean closed, Presentation presentation) {

    /** Optional decoration only. Every fact and action is still present in sections/footer. */
    public record Presentation(String template, String subject, String caption) {
        public static final Presentation NONE = new Presentation("", "", "");
        public boolean specimen() { return template.equals("specimen") && !subject.isEmpty(); }
    }

    /**
     * The mod's own gold, for a document that names no accent.
     */
    public static final int DEFAULT_ACCENT = 0xFFD870;

    public record Section(String title, List<MenuComponent> components) {
    }

    /**
     * A single line of feedback carried by the document that answers a click —
     * "you are not a bishop any more". Styles are the {@link MenuComponent.TextStyle}
     * words {@code info|success|warn|error}.
     */
    public record Toast(String style, String text) {
    }

    /**
     * {@code {"session":…,"closed":true}} — the server taking its own screen
     * away, e.g. because the player lost the permission it was about.
     */
    public static MenuDocument closed(String session) {
        return new MenuDocument(session, 0, "", "", "", 0, MenuIcon.NONE,
                false, true, null, List.of(), List.of(), true, Presentation.NONE);
    }

    public int accentArgb() {
        return 0xFF000000 | (accentRgb != 0 ? accentRgb : DEFAULT_ACCENT);
    }
}
