package es.boopurno.gravestones.client.config;

import es.boopurno.gravestones.config.GravestoneConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class ClothConfigScreen {

        public static Screen createConfigScreen(Screen parent) {
                try {
                        Class<?> configBuilderClass = Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
                        Object builder = configBuilderClass.getMethod("create").invoke(null);

                        configBuilderClass.getMethod("setParentScreen", Screen.class).invoke(builder, parent);
                        configBuilderClass.getMethod("setTitle", Component.class).invoke(builder,
                                        Component.translatable("config.gravestones.title"));

                        configBuilderClass.getMethod("setSavingRunnable", Runnable.class).invoke(builder,
                                        (Runnable) () -> {
                                                GravestoneConfig.SPEC.save();
                                        });

                        Object itemLoss = configBuilderClass.getMethod("getOrCreateCategory", Component.class)
                                        .invoke(builder, Component
                                                        .translatable("config.gravestones.category.item_loss"));
                        Object entryBuilder = configBuilderClass.getMethod("entryBuilder").invoke(builder);

                        addBooleanEntry(itemLoss, entryBuilder, "config.gravestones.enable_item_loss",
                                        GravestoneConfig.ENABLE_ITEM_LOSS.get(), false,
                                        newValue -> GravestoneConfig.ENABLE_ITEM_LOSS.set((Boolean) newValue));

                        addIntSliderEntry(itemLoss, entryBuilder, "config.gravestones.min_slots_lost_percent",
                                        GravestoneConfig.MIN_SLOTS_LOST_PERCENT.get(), 5, 1, 100,
                                        newValue -> GravestoneConfig.MIN_SLOTS_LOST_PERCENT.set((Integer) newValue));

                        addIntSliderEntry(itemLoss, entryBuilder, "config.gravestones.max_slots_lost_percent",
                                        GravestoneConfig.MAX_SLOTS_LOST_PERCENT.get(), 20, 1, 100,
                                        newValue -> GravestoneConfig.MAX_SLOTS_LOST_PERCENT.set((Integer) newValue));

                        addIntSliderEntry(itemLoss, entryBuilder, "config.gravestones.min_stack_loss_percent",
                                        GravestoneConfig.MIN_STACK_LOSS_PERCENT.get(), 10, 10, 90,
                                        newValue -> GravestoneConfig.MIN_STACK_LOSS_PERCENT.set((Integer) newValue));

                        addIntSliderEntry(itemLoss, entryBuilder, "config.gravestones.max_stack_loss_percent",
                                        GravestoneConfig.MAX_STACK_LOSS_PERCENT.get(), 50, 10, 90,
                                        newValue -> GravestoneConfig.MAX_STACK_LOSS_PERCENT.set((Integer) newValue));

                        addIntSliderEntry(itemLoss, entryBuilder, "config.gravestones.min_durability_loss_percent",
                                        GravestoneConfig.MIN_DURABILITY_LOSS_PERCENT.get(), 10, 10, 50,
                                        newValue -> GravestoneConfig.MIN_DURABILITY_LOSS_PERCENT
                                                        .set((Integer) newValue));

                        addIntSliderEntry(itemLoss, entryBuilder, "config.gravestones.max_durability_loss_percent",
                                        GravestoneConfig.MAX_DURABILITY_LOSS_PERCENT.get(), 30, 10, 50,
                                        newValue -> GravestoneConfig.MAX_DURABILITY_LOSS_PERCENT
                                                        .set((Integer) newValue));

                        addStringListEntry(itemLoss, entryBuilder, "config.gravestones.item_blacklist",
                                        (List<String>) GravestoneConfig.ITEM_BLACKLIST.get(),
                                        Arrays.asList("minecraft:totem_of_undying", "curios:ring"),
                                        newValue -> GravestoneConfig.ITEM_BLACKLIST.set((List<String>) newValue));

                        addBooleanEntry(itemLoss, entryBuilder, "config.gravestones.affect_curios_items",
                                        GravestoneConfig.AFFECT_CURIOS_ITEMS.get(), false,
                                        newValue -> GravestoneConfig.AFFECT_CURIOS_ITEMS.set((Boolean) newValue));

                        addBooleanEntry(itemLoss, entryBuilder, "config.gravestones.protect_enchanted_items",
                                        GravestoneConfig.PROTECT_ENCHANTED_ITEMS.get(), true,
                                        newValue -> GravestoneConfig.PROTECT_ENCHANTED_ITEMS.set((Boolean) newValue));

                        addIntSliderEntry(itemLoss, entryBuilder, "config.gravestones.enchanted_item_protection",
                                        GravestoneConfig.ENCHANTED_ITEM_PROTECTION.get(), 50, 0, 100,
                                        newValue -> GravestoneConfig.ENCHANTED_ITEM_PROTECTION.set((Integer) newValue));

                        Object curseMechanics = configBuilderClass.getMethod("getOrCreateCategory", Component.class)
                                        .invoke(builder, Component
                                                        .translatable("config.gravestones.category.curse_mechanics"));

                        addBooleanEntry(curseMechanics, entryBuilder, "config.gravestones.respect_curse_of_vanishing",
                                        GravestoneConfig.RESPECT_CURSE_OF_VANISHING.get(), true,
                                        newValue -> GravestoneConfig.RESPECT_CURSE_OF_VANISHING
                                                        .set((Boolean) newValue));

                        addBooleanEntry(curseMechanics, entryBuilder, "config.gravestones.enable_curse_application",
                                        GravestoneConfig.ENABLE_CURSE_APPLICATION.get(), false,
                                        newValue -> GravestoneConfig.ENABLE_CURSE_APPLICATION.set((Boolean) newValue));

                        addIntSliderEntry(curseMechanics, entryBuilder, "config.gravestones.curse_application_chance",
                                        GravestoneConfig.CURSE_APPLICATION_CHANCE.get(), 10, 0, 100,
                                        newValue -> GravestoneConfig.CURSE_APPLICATION_CHANCE.set((Integer) newValue));

                        addStringListEntry(curseMechanics, entryBuilder, "config.gravestones.available_curses",
                                        (List<String>) GravestoneConfig.AVAILABLE_CURSES.get(),
                                        Arrays.asList("minecraft:binding_curse", "minecraft:vanishing_curse"),
                                        newValue -> GravestoneConfig.AVAILABLE_CURSES.set((List<String>) newValue));

                        return (Screen) configBuilderClass.getMethod("build").invoke(builder);

                } catch (Exception e) {
                        e.printStackTrace();
                        return new SimpleConfigScreen(parent);
                }
        }

        private static void addBooleanEntry(Object category, Object entryBuilder, String translationKey,
                        boolean currentValue, boolean defaultValue,
                        java.util.function.Consumer<Object> saveConsumer) throws Exception {
                Class<?> entryBuilderClass = entryBuilder.getClass();
                Object entry = entryBuilderClass.getMethod("startBooleanToggle", Component.class, boolean.class)
                                .invoke(entryBuilder, Component.translatable(translationKey), currentValue);

                entry = entry.getClass().getMethod("setDefaultValue", boolean.class).invoke(entry, defaultValue);

                try {
                        Component[] tooltipArray = new Component[] {
                                        Component.translatable(translationKey + ".tooltip") };
                        entry = entry.getClass().getMethod("setTooltip", Optional.class)
                                        .invoke(entry, Optional.of(tooltipArray));
                } catch (NoSuchMethodException e1) {
                        try {
                                Component[] tooltipArray = new Component[] {
                                                Component.translatable(translationKey + ".tooltip") };
                                entry = entry.getClass().getMethod("setTooltip", Component[].class)
                                                .invoke(entry, (Object) tooltipArray);
                        } catch (NoSuchMethodException e2) {
                                System.out.println(
                                                "Could not set tooltip for " + translationKey + " - method not found");
                        }
                }

                entry = entry.getClass().getMethod("setSaveConsumer", java.util.function.Consumer.class)
                                .invoke(entry, saveConsumer);
                entry = entry.getClass().getMethod("build").invoke(entry);

                addEntryToCategory(category, entry);
        }

        private static void addIntSliderEntry(Object category, Object entryBuilder, String translationKey,
                        int currentValue, int defaultValue, int min, int max,
                        java.util.function.Consumer<Object> saveConsumer) throws Exception {
                Class<?> entryBuilderClass = entryBuilder.getClass();
                Object entry = entryBuilderClass
                                .getMethod("startIntSlider", Component.class, int.class, int.class, int.class)
                                .invoke(entryBuilder, Component.translatable(translationKey), currentValue, min, max);

                entry = entry.getClass().getMethod("setDefaultValue", int.class).invoke(entry, defaultValue);

                try {
                        Component[] tooltipArray = new Component[] {
                                        Component.translatable(translationKey + ".tooltip") };
                        entry = entry.getClass().getMethod("setTooltip", Optional.class)
                                        .invoke(entry, Optional.of(tooltipArray));
                } catch (NoSuchMethodException e1) {
                        try {
                                Component[] tooltipArray = new Component[] {
                                                Component.translatable(translationKey + ".tooltip") };
                                entry = entry.getClass().getMethod("setTooltip", Component[].class)
                                                .invoke(entry, (Object) tooltipArray);
                        } catch (NoSuchMethodException e2) {
                                System.out.println(
                                                "Could not set tooltip for " + translationKey + " - method not found");
                        }
                }

                entry = entry.getClass().getMethod("setSaveConsumer", java.util.function.Consumer.class)
                                .invoke(entry, saveConsumer);
                entry = entry.getClass().getMethod("build").invoke(entry);

                addEntryToCategory(category, entry);
        }

        private static void addStringListEntry(Object category, Object entryBuilder, String translationKey,
                        List<String> currentValue, List<String> defaultValue,
                        java.util.function.Consumer<Object> saveConsumer) throws Exception {
                Class<?> entryBuilderClass = entryBuilder.getClass();
                Object entry = entryBuilderClass.getMethod("startStrList", Component.class, List.class)
                                .invoke(entryBuilder, Component.translatable(translationKey), currentValue);

                entry = entry.getClass().getMethod("setDefaultValue", List.class).invoke(entry, defaultValue);

                try {
                        Component[] tooltipArray = new Component[] {
                                        Component.translatable(translationKey + ".tooltip") };
                        entry = entry.getClass().getMethod("setTooltip", Optional.class)
                                        .invoke(entry, Optional.of(tooltipArray));
                } catch (NoSuchMethodException e1) {
                        try {
                                Component[] tooltipArray = new Component[] {
                                                Component.translatable(translationKey + ".tooltip") };
                                entry = entry.getClass().getMethod("setTooltip", Component[].class)
                                                .invoke(entry, (Object) tooltipArray);
                        } catch (NoSuchMethodException e2) {
                                System.out.println(
                                                "Could not set tooltip for " + translationKey + " - method not found");
                        }
                }

                entry = entry.getClass().getMethod("setSaveConsumer", java.util.function.Consumer.class)
                                .invoke(entry, saveConsumer);
                entry = entry.getClass().getMethod("build").invoke(entry);

                addEntryToCategory(category, entry);
        }

        private static void addEntryToCategory(Object category, Object entry) throws Exception {
                Class<?> categoryClass = category.getClass();

                try {
                        Class<?> abstractConfigListEntryClass = Class
                                        .forName("me.shedaniel.clothconfig2.api.AbstractConfigListEntry");
                        categoryClass.getMethod("addEntry", abstractConfigListEntryClass).invoke(category, entry);
                } catch (Exception e1) {
                        try {
                                categoryClass.getMethod("addEntry", entry.getClass()).invoke(category, entry);
                        } catch (Exception e2) {
                                try {
                                        categoryClass.getMethod("addEntry", Object.class).invoke(category, entry);
                                } catch (Exception e3) {
                                        java.lang.reflect.Method[] methods = categoryClass.getMethods();
                                        for (java.lang.reflect.Method method : methods) {
                                                if (method.getName().equals("addEntry")
                                                                && method.getParameterCount() == 1) {
                                                        method.invoke(category, entry);
                                                        return;
                                                }
                                        }
                                        throw new RuntimeException("Could not find suitable addEntry method", e3);
                                }
                        }
                }
        }

        private static class SimpleConfigScreen extends Screen {
                private final Screen parent;

                protected SimpleConfigScreen(Screen parent) {
                        super(Component.translatable("config.gravestones.title"));
                        this.parent = parent;
                }

                @Override
                public void onClose() {
                        this.minecraft.setScreen(parent);
                }

                @Override
                public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY,
                                float partialTick) {
                        this.renderBackground(guiGraphics);
                        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 16777215);

                        int y = 50;
                        guiGraphics.drawString(this.font, "Cloth Config not available", 20, y, 16777215);
                        y += 20;
                        guiGraphics.drawString(this.font, "Edit config file: config/gravestones-common.toml", 20, y,
                                        11184810);

                        super.render(guiGraphics, mouseX, mouseY, partialTick);
                }
        }
}