package com.contagion.data;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.contagion.disease.Ingredient;
import com.contagion.text.Msg;

/**
 * The world's pathogen. Generated randomly the first time anyone is infected, so every world has a different cure.
 *
 * @param nameCode "K-30:3:7" (designation, adjective index, noun index); see {@link #nameMsg()}
 * @param profile  0 = respiratory, 1 = neurological, 2 = hemorrhagic (decides which symptoms dominate)
 * @param cure     the three ingredients (reagent + preparation form) that together neutralize the pathogen
 * @param toxic    reagent names that are toxic to the host, whatever their form
 * @param airborne airborne transmissibility multiplier
 * @param contact  contact (blood/melee) transmissibility multiplier
 */
public record Strain(String nameCode, int profile, List<String> cure, List<String> toxic, float airborne, float contact) {
	public static final int RESPIRATORY = 0;
	public static final int NEUROLOGICAL = 1;
	public static final int HEMORRHAGIC = 2;

	public static final Codec<Strain> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("name").forGetter(Strain::nameCode),
			Codec.INT.fieldOf("profile").forGetter(Strain::profile),
			Codec.STRING.listOf().fieldOf("cure").forGetter(Strain::cure),
			Codec.STRING.listOf().fieldOf("toxic").forGetter(Strain::toxic),
			Codec.FLOAT.fieldOf("airborne").forGetter(Strain::airborne),
			Codec.FLOAT.fieldOf("contact").forGetter(Strain::contact)
	).apply(i, Strain::new));

	public List<Ingredient> cureIngredients() {
		return Ingredient.parseAll(this.cure);
	}

	public boolean isCureReagent(String reagent) {
		for (Ingredient ingredient : cureIngredients()) {
			if (ingredient.reagent().name().equals(reagent)) {
				return true;
			}
		}

		return false;
	}

	/** e.g. Strain K-30 "Pale Fever" / Souche K-30 « Fièvre pâle » */
	public Msg nameMsg() {
		String[] parts = this.nameCode.split(":");

		if (parts.length != 3) {
			return Msg.of("contagion.strain.name", this.nameCode, "", "");
		}

		return Msg.of("contagion.strain.name", parts[0], Msg.of("contagion.strain.adj." + parts[1]), Msg.of("contagion.strain.noun." + parts[2]));
	}

	public Msg profileMsg() {
		return Msg.of(switch (this.profile) {
			case NEUROLOGICAL -> "contagion.profile.neurological";
			case HEMORRHAGIC -> "contagion.profile.hemorrhagic";
			default -> "contagion.profile.respiratory";
		});
	}
}
