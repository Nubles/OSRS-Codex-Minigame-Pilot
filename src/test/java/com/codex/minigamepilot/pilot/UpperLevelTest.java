package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class UpperLevelTest
{
	@Test
	public void aFreshAccountIsNotSentUp()
	{
		UpperLevel u = new UpperLevel();
		assertFalse(u.observe(() -> null, false, 80, false));
		assertFalse(u.open(80));
	}

	@Test
	public void oneOddReadingDoesNotUnlockIt()
	{
		// A player downstairs whose tile reads as upstairs for a tick, as the scene loads,
		// was sent to the ladder for good before.
		UpperLevel u = new UpperLevel();
		assertFalse(u.observe(() -> null, true, 80, false));
		u.observe(() -> null, false, 80, false);
		u.observe(() -> null, true, 80, false);
		assertFalse(u.open(80));
	}

	@Test
	public void beingUpThereLearnsItOnce()
	{
		UpperLevel u = new UpperLevel();
		assertFalse(u.observe(() -> null, true, 80, false));
		assertFalse(u.observe(() -> null, true, 80, false));
		assertTrue(u.observe(() -> null, true, 80, false));
		assertFalse(u.observe(() -> null, true, 80, false));
		assertTrue(u.open(80));
	}

	@Test
	public void underSeventyTwoMiningItIsNeverOffered()
	{
		// The player in the report: 40-odd Mining, sent up a ladder they cannot use.
		UpperLevel u = new UpperLevel();
		assertFalse(u.observe(() -> "1", false, 45, false));
		assertFalse(u.open(45));
		for (int i = 0; i < 5; i++)
		{
			assertFalse(u.observe(() -> null, true, 45, true));
		}
		assertFalse(new UpperLevel().open(45));
	}

	@Test
	public void aSavedUnlockOpensItWithTheLevel()
	{
		UpperLevel u = new UpperLevel();
		assertFalse(u.observe(() -> "1", false, 72, false));
		assertTrue(u.open(72));
	}

	@Test
	public void anUnknownLevelSendsNobodyUp()
	{
		UpperLevel u = new UpperLevel();
		u.observe(() -> "1", false, 0, false);
		assertFalse(u.open(0));
	}

	@Test
	public void theHopperSettingImpliesTheLevel()
	{
		UpperLevel u = new UpperLevel();
		assertTrue(u.observe(() -> null, false, 75, true));
		assertTrue(u.open(75));
	}

	@Test
	public void anotherAccountReadsItsOwnStore()
	{
		UpperLevel u = new UpperLevel();
		u.observe(() -> "1", false, 80, false);
		u.forget();
		u.observe(() -> null, false, 80, false);
		assertFalse(u.open(80));
	}
}
