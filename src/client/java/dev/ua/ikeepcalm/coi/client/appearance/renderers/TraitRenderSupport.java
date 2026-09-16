package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Shared textures, render types and color packing for the trait renderers. */
final class TraitRenderSupport {

    static final Identifier WHITE_TEXTURE =
            Identifier.fromNamespaceAndPath("coi-client", "textures/entity/white.png");
    static final RenderType TRANSLUCENT = RenderTypes.entityTranslucent(WHITE_TEXTURE);
    /** Full-bright light coordinate for luminous traits (red/glowing eyes, corrosive claws). */
    static final int FULL_BRIGHT = 0x00F000F0;

    private TraitRenderSupport() {
    }
}
