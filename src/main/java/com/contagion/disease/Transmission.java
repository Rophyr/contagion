package com.contagion.disease;

import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import com.contagion.ContagionConfig;
import com.contagion.data.Infection;
import com.contagion.data.Strain;
import com.contagion.registry.ModItems;

/**
 * How the pathogen spreads:
 * <ul>
 *     <li>Airborne: standing close to a contagious host, worse the sicker they are (masks help a lot).</li>
 *     <li>Coughing/sneezing: a burst aimed at whoever is in front of the host.</li>
 *     <li>Contact: getting hit by an infected host, or hitting one (worst bare-handed).</li>
 *     <li>Food: eating meat from an infected animal (see {@link TaintedFoodItem}).</li>
 * </ul>
 * Hosts are contagious from halfway through incubation, before they show any symptoms.
 */
public final class Transmission {
	private static final double AIRBORNE_RADIUS = 4.0;
	// Roughly: 5 minutes right next to a mildly sick, coughing player without masks is a coin flip.
	private static final double AIRBORNE_BASE = 0.0015;
	private static final double COUGH_RADIUS = 5.0;
	private static final double COUGH_BASE = 0.04;

	private Transmission() {
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((victim, source, baseDamage, damage, blocked) -> {
			if (blocked || !(victim.level() instanceof ServerLevel level) || !(source.getEntity() instanceof LivingEntity attacker) || attacker == victim) {
				return;
			}

			Strain strain = Strains.get(level.getServer());

			if (strain == null) {
				return;
			}

			RandomSource random = victim.getRandom();
			double contact = strain.contact() * ContagionConfig.contactMultiplier;

			// Bitten/clawed/hit by an infected host.
			if (Disease.isContagious(Disease.get(attacker)) && Disease.canCatch(victim) && random.nextDouble() < 0.12 * contact) {
				Disease.infect(level.getServer(), victim, false);
			}

			// Hitting an infected host (animal, villager or player): bare-handed is much riskier than with a weapon.
			if (attacker instanceof Player && Disease.isContagious(Disease.get(victim)) && Disease.canCatch(attacker)
					&& random.nextDouble() < (attacker.getMainHandItem().isEmpty() ? 0.10 : 0.03) * contact) {
				Disease.infect(level.getServer(), attacker, false);
			}
		});
	}

	public static void tickSource(ServerLevel level, LivingEntity source, Infection infection, Strain strain) {
		if (!Disease.isContagious(infection)) {
			return;
		}

		double base = AIRBORNE_BASE * strain.airborne() * ContagionConfig.airborneMultiplier * stageFactor(infection) * maskFactor(source, 0.3);

		// Animals shed less than people, but herds stand close together.
		if (WorldInfection.isLivestock(source)) {
			base *= 0.5;
		}

		if (base <= 0) {
			return;
		}

		RandomSource random = source.getRandom();
		MinecraftServer server = level.getServer();
		List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, source.getBoundingBox().inflate(AIRBORNE_RADIUS),
				target -> target != source && Disease.canCatch(target));

		for (LivingEntity target : nearby) {
			double distance = Math.sqrt(target.distanceToSqr(source));
			double falloff = distance < 2.0 ? 1.5 : distance < 3.0 ? 1.0 : 0.5;

			if (random.nextDouble() < base * falloff * maskFactor(target, 0.4)) {
				Disease.infect(server, target, false);
			}
		}
	}

	/** A cough sprays droplets at whoever is in front of the host. */
	public static void coughBurst(ServerLevel level, LivingEntity source, Strain strain) {
		Infection infection = Disease.get(source);

		if (!Disease.isContagious(infection)) {
			return;
		}

		double base = COUGH_BASE * strain.airborne() * ContagionConfig.airborneMultiplier * maskFactor(source, 0.25);
		RandomSource random = source.getRandom();
		MinecraftServer server = level.getServer();
		var look = source.getLookAngle();
		List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, source.getBoundingBox().inflate(COUGH_RADIUS),
				target -> target != source && Disease.canCatch(target));

		for (LivingEntity target : nearby) {
			double dx = target.getX() - source.getX();
			double dz = target.getZ() - source.getZ();
			double length = Math.sqrt(dx * dx + dz * dz);
			double facing = length < 0.01 ? 1.0 : (dx * look.x + dz * look.z) / length;
			double directional = facing > 0.3 ? 1.0 : 0.25;

			if (random.nextDouble() < base * directional * maskFactor(target, 0.4)) {
				Disease.infect(server, target, false);
			}
		}
	}

	private static double stageFactor(Infection infection) {
		double factor = switch (Disease.stage(infection)) {
			case INCUBATION -> 0.5;
			case MILD -> 1.0;
			case MODERATE -> 1.5;
			case SEVERE -> 2.0;
			case TERMINAL -> 2.5;
			default -> 0.0;
		};

		return infection.remission() > 0 ? factor * 0.4 : factor;
	}

	private static double maskFactor(LivingEntity entity, double masked) {
		return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.FACE_MASK) ? masked : 1.0;
	}
}
