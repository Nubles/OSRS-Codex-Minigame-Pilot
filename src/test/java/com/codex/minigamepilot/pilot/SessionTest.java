package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SessionTest
{
	@Test
	public void onlyALongBreakStartsASessionOver()
	{
		long t = 1_760_000_000_000L;
		assertFalse("the first tick starts a session, it does not restart one", Session.restarts(0, 0, t));
		assertFalse(Session.restarts(t, t + 60_000L, t + 120_000L));
		assertFalse(Session.restarts(t, t, t + Session.BREAK_MS));
		// Back after a long break: the counts kept beside the session must start over with it,
		// or two hours' haul is shown as earned in the first minute.
		assertTrue(Session.restarts(t, t, t + Session.BREAK_MS + 1));
	}

	@Test
	public void experienceIsOnlyTakenAsTheStartOnceTheSkillsHaveArrived()
	{
		long t = 1_760_000_000_000L;
		Session s = new Session(net.runelite.api.Skill.MINING);
		// Logged in inside the mine: the first tick reads no experience at all.
		s.tick(0, false, t);
		assertEquals(0, s.xp(0));
		s.tick(13_034_431, true, t + 600);
		assertEquals("not your whole total", 0, s.xp(13_034_431));
		s.tick(13_034_431, true, t + 1200);
		assertEquals(250, s.xp(13_034_681));
		// A tick where the skills read nothing again (logging out) shows nothing, not a loss.
		assertEquals(0, s.xp(0));
	}

	@Test
	public void anotherAccountStartsTheSessionOverEvenAfterAShortLogout()
	{
		long t = 1_760_000_000_000L;
		Session s = new Session(net.runelite.api.Skill.MINING);
		assertFalse(s.tick(1000, true, t));
		assertFalse(s.tick(2000, true, t + 600));
		// Logged out and into another account a minute later: without this the new account would
		// carry the last one's start, its counts and its hourly pace (and so its best visit).
		s.startOver();
		assertTrue(s.tick(50_000, true, t + 60_000L));
		assertEquals(t + 60_000L, s.startedAt());
		assertEquals(0, s.xp(50_000));
		assertFalse("only once", s.tick(50_100, true, t + 60_600L));
		// Before any session there is nothing to start over.
		Session fresh = new Session(net.runelite.api.Skill.MINING);
		fresh.startOver();
		assertFalse(fresh.tick(10, true, t));
	}

	@Test
	public void onlyALoginToAnotherAccountIsASwitch()
	{
		// The profile goes to none at every logout and back at login.
		assertFalse(Pilots.switched(null, "alpha"));
		assertFalse(Pilots.switched("alpha", null));
		assertFalse("back on the same account", Pilots.switched("alpha", "alpha"));
		assertTrue(Pilots.switched("alpha", "beta"));
	}

	@Test
	public void anotherAccountsSessionIsNotShownBeforeItsFirstTick()
	{
		// The card away from the mine reads the session's counts; between another account's login
		// and its first tick there it would have given the last account's pay-dirt as its own.
		Session s = new Session(net.runelite.api.Skill.MINING);
		s.tick(1000, true, 1_760_000_000_000L);
		assertTrue(s.begun());
		s.startOver();
		assertFalse(s.begun());
		assertEquals(0, s.startedAt());
	}
}
