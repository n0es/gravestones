package es.boopurno.gravestones.block.entity;

import es.boopurno.gravestones.Gravestones;
import es.boopurno.gravestones.integration.ModIntegration;
import es.boopurno.gravestones.integration.CuriosIntegration;
import es.boopurno.gravestones.menu.GravestoneMenu;
import es.boopurno.gravestones.config.GravestoneConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GravestoneBlockEntity extends BlockEntity implements MenuProvider {
    private int maxInventorySize;
    private final boolean hasCosmeticArmor;
    private final boolean hasCurios;
    private int curiosItemCount = 0;

    @Nullable
    private String ownerName;

    private ItemStackHandler itemHandler;
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public GravestoneBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(Gravestones.GRAVESTONE_BLOCK_ENTITY_TYPE.get(), pPos, pBlockState);

        this.hasCosmeticArmor = ModIntegration.HAS_COSMETIC_ARMOR;
        this.hasCurios = ModIntegration.HAS_CURIOS;
        this.maxInventorySize = ModIntegration.calculateInventorySize();

        this.itemHandler = new ItemStackHandler(this.maxInventorySize) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };

        Gravestones.LOGGER.debug("GravestoneBlockEntity initialized with {} slots (Cosmetic: {}, Curios: {})",
                this.maxInventorySize, hasCosmeticArmor, hasCurios);
    }

    public void setOwnerName(@Nullable String name) {
        this.ownerName = name;
        this.setChanged();
    }

    @Nullable
    public String getOwnerName() {
        return this.ownerName;
    }

    public int getMaxInventorySize() {
        return this.maxInventorySize;
    }

    public boolean hasCosmeticArmor() {
        return ModIntegration.HAS_COSMETIC_ARMOR;
    }

    public boolean hasCurios() {
        return this.hasCurios;
    }

    public int getCuriosItemCount() {
        return this.curiosItemCount;
    }

    public void setCuriosItemCount(int count) {
        this.curiosItemCount = count;
        this.setChanged();

        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new GravestoneMenu(pContainerId, pPlayerInventory, this, this.itemHandler);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    public void setItemsOnDeath(Inventory playerInventory) {
        Player player = playerInventory.player;
        applyItemLoss(player);

        int requiredSize = ModIntegration.calculateInventorySize(player);
        if (requiredSize != this.maxInventorySize) {
            Gravestones.LOGGER.debug("Resizing gravestone inventory from {} to {} slots for player {}",
                    this.maxInventorySize, requiredSize, player.getName().getString());

            this.maxInventorySize = requiredSize;

            ItemStackHandler newHandler = new ItemStackHandler(this.maxInventorySize) {
                @Override
                protected void onContentsChanged(int slot) {
                    setChanged();
                }
            };

            for (int i = 0; i < Math.min(this.itemHandler.getSlots(), newHandler.getSlots()); i++) {
                newHandler.setStackInSlot(i, this.itemHandler.getStackInSlot(i));
            }

            this.itemHandler = newHandler;
            this.lazyItemHandler.invalidate();
            this.lazyItemHandler = LazyOptional.of(() -> this.itemHandler);
        }

        if (ModIntegration.HAS_CURIOS) {
            this.setCuriosItemCount(CuriosIntegration.getCuriosItemCount(player));
        }

        ModIntegration.storePlayerInventory(player, this.itemHandler);
        setChanged();
    }

    private void applyItemLoss(Player player) {
        if (!GravestoneConfig.ENABLE_ITEM_LOSS.get()) {
            return;
        }

        Random random = new Random();
        Inventory inventory = player.getInventory();

        List<ItemStack> allItems = new ArrayList<>();
        List<Object> itemSources = new ArrayList<>();

        // Collect all items from player inventory
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inventory.items.get(i);
            if (!stack.isEmpty()) {
                allItems.add(stack);
                itemSources.add(new InventorySource("main", i));
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.items.get(i);
            if (!stack.isEmpty()) {
                allItems.add(stack);
                itemSources.add(new InventorySource("hotbar", i));
            }
        }

        for (int i = 0; i < inventory.armor.size(); i++) {
            ItemStack stack = inventory.armor.get(i);
            if (!stack.isEmpty()) {
                allItems.add(stack);
                itemSources.add(new InventorySource("armor", i));
            }
        }

        if (!inventory.offhand.get(0).isEmpty()) {
            allItems.add(inventory.offhand.get(0));
            itemSources.add(new InventorySource("offhand", 0));
        }

        if (ModIntegration.HAS_COSMETIC_ARMOR) {
            addCosmeticArmorItems(player, allItems, itemSources);
        }

        // First, handle Curse of Vanishing items
        if (GravestoneConfig.RESPECT_CURSE_OF_VANISHING.get()) {
            handleCurseOfVanishing(allItems, itemSources, player);
        }

        // Apply random curses to enchanted items
        if (GravestoneConfig.ENABLE_CURSE_APPLICATION.get()) {
            applyCursesToItems(allItems, random);
        }

        // Calculate number of slots to affect based on percentage of occupied slots
        int minSlotsPercent = GravestoneConfig.MIN_SLOTS_LOST_PERCENT.get();
        int maxSlotsPercent = GravestoneConfig.MAX_SLOTS_LOST_PERCENT.get();
        int slotPercentage = minSlotsPercent + random.nextInt(Math.max(1, maxSlotsPercent - minSlotsPercent + 1));
        int slotsToAffect = Math.max(1, (allItems.size() * slotPercentage) / 100);

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < allItems.size(); i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, random);

        int itemsLost = 0;
        int stacksAffected = 0;

        for (int i = 0; i < Math.min(slotsToAffect, indices.size()); i++) {
            int index = indices.get(i);
            ItemStack itemStack = allItems.get(index);
            Object source = itemSources.get(index);

            if (isItemBlacklisted(itemStack)) {
                continue;
            }

            if (GravestoneConfig.PROTECT_ENCHANTED_ITEMS.get() && !itemStack.getEnchantmentTags().isEmpty()) {
                int protection = GravestoneConfig.ENCHANTED_ITEM_PROTECTION.get();

                if (random.nextInt(100) < protection) {
                    continue;
                }
            }

            int lost = 0;
            if (itemStack.isDamageableItem()) {
                lost = applyDurabilityLoss(itemStack, random);
            } else {
                lost = applyQuantityLoss(itemStack, random);
            }

            if (lost > 0) {
                itemsLost += lost;
                stacksAffected++;
            }

            updateItemSource(source, itemStack, player);
        }

        if (GravestoneConfig.AFFECT_CURIOS_ITEMS.get() && ModIntegration.HAS_CURIOS) {
            int[] curiosResults = applyCuriosItemLoss(player, random);
            itemsLost += curiosResults[0];
            stacksAffected += curiosResults[1];
        }

        if (itemsLost > 0 || stacksAffected > 0) {
            Gravestones.LOGGER.info(
                    "Player {} lost {} items from {} stacks on death ({}-{}% of occupied slots affected)",
                    player.getName().getString(), itemsLost, stacksAffected, minSlotsPercent, maxSlotsPercent);
        }
    }

    private void addCosmeticArmorItems(Player player, List<ItemStack> allItems, List<Object> itemSources) {
        try {
            Class<?> cosArmorApiClass = Class.forName("lain.mods.cos.api.CosArmorAPI");
            Object cosmeticInventory = cosArmorApiClass.getMethod("getCAStacks", java.util.UUID.class)
                    .invoke(null, player.getUUID());

            if (cosmeticInventory != null) {
                for (int i = 0; i < 4; i++) {
                    java.lang.reflect.Method getStackMethod = cosmeticInventory.getClass().getMethod("getStackInSlot",
                            int.class);
                    ItemStack stack = (ItemStack) getStackMethod.invoke(cosmeticInventory, i);

                    if (!stack.isEmpty()) {
                        allItems.add(stack);
                        itemSources.add(new CosmeticArmorSource(cosmeticInventory, i));
                    }
                }
            }
        } catch (Exception e) {
            Gravestones.LOGGER.warn("Failed to access cosmetic armor items for item loss: {}", e.getMessage());
        }
    }

    private void updateItemSource(Object source, ItemStack itemStack, Player player) {
        try {
            if (source instanceof InventorySource) {
                InventorySource invSource = (InventorySource) source;
                Inventory inventory = player.getInventory();

                switch (invSource.type) {
                    case "main":
                        inventory.items.set(invSource.slot, itemStack);
                        break;
                    case "hotbar":
                        inventory.items.set(invSource.slot, itemStack);
                        break;
                    case "armor":
                        inventory.armor.set(invSource.slot, itemStack);
                        break;
                    case "offhand":
                        inventory.offhand.set(invSource.slot, itemStack);
                        break;
                }
            } else if (source instanceof CosmeticArmorSource) {
                CosmeticArmorSource cosSource = (CosmeticArmorSource) source;
                java.lang.reflect.Method setStackMethod = cosSource.inventory.getClass().getMethod("setStackInSlot",
                        int.class, ItemStack.class);
                setStackMethod.invoke(cosSource.inventory, cosSource.slot, itemStack);
            }
        } catch (Exception e) {
            Gravestones.LOGGER.warn("Failed to update item source after loss: {}", e.getMessage());
        }
    }

    private static class InventorySource {
        String type;
        int slot;

        InventorySource(String type, int slot) {
            this.type = type;
            this.slot = slot;
        }
    }

    private static class CosmeticArmorSource {
        Object inventory;
        int slot;

        CosmeticArmorSource(Object inventory, int slot) {
            this.inventory = inventory;
            this.slot = slot;
        }
    }

    private int applyDurabilityLoss(ItemStack itemStack, Random random) {
        int maxDamage = itemStack.getMaxDamage();
        int currentDamage = itemStack.getDamageValue();
        int remainingDurability = maxDamage - currentDamage;

        if (remainingDurability <= 0) {
            return 0;
        }

        // Use configured min/max durability loss percentages
        int minPercent = GravestoneConfig.MIN_DURABILITY_LOSS_PERCENT.get();
        int maxPercent = GravestoneConfig.MAX_DURABILITY_LOSS_PERCENT.get();
        double damageRatio = (minPercent + random.nextInt(Math.max(1, maxPercent - minPercent + 1))) / 100.0;

        int damageToApply = Math.max(1, (int) (remainingDurability * damageRatio));

        int newDamage = Math.min(maxDamage, currentDamage + damageToApply);
        itemStack.setDamageValue(newDamage);

        Gravestones.LOGGER.debug("Applied {} durability damage to {} (was {}/{}, now {}/{}, {}% loss)",
                damageToApply, itemStack.getDisplayName().getString(),
                currentDamage, maxDamage, newDamage, maxDamage, (int) (damageRatio * 100));

        if (newDamage >= maxDamage) {
            int originalCount = itemStack.getCount();
            itemStack.setCount(0);
            Gravestones.LOGGER.debug("Item {} broke completely and was removed",
                    itemStack.getDisplayName().getString());
            return originalCount;
        }

        return 0;
    }

    private int applyQuantityLoss(ItemStack itemStack, Random random) {
        int stackSize = itemStack.getCount();

        // Use configured min/max stack loss percentages
        int minPercent = GravestoneConfig.MIN_STACK_LOSS_PERCENT.get();
        int maxPercent = GravestoneConfig.MAX_STACK_LOSS_PERCENT.get();
        double lossRatio = (minPercent + random.nextInt(Math.max(1, maxPercent - minPercent + 1))) / 100.0;

        int itemsToLose = Math.max(1, (int) (stackSize * lossRatio));
        itemsToLose = Math.min(itemsToLose, stackSize);

        itemStack.setCount(stackSize - itemsToLose);

        Gravestones.LOGGER.debug("Lost {} items from stack of {} {} ({}% of stack)",
                itemsToLose, stackSize, itemStack.getDisplayName().getString(),
                (int) (lossRatio * 100));

        return itemsToLose;
    }

    private boolean isItemBlacklisted(ItemStack itemStack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        String itemIdString = itemId.toString();

        List<? extends String> blacklist = GravestoneConfig.ITEM_BLACKLIST.get();
        for (String blacklistedItem : blacklist) {
            if (itemIdString.equals(blacklistedItem)) {
                return true;
            }
        }

        return false;
    }

    private int[] applyCuriosItemLoss(Player player, Random random) {
        try {
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object curiosInventory = curiosApiClass.getMethod("getCuriosInventory", Player.class)
                    .invoke(null, player);

            if (curiosInventory == null) {
                return new int[] { 0, 0 };
            }

            Object handler = curiosInventory.getClass().getMethod("orElse", Object.class)
                    .invoke(curiosInventory, null);

            if (handler == null) {
                return new int[] { 0, 0 };
            }

            Object curios = handler.getClass().getMethod("getCurios").invoke(handler);
            java.util.Map<?, ?> curiosMap = (java.util.Map<?, ?>) curios;

            List<ItemStack> curiosItems = new ArrayList<>();
            List<Object[]> curiosItemHandlers = new ArrayList<>();

            for (Object curiosSlots : curiosMap.values()) {
                Object itemHandler = curiosSlots.getClass().getMethod("getStacks").invoke(curiosSlots);
                int slots = (Integer) itemHandler.getClass().getMethod("getSlots").invoke(itemHandler);

                for (int i = 0; i < slots; i++) {
                    java.lang.reflect.Method stackMethod = itemHandler.getClass().getMethod("getStackInSlot",
                            int.class);
                    ItemStack itemStack = (ItemStack) stackMethod.invoke(itemHandler, i);

                    if (!itemStack.isEmpty()) {
                        curiosItems.add(itemStack);
                        curiosItemHandlers.add(new Object[] { itemHandler, i }); // Store handler and slot
                    }
                }
            }

            // Use the same min/max slot approach as main inventory
            int minSlotsPercent = GravestoneConfig.MIN_SLOTS_LOST_PERCENT.get();
            int maxSlotsPercent = GravestoneConfig.MAX_SLOTS_LOST_PERCENT.get();
            int slotPercentage = minSlotsPercent + random.nextInt(Math.max(1, maxSlotsPercent - minSlotsPercent + 1));
            int itemsToAffect = Math.min(curiosItems.size(),
                    Math.max(1, (curiosItems.size() * slotPercentage) / 100));

            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < curiosItems.size(); i++) {
                indices.add(i);
            }
            Collections.shuffle(indices, random);

            int itemsLost = 0;
            int stacksAffected = 0;

            for (int i = 0; i < Math.min(itemsToAffect, indices.size()); i++) {
                int index = indices.get(i);
                ItemStack itemStack = curiosItems.get(index);
                Object[] handlerInfo = curiosItemHandlers.get(index);
                Object itemHandler = handlerInfo[0];
                int slot = (Integer) handlerInfo[1];

                if (isItemBlacklisted(itemStack)) {
                    continue;
                }

                if (GravestoneConfig.PROTECT_ENCHANTED_ITEMS.get() && !itemStack.getEnchantmentTags().isEmpty()) {
                    int protection = GravestoneConfig.ENCHANTED_ITEM_PROTECTION.get();

                    if (random.nextInt(100) < protection) {
                        continue;
                    }
                }

                int lost = 0;
                if (itemStack.isDamageableItem()) {
                    lost = applyDurabilityLoss(itemStack, random);
                } else {
                    lost = applyQuantityLoss(itemStack, random);
                }

                if (lost > 0) {
                    itemsLost += lost;
                    stacksAffected++;
                }

                java.lang.reflect.Method setStackMethod = itemHandler.getClass().getMethod("setStackInSlot", int.class,
                        ItemStack.class);
                setStackMethod.invoke(itemHandler, slot, itemStack);
            }

            return new int[] { itemsLost, stacksAffected };
        } catch (Exception e) {
            Gravestones.LOGGER.warn("Failed to apply item loss to curios items: {}", e.getMessage());
            return new int[] { 0, 0 };
        }
    }

    public ItemStackHandler getInternalItemHandler() {
        return this.itemHandler;
    }

    public void transferItemsToPlayer(Player player) {
        if (this.level == null || this.level.isClientSide)
            return;

        ModIntegration.restorePlayerInventory(player, this.itemHandler);

        this.setCuriosItemCount(0);
        setChanged();
    }

    public void dropAllItems(@NotNull Level level, BlockPos pos) {
        SimpleContainer inventoryForDropping = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventoryForDropping.setItem(i, itemHandler.getStackInSlot(i));
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        Containers.dropContents(level, pos, inventoryForDropping);

        this.setCuriosItemCount(0);
        setChanged();
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public Component getDisplayName() {
        String nameForDisplay = (this.ownerName != null && !this.ownerName.isEmpty()) ? this.ownerName : "Unknown";
        return Component.translatable("menu." + Gravestones.MODID + ".gravestone", nameForDisplay);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.putInt("maxInventorySize", this.maxInventorySize);
        if (this.ownerName != null) {
            pTag.putString("OwnerName", this.ownerName);
        }
        pTag.putInt("CuriosItemCount", this.curiosItemCount);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);

        int savedInventorySize = pTag.contains("maxInventorySize") ? pTag.getInt("maxInventorySize")
                : ModIntegration.calculateInventorySize();

        if (savedInventorySize != this.maxInventorySize) {
            this.maxInventorySize = savedInventorySize;

            ItemStackHandler newHandler = new ItemStackHandler(this.maxInventorySize) {
                @Override
                protected void onContentsChanged(int slot) {
                    setChanged();
                }
            };

            this.itemHandler = newHandler;
            this.lazyItemHandler.invalidate();
            this.lazyItemHandler = LazyOptional.of(() -> this.itemHandler);
        }

        itemHandler.deserializeNBT(pTag.getCompound("inventory"));

        if (pTag.contains("OwnerName", CompoundTag.TAG_STRING)) {
            this.ownerName = pTag.getString("OwnerName");
        } else {
            this.ownerName = null;
        }
        this.curiosItemCount = pTag.getInt("CuriosItemCount");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        this.load(packet.getTag());
    }

    private void handleCurseOfVanishing(List<ItemStack> allItems, List<Object> itemSources, Player player) {
        for (int i = allItems.size() - 1; i >= 0; i--) {
            ItemStack stack = allItems.get(i);

            if (EnchantmentHelper.hasVanishingCurse(stack)) {
                Object source = itemSources.get(i);
                ItemStack emptyStack = ItemStack.EMPTY;
                updateItemSource(source, emptyStack, player);

                allItems.remove(i);
                itemSources.remove(i);

                Gravestones.LOGGER.debug("Item {} disappeared due to Curse of Vanishing for player {}",
                        stack.getDisplayName().getString(), player.getName().getString());
            }
        }
    }

    private void applyCursesToItems(List<ItemStack> allItems, Random random) {
        int curseChance = GravestoneConfig.CURSE_APPLICATION_CHANCE.get();
        if (curseChance <= 0) {
            return;
        }

        List<String> availableCurses = (List<String>) GravestoneConfig.AVAILABLE_CURSES.get();

        if (availableCurses.isEmpty()) {
            return;
        }

        for (ItemStack stack : allItems) {
            // Only apply curses to enchanted items
            if (stack.getEnchantmentTags().isEmpty()) {
                continue;
            }

            // Roll for curse application
            if (random.nextInt(100) >= curseChance) {
                continue;
            }

            // Select a random curse from available list
            String selectedCurse = availableCurses.get(random.nextInt(availableCurses.size()));
            ResourceLocation curseLocation = ResourceLocation.tryParse(selectedCurse);

            if (curseLocation == null) {
                Gravestones.LOGGER.warn("Invalid curse enchantment ID: {}", selectedCurse);
                continue;
            }

            // Check if enchantment exists in registry
            Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(curseLocation);
            if (enchantment == null) {
                Gravestones.LOGGER.warn("Enchantment not found in registry: {}", selectedCurse);
                continue;
            }

            // Check if the item already has this curse
            if (EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack) > 0) {
                continue;
            }

            // Apply the curse at level 1
            stack.enchant(enchantment, 1);

            Gravestones.LOGGER.debug("Applied curse {} to item {} for player on death",
                    selectedCurse, stack.getDisplayName().getString());
        }
    }
}