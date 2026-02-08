package com.baldeagle.chunkmap.client;

import com.baldeagle.chunkmap.ChunkMapConstants;
import com.baldeagle.chunkmap.ChunkOwnershipInfo;
import com.baldeagle.chunkmap.ChunkRelation;
import com.baldeagle.chunkmap.ChunkTerrainSnapshot;
import com.baldeagle.chunkmap.ClientChunkMapCache;
import com.baldeagle.chunkmap.ClientChunkOwnershipCache;
import com.baldeagle.items.ItemClaimMap;
import com.baldeagle.network.NetworkHandler;
import com.baldeagle.network.message.ClaimMapRequestMessage;
import com.baldeagle.network.message.ClaimOwnershipRequestMessage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class GuiClaimMap extends GuiScreen {

    private static final int RADIUS = ChunkMapConstants.DEFAULT_RADIUS;

    private final ItemStack mapStack;
    private final int centerX;
    private final int centerZ;
    private final int mapDimension;
    private final int grid;

    private long lastOwnershipRefreshMs = 0L;

    public GuiClaimMap(ItemStack mapStack) {
        this.mapStack = mapStack;

        NBTTagCompound tag = mapStack != null ? mapStack.getTagCompound() : null;
        if (ItemClaimMap.isInitialized(tag)) {
            this.centerX = tag.getInteger(ItemClaimMap.TAG_CENTER_X);
            this.centerZ = tag.getInteger(ItemClaimMap.TAG_CENTER_Z);
            this.mapDimension = tag.getInteger(ItemClaimMap.TAG_DIMENSION);
            this.grid = clampGrid(tag.getInteger(ItemClaimMap.TAG_ZOOM));
        } else {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.player != null) {
                this.centerX = mc.player.chunkCoordX;
                this.centerZ = mc.player.chunkCoordZ;
                this.mapDimension = mc.player.dimension;
            } else {
                this.centerX = 0;
                this.centerZ = 0;
                this.mapDimension = 0;
            }
            this.grid = ChunkMapConstants.DEFAULT_GRID;
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        requestTerrainIfNeeded();
        requestOwnership(true);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        requestOwnership(false);
    }

    private int clampGrid(int grid) {
        if (grid <= 0) {
            return ChunkMapConstants.DEFAULT_GRID;
        }
        return Math.min(ChunkMapConstants.MAX_GRID, grid);
    }

    private void requestTerrainIfNeeded() {
        if (!hasTerrainCache(mapDimension, centerX, centerZ, grid)) {
            NetworkHandler.INSTANCE.sendToServer(
                new ClaimMapRequestMessage(
                    mapDimension,
                    centerX,
                    centerZ,
                    RADIUS,
                    grid
                )
            );
        }
    }

    private void requestOwnership(boolean force) {
        long nowMs = System.currentTimeMillis();
        boolean timedRefresh = (nowMs - lastOwnershipRefreshMs) > 2000L;
        if (!force && !timedRefresh) {
            return;
        }
        lastOwnershipRefreshMs = nowMs;
        NetworkHandler.INSTANCE.sendToServer(
            new ClaimOwnershipRequestMessage(
                mapDimension,
                centerX,
                centerZ,
                RADIUS
            )
        );
    }

    private boolean hasTerrainCache(
        int dim,
        int centerX,
        int centerZ,
        int grid
    ) {
        for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                ChunkTerrainSnapshot snap = ClientChunkMapCache.get(
                    dim,
                    centerX + dx,
                    centerZ + dz
                );
                if (snap == null || snap.grid != grid) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int chunksWide = (RADIUS * 2) + 1;

        int tileSize = 8; // pixels per chunk tile
        int mapW = chunksWide * tileSize;
        int mapH = chunksWide * tileSize;
        int x0 = (width - mapW) / 2;
        int y0 = (height - mapH) / 2;

        HeightRange range = computeHeightRange(
            mapDimension,
            centerX,
            centerZ
        );

        for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                int cx = centerX + dx;
                int cz = centerZ + dz;
                int x = x0 + (dx + RADIUS) * tileSize;
                int y = y0 + (dz + RADIUS) * tileSize;

                ChunkTerrainSnapshot snap = ClientChunkMapCache.get(
                    mapDimension,
                    cx,
                    cz
                );
                if (snap == null || snap.grid != grid) {
                    drawRect(x, y, x + tileSize, y + tileSize, 0xFF202020);
                } else {
                    int cell = tileSize / grid;
                    int idx = 0;
                    for (int gx = 0; gx < grid; gx++) {
                        for (int gz = 0; gz < grid; gz++) {
                            int h = snap.heights[idx] & 0xFF;
                            int rgb = baseColorForStateId(snap.stateIds[idx]);
                            rgb = applyHeightShading(rgb, h, range);

                            int rx = x + (gx * cell);
                            int ry = y + (gz * cell);
                            drawRect(
                                rx,
                                ry,
                                rx + cell,
                                ry + cell,
                                0xFF000000 | (rgb & 0xFFFFFF)
                            );
                            idx++;
                        }
                    }
                }

                // Ownership overlay + borders.
                ChunkOwnershipInfo info = ClientChunkOwnershipCache.get(
                    mapDimension,
                    cx,
                    cz
                );
                ChunkRelation rel =
                    info != null ? info.relation : ChunkRelation.NEUTRAL;
                int fill = 0;
                if (info != null && info.ownerCountryId != null) {
                    fill = fillColor(rel);
                    // Non-allied, non-hostile foreign claims render as neutral (gray) instead.
                    if (fill == 0) {
                        fill = 0x55333333;
                    }
                }
                if (fill != 0) {
                    drawRect(x, y, x + tileSize, y + tileSize, fill);
                }

                // Draw borders only at the outer edge of a contiguous region owned by the
                // same country (prevents "gridline" look inside a country).
                if (info != null && info.ownerCountryId != null) {
                    int border = borderColor(rel);

                    boolean northSame = sameOwner(
                        info,
                        ClientChunkOwnershipCache.get(mapDimension, cx, cz - 1)
                    );
                    boolean southSame = sameOwner(
                        info,
                        ClientChunkOwnershipCache.get(mapDimension, cx, cz + 1)
                    );
                    boolean westSame = sameOwner(
                        info,
                        ClientChunkOwnershipCache.get(mapDimension, cx - 1, cz)
                    );
                    boolean eastSame = sameOwner(
                        info,
                        ClientChunkOwnershipCache.get(mapDimension, cx + 1, cz)
                    );

                    if (!northSame) {
                        drawRect(x, y, x + tileSize, y + 1, border);
                    }
                    if (!southSame) {
                        drawRect(
                            x,
                            y + tileSize - 1,
                            x + tileSize,
                            y + tileSize,
                            border
                        );
                    }
                    if (!westSame) {
                        drawRect(x, y, x + 1, y + tileSize, border);
                    }
                    if (!eastSame) {
                        drawRect(
                            x + tileSize - 1,
                            y,
                            x + tileSize,
                            y + tileSize,
                            border
                        );
                    }
                }
            }
        }

        // Center marker (map center chunk).
        int px = x0 + RADIUS * tileSize + (tileSize / 2) - 1;
        int py = y0 + RADIUS * tileSize + (tileSize / 2) - 1;
        drawRect(px, py, px + 3, py + 3, 0xFFFF3333);

        // Hover highlight (no gridlines; outline only the hovered chunk).
        if (
            mouseX >= x0 &&
            mouseX < (x0 + mapW) &&
            mouseY >= y0 &&
            mouseY < (y0 + mapH)
        ) {
            int hx = x0 + ((mouseX - x0) / tileSize) * tileSize;
            int hy = y0 + ((mouseY - y0) / tileSize) * tileSize;
            drawOutline(hx, hy, tileSize, tileSize, 0xFFFFFFFF);
        }

        drawCenteredString(
            fontRenderer,
            "Claim Map",
            width / 2,
            y0 - 12,
            0xFFFFFF
        );
        drawCenteredString(
            fontRenderer,
            "Center: " + centerX + ", " + centerZ,
            width / 2,
            y0 + mapH + 4,
            0xB0B0B0
        );

        // Tooltip for hovered chunk.
        if (
            mouseX >= x0 &&
            mouseX < (x0 + mapW) &&
            mouseY >= y0 &&
            mouseY < (y0 + mapH)
        ) {
            int dx = (mouseX - x0) / tileSize - RADIUS;
            int dz = (mouseY - y0) / tileSize - RADIUS;
            int hx = centerX + dx;
            int hz = centerZ + dz;
            ChunkOwnershipInfo info = ClientChunkOwnershipCache.get(
                mapDimension,
                hx,
                hz
            );
            List<String> lines = new ArrayList<>();
            lines.add("Chunk: (" + hx + ", " + hz + ")");
            if (info == null || info.ownerCountryId == null) {
                lines.add("Owner: Unclaimed");
                lines.add("Relation: Neutral");
            } else {
                lines.add(
                    "Owner: " +
                        (info.ownerName.isEmpty() ? "Unknown" : info.ownerName)
                );
                lines.add("Relation: " + relationLabel(info.relation));
                if (info.incomePerDay > 0) {
                    lines.add("Income: +" + info.incomePerDay + " per day");
                }
            }
            drawHoveringText(lines, mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private HeightRange computeHeightRange(int dim, int centerX, int centerZ) {
        int min = 255;
        int max = 0;
        boolean any = false;
        for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                ChunkTerrainSnapshot snap = ClientChunkMapCache.get(
                    dim,
                    centerX + dx,
                    centerZ + dz
                );
                if (snap == null || snap.grid != grid) {
                    continue;
                }
                for (byte b : snap.heights) {
                    int h = b & 0xFF;
                    min = Math.min(min, h);
                    max = Math.max(max, h);
                    any = true;
                }
            }
        }
        if (!any) {
            return new HeightRange(0, 255);
        }
        if (min == max) {
            // Prevent division by zero in shading.
            return new HeightRange(
                Math.max(0, min - 1),
                Math.min(255, max + 1)
            );
        }
        return new HeightRange(min, max);
    }

    private static final class HeightRange {

        final int min;
        final int max;

        HeightRange(int min, int max) {
            this.min = min;
            this.max = max;
        }
    }

    private int baseColorForStateId(int stateId) {
        IBlockState state = Block.getStateById(stateId);
        if (state == null) {
            return 0x606060;
        }

        if (
            state.getBlock() == Blocks.GRASS ||
            state.getBlock() == Blocks.GRASS_PATH
        ) {
            return 0x3BAA3B;
        }
        if (
            state.getBlock() == Blocks.SAND ||
            state.getBlock() == Blocks.SANDSTONE
        ) {
            return 0xD9C27A;
        }
        if (
            state.getBlock() == Blocks.STONE ||
            state.getBlock() == Blocks.COBBLESTONE
        ) {
            return 0x8D8D8D;
        }
        if (
            state.getBlock() == Blocks.SNOW ||
            state.getBlock() == Blocks.SNOW_LAYER ||
            state.getBlock() == Blocks.PACKED_ICE ||
            state.getBlock() == Blocks.ICE
        ) {
            return 0xF2F6FF;
        }
        Material mat = state.getMaterial();
        if (mat == Material.WATER) {
            return 0x2E5BFF;
        }

        // Fallback: use map color if available.
        MapColor map = mat.getMaterialMapColor();
        if (map != null) {
            return map.colorValue & 0xFFFFFF;
        }
        return 0x606060;
    }

    private int applyHeightShading(int rgb, int height, HeightRange range) {
        if (range == null) {
            return rgb;
        }
        int denom = Math.max(1, range.max - range.min);
        float t = (height - range.min) / (float) denom; // 0..1
        float mul = 0.75f + (t * 0.5f); // 0.75..1.25

        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        r = clamp255((int) (r * mul));
        g = clamp255((int) (g * mul));
        b = clamp255((int) (b * mul));

        return (r << 16) | (g << 8) | b;
    }

    private int clamp255(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    private int fillColor(ChunkRelation rel) {
        if (rel == null) {
            return 0;
        }
        switch (rel) {
            case OWNED:
                return 0x5522CC22; // translucent green
            case ALLIED:
                return 0x552244DD; // translucent blue
            case HOSTILE:
                return 0x55EE9933; // translucent orange
            case NEUTRAL:
            default:
                return 0;
        }
    }

    private int borderColor(ChunkRelation rel) {
        if (rel == null) {
            return 0xFF707070;
        }
        switch (rel) {
            case OWNED:
                return 0xFF1FAF1F;
            case ALLIED:
                return 0xFF2D5BFF;
            case HOSTILE:
                return 0xFFFF9A2E;
            case NEUTRAL:
            default:
                // Requested: neutral (non-allied) country borders should be green.
                return 0xFF226C22;
        }
    }

    private boolean sameOwner(ChunkOwnershipInfo a, ChunkOwnershipInfo b) {
        if (a == null || a.ownerCountryId == null) {
            return false;
        }
        return b != null && a.ownerCountryId.equals(b.ownerCountryId);
    }

    private String relationLabel(ChunkRelation rel) {
        if (rel == null) {
            return "Neutral";
        }
        switch (rel) {
            case OWNED:
                return "Own";
            case ALLIED:
                return "Ally";
            case HOSTILE:
                return "Hostile";
            case NEUTRAL:
            default:
                return "Neutral";
        }
    }

    private void drawOutline(int x, int y, int w, int h, int color) {
        drawRect(x, y, x + w, y + 1, color);
        drawRect(x, y + h - 1, x + w, y + h, color);
        drawRect(x, y, x + 1, y + h, color);
        drawRect(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        // Let ESC close as normal.
    }

    public boolean doesGuiPauseGame() {
        return false;
    }
}
