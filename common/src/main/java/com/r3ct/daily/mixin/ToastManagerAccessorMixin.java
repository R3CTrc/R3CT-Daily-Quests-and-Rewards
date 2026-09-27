package com.r3ct.daily.mixin;

import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.BitSet;

@Mixin(ToastManager.class)
public interface ToastManagerAccessorMixin {
    @Accessor("occupiedSlots")
    BitSet getOccupiedSlots();
}