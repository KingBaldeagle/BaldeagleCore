package com.baldeagle.network;

import com.baldeagle.BaldeagleCore;
import com.baldeagle.network.message.AtmBalanceSyncMessage;
import com.baldeagle.network.message.AtmSyncMessage;
import com.baldeagle.network.message.AtmWithdrawMessage;
import com.baldeagle.network.message.BankSyncMessage;
import com.baldeagle.network.message.ChunkMapRequestMessage;
import com.baldeagle.network.message.ChunkMapSnapshotMessage;
import com.baldeagle.network.message.ExchangeActionMessage;
import com.baldeagle.network.message.ExchangeSyncMessage;
import com.baldeagle.network.message.MintActionMessage;
import com.baldeagle.network.message.MintSyncMessage;
import com.baldeagle.network.message.ShopActionMessage;
import com.baldeagle.network.message.VaultSyncMessage;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class NetworkHandler {

    public static final SimpleNetworkWrapper INSTANCE =
        NetworkRegistry.INSTANCE.newSimpleChannel(BaldeagleCore.MODID);

    private NetworkHandler() {}

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(
            MintActionMessage.Handler.class,
            MintActionMessage.class,
            id++,
            Side.SERVER
        );
        INSTANCE.registerMessage(
            MintSyncMessage.Handler.class,
            MintSyncMessage.class,
            id++,
            Side.CLIENT
        );
        INSTANCE.registerMessage(
            ExchangeActionMessage.Handler.class,
            ExchangeActionMessage.class,
            id++,
            Side.SERVER
        );
        INSTANCE.registerMessage(
            ExchangeSyncMessage.Handler.class,
            ExchangeSyncMessage.class,
            id++,
            Side.CLIENT
        );
        INSTANCE.registerMessage(
            AtmWithdrawMessage.Handler.class,
            AtmWithdrawMessage.class,
            id++,
            Side.SERVER
        );
        INSTANCE.registerMessage(
            AtmSyncMessage.Handler.class,
            AtmSyncMessage.class,
            id++,
            Side.CLIENT
        );
        INSTANCE.registerMessage(
            AtmBalanceSyncMessage.Handler.class,
            AtmBalanceSyncMessage.class,
            id++,
            Side.CLIENT
        );
        INSTANCE.registerMessage(
            ShopActionMessage.Handler.class,
            ShopActionMessage.class,
            id++,
            Side.SERVER
        );
        INSTANCE.registerMessage(
            VaultSyncMessage.Handler.class,
            VaultSyncMessage.class,
            id++,
            Side.CLIENT
        );
        INSTANCE.registerMessage(
            BankSyncMessage.Handler.class,
            BankSyncMessage.class,
            id++,
            Side.CLIENT
        );

        INSTANCE.registerMessage(
            ChunkMapRequestMessage.Handler.class,
            ChunkMapRequestMessage.class,
            id++,
            Side.SERVER
        );
        INSTANCE.registerMessage(
            ChunkMapSnapshotMessage.Handler.class,
            ChunkMapSnapshotMessage.class,
            id++,
            Side.CLIENT
        );
    }

    public static void sendToAllAround(
        IMessage message,
        int dimension,
        BlockPos pos
    ) {
        INSTANCE.sendToAllAround(
            message,
            new NetworkRegistry.TargetPoint(
                dimension,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                64
            )
        );
    }
}
