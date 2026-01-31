package com.baldeagle.chunkmap.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class InventoryMapButtonHandler {

    private static final int BUTTON_ID = 987650;

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof GuiInventory)) {
            return;
        }
        GuiInventory gui = (GuiInventory) event.getGui();

        int x = gui.getGuiLeft() - 22;
        int y = gui.getGuiTop() + 6;

        // Add an item-icon button similar to "FTB Teams" style entry points.
        event.getButtonList().add(new TerritoryIconButton(BUTTON_ID, x, y));
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!(event.getGui() instanceof GuiInventory)) {
            return;
        }
        GuiButton button = event.getButton();
        if (button == null || button.id != BUTTON_ID) {
            return;
        }

        Minecraft.getMinecraft().displayGuiScreen(new GuiChunkMap());
        event.setCanceled(true);
    }

    private static final class TerritoryIconButton extends GuiButton {

        private final ItemStack icon = new ItemStack(Items.FILLED_MAP);

        TerritoryIconButton(int buttonId, int x, int y) {
            super(buttonId, x, y, 20, 20, "");
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!this.visible) {
                return;
            }
            this.hovered =
                mouseX >= this.x &&
                mouseY >= this.y &&
                mouseX < this.x + this.width &&
                mouseY < this.y + this.height;

            int bg = hovered ? 0x90FFFFFF : 0x60FFFFFF;
            drawRect(this.x, this.y, this.x + this.width, this.y + this.height, bg);

            mc.getRenderItem().renderItemAndEffectIntoGUI(icon, this.x + 2, this.y + 2);
        }
    }
}
