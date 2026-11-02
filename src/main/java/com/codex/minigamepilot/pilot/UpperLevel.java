package com.codex.minigamepilot.pilot;

/**
 * Whether the Motherlode Mine's upper level is open to this account, so the ladder is only
 * offered to a player who can climb it. The level needs 72 Mining and a nugget unlock the game
 * does not tell plugins about, so it is learnt by seeing the player up there, and saved.
 */
final class UpperLevel
{
	/** The Mining level the upper level asks for, on top of its nugget unlock. */
	static final int MINING = 72;
	/**
	 * Ticks in a row the player must read as upstairs before it counts. Tile height is how the two
	 * levels are told apart, and one odd reading (a scene still loading, a ladder tile) must not
	 * unlock the level for good.
	 */
	static final int SETTLE = 3;

	/** Saved as learnt for this account; null until read. */
	private Boolean learnt;
	private int upTicks;

	/** Another account, or a fresh read from the store. */
	void forget()
	{
		learnt = null;
		upTicks = 0;
	}

	/**
	 * One tick's reading.
	 *
	 * @param saved reads what the store holds for this account ("1" once learnt), or null; asked once
	 * @param upstairs the player reads as on the upper level this tick
	 * @param mining the player's Mining level, real or boosted, whichever is higher; 0 while unknown
	 * @param hopperSet the player turned on "Upper hopper unlocked", which needs the level first
	 * @return true the first time this tick makes the level learnt, so the caller saves it
	 */
	boolean observe(java.util.function.Supplier<String> saved, boolean upstairs, int mining, boolean hopperSet)
	{
		if (learnt == null)
		{
			learnt = "1".equals(saved.get());
		}
		upTicks = upstairs ? upTicks + 1 : 0;
		if (!learnt && mining >= MINING && (upTicks >= SETTLE || hopperSet))
		{
			learnt = true;
			return true;
		}
		return false;
	}

	/** Whether to send this player up the ladder. */
	boolean open(int mining)
	{
		return learnt != null && learnt && mining >= MINING;
	}
}
