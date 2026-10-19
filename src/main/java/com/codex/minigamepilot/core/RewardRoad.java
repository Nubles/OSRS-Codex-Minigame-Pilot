package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A minigame's whole shop as one road, in the order players usually buy: each reward set a stop,
 * ticked when you own it, lit when what you hold already pays for it and every unowned stop
 * before it, and otherwise how far off it is in time or rounds at your own pace, counting the
 * stops before it first. A level a Codex Navigator goal needs can close the road as its last
 * stop. The goal card says how far off the next thing is; the road says how far off everything is.
 */
public final class RewardRoad
{
	/** One stop on the road. */
	public static final class Stop
	{
		private final String name;
		private final int icon;
		private final int level;
		private final long cost;
		private final long left;
		private final boolean owned;
		private final boolean affordable;
		private final long minutesAway;
		private final long roundsAway;
		private final String reason;
		private final Goal.Currency currency;
		private final String roundName;
		private final boolean known;
		private final String skill;

		Stop(String name, int icon, int level, long cost, long left, boolean owned, boolean affordable, long minutesAway, long roundsAway, String reason,
			Goal.Currency currency, String roundName, boolean known)
		{
			this(name, icon, level, cost, left, owned, affordable, minutesAway, roundsAway, reason, currency, roundName, known, null);
		}

		Stop(String name, int icon, int level, long cost, long left, boolean owned, boolean affordable, long minutesAway, long roundsAway, String reason,
			Goal.Currency currency, String roundName, boolean known, String skill)
		{
			this.skill = skill;
			this.known = known;
			this.currency = currency;
			this.roundName = roundName;
			this.name = name;
			this.icon = icon;
			this.level = level;
			this.cost = cost;
			this.left = left;
			this.owned = owned;
			this.affordable = affordable;
			this.minutesAway = minutesAway;
			this.roundsAway = roundsAway;
			this.reason = reason;
		}

		public String getName()
		{
			return name;
		}

		/** An item that shows the reward, or 0 when it has none (the bigger sack, a level). */
		public int getIcon()
		{
			return icon;
		}

		/** The level, for a level stop; 0 for a reward. */
		public int getLevel()
		{
			return level;
		}

		/** What the stop's unowned pieces cost, in the shop's currency (or experience for a level). */
		public long getCost()
		{
			return cost;
		}

		/** What is still to earn before this stop is yours, the stops before it first; 0 when it is. */
		public long getLeft()
		{
			return left;
		}

		public boolean isOwned()
		{
			return owned;
		}

		/** True when what you hold already pays for it and every unowned stop before it. */
		public boolean isAffordable()
		{
			return affordable;
		}

		/** Minutes at your pace until it is yours, or -1 with no pace yet. */
		public long getMinutesAway()
		{
			return minutesAway;
		}

		/** Rounds (contracts) until it is yours, or -1 when rounds do not apply. */
		public long getRoundsAway()
		{
			return roundsAway;
		}

		/** False when what you hold is not known yet, so only what the stop costs can be said. */
		public boolean isKnown()
		{
			return known;
		}

		/** The shop's currency, or null for a level, which is counted in experience. */
		public Goal.Currency getCurrency()
		{
			return currency;
		}

		/** What a round is called, such as "adept contract", or null. */
		public String getRoundName()
		{
			return roundName;
		}

		/** For a level stop, the skill and level, such as "Smithing 70", whatever the stop is called; otherwise its name. */
		public String getLevelName()
		{
			return level > 0 && skill != null ? skill + " " + level : name;
		}

		/** For a level stop, whose goal needs it, such as "Dragon Slayer II"; otherwise null. */
		public String getReason()
		{
			return reason;
		}
	}

	private RewardRoad()
	{
	}

	public static List<Stop> motherlode(Goals.Input in)
	{
		return of(Goals.MOTHERLODE_ORDER, in);
	}

	public static List<Stop> homes(Goals.Input in)
	{
		return of(Goals.HOMES_ORDER, in);
	}

	/**
	 * The Blast Furnace has no shop, so its road is the Smithing levels that open its bars (and
	 * the level that ends the foreman's fee), from the last one you have reached onward, timed
	 * at your experience an hour.
	 */
	public static List<Stop> furnace(Goals.Input in)
	{
		if (in == null)
		{
			return Collections.emptyList();
		}
		int now = Goals.levelFor(in.xp);
		List<Stop> all = new ArrayList<>();
		boolean foreman = false;
		for (BarPick.Facts f : BarPick.Facts.values())
		{
			if (!foreman && f.level > BarPick.FOREMAN_FREE_LEVEL)
			{
				all.add(level("No foreman fee", 0, "Smithing", BarPick.FOREMAN_FREE_LEVEL, null, in));
				foreman = true;
			}
			all.add(level(f.bar.getName() + " bars", f.bar.getBar(), "Smithing", f.level, null, in));
		}
		// Only the last level reached, then everything ahead of it.
		int from = 0;
		for (int i = 0; i < all.size(); i++)
		{
			if (all.get(i).getLevel() <= now)
			{
				from = i;
			}
		}
		return new ArrayList<>(all.subList(from, all.size()));
	}

	private static Stop level(String name, int icon, String skill, int level, String reason, Goals.Input in)
	{
		boolean reached = Goals.levelFor(in.xp) >= level;
		long left = reached ? 0 : Math.max(0, Goals.xpFor(level) - in.xp);
		return new Stop(name, icon, level, left, left, reached, false, reached ? 0 : minutes(left, in.xpPerHour),
			reached ? 0 : rounds(left, in.xpPerRound), reason, null, in.roundName, true, skill);
	}

	/**
	 * @param in what you own and hold, and your pace; a balance below 0 means it is not known,
	 *     and then nothing is lit or timed, only what each stop costs
	 */
	static List<Stop> of(List<List<Goal.Reward>> order, Goals.Input in)
	{
		if (in == null)
		{
			return Collections.emptyList();
		}
		List<Stop> road = new ArrayList<>();
		boolean known = in.balance >= 0;
		long spent = 0;
		for (List<Goal.Reward> set : order)
		{
			long cost = 0;
			boolean owned = true;
			int icon = 0;
			for (Goal.Reward r : set)
			{
				int[] items = r.getItems();
				if (icon == 0 && items.length > 0)
				{
					icon = items[0];
				}
				if (!Goals.owned(r, in))
				{
					owned = false;
					cost += r.getCost();
				}
			}
			String name = set.size() > 1 ? Goals.titleOf(set) : set.get(0).getName();
			if (owned)
			{
				road.add(new Stop(name, icon, 0, 0, 0, true, false, 0, -1, null, set.get(0).getCurrency(), in.roundName, known));
				continue;
			}
			spent += cost;
			long left = known ? Math.max(0, spent - in.balance) : spent;
			road.add(new Stop(name, icon, 0, cost, left, false, known && left == 0,
				known ? minutes(left, in.currencyPerHour) : -1, known ? rounds(left, in.currencyPerRound) : -1, null, set.get(0).getCurrency(), in.roundName, known));
		}
		return road;
	}

	/**
	 * Adds a level a Navigator goal needs as the road's last stop, timed at your experience an
	 * hour; nothing when that level is already reached.
	 *
	 * @param goal the goal that needs it, such as "Dragon Slayer II"
	 */
	public static List<Stop> withLevel(List<Stop> road, String skill, int level, String goal, Goals.Input in)
	{
		if (skill == null || level <= 1 || level > 99 || in == null || Goals.levelFor(in.xp) >= level)
		{
			return road;
		}
		List<Stop> out = new ArrayList<>(road);
		// A road of levels keeps its order, and a level already on it just gains the goal's name;
		// reward stops have no level, so on a shop's road the level comes last.
		for (int i = 0; i < out.size(); i++)
		{
			Stop s = out.get(i);
			if (s.getLevel() == level)
			{
				out.set(i, new Stop(s.name, s.icon, s.level, s.cost, s.left, s.owned, s.affordable, s.minutesAway, s.roundsAway, goal,
					s.currency, s.roundName, s.known, s.skill));
				return out;
			}
			if (s.getLevel() > level)
			{
				out.add(i, level(skill + " " + level, 0, skill, level, goal, in));
				return out;
			}
		}
		out.add(level(skill + " " + level, 0, skill, level, goal, in));
		return out;
	}

	/** Whether anything on the road is still to come. */
	public static boolean open(List<Stop> road)
	{
		for (Stop s : road)
		{
			if (!s.isOwned())
			{
				return true;
			}
		}
		return false;
	}

	private static long minutes(long left, double perHour)
	{
		if (left <= 0)
		{
			return 0;
		}
		return perHour > 0 ? (long) Math.ceil(left * 60.0 / perHour) : -1;
	}

	private static long rounds(long left, double perRound)
	{
		if (left <= 0)
		{
			return 0;
		}
		return perRound > 0 ? (long) Math.ceil(left / perRound) : -1;
	}
}
