package com.codex.minigamepilot.core;

import java.util.List;
import java.util.Map;

/**
 * The levels Codex Navigator's pinned goals still need, so a minigame can say what its training
 * is for. Navigator saves its goal board for companion plugins to read (fields only ever added);
 * this keeps just what the Pilot uses: each unfinished goal's title, how long it has left and
 * the levels it needs. With Navigator not installed there is no board and nothing changes.
 */
public final class NavigatorGoals
{
	/** One of Navigator's pinned goals, as saved. Field names are Navigator's. */
	public static final class Entry
	{
		public String key;
		public String title;
		public boolean complete;
		public double hoursLeft;
		public Map<String, Integer> skills;
	}

	/** Navigator's saved board, as far as the Pilot reads it. */
	public static final class Board
	{
		public int version;
		public String focus;
		public List<Entry> goals;
	}

	/** A level a goal needs. */
	public static final class Target
	{
		private final int level;
		private final String goal;
		private final boolean focus;

		Target(int level, String goal, boolean focus)
		{
			this.level = level;
			this.goal = goal;
			this.focus = focus;
		}

		public int getLevel()
		{
			return level;
		}

		/** The goal's title, such as "Dragon Slayer II". */
		public String getGoal()
		{
			return goal;
		}

		/** True when it is the goal Navigator has in focus. */
		public boolean isFocus()
		{
			return focus;
		}
	}

	private NavigatorGoals()
	{
	}

	/**
	 * The level to train a skill to next for your goals: the focus goal's, when it needs this
	 * skill above where you are; otherwise the nearest level above yours any unfinished goal needs.
	 *
	 * @return the target, or null when no goal needs the skill above your level
	 */
	public static Target target(Board board, String skill, int level)
	{
		if (board == null || board.goals == null || skill == null)
		{
			return null;
		}
		Target nearest = null;
		for (Entry e : board.goals)
		{
			if (e == null || e.complete || e.skills == null)
			{
				continue;
			}
			Integer need = need(e.skills, skill);
			if (need == null || need <= level || need > 99)
			{
				continue;
			}
			String title = e.title == null || e.title.isEmpty() ? "a Navigator goal" : e.title;
			boolean focus = e.key != null && e.key.equals(board.focus);
			if (focus)
			{
				return new Target(need, title, true);
			}
			if (nearest == null || need < nearest.level)
			{
				nearest = new Target(need, title, false);
			}
		}
		return nearest;
	}

	/** The level a goal needs in a skill, matching the name however it is capitalised. */
	private static Integer need(Map<String, Integer> skills, String skill)
	{
		for (Map.Entry<String, Integer> s : skills.entrySet())
		{
			if (s.getKey() != null && s.getKey().equalsIgnoreCase(skill) && s.getValue() != null)
			{
				return s.getValue();
			}
		}
		return null;
	}
}
