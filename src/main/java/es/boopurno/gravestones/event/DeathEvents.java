package es.boopurno.gravestones.event;

import es.boopurno.gravestones.Gravestones;
import es.boopurno.gravestones.block.GravestoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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

            BlockPos deathPos = player.blockPosition();
            BlockPos placementPos = deathPos;
            BlockState stateAtPlacementPos = level.getBlockState(placementPos);

            if (!stateAtPlacementPos.canBeReplaced()) {
                placementPos = placementPos.above();
                stateAtPlacementPos = level.getBlockState(placementPos);
            }

            if (!stateAtPlacementPos.canBeReplaced()) {
                Gravestones.LOGGER.warn(
                        "Could not find a suitable replaceable spot for gravestone for player {} at or above {}",
                        player.getName().getString(), deathPos);
                // TODO: wider search- always place a gravestone.
                return;
            }

            if (Gravestones.GRAVESTONE_BLOCK.get() instanceof GravestoneBlock gravestoneBlockInstance) {
                String playerName = player.getGameProfile().getName();

                gravestoneBlockInstance.placeWithInventoryAndName(level, placementPos, player, playerName);
                Gravestones.LOGGER.info("Placed gravestone for player {} at {}", playerName, placementPos);

                player.getInventory().clearContent();
                Gravestones.LOGGER.debug("Cleared live inventory for player {} after copying to gravestone.",
                        playerName);
            } else {
                Gravestones.LOGGER.error("GRAVESTONE_BLOCK is not an instance of GravestoneBlock! Cannot place grave.");
            }
        }
    }
}