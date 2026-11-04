package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GolemLogTest
{
	private static GolemLog.Golem g(int ticks, int xp, GolemCrafting.Gift gem, boolean chisel, GolemCrafting.Fur fur)
	{
		return new GolemLog.Golem(1_767_000_000_000L, ticks, xp, gem, chisel, fur);
	}

	@Test
	public void roundTrip()
	{
		GolemLog l = new GolemLog();
		l.add(g(0, 0, GolemCrafting.Gift.SAPPHIRE, false, GolemCrafting.Fur.DARK_KEBBIT));
		l.add(g(100, 2710, GolemCrafting.Gift.RUBY, true, null));
		l.add(g(0, 0, null, true, GolemCrafting.Fur.POLAR_KEBBIT));
		GolemLog back = GolemLog.decode(l.encode());
		assertEquals(l.encode(), back.encode());
		assertEquals(3, back.count());
		assertEquals(GolemCrafting.Fur.POLAR_KEBBIT, back.lastFur());
		assertTrue(back.hadChisel());
		assertEquals(0, back.sinceChisel());
	}

	@Test
	public void damagedIsLeftOut()
	{
		GolemLog l = GolemLog.decode("1;5,100,2700,s,0,d;x,1,1,s,0,d;5,1,1,c,0,d;5,1,1,-,0,-;5,1,1,q,0,d;5,1,1,s,2,d;5,99999,1,e,0,-");
		assertEquals(2, l.count());
		// A gap beyond ten minutes is untimed.
		assertFalse(l.all().get(1).timed());
		assertEquals(0, GolemLog.decode("2;5,100,2700,s,0,d").count());
		assertEquals(0, GolemLog.decode(null).count());
		assertNull(GolemLog.decode("").last());
	}

	@Test
	public void paceAndSittings()
	{
		GolemLog l = new GolemLog();
		// Session one: 6 golems, 5 timed at 100 ticks (a minute) and 2,700 xp.
		l.add(g(0, 0, GolemCrafting.Gift.SAPPHIRE, false, null));
		for (int i = 0; i < 5; i++)
		{
			l.add(g(100, 2700, GolemCrafting.Gift.SAPPHIRE, false, null));
		}
		// Session two: 3 golems at 50 ticks.
		l.add(g(0, 0, GolemCrafting.Gift.EMERALD, false, null));
		l.add(g(50, 2700, GolemCrafting.Gift.DIAMOND, false, null));
		l.add(g(50, 0, GolemCrafting.Gift.SAPPHIRE, true, null));
		GolemLog.Pace p = l.pace();
		assertEquals(7, p.golems);
		assertEquals(600, p.ticks);
		assertEquals(6, p.withXp);
		assertEquals(2700, p.xpPerGolem(), 1e-9);
		assertEquals(7 / (600 * 0.6 / 3600), p.golemsPerHour(), 1e-9);
		assertEquals(6 * 2700 / (550 * 0.6 / 3600), p.xpPerHour(), 1e-9);
		assertEquals(2, l.sittings().size());
		assertEquals(6, l.sittings().get(0).golems);
		assertEquals(60, l.sittings().get(0).golemsPerHour(), 1e-9);
		// Session two has only three golems: not a record.
		assertEquals(60, l.bestSitting().golemsPerHour(), 1e-9);
		assertEquals(6, l.longestSitting());
		assertEquals(50, l.quickest());
		int[] gifts = l.gifts();
		assertEquals(7, gifts[GolemCrafting.Gift.SAPPHIRE.ordinal()]);
		assertEquals(1, gifts[GolemCrafting.Gift.CHISEL.ordinal()]);
		assertEquals(0, l.sinceChisel());
		assertEquals(6 * 2700, l.xp());
	}

	@Test
	public void keepsTheLatest()
	{
		GolemLog l = new GolemLog();
		for (int i = 0; i < GolemLog.KEEP + 5; i++)
		{
			l.add(g(100, 2700, GolemCrafting.Gift.SAPPHIRE, false, null));
		}
		assertEquals(GolemLog.KEEP, l.count());
		assertEquals(GolemLog.RECENT, l.pace().golems);
	}
}
