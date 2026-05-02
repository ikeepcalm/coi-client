package dev.ua.ikeepcalm.coi.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;

@Mixin(targets = "net.minecraft.client.gui.screens.LoadingOverlay$LogoTexture")
public class LoadingOverlayMixin {

    @Unique
    private static final String CUSTOM_LOGO_PATH = "/assets/coi-client/textures/gui/title/mojangstudios.png";

    @Inject(method = "loadContents", at = @At("HEAD"), cancellable = true)
    private void coi$loadCustomLogo(ResourceManager resourceManager, CallbackInfoReturnable<TextureContents> cir) {
        try (InputStream stream = LoadingOverlayMixin.class.getResourceAsStream(CUSTOM_LOGO_PATH)) {
            if (stream == null) return;
            cir.setReturnValue(new TextureContents(
                NativeImage.read(stream),
                new TextureMetadataSection(true, true, MipmapStrategy.MEAN, 0.0F)
            ));
        } catch (IOException ignored) {
        }
    }
}
