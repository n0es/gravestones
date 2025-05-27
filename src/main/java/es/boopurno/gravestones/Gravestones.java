package es.boopurno.gravestones;

import com.mojang.logging.LogUtils;
import es.boopurno.gravestones.block.GravestoneBlock;
import es.boopurno.gravestones.block.entity.GravestoneBlockEntity;
import es.boopurno.gravestones.init.ModItems;
import es.boopurno.gravestones.integration.ModIntegration;
import es.boopurno.gravestones.menu.GravestoneMenu;
import es.boopurno.gravestones.network.PacketHandler;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(Gravestones.MODID)
public class Gravestones {
    public static final String MODID = "gravestones";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES,
            MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Block> GRAVESTONE_BLOCK = BLOCKS.register("gravestone",
            () -> new GravestoneBlock(BlockBehaviour.Properties.of().destroyTime(1).explosionResistance(100)
                    .sound(SoundType.ROOTED_DIRT)));
    public static final RegistryObject<Item> GRAVESTONE_BLOCK_ITEM = ITEMS.register("gravestone",
            () -> new BlockItem(GRAVESTONE_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<GravestoneBlockEntity>> GRAVESTONE_BLOCK_ENTITY_TYPE = BLOCK_ENTITIES
            .register("gravestone_be",
                    () -> BlockEntityType.Builder.of(GravestoneBlockEntity::new, GRAVESTONE_BLOCK.get()).build(null));

    public static final RegistryObject<MenuType<GravestoneMenu>> GRAVESTONE_MENU_TYPE = MENUS
            .register("gravestone_menu", () -> IForgeMenuType.create(GravestoneMenu::new));

    public static final RegistryObject<CreativeModeTab> GRAVESTONES_TAB = CREATIVE_MODE_TABS.register(
            "gravestones_tab",
            () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> GRAVESTONE_BLOCK_ITEM.get().getDefaultInstance())
                    .title(Component.translatable("itemGroup.gravestones.gravestones_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(GRAVESTONE_BLOCK_ITEM.get());
                    }).build());

    public Gravestones(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ModItems.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);

        es.boopurno.gravestones.config.GravestoneConfig.register();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            PacketHandler.register();
            ModIntegration.init();
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(GRAVESTONE_BLOCK_ITEM);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            event.enqueueWork(() -> {
                ItemBlockRenderTypes.setRenderLayer(GRAVESTONE_BLOCK.get(), RenderType.cutout());
                MenuScreens.register(GRAVESTONE_MENU_TYPE.get(),
                        es.boopurno.gravestones.client.gui.GravestoneScreen::new);

                es.boopurno.gravestones.client.config.ModMenuIntegration.registerConfigScreen();
            });
        }
    }
}
