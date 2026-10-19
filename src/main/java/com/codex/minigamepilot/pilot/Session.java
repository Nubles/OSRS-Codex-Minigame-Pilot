package com.codex.minigamepilot.pilot;

import net.runelite.api.Client;
import net.runelite.api.Skill;

/** A minigame session: when it began, experience in its skill, and per-hour pace. */
final class Session
{
	private final Skill skill;
	private long started;
	private int startXp = -1;
	private long last;
	/** Set when another account logs in: its first tick starts the session over. */
	private boolean startOver;

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
		// Every account has some Hitpoints experience: none at all means the skills have not
		// arrived yet (just logged in), and a start taken then would count your whole total.
		return tick(client.getSkillExperience(skill), client.getSkillExperience(Skill.HITPOINTS) > 0, now);
	}

	/**
	 * @param xp experience in the session's skill now
	 * @param loaded whether the skills have arrived from the game yet
	 */
	boolean tick(int xp, boolean loaded, long now)
	{
		boolean restart = restarts(started, last, now) || startOver && started != 0;
		startOver = false;
		if (started == 0 || restart)
		{
			started = now;
			startXp = -1;
		}
		if (startXp < 0 && loaded)
		{
			startXp = xp;
		}
		last = now;
		return restart;
	}

	/** Whether a session begun at {@code started}, last seen at {@code last}, starts over now. */
	static boolean restarts(long started, long last, long now)
	{
		return started != 0 && now - last > BREAK_MS;
	}

	/** Whether a session is under way; not once another account has logged in, until its first tick. */
	boolean begun()
	{
		return started > 0 && !startOver;
	}

	/**
	 * Another account has logged in. A session otherwise carries on through a short logout, and
	 * would give the new account the last one's counts, hourly pace and best visit.
	 */
	void startOver()
	{
		startOver = true;
	}

	/** When this session began, or 0 before it has; a new one after a break begins again. */
	long startedAt()
	{
		return startOver ? 0 : started;
	}

	/** How long the session has run, to its last tick. */
	long activeMs()
	{
		return started == 0 ? 0 : Math.max(0, last - started);
	}

	int xp(Client client)
	{
		return xp(client.getSkillExperience(skill));
	}

	/** Experience gained this session, from the skill's experience now; 0 while it is not loaded. */
	int xp(int now)
	{
		return startXp < 0 || now <= 0 ? 0 : Math.max(0, now - startXp);
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

	/** A count written as every card writes it: "68,040", "142k", "1.2M". */
	static String format(long n)
	{
		return com.codex.minigamepilot.core.Figures.count(n);
	}
}
