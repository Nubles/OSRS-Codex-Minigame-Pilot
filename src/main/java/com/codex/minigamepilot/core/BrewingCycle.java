package com.codex.minigamepilot.core;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.gameval.ItemID;

/**
 * The next step at Trouble Brewing, as plain information.
 *
 * <p>In the lobby: Cabin Fever and 40 Cooking if you lack them ("you must have completed the
 * Cabin Fever quest and have at least 40 Cooking"); the reward you are saving for, once you hold
 * its pieces of eight; an empty inventory, an empty head slot and no follower ("you cannot have
 * any followers, items in your inventory, or anything equipped in the head slot"); else join a
 * team.
 *
 * <p>In a game: the next link of the ingredient chain you are on, from what you carry, each as
 * the wiki gives it. Carrying nothing for the still, the ingredient the hopper is shortest of
 * (by the overlay's own counts and the bottle's recipe), or a boiler under three logs.
 *
 * <p>Pure: the pilot fills a {@link Lobby} or a {@link Game}.
 */
public final class BrewingCycle
{
	/** Ids carried in a game. The bucket is RuneLite's ordinary "Bucket of water": unconfirmed that the game's buckets are that item. */
	public static final int BUCKET_WATER = ItemID.BUCKET_WATER;
	public static final int[] FLOWERS = {ItemID.BREW_BLUE_FLOWER, ItemID.BREW_RED_FLOWER};
	public static final int[] BOWLS = {ItemID.BREW_BOWL_BLUE, ItemID.BREW_BOWL_RED};
	public static final int MONKEY = ItemID.BREW_MONKEY;
	public static final int[] DYED_MONKEYS = {ItemID.BREW_BLUE_MONKEY, ItemID.BREW_CAREFUL_BLUE_MONKEY, ItemID.BREW_ANGRY_BLUE_MONKEY,
		ItemID.BREW_RED_MONKEY, ItemID.BREW_CAREFUL_RED_MONKEY, ItemID.BREW_ANGRY_RED_MONKEY};
	public static final int BITTERNUT = ItemID.BREW_BITTERNUT;
	public static final int SCRAPEY_LOGS = ItemID.BREW_SCRAPEY_LOGS;
	public static final int SCRAPEY_BARK = ItemID.BREW_SCRAPEY_BARK;
	public static final int SWEETGRUBS = ItemID.BREW_SWEETGRUBS;

	private static final String HOPPERS = "On the top floor are the hoppers";

	private BrewingCycle()
	{
	}

	/** What the lobby shows this tick. */
	public static final class Lobby
	{
		public boolean cabinFever;
		/** Real Cooking level. */
		public int cooking = 1;
		/** Inventory slots in use. */
		public int carried;
		/** Something in the head slot. */
		public boolean hat;
		public boolean follower;
		/** In the waiting room. */
		public boolean waiting;
		/** The waiting room's own text for the time to the game, or "". */
		public String waitingText = "";
		/** Pieces of eight held, or -1 when not known. */
		public long pieces = -1;
		/** The reward saved for, or null. */
		public Brewing.Reward goal;
		/** What it still costs you (its pieces not yet owned), or 0 when yours. */
		public int goalCost;
		/** For a set, the cheapest piece still to buy, or null. */
		public Brewing.Piece nextPiece;
	}

	/** The next step in the lobby. Never null. */
	public static Step lobby(Lobby s)
	{
		if (!s.cabinFever)
		{
			return Step.of("Quest", "Finish Cabin Fever",
				"Trouble Brewing opens after Cabin Fever and " + Brewing.COOKING_LEVEL + " Cooking", Step.Target.NONE);
		}
		if (s.cooking < Brewing.COOKING_LEVEL)
		{
			return Step.of("Train", "Train Cooking to " + Brewing.COOKING_LEVEL,
				"Trouble Brewing lets you in at " + Brewing.COOKING_LEVEL + " Cooking; you have " + s.cooking, Step.Target.NONE);
		}
		if (s.waiting)
		{
			return Step.waiting("Wait for the game", s.waitingText.isEmpty() ? "The game starts when both teams have a player"
				: s.waitingText, Step.Target.NONE);
		}
		if (s.goal != null && !s.goal.repeat() && s.goalCost > 0 && s.pieces >= 0)
		{
			int price = s.nextPiece != null ? s.nextPiece.cost() : s.goal.cost();
			String what = s.nextPiece != null ? s.nextPiece.getTitle() : s.goal.getTitle();
			if (s.pieces >= price)
			{
				return Step.of("Buy", "Buy the " + what.toLowerCase(Locale.ROOT),
					String.format(Locale.ROOT, "%,d of your %,d pieces of eight", price, s.pieces), Step.Target.HONEST_JIMMY);
			}
		}
		if (s.follower)
		{
			return Step.of("Dismiss", "Leave your follower behind", "No team takes you with a follower", Step.Target.NONE);
		}
		if (s.carried > 0 || s.hat)
		{
			String what = s.carried > 0 && s.hat ? "your items and your hat" : s.carried > 0 ? "your items" : "your hat";
			return Step.of("Bank", "Bank " + what,
				"Teams take an empty inventory and bare head", Step.Target.NONE);
		}
		return Step.of("Join", "Join a team",
			"Honest Jimmy signs you to the smaller team", Step.Target.HONEST_JIMMY);
	}

	/** What a game shows this tick. */
	public static final class Game
	{
		/** What is carried, by item id. */
		public Map<Integer, Integer> carried = Collections.emptyMap();
		/** The overlay's count of each ingredient, or -1 when not read. */
		public final int[] hopper = {-1, -1, -1, -1, -1};
		/** The overlay's logs in each boiler, or -1 when not read. */
		public final int[] boilers = {-1, -1, -1};

		int count(int... ids)
		{
			int n = 0;
			for (int id : ids)
			{
				n += carried.getOrDefault(id, 0);
			}
			return n;
		}
	}

	/** The next link of the chain you are on. Never null. */
	public static Step game(Game s)
	{
		// Ingredients ready for the still first: each goes into its hopper.
		if (s.count(SWEETGRUBS) > 0)
		{
			return Step.items("Use", "Put the sweetgrubs in the hopper", HOPPERS + "; a bottle takes one", SWEETGRUBS);
		}
		if (s.count(BITTERNUT) > 0)
		{
			return Step.items("Use", "Put the bitternut in the hopper", HOPPERS + "; a bottle takes one", BITTERNUT);
		}
		if (s.count(SCRAPEY_BARK) > 0)
		{
			return Step.items("Use", "Put the scrapey bark in the hopper", HOPPERS + "; a bottle takes one", SCRAPEY_BARK);
		}
		if (s.count(SCRAPEY_LOGS) > 0)
		{
			return Step.items("Fletch", "Fletch the scrapey logs", "A knife turns them into scrapey bark for the hopper", SCRAPEY_LOGS);
		}
		if (s.count(DYED_MONKEYS) > 0)
		{
			return Step.items("Use", "Use the monkey on the Bitternut tree", "A monkey dyed your team's colour fetches a bitternut; you carry two at most", DYED_MONKEYS);
		}
		if (s.count(MONKEY) > 0)
		{
			if (s.count(BOWLS) > 0)
			{
				return Step.items("Dye", "Dye the monkey", "Coloured water marks it as on your team", BOWLS);
			}
			return Step.of("Fetch", "Fetch coloured water for the monkey", "A bowl draws it from your kettle once there are flowers in it", Step.Target.NONE);
		}
		if (s.count(FLOWERS) > 0)
		{
			return Step.items("Use", "Put the flowers in your kettle", "The kettle in your base holds ten; a bowl draws coloured water from it", FLOWERS);
		}
		if (s.count(BOWLS) > 0)
		{
			return Step.items("Use", "Put the coloured water in the hopper", HOPPERS + "; a bottle takes three bowls", BOWLS);
		}
		if (s.count(BUCKET_WATER) > 0)
		{
			return Step.items("Use", "Put the water in the hopper", HOPPERS + "; a bottle takes five buckets", BUCKET_WATER);
		}
		// Carrying nothing for the still: a cold boiler, else what the hopper is shortest of.
		for (int i = 0; i < s.boilers.length; i++)
		{
			if (s.boilers[i] >= 0 && s.boilers[i] < Brewing.BOILER_LOGS_MIN)
			{
				return Step.of("Fuel", "Put logs in boiler " + (i + 1),
					"It has " + s.boilers[i] + "; each boiler needs " + Brewing.BOILER_LOGS_MIN + " logs, lit, to make rum", Step.Target.NONE);
			}
		}
		Brewing.Ingredient shortest = shortest(s);
		if (shortest == null)
		{
			return Step.of("Brew", "Bring an ingredient for the still", "The overlay along the top shows what the hopper holds", Step.Target.NONE);
		}
		int have = s.hopper[shortest.ordinal()];
		String held = have + " in the hopper, " + shortest.perBottle() + " a bottle";
		switch (shortest)
		{
			case BITTERNUT:
				return Step.of("Catch", "Catch a monkey for a bitternut", "Bitternuts are shortest: " + held, Step.Target.NONE);
			case SWEETGRUB:
				return Step.of("Lure", "Use raw meat on a sweetgrub mound", "Sweetgrubs are shortest: " + held + "; the table in your base has meat", Step.Target.NONE);
			case SCRAPEY_BARK:
				return Step.of("Chop", "Cut a scrapey tree", "Scrapey bark is shortest: " + held, Step.Target.NONE);
			case WATER:
				return Step.of("Fill", "Fill buckets at the pump", "Water is shortest: " + held + "; the pump is outside your base", Step.Target.NONE);
			default:
				return Step.of("Pick", "Pick the other team's flowers", "Coloured water is shortest: " + held + "; they grow outside their base", Step.Target.NONE);
		}
	}

	/** The ingredient the hopper has fewest bottles' worth of, or null when the overlay was not read. */
	static Brewing.Ingredient shortest(Game s)
	{
		Brewing.Ingredient best = null;
		double least = Double.MAX_VALUE;
		for (Brewing.Ingredient i : Brewing.Ingredient.values())
		{
			int n = s.hopper[i.ordinal()];
			if (n < 0)
			{
				continue;
			}
			double bottles = n / (double) i.perBottle();
			if (bottles < least)
			{
				least = bottles;
				best = i;
			}
		}
		return best;
	}
}
