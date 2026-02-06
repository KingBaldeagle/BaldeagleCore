package com.baldeagle.network.message;

import com.baldeagle.blocks.mint.tile.TileEntityMint;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.*;

public class MintActionMessage implements IMessage {

    public enum Action {
        TOGGLE_TYPE,
        PREV_DENOMINATION,
        NEXT_DENOMINATION,
        DECREASE_AMOUNT,
        INCREASE_AMOUNT,
        EXECUTE,
    }

    private BlockPos pos;
    private Action action;
    private int payload;

    public MintActionMessage() {}

    public MintActionMessage(BlockPos pos, Action action, int payload) {
        this.pos = pos;
        this.action = action;
        this.payload = payload;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeByte(action.ordinal());
        buf.writeInt(payload);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        action = Action.values()[buf.readByte()];
        payload = buf.readInt();
    }

    public static class Handler
        implements IMessageHandler<MintActionMessage, IMessage>
    {

        @Override
        public IMessage onMessage(
            MintActionMessage message,
            MessageContext ctx
        ) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player
                .getServerWorld()
                .addScheduledTask(() -> {
                    TileEntity tile = player.world.getTileEntity(message.pos);
                    if (!(tile instanceof TileEntityMint)) {
                        return;
                    }
                    TileEntityMint mint = (TileEntityMint) tile;
                    switch (message.action) {
                        case TOGGLE_TYPE:
                            mint.toggleType(player);
                            break;
                        case PREV_DENOMINATION:
                            mint.cycleDenomination(false, player);
                            break;
                        case NEXT_DENOMINATION:
                            mint.cycleDenomination(true, player);
                            break;
                        case DECREASE_AMOUNT:
                            mint.changeAmount(-1, player);
                            break;
                        case INCREASE_AMOUNT:
                            mint.changeAmount(1, player);
                            break;
                        case EXECUTE:
                            mint.performMint(player);
                            break;
                    }
                });
            return null;
        }
    }
}
