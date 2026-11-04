package com.codex.minigamepilot.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import org.junit.Test;

/** The Nightmare Zone's facts against the wiki's pages, and its ids against the wiki's infoboxes and RuneLite's gameval. */
public class NmzTest
{
	@Test
	public void wikiIds()
	{
		// Dominic Onion 1120; the dream potion 26269; Dominic's coffer 26272; Rewards chest 26273.
		assertEquals(1120, Nmz.DOMINIC);
		assertEquals(26269, Nmz.VIAL_FULL);
		assertEquals(26272, Nmz.COFFER[0]);
		assertEquals(26273, Nmz.CHEST);
		// Barrels: super ranging 26277, super magic 26278, overload 26279, absorption 26280.
		assertEquals(26277, Nmz.Potion.SUPER_RANGING.barrel());
		assertEquals(26278, Nmz.Potion.SUPER_MAGIC.barrel());
		assertEquals(26279, Nmz.Potion.OVERLOAD.barrel());
		assertEquals(26280, Nmz.Potion.ABSORPTION.barrel());
		// Absorption (4) to (1) 11734 to 11737; Overload (1) 11733; Super ranging 11722 to 11725; Super magic 11726 to 11729.
		assertArrayEquals(new int[]{11734, 11735, 11736, 11737}, Nmz.Potion.ABSORPTION.ids());
		assertEquals(11733, Nmz.Potion.OVERLOAD.ids()[3]);
		assertArrayEquals(new int[]{11722, 11723, 11724, 11725}, Nmz.Potion.SUPER_RANGING.ids());
		assertArrayEquals(new int[]{11726, 11727, 11728, 11729}, Nmz.Potion.SUPER_MAGIC.ids());
	}

	@Test
	public void runeliteIds()
	{
		// RuneLite's Nightmare Zone plugin: NMZ_MAP_REGION = {9033}, planes above 0; its varbits.
		assertEquals(9033, Nmz.ARENA_REGION);
		assertTrue(Nmz.inArena(9033, 1));
		assertTrue(Nmz.inArena(9033, 3));
		assertFalse("the King Black Dragon's lair", Nmz.inArena(9033, 0));
		assertEquals(3949, Nmz.DREAM_POINTS);
		assertEquals(3956, Nmz.ABSORPTION);
		assertEquals(3955, Nmz.OVERLOAD);
		assertEquals(1060, Nmz.REWARD_POINTS);
		assertEquals(1777, Nmz.IRONMAN);
		// Each NPC is either hard or normal, sixty-seven of each.
		assertEquals(67, Nmz.HARD.length);
		assertEquals(67, Nmz.NORMAL.length);
		Set<Integer> all = new HashSet<>();
		for (int id : Nmz.HARD)
		{
			assertTrue(all.add(id));
		}
		for (int id : Nmz.NORMAL)
		{
			assertTrue(all.add(id));
		}
		assertTrue(Nmz.hard(NpcID.NZONE_ELVARG_HARD));
		assertFalse(Nmz.hard(NpcID.NZONE_ELVARG_NORMAL));
		assertTrue(Nmz.normal(NpcID.NZONE_FD_DAMIS_NORMAL_NORMAL));
		assertTrue(Nmz.hard(NpcID.NZONE_FD_DAMIS_NORMAL_HARD));
		assertFalse(Nmz.boss(NpcID.NZONE_HOST));
		// Prayer potion (4) 2434, super restore (4) 3024, super combat (4) 12695.
		assertEquals(2434, Nmz.Potion.PRAYER.ids()[0]);
		assertEquals(3024, Nmz.Potion.SUPER_RESTORE.ids()[0]);
		assertEquals(12695, Nmz.Potion.SUPER_COMBAT.ids()[0]);
	}

	@Test
	public void shopPrices()
	{
		// "Super ranging (1) 250", "Super magic potion (1) 250", "Overload (1) 1,500", "Absorption (1) 1,000".
		assertEquals(250, Nmz.Potion.SUPER_RANGING.points());
		assertEquals(250, Nmz.Potion.SUPER_MAGIC.points());
		assertEquals(1_500, Nmz.Potion.OVERLOAD.points());
		assertEquals(1_000, Nmz.Potion.ABSORPTION.points());
		assertEquals(0, Nmz.Potion.PRAYER.points());
		// "Herb box 9,500"; the imbues, and half with the Hard Combat Achievements.
		assertEquals(9_500, Nmz.Reward.HERB_BOX.cost(true));
		assertEquals(1_250_000, Nmz.Reward.BLACK_MASK.cost(false));
		assertEquals(625_000, Nmz.Reward.SLAYER_HELMET.cost(true));
		assertEquals(800_000, Nmz.Reward.SALVE_AMULET_E.cost(false));
		assertEquals(725_000, Nmz.Reward.RING_OF_SUFFERING.cost(false));
		assertEquals(362_500, Nmz.Reward.RING_OF_SUFFERING.cost(true));
		assertEquals(650_000, Nmz.Reward.BERSERKER_RING.cost(false));
		assertEquals(500_000, Nmz.Reward.GRANITE_RING.cost(false));
		// The wiki's total: one black mask or helmet, one salve: "7,825,000 (3,912,500)".
		long total = 0;
		long half = 0;
		for (Nmz.Reward r : Nmz.Reward.values())
		{
			if (r.imbue() && r != Nmz.Reward.SLAYER_HELMET && r != Nmz.Reward.SALVE_AMULET_E)
			{
				total += r.cost(false);
				half += r.cost(true);
			}
		}
		assertEquals(7_825_000, total);
		assertEquals(3_912_500, half);
	}

	@Test
	public void modes()
	{
		// "Endurance 1,000 5,000", "Rumble 2,000 6,000", "Custom rumble 22,000 26,000", max quest points "12,000 16,000".
		assertEquals(Nmz.Mode.ENDURANCE, Nmz.Mode.byCost(1));
		assertTrue(Nmz.Mode.hardByCost(5));
		assertEquals(Nmz.Mode.RUMBLE, Nmz.Mode.byCost(6));
		assertEquals(Nmz.Mode.CUSTOM, Nmz.Mode.byCost(12));
		assertFalse(Nmz.Mode.hardByCost(22));
		assertTrue(Nmz.Mode.hardByCost(16));
		assertNull(Nmz.Mode.byCost(3));
		assertNull(Nmz.Mode.byCost(0));
		assertEquals(26, Nmz.Mode.CUSTOM.cost(true));
	}

	@Test
	public void herbBox()
	{
		// The table is out of 128, seven rolls.
		int sum = 0;
		for (int[] h : Nmz.HERB_TABLE)
		{
			sum += h[1];
		}
		assertEquals(Nmz.HERB_TABLE_OF, sum);
		assertEquals(7, Nmz.HERB_BOX_ROLLS);
		// Every herb at 128 coins: seven of them, 896.
		assertEquals(896, Nmz.herbBoxValue(id -> 128), 1e-9);
		// Only ranarr priced: 7 x 11/128 x 7,000.
		assertEquals(7 * 11 / 128.0 * 7000, Nmz.herbBoxValue(id -> id == ItemID.UNIDENTIFIED_RANARR ? 7000 : 0), 1e-9);
		assertEquals(0, Nmz.herbBoxValue(null), 0);
	}

	@Test
	public void doses()
	{
		assertEquals(4, Nmz.Potion.OVERLOAD.doses(ItemID.NZONE4DOSEOVERLOADPOTION));
		assertEquals(1, Nmz.Potion.ABSORPTION.doses(11737));
		assertEquals(0, Nmz.Potion.ABSORPTION.doses(11733));
	}

	@Test
	public void names()
	{
		assertEquals("Count Draynor", Nmz.cleanName("<col=ffff00>Count  Draynor</col>"));
		assertEquals("A B", Nmz.cleanName("A;B"));
		assertEquals("", Nmz.cleanName(null));
	}
}
