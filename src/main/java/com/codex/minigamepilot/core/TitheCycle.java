package com.codex.minigamepilot.core;

import com.codex.minigamepilot.core.Tithe.Seed;
import com.codex.minigamepilot.core.Tithe.State;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Works out the one next thing to do at the Tithe Farm from the patches themselves: plant the next
 * patch of your round with the best seed your Farming grows, water each new seed at once, keep
 * watering round the patches in your order before each one's minute runs out, harvest what has
 * grown, clear what died, and between rounds deposit the fruit and fill your cans.
 *
 * <p>Facts are from the OSRS wiki (see {@link Tithe}). It only reads; every click is yours.
 */
public final class TitheCycle
{
	/** The stages of a round, for the loop strip. */
	public static final String[] LOOP = {"Seeds", "Plant", "Water", "Harvest", "Deposit"};
	/** A dry plant with this little of its minute left is watered first, wherever it is in the round (15 seconds). */
	public static final int URGENT_TICKS = 25;

	private TitheCycle()
	{
	}

	/** One tithe patch as the game shows it. */
	public static final class Plot
	{
		public State state = State.EMPTY;
		/** 1 to 4 while planted, 0 when empty. */
		public int stage;
		public Seed seed;
		/** Ticks until this stage ends and an unwatered plant dies, or -1 when not known. */
		public int ticksLeft = -1;

		public Plot()
		{
		}

		public Plot(State state, int stage, int ticksLeft)
		{
			this.state = state;
			this.stage = stage;
			this.ticksLeft = ticksLeft;
		}

		/** Growing and not yet grown: still to be watered or waiting for its stage to end. */
		public boolean growing()
		{
			return state == State.DRY || state == State.WET;
		}

		/** Waterings still owed: three a plant, less those it has had. */
		public int owed()
		{
			switch (state)
			{
				case DRY:
					return Tithe.WATERINGS - (stage - 1);
				case WET:
					return Math.max(0, Tithe.WATERINGS - stage);
				default:
					return 0;
			}
		}
	}

	/** Everything a decision needs, read from the game. */
	public static final class Snapshot
	{
		/** Inside the farm (its patches in sight), else in the room with the seed table. */
		public boolean inFarm;
		/** Farming, boosts included. */
		public int level;
		public final List<Plot> plots = new ArrayList<>();
		/** This round's patches (indexes into {@link #plots}) in the order you planted them. */
		public final List<Integer> round = new ArrayList<>();
		/** The patches to plant next, in order: your own route, else the nearest. */
		public final List<Integer> toPlant = new ArrayList<>();
		/** Plants a round, from the settings. */
		public Tithe.Plan plan = Tithe.Plan.TWENTY;
		/** Seeds carried, by {@link Seed#ordinal()}. */
		public final int[] seeds = new int[Seed.values().length];
		public int fruit;
		/** Water doses carried, Gricoller's can included. */
		public int doses;
		public boolean gricoller;
		public boolean anyCan;
		public boolean spade;
		public boolean dibber;
		/** Fruit in the sack this game. */
		public int sack;
		/** A water barrel is in sight. */
		public boolean barrel;
	}

	/** What to do and where. */
	public static final class Decision
	{
		public final Step step;
		/** The patch the step is about (index into the plots), or -1. */
		public final int plot;

		Decision(Step step, int plot)
		{
			this.step = step;
			this.plot = plot;
		}
	}

	/** The seed to plant: the best your level grows that you carry, else the best you carry at all. */
	public static Seed seedCarried(Snapshot s)
	{
		Seed best = null;
		for (Seed seed : Seed.values())
		{
			if (s.seeds[seed.ordinal()] > 0 && seed.level() <= s.level)
			{
				best = seed;
			}
		}
		return best;
	}

	/** Plants this round can still take: the plan, less those planted, no more than your seeds and water allow. */
	public static int roomInRound(Snapshot s)
	{
		Seed seed = seedCarried(s);
		int seeds = seed == null ? 0 : s.seeds[seed.ordinal()];
		int left = s.plan.plants() - s.round.size();
		int water = Math.max(0, s.doses - owedInRound(s)) / Tithe.WATERINGS;
		return Math.max(0, Math.min(left, Math.min(seeds, water)));
	}

	/** Waterings the plants of this round still need. */
	public static int owedInRound(Snapshot s)
	{
		int n = 0;
		for (int i : s.round)
		{
			n += s.plots.get(i).owed();
		}
		return n;
	}

	/** Whether any plant of the round still grows or waits to be harvested or cleared. */
	public static boolean roundOn(Snapshot s)
	{
		for (int i : s.round)
		{
			if (s.plots.get(i).state != State.EMPTY)
			{
				return true;
			}
		}
		return false;
	}

	public static Decision next(Snapshot s)
	{
		Seed best = Seed.best(s.level);
		if (best == null)
		{
			return new Decision(Step.waiting("The Tithe Farm needs " + Tithe.LEVEL + " Farming",
				"Golovanova, the first seed, grows from " + Tithe.LEVEL + " Farming; boost before you take seeds and before you plant", Step.Target.NONE), -1);
		}
		return s.inFarm ? inFarm(s, best) : atTable(s, best);
	}

	/** In the room before the farm: the right seeds, full cans, then the door. */
	private static Decision atTable(Snapshot s, Seed best)
	{
		Seed carried = seedCarried(s);
		if (carried != best)
		{
			String why = "Your Farming (" + s.level + ") grows " + lower(best) + ", from " + best.level() + ": "
				+ Figures.count(best.sackXp()) + " xp a sack of 100";
			if (carried != null)
			{
				why = "The " + lower(carried) + " seeds you carry give less: " + lower(best) + " grows from " + best.level() + " Farming";
			}
			return new Decision(Step.of("Take", "Take " + lower(best) + " seeds", why, Step.Target.SEED_TABLE)
				.withItems(best.seed()), -1);
		}
		if (!s.anyCan)
		{
			return new Decision(Step.waiting("Bring watering cans",
				s.plan.getTitle() + " a round take " + s.plan.cans() + " full watering cans, or Gricoller's can", Step.Target.NONE), -1);
		}
		if (s.doses < s.plan.doses())
		{
			return fill(s, s.plan.doses());
		}
		return new Decision(Step.of("Enter", "Enter the farm",
			Figures.count(s.seeds[carried.ordinal()]) + " " + lower(carried) + " seeds, " + Figures.count(s.doses) + " doses of water: "
				+ s.plan.getTitle() + " a round", Step.Target.FARM_DOOR), -1);
	}

	private static Decision inFarm(Snapshot s, Seed best)
	{
		// A plant about to die is watered first, wherever it is.
		int urgent = -1;
		for (int i : s.round)
		{
			Plot p = s.plots.get(i);
			if (p.state == State.DRY && p.ticksLeft >= 0 && p.ticksLeft <= URGENT_TICKS
				&& (urgent < 0 || p.ticksLeft < s.plots.get(urgent).ticksLeft))
			{
				urgent = i;
			}
		}
		boolean anyDry = false;
		for (int i : s.round)
		{
			anyDry |= s.plots.get(i).state == State.DRY;
		}
		if (anyDry && s.doses <= 0)
		{
			return fill(s, owedInRound(s));
		}
		if (urgent >= 0)
		{
			Plot p = s.plots.get(urgent);
			return water(s, urgent, "Only " + seconds(p.ticksLeft) + " left before it wilts");
		}
		// A seed just planted is watered at once: "Plant and water on the first round."
		for (int k = s.round.size() - 1; k >= 0; k--)
		{
			int i = s.round.get(k);
			Plot p = s.plots.get(i);
			if (p.state == State.DRY && p.stage == 1)
			{
				return water(s, i, "Water each seed as you plant it, then on to the next");
			}
		}
		// Planting, while the round has room and nothing in it has grown or been harvested yet.
		boolean grown = false;
		for (int i : s.round)
		{
			State st = s.plots.get(i).state;
			grown |= st == State.GROWN || st == State.EMPTY;
		}
		Seed seed = seedCarried(s);
		int room = roomInRound(s);
		if (!grown && room > 0 && !s.toPlant.isEmpty() && !(s.round.isEmpty() && s.fruit > 0))
		{
			if (s.round.isEmpty() && s.doses < s.plan.doses() && s.barrel && s.doses < Tithe.GRICOLLER_DOSES)
			{
				return fill(s, s.plan.doses());
			}
			int i = s.toPlant.get(0);
			int n = s.round.size() + 1;
			int of = Math.min(s.plan.plants(), s.round.size() + room);
			String why = s.round.isEmpty()
				? "A new round of " + of + " " + lower(seed) + ": plant, water at once, then the next"
				: seed.getTitle() + (seed == best ? ", the best your Farming grows" : ": " + lower(best) + " gives more")
				+ (of < s.plan.plants() ? "; " + of + " this round" + limit(s, seed) : "");
			return new Decision(Step.of("Plant", "Plant " + n + " of " + of, why, Step.Target.PLOT)
				.withItems(seed.seed()), i);
		}
		// Round the patches in your order, from where you last were.
		for (int i : s.round)
		{
			if (s.plots.get(i).state == State.DRY)
			{
				Plot p = s.plots.get(i);
				return water(s, i, p.ticksLeft >= 0 ? seconds(p.ticksLeft) + " left in its stage" : "It needs water before its stage ends");
			}
		}
		for (int i : s.round)
		{
			if (s.plots.get(i).state == State.GROWN)
			{
				return new Decision(Step.of("Harvest", "Harvest " + (s.round.indexOf(i) + 1) + " of " + s.round.size(),
					"Grown plants cannot die: water first, then harvest", Step.Target.PLOT), i);
			}
		}
		for (int i : s.round)
		{
			if (s.plots.get(i).state == State.DEAD)
			{
				return new Decision(Step.of("Clear", "Clear the blighted plant",
					"It was not watered in time; clearing it frees the patch", Step.Target.PLOT), i);
			}
		}
		// All watered: wait by the one that needs water next.
		int soonest = -1;
		for (int i : s.round)
		{
			Plot p = s.plots.get(i);
			if (p.state == State.WET && (soonest < 0 || later(s.plots.get(soonest), p)))
			{
				soonest = i;
			}
		}
		if (soonest >= 0)
		{
			Plot p = s.plots.get(soonest);
			String when = p.ticksLeft >= 0 ? "in " + seconds(p.ticksLeft) : "when its stage ends";
			String title = p.stage >= Tithe.WATERINGS ? "Wait: it grows " + when : "Wait: it needs water " + when;
			return new Decision(Step.waiting(title, "Plant " + (s.round.indexOf(soonest) + 1) + " of your round is next", Step.Target.PLOT), soonest);
		}
		// Between rounds.
		if (s.fruit > 0)
		{
			int room100 = Tithe.SACK - s.sack;
			String why = s.sack + s.fruit >= Tithe.SACK
				? "It fills the sack at 100: " + Tithe.sackPoints(Tithe.SACK) + " points with the bonus"
				: Figures.count(s.fruit) + " fruit; the sack holds " + room100 + " more before it pays its bonus";
			return new Decision(Step.of("Deposit", "Deposit the fruit", why, Step.Target.SACK), -1);
		}
		if (seed == null)
		{
			Seed carried = null;
			for (Seed x : Seed.values())
			{
				carried = s.seeds[x.ordinal()] > 0 ? x : carried;
			}
			return new Decision(Step.of("Leave", "Leave for " + lower(best) + " seeds",
				carried != null ? "Your " + lower(carried) + " seeds need " + carried.level() + " Farming" : "The seed table is by the door, outside the farm",
				Step.Target.FARM_DOOR), -1);
		}
		if (s.doses < Tithe.WATERINGS || s.doses < s.plan.doses() && s.barrel)
		{
			return fill(s, s.plan.doses());
		}
		return new Decision(Step.waiting("Every patch of your round is taken",
			"Harvest or clear a patch, or set fewer plants a round", Step.Target.NONE), -1);
	}

	private static String limit(Snapshot s, Seed seed)
	{
		int seeds = seed == null ? 0 : s.seeds[seed.ordinal()];
		return seeds < s.plan.plants() ? ", as many as your seeds" : ", as many as your water covers";
	}

	/** Which of two watered plants needs water first: the one with less time left, unknown last. */
	private static boolean later(Plot a, Plot b)
	{
		if (b.ticksLeft < 0)
		{
			return false;
		}
		return a.ticksLeft < 0 || b.ticksLeft < a.ticksLeft;
	}

	private static Decision water(Snapshot s, int i, String why)
	{
		int n = s.round.indexOf(i) + 1;
		Plot p = s.plots.get(i);
		String which = p.stage == 1 ? "first" : p.stage == 2 ? "second" : "third";
		return new Decision(Step.of("Water", "Water " + n + " of " + s.round.size() + " (" + which + ")", why, Step.Target.PLOT)
			.withItems(Tithe.CANS), i);
	}

	private static Decision fill(Snapshot s, int need)
	{
		String why = s.gricoller
			? "Gricoller's can: " + Figures.count(s.doses) + " of " + Figures.count(Tithe.GRICOLLER_DOSES) + " doses; the rewards shop refills it free"
			: Figures.count(s.doses) + " doses carried; " + s.plan.getTitle() + " take " + Figures.count(s.plan.doses())
				+ " (" + s.plan.cans() + " full cans)";
		if (need > s.plan.doses())
		{
			why = "Your plants still need " + need + " waterings; you carry " + s.doses;
		}
		return new Decision(Step.of("Fill", s.gricoller ? "Fill Gricoller's can" : "Fill your watering cans", why,
			s.barrel ? Step.Target.WATER_BARREL : Step.Target.NONE).withItems(Tithe.CANS), -1);
	}

	/** Where in the loop strip a step falls. */
	public static int stage(Step step, Snapshot s)
	{
		if (!s.inFarm)
		{
			return 0;
		}
		String verb = step.getVerb();
		if ("Plant".equals(verb))
		{
			return 1;
		}
		if ("Harvest".equals(verb) || "Clear".equals(verb))
		{
			return 3;
		}
		if ("Deposit".equals(verb))
		{
			return 4;
		}
		if ("Leave".equals(verb))
		{
			return 0;
		}
		return roundOn(s) ? 2 : 4;
	}

	static String lower(Seed s)
	{
		return s.getTitle().toLowerCase(Locale.ROOT);
	}

	/** "0:42" for ticks. */
	public static String seconds(int ticks)
	{
		int sec = Math.max(0, ticks) * 6 / 10;
		return sec / 60 + ":" + (sec % 60 < 10 ? "0" : "") + sec % 60;
	}
}
