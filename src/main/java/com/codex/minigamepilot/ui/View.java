package com.codex.minigamepilot.ui;

import com.codex.minigamepilot.core.RewardRoad;
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
		/** A line on what Codex Navigator's goals need from this minigame's skill, or null. */
		String aside;

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

		/** Adds a line on what your Navigator goals need. */
		public GoalCard aside(String text)
		{
			aside = text;
			return this;
		}
	}

	/** One bar in the ranking of what pays at the Blast Furnace. */
	public static final class Pick
	{
		final String name;
		/** The big figure on the right: coins an hour, or a bar when the trip time is not known. */
		final String figure;
		/** A quieter line: coins a bar, coal trips, experience, or the level it needs. */
		final String detail;
		final boolean unlocked;
		final boolean current;
		/** How it compares with the best bar you can make, 0 to 1, for the bar under it. */
		final double share;
		final boolean loss;

		public Pick(String name, String figure, String detail, boolean unlocked, boolean current, double share, boolean loss)
		{
			this.name = name;
			this.figure = figure;
			this.detail = detail;
			this.unlocked = unlocked;
			this.current = current;
			this.share = share;
			this.loss = loss;
		}
	}

	/** A ranking at today's prices, best first: what each bar pays, or what each contract tier costs. */
	public static final class PickCard
	{
		final String title;
		final String trailing;
		final String note;
		final List<Pick> picks;

		public PickCard(String trailing, String note, List<Pick> picks)
		{
			this("What pays", trailing, note, picks);
		}

		public PickCard(String title, String trailing, String note, List<Pick> picks)
		{
			this.title = title;
			this.trailing = trailing;
			this.note = note;
			this.picks = picks == null ? Collections.emptyList() : picks;
		}
	}

	/**
	 * How far the supplies in your bank go: each supply and how many you have, what they come to
	 * in a sentence, what runs out first, and where and when the bank was seen.
	 */
	public static final class Supply
	{
		final String trailing;
		final List<Line> lines;
		final String headline;
		final String detail;
		/** True when the detail is a shortfall, drawn in ember. */
		final boolean warn;
		final String note;

		public Supply(String trailing, List<Line> lines, String headline, String detail, boolean warn, String note)
		{
			this.trailing = trailing;
			this.lines = lines == null ? Collections.emptyList() : lines;
			this.headline = headline;
			this.detail = detail;
			this.warn = warn;
			this.note = note;
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
	/** The minigame's whole shop as a road, or empty. */
	final List<RewardRoad.Stop> road = new ArrayList<>();
	PickCard pick;
	Supply supply;

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

	/** Logged out: what the Pilot does at each minigame, for a first look before logging in. */
	public static View loggedOut()
	{
		View v = new View(false, Activity.NONE, null, null, null, -1);
		v.brief(new Brief(Activity.MOTHERLODE, null,
			"Each load sized to fill the sack, the nearest free vein on foot, and rockfalls and struts dealt with on the way."));
		v.brief(new Brief(Activity.FURNACE, null,
			"Coal trips worked out ahead, the whole round in order, the coffer watched, and what each bar pays."));
		v.brief(new Brief(Activity.HOMES, null,
			"The planks to bring, the way to the client, each piece of furniture in walking order, and what a contract costs you."));
		return v;
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

	public View pick(PickCard p)
	{
		pick = p;
		return this;
	}

	public View supply(Supply s)
	{
		supply = s;
		return this;
	}

	public View goal(GoalCard g)
	{
		goals.add(g);
		return this;
	}

	/** Sets the reward road for the minigame you are in. */
	public View road(List<RewardRoad.Stop> stops)
	{
		road.clear();
		if (stops != null)
		{
			road.addAll(stops);
		}
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
			b.append("|goal").append(g.activity).append(g.title).append(g.have).append('/').append(g.need).append(g.note).append(g.aside);
			for (Line l : g.pieces)
			{
				b.append(l.mark);
			}
		}
		for (RewardRoad.Stop r : road)
		{
			// Time to a stop moves in steps of five minutes, so the road does not redraw every tick.
			b.append("|road").append(r.getName()).append(r.isOwned()).append(r.isAffordable()).append(r.getLeft())
				.append('/').append(r.getMinutesAway() < 0 ? -1 : r.getMinutesAway() / 5).append('/').append(r.getRoundsAway());
		}
		for (Brief r : briefs)
		{
			b.append('|').append(r.activity).append(r.status);
		}
		if (pick != null)
		{
			b.append("|pick").append(pick.title).append(pick.trailing).append(pick.note);
			for (Pick p : pick.picks)
			{
				b.append(p.name).append(p.figure).append(p.detail).append(p.current);
			}
		}
		if (supply != null)
		{
			b.append("|supply").append(supply.trailing).append(supply.headline).append(supply.detail).append(supply.note);
			for (Line l : supply.lines)
			{
				b.append(l.text).append(l.trailing).append(l.mark);
			}
		}
		return b.toString();
	}

	public View brief(Brief b)
	{
		briefs.add(b);
		return this;
	}
}
