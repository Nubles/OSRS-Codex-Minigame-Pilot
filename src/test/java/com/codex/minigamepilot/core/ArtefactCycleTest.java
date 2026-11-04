package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Artefacts.House;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class ArtefactCycleTest
{
	private static ArtefactCycle.Snapshot ready(House house)
	{
		ArtefactCycle.Snapshot s = new ArtefactCycle.Snapshot();
		s.level = 70;
		s.pick = true;
		s.house = house;
		s.task = house == null ? 0 : house.task();
		return s;
	}

	@Test
	public void needsTheLevel()
	{
		ArtefactCycle.Snapshot s = ready(null);
		s.level = 48;
		Step step = ArtefactCycle.next(s).step;
		assertEquals("Stealing artefacts needs 49 Thieving", step.getTitle());
		assertTrue(step.isWaiting());
	}

	@Test
	public void noTaskAsksKhaled()
	{
		ArtefactCycle.Decision d = ArtefactCycle.next(ready(null));
		assertEquals(Step.Target.KHALED, d.step.getTarget());
		assertEquals("Ask Captain Khaled for a house", d.step.getTitle());
		assertEquals(0, d.stage);
	}

	@Test
	public void bringsAPick()
	{
		ArtefactCycle.Snapshot s = ready(House.WEST);
		s.pick = false;
		Step step = ArtefactCycle.next(s).step;
		assertEquals("Bring a lockpick or hair clip", step.getTitle());
		assertTrue(step.hasItem(ItemID.LOCKPICK) && step.hasItem(ItemID.KR_HAIRCLIP));
	}

	@Test
	public void toTheHouseUpTheLadderAndTheDrawers()
	{
		ArtefactCycle.Snapshot s = ready(House.SOUTH_EAST);
		assertEquals("Go to the south-east house", ArtefactCycle.next(s).step.getTitle());
		assertEquals(Step.Target.HOUSE, ArtefactCycle.next(s).step.getTarget());
		s.ladderUp = true;
		assertEquals("Climb the ladder in the south-east house", ArtefactCycle.next(s).step.getTitle());
		s.plane = 1;
		s.ladderUp = false;
		assertEquals("Climb down: the drawers are in the south-east house", ArtefactCycle.next(s).step.getTitle());
		s.drawers = true;
		ArtefactCycle.Decision d = ArtefactCycle.next(s);
		assertEquals(Step.Target.DRAWERS, d.step.getTarget());
		assertEquals("Pick-lock", d.step.getVerb());
		assertEquals(2, d.stage);
	}

	@Test
	public void northHouseIsOnTheGroundFloor()
	{
		ArtefactCycle.Snapshot s = ready(House.NORTH);
		s.ladderUp = true;
		assertEquals("Go to the north house", ArtefactCycle.next(s).step.getTitle());
		assertTrue(ArtefactCycle.next(s).step.getDetail().contains("ground floor"));
	}

	@Test
	public void carryingGoesDownThenToKhaledOnFoot()
	{
		ArtefactCycle.Snapshot s = ready(House.SOUTH);
		s.carrying = true;
		s.plane = 1;
		s.task = Artefacts.TASK_DELIVER;
		assertEquals("Climb down the ladder", ArtefactCycle.next(s).step.getTitle());
		s.plane = 0;
		s.teleport = true;
		ArtefactCycle.Decision d = ArtefactCycle.next(s);
		assertEquals(Step.Target.KHALED, d.step.getTarget());
		assertEquals(3, d.stage);
		assertTrue(d.step.getDetail().contains("No teleport until he has it"));
		s.patrolsNear = 2;
		s.nearestPatrol = 3;
		s.patrolFacing = true;
		assertTrue(ArtefactCycle.next(s).step.getDetail().startsWith("A patrol 3 tiles off faces you: wait for it to turn"));
		s.patrolFacing = false;
		assertTrue(ArtefactCycle.next(s).step.getDetail().startsWith("Keep clear of the 2 patrols nearby"));
	}

	@Test
	public void failedOrLostGoesBackForANewHouse()
	{
		ArtefactCycle.Snapshot s = ready(null);
		s.task = Artefacts.TASK_FAILED;
		assertEquals("Back to Captain Khaled for a new house", ArtefactCycle.next(s).step.getTitle());
		s.task = Artefacts.TASK_DELIVER;
		assertTrue(ArtefactCycle.next(s).step.getDetail().startsWith("No artefact in your pack"));
	}

	@Test
	public void khaledsWordsAreSaid()
	{
		ArtefactCycle.Snapshot s = ready(House.WEST);
		s.heardOther = true;
		assertTrue(ArtefactCycle.next(s).step.getDetail().endsWith("(as Captain Khaled said)"));
	}
}
