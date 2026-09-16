package dev.ua.ikeepcalm.coi.client.mcf;

import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared lookups for the partial mythical forms — see {@link PartialFormSpec}. Everything that
 * takes part in drawing one (the renderer mixin, the player-model mixin, the armor-layer mixin and
 * {@link PartialFormLayer} itself) has to answer the same two questions about a render state, so
 * they answer them here rather than four times over.
 */
public final class PartialForms {

    /**
     * One query-only baked model per spec, reused every frame. Kept apart from
     * {@link PartialFormLayer}'s render instance because that one is only posed at draw time, long
     * after the player's own body has to be transformed to match it. Baking is the expensive part,
     * so it's cached; {@code setupAnim} still runs once here per player per frame on top of the
     * draw-time one, which is cheap and — being a pure function of the render state — can't
     * disagree with it.
     */
    private static final Map<PartialFormSpec, Model<AvatarRenderState>> QUERY_MODELS = new ConcurrentHashMap<>();

    private static final Matrix4f IDENTITY = new Matrix4f();

    private PartialForms() {
    }

    /**
     * The form the given render state's player is currently transformed into, or null.
     */
    public static MythicalCreatureForm form(EntityRenderState state) {
        if (!(state instanceof AvatarRenderState avatarState)) {
            return null;
        }
        String playerUuid = ((AvatarRenderStateAccessor) avatarState).coi$getPlayerUuid();
        if (playerUuid == null) {
            return null;
        }
        return MythicalFormManager.getRegisteredForm(MythicalFormManager.getForm(playerUuid));
    }

    /**
     * The partial-form spec in effect for the given render state, or null for full forms/no form.
     */
    public static PartialFormSpec partial(EntityRenderState state) {
        MythicalCreatureForm form = form(state);
        return form == null ? null : form.partialForm();
    }

    /**
     * The transform to move the vanilla player by so it rides the baked model's carrier bone
     * rigidly, expressed in player-model space (blocks, the space {@code ModelPart} coordinates
     * live in once divided by 16). Null when the carrier hasn't moved from its baked pose and
     * there's consequently nothing to do — which is the case for the whole idle animation.
     *
     * <p>{@link CoiFormModel#carrierDelta()} is measured in the baked model's own space, so it has
     * to be conjugated by the placement that space is drawn through ({@code L}, from
     * {@link PartialFormSpec#modelToPlayer()}): {@code L * delta * L^-1}. That both scales the
     * carrier's translation down by the model's render scale and moves its rotation pivot to where
     * the player actually sees it, which is the whole point — the pivot is what angle-copying got
     * wrong. Rotation angles themselves pass through untouched, since a uniform scale can't change
     * an angle.
     */
    public static Matrix4f carrierTransform(PartialFormSpec spec, AvatarRenderState state) {
        Model<AvatarRenderState> query = QUERY_MODELS.computeIfAbsent(spec,
                s -> s.factory().apply(s.bodyLayer().get().bakeRoot()));
        query.setupAnim(state);
        if (!(query instanceof CoiFormModel formModel)) {
            return null;
        }
        Matrix4f delta = formModel.carrierDelta();
        if (delta.equals(IDENTITY, 1.0E-5F)) {
            return null;
        }
        Matrix4f placement = spec.modelToPlayer();
        Matrix4f carrier = new Matrix4f(placement).mul(delta);
        return carrier.mul(placement.invertAffine());
    }
}
