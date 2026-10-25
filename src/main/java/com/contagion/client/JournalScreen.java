package com.contagion.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.contagion.ContagionMod;
import com.contagion.disease.Form;
import com.contagion.disease.Reagent;
import com.contagion.lab.Journal;
import com.contagion.lab.Lab;
import com.contagion.network.JournalPayload;
import com.contagion.registry.ModItems;
import com.contagion.text.Msg;

/**
 * The research journal: an open book with bookmark tabs (overview, lab manual, reagents, findings, notebook).
 * Everything is shared research, sent by the server when the journal is opened.
 */
public final class JournalScreen extends Screen {
	private static final Identifier TEXTURE = ContagionMod.id("textures/gui/journal.png");
	private static final int BOOK_W = 300;
	private static final int BOOK_H = 190;
	private static final int PAGE_W = 118;
	private static final int PAGE_TOP = 16;
	private static final int PAGE_H = 150;
	private static final int LEFT_PAGE_X = 20;
	private static final int RIGHT_PAGE_X = 162;
	private static final int TAB_W = 28;
	private static final int ARROW_Y = 172;
	private static final int INK = 0xFF2A2622;
	private static final String[] TABS = {"overview", "manual", "reagents", "findings", "notebook"};

	private static int lastTab;

	private final JournalPayload data;
	private final List<List<List<Element>>> pages = new ArrayList<>();
	private final int[] spread = new int[TABS.length];
	private int tab = lastTab;
	private int bookX;
	private int bookY;

	public JournalScreen(JournalPayload data) {
		super(Msg.of("contagion.book.title").component());
		this.data = data;
	}

	@Override
	protected void init() {
		this.bookX = (this.width - BOOK_W) / 2;
		this.bookY = Math.max(22, (this.height - BOOK_H) / 2 + 8);
		this.pages.clear();
		this.pages.add(paginate(overview()));
		this.pages.add(paginate(manual()));
		this.pages.add(paginate(reagents()));
		this.pages.add(paginate(findings()));
		this.pages.add(paginate(notebook()));
	}

	// ---- content -----------------------------------------------------------------------------------------------

	private interface Element {
		int height();

		void draw(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY);
	}

	private record Line(FormattedCharSequence text, int indent) implements Element {
		@Override
		public int height() {
			return 10;
		}

		@Override
		public void draw(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
			graphics.text(Minecraft.getInstance().font, this.text, x + this.indent, y, INK, false);
		}
	}

	private record Space(int height) implements Element {
		@Override
		public void draw(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
		}
	}

	private record Rule() implements Element {
		@Override
		public int height() {
			return 6;
		}

		@Override
		public void draw(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
			graphics.fill(x + 20, y + 2, x + PAGE_W - 20, y + 3, 0x40604830);
		}
	}

	/** Large, centered title. */
	private record Title(Component text, float scale) implements Element {
		@Override
		public int height() {
			return (int) (10 * this.scale) + 4;
		}

		@Override
		public void draw(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
			Font font = Minecraft.getInstance().font;
			graphics.pose().pushMatrix();
			graphics.pose().translate(x + PAGE_W / 2.0F, y);
			graphics.pose().scale(this.scale, this.scale);
			graphics.text(font, this.text, -font.width(this.text) / 2, 0, INK, false);
			graphics.pose().popMatrix();
		}
	}

	/** Up to four reagent icons with what is known about them (hover for the name). */
	private final class ReagentRow implements Element {
		private final List<Reagent> reagents;

		ReagentRow(List<Reagent> reagents) {
			this.reagents = reagents;
		}

		@Override
		public int height() {
			return 24;
		}

		@Override
		public void draw(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
			for (int i = 0; i < this.reagents.size(); i++) {
				Reagent reagent = this.reagents.get(i);
				int ix = x + 6 + i * 28;
				int iy = y + 2;
				boolean toxic = knows("toxic:" + reagent.name());
				boolean inert = knows("inert:" + reagent.name());
				graphics.fill(ix - 2, iy - 2, ix + 18, iy + 18, toxic ? 0x50C02020 : inert ? 0x30000000 : 0x18604830);
				graphics.outline(ix - 2, iy - 2, 20, 20, toxic ? 0xFFA02020 : 0x60604830);
				graphics.item(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(reagent.id()))), ix, iy);

				if (inert) {
					graphics.fill(ix - 2, iy - 2, ix + 18, iy + 18, 0x90E8E0CC);
					cross(graphics, ix, iy, 0xFF6A6A6A);
				} else if (toxic) {
					graphics.text(JournalScreen.this.font, "☠", ix + 11, iy + 10, 0xFFB01010, false);
				}

				if (mouseX >= ix - 2 && mouseX < ix + 18 && mouseY >= iy - 2 && mouseY < iy + 18) {
					MutableComponent name = reagent.msg().component();
					Component status = Msg.of(toxic ? "contagion.book.legend.toxic" : inert ? "contagion.book.legend.inert" : "contagion.book.legend.unknown")
							.styled(toxic ? ChatFormatting.RED : ChatFormatting.GRAY);
					graphics.setTooltipForNextFrame(JournalScreen.this.font, List.of(name, status), Optional.empty(), mouseX, mouseY);
				}
			}
		}

		private static void cross(GuiGraphicsExtractor graphics, int x, int y, int color) {
			for (int i = 0; i < 14; i++) {
				graphics.fill(x + 1 + i, y + 1 + i, x + 3 + i, y + 2 + i, color);
				graphics.fill(x + 13 - i, y + 1 + i, x + 15 - i, y + 2 + i, color);
			}
		}
	}

	private boolean knows(String finding) {
		return this.data.findings().contains(finding);
	}

	/** A group of elements that should stay on one page if it fits. */
	private record Block(List<Element> elements) {
		int height() {
			int sum = 0;

			for (Element element : this.elements) {
				sum += element.height();
			}

			return sum;
		}
	}

	private Block paragraph(Component text, int indent) {
		List<Element> lines = new ArrayList<>();

		for (FormattedCharSequence line : this.font.split(text, PAGE_W - indent)) {
			lines.add(new Line(line, indent));
		}

		return new Block(lines);
	}

	private static Block single(Element element) {
		return new Block(List.of(element));
	}

	private static Component styled(String key, ChatFormatting... style) {
		return Msg.of(key).styled(style);
	}

	private List<Block> overview() {
		List<Block> blocks = new ArrayList<>();
		blocks.add(single(new Space(14)));
		blocks.add(single(new Title(styled("contagion.book.title", ChatFormatting.DARK_AQUA), 1.5F)));
		blocks.add(single(new Rule()));
		blocks.add(paragraph(Msg.of("contagion.book.day", this.data.day()).styled(ChatFormatting.DARK_GRAY), 0));
		blocks.add(single(new Space(14)));
		blocks.add(paragraph(styled("contagion.book.pathogen", ChatFormatting.BOLD), 0));

		if (this.data.identified()) {
			blocks.add(paragraph(this.data.name().copy().withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), 4));
			blocks.add(paragraph(Component.translatable("contagion.book.type", this.data.profile()).withStyle(ChatFormatting.DARK_GRAY), 4));
			blocks.add(single(new Space(4)));
			blocks.add(paragraph(styled("contagion.book.needs3", ChatFormatting.DARK_GRAY), 4));
		} else {
			blocks.add(paragraph(styled("contagion.book.unidentified", ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), 4));
			blocks.add(single(new Space(4)));
			blocks.add(paragraph(styled("contagion.book.unidentified_hint", ChatFormatting.DARK_GRAY), 4));
		}

		blocks.add(single(new Space(14)));
		blocks.add(paragraph(Msg.of("contagion.book.counts", factCount(), this.data.log().size()).styled(ChatFormatting.DARK_GREEN), 0));
		blocks.add(single(new Space(10)));
		blocks.add(paragraph(styled("contagion.journal.tabs_hint", ChatFormatting.GRAY, ChatFormatting.ITALIC), 0));
		return blocks;
	}

	private int factCount() {
		int count = 0;

		for (String finding : this.data.findings()) {
			if (Lab.describeFinding(finding) != null) {
				count++;
			}
		}

		return count;
	}

	private List<Block> manual() {
		List<Block> blocks = new ArrayList<>();

		for (String[] section : Journal.MANUAL) {
			List<Element> head = new ArrayList<>(paragraph(styled("contagion.book.manual." + section[0], ChatFormatting.DARK_AQUA, ChatFormatting.BOLD), 0).elements());
			head.add(new Space(3));
			Block first = paragraph(styled("contagion.book.manual." + section[1]), 0);
			// Keep the heading with the start of its first paragraph.
			head.addAll(first.elements().subList(0, Math.min(3, first.elements().size())));
			blocks.add(new Block(head));

			for (Element rest : first.elements().subList(Math.min(3, first.elements().size()), first.elements().size())) {
				blocks.add(single(rest));
			}

			for (int i = 2; i < section.length; i++) {
				blocks.add(single(new Space(5)));
				boolean note = i == section.length - 1 && section.length > 2;
				for (Element line : paragraph(note ? styled("contagion.book.manual." + section[i], ChatFormatting.DARK_GRAY)
						: styled("contagion.book.manual." + section[i]), 0).elements()) {
					blocks.add(single(line));
				}
			}

			blocks.add(single(new Space(12)));
		}

		return blocks;
	}

	private List<Block> reagents() {
		List<Block> blocks = new ArrayList<>();
		blocks.add(paragraph(styled("contagion.book.index", ChatFormatting.DARK_AQUA, ChatFormatting.BOLD), 0));
		blocks.add(single(new Space(3)));
		blocks.add(paragraph(styled("contagion.journal.reagents_hint", ChatFormatting.DARK_GRAY), 0));
		blocks.add(paragraph(Component.literal("☠ ").append(styled("contagion.book.legend.toxic")).withStyle(ChatFormatting.DARK_RED), 0));
		blocks.add(paragraph(Component.literal("✗ ").append(styled("contagion.book.legend.inert")).withStyle(ChatFormatting.DARK_GRAY), 0));
		blocks.add(paragraph(Component.literal("★ ").append(styled("contagion.book.legend.active")).withStyle(ChatFormatting.GOLD), 0));
		blocks.add(single(new Space(8)));

		for (Reagent.Category category : Reagent.Category.values()) {
			boolean active = knows("cat:" + category.name());
			MutableComponent header = Component.empty();

			if (active) {
				header.append(Component.literal("★ ").withStyle(ChatFormatting.GOLD));
			}

			header.append(category.msg().styled(active ? ChatFormatting.GOLD : ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
			List<Element> group = new ArrayList<>(paragraph(header, 0).elements());

			for (Form form : Form.values()) {
				if (knows("form:" + category.name() + ":" + form.name())) {
					group.addAll(paragraph(Msg.of("contagion.journal.prepared_as", form.msg()).styled(ChatFormatting.DARK_PURPLE), 4).elements());
				}
			}

			List<Reagent> members = new ArrayList<>();

			for (Reagent reagent : Reagent.values()) {
				if (reagent.category() == category) {
					members.add(reagent);
				}
			}

			group.add(new ReagentRow(members));
			group.add(new Space(6));
			blocks.add(new Block(group));
		}

		return blocks;
	}

	private List<Block> findings() {
		List<Block> blocks = new ArrayList<>();
		blocks.add(paragraph(styled("contagion.book.known_facts", ChatFormatting.DARK_AQUA, ChatFormatting.BOLD), 0));
		blocks.add(single(new Space(6)));
		boolean any = false;

		for (String finding : this.data.findings()) {
			Msg text = Lab.describeFinding(finding);

			if (text != null) {
				blocks.add(paragraph(Component.literal("• ").append(text.component()), 0));
				blocks.add(single(new Space(5)));
				any = true;
			}
		}

		if (!any) {
			blocks.add(paragraph(styled("contagion.book.nothing_yet", ChatFormatting.GRAY, ChatFormatting.ITALIC), 0));
		}

		return blocks;
	}

	private List<Block> notebook() {
		List<Block> blocks = new ArrayList<>();
		blocks.add(paragraph(styled("contagion.book.notebook", ChatFormatting.DARK_AQUA, ChatFormatting.BOLD), 0));
		blocks.add(paragraph(styled("contagion.book.newest_first", ChatFormatting.GRAY), 0));
		blocks.add(single(new Space(6)));

		if (this.data.log().isEmpty()) {
			blocks.add(paragraph(styled("contagion.book.empty", ChatFormatting.GRAY, ChatFormatting.ITALIC), 0));
		}

		for (int i = this.data.log().size() - 1; i >= 0; i--) {
			Block entry = paragraph(this.data.log().get(i), 0);

			// Short entries stay together; long ones may continue on the next page.
			if (entry.height() <= 50) {
				blocks.add(entry);
			} else {
				for (Element line : entry.elements()) {
					blocks.add(single(line));
				}
			}

			blocks.add(single(new Rule()));
		}

		return blocks;
	}

	private static List<List<Element>> paginate(List<Block> blocks) {
		List<List<Element>> result = new ArrayList<>();
		List<Element> page = new ArrayList<>();
		int used = 0;

		for (Block block : blocks) {
			int height = block.height();

			if (used > 0 && used + height > PAGE_H) {
				result.add(page);
				page = new ArrayList<>();
				used = 0;

				// No blank space at the top of a page.
				if (block.elements().size() == 1 && (block.elements().getFirst() instanceof Space || block.elements().getFirst() instanceof Rule)) {
					continue;
				}
			}

			page.addAll(block.elements());
			used += height;
		}

		if (!page.isEmpty() || result.isEmpty()) {
			result.add(page);
		}

		return result;
	}

	// ---- drawing & input ---------------------------------------------------------------------------------------

	private int pageCount() {
		return this.pages.get(this.tab).size();
	}

	private int spreadCount() {
		return (pageCount() + 1) / 2;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		super.extractBackground(graphics, mouseX, mouseY, partial);
		int x = this.bookX;
		int y = this.bookY;

		for (int i = 0; i < TABS.length; i++) {
			if (i != this.tab) {
				drawTab(graphics, i, false, mouseX, mouseY);
			}
		}

		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, BOOK_W, BOOK_H, 512, 256);
		drawTab(graphics, this.tab, true, mouseX, mouseY);

		List<List<Element>> tabPages = this.pages.get(this.tab);
		int first = this.spread[this.tab] * 2;

		for (int side = 0; side < 2; side++) {
			int index = first + side;

			if (index >= tabPages.size()) {
				break;
			}

			int px = x + (side == 0 ? LEFT_PAGE_X : RIGHT_PAGE_X);
			int py = y + PAGE_TOP;

			for (Element element : tabPages.get(index)) {
				element.draw(graphics, px, py, mouseX, mouseY);
				py += element.height();
			}

			String number = (index + 1) + "";
			graphics.text(this.font, number, px + PAGE_W / 2 - this.font.width(number) / 2, y + ARROW_Y + 1, 0xFF9A8A70, false);
		}

		if (this.spread[this.tab] > 0) {
			drawArrow(graphics, x + 20, y + ARROW_Y, false, mouseX, mouseY);
		}

		if (this.spread[this.tab] < spreadCount() - 1) {
			drawArrow(graphics, x + BOOK_W - 20 - 18, y + ARROW_Y, true, mouseX, mouseY);
		}
	}

	private int tabX(int index) {
		return this.bookX + 16 + index * (TAB_W + 4);
	}

	private void drawTab(GuiGraphicsExtractor graphics, int index, boolean selected, int mouseX, int mouseY) {
		int x = tabX(index);
		int y = this.bookY - (selected ? 22 : 18);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, selected ? 32 : 0, 200, TAB_W, selected ? 26 : 20, 512, 256);
		graphics.item(tabIcon(index), x + 6, y + 3);

		if (mouseX >= x && mouseX < x + TAB_W && mouseY >= y && mouseY < this.bookY) {
			graphics.setTooltipForNextFrame(Msg.of("contagion.journal.tab." + TABS[index]).component(), mouseX, mouseY);
		}
	}

	private static ItemStack tabIcon(int index) {
		return new ItemStack(switch (index) {
			case 0 -> ModItems.RESEARCH_JOURNAL;
			case 1 -> Items.WRITABLE_BOOK;
			case 2 -> ModItems.SERUM;
			case 3 -> ModItems.MICROSCOPE;
			default -> Items.FEATHER;
		});
	}

	private void drawArrow(GuiGraphicsExtractor graphics, int x, int y, boolean right, int mouseX, int mouseY) {
		boolean hovered = mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 11;
		int u = (hovered ? 104 : 64) + (right ? 0 : 20);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, u, 200, 18, 11, 512, 256);
	}

	/** Opens a bookmark tab (also used by the client game test). */
	public void selectTab(int index) {
		if (index >= 0 && index < TABS.length && index != this.tab) {
			this.tab = index;
			lastTab = index;
			pageSound();
		}
	}

	private void turn(int delta) {
		int target = Math.max(0, Math.min(spreadCount() - 1, this.spread[this.tab] + delta));

		if (target != this.spread[this.tab]) {
			this.spread[this.tab] = target;
			pageSound();
		}
	}

	private void pageSound() {
		if (this.minecraft.player != null) {
			this.minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x();
		double my = event.y();

		for (int i = 0; i < TABS.length; i++) {
			int x = tabX(i);

			if (mx >= x && mx < x + TAB_W && my >= this.bookY - 22 && my < this.bookY) {
				selectTab(i);
				return true;
			}
		}

		if (my >= this.bookY + ARROW_Y && my < this.bookY + ARROW_Y + 11) {
			if (mx >= this.bookX + 20 && mx < this.bookX + 38) {
				turn(-1);
				return true;
			}

			if (mx >= this.bookX + BOOK_W - 38 && mx < this.bookX + BOOK_W - 20) {
				turn(1);
				return true;
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		turn(scrollY > 0 ? -1 : 1);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		switch (event.key()) {
			case 263, 266 -> turn(-1); // left, page up
			case 262, 267 -> turn(1); // right, page down
			default -> {
				return super.keyPressed(event);
			}
		}

		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
