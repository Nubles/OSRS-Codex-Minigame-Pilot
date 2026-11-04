package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** The lobby's next step. */
public class NmzCycleTest
{
	private static NmzCycle.Snapshot ready()
	{
		NmzCycle.Snapshot s = new NmzCycle.Snapshot();
		s.habit[Nmz.Potion.OVERLOAD.ordinal()] = 5.2;
		s.habit[Nmz.Potion.ABSORPTION.ordinal()] = 20;
		s.rewardPoints = 50_000;
		s.coffer = 100;
		s.usualPrice = 26;
		s.usualKind = "Hard customisable rumble";
		return s;
	}

	@Test
	public void vialFirst()
	{
		NmzCycle.Snapshot s = ready();
		s.vialFull = true;
		assertEquals(Step.Target.DREAM_VIAL, NmzCycle.next(s).getTarget());
	}

	@Test
	public void potionsFromTheBarrelThenTheChest()
	{
		NmzCycle.Snapshot s = ready();
		s.barrel[Nmz.Potion.OVERLOAD.ordinal()] = 4;
		Step step = NmzCycle.next(s);
		assertEquals("Take overload from its barrel", step.getTitle());
		assertEquals("You drink about 6 doses a dream · 4 stored", step.getDetail());
		s.carried[Nmz.Potion.OVERLOAD.ordinal()] = 4;
		s.barrel[Nmz.Potion.OVERLOAD.ordinal()] = 0;
		// Four of six carried and the barrel empty: on to the absorptions, bought as none are stored.
		s.barrel[Nmz.Potion.ABSORPTION.ordinal()] = 0;
		step = NmzCycle.next(s);
		assertEquals("Buy absorption at the Rewards chest", step.getTitle());
		assertEquals("1,000 points a dose, 50 affordable · you drink about 20 doses a dream", step.getDetail());
		// Unknown barrels: never told to buy what may be stored.
		s.barrel[Nmz.Potion.ABSORPTION.ordinal()] = -1;
		assertEquals(Step.Target.DOMINIC, NmzCycle.next(s).getTarget());
	}

	@Test
	public void cofferThenDominic()
	{
		NmzCycle.Snapshot s = ready();
		s.habit = new double[Nmz.Potion.values().length];
		s.coffer = 25;
		assertEquals("Put coins in Dominic's coffer", NmzCycle.next(s).getTitle());
		s.coffer = 26;
		Step step = NmzCycle.next(s);
		assertEquals("Ask Dominic Onion for a dream", step.getTitle());
		assertEquals("Last time: hard customisable rumble, 26,000 coins", step.getDetail());
	}

	@Test
	public void tooFewPointsSkipsTheChest()
	{
		NmzCycle.Snapshot s = ready();
		s.rewardPoints = 900;
		s.barrel[Nmz.Potion.OVERLOAD.ordinal()] = 0;
		s.barrel[Nmz.Potion.ABSORPTION.ordinal()] = 0;
		assertEquals(Step.Target.DOMINIC, NmzCycle.next(s).getTarget());
	}
}
