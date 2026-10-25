package com.contagion.lab;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.contagion.disease.Vanilla;
import com.contagion.network.ThermometerPayload;
import com.contagion.text.Msg;

/**
 * Hold use for a few seconds to take a temperature: your own, or that of whoever you right-clicked.
 * The mercury rises on the HUD while charging; afterwards the thermometer needs a moment to reset.
 */
public final class ThermometerItem extends Item {
	public static final int MEASURE_TICKS = 60;
	public static final int COOLDOWN_TICKS = 100;
	private static final double REACH = 4.0;
	/** Who each player is measuring (when they started on another entity). */
	private static final Map<UUID, UUID> TARGETS = new ConcurrentHashMap<>();

	public ThermometerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player.getCooldowns().isOnCooldown(player.getItemInHand(hand))) {
			return InteractionResult.FAIL;
		}

		if (player instanceof ServerPlayer) {
			TARGETS.remove(player.getUUID());
		}

		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	/** Right-clicking a mob or player starts measuring them instead of yourself. */
	public static InteractionResult useOn(Player player, InteractionHand hand, LivingEntity target) {
		if (player.getCooldowns().isOnCooldown(player.getItemInHand(hand))) {
			return InteractionResult.FAIL;
		}

		if (player instanceof ServerPlayer) {
			TARGETS.put(player.getUUID(), target.getUUID());
		}

		player.startUsingItem(hand);
		return InteractionResult.SUCCESS;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return MEASURE_TICKS;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.TOOT_HORN;
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
		if (entity instanceof ServerPlayer player && remainingTicks % 15 == 0 && remainingTicks > 0) {
			Vanilla.privateSound(player, "block.note_block.hat", player.getX(), player.getY(), player.getZ(), 0.25F, 1.6F);
		}
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (entity instanceof Player player) {
			player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
		}

		if (level instanceof ServerLevel serverLevel && entity instanceof ServerPlayer player) {
			LivingEntity subject = target(serverLevel, player);
			boolean self = subject == player;
			double celsius = Lab.temperature(subject);
			player.sendSystemMessage(Lab.temperatureReading(subject, self, celsius));
			ServerPlayNetworking.send(player, new ThermometerPayload((float) celsius,
					self ? Msg.of("contagion.thermometer.you").component() : subject.getName()));
			Vanilla.privateSound(player, "block.note_block.bell", player.getX(), player.getY(), player.getZ(), 0.5F, 1.8F);
		}

		return stack;
	}

	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTicks) {
		if (entity instanceof ServerPlayer) {
			TARGETS.remove(entity.getUUID());
		}

		return false;
	}

	private static LivingEntity target(ServerLevel level, ServerPlayer player) {
		UUID id = TARGETS.remove(player.getUUID());
		@Nullable LivingEntity found = id == null ? null : level.getEntity(id) instanceof LivingEntity living ? living : null;

		if (found != null && found.isAlive() && found.distanceToSqr(player) <= REACH * REACH) {
			return found;
		}

		return player;
	}
}
