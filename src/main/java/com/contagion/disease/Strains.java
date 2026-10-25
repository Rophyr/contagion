package com.contagion.disease;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;

import com.contagion.ContagionMod;
import com.contagion.data.Research;
import com.contagion.data.Strain;
import com.contagion.registry.ModAttachments;

/**
 * Creates and stores the world's strain and research data (server-wide global attachments).
 */
public final class Strains {
	/** Number of adjective/noun variants in the language files (contagion.strain.adj.N / .noun.N). */
	private static final int NAME_WORDS = 12;

	private Strains() {
	}

	public static @Nullable Strain get(MinecraftServer server) {
		return server.globalAttachments().getAttached(ModAttachments.STRAIN);
	}

	public static Strain getOrCreate(MinecraftServer server) {
		Strain strain = get(server);

		if (strain == null) {
			strain = generate(RandomSource.create());
			server.globalAttachments().setAttached(ModAttachments.STRAIN, strain);
			ContagionMod.LOGGER.info("Generated new pathogen {} (spoiler: cure = {})", strain.nameCode(), strain.cure());
		}

		return strain;
	}

	public static Research research(MinecraftServer server) {
		return server.globalAttachments().getAttachedOrCreate(ModAttachments.RESEARCH);
	}

	public static void setResearch(MinecraftServer server, Research research) {
		server.globalAttachments().setAttached(ModAttachments.RESEARCH, research);
	}

	/** Replaces the strain with a brand new one and wipes research. */
	public static Strain regenerate(MinecraftServer server) {
		Strain strain = generate(RandomSource.create());
		server.globalAttachments().setAttached(ModAttachments.STRAIN, strain);
		setResearch(server, Research.EMPTY);
		return strain;
	}

	public static Strain generate(RandomSource random) {
		List<Reagent.Category> categories = new ArrayList<>(Arrays.asList(Reagent.Category.values()));
		shuffle(categories, random);

		List<String> cure = new ArrayList<>();
		List<String> cureReagents = new ArrayList<>();
		boolean usedHard = false;

		// One reagent from each of three different categories, at most one of them hard to get.
		for (Reagent.Category category : categories) {
			if (cure.size() == 3) {
				break;
			}

			List<Reagent> options = new ArrayList<>();

			for (Reagent reagent : Reagent.values()) {
				if (reagent.category() == category && (!usedHard || !reagent.hard())) {
					options.add(reagent);
				}
			}

			if (options.isEmpty()) {
				continue;
			}

			Reagent pick = options.get(random.nextInt(options.size()));
			usedHard |= pick.hard();
			// Each active agent only works in one preparation: usually ground or boiled, sometimes raw.
			int roll = random.nextInt(10);
			Form form = roll < 2 ? Form.RAW : roll < 6 ? Form.GROUND : Form.EXTRACT;
			cure.add(new Ingredient(pick, form).encode());
			cureReagents.add(pick.name());
		}

		List<String> others = new ArrayList<>();

		for (Reagent reagent : Reagent.values()) {
			if (!cureReagents.contains(reagent.name())) {
				others.add(reagent.name());
			}
		}

		shuffle(others, random);
		List<String> toxic = List.copyOf(others.subList(0, 4));

		int profile = random.nextInt(3);
		float airborne = 0.7F + random.nextFloat() * 0.6F;
		float contact = 0.7F + random.nextFloat() * 0.6F;

		if (profile == Strain.RESPIRATORY) {
			airborne *= 1.3F;
		} else if (profile == Strain.HEMORRHAGIC) {
			contact *= 1.4F;
		}

		String name = (char) ('A' + random.nextInt(26)) + "-" + (10 + random.nextInt(90)) + ":" + random.nextInt(NAME_WORDS) + ":" + random.nextInt(NAME_WORDS);

		return new Strain(name, profile, List.copyOf(cure), toxic, airborne, contact);
	}

	private static <T> void shuffle(List<T> list, RandomSource random) {
		for (int i = list.size() - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			T tmp = list.get(i);
			list.set(i, list.get(j));
			list.set(j, tmp);
		}
	}
}
