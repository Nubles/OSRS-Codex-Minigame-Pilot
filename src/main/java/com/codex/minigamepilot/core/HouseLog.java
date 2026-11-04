package com.codex.minigamepilot.core;

import com.codex.minigamepilot.core.Houses.House;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Your house hauls, kept per account: each from the key in the door to the window out, its house,
 * the valuables taken and kept, the jewellery and the rest at Grand Exchange prices, the Thieving
 * experience, the time inside, and the time and experience of the whole round since the last
 * house (the pickpocketing for the key included). From these the panel says coins and experience
 * an hour at your own pace.
 */
public final class HouseLog
{
	/** Hauls kept. */
	static final int KEEP = 200;
	/** A round longer than this had a break in it and is not timed (ticks: ten minutes). */
	public static final int LONGEST = 1000;
	/** No haul takes more valuables than this (the owner is away at most 190 seconds). */
	static final int MOST = 2000;
	private static final String VERSION = "1";

	/** One house. */
	public static final class Haul
	{
		public final long endedAt;
		/** The house, or null when not known. */
		public final House house;
		/** Ticks inside, or 0 when not timed. */
		public final int ticks;
		/** Ticks since the last house ended, this one's keys and walk included, or 0 when not timed. */
		public final int round;
		/** Valuables taken, at the most held. */
		public final int taken;
		/** Valuables still held on the way out. */
		public final int kept;
		/** Thieving experience inside the house. */
		public final int xp;
		/** Thieving experience over the round. */
		public final int roundXp;
		/** Jewellery and anything else found, at Grand Exchange prices. */
		public final long items;

		public Haul(long endedAt, House house, int ticks, int round, int taken, int kept, int xp, int roundXp, long items)
		{
			this.endedAt = Math.max(0, endedAt);
			this.house = house;
			this.ticks = ticks > 0 && ticks <= LONGEST ? ticks : 0;
			this.round = round >= this.ticks && round > 0 && round <= LONGEST ? round : this.ticks;
			this.taken = Math.max(0, Math.min(MOST, taken));
			this.kept = Math.max(0, Math.min(this.taken, kept));
			this.xp = Math.max(0, Math.min(MOST * Houses.XP_EACH, xp));
			this.roundXp = Math.max(this.xp, Math.min(MOST * Houses.XP_EACH * 2, roundXp));
			this.items = Math.max(0, Math.min(100_000_000L, items));
		}

		/** Lost most of it: the owner caught you inside. */
		public boolean caught()
		{
			return taken > 0 && kept * 2 < taken;
		}

		/** Coins at Oriana's rate, and the items. */
		public long value(int rate)
		{
			return (long) kept * rate + items;
		}
	}

	private final List<Haul> hauls = new ArrayList<>();

	public void add(Haul h)
	{
		hauls.add(h);
		while (hauls.size() > KEEP)
		{
			hauls.remove(0);
		}
	}

	public List<Haul> all()
	{
		return Collections.unmodifiableList(hauls);
	}

	public int count()
	{
		return hauls.size();
	}

	public Haul last()
	{
		return hauls.isEmpty() ? null : hauls.get(hauls.size() - 1);
	}

	public int caught()
	{
		int n = 0;
		for (Haul h : hauls)
		{
			n += h.caught() ? 1 : 0;
		}
		return n;
	}

	public long kept()
	{
		long n = 0;
		for (Haul h : hauls)
		{
			n += h.kept;
		}
		return n;
	}

	/** The haul worth most, or null. */
	public Haul best(int rate)
	{
		Haul best = null;
		for (Haul h : hauls)
		{
			if (best == null || h.value(rate) > best.value(rate))
			{
				best = h;
			}
		}
		return best;
	}

	/** Your average valuables kept from a house, or -1 with none. */
	public int typical(House house)
	{
		long sum = 0;
		int n = 0;
		for (Haul h : hauls)
		{
			if (h.house == house)
			{
				sum += h.kept;
				n++;
			}
		}
		return n == 0 ? -1 : (int) Math.round((double) sum / n);
	}

	public int visits(House house)
	{
		int n = 0;
		for (Haul h : hauls)
		{
			n += h.house == house ? 1 : 0;
		}
		return n;
	}

	private long roundTicks()
	{
		long t = 0;
		for (Haul h : hauls)
		{
			t += h.round;
		}
		return t;
	}

	/** Coins an hour over your timed rounds, at Oriana's rate and the items' prices; -1 with none. */
	public double coinsPerHour(int rate)
	{
		long t = roundTicks();
		long sum = 0;
		for (Haul h : hauls)
		{
			sum += h.round > 0 ? h.value(rate) : 0;
		}
		return t == 0 ? -1 : sum * 3600.0 / (t * 0.6);
	}

	/** Thieving experience an hour over your timed rounds, the pickpocketing included; -1 with none. */
	public double xpPerHour()
	{
		long t = roundTicks();
		long sum = 0;
		for (Haul h : hauls)
		{
			sum += h.round > 0 ? h.roundXp : 0;
		}
		return t == 0 ? -1 : sum * 3600.0 / (t * 0.6);
	}

	/** Houses an hour over your timed rounds; -1 with none. */
	public double housesPerHour()
	{
		long t = roundTicks();
		long n = 0;
		for (Haul h : hauls)
		{
			n += h.round > 0 ? 1 : 0;
		}
		return t == 0 ? -1 : n * 3600.0 / (t * 0.6);
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

	/** "1;endedAt,house,ticks,round,taken,kept,xp,roundXp,items;..." with the house by name, "-" when not known. */
	public String encode()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (Haul h : hauls)
		{
			b.append(';').append(h.endedAt).append(',').append(h.house == null ? "-" : h.house.name()).append(',').append(h.ticks)
				.append(',').append(h.round).append(',').append(h.taken).append(',').append(h.kept).append(',').append(h.xp)
				.append(',').append(h.roundXp).append(',').append(h.items);
		}
		return b.toString();
	}

	/** Reads a save as far as it can; damaged hauls are left out. Never null. */
	public static HouseLog decode(String text)
	{
		HouseLog out = new HouseLog();
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
			if (f.length < 9)
			{
				continue;
			}
			try
			{
				long ended = Long.parseLong(f[0].trim());
				String name = f[1].trim();
				House house = "-".equals(name) ? null : House.valueOf(name);
				int[] n = new int[6];
				for (int k = 0; k < 6; k++)
				{
					n[k] = Integer.parseInt(f[2 + k].trim());
				}
				long items = Long.parseLong(f[8].trim());
				boolean ok = ended >= 0 && items >= 0;
				for (int v : n)
				{
					ok &= v >= 0;
				}
				if (ok)
				{
					out.add(new Haul(ended, house, n[0], n[1], n[2], n[3], n[4], n[5], items));
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
