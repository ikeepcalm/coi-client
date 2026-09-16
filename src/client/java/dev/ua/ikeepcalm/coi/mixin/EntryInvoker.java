package dev.ua.ikeepcalm.coi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.client.gui.components.AbstractSelectionList$Entry")
public interface EntryInvoker {

    @Invoker("getContentX")
    int callGetContentX();

    @Invoker("getContentY")
    int callGetContentY();

    @Invoker("getContentWidth")
    int callGetContentWidth();

}
