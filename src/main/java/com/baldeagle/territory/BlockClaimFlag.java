package com.baldeagle.territory;

import com.baldeagle.BaldeagleCore;
import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.country.creativetab.EconomyTab;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class BlockClaimFlag extends Block {

    public BlockClaimFlag() {
        super(Material.WOOD);
        setRegistryName(BaldeagleCore.MODID, "claim_flag");
        setTranslationKey("baldeaglecore.claim_flag");
        setHardness(2.0F);
        setCreativeTab(EconomyTab.INSTANCE);
    }

    @Override
    public void onBlockPlacedBy(
        World worldIn,
        BlockPos pos,
        IBlockState state,
        EntityLivingBase placer,
        ItemStack stack
    ) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

        if (worldIn.isRemote) {
            return;
        }

        // Defense-in-depth: enforce "one flag per chunk" and "must be in a country"
        // even if the placement event handler is missed.
        if (!(placer instanceof EntityPlayer)) {
            worldIn.setBlockToAir(pos);
            return;
        }

        EntityPlayer player = (EntityPlayer) placer;
        Country country = CountryManager.getCountryForPlayer(
            worldIn,
            player.getUniqueID()
        );
        if (country == null) {
            player.sendStatusMessage(
                new TextComponentString("Join a country to claim chunks"),
                true
            );
            refundAndRemove(worldIn, pos, player);
            return;
        }

        ChunkPos chunk = new ChunkPos(pos);
        TerritoryData data = TerritoryData.get(worldIn);
        long key = TerritoryManager.chunkKey(chunk);
        TerritoryData.ClaimEntry existing = data.getClaims().get(key);

        if (existing != null) {
            // Stale-claim recovery: if the only flag in the chunk is this one, let placement
            // overwrite the old data (this can happen if the flag was removed without break events).
            if (
                existing.flagPos.equals(pos) &&
                !TerritoryManager.chunkHasOtherFlag(worldIn, chunk, pos)
            ) {
                data.getClaims().remove(key);
                data.markDirty();
                existing = null;
            }
        }

        if (existing != null) {
            // Allow the existing claim only if it is this exact flag placement.
            if (
                existing.countryId.equals(country.getId()) &&
                existing.flagPos.equals(pos)
            ) {
                return;
            }
            refundAndRemove(worldIn, pos, player);
            return;
        }

        if (TerritoryManager.chunkHasOtherFlag(worldIn, chunk, pos)) {
            refundAndRemove(worldIn, pos, player);
            return;
        }

        if (
            !TerritoryManager.claimChunk(worldIn, chunk, country.getId(), pos)
        ) {
            refundAndRemove(worldIn, pos, player);
        }
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote) {
            TerritoryManager.unclaimChunkIfFlagMatches(worldIn, pos);
        }
        super.breakBlock(worldIn, pos, state);
    }

    private void refundAndRemove(
        World world,
        BlockPos pos,
        EntityPlayer player
    ) {
        // Remove first to avoid duplicate claim/unclaim bookkeeping.
        world.setBlockToAir(pos);
        if (player != null && !player.capabilities.isCreativeMode) {
            spawnAsEntity(world, pos, new ItemStack(this));
        }
    }
}
