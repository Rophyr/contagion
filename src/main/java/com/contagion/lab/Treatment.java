package com.contagion.lab;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import com.contagion.ContagionConfig;
import com.contagion.data.Infection;
import com.contagion.data.Research;
import com.contagion.data.SerumMix;
import com.contagion.data.Strain;
import com.contagion.disease.Disease;
import com.contagion.disease.Ingredient;
import com.contagion.disease.Strains;
import com.contagion.disease.Vanilla;
import com.contagion.registry.ModAttachments;
import com.contagion.text.Chat;
import com.contagion.text.Msg;

/**
 * What happens when a serum is injected.
 * <ul>
 *     <li>All three active agents (right reagent, right preparation): cured, and immune from then on.</li>
 *     <li>One or two: temporary remission (symptoms ease, the disease pauses), weaker each time.</li>
 *     <li>Right reagent in the wrong preparation: barely any relief.</li>
 *     <li>Toxic reagents: side effects, from nausea up to wither, which can kill.</li>
 * </ul>
 */
public final class Treatment {
	private Treatment() {
	}

	/** Ingredients that are exactly right (reagent and form). */
	public static int activeAgents(Strain strain, List<Ingredient> ingredients) {
		List<Ingredient> cure = strain.cureIngredients();
		int count = 0;

		for (Ingredient ingredient : ingredients) {
			if (cure.contains(ingredient)) {
				count++;
			}
		}

		return count;
	}

	/** Cure reagents present only in a wrong preparation. */
	public static int traceAgents(Strain strain, List<Ingredient> ingredients) {
		int count = 0;

		for (Ingredient cure : strain.cureIngredients()) {
			boolean exact = ingredients.contains(cure);
			boolean reagent = false;

			for (Ingredient ingredient : ingredients) {
				reagent |= ingredient.reagent() == cure.reagent();
			}

			if (reagent && !exact) {
				count++;
			}
		}

		return count;
	}

	/** Toxic reagents (any preparation), counted once each. */
	public static int toxicAgents(Strain strain, List<Ingredient> ingredients) {
		int count = 0;

		for (String toxic : strain.toxic()) {
			for (Ingredient ingredient : ingredients) {
				if (ingredient.reagent().name().equals(toxic)) {
					count++;
					break;
				}
			}
		}

		return count;
	}

	public static void inject(ServerLevel level, ServerPlayer user, LivingEntity target, SerumMix mix) {
		MinecraftServer server = level.getServer();
		Strain strain = Strains.get(server);
		List<Ingredient> ingredients = Ingredient.parseAll(mix.reagents());
		int active = strain == null ? 0 : activeAgents(strain, ingredients);
		int trace = strain == null ? 0 : traceAgents(strain, ingredients);
		int toxic = strain == null ? 0 : toxicAgents(strain, ingredients);
		String name = target.getName().getString();
		boolean self = user == target;

		Vanilla.playSound(level, target, "item.bottle.empty", 0.8F, 1.6F);
		Advancements.award(user, "mad_scientist");

		if (!self) {
			Chat.send(user, Msg.of("contagion.inject.other", name), ChatFormatting.GRAY);
		}

		applySideEffects(target, toxic);

		Infection infection = Disease.get(target);
		String verdict;

		if (infection != null && infection.infected()) {
			if (active >= 3) {
				Disease.cure(target, true);
				Vanilla.addEffect(target, "regeneration", 200, 0);
				Vanilla.addEffect(target, "weakness", 2400, 0);
				Vanilla.playSound(level, target, "entity.player.levelup", 0.7F, 0.6F);
				Chat.send(target, Msg.of("contagion.inject.cured"), ChatFormatting.GREEN);
				verdict = "cured";

				if (target instanceof ServerPlayer patient) {
					Vanilla.title(patient, Msg.of("contagion.title.cured").styled(ChatFormatting.GREEN, ChatFormatting.BOLD),
							Msg.of("contagion.title.cured_sub").styled(ChatFormatting.GRAY));
					Advancements.award(patient, "cured");
				}

				if (!self) {
					Advancements.award(user, "doctor");
				}
			} else if (active > 0 || trace > 0) {
				long day = ContagionConfig.dayLengthTicks;
				long base = active == 2 ? day * 3 / 5 : active == 1 ? day / 4 : day / 12;
				long remission = (long) (base * Math.pow(0.7, infection.treatments()));
				target.setAttached(ModAttachments.INFECTION, infection.withTreatment(Math.max(infection.remission(), remission)));
				verdict = active == 2 ? "clear" : active == 1 ? "slight" : "faint";
				Chat.send(target, Msg.of("contagion.inject.relief_" + verdict), ChatFormatting.GRAY);

				if (infection.treatments() >= 2) {
					Chat.send(target, Msg.of("contagion.inject.weaker"), ChatFormatting.GRAY);
				}
			} else {
				Chat.send(target, Msg.of("contagion.inject.nothing"), ChatFormatting.GRAY);
				verdict = "none";
			}
		} else {
			if (toxic == 0) {
				Chat.send(target, Msg.of("contagion.inject.healthy"), ChatFormatting.GRAY);
			}

			verdict = "healthy";
		}

		// Mobs can't describe how they feel, so the injector observes the reaction instead.
		if (!self && !(target instanceof ServerPlayer)) {
			Chat.send(user, Msg.of("contagion.inject.observed", name, Msg.of("contagion.log.verdict." + verdict)), ChatFormatting.GRAY);
		}

		if (strain != null) {
			Msg entry = Msg.of(toxic > 0 ? "contagion.log.inject_toxic" : "contagion.log.inject", Lab.day(server), Ingredient.listMsg(ingredients), name,
					Msg.of("contagion.log.verdict." + verdict));
			Research research = Strains.research(server).withLog(entry);
			Strains.setResearch(server, research);
		}
	}

	private static void applySideEffects(LivingEntity target, int toxic) {
		if (toxic <= 0) {
			return;
		}

		if (toxic == 1) {
			Vanilla.addEffect(target, "poison", 160, 0);
			Vanilla.addEffect(target, "nausea", 200, 0);
			Chat.send(target, Msg.of("contagion.inject.toxic1"), ChatFormatting.RED);
		} else if (toxic == 2) {
			Vanilla.addEffect(target, "poison", 400, 1);
			Vanilla.addEffect(target, "nausea", 300, 0);
			Vanilla.addEffect(target, "weakness", 1200, 0);
			Chat.send(target, Msg.of("contagion.inject.toxic2"), ChatFormatting.RED);
		} else {
			Vanilla.addEffect(target, "wither", 240, 1);
			Vanilla.addEffect(target, "poison", 600, 1);
			Vanilla.addEffect(target, "nausea", 400, 0);
			Vanilla.addEffect(target, "blindness", 100, 0);
			Chat.send(target, Msg.of("contagion.inject.toxic3"), ChatFormatting.DARK_RED);
		}
	}
}
