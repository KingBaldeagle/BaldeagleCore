package com.baldeagle.bank;

import com.baldeagle.BaldeagleCore;
import com.baldeagle.country.mint.tile.TileEntityCurrencyExchange;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockCurrencyExchange extends Block {

    public BlockCurrencyExchange() {
        super(Material.IRON);
        setRegistryName(BaldeagleCore.MODID, "currency_exchange");
        setTranslationKey("baldeaglecore.currency_exchange");
        setHardness(4.0F);
        setCreativeTab(com.baldeagle.country.creativetab.EconomyTab.INSTANCE);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityCurrencyExchange();
    }

    @Override
    public boolean onBlockActivated(
        World world,
        BlockPos pos,
        IBlockState state,
        EntityPlayer player,
        EnumHand hand,
        EnumFacing facing,
        float hitX,
        float hitY,
        float hitZ
    ) {
        if (world.isRemote) {
            return true;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityCurrencyExchange)) {
            return false;
        }
        player.openGui(
            BaldeagleCore.instance,
            GuiHandler.CURRENCY_EXCHANGE_GUI_ID,
            world,
            pos.getX(),
            pos.getY(),
            pos.getZ()
        );
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity tileentity = worldIn.getTileEntity(pos);
        if (tileentity instanceof TileEntityCurrencyExchange) {
            InventoryHelper.dropInventoryItems(
                worldIn,
                pos,
                (TileEntityCurrencyExchange) tileentity
            );
        }
        super.breakBlock(worldIn, pos, state);
    }
}
