package com.contagion.client;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import com.contagion.lab.Lab;
import com.contagion.lab.ThermometerItem;
import com.contagion.network.ThermometerPayload;
import com.contagion.registry.ModItems;
import com.contagion.text.Msg;

/**
 * A glass thermometer next to the crosshair: while charging the mercury climbs, then it settles on the
 * reading for a few seconds.
 */
public final class ThermometerHud {
	private static final int SHOW_TICKS = 100;
	private static final double MIN_C = 35.0;
	private static final double MAX_C = 42.0;
	private static final int[] LEVEL_COLORS = {0xFF55FF55, 0xFFFFFF55, 0xFFFFAA00, 0xFFFF5555, 0xFFAA0000};

	private static @Nullable ThermometerPayload reading;
	private static long readingTime;

	private ThermometerHud() {
	}

	public static void receive(ThermometerPayload payload) {
		Minecraft minecraft = Minecraft.getInstance();
		reading = payload;
		readingTime = minecraft.level == null ? 0 : minecraft.level.getGameTime();
	}

	public static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;

		if (player == null || minecraft.level == null) {
			return;
		}

		float partial = delta.getGameTimeDeltaPartialTick(false);
		boolean charging = player.isUsingItem() && player.getUseItem().is(ModItems.THERMOMETER);
		long age = minecraft.level.getGameTime() - readingTime;
		boolean showing = reading != null && age >= 0 && age < SHOW_TICKS && !charging;

		if (!charging && !showing) {
			return;
		}

		int x = graphics.guiWidth() / 2 + 16;
		int y = graphics.guiHeight() / 2 - 34;
		double fill;
		int mercury = 0xFFD8262C;
		Font font = minecraft.font;

		if (charging) {
			float progress = Math.min(1.0F, (player.getTicksUsingItem() + partial) / ThermometerItem.MEASURE_TICKS);
			// Climbs quickly, then hesitates while it settles.
			double eased = 1 - Math.pow(1 - progress, 3);
			fill = 0.12 + 0.45 * eased + 0.03 * Math.sin((player.tickCount + partial) * 0.6) * progress;
			drawTube(graphics, x, y, fill, mercury);
			Component text = Msg.of("contagion.thermometer.hud.measuring").component();
			graphics.text(font, text, x + 16, y + 26, 0xFFE0E0E0, true);
			drawCharge(graphics, x + 16, y + 37, progress);
			return;
		}

		double c = reading.celsius();
		int level = Lab.temperatureLevel(c);
		fill = Math.max(0.05, Math.min(1.0, (c - MIN_C) / (MAX_C - MIN_C)));
		float alpha = age > SHOW_TICKS - 20 ? (SHOW_TICKS - age - partial) / 20.0F : 1.0F;

		if (alpha <= 0.05F) {
			return;
		}

		drawTube(graphics, x, y, fill, mercury);
		int color = LEVEL_COLORS[level];
		graphics.text(font, reading.subject(), x + 16, y + 14, withAlpha(0xFFE0E0E0, alpha), true);
		graphics.text(font, String.format(Locale.ROOT, "%.1f°C", c), x + 16, y + 25, withAlpha(color, alpha), true);
		graphics.text(font, Msg.of("contagion.thermometer." + Lab.TEMPERATURE_LABELS[level]).component(), x + 16, y + 36, withAlpha(color, alpha), true);
	}

	private static int withAlpha(int color, float alpha) {
		int a = Math.max(5, Math.min(255, (int) (alpha * 255)));
		return (a << 24) | (color & 0xFFFFFF);
	}

	/** A small segmented bar under the text while charging. */
	private static void drawCharge(GuiGraphicsExtractor g, int x, int y, float progress) {
		g.fill(x - 1, y - 1, x + 51, y + 4, 0xC0000000);
		int filled = (int) (progress * 10);

		for (int i = 0; i < 10; i++) {
			g.fill(x + i * 5, y, x + i * 5 + 4, y + 3, i < filled ? 0xFFE04040 : 0xFF3A3A3A);
		}
	}

	/** Glass tube (8 x 52) with a bulb, scale marks and a red column. */
	private static void drawTube(GuiGraphicsExtractor g, int x, int y, double fill, int mercury) {
		int tubeTop = y;
		int tubeBottom = y + 44;
		// Shadowed backing
		g.fill(x - 2, tubeTop - 2, x + 9, tubeBottom + 2, 0x80000000);
		// Glass
		g.fill(x, tubeTop, x + 7, tubeBottom, 0xC0E8F4F8);
		g.fill(x + 1, tubeTop + 1, x + 6, tubeBottom, 0xFF2A3036);
		// Column
		int columnHeight = (int) Math.round((tubeBottom - tubeTop - 3) * Math.max(0, Math.min(1, fill)));
		g.fill(x + 2, tubeBottom - columnHeight, x + 5, tubeBottom, mercury);
		g.fill(x + 2, tubeBottom - columnHeight, x + 3, tubeBottom, 0xFFFF7070);
		// Scale marks: every whole degree, with the 37 °C mark highlighted.
		for (int deg = 36; deg <= 41; deg++) {
			int my = tubeBottom - 3 - (int) Math.round((tubeBottom - tubeTop - 3) * (deg - MIN_C) / (MAX_C - MIN_C));
			g.fill(x + 7, my, deg == 37 ? x + 11 : x + 9, my + 1, deg == 37 ? 0xFF7CFF7C : 0xFFC8C8C8);
		}

		// Bulb
		int bx = x + 3;
		int by = tubeBottom + 4;
		disc(g, bx, by, 5, 0xC0E8F4F8);
		disc(g, bx, by, 4, mercury);
		g.fill(bx - 2, by - 2, bx, by, 0xFFFF8A8A);
		g.fill(x + 2, tubeBottom - 1, x + 5, by, mercury);
		// Glint on the glass
		g.fill(x + 1, tubeTop + 2, x + 2, tubeBottom - 4, 0x60FFFFFF);
	}

	static void disc(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
		for (int dy = -r; dy <= r; dy++) {
			int half = (int) Math.floor(Math.sqrt(r * r - dy * dy + 0.5));
			g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
		}
	}
}
