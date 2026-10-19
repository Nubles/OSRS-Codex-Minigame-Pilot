package com.codex.minigamepilot.pilot;

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
}
