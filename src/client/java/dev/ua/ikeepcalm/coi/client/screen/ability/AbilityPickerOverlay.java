package dev.ua.ikeepcalm.coi.client.screen.ability;

import dev.ua.ikeepcalm.coi.client.ability.AbilityCategories;
import dev.ua.ikeepcalm.coi.client.ability.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.ability.AbilityRegistry;
import dev.ua.ikeepcalm.coi.client.menu.MenuComponent;
import dev.ua.ikeepcalm.coi.client.menu.MenuDocument;
import dev.ua.ikeepcalm.coi.client.screen.menu.MenuTheme;
import dev.ua.ikeepcalm.coi.client.ui.AbilityIcons;
import dev.ua.ikeepcalm.coi.client.ui.ArchivePaint;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Field manual shared by the server catalogue and all three local binding pickers. */
public class AbilityPickerOverlay {
    private record Hit(int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) { return mx >= x && mx < x+w && my >= y && my < y+h; }
    }
    private final PickerModel model = new PickerModel();
    private final List<Hit> hits = new ArrayList<>();
    private boolean open, browsing, detailsOnly, assigning, serverDetails;
    private Component contextTitle = Component.empty();
    private Consumer<String> onSelect;
    private EditBox search;
    private String selected, feedback = "";
    private int x, y, w, h, top, bottom, indexW, detailX, detailW, listScroll, detailScroll, detailHeight;
    private boolean wide, paintingDetails;
    private long feedbackUntil;
    private int feedbackColor = MenuTheme.SUCCESS;
    private int keyboardHit = -1;
    private Runnable onExit;
    private MenuDocument serverDocument;
    private Consumer<String> serverAction;

    public void setServerControls(MenuDocument document, Consumer<String> action) {
        serverDocument = document;
        serverAction = action;
        if (document.toast() != null && !document.toast().text().isBlank()) {
            feedback = document.toast().text();
            feedbackColor = switch (document.toast().style()) {
                case "error" -> MenuTheme.DANGER;
                case "warn" -> MenuTheme.WARN;
                default -> MenuTheme.INFO;
            };
            feedbackUntil = System.currentTimeMillis() + 5000;
        }
    }

    private MenuComponent.Row abilityControl(String id) {
        if (serverDocument == null) return null;
        for (var section : serverDocument.sections()) {
            for (var component : section.components()) {
                if (component instanceof MenuComponent.ListView list) {
                    for (var row : list.rows()) if (id.equals(row.id())) return row;
                } else if (component instanceof MenuComponent.Grid grid) {
                    for (var row : grid.cells()) if (id.equals(row.id())) return row;
                }
            }
        }
        return null;
    }

    public void open(Component title, String current, Consumer<String> onSelect) {
        this.open = true;
        this.browsing = false;
        this.contextTitle = title;
        this.onSelect = onSelect;
        this.selected = current;
        listScroll = detailScroll = 0;
        assigning = detailsOnly = false;
        feedbackUntil = 0;
        keyboardHit = -1;
        Font font = Minecraft.getInstance().font;
        search = new EditBox(font, 0, 0, 100, 18, Component.translatable("screen.coi.picker_search_hint"));
        search.setMaxLength(60);
        search.setHint(Component.translatable("screen.coi.picker_search_hint"));
        search.setResponder(value -> {
            model.refilter(value);
            listScroll = 0;
            select(model.filtered().isEmpty() ? null : model.filtered().getFirst());
        });
        search.setFocused(true);
        model.refilter("");
        if (selected == null && !model.filtered().isEmpty()) selected = model.filtered().getFirst();
    }

    public void openManual(Component title) {
        open(title, null, null);
        browsing = true;
    }
    public void setOnExit(Runnable action) { onExit = action; }
    public void close() { open = false; hits.clear(); }
    public boolean isOpen() { return open; }

    private void geometry(int screenW, int screenH) {
        w = Math.max(160, Math.min(browsing ? 920 : 620, screenW - 24));
        h = Math.max(120, screenH - 28);
        x = (screenW - w) / 2; y = (screenH - h) / 2;
        top = y + (browsing ? 63 : 77); bottom = y + h - 34;
        wide = browsing && w >= 570;
        indexW = wide ? Math.min(290, w * 2 / 5) : w - 24;
        detailX = wide ? x + indexW + 24 : x + 12;
        detailW = wide ? w - indexW - 36 : w - 24;
        search.setX(x + 12); search.setY(y + 37); search.setWidth(indexW);
        listScroll = Mth.clamp(listScroll, 0, model.maxScroll(bottom - top));
        detailScroll = Mth.clamp(detailScroll, 0, Math.max(0, detailHeight - (bottom - top)));
    }

    public void render(GuiGraphicsExtractor g, Font font, int sw, int sh, int mx, int my, float delta) {
        if (!open) return;
        model.refilter(search.getValue()); // Catalogue pushes can arrive while the manual is open.
        if (selected != null && AbilityRegistry.getAbilityInfo(AbilityInfo.extractId(selected)) == null
                && !AbilityRegistry.getAvailableAbilities().contains(selected)) selected = null;
        if (selected == null && !model.filtered().isEmpty()) select(model.filtered().getFirst());
        geometry(sw, sh);
        hits.clear();
        g.fill(0, 0, sw, sh, CoiStyle.BACKDROP);
        ArchivePaint.folio(g, x, y, w, h);
        g.text(font, I18n.get(browsing ? "screen.coi.manual_title" : "screen.coi.picker_choose"), x + 12, y + 11, ArchivePaint.LABEL, false);
        String context = font.plainSubstrByWidth(contextTitle.getString(), Math.max(20, w - 210));
        g.text(font, context, x + w - 12 - font.width(context), y + 11, CoiStyle.TEXT_MUTED, false);
        if (!browsing) g.text(font, I18n.get("screen.coi.picker_pick_hint"), x+12, y+62, CoiStyle.TEXT_MUTED, false);
        if (!browsing || wide || !detailsOnly) {
            search.extractRenderState(g, mx, my, delta);
            drawIndex(g, font, mx, my);
        } else {
            button(g, font, x + 12, y + 36, 100, 18, I18n.get("screen.coi.manual_index"),
                    () -> { detailsOnly = false; assigning = false; }, mx, my);
        }
        if (browsing && (wide || detailsOnly)) drawDetails(g, font, mx, my);
        if (wide) g.fill(detailX - 7, top, detailX - 6, bottom, 0xFF494C43);
        int footY = y + h - 25;
        button(g, font, x + 12, footY, Math.min(150, (w - 30) / 2), 18,
                I18n.get(browsing ? "screen.coi.manual_controls" : "gui.cancel"), this::close, mx, my);
        if (!browsing) button(g, font, x + w - 112, footY, 100, 18,
                I18n.get("screen.coi.manual_unbind"), () -> pick(null), mx, my);
        else if (onExit != null && System.currentTimeMillis() >= feedbackUntil) button(g, font, x+w-112, footY, 100, 18,
                I18n.get("gui.done"), onExit, mx, my);
        if (System.currentTimeMillis() < feedbackUntil)
            g.text(font, font.plainSubstrByWidth(feedback, Math.max(20,w-174)), x + 174, footY + 5, feedbackColor, false);
        if (keyboardHit >= 0 && keyboardHit < hits.size()) {
            Hit hit = hits.get(keyboardHit);
            g.outline(hit.x(), hit.y(), hit.w(), hit.h(), ArchivePaint.LABEL);
        }
    }

    private void drawIndex(GuiGraphicsExtractor g, Font font, int mx, int my) {
        int ry = top;
        List<PickerModel.Row> rows = model.rows();
        g.enableScissor(x + 12, top, x + 12 + indexW, bottom);
        for (int i = listScroll; i < rows.size() && ry < bottom; i++) {
            var row = rows.get(i);
            int rh = PickerModel.rowHeight(row);
            if (row.kind() == PickerModel.RowKind.ABILITY) {
                String option = row.option();
                String id = AbilityInfo.extractId(option);
                boolean chosen = sameBinding(option, selected);
                boolean hover = mx >= x+12 && mx < x+12+indexW && my >= ry && my < Math.min(bottom, ry+rh);
                int accent = AbilityInfo.pathwayColor(id);
                g.fill(x + 12, ry, x + 12 + indexW, ry + rh - 2, chosen ? 0xFF363B35 : hover ? 0xFF2C302C : 0xFF202521);
                if (chosen) g.fill(x+12, ry, x+14, ry+rh-2, accent);
                AbilityIcons.draw(g, option, x + 18, ry + 5, 18, 255);
                AbilityInfo info = PickerModel.infoFor(option);
                g.text(font, font.plainSubstrByWidth(PickerModel.entryLabel(option), indexW - 36),
                        x+42, ry+4, info != null && info.isUnavailable() ? 0xFFB48B85 : ArchivePaint.LABEL, false);
                g.text(font, font.plainSubstrByWidth(entryMeta(option, info), indexW - 36),
                        x+42, ry+16, CoiStyle.TEXT_MUTED, false);
                int hitY = Math.max(top, ry), hitH = Math.min(bottom, ry+rh)-hitY;
                hits.add(new Hit(x+12, hitY, indexW, hitH, () -> { if (!browsing) pick(option); else { select(option); detailsOnly = true; search.setFocused(false); } }));
                if (hover && !browsing) {
                    String description = info == null ? "" : info.description();
                    var lines = new ArrayList<>(font.split(Component.literal(PickerModel.displayNameOf(option)
                            + (description.isBlank() ? "" : "\n" + description)), Math.min(250, w-32)));
                    int limit = Math.max(3, Math.min(12, (h-32)/11));
                    if (lines.size() > limit) {
                        lines.subList(limit-1, lines.size()).clear();
                        lines.add(Component.literal("…").getVisualOrderText());
                    }
                    g.setTooltipForNextFrame(font, lines, mx, my);
                }
            } else if (row.kind() == PickerModel.RowKind.SPELL) {
                g.fill(x+12, ry+3, x+12+indexW, ry+rh-2, 0xFF363B35);
                g.text(font, font.plainSubstrByWidth(PickerModel.baseName(row.option()), indexW-12),
                        x+18, ry+9, ArchivePaint.LABEL, false);
            } else if (row.kind() == PickerModel.RowKind.HEADER) {
                PickerPainter.headerRow(g, font, x+11, indexW+2, row, ry);
            } else if (row.kind() == PickerModel.RowKind.MESSAGE) {
                g.text(font, I18n.get("screen.coi.manual_no_results"), x+18, ry+4, CoiStyle.TEXT_MUTED, false);
            } else { // The global Unbind command lives in the footer.
                rh = 0;
            }
            ry += rh;
        }
        g.disableScissor();
        if (model.contentHeight() > bottom-top)
            PickerPainter.scrollbar(g, x+11, indexW+2, top, bottom, bottom-top, model.heightAbove(listScroll), model.contentHeight());
    }

    private void select(String option) {
        selected = option;
        serverDetails = false;
        detailScroll = 0;
        assigning = false;
        keyboardHit = -1;
    }

    private void drawDetails(GuiGraphicsExtractor g, Font font, int mx, int my) {
        if (selected == null) {
            g.text(font, I18n.get("screen.coi.manual_waiting"), detailX+8, top+8, CoiStyle.TEXT_MUTED, false);
            return;
        }
        String id = AbilityInfo.extractId(selected);
        AbilityInfo info = AbilityRegistry.getAbilityInfo(id);
        int accent = AbilityInfo.pathwayColor(id);
        g.enableScissor(detailX, top, detailX+detailW, bottom);
        paintingDetails = true;
        int ry = top - detailScroll;
        int titleHeight = font.split(Component.literal(PickerModel.displayNameOf(selected)), Math.max(1, detailW-53)).size() * 11;
        int identityHeight = Math.max(52, titleHeight + 34);
        ArchivePaint.paper(g, detailX, ry, detailW, identityHeight);
        AbilityIcons.draw(g, selected, detailX+6, ry+6, 32, 255);
        int textX = detailX+47;
        ry = paragraph(g, font, PickerModel.displayNameOf(selected), textX, ry+6, detailW-53, ArchivePaint.INK);
        if (info != null) {
            ry = paragraph(g, font, I18n.get("screen.coi.manual_sequence", info.sequence()) + " · "
                    + PickerLabels.category(info).getString(), textX, ry+4, detailW-53, ArchivePaint.FAINT_INK);
        }
        ry = Math.max(top-detailScroll+identityHeight+8, ry+12);
        g.fill(detailX+6, ry, detailX+detailW-6, ry+1, accent);
        ry += 12;
        if (info != null) {
            if (PickerModel.detectMeta()) {
                var fixedCategory = AbilityCategories.find(id, AbilityInfo.extractCategory(selected));
                String stats = fixedCategory == null
                        ? PickerLabels.cost(info).getString() + " · " + PickerLabels.cooldown(info).getString()
                        : I18n.get("screen.coi.picker_meta_cooldown", fixedCategory.cooldownSeconds());
                ry = paragraph(g, font, stats, detailX+6, ry, detailW-12, ArchivePaint.LABEL) + 8;
                if (fixedCategory == null && !AbilityCategories.get(id).isEmpty()) ry = paragraph(g, font,
                        I18n.get("screen.coi.manual_current_cost"), detailX+6, ry, detailW-12, CoiStyle.TEXT_MUTED) + 8;
            }
            var reason = PickerLabels.blockReason(info);
            if (reason != null) ry = paragraph(g, font, reason.getString(), detailX+6, ry, detailW-12, 0xFFE39B90) + 8;
            ry = paragraph(g, font, info.description().isBlank() ? I18n.get("screen.coi.manual_no_description") : info.description(),
                    detailX+6, ry, detailW-12, CoiStyle.TEXT_BODY) + 14;
        }
        String option = chosenOption();
        if (assigning) {
            ry = paragraph(g, font, I18n.get("screen.coi.manual_assign_to"),
                    detailX+6, ry, detailW-12, ArchivePaint.LABEL) + 6;
            ry = drawTargets(g, font, ry, mx, my);
        } else if (option != null) {
            if (info != null && AbilityInfo.KIND_PASSIVE.equals(info.kind())) {
                var control = abilityControl(id);
                String status = control != null && !control.subtitle().isBlank() ? control.subtitle()
                        : I18n.get(info.active() ? "screen.coi.manual_passive_enabled" : "screen.coi.manual_passive_disabled");
                ry = paragraph(g, font, status, detailX+6, ry, detailW-12, ArchivePaint.LABEL) + 6;
                if (control != null && control.enabled() && control.action() != null && !control.action().isBlank()
                        && serverAction != null) {
                    button(g, font, detailX+6, ry, detailW-12, 22,
                            I18n.get(info.active() ? "screen.coi.manual_passive_disable" : "screen.coi.manual_passive_enable"),
                            () -> serverAction.accept(control.action()), mx, my);
                    ry += 30;
                } else if (control != null && !control.disabledReason().isBlank()) {
                    ry = paragraph(g, font, control.disabledReason(), detailX+6, ry, detailW-12, CoiStyle.TEXT_MUTED) + 8;
                }
            }
            for (var target : ManualBindings.targets()) {
                if (sameBinding(option, target.existing()))
                    ry = paragraph(g, font, target.label(), detailX+6, ry, detailW-12,
                            target.conflict() ? 0xFFE39B90 : CoiStyle.TEXT_MUTED) + 3;
            }
            button(g, font, detailX+6, ry+5, detailW-12, 22, I18n.get("screen.coi.manual_bind"),
                    () -> { assigning = true; detailScroll = 0; keyboardHit = -1; }, mx, my);
            ry += 33;
            var control = abilityControl(id);
            if (control != null && info != null && !AbilityInfo.KIND_PASSIVE.equals(info.kind())) {
                if (control.enabled() && !control.action().isBlank() && serverAction != null) {
                    button(g, font, detailX + 6, ry, detailW - 12, 22, I18n.get("screen.coi.manual_take_item"),
                            () -> serverAction.accept(control.action()), mx, my);
                    ry += 30;
                } else if (!control.disabledReason().isBlank()) {
                    ry = paragraph(g, font, control.disabledReason(), detailX + 6, ry, detailW - 12, CoiStyle.TEXT_MUTED) + 8;
                }
            }
            if (control != null && !control.tooltip().isEmpty()) {
                button(g, font, detailX + 6, ry, detailW - 12, 22,
                        I18n.get(serverDetails ? "screen.coi.menu_collapse" : "screen.coi.manual_server_details"),
                        () -> serverDetails = !serverDetails, mx, my);
                ry += 30;
                if (serverDetails) for (String line : control.tooltip())
                    ry = paragraph(g, font, line, detailX + 6, ry, detailW - 12, CoiStyle.TEXT_BODY) + 3;
            }
        }
        detailHeight = ry - (top-detailScroll) + 6;
        paintingDetails = false;
        g.disableScissor();
        if (detailHeight > bottom-top)
            PickerPainter.scrollbar(g, detailX, detailW, top, bottom, bottom-top, detailScroll, detailHeight);
    }

    private int drawTargets(GuiGraphicsExtractor g, Font font, int ry, int mx, int my) {
        for (var target : ManualBindings.targets()) {
            String existing = target.existing() == null ? I18n.get("screen.coi.manual_empty") : PickerModel.displayNameOf(target.existing());
            String label = target.label() + " — " + existing;
            button(g, font, detailX+6, ry, detailW-12, 25, label, () -> {
                String option = chosenOption();
                if (option == null) return;
                target.assign().accept(option);
                feedback = I18n.get("screen.coi.manual_assigned", target.label());
                feedbackColor = MenuTheme.SUCCESS;
                feedbackUntil = System.currentTimeMillis()+4000;
                assigning = false; detailScroll = 0;
            }, mx, my);
            ry += 29;
            if (target.conflict()) ry = paragraph(g, font, I18n.get("screen.coi.manual_key_conflict"),
                    detailX+6, ry, detailW-12, 0xFFE39B90) + 5;
        }
        return ry;
    }

    private String chosenOption() {
        if (selected == null) return null;
        return AbilityRegistry.getAvailableAbilities().stream()
                .filter(option -> sameBinding(option, selected)).findFirst().orElse(null);
    }

    private String entryMeta(String option, AbilityInfo info) {
        var category = AbilityCategories.find(AbilityInfo.extractId(option), AbilityInfo.extractCategory(option));
        if (category != null) return I18n.get("screen.coi.picker_meta_cooldown", category.cooldownSeconds());
        if (PickerModel.hasModes(option) && AbilityInfo.extractCategory(option).isEmpty()
                && AbilityInfo.ACTION_EXECUTE.equals(AbilityInfo.extractAction(option))) {
            var current = AbilityCategories.find(AbilityInfo.extractId(option), AbilityCategories.selected(AbilityInfo.extractId(option)));
            if (current != null) return current.name();
        }
        return PickerLabels.kindTag(info).getString();
    }
    private static boolean sameBinding(String a, String b) {
        return a != null && b != null && Objects.equals(AbilityInfo.extractId(a), AbilityInfo.extractId(b))
                && AbilityInfo.extractCategory(a).equals(AbilityInfo.extractCategory(b))
                && AbilityInfo.extractAction(a).equals(AbilityInfo.extractAction(b));
    }

    private int paragraph(GuiGraphicsExtractor g, Font font, String text, int px, int py, int width, int color) {
        for (var line : font.split(Component.literal(text), Math.max(1,width))) {
            g.text(font, line, px, py, color, false); py += 11;
        }
        return py;
    }

    private void button(GuiGraphicsExtractor g, Font font, int bx, int by, int bw, int bh, String label,
                        Runnable action, int mx, int my) {
        boolean inDetail = paintingDetails;
        boolean hover = mx >= bx && mx < bx+bw && my >= by && my < by+bh
                && (!inDetail || my >= top && my < bottom);
        g.fill(bx, by, bx+bw, by+bh, hover ? 0xFF41473E : 0xFF2D332D);
        g.fill(bx, by+bh-1, bx+bw, by+bh, 0xFF777967);
        g.text(font, font.plainSubstrByWidth(label, Math.max(1,bw-12)), bx+6, by+(bh-8)/2, ArchivePaint.LABEL, false);
        if (hover && font.width(label) > bw-12) g.setTooltipForNextFrame(font, Component.literal(label), mx, my);
        int hitTop = inDetail ? Math.max(top,by) : by;
        int hitBottom = inDetail ? Math.min(bottom,by+bh) : by+bh;
        if (hitBottom > hitTop) hits.add(new Hit(bx, hitTop, bw, hitBottom-hitTop, action));
    }

    public boolean mouseClicked(MouseButtonEvent event) {
        if (!open) return false;
        if (event.button() != 0) return true;
        if ((wide || !detailsOnly) && search.isMouseOver(event.x(),event.y())) {
            search.setFocused(true); search.mouseClicked(event,false); return true;
        }
        search.setFocused(false);
        keyboardHit = -1;
        for (Hit hit : List.copyOf(hits)) if (hit.contains(event.x(),event.y())) { hit.action().run(); break; }
        return true;
    }
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (!open) return false;
        if (browsing && ((wide && mx >= detailX) || (!wide && detailsOnly)))
            detailScroll = Mth.clamp(detailScroll - (int)(vertical*22), 0, Math.max(0,detailHeight-(bottom-top)));
        else listScroll = Mth.clamp(listScroll + (vertical>0?-1:1), 0, model.maxScroll(bottom-top));
        return true;
    }
    public boolean keyPressed(KeyEvent event) {
        if (!open) return false;
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (assigning) assigning = false;
            else if (!wide && detailsOnly) detailsOnly = false;
            else if (browsing && onExit != null) onExit.run();
            else close();
        } else if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
            var list = model.filtered();
            if (!list.isEmpty()) {
                int index = -1;
                for (int i=0;i<list.size();i++) if (sameBinding(list.get(i), selected)) index=i;
                select(list.get(Math.floorMod(index+(key==GLFW.GLFW_KEY_DOWN?1:-1),list.size())));
                for (int i=0;i<model.rows().size();i++) if (Objects.equals(model.rows().get(i).option(), selected)) { listScroll=i; break; }
            }
        } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (keyboardHit >= 0 && keyboardHit < hits.size()) { hits.get(keyboardHit).action().run(); keyboardHit = -1; }
            else if (!browsing && chosenOption()!=null) pick(chosenOption());
            else if (browsing && !wide && !detailsOnly) { detailsOnly=true; search.setFocused(false); }
            else { assigning=!assigning; detailScroll=0; }
        } else if (key == GLFW.GLFW_KEY_PAGE_DOWN || key == GLFW.GLFW_KEY_PAGE_UP) {
            detailScroll = Mth.clamp(detailScroll+(key==GLFW.GLFW_KEY_PAGE_DOWN?1:-1)*(bottom-top),0,Math.max(0,detailHeight-(bottom-top)));
        } else if (key == GLFW.GLFW_KEY_TAB) {
            search.setFocused(false);
            keyboardHit++;
            if (keyboardHit >= hits.size()) { keyboardHit = -1; search.setFocused(true); }
        }
        else search.keyPressed(event);
        return true;
    }
    public boolean charTyped(CharacterEvent event) { if (open && search.isFocused()) search.charTyped(event); return open; }
    private void pick(String option) { if (onSelect != null) onSelect.accept(option); close(); }
}
