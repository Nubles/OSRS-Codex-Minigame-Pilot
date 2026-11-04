package com.codex.minigamepilot.core;

import com.codex.minigamepilot.core.Mixology.Paste;
import com.codex.minigamepilot.core.Mixology.Potion;
import com.codex.minigamepilot.core.Mixology.Station;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The one next step through a round of Mastering Mixology: which of the three orders to make,
 * the levers for each, the vessel, each potion at the station its order names, and the conveyor
 * belt; with the paste hopper kept fed and digweed picked and used on the right potion.
 *
 * <p>Plain Java over a snapshot of the game, so every turn can be tested. Facts (OSRS wiki,
 * Mastering Mixology and Mastering Mixology/Strategies):
 * <ul>
 * <li>three orders are open at once; the conveyor belt takes up to three potions, fills as many
 * orders as it can, and then all three orders change; two filled at once pay 20% more resin, all
 * three 40% more; a potion that fills no order is taken for less;</li>
 * <li>a lever adds its paste to the vessel's three slots in any order, a fourth pull overwrites
 * the oldest, and paste (10 a pull) is only taken from the hopper when the base is taken;</li>
 * <li>herbs and unfinished potions go in the refiner, which makes paste, and paste goes in the
 * hopper by the levers;</li>
 * <li>"making all orders makes the most out of the 40% point bonus"; for the most resin per
 * paste, make every potion in an order except mammoth-might mix, alco-augmentator and liplack
 * liquor, unless a mixalot is ordered too;</li>
 * <li>process at the alembic, then the retort, then the agitator; click the retort every tick;
 * click the agitator again inside the two ticks after its cue, the alembic as its bellows open the
 * fourth time;</li>
 * <li>digweed doubles a potion's experience and resin, one a potion; use it on a mixalot, or below
 * 81 on the potion that gives the most experience.</li>
 * </ul>
 */
public final class MixCycle
{
	/** Stages of the loop, for the pace coach and the panel. */
	public static final String[] LOOP = {"Paste", "Mix", "Process", "Deliver"};

	private MixCycle()
	{
	}

	/** Which orders to make. */
	public enum Plan
	{
		/** Every order: the wiki's simplest way, and the most from the 40% bonus. */
		ALL("Every order"),
		/** Skip mammoth-might mix, alco-augmentator and liplack liquor unless a mixalot is ordered. */
		PER_PASTE("Most resin per paste");

		private final String title;

		Plan(String title)
		{
			this.title = title;
		}

		@Override
		public String toString()
		{
			return title;
		}
	}

	/** One open order and whether its potion is already made. */
	public static final class Order
	{
		public final Potion potion;
		public final Station station;
		/** A potion worked at its station is in your pack for it. */
		public final boolean done;

		public Order(Potion potion, Station station, boolean done)
		{
			this.potion = potion;
			this.station = station;
			this.done = done;
		}
	}

	/** What the Pilot read this tick. */
	public static final class Snapshot
	{
		/** The three open orders, null where none is shown. */
		public Order[] orders = new Order[3];
		public int level;
		/** Paste in the hopper, by {@link Paste#ordinal()}. */
		public int[] hopper = new int[3];
		/** Paste carried as items, from the refiner, not yet in the hopper. */
		public int[] pasteCarried = new int[3];
		/** Paste the herbs and unfinished potions you carry would make. */
		public int[] herbPaste = new int[3];
		/** What the vessel's three slots hold now, nulls for empty. */
		public Paste[] slots = new Paste[3];
		/** The potion ready to take from the vessel, or null. */
		public Potion vessel;
		/** The potion each station is working, by {@link Station#ordinal()}, nulls for none. */
		public Potion[] stations = new Potion[3];
		/** The station whose quick action is live now, or null. */
		public Station cue;
		/** Potion bases (from the vessel, not yet worked) carried, by {@link Potion#ordinal()}. */
		public int[] bases = new int[Potion.values().length];
		/** Worked potions carried. */
		public int[] finished = new int[Potion.values().length];
		/** A digweed is in your pack. */
		public boolean digweed;
		/** A digweed has grown in a corner, as the game shows it. */
		public boolean digweedReady;
		public int freeSlots = 28;
		public Plan plan = Plan.ALL;
	}

	/** The next step and what it is about. */
	public static final class Decision
	{
		public final Step step;
		/** The lever to pull, for a lever step. */
		public final Paste lever;
		/** The potion the step is about, to ring in the pack, or null. */
		public final Potion potion;
		/** The orders this round makes, by index. */
		public final List<Integer> planned;

		Decision(Step step, Paste lever, Potion potion, List<Integer> planned)
		{
			this.step = step;
			this.lever = lever;
			this.potion = potion;
			this.planned = planned;
		}
	}

	/**
	 * The orders to make this round, by index: those the plan keeps whose paste the hopper has.
	 * Of the orders the plan keeps, as many as the paste allows are made, the bigger resin first;
	 * none at all is never the answer while one can be made, since only a deposit changes the
	 * orders.
	 */
	public static List<Integer> plan(Snapshot s)
	{
		List<Integer> open = new ArrayList<>();
		boolean mixalot = false;
		for (int i = 0; i < 3; i++)
		{
			Order o = s.orders[i];
			if (o != null && o.potion.level() <= Math.max(s.level, 0))
			{
				open.add(i);
				mixalot |= o.potion == Potion.MAL;
			}
		}
		List<Integer> wanted = new ArrayList<>();
		for (int i : open)
		{
			Order o = s.orders[i];
			if (s.plan == Plan.ALL || !o.potion.single() || mixalot || o.done)
			{
				wanted.add(i);
			}
		}
		if (wanted.isEmpty() && !open.isEmpty())
		{
			// Every order is a single-paste one: one of them, of the paste there is most of,
			// is the cheapest way to new orders.
			int best = open.get(0);
			for (int i : open)
			{
				if (paste(s, s.orders[i].potion) > paste(s, s.orders[best].potion))
				{
					best = i;
				}
			}
			wanted.add(best);
		}
		// Paste: the potions still to mix must fit in the hopper (what is already mixed is paid for).
		List<Integer> sorted = new ArrayList<>(wanted);
		sorted.sort((a, b) -> s.orders[b].potion.totalResin() - s.orders[a].potion.totalResin());
		int[] left = s.hopper.clone();
		int[] spare = s.bases.clone();
		int[] spareDone = s.finished.clone();
		List<Integer> out = new ArrayList<>();
		for (int i : sorted)
		{
			Potion p = s.orders[i].potion;
			if (s.orders[i].done && spareDone[p.ordinal()] > 0)
			{
				spareDone[p.ordinal()]--;
				out.add(i);
				continue;
			}
			if (spare[p.ordinal()] > 0 || inStation(s, p) || s.vessel == p)
			{
				if (spare[p.ordinal()] > 0)
				{
					spare[p.ordinal()]--;
				}
				out.add(i);
				continue;
			}
			boolean fits = true;
			for (Paste x : Paste.values())
			{
				fits &= left[x.ordinal()] >= p.paste(x);
			}
			if (fits)
			{
				for (Paste x : Paste.values())
				{
					left[x.ordinal()] -= p.paste(x);
				}
				out.add(i);
			}
		}
		Collections.sort(out);
		return out;
	}

	/** How much of a potion's own paste the hopper holds, to choose between single-paste orders. */
	private static int paste(Snapshot s, Potion p)
	{
		int least = Integer.MAX_VALUE;
		for (Paste x : Paste.values())
		{
			if (p.uses(x) > 0)
			{
				least = Math.min(least, s.hopper[x.ordinal()]);
			}
		}
		return least;
	}

	private static boolean inStation(Snapshot s, Potion p)
	{
		for (Potion q : s.stations)
		{
			if (q == p)
			{
				return true;
			}
		}
		return false;
	}

	/** The next step. */
	public static Decision next(Snapshot s)
	{
		List<Integer> planned = plan(s);

		// A potion at a station: keep at it, the quick action when it is live.
		for (Station st : Station.values())
		{
			Potion p = s.stations[st.ordinal()];
			if (p != null)
			{
				return new Decision(working(s, st, p), null, p, planned);
			}
		}

		// Digweed in hand: use it on the potion it pays most on, once that potion is carried.
		if (s.digweed)
		{
			Potion target = digweedTarget(s, planned);
			if (target != null)
			{
				return new Decision(Step.items("Use", "Use the digweed on the " + target.getTitle().toLowerCase(java.util.Locale.ROOT),
					"It doubles the potion's experience and resin", target.unfinished(), target.finished(),
					net.runelite.api.gameval.ItemID.MM_LAB_SPECIAL_HERB), null, target, planned);
			}
		}
		else if (s.digweedReady && s.freeSlots > 0)
		{
			return new Decision(Step.of("Pick", "Pick the digweed", "Used on a potion it doubles its experience and resin",
				Step.Target.DIGWEED), null, null, planned);
		}

		if (planned.isEmpty())
		{
			return new Decision(paste(s), null, null, planned);
		}

		// Which planned orders still need what. Potions carried are matched to orders by kind.
		int[] bases = s.bases.clone();
		int[] finished = s.finished.clone();
		List<Integer> toWork = new ArrayList<>();
		List<Integer> toMix = new ArrayList<>();
		for (int i : planned)
		{
			Order o = s.orders[i];
			int k = o.potion.ordinal();
			if (o.done && finished[k] > 0)
			{
				finished[k]--;
			}
			else if (bases[k] > 0)
			{
				bases[k]--;
				toWork.add(i);
			}
			else
			{
				toMix.add(i);
			}
		}

		// The vessel holds a potion: take it if an order wants it, else the levers change it.
		if (s.vessel != null)
		{
			for (int i : toMix)
			{
				if (s.orders[i].potion == s.vessel)
				{
					return new Decision(Step.of("Take", "Take the " + lower(s.vessel) + " from the vessel",
						"For the " + s.orders[i].station.getTitle().toLowerCase(java.util.Locale.ROOT) + " order"
							+ (s.freeSlots <= 0 ? " · make room in your pack first" : ""), Step.Target.MIXING_VESSEL),
						null, s.vessel, planned);
				}
			}
		}

		if (!toMix.isEmpty())
		{
			// The first order whose recipe the paste already in the vessel fits, so no pull is wasted.
			Order o = s.orders[toMix.get(0)];
			for (int i : toMix)
			{
				if (fits(s.slots, s.orders[i].potion))
				{
					o = s.orders[i];
					break;
				}
			}
			return mix(s, o, planned);
		}

		if (!toWork.isEmpty())
		{
			toWork.sort((a, b) -> s.orders[a].station.rank() - s.orders[b].station.rank());
			Order o = s.orders[toWork.get(0)];
			Station st = o.station;
			return new Decision(Step.of(st.getVerb(), st.getVerb() + " the " + lower(o.potion) + " at " + st.the(),
				startLine(st), st.target()), null, o.potion, planned);
		}

		int filling = planned.size();
		return new Decision(Step.of("Fill", "Deliver on the conveyor belt",
			filling == 3 ? "All three orders at once: 40% more resin"
				: filling == 2 ? "Two orders at once: 20% more resin" : "One order: new orders follow", Step.Target.CONVEYOR),
			null, null, planned);
	}

	/** Whether the paste in the vessel's slots is all part of a potion's recipe. */
	static boolean fits(Paste[] slots, Potion p)
	{
		int[] have = new int[3];
		for (Paste x : slots)
		{
			if (x != null)
			{
				have[x.ordinal()]++;
			}
		}
		for (Paste x : Paste.values())
		{
			if (have[x.ordinal()] > p.uses(x))
			{
				return false;
			}
		}
		return true;
	}

	/** The levers for a potion, or the vessel's slots set right. */
	private static Decision mix(Snapshot s, Order o, List<Integer> planned)
	{
		Potion p = o.potion;
		int[] have = new int[3];
		int filled = 0;
		for (Paste x : s.slots)
		{
			if (x != null)
			{
				have[x.ordinal()]++;
				filled++;
			}
		}
		boolean fits = true;
		for (Paste x : Paste.values())
		{
			fits &= have[x.ordinal()] <= p.uses(x);
		}
		String recipe = recipe(p);
		if (fits && filled < 3)
		{
			// The paste still missing, in the potion's own letter order: what is in already counts
			// for the first letters it can.
			int[] left = have.clone();
			for (char c : p.code().toCharArray())
			{
				Paste x = ofLetter(c);
				if (left[x.ordinal()] > 0)
				{
					left[x.ordinal()]--;
				}
				else
				{
					int pulls = 3 - filled;
					return new Decision(Step.of("Pull", "Pull the " + x.getTitle().toLowerCase(java.util.Locale.ROOT) + " lever",
						p.getTitle() + " · " + recipe + (pulls > 1 ? " · " + pulls + " pulls to go" : " · last pull"), Step.Target.LEVER),
						x, p, planned);
				}
			}
		}
		// Wrong paste in the slots: a pull overwrites the oldest, so the whole recipe again sets it right.
		Paste first = ofLetter(p.code().charAt(0));
		return new Decision(Step.of("Pull", "Pull the " + first.getTitle().toLowerCase(java.util.Locale.ROOT) + " lever",
			p.getTitle() + " · " + recipe + " · pull all three again: the vessel holds other paste, and none is lost",
			Step.Target.LEVER), first, p, planned);
	}

	/** At a station with a potion in it. */
	private static Step working(Snapshot s, Station st, Potion p)
	{
		String title = st.getVerb() + " the " + lower(p);
		switch (st)
		{
			case RETORT:
				return Step.of("Click", "Keep clicking the retort", "Every tick speeds it up and gives 2 xp", Step.Target.RETORT);
			case AGITATOR:
				return s.cue == Station.AGITATOR
					? Step.of("Click", "Click the agitator now", "The cue's two-tick window is open", Step.Target.AGITATOR)
					: Step.waiting(title + " · wait for the cue", "Click again inside the two ticks after it", Step.Target.AGITATOR);
			default:
				return s.cue == Station.ALEMBIC
					? Step.of("Click", "Click the alembic now", "The bellows are open", Step.Target.ALEMBIC)
					: Step.waiting(title + " · wait for the bellows", "Click as they open the fourth time", Step.Target.ALEMBIC);
		}
	}

	private static String startLine(Station st)
	{
		switch (st)
		{
			case RETORT:
				return "Then click it every tick";
			case AGITATOR:
				return "Then click again inside the two ticks after its cue";
			default:
				return "Then click again as the bellows open the fourth time";
		}
	}

	/** Where to get the paste the next order needs: the hopper, the refiner, or the bank. */
	static Step paste(Snapshot s)
	{
		Paste short_ = shortest(s);
		String what = short_ == null ? "paste" : short_.getTitle().toLowerCase(java.util.Locale.ROOT) + " paste";
		if (short_ != null && s.pasteCarried[short_.ordinal()] > 0 || short_ == null && sum(s.pasteCarried) > 0)
		{
			return Step.items("Deposit", "Put your paste in the hopper", "The orders need more " + what,
				Paste.MOX.item(), Paste.AGA.item(), Paste.LYE.item()).withTarget(Step.Target.HOPPER);
		}
		if (short_ != null && s.herbPaste[short_.ordinal()] > 0 || short_ == null && sum(s.herbPaste) > 0)
		{
			return Step.of("Refine", "Refine your herbs", "The orders need more " + what + "; then put it in the hopper",
				Step.Target.REFINER);
		}
		return Step.of("Bank", "Bank for herbs that make " + what, "The hopper cannot make these orders", Step.Target.BANK);
	}

	/** The paste the open orders are shortest of, against what they need, or null. */
	public static Paste shortest(Snapshot s)
	{
		Paste worst = null;
		int worstGap = 0;
		for (Paste x : Paste.values())
		{
			int need = 0;
			for (Order o : s.orders)
			{
				if (o != null && !o.done)
				{
					need = Math.max(need, o.potion.paste(x));
				}
			}
			int gap = need - s.hopper[x.ordinal()];
			if (gap > worstGap)
			{
				worstGap = gap;
				worst = x;
			}
		}
		return worst;
	}

	/**
	 * The potion a digweed is worth most on: a mixalot when one is carried for an order; below 81,
	 * the carried ordered potion that gives the most experience; never one of the single-paste
	 * three (the wiki: they pay least per paste). Null when none is carried yet.
	 */
	public static Potion digweedTarget(Snapshot s, List<Integer> planned)
	{
		Potion best = null;
		for (int i : planned)
		{
			Potion p = s.orders[i].potion;
			int k = p.ordinal();
			if (s.bases[k] + s.finished[k] <= 0 || p.single() || s.level >= Mixology.ALL_RECIPES && p != Potion.MAL)
			{
				continue;
			}
			if (best == null || p.xp() > best.xp())
			{
				best = p;
			}
		}
		return best;
	}

	/** The loop stage a step belongs to, or -1. */
	public static int stage(Step step)
	{
		switch (step.getTarget())
		{
			case HOPPER:
			case REFINER:
			case BANK:
				return 0;
			case LEVER:
			case MIXING_VESSEL:
				return 1;
			case AGITATOR:
			case RETORT:
			case ALEMBIC:
				return 2;
			case CONVEYOR:
				return 3;
			default:
				return -1;
		}
	}

	/** "M M A", the potion's paste in lever order. */
	public static String recipe(Potion p)
	{
		StringBuilder b = new StringBuilder();
		for (char c : p.code().toCharArray())
		{
			if (b.length() > 0)
			{
				b.append(' ');
			}
			b.append(ofLetter(c).getTitle());
		}
		return b.toString();
	}

	static Paste ofLetter(char c)
	{
		for (Paste x : Paste.values())
		{
			if (x.letter() == c)
			{
				return x;
			}
		}
		throw new IllegalArgumentException(String.valueOf(c));
	}

	private static String lower(Potion p)
	{
		return p.getTitle().toLowerCase(java.util.Locale.ROOT);
	}

	private static int sum(int[] a)
	{
		int n = 0;
		for (int x : a)
		{
			n += x;
		}
		return n;
	}
}
