package com.codex.minigamepilot.core;

import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * The facts of Soul Wars, the team minigame on the Isle of Souls, as the OSRS wiki gives them
 * (Soul Wars; Soul Wars/Rewards; Zeal Tokens; Spoils of war), quoted beside each. Ids are
 * RuneLite's gameval names; what the game's own client scripts do with them is quoted from
 * RuneLite's cs2 script dump where it matters.
 *
 * <p>Information only: nothing here is a cue for the fight.
 */
public final class SoulWars
{
	/**
	 * The arena: RuneLite's loot tracker ({@code SOUL_WARS_REGIONS = 8493, 8749, 9005}) and Discord
	 * plugin ({@code MG_SOUL_WARS ... 8493, 8748, 8749, 9005}); 8748 is the camp on the Isle of Souls.
	 */
	public static final int[] ARENA_REGIONS = {8493, 8749, 9005};
	/** Nomad's camp, as RuneLite's world map places Soul Wars ({@code MinigameLocation.SOUL_WARS}): 2209, 2855. */
	public static final int CAMP_X = 2209;
	public static final int CAMP_Y = 2855;

	/**
	 * Zeal Tokens held: the game's reward shop writes "Zeal Tokens: " with
	 * {@code %soul_wars_zeal_tokens} ({@code [proc,soul_wars_display_zeal]}).
	 */
	public static final int TOKENS = VarPlayerID.SOUL_WARS_ZEAL_TOKENS;
	/** Lifetime zeal: the same script writes "Lifetime Zeal: " with {@code %soul_wars_zeal}. */
	public static final int LIFETIME = VarPlayerID.SOUL_WARS_ZEAL;
	/** Games won, by RuneLite's name only: unconfirmed what the game counts in it. */
	public static final int TOTAL_WINS = VarPlayerID.SOUL_WARS_TOTAL_WINS;
	/** Games played, by RuneLite's name only: unconfirmed what the game counts in it. */
	public static final int TOTAL_GAMES = VarPlayerID.SOUL_WARS_TOTAL_GAMES;
	/**
	 * Your team, 1 blue and 2 red: unconfirmed (source: the Soul Wars plugin, which reads it so).
	 * The cape you wear is read first.
	 */
	public static final int TEAM = VarbitID.SOUL_WARS_TEAM;

	/** The game's overlay ({@code SoulWarsGame}): up while you are in a game; its clock. */
	public static final int HUD = InterfaceID.SoulWarsGame.CONTENT;
	public static final int HUD_TIME = InterfaceID.SoulWarsGame.TIME_LEFT;
	/** The lobby's overlay ({@code SoulWarsLobby}): players waiting and the time to the game. */
	public static final int LOBBY = InterfaceID.SoulWarsLobby.CONTENT;
	public static final int LOBBY_PLAYERS = InterfaceID.SoulWarsLobby.PLAYERS_TEXT;
	public static final int LOBBY_TIME = InterfaceID.SoulWarsLobby.TIME_TEXT;
	/** The end-of-game scroll ({@code SoulWarsGameEnd}): your part, the zeal it earned and the two teams' scores. */
	public static final int OVER = InterfaceID.SoulWarsGameEnd.CONTENTS;
	public static final int OVER_ZEAL = InterfaceID.SoulWarsGameEnd.ZEAL_EARNT_TEXT;
	public static final int OVER_BLUE = InterfaceID.SoulWarsGameEnd.BLUE_SCORE_TEXT;
	public static final int OVER_RED = InterfaceID.SoulWarsGameEnd.RED_SCORE_TEXT;
	public static final int OVER_KILLS = InterfaceID.SoulWarsGameEnd.PLAYERS_KILLED_TEXT;
	public static final int OVER_CAPTURES = InterfaceID.SoulWarsGameEnd.AREAS_CAPTURED_TEXT;
	public static final int OVER_DAMAGE = InterfaceID.SoulWarsGameEnd.AVATAR_DAMAGE_TEXT;
	public static final int OVER_FRAGMENTS = InterfaceID.SoulWarsGameEnd.OBELISK_SACRIFICES_TEXT;
	public static final int OVER_BONES = InterfaceID.SoulWarsGameEnd.BONES_BURIED_TEXT;

	/** "The capes worn in Soul Wars act like team capes". */
	public static final int BLUE_CAPE = ItemID.SOUL_WARS_BLUE_CAPE;
	public static final int RED_CAPE = ItemID.SOUL_WARS_RED_CAPE;

	/** "Players require a total level of 500 and a combat level of 40 to play Soul Wars." */
	public static final int TOTAL_LEVEL = 500;
	public static final int COMBAT_LEVEL = 40;
	/** "Each game lasts up to 15 minutes." */
	public static final int GAME_MINUTES = 15;
	/** "players on the winning team can get up to 60 zeal." */
	public static final int WIN_MAX = 60;
	/** "In the case of a draw players can get up to 50 zeal." */
	public static final int DRAW_MAX = 50;
	/** "Players on the losing team can get up to 40 zeal". */
	public static final int LOSS_MAX = 40;
	/** "Players must have at least level 30 in the chosen skill to purchase XP for it". */
	public static final int XP_LEVEL = 30;
	/** "No more than 1,000,000 experience can be redeemed with Zeal Tokens each day." */
	public static final int XP_DAILY_CAP = 1_000_000;
	/** Spoils of war: "It can be purchased from Nomad on the Isle of Souls for 30 Zeal Tokens." */
	public static final int SPOILS_COST = 30;
	/** Lil' Creator from a Spoils of War: "The Soul Wars pet rate is 1/400." */
	public static final int PET_RATE = 400;
	/** The most tokens a balance is taken to be: a guard against a damaged read. */
	public static final int TOKENS_CAP = 2_000_000_000;

	private SoulWars()
	{
	}

	/** In one of the arena's regions. */
	public static boolean inArena(int region)
	{
		for (int r : ARENA_REGIONS)
		{
			if (r == region)
			{
				return true;
			}
		}
		return false;
	}

	/** The two teams. */
	public enum Team
	{
		BLUE("Blue", 'b', 1),
		RED("Red", 'r', 2);

		private final String title;
		final char code;
		private final int varbit;

		Team(String title, char code, int varbit)
		{
			this.title = title;
			this.code = code;
			this.varbit = varbit;
		}

		public String getTitle()
		{
			return title;
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

		/** The team whose cape is worn, or null. */
		public static Team byCape(int itemId)
		{
			return itemId == BLUE_CAPE ? BLUE : itemId == RED_CAPE ? RED : null;
		}

		/** The team by {@link #TEAM}'s value, or null. */
		public static Team byVarbit(int value)
		{
			for (Team t : values())
			{
				if (t.varbit == value)
				{
					return t;
				}
			}
			return null;
		}
	}

	/**
	 * The skills zeal buys experience in, with the wiki's N: "If the skill is Prayer: N=14", "If the
	 * skill is Magic or Ranged: N=27", "If the skill is Attack, Strength, Defence or Hitpoints: N=30".
	 */
	public enum ZealSkill
	{
		ATTACK(Skill.ATTACK, 30),
		STRENGTH(Skill.STRENGTH, 30),
		DEFENCE(Skill.DEFENCE, 30),
		HITPOINTS(Skill.HITPOINTS, 30),
		RANGED(Skill.RANGED, 27),
		MAGIC(Skill.MAGIC, 27),
		PRAYER(Skill.PRAYER, 14);

		private final Skill skill;
		private final int n;

		ZealSkill(Skill skill, int n)
		{
			this.skill = skill;
			this.n = n;
		}

		public Skill skill()
		{
			return skill;
		}

		public int n()
		{
			return n;
		}

		public String getTitle()
		{
			return skill.getName();
		}

		@Override
		public String toString()
		{
			return getTitle();
		}

		/**
		 * Experience one Zeal Token buys at this level: the wiki's "Exp = floor(Level^2 / 600) x N",
		 * as the shop's own script works it ({@code [proc,script575]}: {@code stat_base * stat_base / 600 * N},
		 * 0 below level 30).
		 */
		public int xpPerToken(int level)
		{
			if (level < XP_LEVEL)
			{
				return 0;
			}
			int l = Math.min(126, level);
			return l * l / 600 * n;
		}
	}

	/**
	 * What zeal is saved for, from the Soul Wars Reward Shop ("owned by Nomad located on the Isle of
	 * Souls"): Soul cape 2,500; Ectoplasmator 250; Spoils of War 30; and the imbues, Black mask or
	 * Slayer helmet 500, Salve amulet 320, Ring of suffering 300, the Berserker, Warrior, Archers,
	 * Seers, Tyrannical, Treasonous rings and the Ring of the gods 260, the Granite ring 200.
	 */
	public enum Reward
	{
		/** Not sold: a 1/400 roll on each Spoils of War, so 400 of them on average. */
		LIL_CREATOR("Lil' Creator", SPOILS_COST * PET_RATE, false, false, ItemID.SOULWARSPET_BLUE, ItemID.SOULWARSPET_RED),
		SPOILS("Spoils of War", SPOILS_COST, true, false),
		SOUL_CAPE("Soul cape", 2_500, false, false, ItemID.SOUL_CAPE_BLUE, ItemID.SOUL_CAPE_RED),
		ECTOPLASMATOR("Ectoplasmator", 250, false, false, ItemID.SOUL_WARS_ECTOPLASMATOR),
		SLAYER_HELMET("Slayer helmet (i)", 500, false, true),
		SALVE("Salve amulet (i)", 320, false, true),
		SUFFERING("Ring of suffering (i)", 300, false, true),
		RING("A ring (i)", 260, false, true),
		GRANITE_RING("Granite ring (i)", 200, false, true),
		/** Experience in the chosen skill, to the chosen level. */
		SKILL_LEVEL("A level from zeal", 0, false, false);

		private final String title;
		private final int cost;
		private final boolean repeat;
		private final boolean imbue;
		private final int[] items;

		Reward(String title, int cost, boolean repeat, boolean imbue, int... items)
		{
			this.title = title;
			this.cost = cost;
			this.repeat = repeat;
			this.imbue = imbue;
			this.items = items;
		}

		public String getTitle()
		{
			return title;
		}

		/** Its price in Zeal Tokens: "The cost of imbuing items is halved if you have claimed the hard-tier Combat Achievements rewards." */
		public int cost(boolean halfImbue)
		{
			return imbue && halfImbue ? cost / 2 : cost;
		}

		public boolean repeat()
		{
			return repeat;
		}

		public boolean imbue()
		{
			return imbue;
		}

		/** The items that show it is yours, empty when it cannot be told. */
		public int[] items()
		{
			return items.clone();
		}

		/** A letter for the saved list of what you own. */
		public char code()
		{
			return (char) ('a' + ordinal());
		}
	}

	/** The chance of at least one Lil' Creator in this many Spoils of War: 1 - (399/400)^n. */
	public static double petChance(long spoils)
	{
		if (spoils <= 0)
		{
			return 0;
		}
		return 1 - Math.pow((PET_RATE - 1) / (double) PET_RATE, spoils);
	}

	/** Spoils of War for a chance of at least {@code p} at Lil' Creator. */
	public static long spoilsFor(double p)
	{
		if (p <= 0)
		{
			return 0;
		}
		return (long) Math.ceil(Math.log(1 - Math.min(p, 0.999999)) / Math.log((PET_RATE - 1) / (double) PET_RATE));
	}

	/**
	 * Zeal Tokens to reach {@code targetXp} from {@code xp} in a skill, bought a level at a time so
	 * each level's tokens buy at that level's rate (the rate is the level you have when you buy).
	 * -1 when the level is below 30, where zeal buys nothing.
	 */
	public static long tokensTo(ZealSkill s, long xp, long targetXp)
	{
		long tokens = 0;
		long at = Math.max(0, xp);
		while (at < targetXp)
		{
			int level = Goals.levelFor(at);
			int per = s.xpPerToken(level);
			if (per <= 0)
			{
				return -1;
			}
			long stop = level >= 99 ? targetXp : Math.min(targetXp, Goals.xpFor(level + 1));
			long n = (stop - at + per - 1) / per;
			tokens += n;
			at += n * per;
		}
		return tokens;
	}

	/** The first whole number in a component's text, tags and commas left out, or -1. */
	public static int number(String text)
	{
		return Brewing.number(text);
	}

	/** A component's text as the game writes it, tags left out and spaces tidied, or "". */
	public static String plain(String text)
	{
		return Brewing.plain(text);
	}
}
