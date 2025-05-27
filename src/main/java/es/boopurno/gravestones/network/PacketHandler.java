package es.boopurno.gravestones.network;

import es.boopurno.gravestones.Gravestones;
import es.boopurno.gravestones.network.serverbound.ServerboundTransferItemsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(Gravestones.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        INSTANCE.messageBuilder(ServerboundTransferItemsPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .encoder(ServerboundTransferItemsPacket::encode)
            .decoder(ServerboundTransferItemsPacket::new)
            .consumerMainThread(ServerboundTransferItemsPacket::handle)
            .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.send(PacketDistributor.SERVER.noArg(), message);
    }
}