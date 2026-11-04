package com.codex.minigamepilot.core;

/**
 * Follows Volcanic Mine games a tick at a time: a game runs while you are in the mine, and ends
 * when you come out (by the exit, or dead: "After 10 minutes have elapsed, the volcano will erupt
 * and anyone still in the volcano will die") or log out. Its points are the game's own count of
 * points this game at its highest; its experience is the Mining experience gained from entering
 * until a few ticks after coming out, so experience the game gives as you leave is counted too.
 *
 * <p>Pure: the pilot feeds it what it reads, and tests feed it the same.
 */
public final class VolcanicGames
{
	/** A visit shorter than this is a blink (a load), not a game. */
	static final int SHORTEST = 5;
	/** Experience is still counted this many ticks after coming out. */
	static final int GRACE = 3;
	/** At or under this many seconds left a death is the eruption at the end. */
	static final int ERUPTION_SECONDS = 2;

	private int start = -1;
	private int base;
	private int peak;
	private int xpStart = -1;
	private int players;
	private int lastSeconds = -1;
	private int lastStability = -1;
	private boolean died;
	private boolean played;
	private int games;

	// A game come out of, its experience still being counted.
	private int pendingTick = -1;
	private int pendingTicks;
	private int pendingPoints;
	private int pendingPlayers;
	private int pendingXpStart;
	private VolcanicLog.End pendingEnd;

	/**
	 * One tick.
	 *
	 * @param points the game's points this game, or -1 when not read
	 * @param xp Mining experience now
	 * @param players players in the game, or -1
	 * @param stability mine stability in percent, or -1
	 * @param seconds the time left as the game's overlay shows it, or -1
	 * @param progress the game's progress (1 waiting, 2 playing), or -1
	 * @return the game whose experience was settled this tick, or null
	 */
	public VolcanicLog.Game tick(int tick, boolean inMine, int points, int xp, int players, int stability, int seconds, int progress, long now)
	{
		VolcanicLog.Game done = null;
		if (pendingTick >= 0 && (inMine || tick - pendingTick >= GRACE))
		{
			done = settle(xp, now);
		}
		if (!inMine)
		{
			if (start >= 0)
			{
				leave(tick, now, died ? (eruption() ? VolcanicLog.End.ERUPTED : VolcanicLog.End.DIED) : VolcanicLog.End.LEFT);
			}
			return done;
		}
		if (start < 0)
		{
			start = tick;
			// Going in, the count may still show the last game's points; under way already (the
			// plugin turned on mid-game), it is this game's.
			base = progress == Volcanic.PROGRESS_PLAYING ? 0 : Math.max(0, points);
			peak = base;
			xpStart = xp;
			this.players = 0;
			lastSeconds = -1;
			lastStability = -1;
			died = false;
			played = false;
		}
		if (points >= 0)
		{
			if (points < peak)
			{
				// The count began again: what came before was the last game's.
				base = 0;
			}
			peak = points < peak ? points : Math.max(peak, points);
		}
		if (players > 0)
		{
			this.players = Math.max(this.players, players);
		}
		if (stability >= 0)
		{
			lastStability = stability;
		}
		if (seconds >= 0)
		{
			lastSeconds = seconds;
		}
		if (progress == Volcanic.PROGRESS_PLAYING)
		{
			played = true;
		}
		return done;
	}

	/** A game message: a death in the mine is kept with the game. */
	public void chat(String message)
	{
		if (start >= 0 && Volcanic.DEATH.equals(message))
		{
			died = true;
		}
	}

	private boolean eruption()
	{
		return (lastSeconds >= 0 && lastSeconds <= ERUPTION_SECONDS) || lastStability == 0;
	}

	private void leave(int tick, long now, VolcanicLog.End how)
	{
		int ticks = tick - start;
		if (ticks >= SHORTEST && (played || gained() > 0))
		{
			pendingTick = tick;
			pendingTicks = ticks;
			pendingPoints = gained();
			pendingPlayers = players;
			pendingXpStart = xpStart;
			pendingEnd = how;
		}
		start = -1;
	}

	private VolcanicLog.Game settle(int xp, long now)
	{
		VolcanicLog.Game g = new VolcanicLog.Game(now, pendingTicks, pendingPoints, pendingXpStart < 0 ? 0 : xp - pendingXpStart, pendingPlayers, pendingEnd);
		pendingTick = -1;
		games++;
		return g;
	}

	/** Logged out or hopped: a game under way, or one still being settled, ends now. */
	public VolcanicLog.Game logout(int tick, int xp, long now)
	{
		if (start >= 0)
		{
			leave(tick, now, VolcanicLog.End.LOGOUT);
		}
		return pendingTick >= 0 ? settle(xp, now) : null;
	}

	/** Points this game so far. */
	public int gained()
	{
		return start < 0 ? 0 : Math.max(0, peak - base);
	}

	/** Mining experience this game so far. */
	public int xpGained(int xp)
	{
		return start < 0 || xpStart < 0 ? 0 : Math.max(0, xp - xpStart);
	}

	public boolean open()
	{
		return start >= 0;
	}

	/** A game come out of whose experience is still being counted. */
	public boolean settling()
	{
		return pendingTick >= 0;
	}

	public int ticks(int tick)
	{
		return start >= 0 ? Math.max(0, tick - start) : 0;
	}

	/** Most players seen this game, or 0. */
	public int players()
	{
		return start >= 0 ? players : 0;
	}

	/** Whether the game has begun (past the 30 seconds to prepare), as the game's progress says. */
	public boolean playing()
	{
		return start >= 0 && played;
	}

	/** Games ended since this tracker began. */
	public int games()
	{
		return games;
	}
}
