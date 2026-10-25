package com.contagion.disease;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.world.item.ItemStack;

import com.contagion.registry.ModComponents;
import com.contagion.registry.ModItems;
import com.contagion.text.Msg;

/** A reagent in a given preparation form, e.g. ground Glowstone Dust. Stored as {@code "GLOWSTONE_DUST:GROUND"}. */
public record Ingredient(Reagent reagent, Form form) {
	public static final Codec<Ingredient> CODEC = Codec.STRING.xmap(Ingredient::parseOrDefault, Ingredient::encode);

	public String encode() {
		return this.reagent.name() + ":" + this.form.name();
	}

	public static @Nullable Ingredient parse(String text) {
		int colon = text.indexOf(':');
		Reagent reagent = Reagent.byName(colon < 0 ? text : text.substring(0, colon));
		return reagent == null ? null : new Ingredient(reagent, colon < 0 ? Form.RAW : Form.byName(text.substring(colon + 1)));
	}

	private static Ingredient parseOrDefault(String text) {
		Ingredient ingredient = parse(text);
		return ingredient == null ? new Ingredient(Reagent.SUGAR, Form.RAW) : ingredient;
	}

	public static List<Ingredient> parseAll(List<String> list) {
		List<Ingredient> out = new ArrayList<>();

		for (String text : list) {
			Ingredient ingredient = parse(text);

			if (ingredient != null && !out.contains(ingredient)) {
				out.add(ingredient);
			}
		}

		return out;
	}

	/** A raw reagent item or a prepared reagent (ground powder / extract). */
	public static @Nullable Ingredient of(ItemStack stack) {
		if (stack.is(ModItems.PREPARED_REAGENT)) {
			return stack.get(ModComponents.INGREDIENT);
		}

		Reagent reagent = Reagent.of(stack);
		return reagent == null ? null : new Ingredient(reagent, Form.RAW);
	}

	/** "Glowstone Dust" / "Ground Glowstone Dust" / "Glowstone Dust Extract". */
	public Msg msg() {
		return Msg.of("contagion.ingredient." + this.form.id(), this.reagent.msg());
	}

	/** "[A, B, C]" */
	public static Msg listMsg(List<Ingredient> ingredients) {
		if (ingredients.isEmpty()) {
			return Msg.of("contagion.list", "");
		}

		Msg joined = ingredients.getFirst().msg();

		for (int i = 1; i < ingredients.size(); i++) {
			joined = Msg.of("contagion.join", joined, ingredients.get(i).msg());
		}

		return Msg.of("contagion.list", joined);
	}
}
