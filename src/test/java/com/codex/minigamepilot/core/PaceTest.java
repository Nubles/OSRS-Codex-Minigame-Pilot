package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PaceTest
{
	/** Feeds n ticks of one stage and leg. */
	private static Pace.Round run(Pace p, int n, int stage, String leg, String place, boolean still, int moved, int route, int units)
	{
		Pace.Round r = null;
		for (int i = 0; i < n; i++)
		{
			Pace.Round got = p.tick(stage, leg, place, still, false, moved, route, units, null);
			if (got != null)
			{
				r = got;
			}
		}
		return r;
	}

	@Test
	public void roundsAreCutWhenTheLoopComesBackToTheStart()
	{
		Pace p = new Pace();
		assertNull(run(p, 10, 0, "mine", "a vein", false, 0, -1, 0));
		assertNull(run(p, 5, 1, "hopper", "the hopper", false, 1, 5, 0));
		Pace.Round r = run(p, 1, 0, "mine", "a vein", false, 0, -1, 27);
		assertNotNull(r);
		assertEquals(15, r.ticks);
		assertEquals("3/27", r.kind);
		assertTrue(r.isBest());
	}

	@Test
	public void thePartRoundBeforeTheFirstStartIsSkipped()
	{
		Pace p = new Pace();
		assertNull(run(p, 8, 2, "sack", "the sack", false, 0, -1, 0));
		assertNull(run(p, 3, 0, "mine", "a vein", false, 0, -1, 0));
		assertNull(run(p, 3, 1, "hopper", "the hopper", false, 0, -1, 0));
		Pace.Round r = run(p, 1, 0, "mine", "a vein", false, 0, -1, 0);
		assertEquals(6, r.ticks);
	}

	@Test
	public void standingStillPastTheFirstTickIsCountedWhereItHappened()
	{
		Pace p = new Pace();
		run(p, 2, 0, "mine", "a vein", false, 0, -1, 0);
		run(p, 6, 1, "bank", "the bank", true, 0, -1, 0);
		Pace.Round r = run(p, 1, 0, "mine", "a vein", false, 0, -1, 0);
		assertEquals(5, r.idle);
		assertEquals("the bank", r.idlePlace);
		assertEquals("Stood still 5 ticks at the bank", r.advice());
	}

	@Test
	public void waitingIsNotStandingStill()
	{
		Pace p = new Pace();
		run(p, 1, 0, "bank", "the bank", false, 0, -1, 0);
		for (int i = 0; i < 6; i++)
		{
			p.tick(1, "bars", "the dispenser", true, true, 0, -1, 0, null);
		}
		Pace.Round r = run(p, 1, 0, "bank", "the bank", false, 0, -1, 0);
		assertEquals(0, r.idle);
	}

	@Test
	public void detoursAreTilesWalkedBeyondTheShortestWay()
	{
		Pace p = new Pace();
		run(p, 1, 0, "mine", "a vein", false, 0, -1, 0);
		// The hopper was 6 tiles away; 11 were walked.
		run(p, 11, 1, "hopper", "the hopper", false, 1, 6, 0);
		Pace.Round r = run(p, 1, 0, "mine", "a vein", false, 0, -1, 0);
		assertEquals(5, r.extraTiles);
		assertEquals("Walked 5 tiles more than needed to the hopper", r.advice());
	}

	@Test
	public void aTileOrTwoOverIsForgiven()
	{
		Pace p = new Pace();
		run(p, 1, 0, "mine", "a vein", false, 0, -1, 0);
		run(p, 8, 1, "hopper", "the hopper", false, 1, 6, 0);
		Pace.Round r = run(p, 1, 0, "mine", "a vein", false, 0, -1, 0);
		assertEquals(0, r.extraTiles);
	}

	@Test
	public void roundsAreComparedOnlyWithTheirOwnKind()
	{
		Pace p = new Pace();
		run(p, 10, 0, "mine", "a vein", false, 0, -1, 0);
		run(p, 10, 1, "hopper", "the hopper", false, 0, -1, 0);
		Pace.Round load = run(p, 1, 0, "mine", "a vein", false, 0, -1, 27);
		run(p, 5, 1, "hopper", "the hopper", false, 0, -1, 27);
		run(p, 5, 2, "sack", "the sack", false, 0, -1, 27);
		Pace.Round empty = run(p, 1, 0, "mine", "a vein", false, 0, -1, 54);
		assertEquals("3/27", load.kind);
		assertEquals("7/27", empty.kind);
		assertEquals(0, empty.bestBefore);
		assertEquals(20, p.best("3/27"));
	}

	@Test
	public void aSlowerRoundKeepsTheBestAndAQuickerOneTakesIt()
	{
		Pace p = new Pace();
		run(p, 1, 0, "a", "x", false, 0, -1, 0);
		run(p, 9, 1, "b", "y", false, 0, -1, 0);
		run(p, 1, 0, "a", "x", false, 0, -1, 1);
		run(p, 11, 1, "b", "y", false, 0, -1, 1);
		Pace.Round slower = run(p, 1, 0, "a", "x", false, 0, -1, 2);
		assertFalse(slower.isBest());
		assertEquals(10, slower.bestBefore);
		run(p, 7, 1, "b", "y", false, 0, -1, 2);
		Pace.Round quicker = run(p, 1, 0, "a", "x", false, 0, -1, 3);
		assertTrue(quicker.isBest());
		assertEquals(8, p.best("3/1"));
	}

	@Test
	public void aRoundWithABreakIsLeftOut()
	{
		Pace p = new Pace();
		run(p, 1, 0, "a", "x", false, 0, -1, 0);
		run(p, 9, 1, "b", "y", false, 0, -1, 0);
		Pace.Round first = run(p, 1, 0, "a", "x", false, 0, -1, 1);
		run(p, 99, 1, "b", "y", false, 0, -1, 1);
		Pace.Round slow = run(p, 1, 0, "a", "x", false, 0, -1, 2);
		assertNotNull(first);
		assertTrue(slow.hadBreak);
		assertFalse(slow.isBest());
		assertEquals(10, p.typical("3/1"));
	}

	@Test
	public void theContractARoundWasForIsKept()
	{
		Pace p = new Pace();
		p.tick(0, "job", "the contractor", false, false, 0, -1, 0, null);
		p.tick(1, "go", "the house", false, false, 0, -1, 0, "Jess Adept");
		p.tick(3, "fix", "the furniture", false, false, 0, -1, 0, "Jess Adept");
		Pace.Round r = p.tick(0, "job", "the contractor", false, false, 0, -1, 1, null);
		assertEquals("11/1/Jess Adept", r.kind);
	}

	@Test
	public void bestsSurviveASaveAndLoad()
	{
		Pace p = new Pace();
		p.loadBests("3/27=40;7/27=120;bad;x=y");
		assertEquals(40, p.best("3/27"));
		assertEquals(120, p.best("7/27"));
		assertEquals("3/27=40;7/27=120", p.saveBests());
	}

	@Test
	public void clockReadsMinutesAndSeconds()
	{
		assertEquals("1:00", Pace.clock(100));
		assertEquals("0:06", Pace.clock(10));
	}

	@Test
	public void eachStageIsTimedAndTheSlowestAgainstItsBestIsNamed()
	{
		Pace p = new Pace();
		run(p, 1, 0, "mine", "a vein", false, 0, -1, 0);
		// A first round: 4 mining, 6 at the hopper.
		run(p, 3, 0, "mine", "a vein", false, 0, -1, 0);
		run(p, 6, 1, "hopper", "the hopper", false, 1, -1, 0);
		Pace.Round first = run(p, 1, 0, "mine", "a vein", false, 0, -1, 27);
		assertEquals(4, first.stageTicks(0));
		assertEquals(6, first.stageTicks(1));
		assertEquals(0, first.stageBest(1));
		assertEquals(-1, first.slowestStage());
		// A second round of the same kind loses 7 ticks at the hopper.
		run(p, 3, 0, "mine", "a vein", false, 0, -1, 27);
		run(p, 13, 1, "hopper", "the hopper", false, 1, -1, 27);
		Pace.Round second = run(p, 1, 0, "mine", "a vein", false, 0, -1, 54);
		assertEquals(first.kind, second.kind);
		assertEquals(6, second.stageBest(1));
		assertEquals(13, second.stageTicks(1));
		assertEquals(1, second.slowestStage());
		assertArrayEquals6(p.stageBests(first.kind));
	}

	private static void assertArrayEquals6(int[] bests)
	{
		assertEquals(4, bests[0]);
		assertEquals(6, bests[1]);
	}

	@Test
	public void stageBestsAreSavedAndReadBackBesideRoundBests()
	{
		Pace p = new Pace();
		p.loadBests("3/27=15;3/27#0=4;3/27#1=6;3/27#99=2;bad#x=3");
		assertEquals(15, p.best("3/27"));
		assertEquals(4, p.stageBests("3/27")[0]);
		assertEquals(6, p.stageBests("3/27")[1]);
		assertEquals(0, p.best("3/27#0"));
		assertEquals("3/27=15;3/27#0=4;3/27#1=6", p.saveBests());
	}

	@Test
	public void fieldNotesSeeTheRoundAsBefore()
	{
		Pace p = new Pace();
		run(p, 2, 0, "mine", "a vein", false, 0, -1, 0);
		run(p, 2, 1, "hopper", "the hopper", false, 0, -1, 0);
		Pace.Round r = run(p, 1, 0, "mine", "a vein", false, 0, -1, 27);
		String json = new com.google.gson.Gson().toJson(r);
		assertFalse(json, json.contains("stage"));
		assertTrue(json, json.contains("\"ticks\":4"));
	}
}
