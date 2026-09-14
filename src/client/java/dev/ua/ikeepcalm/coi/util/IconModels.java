package dev.ua.ikeepcalm.coi.util;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Answers "does the loaded resource pack actually define this item model?"
 * for the per-ability icon keys the server sends.
 * <p>
 * Without the check every slot with an unknown icon would blit a missing
 * texture every frame; with it, {@link AbilityIcons} silently falls back to
 * the bundled category/tier icon. The answer only changes on a resource
 * reload, so it is cached per icon string and cleared from the reload
 * listener (and on disconnect, since the next server may key icons
 * differently).
 */
public final class IconModels {

    private static final Map<String, Boolean> CACHE = new ConcurrentHashMap<>();

    private IconModels() {
    }

    /**
     * @param icon a full item-model id such as {@code circleofimagination:sun/holylight}
     */
    public static boolean exists(String icon) {
        if (icon == null || icon.isEmpty()) return false;
        Boolean cached = CACHE.get(icon);
        if (cached != null) return cached;
        boolean present = lookup(icon);
        CACHE.put(icon, present);
        return present;
    }

    private static boolean lookup(String icon) {
        Identifier model = Identifier.tryParse(icon);
        if (model == null) return false;
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getResourceManager() == null) return false;
        Identifier path = Identifier.fromNamespaceAndPath(model.getNamespace(), "items/" + model.getPath() + ".json");
        try {
            return client.getResourceManager().getResource(path).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
