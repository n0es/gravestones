package es.boopurno.gravestones.integration;

import es.boopurno.gravestones.Gravestones;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class CosmeticArmorIntegration {

    private static Class<?> cosArmorApiClass;
    private static Method getCAStacksMethod;
    private static boolean initialized = false;

    public static void init() {
        try {
            cosArmorApiClass = Class.forName("lain.mods.cos.api.CosArmorAPI");
            getCAStacksMethod = cosArmorApiClass.getMethod("getCAStacks", UUID.class);

            initialized = true;
            Gravestones.LOGGER.info("Cosmetic Armor integration initialized successfully using reflection");
        } catch (Exception e) {
            initialized = false;
            Gravestones.LOGGER.warn("Failed to initialize Cosmetic Armor integration: {}", e.getMessage());
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static int storeCosmeticArmor(Player player, ItemStackHandler graveHandler, int startSlot) {
        if (!initialized) {
            Gravestones.LOGGER.debug("Cosmetic Armor not initialized, skipping storage");
            return startSlot + 4;
        }

        try {
            Object cosmeticInventory = getCAStacksMethod.invoke(null, player.getUUID());

            if (cosmeticInventory == null) {
                Gravestones.LOGGER.debug("No cosmetic armor inventory found for player");
                return startSlot + 4;
            }

            int slotIndex = startSlot;
            int itemsStored = 0;

            for (int i = 0; i < 4; i++) {
                if (slotIndex >= graveHandler.getSlots()) {
                    break;
                }

                Method getStackInSlotMethod = cosmeticInventory.getClass().getMethod("getStackInSlot", int.class);
                ItemStack stack = (ItemStack) getStackInSlotMethod.invoke(cosmeticInventory, i);

                if (!stack.isEmpty()) {
                    graveHandler.setStackInSlot(slotIndex, stack.copy());
                    Gravestones.LOGGER.debug("Stored cosmetic armor item {} from slot {} to gravestone slot {}",
                            stack.getDisplayName().getString(), i, slotIndex);

                    Method setStackInSlotMethod = cosmeticInventory.getClass().getMethod("setStackInSlot", int.class,
                            ItemStack.class);
                    setStackInSlotMethod.invoke(cosmeticInventory, i, ItemStack.EMPTY);

                    itemsStored++;
                }
                slotIndex++;
            }

            Gravestones.LOGGER.info("Stored {} cosmetic armor items starting at slot {}", itemsStored, startSlot);
            return startSlot + 4;
        } catch (Exception e) {
            Gravestones.LOGGER.error("Error storing cosmetic armor items: {}", e.getMessage());
            Gravestones.LOGGER.debug("Cosmetic armor storage failed completely, items may drop on ground");
            return startSlot + 4;
        }
    }

    public static int restoreCosmeticArmor(Player player, ItemStackHandler graveHandler, int startSlot,
            List<ItemStack> itemsToDropAtEnd) {
        if (!initialized) {
            Gravestones.LOGGER.debug("Cosmetic Armor not initialized, clearing slots and dropping items");
            return clearCosmeticArmorSlots(graveHandler, startSlot, itemsToDropAtEnd);
        }

        try {
            Object cosmeticInventory = getCAStacksMethod.invoke(null, player.getUUID());

            if (cosmeticInventory == null) {
                Gravestones.LOGGER.debug("No cosmetic armor inventory found for player during restoration");
                return clearCosmeticArmorSlots(graveHandler, startSlot, itemsToDropAtEnd);
            }

            int slotIndex = startSlot;
            int itemsRestored = 0;

            for (int i = 0; i < 4 && slotIndex < graveHandler.getSlots(); i++) {
                ItemStack stack = graveHandler.getStackInSlot(slotIndex);
                if (!stack.isEmpty()) {
                    try {
                        Method setStackInSlotMethod = cosmeticInventory.getClass().getMethod("setStackInSlot",
                                int.class, ItemStack.class);
                        setStackInSlotMethod.invoke(cosmeticInventory, i, stack.copy());
                        graveHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);
                        itemsRestored++;
                        Gravestones.LOGGER.debug("Restored cosmetic armor item {} to slot {}",
                                stack.getDisplayName().getString(), i);
                    } catch (Exception e) {
                        itemsToDropAtEnd.add(stack.copy());
                        graveHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);
                        Gravestones.LOGGER.debug("Could not restore cosmetic armor item {}, will drop: {}",
                                stack.getDisplayName().getString(), e.getMessage());
                    }
                }
                slotIndex++;
            }

            Gravestones.LOGGER.info("Restored {} cosmetic armor items", itemsRestored);
            return startSlot + 4;
        } catch (Exception e) {
            Gravestones.LOGGER.error("Error restoring cosmetic armor items: {}", e.getMessage());
            return clearCosmeticArmorSlots(graveHandler, startSlot, itemsToDropAtEnd);
        }
    }

    private static int clearCosmeticArmorSlots(ItemStackHandler graveHandler, int startSlot,
            List<ItemStack> itemsToDropAtEnd) {
        int slotIndex = startSlot;

        for (int i = 0; i < 4 && slotIndex < graveHandler.getSlots(); i++) {
            ItemStack stack = graveHandler.getStackInSlot(slotIndex);
            if (!stack.isEmpty()) {
                itemsToDropAtEnd.add(stack);
                graveHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);
            }
            slotIndex++;
        }

        return startSlot + 4;
    }

    public static void onCosmeticArmorDeathDrops(Object event) {
        if (!initialized) {
            return;
        }

        try {
            Gravestones.LOGGER.debug("Handling cosmetic armor death drops event");
        } catch (Exception e) {
            Gravestones.LOGGER.error("Error handling cosmetic armor death drops: {}", e.getMessage());
        }
    }
}