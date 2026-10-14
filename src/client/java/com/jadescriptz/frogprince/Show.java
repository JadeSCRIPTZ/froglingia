package com.jadescriptz.frogprince;

/** One run of the royal gamble: timing, reels and the facts it reports. */
final class Show {
	final Outcome outcome;
	final Loot loot;
	final boolean quick;
	final boolean preview;
	final int killNo;
	/** Jewel: kills it took. Anything else: kills since the last jewel. */
	final int dry;
	long startNanos = System.nanoTime();
	final Reel[] reels = new Reel[3];
	/** Time scale: 1 for the full show, smaller for quick. */
	final double k;
	final double length;
	/** When the last reel comes to rest. */
	final double lastStop;

	Show(Outcome outcome, Loot loot, boolean quick, boolean preview, int killNo, int dry) {
		this.outcome = outcome;
		this.loot = loot;
		this.quick = quick;
		this.preview = preview;
		this.killNo = killNo;
		this.dry = dry;
		this.k = quick ? 0.6 : 1.0;

		int[] targets = switch (outcome) {
			case JEWEL -> new int[] { Sprites.GEM, Sprites.GEM, Sprites.GEM };
			case DYE -> new int[] { Sprites.DYE, Sprites.DYE, Sprites.DYE };
			case GOLD -> new int[] { Sprites.LOTUS, Sprites.LOTUS, Sprites.LOTUS };
			case MISS -> new int[] { Sprites.LILY, Sprites.FROG, Sprites.LILY };
		};
		boolean win = outcome != Outcome.MISS;
		double begin = 0.55 * k;
		double[] stop = { 1.75 * k, 2.4 * k, (win ? 4.0 : 3.05) * k };
		long seed = System.nanoTime();
		for (int i = 0; i < 3; i++) {
			reels[i] = new Reel(targets[i], seed + i * 7919L, begin + i * 0.06 * k, stop[i] - begin - i * 0.06 * k, 3 + i);
		}
		lastStop = stop[2];
		length = lastStop + (win ? 3.8 : 2.6) * k;
	}

	double time() {
		return (System.nanoTime() - startNanos) / 1e9;
	}

	boolean finished() {
		return time() > length;
	}
}
