package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Your Wintertodt rounds, kept per account: each round's points, its time from the energy bar's
 * first reading to the Wintertodt subdued, the wait before it, and the knocks you took. From these
 * the panel says your points and rewards an hour at your own pace, and so the rounds and time to
 * the reward you are after.
 *
 * <p>A round you left before its end gave nothing ("Logging out, or leaving the prison before the
 * end of a round will result in the loss of all points for the current round"); it is kept with
 * its points as 0, so the hour it took still counts against your pace.
 */
public final class WintertodtLog
{
	/** Rounds kept. */
	static final int KEEP = 200;
	/** A round longer than this had a break in it and is not timed (ticks: thirty minutes). */
	public static final int LONGEST_ROUND = 3000;
	/** A wait longer than this before a round is a break, not the respawn (ticks: five minutes). */
	public static final int LONGEST_WAIT = 500;
	/** Points a round can give, at most: a guard against damage (the wiki knows no limit). */
	static final int POINTS_MAX = 100_000;
	private static final String VERSION = "1";

	/** One round. */
	public static final class Game
	{
		public final long endedAt;
		/** Ticks from the round's start to its end, or 0 when not timed (joined late, or a break). */
		public final int ticks;
		/** Ticks waited for it to start, or 0 when not known. */
		public final int wait;
		/** Points it gave: 0 when you left before its end. */
		public final int points;
		/** It ran to its end with you in the prison. */
		public final boolean subdued;
		/** Knocks taken: the cold, snowfall and a shattering brazier. */
		public final int hits;

		public Game(long endedAt, int ticks, int wait, int points, boolean subdued, int hits)
		{
			this.endedAt = Math.max(0, endedAt);
			this.ticks = ticks > 0 && ticks <= LONGEST_ROUND ? ticks : 0;
			this.wait = wait > 0 && wait <= LONGEST_WAIT ? wait : 0;
			this.points = subdued ? Math.max(0, Math.min(POINTS_MAX, points)) : 0;
			this.subdued = subdued;
			this.hits = Math.max(0, Math.min(999, hits));
		}

		/** The rewards it gave on average: its sure ones and the chance of one more. */
		public double rolls()
		{
			return Wintertodt.rolls(points);
		}

		/** Ticks it cost: the round and the wait before it, the wiki's one-minute respawn when the wait is not known. */
		public int cost()
		{
			return ticks <= 0 ? 0 : ticks + (wait > 0 ? wait : Wintertodt.RESPAWN_TICKS);
		}
	}

	private final List<Game> games = new ArrayList<>();

	public void add(Game g)
	{
		if (g == null)
		{
			return;
		}
		games.add(g);
		while (games.size() > KEEP)
		{
			games.remove(0);
		}
	}

	public List<Game> all()
	{
		return Collections.unmodifiableList(games);
	}

	public int count()
	{
		return games.size();
	}

	public Game last()
	{
		return games.isEmpty() ? null : games.get(games.size() - 1);
	}

	/** The round with the most points, or null. */
	public Game best()
	{
		Game best = null;
		for (Game g : games)
		{
			if (g.subdued && (best == null || g.points > best.points))
			{
				best = g;
			}
		}
		return best;
	}

	/** Rewards your kept rounds have earned, on average. */
	public double rewards()
	{
		double n = 0;
		for (Game g : games)
		{
			n += g.rolls();
		}
		return n;
	}

	/** Rounds that reached the 500 points a reward needs. */
	public int paid()
	{
		int n = 0;
		for (Game g : games)
		{
			n += g.points >= Wintertodt.MIN_POINTS ? 1 : 0;
		}
		return n;
	}

	/** Points an hour over your timed rounds, waits too, or -1 with none timed. */
	public double pointsPerHour()
	{
		long t = 0;
		long p = 0;
		for (Game g : games)
		{
			if (g.cost() > 0)
			{
				t += g.cost();
				p += g.points;
			}
		}
		return t == 0 ? -1 : p * 3600.0 / (t * 0.6);
	}

	/** Rewards an hour over your timed rounds, waits too, or -1 with none timed. */
	public double rewardsPerHour()
	{
		long t = 0;
		double r = 0;
		for (Game g : games)
		{
			if (g.cost() > 0)
			{
				t += g.cost();
				r += g.rolls();
			}
		}
		return t == 0 ? -1 : r * 3600.0 / (t * 0.6);
	}

	/** Rewards a round, on average over every kept round (left ones count as none), or -1 with none. */
	public double rewardsPerRound()
	{
		return games.isEmpty() ? -1 : rewards() / games.size();
	}

	/** Average ticks a timed round costs, the wait too, or 0. */
	public int typicalRound()
	{
		long t = 0;
		int n = 0;
		for (Game g : games)
		{
			if (g.cost() > 0)
			{
				t += g.cost();
				n++;
			}
		}
		return n == 0 ? 0 : (int) Math.round(t / (double) n);
	}

	/** "1;endedAt,ticks,wait,points,s|l,hits;..." */
	public String encode()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (Game g : games)
		{
			b.append(';').append(g.endedAt).append(',').append(g.ticks).append(',').append(g.wait).append(',').append(g.points)
				.append(',').append(g.subdued ? 's' : 'l').append(',').append(g.hits);
		}
		return b.toString();
	}

	/** Reads a save as far as it can; damaged rounds are left out. Never null. */
	public static WintertodtLog decode(String text)
	{
		WintertodtLog out = new WintertodtLog();
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
				int wait = Integer.parseInt(f[2].trim());
				int points = Integer.parseInt(f[3].trim());
				String how = f[4].trim();
				int hits = Integer.parseInt(f[5].trim());
				if (ended < 0 || ticks < 0 || wait < 0 || points < 0 || hits < 0 || !("s".equals(how) || "l".equals(how)))
				{
					continue;
				}
				out.add(new Game(ended, ticks, wait, points, "s".equals(how), hits));
			}
			catch (IllegalArgumentException e)
			{
				// Left out: a bad number.
			}
		}
		return out;
	}
}
