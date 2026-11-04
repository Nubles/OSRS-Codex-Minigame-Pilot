package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SoulWarsGamesTest
{
	private static SoulWarsGames.Reading in(long tokens, long wins, long played)
	{
		SoulWarsGames.Reading r = new SoulWarsGames.Reading();
		r.inGame = true;
		r.team = SoulWars.Team.RED;
		r.tokens = tokens;
		r.wins = wins;
		r.played = played;
		return r;
	}

	private static SoulWarsGames.Reading out(long tokens, long wins, long played)
	{
		SoulWarsGames.Reading r = in(tokens, wins, played);
		r.inGame = false;
		r.team = null;
		return r;
	}

	private static SoulWarsGames.Reading scroll(long tokens, int blue, int red, int zeal)
	{
		SoulWarsGames.Reading r = out(tokens, -1, -1);
		r.over = true;
		r.overBlue = blue;
		r.overRed = red;
		r.overZeal = zeal;
		r.part.fragments = 24;
		r.part.damage = 310;
		return r;
	}

	@Test
	public void wonGameKeptWithTheScrollsScoresAndTheTokensRise()
	{
		SoulWarsGames g = new SoulWarsGames();
		assertNull(g.tick(0, in(100, 5, 10), 0));
		assertTrue(g.open());
		assertEquals(SoulWars.Team.RED, g.team());
		assertNull(g.tick(1500, in(100, 5, 10), 0));
		// Out of the arena; the scroll shows a few ticks later, the tokens risen by 57.
		assertNull(g.tick(1501, out(100, 5, 10), 0));
		assertTrue(g.settling());
		assertNull(g.tick(1504, scroll(157, 1, 3, 57), 0));
		assertNull(g.tick(1510, out(157, 6, 11), 0));
		SoulWarsLog.Game k = null;
		for (int t = 1511; t < 1540 && k == null; t++)
		{
			k = g.tick(t, out(157, 6, 11), 99);
		}
		assertNotNull(k);
		assertEquals(57, k.zeal);
		assertEquals(SoulWarsLog.Result.WON, k.result);
		assertEquals("3-1", k.score());
		assertEquals(1501, k.ticks);
		assertEquals(24, k.part.fragments);
		assertEquals(SoulWarsLog.End.FINISHED, k.end);
		assertEquals(1, g.games());
	}

	@Test
	public void noScrollFallsBackToTheGamesOwnCounts()
	{
		SoulWarsGames g = new SoulWarsGames();
		g.tick(0, in(0, 2, 4), 0);
		g.tick(1400, out(0, 2, 4), 0);
		SoulWarsLog.Game k = null;
		for (int t = 1401; t < 1500 && k == null; t++)
		{
			// Zeal of 45 with no win: more than a loss can give, so drawn.
			k = g.tick(t, out(45, 2, 5), 0);
		}
		assertNotNull(k);
		assertEquals(45, k.zeal);
		assertEquals(SoulWarsLog.Result.DRAWN, k.result);
		assertEquals(SoulWarsLog.End.FINISHED, k.end);
	}

	@Test
	public void resultRules()
	{
		assertEquals(SoulWarsLog.Result.LOST, SoulWarsGames.result(SoulWars.Team.BLUE, 1, 2, 1, 1, 60));
		assertEquals(SoulWarsLog.Result.DRAWN, SoulWarsGames.result(SoulWars.Team.RED, 2, 2, -1, -1, 0));
		assertEquals(SoulWarsLog.Result.WON, SoulWarsGames.result(null, 1, 2, 1, 1, 0));
		assertEquals(SoulWarsLog.Result.UNKNOWN, SoulWarsGames.result(null, -1, -1, 0, 1, 30));
		assertEquals(SoulWarsLog.Result.UNKNOWN, SoulWarsGames.result(null, -1, -1, -1, -1, 50));
		// The tokens' rise first; a rise larger than a game gives is not this game's.
		assertEquals(40, SoulWarsGames.zeal(10, 50, 33));
		assertEquals(33, SoulWarsGames.zeal(10, 500, 33));
		assertEquals(0, SoulWarsGames.zeal(10, 500, -1));
		assertEquals(20, SoulWarsGames.zeal(-1, 50, 20));
	}

	@Test
	public void leftEarlyAndLoggedOut()
	{
		SoulWarsGames g = new SoulWarsGames();
		g.tick(0, in(10, 0, 0), 0);
		g.tick(200, out(10, 0, 0), 0);
		SoulWarsLog.Game k = null;
		for (int t = 201; t < 260 && k == null; t++)
		{
			k = g.tick(t, out(10, 0, 0), 0);
		}
		assertNotNull(k);
		assertEquals(SoulWarsLog.End.LEFT, k.end);
		assertEquals(SoulWarsLog.Result.UNKNOWN, k.result);
		assertEquals(0, k.zeal);

		g.tick(300, in(10, 0, 0), 0);
		g.tick(600, in(10, 0, 0), 0);
		SoulWarsLog.Game o = g.logout(601, out(10, 0, 0), 0);
		assertNotNull(o);
		assertEquals(SoulWarsLog.End.LOGOUT, o.end);
		assertFalse(g.open());
		assertNull(g.logout(602, out(10, 0, 0), 0));
	}

	@Test
	public void aBlinkIsNoGame()
	{
		SoulWarsGames g = new SoulWarsGames();
		g.tick(0, in(10, 0, 0), 0);
		g.tick(2, out(10, 0, 0), 0);
		for (int t = 3; t < 40; t++)
		{
			assertNull(g.tick(t, out(10, 0, 0), 0));
		}
		assertEquals(0, g.games());
	}
}
