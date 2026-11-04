package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class TierAdviceTest
{
	@Test
	public void theLargestHouseSetsTheMostAContractCanTake()
	{
		// The wiki's expert estimates top out at every piece of the house: Jess 15, Larry 12,
		// Barbara 10, Leela 13, Mariah 14.
		assertEquals(15, Home.JESS.fullPlanks());
		assertEquals(12, Home.LARRY.fullPlanks());
		assertEquals(10, Home.BARBARA.fullPlanks());
		assertEquals(13, Home.LEELA.fullPlanks());
		assertEquals(14, Home.MARIAH.fullPlanks());
		assertEquals(17, Home.mostPlanks());
	}

	@Test
	public void noBankGoesByLevel()
	{
		TierAdvice.Advice a = TierAdvice.advise(72, null, null);
		assertEquals(Contract.Tier.EXPERT, a.getTier());
		assertEquals(-1, a.getCovers());
		assertNull(a.getWhy());
	}

	@Test
	public void enoughMahoganyTakesExpert()
	{
		TierAdvice.Advice a = TierAdvice.advise(72, new long[]{0, 0, 0, 120}, null);
		assertEquals(Contract.Tier.EXPERT, a.getTier());
		assertEquals(7, a.getCovers());
		assertEquals("Your mahogany planks cover at least 7 contracts", a.getWhy());
	}

	@Test
	public void shortOfMahoganyFallsBackToTeak()
	{
		TierAdvice.Advice a = TierAdvice.advise(72, new long[]{0, 0, 160, 9}, null);
		assertEquals(Contract.Tier.ADEPT, a.getTier());
		assertEquals("Not enough mahogany planks for an expert contract; your teak planks cover at least 9 contracts", a.getWhy());
	}

	@Test
	public void yourOwnContractsSetThePlanksOnceLearned()
	{
		ContractBook book = new ContractBook();
		book.record(Contract.Tier.EXPERT, 2900, 12, 1, 200);
		book.record(Contract.Tier.EXPERT, 2900, 14, 0, 200);
		TierAdvice.Advice a = TierAdvice.advise(72, new long[]{0, 0, 0, 26}, book);
		assertEquals(Contract.Tier.EXPERT, a.getTier());
		assertEquals(2, a.getCovers());
		assertEquals("Your mahogany planks cover about 2 contracts", a.getWhy());
	}

	@Test
	public void nothingEnoughSaysWhatToBring()
	{
		TierAdvice.Advice a = TierAdvice.advise(55, new long[]{3, 0, 5, 0}, null);
		assertEquals(Contract.Tier.ADEPT, a.getTier());
		assertEquals(0, a.getCovers());
		assertEquals("Bring 12 teak planks more: a contract can take up to 17", a.getWhy());
	}

	@Test
	public void theStepAsksForTheAdvisedTier()
	{
		Contract.Snapshot s = new Contract.Snapshot();
		s.constructionLevel = 72;
		s.advice = TierAdvice.advise(72, new long[]{0, 0, 160, 9}, null);
		Step step = Contract.next(null, s);
		assertEquals("Get an adept contract", step.getTitle());
		assertEquals("Ask Amy in Falador, Marlo in Varrock, Ellie in East Ardougne or Angelo in Hosidius. "
			+ "Not enough mahogany planks for an expert contract; your teak planks cover at least 9 contracts", step.getDetail());
	}

	@Test
	public void fetchingSaysThePlankSacksRoom()
	{
		Contract c = new Contract(Home.LARRY, Contract.Tier.EXPERT);
		Contract.Snapshot s = new Contract.Snapshot();
		s.values = new int[]{0, 3, 3, 1, 1, 0, 0, 0};
		s.sack = true;
		s.sackPlanks = 20;
		s.planks = 2;
		Step step = Contract.next(c, s);
		assertEquals("Fetch", step.getVerb());
		assertEquals("The job needs 8 mahogany planks; you have 2. Your plank sack has room for 8", step.getDetail());
	}
}
