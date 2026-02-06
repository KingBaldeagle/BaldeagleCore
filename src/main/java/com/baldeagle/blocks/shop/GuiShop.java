package com.baldeagle.blocks.shop;

import com.baldeagle.network.NetworkHandler;
import com.baldeagle.network.message.ShopActionMessage;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class GuiShop extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        "baldeaglecore",
        "textures/gui/shop.png"
    );

    private final ContainerShop container;
    private final TileEntityShop tile;

    private GuiTextField priceField;
    private GuiButton withdrawButton;

    private static final int[] ROW_Y_OFFSETS = new int[] { 0, 2, 4 };

    public GuiShop(ContainerShop container) {
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
        if (container.isOwnerView()) {
            withdrawButton = addButton(
                new GuiButton(1, x + 7, y + 17, 50, 20, "Withdraw")
            );
            priceField = new GuiTextField(
                2,
                fontRenderer,
                x + 8,
                y + 45,
                32,
                14
            );
            priceField.setMaxStringLength(10);
            priceField.setText("0");
            priceField.setFocused(false);

            int buttonStartY = y + 12;
            int buttonSpacing = 22;

            for (int i = 0; i < TileEntityShop.SLOT_COUNT; i++) {
                int row = i / 3;

                addButton(
                    new GuiButton(
                        10 + i,
                        x + 120,
                        buttonStartY + row * buttonSpacing,
                        20,
                        20,
                        "S"
                    )
                );
            }
        } else {
            int buttonStartY = y + 12;
            int buttonSpacing = 22;

            for (int i = 0; i < TileEntityShop.SLOT_COUNT; i++) {
                int row = i / 3;

                addButton(
                    new GuiButton(
                        20 + i,
                        x + 120,
                        buttonStartY + row * buttonSpacing,
                        40,
                        20,
                        "Buy"
                    )
                );
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        if (tile == null) {
            return;
        }

        if (container.isOwnerView()) {
            if (button == withdrawButton) {
                NetworkHandler.INSTANCE.sendToServer(
                    new ShopActionMessage(
                        tile.getPos(),
                        ShopActionMessage.Action.WITHDRAW,
                        0,
                        0
                    )
                );
                return;
            }

            int slot = button.id - 10;
            if (slot >= 0 && slot < TileEntityShop.SLOT_COUNT) {
                long price = parseLong(
                    priceField != null ? priceField.getText() : "0"
                );
                NetworkHandler.INSTANCE.sendToServer(
                    new ShopActionMessage(
                        tile.getPos(),
                        ShopActionMessage.Action.SET_PRICE,
                        slot,
                        price
                    )
                );
            }
            return;
        }

        int slot = button.id - 20;
        if (slot >= 0 && slot < TileEntityShop.SLOT_COUNT) {
            NetworkHandler.INSTANCE.sendToServer(
                new ShopActionMessage(
                    tile.getPos(),
                    ShopActionMessage.Action.BUY,
                    slot,
                    0
                )
            );
        }
    }

    private long parseLong(String text) {
        if (text == null) return 0;
        try {
            return Math.max(0, Long.parseLong(text.trim()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString("Shop", 8, 6, 0x404040);
        if (tile != null) {
            fontRenderer.drawString(
                "Cash: " + tile.getCashStored(),
                8,
                72,
                0x404040
            );
        }

        for (int row = 0; row < 3; row++) {
            // Draw price for the **first slot of the row**
            int slotIndex = row * 3; // first slot in row
            long price = tile != null ? tile.getPrice(slotIndex) : 0;
            if (price > 0) {
                int x = 30; // left side, adjust as needed
                int y = 17 + row * 18 + ROW_Y_OFFSETS[row];

                GlStateManager.pushMatrix();
                GlStateManager.scale(0.5F, 0.5F, 1.0F);
                fontRenderer.drawString(
                    Long.toString(price),
                    x * 2,
                    (y + 18) * 2,
                    0x404040
                );
                GlStateManager.popMatrix();
            }
        }
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

        if (priceField != null) {
            priceField.drawTextBox();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
        throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (priceField != null) {
            priceField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (
            priceField != null && priceField.textboxKeyTyped(typedChar, keyCode)
        ) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (priceField != null) {
            priceField.updateCursorCounter();
        }
    }
}
