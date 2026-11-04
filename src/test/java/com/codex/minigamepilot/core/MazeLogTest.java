package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.codex.minigamepilot.core.Maze.Leg;
import org.junit.Test;

public class MazeLogTest
{
	@Test
	public void keepsRunsAndWhereYouWereSentBack()
	{
		MazeLog g = new MazeLog();
		g.add(new MazeLog.Run(1, 200, true, null, MazeLog.Reward.CRATE, 900, 40));
		g.add(new MazeLog.Run(2, 100, false, Leg.GRILLS, MazeLog.Reward.CRATE, 200, 10));
		g.add(new MazeLog.Run(3, 300, true, Leg.SAFE, MazeLog.Reward.NONE, 900, 40));
		g.add(new MazeLog.Run(4, 50, false, Leg.GRILLS, MazeLog.Reward.NONE, 100, 0));
		assertEquals(4, g.count());
		assertEquals(2, g.finished());
		assertEquals(2, g.failed());
		// A run sent back gave nothing, whatever was asked.
		assertEquals(1, g.rewards(MazeLog.Reward.CRATE));
		assertEquals(Leg.GRILLS, g.worst());
		assertEquals(Integer.valueOf(2), g.sentBack().get(Leg.GRILLS));
		assertEquals(200, g.quickest().ticks);
		assertEquals(250, g.typical());
		// 650 ticks of maze, sent-back runs counted, for two attempts at the safe: 195 seconds each.
		assertEquals(195.0, g.secondsPerAttempt(), 1e-9);
		assertEquals(3600 / 195.0, g.perHour(), 1e-9);
		assertEquals(10 * 195.0 / 3600, g.hoursFor(10), 1e-9);
		assertEquals(2100 * 3600.0 / 390, g.agilityXpPerHour(), 1e-6);
	}

	@Test
	public void survivesASave()
	{
		MazeLog g = new MazeLog();
		g.add(new MazeLog.Run(1760000000000L, 210, true, Leg.SAFE, MazeLog.Reward.KIT, 1000, 50));
		g.add(new MazeLog.Run(1760000100000L, 0, false, null, MazeLog.Reward.NONE, 0, 0));
		MazeLog back = MazeLog.decode(g.encode());
		assertEquals(g.encode(), back.encode());
		assertEquals(MazeLog.Reward.KIT, back.all().get(0).reward);
		assertNull(back.all().get(1).leg);
		assertEquals(0, MazeLog.decode("2;1,2,1,SAFE,C,1,1").count());
		assertEquals(1, MazeLog.decode("1;1,2,1,SAFE,C,1,1;1,2,0,NOWHERE,-,1,1;x").count());
	}

	@Test
	public void noPaceWithoutARunToTheSafe()
	{
		MazeLog g = new MazeLog();
		assertEquals(-1, g.perHour(), 0);
		assertEquals(-1, g.hoursFor(3), 0);
		assertEquals(0, g.hoursFor(0), 0);
		g.add(new MazeLog.Run(1, 80, false, Leg.BARS, MazeLog.Reward.NONE, 50, 0));
		assertEquals(-1, g.secondsPerAttempt(), 0);
		assertNull(g.quickest());
		// Over the longest a run is timed: a break, not timed.
		assertEquals(0, new MazeLog.Run(1, MazeLog.LONGEST + 1, true, null, MazeLog.Reward.NONE, 0, 0).ticks);
	}
}
