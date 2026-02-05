package com.baldeagle.chunkmap.client;

import com.baldeagle.bank.ModBlocks;
import com.baldeagle.country.items.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class InventoryMapButtonHandler {

    private static final int BUTTON_ID = 987650;

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (
            !(event.getGui() instanceof GuiInventory) &&
            !(event.getGui() instanceof GuiContainerCreative)
        ) {
            return;
        }

        int left = 0;
        int top = 0;
        if (event.getGui() instanceof GuiInventory) {
            GuiInventory gui = (GuiInventory) event.getGui();
            left = gui.getGuiLeft();
            top = gui.getGuiTop();
        } else if (event.getGui() instanceof GuiContainer) {
            GuiContainer gui = (GuiContainer) event.getGui();
            left = readGuiLeft(gui);
            top = readGuiTop(gui);
        }

        int x = left - 22;
        int y = top + 6;

        // Creative inventory rebuilds buttons; prevent duplicates.
        event.getButtonList().removeIf(b -> b != null && b.id == BUTTON_ID);
        event.getButtonList().add(new TerritoryIconButton(BUTTON_ID, x, y));
    }

    @SubscribeEvent
    public void onActionPerformed(
        GuiScreenEvent.ActionPerformedEvent.Pre event
    ) {
        if (
            !(event.getGui() instanceof GuiInventory) &&
            !(event.getGui() instanceof GuiContainerCreative)
        ) {
            return;
        }
        GuiButton button = event.getButton();
        if (button == null || button.id != BUTTON_ID) {
            return;
        }

        Minecraft.getMinecraft().displayGuiScreen(new GuiChunkMap());
        event.setCanceled(true);
    }

    private int readGuiLeft(GuiContainer gui) {
        try {
            return ReflectionHelper.findField(
                GuiContainer.class,
                "guiLeft",
                "field_147003_i"
            ).getInt(gui);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int readGuiTop(GuiContainer gui) {
        try {
            return ReflectionHelper.findField(
                GuiContainer.class,
                "guiTop",
                "field_147009_r"
            ).getInt(gui);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static final class TerritoryIconButton extends GuiButton {

        private final ItemStack icon;

        TerritoryIconButton(int buttonId, int x, int y) {
            super(buttonId, x, y, 20, 20, "");
            this.icon =
                ModItems.COIN_1 != null
                    ? new ItemStack(ModItems.COIN_1)
                    : ItemStack.EMPTY;
        }

        @Override
        public void drawButton(
            Minecraft mc,
            int mouseX,
            int mouseY,
            float partialTicks
        ) {
            if (!this.visible) {
                return;
            }
            this.hovered =
                mouseX >= this.x &&
                mouseY >= this.y &&
                mouseX < this.x + this.width &&
                mouseY < this.y + this.height;

            int bg = hovered ? 0x90FFFFFF : 0x60FFFFFF;
            drawRect(
                this.x,
                this.y,
                this.x + this.width,
                this.y + this.height,
                bg
            );

            if (!icon.isEmpty()) {
                mc
                    .getRenderItem()
                    .renderItemAndEffectIntoGUI(icon, this.x + 2, this.y + 2);
            }
        }
    }
}
