package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Altar;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

/**
 * The Guardians of the Rift ids the Pilot reads, by their RuneLite names, against the numbers the
 * Guardians of the Rift Helper and Optimizer plugins use (a cross-check: if RuneLite ever renumbers
 * a name, this says so before players do).
 */
public class RiftIdsTest
{
	@Test
	public void portalGuardiansMatchTheHelpersNumbers()
	{
		int[][] helper = {
			{Altar.AIR.ordinal(), 43701}, {Altar.MIND.ordinal(), 43705}, {Altar.WATER.ordinal(), 43702},
			{Altar.EARTH.ordinal(), 43703}, {Altar.FIRE.ordinal(), 43704}, {Altar.BODY.ordinal(), 43709},
			{Altar.COSMIC.ordinal(), 43710}, {Altar.CHAOS.ordinal(), 43706}, {Altar.NATURE.ordinal(), 43711},
			{Altar.LAW.ordinal(), 43712}, {Altar.DEATH.ordinal(), 43707}, {Altar.BLOOD.ordinal(), 43708},
		};
		for (int[] row : helper)
		{
			Altar a = Altar.values()[row[0]];
			assertEquals(a.name(), row[1], (int) RiftPilot.GUARDIANS.get(a));
		}
		assertEquals(12, RiftPilot.ALTARS.size());
		assertEquals(12, RiftPilot.EXITS.size());
	}

	@Test
	public void objectsNpcAndVarbits()
	{
		assertEquals(43696, ObjectID.GOTR_DEPOSITCHEST);
		assertEquals(43732, ObjectID.GOTR_UNCHARGED_CELLS);
		assertEquals(43729, ObjectID.GOTR_AGILITY_PORTAL_TOP);
		assertEquals(43740, ObjectID.GOTR_CELL_TILE_TIER1);
		assertEquals(43743, ObjectID.GOTR_CELL_TILE_TIER4);
		assertEquals(11403, NpcID.GOTR_GREAT_GUARDIAN);
		assertEquals(13686, VarbitID.GOTR_ELEMENTAL_EARNED_THIS_GAME);
		assertEquals(13685, VarbitID.GOTR_CATALYTIC_EARNED_THIS_GAME);
		// Law's altar is LAW_ALTAR (34767); the Optimizer's 34676 is a wall (MIMIC_WALL_WOOD_02).
		assertEquals(34767, ObjectID.LAW_ALTAR);
		assertTrue(RiftPilot.isWatched(ObjectID.LAW_ALTAR));
	}

	@Test
	public void hudComponentsAndRuneIcons()
	{
		// The Helper reads 48889858 (content), 48889876 (elemental) and 48889879 (catalytic).
		assertEquals(48889858, InterfaceID.GotrHud.CONTENT);
		assertEquals(48889876, InterfaceID.GotrHud.ELEMENTAL_PORTAL);
		assertEquals(48889879, InterfaceID.GotrHud.CATALYTIC_PORTAL);
		// Its rune icons: air 4353 to blood 4364.
		assertEquals(4353, SpriteID.IconRune32x32._0);
		assertEquals(4364, SpriteID.IconRune32x32._11);
		assertEquals(Altar.BLOOD, Altar.byIcon(SpriteID.IconRune32x32._11 - SpriteID.IconRune32x32._0));
	}

	@Test
	public void colossalPouchByLevel()
	{
		assertEquals(8, RiftPilot.Pouches.colossal(25));
		assertEquals(16, RiftPilot.Pouches.colossal(50));
		assertEquals(27, RiftPilot.Pouches.colossal(75));
		assertEquals(40, RiftPilot.Pouches.colossal(85));
	}
}
