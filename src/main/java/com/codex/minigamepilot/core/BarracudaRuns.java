package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Follows one Barracuda Trial run at a time, tick by tick, from what the game shows: whether a
 * trial is under way, which trial and rank, the time on the trial's own clock, the objectives done
 * and where the skiff is. Each rise in the objectives is a split, kept with where it happened.
 * A run is handed back once over, finished (the trial's completion was seen), abandoned (reset,
 * or sailed away from) or cut short by a logout, with the Sailing experience it gave.
 */
public final class BarracudaRuns
{
	/** Ticks to wait once out of a trial before the run is kept, for its experience to land. */
	public static final int SETTLE = 3;
	/** The trial's clock going back by more than this (ticks) is a reset, a new run. */
	static final int RESET_DROP = 5;

	private boolean open;
	private Barracuda.Trial trial;
	private Barracuda.Rank rank;
	private int start;
	private int xpStart = -1;
	private int lastXp = -1;
	private int lastDone = -1;
	private int lastClock = -1;
	private int ofObjectives = -1;
	private boolean finished;
	private int finishTicks = -1;
	private int outSince = -1;
	private int lastTick;
	private final List<BarracudaLog.Split> splits = new ArrayList<>();
	private int runs;

	/** One tick's reading. */
	public static final class Reading
	{
		public boolean inTrial;
		/** The trial, or null when not read. */
		public Barracuda.Trial trial;
		/** The rank, or null when not read. */
		public Barracuda.Rank rank;
		/** The trial's own clock in ticks, or -1. */
		public int clock = -1;
		/** Objectives done, or -1. */
		public int done = -1;
		/** Objectives in all, or -1. */
		public int of = -1;
		public int xp;
		/** Where the skiff is, or 0, 0. */
		public int x;
		public int y;
		/** The trial's completion seen this tick. */
		public boolean completed;
	}

	/**
	 * @return the run that ended this tick, or null
	 */
	public BarracudaLog.Run tick(int tick, Reading r, long now)
	{
		lastTick = tick;
		BarracudaLog.Run out = null;
		if (open && r.inTrial)
		{
			outSince = -1;
			boolean reset = !finished && (r.clock >= 0 && lastClock >= 0 && r.clock + RESET_DROP < lastClock
				|| r.done >= 0 && lastDone > 0 && r.done < lastDone);
			if (reset || finished && r.clock >= 0 && lastClock >= 0 && r.clock + RESET_DROP < lastClock)
			{
				out = close(tick, r.xp, finished ? BarracudaLog.End.FINISHED : BarracudaLog.End.ABANDONED, now);
			}
		}
		if (!open && r.inTrial && r.trial != null && !r.completed)
		{
			begin(tick, r);
		}
		if (!open)
		{
			return out;
		}
		if (r.inTrial)
		{
			follow(tick, r);
			return out;
		}
		// Out of the trial: a completion just before still counts, then wait for the experience.
		if (r.completed && !finished)
		{
			finish(tick);
		}
		if (outSince < 0)
		{
			outSince = tick;
		}
		if (r.xp > 0)
		{
			lastXp = r.xp;
		}
		if (tick - outSince >= SETTLE)
		{
			int endTick = outSince;
			BarracudaLog.Run run = build(endTick, lastXp, finished ? BarracudaLog.End.FINISHED : BarracudaLog.End.ABANDONED, now);
			open = false;
			return run;
		}
		return out;
	}

	private void begin(int tick, Reading r)
	{
		open = true;
		trial = r.trial;
		rank = r.rank;
		start = tick - Math.max(0, r.clock);
		xpStart = r.xp;
		lastXp = r.xp;
		lastDone = r.done;
		lastClock = r.clock;
		ofObjectives = r.of;
		finished = false;
		finishTicks = -1;
		outSince = -1;
		splits.clear();
	}

	private void follow(int tick, Reading r)
	{
		if (trial == null)
		{
			trial = r.trial;
		}
		if (rank == null)
		{
			rank = r.rank;
		}
		if (r.xp > 0)
		{
			lastXp = r.xp;
		}
		if (r.of > 0)
		{
			ofObjectives = r.of;
		}
		if (!finished && r.done >= 0)
		{
			if (lastDone >= 0 && r.done > lastDone)
			{
				int at = Math.max(0, tick - start);
				for (int i = lastDone; i < r.done; i++)
				{
					splits.add(new BarracudaLog.Split(at, r.x, r.y));
				}
			}
			lastDone = r.done;
		}
		if (!finished && r.clock >= 0)
		{
			lastClock = r.clock;
		}
		if (r.completed && !finished)
		{
			finish(tick);
		}
	}

	private void finish(int tick)
	{
		finished = true;
		// The trial's own clock is the run's time when it was read; else the ticks counted.
		finishTicks = lastClock > 0 ? lastClock : Math.max(0, tick - start);
	}

	/** Logged out or hopped: a run under way ends there. */
	public BarracudaLog.Run logout(int tick, int xp, long now)
	{
		if (!open)
		{
			return null;
		}
		return close(tick, xp, finished ? BarracudaLog.End.FINISHED : BarracudaLog.End.LOGOUT, now);
	}

	private BarracudaLog.Run close(int tick, int xp, BarracudaLog.End end, long now)
	{
		BarracudaLog.Run run = build(tick, xp > 0 ? xp : lastXp, end, now);
		open = false;
		return run;
	}

	private BarracudaLog.Run build(int endTick, int xp, BarracudaLog.End end, long now)
	{
		int ticks = end == BarracudaLog.End.FINISHED && finishTicks > 0 ? finishTicks : Math.max(0, endTick - start);
		int gained = xpStart >= 0 && xp >= xpStart ? xp - xpStart : 0;
		runs++;
		return new BarracudaLog.Run(now, trial, rank, ticks, gained, end, new ArrayList<>(splits));
	}

	public boolean open()
	{
		return open;
	}

	/** In a run and not yet out of it. */
	public boolean running()
	{
		return open && outSince < 0;
	}

	public boolean finished()
	{
		return open && finished;
	}

	public Barracuda.Trial trial()
	{
		return open ? trial : null;
	}

	public Barracuda.Rank rank()
	{
		return open ? rank : null;
	}

	/** Ticks into the run: the trial's clock where read, else counted. */
	public int ticks(int tick)
	{
		if (!open)
		{
			return 0;
		}
		if (finished && finishTicks > 0)
		{
			return finishTicks;
		}
		return Math.max(0, tick - start);
	}

	public List<BarracudaLog.Split> splits()
	{
		return Collections.unmodifiableList(splits);
	}

	/** Objectives in all, as the interface last showed them, or -1. */
	public int of()
	{
		return open ? ofObjectives : -1;
	}

	/** Sailing experience this run so far. */
	public int xpGained()
	{
		return open && xpStart >= 0 && lastXp >= xpStart ? lastXp - xpStart : 0;
	}

	/** Runs ended since this was made. */
	public int runs()
	{
		return runs;
	}
}
