package com.codex.minigamepilot.core;

import java.util.Locale;

/**
 * The next step in the lobby, outside a dream: drink the dream potion once Dominic has filled
 * it; else take the potions you drink a dream from their barrels, or buy doses at the Rewards
 * chest; else top up Dominic's coffer for the dream you usually pay for; else ask Dominic for a
 * dream. Nothing here is said inside a dream.
 *
 * <p>Pure: the pilot fills a {@link Snapshot}.
 */
public final class NmzCycle
{
	private NmzCycle()
	{
	}

	/** What the lobby shows this tick. */
	public static final class Snapshot
	{
		/** The dream potion is filled and waiting on the plinth. */
		public boolean vialFull;
		/** Doses carried of each potion, by ordinal. */
		public int[] carried = new int[Nmz.Potion.values().length];
		/** Doses in each barrel by ordinal, or -1 when not known. */
		public int[] barrel = filled(-1);
		/** Doses drunk a dream on average lately, by ordinal. */
		public double[] habit = new double[Nmz.Potion.values().length];
		/** Reward points held, or -1. */
		public int rewardPoints = -1;
		/** The coffer in thousands, or -1. */
		public int coffer = -1;
		/** What your last paid dream cost, in thousands, or -1. */
		public int usualPrice = -1;
		/** "Hard customisable rumble", or null. */
		public String usualKind;
		/** Free inventory slots. */
		public int free = 28;

		private static int[] filled(int v)
		{
			int[] a = new int[Nmz.Potion.values().length];
			java.util.Arrays.fill(a, v);
			return a;
		}
	}

	/** The next step in the lobby. Never null. */
	public static Step next(Snapshot s)
	{
		if (s.vialFull)
		{
			return Step.of("Drink", "Drink the dream potion", "Dominic has filled it: drink when you are ready to begin", Step.Target.DREAM_VIAL);
		}
		for (Nmz.Potion p : Nmz.Potion.values())
		{
			if (!p.nightmare())
			{
				continue;
			}
			int want = (int) Math.ceil(s.habit[p.ordinal()]);
			int have = s.carried[p.ordinal()];
			if (want <= 0 || have >= want)
			{
				continue;
			}
			int stored = s.barrel[p.ordinal()];
			String name = p.getTitle().toLowerCase(Locale.ROOT);
			String used = "You drink about " + want + (want == 1 ? " dose" : " doses") + " a dream" + (have > 0 ? ", " + have + " carried" : "");
			if (stored > 0 && s.free > 0)
			{
				return Step.of("Take", "Take " + name + " from its barrel", used + " · " + stored + " stored", Step.Target.POTION_BARREL);
			}
			if (stored == 0 && have == 0 && s.rewardPoints >= p.points())
			{
				int afford = s.rewardPoints / p.points();
				return Step.of("Buy", "Buy " + name + " at the Rewards chest",
					String.format(Locale.ROOT, "%,d points a dose, %,d affordable · %s", p.points(), afford, used.toLowerCase(Locale.ROOT)), Step.Target.REWARDS_CHEST);
			}
		}
		if (s.coffer >= 0 && s.usualPrice > 0 && s.coffer < s.usualPrice)
		{
			return Step.of("Deposit", "Put coins in Dominic's coffer",
				String.format(Locale.ROOT, "%s costs %,d; it holds %,d", s.usualKind == null ? "Your last dream" : s.usualKind, s.usualPrice * 1000L, s.coffer * 1000L),
				Step.Target.DREAM_COFFER);
		}
		return Step.of("Dream", "Ask Dominic Onion for a dream",
			s.usualKind == null ? "Choose the dream, then drink the potion he fills" : "Last time: " + s.usualKind.toLowerCase(Locale.ROOT)
				+ (s.usualPrice > 0 ? String.format(Locale.ROOT, ", %,d coins", s.usualPrice * 1000L) : ""),
			Step.Target.DOMINIC);
	}
}
