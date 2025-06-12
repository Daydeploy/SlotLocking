package me.day.slotlocking.mixin;

import me.day.slotlocking.IHandledScreen;
import me.day.slotlocking.SlotLocking;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends HandledScreen<CreativeInventoryScreen.CreativeScreenHandler> {

    @Shadow
    private Slot deleteItemSlot;

    public CreativeInventoryScreenMixin(CreativeInventoryScreen.CreativeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(at = @At("HEAD"), method = "onMouseClick", cancellable = true)
    public void onMouseClick(Slot slot, int invSlot, int clickData, SlotActionType actionType, CallbackInfo info) {
        SlotLocking.handleMouseClick(handler, ((IHandledScreen) this).slotlock$getPlayerInventory(), slot, deleteItemSlot, invSlot, clickData, actionType, info);
    }
}
