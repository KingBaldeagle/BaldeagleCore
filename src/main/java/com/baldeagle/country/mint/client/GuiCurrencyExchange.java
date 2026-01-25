package com.baldeagle.country.mint.client;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryStorage;
import com.baldeagle.country.mint.container.ContainerCurrencyExchange;
import com.baldeagle.country.mint.tile.TileEntityCurrencyExchange;
import com.baldeagle.network.NetworkHandler;
import com.baldeagle.network.message.ExchangeActionMessage;
import java.io.IOException;
import java.util.UUID;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class GuiCurrencyExchange extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "baldeaglecore",
        "textures/gui/exchange.png"
    );

    private final ContainerCurrencyExchange container;
    private final TileEntityCurrencyExchange tile;

    private GuiButton cycleTargetLeft;
    private GuiButton cycleTargetRight;
    private GuiButton exchangeButton;

    public GuiCurrencyExchange(ContainerCurrencyExchange container) {
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
        cycleTargetLeft = addButton(
            new GuiButton(0, x + 10, y + 20, 20, 20, "<")
        );
        cycleTargetRight = addButton(
            new GuiButton(1, x + 146, y + 20, 20, 20, ">")
        );
        exchangeButton = addButton(
            new GuiButton(2, x + 110, y + 50, 56, 20, "Exchange")
        );
    }

    private String getTargetCountryName() {
        String name = tile.getTargetCountryName();
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }

        UUID id = tile.getTargetCountryId();
        if (id == null || tile.getWorld() == null) {
            return "None";
        }
        Country country = CountryStorage.get(tile.getWorld())
            .getCountriesMap()
            .get(id);
        return country != null ? country.getName() : "None";
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        if (button == cycleTargetLeft) {
            NetworkHandler.INSTANCE.sendToServer(
                new ExchangeActionMessage(
                    tile.getPos(),
                    ExchangeActionMessage.Action.PREV_COUNTRY
                )
            );
        } else if (button == cycleTargetRight) {
            NetworkHandler.INSTANCE.sendToServer(
                new ExchangeActionMessage(
                    tile.getPos(),
                    ExchangeActionMessage.Action.NEXT_COUNTRY
                )
            );
        } else if (button == exchangeButton) {
            NetworkHandler.INSTANCE.sendToServer(
                new ExchangeActionMessage(
                    tile.getPos(),
                    ExchangeActionMessage.Action.EXECUTE
                )
            );
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString("Currency Exchange", 8, 6, 0x404040);
        fontRenderer.drawString(
            "Target: " + getTargetCountryName(),
            40,
            26,
            0x404040
        );

        fontRenderer.drawString(
            String.format("Rate: %.4f", tile.getProjectedRate()),
            8,
            46,
            0x404040
        );
        fontRenderer.drawString(
            "Proj. Value: " + tile.getProjectedOutput(),
            8,
            60,
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
    }
}
