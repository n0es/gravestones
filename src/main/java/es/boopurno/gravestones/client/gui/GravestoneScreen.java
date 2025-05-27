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
    private static final ResourceLocation GRAVESTONE_VANILLA_TEXTURE = new ResourceLocation(Gravestones.MODID,
            "textures/gui/gravestone_vanilla.png");
    private static final ResourceLocation GRAVESTONE_EXTRA_TEXTURE = new ResourceLocation(Gravestones.MODID,
            "textures/gui/gravestone_extra.png");
    private static final ResourceLocation CURIOS_PANEL_TEXTURE = new ResourceLocation(Gravestones.MODID,
            "textures/gui/panel.png");
    private static final ResourceLocation SLOT_TEXTURE = new ResourceLocation(Gravestones.MODID,
            "textures/gui/slot.png");

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
    private final int yOffset;

    public GravestoneScreen(GravestoneMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);

        GravestoneBlockEntity blockEntity = pMenu.getBlockEntity();
        this.hasCosmeticArmor = blockEntity != null && blockEntity.hasCosmeticArmor();
        this.hasCurios = blockEntity != null && blockEntity.hasCurios();

        this.yOffset = hasCosmeticArmor ? 18 : 0;

        this.imageWidth = 176;
        this.imageHeight = hasCosmeticArmor ? 256 : 238;

        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 140 + yOffset;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int buttonY = y + 118 + yOffset;
        int buttonX = x + (176 / 2) - 50;

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

        int mainGuiX = x;
        int textureWidth = 176;

        ResourceLocation texture = hasCosmeticArmor ? GRAVESTONE_EXTRA_TEXTURE : GRAVESTONE_VANILLA_TEXTURE;
        pGuiGraphics.blit(texture, mainGuiX, y, 0, 0, textureWidth, imageHeight);

        if (hasCurios) {
            renderCuriosSlots(pGuiGraphics, x, y);
        }

        if (hasCosmeticArmor) {
            renderCosmeticArmorSlots(pGuiGraphics, mainGuiX, y);
        }

        renderEmptyArmorSlotIcons(pGuiGraphics, mainGuiX, y);
    }

    private void renderCosmeticArmorSlots(GuiGraphics pGuiGraphics, int x, int y) {
        int startX = x + 7;
        int startY = y + 17;

        for (int i = 0; i < 4; i++) {
            int slotX = startX + (i * 18);
            pGuiGraphics.blit(SLOT_TEXTURE, slotX, startY, 0, 0, 18, 18, 18, 18);
        }
    }

    private void renderCuriosSlots(GuiGraphics pGuiGraphics, int x, int y) {
        GravestoneBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null)
            return;

        int curiosCount = blockEntity.getCuriosItemCount();
        if (curiosCount == 0)
            return;

        int slotsPerColumn = 6;
        int columns = (curiosCount + slotsPerColumn - 1) / slotsPerColumn;
        int rows = Math.min(curiosCount, slotsPerColumn);

        int panelWidth = columns * 18 + 14;
        int panelHeight = rows * 18 + 14;

        int panelX = x - panelWidth - 1;
        int panelY = y + yOffset - 8;

        renderCuriosPanel(pGuiGraphics, panelX, panelY, panelWidth, panelHeight);

        for (int i = 0; i < curiosCount; i++) {
            int column = i / slotsPerColumn;
            int row = i % slotsPerColumn;

            int reversedColumn = columns - 1 - column;

            int slotX = panelX + 7 + reversedColumn * 18;
            int slotY = panelY + 7 + row * 18;

            pGuiGraphics.blit(SLOT_TEXTURE, slotX, slotY, 0, 0, 18, 18, 18, 18);
        }
    }

    private void renderCuriosPanel(GuiGraphics pGuiGraphics, int x, int y, int width, int height) {
        int sliceSize = 8;

        // Top-left corner
        pGuiGraphics.blit(CURIOS_PANEL_TEXTURE, x, y, 0, 0, sliceSize, sliceSize, 24, 24);

        // Top-right corner
        pGuiGraphics.blit(CURIOS_PANEL_TEXTURE, x + width - sliceSize, y, 16, 0, sliceSize, sliceSize, 24, 24);

        // Bottom-left corner
        pGuiGraphics.blit(CURIOS_PANEL_TEXTURE, x, y + height - sliceSize, 0, 16, sliceSize, sliceSize, 24, 24);

        // Bottom-right corner
        pGuiGraphics.blit(CURIOS_PANEL_TEXTURE, x + width - sliceSize, y + height - sliceSize, 16, 16, sliceSize,
                sliceSize, 24, 24);

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

        // Center
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
        this.renderBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        pGuiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        pGuiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                0x404040, false);
    }

    public int getYOffset() {
        return yOffset;
    }
}