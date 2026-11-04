package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BrewingGamesTest
{
	private static BrewingGames.Reading in(int red, int blue, long pieces, int xp)
	{
		BrewingGames.Reading r = new BrewingGames.Reading();
		r.inGame = true;
		r.team = Brewing.Team.RED;
		r.red = red;
		r.blue = blue;
		r.pieces = pieces;
		r.xp = xp;
		return r;
	}

	private static BrewingGames.Reading out(long pieces, int xp)
	{
		BrewingGames.Reading r = new BrewingGames.Reading();
		r.pieces = pieces;
		r.xp = xp;
		return r;
	}

	@Test
	public void finishedGameKeptAfterItsRewards()
	{
		BrewingGames g = new BrewingGames();
		assertNull(g.tick(0, in(0, 0, 100, 1000), 0));
		assertTrue(g.open());
		assertNull(g.tick(1000, in(8, 5, 100, 4000), 0));
		assertEquals(8, g.rum());
		assertEquals(5, g.rival());
		BrewingGames.Reading over = out(100, 4000);
		over.over = true;
		over.overRed = 9;
		over.overBlue = 6;
		over.overPieces = 190;
		assertNull(g.tick(1001, over, 0));
		assertTrue(g.settling());
		// The pieces of eight and the experience arrive a tick later.
		assertNull(g.tick(1002, out(290, 4250), 0));
		BrewingLog.Game kept = null;
		for (int t = 1003; t < 1020 && kept == null; t++)
		{
			kept = g.tick(t, out(290, 4250), 1L);
		}
		assertNotNull(kept);
		assertEquals(BrewingLog.End.FINISHED, kept.end);
		assertEquals(Brewing.Team.RED, kept.team);
		assertEquals(9, kept.rum);
		assertEquals(6, kept.rival);
		assertEquals(190, kept.pieces);
		assertEquals(3250, kept.xp);
		assertEquals(1001, kept.ticks);
		assertEquals("Won", kept.result());
		assertEquals(1, g.games());
	}

	@Test
	public void scrollsPiecesWhenTheCountIsNotRead()
	{
		BrewingGames g = new BrewingGames();
		g.tick(0, in(0, 0, -1, 0), 0);
		BrewingGames.Reading over = out(-1, 0);
		over.over = true;
		over.overPieces = 75;
		g.tick(500, over, 0);
		BrewingLog.Game kept = null;
		for (int t = 501; t < 520 && kept == null; t++)
		{
			kept = g.tick(t, out(-1, 0), 0);
		}
		assertNotNull(kept);
		assertEquals(75, kept.pieces);
	}

	@Test
	public void leftAndLogout()
	{
		BrewingGames g = new BrewingGames();
		g.tick(0, in(1, 1, 50, 0), 0);
		g.tick(60, out(50, 0), 0);
		BrewingLog.Game kept = null;
		for (int t = 61; t < 80 && kept == null; t++)
		{
			kept = g.tick(t, out(50, 0), 0);
		}
		assertNotNull(kept);
		assertEquals(BrewingLog.End.LEFT, kept.end);
		assertEquals(0, kept.pieces);

		g.tick(100, in(0, 3, 50, 0), 0);
		BrewingLog.Game lo = g.logout(150, out(50, 0), 0);
		assertNotNull(lo);
		assertEquals(BrewingLog.End.LOGOUT, lo.end);
		assertFalse(g.open());
		assertNull(g.logout(151, out(50, 0), 0));
	}

	@Test
	public void aBlinkIsNoGame()
	{
		BrewingGames g = new BrewingGames();
		g.tick(0, in(0, 0, 0, 0), 0);
		g.tick(2, out(0, 0), 0);
		for (int t = 3; t < 30; t++)
		{
			assertNull(g.tick(t, out(0, 0), 0));
		}
		assertEquals(0, g.games());
	}

	@Test
	public void overlayStillUpAfterTheScrollStartsNoSecondGame()
	{
		BrewingGames g = new BrewingGames();
		g.tick(0, in(0, 0, 0, 0), 0);
		BrewingGames.Reading over = in(4, 2, 0, 0);
		over.over = true;
		g.tick(100, over, 0);
		int kept = 0;
		for (int t = 101; t < 140; t++)
		{
			kept += g.tick(t, in(4, 2, 40, 0), 0) != null ? 1 : 0;
		}
		assertEquals(1, kept);
		assertFalse(g.open());
		// The overlay goes, then a new game begins.
		g.tick(140, out(40, 0), 0);
		g.tick(141, in(0, 0, 40, 0), 0);
		assertTrue(g.open());
	}
}
