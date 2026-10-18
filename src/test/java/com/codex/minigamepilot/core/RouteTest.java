package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class RouteTest
{
	private static int[][] open(int w, int h)
	{
		return new int[w][h];
	}

	@Test
	public void diagonalStepsCostTheSameAsStraightOnes()
	{
		Route r = Route.flood(open(10, 10), 0, 0, null);
		assertEquals(5, r.distance(5, 5));
		assertEquals(5, r.pathTo(5, 5).size());
	}

	@Test
	public void wallsAreWalkedAround()
	{
		int[][] f = open(10, 10);
		// A solid line at x = 3 from y = 0 to 7.
		for (int y = 0; y <= 7; y++)
		{
			f[3][y] = Route.FULL;
		}
		Route r = Route.flood(f, 0, 0, null);
		assertTrue(r.distance(5, 0) > 5);
		for (int[] t : r.pathTo(5, 0))
		{
			assertFalse("walked through the wall", t[0] == 3 && t[1] <= 7);
		}
	}

	@Test
	public void thinWallsBlockOneSideOnly()
	{
		int[][] f = open(4, 4);
		// A wall on the east edge of (1,1): stepping from (1,1) to (2,1) is blocked.
		f[1][1] |= Route.EAST;
		f[2][1] |= Route.WEST;
		assertEquals(0, Route.stepCost(f, null, 1, 1, 1, 0));
		assertEquals(1, Route.stepCost(f, null, 1, 1, 0, 1));
	}

	@Test
	public void softTilesCostExtraButCanBeCrossed()
	{
		int[][] f = open(10, 3);
		int[][] soft = new int[10][3];
		for (int y = 0; y < 3; y++)
		{
			f[5][y] = Route.FULL;
		}
		soft[5][1] = 8;
		Route blocked = Route.flood(f, 0, 1, null);
		assertFalse(blocked.reaches(9, 1));
		Route r = Route.flood(f, 0, 1, soft);
		assertEquals(9 + 8, r.distance(9, 1));
	}

	@Test
	public void approachPicksTheNearestSideAndNeverACorner()
	{
		int[][] f = open(10, 10);
		// A 2x2 object at (4,4).
		for (int x = 4; x < 6; x++)
		{
			for (int y = 4; y < 6; y++)
			{
				f[x][y] = Route.FULL;
			}
		}
		Route r = Route.flood(f, 0, 4, null);
		int[] stand = r.approach(f, 4, 4, 2, 2);
		assertNotNull(stand);
		assertEquals(3, stand[0]);
		assertTrue(stand[1] == 4 || stand[1] == 5);
		assertTrue(Route.beside(3, 4, 4, 4, 2, 2));
		assertFalse(Route.beside(3, 3, 4, 4, 2, 2));
	}

	@Test
	public void approachSkipsTilesAcrossAWall()
	{
		int[][] f = open(5, 5);
		f[2][2] = Route.FULL;
		// The west tile has a wall on its east edge, between it and the object.
		f[1][2] |= Route.EAST;
		Route r = Route.flood(f, 0, 2, null);
		int[] stand = r.approach(f, 2, 2, 1, 1);
		assertNotNull(stand);
		assertFalse(stand[0] == 1 && stand[1] == 2);
	}

	@Test
	public void unreachableGivesNothing()
	{
		int[][] f = open(5, 5);
		for (int y = 0; y < 5; y++)
		{
			f[2][y] = Route.FULL;
		}
		Route r = Route.flood(f, 0, 0, null);
		assertEquals(-1, r.distance(4, 4));
		List<int[]> path = r.pathTo(4, 4);
		assertTrue(path.isEmpty());
		assertNull(r.approach(f, 4, 4, 1, 1));
	}
}
