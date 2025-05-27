package es.boopurno.gravestones.integration;

import es.boopurno.gravestones.Gravestones;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Integration with Curios mod for storing and restoring curio items
 */
public class CuriosIntegration {

    /**
     * Initialize Curios integration
     */
    public static void init() {
        Gravestones.LOGGER.info("Curios integration initialized successfully");
    }

    /**
     * Get all curios items from a player as individual ItemStacks
     * 
     * @param player The player to get curios from
     * @return List of curios ItemStacks
     */
    public static List<ItemStack> getCuriosItems(Player player) {
        List<ItemStack> curiosItems = new ArrayList<>();
        Optional<ICuriosItemHandler> curiosHandler = CuriosApi.getCuriosInventory(player).resolve();

        if (curiosHandler.isEmpty()) {
            return curiosItems;
        }

        ICuriosItemHandler curios = curiosHandler.get();
        Map<String, ICurioStacksHandler> curiosMap = curios.getCurios();

        // Iterate through all curio slot types
        for (Map.Entry<String, ICurioStacksHandler> entry : curiosMap.entrySet()) {
            ICurioStacksHandler stacksHandler = entry.getValue();
            IDynamicStackHandler stackHandler = stacksHandler.getStacks();

            // Get all items from this slot type
            for (int i = 0; i < stackHandler.getSlots(); i++) {
                ItemStack stack = stackHandler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    curiosItems.add(stack.copy());
                }
            }
        }

        Gravestones.LOGGER.debug("Found {} curios items for player", curiosItems.size());
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
        Optional<ICuriosItemHandler> curiosHandler = CuriosApi.getCuriosInventory(player).resolve();
        if (curiosHandler.isEmpty()) {
            Gravestones.LOGGER.debug("No curios handler found for player");
            return startSlot;
        }

        ICuriosItemHandler curios = curiosHandler.get();
        Map<String, ICurioStacksHandler> curiosMap = curios.getCurios();
        int slotIndex = startSlot;

        // Store each curios item individually and clear from player
        for (Map.Entry<String, ICurioStacksHandler> entry : curiosMap.entrySet()) {
            ICurioStacksHandler stacksHandler = entry.getValue();
            IDynamicStackHandler stackHandler = stacksHandler.getStacks();

            // Store and clear each item from this slot type
            for (int i = 0; i < stackHandler.getSlots(); i++) {
                ItemStack stack = stackHandler.getStackInSlot(i);
                if (!stack.isEmpty() && slotIndex < graveHandler.getSlots()) {
                    // Store the item in the gravestone
                    graveHandler.setStackInSlot(slotIndex, stack.copy());
                    // Clear the item from the player's curios
                    stackHandler.setStackInSlot(i, ItemStack.EMPTY);
                    slotIndex++;
                }
            }
        }

        Gravestones.LOGGER.debug("Stored {} curios items starting at slot {}", slotIndex - startSlot, startSlot);
        return slotIndex;
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
        Optional<ICuriosItemHandler> curiosHandler = CuriosApi.getCuriosInventory(player).resolve();
        if (curiosHandler.isEmpty()) {
            Gravestones.LOGGER.debug("No curios handler found for player during restoration");
            return clearCuriosSlots(graveHandler, startSlot, itemsToDropAtEnd);
        }

        ICuriosItemHandler curios = curiosHandler.get();
        int slotIndex = startSlot;

        // Restore curios items from gravestone slots
        while (slotIndex < graveHandler.getSlots()) {
            ItemStack stack = graveHandler.getStackInSlot(slotIndex);
            if (stack.isEmpty()) {
                slotIndex++;
                continue;
            }

            // Try to find a suitable curios slot for this item
            boolean restored = false;
            Map<String, ICurioStacksHandler> curiosMap = curios.getCurios();

            for (Map.Entry<String, ICurioStacksHandler> entry : curiosMap.entrySet()) {
                if (restored)
                    break;

                String slotType = entry.getKey();
                ICurioStacksHandler stacksHandler = entry.getValue();
                IDynamicStackHandler stackHandler = stacksHandler.getStacks();

                // Check if this item can go in this slot type
                for (int i = 0; i < stackHandler.getSlots(); i++) {
                    if (stackHandler.getStackInSlot(i).isEmpty()) {
                        // Try to place the item
                        if (stackHandler.isItemValid(i, stack)) {
                            stackHandler.setStackInSlot(i, stack.copy());
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
     * Get the number of curios items for a player (actual items, not slot count)
     * 
     * @param player The player to check
     * @return The number of curios items
     */
    public static int getCuriosItemCount(Player player) {
        return getCuriosItems(player).size();
    }

    /**
     * Get the number of curios slots for a player
     * 
     * @param player The player to check
     * @return The number of curios slots
     */
    public static int getCuriosSlotCount(Player player) {
        Optional<ICuriosItemHandler> curiosHandler = CuriosApi.getCuriosInventory(player).resolve();
        if (curiosHandler.isEmpty()) {
            return 0;
        }

        return curiosHandler.get().getVisibleSlots();
    }
}