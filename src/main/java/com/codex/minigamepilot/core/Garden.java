package com.codex.minigamepilot.core;

import java.util.Locale;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;

/**
 * The Sorceress's Garden's facts, each from the OSRS wiki (Sorceress's Garden, Sq'irk tree, each
 * season's sq'irk and sq'irkjuice, each season's elemental) with its ids from RuneLite's gameval.
 *
 * <ul>
 * <li>"You can only enter a seasonal garden if your current Thieving level matches or exceeds the
 * garden's required level." Winter none, spring 25, autumn 45, summer 65.</li>
 * <li>Picking a sq'irk gives 30, 40, 50 or 60 Farming experience (winter to summer), "and teleports
 * the player to the fountain in the inner garden". The tree gives no Thieving experience.</li>
 * <li>"Made from 5 winter sq'irks.", 4 spring, 3 autumn, "Made from 2 summer sq'irks."</li>
 * <li>Osman (or Selim) gives 350, 1,350, 2,350 or 3,000 Thieving experience for a glass.</li>
 * <li>Herbs: "picking them will always yield 50 Farming experience", two clean herbs a trip.</li>
 * <li>"Crushing sq'irks always gives 5 Cooking experience, regardless of the season."</li>
 * </ul>
 */
public final class Garden
{
	private Garden()
	{
	}

	/** Farming experience for picking herbs in any garden (two herbs, 25 each). */
	public static final int HERB_FARMING_XP = 50;
	/** Cooking experience for crushing a glass of juice, whatever the season. */
	public static final int CRUSH_COOKING_XP = 5;
	/** "two or fewer squares between them and the player": an elemental this near can catch you. */
	public static final int CATCH_REACH = 2;
	/** The wiki's "banked inventory of 22 summer sq'irkjuices" (22 beer glasses carried). */
	public static final int INVENTORY_GLASSES = 22;
	/** The fountain in the inner garden, where a pick or a catch sends you. */
	public static final int FOUNTAIN = ObjectID._100_OSMAN_FOUNTAIN;
	public static final int PESTLE_AND_MORTAR = ItemID.PESTLE_AND_MORTAR;
	public static final int BEER_GLASS = ItemID.BEER_GLASS;

	/** The four gardens, coolest first. */
	public enum Season
	{
		WINTER('w', "Winter", 1, 5, 350, 30, ItemID.OSMAN_SQUIRK_WINTER, ItemID.OSMAN_SQUIRK_J_WINTER,
			ObjectID._100_OSMAN_POMY_TREE_WINTER, 5796, 5801),
		SPRING('s', "Spring", 25, 4, 1_350, 40, ItemID.OSMAN_SQUIRK_SPRING, ItemID.OSMAN_SQUIRK_J_SPRING,
			ObjectID._100_OSMAN_POMY_TREE_SPRING, 2956, 2963),
		AUTUMN('a', "Autumn", 45, 3, 2_350, 50, ItemID.OSMAN_SQUIRK_AUTUMN, ItemID.OSMAN_SQUIRK_J_AUTUMN,
			ObjectID._100_OSMAN_POMY_TREE_AUTUMN, 5802, 5807),
		SUMMER('u', "Summer", 65, 2, 3_000, 60, ItemID.OSMAN_SQUIRK_SUMMER, ItemID.OSMAN_SQUIRK_J_SUMMER,
			ObjectID._100_OSMAN_POMY_TREE_SUMMER, 1801, 1806);

		final char code;
		private final String title;
		private final int thieving;
		private final int perJuice;
		private final int osmanXp;
		private final int farmingXp;
		private final int sqirk;
		private final int juice;
		private final int tree;
		private final int elementalFirst;
		private final int elementalLast;

		Season(char code, String title, int thieving, int perJuice, int osmanXp, int farmingXp, int sqirk, int juice,
			int tree, int elementalFirst, int elementalLast)
		{
			this.code = code;
			this.title = title;
			this.thieving = thieving;
			this.perJuice = perJuice;
			this.osmanXp = osmanXp;
			this.farmingXp = farmingXp;
			this.sqirk = sqirk;
			this.juice = juice;
			this.tree = tree;
			this.elementalFirst = elementalFirst;
			this.elementalLast = elementalLast;
		}

		public String getTitle()
		{
			return title;
		}

		/** "summer" */
		public String lower()
		{
			return title.toLowerCase(Locale.ROOT);
		}

		/** Thieving level to enter (current level, so a boost counts); 1 for winter, which has none. */
		public int thieving()
		{
			return thieving;
		}

		/** Sq'irks crushed into one glass of juice. */
		public int perJuice()
		{
			return perJuice;
		}

		/** Thieving experience Osman gives for one glass. */
		public int osmanXp()
		{
			return osmanXp;
		}

		/** Farming experience for picking one sq'irk. */
		public int farmingXp()
		{
			return farmingXp;
		}

		/** Thieving experience one sq'irk is worth once crushed and handed in: the glass's over its sq'irks. */
		public double xpPerSqirk()
		{
			return osmanXp / (double) perJuice;
		}

		public int sqirk()
		{
			return sqirk;
		}

		public int juice()
		{
			return juice;
		}

		public int tree()
		{
			return tree;
		}

		public boolean isElemental(int npcId)
		{
			return npcId >= elementalFirst && npcId <= elementalLast;
		}

		public char code()
		{
			return code;
		}

		public static Season byCode(char c)
		{
			for (Season s : values())
			{
				if (s.code == c)
				{
					return s;
				}
			}
			return null;
		}

		public static Season byTree(int id)
		{
			for (Season s : values())
			{
				if (s.tree == id)
				{
					return s;
				}
			}
			return null;
		}

		public static Season byElemental(int npcId)
		{
			for (Season s : values())
			{
				if (s.isElemental(npcId))
				{
					return s;
				}
			}
			return null;
		}

		/** The best garden open at this (current) Thieving level. */
		public static Season bestFor(int thievingLevel)
		{
			Season best = WINTER;
			for (Season s : values())
			{
				if (thievingLevel >= s.thieving)
				{
					best = s;
				}
			}
			return best;
		}
	}

	/** Whether an object is one the garden pilot watches: a sq'irk tree or the fountain. */
	public static boolean isWatched(int id)
	{
		return id == FOUNTAIN || Season.byTree(id) != null;
	}
}
