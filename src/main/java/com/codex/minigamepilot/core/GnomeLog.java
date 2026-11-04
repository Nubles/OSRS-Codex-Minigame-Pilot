package com.codex.minigamepilot.core;

import com.codex.minigamepilot.core.GnomeRestaurant.Customer;
import com.codex.minigamepilot.core.GnomeRestaurant.Dish;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Your Gnome Restaurant deliveries, kept per account: each one's dish and customer (as the game's
 * own hand-over message named them), easy or hard, its time from the order to the hand-over, the
 * wait before it, Cooking experience gained on the way, and the tip handed over with its worth.
 * A delivery that came too late is kept too, with nothing earned, so its time still counts
 * against your pace.
 */
public final class GnomeLog
{
	/** Deliveries kept. */
	static final int KEEP = 300;
	/** A delivery longer than this had a break in it and is not timed (ms: half an hour). */
	public static final long LONGEST = 30 * 60_000L;
	/** A wait longer than this before an order is a break (ms: ten minutes). */
	public static final long LONGEST_WAIT = 10 * 60_000L;
	/** Experience and coins a delivery can come to, at most: a guard against damage. */
	static final int XP_MAX = 100_000;
	static final int GP_MAX = 50_000_000;
	private static final String VERSION = "1";

	/** One delivery. */
	public static final class Delivery
	{
		public final long endedAt;
		public final boolean hard;
		/** Null when not known. */
		public final Dish dish;
		public final Customer customer;
		/** Ms from the order to the hand-over, or 0 when not timed. */
		public final long ms;
		/** Ms since the delivery before, or 0 when not known. */
		public final long wait;
		/** Handed over in time; false: too late, nothing earned. */
		public final boolean onTime;
		public final int xp;
		/** The tip's worth in coins. */
		public final long gp;
		/** The tip, item id to count. */
		public final Map<Integer, Integer> tip;

		public Delivery(long endedAt, boolean hard, Dish dish, Customer customer, long ms, long wait, boolean onTime, int xp, long gp,
			Map<Integer, Integer> tip)
		{
			this.endedAt = Math.max(0, endedAt);
			this.hard = hard;
			this.dish = dish;
			this.customer = customer;
			this.ms = ms > 0 && ms <= LONGEST ? ms : 0;
			this.wait = wait > 0 && wait <= LONGEST_WAIT ? wait : 0;
			this.onTime = onTime;
			this.xp = Math.max(0, Math.min(XP_MAX, xp));
			this.gp = onTime ? Math.max(0, Math.min(GP_MAX, gp)) : 0;
			Map<Integer, Integer> t = new LinkedHashMap<>();
			if (onTime && tip != null)
			{
				for (Map.Entry<Integer, Integer> e : tip.entrySet())
				{
					if (e.getKey() != null && e.getKey() > 0 && e.getValue() != null && e.getValue() > 0 && t.size() < 8)
					{
						t.put(e.getKey(), Math.min(1_000_000, e.getValue()));
					}
				}
			}
			this.tip = Collections.unmodifiableMap(t);
		}

		/** Reward-token credits it earned: one easy, three hard, none late. */
		public int credits()
		{
			return onTime ? (hard ? GnomeRestaurant.HARD_CREDITS : GnomeRestaurant.EASY_CREDITS) : 0;
		}

		/** Ms it cost: the delivery and the wait before it. */
		public long cost()
		{
			return ms <= 0 ? 0 : ms + wait;
		}
	}

	private final List<Delivery> all = new ArrayList<>();

	public void add(Delivery d)
	{
		if (d == null)
		{
			return;
		}
		all.add(d);
		while (all.size() > KEEP)
		{
			all.remove(0);
		}
	}

	public List<Delivery> all()
	{
		return Collections.unmodifiableList(all);
	}

	public int count()
	{
		return all.size();
	}

	public Delivery last()
	{
		return all.isEmpty() ? null : all.get(all.size() - 1);
	}

	/** Deliveries handed over in time. */
	public int onTime()
	{
		int n = 0;
		for (Delivery d : all)
		{
			n += d.onTime ? 1 : 0;
		}
		return n;
	}

	/** The quickest delivery in time, easy or hard, or null. */
	public Delivery fastest(boolean hard)
	{
		Delivery best = null;
		for (Delivery d : all)
		{
			if (d.onTime && d.hard == hard && d.ms > 0 && (best == null || d.ms < best.ms))
			{
				best = d;
			}
		}
		return best;
	}

	/** Cooking experience an hour over your timed deliveries, waits too, or -1 with none timed. */
	public double xpPerHour()
	{
		long t = 0;
		long x = 0;
		for (Delivery d : all)
		{
			if (d.cost() > 0)
			{
				t += d.cost();
				x += d.xp;
			}
		}
		return t == 0 ? -1 : x * 3_600_000.0 / t;
	}

	/** Tips' worth an hour over your timed deliveries, waits too, or -1 with none timed. */
	public double gpPerHour()
	{
		long t = 0;
		long g = 0;
		for (Delivery d : all)
		{
			if (d.cost() > 0)
			{
				t += d.cost();
				g += d.gp;
			}
		}
		return t == 0 ? -1 : g * 3_600_000.0 / t;
	}

	/** Deliveries an hour over your timed ones, or -1. */
	public double perHour()
	{
		long t = 0;
		int n = 0;
		for (Delivery d : all)
		{
			if (d.cost() > 0)
			{
				t += d.cost();
				n += d.onTime ? 1 : 0;
			}
		}
		return t == 0 ? -1 : n * 3_600_000.0 / t;
	}

	/** Each dish delivered in time, how many times. */
	public Map<Dish, Integer> byDish()
	{
		Map<Dish, Integer> out = new EnumMap<>(Dish.class);
		for (Delivery d : all)
		{
			if (d.onTime && d.dish != null)
			{
				out.merge(d.dish, 1, Integer::sum);
			}
		}
		return out;
	}

	/** Every tip item kept, summed. */
	public Map<Integer, Integer> tips()
	{
		Map<Integer, Integer> out = new LinkedHashMap<>();
		for (Delivery d : all)
		{
			for (Map.Entry<Integer, Integer> e : d.tip.entrySet())
			{
				out.merge(e.getKey(), e.getValue(), Integer::sum);
			}
		}
		return out;
	}

	/** "1;endedAt,E|H,DISH|-,CUSTOMER|-,ms,wait,o|l,xp,gp,id:n+id:n;..." */
	public String encode()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (Delivery d : all)
		{
			b.append(';').append(d.endedAt).append(',').append(d.hard ? 'H' : 'E').append(',').append(d.dish == null ? "-" : d.dish.name())
				.append(',').append(d.customer == null ? "-" : d.customer.name()).append(',').append(d.ms).append(',').append(d.wait)
				.append(',').append(d.onTime ? 'o' : 'l').append(',').append(d.xp).append(',').append(d.gp).append(',');
			boolean first = true;
			for (Map.Entry<Integer, Integer> e : d.tip.entrySet())
			{
				b.append(first ? "" : "+").append(e.getKey()).append(':').append(e.getValue());
				first = false;
			}
		}
		return b.toString();
	}

	/** Reads a save as far as it can; damaged deliveries are left out. Never null. */
	public static GnomeLog decode(String text)
	{
		GnomeLog out = new GnomeLog();
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
			if (f.length < 10)
			{
				continue;
			}
			try
			{
				long ended = Long.parseLong(f[0].trim());
				String level = f[1].trim();
				Dish dish = "-".equals(f[2].trim()) ? null : Dish.valueOf(f[2].trim());
				Customer customer = "-".equals(f[3].trim()) ? null : Customer.valueOf(f[3].trim());
				long ms = Long.parseLong(f[4].trim());
				long wait = Long.parseLong(f[5].trim());
				String how = f[6].trim();
				int xp = Integer.parseInt(f[7].trim());
				long gp = Long.parseLong(f[8].trim());
				if (ended < 0 || ms < 0 || wait < 0 || xp < 0 || gp < 0 || !("E".equals(level) || "H".equals(level))
					|| !("o".equals(how) || "l".equals(how)))
				{
					continue;
				}
				Map<Integer, Integer> tip = new LinkedHashMap<>();
				for (String pair : f[9].trim().split("\\+"))
				{
					if (pair.isEmpty())
					{
						continue;
					}
					String[] kv = pair.split(":");
					if (kv.length == 2)
					{
						tip.merge(Integer.parseInt(kv[0].trim()), Integer.parseInt(kv[1].trim()), Integer::sum);
					}
				}
				out.add(new Delivery(ended, "H".equals(level), dish, customer, ms, wait, "o".equals(how), xp, gp, tip));
			}
			catch (IllegalArgumentException e)
			{
				// Left out: a bad number or name.
			}
		}
		return out;
	}
}
