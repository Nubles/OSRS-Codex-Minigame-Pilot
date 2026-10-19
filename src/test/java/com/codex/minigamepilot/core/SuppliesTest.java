package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SuppliesTest
{
	@Test
	public void runiteRunsOnCoalAsMuchAsOre()
	{
		// 540 runite ore and 1,500 coal: four coal a bar makes only 375 bars.
		Supplies.Furnace f = Supplies.furnace(Bar.RUNITE, 540, 1500, 27, 27, 0);
		assertEquals(375, f.getBars());
		assertTrue(f.isCoalShort());
		assertEquals(540 * 4 - 1500, f.getCoalToUseAllOre());
		// 375 bars are 13.9 loads; each load needs one and a half coal trips more.
		assertEquals(375 / 27.0 * 2.5, f.getTrips(), 0.001);
		assertEquals("375 runite bars, 35 trips", f.headline());
		assertTrue(f.limit().startsWith("Coal runs out first: 660 more"));
	}

	@Test
	public void timeComesFromYourTrip()
	{
		// Steel with a coal bag: no coal trips, 27 a trip at 60 seconds.
		Supplies.Furnace f = Supplies.furnace(Bar.STEEL, 270, 5000, 27, 27, 60);
		assertEquals(270, f.getBars());
		assertFalse(f.isCoalShort());
		assertEquals(600, f.getSeconds(), 0.001);
		assertEquals("270 steel bars, 10 trips, about 10m", f.headline());
		assertEquals("The ore runs out first", f.limit());
	}

	@Test
	public void goldNeedsNoCoalAndNothingMakesNoBar()
	{
		Supplies.Furnace gold = Supplies.furnace(Bar.GOLD, 28, 0, 28, 0, 0);
		assertEquals(28, gold.getBars());
		assertNull(gold.limit());
		Supplies.Furnace none = Supplies.furnace(Bar.MITHRIL, 0, 900, 27, 27, 50);
		assertEquals(0, none.getBars());
		assertEquals("Not enough mithril ore and coal for a bar", none.headline());
		assertNull(none.limit());
	}

	@Test
	public void planksCountInContractsOnceOneIsLearned()
	{
		Supplies.Homes unknown = Supplies.homes(Contract.Tier.ADEPT, 212, 0, null);
		assertEquals(-1, unknown.getContracts());
		assertEquals("212 teak planks", unknown.headline());
		assertEquals("No teak planks in the bank", Supplies.homes(Contract.Tier.ADEPT, 0, 0, null).headline());

		ContractBook book = new ContractBook();
		book.record(Contract.Tier.ADEPT, 1000, 10, 1, 300);
		book.record(Contract.Tier.ADEPT, 1000, 12, 1, 300);
		ContractBook.Tally t = book.tally(Contract.Tier.ADEPT);
		Supplies.Homes h = Supplies.homes(Contract.Tier.ADEPT, 212, 50, t);
		assertEquals(19, h.getContracts());
		assertFalse(h.isSteelShort());
		assertEquals("Teak planks for about 19 contracts, 1h 35m", h.headline());
		// Five steel bars cover five contracts, before the planks run out.
		Supplies.Homes steel = Supplies.homes(Contract.Tier.ADEPT, 212, 5, t);
		assertEquals(5, steel.getContracts());
		assertTrue(steel.isSteelShort());
		assertEquals("Not enough teak planks for a contract", Supplies.homes(Contract.Tier.ADEPT, 4, 9, t).headline());
	}
}
