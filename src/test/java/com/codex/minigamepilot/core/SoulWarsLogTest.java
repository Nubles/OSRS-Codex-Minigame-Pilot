package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SoulWarsLogTest
{
	private static SoulWarsLog.Game game(long at, int ticks, SoulWarsLog.Result r, int zeal)
	{
		SoulWarsLog.Part p = new SoulWarsLog.Part();
		p.fragments = 20;
		p.damage = 150;
		return new SoulWarsLog.Game(at, ticks, SoulWars.Team.BLUE, r, zeal, r == SoulWarsLog.Result.WON ? 3 : 1, r == SoulWarsLog.Result.WON ? 1 : 2, p, SoulWarsLog.End.FINISHED);
	}

	@Test
	public void roundTrip()
	{
		SoulWarsLog l = new SoulWarsLog();
		l.add(game(1_000_000L, 1500, SoulWarsLog.Result.WON, 60));
		l.add(new SoulWarsLog.Game(2_000_000L, 90, null, SoulWarsLog.Result.UNKNOWN, 0, -1, -1, null, SoulWarsLog.End.LEFT));
		SoulWarsLog back = SoulWarsLog.decode(l.encode());
		assertEquals(l.encode(), back.encode());
		assertEquals(2, back.count());
		assertEquals("3-1", back.all().get(0).score());
		assertEquals("", back.all().get(1).score());
		assertEquals(150, back.all().get(0).part.damage);
	}

	@Test
	public void damagedSavesAreReadAsFarAsTheyGo()
	{
		SoulWarsLog l = SoulWarsLog.decode("1;5,1500,b,w,60,3,1,-1,-1,-1,-1,-1,f;x;6,1,q,w,1,1,1,1,1,1,1,1,f;7,99999,r,z,1,1,1,1,1,1,1,1,f;8,10,r,l,999,1,2,1,1,1,1,1,f");
		assertEquals(2, l.count());
		// Zeal is held to what a game can give.
		assertEquals(60, l.last().zeal);
		assertEquals(0, SoulWarsLog.decode("2;5,1,b,w,1,1,1,1,1,1,1,1,f").count());
		assertEquals(0, SoulWarsLog.decode(null).count());
	}

	@Test
	public void paceAndShare()
	{
		SoulWarsLog l = new SoulWarsLog();
		assertEquals(-1, l.zealPerHour(), 1e-9);
		assertEquals(-1, l.share(), 1e-9);
		// Three games, 15 minutes each, 20 minutes apart.
		long t = 1_767_225_600_000L;
		l.add(game(t, 1500, SoulWarsLog.Result.WON, 54));
		l.add(game(t + 1_200_000, 1500, SoulWarsLog.Result.LOST, 36));
		l.add(game(t + 2_400_000, 1500, SoulWarsLog.Result.DRAWN, 45));
		// 135 zeal in 45 minutes of game.
		assertEquals(180, l.zealPerHour(), 1e-6);
		// 81 zeal in 40 minutes, with the waits.
		assertEquals(121.5, l.zealPerHourSitting(), 1e-6);
		assertEquals(121.5, l.pace(), 1e-6);
		assertEquals(45, l.averageZeal(), 1e-9);
		assertEquals(54, l.averageZeal(SoulWarsLog.Result.WON), 1e-9);
		// 135 of 60 + 40 + 50.
		assertEquals(0.9, l.share(), 1e-9);
		assertEquals(3, l.decided());
		assertEquals(54, l.best().zeal);
		assertTrue(l.totalZeal() == 135);
	}
}
