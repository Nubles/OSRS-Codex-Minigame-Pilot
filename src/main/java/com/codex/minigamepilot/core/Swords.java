package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Your Giants' Foundry swords, kept per account: the reputation each paid (the wiki: reputation
 * equals the sword's final quality), the Smithing experience Kovac gave for it, and how long it
 * took from the commission to the hand-in. From these the panel says your reputation and
 * experience an hour across swords, and your best sword: the highest quality, then the quickest,
 * as the game's own personal best is judged (OSRS wiki, Giants' Foundry).
 */
public final class Swords
{
	/** Swords kept: enough for a pace, small enough for a saved setting. */
	static final int KEEP = 50;
	private static final String VERSION = "1";

	/** One sword handed in. */
	public static final class Sword
	{
		public final long endedAt;
		/** Ticks from the commission to the hand-in, or 0 when the commission was not seen. */
		public final int ticks;
		public final int quality;
		public final int xp;

		public Sword(long endedAt, int ticks, int quality, int xp)
		{
			this.endedAt = Math.max(0, endedAt);
			this.ticks = Math.max(0, ticks);
			this.quality = Math.max(0, Math.min(1000, quality));
			this.xp = Math.max(0, xp);
		}
	}

	private final List<Sword> swords = new ArrayList<>();

	public void add(Sword s)
	{
		swords.add(s);
		while (swords.size() > KEEP)
		{
			swords.remove(0);
		}
	}

	public List<Sword> all()
	{
		return Collections.unmodifiableList(swords);
	}

	public int count()
	{
		return swords.size();
	}

	public Sword last()
	{
		return swords.isEmpty() ? null : swords.get(swords.size() - 1);
	}

	/** The best sword: highest quality, then fewest ticks among those timed. Null with none. */
	public Sword best()
	{
		Sword best = null;
		for (Sword s : swords)
		{
			if (best == null || s.quality > best.quality
				|| s.quality == best.quality && s.ticks > 0 && (best.ticks == 0 || s.ticks < best.ticks))
			{
				best = s;
			}
		}
		return best;
	}

	/** Reputation an hour over the timed swords kept, or -1 with none. */
	public double repPerHour()
	{
		return perHour(true);
	}

	/** Hand-in experience an hour over the timed swords kept, or -1 with none. */
	public double xpPerHour()
	{
		return perHour(false);
	}

	private double perHour(boolean rep)
	{
		long ticks = 0;
		long sum = 0;
		for (Sword s : swords)
		{
			if (s.ticks > 0)
			{
				ticks += s.ticks;
				sum += rep ? s.quality : s.xp;
			}
		}
		return ticks == 0 ? -1 : sum * 3600.0 / (ticks * 0.6);
	}

	/** The median ticks of a timed sword, or 0 with none. */
	public int typicalTicks()
	{
		List<Integer> t = new ArrayList<>();
		for (Sword s : swords)
		{
			if (s.ticks > 0)
			{
				t.add(s.ticks);
			}
		}
		if (t.isEmpty())
		{
			return 0;
		}
		Collections.sort(t);
		return t.get(t.size() / 2);
	}

	/** "1;endedAt,ticks,quality,xp;..." */
	public String encode()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (Sword s : swords)
		{
			b.append(';').append(s.endedAt).append(',').append(s.ticks).append(',').append(s.quality).append(',').append(s.xp);
		}
		return b.toString();
	}

	/** Reads a save as far as it can; damaged swords are left out. Never null. */
	public static Swords decode(String text)
	{
		Swords out = new Swords();
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
			if (f.length < 4)
			{
				continue;
			}
			try
			{
				long ended = Long.parseLong(f[0].trim());
				int ticks = Integer.parseInt(f[1].trim());
				int quality = Integer.parseInt(f[2].trim());
				int xp = Integer.parseInt(f[3].trim());
				if (ended < 0 || ticks < 0 || quality < 0 || xp < 0)
				{
					continue;
				}
				out.add(new Sword(ended, ticks, quality, xp));
			}
			catch (NumberFormatException e)
			{
				// Left out.
			}
		}
		return out;
	}
}
