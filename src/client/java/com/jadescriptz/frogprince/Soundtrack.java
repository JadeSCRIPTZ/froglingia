package com.jadescriptz.frogprince;

import java.util.Random;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * A tiny synthesiser. The whole score for one show (reel ticks, thunks, fanfare or croaks) is
 * rendered into one buffer from the same reel maths the screen uses, then played on a
 * daemon thread through javax.sound, so sound and picture stay in step.
 */
final class Soundtrack {
	private Soundtrack() {}

	private static final int SR = 44100;
	private static volatile SourceDataLine current;

	static void playAsync(Show show, float volume) {
		Thread t = new Thread(() -> {
			try {
				short[] pcm = render(show, volume);
				play(pcm);
			} catch (Throwable e) {
				System.err.println("[FrogPrince] sound failed: " + e);
			}
		}, "FrogPrince-Audio");
		t.setDaemon(true);
		t.start();
	}

	static void stop() {
		SourceDataLine l = current;
		current = null;
		if (l != null) {
			try {
				l.stop();
				l.flush();
				l.close();
			} catch (Throwable ignored) {
			}
		}
	}

	private static void play(short[] pcm) throws Exception {
		stop();
		AudioFormat fmt = new AudioFormat(SR, 16, 1, true, false);
		SourceDataLine line = AudioSystem.getSourceDataLine(fmt);
		line.open(fmt, 8192);
		current = line;
		line.start();
		byte[] b = new byte[pcm.length * 2];
		for (int i = 0; i < pcm.length; i++) {
			b[i * 2] = (byte) (pcm[i] & 0xFF);
			b[i * 2 + 1] = (byte) ((pcm[i] >> 8) & 0xFF);
		}
		int off = 0;
		while (off < b.length && current == line) {
			int n = Math.min(4096, b.length - off);
			line.write(b, off, n);
			off += n;
		}
		if (current == line) {
			line.drain();
		}
		if (current == line) {
			current = null;
		}
		line.close();
	}

	// ------------------------------------------------------------------ score

	static short[] render(Show s, float volume) {
		double k = s.k;
		int len = (int) ((s.length + 2.5) * SR);
		float[] buf = new float[len];

		croak(buf, 0.03 * k, 110, 0.5f);

		for (Reel r : s.reels) {
			double lastTick = -1;
			int lastCell = (int) Math.floor(r.pos(r.start));
			for (double t = r.start; t < r.endTime(); t += 0.002) {
				int cell = (int) Math.floor(r.pos(t));
				if (cell > lastCell && t - lastTick >= 0.022) {
					double u = (t - r.start) / r.dur;
					click(buf, t, 0.08f + 0.09f * (float) (1.0 - u), 1500 + (cell % 5) * 130);
					lastTick = t;
				}
				lastCell = cell;
			}
			thunk(buf, r.endTime(), 0.55f);
			click(buf, r.endTime(), 0.2f, 2600);
		}

		double tf = s.lastStop;
		switch (s.outcome) {
			case JEWEL -> {
				for (double f : new double[] { 261.63, 329.63, 392.0 }) {
					bell(buf, tf + 0.10, f, 0.20f, 1.1);
				}
				double[] arp = { 523.25, 659.25, 783.99, 1046.5, 1318.5, 1568.0 };
				for (int i = 0; i < arp.length; i++) {
					bell(buf, tf + 0.15 + 0.09 * i, arp[i], 0.30f, 1.8);
				}
				Random rnd = new Random(7);
				for (int j = 0; j < 18; j++) {
					sparkle(buf, tf + 0.9 + 0.11 * j, 1800 + rnd.nextInt(2400), 0.10f);
				}
				bell(buf, tf + 1.25, 2093.0, 0.22f, 2.2);
			}
			case DYE -> {
				double[] whole = { 880.0, 987.77, 1108.7, 1244.5, 1396.9, 1568.0, 1760.0 };
				for (int i = 0; i < whole.length; i++) {
					bell(buf, tf + 0.10 + 0.08 * i, whole[i], 0.26f, 1.6);
				}
				for (int j = 0; j < 12; j++) {
					sparkle(buf, tf + 0.7 + 0.1 * j, 2200 + j * 160, 0.09f);
				}
			}
			case GOLD -> {
				double[] g = { 392.0, 493.88, 587.33, 783.99 };
				for (int i = 0; i < g.length; i++) {
					bell(buf, tf + 0.10 + 0.10 * i, g[i], 0.28f, 2.0);
				}
				sparkle(buf, tf + 0.6, 2600, 0.10f);
			}
			case MISS -> {
				bloop(buf, tf + 0.10, 0.30f);
				croak(buf, tf + 0.38, 85, 0.40f);
				croak(buf, tf + 0.78, 72, 0.30f);
			}
		}

		// one soft echo, then a gentle limiter
		int d = (int) (0.17 * SR);
		for (int i = d; i < len; i++) {
			buf[i] += buf[i - d] * 0.28f;
		}
		float vol = Math.max(0f, Math.min(1f, volume));
		short[] out = new short[len];
		for (int i = 0; i < len; i++) {
			double v = Math.tanh(buf[i] * 1.2) * vol;
			out[i] = (short) Math.round(v * 30000);
		}
		return out;
	}

	// ------------------------------------------------------------- primitives

	private static void add(float[] buf, double t0, int i, float v) {
		int idx = (int) (t0 * SR) + i;
		if (idx >= 0 && idx < buf.length) {
			buf[idx] += v;
		}
	}

	private static void click(float[] buf, double t0, float amp, double f) {
		Random rnd = new Random((long) (t0 * 1e6));
		int n = (int) (0.012 * SR);
		for (int i = 0; i < n; i++) {
			double t = i / (double) SR;
			double v = Math.sin(2 * Math.PI * f * t) * Math.exp(-t * 600) + (rnd.nextDouble() * 2 - 1) * Math.exp(-t * 900) * 0.5;
			add(buf, t0, i, (float) (v * amp));
		}
	}

	private static void thunk(float[] buf, double t0, float amp) {
		int n = (int) (0.22 * SR);
		double phase = 0;
		for (int i = 0; i < n; i++) {
			double t = i / (double) SR;
			double f = 95 - 40 * t;
			phase += 2 * Math.PI * f / SR;
			add(buf, t0, i, (float) (Math.sin(phase) * Math.exp(-t * 22) * amp));
		}
	}

	private static void bell(float[] buf, double t0, double f, float amp, double decay) {
		double[] mult = { 1.0, 2.0, 3.0, 4.2 };
		double[] gain = { 1.0, 0.5, 0.25, 0.15 };
		int n = (int) (2.2 * SR);
		for (int i = 0; i < n; i++) {
			double t = i / (double) SR;
			double v = 0;
			for (int p = 0; p < mult.length; p++) {
				v += Math.sin(2 * Math.PI * f * mult[p] * t) * gain[p] * Math.exp(-t * decay * (1 + 0.5 * mult[p]));
			}
			double attack = Math.min(1.0, t / 0.004);
			add(buf, t0, i, (float) (v * attack * amp * 0.5));
		}
	}

	private static void sparkle(float[] buf, double t0, double f, float amp) {
		int n = (int) (0.3 * SR);
		for (int i = 0; i < n; i++) {
			double t = i / (double) SR;
			add(buf, t0, i, (float) (Math.sin(2 * Math.PI * f * t) * Math.exp(-t * 18) * amp));
		}
	}

	private static void bloop(float[] buf, double t0, float amp) {
		int n = (int) (0.12 * SR);
		double phase = 0;
		for (int i = 0; i < n; i++) {
			double t = i / (double) SR;
			double f = 900 - 5000 * t;
			phase += 2 * Math.PI * Math.max(300, f) / SR;
			add(buf, t0, i, (float) (Math.sin(phase) * Math.exp(-t * 35) * amp));
		}
	}

	/** Two quick buzzy pulses with a rising sweep: a frog saying "rib-bit". */
	private static void croak(float[] buf, double t0, double f0, float amp) {
		for (int pulse = 0; pulse < 2; pulse++) {
			double start = t0 + pulse * 0.13;
			int n = (int) (0.11 * SR);
			double phase = 0;
			for (int i = 0; i < n; i++) {
				double t = i / (double) SR;
				double u = i / (double) n;
				double f = f0 * (1.0 + 0.35 * u);
				phase += 2 * Math.PI * f / SR;
				double v = 0;
				for (int h = 1; h <= 5; h++) {
					v += Math.sin(phase * h) / h;
				}
				double am = 0.55 + 0.45 * Math.sin(2 * Math.PI * 38 * t);
				double env = Math.pow(Math.sin(Math.PI * u), 1.5);
				add(buf, start, i, (float) (v * am * env * amp * 0.6));
			}
		}
	}
}
