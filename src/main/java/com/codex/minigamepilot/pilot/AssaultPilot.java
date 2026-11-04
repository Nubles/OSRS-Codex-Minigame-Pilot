package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Assault;
import com.codex.minigamepilot.core.Assault.Reward;
import com.codex.minigamepilot.core.Assault.Role;
import com.codex.minigamepilot.core.AssaultLog;
import com.codex.minigamepilot.core.AssaultPlan;
import com.codex.minigamepilot.core.AssaultTracker;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;

/**
 * A companion for Barbarian Assault, outside the fight: every game kept per account (its waves,
 * the role you played in each, their times, and the honour points the game gave each role), your
 * points in each role against the reward you are saving for, and the role to play next to reach
 * it soonest, in games and time at your own pace.
 *
 * <p>It reads the game only: the four roles' point varbits and level varbits, the wave's line in
 * the chat, the role's horn interface and the wave-complete interface. Nothing about the fight
 * itself (the calls, the eggs, the healers) is shown: RuneLite's own Barbarian Assault plugin
 * covers those.
 */
public final class AssaultPilot
{
	static final String LOG_KEY = "assault.kept";
	/** Within this many tiles of Commander Connad or the lobby's scroll table, you are at the outpost. */
	static final int AT_OUTPOST = 30;
	/** Points this close to the cap in a role: spend before the next game. */
	static final int NEAR_CAP = Assault.CAP - 100;

	private final Session session = new Session(Skill.HITPOINTS);
	private final AssaultTracker tracker = new AssaultTracker();
	private Function<String, String> load;
	private BiConsumer<String, String> save;
	private Reward goal = Reward.FIGHTER_TORSO;
	private AssaultLog log;

	// Read each tick.
	private final int[] points = new int[Role.values().length];
	private final int[] levels = new int[Role.values().length];
	private boolean read;
	private Role horn;

	// This session.
	private int wonBase;
	private int fullGames;
	private int pointsEarned;

	Object lastState;

	AssaultPilot()
	{
	}

	void setStore(Function<String, String> load, BiConsumer<String, String> save)
	{
		this.load = load;
		this.save = save;
	}

	/** @param goal the reward saved for, or null for none */
	void settings(Reward goal)
	{
		this.goal = goal;
	}

	void newAccount()
	{
		session.startOver();
		log = null;
		wonBase = tracker.won();
		fullGames = 0;
		pointsEarned = 0;
		read = false;
	}

	AssaultLog log()
	{
		if (log == null)
		{
			log = AssaultLog.decode(load == null ? null : load.apply(LOG_KEY));
		}
		return log;
	}

	/** A wave's line in the chat starts a wave. */
	void chat(String message, int tick)
	{
		int wave = Assault.waveOf(message);
		if (wave > 0)
		{
			tracker.wave(wave, tick);
		}
	}

	/**
	 * Every tick, in the minigame or not: the points and levels, the horn and the wave-complete
	 * interface, so a game is followed and kept even while another pilot is flying.
	 */
	void watch(Client client, long now)
	{
		int tick = client.getTickCount();
		boolean loaded = client.getSkillExperience(Skill.HITPOINTS) > 0;
		int[] now4 = null;
		if (loaded)
		{
			now4 = new int[points.length];
			for (Role r : Role.values())
			{
				now4[r.ordinal()] = Assault.points(client.getVarbitValue(r.baseVarbit()), client.getVarbitValue(r.extraVarbit()));
				points[r.ordinal()] = now4[r.ordinal()];
				levels[r.ordinal()] = Assault.level(client.getVarbitValue(r.levelVarbit()));
			}
			read = true;
		}
		horn = hornShowing(client);
		List<AssaultLog.Game> ended = tracker.tick(tick, horn, shown(client, Assault.WAVE_COMPLETE), now4, now);
		for (AssaultLog.Game g : ended)
		{
			keep(g);
		}
	}

	private void keep(AssaultLog.Game g)
	{
		log().add(g);
		if (save != null)
		{
			save.accept(LOG_KEY, log().encode());
		}
		fullGames += g.full() ? 1 : 0;
		pointsEarned += g.totalPoints();
	}

	/** Logged out or hopped: the game under way ends where it stood. */
	void relogged(long now, int tick)
	{
		AssaultLog.Game g = tracker.logout(tick, now);
		if (g != null)
		{
			keep(g);
		}
		read = false;
	}

	/** The role whose horn interface is showing, or null. */
	private static Role hornShowing(Client client)
	{
		for (Role r : Role.values())
		{
			if (shown(client, r.horn()))
			{
				return r;
			}
		}
		return null;
	}

	private static boolean shown(Client client, int component)
	{
		Widget w = client.getWidget(component);
		return w != null && !w.isHidden();
	}

	/** In a wave, a game under way, or by Commander Connad or the lobby's scroll table. */
	boolean active(Client client, SceneIndex scene, Collection<NPC> npcs)
	{
		if (client.getLocalPlayer() == null)
		{
			return false;
		}
		if (horn != null || tracker.wave() > 0 || tracker.open() && tracker.quiet(client.getTickCount()) <= AssaultTracker.IDLE_TICKS)
		{
			return true;
		}
		WorldPoint at = client.getLocalPlayer().getWorldLocation();
		if (at == null)
		{
			return false;
		}
		for (NPC n : npcs)
		{
			if (n.getId() == Assault.CONNAD && near(n.getWorldLocation(), at, AT_OUTPOST))
			{
				return true;
			}
		}
		for (TileObject o : scene.get(Assault.SCROLL_TABLE))
		{
			if (near(o.getWorldLocation(), at, AT_OUTPOST))
			{
				return true;
			}
		}
		return false;
	}

	static boolean isWatched(int id)
	{
		return id == Assault.SCROLL_TABLE;
	}

	private static boolean near(WorldPoint p, WorldPoint at, int tiles)
	{
		return p != null && p.getPlane() == at.getPlane() && p.distanceTo2D(at) <= tiles;
	}

	/** The pace coach's count: none, as it times no loop here (a wave is no round to hurry). */
	int units()
	{
		return 0;
	}

	void left()
	{
	}

	Flight fly(Client client, Collection<NPC> npcs, long now)
	{
		session.tick(client, now);
		int tick = client.getTickCount();
		Pack pack = Pack.read(client);
		lastState = tracker;

		Map<Role, Integer> have = new EnumMap<>(Role.class);
		Map<Role, Integer> need = new EnumMap<>(Role.class);
		boolean owned = goal != null && goal.items().length > 0 && owns(pack, goal);
		for (Role r : Role.values())
		{
			have.put(r, points[r.ordinal()]);
			need.put(r, goal == null || owned ? 0 : goal.cost(levels[r.ordinal()]));
		}
		AssaultLog g = log();
		AssaultPlan.Result plan = AssaultPlan.plan(need, have, g.rates(), g.typicalGame());

		Step step = step(tick, plan, owned);
		View view = new View(true, View.Activity.BARBARIAN_ASSAULT, step, place(), null, -1);
		Flight out = new Flight(View.Activity.BARBARIAN_ASSAULT, step, view);
		if (step.getTarget() == Step.Target.COMMANDER)
		{
			out.npc = closest(npcs, client.getLocalPlayer().getWorldLocation());
		}
		dress(view, tick, plan, need, owned, now);
		return out;
	}

	private static boolean owns(Pack pack, Reward r)
	{
		int[] ids = r.items();
		return pack.carries(ids) || pack.wears(ids) || pack.banked(ids) > 0;
	}

	private static NPC closest(Collection<NPC> npcs, WorldPoint at)
	{
		NPC best = null;
		int bestD = Integer.MAX_VALUE;
		for (NPC n : npcs)
		{
			WorldPoint p = n.getWorldLocation();
			if (n.getId() == Assault.CONNAD && p != null && at != null && p.getPlane() == at.getPlane() && p.distanceTo2D(at) < bestD)
			{
				bestD = p.distanceTo2D(at);
				best = n;
			}
		}
		return best;
	}

	private String place()
	{
		if (tracker.wave() > 0)
		{
			return "Barbarian Assault · wave " + tracker.wave();
		}
		return tracker.open() ? "Barbarian Assault · between waves" : "Barbarian Outpost";
	}

	/** The one line for the plaque: the wave and its time in a wave; out of one, what your points want next. */
	Step step(int tick, AssaultPlan.Result plan, boolean owned)
	{
		int wave = tracker.wave();
		if (wave > 0)
		{
			Role r = tracker.role();
			int best = log().quickestWave(wave);
			return Step.of("Wave", "Wave " + wave + (r == null ? "" : " as " + r.lower()),
				StormPilot.clock(tracker.waveTicks(tick)) + " so far" + (best > 0 ? " · your quickest wave " + wave + ": " + StormPilot.clock(best) : ""),
				Step.Target.NONE);
		}
		if (tracker.open() && !tracker.waves().isEmpty())
		{
			AssaultLog.Wave last = tracker.waves().get(tracker.waves().size() - 1);
			int next = last.done ? last.number + 1 : last.number;
			if (next <= Assault.WAVES)
			{
				int got = 0;
				for (Role r : Role.values())
				{
					got += tracker.points(r);
				}
				return Step.of("Wave", "Wave " + next + " next" + (next == Assault.WAVES ? ": the Penance Queen" : ""),
					(last.done ? "Wave " + last.number + " won" : "Wave " + last.number + " lost: again") + " · " + StormPilot.clock(tracker.gameTicks(tick))
						+ " this game" + (got > 0 ? " · +" + got + " points" : ""), Step.Target.NONE);
			}
		}
		if (!read)
		{
			return Step.of("Wait", "Reading your honour points", "They arrive a moment after login", Step.Target.NONE);
		}
		for (Role r : Role.values())
		{
			if (points[r.ordinal()] >= NEAR_CAP)
			{
				return Step.of("Spend", "Spend " + r.lower() + " points at Connad",
					Session.format(points[r.ordinal()]) + " of " + Session.format(Assault.CAP) + ": points past the cap are lost", Step.Target.COMMANDER);
			}
		}
		if (goal == null)
		{
			return Step.of("Choose", "Choose a reward to save for", "Barbarian Assault reward, in the settings", Step.Target.NONE);
		}
		if (owned)
		{
			return Step.of("Choose", "You have the " + goal.getTitle().toLowerCase(Locale.ROOT), "Choose another under Barbarian Assault reward", Step.Target.NONE);
		}
		if (goal == Reward.GRANITE_BODY)
		{
			return Step.of("Buy", "Buy the granite body from Connad",
				"95,000 coins once you have killed the queen", Step.Target.COMMANDER);
		}
		if (goal == Reward.ROLE_LEVELS)
		{
			for (Role r : Role.values())
			{
				int l = levels[r.ordinal()];
				int cost = Assault.levelCost(l + 1);
				if (l < Assault.MAX_LEVEL && points[r.ordinal()] >= cost)
				{
					return Step.of("Level up", "Level up " + r.lower() + " to " + (l + 1),
						Session.format(cost) + " " + r.lower() + " points, at Commander Connad", Step.Target.COMMANDER);
				}
			}
		}
		if (plan.next == null)
		{
			if (goal == Reward.ROLE_LEVELS)
			{
				return Step.of("Done", "Every role at level 5", "The Elite Kandarin Diary asks for it", Step.Target.NONE);
			}
			return Step.of("Buy", "Buy the " + goal.getTitle().toLowerCase(Locale.ROOT) + " from Connad",
				Session.format(goal.price()) + " in each role" + (goal.queen() ? " and a Penance Queen kill" : ""), Step.Target.COMMANDER);
		}
		Role r = plan.next;
		return Step.of("Play", "Play " + r.lower() + " next",
			Session.format(plan.shortOf(r)) + " short · " + games(plan), Step.Target.NONE);
	}

	/** "about 9 games, 2h 10m" (at your pace) or "at most 14 games". */
	private static String games(AssaultPlan.Result plan)
	{
		String n = plan.total + (plan.total == 1 ? " game" : " games");
		String count = plan.bound ? "at most " + n : "about " + n;
		return count + (plan.hours > 0 ? ", " + MixologyPilot.clockHours(plan.hours) : "");
	}

	private static Color tone(Role r)
	{
		switch (r)
		{
			case ATTACKER:
				return Theme.EMBER;
			case COLLECTOR:
				return Theme.GOLD;
			case DEFENDER:
				return Theme.TEAL;
			default:
				return Theme.GREEN;
		}
	}

	/** The game under way or your roles' levels, the points gauges, the session, the goal, the role to play, and your games. */
	private void dress(View view, int tick, AssaultPlan.Result plan, Map<Role, Integer> need, boolean owned, long now)
	{
		if (tracker.open())
		{
			view.lines("This game", gameNote(tick), gameLines(tick));
		}
		else if (read)
		{
			List<View.Line> lines = new ArrayList<>();
			int mastered = 0;
			for (Role r : Role.values())
			{
				int l = levels[r.ordinal()];
				if (l >= Assault.MAX_LEVEL)
				{
					mastered++;
					lines.add(new View.Line(r.getTitle() + " level 5", "Mastered", View.Line.Mark.DONE));
					continue;
				}
				int cost = Assault.levelCost(l + 1);
				boolean can = points[r.ordinal()] >= cost;
				lines.add(new View.Line(r.getTitle() + " level " + l, "To " + (l + 1) + ": " + Session.format(cost), can ? View.Line.Mark.NEXT : View.Line.Mark.TODO));
			}
			view.lines("Role levels", mastered == 4 ? "All mastered" : mastered + " of 4 at level 5", lines);
		}

		if (read)
		{
			for (Role r : Role.values())
			{
				int p = points[r.ordinal()];
				int n = need.get(r);
				int max = n > 0 ? n : Assault.CAP;
				String note;
				if (n > 0)
				{
					int s = plan.shortOf(r);
					int games = plan.games(r);
					note = s == 0 ? "Ready for the " + goalWord() : Session.format(s) + " short" + (games > 0 ? " · " + games + (games == 1 ? " game" : " games") + " as " + r.lower() : " · the others' games cover it");
				}
				else
				{
					note = p >= NEAR_CAP ? "Near the 5,000 cap: spend some" : "Level " + levels[r.ordinal()];
				}
				view.gauge(new View.Gauge(r.getTitle(), p, 0, max, note, tone(r)));
			}
		}

		int wavesWon = tracker.won() - wonBase;
		view.stat(new View.Stat(Session.format(wavesWon), "Waves won", session.perHour(wavesWon, now), Theme.GOLD));
		view.stat(new View.Stat(Session.format(fullGames), "Full games", "", Theme.GOLD_BRIGHT));
		int live = 0;
		for (Role r : Role.values())
		{
			live += tracker.points(r);
		}
		view.stat(new View.Stat(Session.format(pointsEarned + live), "Points", session.perHour(pointsEarned + live, now), Theme.TEAL));
		AssaultLog g = log();
		AssaultLog.Game best = g.quickest();
		view.stat(new View.Stat(best == null ? "-" : StormPilot.clock(best.ticks), "Best game", g.queens() > 0 ? g.queens() + (g.queens() == 1 ? " queen" : " queens") : "", Theme.GREEN));

		if (goal != null && read)
		{
			view.goal(goalCard(plan, need, owned));
			if (plan.next != null)
			{
				view.pick(roleCard(plan, g));
			}
		}
		if (g.count() > 0)
		{
			view.supply(historyCard(g));
		}
	}

	private String goalWord()
	{
		return goal == Reward.ROLE_LEVELS ? "levels" : goal.getTitle().toLowerCase(Locale.ROOT);
	}

	private String gameNote(int tick)
	{
		int wave = tracker.wave();
		List<AssaultLog.Wave> w = tracker.waves();
		int on = wave > 0 ? wave : w.isEmpty() ? 1 : Math.min(Assault.WAVES, w.get(w.size() - 1).number + (w.get(w.size() - 1).done ? 1 : 0));
		int got = 0;
		for (Role r : Role.values())
		{
			got += tracker.points(r);
		}
		return "Wave " + on + " of " + Assault.WAVES + " · " + StormPilot.clock(tracker.gameTicks(tick)) + (got > 0 ? " · +" + got : "");
	}

	/** Each wave of the game under way, its role and time, and the one being played. */
	private List<View.Line> gameLines(int tick)
	{
		List<View.Line> lines = new ArrayList<>();
		List<AssaultLog.Wave> waves = tracker.waves();
		for (int i = Math.max(0, waves.size() - 6); i < waves.size(); i++)
		{
			AssaultLog.Wave w = waves.get(i);
			String time = w.ticks > 0 ? StormPilot.clock(w.ticks) : "";
			lines.add(new View.Line("Wave " + w.number + (w.role == null ? "" : " · " + w.role.lower()), w.done ? time : "Lost" + (time.isEmpty() ? "" : " · " + time),
				w.done ? View.Line.Mark.DONE : View.Line.Mark.TODO));
		}
		if (tracker.wave() > 0)
		{
			Role r = tracker.role();
			lines.add(new View.Line("Wave " + tracker.wave() + (r == null ? "" : " · " + r.lower()), StormPilot.clock(tracker.waveTicks(tick)), View.Line.Mark.NEXT));
		}
		return lines;
	}

	/** The reward against your points in every role, with the games and time to it at your pace. */
	private View.GoalCard goalCard(AssaultPlan.Result plan, Map<Role, Integer> need, boolean owned)
	{
		List<View.Line> pieces = new ArrayList<>();
		long have = 0;
		long all = 0;
		for (Role r : Role.values())
		{
			int n = need.get(r);
			all += n;
			have += Math.min(n, points[r.ordinal()]);
		}
		String note;
		if (owned)
		{
			note = "You have it already.";
		}
		else if (goal == Reward.GRANITE_BODY)
		{
			note = "The only reward bought with coins: 95,000, once you have killed the Penance Queen.";
			pieces.add(new View.Line("95,000 coins", "No honour points", View.Line.Mark.IDLE));
		}
		else if (plan.next == null)
		{
			note = goal == Reward.ROLE_LEVELS ? "Every level paid for: level up at Commander Connad." : "Buy it from Commander Connad now.";
		}
		else
		{
			note = (plan.bound ? "At most " : "About ") + plan.total + (plan.total == 1 ? " full game" : " full games")
				+ (plan.hours > 0 ? ", " + MixologyPilot.clockHours(plan.hours) + " at your pace" : "; the time shows after your first full game")
				+ (plan.bound ? ". Roles you have not finished a game in count only the queen's 80 and 5." : ".");
		}
		if (goal.queen())
		{
			int queens = log().queens();
			pieces.add(new View.Line("A Penance Queen kill", queens > 0 ? queens + " seen" : "Finish wave 10", queens > 0 ? View.Line.Mark.DONE : View.Line.Mark.TODO));
		}
		if (goal != Reward.GRANITE_BODY && goal != Reward.ROLE_LEVELS)
		{
			pieces.add(0, new View.Line(Session.format(goal.price()) + " in each role", "4 roles", plan.next == null && !owned ? View.Line.Mark.DONE : View.Line.Mark.TODO));
		}
		boolean done = owned || plan.next == null && goal != Reward.GRANITE_BODY;
		View.GoalCard card = new View.GoalCard(View.Activity.BARBARIAN_ASSAULT, goal.getTitle(), owned || all == 0 ? 1 : have, owned || all == 0 ? 1 : all,
			"points", note, pieces, done);
		if (goal.price() > 0)
		{
			// The wiki: "Players can now sell penance equipment back to Commander Connad for 80% of its original cost."
			card.aside("Sold back to Connad, it returns 80% of its points");
		}
		return card;
	}

	/** Each role short of the goal: the games the plan gives it, the role to play next first. */
	private View.PickCard roleCard(AssaultPlan.Result plan, AssaultLog g)
	{
		Map<Role, AssaultLog.Rate> rates = g.rates();
		int most = 1;
		for (Role r : Role.values())
		{
			most = Math.max(most, plan.games(r));
		}
		List<View.Pick> picks = new ArrayList<>();
		List<Role> order = new ArrayList<>();
		order.add(plan.next);
		for (Role r : Role.values())
		{
			if (r != plan.next)
			{
				order.add(r);
			}
		}
		for (Role r : order)
		{
			int s = plan.shortOf(r);
			int n = plan.games(r);
			AssaultLog.Rate rate = rates.get(r);
			String per = rate != null ? Math.round(rate.own) + " a game, yours" : "80+ a game (the queen's)";
			String detail = s == 0 ? "Covered" : Session.format(s) + " short · " + per;
			String figure = n == 0 ? (s == 0 ? "Ready" : "By the others") : n + (n == 1 ? " game" : " games");
			picks.add(new View.Pick(r.getTitle(), figure, detail, true, r == plan.next, n / (double) most, false));
		}
		return new View.PickCard("Role to play", "Next: " + plan.next.lower(),
			"Every role pays the price, so the role with the most games to go holds you up: play it first, and each won game also gives the other three 5 at the queen's wave.",
			picks);
	}

	/** Your last games, the quickest full one, and points an hour across them. */
	private static View.Supply historyCard(AssaultLog g)
	{
		List<View.Line> lines = new ArrayList<>();
		List<AssaultLog.Game> all = g.all();
		AssaultLog.Game best = g.quickest();
		for (int i = all.size() - 1; i >= 0 && i >= all.size() - 5; i--)
		{
			AssaultLog.Game x = all.get(i);
			Role r = x.role();
			int to = x.waves.get(x.waves.size() - 1).number;
			String what = "Waves " + x.first() + (to != x.first() ? "–" + to : "") + (r == null ? "" : " · " + r.lower()) + (x.ticks > 0 ? " · " + StormPilot.clock(x.ticks) : "");
			lines.add(new View.Line(what, "+" + x.totalPoints(), x == best ? View.Line.Mark.DONE : x.queen() ? View.Line.Mark.IDLE : View.Line.Mark.TODO));
		}
		String headline = best == null ? null : "Quickest full game: " + StormPilot.clock(best.ticks);
		double h = g.pointsPerHour();
		int queenWave = g.quickestWave(Assault.WAVES);
		String detail = h < 0 ? null : Session.format(Math.round(h)) + " honour points an hour over your timed games"
			+ (queenWave > 0 ? "; quickest queen wave " + StormPilot.clock(queenWave) : "");
		return new View.Supply(g.count() == 1 ? "1 kept" : g.count() + " kept", lines, headline, detail, false, null).titled("Your games");
	}

	/** Brief for the panel away from the outpost. */
	View.Brief brief()
	{
		AssaultLog g = log();
		String status = null;
		AssaultLog.Game last = g.last();
		if (last != null)
		{
			Role r = last.role();
			int to = last.waves.get(last.waves.size() - 1).number;
			status = "Last game: waves " + last.first() + (to != last.first() ? "–" + to : "") + (r == null ? "" : " as " + r.lower())
				+ ", +" + last.totalPoints() + " points" + (last.ticks > 0 ? " in " + StormPilot.clock(last.ticks) : "");
		}
		View.Brief b = new View.Brief(View.Activity.BARBARIAN_ASSAULT, status,
			"At the Barbarian Outpost: a team of five in four roles, ten waves to the Penance Queen. Your honour points by role toward "
				+ (goal == null ? "a reward" : "the " + goalWord()) + ".");
		double h = g.pointsPerHour();
		if (h >= 0)
		{
			b.best(Session.format(Math.round(h)) + " honour points an hour over " + g.count() + (g.count() == 1 ? " game." : " games."));
		}
		return b;
	}
}
