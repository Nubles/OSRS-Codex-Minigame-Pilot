package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The points each boss has given you a kill, learnt from your own dreams: when the game's count
 * of points this dream rises as one boss leaves the arena, that boss gave the rise. The wiki's
 * figures are not used ("Points per boss is not accurate"), and they change with how many bosses
 * a customisable rumble has on, so only what you have seen is shown.
 */
public final class NmzKills
{
	/** Bosses kept. */
	static final int KEEP = 200;
	/** Points one kill can give, at most: a rise past it is not a kill. */
	static final int KILL_MAX = 100_000;
	private static final String VERSION = "1";

	/** One boss at one difficulty. */
	public static final class Boss
	{
		public final String name;
		/** 'H' hard, 'N' normal. */
		public final char difficulty;
		public int kills;
		public long points;

		Boss(String name, char difficulty)
		{
			this.name = name;
			this.difficulty = difficulty;
		}

		public double average()
		{
			return kills == 0 ? 0 : points / (double) kills;
		}
	}

	private final Map<String, Boss> bosses = new TreeMap<>();

	/** A kill of a boss that gave so many points. */
	public void kill(String name, char difficulty, int points)
	{
		String n = Nmz.cleanName(name);
		if (n.isEmpty() || points <= 0 || points > KILL_MAX || (difficulty != 'H' && difficulty != 'N'))
		{
			return;
		}
		String key = difficulty + "|" + n;
		Boss b = bosses.get(key);
		if (b == null)
		{
			if (bosses.size() >= KEEP)
			{
				return;
			}
			b = new Boss(n, difficulty);
			bosses.put(key, b);
		}
		b.kills = Math.min(1_000_000, b.kills + 1);
		b.points += points;
	}

	public Boss get(String name, char difficulty)
	{
		return bosses.get(difficulty + "|" + Nmz.cleanName(name));
	}

	/** Every boss learnt, the most points a kill first. */
	public List<Boss> all()
	{
		List<Boss> out = new ArrayList<>(bosses.values());
		out.sort((a, b) -> Double.compare(b.average(), a.average()));
		return Collections.unmodifiableList(out);
	}

	public int count()
	{
		return bosses.size();
	}

	/** "1;H|Name|kills|points;..." */
	public String encode()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (Boss x : bosses.values())
		{
			b.append(';').append(x.difficulty).append('|').append(x.name).append('|').append(x.kills).append('|').append(x.points);
		}
		return b.toString();
	}

	/** Reads a save as far as it can; damaged bosses are left out. Never null. */
	public static NmzKills decode(String text)
	{
		NmzKills out = new NmzKills();
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
			String[] f = parts[i].split("\\|");
			if (f.length != 4 || f[0].length() != 1)
			{
				continue;
			}
			try
			{
				char d = f[0].charAt(0);
				String n = Nmz.cleanName(f[1]);
				int kills = Integer.parseInt(f[2].trim());
				long points = Long.parseLong(f[3].trim());
				if ((d != 'H' && d != 'N') || n.isEmpty() || kills <= 0 || kills > 1_000_000 || points <= 0 || points > (long) kills * KILL_MAX
					|| out.bosses.size() >= KEEP)
				{
					continue;
				}
				Boss b = new Boss(n, d);
				b.kills = kills;
				b.points = points;
				out.bosses.put(d + "|" + n, b);
			}
			catch (IllegalArgumentException e)
			{
				// Left out: a bad number.
			}
		}
		return out;
	}
}
