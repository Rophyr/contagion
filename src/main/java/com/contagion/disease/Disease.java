package com.contagion.disease;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;

import com.contagion.ContagionConfig;
import com.contagion.data.Infection;
import com.contagion.registry.ModAttachments;
import com.contagion.text.Msg;

/**
 * Core rules: stages, viral load, who can catch it.
 */
public final class Disease {
	private Disease() {
	}

	public enum Stage {
		HEALTHY,
		INCUBATION,
		MILD,
		MODERATE,
		SEVERE,
		TERMINAL
	}

	public static @Nullable Infection get(Entity entity) {
		return entity.getAttached(ModAttachments.INFECTION);
	}

	public static boolean isInfected(Entity entity) {
		Infection infection = get(entity);
		return infection != null && infection.infected();
	}

	public static boolean isImmune(Entity entity) {
		Infection infection = get(entity);
		return infection != null && infection.immune();
	}

	public static Stage stage(@Nullable Infection infection) {
		if (infection == null || !infection.infected()) {
			return Stage.HEALTHY;
		}

		long progress = infection.progress();

		if (progress < ContagionConfig.incubationTicks) {
			return Stage.INCUBATION;
		}

		double day = symptomaticDay(infection);

		if (day < 5) {
			return Stage.MILD;
		} else if (day < 12) {
			return Stage.MODERATE;
		} else if (day < ContagionConfig.symptomaticDays) {
			return Stage.SEVERE;
		}

		return Stage.TERMINAL;
	}

	/** Days since the first symptoms (negative during incubation). */
	public static double symptomaticDay(Infection infection) {
		return (infection.progress() - ContagionConfig.incubationTicks) / (double) ContagionConfig.dayLengthTicks;
	}

	public static long terminalStart() {
		return ContagionConfig.incubationTicks + ContagionConfig.symptomaticTicks();
	}

	/** Infected hosts start shedding the pathogen halfway through incubation, before they feel sick. */
	public static boolean isContagious(@Nullable Infection infection) {
		return infection != null && infection.infected() && infection.progress() >= ContagionConfig.incubationTicks / 2;
	}

	/** 0 = none, 1 = trace (early incubation), 2 = low, 3 = moderate, 4 = high, 5 = extreme. */
	public static int viralLoad(@Nullable Infection infection) {
		int load = switch (stage(infection)) {
			case HEALTHY -> 0;
			case INCUBATION -> infection.progress() < ContagionConfig.incubationTicks / 3 ? 1 : 2;
			case MILD -> 3;
			case MODERATE -> 4;
			case SEVERE, TERMINAL -> 5;
		};

		if (load > 0 && infection.remission() > 0) {
			load = Math.max(1, load - 2);
		}

		return load;
	}

	public static Msg loadMsg(int load) {
		return Msg.of("contagion.load." + Math.max(0, Math.min(5, load)));
	}

	/** Natural transmission targets: survival players and (optionally) villagers and farm animals. */
	public static boolean canCatch(Entity entity) {
		if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
			return false;
		}

		if (entity instanceof Player player) {
			if (player.isSpectator() || player.isCreative()) {
				return false;
			}
		} else if (!(ContagionConfig.villagersCanCatch && entity instanceof AbstractVillager)
				&& !(ContagionConfig.animalsCanCatch && WorldInfection.isLivestock(entity))) {
			return false;
		}

		Infection infection = get(entity);
		return infection == null || (!infection.infected() && !infection.immune());
	}

	/**
	 * Infects an entity. Used by commands and transmission.
	 *
	 * @param force ignore immunity and game mode (commands)
	 * @return whether the entity is now newly infected
	 */
	public static boolean infect(MinecraftServer server, LivingEntity entity, boolean force) {
		if (!entity.isAlive()) {
			return false;
		}

		Infection current = get(entity);

		if (current != null && current.infected()) {
			return false;
		}

		if (!force && !canCatch(entity)) {
			return false;
		}

		Strains.getOrCreate(server);
		entity.setAttached(ModAttachments.INFECTION, Infection.fresh());
		return true;
	}

	public static void cure(LivingEntity entity, boolean immunize) {
		if (immunize) {
			entity.setAttached(ModAttachments.INFECTION, Infection.immuneHost());
		} else {
			entity.removeAttached(ModAttachments.INFECTION);
		}

		Symptoms.clearHealthPenalty(entity);
	}
}
