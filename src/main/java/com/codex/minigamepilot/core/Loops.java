package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The tick coach's view of a session: every fair loop of one minigame added up into loops an
 * hour and experience an hour while looping, and spread out as a histogram of loop times. Only
 * loops the coach timed whole and without a break count, so a walk to the bank for a sandwich
 * never drags the pace down.
 */
public final class Loops
{
	/** A game tick is 0.6 seconds (oldschool.runescape.wiki/w/Game_tick). */
	public static final double TICK_SECONDS = 0.6;
	/** Bars the histogram is drawn in, at most. */
	public static final int MAX_BINS = 8;
	/** Bin widths the histogram picks from, in ticks, so its edges read as round numbers. */
	private static final int[] WIDTHS = {1, 2, 5, 10, 20, 25, 50, 100, 200, 250, 500, 1000};

	/** One bar of the histogram: loops that took from..to-1 ticks. */
	public static final class Bin
	{
		public final int from;
		public final int to;
		public final int count;
		/** Whether the last loop fell in this bar. */
		public final boolean last;
		/** Whether your best loop of the last loop's kind falls in this bar. */
		public final boolean best;

		Bin(int from, int to, int count, boolean last, boolean best)
		{
			this.from = from;
			this.to = to;
			this.count = count;
			this.last = last;
			this.best = best;
		}
	}

	private final List<Pace.Round> fair;

	private Loops(List<Pace.Round> fair)
	{
		this.fair = fair;
	}

	/** The fair loops among {@code rounds}, oldest first. */
	public static Loops of(List<Pace.Round> rounds)
	{
		List<Pace.Round> fair = new ArrayList<>();
		if (rounds != null)
		{
			for (Pace.Round r : rounds)
			{
				if (r != null && !r.hadBreak && r.ticks > 0)
				{
					fair.add(r);
				}
			}
		}
		return new Loops(Collections.unmodifiableList(fair));
	}

	public int count()
	{
		return fair.size();
	}

	/** Ticks over every fair loop. */
	public long ticks()
	{
		long t = 0;
		for (Pace.Round r : fair)
		{
			t += r.ticks;
		}
		return t;
	}

	/** Loops an hour at the pace you have looped, or 0 with none. */
	public long perHour()
	{
		long t = ticks();
		return t == 0 ? 0 : Math.round(fair.size() * 3600 / (t * TICK_SECONDS));
	}

	/**
	 * Experience an hour while looping, over the loops whose experience is known, or -1 when
	 * none is.
	 */
	public long xpPerHour()
	{
		long xp = 0;
		long t = 0;
		for (Pace.Round r : fair)
		{
			if (r.xp() >= 0)
			{
				xp += r.xp();
				t += r.ticks;
			}
		}
		return t == 0 ? -1 : Math.round(xp * 3600 / (t * TICK_SECONDS));
	}

	/**
	 * The loop times as at most {@link #MAX_BINS} bars of a round width, from the fastest loop's
	 * bar to the slowest's. Empty with no loops.
	 *
	 * @param last the loop to mark, or null
	 * @param best your best loop of its kind, in ticks, or 0
	 */
	public List<Bin> histogram(Pace.Round last, int best)
	{
		if (fair.isEmpty())
		{
			return Collections.emptyList();
		}
		int min = Integer.MAX_VALUE;
		int max = 0;
		for (Pace.Round r : fair)
		{
			min = Math.min(min, r.ticks);
			max = Math.max(max, r.ticks);
		}
		int width = WIDTHS[WIDTHS.length - 1];
		for (int w : WIDTHS)
		{
			if (max / w - min / w + 1 <= MAX_BINS)
			{
				width = w;
				break;
			}
		}
		int start = min / width * width;
		int bins = Math.max(1, max / width - min / width + 1);
		int[] counts = new int[bins];
		for (Pace.Round r : fair)
		{
			counts[Math.min(bins - 1, (r.ticks - start) / width)]++;
		}
		List<Bin> list = new ArrayList<>();
		for (int i = 0; i < bins; i++)
		{
			int from = start + i * width;
			int to = from + width;
			boolean isLast = last != null && !last.hadBreak && last.ticks >= from && last.ticks < to;
			boolean isBest = best > 0 && best >= from && best < to;
			list.add(new Bin(from, to, counts[i], isLast, isBest));
		}
		return list;
	}
}
