package com.codex.minigamepilot.core;

import java.util.Locale;

/**
 * The next step at the Volcanic Mine's entrance, outside a game: Mining to 50 if you lack it
 * ("To enter the mine, players must have level 50 Mining"); else the reward you are saving for,
 * once you hold its points (ore packs, bought again and again, are left to the reward card); else numulite for the fee ("players must pay Petrified Pete 30
 * numulite each time to enter the mine") unless you have been seen to enter without paying; else
 * pay Petrified Pete and go in. Nothing here is said inside the mine.
 *
 * <p>Pure: the pilot fills a {@link Snapshot}.
 */
public final class VolcanicCycle
{
	private VolcanicCycle()
	{
	}

	/** What the entrance shows this tick. */
	public static final class Snapshot
	{
		/** Real Mining level. */
		public int mining = 1;
		/** Numulite carried. */
		public int numulite;
		/** You have been seen to enter without paying: permanent access. */
		public boolean permanent;
		/** Points held, or -1 when not known. */
		public long points = -1;
		/** The reward saved for, or null. */
		public Volcanic.Reward goal;
		/** Its price now (what you still lack of the kit), or 0 when you own it. */
		public int goalCost;
		/** For the kit, the next piece to buy, or null. */
		public Volcanic.Piece nextPiece;
	}

	/** The next step at the entrance. Never null. */
	public static Step next(Snapshot s)
	{
		if (s.mining < Volcanic.MINING_LEVEL)
		{
			return Step.of("Train", "Train Mining to " + Volcanic.MINING_LEVEL,
				"The mine lets you in at " + Volcanic.MINING_LEVEL + " Mining; you have " + s.mining, Step.Target.NONE);
		}
		if (s.goal != null && !s.goal.repeat() && s.goalCost > 0 && s.points >= 0)
		{
			int price = s.nextPiece != null ? s.nextPiece.cost() : s.goal.cost();
			String what = s.nextPiece != null ? s.nextPiece.getTitle() : s.goal.getTitle();
			if (s.points >= price)
			{
				return Step.of("Buy", "Buy the " + what.toLowerCase(Locale.ROOT),
					String.format(Locale.ROOT, "Pete's shop: %,d of your %,d points", price, s.points), Step.Target.PETRIFIED_PETE);
			}
		}
		if (!s.permanent && s.numulite < Volcanic.ENTRY_FEE)
		{
			return Step.of("Bring", "Bring " + Volcanic.ENTRY_FEE + " numulite",
				"Petrified Pete takes " + Volcanic.ENTRY_FEE + " a game, or " + String.format(Locale.ROOT, "%,d", Volcanic.PERMANENT_FEE) + " once for good"
					+ (s.numulite > 0 ? "; you carry " + s.numulite : ""), Step.Target.VOLCANO_BANK);
		}
		return Step.of("Enter", s.permanent ? "Enter the Volcanic Mine" : "Pay Petrified Pete and go in",
			s.permanent ? "Your access is paid for good" : Volcanic.ENTRY_FEE + " numulite a game; you carry " + s.numulite, Step.Target.PETRIFIED_PETE);
	}
}
