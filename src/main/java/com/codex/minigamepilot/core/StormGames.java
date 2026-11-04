package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Your Tempoross games, kept per account: points, the reward permits they paid and how long they
 * took, so the panel can say your pace across games and what the game under way is worth.
 *
 * <p>Facts (OSRS wiki, Tempoross): "One reward permit is given for the first 2,000 points
 * scored, with an additional permit awarded for every 700 points beyond 2,000. Excess points
 * count towards a linearly scaling chance for an additional permit." Points are capped at
 * 32,000. If the storm washes you away, the points of that game are lost.
 */
public final class StormGames
{
	public static final int FIRST_PERMIT = 2000;
	public static final int PERMIT_STEP = 700;
	public static final int MAX_POINTS = 32000;
	/** Games kept: enough for a pace, small enough for a saved setting. */
	static final int KEEP = 30;
	private static final String VERSION = "1";

	/** One game. */
	public static final class Game
	{
		public final long endedAt;
		public final int ticks;
		public final int points;
		/** Permits it paid: read from the game's count when it rose, else from the points. */
		public final int permits;
		public final boolean won;

		public Game(long endedAt, int ticks, int points, int permits, boolean won)
		{
			this.endedAt = Math.max(0, endedAt);
			this.ticks = Math.max(0, ticks);
			this.points = Math.max(0, Math.min(MAX_POINTS, points));
			this.permits = Math.max(0, Math.min(sure(MAX_POINTS) + 1, permits));
			this.won = won;
		}

		Game permits(int n)
		{
			return new Game(endedAt, ticks, points, n, won);
		}
	}

	private final List<Game> games = new ArrayList<>();

	/** Permits sure to come from these points: none under 2,000, then one and one per 700 more. */
	public static int sure(int points)
	{
		int p = Math.max(0, Math.min(MAX_POINTS, points));
		return p < FIRST_PERMIT ? 0 : 1 + (p - FIRST_PERMIT) / PERMIT_STEP;
	}

	/** The chance, 0 to 1, of one more permit from the points past the last whole one. */
	public static double chance(int points)
	{
		int p = Math.max(0, Math.min(MAX_POINTS, points));
		return p < FIRST_PERMIT ? 0 : ((p - FIRST_PERMIT) % PERMIT_STEP) / (double) PERMIT_STEP;
	}

	/** Points still to score for the next sure permit. */
	public static int toNext(int points)
	{
		int p = Math.max(0, points);
		if (p < FIRST_PERMIT)
		{
			return FIRST_PERMIT - p;
		}
		return PERMIT_STEP - (p - FIRST_PERMIT) % PERMIT_STEP;
	}

	public void add(Game g)
	{
		games.add(g);
		while (games.size() > KEEP)
		{
			games.remove(0);
		}
	}

	/** The permits of the last game, corrected to what the game's own count said. */
	public void correctLast(int permits)
	{
		if (!games.isEmpty())
		{
			games.set(games.size() - 1, games.get(games.size() - 1).permits(permits));
		}
	}

	public List<Game> all()
	{
		return Collections.unmodifiableList(games);
	}

	public Game last()
	{
		return games.isEmpty() ? null : games.get(games.size() - 1);
	}

	public int count()
	{
		return games.size();
	}

	/** The game with the most points, or null. */
	public Game best()
	{
		Game best = null;
		for (Game g : games)
		{
			if (best == null || g.points > best.points)
			{
				best = g;
			}
		}
		return best;
	}

	/** Permits an hour over the games kept that were timed, or -1 with none. */
	public double permitsPerHour()
	{
		long ticks = 0;
		int permits = 0;
		for (Game g : games)
		{
			if (g.ticks > 0)
			{
				ticks += g.ticks;
				permits += g.permits;
			}
		}
		return ticks == 0 ? -1 : permits * 3600.0 / (ticks * 0.6);
	}

	/** The median length of a won game, in ticks, or 0 with none. */
	public int typicalTicks()
	{
		List<Integer> t = new ArrayList<>();
		for (Game g : games)
		{
			if (g.won && g.ticks > 0)
			{
				t.add(g.ticks);
			}
		}
		if (t.isEmpty())
		{
			return 0;
		}
		Collections.sort(t);
		return t.get(t.size() / 2);
	}

	/** The mean points of a game kept, or -1 with none. */
	public int averagePoints()
	{
		if (games.isEmpty())
		{
			return -1;
		}
		long sum = 0;
		for (Game g : games)
		{
			sum += g.points;
		}
		return (int) (sum / games.size());
	}

	/** "1;endedAt,ticks,points,permits,won;..." */
	public String encode()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (Game g : games)
		{
			b.append(';').append(g.endedAt).append(',').append(g.ticks).append(',').append(g.points)
				.append(',').append(g.permits).append(',').append(g.won ? 1 : 0);
		}
		return b.toString();
	}

	/** Reads a save as far as it can; damaged games are left out. Never null. */
	public static StormGames decode(String s)
	{
		StormGames out = new StormGames();
		if (s == null)
		{
			return out;
		}
		String[] parts = s.split(";");
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
				int points = Integer.parseInt(f[2].trim());
				int permits = Integer.parseInt(f[3].trim());
				String won = f[4].trim();
				if (ended < 0 || ticks < 0 || points < 0 || permits < 0 || !("0".equals(won) || "1".equals(won)))
				{
					continue;
				}
				out.add(new Game(ended, ticks, points, permits, "1".equals(won)));
			}
			catch (NumberFormatException e)
			{
				// Left out.
			}
		}
		return out;
	}
}
