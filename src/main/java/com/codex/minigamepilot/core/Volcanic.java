package com.codex.minigamepilot.core;

import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;

/**
 * The facts of the Volcanic Mine, under the Fossil Island Volcano, as the OSRS wiki gives them
 * (Volcanic Mine; Petrified Pete; Petrified Pete's Ore Shop; Prospector helmet, jacket, legs and
 * boots; Ash covered tome; Ore pack (Volcanic Mine); Fossil Island), quoted beside each. Ids are
 * RuneLite's gameval names. The mine's map regions are as RuneLite's own Discord plugin names
 * them.
 *
 * <p>What a varbit holds is, where RuneLite's own plugins do not read it, inferred from its
 * RuneLite name and cross-checked against the Plugin Hub's Volcanic Mine plugins; each says so.
 */
public final class Volcanic
{
	/**
	 * The mine: RuneLite's Discord plugin, {@code MG_VOLCANIC_MINE("Volcanic Mine",
	 * DiscordAreaType.MINIGAMES, 15263, 15262)}.
	 */
	public static final int REGION_NORTH = 15263;
	public static final int REGION_SOUTH = 15262;
	/** The lobby, as RuneLite's world map places the Volcanic Mine ({@code MinigameLocation.VOLCANIC_MINE}): 3812, 3810. */
	public static final int LOBBY_X = 3812;
	public static final int LOBBY_Y = 3810;

	/**
	 * Points this game, by RuneLite's name {@code FOSSIL_MINE_TEMP_PTS}: unconfirmed in the
	 * game's own words; the Hub's Volcanic Skiller plugin reads it as "points earned in the current
	 * game (resets each game)", and Ultimate Volcanic Mine as its points.
	 */
	public static final int GAME_POINTS = VarbitID.FOSSIL_MINE_TEMP_PTS;
	/**
	 * Whether a game is under way, by RuneLite's name {@code FOSSIL_MINE_PROGRESS_STATUS}:
	 * unconfirmed; Volcanic Skiller and Ultimate Volcanic Mine both read "0 = none, 1 = lobby,
	 * 2 = in game".
	 */
	public static final int PROGRESS = VarbitID.FOSSIL_MINE_PROGRESS_STATUS;
	public static final int PROGRESS_WAITING = 1;
	public static final int PROGRESS_PLAYING = 2;
	/** Mine stability in percent, by RuneLite's name {@code FOSSIL_MINE_CURRENT_STABILITY} (Ultimate Volcanic Mine reads it the same). */
	public static final int STABILITY = VarbitID.FOSSIL_MINE_CURRENT_STABILITY;
	/** Players in the game, by RuneLite's name {@code FOSSIL_MINE_NUMBERPLAYERS}. */
	public static final int PLAYERS = VarbitID.FOSSIL_MINE_NUMBERPLAYERS;
	/**
	 * Each chamber's gas pressure in percent once a vent has been checked, A, B and C, by RuneLite's
	 * names {@code FOSSIL_MINE_VENTA/B/C_STATUS}. Ultimate Volcanic Mine shows the value with a
	 * "%" and treats 127 as not yet checked: unconfirmed beyond that.
	 */
	public static final int[] VENTS = {VarbitID.FOSSIL_MINE_VENTA_STATUS, VarbitID.FOSSIL_MINE_VENTB_STATUS, VarbitID.FOSSIL_MINE_VENTC_STATUS};
	/** Above this a vent's value is not a pressure (127, not checked yet). */
	public static final int VENT_MAX = 100;
	/** The larger canteen, by RuneLite's name {@code FOSSIL_MINE_UNLOCKED_MOREWATER}: unconfirmed, inferred from the name. */
	public static final int UNLOCKED_WATER = VarbitID.FOSSIL_MINE_UNLOCKED_MOREWATER;
	/** The ash covered tome read, by RuneLite's name {@code FOSSIL_MINE_UNLOCKED_ULTRAFERTILESOIL}: unconfirmed, inferred from the name. */
	public static final int UNLOCKED_TOME = VarbitID.FOSSIL_MINE_UNLOCKED_ULTRAFERTILESOIL;

	/** The game's own overlay: its time left, as the game writes it ("Time" in RuneLite's {@code FossilVolcanicMine} interface). */
	public static final int HUD_TIME = InterfaceID.FossilVolcanicMine.TIME;
	/** The reward shop's points line (Volcanic Skiller reads the balance from it). */
	public static final int SHOP_POINTS = InterfaceID.FossilVolcanicShop.POINTS;

	/** Petrified Pete, NPC 7776: "runs Petrified Pete's Ore Shop at the entrance to the Volcanic Mine". */
	public static final int PETE = NpcID.FOSSIL_MINEGUARD;
	/** "a bank chest which players can use to prepare and bank rewards". */
	public static final int BANK_CHEST = ObjectID.FOSSIL_VOLCANO_BANK_CHEST;
	public static final int NUMULITE = ItemID.FOSSIL_NUMULITE;

	/** "To enter the mine, players must have level 50 Mining." */
	public static final int MINING_LEVEL = 50;
	/** "players must pay Petrified Pete 30 numulite each time to enter the mine." */
	public static final int ENTRY_FEE = 30;
	/** "A one-time fee of 3,000 numulite can also be paid for permanent access." */
	public static final int PERMANENT_FEE = 3_000;
	/** "Once the game starts, players have ten minutes to mine the boulder and leave." */
	public static final int GAME_SECONDS = 600;
	/** "The maximum amount of points a player can have is 2,097,151". */
	public static final int POINTS_CAP = 2_097_151;
	/** "Oh dear, you are dead!" (Death). */
	public static final String DEATH = "Oh dear, you are dead!";

	private Volcanic()
	{
	}

	/** In the mine: either of its two regions. */
	public static boolean inMine(int region)
	{
		return region == REGION_NORTH || region == REGION_SOUTH;
	}

	/** The prospector kit's pieces, at Petrified Pete's Ore Shop, with every form of each that counts as owned. */
	public enum Piece
	{
		/** "from the Volcanic Mine for 26,000 points" (Prospector helmet). */
		HELMET("Prospector helmet", 26_000, ItemID.MOTHERLODE_REWARD_HAT, ItemID.FOSSIL_MOTHERLODE_REWARD_HAT, ItemID.MOTHERLODE_REWARD_HAT_GOLD),
		/** "from Volcanic Mine for 39,000 points" (Prospector jacket). */
		JACKET("Prospector jacket", 39_000, ItemID.MOTHERLODE_REWARD_TOP, ItemID.FOSSIL_MOTHERLODE_REWARD_TOP, ItemID.MOTHERLODE_REWARD_TOP_GOLD),
		/** "Volcanic Mine for 34,000 points" (Prospector legs). */
		LEGS("Prospector legs", 34_000, ItemID.MOTHERLODE_REWARD_LEGS, ItemID.FOSSIL_MOTHERLODE_REWARD_LEGS, ItemID.MOTHERLODE_REWARD_LEGS_GOLD),
		/** "from Volcanic Mine for 21,000 points" (Prospector boots). */
		BOOTS("Prospector boots", 21_000, ItemID.MOTHERLODE_REWARD_BOOTS, ItemID.FOSSIL_MOTHERLODE_REWARD_BOOTS, ItemID.MOTHERLODE_REWARD_BOOTS_GOLD);

		private final String title;
		private final int cost;
		private final int[] items;

		Piece(String title, int cost, int... items)
		{
			this.title = title;
			this.cost = cost;
			this.items = items;
		}

		public String getTitle()
		{
			return title;
		}

		public int cost()
		{
			return cost;
		}

		public int[] items()
		{
			return items.clone();
		}

		/** A letter for the saved list of what you own. */
		public char code()
		{
			return "HJLB".charAt(ordinal());
		}
	}

	/** What points are saved for, at Petrified Pete's Ore Shop. */
	public enum Reward
	{
		/** The four pieces you do not own yet: 26,000 + 39,000 + 34,000 + 21,000. */
		PROSPECTOR_KIT("Prospector kit", 0, null, false),
		PROSPECTOR_HELMET("Prospector helmet", 0, Piece.HELMET, false),
		PROSPECTOR_JACKET("Prospector jacket", 0, Piece.JACKET, false),
		PROSPECTOR_LEGS("Prospector legs", 0, Piece.LEGS, false),
		PROSPECTOR_BOOTS("Prospector boots", 0, Piece.BOOTS, false),
		/** "Ash covered tome 40,000" (Petrified Pete's Ore Shop; Ash covered tome: "40,000 points"). */
		ASH_COVERED_TOME("Ash covered tome", 40_000, null, false),
		/** "unlocked the larger capacity for 10,000 points" (Volcanic Mine). */
		LARGE_WATER_CONTAINER("Large water container", 10_000, null, false),
		/** "Ore packs are an item purchased from the Volcanic Mine reward shop for 4,000 points." */
		ORE_PACK("Ore packs", 4_000, null, true);

		private final String title;
		private final int cost;
		private final Piece piece;
		private final boolean repeat;

		Reward(String title, int cost, Piece piece, boolean repeat)
		{
			this.title = title;
			this.cost = cost;
			this.piece = piece;
			this.repeat = repeat;
		}

		public String getTitle()
		{
			return title;
		}

		/** The one piece it is, or null. */
		public Piece piece()
		{
			return piece;
		}

		/** Bought again and again (ore packs), not once. */
		public boolean repeat()
		{
			return repeat;
		}

		/** Its price in points, the kit's being every piece's. */
		public int cost()
		{
			if (piece != null)
			{
				return piece.cost();
			}
			if (this == PROSPECTOR_KIT)
			{
				int n = 0;
				for (Piece p : Piece.values())
				{
					n += p.cost();
				}
				return n;
			}
			return cost;
		}
	}

	/** The time left as the game's overlay writes it ("9:41", "0:05"), in seconds, or -1. */
	public static int seconds(String text)
	{
		if (text == null)
		{
			return -1;
		}
		String t = text.replaceAll("<[^>]*>", "").trim();
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,2}):(\\d{2})").matcher(t);
		if (!m.find())
		{
			return -1;
		}
		int s = Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
		return s > 3600 ? -1 : s;
	}

	/** The points in the reward shop's points line ("Points: 12,345"), or -1. */
	public static int shopPoints(String text)
	{
		if (text == null)
		{
			return -1;
		}
		String digits = text.replaceAll("<[^>]*>", "").replaceAll("[^0-9]", "");
		if (digits.isEmpty() || digits.length() > 9)
		{
			return -1;
		}
		long n = Long.parseLong(digits);
		return n > POINTS_CAP ? -1 : (int) n;
	}
}
