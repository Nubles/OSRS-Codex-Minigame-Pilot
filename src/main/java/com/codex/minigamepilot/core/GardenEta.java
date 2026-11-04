package com.codex.minigamepilot.core;

/**
 * How far a Sorceress's Garden target is, in sq'irks, runs and time at your own pace: a Thieving
 * level (the juice handed to Osman), a Farming level (the picking itself) or glasses of juice. What
 * you hold already (juice, and sq'irks enough for whole glasses) counts towards it.
 *
 * <p>Pure arithmetic over the wiki's figures and your own runs; it never guesses a pace.
 */
public final class GardenEta
{
	private GardenEta()
	{
	}

	/** What the target is. */
	public enum Kind
	{
		THIEVING_LEVEL,
		FARMING_LEVEL,
		JUICE
	}

	/** What you hold now. */
	public static final class Holding
	{
		/** Experience in the target's skill (Thieving or Farming), or 0 for juice. */
		public long xp;
		/** Glasses of juice held (pack and bank), by season. */
		public final int[] juice = new int[Garden.Season.values().length];
		/** Sq'irks held (pack and bank), by season. */
		public final int[] sqirks = new int[Garden.Season.values().length];
	}

	/** The answer. */
	public static final class Answer
	{
		/** How much of the target you have and need, in its unit (experience, or glasses). */
		public long have;
		public long need;
		/** Sq'irks still to pick in the chosen garden (Thieving or juice), or -1 for Farming. */
		public long sqirks = -1;
		/** Runs still to make at your rate, or -1 when your rate is not known. */
		public long runs = -1;
		/** Hours at your pace, or -1 when your pace is not known. */
		public double hours = -1;

		public boolean done()
		{
			return have >= need;
		}
	}

	/** Thieving experience your held juice and whole glasses' worth of sq'irks are worth from Osman. */
	public static long heldThievingXp(Holding h)
	{
		long xp = 0;
		for (Garden.Season s : Garden.Season.values())
		{
			xp += (long) h.juice[s.ordinal()] * s.osmanXp() + (long) (h.sqirks[s.ordinal()] / s.perJuice()) * s.osmanXp();
		}
		return xp;
	}

	/**
	 * @param target the level (Thieving or Farming) or the glasses of juice
	 * @param s the garden you run
	 * @param pace your figures in it
	 */
	public static Answer of(Kind kind, int target, Garden.Season s, Holding h, GardenLog.Pace pace)
	{
		Answer a = new Answer();
		switch (kind)
		{
			case THIEVING_LEVEL:
			{
				a.need = Goals.xpFor(Math.max(1, Math.min(99, target)));
				a.have = Math.min(a.need, h.xp + heldThievingXp(h));
				long left = a.need - a.have;
				// Osman pays by the whole glass; a part-glass of sq'irks already held helps fill the first.
				long glasses = left <= 0 ? 0 : (left + s.osmanXp() - 1) / s.osmanXp();
				a.sqirks = Math.max(0, glasses * s.perJuice() - (glasses > 0 ? h.sqirks[s.ordinal()] % s.perJuice() : 0));
				break;
			}
			case FARMING_LEVEL:
			{
				a.need = Goals.xpFor(Math.max(1, Math.min(99, target)));
				a.have = Math.min(a.need, h.xp);
				long left = a.need - a.have;
				double perRun = pace == null ? -1 : pace.farmingXpPerRun();
				double perHour = pace == null ? -1 : pace.farmingXpPerHour();
				a.runs = left <= 0 ? 0 : perRun > 0 ? (long) Math.ceil(left / perRun) : -1;
				a.hours = left <= 0 ? 0 : perHour > 0 ? left / perHour : -1;
				return a;
			}
			case JUICE:
			default:
			{
				a.need = Math.max(1, target);
				long held = h.juice[s.ordinal()];
				long sqirksHeld = h.sqirks[s.ordinal()];
				a.have = Math.min(a.need, held + sqirksHeld / s.perJuice());
				long fruit = a.need * s.perJuice() - held * s.perJuice() - sqirksHeld;
				a.sqirks = Math.max(0, fruit);
				break;
			}
		}
		double perRun = pace == null ? -1 : pace.sqirksPerRun();
		double perHour = pace == null ? -1 : pace.sqirksPerHour();
		a.runs = a.sqirks == 0 ? 0 : perRun > 0 ? (long) Math.ceil(a.sqirks / perRun) : -1;
		a.hours = a.sqirks == 0 ? 0 : perHour > 0 ? a.sqirks / perHour : -1;
		return a;
	}
}
