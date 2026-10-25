package com.contagion.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Data stored on a blood sample item.
 *
 * @param source   name of the donor
 * @param infected whether the donor carried the pathogen when the blood was drawn
 * @param load     viral load, 0 (none) to 5 (extreme)
 * @param immune   whether the donor had antibodies (was cured)
 * @param day      world day the sample was drawn on
 */
public record BloodSample(String source, boolean infected, int load, boolean immune, int day) {
	public static final Codec<BloodSample> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("source").forGetter(BloodSample::source),
			Codec.BOOL.fieldOf("infected").forGetter(BloodSample::infected),
			Codec.INT.fieldOf("load").forGetter(BloodSample::load),
			Codec.BOOL.fieldOf("immune").forGetter(BloodSample::immune),
			Codec.INT.fieldOf("day").forGetter(BloodSample::day)
	).apply(i, BloodSample::new));
}
