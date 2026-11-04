package com.codex.minigamepilot.core;

import com.codex.minigamepilot.core.Artefacts.House;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Your artefact trips, kept per account: each one from the task Captain Khaled gave to the
 * artefact in his hands (or lost on the way), its house, the Thieving experience and coins it paid
 * and its time. From these the panel says artefacts, experience and coins an hour at your own
 * pace, your quickest trip to each house, and how many artefacts a Thieving level is.
 */
public final class ArtefactLog
{
	/** Trips kept. */
	static final int KEEP = 200;
	/** A trip longer than this had a break in it and is not timed (ticks: ten minutes). */
	public static final int LONGEST = 1000;
	private static final String VERSION = "1";

	/** One trip. */
	public static final class Run
	{
		public final long endedAt;
		/** Ticks from the task to the hand-in, or 0 when not timed. */
		public final int ticks;
		/** The house, or null when not known. */
		public final House house;
		/** Handed to Captain Khaled; false when caught or lost on the way. */
		public final boolean delivered;
		public final int xp;
		public final int coins;

		public Run(long endedAt, int ticks, House house, boolean delivered, int xp, int coins)
		{
			this.endedAt = Math.max(0, endedAt);
			this.ticks = ticks > 0 && ticks <= LONGEST ? ticks : 0;
			this.house = house;
			this.delivered = delivered;
			this.xp = Math.max(0, Math.min(Artefacts.xpEach(99), xp));
			this.coins = delivered ? Math.max(0, Math.min(Artefacts.COINS_MAX, coins)) : 0;
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

	public int delivered()
	{
		int n = 0;
		for (Run r : runs)
		{
			n += r.delivered ? 1 : 0;
		}
		return n;
	}

	public int lost()
	{
		return runs.size() - delivered();
	}

	public long coins()
	{
		long c = 0;
		for (Run r : runs)
		{
			c += r.coins;
		}
		return c;
	}

	public Run last()
	{
		return runs.isEmpty() ? null : runs.get(runs.size() - 1);
	}

	/** The quickest timed delivery to a house (any house for null); null with none. */
	public Run quickest(House house)
	{
		Run best = null;
		for (Run r : runs)
		{
			if (r.delivered && r.ticks > 0 && (house == null || r.house == house) && (best == null || r.ticks < best.ticks))
			{
				best = r;
			}
		}
		return best;
	}

	/** Average ticks of your timed deliveries to a house, or -1 with none. */
	public int typical(House house)
	{
		long sum = 0;
		int n = 0;
		for (Run r : runs)
		{
			if (r.delivered && r.ticks > 0 && r.house == house)
			{
				sum += r.ticks;
				n++;
			}
		}
		return n == 0 ? -1 : (int) Math.round((double) sum / n);
	}

	/** Timed deliveries to a house. */
	public int timed(House house)
	{
		int n = 0;
		for (Run r : runs)
		{
			n += r.delivered && r.ticks > 0 && r.house == house ? 1 : 0;
		}
		return n;
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

	/** Artefacts delivered an hour over the timed trips (lost ones count their time), or -1 with none. */
	public double perHour()
	{
		long t = timedTicks();
		long n = 0;
		for (Run r : runs)
		{
			n += r.ticks > 0 && r.delivered ? 1 : 0;
		}
		return t == 0 ? -1 : n * 3600.0 / (t * 0.6);
	}

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

	public double coinsPerHour()
	{
		long t = timedTicks();
		long sum = 0;
		for (Run r : runs)
		{
			sum += r.ticks > 0 ? r.coins : 0;
		}
		return t == 0 ? -1 : sum * 3600.0 / (t * 0.6);
	}

	/** Hours until this much experience more at your pace: 0 when none is needed, -1 without a pace. */
	public double hoursFor(long xp)
	{
		if (xp <= 0)
		{
			return 0;
		}
		double h = xpPerHour();
		return h <= 0 ? -1 : xp / h;
	}

	/** Artefacts for this much experience more at this level: 750 for the lock and 40 times the level for each. */
	public static int artefactsFor(long xp, int level)
	{
		return xp <= 0 ? 0 : (int) Math.ceil(xp / (double) Artefacts.xpEach(level));
	}

	/** "1;endedAt,ticks,house,delivered,xp,coins;..." with the house by name, "-" when not known. */
	public String encode()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (Run r : runs)
		{
			b.append(';').append(r.endedAt).append(',').append(r.ticks).append(',').append(r.house == null ? "-" : r.house.name())
				.append(',').append(r.delivered ? 1 : 0).append(',').append(r.xp).append(',').append(r.coins);
		}
		return b.toString();
	}

	/** Reads a save as far as it can; damaged trips are left out. Never null. */
	public static ArtefactLog decode(String text)
	{
		ArtefactLog out = new ArtefactLog();
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
				String h = f[2].trim();
				House house = "-".equals(h) ? null : House.valueOf(h);
				String d = f[3].trim();
				int xp = Integer.parseInt(f[4].trim());
				int coins = Integer.parseInt(f[5].trim());
				if (ended >= 0 && ticks >= 0 && xp >= 0 && coins >= 0 && ("1".equals(d) || "0".equals(d)))
				{
					out.add(new Run(ended, ticks, house, "1".equals(d), xp, coins));
				}
			}
			catch (IllegalArgumentException e)
			{
				// Left out: a bad number or an unknown house.
			}
		}
		return out;
	}
}
