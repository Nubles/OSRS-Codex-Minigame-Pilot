package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GolemEtaTest
{
	private static GolemLog.Pace pace(int golems, long ticks, long xp)
	{
		GolemLog.Pace p = new GolemLog.Pace();
		p.golems = golems;
		p.ticks = ticks;
		p.withXp = golems;
		p.xp = xp;
		p.xpTicks = ticks;
		return p;
	}

	@Test
	public void levelByFurBeforeYourOwnFigures()
	{
		// 70 Crafting is 737,627 xp; from 700,000, 37,627 at Dark kebbit's 2,700 a golem is 14 golems.
		GolemEta.Answer a = GolemEta.of(GolemEta.Kind.CRAFTING_LEVEL, 70, 700_000, 0, false, GolemCrafting.Fur.DARK_KEBBIT, 0, null);
		assertEquals(737_627, a.need);
		assertEquals(14, a.golems);
		assertEquals(70, a.sunstone);
		assertEquals(-1, a.hours, 0);
		// No fur known: the base 1,560 alone.
		assertEquals(25, GolemEta.of(GolemEta.Kind.CRAFTING_LEVEL, 70, 700_000, 0, false, null, 0, null).golems);
	}

	@Test
	public void levelAtYourPace()
	{
		// 100 golems an hour (100 in 6,000 ticks) at 2,000 xp each.
		GolemEta.Answer a = GolemEta.of(GolemEta.Kind.CRAFTING_LEVEL, 70, 700_000, 0, false, GolemCrafting.Fur.DARK_KEBBIT, 30, pace(100, 6_000, 200_000));
		assertEquals(2_000, a.xpPerGolem, 1e-9);
		assertEquals(19, a.golems);
		assertEquals(65, a.sunstone);
		assertEquals(0.19, a.hours, 1e-9);
	}

	@Test
	public void golemsAndChisel()
	{
		GolemEta.Answer g = GolemEta.of(GolemEta.Kind.GOLEMS, 100, 0, 40, false, null, 0, null);
		assertEquals(60, g.golems);
		GolemEta.Answer c = GolemEta.of(GolemEta.Kind.JEWELLERS_CHISEL, 0, 0, 100, false, null, 0, pace(60, 6_000, 0));
		assertEquals(200, c.golems);
		assertEquals(GolemCrafting.chiselChance(100), c.chance, 1e-12);
		assertEquals(200 / 60.0, c.hours, 1e-9);
		assertTrue(GolemEta.of(GolemEta.Kind.JEWELLERS_CHISEL, 0, 0, 5, true, null, 0, null).done());
		assertTrue(GolemEta.of(GolemEta.Kind.CRAFTING_LEVEL, 60, 300_000, 0, false, null, 0, null).done());
	}
}
