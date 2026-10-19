package com.codex.minigamepilot.core;

import java.util.Locale;

/**
 * Every count the Pilot shows, written one way. Below the cut-off a count is exact with commas
 * ("9,870"); above it thousands are whole ("142K"), millions carry one decimal ("3.1M") and
 * billions two ("1.23B"). A figure is rounded, never cut, and one that rounds up to the next unit
 * moves to it, so 999,960 is "1.0M", not "1000K".
 */
public final class Figures
{
	/** Where {@link #count} stops writing every digit: tiles and lines have the room. */
	static final long COUNT_EXACT = 100_000;
	/** Where {@link #brief} stops: figures beside a label or in a gauge do not. */
	static final long BRIEF_EXACT = 10_000;

	private Figures()
	{
	}

	/** "68,040", "142K", "3.1M". */
	public static String count(long n)
	{
		return scaled(n, COUNT_EXACT);
	}

	/** "9,870", "41K", "3.1M", for a figure that has to stay short. */
	public static String brief(long n)
	{
		return scaled(n, BRIEF_EXACT);
	}

	/** A brief figure with its sign: "+71K", "-104K", "+9,870", "+0". */
	public static String signed(long n)
	{
		return (n < 0 ? "-" : "+") + brief(n == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(n));
	}

	/**
	 * Two brief figures written in the larger one's unit, so a gauge never reads "41,000 / 993K":
	 * "41K / 993K", "612 / 1,800", "1.2M / 13.0M". A part too small to show in that unit keeps
	 * its own rather than reading as nothing: "300 / 41K", "12K / 13.0M".
	 */
	public static String pair(long have, long need)
	{
		long top = Math.max(Math.abs(have), Math.abs(need));
		if (top < BRIEF_EXACT)
		{
			return exact(have) + " / " + exact(need);
		}
		int unit = unit(top);
		return in(have, unit) + " / " + in(need, unit);
	}

	private static String scaled(long n, long exactBelow)
	{
		long a = Math.abs(n);
		if (a < exactBelow)
		{
			return exact(n);
		}
		return in(n, unit(a));
	}

	/** 1 for thousands, 2 for millions, 3 for billions, after rounding. */
	private static int unit(long a)
	{
		if (a < 999_500)
		{
			return 1;
		}
		if (a < 999_950_000)
		{
			return 2;
		}
		return 3;
	}

	private static String in(long n, int unit)
	{
		long a = Math.abs(n);
		String sign = n < 0 ? "-" : "";
		switch (unit)
		{
			case 1:
				return a < 1000 ? exact(n) : sign + Math.round(a / 1000.0) + "K";
			case 2:
				return a < 50_000 ? brief(n) : sign + String.format(Locale.ENGLISH, "%.1fM", a / 1_000_000.0);
			default:
				return a < 5_000_000 ? brief(n) : sign + String.format(Locale.ENGLISH, "%.2fB", a / 1_000_000_000.0);
		}
	}

	private static String exact(long n)
	{
		return String.format(Locale.ENGLISH, "%,d", n);
	}
}
