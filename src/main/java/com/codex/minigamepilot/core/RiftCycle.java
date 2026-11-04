package com.codex.minigamepilot.core;

/**
 * The one next step through a game of Guardians of the Rift: mine fragments, craft them into
 * guardian essence at the workbench, enter the best open altar, craft, come back, power up the
 * Great Guardian, place the charged cell, deposit the runes, and round again.
 *
 * <p>Plain Java over a snapshot of the game, so every turn of it can be tested. The facts behind
 * it (OSRS wiki, Guardians of the Rift and its Strategies page): a fragment makes one essence at
 * the workbench; crafting at an altar with an uncharged cell charges it; guardian stones are used
 * on the Great Guardian and charged cells on the cell tiles for energy; runes go in the deposit
 * pool; the huge guardian remains, through a portal that stays open about 25 seconds, give
 * essence straight away; and the general strategy mines about 120 fragments before the first
 * craft.
 */
public final class RiftCycle
{
	/** The essence a trip to the huge remains has to be worth: less than this is not worth the walk. */
	static final int HUGE_REMAINS_WORTH = 10;

	/** What the Pilot read this tick. */
	public static final class Snapshot
	{
		/** Inside a runic altar, and which, or null in the temple. */
		public Altar inAltar;
		/** On the shelf of the huge guardian remains. */
		public boolean atHugeRemains;
		/** The rift is open: the game is under way. */
		public boolean started;
		/** An altar has been crafted at this game, so the first long mining spell is over. */
		public boolean craftedThisGame;
		public int fragments;
		public int essence;
		public int free;
		/** Essence in your pouches, and room left in them. */
		public int pouchStored;
		public int pouchRoom;
		/** Your pouches, for lighting them up. */
		public int[] pouches = new int[0];
		public int stones;
		/** The charged cell you hold, or null. */
		public Altar.Cell chargedCell;
		public int unchargedCells;
		public boolean runes;
		/** The portal to the huge remains is open. */
		public boolean riftPortalOpen;
		/** The altar to craft at, or null when none of yours is open, and why. */
		public Altar pick;
		public String pickWhy;
		/** The fragments to mine before the first craft of a game. */
		public int firstTarget = 120;
		/** Whether the uncharged cells table is in sight, so a step can point at it. */
		public boolean cellTable = true;
	}

	/** Stages of the loop as the panel shows them. */
	public static final String[] LOOP = {"Mine", "Essence", "Altar", "Hand in"};

	private RiftCycle()
	{
	}

	/** The essence a trip still has room for: free slots and room in your pouches. */
	public static int room(Snapshot s)
	{
		return Math.max(0, s.free) + Math.max(0, s.pouchRoom);
	}

	/** The fragments to have before crafting essence: enough for one trip, or the first spell's target. */
	public static int target(Snapshot s)
	{
		int room = room(s);
		return s.craftedThisGame ? room : Math.max(s.firstTarget, room);
	}

	public static Step next(Snapshot s)
	{
		if (s.inAltar != null)
		{
			return inAltar(s);
		}
		if (s.atHugeRemains)
		{
			if (s.free > 0)
			{
				return Step.of("Mine", "Mine the huge remains", "They give essence straight away, no workbench", Step.Target.HUGE_REMAINS);
			}
			if (s.pouchRoom > 0 && s.essence > 0)
			{
				return fill(s, "Then mine again to fill your pack");
			}
			return Step.of("Enter", "Take the portal back", "Pack and pouches are full of essence", Step.Target.RIFT_PORTAL);
		}
		if (!s.started)
		{
			if (s.unchargedCells == 0 && s.cellTable)
			{
				return Step.of("Take", "Take uncharged cells", "Each altar trip charges one into a cell worth energy", Step.Target.CELLS);
			}
			return Step.waiting("Wait for the rift to open", "Mining starts the moment it opens", Step.Target.REMAINS);
		}
		// Back from an altar: hand everything in before the next round.
		if (s.stones > 0)
		{
			return Step.of("Power-up", "Power up the Great Guardian",
				s.stones + (s.stones == 1 ? " guardian stone" : " guardian stones") + ": energy and power",
				Step.Target.GREAT_GUARDIAN);
		}
		if (s.chargedCell != null)
		{
			return Step.of("Place", "Place the " + s.chargedCell.word() + " cell",
				"Energy too, and you can only hold one", Step.Target.CELL_TILE);
		}
		if (s.runes)
		{
			return Step.of("Deposit", "Deposit your runes", "They go to your bank, clearing the pack", Step.Target.DEPOSIT_POOL);
		}
		int carried = s.essence + s.pouchStored;
		int room = room(s);
		if (s.essence > 0 && s.free == 0 && s.pouchRoom > 0)
		{
			return fill(s, "Pack full: your pouches take " + s.pouchRoom + " more");
		}
		if (carried > 0 && (room == 0 || s.fragments == 0))
		{
			if (s.pick == null)
			{
				return Step.waiting("Wait for an altar you can use", s.pickWhy == null ? "Altars change every so often" : s.pickWhy, Step.Target.WORKBENCH);
			}
			if (s.unchargedCells == 0 && s.chargedCell == null && s.cellTable)
			{
				return Step.of("Take", "Take uncharged cells", "So this trip to " + s.pick.rune().toLowerCase() + " charges a cell too", Step.Target.CELLS);
			}
			return Step.of("Enter", "Enter the " + s.pick.rune().toLowerCase() + " altar",
				s.pickWhy == null ? carried + " essence" : s.pickWhy, Step.Target.GUARDIAN);
		}
		if (s.riftPortalOpen && carried == 0 && s.fragments < room && room >= HUGE_REMAINS_WORTH)
		{
			return Step.of("Enter", "Take the portal to the huge remains", "Essence straight away; it stays open about 25 seconds", Step.Target.RIFT_PORTAL);
		}
		int target = target(s);
		if (s.fragments > 0 && (carried > 0 && room > 0 || s.fragments >= target))
		{
			int make = Math.min(s.fragments, room);
			return Step.of("Craft", "Craft guardian essence",
				make + " of your " + s.fragments + " fragments fill your pack" + (s.pouchRoom > 0 ? " and pouches" : ""), Step.Target.WORKBENCH);
		}
		int left = Math.max(0, target - s.fragments);
		String why = s.craftedThisGame
			? left + " more for a full trip"
			: left + " more to " + target + " before the first craft";
		return Step.of("Mine", "Mine guardian fragments", why, Step.Target.REMAINS);
	}

	private static Step inAltar(Snapshot s)
	{
		String rune = s.inAltar.rune().toLowerCase();
		if (s.essence > 0)
		{
			return Step.of("Craft-rune", "Craft " + rune + " runes",
				s.essence + " essence" + (s.unchargedCells > 0 ? " · the cell becomes " + s.inAltar.cell().word() : ""),
				Step.Target.ALTAR);
		}
		if (s.pouchStored > 0 && s.free > 0)
		{
			return Step.items("Empty", "Empty your pouches", s.pouchStored + " essence still in them", s.pouches);
		}
		return Step.of("Use", "Take the portal back", "Back to the temple to hand in the stones and runes", Step.Target.EXIT);
	}

	/** "an overcharged", "a weak". */
	static String article(String word)
	{
		return ("aeiou".indexOf(Character.toLowerCase(word.charAt(0))) >= 0 ? "an " : "a ") + word;
	}

	private static Step fill(Snapshot s, String why)
	{
		return Step.items("Fill", "Fill your pouches", why, s.pouches);
	}

	/** Where a step sits in the loop: 0 mine, 1 essence, 2 altar, 3 hand in. */
	public static int stage(Step step, boolean inAltar)
	{
		switch (step.getTarget())
		{
			case REMAINS:
			case RIFT_PORTAL:
			case HUGE_REMAINS:
				return 0;
			case WORKBENCH:
			case CELLS:
				return 1;
			case ITEM:
				return inAltar ? 2 : 1;
			case GUARDIAN:
			case ALTAR:
			case EXIT:
				return 2;
			case GREAT_GUARDIAN:
			case CELL_TILE:
			case DEPOSIT_POOL:
				return 3;
			default:
				return 0;
		}
	}
}
