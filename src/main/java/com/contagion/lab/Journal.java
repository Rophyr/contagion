package com.contagion.lab;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.contagion.data.Research;
import com.contagion.data.Strain;
import com.contagion.disease.Strains;
import com.contagion.network.JournalPayload;
import com.contagion.text.Chat;
import com.contagion.text.Msg;

/**
 * The research journal opens its own screen (overview, lab manual, reagent index, known facts and the lab
 * notebook). Research is shared, so every journal shows the same notes. Sneak + use prints a short summary in chat.
 */
public final class Journal {
	/** Manual sections: title key, then paragraph keys (contagion.book.manual.*). */
	public static final String[][] MANUAL = {
			{"blood", "blood.1"},
			{"tips", "tips.1", "tips.2"},
			{"cultures", "cultures.1", "cultures.2"},
			{"preparing", "preparing.1", "preparing.2"},
			{"mixing", "mixing.1", "mixing.2"},
			{"testing", "testing.1", "testing.2"},
			{"treating", "treating.1", "treating.2"},
			{"other", "other.1", "other.2", "other.3"},
	};

	private Journal() {
	}

	/** Sends the shared research to the player, whose client opens the journal screen. Never includes the cure. */
	public static void open(MinecraftServer server, ServerPlayer player) {
		Research research = Strains.research(server);
		Strain strain = Strains.get(server);
		boolean identified = strain != null && research.knows("identified");
		List<String> findings = new ArrayList<>();

		for (String finding : research.findings()) {
			if (Lab.describeFinding(finding) != null) {
				findings.add(finding);
			}
		}

		List<Component> log = new ArrayList<>();

		for (Msg entry : research.log()) {
			log.add(entry.component());
		}

		ServerPlayNetworking.send(player, new JournalPayload((int) Lab.day(server), identified,
				identified ? strain.nameMsg().component() : Component.empty(), identified ? strain.profileMsg().component() : Component.empty(),
				List.copyOf(findings), List.copyOf(log)));
	}

	private static int countFacts(Research research) {
		int count = 0;

		for (String finding : research.findings()) {
			if (Lab.describeFinding(finding) != null) {
				count++;
			}
		}

		return count;
	}

	/** Quick chat summary (sneak + use). */
	public static void show(MinecraftServer server, ServerPlayer player) {
		Research research = Strains.research(server);
		Strain strain = Strains.get(server);
		Chat.send(player, Msg.of("contagion.journal.chat_header"), ChatFormatting.GOLD, ChatFormatting.BOLD);

		if (strain == null || !research.knows("identified")) {
			Chat.send(player, Msg.of("contagion.journal.chat_unidentified"), ChatFormatting.GRAY);
		} else {
			Chat.send(player, Msg.of("contagion.journal.chat_pathogen", strain.nameMsg(), strain.profileMsg()), ChatFormatting.RED);
		}

		Chat.send(player, Msg.of("contagion.journal.chat_counts", countFacts(research), research.log().size()), ChatFormatting.GRAY);
		List<Msg> log = research.log();

		for (Msg entry : log.subList(Math.max(0, log.size() - 3), log.size())) {
			Chat.send(player, entry, ChatFormatting.GRAY);
		}

		Chat.send(player, Msg.of("contagion.journal.chat_open"), ChatFormatting.DARK_GRAY);
	}
}
