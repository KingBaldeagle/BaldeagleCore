package com.baldeagle.bank;

import com.baldeagle.BaldeagleCore;
import com.baldeagle.country.creativetab.EconomyTab;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockBank extends Block {

    public BlockBank() {
        super(Material.IRON);
        setRegistryName(BaldeagleCore.MODID, "bank");
        setTranslationKey("baldeaglecore.bank");
        setHardness(3.5F);
        setCreativeTab(EconomyTab.INSTANCE);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityBank();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }

        TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof TileEntityBank)) {
            return false;
        }

        player.openGui(
                BaldeagleCore.instance,
                GuiHandler.BANK_GUI_ID,
                world, pos.getX(), pos.getY(), pos.getZ()
        );
        return true;
    }
}