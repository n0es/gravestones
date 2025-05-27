package es.boopurno.gravestones.network.serverbound;

import es.boopurno.gravestones.menu.GravestoneMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundTransferItemsPacket {
    private final BlockPos blockPos;

    public ServerboundTransferItemsPacket(BlockPos pos) {
        this.blockPos = pos;
    }

    public ServerboundTransferItemsPacket(FriendlyByteBuf buf) {
        this.blockPos = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.blockPos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof GravestoneMenu gravestoneMenu) {
                if (gravestoneMenu.getBlockEntity() != null
                        && gravestoneMenu.getBlockEntity().getBlockPos().equals(this.blockPos)) {
                    gravestoneMenu.handleTransferItems();
                }
            }
        });
        return true;
    }
}