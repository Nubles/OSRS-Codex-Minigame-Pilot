package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Maze.Leg;
import com.codex.minigamepilot.core.Maze.Obstacle;
import com.codex.minigamepilot.core.Maze.Part;
import com.codex.minigamepilot.core.Maze.Piece;
import java.util.EnumSet;
import java.util.List;
import org.junit.Test;

/** The Rogues' Den's facts against the OSRS wiki (Rogues' Den; Rogue equipment; Tiles (Rogues' Den)). */
public class MazeTest
{
	@Test
	public void levelsAndDrains()
	{
		// "Entering the maze requires level 50[not boostable] in both Thieving and Agility"
		assertEquals(50, Maze.LEVEL);
		// "Players with level 80+ Thieving can pick the locks of these doors."
		assertEquals(80, Maze.SHORTCUT_THIEVING);
		// "Contortion Bars ... drain Agility by 5"; "Pendulums ... by 3"; "Ledges ... by 4 (7 for longest and final ledge)";
		// "Floor blades, drains Agility by 3"; traps: "drains your Thieving by 1"
		assertEquals(5, Obstacle.BARS.drain());
		assertEquals(3, Obstacle.PENDULUM.drain());
		assertEquals(4, Obstacle.LEDGE.drain());
		assertEquals(7, Maze.DRAIN_LEDGE_LONG);
		assertEquals(3, Obstacle.BLADE.drain());
		assertEquals(1, Obstacle.WALL_TRAP.drain());
		assertEquals(1, Obstacle.FLOOR_TRAP.drain());
		// "at least six (6) points remaining for the last two obstacles"
		assertEquals(6, Maze.AGILITY_FOR_LAST_TWO);
		assertEquals(12, Maze.XP_PER_LEVEL_DRAINED);
		assertEquals(22_000, Maze.WIKI_AGILITY_XP_PER_HOUR);
	}

	@Test
	public void hazardsAreNeverTargets()
	{
		// Only the two legs that say "Search wall" are about a trap: the first wall, searched to disable it.
		for (Leg l : Leg.values())
		{
			assertTrue(l.name(), l.obstacle() == null || !l.obstacle().hazard() || l.end() == Maze.End.TRAP);
		}
		assertEquals(Obstacle.WALL_TRAP, Leg.FOUR_WALLS.obstacle());
		assertTrue(Obstacle.SPINNING_BLADES.hazard());
		assertFalse(Obstacle.PENDULUM.hazard());
	}

	@Test
	public void theWikisRoute()
	{
		List<Leg> longRoute = Maze.route(false);
		List<Leg> shortcut = Maze.route(true);
		assertEquals(Leg.BARS, longRoute.get(0));
		assertEquals(Leg.SAFE, longRoute.get(longRoute.size() - 1));
		assertEquals(Leg.SAFE, shortcut.get(shortcut.size() - 1));
		assertFalse(longRoute.contains(Leg.SHORTCUT));
		assertTrue(shortcut.contains(Leg.SHORTCUT));
		for (Leg l : shortcut)
		{
			assertTrue(l.part() != Part.LONG);
		}
		// Long route: "Go north through the passageway" comes straight after the trap room; the tile before the mosaic door.
		assertEquals(longRoute.indexOf(Leg.TRAP_ROOM) + 1, longRoute.indexOf(Leg.LONG_PASSAGE));
		assertEquals(longRoute.indexOf(Leg.TILE) + 1, longRoute.indexOf(Leg.MOSAIC));
		assertEquals(shortcut.indexOf(Leg.SHORTCUT) + 1, shortcut.indexOf(Leg.FINAL_TRAPS));
		// "Move past the last two pendulums"
		assertEquals(2, Leg.LAST_PENDULUMS.times());
		assertEquals(1, Leg.PENDULUMS.times());
	}

	@Test
	public void crateTable()
	{
		// The wiki's table: 50 39.2% 13 48 minutes; 60 40.8% 12 45; 70 42.3% 12 42; 80 43.9% 11 23; 90 45.5% 11 22; 99 46.9% 11 22.
		assertEquals(0.392, Maze.crateChance(50), 1e-9);
		assertEquals(0.392, Maze.crateChance(59), 1e-9);
		assertEquals(0.408, Maze.crateChance(60), 1e-9);
		assertEquals(0.439, Maze.crateChance(85), 1e-9);
		assertEquals(0.469, Maze.crateChance(99), 1e-9);
		assertEquals(80, Maze.tableLevel(85));
		assertEquals(48, Maze.wikiMinutesForSet(50));
		assertEquals(23, Maze.wikiMinutesForSet(80));
		// Our rounding agrees with every row of the wiki's "Expected attempts ... for full set".
		for (int level : new int[]{50, 60, 70, 80, 90, 99})
		{
			assertEquals(String.valueOf(level), Maze.wikiAttemptsForSet(level), Maze.attemptsFor(5, level));
		}
		assertEquals(0, Maze.attemptsFor(0, 70));
		assertEquals(3, Maze.attemptsFor(1, 50));
	}

	@Test
	public void setEffect()
	{
		// 1 = 15%, 2 = 30%, 3 = 45%, 4 = 60%, 5 = 100%.
		assertEquals(0, Maze.doubleLootPercent(0));
		assertEquals(15, Maze.doubleLootPercent(1));
		assertEquals(60, Maze.doubleLootPercent(4));
		assertEquals(100, Maze.doubleLootPercent(5));
	}

	@Test
	public void piecesSurviveASave()
	{
		EnumSet<Piece> p = EnumSet.of(Piece.MASK, Piece.BOOTS);
		assertEquals(p, Maze.decodePieces(Maze.encodePieces(p)));
		assertEquals(EnumSet.of(Piece.TOP), Maze.decodePieces("TOP,CAPE, ,"));
		assertTrue(Maze.decodePieces(null).isEmpty());
	}
}
