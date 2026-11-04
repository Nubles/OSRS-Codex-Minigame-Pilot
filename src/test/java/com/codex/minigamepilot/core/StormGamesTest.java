package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Permits and your games. OSRS wiki, Tempoross: "One reward permit is given for the first 2,000
 * points scored, with an additional permit awarded for every 700 points beyond 2,000"; points
 * are capped at 32,000.
 */
public class StormGamesTest
{
	@Test
	public void permitsFromPoints()
	{
		assertEquals(0, StormGames.sure(0));
		assertEquals(0, StormGames.sure(1999));
		assertEquals(1, StormGames.sure(2000));
		assertEquals(1, StormGames.sure(2699));
		assertEquals(2, StormGames.sure(2700));
		assertEquals(4, StormGames.sure(4350));
		assertEquals(1 + 30000 / 700, StormGames.sure(32000));
		assertEquals(StormGames.sure(32000), StormGames.sure(99999));
		assertEquals(0, StormGames.sure(-5));
	}

	@Test
	public void chanceAndPointsToTheNext()
	{
		assertEquals(0, StormGames.chance(1500), 1e-9);
		assertEquals(0.5, StormGames.chance(2350), 1e-9);
		assertEquals(2000, StormGames.toNext(0));
		assertEquals(700, StormGames.toNext(2000));
		assertEquals(350, StormGames.toNext(2350));
	}

	@Test
	public void keepsGamesAndTheirPace()
	{
		StormGames g = new StormGames();
		assertEquals(-1, g.permitsPerHour(), 1e-9);
		assertNull(g.best());
		// 1,000 ticks is 10 minutes.
		g.add(new StormGames.Game(1, 1000, 4350, 4, true));
		g.add(new StormGames.Game(2, 1000, 0, 0, false));
		assertEquals(12.0, g.permitsPerHour(), 1e-9);
		assertEquals(4350, g.best().points);
		assertEquals(2175, g.averagePoints());
		assertEquals(1000, g.typicalTicks());
		g.correctLast(1);
		assertEquals(1, g.last().permits);
	}

	@Test
	public void keepsOnlyTheLastThirty()
	{
		StormGames g = new StormGames();
		for (int i = 0; i < 40; i++)
		{
			g.add(new StormGames.Game(i, 900, 3000 + i, 2, true));
		}
		assertEquals(StormGames.KEEP, g.count());
		assertEquals(39, g.last().endedAt);
		assertEquals(g.encode(), StormGames.decode(g.encode()).encode());
	}

	@Test
	public void survivesADamagedSave()
	{
		StormGames g = new StormGames();
		g.add(new StormGames.Game(1767225600000L, 950, 4350, 5, true));
		g.add(new StormGames.Game(1767226600000L, 300, 0, 0, false));
		String good = g.encode();
		for (String s : SavedKeysFuzzTest.damaged(good))
		{
			StormGames d = StormGames.decode(s);
			for (StormGames.Game game : d.all())
			{
				assertTrue(s, game.points >= 0 && game.points <= StormGames.MAX_POINTS);
				assertTrue(s, game.permits >= 0 && game.ticks >= 0);
			}
			assertFalse(s, Double.isNaN(d.permitsPerHour()) || Double.isInfinite(d.permitsPerHour()));
			StormGames.decode(d.encode());
		}
		assertEquals(good, StormGames.decode(good).encode());
	}
}
