package com.codex.minigamepilot.core;

import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * The facts of Trouble Brewing, the rum-making minigame on Mos Le'Harmless, as the OSRS wiki
 * gives them (Trouble Brewing; Honest Jimmy's House of Stuff; Bitternut; Pirate bandana (Trouble
 * Brewing); Pirate hat), quoted beside each. Ids are RuneLite's gameval names; the game's map
 * region and the lobby are as RuneLite's own Discord and world map plugins name them.
 *
 * <p>What an interface component's text holds is not documented by the game; the pilot reads the
 * number in it, and says so where it shows one.
 */
public final class Brewing
{
	/** The game: RuneLite's Discord plugin, {@code MG_TROUBLE_BREWING("Trouble Brewing", DiscordAreaType.MINIGAMES, 15150)}. */
	public static final int REGION = 15150;
	/** The lobby, as RuneLite's world map places it ({@code MinigameLocation.TROUBLE_BREWING}): 3811, 3021. */
	public static final int LOBBY_X = 3811;
	public static final int LOBBY_Y = 3021;

	/**
	 * Pieces of eight held, the player variable {@code brew_pieces}: the game's own shop scripts
	 * read and write it as the currency {@code omnishop_currency_brew_pieces}
	 * ({@code [proc,omnishop_var_getter]} in RuneLite's cs2-scripts). The wiki: pieces of eight
	 * "have been converted into a variable stored on the player's account rather than a physical item".
	 */
	public static final int PIECES = VarPlayerID.BREW_PIECES;
	/**
	 * Contribution points this game, by RuneLite's name {@code BREW_PLAYER_REWARD}: unconfirmed
	 * (source: the Hub's Trouble Brewing Rum plugin, which reads it as the contribution).
	 */
	public static final int CONTRIBUTION = VarbitID.BREW_PLAYER_REWARD;

	/** The game's overlay ({@code BrewOverlay}): its scores and its time left. */
	public static final int HUD_TOP = InterfaceID.BrewOverlay.CONTENT_TOP;
	public static final int HUD_RED_SCORE = InterfaceID.BrewOverlay.RED_SCORE;
	public static final int HUD_BLUE_SCORE = InterfaceID.BrewOverlay.BLUE_SCORE;
	public static final int HUD_TIME = InterfaceID.BrewOverlay.BREW_TIME_DISPLAY;
	/** The overlay's ingredient counts: "Along the top is amounts of ingredients." */
	public static final int HUD_BITTERNUT = InterfaceID.BrewOverlay.BITTERNUT_COUNT;
	public static final int HUD_SWEETGRUB = InterfaceID.BrewOverlay.SWEETGRUB_COUNT;
	public static final int HUD_BUCKET = InterfaceID.BrewOverlay.BUCKET_COUNT;
	public static final int HUD_COLOURED_WATER = InterfaceID.BrewOverlay.COLOURWATER_COUNT;
	public static final int HUD_BARK = InterfaceID.BrewOverlay.BARK_COUNT;
	/** The overlay's log count for each of the three boilers. */
	public static final int[] HUD_BOILERS = {InterfaceID.BrewOverlay.BOILER1_COUNT, InterfaceID.BrewOverlay.BOILER2_COUNT, InterfaceID.BrewOverlay.BOILER3_COUNT};
	/** The waiting room's overlay ({@code BrewWaitingRoomOverlay}): players on each side and the time to the game. */
	public static final int WAITING = InterfaceID.BrewWaitingRoomOverlay.CONTENTS;
	public static final int WAITING_RED = InterfaceID.BrewWaitingRoomOverlay.RED_COUNT;
	public static final int WAITING_BLUE = InterfaceID.BrewWaitingRoomOverlay.BLUE_COUNT;
	public static final int WAITING_TIME = InterfaceID.BrewWaitingRoomOverlay.TIME_TEXT;
	/** The game-over scroll ({@code BrewGameOver}): the final scores and the pieces of eight given. */
	public static final int OVER = InterfaceID.BrewGameOver.BREW_SCORE;
	public static final int OVER_PIECES = InterfaceID.BrewGameOver.BREW_PIECES_OF_EIGHT;
	public static final int OVER_RED = InterfaceID.BrewGameOver.BREW_DAN_FINAL_SCORE;
	public static final int OVER_BLUE = InterfaceID.BrewGameOver.BREW_SAN_FINAL_SCORE;

	/** Honest Jimmy, NPC 1816: "Honest Jimmy will sign you to whichever team currently has fewest members", and keeps the reward shop. */
	public static final int HONEST_JIMMY = NpcID.HONEST_JIMMY;
	public static final int SAN_FAN = NpcID.SAN_FAN;
	public static final int FANCY_DAN = NpcID.FANCY_DAN;

	/** "Pirate bandana", item 8949: "automatically equipped when joining her team" (San Fan's, the blue team). */
	public static final int BLUE_HAT = ItemID.BREW_BLUE_PIRATE_HAT;
	/** "Pirate hat", item 8950: "worn in Trouble Brewing to signify that a player is a member of Fancy Dan's team." */
	public static final int RED_HAT = ItemID.BREW_RED_PIRATE_HAT;

	/** "you must have completed the Cabin Fever quest and have at least 40 Cooking." */
	public static final int COOKING_LEVEL = 40;
	/** "Each game is 20 minutes long, with an interval of 3 minutes between games." */
	public static final int GAME_MINUTES = 20;
	public static final int INTERVAL_MINUTES = 3;
	/** "10 pieces of eight are awarded per bottle of rum produced by the team". */
	public static final int PIECES_PER_RUM = 10;
	/** "up to 100 pieces of eight are awarded, based on the contribution points gained during the game." */
	public static final int CONTRIBUTION_PIECES_MAX = 100;
	/** "There must be at least three logs in each boiler to make rum, and they can hold up to a maximum ten." */
	public static final int BOILER_LOGS_MIN = 3;
	/** The most pieces of eight a balance is taken to be: a guard against a damaged read. */
	public static final int PIECES_CAP = 2_000_000_000;

	private Brewing()
	{
	}

	/** In the game's region (the lobby is not). */
	public static boolean inGameRegion(int region)
	{
		return region == REGION;
	}

	/** The two teams. */
	public enum Team
	{
		/** "San Fan will sign you to the blue team". */
		BLUE("Blue", "San Fan", 'b'),
		/** "Fancy Dan will sign you to the red team." */
		RED("Red", "Fancy Dan", 'r');

		private final String title;
		private final String leader;
		final char code;

		Team(String title, String leader, char code)
		{
			this.title = title;
			this.leader = leader;
			this.code = code;
		}

		public String getTitle()
		{
			return title;
		}

		public String leader()
		{
			return leader;
		}

		public Team other()
		{
			return this == BLUE ? RED : BLUE;
		}

		static Team byCode(char c)
		{
			for (Team t : values())
			{
				if (t.code == c)
				{
					return t;
				}
			}
			return null;
		}

		/** The team whose hat is worn, or null. */
		public static Team byHat(int itemId)
		{
			return itemId == BLUE_HAT ? BLUE : itemId == RED_HAT ? RED : null;
		}
	}

	/**
	 * What goes into a bottle: "Each bottle of rum requires 3 bowls of coloured water.", "Each
	 * bottle of rum requires 5 buckets of normal water.", and one each of the rest (the wiki's
	 * rum recipe: 1 bitternut, 1 sweetgrub, 1 scrapey bark).
	 */
	public enum Ingredient
	{
		BITTERNUT("Bitternut", "bitternuts", 1, HUD_BITTERNUT),
		SWEETGRUB("Sweetgrubs", "sweetgrubs", 1, HUD_SWEETGRUB),
		SCRAPEY_BARK("Scrapey bark", "scrapey bark", 1, HUD_BARK),
		WATER("Water", "buckets of water", 5, HUD_BUCKET),
		COLOURED_WATER("Coloured water", "coloured water", 3, HUD_COLOURED_WATER);

		private final String title;
		private final String plural;
		private final int perBottle;
		private final int hud;

		Ingredient(String title, String plural, int perBottle, int hud)
		{
			this.title = title;
			this.plural = plural;
			this.perBottle = perBottle;
			this.hud = hud;
		}

		public String getTitle()
		{
			return title;
		}

		public String plural()
		{
			return plural;
		}

		public int perBottle()
		{
			return perBottle;
		}

		/** The overlay's count of it. */
		public int hud()
		{
			return hud;
		}
	}

	/** The colour sets of naval clothing, each a shirt, a tricorn hat and slacks. */
	public enum Colour
	{
		BLUE("Blue", ItemID.BREW_UNIFORM_BLUE, ItemID.BREW_TRICORN_BLUE, ItemID.BREW_NAVY_SLACKS_BLUE),
		GREEN("Green", ItemID.BREW_UNIFORM_GREEN, ItemID.BREW_TRICORN_GREEN, ItemID.BREW_NAVY_SLACKS_GREEN),
		RED("Red", ItemID.BREW_UNIFORM_RED, ItemID.BREW_TRICORN_RED, ItemID.BREW_NAVY_SLACKS_RED),
		BROWN("Brown", ItemID.BREW_UNIFORM_BROWN, ItemID.BREW_TRICORN_BROWN, ItemID.BREW_NAVY_SLACKS_BROWN),
		BLACK("Black", ItemID.BREW_UNIFORM_BLACK, ItemID.BREW_TRICORN_BLACK, ItemID.BREW_NAVY_SLACKS_BLACK),
		PURPLE("Purple", ItemID.BREW_UNIFORM_PURPLE, ItemID.BREW_TRICORN_PURPLE, ItemID.BREW_NAVY_SLACKS_PURPLE),
		GREY("Grey", ItemID.BREW_UNIFORM_GREY, ItemID.BREW_TRICORN_GREY, ItemID.BREW_NAVY_SLACKS_GREY);

		private final String title;
		final int shirt;
		final int hat;
		final int slacks;

		Colour(String title, int shirt, int hat, int slacks)
		{
			this.title = title;
			this.shirt = shirt;
			this.hat = hat;
			this.slacks = slacks;
		}

		public String getTitle()
		{
			return title;
		}
	}

	/**
	 * Each thing Honest Jimmy's House of Stuff sells once, with its price: naval shirts and navy
	 * slacks "1,000", tricorn hats "500"; Cutthroat flag and Gilded smile flag "2,000", Bronze fist
	 * flag "3,000", Lucky shot flag "4,000", Treasure flag "5,000", Phasmatys flag "6,000".
	 */
	public enum Piece
	{
		BLUE_SHIRT(Colour.BLUE, 0), BLUE_HAT(Colour.BLUE, 1), BLUE_SLACKS(Colour.BLUE, 2),
		GREEN_SHIRT(Colour.GREEN, 0), GREEN_HAT(Colour.GREEN, 1), GREEN_SLACKS(Colour.GREEN, 2),
		RED_SHIRT(Colour.RED, 0), RED_HAT(Colour.RED, 1), RED_SLACKS(Colour.RED, 2),
		BROWN_SHIRT(Colour.BROWN, 0), BROWN_HAT(Colour.BROWN, 1), BROWN_SLACKS(Colour.BROWN, 2),
		BLACK_SHIRT(Colour.BLACK, 0), BLACK_HAT(Colour.BLACK, 1), BLACK_SLACKS(Colour.BLACK, 2),
		PURPLE_SHIRT(Colour.PURPLE, 0), PURPLE_HAT(Colour.PURPLE, 1), PURPLE_SLACKS(Colour.PURPLE, 2),
		GREY_SHIRT(Colour.GREY, 0), GREY_HAT(Colour.GREY, 1), GREY_SLACKS(Colour.GREY, 2),
		CUTTHROAT_FLAG("Cutthroat flag", 2_000, ItemID.BREW_FLAG_1),
		GILDED_SMILE_FLAG("Gilded smile flag", 2_000, ItemID.BREW_FLAG_2),
		BRONZE_FIST_FLAG("Bronze fist flag", 3_000, ItemID.BREW_FLAG_3),
		LUCKY_SHOT_FLAG("Lucky shot flag", 4_000, ItemID.BREW_FLAG_4),
		TREASURE_FLAG("Treasure flag", 5_000, ItemID.BREW_FLAG_5),
		PHASMATYS_FLAG("Phasmatys flag", 6_000, ItemID.BREW_FLAG_6);

		private final String title;
		private final int cost;
		private final int item;
		private final Colour colour;

		Piece(Colour colour, int part)
		{
			this.colour = colour;
			this.title = colour.getTitle() + (part == 0 ? " naval shirt" : part == 1 ? " tricorn hat" : " navy slacks");
			this.cost = part == 1 ? 500 : 1_000;
			this.item = part == 0 ? colour.shirt : part == 1 ? colour.hat : colour.slacks;
		}

		Piece(String title, int cost, int item)
		{
			this.colour = null;
			this.title = title;
			this.cost = cost;
			this.item = item;
		}

		public String getTitle()
		{
			return title;
		}

		public int cost()
		{
			return cost;
		}

		public int item()
		{
			return item;
		}

		/** The naval set it belongs to, or null for a flag. */
		public Colour colour()
		{
			return colour;
		}

		public boolean flag()
		{
			return colour == null;
		}

		/** A letter or digit for the saved list of what you own. */
		public char code()
		{
			return CODES.charAt(ordinal());
		}

		static final String CODES = "abcdefghijklmnopqrstu123456";
	}

	/** What pieces of eight are saved for. */
	public enum Reward
	{
		/**
		 * Every flag and naval piece: the wiki's "Buying every item in the shop costs 39,590" less
		 * the two rums and the stuff (20 + 20 + 50), which are used up and bought again.
		 */
		EVERYTHING("Every flag and naval piece", 0, false),
		NAVAL_BLUE("Blue naval set", 0, false),
		NAVAL_GREEN("Green naval set", 0, false),
		NAVAL_RED("Red naval set", 0, false),
		NAVAL_BROWN("Brown naval set", 0, false),
		NAVAL_BLACK("Black naval set", 0, false),
		NAVAL_PURPLE("Purple naval set", 0, false),
		NAVAL_GREY("Grey naval set", 0, false),
		/** The six flags: 2,000 + 2,000 + 3,000 + 4,000 + 5,000 + 6,000. */
		FLAGS("Every flag", 0, false),
		/** "a medium clue scroll (referred to by Jimmy as "map") for 5,000 pieces of eight". */
		MEDIUM_CLUE("Medium clue scrolls", 5_000, true),
		/** "Rum (red) 20", "Rum (blue) 20": a teleport back to Trouble Brewing. */
		RUM("Rum", 20, true),
		/** "The stuff 50". */
		THE_STUFF("The stuff", 50, true);

		private final String title;
		private final int cost;
		private final boolean repeat;

		Reward(String title, int cost, boolean repeat)
		{
			this.title = title;
			this.cost = cost;
			this.repeat = repeat;
		}

		public String getTitle()
		{
			return title;
		}

		public boolean repeat()
		{
			return repeat;
		}

		/** The pieces it is made of, for a set or every reward; empty for one bought again and again. */
		public java.util.List<Piece> pieces()
		{
			java.util.List<Piece> out = new java.util.ArrayList<>();
			for (Piece p : Piece.values())
			{
				boolean in = this == EVERYTHING
					|| (this == FLAGS && p.flag())
					|| (p.colour() != null && name().equals("NAVAL_" + p.colour().name()));
				if (in)
				{
					out.add(p);
				}
			}
			return out;
		}

		/** Its full price in pieces of eight. */
		public int cost()
		{
			if (repeat)
			{
				return cost;
			}
			int n = 0;
			for (Piece p : pieces())
			{
				n += p.cost();
			}
			return n;
		}
	}

	/** The first whole number in a component's text, tags and commas left out, or -1. */
	public static int number(String text)
	{
		if (text == null)
		{
			return -1;
		}
		String t = text.replaceAll("<[^>]*>", "").replace(",", "");
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d{1,9}").matcher(t);
		return m.find() ? Integer.parseInt(m.group()) : -1;
	}

	/** A component's text as the game writes it, tags left out and spaces tidied, or "". */
	public static String plain(String text)
	{
		return text == null ? "" : text.replaceAll("<br>", " ").replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim();
	}
}
