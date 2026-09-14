package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import dev.ua.ikeepcalm.coi.client.ClientActingState;
import dev.ua.ikeepcalm.coi.client.ClientBeyonderState;
import dev.ua.ikeepcalm.coi.client.config.ClientStateStore;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.hud.AbilityHudOverlay;
import dev.ua.ikeepcalm.coi.client.hud.ActingHudOverlay;
import dev.ua.ikeepcalm.coi.client.hud.HudScale;
import dev.ua.ikeepcalm.coi.client.hud.MadnessHudOverlay;
import dev.ua.ikeepcalm.coi.client.hud.SpiritualityHudOverlay;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * First-join walkthrough: dims the screen, spotlights one UI element at a
 * time and explains it. Movement stays enabled (same pattern as the ability
 * wheel) so the world keeps feeling alive behind the cards.
 * <p>
 * Spotlight rectangles are computed from live {@link HudConfig} values every
 * frame, so the tour follows custom HUD layouts. Dismissing the tour by any
 * means marks it completed in {@link ClientStateStore}; the "Show tour again"
 * button in HUD Settings clears the flag.
 */
public class TourScreen extends Screen {

    private static final int CARD_WIDTH = 280;
    private static final int DIM_COLOR = 0xB0000000;
    private static final int ACCENT_RGB = 0xFFD870;

    @FunctionalInterface
    private interface SpotlightRect {
        /**
         * Returns {x, y, width, height} in gui-scaled coordinates.
         */
        int[] get(int screenWidth, int screenHeight);
    }

    private record TourStep(Component title, Component body, SpotlightRect spotlight) {
    }

    private final List<TourStep> steps = new ArrayList<>();
    private int currentStep = 0;

    public TourScreen() {
        super(Component.translatable("screen.coi.tour_title"));
    }

    private static Component keyName(KeyMapping key) {
        return KeyMappingHelper.getBoundKeyOf(key).getDisplayName();
    }

    private void buildSteps() {
        steps.clear();

        steps.add(new TourStep(
                Component.translatable("screen.coi.tour_step1_title"),
                Component.translatable("screen.coi.tour_step1_body"),
                (w, h) -> {
                    // Slots can be scattered now, so the spotlight is the union
                    // of wherever they actually are
                    int[] box = AbilityHudOverlay.rowBounds(w, h, HudConfig.getSettings());
                    return new int[]{box[0] - 6, box[1] - 6, box[2] + 12, box[3] + 13};
                }));

        steps.add(new TourStep(
                Component.translatable("screen.coi.tour_step2_title"),
                Component.translatable("screen.coi.tour_step2_body", keyName(CircleOfImaginationClient.abilityMenu)),
                null));

        steps.add(new TourStep(
                Component.translatable("screen.coi.tour_step3_title"),
                Component.translatable("screen.coi.tour_step3_body", keyName(CircleOfImaginationClient.abilityWheel)),
                null));

        Component madnessBody = Component.translatable("screen.coi.tour_step4_body");
        if (!HudConfig.getSettings().showMadnessBar) {
            madnessBody = madnessBody.copy().append("\n").append(Component.translatable("screen.coi.tour_step4_hidden"));
        }
        steps.add(new TourStep(
                Component.translatable("screen.coi.tour_step4_title"),
                madnessBody,
                (w, h) -> {
                    // Same anchor math the overlay uses, plus the text line above the bar
                    HudConfig.HudSettings s = HudConfig.getSettings();
                    int[] pos = MadnessHudOverlay.anchor(w, h, s);
                    return barSpotlight(pos, HudScale.size(MadnessHudOverlay.BAR_WIDTH, s.madnessScale),
                            HudScale.size(MadnessHudOverlay.BAR_HEIGHT, s.madnessScale));
                }));

        // Only worth a step when the server actually feeds spirituality
        if (ClientBeyonderState.hasSpiritualityData()) {
            steps.add(new TourStep(
                    Component.translatable("screen.coi.tour_step_spirit_title"),
                    Component.translatable("screen.coi.tour_step_spirit_body"),
                    (w, h) -> {
                        // The bar is a sprite with its own overhang, so it reports its own box
                        int[] box = SpiritualityHudOverlay.bounds(w, h, HudConfig.getSettings());
                        return new int[]{box[0] - 4, box[1] - 4, box[2] + 8, box[3] + 8};
                    }));
        }

        // Outer pathways never gain acting, so their bar is never drawn either
        if (ClientActingState.hasData() && !ClientActingState.isOuter()) {
            steps.add(new TourStep(
                    Component.translatable("screen.coi.tour_step_acting_title"),
                    Component.translatable("screen.coi.tour_step_acting_body"),
                    (w, h) -> {
                        HudConfig.HudSettings s = HudConfig.getSettings();
                        int[] pos = ActingHudOverlay.anchor(w, h, s);
                        return barSpotlight(pos, HudScale.size(ActingHudOverlay.BAR_WIDTH, s.actingScale),
                                HudScale.size(ActingHudOverlay.BAR_HEIGHT, s.actingScale));
                    }));
        }

        steps.add(new TourStep(
                Component.translatable("screen.coi.tour_step5_title"),
                Component.translatable("screen.coi.tour_step5_body"),
                null));
    }

    /**
     * Spotlight around a bar at {@code pos}, widened to take in the label line
     * that sits above it.
     */
    private static int[] barSpotlight(int[] pos, int barWidth, int barHeight) {
        return new int[]{pos[0] - 5, pos[1] - 15, barWidth + 10, barHeight + 26};
    }

    @Override
    protected void init() {
        this.clearWidgets();
        if (steps.isEmpty()) buildSteps();

        int[] card = cardRect();
        int cardX = card[0];
        int cardW = card[2];
        int buttonsY = card[1] + card[3] + 8;
        boolean last = currentStep == steps.size() - 1;

        this.addRenderableWidget(Button.builder(Component.translatable("screen.coi.tour_skip"), b -> this.onClose())
                .bounds(cardX, buttonsY, 90, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable(last ? "screen.coi.tour_finish" : "screen.coi.tour_next"),
                b -> {
                    if (currentStep >= steps.size() - 1) {
                        this.onClose();
                    } else {
                        currentStep++;
                        this.init();
                    }
                }).bounds(cardX + cardW - 90, buttonsY, 90, 20).build());
    }

    private int[] cardRect() {
        TourStep step = steps.get(currentStep);
        List<FormattedCharSequence> lines = this.font.split(step.body(), CARD_WIDTH - 24);
        int cardH = 12 + 12 + 6 + lines.size() * 10 + 12;
        int cardX = (this.width - CARD_WIDTH) / 2;
        int cardY = (this.height - cardH) / 2 - 20;
        return new int[]{cardX, cardY, CARD_WIDTH, cardH};
    }

    @Override
    public void tick() {
        keepMovementKeysAlive();
    }

    /**
     * Screens normally swallow keyboard input, freezing the player. Feed the
     * raw key state back into the movement bindings so the player can keep
     * moving while reading the tour.
     */
    private void keepMovementKeysAlive() {
        if (this.minecraft.player == null) return;
        var options = this.minecraft.options;
        KeyMapping[] movementKeys = {
                options.keyUp, options.keyDown, options.keyLeft, options.keyRight,
                options.keyJump, options.keyShift, options.keySprint
        };
        for (KeyMapping key : movementKeys) {
            key.setDown(CircleOfImaginationClient.isKeyDown(key));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        TourStep step = steps.get(currentStep);

        int[] spot = step.spotlight() != null ? step.spotlight().get(this.width, this.height) : null;
        if (spot != null) {
            // Dim everything except the spotlight cutout (four rects around it)
            int sx = spot[0];
            int sy = spot[1];
            int sw = spot[2];
            int sh = spot[3];
            graphics.fill(0, 0, this.width, Math.max(0, sy), DIM_COLOR);
            graphics.fill(0, sy + sh, this.width, this.height, DIM_COLOR);
            graphics.fill(0, sy, Math.max(0, sx), sy + sh, DIM_COLOR);
            graphics.fill(sx + sw, sy, this.width, sy + sh, DIM_COLOR);

            float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() * 0.006);
            graphics.outline(sx, sy, sw, sh, ((int) (140 + 90 * pulse) << 24) | ACCENT_RGB);
            graphics.outline(sx - 1, sy - 1, sw + 2, sh + 2, ((int) (50 + 60 * pulse) << 24) | ACCENT_RGB);
        } else {
            graphics.fill(0, 0, this.width, this.height, 0x98000000);
        }

        int[] card = cardRect();
        int cardX = card[0];
        int cardY = card[1];
        int cardW = card[2];
        int cardH = card[3];

        graphics.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0xF0121216);
        graphics.outline(cardX, cardY, cardW, cardH, 0xFF3A3A46);
        graphics.fill(cardX, cardY, cardX + cardW, cardY + 1, 0xFF000000 | ACCENT_RGB);

        graphics.text(this.font, step.title(), cardX + 12, cardY + 12, 0xFF000000 | ACCENT_RGB);

        String progress = (currentStep + 1) + " / " + steps.size();
        graphics.text(this.font, progress, cardX + cardW - 12 - this.font.width(progress), cardY + 12, 0xFF808088, false);

        List<FormattedCharSequence> lines = this.font.split(step.body(), CARD_WIDTH - 24);
        int lineY = cardY + 12 + 12 + 6;
        for (FormattedCharSequence line : lines) {
            graphics.text(this.font, line, cardX + 12, lineY, 0xFFE0E0E0);
            lineY += 10;
        }

        // Widgets (buttons) on top
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public void onClose() {
        // Dismissing the tour by any means (Finish, Skip, Esc) counts as done
        ClientStateStore.setTourCompleted(true);
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(null);
        }
    }
}
