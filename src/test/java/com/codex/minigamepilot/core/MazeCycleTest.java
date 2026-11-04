package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Maze.End;
import com.codex.minigamepilot.core.Maze.Leg;
import com.codex.minigamepilot.core.Maze.Obstacle;
import com.codex.minigamepilot.core.Maze.Piece;
import java.util.Arrays;
import java.util.EnumSet;
import org.junit.Test;

public class MazeCycleTest
{
	private static MazeCycle.Snapshot lobby()
	{
		MazeCycle.Snapshot s = new MazeCycle.Snapshot();
		s.agility = 60;
		s.thieving = 60;
		s.agilityNow = 60;
		s.thievingNow = 60;
		s.runsBefore = 3;
		s.missing.addAll(Arrays.asList(Piece.values()));
		return s;
	}

	@Test
	public void trackerFollowsTheStart()
	{
		MazeCycle.Tracker t = new MazeCycle.Tracker(false);
		assertEquals(Leg.BARS, t.leg());
		assertTrue(t.event(End.BARS));
		assertEquals(Leg.FIRST_PENDULUM, t.leg());
		assertTrue(t.event(End.PENDULUM));
		assertEquals(Leg.STAIRS, t.leg());
		// The stairs and the trap line report nothing: the ledge's drain passes them.
		assertTrue(t.event(End.LEDGE));
		assertEquals(Leg.BLADES, t.leg());
		assertEquals(Leg.TWO_LEDGES, t.passed());
	}

	@Test
	public void anotherPendulumInTheSameRoomChangesNothing()
	{
		MazeCycle.Tracker t = new MazeCycle.Tracker(true);
		t.event(End.BARS);
		t.event(End.PENDULUM);
		t.event(End.LEDGE);
		t.event(End.BLADE);
		assertEquals(Leg.PENDULUMS, t.leg());
		t.event(End.PENDULUM);
		assertEquals(Leg.LEDGE, t.leg());
		// The room's second pendulum.
		assertFalse(t.event(End.PENDULUM));
		assertEquals(Leg.LEDGE, t.leg());
	}

	@Test
	public void ambiguousDrainEndsWhicheverComesFirst()
	{
		MazeCycle.Tracker t = new MazeCycle.Tracker(false);
		t.event(End.BARS);
		t.event(End.PENDULUM);
		t.event(End.LEDGE);
		assertEquals(Leg.BLADES, t.leg());
		t.event(EnumSet.of(End.PENDULUM, End.BLADE));
		assertEquals(Leg.PENDULUMS, t.leg());
	}

	@Test
	public void roomsOwnObstaclesResyncButNeverMoveBack()
	{
		MazeCycle.Tracker t = new MazeCycle.Tracker(false);
		assertTrue(t.near(Obstacle.GRILL));
		assertEquals(Leg.GRILLS, t.leg());
		assertFalse(t.near(Obstacle.MOSAIC_DOOR));
		assertEquals(Leg.GRILLS, t.leg());
		assertTrue(t.guardNear());
		assertEquals(Leg.POWDER, t.leg());
		// The shortcut has no grills.
		MazeCycle.Tracker cut = new MazeCycle.Tracker(true);
		assertFalse(cut.near(Obstacle.GRILL));
		assertEquals(Leg.BARS, cut.leg());
	}

	@Test
	public void spinningBladesMeanTheFinishOnlyWhenItIsNear()
	{
		MazeCycle.Tracker t = new MazeCycle.Tracker(false);
		t.near(Obstacle.GRILL);
		// The long route's spinning blades, by the ledge: far from the finish.
		assertFalse(t.near(Obstacle.SPINNING_BLADES));
		t.event(End.TRAP);
		assertEquals(Leg.LONG_PENDULUM, t.leg());
		assertTrue(t.near(Obstacle.SPINNING_BLADES));
		assertEquals(Leg.FINAL_TRAPS, t.leg());
	}

	@Test
	public void lastTwoPendulumsThenTheSafe()
	{
		MazeCycle.Tracker t = new MazeCycle.Tracker(true);
		t.guardNear();
		t.event(End.POWDER_TAKEN);
		t.event(End.POWDER_USED);
		assertEquals(Leg.LAST_PENDULUMS, t.leg());
		t.event(End.PENDULUM);
		assertEquals(Leg.LAST_PENDULUMS, t.leg());
		t.event(End.PENDULUM);
		assertEquals(Leg.SAFE, t.leg());
		// The safe is the end: nothing moves past it.
		t.event(End.PENDULUM);
		assertEquals(Leg.SAFE, t.leg());
	}

	@Test
	public void lobbyOrder()
	{
		MazeCycle.Snapshot s = lobby();
		s.carried = 3;
		s.worn = 2;
		s.crates = 1;
		assertEquals("Search the crate: choose the rogue mask", MazeCycle.next(s).step.getTitle());
		s.crates = 0;
		MazeCycle.Decision d = MazeCycle.next(s);
		assertEquals("Bank everything you carry and wear", d.step.getTitle());
		assertEquals(Step.Target.BANKER, d.step.getTarget());
		s.carried = 0;
		assertEquals("Bank everything you wear", MazeCycle.next(s).step.getTitle());
		s.worn = 0;
		s.follower = true;
		assertEquals("Leave your follower outside", MazeCycle.next(s).step.getTitle());
		s.follower = false;
		d = MazeCycle.next(s);
		assertEquals("Enter the maze through the doorway", d.step.getTitle());
		assertEquals(Step.Target.DOORWAY, d.step.getTarget());
		assertEquals("The long route: the shortcut's doors need 80 Thieving", d.step.getDetail());
		s.runsBefore = 0;
		assertEquals("The first time, talk to Brian O'Richard before you enter", MazeCycle.next(s).step.getDetail());
	}

	@Test
	public void levelsAreRealLevels()
	{
		MazeCycle.Snapshot s = lobby();
		s.agility = 49;
		s.agilityNow = 55;
		MazeCycle.Decision d = MazeCycle.next(s);
		assertTrue(d.step.isWaiting());
		assertEquals("Yours are 49 and 60; boosts do not count", d.step.getDetail());
	}

	@Test
	public void crateNamesWhatYouLack()
	{
		MazeCycle.Snapshot s = lobby();
		s.crates = 1;
		s.missing.clear();
		s.missing.addAll(Arrays.asList(Piece.GLOVES, Piece.BOOTS));
		assertEquals("You choose the piece; you lack 2: gloves and boots", MazeCycle.next(s).step.getDetail());
		s.missing.clear();
		assertEquals("Your set is whole: a rogue kit, or a piece to spare", MazeCycle.next(s).step.getDetail());
	}

	@Test
	public void theRightTileAndThePowder()
	{
		MazeCycle.Snapshot s = lobby();
		s.inMaze = true;
		s.leg = Leg.MOSAIC;
		assertEquals("Take the south-eastern tile", MazeCycle.next(s).step.getTitle());
		s.holdsTile = true;
		MazeCycle.Decision d = MazeCycle.next(s);
		assertEquals("Open the door and click the tile in the puzzle", d.step.getTitle());
		assertTrue(d.step.hasItem(Maze.TILE));
		s.wrongTile = Maze.WRONG_TILES[1];
		assertEquals("Drop the Tiles: take the one called Tile", MazeCycle.next(s).step.getTitle());
		s.wrongTile = 0;
		s.leg = Leg.GUARD;
		s.holdsPowder = false;
		assertEquals("Pick up the flash powder", MazeCycle.next(s).step.getTitle());
		s.holdsPowder = true;
		d = MazeCycle.next(s);
		assertEquals(Step.Target.GUARD, d.step.getTarget());
		assertTrue(d.step.hasItem(Maze.FLASH_POWDER));
		assertEquals(3, d.stage);
	}

	@Test
	public void warnsLowAgilityAndDrainedThieving()
	{
		MazeCycle.Snapshot s = lobby();
		s.inMaze = true;
		s.leg = Leg.LAST_PENDULUMS;
		s.agilityNow = 5;
		assertEquals("Only 5 Agility left: below 6, a drain under zero sends you back", MazeCycle.next(s).step.getDetail());
		s.leg = Leg.SAFE;
		s.thievingNow = 57;
		MazeCycle.Decision d = MazeCycle.next(s);
		assertEquals(Step.Target.WALL_SAFE, d.step.getTarget());
		assertEquals("The crack rolls on your Thieving now, 57; it can fail", d.step.getDetail());
		assertEquals(4, d.stage);
	}
}
