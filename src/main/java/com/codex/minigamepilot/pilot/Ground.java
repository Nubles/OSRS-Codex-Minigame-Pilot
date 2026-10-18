package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Route;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.ToIntFunction;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/**
 * The ground under the player this tick: walking distances from where they stand to every tile
 * in the loaded scene, and the way to the nearest of a set of objects or characters.
 */
final class Ground
{
	/** The way to something: what it is, where to stand, and the tiles to walk. */
	static final class Way<T>
	{
		final T thing;
		final int steps;
		final List<WorldPoint> path;
		final List<int[]> scenePath;

		Way(T thing, int steps, List<WorldPoint> path, List<int[]> scenePath)
		{
			this.thing = thing;
			this.steps = steps;
			this.path = path;
			this.scenePath = scenePath;
		}
	}

	private final WorldView view;
	private final int plane;
	private final int[][] flags;
	private final Route route;
	private final int px;
	private final int py;

	private Ground(WorldView view, int plane, int[][] flags, Route route, int px, int py)
	{
		this.view = view;
		this.plane = plane;
		this.flags = flags;
		this.route = route;
		this.px = px;
		this.py = py;
	}

	/**
	 * @param clearable objects that block the way now but can be cleared, such as rockfalls, or null
	 * @param clearCost extra steps to charge for crossing one of them
	 * @return null when the scene is not loaded
	 */
	static Ground read(Client client, Collection<? extends TileObject> clearable, int clearCost)
	{
		Player me = client.getLocalPlayer();
		WorldView view = client.getTopLevelWorldView();
		if (me == null || view == null)
		{
			return null;
		}
		CollisionData[] maps = view.getCollisionMaps();
		int plane = view.getPlane();
		if (maps == null || maps[plane] == null)
		{
			return null;
		}
		LocalPoint lp = me.getLocalLocation();
		int[][] flags = maps[plane].getFlags();
		int[][] soft = null;
		if (clearable != null && !clearable.isEmpty())
		{
			soft = new int[flags.length][flags.length == 0 ? 0 : flags[0].length];
			for (TileObject o : clearable)
			{
				if (o.getPlane() != plane)
				{
					continue;
				}
				int[] f = footprint(o);
				for (int x = f[0]; x < f[0] + f[2]; x++)
				{
					for (int y = f[1]; y < f[1] + f[3]; y++)
					{
						if (x >= 0 && y >= 0 && x < soft.length && y < soft[x].length)
						{
							soft[x][y] = clearCost;
						}
					}
				}
			}
		}
		return new Ground(view, plane, flags, Route.flood(flags, lp.getSceneX(), lp.getSceneY(), soft), lp.getSceneX(), lp.getSceneY());
	}

	/** The first of some objects that a walked path crosses, or null. */
	static TileObject crossed(List<int[]> scenePath, Collection<? extends TileObject> objects)
	{
		for (int[] t : scenePath)
		{
			for (TileObject o : objects)
			{
				int[] f = footprint(o);
				if (t[0] >= f[0] && t[0] < f[0] + f[2] && t[1] >= f[1] && t[1] < f[1] + f[3])
				{
					return o;
				}
			}
		}
		return null;
	}

	int plane()
	{
		return plane;
	}

	int sceneX()
	{
		return px;
	}

	int sceneY()
	{
		return py;
	}

	/** The tile footprint of an object in scene coordinates: {x, y, width, height}. */
	static int[] footprint(TileObject o)
	{
		if (o instanceof GameObject)
		{
			GameObject go = (GameObject) o;
			Point min = go.getSceneMinLocation();
			Point max = go.getSceneMaxLocation();
			return new int[]{min.getX(), min.getY(), max.getX() - min.getX() + 1, max.getY() - min.getY() + 1};
		}
		LocalPoint lp = o.getLocalLocation();
		return new int[]{lp.getSceneX(), lp.getSceneY(), 1, 1};
	}

	/** The way to an object on this floor, or null when it is out of reach. */
	Way<TileObject> to(TileObject o)
	{
		if (o == null || o.getPlane() != plane)
		{
			return null;
		}
		int[] f = footprint(o);
		if (Route.beside(px, py, f[0], f[1], f[2], f[3]))
		{
			return new Way<>(o, 0, Collections.emptyList(), Collections.emptyList());
		}
		int[] stand = route.approach(flags, f[0], f[1], f[2], f[3]);
		if (stand == null)
		{
			return null;
		}
		List<int[]> tiles = route.pathTo(stand[0], stand[1]);
		return new Way<>(o, stand[2], toWorld(tiles), tiles);
	}

	/** The way to a character: to a tile next to them. */
	Way<NPC> to(NPC npc)
	{
		if (npc == null)
		{
			return null;
		}
		LocalPoint lp = npc.getLocalLocation();
		WorldPoint wp = npc.getWorldLocation();
		if (lp == null || wp == null || wp.getPlane() != plane)
		{
			return null;
		}
		int size = npc.getComposition() == null ? 1 : Math.max(1, npc.getComposition().getSize());
		int x = lp.getSceneX() - (size - 1) / 2;
		int y = lp.getSceneY() - (size - 1) / 2;
		if (Route.beside(px, py, x, y, size, size) || (Math.abs(px - lp.getSceneX()) <= 1 && Math.abs(py - lp.getSceneY()) <= 1))
		{
			return new Way<>(npc, 0, Collections.emptyList(), Collections.emptyList());
		}
		int[] stand = route.approach(flags, x, y, size, size);
		if (stand == null)
		{
			return null;
		}
		List<int[]> tiles = route.pathTo(stand[0], stand[1]);
		return new Way<>(npc, stand[2], toWorld(tiles), tiles);
	}

	/**
	 * The nearest reachable object on foot, with an optional extra charge per object, such as
	 * for a vein another player is already mining.
	 */
	Way<TileObject> nearest(Collection<? extends TileObject> objects, ToIntFunction<TileObject> penalty)
	{
		Way<TileObject> best = null;
		int bestCost = Integer.MAX_VALUE;
		for (TileObject o : objects)
		{
			Way<TileObject> way = to(o);
			if (way == null)
			{
				continue;
			}
			int cost = way.steps + (penalty == null ? 0 : penalty.applyAsInt(o));
			if (cost < bestCost)
			{
				best = way;
				bestCost = cost;
			}
		}
		return best;
	}

	Way<NPC> nearestNpc(Collection<NPC> npcs)
	{
		Way<NPC> best = null;
		for (NPC n : npcs)
		{
			Way<NPC> way = to(n);
			if (way != null && (best == null || way.steps < best.steps))
			{
				best = way;
			}
		}
		return best;
	}

	/** The walk to a tile in the loaded scene, or an empty list when it is outside it or cannot be reached. */
	List<WorldPoint> toTile(WorldPoint target)
	{
		if (target == null || target.getPlane() != plane)
		{
			return Collections.emptyList();
		}
		LocalPoint lp = LocalPoint.fromWorld(view, target);
		if (lp == null)
		{
			return Collections.emptyList();
		}
		return toWorld(route.pathTo(lp.getSceneX(), lp.getSceneY()));
	}

	/** Whether the tile in scene coordinates can be walked to at all. */
	boolean reaches(int sceneX, int sceneY)
	{
		return route.reaches(sceneX, sceneY);
	}

	private List<WorldPoint> toWorld(List<int[]> tiles)
	{
		List<WorldPoint> out = new ArrayList<>(tiles.size());
		for (int[] t : tiles)
		{
			out.add(WorldPoint.fromScene(view, t[0], t[1], plane));
		}
		return out;
	}
}
