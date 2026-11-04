package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Your Soul Wars games, kept per account: each game's team, whether it was won, lost or drawn,
 * the zeal it earned (the game's own count of your tokens), your part in it as the end-of-game
 * scroll gives it, its length and how it ended. From these the panel says your zeal an hour and
 * so the time to the reward you chose.
 */
public final class SoulWarsLog
{
	/** Games kept. */
	static final int KEEP = 300;
	/** A game longer than this is not timed (ticks: thirty minutes; a game is fifteen at most). */
	public static final int LONGEST = 3_000;
	/** Zeal a game can give, at most: the wiki's 60 for a win. */
	public static final int ZEAL_MAX = SoulWars.WIN_MAX;
	/** A score or a count on the scroll, at most: a guard against damage. */
	public static final int COUNT_MAX = 99_999;
	/** Two games ending this far apart or nearer are one sitting, so the wait between them is counted (ms: thirty minutes). */
	public static final long SITTING_GAP = 30 * 60_000L;
	private static final String VERSION = "1";

	/** How a game turned out. */
	public enum Result
	{
		WON('w', "Won"),
		LOST('l', "Lost"),
		DRAWN('d', "Drawn"),
		/** Not read: left early, or no scores seen. */
		UNKNOWN('u', "");

		final char code;
		private final String title;

		Result(char code, String title)
		{
			this.code = code;
			this.title = title;
		}

		public String getTitle()
		{
			return title;
		}

		static Result byCode(String s)
		{
			for (Result r : values())
			{
				if (s.length() == 1 && s.charAt(0) == r.code)
				{
					return r;
				}
			}
			return null;
		}
	}

	/** How a game ended. */
	public enum End
	{
		/** Played to the end. */
		FINISHED('f', "Finished"),
		/** Left before the end. */
		LEFT('l', "Left"),
		/** Logged out or hopped in the game. */
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

	/** Your part in a game, as the end-of-game scroll counts it; -1 for each not read. */
	public static final class Part
	{
		public int kills = -1;
		public int captures = -1;
		public int damage = -1;
		public int fragments = -1;
		public int bones = -1;

		Part copy()
		{
			Part p = new Part();
			p.kills = kills;
			p.captures = captures;
			p.damage = damage;
			p.fragments = fragments;
			p.bones = bones;
			return p;
		}

		static int clean(int n)
		{
			return n < 0 ? -1 : Math.min(COUNT_MAX, n);
		}
	}

	/** One game. */
	public static final class Game
	{
		public final long endedAt;
		/** Ticks in the game, or 0 when not timed. */
		public final int ticks;
		/** Your team, or null when not seen. */
		public final SoulWars.Team team;
		public final Result result;
		/** Zeal it earned. */
		public final int zeal;
		/** The scroll's blue and red scores, or -1 when not read. */
		public final int blue;
		public final int red;
		public final Part part;
		public final End end;

		public Game(long endedAt, int ticks, SoulWars.Team team, Result result, int zeal, int blue, int red, Part part, End end)
		{
			this.endedAt = Math.max(0, endedAt);
			this.ticks = ticks > 0 && ticks <= LONGEST ? ticks : 0;
			this.team = team;
			this.result = result == null ? Result.UNKNOWN : result;
			this.zeal = Math.max(0, Math.min(ZEAL_MAX, zeal));
			this.blue = Part.clean(blue);
			this.red = Part.clean(red);
			Part p = part == null ? new Part() : part.copy();
			p.kills = Part.clean(p.kills);
			p.captures = Part.clean(p.captures);
			p.damage = Part.clean(p.damage);
			p.fragments = Part.clean(p.fragments);
			p.bones = Part.clean(p.bones);
			this.part = p;
			this.end = end == null ? End.LEFT : end;
		}

		/** "2-1" with your team first, or "" when not read. */
		public String score()
		{
			if (team == null || blue < 0 || red < 0)
			{
				return "";
			}
			return team == SoulWars.Team.BLUE ? blue + "-" + red : red + "-" + blue;
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

	/** The game that earned most, or null. */
	public Game best()
	{
		Game best = null;
		for (Game g : games)
		{
			if (g.zeal > 0 && (best == null || g.zeal > best.zeal))
			{
				best = g;
			}
		}
		return best;
	}

	/** Zeal an hour in the game over your timed games, or -1 with none. */
	public double zealPerHour()
	{
		long t = 0;
		long n = 0;
		for (Game g : games)
		{
			if (g.ticks > 0)
			{
				t += g.ticks;
				n += g.zeal;
			}
		}
		return t == 0 ? -1 : n * 3600.0 / (t * 0.6);
	}

	/**
	 * Zeal an hour with the wait between games counted: over each game that followed another within
	 * a sitting, what it earned over the time since the last one ended. -1 with fewer than two such games.
	 */
	public double zealPerHourSitting()
	{
		long ms = 0;
		long n = 0;
		int pairs = 0;
		for (int i = 1; i < games.size(); i++)
		{
			long gap = games.get(i).endedAt - games.get(i - 1).endedAt;
			if (gap > 0 && gap <= SITTING_GAP && games.get(i).ticks > 0)
			{
				ms += gap;
				n += games.get(i).zeal;
				pairs++;
			}
		}
		return pairs < 2 || ms <= 0 ? -1 : n * 3_600_000.0 / ms;
	}

	/** Your pace: with the waits counted when there are enough games for it, else in the game; -1 with none. */
	public double pace()
	{
		double sit = zealPerHourSitting();
		return sit > 0 ? sit : zealPerHour();
	}

	/** Average zeal a game played to the end, or 0. */
	public double averageZeal()
	{
		long z = 0;
		int n = 0;
		for (Game g : games)
		{
			if (g.end == End.FINISHED)
			{
				z += g.zeal;
				n++;
			}
		}
		return n == 0 ? 0 : z / (double) n;
	}

	/** Games played to the end. */
	public int finished()
	{
		int n = 0;
		for (Game g : games)
		{
			n += g.end == End.FINISHED ? 1 : 0;
		}
		return n;
	}

	/** Average zeal of games with this result, or -1 with none. */
	public double averageZeal(Result r)
	{
		long z = 0;
		int n = 0;
		for (Game g : games)
		{
			if (g.result == r && g.end == End.FINISHED)
			{
				z += g.zeal;
				n++;
			}
		}
		return n == 0 ? -1 : z / (double) n;
	}

	/** Games with this result. */
	public int count(Result r)
	{
		int n = 0;
		for (Game g : games)
		{
			n += g.result == r ? 1 : 0;
		}
		return n;
	}

	/** Games whose result was read. */
	public int decided()
	{
		return games.size() - count(Result.UNKNOWN);
	}

	/** Zeal of all games kept. */
	public long totalZeal()
	{
		long n = 0;
		for (Game g : games)
		{
			n += g.zeal;
		}
		return n;
	}

	/**
	 * Of the zeal your games could have given (the wiki's 60 a win, 50 a draw, 40 a loss), the share
	 * they did: how well you keep your activity up. -1 with no decided game played to the end.
	 */
	public double share()
	{
		long got = 0;
		long most = 0;
		for (Game g : games)
		{
			if (g.end != End.FINISHED || g.result == Result.UNKNOWN)
			{
				continue;
			}
			got += g.zeal;
			most += g.result == Result.WON ? SoulWars.WIN_MAX : g.result == Result.DRAWN ? SoulWars.DRAW_MAX : SoulWars.LOSS_MAX;
		}
		return most == 0 ? -1 : got / (double) most;
	}

	/** "1;endedAt,ticks,team,result,zeal,blue,red,kills,captures,damage,fragments,bones,end;..." */
	public String encode()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (Game g : games)
		{
			b.append(';').append(g.endedAt).append(',').append(g.ticks).append(',').append(g.team == null ? '-' : g.team.code)
				.append(',').append(g.result.code).append(',').append(g.zeal).append(',').append(g.blue).append(',').append(g.red)
				.append(',').append(g.part.kills).append(',').append(g.part.captures).append(',').append(g.part.damage)
				.append(',').append(g.part.fragments).append(',').append(g.part.bones).append(',').append(g.end.code);
		}
		return b.toString();
	}

	/** Reads a save as far as it can; damaged games are left out. Never null. */
	public static SoulWarsLog decode(String text)
	{
		SoulWarsLog out = new SoulWarsLog();
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
			if (f.length < 13)
			{
				continue;
			}
			try
			{
				long ended = Long.parseLong(f[0].trim());
				int ticks = Integer.parseInt(f[1].trim());
				String t = f[2].trim();
				SoulWars.Team team = t.length() == 1 ? SoulWars.Team.byCode(t.charAt(0)) : null;
				Result result = Result.byCode(f[3].trim());
				int zeal = Integer.parseInt(f[4].trim());
				int blue = Integer.parseInt(f[5].trim());
				int red = Integer.parseInt(f[6].trim());
				Part p = new Part();
				p.kills = Integer.parseInt(f[7].trim());
				p.captures = Integer.parseInt(f[8].trim());
				p.damage = Integer.parseInt(f[9].trim());
				p.fragments = Integer.parseInt(f[10].trim());
				p.bones = Integer.parseInt(f[11].trim());
				End end = End.byCode(f[12].trim());
				if (ended < 0 || ticks < 0 || zeal < 0 || result == null || end == null || (team == null && !"-".equals(t)))
				{
					continue;
				}
				out.add(new Game(ended, ticks, team, result, zeal, blue, red, p, end));
			}
			catch (IllegalArgumentException e)
			{
				// Left out: a bad number.
			}
		}
		return out;
	}
}
