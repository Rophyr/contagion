package com.contagion.data;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A running lab job: microscope analysis, microscope culture test, or a beaker extraction.
 *
 * @param dimension   dimension id of the block
 * @param pos         packed block position
 * @param readyAt     overworld game time at which it's done
 * @param kind        {@link #ANALYSIS}, {@link #CULTURE_TEST} or {@link #BOIL}
 * @param sample      donor info (analysis, culture test); unused for boiling
 * @param ingredients tested serum (culture test) or the boiled ingredient (boil), encoded
 * @param notified    whether nearby players were told it finished
 */
public record PendingTest(String dimension, long pos, long readyAt, int kind, BloodSample sample, List<String> ingredients, boolean notified) {
	public static final int ANALYSIS = 0;
	public static final int CULTURE_TEST = 1;
	public static final int BOIL = 2;

	public static final BloodSample NO_SAMPLE = new BloodSample("", false, 0, false, 0);

	public static final Codec<PendingTest> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("dimension").forGetter(PendingTest::dimension),
			Codec.LONG.fieldOf("pos").forGetter(PendingTest::pos),
			Codec.LONG.fieldOf("ready_at").forGetter(PendingTest::readyAt),
			Codec.INT.fieldOf("kind").forGetter(PendingTest::kind),
			BloodSample.CODEC.fieldOf("sample").forGetter(PendingTest::sample),
			Codec.STRING.listOf().fieldOf("ingredients").forGetter(PendingTest::ingredients),
			Codec.BOOL.fieldOf("notified").forGetter(PendingTest::notified)
	).apply(i, PendingTest::new));

	public boolean isAt(String dim, long packedPos) {
		return this.dimension.equals(dim) && this.pos == packedPos;
	}

	public PendingTest markNotified() {
		return new PendingTest(this.dimension, this.pos, this.readyAt, this.kind, this.sample, this.ingredients, true);
	}
}
