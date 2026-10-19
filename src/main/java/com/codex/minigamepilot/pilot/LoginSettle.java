package com.codex.minigamepilot.pilot;

/**
 * The first ticks after logging in or hopping, while the inventory, skills and varbits arrive
 * from the game. The minigames are not read for a couple of ticks, and a little longer while the
 * inventory has not shown up, but never for good: an inventory that never arrives (an empty
 * pack, say) must not keep the Pilot, and its field notes, quiet for the rest of the login.
 */
public final class LoginSettle
{
	/** Ticks always waited after logging in; RuneLite's own XP tracker waits the same. */
	static final int GRACE = 2;
	/** The most ticks waited for the inventory before going on without it. */
	static final int MAX_WAIT = 10;

	/** Ticks since logging in, or -1 once settled. */
	private int ticks = -1;

	/** Logging in, or hopping: settle in again. */
	public void relog()
	{
		ticks = 0;
	}

	/**
	 * One tick logged in.
	 *
	 * @param inventoryArrived whether the game has sent the inventory yet
	 * @return true while still settling in, so this tick is not read
	 */
	public boolean tick(boolean inventoryArrived)
	{
		if (ticks < 0)
		{
			return false;
		}
		ticks++;
		if (ticks <= GRACE || !inventoryArrived && ticks < MAX_WAIT)
		{
			return true;
		}
		ticks = -1;
		return false;
	}
}
