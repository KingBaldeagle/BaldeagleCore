package com.baldeagle.bank;

import com.baldeagle.country.mint.client.GuiCurrencyExchange;
import com.baldeagle.country.mint.client.GuiMint;
import com.baldeagle.country.mint.container.ContainerCurrencyExchange;
import com.baldeagle.country.mint.container.ContainerMint;
import com.baldeagle.country.mint.tile.TileEntityCurrencyExchange;
import com.baldeagle.country.mint.tile.TileEntityMint;
import com.baldeagle.country.vault.client.GuiVault;
import com.baldeagle.country.vault.container.ContainerVault;
import com.baldeagle.country.vault.tile.TileEntityVault;
import com.baldeagle.economy.atm.ContainerAtm;
import com.baldeagle.economy.atm.GuiAtm;
import com.baldeagle.economy.atm.TileEntityAtm;
import com.baldeagle.research.client.GuiResearchAssembler;
import com.baldeagle.research.container.ContainerResearchAssembler;
import com.baldeagle.research.tile.TileEntityResearchAssembler;
import com.baldeagle.shop.ContainerShop;
import com.baldeagle.shop.GuiShop;
import com.baldeagle.shop.TileEntityShop;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {

    public static final int BANK_GUI_ID = 1;
    public static final int MINT_GUI_ID = 2;
    public static final int CURRENCY_EXCHANGE_GUI_ID = 3;
    public static final int VAULT_GUI_ID = 4;
    public static final int ATM_GUI_ID = 5;
    public static final int SHOP_GUI_ID = 6;
    public static final int RESEARCH_ASSEMBLER_GUI_ID = 7;

    @Override
    public Object getServerGuiElement(
        int id,
        EntityPlayer player,
        World world,
        int x,
        int y,
        int z
    ) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity tile = world.getTileEntity(pos);
        switch (id) {
            case BANK_GUI_ID:
                if (tile instanceof TileEntityBank) {
                    return new ContainerBank(
                        player.inventory,
                        (TileEntityBank) tile
                    );
                }
                break;
            case MINT_GUI_ID:
                if (tile instanceof TileEntityMint) {
                    return new ContainerMint(
                        player.inventory,
                        (TileEntityMint) tile
                    );
                }
                break;
            case CURRENCY_EXCHANGE_GUI_ID:
                if (tile instanceof TileEntityCurrencyExchange) {
                    return new ContainerCurrencyExchange(
                        player.inventory,
                        (TileEntityCurrencyExchange) tile
                    );
                }
                break;
            case VAULT_GUI_ID:
                if (tile instanceof TileEntityVault) {
                    return new ContainerVault(
                        player.inventory,
                        (TileEntityVault) tile
                    );
                }
                break;
            case ATM_GUI_ID:
                if (tile instanceof TileEntityAtm) {
                    return new ContainerAtm(
                        player.inventory,
                        (TileEntityAtm) tile
                    );
                }
                break;
            case SHOP_GUI_ID:
                if (tile instanceof TileEntityShop) {
                    return new ContainerShop(
                        player.inventory,
                        (TileEntityShop) tile
                    );
                }
                break;
            case RESEARCH_ASSEMBLER_GUI_ID:
                if (tile instanceof TileEntityResearchAssembler) {
                    return new ContainerResearchAssembler(
                        player.inventory,
                        (TileEntityResearchAssembler) tile
                    );
                }
                break;
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(
        int id,
        EntityPlayer player,
        World world,
        int x,
        int y,
        int z
    ) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity tile = world.getTileEntity(pos);
        switch (id) {
            case BANK_GUI_ID:
                if (tile instanceof TileEntityBank) {
                    return new GuiBank(
                        new ContainerBank(
                            player.inventory,
                            (TileEntityBank) tile
                        )
                    );
                }
                break;
            case MINT_GUI_ID:
                if (tile instanceof TileEntityMint) {
                    return new GuiMint(
                        new ContainerMint(
                            player.inventory,
                            (TileEntityMint) tile
                        )
                    );
                }
                break;
            case CURRENCY_EXCHANGE_GUI_ID:
                if (tile instanceof TileEntityCurrencyExchange) {
                    return new GuiCurrencyExchange(
                        new ContainerCurrencyExchange(
                            player.inventory,
                            (TileEntityCurrencyExchange) tile
                        )
                    );
                }
                break;
            case VAULT_GUI_ID:
                if (tile instanceof TileEntityVault) {
                    return new GuiVault(
                        new ContainerVault(
                            player.inventory,
                            (TileEntityVault) tile
                        )
                    );
                }
                break;
            case ATM_GUI_ID:
                if (tile instanceof TileEntityAtm) {
                    return new GuiAtm(
                        new ContainerAtm(player.inventory, (TileEntityAtm) tile)
                    );
                }
                break;
            case SHOP_GUI_ID:
                if (tile instanceof TileEntityShop) {
                    return new GuiShop(
                        new ContainerShop(
                            player.inventory,
                            (TileEntityShop) tile
                        )
                    );
                }
                break;
            case RESEARCH_ASSEMBLER_GUI_ID:
                if (tile instanceof TileEntityResearchAssembler) {
                    return new GuiResearchAssembler(
                        new ContainerResearchAssembler(
                            player.inventory,
                            (TileEntityResearchAssembler) tile
                        )
                    );
                }
                break;
        }
        return null;
    }
}
