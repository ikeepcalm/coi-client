package dev.ua.ikeepcalm.coi.client.mixin;

import dev.ua.ikeepcalm.coi.client.screen.title.TitleTakeover;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SplashRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Redraws the splash where vanilla would have drawn it. Vanilla pins the line
 * to {@code (width / 2 + 123, 69)}, which is inside the mod's much larger
 * wordmark, so the line is drawn from here instead — at vanilla's point in the
 * layer order, but off the wordmark's own corner.
 */
@Mixin(SplashRenderer.class)
public class SplashRendererMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void coi$relocateSplash(GuiGraphicsExtractor ctx, int screenWidth, Font font,
                                    float alpha, CallbackInfo ci) {
        if (!TitleTakeover.onTitleScreen()) return;
        TitleTakeover.drawSplash(ctx, font, screenWidth, ctx.guiHeight());
        ci.cancel();
    }
}
