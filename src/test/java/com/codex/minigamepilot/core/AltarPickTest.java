package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AltarPickTest
{
	private static RiftPoints points(int elemental, int catalytic)
	{
		RiftPoints p = new RiftPoints();
		p.decode(elemental + ":" + catalytic);
		return p;
	}

	@Test
	public void bloodAndDeathFirstThenFireAndEarth()
	{
		RiftPoints even = points(100, 100);
		assertEquals(Altar.BLOOD, AltarPick.choose(Altar.FIRE, Altar.BLOOD, 99, q -> true, even).altar);
		assertEquals(Altar.FIRE, AltarPick.choose(Altar.FIRE, Altar.NATURE, 99, q -> true, even).altar);
		assertEquals(Altar.EARTH, AltarPick.choose(Altar.EARTH, Altar.LAW, 99, q -> true, points(500, 0)).altar);
	}

	@Test
	public void otherwiseTheTypeWithFewerPoints()
	{
		assertEquals(Altar.WATER, AltarPick.choose(Altar.WATER, Altar.NATURE, 99, q -> true, points(10, 40)).altar);
		AltarPick.Choice c = AltarPick.choose(Altar.WATER, Altar.NATURE, 99, q -> true, points(40, 10));
		assertEquals(Altar.NATURE, c.altar);
		assertTrue(c.why, c.why.startsWith("Fewer catalytic points · strong cell"));
	}

	@Test
	public void evenOrUnknownPointsTakeTheStrongerCell()
	{
		assertEquals(Altar.NATURE, AltarPick.choose(Altar.WATER, Altar.NATURE, 99, q -> true, points(10, 10)).altar);
		AltarPick.Choice c = AltarPick.choose(Altar.WATER, Altar.MIND, 99, q -> true, new RiftPoints());
		assertEquals(Altar.WATER, c.altar);
		assertTrue(c.why.contains("Points not known"));
	}

	@Test
	public void neverAnAltarYouCannotUse()
	{
		// Blood would come first, but 70 Runecraft is under its 77.
		AltarPick.Choice c = AltarPick.choose(Altar.AIR, Altar.BLOOD, 70, q -> true, points(0, 100));
		assertEquals(Altar.AIR, c.altar);
		assertEquals("Needs 77 Runecraft", c.open.get(1).locked);
		// Law at 99 without Troll Stronghold.
		assertEquals(Altar.AIR, AltarPick.choose(Altar.AIR, Altar.LAW, 99, q -> !q.equals("Troll Stronghold"), points(0, 0)).altar);
		AltarPick.Choice none = AltarPick.choose(Altar.FIRE, Altar.COSMIC, 10, q -> false, null);
		assertNull(none.altar);
		assertTrue(none.why, none.why.contains("Fire needs 14 Runecraft"));
		assertTrue(none.why, none.why.contains("Cosmic needs 27 Runecraft"));
	}

	@Test
	public void nothingOpen()
	{
		AltarPick.Choice c = AltarPick.choose(null, null, 99, q -> true, null);
		assertNull(c.altar);
		assertTrue(c.open.isEmpty());
	}
}
