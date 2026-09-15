package dev.ua.ikeepcalm.coi.client.mixin;

import dev.ua.ikeepcalm.coi.client.screen.TitleScreenHaunt;
import dev.ua.ikeepcalm.coi.client.screen.title.TitleTakeover;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks the main menu twice over: the title takeover replaces the panorama with its own scene, and
 * the haunting — which predates it and still runs when the takeover is off — swaps in a whispered
 * splash line and draws the corruption overlay last of all.
 * <p>
 * The order is the whole point. {@code extractBackground} runs before the screen's widgets, so the
 * void, the wheel and the wordmark all land under the buttons; the haunt's vignette and eyes go in
 * at {@code extractRenderState} TAIL, which is after everything.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Shadow
    private SplashRenderer splash;

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void coi$hauntSplash(CallbackInfo ci) {
        SplashRenderer haunted = TitleScreenHaunt.hauntedSplash();
        if (haunted != null) {
            this.splash = haunted;
        }
    }

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void coi$titleScene(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!TitleTakeover.active()) return;
        TitleTakeover.drawScene(ctx, this.width, this.height, mouseX, mouseY);
        ci.cancel();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void coi$renderHaunt(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        TitleScreenHaunt.render(ctx, this.width, this.height);
    }
}
