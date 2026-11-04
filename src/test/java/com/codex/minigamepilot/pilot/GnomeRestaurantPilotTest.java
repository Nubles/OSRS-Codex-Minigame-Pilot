package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.GnomeLog;
import com.codex.minigamepilot.core.GnomeOrder;
import com.codex.minigamepilot.core.GnomeRestaurant.Customer;
import com.codex.minigamepilot.core.GnomeRestaurant.Dish;
import com.codex.minigamepilot.core.SavedKeysFuzzTest;
import com.codex.minigamepilot.ui.View;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/** The Gnome Restaurant pilot's saved places, recipe lines and checklist. */
public class GnomeRestaurantPilotTest
{
	@Test
	public void placesRoundTrip()
	{
		Map<String, WorldPoint> p = GnomeRestaurantPilot.decodePlaces("1;ROMETTI:2482:3510:1;GIANNE:2449:3501:1;NOBODY:1:1:0;FEMI:x:1:0;GARKOR:2805:2762:9");
		assertEquals(2, p.size());
		assertEquals(new WorldPoint(2482, 3510, 1), p.get("ROMETTI"));
		assertEquals("1;ROMETTI:2482:3510:1;GIANNE:2449:3501:1", GnomeRestaurantPilot.encodePlaces(p));
		assertTrue(GnomeRestaurantPilot.decodePlaces(null).isEmpty());
		assertTrue(GnomeRestaurantPilot.decodePlaces("2;ROMETTI:1:1:0").isEmpty());
	}

	@Test
	public void placesSurviveDamage()
	{
		SavedKeysFuzzTest.survive("gnome.places", "1;ROMETTI:2482:3510:1;GIANNE:2449:3501:1", s ->
		{
			Map<String, WorldPoint> p = GnomeRestaurantPilot.decodePlaces(s);
			assertEquals(p, GnomeRestaurantPilot.decodePlaces(GnomeRestaurantPilot.encodePlaces(p)));
		});
	}

	@Test
	public void recipeLines()
	{
		GnomeOrder.Snapshot s = new GnomeOrder.Snapshot();
		s.ordered = true;
		s.dish = Dish.CHEESE_TOM_BATTA;
		s.customer = Customer.ROMETTI;
		s.msLeft = 200_000;
		s.held.put(ItemID.HALF_BAKED_BATTA, 1);
		s.held.put(ItemID.CHEESE, 1);
		GnomeOrder.Plan plan = GnomeOrder.next(s);
		List<View.Line> lines = GnomeRestaurantPilot.recipeLines(s, plan);
		assertEquals(6, lines.size());
		assertEquals("Fetch 1 tomato", plan.step.getTitle());
		assertEquals("Prepare: cheese, tomato", GnomeRestaurantPilot.short_(Dish.CHEESE_TOM_BATTA.stages().get(2)));
		assertEquals("Bake on a range", GnomeRestaurantPilot.short_(Dish.CHEESE_TOM_BATTA.stages().get(1)));
		assertEquals("Mix: vodka, 3 lime", GnomeRestaurantPilot.short_(Dish.SHORT_GREEN_GUY.stages().get(0)));
		assertEquals("Pour into a glass: lime slices, equa leaves", GnomeRestaurantPilot.short_(Dish.SHORT_GREEN_GUY.stages().get(1)));
	}

	@Test
	public void checklist()
	{
		GnomeLog g = new GnomeLog();
		g.add(new GnomeLog.Delivery(1, false, Dish.TOAD_BATTA, Customer.FEMI, 60_000, 0, true, 152, 0, null));
		View.PickCard card = GnomeRestaurantPilot.checklist(g, 30);
		Map<String, String> r = new View(true, View.Activity.GNOME_RESTAURANT, null, null, null, -1).pick(card).readings();
		assertEquals("1 of 20 delivered", r.get("pick"));
		assertTrue(r.get("pick.note").startsWith("You can make 14 of 20"));
	}
}
