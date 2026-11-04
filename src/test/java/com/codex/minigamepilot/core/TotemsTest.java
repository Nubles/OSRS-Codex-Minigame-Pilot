package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/** The Vale Totems facts, as the OSRS wiki gives them. */
public class TotemsTest
{
	@Test
	public void woodsAndLevels()
	{
		// "Oak: 20 Fletching" ... "Redwood: 90 Fletching".
		int[] levels = {20, 35, 50, 65, 80, 90};
		for (Totems.Wood w : Totems.Wood.values())
		{
			assertEquals(levels[w.ordinal()], w.level());
		}
		assertNull(Totems.Wood.best(19));
		assertEquals(Totems.Wood.OAK, Totems.Wood.best(20));
		assertEquals(Totems.Wood.MAPLE, Totems.Wood.best(64));
		assertEquals(Totems.Wood.REDWOOD, Totems.Wood.best(99));
		assertEquals(Totems.Wood.YEW, Totems.Wood.ofVarbit(4));
		assertNull(Totems.Wood.ofVarbit(7));
	}

	@Test
	public void decorationsAreTheWoodsOwnFletching()
	{
		// The wiki's oak list: shortbow (u), shortbow, longbow (u), longbow, shield, stock.
		assertEquals(6, Totems.Wood.OAK.decorations().length);
		assertTrue(Totems.Wood.OAK.decorates(ItemID.XBOWS_CROSSBOW_STOCK_OAK));
		assertTrue(Totems.Wood.MAGIC.decorates(ItemID.UNSTRUNG_MAGIC_SHORTBOW));
		assertFalse(Totems.Wood.MAGIC.decorates(ItemID.UNSTRUNG_YEW_SHORTBOW));
		// Redwood: shields and hiking staves.
		assertTrue(Totems.Wood.REDWOOD.decorates(ItemID.REDWOOD_SHIELD));
		assertTrue(Totems.Wood.REDWOOD.decorates(ItemID.REDWOOD_HIKING_STAFF));
		assertEquals(2, Totems.Wood.REDWOOD.decorations().length);
	}

	@Test
	public void spirits()
	{
		assertEquals(Totems.Animal.BUFFALO, Totems.Animal.ofVarbit(1));
		assertEquals(Totems.Animal.SCORPION, Totems.Animal.ofVarbit(5));
		assertNull(Totems.Animal.ofVarbit(0));
		assertEquals(Totems.Animal.EAGLE, Totems.Animal.ofCarved(12));
		assertNull(Totems.Animal.ofCarved(3));
		assertEquals(Totems.Animal.SNAKE, Totems.Animal.ofIcon(ItemID.ENT_TOTEMS_ANIMAL_ICON_D_DISABLED));
		assertNull(Totems.Animal.ofIcon(ItemID.KNIFE));
	}

	@Test
	public void offeringsAndRewards()
	{
		assertEquals(15_000, Totems.OFFERINGS_LIMIT);
		assertEquals(8, Totems.SITES);
		assertEquals(0, Totems.points(99));
		assertEquals(25, Totems.points(2_500));
		assertEquals(0, Totems.points(-5));
		assertEquals(250, Totems.Reward.BOW_STRING_SPOOL.points());
		assertEquals(350, Totems.Reward.FLETCHING_KNIFE.points());
		assertEquals(500, Totems.Reward.GREENMAN_MASK.points());
		assertEquals(20, Totems.Reward.ENT_BRANCH.points());
	}
}
