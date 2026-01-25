package com.baldeagle.bank;

import com.baldeagle.BaldeagleCore;
import com.baldeagle.bank.GuiHandler;
import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.country.mint.tile.TileEntityMint;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class BlockMint extends Block {

    public static final PropertyDirection FACING = PropertyDirection.create(
        "facing",
        EnumFacing.Plane.HORIZONTAL
    );

    public BlockMint() {
        super(Material.IRON);
        setRegistryName(BaldeagleCore.MODID, "mint");
        setTranslationKey("baldeaglecore.mint");
        setHardness(4.5F);
        setCreativeTab(com.baldeagle.country.creativetab.EconomyTab.INSTANCE);
        this.setDefaultState(
            this.blockState.getBaseState().withProperty(
                FACING,
                EnumFacing.NORTH
            )
        );
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
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
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityMint();
    }

    @Override
    public void onBlockPlacedBy(
        World world,
        BlockPos pos,
        IBlockState state,
        EntityLivingBase placer,
        ItemStack stack
    ) {
        if (world.isRemote || !(placer instanceof EntityPlayer)) {
            return;
        }
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof TileEntityMint) {
            EntityPlayer player = (EntityPlayer) placer;
            Country country = CountryManager.getCountryForPlayer(
                world,
                player.getUniqueID()
            );
            if (country != null) {
                ((TileEntityMint) tileEntity).setCountryId(country.getId());
            }
        }
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
        if (!(tileEntity instanceof TileEntityMint)) {
            return false;
        }

        TileEntityMint mint = (TileEntityMint) tileEntity;
        if (!mint.ensureCountry(world, player)) {
            return false;
        }
        if (!mint.isAuthorized(player)) {
            player.sendStatusMessage(
                new TextComponentString(
                    "You are not authorized to use this mint."
                ),
                true
            );
            return false;
        }

        player.openGui(
            BaldeagleCore.instance,
            GuiHandler.MINT_GUI_ID,
            world,
            pos.getX(),
            pos.getY(),
            pos.getZ()
        );
        return true;
    }
}
