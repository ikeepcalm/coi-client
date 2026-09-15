package dev.ua.ikeepcalm.coi.client.state;

import dev.ua.ikeepcalm.coi.client.menu.MenuDocument;
import dev.ua.ikeepcalm.coi.client.menu.MenuParser;

/**
 * The menu document currently on screen.
 * <p>
 * Pure state: the routing (open the screen, replace it, close it) lives in
 * {@code CoiNetworking} beside the other payload receivers, and the
 * screen re-reads {@link #document()} rather than being handed one, so a push
 * that lands while it is open shows up on the next frame.
 * <p>
 * {@link #revision()} is what tells the screen a genuinely new document arrived.
 * Comparing {@code version} would not do: the server mints a fresh session (and
 * restarts its versions) for every menu it opens.
 */
public final class MenuState {

    private static MenuDocument document;
    private static int revision;
    private static boolean fromSheet;

    private MenuState() {
    }

    public static void adopt(MenuDocument next) {
        if (next == null || next.closed()) return;
        document = next;
        revision++;
    }

    public static MenuDocument document() {
        return document;
    }

    public static boolean hasDocument() {
        return document != null;
    }

    public static int revision() {
        return revision;
    }

    /**
     * Remembers that this menu session was reached from the character sheet, so
     * the back arrow at the root of the server's screen has somewhere to go.
     * <p>
     * Only {@code CharacterSheetScreen.openTarget} sets it: the sheet's
     * destination cards are the one doorway that leaves a page behind. Pressing
     * the menu key straight from the world does not, and there the back arrow
     * keeps meaning "close".
     */
    public static void markFromSheet() {
        fromSheet = true;
    }

    /**
     * The other half of {@link #markFromSheet()}: this session was opened
     * straight from the world — a direct keybind or the menu key — so the back
     * arrow at the root keeps meaning "close". Said out loud rather than
     * trusted to already be false, because the flag outlives the document that
     * set it until the menu is closed.
     */
    public static void markDirect() {
        fromSheet = false;
    }

    public static boolean openedFromSheet() {
        return fromSheet;
    }

    public static String session() {
        return document != null ? document.session() : "";
    }

    public static int version() {
        return document != null ? document.version() : 0;
    }

    /**
     * Forgets the document without touching the screen — the caller decides
     * what happens to that.
     */
    public static void clear() {
        document = null;
        fromSheet = false;
    }

    public static void reset() {
        clear();
        revision = 0;
    }

    /**
     * A hand-written document exercising every component type, for the F8 debug
     * screen. It goes through {@link MenuParser} rather than building records
     * directly, so the offline check covers the parser too.
     */
    public static void debugInject() {
        MenuDocument sample = MenuParser.parse(MenuSample.DOCUMENT);
        if (sample != null) adopt(sample);
    }
}
