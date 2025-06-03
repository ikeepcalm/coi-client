package dev.ua.ikeepcalm.coi.client.mixin;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatMessageMixin {
    
    private static final String ABILITY_DATA_PREFIX = "_0_0_1_1_r";

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", 
            at = @At("HEAD"), 
            cancellable = true)
    private void interceptMessage(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        String messageText = message.getString();
        
        if (messageText.startsWith(ABILITY_DATA_PREFIX)) {
            String data = messageText.substring(ABILITY_DATA_PREFIX.length());
            if (data.startsWith("ABILITIES:")) {
                CircleOfImaginationClient.handleAbilityData(data.substring(10));
                ci.cancel();
            }
        }
    }
}