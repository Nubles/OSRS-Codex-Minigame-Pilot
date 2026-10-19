package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class OreHaulTest
{
	@Test
	public void countsOnlyWhatTheSackPaysOut()
	{
		OreHaul h = new OreHaul();
		h.observe(new int[]{0, 0, 0, 0, 0}, false);
		// The sack pays out 14 coal, 3 gold and 2 mithril.
		h.observe(new int[]{14, 3, 2, 0, 0}, true);
		// Banked: counts fall, nothing earned.
		h.observe(new int[]{0, 0, 0, 0, 0}, false);
		h.observe(new int[]{0, 0, 0, 0, 0}, false);
		// Coal taken out of the bank is not a haul.
		h.observe(new int[]{20, 0, 0, 0, 0}, false);
		h.observe(new int[]{20, 0, 0, 0, 1}, true);
		assertEquals(20, h.count());
		// Coal 150, gold 160, mithril 120, runite 10,000.
		assertEquals(14 * 150 + 3 * 160 + 2 * 120 + 10_000, h.worth(id -> id == 453 ? 150 : id == 444 ? 160 : id == 447 ? 120 : id == 451 ? 10_000 : 0));
	}

	@Test
	public void orePackedATickAfterTheSackDropsStillCounts()
	{
		OreHaul h = new OreHaul();
		h.observe(new int[]{0, 0, 0, 0, 0}, false);
		h.observe(new int[]{0, 0, 0, 0, 0}, true);
		h.observe(new int[]{10, 2, 0, 0, 0}, false);
		assertEquals(12, h.count());
		// Two ticks on, the window has shut.
		h.observe(new int[]{10, 2, 0, 0, 0}, false);
		h.observe(new int[]{30, 2, 0, 0, 0}, false);
		assertEquals(12, h.count());
	}

	@Test
	public void theFirstLookAndAReturnToTheMineCountNothing()
	{
		OreHaul h = new OreHaul();
		h.observe(new int[]{27, 0, 0, 0, 0}, true);
		assertEquals(0, h.count());
		h.forget();
		h.observe(new int[]{28, 0, 0, 0, 0}, true);
		assertEquals(0, h.count());
		h.observe(new int[]{28, 1, 0, 0, 0}, true);
		assertEquals(1, h.count());
	}

	@Test
	public void aNewSessionStartsTheHaulAgain()
	{
		OreHaul h = new OreHaul();
		h.observe(new int[]{0, 0, 0, 0, 0}, false);
		h.observe(new int[]{0, 0, 0, 0, 5}, true);
		h.clear();
		assertEquals(0, h.count());
		assertEquals(0, h.worth(id -> 10_000));
		// What is carried still stands as the base for the next payout.
		h.observe(new int[]{0, 0, 0, 0, 7}, true);
		assertEquals(2, h.count());
	}

	@Test
	public void coalThatGoesStraightIntoAnOpenCoalBagStillCounts()
	{
		OreHaul h = new OreHaul();
		h.observe(new int[]{0, 0, 0, 0, 0}, 0, 0, true);
		// The sack gives 40: 27 coal fills the open bag, and 9 coal, 3 gold and 1 nugget land in the pack.
		h.observe(new int[]{9, 3, 0, 0, 0}, 1, 40, true);
		h.observe(new int[]{9, 3, 0, 0, 0}, 1, 0, true);
		assertEquals(39, h.count());
		assertEquals(36 * 150 + 3 * 160, h.worth(id -> id == 453 ? 150 : id == 444 ? 160 : 0));
	}

	@Test
	public void withoutAnOpenBagNothingIsGuessed()
	{
		OreHaul h = new OreHaul();
		h.observe(new int[]{0, 0, 0, 0, 0}, 0, 0, false);
		// A closed bag takes nothing: the sack's 20 all land, or stay in the sack.
		h.observe(new int[]{12, 0, 0, 0, 0}, 0, 20, false);
		h.observe(new int[]{12, 0, 0, 0, 0}, 0, 0, false);
		assertEquals(12, h.count());
	}

	@Test
	public void aPayoutLandingOverTwoTicksIsNotCountedTwice()
	{
		OreHaul h = new OreHaul();
		h.observe(new int[]{0, 0, 0, 0, 0}, 0, 0, true);
		// The sack's count drops a tick before the pack fills.
		h.observe(new int[]{0, 0, 0, 0, 0}, 0, 30, true);
		h.observe(new int[]{3, 0, 0, 0, 0}, 0, 0, true);
		assertEquals("27 in the bag, 3 in the pack", 30, h.count());
		h.observe(new int[]{3, 0, 0, 0, 0}, 0, 0, true);
		assertEquals(30, h.count());
		// The first look in the mine is no base for a guess either.
		OreHaul fresh = new OreHaul();
		fresh.observe(new int[]{0, 0, 0, 0, 0}, 0, 30, true);
		fresh.observe(new int[]{0, 0, 0, 0, 0}, 0, 0, true);
		assertEquals(0, fresh.count());
	}
}
