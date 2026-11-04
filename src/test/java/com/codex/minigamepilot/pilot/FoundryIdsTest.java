package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.FoundryCycle;
import com.codex.minigamepilot.core.Mould;
import java.util.EnumSet;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

/**
 * The Giants' Foundry ids the Pilot reads, by their RuneLite names, against the numbers the Easy
 * Giants' Foundry plugin reads (a cross-check: the wiki lists none), so a renumbered name shows up
 * here before it does at the foundry.
 */
public class FoundryIdsTest
{
	@Test
	public void crossChecks()
	{
		assertEquals(11472, FoundryPilot.KOVAC);
		assertEquals(44619, FoundryPilot.TRIP_HAMMER);
		assertEquals(44620, FoundryPilot.GRINDSTONE);
		assertEquals(44621, FoundryPilot.POLISHING_WHEEL);
		assertEquals(44631, FoundryPilot.LAVA_POOL);
		assertEquals(44632, FoundryPilot.WATERFALL);
		assertEquals(44776, ObjectID.GIANTS_FOUNDRY_CRUCIBLE_MULTI);
		assertEquals(44777, ObjectID.GIANTS_FOUNDRY_MOULD_JIG);
		assertEquals(44778, ObjectID.GIANTS_FOUNDRY_PREFORM_STORAGE);
		assertEquals(27010, FoundryPilot.PREFORM);
		assertEquals(13948, VarbitID.GIANTS_FOUNDRY_PREFORM_TEMPERATURE);
		assertEquals(13949, VarbitID.GIANTS_FOUNDRY_PREFORM_COMPLETION);
		assertEquals(13907, VarbitID.GIANTS_FOUNDRY_COMMISSION_WORD_1);
		assertEquals(13908, VarbitID.GIANTS_FOUNDRY_COMMISSION_WORD_2);
		assertEquals(13931, VarbitID.GIANTS_FOUNDRY_BRONZE_COUNT);
		assertEquals(13936, VarbitID.GIANTS_FOUNDRY_RUNE_COUNT);
		assertEquals(13947, VarbitID.GIANTS_FOUNDRY_PREFORM_STORED);
		assertEquals(3436, VarPlayerID.GIANTS_FOUNDRY_REWARD_SHOP_POINTS);
		// HUD components: the heat bar, the grindstone band, the completion bar, the sweet spot.
		assertEquals(49414153, InterfaceID.GiantsFoundryHud.TEMPERATURE_BAR_RECT1);
		assertEquals(49414164, InterfaceID.GiantsFoundryHud.GRIND_BAR);
		assertEquals(49414219, InterfaceID.GiantsFoundryHud.COMPLETION_BAR);
		assertEquals(49414148, InterfaceID.GiantsFoundryHud.SWEETSPOT_LAYER);
		// The mould jig's list and its two scores.
		assertEquals(47054857, InterfaceID.GiantsFoundryMould.CONTENT);
		assertEquals(47054876, InterfaceID.GiantsFoundryMould.KEYVAL1);
		assertEquals(47054878, InterfaceID.GiantsFoundryMould.KEYVAL2);
		// Stage icons: 4442 trip hammer, 4443 grindstone, 4444 polishing wheel.
		assertEquals(FoundryCycle.Tool.HAMMER, FoundryPilot.toolOfSprite(4442));
		assertEquals(FoundryCycle.Tool.GRIND, FoundryPilot.toolOfSprite(4443));
		assertEquals(FoundryCycle.Tool.POLISH, FoundryPilot.toolOfSprite(4444));
		assertNull(FoundryPilot.toolOfSprite(0));
	}

	@Test
	public void watched()
	{
		for (int id : new int[]{44619, 44620, 44621, 44631, 44632, 44630, 44776, 44777, 44778})
		{
			assertTrue(id + "", FoundryPilot.isWatched(id));
		}
		assertEquals(FoundryCycle.Jig.COOLED, FoundryPilot.jig(ObjectID.GIANTS_FOUNDRY_JIG_MOULD_COOLED));
		assertEquals(FoundryCycle.Jig.EMPTY, FoundryPilot.jig(ObjectID.GIANTS_FOUNDRY_JIG_EMPTY));
		assertEquals(FoundryCycle.Jig.UNKNOWN, FoundryPilot.jig(1));
	}

	@Test
	public void mouldsSaveAndLoad()
	{
		String saved = FoundryPilot.encodeMoulds(EnumSet.of(Mould.CHOPPA, Mould.SAW_TIP, Mould.THE_POINT));
		// Defaults are never saved: every account has them.
		assertEquals("CHOPPA,THE_POINT", saved);
		assertEquals(EnumSet.of(Mould.CHOPPA, Mould.THE_POINT), FoundryPilot.decodeMoulds(saved));
		assertEquals(EnumSet.of(Mould.CHOPPA), FoundryPilot.decodeMoulds("CHOPPA, NOT_A_MOULD,,"));
		assertTrue(FoundryPilot.decodeMoulds(null).isEmpty());
	}
}
