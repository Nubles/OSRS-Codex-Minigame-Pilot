package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

/** The Sorceress's Garden's facts against the wiki's pages, and its ids against the wiki's infoboxes and RuneLite's gameval. */
public class GardenTest
{
	@Test
	public void levels()
	{
		// Sorceress's Garden: "You can only enter a seasonal garden if your current Thieving level matches or exceeds
		// the garden's required level." Winter N/A, Spring 25, Autumn 45, Summer 65.
		assertEquals(1, Garden.Season.WINTER.thieving());
		assertEquals(25, Garden.Season.SPRING.thieving());
		assertEquals(45, Garden.Season.AUTUMN.thieving());
		assertEquals(65, Garden.Season.SUMMER.thieving());
		assertSame(Garden.Season.WINTER, Garden.Season.bestFor(24));
		assertSame(Garden.Season.SPRING, Garden.Season.bestFor(25));
		assertSame(Garden.Season.AUTUMN, Garden.Season.bestFor(64));
		assertSame(Garden.Season.SUMMER, Garden.Season.bestFor(65));
	}

	@Test
	public void juice()
	{
		// "Made from 5 winter sq'irks.", "Made from 4 spring sq'irks.", "Made from 3 autumn sq'irks.", "Made from 2 summer sq'irks."
		assertEquals(5, Garden.Season.WINTER.perJuice());
		assertEquals(4, Garden.Season.SPRING.perJuice());
		assertEquals(3, Garden.Season.AUTUMN.perJuice());
		assertEquals(2, Garden.Season.SUMMER.perJuice());
		// "for 350 Thieving experience", "for 1,350 ...", "for 2,350 ...", "for 3,000 Thieving experience".
		assertEquals(350, Garden.Season.WINTER.osmanXp());
		assertEquals(1_350, Garden.Season.SPRING.osmanXp());
		assertEquals(2_350, Garden.Season.AUTUMN.osmanXp());
		assertEquals(3_000, Garden.Season.SUMMER.osmanXp());
		// The wiki's "Experience per fruit" column: 70, 337.5, 783.3, 1,500.
		assertEquals(70, Garden.Season.WINTER.xpPerSqirk(), 1e-9);
		assertEquals(337.5, Garden.Season.SPRING.xpPerSqirk(), 1e-9);
		assertEquals(783.3, Garden.Season.AUTUMN.xpPerSqirk(), 0.05);
		assertEquals(1_500, Garden.Season.SUMMER.xpPerSqirk(), 1e-9);
		// "Crushing sq'irks always gives 5 Cooking experience, regardless of the season."
		assertEquals(5, Garden.CRUSH_COOKING_XP);
	}

	@Test
	public void farming()
	{
		// Sq'irk tree: Farming 30, 40, 50, 60; "Doing so gives 60 Farming experience and teleports the player to the fountain".
		assertEquals(30, Garden.Season.WINTER.farmingXp());
		assertEquals(40, Garden.Season.SPRING.farmingXp());
		assertEquals(50, Garden.Season.AUTUMN.farmingXp());
		assertEquals(60, Garden.Season.SUMMER.farmingXp());
		// Herbs: "picking them will always yield 50 Farming experience".
		assertEquals(50, Garden.HERB_FARMING_XP);
		// Summer: "Each banked inventory of 22 summer sq'irkjuices plus 2 extra sq'irks is worth 110 Cooking,
		// 2,760 Farming, and 69,000 Thieving experience."
		int sqirks = 22 * Garden.Season.SUMMER.perJuice() + 2;
		assertEquals(2_760, sqirks * Garden.Season.SUMMER.farmingXp());
		assertEquals(110, Garden.INVENTORY_GLASSES * Garden.CRUSH_COOKING_XP);
		assertEquals(69_000, sqirks / Garden.Season.SUMMER.perJuice() * Garden.Season.SUMMER.osmanXp());
		// "The elementals can only 'see' and teleport the player when there are two or fewer squares between them and the player."
		assertEquals(2, Garden.CATCH_REACH);
	}

	@Test
	public void items()
	{
		// Wiki infoboxes: spring 10844, summer 10845, autumn 10846, winter 10847; juice 10848, 10849, 10850, 10851.
		assertEquals(10844, Garden.Season.SPRING.sqirk());
		assertEquals(10845, Garden.Season.SUMMER.sqirk());
		assertEquals(10846, Garden.Season.AUTUMN.sqirk());
		assertEquals(10847, Garden.Season.WINTER.sqirk());
		assertEquals(10848, Garden.Season.SPRING.juice());
		assertEquals(10849, Garden.Season.SUMMER.juice());
		assertEquals(10850, Garden.Season.AUTUMN.juice());
		assertEquals(10851, Garden.Season.WINTER.juice());
		assertEquals(ItemID.OSMAN_SQUIRK_SUMMER, Garden.Season.SUMMER.sqirk());
		assertEquals(ItemID.OSMAN_SQUIRK_J_SUMMER, Garden.Season.SUMMER.juice());
		assertEquals(233, Garden.PESTLE_AND_MORTAR);
		assertEquals(1919, Garden.BEER_GLASS);
	}

	@Test
	public void trees()
	{
		// Sq'irk tree: winter 13407, spring 13405, autumn 13406, summer 12943.
		assertEquals(13407, Garden.Season.WINTER.tree());
		assertEquals(13405, Garden.Season.SPRING.tree());
		assertEquals(13406, Garden.Season.AUTUMN.tree());
		assertEquals(12943, Garden.Season.SUMMER.tree());
		assertSame(Garden.Season.SUMMER, Garden.Season.byTree(ObjectID._100_OSMAN_POMY_TREE_SUMMER));
		assertTrue(Garden.isWatched(ObjectID._100_OSMAN_FOUNTAIN));
		assertTrue(Garden.isWatched(12943));
		assertFalse(Garden.isWatched(ObjectID._100_OSMAN_DOOR_SUMMER));
	}

	@Test
	public void elementals()
	{
		// Summer Elemental "1801,...,1806"; Spring "2956,...,2963"; Autumn 5802-5807; Winter "5796,...,5801".
		for (int id = 1801; id <= 1806; id++)
		{
			assertSame(Garden.Season.SUMMER, Garden.Season.byElemental(id));
		}
		for (int id = 2956; id <= 2963; id++)
		{
			assertSame(Garden.Season.SPRING, Garden.Season.byElemental(id));
		}
		for (int id = 5802; id <= 5807; id++)
		{
			assertSame(Garden.Season.AUTUMN, Garden.Season.byElemental(id));
		}
		for (int id = 5796; id <= 5801; id++)
		{
			assertSame(Garden.Season.WINTER, Garden.Season.byElemental(id));
		}
		assertSame(Garden.Season.SUMMER, Garden.Season.byElemental(NpcID._100_OSMAN_SUMMER_ELEMENTAL_1));
		assertSame(Garden.Season.SUMMER, Garden.Season.byElemental(NpcID._100_OSMAN_SUMMER_ELEMENTAL_6));
		assertSame(Garden.Season.SPRING, Garden.Season.byElemental(NpcID._100_OSMAN_SPRING_ELEMENTAL_8));
		assertSame(Garden.Season.AUTUMN, Garden.Season.byElemental(NpcID._100_OSMAN_AUTUMN_ELEMENTAL_1));
		assertSame(Garden.Season.WINTER, Garden.Season.byElemental(NpcID._100_OSMAN_WINTER_ELEMENTAL_6));
		assertNull(Garden.Season.byElemental(1807));
		assertNull(Garden.Season.byElemental(1800));
	}
}
