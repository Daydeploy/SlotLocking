package me.day.slotlocking.mixin;

import me.day.slotlocking.SlotLocking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow
    @Final
    public GameOptions options;

    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @SuppressWarnings("ConstantConditions")
    @Inject(at = @At("HEAD"), method = "joinWorld")
    public void joinWorld(ClientWorld world, DownloadingTerrainScreen.WorldEntryReason worldEntryReason, CallbackInfo ci) {
        SlotLocking.handleJoinWorld(((MinecraftClient) ((Object) this)));
    }

    @Inject(at = @At("HEAD"), method = "handleInputEvents")
    public void handleInputEvents(CallbackInfo info) {
        SlotLocking.handleInputEvents(options, player);
    }

    @Inject(at = @At(value = "HEAD"), method = "doItemPick", cancellable = true)
    public void handleItemPick(CallbackInfo ci) {
        SlotLocking.handleItemPick(Objects.requireNonNull(player).getInventory().getSelectedSlot(), ci);
    }
}
