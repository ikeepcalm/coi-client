package dev.ua.ikeepcalm.coi.client.mixin;

import dev.ua.ikeepcalm.coi.client.screen.title.TitleTakeover;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LogoRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the vanilla Minecraft logo while the title takeover owns the
 * screen; the mod draws its own wordmark as part of the background.
 * <p>
 * {@code TitleScreen.logoRenderer} is private and final, so there is no seam on
 * the screen itself — the shortest honest hook is the renderer's own entry
 * point. Only the four-argument overload is injected: the three-argument one
 * delegates straight to it, so one injection covers both call shapes and there
 * is no way for a future caller to slip past. The screen check matters because
 * the same renderer draws the logo on the realms and pause screens.
 */
@Mixin(LogoRenderer.class)
public class LogoRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IFI)V",
            at = @At("HEAD"), cancellable = true)
    private void coi$suppressVanillaLogo(GuiGraphicsExtractor ctx, int screenWidth, float alpha,
                                         int heightOffset, CallbackInfo ci) {
        if (TitleTakeover.onTitleScreen()) ci.cancel();
    }
}
