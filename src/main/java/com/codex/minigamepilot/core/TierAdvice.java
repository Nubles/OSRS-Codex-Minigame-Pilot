package com.codex.minigamepilot.core;

/**
 * Which contract tier to ask for, from your Construction level and the planks you have. The OSRS
 * wiki's Mahogany Homes page: each tier needs a level (1, 20, 50, 70) and takes its own plank
 * (plank, oak, teak, mahogany); contracts cannot be cancelled, so taking one you cannot finish
 * means fetching planks before anything else. The best tier your level reaches is the answer
 * when its planks cover a contract; when they do not and a lower tier's do, that one is.
 *
 * <p>A contract's planks are what your own contracts of that tier took on average, once you
 * have finished one; before that the most any contract can take (every piece in the largest
 * house), so "enough" is never a guess in your favour.</p>
 */
public final class TierAdvice
{
	/** What to ask for, and why. */
	public static final class Advice
	{
		private final Contract.Tier tier;
		private final long covers;
		private final String why;

		Advice(Contract.Tier tier, long covers, String why)
		{
			this.tier = tier;
			this.covers = covers;
			this.why = why;
		}

		public Contract.Tier getTier()
		{
			return tier;
		}

		/** Contracts the tier's planks cover, or -1 with no bank seen. */
		public long getCovers()
		{
			return covers;
		}

		/** Why this tier, such as "Your teak planks cover about 9; not one with your mahogany", or null with no bank seen. */
		public String getWhy()
		{
			return why;
		}
	}

	private TierAdvice()
	{
	}

	/** Planks one contract of a tier takes: your own average, else the most any contract can. */
	public static double planksPer(Contract.Tier tier, ContractBook book)
	{
		ContractBook.Tally t = book == null ? null : book.tally(tier);
		return t != null && t.getContracts() > 0 && t.planksEach() > 0 ? t.planksEach() : Home.mostPlanks();
	}

	/**
	 * @param planks each tier's planks you have (bank, carried and plank sack), by tier ordinal, or null with no bank seen
	 */
	public static Advice advise(int constructionLevel, long[] planks, ContractBook book)
	{
		Contract.Tier top = Contract.Tier.forLevel(constructionLevel);
		if (planks == null || planks.length < Contract.Tier.values().length)
		{
			return new Advice(top, -1, null);
		}
		long topCovers = covers(top, planks, book);
		if (topCovers >= 1)
		{
			return new Advice(top, topCovers, "Your " + name(top) + " cover " + about(top, book) + topCovers
				+ (topCovers == 1 ? " contract" : " contracts"));
		}
		for (int i = top.ordinal() - 1; i >= 0; i--)
		{
			Contract.Tier t = Contract.Tier.values()[i];
			long c = covers(t, planks, book);
			if (c >= 1)
			{
				return new Advice(t, c, "Not enough " + name(top) + " for " + article(top) + " contract; your " + name(t) + " cover "
					+ about(t, book) + c + (c == 1 ? " contract" : " contracts"));
			}
		}
		long need = (long) Math.ceil(planksPer(top, book)) - planks[top.ordinal()];
		return new Advice(top, 0, "Bring " + top.planks((int) Math.max(1, need)) + " more: " + per(top, book));
	}

	static long covers(Contract.Tier t, long[] planks, ContractBook book)
	{
		return (long) Math.floor(Math.max(0, planks[t.ordinal()]) / planksPer(t, book));
	}

	private static boolean learned(Contract.Tier t, ContractBook book)
	{
		ContractBook.Tally tally = book == null ? null : book.tally(t);
		return tally != null && tally.getContracts() > 0 && tally.planksEach() > 0;
	}

	private static String about(Contract.Tier t, ContractBook book)
	{
		return learned(t, book) ? "about " : "at least ";
	}

	private static String per(Contract.Tier t, ContractBook book)
	{
		return learned(t, book) ? "your " + t.getName().toLowerCase(java.util.Locale.ENGLISH) + " contracts take about "
			+ Math.round(planksPer(t, book)) : "a contract can take up to " + Home.mostPlanks();
	}

	static String name(Contract.Tier t)
	{
		return t.planks(2).substring(2);
	}

	/** "a" or "an" before a tier's name. */
	public static String article(Contract.Tier t)
	{
		return t == Contract.Tier.ADEPT || t == Contract.Tier.EXPERT ? "an " + t.getName().toLowerCase(java.util.Locale.ENGLISH)
			: "a " + t.getName().toLowerCase(java.util.Locale.ENGLISH);
	}
}
