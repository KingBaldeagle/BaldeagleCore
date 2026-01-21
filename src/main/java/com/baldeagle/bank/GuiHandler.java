package com.baldeagle.bank;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {

    public static final int BANK_GUI_ID = 1;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != BANK_GUI_ID) {
            return null;
        }

        BlockPos pos = new BlockPos(x, y, z);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityBank) {
            return new ContainerBank(player.inventory, (TileEntityBank) tile);
        }

        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != BANK_GUI_ID) {
            return null;
        }

        BlockPos pos = new BlockPos(x, y, z);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityBank) {
            return new GuiBank(new ContainerBank(player.inventory, (TileEntityBank) tile));
        }

        return null;
    }
}