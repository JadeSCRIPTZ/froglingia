package com.jadescriptz.frogprince;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Everything the mod draws: the royal gamble, the boss bar, the tally and the toast. */
final class Renderer {
	private Renderer() {}

	private static final int[] CONFETTI = { 0xFFD54F, 0x2ECC71, 0xF8BBD0, 0x4DD0E1 };

	static void render(GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			return;
		}
		drawToast(g, mc);
		drawHpBar(g, mc);
		drawTally(g, mc);
		Show s = FrogPrinceClient.show;
		if (s != null) {
			drawShow(g, mc, s);
		}
	}

	// ---------------------------------------------------------------- helpers

	private static int col(float a, int rgb) {
		int A = Math.max(0, Math.min(255, Math.round(a * 255)));
		return (A << 24) | (rgb & 0xFFFFFF);
	}

	private static float frac(double v) {
		return (float) (v - Math.floor(v));
	}

	private static float rnd(int i, int k) {
		return frac(Math.sin(i * 12.9898 + k * 78.233) * 43758.5453);
	}

	private static MutableComponent comp(String s, boolean bold) {
		MutableComponent c = Component.literal(s);
		return bold ? c.withStyle(ChatFormatting.BOLD) : c;
	}

	/** Draws scaled, horizontally centred text with a shadow. */
	private static void text(GuiGraphicsExtractor g, Font f, String s, float cx, float y, float scale, int color, boolean bold) {
		if (((color >>> 24) & 0xFF) < 4) {
			return;
		}
		MutableComponent c = comp(s, bold);
		float w = f.width(c) * scale;
		g.pose().pushMatrix();
		g.pose().translate(cx - w / 2f, y);
		g.pose().scale(scale, scale);
		g.text(f, c, 0, 0, color, true);
		g.pose().popMatrix();
	}

	private static void ring(GuiGraphicsExtractor g, float cx, float cy, float r, float flat, int color, int step) {
		if (((color >>> 24) & 0xFF) < 4 || r < 2) {
			return;
		}
		int n = Math.max(12, (int) (2 * Math.PI * r / step));
		for (int i = 0; i < n; i++) {
			double a = i * 2 * Math.PI / n;
			int x = (int) (cx + Math.cos(a) * r);
			int y = (int) (cy + Math.sin(a) * r * flat);
			g.fill(x, y, x + 2, y + 2, color);
		}
	}

	// ------------------------------------------------------------------ toast

	private static void drawToast(GuiGraphicsExtractor g, Minecraft mc) {
		String msg = FrogPrinceClient.toast;
		if (msg == null) {
			return;
		}
		double remaining = (FrogPrinceClient.toastUntil - System.nanoTime()) / 1e9;
		if (remaining <= 0) {
			return;
		}
		float a = (float) Math.min(1.0, remaining);
		int w = g.guiWidth();
		int h = g.guiHeight();
		Font f = mc.font;
		float y = h * 0.2f;
		float tw = f.width(msg) * 1.5f;
		Sprites.draw(g, Sprites.FROG, Math.round(w / 2f - tw / 2f - 30), Math.round(y - 6), 2, a);
		text(g, f, msg, w / 2f, y, 1.5f, col(a, 0x9BE564), true);
	}

	// ---------------------------------------------------------------- hp bar

	private static void drawHpBar(GuiGraphicsExtractor g, Minecraft mc) {
		Config c = FrogPrinceClient.config;
		Detector d = FrogPrinceClient.detector;
		if (!c.hpBar || !c.enabled || d.state != Detector.State.FIGHTING || d.hpMax <= 0 || d.hp < 0) {
			return;
		}
		int w = g.guiWidth();
		int bw = 200;
		int bh = 7;
		int x = w / 2 - bw / 2;
		int y = 36;
		float frac = Util.clamp01(d.hp / d.hpMax);
		g.fill(x - 1, y - 1, x + bw + 1, y + bh + 1, col(0.85f, 0x000000));
		g.fill(x, y, x + bw, y + bh, col(1f, 0x1B2A1B));
		int fw = Math.round(bw * frac);
		if (fw > 0) {
			g.fillGradient(x, y, x + fw, y + bh, col(1f, 0x8BE04A), col(1f, 0x2E9E2E));
		}
		String label = "Frog Prince  " + Util.fmt(d.hp) + " / " + Util.fmt(d.hpMax) + "  (" + Math.round(frac * 100) + "%)";
		text(g, mc.font, label, w / 2f, y - 11, 1f, col(1f, 0xDDF5C8), false);
	}

	// ----------------------------------------------------------------- tally

	private static void drawTally(GuiGraphicsExtractor g, Minecraft mc) {
		Config c = FrogPrinceClient.config;
		if (!c.tally || !c.enabled || (c.hypixelOnly && !Util.onHypixel(mc))) {
			return;
		}
		Stats st = FrogPrinceClient.stats;
		java.util.List<String> lines = new java.util.ArrayList<>();
		lines.add("Frog Prince");
		lines.add("Kills: " + st.kills);
		lines.add("Crown Jewels: " + st.jewels);
		lines.add("Since last: " + st.sinceJewel);
		lines.add("Gold Lotus: " + st.goldLotus);
		if (st.dyes > 0) {
			lines.add("Aquamarine Dye: " + st.dyes);
		}
		Font f = mc.font;
		int tw = 0;
		for (int i = 0; i < lines.size(); i++) {
			tw = Math.max(tw, f.width(comp(lines.get(i), i == 0)));
		}
		int pw = tw + 10;
		int ph = lines.size() * 10 + 6;
		int w = g.guiWidth();
		int h = g.guiHeight();
		int m = 4;
		int x = (c.tallyCorner % 2 == 0) ? m : w - pw - m;
		int y = (c.tallyCorner < 2) ? m : h - ph - m;
		g.fill(x, y, x + pw, y + ph, col(0.45f, 0x000000));
		g.fill(x, y, x + 2, y + ph, col(1f, 0xFFD54F));
		for (int i = 0; i < lines.size(); i++) {
			int color = i == 0 ? col(1f, 0xFFD54F) : col(1f, i == 2 ? 0x9BE564 : 0xE8E8E8);
			g.text(f, comp(lines.get(i), i == 0), x + 6, y + 4 + i * 10, color, true);
		}
	}

	// ------------------------------------------------------------------ show

	private static void drawShow(GuiGraphicsExtractor g, Minecraft mc, Show s) {
		Font font = mc.font;
		int w = g.guiWidth();
		int h = g.guiHeight();
		double t = s.time();
		double k = s.k;
		float aIn = Util.smooth(0, 0.45 * k, t);
		float aOut = 1f - Util.smooth(s.length - 0.7 * k, s.length, t);
		float a = Math.min(aIn, aOut);
		if (a <= 0.01f) {
			return;
		}
		boolean win = s.outcome != Outcome.MISS;
		int accent = switch (s.outcome) {
			case JEWEL -> 0xFFD54F;
			case DYE -> 0x4DD0E1;
			case GOLD -> 0xF8BBD0;
			case MISS -> 0x8FBFA0;
		};

		// pond at night
		g.fillGradient(0, 0, w, h, col(a * 0.80f, 0x0A2E1B), col(a * 0.92f, 0x020D07));
		float pondY = h * 0.88f;
		for (int i = 0; i < 3; i++) {
			double ph = (t * 0.45 + i / 3.0) % 1.0;
			ring(g, w / 2f, pondY, (float) (ph * w * 0.38), 0.18f, col(a * (1f - (float) ph) * 0.35f, 0x7FE0A0), 4);
		}
		for (int i = 0; i < 26; i++) {
			float fx = w * (0.5f + 0.47f * (float) Math.sin(t * 0.21 + i * 1.7));
			float fy = h * (0.12f + 0.7f * frac(i * 0.6180339)) + 6f * (float) Math.sin(t * 0.8 + i);
			float tw = 0.5f + 0.5f * (float) Math.sin(t * 2.3 + i * 2.1);
			g.fill((int) fx, (int) fy, (int) fx + 2, (int) fy + 2, col(a * tw * 0.8f, 0xD4FF6A));
		}

		// layout
		int ps = 2;
		for (int cand = 6; cand >= 2; cand--) {
			if (layoutHeight(cand) <= h * 0.94f) {
				ps = cand;
				break;
			}
		}
		int cell = 13 * ps;
		int gap = 2 * ps;
		int reelsW = 3 * cell + 2 * gap;
		int pad = 2 * ps;
		int bw = ps + 2;
		int cx = w / 2;
		int wh = 2 * cell;
		float tScale = Math.max(1f, ps * 0.5f);
		int y0 = Math.max(2, (h - layoutHeight(ps)) / 2);
		int fy1 = y0 + 11 * ps + Math.round(10 * tScale) + 6;
		int ry0 = fy1 + bw + pad;
		int rx0 = cx - reelsW / 2;
		int fx1 = rx0 - pad - bw;
		int fx2 = rx0 + reelsW + pad + bw;
		int fy2 = ry0 + wh + pad + bw;
		float cyMid = ry0 + wh / 2f;
		boolean allStopped = t >= s.lastStop;

		// rays behind the machine once a prize is locked in
		if (win && allStopped) {
			float ra = a * Util.smooth(s.lastStop, s.lastStop + 0.5 * k, t) * 0.14f;
			int rc = col(ra, accent);
			for (int i = 0; i < 14; i++) {
				g.pose().pushMatrix();
				g.pose().translate(cx, cyMid);
				g.pose().rotate((float) (t * 0.25 + i * Math.PI * 2 / 14));
				g.fill(0, -2, (int) (w * 0.7), 2, rc);
				g.pose().popMatrix();
			}
		}

		// frame
		g.fillGradient(fx1, fy1, fx2, fy2, col(a, 0xFFD54F), col(a, 0x9A6A00));
		g.fill(fx1 + bw, fy1 + bw, fx2 - bw, fy2 - bw, col(a, 0x06150C));

		// reels
		for (int i = 0; i < 3; i++) {
			Reel r = s.reels[i];
			int rx = rx0 + i * (cell + gap);
			g.fillGradient(rx, ry0, rx + cell, ry0 + wh, col(a, 0x103D24), col(a, 0x07170D));
			g.enableScissor(rx, ry0, rx + cell, ry0 + wh);
			double c = r.pos(t);
			double speed = Math.abs(r.pos(t + 0.012) - r.pos(t - 0.012)) / 0.024;
			float sa = (float) Math.max(0.45, Math.min(1.0, 1.0 - speed / 45.0));
			int base = (int) Math.floor(c);
			for (int idx = base - 2; idx <= base + 3; idx++) {
				double dy = idx - c;
				if (Math.abs(dy) > 1.7) {
					continue;
				}
				float cy = (float) (cyMid - dy * cell);
				Sprites.draw(g, r.symbolAt(idx), rx + (cell - Sprites.SIZE * ps) / 2,
					Math.round(cy - Sprites.SIZE * ps / 2f), ps, a * sa);
			}
			g.disableScissor();
			g.fillGradient(rx, ry0, rx + cell, ry0 + cell / 2, col(a * 0.75f, 0x000000), col(0f, 0x000000));
			g.fillGradient(rx, ry0 + wh - cell / 2, rx + cell, ry0 + wh, col(0f, 0x000000), col(a * 0.75f, 0x000000));
		}

		// anticipation: two matching reels, third still spinning
		Reel r0 = s.reels[0];
		Reel r1 = s.reels[1];
		Reel r2 = s.reels[2];
		if (r0.stopped(t) && r1.stopped(t) && !r2.stopped(t) && r0.strip[r0.stopIdx] == r1.strip[r1.stopIdx]) {
			float pulse = 0.5f + 0.5f * (float) Math.sin(t * 14);
			int rx = rx0 + 2 * (cell + gap);
			g.fill(rx, ry0, rx + cell, ry0 + wh, col(a * 0.22f * pulse, 0xFFD54F));
		}

		// payline
		int py1 = Math.round(cyMid - cell / 2f);
		int py2 = Math.round(cyMid + cell / 2f);
		int plc = col(a * 0.9f, 0xFFD54F);
		g.fill(rx0 - pad, py1, rx0 + reelsW + pad, py1 + 1, plc);
		g.fill(rx0 - pad, py2 - 1, rx0 + reelsW + pad, py2, plc);
		for (int q = 0; q < ps * 2; q++) {
			g.fill(fx1 - ps * 3 + q, (int) cyMid - (ps * 2 - q), fx1 - ps * 3 + q + 1, (int) cyMid + (ps * 2 - q), plc);
			g.fill(fx2 + ps * 3 - q - 1, (int) cyMid - (ps * 2 - q), fx2 + ps * 3 - q, (int) cyMid + (ps * 2 - q), plc);
		}
		if (win && allStopped) {
			float pulse = 0.35f + 0.25f * (float) Math.sin(t * 9);
			g.fill(rx0, py1, rx0 + reelsW, py2, col(a * pulse * 0.5f, accent));
		}

		// title, crown, result
		float titleA = a * Util.smooth(0.15 * k, 0.6 * k, t);
		text(g, font, "THE FROG PRINCE FALLS", cx, y0, tScale, col(titleA, 0xFFD54F), true);
		if (s.outcome == Outcome.JEWEL && allStopped) {
			float bob = 2f * (float) Math.sin(t * 5);
			Sprites.draw(g, Sprites.CROWN, cx - Sprites.SIZE * ps / 2, Math.round(fy1 - Sprites.SIZE * ps + 3 + bob),
				ps, a * Util.smooth(s.lastStop, s.lastStop + 0.4 * k, t));
		}

		if (allStopped) {
			double tr = t - s.lastStop - 0.2 * k;
			float ra = Math.min(a, Util.smooth(0, 0.35 * k, tr));
			float rs = Math.max(1.5f, ps * 0.7f) * (win ? 1f + 0.04f * (float) Math.sin(t * 8) : 1f);
			float sub = Math.max(1f, ps * 0.4f);
			float y = fy2 + 8;
			String headline = switch (s.outcome) {
				case JEWEL -> "PRINCE'S CROWN JEWEL";
				case DYE -> "AQUAMARINE DYE";
				case GOLD -> "GOLD LOTUS";
				case MISS -> "NO CROWN JEWEL";
			};
			text(g, font, headline, cx, y, rs, col(ra, accent), true);
			y += 10 * rs + 2;
			String line1 = switch (s.outcome) {
				case JEWEL -> "The 0.25% drop landed - confirmed in your loot.";
				case DYE -> "A 1 in 50,000 drop - confirmed in your loot.";
				case GOLD -> "Gold Lotus (5% drop) - confirmed in your loot.";
				case MISS -> "The pond keeps its treasure this time.";
			};
			text(g, font, line1, cx, y, sub, col(ra, 0xE8F5E9), false);
			y += 10 * sub + 2;
			String line2 = s.outcome == Outcome.JEWEL
				? "Took " + s.dry + " kills  \u00b7  kill #" + s.killNo
				: "Kill #" + s.killNo + "  \u00b7  " + s.dry + " since the last Crown Jewel";
			text(g, font, line2, cx, y, sub, col(ra * 0.9f, 0xB7D8B0), false);
			y += 10 * sub + 4;
			text(g, font, dropLine(s.loot), cx, y, sub, col(ra * 0.9f, 0xFFF3C4), false);
			if (s.preview) {
				text(g, font, "PREVIEW - nothing was recorded", cx, y + 10 * sub + 4, 1f, col(ra * 0.8f, 0x9E9E9E), false);
			}
		}

		// confetti
		if (win && allStopped) {
			double dt = t - s.lastStop;
			int count = s.outcome == Outcome.GOLD ? 28 : 70;
			float fade = a * (1f - Util.smooth(1.8 * k, 3.2 * k, dt));
			int size = Math.max(2, ps / 2 + 1);
			for (int i = 0; i < count && fade > 0.02f; i++) {
				float x0 = cx + (rnd(i, 1) - 0.5f) * w * 0.5f;
				float vx = (rnd(i, 2) - 0.5f) * w * 0.25f;
				float vy = -(0.3f + rnd(i, 3) * 0.7f) * h * 0.7f;
				float gy = h * 1.1f;
				float px = x0 + vx * (float) dt;
				float pyy = cyMid + vy * (float) dt + 0.5f * gy * (float) (dt * dt);
				if (pyy > h || pyy < -20) {
					continue;
				}
				g.fill((int) px, (int) pyy, (int) px + size, (int) pyy + size + 2, col(fade, CONFETTI[i % 4]));
			}
		}
	}

	/** Height of the whole composition at a given pixel scale, so it can be sized to fit the screen. */
	private static int layoutHeight(int ps) {
		int cell = 13 * ps;
		float tScale = Math.max(1f, ps * 0.5f);
		float rs = Math.max(1.5f, ps * 0.7f);
		float sub = Math.max(1f, ps * 0.4f);
		int top = 11 * ps + Math.round(10 * tScale) + 6;
		int frame = 2 * cell + 2 * (2 * ps + ps + 2);
		int bottom = Math.round(10 * rs) + 2 + 3 * Math.round(10 * sub + 3) + 14;
		return top + frame + 8 + bottom;
	}

	private static String dropLine(Loot l) {
		java.util.List<String> parts = new java.util.ArrayList<>();
		if (l.silverSeen) {
			parts.add(l.silver > 0 ? "Silver Lotus x" + l.silver : "Silver Lotus");
		}
		if (l.gold) {
			parts.add("Gold Lotus");
		}
		if (l.jewel) {
			parts.add("Prince's Crown Jewel");
		}
		if (l.dye) {
			parts.add("Aquamarine Dye");
		}
		return parts.isEmpty() ? "No drops detected" : String.join("  \u00b7  ", parts);
	}
}
