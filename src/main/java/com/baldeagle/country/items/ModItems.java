package com.baldeagle.country.items;

import com.baldeagle.country.creativetab.EconomyTab;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.RegistryEvent;

@Mod.EventBusSubscriber(modid = "baldeaglecore")
public class ModItems {

    public static final Item COIN_1 = new ItemCoin(1)
            .setRegistryName("baldeaglecore", "coin_1")
            .setTranslationKey("coin_1")
            .setCreativeTab(EconomyTab.INSTANCE);

    public static final Item COIN_5 = new ItemCoin(5)
            .setRegistryName("baldeaglecore", "coin_5")
            .setTranslationKey("coin_5")
            .setCreativeTab(EconomyTab.INSTANCE);

    public static final Item BILL_10 = new ItemBill(10)
            .setRegistryName("baldeaglecore", "bill_10")
            .setTranslationKey("bill_10")
            .setCreativeTab(EconomyTab.INSTANCE);



    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                COIN_1,
                COIN_5,
                BILL_10
        );
    }
}
