package com.baldeagle.bank;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class GuiBank extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "baldeaglecore",
        "textures/gui/bank.png"
    );
    private final ContainerBank container;

    public GuiBank(ContainerBank container) {
        super(container);
        this.container = container;
        this.xSize = 203;
        this.ySize = 166;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.75F, 0.75F, 1.0F);
        fontRenderer.drawString("Bank", 10, 6, 0x404040);
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        GlStateManager.scale(0.50F, 0.50F, 1.0F);

        fontRenderer.drawString(
            "Player Balance: " + container.getDisplayedPlayerBalance(),
            60,
            50,
            0x404040
        );

        fontRenderer.drawString(
            "Country Balance: " + container.getDisplayedCountryBalance(),
            200,
            50,
            0x404040
        );
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        GlStateManager.scale(0.75F, 0.75F, 1.0F);
        fontRenderer.drawString("Personal Deposit", 25, 74, 0x404040); // moved 10 px right
        fontRenderer.drawString("Country Deposit", 125, 74, 0x404040); // moved 16 px right
        GlStateManager.popMatrix();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(
        float partialTicks,
        int mouseX,
        int mouseY
    ) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }
}
