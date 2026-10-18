package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ContractTest
{
	@Test
	public void everyHouseHasAtMostEightHotspotsWithUniqueObjects()
	{
		java.util.Set<Integer> seen = new java.util.HashSet<>();
		for (Home h : Home.values())
		{
			assertTrue(h.name(), h.hotspotCount() <= Home.HOTSPOTS);
			for (int i = 0; i < h.hotspotCount(); i++)
			{
				assertTrue("duplicate " + h.hotspot(i).getObjectId(), seen.add(h.hotspot(i).getObjectId()));
				assertEquals(i, h.indexOf(h.hotspot(i).getObjectId()));
			}
		}
	}

	@Test
	public void tiersByLevel()
	{
		assertEquals(Contract.Tier.BEGINNER, Contract.Tier.forLevel(1));
		assertEquals(Contract.Tier.NOVICE, Contract.Tier.forLevel(49));
		assertEquals(Contract.Tier.ADEPT, Contract.Tier.forLevel(50));
		assertEquals(Contract.Tier.EXPERT, Contract.Tier.forLevel(99));
		assertEquals(5, Contract.Tier.EXPERT.points());
	}

	@Test
	public void noContractSendsToAContractor()
	{
		Contract.Snapshot s = new Contract.Snapshot();
		s.constructionLevel = 72;
		Step step = Contract.next(null, s);
		assertEquals(Step.Target.CONTRACTOR, step.getTarget());
		assertTrue(step.getDetail().contains("an expert contract"));
	}

	@Test
	public void countsPlanksAndSteelFromTheHotspots()
	{
		Contract c = new Contract(Home.LARRY, Contract.Tier.ADEPT);
		// Larry: SB, B2, B2, B3, RP, B3, RP.
		int[] v = {1, 3, 2, 4, 1, 6, 0, 0};
		assertEquals(2 + 3 + 1, c.planksNeeded(v));
		assertEquals(1, c.steelNeeded(v));
		assertEquals(4, c.remaining(v).size());
	}

	@Test
	public void asksForMissingMaterialsBeforeTravelling()
	{
		Contract c = new Contract(Home.LARRY, Contract.Tier.ADEPT);
		Contract.Snapshot s = new Contract.Snapshot();
		s.values = new int[]{1, 3, 2, 4, 1, 6, 0, 0};
		s.planks = 2;
		Step step = Contract.next(c, s);
		assertEquals("Fetch", step.getVerb());
		assertTrue(step.getTitle(), step.getTitle().contains("4 teak planks and 1 steel bar"));
		assertTrue(step.hasItem(8780));
		s.planks = 6;
		s.steelBars = 1;
		assertEquals(Step.Target.HOME, Contract.next(c, s).getTarget());
		s.atHome = true;
		assertEquals(Step.Target.HOTSPOT, Contract.next(c, s).getTarget());
	}

	@Test
	public void reportsWhenEverythingIsDone()
	{
		Contract c = new Contract(Home.NORMAN, Contract.Tier.EXPERT);
		Contract.Snapshot s = new Contract.Snapshot();
		s.values = new int[]{2, 2, 8, 8, 8, 8, 8, 0};
		Step step = Contract.next(c, s);
		assertEquals(Step.Target.CLIENT, step.getTarget());
		assertTrue(step.getDetail().contains("upstairs"));
		assertEquals(Contract.Tier.EXPERT, Contract.tierFromValues(s.values));
		assertNull(Contract.tierFromValues(new int[8]));
	}
}
