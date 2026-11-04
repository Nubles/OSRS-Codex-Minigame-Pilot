package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.codex.minigamepilot.core.Tithe.Seed;
import com.codex.minigamepilot.core.Tithe.State;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

/** The Tithe Farm's facts against the OSRS wiki's own figures. */
public class TitheTest
{
	@Test
	public void seedsForYourLevel()
	{
		// "Golovanova seed: Farming 34", "Bologano seed: Farming 54", "Logavano seed: Farming 74".
		assertNull(Seed.best(33));
		assertEquals(Seed.GOLOVANOVA, Seed.best(34));
		assertEquals(Seed.GOLOVANOVA, Seed.best(53));
		assertEquals(Seed.BOLOGANO, Seed.best(54));
		assertEquals(Seed.LOGAVANO, Seed.best(74));
		assertEquals(Seed.LOGAVANO, Seed.best(99));
		assertEquals(Tithe.LEVEL, Seed.GOLOVANOVA.level());
	}

	@Test
	public void aSackOfAHundredMatchesTheWikisTable()
	{
		// "Full sack (100)": 9,660, 22,540 and 37,030 experience without the outfit.
		assertEquals(9_660, Seed.GOLOVANOVA.sackXp());
		assertEquals(22_540, Seed.BOLOGANO.sackXp());
		assertEquals(37_030, Seed.LOGAVANO.sackXp());
		// The 75th fruit's bonus: 1,500, 3,500 and 5,750.
		assertEquals(1_500, Tithe.BONUS_TIMES * Seed.GOLOVANOVA.harvestXp());
		assertEquals(5_750, Tithe.BONUS_TIMES * Seed.LOGAVANO.harvestXp());
	}

	@Test
	public void pointsForASack()
	{
		// A point for every three, two more at the hundredth.
		assertEquals(0, Tithe.sackPoints(2));
		assertEquals(1, Tithe.sackPoints(3));
		assertEquals(33, Tithe.sackPoints(99));
		assertEquals(35, Tithe.sackPoints(100));
		assertEquals(35, Tithe.sackPoints(250));
	}

	@Test
	public void patchesReadFromTheirObjects()
	{
		assertEquals(State.EMPTY, Tithe.look(ObjectID.HOSIDIUS_TITHE_EMPTY).state);
		Tithe.Look l = Tithe.look(ObjectID.HOSIDIUS_TITHE_B_2_DRY);
		assertEquals(State.DRY, l.state);
		assertEquals(2, l.stage);
		assertEquals(Seed.BOLOGANO, l.seed);
		assertEquals(1, l.watered());
		l = Tithe.look(ObjectID.HOSIDIUS_TITHE_C_3_WET);
		assertEquals(State.WET, l.state);
		assertEquals(3, l.watered());
		assertEquals(State.DEAD, Tithe.look(ObjectID.HOSIDIUS_TITHE_A_1_DEAD).state);
		assertEquals(State.GROWN, Tithe.look(ObjectID.HOSIDIUS_TITHE_A_4).state);
		assertEquals(State.DEAD, Tithe.look(ObjectID.HOSIDIUS_TITHE_C_4_DEAD).state);
		assertNull(Tithe.look(ObjectID.TITHE_SACK_OF_FRUIT));
		// Every id from the empty patch to the last blighted logavano is a patch: 27383 to 27416.
		int[] ids = Tithe.patchIds();
		assertEquals(34, ids.length);
		for (int id = 27383; id <= 27416; id++)
		{
			assertEquals(id, ids[id - 27383]);
		}
	}

	@Test
	public void waterForTheWikisStrategies()
	{
		// Three doses a plant; eight to a can; "Requires 8+ filled watering can" for 20, 9 for 23, 10 for 25.
		assertEquals(60, Tithe.Plan.TWENTY.doses());
		assertEquals(8, Tithe.Plan.TWENTY.cans());
		assertEquals(9, Tithe.Plan.TWENTY_THREE.cans());
		assertEquals(10, Tithe.Plan.TWENTY_FIVE.cans());
		assertEquals(6, Tithe.Plan.SIXTEEN.cans());
		// Gricoller's can: "lasts 333 plants".
		assertEquals(333, Tithe.GRICOLLER_DOSES / Tithe.WATERINGS);
		assertEquals(1 + 2 * 2 + 8 * 3, Tithe.canDoses(new int[]{1, 2, 0, 0, 0, 0, 0, 3}));
	}

	@Test
	public void rewardPrices()
	{
		// "The outfit costs a total of 400 points"; strawhat 75, jacket or shirt 150, trousers 125, boots 50.
		int sum = 0;
		for (int i = 0; i < Tithe.Reward.FARMERS_OUTFIT.pieces(); i++)
		{
			sum += Tithe.Reward.FARMERS_OUTFIT.price(i);
		}
		assertEquals(400, sum);
		assertEquals(Tithe.Reward.FARMERS_OUTFIT.points(), sum);
		assertEquals(Tithe.Reward.STRAWHAT.points(), Tithe.Reward.FARMERS_OUTFIT.price(0));
		assertEquals(250, Tithe.Reward.SEED_BOX.points());
		assertEquals(250, Tithe.Reward.HERB_SACK.points());
		assertEquals(200, Tithe.Reward.GRICOLLERS_CAN.points());
	}
}
