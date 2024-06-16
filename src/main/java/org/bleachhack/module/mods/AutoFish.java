/*
 * some licence stuff here
 */
package org.bleachhack.module.mods;

import java.util.Comparator;

import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingMode;
import org.bleachhack.setting.module.SettingSlider;
import org.bleachhack.util.InventoryUtils;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoFish extends Module {

	private boolean threwRod;
	private boolean reeledFish;
	private int lastCast = 0;

	public AutoFish() {
		super("AutoFish", KEY_UNBOUND, ModuleCategory.PLAYER, "Automatically fishes for you.",
				new SettingMode("Mode", "Normal", "Aggressive", "Passive").withDesc("AutoFish mode."),
				new SettingSlider("Delay", 0, 20, 1, 1).withDesc("Casting delay in ticks"));
	}

	@Override
	public void onDisable(boolean inWorld) {
		threwRod = false;
		reeledFish = false;

		super.onDisable(inWorld);
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (mc.player.fishHook != null) {
			threwRod = false;

			boolean caughtFish = mc.player.fishHook.getDataTracker().get(FishingBobberEntity.CAUGHT_FISH);
			if (!reeledFish && caughtFish) {
				Hand hand = getHandWithRod();
				if (hand != null) {
					// reel back
					mc.interactionManager.interactItem(mc.player, hand);
					reeledFish = true;
					return;
				}
			} else if (!caughtFish) {
				reeledFish = false;
			}
		}

		if (!threwRod && mc.player.fishHook == null && getSetting(0).asMode().getMode() != 2 && lastCast >= getSetting(1).asSlider().getValueInt()) {
			Hand newHand = getSetting(0).asMode().getMode() == 1 ? InventoryUtils.selectSlot(getBestRodSlot()) : getHandWithRod();
			if (newHand != null) {
				// throw again
				mc.interactionManager.interactItem(mc.player, newHand);
				threwRod = true;
				reeledFish = false;
				lastCast = 0;
			}
		}
		else if (!threwRod && mc.player.fishHook == null)
		{
			lastCast++;
		}
	}

	private Hand getHandWithRod() {
		return mc.player.getMainHandStack().getItem() == Items.FISHING_ROD ? Hand.MAIN_HAND
				: mc.player.getOffHandStack().getItem() == Items.FISHING_ROD ? Hand.OFF_HAND
						: null;
	}

	private int getBestRodSlot() {
		int slot = InventoryUtils.getSlot(true, true, Comparator.comparingInt(i -> {
			ItemStack is = mc.player.getInventory().getStack(i);
			if (is.getItem() != Items.FISHING_ROD)
				return -1;

			return EnchantmentHelper.get(is).values().stream().mapToInt(Integer::intValue).sum();
		}));

		if (mc.player.getInventory().getStack(slot).getItem() == Items.FISHING_ROD) {
			return slot;
		}

		return -1;
	}
}
