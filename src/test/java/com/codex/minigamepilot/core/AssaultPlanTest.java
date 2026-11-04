package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Assault.Role;
import java.util.EnumMap;
import java.util.Map;
import org.junit.Test;

public class AssaultPlanTest
{
	private static Map<Role, Integer> each(int a, int c, int d, int h)
	{
		Map<Role, Integer> m = new EnumMap<>(Role.class);
		m.put(Role.ATTACKER, a);
		m.put(Role.COLLECTOR, c);
		m.put(Role.DEFENDER, d);
		m.put(Role.HEALER, h);
		return m;
	}

	@Test
	public void theRoleFurthestBehindFirst()
	{
		// The torso: 375 each. No games of yours yet: the wiki's 80 and 5 for the queen's wave alone.
		AssaultPlan.Result r = AssaultPlan.plan(each(375, 375, 375, 375), each(375, 0, 300, 375), new EnumMap<>(Role.class), 0);
		assertEquals(Role.COLLECTOR, r.next);
		assertEquals(375, r.shortOf(Role.COLLECTOR));
		assertEquals(75, r.shortOf(Role.DEFENDER));
		assertTrue(r.bound);
		assertEquals(-1, r.hours, 0);
		// Collector: 5 games (5 x 80 = 400) give defender 25; it needs one more game.
		assertEquals(5, r.games(Role.COLLECTOR));
		assertEquals(1, r.games(Role.DEFENDER));
		assertEquals(6, r.total);
	}

	@Test
	public void sidePointsCanCoverASmallShortfall()
	{
		// 10 short in healer: two collector games (5 each at the queen) cover it.
		AssaultPlan.Result r = AssaultPlan.plan(each(375, 375, 375, 375), each(375, 215, 375, 365), new EnumMap<>(Role.class), 0);
		assertEquals(Role.COLLECTOR, r.next);
		assertEquals(2, r.games(Role.COLLECTOR));
		assertEquals(0, r.games(Role.HEALER));
		assertEquals(2, r.total);
	}

	@Test
	public void yourOwnRatesAndTime()
	{
		Map<Role, AssaultLog.Rate> rates = new EnumMap<>(Role.class);
		// Your collector games give 200 a game and 10 to the others, in 25 minutes (2,500 ticks).
		rates.put(Role.COLLECTOR, new AssaultLog.Rate(3, 200, 10, 2500));
		AssaultPlan.Result r = AssaultPlan.plan(each(0, 400, 0, 0), each(0, 0, 0, 0), rates, 2500);
		assertEquals(Role.COLLECTOR, r.next);
		assertEquals(2, r.total);
		assertFalse(r.bound);
		assertEquals(2 * 2500 * 0.6 / 3600, r.hours, 1e-9);
	}

	@Test
	public void nothingShort()
	{
		AssaultPlan.Result r = AssaultPlan.plan(each(100, 100, 100, 100), each(100, 200, 300, 400), null, 0);
		assertNull(r.next);
		assertEquals(0, r.total);
		assertEquals(0, r.hours, 0);
	}

	@Test
	public void levelsByRole()
	{
		// Level goal: attacker at 5 needs nothing; healer at 1 needs 1,400.
		Map<Role, Integer> need = each(Assault.toMastered(5), Assault.toMastered(4), Assault.toMastered(3), Assault.toMastered(1));
		AssaultPlan.Result r = AssaultPlan.plan(need, each(0, 0, 0, 0), new EnumMap<>(Role.class), 0);
		assertEquals(Role.HEALER, r.next);
		assertEquals(0, r.shortOf(Role.ATTACKER));
		assertEquals(500, r.shortOf(Role.COLLECTOR));
	}
}
