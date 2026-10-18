package com.codex.minigamepilot.core;

/**
 * The Blast Furnace loop: bank, load the conveyor belt, collect bars, bank again.
 *
 * <p>What makes it work is choosing the trip at the bank. A bar needs coal already in the
 * furnace, so the cycle compares the coal waiting there (plus a full coal bag) with what a load
 * of ore will burn, and calls a coal trip only when the ore would otherwise sit unsmelted. For
 * steel with a coal bag that is never; for runite it is three coal trips to each ore trip.
 */
public final class FurnaceCycle
{
	/** Bars the dispenser holds before more ore just waits in the furnace. */
	static final int DISPENSER_FULL = 27;
	/** Ticks with ore or coal on the belt and nothing reaching the furnace before the count is dropped. */
	static final int BELT_TIMEOUT = 20;

	/** What the game shows this tick. */
	public static final class Snapshot
	{
		public Bar bar = Bar.STEEL;
		/** Coal and this bar's ore waiting in the furnace. */
		public int furnaceCoal;
		public int furnaceOre;
		/** Finished bars of this kind in the dispenser. */
		public int dispenserBars;
		/** The dispenser varbit: 0 empty, 1 still smelting, 2 hot (ice gloves to take), 3 cooled. */
		public int dispenserState;
		public int ore;
		public int coal;
		/** Bars of any kind carried. */
		public int bars;
		public int free;
		/** Inventory slots not taken by ore, coal or bars: a coal bag, gloves, a stamina potion. */
		public int fixed;
		public boolean coalBag;
		/** Coal in the bag, or -1 when it has not been seen. */
		public int bagCoal;
		public int bagCapacity = 27;
		public boolean bankOpen;
		public boolean iceGlovesWorn;
		public boolean iceGlovesCarried;
		public boolean goldsmithWorn;
		public boolean goldsmithCarried;
		/** Coins in the coffer. */
		public int coffer;
		/** Below this the coffer should be topped up. */
		public int cofferLow = 2500;
		/** Whether the coffer matters: under level 60 Smithing the foreman is paid instead. */
		public boolean usesCoffer = true;
		/** This bar's ore and coal in the bank, or -1 when the bank has not been seen. */
		public int bankOre = -1;
		public int bankCoal = -1;
	}

	private int onBelt;
	private int lastCarried = -1;
	private int lastFurnace = -1;
	private int idle;

	/**
	 * Follows ore and coal from the inventory onto the belt and into the furnace.
	 *
	 * @param carried ore and coal carried
	 * @param inFurnace ore and coal in the furnace
	 * @param atBelt whether the player is at the conveyor belt
	 */
	public void observe(int carried, int inFurnace, boolean atBelt)
	{
		if (lastCarried >= 0 && carried < lastCarried && atBelt)
		{
			onBelt += lastCarried - carried;
			idle = 0;
		}
		if (lastFurnace >= 0 && inFurnace > lastFurnace)
		{
			onBelt = Math.max(0, onBelt - (inFurnace - lastFurnace));
			idle = 0;
		}
		if (onBelt > 0 && ++idle > BELT_TIMEOUT)
		{
			onBelt = 0;
		}
		lastCarried = carried;
		lastFurnace = inFurnace;
	}

	public void reset()
	{
		onBelt = 0;
		lastCarried = -1;
		lastFurnace = -1;
		idle = 0;
	}

	/** Ore and coal on the belt that has not reached the furnace yet. */
	public int getOnBelt()
	{
		return onBelt;
	}

	/** Ore one trip carries: every slot not taken by the coal bag, gloves and the like. */
	public static int load(Snapshot s)
	{
		return Math.max(1, 28 - s.fixed);
	}

	/** Coal one trip brings: a full inventory plus a full bag. */
	public static int coalTrip(Snapshot s)
	{
		return load(s) + (s.coalBag ? s.bagCapacity : 0);
	}

	/** Whether the next trip from the bank should carry coal rather than ore. */
	public static boolean coalRunNext(Snapshot s)
	{
		if (s.bar.getCoal() == 0)
		{
			return false;
		}
		int bag = s.coalBag ? s.bagCapacity : 0;
		return s.furnaceCoal + bag < s.bar.getCoal() * load(s);
	}

	private boolean dispenserReady(Snapshot s)
	{
		return s.dispenserBars > 0 && (s.dispenserState == 3 || (s.dispenserState == 2 && s.iceGlovesWorn));
	}

	public Step next(Snapshot s)
	{
		Bar bar = s.bar;
		boolean needsCoal = bar.getCoal() > 0;
		int bag = Math.max(0, s.bagCoal);

		if (s.usesCoffer && s.coffer < s.cofferLow && !s.bankOpen)
		{
			int minutes = s.coffer / 1200;
			return Step.of("Pay", "Top up the coffer", s.coffer == 0
				? "The furnace stops without coins in it"
				: "About " + (minutes <= 1 ? "a minute" : minutes + " minutes") + " left", Step.Target.COFFER);
		}

		if (s.bankOpen)
		{
			return atBank(s, needsCoal, bag);
		}

		// Bars waiting come out before anything else, so the dispenser never backs up.
		boolean carryingLoad = s.ore > 0 || s.coal > 0;
		if (s.dispenserBars > 0 && !carryingLoad && s.bars == 0)
		{
			if (s.dispenserState == 2 && !s.iceGlovesWorn)
			{
				if (s.iceGlovesCarried)
				{
					return Step.items("Equip", "Put on your ice gloves", "The bars are too hot to touch without them", ItemIds.ICE_GLOVES);
				}
				return Step.waiting("Wait for the bars to cool", "Ice gloves let you take them hot", Step.Target.DISPENSER);
			}
			if (dispenserReady(s))
			{
				return Step.of("Take", "Take the bars from the dispenser",
					s.dispenserBars + " " + bar.barName(s.dispenserBars), Step.Target.DISPENSER);
			}
		}

		if (carryingLoad)
		{
			if (s.dispenserBars >= DISPENSER_FULL && s.bars == 0 && s.free > 0 && dispenserReady(s))
			{
				return Step.of("Take", "Take the bars first", "The dispenser is full", Step.Target.DISPENSER);
			}
			if (bar == Bar.GOLD && s.ore > 0 && s.goldsmithCarried && !s.goldsmithWorn)
			{
				return Step.items("Equip", "Put on your goldsmith gauntlets",
					"They more than double the experience from gold bars", ItemIds.GOLDSMITH_GAUNTLETS);
			}
			String what = s.ore > 0 && s.coal > 0 ? "ore and coal" : s.ore > 0 ? bar.oreName() : "coal";
			return Step.of("Load", "Put the " + what + " on the conveyor belt",
				(s.ore + s.coal) + " to load", Step.Target.BELT);
		}

		if (needsCoal && s.coalBag && bag > 0 && s.free > 0)
		{
			return Step.items("Empty", "Empty the coal bag", bag + " coal, then load the belt again", ItemIds.COAL_BAG, ItemIds.COAL_BAG_OPEN);
		}

		if (s.bars > 0)
		{
			return Step.of("Bank", "Bank your bars", "Then the next load", Step.Target.BANK);
		}

		boolean smelting = s.furnaceOre > 0 || onBelt > 0 || s.dispenserBars > 0;
		if (smelting && (s.furnaceOre == 0 || s.furnaceCoal >= bar.getCoal()))
		{
			if (s.iceGlovesCarried && !s.iceGlovesWorn)
			{
				return Step.items("Equip", "Put on your ice gloves while the bars smelt", "Then take them from the dispenser", ItemIds.ICE_GLOVES);
			}
			return Step.waiting("Wait at the bar dispenser", "The " + bar.barName(2) + " are smelting", Step.Target.DISPENSER);
		}

		return Step.of("Bank", "Open the bank", coalRunNext(s) ? "Next: a coal trip" : "Next: " + bar.oreName(), Step.Target.BANK);
	}

	private Step atBank(Snapshot s, boolean needsCoal, int bag)
	{
		Bar bar = s.bar;
		if (s.bars > 0)
		{
			return Step.items("Deposit", "Deposit your bars", s.bars + (s.bars == 1 ? " bar" : " bars"), barIds());
		}
		if (bar == Bar.GOLD && s.goldsmithCarried && !s.goldsmithWorn)
		{
			return Step.items("Equip", "Put on your goldsmith gauntlets", "Before the gold goes on the belt", ItemIds.GOLDSMITH_GAUNTLETS);
		}
		boolean coalRun = coalRunNext(s);
		if (needsCoal && s.coalBag && (s.bagCoal < 0 || bag < s.bagCapacity) && s.bankCoal != 0)
		{
			return Step.items("Fill", "Fill the coal bag", s.bagCoal < 0 ? "From the bank's coal" : bag + " of " + s.bagCapacity + " coal now",
				ItemIds.COAL_BAG, ItemIds.COAL_BAG_OPEN);
		}
		if (coalRun)
		{
			if (s.coal == 0 && s.free > 0)
			{
				int need = Math.max(1, bar.getCoal() * load(s) - s.furnaceCoal - (s.coalBag ? s.bagCapacity : 0));
				String detail = s.bankCoal == 0 ? "You are out of coal"
					: "A coal trip: the furnace needs " + need + " more before the next ore";
				return Step.items("Withdraw", "Withdraw coal", detail, Bar.COAL);
			}
		}
		else if (s.ore == 0 && s.free > 0)
		{
			if (s.bankOre == 0)
			{
				return Step.items("Withdraw", "Withdraw " + bar.oreName(), "You are out of " + bar.oreName(), bar.getOre());
			}
			String detail = needsCoal ? s.furnaceCoal + " coal waiting in the furnace" : load(s) + " ore makes " + load(s) + " bars";
			return Step.items("Withdraw", "Withdraw " + bar.oreName(), detail, bar.getOre());
		}
		return Step.of("Load", "Close the bank and load the belt", (s.ore + s.coal) + " to load", Step.Target.BELT);
	}

	private static int[] barIds()
	{
		Bar[] bars = Bar.values();
		int[] ids = new int[bars.length];
		for (int i = 0; i < bars.length; i++)
		{
			ids[i] = bars[i].getBar();
		}
		return ids;
	}
}
