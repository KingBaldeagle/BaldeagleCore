package com.baldeagle.items;

import com.baldeagle.chunkmap.ChunkRelation;
import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.territory.TerritoryData;
import com.baldeagle.territory.TerritoryManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.WorldServer;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemClaimMap extends ItemMap {

    public static final String TAG_CENTER_X = "centerChunkX";
    public static final String TAG_CENTER_Z = "centerChunkZ";
    public static final String TAG_DIMENSION = "dimensionId";
    public static final String TAG_ZOOM = "zoomLevel";
    public static final String TAG_MAP_ID = "mapId";
    public static final String TAG_TERRAIN_RGB = "terrainRgb";
    public static final String TAG_LAST_OVERLAY = "lastOverlayTick";

    private static final int MAP_SIZE = 128;
    private static final int DEFAULT_SCALE = 2;

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
            updateClaimOverlay(stack, player);
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
            tag.setInteger(TAG_ZOOM, DEFAULT_SCALE);

            World world = player.world;
            if (world == null || world.isRemote) {
                return;
            }
            int mapId = world.getUniqueDataId("claim_map");
            tag.setInteger(TAG_MAP_ID, mapId);
            stack.setItemDamage(mapId);

            int centerBlockX = (player.chunkCoordX << 4) + 8;
            int centerBlockZ = (player.chunkCoordZ << 4) + 8;

            MapData data = new MapData("claim_map_" + mapId);
            data.scale = (byte) DEFAULT_SCALE;
            data.dimension = (byte) player.dimension;
            data.xCenter = centerBlockX;
            data.zCenter = centerBlockZ;
            data.trackingPosition = false;
            data.colors = new byte[MAP_SIZE * MAP_SIZE];
            world.getMapStorage().setData(data.mapName, data);

            TerrainSnapshot snapshot = buildTerrainSnapshot(
                world,
                centerBlockX,
                centerBlockZ,
                DEFAULT_SCALE
            );
            tag.setByteArray(TAG_TERRAIN_RGB, snapshot.rgb);
            System.arraycopy(
                snapshot.mapColors,
                0,
                data.colors,
                0,
                snapshot.mapColors.length
            );
            data.markDirty();
        }
    }

    public static boolean isInitialized(NBTTagCompound tag) {
        return tag != null &&
            tag.hasKey(TAG_CENTER_X) &&
            tag.hasKey(TAG_CENTER_Z) &&
            tag.hasKey(TAG_DIMENSION) &&
            tag.hasKey(TAG_ZOOM) &&
            tag.hasKey(TAG_MAP_ID);
    }

    @Override
    public void onUpdate(
        ItemStack stack,
        World world,
        net.minecraft.entity.Entity entity,
        int slot,
        boolean selected
    ) {
        super.onUpdate(stack, world, entity, slot, selected);
        if (!world.isRemote && entity instanceof EntityPlayer) {
            updateClaimOverlay(stack, (EntityPlayer) entity);
        }
    }

    private static void updateClaimOverlay(ItemStack stack, EntityPlayer player) {
        if (stack == null || player == null) {
            return;
        }
        World world = player.world;
        if (world == null || world.isRemote) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (!isInitialized(tag)) {
            return;
        }
        long now = world.getTotalWorldTime();
        long last = tag.getLong(TAG_LAST_OVERLAY);
        if ((now - last) < 40L) {
            return;
        }
        tag.setLong(TAG_LAST_OVERLAY, now);

        int mapId = tag.getInteger(TAG_MAP_ID);
        MapData data = getMapData(world, mapId);
        if (data == null) {
            return;
        }

        byte[] terrainRgb = tag.getByteArray(TAG_TERRAIN_RGB);
        if (terrainRgb.length != MAP_SIZE * MAP_SIZE * 3) {
            return;
        }

        byte[] colors = new byte[MAP_SIZE * MAP_SIZE];
        System.arraycopy(data.colors, 0, colors, 0, colors.length);

        int centerChunkX = tag.getInteger(TAG_CENTER_X);
        int centerChunkZ = tag.getInteger(TAG_CENTER_Z);
        int dimension = tag.getInteger(TAG_DIMENSION);
        int scale = tag.getInteger(TAG_ZOOM);
        int blocksPerPixel = 1 << scale;
        int halfSize = (MAP_SIZE * blocksPerPixel) / 2;
        int startX = (centerChunkX << 4) + 8 - halfSize;
        int startZ = (centerChunkZ << 4) + 8 - halfSize;

        WorldServer dimWorld = getWorldForDimension(player, dimension);
        if (dimWorld == null) {
            return;
        }

        Map<ChunkPos, UUID> ownerCache = buildOwnerCache(
            dimWorld,
            startX,
            startZ,
            blocksPerPixel
        );
        Country viewer = CountryManager.getCountryForPlayer(
            dimWorld,
            player.getUniqueID()
        );

        for (int z = 0; z < MAP_SIZE; z++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                int blockX = startX + (x * blocksPerPixel);
                int blockZ = startZ + (z * blocksPerPixel);
                int chunkX = blockX >> 4;
                int chunkZ = blockZ >> 4;
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                UUID ownerId = ownerCache.get(chunkPos);
                if (ownerId == null) {
                    colors[(z * MAP_SIZE) + x] = mapColorFromRgb(
                        terrainRgb,
                        (z * MAP_SIZE) + x
                    );
                    continue;
                }

                ChunkRelation relation = relationFor(viewer, ownerId);
                int fillRgb = fillColor(relation);
                int blended = blendRgb(
                    terrainRgb,
                    (z * MAP_SIZE) + x,
                    fillRgb,
                    0.35f
                );

                boolean border = isBorderPixel(
                    ownerCache,
                    chunkPos,
                    blockX,
                    blockZ,
                    blocksPerPixel
                );
                if (border) {
                    int borderRgb = borderColor(relation);
                    colors[(z * MAP_SIZE) + x] =
                        nearestMapColorByte(borderRgb);
                } else {
                    colors[(z * MAP_SIZE) + x] =
                        nearestMapColorByte(blended);
                }
            }
        }

        System.arraycopy(colors, 0, data.colors, 0, colors.length);
        data.updateMapData(player, stack);
        data.markDirty();
    }

    private static MapData getMapData(World world, int mapId) {
        return (MapData) world
            .getMapStorage()
            .getOrLoadData(MapData.class, "claim_map_" + mapId);
    }

    private static WorldServer getWorldForDimension(
        EntityPlayer player,
        int dimension
    ) {
        if (player == null || player.getServer() == null) {
            return null;
        }
        return player.getServer().getWorld(dimension);
    }

    private static TerrainSnapshot buildTerrainSnapshot(
        World world,
        int centerBlockX,
        int centerBlockZ,
        int scale
    ) {
        int blocksPerPixel = 1 << scale;
        int halfSize = (MAP_SIZE * blocksPerPixel) / 2;
        int startX = centerBlockX - halfSize;
        int startZ = centerBlockZ - halfSize;
        int total = MAP_SIZE * MAP_SIZE;
        int[] heights = new int[total];
        int[] rgb = new int[total];

        int min = 255;
        int max = 0;
        for (int z = 0; z < MAP_SIZE; z++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                int blockX = startX + (x * blocksPerPixel);
                int blockZ = startZ + (z * blocksPerPixel);
                BlockPos top = world.getTopSolidOrLiquidBlock(
                    new BlockPos(blockX, 0, blockZ)
                );
                int y = top != null ? top.getY() : 0;
                if (y < 0) y = 0;
                if (y > 255) y = 255;

                IBlockState state =
                    top != null
                        ? world.getBlockState(top)
                        : net.minecraft.init.Blocks.AIR.getDefaultState();
                if (state == null) {
                    state = net.minecraft.init.Blocks.AIR.getDefaultState();
                }
                if (
                    state.getBlock() == net.minecraft.init.Blocks.AIR &&
                    y > 0
                ) {
                    BlockPos down = top.down();
                    IBlockState downState = world.getBlockState(down);
                    if (downState != null) {
                        state = downState;
                        y = Math.max(0, down.getY());
                    }
                }

                int idx = (z * MAP_SIZE) + x;
                heights[idx] = y;
                rgb[idx] = baseColorForState(state);
                min = Math.min(min, y);
                max = Math.max(max, y);
            }
        }

        if (min == max) {
            min = Math.max(0, min - 1);
            max = Math.min(255, max + 1);
        }

        byte[] mapColors = new byte[total];
        byte[] rgbBytes = new byte[total * 3];
        for (int i = 0; i < total; i++) {
            int shaded = applyHeightShading(rgb[i], heights[i], min, max);
            mapColors[i] = nearestMapColorByte(shaded);
            rgbBytes[i * 3] = (byte) ((shaded >> 16) & 0xFF);
            rgbBytes[i * 3 + 1] = (byte) ((shaded >> 8) & 0xFF);
            rgbBytes[i * 3 + 2] = (byte) (shaded & 0xFF);
        }

        return new TerrainSnapshot(mapColors, rgbBytes);
    }

    private static Map<ChunkPos, UUID> buildOwnerCache(
        World world,
        int startX,
        int startZ,
        int blocksPerPixel
    ) {
        Map<ChunkPos, UUID> owners = new HashMap<>();
        int totalBlocks = MAP_SIZE * blocksPerPixel;
        int startChunkX = startX >> 4;
        int startChunkZ = startZ >> 4;
        int endChunkX = (startX + totalBlocks) >> 4;
        int endChunkZ = (startZ + totalBlocks) >> 4;
        for (int cz = startChunkZ; cz <= endChunkZ; cz++) {
            for (int cx = startChunkX; cx <= endChunkX; cx++) {
                ChunkPos pos = new ChunkPos(cx, cz);
                TerritoryData.ClaimEntry claim = TerritoryManager.getClaim(
                    world,
                    pos
                );
                owners.put(pos, claim != null ? claim.countryId : null);
            }
        }
        return owners;
    }

    private static boolean isBorderPixel(
        Map<ChunkPos, UUID> ownerCache,
        ChunkPos chunkPos,
        int blockX,
        int blockZ,
        int blocksPerPixel
    ) {
        UUID owner = ownerCache.get(chunkPos);
        if (owner == null) {
            return false;
        }
        int localX = Math.floorMod(blockX, 16);
        int localZ = Math.floorMod(blockZ, 16);

        boolean northBorder = localZ == 0;
        boolean westBorder = localX == 0;
        boolean southBorder = (localZ + blocksPerPixel) >= 16;
        boolean eastBorder = (localX + blocksPerPixel) >= 16;

        if (northBorder) {
            UUID other = ownerCache.get(
                new ChunkPos(chunkPos.x, chunkPos.z - 1)
            );
            if (other == null || !other.equals(owner)) {
                return true;
            }
        }
        if (southBorder) {
            UUID other = ownerCache.get(
                new ChunkPos(chunkPos.x, chunkPos.z + 1)
            );
            if (other == null || !other.equals(owner)) {
                return true;
            }
        }
        if (westBorder) {
            UUID other = ownerCache.get(
                new ChunkPos(chunkPos.x - 1, chunkPos.z)
            );
            if (other == null || !other.equals(owner)) {
                return true;
            }
        }
        if (eastBorder) {
            UUID other = ownerCache.get(
                new ChunkPos(chunkPos.x + 1, chunkPos.z)
            );
            if (other == null || !other.equals(owner)) {
                return true;
            }
        }
        return false;
    }

    private static ChunkRelation relationFor(Country viewer, UUID ownerId) {
        if (viewer == null || ownerId == null) {
            return ChunkRelation.NEUTRAL;
        }
        if (ownerId.equals(viewer.getId())) {
            return ChunkRelation.OWNED;
        }
        if (viewer.isAlliedWith(ownerId)) {
            return ChunkRelation.ALLIED;
        }
        if (viewer.isAtWarWith(ownerId)) {
            return ChunkRelation.HOSTILE;
        }
        return ChunkRelation.NEUTRAL;
    }

    private static int fillColor(ChunkRelation rel) {
        if (rel == null) {
            return 0x333333;
        }
        switch (rel) {
            case OWNED:
                return 0x22CC22;
            case ALLIED:
                return 0x2244DD;
            case HOSTILE:
                return 0xEE9933;
            case NEUTRAL:
            default:
                return 0x333333;
        }
    }

    private static int borderColor(ChunkRelation rel) {
        if (rel == null) {
            return 0x707070;
        }
        switch (rel) {
            case OWNED:
                return 0x1FAF1F;
            case ALLIED:
                return 0x2D5BFF;
            case HOSTILE:
                return 0xFF9A2E;
            case NEUTRAL:
            default:
                return 0x226C22;
        }
    }

    private static int baseColorForState(IBlockState state) {
        if (state == null) {
            return 0x606060;
        }
        Block block = state.getBlock();
        if (
            block == net.minecraft.init.Blocks.GRASS ||
            block == net.minecraft.init.Blocks.GRASS_PATH
        ) {
            return 0x3BAA3B;
        }
        if (
            block == net.minecraft.init.Blocks.SAND ||
            block == net.minecraft.init.Blocks.SANDSTONE
        ) {
            return 0xD9C27A;
        }
        if (
            block == net.minecraft.init.Blocks.STONE ||
            block == net.minecraft.init.Blocks.COBBLESTONE
        ) {
            return 0x8D8D8D;
        }
        if (
            block == net.minecraft.init.Blocks.SNOW ||
            block == net.minecraft.init.Blocks.SNOW_LAYER ||
            block == net.minecraft.init.Blocks.PACKED_ICE ||
            block == net.minecraft.init.Blocks.ICE
        ) {
            return 0xF2F6FF;
        }
        Material mat = state.getMaterial();
        if (mat == Material.WATER) {
            return 0x2E5BFF;
        }

        MapColor map = mat.getMaterialMapColor();
        if (map != null) {
            return map.colorValue & 0xFFFFFF;
        }
        return 0x606060;
    }

    private static int applyHeightShading(
        int rgb,
        int height,
        int min,
        int max
    ) {
        int denom = Math.max(1, max - min);
        float t = (height - min) / (float) denom;
        float mul = 0.75f + (t * 0.5f);

        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        r = clamp255((int) (r * mul));
        g = clamp255((int) (g * mul));
        b = clamp255((int) (b * mul));

        return (r << 16) | (g << 8) | b;
    }

    private static int clamp255(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    private static int blendRgb(
        byte[] terrainRgb,
        int index,
        int overlayRgb,
        float alpha
    ) {
        int baseR = terrainRgb[index * 3] & 0xFF;
        int baseG = terrainRgb[index * 3 + 1] & 0xFF;
        int baseB = terrainRgb[index * 3 + 2] & 0xFF;
        int overR = (overlayRgb >> 16) & 0xFF;
        int overG = (overlayRgb >> 8) & 0xFF;
        int overB = overlayRgb & 0xFF;

        int r = clamp255((int) (baseR * (1 - alpha) + overR * alpha));
        int g = clamp255((int) (baseG * (1 - alpha) + overG * alpha));
        int b = clamp255((int) (baseB * (1 - alpha) + overB * alpha));
        return (r << 16) | (g << 8) | b;
    }

    private static byte mapColorFromRgb(byte[] terrainRgb, int index) {
        int r = terrainRgb[index * 3] & 0xFF;
        int g = terrainRgb[index * 3 + 1] & 0xFF;
        int b = terrainRgb[index * 3 + 2] & 0xFF;
        return nearestMapColorByte((r << 16) | (g << 8) | b);
    }

    private static byte nearestMapColorByte(int rgb) {
        MapColor[] colors = MapColor.COLORS;
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (MapColor color : colors) {
            if (color == null) {
                continue;
            }
            int cr = (color.colorValue >> 16) & 0xFF;
            int cg = (color.colorValue >> 8) & 0xFF;
            int cb = color.colorValue & 0xFF;
            int dr = cr - ((rgb >> 16) & 0xFF);
            int dg = cg - ((rgb >> 8) & 0xFF);
            int db = cb - (rgb & 0xFF);
            int dist = (dr * dr) + (dg * dg) + (db * db);
            if (dist < bestDistance) {
                bestDistance = dist;
                bestIndex = color.colorIndex;
            }
        }
        return (byte) ((bestIndex << 2) | 2);
    }

    private static final class TerrainSnapshot {

        final byte[] mapColors;
        final byte[] rgb;

        TerrainSnapshot(byte[] mapColors, byte[] rgb) {
            this.mapColors = mapColors;
            this.rgb = rgb;
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean hasEffect(ItemStack stack) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(
        ItemStack stack,
        World world,
        java.util.List<String> tooltip,
        net.minecraft.client.util.ITooltipFlag flag
    ) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && isInitialized(tag)) {
            tooltip.add(
                "Center: " +
                tag.getInteger(TAG_CENTER_X) +
                ", " +
                tag.getInteger(TAG_CENTER_Z)
            );
        } else {
            tooltip.add("Uninitialized");
        }
    }
}
