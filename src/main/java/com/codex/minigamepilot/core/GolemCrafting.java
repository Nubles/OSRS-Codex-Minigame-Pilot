package com.codex.minigamepilot.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;

/**
 * Golem crafting's facts, each from the OSRS wiki (Golem crafting, Sunstone, Sunstone core, Jeweller's
 * chisel, Fallen From Grace) with its ids from RuneLite's gameval, and the gift message from RuneLite's
 * own loot tracker.
 *
 * <ul>
 * <li>"Golem crafting is a Crafting training method located on the island of Wyrmscraig." "As such, the
 * activity itself requires level 60 Crafting to do." Fallen From Grace gives "Access to the Crafting
 * minigame Golem crafting".</li>
 * <li>"Begin the crafting process by clicking an empty plinth. This requires a chisel, a hammer, and four
 * sunstones per golem." A sunstone core ("using a chisel on a sunstone", 10 Crafting experience) and a
 * fur complete it: "requiring 1 sunstone core and 1 fur per golem."</li>
 * <li>"Crafting a golem gives a base of 1560 XP"; "The higher quality the fur is, the more Crafting
 * experience is given." The fur table gives each fur's Fur XP and Total XP; "Only the furs listed below
 * are usable for golem crafting, and tatty variants will not work."</li>
 * <li>"Each successfully crafted golem awards a gem": uncut sapphire 15/23, emerald 5/23, ruby 2/23,
 * diamond 1/23; Jeweller's chisel 1/300.</li>
 * </ul>
 */
public final class GolemCrafting
{
	private GolemCrafting()
	{
	}

	/** Crafting level the activity needs. */
	public static final int CRAFTING_LEVEL = 60;
	/** Crafting experience a golem gives before its fur. */
	public static final int BASE_XP = 1_560;
	/** Sunstones carved into one golem (its core is a fifth). */
	public static final int SUNSTONE_PER_GOLEM = 4;
	/** Crafting experience for chiselling a sunstone into a core. */
	public static final int CORE_XP = 10;
	/** The Jeweller's chisel is 1 in this many golems. */
	public static final int CHISEL_ODDS = 300;

	public static final int SUNSTONE = ItemID.SUNSTONE;
	public static final int SUNSTONE_CORE = ItemID.SUNSTONE_CORE;
	public static final int CHISEL = ItemID.CHISEL;
	public static final int JEWELLERS_CHISEL = ItemID.JEWELLERS_CHISEL;
	public static final int[] HAMMERS = {ItemID.HAMMER, ItemID.IMCANDO_HAMMER, ItemID.IMCANDO_HAMMER_OFFHAND};

	/** The two golem stations in every state (empty plinth, base, unfinished, unpowered), the sunstone rocks and the monolith. */
	private static final int[] PLACES = {
		ObjectID.GOLEM_STATION_1, ObjectID.GOLEM_STATION_2, ObjectID.GOLEM_STATION_EMPTY, ObjectID.GOLEM_STATION_BASE,
		ObjectID.GOLEM_STATION_SHAPED_1, ObjectID.GOLEM_STATION_SHAPED_2, ObjectID.GOLEM_STATION_SHAPED_3,
		ObjectID.GOLEM_STATION_SHAPED_4, ObjectID.SUNSTONEROCK1, ObjectID.SUNSTONEROCK2, ObjectID.WYRMSCRAIG_SUNSTONE01,
	};

	/**
	 * The gift line, as RuneLite's loot tracker reads it: "As you complete the golem it leaves a gift on the
	 * ground for you: 1 x Uncut sapphire." (or "in your gem bag", "in your gem sack").
	 */
	private static final Pattern GIFT = Pattern.compile("As you complete the golem it leaves a gift "
		+ "(?:on the ground|in your gem (?:bag|sack)) for you: 1 x "
		+ "(Uncut diamond|Uncut emerald|Uncut ruby|Uncut sapphire|Jeweller's chisel)\\.?$");

	/** What a golem leaves, with the wiki's rate. */
	public enum Gift
	{
		SAPPHIRE('s', "Uncut sapphire", ItemID.UNCUT_SAPPHIRE, 15, 23),
		EMERALD('e', "Uncut emerald", ItemID.UNCUT_EMERALD, 5, 23),
		RUBY('r', "Uncut ruby", ItemID.UNCUT_RUBY, 2, 23),
		DIAMOND('d', "Uncut diamond", ItemID.UNCUT_DIAMOND, 1, 23),
		CHISEL('c', "Jeweller's chisel", ItemID.JEWELLERS_CHISEL, 1, CHISEL_ODDS);

		final char code;
		private final String title;
		private final int item;
		private final int num;
		private final int den;

		Gift(char code, String title, int item, int num, int den)
		{
			this.code = code;
			this.title = title;
			this.item = item;
			this.num = num;
			this.den = den;
		}

		public String getTitle()
		{
			return title;
		}

		public int item()
		{
			return item;
		}

		/** "15/23" */
		public String odds()
		{
			return num + "/" + den;
		}

		/** The chance a golem leaves it, 0..1. */
		public double rate()
		{
			return num / (double) den;
		}

		/** One of the four gems, not the chisel. */
		public boolean gem()
		{
			return this != CHISEL;
		}

		static Gift byCode(char c)
		{
			for (Gift g : values())
			{
				if (g.code == c)
				{
					return g;
				}
			}
			return null;
		}

		static Gift byTitle(String s)
		{
			for (Gift g : values())
			{
				if (g.title.equalsIgnoreCase(s))
				{
					return g;
				}
			}
			return null;
		}
	}

	/** The furs a golem takes, best first: the wiki's table, with each fur's Hunter level and Fur XP. */
	public enum Fur
	{
		MOONLIGHT_ANTELOPE('m', "Moonlight antelope fur", ItemID.HUNTING_ANTELOPEMOON_FUR, 91, 1_200),
		SUNLIGHT_ANTELOPE('u', "Sunlight antelope fur", ItemID.HUNTING_ANTELOPESUN_FUR, 72, 1_180),
		DASHING_KEBBIT('a', "Dashing kebbit fur", ItemID.HUNTINGBEAST_SPEEDY2_FUR, 69, 1_160),
		KYATT('k', "Kyatt fur", ItemID.HUNTING_FUR_TIGER_PERFECT, 55, 1_140),
		DARK_KEBBIT('d', "Dark kebbit fur", ItemID.HUNTINGBEAST_SILENT_FUR, 57, 1_140),
		FOX('f', "Fox fur", ItemID.HUNTING_FENNECFOX_FUR, 57, 1_120),
		SPOTTED_KEBBIT('s', "Spotted kebbit fur", ItemID.HUNTINGBEAST_SPEEDY_FUR, 43, 1_100),
		GRAAHK('g', "Graahk fur", ItemID.HUNTING_FUR_LEOPARD_PERFECT, 41, 1_080),
		LARUPIA('l', "Larupia fur", ItemID.HUNTING_FUR_JAGUAR_PERFECT, 31, 1_060),
		FELDIP_WEASEL('w', "Feldip weasel fur", ItemID.HUNTINGBEAST_JUNGLE_FUR, 7, 1_030),
		COMMON_KEBBIT('c', "Common kebbit fur", ItemID.HUNTINGBEAST_WOODLAND_FUR, 3, 1_000),
		DESERT_DEVIL('e', "Desert devil fur", ItemID.HUNTINGBEAST_DESERT_FUR, 13, 960),
		POLAR_KEBBIT('p', "Polar kebbit fur", ItemID.HUNTINGBEAST_POLAR_FUR, 1, 920),
		WYRMSCRAIG_GOAT('y', "Wyrmscraig goat fur", ItemID.GOAT_PIT_FUR, 60, 880);

		final char code;
		private final String title;
		private final int item;
		private final int hunter;
		private final int furXp;

		Fur(char code, String title, int item, int hunter, int furXp)
		{
			this.code = code;
			this.title = title;
			this.item = item;
			this.hunter = hunter;
			this.furXp = furXp;
		}

		public String getTitle()
		{
			return title;
		}

		/** "Dark kebbit" */
		public String shortTitle()
		{
			return title.endsWith(" fur") ? title.substring(0, title.length() - 4) : title;
		}

		public int item()
		{
			return item;
		}

		/** The Hunter level the wiki's table gives for the fur. */
		public int hunter()
		{
			return hunter;
		}

		public int furXp()
		{
			return furXp;
		}

		/** Crafting experience a golem gives with this fur: the base and the fur's. */
		public int totalXp()
		{
			return BASE_XP + furXp;
		}

		static Fur byCode(char c)
		{
			for (Fur f : values())
			{
				if (f.code == c)
				{
					return f;
				}
			}
			return null;
		}

		public static Fur byItem(int id)
		{
			for (Fur f : values())
			{
				if (f.item == id)
				{
					return f;
				}
			}
			return null;
		}
	}

	/** The gift a game message names, or null when it is not a golem's gift line. Tags already removed. */
	public static Gift gift(String message)
	{
		if (message == null)
		{
			return null;
		}
		Matcher m = GIFT.matcher(message.trim());
		return m.find() ? Gift.byTitle(m.group(1)) : null;
	}

	/** Whether an object is one of the golem stations, the sunstone rocks or the monolith. */
	public static boolean isWatched(int id)
	{
		for (int p : PLACES)
		{
			if (p == id)
			{
				return true;
			}
		}
		return false;
	}

	/** Every watched object id, for scanning a scene. */
	public static int[] places()
	{
		return PLACES.clone();
	}

	/**
	 * Golems these sunstones and cores can make: four sunstones and a core each, and a sunstone chiselled
	 * into a core when there are too few.
	 */
	public static int golemsFrom(int sunstone, int cores)
	{
		int s = Math.max(0, sunstone);
		int c = Math.max(0, cores);
		int byCores = Math.min(c, s / SUNSTONE_PER_GOLEM);
		if (byCores == c)
		{
			// Every core has its four; the sunstone left over makes golems of five, one chiselled.
			return c + (s - SUNSTONE_PER_GOLEM * c) / (SUNSTONE_PER_GOLEM + 1);
		}
		return byCores;
	}

	/** The chance of at least one chisel in this many golems, 0..1. */
	public static double chiselChance(long golems)
	{
		return golems <= 0 ? 0 : 1 - Math.pow(1 - 1.0 / CHISEL_ODDS, golems);
	}

	/** Golems for this chance of a chisel (0.5 for even odds). */
	public static long golemsForChisel(double chance)
	{
		return (long) Math.ceil(Math.log(1 - chance) / Math.log(1 - 1.0 / CHISEL_ODDS));
	}
}
