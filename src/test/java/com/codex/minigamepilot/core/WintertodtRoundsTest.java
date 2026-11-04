package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Following Wintertodt rounds off the HUD. */
public class WintertodtRoundsTest
{
	@Test
	public void aWholeRoundFromTheCountdown()
	{
		WintertodtRounds r = new WintertodtRounds();
		assertNull(r.tick(0, true, -1, 30, -1, 0));
		assertNull(r.tick(50, true, 100, -1, 0, 0));
		assertTrue(r.open());
		assertFalse(r.late());
		r.hit();
		assertNull(r.tick(400, true, 3, -1, 980, 0));
		WintertodtLog.Game g = r.tick(450, true, 0, -1, 1010, 99);
		assertNotNull(g);
		assertTrue(g.subdued);
		assertEquals(400, g.ticks);
		assertEquals(50, g.wait);
		assertEquals(1010, g.points);
		assertEquals(1, g.hits);
		// The bar still reads 0 until the countdown: not another round.
		assertNull(r.tick(451, true, 0, -1, 1010, 0));
		assertFalse(r.open());
		assertNull(r.tick(452, true, -1, 59, 1010, 0));
		assertEquals(1, r.rounds());
	}

	@Test
	public void theCountdownEndsARoundToo()
	{
		WintertodtRounds r = new WintertodtRounds();
		r.tick(0, true, 40, -1, 300, 0);
		assertTrue(r.late());
		WintertodtLog.Game g = r.tick(10, true, -1, 60, 300, 0);
		assertTrue(g.subdued);
		assertEquals(0, g.ticks);
		assertEquals(300, g.points);
	}

	@Test
	public void leavingOrLoggingOutLosesIt()
	{
		WintertodtRounds r = new WintertodtRounds();
		r.tick(0, true, -1, 5, -1, 0);
		r.tick(10, true, 100, -1, 0, 0);
		r.tick(20, true, 90, -1, 250, 0);
		WintertodtLog.Game g = r.tick(21, false, -1, -1, -1, 0);
		assertFalse(g.subdued);
		assertEquals(0, g.points);
		r.tick(30, true, 80, -1, 0, 0);
		g = r.logout(40, 0);
		assertFalse(g.subdued);
		assertNull(r.logout(41, 0));
	}
}
