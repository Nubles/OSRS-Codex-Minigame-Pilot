package com.codex.minigamepilot.core;

/**
 * The Motherlode Mine loop: mine pay-dirt, wash it through the hopper until the sack is full,
 * then empty the sack into the bank and go again.
 *
 * <p>The cycle sizes each load so the sack fills exactly: when the sack has room for 19 more,
 * it says to mine 19, not a full inventory that the hopper would turn away. Pay-dirt still
 * washing through the wheel is counted as in flight, so the sack is never overfilled by a load
 * that has not reached it yet. It remembers whether it is filling or emptying the sack, since
 * the same inventory means different things on the way in and on the way out.
 */
public final class MotherlodeCycle
{
	/** Ticks with pay-dirt in flight and nothing reaching the sack before the count is dropped. */
	static final int FLIGHT_TIMEOUT = 60;

	/** What the game shows this tick. */
	public static final class Snapshot
	{
		/** Pay-dirt carried. */
		public int payDirt;
		/** Empty inventory slots. */
		public int free;
		/** Ore and uncut gems carried, which go to the bank. */
		public int loot;
		/** Pay-dirt waiting in the sack. */
		public int sack;
		/** 108, or 189 with the bigger sack. */
		public int capacity = 108;
		/** Struts that are broken; with both down the wheel stops. */
		public int brokenStruts;
		public boolean hammer;
	}

	private boolean emptying;
	private int inFlight;
	private int lastPayDirt = -1;
	private int lastSack = -1;
	private int idle;
	private int deposited;

	/**
	 * Follows pay-dirt from the inventory into the hopper and out into the sack.
	 *
	 * @param nearHopper whether the player is at the hopper, so pay-dirt leaving the inventory went into it
	 * @param wheelStopped whether both struts are broken, which holds pay-dirt in the water
	 */
	public void observe(int payDirt, int sack, boolean nearHopper, boolean wheelStopped)
	{
		if (lastPayDirt >= 0 && payDirt < lastPayDirt && nearHopper)
		{
			int went = lastPayDirt - payDirt;
			inFlight += went;
			deposited += went;
			idle = 0;
		}
		if (lastSack >= 0 && sack > lastSack)
		{
			inFlight = Math.max(0, inFlight - (sack - lastSack));
			idle = 0;
		}
		if (inFlight > 0 && !wheelStopped && ++idle > FLIGHT_TIMEOUT)
		{
			inFlight = 0;
		}
		lastPayDirt = payDirt;
		lastSack = sack;
	}

	/**
	 * After logging in or hopping: the last readings are no base for change, but pay-dirt already
	 * in the hopper is still on its way to the sack.
	 */
	public void relog()
	{
		lastPayDirt = -1;
		lastSack = -1;
	}

	/** Forgets what was in flight, such as after leaving the mine. */
	public void reset()
	{
		inFlight = 0;
		lastPayDirt = -1;
		lastSack = -1;
		idle = 0;
	}

	public int getInFlight()
	{
		return inFlight;
	}

	/** Pay-dirt put in the hopper since the cycle began. */
	public int getDeposited()
	{
		return deposited;
	}

	public boolean isEmptying()
	{
		return emptying;
	}

	/** Room left in the sack once everything in flight has landed. */
	public int room(Snapshot s)
	{
		return Math.max(0, s.capacity - s.sack - inFlight);
	}

	/** How much pay-dirt this load should hold: what fits in the bag, capped by what the sack can still take. */
	public int loadSize(Snapshot s)
	{
		return Math.min(s.free + s.payDirt, room(s));
	}

	public Step next(Snapshot s)
	{
		int room = room(s);
		boolean wheelStopped = s.brokenStruts >= 2;

		if (!emptying && s.payDirt == 0 && room == 0)
		{
			emptying = true;
		}
		if (emptying && s.sack == 0 && inFlight == 0 && s.loot == 0)
		{
			emptying = false;
		}

		if (emptying)
		{
			return emptyingStep(s, room, wheelStopped);
		}

		int load = loadSize(s);
		if (s.payDirt > 0 && (s.payDirt >= load || s.free == 0))
		{
			if (room == 0)
			{
				// Mined more than the sack can take; it must be emptied, which needs room in the bag.
				return Step.of("Bank", "Bank the pay-dirt you are carrying",
					"The sack is full, and emptying it needs free space", Step.Target.BANK);
			}
			int after = s.sack + inFlight + Math.min(s.payDirt, room);
			return Step.of("Deposit", "Deposit your pay-dirt in the hopper",
				s.payDirt + " pay-dirt in, sack " + after + " of " + s.capacity + " once it lands", Step.Target.HOPPER);
		}
		if (wheelStopped && inFlight > 0)
		{
			return repair(s);
		}
		if (s.free == 0)
		{
			return Step.of("Bank", "Bank your ore and gems", "Your inventory is full", Step.Target.BANK);
		}
		int more = load - s.payDirt;
		String detail = more == 1 ? "1 more fills this load" : more + " more fills this load";
		if (room < s.free + s.payDirt)
		{
			detail += ", which fills the sack";
		}
		return Step.of("Mine", "Mine pay-dirt", detail, Step.Target.VEIN);
	}

	private Step emptyingStep(Snapshot s, int room, boolean wheelStopped)
	{
		if (s.payDirt > 0 && room > 0)
		{
			return Step.of("Deposit", "Deposit your pay-dirt in the hopper", "The sack still has room for " + room, Step.Target.HOPPER);
		}
		if (s.free == 0 || s.payDirt > 0)
		{
			String left = s.sack > 0 ? s.sack + " pay-dirt still in the sack" : "Then mine the next sackful";
			return Step.of("Bank", s.payDirt > 0 ? "Bank your inventory" : "Bank your ore and gems", left, Step.Target.BANK);
		}
		if (s.sack > 0)
		{
			int take = Math.min(s.free, s.sack);
			String detail = take == s.sack ? "Takes the last " + take : "Takes " + take + " of " + s.sack + ", then bank";
			return Step.of("Collect", "Search the sack", detail, Step.Target.SACK);
		}
		if (inFlight > 0)
		{
			if (wheelStopped)
			{
				return repair(s);
			}
			return Step.waiting("Wait by the sack", inFlight + " pay-dirt is still washing through", Step.Target.SACK);
		}
		return Step.of("Bank", "Bank your ore and gems", "The sack is empty: then mine the next sackful", Step.Target.BANK);
	}

	private static Step repair(Snapshot s)
	{
		String detail = s.hammer ? "Both struts are broken, so the wheel has stopped"
			: "Both struts are broken: take a hammer from the crate first";
		return Step.of("Repair", "Hammer a broken strut", detail, Step.Target.STRUT);
	}
}
