package com.baldeagle.oc.gov;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class TileEntityGovernmentComputer extends TileEntity {

    private UUID owner;
    private UUID countryId;

    public UUID getOwner() {
        return owner;
    }

    public UUID getCountryId() {
        return countryId;
    }

    public void setCountryId(UUID countryId) {
        this.countryId = countryId;
        markDirty();
        sync();
    }

    public void bindToPlayer(EntityPlayer player) {
        if (player == null || world == null || world.isRemote) {
            return;
        }
        owner = player.getUniqueID();
        Country country = CountryManager.getCountryForPlayer(world, owner);
        if (country != null) {
            countryId = country.getId();
        }
        markDirty();
        sync();
    }

    private void sync() {
        if (world == null || world.isRemote) {
            return;
        }
        world.notifyBlockUpdate(
            pos,
            world.getBlockState(pos),
            world.getBlockState(pos),
            3
        );
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (owner != null) {
            compound.setString("owner", owner.toString());
        }
        if (countryId != null) {
            compound.setString("country", countryId.toString());
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        owner = null;
        if (compound.hasKey("owner")) {
            try {
                owner = UUID.fromString(compound.getString("owner"));
            } catch (IllegalArgumentException ignored) {
                owner = null;
            }
        }
        countryId = null;
        if (compound.hasKey("country")) {
            try {
                countryId = UUID.fromString(compound.getString("country"));
            } catch (IllegalArgumentException ignored) {
                countryId = null;
            }
        }
    }
}
