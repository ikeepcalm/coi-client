package dev.ua.ikeepcalm.coi.client.screen.settings;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.screen.ScrollbarPainter;
import dev.ua.ikeepcalm.coi.client.screen.settings.SettingsRows.ContentWidget;
import dev.ua.ikeepcalm.coi.client.screen.widget.CoiTabButton;
import dev.ua.ikeepcalm.coi.client.ui.CoiIcons;
import dev.ua.ikeepcalm.coi.client.ui.CoiStyle;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;

/**
 * HUD settings in the mod's dark/gold card style, over a scrollable content
 * card so the screen stays usable at every gui scale.
 * <p>
 * Three tabs, split by what a setting <em>is</em> rather than by which overlay
 * happens to draw it:
 * <ul>
 *   <li><b>Ability HUD</b> — everything about the ability slots themselves.</li>
 *   <li><b>Elements</b> — one uniform block per bar/overlay: show it, scale it,
 *       align it, plus the handful of knobs that only that element has.</li>
 *   <li><b>General</b> — the master switch, accessibility, sound, integrations.</li>
 * </ul>
 * Their rows are built by {@link SettingsTabs} onto a {@link SettingsRows}
 * cursor; this class owns the card, the tabs, the scrolling and the bottom row.
 * <p>
 * Placement lives <em>only</em> in {@link HudLayoutScreen}: every positional
 * control here is an <em>Align</em> button that opens the editor on this
 * screen's working copy, so Done still decides whether anything persists.
 */
public class HudSettingsScreen extends Screen {

    private static final int TAB_H = 22;

    /** Fits the tab row and the card between the title and the bottom buttons. */
    private static final int COMPACT_HEIGHT = 300;

    private enum Tab {HUD, ELEMENTS, GENERAL}

    private final Screen parent;
    private HudConfig.HudSettings settings;
    private Tab currentTab = Tab.HUD;
    private double scrollOffset = 0;

    private SettingsRows rows;
    private int contentX, contentW, viewportTop, viewportBottom, tabContentH, buttonY;
    private Button resetButton;

    public HudSettingsScreen(Screen parent) {
        super(Component.translatable("screen.coi.hud_settings"));
        this.parent = parent;
        this.settings = new HudConfig.HudSettings();
        HudConfig.copySettings(HudConfig.getSettings(), this.settings);
    }

    private boolean compact() {
        return this.height < COMPACT_HEIGHT;
    }

    private int tabY() {
        return compact() ? 18 : 30;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        contentW = CoiStyle.formWidth(this.width);
        contentX = (this.width - contentW) / 2;
        viewportTop = tabY() + TAB_H + 6 + 8;

        addArrangeButton();
        addTabs();
        buildCurrentTab();

        tabContentH = Math.max(0, rows.cursor() - (SettingsRows.ROW_STRIDE - 20));

        // The card shrinks to fit its content on tall screens; on short ones the
        // rows scroll inside it
        viewportBottom = Math.min(this.height - (compact() ? 34 : 44), viewportTop + tabContentH);
        buttonY = Math.min(this.height - (compact() ? 24 : 30), viewportBottom + 14);

        addBottomRow();
        applyScroll();
    }

    private void addArrangeButton() {
        int arrangeW = Math.min(150, contentW / 2);
        int arrangeH = compact() ? 14 : 16;
        this.addRenderableWidget(Button.builder(Component.translatable("screen.coi.layout_open"),
                        b -> openLayout(null))
                .bounds(contentX + contentW - arrangeW, compact() ? 2 : 8, arrangeW, arrangeH).build());
    }

    private void addTabs() {
        int tabW = (contentW - 8) / 3;
        addTab(contentX, tabW, Tab.HUD, "screen.coi.settings_tab_hud", (g, x, y, size, color) -> {
            // Ability slot glyph: outlined square with a dot inside
            g.outline(x, y, size, size, color);
            g.fill(x + 3, y + 3, x + size - 3, y + size - 3, color);
        });
        addTab(contentX + tabW + 4, tabW, Tab.ELEMENTS, "screen.coi.settings_tab_elements", (g, x, y, size, color) -> {
            // Elements glyph: a bar above a smaller card
            g.outline(x, y, size, size / 2, color);
            g.fill(x, y + size / 2 + 2, x + size - 2, y + size, color);
        });
        addTab(contentX + (tabW + 4) * 2, tabW, Tab.GENERAL, "screen.coi.settings_tab_general", (g, x, y, size, color) -> {
            // Sliders glyph: two tracks with handles at different positions.
            // Deliberately drawn rather than blitted — the cog artwork is
            // detailed 64px art and turns to mush in a 9px tab slot
            g.fill(x, y + 1, x + size, y + 2, color);
            g.fill(x + size - 4, y, x + size - 2, y + 3, color);
            g.fill(x, y + 6, x + size, y + 7, color);
            g.fill(x + 2, y + 5, x + 4, y + 8, color);
        });
    }

    private void addTab(int x, int tabW, Tab tab, String labelKey, CoiTabButton.IconPainter icon) {
        this.addRenderableWidget(new CoiTabButton(x, tabY(), tabW, TAB_H,
                Component.translatable(labelKey), icon,
                () -> currentTab == tab,
                () -> {
                    currentTab = tab;
                    scrollOffset = 0;
                    this.init();
                }));
    }

    private void buildCurrentTab() {
        rows = new SettingsRows(this.font, contentX, contentW,
                this::addRenderableWidget, this::init, this::openLayout);
        switch (currentTab) {
            case ELEMENTS -> SettingsTabs.elements(rows, settings);
            case GENERAL -> SettingsTabs.general(rows, settings);
            default -> SettingsTabs.hud(rows, settings);
        }
    }

    private void addBottomRow() {
        int buttonW = (contentW - 8) / 3;
        resetButton = Button.builder(Component.translatable("screen.coi.reset_defaults"),
                button -> {
                    settings = new HudConfig.HudSettings();
                    this.init();
                }).bounds(contentX, buttonY, buttonW, 20).build();
        this.addRenderableWidget(resetButton);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.onClose())
                .bounds(contentX + buttonW + 4, buttonY, buttonW, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                button -> {
                    HudConfig.setSettings(settings);
                    this.onClose();
                }).bounds(contentX + (buttonW + 4) * 2, buttonY, buttonW, 20).build());
    }

    /**
     * Hands the working copy to the layout editor, so arranging and the rows
     * on this screen edit the same settings and Done still decides.
     */
    private void openLayout(String elementId) {
        this.minecraft.gui.setScreen(new HudLayoutScreen(this, elementId, settings));
    }

    // --- Scrolling ---

    private double maxScroll() {
        return Math.max(0, tabContentH - (viewportBottom - viewportTop));
    }

    /**
     * Positions content widgets by scroll offset and hides the ones that don't
     * fully fit the viewport — hidden widgets neither render nor take clicks,
     * which stands in for scissor clipping of live widgets.
     */
    private void applyScroll() {
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());
        for (ContentWidget content : rows.widgets()) {
            int y = viewportTop + content.baseY() - (int) scrollOffset;
            content.widget().setY(y);
            content.widget().visible = y >= viewportTop - 2 && y + content.widget().getHeight() <= viewportBottom + 2;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        scrollOffset = Mth.clamp(scrollOffset - verticalAmount * SettingsRows.ROW_STRIDE, 0, maxScroll());
        applyScroll();
        return true;
    }

    // --- Rendering ---

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fill(0, 0, this.width, this.height, CoiStyle.SCRIM);
        int titleY = compact() ? 5 : 12;
        graphics.centeredText(this.font, this.title, this.width / 2, titleY, CoiStyle.ACCENT);
        // Sits in the gap the centred title leaves, so it never crowds the text
        CoiIcons.draw(graphics, CoiIcons.COG,
                this.width / 2 - this.font.width(this.title) / 2 - CoiIcons.COG_SIZE - 5,
                titleY - (CoiIcons.COG_SIZE - this.font.lineHeight) / 2 - 1, CoiIcons.COG_SIZE);

        int cardTop = tabY() + TAB_H + 6;
        CoiStyle.drawCard(graphics, contentX, cardTop, contentW, viewportBottom + 8 - cardTop);

        super.extractRenderState(graphics, mouseX, mouseY, a);

        ScrollbarPainter.draw(graphics, contentX + contentW + 4, viewportTop, viewportBottom,
                tabContentH, scrollOffset, maxScroll());

        if (!settings.enabled) {
            graphics.centeredText(this.font, Component.translatable("screen.coi.hud_disabled_warning").withStyle(ChatFormatting.RED),
                    this.width / 2, buttonY - 11, 0xFFFF5555);
        }

        if (resetButton.isHovered()) {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("screen.coi.reset_defaults.tooltip"), mouseX, mouseY);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}
