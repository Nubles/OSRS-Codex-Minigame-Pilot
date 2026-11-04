package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class MixRoundsTest
{
	@Test
	public void perHourOverTimedRounds()
	{
		MixRounds g = new MixRounds();
		assertEquals(-1, g.resinPerHour(), 0);
		assertNull(g.best());
		// 100 ticks is a minute: 42 mox, 28 aga, 14 lye and 1,000 xp a minute.
		g.add(new MixRounds.Round(1, 100, 42, 28, 14, 1000, 3));
		g.add(new MixRounds.Round(2, 0, 500, 500, 500, 50000, 3));
		assertEquals(42 * 60, g.perHour(Mixology.Paste.MOX), 1e-6);
		assertEquals(84 * 60, g.resinPerHour(), 1e-6);
		assertEquals(60000, g.xpPerHour(), 1e-6);
		assertSame(g.all().get(0), g.best());
	}

	@Test
	public void hoursToAReward()
	{
		MixRounds g = new MixRounds();
		g.add(new MixRounds.Round(1, 100, 10, 10, 10, 100, 3));
		// 600 of each an hour: 420/70/30 apprentice pack from nothing is 0.7 hours, mox last.
		assertEquals(0.7, g.hoursTo(Mixology.Reward.APPRENTICE_PACK, new int[3]), 1e-9);
		assertEquals(0, g.hoursTo(Mixology.Reward.APPRENTICE_PACK, new int[]{420, 70, 30}), 0);
		MixRounds lyeless = new MixRounds();
		lyeless.add(new MixRounds.Round(1, 100, 10, 10, 0, 100, 3));
		assertEquals(-1, lyeless.hoursTo(Mixology.Reward.APPRENTICE_PACK, new int[3]), 0);
	}

	@Test
	public void savesAndLoads()
	{
		MixRounds g = new MixRounds();
		g.add(new MixRounds.Round(1767225600000L, 120, 40, 30, 20, 2000, 3));
		g.add(new MixRounds.Round(1767225700000L, 5000, 1, 2, 3, 4, 1));
		MixRounds back = MixRounds.decode(g.encode());
		assertEquals(g.encode(), back.encode());
		// A round with a break in it is kept, but not timed.
		assertEquals(0, back.last().ticks);
		assertEquals(0, MixRounds.decode("2;1,2,3,4,5,6,7").count());
		assertEquals(1, MixRounds.decode("1;1,2,3,4,5,6,7;x;-1,2,3,4,5,6,7").count());
		for (int i = 0; i < 150; i++)
		{
			g.add(new MixRounds.Round(i, 100, 1, 1, 1, 1, 3));
		}
		assertEquals(MixRounds.KEEP, g.count());
	}
}
