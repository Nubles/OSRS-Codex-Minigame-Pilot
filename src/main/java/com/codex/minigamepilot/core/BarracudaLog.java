package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Your Barracuda Trial runs, kept per account: each run's trial and rank, its time, the Sailing
 * experience it gave, how it ended, and its splits: the tick each objective was done and where
 * your skiff was then. Your quickest finished run of each course (a trial at a rank: "Only skiffs
 * may be used in these trials", so the boat never differs) is its ghost, and from the ghost's
 * splits the next checkpoint is learnt; no route is given by anyone else.
 */
public final class BarracudaLog
{
	/** Runs kept; a course's ghost is never the one let go. */
	static final int KEEP = 150;
	/** A run longer than this is not timed (ticks: twenty minutes; the longest target is 6:09). */
	public static final int LONGEST = 2_000;
	/** Experience a run can give, at most: a guard against damage. */
	public static final int XP_MAX = 200_000;
	/** Splits a run keeps, at most. */
	public static final int SPLITS_MAX = 120;
	/** Two runs ending this far apart or nearer are one sitting, so the time between them counts (ms: ten minutes). */
	public static final long SITTING_GAP = 10 * 60_000L;
	private static final String VERSION = "1";

	/** How a run ended. */
	public enum End
	{
		/** Completed: the trial's own completion was seen. */
		FINISHED('f', "Finished"),
		/** Reset, or sailed away from: no completion. */
		ABANDONED('a', "Abandoned"),
		/** Logged out or hopped in the run. */
		LOGOUT('o', "Logged out");

		final char code;
		private final String title;

		End(char code, String title)
		{
			this.code = code;
			this.title = title;
		}

		public String getTitle()
		{
			return title;
		}

		static End byCode(String s)
		{
			for (End e : values())
			{
				if (s.length() == 1 && s.charAt(0) == e.code)
				{
					return e;
				}
			}
			return null;
		}
	}

	/** One split: the tick into the run an objective was done, and where the skiff was (0, 0 when not read). */
	public static final class Split
	{
		public final int tick;
		public final int x;
		public final int y;

		public Split(int tick, int x, int y)
		{
			this.tick = Math.max(0, Math.min(LONGEST, tick));
			this.x = x >= 0 && x < 16_384 ? x : 0;
			this.y = y >= 0 && y < 16_384 ? y : 0;
		}

		public boolean placed()
		{
			return x > 0 && y > 0;
		}
	}

	/** One run. */
	public static final class Run
	{
		public final long endedAt;
		public final Barracuda.Trial trial;
		/** The rank run for, or null when it was not read. */
		public final Barracuda.Rank rank;
		public final int ticks;
		public final int xp;
		public final End end;
		private final List<Split> splits;

		public Run(long endedAt, Barracuda.Trial trial, Barracuda.Rank rank, int ticks, int xp, End end, List<Split> splits)
		{
			this.endedAt = Math.max(0, endedAt);
			this.trial = trial;
			this.rank = rank;
			this.ticks = ticks > 0 && ticks <= LONGEST ? ticks : 0;
			this.xp = Math.max(0, Math.min(XP_MAX, xp));
			this.end = end == null ? End.ABANDONED : end;
			List<Split> s = new ArrayList<>();
			if (splits != null)
			{
				int last = 0;
				for (Split x : splits)
				{
					// Splits only go forward; one out of order is damage.
					if (x != null && x.tick >= last && s.size() < SPLITS_MAX && (this.ticks == 0 || x.tick <= this.ticks))
					{
						s.add(x);
						last = x.tick;
					}
				}
			}
			this.splits = Collections.unmodifiableList(s);
		}

		public List<Split> splits()
		{
			return splits;
		}

		public boolean finished()
		{
			return end == End.FINISHED && ticks > 0;
		}

		/** Finished inside its rank's target time. */
		public boolean beat()
		{
			return finished() && rank != null && ticks <= trial.targetTicks(rank);
		}

		/** "Shark", or "Rank unread". */
		public String course()
		{
			return rank == null ? "Rank unread" : rank.getTitle();
		}
	}

	private final List<Run> runs = new ArrayList<>();

	public void add(Run r)
	{
		if (r == null || r.trial == null)
		{
			return;
		}
		runs.add(r);
		trim();
	}

	private void trim()
	{
		int i = 0;
		while (runs.size() > KEEP && i < runs.size())
		{
			Run r = runs.get(i);
			if (r.rank != null && ghost(r.trial, r.rank) == r)
			{
				i++;
				continue;
			}
			runs.remove(i);
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

	/** Your quickest finished run of a course, or null: its ghost. */
	public Run ghost(Barracuda.Trial trial, Barracuda.Rank rank)
	{
		Run best = null;
		for (Run r : runs)
		{
			if (r.trial == trial && r.rank == rank && rank != null && r.finished() && (best == null || r.ticks < best.ticks))
			{
				best = r;
			}
		}
		return best;
	}

	/** Runs of a trial, finished or not. */
	public int runs(Barracuda.Trial trial)
	{
		int n = 0;
		for (Run r : runs)
		{
			n += r.trial == trial ? 1 : 0;
		}
		return n;
	}

	/** Ranks seen beaten in your own runs. */
	public boolean beaten(Barracuda.Trial trial, Barracuda.Rank rank)
	{
		for (Run r : runs)
		{
			if (r.trial == trial && r.rank == rank && r.beat())
			{
				return true;
			}
		}
		return false;
	}

	/** Sailing experience an hour in a trial's runs (null for all), over your timed runs; -1 with none. */
	public double xpPerHour(Barracuda.Trial trial)
	{
		long t = 0;
		long x = 0;
		for (Run r : runs)
		{
			if (r.ticks > 0 && (trial == null || r.trial == trial))
			{
				t += r.ticks;
				x += r.xp;
			}
		}
		return t == 0 ? -1 : x * 3600.0 / (t * 0.6);
	}

	/**
	 * Sailing experience an hour with the time between runs counted: over each run of the trial
	 * that followed another of it within a sitting, its experience over the time since the last
	 * one ended. -1 with fewer than two such runs.
	 */
	public double xpPerHourSitting(Barracuda.Trial trial)
	{
		long ms = 0;
		long x = 0;
		int n = 0;
		Run prev = null;
		for (Run r : runs)
		{
			if (trial != null && r.trial != trial)
			{
				continue;
			}
			if (prev != null)
			{
				long gap = r.endedAt - prev.endedAt;
				if (gap > 0 && gap <= SITTING_GAP && r.ticks > 0)
				{
					ms += gap;
					x += r.xp;
					n++;
				}
			}
			prev = r;
		}
		return n < 2 || ms <= 0 ? -1 : x * 3_600_000.0 / ms;
	}

	/** Your quickest finished time at each rank of a trial (ticks, 0 for none). */
	public Map<Barracuda.Rank, Integer> bests(Barracuda.Trial trial)
	{
		Map<Barracuda.Rank, Integer> out = new EnumMap<>(Barracuda.Rank.class);
		for (Barracuda.Rank k : Barracuda.Rank.values())
		{
			Run g = ghost(trial, k);
			out.put(k, g == null ? 0 : g.ticks);
		}
		return out;
	}

	/** "1;endedAt,trial,rank,ticks,xp,end,tick.x.y/tick.x.y;..." with the rank "-" when unread. */
	public String encode()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (Run r : runs)
		{
			b.append(';').append(r.endedAt).append(',').append(r.trial.code).append(',').append(r.rank == null ? '-' : r.rank.code)
				.append(',').append(r.ticks).append(',').append(r.xp).append(',').append(r.end.code).append(',');
			for (int i = 0; i < r.splits.size(); i++)
			{
				Split s = r.splits.get(i);
				b.append(i == 0 ? "" : "/").append(s.tick).append('.').append(s.x).append('.').append(s.y);
			}
		}
		return b.toString();
	}

	/** Reads a save as far as it can; damaged runs are left out. Never null. */
	public static BarracudaLog decode(String text)
	{
		BarracudaLog out = new BarracudaLog();
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
			if (f.length < 7)
			{
				continue;
			}
			try
			{
				long ended = Long.parseLong(f[0].trim());
				String tc = f[1].trim();
				String rc = f[2].trim();
				Barracuda.Trial trial = tc.length() == 1 ? Barracuda.Trial.byCode(tc.charAt(0)) : null;
				Barracuda.Rank rank = rc.length() == 1 ? Barracuda.Rank.byCode(rc.charAt(0)) : null;
				int ticks = Integer.parseInt(f[3].trim());
				int xp = Integer.parseInt(f[4].trim());
				End end = End.byCode(f[5].trim());
				if (ended < 0 || trial == null || (rank == null && !"-".equals(rc)) || ticks < 0 || xp < 0 || end == null)
				{
					continue;
				}
				List<Split> splits = new ArrayList<>();
				String s = f[6].trim();
				if (!s.isEmpty())
				{
					for (String one : s.split("/"))
					{
						String[] p = one.split("\\.", -1);
						if (p.length != 3)
						{
							continue;
						}
						int t = Integer.parseInt(p[0].trim());
						int x = Integer.parseInt(p[1].trim());
						int y = Integer.parseInt(p[2].trim());
						if (t >= 0 && x >= 0 && y >= 0)
						{
							splits.add(new Split(t, x, y));
						}
					}
				}
				out.add(new Run(ended, trial, rank, ticks, xp, end, splits));
			}
			catch (IllegalArgumentException e)
			{
				// Left out: a bad number.
			}
		}
		return out;
	}
}
