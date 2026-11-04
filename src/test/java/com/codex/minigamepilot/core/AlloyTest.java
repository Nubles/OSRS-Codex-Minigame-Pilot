package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** Metal score, sections and experience against the OSRS wiki's own examples (Giants' Foundry). */
public class AlloyTest
{
	private static int[] crucible(int bronze, int iron, int steel, int mithril, int adamant, int rune)
	{
		return new int[]{bronze, iron, steel, mithril, adamant, rune};
	}

	@Test
	public void metalScores()
	{
		assertEquals(10, Alloy.metalScore(crucible(28, 0, 0, 0, 0, 0)));
		assertEquals(20, Alloy.metalScore(crucible(0, 28, 0, 0, 0, 0)));
		assertEquals(30, Alloy.metalScore(crucible(0, 0, 28, 0, 0, 0)));
		assertEquals(40, Alloy.metalScore(crucible(0, 0, 0, 28, 0, 0)));
		assertEquals(50, Alloy.metalScore(crucible(0, 0, 0, 0, 28, 0)));
		assertEquals(60, Alloy.metalScore(crucible(0, 0, 0, 0, 0, 28)));
		assertEquals(59, Alloy.metalScore(crucible(0, 0, 19, 9, 0, 0)));
		assertEquals(89, Alloy.metalScore(crucible(0, 0, 0, 18, 10, 0)));
		assertEquals(118, Alloy.metalScore(crucible(0, 0, 0, 0, 19, 9)));
		assertEquals(130, Alloy.metalScore(crucible(0, 0, 0, 0, 14, 14)));
		assertEquals(0, Alloy.metalScore(crucible(0, 0, 0, 0, 0, 0)));
	}

	@Test
	public void onlyTheTwoMostPlentifulCount()
	{
		// A third metal is disregarded.
		assertEquals(Alloy.metalScore(crucible(0, 0, 0, 18, 10, 0)), Alloy.metalScore(crucible(0, 0, 0, 18, 10, 0)));
		int[] three = crucible(1, 0, 0, 17, 10, 0);
		int[] two = crucible(0, 0, 0, 17, 10, 0);
		assertEquals(Alloy.metalScore(two), Alloy.metalScore(three));
		// A tie goes to the more valuable metal.
		assertEquals("Rune", Alloy.main(crucible(0, 0, 0, 0, 14, 14)));
		assertNull(Alloy.main(crucible(0, 0, 0, 0, 0, 0)));
		assertEquals(28, Alloy.bars(crucible(0, 0, 0, 18, 10, 0)));
	}

	@Test
	public void sections()
	{
		assertEquals(3, Alloy.sections(10));
		assertEquals(3, Alloy.sections(19));
		assertEquals(4, Alloy.sections(20));
		assertEquals(4, Alloy.sections(59));
		assertEquals(5, Alloy.sections(60));
		assertEquals(5, Alloy.sections(89));
		assertEquals(6, Alloy.sections(90));
		assertEquals(6, Alloy.sections(119));
		assertEquals(7, Alloy.sections(120));
		assertEquals(7, Alloy.sections(130));
		assertEquals(2, Alloy.sweetSpots(5));
		assertEquals(3, Alloy.sweetSpots(6));
		assertEquals(3, Alloy.sweetSpots(7));
	}

	@Test
	public void experience()
	{
		// The two figures the wiki states: 30 at quality 0, 25,230 at 199.
		assertEquals(30, Alloy.xp(0));
		assertEquals(25230, Alloy.xp(199));
		assertEquals(13560, Alloy.xp(135));
		assertEquals(30, Alloy.xp(-5));
	}
}
