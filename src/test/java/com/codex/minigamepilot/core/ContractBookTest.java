package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.List;
import java.util.function.IntUnaryOperator;
import org.junit.Test;

public class ContractBookTest
{
	/** Plank 300, oak 600, teak 1,500, mahogany 2,400, steel bar 500. */
	private static final IntUnaryOperator PRICES = id ->
	{
		switch (id)
		{
			case 960:
				return 300;
			case 8778:
				return 600;
			case 8780:
				return 1500;
			case 8782:
				return 2400;
			case Contract.STEEL_BAR:
				return 500;
			default:
				return 0;
		}
	};

	@Test
	public void ranksTheTiersYouHaveDoneByCoinsAnExperiencePoint()
	{
		ContractBook book = new ContractBook();
		// Adept: 10 teak and a steel bar for 1,000 xp, a loop of 4 minutes.
		book.record(Contract.Tier.ADEPT, 1000, 10, 1, 240);
		// Expert: 10 mahogany for 2,000 xp, untimed.
		book.record(Contract.Tier.EXPERT, 2000, 10, 0, 0);
		List<ContractBook.Row> rows = book.rank(PRICES, 75);
		assertEquals(4, rows.size());
		ContractBook.Row first = rows.get(0);
		assertSame(Contract.Tier.EXPERT, first.getTier());
		assertEquals(24_000, first.getCoinsEach(), 0.01);
		assertEquals(12.0, first.getPerXp(), 0.001);
		assertEquals("untimed: no rate", 0, first.getXpPerHour(), 0);
		ContractBook.Row second = rows.get(1);
		assertSame(Contract.Tier.ADEPT, second.getTier());
		assertEquals(15.5, second.getPerXp(), 0.001);
		assertEquals(15_000, second.getXpPerHour(), 0.01);
		// Tiers not done yet follow, highest first, all within reach at 75.
		assertSame(Contract.Tier.NOVICE, rows.get(2).getTier());
		assertFalse(rows.get(2).isKnown());
		assertTrue(rows.get(3).isUnlocked());
	}

	@Test
	public void tiersOutOfReachComeLast()
	{
		ContractBook book = new ContractBook();
		book.record(Contract.Tier.NOVICE, 500, 8, 0, 300);
		List<ContractBook.Row> rows = book.rank(PRICES, 30);
		assertSame(Contract.Tier.NOVICE, rows.get(0).getTier());
		assertSame(Contract.Tier.BEGINNER, rows.get(1).getTier());
		assertFalse(rows.get(2).isUnlocked());
		assertFalse(rows.get(3).isUnlocked());
	}

	@Test
	public void anUnpricedPlankRanksNothingRatherThanCallingItFree()
	{
		ContractBook book = new ContractBook();
		book.record(Contract.Tier.EXPERT, 2000, 10, 0, 0);
		assertTrue(book.rank(id -> id == 8782 ? 0 : 500, 80).isEmpty());
	}

	@Test
	public void leavesOutWhatNoContractGivesAndTimesOnlyRealLoops()
	{
		ContractBook book = new ContractBook();
		assertFalse("no experience", book.record(Contract.Tier.ADEPT, 0, 10, 0, 100));
		assertFalse("POH training mixed in", book.record(Contract.Tier.ADEPT, 50_000, 10, 0, 100));
		assertFalse(book.record(null, 900, 10, 0, 100));
		assertTrue(book.isEmpty());
		assertTrue(book.record(Contract.Tier.ADEPT, 900, 10, 0, ContractBook.MAX_LOOP_SECONDS + 1));
		assertEquals("a long break is not a loop", 0, book.tally(Contract.Tier.ADEPT).secondsEach(), 0);
	}

	@Test
	public void savesAndReadsBackAndDropsDamage()
	{
		ContractBook book = new ContractBook();
		book.record(Contract.Tier.ADEPT, 1000, 10, 1, 240);
		book.record(Contract.Tier.ADEPT, 1200, 12, 0, 0);
		String saved = book.encode();
		assertEquals("ADEPT=2,2200,22,1,1,240", saved);
		ContractBook read = ContractBook.decode(saved + ";EXPERT=x,1,2,3,4,5;NOPE=1,1,1,1,1,1;BEGINNER=1,100,5,0,3,9");
		assertEquals(2, read.tally(Contract.Tier.ADEPT).getContracts());
		assertEquals(11, read.tally(Contract.Tier.ADEPT).planksEach(), 0.001);
		assertNull(read.tally(Contract.Tier.EXPERT));
		assertNull("more timed than done is damage", read.tally(Contract.Tier.BEGINNER));
		assertTrue(ContractBook.decode(null).isEmpty());
	}
}
