package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Tithe;
import java.util.List;
import net.runelite.api.Varbits;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

/**
 * The Tithe Farm ids the Pilot reads, by their RuneLite names, against the numbers the OSRS wiki
 * gives in its infoboxes, and the varbits against RuneLite's own older names for them.
 */
public class TitheIdsTest
{
	@Test
	public void wikiIds()
	{
		// Blighted Golovanova seedling 27386, its later stages 27389, 27392 and 27394.
		assertEquals(27386, ObjectID.HOSIDIUS_TITHE_A_1_DEAD);
		assertEquals(27389, ObjectID.HOSIDIUS_TITHE_A_2_DEAD);
		assertEquals(27392, ObjectID.HOSIDIUS_TITHE_A_3_DEAD);
		assertEquals(27394, ObjectID.HOSIDIUS_TITHE_A_4_DEAD);
		// Water barrel "5598,29101,42073" and 5599, 8702, 8703.
		assertEquals(5598, TithePilot.BARRELS[0]);
		assertEquals(5599, TithePilot.BARRELS[1]);
		assertEquals(8702, TithePilot.BARRELS[2]);
		assertEquals(8703, TithePilot.BARRELS[3]);
		// Gricoller's can 13353; Golovanova seed 13423; Logavano fruit 13428.
		assertEquals(13353, TithePilot.GRICOLLER);
		assertEquals(13423, Tithe.Seed.GOLOVANOVA.seed());
		assertEquals(13428, Tithe.Seed.LOGAVANO.fruit());
		// Watering can(1) 5333 to Watering can(8) 5340.
		for (int d = 1; d <= 8; d++)
		{
			assertEquals(5332 + d, Tithe.CANS[d - 1]);
		}
		// Seed box 13639 (open 24482); herb sack 13226 (open 24478).
		assertEquals(13639, ItemID.SEED_BOX);
		assertEquals(24482, ItemID.SEED_BOX_OPEN);
		assertEquals(13226, ItemID.SLAYER_HERB_SACK);
		assertEquals(24478, ItemID.SLAYER_HERB_SACK_OPEN);
	}

	@Test
	public void varbitsAreRuneLitesTitheFarmOnes()
	{
		assertEquals(Varbits.TITHE_FARM_POINTS, TithePilot.POINTS);
		assertEquals(Varbits.TITHE_FARM_SACK_AMOUNT, TithePilot.SACK);
	}

	@Test
	public void watched()
	{
		for (int id : Tithe.patchIds())
		{
			assertTrue(TithePilot.isWatched(id));
		}
		assertTrue(TithePilot.isWatched(ObjectID.TITHE_PLANT_SEED_TABLE));
		assertTrue(TithePilot.isWatched(ObjectID.TITHE_SACK_OF_FRUIT));
		assertTrue(TithePilot.isWatched(ObjectID.HOSIDIUS_TITHE_FARM_DOOR));
		assertFalse(TithePilot.isWatched(ObjectID.TITHE_PLANT_CART));
	}

	@Test
	public void routeSurvivesASave()
	{
		List<int[]> r = TithePilot.decodeRoute("6,0;6,3;x;0,3;0,3;-1,2;0,0");
		assertEquals(4, r.size());
		assertEquals("6,0;6,3;0,3;0,0", TithePilot.encodeRoute(r));
		assertTrue(TithePilot.decodeRoute(null).isEmpty());
	}
}
