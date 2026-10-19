package com.codex.minigamepilot.core;

/**
 * How far the supplies in your bank go: at the Blast Furnace, how many bars your ore and coal make
 * together and which runs out first; at Mahogany Homes, how many contracts your planks and steel
 * bars cover at what your own contracts have taken. Both turn into time at your own pace once it
 * is known.
 */
public final class Supplies
{
	private Supplies()
	{
	}

	/** What a stock of ore and coal makes at the furnace. */
	public static final class Furnace
	{
		private final Bar bar;
		private final long bars;
		private final boolean coalShort;
		private final long coalToUseAllOre;
		private final double trips;
		private final double seconds;

		Furnace(Bar bar, long bars, boolean coalShort, long coalToUseAllOre, double trips, double seconds)
		{
			this.bar = bar;
			this.bars = bars;
			this.coalShort = coalShort;
			this.coalToUseAllOre = coalToUseAllOre;
			this.trips = trips;
			this.seconds = seconds;
		}

		public long getBars()
		{
			return bars;
		}

		/** True when coal runs out before the ore does. */
		public boolean isCoalShort()
		{
			return coalShort;
		}

		/** More coal it would take to smelt every ore, 0 when the coal is enough. */
		public long getCoalToUseAllOre()
		{
			return coalToUseAllOre;
		}

		/** Trips to the furnace, coal trips included. */
		public double getTrips()
		{
			return trips;
		}

		/** Seconds all of it takes at your trip time, 0 when that is not known. */
		public double getSeconds()
		{
			return seconds;
		}

		/** "378 runite bars, about 1h 50m" or "No runite ore in the bank". */
		public String headline()
		{
			if (bars <= 0)
			{
				return "Not enough " + bar.oreName() + (bar.getCoal() > 0 ? " and coal" : "") + " for a bar";
			}
			String s = Goal.group(bars) + " " + bar.barName(bars == 1 ? 1 : 2) + ", " + Math.max(1, Math.round(trips)) + (Math.round(trips) == 1 ? " trip" : " trips");
			return seconds > 0 ? s + ", about " + Goal.duration(Math.round(seconds / 60)) : s;
		}

		/** What runs out first, or null when the ore and coal come out even. */
		public String limit()
		{
			if (bars <= 0)
			{
				return null;
			}
			if (coalShort)
			{
				return "Coal runs out first: " + Goal.group(coalToUseAllOre) + " more would smelt all the ore";
			}
			return bar.getCoal() > 0 ? "The ore runs out first" : null;
		}
	}

	/**
	 * @param ore the bar's ore in the bank and carried
	 * @param coal coal in the bank and carried
	 * @param slots ore one trip carries
	 * @param bag coal the coal bag brings along, 0 without one
	 * @param tripSeconds your usual trip, bank to bank, or 0 when not known
	 */
	public static Furnace furnace(Bar bar, long ore, long coal, int slots, int bag, double tripSeconds)
	{
		int perBar = bar.getCoal();
		long bars = perBar == 0 ? Math.max(0, ore) : Math.max(0, Math.min(ore, coal / perBar));
		boolean coalShort = perBar > 0 && bars < ore;
		long needed = coalShort ? ore * perBar - coal : 0;
		int load = Math.max(1, slots);
		double trips = bars / (double) load * (1 + BarPick.coalTrips(perBar, load, Math.max(0, bag)));
		return new Furnace(bar, bars, coalShort, Math.max(0, needed), trips, tripSeconds > 0 ? trips * tripSeconds : 0);
	}

	/** What a stock of planks and steel covers at Mahogany Homes. */
	public static final class Homes
	{
		private final Contract.Tier tier;
		private final long planks;
		private final long contracts;
		private final boolean steelShort;
		private final double seconds;

		Homes(Contract.Tier tier, long planks, long contracts, boolean steelShort, double seconds)
		{
			this.tier = tier;
			this.planks = planks;
			this.contracts = contracts;
			this.steelShort = steelShort;
			this.seconds = seconds;
		}

		/** Contracts the planks cover, or -1 when a contract's needs are not learned yet. */
		public long getContracts()
		{
			return contracts;
		}

		public boolean isSteelShort()
		{
			return steelShort;
		}

		/** "Teak planks for about 11 contracts, 1h 10m" or "212 teak planks". */
		public String headline()
		{
			String name = tier.planks(2).substring(2);
			if (contracts < 0)
			{
				return planks <= 0 ? "No " + name + " in the bank" : capital(tier.planks((int) Math.min(Integer.MAX_VALUE, planks)));
			}
			if (contracts == 0)
			{
				return "Not enough " + name + " for a contract";
			}
			String s = capital(name) + " for about " + contracts + (contracts == 1 ? " contract" : " contracts");
			return seconds > 0 ? s + ", " + Goal.duration(Math.round(seconds / 60)) : s;
		}
	}

	/**
	 * @param planks the tier's planks in the bank, carried and in the plank sack
	 * @param steel steel bars in the bank and carried
	 * @param tally what your contracts of this tier have taken, or null when none is recorded
	 */
	public static Homes homes(Contract.Tier tier, long planks, long steel, ContractBook.Tally tally)
	{
		if (tally == null || tally.getContracts() == 0 || tally.planksEach() <= 0)
		{
			return new Homes(tier, planks, -1, false, 0);
		}
		long byPlanks = (long) Math.floor(planks / tally.planksEach());
		long bySteel = tally.steelEach() > 0 ? (long) Math.floor(steel / tally.steelEach()) : Long.MAX_VALUE;
		long contracts = Math.max(0, Math.min(byPlanks, bySteel));
		return new Homes(tier, planks, contracts, bySteel < byPlanks, contracts * tally.secondsEach());
	}

	private static String capital(String s)
	{
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}
