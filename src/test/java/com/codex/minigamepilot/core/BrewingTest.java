package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarPlayerID;
import org.junit.Test;

/** Trouble Brewing's facts against the wiki's pages, and its ids against the wiki's infoboxes and RuneLite's gameval. */
public class BrewingTest
{
	@Test
	public void shopPrices()
	{
		// Honest Jimmy's House of Stuff: naval shirts and navy slacks 1,000, tricorn hats 500.
		assertEquals(1_000, Brewing.Piece.BLUE_SHIRT.cost());
		assertEquals(500, Brewing.Piece.GREY_HAT.cost());
		assertEquals(1_000, Brewing.Piece.PURPLE_SLACKS.cost());
		assertEquals(2_500, Brewing.Reward.NAVAL_RED.cost());
		// Cutthroat 2,000, Gilded smile 2,000, Bronze fist 3,000, Lucky shot 4,000, Treasure 5,000, Phasmatys 6,000.
		assertEquals(2_000, Brewing.Piece.CUTTHROAT_FLAG.cost());
		assertEquals(2_000, Brewing.Piece.GILDED_SMILE_FLAG.cost());
		assertEquals(3_000, Brewing.Piece.BRONZE_FIST_FLAG.cost());
		assertEquals(4_000, Brewing.Piece.LUCKY_SHOT_FLAG.cost());
		assertEquals(5_000, Brewing.Piece.TREASURE_FLAG.cost());
		assertEquals(6_000, Brewing.Piece.PHASMATYS_FLAG.cost());
		assertEquals(22_000, Brewing.Reward.FLAGS.cost());
		// "Buying every item in the shop costs 39,590": every piece plus Rum (red) 20, Rum (blue) 20, The stuff 50.
		assertEquals(39_590, Brewing.Reward.EVERYTHING.cost() + 20 + 20 + 50);
		assertEquals(27, Brewing.Reward.EVERYTHING.pieces().size());
		// The medium clue scroll ("map") "for 5,000 pieces of eight"; rum 20; the stuff 50; each bought again.
		assertEquals(5_000, Brewing.Reward.MEDIUM_CLUE.cost());
		assertEquals(20, Brewing.Reward.RUM.cost());
		assertEquals(50, Brewing.Reward.THE_STUFF.cost());
		assertTrue(Brewing.Reward.RUM.repeat());
		assertTrue(Brewing.Reward.RUM.pieces().isEmpty());
		assertFalse(Brewing.Reward.NAVAL_BLUE.repeat());
		assertEquals(3, Brewing.Reward.NAVAL_BLUE.pieces().size());
	}

	@Test
	public void rules()
	{
		// "you must have completed the Cabin Fever quest and have at least 40 Cooking."
		assertEquals(40, Brewing.COOKING_LEVEL);
		// "Each game is 20 minutes long, with an interval of 3 minutes between games."
		assertEquals(20, Brewing.GAME_MINUTES);
		assertEquals(3, Brewing.INTERVAL_MINUTES);
		// "10 pieces of eight are awarded per bottle of rum"; "up to 100 pieces of eight ... contribution".
		assertEquals(10, Brewing.PIECES_PER_RUM);
		assertEquals(100, Brewing.CONTRIBUTION_PIECES_MAX);
		// "at least three logs in each boiler"; 3 bowls of coloured water, 5 buckets of water a bottle.
		assertEquals(3, Brewing.BOILER_LOGS_MIN);
		assertEquals(3, Brewing.Ingredient.COLOURED_WATER.perBottle());
		assertEquals(5, Brewing.Ingredient.WATER.perBottle());
		assertEquals(1, Brewing.Ingredient.BITTERNUT.perBottle());
		assertEquals(1, Brewing.Ingredient.SWEETGRUB.perBottle());
		assertEquals(1, Brewing.Ingredient.SCRAPEY_BARK.perBottle());
	}

	@Test
	public void ids()
	{
		// Pirate bandana (Trouble Brewing) 8949, San Fan's (blue) team; Pirate hat 8950, Fancy Dan's (red).
		assertEquals(8949, Brewing.BLUE_HAT);
		assertEquals(8950, Brewing.RED_HAT);
		assertEquals(Brewing.Team.BLUE, Brewing.Team.byHat(8949));
		assertEquals(Brewing.Team.RED, Brewing.Team.byHat(8950));
		assertNull(Brewing.Team.byHat(ItemID.BREW_TRICORN_BLUE));
		assertEquals("San Fan", Brewing.Team.BLUE.leader());
		assertEquals("Fancy Dan", Brewing.Team.RED.leader());
		// Bitternut 8976.
		assertEquals(8976, BrewingCycle.BITTERNUT);
		// RuneLite: San Fan 1814, Fancy Dan 1815, Honest Jimmy 1816; brew_pieces 4218.
		assertEquals(1814, NpcID.SAN_FAN);
		assertEquals(1815, NpcID.FANCY_DAN);
		assertEquals(1816, Brewing.HONEST_JIMMY);
		assertEquals(4218, Brewing.PIECES);
		assertEquals(VarPlayerID.BREW_PIECES, Brewing.PIECES);
		assertEquals(15150, Brewing.REGION);
		// Each piece its own item and code.
		Set<Integer> items = new HashSet<>();
		Set<Character> codes = new HashSet<>();
		for (Brewing.Piece p : Brewing.Piece.values())
		{
			assertTrue(items.add(p.item()));
			assertTrue(codes.add(p.code()));
		}
		assertEquals(ItemID.BREW_FLAG_6, Brewing.Piece.PHASMATYS_FLAG.item());
		assertEquals("Grey navy slacks", Brewing.Piece.GREY_SLACKS.getTitle());
		assertEquals(ItemID.BREW_NAVY_SLACKS_GREY, Brewing.Piece.GREY_SLACKS.item());
	}

	@Test
	public void numbers()
	{
		assertEquals(14, Brewing.number("<col=ffffff>14</col>"));
		assertEquals(1235, Brewing.number("Pieces of eight: 1,235"));
		assertEquals(-1, Brewing.number(""));
		assertEquals(-1, Brewing.number(null));
		assertEquals("Next game: 2 mins", Brewing.plain("Next game:<br><col=ff0000>2 mins</col>"));
	}
}
