package es.boopurno.gravestones.integration;

import es.boopurno.gravestones.Gravestones;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles integration with various mods for storing and restoring player
 * inventories
 */
public class ModIntegration {

    // Mod detection flags
    public static final boolean HAS_COSMETIC_ARMOR;
    public static final boolean HAS_CURIOS;

    // Slot layout constants
    public static final int GRAVE_MAIN_INVENTORY_SLOTS = 27; // 3x9 grid
    public static final int GRAVE_HOTBAR_SLOTS = 9; // 1x9 grid
    public static final int BASE_ARMOR_SLOTS = 4; // Standard armor
    public static final int BASE_OFFHAND_SLOTS = 1; // Offhand/shield
    public static final int COSMETIC_ARMOR_SLOTS = 4; // Cosmetic armor (4 armor slots only, no offhand)

    static {
        HAS_COSMETIC_ARMOR = ModList.get().isLoaded("cosmeticarmorreworked");
        HAS_CURIOS = ModList.get().isLoaded("curios");

        Gravestones.LOGGER.info("Mod integration status - CosmeticArmor: {}, Curios: {}",
                HAS_COSMETIC_ARMOR, HAS_CURIOS);
    }

    /**
     * Calculate the total inventory size needed based on loaded mods and player's
     * actual curio slots
     */
    public static int calculateInventorySize(Player player) {
        int size = GRAVE_MAIN_INVENTORY_SLOTS + GRAVE_HOTBAR_SLOTS + BASE_ARMOR_SLOTS + BASE_OFFHAND_SLOTS; // 41 base
                                                                                                            // slots

        if (HAS_COSMETIC_ARMOR) {
            size += COSMETIC_ARMOR_SLOTS;
        }

        if (HAS_CURIOS && player != null) {
            // Dynamically calculate curio slots based on player's actual curio inventory
            int curiosSlots = CuriosIntegration.getCuriosSlotCount(player);
            size += curiosSlots;
            Gravestones.LOGGER.debug("Player has {} curio slots, adding to gravestone size", curiosSlots);
        }

        return size;
    }

    /**
     * Calculate the total inventory size needed based on loaded mods (fallback
     * without player)
     */
    public static int calculateInventorySize() {
        int size = GRAVE_MAIN_INVENTORY_SLOTS + GRAVE_HOTBAR_SLOTS + BASE_ARMOR_SLOTS + BASE_OFFHAND_SLOTS; // 41 base
                                                                                                            // slots

        if (HAS_COSMETIC_ARMOR) {
            size += COSMETIC_ARMOR_SLOTS;
        }

        if (HAS_CURIOS) {
            // Use a reasonable default when player is not available
            size += 24; // Default to 24 slots to handle most cases
            Gravestones.LOGGER.debug("Using default curio slot count of 24 for gravestone size calculation");
        }

        return size;
    }

    /**
     * Initialize all mod integrations
     */
    public static void init() {
        if (HAS_COSMETIC_ARMOR) {
            CosmeticArmorIntegration.init();
        }
        if (HAS_CURIOS) {
            CuriosIntegration.init();
        }
    }

    /**
     * Store all player inventory items including mod items to gravestone
     */
    public static void storePlayerInventory(Player player, ItemStackHandler graveHandler) {
        // Store main inventory (slots 0-26) - 3x9 grid
        for (int i = 0; i < 27; i++) {
            graveHandler.setStackInSlot(i, player.getInventory().items.get(i + 9).copy()); // Skip hotbar initially
        }

        // Store hotbar (slots 27-35) - 1x9 grid
        for (int i = 0; i < 9; i++) {
            graveHandler.setStackInSlot(27 + i, player.getInventory().items.get(i).copy()); // Hotbar is items 0-8
        }

        // Store armor (slots 36-39) + offhand (slot 40) = 5 slots total
        for (int i = 0; i < 4; i++) {
            graveHandler.setStackInSlot(36 + i, player.getInventory().armor.get(i).copy());
        }
        // Offhand as 5th armor slot
        if (!player.getInventory().offhand.get(0).isEmpty()) {
            graveHandler.setStackInSlot(40, player.getInventory().offhand.get(0).copy());
        }

        int nextSlot = 41; // Start after armor + offhand

        // Store cosmetic armor if available (slots 41-44)
        if (HAS_COSMETIC_ARMOR) {
            nextSlot = CosmeticArmorIntegration.storeCosmeticArmor(player, graveHandler, nextSlot);
        }

        // Store curios if available
        if (HAS_CURIOS) {
            nextSlot = storeCurios(player, graveHandler, nextSlot);
        }

        Gravestones.LOGGER.debug("Stored player inventory with {} total slots used", nextSlot);
    }

    /**
     * Restore items from gravestone to player inventory including mod items
     */
    public static void restorePlayerInventory(Player player, ItemStackHandler graveHandler) {
        List<ItemStack> itemsToDropAtEnd = new ArrayList<>();

        // Restore armor first (slots 36-39)
        restoreArmor(player, graveHandler, itemsToDropAtEnd);

        // Restore offhand (slot 40)
        restoreOffhand(player, graveHandler, itemsToDropAtEnd);

        // Restore main inventory (slots 0-26)
        restoreMainInventory(player, graveHandler, itemsToDropAtEnd);

        // Restore hotbar (slots 27-35)
        restoreHotbar(player, graveHandler, itemsToDropAtEnd);

        int nextSlot = 41;

        // Restore cosmetic armor if available (slots 41-44)
        if (HAS_COSMETIC_ARMOR) {
            nextSlot = CosmeticArmorIntegration.restoreCosmeticArmor(player, graveHandler, nextSlot, itemsToDropAtEnd);
        }

        // Restore curios if available
        if (HAS_CURIOS) {
            nextSlot = restoreCurios(player, graveHandler, nextSlot, itemsToDropAtEnd);
        }

        // Drop items that couldn't be placed (if player is not creative)
        if (!player.isCreative()) {
            for (ItemStack stackToDrop : itemsToDropAtEnd) {
                if (!stackToDrop.isEmpty()) {
                    player.drop(stackToDrop, false);
                }
            }
        }

        Gravestones.LOGGER.debug("Restored player inventory, {} items dropped", itemsToDropAtEnd.size());
    }

    private static int storeCurios(Player player, ItemStackHandler graveHandler, int startSlot) {
        if (HAS_CURIOS) {
            return CuriosIntegration.storeCurios(player, graveHandler, startSlot);
        }
        // If curios is not available, just return the start slot
        Gravestones.LOGGER.debug("Curios not available - no slots reserved starting at {}", startSlot);
        return startSlot;
    }

    private static void restoreArmor(Player player, ItemStackHandler graveHandler, List<ItemStack> itemsToDropAtEnd) {
        // Restore armor slots (36-39)
        for (int i = 0; i < 4; i++) {
            int graveSlot = 36 + i;
            ItemStack armorPiece = graveHandler.getStackInSlot(graveSlot);
            if (!armorPiece.isEmpty()) {
                // Try to equip armor piece or add to inventory
                if (player.getInventory().armor.get(i).isEmpty()) {
                    player.getInventory().armor.set(i, armorPiece.copy());
                    graveHandler.setStackInSlot(graveSlot, ItemStack.EMPTY);
                } else if (player.getInventory().add(armorPiece.copy())) {
                    graveHandler.setStackInSlot(graveSlot, ItemStack.EMPTY);
                } else {
                    itemsToDropAtEnd.add(armorPiece);
                    graveHandler.setStackInSlot(graveSlot, ItemStack.EMPTY);
                }
            }
        }
    }

    private static void restoreOffhand(Player player, ItemStackHandler graveHandler, List<ItemStack> itemsToDropAtEnd) {
        int offhandSlot = 40; // Offhand is now slot 40
        ItemStack offhandItem = graveHandler.getStackInSlot(offhandSlot);
        if (!offhandItem.isEmpty()) {
            if (player.getInventory().offhand.get(0).isEmpty()) {
                player.getInventory().offhand.set(0, offhandItem.copy());
                graveHandler.setStackInSlot(offhandSlot, ItemStack.EMPTY);
            } else if (player.getInventory().add(offhandItem.copy())) {
                graveHandler.setStackInSlot(offhandSlot, ItemStack.EMPTY);
            } else {
                itemsToDropAtEnd.add(offhandItem);
                graveHandler.setStackInSlot(offhandSlot, ItemStack.EMPTY);
            }
        }
    }

    private static void restoreMainInventory(Player player, ItemStackHandler graveHandler,
            List<ItemStack> itemsToDropAtEnd) {
        // Restore main inventory (slots 0-26) to player inventory slots 9-35
        for (int i = 0; i < 27; i++) {
            ItemStack item = graveHandler.getStackInSlot(i);
            if (!item.isEmpty()) {
                int playerSlot = i + 9; // Grave slot 0 -> player slot 9, etc.
                // Try to place in original slot first
                if (player.getInventory().items.get(playerSlot).isEmpty()) {
                    player.getInventory().items.set(playerSlot, item.copy());
                    graveHandler.setStackInSlot(i, ItemStack.EMPTY);
                } else if (player.getInventory().add(item.copy())) {
                    graveHandler.setStackInSlot(i, ItemStack.EMPTY);
                } else {
                    itemsToDropAtEnd.add(item);
                    graveHandler.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
    }

    private static void restoreHotbar(Player player, ItemStackHandler graveHandler, List<ItemStack> itemsToDropAtEnd) {
        // Restore hotbar (slots 27-35) to player inventory slots 0-8
        for (int i = 0; i < 9; i++) {
            int graveSlot = 27 + i;
            ItemStack item = graveHandler.getStackInSlot(graveSlot);
            if (!item.isEmpty()) {
                int playerSlot = i; // Grave slot 27 -> player slot 0, etc.
                // Try to place in original slot first
                if (player.getInventory().items.get(playerSlot).isEmpty()) {
                    player.getInventory().items.set(playerSlot, item.copy());
                    graveHandler.setStackInSlot(graveSlot, ItemStack.EMPTY);
                } else if (player.getInventory().add(item.copy())) {
                    graveHandler.setStackInSlot(graveSlot, ItemStack.EMPTY);
                } else {
                    itemsToDropAtEnd.add(item);
                    graveHandler.setStackInSlot(graveSlot, ItemStack.EMPTY);
                }
            }
        }
    }

    private static int restoreCurios(Player player, ItemStackHandler graveHandler, int startSlot,
            List<ItemStack> itemsToDropAtEnd) {
        if (HAS_CURIOS) {
            return CuriosIntegration.restoreCurios(player, graveHandler, startSlot, itemsToDropAtEnd);
        }
        // If curios is not available, clear any remaining slots and drop items
        int slotIndex = startSlot;
        while (slotIndex < graveHandler.getSlots()) {
            ItemStack item = graveHandler.getStackInSlot(slotIndex);
            if (!item.isEmpty()) {
                if (player.getInventory().add(item.copy())) {
                    graveHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);
                } else {
                    itemsToDropAtEnd.add(item);
                    graveHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);
                }
            }
            slotIndex++;
        }
        return slotIndex;
    }
}