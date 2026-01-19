package com.baldeagle.client;

import com.baldeagle.country.items.ModItems;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "baldeaglecore", value = net.minecraftforge.fml.relauncher.Side.CLIENT)
public class ClientModelRegistry {

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerItem(ModItems.COIN_1);
        registerItem(ModItems.COIN_5);
        registerItem(ModItems.BILL_10);
    }

    private static void registerItem(net.minecraft.item.Item item) {
        ModelLoader.setCustomModelResourceLocation(
                item,
                0,
                new ModelResourceLocation(item.getRegistryName(), "inventory")
        );
    }
}
