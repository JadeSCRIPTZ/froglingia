package com.jadescriptz.frogprince;

/** The drops confirmed after one Frog Prince kill. */
final class Loot {
	boolean jewel;
	boolean gold;
	boolean dye;
	/** Silver Lotus seen (0 when none was seen or the amount is unknown). */
	int silver;
	boolean silverSeen;
	/** True when any drop evidence at all was seen after the kill. */
	boolean evidence;

	Outcome outcome() {
		if (dye) {
			return Outcome.DYE;
		}
		if (jewel) {
			return Outcome.JEWEL;
		}
		if (gold) {
			return Outcome.GOLD;
		}
		return Outcome.MISS;
	}
}
