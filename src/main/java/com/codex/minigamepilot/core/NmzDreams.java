package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Follows dreams a tick at a time: a dream runs while you are in the arena, and ends when you
 * wake in the lobby or log out ("Logging out while inside the arena ends the dream"). Its points
 * are the game's own count of points this dream; its mode is the price Dominic's coffer paid on
 * entering ("Upon entering the dream, coins from Dominic's coffer are deducted") and the bosses
 * in it, hard or normal by RuneLite's names; its potions are the doses gone from what you carry.
 *
 * <p>Pure: the pilot feeds it what it reads, and tests feed it the same.
 */
public final class NmzDreams
{
	/** The coffer's price is looked for over the dream's first ticks. */
	static final int PRICE_WINDOW = 20;
	/** A boss leaving and the points rising this many ticks apart are one kill. */
	static final int KILL_WINDOW = 8;
	/** A dream shorter than this is a blink (a load), not kept. */
	static final int SHORTEST = 3;

	/** A boss in the arena this tick. */
	public static final class Seen
	{
		final int index;
		final String name;
		final char difficulty;

		public Seen(int index, String name, boolean hard)
		{
			this.index = index;
			this.name = Nmz.cleanName(name);
			this.difficulty = hard ? 'H' : 'N';
		}
	}

	private static final class Mark
	{
		final String name;
		final char difficulty;
		final int amount;
		final int tick;

		Mark(String name, char difficulty, int amount, int tick)
		{
			this.name = name;
			this.difficulty = difficulty;
			this.amount = amount;
			this.tick = tick;
		}
	}

	private final NmzKills kills;
	private int start = -1;
	private int base;
	private int peak;
	private int lastGained;
	private int cashBefore = -1;
	private int paid = -1;
	private int hard;
	private int normal;
	private final TreeSet<String> bosses = new TreeSet<>();
	private int[] last;
	private final int[] used = new int[Nmz.Potion.values().length];
	private Map<String, Seen> present = new HashMap<>();
	private final List<Mark> gone = new ArrayList<>();
	private final List<Mark> rises = new ArrayList<>();
	private int dreams;

	/** @param kills where each boss's points a kill are learnt, or null */
	public NmzDreams(NmzKills kills)
	{
		this.kills = kills;
	}

	/**
	 * One tick.
	 *
	 * @param points the game's points this dream, or -1 when not read
	 * @param cash Dominic's coffer in thousands, or -1 when not read
	 * @param doses doses carried of each potion, by ordinal
	 * @param seen the bosses in the arena now
	 * @return the dream that ended this tick, or null
	 */
	public NmzLog.Dream tick(int tick, boolean inArena, int points, int cash, int[] doses, List<Seen> seen, long now)
	{
		if (!inArena)
		{
			NmzLog.Dream d = start >= 0 ? end(tick, now, NmzLog.End.WOKE) : null;
			if (cash >= 0)
			{
				cashBefore = cash;
			}
			return d;
		}
		if (start < 0)
		{
			start = tick;
			base = Math.max(0, points);
			peak = base;
			lastGained = 0;
			paid = -1;
			hard = 0;
			normal = 0;
			bosses.clear();
			last = doses == null ? null : doses.clone();
			java.util.Arrays.fill(used, 0);
			present = new HashMap<>();
			gone.clear();
			rises.clear();
		}
		if (points >= 0)
		{
			if (points < peak)
			{
				// The count began again: what came before was the last dream's.
				base = 0;
				peak = points;
			}
			else
			{
				peak = points;
			}
		}
		if (paid < 0 && cashBefore >= 0 && cash >= 0 && cash < cashBefore && tick - start <= PRICE_WINDOW)
		{
			paid = cashBefore - cash;
		}
		if (doses != null)
		{
			if (last != null)
			{
				for (int i = 0; i < used.length && i < doses.length && i < last.length; i++)
				{
					if (doses[i] < last[i])
					{
						used[i] += last[i] - doses[i];
					}
				}
			}
			last = doses.clone();
		}
		Map<String, Seen> now2 = new HashMap<>();
		if (seen != null)
		{
			for (Seen s : seen)
			{
				now2.put(s.index + "|" + s.name, s);
				if (!s.name.isEmpty())
				{
					bosses.add(s.name);
				}
				if (s.difficulty == 'H')
				{
					hard++;
				}
				else
				{
					normal++;
				}
			}
		}
		for (Map.Entry<String, Seen> e : present.entrySet())
		{
			if (!now2.containsKey(e.getKey()))
			{
				gone.add(new Mark(e.getValue().name, e.getValue().difficulty, 0, tick));
			}
		}
		present = now2;
		int gained = gained();
		if (gained > lastGained)
		{
			rises.add(new Mark(null, '?', gained - lastGained, tick));
		}
		lastGained = gained;
		learn(tick);
		return null;
	}

	/** A boss gone and a rise in points close together, with no other to confuse them, are a kill. */
	private void learn(int tick)
	{
		drop(gone, tick);
		drop(rises, tick);
		if (gone.size() == 1 && rises.size() == 1)
		{
			Mark g = gone.remove(0);
			Mark r = rises.remove(0);
			if (kills != null && !g.name.isEmpty())
			{
				kills.kill(g.name, g.difficulty, r.amount);
			}
		}
	}

	private static void drop(List<Mark> marks, int tick)
	{
		for (Iterator<Mark> it = marks.iterator(); it.hasNext(); )
		{
			if (tick - it.next().tick > KILL_WINDOW)
			{
				it.remove();
			}
		}
	}

	private NmzLog.Dream end(int tick, long now, NmzLog.End how)
	{
		int ticks = tick - start;
		NmzLog.Dream d = null;
		if (ticks >= SHORTEST)
		{
			Nmz.Mode mode = paid > 0 ? Nmz.Mode.byCost(paid) : null;
			char diff = hard > normal ? 'H' : normal > hard ? 'N' : mode != null ? (Nmz.Mode.hardByCost(paid) ? 'H' : 'N') : '?';
			if (mode == null)
			{
				// Practice is free and gives no points: a dream with neither, the coffer read and unchanged, was practice.
				mode = paid < 0 && cashBefore >= 0 && gained() == 0 ? Nmz.Mode.PRACTICE : Nmz.Mode.UNKNOWN;
			}
			d = new NmzLog.Dream(now, ticks, gained(), mode, diff, mode == Nmz.Mode.UNKNOWN ? -1 : Math.max(0, paid), how,
				new ArrayList<>(bosses), used.clone());
			dreams++;
		}
		start = -1;
		gone.clear();
		rises.clear();
		present = new HashMap<>();
		return d;
	}

	/** Logged out or hopped: a dream under way ends. */
	public NmzLog.Dream logout(int tick, long now)
	{
		return start >= 0 ? end(tick, now, NmzLog.End.LOGOUT) : null;
	}

	/** Points this dream so far. */
	public int gained()
	{
		return start < 0 ? 0 : Math.max(0, peak - base);
	}

	public boolean open()
	{
		return start >= 0;
	}

	public int ticks(int tick)
	{
		return start >= 0 ? Math.max(0, tick - start) : 0;
	}

	/** Doses drunk of a potion this dream. */
	public int used(Nmz.Potion p)
	{
		return start >= 0 ? used[p.ordinal()] : 0;
	}

	/** Bosses seen this dream, by name. */
	public List<String> bosses()
	{
		return new ArrayList<>(bosses);
	}

	/** Hard, by the bosses seen so far, or by the price paid; '?' before either. */
	public char difficulty()
	{
		if (hard != normal)
		{
			return hard > normal ? 'H' : 'N';
		}
		return paid > 0 && Nmz.Mode.byCost(paid) != null ? (Nmz.Mode.hardByCost(paid) ? 'H' : 'N') : '?';
	}

	/** The mode by the price paid on entering, or null when not seen. */
	public Nmz.Mode mode()
	{
		return paid > 0 ? Nmz.Mode.byCost(paid) : null;
	}

	/** The coffer as last read outside the arena, in thousands, or -1. */
	public int coffer()
	{
		return cashBefore;
	}

	/** Dreams ended since this tracker began. */
	public int dreams()
	{
		return dreams;
	}
}
