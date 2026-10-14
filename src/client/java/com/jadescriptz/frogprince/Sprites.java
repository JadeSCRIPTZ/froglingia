package com.jadescriptz.frogprince;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Hand-made 11x11 pixel art, drawn with plain rectangles so the mod ships no textures. */
final class Sprites {
	private Sprites() {}

	static final int LILY = 0;
	static final int FROG = 1;
	static final int LOTUS = 2;
	static final int GEM = 3;
	static final int CROWN = 4;
	static final int DYE = 5;
	static final int COUNT = 6;
	static final int SIZE = 11;

	private static final String[][] ART = {
		// LILY pad
		{
			"...........",
			"...ggggg...",
			"..gGGGGGg..",
			".gGGllGGGg.",
			".gGGGlGGGg.",
			"gGGGGGGGGGg",
			"gGGGGGGGGGg",
			".gGGGGGGGg.",
			"..gGGGGGg..",
			"...ggggg...",
			"..........."
		},
		// FROG face
		{
			".gg.....gg.",
			"gwwg...gwwg",
			"gwkgggggwkg",
			".gGGGGGGGg.",
			"gGGGGGGGGGg",
			"gGGGGGGGGGg",
			"gGrrrrrrrGg",
			".gGrrrrrGg.",
			".gGGGGGGGg.",
			"..gGGGGGg..",
			"...ggggg..."
		},
		// LOTUS flower
		{
			".....p.....",
			"....pPp....",
			".p..pPp..p.",
			"pPp.pPp.pPp",
			".pPpPPPpPp.",
			"..pPPyPPp..",
			"...pPyyPp..",
			".gggPPPggg.",
			"ggGGggGGGgg",
			".gGGGGGGGg.",
			"..ggggggg.."
		},
		// GEM (the Crown Jewel): gold setting, emerald cut
		{
			"...YYYYY...",
			"..YyyyyyY..",
			".YyeeeeeyY.",
			"YyeewwEeeyY",
			"YyeeEEEeeyY",
			"YyeEEEEEeyY",
			".YyeEEEeyY.",
			"..YyeEeyY..",
			"...YyeyY...",
			"....YyY....",
			".....Y....."
		},
		// CROWN
		{
			"...........",
			"...........",
			".Y...Y...Y.",
			".YY.YYY.YY.",
			".YYYYYYYYY.",
			".YyYYrYYyY.",
			".YYYYYYYYY.",
			".YYyYYYyYY.",
			".YYYYYYYYY.",
			".ooooooooo.",
			"..........."
		},
		// aquamarine DYE bottle
		{
			"....kkk....",
			"....cCc....",
			"...kkkkk...",
			"..kccccck..",
			".kccwcccck.",
			".kcwccccCk.",
			".kcccccCCk.",
			".kccccCCCk.",
			"..kCCCCCk..",
			"...kkkkk...",
			"..........."
		}
	};

	private static final int[] PALETTE = new int[128];

	static {
		PALETTE['g'] = 0x1B5E20;
		PALETTE['G'] = 0x43A047;
		PALETTE['l'] = 0x9CCC65;
		PALETTE['w'] = 0xFFFFFF;
		PALETTE['k'] = 0x101010;
		PALETTE['p'] = 0xF8BBD0;
		PALETTE['P'] = 0xE91E63;
		PALETTE['y'] = 0xFFF59D;
		PALETTE['Y'] = 0xFFC107;
		PALETTE['r'] = 0xD32F2F;
		PALETTE['c'] = 0x4DD0E1;
		PALETTE['C'] = 0x00838F;
		PALETTE['e'] = 0x2ECC71;
		PALETTE['E'] = 0x0B7A3E;
		PALETTE['o'] = 0x5D4037;
	}

	/** Draws one sprite with its top-left at (x, y); every art pixel is ps x ps. */
	static void draw(GuiGraphicsExtractor g, int sprite, int x, int y, int ps, float alpha) {
		int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
		if (a < 4) {
			return;
		}
		String[] rows = ART[sprite];
		for (int ry = 0; ry < SIZE; ry++) {
			String row = rows[ry];
			int rx = 0;
			while (rx < SIZE) {
				char ch = row.charAt(rx);
				if (ch == '.') {
					rx++;
					continue;
				}
				int end = rx + 1;
				while (end < SIZE && row.charAt(end) == ch) {
					end++;
				}
				int color = (a << 24) | PALETTE[ch];
				g.fill(x + rx * ps, y + ry * ps, x + end * ps, y + (ry + 1) * ps, color);
				rx = end;
			}
		}
	}
}
