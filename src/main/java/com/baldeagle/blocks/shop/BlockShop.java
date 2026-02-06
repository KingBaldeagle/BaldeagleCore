package com.baldeagle.blocks.shop;

import com.baldeagle.BaldeagleCore;
import com.baldeagle.GuiHandler;
import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.creativetab.BaldeagleCoreTab;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockShop extends Block {

    public static final PropertyDirection FACING = PropertyDirection.create(
        "facing",
        EnumFacing.Plane.HORIZONTAL
    );

    public BlockShop() {
        super(Material.IRON);
        setRegistryName(BaldeagleCore.MODID, "shop");
        setTranslationKey("baldeaglecore.shop");
        setHardness(4.0F);
        setCreativeTab(BaldeagleCoreTab.INSTANCE);
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
        return new TileEntityShop();
    }

    @Override
    public void onBlockPlacedBy(
        World world,
        BlockPos pos,
        IBlockState state,
        EntityLivingBase placer,
        net.minecraft.item.ItemStack stack
    ) {
        if (world.isRemote || !(placer instanceof EntityPlayer)) {
            return;
        }
        TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof TileEntityShop)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) placer;
        Country country = CountryManager.getCountryForPlayer(
            world,
            player.getUniqueID()
        );
        TileEntityShop shop = (TileEntityShop) tileEntity;
        shop.setOwner(player.getUniqueID());
        if (country != null) {
            shop.setCountryId(country.getId());
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
        if (!(tileEntity instanceof TileEntityShop)) {
            return false;
        }
        player.openGui(
            BaldeagleCore.instance,
            GuiHandler.SHOP_GUI_ID,
            world,
            pos.getX(),
            pos.getY(),
            pos.getZ()
        );
        return true;
    }
}
