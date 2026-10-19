package com.codex.minigamepilot.core;

import java.util.function.IntUnaryOperator;

/**
 * The ore the Motherlode Mine's sack has paid out this session, and what it is worth. Ore is only
 * counted on a tick the sack goes down, when what turns up in your pack can only have come out
 * of it: ore taken from the bank or picked up elsewhere is left out.
 *
 * <p>With an open coal bag, the sack's coal goes straight into the bag and never shows in the
 * pack. The sack says how many pieces it gave, so whatever of that did not land in the pack
 * (ore or nuggets) went into the bag as coal.</p>
 */
public final class OreHaul
{
	/** What the sack pays out: coal and the four ores, in that order. */
	public static final int[] ORES = {453, 444, 447, 449, 451};

	private final long[] haul = new long[ORES.length];
	private final int[] last = new int[ORES.length];
	private boolean seen;
	/** Ticks left in which a pack that grows is the sack paying out. */
	private int window;
	/** Pieces the sack gave in the payout being watched, and how many of them landed in the pack. */
	private int paid;
	private int landed;
	private int lastNuggets;
	/** Whether an open coal bag was carried when the sack paid out. */
	private boolean bagOpen;

	/**
	 * One tick, without a coal bag.
	 *
	 * @param carried how many of each of {@link #ORES} you carry now, in that order
	 * @param sackPaidOut whether the sack went down since the last tick
	 */
	public void observe(int[] carried, boolean sackPaidOut)
	{
		observe(carried, 0, sackPaidOut ? 1 : 0, false);
	}

	/**
	 * One tick.
	 *
	 * @param carried how many of each of {@link #ORES} you carry now, in that order
	 * @param nuggets golden nuggets carried now
	 * @param sackDrop how many pieces the sack went down by since the last tick
	 * @param openCoalBag whether an open coal bag is carried, which takes the sack's coal
	 */
	public void observe(int[] carried, int nuggets, int sackDrop, boolean openCoalBag)
	{
		if (sackDrop > 0)
		{
			if (window == 0)
			{
				paid = 0;
				landed = 0;
				bagOpen = false;
			}
			window = 2;
			// Only a payout seen from a known pack can say what went into the bag.
			paid += seen ? sackDrop : 0;
			bagOpen |= openCoalBag;
		}
		for (int i = 0; i < ORES.length; i++)
		{
			int now = i < carried.length ? Math.max(0, carried[i]) : 0;
			if (seen && window > 0 && now > last[i])
			{
				haul[i] += now - last[i];
				landed += now - last[i];
			}
			last[i] = now;
		}
		int n = Math.max(0, nuggets);
		if (seen && window > 0 && n > lastNuggets)
		{
			landed += n - lastNuggets;
		}
		lastNuggets = n;
		if (window > 0 && --window == 0)
		{
			if (bagOpen && paid > landed)
			{
				// What the sack gave but the pack never showed went into the coal bag.
				haul[0] += paid - landed;
			}
			paid = 0;
			landed = 0;
			bagOpen = false;
		}
		seen = true;
	}

	/** After leaving the mine: what you carry then is no guide to what comes next. */
	public void forget()
	{
		seen = false;
		window = 0;
		paid = 0;
		landed = 0;
		bagOpen = false;
	}

	/** A new session: the haul starts again from nothing. */
	public void clear()
	{
		java.util.Arrays.fill(haul, 0);
	}

	public long count()
	{
		long n = 0;
		for (long h : haul)
		{
			n += h;
		}
		return n;
	}

	/** The haul at today's Grand Exchange prices; ore without a price yet counts nothing. */
	public long worth(IntUnaryOperator price)
	{
		long coins = 0;
		for (int i = 0; i < ORES.length; i++)
		{
			coins += haul[i] * Math.max(0, price.applyAsInt(ORES[i]));
		}
		return coins;
	}
}
