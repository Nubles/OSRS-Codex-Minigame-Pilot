package com.codex.minigamepilot.goal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.codex.minigamepilot.core.Goal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class GoalWatchTest
{
	@Test
	public void readsPointsFromTheContractMessage()
	{
		assertArrayEquals(new long[]{1310, 1234}, GoalWatch.readPoints("You have completed 1,310 contracts with a total of 1,234 points."));
		assertArrayEquals(new long[]{1, 2}, GoalWatch.readPoints("You have completed 1 contract with a total of 2 points."));
		assertNull(GoalWatch.readPoints("You have completed your contract."));
		assertNull(GoalWatch.readPoints(null));
	}

	@Test
	public void aRewardFromNowhereWasBought()
	{
		Map<Integer, Integer> bank = new HashMap<>();
		EnumSet<Goal.Reward> none = EnumSet.noneOf(Goal.Reward.class);
		EnumSet<Goal.Reward> sack = EnumSet.of(Goal.Reward.PLANK_SACK);
		assertEquals(Collections.singletonList(Goal.Reward.PLANK_SACK), GoalWatch.bought(none, sack, sack, bank, false));
		// Not on the first tick seen, nor with the bank open.
		assertTrue(GoalWatch.bought(null, sack, sack, bank, false).isEmpty());
		assertTrue(GoalWatch.bought(none, sack, sack, bank, true).isEmpty());
		// Carried or worn a tick ago (taking off the boots), or in the bank as last seen.
		assertTrue(GoalWatch.bought(sack, sack, sack, bank, false).isEmpty());
		bank.put(24882, 1);
		assertTrue(GoalWatch.bought(none, sack, sack, bank, false).isEmpty());
	}

	@Test
	public void bankMemoryRoundTripsAndDropsJunk()
	{
		Map<Integer, Integer> counts = new HashMap<>();
		counts.put(12012, 87);
		counts.put(12013, 1);
		String saved = GoalWatch.encode(counts);
		assertEquals("12012=87;12013=1", saved);
		assertEquals(counts, GoalWatch.decode(saved));
		// Untracked items, damaged entries and empty counts are left out.
		Map<Integer, Integer> read = GoalWatch.decode("995=5000;12012=x;=4;12016=0;12019=1");
		assertEquals(1, read.size());
		assertTrue(read.containsKey(12019));
		assertTrue(GoalWatch.decode(null).isEmpty());
	}

	@Test
	public void nuggetsMovedOutOfTheBankAreNotEarned()
	{
		// 40 banked, none carried; withdraw all 40 and close the bank on the same tick.
		int before = 0 + 40;
		int after = 40 + 0;
		assertEquals(0, GoalWatch.nuggetsEarned(before, after, false));
		// A sack search adds 3 to the carried pile.
		assertEquals(3, GoalWatch.nuggetsEarned(after, after + 3, false));
		// Nothing counts while the bank is open, nor before the first tick.
		assertEquals(0, GoalWatch.nuggetsEarned(10, 50, true));
		assertEquals(0, GoalWatch.nuggetsEarned(-1, 50, false));
	}
}
