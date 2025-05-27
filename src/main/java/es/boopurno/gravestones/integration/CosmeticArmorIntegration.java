package es.boopurno.gravestones.integration;

import es.boopurno.gravestones.Gravestones;
import lain.mods.cos.api.CosArmorAPI;
import lain.mods.cos.api.inventory.CAStacksBase;
import lain.mods.cos.api.event.CosArmorDeathDrops;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

/**
 * Integration with CosmeticArmorReworked mod using the proper API
 */
public class CosmeticArmorIntegration {

    private static final String COSMETIC_ARMOR_MODID = "cosmeticarmorreworked";
    private static boolean isLoaded = false;

    public static void init() {
        isLoaded = ModList.get().isLoaded(COSMETIC_ARMOR_MODID);
        if (isLoaded) {
            Gravestones.LOGGER.info("CosmeticArmorReworked detected, registering integration");
            MinecraftForge.EVENT_BUS.register(CosmeticArmorIntegration.class);
        } else {
            Gravestones.LOGGER.debug("CosmeticArmorReworked not found, skipping integration");
        }
    }

    public static boolean isLoaded() {
        return isLoaded;
    }

    /**
     * Store cosmetic armor items from player to gravestone
     */
    public static int storeCosmeticArmor(Player player, ItemStackHandler graveHandler, int startSlot) {
        if (!isLoaded)
            return startSlot;

        try {
            CAStacksBase cosmeticInventory = CosArmorAPI.getCAStacks(player.getUUID());
            if (cosmeticInventory != null) {
                // Store cosmetic armor items (4 slots: feet, legs, chest, head)
                for (int i = 0; i < 4; i++) {
                    ItemStack cosmeticItem = cosmeticInventory.getStackInSlot(i);
                    if (!cosmeticItem.isEmpty()) {
                        graveHandler.setStackInSlot(startSlot + i, cosmeticItem.copy());
                        Gravestones.LOGGER.debug("Stored cosmetic armor item {} in grave slot {}",
                                cosmeticItem.getDisplayName().getString(), startSlot + i);
                    }
                }

                // Clear cosmetic armor from player after storing
                for (int i = 0; i < 4; i++) {
                    cosmeticInventory.setStackInSlot(i, ItemStack.EMPTY);
                }

                return startSlot + 4; // Return next available slot
            }
        } catch (Exception e) {
            Gravestones.LOGGER.error("Error storing cosmetic armor: ", e);
        }

        return startSlot;
    }

    /**
     * Restore cosmetic armor items from gravestone to player
     */
    public static int restoreCosmeticArmor(Player player, ItemStackHandler graveHandler, int startSlot,
            List<ItemStack> itemsToDropAtEnd) {
        if (!isLoaded)
            return startSlot;

        try {
            CAStacksBase cosmeticInventory = CosArmorAPI.getCAStacks(player.getUUID());
            if (cosmeticInventory != null) {
                // Restore cosmetic armor items from gravestone (4 slots: feet, legs, chest,
                // head)
                for (int i = 0; i < 4; i++) {
                    int graveSlot = startSlot + i;
                    ItemStack cosmeticItem = graveHandler.getStackInSlot(graveSlot);
                    if (!cosmeticItem.isEmpty()) {
                        ItemStack existingItem = cosmeticInventory.getStackInSlot(i);

                        if (existingItem.isEmpty()) {
                            // Slot is empty, we can place the item directly
                            cosmeticInventory.setStackInSlot(i, cosmeticItem.copy());
                            graveHandler.setStackInSlot(graveSlot, ItemStack.EMPTY);
                            Gravestones.LOGGER.debug("Restored cosmetic armor item {} to slot {}",
                                    cosmeticItem.getDisplayName().getString(), i);
                        } else {
                            // Slot is occupied, try to add to player's inventory instead
                            if (player.getInventory().add(cosmeticItem.copy())) {
                                // Successfully added to inventory
                                graveHandler.setStackInSlot(graveSlot, ItemStack.EMPTY);
                                Gravestones.LOGGER.debug(
                                        "Added cosmetic armor item {} to player inventory (slot {} was occupied by {})",
                                        cosmeticItem.getDisplayName().getString(), i,
                                        existingItem.getDisplayName().getString());
                            } else {
                                // Inventory is full, add to items to drop at the end
                                itemsToDropAtEnd.add(cosmeticItem.copy());
                                graveHandler.setStackInSlot(graveSlot, ItemStack.EMPTY);
                                Gravestones.LOGGER.debug(
                                        "Will drop cosmetic armor item {} (slot {} occupied and inventory full)",
                                        cosmeticItem.getDisplayName().getString(), i);
                            }
                        }
                    }
                }

                return startSlot + 4; // Return next available slot
            }
        } catch (Exception e) {
            Gravestones.LOGGER.error("Error restoring cosmetic armor: ", e);
        }

        return startSlot;
    }

    /**
     * Event handler to prevent cosmetic armor from dropping when player dies
     * This cancels the default cosmetic armor death drops since we handle it in the
     * gravestone
     */
    @SubscribeEvent
    public static void onCosmeticArmorDeathDrops(CosArmorDeathDrops event) {
        // Cancel the event to prevent cosmetic armor from dropping
        // We handle the cosmetic armor storage in our gravestone system
        event.setCanceled(true);

        Gravestones.LOGGER.debug("Prevented cosmetic armor death drops for player {}, " +
                "items will be stored in gravestone instead",
                event.getEntityPlayer().getName().getString());
    }
}