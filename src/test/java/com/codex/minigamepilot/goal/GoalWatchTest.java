package com.codex.minigamepilot.goal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
	public void aRewardFromNowhereAtTheShopWasBought()
	{
		Map<Integer, Integer> bank = new HashMap<>();
		EnumSet<Goal.Reward> none = EnumSet.noneOf(Goal.Reward.class);
		EnumSet<Goal.Reward> sack = EnumSet.of(Goal.Reward.PLANK_SACK);
		assertEquals(Collections.singletonList(Goal.Reward.PLANK_SACK), GoalWatch.bought(none, sack, sack, bank, false, true));
		// Not on the first tick seen, nor with a storage open.
		assertTrue(GoalWatch.bought(null, sack, sack, bank, false, true).isEmpty());
		assertTrue(GoalWatch.bought(none, sack, sack, bank, true, true).isEmpty());
		// Carried or worn a tick ago (taking off the boots), or in the bank as last seen.
		assertTrue(GoalWatch.bought(sack, sack, sack, bank, false, true).isEmpty());
		bank.put(24882, 1);
		assertTrue(GoalWatch.bought(none, sack, sack, bank, false, true).isEmpty());
	}

	@Test
	public void aRewardTakenOutOfGroupStorageOrTheCostumeRoomIsNotBought()
	{
		// The outfit was never in the bank the Pilot saw: it sat in group storage or the costume
		// room. Away from the reward shop it was only taken out, whatever else is open.
		Map<Integer, Integer> bank = new HashMap<>();
		EnumSet<Goal.Reward> none = EnumSet.noneOf(Goal.Reward.class);
		EnumSet<Goal.Reward> shirt = EnumSet.of(Goal.Reward.CARPENTERS_SHIRT);
		assertTrue(GoalWatch.bought(none, shirt, shirt, bank, false, false).isEmpty());
		assertTrue(GoalWatch.bought(none, shirt, shirt, bank, true, false).isEmpty());
	}

	@Test
	public void theShopCountsForAFewTicksAfterItCloses()
	{
		assertTrue(GoalWatch.atShop(100, 100));
		assertTrue(GoalWatch.atShop(100, 100 + GoalWatch.SHOP_GRACE));
		assertFalse(GoalWatch.atShop(100, 101 + GoalWatch.SHOP_GRACE));
		assertFalse(GoalWatch.atShop(-1, 5));
		// Logged in again: the tick count started over below the old shop tick.
		assertFalse(GoalWatch.atShop(1000, 2));
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

	@Test
	public void experienceBeforeTheSkillsArriveIsNoBaseForAGain()
	{
		// Just logged in: no Hitpoints experience means nothing has loaded.
		assertEquals(-1, GoalWatch.known(0, 0));
		assertEquals(-1, GoalWatch.known(13_034_431, 0));
		assertEquals(13_034_431, GoalWatch.known(13_034_431, 1_154));
		// From not known to your whole total is no gain; a real tick's gain is.
		assertEquals(0, GoalWatch.xpGained(-1, 13_034_431));
		assertEquals(0, GoalWatch.xpGained(13_034_431, -1));
		assertEquals(35, GoalWatch.xpGained(13_034_431, 13_034_466));
	}

	@Test
	public void nuggetsAreNotEarnedFromAnInventoryNotYetLoaded()
	{
		assertEquals(0, GoalWatch.nuggetsEarned(-1, 40, false));
		assertEquals(0, GoalWatch.nuggetsEarned(40, -1, false));
		assertEquals(0, GoalWatch.nuggetsEarned(-1, -1, false));
		assertEquals(2, GoalWatch.nuggetsEarned(40, 42, false));
	}

	@Test
	public void aDamagedBankCopyKeepsWhatReads()
	{
		// goal.bank damaged every way the suite's fuzz test damages a save: no throw, no count below one.
		for (String saved : com.codex.minigamepilot.core.SavedKeysFuzzTest.damaged("12012=5000;12013=1;25549=1"))
		{
			for (Integer n : GoalWatch.decode(saved).values())
			{
				assertTrue(saved, n > 0);
			}
			GoalWatch.decode(GoalWatch.encode(GoalWatch.decode(saved)));
		}
	}
}
