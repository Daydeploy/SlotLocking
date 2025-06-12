package me.day.slotlocking;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.day.slotlocking.mixin.accessor.CreativeSlotAccessor;
import me.day.slotlocking.mixin.accessor.KeyBindingAccessor;
import me.day.slotlocking.mixin.accessor.ServerWorldAccessor;
import me.day.slotlocking.mixin.accessor.SlotAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;

public class SlotLocking implements ClientModInitializer {
    public static String MOD_ID = "slotlocking";
    public static final Identifier SLOT_LOCK_TEXTURE = Identifier.of(MOD_ID, "textures/gui/sprites/container/lock.png");
    public static Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static KeyBinding lockBinding;
    public static String currentKey = "world";
    public static boolean isSaveDirty = false;
    private static LinkedHashSet<Integer> lockedSlots = new LinkedHashSet<>();
    private static long lastDirtyCheck = System.currentTimeMillis();

    @SuppressWarnings({"unchecked", "unused"})
    public static LinkedHashSet<Integer> getLockedSlots() {
        return (LinkedHashSet<Integer>) lockedSlots.clone();
    }

    public static boolean isLocked(int slot) {
        return lockedSlots.contains(slot);
    }

    public static void lockSlot(int slot) {
        lockedSlots.add(slot);
        isSaveDirty = true;
    }

    public static void unlockSlot(int slot) {
        lockedSlots.remove(slot);
        isSaveDirty = true;
    }

    public static void handleJoinWorld(MinecraftClient client) {
        String key = "world";
        if (client.isIntegratedServerRunning()) {
            IntegratedServer server = client.getServer();
            if (server != null) {
                key = ((ServerWorldAccessor) server.getOverworld()).getWorldProperties().getLevelName();
            }
        } else {
            ServerInfo info = client.getCurrentServerEntry();
            if (info != null) {
                key = info.address;
            }
        }
        LOGGER.info("Loading lockedslots file");
        currentKey = key;
        lockedSlots = new LinkedHashSet<>();
        File slotLockFile = new File(MinecraftClient.getInstance().runDirectory, "/config/slotlocking/lockedslots.json");
        Path slotLockPath = Paths.get(slotLockFile.getAbsolutePath());
        if (Files.notExists(slotLockPath)) {
            try {
                LOGGER.info("File not found! Creating new lockedslots file");
                Files.writeString(slotLockPath, "{ }");
                LOGGER.info("Successfully created new lockedslots file");
            } catch (Exception e) {
                LOGGER.error("An error occurred while creating the lockedslots file.", e);
            }
        }
        String json;
        try {
            json = Files.readString(slotLockPath);
        } catch (Exception e) {
            LOGGER.error("An error occurred while loading the lockedslots file.", e);
            json = "{ }";
        }
        try {
            JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
            JsonArray lockedSlotsJson = jsonObject.getAsJsonArray(key);
            if (lockedSlotsJson != null) {
                lockedSlotsJson.forEach(element -> {
                    int slot = -1;
                    try {
                        slot = element.getAsInt();
                    } catch (Exception ignored) {
                    }
                    if (slot != -1)
                        lockedSlots.add(slot);
                });
            }
            LOGGER.info("Successfully loaded lockedslots file");
        } catch (Exception e) {
            LOGGER.error("An error occurred while reading the lockedslots file.", e);
        }

    }

    public static void handleMouseClick(ScreenHandler handler, PlayerInventory playerInventory, Slot slot, Slot deleteItemSlot, int invSlot, int clickData, SlotActionType actionType, CallbackInfo info) {
        if (!MinecraftClient.getInstance().isOnThread()) return;
        if (slot != null && slot.inventory == playerInventory) {
            Slot finalSlot = slot;
            if (finalSlot instanceof CreativeInventoryScreen.CreativeSlot) {
                finalSlot = ((CreativeSlotAccessor) finalSlot).getSlot();
            }
            int index = ((SlotAccessor) finalSlot).getIndex();
            if (isLocked(index)) {
                info.cancel();
            }
        }

        if (slot != null && actionType == SlotActionType.PICKUP_ALL) {
            ItemStack pickedStack = handler.getCursorStack();
            handler.slots.forEach(handlerSlot -> {
                int slotIndex = ((SlotAccessor) handlerSlot).getIndex();
                if (handlerSlot.inventory == playerInventory && isLocked(slotIndex) && canMergeItems(pickedStack, handlerSlot.getStack())) {
                    info.cancel();
                }
            });
        }

        if (actionType == SlotActionType.QUICK_MOVE && invSlot >= 0 && invSlot < handler.slots.size()) {
            if (slot != null && slot == deleteItemSlot) {
                for (int i = 0; i < playerInventory.size(); ++i) {
                    if (!isLocked(i)) {
                        playerInventory.removeStack(i);
                    }
                }
                info.cancel();
                return;
            }

            Slot slot2 = handler.slots.get(invSlot);
            if (slot2.inventory == playerInventory && isLocked(((SlotAccessor) slot2).getIndex())) {
                info.cancel();
            }
        }

        if (actionType == SlotActionType.SWAP) {
            for (Slot slot3 : handler.slots) {
                Slot finalSlot = slot3;
                if (finalSlot instanceof CreativeInventoryScreen.CreativeSlot) {
                    finalSlot = ((CreativeSlotAccessor) finalSlot).getSlot();
                }
                int index = ((SlotAccessor) finalSlot).getIndex();
                if (finalSlot.inventory == playerInventory && index == clickData && isLocked(index)) {
                    info.cancel();
                }
            }
        }
    }

    public static void handleKeyPressed(Slot focusedSlot, PlayerInventory playerInventory, int keyCode, int scanCode, CallbackInfoReturnable<Boolean> info) {
        if (!MinecraftClient.getInstance().isOnThread()) return;
        if (keyCode != 256 && !MinecraftClient.getInstance().options.inventoryKey.matchesKey(keyCode, scanCode)) {
            Slot finalSlot = focusedSlot;
            if (finalSlot instanceof CreativeInventoryScreen.CreativeSlot) {
                finalSlot = ((CreativeSlotAccessor) finalSlot).getSlot();
            }
            if (finalSlot != null) {
                int index = ((SlotAccessor) finalSlot).getIndex();
                if (finalSlot.inventory == playerInventory) {
                    if (lockBinding.matchesKey(keyCode, scanCode)) {
                        boolean locked = isLocked(index);
                        if (locked) {
                            unlockSlot(index);
                        } else if (!finalSlot.getStack().isEmpty()) {
                            lockSlot(index);
                        }
                    } else {
                        if (isLocked(index)) {
                            info.setReturnValue(true);
                        }
                    }
                }
            }
        }
    }

    public static void handleHotbarKeyPressed(Slot focusedSlot, PlayerInventory playerInventory, CallbackInfoReturnable<Boolean> info) {
        if (!MinecraftClient.getInstance().isOnThread()) return;
        Slot finalSlot = focusedSlot;
        if (finalSlot instanceof CreativeInventoryScreen.CreativeSlot) {
            finalSlot = ((CreativeSlotAccessor) finalSlot).getSlot();
        }
        if (finalSlot != null && finalSlot.inventory == playerInventory && isLocked(((SlotAccessor) finalSlot).getIndex())) {
            info.setReturnValue(false);
        }
    }

    public static void handleDropSelectedItem(PlayerInventory playerInventory, CallbackInfoReturnable<Boolean> info) {
        if (!MinecraftClient.getInstance().isOnThread()) return;
        int selectedSlot = playerInventory.selectedSlot;
        if (isLocked(selectedSlot)) {
            info.setReturnValue(false);
        }
    }

    public static void handleInputEvents(GameOptions options, ClientPlayerEntity player) {
        if (!MinecraftClient.getInstance().isOnThread()) return;
        boolean toPress = false;
        while (options.swapHandsKey.wasPressed()) {
            if (!player.isSpectator()) {
                int selectedSlot = player.getInventory().selectedSlot;
                if (!isLocked(selectedSlot) && !isLocked(40)) {
                    toPress = true;
                }
            }
        }
        if (toPress) KeyBinding.onKeyPressed(((KeyBindingAccessor) options.swapHandsKey).getBoundKey());
    }

    public static void handleItemPick(int selectedSlot, CallbackInfo info) {
        if (isLocked(selectedSlot)) {
            info.cancel();
        }
    }

    private static boolean canMergeItems(ItemStack first, ItemStack second) {
        if (first.getItem() != second.getItem()) {
            return false;
        } else if (first.getDamage() != second.getDamage()) {
            return false;
        } else if (first.getCount() > first.getMaxCount()) {
            return false;
        } else {
            return ItemStack.areEqual(first, second);
        }
    }

    @Override
    public void onInitializeClient() {
        lockBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.slotlock", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_L, "key.categories.inventory"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long currentDirtyCheck = System.currentTimeMillis();
            if (currentDirtyCheck - lastDirtyCheck > 2000) {
                if (isSaveDirty) {
                    File slotLockFile = new File(MinecraftClient.getInstance().runDirectory, "/config/slotlocking/lockedslots.json");
                    Path slotLockPath = Paths.get(slotLockFile.getAbsolutePath());
                    String json = "{ }";
                    try {
                        json = Files.readString(slotLockPath);
                    } catch (Exception ignored) {
                    }
                    JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
                    JsonArray jsonArray = new JsonArray();
                    lockedSlots.forEach(jsonArray::add);
                    jsonObject.add(currentKey, jsonArray);
                    try {
                        Files.writeString(slotLockPath, jsonObject.toString());
                        LOGGER.info("Successfully updated lockedslots file");
                    } catch (Exception e) {
                        LOGGER.error("Failed to update lockedslots file");
                    }
                    isSaveDirty = false;
                }
                lastDirtyCheck = currentDirtyCheck;
            }
        });
    }
}
