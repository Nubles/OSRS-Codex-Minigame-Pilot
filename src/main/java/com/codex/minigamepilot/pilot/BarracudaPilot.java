package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Barracuda;
import com.codex.minigamepilot.core.BarracudaLog;
import com.codex.minigamepilot.core.BarracudaRuns;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;

/**
 * The Barracuda Trials, Sailing's timed courses: every run kept per account with a split at each
 * objective the trial's own interface counts, its time and the Sailing experience it gave; the
 * ghost of your quickest run of each course (a trial at a rank) to race; your experience an hour
 * by trial; the next rank's time to beat and what it gives; and the next trial or rank worth
 * doing.
 *
 * <p>No route is painted. The wiki gives no course in words, so at most one point is marked in a
 * run: where your own ghost passed its next objective, as a single tile and a ring on the
 * minimap, and only with the setting on. It reads the game only.
 */
public final class BarracudaPilot
{
	static final String LOG_KEY = "barracuda.kept";
	static final String RANKS_KEY = "barracuda.ranks";
	/** At a trial: within this many tiles of its trial master (or Ros). */
	static final int LOBBY_REACH = 24;
	/** The trial a master was last seen beside is remembered this long (ticks), for a start the game does not name. */
	static final int MASTER_MEMORY = 200;

	private Function<String, String> load;
	private BiConsumer<String, String> save;
	private BarracudaLog log;
	private String ranks;
	private boolean waypoint = true;

	private BarracudaRuns runs;
	private int runsBase;
	private long sessionXp;

	// Read each tick.
	private Barracuda.Trial here;
	private Barracuda.Trial lastMaster;
	private int lastMasterTick = -1;
	private Barracuda.Trial announced;
	private boolean completedChat;
	private int completedBase = -1;
	private Barracuda.Trial completedTrial;
	private int x;
	private int y;
	private int plane;

	Object lastState;

	void setStore(Function<String, String> load, BiConsumer<String, String> save)
	{
		this.load = load;
		this.save = save;
	}

	/** @param waypoint mark where your ghost passed its next objective */
	void settings(boolean waypoint)
	{
		this.waypoint = waypoint;
	}

	void newAccount()
	{
		log = null;
		ranks = null;
		runs = null;
		runsBase = 0;
		sessionXp = 0;
		here = null;
		lastMaster = null;
		lastMasterTick = -1;
		announced = null;
		completedChat = false;
		completedBase = -1;
		completedTrial = null;
	}

	BarracudaLog log()
	{
		if (log == null)
		{
			log = BarracudaLog.decode(load == null ? null : load.apply(LOG_KEY));
		}
		return log;
	}

	private BarracudaRuns runs()
	{
		if (runs == null)
		{
			runs = new BarracudaRuns();
			runsBase = 0;
		}
		return runs;
	}

	/** A game message: a trial's start or its completion. */
	void chat(String message)
	{
		if (message == null)
		{
			return;
		}
		String m = message.replaceAll("<[^>]*>", "");
		if (m.contains(Barracuda.STARTING))
		{
			announced = Barracuda.Trial.named(m);
		}
		else if (m.contains(Barracuda.COMPLETED))
		{
			completedChat = true;
		}
	}

	/** Every tick, wherever you are: so a run is kept even as you sail away or log out. */
	void watch(Client client, Collection<NPC> npcs, long now)
	{
		int tick = client.getTickCount();
		readPlace(client);
		here = null;
		for (NPC n : npcs)
		{
			Barracuda.Trial t = trialOf(n.getId());
			WorldPoint w = t == null ? null : n.getWorldLocation();
			if (w != null && Math.abs(w.getX() - x) <= LOBBY_REACH && Math.abs(w.getY() - y) <= LOBBY_REACH)
			{
				here = t;
				lastMaster = t;
				lastMasterTick = tick;
				break;
			}
		}
		learnRanks(client);
		BarracudaRuns r = runs();
		BarracudaRuns.Reading reading = read(client, tick);
		if (!r.open() && !reading.inTrial)
		{
			completedChat = false;
			return;
		}
		BarracudaLog.Run done = r.tick(tick, reading, now);
		if (r.open() && completedBase < 0 && r.trial() != null)
		{
			completedTrial = r.trial();
			completedBase = client.getVarpValue(r.trial().completedVarp());
		}
		completedChat = false;
		keep(done);
		if (!r.open())
		{
			completedBase = -1;
			completedTrial = null;
			announced = null;
		}
	}

	private static Barracuda.Trial trialOf(int npcId)
	{
		for (Barracuda.Trial t : Barracuda.Trial.values())
		{
			if (t.isMaster(npcId))
			{
				return t;
			}
		}
		return null;
	}

	/** Where you are in the world: your skiff's place when aboard, else your own. */
	private void readPlace(Client client)
	{
		x = 0;
		y = 0;
		Player me = client.getLocalPlayer();
		WorldView top = client.getTopLevelWorldView();
		if (me == null || top == null)
		{
			return;
		}
		plane = top.getPlane();
		WorldView mine = me.getWorldView();
		LocalPoint lp = me.getLocalLocation();
		if (mine != null && !mine.isTopLevel())
		{
			lp = null;
			for (WorldEntity e : top.worldEntities())
			{
				if (e != null && e.getWorldView() != null && e.getWorldView().getId() == mine.getId())
				{
					lp = e.getLocalLocation();
					break;
				}
			}
		}
		if (lp == null)
		{
			return;
		}
		WorldPoint w = WorldPoint.fromLocalInstance(client, lp);
		if (w != null)
		{
			x = w.getX();
			y = w.getY();
		}
	}

	private BarracudaRuns.Reading read(Client client, int tick)
	{
		BarracudaRuns.Reading r = new BarracudaRuns.Reading();
		Barracuda.Trial byState = null;
		for (Barracuda.Trial t : Barracuda.Trial.values())
		{
			if (client.getVarbitValue(t.masterStateVarbit()) == Barracuda.MASTER_STATE_IN_TRIAL)
			{
				byState = t;
			}
		}
		Widget clock = client.getWidget(Barracuda.HUD_TIME);
		boolean hud = clock != null && !clock.isHidden();
		r.inTrial = client.getVarbitValue(Barracuda.IN_TRIAL) > 0 || byState != null || hud;
		if (!r.inTrial)
		{
			return r;
		}
		Barracuda.Trial t = byState;
		if (t == null)
		{
			t = Barracuda.Trial.named(text(client, Barracuda.HUD_TITLE));
		}
		if (t == null)
		{
			t = announced;
		}
		if (t == null && lastMaster != null && tick - lastMasterTick <= MASTER_MEMORY)
		{
			t = lastMaster;
		}
		r.trial = t;
		if (hud)
		{
			r.clock = Barracuda.ticks(clock.getText());
		}
		int target = Barracuda.ticks(text(client, Barracuda.HUD_RANK_TIME));
		if (t != null && target > 0)
		{
			r.rank = t.rankFor(target);
		}
		int[] p = Barracuda.progress(text(client, Barracuda.HUD_PROGRESS));
		int[] o = Barracuda.progress(text(client, Barracuda.HUD_OPTIONAL));
		if (p[0] >= 0 || o[0] >= 0)
		{
			r.done = Math.max(0, p[0]) + Math.max(0, o[0]);
			r.of = Math.max(0, p[1]) + Math.max(0, o[1]);
		}
		r.xp = client.getSkillExperience(Skill.SAILING);
		r.x = x;
		r.y = y;
		boolean byCount = completedBase >= 0 && completedTrial != null && client.getVarpValue(completedTrial.completedVarp()) > completedBase;
		r.completed = completedChat || byCount;
		return r;
	}

	private static String text(Client client, int id)
	{
		Widget w = client.getWidget(id);
		return w == null || w.isHidden() ? null : w.getText();
	}

	private void keep(BarracudaLog.Run run)
	{
		if (run == null || run.trial == null)
		{
			return;
		}
		// A run cut off in its first seconds, with nothing done, is a false start: not kept.
		if (run.end != BarracudaLog.End.FINISHED && run.splits().isEmpty() && run.ticks < 10 && run.xp == 0)
		{
			return;
		}
		log().add(run);
		sessionXp += run.xp;
		if (run.beat())
		{
			markRank(run.trial, run.rank);
		}
		if (save != null)
		{
			save.accept(LOG_KEY, log().encode());
		}
	}

	/** The ranks you hold, from the game's first-completion flags as they are seen, kept for when they are not. */
	private void learnRanks(Client client)
	{
		for (Barracuda.Trial t : Barracuda.Trial.values())
		{
			for (Barracuda.Rank k : Barracuda.Rank.values())
			{
				if (client.getVarbitValue(t.firstVarbit(k)) > 0)
				{
					markRank(t, k);
				}
			}
		}
	}

	private void markRank(Barracuda.Trial t, Barracuda.Rank k)
	{
		String code = "" + t.code() + k.code();
		String r = ranks();
		if (!r.contains(code))
		{
			ranks = r + code;
			if (save != null)
			{
				save.accept(RANKS_KEY, "1;" + ranks);
			}
		}
	}

	String ranks()
	{
		if (ranks == null)
		{
			ranks = decodeRanks(load == null ? null : load.apply(RANKS_KEY));
		}
		return ranks;
	}

	/** "1;tsjk": pairs of a trial's and a rank's letters. Anything else is left out. */
	static String decodeRanks(String text)
	{
		if (text == null || !text.startsWith("1;"))
		{
			return "";
		}
		String s = text.substring(2);
		StringBuilder b = new StringBuilder();
		for (int i = 0; i + 1 < s.length(); i += 2)
		{
			Barracuda.Trial t = Barracuda.Trial.byCode(s.charAt(i));
			Barracuda.Rank k = Barracuda.Rank.byCode(s.charAt(i + 1));
			String code = s.substring(i, i + 2);
			if (t != null && k != null && b.indexOf(code) < 0)
			{
				b.append(code);
			}
		}
		return b.toString();
	}

	boolean holds(Barracuda.Trial t, Barracuda.Rank k)
	{
		return ranks().contains("" + t.code() + k.code()) || log().beaten(t, k);
	}

	/** The lowest rank of a trial not yet yours (ranks "must be earned in order"), or null when all are. */
	Barracuda.Rank nextRank(Barracuda.Trial t)
	{
		for (Barracuda.Rank k : Barracuda.Rank.values())
		{
			if (!holds(t, k))
			{
				return k;
			}
		}
		return null;
	}

	/** Logged out or hopped: a run under way ends. */
	void relogged(Client client, long now, int tick)
	{
		if (runs != null)
		{
			keep(runs.logout(tick, client.getSkillExperience(Skill.SAILING), now));
		}
		completedBase = -1;
		completedTrial = null;
		announced = null;
	}

	/** In a run, or beside a trial master. */
	boolean active(Client client)
	{
		return client.getLocalPlayer() != null && (runs().open() || here != null);
	}

	/** The pace coach's count: none, as each run is timed here against its own ghost. */
	int units()
	{
		return 0;
	}

	void left()
	{
	}

	Flight fly(Client client, Collection<NPC> npcs, long now)
	{
		int tick = client.getTickCount();
		BarracudaRuns r = runs();
		BarracudaLog l = log();
		int level = client.getRealSkillLevel(Skill.SAILING);
		Flight out;
		if (r.open())
		{
			out = runFlight(client, r, l, tick);
		}
		else
		{
			Barracuda.Trial t = here != null ? here : lastMaster;
			Step step = lobbyStep(t, level, l);
			lastState = t;
			View view = new View(true, View.Activity.BARRACUDA_TRIALS, step, t == null ? "Barracuda Trials" : t.getTitle() + " · " + t.place(), null, -1);
			out = new Flight(View.Activity.BARRACUDA_TRIALS, step, view);
			if (step.getTarget() == Step.Target.TRIAL_MASTER && t != null)
			{
				for (NPC n : npcs)
				{
					if (t.isMaster(n.getId()))
					{
						out.npc = n;
						break;
					}
				}
			}
			if (t != null)
			{
				view.lines("Ranks", t.getTitle() + " · " + t.level() + " Sailing", rankLines(t, l));
			}
			View.PaceCard pace = lastRunCard(l, t);
			if (pace != null)
			{
				view.pace(pace);
			}
			view.pick(trialsCard(level, l, t));
		}
		stats(out.view, l, r, tick);
		if (l.count() > 0)
		{
			out.view.supply(historyCard(l, r.open() ? r.trial() : here));
		}
		return out;
	}

	/** In a run: the clock, the ghost, the rank's time, and at most one point ahead. */
	private Flight runFlight(Client client, BarracudaRuns r, BarracudaLog l, int tick)
	{
		Barracuda.Trial t = r.trial();
		Barracuda.Rank k = r.rank();
		int now = r.ticks(tick);
		BarracudaLog.Run ghost = t == null || k == null ? null : l.ghost(t, k);
		List<BarracudaLog.Split> mine = r.splits();
		int n = mine.size();
		String title;
		String detail;
		if (r.finished())
		{
			title = "Finished in " + Barracuda.clock(now);
			detail = k == null ? "Run kept." : now <= t.targetTicks(k) ? "Inside " + Barracuda.target(t.targetTicks(k)) + ": the " + k.getTitle() + " rank's time is beaten."
				: Barracuda.seconds(now - t.targetTicks(k)) + " over the " + k.getTitle() + " rank's " + Barracuda.target(t.targetTicks(k)) + ".";
		}
		else
		{
			int of = r.of();
			title = of > 0 ? "Objective " + Math.min(n + 1, of) + " of " + of : n == 0 ? "Under way" : n + (n == 1 ? " objective done" : " objectives done");
			int live = liveGap(mine, ghost, now);
			if (ghost == null)
			{
				detail = k == null ? "Your splits are kept; the rank is read from the trial's own time." : "Your first " + k.getTitle() + " run here: it becomes the ghost to race.";
			}
			else if (n < ghost.splits().size())
			{
				detail = "Your best was at objective " + (n + 1) + " by " + Barracuda.clock(ghost.splits().get(n).tick) + "; " + Barracuda.gap(live) + ".";
			}
			else
			{
				detail = "Past every split of your best run, which finished in " + Barracuda.clock(ghost.ticks) + ".";
			}
		}
		Step step = Step.of("Sail", title, detail, Step.Target.NONE);
		lastState = "run";
		View view = new View(true, View.Activity.BARRACUDA_TRIALS, step, (t == null ? "Barracuda Trial" : t.getTitle()) + " · " + (k == null ? "in a run" : k.getTitle()), null, -1);
		Flight out = new Flight(View.Activity.BARRACUDA_TRIALS, step, view);
		// The panel and one point carry it; no plaque over the course.
		out.quiet = true;
		if (waypoint && !r.finished() && ghost != null && n < ghost.splits().size())
		{
			BarracudaLog.Split next = ghost.splits().get(n);
			if (next.placed())
			{
				out.route = Collections.singletonList(new WorldPoint(next.x, next.y, plane));
			}
		}
		if (t != null && k != null)
		{
			int target = t.targetTicks(k);
			// In whole seconds, as the trial's own clock counts.
			view.gauge(new View.Gauge(k.getTitle() + " " + Barracuda.target(target), Math.min(now, target) * 6 / 10, 0, target * 6 / 10,
				now <= target ? Barracuda.seconds(target - now) + " left on the rank's time" : Barracuda.seconds(now - target) + " over the rank's time",
				now <= target ? Theme.GOLD : Theme.EMBER));
		}
		view.lines("Splits", ghost == null ? "This run" : "Against your best, " + Barracuda.clock(ghost.ticks), splitLines(mine, ghost, now, r.finished()));
		return out;
	}

	/**
	 * The gap to the ghost now, in ticks (positive is slower): at the last objective done, or, once
	 * the clock passes the ghost's time for the next one, at least that far behind.
	 */
	static int liveGap(List<BarracudaLog.Split> mine, BarracudaLog.Run ghost, int now)
	{
		if (ghost == null)
		{
			return 0;
		}
		List<BarracudaLog.Split> g = ghost.splits();
		int n = mine.size();
		int gap = n == 0 || n > g.size() ? 0 : mine.get(n - 1).tick - g.get(n - 1).tick;
		if (n < g.size() && now > g.get(n).tick)
		{
			gap = Math.max(gap, now - g.get(n).tick);
		}
		return gap;
	}

	/** The splits around where you are: the last few done against the ghost, the one under way, and the next of the ghost's. */
	static List<View.Line> splitLines(List<BarracudaLog.Split> mine, BarracudaLog.Run ghost, int now, boolean finished)
	{
		List<View.Line> out = new ArrayList<>();
		List<BarracudaLog.Split> g = ghost == null ? Collections.emptyList() : ghost.splits();
		int n = mine.size();
		int from = Math.max(0, n - 4);
		for (int i = from; i < n; i++)
		{
			int t = mine.get(i).tick;
			String trailing = i < g.size() ? Barracuda.gap(t - g.get(i).tick) : "";
			boolean ahead = i < g.size() && t <= g.get(i).tick;
			out.add(new View.Line("#" + (i + 1) + "  " + Barracuda.clock(t), trailing, ahead ? View.Line.Mark.DONE : View.Line.Mark.IDLE));
		}
		if (!finished)
		{
			if (n < g.size())
			{
				out.add(new View.Line("#" + (n + 1) + "  " + Barracuda.clock(now), "best " + Barracuda.clock(g.get(n).tick), View.Line.Mark.NEXT));
				if (n + 1 < g.size())
				{
					out.add(new View.Line("#" + (n + 2), "best " + Barracuda.clock(g.get(n + 1).tick), View.Line.Mark.TODO));
				}
			}
			else
			{
				out.add(new View.Line(n == 0 ? "First objective" : "#" + (n + 1), Barracuda.clock(now), View.Line.Mark.NEXT));
			}
		}
		return out;
	}

	/** At a trial: the next rank to beat here, else the next trial worth sailing to, else experience. */
	Step lobbyStep(Barracuda.Trial t, int level, BarracudaLog l)
	{
		if (t == null)
		{
			return Step.of("Sail", "To a trial master", "Each trial is started with its trial master.", Step.Target.NONE);
		}
		if (level > 0 && level < t.level())
		{
			return Step.of("Train", "Sailing to " + t.level(), t.getTitle() + " needs " + t.level() + " Sailing"
				+ (t.quest() == null ? "" : " and " + t.quest()) + ", and the level is not boostable.", Step.Target.NONE);
		}
		Barracuda.Rank k = nextRank(t);
		if (k != null)
		{
			return Step.of("Talk-to", "Beat " + Barracuda.target(t.targetTicks(k)) + " for " + k.getTitle(), rankWhy(t, k, l), Step.Target.TRIAL_MASTER);
		}
		Barracuda.Trial other = nextTrial(level, t);
		if (other != null)
		{
			Barracuda.Rank ok = nextRank(other);
			return Step.of("Sail", "On to " + other.inSentence(), "All three ranks here are yours. Next: " + ok.getTitle() + " at "
				+ other.inSentence() + ", " + Barracuda.target(other.targetTicks(ok)) + " for the " + lowerFirst(other.reward(ok)) + bonus(other, ok) + ".", Step.Target.NONE);
		}
		double h = l.xpPerHour(t);
		return Step.of("Talk-to", "Run for experience", "Every rank you can reach is yours" + (h > 0 ? "; here you make " + Session.format(Math.round(h)) + " Sailing xp an hour in the run." : "."),
			Step.Target.TRIAL_MASTER);
	}

	/** The first other trial, in level order, open to you with a rank not yet yours; or null. */
	Barracuda.Trial nextTrial(int level, Barracuda.Trial except)
	{
		for (Barracuda.Trial o : Barracuda.Trial.values())
		{
			if (o != except && (level <= 0 || level >= o.level()) && nextRank(o) != null)
			{
				return o;
			}
		}
		return null;
	}

	private String rankWhy(Barracuda.Trial t, Barracuda.Rank k, BarracudaLog l)
	{
		StringBuilder b = new StringBuilder("The ").append(lowerFirst(t.reward(k))).append(bonus(t, k)).append(" once. ");
		BarracudaLog.Run g = l.ghost(t, k);
		if (g != null)
		{
			int over = g.ticks - t.targetTicks(k);
			b.append("Your best ").append(k.getTitle()).append(" run: ").append(Barracuda.clock(g.ticks))
				.append(over > 0 ? ", " + Barracuda.seconds(over) + " over." : ".");
		}
		else
		{
			b.append("No ").append(k.getTitle()).append(" run kept yet; your first becomes the ghost to race.");
		}
		return b.toString();
	}

	private static String bonus(Barracuda.Trial t, Barracuda.Rank k)
	{
		int x = t.bonusXp(k);
		return x > 0 ? " and " + Session.format(x) + " bonus Sailing xp" : "";
	}

	private static String lowerFirst(String s)
	{
		// A name's own capital stays ("Ralph's fabric roll"); an item's first word drops it ("stormy key").
		String first = s.split(" ", 2)[0];
		return s.isEmpty() || first.endsWith("'s") ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
	}

	/** Each rank of a trial: its time, what it gives, and whether it is yours. */
	private List<View.Line> rankLines(Barracuda.Trial t, BarracudaLog l)
	{
		List<View.Line> out = new ArrayList<>();
		Barracuda.Rank next = nextRank(t);
		Map<Barracuda.Rank, Integer> bests = l.bests(t);
		for (Barracuda.Rank k : Barracuda.Rank.values())
		{
			int best = bests.get(k);
			String text = k.getTitle() + "  " + Barracuda.target(t.targetTicks(k));
			String trailing = best > 0 ? "best " + Barracuda.clock(best) : t.reward(k);
			out.add(new View.Line(text, trailing, holds(t, k) ? View.Line.Mark.DONE : k == next ? View.Line.Mark.NEXT : View.Line.Mark.TODO));
		}
		return out;
	}

	/** The last run here against its course's best, with its splits in up to five stretches. */
	private static View.PaceCard lastRunCard(BarracudaLog l, Barracuda.Trial t)
	{
		BarracudaLog.Run last = null;
		for (BarracudaLog.Run r : l.all())
		{
			if (t == null || r.trial == t)
			{
				last = r;
			}
		}
		if (last == null || !last.finished())
		{
			return null;
		}
		BarracudaLog.Run ghost = last.rank == null ? null : l.ghost(last.trial, last.rank);
		BarracudaLog.Run before = null;
		if (last.rank != null)
		{
			for (BarracudaLog.Run r : l.all())
			{
				if (r != last && r.trial == last.trial && r.rank == last.rank && r.finished() && r.endedAt <= last.endedAt
					&& (before == null || r.ticks < before.ticks))
				{
					before = r;
				}
			}
		}
		boolean best = ghost == last;
		int against = before == null ? 0 : last.ticks - before.ticks;
		String advice;
		if (last.rank == null)
		{
			advice = "Finished in " + Barracuda.clock(last.ticks) + "; the rank was not read, so no ghost is set.";
		}
		else if (last.beat())
		{
			advice = "Inside the " + last.rank.getTitle() + " rank's " + Barracuda.target(last.trial.targetTicks(last.rank)) + (best && before != null ? ", and your new best." : ".");
		}
		else
		{
			advice = Barracuda.seconds(last.ticks - last.trial.targetTicks(last.rank)) + " over the " + last.rank.getTitle() + " rank's "
				+ Barracuda.target(last.trial.targetTicks(last.rank)) + (best && before != null ? ", but your new best." : ".");
		}
		int of = l.runs(last.trial);
		View.PaceCard card = new View.PaceCard(last.ticks, ghost == null ? 0 : ghost.ticks, best && before != null, against, advice,
			!last.beat() && last.rank != null, of + (of == 1 ? " run" : " runs") + " of " + last.trial.inSentence() + " kept");
		if (ghost != null && ghost != last)
		{
			List<View.Split> stretches = stretches(last, ghost);
			if (stretches.size() > 1)
			{
				card.splits(stretches, slowNote(stretches));
			}
		}
		return card;
	}

	/** A run cut into up to five stretches of objectives, each against the ghost's same stretch. */
	static List<View.Split> stretches(BarracudaLog.Run run, BarracudaLog.Run ghost)
	{
		List<View.Split> out = new ArrayList<>();
		List<BarracudaLog.Split> a = run.splits();
		List<BarracudaLog.Split> g = ghost.splits();
		int n = Math.min(a.size(), g.size());
		if (n == 0)
		{
			return out;
		}
		int parts = Math.min(5, n);
		int prevA = 0;
		int prevG = 0;
		int slow = -1;
		int worst = 0;
		List<int[]> raw = new ArrayList<>();
		for (int p = 0; p < parts; p++)
		{
			int end = (p + 1) * n / parts - 1;
			int ta = a.get(end).tick - prevA;
			int tg = g.get(end).tick - prevG;
			raw.add(new int[]{(p * n / parts) + 1, end + 1, ta, tg});
			if (ta - tg > worst)
			{
				worst = ta - tg;
				slow = p;
			}
			prevA = a.get(end).tick;
			prevG = g.get(end).tick;
		}
		for (int p = 0; p < raw.size(); p++)
		{
			int[] s = raw.get(p);
			String name = s[0] == s[1] ? "#" + s[0] : "#" + s[0] + "-" + s[1];
			out.add(new View.Split(name, Math.max(1, s[2]), Math.max(1, s[3]), p == slow));
		}
		return out;
	}

	/** "Objectives #5-8 took 2.4s more than your best", or null when no stretch lost a second. */
	static String slowNote(List<View.Split> stretches)
	{
		View.Split slow = null;
		for (View.Split s : stretches)
		{
			if (s.isSlowest())
			{
				slow = s;
			}
		}
		if (slow == null || slow.ticks() - slow.best() < 2)
		{
			return null;
		}
		return "Objectives " + slow.name() + " took " + Barracuda.seconds(slow.ticks() - slow.best()) + " more than your best.";
	}

	/** Each trial: your experience an hour there, and its next rank or that all are yours. */
	private View.PickCard trialsCard(int level, BarracudaLog l, Barracuda.Trial at)
	{
		List<View.Pick> picks = new ArrayList<>();
		double top = 1;
		Map<Barracuda.Trial, Double> rates = new EnumMap<>(Barracuda.Trial.class);
		for (Barracuda.Trial t : Barracuda.Trial.values())
		{
			double s = l.xpPerHourSitting(t);
			double h = s > 0 ? s : l.xpPerHour(t);
			rates.put(t, h);
			top = Math.max(top, h);
		}
		for (Barracuda.Trial t : Barracuda.Trial.values())
		{
			boolean open = level <= 0 || level >= t.level();
			Barracuda.Rank k = nextRank(t);
			double h = rates.get(t);
			String figure = h > 0 ? Session.format(Math.round(h)) + "/h" : open ? "-" : t.level() + " Sailing";
			String detail;
			if (!open)
			{
				detail = "Needs " + t.level() + " Sailing" + (t.quest() == null ? "" : " and " + t.quest());
			}
			else if (k == null)
			{
				detail = "All three ranks yours";
			}
			else
			{
				BarracudaLog.Run g = l.ghost(t, k);
				detail = "Next " + k.getTitle() + " in " + Barracuda.target(t.targetTicks(k)) + (g == null ? "" : " · best " + Barracuda.clock(g.ticks));
			}
			picks.add(new View.Pick(t.shortName(), figure, detail, open, t == at, h > 0 ? h / top : 0, false));
		}
		Barracuda.Trial next = at != null && nextRank(at) != null ? at : nextTrial(level, null);
		String note = next == null ? "Every rank you can reach is yours: these are your Sailing experience an hour in each trial, from your own runs."
			: "Sailing xp an hour from your own runs, the time between runs counted. Next worth doing: " + nextRank(next).getTitle() + " at " + next.inSentence()
				+ ", for the " + lowerFirst(next.reward(nextRank(next))) + ".";
		return new View.PickCard("Trials", Session.format(l.count()) + (l.count() == 1 ? " run" : " runs"), note, picks);
	}

	private void stats(View view, BarracudaLog l, BarracudaRuns r, int tick)
	{
		if (r.open())
		{
			Barracuda.Trial t = r.trial();
			Barracuda.Rank k = r.rank();
			int now = r.ticks(tick);
			view.stat(new View.Stat(Barracuda.clock(now), "Time", r.finished() ? "Finished" : "This run", Theme.GOLD_BRIGHT));
			BarracudaLog.Run ghost = t == null || k == null ? null : l.ghost(t, k);
			if (ghost != null)
			{
				int gap = liveGap(r.splits(), ghost, now);
				view.stat(new View.Stat(gap == 0 ? "Level" : (gap > 0 ? "+" : "-") + Barracuda.seconds(gap),
					"On your best", "Best " + Barracuda.clock(ghost.ticks), gap > 0 ? Theme.EMBER : Theme.GREEN));
			}
			int xp = r.xpGained();
			if (xp > 0)
			{
				view.stat(new View.Stat(Session.format(xp), "Sailing xp", "This run", Theme.TEAL));
			}
			return;
		}
		double h = l.xpPerHour(null);
		if (h > 0)
		{
			double s = l.xpPerHourSitting(here);
			view.stat(new View.Stat(Session.format(Math.round(s > 0 ? s : h)), "Xp an hour", s > 0 && here != null ? here.shortName() + ", with restarts" : "In your runs", Theme.TEAL));
		}
		int done = runs().runs() - runsBase;
		if (done > 0)
		{
			view.stat(new View.Stat(Integer.toString(done), "Runs today", Session.format(sessionXp) + " Sailing xp", Theme.EMBER));
		}
	}

	/** Your last runs here, or anywhere. */
	private static View.Supply historyCard(BarracudaLog l, Barracuda.Trial t)
	{
		List<View.Line> lines = new ArrayList<>();
		List<BarracudaLog.Run> all = l.all();
		for (int i = all.size() - 1; i >= 0 && lines.size() < 5; i--)
		{
			BarracudaLog.Run x = all.get(i);
			if (t != null && x.trial != t)
			{
				continue;
			}
			boolean ghost = x.rank != null && l.ghost(x.trial, x.rank) == x;
			String what = (t == null ? x.trial.shortName() + " " : "") + x.course() + "  " + (x.ticks > 0 ? Barracuda.clock(x.ticks) : "-");
			String trailing = x.finished() ? Session.format(x.xp) + " xp" : x.end.getTitle();
			lines.add(new View.Line(what, trailing, ghost ? View.Line.Mark.NEXT : x.beat() ? View.Line.Mark.DONE : x.finished() ? View.Line.Mark.TODO : View.Line.Mark.IDLE));
		}
		if (lines.isEmpty())
		{
			return null;
		}
		String headline = null;
		if (t != null)
		{
			StringBuilder b = new StringBuilder();
			for (Map.Entry<Barracuda.Rank, Integer> e : l.bests(t).entrySet())
			{
				if (e.getValue() > 0)
				{
					b.append(b.length() == 0 ? "Best: " : " · ").append(e.getKey().getTitle()).append(' ').append(Barracuda.clock(e.getValue()));
				}
			}
			headline = b.length() == 0 ? null : b.toString();
		}
		double h = l.xpPerHour(t);
		String detail = h > 0 ? Session.format(Math.round(h)) + " Sailing xp an hour in the run" + (t == null ? ", over every trial" : "") : null;
		int n = t == null ? l.count() : l.runs(t);
		return new View.Supply(n == 1 ? "1 kept" : Session.format(n) + " kept", lines, headline, detail, false,
			"Gold is a course's ghost, your quickest finished run of it.").titled("Your runs");
	}

	/** Brief for the panel away from the trials. */
	View.Brief brief(int level)
	{
		BarracudaLog l = log();
		BarracudaLog.Run last = l.last();
		String status = last == null ? null : "Last run: " + last.trial.shortName() + " " + last.course().toLowerCase(Locale.ROOT)
			+ (last.ticks > 0 ? " in " + Barracuda.clock(last.ticks) : "") + (last.finished() ? ", " + Session.format(last.xp) + " Sailing xp" : ", " + last.end.getTitle().toLowerCase(Locale.ROOT));
		Barracuda.Trial next = nextTrial(level, null);
		String how;
		if (next == null)
		{
			Barracuda.Trial first = Barracuda.Trial.TEMPOR_TANTRUM;
			how = level > 0 && level < first.level() ? "Sailing's timed courses open at " + first.level() + " Sailing, with " + first.master() + " in " + first.place() + "."
				: "Every rank you can reach is yours; runs are still kept with their splits and experience.";
		}
		else
		{
			Barracuda.Rank k = nextRank(next);
			how = "Next: " + k.getTitle() + " at " + next.inSentence() + " (" + next.place() + "), " + Barracuda.target(next.targetTicks(k)) + " for the " + lowerFirst(next.reward(k)) + ".";
		}
		View.Brief b = new View.Brief(View.Activity.BARRACUDA_TRIALS, status, how);
		double h = l.xpPerHour(null);
		if (h > 0)
		{
			b.best(Session.format(Math.round(h)) + " Sailing xp an hour over " + l.count() + (l.count() == 1 ? " run." : " runs."));
		}
		return b;
	}
}
