package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Every turn of a Giants' Foundry sword. */
public class FoundryCycleTest
{
	private static final FoundryCycle.Tool H = FoundryCycle.Tool.HAMMER;
	private static final FoundryCycle.Tool G = FoundryCycle.Tool.GRIND;
	private static final FoundryCycle.Tool P = FoundryCycle.Tool.POLISH;

	private static FoundryCycle.Snapshot commissioned()
	{
		FoundryCycle.Snapshot s = new FoundryCycle.Snapshot();
		s.first = Mould.Style.NARROW;
		s.second = Mould.Style.HEAVY;
		s.pick = MouldPick.best(s.first, s.second, null);
		s.jig = FoundryCycle.Jig.EMPTY;
		return s;
	}

	private static FoundryCycle.Snapshot working(int progress, int heat, FoundryCycle.Tool... stages)
	{
		FoundryCycle.Snapshot s = commissioned();
		s.jig = FoundryCycle.Jig.EMPTY;
		s.holding = true;
		s.progress = progress;
		s.heat = heat;
		s.stages = stages;
		s.halfBand = 100;
		return s;
	}

	@Test
	public void toolsAsTheWikiHasThem()
	{
		// Trip hammer: hot, +2% progress, -2.5% heat. Grindstone: medium, +1%, +1.5%. Polishing wheel: cold, +1%, -1.7%.
		assertEquals(20, H.progress());
		assertEquals(-25, H.heat());
		assertEquals("hot", H.heatWord());
		assertEquals(10, G.progress());
		assertEquals(15, G.heat());
		assertEquals("medium", G.heatWord());
		assertEquals(10, P.progress());
		assertEquals(-17, P.heat());
		assertEquals("cold", P.heatWord());
		assertTrue(P.centre() < G.centre() && G.centre() < H.centre());
	}

	@Test
	public void beforeTheSword()
	{
		FoundryCycle.Snapshot s = new FoundryCycle.Snapshot();
		assertEquals(Step.Target.KOVAC, FoundryCycle.next(s).getTarget());
		s = commissioned();
		Step set = FoundryCycle.next(s);
		assertEquals("Set the moulds", set.getTitle());
		assertEquals("Serrated Forte, Gladius Edge, Gladius Point (46)", set.getDetail());
		s.jig = FoundryCycle.Jig.SET;
		assertEquals(Step.Target.BANK_CHEST, FoundryCycle.next(s).getTarget());
		s.barsCarried = 10;
		s.crucible = 18;
		s.metal = 40;
		Step fill = FoundryCycle.next(s);
		assertEquals(Step.Target.CRUCIBLE, fill.getTarget());
		assertTrue(fill.getDetail().contains("18 of 28") && fill.getDetail().contains("10 to go"));
		s.crucible = 28;
		s.metal = 89;
		assertEquals("Full: metal score 89, quality up to 135", FoundryCycle.next(s).getDetail());
		s.jig = FoundryCycle.Jig.COOLING;
		assertTrue(FoundryCycle.next(s).isWaiting());
		s.jig = FoundryCycle.Jig.COOLED;
		s.canPickUp = false;
		assertTrue(FoundryCycle.next(s).getDetail().contains("bucket of water"));
		s.canPickUp = true;
		assertEquals("Pick up the preform", FoundryCycle.next(s).getTitle());
	}

	@Test
	public void heatsAndCoolsToTheBand()
	{
		// Hammer band 733-933 with a half band of 100.
		Step dunk = FoundryCycle.next(working(0, 300, H, P, G));
		assertEquals(Step.Target.LAVA_POOL, dunk.getTarget());
		assertEquals("Dunk", dunk.getVerb());
		Step heat = FoundryCycle.next(working(0, 650, H, P, G));
		assertEquals("Heat", heat.getVerb());
		assertTrue(heat.getDetail().contains("stop near the top"));
		Step quench = FoundryCycle.next(working(400, 900, H, P, G));
		assertEquals(Step.Target.WATERFALL, quench.getTarget());
		assertEquals("Quench", quench.getVerb());
		Step cool = FoundryCycle.next(working(400, 400, H, P, G));
		assertEquals("Cool", cool.getVerb());
		// The grindstone heats as you work it: stop near the bottom of its band.
		Step grindHeat = FoundryCycle.next(working(700, 300, H, P, G));
		assertTrue(grindHeat.getDetail().contains("near the bottom"));
	}

	@Test
	public void inTheBand()
	{
		FoundryCycle.Snapshot s = working(0, 900, H, P, G, H, P);
		Step use = FoundryCycle.next(s);
		assertEquals(Step.Target.TRIP_HAMMER, use.getTarget());
		assertEquals("Stage 1 of 5 · heat for 7 uses, 10 to finish", use.getDetail());
		s.bonus = true;
		assertEquals("Bonus", FoundryCycle.next(s).getVerb());
		FoundryCycle.Snapshot end = working(180, 760, H, P, G, H, P);
		assertEquals("Stage 1 of 5 · 1 to finish, then polish (cold)", FoundryCycle.next(end).getDetail());
	}

	@Test
	public void stagesAndCounts()
	{
		FoundryCycle.Snapshot s = working(0, 500, H, G, P);
		assertEquals(0, FoundryCycle.stageIndex(s));
		s.progress = 333;
		assertEquals(0, FoundryCycle.stageIndex(s));
		s.progress = 334;
		assertEquals(1, FoundryCycle.stageIndex(s));
		s.progress = 1000;
		assertEquals(2, FoundryCycle.stageIndex(s));
		assertEquals(0, FoundryCycle.usesInBand(H, 500, 100));
		assertEquals(0, FoundryCycle.usesInBand(H, 900, -1));
		assertNull(FoundryCycle.band(H, -1));
		s.stages = new FoundryCycle.Tool[0];
		assertEquals(-1, FoundryCycle.stageIndex(s));
	}

	@Test
	public void withoutTheBandsItStillNamesTheTool()
	{
		FoundryCycle.Snapshot s = working(0, 500, G, H);
		s.halfBand = -1;
		Step step = FoundryCycle.next(s);
		assertEquals(Step.Target.GRINDSTONE, step.getTarget());
		assertTrue(step.getDetail().contains("medium"));
		s.stages = new FoundryCycle.Tool[0];
		assertEquals(Step.Target.NONE, FoundryCycle.next(s).getTarget());
	}

	@Test
	public void storedFinishedRuined()
	{
		FoundryCycle.Snapshot s = working(400, 500, H, G);
		s.holding = false;
		s.stored = true;
		assertEquals(Step.Target.PREFORM_STORAGE, FoundryCycle.next(s).getTarget());
		s.holding = true;
		s.stored = false;
		s.progress = 1000;
		assertEquals(Step.Target.KOVAC, FoundryCycle.next(s).getTarget());
		s.progress = 400;
		s.ruined = true;
		Step hand = FoundryCycle.next(s);
		assertEquals(Step.Target.KOVAC, hand.getTarget());
		assertTrue(hand.getDetail().contains("Too damaged"));
		assertEquals(4, FoundryCycle.stage(hand));
	}

	@Test
	public void loopStages()
	{
		assertEquals(0, FoundryCycle.stage(FoundryCycle.next(new FoundryCycle.Snapshot())));
		assertEquals(1, FoundryCycle.stage(FoundryCycle.next(commissioned())));
		FoundryCycle.Snapshot s = commissioned();
		s.jig = FoundryCycle.Jig.SET;
		assertEquals(2, FoundryCycle.stage(FoundryCycle.next(s)));
		assertEquals(3, FoundryCycle.stage(FoundryCycle.next(working(0, 300, H))));
	}
}
