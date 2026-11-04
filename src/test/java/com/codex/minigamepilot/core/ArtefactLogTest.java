package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.codex.minigamepilot.core.Artefacts.House;
import org.junit.Test;

public class ArtefactLogTest
{
	@Test
	public void paceAndBests()
	{
		ArtefactLog g = new ArtefactLog();
		// Two minutes for two deliveries, a lost one of a minute: three minutes in all.
		g.add(new ArtefactLog.Run(1, 100, House.NORTH, true, 3550, 700));
		g.add(new ArtefactLog.Run(2, 100, House.NORTH, false, 750, 900));
		g.add(new ArtefactLog.Run(3, 100, House.SOUTH, true, 3550, 900));
		assertEquals(2, g.delivered());
		assertEquals(1, g.lost());
		// A lost artefact pays nothing.
		assertEquals(1600, g.coins());
		assertEquals(40.0, g.perHour(), 1e-9);
		assertEquals((3550 * 2 + 750) * 20.0, g.xpPerHour(), 1e-9);
		assertEquals(1600 * 20.0, g.coinsPerHour(), 1e-9);
		assertEquals(100, g.typical(House.NORTH));
		assertEquals(-1, g.typical(House.WEST));
		assertEquals(House.NORTH, g.quickest(null).house);
		assertNull(g.quickest(House.WEST));
	}

	@Test
	public void artefactsToALevel()
	{
		// At 70: 750 + 2,800 = 3,550 an artefact.
		assertEquals(3, ArtefactLog.artefactsFor(10_000, 70));
		assertEquals(0, ArtefactLog.artefactsFor(0, 70));
	}

	@Test
	public void survivesASave()
	{
		ArtefactLog g = new ArtefactLog();
		g.add(new ArtefactLog.Run(5, 90, House.NORTH_WEST, true, 4000, 650));
		g.add(new ArtefactLog.Run(6, 0, null, false, 750, 0));
		ArtefactLog back = ArtefactLog.decode(g.encode());
		assertEquals(g.encode(), back.encode());
		assertEquals("1;5,90,NORTH_WEST,1,4000,650;6,0,-,0,750,0", back.encode());
		assertEquals(1, ArtefactLog.decode("1;x;5,90,NOWHERE,1,1,1;5,90,WEST,2,1,1;5,90,WEST,1,1,1").count());
		assertEquals(0, ArtefactLog.decode("2;5,90,WEST,1,1,1").count());
	}

	@Test
	public void aLongTripIsNotTimed()
	{
		ArtefactLog.Run r = new ArtefactLog.Run(1, ArtefactLog.LONGEST + 1, House.WEST, true, 1, 2000);
		assertEquals(0, r.ticks);
		assertEquals(1000, r.coins);
	}
}
