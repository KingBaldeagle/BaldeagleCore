package com.baldeagle.territory;

import com.baldeagle.BaldeagleCore;
import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.creativetab.BaldeagleCoreTab;
import java.util.UUID;
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

public class BlockCaptureFlag extends Block {

    public BlockCaptureFlag() {
        super(Material.WOOD);
        setRegistryName(BaldeagleCore.MODID, "capture_flag");
        setTranslationKey("baldeaglecore.capture_flag");
        setHardness(2.0F);
        setCreativeTab(BaldeagleCoreTab.INSTANCE);
    }

    @Override
    public boolean onBlockActivated(
        World worldIn,
        BlockPos pos,
        IBlockState state,
        EntityPlayer playerIn,
        net.minecraft.util.EnumHand hand,
        net.minecraft.util.EnumFacing facing,
        float hitX,
        float hitY,
        float hitZ
    ) {
        if (worldIn.isRemote) {
            return true;
        }

        ChunkPos chunk = new ChunkPos(pos);
        TerritoryManager.DimChunkKey key = TerritoryManager.chunkKey(worldIn, chunk);
        TerritoryData data = TerritoryData.get(worldIn);
        
        CaptureSystem.CaptureEntry capture = data.getCaptureData().get(key);
        
        if (capture == null) {
            return false;
        }

        Country attacker = CountryManager.getCountry(worldIn, capture.attackerId);
        Country defender = CountryManager.getCountry(worldIn, capture.defenderId);
        
        String attackerName = attacker != null ? attacker.getName() : "Unknown";
        String defenderName = defender != null ? defender.getName() : "Unknown";

        long ticksRemaining = CaptureSystem.getTicksRemaining(capture);
        long hoursRemaining = ticksRemaining / 1000;
        long minutesRemaining = (ticksRemaining % 1000) / 1000 * 60;
        
        double progress = CaptureSystem.getCaptureProgress(capture) * 100;
        
        String stageName = capture.stage.name();
        
        playerIn.sendStatusMessage(
            new TextComponentString("§6=== Capture Information ==="), 
            true
        );
        playerIn.sendStatusMessage(
            new TextComponentString("§eAttacker: §f" + attackerName), 
            true
        );
        playerIn.sendStatusMessage(
            new TextComponentString("§eDefender: §f" + defenderName), 
            true
        );
        playerIn.sendStatusMessage(
            new TextComponentString("§eStage: §f" + stageName), 
            true
        );
        playerIn.sendStatusMessage(
            new TextComponentString("§eProgress: §f" + String.format("%.1f%%", progress)), 
            true
        );
        playerIn.sendStatusMessage(
            new TextComponentString("§eTime Remaining: §f" + hoursRemaining + " hours, " + minutesRemaining + " minutes"), 
            true
        );

        return true;
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

        if (!(placer instanceof EntityPlayer)) {
            worldIn.setBlockToAir(pos);
            return;
        }

        EntityPlayer player = (EntityPlayer) placer;
        Country attackerCountry = CountryManager.getCountryForPlayer(
            worldIn,
            player.getUniqueID()
        );

        if (attackerCountry == null) {
            player.sendStatusMessage(
                new TextComponentString("Join a country to place capture flags"),
                true
            );
            refundAndRemove(worldIn, pos, player);
            return;
        }

        ChunkPos chunk = new ChunkPos(pos);
        UUID owningCountryId = TerritoryManager.getOwningCountryId(worldIn, chunk);

        if (owningCountryId == null) {
            player.sendStatusMessage(
                new TextComponentString("You can only place capture flags on claimed territory"),
                true
            );
            refundAndRemove(worldIn, pos, player);
            return;
        }

        if (owningCountryId.equals(attackerCountry.getId())) {
            player.sendStatusMessage(
                new TextComponentString("You cannot capture your own territory"),
                true
            );
            refundAndRemove(worldIn, pos, player);
            return;
        }

        Country defenderCountry = CountryManager.getCountry(worldIn, owningCountryId);
        if (defenderCountry == null) {
            refundAndRemove(worldIn, pos, player);
            return;
        }

        if (!WarManager.canCapture(attackerCountry.getId(), owningCountryId, worldIn)) {
            player.sendStatusMessage(
                new TextComponentString("You must be at war with this nation to capture their territory"),
                true
            );
            refundAndRemove(worldIn, pos, player);
            return;
        }

        if (!CaptureSystem.hasSupplyLine(worldIn, chunk, attackerCountry.getId())) {
            player.sendStatusMessage(
                new TextComponentString("Capture flag must be connected to your territory (supply line required)"),
                true
            );
            refundAndRemove(worldIn, pos, player);
            return;
        }

        TerritoryData data = TerritoryData.get(worldIn);
        TerritoryManager.DimChunkKey key = TerritoryManager.chunkKey(worldIn, chunk);
        
        if (data.getCaptureData().containsKey(key)) {
            player.sendStatusMessage(
                new TextComponentString("This chunk already has an active capture"),
                true
            );
            refundAndRemove(worldIn, pos, player);
            return;
        }

        CaptureSystem.startCapture(worldIn, chunk, attackerCountry.getId(), owningCountryId, pos);
        data.markDirty();
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote) {
            ChunkPos chunk = new ChunkPos(pos);
            TerritoryManager.DimChunkKey key = TerritoryManager.chunkKey(worldIn, chunk);
            TerritoryData data = TerritoryData.get(worldIn);
            
            CaptureSystem.CaptureEntry capture = data.getCaptureData().get(key);
            if (capture != null && capture.captureBlockPos.equals(pos)) {
                CaptureSystem.resetCapture(worldIn, chunk);
                data.markDirty();
            }
        }
        super.breakBlock(worldIn, pos, state);
    }

    private void refundAndRemove(
        World world,
        BlockPos pos,
        EntityPlayer player
    ) {
        world.setBlockToAir(pos);
        if (player != null && !player.capabilities.isCreativeMode) {
            spawnAsEntity(world, pos, new ItemStack(this));
        }
    }
}
