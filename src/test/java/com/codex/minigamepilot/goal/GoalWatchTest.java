package com.codex.minigamepilot.goal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class GoalWatchTest
{
	@Test
	public void readsPointsFromTheContractMessage()
	{
		assertEquals(1234, GoalWatch.readPoints("You have completed 310 contracts with a total of 1,234 points."));
		assertEquals(2, GoalWatch.readPoints("You have completed 1 contract with a total of 2 points."));
		assertEquals(-1, GoalWatch.readPoints("You have completed your contract."));
		assertEquals(-1, GoalWatch.readPoints(null));
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
}
