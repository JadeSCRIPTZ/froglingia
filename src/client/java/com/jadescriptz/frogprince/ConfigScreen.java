package com.jadescriptz.frogprince;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Settings screen, opened with /frogprince. */
final class ConfigScreen extends Screen {
	private static final float[] VOLUMES = { 0.2f, 0.4f, 0.6f, 0.8f, 1.0f };
	private static final int[] WINDOWS = { 4, 6, 8, 10 };
	private static final String[] CORNERS = { "Top left", "Top right", "Bottom left", "Bottom right" };
	private static final String[] ANIMS = { "Full", "Quick", "Off" };

	private final Screen parent;
	private boolean confirmReset;

	ConfigScreen(Screen parent) {
		super(Component.literal("Frog Prince"));
		this.parent = parent;
	}

	private static String onOff(boolean b) {
		return b ? "ON" : "OFF";
	}

	private void add(int col, int row, Supplier<String> label, Runnable action) {
		int bw = 200;
		int x = col == 0 ? this.width / 2 - bw - 5 : this.width / 2 + 5;
		int y = 36 + row * 24;
		Button b = Button.builder(Component.literal(label.get()), btn -> {
			action.run();
			btn.setMessage(Component.literal(label.get()));
		}).bounds(x, y, bw, 20).build();
		this.addRenderableWidget(b);
	}

	@Override
	protected void init() {
		Config c = FrogPrinceClient.config;
		add(0, 0, () -> "Mod: " + onOff(c.enabled), () -> c.enabled = !c.enabled);
		add(1, 0, () -> "Hypixel only: " + onOff(c.hypixelOnly), () -> c.hypixelOnly = !c.hypixelOnly);
		add(0, 1, () -> "Animation: " + ANIMS[c.animation], () -> c.animation = (c.animation + 1) % 3);
		add(1, 1, () -> "Sound: " + onOff(c.sound), () -> c.sound = !c.sound);
		add(0, 2, () -> "Volume: " + Math.round(c.volume * 100) + "%", () -> {
			int next = 0;
			for (int i = 0; i < VOLUMES.length; i++) {
				if (VOLUMES[i] > c.volume + 0.01f) {
					next = i;
					break;
				}
			}
			c.volume = VOLUMES[next];
		});
		add(1, 2, () -> "Tally HUD: " + onOff(c.tally), () -> c.tally = !c.tally);
		add(0, 3, () -> "Tally corner: " + CORNERS[c.tallyCorner], () -> c.tallyCorner = (c.tallyCorner + 1) % 4);
		add(1, 3, () -> "Fight health bar: " + onOff(c.hpBar), () -> c.hpBar = !c.hpBar);
		add(0, 4, () -> "Strict (only with a seen drop): " + onOff(c.strict), () -> c.strict = !c.strict);
		add(1, 4, () -> "Chat summary: " + onOff(c.chatSummary), () -> c.chatSummary = !c.chatSummary);
		add(0, 5, () -> "Drop window: " + c.collectSeconds + "s", () -> {
			int next = 0;
			for (int i = 0; i < WINDOWS.length; i++) {
				if (WINDOWS[i] > c.collectSeconds) {
					next = i;
					break;
				}
			}
			c.collectSeconds = WINDOWS[next];
		});
		add(1, 5, () -> confirmReset ? "Click again to wipe stats" : "Reset stats", () -> {
			if (confirmReset) {
				FrogPrinceClient.stats.reset();
				FrogPrinceClient.stats.save();
				confirmReset = false;
			} else {
				confirmReset = true;
			}
		});
		add(0, 6, () -> "Preview: Crown Jewel", () -> {
			c.save();
			Minecraft.getInstance().setScreen(null);
			FrogPrinceClient.preview(Outcome.JEWEL);
		});
		add(1, 6, () -> "Preview: no drop", () -> {
			c.save();
			Minecraft.getInstance().setScreen(null);
			FrogPrinceClient.preview(Outcome.MISS);
		});

		Button done = Button.builder(Component.literal("Done"), btn -> this.onClose())
			.bounds(this.width / 2 - 100, 36 + 7 * 24 + 6, 200, 20).build();
		this.addRenderableWidget(done);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.centeredText(Minecraft.getInstance().font, Component.literal("Frog Prince \u00b7 Crown Jewel Gamble"),
			this.width / 2, 14, 0xFFFFD54F);
	}

	@Override
	public void onClose() {
		FrogPrinceClient.config.save();
		Minecraft.getInstance().setScreen(this.parent);
	}
}
