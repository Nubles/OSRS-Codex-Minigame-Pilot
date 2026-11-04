package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Houses;
import com.codex.minigamepilot.core.Houses.House;
import com.codex.minigamepilot.core.SavedKeysFuzzTest;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

/** The stealing-valuables ids the Pilot reads, by their RuneLite names, against the numbers the OSRS wiki gives. */
public class HousesIdsTest
{
	@Test
	public void wikiIds()
	{
		// House keys: "Item ID" 29325; Valuables: 29332.
		assertEquals(29325, Houses.KEY);
		assertEquals(29332, Houses.VALUABLES);
		// Locked Door (stealing valuables): 51998; Window (stealing valuables): 52000.
		assertEquals(51998, Houses.DOOR_LOCKED);
		assertEquals(52000, Houses.WINDOW_OPEN);
		// Chest 52008, Wardrobe 52010, Jewellery case 52011 (stealing valuables).
		assertTrue(Houses.isLoot(52008));
		assertTrue(Houses.isLoot(52010));
		assertTrue(Houses.isLoot(52011));
		// Wealthy citizen 13302-13305; Leo 13306, Julia 13307, Aurelia 13308; Oriana 13311.
		for (int id = 13302; id <= 13305; id++)
		{
			assertTrue(Houses.isCitizen(id));
		}
		assertTrue(Houses.isDistracter(13306));
		assertTrue(Houses.isDistracter(13307));
		assertTrue(Houses.isDistracter(13308));
		assertTrue(Houses.isOriana(13311));
		// Lavinia 13312, Victor (Civitas illa Fortis) 13313, Caius 13314.
		assertEquals(13312, House.LAVINIA.npc());
		assertEquals(13313, House.VICTOR.npc());
		assertEquals(13314, House.CAIUS.npc());
	}

	@Test
	public void watched()
	{
		assertTrue(HousesPilot.isWatched(Houses.DOOR_LOCKED));
		assertTrue(HousesPilot.isWatched(Houses.DOOR));
		assertTrue(HousesPilot.isWatched(Houses.WINDOW_MULTI));
		for (int id : Houses.LOOT)
		{
			assertTrue(HousesPilot.isWatched(id));
		}
		// The glass case is not one the wiki names as searchable.
		assertFalse(HousesPilot.isWatched(net.runelite.api.gameval.ObjectID.VARLAMORE_THIEVING_HOUSE_GLASS_CASE));
	}

	@Test
	public void messagesAreTheWikis()
	{
		assertEquals("You notice an urchin distract a wealthy citizen nearby.", Houses.DISTRACTED);
		assertEquals("You hear someone outside spot the homeowner coming back!", Houses.OWNER_BACK);
		assertEquals("You notice the owner is still near the door and decide not to enter.", Houses.STILL_NEAR);
		assertEquals("You are escorted out of the house as the owner has locked the door.", Houses.ESCORTED);
		assertEquals("You notice something shine somewhere else in the house.", Houses.SHINE);
	}

	@Test
	public void placesSurviveASave()
	{
		Map<House, WorldPoint> m = new EnumMap<>(House.class);
		m.put(House.LAVINIA, new WorldPoint(1660, 3090, 0));
		m.put(House.CAIUS, new WorldPoint(1630, 3125, 0));
		String text = HousesPilot.encodePlaces(m, new WorldPoint(1640, 3108, 0));
		Map<House, WorldPoint> back = new EnumMap<>(House.class);
		assertEquals(new WorldPoint(1640, 3108, 0), HousesPilot.decodePlaces(text, back));
		assertEquals(m, back);
		back.clear();
		assertNull(HousesPilot.decodePlaces("1;LAVINIA:1,2;NOBODY:1660,3090,0;VICTOR:-5,3080,0;CAIUS:1630,3125,0", back));
		assertEquals(1, back.size());
		for (String saved : SavedKeysFuzzTest.damaged(text))
		{
			HousesPilot.decodePlaces(saved, new EnumMap<>(House.class));
		}
	}

	@Test
	public void rateSurvivesASave()
	{
		assertEquals(65, HousesPilot.parseRate("65"));
		assertEquals(55, HousesPilot.parseRate("55"));
		for (String saved : SavedKeysFuzzTest.damaged("65"))
		{
			int r = HousesPilot.parseRate(saved);
			assertTrue(r == 55 || r == 65);
		}
	}
}
