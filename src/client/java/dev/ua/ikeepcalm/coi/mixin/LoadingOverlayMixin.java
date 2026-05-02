package dev.ua.ikeepcalm.coi.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;

@Mixin(targets = "net.minecraft.client.gui.screens.LoadingOverlay$LogoTexture")
public class LoadingOverlayMixin {

//    @Unique private static final Logger LOGGER = LoggerFactory.getLogger("coi-client/LoadingOverlayMixin");

    @Inject(method = "loadContents", at = @At("HEAD"), cancellable = true)
    private void coi$loadCustomLogo(ResourceManager resourceManager, CallbackInfoReturnable<TextureContents> cir) {
//        LOGGER.info("loadContents called, resourceManager={}", resourceManager.getClass().getSimpleName());
        try (InputStream stream = resourceManager.open(LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION)) {
//            LOGGER.info("Loaded logo via full resource manager (pack overrides respected)");
            cir.setReturnValue(new TextureContents(
                NativeImage.read(stream),
                new TextureMetadataSection(true, true, MipmapStrategy.MEAN, 0.0F)
            ));
        } catch (IOException e) {
//            LOGGER.warn("Failed to load logo via resource manager: {}", e.getMessage());
        }
    }
}
