package com.codex.minigamepilot.core;

import net.runelite.api.gameval.ItemID;

/**
 * The facts of Vale Totems, the Fletching activity in Auburn Valley, Varlamore: its eight totem
 * sites, the six woods with their Fletching levels and the fletched items that decorate each, the
 * five spirits a totem is carved with, the offerings ents leave and the Vale Research Exchange's
 * prices.
 *
 * <p>Every figure is from the OSRS wiki (Vale Totems; Oak totem; Redwood totem; Offering site; Ent
 * trail; Vale offerings; Vale Research Points; Vale Research Exchange; the five spirit pages); ids
 * are RuneLite's gameval names. How the game numbers the spirits in its varbits (1 to 5 for
 * {@link Animal#ordinal()} + 1) is as the Totem Fletching plugin by TheRealGuru (BSD-2) reads
 * them: a cross-check, not a source. The Pilot reads the carving choices by the icons the game
 * draws in them where it can.
 */
public final class Totems
{
	/** "There are eight totems in the valley." */
	public static final int SITES = 8;
	/** "of the 5 spirit variants, only 3 will be active near a totem site at any time." */
	public static final int CARVINGS = 3;
	/** "one to build the totem and four fletched items for decorations" */
	public static final int DECORATIONS = 4;
	/** Offerings at a site from which no new totem can be built there until some are claimed. */
	public static final int OFFERINGS_LIMIT = 15_000;
	/** "Rummaging the offerings consumes 100 of them for one Vale Research Point". */
	public static final int OFFERINGS_PER_POINT = 100;
	/** "Every 80 ticks (48 seconds), Ents travel between these sites". */
	public static final int ENT_TICKS = 80;
	/** Per cent less decoration experience for each wrong carving. */
	public static final int WRONG_CARVING_COST = 25;
	/** Per cent bonus experience from a pair of ent trails. */
	public static final int TRAIL_BONUS = 15;

	private Totems()
	{
	}

	/** The six woods, with the Fletching level each needs ("Oak: 20 Fletching" to "Redwood: 90 Fletching"). */
	public enum Wood
	{
		OAK("Oak", 20, ItemID.OAK_LOGS, ItemID.UNSTRUNG_OAK_SHORTBOW, ItemID.OAK_SHORTBOW, ItemID.UNSTRUNG_OAK_LONGBOW,
			ItemID.OAK_LONGBOW, ItemID.OAK_SHIELD, ItemID.XBOWS_CROSSBOW_STOCK_OAK),
		WILLOW("Willow", 35, ItemID.WILLOW_LOGS, ItemID.UNSTRUNG_WILLOW_SHORTBOW, ItemID.WILLOW_SHORTBOW,
			ItemID.UNSTRUNG_WILLOW_LONGBOW, ItemID.WILLOW_LONGBOW, ItemID.WILLOW_SHIELD, ItemID.XBOWS_CROSSBOW_STOCK_WILLOW),
		MAPLE("Maple", 50, ItemID.MAPLE_LOGS, ItemID.UNSTRUNG_MAPLE_SHORTBOW, ItemID.MAPLE_SHORTBOW,
			ItemID.UNSTRUNG_MAPLE_LONGBOW, ItemID.MAPLE_LONGBOW, ItemID.MAPLE_SHIELD, ItemID.XBOWS_CROSSBOW_STOCK_MAPLE),
		YEW("Yew", 65, ItemID.YEW_LOGS, ItemID.UNSTRUNG_YEW_SHORTBOW, ItemID.YEW_SHORTBOW, ItemID.UNSTRUNG_YEW_LONGBOW,
			ItemID.YEW_LONGBOW, ItemID.YEW_SHIELD, ItemID.XBOWS_CROSSBOW_STOCK_YEW),
		MAGIC("Magic", 80, ItemID.MAGIC_LOGS, ItemID.UNSTRUNG_MAGIC_SHORTBOW, ItemID.MAGIC_SHORTBOW,
			ItemID.UNSTRUNG_MAGIC_LONGBOW, ItemID.MAGIC_LONGBOW, ItemID.MAGIC_SHIELD, ItemID.XBOWS_CROSSBOW_STOCK_MAGIC),
		REDWOOD("Redwood", 90, ItemID.REDWOOD_LOGS, ItemID.REDWOOD_HIKING_STAFF, ItemID.REDWOOD_SHIELD);

		private final String title;
		private final int level;
		private final int log;
		private final int[] decorations;

		Wood(String title, int level, int log, int... decorations)
		{
			this.title = title;
			this.level = level;
			this.log = log;
			this.decorations = decorations;
		}

		public String getTitle()
		{
			return title;
		}

		/** The Fletching level to build and decorate a totem of this wood; boosts count. */
		public int level()
		{
			return level;
		}

		public int log()
		{
			return log;
		}

		/**
		 * The fletched items of this wood that decorate its totem: "4 of any Fletching product made
		 * of redwood logs"; for oak the wiki lists the shortbow and longbow, strung or not, the
		 * shield and the stock.
		 */
		public int[] decorations()
		{
			return decorations.clone();
		}

		public boolean decorates(int item)
		{
			for (int d : decorations)
			{
				if (d == item)
				{
					return true;
				}
			}
			return false;
		}

		/** The best wood a Fletching level (boosted) can build, or null below 20. */
		public static Wood best(int level)
		{
			Wood best = null;
			for (Wood w : values())
			{
				if (w.level <= level)
				{
					best = w;
				}
			}
			return best;
		}

		/** A wood by its number in a site's base varbit (1 oak to 6 redwood), or null. */
		public static Wood ofVarbit(int value)
		{
			return value >= 1 && value <= values().length ? values()[value - 1] : null;
		}
	}

	/**
	 * The five spirits, in the game's order (its NPCs {@code ENT_TOTEMS_ANIMAL_A} to {@code _E}:
	 * the wiki's buffalo spirit is 14589, jaguar 14590, eagle 14591, snake 14592, scorpion 14593),
	 * with the icon the carving choice shows for each.
	 */
	public enum Animal
	{
		BUFFALO("Buffalo", 'A', ItemID.ENT_TOTEMS_ANIMAL_ICON_A, ItemID.ENT_TOTEMS_ANIMAL_ICON_A_DISABLED),
		JAGUAR("Jaguar", 'B', ItemID.ENT_TOTEMS_ANIMAL_ICON_B, ItemID.ENT_TOTEMS_ANIMAL_ICON_B_DISABLED),
		EAGLE("Eagle", 'C', ItemID.ENT_TOTEMS_ANIMAL_ICON_C, ItemID.ENT_TOTEMS_ANIMAL_ICON_C_DISABLED),
		SNAKE("Snake", 'D', ItemID.ENT_TOTEMS_ANIMAL_ICON_D, ItemID.ENT_TOTEMS_ANIMAL_ICON_D_DISABLED),
		SCORPION("Scorpion", 'E', ItemID.ENT_TOTEMS_ANIMAL_ICON_E, ItemID.ENT_TOTEMS_ANIMAL_ICON_E_DISABLED);

		private final String title;
		private final char letter;
		private final int icon;
		private final int iconDisabled;

		Animal(String title, char letter, int icon, int iconDisabled)
		{
			this.title = title;
			this.letter = letter;
			this.icon = icon;
			this.iconDisabled = iconDisabled;
		}

		public String getTitle()
		{
			return title;
		}

		public char letter()
		{
			return letter;
		}

		public int icon()
		{
			return icon;
		}

		/** The spirit a site's animal varbit names (1 to 5), or null for none. */
		public static Animal ofVarbit(int value)
		{
			return value >= 1 && value <= values().length ? values()[value - 1] : null;
		}

		/** The spirit a carved tier's varbit shows: its number plus 9 once carved, or null. */
		public static Animal ofCarved(int value)
		{
			return ofVarbit(value - 9);
		}

		/** The spirit an icon in the carving choices stands for, lit or greyed, or null. */
		public static Animal ofIcon(int item)
		{
			for (Animal a : values())
			{
				if (a.icon == item || a.iconDisabled == item)
				{
					return a;
				}
			}
			return null;
		}
	}

	/** The Vale Research Exchange's stock and its price in Vale Research Points (wiki). */
	public enum Reward
	{
		ENT_BRANCH("Ent branch", 20),
		BOW_STRING_SPOOL("Bow string spool", 250),
		FLETCHING_KNIFE("Fletching knife", 350),
		GREENMAN_MASK("Greenman mask", 500);

		private final String title;
		private final int points;

		Reward(String title, int points)
		{
			this.title = title;
			this.points = points;
		}

		public String getTitle()
		{
			return title;
		}

		public int points()
		{
			return points;
		}
	}

	/** Research points a number of offerings rummage into. */
	public static int points(long offerings)
	{
		return (int) Math.max(0, offerings / OFFERINGS_PER_POINT);
	}
}
