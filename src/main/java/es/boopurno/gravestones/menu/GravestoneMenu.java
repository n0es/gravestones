package es.boopurno.gravestones.menu;

import es.boopurno.gravestones.Gravestones;
import es.boopurno.gravestones.block.entity.GravestoneBlockEntity;
import es.boopurno.gravestones.integration.ModIntegration;
import net.minecraft.core.BlockPos; // Import BlockPos
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
    private final int yOffset; // 18 pixels down when cosmetic armor is present

    // Slot organization constants
    public static final int HOTBAR_SLOTS = 9;
    public static final int MAIN_INVENTORY_SLOTS = 27;
    public static final int ARMOR_SLOTS = 4;
    public static final int OFFHAND_SLOTS = 1;
    public static final int COSMETIC_ARMOR_SLOTS = 4;

    // Server-side constructor (updated)
    public GravestoneMenu(int pContainerId, Inventory playerInventory, GravestoneBlockEntity blockEntity,
            IItemHandler graveInventory) {
        super(Gravestones.GRAVESTONE_MENU_TYPE.get(), pContainerId);
        this.blockEntity = blockEntity;
        this.graveInventory = graveInventory;
        this.player = playerInventory.player;
        this.yOffset = blockEntity.hasCosmeticArmor() ? 18 : 0;

        Gravestones.LOGGER.info("DEBUG: GravestoneMenu constructor - curiosItemCount={}",
                blockEntity.getCuriosItemCount());

        // Debug: Log what items are in the gravestone
        for (int i = 0; i < graveInventory.getSlots(); i++) {
            ItemStack stack = graveInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Gravestones.LOGGER.info("DEBUG: Gravestone slot {} contains: {}",
                        i, stack.getDisplayName().getString());
            }
        }

        int slotIndex = 0;

        // Add gravestone inventory slots with exact positioning
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

        // Add player inventory and hotbar at exact positions
        addPlayerInventorySlots(playerInventory);
        addPlayerHotbarSlots(playerInventory);
    }

    private int addGraveMainInventorySlots(int startIndex) {
        // Grave inventory: 8,40 through 152,76 (with offset if cosmetic armor present)
        // This is a 3x9 grid: 3 rows, 9 columns
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int slotIndex = startIndex + row * 9 + col;
                if (slotIndex < blockEntity.getMaxInventorySize()) {
                    int x = 8 + col * 18; // 8 to 152 (8 + 8*18 = 152)
                    int y = 40 + row * 18 + yOffset; // 40 to 76
                    this.addSlot(new SlotItemHandler(this.graveInventory, slotIndex, x, y) {
                        @Override
                        public boolean mayPlace(@NotNull ItemStack stack) {
                            return false; // Read-only
                        }
                    });
                }
            }
        }
        return startIndex + 27; // 3x9 = 27 slots
    }

    private int addGraveHotbarSlots(int startIndex) {
        // Grave hotbar: 8,98 through 152,98 (with offset if cosmetic armor present)
        // This is a 1x9 grid
        for (int col = 0; col < 9; ++col) {
            int slotIndex = startIndex + col;
            if (slotIndex < blockEntity.getMaxInventorySize()) {
                int x = 8 + col * 18; // 8 to 152
                int y = 98 + yOffset;
                this.addSlot(new SlotItemHandler(this.graveInventory, slotIndex, x, y) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return false; // Read-only
                    }
                });
            }
        }
        return startIndex + 9; // 9 hotbar slots
    }

    private int addArmorSlots(int startIndex) {
        // Armor slots: 8,18 through 71,33 (with offset if cosmetic armor present)
        // 4 slots: helmet, chestplate, leggings, boots (NOT including offhand)
        for (int i = 0; i < 4; i++) {
            int slotIndex = startIndex + i;
            if (slotIndex < blockEntity.getMaxInventorySize()) {
                int x = 8 + i * 18; // 8, 26, 44, 62
                int y = 18 + yOffset;
                this.addSlot(new SlotItemHandler(this.graveInventory, slotIndex, x, y) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return false; // Read-only
                    }
                });
            }
        }
        return startIndex + 4; // 4 armor slots (NOT including offhand)
    }

    private int addCosmeticArmorSlots(int startIndex) {
        // Cosmetic armor slots: positioned above regular armor slots
        // Same x positions as regular armor, but at y=18 (before yOffset is applied to
        // regular armor)
        for (int i = 0; i < 4; i++) {
            int slotIndex = startIndex + i;
            if (slotIndex < blockEntity.getMaxInventorySize()) {
                int x = 8 + i * 18; // Same x positions as regular armor: 8, 26, 44, 62
                int y = 18; // Fixed position above regular armor (before yOffset)
                this.addSlot(new SlotItemHandler(this.graveInventory, slotIndex, x, y) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return false; // Read-only
                    }
                });
            }
        }
        return startIndex + 4; // 4 cosmetic armor slots
    }

    private int addOffhandSlot(int startIndex) {
        // Offhand slot: positioned at x=80 (after the 4 armor slots)
        int slotIndex = startIndex;
        if (slotIndex < blockEntity.getMaxInventorySize()) {
            int x = 80; // After the 4 armor slots (8 + 4*18 = 80)
            int y = 18 + yOffset;
            this.addSlot(new SlotItemHandler(this.graveInventory, slotIndex, x, y) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false; // Read-only
                }
            });
        }
        return startIndex + 1; // 1 offhand slot
    }

    private int addCuriosSlots(int startIndex) {
        // Add curios slots dynamically based on actual curios items stored
        int curiosCount = blockEntity.getCuriosItemCount();
        int slotsPerColumn = 6;

        Gravestones.LOGGER.info("DEBUG: addCuriosSlots called with startIndex={}, curiosCount={}",
                startIndex, curiosCount);

        for (int i = 0; i < curiosCount; i++) {
            int slotIndex = startIndex + i;
            if (slotIndex < blockEntity.getMaxInventorySize()) {
                int column = i / slotsPerColumn;
                int row = i % slotsPerColumn;
                int x = -25 - column * 18; // To the left of main GUI (negative x coordinates)
                int y = 0 + row * 18 + yOffset; // Start at same height as main inventory

                Gravestones.LOGGER.info("DEBUG: Creating curios slot {} at position ({}, {})",
                        slotIndex, x, y);

                this.addSlot(new SlotItemHandler(this.graveInventory, slotIndex, x, y) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return false; // Read-only
                    }
                });
            }
        }
        return startIndex + curiosCount;
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        // Player inventory: 8,153 through 169,206 (with offset if cosmetic armor
        // present)
        // This is a 3x9 grid
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int x = 8 + col * 18; // 8 to 152
                int y = 153 + row * 18 + yOffset; // 153 to 189 (153 + 2*18)
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, x, y));
            }
        }
    }

    private void addPlayerHotbarSlots(Inventory playerInventory) {
        // Player hotbar: 8,211 through 152,211 (with offset if cosmetic armor present)
        for (int col = 0; col < 9; ++col) {
            int x = 8 + col * 18; // 8 to 152
            int y = 211 + yOffset;
            this.addSlot(new Slot(playerInventory, col, x, y));
        }
    }

    // Client-side constructor (Corrected)
    public GravestoneMenu(int pContainerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(pContainerId, playerInventory, getResolvedBlockEntity(playerInventory, extraData));
    }

    // Helper constructor to pass the resolved BlockEntity and its ItemHandler
    private GravestoneMenu(int pContainerId, Inventory playerInventory, ResolvedBEData resolvedData) {
        this(pContainerId, playerInventory, resolvedData.blockEntity, resolvedData.itemHandler);
    }

    // Static helper method to read from buffer and resolve BE and its capability
    private static ResolvedBEData getResolvedBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        final BlockPos pos = extraData.readBlockPos(); // Read BlockPos ONCE
        final BlockEntity be = playerInventory.player.level().getBlockEntity(pos);

        if (be instanceof GravestoneBlockEntity gravestoneBE) {
            IItemHandler itemHandler = gravestoneBE.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(
                    () -> new IllegalStateException(
                            "Item handler capability not found on GravestoneBlockEntity at " + pos) // Corrected
                                                                                                    // orElseThrow
            );
            return new ResolvedBEData(gravestoneBE, itemHandler);
        }
        throw new IllegalStateException("Incorrect BlockEntity type at position: " + pos + " found " + be);
    }

    // Inner record (or class) to hold the resolved BE and its item handler
    private record ResolvedBEData(GravestoneBlockEntity blockEntity, IItemHandler itemHandler) {
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        // Completely disable shift-clicking to prevent any automatic item movement
        // Items should only be transferred via the transfer button
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