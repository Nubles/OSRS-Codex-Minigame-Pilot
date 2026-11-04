package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Your Tithe Farm runs, kept per account: each sack of fruit (a hundred, or what you had put in
 * when you left the farm), its seed, the points and Farming experience it gave, and the time from
 * its first seed to its last deposit. From these the panel says your points and experience an
 * hour, your quickest sack, and how long a reward is at your own pace.
 */
public final class TitheLog
{
	/** Runs kept. */
	static final int KEEP = 100;
	/** A run longer than this had a break in it and is not timed (ticks: an hour). */
	public static final int LONGEST = 6000;
	private static final String VERSION = "1";

	/** One sack. */
	public static final class Run
	{
		public final long endedAt;
		/** Ticks from its first seed to its last deposit, or 0 when not timed. */
		public final int ticks;
		/** The seed, or null when not known. */
		public final Tithe.Seed seed;
		public final int fruit;
		public final int points;
		public final int xp;

		public Run(long endedAt, int ticks, Tithe.Seed seed, int fruit, int points, int xp)
		{
			this.endedAt = Math.max(0, endedAt);
			this.ticks = ticks > 0 && ticks <= LONGEST ? ticks : 0;
			this.seed = seed;
			this.fruit = Math.max(0, Math.min(Tithe.SACK, fruit));
			this.points = Math.max(0, Math.min(Tithe.POINTS_CAP, points));
			this.xp = Math.max(0, Math.min(1_000_000, xp));
		}

		/** A whole sack of a hundred. */
		public boolean full()
		{
			return fruit >= Tithe.SACK;
		}
	}

	private final List<Run> runs = new ArrayList<>();

	public void add(Run r)
	{
		runs.add(r);
		while (runs.size() > KEEP)
		{
			runs.remove(0);
		}
	}

	public List<Run> all()
	{
		return Collections.unmodifiableList(runs);
	}

	public int count()
	{
		return runs.size();
	}

	public Run last()
	{
		return runs.isEmpty() ? null : runs.get(runs.size() - 1);
	}

	/** The quickest timed full sack; null with none. */
	public Run quickest()
	{
		Run best = null;
		for (Run r : runs)
		{
			if (r.ticks > 0 && r.full() && (best == null || r.ticks < best.ticks))
			{
				best = r;
			}
		}
		return best;
	}

	private long timedTicks()
	{
		long t = 0;
		for (Run r : runs)
		{
			t += r.ticks;
		}
		return t;
	}

	/** Points an hour over the timed runs kept, or -1 with none. */
	public double pointsPerHour()
	{
		long t = timedTicks();
		long sum = 0;
		for (Run r : runs)
		{
			sum += r.ticks > 0 ? r.points : 0;
		}
		return t == 0 ? -1 : sum * 3600.0 / (t * 0.6);
	}

	/** Farming experience an hour over the timed runs kept, or -1 with none. */
	public double xpPerHour()
	{
		long t = timedTicks();
		long sum = 0;
		for (Run r : runs)
		{
			sum += r.ticks > 0 ? r.xp : 0;
		}
		return t == 0 ? -1 : sum * 3600.0 / (t * 0.6);
	}

	/** Hours until this many points more at your pace: 0 when none are needed, -1 without a pace. */
	public double hoursFor(long points)
	{
		if (points <= 0)
		{
			return 0;
		}
		double h = pointsPerHour();
		return h <= 0 ? -1 : points / h;
	}

	/** Full sacks for this many points more: your own points a sack, else the 35 a full sack pays. */
	public int sacksFor(long points)
	{
		if (points <= 0)
		{
			return 0;
		}
		long sum = 0;
		int n = 0;
		for (Run r : runs)
		{
			if (r.full() && r.points > 0)
			{
				sum += r.points;
				n++;
			}
		}
		double each = n == 0 ? Tithe.sackPoints(Tithe.SACK) : (double) sum / n;
		return (int) Math.ceil(points / each);
	}

	/** "1;endedAt,ticks,seed,fruit,points,xp;..." with the seed by name, "-" when not known. */
	public String encode()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (Run r : runs)
		{
			b.append(';').append(r.endedAt).append(',').append(r.ticks).append(',').append(r.seed == null ? "-" : r.seed.name())
				.append(',').append(r.fruit).append(',').append(r.points).append(',').append(r.xp);
		}
		return b.toString();
	}

	/** Reads a save as far as it can; damaged runs are left out. Never null. */
	public static TitheLog decode(String text)
	{
		TitheLog out = new TitheLog();
		if (text == null)
		{
			return out;
		}
		String[] parts = text.split(";");
		if (parts.length == 0 || !VERSION.equals(parts[0].trim()))
		{
			return out;
		}
		for (int i = 1; i < parts.length; i++)
		{
			String[] f = parts[i].split(",");
			if (f.length < 6)
			{
				continue;
			}
			try
			{
				long ended = Long.parseLong(f[0].trim());
				int ticks = Integer.parseInt(f[1].trim());
				String s = f[2].trim();
				Tithe.Seed seed = "-".equals(s) ? null : Tithe.Seed.valueOf(s);
				int fruit = Integer.parseInt(f[3].trim());
				int points = Integer.parseInt(f[4].trim());
				int xp = Integer.parseInt(f[5].trim());
				if (ended >= 0 && ticks >= 0 && fruit >= 0 && points >= 0 && xp >= 0)
				{
					out.add(new Run(ended, ticks, seed, fruit, points, xp));
				}
			}
			catch (IllegalArgumentException e)
			{
				// Left out: a bad number or an unknown seed.
			}
		}
		return out;
	}
}
