package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * Turns a goal chosen in the settings into a {@link Goal}: which rewards it covers, what you
 * already own, what you hold, and your pace. "Next reward" walks the usual order of buys and
 * picks the first set you have not finished; once every reward is owned it becomes the next
 * level.
 */
public final class Goals
{
	/** The goals each minigame offers, as the settings name them. */
	public enum Choice
	{
		NEXT_REWARD("Next reward"),
		PROSPECTOR_KIT("Prospector kit"),
		COAL_BAG("Coal bag"),
		GEM_BAG("Gem bag"),
		BIGGER_SACK("Bigger sack"),
		CARPENTERS_OUTFIT("Carpenter's outfit"),
		PLANK_SACK("Plank sack"),
		AMYS_SAW("Amy's saw"),
		HOSIDIUS_BLUEPRINTS("Hosidius blueprints"),
		EVERY_REWARD("Every reward"),
		NEXT_LEVEL("Next level"),
		OFF("Off");

		private final String label;

		Choice(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	/** Reward sets in the order players usually buy them. */
	static final List<List<Goal.Reward>> MOTHERLODE_ORDER = Arrays.asList(
		Arrays.asList(Goal.Reward.PROSPECTOR_HELMET, Goal.Reward.PROSPECTOR_JACKET, Goal.Reward.PROSPECTOR_LEGS, Goal.Reward.PROSPECTOR_BOOTS),
		Collections.singletonList(Goal.Reward.COAL_BAG),
		Collections.singletonList(Goal.Reward.GEM_BAG),
		Collections.singletonList(Goal.Reward.BIGGER_SACK));
	static final List<List<Goal.Reward>> HOMES_ORDER = Arrays.asList(
		Collections.singletonList(Goal.Reward.PLANK_SACK),
		Arrays.asList(Goal.Reward.CARPENTERS_HELMET, Goal.Reward.CARPENTERS_SHIRT, Goal.Reward.CARPENTERS_TROUSERS, Goal.Reward.CARPENTERS_BOOTS),
		Collections.singletonList(Goal.Reward.AMYS_SAW),
		Collections.singletonList(Goal.Reward.HOSIDIUS_BLUEPRINTS));

	/** What a goal is worked out from. */
	public static final class Input
	{
		/** Whether you have an item anywhere the Pilot can see: carried, worn or banked. */
		public IntPredicate have = id -> false;
		/** Rewards owned some other way, such as the bigger sack. */
		public Set<Goal.Reward> ownedOtherwise = EnumSet.noneOf(Goal.Reward.class);
		/** The currency you hold, or -1 when not known yet. */
		public long balance = -1;
		public double currencyPerHour;
		public double currencyPerRound;
		public String roundName;
		public String skill;
		public long xp;
		public double xpPerHour;
		public double xpPerRound;
		/** The level to reach for a level goal, 0 for the next one. */
		public int targetLevel;
	}

	private Goals()
	{
	}

	public static Goal motherlode(Choice choice, Input in)
	{
		return plan(choice, in, MOTHERLODE_ORDER, Goal.Currency.NUGGETS);
	}

	public static Goal homes(Choice choice, Input in)
	{
		return plan(choice, in, HOMES_ORDER, Goal.Currency.POINTS);
	}

	/** The Blast Furnace has no shop: its goal is always a level. */
	public static Goal furnace(Input in)
	{
		return level(in);
	}

	static Goal plan(Choice choice, Input in, List<List<Goal.Reward>> order, Goal.Currency currency)
	{
		if (choice == null || choice == Choice.OFF)
		{
			return null;
		}
		if (choice == Choice.NEXT_LEVEL)
		{
			return level(in);
		}
		List<Goal.Reward> set;
		String title;
		if (choice == Choice.NEXT_REWARD)
		{
			set = null;
			title = null;
			for (List<Goal.Reward> s : order)
			{
				if (!allOwned(s, in))
				{
					set = s;
					title = titleOf(s);
					break;
				}
			}
			if (set == null)
			{
				return level(in);
			}
		}
		else if (choice == Choice.EVERY_REWARD)
		{
			set = new ArrayList<>();
			for (List<Goal.Reward> s : order)
			{
				set.addAll(s);
			}
			title = "Every reward";
		}
		else
		{
			set = setFor(choice, order);
			if (set == null)
			{
				// A goal from the other minigame's shop: fall back to the next reward here.
				return plan(Choice.NEXT_REWARD, in, order, currency);
			}
			title = titleOf(set);
		}
		List<Goal.Piece> pieces = new ArrayList<>();
		for (Goal.Reward r : set)
		{
			pieces.add(new Goal.Piece(r, owned(r, in)));
		}
		return Goal.rewards(title, currency, pieces, Math.max(0, in.balance), in.currencyPerHour, in.currencyPerRound, in.roundName);
	}

	private static List<Goal.Reward> setFor(Choice choice, List<List<Goal.Reward>> order)
	{
		Goal.Reward key;
		switch (choice)
		{
			case PROSPECTOR_KIT:
				key = Goal.Reward.PROSPECTOR_HELMET;
				break;
			case COAL_BAG:
				key = Goal.Reward.COAL_BAG;
				break;
			case GEM_BAG:
				key = Goal.Reward.GEM_BAG;
				break;
			case BIGGER_SACK:
				key = Goal.Reward.BIGGER_SACK;
				break;
			case CARPENTERS_OUTFIT:
				key = Goal.Reward.CARPENTERS_HELMET;
				break;
			case PLANK_SACK:
				key = Goal.Reward.PLANK_SACK;
				break;
			case AMYS_SAW:
				key = Goal.Reward.AMYS_SAW;
				break;
			case HOSIDIUS_BLUEPRINTS:
				key = Goal.Reward.HOSIDIUS_BLUEPRINTS;
				break;
			default:
				return null;
		}
		for (List<Goal.Reward> s : order)
		{
			if (s.contains(key))
			{
				return s;
			}
		}
		return null;
	}

	static String titleOf(List<Goal.Reward> set)
	{
		if (set.contains(Goal.Reward.PROSPECTOR_HELMET))
		{
			return "Prospector kit";
		}
		if (set.contains(Goal.Reward.CARPENTERS_HELMET))
		{
			return "Carpenter's outfit";
		}
		return set.get(0).getName();
	}

	static boolean owned(Goal.Reward r, Input in)
	{
		return in.ownedOtherwise.contains(r) || r.ownedBy(in.have);
	}

	private static boolean allOwned(List<Goal.Reward> set, Input in)
	{
		for (Goal.Reward r : set)
		{
			if (!owned(r, in))
			{
				return false;
			}
		}
		return true;
	}

	static Goal level(Input in)
	{
		int now = levelFor(in.xp);
		if (now >= 99 && in.targetLevel <= 99)
		{
			return null;
		}
		int target = in.targetLevel > now ? Math.min(99, in.targetLevel) : Math.min(99, now + 1);
		// The bar measures from the start of the level you are on.
		long from = xpFor(now);
		return Goal.level(in.skill == null ? "Skill" : in.skill, target, from, in.xp, xpFor(target), in.xpPerHour, in.xpPerRound, in.roundName);
	}

	/** The experience a level takes, by the game's own table. */
	public static long xpFor(int level)
	{
		if (XP != null && level >= 1 && level < XP.length)
		{
			return XP[level];
		}
		return worked(level);
	}

	/** Experience at each level 1 to 99, worked out once: a level is looked up every tick. */
	private static final long[] XP = table();

	private static long[] table()
	{
		long[] t = new long[100];
		for (int level = 1; level < t.length; level++)
		{
			t[level] = worked(level);
		}
		return t;
	}

	/** The game's experience formula for a level. */
	private static long worked(int level)
	{
		long points = 0;
		for (int i = 1; i < level; i++)
		{
			points += (long) Math.floor(i + 300 * Math.pow(2, i / 7.0));
		}
		return points / 4;
	}

	/** The level some experience gives, up to 99. */
	public static int levelFor(long xp)
	{
		int level = 1;
		while (level < 99 && xpFor(level + 1) <= xp)
		{
			level++;
		}
		return level;
	}
}
