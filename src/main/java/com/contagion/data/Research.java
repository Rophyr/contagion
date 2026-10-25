package com.contagion.data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.contagion.text.Msg;

/**
 * Shared (server-wide) research progress: discovered facts, the lab notebook, and running microscope jobs.
 *
 * <p>Findings are keys such as {@code identified}, {@code cat:MINERAL}, {@code form:MINERAL:GROUND},
 * {@code toxic:SPIDER_EYE}, {@code inert:SUGAR}, plus bookkeeping keys ({@code used:<donor>:<day>}).
 * Notebook entries are {@link Msg}s so each reader sees them in their own language.
 */
public record Research(List<String> findings, List<Msg> log, List<PendingTest> pending) {
	public static final int MAX_LOG = 60;
	public static final Research EMPTY = new Research(List.of(), List.of(), List.of());

	public static final Codec<Research> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.listOf().fieldOf("findings").forGetter(Research::findings),
			Msg.CODEC.listOf().fieldOf("log").forGetter(Research::log),
			PendingTest.CODEC.listOf().fieldOf("pending").forGetter(Research::pending)
	).apply(i, Research::new));

	public boolean knows(String finding) {
		return this.findings.contains(finding);
	}

	public Research withFinding(String finding) {
		if (knows(finding)) {
			return this;
		}

		List<String> list = new ArrayList<>(this.findings);
		list.add(finding);
		return new Research(List.copyOf(list), this.log, this.pending);
	}

	public Research withLog(Msg entry) {
		List<Msg> list = new ArrayList<>(this.log);
		list.add(entry);

		while (list.size() > MAX_LOG) {
			list.removeFirst();
		}

		return new Research(this.findings, List.copyOf(list), this.pending);
	}

	public Research withPending(PendingTest test) {
		List<PendingTest> list = new ArrayList<>(this.pending);
		list.add(test);
		return new Research(this.findings, this.log, List.copyOf(list));
	}

	public Research withoutPending(Predicate<PendingTest> filter) {
		List<PendingTest> list = new ArrayList<>(this.pending);
		list.removeIf(filter);
		return new Research(this.findings, this.log, List.copyOf(list));
	}

	public Research replacePending(PendingTest old, PendingTest replacement) {
		List<PendingTest> list = new ArrayList<>(this.pending);
		int index = list.indexOf(old);

		if (index >= 0) {
			list.set(index, replacement);
		}

		return new Research(this.findings, this.log, List.copyOf(list));
	}
}
