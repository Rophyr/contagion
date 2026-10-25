package com.contagion.lab;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;

import com.contagion.ContagionMod;

/**
 * Grants the mod's code-triggered advancements (their JSON uses the {@code minecraft:impossible} trigger).
 */
public final class Advancements {
	private Advancements() {
	}

	public static void award(ServerPlayer player, String name) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(ContagionMod.id(name));

		if (advancement != null) {
			player.getAdvancements().award(advancement, "done");
		}
	}
}
