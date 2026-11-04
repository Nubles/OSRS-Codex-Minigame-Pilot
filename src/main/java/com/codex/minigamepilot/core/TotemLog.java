package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Your Vale Totems, kept per account: each totem you finished (its fourth decoration), its wood,
 * the Fletching experience gained since the one before, the offerings the ents left at your sites
 * over that time, and the time between the two. From these the panel says your totems, experience
 * and offerings an hour, and how long a Vale Research Exchange reward is at your own pace.
 */
public final class TotemLog
{
	/** Totems kept: a dozen loops, small enough for a saved setting. */
	static final int KEEP = 100;
	/** A gap longer than this had a break in it and is not timed (ticks: ten minutes). */
	public static final int LONGEST = 1000;
	private static final String VERSION = "1";

	/** One finished totem. */
	public static final class Totem
	{
		public final long endedAt;
		/** Ticks since the totem before, or 0 when that was not seen or there was a break. */
		public final int ticks;
		/** The wood, or null when it was not known. */
		public final Totems.Wood wood;
		public final int xp;
		/** Offerings the ents left at your sites since the totem before. */
		public final int offerings;

		public Totem(long endedAt, int ticks, Totems.Wood wood, int xp, int offerings)
		{
			this.endedAt = Math.max(0, endedAt);
			this.ticks = ticks > 0 && ticks <= LONGEST ? ticks : 0;
			this.wood = wood;
			this.xp = Math.max(0, Math.min(1_000_000, xp));
			this.offerings = Math.max(0, Math.min(Totems.OFFERINGS_LIMIT * Totems.SITES, offerings));
		}
	}

	private final List<Totem> totems = new ArrayList<>();

	public void add(Totem t)
	{
		totems.add(t);
		while (totems.size() > KEEP)
		{
			totems.remove(0);
		}
	}

	public List<Totem> all()
	{
		return Collections.unmodifiableList(totems);
	}

	public int count()
	{
		return totems.size();
	}

	public Totem last()
	{
		return totems.isEmpty() ? null : totems.get(totems.size() - 1);
	}

	/** The quickest timed totem; null with none timed. */
	public Totem quickest()
	{
		Totem best = null;
		for (Totem t : totems)
		{
			if (t.ticks > 0 && (best == null || t.ticks < best.ticks))
			{
				best = t;
			}
		}
		return best;
	}

	private long timedTicks()
	{
		long ticks = 0;
		for (Totem t : totems)
		{
			ticks += t.ticks;
		}
		return ticks;
	}

	/** Totems an hour over the timed ones kept, or -1 with none. */
	public double perHour()
	{
		long ticks = timedTicks();
		long n = 0;
		for (Totem t : totems)
		{
			n += t.ticks > 0 ? 1 : 0;
		}
		return ticks == 0 ? -1 : n * 3600.0 / (ticks * 0.6);
	}

	/** Fletching experience an hour over the timed totems, or -1 with none. */
	public double xpPerHour()
	{
		long ticks = timedTicks();
		long sum = 0;
		for (Totem t : totems)
		{
			sum += t.ticks > 0 ? t.xp : 0;
		}
		return ticks == 0 ? -1 : sum * 3600.0 / (ticks * 0.6);
	}

	/** Offerings the ents left an hour over the timed totems, or -1 with none. */
	public double offeringsPerHour()
	{
		long ticks = timedTicks();
		long sum = 0;
		for (Totem t : totems)
		{
			sum += t.ticks > 0 ? t.offerings : 0;
		}
		return ticks == 0 ? -1 : sum * 3600.0 / (ticks * 0.6);
	}

	/** Vale Research Points an hour: a point for every hundred offerings. -1 without a pace. */
	public double pointsPerHour()
	{
		double o = offeringsPerHour();
		return o < 0 ? -1 : o / Totems.OFFERINGS_PER_POINT;
	}

	/**
	 * Hours until a reward at your pace: 0 when you have the points already (offerings held and
	 * waiting count, rummaged at a hundred a point), -1 without a pace that earns any.
	 */
	public double hoursTo(Totems.Reward reward, int points, long offerings)
	{
		long need = reward.points() - (long) points - Totems.points(offerings);
		if (need <= 0)
		{
			return 0;
		}
		double h = pointsPerHour();
		return h <= 0 ? -1 : need / h;
	}

	/** "1;endedAt,ticks,wood,xp,offerings;..." with the wood as its name, or "-" when not known. */
	public String encode()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (Totem t : totems)
		{
			b.append(';').append(t.endedAt).append(',').append(t.ticks).append(',').append(t.wood == null ? "-" : t.wood.name())
				.append(',').append(t.xp).append(',').append(t.offerings);
		}
		return b.toString();
	}

	/** Reads a save as far as it can; damaged totems are left out. Never null. */
	public static TotemLog decode(String text)
	{
		TotemLog out = new TotemLog();
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
			if (f.length < 5)
			{
				continue;
			}
			try
			{
				long ended = Long.parseLong(f[0].trim());
				int ticks = Integer.parseInt(f[1].trim());
				Totems.Wood wood = null;
				String w = f[2].trim();
				if (!"-".equals(w))
				{
					wood = Totems.Wood.valueOf(w);
				}
				int xp = Integer.parseInt(f[3].trim());
				int offerings = Integer.parseInt(f[4].trim());
				if (ended >= 0 && ticks >= 0 && xp >= 0 && offerings >= 0)
				{
					out.add(new Totem(ended, ticks, wood, xp, offerings));
				}
			}
			catch (IllegalArgumentException e)
			{
				// Left out: a bad number or an unknown wood.
			}
		}
		return out;
	}
}
