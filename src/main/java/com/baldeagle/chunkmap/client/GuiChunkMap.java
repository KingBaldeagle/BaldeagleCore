package com.baldeagle.chunkmap.client;

import com.baldeagle.chunkmap.ChunkMapConstants;
import com.baldeagle.chunkmap.ChunkTerrainSnapshot;
import com.baldeagle.chunkmap.ClientChunkMapCache;
import com.baldeagle.network.NetworkHandler;
import com.baldeagle.network.message.ChunkMapRequestMessage;
import java.io.IOException;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.Blocks;

public class GuiChunkMap extends GuiScreen {

    private static final int RADIUS = ChunkMapConstants.DEFAULT_RADIUS;
    private static final int GRID = ChunkMapConstants.DEFAULT_GRID;

    private int lastCenterX = Integer.MIN_VALUE;
    private int lastCenterZ = Integer.MIN_VALUE;

    @Override
    public void initGui() {
        super.initGui();
        requestIfNeeded(true);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        requestIfNeeded(false);
    }

    private void requestIfNeeded(boolean force) {
        if (mc == null || mc.player == null) {
            return;
        }
        int cx = mc.player.chunkCoordX;
        int cz = mc.player.chunkCoordZ;
        if (!force && cx == lastCenterX && cz == lastCenterZ) {
            return;
        }
        lastCenterX = cx;
        lastCenterZ = cz;
        NetworkHandler.INSTANCE.sendToServer(
            new ChunkMapRequestMessage(cx, cz, RADIUS, GRID)
        );
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) {
            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }

        int dim = mc.world.provider.getDimension();
        int centerX = mc.player.chunkCoordX;
        int centerZ = mc.player.chunkCoordZ;

        int chunksWide = (RADIUS * 2) + 1;

        int tileSize = 8; // pixels per chunk tile
        int mapW = chunksWide * tileSize;
        int mapH = chunksWide * tileSize;
        int x0 = (width - mapW) / 2;
        int y0 = (height - mapH) / 2;

        HeightRange range = computeHeightRange(dim, centerX, centerZ);

        for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                int cx = centerX + dx;
                int cz = centerZ + dz;
                int x = x0 + (dx + RADIUS) * tileSize;
                int y = y0 + (dz + RADIUS) * tileSize;

                ChunkTerrainSnapshot snap = ClientChunkMapCache.get(dim, cx, cz);
                if (snap == null || snap.grid != GRID) {
                    drawRect(x, y, x + tileSize, y + tileSize, 0xFF202020);
                    continue;
                }

                int cell = tileSize / GRID;
                int idx = 0;
                for (int gx = 0; gx < GRID; gx++) {
                    for (int gz = 0; gz < GRID; gz++) {
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
        }

        // Player marker (center chunk).
        int px = x0 + RADIUS * tileSize + (tileSize / 2) - 1;
        int py = y0 + RADIUS * tileSize + (tileSize / 2) - 1;
        drawRect(px, py, px + 3, py + 3, 0xFFFF3333);

        drawCenteredString(fontRenderer, "Chunk Map", width / 2, y0 - 12, 0xFFFFFF);
        drawCenteredString(
            fontRenderer,
            "Center: " + centerX + ", " + centerZ,
            width / 2,
            y0 + mapH + 4,
            0xB0B0B0
        );

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
                if (snap == null || snap.grid != GRID) {
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
            return new HeightRange(Math.max(0, min - 1), Math.min(255, max + 1));
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

        if (state.getBlock() == Blocks.GRASS || state.getBlock() == Blocks.GRASS_PATH) {
            return 0x3BAA3B;
        }
        if (state.getBlock() == Blocks.SAND || state.getBlock() == Blocks.SANDSTONE) {
            return 0xD9C27A;
        }
        if (state.getBlock() == Blocks.STONE || state.getBlock() == Blocks.COBBLESTONE) {
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

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        // Let ESC close as normal.
    }
}
