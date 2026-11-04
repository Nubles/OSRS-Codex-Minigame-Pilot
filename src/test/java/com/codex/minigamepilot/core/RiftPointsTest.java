package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RiftPointsTest
{
	@Test
	public void theCheckAndTheEndOfGameTotals()
	{
		RiftPoints p = new RiftPoints();
		assertFalse(p.known());
		assertTrue(p.read("You have 1,204 catalytic energy and 980 elemental energy."));
		assertEquals(980, p.bankedElemental());
		assertEquals(1204, p.bankedCatalytic());
		assertEquals(980, p.searches());
		assertTrue(p.read("Elemental energy attuned: 4. Catalytic energy attuned: 6. Total elemental energy: 984. Total catalytic energy: 1,210."));
		assertEquals("984:1210", p.encode());
	}

	@Test
	public void thisGamesEnergyIsAHundredAPoint()
	{
		RiftPoints p = new RiftPoints();
		p.decode("10:20");
		p.energy(250, 0);
		assertEquals(12.5, p.elemental(), 1e-9);
		assertEquals(20, p.catalytic(), 1e-9);
		// The end-of-game totals already hold this game's points: they are not counted twice.
		p.read("Total elemental energy: 12. Total catalytic energy: 20.");
		assertEquals(12, p.elemental(), 1e-9);
		// A new game: the counters start again from nothing.
		p.energy(0, 0);
		p.energy(100, 0);
		assertEquals(13, p.elemental(), 1e-9);
	}

	@Test
	public void aSearchSpendsOneOfEach()
	{
		RiftPoints p = new RiftPoints();
		assertFalse("unknown points are not spent", p.read("You found some loot: 1 x Mist rune"));
		p.decode("5:0");
		p.read("You found some loot: 1 x Mist rune");
		assertEquals("4:0", p.encode());
	}

	@Test
	public void damagedSavesAreUnknown()
	{
		RiftPoints p = new RiftPoints();
		for (String bad : new String[]{"", "x", "1:2:3", "-1:4", "a:b", ":"})
		{
			p.decode(bad);
			assertFalse(bad, p.known());
			assertNull(p.encode());
		}
	}

	@Test
	public void energyComesAsWholePointsAndAChance()
	{
		assertEquals("+4-5", RiftPoints.coming(470));
		assertEquals("+3", RiftPoints.coming(300));
		assertEquals("+0-1", RiftPoints.coming(73));
	}

	@Test
	public void figures()
	{
		assertEquals("1,204", RiftPoints.show(1204));
		assertEquals("12.5", RiftPoints.show(12.5));
	}
}
