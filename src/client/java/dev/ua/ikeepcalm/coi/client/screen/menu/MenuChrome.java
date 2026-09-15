package dev.ua.ikeepcalm.coi.client.screen.menu;

import static dev.ua.ikeepcalm.coi.client.screen.menu.MenuMetrics.GAP;
import static dev.ua.ikeepcalm.coi.client.screen.menu.MenuMetrics.ICON;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.menu.MenuDocument;
import dev.ua.ikeepcalm.coi.client.menu.MenuIcon;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The card around the document: the watermark behind it, the header above it,
 * the notice under that and the footer below — everything {@link MenuScreen}
 * draws that is not the scrolling content.
 * <p>
 * It measures itself first and draws second, because the card's height is the
 * sum of those bands and the content between them. Whoever draws a control also
 * hit-tests it, so the back chevron, the close cross and the footer buttons
 * cannot drift from the boxes the mouse is checked against.
 */
final class MenuChrome {

    private static final int WATERMARK = 64;

    /**
     * How long a document's notice keeps its arrival highlight.
     */
    private static final long NOTICE_FLASH_MS = 900;

    /** The back chevron and the close cross, and the box each answers to. */
    private static final int CONTROL_W = 12;
    private static final int CONTROL_H = 11;

    /** What a press on the header landed on, if anything. */
    enum HeaderHit {NONE, BACK, CLOSE}

    private final MenuContext ctx;

    private MenuDocument doc;
    private int headerH;
    private int noticeH;
    private int footerH;
    private List<FormattedCharSequence> noticeLines = List.of();

    private int cardX, cardY, cardW, cardH;

    MenuChrome(MenuContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Takes the bands' heights for this document. Called once per layout, after
     * the card's width is known and before its height is, since the height is
     * what these add up to.
     */
    void measure(MenuDocument document) {
        this.doc = document;

        int titleBlock = doc.subtitle().isEmpty() ? 9 : 20;
        headerH = ctx.pad() + Math.max(ICON, titleBlock) + 6;

        noticeLines = doc.toast() == null ? List.of()
                : ctx.font().split(Component.literal(doc.toast().text()), ctx.contentW() - 12);
        noticeH = noticeLines.isEmpty() ? 0 : 6 + noticeLines.size() * 9 + 6;

        footerH = doc.footer().isEmpty() ? 0 : footerBlockH() + ctx.pad();
    }

    /** The card's rectangle, once the layout has settled on one. */
    void place(int x, int y, int w, int h) {
        this.cardX = x;
        this.cardY = y;
        this.cardW = w;
        this.cardH = h;
    }

    int headerH() {
        return headerH;
    }

    int noticeH() {
        return noticeH;
    }

    int footerH() {
        return footerH;
    }

    /**
     * The document's pathway emblem, huge and almost invisible behind the top
     * right of the card. Clipped to the card, so a big emblem on a short
     * document does not bleed onto the backdrop.
     */
    void drawWatermark(GuiGraphicsExtractor g) {
        if (doc.icon().kind() != MenuIcon.Kind.PATHWAY) return;
        g.enableScissor(cardX + 1, cardY + 1, cardX + cardW - 1, cardY + cardH - 1);
        MenuIcons.watermark(g, ctx.font(), doc.icon(), cardX + cardW - WATERMARK - 4, cardY + 4, WATERMARK);
        g.disableScissor();
    }

    void drawHeader(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int left = cardX + ctx.pad();
        int right = cardX + cardW - ctx.pad();
        int top = cardY + ctx.pad();

        // The accent band: the header is the one block that belongs to the
        // document's own colour rather than to the card
        g.fill(cardX + 1, cardY + 1, cardX + cardW - 1, cardY + headerH - 3,
                MenuTheme.withAlpha(ctx.accent(), 0.06f));

        if (doc.back()) {
            boolean hovered = MenuMetrics.inBox(mouseX, mouseY, left, top, CONTROL_W, CONTROL_H);
            MenuTheme.chevron(g, left + 1, top + 1, hovered ? ctx.accent() : CoiStyle.TEXT_MUTED);
            left += 15;
        }
        if (doc.closable()) {
            int closeX = right - 8;
            boolean hovered = MenuMetrics.inBox(mouseX, mouseY, closeX - 2, top, CONTROL_W, CONTROL_H);
            MenuTheme.cross(g, closeX, top + 1, hovered ? MenuTheme.DANGER : CoiStyle.TEXT_MUTED);
            right -= 14;
        }

        if (MenuIcons.draw(g, ctx.font(), doc.icon(), left, top, ICON, 1f)) {
            left += ICON + 6;
        }

        int textW = Math.max(20, right - left);
        int textY = doc.subtitle().isEmpty() ? top + (ICON - 8) / 2 : top + 1;
        g.text(ctx.font(), ctx.font().plainSubstrByWidth(doc.title(), textW), left, textY, ctx.accent(), true);
        if (!doc.subtitle().isEmpty()) {
            g.text(ctx.font(), ctx.font().plainSubstrByWidth(doc.subtitle(), textW), left, textY + 11,
                    CoiStyle.TEXT_MUTED, false);
        }
        MenuTheme.hairline(g, cardX + ctx.pad(), cardY + headerH - 3, ctx.contentW());
    }

    /**
     * The document's own one-line answer to the click that produced it. It
     * stays for the document's life rather than timing out, so the card never
     * reflows under the cursor; only the arrival highlight is transient.
     */
    void drawNotice(GuiGraphicsExtractor g, long adoptedAt) {
        if (noticeLines.isEmpty()) return;
        int color = MenuTheme.toastColor(doc.toast().style(), ctx.accent());
        int x = cardX + ctx.pad();
        int y = cardY + headerH;
        float flash = 0f;
        if (!HudConfig.getSettings().epilepsyMode) {
            long age = System.currentTimeMillis() - adoptedAt;
            if (age < NOTICE_FLASH_MS) flash = 1f - age / (float) NOTICE_FLASH_MS;
        }
        g.fill(x, y, x + ctx.contentW(), y + noticeH - 4, MenuTheme.withAlpha(color, 0.10f + 0.18f * flash));
        g.fill(x, y, x + 2, y + noticeH - 4, color);
        int lineY = y + 5;
        for (FormattedCharSequence line : noticeLines) {
            g.text(ctx.font(), line, x + 9, lineY, color, false);
            lineY += 9;
        }
    }

    void drawFooter(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (doc.footer().isEmpty()) return;
        int y = footerTop();
        MenuTheme.hairline(g, cardX + ctx.pad(), y - ctx.pad() / 2, ctx.contentW());
        ctx.drawButtons(g, doc.footer(), cardX + ctx.pad(), y, ctx.contentW(), footerCols(),
                ctx.rowHeightFor(doc.footer()), mouseX, mouseY);
    }

    HeaderHit hitHeader(double mx, double my) {
        int top = cardY + ctx.pad();
        if (doc.back() && MenuMetrics.inBox(mx, my, cardX + ctx.pad(), top, CONTROL_W, CONTROL_H)) {
            return HeaderHit.BACK;
        }
        if (doc.closable()
                && MenuMetrics.inBox(mx, my, cardX + cardW - ctx.pad() - 10, top, CONTROL_W, CONTROL_H)) {
            return HeaderHit.CLOSE;
        }
        return HeaderHit.NONE;
    }

    /**
     * @return true when the press landed on a footer button, which has then
     *         been activated
     */
    boolean clickFooter(double mx, double my) {
        if (doc.footer().isEmpty()) return false;
        int index = ctx.hitButton(mx, my, doc.footer().size(), cardX + ctx.pad(), footerTop(),
                ctx.contentW(), footerCols(), ctx.rowHeightFor(doc.footer()));
        if (index < 0) return false;
        ctx.activate(doc.footer().get(index), null);
        return true;
    }

    private int footerCols() {
        return Math.clamp(doc.footer().size(), 1, 3);
    }

    private int footerBlockH() {
        int cols = footerCols();
        int rows = (doc.footer().size() + cols - 1) / cols;
        return rows * (ctx.rowHeightFor(doc.footer()) + GAP) - GAP;
    }

    private int footerTop() {
        return cardY + cardH - ctx.pad() / 2 - footerBlockH();
    }
}
