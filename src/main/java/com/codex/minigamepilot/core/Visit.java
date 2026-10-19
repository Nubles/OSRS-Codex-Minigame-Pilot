package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What a session at a minigame came to, kept when you leave it: how long it ran and its counts
 * ("318 pay-dirt", "21,400 Mining xp"), said in the chat as you go and shown on the panel's card
 * for that minigame until the next visit. Saved per account as {@code endedAt|minutes|part;part}.
 */
public final class Visit
{
	/** A session shorter than this, or with nothing done, is not worth a summary. */
	public static final long MIN_MS = 5 * 60_000L;

	private final long endedAt;
	private final long minutes;
	private final List<String> parts;

	public Visit(long endedAt, long minutes, List<String> parts)
	{
		this.endedAt = endedAt;
		this.minutes = Math.max(0, minutes);
		List<String> clean = new ArrayList<>();
		for (String p : parts == null ? Collections.<String>emptyList() : parts)
		{
			if (p != null && !p.trim().isEmpty())
			{
				// The saved form splits on these.
				clean.add(p.replace('|', ' ').replace(';', ',').trim());
			}
		}
		this.parts = Collections.unmodifiableList(clean);
	}

	/**
	 * A summary of a session, or null when it was too short or did nothing worth saying.
	 *
	 * @param elapsedMs how long the session ran
	 * @param parts its counts, those with nothing to show already left out
	 */
	public static Visit of(long endedAt, long elapsedMs, List<String> parts)
	{
		Visit v = new Visit(endedAt, elapsedMs / 60_000L, parts);
		return elapsedMs < MIN_MS || v.parts.isEmpty() ? null : v;
	}

	public long getEndedAt()
	{
		return endedAt;
	}

	public List<String> getParts()
	{
		return parts;
	}

	/** "52m" or "1h 12m". */
	public String duration()
	{
		return minutes < 60 ? minutes + "m" : (minutes / 60) + "h " + (minutes % 60) + "m";
	}

	/** "1h 12m: 318 pay-dirt, 21,400 Mining xp and ore worth 48k." */
	public String line()
	{
		return duration() + ": " + counts() + ".";
	}

	/** "Last visit 3 hours ago: 1h 12m for 318 pay-dirt, ..." for the card, against the time now. */
	public String status(long now)
	{
		return "Last visit " + ago(endedAt, now) + ": " + duration() + " for " + counts() + ".";
	}

	/** "318 pay-dirt, 21,400 Mining xp and ore worth 48k" */
	private String counts()
	{
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < parts.size(); i++)
		{
			if (i > 0)
			{
				b.append(i == parts.size() - 1 ? " and " : ", ");
			}
			b.append(parts.get(i));
		}
		return b.toString();
	}

	/** "just now", "12 minutes ago", "3 hours ago", "2 days ago". */
	static String ago(long then, long now)
	{
		long m = Math.max(0, now - then) / 60_000L;
		if (m < 1)
		{
			return "just now";
		}
		if (m < 60)
		{
			return m + (m == 1 ? " minute ago" : " minutes ago");
		}
		long h = m / 60;
		if (h < 48)
		{
			return h + (h == 1 ? " hour ago" : " hours ago");
		}
		return (h / 24) + " days ago";
	}

	/** Whether this came to the same counts as another, however long each ran. */
	public boolean sameCounts(Visit other)
	{
		return other != null && parts.equals(other.parts);
	}

	public String encode()
	{
		return endedAt + "|" + minutes + "|" + String.join(";", parts);
	}

	private static final java.util.regex.Pattern DAMAGED = java.util.regex.Pattern.compile("NaN|Infinity|(^|\\s)-\\d");

	/** True when no count in a summary is below zero or not a number, as no visit writes one. */
	private static boolean plain(List<String> parts)
	{
		for (String p : parts)
		{
			if (DAMAGED.matcher(p).find())
			{
				return false;
			}
		}
		return true;
	}

	/** A saved summary, or null when there is none, it cannot be read or it is damaged. */
	public static Visit decode(String saved)
	{
		if (saved == null || saved.isEmpty())
		{
			return null;
		}
		String[] f = saved.split("\\|", 3);
		if (f.length < 3)
		{
			return null;
		}
		try
		{
			long ended = Long.parseLong(f[0].trim());
			long minutes = Long.parseLong(f[1].trim());
			List<String> parts = new ArrayList<>();
			Collections.addAll(parts, f[2].split(";"));
			Visit v = new Visit(ended, minutes, parts);
			return ended <= 0 || v.parts.isEmpty() || !plain(v.parts) ? null : v;
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}
}
