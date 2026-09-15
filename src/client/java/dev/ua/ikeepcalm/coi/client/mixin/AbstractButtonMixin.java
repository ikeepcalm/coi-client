package dev.ua.ikeepcalm.coi.client.mixin;

import dev.ua.ikeepcalm.coi.client.screen.title.TitleButtons;
import dev.ua.ikeepcalm.coi.client.screen.title.TitleTakeover;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Re-chromes the title screen's buttons in the mod's dark/gold language.
 * <p>
 * {@code extractDefaultSprite} draws the button background and nothing else —
 * the label comes from {@code extractContents} afterwards — so cancelling it is
 * how the chrome is replaced without touching a single word vanilla writes.
 * <p>
 * Every button in the game runs through this method, which is why
 * {@link TitleTakeover#onTitleScreen()} checks the screen as well as the
 * setting. The widget is reached by cast rather than by extending
 * {@code AbstractWidget}: all this needs is four public getters, and a mixin
 * that declares a superclass has to keep a constructor honest for no gain.
 */
@Mixin(AbstractButton.class)
public class AbstractButtonMixin {

    @Inject(method = "extractDefaultSprite", at = @At("HEAD"), cancellable = true)
    private void coi$titleChrome(GuiGraphicsExtractor ctx, CallbackInfo ci) {
        if (!TitleTakeover.onTitleScreen()) return;
        AbstractWidget self = (AbstractWidget) (Object) this;
        TitleButtons.plate(ctx, self.getX(), self.getY(), self.getWidth(), self.getHeight(),
                self.isHoveredOrFocused(), self.isActive());
        ci.cancel();
    }
}
