package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * What your Mahogany Homes contracts really cost, learned one finished contract at a time: the
 * Construction experience each one gave you, the planks and steel bars it took, and how long the
 * whole loop took, kept per tier. Priced at today's Grand Exchange prices, that answers the
 * question every Construction trainer asks: how many coins each experience point costs at each
 * tier, and how fast it comes.
 *
 * <p>Nothing is assumed about experience per plank: a tier is only ranked once you have finished
 * a contract of it, so carpenter's outfit bonuses and the like are counted as you earn them.</p>
 */
public final class ContractBook
{
	/** A loop longer than this (a break, a bank trip elsewhere) is not timed. */
	public static final int MAX_LOOP_SECONDS = 20 * 60;
	/** Experience one contract cannot plausibly give; anything above means other training mixed in. */
	static final int MAX_XP = 20_000;

	/** One tier's contracts so far. */
	public static final class Tally
	{
		int contracts;
		long xp;
		long planks;
		long steel;
		/** Contracts whose whole loop was timed, and the seconds they took. */
		int timed;
		long seconds;

		public int getContracts()
		{
			return contracts;
		}

		public double xpEach()
		{
			return contracts == 0 ? 0 : xp / (double) contracts;
		}

		public double planksEach()
		{
			return contracts == 0 ? 0 : planks / (double) contracts;
		}

		public double steelEach()
		{
			return contracts == 0 ? 0 : steel / (double) contracts;
		}

		/** Seconds a contract takes you, the trip for it included, or 0 when none was timed. */
		public double secondsEach()
		{
			return timed == 0 ? 0 : seconds / (double) timed;
		}
	}

	/** A tier priced at today's prices. */
	public static final class Row
	{
		private final Contract.Tier tier;
		private final Tally tally;
		private final double coinsEach;
		private final double perXp;
		private final double xpPerHour;
		private final boolean unlocked;

		Row(Contract.Tier tier, Tally tally, double coinsEach, double perXp, double xpPerHour, boolean unlocked)
		{
			this.tier = tier;
			this.tally = tally;
			this.coinsEach = coinsEach;
			this.perXp = perXp;
			this.xpPerHour = xpPerHour;
			this.unlocked = unlocked;
		}

		public Contract.Tier getTier()
		{
			return tier;
		}

		/** This tier's contracts, or null when you have finished none. */
		public Tally getTally()
		{
			return tally;
		}

		/** Coins one contract's planks and steel cost. */
		public double getCoinsEach()
		{
			return coinsEach;
		}

		/** Coins each experience point costs; 0 when not known. */
		public double getPerXp()
		{
			return perXp;
		}

		/** Experience an hour at your own loop time, or 0 when not timed. */
		public double getXpPerHour()
		{
			return xpPerHour;
		}

		public boolean isUnlocked()
		{
			return unlocked;
		}

		public boolean isKnown()
		{
			return tally != null && tally.contracts > 0;
		}
	}

	private final Map<Contract.Tier, Tally> tallies = new EnumMap<>(Contract.Tier.class);

	/**
	 * A finished contract.
	 *
	 * @param xp Construction experience from taking the job to handing it in
	 * @param planks planks the whole job took
	 * @param steel steel bars it took
	 * @param seconds the whole loop, from the contract before it was handed in, or 0 when not timed
	 * @return true when it was kept; experience no contract gives is left out
	 */
	public boolean record(Contract.Tier tier, long xp, int planks, int steel, long seconds)
	{
		if (tier == null || xp <= 0 || xp > MAX_XP || planks < 0 || steel < 0)
		{
			return false;
		}
		Tally t = tallies.computeIfAbsent(tier, k -> new Tally());
		t.contracts++;
		t.xp += xp;
		t.planks += planks;
		t.steel += steel;
		if (seconds > 0 && seconds <= MAX_LOOP_SECONDS)
		{
			t.timed++;
			t.seconds += seconds;
		}
		return true;
	}

	/** A tier's contracts, or null when none is recorded. */
	public Tally tally(Contract.Tier tier)
	{
		return tallies.get(tier);
	}

	public boolean isEmpty()
	{
		return tallies.isEmpty();
	}

	/**
	 * Every tier, cheapest experience first among those you have done, then those you can take but
	 * have not, then those your level does not reach.
	 *
	 * @param price an item's Grand Exchange price, 0 when unknown
	 * @return the tiers, or an empty list while steel bar or a done tier's plank has no price yet
	 */
	public List<Row> rank(IntUnaryOperator price, int constructionLevel)
	{
		List<Row> rows = new ArrayList<>();
		int steelPrice = price.applyAsInt(Contract.STEEL_BAR);
		for (Contract.Tier tier : Contract.Tier.values())
		{
			Tally t = tallies.get(tier);
			boolean unlocked = constructionLevel >= tier.getLevel();
			if (t == null || t.contracts == 0)
			{
				rows.add(new Row(tier, null, 0, 0, 0, unlocked));
				continue;
			}
			int plankPrice = price.applyAsInt(tier.getPlank());
			if (plankPrice <= 0 || t.steel > 0 && steelPrice <= 0)
			{
				// An unpriced plank would make the tier look free.
				return new ArrayList<>();
			}
			double coins = t.planksEach() * plankPrice + t.steelEach() * steelPrice;
			double perXp = coins / t.xpEach();
			double xpPerHour = t.secondsEach() > 0 ? t.xpEach() * 3600 / t.secondsEach() : 0;
			rows.add(new Row(tier, t, coins, perXp, xpPerHour, unlocked));
		}
		rows.sort((a, b) ->
		{
			int ga = group(a);
			int gb = group(b);
			if (ga != gb)
			{
				return Integer.compare(ga, gb);
			}
			if (ga == 0)
			{
				return Double.compare(a.perXp, b.perXp);
			}
			return Integer.compare(b.tier.ordinal(), a.tier.ordinal());
		});
		return rows;
	}

	private static int group(Row r)
	{
		return r.isKnown() ? 0 : r.unlocked ? 1 : 2;
	}

	/** Saved as "tier=contracts,xp,planks,steel,timed,seconds;..." with tiers by name. */
	public String encode()
	{
		StringBuilder b = new StringBuilder();
		for (Map.Entry<Contract.Tier, Tally> e : tallies.entrySet())
		{
			Tally t = e.getValue();
			if (b.length() > 0)
			{
				b.append(';');
			}
			b.append(e.getKey().name()).append('=').append(t.contracts).append(',').append(t.xp).append(',').append(t.planks)
				.append(',').append(t.steel).append(',').append(t.timed).append(',').append(t.seconds);
		}
		return b.toString();
	}

	/** Reads a saved book; damaged entries are dropped and the rest kept. */
	public static ContractBook decode(String saved)
	{
		ContractBook book = new ContractBook();
		if (saved == null || saved.isEmpty())
		{
			return book;
		}
		for (String part : saved.split(";"))
		{
			int eq = part.indexOf('=');
			if (eq <= 0)
			{
				continue;
			}
			Contract.Tier tier;
			try
			{
				tier = Contract.Tier.valueOf(part.substring(0, eq).toUpperCase(Locale.ENGLISH));
			}
			catch (IllegalArgumentException e)
			{
				continue;
			}
			String[] f = part.substring(eq + 1).split(",");
			if (f.length != 6)
			{
				continue;
			}
			try
			{
				Tally t = new Tally();
				t.contracts = Integer.parseInt(f[0]);
				t.xp = Long.parseLong(f[1]);
				t.planks = Long.parseLong(f[2]);
				t.steel = Long.parseLong(f[3]);
				t.timed = Integer.parseInt(f[4]);
				t.seconds = Long.parseLong(f[5]);
				if (t.contracts > 0 && t.xp > 0 && t.planks >= 0 && t.steel >= 0 && t.timed >= 0 && t.timed <= t.contracts && t.seconds >= 0)
				{
					book.tallies.put(tier, t);
				}
			}
			catch (NumberFormatException e)
			{
				// A damaged entry is dropped; the rest still count.
			}
		}
		return book;
	}
}
