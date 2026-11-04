package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Barracuda.Rank;
import com.codex.minigamepilot.core.Barracuda.Trial;
import org.junit.Test;

public class BarracudaRunsTest
{
	private static BarracudaRuns.Reading in(int clock, int done, int xp)
	{
		BarracudaRuns.Reading r = new BarracudaRuns.Reading();
		r.inTrial = true;
		r.trial = Trial.JUBBLY_JIVE;
		r.rank = Rank.SWORDFISH;
		r.clock = clock;
		r.done = done;
		r.of = 21;
		r.xp = xp;
		r.x = 3000;
		r.y = 3000 + done;
		return r;
	}

	private static BarracudaRuns.Reading out(int xp)
	{
		BarracudaRuns.Reading r = new BarracudaRuns.Reading();
		r.xp = xp;
		return r;
	}

	@Test
	public void aFinishedRunWithItsSplitsAndExperience()
	{
		BarracudaRuns runs = new BarracudaRuns();
		assertNull(runs.tick(100, in(0, 0, 1_000), 0));
		assertTrue(runs.running());
		assertNull(runs.tick(130, in(50, 1, 1_000), 0));
		assertNull(runs.tick(131, in(52, 3, 1_000), 0));
		assertEquals(3, runs.splits().size());
		assertEquals(30, runs.splits().get(0).tick);
		assertEquals(31, runs.splits().get(2).tick);
		assertEquals(3003, runs.splits().get(2).y);
		BarracudaRuns.Reading done = in(200, 21, 1_000);
		done.completed = true;
		assertNull(runs.tick(300, done, 0));
		assertTrue(runs.finished());
		// The trial's own clock is the time: 200 ticks.
		assertEquals(200, runs.ticks(305));
		assertNull(runs.tick(301, out(2_700), 0));
		assertNull(runs.tick(302, out(2_700), 0));
		assertNull(runs.tick(303, out(2_700), 0));
		BarracudaLog.Run r = runs.tick(304, out(2_700), 9);
		assertNotNull(r);
		assertEquals(BarracudaLog.End.FINISHED, r.end);
		assertEquals(200, r.ticks);
		assertEquals(1_700, r.xp);
		assertEquals(Trial.JUBBLY_JIVE, r.trial);
		assertEquals(Rank.SWORDFISH, r.rank);
		assertTrue(r.beat());
		assertFalse(runs.open());
	}

	@Test
	public void aResetEndsTheRunAndBeginsAnother()
	{
		BarracudaRuns runs = new BarracudaRuns();
		runs.tick(10, in(0, 0, 0), 0);
		runs.tick(50, in(66, 1, 0), 0);
		BarracudaLog.Run r = runs.tick(51, in(0, 0, 0), 0);
		assertNotNull(r);
		assertEquals(BarracudaLog.End.ABANDONED, r.end);
		assertEquals(1, r.splits().size());
		assertTrue(runs.open());
		assertEquals(0, runs.splits().size());
	}

	@Test
	public void backInsideTheReturnWindowGoesOn()
	{
		BarracudaRuns runs = new BarracudaRuns();
		runs.tick(10, in(0, 0, 0), 0);
		assertNull(runs.tick(11, out(0), 0));
		assertNull(runs.tick(12, in(3, 0, 0), 0));
		assertTrue(runs.running());
	}

	@Test
	public void aLogoutKeepsTheRunSoFar()
	{
		BarracudaRuns runs = new BarracudaRuns();
		assertNull(runs.logout(5, 0, 0));
		runs.tick(10, in(0, 0, 100), 0);
		BarracudaLog.Run r = runs.logout(40, 160, 0);
		assertEquals(BarracudaLog.End.LOGOUT, r.end);
		assertEquals(30, r.ticks);
		assertEquals(60, r.xp);
	}

	@Test
	public void noTrialNoRun()
	{
		BarracudaRuns runs = new BarracudaRuns();
		BarracudaRuns.Reading r = in(0, 0, 0);
		r.trial = null;
		runs.tick(1, r, 0);
		assertFalse(runs.open());
	}
}
