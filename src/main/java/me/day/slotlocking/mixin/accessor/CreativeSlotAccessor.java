package me.day.slotlocking.mixin.accessor;

import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CreativeInventoryScreen.CreativeSlot.class)
public interface CreativeSlotAccessor {
    @Accessor
    Slot getSlot();
}
