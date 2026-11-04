package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/** Runs found from the trip to the fountain and what came with it, and juice from the pack. */
public class GardenRunsTest
{
	private GardenRuns g;
	private int tick;
	private GardenLog.Run last;
	private final int[] juiceMade = new int[4];
	// Where the player is now, schematic.
	private int x;
	private int y;
	private boolean fountain;
	private Garden.Season elemental;
	private final int[] sqirks = new int[4];
	private final int[] juice = new int[4];
	private int farming = 1_000_000;
	private boolean inGarden = true;

	@Before
	public void setUp()
	{
		g = new GardenRuns();
		tick = 100;
		x = 2910;
		y = 5480;
		fountain = true;
	}

	private void step(int n)
	{
		for (int i = 0; i < n; i++)
		{
			GardenRuns.Reading r = new GardenRuns.Reading();
			r.inGarden = inGarden;
			r.x = x;
			r.y = y;
			r.plane = 0;
			r.byFountain = fountain;
			r.elemental = elemental;
			r.tree = Garden.Season.SUMMER;
			System.arraycopy(sqirks, 0, r.sqirks, 0, 4);
			System.arraycopy(juice, 0, r.juice, 0, 4);
			r.farming = farming;
			GardenRuns.Result res = g.tick(tick++, r, 1_000L * tick);
			if (res.run != null)
			{
				last = res.run;
			}
			for (int j = 0; j < 4; j++)
			{
				juiceMade[j] += res.juice[j];
			}
		}
	}

	/** Walk off into the maze, a tile or two a tick, for n ticks. */
	private void walk(int n)
	{
		fountain = false;
		for (int i = 0; i < n; i++)
		{
			y += 2;
			step(1);
		}
	}

	/** Sent back to the fountain. */
	private void toFountain()
	{
		x = 2910;
		y = 5480;
		fountain = true;
		step(1);
	}

	@Test
	public void summerSqirk()
	{
		step(2);
		walk(10);
		sqirks[Garden.Season.SUMMER.ordinal()]++;
		farming += 60;
		step(1);
		toFountain();
		step(3);
		assertSame(GardenLog.Outcome.SQIRK, last.outcome);
		assertSame(Garden.Season.SUMMER, last.season);
		// Arrived at tick 100; two ticks by the fountain, ten walking, one picking, the trip on tick 113.
		assertEquals(13, last.ticks);
		assertEquals(1, g.runs());
		assertEquals(1, g.paid());
	}

	@Test
	public void rewardsLandingAfterTheTrip()
	{
		step(1);
		walk(8);
		toFountain();
		sqirks[Garden.Season.SUMMER.ordinal()]++;
		farming += 60;
		step(3);
		assertSame(GardenLog.Outcome.SQIRK, last.outcome);
	}

	@Test
	public void caughtBySummerElemental()
	{
		step(1);
		walk(5);
		elemental = Garden.Season.SUMMER;
		step(1);
		elemental = null;
		toFountain();
		step(3);
		assertSame(GardenLog.Outcome.CAUGHT, last.outcome);
		assertSame(Garden.Season.SUMMER, last.season);
		assertEquals(0, g.paid());
	}

	@Test
	public void caughtAgainLapFromTheLastTrip()
	{
		step(1);
		walk(5);
		elemental = Garden.Season.SUMMER;
		step(1);
		elemental = null;
		toFountain();
		step(3);
		walk(20);
		sqirks[Garden.Season.SUMMER.ordinal()]++;
		farming += 60;
		toFountain();
		step(3);
		assertSame(GardenLog.Outcome.SQIRK, last.outcome);
		// From the last trip (tick 107) to this one (tick 131).
		assertEquals(24, last.ticks);
		assertEquals(2, g.runs());
	}

	@Test
	public void herbsFromAutumn()
	{
		step(1);
		walk(5);
		elemental = Garden.Season.AUTUMN;
		step(1);
		elemental = null;
		farming += 50;
		toFountain();
		step(3);
		assertSame(GardenLog.Outcome.HERBS, last.outcome);
		assertSame(Garden.Season.AUTUMN, last.season);
	}

	@Test
	public void autumnSqirkIsNotHerbs()
	{
		step(1);
		walk(5);
		sqirks[Garden.Season.AUTUMN.ordinal()]++;
		farming += 50;
		toFountain();
		step(3);
		assertSame(GardenLog.Outcome.SQIRK, last.outcome);
		assertSame(Garden.Season.AUTUMN, last.season);
	}

	@Test
	public void aJumpWithNothingIsNoRun()
	{
		step(1);
		walk(5);
		toFountain();
		step(3);
		assertNull(last);
		assertEquals(0, g.runs());
	}

	@Test
	public void walkingAboutTheFountainIsNoRun()
	{
		step(1);
		elemental = Garden.Season.SUMMER;
		for (int i = 0; i < 10; i++)
		{
			x++;
			step(1);
		}
		assertNull(last);
	}

	@Test
	public void leavingKeepsATripAlreadyMade()
	{
		step(1);
		walk(5);
		sqirks[Garden.Season.SUMMER.ordinal()]++;
		farming += 60;
		toFountain();
		inGarden = false;
		step(1);
		assertSame(GardenLog.Outcome.SQIRK, last.outcome);
	}

	@Test
	public void logoutKeepsATripAlreadyMade()
	{
		step(1);
		walk(5);
		sqirks[Garden.Season.SUMMER.ordinal()]++;
		farming += 60;
		toFountain();
		GardenLog.Run r = g.logout(5L);
		assertSame(GardenLog.Outcome.SQIRK, r.outcome);
		assertNull(g.logout(6L));
	}

	@Test
	public void juiceCrushed()
	{
		int s = Garden.Season.SUMMER.ordinal();
		sqirks[s] = 4;
		step(1);
		sqirks[s] = 2;
		juice[s] = 1;
		step(1);
		sqirks[s] = 0;
		juice[s] = 2;
		step(1);
		assertEquals(2, juiceMade[s]);
		// Juice out of the bank is not juice made.
		juice[s] = 10;
		step(1);
		assertEquals(2, juiceMade[s]);
		// Crushed outside the garden too.
		inGarden = false;
		sqirks[s] = 2;
		step(1);
		sqirks[s] = 0;
		juice[s] = 11;
		step(1);
		assertEquals(3, juiceMade[s]);
		assertTrue(g.lap(tick) < 0);
	}
}
