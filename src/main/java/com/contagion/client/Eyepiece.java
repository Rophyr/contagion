package com.contagion.client;

import java.util.Random;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import com.contagion.network.MicroscopePayload;

/**
 * Draws the view through the microscope's eyepiece: blood cells or a culture, drifting under the lens.
 * Everything is clipped to the round viewport. Nothing is shown that the players haven't found out yet:
 * the pathogen only appears once an analysis result is in.
 */
final class Eyepiece {
	enum Scene {
		EMPTY,
		BLOOD,
		CULTURE
	}

	private final GuiGraphicsExtractor g;
	private final int cx;
	private final int cy;
	private final int radius;
	private final double time;

	Eyepiece(GuiGraphicsExtractor g, int cx, int cy, int radius, double time) {
		this.g = g;
		this.cx = cx;
		this.cy = cy;
		this.radius = radius;
		this.time = time;
	}

	/**
	 * @param focus  0 = blurry (not examined yet), 1 = sharp
	 * @param result the result to picture, or null to only show the sample
	 * @param scan   progress of a running job (draws a scanning beam), or a negative value
	 */
	void draw(Scene scene, double focus, MicroscopePayload result, double scan) {
		switch (scene) {
			case EMPTY -> drawEmpty();
			case BLOOD -> drawBlood(focus, result);
			case CULTURE -> drawCulture(focus, result, scan);
		}

		if (scan >= 0) {
			drawScan(scan);
		}

		drawLens();
	}

	// ---- scenes ------------------------------------------------------------------------------------------------

	private void drawEmpty() {
		blob(this.cx, this.cy, this.radius, 0xFF26282C);
		blob(this.cx - 8, this.cy - 10, this.radius - 14, 0xFF2E3136);
		Random random = new Random(3);

		for (int i = 0; i < 14; i++) {
			int x = this.cx + random.nextInt(this.radius * 2) - this.radius;
			int y = this.cy + random.nextInt(this.radius * 2) - this.radius;
			blob(x, y, random.nextInt(2), 0xFF4A4E56);
		}
	}

	private void drawBlood(double focus, MicroscopePayload result) {
		boolean degraded = result != null && result.has(MicroscopePayload.DEGRADED);
		blob(this.cx, this.cy, this.radius, degraded ? 0xFFC4AE84 : 0xFFF1DEB4);
		blob(this.cx + 6, this.cy + 8, this.radius - 10, degraded ? 0xFFCAB48C : 0xFFF5E4BE);

		Random random = new Random(11);
		double blur = 1.0 - focus;
		int alpha = (int) (110 + 145 * focus);

		// White blood cells
		for (int i = 0; i < 2; i++) {
			double[] p = drift(random, 0.8);
			int r = 7 + (int) Math.round(blur * 3);
			blob(p[0], p[1], r, argb(alpha, 0xE4D8EE));
			blob(p[0] - 2, p[1] - 1, 2 + (int) Math.round(blur), argb(alpha, 0x7A4AA0));
			blob(p[0] + 2, p[1] + 1, 2 + (int) Math.round(blur), argb(alpha, 0x8A58B0));
		}

		// Red blood cells: pale centers (biconcave discs); degraded blood is shriveled and brown.
		for (int i = 0; i < 28; i++) {
			double[] p = drift(random, 1.6);

			if (degraded) {
				blob(p[0], p[1], 3, argb(alpha, 0x8A4A2A));
				blob(p[0] + 2, p[1] - 1, 1, argb(alpha, 0x6A3A20));
			} else {
				int r = 5 + (int) Math.round(blur * 2);
				blob(p[0], p[1], r, argb(alpha, 0xB8262A));
				blob(p[0], p[1], r - 1, argb(alpha, 0xCC3434));
				blob(p[0] - 0.5, p[1] - 0.5, Math.max(1, r - 3), argb(alpha, 0xE2685E));
			}
		}

		if (result == null || degraded) {
			return;
		}

		if (result.has(MicroscopePayload.ANTIBODIES)) {
			Random antibodies = new Random(29);

			for (int i = 0; i < 16; i++) {
				double[] p = drift(antibodies, 2.4);
				antibody((int) p[0], (int) p[1]);
			}
		}

		if (result.has(MicroscopePayload.POSITIVE)) {
			Random pathogens = new Random(41);
			int count = 6 + result.load() * 8;
			int highlighted = -1;

			if (result.has(MicroscopePayload.INSIGHT)) {
				highlighted = 0;
			}

			for (int i = 0; i < count; i++) {
				double[] p = drift(pathogens, 3.0);
				double jx = Math.sin(this.time * 2.3 + i * 1.7) * 1.2;
				double jy = Math.cos(this.time * 2.9 + i * 2.3) * 1.2;
				virion(p[0] + jx, p[1] + jy);

				if (i == highlighted) {
					double pulse = 5 + Math.sin(this.time * 4) * 1.5;
					ring(p[0] + jx, p[1] + jy, pulse, 0xC0FFD040);
				}
			}
		}
	}

	private void drawCulture(double focus, MicroscopePayload result, double scan) {
		blob(this.cx, this.cy, this.radius, 0xFFE2CB80);
		blob(this.cx - 4, this.cy - 6, this.radius - 8, 0xFFE8D38E);
		ring(this.cx, this.cy, this.radius - 3, 0x60FFFFFF);

		double blur = 1.0 - focus;
		int alpha = (int) (120 + 135 * focus);
		int colonies = 9;
		int dead = result == null ? 0 : (int) Math.round(colonies * result.active() / 3.0);
		Random random = new Random(17);

		// Healthy tissue strip at the bottom (where toxicity shows).
		int tissueTop = this.cy + this.radius / 3;
		Random tissue = new Random(23);

		for (int row = 0; row < 3; row++) {
			for (int col = -6; col <= 6; col++) {
				int x = this.cx + col * 8 + (row % 2) * 4;
				int y = tissueTop + row * 7 + 4;
				int roll = tissue.nextInt(3);
				boolean damaged = result != null && roll < result.toxic() + (result.toxic() > 1 ? 1 : 0);
				blob(x, y, 3, argb(alpha, damaged ? 0x5E4040 : 0xEFA4B2));
				blob(x - 1, y - 1, 1, argb(alpha, damaged ? 0x3A2626 : 0xC86A80));
			}
		}

		for (int i = 0; i < colonies; i++) {
			double x = this.cx + random.nextDouble() * this.radius * 1.4 - this.radius * 0.7;
			double y = this.cy - this.radius * 0.75 + random.nextDouble() * this.radius * 0.95;
			boolean isDead = i < dead;
			int lobes = 5 + random.nextInt(4);
			double breathe = isDead ? 0 : Math.sin(this.time * 1.2 + i) * 0.6;

			for (int lobe = 0; lobe < lobes; lobe++) {
				double angle = random.nextDouble() * Math.PI * 2;
				double dist = random.nextDouble() * 5;
				int r = (isDead ? 2 : 3 + random.nextInt(3)) + (int) Math.round(blur * 2);
				int color = isDead ? 0x8C867A : lobe % 2 == 0 ? 0x5E8E30 : 0x76A43E;
				blob(x + Math.cos(angle) * dist, y + Math.sin(angle) * dist + breathe, r, argb(alpha, color));
			}

			if (!isDead) {
				blob(x, y, 1, argb(alpha, 0x3E6A1E));
			}
		}

		if (scan >= 0) {
			// The serum spreading over the dish.
			double r = scan * this.radius;
			blob(this.cx, this.cy, (int) r, 0x284A86D0);
			ring(this.cx, this.cy, r, 0x605A96E0);
		}

		if (result != null && result.has(MicroscopePayload.TRACE)) {
			for (int i = 0; i < 24; i++) {
				double angle = i / 24.0 * Math.PI * 2 + this.time * 0.5;
				double r = this.radius - 6 + Math.sin(this.time * 3 + i) * 1.5;
				blob(this.cx + Math.cos(angle) * r, this.cy + Math.sin(angle) * r, 1, 0xC0B060E0);
			}
		}
	}

	private void drawScan(double progress) {
		double sweep = (this.time * 0.6) % 2.0;
		double y = this.cy - this.radius + (sweep < 1 ? sweep : 2 - sweep) * this.radius * 2;
		rect(this.cx - this.radius, (int) y - 1, this.cx + this.radius, (int) y + 2, 0x3050E0FF);
		rect(this.cx - this.radius, (int) y, this.cx + this.radius, (int) y + 1, 0x6080F0FF);
	}

	/** Reticle, vignette and a little glare: makes it feel like looking down a lens. */
	private void drawLens() {
		for (int x = this.cx - this.radius + 8; x < this.cx + this.radius - 8; x += 6) {
			rect(x, this.cy - (x % 12 == 0 ? 2 : 1), x + 1, this.cy + (x % 12 == 0 ? 3 : 2), 0x50202020);
		}

		rect(this.cx - this.radius + 6, this.cy, this.cx + this.radius - 6, this.cy + 1, 0x30202020);
		rect(this.cx, this.cy - this.radius + 6, this.cx + 1, this.cy + this.radius - 6, 0x30202020);

		for (int i = 0; i < 6; i++) {
			annulus(this.radius - i, this.radius - i - 1, ((120 - i * 20) << 24));
		}

		blob(this.cx - this.radius / 2, this.cy - this.radius / 2, 5, 0x22FFFFFF);
		blob(this.cx - this.radius / 2 + 6, this.cy - this.radius / 2 - 3, 2, 0x30FFFFFF);
	}

	// ---- shapes ------------------------------------------------------------------------------------------------

	private void virion(double x, double y) {
		int ix = (int) Math.round(x);
		int iy = (int) Math.round(y);
		blob(ix, iy, 2, 0xFF3C8A2A);
		blob(ix, iy, 1, 0xFF5CB83A);
		rect(ix - 4, iy, ix - 2, iy + 1, 0xFF2A6A1E);
		rect(ix + 3, iy, ix + 5, iy + 1, 0xFF2A6A1E);
		rect(ix, iy - 4, ix + 1, iy - 2, 0xFF2A6A1E);
		rect(ix, iy + 3, ix + 1, iy + 5, 0xFF2A6A1E);
	}

	private void antibody(int x, int y) {
		rect(x, y, x + 1, y + 3, 0xFF2E5EC8);
		rect(x - 1, y - 1, x, y, 0xFF2E5EC8);
		rect(x + 1, y - 1, x + 2, y, 0xFF2E5EC8);
		rect(x - 2, y - 2, x - 1, y - 1, 0xFF4A7AE0);
		rect(x + 2, y - 2, x + 3, y - 1, 0xFF4A7AE0);
	}

	/** Slowly drifting position inside the viewport (wraps around). */
	private double[] drift(Random random, double speed) {
		double span = this.radius * 2 + 16;
		double bx = random.nextDouble() * span;
		double by = random.nextDouble() * span;
		double angle = random.nextDouble() * Math.PI * 2;
		double v = (0.4 + random.nextDouble()) * speed;
		double x = floorMod(bx + Math.cos(angle) * v * this.time, span);
		double y = floorMod(by + Math.sin(angle) * v * this.time, span);
		return new double[] {this.cx - this.radius - 8 + x, this.cy - this.radius - 8 + y};
	}

	private static double floorMod(double value, double span) {
		double m = value % span;
		return m < 0 ? m + span : m;
	}

	private static int argb(int alpha, int rgb) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
	}

	/** Half the viewport's width at screen row y, or -1 outside it. */
	private int viewHalf(int y) {
		int dy = y - this.cy;

		if (Math.abs(dy) > this.radius) {
			return -1;
		}

		return (int) Math.floor(Math.sqrt(this.radius * this.radius - dy * dy));
	}

	/** Filled disc, clipped to the viewport. */
	private void blob(double x, double y, int r, int color) {
		int ix = (int) Math.round(x);
		int iy = (int) Math.round(y);

		for (int dy = -r; dy <= r; dy++) {
			int row = iy + dy;
			int view = viewHalf(row);

			if (view < 0) {
				continue;
			}

			int half = (int) Math.floor(Math.sqrt(r * r - dy * dy + 0.5));
			int x0 = Math.max(ix - half, this.cx - view);
			int x1 = Math.min(ix + half + 1, this.cx + view + 1);

			if (x0 < x1) {
				this.g.fill(x0, row, x1, row + 1, color);
			}
		}
	}

	/** One-pixel circle outline, clipped to the viewport. */
	private void ring(double x, double y, double r, int color) {
		int steps = Math.max(12, (int) (r * 6));

		for (int i = 0; i < steps; i++) {
			double angle = i / (double) steps * Math.PI * 2;
			int px = (int) Math.round(x + Math.cos(angle) * r);
			int py = (int) Math.round(y + Math.sin(angle) * r);
			int view = viewHalf(py);

			if (view >= 0 && Math.abs(px - this.cx) <= view) {
				this.g.fill(px, py, px + 1, py + 1, color);
			}
		}
	}

	/** Rectangle clipped to the viewport row by row. */
	private void rect(int x0, int y0, int x1, int y1, int color) {
		for (int row = y0; row < y1; row++) {
			int view = viewHalf(row);

			if (view < 0) {
				continue;
			}

			int a = Math.max(x0, this.cx - view);
			int b = Math.min(x1, this.cx + view + 1);

			if (a < b) {
				this.g.fill(a, row, b, row + 1, color);
			}
		}
	}

	/** Ring between two radii around the viewport center (for the vignette). */
	private void annulus(int outer, int inner, int color) {
		for (int dy = -outer; dy <= outer; dy++) {
			int ho = (int) Math.floor(Math.sqrt(outer * outer - dy * dy));
			int hi = Math.abs(dy) <= inner ? (int) Math.floor(Math.sqrt(inner * inner - dy * dy)) : -1;
			int row = this.cy + dy;

			if (hi < 0) {
				this.g.fill(this.cx - ho, row, this.cx + ho + 1, row + 1, color);
			} else {
				this.g.fill(this.cx - ho, row, this.cx - hi, row + 1, color);
				this.g.fill(this.cx + hi + 1, row, this.cx + ho + 1, row + 1, color);
			}
		}
	}
}
