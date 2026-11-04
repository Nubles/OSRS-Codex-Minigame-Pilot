package com.codex.minigamepilot.core;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The moulds to set for a commission: for each part, the one you own with the best score for its
 * two words (the mould score is the sum of the three, OSRS wiki, Giants' Foundry). And which mould
 * to buy next: the one at your level that adds most to the best score you can set, averaged over
 * every commission Kovac can give, for each point of reputation it costs.
 *
 * <p>Plain Java, so the choice can be tested against the wiki's own figures: 69 is the most a
 * mould scores (Light Flat with the bought moulds), 46 the most with the default ones (Narrow
 * Heavy) and 24 the least (Broad Light with the defaults).
 */
public final class MouldPick
{
	private MouldPick()
	{
	}

	/** The best moulds for a commission among the ones owned. */
	public static final class Pick
	{
		private final Map<Mould.Part, Mould> parts;
		private final int score;

		Pick(Map<Mould.Part, Mould> parts, int score)
		{
			this.parts = parts;
			this.score = score;
		}

		public Mould get(Mould.Part part)
		{
			return parts.get(part);
		}

		public int getScore()
		{
			return score;
		}

		/** "Serrated Forte, Gladius Edge, Gladius Point". */
		public String names()
		{
			StringBuilder b = new StringBuilder();
			for (Mould.Part p : Mould.Part.values())
			{
				Mould m = parts.get(p);
				if (m != null)
				{
					if (b.length() > 0)
					{
						b.append(", ");
					}
					b.append(m.getTitle());
				}
			}
			return b.toString();
		}
	}

	/** A mould worth buying: what it adds, here and on average, and what it costs. */
	public static final class Buy
	{
		public final Mould mould;
		/** Mould score it adds to the commission asked about, maybe 0. */
		public final int here;
		/** Mould score it adds to the best you can set, averaged over every commission. */
		public final double average;

		Buy(Mould mould, int here, double average)
		{
			this.mould = mould;
			this.here = here;
			this.average = average;
		}
	}

	/** The default moulds, which every account owns. */
	public static Set<Mould> defaults()
	{
		Set<Mould> out = EnumSet.noneOf(Mould.class);
		for (Mould m : Mould.values())
		{
			if (m.isDefault())
			{
				out.add(m);
			}
		}
		return out;
	}

	/** The best moulds among {@code owned} (the defaults always count) for a commission. */
	public static Pick best(Mould.Style first, Mould.Style second, Collection<Mould> owned)
	{
		Set<Mould> have = defaults();
		if (owned != null)
		{
			have.addAll(owned);
		}
		Map<Mould.Part, Mould> parts = new EnumMap<>(Mould.Part.class);
		int total = 0;
		for (Mould.Part part : Mould.Part.values())
		{
			Mould best = null;
			for (Mould m : Mould.values())
			{
				if (m.getPart() != part || !have.contains(m))
				{
					continue;
				}
				// Ties go to the first listed, so a pick never flickers between two equals.
				if (best == null || m.score(first, second) > best.score(first, second))
				{
					best = m;
				}
			}
			parts.put(part, best);
			total += best == null ? 0 : best.score(first, second);
		}
		return new Pick(parts, total);
	}

	/** The best mould score the owned moulds give, averaged over every commission Kovac can give. */
	public static double average(Collection<Mould> owned)
	{
		int n = 0;
		int sum = 0;
		for (Mould.Style a : Mould.FIRST)
		{
			for (Mould.Style b : Mould.SECOND)
			{
				if (Mould.canCommission(a, b))
				{
					sum += best(a, b, owned).getScore();
					n++;
				}
			}
		}
		return n == 0 ? 0 : sum / (double) n;
	}

	/**
	 * The mould to buy next at this Smithing level: of those not owned, the one that adds most
	 * to the average best score for each point of reputation, or null when none adds anything.
	 *
	 * @param first the commission under way, or null; {@code here} is then 0
	 */
	public static Buy next(int level, Collection<Mould> owned, Mould.Style first, Mould.Style second)
	{
		Set<Mould> have = defaults();
		if (owned != null)
		{
			have.addAll(owned);
		}
		double base = average(have);
		int baseHere = first == null ? 0 : best(first, second, have).getScore();
		Buy best = null;
		double bestRate = 0;
		for (Mould m : Mould.values())
		{
			if (have.contains(m) || m.getLevel() > level)
			{
				continue;
			}
			Set<Mould> with = EnumSet.copyOf(have);
			with.add(m);
			double gain = average(with) - base;
			if (gain <= 1e-9)
			{
				continue;
			}
			double rate = gain / m.getRep();
			if (best == null || rate > bestRate + 1e-12)
			{
				int here = first == null ? 0 : best(first, second, with).getScore() - baseHere;
				best = new Buy(m, here, gain);
				bestRate = rate;
			}
		}
		return best;
	}

	/** The best mould you could buy at this level for this commission's part, if it beats what you own. */
	public static Mould upgrade(Mould.Part part, int level, Collection<Mould> owned, Mould.Style first, Mould.Style second)
	{
		Pick have = best(first, second, owned);
		Mould current = have.get(part);
		int now = current == null ? 0 : current.score(first, second);
		Mould best = null;
		for (Mould m : Mould.values())
		{
			if (m.getPart() != part || m.getLevel() > level || m.isDefault() || (owned != null && owned.contains(m)))
			{
				continue;
			}
			if (m.score(first, second) > now && (best == null || m.score(first, second) > best.score(first, second)))
			{
				best = m;
			}
		}
		return best;
	}
}
