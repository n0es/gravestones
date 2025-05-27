package es.boopurno.gravestones.integration;

import es.boopurno.gravestones.Gravestones;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * Integration with Cosmetic Armor Rewritten mod
 * Uses reflection to avoid hard dependencies
 */
public class CosmeticArmorIntegration {

    private static Class<?> cosArmorApiClass;
    private static Method getCAStacksMethod;
    private static boolean initialized = false;

    /**
     * Initialize Cosmetic Armor integration using reflection
     */
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

    /**
     * Check if Cosmetic Armor integration is initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Store cosmetic armor items from player to gravestone
     * 
     * @param player       The player whose cosmetic armor to store
     * @param graveHandler The gravestone's item handler
     * @param startSlot    The starting slot index in the gravestone
     * @return The next available slot index
     */
    public static int storeCosmeticArmor(Player player, ItemStackHandler graveHandler, int startSlot) {
        if (!initialized) {
            Gravestones.LOGGER.debug("Cosmetic Armor not initialized, skipping storage");
            return startSlot;
        }

        try {
            // Get cosmetic armor inventory using reflection
            Object cosmeticInventory = getCAStacksMethod.invoke(null, player.getUUID());

            if (cosmeticInventory == null) {
                Gravestones.LOGGER.debug("No cosmetic armor inventory found for player");
                return startSlot;
            }

            int slotIndex = startSlot;

            // Get the stacks method
            Method getStacksMethod = cosmeticInventory.getClass().getMethod("getStacks");
            Object stacks = getStacksMethod.invoke(cosmeticInventory);

            // Assume it's a list or array-like structure
            if (stacks instanceof List) {
                List<?> stacksList = (List<?>) stacks;
                for (int i = 0; i < stacksList.size() && i < 4; i++) { // 4 cosmetic armor slots
                    Object stackObj = stacksList.get(i);
                    if (stackObj instanceof ItemStack) {
                        ItemStack stack = (ItemStack) stackObj;
                        if (!stack.isEmpty() && slotIndex < graveHandler.getSlots()) {
                            graveHandler.setStackInSlot(slotIndex, stack.copy());

                            // Clear the cosmetic armor slot
                            Method setStackMethod = cosmeticInventory.getClass().getMethod("setStack", int.class,
                                    ItemStack.class);
                            setStackMethod.invoke(cosmeticInventory, i, ItemStack.EMPTY);

                            slotIndex++;
                        }
                    }
                }
            }

            Gravestones.LOGGER.debug("Stored {} cosmetic armor items starting at slot {}", slotIndex - startSlot,
                    startSlot);
            return slotIndex;
        } catch (Exception e) {
            Gravestones.LOGGER.error("Error storing cosmetic armor items: {}", e.getMessage());
            return startSlot;
        }
    }

    /**
     * Restore cosmetic armor items from gravestone to player
     * 
     * @param player           The player to restore cosmetic armor to
     * @param graveHandler     The gravestone's item handler
     * @param startSlot        The starting slot index in the gravestone
     * @param itemsToDropAtEnd List to add items that couldn't be restored
     * @return The next slot index after cosmetic armor slots
     */
    public static int restoreCosmeticArmor(Player player, ItemStackHandler graveHandler, int startSlot,
            List<ItemStack> itemsToDropAtEnd) {
        if (!initialized) {
            Gravestones.LOGGER.debug("Cosmetic Armor not initialized, clearing slots and dropping items");
            return clearCosmeticArmorSlots(graveHandler, startSlot, itemsToDropAtEnd);
        }

        try {
            // Get cosmetic armor inventory using reflection
            Object cosmeticInventory = getCAStacksMethod.invoke(null, player.getUUID());

            if (cosmeticInventory == null) {
                Gravestones.LOGGER.debug("No cosmetic armor inventory found for player during restoration");
                return clearCosmeticArmorSlots(graveHandler, startSlot, itemsToDropAtEnd);
            }

            int slotIndex = startSlot;

            // Restore up to 4 cosmetic armor slots
            for (int i = 0; i < 4 && slotIndex < graveHandler.getSlots(); i++) {
                ItemStack stack = graveHandler.getStackInSlot(slotIndex);
                if (!stack.isEmpty()) {
                    try {
                        // Try to restore the cosmetic armor item
                        Method setStackMethod = cosmeticInventory.getClass().getMethod("setStack", int.class,
                                ItemStack.class);
                        setStackMethod.invoke(cosmeticInventory, i, stack.copy());
                        graveHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);

                        Gravestones.LOGGER.debug("Restored cosmetic armor item {} to slot {}",
                                stack.getDisplayName().getString(), i);
                    } catch (Exception e) {
                        // Couldn't restore this item, add to drop list
                        itemsToDropAtEnd.add(stack.copy());
                        graveHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);
                        Gravestones.LOGGER.debug("Could not restore cosmetic armor item {}, will drop",
                                stack.getDisplayName().getString());
                    }
                }
                slotIndex++;
            }

            Gravestones.LOGGER.debug("Restored cosmetic armor items up to slot {}", slotIndex - 1);
            return slotIndex;
        } catch (Exception e) {
            Gravestones.LOGGER.error("Error restoring cosmetic armor items: {}", e.getMessage());
            return clearCosmeticArmorSlots(graveHandler, startSlot, itemsToDropAtEnd);
        }
    }

    /**
     * Clear cosmetic armor slots when cosmetic armor is not available
     */
    private static int clearCosmeticArmorSlots(ItemStackHandler graveHandler, int startSlot,
            List<ItemStack> itemsToDropAtEnd) {
        int slotIndex = startSlot;

        // Clear up to 4 cosmetic armor slots
        for (int i = 0; i < 4 && slotIndex < graveHandler.getSlots(); i++) {
            ItemStack stack = graveHandler.getStackInSlot(slotIndex);
            if (!stack.isEmpty()) {
                itemsToDropAtEnd.add(stack);
                graveHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);
            }
            slotIndex++;
        }

        return slotIndex;
    }

    /**
     * Event handler for cosmetic armor death drops (if needed)
     * This would be called from an event handler if the mod is present
     */
    public static void onCosmeticArmorDeathDrops(Object event) {
        if (!initialized) {
            return;
        }

        try {
            // Handle the event using reflection if needed
            // This is a placeholder for potential future event handling
            Gravestones.LOGGER.debug("Handling cosmetic armor death drops event");
        } catch (Exception e) {
            Gravestones.LOGGER.error("Error handling cosmetic armor death drops: {}", e.getMessage());
        }
    }
}