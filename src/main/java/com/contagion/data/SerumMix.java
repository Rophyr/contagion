package com.contagion.data;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Contents of a serum vial: the reagents (by {@link com.contagion.disease.Reagent} name) and whether it is sealed.
 */
public record SerumMix(List<String> reagents, boolean sealed) {
	public static final int MAX_REAGENTS = 4;

	public static final Codec<SerumMix> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.listOf().fieldOf("reagents").forGetter(SerumMix::reagents),
			Codec.BOOL.fieldOf("sealed").forGetter(SerumMix::sealed)
	).apply(i, SerumMix::new));

	public SerumMix with(String reagent) {
		List<String> list = new ArrayList<>(this.reagents);
		list.add(reagent);
		return new SerumMix(List.copyOf(list), this.sealed);
	}

	public SerumMix seal() {
		return new SerumMix(this.reagents, true);
	}

	public boolean isFull() {
		return this.reagents.size() >= MAX_REAGENTS;
	}
}
