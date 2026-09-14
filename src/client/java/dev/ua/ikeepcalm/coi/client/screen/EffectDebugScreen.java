package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.ClientActingState;
import dev.ua.ikeepcalm.coi.client.ClientActionBarState;
import dev.ua.ikeepcalm.coi.client.ClientBeyonderState;
import dev.ua.ikeepcalm.coi.client.ClientCogitationState;
import dev.ua.ikeepcalm.coi.client.ClientNotificationState;
import dev.ua.ikeepcalm.coi.client.ClientResourceState;
import dev.ua.ikeepcalm.coi.client.ClientSheetState;
import dev.ua.ikeepcalm.coi.client.ClientTargetState;
import dev.ua.ikeepcalm.coi.client.effects.EffectManager;
import dev.ua.ikeepcalm.coi.client.effects.VisualEffect;
import dev.ua.ikeepcalm.coi.client.effects.impl.ImpactFrameEffect;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalFormManager;
import dev.ua.ikeepcalm.coi.client.menu.ClientMenuState;
import dev.ua.ikeepcalm.coi.client.screen.menu.MenuScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

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
    /**
     * Default max for the debug bar, so the buttons do something useful before
     * a server has ever sent a real one.
     */
    private static final int DEBUG_MAX_SPIRIT = 1000;

    private final Screen parent;
    private final List<EffectRow> rows = new ArrayList<>();
    private EditBox paramsField;
    private int madnessRowY;
    private int spiritRowY;
    private int actingRowY;
    private int overlayRowY;
    private int resourceRowY;

    public EffectDebugScreen(Screen parent) {
        super(Component.literal("Visual Effects — Debug"));
        this.parent = parent;
    }

    private static void addMadness(double delta) {
        setMadness(ClientBeyonderState.getMadness() + delta);
    }

    /**
     * Jumps to the next stage threshold: 0 → 25 → 50 → 75 → 100 → 0.
     */
    private static void cycleStage() {
        double m = ClientBeyonderState.getMadness();
        double next;
        if (m < 25) next = 25;
        else if (m < 50) next = 50;
        else if (m < 75) next = 75;
        else if (m < 100) next = 100;
        else next = 0;
        setMadness(next);
    }

    private static void addPermMadness(double delta) {
        double perm = Math.clamp(ClientBeyonderState.getPermanentMadness() + delta, 0.0, 100.0);
        ClientBeyonderState.updateConditions(
                ClientBeyonderState.getMadness(),
                perm,
                ClientBeyonderState.getFreezeStacks(),
                ClientBeyonderState.getMentalPressure(),
                ClientBeyonderState.getTiredness());
    }

    /**
     * Sets madness via updateConditions so increases also trigger the
     * bar's flash/shake animation, exactly like a server update would.
     */
    private static void setMadness(double value) {
        ClientBeyonderState.updateConditions(
                Math.clamp(value, 0.0, 100.0),
                ClientBeyonderState.getPermanentMadness(),
                ClientBeyonderState.getFreezeStacks(),
                ClientBeyonderState.getMentalPressure(),
                ClientBeyonderState.getTiredness());
    }

    /**
     * Fills every row the character plate can show, so it can be judged without
     * a server: an identity for the header, a madness value for the sanity
     * gauge with a permanent floor to cap it, an acting grant and its cooldown
     * for the mask row, and the two fake resource meters for the reserves.
     */
    private static void seedPlate() {
        if (!ClientBeyonderState.hasIdentity()) ClientBeyonderState.updateIdentity("fool", 5);
        if (ClientBeyonderState.getMadness() <= 0) setMadness(38);
        if (ClientBeyonderState.getPermanentMadness() <= 0) addPermMadness(12);
        ClientActingState.debugGrant(10);
        ClientActingState.debugCooldown(252);
        ClientResourceState.debugInject();
    }

    /**
     * A Sequence-0 sized pool, so the readout has to solve the four-digit
     * fitting problem the bar was written for.
     */
    private static final double DEBUG_MAX_HEALTH = 1750;

    private static int debugMaxSpirit() {
        int max = ClientBeyonderState.getMaxSpirituality();
        return max > 0 ? max : DEBUG_MAX_SPIRIT;
    }

    private static void addSpirit(int delta) {
        int max = debugMaxSpirit();
        int current = Math.clamp(ClientBeyonderState.getSpirituality() + delta, 0, max);
        ClientBeyonderState.updateSpirituality(current, max, current < max);
    }

    private static void setSpiritMax(int max) {
        int current = Math.min(ClientBeyonderState.getSpirituality(), max);
        ClientBeyonderState.updateSpirituality(current, max, current < max);
    }

    @Override
    protected void init() {
        rows.clear();

        int panelX = (this.width - PANEL_W) / 2;
        int y = 50;

        // Params input shared by all "Test" buttons
        this.paramsField = new EditBox(this.font,
                panelX, y, PANEL_W - 4, 20, Component.literal("params")
        );
        paramsField.setMaxLength(200);
        paramsField.setHint(Component.literal("params (leave blank for defaults)").withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(paramsField);
        y += 28;

        // One row per registered effect
        Map<String, Supplier<VisualEffect>> registry = EffectManager.getRegistry();
        for (Map.Entry<String, Supplier<VisualEffect>> entry : registry.entrySet()) {
            String id = entry.getKey();
            VisualEffect probe = entry.getValue().get(); // just for metadata
            String defaultParams = probe.getDefaultParams();

            final int rowY = y;

            // [Test] button
            Button testBtn = Button.builder(Component.literal("Test"), btn -> {
                String raw = paramsField.getValue().trim();
                String p = raw.isEmpty() ? defaultParams : raw;
                if (ImpactFrameEffect.ID.equals(id)) {
                    if (raw.isEmpty()) {
                        p = "style=burst,scope=world,color=FFFFFF,accent=FF7A22,intensity=0.95,radius=2.5,duration=1200";
                    }
                    EffectManager.triggerDebug(id, p);
                    onClose();
                } else {
                    EffectManager.trigger(id, p);
                }
            }).bounds(panelX, rowY, BTN_W, 20).build();
            addRenderableWidget(testBtn);

            // [Stop] button
            Button stopBtn = Button.builder(Component.literal("Stop"), btn ->
                    EffectManager.stopEffect(id)).bounds(panelX + BTN_W + 4, rowY, 50, 20).build();
            addRenderableWidget(stopBtn);

            // [Defaults] button — fills the params field with this effect's defaults
            Button defsBtn = Button.builder(Component.literal("↩ defaults"), btn -> paramsField.setValue(defaultParams)).bounds(panelX + BTN_W + 58, rowY, 90, 20).build();
            addRenderableWidget(defsBtn);

            rows.add(new EffectRow(id, probe.getDisplayName(), panelX + BTN_W + 154, rowY));
            y += ROW_H;
        }

        y += 6;

        // Madness debug controls — exercise the madness bar stages without a server
        madnessRowY = y;
        addRenderableWidget(Button.builder(Component.literal("-10"), btn -> addMadness(-10))
                .bounds(panelX, madnessRowY, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+10"), btn -> addMadness(10))
                .bounds(panelX + 44, madnessRowY, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cycle Stage"), btn -> cycleStage())
                .bounds(panelX + 88, madnessRowY, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Perm +10"), btn -> addPermMadness(10))
                .bounds(panelX + 172, madnessRowY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), btn -> ClientBeyonderState.reset())
                .bounds(panelX + 246, madnessRowY, 50, 20).build());
        y += 26;

        // Spirituality debug controls — drive the spirituality bar with no server
        spiritRowY = y;
        addRenderableWidget(Button.builder(Component.literal("Spirit -50"), btn -> addSpirit(-50))
                .bounds(panelX, spiritRowY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Spirit +50"), btn -> addSpirit(50))
                .bounds(panelX + 74, spiritRowY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Spirit Full"), btn -> addSpirit(debugMaxSpirit()))
                .bounds(panelX + 148, spiritRowY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Max 1000"), btn -> setSpiritMax(DEBUG_MAX_SPIRIT))
                .bounds(panelX + 222, spiritRowY, 70, 20).build());
        y += 26;

        // Acting debug controls
        actingRowY = y;
        addRenderableWidget(Button.builder(Component.literal("Acting +10%"), btn -> ClientActingState.debugGrant(10))
                .bounds(panelX, actingRowY, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Acting CD 5m"), btn -> ClientActingState.debugCooldown(300))
                .bounds(panelX + 84, actingRowY, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Acting reset"), btn -> ClientActingState.reset())
                .bounds(panelX + 168, actingRowY, 80, 20).build());
        y += 26;

        // Batch 3 overlays — action bar, target health, cogitation, toasts
        overlayRowY = y;
        addRenderableWidget(Button.builder(Component.literal("ActionBar"), btn -> ClientActionBarState.debugInject(3000))
                .bounds(panelX, overlayRowY, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Target"), btn -> ClientTargetState.debugHit())
                .bounds(panelX + 84, overlayRowY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cogitate"), btn -> ClientCogitationState.debugPrompt())
                .bounds(panelX + 158, overlayRowY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Toast"), btn -> ClientNotificationState.debugToast())
                .bounds(panelX + 232, overlayRowY, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Sheet"), btn -> {
            // Fake payload first, so the sheet has something to draw offline;
            // CharacterSheetScreen itself never sends to a server we aren't on.
            ClientSheetState.debugInject();
            minecraft.gui.setScreen(new CharacterSheetScreen(this));
        }).bounds(panelX + 296, overlayRowY, 60, 20).build());
        // Same idea for the declarative menus: the sample document covers every
        // component type, so the renderer can be judged with no server attached
        addRenderableWidget(Button.builder(Component.literal("Menu"), btn -> {
            ClientMenuState.debugInject();
            minecraft.gui.setScreen(new MenuScreen(this));
        }).bounds(panelX + 360, overlayRowY, 76, 20).build());
        y += 26;

        // Resource meters — the overlay row above is full, so these get their own
        resourceRowY = y;
        addRenderableWidget(Button.builder(Component.literal("Resource"), btn -> ClientResourceState.debugInject())
                .bounds(panelX, resourceRowY, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Res clear"), btn -> ClientResourceState.debugClear())
                .bounds(panelX + 84, resourceRowY, 70, 20).build());
        // The pool bar only replaces the hearts once a server has named a
        // maximum; these two stand in for that one conditions key. The current
        // value needs no button - it is derived from vanilla health, so taking
        // real damage in the dev world moves the bar
        addRenderableWidget(Button.builder(Component.literal("HP pool"), btn -> ClientBeyonderState.updateMaxHealth(DEBUG_MAX_HEALTH))
                .bounds(panelX + 158, resourceRowY, 65, 20).build());
        addRenderableWidget(Button.builder(Component.literal("HP clear"), btn -> ClientBeyonderState.updateMaxHealth(0))
                .bounds(panelX + 227, resourceRowY, 65, 20).build());
        // The plate needs an identity, a madness value, acting and a resource
        // before it draws more than a header, so one button seeds all four
        addRenderableWidget(Button.builder(Component.literal("Plate sample"), btn -> seedPlate())
                .bounds(panelX + 296, resourceRowY, 80, 20).build());
        y += 26;

        int formY = y;
        java.util.List<String> forms = MythicalFormManager.getRegisteredPathwayNames();
        String currentForm = minecraft.player != null ? MythicalFormManager.getForm(minecraft.player.getUUID().toString()) : null;
        final int[] activeIndex = {-1};
        if (currentForm != null) {
            for (int i = 0; i < forms.size(); i++) {
                if (forms.get(i).equalsIgnoreCase(currentForm)) {
                    activeIndex[0] = i;
                    break;
                }
            }
        }

        String label = activeIndex[0] == -1 ? "Form: None (Click to cycle)" : "Form: " + forms.get(activeIndex[0]);
        Button formCycleBtn = Button.builder(Component.literal(label), btn -> {
            if (minecraft.player == null || forms.isEmpty()) return;
            String uuid = minecraft.player.getUUID().toString();
            activeIndex[0] = (activeIndex[0] + 1) % forms.size();
            String selected = forms.get(activeIndex[0]);
            MythicalFormManager.handlePacket(uuid, selected + ":true:start");
            btn.setMessage(Component.literal("Form: " + selected));
        }).bounds(panelX, formY, PANEL_W / 2 - 2, 20).build();
        addRenderableWidget(formCycleBtn);

        addRenderableWidget(Button.builder(Component.literal("Clear Form").withStyle(ChatFormatting.YELLOW), btn -> {
            if (minecraft.player != null) {
                MythicalFormManager.handlePacket(minecraft.player.getUUID().toString(), ":true:stop");
                activeIndex[0] = -1;
                formCycleBtn.setMessage(Component.literal("Form: None (Click to cycle)"));
            }
        }).bounds(panelX + PANEL_W / 2 + 2, formY, PANEL_W / 2 - 2, 20).build());

        y += 26;

        addRenderableWidget(Button.builder(
                Component.literal("Appearance Traits — Local Preview").withStyle(ChatFormatting.AQUA),
                btn -> minecraft.gui.setScreen(new AppearanceDebugScreen(this))
        ).bounds(panelX, y, PANEL_W, 20).build());
        y += 26;

        // Stop All
        addRenderableWidget(Button.builder(Component.literal("Stop All Effects").withStyle(ChatFormatting.RED),
                btn -> EffectManager.stopAll()).bounds(panelX, y, PANEL_W / 2 - 2, 20).build());

        // Done
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), btn -> onClose()).bounds(panelX + PANEL_W / 2 + 2, y, PANEL_W / 2 - 2, 20).build());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // Semi-transparent panel behind controls (no blur — world is still rendering)
        int panelX = (this.width - PANEL_W) / 2;
        int panelH = 50 + EffectManager.getRegistry().size() * ROW_H + 34 + 26 + 26 + 26 + 26 + 26 + 26 + 26;
        graphics.fill(panelX - 8, 8, panelX + PANEL_W + 8, 8 + panelH, 0xCC000000);

        super.extractRenderState(graphics, mouseX, mouseY, a);

        // Title
        graphics.centeredText(font, Component.literal("Visual Effects — Debug").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                this.width / 2, 18, 0xFFFFFFFF);

        // Column header
        graphics.text(font, Component.literal("Params:").withStyle(ChatFormatting.GRAY),
                panelX, 38, 0xFFFFFFFF);

        // Effect name labels + active indicator
        for (EffectRow row : rows) {
            boolean active = EffectManager.isActive(row.id);
            int nameColor = active ? 0xFF55FF55 : 0xFFAAAAAA;
            String indicator = active ? "● " : "○ ";
            graphics.text(font, Component.literal(indicator + row.displayName).withStyle(active ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                    row.labelX, row.y + 6, nameColor);
        }

        // Live madness readout next to the debug controls
        double m = ClientBeyonderState.getMadness();
        int stage = m >= 100 ? 4 : m >= 75 ? 3 : m >= 50 ? 2 : m >= 25 ? 1 : 0;
        String madnessLabel = String.format("Madness %.0f%% · S%d (Min %.0f%%)",
                m, stage, ClientBeyonderState.getPermanentMadness());
        graphics.text(font, Component.literal(madnessLabel).withStyle(ChatFormatting.LIGHT_PURPLE),
                panelX + 300, madnessRowY + 6, 0xFFFFFFFF);

        // Live spirituality readout next to its own row
        String spiritLabel = ClientBeyonderState.hasSpiritualityData()
                ? String.format("Spirit %d / %d", ClientBeyonderState.getSpirituality(), ClientBeyonderState.getMaxSpirituality())
                : "Spirit — no data";
        graphics.text(font, Component.literal(spiritLabel).withStyle(ChatFormatting.AQUA),
                panelX + 300, spiritRowY + 6, 0xFFFFFFFF);

        // Label for the overlay test row
        graphics.text(font, Component.literal("Overlays").withStyle(ChatFormatting.GRAY),
                panelX + 300, overlayRowY + 6, 0xFFFFFFFF);

        // Live acting readout
        String actingLabel = ClientActingState.hasData()
                ? String.format("Acting %.1f%% \u00B7 CD %s", ClientActingState.getPercent(), ClientActingState.cooldownClock())
                : "Acting \u2014 no data";
        graphics.text(font, Component.literal(actingLabel).withStyle(ChatFormatting.GOLD),
                panelX + 260, actingRowY + 6, 0xFFFFFFFF);

        // Live resource-bar count
        graphics.text(font, Component.literal("Resource bars: " + ClientResourceState.visible().size()).withStyle(ChatFormatting.GRAY),
                panelX + 160, resourceRowY + 6, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record EffectRow(String id, String displayName, int labelX, int y) {
    }
}
