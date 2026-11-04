package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

/**
 * The facts of the Rogues' Den maze, in Burthorpe: its levels, the rule on items, the obstacles
 * and what each drains, the wiki's way through it leg by leg, the mosaic door's tile, the wall
 * safe's crate odds and the five pieces of rogue equipment.
 *
 * <p>Everything here is from the OSRS wiki (Rogues' Den, its maze walkthrough and crate table;
 * Rogue equipment; Rogue's equipment crate; Tiles (Rogues' Den); Mystic jewel), quoted beside each
 * figure. Ids are RuneLite's gameval names, whose names in the game ("Contortion Bars",
 * "Pendulum", "Ledge", "Blade", "Grill", "Wall safe") match the wiki's legend. The wiki gives the
 * way through as rooms and obstacles, not tiles, so no tile of the maze is written here: the
 * Pilot lights the obstacles of the leg you are on as they come into view.
 */
public final class Maze
{
	/** "Entering the maze requires level 50[not boostable] in both Thieving and Agility". */
	public static final int LEVEL = 50;
	/** "Players with level 80+ Thieving can pick the locks of these doors." */
	public static final int SHORTCUT_THIEVING = 80;
	/** "make sure you have at least six (6) points remaining for the last two obstacles". */
	public static final int AGILITY_FOR_LAST_TWO = 6;
	/** "Players will also receive 12 experience per stat level drained in both Agility and Thieving." */
	public static final int XP_PER_LEVEL_DRAINED = 12;
	/** "At level 99 Agility and level 80 Thieving, up to 22k Agility XP per hour can be gained". */
	public static final int WIKI_AGILITY_XP_PER_HOUR = 22_000;

	/** "Contortion Bars, agility obstacles which drain Agility by 5". */
	public static final int DRAIN_BARS = 5;
	/** "Pendulums, agility obstacles which drain Agility by 3". */
	public static final int DRAIN_PENDULUM = 3;
	/** "Ledges ... which drain Agility by 4 (7 for longest and final ledge)". */
	public static final int DRAIN_LEDGE = 4;
	public static final int DRAIN_LEDGE_LONG = 7;
	/** "Floor blades, drains Agility by 3". */
	public static final int DRAIN_BLADE = 3;
	/** Floor and wall traps: "Disabling these traps drains your Thieving by 1." */
	public static final int DRAIN_TRAP = 1;

	/** The Mystic jewel: "automatically given when entering the Rogues' Den maze"; it "teleports players from the maze back to the waiting room". */
	public static final int JEWEL = ItemID.ROGUESDEN_GEM;
	/** "Only the item named Tile rather than Tiles is the correct item for the maze." Tile is 5568; the three Tiles are 5569 to 5571. */
	public static final int TILE = ItemID.ROGUESDEN_PUZZLE_MOSAIC_TILE1;
	public static final int[] WRONG_TILES = {ItemID.ROGUESDEN_PUZZLE_MOSAIC_TILE2, ItemID.ROGUESDEN_PUZZLE_MOSAIC_TILE3,
		ItemID.ROGUESDEN_PUZZLE_MOSAIC_TILE4};
	/** "Flash powder, which can be used to temporarily blind rogue guards." */
	public static final int FLASH_POWDER = ItemID.ROGUESDEN_FLASH_POWDER;
	/** "Rogue's equipment crate" (21307): "the choice to either receive one piece of rogue equipment or a rogue kit". */
	public static final int CRATE = ItemID.ROGUESDEN_CRATE;
	/** "Rogue kit". */
	public static final int KIT = ItemID.ROGUESDEN_KIT;

	/** Brian O'Richard, who runs the maze; "Emerald Benedict runs the bank."; the Rogue Guards. */
	public static final int BRIAN = NpcID.ROGUESDEN_MAZEMASTER;
	public static final int BANKER = NpcID.ROGUESDEN_BANKER;
	public static final int[] GUARDS = {NpcID.ROGUESDEN_GUARD1, NpcID.ROGUESDEN_GUARD2, NpcID.ROGUESDEN_GUARD3};

	/** The maze's "Doorway", the way in from the lobby. */
	public static final int DOORWAY = ObjectID.ROGUESDEN_MAZEENTRANCE;
	/** "Eight wall safes, the objective of the maze": RuneLite calls the maze's one ROGUESDEN_WALLDECOR_MAZESAFE. */
	public static final int SAFE = ObjectID.ROGUESDEN_WALLDECOR_MAZESAFE;

	/** The obstacles the walkthrough names, by their names in the game. */
	public enum Obstacle
	{
		BARS("contortion bars", DRAIN_BARS, ObjectID.ROGUESDEN_OBSTACLE_CONTORTION_BARS),
		PENDULUM("pendulum", DRAIN_PENDULUM, ObjectID.ROGUESDEN_TRAP_FLOOR_PENDULUM),
		LEDGE("ledge", DRAIN_LEDGE, ObjectID.ROGUESDEN_OBSTACLE_FLOOR_BALANCING_LEDGE, ObjectID.ROGUESDEN_OBSTACLE_WALL_HANGING_LEDGE),
		BLADE("blade", DRAIN_BLADE, ObjectID.ROGUESDEN_TRAP_FLOOR_TIMEDBLADE),
		PASSAGEWAY("passageway", 0, ObjectID.ROGUESDEN_PASSAGE_ACROSS, ObjectID.ROGUESDEN_PASSAGE_UP),
		/** "Wall traps. Disabling these traps drains your Thieving by 1." */
		WALL_TRAP("wall trap", DRAIN_TRAP, ObjectID.ROGUESDEN_TRAP_WALL_SCYTHEBLADE, ObjectID.ROGUESDEN_TRAP_WALL_SPIKES,
			ObjectID.ROGUESDEN_TRAP_WALL_POISON_GAS, ObjectID.ROGUESDEN_TRAP_WALL_SAW, ObjectID.ROGUESDEN_TRAP_WALL_CRUSHER),
		/** "Floor traps, including floor spikes and pressure pads." */
		FLOOR_TRAP("floor trap", DRAIN_TRAP, ObjectID.ROGUESDEN_TRAP_FLOOR_SPIKES, ObjectID.ROGUESDEN_TRAP_FLOOR_EXPLOSION,
			ObjectID.ROGUESDEN_TRAP_FLOOR_SPRINGTILE, ObjectID.ROGUESDEN_TRAP_FLOOR_PITFALL),
		/** "Spinning blades on pedestals. Cannot be disabled." */
		SPINNING_BLADES("spinning blades", 0, ObjectID.ROGUESDEN_TRAP_FLOOR_SCYTHEBLADE),
		/** "The mosaic door, which opens when the correct tile is inserted". */
		MOSAIC_DOOR("mosaic door", 0, ObjectID.ROGUESDEN_PUZZLE_DOOR_MOSAIC),
		/** "Grill room. There is only one correct path through the grills (shown on the map)." */
		GRILL("grill", 0, ObjectID.ROGUESDEN_OBSTACLE_BLOCKING_DOOR_NOENTRY, ObjectID.ROGUESDEN_OBSTACLE_BLOCKING_DOOR_ENTER),
		WALL_SAFE("wall safe", 0, SAFE);

		private final String name;
		private final int drain;
		private final int[] ids;

		Obstacle(String name, int drain, int... ids)
		{
			this.name = name;
			this.drain = drain;
			this.ids = ids;
		}

		public String getName()
		{
			return name;
		}

		/** Agility (or, for traps, Thieving) the wiki says it drains. */
		public int drain()
		{
			return drain;
		}

		public int[] ids()
		{
			return ids.clone();
		}

		public boolean has(int id)
		{
			for (int i : ids)
			{
				if (i == id)
				{
					return true;
				}
			}
			return false;
		}

		/** Traps and blades that end a run if touched: never a target, only marked. */
		public boolean hazard()
		{
			return this == WALL_TRAP || this == FLOOR_TRAP || this == SPINNING_BLADES;
		}

		public static Obstacle of(int id)
		{
			for (Obstacle o : values())
			{
				if (o.has(id))
				{
					return o;
				}
			}
			return null;
		}
	}

	/** What tells the Pilot a leg is behind you. */
	public enum End
	{
		/** Nothing the game reports: the next leg's own event, or an obstacle further on coming near, moves on. */
		NONE,
		BARS,
		PENDULUM,
		LEDGE,
		BLADE,
		/** A trap searched: Thieving drained by one. */
		TRAP,
		TILE_TAKEN,
		TILE_USED,
		POWDER_TAKEN,
		POWDER_USED,
		/** The run's end. */
		SAFE
	}

	/** Which part of the maze a leg belongs to. */
	public enum Part
	{
		START("Start"),
		LONG("Long route"),
		SHORTCUT("Shortcut"),
		FINISH("Finish");

		private final String title;

		Part(String title)
		{
			this.title = title;
		}

		public String getTitle()
		{
			return title;
		}
	}

	/**
	 * The wiki's walkthrough, row by row ("Pre-shortcut", "Long route", "Post-shortcut"), each row
	 * split where it asks for two things. The title is the action; the detail is the wiki's own
	 * caution for that room.
	 */
	public enum Leg
	{
		// "Walk over the floor traps at the start and go west through the contortion bars and pendulum."
		BARS(Part.START, "Squeeze", "West through the contortion bars", "Walk over the floor traps at the start", End.BARS, "the contortion bars", Obstacle.BARS),
		FIRST_PENDULUM(Part.START, "Pass", "Past the pendulum", "Then the stairs", End.PENDULUM, "the first pendulum", Obstacle.PENDULUM),
		// "Avoid the traps by staying on the stairs and go through the north-west gate."
		STAIRS(Part.START, "Go", "Stay on the stairs to the north-west gate", "Off the stairs are traps", End.NONE, "the stairs", null),
		// "Follow the line as shown to the side using the "run trick" where possible." "Don't stand still on any traps or next to any wall traps."
		LINE(Part.START, "Run", "Follow the line past the traps, running", "Don't stand still on any traps or next to any wall traps", End.NONE,
			"the trap line", null),
		// "Climb across one of the two ledges and go north."
		TWO_LEDGES(Part.START, "Climb", "Climb across either ledge, then north", "One of the two ledges; either will do", End.LEDGE, "the two ledges",
			Obstacle.LEDGE),
		// "Be sure to left click on one of the blades otherwise your character will try to walk around them."
		BLADES(Part.START, "Cross", "Left-click a blade to cross it", "Or your character walks around them", End.BLADE, "the blades", Obstacle.BLADE),
		PENDULUMS(Part.START, "Pass", "Past the pendulums", "Then the ledge to the next room", End.PENDULUM, "the pendulums", Obstacle.PENDULUM),
		// "Walk over the ledge to the next room."
		LEDGE(Part.START, "Walk", "Walk over the ledge to the next room", "Traps on the floor and along both walls there", End.LEDGE, "the ledge",
			Obstacle.LEDGE),
		// "Click in between the two trap tiles, then click at the bend past all the traps."
		TRAP_ROOM(Part.START, "Go", "Between the two trap tiles, then the bend", "Traps on the floor and along the two walls", End.NONE,
			"the trap room", null),
		// "If you have at least 80 Thieving, unlock the doors and proceed to go right, left, left, and skip to post-shortcut."
		SHORTCUT(Part.SHORTCUT, "Unlock", "Unlock the doors: right, left, left", "Your 80 Thieving picks these locks and skips the long route",
			End.NONE, "the locked doors", null),
		// "Go north through the passageway, over the blade, and through another passageway."
		LONG_PASSAGE(Part.LONG, "Enter", "North through the passageway, over the blade", "Then through another passageway", End.BLADE,
			"the first passageway", Obstacle.PASSAGEWAY),
		// "Go east and stand before the traps (see screenshot), then click on the passageway while RUNNING"
		RUN_PASSAGE(Part.LONG, "Run", "East: stop before the traps, then run to the passageway", "Running, so the traps do not go off", End.NONE,
			"the running passageway", Obstacle.PASSAGEWAY),
		// "Walk carefully around the spinning blades and climb the ledge."
		SPIN_LEDGE(Part.LONG, "Climb", "Around the spinning blades, then climb the ledge", "Spinning blades cannot be disabled", End.LEDGE,
			"the spinning blades", Obstacle.LEDGE),
		// "Right click on the first wall and use the function "Search wall" ... Then RUN past the other three walls."
		FOUR_WALLS(Part.LONG, "Search", "Search the first wall, then run past the other three", "Search wall disables this trap for a while", End.TRAP,
			"the four walls", Obstacle.WALL_TRAP),
		// "Grab the south-eastern tile (see screenshot) and open the door."
		TILE(Part.LONG, "Take", "Take the south-eastern tile", "Only the one called Tile opens the door, not the Tiles", End.TILE_TAKEN, "the tiles", null),
		// "Click on the tile in the puzzle screen and you can proceed."
		MOSAIC(Part.LONG, "Open", "Open the door and click the tile in the puzzle", "The bottom right is the correct tile", End.TILE_USED, "the mosaic door",
			Obstacle.MOSAIC_DOOR),
		// "Pass through the grills as shown in the screenshot."
		GRILLS(Part.LONG, "Pass", "Through the grills by the one correct path", "There is only one correct path, on the wiki's map", End.NONE, "the grills",
			Obstacle.GRILL),
		// "Walk all the way to the south. Go to the west and stand still right before the three wall traps, ... Then RUN past them."
		THREE_WALLS(Part.LONG, "Search", "South, west: search the first of three walls", "Then run past them", End.TRAP, "the three walls",
			Obstacle.WALL_TRAP),
		// "Enter the next room, stop right before the traps and RUN past them. Do the same for the next room and click on the pendulum."
		LONG_PENDULUM(Part.LONG, "Run", "Run past the traps, then the pendulum", "A single floor trap waits in the corridor after it", End.PENDULUM,
			"the long route's pendulum", Obstacle.PENDULUM),
		// "Go to the left and again stop right before the first floor trap, then RUN past all of them."
		LONG_FLOOR(Part.LONG, "Run", "Left: stop before the first floor trap, run past them all", "Running, so they do not go off", End.NONE,
			"the last floor traps", null),
		// "Ignore the spinning blades. Stop before the first floor trap and RUN past all of them."
		FINAL_TRAPS(Part.FINISH, "Run", "Past the spinning blades: stop, then run past the floor traps", "Ignore the spinning blades", End.NONE,
			"the final floor traps", null),
		// "Pick up the Flash powder, but do not get close to a guard."
		POWDER(Part.FINISH, "Take", "Pick up the flash powder", "Do not get close to a guard", End.POWDER_TAKEN, "the flash powder", null),
		// "Then, use the powder on the guard and RUN past him while he is stunned."
		GUARD(Part.FINISH, "Use", "Use the flash powder on the guard, then run past", "While he is stunned", End.POWDER_USED, "the guard", null),
		// "Move past the last two pendulums and you have completed the maze."
		LAST_PENDULUMS(Part.FINISH, "Pass", "Past the last two pendulums", "Keep at least 6 Agility for them", End.PENDULUM, "the last two pendulums",
			Obstacle.PENDULUM),
		// "Crack any of wall safes and you will teleport out of the maze."
		SAFE(Part.FINISH, "Crack", "Crack any wall safe", "It teleports you out; the crack can fail", End.SAFE, "the wall safe", Obstacle.WALL_SAFE);

		private final Part part;
		private final String verb;
		private final String title;
		private final String detail;
		private final End end;
		private final String where;
		private final Obstacle obstacle;

		Leg(Part part, String verb, String title, String detail, End end, String where, Obstacle obstacle)
		{
			this.part = part;
			this.verb = verb;
			this.title = title;
			this.detail = detail;
			this.end = end;
			this.where = where;
			this.obstacle = obstacle;
		}

		public Part part()
		{
			return part;
		}

		public String verb()
		{
			return verb;
		}

		public String title()
		{
			return title;
		}

		public String detail()
		{
			return detail;
		}

		public End end()
		{
			return end;
		}

		/** Where in the maze, for "sent back at the grills". */
		public String where()
		{
			return where;
		}

		/** The obstacle to light for this leg, or null when there is none to click. */
		public Obstacle obstacle()
		{
			return obstacle;
		}

		/** How many of its event end it: "the last two pendulums". */
		public int times()
		{
			return this == LAST_PENDULUMS ? 2 : 1;
		}
	}

	/** The way through: the start, then the shortcut with 80 Thieving or the long route, then the finish. */
	public static List<Leg> route(boolean shortcut)
	{
		List<Leg> out = new ArrayList<>();
		for (Leg l : Leg.values())
		{
			if (l.part() == Part.START || l.part() == Part.FINISH || l.part() == (shortcut ? Part.SHORTCUT : Part.LONG))
			{
				out.add(l);
			}
		}
		return Collections.unmodifiableList(out);
	}

	/** The five pieces, in the wiki's order: "Rogue mask, Rogue top, Rogue gloves, Rogue trousers, and Rogue boots". */
	public enum Piece
	{
		MASK("Rogue mask", ItemID.ROGUESDEN_HELM),
		TOP("Rogue top", ItemID.ROGUESDEN_BODY),
		GLOVES("Rogue gloves", ItemID.ROGUESDEN_GLOVES),
		TROUSERS("Rogue trousers", ItemID.ROGUESDEN_LEGS),
		BOOTS("Rogue boots", ItemID.ROGUESDEN_BOOTS);

		private final String title;
		private final int item;

		Piece(String title, int item)
		{
			this.title = title;
			this.item = item;
		}

		public String getTitle()
		{
			return title;
		}

		public int item()
		{
			return item;
		}

		public static Piece of(int item)
		{
			for (Piece p : values())
			{
				if (p.item == item)
				{
					return p;
				}
			}
			return null;
		}
	}

	/** "Each piece increases the chance of receiving double loot by 15%, unless all five components are worn together, in which case ... 100%." */
	public static int doubleLootPercent(int worn)
	{
		if (worn >= Piece.values().length)
		{
			return 100;
		}
		return Math.max(0, worn) * 15;
	}

	/** The wiki's table "Chance for Rogue's equipment crate each attempt", in tenths of a percent, by Thieving level. */
	private static final int[] TABLE_LEVEL = {50, 60, 70, 80, 90, 99};
	private static final int[] TABLE_PERMILLE = {392, 408, 423, 439, 455, 469};
	/** "Expected attempts to crack a safe for full set" and "Expected time to get full set" (minutes). */
	private static final int[] TABLE_ATTEMPTS = {13, 12, 12, 11, 11, 11};
	private static final int[] TABLE_MINUTES = {48, 45, 42, 23, 22, 22};

	/** The row of the wiki's table at or below a Thieving level; the first row below 50. */
	private static int row(int thieving)
	{
		int r = 0;
		for (int i = 0; i < TABLE_LEVEL.length; i++)
		{
			if (thieving >= TABLE_LEVEL[i])
			{
				r = i;
			}
		}
		return r;
	}

	/** The level of the table's row used for a Thieving level. */
	public static int tableLevel(int thieving)
	{
		return TABLE_LEVEL[row(thieving)];
	}

	/**
	 * The chance of a crate from one attempt at the safe, from the wiki's table at the row at or
	 * below your level. "The percentage above includes failures to crack the safe as well as the
	 * chance of getting a rogue kit instead."
	 */
	public static double crateChance(int thieving)
	{
		return TABLE_PERMILLE[row(thieving)] / 1000.0;
	}

	public static int wikiAttemptsForSet(int thieving)
	{
		return TABLE_ATTEMPTS[row(thieving)];
	}

	public static int wikiMinutesForSet(int thieving)
	{
		return TABLE_MINUTES[row(thieving)];
	}

	/** Attempts at the safe for this many crates more, on average, rounded as the wiki's table rounds them (5 at 60 is 12). */
	public static int attemptsFor(int crates, int thieving)
	{
		return crates <= 0 ? 0 : Math.max(1, (int) Math.round(crates / crateChance(thieving)));
	}

	/** The pieces by name, joined by commas. */
	public static String encodePieces(Set<Piece> pieces)
	{
		StringBuilder b = new StringBuilder();
		for (Piece p : pieces)
		{
			b.append(b.length() == 0 ? "" : ",").append(p.name());
		}
		return b.toString();
	}

	/** Reads a save as far as it can; a name this version does not know is left out. Never null. */
	public static EnumSet<Piece> decodePieces(String text)
	{
		EnumSet<Piece> out = EnumSet.noneOf(Piece.class);
		if (text == null)
		{
			return out;
		}
		for (String part : text.split(","))
		{
			try
			{
				out.add(Piece.valueOf(part.trim()));
			}
			catch (IllegalArgumentException e)
			{
				// Left out.
			}
		}
		return out;
	}

	private Maze()
	{
	}
}
