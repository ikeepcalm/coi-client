package dev.ua.ikeepcalm.coi.client.mixin;

import dev.ua.ikeepcalm.coi.client.screen.AbilityBindingScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeybindsScreen.class)
public abstract class KeybindsScreenMixin extends Screen {

    protected KeybindsScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "initBody", at = @At("TAIL"))
    private void coi$addAbilityBindingButton(CallbackInfo ci) {
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.coi.ability_binding"),
                button -> {

                    MinecraftClient.getInstance().setScreen(new AbilityBindingScreen(this));
                }
        ).dimensions(
                this.width / 2 - 155, //155
                this.height - 50, //29
                150,
                20
        ).build());
    }
}
