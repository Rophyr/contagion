package com.contagion.lab;

import java.util.List;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import com.contagion.ContagionConfig;
import com.contagion.block.BeakerBlock;
import com.contagion.data.PendingTest;
import com.contagion.data.Research;
import com.contagion.disease.Form;
import com.contagion.disease.Ingredient;
import com.contagion.disease.Strains;
import com.contagion.disease.Vanilla;
import com.contagion.registry.ModItems;
import com.contagion.text.Chat;
import com.contagion.text.Msg;

/**
 * Beaker: fill with water, place over heat, add a reagent, wait, then collect the extract with an Empty Vial.
 * Sneak + use with an empty hand pours it out.
 */
public final class Beaker {
	private static final Set<String> HEAT = Set.of("campfire", "soul_campfire", "fire", "soul_fire", "lava", "magma_block");

	private Beaker() {
	}

	public static void use(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
		MinecraftServer server = level.getServer();
		long now = Lab.now(server);
		Research research = Strains.research(server);
		PendingTest job = LabJobs.find(research, level, pos);
		ItemStack main = player.getMainHandItem();
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.5;
		double z = pos.getZ() + 0.5;

		if (job != null) {
			List<Ingredient> result = Ingredient.parseAll(job.ingredients());

			if (now < job.readyAt()) {
				long remaining = job.readyAt() - now;
				player.sendSystemMessage(Lab.progressBar(Msg.of("contagion.progress.boil"), 1.0 - remaining / (double) Math.max(1, ContagionConfig.boilTicks),
						remaining / 20), true);
			} else if (main.is(ModItems.EMPTY_VIAL) && !result.isEmpty()) {
				main.shrink(1);
				player.getInventory().placeItemBackInInventory(Lab.prepared(result.getFirst()), Prediction.SERVER_ONLY);
				Strains.setResearch(server, LabJobs.remove(research, level, pos));
				BeakerBlock.setContents(level, pos, BeakerBlock.EMPTY);
				Vanilla.playSound(level, x, y, z, "item.bottle.fill", SoundSource.BLOCKS, 1.0F, 1.0F);
				Chat.bar(player, Msg.of("contagion.beaker.collected", result.getFirst().msg()), ChatFormatting.GREEN);
				Advancements.award(player, "alchemist");
			} else {
				Chat.bar(player, Msg.of("contagion.beaker.need_vial"), ChatFormatting.YELLOW);
			}

			return;
		}

		int contents = state.getValue(BeakerBlock.CONTENTS);

		if (contents >= BeakerBlock.BOILING) {
			// Job lost (e.g. edited world): reset.
			BeakerBlock.setContents(level, pos, BeakerBlock.EMPTY);
			contents = BeakerBlock.EMPTY;
		}

		if (main.isEmpty() && player.isShiftKeyDown()) {
			if (contents == BeakerBlock.WATER) {
				BeakerBlock.setContents(level, pos, BeakerBlock.EMPTY);
				Vanilla.playSound(level, x, y, z, "item.bucket.empty", SoundSource.BLOCKS, 0.6F, 1.4F);
			}

			return;
		}

		if (contents == BeakerBlock.EMPTY) {
			Item returned = waterContainer(main);

			if (returned == null) {
				Chat.bar(player, Msg.of("contagion.beaker.need_water"), ChatFormatting.YELLOW);
				return;
			}

			main.shrink(1);
			player.getInventory().placeItemBackInInventory(new ItemStack(returned), Prediction.SERVER_ONLY);
			BeakerBlock.setContents(level, pos, BeakerBlock.WATER);
			Vanilla.playSound(level, x, y, z, "item.bottle.empty", SoundSource.BLOCKS, 1.0F, 1.0F);
			return;
		}

		Ingredient ingredient = Ingredient.of(main);

		if (ingredient == null) {
			Chat.bar(player, Msg.of("contagion.beaker.need_reagent"), ChatFormatting.YELLOW);
			return;
		}

		if (ingredient.form() == Form.EXTRACT) {
			Chat.bar(player, Msg.of("contagion.beaker.already_extract"), ChatFormatting.YELLOW);
			return;
		}

		if (!hasHeat(level, pos)) {
			Chat.bar(player, Msg.of("contagion.beaker.need_heat"), ChatFormatting.YELLOW);
			return;
		}

		main.shrink(1);
		Lab.noteTried(server, ingredient);
		Ingredient extract = new Ingredient(ingredient.reagent(), Form.EXTRACT);
		Lab.noteTried(server, extract);
		Strains.setResearch(server, research.withPending(new PendingTest(LabJobs.dimension(level), pos.asLong(), now + ContagionConfig.boilTicks,
				PendingTest.BOIL, PendingTest.NO_SAMPLE, List.of(extract.encode()), false)));
		BeakerBlock.setContents(level, pos, BeakerBlock.BOILING);
		Vanilla.playSound(level, x, y, z, "block.brewing_stand.brew", SoundSource.BLOCKS, 0.8F, 0.8F);
		Chat.bar(player, Msg.of("contagion.beaker.started", ingredient.reagent().msg()), ChatFormatting.AQUA);
	}

	private static boolean hasHeat(ServerLevel level, BlockPos pos) {
		Identifier below = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos.below()).getBlock());
		return "minecraft".equals(below.getNamespace()) && HEAT.contains(below.getPath());
	}

	/** Returns the empty container left after pouring, or null if the stack holds no water. */
	private static Item waterContainer(ItemStack stack) {
		if (stack.is(vanilla("water_bucket"))) {
			return vanilla("bucket");
		}

		if (stack.is(vanilla("potion"))) {
			return vanilla("glass_bottle");
		}

		return null;
	}

	private static Item vanilla(String path) {
		return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
	}
}
