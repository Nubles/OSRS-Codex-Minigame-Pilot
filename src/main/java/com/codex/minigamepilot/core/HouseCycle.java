package com.codex.minigamepilot.core;

import com.codex.minigamepilot.core.Houses.House;

/**
 * Works out the one next thing to do when stealing valuables: pickpocket the citizen an urchin
 * has distracted (or any, for house keys), go to the vacant house once its owner has left, search
 * its chests, wardrobes and jewellery cases, leave by the open window before the owner is back,
 * and trade the valuables in at Oriana.
 *
 * <p>Facts are from the OSRS wiki (see {@link Houses}). It only reads; every click is yours.
 */
public final class HouseCycle
{
	/** The stages of a round, for the loop strip. */
	public static final String[] LOOP = {"Keys", "House", "Loot", "Oriana"};
	/** Leave by the window when the owner may be back within this many seconds. */
	public static final int LEAVE_AT = 20;
	/** A vacant house with less than this many seconds left is not worth a key. */
	public static final int TOO_LATE = 40;
	/** Wait by the citizens when the next distraction may come within this many seconds. */
	public static final int SOON = 10;

	private HouseCycle()
	{
	}

	/** Everything a decision needs, read from the game. */
	public static final class Snapshot
	{
		/** Thieving, boosts included. */
		public int level;
		public int keys;
		public int valuables;
		public int free;
		/** What Oriana pays you a valuable: 55, or 65 once seen. */
		public int rate = Houses.ORIANA_PAYS;

		/** You are inside a house you unlocked; its owner, or null when not known. */
		public boolean inside;
		public House insideHouse;
		/** The whistle: the owner is on the way back. */
		public boolean ownerBack;
		/** The flashing arrow is up: "You notice something shine". */
		public boolean shine;
		/** Valuables came in the last few ticks: a search is running. */
		public boolean searching;

		/** A house is vacant (its Locked Door seen); its owner when known. */
		public boolean vacant;
		public House vacantHouse;
		/** Its Locked Door is in the scene. */
		public boolean doorInSight;
		/** The owner of the vacant house is still about its door. */
		public boolean ownerNear;
		/** The fewest and most seconds before the owner may be back, or -1 when not known. */
		public int leftMin = -1;
		public int leftMax = -1;

		/** A citizen an urchin is distracting, and seconds the distraction may have left. */
		public boolean distracted;
		public int distractedLeft;
		/** Seconds to the next distraction at the soonest and latest, or -1 when not known. */
		public int nextMin = -1;
		public int nextMax = -1;
		/** Wealthy citizens in sight. */
		public boolean citizens;
		/** Oriana in sight, and a street urchin who tells you the vacant house. */
		public boolean oriana;
		public boolean teller;
	}

	/** A step and where it is in the round. */
	public static final class Decision
	{
		public final Step step;
		public final int stage;

		Decision(Step step, int stage)
		{
			this.step = step;
			this.stage = stage;
		}
	}

	public static Decision next(Snapshot s)
	{
		if (s.inside)
		{
			return inside(s);
		}
		if (s.level < Houses.LEVEL)
		{
			return new Decision(Step.waiting("Stealing valuables needs 50 Thieving",
				"Yours is " + s.level + "; it also needs Children of the Sun", Step.Target.NONE), 0);
		}
		if (s.distracted)
		{
			return new Decision(Step.of("Pickpocket", "Pickpocket the distracted citizen",
				"Every pick succeeds for " + range(Math.max(1, s.distractedLeft), Math.max(1, s.distractedLeft)) + " more", Step.Target.CITIZEN), 0);
		}
		boolean open = s.vacant && (s.leftMax < 0 || s.leftMax >= TOO_LATE);
		if (s.keys > 0 && open)
		{
			String name = s.vacantHouse == null ? "the locked house" : s.vacantHouse.title();
			String time = s.leftMin >= 0 ? "Back in " + range(s.leftMin, s.leftMax) : "Out for about 3:00 from leaving";
			if (s.ownerNear)
			{
				String who = s.vacantHouse == null ? "The owner" : s.vacantHouse.owner();
				return new Decision(Step.waiting(who + " is still by the door",
					"Go in once " + (s.vacantHouse == null ? "they have" : s.vacantHouse == House.LAVINIA ? "she has" : "he has")
						+ " walked off", Step.Target.LOCKED_DOOR), 1);
			}
			if (!s.doorInSight)
			{
				return new Decision(Step.of("Go", "Go to " + name,
					s.vacantHouse == null ? time : capital(s.vacantHouse.where()), Step.Target.HOUSE), 1);
			}
			return new Decision(Step.of("Open", "Open the locked door of " + name,
				time + "; out by an open window", Step.Target.LOCKED_DOOR), 1);
		}
		if (s.keys > 0)
		{
			if (s.teller && s.valuables >= Houses.URCHIN_FEE)
			{
				return new Decision(Step.of("Talk-to", "Ask the urchin which house is vacant",
					s.vacant ? "Too little time left at this one; 15 valuables" : "15 valuables; only one house is open at a time", Step.Target.URCHIN), 1);
			}
			if (!s.vacant)
			{
				return new Decision(Step.of("Look", "Find the house with a Locked Door",
					s.nextMin >= 0 ? "One is vacant at a time; distraction in " + range(s.nextMin, s.nextMax)
						: "Only one of the three is vacant at a time", Step.Target.NONE), 1);
			}
		}
		if (s.valuables > 0 && s.oriana)
		{
			return new Decision(Step.of("Trade-in", "Trade your valuables in to Oriana",
				format(s.valuables) + " valuables at " + s.rate + " coins: " + format((long) s.valuables * s.rate) + " coins", Step.Target.ORIANA), 3);
		}
		if (s.nextMin >= 0 && s.nextMin <= SOON && s.citizens)
		{
			return new Decision(Step.waiting("Wait by the citizens: an urchin is due",
				"A distraction in " + range(s.nextMin, s.nextMax) + "; then every pick succeeds", Step.Target.CITIZEN), 0);
		}
		String why;
		if (s.keys > 0)
		{
			why = "Too little time at " + (s.vacantHouse == null ? "the locked house" : s.vacantHouse.title()) + ": keep the "
				+ (s.keys == 1 ? "key" : "keys");
		}
		else if (s.nextMin >= 0)
		{
			why = "A key about 5 in 85; distraction in " + range(s.nextMin, s.nextMax);
		}
		else
		{
			why = "A house key about 5 in 85 picks; a miss stuns " + Houses.STUN_SECONDS + "s";
		}
		return new Decision(Step.of("Pickpocket", "Pickpocket a wealthy citizen", why, Step.Target.CITIZEN), 0);
	}

	private static Decision inside(Snapshot s)
	{
		String owner = s.insideHouse == null ? "The owner" : s.insideHouse.owner();
		if (s.ownerBack || s.leftMin >= 0 && s.leftMin <= LEAVE_AT)
		{
			return new Decision(Step.of("Exit", "Leave by the open window now",
				(s.ownerBack ? owner + " is coming back" : owner + " may be back in " + range(s.leftMin, s.leftMax))
					+ ": caught, you lose about 90%", Step.Target.WINDOW), 2);
		}
		String left = s.leftMin >= 0 ? owner + " back in " + range(s.leftMin, s.leftMax) : owner + " is out about 3:00 from leaving";
		if (s.free <= 0 && !s.searching)
		{
			return new Decision(Step.waiting("Free an inventory slot", "Searching needs one free space", Step.Target.NONE), 2);
		}
		if (s.shine)
		{
			return new Decision(Step.of("Search", "Search where the flashing arrow points",
				"Once: 14 valuables and 630 xp, gone in about 7s", Step.Target.LOOT), 2);
		}
		if (s.searching)
		{
			return new Decision(Step.waiting("Keep searching: valuables keep coming", left, Step.Target.LOOT), 2);
		}
		return new Decision(Step.of("Search", "Search a chest, wardrobe or jewellery case", left, Step.Target.LOOT), 2);
	}

	private static String capital(String s)
	{
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	/** "40-55s", "about 12s", or "up to 9s". */
	static String range(int min, int max)
	{
		int a = Math.max(0, min);
		int b = Math.max(a, max);
		if (b == a)
		{
			return "about " + a + "s";
		}
		if (a == 0)
		{
			return "up to " + b + "s";
		}
		return a + "-" + b + "s";
	}

	private static String format(long n)
	{
		return String.format(java.util.Locale.ROOT, "%,d", n);
	}
}
