package com.codex.minigamepilot.ui;

import com.codex.minigamepilot.core.Step;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Everything the panel shows, worked out on the client thread and handed over whole. */
public final class View
{
	/** Which minigame the Pilot is flying. */
	public enum Activity
	{
		NONE("Minigame Pilot"),
		MOTHERLODE("Motherlode Mine"),
		FURNACE("Blast Furnace"),
		HOMES("Mahogany Homes");

		private final String title;

		Activity(String title)
		{
			this.title = title;
		}

		public String getTitle()
		{
			return title;
		}
	}

	/** A filled bar: how much of something there is against its limit, with a lighter part still on its way. */
	public static final class Gauge
	{
		final String label;
		final int value;
		final int coming;
		final int max;
		final String note;
		final Color tone;

		public Gauge(String label, int value, int coming, int max, String note, Color tone)
		{
			this.label = label;
			this.value = value;
			this.coming = coming;
			this.max = max;
			this.note = note;
			this.tone = tone;
		}
	}

	/** A number for the session tiles. */
	public static final class Stat
	{
		final String value;
		final String label;
		final String note;
		final Color tone;

		public Stat(String value, String label, String note, Color tone)
		{
			this.value = value;
			this.label = label;
			this.note = note;
			this.tone = tone;
		}
	}

	/** One line of a checklist, such as a piece of furniture. */
	public static final class Line
	{
		/** How a line is marked. */
		public enum Mark
		{
			TODO,
			NEXT,
			DONE,
			IDLE
		}

		final String text;
		final String trailing;
		final Mark mark;

		public Line(String text, String trailing, Mark mark)
		{
			this.text = text;
			this.trailing = trailing;
			this.mark = mark;
		}
	}

	/** A minigame the Pilot knows, for when the player is in none of them. */
	public static final class Brief
	{
		final Activity activity;
		final String status;
		final String how;

		public Brief(Activity activity, String status, String how)
		{
			this.activity = activity;
			this.status = status;
			this.how = how;
		}
	}

	/** One stage of the last round against your best for it. */
	public static final class Split
	{
		final String name;
		final int ticks;
		final int best;
		final boolean slowest;

		public Split(String name, int ticks, int best, boolean slowest)
		{
			this.name = name;
			this.ticks = ticks;
			this.best = best;
			this.slowest = slowest;
		}
	}

	/** The pace coach's verdict on the last round. */
	public static final class PaceCard
	{
		final int lastTicks;
		/** Your best for this kind of round, this one included; 0 when there is none. */
		final int bestTicks;
		final boolean isBest;
		/** Ticks over (positive) or under your best before this round; 0 when there was none. */
		final int againstBest;
		final String advice;
		final boolean costly;
		final String tail;
		/** Each stage of the loop this round: its name, ticks, and your best for it (0 for none). */
		final List<Split> splits = new ArrayList<>();
		/** The stage that cost the most against its best, in a sentence, or null. */
		String splitNote;

		public PaceCard(int lastTicks, int bestTicks, boolean isBest, int againstBest, String advice, boolean costly, String tail)
		{
			this.lastTicks = lastTicks;
			this.bestTicks = bestTicks;
			this.isBest = isBest;
			this.againstBest = againstBest;
			this.advice = advice;
			this.costly = costly;
			this.tail = tail;
		}

		/** Adds the round's stages and the line on the slowest one. */
		public PaceCard splits(List<Split> list, String note)
		{
			splits.addAll(list);
			splitNote = note;
			return this;
		}
	}

	/** A goal being worked toward in a minigame: a reward from its shop, or a level. */
	public static final class GoalCard
	{
		final Activity activity;
		final String title;
		final long have;
		final long need;
		final String unit;
		final String note;
		final List<Line> pieces;
		final boolean done;

		/**
		 * @param have how much of the goal you hold, against need
		 * @param note how far off it is, in a sentence
		 * @param pieces the rewards it covers, ticked when owned, or empty for a level
		 */
		public GoalCard(Activity activity, String title, long have, long need, String unit, String note, List<Line> pieces, boolean done)
		{
			this.activity = activity;
			this.title = title;
			this.have = have;
			this.need = need;
			this.unit = unit;
			this.note = note;
			this.pieces = pieces == null ? Collections.emptyList() : pieces;
			this.done = done;
		}
	}

	final boolean loggedIn;
	final Activity activity;
	final Step step;
	final String place;
	final String[] loop;
	final int loopAt;
	final List<Gauge> gauges = new ArrayList<>();
	final List<Stat> stats = new ArrayList<>();
	final List<Line> lines = new ArrayList<>();
	String linesTitle;
	String linesNote;
	final List<Brief> briefs = new ArrayList<>();
	PaceCard pace;
	final List<GoalCard> goals = new ArrayList<>();

	/**
	 * @param place where, such as "Falador" or "Lower level", or null
	 * @param loop the names of the loop's stages, or null
	 * @param loopAt which stage the step belongs to
	 */
	public View(boolean loggedIn, Activity activity, Step step, String place, String[] loop, int loopAt)
	{
		this.loggedIn = loggedIn;
		this.activity = activity;
		this.step = step;
		this.place = place;
		this.loop = loop;
		this.loopAt = loopAt;
	}

	public static View loggedOut()
	{
		return new View(false, Activity.NONE, null, null, null, -1);
	}

	public int loopAt()
	{
		return loopAt;
	}

	/** The names of the loop's stages, or null. */
	public String[] loopNames()
	{
		return loop == null ? null : loop.clone();
	}

	public View goal(GoalCard g)
	{
		goals.add(g);
		return this;
	}

	public Activity getActivity()
	{
		return activity;
	}

	public View pace(PaceCard p)
	{
		pace = p;
		return this;
	}

	public View gauge(Gauge g)
	{
		gauges.add(g);
		return this;
	}

	public View stat(Stat s)
	{
		stats.add(s);
		return this;
	}

	public View lines(String title, String note, List<Line> list)
	{
		linesTitle = title;
		linesNote = note;
		lines.addAll(list == null ? Collections.emptyList() : list);
		return this;
	}

	/** A summary of everything shown, so the panel is only rebuilt when something on it changes. */
	public String signature()
	{
		StringBuilder b = new StringBuilder();
		b.append(loggedIn).append(activity).append(step).append(place).append(loopAt).append(linesNote);
		for (Gauge g : gauges)
		{
			b.append('|').append(g.label).append(g.value).append('/').append(g.coming).append('/').append(g.max).append(g.note);
		}
		for (Stat s : stats)
		{
			b.append('|').append(s.value).append(s.label).append(s.note);
		}
		for (Line l : lines)
		{
			b.append('|').append(l.text).append(l.trailing).append(l.mark);
		}
		if (pace != null)
		{
			b.append("|pace").append(pace.lastTicks).append(pace.bestTicks).append(pace.advice).append(pace.tail).append(pace.splitNote);
		}
		for (GoalCard g : goals)
		{
			b.append("|goal").append(g.activity).append(g.title).append(g.have).append('/').append(g.need).append(g.note);
			for (Line l : g.pieces)
			{
				b.append(l.mark);
			}
		}
		for (Brief r : briefs)
		{
			b.append('|').append(r.activity).append(r.status);
		}
		return b.toString();
	}

	public View brief(Brief b)
	{
		briefs.add(b);
		return this;
	}
}
