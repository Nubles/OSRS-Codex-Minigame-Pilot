package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Your Volcanic Mine games, kept per account: each game's points (the game's own count), the
 * Mining experience it gave, its length, the players in it and how it ended. From these the panel
 * says your points and experience an hour, in the mine and with the wait between games counted,
 * and so the time to the reward you are after.
 */
public final class VolcanicLog
{
	/** Games kept. */
	static final int KEEP = 200;
	/** A game longer than this is not timed (ticks: thirty minutes; a game is ten and a half). */
	public static final int LONGEST = 3_000;
	/** Points a game can give, at most: a guard against damage. */
	public static final int POINTS_MAX = 20_000;
	/** Experience a game can give, at most: a guard against damage. */
	public static final int XP_MAX = 200_000;
	/** Two games ending this far apart or nearer are one sitting, so the wait between them is counted (ms: thirty minutes). */
	public static final long SITTING_GAP = 30 * 60_000L;
	private static final String VERSION = "1";

	/** How a game ended. */
	public enum End
	{
		/** Left by the exit, alive. */
		LEFT('l', "Left"),
		/** Died: "Oh dear, you are dead!" in the mine, the volcano not yet at its end. */
		DIED('d', "Died"),
		/** Died as the volcano erupted: the time out, or stability at 0%. */
		ERUPTED('e', "Erupted"),
		/** Logged out or hopped in the mine. */
		LOGOUT('o', "Logged out");

		final char code;
		private final String title;

		End(char code, String title)
		{
			this.code = code;
			this.title = title;
		}

		public String getTitle()
		{
			return title;
		}

		static End byCode(String s)
		{
			for (End e : values())
			{
				if (s.length() == 1 && s.charAt(0) == e.code)
				{
					return e;
				}
			}
			return null;
		}
	}

	/** One game. */
	public static final class Game
	{
		public final long endedAt;
		/** Ticks in the mine, the 30 seconds to prepare included. */
		public final int ticks;
		public final int points;
		/** Mining experience gained, or 0. */
		public final int xp;
		/** Most players seen in it, or 0 when not read. */
		public final int players;
		public final End end;

		public Game(long endedAt, int ticks, int points, int xp, int players, End end)
		{
			this.endedAt = Math.max(0, endedAt);
			this.ticks = ticks > 0 && ticks <= LONGEST ? ticks : 0;
			this.points = Math.max(0, Math.min(POINTS_MAX, points));
			this.xp = Math.max(0, Math.min(XP_MAX, xp));
			this.players = Math.max(0, Math.min(99, players));
			this.end = end == null ? End.LEFT : end;
		}

		/** "Solo", "3 players", or "". */
		public String team()
		{
			return players <= 0 ? "" : players == 1 ? "Solo" : players + " players";
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

	/** The game with the most points, or null. */
	public Game best()
	{
		Game best = null;
		for (Game g : games)
		{
			if (g.points > 0 && (best == null || g.points > best.points))
			{
				best = g;
			}
		}
		return best;
	}

	/** Points kept since a moment (ms), from games ended after it. */
	public long pointsSince(long at)
	{
		long n = 0;
		for (Game g : games)
		{
			if (g.endedAt > at)
			{
				n += g.points;
			}
		}
		return n;
	}

	/** Points an hour in the mine over your timed games, or -1 with none. */
	public double pointsPerHour()
	{
		long t = 0;
		long p = 0;
		for (Game g : games)
		{
			if (g.ticks > 0)
			{
				t += g.ticks;
				p += g.points;
			}
		}
		return t == 0 ? -1 : p * 3600.0 / (t * 0.6);
	}

	/** Mining experience an hour in the mine over your timed games, or -1 with none. */
	public double xpPerHour()
	{
		long t = 0;
		long x = 0;
		for (Game g : games)
		{
			if (g.ticks > 0)
			{
				t += g.ticks;
				x += g.xp;
			}
		}
		return t == 0 ? -1 : x * 3600.0 / (t * 0.6);
	}

	/** Games where the time from the last game's end to this one's is known: the sitting's pairs. */
	private int[] sittingIndexes()
	{
		List<Integer> out = new ArrayList<>();
		for (int i = 1; i < games.size(); i++)
		{
			long gap = games.get(i).endedAt - games.get(i - 1).endedAt;
			if (gap > 0 && gap <= SITTING_GAP && games.get(i).ticks > 0)
			{
				out.add(i);
			}
		}
		int[] a = new int[out.size()];
		for (int i = 0; i < a.length; i++)
		{
			a[i] = out.get(i);
		}
		return a;
	}

	/**
	 * Points an hour with the wait between games counted: over each game that followed another
	 * within a sitting, its points over the time since the last one ended. -1 with fewer than two.
	 */
	public double pointsPerHourSitting()
	{
		int[] idx = sittingIndexes();
		if (idx.length < 2)
		{
			return -1;
		}
		long ms = 0;
		long p = 0;
		for (int i : idx)
		{
			ms += games.get(i).endedAt - games.get(i - 1).endedAt;
			p += games.get(i).points;
		}
		return ms <= 0 ? -1 : p * 3_600_000.0 / ms;
	}

	/** Mining experience an hour with the wait between games counted, as {@link #pointsPerHourSitting}. */
	public double xpPerHourSitting()
	{
		int[] idx = sittingIndexes();
		if (idx.length < 2)
		{
			return -1;
		}
		long ms = 0;
		long x = 0;
		for (int i : idx)
		{
			ms += games.get(i).endedAt - games.get(i - 1).endedAt;
			x += games.get(i).xp;
		}
		return ms <= 0 ? -1 : x * 3_600_000.0 / ms;
	}

	/** Average points a game, or 0. */
	public int averagePoints()
	{
		long p = 0;
		int n = 0;
		for (Game g : games)
		{
			if (g.points > 0)
			{
				p += g.points;
				n++;
			}
		}
		return n == 0 ? 0 : (int) (p / n);
	}

	/** Games that ended in a death, eruption or not. */
	public int deaths()
	{
		int n = 0;
		for (Game g : games)
		{
			n += g.end == End.DIED || g.end == End.ERUPTED ? 1 : 0;
		}
		return n;
	}

	/** "1;endedAt,ticks,points,xp,players,end;..." */
	public String encode()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (Game g : games)
		{
			b.append(';').append(g.endedAt).append(',').append(g.ticks).append(',').append(g.points).append(',').append(g.xp)
				.append(',').append(g.players).append(',').append(g.end.code);
		}
		return b.toString();
	}

	/** Reads a save as far as it can; damaged games are left out. Never null. */
	public static VolcanicLog decode(String text)
	{
		VolcanicLog out = new VolcanicLog();
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
			String[] f = parts[i].split(",", -1);
			if (f.length < 6)
			{
				continue;
			}
			try
			{
				long ended = Long.parseLong(f[0].trim());
				int ticks = Integer.parseInt(f[1].trim());
				int points = Integer.parseInt(f[2].trim());
				int xp = Integer.parseInt(f[3].trim());
				int players = Integer.parseInt(f[4].trim());
				End end = End.byCode(f[5].trim());
				if (ended < 0 || ticks < 0 || points < 0 || xp < 0 || players < 0 || end == null)
				{
					continue;
				}
				out.add(new Game(ended, ticks, points, xp, players, end));
			}
			catch (IllegalArgumentException e)
			{
				// Left out: a bad number.
			}
		}
		return out;
	}
}
