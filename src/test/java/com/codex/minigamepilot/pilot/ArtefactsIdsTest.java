package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Artefacts;
import com.codex.minigamepilot.core.Artefacts.House;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

/**
 * The stealing-artefacts ids the Pilot reads, by their RuneLite names, against the numbers the OSRS
 * wiki gives in its infoboxes, and the varbit against the Stealing Artefacts plugin's.
 */
public class ArtefactsIdsTest
{
	@Test
	public void wikiIds()
	{
		// Drawers (stealing artefacts): "27771, 27772, 27774, 27776" and "27773, 27775".
		assertEquals(27771, House.NORTH.drawers());
		assertEquals(27772, House.SOUTH_EAST.drawers());
		assertEquals(27773, House.SOUTH.drawers());
		assertEquals(27774, House.SOUTH_WEST.drawers());
		assertEquals(27775, House.WEST.drawers());
		assertEquals(27776, House.NORTH_WEST.drawers());
		// Stolen pendant: item ID 13434.
		assertEquals(13434, ItemID.PISCARILIUS_STOLEN_PENDANT);
		assertEquals(1523, ItemID.LOCKPICK);
	}

	/** The Stealing Artefacts plugin's STEALING_ARTEFACTS_VARBIT 4903 and patrol ids 6973 to 6980: a cross-check. */
	@Test
	public void crossCheckedWithTheStealingArtefactsPlugin()
	{
		assertEquals(4903, ArtefactsPilot.TASK);
		for (int id = 6973; id <= 6980; id++)
		{
			assertTrue(Artefacts.isPatrol(id));
		}
	}

	@Test
	public void watched()
	{
		for (House h : House.values())
		{
			assertTrue(ArtefactsPilot.isWatched(h.drawers()));
		}
		assertTrue(ArtefactsPilot.isWatched(ObjectID.PISCARILIUS_LADDER_BOTTOM));
		assertTrue(ArtefactsPilot.isWatched(ObjectID.PISCARILIUS_LADDER_TOP_01));
		assertFalse(ArtefactsPilot.isWatched(ObjectID.PISCARILIUS_DRAWERS_SMALL));
	}

	@Test
	public void facing()
	{
		// 0 faces south, 512 west, 1024 north, 1536 east.
		assertTrue(ArtefactsPilot.facing(0, 0, -3));
		assertFalse(ArtefactsPilot.facing(0, 0, 3));
		assertTrue(ArtefactsPilot.facing(512, -2, 0));
		assertTrue(ArtefactsPilot.facing(1024, 1, 1));
		assertFalse(ArtefactsPilot.facing(1536, -1, 0));
		assertTrue(ArtefactsPilot.facing(1536, 4, 1));
	}

	@Test
	public void placesSurviveASave()
	{
		Map<House, WorldPoint> m = new EnumMap<>(House.class);
		m.put(House.WEST, new WorldPoint(1750, 3749, 1));
		String text = ArtefactsPilot.encodePlaces(m, new WorldPoint(1840, 3740, 0));
		Map<House, WorldPoint> back = new EnumMap<>(House.class);
		WorldPoint k = ArtefactsPilot.decodePlaces(text, back);
		assertEquals(new WorldPoint(1840, 3740, 0), k);
		assertEquals(m, back);
		// Damaged parts and places outside the port are left out.
		back.clear();
		assertNull(ArtefactsPilot.decodePlaces("1;WEST:1,2,0;NOWHERE:1750,3749,0;SOUTH:1765,3732;NORTH:1768,3750,0", back));
		assertEquals(1, back.size());
		assertNull(ArtefactsPilot.decodePlaces(null, back));
	}
}
