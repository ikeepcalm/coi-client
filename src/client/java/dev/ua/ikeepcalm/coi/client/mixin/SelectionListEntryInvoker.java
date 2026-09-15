package dev.ua.ikeepcalm.coi.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.client.gui.components.AbstractSelectionList$Entry")
/**
 * Exposes the protected content bounds of a selection-list entry, so
 * {@link OnlineServerEntryMixin} can draw its highlight over exactly the row vanilla laid out.
 */
public interface SelectionListEntryInvoker {

    @Invoker("getContentX")
    int callGetContentX();

    @Invoker("getContentY")
    int callGetContentY();

    @Invoker("getContentWidth")
    int callGetContentWidth();

}
