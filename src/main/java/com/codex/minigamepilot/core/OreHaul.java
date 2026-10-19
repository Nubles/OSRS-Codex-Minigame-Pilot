package com.codex.minigamepilot.core;

import java.util.function.IntUnaryOperator;

/**
 * The ore the Motherlode Mine's sack has paid out this session, and what it is worth. Ore is only
 * counted on a tick the sack goes down, when what turns up in your pack can only have come out
 * of it: ore taken from the bank or picked up elsewhere is left out.
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

	/**
	 * One tick.
	 *
	 * @param carried how many of each of {@link #ORES} you carry now, in that order
	 * @param sackPaidOut whether the sack went down since the last tick
	 */
	public void observe(int[] carried, boolean sackPaidOut)
	{
		if (sackPaidOut)
		{
			window = 2;
		}
		for (int i = 0; i < ORES.length; i++)
		{
			int now = i < carried.length ? Math.max(0, carried[i]) : 0;
			if (seen && window > 0 && now > last[i])
			{
				haul[i] += now - last[i];
			}
			last[i] = now;
		}
		window = Math.max(0, window - 1);
		seen = true;
	}

	/** After leaving the mine: what you carry then is no guide to what comes next. */
	public void forget()
	{
		seen = false;
		window = 0;
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
