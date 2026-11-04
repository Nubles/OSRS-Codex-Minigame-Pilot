package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Goals;
import com.codex.minigamepilot.core.GolemCrafting;
import com.codex.minigamepilot.core.GolemEta;
import com.codex.minigamepilot.core.GolemLog;
import com.codex.minigamepilot.core.GolemTracker;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;

/**
 * Golem crafting on Wyrmscraig: every golem kept per account (its gift, the fur that powered it when
 * seen, the Crafting experience since the golem before and the time since it), your golems and
 * Crafting experience an hour at your own pace, your gifts against the wiki's rates and the Jeweller's
 * chisel's odds, your best session, what your sunstone will make, and the time to the Crafting level,
 * golems or chisel you choose, with each fur's experience a golem.
 *
 * <p>Information only. Nothing is lit or drawn in the game view and no click is timed: the tick a
 * side's experience comes is never cued. A golem's time is measured after it is done.
 */
public final class GolemPilot
{
	static final String LOG_KEY = "golem.kept";
	/** Furs listed on the panel: the best, and the one you use. */
	static final int SHOWN_FURS = 6;

	private Function<String, String> load;
	private BiConsumer<String, String> save;
	private IntUnaryOperator price = id -> 0;
	private GolemLog log;
	private GolemTracker tracker;
	private GolemEta.Kind goal = GolemEta.Kind.CRAFTING_LEVEL;
	private int target;
	/** The fur chosen in the settings, or null for the one you last used. */
	private GolemCrafting.Fur chosenFur;

	private boolean atStations;
	private int visitBase;

	Object lastState;

	void setStore(Function<String, String> load, BiConsumer<String, String> save)
	{
		this.load = load;
		this.save = save;
	}

	void setPrice(IntUnaryOperator price)
	{
		this.price = price == null ? id -> 0 : price;
	}

	/**
	 * @param goal the target, or null for none
	 * @param target its level or golems; 0 for the next level or 100 golems
	 * @param fur the fur to reckon with, or null for the one last used
	 */
	void settings(GolemEta.Kind goal, int target, GolemCrafting.Fur fur)
	{
		this.goal = goal;
		this.target = Math.max(0, target);
		this.chosenFur = fur;
	}

	void newAccount()
	{
		log = null;
		tracker = null;
		atStations = false;
		visitBase = 0;
	}

	static boolean isWatched(int id)
	{
		return GolemCrafting.isWatched(id);
	}

	GolemLog log()
	{
		if (log == null)
		{
			log = GolemLog.decode(load == null ? null : load.apply(LOG_KEY));
		}
		return log;
	}

	private GolemTracker tracker()
	{
		if (tracker == null)
		{
			tracker = new GolemTracker();
			visitBase = 0;
		}
		return tracker;
	}

	/** A game message, tags removed. */
	void chat(String message, int tick)
	{
		GolemCrafting.Gift g = GolemCrafting.gift(message);
		if (g != null)
		{
			tracker().gift(g, tick);
		}
	}

	/** Every tick: the golems whose gift lines have come are kept. */
	void watch(Client client, SceneIndex scene, long now)
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}
		GolemTracker.Reading r = new GolemTracker.Reading();
		r.atStations = !scene.get(GolemCrafting.places()).isEmpty();
		if (r.atStations && !atStations)
		{
			visitBase = tracker().golems();
		}
		atStations = r.atStations;
		Pack pack = Pack.read(client);
		r.crafting = client.getSkillExperience(Skill.CRAFTING);
		r.cores = pack.count(GolemCrafting.SUNSTONE_CORE);
		for (GolemCrafting.Fur f : GolemCrafting.Fur.values())
		{
			r.furs[f.ordinal()] = pack.count(f.item());
		}
		keep(tracker().tick(client.getTickCount(), r, now));
	}

	private void keep(List<GolemLog.Golem> golems)
	{
		if (golems == null || golems.isEmpty())
		{
			return;
		}
		GolemLog l = log();
		for (GolemLog.Golem g : golems)
		{
			l.add(g);
		}
		if (save != null)
		{
			save.accept(LOG_KEY, l.encode());
		}
	}

	/** Logged out or hopped: a golem whose gift line came is kept. */
	void relogged(Client client, long now, int tick)
	{
		if (tracker != null)
		{
			keep(tracker.logout(now));
		}
		atStations = false;
	}

	/** By the golem stations or the sunstone. */
	boolean active(Client client)
	{
		return client.getLocalPlayer() != null && atStations;
	}

	/** The pace coach's count: none. Golem crafting is not timed for you. */
	int units()
	{
		return 0;
	}

	void left()
	{
	}

	/** The fur the figures are reckoned with: the one chosen, else the one you last used, else none. */
	GolemCrafting.Fur fur()
	{
		return chosenFur != null ? chosenFur : log().lastFur();
	}

	/**
	 * The one plain line: the Crafting level when it is below 60, a chisel or hammer when none is seen,
	 * else how your golems have gone. Never when to click.
	 */
	static Step step(int crafting, boolean chisel, boolean hammer, int visit, GolemLog.Pace p)
	{
		if (crafting < GolemCrafting.CRAFTING_LEVEL)
		{
			return Step.of("Level", "Golem crafting needs " + GolemCrafting.CRAFTING_LEVEL + " Crafting",
				"Your Crafting is " + crafting + "; boosts do not count for it", Step.Target.NONE);
		}
		if (!chisel || !hammer)
		{
			return Step.of("Bring", !chisel && !hammer ? "Bring a chisel and a hammer" : !chisel ? "Bring a chisel" : "Bring a hammer",
				"A plinth takes a chisel, a hammer and four sunstones a golem", Step.Target.NONE);
		}
		String detail = p == null || p.golems == 0 ? "Every golem is kept, and your pace follows from them"
			: visit > 0 ? visit + (visit == 1 ? " golem" : " golems") + " this visit, " + Session.format(Math.round(p.golemsPerHour())) + " an hour at your pace"
			: Session.format(Math.round(p.golemsPerHour())) + " golems an hour over your last " + p.golems;
		return Step.of("Golems", "Golem crafting, at your own pace", detail, Step.Target.NONE);
	}

	Flight fly(Client client, long now)
	{
		int tick = client.getTickCount();
		GolemLog l = log();
		GolemLog.Pace p = l.pace();
		Pack pack = Pack.read(client);
		int visit = tracker().golems() - visitBase;
		boolean chisel = pack.carries(GolemCrafting.CHISEL, GolemCrafting.JEWELLERS_CHISEL);
		boolean hammer = pack.carries(GolemCrafting.HAMMERS) || pack.wears(GolemCrafting.HAMMERS);
		Step step = step(client.getRealSkillLevel(Skill.CRAFTING), chisel, hammer, visit, p);
		lastState = step.getTitle();
		View view = new View(true, View.Activity.GOLEM_CRAFTING, step, "Golem crafting · Wyrmscraig", null, -1);
		Flight out = new Flight(View.Activity.GOLEM_CRAFTING, step, view);
		// Panel only: no plaque, nothing lit, no cue.
		out.quiet = true;
		stats(view, l, p, visit, tick);
		sunstoneView(view, pack);
		giftsView(view, l);
		if (goal != null)
		{
			view.goal(goalCard(client, pack, l, p));
		}
		view.pick(fursCard(client, l, p));
		if (l.count() > 0)
		{
			view.supply(historyCard(client, l));
		}
		return out;
	}

	private void stats(View view, GolemLog l, GolemLog.Pace p, int visit, int tick)
	{
		view.stat(new View.Stat(Integer.toString(visit), "Golems", "This visit", Theme.EMBER));
		double gph = p.golemsPerHour();
		view.stat(new View.Stat(gph < 0 ? "-" : Session.format(Math.round(gph)), "Golems an hour",
			p.golems == 0 ? "At your pace" : "Last " + p.golems, Theme.GOLD_BRIGHT));
		double xph = p.xpPerHour();
		view.stat(new View.Stat(xph < 0 ? "-" : Session.format(Math.round(xph)), "Xp an hour", "Crafting", Theme.TEAL));
		double xpg = p.xpPerGolem();
		GolemCrafting.Fur fur = fur();
		// Your own figure once you have one, else the fur's from the wiki's table.
		view.stat(new View.Stat(xpg > 0 ? Session.format(Math.round(xpg)) : fur != null ? Session.format(fur.totalXp()) : "-", "Xp a golem",
			xpg > 0 ? "Your own golems" : fur != null ? fur.shortTitle() : "Base " + Session.format(GolemCrafting.BASE_XP) + " and fur", Theme.GOLD));
		double value = giftValue(l);
		if (value > 0 && gph > 0)
		{
			view.stat(new View.Stat(Session.format(Math.round(value * gph)), "Gifts an hour", "At today's prices", Theme.TEXT));
		}
		GolemLog.Golem last = l.last();
		if (last != null && last.timed())
		{
			int best = l.quickest();
			view.stat(new View.Stat(StormPilot.clock(last.ticks), "Last golem", best > 0 ? "Quickest " + StormPilot.clock(best) : "", Theme.TEXT));
		}
	}

	/** Your own gifts' average worth a golem, at today's prices, or 0. */
	private double giftValue(GolemLog l)
	{
		int[] n = l.gifts();
		long coins = 0;
		for (GolemCrafting.Gift g : GolemCrafting.Gift.values())
		{
			coins += (long) n[g.ordinal()] * Math.max(0, price.applyAsInt(g.item()));
		}
		return l.count() == 0 ? 0 : coins / (double) l.count();
	}

	/** What the sunstone and cores you carry will make: four sunstones and a core a golem. */
	private static void sunstoneView(View view, Pack pack)
	{
		int s = pack.count(GolemCrafting.SUNSTONE);
		int c = pack.count(GolemCrafting.SUNSTONE_CORE);
		if (s == 0 && c == 0)
		{
			return;
		}
		int golems = GolemCrafting.golemsFrom(s, c);
		view.stat(new View.Stat(Integer.toString(golems), "In your pack", c == 0 ? s + " sunstone" : s + " sunstone · " + c + (c == 1 ? " core" : " cores"), Theme.GOLD));
	}

	/** Your gifts against the wiki's rates, and the chisel's odds. */
	private static void giftsView(View view, GolemLog l)
	{
		if (l.count() == 0)
		{
			return;
		}
		int[] n = l.gifts();
		List<View.Line> lines = new ArrayList<>();
		for (GolemCrafting.Gift g : GolemCrafting.Gift.values())
		{
			int got = n[g.ordinal()];
			double expected = l.count() * g.rate();
			if (g == GolemCrafting.Gift.CHISEL)
			{
				String trailing = got > 0 ? "Yours · " + g.odds() + " a golem"
					: g.odds() + " · " + GardenPilot.percent(GolemCrafting.chiselChance(l.count())) + " by now";
				lines.add(new View.Line(g.getTitle() + " · " + got, trailing, got > 0 ? View.Line.Mark.DONE : View.Line.Mark.TODO));
			}
			else
			{
				lines.add(new View.Line(g.getTitle().substring(6, 7).toUpperCase(Locale.ROOT) + g.getTitle().substring(7) + " · " + got,
					g.odds() + " · " + Math.round(expected) + " expected",
					got >= expected ? View.Line.Mark.DONE : View.Line.Mark.IDLE));
			}
		}
		view.lines("Gifts", Session.format(l.count()) + (l.count() == 1 ? " golem kept" : " golems kept") + " · the wiki's rates", lines);
	}

	/** The level, golems or chisel you chose, and the golems, sunstone and time to it at your own pace. */
	View.GoalCard goalCard(Client client, Pack pack, GolemLog l, GolemLog.Pace p)
	{
		long xp = client.getSkillExperience(Skill.CRAFTING);
		int t = target;
		String title;
		String unit;
		boolean owned = l.hadChisel() || pack.carries(GolemCrafting.JEWELLERS_CHISEL) || pack.banked(GolemCrafting.JEWELLERS_CHISEL) > 0;
		switch (goal)
		{
			case GOLEMS:
				t = t <= 0 ? 100 : t;
				title = Session.format(t) + " golems";
				unit = "golems";
				break;
			case JEWELLERS_CHISEL:
				title = "Jeweller's chisel";
				unit = "golems of 300";
				break;
			case CRAFTING_LEVEL:
			default:
				t = t <= 0 ? Math.min(99, Goals.levelFor(xp) + 1) : t;
				title = "Crafting " + t;
				unit = "Crafting xp";
				break;
		}
		long held = pack.count(GolemCrafting.SUNSTONE) + pack.count(GolemCrafting.SUNSTONE_CORE);
		GolemCrafting.Fur fur = fur();
		GolemEta.Answer a = GolemEta.of(goal, t, xp, l.count(), owned, fur, held, p);
		StringBuilder b = new StringBuilder();
		if (a.done())
		{
			b.append(goal == GolemEta.Kind.JEWELLERS_CHISEL ? "Yours: set another under Golem target." : "Reached: set another under Golem target.");
		}
		else
		{
			if (goal == GolemEta.Kind.CRAFTING_LEVEL)
			{
				b.append(GardenPilot.exact(a.need - a.have)).append(" Crafting xp to go: about ");
			}
			else if (goal == GolemEta.Kind.JEWELLERS_CHISEL)
			{
				b.append(GardenPilot.percent(a.chance)).append(" chance of it by now; the average 300 is ");
			}
			b.append(GardenPilot.exact(a.golems)).append(a.golems == 1 ? " golem" : " golems");
			if (a.sunstone > 0)
			{
				b.append(" (").append(GardenPilot.exact(a.sunstone)).append(" sunstone to mine)");
			}
			if (a.hours >= 0)
			{
				b.append(", ").append(MixologyPilot.clockHours(a.hours)).append(" at your pace.");
			}
			else
			{
				b.append(". Craft a few and the time to it follows from your own pace.");
			}
			if (goal == GolemEta.Kind.JEWELLERS_CHISEL)
			{
				b.append(" Even odds at ").append(GardenPilot.exact(GolemCrafting.golemsForChisel(0.5))).append(" golems.");
			}
		}
		View.GoalCard card = new View.GoalCard(View.Activity.GOLEM_CRAFTING, title, a.have, a.need, unit, b.toString().trim(), null, a.done());
		boolean mine = p.xpPerGolem() > 0;
		card.aside(goal == GolemEta.Kind.CRAFTING_LEVEL ? "At " + Session.format(Math.round(a.xpPerGolem)) + " xp a golem, "
			+ (mine ? "your own" : fur != null ? fur.shortTitle() + " fur's" : "the base without fur") : "A golem: four sunstones, a core and a fur");
		return card;
	}

	/** Each fur's Crafting experience a golem, best first, with the golems to your next level by it. */
	private View.PickCard fursCard(Client client, GolemLog l, GolemLog.Pace p)
	{
		long xp = client.getSkillExperience(Skill.CRAFTING);
		int level = Goals.levelFor(xp);
		long left = level >= 99 ? 0 : Goals.xpFor(level + 1) - xp;
		GolemCrafting.Fur current = fur();
		int hunter = client.getRealSkillLevel(Skill.HUNTER);
		double top = GolemCrafting.Fur.MOONLIGHT_ANTELOPE.totalXp();
		List<View.Pick> picks = new ArrayList<>();
		int shown = 0;
		for (GolemCrafting.Fur f : GolemCrafting.Fur.values())
		{
			// The best furs, and the one you use wherever it stands.
			if (shown >= SHOWN_FURS && f != current)
			{
				continue;
			}
			shown++;
			long golems = (left + f.totalXp() - 1) / f.totalXp();
			String detail = (left > 0 ? Session.format(golems) + (golems == 1 ? " golem to " : " golems to ") + (level + 1) : "Fur xp " + f.furXp())
				+ (hunter >= f.hunter() ? "" : " · Hunter " + f.hunter());
			picks.add(new View.Pick(f.shortTitle(), Session.format(f.totalXp()) + " xp", detail, true, f == current, f.totalXp() / top, false));
		}
		return new View.PickCard("By fur", current == null ? "14 furs" : current.shortTitle(),
			"Crafting xp a golem: the base 1,560 and the fur's, down to Wyrmscraig goat's 2,440. Tatty furs will not do.", picks);
	}

	/** Your last sessions, your record, and what golem crafting has given you. */
	private static View.Supply historyCard(Client client, GolemLog l)
	{
		List<View.Line> lines = new ArrayList<>();
		List<GolemLog.Sitting> all = l.sittings();
		GolemLog.Sitting best = l.bestSitting();
		for (int i = all.size() - 1; i >= 0 && i >= all.size() - 5; i--)
		{
			GolemLog.Sitting s = all.get(i);
			double xph = s.xpPerHour();
			lines.add(new View.Line(s.golems + (s.golems == 1 ? " golem" : " golems") + (s.ticks > 0 ? " · " + clockTicks(s.ticks) : ""),
				xph > 0 ? Session.format(Math.round(xph)) + " xp/h" : "-", s == best ? View.Line.Mark.DONE : View.Line.Mark.IDLE));
		}
		String headline = best == null ? null : "Best session: " + Session.format(Math.round(best.golemsPerHour())) + " golems an hour over " + best.golems;
		StringBuilder d = new StringBuilder();
		d.append(Session.format(l.count())).append(l.count() == 1 ? " golem kept, " : " golems kept, ")
			.append(Session.format(l.xp())).append(" Crafting xp seen");
		int quickest = l.quickest();
		if (quickest > 0)
		{
			d.append("; quickest golem ").append(StormPilot.clock(quickest));
		}
		d.append("; most in a session ").append(l.longestSitting());
		int count = client.getVarbitValue(VarbitID.GOLEM_CRAFTING_COUNT);
		if (count > 0)
		{
			d.append("; the game counts ").append(GardenPilot.exact(count));
		}
		return new View.Supply(all.size() == 1 ? "1 session" : Session.format(all.size()) + " sessions", lines, headline, d.toString(), false, null)
			.titled("Your golems");
	}

	/** "1h 04m" or "12m" for ticks. */
	static String clockTicks(long ticks)
	{
		return MixologyPilot.clockHours(ticks * 0.6 / 3600.0);
	}

	/** Brief for the panel away from Wyrmscraig. */
	View.Brief brief()
	{
		GolemLog l = log();
		GolemLog.Pace p = l.pace();
		String status = l.count() == 0 ? null : Session.format(l.count()) + (l.count() == 1 ? " golem kept" : " golems kept")
			+ (l.hadChisel() ? ", Jeweller's chisel yours" : "");
		View.Brief b = new View.Brief(View.Activity.GOLEM_CRAFTING, status,
			"On Wyrmscraig after Fallen From Grace, at 60 Crafting. Bring a chisel and a hammer; mine sunstone, chisel cores, and power each golem with a fur.");
		double xph = p.xpPerHour();
		if (p.golems > 0)
		{
			b.best(Session.format(Math.round(p.golemsPerHour())) + " golems an hour" + (xph > 0 ? ", " + Session.format(Math.round(xph)) + " Crafting xp an hour." : "."));
		}
		return b;
	}
}
