package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** The one next step at the Volcanic Mine's entrance. */
public class VolcanicCycleTest
{
	private static VolcanicCycle.Snapshot ready()
	{
		VolcanicCycle.Snapshot s = new VolcanicCycle.Snapshot();
		s.mining = 70;
		s.numulite = 120;
		s.points = 5_000;
		s.goal = Volcanic.Reward.PROSPECTOR_KIT;
		s.goalCost = 120_000;
		s.nextPiece = Volcanic.Piece.BOOTS;
		return s;
	}

	@Test
	public void miningFirst()
	{
		VolcanicCycle.Snapshot s = ready();
		s.mining = 49;
		Step step = VolcanicCycle.next(s);
		assertEquals("Train Mining to 50", step.getTitle());
		assertEquals(Step.Target.NONE, step.getTarget());
	}

	@Test
	public void payAndGoIn()
	{
		Step step = VolcanicCycle.next(ready());
		assertEquals("Pay Petrified Pete and go in", step.getTitle());
		assertEquals("30 numulite a game; you carry 120", step.getDetail());
		assertEquals(Step.Target.PETRIFIED_PETE, step.getTarget());
	}

	@Test
	public void numuliteForTheFee()
	{
		VolcanicCycle.Snapshot s = ready();
		s.numulite = 12;
		Step step = VolcanicCycle.next(s);
		assertEquals("Bring 30 numulite", step.getTitle());
		assertEquals("Petrified Pete takes 30 a game, or 3,000 once for good; you carry 12", step.getDetail());
		assertEquals(Step.Target.VOLCANO_BANK, step.getTarget());
		s.permanent = true;
		assertEquals("Enter the Volcanic Mine", VolcanicCycle.next(s).getTitle());
	}

	@Test
	public void buyThePieceYouCanAfford()
	{
		VolcanicCycle.Snapshot s = ready();
		s.points = 21_000;
		Step step = VolcanicCycle.next(s);
		assertEquals("Buy the prospector boots", step.getTitle());
		assertEquals("Pete's shop: 21,000 of your 21,000 points", step.getDetail());
		s.goal = Volcanic.Reward.ORE_PACK;
		s.goalCost = 4_000;
		s.nextPiece = null;
		// Ore packs are bought again and again: the reward card says how many, the step stays on the game.
		assertEquals("Pay Petrified Pete and go in", VolcanicCycle.next(s).getTitle());
		// Owned already, or points not known: go and play.
		s.goal = Volcanic.Reward.ASH_COVERED_TOME;
		s.goalCost = 0;
		assertEquals("Pay Petrified Pete and go in", VolcanicCycle.next(s).getTitle());
		s.goalCost = 40_000;
		s.points = -1;
		assertEquals("Pay Petrified Pete and go in", VolcanicCycle.next(s).getTitle());
	}
}
