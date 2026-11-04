package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Maze;
import com.codex.minigamepilot.core.Maze.End;
import com.codex.minigamepilot.core.Maze.Obstacle;
import com.codex.minigamepilot.core.Maze.Piece;
import java.util.EnumSet;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

/** The Rogues' Den ids the Pilot reads, by their RuneLite names, against the numbers the OSRS wiki gives in its infoboxes. */
public class RoguesDenIdsTest
{
	@Test
	public void wikiItemIds()
	{
		// Rogue mask 5554, Rogue top 5553, Rogue trousers 5555, Rogue gloves 5556, Rogue boots 5557.
		assertEquals(5554, Piece.MASK.item());
		assertEquals(5553, Piece.TOP.item());
		assertEquals(5555, Piece.TROUSERS.item());
		assertEquals(5556, Piece.GLOVES.item());
		assertEquals(5557, Piece.BOOTS.item());
		// Rogue kit 5558, Flash powder 5559, Mystic jewel 5561, Rogue's equipment crate 21307.
		assertEquals(5558, Maze.KIT);
		assertEquals(5559, Maze.FLASH_POWDER);
		assertEquals(5561, Maze.JEWEL);
		assertEquals(21307, Maze.CRATE);
		// Tiles (Rogues' Den): "5568: Tile (correct)", 5569 to 5571 the three Tiles.
		assertEquals(5568, Maze.TILE);
		assertEquals(5569, Maze.WRONG_TILES[0]);
		assertEquals(5571, Maze.WRONG_TILES[2]);
		// Brian O'Richard: NPC ID 3189.
		assertEquals(3189, Maze.BRIAN);
		assertEquals(NpcID.ROGUESDEN_BANKER, Maze.BANKER);
	}

	@Test
	public void obstaclesByTheirGameNames()
	{
		// RuneLite's gameval: "Contortion Bars" 7251, "Pendulum" 7241, "Ledge" 7239 and 7240, "Blade" 7252, "Grill" 7254 and 7255,
		// "Doorway" 7256, "Wall safe" 7237 (the maze's), "Spinning blades" 7224, "Door" 7234 (the mosaic puzzle door).
		assertEquals(Obstacle.BARS, Obstacle.of(7251));
		assertEquals(Obstacle.PENDULUM, Obstacle.of(7241));
		assertEquals(Obstacle.LEDGE, Obstacle.of(7239));
		assertEquals(Obstacle.LEDGE, Obstacle.of(7240));
		assertEquals(Obstacle.BLADE, Obstacle.of(7252));
		assertEquals(Obstacle.GRILL, Obstacle.of(7254));
		assertEquals(Obstacle.SPINNING_BLADES, Obstacle.of(7224));
		assertEquals(Obstacle.MOSAIC_DOOR, Obstacle.of(7234));
		assertEquals(Obstacle.WALL_SAFE, Obstacle.of(7237));
		assertEquals(7256, Maze.DOORWAY);
		assertEquals(Obstacle.FLOOR_TRAP, Obstacle.of(ObjectID.ROGUESDEN_TRAP_FLOOR_PITFALL));
		assertEquals(Obstacle.WALL_TRAP, Obstacle.of(ObjectID.ROGUESDEN_TRAP_WALL_CRUSHER));
	}

	@Test
	public void watched()
	{
		assertTrue(RoguesDenPilot.isWatched(Maze.DOORWAY));
		assertTrue(RoguesDenPilot.isWatched(ObjectID.ROGUESDEN_TRAP_FLOOR_SPIKES));
		assertFalse(RoguesDenPilot.isWatched(ObjectID.ROGUESDEN_WALL));
		// The lobby's own wall safes are not the maze's.
		assertFalse(RoguesDenPilot.isWatched(ObjectID.ROGUESDEN_WALLDECOR_SAFE));
	}

	@Test
	public void drains()
	{
		assertEquals(EnumSet.of(End.BARS), RoguesDenPilot.agilityEnds(5, null));
		assertEquals(EnumSet.of(End.LEDGE), RoguesDenPilot.agilityEnds(7, null));
		assertEquals(EnumSet.of(End.PENDULUM, End.BLADE), RoguesDenPilot.agilityEnds(3, null));
		// Beside a blade, a drain of 3 is the blade.
		assertEquals(EnumSet.of(End.BLADE), RoguesDenPilot.agilityEnds(3, Obstacle.BLADE));
		assertTrue(RoguesDenPilot.agilityEnds(2, null).isEmpty());
	}
}
