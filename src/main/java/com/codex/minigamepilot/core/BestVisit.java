package com.codex.minigamepilot.core;

/**
 * Your fastest visit to a minigame: the most pay-dirt or bars an hour over a stretch of at least
 * {@link #MIN_MS}, with its experience an hour. Kept per account beside the last visit's summary,
 * shown on the minigame's card away from it, and said in the chat when a visit beats it. Saved as
 * {@code startedAt|endedAt|minutes|perHour|xpPerHour}.
 *
 * <p>A visit is measured each time it is summed up (walking out, logging out, hopping). Its
 * session carries on when you step back in, so a later reading of the same session replaces an
 * earlier one only when it is faster, and only beating a different session's record is news.
 */
public final class BestVisit
{
	/** Shorter stretches swing too much on one good sack or one quick trip to stand as a record. */
	public static final long MIN_MS = 15 * 60_000L;

	private final long startedAt;
	private final long endedAt;
	private final long minutes;
	private final long perHour;
	private final long xpPerHour;

	BestVisit(long startedAt, long endedAt, long minutes, long perHour, long xpPerHour)
	{
		this.startedAt = startedAt;
		this.endedAt = endedAt;
		this.minutes = minutes;
		this.perHour = perHour;
		this.xpPerHour = xpPerHour;
	}

	/**
	 * A visit's pace, or null when it ran under {@link #MIN_MS} or made nothing.
	 *
	 * @param startedAt when its session began, to tell one session's readings from another's
	 * @param activeMs how long it ran
	 * @param units pay-dirt deposited or bars taken
	 * @param xp experience gained in the minigame's skill
	 */
	public static BestVisit of(long startedAt, long endedAt, long activeMs, long units, long xp)
	{
		if (startedAt <= 0 || activeMs < MIN_MS || units <= 0)
		{
			return null;
		}
		return new BestVisit(startedAt, endedAt, activeMs / 60_000L,
			Math.round(units * 3_600_000d / activeMs), Math.round(Math.max(0, xp) * 3_600_000d / activeMs));
	}

	/** What a new reading does to the record held. */
	public enum Outcome
	{
		/** Not faster, or not a reading at all: the record stands. */
		KEPT,
		/** The first record, or the same session going faster: saved without a word. */
		QUIET,
		/** A faster visit than a different session's record: worth saying. */
		BEATEN
	}

	/** @param held the record so far, or null */
	public static Outcome judge(BestVisit held, BestVisit reading)
	{
		if (reading == null)
		{
			return Outcome.KEPT;
		}
		if (held == null)
		{
			return Outcome.QUIET;
		}
		if (reading.perHour <= held.perHour)
		{
			return Outcome.KEPT;
		}
		return held.startedAt == reading.startedAt ? Outcome.QUIET : Outcome.BEATEN;
	}

	/** Whether two readings are of the same session. */
	public boolean sameSession(BestVisit other)
	{
		return other != null && other.startedAt == startedAt;
	}

	public long getPerHour()
	{
		return perHour;
	}

	public long getXpPerHour()
	{
		return xpPerHour;
	}

	public long getEndedAt()
	{
		return endedAt;
	}

	/** "1,620 pay-dirt an hour" */
	public String pace(String unit)
	{
		return Figures.count(perHour) + " " + unit + " an hour";
	}

	/** For the chat: "A new best: 1,620 pay-dirt an hour, up from 1,512." */
	public String beat(BestVisit held, String unit)
	{
		return "A new best: " + pace(unit) + (held == null ? "." : ", up from " + Figures.count(held.perHour) + ".");
	}

	/**
	 * For the card: "Best visit 3 days ago: 1,620 pay-dirt and 81K Mining xp an hour over 1h 12m."
	 * When the last visit was the best, the card already says when and how long, so it reads
	 * "Your best visit yet, at 1,620 pay-dirt and 81K Mining xp an hour."
	 *
	 * @param last the summary of your last visit, or null
	 */
	public String status(String unit, String skill, Visit last, long now)
	{
		String rate = Figures.count(perHour) + " " + unit + (xpPerHour > 0 ? " and " + Figures.count(xpPerHour) + " " + skill + " xp" : "") + " an hour";
		if (last != null && last.getEndedAt() == endedAt)
		{
			return "Your best visit yet, at " + rate + ".";
		}
		long m = minutes;
		String over = m < 60 ? m + "m" : (m / 60) + "h " + (m % 60) + "m";
		return "Best visit " + Visit.ago(endedAt, now) + ": " + rate + " over " + over + ".";
	}

	public String encode()
	{
		return startedAt + "|" + endedAt + "|" + minutes + "|" + perHour + "|" + xpPerHour;
	}

	/** A saved record, or null when there is none or it cannot be read. */
	public static BestVisit decode(String saved)
	{
		if (saved == null || saved.isEmpty())
		{
			return null;
		}
		String[] f = saved.split("\\|");
		if (f.length != 5)
		{
			return null;
		}
		try
		{
			long started = Long.parseLong(f[0].trim());
			long ended = Long.parseLong(f[1].trim());
			long minutes = Long.parseLong(f[2].trim());
			long perHour = Long.parseLong(f[3].trim());
			long xpPerHour = Long.parseLong(f[4].trim());
			if (started <= 0 || ended < started || minutes < MIN_MS / 60_000L || perHour <= 0 || xpPerHour < 0)
			{
				return null;
			}
			return new BestVisit(started, ended, minutes, perHour, xpPerHour);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}
}
