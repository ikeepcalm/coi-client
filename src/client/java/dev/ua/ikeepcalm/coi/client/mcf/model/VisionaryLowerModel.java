package dev.ua.ikeepcalm.coi.client.mcf.model;

import dev.ua.ikeepcalm.coi.client.mcf.CoiFormModel;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class VisionaryLowerModel extends Model<AvatarRenderState> implements CoiFormModel {

    private static final float WALK_EPSILON = 0.015F;

    private final ModelPart WHole;
    private final ModelPart BODY;
    private final ModelPart tail;
    private final ModelPart rWing;
    private final ModelPart lWing;
    private final ModelPart legBack;
    private final ModelPart UPPER;
    private final ModelPart LOWER;
    private final ModelPart FEET;
    private final ModelPart legBack2;
    private final ModelPart UPPER2;
    private final ModelPart LOWER2;
    private final ModelPart FEET2;
    private final ModelPart legFront;
    private final ModelPart upper3;
    private final ModelPart lower3;
    private final ModelPart FEET3;
    private final ModelPart legFront2;
    private final ModelPart Upper4;
    private final ModelPart lower4;
    private final ModelPart Feet4;

    private final KeyframeAnimation walkAnim;
    private final KeyframeAnimation idleAnim;

    public VisionaryLowerModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);

        this.WHole = root.getChild("WHole");
        this.BODY = this.WHole.getChild("BODY");
        this.tail = this.WHole.getChild("tail");
        this.rWing = this.WHole.getChild("R>WINGS1");
        this.lWing = this.WHole.getChild("L<WINGS2");
        this.legBack = this.WHole.getChild("L>LEGS2");
        this.UPPER = this.legBack.getChild("UPPER");
        this.LOWER = this.legBack.getChild("LOWER");
        this.FEET = this.legBack.getChild("FEET");
        this.legBack2 = this.WHole.getChild("L>LEGS3");
        this.UPPER2 = this.legBack2.getChild("UPPER2");
        this.LOWER2 = this.legBack2.getChild("LOWER2");
        this.FEET2 = this.legBack2.getChild("FEET2");
        this.legFront = this.WHole.getChild("R<ARM");
        this.upper3 = this.legFront.getChild("upper3");
        this.lower3 = this.upper3.getChild("lower3");
        this.FEET3 = this.lower3.getChild("FEET3");
        this.legFront2 = this.WHole.getChild("L>ARM2");
        this.Upper4 = this.legFront2.getChild("Upper4");
        this.lower4 = this.Upper4.getChild("lower4");
        this.Feet4 = this.lower4.getChild("Feet4");
        this.walkAnim = VisionaryLowerAnimations.WALK.bake(root);
        this.idleAnim = VisionaryLowerAnimations.IDLE.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition WHole = partdefinition.addOrReplaceChild("WHole", CubeListBuilder.create(), PartPose.offset(192.0F, -203.0F, -180.0F));

        PartDefinition BODY = WHole.addOrReplaceChild("BODY", CubeListBuilder.create().texOffs(1016, 691).addBox(-186.0F, 70.1274F, 13.3137F, 29.0F, 25.0F, 28.0F, new CubeDeformation(0.0F))
                .texOffs(402, 1023).addBox(-145.0F, 70.1274F, -104.6863F, 19.0F, 25.0F, 28.0F, new CubeDeformation(0.0F))
                .texOffs(1046, 958).addBox(-218.0F, 70.1274F, -104.6863F, 11.0F, 25.0F, 28.0F, new CubeDeformation(0.0F))
                .texOffs(720, 1041).addBox(-221.0F, 70.1274F, -22.6863F, 14.0F, 25.0F, 28.0F, new CubeDeformation(0.0F))
                .texOffs(496, 1028).addBox(-145.0F, 70.1274F, -22.6863F, 19.0F, 25.0F, 28.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-213.0F, 62.1274F, -144.6863F, 82.0F, 39.0F, 168.0F, new CubeDeformation(0.0F))
                .texOffs(0, 207).addBox(-205.0F, 52.1274F, -138.6863F, 68.0F, 39.0F, 155.0F, new CubeDeformation(0.0F))
                .texOffs(0, 401).addBox(-205.0F, 69.1274F, -138.6863F, 68.0F, 39.0F, 155.0F, new CubeDeformation(0.0F))
                .texOffs(0, 704).addBox(-202.0F, 79.1274F, -98.6863F, 63.0F, 39.0F, 49.0F, new CubeDeformation(0.0F))
                .texOffs(224, 709).addBox(-202.0F, 80.1274F, -60.6863F, 63.0F, 39.0F, 49.0F, new CubeDeformation(0.0F))
                .texOffs(446, 207).addBox(-197.0F, 38.1274F, -129.6863F, 54.0F, 39.0F, 136.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-24.0F, 3.0F, 137.0F, -0.1309F, 0.0F, 0.0F));

        PartDefinition cube_r1 = BODY.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(1068, 115).addBox(-33.5508F, 62.9029F, 25.0F, 28.0F, 16.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-141.0F, -27.0F, 56.0F, 1.5708F, -1.4835F, -1.5708F));

        PartDefinition cube_r2 = BODY.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(1068, 88).addBox(-33.5508F, 62.9029F, 25.0F, 28.0F, 16.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-141.0F, -27.0F, 96.0F, 1.5708F, -1.4835F, -1.5708F));

        PartDefinition cube_r3 = BODY.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(1050, 249).addBox(-24.6863F, 62.1274F, 25.0F, 28.0F, 16.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-141.0F, -39.0F, -22.0F, 0.0F, -1.5708F, 0.0F));

        PartDefinition cube_r4 = BODY.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(1068, 142).addBox(-24.6863F, 62.1274F, 24.0F, 15.0F, 16.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-142.0F, -39.0F, 13.0F, 0.0F, -1.5708F, 0.0F));

        PartDefinition cube_r5 = BODY.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(1050, 222).addBox(-24.6863F, 62.1274F, 25.0F, 28.0F, 16.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-141.0F, -39.0F, -59.0F, 0.0F, -1.5708F, 0.0F));

        PartDefinition cube_r6 = BODY.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(1048, 423).addBox(-24.6863F, 62.1274F, 25.0F, 28.0F, 16.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-141.0F, -39.0F, -99.0F, 0.0F, -1.5708F, 0.0F));

        PartDefinition cube_r7 = BODY.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(878, 870).addBox(-197.0F, 62.9029F, -147.5508F, 54.0F, 39.0F, 49.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -19.0F, 163.0F, 0.0873F, 0.0F, 0.0F));

        PartDefinition cube_r8 = BODY.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(200, 885).addBox(-188.0F, 62.9029F, -147.5508F, 45.0F, 39.0F, 49.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -26.0F, 227.0F, 0.0873F, 0.0F, 0.0F));

        PartDefinition cube_r9 = BODY.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(0, 880).addBox(-194.0F, 62.9029F, -147.5508F, 51.0F, 39.0F, 49.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -21.0F, 205.0F, 0.0873F, 0.0F, 0.0F));

        PartDefinition cube_r10 = BODY.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(672, 870).addBox(-197.0F, 62.9029F, -147.5508F, 54.0F, 39.0F, 49.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -12.0F, 163.0F, 0.0873F, 0.0F, 0.0F));

        PartDefinition cube_r11 = BODY.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(826, 285).addBox(-197.0F, 62.9029F, -156.4492F, 54.0F, 39.0F, 49.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -16.0F, 152.0F, 0.1745F, 0.0F, 0.0F));

        PartDefinition cube_r12 = BODY.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(448, 709).addBox(-202.0F, 62.9029F, -147.5508F, 63.0F, 39.0F, 49.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.0F, 106.0F, 0.0873F, 0.0F, 0.0F));

        PartDefinition cube_r13 = BODY.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(950, 0).addBox(-20.6846F, -193.6407F, -51.4306F, 11.0F, 39.0F, 49.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 20.0F, -178.0F, 0.0082F, -1.1784F, -1.5655F));

        PartDefinition cube_r14 = BODY.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(948, 88).addBox(-150.0F, -43.4492F, -50.0971F, 11.0F, 39.0F, 49.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-50.0F, -3.0F, -203.0F, -1.4835F, 0.0F, 0.0F));

        PartDefinition cube_r15 = BODY.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(896, 738).addBox(-150.0F, -43.4492F, -50.0971F, 11.0F, 39.0F, 49.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -3.0F, -199.0F, -1.4835F, 0.0F, 0.0F));

        PartDefinition cube_r16 = BODY.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(448, 797).addBox(-202.0F, -43.4492F, -50.0971F, 63.0F, 39.0F, 49.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.0F, -178.0F, -1.4835F, 0.0F, 0.0F));

        PartDefinition cube_r17 = BODY.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(224, 797).addBox(-202.0F, -43.4492F, -50.0971F, 63.0F, 39.0F, 49.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 41.0F, -178.0F, -1.4835F, 0.0F, 0.0F));

        PartDefinition cube_r18 = BODY.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(0, 792).addBox(-202.0F, 12.0F, -63.6654F, 63.0F, 39.0F, 49.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 41.0F, -125.0F, -0.9163F, 0.0F, 0.0F));

        PartDefinition cube_r19 = BODY.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(672, 782).addBox(-202.0F, 39.1365F, -86.4357F, 63.0F, 39.0F, 49.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 27.0F, -89.0F, -0.5672F, 0.0F, 0.0F));

        PartDefinition cube_r20 = BODY.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(726, 0).addBox(-202.0F, 56.8486F, -117.1139F, 63.0F, 39.0F, 49.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.0F, -54.0F, -0.2182F, 0.0F, 0.0F));

        PartDefinition cube_r21 = BODY.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs(826, 197).addBox(-202.0F, 53.4434F, -108.8929F, 63.0F, 39.0F, 49.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 34.0F, -46.0F, -0.3054F, 0.0F, 0.0F));

        PartDefinition cube_r22 = BODY.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs(724, 109).addBox(-202.0F, 56.8486F, -117.1139F, 63.0F, 39.0F, 49.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 11.0F, -54.0F, -0.2182F, 0.0F, 0.0F));

        PartDefinition cube_r23 = BODY.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs(500, 109).addBox(-202.0F, 61.4504F, -134.2879F, 63.0F, 39.0F, 49.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 20.0F, 0.0F, -0.0436F, 0.0F, 0.0F));

        PartDefinition tail = WHole.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(-193.3589F, 55.2525F, 341.3383F));

        PartDefinition cube_r24 = tail.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs(194, 1064).addBox(-33.5508F, 62.9029F, 24.0F, 28.0F, 16.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(28.3589F, -66.2525F, -71.3383F, 1.5708F, -1.4835F, -1.5708F));

        PartDefinition cube_r25 = tail.addOrReplaceChild("cube_r25", CubeListBuilder.create().texOffs(1070, 49).addBox(-42.4492F, 62.9029F, 23.0F, 15.0F, 16.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(27.3589F, -72.2525F, -30.3383F, 1.5708F, -1.3963F, -1.5708F));

        PartDefinition cube_r26 = tail.addOrReplaceChild("cube_r26", CubeListBuilder.create().texOffs(1084, 911).addBox(-42.4492F, 62.9029F, 23.0F, 15.0F, 16.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(27.3589F, -72.2525F, -7.3383F, 1.5708F, -1.3963F, -1.5708F));

        PartDefinition cube_r27 = tail.addOrReplaceChild("cube_r27", CubeListBuilder.create().texOffs(1084, 1035).addBox(-42.4492F, 62.9029F, 23.0F, 15.0F, 16.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(27.3589F, -79.2525F, 25.6617F, 1.5708F, -1.3963F, -1.5708F));

        PartDefinition cube_r28 = tail.addOrReplaceChild("cube_r28", CubeListBuilder.create().texOffs(1084, 1062).addBox(-44.3736F, 63.0411F, 23.0F, 15.0F, 16.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(27.3589F, -88.2525F, 58.6617F, 1.5708F, -1.309F, -1.5708F));

        PartDefinition cube_r29 = tail.addOrReplaceChild("cube_r29", CubeListBuilder.create().texOffs(388, 885).addBox(-186.5734F, 65.1298F, -149.9878F, 42.0F, 39.0F, 49.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(164.3589F, -79.2525F, 50.6617F, 0.1486F, -0.0264F, 0.01F));

        PartDefinition cube_r30 = tail.addOrReplaceChild("cube_r30", CubeListBuilder.create().texOffs(886, 373).addBox(-180.5734F, 68.1185F, -154.5317F, 32.0F, 31.0F, 49.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(164.3589F, -90.2525F, 82.6617F, 0.1922F, -0.0264F, 0.01F));

        PartDefinition cube_r31 = tail.addOrReplaceChild("cube_r31", CubeListBuilder.create().texOffs(892, 553).addBox(-177.5734F, 68.1185F, -154.5317F, 24.0F, 31.0F, 48.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(164.3589F, -98.2525F, 123.6617F, 0.1922F, -0.0264F, 0.01F));

        PartDefinition cube_r32 = tail.addOrReplaceChild("cube_r32", CubeListBuilder.create().texOffs(570, 958).addBox(-174.5734F, 70.5016F, -163.5966F, 19.0F, 22.0F, 48.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(164.3589F, -119.2525F, 154.6617F, 0.2795F, -0.0264F, 0.01F));

        PartDefinition cube_r33 = tail.addOrReplaceChild("cube_r33", CubeListBuilder.create().texOffs(0, 968).addBox(-171.5734F, 71.5016F, -163.5966F, 13.0F, 18.0F, 48.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(164.3589F, -127.2525F, 182.6617F, 0.2795F, -0.0264F, 0.01F));

        PartDefinition cube_r34 = tail.addOrReplaceChild("cube_r34", CubeListBuilder.create().texOffs(1006, 632).addBox(-169.5734F, 73.5016F, -163.5966F, 7.0F, 11.0F, 48.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(164.3589F, -139.2525F, 224.6617F, 0.2795F, -0.0264F, 0.01F));

        PartDefinition rWing = WHole.addOrReplaceChild("R>WINGS1", CubeListBuilder.create(), PartPose.offset(-109.5597F, -73.6134F, 52.0108F));

        PartDefinition cube_r35 = rWing.addOrReplaceChild("cube_r35", CubeListBuilder.create().texOffs(444, 1076).addBox(-17.0F, -39.0F, -149.0F, 11.0F, 39.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-35.4403F, 110.6134F, -8.0108F, -1.6282F, 0.7574F, 0.2662F));

        PartDefinition cube_r36 = rWing.addOrReplaceChild("cube_r36", CubeListBuilder.create().texOffs(346, 1023).addBox(-17.0F, -76.0F, -149.0F, 11.0F, 76.0F, 17.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(21.5597F, 105.6134F, 50.9892F, -1.2355F, 0.7574F, 0.2662F));

        PartDefinition cube_r37 = rWing.addOrReplaceChild("cube_r37", CubeListBuilder.create().texOffs(402, 1076).addBox(-17.0F, -39.0F, -149.0F, 11.0F, 39.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5597F, 105.6134F, 30.9892F, -1.2355F, 0.7574F, 0.2662F));

        PartDefinition cube_r38 = rWing.addOrReplaceChild("cube_r38", CubeListBuilder.create().texOffs(122, 968).addBox(-17.0F, -77.0F, -153.0F, 11.0F, 86.0F, 25.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5597F, 115.6134F, 30.9892F, -1.3664F, 0.7574F, 0.2662F));

        PartDefinition cube_r39 = rWing.addOrReplaceChild("cube_r39", CubeListBuilder.create().texOffs(1070, 0).addBox(-17.0F, -39.0F, -149.0F, 11.0F, 39.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.5597F, 117.6134F, 36.9892F, -0.9301F, 0.7574F, 0.2662F));

        PartDefinition cube_r40 = rWing.addOrReplaceChild("cube_r40", CubeListBuilder.create().texOffs(516, 973).addBox(-17.0F, -39.0F, -149.0F, 11.0F, 39.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.4403F, 134.6134F, 16.9892F, -0.9301F, 0.7574F, 0.2662F));

        PartDefinition cube_r41 = rWing.addOrReplaceChild("cube_r41", CubeListBuilder.create().texOffs(590, 1028).addBox(-27.0F, -75.0F, -152.0F, 18.0F, 75.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-68.4403F, 160.6134F, -50.0108F, -1.7155F, 0.7574F, 0.2662F));

        PartDefinition cube_r42 = rWing.addOrReplaceChild("cube_r42", CubeListBuilder.create().texOffs(70, 1034).addBox(-17.0F, -75.0F, -152.0F, 11.0F, 75.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-90.4403F, 166.6134F, -62.0108F, -1.7155F, 0.7574F, 0.2662F));

        PartDefinition cube_r43 = rWing.addOrReplaceChild("cube_r43", CubeListBuilder.create().texOffs(1014, 1041).addBox(-17.0F, -39.0F, -152.0F, 11.0F, 39.0F, 24.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-69.4403F, 145.6134F, -46.0108F, -1.7155F, 0.7574F, 0.2662F));

        PartDefinition cube_r44 = rWing.addOrReplaceChild("cube_r44", CubeListBuilder.create().texOffs(276, 1023).addBox(-17.0F, -56.0F, -152.0F, 11.0F, 56.0F, 24.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-49.4403F, 146.6134F, -26.0108F, -1.7155F, 0.7574F, 0.2662F));

        PartDefinition cube_r45 = rWing.addOrReplaceChild("cube_r45", CubeListBuilder.create().texOffs(194, 973).addBox(-17.0F, -64.0F, -152.0F, 14.0F, 64.0F, 27.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-25.4403F, 116.6134F, 4.9892F, -1.7155F, 0.7574F, 0.2662F));

        PartDefinition cube_r46 = rWing.addOrReplaceChild("cube_r46", CubeListBuilder.create().texOffs(1018, 453).addBox(-17.0F, -60.0F, -152.0F, 11.0F, 60.0F, 24.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-37.4403F, 137.6134F, -12.0108F, -1.7155F, 0.7574F, 0.2662F));

        PartDefinition cube_r47 = rWing.addOrReplaceChild("cube_r47", CubeListBuilder.create().texOffs(650, 1041).addBox(-17.0F, -39.0F, -152.0F, 11.0F, 46.0F, 24.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-47.4403F, 116.6134F, -25.0108F, -1.7155F, 0.7574F, 0.2662F));

        PartDefinition cube_r48 = rWing.addOrReplaceChild("cube_r48", CubeListBuilder.create().texOffs(944, 1041).addBox(-17.0F, -39.0F, -152.0F, 11.0F, 39.0F, 24.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-62.4403F, 125.6134F, -40.0108F, -1.7155F, 0.7574F, 0.2662F));

        PartDefinition cube_r49 = rWing.addOrReplaceChild("cube_r49", CubeListBuilder.create().texOffs(874, 1041).addBox(-17.0F, -39.0F, -152.0F, 11.0F, 39.0F, 24.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-35.4403F, 144.6134F, -5.0108F, -1.1483F, 0.7574F, 0.2662F));

        PartDefinition cube_r50 = rWing.addOrReplaceChild("cube_r50", CubeListBuilder.create().texOffs(804, 1041).addBox(-17.0F, -39.0F, -152.0F, 11.0F, 39.0F, 24.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-63.4403F, 168.6134F, -29.0108F, -1.3228F, 0.7574F, 0.2662F));

        PartDefinition lWing = WHole.addOrReplaceChild("L<WINGS2", CubeListBuilder.create(), PartPose.offset(-282.842F, -113.999F, 60.0108F));

        PartDefinition cube_r51 = lWing.addOrReplaceChild("cube_r51", CubeListBuilder.create().texOffs(444, 1076).mirror().addBox(6.0F, -39.0F, -149.0F, 11.0F, 39.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(22.5831F, 131.7012F, -16.0108F, -1.6282F, -0.7574F, -0.2662F));

        PartDefinition cube_r52 = lWing.addOrReplaceChild("cube_r52", CubeListBuilder.create().texOffs(346, 1023).mirror().addBox(6.0F, -76.0F, -149.0F, 11.0F, 76.0F, 17.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-34.4169F, 126.7012F, 42.9892F, -1.2355F, -0.7574F, -0.2662F));

        PartDefinition cube_r53 = lWing.addOrReplaceChild("cube_r53", CubeListBuilder.create().texOffs(402, 1076).mirror().addBox(6.0F, -39.0F, -149.0F, 11.0F, 39.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-13.4169F, 126.7012F, 22.9892F, -1.2355F, -0.7574F, -0.2662F));

        PartDefinition cube_r54 = lWing.addOrReplaceChild("cube_r54", CubeListBuilder.create().texOffs(122, 968).mirror().addBox(6.0F, -77.0F, -153.0F, 11.0F, 86.0F, 25.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-13.4169F, 136.7012F, 22.9892F, -1.3664F, -0.7574F, -0.2662F));

        PartDefinition cube_r55 = lWing.addOrReplaceChild("cube_r55", CubeListBuilder.create().texOffs(1070, 0).mirror().addBox(6.0F, -39.0F, -149.0F, 11.0F, 39.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-23.4169F, 138.7012F, 28.9892F, -0.9301F, -0.7574F, -0.2662F));

        PartDefinition cube_r56 = lWing.addOrReplaceChild("cube_r56", CubeListBuilder.create().texOffs(516, 973).mirror().addBox(6.0F, -39.0F, -149.0F, 11.0F, 39.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-2.4169F, 155.7012F, 8.9892F, -0.9301F, -0.7574F, -0.2662F));

        PartDefinition cube_r57 = lWing.addOrReplaceChild("cube_r57", CubeListBuilder.create().texOffs(590, 1028).mirror().addBox(9.0F, -75.0F, -152.0F, 18.0F, 75.0F, 12.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(55.5831F, 181.7012F, -58.0108F, -1.7155F, -0.7574F, -0.2662F));

        PartDefinition cube_r58 = lWing.addOrReplaceChild("cube_r58", CubeListBuilder.create().texOffs(70, 1034).mirror().addBox(6.0F, -75.0F, -152.0F, 11.0F, 75.0F, 12.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(77.5831F, 187.7012F, -70.0108F, -1.7155F, -0.7574F, -0.2662F));

        PartDefinition cube_r59 = lWing.addOrReplaceChild("cube_r59", CubeListBuilder.create().texOffs(1014, 1041).mirror().addBox(6.0F, -39.0F, -152.0F, 11.0F, 39.0F, 24.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(56.5831F, 166.7012F, -54.0108F, -1.7155F, -0.7574F, -0.2662F));

        PartDefinition cube_r60 = lWing.addOrReplaceChild("cube_r60", CubeListBuilder.create().texOffs(276, 1023).mirror().addBox(6.0F, -56.0F, -152.0F, 11.0F, 56.0F, 24.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(36.5831F, 167.7012F, -34.0108F, -1.7155F, -0.7574F, -0.2662F));

        PartDefinition cube_r61 = lWing.addOrReplaceChild("cube_r61", CubeListBuilder.create().texOffs(194, 973).mirror().addBox(3.0F, -64.0F, -152.0F, 14.0F, 64.0F, 27.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(12.5831F, 137.7012F, -3.0108F, -1.7155F, -0.7574F, -0.2662F));

        PartDefinition cube_r62 = lWing.addOrReplaceChild("cube_r62", CubeListBuilder.create().texOffs(1018, 453).mirror().addBox(6.0F, -60.0F, -152.0F, 11.0F, 60.0F, 24.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(24.5831F, 158.7012F, -20.0108F, -1.7155F, -0.7574F, -0.2662F));

        PartDefinition cube_r63 = lWing.addOrReplaceChild("cube_r63", CubeListBuilder.create().texOffs(650, 1041).mirror().addBox(6.0F, -39.0F, -152.0F, 11.0F, 46.0F, 24.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(34.5831F, 137.7012F, -33.0108F, -1.7155F, -0.7574F, -0.2662F));

        PartDefinition cube_r64 = lWing.addOrReplaceChild("cube_r64", CubeListBuilder.create().texOffs(944, 1041).mirror().addBox(6.0F, -39.0F, -152.0F, 11.0F, 39.0F, 24.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(49.5831F, 146.7012F, -48.0108F, -1.7155F, -0.7574F, -0.2662F));

        PartDefinition cube_r65 = lWing.addOrReplaceChild("cube_r65", CubeListBuilder.create().texOffs(874, 1041).mirror().addBox(6.0F, -39.0F, -152.0F, 11.0F, 39.0F, 24.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(22.5831F, 165.7012F, -13.0108F, -1.1483F, -0.7574F, -0.2662F));

        PartDefinition cube_r66 = lWing.addOrReplaceChild("cube_r66", CubeListBuilder.create().texOffs(804, 1041).mirror().addBox(6.0F, -39.0F, -152.0F, 11.0F, 39.0F, 24.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(50.5831F, 189.7012F, -37.0108F, -1.3228F, -0.7574F, -0.2662F));

        PartDefinition legBack = WHole.addOrReplaceChild("L>LEGS2", CubeListBuilder.create(), PartPose.offsetAndRotation(-66.0F, -24.0F, 115.0F, 1.0908F, 0.0F, 0.0F));

        PartDefinition UPPER = legBack.addOrReplaceChild("UPPER", CubeListBuilder.create().texOffs(704, 958).mirror().addBox(71.0F, -9.3005F, -48.1584F, 29.0F, 55.0F, 28.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(1016, 744).mirror().addBox(71.0F, -9.3005F, -48.1584F, 29.0F, 25.0F, 28.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-156.0F, 27.2432F, -52.3336F));

        PartDefinition cube_r67 = UPPER.addOrReplaceChild("cube_r67", CubeListBuilder.create().texOffs(678, 682).mirror().addBox(71.0F, -32.6995F, -37.8416F, 29.0F, 22.0F, 78.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 37.0F, -49.0F, -3.1416F, 0.0F, 0.0F));

        PartDefinition cube_r68 = UPPER.addOrReplaceChild("cube_r68", CubeListBuilder.create().texOffs(678, 582).mirror().addBox(71.0F, -41.0416F, -43.9584F, 29.0F, 22.0F, 78.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 37.0F, -25.0F, 2.8362F, 0.0F, 0.0F));

        PartDefinition cube_r69 = UPPER.addOrReplaceChild("cube_r69", CubeListBuilder.create().texOffs(932, 958).mirror().addBox(71.0F, -54.4449F, -35.0F, 29.0F, 55.0F, 28.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 38.0F, -29.0F, 1.5272F, 0.0F, 0.0F));

        PartDefinition cube_r70 = UPPER.addOrReplaceChild("cube_r70", CubeListBuilder.create().texOffs(818, 958).mirror().addBox(71.0F, -57.4264F, -7.776F, 29.0F, 55.0F, 28.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 26.0F, -2.0F, 2.3562F, 0.0F, 0.0F));

        PartDefinition cube_r71 = UPPER.addOrReplaceChild("cube_r71", CubeListBuilder.create().texOffs(892, 632).mirror().addBox(71.0F, -17.6411F, -51.1941F, 29.0F, 78.0F, 28.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 14.0F, 0.2618F, 0.0F, 0.0F));

        PartDefinition cube_r72 = UPPER.addOrReplaceChild("cube_r72", CubeListBuilder.create().texOffs(886, 453).mirror().addBox(71.0F, -34.4831F, -60.9676F, 29.0F, 63.0F, 37.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 14.0F, -9.0F, 0.5236F, 0.0F, 0.0F));

        PartDefinition LOWER = legBack.addOrReplaceChild("LOWER", CubeListBuilder.create(), PartPose.offset(-156.0F, 38.0F, -29.0F));

        PartDefinition cube_r73 = LOWER.addOrReplaceChild("cube_r73", CubeListBuilder.create().texOffs(1036, 537).mirror().addBox(77.0F, -124.5822F, -33.0768F, 19.0F, 55.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 63.0F, 4.0F, 1.8326F, 0.0F, 0.0F));

        PartDefinition cube_r74 = LOWER.addOrReplaceChild("cube_r74", CubeListBuilder.create().texOffs(0, 1034).mirror().addBox(77.0F, -122.2791F, 19.672F, 19.0F, 55.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 53.0F, -5.0F, 2.3562F, 0.0F, 0.0F));

        PartDefinition cube_r75 = LOWER.addOrReplaceChild("cube_r75", CubeListBuilder.create().texOffs(672, 482).mirror().addBox(71.0F, -111.2357F, -107.0337F, 29.0F, 22.0F, 78.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, -24.0F, 1.6581F, 0.0F, 0.0F));

        PartDefinition cube_r76 = LOWER.addOrReplaceChild("cube_r76", CubeListBuilder.create().texOffs(672, 382).mirror().addBox(71.0F, -95.1365F, -133.5643F, 29.0F, 22.0F, 78.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.3526F, 0.0F, 0.0F));

        PartDefinition FEET = legBack.addOrReplaceChild("FEET", CubeListBuilder.create(), PartPose.offset(-164.0F, 113.0F, -52.0F));

        PartDefinition cube_r77 = FEET.addOrReplaceChild("cube_r77", CubeListBuilder.create().texOffs(1084, 868).mirror().addBox(32.8017F, -65.512F, -107.6536F, 6.0F, 27.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(57.0F, 0.0F, -36.0F, 0.6189F, -0.1423F, 0.2836F));

        PartDefinition cube_r78 = FEET.addOrReplaceChild("cube_r78", CubeListBuilder.create().texOffs(486, 1081).mirror().addBox(93.937F, -21.609F, -114.9997F, 6.0F, 27.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-18.0F, 0.0F, -36.0F, 0.6189F, 0.1423F, -0.2836F));

        PartDefinition cube_r79 = FEET.addOrReplaceChild("cube_r79", CubeListBuilder.create().texOffs(1050, 176).mirror().addBox(56.7121F, 41.6624F, -83.7614F, 6.0F, 19.0F, 27.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(31.0F, -21.0F, -56.0F, -0.2013F, 0.2255F, 0.0279F));

        PartDefinition cube_r80 = FEET.addOrReplaceChild("cube_r80", CubeListBuilder.create().texOffs(1048, 377).mirror().addBox(73.6895F, 51.7406F, -113.6451F, 6.0F, 19.0F, 27.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(7.0F, -21.0F, -56.0F, -0.2013F, -0.2255F, -0.0279F));

        PartDefinition cube_r81 = FEET.addOrReplaceChild("cube_r81", CubeListBuilder.create().texOffs(1048, 331).mirror().addBox(92.4184F, 51.7406F, -101.3673F, 6.0F, 19.0F, 27.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-11.0F, -28.0F, -46.0F, -0.1961F, -0.0116F, -0.0711F));

        PartDefinition cube_r82 = FEET.addOrReplaceChild("cube_r82", CubeListBuilder.create().texOffs(116, 1079).mirror().addBox(92.4184F, -37.0752F, -115.3006F, 6.0F, 27.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-9.0F, -10.0F, -37.0F, 0.6329F, -0.0116F, -0.0711F));

        PartDefinition cube_r83 = FEET.addOrReplaceChild("cube_r83", CubeListBuilder.create().texOffs(530, 1081).mirror().addBox(92.4184F, -37.0752F, -115.3006F, 6.0F, 27.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-6.0F, 25.0F, 8.0F, 0.6329F, -0.0116F, -0.0711F));

        PartDefinition cube_r84 = FEET.addOrReplaceChild("cube_r84", CubeListBuilder.create().texOffs(570, 885).mirror().addBox(64.4184F, -80.0232F, -100.4252F, 34.0F, 55.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(6.0F, -1.0F, -27.0F, 1.0692F, -0.0116F, -0.0711F));

        PartDefinition cube_r85 = FEET.addOrReplaceChild("cube_r85", CubeListBuilder.create().texOffs(1016, 797).mirror().addBox(77.0F, -108.5535F, -69.5048F, 30.0F, 55.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.4399F, 0.0F, 0.0F));

        PartDefinition legBack2 = WHole.addOrReplaceChild("L>LEGS3", CubeListBuilder.create(), PartPose.offsetAndRotation(-328.0F, -24.0F, 115.0F, 1.0908F, 0.0F, 0.0F));

        PartDefinition UPPER2 = legBack2.addOrReplaceChild("UPPER2", CubeListBuilder.create().texOffs(704, 958).addBox(-100.0F, -9.3005F, -48.1584F, 29.0F, 55.0F, 28.0F, new CubeDeformation(0.0F))
                .texOffs(1016, 744).addBox(-100.0F, -9.3005F, -48.1584F, 29.0F, 25.0F, 28.0F, new CubeDeformation(0.0F)), PartPose.offset(156.0F, 27.2432F, -52.3336F));

        PartDefinition cube_r86 = UPPER2.addOrReplaceChild("cube_r86", CubeListBuilder.create().texOffs(678, 682).addBox(-100.0F, -32.6995F, -37.8416F, 29.0F, 22.0F, 78.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 37.0F, -49.0F, -3.1416F, 0.0F, 0.0F));

        PartDefinition cube_r87 = UPPER2.addOrReplaceChild("cube_r87", CubeListBuilder.create().texOffs(678, 582).addBox(-100.0F, -41.0416F, -43.9584F, 29.0F, 22.0F, 78.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 37.0F, -25.0F, 2.8362F, 0.0F, 0.0F));

        PartDefinition cube_r88 = UPPER2.addOrReplaceChild("cube_r88", CubeListBuilder.create().texOffs(932, 958).addBox(-100.0F, -54.4449F, -35.0F, 29.0F, 55.0F, 28.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 38.0F, -29.0F, 1.5272F, 0.0F, 0.0F));

        PartDefinition cube_r89 = UPPER2.addOrReplaceChild("cube_r89", CubeListBuilder.create().texOffs(818, 958).addBox(-100.0F, -57.4264F, -7.776F, 29.0F, 55.0F, 28.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 26.0F, -2.0F, 2.3562F, 0.0F, 0.0F));

        PartDefinition cube_r90 = UPPER2.addOrReplaceChild("cube_r90", CubeListBuilder.create().texOffs(892, 632).addBox(-100.0F, -17.6411F, -51.1941F, 29.0F, 78.0F, 28.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 14.0F, 0.2618F, 0.0F, 0.0F));

        PartDefinition cube_r91 = UPPER2.addOrReplaceChild("cube_r91", CubeListBuilder.create().texOffs(886, 453).addBox(-100.0F, -34.4831F, -60.9676F, 29.0F, 63.0F, 37.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 14.0F, -9.0F, 0.5236F, 0.0F, 0.0F));

        PartDefinition LOWER2 = legBack2.addOrReplaceChild("LOWER2", CubeListBuilder.create(), PartPose.offset(156.0F, 38.0F, -29.0F));

        PartDefinition cube_r92 = LOWER2.addOrReplaceChild("cube_r92", CubeListBuilder.create().texOffs(1036, 537).addBox(-96.0F, -124.5822F, -33.0768F, 19.0F, 55.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 63.0F, 4.0F, 1.8326F, 0.0F, 0.0F));

        PartDefinition cube_r93 = LOWER2.addOrReplaceChild("cube_r93", CubeListBuilder.create().texOffs(0, 1034).addBox(-96.0F, -122.2791F, 19.672F, 19.0F, 55.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 53.0F, -5.0F, 2.3562F, 0.0F, 0.0F));

        PartDefinition cube_r94 = LOWER2.addOrReplaceChild("cube_r94", CubeListBuilder.create().texOffs(672, 482).addBox(-100.0F, -111.2357F, -107.0337F, 29.0F, 22.0F, 78.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -24.0F, 1.6581F, 0.0F, 0.0F));

        PartDefinition cube_r95 = LOWER2.addOrReplaceChild("cube_r95", CubeListBuilder.create().texOffs(672, 382).addBox(-100.0F, -95.1365F, -133.5643F, 29.0F, 22.0F, 78.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.3526F, 0.0F, 0.0F));

        PartDefinition FEET2 = legBack2.addOrReplaceChild("FEET2", CubeListBuilder.create(), PartPose.offset(164.0F, 113.0F, -52.0F));

        PartDefinition cube_r96 = FEET2.addOrReplaceChild("cube_r96", CubeListBuilder.create().texOffs(1084, 868).addBox(-38.8017F, -65.512F, -107.6536F, 6.0F, 27.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-57.0F, 0.0F, -36.0F, 0.6189F, 0.1423F, -0.2836F));

        PartDefinition cube_r97 = FEET2.addOrReplaceChild("cube_r97", CubeListBuilder.create().texOffs(486, 1081).addBox(-99.937F, -21.609F, -114.9997F, 6.0F, 27.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(18.0F, 0.0F, -36.0F, 0.6189F, -0.1423F, 0.2836F));

        PartDefinition cube_r98 = FEET2.addOrReplaceChild("cube_r98", CubeListBuilder.create().texOffs(1050, 176).addBox(-62.7121F, 41.6624F, -83.7614F, 6.0F, 19.0F, 27.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-31.0F, -21.0F, -56.0F, -0.2013F, -0.2255F, -0.0279F));

        PartDefinition cube_r99 = FEET2.addOrReplaceChild("cube_r99", CubeListBuilder.create().texOffs(1048, 377).addBox(-79.6895F, 51.7406F, -113.6451F, 6.0F, 19.0F, 27.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F, -21.0F, -56.0F, -0.2013F, 0.2255F, 0.0279F));

        PartDefinition cube_r100 = FEET2.addOrReplaceChild("cube_r100", CubeListBuilder.create().texOffs(1048, 331).addBox(-98.4184F, 51.7406F, -101.3673F, 6.0F, 19.0F, 27.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(11.0F, -28.0F, -46.0F, -0.1961F, 0.0116F, 0.0711F));

        PartDefinition cube_r101 = FEET2.addOrReplaceChild("cube_r101", CubeListBuilder.create().texOffs(116, 1079).addBox(-98.4184F, -37.0752F, -115.3006F, 6.0F, 27.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.0F, -10.0F, -37.0F, 0.6329F, 0.0116F, 0.0711F));

        PartDefinition cube_r102 = FEET2.addOrReplaceChild("cube_r102", CubeListBuilder.create().texOffs(530, 1081).addBox(-98.4184F, -37.0752F, -115.3006F, 6.0F, 27.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, 25.0F, 8.0F, 0.6329F, 0.0116F, 0.0711F));

        PartDefinition cube_r103 = FEET2.addOrReplaceChild("cube_r103", CubeListBuilder.create().texOffs(570, 885).addBox(-98.4184F, -80.0232F, -100.4252F, 34.0F, 55.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, -1.0F, -27.0F, 1.0692F, 0.0116F, 0.0711F));

        PartDefinition cube_r104 = FEET2.addOrReplaceChild("cube_r104", CubeListBuilder.create().texOffs(1016, 797).addBox(-107.0F, -108.5535F, -69.5048F, 30.0F, 55.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.4399F, 0.0F, 0.0F));

        PartDefinition legFront = WHole.addOrReplaceChild("R<ARM", CubeListBuilder.create(), PartPose.offset(-192.0F, 227.0F, 194.0F));

        PartDefinition upper3 = legFront.addOrReplaceChild("upper3", CubeListBuilder.create(), PartPose.offset(-50.0F, -29.0F, -108.0F));

        PartDefinition cube_r105 = upper3.addOrReplaceChild("cube_r105", CubeListBuilder.create().texOffs(446, 382).addBox(-11.0F, -24.0F, -152.0F, 28.0F, 24.0F, 85.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0082F, -1.1784F, -1.5655F));

        PartDefinition cube_r106 = upper3.addOrReplaceChild("cube_r106", CubeListBuilder.create().texOffs(0, 595).addBox(-11.0F, -24.0F, -152.0F, 28.0F, 24.0F, 85.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0063F, -1.0475F, -1.5634F));

        PartDefinition cube_r107 = upper3.addOrReplaceChild("cube_r107", CubeListBuilder.create().texOffs(500, 0).addBox(-11.0F, -24.0F, -152.0F, 28.0F, 24.0F, 85.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0055F, -0.9602F, -1.5624F));

        PartDefinition lower3 = upper3.addOrReplaceChild("lower3", CubeListBuilder.create(), PartPose.offset(-4.0F, -96.0F, 46.0F));

        PartDefinition cube_r108 = lower3.addOrReplaceChild("cube_r108", CubeListBuilder.create().texOffs(452, 600).addBox(-11.0F, -24.0F, -152.0F, 28.0F, 24.0F, 85.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 16.0F, 36.0F, 0.0034F, 0.3488F, -1.5568F));

        PartDefinition cube_r109 = lower3.addOrReplaceChild("cube_r109", CubeListBuilder.create().texOffs(226, 600).addBox(-11.0F, -24.0F, -152.0F, 28.0F, 24.0F, 85.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 32.0F, 12.0F, 0.0034F, 0.3488F, -1.5568F));

        PartDefinition cube_r110 = lower3.addOrReplaceChild("cube_r110", CubeListBuilder.create().texOffs(446, 491).addBox(-11.0F, -24.0F, -152.0F, 28.0F, 24.0F, 85.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0037F, 0.5669F, -1.5559F));

        PartDefinition FEET3 = lower3.addOrReplaceChild("FEET3", CubeListBuilder.create(), PartPose.offset(-1.0F, 5.0F, -64.0F));

        PartDefinition cube_r111 = FEET3.addOrReplaceChild("cube_r111", CubeListBuilder.create().texOffs(1046, 1011).addBox(-12.0F, -20.0F, -124.0F, 29.0F, 9.0F, 15.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-13.0F, -24.0F, -110.0F, -1.4006F, -1.3085F, 2.9643F));

        PartDefinition cube_r112 = FEET3.addOrReplaceChild("cube_r112", CubeListBuilder.create().texOffs(1036, 608).addBox(-17.0F, -20.0F, -124.0F, 29.0F, 9.0F, 15.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-14.0F, -24.0F, -110.0F, -1.4006F, 1.3085F, -2.9643F));

        PartDefinition cube_r113 = FEET3.addOrReplaceChild("cube_r113", CubeListBuilder.create().texOffs(896, 826).addBox(-17.0F, -20.0F, -124.0F, 29.0F, 9.0F, 15.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -24.0F, -107.0F, 0.0716F, 1.5267F, -1.4863F));

        PartDefinition cube_r114 = FEET3.addOrReplaceChild("cube_r114", CubeListBuilder.create().texOffs(726, 88).addBox(-17.0F, -20.0F, -124.0F, 39.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -19.0F, -46.0F, 0.0716F, 1.5267F, -1.4863F));

        PartDefinition cube_r115 = FEET3.addOrReplaceChild("cube_r115", CubeListBuilder.create().texOffs(1032, 285).addBox(-17.0F, -31.0F, -124.0F, 39.0F, 37.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -22.0F, -81.0F, 0.0716F, 1.5267F, -1.4863F));

        PartDefinition cube_r116 = FEET3.addOrReplaceChild("cube_r116", CubeListBuilder.create().texOffs(396, 973).addBox(-17.0F, -24.0F, -124.0F, 34.0F, 24.0F, 26.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -17.0F, -64.0F, 0.0241F, 1.4396F, -1.534F));

        PartDefinition cube_r117 = FEET3.addOrReplaceChild("cube_r117", CubeListBuilder.create().texOffs(276, 973).addBox(-17.0F, -24.0F, -124.0F, 34.0F, 24.0F, 26.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0052F, 0.916F, -1.5538F));

        PartDefinition legFront2 = WHole.addOrReplaceChild("L>ARM2", CubeListBuilder.create(), PartPose.offset(-115.0F, 227.0F, 191.0F));

        PartDefinition Upper4 = legFront2.addOrReplaceChild("Upper4", CubeListBuilder.create(), PartPose.offset(-25.4069F, -125.8469F, -158.5524F));

        PartDefinition cube_r118 = Upper4.addOrReplaceChild("cube_r118", CubeListBuilder.create().texOffs(0, 595).mirror().addBox(-17.0F, -24.0F, -152.0F, 28.0F, 24.0F, 85.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-10.5931F, 96.8469F, 50.5524F, 0.0063F, 1.0475F, 1.5634F));

        PartDefinition cube_r119 = Upper4.addOrReplaceChild("cube_r119", CubeListBuilder.create().texOffs(500, 0).mirror().addBox(-17.0F, -24.0F, -152.0F, 28.0F, 24.0F, 85.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-10.5931F, 96.8469F, 50.5524F, 0.0055F, 0.9602F, 1.5624F));

        PartDefinition cube_r120 = Upper4.addOrReplaceChild("cube_r120", CubeListBuilder.create().texOffs(446, 382).mirror().addBox(-17.0F, -24.0F, -152.0F, 28.0F, 24.0F, 85.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-10.5931F, 96.8469F, 50.5524F, 0.0082F, 1.1784F, 1.5655F));

        PartDefinition lower4 = Upper4.addOrReplaceChild("lower4", CubeListBuilder.create(), PartPose.offset(2.8226F, 47.3098F, -2.916F));

        PartDefinition cube_r121 = lower4.addOrReplaceChild("cube_r121", CubeListBuilder.create().texOffs(446, 491).mirror().addBox(-17.0F, -24.0F, -152.0F, 28.0F, 24.0F, 85.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-9.4157F, -22.4763F, 102.7001F, 0.0037F, -0.5669F, 1.5559F));

        PartDefinition cube_r122 = lower4.addOrReplaceChild("cube_r122", CubeListBuilder.create().texOffs(226, 600).mirror().addBox(-17.0F, -24.0F, -152.0F, 28.0F, 24.0F, 85.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-9.4157F, 9.5237F, 114.7001F, 0.0034F, -0.3488F, 1.5568F));

        PartDefinition cube_r123 = lower4.addOrReplaceChild("cube_r123", CubeListBuilder.create().texOffs(452, 600).mirror().addBox(-17.0F, -24.0F, -152.0F, 28.0F, 24.0F, 85.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-9.4157F, -6.4763F, 138.7001F, 0.0034F, -0.3488F, 1.5568F));

        PartDefinition Feet4 = lower4.addOrReplaceChild("Feet4", CubeListBuilder.create(), PartPose.offset(-8.4157F, -17.4763F, 38.7001F));

        PartDefinition cube_r124 = Feet4.addOrReplaceChild("cube_r124", CubeListBuilder.create().texOffs(1046, 1011).mirror().addBox(-17.0F, -20.0F, -124.0F, 29.0F, 9.0F, 15.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(13.0F, -24.0F, -110.0F, -1.4006F, 1.3085F, -2.9643F));

        PartDefinition cube_r125 = Feet4.addOrReplaceChild("cube_r125", CubeListBuilder.create().texOffs(1036, 608).mirror().addBox(-12.0F, -20.0F, -124.0F, 29.0F, 9.0F, 15.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(14.0F, -24.0F, -110.0F, -1.4006F, -1.3085F, 2.9643F));

        PartDefinition cube_r126 = Feet4.addOrReplaceChild("cube_r126", CubeListBuilder.create().texOffs(896, 826).mirror().addBox(-12.0F, -20.0F, -124.0F, 29.0F, 9.0F, 15.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-4.0F, -24.0F, -107.0F, 0.0716F, -1.5267F, 1.4863F));

        PartDefinition cube_r127 = Feet4.addOrReplaceChild("cube_r127", CubeListBuilder.create().texOffs(726, 88).mirror().addBox(-22.0F, -20.0F, -124.0F, 39.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, -19.0F, -46.0F, 0.0716F, -1.5267F, 1.4863F));

        PartDefinition cube_r128 = Feet4.addOrReplaceChild("cube_r128", CubeListBuilder.create().texOffs(1032, 285).mirror().addBox(-22.0F, -31.0F, -124.0F, 39.0F, 37.0F, 9.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, -22.0F, -81.0F, 0.0716F, -1.5267F, 1.4863F));

        PartDefinition cube_r129 = Feet4.addOrReplaceChild("cube_r129", CubeListBuilder.create().texOffs(396, 973).mirror().addBox(-17.0F, -24.0F, -124.0F, 34.0F, 24.0F, 26.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, -17.0F, -64.0F, 0.0241F, -1.4396F, 1.534F));

        PartDefinition cube_r130 = Feet4.addOrReplaceChild("cube_r130", CubeListBuilder.create().texOffs(276, 973).mirror().addBox(-17.0F, -24.0F, -124.0F, 34.0F, 24.0F, 26.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0052F, -0.916F, 1.5538F));

        return LayerDefinition.create(meshdefinition, 2050, 2050);
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        this.resetPose();
        float walkAmount = Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F);
        if (walkAmount > WALK_EPSILON) {
            this.walkAnim.applyWalk(state.walkAnimationPos, walkAmount, 1.0F, 1.0F);
        } else {
            this.idleAnim.apply((long) (state.ageInTicks * 50.0F), 1.0F);
        }
    }

    public ModelPart getBody() {
        return BODY;
    }

    /**
     * BODY is the torso the player's own upper body sits on, so it's the carrier. Only the walk
     * animation touches it — idle has no BODY channel — so this is identity whenever
     * {@link #setupAnim} took the idle branch, which is exactly when there's nothing to follow.
     */
    @Override
    public Matrix4f carrierDelta() {
        return CoiFormModel.carrierDelta(WHole, BODY);
    }
}
