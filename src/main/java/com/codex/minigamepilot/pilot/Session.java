package com.codex.minigamepilot.pilot;

import java.text.NumberFormat;
import java.util.Locale;
import net.runelite.api.Client;
import net.runelite.api.Skill;

/** A minigame session: when it began, experience in its skill, and per-hour pace. */
final class Session
{
	private final Skill skill;
	private long started;
	private int startXp = -1;
	private long last;

	Session(Skill skill)
	{
		this.skill = skill;
	}

	/** A break longer than this starts a new session. */
	static final long BREAK_MS = 30 * 60_000L;

	/**
	 * Starts the clock on the first tick in the minigame, and again after a long break.
	 *
	 * @return true when a session already under way was started over after a break, so the
	 *     counts kept alongside it should start over too
	 */
	boolean tick(Client client, long now)
	{
		boolean restart = restarts(started, last, now);
		if (started == 0 || restart)
		{
			started = now;
			startXp = client.getSkillExperience(skill);
		}
		last = now;
		return restart;
	}

	/** Whether a session begun at {@code started}, last seen at {@code last}, starts over now. */
	static boolean restarts(long started, long last, long now)
	{
		return started != 0 && now - last > BREAK_MS;
	}

	boolean begun()
	{
		return started > 0;
	}

	int xp(Client client)
	{
		return startXp < 0 ? 0 : Math.max(0, client.getSkillExperience(skill) - startXp);
	}

	/** A count as an hourly pace, once there has been enough time to make it mean something. */
	String perHour(long count, long now)
	{
		long ms = now - started;
		if (started == 0 || ms < 60_000L)
		{
			return "";
		}
		return format(Math.round(count * 3_600_000d / ms)) + " an hour";
	}

	String elapsed(long now)
	{
		long minutes = started == 0 ? 0 : (now - started) / 60_000L;
		return minutes < 60 ? minutes + "m" : (minutes / 60) + "h " + (minutes % 60) + "m";
	}

	static String format(long n)
	{
		if (n >= 10_000_000)
		{
			return (n / 1_000_000) + "M";
		}
		if (n >= 100_000)
		{
			return (n / 1000) + "K";
		}
		return NumberFormat.getIntegerInstance(Locale.ENGLISH).format(n);
	}
}
