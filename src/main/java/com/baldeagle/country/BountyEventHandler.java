package com.baldeagle.country;

import com.baldeagle.country.currency.CurrencyDenomination;
import com.baldeagle.country.currency.CurrencyItemHelper;
import com.baldeagle.economy.EconomyManager;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

public class BountyEventHandler {

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() == null) {
            return;
        }
        World world = event.getEntityLiving().world;
        if (world == null || world.isRemote) {
            return;
        }
        if (!(event.getEntityLiving() instanceof EntityPlayerMP)) {
            return;
        }

        Entity source = event.getSource().getTrueSource();
        if (!(source instanceof EntityPlayerMP)) {
            return;
        }

        EntityPlayerMP victim = (EntityPlayerMP) event.getEntityLiving();
        EntityPlayerMP killer = (EntityPlayerMP) source;
        UUID victimId = victim.getUniqueID();

        Country victimCountry = CountryManager.getCountryForPlayer(
            world,
            victimId
        );
        if (victimCountry == null) {
            return;
        }

        Map<UUID, Country> countries = CountryManager.getAllCountries(world);
        boolean bountyPaid = false;
        for (Country bountyCountry : countries.values()) {
            Long reward = bountyCountry.getBountyReward(victimId);
            if (reward == null || reward <= 0) {
                continue;
            }
            if (!bountyCountry.isAtWarWith(victimCountry.getId())) {
                continue;
            }
            if (
                !EconomyManager.withdrawCountry(
                    world,
                    bountyCountry.getName(),
                    reward
                )
            ) {
                continue;
            }

            if (bountyCountry.isMember(killer.getUniqueID())) {
                EconomyManager.depositPlayer(
                    world,
                    killer.getUniqueID(),
                    reward
                );
            } else {
                spawnCurrency(killer, bountyCountry, reward);
            }

            bountyCountry.removeBounty(victimId);
            bountyPaid = true;
        }

        if (bountyPaid) {
            CountryStorage.get(world).markDirty();
        }
    }

    private void spawnCurrency(
        EntityPlayerMP player,
        Country country,
        long amount
    ) {
        long remaining = amount;
        List<CurrencyDenomination> denominations = Arrays.stream(
            CurrencyDenomination.values()
        )
            .sorted(
                Comparator.comparingInt(
                    CurrencyDenomination::getValue
                ).reversed()
            )
            .collect(Collectors.toList());

        for (CurrencyDenomination denom : denominations) {
            long count = remaining / denom.getValue();
            if (count <= 0) {
                continue;
            }

            int maxStack =
                denom.getType() ==
                com.baldeagle.country.currency.CurrencyType.COIN
                    ? 64
                    : 16;
            while (count > 0) {
                int give = (int) Math.min(count, (long) maxStack);
                ItemStack stack = CurrencyItemHelper.createCurrencyStack(
                    country,
                    denom,
                    give
                );
                if (!stack.isEmpty()) {
                    if (!player.inventory.addItemStackToInventory(stack)) {
                        player.dropItem(stack, false);
                    }
                }
                count -= give;
            }

            remaining = remaining % denom.getValue();
            if (remaining <= 0) {
                break;
            }
        }
    }
}
