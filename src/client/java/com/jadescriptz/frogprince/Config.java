package com.jadescriptz.frogprince;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/** User settings, saved as config/frogprince/config.json. */
public final class Config {
	public boolean enabled = true;
	public boolean hypixelOnly = true;
	/** 0 = full, 1 = quick, 2 = off (stats and chat line only). */
	public int animation = 0;
	public boolean sound = true;
	public float volume = 0.6f;
	public boolean tally = true;
	/** 0 top-left, 1 top-right, 2 bottom-left, 3 bottom-right. */
	public int tallyCorner = 1;
	public boolean hpBar = true;
	/** Only play the show when a drop was actually seen after the kill. */
	public boolean strict = false;
	public boolean chatSummary = true;
	public int collectSeconds = 6;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	static Path dir() {
		Path d = FabricLoader.getInstance().getConfigDir().resolve("frogprince");
		try {
			Files.createDirectories(d);
		} catch (Exception ignored) {
		}
		return d;
	}

	static Config load() {
		Path f = dir().resolve("config.json");
		Config c = null;
		if (Files.exists(f)) {
			try (Reader r = Files.newBufferedReader(f)) {
				c = GSON.fromJson(r, Config.class);
			} catch (Exception e) {
				System.err.println("[FrogPrince] could not read config.json, using defaults: " + e);
			}
		}
		if (c == null) {
			c = new Config();
		}
		c.animation = Math.max(0, Math.min(2, c.animation));
		c.tallyCorner = Math.max(0, Math.min(3, c.tallyCorner));
		c.volume = Math.max(0f, Math.min(1f, c.volume));
		c.collectSeconds = Math.max(2, Math.min(20, c.collectSeconds));
		return c;
	}

	void save() {
		try (Writer w = Files.newBufferedWriter(dir().resolve("config.json"))) {
			GSON.toJson(this, w);
		} catch (Exception e) {
			System.err.println("[FrogPrince] could not save config.json: " + e);
		}
	}
}
