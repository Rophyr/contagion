package com.contagion.test;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.contagion.ContagionConfig;
import com.contagion.ContagionMod;
import com.contagion.client.JournalScreen;
import com.contagion.client.MicroscopeScreen;
import com.contagion.data.CultureData;
import com.contagion.data.Infection;
import com.contagion.data.SerumMix;
import com.contagion.data.Strain;
import com.contagion.disease.Disease;
import com.contagion.disease.Ingredient;
import com.contagion.disease.Strains;
import com.contagion.disease.WorldInfection;
import com.contagion.lab.Journal;
import com.contagion.lab.Lab;
import com.contagion.lab.Microscope;
import com.contagion.lab.MicroscopeMenu;
import com.contagion.registry.ModAttachments;
import com.contagion.registry.ModBlocks;
import com.contagion.registry.ModItems;

/**
 * Walks through the mod's screens in a real client and takes screenshots of each (thermometer HUD, microscope
 * in every state, every journal tab). Run in CI with {@code runClientGameTest}.
 */
public final class ContagionClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		ContagionConfig.analysisTicks = 100;
		ContagionConfig.cultureTicks = 100;

		try (TestSingleplayerContext game = context.worldBuilder().create()) {
			game.getConnection().waitForChunksRender();
			game.getServer().runCommand("time set noon");
			game.getServer().runCommand("weather clear");

			BlockPos microscope = game.getServer().computeOnServer(server -> {
				ServerPlayer player = player(server);
				ServerLevel level = player.level();
				BlockPos at = player.blockPosition().relative(player.getDirection(), 2);
				level.setBlockAndUpdate(at, ModBlocks.MICROSCOPE.defaultBlockState());
				Disease.infect(server, player, true);
				player.setAttached(ModAttachments.INFECTION, Infection.fresh().withProgress(ContagionConfig.incubationTicks + 7 * ContagionConfig.dayLengthTicks, 0));
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.THERMOMETER));
				return at;
			});
			context.waitTicks(20);

			// ---- Thermometer HUD
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(30);
			context.takeScreenshot("01_thermometer_charging");
			context.waitTicks(40);
			context.getInput().releaseKey(options -> options.keyUse);
			context.waitTicks(4);
			context.takeScreenshot("02_thermometer_result");

			// ---- Microscope
			game.getServer().runOnServer(server -> Microscope.use(player(server).level(), player(server), microscope));
			context.waitForScreen(MicroscopeScreen.class);
			context.waitTicks(5);
			context.takeScreenshot("03_microscope_empty");

			game.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				player.containerMenu.getSlot(MicroscopeMenu.SLIDE_SLOT).set(Lab.bloodSample(server, player));
				player.containerMenu.broadcastChanges();
			});
			context.waitTicks(5);
			context.takeScreenshot("04_microscope_slide");

			game.getServer().runOnServer(server -> player(server).containerMenu.clickMenuButton(player(server), Microscope.BUTTON_START));
			context.waitTicks(50);
			context.takeScreenshot("05_microscope_running");
			context.waitTicks(70);
			context.takeScreenshot("06_microscope_ready");

			game.getServer().runOnServer(server -> player(server).containerMenu.clickMenuButton(player(server), Microscope.BUTTON_EXAMINE));
			context.waitTicks(10);
			context.takeScreenshot("07_microscope_blood_result");

			game.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				Strain strain = Strains.getOrCreate(server);
				long now = Lab.now(server);
				CultureData data = new CultureData(player.getName().getString(), 4, (int) Lab.day(server), now - ContagionConfig.cultureGrowTicks - 20, true);
				ItemStack serum = new ItemStack(ModItems.SERUM);
				List<String> reagents = new ArrayList<>(strain.cure().subList(0, 2));
				reagents.add(strain.toxic().getFirst() + ":RAW");
				Lab.updateSerumStack(serum, new SerumMix(List.copyOf(reagents), true));

				for (Ingredient ingredient : Ingredient.parseAll(reagents)) {
					Lab.noteTried(server, ingredient);
				}
				player.containerMenu.getSlot(MicroscopeMenu.SLIDE_SLOT).set(Lab.culture(data, now));
				player.containerMenu.getSlot(MicroscopeMenu.SERUM_SLOT).set(serum);
				player.containerMenu.broadcastChanges();
			});
			context.waitTicks(5);
			context.takeScreenshot("08_microscope_culture_slide");
			game.getServer().runOnServer(server -> player(server).containerMenu.clickMenuButton(player(server), Microscope.BUTTON_START));
			context.waitTicks(40);
			context.takeScreenshot("09_microscope_culture_running");
			context.waitTicks(80);
			game.getServer().runOnServer(server -> player(server).containerMenu.clickMenuButton(player(server), Microscope.BUTTON_EXAMINE));
			context.waitTicks(10);
			context.takeScreenshot("10_microscope_culture_result");
			context.setScreen(() -> null);
			context.waitTicks(5);

			// ---- Journal
			game.getServer().runOnServer(server -> Journal.open(server, player(server)));
			context.waitForScreen(JournalScreen.class);

			for (int tab = 0; tab < 5; tab++) {
				int index = tab;
				context.runOnClient(minecraft -> ((JournalScreen) minecraft.gui.screen()).selectTab(index));
				context.waitTicks(3);
				context.takeScreenshot("1" + (tab + 1) + "_journal_tab" + tab);
			}

			context.setScreen(() -> null);

			// ---- Same screens in French (longer text)
			context.runOnClient(minecraft -> {
				minecraft.options.languageCode = "fr_fr";
				minecraft.getLanguageManager().setSelected("fr_fr");
				minecraft.reloadResourcePacks();
			});
			context.waitTicks(100);
			game.getServer().runOnServer(server -> Journal.open(server, player(server)));
			context.waitForScreen(JournalScreen.class);

			for (int tab : new int[] {0, 1, 2}) {
				context.runOnClient(minecraft -> ((JournalScreen) minecraft.gui.screen()).selectTab(tab));
				context.waitTicks(3);
				context.takeScreenshot("2" + tab + "_fr_journal_tab" + tab);
			}

			context.setScreen(() -> null);
			game.getServer().runOnServer(server -> Microscope.use(player(server).level(), player(server), microscope));
			context.waitForScreen(MicroscopeScreen.class);
			context.waitTicks(5);
			context.takeScreenshot("23_fr_microscope_result");
			context.setScreen(() -> null);

			// ---- World infection: how many freshly spawned farm animals carry it?
			int carriers = game.getServer().computeOnServer(server -> {
				ServerPlayer player = player(server);

				for (int i = 0; i < 200; i++) {
					server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
							"summon " + (i % 2 == 0 ? "cow" : "pig") + " " + player.getX() + " " + (player.getY() + 40) + " " + player.getZ() + " {NoAI:1b}");
				}

				int count = 0;

				for (Entity entity : player.level().getAllEntities()) {
					if (WorldInfection.isLivestock(entity) && Disease.isInfected(entity)) {
						count++;
					}
				}

				return count;
			});
			ContagionMod.LOGGER.info("[client-test] infected livestock after summoning 200: {}", carriers);
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}
}
