package es.boopurno.gravestones.event;

import es.boopurno.gravestones.Gravestones;
import es.boopurno.gravestones.block.GravestoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Gravestones.MODID)
public class DeathEvents {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            Level level = player.level();

            if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
                Gravestones.LOGGER.debug("keepInventory is true. Gravestone will not be placed for player {}",
                        player.getName().getString());
                return;
            }

            // Check if death was caused by explosion (creeper, TNT, etc.)
            boolean isExplosionDeath = isExplosionRelatedDeath(event.getSource());
            int delayTicks = isExplosionDeath ? 5 : 0; // 5 ticks = 0.25 seconds delay for explosions

            if (delayTicks > 0) {
                Gravestones.LOGGER.debug(
                        "Explosion death detected for player {}. Delaying gravestone placement by {} ticks.",
                        player.getName().getString(), delayTicks);
            }

            // Store the inventory immediately to prevent item loss during the delay
            java.util.List<ItemStack> storedInventory = new java.util.ArrayList<>();

            // Copy main inventory
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                storedInventory.add(player.getInventory().items.get(i).copy());
            }
            // Copy armor
            for (int i = 0; i < player.getInventory().armor.size(); i++) {
                storedInventory.add(player.getInventory().armor.get(i).copy());
            }
            // Copy offhand
            for (int i = 0; i < player.getInventory().offhand.size(); i++) {
                storedInventory.add(player.getInventory().offhand.get(i).copy());
            }

            // Clear inventory immediately to prevent vanilla drops
            player.getInventory().clearContent();

            BlockPos deathPos = player.blockPosition();
            String playerName = player.getGameProfile().getName();

            // Schedule gravestone placement
            ((ServerLevel) level).getServer().execute(() -> {
                scheduleGravestonePlacement(level, deathPos, player, playerName, storedInventory, delayTicks);
            });
        }
    }

    private static void scheduleGravestonePlacement(Level level, BlockPos deathPos, ServerPlayer player,
            String playerName, java.util.List<ItemStack> storedInventory, int delayTicks) {

        if (delayTicks <= 0) {
            // Place immediately
            placeGravestone(level, deathPos, player, playerName, storedInventory);
        } else {
            // Schedule for later
            ((ServerLevel) level).getServer().tell(new net.minecraft.server.TickTask(
                    ((ServerLevel) level).getServer().getTickCount() + delayTicks,
                    () -> placeGravestone(level, deathPos, player, playerName, storedInventory)));
        }
    }

    private static void placeGravestone(Level level, BlockPos deathPos, ServerPlayer player,
            String playerName, java.util.List<ItemStack> storedInventory) {

        if (Gravestones.GRAVESTONE_BLOCK.get() instanceof GravestoneBlock gravestoneBlockInstance) {
            // Temporarily restore inventory for the placement method
            restoreInventoryForPlacement(player, storedInventory);

            gravestoneBlockInstance.placeWithInventoryAndName(level, deathPos, player, playerName);
            Gravestones.LOGGER.info("Placed gravestone for player {} at {}", playerName, deathPos);

            // Clear inventory again after placement (the gravestone already copied it)
            player.getInventory().clearContent();
            Gravestones.LOGGER.debug("Cleared live inventory for player {} after copying to gravestone.", playerName);
        } else {
            Gravestones.LOGGER.error("GRAVESTONE_BLOCK is not an instance of GravestoneBlock! Cannot place grave.");
            // As fallback, drop the stored items
            dropStoredItems(level, deathPos, storedInventory);
        }
    }

    private static void restoreInventoryForPlacement(ServerPlayer player, java.util.List<ItemStack> storedInventory) {
        int index = 0;

        // Restore main inventory
        for (int i = 0; i < player.getInventory().items.size() && index < storedInventory.size(); i++) {
            player.getInventory().items.set(i, storedInventory.get(index).copy());
            index++;
        }
        // Restore armor
        for (int i = 0; i < player.getInventory().armor.size() && index < storedInventory.size(); i++) {
            player.getInventory().armor.set(i, storedInventory.get(index).copy());
            index++;
        }
        // Restore offhand
        for (int i = 0; i < player.getInventory().offhand.size() && index < storedInventory.size(); i++) {
            player.getInventory().offhand.set(i, storedInventory.get(index).copy());
            index++;
        }
    }

    private static void dropStoredItems(Level level, BlockPos pos, java.util.List<ItemStack> storedInventory) {
        for (ItemStack stack : storedInventory) {
            if (!stack.isEmpty()) {
                net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                level.addFreshEntity(itemEntity);
            }
        }
        Gravestones.LOGGER.warn("Dropped stored items at {} due to gravestone placement failure", pos);
    }

    private static boolean isExplosionRelatedDeath(DamageSource damageSource) {
        // Check if damage source indicates explosion
        if (damageSource.getMsgId().contains("explosion")) {
            return true;
        }

        // Check if damage source is from a creeper
        if (damageSource.getEntity() instanceof Creeper) {
            return true;
        }

        // Check for other explosion types
        String damageType = damageSource.getMsgId();
        return damageType.equals("explosion.player") ||
                damageType.equals("explosion") ||
                damageType.contains("tnt") ||
                damageType.contains("fireball");
    }
}