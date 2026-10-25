package com.contagion.disease;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import com.contagion.ContagionConfig;
import com.contagion.ContagionMod;
import com.contagion.data.Infection;
import com.contagion.data.Strain;
import com.contagion.text.Chat;
import com.contagion.text.Msg;

/**
 * Applies symptoms once per second. Symptoms are random episodes whose frequency depends on the stage and on
 * the strain's profile, so two worlds' diseases feel different and are easy to mistake for something else early on.
 */
public final class Symptoms {
	public static final Identifier HEALTH_PENALTY_ID = ContagionMod.id("disease_health_penalty");
	/** Last penalty applied per entity (the modifier is transient and re-applied after relogging). */
	private static final Map<UUID, Integer> APPLIED_PENALTY = new HashMap<>();

	// Per-second chances indexed by stage: MILD, MODERATE, SEVERE, TERMINAL.
	private static final double[] COUGH = {1 / 90.0, 1 / 45.0, 1 / 25.0, 1 / 20.0};
	private static final double[] FEVER = {1 / 240.0, 1 / 90.0, 1 / 60.0, 1 / 45.0};
	private static final double[] FATIGUE = {1 / 300.0, 1 / 120.0, 1 / 60.0, 1 / 40.0};
	private static final double[] HUNGER = {1 / 200.0, 1 / 120.0, 1 / 80.0, 1 / 60.0};
	private static final double[] BREATHLESS = {0, 1 / 300.0, 1 / 120.0, 1 / 80.0};
	private static final double[] VOMIT = {0, 1 / 400.0, 1 / 150.0, 1 / 100.0};
	private static final double[] DIZZY = {0, 1 / 300.0, 1 / 120.0, 1 / 80.0};
	private static final double[] DARKNESS = {0, 1 / 400.0, 1 / 150.0, 1 / 100.0};
	private static final double[] HALLUCINATION = {0, 1 / 600.0, 1 / 200.0, 1 / 120.0};
	private static final double[] FAINT = {0, 0, 1 / 400.0, 1 / 200.0};
	private static final double[] BLEED = {0, 1 / 300.0, 1 / 100.0, 1 / 60.0};
	private static final double[] CHILLS = {1 / 400.0, 1 / 200.0, 1 / 120.0, 1 / 90.0};
	private static final Map<UUID, Long> CHILLS_UNTIL = new HashMap<>();

	private static final String[] HALLUCINATION_SOUNDS = {
			"entity.creeper.primed", "entity.zombie.ambient", "entity.skeleton.ambient", "entity.spider.ambient",
			"block.wooden_door.open", "entity.enderman.stare", "ambient.cave", "entity.player.hurt"
	};

	private Symptoms() {
	}

	public static void tickPlayer(ServerLevel level, ServerPlayer player, Infection infection, Strain strain) {
		Disease.Stage stage = Disease.stage(infection);

		if (stage == Disease.Stage.HEALTHY || stage == Disease.Stage.INCUBATION) {
			clearHealthPenalty(player);
			return;
		}

		if (player.isSpectator()) {
			return;
		}

		RandomSource random = player.getRandom();
		int s = stage.ordinal() - Disease.Stage.MILD.ordinal();
		boolean remission = infection.remission() > 0;
		double mul = remission ? 0.15 : 1.0;
		int profile = strain.profile();
		boolean resp = profile == Strain.RESPIRATORY;
		boolean neuro = profile == Strain.NEUROLOGICAL;
		boolean hemo = profile == Strain.HEMORRHAGIC;

		if (roll(random, COUGH[s] * mul * (resp ? 2.0 : 0.7))) {
			cough(level, player, strain);
		}

		if (roll(random, FEVER[s] * mul)) {
			Vanilla.addEffect(player, "slowness", 300, 0);

			if (s >= 1) {
				Vanilla.addEffect(player, "weakness", 300, 0);
			}

			Vanilla.particles(level, "falling_water", player.getX(), player.getEyeY(), player.getZ(), 6, 0.3, 0.0);
			say(player, s >= 2 ? "fever2" : "fever1");
		}

		if (roll(random, CHILLS[s] * mul)) {
			CHILLS_UNTIL.put(player.getUUID(), level.getGameTime() + 200 + random.nextInt(200));
			say(player, "chills");
		}

		Long chills = CHILLS_UNTIL.get(player.getUUID());

		if (chills != null) {
			if (level.getGameTime() < chills && !remission) {
				// Frost overlay without reaching the freeze-damage threshold (140).
				player.setTicksFrozen(110);
			} else {
				CHILLS_UNTIL.remove(player.getUUID());
			}
		}

		// A pounding heartbeat once things get serious.
		if ((stage == Disease.Stage.TERMINAL || (s >= 2 && player.getHealth() <= 8.0F)) && !remission && random.nextInt(3) == 0) {
			Vanilla.privateSound(player, "entity.warden.heartbeat", player.getX(), player.getY(), player.getZ(), 1.0F, stage == Disease.Stage.TERMINAL ? 1.1F : 0.9F);
		}

		if (roll(random, FATIGUE[s] * mul)) {
			Vanilla.addEffect(player, "mining_fatigue", 400, 0);
			player.causeFoodExhaustion(1.5F);
			say(player, s >= 2 ? "fatigue2" : "fatigue1");
		}

		if (roll(random, HUNGER[s] * mul)) {
			Vanilla.addEffect(player, "hunger", 200, s >= 2 ? 1 : 0);
		}

		if (roll(random, BREATHLESS[s] * mul * (resp ? 2.0 : 0.5))) {
			Vanilla.addEffect(player, "slowness", 200, 1);
			player.causeFoodExhaustion(3.0F);
			say(player, "breathless");
		}

		if (roll(random, VOMIT[s] * mul * (resp ? 0.7 : 1.3))) {
			vomit(level, player);
		}

		if (roll(random, DIZZY[s] * mul * (neuro ? 2.0 : 0.6))) {
			Vanilla.addEffect(player, "nausea", 200, 0);
			say(player, "dizzy");
		}

		if (roll(random, DARKNESS[s] * mul * (neuro ? 2.0 : 0.3))) {
			Vanilla.addEffect(player, "darkness", 160, 0);
			say(player, "darkness");
		}

		if (roll(random, HALLUCINATION[s] * mul * (neuro ? 3.0 : 0.0))) {
			hallucinate(level, player, random);
		}

		if (roll(random, FAINT[s] * mul * (neuro ? 1.5 : 1.0))) {
			Vanilla.addEffect(player, "blindness", 80, 0);
			Vanilla.addEffect(player, "slowness", 80, 3);
			say(player, "faint");
		}

		if (roll(random, BLEED[s] * mul * (hemo ? 3.0 : 0.15))) {
			bleed(level, player, stage);
		}

		// Constant weakness once the disease is severe.
		if (s >= 2 && !remission) {
			Vanilla.addEffect(player, "weakness", 60, 0);
		}

		applyHealthPenalty(player, healthPenalty(infection));

		// Terminal: once the body is nearly spent, the disease finishes the job.
		if (stage == Disease.Stage.TERMINAL && !remission && player.getMaxHealth() <= 2.0F && random.nextInt(20) == 0) {
			player.hurtServer(level, Vanilla.infectionDamage(level), 1.0F);
		}
	}

	/** Simplified symptoms for infected mobs (villagers). */
	public static void tickMob(ServerLevel level, LivingEntity entity, Infection infection, Strain strain) {
		Disease.Stage stage = Disease.stage(infection);

		if (stage == Disease.Stage.HEALTHY || stage == Disease.Stage.INCUBATION) {
			return;
		}

		RandomSource random = entity.getRandom();
		boolean remission = infection.remission() > 0;

		if (random.nextInt(remission ? 200 : 40) == 0) {
			cough(level, entity, strain);
		}

		if (stage.ordinal() >= Disease.Stage.SEVERE.ordinal() && !remission) {
			Vanilla.addEffect(entity, "slowness", 60, 0);
		}

		if (stage == Disease.Stage.TERMINAL && !remission && random.nextInt(10) == 0) {
			entity.hurtServer(level, Vanilla.infectionDamage(level), 2.0F);
		}
	}

	public static void onStageReached(ServerPlayer player, Disease.Stage stage, Strain strain) {
		String key = switch (stage) {
			case MILD -> switch (strain.profile()) {
				case Strain.NEUROLOGICAL -> "mild_neurological";
				case Strain.HEMORRHAGIC -> "mild_hemorrhagic";
				default -> "mild_respiratory";
			};
			case MODERATE -> "moderate";
			case SEVERE -> "severe";
			case TERMINAL -> "terminal";
			default -> null;
		};

		if (key != null) {
			Chat.send(player, Msg.of("contagion.stage." + key), ChatFormatting.GRAY, ChatFormatting.ITALIC);
		}
	}

	/** Max-health loss: none while mild, then growing; in the terminal stage one more heart is lost every step. */
	public static int healthPenalty(Infection infection) {
		int penalty = switch (Disease.stage(infection)) {
			case MODERATE -> 2;
			case SEVERE -> 6;
			case TERMINAL -> {
				long into = infection.progress() - Disease.terminalStart();
				yield (int) Math.min(19, 8 + 2 * (into / ContagionConfig.terminalStepTicks));
			}
			default -> 0;
		};

		return infection.remission() > 0 ? penalty / 2 : penalty;
	}

	public static void applyHealthPenalty(LivingEntity entity, int amount) {
		AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);

		if (maxHealth == null) {
			return;
		}

		boolean present = maxHealth.hasModifier(HEALTH_PENALTY_ID);
		UUID uuid = entity.getUUID();

		if (amount <= 0) {
			if (present) {
				maxHealth.removeModifier(HEALTH_PENALTY_ID);
			}

			APPLIED_PENALTY.remove(uuid);
			return;
		}

		Integer applied = APPLIED_PENALTY.get(uuid);

		if (present && applied != null && applied == amount) {
			return;
		}

		if (present) {
			maxHealth.removeModifier(HEALTH_PENALTY_ID);
		}

		maxHealth.addTransientModifier(new AttributeModifier(HEALTH_PENALTY_ID, -amount, AttributeModifier.Operation.ADD_VALUE));
		APPLIED_PENALTY.put(uuid, amount);

		if (entity.getHealth() > entity.getMaxHealth()) {
			entity.setHealth(entity.getMaxHealth());
		}
	}

	public static void clearHealthPenalty(LivingEntity entity) {
		applyHealthPenalty(entity, 0);
	}

	public static void cough(ServerLevel level, LivingEntity entity, Strain strain) {
		RandomSource random = entity.getRandom();
		Vec3 look = entity.getLookAngle();
		double x = entity.getX() + look.x * 0.6;
		double y = entity.getEyeY() - 0.1;
		double z = entity.getZ() + look.z * 0.6;
		boolean sneeze = random.nextInt(4) == 0;

		Vanilla.particles(level, "sneeze", x, y, z, sneeze ? 10 : 5, 0.15, 0.05);
		Vanilla.playSound(level, entity.getX(), entity.getY(), entity.getZ(),
				sneeze ? "entity.panda.sneeze" : "entity.llama.spit", SoundSource.PLAYERS, 0.8F, 0.6F + random.nextFloat() * 0.3F);

		if (entity instanceof ServerPlayer player) {
			say(player, sneeze ? "sneeze" : "cough");
		}

		Transmission.coughBurst(level, entity, strain);
	}

	private static void vomit(ServerLevel level, ServerPlayer player) {
		Vanilla.addEffect(player, "nausea", 160, 0);
		Vanilla.addEffect(player, "hunger", 100, 2);
		player.causeFoodExhaustion(12.0F);
		Vec3 look = player.getLookAngle();
		Vanilla.particles(level, "item_slime", player.getX() + look.x * 0.5, player.getEyeY() - 0.3, player.getZ() + look.z * 0.5, 20, 0.2, 0.1);
		Vanilla.playSound(level, player, "entity.player.burp", 1.0F, 0.5F);
		say(player, "vomit");
	}

	private static void hallucinate(ServerLevel level, ServerPlayer player, RandomSource random) {
		Vec3 look = player.getLookAngle();
		// Sounds come from behind the player.
		double x = player.getX() - look.x * 3.0;
		double z = player.getZ() - look.z * 3.0;
		String sound = HALLUCINATION_SOUNDS[random.nextInt(HALLUCINATION_SOUNDS.length)];
		// Only the patient hears it.
		Vanilla.privateSound(player, sound, x, player.getY(), z, 0.7F, 0.8F + random.nextFloat() * 0.3F);

		if (random.nextInt(3) == 0) {
			say(player, "hallucination");
		}
	}

	private static void bleed(ServerLevel level, ServerPlayer player, Disease.Stage stage) {
		Vanilla.particles(level, "damage_indicator", player.getX(), player.getEyeY() - 0.2, player.getZ(), 4, 0.2, 0.05);
		Vanilla.particles(level, "dripping_lava", player.getX(), player.getEyeY() - 0.2, player.getZ(), 3, 0.1, 0.0);

		// Before the terminal stage bleeding cannot take the last hearts.
		if (stage == Disease.Stage.TERMINAL || player.getHealth() > 4.0F) {
			player.hurtServer(level, Vanilla.infectionDamage(level), 1.0F);
		}

		say(player, stage.ordinal() >= Disease.Stage.SEVERE.ordinal() ? "bleed2" : "bleed1");
	}

	private static boolean roll(RandomSource random, double chance) {
		return chance > 0 && random.nextDouble() < chance;
	}

	/** Action bar message, so symptoms don't flood chat. */
	public static void say(ServerPlayer player, String symptom) {
		Chat.bar(player, Msg.of("contagion.symptom." + symptom), ChatFormatting.GRAY, ChatFormatting.ITALIC);
	}
}
