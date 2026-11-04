package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Your Mastering Mixology deliveries, kept per account: the mox, aga and lye resin each paid (as
 * the game's own resin counts rose), the Herblore experience gained over the round, how many
 * orders it filled and how long the round took, from the delivery before it. From these the
 * panel says your resin and experience an hour, and how long a reward is at your own pace.
 */
public final class MixRounds
{
	/** Rounds kept: enough for a pace, small enough for a saved setting. */
	static final int KEEP = 100;
	/** A round longer than this had a break in it and is not timed (ticks: ten minutes). */
	public static final int LONGEST = 1000;
	private static final String VERSION = "1";

	/** One delivery. */
	public static final class Round
	{
		public final long endedAt;
		/** Ticks since the delivery before, or 0 when that was not seen or there was a break. */
		public final int ticks;
		public final int mox;
		public final int aga;
		public final int lye;
		public final int xp;
		/** Orders the delivery filled, 0 when not known. */
		public final int orders;

		public Round(long endedAt, int ticks, int mox, int aga, int lye, int xp, int orders)
		{
			this.endedAt = Math.max(0, endedAt);
			this.ticks = ticks > 0 && ticks <= LONGEST ? ticks : 0;
			this.mox = clamp(mox);
			this.aga = clamp(aga);
			this.lye = clamp(lye);
			this.xp = Math.max(0, Math.min(1_000_000, xp));
			this.orders = Math.max(0, Math.min(3, orders));
		}

		private static int clamp(int n)
		{
			return Math.max(0, Math.min(10_000, n));
		}

		public int resin()
		{
			return mox + aga + lye;
		}

		/** Resin of a kind, by {@link Mixology.Paste#ordinal()}. */
		public int resin(Mixology.Paste p)
		{
			return p == Mixology.Paste.MOX ? mox : p == Mixology.Paste.AGA ? aga : lye;
		}
	}

	private final List<Round> rounds = new ArrayList<>();

	public void add(Round r)
	{
		rounds.add(r);
		while (rounds.size() > KEEP)
		{
			rounds.remove(0);
		}
	}

	public List<Round> all()
	{
		return Collections.unmodifiableList(rounds);
	}

	public int count()
	{
		return rounds.size();
	}

	public Round last()
	{
		return rounds.isEmpty() ? null : rounds.get(rounds.size() - 1);
	}

	/** The timed round that paid the most resin a minute; null with none timed. */
	public Round best()
	{
		Round best = null;
		for (Round r : rounds)
		{
			if (r.ticks > 0 && (best == null || (long) r.resin() * best.ticks > (long) best.resin() * r.ticks))
			{
				best = r;
			}
		}
		return best;
	}

	/** Resin of a kind an hour over the timed rounds kept, or -1 with none. */
	public double perHour(Mixology.Paste p)
	{
		long ticks = 0;
		long sum = 0;
		for (Round r : rounds)
		{
			if (r.ticks > 0)
			{
				ticks += r.ticks;
				sum += r.resin(p);
			}
		}
		return ticks == 0 ? -1 : sum * 3600.0 / (ticks * 0.6);
	}

	/** All resin an hour over the timed rounds, or -1 with none. */
	public double resinPerHour()
	{
		double sum = 0;
		for (Mixology.Paste p : Mixology.Paste.values())
		{
			double h = perHour(p);
			if (h < 0)
			{
				return -1;
			}
			sum += h;
		}
		return sum;
	}

	/** Herblore experience an hour over the timed rounds, or -1 with none. */
	public double xpPerHour()
	{
		long ticks = 0;
		long sum = 0;
		for (Round r : rounds)
		{
			if (r.ticks > 0)
			{
				ticks += r.ticks;
				sum += r.xp;
			}
		}
		return ticks == 0 ? -1 : sum * 3600.0 / (ticks * 0.6);
	}

	/**
	 * Hours until resin of every kind reaches a reward's price at your pace, or -1 without a pace
	 * for a kind still short. 0 when you can already afford it.
	 */
	public double hoursTo(Mixology.Reward reward, int[] resin)
	{
		double hours = 0;
		for (Mixology.Paste p : Mixology.Paste.values())
		{
			int need = reward.cost(p) - resin[p.ordinal()];
			if (need <= 0)
			{
				continue;
			}
			double h = perHour(p);
			if (h <= 0)
			{
				return -1;
			}
			hours = Math.max(hours, need / h);
		}
		return hours;
	}

	/** "1;endedAt,ticks,mox,aga,lye,xp,orders;..." */
	public String encode()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (Round r : rounds)
		{
			b.append(';').append(r.endedAt).append(',').append(r.ticks).append(',').append(r.mox).append(',').append(r.aga)
				.append(',').append(r.lye).append(',').append(r.xp).append(',').append(r.orders);
		}
		return b.toString();
	}

	/** Reads a save as far as it can; damaged rounds are left out. Never null. */
	public static MixRounds decode(String text)
	{
		MixRounds out = new MixRounds();
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
			String[] f = parts[i].split(",");
			if (f.length < 7)
			{
				continue;
			}
			try
			{
				long ended = Long.parseLong(f[0].trim());
				int[] n = new int[6];
				boolean bad = ended < 0;
				for (int k = 0; k < 6; k++)
				{
					n[k] = Integer.parseInt(f[k + 1].trim());
					bad |= n[k] < 0;
				}
				if (!bad)
				{
					out.add(new Round(ended, n[0], n[1], n[2], n[3], n[4], n[5]));
				}
			}
			catch (NumberFormatException e)
			{
				// Left out.
			}
		}
		return out;
	}
}
