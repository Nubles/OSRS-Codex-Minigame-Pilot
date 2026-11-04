package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** Your games kept, and your pace from them. */
public class VolcanicLogTest
{
	private static final long T = 1_767_225_600_000L;
	private static final long MIN = 60_000L;

	@Test
	public void paceInTheMineAndWithTheWaits()
	{
		VolcanicLog l = new VolcanicLog();
		assertEquals(-1, l.pointsPerHour(), 0);
		assertNull(l.best());
		// Three games of 10 minutes (1,000 ticks), one every 12 minutes.
		l.add(new VolcanicLog.Game(T, 1000, 2000, 30_000, 3, VolcanicLog.End.LEFT));
		l.add(new VolcanicLog.Game(T + 12 * MIN, 1000, 2400, 33_000, 3, VolcanicLog.End.LEFT));
		l.add(new VolcanicLog.Game(T + 24 * MIN, 1000, 2600, 36_000, 3, VolcanicLog.End.DIED));
		assertEquals(7000 * 2.0, l.pointsPerHour(), 0.01);
		assertEquals(99_000 * 2.0, l.xpPerHour(), 0.01);
		// The last two followed another 12 minutes after: 5,000 points in 24 minutes.
		assertEquals(5000 * 60 / 24.0, l.pointsPerHourSitting(), 0.01);
		assertEquals(69_000 * 60 / 24.0, l.xpPerHourSitting(), 0.01);
		assertEquals(2600, l.best().points);
		assertEquals(2333, l.averagePoints());
		assertEquals(1, l.deaths());
		assertEquals(5000, l.pointsSince(T));
	}

	@Test
	public void aLongBreakIsNotAWait()
	{
		VolcanicLog l = new VolcanicLog();
		l.add(new VolcanicLog.Game(T, 1000, 2000, 30_000, 1, VolcanicLog.End.LEFT));
		l.add(new VolcanicLog.Game(T + 12 * MIN, 1000, 2000, 30_000, 1, VolcanicLog.End.LEFT));
		l.add(new VolcanicLog.Game(T + 300 * MIN, 1000, 2000, 30_000, 1, VolcanicLog.End.LEFT));
		// Only one game followed another within a sitting: not enough.
		assertEquals(-1, l.pointsPerHourSitting(), 0);
	}

	@Test
	public void roundTripAndDamage()
	{
		VolcanicLog l = new VolcanicLog();
		l.add(new VolcanicLog.Game(T, 1000, 2000, 30_000, 3, VolcanicLog.End.LEFT));
		l.add(new VolcanicLog.Game(T + 1, 178, 406, 4_200, 1, VolcanicLog.End.LOGOUT));
		l.add(new VolcanicLog.Game(T + 2, 900, 800, 0, 0, VolcanicLog.End.ERUPTED));
		VolcanicLog back = VolcanicLog.decode(l.encode());
		assertEquals(l.encode(), back.encode());
		assertEquals(3, back.count());
		assertEquals(VolcanicLog.End.ERUPTED, back.last().end);
		assertEquals(0, VolcanicLog.decode("2;1,2,3,4,5,l").count());
		assertEquals(1, VolcanicLog.decode("1;" + T + ",10,x,1,1,l;" + T + ",10,5,1,1,l;1,2,3,4,5,z").count());
		// Out-of-range figures are held in bounds.
		VolcanicLog.Game g = VolcanicLog.decode("1;" + T + ",99999,999999,9999999,500,l").last();
		assertEquals(0, g.ticks);
		assertEquals(VolcanicLog.POINTS_MAX, g.points);
		assertEquals(VolcanicLog.XP_MAX, g.xp);
		assertEquals(99, g.players);
	}

	@Test
	public void keepsTheLatest()
	{
		VolcanicLog l = new VolcanicLog();
		for (int i = 0; i < VolcanicLog.KEEP + 10; i++)
		{
			l.add(new VolcanicLog.Game(T + i, 100, i, 0, 1, VolcanicLog.End.LEFT));
		}
		assertEquals(VolcanicLog.KEEP, l.count());
		assertEquals(10, l.all().get(0).points);
	}
}
