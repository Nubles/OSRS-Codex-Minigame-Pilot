package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Test;

/** The tick coach: the floor of a loop's walks, its biggest leak, and the session's figures. */
public class TickCoachTest
{
	private static Pace.Round run(Pace p, int n, int stage, String leg, String place, boolean still, int moved, int route, int units, int xp)
	{
		Pace.Round r = null;
		for (int i = 0; i < n; i++)
		{
			Pace.Round got = p.tick(stage, leg, place, still, false, moved, route, units, null, xp);
			if (got != null)
			{
				r = got;
			}
		}
		return r;
	}

	/** One loop: mine 10 ticks, then a walk of {@code tiles} to the hopper at {@code perTick} tiles a tick, then idle. */
	private static Pace.Round loop(Pace p, int tiles, int perTick, int idle, int xpStart, int xpEnd)
	{
		run(p, 10, 0, "mine", "a vein", false, 0, -1, 0, xpStart);
		run(p, (tiles + perTick - 1) / perTick, 1, "hopper", "the hopper", false, perTick, tiles, 0, xpStart);
		run(p, idle, 1, "hopper", "the hopper", true, 0, tiles, 0, xpStart);
		return run(p, 1, 0, "mine", "a vein", false, 0, -1, 27, xpEnd);
	}

	@Test
	public void runningTheShortestWayIsTheFloor()
	{
		Pace p = new Pace();
		run(p, 1, 0, "mine", "a vein", false, 0, -1, 0, 0);
		Pace.Round r = loop(p, 20, 2, 0, 0, 0);
		// 20 tiles at two a tick (the wiki's running pace) is 10 ticks, and that is what it took.
		assertEquals(10, r.floorTicks());
		assertEquals(10, r.footTicks());
		assertEquals(Pace.Leak.Kind.NONE, r.leak().kind);
	}

	@Test
	public void walkingInsteadOfRunningIsAWalkLeak()
	{
		Pace p = new Pace();
		run(p, 1, 0, "mine", "a vein", false, 0, -1, 0, 0);
		Pace.Round r = loop(p, 20, 1, 0, 0, 0);
		assertEquals(10, r.floorTicks());
		assertEquals(20, r.footTicks());
		Pace.Leak leak = r.leak();
		assertEquals(Pace.Leak.Kind.WALK, leak.kind);
		assertEquals(10, leak.ticks);
		assertEquals("10 ticks lost on foot to the hopper", leak.text(null));
	}

	@Test
	public void theBiggerOfIdleAndWalkingIsTheLeak()
	{
		Pace p = new Pace();
		run(p, 1, 0, "mine", "a vein", false, 0, -1, 0, 0);
		// Walked 4 ticks over the floor, but stood still 7 ticks past the click's grace.
		Pace.Round r = loop(p, 8, 1, 8, 0, 0);
		Pace.Leak leak = r.leak();
		assertEquals(Pace.Leak.Kind.IDLE, leak.kind);
		assertEquals("Idle 7 ticks at the hopper", leak.text(null));
	}

	@Test
	public void aOneTickWobbleIsNoLeak()
	{
		Pace p = new Pace();
		run(p, 1, 0, "mine", "a vein", false, 0, -1, 0, 0);
		// Two tiles walked, one tick over its floor: under LEAK_MIN.
		Pace.Round r = loop(p, 2, 1, 0, 0, 0);
		assertEquals(Pace.Leak.Kind.NONE, r.leak().kind);
		assertTrue(r.leak().text(null).startsWith("No leak"));
	}

	@Test
	public void aStageOverItsBestStandsInWhenNothingElseLeaked()
	{
		Pace p = new Pace();
		run(p, 1, 0, "mine", "a vein", false, 0, -1, 0, 0);
		loop(p, 20, 2, 0, 0, 0);
		// Same shape, but mining took 6 ticks longer: no idle, no slow walk, a slow stage.
		run(p, 15, 0, "mine", "a vein", false, 0, -1, 27, 0);
		run(p, 10, 1, "hopper", "the hopper", false, 2, 20, 27, 0);
		Pace.Round r = run(p, 1, 0, "mine", "a vein", false, 0, -1, 54, 0);
		Pace.Leak leak = r.leak();
		assertEquals(Pace.Leak.Kind.STAGE, leak.kind);
		assertEquals("Mine 5 ticks over your best for it", leak.text(new String[]{"Mine", "Hopper"}));
	}

	@Test
	public void experienceIsCountedPerLoopOnlyWhenKnownThroughout()
	{
		Pace p = new Pace();
		run(p, 1, 0, "mine", "a vein", false, 0, -1, 0, 1000);
		Pace.Round r = loop(p, 20, 2, 0, 1000, 1500);
		assertEquals(500, r.xp());
		// Skills not arrived for part of the next loop: its experience is unknown.
		run(p, 3, 0, "mine", "a vein", false, 0, -1, 27, -1);
		run(p, 10, 1, "hopper", "the hopper", false, 2, 20, 27, 1600);
		Pace.Round r2 = run(p, 1, 0, "mine", "a vein", false, 0, -1, 54, 1700);
		assertEquals(-1, r2.xp());
	}

	@Test
	public void loopsAnHourAndExperienceAnHourComeFromTheLoopsThemselves()
	{
		Pace p = new Pace();
		run(p, 1, 0, "mine", "a vein", false, 0, -1, 0, 0);
		int xp = 0;
		for (int i = 0; i < 4; i++)
		{
			loop(p, 20, 2, 0, xp, xp + 300);
			xp += 300;
		}
		Loops loops = Loops.of(p.rounds());
		assertEquals(4, loops.count());
		// Every loop is 21 ticks (10 mining, 10 walking, the tick back at the vein): 12.6 seconds.
		assertEquals(84, loops.ticks());
		assertEquals(Math.round(4 * 3600 / (84 * 0.6)), loops.perHour());
		assertEquals(Math.round(1200 * 3600 / (84 * 0.6)), loops.xpPerHour());
	}

	@Test
	public void noLoopsMeansNoFigures()
	{
		Loops loops = Loops.of(null);
		assertEquals(0, loops.perHour());
		assertEquals(-1, loops.xpPerHour());
		assertTrue(loops.histogram(null, 0).isEmpty());
	}

	@Test
	public void theHistogramSpansTheLoopsInRoundBins()
	{
		Pace p = new Pace();
		run(p, 1, 0, "mine", "a vein", false, 0, -1, 0, 0);
		Pace.Round last = null;
		for (int tiles : new int[]{20, 40, 60, 100, 20})
		{
			last = loop(p, tiles, 2, 0, 0, 0);
		}
		Loops loops = Loops.of(p.rounds());
		List<Loops.Bin> bins = loops.histogram(last, p.best(last.kind));
		assertTrue(bins.size() <= Loops.MAX_BINS);
		int total = 0;
		boolean marked = false;
		for (Loops.Bin b : bins)
		{
			total += b.count;
			assertEquals(bins.get(0).to - bins.get(0).from, b.to - b.from);
			marked |= b.last;
		}
		assertEquals(5, total);
		assertTrue(marked);
		// Loops of 21 to 61 ticks: bins of 5 would take 9 bars, so 5 bars of 10 ticks, from 20.
		assertEquals(20, bins.get(0).from);
		assertEquals(10, bins.get(0).to - bins.get(0).from);
		assertTrue(bins.get(0).last);
	}

	@Test
	public void aRoundWithABreakIsLeftOut()
	{
		Pace p = new Pace();
		run(p, 1, 0, "mine", "a vein", false, 0, -1, 0, 0);
		loop(p, 20, 2, 0, 0, 0);
		loop(p, 20, 2, 0, 0, 0);
		Pace.Round longOne = loop(p, 20, 2, 200, 0, 0);
		assertTrue(longOne.hadBreak);
		assertEquals(Pace.Leak.Kind.NONE, longOne.leak().kind);
		assertEquals(2, Loops.of(p.rounds()).count());
		assertFalse(Loops.of(p.rounds()).histogram(longOne, 0).stream().anyMatch(b -> b.last));
	}
}
