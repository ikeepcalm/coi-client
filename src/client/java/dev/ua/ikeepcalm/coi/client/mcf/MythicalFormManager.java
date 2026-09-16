package dev.ua.ikeepcalm.coi.client.mcf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.effects.EffectManager;
import dev.ua.ikeepcalm.coi.client.effects.impl.ImpactFrameEffect;
import dev.ua.ikeepcalm.coi.client.mcf.forms.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MythicalFormManager {

    private static final Map<String, String> uuidForms = new ConcurrentHashMap<>();
    private static final Map<String, MythicalCreatureForm> REGISTERED_FORMS = new ConcurrentHashMap<>();

    static {
        register(new FoolForm());
        register(new DoorForm());
        register(new ErrorForm());
        register(new TowerForm());
        register(new VisionaryForm());
        register(new SunForm());
        register(new TyrantForm());
        register(new HangedForm());
        register(new DarknessForm());
        register(new DeathForm());
        register(new GiantForm());
        register(new PriestForm());
        register(new DemonessForm());
        register(new ParagonForm());
        register(new HermitForm());
        register(new FortuneForm());
        register(new ChainedForm());
        register(new AbyssForm());
        register(new JusticiarForm());
        register(new EmperorForm());
    }

    public static void register(MythicalCreatureForm form) {
        String name = form.getPathwayName().toLowerCase();
        REGISTERED_FORMS.put(name, form);

        // Also register normalized variants for robustness
        String spaceRemoved = name.replace(" ", "");
        String underscoreRemoved = name.replace("_", "");
        String spaceToUnderscore = name.replace(" ", "_");
        String underscoreToSpace = name.replace("_", " ");

        REGISTERED_FORMS.put(spaceRemoved, form);
        REGISTERED_FORMS.put(underscoreRemoved, form);
        REGISTERED_FORMS.put(spaceToUnderscore, form);
        REGISTERED_FORMS.put(underscoreToSpace, form);
    }

    public static void handlePacket(String targetUuid, String params) {
        if (params == null || params.isBlank()) {
            uuidForms.remove(targetUuid);
            return;
        }
        String[] parts = params.split(":");
        if (parts.length < 3) {
            uuidForms.remove(targetUuid);
            return;
        }
        String pathwayName = parts[0].toLowerCase();
        String action = parts[2].toLowerCase();
        String previous = uuidForms.get(targetUuid);
        if ("stop".equals(action)) {
            uuidForms.remove(targetUuid);
            if (previous != null) {
                triggerTransformVfx(targetUuid, previous, false);
            }
        } else {
            uuidForms.put(targetUuid, pathwayName);
            if (!pathwayName.equals(previous)) {
                triggerTransformVfx(targetUuid, pathwayName, true);
            }
        }
    }

    /**
     * World-space burst at the transforming player, plus a brief tinted
     * screen flash if it's the local player. Reverting plays a softer ripple.
     */
    private static void triggerTransformVfx(String targetUuid, String pathway, boolean starting) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        for (AbstractClientPlayer player : client.level.players()) {
            if (!player.getUUID().toString().equals(targetUuid)) continue;

            Vec3 pos = player.position();
            String hex = String.format("%06X", formAccentColor(pathway));
            String params = String.format(Locale.ROOT,
                    "style=%s,scope=world,x=%.2f,y=%.2f,z=%.2f,color=FFFFFF,accent=%s,intensity=0.9,radius=1.7,duration=850",
                    starting ? "burst" : "ripple", pos.x, pos.y + 0.1, pos.z, hex);
            EffectManager.trigger(ImpactFrameEffect.ID, params);

            if (player == client.player) {
                EffectManager.trigger("flash", "color=" + hex + ",intensity=0.3,duration=350");
            }
            return;
        }
    }

    private static int formAccentColor(String pathway) {
        return switch (pathway.toLowerCase().replace("_", " ")) {
            case "fool" -> 0xB347CC;
            case "door" -> 0x5B7FE6;
            case "sun" -> 0xFFE55C;
            case "tyrant" -> 0x4AA3FF;
            case "demoness" -> 0xB22222;
            case "priest" -> 0xFF6B35;
            case "error" -> 0x999999;
            case "tower" -> 0x7788AA;
            case "visionary" -> 0x44CCBB;
            case "hanged" -> 0x3A6E4F;
            case "darkness" -> 0x4A1A6E;
            case "death" -> 0xC8D0E8;
            case "giant" -> 0xC08840;
            case "paragon" -> 0xE8E8FF;
            case "hermit" -> 0x8855CC;
            case "fortune" -> 0xFFD700;
            case "chained" -> 0x666677;
            case "abyss" -> 0x551133;
            case "justiciar" -> 0xEEDD88;
            case "emperor" -> 0xDD9922;
            default -> 0xCCCCFF;
        };
    }

    public static boolean isTransformed(AbstractClientPlayer player) {
        if (player == null) return false;
        return uuidForms.containsKey(player.getUUID().toString());
    }

    public static String getForm(String playerUuid) {
        return uuidForms.get(playerUuid);
    }

    public static MythicalCreatureForm getRegisteredForm(String pathway) {
        return pathway == null ? null : REGISTERED_FORMS.get(pathway.toLowerCase());
    }

    /**
     * Distinct partial-form specs across all registered forms, keyed by {@link PartialFormSpec#layer()}.
     * {@link #register(MythicalCreatureForm)} stores each form under several normalized key
     * variants, so this dedupes by layer identity rather than iterating the raw map.
     */
    public static Map<ModelLayerLocation, PartialFormSpec> getPartialForms() {
        Map<ModelLayerLocation, PartialFormSpec> result = new HashMap<>();
        for (MythicalCreatureForm form : new HashSet<>(REGISTERED_FORMS.values())) {
            PartialFormSpec spec = form.partialForm();
            if (spec != null) {
                result.put(spec.layer(), spec);
            }
        }
        return result;
    }

    public static java.util.List<String> getRegisteredPathwayNames() {
        return REGISTERED_FORMS.values().stream()
                .map(MythicalCreatureForm::getPathwayName)
                .distinct()
                .sorted()
                .toList();
    }

    public static void clearAll() {
        uuidForms.clear();
    }

    public static void renderFormSubmit(String pathway, AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        MythicalCreatureForm form = REGISTERED_FORMS.get(pathway.toLowerCase());
        if (form != null) {
            form.render(state, pose, consumer);
        }
    }
}
