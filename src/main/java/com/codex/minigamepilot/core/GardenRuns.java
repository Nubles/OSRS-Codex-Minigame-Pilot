package com.codex.minigamepilot.core;

/**
 * Follows Sorceress's Garden runs a tick at a time, from what the game shows: a run ends when you
 * are sent back to the fountain in the inner garden. Picking a sq'irk does that ("Doing so gives 60
 * Farming experience and teleports the player to the fountain in the inner garden"), picking herbs
 * does, and so does an elemental that sees you ("they teleport the player outside to the central
 * garden"). So a jump of more than a run's two tiles that lands by the fountain ends a run, and
 * what came with it says how: a sq'irk in the pack (its season), Farming experience for herbs, or
 * nothing, caught. The game has no chat message for any of it on the wiki, so none is read.
 *
 * <p>It also counts the juice you crush, wherever you are: a season's sq'irkjuice going up in the
 * same tick as its sq'irks go down.
 *
 * <p>It never times anything for you: the lap is measured after it is run, from one trip to the
 * fountain to the next. Pure: the pilot feeds it what it reads, and tests feed it the same.
 */
public final class GardenRuns
{
	/** A move of more than this many tiles in a tick is a teleport (running covers two). */
	static final int JUMP = 3;
	/** What a run paid is still counted this many ticks after the trip to the fountain. */
	static final int GRACE = 2;
	/** An elemental seen this many ticks before the trip to the fountain is the one that caught you. */
	static final int CATCH_MEMORY = 3;

	/** What is read in a tick. */
	public static final class Reading
	{
		/** A sq'irk tree is in the scene: you are in the Sorceress's Garden. */
		public boolean inGarden;
		/** Where you stand, or plane -1 when not known. */
		public int x;
		public int y;
		public int plane = -1;
		/** You stand by the fountain in the inner garden. */
		public boolean byFountain;
		/** The season of the nearest elemental within reach of catching you, or null. */
		public Garden.Season elemental;
		/** The season of the nearest sq'irk tree, or null. */
		public Garden.Season tree;
		/** Sq'irks and juice carried, by season. */
		public final int[] sqirks = new int[Garden.Season.values().length];
		public final int[] juice = new int[Garden.Season.values().length];
		/** Farming experience now. */
		public int farming;
	}

	/** What a tick settled: a run and juice crushed (by season), either or both empty. */
	public static final class Result
	{
		public GardenLog.Run run;
		public final int[] juice = new int[Garden.Season.values().length];

		public boolean isEmpty()
		{
			if (run != null)
			{
				return false;
			}
			for (int j : juice)
			{
				if (j > 0)
				{
					return false;
				}
			}
			return true;
		}
	}

	private Reading prev;
	/** The previous tick's pack, in or out of the garden, for the juice. */
	private int[] prevSqirks;
	private int[] prevJuice;
	/** When the lap now being run began: arriving, or the last trip to the fountain. */
	private int lapStart = -1;
	/** Since lapStart: the season of a sq'irk gained, and Farming experience gained. */
	private Garden.Season picked;
	private int farmingGain;
	/** The last elemental near enough to catch you, and when. */
	private Garden.Season elemental;
	private int elementalTick = -1;
	/** A trip to the fountain waiting for its rewards. */
	private int pendingAt = -1;
	private Garden.Season pendingElemental;
	private Garden.Season pendingTree;
	/** When the last trip to the fountain was made. */
	private int lastEnd = -1;
	private int runs;
	private int paid;

	/**
	 * One tick.
	 *
	 * @return what was settled this tick; never null
	 */
	public Result tick(int tick, Reading r, long now)
	{
		Result out = new Result();
		juice(r, out);
		if (!r.inGarden)
		{
			// Left the garden (or a load): a trip already made is still kept.
			if (pendingAt >= 0)
			{
				out.run = settle(now);
			}
			prev = null;
			lapStart = -1;
			resetLap();
			return out;
		}
		if (prev == null)
		{
			// Arrived: the first lap is timed from here.
			prev = r;
			lapStart = tick;
			resetLap();
			return out;
		}
		for (Garden.Season s : Garden.Season.values())
		{
			if (r.sqirks[s.ordinal()] > prev.sqirks[s.ordinal()])
			{
				picked = s;
			}
		}
		if (r.farming > prev.farming && prev.farming > 0)
		{
			farmingGain += r.farming - prev.farming;
		}
		if (r.elemental != null)
		{
			elemental = r.elemental;
			elementalTick = tick;
		}
		boolean jumped = prev.plane >= 0 && prev.plane == r.plane && r.byFountain
			&& Math.max(Math.abs(r.x - prev.x), Math.abs(r.y - prev.y)) > JUMP;
		if (jumped && pendingAt < 0)
		{
			pendingAt = tick;
			pendingElemental = elemental != null && tick - elementalTick <= CATCH_MEMORY ? elemental : null;
			pendingTree = prev.tree;
		}
		prev = r;
		if (pendingAt >= 0 && tick - pendingAt >= GRACE)
		{
			out.run = settle(now);
			lapStart = lastEnd;
		}
		return out;
	}

	private void juice(Reading r, Result out)
	{
		if (prevSqirks != null)
		{
			for (Garden.Season s : Garden.Season.values())
			{
				int i = s.ordinal();
				int made = r.juice[i] - prevJuice[i];
				if (made > 0 && r.sqirks[i] < prevSqirks[i])
				{
					out.juice[i] = made;
				}
			}
		}
		prevSqirks = r.sqirks.clone();
		prevJuice = r.juice.clone();
	}

	private void resetLap()
	{
		picked = null;
		farmingGain = 0;
		elemental = null;
		elementalTick = -1;
		pendingAt = -1;
	}

	/** The run the last trip to the fountain ended, or null when it was no run (a trip from nowhere). */
	private GardenLog.Run settle(long now)
	{
		int at = pendingAt;
		GardenLog.Outcome how;
		Garden.Season season;
		if (picked != null)
		{
			how = GardenLog.Outcome.SQIRK;
			season = picked;
		}
		else if (farmingGain > 0)
		{
			// No sq'irk in the pack: herbs, unless the experience is a sq'irk's other than autumn's (whose 50 is the herbs').
			Garden.Season bySqirk = null;
			for (Garden.Season s : Garden.Season.values())
			{
				if (s.farmingXp() == farmingGain && farmingGain != Garden.HERB_FARMING_XP)
				{
					bySqirk = s;
				}
			}
			how = bySqirk != null ? GardenLog.Outcome.SQIRK : GardenLog.Outcome.HERBS;
			season = bySqirk != null ? bySqirk : pendingElemental != null ? pendingElemental : pendingTree;
		}
		else if (pendingElemental != null)
		{
			how = GardenLog.Outcome.CAUGHT;
			season = pendingElemental;
		}
		else
		{
			how = null;
			season = null;
		}
		int ticks = lapStart >= 0 ? at - lapStart : 0;
		lastEnd = at;
		picked = null;
		farmingGain = 0;
		elemental = null;
		elementalTick = -1;
		pendingAt = -1;
		if (how == null || season == null)
		{
			return null;
		}
		runs++;
		paid += how.paid() ? 1 : 0;
		return new GardenLog.Run(now, ticks, season, how);
	}

	/** Logged out or hopped: a trip already made is kept, and the lap now being run is dropped. */
	public GardenLog.Run logout(long now)
	{
		GardenLog.Run r = pendingAt >= 0 ? settle(now) : null;
		prev = null;
		lapStart = -1;
		resetLap();
		return r;
	}

	/** Ticks into the lap now being run, or -1 out of the garden. Shown after the fact only, never as a cue. */
	public int lap(int tick)
	{
		return prev == null || lapStart < 0 ? -1 : Math.max(0, tick - lapStart);
	}

	/** Runs settled since this tracker began. */
	public int runs()
	{
		return runs;
	}

	/** Of them, those that paid. */
	public int paid()
	{
		return paid;
	}
}
