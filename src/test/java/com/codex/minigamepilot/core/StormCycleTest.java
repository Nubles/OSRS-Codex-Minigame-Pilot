package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** The one next step at Tempoross, turn by turn. Facts: OSRS wiki, Tempoross and Tempoross/Strategies. */
public class StormCycleTest
{
	private static StormCycle.Snapshot fishing()
	{
		StormCycle.Snapshot s = new StormCycle.Snapshot();
		s.energy = 100;
		s.essence = 100;
		s.storm = 10;
		s.free = 24;
		return s;
	}

	@Test
	public void fishesUntilThePackIsFull()
	{
		StormCycle.Snapshot s = fishing();
		assertEquals(Step.Target.FISHING_SPOT, StormCycle.next(s).getTarget());
		assertEquals("Catch harpoonfish", StormCycle.next(s).getTitle());
	}

	@Test
	public void theDoubleSpotComesFirstEvenWithRawFishToCook()
	{
		StormCycle.Snapshot s = fishing();
		s.doubleSpot = true;
		s.raw = 10;
		s.free = 14;
		assertEquals("Fish at the double spot", StormCycle.next(s).getTitle());
	}

	@Test
	public void cooksAFullPackThenLoadsIt()
	{
		StormCycle.Snapshot s = fishing();
		s.free = 0;
		s.raw = 24;
		assertEquals(Step.Target.SHRINE, StormCycle.next(s).getTarget());
		s.raw = 0;
		s.cooked = 24;
		Step load = StormCycle.next(s);
		assertEquals(Step.Target.AMMO_CRATE, load.getTarget());
		// Cooked fish drain 15 energy each (OSRS wiki).
		assertTrue(load.getDetail(), load.getDetail().contains("360 energy"));
	}

	@Test
	public void withoutCookingLoadsRaw()
	{
		StormCycle.Snapshot s = fishing();
		s.cook = false;
		s.free = 0;
		s.raw = 20;
		Step load = StormCycle.next(s);
		assertEquals(Step.Target.AMMO_CRATE, load.getTarget());
		assertTrue(load.getDetail().contains("200 energy"));
	}

	@Test
	public void aHighStormLoadsWhatYouHold()
	{
		StormCycle.Snapshot s = fishing();
		s.storm = 92;
		s.raw = 3;
		s.free = 21;
		s.doubleSpot = true;
		assertEquals(Step.Target.AMMO_CRATE, StormCycle.next(s).getTarget());
		s.stormLoad = 0;
		assertEquals(Step.Target.FISHING_SPOT, StormCycle.next(s).getTarget());
		// Nothing held: nothing to load, so fish.
		s.stormLoad = 80;
		s.raw = 0;
		assertEquals(Step.Target.FISHING_SPOT, StormCycle.next(s).getTarget());
	}

	@Test
	public void aWaveComesBeforeEverything()
	{
		StormCycle.Snapshot s = fishing();
		s.poolActive = true;
		s.waveIncoming = true;
		s.canTether = true;
		assertEquals("Tether before the wave hits", StormCycle.next(s).getTitle());
		s.tethered = true;
		Step hold = StormCycle.next(s);
		assertTrue(hold.isWaiting());
		assertEquals(Step.Target.TETHER, hold.getTarget());
		s.tethered = false;
		s.canTether = false;
		assertEquals("Take a rope", StormCycle.next(s).getTitle());
	}

	@Test
	public void theSpiritPoolWhileTemporossIsDown()
	{
		StormCycle.Snapshot s = fishing();
		s.energy = 0;
		s.poolActive = true;
		s.raw = 5;
		Step step = StormCycle.next(s);
		assertEquals(Step.Target.SPIRIT_POOL, step.getTarget());
		assertEquals(3, StormCycle.stage(step));
	}

	@Test
	public void noHarpoonTakesOne()
	{
		StormCycle.Snapshot s = fishing();
		s.harpoon = false;
		assertEquals("Take a harpoon", StormCycle.next(s).getTitle());
	}

	@Test
	public void waitsOutsideAGame()
	{
		StormCycle.Snapshot s = fishing();
		s.inGame = false;
		assertTrue(StormCycle.next(s).isWaiting());
	}

	@Test
	public void wavesAndFiresKeepTheStageTheyBreakInto()
	{
		assertEquals(-1, StormCycle.stage(StormCycle.douse(null, "the shrine")));
		assertEquals(-1, StormCycle.stage(Step.of("Tether", "t", "", Step.Target.TETHER)));
		assertEquals(0, StormCycle.stage(Step.of("Harpoon", "f", "", Step.Target.FISHING_SPOT)));
	}
}
