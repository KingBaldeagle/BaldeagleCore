package com.baldeagle.network.message;

import com.baldeagle.country.mint.tile.TileEntityCurrencyExchange;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.*;

public class ExchangeActionMessage implements IMessage {

    public enum Action {
        PREV_COUNTRY,
        NEXT_COUNTRY,
        PREV_DENOMINATION,
        NEXT_DENOMINATION,
        EXECUTE,
    }

    private BlockPos pos;
    private Action action;

    public ExchangeActionMessage() {}

    public ExchangeActionMessage(BlockPos pos, Action action) {
        this.pos = pos;
        this.action = action;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeByte(action.ordinal());
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        action = Action.values()[buf.readByte()];
    }

    public static class Handler
        implements IMessageHandler<ExchangeActionMessage, IMessage>
    {

        @Override
        public IMessage onMessage(
            ExchangeActionMessage message,
            MessageContext ctx
        ) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player
                .getServerWorld()
                .addScheduledTask(() -> {
                    TileEntity tile = player.world.getTileEntity(message.pos);
                    if (!(tile instanceof TileEntityCurrencyExchange)) {
                        return;
                    }
                    TileEntityCurrencyExchange exchange =
                        (TileEntityCurrencyExchange) tile;
                    switch (message.action) {
                        case PREV_COUNTRY:
                            exchange.handleCycleTarget(false);
                            break;
                        case NEXT_COUNTRY:
                            exchange.handleCycleTarget(true);
                            break;
                        case PREV_DENOMINATION:
                            exchange.handleCycleDenomination(false);
                            break;
                        case NEXT_DENOMINATION:
                            exchange.handleCycleDenomination(true);
                            break;
                        case EXECUTE:
                            exchange.executeExchange(player);
                            break;
                    }
                });
            return null;
        }
    }
}
