package com.baldeagle.territory;

import com.baldeagle.bank.ModBlocks;
import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "baldeaglecore")
public class TerritoryProtectionHandler {

    @SubscribeEvent
    public static void onPlace(BlockEvent.PlaceEvent event) {
        EntityPlayer player = event.getPlayer();
        World world = event.getWorld();
        if (player == null || world == null || world.isRemote) {
            return;
        }

        BlockPos pos = event.getPos();
        ChunkPos chunk = new ChunkPos(pos);

        boolean isClaimFlag =
            event.getPlacedBlock().getBlock() == ModBlocks.CLAIM_FLAG;

        if (isClaimFlag) {
            Country playerCountry = CountryManager.getCountryForPlayer(
                world,
                player.getUniqueID()
            );
            if (playerCountry == null) {
                event.setCanceled(true);
                player.sendStatusMessage(
                    new TextComponentString("Join a country to claim chunks"),
                    true
                );
                return;
            }

            // PlaceEvent fires after the block is placed. If a stale claim exists whose stored
            // flag position matches this placement, validate against the pre-place state.
            TerritoryData data = TerritoryData.get(world);
            long key = TerritoryManager.chunkKey(chunk);
            TerritoryData.ClaimEntry raw = data.getClaims().get(key);
            if (raw != null && pos.equals(raw.flagPos)) {
                IBlockState replaced = null;
                try {
                    replaced = event.getBlockSnapshot().getReplacedBlock();
                } catch (Throwable ignored) {}
                if (
                    replaced != null &&
                    replaced.getBlock() != ModBlocks.CLAIM_FLAG
                ) {
                    data.getClaims().remove(key);
                    data.markDirty();
                }
            }

            TerritoryData.ClaimEntry claim = TerritoryManager.getClaim(
                world,
                chunk
            );
            if (claim != null) {
                event.setCanceled(true);
                player.sendStatusMessage(
                    new TextComponentString("This chunk is already claimed."),
                    true
                );
                return;
            }
            if (TerritoryManager.chunkHasOtherFlag(world, chunk, pos)) {
                event.setCanceled(true);
                player.sendStatusMessage(
                    new TextComponentString(
                        "Only one claim flag is allowed per chunk."
                    ),
                    true
                );
                return;
            }

            boolean ok = TerritoryManager.claimChunk(
                world,
                chunk,
                playerCountry.getId(),
                pos
            );
            if (!ok) {
                event.setCanceled(true);
                player.sendStatusMessage(
                    new TextComponentString("Failed to claim this chunk."),
                    true
                );
            } else {
                player.sendStatusMessage(
                    new TextComponentString(
                        "Chunk now claimed by " + playerCountry.getName()
                    ),
                    true
                );
            }
            return;
        }

        TerritoryData.ClaimEntry claim = TerritoryManager.getClaim(
            world,
            chunk
        );

        // Regular block placement protection.
        if (claim == null) {
            return;
        }

        Country owner = CountryManager.getCountry(world, claim.countryId);
        Country playerCountry = CountryManager.getCountryForPlayer(
            world,
            player.getUniqueID()
        );
        boolean isOwnerMember =
            owner != null && owner.isMember(player.getUniqueID());
        boolean isAllied =
            owner != null &&
            playerCountry != null &&
            owner.isAlliedWith(playerCountry.getId());

        if (!isOwnerMember && !isAllied) {
            event.setCanceled(true);
            player.sendStatusMessage(
                new TextComponentString("This land is claimed."),
                true
            );
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        World world = event.getWorld();
        if (player == null || world == null || world.isRemote) {
            return;
        }

        BlockPos pos = event.getPos();
        ChunkPos chunk = new ChunkPos(pos);

        boolean isClaimFlag =
            event.getState().getBlock() == ModBlocks.CLAIM_FLAG;
        Country playerCountry = CountryManager.getCountryForPlayer(
            world,
            player.getUniqueID()
        );

        // Players without a country can never break claim flags (even if unclaimed).
        if (isClaimFlag) {
            if (playerCountry == null) {
                event.setCanceled(true);
                player.sendStatusMessage(
                    new TextComponentString("Join a country to claim chunks"),
                    true
                );
                return;
            }
        }

        TerritoryData.ClaimEntry claim = TerritoryManager.getClaim(
            world,
            chunk
        );
        if (claim == null) {
            return;
        }

        Country owner = CountryManager.getCountry(world, claim.countryId);
        boolean isOwnerMember =
            owner != null && owner.isMember(player.getUniqueID());
        boolean isAllied =
            owner != null &&
            playerCountry != null &&
            owner.isAlliedWith(playerCountry.getId());

        if (isClaimFlag) {
            if (isOwnerMember) {
                TerritoryManager.unclaimChunk(world, chunk);
                return;
            }

            if (isAllied) {
                event.setCanceled(true);
                player.sendStatusMessage(
                    new TextComponentString(
                        "You cannot break an allied claim flag."
                    ),
                    true
                );
                return;
            }

            if (
                playerCountry != null &&
                WarManager.canCapture(
                    playerCountry.getId(),
                    claim.countryId,
                    world
                )
            ) {
                TerritoryManager.unclaimChunk(world, chunk);
                return;
            }

            event.setCanceled(true);
            player.sendStatusMessage(
                new TextComponentString(
                    "You cannot break this claim flag right now."
                ),
                true
            );
            return;
        }

        // Non-flag block breaks are always protected for non-owners.
        if (!isOwnerMember && !isAllied) {
            event.setCanceled(true);
            player.sendStatusMessage(
                new TextComponentString("This land is claimed."),
                true
            );
        }
    }
}
