package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TitheLogTest
{
	@Test
	public void ratesFromTimedSacks()
	{
		TitheLog g = TitheLog.decode("1;100,1000,LOGAVANO,100,35,37030;200,1000,-,100,35,37030;300,0,LOGAVANO,30,10,9000");
		assertEquals(3, g.count());
		assertNull(g.all().get(1).seed);
		// 2,000 timed ticks is 1,200 seconds: 70 points and 74,060 xp in it.
		assertEquals(210, g.pointsPerHour(), 1e-9);
		assertEquals(222_180, g.xpPerHour(), 1e-6);
		assertEquals(1000, g.quickest().ticks);
		// 105 points more: half an hour, three sacks of your 35.
		assertEquals(0.5, g.hoursFor(105), 1e-9);
		assertEquals(3, g.sacksFor(105));
		assertEquals(0, g.hoursFor(0), 1e-9);
		assertEquals(-1, new TitheLog().hoursFor(10), 1e-9);
		// No sacks yet: the 35 a full sack pays.
		assertEquals(2, new TitheLog().sacksFor(36));
	}

	@Test
	public void saveRoundTripsAndDamageIsLeftOut()
	{
		TitheLog g = new TitheLog();
		g.add(new TitheLog.Run(5, 99_999, Tithe.Seed.BOLOGANO, 100, 35, 22540));
		assertEquals("1;5,0,BOLOGANO,100,35,22540", g.encode());
		assertEquals(1, TitheLog.decode(g.encode()).count());
		assertEquals(0, TitheLog.decode("1;5,0,TOMATO,100,35,1;x").count());
		assertEquals(0, TitheLog.decode("2;5,0,BOLOGANO,100,35,1").count());
		for (int i = 0; i < 120; i++)
		{
			g.add(new TitheLog.Run(i, 100, null, 1, 1, 1));
		}
		assertEquals(TitheLog.KEEP, g.count());
	}
}
