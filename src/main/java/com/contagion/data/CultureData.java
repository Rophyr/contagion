package com.contagion.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.contagion.ContagionConfig;

/**
 * A petri dish inoculated with blood. It grows for a while (anywhere, even in a chest), then either shows
 * colonies or stays sterile, and eventually dies.
 *
 * @param source    donor name
 * @param load      viral load of the blood used
 * @param day       world day the blood was drawn
 * @param startedAt overworld game time of inoculation
 * @param grows     decided at inoculation: whether colonies will appear (needs enough pathogen)
 */
public record CultureData(String source, int load, int day, long startedAt, boolean grows) {
	public static final int GROWING = 0;
	public static final int COLONIZED = 1;
	public static final int STERILE = 2;
	public static final int DEAD = 3;

	public static final Codec<CultureData> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("source").forGetter(CultureData::source),
			Codec.INT.fieldOf("load").forGetter(CultureData::load),
			Codec.INT.fieldOf("day").forGetter(CultureData::day),
			Codec.LONG.fieldOf("started_at").forGetter(CultureData::startedAt),
			Codec.BOOL.fieldOf("grows").forGetter(CultureData::grows)
	).apply(i, CultureData::new));

	public int state(long now) {
		long age = now - this.startedAt;

		if (age < ContagionConfig.cultureGrowTicks) {
			return GROWING;
		}

		if (!this.grows) {
			return STERILE;
		}

		return age > ContagionConfig.cultureGrowTicks + ContagionConfig.cultureLifeTicks ? DEAD : COLONIZED;
	}

	public double growth(long now) {
		return Math.min(1.0, (now - this.startedAt) / (double) Math.max(1, ContagionConfig.cultureGrowTicks));
	}
}
