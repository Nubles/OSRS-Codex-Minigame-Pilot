package com.codex.minigamepilot.core;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * The facts of the Nightmare Zone, north of Yanille, as the OSRS wiki gives them (Nightmare Zone;
 * Dom Onion's Reward Shop; Herb box; Absorption; Overload (Nightmare Zone); Dominic Onion;
 * Dominic's coffer; Potion (Nightmare Zone, dream); the potion barrels and Rewards chest), quoted
 * beside each. Ids are RuneLite's gameval names. The arena's map region is as RuneLite's own
 * Nightmare Zone plugin reads it.
 *
 * <p>Points each boss gives are not here: the wiki's own strategy page says "Points per boss is
 * not accurate", and "Disabling bosses that are unlocked from completing quests reduces points per
 * kill". They are learnt from your own dreams instead ({@link NmzKills}).
 */
public final class Nmz
{
	/** The arena: "NMZ and the KBD lair uses the same region ID but NMZ uses planes 1-3" (RuneLite's NightmareZonePlugin, {@code NMZ_MAP_REGION = {9033}}). */
	public static final int ARENA_REGION = 9033;
	/** The lobby, as RuneLite's world map places the Nightmare Zone ({@code MinigameLocation.NIGHTMARE_ZONE}): 2606, 3115. */
	public static final int LOBBY_X = 2606;
	public static final int LOBBY_Y = 3115;

	/** Points this dream, as RuneLite's Nightmare Zone plugin reads them for its points an hour. */
	public static final int DREAM_POINTS = VarbitID.NZONE_CURRENTPOINTS;
	/** Reward points held at the chest. */
	public static final int REWARD_POINTS = VarPlayerID.NZONE_REWARDPOINTS;
	/** Absorption points left, as RuneLite's Nightmare Zone plugin reads them. */
	public static final int ABSORPTION = VarbitID.NZONE_ABSORB_POTION_EFFECTS;
	/** Overload refreshes left, each 25 ticks apart, as RuneLite's Timers and Buffs plugin reads them. */
	public static final int OVERLOAD = VarbitID.NZONE_OVERLOAD_POTION_EFFECTS;
	public static final int OVERLOAD_REFRESH_TICKS = 25;
	/**
	 * Dominic's coffer, in thousands of coins: unconfirmed. The name is RuneLite's; the unit is
	 * inferred from the wiki's "multiples of 1,000, up to a maximum of 262,143,000" (262,143 is the
	 * largest 18-bit number). A cost read from it that matches no dream is ignored.
	 */
	public static final int COFFER_THOUSANDS = VarbitID.NZONE_CASH;
	/**
	 * Doses in each barrel, by RuneLite's names {@code NZONE_POTION_1..4} beside the barrels
	 * {@code NZONE_BARREL_1..4}: unconfirmed, inferred from the names and the wiki's "can hold up to
	 * 255 doses of each potion" (eight bits each, four to the varp {@code NZONE_POTION_1_2_3_4}).
	 */
	static final int[] BARREL_DOSES = {VarbitID.NZONE_POTION_1, VarbitID.NZONE_POTION_2, VarbitID.NZONE_POTION_3, VarbitID.NZONE_POTION_4};
	/** "can hold up to 255 doses of each potion". */
	public static final int BARREL_MAX = 255;
	/** Any account type but a normal one (RuneLite's HiscorePlugin and DailyTasksPlugin read 0 as normal). */
	public static final int IRONMAN = VarbitID.IRONMAN;

	/** Absorption: "Each dose will absorb 50 damage, which stacks with previous unused Absorption damage up to 1,000 damage." */
	public static final int ABSORB_DOSE = 50;
	public static final int ABSORB_MAX = 1000;
	/** Overload: "Five minutes", boosts "reapplied every 15 seconds". */
	public static final int OVERLOAD_SECONDS = 300;

	/** Dominic Onion, NPC 1120: "hosts the Nightmare Zone minigame". */
	public static final int DOMINIC = NpcID.NZONE_HOST;
	/** The dream potion, object 26269, "filled by Dominic after the player requests a dream. Once filled, the player must drink it." */
	public static final int VIAL_FULL = ObjectID.NZONE_LOBBY_VIAL_FULL;
	/** Dominic's coffer, object 26272, and its other forms by RuneLite's names. */
	public static final int[] COFFER = {ObjectID.NZONE_LOBBY_MONEYPOUCH_UNLOCKED, ObjectID.NZONE_LOBBY_MONEYPOUCH_LOCKED, ObjectID.NZONE_LOBBY_MONEYPOUCH};
	/** The Rewards chest, object 26273: "Exchange your reward points here." */
	public static final int CHEST = ObjectID.NZONE_LOBBY_CHEST;

	/** Herb box: 9,500 points; "players can only purchase 15 herb boxes per day". */
	public static final int HERB_BOX_COST = 9_500;
	public static final int HERB_BOX_DAILY = 15;
	/** "The box rolls the herb drop table seven times" (since 25 June 2025). */
	public static final int HERB_BOX_ROLLS = 7;
	/** The herb drop table, out of 128: guam 32, marrentill 24, tarromin 18, harralander 14, ranarr 11, irit 8, avantoe 6, kwuarm 5, cadantine 4, lantadyme 3, dwarf weed 3. */
	static final int[][] HERB_TABLE = {
		{ItemID.UNIDENTIFIED_GUAM, 32}, {ItemID.UNIDENTIFIED_MARENTILL, 24}, {ItemID.UNIDENTIFIED_TARROMIN, 18},
		{ItemID.UNIDENTIFIED_HARRALANDER, 14}, {ItemID.UNIDENTIFIED_RANARR, 11}, {ItemID.UNIDENTIFIED_IRIT, 8},
		{ItemID.UNIDENTIFIED_AVANTOE, 6}, {ItemID.UNIDENTIFIED_KWUARM, 5}, {ItemID.UNIDENTIFIED_CADANTINE, 4},
		{ItemID.UNIDENTIFIED_LANTADYME, 3}, {ItemID.UNIDENTIFIED_DWARF_WEED, 3}};
	static final int HERB_TABLE_OF = 128;

	private Nmz()
	{
	}

	/**
	 * What a herb box is worth on average at today's prices: seven rolls of the herb table, each
	 * herb at its price. 0 without prices.
	 */
	public static double herbBoxValue(IntUnaryOperator price)
	{
		if (price == null)
		{
			return 0;
		}
		double each = 0;
		for (int[] h : HERB_TABLE)
		{
			each += Math.max(0, price.applyAsInt(h[0])) * (double) h[1] / HERB_TABLE_OF;
		}
		return each * HERB_BOX_ROLLS;
	}

	/**
	 * The potions counted into a dream: the four the reward shop sells by the dose ("Super ranging
	 * (1) 250", "Super magic potion (1) 250", "Overload (1) 1,500", "Absorption (1) 1,000"), each
	 * "only usable in the Nightmare Zone" and stored in its barrel, and the potions players bring
	 * from the bank. Ids from (4) to (1), by RuneLite's names; the four Nightmare Zone potions'
	 * ids are as their wiki pages give them (Absorption 11734 to 11737; Overload (1) 11733;
	 * Super ranging 11722 to 11725; Super magic potion 11726 to 11729).
	 */
	public enum Potion
	{
		OVERLOAD("Overload", "ovl", 'O', 1_500, ObjectID.NZONE_BARREL_3, 2,
			ItemID.NZONE4DOSEOVERLOADPOTION, ItemID.NZONE3DOSEOVERLOADPOTION, ItemID.NZONE2DOSEOVERLOADPOTION, ItemID.NZONE1DOSEOVERLOADPOTION),
		ABSORPTION("Absorption", "abs", 'A', 1_000, ObjectID.NZONE_BARREL_4, 3,
			ItemID.NZONE4DOSEABSORPTIONPOTION, ItemID.NZONE3DOSEABSORPTIONPOTION, ItemID.NZONE2DOSEABSORPTIONPOTION, ItemID.NZONE1DOSEABSORPTIONPOTION),
		SUPER_RANGING("Super ranging", "s.rng", 'R', 250, ObjectID.NZONE_BARREL_1, 0,
			ItemID.NZONE4DOSE2RANGERSPOTION, ItemID.NZONE3DOSE2RANGERSPOTION, ItemID.NZONE2DOSE2RANGERSPOTION, ItemID.NZONE1DOSE2RANGERSPOTION),
		SUPER_MAGIC("Super magic potion", "s.mag", 'M', 250, ObjectID.NZONE_BARREL_2, 1,
			ItemID.NZONE4DOSE2MAGICPOTION, ItemID.NZONE3DOSE2MAGICPOTION, ItemID.NZONE2DOSE2MAGICPOTION, ItemID.NZONE1DOSE2MAGICPOTION),
		PRAYER("Prayer potion", "pray", 'P', 0, -1, -1,
			ItemID._4DOSEPRAYERRESTORE, ItemID._3DOSEPRAYERRESTORE, ItemID._2DOSEPRAYERRESTORE, ItemID._1DOSEPRAYERRESTORE),
		SUPER_RESTORE("Super restore", "rest", 'S', 0, -1, -1,
			ItemID._4DOSE2RESTORE, ItemID._3DOSE2RESTORE, ItemID._2DOSE2RESTORE, ItemID._1DOSE2RESTORE),
		SUPER_COMBAT("Super combat", "scb", 'C', 0, -1, -1,
			ItemID._4DOSE2COMBAT, ItemID._3DOSE2COMBAT, ItemID._2DOSE2COMBAT, ItemID._1DOSE2COMBAT),
		RANGING("Ranging potion", "rng", 'G', 0, -1, -1,
			ItemID._4DOSERANGERSPOTION, ItemID._3DOSERANGERSPOTION, ItemID._2DOSERANGERSPOTION, ItemID._1DOSERANGERSPOTION),
		MAGIC("Magic potion", "mag", 'X', 0, -1, -1,
			ItemID._4DOSE1MAGIC, ItemID._3DOSE1MAGIC, ItemID._2DOSE1MAGIC, ItemID._1DOSE1MAGIC);

		private final String title;
		private final String brief;
		private final char code;
		private final int points;
		private final int barrel;
		private final int stock;
		private final int[] ids;

		Potion(String title, String brief, char code, int points, int barrel, int stock, int... ids)
		{
			this.title = title;
			this.brief = brief;
			this.code = code;
			this.points = points;
			this.barrel = barrel;
			this.stock = stock;
			this.ids = ids;
		}

		public String getTitle()
		{
			return title;
		}

		public char code()
		{
			return code;
		}

		/** A short label for a dream's line: "ovl", "abs". */
		public String brief()
		{
			return brief;
		}

		/** Reward points a dose at the chest, or 0 for a potion brought from the bank. */
		public int points()
		{
			return points;
		}

		/** Sold by the dose at the reward chest and kept in its barrel. */
		public boolean nightmare()
		{
			return points > 0;
		}

		/** Its barrel in the lobby (super ranging 26277, super magic 26278, overload 26279, absorption 26280), or -1. */
		public int barrel()
		{
			return barrel;
		}

		/** The varbit of doses in its barrel (unconfirmed, see {@link #BARREL_DOSES}), or -1. */
		public int stockVarbit()
		{
			return stock < 0 ? -1 : BARREL_DOSES[stock];
		}

		/** Ids from (4) to (1). */
		public int[] ids()
		{
			return ids.clone();
		}

		/** Doses of it one item holds, or 0 if the item is not this potion. */
		public int doses(int item)
		{
			for (int i = 0; i < ids.length; i++)
			{
				if (ids[i] == item)
				{
					return 4 - i;
				}
			}
			return 0;
		}

		public static Potion byCode(char c)
		{
			for (Potion p : values())
			{
				if (p.code == c)
				{
					return p;
				}
			}
			return null;
		}
	}

	/** The dream modes and their prices in thousands of coins, normal then hard: "Practice Free Free", "Endurance 1,000 5,000", "Rumble 2,000 6,000", "Custom rumble 22,000 26,000", with max quest points "12,000 16,000". */
	public enum Mode
	{
		PRACTICE("Practice", 'P', 0, 0),
		ENDURANCE("Endurance", 'E', 1, 5),
		RUMBLE("Rumble", 'R', 2, 6),
		CUSTOM("Customisable rumble", 'C', 22, 26),
		UNKNOWN("Dream", '?', -1, -1);

		/** Customisable rumble with maximum quest points: "12,000 16,000". */
		static final int CUSTOM_MAX_QP_NORMAL = 12;
		static final int CUSTOM_MAX_QP_HARD = 16;

		private final String title;
		private final char code;
		private final int normal;
		private final int hard;

		Mode(String title, char code, int normal, int hard)
		{
			this.title = title;
			this.code = code;
			this.normal = normal;
			this.hard = hard;
		}

		public String getTitle()
		{
			return title;
		}

		public char code()
		{
			return code;
		}

		/** Its price in thousands of coins, or -1. */
		public int cost(boolean hardMode)
		{
			return hardMode ? hard : normal;
		}

		public static Mode byCode(char c)
		{
			for (Mode m : values())
			{
				if (m.code == c)
				{
					return m;
				}
			}
			return UNKNOWN;
		}

		/** The mode a price paid on entering a dream (thousands) shows, or null when it matches none. "Upon entering the dream, coins from Dominic's coffer are deducted." */
		public static Mode byCost(int thousands)
		{
			switch (thousands)
			{
				case 1:
				case 5:
					return ENDURANCE;
				case 2:
				case 6:
					return RUMBLE;
				case 12:
				case 16:
				case 22:
				case 26:
					return CUSTOM;
				default:
					return null;
			}
		}

		/** Whether a price paid (thousands) is a hard dream's; only meaningful when {@link #byCost} matched. */
		public static boolean hardByCost(int thousands)
		{
			return thousands == 5 || thousands == 6 || thousands == 16 || thousands == 26;
		}
	}

	/**
	 * What reward points are saved for, from Dom Onion's Reward Shop: the herb box ("Herb box
	 * 9,500") and the imbues ("Base cost, with the 50% discount from Hard Combat Achievements").
	 */
	public enum Reward
	{
		HERB_BOX("Herb box", 9_500, false),
		BLACK_MASK("Black mask (i)", 1_250_000, true),
		SLAYER_HELMET("Slayer helmet (i)", 1_250_000, true),
		SALVE_AMULET("Salve amulet(i)", 800_000, true),
		SALVE_AMULET_E("Salve amulet(ei)", 800_000, true),
		RING_OF_SUFFERING("Ring of suffering (i)", 725_000, true),
		RING_OF_THE_GODS("Ring of the gods (i)", 650_000, true),
		BERSERKER_RING("Berserker ring (i)", 650_000, true),
		WARRIOR_RING("Warrior ring (i)", 650_000, true),
		ARCHERS_RING("Archers ring (i)", 650_000, true),
		SEERS_RING("Seers ring (i)", 650_000, true),
		TYRANNICAL_RING("Tyrannical ring (i)", 650_000, true),
		TREASONOUS_RING("Treasonous ring (i)", 650_000, true),
		GRANITE_RING("Granite ring (i)", 500_000, true);

		private final String title;
		private final int cost;
		private final boolean imbue;

		Reward(String title, int cost, boolean imbue)
		{
			this.title = title;
			this.cost = cost;
			this.imbue = imbue;
		}

		public String getTitle()
		{
			return title;
		}

		public boolean imbue()
		{
			return imbue;
		}

		/** Its price in points; an imbue at half with the Hard Combat Achievements done. */
		public int cost(boolean halved)
		{
			return imbue && halved ? cost / 2 : cost;
		}
	}

	/** Hard-mode bosses, by RuneLite's names ({@code NZONE_..._HARD}). */
	static final int[] HARD = {

		NpcID.NZONE_BRAIN_BARREL_CHEST_HARD, NpcID.NZONE_CONTACT_SCARAB_BOSS_HARD, NpcID.NZONE_BLOODDIAMOND_VAMPIREWARRIOR_HARD,
		NpcID.NZONE_ICEDIAMOND_ICEWARRIOR_HARD, NpcID.NZONE_FD_DAMIS_NORMAL_HARD, NpcID.NZONE_FD_DAMIS_TOUGHER_HARD,
		NpcID.NZONE_FIREDIAMOND_FIREWARRIOR_HARD, NpcID.NZONE_ELVARG_HARD, NpcID.NZONE_DREAM_INADEQUACY_HARD,
		NpcID.NZONE_DREAM_EVERLASTING_HARD, NpcID.NZONE_DREAM_UNTOUCHABLE_HARD, NpcID.NZONE_FAIRY_TANGLEFOOT_HARD,
		NpcID.NZONE_CHRONOZON_HARD, NpcID.NZONE_ARENA_BOUNCER_HARD, NpcID.NZONE_FRIS_TROLL_KING_TRUE_HARD,
		NpcID.NZONE_GRANDTREE_BLACKDEMON_HARD, NpcID.NZONE_GRIM_GLOD_HARD, NpcID.NZONE_HAUNTEDMINE_BOSS_GHOST_HARD,
		NpcID.NZONE_BLACK_KNIGHT_TITAN_HARD, NpcID.NZONE_HORROR_DAGGANOTH_AIR_HARD, NpcID.NZONE_HORROR_DAGGANOTH_WATER_HARD,
		NpcID.NZONE_HORROR_DAGGANOTH_FIRE_HARD, NpcID.NZONE_HORROR_DAGGANOTH_EARTH_HARD, NpcID.NZONE_HORROR_DAGGANOTH_RANGED_HARD,
		NpcID.NZONE_HORROR_DAGGANOTH_MELEE_HARD, NpcID.NZONE_CHICKENQUEST_EVIL_CHICKEN_HARD, NpcID.NZONE_HUNDRED_CULINAROMANCER_FINAL_HARD,
		NpcID.NZONE_HUNDRED_MINION1_HARD, NpcID.NZONE_HUNDRED_MINION2_HARD, NpcID.NZONE_HUNDRED_MINION3_HARD,
		NpcID.NZONE_HUNDRED_MINION4_HARD, NpcID.NZONE_HUNDRED_MINION5_AIR_HARD, NpcID.NZONE_HUNDRED_MINION5_MELEE_HARD,
		NpcID.NZONE_HUNDRED_MINION5_WATER_HARD, NpcID.NZONE_HUNDRED_MINION5_FIRE_HARD, NpcID.NZONE_HUNDRED_MINION5_RANGED_HARD,
		NpcID.NZONE_HUNDRED_MINION5_EARTH_HARD, NpcID.NZONE_NEZIKCHENED_HARD, NpcID.NZONE_TREE_SPIRIT_HARD,
		NpcID.NZONE_QUEST_LUNAR_MIRROR_OF_PLAYER_HARD, NpcID.NZONE_MM_DEMON_HARD, NpcID.NZONE_MDAUGHTER_BEARMAN_FIGHTER_HARD,
		NpcID.NZONE_MYARM_GIANT_ROC_HARD, NpcID.NZONE_SLAGILITH_HARD, NpcID.NZONE_ROVING_MOSSGIANT_HARD,
		NpcID.NZONE_SKELETON_HELLHOUND_HARD, NpcID.NZONE_AGRITH_NAAR_HARD, NpcID.NZONE_SUROK_KING_HARD,
		NpcID.NZONE_KHAZARD_WARLORD_HARD, NpcID.NZONE_TROLL_CHAMPION_HARD, NpcID.NZONE_TROLLROMANCE_ARRG_HARD,
		NpcID.NZONE_COUNT_DRAYNOR_HARD, NpcID.NZONE_SHAPESHIFTERGLOB_HARD, NpcID.NZONE_SHAPESHIFTERSPIDER_HARD,
		NpcID.NZONE_SHAPESHIFTERBEAR_HARD, NpcID.NZONE_SHAPESHIFTERWOLF_HARD, NpcID.NZONE_ZQ_MAINZOMBIE1_HARD,
		NpcID.NZONE_ZQ_MAINZOMBIE2_HARD, NpcID.NZONE_ZQ_MAINZOMBIE3_HARD, NpcID.NZONE_COW_HARD,
		NpcID.NZONE_HOSIDIUSQUEST_SNAKE_HARD, NpcID.NZONE_CORSAIRCURSE_NAVIGATOR_HARD, NpcID.NZONE_SHAYZIENQUEST_LIZARDMAN_HARD,
		NpcID.NZONE_ARCEUUSQUEST_SOUL_HARD, NpcID.NZONE_SOTE_ARIANWYN_HARD, NpcID.NZONE_SOTE_ESSYLLT_HARD,
		NpcID.NZONE_GA_BEAST_HARD
	};

	/** Normal-mode bosses, by RuneLite's names ({@code NZONE_..._NORMAL}). */
	static final int[] NORMAL = {

		NpcID.NZONE_BRAIN_BARREL_CHEST_NORMAL, NpcID.NZONE_CONTACT_SCARAB_BOSS_NORMAL, NpcID.NZONE_BLOODDIAMOND_VAMPIREWARRIOR_NORMAL,
		NpcID.NZONE_ICEDIAMOND_ICEWARRIOR_NORMAL, NpcID.NZONE_FD_DAMIS_NORMAL_NORMAL, NpcID.NZONE_FD_DAMIS_TOUGHER_NORMAL,
		NpcID.NZONE_FIREDIAMOND_FIREWARRIOR_NORMAL, NpcID.NZONE_ELVARG_NORMAL, NpcID.NZONE_DREAM_INADEQUACY_NORMAL,
		NpcID.NZONE_DREAM_EVERLASTING_NORMAL, NpcID.NZONE_DREAM_UNTOUCHABLE_NORMAL, NpcID.NZONE_FAIRY_TANGLEFOOT_NORMAL,
		NpcID.NZONE_CHRONOZON_NORMAL, NpcID.NZONE_ARENA_BOUNCER_NORMAL, NpcID.NZONE_FRIS_TROLL_KING_TRUE_NORMAL,
		NpcID.NZONE_GRANDTREE_BLACKDEMON_NORMAL, NpcID.NZONE_GRIM_GLOD_NORMAL, NpcID.NZONE_HAUNTEDMINE_BOSS_GHOST_NORMAL,
		NpcID.NZONE_BLACK_KNIGHT_TITAN_NORMAL, NpcID.NZONE_HORROR_DAGGANOTH_AIR_NORMAL, NpcID.NZONE_HORROR_DAGGANOTH_WATER_NORMAL,
		NpcID.NZONE_HORROR_DAGGANOTH_FIRE_NORMAL, NpcID.NZONE_HORROR_DAGGANOTH_EARTH_NORMAL, NpcID.NZONE_HORROR_DAGGANOTH_RANGED_NORMAL,
		NpcID.NZONE_HORROR_DAGGANOTH_MELEE_NORMAL, NpcID.NZONE_CHICKENQUEST_EVIL_CHICKEN_NORMAL, NpcID.NZONE_HUNDRED_CULINAROMANCER_FINAL_NORMAL,
		NpcID.NZONE_HUNDRED_MINION1_NORMAL, NpcID.NZONE_HUNDRED_MINION2_NORMAL, NpcID.NZONE_HUNDRED_MINION3_NORMAL,
		NpcID.NZONE_HUNDRED_MINION4_NORMAL, NpcID.NZONE_HUNDRED_MINION5_AIR_NORMAL, NpcID.NZONE_HUNDRED_MINION5_MELEE_NORMAL,
		NpcID.NZONE_HUNDRED_MINION5_WATER_NORMAL, NpcID.NZONE_HUNDRED_MINION5_FIRE_NORMAL, NpcID.NZONE_HUNDRED_MINION5_RANGED_NORMAL,
		NpcID.NZONE_HUNDRED_MINION5_EARTH_NORMAL, NpcID.NZONE_NEZIKCHENED_NORMAL, NpcID.NZONE_TREE_SPIRIT_NORMAL,
		NpcID.NZONE_QUEST_LUNAR_MIRROR_OF_PLAYER_NORMAL, NpcID.NZONE_MM_DEMON_NORMAL, NpcID.NZONE_MDAUGHTER_BEARMAN_FIGHTER_NORMAL,
		NpcID.NZONE_MYARM_GIANT_ROC_NORMAL, NpcID.NZONE_SLAGILITH_NORMAL, NpcID.NZONE_ROVING_MOSSGIANT_NORMAL,
		NpcID.NZONE_SKELETON_HELLHOUND_NORMAL, NpcID.NZONE_AGRITH_NAAR_NORMAL, NpcID.NZONE_SUROK_KING_NORMAL,
		NpcID.NZONE_KHAZARD_WARLORD_NORMAL, NpcID.NZONE_TROLL_CHAMPION_NORMAL, NpcID.NZONE_TROLLROMANCE_ARRG_NORMAL,
		NpcID.NZONE_COUNT_DRAYNOR_NORMAL, NpcID.NZONE_SHAPESHIFTERGLOB_NORMAL, NpcID.NZONE_SHAPESHIFTERSPIDER_NORMAL,
		NpcID.NZONE_SHAPESHIFTERBEAR_NORMAL, NpcID.NZONE_SHAPESHIFTERWOLF_NORMAL, NpcID.NZONE_ZQ_MAINZOMBIE1_NORMAL,
		NpcID.NZONE_ZQ_MAINZOMBIE2_NORMAL, NpcID.NZONE_ZQ_MAINZOMBIE3_NORMAL, NpcID.NZONE_COW_NORMAL,
		NpcID.NZONE_HOSIDIUSQUEST_SNAKE_NORMAL, NpcID.NZONE_CORSAIRCURSE_NAVIGATOR_NORMAL, NpcID.NZONE_SHAYZIENQUEST_LIZARDMAN_NORMAL,
		NpcID.NZONE_ARCEUUSQUEST_SOUL_NORMAL, NpcID.NZONE_SOTE_ARIANWYN_NORMAL, NpcID.NZONE_SOTE_ESSYLLT_NORMAL,
		NpcID.NZONE_GA_BEAST_NORMAL
	};

	static
	{
		Arrays.sort(HARD);
		Arrays.sort(NORMAL);
	}

	/** A boss of a hard dream. */
	public static boolean hard(int npc)
	{
		return Arrays.binarySearch(HARD, npc) >= 0;
	}

	/** A boss of a normal dream. */
	public static boolean normal(int npc)
	{
		return Arrays.binarySearch(NORMAL, npc) >= 0;
	}

	/** A dream's boss, of either difficulty. */
	public static boolean boss(int npc)
	{
		return hard(npc) || normal(npc);
	}

	/** In the arena: region 9033 above the ground floor. */
	public static boolean inArena(int region, int plane)
	{
		return region == ARENA_REGION && plane > 0;
	}

	/** A boss name as it is kept: the characters the saves use as separators taken out. */
	public static String cleanName(String name)
	{
		if (name == null)
		{
			return "";
		}
		String n = name.replaceAll("<[^>]*>", "").replaceAll("[,;|:+]", " ").replace(' ', ' ').trim().replaceAll("\\s+", " ");
		return n.length() > 40 ? n.substring(0, 40) : n;
	}
}
