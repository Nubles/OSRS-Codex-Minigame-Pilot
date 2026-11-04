package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.MixCycle.Decision;
import com.codex.minigamepilot.core.MixCycle.Order;
import com.codex.minigamepilot.core.MixCycle.Snapshot;
import com.codex.minigamepilot.core.Mixology.Paste;
import com.codex.minigamepilot.core.Mixology.Potion;
import com.codex.minigamepilot.core.Mixology.Station;
import java.util.Arrays;
import org.junit.Test;

public class MixCycleTest
{
	private static Snapshot lab(int level, Order... orders)
	{
		Snapshot s = new Snapshot();
		s.level = level;
		for (int i = 0; i < orders.length; i++)
		{
			s.orders[i] = orders[i];
		}
		Arrays.fill(s.hopper, 1000);
		return s;
	}

	private static Order order(Potion p, Station st)
	{
		return new Order(p, st, false);
	}

	@Test
	public void leversInTheRecipesOrder()
	{
		Snapshot s = lab(81, order(Potion.MMA, Station.RETORT), order(Potion.ALA, Station.AGITATOR), order(Potion.MAL, Station.ALEMBIC));
		Decision d = MixCycle.next(s);
		assertEquals(Arrays.asList(0, 1, 2), d.planned);
		assertEquals(Step.Target.LEVER, d.step.getTarget());
		assertEquals(Paste.MOX, d.lever);
		assertEquals("Pull the mox lever", d.step.getTitle());
		assertTrue(d.step.getDetail(), d.step.getDetail().startsWith("Mystic mana amalgam · Mox Mox Aga · 3 pulls"));

		s.slots[0] = Paste.MOX;
		s.slots[1] = Paste.MOX;
		d = MixCycle.next(s);
		assertEquals(Paste.AGA, d.lever);
		assertTrue(d.step.getDetail().endsWith("last pull"));

		// Mox and lye in the vessel: the mixalot takes both, so it is mixed first.
		s.slots[1] = Paste.LYE;
		d = MixCycle.next(s);
		assertEquals(Potion.MAL, d.potion);
		assertEquals(Paste.AGA, d.lever);
		// Lye twice fits no order: the first order's whole recipe again, no paste lost.
		s.slots[0] = Paste.LYE;
		d = MixCycle.next(s);
		assertEquals(Paste.MOX, d.lever);
		assertTrue(d.step.getDetail().contains("pull all three again"));
	}

	@Test
	public void theOrderTheVesselAlreadyFitsIsMixedFirst()
	{
		Snapshot s = lab(81, order(Potion.MMA, Station.RETORT), order(Potion.ALA, Station.AGITATOR), order(Potion.MAL, Station.ALEMBIC));
		s.bases[Potion.MMA.ordinal()] = 1;
		// Lye in the vessel: the aqualux amalgam (A L A) takes it, so its levers come next, not a reset.
		s.slots[0] = Paste.LYE;
		Decision d = MixCycle.next(s);
		assertEquals(Potion.ALA, d.potion);
		assertEquals(Paste.AGA, d.lever);
		// Mox in the vessel: only the mixalot takes it.
		s.slots[0] = Paste.MOX;
		d = MixCycle.next(s);
		assertEquals(Potion.MAL, d.potion);
		assertEquals(Paste.AGA, d.lever);
		assertTrue(MixCycle.fits(new Paste[3], Potion.MMM));
	}

	@Test
	public void takeThenStationThenConveyor()
	{
		Snapshot s = lab(81, order(Potion.MMA, Station.RETORT), order(Potion.ALA, Station.AGITATOR), order(Potion.MAL, Station.ALEMBIC));
		s.vessel = Potion.MMA;
		Decision d = MixCycle.next(s);
		assertEquals(Step.Target.MIXING_VESSEL, d.step.getTarget());
		assertEquals("Take the mystic mana amalgam from the vessel", d.step.getTitle());

		// All three bases carried: the alembic first, then the retort, then the agitator.
		s.vessel = null;
		s.bases[Potion.MMA.ordinal()] = 1;
		s.bases[Potion.ALA.ordinal()] = 1;
		s.bases[Potion.MAL.ordinal()] = 1;
		d = MixCycle.next(s);
		assertEquals(Step.Target.ALEMBIC, d.step.getTarget());
		assertEquals("Crystallise the mixalot at the alembic", d.step.getTitle());

		// It is in the alembic: wait for the bellows, then click on the cue.
		s.bases[Potion.MAL.ordinal()] = 0;
		s.stations[Station.ALEMBIC.ordinal()] = Potion.MAL;
		d = MixCycle.next(s);
		assertTrue(d.step.isWaiting());
		s.cue = Station.ALEMBIC;
		assertEquals("Click the alembic now", MixCycle.next(s).step.getTitle());

		// Done; the retort next, then the agitator.
		s.cue = null;
		s.stations[Station.ALEMBIC.ordinal()] = null;
		s.finished[Potion.MAL.ordinal()] = 1;
		s.orders[2] = new Order(Potion.MAL, Station.ALEMBIC, true);
		assertEquals(Step.Target.RETORT, MixCycle.next(s).step.getTarget());
		s.stations[Station.RETORT.ordinal()] = Potion.MMA;
		s.bases[Potion.MMA.ordinal()] = 0;
		assertEquals("Keep clicking the retort", MixCycle.next(s).step.getTitle());
		s.stations[Station.RETORT.ordinal()] = null;
		s.finished[Potion.MMA.ordinal()] = 1;
		s.orders[0] = new Order(Potion.MMA, Station.RETORT, true);
		assertEquals(Step.Target.AGITATOR, MixCycle.next(s).step.getTarget());
		s.bases[Potion.ALA.ordinal()] = 0;
		s.finished[Potion.ALA.ordinal()] = 1;
		s.orders[1] = new Order(Potion.ALA, Station.AGITATOR, true);
		d = MixCycle.next(s);
		assertEquals(Step.Target.CONVEYOR, d.step.getTarget());
		assertEquals("All three orders at once: 40% more resin", d.step.getDetail());
	}

	@Test
	public void pasteDecidesWhichOrders()
	{
		Snapshot s = lab(81, order(Potion.LLL, Station.RETORT), order(Potion.MMA, Station.AGITATOR), order(Potion.MLL, Station.ALEMBIC));
		s.hopper[Paste.LYE.ordinal()] = 30;
		// 30 lye: either liplack liquor (30) or megalite liquid (20); the bigger resin is kept.
		Decision d = MixCycle.next(s);
		assertEquals(Arrays.asList(1, 2), d.planned);
		// No lye at all: only the mystic mana amalgam.
		s.hopper[Paste.LYE.ordinal()] = 0;
		assertEquals(Arrays.asList(1), MixCycle.plan(s));
		// No paste for any of them: the hopper, the refiner or the bank, for the paste shortest.
		Arrays.fill(s.hopper, 0);
		d = MixCycle.next(s);
		assertEquals(Step.Target.BANK, d.step.getTarget());
		assertEquals(Paste.LYE, MixCycle.shortest(s));
		s.herbPaste[Paste.LYE.ordinal()] = 300;
		assertEquals(Step.Target.REFINER, MixCycle.next(s).step.getTarget());
		s.pasteCarried[Paste.LYE.ordinal()] = 300;
		d = MixCycle.next(s);
		assertEquals(Step.Target.HOPPER, d.step.getTarget());
		assertTrue(d.step.hasItem(Paste.LYE.item()));
	}

	@Test
	public void potionsAlreadyMixedAreNotPaidForTwice()
	{
		Snapshot s = lab(81, order(Potion.LLL, Station.RETORT), order(Potion.MMA, Station.AGITATOR), order(Potion.MLL, Station.ALEMBIC));
		Arrays.fill(s.hopper, 0);
		s.bases[Potion.LLL.ordinal()] = 1;
		s.bases[Potion.MMA.ordinal()] = 1;
		// Two carried, one with no paste for it: two orders, at their stations.
		Decision d = MixCycle.next(s);
		assertEquals(Arrays.asList(0, 1), d.planned);
		assertEquals(Step.Target.RETORT, d.step.getTarget());
	}

	@Test
	public void mostResinPerPasteSkipsSingles()
	{
		Snapshot s = lab(80, order(Potion.MMM, Station.RETORT), order(Potion.MMA, Station.AGITATOR), order(Potion.AAA, Station.ALEMBIC));
		s.plan = MixCycle.Plan.PER_PASTE;
		assertEquals(Arrays.asList(1), MixCycle.plan(s));
		// A mixalot is ordered: every order is worth it (wiki strategies).
		s.level = 81;
		s.orders[1] = order(Potion.MAL, Station.AGITATOR);
		assertEquals(Arrays.asList(0, 1, 2), MixCycle.plan(s));
		// Only singles: one, of the paste there is most of, to get new orders.
		s.orders[1] = order(Potion.LLL, Station.AGITATOR);
		s.hopper[Paste.LYE.ordinal()] = 2000;
		assertEquals(Arrays.asList(1), MixCycle.plan(s));
		// Every order, by the plain plan.
		s.plan = MixCycle.Plan.ALL;
		assertEquals(Arrays.asList(0, 1, 2), MixCycle.plan(s));
	}

	@Test
	public void ordersAboveYourLevelAreLeft()
	{
		Snapshot s = lab(70, order(Potion.MAL, Station.RETORT), order(Potion.MMA, Station.AGITATOR), null);
		assertEquals(Arrays.asList(1), MixCycle.plan(s));
	}

	@Test
	public void digweedPickedAndUsedWhereItPays()
	{
		Snapshot s = lab(81, order(Potion.MMM, Station.RETORT), order(Potion.MAL, Station.AGITATOR), order(Potion.AAM, Station.ALEMBIC));
		s.digweedReady = true;
		Decision d = MixCycle.next(s);
		assertEquals(Step.Target.DIGWEED, d.step.getTarget());
		s.digweedReady = false;
		s.digweed = true;
		// Held, no mixalot carried yet: carry on mixing.
		assertEquals(Step.Target.LEVER, MixCycle.next(s).step.getTarget());
		s.bases[Potion.MAL.ordinal()] = 1;
		s.bases[Potion.AAM.ordinal()] = 1;
		d = MixCycle.next(s);
		assertEquals("Use the digweed on the mixalot", d.step.getTitle());
		assertTrue(d.step.hasItem(Potion.MAL.unfinished()));
		// Below 81: the carried ordered potion with the most experience, never a single.
		s.level = 80;
		s.orders[1] = order(Potion.ALL, Station.AGITATOR);
		s.bases[Potion.ALL.ordinal()] = 1;
		s.bases[Potion.MMM.ordinal()] = 1;
		assertEquals(Potion.ALL, MixCycle.digweedTarget(s, MixCycle.plan(s)));
		// A potion at a station keeps you there: the digweed waits.
		s.stations[Station.RETORT.ordinal()] = Potion.MMM;
		assertEquals(Step.Target.RETORT, MixCycle.next(s).step.getTarget());
	}

	@Test
	public void agitatorCue()
	{
		Snapshot s = lab(81, order(Potion.MMA, Station.AGITATOR), null, null);
		s.stations[Station.AGITATOR.ordinal()] = Potion.MMA;
		assertTrue(MixCycle.next(s).step.isWaiting());
		s.cue = Station.AGITATOR;
		Decision d = MixCycle.next(s);
		assertEquals("Click the agitator now", d.step.getTitle());
		assertEquals("Click", d.step.getVerb());
	}

	@Test
	public void noOrdersShownMeansPaste()
	{
		Snapshot s = lab(81);
		Decision d = MixCycle.next(s);
		assertTrue(d.planned.isEmpty());
		assertNull(MixCycle.shortest(s));
		assertEquals(Step.Target.BANK, d.step.getTarget());
	}

	@Test
	public void stagesOfTheLoop()
	{
		assertEquals(0, MixCycle.stage(Step.of("x", "x", "", Step.Target.REFINER)));
		assertEquals(1, MixCycle.stage(Step.of("x", "x", "", Step.Target.LEVER)));
		assertEquals(2, MixCycle.stage(Step.of("x", "x", "", Step.Target.ALEMBIC)));
		assertEquals(3, MixCycle.stage(Step.of("x", "x", "", Step.Target.CONVEYOR)));
		assertEquals(-1, MixCycle.stage(Step.of("x", "x", "", Step.Target.DIGWEED)));
		assertEquals("Mox Aga Lye", MixCycle.recipe(Potion.MAL));
		assertEquals("Aga Lye Aga", MixCycle.recipe(Potion.ALA));
	}
}
