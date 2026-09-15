package dev.ua.ikeepcalm.coi.client.effect.visual;

import dev.ua.ikeepcalm.coi.client.effect.HallucinationManager;
import dev.ua.ikeepcalm.coi.client.effect.VisualEffect;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Pseudo-effect: fires a single hallucination event and finishes immediately.
 * Exists so hallucinations show up in the Effect Debug screen and can be
 * triggered by the server through the regular effect payload.
 * <p>
 * Params: {@code event} ({@code footsteps}, {@code whisper}, {@code cave},
 * {@code block}, {@code flicker} or {@code random}).
 */
public class HallucinationEffect implements VisualEffect {

    public static final String ID = "hallucination";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Hallucination (one-shot)";
    }

    @Override
    public String getDefaultParams() {
        return "event=random";
    }

    @Override
    public void start(String params) {
        HallucinationManager.triggerNamed(params);
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int screenWidth, int screenHeight, float tickDelta) {
    }

    @Override
    public boolean isFinished() {
        return true;
    }
}
