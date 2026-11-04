package com.codex.minigamepilot.core;

import com.codex.minigamepilot.core.Assault.Role;
import java.util.EnumMap;
import java.util.Map;

/**
 * Which role to play next for a reward, and how many games and how long it is at your own pace.
 *
 * <p>A reward costs its price in every role, so the role that needs the most games is the one
 * that holds you up; and every won game also gives the other three roles a few points ("80 points
 * for their role plus 5 points in each of the other 3 roles" for wave 10), so playing that role
 * first lets the others fill as you go. The plan plays one game at a time in the role with the
 * most games still to go, counting what a full game gives you in each role from your own games,
 * or, for a role you have not finished a game in, only the wiki's 80 and 5 for the queen's wave
 * (so its count is at most: waves 1 to 9 give more).
 */
public final class AssaultPlan
{
	/** Games planned at most, so a plan always ends. */
	static final int MOST = 2000;

	/** What the plan came to. */
	public static final class Result
	{
		/** The role to play next, or null when nothing is short. */
		public final Role next;
		/** Points still short in each role. */
		public final Map<Role, Integer> missing;
		/** Full games in each role, in the plan. */
		public final Map<Role, Integer> games;
		/** Full games in all. */
		public final int total;
		/** Hours at your pace, or -1 when a game's time is not known yet. */
		public final double hours;
		/** Some role's games are counted from the wiki's queen points only: the count is at most. */
		public final boolean bound;

		Result(Role next, Map<Role, Integer> missing, Map<Role, Integer> games, int total, double hours, boolean bound)
		{
			this.next = next;
			this.missing = missing;
			this.games = games;
			this.total = total;
			this.hours = hours;
			this.bound = bound;
		}

		/** Games in this role in the plan. */
		public int games(Role r)
		{
			Integer n = games.get(r);
			return n == null ? 0 : n;
		}

		public int shortOf(Role r)
		{
			Integer n = missing.get(r);
			return n == null ? 0 : n;
		}
	}

	private AssaultPlan()
	{
	}

	/**
	 * @param need points each role must reach, by role
	 * @param have points each role holds now
	 * @param rates what a full game in each role gives you, from your games; roles left out use the wiki's queen points
	 * @param typicalTicks ticks of a full game in any role, or 0 when none is timed
	 */
	public static Result plan(Map<Role, Integer> need, Map<Role, Integer> have, Map<Role, AssaultLog.Rate> rates, int typicalTicks)
	{
		Map<Role, Integer> shortBy = new EnumMap<>(Role.class);
		double[] left = new double[Role.values().length];
		for (Role r : Role.values())
		{
			int n = need.getOrDefault(r, 0);
			int h = have.getOrDefault(r, 0);
			int s = Math.max(0, n - h);
			shortBy.put(r, s);
			left[r.ordinal()] = s;
		}
		double[] own = new double[left.length];
		double[] side = new double[left.length];
		int[] ticks = new int[left.length];
		boolean[] wiki = new boolean[left.length];
		for (Role r : Role.values())
		{
			AssaultLog.Rate rate = rates == null ? null : rates.get(r);
			if (rate != null && rate.own > 0)
			{
				own[r.ordinal()] = rate.own;
				side[r.ordinal()] = Math.max(0, rate.side);
				ticks[r.ordinal()] = rate.ticks > 0 ? rate.ticks : typicalTicks;
			}
			else
			{
				own[r.ordinal()] = Assault.QUEEN_ROLE_POINTS;
				side[r.ordinal()] = Assault.QUEEN_OTHER_POINTS;
				ticks[r.ordinal()] = typicalTicks;
				wiki[r.ordinal()] = true;
			}
		}
		Map<Role, Integer> games = new EnumMap<>(Role.class);
		Role next = null;
		int total = 0;
		long allTicks = 0;
		boolean timed = true;
		boolean bound = false;
		while (total < MOST)
		{
			Role pick = null;
			double most = 0;
			for (Role r : Role.values())
			{
				double g = left[r.ordinal()] / own[r.ordinal()];
				if (left[r.ordinal()] > 0 && g > most)
				{
					most = g;
					pick = r;
				}
			}
			if (pick == null)
			{
				break;
			}
			if (next == null)
			{
				next = pick;
			}
			int p = pick.ordinal();
			for (int i = 0; i < left.length; i++)
			{
				left[i] -= i == p ? own[p] : side[p];
			}
			games.merge(pick, 1, Integer::sum);
			total++;
			allTicks += ticks[p];
			timed &= ticks[p] > 0;
			bound |= wiki[p];
		}
		double hours = timed && total > 0 ? allTicks * 0.6 / 3600 : total == 0 ? 0 : -1;
		return new Result(next, shortBy, games, total, hours, bound);
	}
}
