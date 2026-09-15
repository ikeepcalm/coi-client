package dev.ua.ikeepcalm.coi.client.form;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * One pathway's mythical creature form.
 *
 * <p>A form either replaces the player outright — {@link #render} draws procedural geometry and the
 * vanilla render is cancelled — or, when {@link #partialForm} is non-null, contributes a baked model
 * for the lower body while the player's own head, torso and arms keep rendering.
 */
public interface MythicalCreatureForm extends FormPrimitives {

    String getPathwayName();

    void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer);

    /**
     * Non-null when this form renders a baked model beside the vanilla player instead of
     * replacing it outright. When present, {@link #render} is not called.
     */
    default PartialFormSpec partialForm() {
        return null;
    }

}
