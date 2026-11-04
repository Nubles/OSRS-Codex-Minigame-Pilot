package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Totems;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

/**
 * The Vale Totems ids the Pilot reads, by their RuneLite names, against the numbers the OSRS wiki
 * gives (object, item and NPC ids in its infoboxes), and every site's varbits against the order
 * the Pilot reads them in, so a renumbered name shows up here before it does in the valley.
 */
public class TotemsIdsTest
{
	@Test
	public void wikiIds()
	{
		// Totem site 57064, Oak totem 57065, Redwood totem 57091 (wiki infoboxes).
		assertEquals(57064, ObjectID.ENT_TOTEMS_BASE_NONE);
		assertEquals(57065, ObjectID.ENT_TOTEMS_CARVED_BASE_OAK);
		assertEquals(57091, ObjectID.ENT_TOTEMS_CARVED_BASE_REDWOOD);
		// Offering site 57095 to 57099, Ent trail 57115.
		assertEquals(57095, ObjectID.ENT_TOTEMS_OFFERINGS_NONE);
		assertEquals(57099, ObjectID.ENT_TOTEMS_OFFERINGS_D);
		assertEquals(57115, TotemsPilot.TRAILS[0]);
		// Vale offerings 31054.
		assertEquals(31054, TotemsPilot.VALE_OFFERINGS);
		// Spirits: buffalo 14584/14589, jaguar 14585/14590, eagle 14586/14591, snake 14587/14592,
		// scorpion 14588/14593.
		assertEquals(14589, NpcID.ENT_TOTEMS_ANIMAL_A);
		assertEquals(14590, NpcID.ENT_TOTEMS_ANIMAL_B);
		assertEquals(14591, NpcID.ENT_TOTEMS_ANIMAL_C);
		assertEquals(14592, NpcID.ENT_TOTEMS_ANIMAL_D);
		assertEquals(14593, NpcID.ENT_TOTEMS_ANIMAL_E);
		assertEquals(14584, NpcID.ENT_TOTEMS_ANIMAL_A_CS);
		assertEquals(14588, NpcID.ENT_TOTEMS_ANIMAL_E_CS);
		assertEquals('A', Totems.Animal.BUFFALO.letter());
		assertEquals('E', Totems.Animal.SCORPION.letter());
		assertEquals(ItemID.ENT_TOTEMS_ANIMAL_ICON_C, Totems.Animal.EAGLE.icon());
	}

	@Test
	public void everySiteReadsTheSameWay() throws Exception
	{
		String[] parts = {"BASE", "BASE_CARVED", "LOW", "MID", "TOP", "DECORATIONS", "ANIMAL_1", "ANIMAL_2", "ANIMAL_3", "POINTS"};
		int[] offsets = {TotemsPilot.BASE, TotemsPilot.BASE_CARVED, TotemsPilot.LOW, TotemsPilot.MID, TotemsPilot.TOP,
			TotemsPilot.DECORATIONS, TotemsPilot.ANIMAL_1, TotemsPilot.ANIMAL_1 + 1, TotemsPilot.ANIMAL_1 + 2, TotemsPilot.POINTS};
		for (int site = 1; site <= Totems.SITES; site++)
		{
			for (int k = 0; k < parts.length; k++)
			{
				int named = VarbitID.class.getField("ENT_TOTEMS_SITE_" + site + "_" + parts[k]).getInt(null);
				assertEquals("site " + site + " " + parts[k], named, TotemsPilot.SITE_VARBITS[site - 1] + offsets[k]);
			}
			// ENT_TOTEMS_SITE_1_BASE is 57016 and _OFFERINGS 57020; each site's six objects follow the last's.
			assertEquals(57016 + 6 * (site - 1), TotemsPilot.BASES[site - 1]);
			assertEquals(57020 + 6 * (site - 1), TotemsPilot.OFFERINGS[site - 1]);
			assertTrue(TotemsPilot.isWatched(TotemsPilot.BASES[site - 1]));
			assertTrue(TotemsPilot.isWatched(TotemsPilot.OFFERINGS[site - 1]));
		}
		assertTrue(TotemsPilot.isWatched(ObjectID.ENT_TOTEMS_TRAIL_PART_1));
		assertFalse(TotemsPilot.isWatched(ObjectID.ENT_TOTEMS_BASE_NONE));
	}

	@Test
	public void woodFromTheTotemShown()
	{
		assertEquals(Totems.Wood.OAK, TotemsPilot.woodOf(ObjectID.ENT_TOTEMS_CARVED_BASE_OAK));
		assertEquals(Totems.Wood.MAPLE, TotemsPilot.woodOf(ObjectID.ENT_TOTEMS_TOP_MAPLE));
		assertEquals(Totems.Wood.REDWOOD, TotemsPilot.woodOf(ObjectID.ENT_TOTEMS_BASE_REDWOOD));
		assertNull(TotemsPilot.woodOf(ObjectID.ENT_TOTEMS_BASE_NONE));
	}

	@Test
	public void placesAndLoopSurviveASave()
	{
		WorldPoint[] where = new WorldPoint[Totems.SITES];
		where[2] = new WorldPoint(1400, 3330, 0);
		String text = TotemsPilot.encodePlaces(where);
		assertEquals("-;-;1400,3330,0;-;-;-;-;-", text);
		assertEquals(where[2], TotemsPilot.decodePlaces(text)[2]);
		assertNull(TotemsPilot.decodePlaces("x;;1,2;9,9,9")[0]);
		int[] loop = TotemsPilot.decodeLoop("2,3,-1,9,5,x");
		assertEquals(1, loop[0]);
		assertEquals(2, loop[1]);
		assertEquals(-1, loop[3]);
		// A site is never its own next.
		assertEquals(-1, loop[4]);
		assertEquals("2,3,-1,-1,-1,-1,-1,-1", TotemsPilot.encodeLoop(loop));
	}
}
