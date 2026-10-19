package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PointsTest
{
	private static final long PLANK_SACK = 350;

	@Test
	public void aTotalThatDropsAfterAPurchaseIsABalance()
	{
		Points p = new Points();
		assertEquals(0, p.message(120, 470, 0));
		p.bought(PLANK_SACK);
		// Bought for 350, then one contract paid 4: a balance reads 124.
		assertEquals(4, p.message(121, 124, PLANK_SACK));
		assertEquals(Points.Meaning.BALANCE, p.getMeaning());
		assertEquals(124, p.balance(PLANK_SACK));
	}

	@Test
	public void aTotalThatKeepsClimbingAfterAPurchaseIsLifetime()
	{
		Points p = new Points();
		p.message(120, 470, 0);
		p.bought(PLANK_SACK);
		assertEquals(4, p.message(121, 474, PLANK_SACK));
		assertEquals(Points.Meaning.LIFETIME, p.getMeaning());
		// Every point ever earned less what the sack cost.
		assertEquals(124, p.balance(PLANK_SACK));
	}

	@Test
	public void untilItIsKnownTheLowerReadingIsTaken()
	{
		Points p = new Points();
		p.message(120, 470, PLANK_SACK);
		assertEquals(4, p.message(121, 474, PLANK_SACK));
		assertEquals(Points.Meaning.UNKNOWN, p.getMeaning());
		assertEquals(124, p.balance(PLANK_SACK));
		// Owning nothing, both readings agree.
		assertEquals(474, p.balance(0));
	}

	@Test
	public void aPurchaseBeforeTheNextContractCountsAtOnce()
	{
		Points p = new Points();
		p.message(120, 470, 0);
		p.bought(PLANK_SACK);
		// Unknown meaning: the sack is off both readings (spent from a balance, owned against a lifetime).
		assertEquals(120, p.balance(PLANK_SACK));
	}

	@Test
	public void aTotalBelowWhatYouOwnCanOnlyBeABalance()
	{
		Points p = new Points();
		p.message(10, 100, PLANK_SACK);
		assertEquals(Points.Meaning.BALANCE, p.getMeaning());
		assertEquals(100, p.balance(PLANK_SACK));
	}

	@Test
	public void aDropWithNoPurchaseSeenIsStillABalance()
	{
		Points p = new Points();
		p.message(120, 470, 0);
		// Bought somewhere the Pilot did not see (straight to the bank, say).
		p.message(121, 120, 0);
		assertEquals(Points.Meaning.BALANCE, p.getMeaning());
		assertEquals(120, p.balance(0));
	}

	@Test
	public void theSameLineTwiceIsNotEarningsTwice()
	{
		Points p = new Points();
		p.message(120, 470, 0);
		assertEquals(4, p.message(121, 474, 0));
		assertEquals(0, p.message(121, 474, 0));
		assertEquals(474, p.getTotal());
	}

	@Test
	public void severalContractsBetweenLinesAreCountedTogether()
	{
		Points p = new Points();
		p.message(120, 470, 0);
		p.bought(200);
		// Three contracts' lines missed, and boots bought for 200: a total of 482 climbed 12, which
		// fits three contracts' pay, while a balance would have had to climb 212.
		assertEquals(12, p.message(123, 482, 200));
		assertEquals(Points.Meaning.LIFETIME, p.getMeaning());
	}

	@Test
	public void savesAndReadsBackIncludingOlderBareTotals()
	{
		Points p = new Points();
		p.message(120, 470, 0);
		p.bought(PLANK_SACK);
		Points back = Points.decode(p.encode());
		assertEquals(p.encode(), back.encode());
		assertEquals(120, back.balance(PLANK_SACK));

		Points old = Points.decode("470");
		assertTrue(old.known());
		assertEquals(470, old.getTotal());
		assertEquals(Points.Meaning.UNKNOWN, old.getMeaning());
		// The first line after an upgrade sets the contract count without counting earnings.
		assertEquals(0, old.message(121, 474, 0));
		assertEquals(4, old.message(122, 478, 0));

		assertFalse(Points.decode(null).known());
		assertFalse(Points.decode("470:x:BALANCE:0").known());
		assertFalse(Points.decode("470:1:SIDEWAYS:0").known());
		assertEquals(-1, new Points().balance(0));
	}
}
