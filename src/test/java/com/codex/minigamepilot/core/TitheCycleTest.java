package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Tithe.Seed;
import com.codex.minigamepilot.core.Tithe.State;
import org.junit.Test;

public class TitheCycleTest
{
	/** A farm of eight empty patches, with 80 Farming, logavano seeds and eight full cans. */
	private static TitheCycle.Snapshot farm()
	{
		TitheCycle.Snapshot s = new TitheCycle.Snapshot();
		s.inFarm = true;
		s.level = 80;
		s.seeds[Seed.LOGAVANO.ordinal()] = 100;
		s.doses = 64;
		s.anyCan = true;
		s.barrel = true;
		for (int i = 0; i < 8; i++)
		{
			s.plots.add(new TitheCycle.Plot());
			s.toPlant.add(i);
		}
		return s;
	}

	private static void plant(TitheCycle.Snapshot s, int i, State state, int stage, int ticksLeft)
	{
		TitheCycle.Plot p = s.plots.get(i);
		p.state = state;
		p.stage = stage;
		p.ticksLeft = ticksLeft;
		if (!s.round.contains(i))
		{
			s.round.add(i);
			s.toPlant.remove((Integer) i);
		}
	}

	@Test
	public void plantThenWaterAtOnce()
	{
		TitheCycle.Snapshot s = farm();
		TitheCycle.Decision d = TitheCycle.next(s);
		assertEquals("Plant 1 of 20", d.step.getTitle());
		assertEquals(0, d.plot);
		assertTrue(d.step.hasItem(Seed.LOGAVANO.seed()));
		plant(s, 0, State.DRY, 1, 100);
		d = TitheCycle.next(s);
		assertEquals("Water 1 of 1 (first)", d.step.getTitle());
		plant(s, 0, State.WET, 1, 98);
		d = TitheCycle.next(s);
		assertEquals("Plant 2 of 20", d.step.getTitle());
		assertEquals(1, d.plot);
	}

	@Test
	public void aWiltingPlantComesFirst()
	{
		TitheCycle.Snapshot s = farm();
		plant(s, 0, State.DRY, 2, 60);
		plant(s, 1, State.DRY, 2, 20);
		plant(s, 2, State.WET, 1, 80);
		TitheCycle.Decision d = TitheCycle.next(s);
		assertEquals(1, d.plot);
		assertEquals("Only 0:12 left before it wilts", d.step.getDetail());
	}

	@Test
	public void roundsAreSizedByWhatYouCarry()
	{
		TitheCycle.Snapshot s = farm();
		s.seeds[Seed.LOGAVANO.ordinal()] = 5;
		assertEquals(5, TitheCycle.roomInRound(s));
		s.seeds[Seed.LOGAVANO.ordinal()] = 100;
		s.doses = 30;
		s.barrel = false;
		// Thirty doses water ten plants three times.
		assertEquals(10, TitheCycle.roomInRound(s));
		assertEquals("Plant 1 of 10", TitheCycle.next(s).step.getTitle());
		// With a barrel in sight, a round is not begun short of water.
		s.barrel = true;
		assertEquals("Fill your watering cans", TitheCycle.next(s).step.getTitle());
	}

	@Test
	public void waterBeforeHarvestThenClearThenDeposit()
	{
		TitheCycle.Snapshot s = farm();
		s.plan = Tithe.Plan.SIXTEEN;
		plant(s, 0, State.GROWN, 4, -1);
		plant(s, 1, State.DRY, 3, 70);
		plant(s, 2, State.DEAD, 2, -1);
		assertEquals("Water 2 of 3 (third)", TitheCycle.next(s).step.getTitle());
		plant(s, 1, State.GROWN, 4, -1);
		assertEquals("Harvest 1 of 3", TitheCycle.next(s).step.getTitle());
		plant(s, 0, State.EMPTY, 0, -1);
		plant(s, 1, State.EMPTY, 0, -1);
		TitheCycle.Decision d = TitheCycle.next(s);
		assertEquals("Clear the blighted plant", d.step.getTitle());
		assertEquals(2, d.plot);
		plant(s, 2, State.EMPTY, 0, -1);
		s.round.clear();
		s.fruit = 2;
		s.sack = 98;
		d = TitheCycle.next(s);
		assertEquals("Deposit the fruit", d.step.getTitle());
		assertEquals("It fills the sack at 100: 35 points with the bonus", d.step.getDetail());
	}

	@Test
	public void outOfWaterWithPlantsGrowing()
	{
		TitheCycle.Snapshot s = farm();
		plant(s, 0, State.DRY, 2, 60);
		s.doses = 0;
		assertEquals(Step.Target.WATER_BARREL, TitheCycle.next(s).step.getTarget());
		s.gricoller = true;
		assertEquals("Fill Gricoller's can", TitheCycle.next(s).step.getTitle());
	}

	@Test
	public void atTheSeedTable()
	{
		TitheCycle.Snapshot s = farm();
		s.inFarm = false;
		s.level = 60;
		s.seeds[Seed.LOGAVANO.ordinal()] = 0;
		TitheCycle.Decision d = TitheCycle.next(s);
		assertEquals("Take bologano seeds", d.step.getTitle());
		assertEquals(Step.Target.SEED_TABLE, d.step.getTarget());
		// Boosted to 74 before taking seeds: logavano.
		s.level = 74;
		assertEquals("Take logavano seeds", TitheCycle.next(s).step.getTitle());
		s.seeds[Seed.LOGAVANO.ordinal()] = 100;
		assertEquals("Enter the farm", TitheCycle.next(s).step.getTitle());
		s.level = 20;
		assertTrue(TitheCycle.next(s).step.isWaiting());
	}
}
