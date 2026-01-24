package com.baldeagle.country.mint.client;

import com.baldeagle.country.currency.CurrencyDenomination;
import com.baldeagle.country.mint.container.ContainerMint;
import com.baldeagle.country.mint.tile.TileEntityMint;
import com.baldeagle.network.NetworkHandler;
import com.baldeagle.network.message.MintActionMessage;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class GuiMint extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "baldeaglecore",
        "textures/gui/mint.png"
    );

    private final ContainerMint container;
    private final TileEntityMint tile;

    private GuiButton prevDenom;
    private GuiButton nextDenom;
    private GuiButton minusAmount;
    private GuiButton plusAmount;
    private GuiButton mintButton;

    public GuiMint(ContainerMint container) {
        super(container);
        this.container = container;
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
        prevDenom = addButton(new GuiButton(0, x + 10, y + 30, 20, 20, "<"));
        nextDenom = addButton(new GuiButton(1, x + 80, y + 30, 20, 20, ">"));
        minusAmount = addButton(new GuiButton(2, x + 10, y + 60, 20, 20, "-"));
        plusAmount = addButton(new GuiButton(3, x + 80, y + 60, 20, 20, "+"));
        mintButton = addButton(
            new GuiButton(4, x + 110, y + 60, 56, 20, "Mint")
        );
    }

    private String getDenomLabel() {
        CurrencyDenomination denom = tile.getSelectedDenomination();
        return denom != null ? Integer.toString(denom.getValue()) : "?";
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        if (button == prevDenom) {
            NetworkHandler.INSTANCE.sendToServer(
                new MintActionMessage(
                    tile.getPos(),
                    MintActionMessage.Action.PREV_DENOMINATION,
                    0
                )
            );
        } else if (button == nextDenom) {
            NetworkHandler.INSTANCE.sendToServer(
                new MintActionMessage(
                    tile.getPos(),
                    MintActionMessage.Action.NEXT_DENOMINATION,
                    0
                )
            );
        } else if (button == minusAmount) {
            NetworkHandler.INSTANCE.sendToServer(
                new MintActionMessage(
                    tile.getPos(),
                    MintActionMessage.Action.DECREASE_AMOUNT,
                    0
                )
            );
        } else if (button == plusAmount) {
            NetworkHandler.INSTANCE.sendToServer(
                new MintActionMessage(
                    tile.getPos(),
                    MintActionMessage.Action.INCREASE_AMOUNT,
                    0
                )
            );
        } else if (button == mintButton) {
            NetworkHandler.INSTANCE.sendToServer(
                new MintActionMessage(
                    tile.getPos(),
                    MintActionMessage.Action.EXECUTE,
                    0
                )
            );
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString("National Mint", 8, 6, 0x404040);
        fontRenderer.drawString("D " + getDenomLabel(), 45, 35, 0x404040);
        fontRenderer.drawString("A " + tile.getAmount(), 45, 65, 0x404040);

        fontRenderer.drawString(
            String.format(
                "Inflation Impact: +%.4f",
                tile.getProjectedInflation()
            ),
            8,
            20,
            0x7F0000
        );
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
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
    }
}
