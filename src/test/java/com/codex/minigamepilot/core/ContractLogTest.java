package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import org.junit.Test;

public class ContractLogTest
{
	@Test
	public void pointsAreTheWikisPerTier()
	{
		// OSRS wiki, Carpenter points: beginner 2, novice 3, adept 4, expert 5.
		assertEquals(2, Contract.Tier.BEGINNER.points());
		assertEquals(3, Contract.Tier.NOVICE.points());
		assertEquals(4, Contract.Tier.ADEPT.points());
		assertEquals(5, Contract.Tier.EXPERT.points());
	}

	@Test
	public void ratesCountOnlyTimedContracts()
	{
		ContractLog log = new ContractLog();
		assertEquals(-1, log.pointsPerHour(), 0);
		log.add(new ContractLog.Entry(1, Home.LARRY, Contract.Tier.EXPERT, 3000, 180));
		log.add(new ContractLog.Entry(2, Home.BOB, Contract.Tier.EXPERT, 0, 180));
		log.add(new ContractLog.Entry(3, Home.TAU, Contract.Tier.EXPERT, 2800, 0));
		// Two timed contracts, 10 points over 6 minutes: 100 an hour.
		assertEquals(100, log.pointsPerHour(), 1e-9);
		// Only Larry's is both timed and measured: 3,000 in 3 minutes.
		assertEquals(60_000, log.xpPerHour(), 1e-9);
		assertSame(log.all().get(0), log.quickest(Contract.Tier.EXPERT));
		assertNull(log.quickest(Contract.Tier.ADEPT));
	}

	@Test
	public void keepsTheLastFifty()
	{
		ContractLog log = new ContractLog();
		for (int i = 0; i < 60; i++)
		{
			log.add(new ContractLog.Entry(i, Home.JESS, Contract.Tier.ADEPT, 2000, 200));
		}
		assertEquals(50, log.count());
		assertEquals(10, log.all().get(0).endedAt);
	}

	@Test
	public void roundTrips()
	{
		ContractLog log = new ContractLog();
		log.add(new ContractLog.Entry(1760000000000L, Home.LARRY, Contract.Tier.EXPERT, 2950, 190));
		log.add(new ContractLog.Entry(1760000200000L, Home.BOB, Contract.Tier.ADEPT, 0, 99_999));
		String saved = log.encode();
		assertEquals("1;1760000000000,LARRY,EXPERT,2950,190;1760000200000,BOB,ADEPT,0,0", saved);
		assertEquals(saved, ContractLog.decode(saved).encode());
		assertEquals(1, ContractLog.decode("1;1,NOBODY,EXPERT,1,1;2,LARRY,EXPERT,1,1").count());
		assertEquals(0, ContractLog.decode("2;1,LARRY,EXPERT,1,1").count());
	}
}
