package com.codex.minigamepilot.core;

/**
 * How far a golem crafting target is, in golems, sunstone and time at your own pace: a Crafting level,
 * a number of golems, or the Jeweller's chisel (a 1/300 roll on every golem, so its odds and the time
 * to the average). Pure arithmetic over the wiki's figures and your own golems; it never guesses a pace.
 */
public final class GolemEta
{
	private GolemEta()
	{
	}

	/** What the target is. */
	public enum Kind
	{
		CRAFTING_LEVEL,
		GOLEMS,
		JEWELLERS_CHISEL
	}

	/** The answer. */
	public static final class Answer
	{
		/** How much of the target you have and need: experience, golems, or golems towards the average 300. */
		public long have;
		public long need;
		/** Golems still to craft. */
		public long golems;
		/** Sunstone still to mine for them (five a golem, its core one), after what you hold. */
		public long sunstone;
		/** Hours at your pace, or -1 when your pace is not known. */
		public double hours = -1;
		/** Crafting experience a golem the answer is reckoned at. */
		public double xpPerGolem;
		/** For the chisel: your chance of one by now, 0..1. */
		public double chance;

		public boolean done()
		{
			return have >= need;
		}
	}

	/**
	 * @param target the level, or the golems; ignored for the chisel
	 * @param xp your Crafting experience now
	 * @param kept golems you have crafted (for the chisel: since you began, or since your last chisel)
	 * @param owned you have a Jeweller's chisel
	 * @param fur the fur the golems are reckoned with until your own figures say otherwise
	 * @param sunstone sunstone held, cores counted as one each
	 * @param pace your figures, or null
	 */
	public static Answer of(Kind kind, int target, long xp, long kept, boolean owned, GolemCrafting.Fur fur,
		long sunstone, GolemLog.Pace pace)
	{
		Answer a = new Answer();
		double mine = pace == null ? -1 : pace.xpPerGolem();
		a.xpPerGolem = mine > 0 ? mine : (fur == null ? GolemCrafting.BASE_XP : fur.totalXp());
		double perHour = pace == null ? -1 : pace.golemsPerHour();
		switch (kind)
		{
			case CRAFTING_LEVEL:
				a.need = Goals.xpFor(Math.max(1, Math.min(99, target)));
				a.have = Math.min(a.need, Math.max(0, xp));
				a.golems = a.done() ? 0 : (long) Math.ceil((a.need - a.have) / a.xpPerGolem);
				break;
			case GOLEMS:
				a.need = Math.max(1, target);
				a.have = Math.min(a.need, Math.max(0, kept));
				a.golems = a.need - a.have;
				break;
			case JEWELLERS_CHISEL:
			default:
				a.need = GolemCrafting.CHISEL_ODDS;
				a.chance = GolemCrafting.chiselChance(kept);
				a.have = owned ? a.need : Math.min(a.need, Math.max(0, kept));
				a.golems = a.done() ? 0 : a.need - a.have;
				break;
		}
		a.sunstone = Math.max(0, a.golems * (GolemCrafting.SUNSTONE_PER_GOLEM + 1) - Math.max(0, sunstone));
		a.hours = a.golems == 0 ? 0 : perHour > 0 ? a.golems / perHour : -1;
		return a;
	}
}
