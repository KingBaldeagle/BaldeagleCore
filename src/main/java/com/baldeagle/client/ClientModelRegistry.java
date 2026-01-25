package com.baldeagle.client;

import com.baldeagle.bank.ModBlocks;
import com.baldeagle.country.items.ModItems;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(
    modid = "baldeaglecore",
    value = net.minecraftforge.fml.relauncher.Side.CLIENT
)
public class ClientModelRegistry {

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerItem(ModItems.COIN_1);
        registerItem(ModItems.COIN_5);
        registerItem(ModItems.COIN_10);
        registerItem(ModItems.BILL_50);
        registerItem(ModItems.BILL_100);
        registerItem(Item.getItemFromBlock(ModBlocks.BANK));
        registerItem(Item.getItemFromBlock(ModBlocks.MINT));
        registerItem(Item.getItemFromBlock(ModBlocks.CURRENCY_EXCHANGE));
        registerItem(Item.getItemFromBlock(ModBlocks.VAULT));
        registerItem(Item.getItemFromBlock(ModBlocks.ATM));
        registerItem(Item.getItemFromBlock(ModBlocks.SHOP));
    }

    private static void registerItem(Item item) {
        ModelLoader.setCustomModelResourceLocation(
            item,
            0,
            new ModelResourceLocation(item.getRegistryName(), "inventory")
        );
    }
}
