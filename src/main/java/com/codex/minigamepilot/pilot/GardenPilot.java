package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Garden;
import com.codex.minigamepilot.core.GardenEta;
import com.codex.minigamepilot.core.GardenLog;
import com.codex.minigamepilot.core.GardenRuns;
import com.codex.minigamepilot.core.Goals;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

/**
 * The Sorceress's Garden, the summer garden first: every run kept per account (its garden, and
 * whether it paid a sq'irk, herbs or was caught), your success rate and sq'irks an hour at your own
 * pace, the juice you crush and the Thieving experience Osman gives for it, the Farming experience
 * the picking gave, and the time to the Thieving level, Farming level or glasses of juice you
 * choose.
 *
 * <p>Information only. It gives no start cue and times nothing ahead: the elementals' rounds are
 * not followed, nothing is lit in the world and no plaque is drawn in the game view. A lap is only
 * measured after you have run it, and you set off whenever you choose.
 */
public final class GardenPilot
{
	static final String LOG_KEY = "garden.kept";
	static final String JUICE_KEY = "garden.juice";
	/** Within this many tiles of the fountain is where a pick or a catch puts you. */
	static final int FOUNTAIN_REACH = 6;

	private Function<String, String> load;
	private BiConsumer<String, String> save;
	private GardenLog log;
	private GardenRuns runs;
	/** The garden chosen in the settings, or null for the one you last ran. */
	private Garden.Season chosen = Garden.Season.SUMMER;
	private GardenEta.Kind goal = GardenEta.Kind.THIEVING_LEVEL;
	private int target;

	private boolean inGarden;
	private int runsBase;
	private int paidBase;

	Object lastState;

	void setStore(Function<String, String> load, BiConsumer<String, String> save)
	{
		this.load = load;
		this.save = save;
	}

	/**
	 * @param season the garden to judge by, or null for the one last run
	 * @param goal the target, or null for none
	 * @param target its level or glasses; 0 for the next level or a banked inventory's 22 glasses
	 */
	void settings(Garden.Season season, GardenEta.Kind goal, int target)
	{
		this.chosen = season;
		this.goal = goal;
		this.target = Math.max(0, target);
	}

	void newAccount()
	{
		log = null;
		runs = null;
		inGarden = false;
		runsBase = 0;
		paidBase = 0;
	}

	static boolean isWatched(int id)
	{
		return Garden.isWatched(id);
	}

	GardenLog log()
	{
		if (log == null)
		{
			log = GardenLog.decode(load == null ? null : load.apply(LOG_KEY), load == null ? null : load.apply(JUICE_KEY));
		}
		return log;
	}

	private GardenRuns runs()
	{
		if (runs == null)
		{
			runs = new GardenRuns();
			runsBase = 0;
			paidBase = 0;
		}
		return runs;
	}

	private static WorldPoint at(Client client)
	{
		return client.getLocalPlayer() == null ? null
			: WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation());
	}

	private static int reach(WorldPoint a, WorldPoint b)
	{
		return a == null || b == null || a.getPlane() != b.getPlane() ? Integer.MAX_VALUE
			: Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
	}

	/** Every tick, wherever you are: runs in the garden, and the juice you crush anywhere. */
	void watch(Client client, Collection<NPC> npcs, SceneIndex scene, long now)
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}
		GardenRuns.Reading r = new GardenRuns.Reading();
		WorldPoint at = at(client);
		Garden.Season nearestTree = null;
		int treeReach = Integer.MAX_VALUE;
		for (Garden.Season s : Garden.Season.values())
		{
			for (TileObject o : scene.get(s.tree()))
			{
				r.inGarden = true;
				int d = reach(at, o.getWorldLocation());
				if (d < treeReach)
				{
					treeReach = d;
					nearestTree = s;
				}
			}
		}
		inGarden = r.inGarden;
		if (at != null)
		{
			r.x = at.getX();
			r.y = at.getY();
			r.plane = at.getPlane();
		}
		r.tree = nearestTree;
		if (r.inGarden)
		{
			for (TileObject o : scene.get(Garden.FOUNTAIN))
			{
				r.byFountain |= reach(at, o.getWorldLocation()) <= FOUNTAIN_REACH;
			}
			int best = Integer.MAX_VALUE;
			for (NPC n : npcs)
			{
				Garden.Season s = Garden.Season.byElemental(n.getId());
				int d = s == null ? Integer.MAX_VALUE : reach(at, n.getWorldLocation());
				// One tile more than the wiki's two squares: the elemental may have moved as it caught you.
				if (d <= Garden.CATCH_REACH + 1 && d < best)
				{
					best = d;
					r.elemental = s;
				}
			}
		}
		Pack pack = Pack.read(client);
		for (Garden.Season s : Garden.Season.values())
		{
			r.sqirks[s.ordinal()] = pack.count(s.sqirk());
			r.juice[s.ordinal()] = pack.count(s.juice());
		}
		r.farming = client.getSkillExperience(Skill.FARMING);
		keep(runs().tick(client.getTickCount(), r, now));
	}

	private void keep(GardenRuns.Result res)
	{
		if (res == null || res.isEmpty())
		{
			return;
		}
		GardenLog l = log();
		if (res.run != null)
		{
			l.add(res.run);
			if (save != null)
			{
				save.accept(LOG_KEY, l.encode());
			}
		}
		boolean juice = false;
		for (Garden.Season s : Garden.Season.values())
		{
			if (res.juice[s.ordinal()] > 0)
			{
				l.addJuice(s, res.juice[s.ordinal()]);
				juice = true;
			}
		}
		if (juice && save != null)
		{
			save.accept(JUICE_KEY, l.encodeJuice());
		}
	}

	/** Logged out or hopped: a trip to the fountain already made is kept. */
	void relogged(Client client, long now, int tick)
	{
		if (runs != null)
		{
			GardenLog.Run r = runs.logout(now);
			if (r != null)
			{
				GardenRuns.Result res = new GardenRuns.Result();
				res.run = r;
				keep(res);
			}
		}
		inGarden = false;
	}

	/** In the Sorceress's Garden. */
	boolean active(Client client)
	{
		return client.getLocalPlayer() != null && inGarden;
	}

	/** The pace coach's count: none. The garden is not timed for you. */
	int units()
	{
		return 0;
	}

	void left()
	{
	}

	/** The garden the figures are for: the one chosen, else the one you last ran, else the best open to you. */
	Garden.Season season(Client client)
	{
		if (chosen != null)
		{
			return chosen;
		}
		Garden.Season last = log().lastSeason();
		return last != null ? last : Garden.Season.bestFor(client.getBoostedSkillLevel(Skill.THIEVING));
	}

	/**
	 * The one plain line: the garden's level when it is above yours, the sq'irks to crush, a pestle
	 * and mortar and beer glasses, else the garden and how your runs have gone. Never when to go.
	 */
	static Step step(Garden.Season s, int thieving, int sqirks, boolean pestle, boolean glass, GardenLog.Pace p)
	{
		if (thieving < s.thieving())
		{
			Garden.Season open = Garden.Season.bestFor(thieving);
			return Step.of("Level", "The " + s.lower() + " garden needs " + s.thieving() + " Thieving",
				"Your Thieving is " + thieving + "; the " + open.lower() + " garden is open to you", Step.Target.NONE);
		}
		if (sqirks >= s.perJuice() && pestle && glass)
		{
			return Step.of("Crush", "Crush your " + s.lower() + " sq'irks",
				s.perJuice() + " fill a beer glass: " + Session.format(s.osmanXp()) + " Thieving xp from Osman", Step.Target.NONE);
		}
		if (!pestle || !glass)
		{
			return Step.of("Bring", !pestle && !glass ? "Bring a pestle and mortar and beer glasses" : !pestle ? "Bring a pestle and mortar" : "Bring beer glasses",
				"Beer glasses are free from the shelves in the Sorceress's house", Step.Target.NONE);
		}
		String detail = p == null || p.runs == 0 ? "Every run is kept, caught or not, and your pace follows from them"
			: percent(p.sqirksPerRun()) + " of your last " + p.runs + (p.runs == 1 ? " run" : " runs") + " paid a sq'irk";
		return Step.of(s.getTitle(), "The " + s.lower() + " garden, at your own pace", detail, Step.Target.NONE);
	}

	static String percent(double share)
	{
		return Math.round(share * 100) + "%";
	}

	Flight fly(Client client, long now)
	{
		int tick = client.getTickCount();
		GardenLog l = log();
		Garden.Season want = season(client);
		int thieving = client.getBoostedSkillLevel(Skill.THIEVING);
		// Until your Thieving opens the garden you want, the figures are for the best one open to you.
		Garden.Season s = thieving >= want.thieving() ? want : Garden.Season.bestFor(thieving);
		GardenLog.Pace p = l.pace(s);
		Pack pack = Pack.read(client);
		Step step = step(want, thieving, pack.count(want.sqirk()), pack.carries(Garden.PESTLE_AND_MORTAR), pack.carries(Garden.BEER_GLASS), l.pace(want));
		lastState = s;
		View view = new View(true, View.Activity.SORCERESS_GARDEN, step, "Sorceress's Garden · the " + s.lower() + " garden", null, -1);
		Flight out = new Flight(View.Activity.SORCERESS_GARDEN, step, view);
		// Panel only: no plaque, nothing lit, no cue.
		out.quiet = true;
		stats(view, l, s, p, tick);
		if (p.runs > 0)
		{
			view.gauge(new View.Gauge(s.getTitle() + " runs that paid", p.sqirks + p.herbs, 0, p.runs,
				p.caught + " caught by an elemental" + (p.herbs > 0 ? " · " + p.herbs + " for herbs" : ""), Theme.GOLD));
		}
		juiceView(view, client, pack, l);
		if (goal != null)
		{
			view.goal(goalCard(client, pack, l, s, p));
		}
		View.PickCard gardens = gardensCard(l, s);
		if (gardens != null)
		{
			view.pick(gardens);
		}
		if (l.count() > 0)
		{
			view.supply(historyCard(l, s));
		}
		return out;
	}

	private void stats(View view, GardenLog l, Garden.Season s, GardenLog.Pace p, int tick)
	{
		int done = runs().runs() - runsBase;
		int paid = runs().paid() - paidBase;
		view.stat(new View.Stat(Integer.toString(done), "Runs", done == 0 ? "This visit" : paid + " paid this visit", Theme.EMBER));
		view.stat(new View.Stat(p.runs == 0 ? "-" : percent(p.successRate()), "Success", p.runs == 0 ? "No runs yet"
			: "Last " + p.runs + " " + s.lower(), Theme.GOLD_BRIGHT));
		double sph = p.sqirksPerHour();
		view.stat(new View.Stat(sph < 0 ? "-" : Session.format(Math.round(sph)), "Sq'irks an hour", p.runs == 0 ? "At your pace"
			: String.format(Locale.ROOT, "%.2f a run", p.sqirksPerRun()), Theme.GOLD));
		double xph = p.thievingXpPerHour();
		view.stat(new View.Stat(xph < 0 ? "-" : Session.format(Math.round(xph)), "Xp an hour", "Thieving, as juice", Theme.TEAL));
		GardenLog.Run last = l.last();
		if (last != null && last.ticks > 0)
		{
			int best = l.bestLap(last.season);
			view.stat(new View.Stat(StormPilot.clock(last.ticks), "Last lap", best > 0 ? "Best " + StormPilot.clock(best) : last.outcome.getTitle(), Theme.TEXT));
		}
	}

	/** The juice and sq'irks you hold, and what Osman gives for them. */
	private void juiceView(View view, Client client, Pack pack, GardenLog l)
	{
		List<View.Line> lines = new ArrayList<>();
		for (Garden.Season x : Garden.Season.values())
		{
			int juice = held(pack, x.juice());
			int fruit = held(pack, x.sqirk());
			long made = l.juice(x);
			if (juice > 0)
			{
				lines.add(new View.Line(x.getTitle() + " sq'irkjuice · " + juice, Session.format((long) juice * x.osmanXp()) + " Thieving xp", View.Line.Mark.DONE));
			}
			if (fruit > 0)
			{
				int whole = fruit / x.perJuice();
				int part = fruit % x.perJuice();
				String trailing = whole > 0 ? whole + (whole == 1 ? " glass" : " glasses") + (part > 0 ? " and " + part + " over" : "")
					: part + " of " + x.perJuice() + " for a glass";
				lines.add(new View.Line(x.getTitle() + " sq'irks · " + fruit, trailing, whole > 0 ? View.Line.Mark.NEXT : View.Line.Mark.TODO));
			}
			if (made > 0 && juice == 0 && fruit == 0)
			{
				lines.add(new View.Line(x.getTitle() + " juice crushed · " + Session.format(made), Session.format(made * x.osmanXp()) + " Thieving xp", View.Line.Mark.IDLE));
			}
		}
		if (!lines.isEmpty())
		{
			boolean bank = pack.banked(Garden.Season.SUMMER.juice()) >= 0;
			view.lines("Juice", bank ? "Pack and bank · Osman's Thieving xp" : "Pack · Osman's Thieving xp", lines);
		}
	}

	/** In the pack and, once seen this session, the bank. */
	private static int held(Pack pack, int id)
	{
		return pack.count(id) + Math.max(0, pack.banked(id));
	}

	/** The level or glasses you chose, and the sq'irks, runs and time to it at your own pace. */
	View.GoalCard goalCard(Client client, Pack pack, GardenLog l, Garden.Season s, GardenLog.Pace p)
	{
		GardenEta.Holding h = new GardenEta.Holding();
		for (Garden.Season x : Garden.Season.values())
		{
			h.juice[x.ordinal()] = held(pack, x.juice());
			h.sqirks[x.ordinal()] = held(pack, x.sqirk());
		}
		Skill skill = goal == GardenEta.Kind.FARMING_LEVEL ? Skill.FARMING : Skill.THIEVING;
		int t = target;
		String title;
		String unit;
		if (goal == GardenEta.Kind.JUICE)
		{
			t = t <= 0 ? Garden.INVENTORY_GLASSES : t;
			title = Session.format(t) + " glasses of " + s.lower() + " juice";
			unit = "glasses";
		}
		else
		{
			h.xp = client.getSkillExperience(skill);
			int level = Goals.levelFor(h.xp);
			t = t <= 0 ? Math.min(99, level + 1) : t;
			title = skill.getName() + " " + t;
			unit = skill.getName() + " xp";
		}
		GardenEta.Answer a = GardenEta.of(goal, t, s, h, p);
		StringBuilder b = new StringBuilder();
		if (a.done())
		{
			b.append(goal == GardenEta.Kind.THIEVING_LEVEL && h.xp < a.need
				? "Your juice covers it: hand it to Osman. Set another under Garden target."
				: "Reached: set another under Garden target.");
		}
		else
		{
			if (goal == GardenEta.Kind.FARMING_LEVEL)
			{
				b.append(exact(a.need - a.have)).append(" Farming xp to go");
			}
			else
			{
				long glasses = (a.sqirks + s.perJuice() - 1) / s.perJuice();
				b.append(exact(a.sqirks)).append(' ').append(s.lower()).append(a.sqirks == 1 ? " sq'irk" : " sq'irks")
					.append(" to go (").append(exact(glasses)).append(glasses == 1 ? " glass)" : " glasses)");
			}
			if (a.hours >= 0)
			{
				b.append(": about ").append(Session.format(a.runs)).append(a.runs == 1 ? " run, " : " runs, ")
					.append(MixologyPilot.clockHours(a.hours)).append(" at your pace.");
			}
			else
			{
				b.append(". Run the " + s.lower() + " garden and the time to it follows from your own pace.");
			}
			if (goal == GardenEta.Kind.THIEVING_LEVEL)
			{
				long worth = GardenEta.heldThievingXp(h);
				if (worth > 0)
				{
					b.append(" Your juice is ").append(exact(worth)).append(" of it.");
				}
			}
		}
		View.GoalCard card = new View.GoalCard(View.Activity.SORCERESS_GARDEN, title, a.have, a.need, unit, b.toString().trim(), null, a.done());
		card.aside(goal == GardenEta.Kind.FARMING_LEVEL ? "Picking pays the Farming xp, " + s.farmingXp() + " a sq'irk"
			: "Osman's Thieving xp, after Prince Ali Rescue");
		return card;
	}

	/** Your gardens, best Thieving xp an hour first: from your own runs only. */
	private static View.PickCard gardensCard(GardenLog l, Garden.Season current)
	{
		List<GardenLog.Pace> rows = new ArrayList<>();
		for (Garden.Season x : Garden.Season.values())
		{
			GardenLog.Pace p = l.pace(x);
			if (p.runs > 0)
			{
				rows.add(p);
			}
		}
		if (rows.size() < 2)
		{
			return null;
		}
		rows.sort((a, b) -> Double.compare(b.thievingXpPerHour(), a.thievingXpPerHour()));
		double top = Math.max(1, rows.get(0).thievingXpPerHour());
		List<View.Pick> picks = new ArrayList<>();
		for (GardenLog.Pace p : rows)
		{
			double xph = p.thievingXpPerHour();
			double sph = p.sqirksPerHour();
			picks.add(new View.Pick(p.season.getTitle(), xph < 0 ? "-" : Session.format(Math.round(xph)) + "/h",
				percent(p.successRate()) + " paid · " + (sph < 0 ? "untimed" : Session.format(Math.round(sph)) + " sq'irks/h"),
				true, p.season == current, Math.max(0, xph) / top, false));
		}
		return new View.PickCard("By garden", rows.size() + " gardens",
			"Thieving xp an hour as juice for Osman, from your own last runs in each garden.", picks);
	}

	/** Your last runs, the best lap, and what the garden has given you. */
	private static View.Supply historyCard(GardenLog l, Garden.Season s)
	{
		List<View.Line> lines = new ArrayList<>();
		List<GardenLog.Run> all = l.all();
		for (int i = all.size() - 1; i >= 0 && i >= all.size() - 5; i--)
		{
			GardenLog.Run r = all.get(i);
			String what = r.season.getTitle() + (r.ticks > 0 ? " · " + StormPilot.clock(r.ticks) : "");
			lines.add(new View.Line(what, r.outcome.getTitle(), r.outcome == GardenLog.Outcome.SQIRK ? View.Line.Mark.DONE
				: r.outcome == GardenLog.Outcome.HERBS ? View.Line.Mark.IDLE : View.Line.Mark.TODO));
		}
		int best = l.bestLap(s);
		String headline = best > 0 ? "Best " + s.lower() + " lap: " + StormPilot.clock(best) : null;
		String detail = Session.format(l.farmingXp()) + " Farming xp from " + Session.format(l.count()) + (l.count() == 1 ? " run" : " runs") + " kept";
		long juiceXp = l.juiceXp();
		if (juiceXp > 0)
		{
			long glasses = 0;
			for (Garden.Season x : Garden.Season.values())
			{
				glasses += l.juice(x);
			}
			detail += "; " + Session.format(glasses) + (glasses == 1 ? " glass" : " glasses") + " crushed, " + Session.format(juiceXp) + " Thieving xp from Osman";
		}
		return new View.Supply(l.count() == 1 ? "1 kept" : Session.format(l.count()) + " kept", lines, headline, detail, false, null).titled("Your runs");
	}

	static String exact(long n)
	{
		return String.format(Locale.ROOT, "%,d", n);
	}

	/** Brief for the panel away from the garden. */
	View.Brief brief()
	{
		GardenLog l = log();
		GardenLog.Run last = l.last();
		String status = last == null ? null : "Last run: " + last.season.lower() + " garden, " + last.outcome.getTitle().toLowerCase(Locale.ROOT)
			+ (last.ticks > 0 ? " in " + StormPilot.clock(last.ticks) : "");
		View.Brief b = new View.Brief(View.Activity.SORCERESS_GARDEN, status,
			"Through the apprentice in the Sorceress's house near Shantay Pass. Bring a pestle and mortar and beer glasses; Osman gives Thieving xp for the juice after Prince Ali Rescue.");
		Garden.Season s = chosen != null ? chosen : l.lastSeason();
		if (s != null)
		{
			GardenLog.Pace p = l.pace(s);
			double sph = p.sqirksPerHour();
			if (p.runs > 0)
			{
				b.best(percent(p.successRate()) + " of " + p.runs + " " + s.lower() + (p.runs == 1 ? " run" : " runs") + " paid"
					+ (sph >= 0 ? ", " + Session.format(Math.round(sph)) + " sq'irks an hour." : "."));
			}
		}
		return b;
	}
}
