package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Your dreams in the Nightmare Zone, kept per account: each dream's mode, the bosses seen in it,
 * its points (the game's own count of points this dream), its time, and the doses drunk of each
 * potion. From these the panel says your points an hour, after the points your potions cost,
 * for each set of bosses you have dreamt, and so the time to the reward you are after.
 */
public final class NmzLog
{
	/** Dreams kept. */
	static final int KEEP = 150;
	/** A dream longer than this is not timed (ticks: twelve hours). */
	public static final int LONGEST = 72_000;
	/** Points a dream can give, at most: a guard against damage. */
	static final int POINTS_MAX = 5_000_000;
	/** Bosses a dream keeps by name. */
	static final int BOSSES_MAX = 20;
	private static final String VERSION = "1";

	/** How a dream ended. */
	public enum End
	{
		/** Woke in the lobby: the dream ended in the arena (a death, or leaving it). */
		WOKE('w'),
		/** "Logging out while inside the arena ends the dream." */
		LOGOUT('l');

		final char code;

		End(char code)
		{
			this.code = code;
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

	/** One dream. */
	public static final class Dream
	{
		public final long endedAt;
		/** Ticks in the arena. */
		public final int ticks;
		public final int points;
		public final Nmz.Mode mode;
		/** 'H' hard, 'N' normal, '?' not known. */
		public final char difficulty;
		/** Coins paid, in thousands, or -1 when not seen. */
		public final int paid;
		public final End end;
		/** Boss names seen, sorted. */
		public final List<String> bosses;
		/** Doses drunk, by {@link Nmz.Potion} ordinal. */
		private final int[] doses;

		public Dream(long endedAt, int ticks, int points, Nmz.Mode mode, char difficulty, int paid, End end, List<String> bosses, int[] doses)
		{
			this.endedAt = Math.max(0, endedAt);
			this.ticks = ticks > 0 && ticks <= LONGEST ? ticks : 0;
			this.points = Math.max(0, Math.min(POINTS_MAX, points));
			this.mode = mode == null ? Nmz.Mode.UNKNOWN : mode;
			this.difficulty = difficulty == 'H' || difficulty == 'N' ? difficulty : '?';
			this.paid = paid >= 0 && paid <= 1000 ? paid : -1;
			this.end = end == null ? End.WOKE : end;
			TreeSet<String> names = new TreeSet<>();
			if (bosses != null)
			{
				for (String b : bosses)
				{
					String n = Nmz.cleanName(b);
					if (!n.isEmpty() && names.size() < BOSSES_MAX)
					{
						names.add(n);
					}
				}
			}
			this.bosses = Collections.unmodifiableList(new ArrayList<>(names));
			this.doses = new int[Nmz.Potion.values().length];
			if (doses != null)
			{
				for (int i = 0; i < this.doses.length && i < doses.length; i++)
				{
					this.doses[i] = Math.max(0, Math.min(9_999, doses[i]));
				}
			}
		}

		public int doses(Nmz.Potion p)
		{
			return doses[p.ordinal()];
		}

		/** Reward points its Nightmare Zone potions cost at the chest. */
		public long potionPoints()
		{
			long n = 0;
			for (Nmz.Potion p : Nmz.Potion.values())
			{
				n += (long) doses[p.ordinal()] * p.points();
			}
			return n;
		}

		/** "Hard customisable rumble", "Normal dream". */
		public String kind()
		{
			String d = difficulty == 'H' ? "Hard" : difficulty == 'N' ? "Normal" : "";
			String m = mode == Nmz.Mode.UNKNOWN ? (d.isEmpty() ? "Dream" : "dream") : mode.getTitle().toLowerCase(Locale.ROOT);
			return d.isEmpty() ? Character.toUpperCase(m.charAt(0)) + m.substring(1) : d + " " + m;
		}

		/** The set it is grouped under for points an hour: its kind and bosses. */
		public String set()
		{
			return kind() + "|" + String.join("|", bosses);
		}

		/** "6 overload · 22 absorption", or "". */
		public String potionsLine()
		{
			StringBuilder b = new StringBuilder();
			for (Nmz.Potion p : Nmz.Potion.values())
			{
				int n = doses[p.ordinal()];
				if (n > 0)
				{
					b.append(b.length() == 0 ? "" : " · ").append(n).append(' ').append(p.getTitle().toLowerCase(Locale.ROOT));
				}
			}
			return b.toString();
		}
	}

	/** Your pace with one set of bosses. */
	public static final class Pace
	{
		public final String set;
		public final String kind;
		public final List<String> bosses;
		public int dreams;
		public long points;
		public long ticks;
		public long potionPoints;

		Pace(Dream d)
		{
			this.set = d.set();
			this.kind = d.kind();
			this.bosses = d.bosses;
		}

		/** Points an hour over its timed dreams, or -1. */
		public double perHour()
		{
			return ticks <= 0 ? -1 : points * 3600.0 / (ticks * 0.6);
		}

		/** Points an hour after the points its Nightmare Zone potions cost, or -1. */
		public double netPerHour()
		{
			return ticks <= 0 ? -1 : (points - potionPoints) * 3600.0 / (ticks * 0.6);
		}
	}

	private final List<Dream> dreams = new ArrayList<>();

	public void add(Dream d)
	{
		if (d == null)
		{
			return;
		}
		dreams.add(d);
		while (dreams.size() > KEEP)
		{
			dreams.remove(0);
		}
	}

	public List<Dream> all()
	{
		return Collections.unmodifiableList(dreams);
	}

	public int count()
	{
		return dreams.size();
	}

	public Dream last()
	{
		return dreams.isEmpty() ? null : dreams.get(dreams.size() - 1);
	}

	/** The dream with the most points, or null. */
	public Dream best()
	{
		Dream best = null;
		for (Dream d : dreams)
		{
			if (d.points > 0 && (best == null || d.points > best.points))
			{
				best = d;
			}
		}
		return best;
	}

	/** Points an hour over your timed dreams (practice, which pays none, left out), or -1 with none. */
	public double pointsPerHour()
	{
		long t = 0;
		long p = 0;
		for (Dream d : dreams)
		{
			if (d.ticks > 0 && d.mode != Nmz.Mode.PRACTICE)
			{
				t += d.ticks;
				p += d.points;
			}
		}
		return t == 0 ? -1 : p * 3600.0 / (t * 0.6);
	}

	/** Points an hour after the points your Nightmare Zone potions cost, or -1 with none timed. */
	public double netPointsPerHour()
	{
		long t = 0;
		long p = 0;
		for (Dream d : dreams)
		{
			if (d.ticks > 0 && d.mode != Nmz.Mode.PRACTICE)
			{
				t += d.ticks;
				p += d.points - d.potionPoints();
			}
		}
		return t == 0 ? -1 : p * 3600.0 / (t * 0.6);
	}

	/** Doses of each potion drunk a dream on average over your last few dreams (to five), by ordinal. */
	public double[] habit()
	{
		double[] out = new double[Nmz.Potion.values().length];
		int n = 0;
		for (int i = dreams.size() - 1; i >= 0 && n < 5; i--)
		{
			Dream d = dreams.get(i);
			if (d.points <= 0)
			{
				continue;
			}
			n++;
			for (Nmz.Potion p : Nmz.Potion.values())
			{
				out[p.ordinal()] += d.doses(p);
			}
		}
		for (int i = 0; i < out.length && n > 0; i++)
		{
			out[i] /= n;
		}
		return out;
	}

	/** Your pace with each set of bosses dreamt, the best points an hour first. */
	public List<Pace> bySet()
	{
		Map<String, Pace> m = new LinkedHashMap<>();
		for (Dream d : dreams)
		{
			if (d.ticks <= 0 || d.mode == Nmz.Mode.PRACTICE)
			{
				continue;
			}
			Pace p = m.computeIfAbsent(d.set(), k -> new Pace(d));
			p.dreams++;
			p.points += d.points;
			p.ticks += d.ticks;
			p.potionPoints += d.potionPoints();
		}
		List<Pace> out = new ArrayList<>(m.values());
		out.sort((a, b) -> Double.compare(b.perHour(), a.perHour()));
		return out;
	}

	/** "1;endedAt,ticks,points,mode,diff,paid,end,boss|boss,O6+A22;..." */
	public String encode()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (Dream d : dreams)
		{
			b.append(';').append(d.endedAt).append(',').append(d.ticks).append(',').append(d.points).append(',').append(d.mode.code())
				.append(',').append(d.difficulty).append(',').append(d.paid).append(',').append(d.end.code).append(',')
				.append(String.join("|", d.bosses)).append(',');
			boolean first = true;
			for (Nmz.Potion p : Nmz.Potion.values())
			{
				int n = d.doses(p);
				if (n > 0)
				{
					b.append(first ? "" : "+").append(p.code()).append(n);
					first = false;
				}
			}
		}
		return b.toString();
	}

	/** Reads a save as far as it can; damaged dreams are left out. Never null. */
	public static NmzLog decode(String text)
	{
		NmzLog out = new NmzLog();
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
			if (f.length < 9)
			{
				continue;
			}
			try
			{
				long ended = Long.parseLong(f[0].trim());
				int ticks = Integer.parseInt(f[1].trim());
				int points = Integer.parseInt(f[2].trim());
				String mode = f[3].trim();
				String diff = f[4].trim();
				int paid = Integer.parseInt(f[5].trim());
				End end = End.byCode(f[6].trim());
				if (ended < 0 || ticks < 0 || points < 0 || mode.length() != 1 || diff.length() != 1 || end == null)
				{
					continue;
				}
				List<String> bosses = new ArrayList<>();
				for (String n : f[7].split("\\|"))
				{
					if (!n.trim().isEmpty())
					{
						bosses.add(n);
					}
				}
				int[] doses = new int[Nmz.Potion.values().length];
				boolean bad = false;
				for (String d : f[8].split("\\+"))
				{
					if (d.trim().isEmpty())
					{
						continue;
					}
					Nmz.Potion p = Nmz.Potion.byCode(d.trim().charAt(0));
					int n = Integer.parseInt(d.trim().substring(1));
					if (p == null || n < 0)
					{
						bad = true;
						break;
					}
					doses[p.ordinal()] = n;
				}
				if (bad)
				{
					continue;
				}
				out.add(new Dream(ended, ticks, points, Nmz.Mode.byCode(mode.charAt(0)), diff.charAt(0), paid, end, bosses, doses));
			}
			catch (IllegalArgumentException | IndexOutOfBoundsException e)
			{
				// Left out: a bad number.
			}
		}
		return out;
	}
}
