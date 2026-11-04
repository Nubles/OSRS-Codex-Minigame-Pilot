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
			"motherlodeGoal", "homesGoal", "furnaceGoal", "showGoals", "rewardRoad",
			// The tick coach.
			"tickCoach",
			// Guardians of the Rift.
			"rift", "riftFragments",
			"fieldNotes")), keys);
		assertEquals("codexminigamepilot", CodexMinigamePilotConfig.GROUP);
	}
}
