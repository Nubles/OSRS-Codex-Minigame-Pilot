package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Assault.Role;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class AssaultLogTest
{
	/** A full game in one role: ten won waves of {@code waveTicks}, the points given. */
	static AssaultLog.Game full(Role role, int waveTicks, int own, int side, long at)
	{
		List<AssaultLog.Wave> waves = new ArrayList<>();
		for (int n = 1; n <= 10; n++)
		{
			waves.add(new AssaultLog.Wave(n, role, waveTicks, true));
		}
		int[] p = new int[4];
		for (Role r : Role.values())
		{
			p[r.ordinal()] = r == role ? own : side;
		}
		return new AssaultLog.Game(at, waveTicks * 10 + 300, waves, p);
	}

	@Test
	public void roundTrip()
	{
		AssaultLog g = new AssaultLog();
		g.add(full(Role.COLLECTOR, 100, 160, 5, 1));
		List<AssaultLog.Wave> part = new ArrayList<>();
		part.add(new AssaultLog.Wave(4, Role.ATTACKER, 80, true));
		part.add(new AssaultLog.Wave(5, null, 0, false));
		g.add(new AssaultLog.Game(2, 200, part, new int[]{20, 0, 0, 0}));
		AssaultLog back = AssaultLog.decode(g.encode());
		assertEquals(g.encode(), back.encode());
		assertEquals(2, back.count());
		AssaultLog.Game last = back.last();
		assertEquals(4, last.first());
		assertEquals(4, last.reached());
		assertNull(last.waves.get(1).role);
		assertEquals(Role.ATTACKER, last.role());
		assertEquals(1, back.queens());
	}

	@Test
	public void ratesFromFullGamesOnly()
	{
		AssaultLog g = new AssaultLog();
		g.add(full(Role.COLLECTOR, 100, 160, 5, 1));
		g.add(full(Role.COLLECTOR, 120, 180, 5, 2));
		List<AssaultLog.Wave> part = new ArrayList<>();
		part.add(new AssaultLog.Wave(1, Role.HEALER, 80, true));
		g.add(new AssaultLog.Game(3, 80, part, new int[]{0, 0, 0, 15}));
		Map<Role, AssaultLog.Rate> rates = g.rates();
		assertEquals(1, rates.size());
		AssaultLog.Rate c = rates.get(Role.COLLECTOR);
		assertEquals(2, c.games);
		assertEquals(170, c.own, 1e-9);
		assertEquals(5, c.side, 1e-9);
		assertEquals(1400, c.ticks);
		assertEquals(1300, g.quickest().ticks);
		assertEquals(100, g.quickestWave(3));
		assertEquals(1400, g.typicalGame());
		assertTrue(g.pointsPerHour() > 0);
	}

	@Test
	public void keepsTheLastHundred()
	{
		AssaultLog g = new AssaultLog();
		for (int i = 0; i < 130; i++)
		{
			g.add(full(Role.DEFENDER, 100, 150, 5, i));
		}
		assertEquals(AssaultLog.KEEP, g.count());
		assertEquals(129, g.last().endedAt);
	}

	@Test
	public void damagedGamesAreLeftOut()
	{
		assertEquals(0, AssaultLog.decode("1;5,100,1,2,3,4,1A9d/zz").count());
		assertEquals(0, AssaultLog.decode("2;5,100,1,2,3,4,1A9d").count());
		assertEquals(1, AssaultLog.decode("1;5,100,1,2,3,4,1A9d;bad").count());
	}
}
