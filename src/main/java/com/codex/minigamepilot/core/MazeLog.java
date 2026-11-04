package com.codex.minigamepilot.core;

import com.codex.minigamepilot.core.Maze.Leg;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Your runs through the Rogues' Den maze, kept per account: each one's time from the doorway to
 * the safe (or to where you were sent back), whether it reached the safe, the leg it ended on,
 * what the safe gave and the Agility and Thieving experience. From these the panel says your time
 * per run, where the maze sends you back, crates an hour and the time to the full rogue set.
 */
public final class MazeLog
{
	/** Runs kept. */
	static final int KEEP = 200;
	/** A run longer than this had a break in it and is not timed (ticks: fifteen minutes). */
	public static final int LONGEST = 1500;
	/** Experience a run can give, at most: a guard against damage. */
	static final int XP_MAX = 5000;
	private static final String VERSION = "1";

	/** What the safe gave. */
	public enum Reward
	{
		/** Nothing: sent back before the safe, or the crack failed. */
		NONE('-'),
		CRATE('C'),
		KIT('K');

		final char code;

		Reward(char code)
		{
			this.code = code;
		}

		static Reward of(String s)
		{
			for (Reward r : values())
			{
				if (s.length() == 1 && s.charAt(0) == r.code)
				{
					return r;
				}
			}
			throw new IllegalArgumentException(s);
		}
	}

	/** One run. */
	public static final class Run
	{
		public final long endedAt;
		/** Ticks from entering to leaving, or 0 when not timed. */
		public final int ticks;
		/** Reached the safe and cracked it (whatever it gave). */
		public final boolean finished;
		/** The leg it ended on: the safe when finished, else where you were sent back. Null when not known. */
		public final Leg leg;
		public final Reward reward;
		public final int agilityXp;
		public final int thievingXp;

		public Run(long endedAt, int ticks, boolean finished, Leg leg, Reward reward, int agilityXp, int thievingXp)
		{
			this.endedAt = Math.max(0, endedAt);
			this.ticks = ticks > 0 && ticks <= LONGEST ? ticks : 0;
			this.finished = finished;
			this.leg = finished ? Leg.SAFE : leg;
			this.reward = finished && reward != null ? reward : Reward.NONE;
			this.agilityXp = Math.max(0, Math.min(XP_MAX, agilityXp));
			this.thievingXp = Math.max(0, Math.min(XP_MAX, thievingXp));
		}
	}

	private final List<Run> runs = new ArrayList<>();

	public void add(Run r)
	{
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

	public int finished()
	{
		int n = 0;
		for (Run r : runs)
		{
			n += r.finished ? 1 : 0;
		}
		return n;
	}

	public int failed()
	{
		return runs.size() - finished();
	}

	public int rewards(Reward kind)
	{
		int n = 0;
		for (Run r : runs)
		{
			n += r.reward == kind ? 1 : 0;
		}
		return n;
	}

	/** Where runs ended short of the safe, by leg, in the maze's order. */
	public Map<Leg, Integer> sentBack()
	{
		Map<Leg, Integer> out = new EnumMap<>(Leg.class);
		for (Run r : runs)
		{
			if (!r.finished && r.leg != null)
			{
				out.merge(r.leg, 1, Integer::sum);
			}
		}
		return out;
	}

	/** The leg that sends you back most, or null with none. */
	public Leg worst()
	{
		Leg best = null;
		int most = 0;
		for (Map.Entry<Leg, Integer> e : sentBack().entrySet())
		{
			if (e.getValue() > most)
			{
				most = e.getValue();
				best = e.getKey();
			}
		}
		return best;
	}

	/** The quickest timed run to the safe, or null. */
	public Run quickest()
	{
		Run best = null;
		for (Run r : runs)
		{
			if (r.finished && r.ticks > 0 && (best == null || r.ticks < best.ticks))
			{
				best = r;
			}
		}
		return best;
	}

	/** Average ticks of your timed runs to the safe, or -1 with none. */
	public int typical()
	{
		long sum = 0;
		int n = 0;
		for (Run r : runs)
		{
			if (r.finished && r.ticks > 0)
			{
				sum += r.ticks;
				n++;
			}
		}
		return n == 0 ? -1 : (int) Math.round((double) sum / n);
	}

	private long timedTicks()
	{
		long t = 0;
		for (Run r : runs)
		{
			t += r.ticks;
		}
		return t;
	}

	/**
	 * Seconds of maze for each attempt at the safe, the runs that sent you back included, or -1
	 * before a timed run has reached the safe.
	 */
	public double secondsPerAttempt()
	{
		long t = timedTicks();
		int n = 0;
		for (Run r : runs)
		{
			n += r.finished && r.ticks > 0 ? 1 : 0;
		}
		return n == 0 ? -1 : t * 0.6 / n;
	}

	/** Runs to the safe an hour across timed runs, or -1 with none. */
	public double perHour()
	{
		double s = secondsPerAttempt();
		return s <= 0 ? -1 : 3600 / s;
	}

	public double agilityXpPerHour()
	{
		long t = timedTicks();
		long sum = 0;
		for (Run r : runs)
		{
			sum += r.ticks > 0 ? r.agilityXp : 0;
		}
		return t == 0 ? -1 : sum * 3600.0 / (t * 0.6);
	}

	/** Hours for this many attempts at the safe at your pace, or -1 without one. */
	public double hoursFor(int attempts)
	{
		if (attempts <= 0)
		{
			return 0;
		}
		double s = secondsPerAttempt();
		return s <= 0 ? -1 : attempts * s / 3600;
	}

	/** "1;endedAt,ticks,finished,leg,reward,agilityXp,thievingXp;..." with the leg by name, "-" when not known. */
	public String encode()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (Run r : runs)
		{
			b.append(';').append(r.endedAt).append(',').append(r.ticks).append(',').append(r.finished ? 1 : 0).append(',')
				.append(r.leg == null ? "-" : r.leg.name()).append(',').append(r.reward.code).append(',').append(r.agilityXp)
				.append(',').append(r.thievingXp);
		}
		return b.toString();
	}

	/** Reads a save as far as it can; damaged runs are left out. Never null. */
	public static MazeLog decode(String text)
	{
		MazeLog out = new MazeLog();
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
			String[] f = parts[i].split(",");
			if (f.length < 7)
			{
				continue;
			}
			try
			{
				long ended = Long.parseLong(f[0].trim());
				int ticks = Integer.parseInt(f[1].trim());
				String done = f[2].trim();
				String l = f[3].trim();
				Leg leg = "-".equals(l) ? null : Leg.valueOf(l);
				Reward reward = Reward.of(f[4].trim());
				int axp = Integer.parseInt(f[5].trim());
				int txp = Integer.parseInt(f[6].trim());
				if (ended >= 0 && ticks >= 0 && axp >= 0 && txp >= 0 && ("1".equals(done) || "0".equals(done)))
				{
					out.add(new Run(ended, ticks, "1".equals(done), leg, reward, axp, txp));
				}
			}
			catch (IllegalArgumentException e)
			{
				// Left out: a bad number, an unknown leg or reward.
			}
		}
		return out;
	}
}
