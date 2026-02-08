package com.baldeagle.items;

import com.baldeagle.chunkmap.ChunkMapConstants;
import com.baldeagle.chunkmap.client.GuiClaimMap;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemClaimMap extends Item {

    public static final String TAG_CENTER_X = "centerChunkX";
    public static final String TAG_CENTER_Z = "centerChunkZ";
    public static final String TAG_DIMENSION = "dimensionId";
    public static final String TAG_ZOOM = "zoomLevel";

    public ItemClaimMap() {
        super();
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(
        World world,
        EntityPlayer player,
        EnumHand hand
    ) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            ensureInitialized(stack, player);
        }
        if (world.isRemote) {
            openMapGui(stack);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    public static void ensureInitialized(ItemStack stack, EntityPlayer player) {
        if (stack == null || player == null) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        if (!isInitialized(tag)) {
            tag.setInteger(TAG_CENTER_X, player.chunkCoordX);
            tag.setInteger(TAG_CENTER_Z, player.chunkCoordZ);
            tag.setInteger(TAG_DIMENSION, player.dimension);
            tag.setInteger(TAG_ZOOM, ChunkMapConstants.DEFAULT_GRID);
        }
    }

    public static boolean isInitialized(NBTTagCompound tag) {
        return tag != null &&
            tag.hasKey(TAG_CENTER_X) &&
            tag.hasKey(TAG_CENTER_Z) &&
            tag.hasKey(TAG_DIMENSION) &&
            tag.hasKey(TAG_ZOOM);
    }

    @SideOnly(Side.CLIENT)
    private void openMapGui(ItemStack stack) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiClaimMap(stack));
    }
}
