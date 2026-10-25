package com.contagion.disease;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import com.contagion.ContagionMod;

/**
 * Looks up vanilla effects, sounds and particles by their stable registry ids, so the mod does not depend on
 * Java constant names that Mojang renames between versions.
 */
public final class Vanilla {
	public static final ResourceKey<DamageType> INFECTION_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ContagionMod.id("infection"));

	private static final Map<String, Holder<MobEffect>> EFFECTS = new HashMap<>();

	private Vanilla() {
	}

	public static @Nullable Holder<MobEffect> effect(String id) {
		return EFFECTS.computeIfAbsent(id, key -> {
			MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(Identifier.withDefaultNamespace(key));
			return effect == null ? null : BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
		});
	}

	/** Applies an effect without particles (symptoms should not glow like potions). */
	public static void addEffect(LivingEntity entity, String id, int ticks, int amplifier) {
		Holder<MobEffect> effect = effect(id);

		if (effect != null) {
			entity.addEffect(new MobEffectInstance(effect, ticks, amplifier, false, false, true));
		}
	}

	public static void playSound(ServerLevel level, double x, double y, double z, String id, SoundSource source, float volume, float pitch) {
		SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getValue(Identifier.withDefaultNamespace(id));

		if (sound != null) {
			level.playSound(null, x, y, z, sound, source, volume, pitch);
		}
	}

	public static void playSound(ServerLevel level, LivingEntity at, String id, float volume, float pitch) {
		playSound(level, at.getX(), at.getY(), at.getZ(), id, SoundSource.PLAYERS, volume, pitch);
	}

	/** A sound only this player hears (hallucinations, heartbeat). */
	public static void privateSound(ServerPlayer player, String id, double x, double y, double z, float volume, float pitch) {
		SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getValue(Identifier.withDefaultNamespace(id));

		if (sound != null) {
			player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.AMBIENT,
					x, y, z, volume, pitch, player.getRandom().nextLong()));
		}
	}

	/** Big centered title + subtitle on one player's screen. */
	public static void title(ServerPlayer player, Component title, Component subtitle) {
		player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 30));
		player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
		player.connection.send(new ClientboundSetTitleTextPacket(title));
	}

	public static @Nullable ParticleOptions particle(String id) {
		ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.getValue(Identifier.withDefaultNamespace(id));
		return type instanceof SimpleParticleType simple ? simple : null;
	}

	public static void particles(ServerLevel level, String id, double x, double y, double z, int count, double spread, double speed) {
		ParticleOptions particle = particle(id);

		if (particle != null) {
			level.sendParticles(particle, x, y, z, count, spread, spread, spread, speed);
		}
	}

	public static DamageSource infectionDamage(ServerLevel level) {
		return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(INFECTION_DAMAGE));
	}
}
