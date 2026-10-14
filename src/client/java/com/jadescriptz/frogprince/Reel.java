package com.jadescriptz.frogprince;

import java.util.Random;

/** One slot reel: a looping strip of symbols that decelerates onto a chosen symbol. */
final class Reel {
	static final int N = 32;

	final int[] strip = new int[N];
	final int stopIdx;
	final double start;
	final double dur;
	final double total;

	Reel(int target, long seed, double start, double dur, int turns) {
		Random rnd = new Random(seed);
		this.stopIdx = 20 + rnd.nextInt(8);
		this.start = start;
		this.dur = dur;
		this.total = turns * (double) N + stopIdx;
		for (int i = 0; i < N; i++) {
			strip[i] = rnd.nextInt(Sprites.COUNT);
		}
		strip[stopIdx] = target;
		// keep the cells right next to the stop calm, so a resting reel never shows a fake near miss
		int[] calm = { Sprites.LILY, Sprites.FROG, Sprites.LOTUS };
		for (int d = -1; d <= 1; d += 2) {
			int s;
			do {
				s = calm[rnd.nextInt(calm.length)];
			} while (s == target);
			strip[stopIdx + d] = s;
		}
	}

	boolean stopped(double t) {
		return t >= start + dur;
	}

	double endTime() {
		return start + dur;
	}

	/** Strip position in cells; the symbol on the payline is strip[floor(pos + 0.5)]. */
	double pos(double t) {
		double u = Math.max(0.0, Math.min(1.0, (t - start) / dur));
		double c = total * (1.0 - Math.pow(1.0 - u, 2.6));
		if (t > start + dur) {
			double dt = t - start - dur;
			c += 0.12 * Math.exp(-dt * 7.0) * Math.sin(dt * 20.0);
		}
		return c;
	}

	int symbolAt(int idx) {
		return strip[Math.floorMod(idx, N)];
	}
}
