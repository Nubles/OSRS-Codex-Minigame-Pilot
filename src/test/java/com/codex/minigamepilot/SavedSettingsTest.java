package com.codex.minigamepilot;

import static org.junit.Assert.assertEquals;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.TreeSet;
import net.runelite.client.config.ConfigItem;
import org.junit.Test;

/** Settings are saved under their key names: a renamed key would lose every player's choice. */
public class SavedSettingsTest
{
	@Test
	public void savedSettingsKeepTheirKeys()
	{
		TreeSet<String> keys = new TreeSet<>();
		for (Method m : CodexMinigamePilotConfig.class.getDeclaredMethods())
		{
			ConfigItem item = m.getAnnotation(ConfigItem.class);
			if (item != null)
			{
				keys.add(item.keyName());
			}
		}
		assertEquals(new TreeSet<>(Arrays.asList(
			"motherlode", "furnace", "homes", "bar", "cofferLow", "upperHopper",
			"plaque", "route", "minimap", "labels", "others", "hintArrow", "worldMap", "visitChat",
			// Mahogany Homes.
			"homesLowPlanks",
			"motherlodeGoal", "homesGoal", "furnaceGoal", "showGoals", "rewardRoad",
			// The tick coach.
			"tickCoach",
			// Guardians of the Rift.
			"rift", "riftFragments",
			// Tempoross.
			"tempoross", "temporossCook", "temporossStormLoad",
			// Giants' Foundry.
			"foundry",
			// Mastering Mixology.
			"mixology", "mixologyPlan", "mixologyCues", "mixologyReward",
			// Vale Totems.
			"valeTotems", "totemsWood", "totemsClaim", "totemsReward",
			// Tithe Farm.
			"titheFarm", "tithePlants", "titheReward",
			// Stealing artefacts.
			"stealingArtefacts", "artefactsPatrols", "artefactsLevel",
			// Stealing valuables.
			"stealingValuables", "valuablesOwners", "valuablesLevel",
			// Rogues' Den.
			"roguesDen", "roguesDenTraps",
			// Barbarian Assault.
			"barbarianAssault", "assaultReward",
			// Wintertodt.
			"wintertodt", "wintertodtFletch", "wintertodtWarmth", "wintertodtReward",
			// Gnome Restaurant.
			"gnomeRestaurant",
			// Nightmare Zone.
			"nightmareZone", "nmzReward", "nmzHalfImbues",
			// Volcanic Mine.
			"volcanicMine", "volcanicReward",
			// Barracuda Trials.
			"barracudaTrials", "barracudaWaypoint",
			// Trouble Brewing.
			"troubleBrewing", "brewingReward",
			// Sorceress's Garden.
			"sorceressGarden", "gardenSeason", "gardenGoal", "gardenTarget",
			// Soul Wars.
			"soulWars", "soulWarsReward", "soulWarsSkill", "soulWarsLevel", "soulWarsSpend", "soulWarsHalfImbue",
			"fieldNotes")), keys);
		assertEquals("codexminigamepilot", CodexMinigamePilotConfig.GROUP);
	}
}
