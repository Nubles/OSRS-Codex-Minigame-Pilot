package com.codex.minigamepilot.core;

import java.util.Locale;

/**
 * The one next step at the Wintertodt: chop bruma roots, fletch them into kindling, feed the
 * brazier, light or fix it, heal its pyromancer, warm up before the cold takes you, and search
 * the reward cart after.
 *
 * <p>Plain Java over a snapshot of the game, so every turn of it can be tested. The facts behind
 * it (OSRS wiki, Wintertodt; Brazier; Pyromancer; Guide:Efficient Wintertodt) are in
 * {@link Wintertodt}; how they decide the step:
 * <ul>
 * <li>a round pays only when you feed, light, fix and heal (roots 10 a root, kindling 25, light
 * 25, fix 25, heal 75); chopping and fletching pay nothing until it burns;</li>
 * <li>"Dropping to 0% warmth will cause instant death", and food restores 35%, a rejuvenation
 * dose 30%: warming up comes first, at the warmth you choose in the settings;</li>
 * <li>"If the braziers are unlit, they cannot be lit until the Pyromancer is healed", so heal
 * before lighting; a broken brazier is fixed before it can be lit;</li>
 * <li>near the round's end, "run to the brazier when the number of logs is two less than the
 * remaining energy", so nothing you hold is left unburnt;</li>
 * <li>the usual loop is a full pack of roots, fletched into kindling (25 a piece, not 10) unless
 * you have turned fletching off, then fed.</li>
 * </ul>
 */
public final class WintertodtCycle
{
	/** Stages of the loop as the panel shows them. */
	public static final String[] LOOP = {"Chop", "Fletch", "Feed"};

	/** A brazier's state, read from its object. */
	public enum Brazier
	{
		NONE,
		UNLIT,
		LIT,
		BROKEN
	}

	/** What the Pilot read this tick. */
	public static final class Snapshot
	{
		/** In the prison. */
		public boolean inPrison = true;
		/** A round is under way: the HUD shows the energy, not the countdown. */
		public boolean round = true;
		/** Seconds to the next round, or -1. */
		public int returnsIn = -1;
		/** Wintertodt's energy, 0 to 100, or -1. */
		public int energy = -1;
		/** Your warmth, 0 to 100, or -1. */
		public int warmth = -1;
		/** Your points this round, or -1. */
		public int points = -1;
		public int firemaking = 99;
		public int roots;
		public int kindling;
		public int free = 28;
		public boolean axe = true;
		public boolean knife = true;
		public boolean lighter = true;
		public boolean hammer = true;
		/** Rejuvenation potion doses carried. */
		public int doses;
		/** Your brazier, the nearest. */
		public Brazier brazier = Brazier.LIT;
		/** Its pyromancer is incapacitated. */
		public boolean pyroDown;
		/** Some brazier in sight burns, for feeding when yours does not. */
		public boolean anyLit = true;
		/** The "Fletch kindling" setting. */
		public boolean fletch = true;
		/** The "Warm up at" setting, a percentage; 0 turns it off. */
		public int eatAt = 40;
		/** Out of the prison: the reward cart is in sight, and what the last round earned there, or 0. */
		public boolean cartInSight;
		public int owed;
		/** The last round's points, for the cart's line. */
		public int lastPoints;
	}

	private WintertodtCycle()
	{
	}

	/** Points the roots and kindling you hold give when fed. */
	public static int held(Snapshot s)
	{
		return s.roots * Wintertodt.ROOT_POINTS + s.kindling * Wintertodt.KINDLING_POINTS;
	}

	/** The step, or null when there is nothing to do here (out of the prison, nothing owed). */
	public static Step next(Snapshot s)
	{
		if (!s.inPrison)
		{
			if (s.owed > 0 && s.cartInSight)
			{
				return Step.of("Search", "Search the reward cart", cart(s), Step.Target.REWARD_CART);
			}
			return null;
		}
		if (s.firemaking < Wintertodt.FIREMAKING)
		{
			return Step.waiting("Train Firemaking to " + Wintertodt.FIREMAKING,
				"The Wintertodt asks " + Wintertodt.FIREMAKING + " Firemaking to fight; you have " + s.firemaking, Step.Target.NONE);
		}
		if (s.round && s.warmth >= 0 && s.eatAt > 0 && s.warmth <= s.eatAt)
		{
			String why = "Warmth " + s.warmth + "%: food 4+ gives " + Wintertodt.FOOD_WARMTH + "%"
				+ (s.doses > 0 ? ", a dose " + Wintertodt.POTION_WARMTH + "%" : "");
			return Step.items("Eat", "Warm up: eat or drink", why, Wintertodt.POTIONS);
		}
		if (!s.round)
		{
			return between(s);
		}
		int items = s.roots + s.kindling;
		if (s.brazier == Brazier.BROKEN && s.hammer)
		{
			return Step.of("Fix", "Fix the brazier", "+" + Wintertodt.FIX_POINTS + " points, and it can burn again" + points(s), Step.Target.BRAZIER);
		}
		if (s.pyroDown && s.doses > 0)
		{
			return Step.of("Help", "Heal the Pyromancer", "+" + Wintertodt.HEAL_POINTS + " points for a dose · her brazier cannot be lit until she is up",
				Step.Target.PYROMANCER);
		}
		if (s.brazier == Brazier.UNLIT && !s.pyroDown && s.lighter)
		{
			return Step.of("Light", "Light the brazier", "+" + Wintertodt.LIGHT_POINTS + " points, and it drains the Wintertodt" + points(s), Step.Target.BRAZIER);
		}
		boolean canFeed = s.brazier == Brazier.LIT || s.anyLit;
		// The round is ending: burn all you hold before it does.
		if (items > 0 && canFeed && s.energy >= 0 && items >= s.energy - Wintertodt.FEED_MARGIN)
		{
			return feed(s, "Energy " + s.energy + "%: burn your " + items + " before the round ends · +" + held(s));
		}
		boolean fletching = s.fletch && s.knife;
		if (fletching && s.roots > 0 && (s.free == 0 || s.kindling > 0))
		{
			return Step.items("Fletch", "Fletch the roots into kindling",
				s.roots + " to go: kindling burns for " + Wintertodt.KINDLING_POINTS + ", a root " + Wintertodt.ROOT_POINTS + points(s),
				Wintertodt.ROOT);
		}
		if (items > 0 && (s.free == 0 || s.kindling > 0 || !s.axe))
		{
			if (canFeed)
			{
				return feed(s, items + " to burn · +" + held(s) + " points" + points(s));
			}
			return Step.waiting("Wait for a brazier to burn", "Every brazier is out or broken" + (s.lighter ? "" : ": take a tinderbox to light one"),
				Step.Target.BRAZIER);
		}
		if (!s.axe)
		{
			return Step.of("Take", "Take an axe", "Free from the crate in the prison; roots need one", Step.Target.TOOL_CRATE);
		}
		return Step.of("Chop", "Chop bruma roots", (s.free == 1 ? "1 free slot" : s.free + " free slots") + (items > 0 ? " · " + items + " held" : "") + points(s),
			Step.Target.ROOTS);
	}

	/** Between rounds: the tools for the next, then wait by your brazier. */
	private static Step between(Snapshot s)
	{
		if (!s.lighter)
		{
			return Step.of("Take", "Take a tinderbox", "Free from the crate; a brazier is lit with a tinderbox or bruma torch", Step.Target.TOOL_CRATE);
		}
		if (!s.axe)
		{
			return Step.of("Take", "Take an axe", "Free from the crate; roots need one", Step.Target.TOOL_CRATE);
		}
		if (s.fletch && !s.knife)
		{
			return Step.of("Take", "Take a knife", "Free from the crate; kindling burns for " + Wintertodt.KINDLING_POINTS + ", a root " + Wintertodt.ROOT_POINTS,
				Step.Target.TOOL_CRATE);
		}
		if (!s.hammer)
		{
			return Step.of("Take", "Take a hammer", "Free from the crate; a broken brazier is fixed with one (+" + Wintertodt.FIX_POINTS + ")",
				Step.Target.TOOL_CRATE);
		}
		String when = s.returnsIn >= 0 ? "Back in " + s.returnsIn / 60 + ":" + String.format(Locale.ROOT, "%02d", s.returnsIn % 60) : "Back in a minute";
		return Step.waiting("Wait by your brazier", when + " · light it as the round starts (+" + Wintertodt.LIGHT_POINTS + ")", Step.Target.BRAZIER);
	}

	private static Step feed(Snapshot s, String why)
	{
		return Step.of("Feed", "Feed the brazier", why, Step.Target.BRAZIER);
	}

	/** " · 320 points, 180 to 500" or " · 1,240 points, 3 rewards" for the end of a detail line. */
	static String points(Snapshot s)
	{
		if (s.points < 0)
		{
			return "";
		}
		if (s.points < Wintertodt.MIN_POINTS)
		{
			return " · " + (Wintertodt.MIN_POINTS - s.points) + " to " + Wintertodt.MIN_POINTS;
		}
		int sure = Wintertodt.sureRolls(s.points);
		return " · " + sure + " rewards so far";
	}

	private static String cart(Snapshot s)
	{
		int extra = (int) Math.round(Wintertodt.extraChance(s.lastPoints) * 100);
		return s.owed + (s.owed == 1 ? " reward" : " rewards") + " owed" + (extra > 0 ? ", +" + extra + "% of one more" : "")
			+ " · Big-search takes " + Wintertodt.BIG_SEARCH + " a click";
	}

	/** Where a step sits in the loop: 0 chop, 1 fletch, 2 feed, or -1 for one that breaks in. */
	public static int stage(Step step)
	{
		if (step == null)
		{
			return -1;
		}
		switch (step.getTarget())
		{
			case ROOTS:
				return 0;
			case ITEM:
				return "Fletch".equals(step.getVerb()) ? 1 : -1;
			case BRAZIER:
				return "Feed".equals(step.getVerb()) ? 2 : -1;
			default:
				return -1;
		}
	}
}
