package com.jadescriptz.frogprince;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/**
 * Watches for a Frog Prince on the line, follows its nameplate while you fight it, and decides
 * when it died. After a kill it collects drop evidence (inventory gains and drop lines in chat)
 * so the result of the show only ever reflects drops that really arrived.
 */
final class Detector {
	enum State { IDLE, FIGHTING, COLLECTING }

	static final Pattern HP = Pattern.compile(
		"([0-9][0-9,.]*)\\s*([kKmMbB]?)\\s*/\\s*([0-9][0-9,.]*)\\s*([kKmMbB]?)");

	private record Ev(String kind, long tick) {}

	volatile State state = State.IDLE;
	/** Nameplate health of the Frog Prince you are fighting, -1 when unreadable. */
	volatile double hp = -1;
	volatile double hpMax = -1;

	private long tick;
	private long armedUntil;
	private long lastSeen;
	private double lastDist;
	private long killTick;
	private long collectUntil;
	private Map<String, Integer> baseline = new HashMap<>();
	private final ArrayDeque<Ev> evidence = new ArrayDeque<>();

	boolean armed() {
		return tick < armedUntil;
	}

	void reset() {
		state = State.IDLE;
		hp = -1;
		hpMax = -1;
		armedUntil = 0;
		evidence.clear();
	}

	void tick(Minecraft mc) {
		tick++;
		String line;
		while ((line = FrogPrinceClient.chatQueue.poll()) != null) {
			onChat(Util.norm(line));
		}
		Config c = FrogPrinceClient.config;
		if (mc.level == null || mc.player == null) {
			reset();
			return;
		}
		if (!c.enabled || (c.hypixelOnly && !Util.onHypixel(mc))) {
			reset();
			return;
		}
		while (!evidence.isEmpty() && tick - evidence.peekFirst().tick() > 400) {
			evidence.pollFirst();
		}
		if (tick % 2 == 0) {
			scan(mc, c);
		}
		if (state == State.COLLECTING && tick >= collectUntil) {
			finish(mc, c);
		}
	}

	private void onChat(String s) {
		if (s.contains("bow down before the frog prince")) {
			armedUntil = tick + 20L * 360;
			FrogPrinceClient.toast("Frog Prince on the line - bow down!");
			return;
		}
		if (s.contains(": ")) {
			return; // player chat, never a drop line
		}
		if (s.contains("crown jewel") && (s.contains("drop") || s.contains("found") || s.contains("caught"))) {
			evidence.addLast(new Ev("jewel", tick));
		}
		if (s.contains("gold lotus") && s.contains("drop")) {
			evidence.addLast(new Ev("gold", tick));
		}
		if (s.contains("aquamarine dye") && s.contains("drop")) {
			evidence.addLast(new Ev("dye", tick));
		}
		if (s.contains("silver lotus") && !s.contains("sold") && !s.contains("bought") && !s.contains("purchased")) {
			evidence.addLast(new Ev("silver", tick));
		}
	}

	private boolean hasEvidence(String kind, long from, long to) {
		for (Ev e : evidence) {
			if (e.kind().equals(kind) && e.tick() >= from && e.tick() <= to) {
				return true;
			}
		}
		return false;
	}

	private void scan(Minecraft mc, Config c) {
		double bestD = Double.MAX_VALUE;
		double bestHp = -1;
		double bestMax = -1;
		boolean found = false;
		for (Entity e : mc.level.entitiesForRendering()) {
			Component name = e.getCustomName();
			if (name == null) {
				continue;
			}
			String s = Util.norm(name.getString());
			if (!s.contains("frog prince")) {
				continue;
			}
			if (!s.contains("lv") && !s.contains("\u2764")) {
				continue; // a nameplate always carries a level or hearts
			}
			double d = e.distanceToSqr(mc.player);
			if (d < bestD) {
				bestD = d;
				found = true;
				bestHp = -1;
				bestMax = -1;
				Matcher m = HP.matcher(s);
				if (m.find()) {
					bestHp = Util.parseNum(m.group(1), m.group(2));
					bestMax = Util.parseNum(m.group(3), m.group(4));
				}
			}
		}
		if (found) {
			double dist = Math.sqrt(bestD);
			if (state == State.IDLE && (armed() || dist <= 10)) {
				state = State.FIGHTING;
				armedUntil = Math.max(armedUntil, tick + 20L * 60);
				baseline = Util.snapshot(mc.player.getInventory());
				lastSeen = tick;
			}
			if (state == State.FIGHTING) {
				lastSeen = tick;
				lastDist = dist;
				hp = bestHp;
				hpMax = bestMax;
				if (tick % 10 == 0) {
					baseline = Util.snapshot(mc.player.getInventory());
				}
			}
		} else if (state == State.FIGHTING && tick - lastSeen >= 12) {
			hp = -1;
			hpMax = -1;
			if (lastDist <= 48) {
				state = State.COLLECTING;
				killTick = lastSeen;
				collectUntil = tick + 20L * c.collectSeconds;
			} else {
				state = State.IDLE; // walked away, not a kill
			}
		}
	}

	private void finish(Minecraft mc, Config c) {
		Map<String, Integer> now = Util.snapshot(mc.player.getInventory());
		long from = killTick - 60;
		long to = tick + 5;
		Loot loot = new Loot();
		loot.jewel = Util.diff(baseline, now, "jewel") > 0 || hasEvidence("jewel", from, to);
		loot.gold = Util.diff(baseline, now, "gold") > 0 || hasEvidence("gold", from, to);
		loot.dye = Util.diff(baseline, now, "dye") > 0 || hasEvidence("dye", from, to);
		int silver = Util.diff(baseline, now, "silver");
		loot.silverSeen = silver > 0 || hasEvidence("silver", from, to);
		loot.silver = Math.max(0, silver);
		loot.evidence = loot.silverSeen || loot.jewel || loot.gold || loot.dye;
		state = State.IDLE;
		armedUntil = 0;
		evidence.clear();
		if (c.strict && !loot.evidence) {
			return;
		}
		FrogPrinceClient.onKill(loot);
	}
}
