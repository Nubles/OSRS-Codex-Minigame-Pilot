package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The pace coach. Fed one sample a game tick, it cuts the minigame into rounds (each time the
 * loop comes back to its first stage), and for each round counts where time went: ticks spent
 * standing still while there was something to do, and tiles walked beyond the shortest way.
 * Each round is compared with your best of the same kind, such as a full load of pay-dirt or an
 * ore trip of steel, and the biggest loss becomes one line of advice.
 */
public final class Pace
{
	/** Standing still for a tick after the step changes is just the time it takes to click. */
	static final int GRACE = 1;
	/** Walking a tile or two over the shortest way is not worth mentioning. */
	public static final int TOLERANCE = 2;
	/** A round this many times the usual length had a break in it, and is not a fair comparison. */
	static final int BREAK_FACTOR = 3;
	private static final int KEEP = 50;
	/** Stages a loop can have; the step's stage number indexes the splits. */
	public static final int STAGES = 31;
	/** Marks a saved best as one stage of a kind of round: "kind#stage=ticks". */
	private static final char STAGE_MARK = '#';

	/** One finished round. */
	public static final class Round
	{
		public final String kind;
		public final int ticks;
		public final int units;
		public final int idle;
		/** Where the most standing still happened, such as "the bank", or null. */
		public final String idlePlace;
		public final int idlePlaceTicks;
		public final int extraTiles;
		/** The walk that went furthest out of its way, or null. */
		public final String extraPlace;
		public final int extraPlaceTiles;
		/** Your best for this kind of round before this one, or 0 when this is the first. */
		public final int bestBefore;
		public final boolean hadBreak;
		/*
		 * Stage splits. Transient so the field notes' record of a round keeps exactly the shape it
		 * had before splits existed.
		 */
		/** Ticks spent in each stage of the loop this round, by stage. */
		private final transient int[] stageTicks;
		/** Your best for each stage in this kind of round before this one, 0 where there was none. */
		private final transient int[] stageBests;
		/*
		 * The tick coach's figures, transient for the same reason as the splits.
		 */
		/** Ticks spent moving on walks whose shortest way was known. */
		private transient int footTicks;
		/** Those walks at run pace along their shortest way: two tiles a tick (see {@link #RUN_TILES}). */
		private transient int floorTicks;
		/** The walk that lost the most ticks against its floor, or null, and the ticks it lost. */
		private transient String walkPlace;
		private transient int walkLoss;
		/** Experience gained over the round in the minigame's skill, or -1 when it was not known throughout. */
		private transient int xp = -1;

		Round(String kind, int ticks, int units, int idle, String idlePlace, int idlePlaceTicks,
			int extraTiles, String extraPlace, int extraPlaceTiles, int bestBefore, boolean hadBreak)
		{
			this(kind, ticks, units, idle, idlePlace, idlePlaceTicks, extraTiles, extraPlace, extraPlaceTiles, bestBefore, hadBreak,
				new int[STAGES], new int[STAGES]);
		}

		Round(String kind, int ticks, int units, int idle, String idlePlace, int idlePlaceTicks,
			int extraTiles, String extraPlace, int extraPlaceTiles, int bestBefore, boolean hadBreak, int[] stageTicks, int[] stageBests)
		{
			this.stageTicks = stageTicks.clone();
			this.stageBests = stageBests.clone();
			this.kind = kind;
			this.ticks = ticks;
			this.units = units;
			this.idle = idle;
			this.idlePlace = idlePlace;
			this.idlePlaceTicks = idlePlaceTicks;
			this.extraTiles = extraTiles;
			this.extraPlace = extraPlace;
			this.extraPlaceTiles = extraPlaceTiles;
			this.bestBefore = bestBefore;
			this.hadBreak = hadBreak;
		}

		/** Ticks spent in a stage of the loop this round. */
		public int stageTicks(int stage)
		{
			return stage >= 0 && stage < STAGES ? stageTicks[stage] : 0;
		}

		/** Your best for a stage in this kind of round before this one, or 0. */
		public int stageBest(int stage)
		{
			return stage >= 0 && stage < STAGES ? stageBests[stage] : 0;
		}

		/**
		 * The stage that lost the most time against your best for it, or -1 when every stage was
		 * within a tick or two of its best (or there were no bests to compare with).
		 */
		public int slowestStage()
		{
			if (hadBreak)
			{
				return -1;
			}
			int worst = -1;
			int by = TOLERANCE;
			for (int s = 0; s < STAGES; s++)
			{
				if (stageTicks[s] > 0 && stageBests[s] > 0 && stageTicks[s] - stageBests[s] > by)
				{
					by = stageTicks[s] - stageBests[s];
					worst = s;
				}
			}
			return worst;
		}

		/** Ticks spent moving on walks whose shortest way was known. */
		public int footTicks()
		{
			return footTicks;
		}

		/** The fewest ticks those walks could have taken: their shortest way at run pace. */
		public int floorTicks()
		{
			return floorTicks;
		}

		/** Experience the round gave, or -1 when it is not known. */
		public int xp()
		{
			return xp;
		}

		/**
		 * The single biggest leak this round, in ticks: standing still with something to do, or a
		 * walk slower than its shortest way at run pace (a detour, or walking instead of running),
		 * whichever lost more. Only when neither lost {@link #LEAK_MIN} ticks does a stage over its
		 * best stand in, since a slow stage is made of those two.
		 */
		public Leak leak()
		{
			if (hadBreak)
			{
				return Leak.NONE;
			}
			int idleTicks = idlePlace == null ? 0 : idlePlaceTicks;
			int walkTicks = walkPlace == null ? 0 : walkLoss;
			if (idleTicks >= LEAK_MIN && idleTicks >= walkTicks)
			{
				return new Leak(Leak.Kind.IDLE, idlePlace, idleTicks, -1);
			}
			if (walkTicks >= LEAK_MIN)
			{
				return new Leak(Leak.Kind.WALK, walkPlace, walkTicks, -1);
			}
			int slow = slowestStage();
			if (slow >= 0)
			{
				return new Leak(Leak.Kind.STAGE, null, stageTicks[slow] - stageBests[slow], slow);
			}
			return Leak.NONE;
		}

		public boolean isBest()
		{
			return !hadBreak && (bestBefore == 0 || ticks < bestBefore);
		}

		/** Whether the round lost time worth mentioning. */
		public boolean costly()
		{
			return !hadBreak && (idlePlaceTicks >= 3 && idlePlace != null || extraPlaceTiles > TOLERANCE && extraPlace != null);
		}

		/** The one thing that would have made this round quicker, in a sentence. */
		public String advice()
		{
			if (hadBreak)
			{
				return "Long round, so it is left out of your bests";
			}
			boolean idleMatters = idlePlaceTicks >= 3 && idlePlace != null;
			boolean walkMatters = extraPlaceTiles > TOLERANCE && extraPlace != null;
			// A tile walked costs about half a tick when running, so compare like with like.
			if (idleMatters && (!walkMatters || idlePlaceTicks * 2 >= extraPlaceTiles))
			{
				return "Stood still " + idlePlaceTicks + " ticks at " + idlePlace;
			}
			if (walkMatters)
			{
				return "Walked " + extraPlaceTiles + " tiles more than needed to " + extraPlace;
			}
			if (isBest() && bestBefore > 0)
			{
				return "Your best round of this kind";
			}
			return "A clean round: no standing still, no detours";
		}
	}

	/**
	 * Tiles a player covers in a tick running (oldschool.runescape.wiki/w/Energy: "move two tiles
	 * per game tick while running"); walking covers one.
	 */
	public static final int RUN_TILES = 2;
	/** A leak under this many ticks is noise: a click's delay, a tile around a player. */
	public static final int LEAK_MIN = 2;

	/** What lost a round the most time. */
	public static final class Leak
	{
		public enum Kind
		{
			NONE,
			IDLE,
			WALK,
			STAGE
		}

		static final Leak NONE = new Leak(Kind.NONE, null, 0, -1);

		public final Kind kind;
		/** Where, as "the sack", for IDLE and WALK; null otherwise. */
		public final String place;
		public final int ticks;
		/** The stage, for STAGE; -1 otherwise. */
		public final int stage;

		Leak(Kind kind, String place, int ticks, int stage)
		{
			this.kind = kind;
			this.place = place;
			this.ticks = ticks;
			this.stage = stage;
		}

		/**
		 * "Idle 7 ticks at the sack", "6 ticks lost on foot to the bank", "Bank 5 ticks over your
		 * best for it", or a clean round.
		 *
		 * @param stageNames the loop's stage names, for a STAGE leak, or null
		 */
		public String text(String[] stageNames)
		{
			switch (kind)
			{
				case IDLE:
					return "Idle " + ticks(ticks) + " at " + place;
				case WALK:
					return ticks(ticks) + " lost on foot to " + place;
				case STAGE:
					String name = stageNames != null && stage < stageNames.length ? stageNames[stage] : "One stage";
					return name + " " + ticks(ticks) + " over your best for it";
				default:
					return "No leak of " + LEAK_MIN + " ticks or more";
			}
		}

		private static String ticks(int n)
		{
			return n == 1 ? "1 tick" : n + " ticks";
		}
	}

	private final Map<String, Integer> bests = new HashMap<>();
	private final Map<String, int[]> stageBests = new HashMap<>();
	private final int[] stageTicks = new int[STAGES];
	private final List<Round> rounds = new ArrayList<>();

	private boolean started;
	private boolean leftStart;
	private int ticks;
	private int mask;
	private int unitsAtStart = -1;
	private int units;
	private int idle;
	private final Map<String, Integer> idleAt = new HashMap<>();
	private final Map<String, Integer> extraAt = new HashMap<>();
	private int extraTotal;
	private int stillRun;
	private String leg;
	private String legPlace;
	private int legPlanned;
	private int legWalked;
	private int idleTotal;
	private String roundHint;
	private int legFoot;
	private int footTicks;
	private int floorTicks;
	private final Map<String, Integer> walkLossAt = new HashMap<>();
	private int xpAtStart = -1;
	private int xpNow = -1;
	private boolean xpLost;

	/**
	 * One game tick.
	 *
	 * @param stage where in the loop the step sits, 0 being the start of a round
	 * @param leg a key for the current step and its target, so a new walk is told from an old one
	 * @param place where the step is, said as "the bank"
	 * @param still whether you stood still this tick (not moving, not animating, nothing open)
	 * @param waiting whether the step is to wait, so standing still is right
	 * @param moved tiles moved since the last tick
	 * @param routeLength the shortest walk to the target from here, or -1 when it is not known
	 * @param unitCount a running count of what a round produces, such as pay-dirt deposited or bars taken
	 * @param kindHint what makes rounds comparable beyond their shape, such as the contract, or null;
	 *                 the last one seen during a round is the one it is filed under
	 * @return the round that just finished, or null
	 */
	public Round tick(int stage, String leg, String place, boolean still, boolean waiting, int moved,
		int routeLength, int unitCount, String kindHint)
	{
		return tick(stage, leg, place, still, waiting, moved, routeLength, unitCount, kindHint, -1);
	}

	/**
	 * {@link #tick(int, String, String, boolean, boolean, int, int, int, String)} with the
	 * experience in the minigame's skill now, or -1 when it is not known, for experience a round.
	 */
	public Round tick(int stage, String leg, String place, boolean still, boolean waiting, int moved,
		int routeLength, int unitCount, String kindHint, int xp)
	{
		Round finished = null;
		// A round ends on the tick the loop comes back to its start, so that tick's experience
		// closes it; the next round starts from the same reading.
		xpNow = xp;
		if (!started)
		{
			if (stage != 0)
			{
				// Rounds are only timed from the start of the loop, so the first part-round is skipped.
				return null;
			}
			begin(unitCount);
		}
		else if (stage == 0 && leftStart)
		{
			endLeg();
			units = unitCount;
			finished = finish(roundHint);
			begin(unitCount);
		}
		if (kindHint != null)
		{
			roundHint = kindHint.replaceAll("[;=/#]", " ");
		}
		if (stage != 0)
		{
			leftStart = true;
		}
		if (stage >= 0 && stage < STAGES)
		{
			mask |= 1 << stage;
			stageTicks[stage]++;
		}
		units = unitCount;
		ticks++;

		if (leg == null || !leg.equals(this.leg))
		{
			endLeg();
			this.leg = leg;
			legPlace = place;
			legPlanned = routeLength;
			legWalked = 0;
			legFoot = 0;
			stillRun = 0;
		}
		legWalked += Math.max(0, moved);
		if (moved > 0 && legPlanned > 0)
		{
			legFoot++;
		}
		if (xp < 0)
		{
			xpLost = true;
		}

		if (still && !waiting)
		{
			stillRun++;
			if (stillRun > GRACE)
			{
				idle++;
				idleTotal++;
				if (place != null)
				{
					idleAt.merge(place, 1, Integer::sum);
				}
			}
		}
		else
		{
			stillRun = 0;
		}
		return finished;
	}

	/** Leaving the minigame: the round in progress is dropped, as it was not finished. */
	public void reset()
	{
		started = false;
		leg = null;
	}

	public List<Round> rounds()
	{
		return Collections.unmodifiableList(rounds);
	}

	public Round last()
	{
		return rounds.isEmpty() ? null : rounds.get(rounds.size() - 1);
	}

	/** Your best for each stage of a kind of round, by stage; zeros where there is none. */
	public int[] stageBests(String kind)
	{
		int[] b = stageBests.get(kind);
		return b == null ? new int[STAGES] : b.clone();
	}

	public int best(String kind)
	{
		Integer b = bests.get(kind);
		return b == null ? 0 : b;
	}

	/** Ticks spent standing still with something to do, across every round so far. */
	public int idleTotal()
	{
		return idleTotal;
	}

	/** The usual round of this kind this session, the median, or 0 before there are any. */
	public int typical(String kind)
	{
		List<Integer> t = new ArrayList<>();
		for (Round r : rounds)
		{
			if (r.kind.equals(kind) && !r.hadBreak)
			{
				t.add(r.ticks);
			}
		}
		if (t.isEmpty())
		{
			return 0;
		}
		Collections.sort(t);
		return t.get(t.size() / 2);
	}

	/** Bests carried over from earlier sessions, as "kind=ticks;kind=ticks". */
	public void loadBests(String saved)
	{
		bests.clear();
		stageBests.clear();
		if (saved == null || saved.isEmpty())
		{
			return;
		}
		for (String part : saved.split(";"))
		{
			int eq = part.lastIndexOf('=');
			if (eq <= 0)
			{
				continue;
			}
			try
			{
				int t = Integer.parseInt(part.substring(eq + 1));
				String key = part.substring(0, eq);
				int mark = key.lastIndexOf(STAGE_MARK);
				if (t <= 0)
				{
					continue;
				}
				if (mark > 0)
				{
					int stage = Integer.parseInt(key.substring(mark + 1));
					if (stage >= 0 && stage < STAGES)
					{
						stageBests.computeIfAbsent(key.substring(0, mark), k -> new int[STAGES])[stage] = t;
					}
				}
				else
				{
					bests.put(key, t);
				}
			}
			catch (NumberFormatException ignored)
			{
				// A damaged entry is dropped; the rest still count.
			}
		}
	}

	public String saveBests()
	{
		StringBuilder b = new StringBuilder();
		java.util.TreeMap<String, Integer> all = new java.util.TreeMap<>(bests);
		for (Map.Entry<String, int[]> e : stageBests.entrySet())
		{
			for (int s = 0; s < STAGES; s++)
			{
				if (e.getValue()[s] > 0)
				{
					all.put(e.getKey() + STAGE_MARK + s, e.getValue()[s]);
				}
			}
		}
		all.forEach((k, v) ->
		{
			if (b.length() > 0)
			{
				b.append(';');
			}
			b.append(k).append('=').append(v);
		});
		return b.toString();
	}

	private void begin(int unitCount)
	{
		started = true;
		leftStart = false;
		ticks = 0;
		mask = 0;
		unitsAtStart = unitCount;
		units = unitCount;
		idle = 0;
		idleAt.clear();
		extraAt.clear();
		extraTotal = 0;
		roundHint = null;
		java.util.Arrays.fill(stageTicks, 0);
		footTicks = 0;
		floorTicks = 0;
		walkLossAt.clear();
		xpAtStart = xpNow;
		xpLost = xpNow < 0;
	}

	private void endLeg()
	{
		if (leg != null && legPlanned > 0 && legPlace != null)
		{
			int extra = legWalked - legPlanned;
			if (extra > TOLERANCE)
			{
				extraTotal += extra;
				extraAt.merge(legPlace, extra, Integer::sum);
			}
			// The floor counts only the walk you made: a leg cut short by a new step is weighed
			// up to where you got, at run pace over the shortest way.
			if (legFoot > 0)
			{
				int floor = (Math.min(legPlanned, legWalked) + RUN_TILES - 1) / RUN_TILES;
				footTicks += legFoot;
				floorTicks += floor;
				if (legFoot > floor)
				{
					walkLossAt.merge(legPlace, legFoot - floor, Integer::sum);
				}
			}
		}
		leg = null;
		legFoot = 0;
	}

	private Round finish(String kindHint)
	{
		int made = Math.max(0, units - unitsAtStart);
		String kind = Integer.toString(mask) + "/" + made + (kindHint == null ? "" : "/" + kindHint);
		int typical = typical(kind);
		boolean hadBreak = typical > 0 && ticks > typical * BREAK_FACTOR;
		int bestBefore = best(kind);
		Map.Entry<String, Integer> worstIdle = worst(idleAt);
		Map.Entry<String, Integer> worstWalk = worst(extraAt);
		int[] stagesBefore = stageBests(kind);
		Round r = new Round(kind, ticks, made, idle,
			worstIdle == null ? null : worstIdle.getKey(), worstIdle == null ? 0 : worstIdle.getValue(),
			extraTotal, worstWalk == null ? null : worstWalk.getKey(), worstWalk == null ? 0 : worstWalk.getValue(),
			bestBefore, hadBreak, stageTicks, stagesBefore);
		if (r.isBest())
		{
			bests.put(kind, ticks);
		}
		if (!hadBreak)
		{
			// Each stage keeps its own best, so a round can be slow overall yet set a best bank trip.
			int[] sb = stageBests.computeIfAbsent(kind, k -> new int[STAGES]);
			for (int s = 0; s < STAGES; s++)
			{
				if (stageTicks[s] > 0 && (sb[s] == 0 || stageTicks[s] < sb[s]))
				{
					sb[s] = stageTicks[s];
				}
			}
		}
		r.footTicks = footTicks;
		r.floorTicks = floorTicks;
		Map.Entry<String, Integer> worstFoot = worst(walkLossAt);
		r.walkPlace = worstFoot == null ? null : worstFoot.getKey();
		r.walkLoss = worstFoot == null ? 0 : worstFoot.getValue();
		r.xp = xpLost || xpAtStart < 0 || xpNow < xpAtStart ? -1 : xpNow - xpAtStart;
		rounds.add(r);
		if (rounds.size() > KEEP)
		{
			rounds.remove(0);
		}
		return r;
	}

	private static Map.Entry<String, Integer> worst(Map<String, Integer> m)
	{
		Map.Entry<String, Integer> w = null;
		for (Map.Entry<String, Integer> e : m.entrySet())
		{
			if (w == null || e.getValue() > w.getValue())
			{
				w = e;
			}
		}
		return w == null ? null : new java.util.AbstractMap.SimpleImmutableEntry<>(w);
	}

	/** Ticks as m:ss. */
	public static String clock(int ticks)
	{
		long ms = ticks * 600L;
		long s = Math.round(ms / 1000d);
		return (s / 60) + ":" + String.format("%02d", s % 60);
	}
}
