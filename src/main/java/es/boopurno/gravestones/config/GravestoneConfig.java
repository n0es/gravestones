package es.boopurno.gravestones.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.Arrays;
import java.util.List;

public class GravestoneConfig {

        public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
        public static final ForgeConfigSpec SPEC;

        public static final ForgeConfigSpec.BooleanValue ENABLE_ITEM_LOSS;
        public static final ForgeConfigSpec.IntValue MIN_SLOTS_LOST_PERCENT;
        public static final ForgeConfigSpec.IntValue MAX_SLOTS_LOST_PERCENT;
        public static final ForgeConfigSpec.IntValue MIN_STACK_LOSS_PERCENT;
        public static final ForgeConfigSpec.IntValue MAX_STACK_LOSS_PERCENT;
        public static final ForgeConfigSpec.IntValue MIN_DURABILITY_LOSS_PERCENT;
        public static final ForgeConfigSpec.IntValue MAX_DURABILITY_LOSS_PERCENT;
        public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_BLACKLIST;
        public static final ForgeConfigSpec.BooleanValue AFFECT_CURIOS_ITEMS;
        public static final ForgeConfigSpec.BooleanValue PROTECT_ENCHANTED_ITEMS;
        public static final ForgeConfigSpec.IntValue ENCHANTED_ITEM_PROTECTION;
        public static final ForgeConfigSpec.BooleanValue RESPECT_CURSE_OF_VANISHING;
        public static final ForgeConfigSpec.BooleanValue ENABLE_CURSE_APPLICATION;
        public static final ForgeConfigSpec.IntValue CURSE_APPLICATION_CHANCE;
        public static final ForgeConfigSpec.ConfigValue<List<? extends String>> AVAILABLE_CURSES;

        static {
                BUILDER.push("Item Loss Settings");

                ENABLE_ITEM_LOSS = BUILDER
                                .comment("Enable the item loss mechanic on death")
                                .define("enableItemLoss", false);

                MIN_SLOTS_LOST_PERCENT = BUILDER
                                .comment("Minimum percentage of occupied inventory slots to be affected by item loss (0-100)")
                                .defineInRange("minSlotsLostPercent", 5, 0, 100);

                MAX_SLOTS_LOST_PERCENT = BUILDER
                                .comment("Maximum percentage of occupied inventory slots to be affected by item loss (0-100)")
                                .defineInRange("maxSlotsLostPercent", 20, 0, 100);

                MIN_STACK_LOSS_PERCENT = BUILDER
                                .comment("Minimum percentage of items to lose from each affected stack (0-100)")
                                .defineInRange("minStackLossPercent", 10, 0, 100);

                MAX_STACK_LOSS_PERCENT = BUILDER
                                .comment("Maximum percentage of items to lose from each affected stack (0-100)")
                                .defineInRange("maxStackLossPercent", 50, 0, 100);

                MIN_DURABILITY_LOSS_PERCENT = BUILDER
                                .comment("Minimum percentage of durability damage to apply to tools/armor (0-100)")
                                .defineInRange("minDurabilityLossPercent", 10, 0, 100);

                MAX_DURABILITY_LOSS_PERCENT = BUILDER
                                .comment("Maximum percentage of durability damage to apply to tools/armor (0-100)")
                                .defineInRange("maxDurabilityLossPercent", 30, 0, 100);

                ITEM_BLACKLIST = BUILDER
                                .comment("Items that should never be lost on death (use registry names like 'minecraft:diamond')",
                                                "Supports wildcards:",
                                                "  sophisticatedbackpacks:* - All items from sophisticatedbackpacks mod",
                                                "  *:totem_of_undying - Totems from any mod",
                                                "  minecraft:*_sword - All sword items from minecraft",
                                                "  *:*_ring - All ring items from any mod")
                                .defineList("itemBlacklist",
                                                Arrays.asList("minecraft:totem_of_undying", "curios:ring",
                                                                "sophisticatedbackpacks:*"),
                                                obj -> obj instanceof String);

                AFFECT_CURIOS_ITEMS = BUILDER
                                .comment("Should items in curios slots be affected by item loss?")
                                .define("affectCuriosItems", false);

                PROTECT_ENCHANTED_ITEMS = BUILDER
                                .comment("Should enchanted items have a lower chance of being lost?")
                                .define("protectEnchantedItems", true);

                ENCHANTED_ITEM_PROTECTION = BUILDER
                                .comment("Reduction in loss chance for enchanted items (0-100)")
                                .defineInRange("enchantedItemProtection", 50, 0, 100);

                BUILDER.pop();

                BUILDER.comment("Curse Mechanics").push("curse_mechanics");

                RESPECT_CURSE_OF_VANISHING = BUILDER
                                .comment("Should items with Curse of Vanishing disappear completely on death?")
                                .define("respectCurseOfVanishing", true);

                ENABLE_CURSE_APPLICATION = BUILDER
                                .comment("Enable randomly applying curses to enchanted items on death")
                                .define("enableCurseApplication", false);

                CURSE_APPLICATION_CHANCE = BUILDER
                                .comment("Chance to apply a curse to an enchanted item on death (0-100)")
                                .defineInRange("curseApplicationChance", 10, 0, 100);

                AVAILABLE_CURSES = BUILDER
                                .comment("List of curses that can be applied to items on death (use enchantment registry names)",
                                                "Available curses in vanilla Minecraft:",
                                                "  minecraft:binding_curse - Prevents removal from armor slots",
                                                "  minecraft:vanishing_curse - Item disappears on death")
                                .defineList("availableCurses",
                                                Arrays.asList("minecraft:binding_curse", "minecraft:vanishing_curse"),
                                                obj -> obj instanceof String);

                BUILDER.pop();
                SPEC = BUILDER.build();
        }

        public static void register() {
                ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
        }

        public static class ItemLossSettings {
                public boolean enableItemLoss = false;
                public int minSlotsLostPercent = 5;
                public int maxSlotsLostPercent = 20;
                public int minStackLossPercent = 10;
                public int maxStackLossPercent = 50;
                public int minDurabilityLossPercent = 10;
                public int maxDurabilityLossPercent = 30;
                public List<String> itemBlacklist = Arrays.asList("minecraft:totem_of_undying",
                                "sophisticatedbackpacks:*");
                public boolean affectCuriosItems = false;
                public boolean protectEnchantedItems = true;
                public int enchantedItemProtection = 50;

                public void updateFromConfig() {
                        this.enableItemLoss = ENABLE_ITEM_LOSS.get();
                        this.minSlotsLostPercent = MIN_SLOTS_LOST_PERCENT.get();
                        this.maxSlotsLostPercent = MAX_SLOTS_LOST_PERCENT.get();
                        this.minStackLossPercent = MIN_STACK_LOSS_PERCENT.get();
                        this.maxStackLossPercent = MAX_STACK_LOSS_PERCENT.get();
                        this.minDurabilityLossPercent = MIN_DURABILITY_LOSS_PERCENT.get();
                        this.maxDurabilityLossPercent = MAX_DURABILITY_LOSS_PERCENT.get();
                        this.itemBlacklist = (List<String>) ITEM_BLACKLIST.get();
                        this.affectCuriosItems = AFFECT_CURIOS_ITEMS.get();
                        this.protectEnchantedItems = PROTECT_ENCHANTED_ITEMS.get();
                        this.enchantedItemProtection = ENCHANTED_ITEM_PROTECTION.get();
                }
        }

        public static ItemLossSettings itemLoss = new ItemLossSettings();
}