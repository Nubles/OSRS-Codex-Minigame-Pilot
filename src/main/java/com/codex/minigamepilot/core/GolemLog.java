package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Your golems, kept per account: each golem's gift (and a Jeweller's chisel with it), the fur that
 * powered it when the pack showed it, the Crafting experience since the golem before, and the time
 * since it. Golems no more than {@link #LONGEST} apart make one session. From these come your golems
 * and Crafting experience an hour at your own pace, your gifts against the wiki's rates, and your
 * record: the best session and the quickest golem.
 */
public final class GolemLog
{
	/** Golems kept. */
	static final int KEEP = 2_000;
	/** Pace is judged over this many of your latest timed golems, so it follows how you craft now. */
	public static final int RECENT = 120;
	/** A gap longer than this starts a new session and is not timed (ticks: ten minutes). */
	public static final int LONGEST = 1_000;
	/** Crafting experience one golem's gap can hold, at most: a guard against damage. */
	public static final int XP_MAX = 20_000;
	/** A session needs this many golems to stand as a record. */
	public static final int RECORD_GOLEMS = 5;
	private static final String VERSION = "1";

	/** One golem. */
	public static final class Golem
	{
		public final long endedAt;
		/** Ticks since the golem before, or 0 when it began a session. */
		public final int ticks;
		/** Crafting experience since the golem before (carving, cores and the golem), or 0 when not seen. */
		public final int xp;
		/** The gem it left, or null when it left only a chisel. */
		public final GolemCrafting.Gift gem;
		/** It left a Jeweller's chisel. */
		public final boolean chisel;
		/** The fur it took, or null when not seen (a fur pouch). */
		public final GolemCrafting.Fur fur;

		public Golem(long endedAt, int ticks, int xp, GolemCrafting.Gift gem, boolean chisel, GolemCrafting.Fur fur)
		{
			this.endedAt = Math.max(0, endedAt);
			this.ticks = ticks > 0 && ticks <= LONGEST ? ticks : 0;
			this.xp = xp > 0 && xp <= XP_MAX ? xp : 0;
			this.gem = gem == null || !gem.gem() ? null : gem;
			this.chisel = chisel || gem == GolemCrafting.Gift.CHISEL;
			this.fur = fur;
		}

		public boolean timed()
		{
			return ticks > 0;
		}
	}

	/** Figures over your latest timed golems. */
	public static final class Pace
	{
		public int golems;
		public long ticks;
		public long xp;
		/** Of them, those whose experience was seen. */
		public int withXp;
		public long xpTicks;

		private double hours(long t)
		{
			return t * 0.6 / 3600.0;
		}

		/** -1 with none timed. */
		public double golemsPerHour()
		{
			return ticks <= 0 ? -1 : golems / hours(ticks);
		}

		public double xpPerHour()
		{
			return xpTicks <= 0 ? -1 : xp / hours(xpTicks);
		}

		/** Crafting experience a golem, cores and carving counted, or -1. */
		public double xpPerGolem()
		{
			return withXp == 0 ? -1 : xp / (double) withXp;
		}

		/** Average ticks a golem, or -1. */
		public double gap()
		{
			return golems == 0 ? -1 : ticks / (double) golems;
		}
	}

	/** A run of golems no more than {@link #LONGEST} apart. */
	public static final class Sitting
	{
		public long startedAt;
		public long endedAt;
		public int golems;
		/** Over its timed golems: their ticks and experience. */
		public int timed;
		public long ticks;
		public long xp;

		/** Golems an hour over the timed part, or -1. */
		public double golemsPerHour()
		{
			return ticks <= 0 ? -1 : timed / (ticks * 0.6 / 3600.0);
		}

		public double xpPerHour()
		{
			return ticks <= 0 ? -1 : xp / (ticks * 0.6 / 3600.0);
		}
	}

	private final List<Golem> golems = new ArrayList<>();

	public void add(Golem g)
	{
		if (g == null)
		{
			return;
		}
		golems.add(g);
		while (golems.size() > KEEP)
		{
			golems.remove(0);
		}
	}

	public List<Golem> all()
	{
		return Collections.unmodifiableList(golems);
	}

	public int count()
	{
		return golems.size();
	}

	public Golem last()
	{
		return golems.isEmpty() ? null : golems.get(golems.size() - 1);
	}

	/** Your latest timed golems' figures. */
	public Pace pace()
	{
		Pace p = new Pace();
		for (int i = golems.size() - 1; i >= 0 && p.golems < RECENT; i--)
		{
			Golem g = golems.get(i);
			if (!g.timed())
			{
				continue;
			}
			p.golems++;
			p.ticks += g.ticks;
			if (g.xp > 0)
			{
				p.withXp++;
				p.xp += g.xp;
				p.xpTicks += g.ticks;
			}
		}
		return p;
	}

	/** Each gift's count over every golem kept. */
	public int[] gifts()
	{
		int[] n = new int[GolemCrafting.Gift.values().length];
		for (Golem g : golems)
		{
			if (g.gem != null)
			{
				n[g.gem.ordinal()]++;
			}
			if (g.chisel)
			{
				n[GolemCrafting.Gift.CHISEL.ordinal()]++;
			}
		}
		return n;
	}

	/** Golems since the last Jeweller's chisel, or all of them when none came. */
	public int sinceChisel()
	{
		int n = 0;
		for (int i = golems.size() - 1; i >= 0; i--)
		{
			if (golems.get(i).chisel)
			{
				return n;
			}
			n++;
		}
		return n;
	}

	public boolean hadChisel()
	{
		return gifts()[GolemCrafting.Gift.CHISEL.ordinal()] > 0;
	}

	/** The fur of your latest golem whose fur was seen, or null. */
	public GolemCrafting.Fur lastFur()
	{
		for (int i = golems.size() - 1; i >= 0; i--)
		{
			if (golems.get(i).fur != null)
			{
				return golems.get(i).fur;
			}
		}
		return null;
	}

	/** Your sessions, oldest first. */
	public List<Sitting> sittings()
	{
		List<Sitting> out = new ArrayList<>();
		Sitting s = null;
		for (Golem g : golems)
		{
			if (s == null || !g.timed())
			{
				s = new Sitting();
				s.startedAt = g.endedAt;
				out.add(s);
			}
			s.golems++;
			s.endedAt = g.endedAt;
			if (g.timed())
			{
				s.timed++;
				s.ticks += g.ticks;
				s.xp += g.xp;
			}
		}
		return out;
	}

	/** Your best session of at least {@link #RECORD_GOLEMS} golems, by golems an hour, or null. */
	public Sitting bestSitting()
	{
		Sitting best = null;
		for (Sitting s : sittings())
		{
			if (s.golems >= RECORD_GOLEMS && s.ticks > 0 && (best == null || s.golemsPerHour() > best.golemsPerHour()))
			{
				best = s;
			}
		}
		return best;
	}

	/** Your most golems in one session, or 0. */
	public int longestSitting()
	{
		int best = 0;
		for (Sitting s : sittings())
		{
			best = Math.max(best, s.golems);
		}
		return best;
	}

	/** The quickest gap from one golem to the next, or -1. */
	public int quickest()
	{
		int best = -1;
		for (Golem g : golems)
		{
			if (g.timed() && (best < 0 || g.ticks < best))
			{
				best = g.ticks;
			}
		}
		return best;
	}

	/** Crafting experience your kept golems gave, as seen. */
	public long xp()
	{
		long xp = 0;
		for (Golem g : golems)
		{
			xp += g.xp;
		}
		return xp;
	}

	/** "1;endedAt,ticks,xp,gem,chisel,fur;..." with "-" for none. */
	public String encode()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (Golem g : golems)
		{
			b.append(';').append(g.endedAt).append(',').append(g.ticks).append(',').append(g.xp).append(',')
				.append(g.gem == null ? '-' : g.gem.code).append(',').append(g.chisel ? '1' : '0').append(',')
				.append(g.fur == null ? '-' : g.fur.code);
		}
		return b.toString();
	}

	/** Reads the save as far as it can; damaged golems are left out. Never null. */
	public static GolemLog decode(String text)
	{
		GolemLog out = new GolemLog();
		if (text == null)
		{
			return out;
		}
		String[] parts = text.split(";");
		if (parts.length == 0 || !VERSION.equals(parts[0].trim()))
		{
			return out;
		}
		for (int i = 1; i < parts.length; i++)
		{
			String[] f = parts[i].split(",", -1);
			if (f.length < 6)
			{
				continue;
			}
			try
			{
				long ended = Long.parseLong(f[0].trim());
				int ticks = Integer.parseInt(f[1].trim());
				int xp = Integer.parseInt(f[2].trim());
				String gem = f[3].trim();
				String chisel = f[4].trim();
				String fur = f[5].trim();
				if (ended < 0 || ticks < 0 || xp < 0 || gem.length() != 1 || fur.length() != 1
					|| !("0".equals(chisel) || "1".equals(chisel)))
				{
					continue;
				}
				GolemCrafting.Gift g = gem.charAt(0) == '-' ? null : GolemCrafting.Gift.byCode(gem.charAt(0));
				GolemCrafting.Fur u = fur.charAt(0) == '-' ? null : GolemCrafting.Fur.byCode(fur.charAt(0));
				boolean c = "1".equals(chisel);
				if ((g == null && gem.charAt(0) != '-') || (u == null && fur.charAt(0) != '-') || (g == null && !c)
					|| g == GolemCrafting.Gift.CHISEL)
				{
					continue;
				}
				out.add(new Golem(ended, ticks, xp, g, c, u));
			}
			catch (IllegalArgumentException e)
			{
				// Left out: a bad number.
			}
		}
		return out;
	}
}
