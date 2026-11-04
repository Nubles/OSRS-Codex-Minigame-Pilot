package com.codex.minigamepilot.core;

import com.codex.minigamepilot.core.Maze.End;
import com.codex.minigamepilot.core.Maze.Leg;
import com.codex.minigamepilot.core.Maze.Obstacle;
import com.codex.minigamepilot.core.Maze.Piece;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Works out the one next thing to do at the Rogues' Den: in the lobby, the levels, the crate to
 * search, the bank and the doorway; in the maze, the leg of the wiki's way through that you are
 * on, its obstacle and the right choice where the maze has one (either ledge, the blade to click,
 * the tile called Tile, the flash powder for the guard, the shortcut at 80 Thieving).
 *
 * <p>Where you are in the maze is followed by {@link Tracker} from what the game reports: each
 * drain of Agility or Thieving at an obstacle, the tile and the flash powder in your pack, and the
 * obstacles that only stand in one room (the mosaic door, the grills, the guard, the wall safe)
 * coming near. It only reads; every click is yours.
 */
public final class MazeCycle
{
	/** The stages of a run, for the loop strip. */
	public static final String[] LOOP = {"Lobby", "Start", "Route", "Finish", "Safe"};

	private MazeCycle()
	{
	}

	/**
	 * Follows a run through the maze, leg by leg. An event moves it on to the leg it ends, up to a
	 * few legs ahead (a leg the game reports nothing for is passed by the next one's event); one
	 * that matches the leg just passed again (another pendulum in the same room) changes nothing.
	 */
	public static final class Tracker
	{
		/** How many legs ahead an event may end. */
		static final int LOOKAHEAD = 4;

		private final List<Leg> route;
		private final boolean shortcut;
		private int at;
		private int count;
		private Leg passed;

		public Tracker(boolean shortcut)
		{
			this.shortcut = shortcut;
			this.route = Maze.route(shortcut);
		}

		public boolean shortcut()
		{
			return shortcut;
		}

		public List<Leg> route()
		{
			return route;
		}

		/** The leg you are on. */
		public Leg leg()
		{
			return route.get(at);
		}

		public int index()
		{
			return at;
		}

		/** The leg last passed, or null at the start. */
		public Leg passed()
		{
			return passed;
		}

		/**
		 * Something the game reported that may end a leg.
		 *
		 * @param could every end that might explain it (a drain of 3 Agility with no obstacle by
		 *     you is a pendulum or a blade)
		 * @return true when it moved the run on
		 */
		public boolean event(Set<End> could)
		{
			for (int i = at; i < route.size() && i <= at + LOOKAHEAD; i++)
			{
				Leg l = route.get(i);
				if (l.end() != End.NONE && could.contains(l.end()))
				{
					count = i == at ? count + 1 : 1;
					at = i;
					if (count >= l.times())
					{
						passed = l;
						count = 0;
						at = Math.min(route.size() - 1, i + 1);
					}
					return true;
				}
			}
			return false;
		}

		public boolean event(End end)
		{
			return event(EnumSet.of(end));
		}

		/**
		 * An obstacle that stands in one room only has come near: if its leg is still ahead, you are
		 * there. Never moves back.
		 */
		public boolean near(Obstacle o)
		{
			Leg to = null;
			switch (o)
			{
				case MOSAIC_DOOR:
					to = Leg.TILE;
					break;
				case GRILL:
					to = Leg.GRILLS;
					break;
				case WALL_SAFE:
					to = Leg.SAFE;
					break;
				case SPINNING_BLADES:
				{
					// Spinning blades stand in a room of the long route too: they mean the finish only
					// when it is at most two legs on (from the long route's pendulum, or the shortcut).
					int i = route.indexOf(Leg.FINAL_TRAPS);
					return i > at && i - at <= 2 && jump(Leg.FINAL_TRAPS);
				}
				default:
					break;
			}
			return to != null && jump(to);
		}

		/** A rogue guard has come near: the flash powder's room. */
		public boolean guardNear()
		{
			return jump(Leg.POWDER);
		}

		private boolean jump(Leg to)
		{
			int i = route.indexOf(to);
			if (i > at)
			{
				at = i;
				count = 0;
				passed = route.get(i - 1);
				return true;
			}
			return false;
		}
	}

	/** Everything a decision needs, read from the game. */
	public static final class Snapshot
	{
		/** In the maze: the mystic jewel is carried. */
		public boolean inMaze;
		/** Real levels: "not boostable". */
		public int agility;
		public int thieving;
		/** Visible levels now, as the maze drains them. */
		public int agilityNow;
		public int thievingNow;
		/** Items carried other than the mystic jewel, and items worn. */
		public int carried;
		public int worn;
		public boolean follower;
		/** Rogue's equipment crates carried. */
		public int crates;
		/** The pieces you do not own yet, in the wiki's order. */
		public List<Piece> missing = new ArrayList<>();
		/** The leg you are on in the maze, or null in the lobby. */
		public Leg leg;
		public boolean holdsTile;
		/** One of the three items called Tiles, which do not open the door; its id, or 0. */
		public int wrongTile;
		public boolean holdsPowder;
		/** This run takes the shortcut: 80 Thieving when it began. */
		public boolean shortcut;
		/** Runs kept: none means this may be your first. */
		public int runsBefore;
	}

	/** A step and where it is in the run. */
	public static final class Decision
	{
		public final Step step;
		public final int stage;
		/** The leg the step is about, which may be the tracker's or the one before it (the tile before the door); null in the lobby. */
		public final Leg leg;

		Decision(Step step, int stage)
		{
			this(step, stage, null);
		}

		Decision(Step step, int stage, Leg leg)
		{
			this.step = step;
			this.stage = stage;
			this.leg = leg;
		}
	}

	public static Decision next(Snapshot s)
	{
		if (!s.inMaze || s.leg == null)
		{
			return lobby(s);
		}
		return maze(s);
	}

	private static Decision lobby(Snapshot s)
	{
		if (s.crates > 0)
		{
			if (s.missing.isEmpty())
			{
				return new Decision(Step.items("Search", "Search the crate", "Your set is whole: a rogue kit, or a piece to spare", Maze.CRATE), 0);
			}
			Piece first = s.missing.get(0);
			return new Decision(Step.items("Search", "Search the crate: choose the " + first.getTitle().toLowerCase(Locale.ROOT),
				"You choose the piece; you lack " + s.missing.size() + (s.missing.size() == 1 ? " now" : ": " + names(s.missing)), Maze.CRATE), 0);
		}
		if (s.agility < Maze.LEVEL || s.thieving < Maze.LEVEL)
		{
			return new Decision(Step.waiting("The maze needs 50 Agility and 50 Thieving",
				"Yours are " + s.agility + " and " + s.thieving + "; boosts do not count", Step.Target.NONE), 0);
		}
		if (s.carried > 0 || s.worn > 0)
		{
			String what = s.carried > 0 && s.worn > 0 ? "everything you carry and wear" : s.carried > 0 ? "everything you carry" : "everything you wear";
			return new Decision(Step.of("Bank", "Bank " + what,
				"No items can be brought or worn inside. Emerald Benedict runs the bank", Step.Target.BANKER), 0);
		}
		if (s.follower)
		{
			return new Decision(Step.of("Dismiss", "Leave your follower outside", "The maze is entered without any follower", Step.Target.NONE), 0);
		}
		String route = s.thieving >= Maze.SHORTCUT_THIEVING ? "Your 80 Thieving opens the shortcut" : "The long route: the shortcut's doors need 80 Thieving";
		return new Decision(Step.of("Enter", "Enter the maze through the doorway",
			s.runsBefore == 0 ? "The first time, talk to Brian O'Richard before you enter" : route, Step.Target.DOORWAY), 0);
	}

	private static Decision maze(Snapshot s)
	{
		Leg leg = s.leg;
		int stage = leg == Leg.SAFE ? 4 : leg.part() == Maze.Part.FINISH ? 3 : leg.part() == Maze.Part.START ? 1 : 2;
		if (s.wrongTile > 0)
		{
			return new Decision(Step.items("Drop", "Drop the Tiles: take the one called Tile",
				"The south-eastern one; the bottom right is the correct tile", s.wrongTile), stage, Leg.TILE);
		}
		if (leg == Leg.MOSAIC && !s.holdsTile)
		{
			leg = Leg.TILE;
		}
		if (leg == Leg.TILE && s.holdsTile)
		{
			leg = Leg.MOSAIC;
		}
		if (leg == Leg.GUARD && !s.holdsPowder)
		{
			leg = Leg.POWDER;
		}
		if (leg == Leg.POWDER && s.holdsPowder)
		{
			leg = Leg.GUARD;
		}
		String detail = leg.detail();
		if (leg == Leg.TRAP_ROOM && s.shortcut)
		{
			detail = "Then unlock the doors: right, left, left, with your 80 Thieving";
		}
		else if (leg == Leg.LAST_PENDULUMS && s.agilityNow < Maze.AGILITY_FOR_LAST_TWO)
		{
			detail = "Only " + s.agilityNow + " Agility left: below 6, a drain under zero sends you back";
		}
		else if (leg == Leg.SAFE && s.thievingNow < s.thieving)
		{
			detail = "The crack rolls on your Thieving now, " + s.thievingNow + "; it can fail";
		}
		switch (leg)
		{
			case GUARD:
				return new Decision(Step.items(leg.verb(), leg.title(), detail, Maze.FLASH_POWDER).withTarget(Step.Target.GUARD), stage, leg);
			case MOSAIC:
				return new Decision(Step.items(leg.verb(), leg.title(), detail, Maze.TILE).withTarget(Step.Target.OBSTACLE), stage, leg);
			case SAFE:
				return new Decision(Step.of(leg.verb(), leg.title(), detail, Step.Target.WALL_SAFE), stage, leg);
			default:
				return new Decision(Step.of(leg.verb(), leg.title(), detail, leg.obstacle() == null ? Step.Target.NONE : Step.Target.OBSTACLE), stage, leg);
		}
	}

	/** "mask, gloves and boots". */
	static String names(List<Piece> pieces)
	{
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < pieces.size(); i++)
		{
			if (i > 0)
			{
				b.append(i == pieces.size() - 1 ? " and " : ", ");
			}
			b.append(pieces.get(i).getTitle().substring("Rogue ".length()));
		}
		return b.toString();
	}
}
