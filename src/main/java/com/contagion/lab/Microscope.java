package com.contagion.lab;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

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
import com.contagion.network.MicroscopePayload;
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
 * Using the block opens its screen ({@link MicroscopeMenu}): a slide slot, a serum slot and the eyepiece view.
 * A finished job is read ("examined") the next time someone looks through the eyepiece.
 */
public final class Microscope {
	public static final int MAX_SAMPLE_AGE = 3;
	public static final int BUTTON_START = 0;
	public static final int BUTTON_EXAMINE = 1;

	/** The last result of each microscope, so it can be looked at again until the next job starts. */
	/** Gold that is still readable on paper. */
	public static final int DARK_GOLD = 0x9A6400;

	private static final Map<String, MicroscopePayload> LAST_RESULT = new ConcurrentHashMap<>();

	private Microscope() {
	}

	private static String key(ServerLevel level, BlockPos pos) {
		return LabJobs.dimension(level) + "|" + pos.asLong();
	}

	public static void use(ServerLevel level, ServerPlayer player, BlockPos pos) {
		examine(level, player, pos);
		player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new MicroscopeMenu(id, inventory, ContainerLevelAccess.create(level, pos)),
				Msg.of("block.contagion.microscope").component()));
		sendView(level, player, pos);
		Vanilla.playSound(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, "block.glass.hit", SoundSource.BLOCKS, 0.5F, 1.6F);
	}

	/** Finishes a done job (if any). */
	public static void examine(ServerLevel level, ServerPlayer player, BlockPos pos) {
		MinecraftServer server = level.getServer();
		Research research = Strains.research(server);
		PendingTest pending = LabJobs.find(research, level, pos);

		if (pending == null || Lab.now(server) < pending.readyAt()) {
			return;
		}

		boolean culture = pending.kind() == PendingTest.CULTURE_TEST;
		Report report = new Report(culture ? MicroscopePayload.CULTURE : MicroscopePayload.ANALYSIS);
		Research updated = LabJobs.remove(research, level, pos);
		updated = culture ? finishCulture(server, player, pending, updated, report) : finishAnalysis(server, player, pending, updated, report);
		Strains.setResearch(server, updated);
		LAST_RESULT.put(key(level, pos), report.payload());
		MicroscopeBlock.setStatus(level, pos, MicroscopeBlock.IDLE);
		Vanilla.playSound(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, "item.book.page_turn", SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	public static void forget(ServerLevel level, BlockPos pos) {
		LAST_RESULT.remove(key(level, pos));
	}

	/** Start button: a blood sample alone is analyzed; a grown culture plus a sealed serum is a culture test. */
	public static void start(ServerLevel level, ServerPlayer player, BlockPos pos, Container slots) {
		MinecraftServer server = level.getServer();
		long now = Lab.now(server);
		Research research = Strains.research(server);

		if (LabJobs.find(research, level, pos) != null) {
			return;
		}

		ItemStack slide = slots.getItem(MicroscopeMenu.SLIDE_SLOT);
		ItemStack serumStack = slots.getItem(MicroscopeMenu.SERUM_SLOT);
		String dimension = LabJobs.dimension(level);
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.5;
		double z = pos.getZ() + 0.5;

		if (slide.is(ModItems.BLOOD_SAMPLE)) {
			BloodSample sample = slide.get(ModComponents.BLOOD_SAMPLE);

			if (sample == null) {
				return;
			}

			slide.shrink(1);
			slots.setChanged();
			Strains.setResearch(server, research.withPending(new PendingTest(dimension, pos.asLong(), now + ContagionConfig.analysisTicks,
					PendingTest.ANALYSIS, sample, List.of(), false)));
			forget(level, pos);
			MicroscopeBlock.setStatus(level, pos, MicroscopeBlock.WORKING);
			Chat.send(player, Msg.of("contagion.microscope.start_analysis", sample.source(), ContagionConfig.analysisTicks / 20), ChatFormatting.AQUA);
			Vanilla.playSound(level, x, y, z, "block.glass.place", SoundSource.BLOCKS, 0.8F, 1.4F);
			broadcast(level, pos);
			return;
		}

		if (!slide.is(ModItems.CULTURE)) {
			error(level, player, pos, Msg.of("contagion.microscope.ui.empty"));
			return;
		}

		CultureData culture = slide.get(ModComponents.CULTURE);

		if (culture == null) {
			return;
		}

		int state = culture.state(now);

		if (state != CultureData.COLONIZED) {
			error(level, player, pos, Msg.of(switch (state) {
				case CultureData.GROWING -> "contagion.microscope.culture_growing";
				case CultureData.STERILE -> "contagion.microscope.culture_sterile";
				default -> "contagion.microscope.culture_dead";
			}));
			return;
		}

		SerumMix mix = serumStack.is(ModItems.SERUM) ? serumStack.get(ModComponents.SERUM) : null;

		if (mix == null) {
			error(level, player, pos, Msg.of("contagion.microscope.ui.need_serum"));
			return;
		}

		if (!mix.sealed()) {
			error(level, player, pos, Msg.of("contagion.microscope.seal_first"));
			return;
		}

		slide.shrink(1);
		serumStack.shrink(1);
		slots.setChanged();
		BloodSample donor = new BloodSample(culture.source(), true, culture.load(), false, culture.day());
		Strains.setResearch(server, research.withPending(new PendingTest(dimension, pos.asLong(), now + ContagionConfig.cultureTicks,
				PendingTest.CULTURE_TEST, donor, mix.reagents(), false)));
		forget(level, pos);
		MicroscopeBlock.setStatus(level, pos, MicroscopeBlock.WORKING);
		Chat.send(player, Msg.of("contagion.microscope.start_culture", culture.source(), ContagionConfig.cultureTicks / 20), ChatFormatting.AQUA);
		Advancements.award(player, "petri");
		Vanilla.playSound(level, x, y, z, "item.bottle.empty", SoundSource.BLOCKS, 0.8F, 1.2F);
		broadcast(level, pos);
	}

	private static void error(ServerLevel level, ServerPlayer player, BlockPos pos, Msg message) {
		MicroscopePayload view = view(level, pos);
		ServerPlayNetworking.send(player, new MicroscopePayload(view.mode(), view.kind(), view.remaining(), view.total(), view.load(),
				view.flags() | MicroscopePayload.ERROR, view.active(), view.toxic(), List.of(message.styled(ChatFormatting.DARK_RED))));
	}

	public static MicroscopePayload view(ServerLevel level, BlockPos pos) {
		MinecraftServer server = level.getServer();
		PendingTest pending = LabJobs.find(Strains.research(server), level, pos);

		if (pending != null) {
			boolean culture = pending.kind() == PendingTest.CULTURE_TEST;
			long total = culture ? ContagionConfig.cultureTicks : ContagionConfig.analysisTicks;
			long remaining = Math.max(0, pending.readyAt() - Lab.now(server));
			Msg status = culture
					? Msg.of("contagion.microscope.ui.running_culture", Ingredient.listMsg(Ingredient.parseAll(pending.ingredients())), pending.sample().source())
					: Msg.of("contagion.microscope.ui.running_analysis", pending.sample().source());
			return new MicroscopePayload(remaining > 0 ? MicroscopePayload.RUNNING : MicroscopePayload.READY,
					culture ? MicroscopePayload.CULTURE : MicroscopePayload.ANALYSIS, (int) Math.min(Integer.MAX_VALUE, remaining),
					(int) Math.min(Integer.MAX_VALUE, total), 0, 0, 0, 0, List.of(status.styled(ChatFormatting.DARK_AQUA)));
		}

		MicroscopePayload last = LAST_RESULT.get(key(level, pos));
		return last != null ? last : MicroscopePayload.EMPTY;
	}

	public static void sendView(ServerLevel level, ServerPlayer player, BlockPos pos) {
		ServerPlayNetworking.send(player, view(level, pos));
	}

	/** Updates everyone looking through this microscope. */
	public static void broadcast(ServerLevel level, BlockPos pos) {
		for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
			if (player.containerMenu instanceof MicroscopeMenu menu && menu.isAt(level, pos)) {
				sendView(level, player, pos);
			}
		}
	}

	private static Research finishAnalysis(MinecraftServer server, ServerPlayer player, PendingTest test, Research research, Report report) {
		BloodSample sample = test.sample();
		Strain strain = Strains.get(server);
		RandomSource random = player.getRandom();
		Msg logResult;

		report.header(player, Msg.of("contagion.microscope.analysis_header", sample.source(), sample.day()));
		Advancements.award(player, "first_analysis");

		boolean detected = sample.infected() && sample.load() >= 2 && strain != null;

		if (isDegraded(server, sample)) {
			report.flags |= MicroscopePayload.DEGRADED;
			report.say(player, Msg.of("contagion.microscope.degraded", MAX_SAMPLE_AGE), ChatFormatting.GRAY);
			logResult = Msg.of("contagion.log.result.too_old");
		} else if (!detected) {
			if (sample.immune() && strain != null) {
				report.flags |= MicroscopePayload.ANTIBODIES;
				report.say(player, Msg.of("contagion.microscope.antibodies"), ChatFormatting.GREEN);
				logResult = Msg.of("contagion.log.result.antibodies");
			} else {
				report.say(player, Msg.of("contagion.microscope.negative"), ChatFormatting.GREEN);
				logResult = Msg.of("contagion.log.result.negative");
			}

			report.say(player, Msg.of("contagion.microscope.negative_note"), ChatFormatting.DARK_GRAY);
		} else {
			report.flags |= MicroscopePayload.POSITIVE;
			report.load = sample.load();
			report.say(player, Msg.of("contagion.microscope.positive", Disease.loadMsg(sample.load())), ChatFormatting.RED);
			logResult = Msg.of("contagion.log.result.positive", Disease.loadMsg(sample.load()));

			if (!research.knows("identified")) {
				research = research.withFinding("identified");
				report.say(player, Msg.of("contagion.microscope.identified", strain.nameMsg(), strain.profileMsg()), ChatFormatting.GOLD);
				report.say(player, Msg.of("contagion.microscope.identified2"), ChatFormatting.GOLD);
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
				report.say(player, Msg.of("contagion.microscope.nothing_new_donor", sample.source()), ChatFormatting.GRAY);
			} else {
				String finding = random.nextDouble() < chance ? Lab.newFinding(strain, research, random) : null;
				Msg description = finding == null ? null : Lab.describeFinding(finding);

				if (finding != null && description != null) {
					research = research.withFinding(finding).withFinding(donorKey);
					report.flags |= MicroscopePayload.INSIGHT;
					report.say(player, Msg.of("contagion.microscope.insight", description), ChatFormatting.YELLOW);
					logResult = Msg.of("contagion.log.result.positive_insight", Disease.loadMsg(sample.load()));
				} else {
					report.say(player, Msg.of("contagion.microscope.no_insight"), ChatFormatting.GRAY);
				}
			}
		}

		report.say(player, Msg.of("contagion.microscope.recorded"), ChatFormatting.DARK_GRAY);
		return research.withLog(Msg.of("contagion.log.analysis", Lab.day(server), sample.source(), logResult));
	}

	private static Research finishCulture(MinecraftServer server, ServerPlayer player, PendingTest test, Research research, Report report) {
		BloodSample donor = test.sample();
		Strain strain = Strains.get(server);
		List<Ingredient> ingredients = Ingredient.parseAll(test.ingredients());
		Msg serum = Ingredient.listMsg(ingredients);
		Msg logResult;

		report.header(player, Msg.of("contagion.microscope.culture_header", serum));
		report.load = Math.max(2, donor.load());

		if (strain == null) {
			report.flags |= MicroscopePayload.DEGRADED;
			report.say(player, Msg.of("contagion.microscope.culture_failed"), ChatFormatting.GRAY);
			logResult = Msg.of("contagion.log.result.no_result");
		} else {
			int active = Treatment.activeAgents(strain, ingredients);
			int trace = Treatment.traceAgents(strain, ingredients);
			int toxic = Treatment.toxicAgents(strain, ingredients);
			report.active = Math.min(3, active);
			report.toxic = Math.min(2, toxic);
			report.flags |= MicroscopePayload.POSITIVE;

			report.say(player, Msg.of("contagion.culture.efficacy." + Math.min(3, active)),
					active >= 3 ? ChatFormatting.GREEN : active > 0 ? ChatFormatting.YELLOW : ChatFormatting.GRAY);

			// A right reagent in the wrong preparation leaves a hint, but no count.
			if (trace > 0 && active < 3) {
				report.flags |= MicroscopePayload.TRACE;
				report.say(player, Msg.of("contagion.culture.trace"), ChatFormatting.LIGHT_PURPLE);
			}

			report.say(player, Msg.of("contagion.culture.tissue." + Math.min(2, toxic)), toxic == 0 ? ChatFormatting.GREEN : ChatFormatting.RED);
			logResult = Msg.of(trace > 0 && active < 3 ? "contagion.log.result.culture_trace" : "contagion.log.result.culture",
					Msg.of("contagion.log.efficacy." + Math.min(3, active)), Msg.of("contagion.log.tissue." + Math.min(2, toxic)));
		}

		report.say(player, Msg.of("contagion.microscope.recorded"), ChatFormatting.DARK_GRAY);
		return research.withLog(Msg.of("contagion.log.culture", Lab.day(server), serum, donor.source(), logResult));
	}

	private static boolean isDegraded(MinecraftServer server, BloodSample sample) {
		return Lab.day(server) - sample.day() > MAX_SAMPLE_AGE;
	}

	public static boolean isFresh(MinecraftServer server, int drawnDay) {
		return Lab.day(server) - drawnDay <= MAX_SAMPLE_AGE;
	}

	/** Collects a result for both chat and the microscope screen. */
	private static final class Report {
		private final int kind;
		private final List<Component> lines = new ArrayList<>();
		int load;
		int flags;
		int active;
		int toxic;

		Report(int kind) {
			this.kind = kind;
		}

		void header(ServerPlayer player, Msg text) {
			Chat.send(player, Msg.of("contagion.header", text), ChatFormatting.AQUA, ChatFormatting.BOLD);
			this.lines.add(text.styled(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
		}

		/** Chat gets the bright color; the result panel (paper) gets a darker one that reads well on it. */
		void say(ServerPlayer player, Msg text, ChatFormatting chatColor) {
			Chat.send(player, text, chatColor);
			MutableComponent paper = text.component();
			this.lines.add(switch (chatColor) {
				case GREEN -> paper.withStyle(ChatFormatting.DARK_GREEN);
				case LIGHT_PURPLE -> paper.withStyle(ChatFormatting.DARK_PURPLE);
				case AQUA -> paper.withStyle(ChatFormatting.DARK_AQUA);
				case RED -> paper.withStyle(ChatFormatting.DARK_RED);
				case GOLD, YELLOW -> paper.withColor(DARK_GOLD);
				case GRAY -> paper.withStyle(ChatFormatting.DARK_GRAY);
				default -> paper.withStyle(chatColor);
			});
		}

		MicroscopePayload payload() {
			return new MicroscopePayload(MicroscopePayload.RESULT, this.kind, 0, 0, this.load, this.flags, this.active, this.toxic, List.copyOf(this.lines));
		}
	}
}
