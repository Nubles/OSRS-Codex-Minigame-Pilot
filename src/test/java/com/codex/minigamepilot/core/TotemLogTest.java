package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TotemLogTest
{
	@Test
	public void ratesFromTimedTotems()
	{
		TotemLog g = TotemLog.decode("1;100,200,MAGIC,5000,1000;200,200,-,5000,1000;300,0,OAK,900,50");
		assertEquals(3, g.count());
		assertNull(g.all().get(1).wood);
		// 400 timed ticks is 240 seconds: 2 totems, 10,000 xp and 2,000 offerings in it.
		assertEquals(30, g.perHour(), 1e-9);
		assertEquals(150_000, g.xpPerHour(), 1e-6);
		assertEquals(30_000, g.offeringsPerHour(), 1e-6);
		assertEquals(300, g.pointsPerHour(), 1e-9);
		assertEquals(200, g.quickest().ticks);
	}

	@Test
	public void timeToAReward()
	{
		TotemLog g = TotemLog.decode("1;100,200,MAGIC,5000,1000;200,200,MAGIC,5000,1000");
		// 350 - 200 points - 25 in 2,500 offerings = 125 points at 300 an hour.
		assertEquals(125 / 300.0, g.hoursTo(Totems.Reward.FLETCHING_KNIFE, 200, 2_500), 1e-9);
		assertEquals(0, g.hoursTo(Totems.Reward.ENT_BRANCH, 20, 0), 1e-9);
		assertEquals(-1, new TotemLog().hoursTo(Totems.Reward.GREENMAN_MASK, 0, 0), 1e-9);
	}

	@Test
	public void saveRoundTripsAndDamageIsLeftOut()
	{
		TotemLog g = new TotemLog();
		g.add(new TotemLog.Totem(5, 2000, Totems.Wood.YEW, 3000, 40));
		assertEquals("1;5,0,YEW,3000,40", g.encode());
		assertEquals(1, TotemLog.decode(g.encode()).count());
		assertEquals(0, TotemLog.decode("1;5,0,BIRCH,3000,40;x").count());
		assertEquals(0, TotemLog.decode("2;5,0,YEW,3000,40").count());
		for (int i = 0; i < 120; i++)
		{
			g.add(new TotemLog.Totem(i, 100, null, 1, 1));
		}
		assertEquals(TotemLog.KEEP, g.count());
	}
}
