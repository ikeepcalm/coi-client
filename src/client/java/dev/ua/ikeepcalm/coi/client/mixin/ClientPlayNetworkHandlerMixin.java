package dev.ua.ikeepcalm.coi.client.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.CustomPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Inject(
            method = "onCustomPayload",
            at = @At("HEAD")
    )
    private void onCustomPayloadReceiveHEAD(CustomPayload payload, CallbackInfo ci) {
        System.out.println("[COI DEBUG - GLOBAL MIXIN] Received custom S2C packet. ID: " + payload.getId() + ", Payload: " + payload);
        System.out.println(payload.getId());
    }
}