package es.boopurno.gravestones.menu;

import es.boopurno.gravestones.Gravestones;
import es.boopurno.gravestones.block.entity.GravestoneBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class GravestoneMenu extends AbstractContainerMenu {
    private final GravestoneBlockEntity blockEntity;
    private final IItemHandler graveInventory;
    private final Player player;
    private final int yOffset;

    public static final int HOTBAR_SLOTS = 9;
    public static final int MAIN_INVENTORY_SLOTS = 27;
    public static final int ARMOR_SLOTS = 4;
    public static final int OFFHAND_SLOTS = 1;
    public static final int COSMETIC_ARMOR_SLOTS = 4;

    public GravestoneMenu(int pContainerId, Inventory playerInventory, GravestoneBlockEntity blockEntity,
            IItemHandler graveInventory) {
        super(Gravestones.GRAVESTONE_MENU_TYPE.get(), pContainerId);
        this.blockEntity = blockEntity;
        this.graveInventory = graveInventory;
        this.player = playerInventory.player;
        this.yOffset = blockEntity.hasCosmeticArmor() ? 18 : 0;

        Gravestones.LOGGER.info("DEBUG: GravestoneMenu constructor - curiosItemCount={}",
                blockEntity.getCuriosItemCount());

        for (int i = 0; i < graveInventory.getSlots(); i++) {
            ItemStack stack = graveInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Gravestones.LOGGER.info("DEBUG: Gravestone slot {} contains: {}",
                        i, stack.getDisplayName().getString());
            }
        }

        int slotIndex = 0;

        slotIndex = addGraveMainInventorySlots(slotIndex);
        slotIndex = addGraveHotbarSlots(slotIndex);
        slotIndex = addArmorSlots(slotIndex);
        slotIndex = addOffhandSlot(slotIndex);
        if (blockEntity.hasCosmeticArmor()) {
            slotIndex = addCosmeticArmorSlots(slotIndex);
        }
        if (blockEntity.getCuriosItemCount() > 0) {
            slotIndex = addCuriosSlots(slotIndex);
        }

        addPlayerInventorySlots(playerInventory);
        addPlayerHotbarSlots(playerInventory);
    }

    private int addGraveMainInventorySlots(int startIndex) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int slotIndex = startIndex + row * 9 + col;
                if (slotIndex < blockEntity.getMaxInventorySize()) {
                    int x = 8 + col * 18;
                    int y = 40 + row * 18 + yOffset;
                    this.addSlot(new SlotItemHandler(this.graveInventory, slotIndex, x, y) {
                        @Override
                        public boolean mayPlace(@NotNull ItemStack stack) {
                            return false;
                        }
                    });
                }
            }
        }
        return startIndex + 27;
    }

    private int addGraveHotbarSlots(int startIndex) {
        for (int col = 0; col < 9; ++col) {
            int slotIndex = startIndex + col;
            if (slotIndex < blockEntity.getMaxInventorySize()) {
                int x = 8 + col * 18;
                int y = 98 + yOffset;
                this.addSlot(new SlotItemHandler(this.graveInventory, slotIndex, x, y) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        return startIndex + 9;
    }

    private int addArmorSlots(int startIndex) {
        for (int i = 0; i < 4; i++) {
            int slotIndex = startIndex + i;
            if (slotIndex < blockEntity.getMaxInventorySize()) {
                int x = 8 + i * 18;
                int y = 18 + yOffset;
                this.addSlot(new SlotItemHandler(this.graveInventory, slotIndex, x, y) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        return startIndex + 4;
    }

    private int addCosmeticArmorSlots(int startIndex) {
        for (int i = 0; i < 4; i++) {
            int slotIndex = startIndex + i;
            if (slotIndex < blockEntity.getMaxInventorySize()) {
                int x = 8 + i * 18;
                int y = 18;
                this.addSlot(new SlotItemHandler(this.graveInventory, slotIndex, x, y) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        return startIndex + 4;
    }

    private int addOffhandSlot(int startIndex) {
        int slotIndex = startIndex;
        if (slotIndex < blockEntity.getMaxInventorySize()) {
            int x = 80;
            int y = 18 + yOffset;
            this.addSlot(new SlotItemHandler(this.graveInventory, slotIndex, x, y) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });
        }
        return startIndex + 1;
    }

    private int addCuriosSlots(int startIndex) {
        int curiosCount = blockEntity.getCuriosItemCount();
        int slotsPerColumn = 6;

        Gravestones.LOGGER.info("DEBUG: addCuriosSlots called with startIndex={}, curiosCount={}",
                startIndex, curiosCount);

        for (int i = 0; i < curiosCount; i++) {
            int slotIndex = startIndex + i;
            if (slotIndex < blockEntity.getMaxInventorySize()) {
                int column = i / slotsPerColumn;
                int row = i % slotsPerColumn;
                int x = -25 - column * 18;
                int y = 0 + row * 18 + yOffset;

                Gravestones.LOGGER.info("DEBUG: Creating curios slot {} at position ({}, {})",
                        slotIndex, x, y);

                this.addSlot(new SlotItemHandler(this.graveInventory, slotIndex, x, y) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        return startIndex + curiosCount;
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int x = 8 + col * 18;
                int y = 153 + row * 18 + yOffset;
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, x, y));
            }
        }
    }

    private void addPlayerHotbarSlots(Inventory playerInventory) {
        for (int col = 0; col < 9; ++col) {
            int x = 8 + col * 18;
            int y = 211 + yOffset;
            this.addSlot(new Slot(playerInventory, col, x, y));
        }
    }

    public GravestoneMenu(int pContainerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(pContainerId, playerInventory, getResolvedBlockEntity(playerInventory, extraData));
    }

    private GravestoneMenu(int pContainerId, Inventory playerInventory, ResolvedBEData resolvedData) {
        this(pContainerId, playerInventory, resolvedData.blockEntity, resolvedData.itemHandler);
    }

    private static ResolvedBEData getResolvedBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        final BlockPos pos = extraData.readBlockPos();
        final BlockEntity be = playerInventory.player.level().getBlockEntity(pos);

        if (be instanceof GravestoneBlockEntity gravestoneBE) {
            IItemHandler itemHandler = gravestoneBE.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                    () -> new IllegalStateException(
                            "Item handler capability not found on GravestoneBlockEntity at " + pos));
            return new ResolvedBEData(gravestoneBE, itemHandler);
        }
        throw new IllegalStateException("Incorrect BlockEntity type at position: " + pos + " found " + be);
    }

    private record ResolvedBEData(GravestoneBlockEntity blockEntity, IItemHandler itemHandler) {
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), pPlayer,
                Gravestones.GRAVESTONE_BLOCK.get());
    }

    public void handleTransferItems() {
        if (blockEntity != null && !blockEntity.getLevel().isClientSide()) {
            blockEntity.transferItemsToPlayer(this.player);
            this.broadcastChanges();
        }
    }

    public GravestoneBlockEntity getBlockEntity() {
        return this.blockEntity;
    }
}