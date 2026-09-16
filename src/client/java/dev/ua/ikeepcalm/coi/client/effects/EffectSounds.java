package dev.ua.ikeepcalm.coi.client.effects;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.effects.impl.CracksEffect;
import dev.ua.ikeepcalm.coi.client.effects.impl.FrostEffect;
import dev.ua.ikeepcalm.coi.client.effects.impl.GlitchEffect;
import dev.ua.ikeepcalm.coi.client.effects.impl.HeartbeatEffect;
import dev.ua.ikeepcalm.coi.client.effects.impl.TunnelEffect;
import dev.ua.ikeepcalm.coi.client.effects.impl.WhispersEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;

/**
 * Audio companion for visual effects. Looping sounds follow the effect's
 * lifetime (stopped when the effect stops or finishes naturally); one-shots
 * simply play out. All playback is scaled by the effectSoundVolume HUD
 * setting — 0 disables effect sounds entirely.
 */
public class EffectSounds {

    private record Spec(Identifier soundId, boolean loop, float volume) {
        static Spec loop(String path, float volume) {
            return new Spec(id(path), true, volume);
        }

        static Spec once(String path, float volume) {
            return new Spec(id(path), false, volume);
        }
    }

    private static final Map<String, Spec> SPECS = Map.of(
            HeartbeatEffect.ID, Spec.loop("effect.heartbeat", 0.9f),
            WhispersEffect.ID, Spec.loop("effect.whispers", 0.8f),
            TunnelEffect.ID, Spec.loop("effect.tunnel", 0.7f),
            CracksEffect.ID, Spec.once("effect.cracks", 0.8f),
            FrostEffect.ID, Spec.once("effect.frost", 0.8f),
            GlitchEffect.ID, Spec.once("effect.glitch", 0.7f)
    );

    private static final Map<String, SoundInstance> activeLoops = new HashMap<>();

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("coi-client", path);
    }

    public static void onEffectStart(String effectId) {
        Spec spec = SPECS.get(effectId);
        if (spec == null) return;
        float master = HudConfig.getSettings().effectSoundVolume;
        if (master <= 0f) return;

        if (spec.loop()) {
            // Effect restarts of the same type keep the already-running loop
            if (activeLoops.containsKey(effectId)) return;
            SoundInstance instance = globalSound(spec, master, true);
            activeLoops.put(effectId, instance);
            Minecraft.getInstance().getSoundManager().play(instance);
        } else {
            Minecraft.getInstance().getSoundManager().play(globalSound(spec, master, false));
        }
    }

    public static void onEffectStop(String effectId) {
        SoundInstance loop = activeLoops.remove(effectId);
        if (loop != null) {
            Minecraft.getInstance().getSoundManager().stop(loop);
        }
    }

    public static void stopAll() {
        activeLoops.values().forEach(loop -> Minecraft.getInstance().getSoundManager().stop(loop));
        activeLoops.clear();
    }

    /**
     * Non-positional, non-attenuated sound — plays "inside the player's head"
     * like the visual effects it accompanies.
     */
    private static SoundInstance globalSound(Spec spec, float master, boolean loop) {
        return new SimpleSoundInstance(spec.soundId(), SoundSource.AMBIENT,
                spec.volume() * master, 1.0f, RandomSource.create(), loop, 0,
                SoundInstance.Attenuation.NONE, 0, 0, 0, true);
    }
}
