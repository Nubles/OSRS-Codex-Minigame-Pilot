package com.codex.minigamepilot.core;

/**
 * What the metal in the crucible is worth, and what a sword pays.
 *
 * <p>Facts (OSRS wiki, Giants' Foundry):
 * <ul>
 * <li>the crucible takes 28 bars' worth of bronze, iron, steel, mithril, adamant or rune;</li>
 * <li>metal score {@code ⌊(⌊10·V1⌋ + ⌊10·V2⌋ + ⌊V1·V2⌋) / 10⌋}, where {@code Vn = 10 × tier × amount / 28}
 * for the two most plentiful metals (tier 1 bronze to 6 rune; a pure metal has V2 = 0); the
 * wiki's own examples, which the tests hold this to: all bronze 10, all rune 60, 19 steel and
 * 9 mithril 59, 18 mithril and 10 adamant 89, 19 adamant and 9 rune 118, 14 and 14 130;</li>
 * <li>sections (and sweet spots) by metal score: 10-19 three (2), 20-59 four (2), 60-89 five
 * (2), 90-119 six (3), 120-130 seven (3);</li>
 * <li>quality is metal score plus mould score, less 10 for each mistake; reputation is the
 * final quality (Foundry Reputation); a ruined sword pays 30 Smithing experience;</li>
 * <li>experience at hand-in: the wiki's formula renders flattened, but read as
 * {@code (⌊q²/73⌋ + ⌊1.5q⌋ + 1) × 30} it gives the two figures the page states, 30 at quality
 * 0 and 25,230 at 199; coins are double the experience.</li>
 * </ul>
 */
public final class Alloy
{
	public static final int CRUCIBLE = 28;
	/** Quality lost to a wrong tool or a wrong heat. */
	public static final int MISTAKE = 10;
	public static final String[] METALS = {"Bronze", "Iron", "Steel", "Mithril", "Adamant", "Rune"};

	private Alloy()
	{
	}

	/** Bars in the crucible, from counts by tier (bronze first). */
	public static int bars(int[] counts)
	{
		int n = 0;
		for (int c : counts)
		{
			n += Math.max(0, c);
		}
		return n;
	}

	/** The metal score of a crucible's counts by tier, bronze first; 0 when empty. */
	public static int metalScore(int[] counts)
	{
		int first = -1;
		int second = -1;
		for (int t = counts.length - 1; t >= 0; t--)
		{
			// From rune down, so a tie goes to the more valuable metal, as the wiki says.
			int c = Math.max(0, counts[t]);
			if (c == 0)
			{
				continue;
			}
			if (first < 0 || c > counts[first])
			{
				second = first;
				first = t;
			}
			else if (second < 0 || c > counts[second])
			{
				second = t;
			}
		}
		if (first < 0)
		{
			return 0;
		}
		double v1 = value(first, counts[first]);
		double v2 = second < 0 ? 0 : value(second, counts[second]);
		return (int) Math.floor((Math.floor(10 * v1) + Math.floor(10 * v2) + Math.floor(v1 * v2)) / 10);
	}

	private static double value(int tierIndex, int amount)
	{
		return 10.0 * (tierIndex + 1) * amount / CRUCIBLE;
	}

	/** The metal the crucible holds most of, or null when empty. */
	public static String main(int[] counts)
	{
		int best = -1;
		for (int t = counts.length - 1; t >= 0; t--)
		{
			if (counts[t] > 0 && (best < 0 || counts[t] > counts[best]))
			{
				best = t;
			}
		}
		return best < 0 ? null : METALS[best];
	}

	/** Sections a sword of this metal score has: 3 to 7. */
	public static int sections(int metalScore)
	{
		if (metalScore >= 120)
		{
			return 7;
		}
		if (metalScore >= 90)
		{
			return 6;
		}
		if (metalScore >= 60)
		{
			return 5;
		}
		if (metalScore >= 20)
		{
			return 4;
		}
		return 3;
	}

	/** Sweet spots a sword with this many sections has: 3 for six or seven, else 2. */
	public static int sweetSpots(int sections)
	{
		return sections >= 6 ? 3 : 2;
	}

	/** Smithing experience for handing in a sword of this quality. */
	public static int xp(int quality)
	{
		int q = Math.max(0, quality);
		return (q * q / 73 + q * 3 / 2 + 1) * 30;
	}
}
