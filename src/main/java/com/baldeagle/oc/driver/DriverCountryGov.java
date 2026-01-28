package com.baldeagle.oc.driver;

import com.baldeagle.oc.env.EnvironmentCountryGov;
import com.baldeagle.oc.gov.TileEntityGovernmentComputer;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DriverCountryGov extends DriverSidedTileEntity {

    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityGovernmentComputer.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(
        World world,
        BlockPos pos,
        EnumFacing side
    ) {
        if (world == null || world.isRemote) {
            return null;
        }
        Network.joinOrCreateNetwork(world, pos);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityGovernmentComputer) {
            return new EnvironmentCountryGov(
                (TileEntityGovernmentComputer) tile
            );
        }
        return null;
    }
}
