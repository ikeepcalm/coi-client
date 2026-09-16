package dev.ua.ikeepcalm.coi.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public class JoinMultiplayerScreenMixin {

    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        ServerList serverList = new ServerList(client);
        serverList.load();
        boolean exists = false;

        for (int i = 0; i < serverList.size(); i++) {
            ServerData server = serverList.get(i);
            if (server.ip.equalsIgnoreCase("mc.mysterria.net") || server.ip.equalsIgnoreCase("play.mysterria.net")) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            ServerData newServer = new ServerData("Mysterria", "mc.mysterria.net", ServerData.Type.OTHER);
            serverList.add(newServer, false);
            serverList.save();
        }
    }

}
