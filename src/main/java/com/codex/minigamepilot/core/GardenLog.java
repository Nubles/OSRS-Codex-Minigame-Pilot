package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Your Sorceress's Garden runs, kept per account: each run's garden, whether it paid (a sq'irk,
 * herbs, or caught by an elemental) and its lap, from one trip to the fountain to the next. From
 * these come your success rate and your sq'irks an hour at your own pace. Also kept: the glasses
 * of juice you have crushed, by season.
 */
public final class GardenLog
{
	/** Runs kept. */
	static final int KEEP = 1_000;
	/** Pace is judged over this many of a garden's latest runs, so it follows how you run now. */
	public static final int RECENT = 100;
	/** A lap longer than this is not timed (ticks: five minutes; a summer lap is about one). */
	public static final int LONGEST = 500;
	/** Juice a season can have made, at most: a guard against damage. */
	public static final int JUICE_MAX = 10_000_000;
	private static final String VERSION = "1";

	/** How a run ended. */
	public enum Outcome
	{
		/** A sq'irk picked. */
		SQIRK('q', "Sq'irk"),
		/** Two herbs picked instead. */
		HERBS('h', "Herbs"),
		/** Seen by an elemental and sent back to the fountain. */
		CAUGHT('c', "Caught");

		final char code;
		private final String title;

		Outcome(char code, String title)
		{
			this.code = code;
			this.title = title;
		}

		public String getTitle()
		{
			return title;
		}

		static Outcome byCode(String s)
		{
			for (Outcome o : values())
			{
				if (s.length() == 1 && s.charAt(0) == o.code)
				{
					return o;
				}
			}
			return null;
		}

		/** The run paid something: a sq'irk or herbs. */
		public boolean paid()
		{
			return this != CAUGHT;
		}
	}

	/** One run. */
	public static final class Run
	{
		public final long endedAt;
		/** Ticks from the last trip to the fountain (or arriving) to this one, or 0 when not timed. */
		public final int ticks;
		public final Garden.Season season;
		public final Outcome outcome;

		public Run(long endedAt, int ticks, Garden.Season season, Outcome outcome)
		{
			this.endedAt = Math.max(0, endedAt);
			this.ticks = ticks > 0 && ticks <= LONGEST ? ticks : 0;
			this.season = season == null ? Garden.Season.SUMMER : season;
			this.outcome = outcome == null ? Outcome.CAUGHT : outcome;
		}

		/** Farming experience the run gave. */
		public int farmingXp()
		{
			return outcome == Outcome.SQIRK ? season.farmingXp() : outcome == Outcome.HERBS ? Garden.HERB_FARMING_XP : 0;
		}
	}

	/** One garden's figures over its latest runs. */
	public static final class Pace
	{
		public final Garden.Season season;
		public int runs;
		public int sqirks;
		public int herbs;
		public int caught;
		/** Over the timed runs only. */
		public int timedRuns;
		public int timedSqirks;
		public long timedTicks;
		public long timedFarmingXp;

		Pace(Garden.Season season)
		{
			this.season = season;
		}

		/** Runs that paid (a sq'irk or herbs), of all, 0..1; -1 with none. */
		public double successRate()
		{
			return runs == 0 ? -1 : (sqirks + herbs) / (double) runs;
		}

		/** Sq'irks a run, 0..1 (one fruit a trip); -1 with none. */
		public double sqirksPerRun()
		{
			return runs == 0 ? -1 : sqirks / (double) runs;
		}

		private double hours()
		{
			return timedTicks * 0.6 / 3600.0;
		}

		/** -1 when no run is timed. */
		public double runsPerHour()
		{
			return timedTicks <= 0 ? -1 : timedRuns / hours();
		}

		public double sqirksPerHour()
		{
			return timedTicks <= 0 ? -1 : timedSqirks / hours();
		}

		/** Thieving experience an hour once the sq'irks are crushed and handed to Osman. */
		public double thievingXpPerHour()
		{
			double s = sqirksPerHour();
			return s < 0 ? -1 : s * season.xpPerSqirk();
		}

		public double farmingXpPerHour()
		{
			return timedTicks <= 0 ? -1 : timedFarmingXp / hours();
		}

		/** Average lap in ticks, or -1. */
		public double lap()
		{
			return timedRuns == 0 ? -1 : timedTicks / (double) timedRuns;
		}

		/** Farming experience a run, on average, or -1. */
		public double farmingXpPerRun()
		{
			return runs == 0 ? -1 : (sqirks * (double) season.farmingXp() + herbs * (double) Garden.HERB_FARMING_XP) / runs;
		}
	}

	private final List<Run> runs = new ArrayList<>();
	private final long[] juice = new long[Garden.Season.values().length];

	public void add(Run r)
	{
		if (r == null)
		{
			return;
		}
		runs.add(r);
		while (runs.size() > KEEP)
		{
			runs.remove(0);
		}
	}

	public List<Run> all()
	{
		return Collections.unmodifiableList(runs);
	}

	public int count()
	{
		return runs.size();
	}

	public Run last()
	{
		return runs.isEmpty() ? null : runs.get(runs.size() - 1);
	}

	/** Glasses of juice crushed in this season, ever (since kept). */
	public long juice(Garden.Season s)
	{
		return juice[s.ordinal()];
	}

	public void addJuice(Garden.Season s, int n)
	{
		if (s != null && n > 0)
		{
			juice[s.ordinal()] = Math.min(JUICE_MAX, juice[s.ordinal()] + n);
		}
	}

	/** Every season's juice, as Thieving experience from Osman. */
	public long juiceXp()
	{
		long xp = 0;
		for (Garden.Season s : Garden.Season.values())
		{
			xp += juice(s) * s.osmanXp();
		}
		return xp;
	}

	/** Farming experience your kept runs gave. */
	public long farmingXp()
	{
		long xp = 0;
		for (Run r : runs)
		{
			xp += r.farmingXp();
		}
		return xp;
	}

	/** A garden's figures over its latest {@link #RECENT} runs. */
	public Pace pace(Garden.Season s)
	{
		Pace p = new Pace(s);
		for (int i = runs.size() - 1; i >= 0 && p.runs < RECENT; i--)
		{
			Run r = runs.get(i);
			if (r.season != s)
			{
				continue;
			}
			p.runs++;
			p.sqirks += r.outcome == Outcome.SQIRK ? 1 : 0;
			p.herbs += r.outcome == Outcome.HERBS ? 1 : 0;
			p.caught += r.outcome == Outcome.CAUGHT ? 1 : 0;
			if (r.ticks > 0)
			{
				p.timedRuns++;
				p.timedTicks += r.ticks;
				p.timedSqirks += r.outcome == Outcome.SQIRK ? 1 : 0;
				p.timedFarmingXp += r.farmingXp();
			}
		}
		return p;
	}

	/** The garden of your latest run, or null. */
	public Garden.Season lastSeason()
	{
		Run r = last();
		return r == null ? null : r.season;
	}

	/** The shortest lap of a garden that paid a sq'irk, or -1. */
	public int bestLap(Garden.Season s)
	{
		int best = -1;
		for (Run r : runs)
		{
			if (r.season == s && r.outcome == Outcome.SQIRK && r.ticks > 0 && (best < 0 || r.ticks < best))
			{
				best = r.ticks;
			}
		}
		return best;
	}

	/** "1;endedAt,ticks,season,outcome;..." */
	public String encode()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (Run r : runs)
		{
			b.append(';').append(r.endedAt).append(',').append(r.ticks).append(',').append(r.season.code).append(',').append(r.outcome.code);
		}
		return b.toString();
	}

	/** "1;winter,spring,autumn,summer" glasses crushed. */
	public String encodeJuice()
	{
		StringBuilder b = new StringBuilder(VERSION).append(';');
		for (int i = 0; i < juice.length; i++)
		{
			b.append(i == 0 ? "" : ",").append(juice[i]);
		}
		return b.toString();
	}

	/** Reads both saves as far as it can; damaged runs are left out. Never null. */
	public static GardenLog decode(String text, String juiceText)
	{
		GardenLog out = new GardenLog();
		if (juiceText != null)
		{
			String[] p = juiceText.split(";", -1);
			if (p.length == 2 && VERSION.equals(p[0].trim()))
			{
				String[] f = p[1].split(",", -1);
				for (int i = 0; i < f.length && i < out.juice.length; i++)
				{
					try
					{
						long n = Long.parseLong(f[i].trim());
						out.juice[i] = n < 0 ? 0 : Math.min(JUICE_MAX, n);
					}
					catch (NumberFormatException e)
					{
						// Left at 0: a bad number.
					}
				}
			}
		}
		if (text == null)
		{
			return out;
		}
		String[] parts = text.split(";");
		if (parts.length == 0 || !VERSION.equals(parts[0].trim()))
		{
			return out;
		}
		for (int i = 1; i < parts.length; i++)
		{
			String[] f = parts[i].split(",", -1);
			if (f.length < 4)
			{
				continue;
			}
			try
			{
				long ended = Long.parseLong(f[0].trim());
				int ticks = Integer.parseInt(f[1].trim());
				String s = f[2].trim();
				Garden.Season season = s.length() == 1 ? Garden.Season.byCode(s.charAt(0)) : null;
				Outcome o = Outcome.byCode(f[3].trim());
				if (ended < 0 || ticks < 0 || season == null || o == null)
				{
					continue;
				}
				out.add(new Run(ended, ticks, season, o));
			}
			catch (IllegalArgumentException e)
			{
				// Left out: a bad number.
			}
		}
		return out;
	}
}
