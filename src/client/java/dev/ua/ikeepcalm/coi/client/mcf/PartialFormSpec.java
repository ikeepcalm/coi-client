package dev.ua.ikeepcalm.coi.client.mcf;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Marks a {@link MythicalCreatureForm} as rendering a baked model alongside the vanilla player
 * model instead of replacing it outright. The player's own head/torso/arms keep rendering; only
 * the legs are hidden so the baked model can stand in for the character's lower body.
 *
 * @param bodyLayer supplies a fresh {@link LayerDefinition} to bake a standalone, non-textured
 *                  {@code ModelPart} tree from — used by {@link PartialForms} to build a query-only
 *                  instance (via {@code bodyLayer.get().bakeRoot()} + {@link #factory}) for reading
 *                  {@link CoiFormModel#carrierDelta()} independently of the real render instance
 *                  {@link PartialFormLayer} owns. A separate instance is needed because the real
 *                  one is only posed at draw time (geometry submission is deferred), which is well
 *                  after the point where the player's own body has to be transformed to match.
 * @param scale     uniform scale applied to the baked model before it's submitted
 * @param offsetX   blocks (final, already-scaled output space) the model is translated by on X to
 *                  recenter it under the player — corrects whatever arbitrary pivot the rig's
 *                  root bone happened to have in the authoring tool
 * @param offsetY   blocks the model is translated down (Y-down, so positive moves it toward the
 *                  ground) to bring its feet down to the player's feet
 * @param offsetZ   blocks the model is translated by on Z, same purpose as {@code offsetX}
 * @param hipRaise  blocks the vanilla upper body is translated *up* to sit on top of the baked
 *                  model; 0 when the model already fits inside the player's normal leg envelope.
 *                  Applied to {@code submit()}'s outermost pose stack, before {@code setupRotations}
 *                  — empirically that's world-relative Y-up space (positive = up), not the Y-down
 *                  convention individual ModelPart offsets use further down the pipeline. Also
 *                  shifts the baked model by the same amount (see PartialFormLayer/CoiModelLayers
 *                  notes), and being outside the model's own pose it carries the name tag up too,
 *                  which is what keeps the tag sitting above the raised head.
 */
public record PartialFormSpec(
        ModelLayerLocation layer,
        Identifier texture,
        Function<ModelPart, ? extends Model<AvatarRenderState>> factory,
        Supplier<LayerDefinition> bodyLayer,
        float scale,
        float offsetX,
        float offsetY,
        float offsetZ,
        float hipRaise
) {

    /**
     * The placement {@link PartialFormLayer} applies to the baked model, i.e. the map from this
     * model's own space into player-model space. {@link PartialForms#carrierTransform} needs it to
     * re-express a carrier motion measured in model space as one the player's body can be moved by.
     */
    public Matrix4f modelToPlayer() {
        return new Matrix4f()
                .translate(offsetX, offsetY, offsetZ)
                .scale(scale);
    }
}
