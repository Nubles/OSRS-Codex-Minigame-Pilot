package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Artefacts.House;
import org.junit.Test;

/** The facts of stealing artefacts against the OSRS wiki's own words and table. */
public class ArtefactsTest
{
	@Test
	public void wikiFigures()
	{
		// "requiring level 49 Thieving to participate"
		assertEquals(49, Artefacts.LEVEL);
		// "Successfully picking the lock grants 750 Thieving experience."
		assertEquals(750, Artefacts.LOCKPICK_XP);
		// "equal to 40 times the current Thieving level", plus "500–1,000 coins"
		assertEquals(2800, Artefacts.deliveryXp(70));
		assertEquals(500, Artefacts.COINS_MIN);
		assertEquals(1000, Artefacts.COINS_MAX);
	}

	/** The wiki's Base XP/HR column is 48 artefacts an hour, each 750 and 40 times the level. */
	@Test
	public void wikiXpTable()
	{
		int[][] table = {{49, 130_080}, {55, 141_600}, {60, 151_200}, {65, 160_800}, {70, 170_400}, {75, 180_000},
			{80, 189_600}, {85, 199_200}, {90, 208_800}, {95, 218_400}, {99, 226_080}};
		for (int[] row : table)
		{
			assertEquals("level " + row[0], row[1], Artefacts.wikiXpPerHour(row[0], false));
		}
		// The teleport column is rounded to the thousand: 150,000 at 49, 261,000 at 99, at about 55 an hour.
		int[][] teleport = {{49, 150_000}, {70, 197_000}, {99, 261_000}};
		for (int[] row : teleport)
		{
			assertTrue("level " + row[0], Math.abs(Artefacts.wikiXpPerHour(row[0], true) - row[1]) <= row[1] / 100);
		}
	}

	/** "The artefact is located in a drawer on the ground floor in the northern house, and upstairs in the others." */
	@Test
	public void onlyTheNorthHouseIsOnTheGroundFloor()
	{
		for (House h : House.values())
		{
			assertEquals(h.name(), h != House.NORTH, h.upstairs());
		}
	}

	/** The wiki's average time per house: North 1:00, South-east 1:05, South 1:10, West 1:20, North-west 1:25, South-west 1:30. */
	@Test
	public void wikiTimes()
	{
		assertEquals(60, House.NORTH.wikiSeconds());
		assertEquals(65, House.SOUTH_EAST.wikiSeconds());
		assertEquals(70, House.SOUTH.wikiSeconds());
		assertEquals(80, House.WEST.wikiSeconds());
		assertEquals(85, House.NORTH_WEST.wikiSeconds());
		assertEquals(90, House.SOUTH_WEST.wikiSeconds());
		for (House h : House.values())
		{
			assertEquals(h.name(), h.wikiSeconds() - 10, h.wikiSecondsTeleport());
		}
	}

	@Test
	public void tasksAndDrawers()
	{
		for (House h : House.values())
		{
			assertEquals(h, House.byTask(h.task()));
			assertEquals(h, House.byDrawers(h.drawers()));
			assertTrue(Artefacts.inPort(h.x(), h.y()));
		}
		assertNull(House.byTask(Artefacts.TASK_NONE));
		assertNull(House.byTask(Artefacts.TASK_FAILED));
		assertNull(House.byTask(Artefacts.TASK_DELIVER));
	}

	/** Captain Khaled: "It can be found in the [location]." */
	@Test
	public void heardFromKhaled()
	{
		assertEquals(House.NORTH_WEST, House.heard("You need to recover an artefact for me. It can be found in the North West House."));
		assertEquals(House.NORTH_WEST, House.heard("It can be found in the north-western house."));
		assertEquals(House.SOUTH_EAST, House.heard("It can be found in the South-east house."));
		assertEquals(House.SOUTH_WEST, House.heard("It can be found in the southwest house."));
		assertEquals(House.NORTH, House.heard("It can be found in the Northern house."));
		assertEquals(House.SOUTH, House.heard("It can be found in the South House."));
		assertEquals(House.WEST, House.heard("It can be found in the western house."));
		assertNull(House.heard("Here's your cut."));
		assertNull(House.heard(null));
	}

	@Test
	public void ids()
	{
		assertTrue(Artefacts.isArtefact(13434));
		assertFalse(Artefacts.isArtefact(995));
		assertEquals(5, Artefacts.ITEMS.length);
		assertEquals(8, Artefacts.PATROLS.length);
	}
}
