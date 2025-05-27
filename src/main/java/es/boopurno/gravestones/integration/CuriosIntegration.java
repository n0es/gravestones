package es.boopurno.gravestones.integration;

import es.boopurno.gravestones.Gravestones;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CuriosIntegration {

    private static Class<?> curiosApiClass;
    private static Method getCuriosInventoryMethod;
    private static Method resolveMethod;
    private static boolean initialized = false;

    public static void init() {
        try {
            curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            getCuriosInventoryMethod = curiosApiClass.getMethod("getCuriosInventory",
                    net.minecraft.world.entity.LivingEntity.class);

            Class<?> lazyOptionalClass = Class.forName("net.minecraftforge.common.util.LazyOptional");
            resolveMethod = lazyOptionalClass.getMethod("resolve");

            initialized = true;
            Gravestones.LOGGER.info("Curios integration initialized successfully using reflection");
        } catch (Exception e) {
            initialized = false;
            Gravestones.LOGGER.warn("Failed to initialize Curios integration: {}", e.getMessage());
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static List<ItemStack> getCuriosItems(Player player) {
        List<ItemStack> curiosItems = new ArrayList<>();

        if (!initialized) {
            return curiosItems;
        }

        try {
            Object lazyOptional = getCuriosInventoryMethod.invoke(null, player);
            Optional<?> curiosHandler = (Optional<?>) resolveMethod.invoke(lazyOptional);

            if (curiosHandler.isEmpty()) {
                return curiosItems;
            }

            Object curios = curiosHandler.get();

            Method getCuriosMethod = curios.getClass().getMethod("getCurios");
            Map<String, Object> curiosMap = (Map<String, Object>) getCuriosMethod.invoke(curios);

            for (Map.Entry<String, Object> entry : curiosMap.entrySet()) {
                Object stacksHandler = entry.getValue();

                Method getStacksMethod = stacksHandler.getClass().getMethod("getStacks");
                Object stackHandler = getStacksMethod.invoke(stacksHandler);

                Method getSlotsMethod = stackHandler.getClass().getMethod("getSlots");
                int slots = (Integer) getSlotsMethod.invoke(stackHandler);

                Method getStackInSlotMethod = stackHandler.getClass().getMethod("getStackInSlot", int.class);
                for (int i = 0; i < slots; i++) {
                    ItemStack stack = (ItemStack) getStackInSlotMethod.invoke(stackHandler, i);
                    if (!stack.isEmpty()) {
                        curiosItems.add(stack.copy());
                    }
                }
            }

            Gravestones.LOGGER.debug("Found {} curios items for player", curiosItems.size());
        } catch (Exception e) {
            Gravestones.LOGGER.error("Error getting curios items: {}", e.getMessage());
        }

        return curiosItems;
    }

    public static int storeCurios(Player player, ItemStackHandler graveHandler, int startSlot) {
        if (!initialized) {
            Gravestones.LOGGER.debug("Curios not initialized, skipping storage");
            return startSlot;
        }

        try {
            Object lazyOptional = getCuriosInventoryMethod.invoke(null, player);
            Optional<?> curiosHandler = (Optional<?>) resolveMethod.invoke(lazyOptional);

            if (curiosHandler.isEmpty()) {
                Gravestones.LOGGER.debug("No curios handler found for player");
                return startSlot;
            }

            Object curios = curiosHandler.get();
            int slotIndex = startSlot;

            Method getCuriosMethod = curios.getClass().getMethod("getCurios");
            Map<String, Object> curiosMap = (Map<String, Object>) getCuriosMethod.invoke(curios);

            for (Map.Entry<String, Object> entry : curiosMap.entrySet()) {
                Object stacksHandler = entry.getValue();

                Method getStacksMethod = stacksHandler.getClass().getMethod("getStacks");
                Object stackHandler = getStacksMethod.invoke(stacksHandler);

                Method getSlotsMethod = stackHandler.getClass().getMethod("getSlots");
                Method getStackInSlotMethod = stackHandler.getClass().getMethod("getStackInSlot", int.class);
                Method setStackInSlotMethod = stackHandler.getClass().getMethod("setStackInSlot", int.class,
                        ItemStack.class);

                int slots = (Integer) getSlotsMethod.invoke(stackHandler);

                for (int i = 0; i < slots; i++) {
                    ItemStack stack = (ItemStack) getStackInSlotMethod.invoke(stackHandler, i);
                    if (!stack.isEmpty() && slotIndex < graveHandler.getSlots()) {
                        graveHandler.setStackInSlot(slotIndex, stack.copy());
                        setStackInSlotMethod.invoke(stackHandler, i, ItemStack.EMPTY);
                        slotIndex++;
                    }
                }
            }

            Gravestones.LOGGER.debug("Stored {} curios items starting at slot {}", slotIndex - startSlot, startSlot);
            return slotIndex;
        } catch (Exception e) {
            Gravestones.LOGGER.error("Error storing curios items: {}", e.getMessage());
            return startSlot;
        }
    }

    public static int restoreCurios(Player player, ItemStackHandler graveHandler, int startSlot,
            List<ItemStack> itemsToDropAtEnd) {
        if (!initialized) {
            Gravestones.LOGGER.debug("Curios not initialized, clearing slots and dropping items");
            return clearCuriosSlots(graveHandler, startSlot, itemsToDropAtEnd);
        }

        try {
            Object lazyOptional = getCuriosInventoryMethod.invoke(null, player);
            Optional<?> curiosHandler = (Optional<?>) resolveMethod.invoke(lazyOptional);

            if (curiosHandler.isEmpty()) {
                Gravestones.LOGGER.debug("No curios handler found for player during restoration");
                return clearCuriosSlots(graveHandler, startSlot, itemsToDropAtEnd);
            }

            Object curios = curiosHandler.get();
            int slotIndex = startSlot;

            Method getCuriosMethod = curios.getClass().getMethod("getCurios");
            Map<String, Object> curiosMap = (Map<String, Object>) getCuriosMethod.invoke(curios);

            while (slotIndex < graveHandler.getSlots()) {
                ItemStack stack = graveHandler.getStackInSlot(slotIndex);
                if (stack.isEmpty()) {
                    slotIndex++;
                    continue;
                }

                boolean restored = false;

                for (Map.Entry<String, Object> entry : curiosMap.entrySet()) {
                    if (restored)
                        break;

                    String slotType = entry.getKey();
                    Object stacksHandler = entry.getValue();

                    Method getStacksMethod = stacksHandler.getClass().getMethod("getStacks");
                    Object stackHandler = getStacksMethod.invoke(stacksHandler);

                    Method getSlotsMethod = stackHandler.getClass().getMethod("getSlots");
                    Method getStackInSlotMethod = stackHandler.getClass().getMethod("getStackInSlot", int.class);
                    Method setStackInSlotMethod = stackHandler.getClass().getMethod("setStackInSlot", int.class,
                            ItemStack.class);
                    Method isItemValidMethod = stackHandler.getClass().getMethod("isItemValid", int.class,
                            ItemStack.class);

                    int slots = (Integer) getSlotsMethod.invoke(stackHandler);

                    for (int i = 0; i < slots; i++) {
                        ItemStack existingStack = (ItemStack) getStackInSlotMethod.invoke(stackHandler, i);
                        if (existingStack.isEmpty()) {
                            boolean isValid = (Boolean) isItemValidMethod.invoke(stackHandler, i, stack);
                            if (isValid) {
                                setStackInSlotMethod.invoke(stackHandler, i, stack.copy());
                                graveHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);
                                restored = true;
                                Gravestones.LOGGER.debug("Restored curios item {} to slot type {} index {}",
                                        stack.getDisplayName().getString(), slotType, i);
                                break;
                            }
                        }
                    }
                }

                if (!restored) {
                    itemsToDropAtEnd.add(stack.copy());
                    graveHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);
                    Gravestones.LOGGER.debug("Could not restore curios item {}, will drop",
                            stack.getDisplayName().getString());
                }

                slotIndex++;
            }

            Gravestones.LOGGER.debug("Restored curios items up to slot {}", slotIndex - 1);
            return slotIndex;
        } catch (Exception e) {
            Gravestones.LOGGER.error("Error restoring curios items: {}", e.getMessage());
            return clearCuriosSlots(graveHandler, startSlot, itemsToDropAtEnd);
        }
    }

    private static int clearCuriosSlots(ItemStackHandler graveHandler, int startSlot,
            List<ItemStack> itemsToDropAtEnd) {
        int slotIndex = startSlot;

        while (slotIndex < graveHandler.getSlots()) {
            ItemStack stack = graveHandler.getStackInSlot(slotIndex);
            if (stack.isEmpty()) {
                slotIndex++;
                continue;
            }

            itemsToDropAtEnd.add(stack);
            graveHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);
            slotIndex++;
        }

        return slotIndex;
    }

    public static int getCuriosItemCount(Player player) {
        return getCuriosItems(player).size();
    }

    public static int getCuriosSlotCount(Player player) {
        if (!initialized) {
            return 0;
        }

        try {
            Object lazyOptional = getCuriosInventoryMethod.invoke(null, player);
            Optional<?> curiosHandler = (Optional<?>) resolveMethod.invoke(lazyOptional);

            if (curiosHandler.isEmpty()) {
                return 0;
            }

            Object curios = curiosHandler.get();
            int totalSlots = 0;

            Method getCuriosMethod = curios.getClass().getMethod("getCurios");
            Map<String, Object> curiosMap = (Map<String, Object>) getCuriosMethod.invoke(curios);

            for (Object stacksHandler : curiosMap.values()) {
                Method getStacksMethod = stacksHandler.getClass().getMethod("getStacks");
                Object stackHandler = getStacksMethod.invoke(stacksHandler);

                Method getSlotsMethod = stackHandler.getClass().getMethod("getSlots");
                totalSlots += (Integer) getSlotsMethod.invoke(stackHandler);
            }

            return totalSlots;
        } catch (Exception e) {
            Gravestones.LOGGER.error("Error getting curios slot count: {}", e.getMessage());
            return 0;
        }
    }
}