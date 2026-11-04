package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class BrewingCycleTest
{
	private static BrewingCycle.Lobby ready()
	{
		BrewingCycle.Lobby s = new BrewingCycle.Lobby();
		s.cabinFever = true;
		s.cooking = 60;
		s.pieces = 0;
		return s;
	}

	@Test
	public void lobbyOrder()
	{
		BrewingCycle.Lobby s = ready();
		s.cabinFever = false;
		s.cooking = 10;
		assertEquals("Finish Cabin Fever", BrewingCycle.lobby(s).getTitle());
		s.cabinFever = true;
		assertEquals("Train Cooking to 40", BrewingCycle.lobby(s).getTitle());
		s.cooking = 40;
		s.carried = 2;
		s.hat = true;
		assertEquals("Bank your items and your hat", BrewingCycle.lobby(s).getTitle());
		s.carried = 0;
		assertEquals("Bank your hat", BrewingCycle.lobby(s).getTitle());
		s.follower = true;
		assertEquals("Leave your follower behind", BrewingCycle.lobby(s).getTitle());
		s.follower = false;
		s.hat = false;
		assertEquals("Join a team", BrewingCycle.lobby(s).getTitle());
		assertEquals(Step.Target.HONEST_JIMMY, BrewingCycle.lobby(s).getTarget());
		s.waiting = true;
		s.waitingText = "Next game: 1 min";
		assertTrue(BrewingCycle.lobby(s).isWaiting());
		assertEquals("Next game: 1 min", BrewingCycle.lobby(s).getDetail());
	}

	@Test
	public void buysOnceAffordable()
	{
		BrewingCycle.Lobby s = ready();
		s.goal = Brewing.Reward.FLAGS;
		s.goalCost = 22_000;
		s.nextPiece = Brewing.Piece.CUTTHROAT_FLAG;
		s.pieces = 1_999;
		assertEquals("Join a team", BrewingCycle.lobby(s).getTitle());
		s.pieces = 2_000;
		assertEquals("Buy the cutthroat flag", BrewingCycle.lobby(s).getTitle());
		// Rum, bought again and again, is left to the reward card.
		s.goal = Brewing.Reward.RUM;
		s.goalCost = 20;
		s.nextPiece = null;
		assertEquals("Join a team", BrewingCycle.lobby(s).getTitle());
	}

	private static BrewingCycle.Game carrying(int... idCount)
	{
		BrewingCycle.Game s = new BrewingCycle.Game();
		Map<Integer, Integer> m = new HashMap<>();
		for (int i = 0; i < idCount.length; i += 2)
		{
			m.put(idCount[i], idCount[i + 1]);
		}
		s.carried = m;
		return s;
	}

	@Test
	public void chain()
	{
		assertEquals("Put the sweetgrubs in the hopper", BrewingCycle.game(carrying(BrewingCycle.SWEETGRUBS, 1, BrewingCycle.FLOWERS[0], 1)).getTitle());
		assertEquals("Fletch the scrapey logs", BrewingCycle.game(carrying(BrewingCycle.SCRAPEY_LOGS, 1)).getTitle());
		assertEquals("Put the scrapey bark in the hopper", BrewingCycle.game(carrying(BrewingCycle.SCRAPEY_BARK, 1)).getTitle());
		assertEquals("Fetch coloured water for the monkey", BrewingCycle.game(carrying(BrewingCycle.MONKEY, 1)).getTitle());
		assertEquals("Dye the monkey", BrewingCycle.game(carrying(BrewingCycle.MONKEY, 1, BrewingCycle.BOWLS[1], 1)).getTitle());
		assertEquals("Use the monkey on the Bitternut tree", BrewingCycle.game(carrying(BrewingCycle.DYED_MONKEYS[4], 1)).getTitle());
		assertEquals("Put the flowers in your kettle", BrewingCycle.game(carrying(BrewingCycle.FLOWERS[1], 3)).getTitle());
		assertEquals("Put the coloured water in the hopper", BrewingCycle.game(carrying(BrewingCycle.BOWLS[0], 2)).getTitle());
		assertEquals("Put the water in the hopper", BrewingCycle.game(carrying(BrewingCycle.BUCKET_WATER, 2)).getTitle());
		assertEquals("Bring an ingredient for the still", BrewingCycle.game(carrying()).getTitle());
	}

	@Test
	public void shortestAndBoilers()
	{
		BrewingCycle.Game s = carrying();
		int[] hopper = {3, 2, 4, 5, 9};
		System.arraycopy(hopper, 0, s.hopper, 0, 5);
		// Water: 5 buckets is one bottle; sweetgrubs 2 is two; water is shortest.
		assertEquals("Fill buckets at the pump", BrewingCycle.game(s).getTitle());
		s.hopper[Brewing.Ingredient.COLOURED_WATER.ordinal()] = 2;
		assertEquals("Pick the other team's flowers", BrewingCycle.game(s).getTitle());
		s.boilers[1] = 2;
		s.boilers[0] = 5;
		assertEquals("Put logs in boiler 2", BrewingCycle.game(s).getTitle());
		// Carrying an ingredient comes before a cold boiler.
		s.carried = new HashMap<>();
		s.carried.put(BrewingCycle.BITTERNUT, 1);
		assertEquals("Put the bitternut in the hopper", BrewingCycle.game(s).getTitle());
	}
}
