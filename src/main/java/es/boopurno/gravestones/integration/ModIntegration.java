package es.boopurno.gravestones.integration;

import es.boopurno.gravestones.Gravestones;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class ModIntegration {
    public static final int GRAVE_MAIN_INVENTORY_SLOTS = 27;
    public static final int GRAVE_HOTBAR_SLOTS = 9;
    public static final int BASE_ARMOR_SLOTS = 4;
    public static final int BASE_OFFHAND_SLOTS = 1;
    public static final int COSMETIC_ARMOR_SLOTS = 4;

    public static boolean hasCosmeticArmor() {
        return ModList.get().isLoaded("cosmeticarmorreworked") && CosmeticArmorIntegration.isInitialized();
    }

    public static boolean hasCurios() {
        return ModList.get().isLoaded("curios") && CuriosIntegration.isInitialized();
    }

    public static int calculateInventorySize(Player player) {
        int size = GRAVE_MAIN_INVENTORY_SLOTS + GRAVE_HOTBAR_SLOTS + BASE_ARMOR_SLOTS + BASE_OFFHAND_SLOTS;

        if (hasCosmeticArmor()) {
            size += COSMETIC_ARMOR_SLOTS;
        }

        if (hasCurios() && player != null) {
            int curiosSlots = CuriosIntegration.getCuriosSlotCount(player);
            size += curiosSlots;
            Gravestones.LOGGER.debug("Player has {} curio slots, adding to gravestone size", curiosSlots);
        }

        return size;
    }

    public static int calculateInventorySize() {
        int size = GRAVE_MAIN_INVENTORY_SLOTS + GRAVE_HOTBAR_SLOTS + BASE_ARMOR_SLOTS + BASE_OFFHAND_SLOTS;

        if (hasCosmeticArmor()) {
            size += COSMETIC_ARMOR_SLOTS;
        }

        if (hasCurios()) {
            size += 24;
            Gravestones.LOGGER.debug("Using default curio slot count of 24 for gravestone size calculation");
        }

        return size;
    }

    public static void init() {
        CosmeticArmorIntegration.init();
        CuriosIntegration.init();

        Gravestones.LOGGER.info("Mod integration status - CosmeticArmor: {}, Curios: {}",
                hasCosmeticArmor(), hasCurios());
    }

    public static void storePlayerInventory(Player player, ItemStackHandler graveHandler) {
        Gravestones.LOGGER.info("=== STORING PLAYER INVENTORY ===");
        Gravestones.LOGGER.info("Cosmetic Armor available: {}", hasCosmeticArmor());
        Gravestones.LOGGER.info("Curios available: {}", hasCurios());
        Gravestones.LOGGER.info("Gravestone handler size: {}", graveHandler.getSlots());

        for (int i = 0; i < 27; i++) {
            ItemStack item = player.getInventory().items.get(i + 9);
            if (!item.isEmpty()) {
                graveHandler.setStackInSlot(i, item.copy());
                Gravestones.LOGGER.info("Stored main inventory item {} in slot {}", item.getDisplayName().getString(),
                        i);
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().items.get(i);
            if (!item.isEmpty()) {
                graveHandler.setStackInSlot(27 + i, item.copy());
                Gravestones.LOGGER.info("Stored hotbar item {} in slot {}", item.getDisplayName().getString(), 27 + i);
            }
        }

        for (int i = 0; i < 4; i++) {
            ItemStack item = player.getInventory().armor.get(i);
            if (!item.isEmpty()) {
                graveHandler.setStackInSlot(36 + i, item.copy());
                Gravestones.LOGGER.info("Stored armor item {} in slot {}", item.getDisplayName().getString(), 36 + i);
            }
        }

        ItemStack offhandItem = player.getInventory().offhand.get(0);
        if (!offhandItem.isEmpty()) {
            graveHandler.setStackInSlot(40, offhandItem.copy());
            Gravestones.LOGGER.info("Stored offhand item {} in slot {}", offhandItem.getDisplayName().getString(), 40);
        }

        int nextSlot = 41;
        Gravestones.LOGGER.info("Starting mod integration storage at slot {}", nextSlot);

        if (hasCosmeticArmor()) {
            Gravestones.LOGGER.info("Storing cosmetic armor starting at slot {}", nextSlot);
            nextSlot = CosmeticArmorIntegration.storeCosmeticArmor(player, graveHandler, nextSlot);
            Gravestones.LOGGER.info("Cosmetic armor storage finished, next slot: {}", nextSlot);
        } else {
            Gravestones.LOGGER.info("Cosmetic armor not available, skipping slots 41-44");
            nextSlot += 4;
        }

        if (hasCurios()) {
            Gravestones.LOGGER.info("Storing curios starting at slot {}", nextSlot);
            nextSlot = storeCurios(player, graveHandler, nextSlot);
            Gravestones.LOGGER.info("Curios storage finished, next slot: {}", nextSlot);
        } else {
            Gravestones.LOGGER.info("Curios not available, skipping curios storage");
        }

        Gravestones.LOGGER.info("=== STORAGE COMPLETE - {} total slots used ===", nextSlot);

        for (int i = 0; i < graveHandler.getSlots(); i++) {
            ItemStack item = graveHandler.getStackInSlot(i);
            if (!item.isEmpty()) {
                Gravestones.LOGGER.info("Gravestone slot {}: {}", i, item.getDisplayName().getString());
            }
        }
    }

    public static void restorePlayerInventory(Player player, ItemStackHandler graveHandler) {
        Gravestones.LOGGER.info("=== RESTORING PLAYER INVENTORY ===");
        Gravestones.LOGGER.info("Cosmetic Armor available: {}", hasCosmeticArmor());
        Gravestones.LOGGER.info("Curios available: {}", hasCurios());
        Gravestones.LOGGER.info("Gravestone handler size: {}", graveHandler.getSlots());

        for (int i = 0; i < graveHandler.getSlots(); i++) {
            ItemStack item = graveHandler.getStackInSlot(i);
            if (!item.isEmpty()) {
                Gravestones.LOGGER.info("Gravestone slot {} contains: {}", i, item.getDisplayName().getString());
            }
        }

        List<ItemStack> itemsToDropAtEnd = new ArrayList<>();

        Gravestones.LOGGER.info("Restoring armor from slots 36-39");
        restoreArmor(player, graveHandler, itemsToDropAtEnd);

        Gravestones.LOGGER.info("Restoring offhand from slot 40");
        restoreOffhand(player, graveHandler, itemsToDropAtEnd);

        Gravestones.LOGGER.info("Restoring main inventory from slots 0-26");
        restoreMainInventory(player, graveHandler, itemsToDropAtEnd);

        Gravestones.LOGGER.info("Restoring hotbar from slots 27-35");
        restoreHotbar(player, graveHandler, itemsToDropAtEnd);

        int nextSlot = 41;
        Gravestones.LOGGER.info("Starting mod integration restoration at slot {}", nextSlot);

        if (hasCosmeticArmor()) {
            Gravestones.LOGGER.info("Restoring cosmetic armor starting at slot {}", nextSlot);
            nextSlot = CosmeticArmorIntegration.restoreCosmeticArmor(player, graveHandler, nextSlot, itemsToDropAtEnd);
            Gravestones.LOGGER.info("Cosmetic armor restoration finished, next slot: {}", nextSlot);
        } else {
            Gravestones.LOGGER.info("Cosmetic armor not available, clearing slots 41-44");
            for (int i = 0; i < 4 && (nextSlot + i) < graveHandler.getSlots(); i++) {
                ItemStack stack = graveHandler.getStackInSlot(nextSlot + i);
                if (!stack.isEmpty()) {
                    itemsToDropAtEnd.add(stack);
                    graveHandler.setStackInSlot(nextSlot + i, ItemStack.EMPTY);
                    Gravestones.LOGGER.info("Dropping item from cosmetic armor slot {}: {}", nextSlot + i,
                            stack.getDisplayName().getString());
                }
            }
            nextSlot += 4;
        }

        if (hasCurios()) {
            Gravestones.LOGGER.info("Restoring curios starting at slot {}", nextSlot);
            nextSlot = restoreCurios(player, graveHandler, nextSlot, itemsToDropAtEnd);
            Gravestones.LOGGER.info("Curios restoration finished, next slot: {}", nextSlot);
        } else {
            Gravestones.LOGGER.info("Curios not available, clearing remaining slots starting at {}", nextSlot);
            nextSlot = restoreCurios(player, graveHandler, nextSlot, itemsToDropAtEnd);
        }

        if (!player.isCreative()) {
            for (ItemStack stackToDrop : itemsToDropAtEnd) {
                if (!stackToDrop.isEmpty()) {
                    player.drop(stackToDrop, false);
                    Gravestones.LOGGER.info("Dropped item: {}", stackToDrop.getDisplayName().getString());
                }
            }
        }

        Gravestones.LOGGER.info("=== RESTORATION COMPLETE - {} items dropped ===", itemsToDropAtEnd.size());
    }

    private static int storeCurios(Player player, ItemStackHandler graveHandler, int startSlot) {
        if (hasCurios()) {
            return CuriosIntegration.storeCurios(player, graveHandler, startSlot);
        }
        Gravestones.LOGGER.debug("Curios not available - no slots reserved starting at {}", startSlot);
        return startSlot;
    }

    private static void restoreArmor(Player player, ItemStackHandler graveHandler, List<ItemStack> itemsToDropAtEnd) {
        for (int i = 0; i < 4; i++) {
            int graveSlot = 36 + i;
            ItemStack armorPiece = graveHandler.getStackInSlot(graveSlot);
            if (!armorPiece.isEmpty()) {
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
        for (int i = 0; i < 27; i++) {
            ItemStack item = graveHandler.getStackInSlot(i);
            if (!item.isEmpty()) {
                int playerSlot = i + 9;
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
        for (int i = 0; i < 9; i++) {
            int graveSlot = 27 + i;
            ItemStack item = graveHandler.getStackInSlot(graveSlot);
            if (!item.isEmpty()) {
                int playerSlot = i;

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
        if (hasCurios()) {
            return CuriosIntegration.restoreCurios(player, graveHandler, startSlot, itemsToDropAtEnd);
        }

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