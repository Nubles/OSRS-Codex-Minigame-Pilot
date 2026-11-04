package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import com.codex.minigamepilot.core.Home;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

/** Every Mahogany Homes id the Pilot uses, checked against RuneLite's own names for them. */
public class HomesIdsTest
{
	private static final Pattern HOTSPOT = Pattern.compile("CON_CONTRACT_(\\w+)_HOTSPOT(\\d)");

	@Test
	public void eachHousesFurnitureIsTheGamesHotspotInOrder() throws IllegalAccessException
	{
		Map<Integer, String> names = new HashMap<>();
		for (Class<?> c = ObjectID.class; c != null && c != Object.class; c = c.getSuperclass())
		{
			for (Field f : c.getDeclaredFields())
			{
				if (Modifier.isStatic(f.getModifiers()) && f.getType() == int.class && f.getName().startsWith("CON_CONTRACT_"))
				{
					f.setAccessible(true);
					names.put(f.getInt(null), f.getName());
				}
			}
		}
		for (Home h : Home.values())
		{
			String area = null;
			for (int i = 0; i < h.hotspotCount(); i++)
			{
				String name = names.get(h.hotspot(i).getObjectId());
				assertNotNull(h + " hotspot " + i, name);
				Matcher m = HOTSPOT.matcher(name);
				assertEquals(name, true, m.matches());
				assertEquals(h + " " + name, i + 1, Integer.parseInt(m.group(2)));
				if (area == null)
				{
					area = m.group(1);
				}
				assertEquals(h + " " + name, area, m.group(1));
			}
		}
	}

	@Test
	public void varbits()
	{
		assertEquals(VarbitID.CON_CONTRACT_HOTSPOT_1_DISPLAY, Home.HOTSPOT_VARBIT);
		assertEquals(VarbitID.CON_CONTRACT_HOTSPOT_8_DISPLAY, Home.HOTSPOT_VARBIT + Home.HOTSPOTS - 1);
	}
}
