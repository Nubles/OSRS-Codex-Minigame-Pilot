package com.codex.minigamepilot.core;

import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;

/**
 * The facts of the Tithe Farm, the Farming minigame in Hosidius: its three seeds with their
 * Farming levels and experience, how a plant grows, the watering cans, the fruit sack's points and
 * Farmer Gricoller's rewards.
 *
 * <p>Every figure is from the OSRS wiki (Tithe Farm; Tithe Farm/Strategies; Watering can;
 * Gricoller's can; Farmer's outfit; Seed box; Herb sack; Blighted Golovanova seedling); ids are
 * RuneLite's gameval names. A plant object's stage and whether it is watered is read from those
 * names ({@code HOSIDIUS_TITHE_A_1_DRY}, {@code _WET}, {@code _DEAD}, {@code HOSIDIUS_TITHE_A_4}).
 */
public final class Tithe
{
	/** "requiring level 34 Farming to play". */
	public static final int LEVEL = 34;
	/** "Each seed must be watered three times; before minutes 1, 2 and 3 respectively". */
	public static final int WATERINGS = 3;
	/** One growth stage: a minute, a hundred game ticks of 0.6 seconds. */
	public static final int STAGE_TICKS = 100;
	/** "A watering can holds eight doses." */
	public static final int CAN_DOSES = 8;
	/** Gricoller's can "holds 1,000 doses of water". */
	public static final int GRICOLLER_DOSES = 1000;
	/** Fruit is deposited "in batches of up to 100". */
	public static final int SACK = 100;
	/** "For every 3 plants a player deposits in the sack, they will earn 1 point". */
	public static final int FRUIT_PER_POINT = 3;
	/** "2 bonus points are awarded to players when they add the 100th fruit". */
	public static final int SACK_BONUS = 2;
	/** "Players can only possess a maximum of 16,000 points." */
	public static final int POINTS_CAP = 16_000;
	/** "Players receive bonus experience of 250 times the harvest experience rate when depositing the 75th fruit". */
	public static final int BONUS_FRUIT = 75;
	public static final int BONUS_TIMES = 250;
	/** Depositing gives ten times the harvest experience for the first 74 fruit and twenty for the 75th to 100th. */
	public static final int DEPOSIT_TIMES = 10;

	public static final int[] CANS = {ItemID.WATERING_CAN_1, ItemID.WATERING_CAN_2, ItemID.WATERING_CAN_3,
		ItemID.WATERING_CAN_4, ItemID.WATERING_CAN_5, ItemID.WATERING_CAN_6, ItemID.WATERING_CAN_7, ItemID.WATERING_CAN_8};

	private Tithe()
	{
	}

	/** The three seeds, from "Golovanova seed: Farming 34" to "Logavano seed: Farming 74". */
	public enum Seed
	{
		GOLOVANOVA("Golovanova", 34, 6, ItemID.HOSIDIUS_TITHE_SEED_A, ItemID.HOSIDIUS_TITHE_FRUIT_A),
		BOLOGANO("Bologano", 54, 14, ItemID.HOSIDIUS_TITHE_SEED_B, ItemID.HOSIDIUS_TITHE_FRUIT_B),
		LOGAVANO("Logavano", 74, 23, ItemID.HOSIDIUS_TITHE_SEED_C, ItemID.HOSIDIUS_TITHE_FRUIT_C);

		private final String title;
		private final int level;
		private final int harvestXp;
		private final int seed;
		private final int fruit;

		Seed(String title, int level, int harvestXp, int seed, int fruit)
		{
			this.title = title;
			this.level = level;
			this.harvestXp = harvestXp;
			this.seed = seed;
			this.fruit = fruit;
		}

		public String getTitle()
		{
			return title;
		}

		/** The Farming level; "Boosts can be used, but the level must be boosted before taking seeds and before planting them." */
		public int level()
		{
			return level;
		}

		/** Experience for harvesting one fruit, without the farmer's outfit. */
		public int harvestXp()
		{
			return harvestXp;
		}

		public int seed()
		{
			return seed;
		}

		public int fruit()
		{
			return fruit;
		}

		/**
		 * Experience a sack of 100 gives, harvest and deposit, without the outfit: 9,660,
		 * 22,540 and 37,030 on the wiki.
		 */
		public int sackXp()
		{
			return SACK * harvestXp + (BONUS_FRUIT - 1) * DEPOSIT_TIMES * harvestXp
				+ (SACK - BONUS_FRUIT + 1) * 2 * DEPOSIT_TIMES * harvestXp + BONUS_TIMES * harvestXp;
		}

		/** "players should pick the highest tier they qualify for": the best a Farming level grows, or null below 34. */
		public static Seed best(int level)
		{
			Seed best = null;
			for (Seed s : values())
			{
				if (s.level <= level)
				{
					best = s;
				}
			}
			return best;
		}
	}

	/** What a tithe patch shows. */
	public enum State
	{
		EMPTY,
		/** Planted and needing water before its stage ends. */
		DRY,
		/** Watered for this stage. */
		WET,
		/** Fully grown: "Fully grown plants cannot die." */
		GROWN,
		/** Blighted: "Clear" it. */
		DEAD
	}

	/** One patch object read: its state, its stage (1 to 4, 0 when empty) and its seed (null when empty). */
	public static final class Look
	{
		public final State state;
		public final int stage;
		public final Seed seed;

		Look(State state, int stage, Seed seed)
		{
			this.state = state;
			this.stage = stage;
			this.seed = seed;
		}

		/** Waterings this plant has had: a dry plant in stage n has had n - 1, a watered one n. */
		public int watered()
		{
			switch (state)
			{
				case DRY:
					return stage - 1;
				case WET:
					return stage;
				case GROWN:
					return WATERINGS;
				default:
					return 0;
			}
		}
	}

	/** Every patch object, in the order RuneLite names them: each seed's stages 1 to 3 dry, wet and blighted, then grown and blighted. */
	private static final int[][] PLANTS = {
		{ObjectID.HOSIDIUS_TITHE_A_1_DRY, ObjectID.HOSIDIUS_TITHE_A_1_WET, ObjectID.HOSIDIUS_TITHE_A_1_DEAD,
			ObjectID.HOSIDIUS_TITHE_A_2_DRY, ObjectID.HOSIDIUS_TITHE_A_2_WET, ObjectID.HOSIDIUS_TITHE_A_2_DEAD,
			ObjectID.HOSIDIUS_TITHE_A_3_DRY, ObjectID.HOSIDIUS_TITHE_A_3_WET, ObjectID.HOSIDIUS_TITHE_A_3_DEAD,
			ObjectID.HOSIDIUS_TITHE_A_4, ObjectID.HOSIDIUS_TITHE_A_4_DEAD},
		{ObjectID.HOSIDIUS_TITHE_B_1_DRY, ObjectID.HOSIDIUS_TITHE_B_1_WET, ObjectID.HOSIDIUS_TITHE_B_1_DEAD,
			ObjectID.HOSIDIUS_TITHE_B_2_DRY, ObjectID.HOSIDIUS_TITHE_B_2_WET, ObjectID.HOSIDIUS_TITHE_B_2_DEAD,
			ObjectID.HOSIDIUS_TITHE_B_3_DRY, ObjectID.HOSIDIUS_TITHE_B_3_WET, ObjectID.HOSIDIUS_TITHE_B_3_DEAD,
			ObjectID.HOSIDIUS_TITHE_B_4, ObjectID.HOSIDIUS_TITHE_B_4_DEAD},
		{ObjectID.HOSIDIUS_TITHE_C_1_DRY, ObjectID.HOSIDIUS_TITHE_C_1_WET, ObjectID.HOSIDIUS_TITHE_C_1_DEAD,
			ObjectID.HOSIDIUS_TITHE_C_2_DRY, ObjectID.HOSIDIUS_TITHE_C_2_WET, ObjectID.HOSIDIUS_TITHE_C_2_DEAD,
			ObjectID.HOSIDIUS_TITHE_C_3_DRY, ObjectID.HOSIDIUS_TITHE_C_3_WET, ObjectID.HOSIDIUS_TITHE_C_3_DEAD,
			ObjectID.HOSIDIUS_TITHE_C_4, ObjectID.HOSIDIUS_TITHE_C_4_DEAD}};

	/** The empty patch: "Tithe patch". */
	public static final int EMPTY = ObjectID.HOSIDIUS_TITHE_EMPTY;

	/** Every id a tithe patch can show, empty first. */
	public static int[] patchIds()
	{
		int[] out = new int[1 + PLANTS.length * PLANTS[0].length];
		out[0] = EMPTY;
		int k = 1;
		for (int[] seed : PLANTS)
		{
			for (int id : seed)
			{
				out[k++] = id;
			}
		}
		return out;
	}

	/** What a patch object shows, or null when the id is no tithe patch. */
	public static Look look(int id)
	{
		if (id == EMPTY)
		{
			return new Look(State.EMPTY, 0, null);
		}
		for (Seed s : Seed.values())
		{
			int[] ids = PLANTS[s.ordinal()];
			for (int k = 0; k < ids.length; k++)
			{
				if (ids[k] != id)
				{
					continue;
				}
				if (k == 9)
				{
					return new Look(State.GROWN, 4, s);
				}
				if (k == 10)
				{
					return new Look(State.DEAD, 4, s);
				}
				int stage = k / 3 + 1;
				State st = k % 3 == 0 ? State.DRY : k % 3 == 1 ? State.WET : State.DEAD;
				return new Look(st, stage, s);
			}
		}
		return null;
	}

	/** Water doses in watering cans carried: a can's number is its doses. */
	public static int canDoses(int[] cansByDoses)
	{
		int n = 0;
		for (int i = 0; i < cansByDoses.length; i++)
		{
			n += (i + 1) * cansByDoses[i];
		}
		return n;
	}

	/**
	 * Points a sack of fruit earns: a point for every three, and two more at the hundredth. A full
	 * sack is 35.
	 */
	public static int sackPoints(int fruit)
	{
		int f = Math.max(0, Math.min(SACK, fruit));
		return f / FRUIT_PER_POINT + (f >= SACK ? SACK_BONUS : 0);
	}

	/**
	 * The wiki's strategies, by plants a round, with the cans each asks for: the basic strategy
	 * ("A strategy for planting 20 seeds. If 20 is tedious, players can cut off at 16."; "Requires
	 * 8+ filled watering can"), the 23x method ("Requires 9 filled watering can") and the 25x4
	 * method ("Bring 10 watering cans").
	 */
	public enum Plan
	{
		SIXTEEN("16 plants", 16, 0),
		TWENTY("20 plants", 20, 8),
		TWENTY_THREE("23 plants", 23, 9),
		TWENTY_FIVE("25 plants", 25, 10);

		private final String title;
		private final int plants;
		private final int cans;

		Plan(String title, int plants, int cans)
		{
			this.title = title;
			this.plants = plants;
			this.cans = cans;
		}

		public String getTitle()
		{
			return title;
		}

		public int plants()
		{
			return plants;
		}

		/** Water doses a round takes: three a plant. */
		public int doses()
		{
			return plants * WATERINGS;
		}

		/** Full cans the round takes: the wiki's count, or the doses in eights where it names none. */
		public int cans()
		{
			return cans > 0 ? cans : (doses() + CAN_DOSES - 1) / CAN_DOSES;
		}
	}

	/** Farmer Gricoller's rewards, at their prices in points (wiki). */
	public enum Reward
	{
		FARMERS_OUTFIT("Farmer's outfit", 400, new int[][]{
			{ItemID.TITHE_REWARD_HAT_MALE, ItemID.TITHE_REWARD_HAT_FEMALE},
			{ItemID.TITHE_REWARD_TORSO_MALE, ItemID.TITHE_REWARD_TORSO_FEMALE},
			{ItemID.TITHE_REWARD_LEGS_MALE, ItemID.TITHE_REWARD_LEGS_FEMALE},
			{ItemID.TITHE_REWARD_FEET_MALE, ItemID.TITHE_REWARD_FEET_FEMALE}}, new int[]{75, 150, 125, 50}),
		STRAWHAT("Farmer's strawhat", 75, ItemID.TITHE_REWARD_HAT_MALE, ItemID.TITHE_REWARD_HAT_FEMALE),
		JACKET("Farmer's jacket or shirt", 150, ItemID.TITHE_REWARD_TORSO_MALE, ItemID.TITHE_REWARD_TORSO_FEMALE),
		TROUSERS("Farmer's boro trousers", 125, ItemID.TITHE_REWARD_LEGS_MALE, ItemID.TITHE_REWARD_LEGS_FEMALE),
		BOOTS("Farmer's boots", 50, ItemID.TITHE_REWARD_FEET_MALE, ItemID.TITHE_REWARD_FEET_FEMALE),
		GRICOLLERS_CAN("Gricoller's can", 200, ItemID.ZEAH_WATERINGCAN),
		SEED_BOX("Seed box", 250, ItemID.SEED_BOX, ItemID.SEED_BOX_OPEN),
		HERB_SACK("Herb sack", 250, ItemID.SLAYER_HERB_SACK, ItemID.SLAYER_HERB_SACK_OPEN);

		private final String title;
		private final int points;
		/** The items that show it owned, a set of ids for each piece. */
		private final int[][] pieces;
		private final int[] prices;

		Reward(String title, int points, int... ids)
		{
			this(title, points, new int[][]{ids}, new int[]{points});
		}

		Reward(String title, int points, int[][] pieces, int[] prices)
		{
			this.title = title;
			this.points = points;
			this.pieces = pieces;
			this.prices = prices;
		}

		public String getTitle()
		{
			return title;
		}

		public int points()
		{
			return points;
		}

		public int pieces()
		{
			return pieces.length;
		}

		public int[] piece(int i)
		{
			return pieces[i].clone();
		}

		public int price(int i)
		{
			return prices[i];
		}
	}
}
