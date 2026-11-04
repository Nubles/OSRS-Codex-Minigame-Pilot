package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Wintertodt;
import com.codex.minigamepilot.core.Wintertodt.Piece;
import com.codex.minigamepilot.core.Wintertodt.Reward;
import net.runelite.api.gameval.InterfaceID;
import org.junit.Test;

/** The Wintertodt ids the Pilot reads, against the wiki's infoboxes and RuneLite's gameval and plugin. */
public class WintertodtIdsTest
{
	@Test
	public void wikiIds()
	{
		// Brazier: 29312 unlit, 29313 broken, 29314 burning. Bruma roots 29311. Reward Cart 55411 to 55416.
		assertEquals(29312, Wintertodt.BRAZIER_UNLIT);
		assertEquals(29313, Wintertodt.BRAZIER_BROKEN);
		assertEquals(29314, Wintertodt.BRAZIER_LIT);
		assertEquals(29311, Wintertodt.ROOTS);
		assertArrayEquals(new int[]{55411, 55412, 55413, 55414, 55415, 55416}, Wintertodt.CART);
		// Pyromancer 7371, incapacitated 7372.
		assertEquals(7371, Wintertodt.PYROMANCER);
		assertEquals(7372, Wintertodt.PYROMANCER_DOWN);
		// Bruma root 20695, kindling 20696; rejuvenation potion (4) to (1) 20699 to 20702.
		assertEquals(20695, Wintertodt.ROOT);
		assertEquals(20696, Wintertodt.KINDLING);
		assertArrayEquals(new int[]{20699, 20700, 20701, 20702}, Wintertodt.POTIONS);
		// Pyromancer garb 20704, hood 20708, robe 20706, boots 20710.
		assertEquals(20704, Piece.GARB.item());
		assertEquals(20708, Piece.HOOD.item());
		assertEquals(20706, Piece.ROBE.item());
		assertEquals(20710, Piece.BOOTS.item());
		// Warm gloves 20712; bruma torch 20720; tome of fire 20714, empty 20716; phoenix 20693.
		assertEquals(20712, Reward.WARM_GLOVES.items()[0]);
		assertEquals(20720, Reward.BRUMA_TORCH.items()[0]);
		assertEquals(20714, Reward.TOME_OF_FIRE.items()[0]);
		assertEquals(20716, Reward.TOME_OF_FIRE.items()[1]);
		assertEquals(20693, Reward.PHOENIX.items()[0]);
		assertEquals(5, Reward.PHOENIX.items().length);
	}

	@Test
	public void gamevalAndPlugin()
	{
		// RuneLite's Wintertodt plugin: WINTERTODT_REGION = 6462.
		assertEquals(6462, Wintertodt.PRISON_REGION);
		// The HUD is group 396 (InterfaceID.WINT_STATUS); RuneLite's status bars read VarbitID.WINT_WARMTH (11434).
		assertEquals(InterfaceID.WINT_STATUS, Wintertodt.HUD_ENERGY >>> 16);
		assertEquals(InterfaceID.WINT_STATUS, Wintertodt.HUD_POINTS >>> 16);
		assertEquals(11434, Wintertodt.WARMTH_VARBIT);
		// Tools: tinderbox 590, bruma torch 20720 and off-hand 29777; knife 946, fletching knife 31043; hammer 2347.
		assertArrayEquals(new int[]{590, 20720, 29777}, Wintertodt.LIGHTERS);
		assertArrayEquals(new int[]{946, 31043}, Wintertodt.KNIVES);
		assertEquals(2347, Wintertodt.HAMMERS[0]);
	}

	@Test
	public void axesByName()
	{
		assertTrue(WintertodtPilot.isAxe("Rune axe"));
		assertTrue(WintertodtPilot.isAxe("Dragon felling axe"));
		assertTrue(WintertodtPilot.isAxe("Infernal axe (uncharged)"));
		assertTrue(WintertodtPilot.isAxe("3rd age axe"));
		assertFalse(WintertodtPilot.isAxe("Rune pickaxe"));
		assertFalse(WintertodtPilot.isAxe("Rune battleaxe"));
		assertFalse(WintertodtPilot.isAxe("Axe handle"));
		assertFalse(WintertodtPilot.isAxe(null));
	}

	@Test
	public void rewardsText()
	{
		assertEquals("No reward", WintertodtPilot.rewardsText(480));
		assertEquals("3 rewards +48%", WintertodtPilot.rewardsText(1240));
		assertEquals("2 rewards", WintertodtPilot.rewardsText(500));
		assertEquals("180 to 500: no reward below it", WintertodtPilot.pointsNote(320));
	}
}
