package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FurnaceCycleTest
{
	private static FurnaceCycle.Snapshot snap(Bar bar)
	{
		FurnaceCycle.Snapshot s = new FurnaceCycle.Snapshot();
		s.bar = bar;
		s.coffer = 50_000;
		s.free = 26;
		s.fixed = 2;
		s.iceGlovesWorn = true;
		return s;
	}

	@Test
	public void steelWithACoalBagNeverNeedsACoalTrip()
	{
		FurnaceCycle.Snapshot s = snap(Bar.STEEL);
		s.coalBag = true;
		s.furnaceCoal = 0;
		assertFalse(FurnaceCycle.coalRunNext(s));
	}

	@Test
	public void runiteAlternatesCoalTrips()
	{
		FurnaceCycle.Snapshot s = snap(Bar.RUNITE);
		s.coalBag = true;
		s.furnaceCoal = 0;
		assertTrue(FurnaceCycle.coalRunNext(s));
		// 26 ore burns 104 coal; with a full bag of 27, 77 must already be waiting.
		s.furnaceCoal = 77;
		assertFalse(FurnaceCycle.coalRunNext(s));
		s.furnaceCoal = 76;
		assertTrue(FurnaceCycle.coalRunNext(s));
	}

	/**
	 * Plays runite out trip by trip as the pilot calls them: coal trips bring a pack and a bag,
	 * ore trips burn a load less what the bag brings. About one and a half coal trips go to each
	 * ore trip, the same as the "what pays" ranking counts, not three.
	 */
	@Test
	public void runiteTakesAboutOneAndAHalfCoalTripsAnOreTrip()
	{
		FurnaceCycle.Snapshot s = snap(Bar.RUNITE);
		s.coalBag = true;
		s.bagCapacity = 27;
		s.furnaceCoal = 0;
		int coalTrips = 0;
		int oreTrips = 0;
		while (oreTrips < 1000)
		{
			if (FurnaceCycle.coalRunNext(s))
			{
				s.furnaceCoal += FurnaceCycle.coalTrip(s);
				coalTrips++;
			}
			else
			{
				s.furnaceCoal -= Bar.RUNITE.getCoal() * FurnaceCycle.load(s) - s.bagCapacity;
				oreTrips++;
			}
		}
		double perOre = coalTrips / (double) oreTrips;
		assertEquals(1.45, perOre, 0.01);
		assertEquals(BarPick.coalTrips(Bar.RUNITE.getCoal(), FurnaceCycle.load(s), 27), perOre, 0.01);
	}

	@Test
	public void atTheBankFillsTheBagThenWithdraws()
	{
		FurnaceCycle c = new FurnaceCycle();
		FurnaceCycle.Snapshot s = snap(Bar.MITHRIL);
		s.bankOpen = true;
		s.coalBag = true;
		s.bagCoal = 0;
		s.furnaceCoal = 100;
		Step step = c.next(s);
		assertEquals("Fill", step.getVerb());
		s.bagCoal = 27;
		step = c.next(s);
		assertEquals("Withdraw", step.getVerb());
		assertTrue(step.hasItem(Bar.MITHRIL.getOre()));
		s.ore = 26;
		s.free = 0;
		assertEquals(Step.Target.BELT, c.next(s).getTarget());
	}

	@Test
	public void depositsBarsBeforeAnythingElseAtTheBank()
	{
		FurnaceCycle c = new FurnaceCycle();
		FurnaceCycle.Snapshot s = snap(Bar.STEEL);
		s.bankOpen = true;
		s.bars = 26;
		s.free = 0;
		Step step = c.next(s);
		assertEquals("Deposit", step.getVerb());
		assertTrue(step.hasItem(Bar.STEEL.getBar()));
	}

	@Test
	public void loadsThenEmptiesTheBagThenWaitsForBars()
	{
		FurnaceCycle c = new FurnaceCycle();
		FurnaceCycle.Snapshot s = snap(Bar.STEEL);
		s.coalBag = true;
		s.ore = 26;
		s.free = 0;
		s.bagCoal = 27;
		assertEquals(Step.Target.BELT, c.next(s).getTarget());
		s.ore = 0;
		s.free = 26;
		Step empty = c.next(s);
		assertEquals("Empty", empty.getVerb());
		s.bagCoal = 0;
		s.furnaceOre = 26;
		s.furnaceCoal = 27;
		Step wait = c.next(s);
		assertTrue(wait.isWaiting());
		assertEquals(Step.Target.DISPENSER, wait.getTarget());
	}

	@Test
	public void takesBarsWhenReadyAndAsksForIceGlovesWhenHot()
	{
		FurnaceCycle c = new FurnaceCycle();
		FurnaceCycle.Snapshot s = snap(Bar.GOLD);
		s.dispenserBars = 26;
		s.dispenserState = 2;
		s.iceGlovesWorn = false;
		s.iceGlovesCarried = true;
		Step equip = c.next(s);
		assertEquals("Equip", equip.getVerb());
		assertTrue(equip.hasItem(ItemIds.ICE_GLOVES));
		s.iceGlovesWorn = true;
		assertEquals("Take", c.next(s).getVerb());
		s.dispenserState = 3;
		s.iceGlovesWorn = false;
		assertEquals("Take", c.next(s).getVerb());
	}

	@Test
	public void goldAsksForGauntletsBeforeLoading()
	{
		FurnaceCycle c = new FurnaceCycle();
		FurnaceCycle.Snapshot s = snap(Bar.GOLD);
		s.ore = 26;
		s.free = 0;
		s.goldsmithCarried = true;
		Step step = c.next(s);
		assertTrue(step.hasItem(ItemIds.GOLDSMITH_GAUNTLETS));
		s.goldsmithWorn = true;
		s.goldsmithCarried = false;
		assertEquals(Step.Target.BELT, c.next(s).getTarget());
	}

	@Test
	public void lowCofferComesFirst()
	{
		FurnaceCycle c = new FurnaceCycle();
		FurnaceCycle.Snapshot s = snap(Bar.STEEL);
		s.coffer = 0;
		s.ore = 26;
		Step step = c.next(s);
		assertEquals(Step.Target.COFFER, step.getTarget());
		assertTrue(step.getDetail().contains("stops"));
	}

	@Test
	public void beltFlightIsFollowed()
	{
		FurnaceCycle c = new FurnaceCycle();
		c.observe(52, 0, true);
		c.observe(0, 0, true);
		assertEquals(52, c.getOnBelt());
		c.observe(0, 52, true);
		assertEquals(0, c.getOnBelt());
	}

	@Test
	public void emptyHandedGoesToTheBank()
	{
		FurnaceCycle c = new FurnaceCycle();
		FurnaceCycle.Snapshot s = snap(Bar.ADAMANTITE);
		Step step = c.next(s);
		assertEquals(Step.Target.BANK, step.getTarget());
		assertTrue(step.getDetail().contains("coal"));
	}

	@Test
	public void aHopKeepsOreOnTheBeltButReadsAfresh()
	{
		FurnaceCycle c = new FurnaceCycle();
		c.observe(52, 0, true);
		c.observe(0, 0, true);
		c.relog();
		// Back in after the login grace with a fresh load carried: only a base, no new load.
		c.observe(27, 0, true);
		c.observe(27, 0, true);
		assertEquals(52, c.getOnBelt());
	}
}
