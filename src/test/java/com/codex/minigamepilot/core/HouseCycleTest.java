package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Houses.House;
import org.junit.Test;

/** The one next step when stealing valuables, for each state of a round. */
public class HouseCycleTest
{
	private static HouseCycle.Snapshot at(int level)
	{
		HouseCycle.Snapshot s = new HouseCycle.Snapshot();
		s.level = level;
		s.free = 20;
		s.citizens = true;
		return s;
	}

	private static Step next(HouseCycle.Snapshot s)
	{
		return HouseCycle.next(s).step;
	}

	@Test
	public void levelFirst()
	{
		Step s = next(at(49));
		assertEquals("Stealing valuables needs 50 Thieving", s.getTitle());
		assertTrue(s.isWaiting());
		assertTrue(s.getDetail().contains("Children of the Sun"));
	}

	@Test
	public void theDistractedCitizenBeforeAnything()
	{
		HouseCycle.Snapshot s = at(60);
		s.keys = 3;
		s.vacant = true;
		s.doorInSight = true;
		s.distracted = true;
		s.distractedLeft = 12;
		Step step = next(s);
		assertEquals("Pickpocket the distracted citizen", step.getTitle());
		assertEquals(Step.Target.CITIZEN, step.getTarget());
		assertEquals("Every pick succeeds for about 12s more", step.getDetail());
	}

	@Test
	public void waitForTheOwnerToWalkOff()
	{
		HouseCycle.Snapshot s = at(60);
		s.keys = 1;
		s.vacant = true;
		s.vacantHouse = House.CAIUS;
		s.doorInSight = true;
		s.ownerNear = true;
		Step step = next(s);
		assertEquals("Caius is still by the door", step.getTitle());
		assertTrue(step.isWaiting());
		s.ownerNear = false;
		s.leftMin = 150;
		s.leftMax = 160;
		step = next(s);
		assertEquals("Open the locked door of Caius' house", step.getTitle());
		assertEquals(Step.Target.LOCKED_DOOR, step.getTarget());
		assertEquals("Back in 150-160s; out by an open window", step.getDetail());
		s.doorInSight = false;
		assertEquals("Go to Caius' house", next(s).getTitle());
		assertEquals("Directly north-west of the western bank", next(s).getDetail());
	}

	@Test
	public void tooLateIsNotWorthAKey()
	{
		HouseCycle.Snapshot s = at(60);
		s.keys = 2;
		s.vacant = true;
		s.vacantHouse = House.VICTOR;
		s.doorInSight = true;
		s.leftMin = 20;
		s.leftMax = 30;
		Step step = next(s);
		assertEquals("Pickpocket a wealthy citizen", step.getTitle());
		assertEquals("Too little time at Victor's house: keep the keys", step.getDetail());
		s.teller = true;
		s.valuables = 15;
		assertEquals("Ask the urchin which house is vacant", next(s).getTitle());
	}

	@Test
	public void noKnownHouse()
	{
		HouseCycle.Snapshot s = at(60);
		s.keys = 1;
		assertEquals("Find the house with a Locked Door", next(s).getTitle());
		s.teller = true;
		s.valuables = 14;
		assertEquals("Find the house with a Locked Door", next(s).getTitle());
		s.valuables = 15;
		assertEquals(Step.Target.URCHIN, next(s).getTarget());
	}

	@Test
	public void inside()
	{
		HouseCycle.Snapshot s = at(60);
		s.inside = true;
		s.insideHouse = House.LAVINIA;
		s.leftMin = 100;
		s.leftMax = 110;
		assertEquals("Search a chest, wardrobe or jewellery case", next(s).getTitle());
		assertEquals(2, HouseCycle.next(s).stage);
		s.searching = true;
		assertEquals("Keep searching: valuables keep coming", next(s).getTitle());
		s.shine = true;
		assertEquals("Search where the flashing arrow points", next(s).getTitle());
		s.leftMin = 20;
		s.leftMax = 30;
		Step out = next(s);
		assertEquals("Leave by the open window now", out.getTitle());
		assertEquals(Step.Target.WINDOW, out.getTarget());
		assertEquals("Lavinia may be back in 20-30s: caught, you lose about 90%", out.getDetail());
		s.leftMin = 100;
		s.ownerBack = true;
		assertTrue(next(s).getDetail().startsWith("Lavinia is coming back"));
	}

	@Test
	public void aFullPackInside()
	{
		HouseCycle.Snapshot s = at(60);
		s.inside = true;
		s.free = 0;
		assertEquals("Free an inventory slot", next(s).getTitle());
		// A stack of valuables still grows in a full pack while a search runs.
		s.searching = true;
		assertEquals("Keep searching: valuables keep coming", next(s).getTitle());
	}

	@Test
	public void sellAndWait()
	{
		HouseCycle.Snapshot s = at(60);
		s.valuables = 200;
		s.oriana = true;
		s.rate = 65;
		Step step = next(s);
		assertEquals("Trade your valuables in to Oriana", step.getTitle());
		assertEquals("200 valuables at 65 coins: 13,000 coins", step.getDetail());
		s.oriana = false;
		s.nextMin = 5;
		s.nextMax = 20;
		assertEquals("Wait by the citizens: an urchin is due", next(s).getTitle());
		s.nextMin = 40;
		s.nextMax = 55;
		assertEquals("A key about 5 in 85; distraction in 40-55s", next(s).getDetail());
	}

	@Test
	public void ranges()
	{
		assertEquals("about 5s", HouseCycle.range(5, 5));
		assertEquals("up to 9s", HouseCycle.range(0, 9));
		assertEquals("3-8s", HouseCycle.range(3, 8));
	}
}
