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
