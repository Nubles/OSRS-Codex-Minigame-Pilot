package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.WintertodtCycle.Brazier;
import com.codex.minigamepilot.core.WintertodtCycle.Snapshot;
import org.junit.Test;

/** Every turn of a Wintertodt round. */
public class WintertodtCycleTest
{
	private static Snapshot round()
	{
		Snapshot s = new Snapshot();
		s.energy = 80;
		s.warmth = 90;
		s.points = 120;
		return s;
	}

	@Test
	public void chopUntilFullThenFletchThenFeed()
	{
		Snapshot s = round();
		Step step = WintertodtCycle.next(s);
		assertEquals("Chop bruma roots", step.getTitle());
		assertEquals(Step.Target.ROOTS, step.getTarget());
		assertEquals("28 free slots · 380 to 500", step.getDetail());
		s.roots = 10;
		s.free = 18;
		assertEquals("Chop bruma roots", WintertodtCycle.next(s).getTitle());
		s.roots = 28;
		s.free = 0;
		step = WintertodtCycle.next(s);
		assertEquals("Fletch the roots into kindling", step.getTitle());
		assertTrue(step.hasItem(Wintertodt.ROOT));
		// Part fletched: still fletching, though slots are free again.
		s.roots = 12;
		s.kindling = 16;
		assertEquals("Fletch the roots into kindling", WintertodtCycle.next(s).getTitle());
		s.roots = 0;
		s.kindling = 28;
		step = WintertodtCycle.next(s);
		assertEquals("Feed the brazier", step.getTitle());
		assertEquals("28 to burn · +700 points · 380 to 500", step.getDetail());
		assertEquals(2, WintertodtCycle.stage(step));
	}

	@Test
	public void withoutFletchingRootsAreFedWhenFull()
	{
		Snapshot s = round();
		s.fletch = false;
		s.roots = 28;
		s.free = 0;
		assertEquals("Feed the brazier", WintertodtCycle.next(s).getTitle());
		s.fletch = true;
		s.knife = false;
		assertEquals("Feed the brazier", WintertodtCycle.next(s).getTitle());
	}

	@Test
	public void theRoundEndingBurnsWhatYouHold()
	{
		Snapshot s = round();
		s.roots = 6;
		s.free = 22;
		s.energy = 9;
		assertEquals("Chop bruma roots", WintertodtCycle.next(s).getTitle());
		// Six held, energy 8: "two less than the remaining energy".
		s.energy = 8;
		Step step = WintertodtCycle.next(s);
		assertEquals("Feed the brazier", step.getTitle());
		assertTrue(step.getDetail().startsWith("Energy 8%: burn your 6"));
	}

	@Test
	public void warmthComesFirst()
	{
		Snapshot s = round();
		s.warmth = 40;
		s.brazier = Brazier.BROKEN;
		Step step = WintertodtCycle.next(s);
		assertEquals("Warm up: eat or drink", step.getTitle());
		assertTrue(step.getDetail().startsWith("Warmth 40%: food 4+ gives 35%"));
		s.eatAt = 30;
		assertEquals("Fix the brazier", WintertodtCycle.next(s).getTitle());
		s.eatAt = 0;
		s.warmth = 5;
		assertEquals("Fix the brazier", WintertodtCycle.next(s).getTitle());
	}

	@Test
	public void brazierFixHealLight()
	{
		Snapshot s = round();
		s.brazier = Brazier.BROKEN;
		s.pyroDown = true;
		s.doses = 2;
		assertEquals("Fix the brazier", WintertodtCycle.next(s).getTitle());
		s.brazier = Brazier.UNLIT;
		Step step = WintertodtCycle.next(s);
		assertEquals("Heal the Pyromancer", step.getTitle());
		assertEquals(Step.Target.PYROMANCER, step.getTarget());
		// No dose: she cannot be healed, and the brazier cannot be lit; chop on.
		s.doses = 0;
		assertEquals("Chop bruma roots", WintertodtCycle.next(s).getTitle());
		s.pyroDown = false;
		step = WintertodtCycle.next(s);
		assertEquals("Light the brazier", step.getTitle());
		assertTrue(step.getDetail().startsWith("+25 points"));
		s.lighter = false;
		assertEquals("Chop bruma roots", WintertodtCycle.next(s).getTitle());
	}

	@Test
	public void nothingLitAnywhere()
	{
		Snapshot s = round();
		s.brazier = Brazier.BROKEN;
		s.hammer = false;
		s.anyLit = false;
		s.kindling = 28;
		s.free = 0;
		Step step = WintertodtCycle.next(s);
		assertEquals("Wait for a brazier to burn", step.getTitle());
		assertTrue(step.isWaiting());
	}

	@Test
	public void betweenRoundsTheToolsThenWait()
	{
		Snapshot s = round();
		s.round = false;
		s.energy = -1;
		s.returnsIn = 42;
		s.lighter = false;
		s.axe = false;
		assertEquals("Take a tinderbox", WintertodtCycle.next(s).getTitle());
		s.lighter = true;
		assertEquals("Take an axe", WintertodtCycle.next(s).getTitle());
		s.axe = true;
		s.knife = false;
		assertEquals("Take a knife", WintertodtCycle.next(s).getTitle());
		s.fletch = false;
		s.hammer = false;
		assertEquals("Take a hammer", WintertodtCycle.next(s).getTitle());
		s.hammer = true;
		Step step = WintertodtCycle.next(s);
		assertEquals("Wait by your brazier", step.getTitle());
		assertEquals("Back in 0:42 · light it as the round starts (+25)", step.getDetail());
		assertEquals(Step.Target.TOOL_CRATE, WintertodtCycle.next(withNoAxe(s)).getTarget());
	}

	private static Snapshot withNoAxe(Snapshot s)
	{
		s.axe = false;
		return s;
	}

	@Test
	public void underFiftyFiremaking()
	{
		Snapshot s = round();
		s.firemaking = 49;
		assertEquals("Train Firemaking to 50", WintertodtCycle.next(s).getTitle());
	}

	@Test
	public void theCartAfter()
	{
		Snapshot s = new Snapshot();
		s.inPrison = false;
		assertNull(WintertodtCycle.next(s));
		s.owed = 3;
		s.lastPoints = 1240;
		assertNull(WintertodtCycle.next(s));
		s.cartInSight = true;
		Step step = WintertodtCycle.next(s);
		assertEquals("Search the reward cart", step.getTitle());
		assertEquals("3 rewards owed, +48% of one more · Big-search takes 5 a click", step.getDetail());
	}

	@Test
	public void pointsPastFiveHundred()
	{
		Snapshot s = round();
		s.points = 1240;
		assertEquals("28 free slots · 3 rewards so far", WintertodtCycle.next(s).getDetail());
	}
}
