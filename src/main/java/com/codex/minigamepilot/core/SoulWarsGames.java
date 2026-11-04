package com.codex.minigamepilot.core;

/**
 * Follows Soul Wars games a tick at a time. A game runs while the game's own overlay is up (or you
 * stand in the arena); it ends when the overlay goes, the end-of-game scroll shows, or you log
 * out. What it earned is counted until a while after: the zeal from the game's own count of your
 * tokens ({@code soul_wars_zeal_tokens}), else from the scroll, and the result from the scroll's
 * two scores for your team, else from the game's count of games won.
 *
 * <p>Pure: the pilot feeds it what it reads, and tests feed it the same.
 */
public final class SoulWarsGames
{
	/** A visit shorter than this is a blink (a load), not a game. */
	static final int SHORTEST = 5;
	/** Rewards are still counted this many ticks after the game ends, the scroll closed. */
	static final int GRACE = 15;
	/** Never wait longer than this on an open scroll. */
	static final int LONGEST_WAIT = 200;

	/** What is read in a tick. */
	public static final class Reading
	{
		/** The game's overlay is up, or you are in the arena. */
		public boolean inGame;
		/** The end-of-game scroll is up. */
		public boolean over;
		/** The team whose cape you wear (else by the team varbit), or null. */
		public SoulWars.Team team;
		/** The scroll's scores and zeal, or -1. */
		public int overBlue = -1;
		public int overRed = -1;
		public int overZeal = -1;
		/** Your part as the scroll counts it. */
		public SoulWarsLog.Part part = new SoulWarsLog.Part();
		/** Zeal Tokens held, or -1 when not read. */
		public long tokens = -1;
		/** The game's counts of games won and played, or -1 when not read. */
		public long wins = -1;
		public long played = -1;
	}

	private int start = -1;
	private long tokensStart = -1;
	private long winsStart = -1;
	private long playedStart = -1;
	private SoulWars.Team team;
	private int blue = -1;
	private int red = -1;
	private int overZeal = -1;
	private SoulWarsLog.Part part = new SoulWarsLog.Part();
	private boolean finished;
	private int games;
	/** A game settled with its overlay still up: the next begins only once it has gone. */
	private boolean needOut;

	// A game ended, its rewards still being counted.
	private int endTick = -1;
	private int lastOverTick = -1;
	private int pendingTicks;
	private SoulWarsLog.End pendingEnd;

	/**
	 * One tick.
	 *
	 * @return the game settled this tick, or null
	 */
	public SoulWarsLog.Game tick(int tick, Reading r, long now)
	{
		if (start >= 0 && endTick >= 0)
		{
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
				SoulWarsLog.Game g = settle(r, now);
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
			end(tick, SoulWarsLog.End.FINISHED);
		}
		else if (!r.inGame)
		{
			end(tick, SoulWarsLog.End.LEFT);
		}
		return null;
	}

	private void begin(int tick, Reading r)
	{
		start = tick;
		tokensStart = r.tokens;
		winsStart = r.wins;
		playedStart = r.played;
		team = r.team;
		blue = -1;
		red = -1;
		overZeal = -1;
		part = new SoulWarsLog.Part();
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
		if (endTick < 0)
		{
			if (tokensStart < 0 && r.tokens >= 0)
			{
				tokensStart = r.tokens;
			}
			if (winsStart < 0 && r.wins >= 0)
			{
				winsStart = r.wins;
			}
			if (playedStart < 0 && r.played >= 0)
			{
				playedStart = r.played;
			}
		}
		if (r.over)
		{
			blue = r.overBlue >= 0 ? r.overBlue : blue;
			red = r.overRed >= 0 ? r.overRed : red;
			overZeal = r.overZeal >= 0 ? r.overZeal : overZeal;
			SoulWarsLog.Part p = r.part == null ? new SoulWarsLog.Part() : r.part;
			part.kills = p.kills >= 0 ? p.kills : part.kills;
			part.captures = p.captures >= 0 ? p.captures : part.captures;
			part.damage = p.damage >= 0 ? p.damage : part.damage;
			part.fragments = p.fragments >= 0 ? p.fragments : part.fragments;
			part.bones = p.bones >= 0 ? p.bones : part.bones;
		}
	}

	private void end(int tick, SoulWarsLog.End how)
	{
		endTick = tick;
		pendingTicks = tick - start;
		pendingEnd = how;
	}

	/** Zeal earned so far: the rise in tokens held, else the scroll's figure, else 0. */
	static int zeal(long from, long now, int scroll)
	{
		long gained = from >= 0 && now >= 0 ? now - from : -1;
		if (gained > 0 && gained <= SoulWarsLog.ZEAL_MAX)
		{
			return (int) gained;
		}
		if (scroll >= 0)
		{
			return Math.min(SoulWarsLog.ZEAL_MAX, scroll);
		}
		// A rise larger than a game can give is a purchase undone or a late first read, not this game.
		return 0;
	}

	/**
	 * The result: the scroll's two scores for your team; else the game's count of games won rising
	 * (won); else, a game played with more zeal than a loss can give (the wiki's 40), drawn.
	 */
	static SoulWarsLog.Result result(SoulWars.Team team, int blue, int red, long winsGained, long playedGained, int zeal)
	{
		if (team != null && blue >= 0 && red >= 0)
		{
			int mine = team == SoulWars.Team.BLUE ? blue : red;
			int theirs = team == SoulWars.Team.BLUE ? red : blue;
			return mine > theirs ? SoulWarsLog.Result.WON : mine < theirs ? SoulWarsLog.Result.LOST : SoulWarsLog.Result.DRAWN;
		}
		if (winsGained > 0)
		{
			return SoulWarsLog.Result.WON;
		}
		if (winsGained == 0 && playedGained > 0 && zeal > SoulWars.LOSS_MAX)
		{
			return SoulWarsLog.Result.DRAWN;
		}
		return SoulWarsLog.Result.UNKNOWN;
	}

	private SoulWarsLog.Game settle(Reading r, long now)
	{
		SoulWarsLog.Game g = null;
		int zeal = zeal(tokensStart, r.tokens, overZeal);
		long winsGained = winsStart >= 0 && r.wins >= 0 ? r.wins - winsStart : -1;
		long playedGained = playedStart >= 0 && r.played >= 0 ? r.played - playedStart : -1;
		boolean done = finished || winsGained > 0 || playedGained > 0 || zeal > 0;
		SoulWarsLog.End how = pendingEnd == SoulWarsLog.End.LOGOUT && !finished ? SoulWarsLog.End.LOGOUT
			: done ? SoulWarsLog.End.FINISHED : pendingEnd;
		if (pendingTicks >= SHORTEST || zeal > 0)
		{
			SoulWarsLog.Result result = how == SoulWarsLog.End.FINISHED
				? result(team, blue, red, winsGained, playedGained, zeal) : SoulWarsLog.Result.UNKNOWN;
			g = new SoulWarsLog.Game(now, pendingTicks, team, result, zeal, blue, red, part, how);
			games++;
		}
		start = -1;
		endTick = -1;
		return g;
	}

	/** Logged out or hopped: a game under way, or one still being settled, ends now. */
	public SoulWarsLog.Game logout(int tick, Reading r, long now)
	{
		if (start < 0)
		{
			return null;
		}
		if (endTick < 0)
		{
			end(tick, SoulWarsLog.End.LOGOUT);
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

	public SoulWars.Team team()
	{
		return start >= 0 ? team : null;
	}

	/** Zeal this game so far, by the tokens' rise. */
	public int zealSoFar(long tokens)
	{
		return start < 0 ? 0 : zeal(tokensStart, tokens, -1);
	}

	/** Games ended since this tracker began. */
	public int games()
	{
		return games;
	}
}
