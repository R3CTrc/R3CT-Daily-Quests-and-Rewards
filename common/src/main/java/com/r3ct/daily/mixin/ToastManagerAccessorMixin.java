package com.r3ct.daily.mixin;

import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.BitSet;

@Mixin(ToastComponent.class)
public interface ToastManagerAccessorMixin {
    @Accessor("occupiedSlots")
    BitSet getOccupiedSlots();
}