package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Assault.Role;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class AssaultTrackerTest
{
	private final AssaultTracker t = new AssaultTracker();
	private final List<AssaultLog.Game> kept = new ArrayList<>();
	private int tick = 100;
	private int[] points = {100, 200, 300, 400};

	private void step(Role horn, boolean complete)
	{
		kept.addAll(t.tick(tick++, horn, complete, points.clone(), 0));
	}

	/** A wave played to its end: the line, the horn for {@code ticks}, points, then out. */
	private void wave(int n, Role role, int ticks, int earned)
	{
		t.wave(n, tick);
		for (int i = 0; i < ticks; i++)
		{
			step(role, false);
		}
		points[role.ordinal()] += earned;
		step(null, true);
		step(null, false);
		step(null, false);
		// The lobby between waves.
		for (int i = 0; i < 20; i++)
		{
			step(null, false);
		}
	}

	@Test
	public void wholeGame()
	{
		step(null, false);
		for (int n = 1; n <= 9; n++)
		{
			wave(n, Role.COLLECTOR, 100, 10);
		}
		assertTrue(t.open());
		assertEquals(9, t.waves().size());
		assertEquals(90, t.points(Role.COLLECTOR));
		// The queen's wave: 80 in the role, 5 in each other (the wiki).
		t.wave(10, tick);
		for (int i = 0; i < 200; i++)
		{
			step(Role.COLLECTOR, false);
		}
		points[Role.COLLECTOR.ordinal()] += 80;
		points[Role.ATTACKER.ordinal()] += 5;
		points[Role.DEFENDER.ordinal()] += 5;
		points[Role.HEALER.ordinal()] += 5;
		step(null, true);
		for (int i = 0; i < 5; i++)
		{
			step(null, false);
		}
		assertFalse(t.open());
		assertEquals(1, kept.size());
		AssaultLog.Game g = kept.get(0);
		assertTrue(g.full());
		assertTrue(g.queen());
		assertEquals(Role.COLLECTOR, g.role());
		assertEquals(170, g.points(Role.COLLECTOR));
		assertEquals(5, g.points(Role.ATTACKER));
		assertEquals(185, g.totalPoints());
		assertEquals(100, g.waveTicks(1));
		assertEquals(200, g.waveTicks(10));
		assertEquals(10, t.won());
		assertTrue(g.ticks > 9 * 100 + 200);
	}

	@Test
	public void hornGoneWithoutPointsIsALoss()
	{
		step(null, false);
		t.wave(1, tick);
		for (int i = 0; i < 50; i++)
		{
			step(Role.ATTACKER, false);
		}
		step(null, false);
		step(null, false);
		assertEquals(-1, t.wave());
		assertFalse(t.waves().get(0).done);
		assertEquals(50, t.waves().get(0).ticks);
		// The same wave again carries on the game.
		wave(1, Role.ATTACKER, 60, 12);
		assertEquals(2, t.waves().size());
		assertTrue(t.waves().get(1).done);
		assertTrue(kept.isEmpty());
		// A new wave 1 after that is another game.
		t.wave(1, tick);
		step(Role.ATTACKER, false);
		assertEquals(1, kept.size());
		assertEquals(12, kept.get(0).points(Role.ATTACKER));
		assertFalse(kept.get(0).queen());
	}

	@Test
	public void pointsAfterTheHornMarkTheWaveWon()
	{
		step(null, false);
		t.wave(4, tick);
		for (int i = 0; i < 30; i++)
		{
			step(Role.HEALER, false);
		}
		step(null, false);
		step(null, false);
		assertFalse(t.waves().get(0).done);
		points[Role.HEALER.ordinal()] += 20;
		step(null, false);
		assertTrue(t.waves().get(0).done);
		assertEquals(20, t.points(Role.HEALER));
	}

	@Test
	public void spendingAndLoginReadsAreNotEarnings()
	{
		step(null, false);
		t.wave(1, tick);
		step(Role.DEFENDER, false);
		// A purchase: every role falls by 375.
		for (int i = 0; i < 4; i++)
		{
			points[i] -= 50;
		}
		step(Role.DEFENDER, false);
		// A jump far past any wave's points.
		points[0] += 2000;
		step(Role.DEFENDER, false);
		for (Role r : Role.values())
		{
			assertEquals(0, t.points(r));
		}
	}

	@Test
	public void idleEndsTheGame()
	{
		step(null, false);
		wave(1, Role.ATTACKER, 40, 9);
		for (int i = 0; i <= AssaultTracker.IDLE_TICKS; i++)
		{
			step(null, false);
		}
		assertEquals(1, kept.size());
		assertEquals(1, kept.get(0).reached());
	}

	@Test
	public void logoutEndsTheGameUntimedWave()
	{
		step(null, false);
		wave(1, Role.ATTACKER, 40, 9);
		t.wave(2, tick);
		step(Role.ATTACKER, false);
		AssaultLog.Game g = t.logout(tick, 5);
		assertNotNull(g);
		assertEquals(2, g.waves.size());
		assertEquals(0, g.waves.get(1).ticks);
		assertFalse(t.open());
		assertNull(t.logout(tick, 5));
	}

	@Test
	public void aWaveThatDoesNotFollowStartsAnotherGame()
	{
		step(null, false);
		wave(3, Role.COLLECTOR, 40, 9);
		wave(7, Role.COLLECTOR, 40, 9);
		assertEquals(1, kept.size());
		assertEquals(3, kept.get(0).first());
		assertEquals(1, t.waves().size());
	}
}
