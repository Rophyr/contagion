package com.contagion.lab;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import com.contagion.block.BeakerBlock;
import com.contagion.block.MicroscopeBlock;
import com.contagion.data.PendingTest;
import com.contagion.data.Research;
import com.contagion.disease.Strains;
import com.contagion.disease.Vanilla;
import com.contagion.text.Chat;
import com.contagion.text.Msg;

/** Timed jobs running on lab blocks (microscopes, beakers), stored in the shared research data. */
public final class LabJobs {
	private LabJobs() {
	}

	public static String dimension(ServerLevel level) {
		return level.dimension().identifier().toString();
	}

	public static @Nullable PendingTest find(Research research, ServerLevel level, BlockPos pos) {
		String dimension = dimension(level);
		long packed = pos.asLong();

		for (PendingTest test : research.pending()) {
			if (test.isAt(dimension, packed)) {
				return test;
			}
		}

		return null;
	}

	public static Research remove(Research research, ServerLevel level, BlockPos pos) {
		String dimension = dimension(level);
		long packed = pos.asLong();
		return research.withoutPending(test -> test.isAt(dimension, packed));
	}

	/** Breaking a lab block ruins whatever was running on it. */
	public static void onBroken(ServerLevel level, BlockPos pos) {
		MinecraftServer server = level.getServer();
		Research research = Strains.research(server);

		if (find(research, level, pos) != null) {
			Strains.setResearch(server, remove(research, level, pos));
		}
	}

	/** Once per second: lamps, bubbles, and "it's done" notifications. */
	public static void tick(MinecraftServer server) {
		Research research = Strains.research(server);

		if (research.pending().isEmpty()) {
			return;
		}

		long now = Lab.now(server);
		Research updated = research;

		for (PendingTest test : research.pending()) {
			ServerLevel level = levelOf(server, test.dimension());

			if (level == null) {
				continue;
			}

			BlockPos pos = BlockPos.of(test.pos());
			double x = pos.getX() + 0.5;
			double y = pos.getY() + 0.5;
			double z = pos.getZ() + 0.5;

			if (now < test.readyAt()) {
				if (test.kind() == PendingTest.BOIL && level.isLoaded(pos)) {
					Vanilla.particles(level, "bubble_pop", x, pos.getY() + 0.6, z, 4, 0.1, 0.02);

					if (level.getRandom().nextInt(4) == 0) {
						Vanilla.particles(level, "cloud", x, pos.getY() + 0.75, z, 1, 0.05, 0.01);
					}
				}

				continue;
			}

			if (test.notified()) {
				continue;
			}

			boolean beaker = test.kind() == PendingTest.BOIL;

			if (beaker) {
				BeakerBlock.setContents(level, pos, BeakerBlock.DONE);
			} else {
				MicroscopeBlock.setStatus(level, pos, MicroscopeBlock.READY);
			}

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.level() == level && player.distanceToSqr(x, y, z) < 32 * 32) {
					Chat.send(player, Msg.of(beaker ? "contagion.beaker.done_notify" : "contagion.microscope.done_notify"), ChatFormatting.AQUA);
				}
			}

			Vanilla.playSound(level, x, y, z, "block.note_block.bell", SoundSource.BLOCKS, 1.0F, beaker ? 0.9F : 1.2F);
			updated = updated.replacePending(test, test.markNotified());
		}

		if (updated != research) {
			Strains.setResearch(server, updated);
		}
	}

	private static @Nullable ServerLevel levelOf(MinecraftServer server, String dimension) {
		for (ServerLevel level : server.getAllLevels()) {
			if (dimension(level).equals(dimension)) {
				return level;
			}
		}

		return null;
	}
}
