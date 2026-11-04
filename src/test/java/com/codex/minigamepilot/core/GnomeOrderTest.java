package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.GnomeRestaurant.Customer;
import com.codex.minigamepilot.core.GnomeRestaurant.Dish;
import com.codex.minigamepilot.core.GnomeRestaurant.Ingredient;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/** The one next step of a delivery, turn by turn. */
public class GnomeOrderTest
{
	private static GnomeOrder.Snapshot order(Dish d, Customer c, int... items)
	{
		GnomeOrder.Snapshot s = new GnomeOrder.Snapshot();
		s.ordered = true;
		s.dish = d;
		s.customer = c;
		s.msLeft = 300_000;
		for (int i = 0; i < items.length; i += 2)
		{
			s.held.put(items[i], items[i + 1]);
		}
		return s;
	}

	@Test
	public void noOrder()
	{
		GnomeOrder.Snapshot s = new GnomeOrder.Snapshot();
		assertEquals("Ask Gianne jnr. for a delivery", GnomeOrder.next(s).step.getTitle());
		assertEquals(Step.Target.GIANNE, GnomeOrder.next(s).step.getTarget());
		s.cooking = 20;
		assertEquals("Train Cooking to 29", GnomeOrder.next(s).step.getTitle());
		s.ordered = true;
		assertEquals("Check your delivery box", GnomeOrder.next(s).step.getTitle());
	}

	@Test
	public void worksFromTheFurthestStageHeld()
	{
		GnomeOrder.Snapshot s = order(Dish.WORM_HOLE, Customer.FEMI, ItemID.HALF_BAKED_BOWL, 1, ItemID.KING_WORM, 4, ItemID.ONION, 2,
			ItemID.GNOME_SPICE, 1, ItemID.EQUA_LEAVES, 1);
		GnomeOrder.Plan p = GnomeOrder.next(s);
		assertEquals(2, p.at);
		assertEquals("Prepare the half baked bowl with 4 king worms, 2 onions and gnome spice", p.step.getTitle());
		assertTrue(p.step.getDetail().startsWith("5:00 left · 2 more steps, then to Femi"));
		assertTrue(p.missing.get(2).isEmpty());
		assertTrue(p.missing.get(4).isEmpty());

		// A worm short, and no spice: the fetch names both.
		s.held.put(ItemID.KING_WORM, 3);
		s.held.remove(ItemID.GNOME_SPICE);
		p = GnomeOrder.next(s);
		assertEquals("Fetch 1 king worm and 1 gnome spice", p.step.getTitle());
		assertTrue(p.step.getDetail().contains("Hudo"));

		// The unfinished bowl, with no leaves left: the garnish lacks them.
		s.held.clear();
		s.held.put(ItemID.ALUFT_BAKED_WORM_HOLE, 1);
		p = GnomeOrder.next(s);
		assertEquals(4, p.at);
		assertEquals("Fetch 1 equa leaves", p.step.getTitle());
		assertEquals(Integer.valueOf(1), p.missing.get(4).get(Ingredient.EQUA_LEAVES));
	}

	@Test
	public void missingIsCountedAcrossTheStagesLeft()
	{
		// Fruit batta: 4 equa leaves to prepare it; gnome spice only as the garnish's tool.
		GnomeOrder.Snapshot s = order(Dish.FRUIT_BATTA, Customer.EEBEL, ItemID.GIANNE_DOUGH, 1, ItemID.BATTA_TIN, 1, ItemID.EQUA_LEAVES, 2,
			ItemID.LIME_CHUNKS, 1, ItemID.ORANGE_CHUNKS, 1, ItemID.PINEAPPLE_CHUNKS, 1);
		GnomeOrder.Plan p = GnomeOrder.next(s);
		assertEquals("Use gianne dough on a batta tin", p.step.getTitle());
		assertEquals(Integer.valueOf(2), p.missing.get(2).get(Ingredient.EQUA_LEAVES));
		assertEquals(Integer.valueOf(1), p.missing.get(4).get(Ingredient.GNOME_SPICE));
	}

	@Test
	public void cutsFruitWithAKnife()
	{
		// Wizard blizzard mixed: the pour wants pineapple chunks and lime slices.
		GnomeOrder.Snapshot s = order(Dish.WIZARD_BLIZZARD, Customer.HAZELMERE, ItemID.ALUFT_SHAKER_WIZZARD_BLIZZARD, 1, ItemID.COCKTAIL_GLASS_EMPTY, 1,
			ItemID.PINEAPPLE, 1, ItemID.LIME, 1, ItemID.KNIFE, 1);
		GnomeOrder.Plan p = GnomeOrder.next(s);
		assertEquals("Cut a pineapple into chunks", p.step.getTitle());
		assertTrue(p.step.hasItem(ItemID.KNIFE));
		s.held.remove(ItemID.PINEAPPLE);
		s.held.put(ItemID.PINEAPPLE_CHUNKS, 1);
		assertEquals("Cut a lime into slices", GnomeOrder.next(s).step.getTitle());
		// No knife: fetch them.
		s.held.remove(ItemID.KNIFE);
		assertEquals("Fetch 1 lime slices", GnomeOrder.next(s).step.getTitle());
		// A whole lime the mix still needs is not cut.
		GnomeOrder.Snapshot sgg = order(Dish.SHORT_GREEN_GUY, Customer.FEMI, ItemID.VODKA, 1, ItemID.LIME, 3, ItemID.COCKTAIL_SHAKER, 1,
			ItemID.KNIFE, 1);
		assertEquals("Mix 1 vodka and 3 limes in a cocktail shaker", GnomeOrder.next(sgg).step.getTitle());
	}

	@Test
	public void deliverAndTime()
	{
		GnomeOrder.Snapshot s = order(Dish.CHOCOLATE_BOMB, Customer.CAPTAIN_NINTO, ItemID.CHOCOLATE_BOMB, 1);
		GnomeOrder.Plan p = GnomeOrder.next(s);
		assertEquals(5, p.at);
		assertEquals("Deliver the chocolate bomb to Captain Ninto", p.step.getTitle());
		assertEquals(Step.Target.GNOME_CUSTOMER, p.step.getTarget());
		assertTrue(p.step.getDetail().contains("The dwarf bar beneath White Wolf Mountain (Fishing Contest for the tunnel)"));
		assertTrue(p.step.getDetail().contains("not yet on your map"));
		s.placeKnown = true;
		s.way = "Camelot teleport, about 40s";
		assertTrue(p.step.getDetail().contains("5:00 left"));
		assertTrue(GnomeOrder.next(s).step.getDetail().endsWith(". Camelot teleport, about 40s"));
		s.customerInSight = true;
		assertTrue(GnomeOrder.next(s).step.getDetail().endsWith("Captain Ninto is in sight"));
		s.msLeft = 0;
		assertEquals("Back to Gianne jnr. for a new order", GnomeOrder.next(s).step.getTitle());
		s.msLeft = GnomeOrder.UNKNOWN;
		assertTrue(GnomeOrder.next(s).step.getDetail().startsWith("Captain Ninto"));
	}

	@Test
	public void aboveYourLevel()
	{
		GnomeOrder.Snapshot s = order(Dish.CHOCOLATE_BOMB, Customer.FEMI, ItemID.GIANNE_DOUGH, 1);
		s.cooking = 35;
		GnomeOrder.Plan p = GnomeOrder.next(s);
		assertEquals("Chocolate bomb needs 42 Cooking", p.step.getTitle());
		assertTrue(p.step.isWaiting());
		// Holding one made by another player: deliver it anyway.
		s.held.put(ItemID.CHOCOLATE_BOMB, 1);
		assertEquals("Deliver the chocolate bomb to Femi", GnomeOrder.next(s).step.getTitle());
	}

	@Test
	public void loop()
	{
		assertEquals("Dough,Bake,Prepare,Bake,Garnish,Deliver", String.join(",", GnomeOrder.loop(Dish.CHEESE_TOM_BATTA)));
		assertEquals("Dough,Bake,Prepare,Bake,Deliver", String.join(",", GnomeOrder.loop(Dish.TOAD_BATTA)));
		assertEquals("Mix,Pour,Deliver", String.join(",", GnomeOrder.loop(Dish.FRUIT_BLAST)));
		assertEquals("Mix,Pour,Garnish,Heat,Deliver", String.join(",", GnomeOrder.loop(Dish.DRUNK_DRAGON)));
		assertEquals("Mix,Pour,Heat,Garnish,Deliver", String.join(",", GnomeOrder.loop(Dish.CHOC_SATURDAY)));
	}
}
