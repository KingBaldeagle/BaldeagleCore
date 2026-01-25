package com.baldeagle.bank;

import com.baldeagle.BaldeagleCore;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = BaldeagleCore.MODID)
public final class ModBlocks {

    public static Block BANK;
    public static Block MINT;
    public static Block CURRENCY_EXCHANGE;
    public static Block VAULT;
    public static Block ATM;

    private ModBlocks() {}

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        // Initialize blocks here, after Forge is ready
        BANK = new BlockBank();
        MINT = new BlockMint();
        CURRENCY_EXCHANGE = new BlockCurrencyExchange();
        VAULT = new BlockVault();
        ATM = new BlockAtm();

        event
            .getRegistry()
            .registerAll(BANK, MINT, CURRENCY_EXCHANGE, VAULT, ATM);
    }

    @SubscribeEvent
    public static void registerItemBlocks(RegistryEvent.Register<Item> event) {
        event
            .getRegistry()
            .registerAll(
                new ItemBlock(BANK).setRegistryName(BANK.getRegistryName()),
                new ItemBlock(MINT).setRegistryName(MINT.getRegistryName()),
                new ItemBlock(CURRENCY_EXCHANGE).setRegistryName(
                    CURRENCY_EXCHANGE.getRegistryName()
                ),
                new ItemBlock(VAULT).setRegistryName(VAULT.getRegistryName()),
                new ItemBlock(ATM).setRegistryName(ATM.getRegistryName())
            );
    }
}
