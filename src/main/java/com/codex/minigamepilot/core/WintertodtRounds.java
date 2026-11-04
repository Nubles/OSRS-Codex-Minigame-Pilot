package com.codex.minigamepilot.core;

/**
 * Follows Wintertodt rounds from the HUD, a tick at a time: a round runs while its title reads
 * the energy ("Wintertodt's Energy: 45%"), and ends when the energy is gone or the title turns to
 * the countdown ("The Wintertodt returns in: 0:59"). Leaving the prison mid-round, or logging
 * out, ends it with nothing ("the loss of all points for the current round"). Each round ended is
 * handed back whole for the log.
 *
 * <p>Pure: the pilot feeds it what it reads, and tests feed it the same.
 */
public final class WintertodtRounds
{
	private int start = -1;
	private boolean late;
	private int wait;
	private int points;
	private int hits;
	/** The countdown was first seen on this tick, or -1. */
	private int waitFrom = -1;
	/** The round just ended at 0 energy; the bar still reads 0 until the countdown. */
	private boolean spent;
	private int rounds;

	/**
	 * One tick.
	 *
	 * @param energy the energy in a round, or -1 when the HUD shows none
	 * @param returnsIn seconds on the countdown between rounds, or -1
	 * @param read your points from the HUD, or -1
	 * @return the round that ended this tick, or null
	 */
	public WintertodtLog.Game tick(int tick, boolean inPrison, int energy, int returnsIn, int read, long now)
	{
		if (!inPrison)
		{
			waitFrom = -1;
			spent = false;
			return start >= 0 ? end(tick, now, false) : null;
		}
		if (returnsIn >= 0)
		{
			spent = false;
			WintertodtLog.Game g = start >= 0 ? end(tick, now, true) : null;
			if (waitFrom < 0)
			{
				waitFrom = tick;
			}
			return g;
		}
		if (energy < 0)
		{
			return null;
		}
		if (spent)
		{
			if (energy == 0)
			{
				return null;
			}
			spent = false;
		}
		if (start < 0)
		{
			start = tick;
			late = waitFrom < 0;
			wait = waitFrom < 0 ? 0 : tick - waitFrom;
			points = 0;
			hits = 0;
			waitFrom = -1;
		}
		if (read >= 0)
		{
			points = read;
		}
		if (energy == 0)
		{
			spent = true;
			return end(tick, now, true);
		}
		return null;
	}

	private WintertodtLog.Game end(int tick, long now, boolean subdued)
	{
		WintertodtLog.Game g = new WintertodtLog.Game(now, late ? 0 : tick - start, wait, points, subdued, hits);
		start = -1;
		points = 0;
		hits = 0;
		waitFrom = subdued ? tick : -1;
		rounds++;
		return g;
	}

	/** A knock taken: counted only in a round. */
	public void hit()
	{
		if (start >= 0)
		{
			hits++;
		}
	}

	/** Logged out or hopped: a round under way is lost. */
	public WintertodtLog.Game logout(int tick, long now)
	{
		waitFrom = -1;
		spent = false;
		return start >= 0 ? end(tick, now, false) : null;
	}

	/** A round is under way. */
	public boolean open()
	{
		return start >= 0;
	}

	/** Ticks into the round under way. */
	public int ticks(int tick)
	{
		return start >= 0 ? Math.max(0, tick - start) : 0;
	}

	/** The round under way was joined after its start, so it is not timed. */
	public boolean late()
	{
		return start >= 0 && late;
	}

	/** Knocks this round. */
	public int hits()
	{
		return hits;
	}

	/** Rounds ended since this tracker began. */
	public int rounds()
	{
		return rounds;
	}
}
