package com.contagion.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import com.contagion.ContagionMod;
import com.contagion.lab.Microscope;
import com.contagion.lab.MicroscopeMenu;
import com.contagion.network.MicroscopePayload;
import com.contagion.registry.ModItems;
import com.contagion.text.Msg;

/**
 * Looking through the microscope: the slide and serum slots on the left, the eyepiece in the middle and a
 * clipboard with notes on the right.
 */
public final class MicroscopeScreen extends AbstractContainerScreen<MicroscopeMenu> {
	private static final Identifier TEXTURE = ContagionMod.id("textures/gui/microscope.png");
	private static final int NOTES_U = 180;
	private static final int NOTES_W = 140;
	private static final int GAP = 2;
	private static final int EYE_X = 108;
	private static final int EYE_Y = 64;
	private static final int EYE_R = 46;
	private static final int TEXT_X = 23;
	private static final int TEXT_W = 106;
	private static final int TEXT_TOP = 21;
	private static final int TEXT_BOTTOM = 210;

	private static MicroscopePayload view = MicroscopePayload.EMPTY;
	private static long viewTime;

	private Button action;
	private int scroll;

	public MicroscopeScreen(MicroscopeMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 222);
	}

	public static void receive(MicroscopePayload payload) {
		Minecraft minecraft = Minecraft.getInstance();
		view = payload;
		viewTime = minecraft.level == null ? 0 : minecraft.level.getGameTime();

		if (minecraft.screen instanceof MicroscopeScreen screen) {
			screen.scroll = 0;
		}
	}

	@Override
	protected void init() {
		super.init();
		this.leftPos = (this.width - this.imageWidth - GAP - NOTES_W) / 2;
		this.action = addRenderableWidget(Button.builder(Msg.of("contagion.microscope.ui.start").component(), button -> press())
				.bounds(this.leftPos + 6, this.topPos + 100, 46, 16).build());
	}

	private void press() {
		if (this.minecraft.gameMode != null) {
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, isReady() ? Microscope.BUTTON_EXAMINE : Microscope.BUTTON_START);
		}
	}

	private double remainingTicks(float partial) {
		if (this.minecraft.level == null) {
			return 0;
		}

		return view.remaining() - (this.minecraft.level.getGameTime() - viewTime) - partial;
	}

	private boolean isRunning() {
		return view.mode() == MicroscopePayload.RUNNING && remainingTicks(0) > 0;
	}

	private boolean isReady() {
		return view.mode() == MicroscopePayload.READY || view.mode() == MicroscopePayload.RUNNING && remainingTicks(0) <= 0;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		boolean ready = isReady();
		this.action.setMessage(Msg.of(ready ? "contagion.microscope.ui.examine" : "contagion.microscope.ui.start").component());
		this.action.active = ready || !isRunning() && MicroscopeMenu.isSlide(this.menu.slide());
		super.extractRenderState(graphics, mouseX, mouseY, partial);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		super.extractBackground(graphics, mouseX, mouseY, partial);
		int x = this.leftPos;
		int y = this.topPos;
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 512, 256);
		int notesX = x + this.imageWidth + GAP;
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, notesX, y, NOTES_U, 0, NOTES_W, this.imageHeight, 512, 256);

		drawEyepiece(graphics, x + EYE_X, y + EYE_Y, partial);
		drawNotes(graphics, notesX, y, partial);
	}

	private void drawEyepiece(GuiGraphicsExtractor graphics, int cx, int cy, float partial) {
		double time = this.minecraft.level == null ? 0 : (this.minecraft.level.getGameTime() + partial) / 20.0;
		Eyepiece eyepiece = new Eyepiece(graphics, cx, cy, EYE_R, time);
		ItemStack slide = this.menu.slide();
		boolean running = isRunning() || isReady();

		if (running) {
			double progress = view.total() <= 0 ? 1 : 1 - Math.max(0, remainingTicks(partial)) / view.total();
			Eyepiece.Scene scene = view.kind() == MicroscopePayload.CULTURE ? Eyepiece.Scene.CULTURE : Eyepiece.Scene.BLOOD;
			eyepiece.draw(scene, 0.3 + 0.6 * progress, null, isReady() ? -1 : progress);
		} else if (!slide.isEmpty()) {
			eyepiece.draw(slide.is(ModItems.CULTURE) ? Eyepiece.Scene.CULTURE : Eyepiece.Scene.BLOOD, 0.3, null, -1);
		} else if (view.mode() == MicroscopePayload.RESULT) {
			eyepiece.draw(view.kind() == MicroscopePayload.CULTURE ? Eyepiece.Scene.CULTURE : Eyepiece.Scene.BLOOD, 1.0, view, -1);
		} else {
			eyepiece.draw(Eyepiece.Scene.EMPTY, 1.0, null, -1);
		}
	}

	private void drawNotes(GuiGraphicsExtractor graphics, int x, int y, float partial) {
		List<Component> paragraphs = new ArrayList<>();

		if (isReady()) {
			paragraphs.add(Msg.of("contagion.microscope.ui.ready").styled(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
			paragraphs.addAll(view.lines());
		} else if (isRunning()) {
			paragraphs.addAll(view.lines());
		} else {
			if (view.has(MicroscopePayload.ERROR) || view.mode() == MicroscopePayload.RESULT) {
				paragraphs.addAll(view.lines());
			}

			if (view.mode() != MicroscopePayload.RESULT) {
				paragraphs.add(Msg.of("contagion.microscope.ui.hint_title").styled(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
				paragraphs.add(Msg.of("contagion.microscope.ui.hint_blood").component());
				paragraphs.add(Msg.of("contagion.microscope.ui.hint_culture").component());
			}
		}

		List<FormattedCharSequence> lines = new ArrayList<>();

		for (Component paragraph : paragraphs) {
			lines.addAll(this.font.split(paragraph, TEXT_W));
			lines.add(FormattedCharSequence.EMPTY);
		}

		int visible = (TEXT_BOTTOM - TEXT_TOP) / 10;
		int maxScroll = Math.max(0, lines.size() - visible);
		this.scroll = Math.max(0, Math.min(this.scroll, maxScroll));
		int lineY = y + TEXT_TOP;

		if (isRunning()) {
			lineY = drawProgress(graphics, x + TEXT_X, lineY, partial);
		}

		for (int i = this.scroll; i < lines.size() && lineY <= y + TEXT_BOTTOM - 9; i++) {
			graphics.text(this.font, lines.get(i), x + TEXT_X, lineY, 0xFF3A3A3A, false);
			lineY += 10;
		}

		if (maxScroll > 0) {
			int barHeight = Math.max(8, (TEXT_BOTTOM - TEXT_TOP) * visible / lines.size());
			int barY = y + TEXT_TOP + (TEXT_BOTTOM - TEXT_TOP - barHeight) * this.scroll / maxScroll;
			graphics.fill(x + NOTES_W - 9, barY, x + NOTES_W - 8, barY + barHeight, 0xFF9A8A70);
		}
	}

	private int drawProgress(GuiGraphicsExtractor graphics, int x, int y, float partial) {
		double remaining = Math.max(0, remainingTicks(partial));
		double progress = view.total() <= 0 ? 1 : 1 - remaining / view.total();
		Component label = Msg.of(view.kind() == MicroscopePayload.CULTURE ? "contagion.progress.culture" : "contagion.progress.analysis").component();
		graphics.text(this.font, label, x, y, 0xFF1E5A6A, false);
		graphics.fill(x, y + 11, x + TEXT_W, y + 17, 0xFF5A4A36);
		graphics.fill(x + 1, y + 12, x + 1 + (int) ((TEXT_W - 2) * progress), y + 16, 0xFF4EB04A);
		Component left = Msg.of("contagion.progress.left", (int) Math.ceil(remaining / 20.0)).component();
		graphics.text(this.font, left, x, y + 20, 0xFF6A6A6A, false);
		return y + 40;
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFE4EAF2, false);
		graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
		graphics.text(this.font, Msg.of("contagion.microscope.ui.slide").component(), MicroscopeMenu.SLIDE_X - 5, MicroscopeMenu.SLIDE_Y - 16, 0xFFB8C4D4, false);
		graphics.text(this.font, Msg.of("contagion.microscope.ui.serum").component(), MicroscopeMenu.SERUM_X - 5, MicroscopeMenu.SERUM_Y - 16, 0xFFB8C4D4, false);
	}

	/** The notes panel counts as part of the screen (so clicking it doesn't drop the carried item). */
	@Override
	protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
		int notesX = this.leftPos + this.imageWidth + GAP;
		boolean onNotes = mouseX >= notesX && mouseX < notesX + NOTES_W && mouseY >= this.topPos && mouseY < this.topPos + this.imageHeight;
		return !onNotes && super.hasClickedOutside(mouseX, mouseY, left, top);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int notesX = this.leftPos + this.imageWidth + GAP;

		if (mouseX >= notesX && mouseX < notesX + NOTES_W && mouseY >= this.topPos && mouseY < this.topPos + this.imageHeight) {
			this.scroll -= (int) Math.signum(scrollY);
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}
}
