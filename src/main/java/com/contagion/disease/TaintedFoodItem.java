package com.contagion.disease;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Meat from an infected animal. It looks and tastes like the normal item, cooking does not help,
 * and eating it always passes the pathogen on (unless the eater is immune).
 */
public final class TaintedFoodItem extends Item {
	public TaintedFoodItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		ItemStack result = super.finishUsingItem(stack, level, entity);

		if (level instanceof ServerLevel serverLevel && Disease.canCatch(entity)) {
			Disease.infect(serverLevel.getServer(), entity, false);
		}

		return result;
	}
}
