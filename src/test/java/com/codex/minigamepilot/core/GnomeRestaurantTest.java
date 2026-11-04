package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.GnomeRestaurant.Customer;
import com.codex.minigamepilot.core.GnomeRestaurant.Dish;
import com.codex.minigamepilot.core.GnomeRestaurant.Ingredient;
import com.codex.minigamepilot.core.GnomeRestaurant.Kind;
import com.codex.minigamepilot.core.GnomeRestaurant.Stage;
import com.codex.minigamepilot.core.GnomeRestaurant.Tip;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/** The Gnome Restaurant's facts against the OSRS wiki, and its ids against the wiki's infoboxes. */
public class GnomeRestaurantTest
{
	@Test
	public void rules()
	{
		// "A minimum cooking level of 29 is required in order to complete the training."
		assertEquals(29, GnomeRestaurant.TRAINING_COOKING);
		// "Easy tasks have a time limit of 6 minutes, whereas hard tasks have a limit of 11 minutes."
		assertEquals(360_000L, GnomeRestaurant.EASY_MS);
		assertEquals(660_000L, GnomeRestaurant.HARD_MS);
		assertEquals(360_000L, Customer.ROMETTI.limitMs());
		assertEquals(660_000L, Customer.GARKOR.limitMs());
		// One credit easy, three hard, toward "the 12 needed for one free food item"; "up to 10 free items".
		assertEquals(1, Customer.FEMI.credits());
		assertEquals(3, Customer.WINGSTONE.credits());
		assertEquals(0, GnomeRestaurant.foods(11));
		assertEquals(1, GnomeRestaurant.foods(12));
		assertEquals(10, GnomeRestaurant.foods(500));
		assertEquals(11, GnomeRestaurant.toNextFood(13));
		assertEquals(0, GnomeRestaurant.toNextFood(120));
		// "a 5 minute and 30 second waiting penalty".
		assertEquals(330_000L, GnomeRestaurant.HARD_DECLINE_MS);
	}

	@Test
	public void customers()
	{
		// The wiki's easy table has twenty customers, the hard one twenty (Garkor's two rows one customer).
		int easy = 0;
		int hard = 0;
		Set<Integer> varbits = new HashSet<>();
		for (Customer c : Customer.values())
		{
			assertTrue(varbits.add(c.varbit()));
			assertEquals(c, Customer.named(c.getTitle()));
			assertEquals(c, Customer.ofVarbit(c.varbit()));
			if (c.isHard())
			{
				hard++;
			}
			else
			{
				easy++;
			}
		}
		assertEquals(20, easy);
		assertEquals(20, hard);
		assertEquals(Customer.COACH, Customer.named("Coach"));
		assertEquals(Customer.GLO_CARANOCK, Customer.named("G.L.O Caranock"));
		assertNull(Customer.named("Hudo"));
		assertNull(Customer.ofVarbit(0));
		// Easy and hard by the wiki's tables.
		assertFalse(Customer.KING_NARNODE.isHard());
		assertFalse(Customer.CAPTAIN_ERRDO.isHard());
		assertTrue(Customer.CAPTAIN_ERRDO.anchor() != null);
		assertTrue(Customer.CAPTAIN_DAERKIN.isHard());
		// Gnome goggles and scarf: "from Captain Daerkin and Captain Ninto"; mint cake: "Brambickle, Wingstone, and Penwie";
		// seed pods: "only from Manglethorp and the three exploring gnomes".
		assertTrue(Customer.CAPTAIN_NINTO.tips().contains(Tip.GNOME_GOGGLES));
		assertTrue(Customer.CAPTAIN_DAERKIN.tips().contains(Tip.GNOME_SCARF));
		assertTrue(Customer.PENWIE.tips().contains(Tip.MINT_CAKE));
		assertTrue(Customer.PROFESSOR_MANGLETHORP.tips().contains(Tip.GRAND_SEED_POD));
		assertFalse(Customer.PROFESSOR_MANGLETHORP.tips().contains(Tip.MINT_CAKE));
		assertTrue(Customer.ROMETTI.tips().isEmpty());
		// Item ids (OSRS wiki): gnome goggles 9472, gnome scarf 9470, mint cake 9475, grand seed pod 9469.
		assertEquals(9472, Tip.GNOME_GOGGLES.item());
		assertEquals(9470, Tip.GNOME_SCARF.item());
		assertEquals(9475, Tip.MINT_CAKE.item());
		assertEquals(9469, Tip.GRAND_SEED_POD.item());
	}

	@Test
	public void runeliteWorldMapAnchors()
	{
		// RuneLite's TransportationPointLocation gnome gliders and MinigameLocation.GNOME_RESTAURANT.
		assertEquals(2460, Customer.CAPTAIN_ERRDO.anchor().x);
		assertEquals(3502, Customer.CAPTAIN_ERRDO.anchor().y);
		assertEquals(2845, Customer.CAPTAIN_BLEEMADGE.anchor().x);
		assertEquals(3501, Customer.CAPTAIN_BLEEMADGE.anchor().y);
		assertEquals(3278, Customer.CAPTAIN_DALBUR.anchor().x);
		assertEquals(2971, Customer.CAPTAIN_KLEMFOODLE.anchor().x);
		assertEquals(2540, Customer.GNORMADIUM_AVLAFRIM.anchor().x);
		assertEquals(2436, GnomeRestaurant.RESTAURANT.x);
		assertEquals(3502, GnomeRestaurant.RESTAURANT.y);
		// No coordinates for anyone else: they are learnt.
		assertNull(Customer.ROMETTI.anchor());
		assertNull(Customer.GARKOR.anchor());
	}

	@Test
	public void dishesByTheWiki()
	{
		// Level and experience, from the Gnome cooking table and each dish's page.
		assertEquals(10, Dish.TOAD_CRUNCHIES.level());
		assertEquals(100, Dish.TOAD_CRUNCHIES.xp());
		assertEquals(16, Dish.CHOCCHIP_CRUNCHIES.level());
		assertEquals(25, Dish.FRUIT_BATTA.level());
		assertEquals(150, Dish.FRUIT_BATTA.xp());
		assertEquals(28, Dish.VEGETABLE_BATTA.level());
		assertEquals(29, Dish.CHEESE_TOM_BATTA.level());
		assertEquals(158, Dish.CHEESE_TOM_BATTA.xp());
		assertEquals(42, Dish.CHOCOLATE_BOMB.level());
		assertEquals(190, Dish.CHOCOLATE_BOMB.xp());
		assertEquals(6, Dish.FRUIT_BLAST.level());
		assertEquals(37, Dish.BLURBERRY_SPECIAL.level());
		assertEquals(0, Dish.WIZARD_BLIZZARD.xp());
		// Finished item ids (OSRS wiki infoboxes).
		assertEquals(2259, Dish.CHEESE_TOM_BATTA.item());
		assertEquals(2277, Dish.FRUIT_BATTA.item());
		assertEquals(2255, Dish.TOAD_BATTA.item());
		assertEquals(2253, Dish.WORM_BATTA.item());
		assertEquals(2281, Dish.VEGETABLE_BATTA.item());
		assertEquals(2217, Dish.TOAD_CRUNCHIES.item());
		assertEquals(2213, Dish.SPICY_CRUNCHIES.item());
		assertEquals(2205, Dish.WORM_CRUNCHIES.item());
		assertEquals(2209, Dish.CHOCCHIP_CRUNCHIES.item());
		assertEquals(2191, Dish.WORM_HOLE.item());
		assertEquals(2195, Dish.VEG_BALL.item());
		assertEquals(2187, Dish.TANGLED_TOADS_LEGS.item());
		assertEquals(2185, Dish.CHOCOLATE_BOMB.item());
		assertEquals(2084, Dish.FRUIT_BLAST.item());
		assertEquals(2048, Dish.PINEAPPLE_PUNCH.item());
		assertEquals(2054, Dish.WIZARD_BLIZZARD.item());
		assertEquals(2080, Dish.SHORT_GREEN_GUY.item());
		assertEquals(2092, Dish.DRUNK_DRAGON.item());
		assertEquals(2074, Dish.CHOC_SATURDAY.item());
		assertEquals(2064, Dish.BLURBERRY_SPECIAL.item());
		Set<Integer> varbits = new HashSet<>();
		for (Dish d : Dish.values())
		{
			assertTrue(varbits.add(d.varbit()));
			assertEquals(d, Dish.ofVarbit(d.varbit()));
			assertEquals(d, Dish.named(d.getTitle()));
			List<Stage> stages = d.stages();
			assertEquals(d + " ends in its dish", d.item(), stages.get(stages.size() - 1).makes);
		}
		assertEquals(Dish.CHEESE_TOM_BATTA, Dish.named("cheese+tom batta"));
		assertEquals(Dish.SHORT_GREEN_GUY, Dish.named("Short green guy"));
	}

	@Test
	public void bakedStagesAndIds()
	{
		// Raw batta 2250, half baked batta 2249; half made batta (cheese+tom) 9483, unfinished 9484.
		List<Stage> s = Dish.CHEESE_TOM_BATTA.stages();
		assertEquals(5, s.size());
		assertEquals("Use gianne dough on a batta tin", s.get(0).text);
		assertEquals(2250, s.get(0).makes);
		assertEquals(Ingredient.GIANNE_DOUGH, s.get(0).uses.get(0));
		assertEquals(2171, Ingredient.GIANNE_DOUGH.item());
		assertTrue(s.get(1).range);
		assertEquals(2249, s.get(1).makes);
		assertEquals("Prepare the half baked batta with 1 cheese and 1 tomato", s.get(2).text);
		assertEquals(9483, s.get(2).makes);
		assertEquals(9484, s.get(3).makes);
		assertEquals("Add 1 equa leaves to finish the cheese+tom batta", s.get(4).text);
		// Half made / unfinished batta: fruit 9478/9479, worm 9480/9481, toad 9482 (baked straight into the toad batta), vegetable 9485/9486.
		assertEquals(9478, Dish.FRUIT_BATTA.stages().get(2).makes);
		assertEquals(9479, Dish.FRUIT_BATTA.stages().get(3).makes);
		assertEquals(9480, Dish.WORM_BATTA.stages().get(2).makes);
		assertEquals(9481, Dish.WORM_BATTA.stages().get(3).makes);
		assertEquals(9482, Dish.TOAD_BATTA.stages().get(2).makes);
		assertEquals(4, Dish.TOAD_BATTA.stages().size());
		assertEquals(9485, Dish.VEGETABLE_BATTA.stages().get(2).makes);
		assertEquals(9486, Dish.VEGETABLE_BATTA.stages().get(3).makes);
		// "Prepare a half made batta (fruit) with 4 equa leaves, 1 lime chunks, 1 orange chunks, and 1 pineapple chunks"; garnish gnome spice.
		Stage fruit = Dish.FRUIT_BATTA.stages().get(2);
		assertEquals(Integer.valueOf(4), fruit.counts.get(0));
		assertEquals(Ingredient.EQUA_LEAVES, fruit.uses.get(0));
		assertTrue(Dish.FRUIT_BATTA.stages().get(4).tools.contains(Ingredient.GNOME_SPICE));
		assertTrue(Dish.FRUIT_BATTA.stages().get(4).uses.isEmpty());
		// Crunchies: raw 2202, half baked 2201; half made / unfinished: toad 9581/9582, spicy 9579/9580, worm 9583/9584, chocchip 9577/9578.
		assertEquals(2202, Dish.TOAD_CRUNCHIES.stages().get(0).makes);
		assertEquals(2201, Dish.TOAD_CRUNCHIES.stages().get(1).makes);
		assertEquals(9581, Dish.TOAD_CRUNCHIES.stages().get(2).makes);
		assertEquals(9582, Dish.TOAD_CRUNCHIES.stages().get(3).makes);
		assertEquals(9579, Dish.SPICY_CRUNCHIES.stages().get(2).makes);
		assertEquals(9580, Dish.SPICY_CRUNCHIES.stages().get(3).makes);
		assertEquals(9583, Dish.WORM_CRUNCHIES.stages().get(2).makes);
		assertEquals(9584, Dish.WORM_CRUNCHIES.stages().get(3).makes);
		assertEquals(9577, Dish.CHOCCHIP_CRUNCHIES.stages().get(2).makes);
		assertEquals(9578, Dish.CHOCCHIP_CRUNCHIES.stages().get(3).makes);
		assertEquals(Ingredient.CHOCOLATE_DUST, Dish.CHOCCHIP_CRUNCHIES.stages().get(4).uses.get(0));
		// Gnomebowls: raw 2178, half baked 2177; half made / unfinished: worm hole 9559/9560, veg ball 9561/9562,
		// tangled toad's legs 9558 (baked into the dish), chocolate bomb 9563 (gameval; the wiki gives none)/9564.
		assertEquals(2178, Dish.WORM_HOLE.stages().get(0).makes);
		assertEquals(2177, Dish.WORM_HOLE.stages().get(1).makes);
		assertEquals(9559, Dish.WORM_HOLE.stages().get(2).makes);
		assertEquals(9560, Dish.WORM_HOLE.stages().get(3).makes);
		assertEquals(9561, Dish.VEG_BALL.stages().get(2).makes);
		assertEquals(9562, Dish.VEG_BALL.stages().get(3).makes);
		assertEquals(9558, Dish.TANGLED_TOADS_LEGS.stages().get(2).makes);
		assertEquals(4, Dish.TANGLED_TOADS_LEGS.stages().size());
		assertEquals(9563, Dish.CHOCOLATE_BOMB.stages().get(2).makes);
		assertEquals(9564, Dish.CHOCOLATE_BOMB.stages().get(3).makes);
		// "garnished with two pots of cream and chocolate dust".
		Stage bomb = Dish.CHOCOLATE_BOMB.stages().get(4);
		assertEquals(Ingredient.POT_OF_CREAM, bomb.uses.get(0));
		assertEquals(Integer.valueOf(2), bomb.counts.get(0));
		for (Dish d : Dish.values())
		{
			if (d.kind() != Kind.COCKTAIL)
			{
				assertTrue(d + " starts with dough", d.stages().get(0).uses.contains(Ingredient.GIANNE_DOUGH));
			}
		}
	}

	@Test
	public void cocktailStagesAndIds()
	{
		// Mixed blast 9568, punch 9569, blizzard 9566, sgg 9567, special 9570: "Mix-cocktail", then "Pour".
		assertEquals(9568, Dish.FRUIT_BLAST.stages().get(0).makes);
		assertEquals(9569, Dish.PINEAPPLE_PUNCH.stages().get(0).makes);
		assertEquals(9566, Dish.WIZARD_BLIZZARD.stages().get(0).makes);
		assertEquals(9567, Dish.SHORT_GREEN_GUY.stages().get(0).makes);
		assertEquals(9570, Dish.BLURBERRY_SPECIAL.stages().get(0).makes);
		assertEquals(2, Dish.FRUIT_BLAST.stages().size());
		assertTrue(Dish.FRUIT_BLAST.stages().get(0).uses.contains(Ingredient.COCKTAIL_SHAKER));
		assertTrue(Dish.FRUIT_BLAST.stages().get(1).uses.contains(Ingredient.COCKTAIL_GLASS));
		assertTrue(Dish.FRUIT_BLAST.stages().get(1).uses.contains(Ingredient.LEMON_SLICES));
		// "Creating a short green guy requires one vodka, three limes, one sliced lime, and one equa leaf."
		Stage sgg = Dish.SHORT_GREEN_GUY.stages().get(0);
		assertEquals(Integer.valueOf(3), sgg.counts.get(sgg.uses.indexOf(Ingredient.LIME)));
		// Mixed saturday (shaker) 9571, (poured) 9572, (heated) 9573.
		List<Stage> sat = Dish.CHOC_SATURDAY.stages();
		assertEquals(9571, sat.get(0).makes);
		assertEquals(9572, sat.get(1).makes);
		assertEquals(9573, sat.get(2).makes);
		assertTrue(sat.get(2).range);
		// Mixed dragon (shaker) 9574, (poured) 9575, (garnished) 9576, heated into the drunk dragon.
		List<Stage> dragon = Dish.DRUNK_DRAGON.stages();
		assertEquals(9574, dragon.get(0).makes);
		assertEquals(9575, dragon.get(1).makes);
		assertEquals(9576, dragon.get(2).makes);
		assertTrue(dragon.get(3).range);
		// Lime chunks 2122 ("using a knife on a lime"), lemon slices 2106.
		assertEquals(2122, Ingredient.LIME_CHUNKS.item());
		assertEquals(Ingredient.LIME, Ingredient.LIME_CHUNKS.cutFrom());
		assertEquals(2106, Ingredient.LEMON_SLICES.item());
		assertEquals("slices", Ingredient.LEMON_SLICES.cut());
		assertNull(Ingredient.CHEESE.cutFrom());
	}

	@Test
	public void messages()
	{
		// Transcript: Rometti; Transcript: Captain Bleemadge.
		GnomeRestaurant.HandOver h = GnomeRestaurant.handOver("You hand over your delivery of a cheese+tom batta to Rometti");
		assertNotNull(h);
		assertEquals(Dish.CHEESE_TOM_BATTA, h.dish);
		assertEquals(Customer.ROMETTI, h.customer);
		h = GnomeRestaurant.handOver("You hand over your delivery of some drunk dragon to Captain Bleemadge");
		assertEquals(Dish.DRUNK_DRAGON, h.dish);
		assertEquals(Customer.CAPTAIN_BLEEMADGE, h.customer);
		h = GnomeRestaurant.handOver("You hand over your delivery of Toad crunchies to G.L.O. Caranock");
		assertEquals(Dish.TOAD_CRUNCHIES, h.dish);
		assertEquals(Customer.GLO_CARANOCK, h.customer);
		assertNull(GnomeRestaurant.handOver("You hand over the planks."));
		assertEquals("ranarr weed", GnomeRestaurant.tip("You are given some ranarr weed as a tip."));
		assertEquals("5 grand seed pods", GnomeRestaurant.tip("You are given 5 grand seed pods as a tip."));
		assertNull(GnomeRestaurant.tip("You are given a job."));
		assertEquals(13, GnomeRestaurant.credits("You have 13 credits towards your free delivery token. You may get 1 pieces of gnome food delivered to you."));
		assertEquals(7, GnomeRestaurant.credits("You have 7 credits towards your free delivery token. You are not eligible to get gnome food delivered to you yet."));
		assertEquals(-1, GnomeRestaurant.credits("You have 7 coins."));
		assertEquals("5:59", GnomeRestaurant.clock(358_800));
		assertEquals("0:00", GnomeRestaurant.clock(-5));
	}
}
