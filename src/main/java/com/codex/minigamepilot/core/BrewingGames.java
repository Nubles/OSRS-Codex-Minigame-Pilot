package com.codex.minigamepilot.core;

/**
 * Follows Trouble Brewing games a tick at a time. A game runs while the game's own overlay is
 * up; it ends when the game-over scroll shows or you leave or log out. What it paid is counted
 * until a while after: the pieces of eight from the game's own count of them (the account's
 * {@code brew_pieces}), else from the scroll, and the Cooking experience gained from the start,
 * so "an experience reward of 100 Crafting experience, 250 Cooking experience, and 150
 * Firemaking experience" given at the end is in it.
 *
 * <p>Pure: the pilot feeds it what it reads, and tests feed it the same.
 */
public final class BrewingGames
{
	/** A visit shorter than this is a blink (a load), not a game. */
	static final int SHORTEST = 5;
	/** Rewards are still counted this many ticks after the game ends, the scroll closed. */
	static final int GRACE = 10;
	/** Never wait longer than this on an open scroll. */
	static final int LONGEST_WAIT = 200;

	/** What is read in a tick. */
	public static final class Reading
	{
		/** The game's overlay is up. */
		public boolean inGame;
		/** The game-over scroll is up. */
		public boolean over;
		/** The team whose hat you wear, or null. */
		public Brewing.Team team;
		/** The overlay's red and blue scores, or -1. */
		public int red = -1;
		public int blue = -1;
		/** The scroll's final red and blue scores and its pieces of eight, or -1. */
		public int overRed = -1;
		public int overBlue = -1;
		public int overPieces = -1;
		/** Pieces of eight held, or -1 when not read. */
		public long pieces = -1;
		/** Cooking experience now. */
		public int xp;
	}

	private int start = -1;
	private int xpStart = -1;
	private long piecesStart = -1;
	private Brewing.Team team;
	private int red = -1;
	private int blue = -1;
	private int overPieces = -1;
	private boolean finished;
	private int games;
	/** A game played to its end was settled with its overlay still up: the next begins only once it has gone. */
	private boolean needOut;

	// A game ended, its rewards still being counted.
	private int endTick = -1;
	private int lastOverTick = -1;
	private int pendingTicks;
	private BrewingLog.End pendingEnd;

	/**
	 * One tick.
	 *
	 * @return the game settled this tick, or null
	 */
	public BrewingLog.Game tick(int tick, Reading r, long now)
	{
		if (start >= 0 && endTick >= 0)
		{
			// Rewards still coming in.
			take(r);
			if (r.over)
			{
				lastOverTick = tick;
				finished = true;
			}
			boolean quiet = !r.over && tick - Math.max(endTick, lastOverTick) >= GRACE;
			if (r.inGame && !r.over && !finished || quiet || tick - endTick >= LONGEST_WAIT)
			{
				boolean wasFinished = finished;
				BrewingLog.Game g = settle(r, now);
				needOut = wasFinished && r.inGame;
				if (r.inGame && !needOut)
				{
					begin(tick, r);
				}
				return g;
			}
			return null;
		}
		if (start < 0)
		{
			needOut &= r.inGame;
			if (r.inGame && !needOut && !r.over)
			{
				begin(tick, r);
			}
			return null;
		}
		take(r);
		if (r.over)
		{
			finished = true;
			lastOverTick = tick;
			end(tick, BrewingLog.End.FINISHED);
		}
		else if (!r.inGame)
		{
			end(tick, BrewingLog.End.LEFT);
		}
		return null;
	}

	private void begin(int tick, Reading r)
	{
		start = tick;
		xpStart = r.xp;
		piecesStart = r.pieces;
		team = r.team;
		red = -1;
		blue = -1;
		overPieces = -1;
		finished = false;
		endTick = -1;
		lastOverTick = -1;
		take(r);
	}

	private void take(Reading r)
	{
		if (r.team != null && team == null)
		{
			team = r.team;
		}
		if (piecesStart < 0 && r.pieces >= 0 && endTick < 0)
		{
			piecesStart = r.pieces;
		}
		if (r.overRed >= 0)
		{
			red = r.overRed;
		}
		else if (r.red >= 0 && !finished)
		{
			red = r.red;
		}
		if (r.overBlue >= 0)
		{
			blue = r.overBlue;
		}
		else if (r.blue >= 0 && !finished)
		{
			blue = r.blue;
		}
		if (r.overPieces >= 0)
		{
			overPieces = r.overPieces;
		}
	}

	private void end(int tick, BrewingLog.End how)
	{
		endTick = tick;
		pendingTicks = tick - start;
		pendingEnd = how;
	}

	private BrewingLog.Game settle(Reading r, long now)
	{
		BrewingLog.Game g = null;
		long gained = piecesStart >= 0 && r.pieces >= 0 ? r.pieces - piecesStart : -1;
		int pieces = (int) Math.max(0, gained > 0 ? gained : Math.max(0, overPieces));
		int xp = xpStart < 0 ? 0 : Math.max(0, r.xp - xpStart);
		BrewingLog.End how = finished ? BrewingLog.End.FINISHED : pendingEnd;
		if (pendingTicks >= SHORTEST || pieces > 0)
		{
			int mine = team == null ? -1 : team == Brewing.Team.RED ? red : blue;
			int theirs = team == null ? -1 : team == Brewing.Team.RED ? blue : red;
			g = new BrewingLog.Game(now, pendingTicks, team, mine, theirs, pieces, xp, how);
			games++;
		}
		start = -1;
		endTick = -1;
		return g;
	}

	/** Logged out or hopped: a game under way, or one still being settled, ends now. */
	public BrewingLog.Game logout(int tick, Reading r, long now)
	{
		if (start < 0)
		{
			return null;
		}
		if (endTick < 0)
		{
			end(tick, BrewingLog.End.LOGOUT);
		}
		return settle(r, now);
	}

	/** In a game, not yet ended. */
	public boolean open()
	{
		return start >= 0 && endTick < 0;
	}

	/** A game ended whose rewards are still being counted. */
	public boolean settling()
	{
		return start >= 0 && endTick >= 0;
	}

	public int ticks(int tick)
	{
		return open() ? Math.max(0, tick - start) : 0;
	}

	public Brewing.Team team()
	{
		return start >= 0 ? team : null;
	}

	/** Your team's rum this game, or -1. */
	public int rum()
	{
		return team == null || start < 0 ? -1 : team == Brewing.Team.RED ? red : blue;
	}

	/** The other team's rum this game, or -1. */
	public int rival()
	{
		return team == null || start < 0 ? -1 : team == Brewing.Team.RED ? blue : red;
	}

	/** Cooking experience this game so far. */
	public int xpGained(int xp)
	{
		return start < 0 || xpStart < 0 ? 0 : Math.max(0, xp - xpStart);
	}

	/** Games ended since this tracker began. */
	public int games()
	{
		return games;
	}
}
