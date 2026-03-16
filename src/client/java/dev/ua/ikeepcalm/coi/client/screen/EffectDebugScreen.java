package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.effects.EffectManager;
import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Developer-only screen for testing visual effects without server commands.
 * Only accessible via the debug keybinding registered in dev environments.
 */
public class EffectDebugScreen extends Screen {

    private static final int ROW_H = 26;
    private static final int BTN_W = 80;
    private static final int PANEL_W = 440;

    private final Screen parent;
    private TextFieldWidget paramsField;
    private final List<EffectRow> rows = new ArrayList<>();

    public EffectDebugScreen(Screen parent) {
        super(Text.literal("Visual Effects — Debug"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rows.clear();

        int panelX = (this.width - PANEL_W) / 2;
        int y = 50;

        // Params input shared by all "Test" buttons
        this.paramsField = new TextFieldWidget(
                this.textRenderer,
                panelX, y, PANEL_W - 4, 20,
                Text.literal("params")
        );
        paramsField.setMaxLength(200);
        paramsField.setPlaceholder(Text.literal("params (leave blank for defaults)").formatted(Formatting.DARK_GRAY));
        addDrawableChild(paramsField);
        y += 28;

        // One row per registered effect
        Map<String, Supplier<VisualEffect>> registry = EffectManager.getRegistry();
        for (Map.Entry<String, Supplier<VisualEffect>> entry : registry.entrySet()) {
            String id = entry.getKey();
            VisualEffect probe = entry.getValue().get(); // just for metadata
            String defaultParams = probe.getDefaultParams();

            final int rowY = y;

            // [Test] button
            ButtonWidget testBtn = ButtonWidget.builder(Text.literal("Test"), btn -> {
                String raw = paramsField.getText().trim();
                String p = raw.isEmpty() ? defaultParams : raw;
                EffectManager.trigger(id, p);
            }).dimensions(panelX, rowY, BTN_W, 20).build();
            addDrawableChild(testBtn);

            // [Stop] button
            ButtonWidget stopBtn = ButtonWidget.builder(Text.literal("Stop"), btn ->
                    EffectManager.stopEffect(id)
            ).dimensions(panelX + BTN_W + 4, rowY, 50, 20).build();
            addDrawableChild(stopBtn);

            // [Defaults] button — fills the params field with this effect's defaults
            ButtonWidget defsBtn = ButtonWidget.builder(Text.literal("↩ defaults"), btn ->
                    paramsField.setText(defaultParams)
            ).dimensions(panelX + BTN_W + 58, rowY, 90, 20).build();
            addDrawableChild(defsBtn);

            rows.add(new EffectRow(id, probe.getDisplayName(), panelX + BTN_W + 154, rowY));
            y += ROW_H;
        }

        y += 6;

        // Stop All
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Stop All Effects").formatted(Formatting.RED),
                btn -> EffectManager.stopAll()
        ).dimensions(panelX, y, PANEL_W / 2 - 2, 20).build());

        // Done
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.done"),
                btn -> close()
        ).dimensions(panelX + PANEL_W / 2 + 2, y, PANEL_W / 2 - 2, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Semi-transparent panel behind controls (no blur — world is still rendering)
        int panelX = (this.width - PANEL_W) / 2;
        int panelH = 50 + EffectManager.getRegistry().size() * ROW_H + 34;
        ctx.fill(panelX - 8, 8, panelX + PANEL_W + 8, 8 + panelH, 0xCC000000);

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Visual Effects — Debug").formatted(Formatting.GOLD, Formatting.BOLD),
                this.width / 2, 18, 0xFFFFFFFF);

        // Column header
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("Params:").formatted(Formatting.GRAY),
                panelX, 38, 0xFFFFFFFF);

        // Effect name labels + active indicator
        for (EffectRow row : rows) {
            boolean active = EffectManager.isActive(row.id);
            int nameColor = active ? 0xFF55FF55 : 0xFFAAAAAA;
            String indicator = active ? "● " : "○ ";
            ctx.drawTextWithShadow(textRenderer,
                    Text.literal(indicator + row.displayName).formatted(active ? Formatting.GREEN : Formatting.GRAY),
                    row.labelX, row.y + 6, nameColor);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record EffectRow(String id, String displayName, int labelX, int y) {
    }
}
