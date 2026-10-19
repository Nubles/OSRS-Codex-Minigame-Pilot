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
}
