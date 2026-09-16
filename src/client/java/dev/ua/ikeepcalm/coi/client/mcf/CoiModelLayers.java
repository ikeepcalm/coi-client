package dev.ua.ikeepcalm.coi.client.mcf;

import dev.ua.ikeepcalm.coi.client.mcf.model.VisionaryLowerModel;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

public class CoiModelLayers {

    public static final ModelLayerLocation VISIONARY_LOWER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("coi-client", "visionary_lower"), "main");

    // hipRaise (PlayerRendererMixin) is an un-popped pushPose/translate at the top of submit(), so
    // it shifts the player's body AND (since PartialFormLayer inherits the same un-popped pose
    // stack) the dragon by the exact same absolute amount — it cancels out of the relationship
    // between them. That splits vertical tuning into two independent knobs:
    //   - hipRaise: how high the whole assembly floats off the ground. The player's hitbox bottom
    //               is always pinned to the true ground (nothing client-side can move that), so
    //               hipRaise must be at least the dragon's own scaled leg length or the legs have
    //               nowhere to go but through the floor — this is inherently a bigger number than
    //               it looks like it should be, and means the visual body sits well above the
    //               actual collision box, like riding an invisible mount. Third-person only, so
    //               this doesn't affect the wearer's own camera/eye height.
    //   - offsetY:  how the dragon sits relative to the player specifically (fixes player floating
    //               above/sinking below the dragon's back), unaffected by hipRaise
    // Sign note: hipRaise is applied to submit()'s outermost pose stack (pre-setupRotations), which
    // is empirically Y-up there, unlike the Y-down convention individual ModelPart offsets use —
    // positive hipRaise = up. Got this backwards once already; don't re-flip without re-testing.
    //
    // offsetX/Y/Z + hipRaise CONFIRMED correct as of this pass ("connection is perfect") — leave
    // these alone unless there's a reason to think one of them specifically regressed. They also
    // now feed PartialFormSpec#modelToPlayer, which is what converts the rig's carrier motion into
    // player space (PartialForms#carrierTransform), so changing one shifts the walk-cycle sway to
    // match rather than needing its own second round of tuning.
    public static final PartialFormSpec VISIONARY_LOWER_SPEC = new PartialFormSpec(
            VISIONARY_LOWER,
            Identifier.fromNamespaceAndPath("coi-client", "textures/entity/forms/visionary_lower.png"),
            VisionaryLowerModel::new,
            VisionaryLowerModel::createBodyLayer,
            0.12F,
            0.03F,
            2.6F,
            1.7F,
            1.2F
    );

    public static void register() {
        ModelLayerRegistry.registerModelLayer(VISIONARY_LOWER, VisionaryLowerModel::createBodyLayer);
    }
}
