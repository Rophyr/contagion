package com.contagion.disease;

import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import com.contagion.ContagionConfig;
import com.contagion.data.Infection;
import com.contagion.registry.ModAttachments;
import com.contagion.registry.ModItems;

/**
 * The pathogen's natural reservoir: farm animals (and the odd villager) carry it from the start.
 * <ul>
 *     <li>Every animal or villager is checked once, the first time it is loaded: a few are already infected.</li>
 *     <li>Sick animals are ill for a few days, then recover with antibodies or die.</li>
 *     <li>Meat from an infected animal is tainted: it looks normal, but eating it (even cooked) infects you.</li>
 * </ul>
 */
public final class WorldInfection {
	private static final Set<String> LIVESTOCK = Set.of("cow", "mooshroom", "pig", "sheep", "chicken", "rabbit", "goat");

	private WorldInfection() {
	}

	public static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> screen(level, entity));
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> taint(context, drops));
	}

	public static boolean isLivestock(Entity entity) {
		return LIVESTOCK.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath());
	}

	private static void screen(ServerLevel level, Entity entity) {
		if (!ContagionConfig.worldInfection || !(entity instanceof LivingEntity living) || entity.hasAttached(ModAttachments.SCREENED)) {
			return;
		}

		boolean animal = isLivestock(entity);

		if (!animal && !(entity instanceof AbstractVillager)) {
			return;
		}

		entity.setAttached(ModAttachments.SCREENED, true);
		RandomSource random = level.getRandom();
		double chance = animal ? ContagionConfig.animalCarrierChance : ContagionConfig.villagerCarrierChance;

		if (Disease.get(entity) != null || random.nextDouble() >= chance) {
			return;
		}

		Strains.getOrCreate(level.getServer());
		// Carriers are found at any point of their illness; villagers never past the first days.
		long span = ContagionConfig.incubationTicks + (animal ? ContagionConfig.animalIllnessDays : 2) * ContagionConfig.dayLengthTicks;
		long progress = (long) (random.nextDouble() * span * 0.8);
		Infection fresh = Infection.fresh();
		living.setAttached(ModAttachments.INFECTION, fresh.withProgress(progress, 0));
	}

	/** Called every second for infected livestock: the end of the illness. */
	public static boolean tickAnimal(ServerLevel level, LivingEntity animal, Infection infection) {
		if (Disease.symptomaticDay(infection) < ContagionConfig.animalIllnessDays) {
			return false;
		}

		if (animal.getRandom().nextDouble() < ContagionConfig.animalDeathChance) {
			animal.hurtServer(level, Vanilla.infectionDamage(level), 1000.0F);
		} else {
			Disease.cure(animal, true);
		}

		return true;
	}

	/** Drops of an infected animal (or villager) become tainted. */
	private static void taint(LootContext context, List<ItemStack> drops) {
		Entity entity = context.getOptional(LootContextParams.THIS_ENTITY);

		if (!(entity instanceof LivingEntity) || entity instanceof Player || !Disease.isInfected(entity)) {
			return;
		}

		for (int i = 0; i < drops.size(); i++) {
			ItemStack stack = drops.get(i);
			Item tainted = ModItems.TAINTED.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());

			if (tainted != null && BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft")) {
				drops.set(i, stack.transmuteCopy(tainted));
			}
		}
	}
}
