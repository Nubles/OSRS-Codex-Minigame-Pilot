package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class LoginSettleTest
{
	@Test
	public void startedWhileLoggedInReadsAtOnce()
	{
		assertFalse(new LoginSettle().tick(false));
	}

	@Test
	public void waitsTwoTicksAfterLoggingIn()
	{
		LoginSettle s = new LoginSettle();
		s.relog();
		assertTrue(s.tick(true));
		assertTrue(s.tick(true));
		assertFalse(s.tick(true));
		assertFalse(s.tick(true));
	}

	@Test
	public void anInventoryThatNeverArrivesDoesNotSilenceTheLogin()
	{
		// Before, a tick with no inventory was never read, so an account that logged in with
		// nothing the game sent kept the Pilot and its field notes quiet until it picked
		// something up.
		LoginSettle s = new LoginSettle();
		s.relog();
		int quiet = 0;
		while (s.tick(false))
		{
			quiet++;
			assertTrue("must give up waiting", quiet < 100);
		}
		assertTrue(quiet >= LoginSettle.GRACE);
		assertFalse("settled for good", s.tick(false));
	}

	@Test
	public void goesOnAsSoonAsTheInventoryArrivesAfterTheGrace()
	{
		LoginSettle s = new LoginSettle();
		s.relog();
		assertTrue(s.tick(false));
		assertTrue(s.tick(false));
		assertTrue(s.tick(false));
		assertFalse(s.tick(true));
	}
}
