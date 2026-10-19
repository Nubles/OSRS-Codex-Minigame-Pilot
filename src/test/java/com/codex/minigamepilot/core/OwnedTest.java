package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.EnumSet;
import org.junit.Test;

public class OwnedTest
{
	private static final EnumSet<Goal.Reward> NONE = EnumSet.noneOf(Goal.Reward.class);

	@Test
	public void aRewardSeenOnceStaysOwnedOnceItIsInTheCostumeRoom()
	{
		Owned o = new Owned();
		EnumSet<Goal.Reward> shirt = EnumSet.of(Goal.Reward.CARPENTERS_SHIRT);
		assertTrue(o.see(shirt, NONE, false));
		assertFalse("already remembered", o.see(shirt, NONE, false));
		// Put away in the costume room: out of sight, still owned.
		o.see(NONE, NONE, false);
		assertTrue(o.has(Goal.Reward.CARPENTERS_SHIRT));
		assertTrue(o.hasItem(24874));
		assertFalse(o.hasItem(24872));
	}

	@Test
	public void aRewardOutOfGroupStorageMayBeAGroupMatesAndIsNotRemembered()
	{
		Owned o = new Owned();
		EnumSet<Goal.Reward> saw = EnumSet.of(Goal.Reward.AMYS_SAW);
		assertFalse(o.see(saw, saw, true));
		// Still carried after the storage shuts: not remembered this session.
		assertFalse(o.see(saw, NONE, false));
		assertFalse(o.has(Goal.Reward.AMYS_SAW));
		// Your own, already remembered, is not taken back by passing through group storage.
		Owned mine = Owned.decode("AMYS_SAW");
		mine.see(saw, saw, true);
		assertTrue(mine.has(Goal.Reward.AMYS_SAW));
	}

	@Test
	public void savesNamesAndKeepsTheUnknownForTheVersionThatWroteThem()
	{
		Owned o = new Owned();
		o.see(EnumSet.of(Goal.Reward.PLANK_SACK, Goal.Reward.PROSPECTOR_HELMET, Goal.Reward.BIGGER_SACK), NONE, false);
		assertEquals("the bigger sack is owned by a varbit, not an item", "PROSPECTOR_HELMET,PLANK_SACK", o.encode());
		Owned read = Owned.decode("PLANK_SACK,NOT_A_REWARD,,bigger_sack");
		assertTrue(read.has(Goal.Reward.PLANK_SACK));
		assertFalse(read.has(Goal.Reward.BIGGER_SACK));
		// A name this version does not know counts for nothing, but is saved again as it was.
		assertEquals("PLANK_SACK,NOT_A_REWARD", read.encode());
	}
}
