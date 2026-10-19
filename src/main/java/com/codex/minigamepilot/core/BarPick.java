package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Which bar pays best at the Blast Furnace right now. Each bar is priced at the Grand Exchange:
 * what a bar sells for less its ore and the coal it burns. Then the trips are counted: a load of
 * ore takes one trip, and bars that burn more coal than the coal bag carries along need coal trips
 * too, so runite makes far fewer bars an hour than steel. At your own trip time that gives coins
 * and experience an hour, less the coffer (and the foreman under level 60).
 */
public final class BarPick
{
	/** Coins the coffer burns an hour. */
	public static final int COFFER_PER_HOUR = 72_000;
	/** The foreman's fee an hour below level 60 Smithing: 2,500 coins every ten minutes. */
	public static final int FOREMAN_PER_HOUR = 15_000;
	public static final int FOREMAN_FREE_LEVEL = 60;

	/** A bar's Smithing level and experience at the Blast Furnace. */
	enum Facts
	{
		IRON(Bar.IRON, 15, 12.5),
		SILVER(Bar.SILVER, 20, 13.7),
		STEEL(Bar.STEEL, 30, 17.5),
		GOLD(Bar.GOLD, 40, 22.5),
		MITHRIL(Bar.MITHRIL, 50, 30),
		ADAMANTITE(Bar.ADAMANTITE, 70, 37.5),
		RUNITE(Bar.RUNITE, 85, 50);

		final Bar bar;
		final int level;
		final double xp;

		Facts(Bar bar, int level, double xp)
		{
			this.bar = bar;
			this.level = level;
			this.xp = xp;
		}
	}

	/** Gold with goldsmith gauntlets on gives this much experience a bar instead. */
	static final double GOLD_GAUNTLETS_XP = 56.2;

	/** One bar, priced. */
	public static final class Row
	{
		private final Bar bar;
		private final int level;
		private final boolean unlocked;
		private final long perBar;
		private final double perTrip;
		private final double coalTrips;
		private final double barsPerHour;
		private final long perHour;
		private final double xpPerHour;

		Row(Bar bar, int level, boolean unlocked, long perBar, double perTrip, double coalTrips, double barsPerHour, long perHour, double xpPerHour)
		{
			this.bar = bar;
			this.level = level;
			this.unlocked = unlocked;
			this.perBar = perBar;
			this.perTrip = perTrip;
			this.coalTrips = coalTrips;
			this.barsPerHour = barsPerHour;
			this.perHour = perHour;
			this.xpPerHour = xpPerHour;
		}

		public Bar getBar()
		{
			return bar;
		}

		public int getLevel()
		{
			return level;
		}

		public boolean isUnlocked()
		{
			return unlocked;
		}

		/** Coins a bar makes after its ore and coal. */
		public long getPerBar()
		{
			return perBar;
		}

		/** Coins each trip makes, coal trips counted in, before the coffer. */
		public double getPerTrip()
		{
			return perTrip;
		}

		/** Coal trips each load of ore needs. */
		public double getCoalTrips()
		{
			return coalTrips;
		}

		/** Bars an hour at the trip time given, or 0 when no trip time is known. */
		public double getBarsPerHour()
		{
			return barsPerHour;
		}

		/** Coins an hour after the coffer, or Long.MIN_VALUE when no trip time is known. */
		public long getPerHour()
		{
			return perHour;
		}

		public double getXpPerHour()
		{
			return xpPerHour;
		}
	}

	/** What the ranking is worked out for. */
	public static final class Setup
	{
		/** Your Smithing level. */
		public int level = 1;
		public boolean coalBag;
		public int bagCapacity = 27;
		/** Ore one trip carries: free slots once the coal bag and the like are in. */
		public int oreSlots = 27;
		/** Seconds a trip takes you, bank to bank, or 0 when not known. */
		public double tripSeconds;
		public boolean goldsmith;
	}

	private BarPick()
	{
	}

	/**
	 * Every bar the furnace makes, priced, best first: those you can make by coins a trip (which
	 * at one trip time is the same order as coins an hour), then those your level does not reach.
	 *
	 * @param price an item's Grand Exchange price, 0 when unknown
	 * @return the bars, or an empty list when prices are not loaded
	 */
	public static List<Row> rank(IntUnaryOperator price, Setup s)
	{
		List<Row> rows = new ArrayList<>();
		int coal = price.applyAsInt(Bar.COAL);
		int slots = Math.max(1, s.oreSlots);
		int bag = s.coalBag ? Math.max(0, s.bagCapacity) : 0;
		long fixed = COFFER_PER_HOUR + (s.level < FOREMAN_FREE_LEVEL ? FOREMAN_PER_HOUR : 0);
		for (Facts f : Facts.values())
		{
			int barPrice = price.applyAsInt(f.bar.getBar());
			int ore = price.applyAsInt(f.bar.getOre());
			if (barPrice <= 0 || ore <= 0 || f.bar.getCoal() > 0 && coal <= 0)
			{
				// A price not loaded yet would rank the bar wrongly; leave it out until it is.
				continue;
			}
			long perBar = barPrice - ore - (long) f.bar.getCoal() * coal;
			double coalTrips = coalTrips(f.bar.getCoal(), slots, bag);
			double tripsPerLoad = 1 + coalTrips;
			double perTrip = perBar * slots / tripsPerLoad;
			double barsPerHour = 0;
			long perHour = Long.MIN_VALUE;
			double xpPerHour = 0;
			if (s.tripSeconds > 0)
			{
				barsPerHour = slots * 3600 / (s.tripSeconds * tripsPerLoad);
				perHour = Math.round(barsPerHour * perBar) - fixed;
				double xp = f.bar == Bar.GOLD && s.goldsmith ? GOLD_GAUNTLETS_XP : f.xp;
				xpPerHour = barsPerHour * xp;
			}
			rows.add(new Row(f.bar, f.level, s.level >= f.level, perBar, perTrip, coalTrips, barsPerHour, perHour, xpPerHour));
		}
		rows.sort((a, b) ->
		{
			if (a.unlocked != b.unlocked)
			{
				return a.unlocked ? -1 : 1;
			}
			return Double.compare(b.perTrip, a.perTrip);
		});
		return rows;
	}

	/**
	 * Coal trips each load of ore needs, on average: a load burns slots times the bar's coal,
	 * the coal bag brings some of it along with the ore, and each coal trip brings a full pack
	 * and a full bag.
	 */
	static double coalTrips(int coalPerBar, int slots, int bag)
	{
		double need = (double) coalPerBar * slots - bag;
		return need <= 0 ? 0 : need / (slots + bag);
	}
}
