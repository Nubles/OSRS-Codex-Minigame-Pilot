package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RiftCycleTest
{
	private static RiftCycle.Snapshot started()
	{
		RiftCycle.Snapshot s = new RiftCycle.Snapshot();
		s.started = true;
		s.free = 26;
		s.unchargedCells = 1;
		return s;
	}

	@Test
	public void minesToTheFirstTargetThenCrafts()
	{
		RiftCycle.Snapshot s = started();
		s.fragments = 100;
		assertEquals(Step.Target.REMAINS, RiftCycle.next(s).getTarget());
		assertTrue(RiftCycle.next(s).getDetail().startsWith("20 more to 120"));
		s.fragments = 120;
		assertEquals(Step.Target.WORKBENCH, RiftCycle.next(s).getTarget());
	}

	@Test
	public void afterTheFirstCraftATripsWorthIsEnough()
	{
		RiftCycle.Snapshot s = started();
		s.craftedThisGame = true;
		s.pouchRoom = 12;
		s.fragments = 37;
		assertEquals(38, RiftCycle.target(s));
		assertEquals(Step.Target.REMAINS, RiftCycle.next(s).getTarget());
		s.fragments = 38;
		assertEquals(Step.Target.WORKBENCH, RiftCycle.next(s).getTarget());
	}

	@Test
	public void aFullPackFillsThePouchesThenGoesToTheAltar()
	{
		RiftCycle.Snapshot s = started();
		s.free = 0;
		s.essence = 25;
		s.pouchRoom = 12;
		s.pouches = new int[]{5509};
		s.fragments = 40;
		Step fill = RiftCycle.next(s);
		assertEquals("Fill your pouches", fill.getTitle());
		assertTrue(fill.hasItem(5509));
		s.pouchRoom = 0;
		s.pouchStored = 12;
		s.pick = Altar.NATURE;
		s.pickWhy = "why";
		Step enter = RiftCycle.next(s);
		assertEquals(Step.Target.GUARDIAN, enter.getTarget());
		assertEquals("Enter the nature altar", enter.getTitle());
		s.pick = null;
		assertTrue(RiftCycle.next(s).isWaiting());
	}

	@Test
	public void handInOrder()
	{
		RiftCycle.Snapshot s = started();
		s.stones = 20;
		s.chargedCell = Altar.Cell.STRONG;
		s.runes = true;
		assertEquals(Step.Target.GREAT_GUARDIAN, RiftCycle.next(s).getTarget());
		s.stones = 0;
		assertEquals("Place the strong cell", RiftCycle.next(s).getTitle());
		s.chargedCell = null;
		assertEquals(Step.Target.DEPOSIT_POOL, RiftCycle.next(s).getTarget());
	}

	@Test
	public void insideTheAltar()
	{
		RiftCycle.Snapshot s = started();
		s.inAltar = Altar.BLOOD;
		s.essence = 20;
		assertEquals("Craft blood runes", RiftCycle.next(s).getTitle());
		s.essence = 0;
		s.pouchStored = 12;
		s.pouches = new int[]{5509};
		assertEquals("Empty your pouches", RiftCycle.next(s).getTitle());
		s.pouchStored = 0;
		assertEquals(Step.Target.EXIT, RiftCycle.next(s).getTarget());
		assertEquals(2, RiftCycle.stage(RiftCycle.next(s), true));
	}

	@Test
	public void theHugeRemainsWhenTheirPortalIsOpen()
	{
		RiftCycle.Snapshot s = started();
		s.craftedThisGame = true;
		s.riftPortalOpen = true;
		s.fragments = 5;
		assertEquals(Step.Target.RIFT_PORTAL, RiftCycle.next(s).getTarget());
		// Enough fragments for the trip: no need to go.
		s.fragments = 30;
		assertEquals(Step.Target.WORKBENCH, RiftCycle.next(s).getTarget());
		RiftCycle.Snapshot shelf = started();
		shelf.atHugeRemains = true;
		assertEquals(Step.Target.HUGE_REMAINS, RiftCycle.next(shelf).getTarget());
		shelf.free = 0;
		shelf.essence = 26;
		assertEquals(Step.Target.RIFT_PORTAL, RiftCycle.next(shelf).getTarget());
	}

	@Test
	public void beforeTheRiftOpens()
	{
		RiftCycle.Snapshot s = new RiftCycle.Snapshot();
		s.free = 27;
		assertEquals(Step.Target.CELLS, RiftCycle.next(s).getTarget());
		s.unchargedCells = 1;
		assertTrue(RiftCycle.next(s).isWaiting());
	}
}
