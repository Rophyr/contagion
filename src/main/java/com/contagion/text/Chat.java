package com.contagion.text;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Sends translatable messages; each client shows them in its own language. */
public final class Chat {
	private Chat() {
	}

	public static void send(Entity entity, Msg msg, ChatFormatting... style) {
		if (entity instanceof ServerPlayer player) {
			player.sendSystemMessage(msg.styled(style));
		}
	}

	/** Action bar (above the hotbar). */
	public static void bar(Entity entity, Msg msg, ChatFormatting... style) {
		if (entity instanceof ServerPlayer player) {
			player.sendSystemMessage(msg.styled(style), true);
		}
	}
}
