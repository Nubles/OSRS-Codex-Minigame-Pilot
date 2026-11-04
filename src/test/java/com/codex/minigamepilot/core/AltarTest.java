package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** The altar table against the OSRS wiki's Guardians of the Rift "Runic altars" tables. */
public class AltarTest
{
	@Test
	public void levelsAlignmentsCellsAndQuestsAreTheWikis()
	{
		Object[][] wiki = {
			{Altar.AIR, 1, Altar.Alignment.ELEMENTAL, Altar.Cell.WEAK, null},
			{Altar.WATER, 5, Altar.Alignment.ELEMENTAL, Altar.Cell.MEDIUM, null},
			{Altar.EARTH, 9, Altar.Alignment.ELEMENTAL, Altar.Cell.STRONG, null},
			{Altar.FIRE, 14, Altar.Alignment.ELEMENTAL, Altar.Cell.OVERCHARGED, null},
			{Altar.MIND, 2, Altar.Alignment.CATALYTIC, Altar.Cell.WEAK, null},
			{Altar.BODY, 20, Altar.Alignment.CATALYTIC, Altar.Cell.WEAK, null},
			{Altar.COSMIC, 27, Altar.Alignment.CATALYTIC, Altar.Cell.MEDIUM, "Lost City"},
			{Altar.CHAOS, 35, Altar.Alignment.CATALYTIC, Altar.Cell.MEDIUM, null},
			{Altar.NATURE, 44, Altar.Alignment.CATALYTIC, Altar.Cell.STRONG, null},
			{Altar.LAW, 54, Altar.Alignment.CATALYTIC, Altar.Cell.STRONG, "Troll Stronghold"},
			{Altar.DEATH, 65, Altar.Alignment.CATALYTIC, Altar.Cell.OVERCHARGED, "Mourning's End Part II"},
			{Altar.BLOOD, 77, Altar.Alignment.CATALYTIC, Altar.Cell.OVERCHARGED, "Sins of the Father"},
		};
		assertEquals(wiki.length, Altar.values().length);
		for (Object[] row : wiki)
		{
			Altar a = (Altar) row[0];
			assertEquals(a.name(), row[1], a.level());
			assertEquals(a.name(), row[2], a.alignment());
			assertEquals(a.name(), row[3], a.cell());
			assertEquals(a.name(), row[4], a.quest());
		}
	}

	@Test
	public void runeIconsRunAirToBlood()
	{
		assertEquals(Altar.AIR, Altar.byIcon(0));
		assertEquals(Altar.BLOOD, Altar.byIcon(11));
		assertNull(Altar.byIcon(-1));
		assertNull(Altar.byIcon(12));
	}
}
