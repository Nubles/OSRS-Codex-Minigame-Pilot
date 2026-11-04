package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Mixology;
import com.codex.minigamepilot.core.MixRounds;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

/**
 * The Mastering Mixology ids the Pilot reads, by their RuneLite names, against the numbers the
 * Mastering Mixology plugin by hex-agon reads (a cross-check: the wiki lists none), so a
 * renumbered name shows up here before it does in the lab.
 */
public class MixologyIdsTest
{
	@Test
	public void crossChecks()
	{
		assertEquals(54868, MixologyPilot.LEVERS[0]);
		assertEquals(54867, MixologyPilot.LEVERS[1]);
		assertEquals(54869, MixologyPilot.LEVERS[2]);
		assertEquals(55395, MixologyPilot.VESSEL);
		assertEquals(55390, MixologyPilot.STATIONS[0]);
		assertEquals(55389, MixologyPilot.STATIONS[1]);
		assertEquals(55391, MixologyPilot.STATIONS[2]);
		assertEquals(54917, MixologyPilot.CONVEYOR);
		assertEquals(54903, MixologyPilot.HOPPER);
		assertEquals(54904, MixologyPilot.REFINER);
		assertEquals(55396, MixologyPilot.DIGWEEDS[0]);
		assertEquals(30031, MixologyPilot.DIGWEED);
		assertEquals(11315, VarbitID.MM_LAB_ORDER_1_TYPE);
		assertEquals(11316, VarbitID.MM_LAB_ORDER_1_MODIFIER);
		assertEquals(11339, VarbitID.MM_LAB_VESSEL_READY);
		assertEquals(11340, MixologyPilot.STATION_POTION[0]);
		assertEquals(11341, MixologyPilot.STATION_POTION[1]);
		assertEquals(11342, MixologyPilot.STATION_POTION[2]);
		assertEquals(11431, MixologyPilot.HOPPER_PASTE[0]);
		assertEquals(4416, MixologyPilot.RESIN[0]);
		assertEquals(4415, MixologyPilot.RESIN[1]);
		assertEquals(4414, MixologyPilot.RESIN[2]);
		assertEquals(2954, MixologyPilot.CUE_AGITATOR);
		assertEquals(2955, MixologyPilot.CUE_ALEMBIC);
		assertEquals(0x0372_0002, InterfaceID.MmOverlay.CONTENT);
		// Potion items: unfinished 30011-30020 and worked 30021-30030, in item order.
		assertEquals(30011, Mixology.Potion.MMM.unfinished());
		assertEquals(30030, Mixology.Potion.MAL.finished());
		assertEquals(ItemID.MM_POTION_AAL_FINISHED, Mixology.Potion.ALA.finished());
		assertEquals(ItemID.MM_POTION_LLA_UNFINISHED, Mixology.Potion.ALL.unfinished());
		assertEquals(30005, Mixology.Paste.MOX.item());
	}

	@Test
	public void watched()
	{
		for (int id : new int[]{54867, 54868, 54869, 55395, 55389, 55390, 55391, 54903, 54904, 54917, 55396, 55399})
		{
			assertTrue(id + "", MixologyPilot.isWatched(id));
		}
		assertFalse(MixologyPilot.isWatched(1));
	}

	@Test
	public void lines()
	{
		assertEquals("40M 30A", MixologyPilot.resinLine(new MixRounds.Round(1, 1, 40, 30, 0, 0, 2)));
		assertEquals("No resin", MixologyPilot.resinLine(new MixRounds.Round(1, 1, 0, 0, 0, 0, 0)));
		assertEquals("45m", MixologyPilot.clockHours(0.75));
		assertEquals("940", MixologyPilot.compact(940));
		assertEquals("6.2k", MixologyPilot.compact(6240));
		assertEquals("152k", MixologyPilot.compact(152_400));
		assertEquals("3h 10m", MixologyPilot.clockHours(3 + 1 / 6.0));
	}
}
