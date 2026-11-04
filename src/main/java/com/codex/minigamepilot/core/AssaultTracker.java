package com.codex.minigamepilot.core;

import com.codex.minigamepilot.core.Assault.Role;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Follows a Barbarian Assault game from what the game shows, a tick at a time: the "---- Wave: N
 * ----" line starting each wave, the role's horn interface while you are in one, the
 * wave-complete interface, and the four roles' honour points rising. A game runs from its first
 * wave to the queen, to a wave that does not follow on, or to a long time with no wave; then it
 * is handed back whole for the log.
 *
 * <p>Pure: the pilot feeds it what it reads, and tests feed it the same.
 */
public final class AssaultTracker
{
	/** The horn gone this many ticks running: you are out of the wave. */
	static final int GONE_TICKS = 2;
	/** After the last wave's end and the last points, the game is kept this many ticks on. */
	static final int SETTLE_TICKS = 3;
	/** No wave for this long and the game is over (ticks: ten minutes). */
	public static final int IDLE_TICKS = 1000;
	/** A rise in one role's points larger than this in one tick is not a wave's points (a fresh login's read). */
	static final int MAX_STEP = 300;

	private int[] seen;
	// The game under way.
	private boolean open;
	private int startTick;
	private int lastTick;
	private final List<AssaultLog.Wave> waves = new ArrayList<>();
	private final int[] points = new int[Role.values().length];
	// The wave under way.
	private int wave = -1;
	private int waveStart;
	private Role waveRole;
	private boolean hornSeen;
	private int goneSince = -1;
	private boolean waveDone;
	private int lastEnd = -1;
	private int lastPoints = -1;
	private final List<AssaultLog.Game> ready = new ArrayList<>();
	private long now;
	private int won;

	/** A wave starts: the line "---- Wave: N ----". */
	public void wave(int number, int tick)
	{
		if (number < 1)
		{
			return;
		}
		if (wave > 0)
		{
			closeWave(tick);
		}
		if (open && !follows(number))
		{
			finish();
		}
		if (!open)
		{
			open = true;
			startTick = tick;
			waves.clear();
			java.util.Arrays.fill(points, 0);
		}
		wave = number;
		waveStart = tick;
		waveRole = null;
		hornSeen = false;
		goneSince = -1;
		waveDone = false;
		lastTick = tick;
	}

	/** The same wave again after losing it, or the next one. */
	private boolean follows(int number)
	{
		if (waves.isEmpty())
		{
			return true;
		}
		AssaultLog.Wave last = waves.get(waves.size() - 1);
		return number == last.number && !last.done || number == last.number + 1;
	}

	/**
	 * One tick.
	 *
	 * @param role the role whose horn is showing, or null
	 * @param complete whether the wave-complete interface is showing
	 * @param read each role's points now, by {@link Role} order, or null when not readable
	 * @return games that ended, oldest first; empty most ticks
	 */
	public List<AssaultLog.Game> tick(int tick, Role role, boolean complete, int[] read, long now)
	{
		this.now = now;
		earn(read, tick);
		if (wave > 0)
		{
			if (role != null)
			{
				if (waveRole == null)
				{
					waveRole = role;
				}
				hornSeen = true;
				goneSince = -1;
			}
			else if (hornSeen)
			{
				if (goneSince < 0)
				{
					goneSince = tick;
				}
				if (tick - goneSince + 1 >= GONE_TICKS)
				{
					closeWave(goneSince);
				}
			}
			if (wave > 0 && complete)
			{
				waveDone = true;
				closeWave(tick);
			}
			if (wave > 0 && tick - waveStart > AssaultLog.LONGEST_WAVE)
			{
				closeWave(-1);
			}
		}
		if (open && wave < 0 && complete && !waves.isEmpty() && !waves.get(waves.size() - 1).done)
		{
			// The wave-complete interface after the horn: the wave just ended was won.
			markWon();
		}
		if (open && wave < 0)
		{
			AssaultLog.Wave last = waves.isEmpty() ? null : waves.get(waves.size() - 1);
			int quiet = Math.max(lastEnd, lastPoints);
			if (last != null && last.number == Assault.WAVES && last.done && tick - quiet >= SETTLE_TICKS)
			{
				finish();
			}
			else if (tick - lastTick > IDLE_TICKS)
			{
				finish();
			}
		}
		List<AssaultLog.Game> out = ready.isEmpty() ? Collections.emptyList() : new ArrayList<>(ready);
		ready.clear();
		return out;
	}

	/** Points rising in a role while a game is open are that game's; a fall (a purchase) only moves the mark. */
	private void earn(int[] read, int tick)
	{
		if (read == null)
		{
			return;
		}
		if (seen == null)
		{
			seen = read.clone();
			return;
		}
		boolean rose = false;
		for (int i = 0; i < points.length && i < read.length; i++)
		{
			int d = read[i] - seen[i];
			if (d > 0 && d <= MAX_STEP && open)
			{
				points[i] += d;
				rose = true;
			}
		}
		seen = read.clone();
		if (rose)
		{
			lastPoints = tick;
			lastTick = tick;
			// Points come as a wave is won: the wave under way, else the last one ended.
			if (wave > 0)
			{
				waveDone = true;
			}
			else if (!waves.isEmpty() && !waves.get(waves.size() - 1).done)
			{
				markWon();
			}
		}
	}

	/** The last wave ended was won after all. */
	private void markWon()
	{
		AssaultLog.Wave w = waves.remove(waves.size() - 1);
		waves.add(new AssaultLog.Wave(w.number, w.role, w.ticks, true));
		won++;
	}

	/** Waves won since this tracker began. */
	public int won()
	{
		return won;
	}

	private void closeWave(int end)
	{
		won += waveDone ? 1 : 0;
		int ticks = end < 0 ? 0 : end - waveStart;
		waves.add(new AssaultLog.Wave(wave, waveRole, ticks, waveDone));
		lastEnd = end < 0 ? lastTick : end;
		lastTick = Math.max(lastTick, lastEnd);
		wave = -1;
		waveRole = null;
		hornSeen = false;
		goneSince = -1;
		waveDone = false;
	}

	private void finish()
	{
		if (!open)
		{
			return;
		}
		if (!waves.isEmpty())
		{
			int span = lastEnd > startTick ? lastEnd - startTick : 0;
			ready.add(new AssaultLog.Game(now, span, waves, points));
		}
		open = false;
		waves.clear();
		java.util.Arrays.fill(points, 0);
		lastEnd = -1;
		lastPoints = -1;
	}

	/**
	 * Logged out or hopped: the wave under way is over, and so is the game; the points are read
	 * afresh after.
	 *
	 * @return the game that ended, or null
	 */
	public AssaultLog.Game logout(int tick, long now)
	{
		this.now = now;
		if (wave > 0)
		{
			// Not timed: the logout's moment is not the wave's end.
			closeWave(-1);
		}
		finish();
		seen = null;
		AssaultLog.Game g = ready.isEmpty() ? null : ready.get(ready.size() - 1);
		ready.clear();
		return g;
	}

	/** A game is under way. */
	public boolean open()
	{
		return open;
	}

	/** The wave under way, or -1 between waves. */
	public int wave()
	{
		return wave;
	}

	/** The role of the wave under way, or null. */
	public Role role()
	{
		return waveRole;
	}

	/** Ticks into the wave under way. */
	public int waveTicks(int tick)
	{
		return wave > 0 ? Math.max(0, tick - waveStart) : 0;
	}

	/** Ticks since the game's first wave began. */
	public int gameTicks(int tick)
	{
		return open ? Math.max(0, tick - startTick) : 0;
	}

	/** The waves of the game under way, ended ones. */
	public List<AssaultLog.Wave> waves()
	{
		return Collections.unmodifiableList(waves);
	}

	/** Points this game has given a role so far. */
	public int points(Role r)
	{
		return points[r.ordinal()];
	}

	/** Ticks since anything happened in the game, for the pilot to stay with it between waves. */
	public int quiet(int tick)
	{
		return open ? tick - lastTick : Integer.MAX_VALUE;
	}
}
