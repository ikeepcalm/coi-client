package dev.ua.ikeepcalm.coi.client.screen.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/**
 * The card's scroll position and the bar that shows it — the handle is
 * grabbable, not decoration.
 * <p>
 * Geometry is handed over once per layout rather than recomputed per call, so
 * the mouse handlers and the paint can never disagree about where the track is.
 * One card has exactly one of these: a list flattens into the outer scroll
 * instead of growing a bar of its own.
 */
final class MenuScrollbar {

    /** Three pixels of paint, and a grab box deliberately wider — see {@link #onTrack}. */
    private static final int WIDTH = 3;
    private static final int MIN_HANDLE = 14;

    private int trackX;
    private int trackTop;
    private int trackH;
    private int viewH;
    private int contentH;

    private int scrollY;
    private boolean dragging;
    private int grab;

    /**
     * Takes the card's measurements for this layout and re-clamps the scroll
     * position into whatever content height came with them.
     */
    void place(int cardX, int cardW, int viewTop, int viewBottom, int contentHeight) {
        this.trackX = cardX + cardW - 5;
        this.trackTop = viewTop + 1;
        this.viewH = viewBottom - viewTop;
        this.trackH = viewH - 2;
        this.contentH = contentHeight;
        this.scrollY = Mth.clamp(scrollY, 0, maxScroll());
    }

    int scrollY() {
        return scrollY;
    }

    /** A new screen starts at the top; a refresh of the same one does not. */
    void toTop() {
        scrollY = 0;
    }

    int maxScroll() {
        return Math.max(0, contentH - viewH);
    }

    void scrollBy(int delta) {
        scrollY = Mth.clamp(scrollY + delta, 0, maxScroll());
    }

    void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, int accentArgb) {
        if (trackH <= 0) return;
        boolean grabbed = dragging || onHandle(mouseX, mouseY);
        MenuTheme.scrollbar(g, trackX, trackTop, trackH, handleY(), handleH(), WIDTH, accentArgb, grabbed);
    }

    /**
     * @return true when the press landed on the bar and the drag has begun
     */
    boolean press(double mx, double my) {
        if (!onTrack(mx, my)) return false;
        dragging = true;
        // Grabbing the handle keeps the point under the cursor; clicking the
        // bare track centres the handle there instead of paging
        grab = onHandle(mx, my) ? (int) (my - handleY()) : handleH() / 2;
        scrollTo(my - grab);
        return true;
    }

    boolean drag(double my) {
        if (!dragging) return false;
        scrollTo(my - grab);
        return true;
    }

    void release() {
        dragging = false;
    }

    private int handleH() {
        return Math.clamp((long) viewH * trackH / Math.max(1, contentH), Math.min(MIN_HANDLE, trackH), trackH);
    }

    private int handleY() {
        return trackTop + (trackH - handleH()) * scrollY / Math.max(1, maxScroll());
    }

    private boolean onTrack(double mx, double my) {
        // Three pixels of bar is a two-pixel target once the gui scale is 1, so
        // the grab box is deliberately wider than the paint
        return maxScroll() > 0 && mx >= trackX - 3 && mx < trackX + 6
                && my >= trackTop && my < trackTop + trackH;
    }

    private boolean onHandle(double mx, double my) {
        return onTrack(mx, my) && my >= handleY() && my < handleY() + handleH();
    }

    private void scrollTo(double handleTop) {
        int span = trackH - handleH();
        if (span <= 0) {
            scrollY = 0;
            return;
        }
        scrollY = Mth.clamp((int) Math.round((handleTop - trackTop) * maxScroll() / span), 0, maxScroll());
    }
}
