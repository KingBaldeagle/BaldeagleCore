package com.baldeagle.oc.gov;

import com.baldeagle.BaldeagleCore;
import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.country.creativetab.EconomyTab;
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
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class BlockGovernmentComputer extends Block {

    public static final PropertyDirection FACING = PropertyDirection.create(
        "facing",
        EnumFacing.Plane.HORIZONTAL
    );

    public BlockGovernmentComputer() {
        super(Material.IRON);
        setRegistryName(BaldeagleCore.MODID, "government_computer");
        setTranslationKey("baldeaglecore.government_computer");
        setHardness(3.5F);
        setCreativeTab(EconomyTab.INSTANCE);
        this.setDefaultState(
            this.blockState.getBaseState().withProperty(
                FACING,
                EnumFacing.NORTH
            )
        );
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityGovernmentComputer();
    }

    @Override
    public void onBlockPlacedBy(
        World worldIn,
        BlockPos pos,
        IBlockState state,
        EntityLivingBase placer,
        net.minecraft.item.ItemStack stack
    ) {
        if (worldIn.isRemote) {
            return;
        }
        if (!(placer instanceof EntityPlayer)) {
            return;
        }
        TileEntity tile = worldIn.getTileEntity(pos);
        if (!(tile instanceof TileEntityGovernmentComputer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) placer;
        TileEntityGovernmentComputer gov = (TileEntityGovernmentComputer) tile;
        gov.bindToPlayer(player);
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
        if (!(tile instanceof TileEntityGovernmentComputer)) {
            return false;
        }
        TileEntityGovernmentComputer gov = (TileEntityGovernmentComputer) tile;
        Country country = CountryManager.getCountryForPlayer(
            world,
            player.getUniqueID()
        );
        if (country == null) {
            player.sendStatusMessage(
                new TextComponentString("Join a country first."),
                true
            );
            return true;
        }
        if (!country.isHighAuthority(player.getUniqueID())) {
            player.sendStatusMessage(
                new TextComponentString("You are not authorized."),
                true
            );
            return true;
        }
        gov.setCountryId(country.getId());
        player.sendStatusMessage(
            new TextComponentString(
                "Government computer bound to " + country.getName() + "."
            ),
            true
        );
        return true;
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
}
