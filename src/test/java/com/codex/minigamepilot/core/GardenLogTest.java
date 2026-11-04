package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The kept runs, their pace, and the time to a target from it. */
public class GardenLogTest
{
	private static GardenLog summer(int sqirk, int caught, int ticks)
	{
		GardenLog l = new GardenLog();
		for (int i = 0; i < sqirk; i++)
		{
			l.add(new GardenLog.Run(1_000L + i, ticks, Garden.Season.SUMMER, GardenLog.Outcome.SQIRK));
		}
		for (int i = 0; i < caught; i++)
		{
			l.add(new GardenLog.Run(2_000L + i, ticks, Garden.Season.SUMMER, GardenLog.Outcome.CAUGHT));
		}
		return l;
	}

	@Test
	public void pace()
	{
		// 3 of 4 runs paid, a minute a lap (100 ticks).
		GardenLog.Pace p = summer(3, 1, 100).pace(Garden.Season.SUMMER);
		assertEquals(4, p.runs);
		assertEquals(0.75, p.successRate(), 1e-9);
		assertEquals(0.75, p.sqirksPerRun(), 1e-9);
		assertEquals(60, p.runsPerHour(), 1e-9);
		assertEquals(45, p.sqirksPerHour(), 1e-9);
		assertEquals(45 * 1_500, p.thievingXpPerHour(), 1e-6);
		assertEquals(45 * 60, p.farmingXpPerHour(), 1e-6);
		assertEquals(-1, new GardenLog().pace(Garden.Season.SUMMER).sqirksPerHour(), 0);
	}

	@Test
	public void herbsPayButAreNoSqirk()
	{
		GardenLog l = summer(1, 0, 100);
		l.add(new GardenLog.Run(5, 100, Garden.Season.SUMMER, GardenLog.Outcome.HERBS));
		GardenLog.Pace p = l.pace(Garden.Season.SUMMER);
		assertEquals(1.0, p.successRate(), 1e-9);
		assertEquals(0.5, p.sqirksPerRun(), 1e-9);
		assertEquals(110, l.farmingXp());
	}

	@Test
	public void recentRunsOnly()
	{
		GardenLog l = summer(0, 200, 100);
		for (int i = 0; i < GardenLog.RECENT; i++)
		{
			l.add(new GardenLog.Run(9_000L + i, 100, Garden.Season.SUMMER, GardenLog.Outcome.SQIRK));
		}
		assertEquals(1.0, l.pace(Garden.Season.SUMMER).successRate(), 1e-9);
	}

	@Test
	public void untimedLapsCountForSuccessNotPace()
	{
		GardenLog l = summer(2, 0, 0);
		GardenLog.Pace p = l.pace(Garden.Season.SUMMER);
		assertEquals(1.0, p.successRate(), 1e-9);
		assertEquals(-1, p.sqirksPerHour(), 0);
		assertEquals(0, new GardenLog.Run(1, GardenLog.LONGEST + 1, Garden.Season.SUMMER, GardenLog.Outcome.SQIRK).ticks);
	}

	@Test
	public void roundTrip()
	{
		GardenLog l = summer(2, 1, 90);
		l.add(new GardenLog.Run(9, 300, Garden.Season.AUTUMN, GardenLog.Outcome.HERBS));
		l.addJuice(Garden.Season.SUMMER, 23);
		l.addJuice(Garden.Season.WINTER, 1);
		GardenLog back = GardenLog.decode(l.encode(), l.encodeJuice());
		assertEquals(l.encode(), back.encode());
		assertEquals(l.encodeJuice(), back.encodeJuice());
		assertEquals(23 * 3_000 + 350, back.juiceXp());
		assertSame(Garden.Season.AUTUMN, back.lastSeason());
		assertEquals(90, back.bestLap(Garden.Season.SUMMER));
	}

	@Test
	public void damagedSaves()
	{
		GardenLog l = GardenLog.decode("1;5,90,u,q;x,1,u,q;5,90,z,q;5,90,u,z;5,-1,u,q;;5,90,a,c", "1;1,x,-3,4,9");
		assertEquals(2, l.count());
		assertEquals(1, l.juice(Garden.Season.WINTER));
		assertEquals(0, l.juice(Garden.Season.SPRING));
		assertEquals(0, l.juice(Garden.Season.AUTUMN));
		assertEquals(4, l.juice(Garden.Season.SUMMER));
		assertEquals(0, GardenLog.decode("2;5,90,u,q", "2;1,1,1,1").count());
		assertEquals(0, GardenLog.decode(null, null).count());
	}

	@Test
	public void etaToThieving()
	{
		GardenEta.Holding h = new GardenEta.Holding();
		h.xp = Goals.xpFor(80) - 6_001;
		GardenLog.Pace p = summer(3, 1, 100).pace(Garden.Season.SUMMER);
		GardenEta.Answer a = GardenEta.of(GardenEta.Kind.THIEVING_LEVEL, 80, Garden.Season.SUMMER, h, p);
		// 6,001 xp: three glasses, six sq'irks; at 0.75 a run, 8 runs; at 45 an hour, 8 minutes.
		assertEquals(6, a.sqirks);
		assertEquals(8, a.runs);
		assertEquals(6 / 45.0, a.hours, 1e-9);
		// Two glasses held and a sq'irk over: one glass left to make, one sq'irk to pick.
		h.juice[Garden.Season.SUMMER.ordinal()] = 2;
		h.sqirks[Garden.Season.SUMMER.ordinal()] = 1;
		a = GardenEta.of(GardenEta.Kind.THIEVING_LEVEL, 80, Garden.Season.SUMMER, h, p);
		assertEquals(1, a.sqirks);
		assertEquals(2, a.runs);
		h.juice[Garden.Season.SUMMER.ordinal()] = 3;
		a = GardenEta.of(GardenEta.Kind.THIEVING_LEVEL, 80, Garden.Season.SUMMER, h, p);
		assertTrue(a.done());
		assertEquals(0, a.sqirks);
		// No pace yet: no time.
		h.juice[Garden.Season.SUMMER.ordinal()] = 0;
		a = GardenEta.of(GardenEta.Kind.THIEVING_LEVEL, 80, Garden.Season.SUMMER, h, new GardenLog().pace(Garden.Season.SUMMER));
		assertEquals(-1, a.hours, 0);
		assertEquals(-1, a.runs);
	}

	@Test
	public void etaToFarmingAndJuice()
	{
		GardenEta.Holding h = new GardenEta.Holding();
		h.xp = Goals.xpFor(50) - 600;
		GardenLog.Pace p = summer(3, 1, 100).pace(Garden.Season.SUMMER);
		GardenEta.Answer a = GardenEta.of(GardenEta.Kind.FARMING_LEVEL, 50, Garden.Season.SUMMER, h, p);
		// 45 Farming xp a run on average (60 x 0.75): 14 runs; 2,700 an hour.
		assertEquals(14, a.runs);
		assertEquals(600 / 2_700.0, a.hours, 1e-9);
		GardenEta.Holding j = new GardenEta.Holding();
		j.juice[Garden.Season.SUMMER.ordinal()] = 20;
		j.sqirks[Garden.Season.SUMMER.ordinal()] = 3;
		a = GardenEta.of(GardenEta.Kind.JUICE, 22, Garden.Season.SUMMER, j, p);
		assertEquals(21, a.have);
		assertEquals(1, a.sqirks);
	}
}
