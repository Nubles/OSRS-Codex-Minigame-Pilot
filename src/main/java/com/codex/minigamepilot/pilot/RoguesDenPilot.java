package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Maze;
import com.codex.minigamepilot.core.Maze.End;
import com.codex.minigamepilot.core.Maze.Leg;
import com.codex.minigamepilot.core.Maze.Obstacle;
import com.codex.minigamepilot.core.Maze.Piece;
import com.codex.minigamepilot.core.MazeCycle;
import com.codex.minigamepilot.core.MazeLog;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

/**
 * Flies the Rogues' Den maze: in the lobby the levels, the crate to search, the bank and the
 * doorway; in the maze one leg of the wiki's way through at a time, its obstacle lit and the
 * traps near you marked in ember; and every run kept per account with its time, where the maze
 * sent you back and what the safe gave, with the rogue set's time at your own pace.
 *
 * <p>It reads the game only: the mystic jewel in your pack (given on entering the maze, gone when
 * you leave it), your Agility and Thieving as the maze drains them, the tile and the flash powder
 * you carry, and the obstacles and characters in view. No tile of the maze is written in: the
 * obstacles are found as they come into view.
 */
public final class RoguesDenPilot
{
	static final String LOG_KEY = "roguesden.kept";
	static final String OWNED_KEY = "roguesden.owned";
	/** Within this many tiles of Brian O'Richard or the doorway, you are at the den. */
	static final int AT_DEN = 15;
	/** An obstacle within this many tiles of you when a drain comes is the one that drained it. */
	static final int AT_OBSTACLE = 3;
	/** The mosaic door, the grills or the wall safe within this many tiles: you are in their room. */
	static final int ANCHOR = 4;
	/** A rogue guard within this many tiles: the flash powder's room. */
	static final int GUARD_NEAR = 7;
	/** Obstacles and traps within this many tiles are lit or marked. */
	static final int LIGHT = 12;
	/** How long leaving the maze waits for the safe's crate or kit and the run's experience. */
	static final int SETTLE_TICKS = 3;

	private final Session session = new Session(Skill.AGILITY);
	private Function<String, String> load;
	private BiConsumer<String, String> save;
	private boolean markTraps = true;
	private MazeLog log;
	private EnumSet<Piece> owned;

	// The run under way.
	private boolean read;
	private MazeCycle.Tracker tracker;
	private int runTick = -1;
	private int runAgility = -1;
	private int runThieving = -1;
	private boolean prevJewel;
	private boolean prevTile;
	private boolean prevPowder;
	private int prevAgilityNow;
	private int prevThievingNow;
	private int prevCrates;
	private int prevKits;
	// Out of the maze, waiting for what the safe gave.
	private int pendingTick = -1;
	private Leg pendingLeg;
	private int pendingCrates;
	private int pendingKits;

	// This session.
	private int runs;
	private int finished;
	private int crates;

	Object lastState;

	RoguesDenPilot()
	{
	}

	void setStore(Function<String, String> load, BiConsumer<String, String> save)
	{
		this.load = load;
		this.save = save;
	}

	void settings(boolean markTraps)
	{
		this.markTraps = markTraps;
	}

	static boolean isWatched(int id)
	{
		return id == Maze.DOORWAY || Obstacle.of(id) != null;
	}

	/** In the maze (the mystic jewel carried), a run being settled, or by Brian O'Richard or the doorway. */
	boolean active(Client client, SceneIndex scene, Collection<NPC> npcs)
	{
		if (client.getLocalPlayer() == null)
		{
			return false;
		}
		WorldPoint at = client.getLocalPlayer().getWorldLocation();
		if (at == null)
		{
			return false;
		}
		if (pendingTick >= 0 || Pack.read(client).carries(Maze.JEWEL))
		{
			return true;
		}
		for (NPC n : npcs)
		{
			if (n.getId() == Maze.BRIAN && near(n.getWorldLocation(), at, AT_DEN))
			{
				return true;
			}
		}
		for (TileObject o : scene.get(Maze.DOORWAY))
		{
			if (near(o.getWorldLocation(), at, AT_DEN))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean near(WorldPoint p, WorldPoint at, int tiles)
	{
		return p != null && p.getPlane() == at.getPlane() && p.distanceTo2D(at) <= tiles;
	}

	/** Away from the den: a run being settled is kept as it stands. */
	void left(long now)
	{
		if (pendingTick >= 0)
		{
			keep(now, pendingLeg, -1, -1, MazeLog.Reward.NONE, pendingLeg == Leg.SAFE);
		}
		pendingTick = -1;
		read = false;
	}

	/** Logged out or hopped: a run under way ends where it stood. */
	void relogged(long now)
	{
		if (read && prevJewel && tracker != null)
		{
			// Not timed: the logout's moment is not a tick of the run.
			runTick = -1;
			keep(now, tracker.leg(), -1, -1, MazeLog.Reward.NONE, false);
		}
		tracker = null;
		left(now);
	}

	void newAccount()
	{
		session.startOver();
		log = null;
		owned = null;
		tracker = null;
		runTick = -1;
		runs = 0;
		finished = 0;
		crates = 0;
		pendingTick = -1;
		read = false;
	}

	/** A running count for the pace coach: runs to the safe. */
	int units()
	{
		return finished;
	}

	MazeLog log()
	{
		if (log == null)
		{
			log = MazeLog.decode(load == null ? null : load.apply(LOG_KEY));
		}
		return log;
	}

	private EnumSet<Piece> owned()
	{
		if (owned == null)
		{
			owned = Maze.decodePieces(load == null ? null : load.apply(OWNED_KEY));
		}
		return owned;
	}

	Flight fly(Client client, SceneIndex scene, Collection<NPC> npcs, long now)
	{
		session.tick(client, now);
		Pack pack = Pack.read(client);
		int tick = client.getTickCount();
		WorldPoint at = client.getLocalPlayer().getWorldLocation();
		boolean jewel = pack.carries(Maze.JEWEL);
		learnOwned(pack);

		MazeCycle.Snapshot s = new MazeCycle.Snapshot();
		lastState = s;
		s.agility = client.getRealSkillLevel(Skill.AGILITY);
		s.thieving = client.getRealSkillLevel(Skill.THIEVING);
		s.agilityNow = client.getBoostedSkillLevel(Skill.AGILITY);
		s.thievingNow = client.getBoostedSkillLevel(Skill.THIEVING);
		s.crates = pack.count(Maze.CRATE);
		s.holdsTile = pack.carries(Maze.TILE);
		s.holdsPowder = pack.carries(Maze.FLASH_POWDER);
		for (int id : Maze.WRONG_TILES)
		{
			if (pack.carries(id))
			{
				s.wrongTile = id;
			}
		}
		int all = 0;
		for (int n : pack.contents().values())
		{
			all += n;
		}
		s.carried = all - pack.count(Maze.JEWEL);
		s.worn = pack.wornCount();
		s.follower = client.getFollower() != null;
		for (Piece p : Piece.values())
		{
			if (!owned().contains(p))
			{
				s.missing.add(p);
			}
		}

		follow(client, scene, npcs, pack, s, jewel, at, tick, now);
		s.runsBefore = log().count();
		s.inMaze = jewel && tracker != null;
		s.leg = s.inMaze ? tracker.leg() : null;
		s.shortcut = s.inMaze && tracker.shortcut();

		MazeCycle.Decision d = MazeCycle.next(s);
		Step step = d.step;
		View view = new View(true, View.Activity.ROGUES_DEN, step, place(s), MazeCycle.LOOP, d.stage);
		Flight out = new Flight(View.Activity.ROGUES_DEN, step, view);
		target(out, step, d.leg, s, scene, npcs, at);
		dress(view, s, client, pack, now);
		return out;
	}

	/**
	 * Every tick: the jewel arriving starts a run and its leaving ends it; between, each drain at an
	 * obstacle, the tile and flash powder in your pack and the rooms' own obstacles coming near move
	 * the run on through the maze.
	 */
	private void follow(Client client, SceneIndex scene, Collection<NPC> npcs, Pack pack, MazeCycle.Snapshot s, boolean jewel,
		WorldPoint at, int tick, long now)
	{
		if (client.getSkillExperience(Skill.HITPOINTS) <= 0)
		{
			return;
		}
		int kits = pack.count(Maze.KIT);
		if (!read)
		{
			read = true;
			if (jewel && tracker == null)
			{
				// A run already under way: followed from here, kept, but not timed.
				startRun(client, s, -1);
			}
			remember(s, jewel, kits);
			return;
		}
		if (jewel && !prevJewel)
		{
			if (pendingTick >= 0)
			{
				settle(client, pack, tick, now, true);
			}
			startRun(client, s, tick);
		}
		else if (!jewel && prevJewel && tracker != null)
		{
			pendingTick = tick;
			pendingLeg = tracker.leg();
			pendingCrates = prevCrates;
			pendingKits = prevKits;
			tracker = null;
		}
		if (jewel && tracker != null)
		{
			if (s.agilityNow < prevAgilityNow)
			{
				tracker.event(agilityEnds(prevAgilityNow - s.agilityNow, nearestObstacle(scene, at)));
			}
			if (s.thievingNow < prevThievingNow)
			{
				tracker.event(End.TRAP);
			}
			if (s.holdsTile && !prevTile)
			{
				tracker.event(End.TILE_TAKEN);
			}
			else if (!s.holdsTile && prevTile)
			{
				tracker.event(End.TILE_USED);
			}
			if (s.holdsPowder && !prevPowder)
			{
				tracker.event(End.POWDER_TAKEN);
			}
			else if (!s.holdsPowder && prevPowder)
			{
				tracker.event(End.POWDER_USED);
			}
			for (Obstacle o : new Obstacle[]{Obstacle.MOSAIC_DOOR, Obstacle.GRILL, Obstacle.SPINNING_BLADES, Obstacle.WALL_SAFE})
			{
				if (!nearby(scene, o, at, ANCHOR).isEmpty())
				{
					tracker.near(o);
				}
			}
			for (NPC n : npcs)
			{
				if (isGuard(n.getId()) && near(n.getWorldLocation(), at, GUARD_NEAR))
				{
					tracker.guardNear();
				}
			}
		}
		if (pendingTick >= 0)
		{
			settle(client, pack, tick, now, false);
		}
		remember(s, jewel, kits);
	}

	private void remember(MazeCycle.Snapshot s, boolean jewel, int kits)
	{
		prevJewel = jewel;
		prevTile = s.holdsTile;
		prevPowder = s.holdsPowder;
		prevAgilityNow = s.agilityNow;
		prevThievingNow = s.thievingNow;
		prevCrates = s.crates;
		prevKits = kits;
	}

	private void startRun(Client client, MazeCycle.Snapshot s, int tick)
	{
		tracker = new MazeCycle.Tracker(s.thieving >= Maze.SHORTCUT_THIEVING);
		runTick = tick;
		runAgility = tick < 0 ? -1 : client.getSkillExperience(Skill.AGILITY);
		runThieving = tick < 0 ? -1 : client.getSkillExperience(Skill.THIEVING);
	}

	/** Out of the maze: once the safe's crate or kit has come, or a few ticks have passed, the run is kept. */
	private void settle(Client client, Pack pack, int tick, long now, boolean force)
	{
		int crateNow = pack.count(Maze.CRATE);
		int kitNow = pack.count(Maze.KIT);
		MazeLog.Reward reward = crateNow > pendingCrates ? MazeLog.Reward.CRATE : kitNow > pendingKits ? MazeLog.Reward.KIT : MazeLog.Reward.NONE;
		if (reward == MazeLog.Reward.NONE && !force && tick - pendingTick < SETTLE_TICKS - 1)
		{
			return;
		}
		boolean done = reward != MazeLog.Reward.NONE || pendingLeg == Leg.SAFE;
		keep(now, pendingLeg, client.getSkillExperience(Skill.AGILITY), client.getSkillExperience(Skill.THIEVING), reward, done);
		pendingTick = -1;
	}

	private void keep(long now, Leg leg, int agilityXp, int thievingXp, MazeLog.Reward reward, boolean done)
	{
		int ticks = runTick >= 0 && pendingTick >= 0 ? pendingTick - runTick : 0;
		int axp = runAgility >= 0 && agilityXp >= 0 ? agilityXp - runAgility : 0;
		int txp = runThieving >= 0 && thievingXp >= 0 ? thievingXp - runThieving : 0;
		log().add(new MazeLog.Run(now, ticks, done, leg, reward, axp, txp));
		if (save != null)
		{
			save.accept(LOG_KEY, log().encode());
		}
		runs++;
		if (done)
		{
			finished++;
		}
		if (reward == MazeLog.Reward.CRATE)
		{
			crates++;
		}
		runTick = -1;
		runAgility = -1;
		runThieving = -1;
	}

	/**
	 * What a drain of Agility says was passed: the obstacle beside you when it came, else the
	 * obstacles the wiki says drain that much (3 is a pendulum or a blade).
	 */
	static Set<End> agilityEnds(int drained, Obstacle beside)
	{
		if (beside != null)
		{
			switch (beside)
			{
				case BARS:
					return EnumSet.of(End.BARS);
				case PENDULUM:
					return EnumSet.of(End.PENDULUM);
				case LEDGE:
					return EnumSet.of(End.LEDGE);
				case BLADE:
					return EnumSet.of(End.BLADE);
				default:
					break;
			}
		}
		EnumSet<End> out = EnumSet.noneOf(End.class);
		if (drained == Maze.DRAIN_BARS)
		{
			out.add(End.BARS);
		}
		if (drained == Maze.DRAIN_LEDGE || drained == Maze.DRAIN_LEDGE_LONG)
		{
			out.add(End.LEDGE);
		}
		if (drained == Maze.DRAIN_PENDULUM)
		{
			out.add(End.PENDULUM);
		}
		if (drained == Maze.DRAIN_BLADE)
		{
			out.add(End.BLADE);
		}
		return out;
	}

	/** The Agility obstacle nearest you, within a few tiles, or null. */
	private static Obstacle nearestObstacle(SceneIndex scene, WorldPoint at)
	{
		Obstacle best = null;
		int bestD = AT_OBSTACLE + 1;
		for (Obstacle o : new Obstacle[]{Obstacle.BARS, Obstacle.PENDULUM, Obstacle.LEDGE, Obstacle.BLADE})
		{
			for (TileObject t : nearby(scene, o, at, AT_OBSTACLE))
			{
				int d = t.getWorldLocation().distanceTo2D(at);
				if (d < bestD)
				{
					bestD = d;
					best = o;
				}
			}
		}
		return best;
	}

	private static List<TileObject> nearby(SceneIndex scene, Obstacle o, WorldPoint at, int tiles)
	{
		List<TileObject> out = new ArrayList<>();
		for (int id : o.ids())
		{
			for (TileObject t : scene.get(id))
			{
				if (near(t.getWorldLocation(), at, tiles))
				{
					out.add(t);
				}
			}
		}
		return out;
	}

	private static boolean isGuard(int id)
	{
		for (int g : Maze.GUARDS)
		{
			if (g == id)
			{
				return true;
			}
		}
		return false;
	}

	/** The pieces carried, worn or in the bank as last seen: once seen, owned (they are untradeable). */
	private void learnOwned(Pack pack)
	{
		EnumSet<Piece> o = owned();
		boolean grew = false;
		for (Piece p : Piece.values())
		{
			if (!o.contains(p) && (pack.carries(p.item()) || pack.wears(p.item()) || pack.banked(p.item()) > 0))
			{
				o.add(p);
				grew = true;
			}
		}
		if (grew && save != null)
		{
			save.accept(OWNED_KEY, Maze.encodePieces(o));
		}
	}

	/** Lights the step's thing: the doorway, the banker, the leg's obstacle, the guard or a safe; the traps near you in ember. */
	private void target(Flight out, Step step, Leg leg, MazeCycle.Snapshot s, SceneIndex scene, Collection<NPC> npcs, WorldPoint at)
	{
		switch (step.getTarget())
		{
			case DOORWAY:
				out.object = closest(scene.get(Maze.DOORWAY), at, AT_DEN);
				break;
			case BANKER:
				out.npc = closestNpc(npcs, at, Maze.BANKER);
				break;
			case GUARD:
				out.npc = closestNpc(npcs, at, Maze.GUARDS);
				break;
			case WALL_SAFE:
				out.object = closest(nearby(scene, Obstacle.WALL_SAFE, at, LIGHT), at, LIGHT);
				break;
			case OBSTACLE:
			{
				Obstacle o = leg == null ? null : leg.obstacle();
				if (o != null)
				{
					List<TileObject> lit = nearby(scene, o, at, LIGHT);
					out.object = closest(lit, at, LIGHT);
					// The other walls of a trap row are run past, not searched: they stay marked as traps.
					for (TileObject t : lit)
					{
						if (t != out.object && !o.hazard())
						{
							out.others.add(t);
						}
					}
				}
				break;
			}
			default:
				break;
		}
		if (markTraps && s.inMaze)
		{
			for (Obstacle o : Obstacle.values())
			{
				if (o.hazard())
				{
					for (TileObject t : nearby(scene, o, at, LIGHT))
					{
						if (t != out.object)
						{
							out.hazards.add(t);
						}
					}
				}
			}
		}
	}

	private static TileObject closest(Collection<TileObject> things, WorldPoint at, int tiles)
	{
		TileObject best = null;
		int bestD = tiles + 1;
		for (TileObject t : things)
		{
			WorldPoint p = t.getWorldLocation();
			if (p != null && p.getPlane() == at.getPlane() && p.distanceTo2D(at) < bestD)
			{
				bestD = p.distanceTo2D(at);
				best = t;
			}
		}
		return best;
	}

	private static NPC closestNpc(Collection<NPC> npcs, WorldPoint at, int... ids)
	{
		NPC best = null;
		int bestD = Integer.MAX_VALUE;
		for (NPC n : npcs)
		{
			boolean match = false;
			for (int id : ids)
			{
				match |= n.getId() == id;
			}
			WorldPoint p = n.getWorldLocation();
			if (match && p != null && p.getPlane() == at.getPlane() && p.distanceTo2D(at) < bestD)
			{
				bestD = p.distanceTo2D(at);
				best = n;
			}
		}
		return best;
	}

	private String place(MazeCycle.Snapshot s)
	{
		if (!s.inMaze)
		{
			return "Rogues' Den · lobby";
		}
		return "Rogues' Den · " + (s.leg == Leg.SAFE ? "the safes" : s.leg.part().getTitle().toLowerCase(Locale.ROOT));
	}

	/** The way through or the lobby's checklist, your session, the rogue set, where the maze sends you back, and your runs. */
	private void dress(View view, MazeCycle.Snapshot s, Client client, Pack pack, long now)
	{
		if (s.inMaze)
		{
			view.lines("The maze", wayNote(), wayLines(s));
		}
		else
		{
			List<View.Line> lines = new ArrayList<>();
			lines.add(new View.Line("50 Agility", String.valueOf(s.agility), s.agility >= Maze.LEVEL ? View.Line.Mark.DONE : View.Line.Mark.TODO));
			lines.add(new View.Line("50 Thieving", String.valueOf(s.thieving), s.thieving >= Maze.LEVEL ? View.Line.Mark.DONE : View.Line.Mark.TODO));
			boolean empty = s.carried == 0 && s.worn == 0;
			lines.add(new View.Line("Nothing carried or worn", empty ? "Ready" : (s.carried + s.worn) + (s.carried + s.worn == 1 ? " item" : " items"),
				empty ? View.Line.Mark.DONE : View.Line.Mark.TODO));
			lines.add(new View.Line("No follower", s.follower ? "Following" : "None", s.follower ? View.Line.Mark.TODO : View.Line.Mark.DONE));
			boolean cut = s.thieving >= Maze.SHORTCUT_THIEVING;
			lines.add(new View.Line("Shortcut, 80 Thieving", cut ? "Open" : "Long route", cut ? View.Line.Mark.DONE : View.Line.Mark.IDLE));
			view.lines("The lobby", s.runsBefore == 0 ? "First time: see Brian O'Richard" : null, lines);
		}

		view.stat(new View.Stat(Session.format(runs), "Runs", session.perHour(runs, now), Theme.GOLD));
		view.stat(new View.Stat(Session.format(finished), "To the safe", runs > finished ? (runs - finished) + " sent back" : "", Theme.GOLD_BRIGHT));
		view.stat(new View.Stat(Session.format(crates), "Crates", session.perHour(crates, now), Theme.TEAL));
		int xp = session.xp(client);
		view.stat(new View.Stat(Session.format(xp), "Agility xp", session.perHour(xp, now), Theme.GREEN));

		view.goal(setCard(s, pack));
		MazeLog g = log();
		if (g.failed() > 0)
		{
			view.pick(sentBackCard(g, s.leg));
		}
		if (g.count() > 0)
		{
			view.supply(historyCard(g));
		}
	}

	private String wayNote()
	{
		List<Leg> route = tracker.route();
		return "Leg " + (tracker.index() + 1) + " of " + route.size() + (tracker.shortcut() ? " · shortcut" : " · long route");
	}

	/** The leg passed, the one you are on and the next few, as the wiki walks them. */
	private List<View.Line> wayLines(MazeCycle.Snapshot s)
	{
		List<View.Line> lines = new ArrayList<>();
		List<Leg> route = tracker.route();
		int i = tracker.index();
		Map<Leg, Integer> back = log().sentBack();
		for (int k = Math.max(0, i - 1); k < route.size() && k <= i + 4; k++)
		{
			Leg l = route.get(k);
			Integer fails = back.get(l);
			String trailing = fails != null && k >= i ? "Sent back " + fails + "×" : "";
			lines.add(new View.Line(cap(l.where()), trailing, k < i ? View.Line.Mark.DONE : k == i ? View.Line.Mark.NEXT : View.Line.Mark.TODO));
		}
		return lines;
	}

	private static String cap(String s)
	{
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	/** The rogue set: the pieces you own, the crates you hold, and attempts and time to the rest at your pace. */
	private View.GoalCard setCard(MazeCycle.Snapshot s, Pack pack)
	{
		EnumSet<Piece> have = owned();
		int banked = Math.max(0, pack.banked(Maze.CRATE));
		int held = s.crates + banked;
		List<View.Line> pieces = new ArrayList<>();
		int toChoose = held;
		for (Piece p : Piece.values())
		{
			if (have.contains(p))
			{
				pieces.add(new View.Line(p.getTitle(), "Owned", View.Line.Mark.DONE));
			}
			else if (toChoose > 0)
			{
				toChoose--;
				pieces.add(new View.Line(p.getTitle(), "In a crate", View.Line.Mark.NEXT));
			}
			else
			{
				pieces.add(new View.Line(p.getTitle(), "A crate", View.Line.Mark.TODO));
			}
		}
		int need = Math.max(0, Piece.values().length - have.size() - held);
		int level = s.thieving;
		String note;
		if (have.size() == Piece.values().length)
		{
			note = "The whole set: every pickpocket doubled.";
		}
		else if (need == 0)
		{
			note = "Your crates complete the set: search each and choose a piece you lack.";
		}
		else
		{
			int attempts = Maze.attemptsFor(need, level);
			double hours = log().hoursFor(attempts);
			String odds = String.format(Locale.ROOT, "%.1f%%", Maze.crateChance(level) * 100);
			note = need + (need == 1 ? " crate" : " crates") + " more: about " + attempts + (attempts == 1 ? " run" : " runs") + " to the safe at " + odds
				+ (level >= Maze.LEVEL ? " (the wiki, at " + Maze.tableLevel(level) + " Thieving)" : "")
				+ (hours > 0 ? ", about " + MixologyPilot.clockHours(hours) + " at your pace" : "; the time shows after your first run to the safe");
		}
		int worn = 0;
		for (Piece p : Piece.values())
		{
			worn += pack.wears(p.item()) ? 1 : 0;
		}
		View.GoalCard card = new View.GoalCard(View.Activity.ROGUES_DEN, "Rogue equipment", Math.min(5, have.size() + held), 5, "pieces", note, pieces,
			have.size() == Piece.values().length);
		card.aside(worn > 0 ? worn + (worn == 1 ? " piece" : " pieces") + " worn: double loot " + Maze.doubleLootPercent(worn) + "% of pickpockets"
			: "Worn to pickpocket: 15% double loot a piece, all five 100%");
		return card;
	}

	/** Where the maze sends you back, leg by leg in the maze's order, the leg you are on lit. */
	private static View.PickCard sentBackCard(MazeLog g, Leg current)
	{
		Map<Leg, Integer> back = g.sentBack();
		int most = 1;
		for (int n : back.values())
		{
			most = Math.max(most, n);
		}
		List<View.Pick> picks = new ArrayList<>();
		for (Map.Entry<Leg, Integer> e : back.entrySet())
		{
			Leg l = e.getKey();
			int n = e.getValue();
			picks.add(new View.Pick(cap(l.where()), n + "×", l.part().getTitle() + ": " + l.detail().toLowerCase(Locale.ROOT), true, l == current,
				n / (double) most, true));
		}
		return new View.PickCard("Sent back", g.failed() + " of " + g.count(),
			"Where your runs ended short of the safe. The wiki: you leave the maze if you trigger a trap or touch a dangerous obstacle.", picks);
	}

	/** Your last runs, the quickest, and runs and Agility experience an hour across them. */
	private static View.Supply historyCard(MazeLog g)
	{
		List<View.Line> lines = new ArrayList<>();
		List<MazeLog.Run> all = g.all();
		MazeLog.Run best = g.quickest();
		for (int i = all.size() - 1; i >= 0 && i >= all.size() - 5; i--)
		{
			MazeLog.Run r = all.get(i);
			String what = (r.finished ? "To the safe" : "Sent back") + (r.ticks > 0 ? " · " + StormPilot.clock(r.ticks) : "");
			String trailing = r.finished ? (r.reward == MazeLog.Reward.CRATE ? "Crate" : r.reward == MazeLog.Reward.KIT ? "Rogue kit" : "Safe failed")
				: r.leg == null ? "Left" : "At " + r.leg.where();
			lines.add(new View.Line(what, trailing, r == best ? View.Line.Mark.DONE : r.finished ? View.Line.Mark.IDLE : View.Line.Mark.TODO));
		}
		String headline = best == null ? null : "Quickest to the safe: " + StormPilot.clock(best.ticks);
		double n = g.perHour();
		String detail = n < 0 ? null : String.format(Locale.ROOT, "%.0f to the safe an hour, sent-back runs counted; %s Agility xp an hour (the wiki: up to %s at 99 Agility and 80 Thieving)",
			n, Session.format(Math.round(g.agilityXpPerHour())), Session.format(Maze.WIKI_AGILITY_XP_PER_HOUR));
		String note = g.failed() > 0 && g.worst() != null ? g.failed() + " of " + g.count() + " sent back, most at " + g.worst().where() : null;
		return new View.Supply(g.finished() + " to the safe", lines, headline, detail, false, note).titled("Your runs");
	}

	/** Brief for the panel away from the den. */
	View.Brief brief()
	{
		MazeLog g = log();
		String status = null;
		MazeLog.Run last = g.last();
		if (last != null)
		{
			status = "Last run: " + (last.finished ? "to the safe" + (last.reward == MazeLog.Reward.CRATE ? ", a crate" : last.reward == MazeLog.Reward.KIT ? ", a rogue kit" : "")
				: "sent back at " + (last.leg == null ? "the maze" : last.leg.where())) + (last.ticks > 0 ? " in " + StormPilot.clock(last.ticks) : "");
		}
		int have = owned().size();
		View.Brief b = new View.Brief(View.Activity.ROGUES_DEN, status,
			"In Burthorpe, from 50 Agility and 50 Thieving, with nothing carried or worn. Rogue set: " + have + " of 5 pieces.");
		double hour = g.perHour();
		if (hour >= 0)
		{
			b.best(Math.round(hour) + " runs to the safe and " + Session.format(Math.round(g.agilityXpPerHour())) + " Agility xp an hour over "
				+ g.count() + (g.count() == 1 ? " run." : " runs."));
		}
		return b;
	}
}
