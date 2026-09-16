package dev.ua.ikeepcalm.coi.mixin;

import dev.ua.ikeepcalm.coi.client.screen.TitleScreenHaunt;
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

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void coi$renderHaunt(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        TitleScreenHaunt.render(ctx, this.width, this.height);
    }
}
