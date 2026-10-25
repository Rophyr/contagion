package com.contagion.lab;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;

import com.contagion.data.Research;
import com.contagion.data.Strain;
import com.contagion.disease.Form;
import com.contagion.disease.Reagent;
import com.contagion.disease.Strains;
import com.contagion.text.Chat;
import com.contagion.text.Lang;
import com.contagion.text.Msg;

/**
 * The research journal opens as a book: cover, lab manual, reagent index, known facts and the lab notebook.
 * Research is shared, so every journal shows the same notes. Sneak + use prints a short summary in chat instead.
 */
public final class Journal {
	/** Manual sections: title key, then paragraph keys (contagion.book.manual.*). */
	private static final String[][] MANUAL = {
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

	public static void open(MinecraftServer server, ServerPlayer player, ItemStack stack, InteractionHand hand) {
		String language = Lang.of(player);
		List<Filterable<Component>> pages = new ArrayList<>();

		for (Component page : buildPages(Strains.research(server), Strains.get(server), Lab.day(server), language)) {
			pages.add(Filterable.passThrough(page));
		}

		stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(Lang.get(language, "contagion.book.title")),
				Lang.get(language, "contagion.book.author"), 0, pages, true));
		// Make sure the client has the fresh pages before the book screen opens.
		player.containerMenu.broadcastChanges();
		player.openItemGui(stack, hand);
	}

	static List<Component> buildPages(Research research, @Nullable Strain strain, long day, String language) {
		boolean identified = strain != null && research.knows("identified");
		BookWriter book = new BookWriter();
		Text t = (key, args) -> Msg.of(key, args).resolve(language);

		// ---- Cover
		book.line(t.get("contagion.book.title"), ChatFormatting.DARK_AQUA, ChatFormatting.BOLD);
		book.line("~~~~~~~~~~~~~~~~", ChatFormatting.DARK_GRAY);
		book.line(t.get("contagion.book.day", day), ChatFormatting.GRAY);
		book.blank();
		book.line(t.get("contagion.book.pathogen"), ChatFormatting.BOLD);

		if (identified) {
			book.line(strain.nameMsg().resolve(language), ChatFormatting.DARK_RED);
			book.line(t.get("contagion.book.type", strain.profileMsg()), ChatFormatting.GRAY);
			book.line(t.get("contagion.book.needs3"), ChatFormatting.GRAY);
		} else {
			book.line(t.get("contagion.book.unidentified"), ChatFormatting.DARK_GRAY);
			book.line(t.get("contagion.book.unidentified_hint"), ChatFormatting.GRAY);
		}

		book.blank();
		book.line(t.get("contagion.book.counts", countFacts(research), research.log().size()), ChatFormatting.DARK_GREEN);
		book.newPage();

		// ---- Lab manual
		for (String[] section : MANUAL) {
			book.line(t.get("contagion.book.manual." + section[0]), ChatFormatting.DARK_AQUA, ChatFormatting.BOLD);

			for (int i = 1; i < section.length; i++) {
				book.line(t.get("contagion.book.manual." + section[i]), i == section.length - 1 && section.length > 2 ? ChatFormatting.DARK_GRAY : ChatFormatting.BLACK);

				if (i < section.length - 1) {
					book.blank();
				}
			}

			book.newPage();
		}

		// ---- Reagent index
		book.line(t.get("contagion.book.index"), ChatFormatting.DARK_AQUA, ChatFormatting.BOLD);
		book.line("? " + t.get("contagion.book.legend.unknown"), ChatFormatting.DARK_GRAY);
		book.line("\u2717 " + t.get("contagion.book.legend.inert"), ChatFormatting.GRAY);
		book.line("\u2620 " + t.get("contagion.book.legend.toxic"), ChatFormatting.RED);
		book.line("\u2605 " + t.get("contagion.book.legend.active"), ChatFormatting.GOLD);
		book.blank();

		for (Reagent.Category category : Reagent.Category.values()) {
			book.keepTogether(6);
			boolean active = research.knows("cat:" + category.name());
			String header = (active ? "\u2605 " : "") + category.msg().resolve(language);

			for (Form form : Form.values()) {
				if (research.knows("form:" + category.name() + ":" + form.name())) {
					header = t.get("contagion.book.category_form", header, form.msg());
				}
			}

			book.line(header, active ? ChatFormatting.GOLD : ChatFormatting.DARK_AQUA, ChatFormatting.BOLD);

			for (Reagent reagent : Reagent.values()) {
				if (reagent.category() != category) {
					continue;
				}

				String name = reagent.msg().resolve(language);

				if (research.knows("toxic:" + reagent.name())) {
					book.line("\u2620 " + name, ChatFormatting.RED);
				} else if (research.knows("inert:" + reagent.name())) {
					book.line("\u2717 " + name, ChatFormatting.GRAY, ChatFormatting.STRIKETHROUGH);
				} else {
					book.line("? " + name, ChatFormatting.BLACK);
				}
			}

			book.blank();
		}

		book.newPage();

		// ---- Facts
		book.line(t.get("contagion.book.known_facts"), ChatFormatting.DARK_AQUA, ChatFormatting.BOLD);
		book.blank();
		boolean anyFact = false;

		for (String finding : research.findings()) {
			Msg text = Lab.describeFinding(finding);

			if (text != null) {
				book.paragraph("\u2022 " + text.resolve(language), ChatFormatting.BLACK);
				anyFact = true;
			}
		}

		if (!anyFact) {
			book.line(t.get("contagion.book.nothing_yet"), ChatFormatting.GRAY);
		}

		book.newPage();

		// ---- Notebook (newest first); keep the title on the same page as the first entry.
		List<Msg> log = research.log();

		book.keepTogether(log.isEmpty() ? 4 : 8);

		book.line(t.get("contagion.book.notebook"), ChatFormatting.DARK_AQUA, ChatFormatting.BOLD);
		book.line(t.get("contagion.book.newest_first"), ChatFormatting.GRAY);
		book.blank();

		if (log.isEmpty()) {
			book.line(t.get("contagion.book.empty"), ChatFormatting.GRAY);
		}

		for (int i = log.size() - 1; i >= 0; i--) {
			String entry = log.get(i).resolve(language);
			int dash = entry.indexOf(" - ");

			if (dash > 0) {
				book.paragraphWithHeader(entry.substring(0, dash), entry.substring(dash + 3));
			} else {
				book.paragraph(entry, ChatFormatting.BLACK);
			}
		}

		return book.build();
	}

	@FunctionalInterface
	private interface Text {
		String get(String key, Object... args);
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

	/**
	 * Lays text out on book pages. Pages hold about 14 lines of ~19 characters; the writer stays a little under
	 * that, because the book screen wraps (and could cut off) lines that are too wide.
	 */
	static final class BookWriter {
		private static final int LINES = 14;
		private static final int WIDTH = 18;

		private final List<Component> pages = new ArrayList<>();
		private MutableComponent page = Component.empty();
		private int lines;

		void line(String text, ChatFormatting... style) {
			for (String part : wrap(text, isBold(style) ? WIDTH - 2 : WIDTH)) {
				if (this.lines >= LINES) {
					newPage();
				}

				if (this.lines > 0) {
					this.page.append(Component.literal("\n"));
				}

				this.page.append(Component.literal(part).withStyle(style));
				this.lines++;
			}
		}

		void blank() {
			if (this.lines > 0 && this.lines < LINES) {
				this.page.append(Component.literal("\n"));
				this.lines++;
			}
		}

		/** Starts on a new page only if fewer than 5 lines are left; long paragraphs flow onto the next page. */
		void paragraph(String text, ChatFormatting... style) {
			keepTogether(Math.min(5, wrap(text, WIDTH).size()));
			line(text, style);
			blank();
		}

		void paragraphWithHeader(String header, String text) {
			keepTogether(Math.min(5, 1 + wrap(text, WIDTH).size()));
			line(header, ChatFormatting.DARK_GRAY);
			line(text, ChatFormatting.BLACK);
			blank();
		}

		void keepTogether(int neededLines) {
			if (this.lines > 0 && this.lines + neededLines > LINES) {
				newPage();
			}
		}

		void newPage() {
			if (this.lines > 0) {
				this.pages.add(this.page);
			}

			this.page = Component.empty();
			this.lines = 0;
		}

		List<Component> build() {
			newPage();
			return this.pages.size() > 100 ? List.copyOf(this.pages.subList(0, 100)) : List.copyOf(this.pages);
		}

		private static boolean isBold(ChatFormatting... style) {
			for (ChatFormatting formatting : style) {
				if (formatting == ChatFormatting.BOLD) {
					return true;
				}
			}

			return false;
		}

		static List<String> wrap(String text, int width) {
			List<String> out = new ArrayList<>();
			StringBuilder current = new StringBuilder();

			for (String word : text.split(" ")) {
				while (word.length() > width) {
					if (!current.isEmpty()) {
						out.add(current.toString());
						current.setLength(0);
					}

					out.add(word.substring(0, width));
					word = word.substring(width);
				}

				if (current.isEmpty()) {
					current.append(word);
				} else if (current.length() + 1 + word.length() <= width) {
					current.append(' ').append(word);
				} else {
					out.add(current.toString());
					current.setLength(0);
					current.append(word);
				}
			}

			if (!current.isEmpty() || out.isEmpty()) {
				out.add(current.toString());
			}

			return out;
		}
	}
}
