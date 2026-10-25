package com.contagion.disease;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.contagion.ContagionConfig;
import com.contagion.data.Infection;
import com.contagion.data.Strain;
import com.contagion.lab.Lab;
import com.contagion.lab.LabJobs;
import com.contagion.registry.ModAttachments;
import com.contagion.registry.ModItems;
import com.contagion.text.Chat;
import com.contagion.text.Msg;

/**
 * Drives the disease: once per second every infected entity progresses, shows symptoms and spreads the pathogen.
 */
public final class DiseaseTicker {
	private static final int INTERVAL = 20;
	private static final Map<UUID, Long> SLEEP_START = new HashMap<>();
	private static int counter;

	private DiseaseTicker() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(DiseaseTicker::onServerTick);

		EntitySleepEvents.START_SLEEPING.register((entity, pos) -> {
			if (entity instanceof ServerPlayer player) {
				SLEEP_START.put(player.getUUID(), player.level().getGameTime());
			}
		});

		EntitySleepEvents.STOP_SLEEPING.register((entity, pos) -> {
			if (!(entity instanceof ServerPlayer player)) {
				return;
			}

			Long start = SLEEP_START.remove(player.getUUID());

			// A full sleep (the night was slept through) means real time passed for the body too.
			if (ContagionConfig.sleepAdvancesDisease && start != null && player.level().getGameTime() - start >= 100) {
				Infection infection = Disease.get(player);

				if (infection != null && infection.infected()) {
					long skip = ContagionConfig.sleepSkipTicks;
					long remission = infection.remission();
					long fromRemission = Math.min(remission, skip);
					player.setAttached(ModAttachments.INFECTION, infection.withProgress(infection.progress() + skip - fromRemission, remission - fromRemission));
				}
			}
		});

		// Immunity survives death/respawn (non-hardcore); the disease itself does not.
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			Infection infection = Disease.get(oldPlayer);

			if (infection != null && (alive || infection.immune())) {
				newPlayer.setAttached(ModAttachments.INFECTION, alive ? infection : Infection.immuneHost());
			}
		});
	}

	private static void onServerTick(MinecraftServer server) {
		if (++counter % INTERVAL != 0) {
			return;
		}

		Strain strain = Strains.get(server);

		if (strain != null) {
			for (ServerLevel level : server.getAllLevels()) {
				List<LivingEntity> infected = new ArrayList<>();

				for (Entity entity : level.getAllEntities()) {
					if (entity instanceof LivingEntity living && living.isAlive() && Disease.isInfected(living)) {
						infected.add(living);
					}
				}

				for (LivingEntity living : infected) {
					if (living.isAlive()) {
						tickEntity(level, living, strain);
					}
				}
			}
		}

		// Remove leftover health penalties (e.g. after a cure or an admin command).
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!Disease.isInfected(player)) {
				Symptoms.clearHealthPenalty(player);
			}
		}

		LabJobs.tick(server);

		// Petri dishes in inventories update their look as they grow.
		long now = Lab.now(server);

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Inventory inventory = player.getInventory();

			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				ItemStack stack = inventory.getItem(slot);

				if (stack.is(ModItems.CULTURE)) {
					Lab.refreshCulture(stack, now);
				}
			}
		}
	}

	private static void tickEntity(ServerLevel level, LivingEntity entity, Strain strain) {
		Infection infection = Disease.get(entity);

		if (infection == null || !infection.infected()) {
			return;
		}

		long progress = infection.progress();
		long remission = infection.remission();
		boolean wasInRemission = remission > 0;

		if (remission > 0) {
			remission = Math.max(0, remission - INTERVAL);
		} else {
			progress += INTERVAL;
		}

		Infection updated = infection.withProgress(progress, remission);
		Disease.Stage stage = Disease.stage(updated);

		if (stage.ordinal() > infection.lastStage()) {
			updated = updated.withLastStage(stage.ordinal());

			if (entity instanceof ServerPlayer player) {
				Symptoms.onStageReached(player, stage, strain);
			}
		}

		entity.setAttached(ModAttachments.INFECTION, updated);

		if (WorldInfection.isLivestock(entity) && WorldInfection.tickAnimal(level, entity, updated)) {
			return;
		}

		if (entity instanceof ServerPlayer player) {
			if (wasInRemission && remission == 0) {
				Chat.send(player, Msg.of("contagion.stage.relief_fading"), ChatFormatting.GRAY, ChatFormatting.ITALIC);
			}

			Symptoms.tickPlayer(level, player, updated, strain);
		} else {
			Symptoms.tickMob(level, entity, updated, strain);
		}

		if (entity.isAlive()) {
			Transmission.tickSource(level, entity, updated, strain);
		}
	}
}
