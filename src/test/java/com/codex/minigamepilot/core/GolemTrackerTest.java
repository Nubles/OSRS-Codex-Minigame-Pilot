package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Test;

public class GolemTrackerTest
{
	private static GolemTracker.Reading at(int crafting, int cores, GolemCrafting.Fur fur, int furs)
	{
		GolemTracker.Reading r = new GolemTracker.Reading();
		r.atStations = true;
		r.crafting = crafting;
		r.cores = cores;
		if (fur != null)
		{
			r.furs[fur.ordinal()] = furs;
		}
		return r;
	}

	@Test
	public void golemsWithXpFurAndGaps()
	{
		GolemTracker t = new GolemTracker();
		assertTrue(t.tick(1, at(1_000_000, 5, GolemCrafting.Fur.DARK_KEBBIT, 5), 0).isEmpty());
		// The first golem: the core and a fur go, the experience comes and the gift line.
		t.tick(10, at(1_002_700, 4, GolemCrafting.Fur.DARK_KEBBIT, 4), 0);
		t.gift(GolemCrafting.Gift.SAPPHIRE, 10);
		List<GolemLog.Golem> one = t.tick(11, at(1_002_700, 4, GolemCrafting.Fur.DARK_KEBBIT, 4), 1);
		assertEquals(1, one.size());
		assertFalse(one.get(0).timed());
		assertEquals(0, one.get(0).xp);
		assertEquals(GolemCrafting.Fur.DARK_KEBBIT, one.get(0).fur);
		assertEquals(GolemCrafting.Gift.SAPPHIRE, one.get(0).gem);
		// The second, 100 ticks on, with the experience its sides and a core gave on the way.
		t.tick(50, at(1_003_500, 4, GolemCrafting.Fur.DARK_KEBBIT, 4), 0);
		t.tick(110, at(1_005_400, 3, GolemCrafting.Fur.DARK_KEBBIT, 3), 0);
		t.gift(GolemCrafting.Gift.RUBY, 110);
		t.gift(GolemCrafting.Gift.CHISEL, 110);
		List<GolemLog.Golem> two = t.tick(111, at(1_005_400, 3, GolemCrafting.Fur.DARK_KEBBIT, 3), 2);
		assertEquals(1, two.size());
		assertEquals(100, two.get(0).ticks);
		assertEquals(2_700, two.get(0).xp);
		assertTrue(two.get(0).chisel);
		assertEquals(GolemCrafting.Gift.RUBY, two.get(0).gem);
		assertEquals(2, t.golems());
		assertEquals(1, t.chisels());
		assertEquals(10, t.sinceLast(120));
	}

	@Test
	public void furPouchHidesTheFur()
	{
		GolemTracker t = new GolemTracker();
		t.tick(1, at(1_000_000, 2, null, 0), 0);
		t.tick(5, at(1_002_700, 1, null, 0), 0);
		t.gift(GolemCrafting.Gift.EMERALD, 5);
		List<GolemLog.Golem> g = t.tick(6, at(1_002_700, 1, null, 0), 0);
		assertNull(g.get(0).fur);
	}

	@Test
	public void bankingIsNoFur()
	{
		GolemTracker t = new GolemTracker();
		t.tick(1, at(1_000_000, 0, GolemCrafting.Fur.FOX, 10), 0);
		// Fur deposited, no core used.
		t.tick(2, at(1_000_000, 0, GolemCrafting.Fur.FOX, 9), 0);
		t.gift(GolemCrafting.Gift.SAPPHIRE, 3);
		assertNull(t.tick(4, at(1_000_000, 0, GolemCrafting.Fur.FOX, 9), 0).get(0).fur);
	}

	@Test
	public void twoGemsAreTwoGolems()
	{
		GolemTracker t = new GolemTracker();
		t.tick(1, at(1_000_000, 0, null, 0), 0);
		t.gift(GolemCrafting.Gift.SAPPHIRE, 3);
		t.gift(GolemCrafting.Gift.EMERALD, 3);
		assertEquals(2, t.tick(4, at(1_000_000, 0, null, 0), 0).size());
	}

	@Test
	public void aLoneChiselIsAGolem()
	{
		GolemTracker t = new GolemTracker();
		t.tick(1, at(1_000_000, 0, null, 0), 0);
		t.gift(GolemCrafting.Gift.CHISEL, 3);
		List<GolemLog.Golem> g = t.tick(4, at(1_000_000, 0, null, 0), 0);
		assertEquals(1, g.size());
		assertNull(g.get(0).gem);
		assertTrue(g.get(0).chisel);
	}

	@Test
	public void longGapStartsASession()
	{
		GolemTracker t = new GolemTracker();
		t.tick(1, at(1_000_000, 0, null, 0), 0);
		t.gift(GolemCrafting.Gift.SAPPHIRE, 2);
		t.tick(3, at(1_000_000, 0, null, 0), 0);
		t.tick(1500, at(1_050_000, 0, null, 0), 0);
		t.gift(GolemCrafting.Gift.SAPPHIRE, 1500);
		GolemLog.Golem g = t.tick(1501, at(1_050_000, 0, null, 0), 0).get(0);
		assertFalse(g.timed());
		assertEquals(0, g.xp);
	}

	@Test
	public void awayExperienceIsNotCounted()
	{
		GolemTracker t = new GolemTracker();
		t.tick(1, at(1_000_000, 0, null, 0), 0);
		t.gift(GolemCrafting.Gift.SAPPHIRE, 2);
		t.tick(3, at(1_000_000, 0, null, 0), 0);
		GolemTracker.Reading away = at(1_010_000, 0, null, 0);
		away.atStations = false;
		t.tick(20, away, 0);
		t.tick(40, at(1_012_700, 0, null, 0), 0);
		t.gift(GolemCrafting.Gift.SAPPHIRE, 40);
		assertEquals(2_700, t.tick(41, at(1_012_700, 0, null, 0), 0).get(0).xp);
	}

	@Test
	public void logoutKeepsAGiftAlreadyShown()
	{
		GolemTracker t = new GolemTracker();
		t.tick(1, at(1_000_000, 0, null, 0), 0);
		t.gift(GolemCrafting.Gift.DIAMOND, 1);
		assertEquals(1, t.logout(0).size());
		assertEquals(-1, t.sinceLast(5));
		assertTrue(t.logout(0).isEmpty());
	}
}
