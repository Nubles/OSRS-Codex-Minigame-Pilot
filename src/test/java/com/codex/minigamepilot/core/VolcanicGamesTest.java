package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** A game followed tick by tick: its points, experience, length and how it ended. */
public class VolcanicGamesTest
{
	private static final long NOW = 1_767_225_600_000L;

	/** Plays a game from entering to coming out, the points rising to {@code points} and the experience to {@code xp} more. */
	private static VolcanicGames inside(VolcanicGames g, int from, int ticks, int points, int xp)
	{
		for (int t = 0; t < ticks; t++)
		{
			int p = points * t / Math.max(1, ticks - 1);
			assertNull(g.tick(from + t, true, p, 1_000_000 + xp * t / Math.max(1, ticks - 1), 3, 80, 600 - t * 6 / 10, t < 50 ? 1 : 2, NOW));
		}
		return g;
	}

	@Test
	public void leftByTheExit()
	{
		VolcanicGames g = inside(new VolcanicGames(), 100, 900, 2_400, 30_000);
		assertTrue(g.open());
		assertTrue(g.playing());
		assertEquals(2_400, g.gained());
		// Out: the game is settled a few ticks later, experience given on leaving counted.
		assertNull(g.tick(1000, false, -1, 1_030_000, -1, -1, -1, -1, NOW));
		assertTrue(g.settling());
		assertNull(g.tick(1001, false, -1, 1_031_500, -1, -1, -1, -1, NOW));
		VolcanicLog.Game done = g.tick(1003, false, -1, 1_031_500, -1, -1, -1, -1, NOW + 5);
		assertNotNull(done);
		assertEquals(2_400, done.points);
		assertEquals(31_500, done.xp);
		assertEquals(900, done.ticks);
		assertEquals(3, done.players);
		assertEquals(VolcanicLog.End.LEFT, done.end);
		assertEquals(1, g.games());
		assertFalse(g.settling());
	}

	@Test
	public void lastGamesPointsAreNotThisOnes()
	{
		VolcanicGames g = new VolcanicGames();
		// Going in, the count still shows the last game's 3,000 until it begins again.
		g.tick(1, true, 3_000, 0, 1, 100, 630, 1, NOW);
		g.tick(2, true, 0, 0, 1, 100, 629, 1, NOW);
		for (int t = 3; t < 60; t++)
		{
			g.tick(t, true, 406, 0, 1, 100, 600, 2, NOW);
		}
		assertEquals(406, g.gained());
	}

	@Test
	public void diedAtTheEruption()
	{
		VolcanicGames g = new VolcanicGames();
		for (int t = 0; t < 400; t++)
		{
			g.tick(t, true, 900, 0, 1, 40, Math.max(0, 300 - t), 2, NOW);
		}
		g.chat("Oh dear, you are dead!");
		g.tick(400, false, -1, 0, -1, -1, -1, -1, NOW);
		VolcanicLog.Game done = g.tick(403, false, -1, 0, -1, -1, -1, -1, NOW);
		assertEquals(VolcanicLog.End.ERUPTED, done.end);
		assertEquals(900, done.points);
	}

	@Test
	public void diedMidGame()
	{
		VolcanicGames g = new VolcanicGames();
		for (int t = 0; t < 100; t++)
		{
			g.tick(t, true, 200, 0, 1, 60, 500, 2, NOW);
		}
		g.chat("Oh dear, you are dead!");
		g.tick(100, false, -1, 0, -1, -1, -1, -1, NOW);
		assertEquals(VolcanicLog.End.DIED, g.tick(103, false, -1, 0, -1, -1, -1, -1, NOW).end);
	}

	@Test
	public void deathOutsideIsNotTheMines()
	{
		VolcanicGames g = new VolcanicGames();
		g.chat("Oh dear, you are dead!");
		for (int t = 0; t < 100; t++)
		{
			g.tick(t, true, 200, 0, 1, 60, 500, 2, NOW);
		}
		g.tick(100, false, -1, 0, -1, -1, -1, -1, NOW);
		assertEquals(VolcanicLog.End.LEFT, g.tick(103, false, -1, 0, -1, -1, -1, -1, NOW).end);
	}

	@Test
	public void loggedOut()
	{
		VolcanicGames g = inside(new VolcanicGames(), 0, 200, 500, 4_000);
		VolcanicLog.Game done = g.logout(200, 1_004_000, NOW);
		assertNotNull(done);
		assertEquals(VolcanicLog.End.LOGOUT, done.end);
		assertEquals(4_000, done.xp);
		assertNull(g.logout(201, 1_004_000, NOW));
	}

	@Test
	public void aBlinkOrAVisitWithNoGameIsNotKept()
	{
		VolcanicGames g = new VolcanicGames();
		g.tick(0, true, 0, 0, -1, -1, -1, 1, NOW);
		g.tick(1, true, 0, 0, -1, -1, -1, 1, NOW);
		g.tick(2, false, -1, 0, -1, -1, -1, -1, NOW);
		assertFalse(g.settling());
		// In and out before the game began, no points: not a game.
		for (int t = 10; t < 40; t++)
		{
			g.tick(t, true, 0, 0, 1, 100, 620, 1, NOW);
		}
		g.tick(40, false, -1, 0, -1, -1, -1, -1, NOW);
		assertFalse(g.settling());
		assertEquals(0, g.games());
	}

	@Test
	public void goingBackInSettlesTheLastGame()
	{
		VolcanicGames g = inside(new VolcanicGames(), 0, 300, 400, 3_000);
		g.tick(300, false, -1, 1_003_000, -1, -1, -1, -1, NOW);
		VolcanicLog.Game done = g.tick(301, true, 0, 1_003_000, 1, 100, 630, 1, NOW);
		assertNotNull(done);
		assertEquals(3_000, done.xp);
		assertTrue(g.open());
		assertEquals(0, g.xpGained(1_003_000));
	}
}
