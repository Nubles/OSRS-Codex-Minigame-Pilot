package com.codex.minigamepilot.core;

import com.codex.minigamepilot.core.Assault.Role;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Your Barbarian Assault games, kept per account: each wave's number, the role you played and its
 * time, whether it was won, and the honour points the game gave in each role, as the game's own
 * point varbits rose. From these the panel says your time per wave and per game, the points each
 * role gives you a game, and so the games and time to a reward at your own pace.
 */
public final class AssaultLog
{
	/** Games kept. */
	static final int KEEP = 100;
	/** A wave longer than this had a break in it and is not timed (ticks: twenty minutes). */
	public static final int LONGEST_WAVE = 2000;
	/** A game longer than this is not timed (ticks: two hours). */
	public static final int LONGEST_GAME = 12_000;
	/** Points a game can give a role, at most: a guard against damage (the cap is 5,000). */
	static final int POINTS_MAX = Assault.CAP;
	private static final String VERSION = "1";
	private static final Pattern WAVE = Pattern.compile("(\\d{1,2})([ACDH-])(\\d{1,5})([dx])");

	/** One wave of a game. */
	public static final class Wave
	{
		public final int number;
		/** The role you played, or null when not seen. */
		public final Role role;
		/** Ticks from its start to its end, or 0 when not timed. */
		public final int ticks;
		/** Won: the wave-complete interface came, or points for it. */
		public final boolean done;

		public Wave(int number, Role role, int ticks, boolean done)
		{
			this.number = Math.max(1, Math.min(Assault.WAVES, number));
			this.role = role;
			this.ticks = ticks > 0 && ticks <= LONGEST_WAVE ? ticks : 0;
			this.done = done;
		}
	}

	/** One game: the waves played in a row, from the first seen to the queen or to leaving. */
	public static final class Game
	{
		public final long endedAt;
		/** Ticks from the first wave's start to the last wave's end, lobby between waves too, or 0 when not timed. */
		public final int ticks;
		public final List<Wave> waves;
		/** Honour points the game gave, by role. */
		private final int[] points;

		public Game(long endedAt, int ticks, List<Wave> waves, int[] points)
		{
			this.endedAt = Math.max(0, endedAt);
			this.ticks = ticks > 0 && ticks <= LONGEST_GAME ? ticks : 0;
			this.waves = Collections.unmodifiableList(new ArrayList<>(waves));
			this.points = new int[Role.values().length];
			for (int i = 0; i < this.points.length && points != null && i < points.length; i++)
			{
				this.points[i] = Math.max(0, Math.min(POINTS_MAX, points[i]));
			}
		}

		public int points(Role r)
		{
			return points[r.ordinal()];
		}

		public int totalPoints()
		{
			int n = 0;
			for (int p : points)
			{
				n += p;
			}
			return n;
		}

		/** The first wave played. */
		public int first()
		{
			return waves.isEmpty() ? 0 : waves.get(0).number;
		}

		/** The highest wave won, or 0. */
		public int reached()
		{
			int n = 0;
			for (Wave w : waves)
			{
				if (w.done)
				{
					n = Math.max(n, w.number);
				}
			}
			return n;
		}

		/** The Penance Queen was killed: wave 10 won. */
		public boolean queen()
		{
			return reached() >= Assault.WAVES;
		}

		/** Every wave from 1 to the queen, in this one game. */
		public boolean full()
		{
			return queen() && first() == 1;
		}

		/** The role played most, the later on a tie, or null when none was seen. */
		public Role role()
		{
			int[] n = new int[Role.values().length];
			Role best = null;
			for (Wave w : waves)
			{
				if (w.role != null)
				{
					n[w.role.ordinal()]++;
					if (best == null || n[w.role.ordinal()] >= n[best.ordinal()])
					{
						best = w.role;
					}
				}
			}
			return best;
		}

		/** Your time for a wave number in this game, or 0. */
		public int waveTicks(int number)
		{
			for (Wave w : waves)
			{
				if (w.number == number && w.done && w.ticks > 0)
				{
					return w.ticks;
				}
			}
			return 0;
		}
	}

	private final List<Game> games = new ArrayList<>();

	public void add(Game g)
	{
		if (g == null || g.waves.isEmpty())
		{
			return;
		}
		games.add(g);
		while (games.size() > KEEP)
		{
			games.remove(0);
		}
	}

	public List<Game> all()
	{
		return Collections.unmodifiableList(games);
	}

	public int count()
	{
		return games.size();
	}

	public Game last()
	{
		return games.isEmpty() ? null : games.get(games.size() - 1);
	}

	/** Games that killed the Penance Queen. */
	public int queens()
	{
		int n = 0;
		for (Game g : games)
		{
			n += g.queen() ? 1 : 0;
		}
		return n;
	}

	/** The quickest timed full game, or null. */
	public Game quickest()
	{
		Game best = null;
		for (Game g : games)
		{
			if (g.full() && g.ticks > 0 && (best == null || g.ticks < best.ticks))
			{
				best = g;
			}
		}
		return best;
	}

	/** Your quickest won wave of this number, in any role, or 0. */
	public int quickestWave(int number)
	{
		int best = 0;
		for (Game g : games)
		{
			int t = g.waveTicks(number);
			if (t > 0 && (best == 0 || t < best))
			{
				best = t;
			}
		}
		return best;
	}

	/**
	 * What a full game as each role gives you, from your own full games: points in the role, in
	 * each other role, and ticks. Roles you have not finished a game in are left out.
	 */
	public Map<Role, Rate> rates()
	{
		Map<Role, long[]> sums = new EnumMap<>(Role.class);
		for (Game g : games)
		{
			Role r = g.role();
			if (!g.full() || r == null)
			{
				continue;
			}
			long[] s = sums.computeIfAbsent(r, k -> new long[5]);
			s[0]++;
			s[1] += g.points(r);
			s[2] += g.totalPoints() - g.points(r);
			if (g.ticks > 0)
			{
				s[3] += g.ticks;
				s[4]++;
			}
		}
		Map<Role, Rate> out = new EnumMap<>(Role.class);
		for (Map.Entry<Role, long[]> e : sums.entrySet())
		{
			long[] s = e.getValue();
			out.put(e.getKey(), new Rate((int) s[0], s[1] / (double) s[0], s[2] / (3.0 * s[0]), s[4] == 0 ? 0 : (int) Math.round(s[3] / (double) s[4])));
		}
		return out;
	}

	/** Average ticks of your timed full games, in any role, or 0. */
	public int typicalGame()
	{
		long t = 0;
		int n = 0;
		for (Game g : games)
		{
			if (g.full() && g.ticks > 0)
			{
				t += g.ticks;
				n++;
			}
		}
		return n == 0 ? 0 : (int) Math.round(t / (double) n);
	}

	/** Honour points an hour, all roles, across your timed games, or -1 with none. */
	public double pointsPerHour()
	{
		long t = 0;
		long p = 0;
		for (Game g : games)
		{
			if (g.ticks > 0)
			{
				t += g.ticks;
				p += g.totalPoints();
			}
		}
		return t == 0 ? -1 : p * 3600.0 / (t * 0.6);
	}

	/** What a full game in one role gives you, as your own games have. */
	public static final class Rate
	{
		public final int games;
		/** Points in the role played. */
		public final double own;
		/** Points in each of the other three. */
		public final double side;
		/** Ticks a game, or 0 when none was timed. */
		public final int ticks;

		public Rate(int games, double own, double side, int ticks)
		{
			this.games = games;
			this.own = own;
			this.side = side;
			this.ticks = ticks;
		}
	}

	/** "1;endedAt,ticks,pA,pC,pD,pH,waves" with waves as "1A93d/2A120d/3C80x": number, role (- unseen), ticks, won (d) or not (x). */
	public String encode()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (Game g : games)
		{
			b.append(';').append(g.endedAt).append(',').append(g.ticks);
			for (Role r : Role.values())
			{
				b.append(',').append(g.points(r));
			}
			b.append(',');
			for (int i = 0; i < g.waves.size(); i++)
			{
				Wave w = g.waves.get(i);
				b.append(i == 0 ? "" : "/").append(w.number).append(w.role == null ? '-' : w.role.code()).append(w.ticks).append(w.done ? 'd' : 'x');
			}
		}
		return b.toString();
	}

	/** Reads a save as far as it can; damaged games are left out. Never null. */
	public static AssaultLog decode(String text)
	{
		AssaultLog out = new AssaultLog();
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
				int[] points = new int[4];
				boolean ok = ended >= 0 && ticks >= 0;
				for (int k = 0; k < 4; k++)
				{
					points[k] = Integer.parseInt(f[2 + k].trim());
					ok &= points[k] >= 0;
				}
				List<Wave> waves = new ArrayList<>();
				for (String w : f[6].trim().split("/"))
				{
					Matcher m = WAVE.matcher(w.trim());
					if (!m.matches())
					{
						ok = false;
						break;
					}
					int n = Integer.parseInt(m.group(1));
					ok &= n >= 1 && n <= Assault.WAVES;
					waves.add(new Wave(n, Role.of(m.group(2).charAt(0)), Integer.parseInt(m.group(3)), "d".equals(m.group(4))));
				}
				if (ok && !waves.isEmpty())
				{
					out.add(new Game(ended, ticks, waves, points));
				}
			}
			catch (IllegalArgumentException e)
			{
				// Left out: a bad number.
			}
		}
		return out;
	}
}
