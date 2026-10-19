package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.IntPredicate;

/**
 * What you are working toward in a minigame and how far off it is. A reward goal adds up the
 * pieces you do not own yet (a prospector piece in the bank counts as owned), takes off what you
 * already hold in the minigame's currency, and turns the rest into rounds and time at your own
 * pace. A level goal does the same with experience.
 */
public final class Goal
{
	/** A reward from a minigame's shop. */
	public enum Reward
	{
		PROSPECTOR_HELMET("Prospector helmet", 40, Currency.NUGGETS, 12013, 25549),
		PROSPECTOR_JACKET("Prospector jacket", 60, Currency.NUGGETS, 12014, 25551),
		PROSPECTOR_LEGS("Prospector legs", 50, Currency.NUGGETS, 12015, 25553),
		PROSPECTOR_BOOTS("Prospector boots", 30, Currency.NUGGETS, 12016, 25555),
		COAL_BAG("Coal bag", 100, Currency.NUGGETS, 12019, 24480),
		GEM_BAG("Gem bag", 100, Currency.NUGGETS, 12020, 24481),
		/** Owned when the sack's upgrade varbit is set, not by an item. */
		BIGGER_SACK("Bigger sack", 200, Currency.NUGGETS),
		CARPENTERS_HELMET("Carpenter's helmet", 400, Currency.POINTS, 24872),
		CARPENTERS_SHIRT("Carpenter's shirt", 800, Currency.POINTS, 24874),
		CARPENTERS_TROUSERS("Carpenter's trousers", 600, Currency.POINTS, 24876),
		CARPENTERS_BOOTS("Carpenter's boots", 200, Currency.POINTS, 24878),
		PLANK_SACK("Plank sack", 350, Currency.POINTS, 24882),
		AMYS_SAW("Amy's saw", 500, Currency.POINTS, 24880),
		HOSIDIUS_BLUEPRINTS("Hosidius blueprints", 2000, Currency.POINTS, 24885);

		private final String name;
		private final int cost;
		private final Currency currency;
		private final int[] items;

		Reward(String name, int cost, Currency currency, int... items)
		{
			this.name = name;
			this.cost = cost;
			this.currency = currency;
			this.items = items;
		}

		public String getName()
		{
			return name;
		}

		public int getCost()
		{
			return cost;
		}

		public Currency getCurrency()
		{
			return currency;
		}

		/** The items that mean you own it, any one of them. */
		public int[] getItems()
		{
			return items.clone();
		}

		/** Whether any of its items is among what you have. */
		public boolean ownedBy(IntPredicate have)
		{
			for (int id : items)
			{
				if (have.test(id))
				{
					return true;
				}
			}
			return false;
		}
	}

	/** What a minigame's shop takes. */
	public enum Currency
	{
		NUGGETS("nugget", "nuggets"),
		POINTS("point", "points");

		private final String one;
		private final String many;

		Currency(String one, String many)
		{
			this.one = one;
			this.many = many;
		}

		public String of(long n)
		{
			return n == 1 ? one : many;
		}
	}

	/** One piece of a reward goal. */
	public static final class Piece
	{
		private final Reward reward;
		private final boolean owned;

		public Piece(Reward reward, boolean owned)
		{
			this.reward = reward;
			this.owned = owned;
		}

		public Reward getReward()
		{
			return reward;
		}

		public boolean isOwned()
		{
			return owned;
		}
	}

	private final String title;
	private final long have;
	private final long need;
	private final String unit;
	private final List<Piece> pieces;
	private final double perHour;
	private final double perRound;
	private final String roundName;

	private Goal(String title, long have, long need, String unit, List<Piece> pieces, double perHour, double perRound, String roundName)
	{
		this.title = title;
		this.have = Math.max(0, have);
		this.need = Math.max(0, need);
		this.unit = unit;
		this.pieces = Collections.unmodifiableList(pieces);
		this.perHour = perHour;
		this.perRound = perRound;
		this.roundName = roundName;
	}

	/**
	 * @param title "Prospector kit"
	 * @param pieces every reward in the goal, with whether you own it
	 * @param balance what you hold of the currency now
	 * @param perHour currency you earn an hour at your pace, or 0 when not known yet
	 * @param perRound currency a round earns, or 0 when not known
	 * @param roundName "contract", for "4 contracts to go"
	 */
	public static Goal rewards(String title, Currency currency, List<Piece> pieces, long balance, double perHour, double perRound, String roundName)
	{
		long cost = 0;
		for (Piece p : pieces)
		{
			if (!p.owned)
			{
				cost += p.reward.cost;
			}
		}
		return new Goal(title, Math.min(balance, cost), cost, currency.many, new ArrayList<>(pieces), perHour, perRound, roundName);
	}

	/**
	 * @param skill "Mining"
	 * @param xp your experience now
	 * @param level the level to reach
	 * @param fromXp the experience the climb is measured from, such as the start of the current level
	 * @param toXp the experience the level takes
	 * @param perHour experience an hour at your pace, or 0
	 * @param perRound experience a round earns, or 0
	 */
	public static Goal level(String skill, int level, long fromXp, long xp, long toXp, double perHour, double perRound, String roundName)
	{
		long span = Math.max(1, toXp - fromXp);
		long done = Math.max(0, Math.min(span, xp - fromXp));
		return new Goal(skill + " level " + level, done, span, "xp", Collections.emptyList(), perHour, perRound, roundName);
	}

	/** The first reward in the list you do not own yet, or null when you have them all. */
	public static Reward firstMissing(List<Reward> rewards, IntPredicate have, java.util.Set<Reward> ownedOtherwise)
	{
		for (Reward r : rewards)
		{
			if (!ownedOtherwise.contains(r) && !r.ownedBy(have))
			{
				return r;
			}
		}
		return null;
	}

	public String getTitle()
	{
		return title;
	}

	/** How much of the goal you have, capped at what it needs. */
	public long getHave()
	{
		return have;
	}

	/** What the goal still takes in all, before what you hold. */
	public long getNeed()
	{
		return need;
	}

	public String getUnit()
	{
		return unit;
	}

	public List<Piece> getPieces()
	{
		return pieces;
	}

	public long toGo()
	{
		return Math.max(0, need - have);
	}

	public boolean done()
	{
		return toGo() == 0;
	}

	public double progress()
	{
		return need == 0 ? 1 : have / (double) need;
	}

	/** Rounds left at what a round earns, or -1 when not known. */
	public long roundsLeft()
	{
		if (perRound <= 0)
		{
			return -1;
		}
		return (long) Math.ceil(toGo() / perRound - 1e-9);
	}

	/** Minutes left at your pace, or -1 when your pace is not known yet. */
	public long minutesLeft()
	{
		if (perHour <= 0)
		{
			return -1;
		}
		return (long) Math.ceil(toGo() * 60 / perHour - 1e-9);
	}

	/** "143 nuggets to go · 36 contracts · about 2h 10m" */
	public String describe()
	{
		if (done())
		{
			return pieces.isEmpty() ? "Reached" : "You have enough: off to the shop";
		}
		StringBuilder b = new StringBuilder(group(toGo()) + " " + unit + " to go");
		long rounds = roundsLeft();
		if (rounds > 0 && roundName != null)
		{
			b.append(" · ").append(rounds).append(' ').append(rounds == 1 ? roundName : roundName + "s");
		}
		long minutes = minutesLeft();
		if (minutes >= 0)
		{
			b.append(" · about ").append(duration(minutes));
		}
		return b.toString();
	}

	/** 130 minutes as "2h 10m". */
	public static String duration(long minutes)
	{
		if (minutes < 60)
		{
			return Math.max(1, minutes) + "m";
		}
		long h = minutes / 60;
		long m = minutes % 60;
		if (h >= 100)
		{
			return h + "h";
		}
		return m == 0 ? h + "h" : h + "h " + m + "m";
	}

	static String group(long n)
	{
		return String.format(Locale.ENGLISH, "%,d", n);
	}
}
