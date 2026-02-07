package com.baldeagle.network.message;

import com.baldeagle.blocks.research.tile.TileEntityResearchAssembler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ResearchAssemblerActionMessage implements IMessage {

    public enum Action {
        PREV_TIER,
        NEXT_TIER,
        TOGGLE_MODE,
    }

    private BlockPos pos;
    private Action action;

    public ResearchAssemblerActionMessage() {}

    public ResearchAssemblerActionMessage(BlockPos pos, Action action) {
        this.pos = pos;
        this.action = action;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeByte(action.ordinal());
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int x = buf.readInt();
        int y = buf.readInt();
        int z = buf.readInt();
        pos = new BlockPos(x, y, z);
        int ord = buf.readUnsignedByte();
        action =
            ord < Action.values().length
                ? Action.values()[ord]
                : Action.NEXT_TIER;
    }

    public static class Handler
        implements IMessageHandler<ResearchAssemblerActionMessage, IMessage>
    {

        @Override
        public IMessage onMessage(
            ResearchAssemblerActionMessage message,
            MessageContext ctx
        ) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null || message == null) {
                return null;
            }
            player
                .getServerWorld()
                .addScheduledTask(() -> {
                    TileEntity tile = player
                        .getServerWorld()
                        .getTileEntity(message.pos);
                    if (!(tile instanceof TileEntityResearchAssembler)) {
                        return;
                    }
                    TileEntityResearchAssembler assembler =
                        (TileEntityResearchAssembler) tile;
                    if (message.action == Action.PREV_TIER) {
                        assembler.cycleTier(false);
                    } else if (message.action == Action.NEXT_TIER) {
                        assembler.cycleTier(true);
                    } else if (message.action == Action.TOGGLE_MODE) {
                        assembler.toggleAutoCreateCores();
                    }
                });
            return null;
        }
    }
}
