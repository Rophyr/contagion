package com.contagion.registry;

import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

import com.contagion.ContagionMod;
import com.contagion.data.BloodSample;
import com.contagion.data.CultureData;
import com.contagion.data.SerumMix;
import com.contagion.disease.Ingredient;

public final class ModComponents {
	public static final DataComponentType<BloodSample> BLOOD_SAMPLE = register("blood_sample", BloodSample.CODEC);
	public static final DataComponentType<SerumMix> SERUM = register("serum", SerumMix.CODEC);
	public static final DataComponentType<CultureData> CULTURE = register("culture", CultureData.CODEC);
	public static final DataComponentType<Ingredient> INGREDIENT = register("ingredient", Ingredient.CODEC);

	private ModComponents() {
	}

	private static <T> DataComponentType<T> register(String name, Codec<T> codec) {
		return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, ContagionMod.id(name),
				DataComponentType.<T>builder()
						.persistent(codec)
						.networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(codec))
						.build());
	}

	public static void init() {
		// Loads the class, which registers the components.
	}
}
