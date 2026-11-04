package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Random;
import org.junit.Test;

/** Your swords: kept, read back, and read as far as they can be when damaged. */
public class SwordsTest
{
	@Test
	public void roundTrip()
	{
		Swords s = new Swords();
		s.add(new Swords.Sword(1000, 300, 135, 13560));
		s.add(new Swords.Sword(2000, 250, 130, 12780));
		Swords back = Swords.decode(s.encode());
		assertEquals(2, back.count());
		assertEquals(135, back.all().get(0).quality);
		assertEquals(12780, back.last().xp);
	}

	@Test
	public void bestIsQualityThenTime()
	{
		Swords s = new Swords();
		Swords.Sword slow = new Swords.Sword(1, 400, 135, 1);
		Swords.Sword quick = new Swords.Sword(2, 300, 135, 1);
		Swords.Sword low = new Swords.Sword(3, 100, 120, 1);
		s.add(slow);
		s.add(low);
		s.add(quick);
		assertSame(quick, s.best());
		assertNull(new Swords().best());
	}

	@Test
	public void perHour()
	{
		Swords s = new Swords();
		assertEquals(-1, s.repPerHour(), 0);
		// Two swords of 300 ticks (3 minutes each): 20 an hour.
		s.add(new Swords.Sword(1, 300, 100, 1000));
		s.add(new Swords.Sword(2, 300, 140, 2000));
		assertEquals(240 * 10, s.repPerHour(), 0.001);
		assertEquals(3000 * 10, s.xpPerHour(), 0.001);
		// An untimed sword does not count toward the pace.
		s.add(new Swords.Sword(3, 0, 500, 9000));
		assertEquals(240 * 10, s.repPerHour(), 0.001);
		assertEquals(300, s.typicalTicks());
	}

	@Test
	public void keepsTheLastFifty()
	{
		Swords s = new Swords();
		for (int i = 0; i < 60; i++)
		{
			s.add(new Swords.Sword(i, 100, i, 0));
		}
		assertEquals(Swords.KEEP, s.count());
		assertEquals(10, s.all().get(0).quality);
	}

	@Test
	public void damagedSaves()
	{
		assertEquals(0, Swords.decode(null).count());
		assertEquals(0, Swords.decode("").count());
		assertEquals(0, Swords.decode("2;1,2,3,4").count());
		assertEquals(1, Swords.decode("1;x,2,3,4;5,6,7,8;-1,2,3,4;1,2;").count());
		Random r = new Random(7);
		String good = "1;1000,300,135,13560;2000,250,130,12780";
		for (int i = 0; i < 2000; i++)
		{
			StringBuilder b = new StringBuilder(good);
			for (int k = 0; k < 3; k++)
			{
				int at = r.nextInt(b.length());
				b.setCharAt(at, ";,-x9 ".charAt(r.nextInt(6)));
			}
			Swords.decode(b.toString());
		}
	}
}
