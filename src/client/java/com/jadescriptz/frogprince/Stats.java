package com.jadescriptz.frogprince;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** Lifetime kill and drop counters, saved as config/frogprince/stats.json. */
public final class Stats {
	public int kills;
	public int jewels;
	public int goldLotus;
	public int dyes;
	/** Kills since the last Crown Jewel (0 right after one dropped). */
	public int sinceJewel;
	/** How many kills the most recent Crown Jewel took. */
	public int lastDry;
	public int longestDry;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	static Stats load() {
		Path f = Config.dir().resolve("stats.json");
		Stats s = null;
		if (Files.exists(f)) {
			try (Reader r = Files.newBufferedReader(f)) {
				s = GSON.fromJson(r, Stats.class);
			} catch (Exception e) {
				System.err.println("[FrogPrince] could not read stats.json: " + e);
			}
		}
		return s == null ? new Stats() : s;
	}

	void save() {
		try (Writer w = Files.newBufferedWriter(Config.dir().resolve("stats.json"))) {
			GSON.toJson(this, w);
		} catch (Exception e) {
			System.err.println("[FrogPrince] could not save stats.json: " + e);
		}
	}

	void record(Loot loot) {
		kills++;
		if (loot.gold) {
			goldLotus++;
		}
		if (loot.dye) {
			dyes++;
		}
		if (loot.jewel) {
			jewels++;
			lastDry = sinceJewel + 1;
			longestDry = Math.max(longestDry, lastDry);
			sinceJewel = 0;
		} else {
			sinceJewel++;
			longestDry = Math.max(longestDry, sinceJewel);
		}
	}

	void reset() {
		kills = 0;
		jewels = 0;
		goldLotus = 0;
		dyes = 0;
		sinceJewel = 0;
		lastDry = 0;
		longestDry = 0;
	}
}
