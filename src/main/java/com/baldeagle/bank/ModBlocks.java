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

    public static final Block BANK = new BlockBank();

    private ModBlocks() {}

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(BANK);
    }

    @SubscribeEvent
    public static void registerItemBlocks(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(
                new ItemBlock(BANK).setRegistryName(BANK.getRegistryName())
        );
    }
}