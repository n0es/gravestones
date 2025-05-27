package es.boopurno.gravestones.integration;

import es.boopurno.gravestones.Gravestones;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Integration with Curios mod for storing and restoring curio items
 * Uses reflection to avoid hard dependencies
 */
public class CuriosIntegration {

    private static Class<?> curiosApiClass;
    private static Method getCuriosInventoryMethod;
    private static Method resolveMethod;
    private static boolean initialized = false;

    /**
     * Initialize Curios integration using reflection
     */
    public static void init() {
        try {
            curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            getCuriosInventoryMethod = curiosApiClass.getMethod("getCuriosInventory",
                    net.minecraft.world.entity.LivingEntity.class);

            // Get the LazyOptional class and resolve method
            Class<?> lazyOptionalClass = Class.forName("net.minecraftforge.common.util.LazyOptional");
            resolveMethod = lazyOptionalClass.getMethod("resolve");

            initialized = true;
            Gravestones.LOGGER.info("Curios integration initialized successfully using reflection");
        } catch (Exception e) {
            initialized = false;
            Gravestones.LOGGER.warn("Failed to initialize Curios integration: {}", e.getMessage());
        }
    }

    /**
     * Get all curios items from a player as individual ItemStacks
     * 
     * @param player The player to get curios from
     * @return List of curios ItemStacks
     */
    public static List<ItemStack> getCuriosItems(Player player) {
        List<ItemStack> curiosItems = new ArrayList<>();

        if (!initialized) {
            return curiosItems;
        }

        try {
            // Get curios inventory using reflection
            Object lazyOptional = getCuriosInventoryMethod.invoke(null, player);
            Optional<?> curiosHandler = (Optional<?>) resolveMethod.invoke(lazyOptional);

            if (curiosHandler.isEmpty()) {
                return curiosItems;
            }

            Object curios = curiosHandler.get();

            // Use reflection to get curios map
            Method getCuriosMethod = curios.getClass().getMethod("getCurios");
            Map<String, Object> curiosMap = (Map<String, Object>) getCuriosMethod.invoke(curios);

            // Iterate through all curio slot types
            for (Map.Entry<String, Object> entry : curiosMap.entrySet()) {
                Object stacksHandler = entry.getValue();

                // Get stacks from handler
                Method getStacksMethod = stacksHandler.getClass().getMethod("getStacks");
                Object stackHandler = getStacksMethod.invoke(stacksHandler);

                // Get slot count
                Method getSlotsMethod = stackHandler.getClass().getMethod("getSlots");
                int slots = (Integer) getSlotsMethod.invoke(stackHandler);

                // Get items from slots
                Method getStackInSlotMethod = stackHandler.getClass().getMethod("getStackInSlot", int.class);
                for (int i = 0; i < slots; i++) {
                    ItemStack stack = (ItemStack) getStackInSlotMethod.invoke(stackHandler, i);
                    if (!stack.isEmpty()) {
                        curiosItems.add(stack.copy());
                    }
                }
            }

            Gravestones.LOGGER.debug("Found {} curios items for player", curiosItems.size());
        } catch (Exception e) {
            Gravestones.LOGGER.error("Error getting curios items: {}", e.getMessage());
        }

        return curiosItems;
    }

    /**
     * Store curios items from player to gravestone as individual ItemStacks
     * 
     * @param player       The player whose curios to store
     * @param graveHandler The gravestone's item handler
     * @param startSlot    The starting slot index in the gravestone
     * @return The next available slot index
     */
    public static int storeCurios(Player player, ItemStackHandler graveHandler, int startSlot) {
        if (!initialized) {
            Gravestones.LOGGER.debug("Curios not initialized, skipping storage");
            return startSlot;
        }

        try {
            // Get curios inventory using reflection
            Object lazyOptional = getCuriosInventoryMethod.invoke(null, player);
            Optional<?> curiosHandler = (Optional<?>) resolveMethod.invoke(lazyOptional);

            if (curiosHandler.isEmpty()) {
                Gravestones.LOGGER.debug("No curios handler found for player");
                return startSlot;
            }

            Object curios = curiosHandler.get();
            int slotIndex = startSlot;

            // Use reflection to get curios map
            Method getCuriosMethod = curios.getClass().getMethod("getCurios");
            Map<String, Object> curiosMap = (Map<String, Object>) getCuriosMethod.invoke(curios);

            // Store each curios item individually and clear from player
            for (Map.Entry<String, Object> entry : curiosMap.entrySet()) {
                Object stacksHandler = entry.getValue();

                // Get stacks from handler
                Method getStacksMethod = stacksHandler.getClass().getMethod("getStacks");
                Object stackHandler = getStacksMethod.invoke(stacksHandler);

                // Get slot count and methods
                Method getSlotsMethod = stackHandler.getClass().getMethod("getSlots");
                Method getStackInSlotMethod = stackHandler.getClass().getMethod("getStackInSlot", int.class);
                Method setStackInSlotMethod = stackHandler.getClass().getMethod("setStackInSlot", int.class,
                        ItemStack.class);

                int slots = (Integer) getSlotsMethod.invoke(stackHandler);

                // Store and clear each item from this slot type
                for (int i = 0; i < slots; i++) {
                    ItemStack stack = (ItemStack) getStackInSlotMethod.invoke(stackHandler, i);
                    if (!stack.isEmpty() && slotIndex < graveHandler.getSlots()) {
                        // Store the item in the gravestone
                        graveHandler.setStackInSlot(slotIndex, stack.copy());
                        // Clear the item from the player's curios
                        setStackInSlotMethod.invoke(stackHandler, i, ItemStack.EMPTY);
                        slotIndex++;
                    }
                }
            }

            Gravestones.LOGGER.debug("Stored {} curios items starting at slot {}", slotIndex - startSlot, startSlot);
            return slotIndex;
        } catch (Exception e) {
            Gravestones.LOGGER.error("Error storing curios items: {}", e.getMessage());
            return startSlot;
        }
    }

    /**
     * Restore curios items from gravestone to player
     * 
     * @param player           The player to restore curios to
     * @param graveHandler     The gravestone's item handler
     * @param startSlot        The starting slot index in the gravestone
     * @param itemsToDropAtEnd List to add items that couldn't be restored
     * @return The next slot index after curios slots
     */
    public static int restoreCurios(Player player, ItemStackHandler graveHandler, int startSlot,
            List<ItemStack> itemsToDropAtEnd) {
        if (!initialized) {
            Gravestones.LOGGER.debug("Curios not initialized, clearing slots and dropping items");
            return clearCuriosSlots(graveHandler, startSlot, itemsToDropAtEnd);
        }

        try {
            // Get curios inventory using reflection
            Object lazyOptional = getCuriosInventoryMethod.invoke(null, player);
            Optional<?> curiosHandler = (Optional<?>) resolveMethod.invoke(lazyOptional);

            if (curiosHandler.isEmpty()) {
                Gravestones.LOGGER.debug("No curios handler found for player during restoration");
                return clearCuriosSlots(graveHandler, startSlot, itemsToDropAtEnd);
            }

            Object curios = curiosHandler.get();
            int slotIndex = startSlot;

            // Use reflection to get curios map
            Method getCuriosMethod = curios.getClass().getMethod("getCurios");
            Map<String, Object> curiosMap = (Map<String, Object>) getCuriosMethod.invoke(curios);

            // Restore curios items from gravestone slots
            while (slotIndex < graveHandler.getSlots()) {
                ItemStack stack = graveHandler.getStackInSlot(slotIndex);
                if (stack.isEmpty()) {
                    slotIndex++;
                    continue;
                }

                // Try to find a suitable curios slot for this item
                boolean restored = false;

                for (Map.Entry<String, Object> entry : curiosMap.entrySet()) {
                    if (restored)
                        break;

                    String slotType = entry.getKey();
                    Object stacksHandler = entry.getValue();

                    // Get stacks from handler
                    Method getStacksMethod = stacksHandler.getClass().getMethod("getStacks");
                    Object stackHandler = getStacksMethod.invoke(stacksHandler);

                    // Get methods
                    Method getSlotsMethod = stackHandler.getClass().getMethod("getSlots");
                    Method getStackInSlotMethod = stackHandler.getClass().getMethod("getStackInSlot", int.class);
                    Method setStackInSlotMethod = stackHandler.getClass().getMethod("setStackInSlot", int.class,
                            ItemStack.class);
                    Method isItemValidMethod = stackHandler.getClass().getMethod("isItemValid", int.class,
                            ItemStack.class);

                    int slots = (Integer) getSlotsMethod.invoke(stackHandler);

                    // Check if this item can go in this slot type
                    for (int i = 0; i < slots; i++) {
                        ItemStack existingStack = (ItemStack) getStackInSlotMethod.invoke(stackHandler, i);
                        if (existingStack.isEmpty()) {
                            // Try to place the item
                            boolean isValid = (Boolean) isItemValidMethod.invoke(stackHandler, i, stack);
                            if (isValid) {
                                setStackInSlotMethod.invoke(stackHandler, i, stack.copy());
                                graveHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);
                                restored = true;
                                Gravestones.LOGGER.debug("Restored curios item {} to slot type {} index {}",
                                        stack.getDisplayName().getString(), slotType, i);
                                break;
                            }
                        }
                    }
                }

                if (!restored) {
                    // Couldn't restore this item, add to drop list
                    itemsToDropAtEnd.add(stack.copy());
                    graveHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);
                    Gravestones.LOGGER.debug("Could not restore curios item {}, will drop",
                            stack.getDisplayName().getString());
                }

                slotIndex++;
            }

            Gravestones.LOGGER.debug("Restored curios items up to slot {}", slotIndex - 1);
            return slotIndex;
        } catch (Exception e) {
            Gravestones.LOGGER.error("Error restoring curios items: {}", e.getMessage());
            return clearCuriosSlots(graveHandler, startSlot, itemsToDropAtEnd);
        }
    }

    /**
     * Clear curios slots when curios handler is not available
     */
    private static int clearCuriosSlots(ItemStackHandler graveHandler, int startSlot,
            List<ItemStack> itemsToDropAtEnd) {
        int slotIndex = startSlot;

        // Clear all remaining slots that might contain curios items
        while (slotIndex < graveHandler.getSlots()) {
            ItemStack stack = graveHandler.getStackInSlot(slotIndex);
            if (stack.isEmpty()) {
                slotIndex++;
                continue;
            }

            itemsToDropAtEnd.add(stack);
            graveHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);
            slotIndex++;
        }

        return slotIndex;
    }

    /**
     * Get the number of curios items a player has
     */
    public static int getCuriosItemCount(Player player) {
        return getCuriosItems(player).size();
    }

    /**
     * Get the total number of curios slots a player has
     */
    public static int getCuriosSlotCount(Player player) {
        if (!initialized) {
            return 0;
        }

        try {
            // Get curios inventory using reflection
            Object lazyOptional = getCuriosInventoryMethod.invoke(null, player);
            Optional<?> curiosHandler = (Optional<?>) resolveMethod.invoke(lazyOptional);

            if (curiosHandler.isEmpty()) {
                return 0;
            }

            Object curios = curiosHandler.get();
            int totalSlots = 0;

            // Use reflection to get curios map
            Method getCuriosMethod = curios.getClass().getMethod("getCurios");
            Map<String, Object> curiosMap = (Map<String, Object>) getCuriosMethod.invoke(curios);

            // Count all slots
            for (Object stacksHandler : curiosMap.values()) {
                Method getStacksMethod = stacksHandler.getClass().getMethod("getStacks");
                Object stackHandler = getStacksMethod.invoke(stacksHandler);

                Method getSlotsMethod = stackHandler.getClass().getMethod("getSlots");
                totalSlots += (Integer) getSlotsMethod.invoke(stackHandler);
            }

            return totalSlots;
        } catch (Exception e) {
            Gravestones.LOGGER.error("Error getting curios slot count: {}", e.getMessage());
            return 0;
        }
    }
}