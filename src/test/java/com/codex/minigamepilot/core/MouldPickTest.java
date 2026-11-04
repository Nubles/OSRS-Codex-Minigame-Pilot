package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import org.junit.Test;

/** The mould pick against the OSRS wiki's own figures (Giants' Foundry, Giants' Foundry/Mould List). */
public class MouldPickTest
{
	private static Set<Mould> all()
	{
		return EnumSet.allOf(Mould.class);
	}

	@Test
	public void tableHasSixDefaultsAndFiveToBuyAPart()
	{
		for (Mould.Part part : Mould.Part.values())
		{
			int defaults = 0;
			int bought = 0;
			for (Mould m : Mould.values())
			{
				if (m.getPart() == part)
				{
					if (m.isDefault())
					{
						defaults++;
					}
					else
					{
						bought++;
					}
				}
			}
			assertEquals(6, defaults);
			assertEquals(5, bought);
		}
	}

	@Test
	public void buyingEveryMouldCosts5400()
	{
		// Giants' Foundry: "Buying all of them costs 5,400" reputation.
		int sum = 0;
		for (Mould m : Mould.values())
		{
			sum += m.getRep();
		}
		assertEquals(5400, sum);
	}

	@Test
	public void wikiFigures()
	{
		// The most a mould scores: Light Flat with the bought moulds, 69.
		MouldPick.Pick best = MouldPick.best(Mould.Style.LIGHT, Mould.Style.FLAT, all());
		assertEquals(69, best.getScore());
		assertEquals(Mould.CHOPPER_FORTE_PLUS, best.get(Mould.Part.FORTE));
		assertEquals(Mould.CHOPPA, best.get(Mould.Part.BLADE));
		assertEquals(Mould.THE_POINT, best.get(Mould.Part.TIP));
		// The most with the defaults: Narrow Heavy, 46.
		assertEquals(46, MouldPick.best(Mould.Style.NARROW, Mould.Style.HEAVY, null).getScore());
		// The worst commission: Broad Light, 24 with the defaults and 49 with every mould.
		assertEquals(24, MouldPick.best(Mould.Style.BROAD, Mould.Style.LIGHT, null).getScore());
		assertEquals(49, MouldPick.best(Mould.Style.BROAD, Mould.Style.LIGHT, all()).getScore());
		// No default commission beats 46, none with every mould beats 69.
		for (Mould.Style a : Mould.FIRST)
		{
			for (Mould.Style b : Mould.SECOND)
			{
				if (Mould.canCommission(a, b))
				{
					assertTrue(MouldPick.best(a, b, null).getScore() <= 46);
					assertTrue(MouldPick.best(a, b, all()).getScore() <= 69);
					assertTrue(MouldPick.best(a, b, null).getScore() >= 24);
				}
			}
		}
	}

	@Test
	public void fourteenCommissions()
	{
		int n = 0;
		for (Mould.Style a : Mould.FIRST)
		{
			for (Mould.Style b : Mould.SECOND)
			{
				n += Mould.canCommission(a, b) ? 1 : 0;
			}
		}
		// Four by four, less Heavy Heavy and Light Light.
		assertEquals(14, n);
		assertFalse(Mould.canCommission(Mould.Style.HEAVY, Mould.Style.HEAVY));
		assertFalse(Mould.canCommission(Mould.Style.FLAT, Mould.Style.BROAD));
	}

	@Test
	public void ownsOnlyWhatItKnows()
	{
		Set<Mould> owned = EnumSet.of(Mould.CHOPPA);
		MouldPick.Pick p = MouldPick.best(Mould.Style.LIGHT, Mould.Style.FLAT, owned);
		assertEquals(Mould.CHOPPA, p.get(Mould.Part.BLADE));
		assertEquals(Mould.SERPENT_RICASSO, p.get(Mould.Part.FORTE));
		assertEquals(46, p.getScore());
		assertEquals("Serpent Ricasso, Choppa!, Gladius Point", p.names());
	}

	@Test
	public void nextBuyIsAtYourLevelAndAddsSomething()
	{
		MouldPick.Buy at40 = MouldPick.next(40, null, null, null);
		assertNull(at40);
		MouldPick.Buy at99 = MouldPick.next(99, null, Mould.Style.LIGHT, Mould.Style.FLAT);
		assertNotNull(at99);
		assertTrue(at99.average > 0);
		// Nothing left to buy once every mould is owned.
		assertNull(MouldPick.next(99, all(), null, null));
		// Never a mould above the level.
		for (int level = 48; level <= 81; level++)
		{
			MouldPick.Buy b = MouldPick.next(level, null, null, null);
			assertTrue(b == null || b.mould.getLevel() <= level);
		}
	}

	@Test
	public void upgradeForThisCommission()
	{
		assertEquals(Mould.CHOPPA, MouldPick.upgrade(Mould.Part.BLADE, 80, null, Mould.Style.LIGHT, Mould.Style.FLAT));
		// Narrow Heavy: Fleur de Blade (Heavy 18, level 71) beats Gladius Edge's 14.
		assertEquals(Mould.FLEUR_DE_BLADE, MouldPick.upgrade(Mould.Part.BLADE, 79, null, Mould.Style.NARROW, Mould.Style.HEAVY));
		assertNull(MouldPick.upgrade(Mould.Part.BLADE, 47, null, Mould.Style.NARROW, Mould.Style.HEAVY));
		assertNull(MouldPick.upgrade(Mould.Part.BLADE, 99, EnumSet.of(Mould.FLEUR_DE_BLADE), Mould.Style.NARROW, Mould.Style.HEAVY));
	}

	@Test
	public void readsWords()
	{
		assertEquals(Mould.Style.NARROW, Mould.Style.ofVarbit(1));
		assertEquals(Mould.Style.SPIKED, Mould.Style.ofVarbit(6));
		assertNull(Mould.Style.ofVarbit(0));
		assertNull(Mould.Style.ofVarbit(7));
		assertEquals(Mould.Style.BROAD, Mould.Style.ofWord(" broad "));
		assertEquals(Mould.CHOPPA, Mould.ofTitle("<col=ffffff>Choppa!</col>"));
		assertNull(Mould.ofTitle("Bucket"));
	}
}
