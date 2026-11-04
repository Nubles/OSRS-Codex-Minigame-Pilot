package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** The kept Wintertodt rounds and the pace from them. */
public class WintertodtLogTest
{
	@Test
	public void roundTrip()
	{
		WintertodtLog g = new WintertodtLog();
		g.add(new WintertodtLog.Game(1_000, 400, 100, 1200, true, 3));
		g.add(new WintertodtLog.Game(2_000, 0, 0, 900, false, 1));
		WintertodtLog back = WintertodtLog.decode(g.encode());
		assertEquals("1;1000,400,100,1200,s,3;2000,0,0,0,l,1", g.encode());
		assertEquals(2, back.count());
		assertEquals(1200, back.best().points);
		// Left before the end: "the loss of all points for the current round".
		assertEquals(0, back.last().points);
	}

	@Test
	public void paceCountsTheWait()
	{
		WintertodtLog g = new WintertodtLog();
		// 400 ticks a round and 100 waiting: 500 ticks, 300 seconds; 1,200 points.
		g.add(new WintertodtLog.Game(1, 400, 100, 1200, true, 0));
		assertEquals(14_400, g.pointsPerHour(), 1e-6);
		assertEquals(3.4 * 12, g.rewardsPerHour(), 1e-6);
		assertEquals(500, g.typicalRound());
		// Wait not known: the wiki's one minute.
		g.add(new WintertodtLog.Game(2, 400, 0, 1200, true, 0));
		assertEquals(500, g.typicalRound());
		// A round joined late is not timed, but its rewards count.
		g.add(new WintertodtLog.Game(3, 0, 0, 600, true, 0));
		assertEquals(500, g.typicalRound());
		assertEquals((3.4 + 3.4 + 2.2) / 3, g.rewardsPerRound(), 1e-9);
		assertEquals(3, g.paid());
	}

	@Test
	public void emptyAndDamaged()
	{
		WintertodtLog g = WintertodtLog.decode("1;x,1,2,3,s,0;5,-1,0,0,s,0;5,10,0,100,q,0;7,10,0,100,s,0");
		assertEquals(1, g.count());
		assertEquals(-1, WintertodtLog.decode(null).pointsPerHour(), 0);
		assertNull(WintertodtLog.decode("2;1,2,3,4,s,0").last());
		assertEquals(-1, new WintertodtLog().rewardsPerRound(), 0);
	}

	@Test
	public void keepsTheLatest()
	{
		WintertodtLog g = new WintertodtLog();
		for (int i = 0; i < WintertodtLog.KEEP + 5; i++)
		{
			g.add(new WintertodtLog.Game(i, 300, 100, 500, true, 0));
		}
		assertEquals(WintertodtLog.KEEP, g.count());
		assertEquals(5, g.all().get(0).endedAt);
	}
}
