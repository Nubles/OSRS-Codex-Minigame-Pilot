package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Walking distances across the loaded scene, worked out from the game's own collision flags.
 * One flood from where the player stands gives the distance to every tile, so the nearest of
 * many targets (veins, hotspots) is the nearest on foot rather than as the crow flies, and a
 * target behind a wall loses to one around the corner.
 *
 * <p>Some tiles can be marked soft: blocked in the game, but passable here at a price. A rockfall
 * in the Motherlode Mine is one; a route through it means mining it first is the quicker way.
 */
public final class Route
{
	// The game's collision bits, as RuneLite's CollisionDataFlag names them.
	static final int NORTH_WEST = 0x1;
	static final int NORTH = 0x2;
	static final int NORTH_EAST = 0x4;
	static final int EAST = 0x8;
	static final int SOUTH_EAST = 0x10;
	static final int SOUTH = 0x20;
	static final int SOUTH_WEST = 0x40;
	static final int WEST = 0x80;
	static final int FULL = 0x100 | 0x40000 | 0x200000;

	private static final int[] DX = {0, 1, 0, -1, 1, 1, -1, -1};
	private static final int[] DY = {1, 0, -1, 0, 1, -1, -1, 1};
	private static final int UNREACHED = Integer.MAX_VALUE;

	private final int width;
	private final int height;
	private final int[] dist;
	private final int[] from;

	private Route(int width, int height)
	{
		this.width = width;
		this.height = height;
		this.dist = new int[width * height];
		this.from = new int[width * height];
		Arrays.fill(dist, UNREACHED);
		Arrays.fill(from, -1);
	}

	/**
	 * Floods out from a tile.
	 *
	 * @param flags the collision flags, indexed [x][y]
	 * @param soft extra cost of stepping onto each tile that is otherwise blocked, or null; zero means blocked
	 */
	public static Route flood(int[][] flags, int startX, int startY, int[][] soft)
	{
		int w = flags.length;
		int h = w == 0 ? 0 : flags[0].length;
		Route r = new Route(w, h);
		if (!r.inside(startX, startY))
		{
			return r;
		}
		PriorityQueue<long[]> open = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));
		int start = startX * h + startY;
		r.dist[start] = 0;
		open.add(new long[]{0, start});
		while (!open.isEmpty())
		{
			long[] head = open.poll();
			int at = (int) head[1];
			if (head[0] > r.dist[at])
			{
				continue;
			}
			int x = at / h;
			int y = at % h;
			for (int d = 0; d < 8; d++)
			{
				int nx = x + DX[d];
				int ny = y + DY[d];
				if (!r.inside(nx, ny))
				{
					continue;
				}
				int cost = stepCost(flags, soft, x, y, DX[d], DY[d]);
				if (cost <= 0)
				{
					continue;
				}
				int next = nx * h + ny;
				int nd = r.dist[at] + cost;
				if (nd < r.dist[next])
				{
					r.dist[next] = nd;
					r.from[next] = at;
					open.add(new long[]{nd, next});
				}
			}
		}
		return r;
	}

	/** The cost of one step, or zero when it cannot be taken. Diagonal steps cost the same tick as straight ones. */
	static int stepCost(int[][] flags, int[][] soft, int x, int y, int dx, int dy)
	{
		int nx = x + dx;
		int ny = y + dy;
		if (dx != 0 && dy != 0)
		{
			// A diagonal needs both straight steps open and the corner free.
			if (stepCost(flags, null, x, y, dx, 0) <= 0 || stepCost(flags, null, x, y, 0, dy) <= 0)
			{
				return 0;
			}
			int corner = dx > 0 ? (dy > 0 ? SOUTH_WEST : NORTH_WEST) : (dy > 0 ? SOUTH_EAST : NORTH_EAST);
			int side = (dx > 0 ? WEST : EAST) | (dy > 0 ? SOUTH : NORTH);
			if ((flags[nx][ny] & (FULL | corner | side)) != 0)
			{
				return softCost(flags, soft, nx, ny, corner | side);
			}
			return 1;
		}
		int wall = dx > 0 ? WEST : dx < 0 ? EAST : dy > 0 ? SOUTH : NORTH;
		if ((flags[nx][ny] & (FULL | wall)) != 0)
		{
			return softCost(flags, soft, nx, ny, wall);
		}
		return 1;
	}

	/** A blocked tile that is marked soft can still be crossed, unless a wall is in the way too. */
	private static int softCost(int[][] flags, int[][] soft, int x, int y, int walls)
	{
		if (soft == null || soft[x][y] <= 0 || (flags[x][y] & walls) != 0)
		{
			return 0;
		}
		return 1 + soft[x][y];
	}

	private boolean inside(int x, int y)
	{
		return x >= 0 && y >= 0 && x < width && y < height;
	}

	/** Steps to reach a tile, or -1 when it cannot be reached. */
	public int distance(int x, int y)
	{
		if (!inside(x, y))
		{
			return -1;
		}
		int d = dist[x * height + y];
		return d == UNREACHED ? -1 : d;
	}

	public boolean reaches(int x, int y)
	{
		return distance(x, y) >= 0;
	}

	/** The tiles walked from the start (not included) to a tile, or an empty list when unreachable. */
	public List<int[]> pathTo(int x, int y)
	{
		if (!reaches(x, y))
		{
			return Collections.emptyList();
		}
		List<int[]> path = new ArrayList<>();
		int at = x * height + y;
		while (from[at] >= 0)
		{
			path.add(new int[]{at / height, at % height});
			at = from[at];
		}
		Collections.reverse(path);
		return path;
	}

	/**
	 * Where to stand to use an object: the reachable tile beside its footprint that is fewest
	 * steps away, never a corner and never across a wall from it.
	 *
	 * @param flags the same flags the route was flooded over
	 * @return {x, y, steps}, or null when nothing beside it can be reached
	 */
	public int[] approach(int[][] flags, int ox, int oy, int sizeX, int sizeY)
	{
		int[] best = null;
		for (int x = ox - 1; x <= ox + sizeX; x++)
		{
			for (int y = oy - 1; y <= oy + sizeY; y++)
			{
				boolean insideX = x >= ox && x < ox + sizeX;
				boolean insideY = y >= oy && y < oy + sizeY;
				if (insideX == insideY)
				{
					// Either on the footprint itself or on one of its corners.
					continue;
				}
				int d = distance(x, y);
				if (d < 0 || (best != null && d >= best[2]))
				{
					continue;
				}
				int toward = x < ox ? EAST : x >= ox + sizeX ? WEST : y < oy ? NORTH : SOUTH;
				if ((flags[x][y] & toward) != 0)
				{
					continue;
				}
				best = new int[]{x, y, d};
			}
		}
		return best;
	}

	/** Whether standing on a tile already counts as being beside an object. */
	public static boolean beside(int px, int py, int ox, int oy, int sizeX, int sizeY)
	{
		boolean insideX = px >= ox && px < ox + sizeX;
		boolean insideY = py >= oy && py < oy + sizeY;
		if (insideX && insideY)
		{
			return false;
		}
		return (insideX && (py == oy - 1 || py == oy + sizeY)) || (insideY && (px == ox - 1 || px == ox + sizeX));
	}
}
