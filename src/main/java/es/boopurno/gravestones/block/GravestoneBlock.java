package es.boopurno.gravestones.block;

import es.boopurno.gravestones.Gravestones;
import es.boopurno.gravestones.block.entity.GravestoneBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GravestoneBlock extends HorizontalDirectionalBlock implements EntityBlock, SimpleWaterloggedBlock {
    public static final VoxelShape SHAPE = makeShape();
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public GravestoneBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, Boolean.FALSE));
    }

    public static VoxelShape makeShape() {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Shapes.box(0.375, 0.0625, 0.75, 0.625, 0.8125, 0.875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.1875, 0.0625, 0.25, 0.8125, 0.125, 0.75), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.4375, 0.8125, 0.75, 0.5625, 0.875, 0.875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.625, 0.3125, 0.75, 0.6875, 0.6875, 0.875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.3125, 0.75, 0.375, 0.6875, 0.875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.6875, 0.375, 0.75, 0.8125, 0.625, 0.875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.1875, 0.375, 0.75, 0.3125, 0.625, 0.875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.3125, 0.0625, 0.1875, 0.75, 0.125, 0.25), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.625, 0.0625, 0.75, 0.8125, 0.125, 0.8125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.625, 0.0625, 0.8125, 0.75, 0.125, 0.875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.25, 0.0625, 0.75, 0.375, 0.125, 0.8125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.1875, 0, 0.0625, 0.8125, 0.0625, 0.125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.125, 0, 0.125, 0.875, 0.0625, 0.875), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.1875, 0, 0.875, 0.8125, 0.0625, 0.9375), BooleanOp.OR);

        return shape;
    }

    public static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[] { shape, Shapes.empty() };

        int times = (to.get2DDataValue() - from.get2DDataValue() + 4) % 4;
        for (int i = 0; i < times; i++) {
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = Shapes.or(buffer[1],
                    Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = Shapes.empty();
        }

        return buffer[0];
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return switch (pState.getValue(FACING)) {
            case NORTH -> SHAPE;
            case SOUTH -> rotateShape(Direction.NORTH, Direction.SOUTH, SHAPE);
            case WEST -> rotateShape(Direction.NORTH, Direction.WEST, SHAPE);
            case EAST -> rotateShape(Direction.NORTH, Direction.EAST, SHAPE);
            default -> SHAPE;
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext pContext) {
        FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
        return this.defaultBlockState()
                .setValue(FACING, pContext.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, fluidstate.is(FluidTags.WATER) && fluidstate.getAmount() == 8);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
        pBuilder.add(WATERLOGGED);
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        return new GravestoneBlockEntity(pPos, pState);
    }

    @Override
    public InteractionResult use(@NotNull BlockState pState, Level pLevel, @NotNull BlockPos pPos,
            @NotNull Player pPlayer, @NotNull InteractionHand pHand, @NotNull BlockHitResult pHit) {
        if (!pLevel.isClientSide()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof GravestoneBlockEntity gravestoneBE) {
                NetworkHooks.openScreen((ServerPlayer) pPlayer, gravestoneBE, pPos);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    @Override
    public void playerWillDestroy(Level pLevel, @NotNull BlockPos pPos, @NotNull BlockState pState,
            @NotNull Player pPlayer) {
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof GravestoneBlockEntity gravestoneBE && !pLevel.isClientSide) {
            gravestoneBE.transferItemsToPlayer(pPlayer);
            Gravestones.LOGGER.debug("Restored items to player {} from gravestone at {} via breaking",
                    pPlayer.getName().getString(), pPos);
        }
        super.playerWillDestroy(pLevel, pPos, pState, pPlayer);
    }

    @Override
    public void onRemove(BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos, BlockState pNewState,
            boolean pIsMoving) {
        if (pState.getBlock() != pNewState.getBlock()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof GravestoneBlockEntity gravestoneBE) {
                gravestoneBE.dropAllItems(pLevel, pPos);
            }
            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
        }
    }

    public void placeWithInventoryAndName(Level level, BlockPos pos, Player player, String ownerName) {
        if (level.isClientSide) {
            return;
        }

        BlockPos validPos = findValidGravestonePosition(level, pos);
        if (validPos == null) {
            Gravestones.LOGGER.error("Could not find valid position for gravestone near {}. Dropping items instead.",
                    pos);
            player.getInventory().dropAll();
            return;
        }

        Direction placementDirection = player.getDirection().getOpposite();
        FluidState fluidstate = level.getFluidState(validPos);
        BlockState graveState = this.defaultBlockState()
                .setValue(FACING, placementDirection)
                .setValue(WATERLOGGED, fluidstate.is(FluidTags.WATER) && fluidstate.getAmount() == 8);

        level.setBlock(validPos, graveState, 3);
        BlockEntity blockEntity = level.getBlockEntity(validPos);
        if (blockEntity instanceof GravestoneBlockEntity gravestoneBE) {
            gravestoneBE.setItemsOnDeath(player.getInventory());
            gravestoneBE.setOwnerName(ownerName);
            Gravestones.LOGGER.debug("GravestoneBlockEntity populated for {} at {}", ownerName, validPos);
        } else {
            Gravestones.LOGGER.error(
                    "Failed to get GravestoneBlockEntity after placing gravestone at {}. Inventory NOT saved to grave.",
                    validPos);
        }
    }

    private BlockPos findValidGravestonePosition(Level level, BlockPos originalPos) {
        BlockPos searchCenter = adjustSearchCenterForExtremeY(level, originalPos);

        if (isValidGravestonePosition(level, searchCenter)) {
            if (!searchCenter.equals(originalPos)) {
                Gravestones.LOGGER.debug("Adjusted search center from {} to {} due to extreme Y position",
                        originalPos, searchCenter);
            }
            return searchCenter;
        }

        final int maxSearchRadius = 16;

        for (int radius = 1; radius <= maxSearchRadius; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }

                    // Check both at same Y level and within a reasonable Y range
                    for (int yOffset = -2; yOffset <= 5; yOffset++) {
                        BlockPos testPos = searchCenter.offset(x, yOffset, z);
                        if (isValidGravestonePosition(level, testPos)) {
                            Gravestones.LOGGER.debug(
                                    "Found valid gravestone position at {} (offset from original death at {})",
                                    testPos, originalPos);
                            return testPos;
                        }
                    }
                }
            }
        }

        return null;
    }

    private BlockPos adjustSearchCenterForExtremeY(Level level, BlockPos originalPos) {
        int originalY = originalPos.getY();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        if (originalY < minY + 5) {
            BlockPos surfacePos = findNearestSurface(level, originalPos.getX(), originalPos.getZ());
            if (surfacePos != null) {
                Gravestones.LOGGER.debug("Death in void at Y={}, found surface at Y={}", originalY, surfacePos.getY());
                return surfacePos;
            }

            return new BlockPos(originalPos.getX(), minY + 5, originalPos.getZ());
        }

        if (originalY > maxY - 5) {
            for (int y = maxY - 5; y >= minY; y--) {
                BlockPos testPos = new BlockPos(originalPos.getX(), y, originalPos.getZ());
                if (level.getBlockState(testPos.below()).isSolidRender(level, testPos.below())) {
                    return testPos;
                }
            }

            return new BlockPos(originalPos.getX(), maxY - 5, originalPos.getZ());
        }

        return originalPos;
    }

    private BlockPos findNearestSurface(Level level, int x, int z) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        for (int radius = 0; radius <= 8; radius++) {
            for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                    if (radius > 0 && Math.abs(xOffset) != radius && Math.abs(zOffset) != radius) {
                        continue;
                    }

                    int testX = x + xOffset;
                    int testZ = z + zOffset;

                    for (int y = maxY - 1; y >= minY; y--) {
                        BlockPos testPos = new BlockPos(testX, y, testZ);
                        BlockState blockState = level.getBlockState(testPos);

                        if (blockState.isSolidRender(level, testPos)) {
                            BlockPos gravePos = testPos.above();
                            if (gravePos.getY() < maxY && isValidGravestonePosition(level, gravePos)) {
                                return gravePos;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean isValidGravestonePosition(Level level, BlockPos pos) {
        if (!level.isInWorldBounds(pos)) {
            return false;
        }

        BlockState blockState = level.getBlockState(pos);

        if (!blockState.canBeReplaced()) {
            return false;
        }

        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);

        if (!belowState.isSolidRender(level, belowPos) && !level.getFluidState(belowPos).is(FluidTags.WATER)) {
            return false;
        }

        FluidState fluidState = level.getFluidState(pos);
        if (!fluidState.isEmpty() && !fluidState.is(FluidTags.WATER)) {
            return false;
        }

        return true;
    }

    @Override
    public FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    @Override
    public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState,
            LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
        if (pState.getValue(WATERLOGGED)) {
            pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
        }
        return super.updateShape(pState, pDirection, pNeighborState, pLevel, pCurrentPos, pNeighborPos);
    }
}
