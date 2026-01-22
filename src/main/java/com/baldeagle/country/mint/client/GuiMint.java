package com.baldeagle.country.mint.client;

import com.baldeagle.country.currency.CurrencyDenomination;
import com.baldeagle.country.currency.CurrencyType;
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

    private GuiButton typeButton;
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
        typeButton = addButton(
            new GuiButton(0, x + 10, y + 20, 60, 20, getTypeLabel())
        );
        prevDenom = addButton(new GuiButton(1, x + 10, y + 50, 20, 20, "<"));
        nextDenom = addButton(new GuiButton(2, x + 80, y + 50, 20, 20, ">"));
        minusAmount = addButton(new GuiButton(3, x + 10, y + 80, 20, 20, "-"));
        plusAmount = addButton(new GuiButton(4, x + 80, y + 80, 20, 20, "+"));
        mintButton = addButton(
            new GuiButton(5, x + 110, y + 110, 56, 20, "Mint")
        );
    }

    private String getTypeLabel() {
        CurrencyType type = tile.getSelectedType();
        return type == CurrencyType.COIN ? "Coin" : "Bill";
    }

    private String getDenomLabel() {
        CurrencyDenomination denom = tile.getSelectedDenomination();
        return denom != null ? Integer.toString(denom.getValue()) : "?";
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        if (button == typeButton) {
            NetworkHandler.INSTANCE.sendToServer(
                new MintActionMessage(
                    tile.getPos(),
                    MintActionMessage.Action.TOGGLE_TYPE,
                    0
                )
            );
        } else if (button == prevDenom) {
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
        fontRenderer.drawString("Type: " + getTypeLabel(), 8, 30, 0x404040);
        fontRenderer.drawString(
            "Denomination: " + getDenomLabel(),
            8,
            60,
            0x404040
        );
        fontRenderer.drawString("Amount: " + tile.getAmount(), 8, 90, 0x404040);

        fontRenderer.drawString(
            String.format(
                "Inflation Impact: +%.4f",
                tile.getProjectedInflation()
            ),
            8,
            120,
            0x7F0000
        );
        fontRenderer.drawString(
            "New Circulation: " + tile.getProjectedCirculation(),
            8,
            135,
            0x404040
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
        if (typeButton != null) typeButton.displayString = getTypeLabel();
    }
}
