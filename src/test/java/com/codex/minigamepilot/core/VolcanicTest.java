package com.codex.minigamepilot.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The Volcanic Mine's facts against the wiki's pages, and its ids against the wiki's infoboxes and RuneLite's gameval. */
public class VolcanicTest
{
	@Test
	public void rewardShopPrices()
	{
		// Prospector helmet "from the Volcanic Mine for 26,000 points"; jacket "39,000 points";
		// legs "34,000 points"; boots "21,000 points".
		assertEquals(26_000, Volcanic.Piece.HELMET.cost());
		assertEquals(39_000, Volcanic.Piece.JACKET.cost());
		assertEquals(34_000, Volcanic.Piece.LEGS.cost());
		assertEquals(21_000, Volcanic.Piece.BOOTS.cost());
		assertEquals(120_000, Volcanic.Reward.PROSPECTOR_KIT.cost());
		assertEquals(26_000, Volcanic.Reward.PROSPECTOR_HELMET.cost());
		// Ash covered tome "40,000 points"; the larger canteen "for 10,000 points"; ore packs "for 4,000 points".
		assertEquals(40_000, Volcanic.Reward.ASH_COVERED_TOME.cost());
		assertEquals(10_000, Volcanic.Reward.LARGE_WATER_CONTAINER.cost());
		assertEquals(4_000, Volcanic.Reward.ORE_PACK.cost());
		assertTrue(Volcanic.Reward.ORE_PACK.repeat());
		assertFalse(Volcanic.Reward.ASH_COVERED_TOME.repeat());
		assertEquals(2_097_151, Volcanic.POINTS_CAP);
	}

	@Test
	public void entry()
	{
		// "To enter the mine, players must have level 50 Mining."; "30 numulite each time";
		// "A one-time fee of 3,000 numulite can also be paid for permanent access."
		assertEquals(50, Volcanic.MINING_LEVEL);
		assertEquals(30, Volcanic.ENTRY_FEE);
		assertEquals(3_000, Volcanic.PERMANENT_FEE);
		assertEquals(600, Volcanic.GAME_SECONDS);
		assertEquals("Oh dear, you are dead!", Volcanic.DEATH);
	}

	@Test
	public void wikiIds()
	{
		// Petrified Pete 7776; Prospector helmet 12013, jacket 12014, legs 12015, boots 12016;
		// Ash covered tome 21697; Ore pack (Volcanic Mine) 27693.
		assertEquals(7776, Volcanic.PETE);
		assertEquals(12013, Volcanic.Piece.HELMET.items()[0]);
		assertEquals(12014, Volcanic.Piece.JACKET.items()[0]);
		assertEquals(12015, Volcanic.Piece.LEGS.items()[0]);
		assertEquals(12016, Volcanic.Piece.BOOTS.items()[0]);
		assertEquals(21697, net.runelite.api.gameval.ItemID.FOSSIL_MINE_ULTRASOIL_BOOK);
		assertEquals(27693, net.runelite.api.gameval.ItemID.WBR_VM_OREPACK);
		// The Volcanic Mine's own kit (29472...) and the golden kit (25549...) count as owned too.
		assertArrayEquals(new int[]{12013, 29472, 25549}, Volcanic.Piece.HELMET.items());
		assertArrayEquals(new int[]{12016, 29478, 25555}, Volcanic.Piece.BOOTS.items());
	}

	@Test
	public void runeliteIds()
	{
		// RuneLite's Discord plugin: MG_VOLCANIC_MINE(..., 15263, 15262).
		assertTrue(Volcanic.inMine(15263));
		assertTrue(Volcanic.inMine(15262));
		assertFalse(Volcanic.inMine(15163));
		// RuneLite's world map: MinigameLocation.VOLCANIC_MINE at 3812, 3810.
		assertEquals(3812, Volcanic.LOBBY_X);
		assertEquals(3810, Volcanic.LOBBY_Y);
		// gameval: FOSSIL_MINE_TEMP_PTS 5934, _CURRENT_STABILITY 5938, _VENTA/B/C_STATUS 5939, 5940, 5942,
		// _PROGRESS_STATUS 5941, _NUMBERPLAYERS 5937.
		assertEquals(5934, Volcanic.GAME_POINTS);
		assertEquals(5938, Volcanic.STABILITY);
		assertArrayEquals(new int[]{5939, 5940, 5942}, Volcanic.VENTS);
		assertEquals(5941, Volcanic.PROGRESS);
		assertEquals(5937, Volcanic.PLAYERS);
		// Interface 611 (FOSSIL_VOLCANIC_MINE) component 7, and the shop 612 component 5.
		assertEquals((611 << 16) | 7, Volcanic.HUD_TIME);
		assertEquals((612 << 16) | 5, Volcanic.SHOP_POINTS);
	}

	@Test
	public void readsTheGamesText()
	{
		assertEquals(581, Volcanic.seconds("9:41"));
		assertEquals(5, Volcanic.seconds("<col=ff0000>0:05</col>"));
		assertEquals(-1, Volcanic.seconds(null));
		assertEquals(-1, Volcanic.seconds("Time"));
		assertEquals(12_345, Volcanic.shopPoints("Points: <col=ffa82f>12,345</col>"));
		assertEquals(0, Volcanic.shopPoints("Points: 0"));
		assertEquals(-1, Volcanic.shopPoints("Points:"));
		assertEquals(-1, Volcanic.shopPoints("Points: 99999999999"));
	}
}
