package dev.ua.ikeepcalm.coi.client.screen.menu;

import dev.ua.ikeepcalm.coi.client.form.MythicalFormManager;
import dev.ua.ikeepcalm.coi.client.menu.MenuDocument;
import dev.ua.ikeepcalm.coi.client.ui.ArchivePaint;
import dev.ua.ikeepcalm.coi.client.ui.EntityStudy;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

/** A reusable specimen leaf. Owns only the local camera, never gameplay actions. */
final class MenuSpecimen {
    private int x, y, w, h, clipTop, clipBottom;
    private float yaw = -20;
    private float zoom = 1;
    private boolean dragging;
    private boolean available;
    private int modelX, modelY, modelW, modelH;

    void reset() { yaw = -20; zoom = 1; dragging = false; }
    void clear() { available = false; dragging = false; }

    void draw(GuiGraphicsExtractor g, Font font, MenuDocument doc,
              int x, int y, int w, int h, int clipTop, int clipBottom) {
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.clipTop = clipTop; this.clipBottom = clipBottom;
        available = MythicalFormManager.getRegisteredForm(doc.presentation().subject()) != null;
        ArchivePaint.folio(g, x, y, w, h);
        if (h < 180) {
            drawCompact(g, font, doc);
            return;
        }
        ArchivePaint.registration(g, x + 14, y + 28, w - 28, h - 58, 0xFF626658);
        int radius = Math.max(8, Math.min(w - 32, h - 65) / 2);
        ArchivePaint.seal(g, x + w / 2, y + h / 2, radius, 0x405C7060);
        g.text(font, font.plainSubstrByWidth(doc.presentation().caption(), w - 28),
                x + 14, y + 12, ArchivePaint.LABEL, false);
        if (available) {
            modelX = x + 12; modelY = y + 28; modelW = w - 24; modelH = h - 56;
            drawModel(g, doc);
        } else {
            MenuIcons.draw(g, font, doc.icon(), x + w / 2 - 24, y + h / 2 - 24, 48, 0.8f);
        }
        String hint = I18n.get(available ? "screen.coi.specimen_controls" : "screen.coi.specimen_unavailable");
        g.text(font, font.plainSubstrByWidth(hint, w - 28), x + 14, y + h - 18, 0xFFABB1A4, false);
    }

    private void drawCompact(GuiGraphicsExtractor g, Font font, MenuDocument doc) {
        modelX = x + 8; modelY = y + 6; modelW = Math.min(112, w / 3); modelH = h - 12;
        ArchivePaint.seal(g, modelX + modelW / 2, y + h / 2, Math.max(8, modelH / 2 - 6), 0x405C7060);
        if (available) drawModel(g, doc);
        else MenuIcons.draw(g, font, doc.icon(), modelX + modelW / 2 - 16, y + h / 2 - 16, 32, 0.8f);
        int tx = modelX + modelW + 10;
        int tw = Math.max(1, x + w - tx - 10);
        g.text(font, font.plainSubstrByWidth(doc.presentation().caption(), tw), tx, y + 18, ArchivePaint.LABEL, false);
        String hint = I18n.get(available ? "screen.coi.specimen_controls" : "screen.coi.specimen_unavailable");
        int lineY = y + 36;
        for (var line : font.split(net.minecraft.network.chat.Component.literal(hint), tw)) {
            g.text(font, line, tx, lineY, 0xFFABB1A4, false);
            lineY += 11;
        }
    }

    private void drawModel(GuiGraphicsExtractor g, MenuDocument doc) {
        EntityStudy.draw(g, modelX, modelY, modelW, modelH, doc.presentation().subject(), yaw, zoom);
    }

    private boolean contains(double mx, double my) {
        return available && my >= clipTop && my < clipBottom
                && MenuMetrics.inBox(mx, my, modelX, modelY, modelW, modelH);
    }

    boolean press(double mx, double my) {
        dragging = contains(mx, my);
        return dragging;
    }

    boolean drag(double dx) {
        if (!dragging) return false;
        yaw = (yaw + (float) dx * 0.8f) % 360;
        return true;
    }

    void release() { dragging = false; }

    boolean zoom(double mx, double my, double amount) {
        if (!contains(mx, my)) return false;
        zoom = Math.clamp(zoom + (float) amount * 0.08f, 0.65f, 1.6f);
        return true;
    }
}
