package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MotherlodeCycleTest
{
	private static MotherlodeCycle.Snapshot snap(int payDirt, int free, int loot, int sack)
	{
		MotherlodeCycle.Snapshot s = new MotherlodeCycle.Snapshot();
		s.payDirt = payDirt;
		s.free = free;
		s.loot = loot;
		s.sack = sack;
		return s;
	}

	@Test
	public void minesUntilTheInventoryIsFull()
	{
		MotherlodeCycle c = new MotherlodeCycle();
		Step s = c.next(snap(10, 17, 0, 0));
		assertEquals(Step.Target.VEIN, s.getTarget());
		assertTrue(s.getDetail().startsWith("17 more"));
		assertEquals(Step.Target.HOPPER, c.next(snap(27, 0, 0, 0)).getTarget());
	}

	@Test
	public void sizesTheLastLoadToFillTheSackExactly()
	{
		MotherlodeCycle c = new MotherlodeCycle();
		// 89 in the sack leaves room for 19: deposit at 19, not 27.
		Step s = c.next(snap(5, 22, 0, 89));
		assertEquals(Step.Target.VEIN, s.getTarget());
		assertTrue(s.getDetail(), s.getDetail().contains("14 more"));
		assertTrue(s.getDetail().contains("fills the sack"));
		assertEquals(Step.Target.HOPPER, c.next(snap(19, 8, 0, 89)).getTarget());
	}

	@Test
	public void countsPayDirtInFlightAgainstTheSack()
	{
		MotherlodeCycle c = new MotherlodeCycle();
		c.observe(27, 54, true, false);
		c.observe(0, 54, true, false);
		assertEquals(27, c.getInFlight());
		assertEquals(27, c.room(snap(0, 27, 0, 54)));
		c.observe(0, 70, true, false);
		assertEquals(11, c.getInFlight());
		assertEquals(27, c.getDeposited());
	}

	@Test
	public void forgetsFlightThatNeverLands()
	{
		MotherlodeCycle c = new MotherlodeCycle();
		c.observe(27, 0, true, false);
		c.observe(0, 0, true, false);
		for (int i = 0; i <= MotherlodeCycle.FLIGHT_TIMEOUT; i++)
		{
			c.observe(0, 0, false, false);
		}
		assertEquals(0, c.getInFlight());
	}

	@Test
	public void holdsFlightWhileTheWheelIsStopped()
	{
		MotherlodeCycle c = new MotherlodeCycle();
		c.observe(27, 0, true, true);
		c.observe(0, 0, true, true);
		for (int i = 0; i < 200; i++)
		{
			c.observe(0, 0, false, true);
		}
		assertEquals(27, c.getInFlight());
		MotherlodeCycle.Snapshot s = snap(0, 27, 0, 0);
		s.brokenStruts = 2;
		assertEquals(Step.Target.STRUT, c.next(s).getTarget());
	}

	@Test
	public void emptiesTheSackOnceFullThenGoesBackToMining()
	{
		MotherlodeCycle c = new MotherlodeCycle();
		assertEquals(Step.Target.SACK, c.next(snap(0, 27, 0, 108)).getTarget());
		assertTrue(c.isEmptying());
		// Collected 27: inventory full of ore, bank it.
		assertEquals(Step.Target.BANK, c.next(snap(0, 0, 27, 81)).getTarget());
		// After banking, back to the sack, not to the veins, though there is room.
		assertEquals(Step.Target.SACK, c.next(snap(0, 27, 0, 81)).getTarget());
		assertEquals(Step.Target.BANK, c.next(snap(0, 0, 27, 0)).getTarget());
		Step s = c.next(snap(0, 27, 0, 0));
		assertEquals(Step.Target.VEIN, s.getTarget());
		assertFalse(c.isEmptying());
	}

	@Test
	public void bankedEarlyWhenOverfull()
	{
		MotherlodeCycle c = new MotherlodeCycle();
		// The sack is full yet 27 pay-dirt is carried: it cannot go in, so bank it.
		assertEquals(Step.Target.BANK, c.next(snap(27, 0, 0, 108)).getTarget());
	}

	@Test
	public void biggerSackTakesMore()
	{
		MotherlodeCycle c = new MotherlodeCycle();
		MotherlodeCycle.Snapshot s = snap(0, 27, 0, 108);
		s.capacity = 189;
		assertEquals(Step.Target.VEIN, c.next(s).getTarget());
	}
}
