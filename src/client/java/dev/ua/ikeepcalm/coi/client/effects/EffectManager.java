package dev.ua.ikeepcalm.coi.client.effects;

import dev.ua.ikeepcalm.coi.client.effects.impl.*;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.*;
import java.util.function.Supplier;

public class EffectManager {

    private static final Map<String, Supplier<VisualEffect>> REGISTRY = new LinkedHashMap<>();
    private static final List<VisualEffect> activeEffects = new ArrayList<>();

    public static void initialize() {
        register(CracksEffect.ID, CracksEffect::new);
        register(EyesEffect.ID, EyesEffect::new);
        register(VignetteEffect.ID, VignetteEffect::new);
        register(HeartbeatEffect.ID, HeartbeatEffect::new);
        register(GlitchEffect.ID, GlitchEffect::new);
        register(BloodRainEffect.ID, BloodRainEffect::new);
        register(FrostEffect.ID, FrostEffect::new);
        register(WhispersEffect.ID, WhispersEffect::new);
        register(TunnelEffect.ID, TunnelEffect::new);
        register(FlashEffect.ID, FlashEffect::new);

        HudRenderCallback.EVENT.register(EffectManager::render);
    }

    public static void register(String id, Supplier<VisualEffect> factory) {
        REGISTRY.put(id, factory);
    }

    /**
     * Trigger an effect by id. Special cases:
     * params = "stop"   → remove this specific effect
     * effectId = "all"  → stop all active effects (params ignored)
     */
    public static void trigger(String effectId, String params) {
        if ("all".equals(effectId)) {
            stopAll();
            return;
        }
        if ("stop".equals(params)) {
            stopEffect(effectId);
            return;
        }

        Supplier<VisualEffect> factory = REGISTRY.get(effectId);
        if (factory == null) {
            System.out.println("COI Effects: Unknown effect '" + effectId + "'");
            return;
        }

        // Replace existing effect of the same type
        activeEffects.removeIf(e -> e.getId().equals(effectId));

        VisualEffect effect = factory.get();
        effect.start(params);
        activeEffects.add(effect);
    }

    public static void stopEffect(String effectId) {
        activeEffects.stream()
                .filter(e -> e.getId().equals(effectId))
                .forEach(VisualEffect::stop);
        activeEffects.removeIf(e -> e.getId().equals(effectId));
    }

    public static void stopAll() {
        activeEffects.forEach(VisualEffect::stop);
        activeEffects.clear();
    }

    public static boolean isActive(String effectId) {
        return activeEffects.stream().anyMatch(e -> e.getId().equals(effectId));
    }

    public static Map<String, Supplier<VisualEffect>> getRegistry() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    private static void render(DrawContext ctx, RenderTickCounter counter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();
        float tickDelta = 1.0f;

        activeEffects.removeIf(VisualEffect::isFinished);

        for (VisualEffect effect : new ArrayList<>(activeEffects)) {
            effect.render(ctx, w, h, tickDelta);
        }
    }
}
