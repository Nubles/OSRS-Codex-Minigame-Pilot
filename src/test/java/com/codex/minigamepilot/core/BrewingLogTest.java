package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class BrewingLogTest
{
	@Test
	public void roundTripAndPace()
	{
		BrewingLog l = new BrewingLog();
		// Three twenty-minute games (2,000 ticks), each ending 23 minutes after the last.
		long t0 = 1_767_225_600_000L;
		l.add(new BrewingLog.Game(t0, 2000, Brewing.Team.BLUE, 12, 10, 200, 3000, BrewingLog.End.FINISHED));
		l.add(new BrewingLog.Game(t0 + 23 * 60_000L, 2000, Brewing.Team.RED, 9, 14, 140, 2000, BrewingLog.End.FINISHED));
		l.add(new BrewingLog.Game(t0 + 46 * 60_000L, 2000, null, -1, -1, 0, 0, BrewingLog.End.LEFT));
		BrewingLog back = BrewingLog.decode(l.encode());
		assertEquals(l.encode(), back.encode());
		assertEquals(3, back.count());
		assertNull(back.all().get(2).team);
		// 340 pieces over 6,000 ticks (an hour): 340 an hour.
		assertEquals(340, back.piecesPerHour(), 0.001);
		// Sitting: 140 + 0 over 46 minutes.
		assertEquals(140 * 60.0 / 46, back.piecesPerHourSitting(), 0.001);
		assertEquals(170, back.averagePieces());
		assertEquals(10.5, back.averageRum(), 0.001);
		assertEquals(1, back.won());
		assertEquals(2, back.scored());
		assertEquals(200, back.best().pieces);
		assertEquals("Lost", back.all().get(1).result());
	}

	@Test
	public void damagedSavesAreSkipped()
	{
		BrewingLog l = BrewingLog.decode("1;x,1,b,1,1,1,1,f;5,10,q,1,1,1,1,f;5,10,b,1,1,1,1,z;5,10,-,1,1,99999999,1,f");
		assertEquals(1, l.count());
		assertEquals(BrewingLog.PIECES_MAX, l.last().pieces);
		assertEquals(0, BrewingLog.decode("2;5,10,b,1,1,1,1,f").count());
		assertEquals(0, BrewingLog.decode(null).count());
		assertEquals(-1, BrewingLog.decode("1").piecesPerHour(), 0);
	}
}
