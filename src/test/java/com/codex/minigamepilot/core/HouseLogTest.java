package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Houses.House;
import org.junit.Test;

/** Your hauls: kept, valued at Oriana's rate and the items' prices, and an hour at your pace. */
public class HouseLogTest
{
	@Test
	public void keepsAndReadsBack()
	{
		HouseLog g = new HouseLog();
		g.add(new HouseLog.Haul(1000, House.LAVINIA, 250, 400, 120, 120, 5400, 6300, 900));
		g.add(new HouseLog.Haul(2000, null, 260, 380, 100, 10, 4500, 5000, 0));
		HouseLog back = HouseLog.decode(g.encode());
		assertEquals(g.encode(), back.encode());
		assertEquals(2, back.count());
		assertEquals(1, back.caught());
		assertEquals(130, back.kept());
		assertEquals(House.LAVINIA, back.best(55).house);
		assertEquals(120, back.typical(House.LAVINIA));
		assertEquals(-1, back.typical(House.CAIUS));
		assertNull(back.last().house);
	}

	@Test
	public void anHourAtYourPace()
	{
		HouseLog g = new HouseLog();
		// 600 ticks (six minutes) a round: ten an hour.
		g.add(new HouseLog.Haul(0, House.VICTOR, 300, 600, 150, 150, 6750, 7500, 1000));
		g.add(new HouseLog.Haul(0, House.CAIUS, 300, 600, 150, 150, 6750, 7500, 0));
		assertEquals(10, g.housesPerHour(), 1e-9);
		assertEquals(75_000, g.xpPerHour(), 1e-6);
		// 300 valuables at 55 and 1,000 of finds over 12 minutes.
		assertEquals((300 * 55 + 1000) * 5, g.coinsPerHour(55), 1e-6);
		assertEquals((300 * 65 + 1000) * 5, g.coinsPerHour(65), 1e-6);
		assertEquals(2, g.hoursFor(150_000), 1e-9);
		assertEquals(0, g.hoursFor(0), 1e-9);
		assertEquals(-1, new HouseLog().hoursFor(10), 1e-9);
		assertEquals(-1, new HouseLog().coinsPerHour(55), 1e-9);
	}

	@Test
	public void boundsHold()
	{
		HouseLog.Haul h = new HouseLog.Haul(-5, null, 5000, 2, -1, 99, -7, 3, -1);
		assertEquals(0, h.endedAt);
		assertEquals(0, h.ticks);
		assertEquals(0, h.taken);
		assertEquals(0, h.kept);
		assertEquals(0, h.xp);
		assertEquals(0, h.items);
		assertFalse(h.caught());
		// A round shorter than its house is the house.
		HouseLog.Haul r = new HouseLog.Haul(0, null, 300, 100, 10, 10, 450, 450, 0);
		assertEquals(300, r.round);
		assertTrue(new HouseLog.Haul(0, null, 300, 300, 100, 10, 0, 0, 0).caught());
	}

	@Test
	public void keepsTheLastTwoHundred()
	{
		HouseLog g = new HouseLog();
		for (int i = 0; i < 230; i++)
		{
			g.add(new HouseLog.Haul(i, House.LAVINIA, 100, 100, 1, 1, 45, 45, 0));
		}
		assertEquals(HouseLog.KEEP, g.count());
		assertEquals(229, g.last().endedAt);
	}

	@Test
	public void damagedHaulsAreLeftOut()
	{
		HouseLog g = HouseLog.decode("1;5,LAVINIA,100,200,10,10,450,500,0;6,NOBODY,1,1,1,1,1,1,1;7,-,1,2;8,-,-1,1,1,1,1,1,1");
		assertEquals(1, g.count());
		assertEquals(0, HouseLog.decode("2;5,LAVINIA,100,200,10,10,450,500,0").count());
		assertEquals(0, HouseLog.decode(null).count());
	}
}
