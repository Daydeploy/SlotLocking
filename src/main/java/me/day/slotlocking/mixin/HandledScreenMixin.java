package me.day.slotlocking.mixin;

import me.day.slotlocking.IHandledScreen;
import me.day.slotlocking.SlotLocking;
import me.day.slotlocking.mixin.accessor.CreativeSlotAccessor;
import me.day.slotlocking.mixin.accessor.SlotAccessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.day.slotlocking.SlotLocking.SLOT_LOCK_TEXTURE;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen implements IHandledScreen {
    @Shadow
    @Nullable
    protected Slot focusedSlot;
    @Shadow
    @Final
    protected T handler;
    @Unique
    private PlayerInventory slotlock$playerInventory;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Override
    public PlayerInventory slotlock$getPlayerInventory() {
        return slotlock$playerInventory;
    }

    @Inject(at = @At("TAIL"), method = "<init>")
    public void onInit(T handler, PlayerInventory inventory, Text title, CallbackInfo ci) {
        slotlock$playerInventory = inventory;
    }

    @Inject(at = @At("HEAD"), method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", cancellable = true)
    public void onMouseClick(Slot slot, int invSlot, int clickData, SlotActionType actionType, CallbackInfo info) {
        SlotLocking.handleMouseClick(handler, slotlock$playerInventory, slot, null, invSlot, clickData, actionType, info);
    }

    @Inject(at = @At("HEAD"), method = "keyPressed", cancellable = true)
    public void keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> info) {
        SlotLocking.handleKeyPressed(focusedSlot, slotlock$playerInventory, keyCode, scanCode, info);
    }

    @Inject(at = @At("HEAD"), method = "handleHotbarKeyPressed", cancellable = true)
    public void handleHotbarKeyPressed(int keyCode, int scanCode, CallbackInfoReturnable<Boolean> info) {
        SlotLocking.handleHotbarKeyPressed(focusedSlot, slotlock$playerInventory, info);
    }

    @Inject(at = @At("TAIL"), method = "drawSlot")
    public void drawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        Slot finalSlot = slot;
        if (finalSlot instanceof CreativeInventoryScreen.CreativeSlot) {
            finalSlot = ((CreativeSlotAccessor) finalSlot).getSlot();
        }
        if (this.client != null && slot.inventory == slotlock$playerInventory && SlotLocking.isLocked(((SlotAccessor) finalSlot).getIndex())) {
            if (!finalSlot.hasStack()) {
                SlotLocking.unlockSlot(((SlotAccessor) finalSlot).getIndex());
                return;
            }
            context.drawTexture(RenderLayer::getGuiTexturedOverlay, SLOT_LOCK_TEXTURE, slot.x, slot.y, 0F, 0F, 16, 16, 16, 16, 0x80FFFFFF);
        }
    }
}
