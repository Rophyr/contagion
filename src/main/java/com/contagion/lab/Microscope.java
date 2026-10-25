package com.contagion.lab;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import com.contagion.ContagionConfig;
import com.contagion.block.MicroscopeBlock;
import com.contagion.data.BloodSample;
import com.contagion.data.CultureData;
import com.contagion.data.PendingTest;
import com.contagion.data.Research;
import com.contagion.data.SerumMix;
import com.contagion.data.Strain;
import com.contagion.disease.Disease;
import com.contagion.disease.Ingredient;
import com.contagion.disease.Strains;
import com.contagion.disease.Vanilla;
import com.contagion.registry.ModComponents;
import com.contagion.registry.ModItems;
import com.contagion.text.Chat;
import com.contagion.text.Msg;

/**
 * The microscope runs two kinds of slow, real-time jobs:
 * <ul>
 *     <li>Analysis of a blood sample: is the pathogen there, how much, and sometimes a new fact about it.</li>
 *     <li>Culture test: a sealed serum against a grown culture (petri dish), showing how well it works and if it
 *     harms tissue, without risking a patient.</li>
 * </ul>
 */
public final class Microscope {
	public static final int MAX_SAMPLE_AGE = 3;

	private Microscope() {
	}

	public static void use(ServerLevel level, ServerPlayer player, BlockPos pos) {
		MinecraftServer server = level.getServer();
		long now = Lab.now(server);
		Research research = Strains.research(server);
		PendingTest pending = LabJobs.find(research, level, pos);
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.5;
		double z = pos.getZ() + 0.5;

		if (pending != null) {
			boolean culture = pending.kind() == PendingTest.CULTURE_TEST;

			if (now < pending.readyAt()) {
				long total = culture ? ContagionConfig.cultureTicks : ContagionConfig.analysisTicks;
				long remaining = pending.readyAt() - now;
				player.sendSystemMessage(Lab.progressBar(Msg.of(culture ? "contagion.progress.culture" : "contagion.progress.analysis"),
						1.0 - remaining / (double) Math.max(1, total), remaining / 20), true);
			} else {
				Research updated = LabJobs.remove(research, level, pos);
				updated = culture ? finishCulture(server, player, pending, updated) : finishAnalysis(server, player, pending, updated);
				Strains.setResearch(server, updated);
				MicroscopeBlock.setStatus(level, pos, MicroscopeBlock.IDLE);
				Vanilla.playSound(level, x, y, z, "item.book.page_turn", SoundSource.BLOCKS, 1.0F, 1.0F);
			}

			return;
		}

		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();
		String dimension = LabJobs.dimension(level);

		if (main.is(ModItems.BLOOD_SAMPLE)) {
			BloodSample sample = main.get(ModComponents.BLOOD_SAMPLE);

			if (sample == null) {
				return;
			}

			main.shrink(1);
			Strains.setResearch(server, research.withPending(new PendingTest(dimension, pos.asLong(), now + ContagionConfig.analysisTicks,
					PendingTest.ANALYSIS, sample, List.of(), false)));
			MicroscopeBlock.setStatus(level, pos, MicroscopeBlock.WORKING);
			Chat.send(player, Msg.of("contagion.microscope.start_analysis", sample.source(), ContagionConfig.analysisTicks / 20), ChatFormatting.AQUA);
			Vanilla.playSound(level, x, y, z, "block.glass.place", SoundSource.BLOCKS, 0.8F, 1.4F);
			return;
		}

		if (main.is(ModItems.SERUM)) {
			SerumMix mix = main.get(ModComponents.SERUM);

			if (mix == null || !mix.sealed()) {
				Chat.send(player, Msg.of("contagion.microscope.seal_first"), ChatFormatting.YELLOW);
				return;
			}

			CultureData culture = off.is(ModItems.CULTURE) ? off.get(ModComponents.CULTURE) : null;

			if (culture == null) {
				Chat.send(player, Msg.of("contagion.microscope.need_culture"), ChatFormatting.YELLOW);
				return;
			}

			int state = culture.state(now);

			if (state != CultureData.COLONIZED) {
				Chat.send(player, Msg.of(switch (state) {
					case CultureData.GROWING -> "contagion.microscope.culture_growing";
					case CultureData.STERILE -> "contagion.microscope.culture_sterile";
					default -> "contagion.microscope.culture_dead";
				}), ChatFormatting.YELLOW);
				return;
			}

			main.shrink(1);
			off.shrink(1);
			BloodSample donor = new BloodSample(culture.source(), true, culture.load(), false, culture.day());
			Strains.setResearch(server, research.withPending(new PendingTest(dimension, pos.asLong(), now + ContagionConfig.cultureTicks,
					PendingTest.CULTURE_TEST, donor, mix.reagents(), false)));
			MicroscopeBlock.setStatus(level, pos, MicroscopeBlock.WORKING);
			Chat.send(player, Msg.of("contagion.microscope.start_culture", culture.source(), ContagionConfig.cultureTicks / 20), ChatFormatting.AQUA);
			Advancements.award(player, "petri");
			Vanilla.playSound(level, x, y, z, "item.bottle.empty", SoundSource.BLOCKS, 0.8F, 1.2F);
			return;
		}

		Chat.send(player, Msg.of("contagion.microscope.help"), ChatFormatting.GRAY);
	}

	private static Research finishAnalysis(MinecraftServer server, ServerPlayer player, PendingTest test, Research research) {
		BloodSample sample = test.sample();
		Strain strain = Strains.get(server);
		RandomSource random = player.getRandom();
		Msg logResult;

		header(player, Msg.of("contagion.microscope.analysis_header", sample.source(), sample.day()));
		Advancements.award(player, "first_analysis");

		boolean detected = sample.infected() && sample.load() >= 2 && strain != null;

		if (isDegraded(server, sample)) {
			Chat.send(player, Msg.of("contagion.microscope.degraded", MAX_SAMPLE_AGE), ChatFormatting.GRAY);
			logResult = Msg.of("contagion.log.result.too_old");
		} else if (!detected) {
			if (sample.immune() && strain != null) {
				Chat.send(player, Msg.of("contagion.microscope.antibodies"), ChatFormatting.GREEN);
				logResult = Msg.of("contagion.log.result.antibodies");
			} else {
				Chat.send(player, Msg.of("contagion.microscope.negative"), ChatFormatting.GREEN);
				logResult = Msg.of("contagion.log.result.negative");
			}

			Chat.send(player, Msg.of("contagion.microscope.negative_note"), ChatFormatting.DARK_GRAY);
		} else {
			Chat.send(player, Msg.of("contagion.microscope.positive", Disease.loadMsg(sample.load())), ChatFormatting.RED);
			logResult = Msg.of("contagion.log.result.positive", Disease.loadMsg(sample.load()));

			if (!research.knows("identified")) {
				research = research.withFinding("identified");
				Chat.send(player, Msg.of("contagion.microscope.identified", strain.nameMsg(), strain.profileMsg()), ChatFormatting.GOLD);
				Chat.send(player, Msg.of("contagion.microscope.identified2"), ChatFormatting.GOLD);
				Advancements.award(player, "identified");
			}

			// Each donor's blood can only teach one new thing per day; research takes real time.
			String donorKey = "used:" + sample.source() + ":" + sample.day();
			double chance = switch (sample.load()) {
				case 2 -> 0.25;
				case 3 -> 0.4;
				case 4 -> 0.55;
				default -> 0.7;
			};

			if (research.knows(donorKey)) {
				Chat.send(player, Msg.of("contagion.microscope.nothing_new_donor", sample.source()), ChatFormatting.GRAY);
			} else {
				String finding = random.nextDouble() < chance ? Lab.newFinding(strain, research, random) : null;
				Msg description = finding == null ? null : Lab.describeFinding(finding);

				if (finding != null && description != null) {
					research = research.withFinding(finding).withFinding(donorKey);
					Chat.send(player, Msg.of("contagion.microscope.insight", description), ChatFormatting.YELLOW);
					logResult = Msg.of("contagion.log.result.positive_insight", Disease.loadMsg(sample.load()));
				} else {
					Chat.send(player, Msg.of("contagion.microscope.no_insight"), ChatFormatting.GRAY);
				}
			}
		}

		Chat.send(player, Msg.of("contagion.microscope.recorded"), ChatFormatting.DARK_GRAY);
		return research.withLog(Msg.of("contagion.log.analysis", Lab.day(server), sample.source(), logResult));
	}

	private static Research finishCulture(MinecraftServer server, ServerPlayer player, PendingTest test, Research research) {
		BloodSample donor = test.sample();
		Strain strain = Strains.get(server);
		List<Ingredient> ingredients = Ingredient.parseAll(test.ingredients());
		Msg serum = Ingredient.listMsg(ingredients);
		Msg logResult;

		header(player, Msg.of("contagion.microscope.culture_header", serum));

		if (strain == null) {
			Chat.send(player, Msg.of("contagion.microscope.culture_failed"), ChatFormatting.GRAY);
			logResult = Msg.of("contagion.log.result.no_result");
		} else {
			int active = Treatment.activeAgents(strain, ingredients);
			int trace = Treatment.traceAgents(strain, ingredients);
			int toxic = Treatment.toxicAgents(strain, ingredients);

			Chat.send(player, Msg.of("contagion.culture.efficacy." + Math.min(3, active)),
					active >= 3 ? ChatFormatting.GREEN : active > 0 ? ChatFormatting.YELLOW : ChatFormatting.GRAY);

			// A right reagent in the wrong preparation leaves a hint, but no count.
			if (trace > 0 && active < 3) {
				Chat.send(player, Msg.of("contagion.culture.trace"), ChatFormatting.LIGHT_PURPLE);
			}

			Chat.send(player, Msg.of("contagion.culture.tissue." + Math.min(2, toxic)), toxic == 0 ? ChatFormatting.GREEN : ChatFormatting.RED);
			logResult = Msg.of(trace > 0 && active < 3 ? "contagion.log.result.culture_trace" : "contagion.log.result.culture",
					Msg.of("contagion.log.efficacy." + Math.min(3, active)), Msg.of("contagion.log.tissue." + Math.min(2, toxic)));
		}

		Chat.send(player, Msg.of("contagion.microscope.recorded"), ChatFormatting.DARK_GRAY);
		return research.withLog(Msg.of("contagion.log.culture", Lab.day(server), serum, donor.source(), logResult));
	}

	private static boolean isDegraded(MinecraftServer server, BloodSample sample) {
		return Lab.day(server) - sample.day() > MAX_SAMPLE_AGE;
	}

	public static boolean isFresh(MinecraftServer server, int drawnDay) {
		return Lab.day(server) - drawnDay <= MAX_SAMPLE_AGE;
	}

	private static void header(ServerPlayer player, Msg text) {
		Chat.send(player, Msg.of("contagion.header", text), ChatFormatting.AQUA, ChatFormatting.BOLD);
	}
}
