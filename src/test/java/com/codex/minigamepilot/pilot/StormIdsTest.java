package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

/**
 * The Tempoross ids the Pilot reads, by their RuneLite names, against the numbers the OSRS wiki
 * gives (Spirit pool, Ammunition crate, Totem pole, Raw harpoonfish), so a renumbered name shows
 * up here before it does in a game.
 */
public class StormIdsTest
{
	@Test
	public void matchTheWiki()
	{
		// Spirit pool: NPC ID 10571.
		assertEquals(10571, StormPilot.SPIRIT_POOL);
		// Ammunition crate: NPC IDs 10576-10579.
		assertEquals(4, StormPilot.CRATES.length);
		for (int i = 0; i < 4; i++)
		{
			assertEquals(10576 + i, StormPilot.CRATES[i]);
		}
		// Totem pole: object IDs 41012 and 41233, both tethering sites.
		assertTrue(StormPilot.contains(StormPilot.TETHERS, 41012));
		assertTrue(StormPilot.contains(StormPilot.TETHERS, 41233));
		// Raw harpoonfish: item 25564.
		assertEquals(25564, ItemID.TEMPOROSS_RAW_HARPOONFISH);
	}

	@Test
	public void crossChecks()
	{
		// The double spot and the fire, as the Easy Tempoross plugin reads them.
		assertEquals(10569, StormPilot.DOUBLE_SPOT);
		assertEquals(8643, StormPilot.FIRE);
		// The tether sites as the game spawns them, as the Tempoross Wave Timer plugin reads them.
		for (int id : new int[]{41352, 41353, 41354, 41355})
		{
			assertTrue(id + "", StormPilot.contains(StormPilot.TETHERS, id));
		}
		assertEquals(41236, StormPilot.SHRINE);
		assertEquals(11895, VarbitID.TEMPOROSS_TETHERED);
		assertEquals(11936, VarbitID.TEMPOROSS_REWARDPERMITS);
		// Broken ones are never offered as a tether.
		for (int id : StormPilot.BROKEN)
		{
			assertTrue(StormPilot.contains(StormPilot.TETHERS, id));
		}
		assertTrue(StormPilot.isWatched(ObjectID.TEMPOROSS_CRATE_ROPE));
	}
}
