package com.baldeagle.bank;

import com.baldeagle.BaldeagleCore;
import com.baldeagle.country.creativetab.BaldeagleCoreTab;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockBank extends Block {

    public BlockBank() {
        super(Material.IRON);
        setRegistryName(BaldeagleCore.MODID, "bank");
        setTranslationKey("baldeaglecore.bank");
        setHardness(3.5F);
        setCreativeTab(BaldeagleCoreTab.INSTANCE);
        this.setDefaultState(
            this.blockState.getBaseState().withProperty(
                FACING,
                EnumFacing.NORTH
            )
        );
    }

    public static final PropertyDirection FACING = PropertyDirection.create(
        "facing",
        EnumFacing.Plane.HORIZONTAL
    );

    @Override
    public IBlockState getStateForPlacement(
        World world,
        BlockPos pos,
        EnumFacing facing,
        float hitX,
        float hitY,
        float hitZ,
        int meta,
        EntityLivingBase placer
    ) {
        return this.getDefaultState().withProperty(
            FACING,
            placer.getHorizontalFacing().getOpposite()
        );
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(
            FACING,
            rot.rotate((EnumFacing) state.getValue(FACING))
        );
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(
            mirror.toRotation((EnumFacing) state.getValue(FACING))
        );
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(
            FACING,
            EnumFacing.byHorizontalIndex(meta & 3)
        );
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
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

        TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof TileEntityBank)) {
            return false;
        }

        player.openGui(
            BaldeagleCore.instance,
            GuiHandler.BANK_GUI_ID,
            world,
            pos.getX(),
            pos.getY(),
            pos.getZ()
        );
        return true;
    }
}
