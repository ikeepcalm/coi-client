package dev.ua.ikeepcalm.coi.client.mcf.forms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public class ErrorForm implements MythicalCreatureForm {

    @Override
    public String getPathwayName() {
        return "Error";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;

        // Body composed of stacked independent clock-dial segments (Worms of Time)
        // Clocks face the viewer (X-Y plane, front = -Z direction)

        // Leg clocks
        drawClockDial(pose, consumer, 0.20f, 0.48f, 0.0f, 0.16f, time * 0.07f, light, 1);
        drawClockDial(pose, consumer, -0.20f, 0.48f, 0.0f, 0.16f, -time * 0.065f, light, 2);
        // Lower torso
        drawClockDial(pose, consumer, 0.0f, 1.25f, 0.0f, 0.34f, time * 0.042f, light, 3);
        // Upper torso (counter-rotating)
        drawClockDial(pose, consumer, 0.0f, 1.72f, 0.0f, 0.30f, -time * 0.055f, light, 4);
        // Arm clocks
        drawClockDial(pose, consumer, 0.52f, 1.5f, 0.0f, 0.20f, time * 0.095f, light, 5);
        drawClockDial(pose, consumer, -0.52f, 1.5f, 0.0f, 0.20f, -time * 0.085f, light, 6);
        // Head clock
        drawClockDial(pose, consumer, 0.0f, 2.28f, 0.0f, 0.25f, time * 0.032f, light, 7);

        // Ring of 12 faint doors of light tightly connected around the mid-body
        int doorCount = 12;
        float ringRadius = 0.88f;
        float ringY = 1.5f + (float) Math.sin(time * 0.038f) * 0.1f;
        for (int i = 0; i < doorCount; i++) {
            float angle = (float) (i * (Math.PI * 2.0 / doorCount)) + time * 0.012f;
            float dx = (float) Math.cos(angle) * ringRadius;
            float dz = (float) Math.sin(angle) * ringRadius;
            float dw = 0.068f, dh = 0.125f;
            drawBox(pose, consumer, dx - dw, ringY - dh, dz - 0.012f, dx + dw, ringY + dh, dz + 0.012f, 0.18f, 0.52f, 0.85f, 0.28f, light);
            drawBox(pose, consumer, dx - 0.014f, ringY - 0.014f, dz - 0.016f, dx + 0.014f, ringY + 0.014f, dz + 0.016f, 0.85f, 0.94f, 1.0f, 0.72f, light);
        }
    }

    private void drawClockDial(PoseStack.Pose pose, VertexConsumer consumer,
                               float cx, float cy, float cz, float radius, float angle, int light, int seed) {
        java.util.Random rand = new java.util.Random(seed * 9991L);

        // Central hub
        float hubSize = 0.055f;
        drawBox(pose, consumer, cx - hubSize, cy - hubSize, cz - hubSize, cx + hubSize, cy + hubSize, cz + hubSize, 0.32f, 0.62f, 0.44f, 0.82f, light);

        // Clock bezel ring (thin border at the rim, in X-Y plane)
        int bezelSegs = 10;
        for (int b = 0; b < bezelSegs; b++) {
            float bA = (float) (b * Math.PI * 2.0 / bezelSegs);
            float bx = cx + (float) Math.cos(bA) * radius;
            float by = cy + (float) Math.sin(bA) * radius;
            float bSize = 0.025f;
            drawBox(pose, consumer, bx - bSize, by - bSize, cz - bSize, bx + bSize, by + bSize, cz + bSize, 0.22f, 0.52f, 0.36f, 0.65f, light);
        }

        // Hour hand — points within X-Y plane (vertical clock face)
        float hLen = radius * 0.55f;
        float hAngle = angle * 0.18f;
        float hx = cx + (float) Math.cos(hAngle) * hLen;
        float hy = cy + (float) Math.sin(hAngle) * hLen;
        float hThick = 0.014f;
        drawBox(pose, consumer,
                Math.min(cx, hx) - hThick, Math.min(cy, hy) - hThick, cz - hThick,
                Math.max(cx, hx) + hThick, Math.max(cy, hy) + hThick, cz + hThick,
                0.3f, 0.6f, 0.4f, 0.87f, light);

        // Minute hand (longer, faster)
        float mLen = radius * 0.88f;
        float mAngle = angle * 2.4f;
        float mx = cx + (float) Math.cos(mAngle) * mLen;
        float my = cy + (float) Math.sin(mAngle) * mLen;
        float mThick = 0.010f;
        drawBox(pose, consumer,
                Math.min(cx, mx) - mThick, Math.min(cy, my) - mThick, cz - mThick,
                Math.max(cx, mx) + mThick, Math.max(cy, my) + mThick, cz + mThick,
                0.2f, 0.5f, 0.32f, 0.87f, light);

        // Tick marks at different scales around the rim (in X-Y plane)
        int ticks = 8;
        for (int t = 0; t < ticks; t++) {
            float tickAngle = (float) (t * (Math.PI * 2.0 / ticks)) + angle * 0.04f;
            float tx = cx + (float) Math.cos(tickAngle) * radius;
            float ty = cy + (float) Math.sin(tickAngle) * radius;
            float tickSize = 0.022f;
            float tr = 0.18f + rand.nextFloat() * 0.3f;
            float tg = 0.58f + rand.nextFloat() * 0.3f;
            float tb = 0.28f + rand.nextFloat() * 0.3f;
            drawBox(pose, consumer, tx - tickSize, ty - tickSize, cz - tickSize, tx + tickSize, ty + tickSize, cz + tickSize, tr, tg, tb, 0.62f, light);
        }
    }
}
