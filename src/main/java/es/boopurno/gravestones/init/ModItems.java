package es.boopurno.gravestones.init;

import es.boopurno.gravestones.Gravestones;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Registry for mod items
 */
public class ModItems {
        public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
                        Gravestones.MODID);

        /**
         * Register the items to the event bus
         */
        public static void register(IEventBus eventBus) {
                ITEMS.register(eventBus);
        }
}