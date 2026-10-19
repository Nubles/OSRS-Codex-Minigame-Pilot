package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import org.junit.Test;

public class BarPickTest
{
	private static IntUnaryOperator prices()
	{
		Map<Integer, Integer> p = new HashMap<>();
		p.put(Bar.COAL, 150);
		put(p, Bar.IRON, 100, 300);
		put(p, Bar.STEEL, 100, 600);
		put(p, Bar.MITHRIL, 150, 600);
		put(p, Bar.ADAMANTITE, 1000, 2100);
		put(p, Bar.RUNITE, 11000, 12500);
		put(p, Bar.GOLD, 150, 120);
		put(p, Bar.SILVER, 60, 150);
		return id -> p.getOrDefault(id, 0);
	}

	private static void put(Map<Integer, Integer> p, Bar bar, int ore, int made)
	{
		p.put(bar.getOre(), ore);
		p.put(bar.getBar(), made);
	}

	@Test
	public void coalTripsFollowTheCoalBag()
	{
		assertEquals(0, BarPick.coalTrips(0, 27, 27), 1e-9);
		assertEquals(0, BarPick.coalTrips(1, 27, 27), 1e-9);
		assertEquals(0.5, BarPick.coalTrips(2, 27, 27), 1e-9);
		assertEquals(1.5, BarPick.coalTrips(4, 27, 27), 1e-9);
		// No bag: steel needs a full coal trip for each ore trip.
		assertEquals(1, BarPick.coalTrips(1, 27, 0), 1e-9);
	}

	@Test
	public void ranksUnlockedBarsByCoinsATrip()
	{
		BarPick.Setup s = new BarPick.Setup();
		s.level = 72;
		s.coalBag = true;
		s.oreSlots = 26;
		s.tripSeconds = 60;
		List<BarPick.Row> rows = BarPick.rank(prices(), s);
		assertEquals(7, rows.size());
		// Steel: 600 - 100 - 150 = 350 a bar, no coal trips with the bag.
		BarPick.Row first = rows.get(0);
		assertEquals(Bar.STEEL, first.getBar());
		assertEquals(350, first.getPerBar());
		assertEquals(26 * 350, first.getPerTrip(), 1e-6);
		assertEquals(26 * 60, first.getBarsPerHour(), 1e-6);
		// Over 60 Smithing, only the coffer comes off.
		assertEquals(26 * 60 * 350 - BarPick.COFFER_PER_HOUR, first.getPerHour());
		assertEquals(26 * 60 * 17.5, first.getXpPerHour(), 1e-6);
		// Runite is out of reach at 72 and goes last whatever it pays.
		BarPick.Row last = rows.get(rows.size() - 1);
		assertEquals(Bar.RUNITE, last.getBar());
		assertFalse(last.isUnlocked());
		for (int i = 1; i < rows.size() - 1; i++)
		{
			assertTrue(rows.get(i - 1).getPerTrip() >= rows.get(i).getPerTrip());
		}
	}

	@Test
	public void theForemanAndGauntletsCountBelowSixtyAndOnGold()
	{
		BarPick.Setup s = new BarPick.Setup();
		s.level = 45;
		s.goldsmith = true;
		s.tripSeconds = 36;
		for (BarPick.Row r : BarPick.rank(prices(), s))
		{
			if (r.getBar() == Bar.GOLD)
			{
				assertEquals(27 * 100 * 56.2, r.getXpPerHour(), 1e-6);
				assertEquals(Math.round(2700.0 * -30) - BarPick.COFFER_PER_HOUR - BarPick.FOREMAN_PER_HOUR, r.getPerHour());
			}
		}
	}

	@Test
	public void missingPricesAndTripTimesAreLeftOut()
	{
		BarPick.Setup s = new BarPick.Setup();
		assertTrue(BarPick.rank(id -> 0, s).isEmpty());
		// Coal not priced: only the bars that burn none are ranked.
		IntUnaryOperator known = prices();
		List<BarPick.Row> rows = BarPick.rank(id -> id == Bar.COAL ? 0 : known.applyAsInt(id), s);
		assertEquals(3, rows.size());
		assertEquals(Long.MIN_VALUE, rows.get(0).getPerHour());
	}
}
