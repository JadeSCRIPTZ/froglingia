package com.jadescriptz.frogprince;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/**
 * Frog Prince: turns a Frog Prince kill on the Lotus Atoll into a three-reel royal gamble.
 * The reels only ever land on what really dropped; nothing here talks to the network.
 */
public final class FrogPrinceClient implements ClientModInitializer {
	public static final String ID = "frogprince";

	static Config config;
	static Stats stats;
	static final Detector detector = new Detector();
	static final ConcurrentLinkedQueue<String> chatQueue = new ConcurrentLinkedQueue<>();
	static volatile Show show;
	static volatile String toast;
	static volatile long toastUntil;
	private static volatile boolean openConfigNext;

	@Override
	public void onInitializeClient() {
		config = Config.load();
		stats = Stats.load();

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) {
				chatQueue.add(message.getString());
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(FrogPrinceClient::tick);
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(ID, "royal_court"), (graphics, delta) -> Renderer.render(graphics));
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerCommands(dispatcher));
	}

	private static void tick(Minecraft mc) {
		if (openConfigNext) {
			openConfigNext = false;
			mc.setScreen(new ConfigScreen(null));
		}
		detector.tick(mc);
		Show s = show;
		if (s != null && (s.finished() || mc.level == null)) {
			show = null;
		}
	}

	static void toast(String msg) {
		toast = msg;
		toastUntil = System.nanoTime() + 4_000_000_000L;
	}

	/** Called by the detector once a kill and its drops are settled. */
	static void onKill(Loot loot) {
		stats.record(loot);
		stats.save();
		Outcome o = loot.outcome();
		int dry = o == Outcome.JEWEL ? stats.lastDry : stats.sinceJewel;
		if (config.chatSummary) {
			summary(loot, o, dry);
		}
		if (config.animation != 2) {
			start(new Show(o, loot, config.animation == 1, false, stats.kills, dry));
		}
	}

	static void preview(Outcome o) {
		Loot l = new Loot();
		l.silverSeen = true;
		l.silver = 8;
		l.evidence = true;
		l.jewel = o == Outcome.JEWEL;
		l.gold = o == Outcome.GOLD;
		l.dye = o == Outcome.DYE;
		int dry = o == Outcome.JEWEL ? 400 : stats.sinceJewel;
		start(new Show(o, l, config.animation == 1, true, stats.kills + 1, dry));
	}

	private static void start(Show s) {
		Soundtrack.stop();
		show = s;
		if (config.sound && config.volume > 0f) {
			Soundtrack.playAsync(s, config.volume);
		}
	}

	private static void skip() {
		show = null;
		Soundtrack.stop();
	}

	private static void summary(Loot loot, Outcome o, int dry) {
		Minecraft mc = Minecraft.getInstance();
		MutableComponent line = Component.literal("[Frog Prince] ").withStyle(ChatFormatting.GREEN);
		line.append(Component.literal("Kill #" + stats.kills + " - ").withStyle(ChatFormatting.GRAY));
		switch (o) {
			case JEWEL -> line.append(Component.literal("Prince's Crown Jewel! (took " + dry + " kills)").withStyle(ChatFormatting.GOLD));
			case DYE -> line.append(Component.literal("Aquamarine Dye!").withStyle(ChatFormatting.AQUA));
			case GOLD -> line.append(Component.literal("Gold Lotus").withStyle(ChatFormatting.LIGHT_PURPLE));
			case MISS -> line.append(Component.literal("no Crown Jewel (" + dry + " since the last one)").withStyle(ChatFormatting.GRAY));
		}
		mc.gui.getChat().addClientSystemMessage(line);
	}

	// --------------------------------------------------------------- commands

	private static void registerCommands(CommandDispatcher<FabricClientCommandSource> d) {
		d.register(ClientCommands.literal("frogprince")
			.executes(ctx -> {
				openConfigNext = true;
				return 1;
			})
			.then(ClientCommands.literal("test")
				.executes(ctx -> {
					preview(Outcome.JEWEL);
					return 1;
				})
				.then(ClientCommands.literal("jewel").executes(ctx -> {
					preview(Outcome.JEWEL);
					return 1;
				}))
				.then(ClientCommands.literal("gold").executes(ctx -> {
					preview(Outcome.GOLD);
					return 1;
				}))
				.then(ClientCommands.literal("dye").executes(ctx -> {
					preview(Outcome.DYE);
					return 1;
				}))
				.then(ClientCommands.literal("miss").executes(ctx -> {
					preview(Outcome.MISS);
					return 1;
				})))
			.then(ClientCommands.literal("skip").executes(ctx -> {
				skip();
				return 1;
			}))
			.then(ClientCommands.literal("stats").executes(ctx -> {
				Stats s = stats;
				ctx.getSource().sendFeedback(Component.literal(
					"Frog Prince - kills: " + s.kills + ", Crown Jewels: " + s.jewels
						+ ", Gold Lotus: " + s.goldLotus + ", Aquamarine Dye: " + s.dyes
						+ ", since last jewel: " + s.sinceJewel + ", longest dry spell: " + s.longestDry));
				return 1;
			}))
			.then(ClientCommands.literal("reset").executes(ctx -> {
				stats.reset();
				stats.save();
				ctx.getSource().sendFeedback(Component.literal("Frog Prince - stats reset."));
				return 1;
			})));
	}
}
