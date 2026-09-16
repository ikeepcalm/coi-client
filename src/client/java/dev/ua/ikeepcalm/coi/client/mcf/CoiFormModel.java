package dev.ua.ikeepcalm.coi.client.mcf;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Matrix4f;

/**
 * Optional extra contract a baked partial-form model can implement: exposes the motion of the bone
 * the player's upper body rides on — the <em>carrier</em> — so the vanilla player can be moved by
 * that exact same transform and the two halves read as one body.
 *
 * <p>Angle-only mimicry isn't enough for this. A rig's carrier bone rotates around whatever pivot
 * the authoring tool gave it, which is generally nowhere near the player's own waist (the Visionary
 * rig's is over a block off to the side), and animations move the carrier's <em>position</em> as
 * well as its rotation. Copying just the angle onto the player's body leaves the player rotating
 * around a different point than the dragon does, which reads as the two halves shearing apart even
 * though both are "tilted the same amount". Handing out the full rigid transform instead lets the
 * caller reproduce the motion exactly, pivot and all.
 */
public interface CoiFormModel {

    /**
     * Rigid transform — in this model's own space, block units (i.e. {@link ModelPart}
     * coordinates divided by 16) — that the carrier bone has moved through relative to the pose it
     * was baked in. Identity means the carrier is sitting exactly where it was baked, which is the
     * case for any animation that leaves it alone.
     */
    default Matrix4f carrierDelta() {
        return new Matrix4f();
    }

    /**
     * {@link #carrierDelta()} for a carrier reached by walking {@code chain} down from the model
     * root (root-most bone first, the carrier itself last). Every bone along the way contributes
     * its current pose, so an animation that moves a <em>parent</em> of the carrier is picked up
     * without having to special-case it.
     *
     * <p>Part scaling is deliberately ignored: a carrier that changes scale would scale the player
     * along with it, which is never what a "ride on this bone" relationship wants.
     */
    static Matrix4f carrierDelta(ModelPart... chain) {
        Matrix4f current = new Matrix4f();
        Matrix4f rest = new Matrix4f();
        for (ModelPart part : chain) {
            translateAndRotate(current, part.x, part.y, part.z, part.xRot, part.yRot, part.zRot);
            PartPose initial = part.getInitialPose();
            translateAndRotate(rest, initial.x(), initial.y(), initial.z(), initial.xRot(), initial.yRot(), initial.zRot());
        }
        // current maps carrier-local -> model space now, rest did so at bake time, so
        // current * rest^-1 is what model-space points glued to the carrier have moved by.
        return current.mul(rest.invertAffine());
    }

    /**
     * Mirrors {@link ModelPart#translateAndRotate}: translate, then Z, then Y, then X.
     */
    private static void translateAndRotate(Matrix4f matrix, float x, float y, float z, float xRot, float yRot, float zRot) {
        matrix.translate(x / 16.0F, y / 16.0F, z / 16.0F);
        if (zRot != 0.0F) {
            matrix.rotateZ(zRot);
        }
        if (yRot != 0.0F) {
            matrix.rotateY(yRot);
        }
        if (xRot != 0.0F) {
            matrix.rotateX(xRot);
        }
    }
}
