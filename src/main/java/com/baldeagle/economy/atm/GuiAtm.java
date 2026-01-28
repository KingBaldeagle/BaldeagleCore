package com.baldeagle.economy.atm;

import com.baldeagle.network.NetworkHandler;
import com.baldeagle.network.message.AtmWithdrawMessage;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class GuiAtm extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "baldeaglecore",
        "textures/gui/atm.png"
    );

    private final ContainerAtm container;
    private final TileEntityAtm tile;

    private GuiTextField amountField;
    private GuiButton sourceButton;
    private GuiButton withdrawButton;
    private AtmWithdrawMessage.Source source = AtmWithdrawMessage.Source.PLAYER;

    public GuiAtm(ContainerAtm container) {
        super(container);
        this.container = container;
        this.tile = container.getTile();
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;

        buttonList.clear();
        sourceButton = addButton(
            new GuiButton(0, x + 10, y + 20, 70, 20, getSourceLabel())
        );
        withdrawButton = addButton(
            new GuiButton(1, x + 96, y + 20, 70, 20, "Withdraw")
        );

        amountField = new GuiTextField(
            2,
            fontRenderer,
            x + 10,
            y + 45,
            156,
            18
        );
        amountField.setMaxStringLength(18);
        amountField.setText("0");
        amountField.setFocused(false);
    }

    private String getSourceLabel() {
        if (source == AtmWithdrawMessage.Source.PLAYER) {
            return "Personal";
        }
        return getCountryLabel();
    }

    private String getCountryLabel() {
        String name = tile != null ? tile.getCountryName() : null;
        return name != null && !name.trim().isEmpty() ? name : "Country";
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        if (button == sourceButton) {
            source =
                source == AtmWithdrawMessage.Source.PLAYER
                    ? AtmWithdrawMessage.Source.COUNTRY
                    : AtmWithdrawMessage.Source.PLAYER;
            sourceButton.displayString = getSourceLabel();
        } else if (button == withdrawButton) {
            long amount = parseAmount(amountField.getText());
            if (amount <= 0) {
                return;
            }
            NetworkHandler.INSTANCE.sendToServer(
                new AtmWithdrawMessage(tile.getPos(), source, amount)
            );
        }
    }

    private long parseAmount(String text) {
        if (text == null) return 0;
        try {
            long v = Long.parseLong(text.trim());
            return Math.max(0, v);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString("ATM", 8, 6, 0x404040);
        fontRenderer.drawString(
            "You: " + container.getDisplayedPlayerBalance(),
            8,
            70,
            0x404040
        );
        fontRenderer.drawString(
            getCountryLabel() + ": " + container.getDisplayedCountryBalance(),
            92,
            70,
            0x404040
        );
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
        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(TEXTURE);
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        drawTexturedModalRect(x, y, 0, 0, xSize, ySize);

        amountField.drawTextBox();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
        throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        amountField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (amountField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        amountField.updateCursorCounter();
    }
}
