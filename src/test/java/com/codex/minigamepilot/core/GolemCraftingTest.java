package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** Golem crafting's facts against the OSRS wiki and RuneLite's gameval ids. */
public class GolemCraftingTest
{
	@Test
	public void wikiFigures()
	{
		// "the activity itself requires level 60 Crafting"; "Crafting a golem gives a base of 1560 XP"
		assertEquals(60, GolemCrafting.CRAFTING_LEVEL);
		assertEquals(1560, GolemCrafting.BASE_XP);
		assertEquals(4, GolemCrafting.SUNSTONE_PER_GOLEM);
		assertEquals(10, GolemCrafting.CORE_XP);
		assertEquals(300, GolemCrafting.CHISEL_ODDS);
	}

	@Test
	public void furTableTotals()
	{
		// The wiki's Total XP column: the base and the fur's.
		int[] total = {2760, 2740, 2720, 2700, 2700, 2680, 2660, 2640, 2620, 2590, 2560, 2520, 2480, 2440};
		int[] hunter = {91, 72, 69, 55, 57, 57, 43, 41, 31, 7, 3, 13, 1, 60};
		GolemCrafting.Fur[] f = GolemCrafting.Fur.values();
		assertEquals(14, f.length);
		for (int i = 0; i < f.length; i++)
		{
			assertEquals(f[i].getTitle(), total[i], f[i].totalXp());
			assertEquals(f[i].getTitle(), hunter[i], f[i].hunter());
			assertEquals(f[i], GolemCrafting.Fur.byItem(f[i].item()));
			assertEquals(f[i], GolemCrafting.Fur.byCode(f[i].code));
		}
	}

	@Test
	public void ids()
	{
		// RuneLite gameval, and the wiki's infoboxes: sunstone 34020, sunstone core 34022, Jeweller's chisel 34024.
		assertEquals(34020, GolemCrafting.SUNSTONE);
		assertEquals(34022, GolemCrafting.SUNSTONE_CORE);
		assertEquals(34024, GolemCrafting.JEWELLERS_CHISEL);
		assertEquals(29174, GolemCrafting.Fur.MOONLIGHT_ANTELOPE.item());
		assertEquals(10115, GolemCrafting.Fur.DARK_KEBBIT.item());
		assertEquals(10103, GolemCrafting.Fur.KYATT.item());
		assertEquals(34017, GolemCrafting.Fur.WYRMSCRAIG_GOAT.item());
		assertEquals(1623, GolemCrafting.Gift.SAPPHIRE.item());
		assertEquals(1617, GolemCrafting.Gift.DIAMOND.item());
		for (int id = 62351; id <= 62358; id++)
		{
			assertTrue(GolemCrafting.isWatched(id));
		}
		assertTrue(GolemCrafting.isWatched(62393) && GolemCrafting.isWatched(62394) && GolemCrafting.isWatched(62216));
		assertFalse(GolemCrafting.isWatched(6338));
	}

	@Test
	public void giftRatesAddUp()
	{
		double gems = 0;
		for (GolemCrafting.Gift g : GolemCrafting.Gift.values())
		{
			gems += g.gem() ? g.rate() : 0;
		}
		assertEquals(1.0, gems, 1e-9);
		assertEquals("15/23", GolemCrafting.Gift.SAPPHIRE.odds());
		assertEquals("1/300", GolemCrafting.Gift.CHISEL.odds());
	}

	@Test
	public void giftLines()
	{
		// RuneLite's loot tracker test lines, tags removed.
		assertEquals(GolemCrafting.Gift.CHISEL, GolemCrafting.gift("As you complete the golem it leaves a gift on the ground for you: 1 x Jeweller's chisel."));
		assertEquals(GolemCrafting.Gift.SAPPHIRE, GolemCrafting.gift("As you complete the golem it leaves a gift on the ground for you: 1 x Uncut sapphire."));
		assertEquals(GolemCrafting.Gift.DIAMOND, GolemCrafting.gift("As you complete the golem it leaves a gift in your gem bag for you: 1 x Uncut diamond."));
		assertEquals(GolemCrafting.Gift.RUBY, GolemCrafting.gift("As you complete the golem it leaves a gift in your gem sack for you: 1 x Uncut ruby"));
		assertEquals(GolemCrafting.Gift.EMERALD, GolemCrafting.gift("As you complete the golem it leaves a gift on the ground for you: 1 x Uncut emerald."));
		assertNull(GolemCrafting.gift("You finish crafting the golem from this angle."));
		assertNull(GolemCrafting.gift("As you complete the golem it leaves a gift on the ground for you: 1 x Uncut opal."));
		assertNull(GolemCrafting.gift(null));
	}

	@Test
	public void golemsFromSunstone()
	{
		// The wiki's rotation: 25 sunstone, 5 chiselled into cores, 20 left: 5 golems.
		assertEquals(5, GolemCrafting.golemsFrom(25, 0));
		assertEquals(5, GolemCrafting.golemsFrom(20, 5));
		assertEquals(2, GolemCrafting.golemsFrom(8, 5));
		assertEquals(0, GolemCrafting.golemsFrom(3, 4));
		assertEquals(3, GolemCrafting.golemsFrom(14, 1));
		assertEquals(0, GolemCrafting.golemsFrom(-1, -1));
	}

	@Test
	public void chiselOdds()
	{
		assertEquals(0, GolemCrafting.chiselChance(0), 0);
		assertEquals(1 - Math.pow(299 / 300.0, 300), GolemCrafting.chiselChance(300), 1e-12);
		assertEquals(208, GolemCrafting.golemsForChisel(0.5));
	}
}
