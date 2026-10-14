package com.jadescriptz.frogprince;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Small shared helpers: text normalising, number parsing, inventory snapshots. */
final class Util {
	private Util() {}

	/** Strips legacy colour codes, normalises apostrophes and lower-cases. */
	static String norm(String s) {
		if (s == null) {
			return "";
		}
		return s.replaceAll("\u00a7.", "").replace('\u2019', '\'').trim().toLowerCase(Locale.ROOT);
	}

	static boolean onHypixel(Minecraft mc) {
		ServerData sd = mc.getCurrentServer();
		return sd != null && sd.ip != null && sd.ip.toLowerCase(Locale.ROOT).contains("hypixel");
	}

	/** "10M" / "9,876,543" / "1.5K" to a number, or -1 when unreadable. */
	static double parseNum(String digits, String suffix) {
		double v;
		try {
			v = Double.parseDouble(digits.replace(",", ""));
		} catch (NumberFormatException e) {
			return -1;
		}
		switch (suffix.toLowerCase(Locale.ROOT)) {
			case "k" -> v *= 1e3;
			case "m" -> v *= 1e6;
			case "b" -> v *= 1e9;
			default -> {
			}
		}
		return v;
	}

	static String fmt(double v) {
		if (v >= 1e9) {
			return String.format(Locale.ROOT, "%.2fB", v / 1e9);
		}
		if (v >= 1e6) {
			return String.format(Locale.ROOT, "%.1fM", v / 1e6);
		}
		if (v >= 1e3) {
			return String.format(Locale.ROOT, "%.1fK", v / 1e3);
		}
		return String.format(Locale.ROOT, "%.0f", v);
	}

	/** Item keys the mod cares about. */
	static String keyFor(String n) {
		if (n.equals("prince's crown jewel") || n.contains("crown jewel")) {
			return "jewel";
		}
		if (n.equals("gold lotus")) {
			return "gold";
		}
		if (n.equals("silver lotus")) {
			return "silver";
		}
		if (n.equals("aquamarine dye") || n.contains("aquamarine dye")) {
			return "dye";
		}
		return null;
	}

	/** Counts of the tracked drops currently sitting in the player's inventory. */
	static Map<String, Integer> snapshot(Inventory inv) {
		Map<String, Integer> out = new HashMap<>();
		int size = inv.getContainerSize();
		for (int i = 0; i < size; i++) {
			ItemStack st = inv.getItem(i);
			if (st == null || st.isEmpty()) {
				continue;
			}
			String key = keyFor(norm(st.getHoverName().getString()));
			if (key != null) {
				out.merge(key, st.getCount(), Integer::sum);
			}
		}
		return out;
	}

	static int diff(Map<String, Integer> before, Map<String, Integer> after, String key) {
		return after.getOrDefault(key, 0) - before.getOrDefault(key, 0);
	}

	static float clamp01(double v) {
		return (float) Math.max(0.0, Math.min(1.0, v));
	}

	static float smooth(double a, double b, double x) {
		float t = clamp01((x - a) / (b - a));
		return t * t * (3 - 2 * t);
	}
}
