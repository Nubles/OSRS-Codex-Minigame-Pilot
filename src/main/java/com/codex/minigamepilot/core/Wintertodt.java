package com.codex.minigamepilot.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;

/**
 * The facts of the Wintertodt, in its prison north of the Wintertodt Camp, as the game has been
 * since the 9 October 2024 changes (the warmth meter in place of Hitpoints, and the reward cart
 * in place of supply crates).
 *
 * <p>Figures are from the OSRS wiki (Wintertodt; Reward Cart; Brazier; Pyromancer; Rejuvenation
 * potion; Guide:Efficient Wintertodt), quoted beside each. Ids are RuneLite's gameval names. How
 * the HUD writes its energy, warmth, points and the countdown to the next round is read from the
 * game's own client scripts as RuneLite publishes them (runelite/cs2-scripts: wint_update,
 * wint_timer_countdown). The prison's map region is as RuneLite's own Wintertodt plugin reads it.
 */
public final class Wintertodt
{
	/** "50 Firemaking is required to fight the Wintertodt". */
	public static final int FIREMAKING = 50;
	/** The wiki's points table: "Adding bruma roots to a brazier" 10. */
	public static final int ROOT_POINTS = 10;
	/** "Adding bruma kindling to a brazier" 25. */
	public static final int KINDLING_POINTS = 25;
	/** "Lighting a brazier" 25. */
	public static final int LIGHT_POINTS = 25;
	/** "Repairing a destroyed brazier" 25. */
	public static final int FIX_POINTS = 25;
	/** "Healing a pyromancer" 75 ("increased from 30 to 75", 16 October 2024). */
	public static final int HEAL_POINTS = 75;
	/** "Players must collect a minimum of 500 points to be rewarded anything for their efforts." */
	public static final int MIN_POINTS = 500;
	/** "Every 500 additional points will guarantee an extra reward owed." */
	public static final int PER_ROLL = 500;
	/** Food "that heals at least 4 Hitpoints ... will restore 35% warmth". */
	public static final int FOOD_WARMTH = 35;
	/** A rejuvenation potion: "restore 30% of your own warmth" a dose. */
	public static final int POTION_WARMTH = 30;
	/** "The Wintertodt ... now always respawns in one minute" (13 September 2023): 100 ticks. */
	public static final int RESPAWN_TICKS = 100;
	/** The Efficient Wintertodt guide: "run to the brazier when the number of logs is two less than the remaining energy." */
	public static final int FEED_MARGIN = 2;
	/** "Rewards are collected from the reward cart at a rate of 1 reward for every 3 ticks"; Big-search takes "batches of 5 per click". */
	public static final int BIG_SEARCH = 5;
	/** The phoenix is "fifteen times more common" at 200 million Firemaking experience (OSRS wiki, Phoenix). */
	public static final int MAX_XP = 200_000_000;
	public static final int MAX_XP_PET_FACTOR = 15;

	/** The prison, as RuneLite's Wintertodt plugin reads it ({@code WINTERTODT_REGION = 6462}). */
	public static final int PRISON_REGION = 6462;

	/** The HUD, shown in the prison: its title reads the energy in a round, and the countdown between rounds. */
	public static final int HUD_ENERGY = InterfaceID.WintStatus.ENERGY_TITLE;
	public static final int HUD_WARMTH = InterfaceID.WintStatus.WARMTH_TITLE;
	public static final int HUD_POINTS = InterfaceID.WintStatus.POINTS;
	/** Your warmth, 0 to 1000 (the game's script shows it as a percentage of 1000; RuneLite's status bars divide by 10). */
	public static final int WARMTH_VARBIT = VarbitID.WINT_WARMTH;

	/** Brazier: 29312 unlit, 29313 broken, 29314 burning (OSRS wiki, Brazier). */
	public static final int BRAZIER_UNLIT = ObjectID.WINT_BRAZIER;
	public static final int BRAZIER_BROKEN = ObjectID.WINT_BRAZIER_BROKEN;
	public static final int BRAZIER_LIT = ObjectID.WINT_BRAZIER_LIT;
	/** Bruma roots, the object chopped: 29311 (OSRS wiki, Bruma roots). */
	public static final int ROOTS = ObjectID.WINT_ROOTS;
	/** The crates of free tools in the prison, by RuneLite's names: "most are obtainable from crates upon entering the Doors of Dinh". */
	public static final int CRATE_HAMMER = ObjectID.WINT_CHEST_HAMMER;
	public static final int CRATE_KNIFE = ObjectID.WINT_CHEST_KNIFE;
	public static final int CRATE_AXE = ObjectID.WINT_CHEST_AXE;
	public static final int CRATE_TINDERBOX = ObjectID.WINT_CHEST_TINDERBOX;
	/** The reward cart: 55411, and 55412 to 55416 as it fills (OSRS wiki, Reward Cart). */
	public static final int[] CART = {ObjectID.WINT_REWARD_PILE_EMPTY, ObjectID.WINT_REWARD_PILE_1, ObjectID.WINT_REWARD_PILE_2,
		ObjectID.WINT_REWARD_PILE_3, ObjectID.WINT_REWARD_PILE_4, ObjectID.WINT_REWARD_PILE_FULL};

	/** Pyromancer 7371, and 7372 incapacitated (OSRS wiki, Pyromancer). */
	public static final int PYROMANCER = NpcID.WINT_WIZARD;
	public static final int PYROMANCER_DOWN = NpcID.WINT_WIZARD_DOWN;

	public static final int ROOT = ItemID.WINT_BRUMA_ROOT;
	public static final int KINDLING = ItemID.WINT_BRUMA_KINDLING;
	/** Rejuvenation potion (4) to (1): 20699 to 20702. */
	public static final int[] POTIONS = {ItemID.WINT_POTION4, ItemID.WINT_POTION3, ItemID.WINT_POTION2, ItemID.WINT_POTION1};
	/** "You must have either a tinderbox or a bruma torch in your inventory (or equipped) to light the brazier." */
	public static final int[] LIGHTERS = {ItemID.TINDERBOX, ItemID.WINT_TORCH, ItemID.WINT_TORCH_OFFHAND};
	/** A knife fletches a root ("using a knife on a bruma root"); "The fletching knife works on bruma roots". */
	public static final int[] KNIVES = {ItemID.KNIFE, ItemID.FLETCHING_KNIFE};
	/** "repaired with a hammer"; the Imcando hammer as RuneLite's Wintertodt plugin sees it fixing a brazier. */
	public static final int[] HAMMERS = {ItemID.HAMMER, ItemID.IMCANDO_HAMMER, ItemID.IMCANDO_HAMMER_OFFHAND};

	private static final Pattern ENERGY = Pattern.compile("Energy:\\s*(\\d{1,3})\\s*%");
	private static final Pattern WARMTH = Pattern.compile("Warmth:\\s*(\\d{1,3})\\s*%");
	private static final Pattern RETURNS = Pattern.compile("returns in:\\s*(\\d{1,2}):(\\d{2})");
	private static final Pattern POINTS = Pattern.compile("Points\\D*(\\d{1,6})");

	private Wintertodt()
	{
	}

	/** Wintertodt's energy from the HUD title, "Wintertodt's Energy: 45%", or -1. */
	public static int energy(String title)
	{
		return percent(ENERGY, title);
	}

	/** Your warmth from the HUD title, "Your Warmth: 72%", or -1. */
	public static int warmth(String title)
	{
		return percent(WARMTH, title);
	}

	/** Your warmth from its varbit (0 to 1000), as a percentage. */
	public static int warmthOf(int varbit)
	{
		return Math.max(0, Math.min(100, varbit / 10));
	}

	/** Seconds until the next round from the HUD title, "The Wintertodt returns in: 0:42", or -1. */
	public static int returnsIn(String title)
	{
		if (title == null)
		{
			return -1;
		}
		Matcher m = RETURNS.matcher(title);
		return m.find() ? Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2)) : -1;
	}

	/** Your points this round from the HUD, "Points<br>320", or -1. */
	public static int points(String text)
	{
		if (text == null)
		{
			return -1;
		}
		Matcher m = POINTS.matcher(text);
		return m.find() ? Integer.parseInt(m.group(1)) : -1;
	}

	private static int percent(Pattern p, String text)
	{
		if (text == null)
		{
			return -1;
		}
		Matcher m = p.matcher(text);
		return m.find() ? Math.min(100, Integer.parseInt(m.group(1))) : -1;
	}

	/**
	 * Rewards a round's points are sure to give: none under 500, then "500 points = 2 rolls",
	 * "1000 points = 3 rolls", "1500 points = 4 rolls" (OSRS wiki, Reward Cart).
	 */
	public static int sureRolls(int points)
	{
		return points < MIN_POINTS ? 0 : 1 + points / PER_ROLL;
	}

	/**
	 * The chance of one more reward past the sure ones: "For every 5 points past 500 points player
	 * gets 1% extra chance to get additional loot" ("750 points = 2 rolls + 50% chance").
	 */
	public static double extraChance(int points)
	{
		return points < MIN_POINTS ? 0 : (points % PER_ROLL) / (double) PER_ROLL;
	}

	/** The rewards a round's points give on average: the sure ones and the chance of one more. */
	public static double rolls(int points)
	{
		return sureRolls(points) + extraChance(points);
	}

	/** Points to the next sure reward: to 500 first, then to each 500 past it. */
	public static int toNextRoll(int points)
	{
		int p = Math.max(0, points);
		return p < MIN_POINTS ? MIN_POINTS - p : PER_ROLL - p % PER_ROLL;
	}

	/**
	 * The reward cart's rare rolls, in the order the cart tries them: "a drop will be rolled for
	 * until the roll results in a success", Phoenix first, then the dragon axe, the tome of fire,
	 * warm gloves and so on; "If none of the unique drop rolls are successful, then the player will
	 * receive an item from the normal drop table." Each is its chance on its own roll.
	 */
	public enum Unique
	{
		PHOENIX("Phoenix", 5_000),
		DRAGON_AXE("Dragon axe", 10_000),
		TOME_OF_FIRE("Tome of fire", 1_000),
		WARM_GLOVES("Warm gloves", 150),
		BRUMA_TORCH("Bruma torch", 150),
		/** "There is a 1/150 chance to receive a piece of the pyromancer's outfit players have the least of." */
		PYROMANCER("Pyromancer piece", 150),
		BURNT_PAGE("Burnt page", 45);

		private final String title;
		private final int oneIn;

		Unique(String title, int oneIn)
		{
			this.title = title;
			this.oneIn = oneIn;
		}

		public String getTitle()
		{
			return title;
		}

		/** Its chance on its own roll, one in this many. */
		public int oneIn()
		{
			return oneIn;
		}

		/**
		 * The chance a reward is this one: its own chance after every earlier roll in the order
		 * missed (the wiki's effective rates: bruma torch "about 1/151", the outfit "about 1/152").
		 *
		 * @param maxedFiremaking 200 million Firemaking experience, for the phoenix's fifteen-fold chance
		 */
		public double perReward(boolean maxedFiremaking)
		{
			double miss = 1;
			for (Unique u : values())
			{
				double p = u.own(maxedFiremaking);
				if (u == this)
				{
					return miss * p;
				}
				miss *= 1 - p;
			}
			return 0;
		}

		private double own(boolean maxedFiremaking)
		{
			double p = 1.0 / oneIn;
			return this == PHOENIX && maxedFiremaking ? Math.min(1, p * MAX_XP_PET_FACTOR) : p;
		}
	}

	/**
	 * What a player saves the reward cart's rolls for, with the items that show it owned. The
	 * pyromancer outfit is four pieces, one a success, always the piece "the player has least of".
	 */
	public enum Reward
	{
		PYROMANCER_OUTFIT("Pyromancer outfit", Unique.PYROMANCER),
		WARM_GLOVES("Warm gloves", Unique.WARM_GLOVES, ItemID.PYROMANCER_GLOVES),
		BRUMA_TORCH("Bruma torch", Unique.BRUMA_TORCH, ItemID.WINT_TORCH, ItemID.WINT_TORCH_OFFHAND),
		TOME_OF_FIRE("Tome of fire", Unique.TOME_OF_FIRE, ItemID.TOME_OF_FIRE, ItemID.TOME_OF_FIRE_UNCHARGED),
		PHOENIX("Phoenix", Unique.PHOENIX, ItemID.PHOENIXPET, ItemID.PHOENIXPET_GREEN, ItemID.PHOENIXPET_BLUE,
			ItemID.PHOENIXPET_WHITE, ItemID.PHOENIXPET_PURPLE);

		private final String title;
		private final Unique unique;
		private final int[] items;

		Reward(String title, Unique unique, int... items)
		{
			this.title = title;
			this.unique = unique;
			this.items = items;
		}

		public String getTitle()
		{
			return title;
		}

		public Unique unique()
		{
			return unique;
		}

		/** The items that show it owned; for the outfit, see {@link Piece}. */
		public int[] items()
		{
			return items.clone();
		}
	}

	/** The pyromancer outfit's pieces, in the wiki's order for ties: "Garb, hood, robe, and boots." */
	public enum Piece
	{
		GARB("Pyromancer garb", ItemID.PYROMANCER_TOP),
		HOOD("Pyromancer hood", ItemID.PYROMANCER_HOOD),
		ROBE("Pyromancer robe", ItemID.PYROMANCER_BOTTOM),
		BOOTS("Pyromancer boots", ItemID.PYROMANCER_BOOTS);

		private final String title;
		private final int item;

		Piece(String title, int item)
		{
			this.title = title;
			this.item = item;
		}

		public String getTitle()
		{
			return title;
		}

		public int item()
		{
			return item;
		}
	}

	/** The outfit as one item, the Pyromancer set (RuneLite's {@code WINT_PYRO_SET}): all four pieces owned. */
	public static final int PYROMANCER_SET = ItemID.WINT_PYRO_SET;

	/**
	 * What has been seen owned, saved as "1;GHW": a letter each for the garb, hood, robe and boots
	 * (G, H, R, B), warm gloves (W), bruma torch (T), tome of fire (F) and phoenix (P). Unknown
	 * letters are dropped. Never null.
	 */
	public static String decodeOwned(String text)
	{
		if (text == null || !text.startsWith("1;"))
		{
			return "";
		}
		StringBuilder b = new StringBuilder();
		for (char c : text.substring(2).toCharArray())
		{
			if (OWNED_CODES.indexOf(c) >= 0 && b.indexOf(String.valueOf(c)) < 0)
			{
				b.append(c);
			}
		}
		return b.toString();
	}

	/** The letters {@link #decodeOwned} knows. */
	public static final String OWNED_CODES = "GHRBWTFP";

	/** Rewards on average until {@code successes} of a unique come, at its chance a reward. */
	public static double expectedRewards(double perReward, int successes)
	{
		return perReward <= 0 ? Double.POSITIVE_INFINITY : successes / perReward;
	}

	/** Rewards for an even chance of at least one of a unique. */
	public static double evenOdds(double perReward)
	{
		return perReward <= 0 || perReward >= 1 ? 1 : Math.log(0.5) / Math.log(1 - perReward);
	}

	/** The chance of at least one of a unique in so many rewards. */
	public static double chanceIn(double perReward, double rewards)
	{
		return rewards <= 0 ? 0 : 1 - Math.pow(1 - perReward, rewards);
	}
}
