package dev.ua.ikeepcalm.coi.client.ui;

import dev.ua.ikeepcalm.coi.client.ability.Pathways;
import net.minecraft.client.model.player.PlayerModel;

/**
 * A pose carried by one GUI render state, never by a live player or a shared render context.
 */
public record PortraitPose(String pathway, int sequence, float lookYaw, float lookPitch) {
    public PortraitPose(String pathway, int sequence) {
        this(pathway, sequence, 0, 0);
    }

    public PortraitPose {
        pathway = Pathways.normalizePathway(pathway);
        sequence = Math.clamp(sequence, 0, 9);
        lookYaw = Math.clamp(lookYaw, -0.105f, 0.105f);
        lookPitch = Math.clamp(lookPitch, -0.07f, 0.07f);
    }

    private static float wave(float time, float speed) {
        return (float) Math.sin(time * speed);
    }

    private static void arms(PlayerModel model, float rightX, float rightZ, float leftX, float leftZ) {
        model.rightArm.xRot = rightX;
        model.rightArm.zRot = rightZ;
        model.leftArm.xRot = leftX;
        model.leftArm.zRot = leftZ;
    }

    public float ascension() {
        return (9 - sequence) / 9f;
    }

    public boolean sword() {
        return (pathway.equals("priest") || pathway.equals("giant")) && sequence <= 2;
    }

    public void apply(PlayerModel model, float ageInTicks) {
        float power = ascension();
        // EntityStudy freezes age at zero for reduced effects. No world clock or mutable pose state.
        float time = ageInTicks / 20f;
        float breath = wave(time, 0.65f) * (0.025f + power * 0.025f);
        float gesture = wave(time, 0.38f) * (0.045f + power * 0.045f);
        model.head.xRot = -0.05f - power * 0.12f;
        model.rightLeg.zRot = -0.025f - power * 0.055f;
        model.leftLeg.zRot = -model.rightLeg.zRot;
        switch (pathway) {
            case "priest" -> {
                // Victory: lifted weapon and an open hand addressing the battlefield.
                arms(model, sword() ? -2.55f + breath : -0.25f - power * 0.5f,
                        sword() ? -0.25f : 0.12f + power * 0.35f,
                        -0.18f - power * 0.35f + gesture, -0.18f - power * 0.7f);
                model.head.yRot = gesture * 0.65f;
            }
            case "fool" -> {
                // One hand conducts the marionettes while the other presents the stage.
                arms(model, -0.5f - power * 0.7f + gesture, 0.25f + power * 0.45f,
                        -0.25f - gesture * 0.5f, -0.16f - power * 0.5f);
                model.head.zRot = 0.08f + power * 0.06f + breath;
                model.leftLeg.xRot = -0.08f - power * 0.12f;
            }
            case "door" -> {
                // Unequal hands part the edge of a doorway.
                arms(model, -0.95f - power * 0.22f + gesture, 0.42f + power * 0.35f,
                        -0.55f - power * 0.32f - gesture, -0.3f - power * 0.35f);
                model.head.yRot = -0.14f + breath;
                model.rightArm.yRot = 0.12f + gesture;
            }
            case "sun" -> {
                // Right hand raised in blessing, the other held over the heart.
                arms(model, -1.85f - power * 0.3f + breath, -0.25f,
                        -0.92f, 0.38f + power * 0.12f);
                model.head.xRot = -0.12f - power * 0.18f;
                model.leftArm.yRot = 0.18f;
            }
            case "tyrant" -> {
                // Commanding the storm with a forward arm and a grounded stance.
                arms(model, -1.28f - power * 0.12f + gesture, 0.12f,
                        -0.12f + breath, -0.4f - power * 0.5f);
                model.rightArm.yRot = -0.16f;
                model.head.yRot = -0.15f;
                model.rightLeg.zRot = -0.14f;
                model.leftLeg.zRot = 0.14f;
            }
            case "demoness" -> {
                // A hand near the face, the opposite hand trailing an invisible thread.
                arms(model, -1.7f + gesture, -0.3f,
                        -0.1f + breath, -0.35f - power * 0.4f);
                model.head.zRot = -0.16f - breath;
                model.head.yRot = 0.18f;
                model.leftLeg.zRot = -0.055f;
                model.leftLeg.xRot = -0.13f;
            }
            case "error" -> {
                // Picking an unseen clock apart; hands drift in opposing rhythms.
                arms(model, -1.15f + gesture, -0.22f,
                        -0.7f - gesture, -0.22f - power * 0.18f);
                model.rightArm.yRot = -0.24f + breath;
                model.leftArm.yRot = 0.32f - gesture;
                model.head.zRot = -0.11f;
                model.head.yRot = gesture * 2;
            }
            case "tower" -> {
                // The scholar considers a point held between finger and eye.
                arms(model, -1.8f + breath, -0.28f,
                        -0.85f + gesture * 0.4f, 0.32f);
                model.head.xRot = 0.1f;
                model.head.yRot = -0.12f;
                model.rightArm.yRot = -0.2f;
            }
            case "visionary" -> {
                // Framing a thought, with a small continuous turn of the head.
                arms(model, -1.28f + breath, 0.28f + power * 0.18f,
                        -1.15f - breath, -0.24f - power * 0.16f);
                model.rightArm.yRot = -0.2f;
                model.leftArm.yRot = 0.2f;
                model.head.yRot = gesture * 1.5f;
                model.head.xRot = 0.08f;
            }
            case "hanged" -> {
                // Uneven sacrificial surrender rather than a symmetrical victory stance.
                arms(model, -0.15f + breath, 0.75f + power * 0.4f,
                        -0.45f - breath, -0.55f - power * 0.32f);
                model.head.xRot = 0.3f + power * 0.13f;
                model.head.zRot = 0.12f;
                model.rightLeg.xRot = 0.14f;
            }
            case "darkness" -> {
                // A quiet, inward rest; almost no movement beyond breathing.
                arms(model, -0.65f + breath * 0.4f, -0.2f,
                        -0.65f + breath * 0.4f, 0.2f);
                model.head.xRot = 0.2f + breath * 0.5f;
                model.head.zRot = -0.06f;
                model.rightLeg.zRot = -0.015f;
                model.leftLeg.zRot = 0.015f;
            }
            case "death" -> {
                // Beckoning with one hand while the other hangs still.
                arms(model, -0.9f - power * 0.24f + gesture, 0.15f,
                        0.08f, -0.08f);
                model.rightArm.yRot = -0.28f + breath;
                model.head.xRot = 0.1f;
                model.head.yRot = -0.2f;
            }
            case "giant" -> {
                // A sentinel's upright blade and braced off-hand, distinct from Priest's salute.
                arms(model, sword() ? -2.32f + breath * 0.4f : -0.6f, sword() ? -0.08f : 0.18f,
                        -1.08f + breath, 0.1f);
                model.leftArm.yRot = 0.25f;
                model.head.xRot = -0.08f;
                model.rightLeg.zRot = -0.15f;
                model.leftLeg.zRot = 0.15f;
                model.leftLeg.xRot = -0.13f;
            }
            case "paragon" -> {
                // Working a small mechanism between hands at different heights.
                arms(model, -0.98f + gesture, -0.13f,
                        -1.25f - gesture * 0.6f, 0.18f);
                model.rightArm.yRot = -0.25f;
                model.leftArm.yRot = 0.3f;
                model.head.xRot = 0.24f + breath;
                model.head.yRot = -0.08f;
            }
            case "hermit" -> {
                // Hands support an invisible open tome, with the gaze down at its pages.
                arms(model, -1.15f + breath, 0.05f,
                        -1.15f + breath, -0.05f);
                model.rightArm.yRot = -0.22f;
                model.leftArm.yRot = 0.22f;
                model.head.xRot = 0.28f + gesture * 0.4f;
                model.head.yRot = gesture;
            }
            case "fortune" -> {
                // Tossing luck upward from an open hand, keeping the other at the hip.
                arms(model, -1.25f + gesture * 1.5f, 0.42f,
                        -0.15f, 0.12f);
                model.head.xRot = -0.22f + breath;
                model.head.yRot = -0.22f;
                model.rightLeg.xRot = -0.1f;
            }
            case "chained" -> {
                // Stable wrist anchors let the foreground chain links meet the restrained hands.
                float strain = breath * 0.3f;
                arms(model, -0.08f, 1.15f + strain, -0.08f, -1.15f - strain);
                model.head.xRot = 0.18f - power * 0.14f;
                model.head.zRot = gesture * 0.4f;
                model.rightLeg.zRot = -0.11f;
                model.leftLeg.zRot = 0.11f;
            }
            case "abyss" -> {
                // Predatory reach, shoulders open and the head lowered toward the viewer.
                arms(model, -1.05f + gesture, 0.7f + power * 0.15f,
                        -1.35f - gesture, -0.62f - power * 0.15f);
                model.head.xRot = 0.22f;
                model.head.zRot = breath;
                model.rightLeg.xRot = 0.1f;
                model.leftLeg.xRot = -0.16f;
            }
            case "justiciar" -> {
                // One palm delivers judgment; the other remains straight at the side.
                arms(model, -1.5f + breath * 0.4f, 0.02f,
                        0, -0.06f);
                model.head.xRot = -0.02f;
                model.head.yRot = 0;
                model.rightLeg.zRot = -0.045f;
                model.leftLeg.zRot = 0.045f;
            }
            case "emperor" -> {
                // An imperious hand extends sideways while the other rests across the chest.
                arms(model, -0.65f + gesture * 0.45f, 0.85f + power * 0.18f,
                        -0.95f, 0.32f);
                model.head.xRot = -0.22f;
                model.head.yRot = -0.18f + breath;
                model.leftLeg.xRot = -0.07f;
            }
            case "moon" -> {
                // Cradling moonlight in one hand while tracing its arc with the other.
                arms(model, -1.45f + gesture, 0.5f,
                        -0.8f - breath, -0.12f);
                model.head.zRot = 0.13f;
                model.head.yRot = -0.2f;
                model.leftArm.yRot = 0.26f;
            }
            case "mother" -> {
                // Cupped hands tend a seedling below eye level.
                arms(model, -0.72f + breath, -0.08f,
                        -0.82f - breath, 0.08f);
                model.rightArm.yRot = -0.3f - gesture;
                model.leftArm.yRot = 0.3f + gesture;
                model.head.xRot = 0.25f;
                model.head.zRot = 0.05f;
            }
            case "patriarch" -> {
                // Broad protective embrace, rising and settling like a slow tide.
                arms(model, -0.55f + gesture * 0.6f, 0.75f + power * 0.2f,
                        -0.55f + gesture * 0.6f, -0.75f - power * 0.2f);
                model.head.xRot = -0.12f;
                model.head.yRot = breath;
                model.rightLeg.zRot = -0.12f;
                model.leftLeg.zRot = 0.12f;
            }
            case "sublunary" -> {
                // An oblique, drifting pose with one hand following an unseen orbit.
                arms(model, -1.08f + gesture, 0.38f,
                        0.22f - breath, -0.5f);
                model.head.zRot = -0.18f;
                model.head.yRot = 0.16f + gesture;
                model.leftLeg.xRot = -0.23f;
                model.leftLeg.zRot = 0.035f;
            }
            case "aeon" -> {
                // Holding two different moments apart; opposing slow hand movement.
                arms(model, -1.45f + gesture * 0.5f, 0.6f,
                        -0.5f - gesture * 0.5f, -0.6f);
                model.head.xRot = -0.1f;
                model.head.yRot = gesture * 0.75f;
                model.rightLeg.zRot = -0.03f;
                model.leftLeg.zRot = 0.03f;
            }
            default -> arms(model, -0.15f - power * 0.25f, 0.1f + power * 0.5f,
                    -0.15f - power * 0.25f, -0.1f - power * 0.5f);
        }
        // A small gaze offset adds to the authored pose without moving its body or limbs.
        model.head.yRot += lookYaw;
        model.head.xRot += lookPitch;
        // Skin overlays are children of these bones in 26.2 and inherit their transforms.
    }
}
