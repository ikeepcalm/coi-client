package dev.ua.ikeepcalm.coi.client.effect;

import dev.ua.ikeepcalm.coi.CoiLog;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.effect.visual.*;

import java.util.*;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Registry and active list for the screen-space visual effects the server
 * triggers over {@code coi-client:effect}. Every registered effect is created
 * fresh on each trigger, rendered from a single HUD element attached before
 * chat, and dropped once it reports itself finished.
 */
public class EffectManager {

    private static final Map<String, Supplier<VisualEffect>> REGISTRY = new LinkedHashMap<>();
    private static final List<VisualEffect> activeEffects = new ArrayList<>();
    private static final Set<String> PHOTOSENSITIVE_EFFECTS = Set.of(FlashEffect.ID, GlitchEffect.ID, HeartbeatEffect.ID);

    private EffectManager() {
    }

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
        register(ImpactFrameEffect.ID, ImpactFrameEffect::new);
        register(HallucinationEffect.ID, HallucinationEffect::new);
        ImpactFrameEffect.initializeWorldRenderer();

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath("coi-client", "effects"), EffectManager::render);
    }

    private static void register(String id, Supplier<VisualEffect> factory) {
        REGISTRY.put(id, factory);
    }

    /**
     * Trigger an effect by id. Special cases:
     * params = "stop"   → remove this specific effect
     * effectId = "all"  → stop all active effects (params ignored)
     */
    public static void trigger(String effectId, String params) {
        trigger(effectId, params, false);
    }

    public static void triggerDebug(String effectId, String params) {
        trigger(effectId, params, true);
    }

    private static void trigger(String effectId, String params, boolean bypassPhotosensitiveGuard) {
        if (!bypassPhotosensitiveGuard && HudConfig.getSettings().epilepsyMode && PHOTOSENSITIVE_EFFECTS.contains(effectId)) {
            CoiLog.LOG.info("Skipped photosensitive effect '{}' because epilepsy mode is enabled", effectId);
            return;
        }

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
            CoiLog.LOG.warn("Unknown effect '{}'", effectId);
            return;
        }

        // Replace existing effect of the same type
        activeEffects.removeIf(e -> e.getId().equals(effectId));

        VisualEffect effect = factory.get();
        effect.start(params);
        activeEffects.add(effect);
        EffectSounds.onEffectStart(effectId);
    }

    public static void stopEffect(String effectId) {
        if (ImpactFrameEffect.ID.equals(effectId)) {
            ImpactFrameEffect.clearWorldImpacts();
        }

        activeEffects.stream().filter(e -> e.getId().equals(effectId)).forEach(VisualEffect::stop);
        activeEffects.removeIf(e -> e.getId().equals(effectId));
        EffectSounds.onEffectStop(effectId);
    }

    public static void stopAll() {
        ImpactFrameEffect.clearWorldImpacts();
        activeEffects.forEach(VisualEffect::stop);
        activeEffects.clear();
        EffectSounds.stopAll();
    }

    public static boolean isActive(String effectId) {
        return activeEffects.stream().anyMatch(e -> e.getId().equals(effectId));
    }

    public static Map<String, Supplier<VisualEffect>> getRegistry() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    private static void render(GuiGraphicsExtractor ctx, DeltaTracker counter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();
        float tickDelta = 1.0f;

        dropFinishedEffects();

        for (VisualEffect effect : new ArrayList<>(activeEffects)) {
            effect.render(ctx, w, h, tickDelta);
        }
    }

    /**
     * An effect that has run its course is removed here, not by whoever
     * triggered it, so its looping sound stops with it.
     */
    private static void dropFinishedEffects() {
        Iterator<VisualEffect> it = activeEffects.iterator();
        while (it.hasNext()) {
            VisualEffect effect = it.next();
            if (effect.isFinished()) {
                EffectSounds.onEffectStop(effect.getId());
                it.remove();
            }
        }
    }
}
