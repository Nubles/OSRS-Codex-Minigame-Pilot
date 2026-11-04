package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Your Trouble Brewing games, kept per account: each game's team, the rum your team made and the
 * other team's (the game's own scores: "Each bottle of rum is worth one point"), the pieces of
 * eight it paid (the game's own count), the Cooking experience it gave, its length and how it
 * ended. From these the panel says your pieces of eight an hour and so the time to your reward.
 */
public final class BrewingLog
{
	/** Games kept. */
	static final int KEEP = 200;
	/** A game longer than this is not timed (ticks: forty minutes; a game is twenty). */
	public static final int LONGEST = 4_000;
	/** Rum a team can make in a game, at most: a guard against damage. */
	public static final int RUM_MAX = 999;
	/** Pieces of eight a game can give, at most: a guard against damage. */
	public static final int PIECES_MAX = 20_000;
	/** Experience a game can give, at most: a guard against damage. */
	public static final int XP_MAX = 500_000;
	/** Two games ending this far apart or nearer are one sitting, so the wait between them is counted (ms: forty minutes). */
	public static final long SITTING_GAP = 40 * 60_000L;
	private static final String VERSION = "1";

	/** How a game ended. */
	public enum End
	{
		/** Played to the end: the game-over scroll was seen. */
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

	/** One game. */
	public static final class Game
	{
		public final long endedAt;
		/** Ticks in the game. */
		public final int ticks;
		/** Your team, or null when not seen. */
		public final Brewing.Team team;
		/** Rum your team made, and the other team, or -1 when not read. */
		public final int rum;
		public final int rival;
		/** Pieces of eight it paid. */
		public final int pieces;
		/** Cooking experience gained, or 0. */
		public final int xp;
		public final End end;

		public Game(long endedAt, int ticks, Brewing.Team team, int rum, int rival, int pieces, int xp, End end)
		{
			this.endedAt = Math.max(0, endedAt);
			this.ticks = ticks > 0 && ticks <= LONGEST ? ticks : 0;
			this.team = team;
			this.rum = rum < 0 ? -1 : Math.min(RUM_MAX, rum);
			this.rival = rival < 0 ? -1 : Math.min(RUM_MAX, rival);
			this.pieces = Math.max(0, Math.min(PIECES_MAX, pieces));
			this.xp = Math.max(0, Math.min(XP_MAX, xp));
			this.end = end == null ? End.LEFT : end;
		}

		/** "Won", "Lost", "Drawn", or "" when the scores were not read. */
		public String result()
		{
			if (rum < 0 || rival < 0)
			{
				return "";
			}
			return rum > rival ? "Won" : rum < rival ? "Lost" : "Drawn";
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

	/** The game that paid most, or null. */
	public Game best()
	{
		Game best = null;
		for (Game g : games)
		{
			if (g.pieces > 0 && (best == null || g.pieces > best.pieces))
			{
				best = g;
			}
		}
		return best;
	}

	/** Pieces of eight an hour in the game over your timed games, or -1 with none. */
	public double piecesPerHour()
	{
		return perHour(true);
	}

	/** Cooking experience an hour in the game over your timed games, or -1 with none. */
	public double xpPerHour()
	{
		return perHour(false);
	}

	private double perHour(boolean pieces)
	{
		long t = 0;
		long n = 0;
		for (Game g : games)
		{
			if (g.ticks > 0)
			{
				t += g.ticks;
				n += pieces ? g.pieces : g.xp;
			}
		}
		return t == 0 ? -1 : n * 3600.0 / (t * 0.6);
	}

	/**
	 * Pieces of eight an hour with the wait between games counted: over each game that followed
	 * another within a sitting, what it paid over the time since the last one ended. -1 with
	 * fewer than two such games.
	 */
	public double piecesPerHourSitting()
	{
		return sitting(true);
	}

	/** Cooking experience an hour with the waits counted, as {@link #piecesPerHourSitting}. */
	public double xpPerHourSitting()
	{
		return sitting(false);
	}

	private double sitting(boolean pieces)
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
				n += pieces ? games.get(i).pieces : games.get(i).xp;
				pairs++;
			}
		}
		return pairs < 2 || ms <= 0 ? -1 : n * 3_600_000.0 / ms;
	}

	/** Average pieces of eight a game that paid, or 0. */
	public int averagePieces()
	{
		long p = 0;
		int n = 0;
		for (Game g : games)
		{
			if (g.pieces > 0)
			{
				p += g.pieces;
				n++;
			}
		}
		return n == 0 ? 0 : (int) (p / n);
	}

	/** Average rum your team made in games played to the end, or -1. */
	public double averageRum()
	{
		long r = 0;
		int n = 0;
		for (Game g : games)
		{
			if (g.end == End.FINISHED && g.rum >= 0)
			{
				r += g.rum;
				n++;
			}
		}
		return n == 0 ? -1 : r / (double) n;
	}

	/** Games won, of those whose scores were read. */
	public int won()
	{
		int n = 0;
		for (Game g : games)
		{
			n += "Won".equals(g.result()) ? 1 : 0;
		}
		return n;
	}

	/** Games whose scores were read. */
	public int scored()
	{
		int n = 0;
		for (Game g : games)
		{
			n += g.result().isEmpty() ? 0 : 1;
		}
		return n;
	}

	/** "1;endedAt,ticks,team,rum,rival,pieces,xp,end;..." */
	public String encode()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (Game g : games)
		{
			b.append(';').append(g.endedAt).append(',').append(g.ticks).append(',').append(g.team == null ? '-' : g.team.code)
				.append(',').append(g.rum).append(',').append(g.rival).append(',').append(g.pieces).append(',').append(g.xp)
				.append(',').append(g.end.code);
		}
		return b.toString();
	}

	/** Reads a save as far as it can; damaged games are left out. Never null. */
	public static BrewingLog decode(String text)
	{
		BrewingLog out = new BrewingLog();
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
			if (f.length < 8)
			{
				continue;
			}
			try
			{
				long ended = Long.parseLong(f[0].trim());
				int ticks = Integer.parseInt(f[1].trim());
				String t = f[2].trim();
				Brewing.Team team = t.length() == 1 ? Brewing.Team.byCode(t.charAt(0)) : null;
				int rum = Integer.parseInt(f[3].trim());
				int rival = Integer.parseInt(f[4].trim());
				int pieces = Integer.parseInt(f[5].trim());
				int xp = Integer.parseInt(f[6].trim());
				End end = End.byCode(f[7].trim());
				if (ended < 0 || ticks < 0 || pieces < 0 || xp < 0 || end == null || (team == null && !"-".equals(t)))
				{
					continue;
				}
				out.add(new Game(ended, ticks, team, rum, rival, pieces, xp, end));
			}
			catch (IllegalArgumentException e)
			{
				// Left out: a bad number.
			}
		}
		return out;
	}
}
