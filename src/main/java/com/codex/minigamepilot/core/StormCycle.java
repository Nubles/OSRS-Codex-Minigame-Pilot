package com.codex.minigamepilot.core;

/**
 * The one next step through a Tempoross game: fish, the double spot first when it is up, cook
 * at the shrine, load the ammunition crate, tether before each colossal wave, douse a fire in
 * your way, and harpoon the spirit pool while Tempoross is down.
 *
 * <p>Plain Java over a snapshot of the game, so every turn of it can be tested. The facts behind
 * it (OSRS wiki, Tempoross, Tempoross/Strategies, Spirit pool, Ammunition crate, Totem pole,
 * Raw harpoonfish, Spirit angler outfit):
 * <ul>
 * <li>harpoonfish are caught with a harpoon at 35 Fishing; "fishing at these spots [jumping
 * harpoonfish] allows for the chance of catching two harpoonfish at a time", and the strategies
 * page says "always fish at the double spots where possible";</li>
 * <li>raw harpoonfish loaded into the crates drain 10 of Tempoross's energy each, cooked ones 15;
 * they are cooked at the shrine;</li>
 * <li>once its energy is 0 the spirit pools become active, and harpooning them takes its essence
 * (55 points a scatter); the fight is won when essence reaches 0;</li>
 * <li>if the storm intensity reaches 100% before the energy is gone, a colossal wave washes
 * everyone away and their points are lost;</li>
 * <li>tether to a totem pole or mast with a rope (or wearing the whole spirit angler outfit)
 * before a colossal wave, or lose minigame items; lightning starts fires that spread and are
 * doused with a bucket of water (40 points).</li>
 * </ul>
 */
public final class StormCycle
{
	/** Energy a raw harpoonfish drains when loaded, and a cooked one (OSRS wiki, Tempoross). */
	public static final int RAW_ENERGY = 10;
	public static final int COOKED_ENERGY = 15;

	/** What the Pilot read this tick. */
	public static final class Snapshot
	{
		/** The Tempoross HUD is up: a game is under way. */
		public boolean inGame = true;
		/** A colossal wave has been called and has not hit yet. */
		public boolean waveIncoming;
		/** Tethered to a totem pole or mast now. */
		public boolean tethered;
		/** A rope carried, or the whole spirit angler outfit worn. */
		public boolean canTether;
		/** A crate of ropes is in sight. */
		public boolean ropeCrate = true;
		/** An unbroken totem or mast is in sight. */
		public boolean tetherInSight = true;
		/** The spirit pool is active: Tempoross is down. */
		public boolean poolActive;
		/** Read from the HUD, 0 to 100, or -1 when not shown. */
		public int energy = -1;
		public int essence = -1;
		public int storm = -1;
		public int raw;
		public int cooked;
		public int crystallised;
		public int free;
		public boolean harpoon = true;
		/** A fishing spot is up, and whether one is the double spot. */
		public boolean spot = true;
		public boolean doubleSpot;
		/** The "Cook before loading" setting. */
		public boolean cook = true;
		/** The "Load what you hold at storm" setting, a percentage; 0 or more than 100 turns it off. */
		public int stormLoad = 80;
	}

	/** Stages of the loop as the panel shows them. */
	public static final String[] LOOP = {"Fish", "Cook", "Load", "Attack"};

	private StormCycle()
	{
	}

	/** All the harpoonfish you hold. */
	public static int fish(Snapshot s)
	{
		return s.raw + s.cooked + s.crystallised;
	}

	/** The energy the fish you hold take off when loaded; crystallised ones are left out, as the wiki gives no figure. */
	public static int energyOff(Snapshot s)
	{
		return s.raw * RAW_ENERGY + s.cooked * COOKED_ENERGY;
	}

	public static Step next(Snapshot s)
	{
		if (!s.inGame)
		{
			return Step.waiting("Wait for the storm", "The game starts as the boat reaches the cove", Step.Target.NONE);
		}
		if (s.waveIncoming)
		{
			if (s.tethered)
			{
				return Step.waiting("Hold fast: the wave is passing", "Tethered, so your fish and gear are safe", Step.Target.TETHER);
			}
			if (s.canTether)
			{
				return Step.of("Tether", "Tether before the wave hits",
					s.tetherInSight ? "A colossal wave is coming: tether or lose your fish" : "A wave is coming: find an unbroken totem or mast",
					Step.Target.TETHER);
			}
			if (s.ropeCrate)
			{
				return Step.of("Take", "Take a rope", "A wave is coming and you need a rope to tether", Step.Target.SUPPLY_CRATE);
			}
		}
		if (!s.harpoon)
		{
			return Step.of("Take", "Take a harpoon", "Every catch here needs one", Step.Target.SUPPLY_CRATE);
		}
		if (s.poolActive)
		{
			return Step.of("Harpoon", "Harpoon the spirit pool",
				"Tempoross is down: each scatter takes essence" + (s.essence >= 0 ? " (" + s.essence + "% left)" : "") + " and gives 55 points",
				Step.Target.SPIRIT_POOL);
		}
		int fish = fish(s);
		if (s.stormLoad > 0 && s.stormLoad <= 100 && s.storm >= s.stormLoad && fish > 0)
		{
			return load(s, "The storm is at " + s.storm + "%: load your " + fish + " fish before it reaches 100%");
		}
		if (s.doubleSpot && s.free > 0)
		{
			return Step.of("Harpoon", "Fish at the double spot",
				"It can give two harpoonfish a catch · " + s.free + " free " + (s.free == 1 ? "slot" : "slots"), Step.Target.FISHING_SPOT);
		}
		if (s.free > 0 && s.spot)
		{
			return Step.of("Harpoon", "Catch harpoonfish",
				s.free + " free " + (s.free == 1 ? "slot" : "slots") + (fish > 0 ? " · " + fish + " fish so far" : ""), Step.Target.FISHING_SPOT);
		}
		if (s.cook && s.raw > 0)
		{
			return Step.of("Cook", "Cook at the shrine",
				s.raw + " raw: cooked fish drain " + COOKED_ENERGY + " energy each, raw " + RAW_ENERGY, Step.Target.SHRINE);
		}
		if (fish > 0)
		{
			return load(s, fish + " fish: about " + energyOff(s) + " energy off Tempoross"
				+ (s.crystallised > 0 ? ", the crystallised ones besides" : ""));
		}
		return Step.waiting("Wait for a fishing spot", "Spots come and go around the island", Step.Target.FISHING_SPOT);
	}

	private static Step load(Snapshot s, String why)
	{
		return Step.of("Fill", "Fill the ammunition crate", why, Step.Target.AMMO_CRATE);
	}

	/** Douse a fire that is in the way of a step. */
	public static Step douse(Step blocked, String place)
	{
		return Step.of("Douse", "Douse the fire", "It is in your way to " + place + " and spreads if left · 40 points", Step.Target.FIRE);
	}

	/** Fill buckets first, when a fire is in the way and none holds water. */
	public static Step fillBuckets(String place)
	{
		return Step.of("Use", "Fill a bucket at the pump", "A fire is in your way to " + place, Step.Target.WATER_PUMP);
	}

	/**
	 * Where a step sits in the loop: 0 fish, 1 cook, 2 load, 3 attack, or -1 for a step that
	 * breaks into the loop (a wave, a fire), which keeps the stage it broke into.
	 */
	public static int stage(Step step)
	{
		switch (step.getTarget())
		{
			case FISHING_SPOT:
				return 0;
			case SHRINE:
				return 1;
			case AMMO_CRATE:
				return 2;
			case SPIRIT_POOL:
				return 3;
			default:
				return -1;
		}
	}
}
