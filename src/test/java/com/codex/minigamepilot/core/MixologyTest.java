package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Mixology.Herb;
import com.codex.minigamepilot.core.Mixology.Paste;
import com.codex.minigamepilot.core.Mixology.Potion;
import com.codex.minigamepilot.core.Mixology.Reward;
import com.codex.minigamepilot.core.Mixology.Station;
import org.junit.Test;

/** Mastering Mixology's tables against the OSRS wiki's own figures and examples. */
public class MixologyTest
{
	@Test
	public void potionsAsTheWikiTablesThem()
	{
		// Mastering Mixology, "Potion bases": code, level, ordered points and experience.
		assertEquals("MMA", Potion.MMA.code());
		assertEquals(63, Potion.MMA.level());
		assertEquals(20, Potion.MMA.resin(Paste.MOX));
		assertEquals(10, Potion.MMA.resin(Paste.AGA));
		assertEquals(0, Potion.MMA.resin(Paste.LYE));
		assertEquals(215, Potion.MMA.xp());
		assertEquals(60, Potion.MAL.totalResin());
		assertEquals(365, Potion.MAL.xp());
		assertEquals(81, Potion.MAL.level());
		assertEquals(78, Potion.ALL.level());
		assertEquals(340, Potion.ALL.xp());
		// Every potion takes three pulls, 10 paste each, and a level from 60.
		for (Potion p : Potion.values())
		{
			int pulls = 0;
			int resin = 0;
			for (Paste x : Paste.values())
			{
				pulls += p.uses(x);
				resin += p.resin(x);
				assertEquals(p.uses(x) * 10, p.paste(x));
			}
			assertEquals(3, pulls);
			assertEquals(p.single() ? 20 : p == Potion.MAL ? 60 : 30, resin);
			assertTrue(p.level() >= 60 && p.level() <= 81);
		}
		assertTrue(Potion.MMM.single() && Potion.AAA.single() && Potion.LLL.single());
		assertFalse(Potion.MAL.single());
	}

	@Test
	public void orderBalanceAsTheWikiSaysIt()
	{
		// "At level 60 or 78, mox, aga, and lye orders appear at a 1:1:1 ratio on average. At level 75, the ratio is about 39:35:31."
		for (int level : new int[]{60, 78})
		{
			double m = Mixology.pastePerOrder(Paste.MOX, level);
			assertEquals(m, Mixology.pastePerOrder(Paste.AGA, level), 1e-9);
			assertEquals(m, Mixology.pastePerOrder(Paste.LYE, level), 1e-9);
		}
		double m = Mixology.pastePerOrder(Paste.MOX, 75);
		double a = Mixology.pastePerOrder(Paste.AGA, 75);
		double l = Mixology.pastePerOrder(Paste.LYE, 75);
		assertEquals(39 / 35.0, m / a, 1e-9);
		assertEquals(35 / 31.0, a / l, 1e-9);
		// Below 60 nothing is ordered.
		assertEquals(0, Mixology.pastePerOrder(Paste.MOX, 59), 0);
		// At 60 only the three singles, 5:5:5: 10 of each paste an order.
		assertEquals(10, Mixology.pastePerOrder(Paste.LYE, 60), 1e-9);
	}

	@Test
	public void bonusForOrdersFilledAtOnce()
	{
		assertEquals(20, Mixology.withBonus(20, 1), 1e-9);
		assertEquals(60, Mixology.withBonus(50, 2), 1e-9);
		assertEquals(84, Mixology.withBonus(60, 3), 1e-9);
		// Per order at 60, with the 40% bonus: a third of 20 of each kind.
		assertEquals(20 / 3.0 * 1.4, Mixology.resinPerOrder(Paste.MOX, 60), 1e-9);
	}

	@Test
	public void varbitsAndNames()
	{
		assertEquals(Potion.MMM, Potion.ofVarbit(1));
		assertEquals(Potion.MAL, Potion.ofVarbit(10));
		assertNull(Potion.ofVarbit(0));
		assertNull(Potion.ofVarbit(11));
		assertEquals(Station.AGITATOR, Station.ofVarbit(1));
		assertEquals(Station.RETORT, Station.ofVarbit(2));
		assertEquals(Station.ALEMBIC, Station.ofVarbit(3));
		assertNull(Station.ofVarbit(4));
		assertEquals(Paste.LYE, Paste.ofSlot(3));
		assertNull(Paste.ofSlot(0));
		assertEquals(Potion.MML, Potion.ofTitle("Marley's moonlight"));
		assertEquals(Potion.MAL, Potion.ofTitle("<str>Mixalot</str>"));
		assertNull(Potion.ofTitle("Potion Orders"));
		assertEquals(Potion.ALA, Potion.ofItem(Potion.ALA.finished()));
		// The wiki's processing order: alembic, retort, agitator.
		assertTrue(Station.ALEMBIC.rank() < Station.RETORT.rank() && Station.RETORT.rank() < Station.AGITATOR.rank());
	}

	@Test
	public void herbsAndThePasteTheyMake()
	{
		// Mox paste, Aga paste and Lye paste pages: one herb or unfinished potion each.
		assertEquals(15, Herb.TARROMIN.yield());
		assertEquals(Paste.MOX, Herb.TARROMIN.paste());
		assertEquals(40, Herb.LANTADYME.yield());
		assertEquals(Paste.AGA, Herb.HUASCA.paste());
		assertEquals(30, Herb.AVANTOE.yield());
		assertEquals(Paste.LYE, Herb.SNAPDRAGON.paste());
		assertEquals(Herb.TORSTOL, Herb.ofItem(Herb.TORSTOL.unfinished()));
		assertNull(Herb.ofItem(-1));
	}

	@Test
	public void shopPrices()
	{
		assertEquals(8600, Reward.GOGGLES.cost(Paste.MOX));
		assertEquals(7000, Reward.GOGGLES.cost(Paste.AGA));
		assertEquals(9350, Reward.GOGGLES.cost(Paste.LYE));
		assertEquals(18600, Reward.CHUGGING_BARREL.cost(Paste.LYE));
		assertEquals(90, Reward.ALDARIUM.cost(Paste.LYE));
	}
}
