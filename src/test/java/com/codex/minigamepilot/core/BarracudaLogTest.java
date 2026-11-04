package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Barracuda.Rank;
import com.codex.minigamepilot.core.Barracuda.Trial;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class BarracudaLogTest
{
	private static BarracudaLog.Run run(long at, Trial t, Rank k, int ticks, int xp, BarracudaLog.End end, int... splitTicks)
	{
		BarracudaLog.Split[] s = new BarracudaLog.Split[splitTicks.length];
		for (int i = 0; i < s.length; i++)
		{
			s[i] = new BarracudaLog.Split(splitTicks[i], 3000 + i, 3100 + i);
		}
		return new BarracudaLog.Run(at, t, k, ticks, xp, end, Arrays.asList(s));
	}

	@Test
	public void ghostIsTheQuickestFinishedRunOfTheCourse()
	{
		BarracudaLog l = new BarracudaLog();
		BarracudaLog.Run slow = run(1_000, Trial.TEMPOR_TANTRUM, Rank.SWORDFISH, 190, 800, BarracudaLog.End.FINISHED, 60, 120);
		BarracudaLog.Run fast = run(2_000, Trial.TEMPOR_TANTRUM, Rank.SWORDFISH, 170, 800, BarracudaLog.End.FINISHED, 50, 100);
		BarracudaLog.Run quit = run(3_000, Trial.TEMPOR_TANTRUM, Rank.SWORDFISH, 40, 0, BarracudaLog.End.ABANDONED, 20);
		BarracudaLog.Run shark = run(4_000, Trial.TEMPOR_TANTRUM, Rank.SHARK, 300, 1_200, BarracudaLog.End.FINISHED, 80);
		l.add(slow);
		l.add(fast);
		l.add(quit);
		l.add(shark);
		assertSame(fast, l.ghost(Trial.TEMPOR_TANTRUM, Rank.SWORDFISH));
		assertSame(shark, l.ghost(Trial.TEMPOR_TANTRUM, Rank.SHARK));
		assertNull(l.ghost(Trial.JUBBLY_JIVE, Rank.SWORDFISH));
		assertTrue(fast.beat());
		assertFalse(slow.beat());
		assertFalse(shark.beat());
		assertTrue(l.beaten(Trial.TEMPOR_TANTRUM, Rank.SWORDFISH));
		assertFalse(l.beaten(Trial.TEMPOR_TANTRUM, Rank.SHARK));
		assertEquals(4, l.runs(Trial.TEMPOR_TANTRUM));
		assertEquals(170, (int) l.bests(Trial.TEMPOR_TANTRUM).get(Rank.SWORDFISH));
		assertEquals(0, (int) l.bests(Trial.TEMPOR_TANTRUM).get(Rank.MARLIN));
	}

	@Test
	public void experienceAnHour()
	{
		BarracudaLog l = new BarracudaLog();
		assertEquals(-1, l.xpPerHour(null), 0);
		// 600 ticks is six minutes: 1,000 xp in it is 10,000 an hour.
		l.add(run(60_000, Trial.JUBBLY_JIVE, Rank.SWORDFISH, 300, 500, BarracudaLog.End.FINISHED));
		l.add(run(300_000, Trial.JUBBLY_JIVE, Rank.SWORDFISH, 300, 500, BarracudaLog.End.FINISHED));
		assertEquals(10_000, l.xpPerHour(Trial.JUBBLY_JIVE), 0.01);
		assertEquals(-1, l.xpPerHour(Trial.TEMPOR_TANTRUM), 0);
		// With the restarts: one pair only, not enough.
		assertEquals(-1, l.xpPerHourSitting(Trial.JUBBLY_JIVE), 0);
		l.add(run(540_000, Trial.JUBBLY_JIVE, Rank.SWORDFISH, 300, 500, BarracudaLog.End.FINISHED));
		// 1,000 xp over two four-minute gaps: 7,500 an hour.
		assertEquals(7_500, l.xpPerHourSitting(Trial.JUBBLY_JIVE), 0.01);
	}

	@Test
	public void roundTrips()
	{
		BarracudaLog l = new BarracudaLog();
		l.add(run(1_767_000_000_000L, Trial.GWENITH_GLIDE, Rank.MARLIN, 600, 9_000, BarracudaLog.End.FINISHED, 100, 200, 300));
		l.add(run(1_767_000_100_000L, Trial.TEMPOR_TANTRUM, null, 30, 0, BarracudaLog.End.LOGOUT));
		String s = l.encode();
		assertEquals("1;1767000000000,g,m,600,9000,f,100.3000.3100/200.3001.3101/300.3002.3102;1767000100000,t,-,30,0,o,", s);
		BarracudaLog back = BarracudaLog.decode(s);
		assertEquals(s, back.encode());
		assertEquals(3, back.ghost(Trial.GWENITH_GLIDE, Rank.MARLIN).splits().size());
		assertNull(back.last().rank);
		assertEquals(0, BarracudaLog.decode("2;whatever").count());
		assertEquals(0, BarracudaLog.decode(null).count());
	}

	@Test
	public void splitsOnlyGoForward()
	{
		BarracudaLog.Run r = run(1, Trial.TEMPOR_TANTRUM, Rank.SWORDFISH, 100, 0, BarracudaLog.End.FINISHED, 10, 5, 50, 150);
		assertEquals(2, r.splits().size());
		assertEquals(50, r.splits().get(1).tick);
	}

	@Test
	public void trimmingKeepsEveryGhost()
	{
		BarracudaLog l = new BarracudaLog();
		BarracudaLog.Run ghost = run(1, Trial.TEMPOR_TANTRUM, Rank.MARLIN, 400, 0, BarracudaLog.End.FINISHED);
		l.add(ghost);
		for (int i = 0; i < BarracudaLog.KEEP + 20; i++)
		{
			l.add(new BarracudaLog.Run(10 + i, Trial.TEMPOR_TANTRUM, Rank.MARLIN, 500, 0, BarracudaLog.End.FINISHED, Collections.emptyList()));
		}
		assertEquals(BarracudaLog.KEEP, l.count());
		assertSame(ghost, l.ghost(Trial.TEMPOR_TANTRUM, Rank.MARLIN));
	}
}
