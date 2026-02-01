package com.baldeagle.bank;

import com.baldeagle.BaldeagleCore;
import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.country.creativetab.BaldeagleCoreTab;
import com.baldeagle.country.vault.tile.TileEntityVault;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class BlockVault extends Block {

    public static final PropertyDirection FACING = PropertyDirection.create(
        "facing",
        EnumFacing.Plane.HORIZONTAL
    );

    public BlockVault() {
        super(Material.IRON);
        setRegistryName(BaldeagleCore.MODID, "vault");
        setTranslationKey("baldeaglecore.vault");
        setHardness(50.0F);
        setResistance(2000.0F);
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
        return new TileEntityVault();
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
        if (!(tileEntity instanceof TileEntityVault)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) placer;
        Country country = CountryManager.getCountryForPlayer(
            world,
            player.getUniqueID()
        );
        if (country != null) {
            ((TileEntityVault) tileEntity).setCountryId(country.getId());
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
        if (!(tileEntity instanceof TileEntityVault)) {
            return false;
        }

        TileEntityVault vault = (TileEntityVault) tileEntity;
        if (!vault.ensureCountry(player)) {
            player.sendStatusMessage(
                new TextComponentString("Join a country first."),
                true
            );
            return false;
        }
        if (!vault.isAuthorized(player)) {
            player.sendStatusMessage(
                new TextComponentString(
                    "You are not authorized to use this vault."
                ),
                true
            );
            return false;
        }

        player.openGui(
            BaldeagleCore.instance,
            GuiHandler.VAULT_GUI_ID,
            world,
            pos.getX(),
            pos.getY(),
            pos.getZ()
        );
        return true;
    }

    @Override
    public boolean removedByPlayer(
        IBlockState state,
        World world,
        BlockPos pos,
        EntityPlayer player,
        boolean willHarvest
    ) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof TileEntityVault) {
            TileEntityVault vault = (TileEntityVault) tileEntity;
            if (!vault.isAuthorized(player)) {
                if (!world.isRemote) {
                    player.sendStatusMessage(
                        new TextComponentString(
                            "You are not authorized to break this vault."
                        ),
                        true
                    );
                }
                return false;
            }
        }
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity tileentity = worldIn.getTileEntity(pos);
        if (tileentity instanceof TileEntityVault) {
            if (!worldIn.isRemote) {
                ((TileEntityVault) tileentity).prepareForDropAndUntrack();
            }
            InventoryHelper.dropInventoryItems(
                worldIn,
                pos,
                (TileEntityVault) tileentity
            );
        }
        super.breakBlock(worldIn, pos, state);
    }
}
