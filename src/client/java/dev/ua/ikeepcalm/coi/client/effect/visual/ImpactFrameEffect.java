package dev.ua.ikeepcalm.coi.client.effect.visual;

import dev.ua.ikeepcalm.coi.CoiLog;
import dev.ua.ikeepcalm.coi.client.effect.VisualEffect;
import dev.ua.ikeepcalm.coi.client.effect.visual.impact.ImpactRenderer;
import dev.ua.ikeepcalm.coi.client.effect.visual.impact.WorldImpact;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * MMORPG-style world-space spell impact VFX: a soft core flash, expanding
 * shockwave rings, radial light spikes and physical spark streaks composed
 * per style. Rendered as emissive translucent geometry at the impact point;
 * soft edges come from per-vertex alpha gradients (no textures needed).
 * <p>
 * The optional screen component (scope=screen/both) is a single subtle
 * accent-colored edge pulse — no full-screen frame flashing.
 */
public class ImpactFrameEffect implements VisualEffect {

    public static final String ID = "impact";
    private static final Identifier WHITE_TEXTURE = Identifier.fromNamespaceAndPath("coi-client", "textures/entity/white.png");
    /**
     * The screen-scope edge pulse never outlasts this, however long the world VFX runs.
     */
    private static final long MAX_SCREEN_PULSE_MS = 450;

    private static final List<WorldImpact> ACTIVE_IMPACTS = new ArrayList<>();
    private static boolean initialized = false;

    private ImpactParams activeParams;
    private long screenStartTime;

    public static void initializeWorldRenderer() {
        if (initialized) return;
        initialized = true;

        ClientTickEvents.END_CLIENT_TICK.register(client ->
                ACTIVE_IMPACTS.removeIf(WorldImpact::isFinished));

        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            if (ACTIVE_IMPACTS.isEmpty()) return;

            PoseStack poseStack = context.poseStack();
            Vec3 cameraPos = context.levelState().cameraRenderState.pos;
            SubmitNodeCollector collector = context.submitNodeCollector();

            for (WorldImpact impact : new ArrayList<>(ACTIVE_IMPACTS)) {
                Vec3 camLocal = cameraPos.subtract(impact.position());
                poseStack.pushPose();
                poseStack.translate(
                        impact.position().x - cameraPos.x,
                        impact.position().y - cameraPos.y,
                        impact.position().z - cameraPos.z
                );
                collector.order(900).submitCustomGeometry(
                        poseStack,
                        RenderTypes.entityTranslucentEmissive(WHITE_TEXTURE),
                        (pose, consumer) -> impact.render(pose, consumer, camLocal)
                );
                poseStack.popPose();
            }
        });
    }

    public static void clearWorldImpacts() {
        ACTIVE_IMPACTS.clear();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Impact Frame";
    }

    @Override
    public String getDefaultParams() {
        return "style=burst,scope=world,color=FFFFFF,accent=FF7A22,intensity=0.85,radius=2.0,duration=900";
    }

    @Override
    public void start(String params) {
        ImpactParams parsed = ImpactParams.parse(params);
        if (parsed.world) {
            parsed.resolveMissingPosition();
            if (parsed.position != null) {
                ACTIVE_IMPACTS.add(new WorldImpact(parsed.position, parsed.color, parsed.accent, parsed.intensity,
                        parsed.radius, parsed.duration, parsed.style, parsed.seed));
            } else {
                CoiLog.LOG.info("Impact frame has no world position; rendering screen scope only");
            }
        }

        activeParams = parsed.screen ? parsed : null;
        screenStartTime = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphicsExtractor ctx, int w, int h, float tickDelta) {
        if (activeParams == null) return;

        long elapsed = System.currentTimeMillis() - screenStartTime;
        long pulseDuration = Math.min(activeParams.duration, MAX_SCREEN_PULSE_MS);
        if (elapsed > pulseDuration) return;

        float fade = 1f - elapsed / (float) pulseDuration;
        fade *= fade;

        ImpactRenderer.screenPulse(ctx, w, h, activeParams.accent, activeParams.intensity, fade);
    }

    @Override
    public boolean isFinished() {
        return activeParams == null || System.currentTimeMillis() - screenStartTime > activeParams.duration;
    }

    @Override
    public void stop() {
        activeParams = null;
        clearWorldImpacts();
    }

    private static class ImpactParams {
        private Vec3 position;
        private String style = "burst";
        private boolean screen = false;
        private boolean world = true;
        private int color = 0xFFFFFF;
        private int accent = 0xFF7A22;
        private float intensity = 0.85f;
        private float radius = 2.0f;
        private long duration = 900;
        private long seed = System.nanoTime();

        private static ImpactParams parse(String params) {
            ImpactParams parsed = new ImpactParams();
            if (params == null || params.isBlank()) return parsed;

            Double x = null;
            Double y = null;
            Double z = null;

            for (String part : params.split(",")) {
                String[] kv = part.split("=", 2);
                if (kv.length != 2) continue;

                String key = kv[0].trim();
                String value = kv[1].trim();
                switch (key) {
                    case "style" -> parsed.style = value.toLowerCase();
                    case "scope" -> {
                        String scope = value.toLowerCase();
                        parsed.screen = "screen".equals(scope) || "both".equals(scope);
                        parsed.world = !"screen".equals(scope);
                    }
                    case "x" -> x = Double.parseDouble(value);
                    case "y" -> y = Double.parseDouble(value);
                    case "z" -> z = Double.parseDouble(value);
                    case "color" -> parsed.color = Integer.parseInt(value, 16);
                    case "accent" -> parsed.accent = Integer.parseInt(value, 16);
                    case "intensity" -> parsed.intensity = EffectPaint.clamp(Float.parseFloat(value), 0f, 1f);
                    case "radius" -> parsed.radius = Math.max(0.25f, Float.parseFloat(value));
                    case "duration" -> parsed.duration = Math.max(80, Long.parseLong(value));
                    case "seed" -> parsed.seed = Long.parseLong(value);
                    // "frames" is accepted and ignored for backwards compatibility
                }
            }

            if (x != null && y != null && z != null) {
                parsed.position = new Vec3(x, y, z);
            }

            return parsed;
        }

        private void resolveMissingPosition() {
            if (position != null) return;

            Minecraft client = Minecraft.getInstance();
            if (client.hitResult != null && client.hitResult.getType() != HitResult.Type.MISS) {
                position = client.hitResult.getLocation();
                return;
            }

            Entity camera = client.getCameraEntity();
            if (camera != null) {
                position = camera.getEyePosition().add(camera.getLookAngle().scale(4.0));
            }
        }
    }
}
