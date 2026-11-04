package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Your Mahogany Homes contracts one by one, kept per account: the client, the tier, the
 * Construction experience it gave (0 when the job was first seen part done, so not measured) and
 * the whole loop's time from the hand-in before. Points are the tier's (OSRS wiki, Carpenter
 * points: beginner 2, novice 3, adept 4, expert 5). From these the panel gives points and
 * experience an hour across contracts, and your quickest contract.
 */
public final class ContractLog
{
	/** Contracts kept: enough for a pace, small enough for a saved setting. */
	static final int KEEP = 50;
	private static final String VERSION = "1";

	/** One contract handed in. */
	public static final class Entry
	{
		public final long endedAt;
		public final Home home;
		public final Contract.Tier tier;
		/** Experience from taking the job to the hand-in, or 0 when not measured. */
		public final int xp;
		/** The loop from the last hand-in, or 0 when not timed. */
		public final int seconds;

		public Entry(long endedAt, Home home, Contract.Tier tier, int xp, int seconds)
		{
			this.endedAt = Math.max(0, endedAt);
			this.home = home;
			this.tier = tier;
			this.xp = Math.max(0, xp);
			this.seconds = seconds > 0 && seconds <= ContractBook.MAX_LOOP_SECONDS ? seconds : 0;
		}

		public int points()
		{
			return tier.points();
		}
	}

	private final List<Entry> entries = new ArrayList<>();

	public void add(Entry e)
	{
		if (e == null || e.home == null || e.tier == null)
		{
			return;
		}
		entries.add(e);
		while (entries.size() > KEEP)
		{
			entries.remove(0);
		}
	}

	public List<Entry> all()
	{
		return Collections.unmodifiableList(entries);
	}

	public int count()
	{
		return entries.size();
	}

	/** Points an hour over the timed contracts kept, or -1 with none timed. */
	public double pointsPerHour()
	{
		long seconds = 0;
		long points = 0;
		for (Entry e : entries)
		{
			if (e.seconds > 0)
			{
				seconds += e.seconds;
				points += e.points();
			}
		}
		return seconds == 0 ? -1 : points * 3600.0 / seconds;
	}

	/** Experience an hour over contracts both timed and measured, or -1 with none. */
	public double xpPerHour()
	{
		long seconds = 0;
		long xp = 0;
		for (Entry e : entries)
		{
			if (e.seconds > 0 && e.xp > 0)
			{
				seconds += e.seconds;
				xp += e.xp;
			}
		}
		return seconds == 0 ? -1 : xp * 3600.0 / seconds;
	}

	/** The quickest timed contract of a tier, or null. */
	public Entry quickest(Contract.Tier tier)
	{
		Entry best = null;
		for (Entry e : entries)
		{
			if (e.tier == tier && e.seconds > 0 && (best == null || e.seconds < best.seconds))
			{
				best = e;
			}
		}
		return best;
	}

	/** "1;endedAt,HOME,TIER,xp,seconds;..." with the client and tier by name. */
	public String encode()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (Entry e : entries)
		{
			b.append(';').append(e.endedAt).append(',').append(e.home.name()).append(',').append(e.tier.name())
				.append(',').append(e.xp).append(',').append(e.seconds);
		}
		return b.toString();
	}

	/** Reads a save as far as it can; damaged entries are left out. Never null. */
	public static ContractLog decode(String text)
	{
		ContractLog out = new ContractLog();
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
				Home home = Home.valueOf(f[1].trim());
				Contract.Tier tier = Contract.Tier.valueOf(f[2].trim());
				int xp = Integer.parseInt(f[3].trim());
				int seconds = Integer.parseInt(f[4].trim());
				if (ended < 0 || xp < 0 || xp > ContractBook.MAX_XP || seconds < 0)
				{
					continue;
				}
				out.add(new Entry(ended, home, tier, xp, seconds));
			}
			catch (IllegalArgumentException e)
			{
				// Left out; NumberFormatException is one of these too.
			}
		}
		return out;
	}
}
