package com.codex.minigamepilot.core;

import java.util.Locale;

/**
 * Every count the Pilot shows, written the one way the whole Codex suite writes them: in full
 * with commas below 10,000 ("9,870"), whole lowercase thousands below a million ("142k"),
 * millions with one decimal ("3.1M") and billions with two ("1.23B"). A figure is rounded, never
 * cut, and one that rounds up to the next unit moves to it, so 999,960 is "1.0M", not "1000k".
 */
public final class Figures
{
	/** Where a count stops writing every digit, as everywhere in the suite. */
	static final long BRIEF_EXACT = 10_000;

	private Figures()
	{
	}

	/** "9,870", "68k", "142k", "3.1M". */
	public static String count(long n)
	{
		return scaled(n, BRIEF_EXACT);
	}

	/** The same as {@link #count}: "9,870", "41k", "3.1M". */
	public static String brief(long n)
	{
		return scaled(n, BRIEF_EXACT);
	}

	/** A brief figure with its sign: "+71k", "-104k", "+9,870", "+0". */
	public static String signed(long n)
	{
		return (n < 0 ? "-" : "+") + brief(n == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(n));
	}

	/**
	 * Two brief figures written in the larger one's unit, so a gauge never reads "41,000 / 993k":
	 * "41k / 993k", "612 / 1,800", "1.2M / 13.0M". A part too small to show in that unit keeps
	 * its own rather than reading as nothing: "300 / 41k", "12k / 13.0M". A part short of the
	 * whole is never written as all of it: where rounding would make the two read the same, the
	 * part is rounded down instead, "9k / 10k" for 9,600 of 10,000, not "10k / 10k", and when
	 * even that reads the same (13,000,577 of 13,034,431) it reads one step below the whole,
	 * "12.9M / 13.0M": still true of what you have, and the gauge's fill shows the rest.
	 */
	public static String pair(long have, long need)
	{
		long top = Math.max(Math.abs(have), Math.abs(need));
		if (top < BRIEF_EXACT)
		{
			return exact(have) + " / " + exact(need);
		}
		int unit = unit(top);
		String part = in(have, unit);
		String whole = in(need, unit);
		if (have >= 0 && have < need && part.equals(whole))
		{
			part = down(have, unit);
			if (part.equals(whole))
			{
				long step = unit == 1 ? 1_000 : unit == 2 ? 100_000 : 10_000_000;
				part = down(Math.max(0, Math.round((double) need / step) - 1) * step, unit);
			}
		}
		return part + " / " + whole;
	}

	/** {@link #in} for a figure that is not below zero, rounded down rather than to the nearest. */
	private static String down(long n, int unit)
	{
		switch (unit)
		{
			case 1:
				return n < 1000 ? exact(n) : n / 1000 + "k";
			case 2:
				return String.format(Locale.ENGLISH, "%d.%dM", n / 1_000_000, n / 100_000 % 10);
			default:
				return String.format(Locale.ENGLISH, "%d.%02dB", n / 1_000_000_000, n / 10_000_000 % 100);
		}
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
				return a < 1000 ? exact(n) : sign + Math.round(a / 1000.0) + "k";
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
