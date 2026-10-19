package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.List;

/**
 * A Mahogany Homes contract and what is left of it: which client, what tier, the state of each
 * hotspot, and so what to fetch, where to go and what to fix next.
 *
 * <p>Hotspot varbit values, as the game uses them: 0 not part of this job, 1 needs mending,
 * 2 mended, 3 needs taking out, 4 ready to build, 5 to 8 built at tiers one to four.
 */
public final class Contract
{
	public static final int STEEL_BAR = 2353;

	/** Contract tiers, the plank each uses and the Construction level that unlocks it. */
	public enum Tier
	{
		BEGINNER("Beginner", 1, 960, "plank"),
		NOVICE("Novice", 20, 8778, "oak plank"),
		ADEPT("Adept", 50, 8780, "teak plank"),
		EXPERT("Expert", 70, 8782, "mahogany plank");

		private final String name;
		private final int level;
		private final int plank;
		private final String plankName;

		Tier(String name, int level, int plank, String plankName)
		{
			this.name = name;
			this.level = level;
			this.plank = plank;
			this.plankName = plankName;
		}

		public String getName()
		{
			return name;
		}

		public int getLevel()
		{
			return level;
		}

		public int getPlank()
		{
			return plank;
		}

		public String planks(int count)
		{
			return count + " " + plankName + (count == 1 ? "" : "s");
		}

		/** Points a finished contract of this tier is worth. */
		public int points()
		{
			return ordinal() + 2;
		}

		/** The best tier a Construction level can take. */
		public static Tier forLevel(int level)
		{
			Tier best = BEGINNER;
			for (Tier t : values())
			{
				if (level >= t.level)
				{
					best = t;
				}
			}
			return best;
		}

		public static Tier byName(String name)
		{
			for (Tier t : values())
			{
				if (t.name.equalsIgnoreCase(name))
				{
					return t;
				}
			}
			return null;
		}
	}

	/** What the game shows this tick. */
	public static final class Snapshot
	{
		/** The eight hotspot varbits. */
		public int[] values = new int[Home.HOTSPOTS];
		/** Planks of this contract's kind carried, plank sack included. */
		public int planks;
		public int steelBars;
		/** Whether the player is inside the client's house, on any floor. */
		public boolean atHome;
		public int constructionLevel = 1;
	}

	private final Home home;
	private final Tier tier;

	public Contract(Home home, Tier tier)
	{
		this.home = home;
		this.tier = tier;
	}

	public Home getHome()
	{
		return home;
	}

	/** The tier, or null when it has not been seen yet. */
	public Tier getTier()
	{
		return tier;
	}

	/** Whether a hotspot value still needs work. */
	/**
	 * The contract saved as "home:tier" (the tier left off while not known), or null when there
	 * is none or it names no home.
	 */
	public static Contract decode(String saved)
	{
		if (saved == null || saved.isEmpty())
		{
			return null;
		}
		String[] parts = saved.split(":");
		// ":" alone splits to nothing at all.
		Home home = parts.length == 0 ? null : Home.byName(parts[0]);
		if (home == null)
		{
			return null;
		}
		Tier tier = parts.length > 1 ? Tier.byName(parts[1]) : null;
		return new Contract(home, tier);
	}

	public static boolean needsWork(int value)
	{
		return value == 1 || value == 3 || value == 4;
	}

	/** The word for what a hotspot needs: Repair, Remove or Build. */
	public static String verb(int value)
	{
		switch (value)
		{
			case 1:
				return "Repair";
			case 3:
				return "Remove";
			case 4:
				return "Build";
			default:
				return "Done";
		}
	}

	/** The tier the hotspots show once furniture is built, or null. Values 5 to 8 are tiers one to four. */
	public static Tier tierFromValues(int[] values)
	{
		int best = 0;
		for (int v : values)
		{
			best = Math.max(best, v);
		}
		return best >= 5 && best <= 8 ? Tier.values()[best - 5] : null;
	}

	/** Whether the hotspot varbits say anything about this job yet. */
	public static boolean known(int[] values)
	{
		for (int v : values)
		{
			if (v != 0)
			{
				return true;
			}
		}
		return false;
	}

	/** Hotspots still to do, by index. */
	public List<Integer> remaining(int[] values)
	{
		List<Integer> left = new ArrayList<>();
		for (int i = 0; i < home.hotspotCount() && i < values.length; i++)
		{
			if (needsWork(values[i]))
			{
				left.add(i);
			}
		}
		return left;
	}

	/** Planks the rest of the job takes. Taking furniture out costs nothing; building it does. */
	public int planksNeeded(int[] values)
	{
		int n = 0;
		for (int i : remaining(values))
		{
			Home.Kind kind = home.hotspot(i).getKind();
			if (!kind.isSteel())
			{
				n += kind.getAmount();
			}
		}
		return n;
	}

	public int steelNeeded(int[] values)
	{
		int n = 0;
		for (int i : remaining(values))
		{
			if (home.hotspot(i).getKind().isSteel())
			{
				n++;
			}
		}
		return n;
	}

	/** The next step for this contract, or for getting one when there is none. */
	public static Step next(Contract contract, Snapshot s)
	{
		if (contract == null)
		{
			Tier best = Tier.forLevel(s.constructionLevel);
			return Step.of("Contract", "Get a Mahogany Homes contract",
				"Ask Amy in Falador, Marlo in Varrock, Ellie in East Ardougne or Angelo in Hosidius for "
					+ (best == Tier.EXPERT || best == Tier.ADEPT ? "an " : "a ") + best.getName().toLowerCase() + " contract",
				Step.Target.CONTRACTOR);
		}
		return contract.next(s);
	}

	private Step next(Snapshot s)
	{
		Home h = home;
		boolean known = known(s.values);
		List<Integer> left = remaining(s.values);
		if (known && left.isEmpty())
		{
			return Step.of("Talk", "Tell " + h.getName() + " the work is done",
				"Worth " + (tier == null ? "your" : tier.points()) + " points" + (h.isClientUpstairs() ? ". " + h.getName() + " is upstairs" : ""),
				Step.Target.CLIENT);
		}

		if (tier != null && known)
		{
			int planks = planksNeeded(s.values);
			int steel = steelNeeded(s.values);
			int shortPlanks = Math.max(0, planks - s.planks);
			int shortSteel = Math.max(0, steel - s.steelBars);
			if (shortPlanks > 0 || shortSteel > 0)
			{
				String what = shortPlanks > 0 ? tier.planks(shortPlanks) : "";
				if (shortSteel > 0)
				{
					what += (what.isEmpty() ? "" : " and ") + shortSteel + (shortSteel == 1 ? " steel bar" : " steel bars");
				}
				int[] items = shortSteel > 0 && shortPlanks > 0 ? new int[]{tier.getPlank(), STEEL_BAR}
					: shortSteel > 0 ? new int[]{STEEL_BAR} : new int[]{tier.getPlank()};
				return Step.items("Fetch", "Bring " + what,
					"The job needs " + tier.planks(planks) + (steel > 0 ? " and " + steel + " steel" : "") + "; you have " + s.planks
						+ (steel > 0 ? " and " + s.steelBars : ""), items);
			}
		}

		if (!s.atHome)
		{
			return Step.of("Travel", "Go to " + h.getName() + "'s house",
				h.getCity().getName() + ", " + h.getWhere(), Step.Target.HOME);
		}
		if (!known)
		{
			return Step.of("Look", "Look over " + h.getName() + "'s furniture",
				"The pieces that need work light up as you get close", Step.Target.HOTSPOT);
		}
		int planks = planksNeeded(s.values);
		String rest = left.size() == 1 ? "Last piece" : left.size() + " pieces left";
		if (planks > 0 && tier != null)
		{
			rest += ", " + tier.planks(planks);
		}
		return Step.of("Fix", "Fix up the furniture", rest, Step.Target.HOTSPOT);
	}
}
