package com.codex.minigamepilot.core;

/**
 * Your Mahogany Homes points, from the line after each contract: "You have completed 120
 * contracts with a total of 470 points." Nobody has yet confirmed whether that total is what you
 * can spend or everything you have ever earned, so the book works it out as you play instead of
 * guessing:
 *
 * <ul>
 * <li>A total that goes down can only be a balance.</li>
 * <li>A total smaller than what the rewards you own cost can only be a balance, since every
 * reward is bought with points.</li>
 * <li>Buy a reward between two contracts and the next total tells: a balance drops by its cost,
 * a lifetime total just carries on climbing by a contract's few points.</li>
 * </ul>
 *
 * Until one of those settles it, what you can spend is taken as the lower of the two readings,
 * so the goal card never lights a reward you cannot afford.
 */
public final class Points
{
	/** What the total in the message means. */
	public enum Meaning
	{
		UNKNOWN,
		BALANCE,
		LIFETIME
	}

	/** The fewest and most points one contract pays, across every tier. */
	static final int MIN_PER_CONTRACT = 1;
	static final int MAX_PER_CONTRACT = 10;

	private long total = -1;
	private long contracts = -1;
	private Meaning meaning = Meaning.UNKNOWN;
	/** Points spent on rewards bought since the last message. */
	private long spentSince;

	/** True once a message has been read. */
	public boolean known()
	{
		return total >= 0;
	}

	public Meaning getMeaning()
	{
		return meaning;
	}

	/** The total from the last message, or -1. */
	public long getTotal()
	{
		return total;
	}

	/**
	 * The line after a contract.
	 *
	 * @param count the contracts it says you have completed
	 * @param newTotal the points total it gives
	 * @param ownedCost what every points reward you own cost, all bought with points
	 * @return points earned since the last message, or 0 when that cannot be told
	 */
	public long message(long count, long newTotal, long ownedCost)
	{
		if (total >= 0 && contracts >= 0 && count <= contracts)
		{
			// The same line again, or an older one: nothing new to learn from it.
			return 0;
		}
		long earned = 0;
		if (total >= 0 && contracts >= 0)
		{
			long k = count - contracts;
			long delta = newTotal - total;
			boolean lifetimeFits = fits(delta, k);
			boolean balanceFits = fits(delta + spentSince, k);
			if (delta < 0)
			{
				meaning = Meaning.BALANCE;
			}
			else if (spentSince > 0 && lifetimeFits != balanceFits)
			{
				meaning = lifetimeFits ? Meaning.LIFETIME : Meaning.BALANCE;
			}
			// Either way the contracts in between paid what the totals climbed, plus anything spent
			// from a balance; take the reading that fits a contract's pay.
			if (meaning == Meaning.BALANCE)
			{
				earned = delta + spentSince;
			}
			else if (meaning == Meaning.LIFETIME || lifetimeFits)
			{
				earned = Math.max(0, delta);
			}
			else
			{
				earned = balanceFits ? delta + spentSince : Math.max(0, delta);
			}
		}
		if (newTotal < ownedCost)
		{
			meaning = Meaning.BALANCE;
		}
		total = newTotal;
		contracts = count;
		spentSince = 0;
		// A drop with no purchase seen (one bought out of sight) leaves this contract's pay unknown.
		return Math.max(0, earned);
	}

	private static boolean fits(long delta, long contracts)
	{
		return delta >= MIN_PER_CONTRACT * contracts && delta <= MAX_PER_CONTRACT * contracts;
	}

	/** A points reward just bought (it turned up in your pack from nowhere you had it). */
	public void bought(long cost)
	{
		if (cost > 0)
		{
			spentSince += cost;
		}
	}

	/**
	 * What you can spend now, or -1 before the first message.
	 *
	 * @param ownedCost what every points reward you own cost
	 */
	public long balance(long ownedCost)
	{
		if (total < 0)
		{
			return -1;
		}
		long asBalance = total - spentSince;
		long asLifetime = total - ownedCost;
		switch (meaning)
		{
			case BALANCE:
				return Math.max(0, asBalance);
			case LIFETIME:
				return Math.max(0, asLifetime);
			default:
				return Math.max(0, Math.min(asBalance, asLifetime));
		}
	}

	/** "470:120:UNKNOWN:350", to save. */
	public String encode()
	{
		return total + ":" + contracts + ":" + meaning.name() + ":" + spentSince;
	}

	/**
	 * Reads a saved book. A bare number, as older versions saved, is taken as a total with
	 * nothing known about what it means.
	 */
	public static Points decode(String saved)
	{
		Points p = new Points();
		if (saved == null || saved.trim().isEmpty())
		{
			return p;
		}
		String[] parts = saved.trim().split(":");
		try
		{
			p.total = Long.parseLong(parts[0]);
			if (parts.length >= 4)
			{
				p.contracts = Long.parseLong(parts[1]);
				p.meaning = Meaning.valueOf(parts[2]);
				p.spentSince = Math.max(0, Long.parseLong(parts[3]));
			}
		}
		catch (IllegalArgumentException e)
		{
			return new Points();
		}
		return p;
	}
}
