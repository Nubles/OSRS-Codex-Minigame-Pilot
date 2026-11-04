package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Follows golem crafting from what the game shows. A golem is done when its gift line comes ("As you
 * complete the golem it leaves a gift ... for you: 1 x Uncut sapphire", the line RuneLite's own loot
 * tracker reads); a Jeweller's chisel line within a tick of a gem's is the same golem. The Crafting
 * experience gained at the stations since the golem before is its experience (carving, cores and the
 * golem itself), and a fur leaving the pack in the same tick as a sunstone core is the fur that
 * powered it (a fur pouch hides it, and it is then not known).
 *
 * <p>It never times anything ahead: a golem's time is measured after it is done, from the golem
 * before. Pure: the pilot feeds it what it reads, and tests feed it the same.
 */
public final class GolemTracker
{
	/** A golem is settled this many ticks after its gift line, so experience in the same tick is counted. */
	static final int GRACE = 1;

	/** What is read in a tick. */
	public static final class Reading
	{
		/** A golem station, sunstone rock or the monolith is in the scene. */
		public boolean atStations;
		/** Crafting experience now. */
		public int crafting;
		/** Sunstone cores carried. */
		public int cores;
		/** Furs carried, by {@link GolemCrafting.Fur}. */
		public final int[] furs = new int[GolemCrafting.Fur.values().length];
	}

	private static final class Pending
	{
		final int tick;
		GolemCrafting.Gift gem;
		boolean chisel;

		Pending(int tick)
		{
			this.tick = tick;
		}
	}

	private final List<Pending> pending = new ArrayList<>();
	private Reading prev;
	private int gained;
	private GolemCrafting.Fur fur;
	private int lastGolem = -1;
	private int golems;
	private int chisels;

	/** A gift line in this tick. */
	public void gift(GolemCrafting.Gift g, int tick)
	{
		if (g == null)
		{
			return;
		}
		Pending p = pending.isEmpty() ? null : pending.get(pending.size() - 1);
		boolean near = p != null && tick - p.tick <= GRACE;
		if (near && g == GolemCrafting.Gift.CHISEL && !p.chisel)
		{
			p.chisel = true;
			return;
		}
		if (near && g.gem() && p.gem == null)
		{
			p.gem = g;
			return;
		}
		Pending n = new Pending(tick);
		if (g == GolemCrafting.Gift.CHISEL)
		{
			n.chisel = true;
		}
		else
		{
			n.gem = g;
		}
		pending.add(n);
	}

	/**
	 * One tick.
	 *
	 * @return the golems settled this tick, oldest first; never null
	 */
	public List<GolemLog.Golem> tick(int tick, Reading r, long now)
	{
		if (prev != null && r.atStations)
		{
			if (prev.crafting > 0 && r.crafting > prev.crafting)
			{
				gained += r.crafting - prev.crafting;
			}
			if (prev.cores - r.cores == 1)
			{
				for (GolemCrafting.Fur f : GolemCrafting.Fur.values())
				{
					if (prev.furs[f.ordinal()] - r.furs[f.ordinal()] == 1)
					{
						fur = f;
					}
				}
			}
		}
		prev = r;
		List<GolemLog.Golem> out = new ArrayList<>();
		while (!pending.isEmpty() && tick - pending.get(0).tick >= GRACE)
		{
			out.add(settle(pending.remove(0), now));
		}
		return out;
	}

	private GolemLog.Golem settle(Pending p, long now)
	{
		int ticks = lastGolem >= 0 ? p.tick - lastGolem : 0;
		boolean timed = ticks > 0 && ticks <= GolemLog.LONGEST;
		GolemLog.Golem g = new GolemLog.Golem(now, timed ? ticks : 0, timed ? gained : 0, p.gem, p.chisel, fur);
		lastGolem = p.tick;
		gained = 0;
		fur = null;
		golems++;
		chisels += p.chisel ? 1 : 0;
		return g;
	}

	/** Logged out or hopped: a gift already shown is kept, and the next golem begins a new session. */
	public List<GolemLog.Golem> logout(long now)
	{
		List<GolemLog.Golem> out = new ArrayList<>();
		while (!pending.isEmpty())
		{
			out.add(settle(pending.remove(0), now));
		}
		prev = null;
		lastGolem = -1;
		gained = 0;
		fur = null;
		return out;
	}

	/** Ticks since the last golem, or -1. Shown after the fact only, never as a cue. */
	public int sinceLast(int tick)
	{
		return lastGolem < 0 ? -1 : Math.max(0, tick - lastGolem);
	}

	/** Golems settled since this tracker began. */
	public int golems()
	{
		return golems;
	}

	public int chisels()
	{
		return chisels;
	}
}
