package es.boopurno.gravestones.client.gui;

import es.boopurno.gravestones.Gravestones;
import es.boopurno.gravestones.block.entity.GravestoneBlockEntity;
import es.boopurno.gravestones.menu.GravestoneMenu;
import es.boopurno.gravestones.network.PacketHandler;
import es.boopurno.gravestones.network.serverbound.ServerboundTransferItemsPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GravestoneScreen extends AbstractContainerScreen<GravestoneMenu> {
    // Custom textures
    private static final ResourceLocation GRAVESTONE_VANILLA_TEXTURE = new ResourceLocation(Gravestones.MODID,
            "textures/gui/gravestone_vanilla.png");
    private static final ResourceLocation GRAVESTONE_EXTRA_TEXTURE = new ResourceLocation(Gravestones.MODID,
            "textures/gui/gravestone_extra.png");
    private static final ResourceLocation CURIOS_PANEL_TEXTURE = new ResourceLocation(Gravestones.MODID,
            "textures/gui/panel.png");
    private static final ResourceLocation SLOT_TEXTURE = new ResourceLocation(Gravestones.MODID,
            "textures/gui/slot.png");

    // Empty armor slot textures
    private static final ResourceLocation EMPTY_ARMOR_SLOT_HELMET = new ResourceLocation("minecraft",
            "textures/item/empty_armor_slot_helmet.png");
    private static final ResourceLocation EMPTY_ARMOR_SLOT_CHESTPLATE = new ResourceLocation("minecraft",
            "textures/item/empty_armor_slot_chestplate.png");
    private static final ResourceLocation EMPTY_ARMOR_SLOT_LEGGINGS = new ResourceLocation("minecraft",
            "textures/item/empty_armor_slot_leggings.png");
    private static final ResourceLocation EMPTY_ARMOR_SLOT_BOOTS = new ResourceLocation("minecraft",
            "textures/item/empty_armor_slot_boots.png");
    private static final ResourceLocation EMPTY_ARMOR_SLOT_SHIELD = new ResourceLocation("minecraft",
            "textures/item/empty_armor_slot_shield.png");

    private final boolean hasCosmeticArmor;
    private final boolean hasCurios;
    private final int yOffset; // 18 pixels down when cosmetic armor is present

    public GravestoneScreen(GravestoneMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);

        GravestoneBlockEntity blockEntity = pMenu.getBlockEntity();
        this.hasCosmeticArmor = blockEntity != null && blockEntity.hasCosmeticArmor();
        this.hasCurios = blockEntity != null && blockEntity.hasCurios();

        //
        // Calculate offset and size
        this.yOffset = hasCosmeticArmor ? 18 : 0;

        // Keep the main GUI size as original - we'll handle curios positioning
        // separately
        this.imageWidth = 176; // Always use original width for centering
        this.imageHeight = hasCosmeticArmor ? 256 : 238; // Adjusted for extra height when cosmetic armor present

        // Position labels - keep main GUI labels in original position
        this.titleLabelX = 8; // Keep title at original position
        this.titleLabelY = 6; // Keep title at fixed position, don't add yOffset here
        this.inventoryLabelX = 8; // Keep inventory label at original position
        this.inventoryLabelY = 140 + yOffset; // Player inventory label moves with offset
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Position transfer button between grave inventory and player inventory
        // Button is centered on the main texture (which is now always centered)
        int buttonY = y + 118 + yOffset;
        int buttonX = x + (176 / 2) - 50; // Center on the main texture

        this.addRenderableWidget(
                Button.builder(Component.translatable("gui.gravestones.button.transfer_items"), (button) -> {
                    if (this.menu.getBlockEntity() != null) {
                        PacketHandler.sendToServer(
                                new ServerboundTransferItemsPacket(this.menu.getBlockEntity().getBlockPos()));
                    }
                }).bounds(buttonX, buttonY, 100, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Main GUI is always centered - no offset needed
        int mainGuiX = x;
        int textureWidth = 176; // Original texture width

        // Use appropriate texture based on cosmetic armor presence
        ResourceLocation texture = hasCosmeticArmor ? GRAVESTONE_EXTRA_TEXTURE : GRAVESTONE_VANILLA_TEXTURE;
        pGuiGraphics.blit(texture, mainGuiX, y, 0, 0, textureWidth, imageHeight);

        // Render curios slots if needed - these go to the left of the centered main GUI
        if (hasCurios) {
            renderCuriosSlots(pGuiGraphics, x, y);
        }

        // Render cosmetic armor slot sprites if needed
        if (hasCosmeticArmor) {
            renderCosmeticArmorSlots(pGuiGraphics, mainGuiX, y);
        }

        // Render empty armor slot icons
        renderEmptyArmorSlotIcons(pGuiGraphics, mainGuiX, y);
    }

    private void renderCosmeticArmorSlots(GuiGraphics pGuiGraphics, int x, int y) {
        // Render 4 cosmetic armor slot sprites at y=18 (same as slot positions)
        int startX = x + 7; // Same x as slot positions
        int startY = y + 17; // Same y as slot positions

        for (int i = 0; i < 4; i++) { // Only 4 slots, no cosmetic offhand
            int slotX = startX + (i * 18);
            // Render slot background using your custom slot texture
            pGuiGraphics.blit(SLOT_TEXTURE, slotX, startY, 0, 0, 18, 18, 18, 18);
        }
    }

    private void renderCuriosSlots(GuiGraphics pGuiGraphics, int x, int y) {
        // Render curios slots dynamically based on actual curios items stored
        GravestoneBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null)
            return;

        int curiosCount = blockEntity.getCuriosItemCount();
        if (curiosCount == 0)
            return;

        int slotsPerColumn = 6;
        int columns = (curiosCount + slotsPerColumn - 1) / slotsPerColumn; // Ceiling division
        int rows = Math.min(curiosCount, slotsPerColumn);

        // Calculate panel dimensions
        int panelWidth = columns * 18 + 14; // 18 pixels per column + 14 for borders (7px on each side)
        int panelHeight = rows * 18 + 14; // 18 pixels per row + 14 for borders (7px on each side)

        // Calculate panel position (to the left of main GUI)
        int panelX = x - panelWidth - 1; // 3 pixels gap from main GUI
        int panelY = y + yOffset - 8; // Align with main inventory

        // Render the panel backdrop
        renderCuriosPanel(pGuiGraphics, panelX, panelY, panelWidth, panelHeight);

        // Render individual slot backgrounds
        for (int i = 0; i < curiosCount; i++) {
            int column = i / slotsPerColumn;
            int row = i % slotsPerColumn;

            // Reverse column order so partial columns appear on the left
            int reversedColumn = columns - 1 - column;

            // Slot positions relative to panel
            int slotX = panelX + 7 + reversedColumn * 18; // 7 pixels from panel edge
            int slotY = panelY + 7 + row * 18; // 7 pixels from panel edge

            // Render slot background using your custom slot texture
            pGuiGraphics.blit(SLOT_TEXTURE, slotX, slotY, 0, 0, 18, 18, 18, 18);
        }
    }

    /**
     * Renders a scalable panel backdrop using 9-slice approach with 24x24 texture
     * Treats the texture as a 3x3 grid of 8x8 sections for corners, edges, and
     * center
     */
    private void renderCuriosPanel(GuiGraphics pGuiGraphics, int x, int y, int width, int height) {
        int sliceSize = 8; // Each slice is 8x8 pixels

        // Render corners (fixed size, never stretch)
        // Top-left corner
        pGuiGraphics.blit(CURIOS_PANEL_TEXTURE, x, y, 0, 0, sliceSize, sliceSize, 24, 24);

        // Top-right corner
        pGuiGraphics.blit(CURIOS_PANEL_TEXTURE, x + width - sliceSize, y, 16, 0, sliceSize, sliceSize, 24, 24);

        // Bottom-left corner
        pGuiGraphics.blit(CURIOS_PANEL_TEXTURE, x, y + height - sliceSize, 0, 16, sliceSize, sliceSize, 24, 24);

        // Bottom-right corner
        pGuiGraphics.blit(CURIOS_PANEL_TEXTURE, x + width - sliceSize, y + height - sliceSize, 16, 16, sliceSize,
                sliceSize, 24, 24);

        // Render edges (tile to fill the space)
        // Top edge
        for (int i = sliceSize; i < width - sliceSize; i += sliceSize) {
            int segmentWidth = Math.min(sliceSize, width - sliceSize - i);
            pGuiGraphics.blit(CURIOS_PANEL_TEXTURE, x + i, y, 8, 0, segmentWidth, sliceSize, 24, 24);
        }

        // Bottom edge
        for (int i = sliceSize; i < width - sliceSize; i += sliceSize) {
            int segmentWidth = Math.min(sliceSize, width - sliceSize - i);
            pGuiGraphics.blit(CURIOS_PANEL_TEXTURE, x + i, y + height - sliceSize, 8, 16, segmentWidth, sliceSize, 24,
                    24);
        }

        // Left edge
        for (int j = sliceSize; j < height - sliceSize; j += sliceSize) {
            int segmentHeight = Math.min(sliceSize, height - sliceSize - j);
            pGuiGraphics.blit(CURIOS_PANEL_TEXTURE, x, y + j, 0, 8, sliceSize, segmentHeight, 24, 24);
        }

        // Right edge
        for (int j = sliceSize; j < height - sliceSize; j += sliceSize) {
            int segmentHeight = Math.min(sliceSize, height - sliceSize - j);
            pGuiGraphics.blit(CURIOS_PANEL_TEXTURE, x + width - sliceSize, y + j, 16, 8, sliceSize, segmentHeight, 24,
                    24);
        }

        // Fill the center area
        for (int i = sliceSize; i < width - sliceSize; i += sliceSize) {
            for (int j = sliceSize; j < height - sliceSize; j += sliceSize) {
                int segmentWidth = Math.min(sliceSize, width - sliceSize - i);
                int segmentHeight = Math.min(sliceSize, height - sliceSize - j);
                pGuiGraphics.blit(CURIOS_PANEL_TEXTURE, x + i, y + j, 8, 8, segmentWidth, segmentHeight, 24, 24);
            }
        }
    }

    private void renderEmptyArmorSlotIcons(GuiGraphics pGuiGraphics, int x, int y) {
        int armorY = y + 18 + yOffset;

        ResourceLocation[] armorSlotTextures = {
                EMPTY_ARMOR_SLOT_BOOTS,
                EMPTY_ARMOR_SLOT_LEGGINGS,
                EMPTY_ARMOR_SLOT_CHESTPLATE,
                EMPTY_ARMOR_SLOT_HELMET,
                EMPTY_ARMOR_SLOT_SHIELD
        };

        for (int i = 0; i < armorSlotTextures.length; i++) {
            int slotIndex = 36 + i;
            if (slotIndex < this.menu.slots.size()) {
                if (this.menu.slots.get(slotIndex).getItem().isEmpty()) {
                    int slotX = x + 8 + (i * 18);
                    int slotY = armorY;

                    try {
                        pGuiGraphics.blit(armorSlotTextures[i], slotX, slotY, 0, 0, 16, 16, 16, 16);
                    } catch (Exception e) {
                    }
                }
            }
        }

        if (hasCosmeticArmor) {
            int cosmeticArmorY = y + 18;
            for (int i = 0; i < 4; i++) {
                int slotIndex = 41 + i;
                if (slotIndex < this.menu.slots.size()) {
                    if (this.menu.slots.get(slotIndex).getItem().isEmpty()) {
                        int slotX = x + 8 + (i * 18);
                        int slotY = cosmeticArmorY;

                        try {
                            pGuiGraphics.blit(armorSlotTextures[i], slotX, slotY, 0, 0, 16, 16, 16, 16);
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        // Render the background dimming effect (semi-transparent dark overlay)
        this.renderBackground(pGuiGraphics);

        // Render the GUI and its contents
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        // Render tooltips on top
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        pGuiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        pGuiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                0x404040, false);
    }

    // Getter for yOffset to use in menu positioning
    public int getYOffset() {
        return yOffset;
    }
}