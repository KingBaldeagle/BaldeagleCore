package com.baldeagle.blocks.research.client;

import com.baldeagle.blocks.research.ResearchCoreTier;
import com.baldeagle.blocks.research.container.ContainerResearchAssembler;
import com.baldeagle.blocks.research.tile.TileEntityResearchAssembler;
import com.baldeagle.network.NetworkHandler;
import com.baldeagle.network.message.ResearchAssemblerActionMessage;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class GuiResearchAssembler extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "baldeaglecore",
        "textures/gui/research_assembler.png"
    );

    private final TileEntityResearchAssembler tile;

    private GuiButton prevTier;
    private GuiButton nextTier;

    public GuiResearchAssembler(ContainerResearchAssembler container) {
        super(container);
        this.tile = container.getTileEntity();
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;

        buttonList.clear();
        prevTier = addButton(new GuiButton(0, x + 10, y + 20, 20, 20, "<"));
        nextTier = addButton(new GuiButton(1, x + 80, y + 20, 20, 20, ">"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        if (button == prevTier) {
            NetworkHandler.INSTANCE.sendToServer(
                new ResearchAssemblerActionMessage(
                    tile.getPos(),
                    ResearchAssemblerActionMessage.Action.PREV_TIER
                )
            );
        } else if (button == nextTier) {
            NetworkHandler.INSTANCE.sendToServer(
                new ResearchAssemblerActionMessage(
                    tile.getPos(),
                    ResearchAssemblerActionMessage.Action.NEXT_TIER
                )
            );
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString("Research Assembler", 8, 6, 0x404040);

        ResearchCoreTier tier = tile.getSelectedTier();
        long cost = tier != null ? tier.getCost() : 0L;
        long stored = tile.getStoredResearchCredits();
        String tierLabel = tier != null ? tier.getLabel() : "?";

        fontRenderer.drawString(tierLabel, 35, 26, 0x404040);
        if (tier == ResearchCoreTier.T1) {
            fontRenderer.drawString("Cost: " + cost + " RC", 8, 46, 0x404040);
            fontRenderer.drawString(
                "Country: " + stored + " RC",
                8,
                58,
                0x404040
            );
        } else if (tier == ResearchCoreTier.T1_DEPOSIT) {
            fontRenderer.drawString("Deposit", 8, 46, 0x404040);
            fontRenderer.drawString(
                "Country: " + stored + " RC",
                8,
                58,
                0x404040
            );
        } else {
            fontRenderer.drawString("Cost: 9x lower cores", 8, 46, 0x404040);
        }

        String owner = tile.getOwnerCountryName();
        if (owner == null || owner.trim().isEmpty()) {
            owner = "-";
        }

        String country = tile.getLastCountryName();
        if (country == null || country.trim().isEmpty()) {
            country = "-";
        }

        if (
            tier == ResearchCoreTier.T1 || tier == ResearchCoreTier.T1_DEPOSIT
        ) {
            fontRenderer.drawString(
                String.format(
                    "Modifier: %.3f",
                    tile.getLastExchangeRate() * tile.getLastInflationModifier()
                ),
                8,
                68,
                0x404040
            );
        }

        String status = tile.getLastStatus();
        if (status != null && !status.trim().isEmpty()) {
            fontRenderer.drawString(status, 81, 51, 0x7F0000);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(
        float partialTicks,
        int mouseX,
        int mouseY
    ) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(TEXTURE);
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        drawTexturedModalRect(x, y, 0, 0, xSize, ySize);

        ResearchCoreTier tier = tile.getSelectedTier();
        long cost = tier != null ? tier.getCost() : 0L;
        long stored = tile.getStoredResearchCredits();
        int barW = 6;
        int barH = 48;
        int barX = x + xSize - 12;
        int barY = y + 30;
        drawRect(barX, barY, barX + barW, barY + barH, 0xFF2A2A2A);
        if (tier == ResearchCoreTier.T1 && cost > 0) {
            float pct = Math.min(1.0F, stored / (float) cost);
            int filled = (int) (barH * pct);
            drawRect(
                barX,
                barY + barH - filled,
                barX + barW,
                barY + barH,
                0xFF2D9C3B
            );
        }
    }
}
