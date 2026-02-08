package com.baldeagle.items;

import com.baldeagle.country.currency.CurrencyDenomination;
import com.baldeagle.creativetab.BaldeagleCoreTab;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "baldeaglecore")
public class ModItems {

    public static final Item T1_CORE = (Item) new T1Core()
        .setRegistryName("baldeaglecore", "t1_core")
        .setTranslationKey("baldeaglecore.t1_core")
        .setCreativeTab(BaldeagleCoreTab.INSTANCE);

    public static final Item T2_CORE = (Item) new T2Core()
        .setRegistryName("baldeaglecore", "t2_core")
        .setTranslationKey("baldeaglecore.t2_core")
        .setCreativeTab(BaldeagleCoreTab.INSTANCE);

    public static final Item T3_CORE = (Item) new T3Core()
        .setRegistryName("baldeaglecore", "t3_core")
        .setTranslationKey("baldeaglecore.t3_core")
        .setCreativeTab(BaldeagleCoreTab.INSTANCE);

    public static final Item T4_CORE = (Item) new T4Core()
        .setRegistryName("baldeaglecore", "t4_core")
        .setTranslationKey("baldeaglecore.t4_core")
        .setCreativeTab(BaldeagleCoreTab.INSTANCE);

    public static final Item T5_CORE = (Item) new T5Core()
        .setRegistryName("baldeaglecore", "t5_core")
        .setTranslationKey("baldeaglecore.t5_core")
        .setCreativeTab(BaldeagleCoreTab.INSTANCE);

    public static final ItemCoin COIN_1 = (ItemCoin) new ItemCoin(
        CurrencyDenomination.COIN_1
    )
        .setRegistryName("baldeaglecore", "coin_1")
        .setTranslationKey("baldeaglecore.coin_1")
        .setCreativeTab(BaldeagleCoreTab.INSTANCE);

    public static final ItemCoin COIN_5 = (ItemCoin) new ItemCoin(
        CurrencyDenomination.COIN_5
    )
        .setRegistryName("baldeaglecore", "coin_5")
        .setTranslationKey("baldeaglecore.coin_5")
        .setCreativeTab(BaldeagleCoreTab.INSTANCE);

    public static final ItemCoin COIN_10 = (ItemCoin) new ItemCoin(
        CurrencyDenomination.COIN_10
    )
        .setRegistryName("baldeaglecore", "coin_10")
        .setTranslationKey("baldeaglecore.coin_10")
        .setCreativeTab(BaldeagleCoreTab.INSTANCE);

    public static final ItemBill BILL_50 = (ItemBill) new ItemBill(
        CurrencyDenomination.BILL_50
    )
        .setRegistryName("baldeaglecore", "bill_50")
        .setTranslationKey("baldeaglecore.bill_50")
        .setCreativeTab(BaldeagleCoreTab.INSTANCE);

    public static final ItemBill BILL_100 = (ItemBill) new ItemBill(
        CurrencyDenomination.BILL_100
    )
        .setRegistryName("baldeaglecore", "bill_100")
        .setTranslationKey("baldeaglecore.bill_100")
        .setCreativeTab(BaldeagleCoreTab.INSTANCE);

    public static Item getCurrencyItem(CurrencyDenomination denomination) {
        if (denomination == null) {
            return null;
        }
        switch (denomination) {
            case COIN_1:
                return COIN_1;
            case COIN_5:
                return COIN_5;
            case COIN_10:
                return COIN_10;
            case BILL_50:
                return BILL_50;
            case BILL_100:
                return BILL_100;
            default:
                return null;
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event
            .getRegistry()
            .registerAll(
                T1_CORE,
                T2_CORE,
                T3_CORE,
                T4_CORE,
                T5_CORE,
                COIN_1,
                COIN_5,
                COIN_10,
                BILL_50,
                BILL_100
            );
    }
}
