package com.contagion.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-entity disease state (stored as a persistent Fabric data attachment).
 *
 * @param infected   currently carrying the pathogen
 * @param immune     has antibodies (was cured) and cannot be infected again
 * @param progress   ticks of disease progression since infection
 * @param remission  remaining ticks of symptom suppression from a partial treatment (progression is paused)
 * @param treatments number of partial treatments taken (the pathogen builds tolerance)
 * @param lastStage  highest stage ordinal the host has been told about
 */
public record Infection(boolean infected, boolean immune, long progress, long remission, int treatments, int lastStage) {
	public static final Codec<Infection> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.fieldOf("infected").forGetter(Infection::infected),
			Codec.BOOL.fieldOf("immune").forGetter(Infection::immune),
			Codec.LONG.fieldOf("progress").forGetter(Infection::progress),
			Codec.LONG.fieldOf("remission").forGetter(Infection::remission),
			Codec.INT.fieldOf("treatments").forGetter(Infection::treatments),
			Codec.INT.fieldOf("last_stage").forGetter(Infection::lastStage)
	).apply(i, Infection::new));

	public static Infection fresh() {
		return new Infection(true, false, 0, 0, 0, 0);
	}

	public static Infection immuneHost() {
		return new Infection(false, true, 0, 0, 0, 0);
	}

	public Infection withProgress(long newProgress, long newRemission) {
		return new Infection(this.infected, this.immune, newProgress, newRemission, this.treatments, this.lastStage);
	}

	public Infection withLastStage(int stage) {
		return new Infection(this.infected, this.immune, this.progress, this.remission, this.treatments, stage);
	}

	public Infection withTreatment(long newRemission) {
		return new Infection(this.infected, this.immune, this.progress, newRemission, this.treatments + 1, this.lastStage);
	}
}
