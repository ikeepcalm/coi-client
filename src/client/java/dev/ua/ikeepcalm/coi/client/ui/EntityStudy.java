package dev.ua.ikeepcalm.coi.client.ui;

import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.duck.AvatarRenderStateAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** An isolated inventory render state. Never changes the entity or the server's form registry. */
public class EntityStudy {
    private EntityStudy() {}

    /** Empty form means a human portrait; a nonempty form is a server-selected specimen. */
    public static void draw(GuiGraphicsExtractor g, int x, int y, int w, int h,
                            String form, float yaw, float zoom) {
        draw(g, x, y, w, h, form, yaw, zoom, null);
    }

    public static void draw(GuiGraphicsExtractor g, int x, int y, int w, int h,
                            String form, float yaw, float zoom, PortraitPose portrait) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || w < 16 || h < 16) return;
        var renderer = client.getEntityRenderDispatcher().getRenderer(client.player);
        if (!(renderer.createRenderState(client.player, 1f) instanceof AvatarRenderState state)) return;
        ((AvatarRenderStateAccessor) state).coi$setPreviewForm(form);
        ((AvatarRenderStateAccessor) state).coi$setPortraitPose(portrait);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        state.nameTag = null;
        state.pose = Pose.STANDING;
        state.isInvisible = false;
        state.isInvisibleToPlayer = false;
        state.isCrouching = false;
        state.isFallFlying = false;
        state.isPassenger = false;
        state.deathTime = 0;
        state.walkAnimationPos = 0;
        state.walkAnimationSpeed = 0;
        state.bodyRot = 180 + yaw;
        state.yRot = 0;
        state.xRot = 0;
        state.scale = 1;
        state.ageInTicks = HudConfig.getSettings().epilepsyMode ? 0 : state.ageInTicks;
        state.rightHandItemState.clear();
        state.leftHandItemState.clear();
        state.heldOnHead.clear();
        if (portrait != null) {
            state.isSpectator = false;
            state.isUsingItem = false;
            state.isVisuallySwimming = false;
            state.swimAmount = 0;
            state.attackTime = 0;
            state.isAutoSpinAttack = false;
            state.isUpsideDown = false;
            state.hasRedOverlay = false;
            state.rightArmPose = HumanoidModel.ArmPose.EMPTY;
            state.leftArmPose = HumanoidModel.ArmPose.EMPTY;
            state.headItem.clear();
            state.wornHeadType = null;
            // The dossier depicts the skin. Equipment remains untouched on the actual player.
            state.headEquipment = ItemStack.EMPTY;
            state.chestEquipment = ItemStack.EMPTY;
            state.legsEquipment = ItemStack.EMPTY;
            state.feetEquipment = ItemStack.EMPTY;
            state.showCape = false;
            state.arrowCount = 0;
            state.stingerCount = 0;
            state.parrotOnLeftShoulder = null;
            state.parrotOnRightShoulder = null;
            if (portrait.sword()) {
                client.getItemModelResolver().updateForLiving(state.rightHandItemState,
                        new ItemStack(Items.NETHERITE_SWORD),
                        ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, client.player);
            }
        }
        float horizontalExtent = form.isEmpty() ? 2.3f : 3.4f;
        float verticalExtent = form.isEmpty() ? 2.3f : 3.8f;
        if (portrait != null) {
            horizontalExtent = 2.5f;
            // Raised weapons extend beyond the head; reserve their space inside the clipping box.
            if (portrait.sword()) verticalExtent = 3f;
        }
        float scale = Math.min(w / horizontalExtent, h / verticalExtent) * zoom;
        g.entity(state, scale, new Vector3f(0, form.isEmpty() ? 0.95f : 1.3f, 0),
                new Quaternionf().rotationZ((float) Math.PI), null, x, y, x + w, y + h);
    }
}
