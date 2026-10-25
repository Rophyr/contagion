package com.contagion.lab;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import com.contagion.ContagionConfig;
import com.contagion.data.BloodSample;
import com.contagion.data.CultureData;
import com.contagion.data.SerumMix;
import com.contagion.disease.Form;
import com.contagion.disease.Ingredient;
import com.contagion.disease.Vanilla;
import com.contagion.registry.ModBlocks;
import com.contagion.registry.ModComponents;
import com.contagion.registry.ModItems;
import com.contagion.text.Chat;
import com.contagion.text.Msg;

/**
 * Right-click behavior of all lab items and blocks. Every handler runs on both sides: the client only decides
 * whether the click is "ours" (so the hand swings and the packet is sent), the server does the work.
 */
public final class LabInteractions {
	private static final Map<UUID, Long> PENDING_SELF_INJECTION = new HashMap<>();
	private static final Map<UUID, Long> LAST_DRAW = new HashMap<>();

	private LabInteractions() {
	}

	public static void register() {
		UseItemCallback.EVENT.register(LabInteractions::onUseItem);
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> onUseEntity(player, level, hand, entity));
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (player.isSpectator() || hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}

			BlockState state = level.getBlockState(hit.getBlockPos());
			boolean microscope = state.is(ModBlocks.MICROSCOPE) && !player.isShiftKeyDown();
			// Sneaking is allowed on the beaker only to pour it out with an empty hand.
			boolean beaker = state.is(ModBlocks.BEAKER) && (!player.isShiftKeyDown() || player.getMainHandItem().isEmpty());

			if (!microscope && !beaker) {
				return InteractionResult.PASS;
			}

			if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
				if (microscope) {
					Microscope.use(serverLevel, serverPlayer, hit.getBlockPos());
				} else {
					Beaker.use(serverLevel, serverPlayer, hit.getBlockPos(), state);
				}
			}

			return InteractionResult.SUCCESS;
		});
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (level instanceof ServerLevel serverLevel && (state.is(ModBlocks.MICROSCOPE) || state.is(ModBlocks.BEAKER))) {
				LabJobs.onBroken(serverLevel, pos);
			}
		});
	}

	private static InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
		if (player.isSpectator()) {
			return InteractionResult.PASS;
		}

		ItemStack stack = player.getItemInHand(hand);
		ServerPlayer sp = player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel ? serverPlayer : null;

		if (stack.is(ModItems.SYRINGE)) {
			if (!player.isShiftKeyDown()) {
				Chat.bar(sp, Msg.of("contagion.hint.syringe"), ChatFormatting.GRAY);
				return InteractionResult.PASS;
			}

			if (sp != null) {
				drawOwnBlood((ServerLevel) level, sp, stack);
			}

			return InteractionResult.SUCCESS;
		}

		if (stack.is(ModItems.PETRI_DISH) && hand == InteractionHand.MAIN_HAND) {
			if (sp != null) {
				inoculate((ServerLevel) level, sp, stack);
			}

			return InteractionResult.SUCCESS;
		}

		if (stack.is(ModItems.CULTURE)) {
			if (sp != null) {
				describeCulture(sp, stack);
			}

			return InteractionResult.SUCCESS;
		}

		if (stack.is(ModItems.MORTAR) && hand == InteractionHand.MAIN_HAND) {
			if (sp != null) {
				grind((ServerLevel) level, sp);
			}

			return InteractionResult.SUCCESS;
		}

		if (stack.is(ModItems.EMPTY_VIAL) && hand == InteractionHand.MAIN_HAND) {
			if (sp != null) {
				startMixture((ServerLevel) level, sp, stack);
			}

			return InteractionResult.SUCCESS;
		}

		if (stack.is(ModItems.SERUM)) {
			SerumMix mix = stack.get(ModComponents.SERUM);

			if (mix == null) {
				return InteractionResult.PASS;
			}

			if (sp != null) {
				if (mix.sealed()) {
					if (player.isShiftKeyDown()) {
						Chat.bar(sp, Msg.of("contagion.hint.sealed_serum", Ingredient.listMsg(Ingredient.parseAll(mix.reagents()))), ChatFormatting.GRAY);
					} else if (confirmSelfInjection(sp, level.getGameTime())) {
						stack.shrink(1);
						Treatment.inject((ServerLevel) level, sp, sp, mix);
					}
				} else if (player.isShiftKeyDown()) {
					seal((ServerLevel) level, sp, stack, mix);
				} else if (hand == InteractionHand.MAIN_HAND) {
					addReagent((ServerLevel) level, sp, stack, mix);
				}
			}

			return InteractionResult.SUCCESS;
		}

		if (stack.is(ModItems.THERMOMETER)) {
			if (sp != null) {
				sp.sendSystemMessage(Lab.temperatureReading(sp, true));
			}

			return InteractionResult.SUCCESS;
		}

		if (stack.is(ModItems.RESEARCH_JOURNAL)) {
			if (sp != null) {
				ServerLevel serverLevel = (ServerLevel) level;

				if (player.isShiftKeyDown()) {
					Journal.show(serverLevel.getServer(), sp);
				} else {
					Journal.open(serverLevel.getServer(), sp, stack, hand);
				}

				Vanilla.playSound(serverLevel, sp, "item.book.page_turn", 1.0F, 1.0F);
			}

			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	private static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity entity) {
		if (player.isSpectator() || !(entity instanceof LivingEntity target) || !target.isAlive()) {
			return InteractionResult.PASS;
		}

		ItemStack stack = player.getItemInHand(hand);
		ServerPlayer sp = player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel ? serverPlayer : null;

		if (stack.is(ModItems.SYRINGE)) {
			if (sp != null && canDraw(sp, target)) {
				ServerLevel serverLevel = (ServerLevel) level;
				stack.shrink(1);
				give(sp, Lab.bloodSample(serverLevel.getServer(), target));
				Vanilla.playSound(serverLevel, target, "item.bottle.fill", 0.6F, 1.8F);
				Chat.bar(sp, Msg.of("contagion.hint.drew_blood", target.getName().getString()), ChatFormatting.GRAY);
				Chat.bar(target, Msg.of("contagion.hint.blood_drawn_by", player.getName().getString()), ChatFormatting.GRAY);
			}

			return InteractionResult.SUCCESS;
		}

		// Transfusing needs sneaking, so trading with a villager while holding a sample stays safe.
		if (stack.is(ModItems.BLOOD_SAMPLE) && player.isShiftKeyDown()) {
			BloodSample sample = stack.get(ModComponents.BLOOD_SAMPLE);

			if (sample == null) {
				return InteractionResult.PASS;
			}

			if (sp != null) {
				stack.shrink(1);
				Transfusion.transfuse((ServerLevel) level, sp, target, sample);
			}

			return InteractionResult.SUCCESS;
		}

		if (stack.is(ModItems.SERUM)) {
			SerumMix mix = stack.get(ModComponents.SERUM);

			if (mix == null || !mix.sealed()) {
				return InteractionResult.PASS;
			}

			if (sp != null) {
				stack.shrink(1);
				Treatment.inject((ServerLevel) level, sp, target, mix);
			}

			return InteractionResult.SUCCESS;
		}

		if (stack.is(ModItems.THERMOMETER)) {
			if (sp != null) {
				sp.sendSystemMessage(Lab.temperatureReading(target, false));
			}

			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	/** Veins need time to recover between blood draws, which paces how many cultures a team can run. */
	private static boolean canDraw(ServerPlayer player, LivingEntity donor) {
		long now = Lab.now(player.level().getServer());
		Long last = LAST_DRAW.get(donor.getUUID());

		if (last != null && now - last < ContagionConfig.bloodDrawCooldownTicks) {
			long minutes = Math.max(1, (ContagionConfig.bloodDrawCooldownTicks - (now - last)) / 1200);
			Chat.bar(player, Msg.of("contagion.hint.vein_recovering", donor.getName().getString(), minutes), ChatFormatting.GRAY);
			return false;
		}

		LAST_DRAW.put(donor.getUUID(), now);
		return true;
	}

	/** Self-injection is a hardcore gamble, so it needs a second use within 3 seconds. */
	private static boolean confirmSelfInjection(ServerPlayer player, long now) {
		Long first = PENDING_SELF_INJECTION.get(player.getUUID());

		if (first != null && now - first <= 60) {
			PENDING_SELF_INJECTION.remove(player.getUUID());
			return true;
		}

		PENDING_SELF_INJECTION.put(player.getUUID(), now);
		Chat.bar(player, Msg.of("contagion.hint.confirm_inject"), ChatFormatting.YELLOW);
		return false;
	}

	private static void drawOwnBlood(ServerLevel level, ServerPlayer player, ItemStack syringe) {
		if (player.getHealth() <= 3.0F) {
			Chat.bar(player, Msg.of("contagion.hint.too_weak"), ChatFormatting.GRAY);
			return;
		}

		if (!canDraw(player, player)) {
			return;
		}

		syringe.shrink(1);
		player.hurtServer(level, level.damageSources().generic(), 1.0F);
		give(player, Lab.bloodSample(level.getServer(), player));
		Vanilla.playSound(level, player, "item.bottle.fill", 0.6F, 1.8F);
		Chat.bar(player, Msg.of("contagion.hint.drew_own"), ChatFormatting.GRAY);
	}

	/** Petri dish in hand, blood sample in the off-hand: start a culture. */
	private static void inoculate(ServerLevel level, ServerPlayer player, ItemStack dish) {
		ItemStack off = player.getOffhandItem();
		BloodSample sample = off.is(ModItems.BLOOD_SAMPLE) ? off.get(ModComponents.BLOOD_SAMPLE) : null;

		if (sample == null) {
			Chat.bar(player, Msg.of("contagion.hint.dish_need_sample"), ChatFormatting.GRAY);
			return;
		}

		MinecraftServer server = level.getServer();
		// Whether colonies will appear is decided now (and hidden): more pathogen, better odds.
		double odds = !sample.infected() || !Microscope.isFresh(server, sample.day()) ? 0.0 : switch (sample.load()) {
			case 0, 1 -> 0.0;
			case 2 -> 0.5;
			case 3 -> 0.85;
			default -> 1.0;
		};
		boolean grows = player.getRandom().nextDouble() < odds;

		dish.shrink(1);
		off.shrink(1);
		long now = Lab.now(server);
		give(player, Lab.culture(new CultureData(sample.source(), sample.load(), sample.day(), now, grows), now));
		Vanilla.playSound(level, player, "item.bottle.empty", 0.6F, 1.6F);
		Chat.bar(player, Msg.of("contagion.hint.inoculated", sample.source(), ContagionConfig.cultureGrowTicks / 1200), ChatFormatting.AQUA);
	}

	private static void describeCulture(ServerPlayer player, ItemStack stack) {
		CultureData data = stack.get(ModComponents.CULTURE);

		if (data == null) {
			return;
		}

		long now = Lab.now(player.level().getServer());
		Lab.refreshCulture(stack, now);

		switch (data.state(now)) {
			case CultureData.GROWING -> player.sendSystemMessage(Lab.progressBar(Msg.of("contagion.progress.growing"), data.growth(now),
					(ContagionConfig.cultureGrowTicks - (now - data.startedAt())) / 20), true);
			case CultureData.COLONIZED -> Chat.bar(player, Msg.of("contagion.hint.culture_ready"), ChatFormatting.GREEN);
			case CultureData.STERILE -> Chat.bar(player, Msg.of("contagion.hint.culture_sterile"), ChatFormatting.GRAY);
			default -> Chat.bar(player, Msg.of("contagion.hint.culture_dead"), ChatFormatting.DARK_GRAY);
		}
	}

	/** Mortar in hand, raw reagent in the off-hand: grind it. */
	private static void grind(ServerLevel level, ServerPlayer player) {
		ItemStack off = player.getOffhandItem();
		Ingredient ingredient = Ingredient.of(off);

		if (ingredient == null) {
			Chat.bar(player, Msg.of("contagion.hint.mortar_need_reagent"), ChatFormatting.GRAY);
			return;
		}

		if (ingredient.form() != Form.RAW) {
			Chat.bar(player, Msg.of("contagion.hint.mortar_only_raw"), ChatFormatting.GRAY);
			return;
		}

		off.shrink(1);
		Ingredient ground = new Ingredient(ingredient.reagent(), Form.GROUND);
		give(player, Lab.prepared(ground));
		Vanilla.playSound(level, player, "block.grindstone.use", 0.6F, 1.4F);
		Chat.bar(player, Msg.of("contagion.hint.ground", ground.msg()), ChatFormatting.GRAY);
	}

	private static void startMixture(ServerLevel level, ServerPlayer player, ItemStack vial) {
		Ingredient ingredient = Ingredient.of(player.getOffhandItem());

		if (ingredient == null) {
			Chat.bar(player, Msg.of("contagion.hint.vial_need_reagent"), ChatFormatting.GRAY);
			return;
		}

		vial.shrink(1);
		player.getOffhandItem().shrink(1);

		ItemStack serum = new ItemStack(ModItems.SERUM);
		Lab.updateSerumStack(serum, new SerumMix(List.of(ingredient.encode()), false));
		give(player, serum);
		Vanilla.playSound(level, player, "item.bottle.fill", 0.8F, 1.0F);
		Chat.bar(player, Msg.of("contagion.hint.mixture_started", ingredient.msg()), ChatFormatting.GRAY);
	}

	private static void addReagent(ServerLevel level, ServerPlayer player, ItemStack stack, SerumMix mix) {
		Ingredient ingredient = Ingredient.of(player.getOffhandItem());

		if (ingredient == null) {
			Chat.bar(player, Msg.of("contagion.hint.mixture_need_reagent"), ChatFormatting.GRAY);
			return;
		}

		if (mix.isFull()) {
			Chat.bar(player, Msg.of("contagion.hint.mixture_full"), ChatFormatting.GRAY);
			return;
		}

		if (mix.reagents().contains(ingredient.encode())) {
			Chat.bar(player, Msg.of("contagion.hint.mixture_duplicate", ingredient.msg()), ChatFormatting.GRAY);
			return;
		}

		player.getOffhandItem().shrink(1);
		Lab.updateSerumStack(stack, mix.with(ingredient.encode()));
		Vanilla.playSound(level, player, "item.bottle.fill", 0.8F, 1.2F);
		Chat.bar(player, Msg.of("contagion.hint.mixture_added", ingredient.msg(), mix.reagents().size() + 1, SerumMix.MAX_REAGENTS), ChatFormatting.GRAY);
	}

	private static void seal(ServerLevel level, ServerPlayer player, ItemStack stack, SerumMix mix) {
		Lab.updateSerumStack(stack, mix.seal());
		Vanilla.playSound(level, player, "block.brewing_stand.brew", 0.8F, 1.0F);
		Chat.bar(player, Msg.of("contagion.hint.sealed"), ChatFormatting.GRAY);
	}

	private static void give(Player player, ItemStack stack) {
		player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
	}
}
