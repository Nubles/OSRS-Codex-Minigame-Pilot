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
		HOMES("Mahogany Homes"),
		RIFT("Guardians of the Rift"),
		TEMPOROSS("Tempoross"),
		FOUNDRY("Giants' Foundry"),
		MIXOLOGY("Mastering Mixology"),
		TOTEMS("Vale Totems"),
		TITHE("Tithe Farm"),
		ARTEFACTS("Stealing Artefacts"),
		HOUSES("Stealing Valuables"),
		ROGUES_DEN("Rogues' Den"),
		BARBARIAN_ASSAULT("Barbarian Assault"),
		WINTERTODT("Wintertodt"),
		GNOME_RESTAURANT("Gnome Restaurant"),
		NIGHTMARE_ZONE("Nightmare Zone"),
		VOLCANIC_MINE("Volcanic Mine"),
		BARRACUDA_TRIALS("Barracuda Trials"),
		TROUBLE_BREWING("Trouble Brewing"),
		SORCERESS_GARDEN("Sorceress's Garden");

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

		/** Your fastest visit, in gold under the status, or null. */
		String best;

		public Brief best(String line)
		{
			best = line;
			return this;
		}

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

		public String name()
		{
			return name;
		}

		public int ticks()
		{
			return ticks;
		}

		public int best()
		{
			return best;
		}

		public boolean isSlowest()
		{
			return slowest;
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

	/**
	 * The tick coach: the last loop in ticks against your best and against the floor of its walks,
	 * its single biggest leak, loops and experience an hour, and every loop as a histogram.
	 */
	public static final class TickCard
	{
		final int lastTicks;
		/** Your best loop of the same kind, this one included, or 0. */
		final int bestTicks;
		/** Ticks the loop spent on foot on walks of known length, and their floor at run pace. */
		final int footTicks;
		final int floorTicks;
		final String leak;
		/** Whether the leak is one worth fixing, rather than a clean loop. */
		final boolean leaking;
		final long perHour;
		/** Experience an hour while looping, or -1 when not known. */
		final long xpPerHour;
		/** "Mining xp", for the tile under the figure. */
		final String xpLabel;
		final int loops;
		final List<com.codex.minigamepilot.core.Loops.Bin> bins = new ArrayList<>();
		/** True when no loop of this kind was timed before, so there was no best to beat. */
		boolean first;

		/** Marks the loop as the first of its kind. */
		public TickCard first(boolean f)
		{
			first = f;
			return this;
		}

		public TickCard(int lastTicks, int bestTicks, int footTicks, int floorTicks, String leak, boolean leaking,
			long perHour, long xpPerHour, String xpLabel, int loops, List<com.codex.minigamepilot.core.Loops.Bin> bins)
		{
			this.lastTicks = lastTicks;
			this.bestTicks = bestTicks;
			this.footTicks = footTicks;
			this.floorTicks = floorTicks;
			this.leak = leak;
			this.leaking = leaking;
			this.perHour = perHour;
			this.xpPerHour = xpPerHour;
			this.xpLabel = xpLabel;
			this.loops = loops;
			if (bins != null)
			{
				this.bins.addAll(bins);
			}
		}

		public String getLeak()
		{
			return leak;
		}

		public int getLastTicks()
		{
			return lastTicks;
		}

		public int getFloorTicks()
		{
			return floorTicks;
		}

		public int getFootTicks()
		{
			return footTicks;
		}

		public long getPerHour()
		{
			return perHour;
		}

		public int getLoops()
		{
			return loops;
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
		String title = "In the bank";

		/** The card's heading, "In the bank" unless set. */
		public Supply titled(String t)
		{
			title = t;
			return this;
		}

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

	/**
	 * A small map of a minigame's patches as they stand, such as the Tithe Farm's: each patch at
	 * its place on the ground, coloured by what it needs, numbered in your round's order.
	 */
	public static final class Field
	{
		/** What a patch needs, which sets its colour. */
		public enum Need
		{
			EMPTY,
			/** Needs water. */
			DRY,
			/** Needs water and is about to die. */
			URGENT,
			WET,
			GROWN,
			DEAD
		}

		/** One patch: its south-west tile relative to the field's, its size in tiles, and its state. */
		public static final class Cell
		{
			final int x;
			final int y;
			final int size;
			final Need need;
			/** Its place in your round, from 1, or 0 when it is not in it. */
			final int order;
			final boolean next;
			/** Planned as one of the next to plant. */
			final boolean planned;

			public Cell(int x, int y, int size, Need need, int order, boolean next, boolean planned)
			{
				this.x = x;
				this.y = y;
				this.size = Math.max(1, size);
				this.need = need;
				this.order = order;
				this.next = next;
				this.planned = planned;
			}
		}

		final String title;
		final String note;
		final List<Cell> cells;
		/** Where you stand relative to the field, or null. */
		final int[] you;

		public Field(String title, String note, List<Cell> cells, int[] you)
		{
			this.title = title;
			this.note = note;
			this.cells = cells == null ? Collections.emptyList() : cells;
			this.you = you;
		}

		/** "D2 W5 G1" style counts, for replays and tests. */
		public String summary()
		{
			int[] n = new int[Need.values().length];
			for (Cell c : cells)
			{
				n[c.need.ordinal()]++;
			}
			StringBuilder b = new StringBuilder();
			for (Need k : Need.values())
			{
				if (k != Need.EMPTY && n[k.ordinal()] > 0)
				{
					b.append(b.length() == 0 ? "" : " ").append(k.name().toLowerCase(java.util.Locale.ROOT)).append(' ').append(n[k.ordinal()]);
				}
			}
			return b.length() == 0 ? "empty" : b.toString();
		}
	}

	/**
	 * Port Piscarilius's houses on the panel, north up, in world tiles: the house of the task
	 * ringed, Captain Khaled, the patrols with the way each faces, and where you stand.
	 */
	public static final class District
	{
		/** One house: where its drawers are (or about), a short label, and whether it is the task's. */
		public static final class Place
		{
			final int x;
			final int y;
			final String label;
			final boolean target;
			/** Its drawers have been seen there, so the spot is the game's, not an estimate. */
			final boolean seen;

			public Place(int x, int y, String label, boolean target, boolean seen)
			{
				this.x = x;
				this.y = y;
				this.label = label;
				this.target = target;
				this.seen = seen;
			}
		}

		/** A patrol: where it stands and the way it faces (the game's orientation, 0 south, 512 west). */
		public static final class Patrol
		{
			final int x;
			final int y;
			final int orientation;

			public Patrol(int x, int y, int orientation)
			{
				this.x = x;
				this.y = y;
				this.orientation = orientation;
			}
		}

		final String title;
		final String note;
		final List<Place> houses;
		final List<Patrol> patrols;
		/** Captain Khaled, where last seen, or null. */
		final int[] khaled;
		/** Where you stand, or null. */
		final int[] you;
		/** You carry an artefact: the patrols matter. */
		final boolean hot;

		/** What the key calls the ringed house, the marks and the teal dot: "Task", "Patrol" and "Khaled" unless renamed. */
		String targetName = "Task";
		String patrolName = "Patrol";
		String keeperName = "Khaled";
		/** Name the marks in the key even when there are none. */
		boolean keyPatrols = true;

		/** The same map for another place: what the ringed house, the marks and the teal dot are called. */
		public District named(String target, String patrol, String keeper)
		{
			targetName = target;
			patrolName = patrol;
			keeperName = keeper;
			keyPatrols = false;
			return this;
		}

		public District(String title, String note, List<Place> houses, List<Patrol> patrols, int[] khaled, int[] you, boolean hot)
		{
			this.title = title;
			this.note = note;
			this.houses = houses == null ? Collections.emptyList() : houses;
			this.patrols = patrols == null ? Collections.emptyList() : patrols;
			this.khaled = khaled;
			this.you = you;
			this.hot = hot;
		}

		/** "target North house, 2 patrols" style, for replays and tests. */
		public String summary()
		{
			String t = "none";
			for (Place p : houses)
			{
				if (p.target)
				{
					t = p.label;
				}
			}
			if (!keyPatrols)
			{
				return "target " + t + ", " + houses.size() + (houses.size() == 1 ? " house" : " houses") + ", " + patrols.size() + " "
					+ patrolName.toLowerCase(java.util.Locale.ROOT) + (patrols.size() == 1 ? "" : "s") + (khaled != null ? ", " + keeperName : "");
			}
			return "target " + t + ", " + patrols.size() + (patrols.size() == 1 ? " patrol" : " patrols") + (khaled != null ? ", Khaled" : "");
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
	TickCard ticks;
	final List<GoalCard> goals = new ArrayList<>();
	/** The minigame's whole shop as a road, or empty. */
	final List<RewardRoad.Stop> road = new ArrayList<>();
	PickCard pick;
	Supply supply;
	Field field;
	District district;

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

	/** Where the step is, such as "Lower level", or null. */
	public String place()
	{
		return place;
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
		v.brief(new Brief(Activity.RIFT, null,
			"Fragments to mine, essence to craft, the best open altar for your levels and points, then hand in."));
		v.brief(new Brief(Activity.TEMPOROSS, null,
			"The double spot when it is up, the shrine, the crate, a tether before each wave, and your permits game by game."));
		v.brief(new Brief(Activity.FOUNDRY, null,
			"The best moulds you own for the commission, the right tool at the right heat for each stage, and your swords kept."));
		v.brief(new Brief(Activity.MIXOLOGY, null,
			"Which orders your paste can fill, the levers for each, the right station with its cue, the paste to refine next, and your resin towards a reward."));
		v.brief(new Brief(Activity.TOTEMS, null,
			"The next totem site and the logs to bring, the three spirits each totem wants ringed in the carving choices, the offerings waiting, and your research points towards a reward."));
		v.brief(new Brief(Activity.TITHE, null,
			"The seed your Farming grows, the next patch to plant, water or harvest read from the patches themselves, the water you carry, and your points towards a reward."));
		v.brief(new Brief(Activity.ARTEFACTS, null,
			"The house Captain Khaled names, its ladder and drawers lit, the patrols marked on the way back, and every artefact kept with experience and coins an hour."));
		v.brief(new Brief(Activity.HOUSES, null,
			"The citizen an urchin has distracted, the vacant house once its owner has gone and how long you have, the open window out, and every haul kept with coins and experience an hour."));
		v.brief(new Brief(Activity.ROGUES_DEN, null,
			"The maze one leg at a time with its traps marked, the right tile and the shortcut at 80 Thieving, every run kept, and the rogue set's time at your pace."));
		v.brief(new Brief(Activity.BARBARIAN_ASSAULT, null,
			"Every game kept with its waves, roles and times, your honour points in each role against the reward you want, and the role to play next to get there soonest."));
		v.brief(new Brief(Activity.WINTERTODT, null,
			"Chop, fletch and feed one step at a time, your brazier lit, fixed and its Pyromancer healed, warmth watched, every round kept, and the pyromancer outfit's odds at your pace."));
		v.brief(new Brief(Activity.GNOME_RESTAURANT, null,
			"Each order's recipe one step at a time with what you lack, then the customer marked with the time left, every delivery kept with its tip, and every gnome dish as a checklist."));
		v.brief(new Brief(Activity.NIGHTMARE_ZONE, null,
			"Every dream kept with its bosses, points, time and potions, your points an hour by the bosses you dream, and the time to your imbue or herb boxes."));
		v.brief(new Brief(Activity.VOLCANIC_MINE, null,
			"Every game kept with its points, Mining experience, length and how it ended, points and experience an hour at your pace, and the time to your prospector kit or tome."));
		v.brief(new Brief(Activity.BARRACUDA_TRIALS, null,
			"Every Sailing trial run kept with its splits, a ghost of your best run of each course, Sailing experience an hour by trial, and the next rank's time to beat."));
		v.brief(new Brief(Activity.TROUBLE_BREWING, null,
			"Every game kept with its team, rum, pieces of eight and Cooking experience, pieces of eight an hour at your pace, and the time to your naval set or flags."));
		v.brief(new Brief(Activity.SORCERESS_GARDEN, null,
			"Every garden run kept, caught or not, your success rate and sq'irks an hour at your own pace, the juice you crush and Osman's Thieving xp for it, and the time to your level. No start cue: you run when you choose."));
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

	public View district(District d)
	{
		district = d;
		return this;
	}

	public View field(Field f)
	{
		field = f;
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

	public View ticks(TickCard t)
	{
		ticks = t;
		return this;
	}

	/** The tick coach's card, or null. */
	public TickCard ticks()
	{
		return ticks;
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

	/**
	 * What the tiles and gauges show, by label: a tile's value and a gauge's "value/max", plus
	 * the choice card's headline as "pick". For replays and tests.
	 */
	public java.util.Map<String, String> readings()
	{
		java.util.Map<String, String> r = new java.util.TreeMap<>();
		for (Stat s : stats)
		{
			r.put("stat." + s.label.replace(' ', '_'), s.value);
		}
		for (Gauge g : gauges)
		{
			r.put("gauge." + g.label.replace(' ', '_'), g.value + "/" + g.max);
		}
		if (pick != null)
		{
			r.put("pick", pick.trailing);
			r.put("pick.note", pick.note);
		}
		if (!lines.isEmpty())
		{
			StringBuilder b = new StringBuilder();
			for (Line l : lines)
			{
				b.append(b.length() == 0 ? "" : ", ").append(l.text).append(' ').append(l.mark);
			}
			r.put("lines", b.toString());
			r.put("lines.note", String.valueOf(linesNote));
		}
		for (GoalCard g : goals)
		{
			r.put("goal", g.have + "/" + g.need);
			r.put("goal.note", g.note);
			if (!g.pieces.isEmpty())
			{
				StringBuilder b = new StringBuilder();
				for (Line l : g.pieces)
				{
					b.append(b.length() == 0 ? "" : ", ").append(l.text).append(' ').append(l.mark);
				}
				r.put("goal.pieces", b.toString());
			}
		}
		if (supply != null)
		{
			r.put("supply.headline", String.valueOf(supply.headline));
		}
		if (field != null)
		{
			r.put("field", field.summary());
		}
		if (district != null)
		{
			r.put("district", district.summary());
		}
		if (pick != null && !pick.picks.isEmpty())
		{
			for (Pick p : pick.picks)
			{
				if (p.current)
				{
					r.put("pick.current", p.name + " " + p.figure);
				}
			}
		}
		return r;
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
		if (ticks != null)
		{
			b.append("|ticks").append(ticks.lastTicks).append('/').append(ticks.bestTicks).append('/').append(ticks.footTicks)
				.append('/').append(ticks.floorTicks).append(ticks.leak).append(ticks.perHour).append('/').append(ticks.xpPerHour)
				.append('/').append(ticks.loops);
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
			b.append('|').append(r.activity).append(r.status).append(r.best);
		}
		if (pick != null)
		{
			b.append("|pick").append(pick.title).append(pick.trailing).append(pick.note);
			for (Pick p : pick.picks)
			{
				b.append(p.name).append(p.figure).append(p.detail).append(p.current);
			}
		}
		if (field != null)
		{
			b.append("|field").append(field.title).append(field.note);
			for (Field.Cell c : field.cells)
			{
				b.append(c.need.ordinal()).append(c.order).append(c.next ? '*' : '.').append(c.planned ? '+' : '.');
			}
			if (field.you != null)
			{
				b.append(field.you[0]).append(',').append(field.you[1]);
			}
		}
		if (district != null)
		{
			b.append("|district").append(district.title).append(district.note).append(district.hot).append(district.keeperName);
			for (District.Place p : district.houses)
			{
				b.append(p.x).append(',').append(p.y).append(p.target ? '*' : '.').append(p.seen ? '+' : '.');
			}
			for (District.Patrol p : district.patrols)
			{
				b.append(p.x).append(',').append(p.y).append('/').append(p.orientation / 256);
			}
			if (district.khaled != null)
			{
				b.append('k').append(district.khaled[0]).append(',').append(district.khaled[1]);
			}
			if (district.you != null)
			{
				b.append('y').append(district.you[0]).append(',').append(district.you[1]);
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
