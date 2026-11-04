package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.ArtefactCycle;
import com.codex.minigamepilot.core.ArtefactLog;
import com.codex.minigamepilot.core.Artefacts;
import com.codex.minigamepilot.core.Artefacts.House;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

/**
 * Flies the stealing of artefacts in Port Piscarilius: the house Captain Khaled names, its ladder
 * and drawers, then the artefact back to him on foot with the patrols marked, and every trip kept
 * per account with its experience, coins and time. It only reads the game (the task varbit,
 * Captain Khaled's words, what you carry, the drawers, ladders and characters in view) and draws.
 *
 * <p>Ids are RuneLite's gameval names: the varbit {@code PISCARILIUS_BURGLARY_TARGET}, the
 * drawers {@code PISCARILIUS_BURGLARY_DRAWERS_N} to {@code _NW}, the ladders
 * {@code PISCARILIUS_LADDER_BOTTOM} and {@code _TOP_01}/{@code _02}, Captain Khaled
 * {@code PISCARILIUS_CAPTAIN_KHALED_*} and the patrols {@code PISCARILIUS_PATROLMAN_*}. Which
 * varbit value is which house is as the Stealing Artefacts plugin (pajlads, MIT) reads it:
 * unconfirmed, so Captain Khaled's own words win when he names another house.
 */
public final class ArtefactsPilot
{
	static final int TASK = VarbitID.PISCARILIUS_BURGLARY_TARGET;
	static final int DIARY_HARD = VarbitID.KOUREND_DIARY_HARD_COMPLETE;
	static final String LOG_KEY = "artefacts.kept";
	static final String PLACES_KEY = "artefacts.places";
	/** Within this many tiles of Captain Khaled, with no task, you are about to play. */
	static final int AT_KHALED = 10;
	/** Patrols within this many tiles count as near. */
	static final int NEAR = 8;
	/** Patrols within this many tiles are marked while you carry an artefact. */
	static final int MARK = 15;
	/** A ladder within this many tiles of a house's ladder belongs to it. */
	static final int LADDER_NEAR = 6;
	/** How long the artefact's leaving the pack waits for Captain Khaled's experience and coins. */
	static final int SETTLE_TICKS = 3;

	private final Session session = new Session(Skill.THIEVING);
	private Function<String, String> load;
	private BiConsumer<String, String> save;
	private boolean markPatrols = true;
	private int goalLevel;
	private ArtefactLog log;
	/** Where each house's drawers and Captain Khaled were last seen, per account. */
	private Map<House, WorldPoint> drawersSeen;
	private WorldPoint khaledSeen;

	// Captain Khaled's words.
	private String lastDialog;
	private House heard;
	private int heardTick = -1;
	private int lastTask = -1;
	private int taskTick = -1;

	// A trip: from the task to the hand-in.
	private boolean read;
	private int runTick = -1;
	private int runXp = -1;
	private House runHouse;
	private boolean prevCarrying;
	private int prevXp;
	private int prevCoins;
	private int pendingTick = -1;
	private int pendingXp;
	private int pendingCoins;

	// This session.
	private int delivered;
	private int lost;
	private long coins;

	Object lastState;

	ArtefactsPilot()
	{
	}

	void setStore(Function<String, String> load, BiConsumer<String, String> save)
	{
		this.load = load;
		this.save = save;
	}

	/** @param goalLevel the Thieving level to work toward, 0 for the next */
	void settings(boolean markPatrols, int goalLevel)
	{
		this.markPatrols = markPatrols;
		this.goalLevel = Math.max(0, Math.min(99, goalLevel));
	}

	static boolean isWatched(int id)
	{
		return House.byDrawers(id) != null || id == Artefacts.LADDER_UP || StormPilot.contains(Artefacts.LADDER_DOWN, id);
	}

	/** In Port Piscarilius with an artefact or a task, or by Captain Khaled. */
	boolean active(Client client, Collection<NPC> npcs)
	{
		if (client.getLocalPlayer() == null)
		{
			return false;
		}
		WorldPoint at = client.getLocalPlayer().getWorldLocation();
		if (at == null || !Artefacts.inPort(at.getX(), at.getY()))
		{
			return false;
		}
		if (Pack.read(client).carries(Artefacts.ITEMS))
		{
			return true;
		}
		int task = client.getVarbitValue(TASK);
		if (task >= 1 && task <= Artefacts.TASK_DELIVER)
		{
			return true;
		}
		for (NPC n : npcs)
		{
			WorldPoint p = n.getWorldLocation();
			if (Artefacts.isKhaled(n.getId()) && p != null && p.getPlane() == at.getPlane() && p.distanceTo2D(at) <= AT_KHALED)
			{
				return true;
			}
		}
		return false;
	}

	/** Out of the port: a hand-in waiting is dropped. */
	void left()
	{
		pendingTick = -1;
		prevCarrying = false;
		read = false;
	}

	/** Logged out or hopped: "logging out" removes an artefact, so one carried is lost. */
	void relogged(long now)
	{
		if (read && prevCarrying)
		{
			keep(false, now, -1, 0);
		}
		left();
	}

	void newAccount()
	{
		session.startOver();
		log = null;
		drawersSeen = null;
		khaledSeen = null;
		heard = null;
		heardTick = -1;
		lastDialog = null;
		lastTask = -1;
		taskTick = -1;
		runTick = -1;
		runXp = -1;
		runHouse = null;
		delivered = 0;
		lost = 0;
		coins = 0;
		left();
	}

	/** A running count for the pace coach: artefacts delivered. */
	int units()
	{
		return delivered;
	}

	private ArtefactLog log()
	{
		if (log == null)
		{
			log = ArtefactLog.decode(load == null ? null : load.apply(LOG_KEY));
		}
		return log;
	}

	private Map<House, WorldPoint> places()
	{
		if (drawersSeen == null)
		{
			drawersSeen = new EnumMap<>(House.class);
			String text = load == null ? null : load.apply(PLACES_KEY);
			khaledSeen = decodePlaces(text, drawersSeen);
		}
		return drawersSeen;
	}

	Flight fly(Client client, SceneIndex scene, Collection<NPC> npcs, long now)
	{
		session.tick(client, now);
		Pack pack = Pack.read(client);
		int tick = client.getTickCount();
		WorldPoint at = client.getLocalPlayer().getWorldLocation();
		ArtefactCycle.Snapshot s = new ArtefactCycle.Snapshot();
		lastState = s;
		s.level = client.getBoostedSkillLevel(Skill.THIEVING);
		s.task = client.getVarbitValue(TASK);
		s.carrying = pack.carries(Artefacts.ITEMS);
		s.pick = pack.carries(Artefacts.PICKS);
		s.teleport = pack.carries(Artefacts.TELEPORTS);
		s.plane = at.getPlane();
		if (s.task != lastTask)
		{
			lastTask = s.task;
			taskTick = tick;
		}
		listen(client, tick);
		House varbitHouse = House.byTask(s.task);
		// His words, when he spoke them about when the task changed, win over the varbit's reading.
		boolean heardNow = heard != null && varbitHouse != null && heardTick >= taskTick - 10;
		s.house = heardNow ? heard : varbitHouse;
		s.heard = heardNow && heard == s.house;
		s.heardOther = heardNow && varbitHouse != null && heard != varbitHouse;

		learn(scene, npcs);
		NPC khaled = nearest(npcs, at, true);
		List<NPC> patrols = new ArrayList<>();
		for (NPC n : npcs)
		{
			WorldPoint p = n.getWorldLocation();
			if (Artefacts.isPatrol(n.getId()) && p != null && p.getPlane() == at.getPlane())
			{
				patrols.add(n);
				int d = p.distanceTo2D(at);
				if (d <= NEAR)
				{
					s.patrolsNear++;
					if (s.nearestPatrol < 0 || d < s.nearestPatrol)
					{
						s.nearestPatrol = d;
						s.patrolFacing = facing(n, at);
					}
				}
			}
		}
		List<TileObject> drawers = new ArrayList<>();
		if (s.house != null)
		{
			for (TileObject o : scene.get(s.house.drawers()))
			{
				if (o.getPlane() == s.plane)
				{
					drawers.add(o);
				}
			}
		}
		s.drawers = !drawers.isEmpty();
		List<TileObject> up = ladders(scene, s.house, s.plane);
		s.ladderUp = !up.isEmpty();
		List<TileObject> down = new ArrayList<>();
		for (int id : Artefacts.LADDER_DOWN)
		{
			for (TileObject o : scene.get(id))
			{
				if (o.getPlane() == s.plane && o.getWorldLocation().distanceTo2D(at) <= 12)
				{
					down.add(o);
				}
			}
		}
		s.ladderDown = !down.isEmpty();

		settle(client, pack, s, tick, now);

		ArtefactCycle.Decision d = ArtefactCycle.next(s);
		Step step = d.step;
		Ground ground = Ground.read(client, Collections.emptyList(), 0);
		Flight flight = new Flight(View.Activity.ARTEFACTS, step, null);
		switch (step.getTarget())
		{
			case KHALED:
				if (khaled != null)
				{
					Ground.Way<NPC> way = ground == null ? null : ground.to(khaled);
					if (way != null)
					{
						flight.to(way);
					}
					else
					{
						flight.npc = khaled;
					}
				}
				else if (khaledSeen != null)
				{
					flight.destination = khaledSeen;
				}
				break;
			case DRAWERS:
				toObject(flight, ground, drawers);
				break;
			case LADDER:
				toObject(flight, ground, s.carrying || s.plane > 0 ? down : up);
				break;
			case HOUSE:
				flight.destination = houseAt(s.house);
				break;
			default:
				break;
		}
		View view = new View(true, View.Activity.ARTEFACTS, step, place(s), ArtefactCycle.LOOP, d.stage);
		Flight out = new Flight(View.Activity.ARTEFACTS, step, view);
		out.object = flight.object;
		out.npc = flight.npc;
		out.route = flight.route;
		out.destination = flight.destination;
		if (markPatrols && s.carrying && s.plane == 0)
		{
			for (NPC n : patrols)
			{
				if (n.getWorldLocation().distanceTo2D(at) <= MARK)
				{
					out.wary.add(n);
				}
			}
		}
		dress(view, s, client, pack, at, patrols, now);
		return out;
	}

	private static void toObject(Flight flight, Ground ground, Collection<TileObject> things)
	{
		if (things.isEmpty())
		{
			return;
		}
		Ground.Way<TileObject> way = ground == null ? null : ground.nearest(things, null);
		if (way != null)
		{
			flight.to(way);
			return;
		}
		flight.object = things.iterator().next();
	}

	/** The ladders up on the ground floor that belong to the house: those nearest where its ladder stands. */
	private static List<TileObject> ladders(SceneIndex scene, House house, int plane)
	{
		List<TileObject> out = new ArrayList<>();
		if (house == null || !house.upstairs() || plane != 0)
		{
			return out;
		}
		for (TileObject o : scene.get(Artefacts.LADDER_UP))
		{
			WorldPoint p = o.getWorldLocation();
			if (p != null && p.getPlane() == 0 && Math.max(Math.abs(p.getX() - house.ladderX()), Math.abs(p.getY() - house.ladderY())) <= LADDER_NEAR)
			{
				out.add(o);
			}
		}
		return out;
	}

	/** Where to point for a house: its drawers where seen, else about where they stand; on the ground floor. */
	private WorldPoint houseAt(House house)
	{
		if (house == null)
		{
			return null;
		}
		WorldPoint seen = places().get(house);
		return seen != null ? new WorldPoint(seen.getX(), seen.getY(), 0) : new WorldPoint(house.x(), house.y(), 0);
	}

	/** Captain Khaled's words: "It can be found in the [location]." */
	private void listen(Client client, int tick)
	{
		Widget npc = client.getWidget(InterfaceID.ChatLeft.TEXT);
		String text = npc != null && !npc.isHidden() ? npc.getText() : null;
		if (text == null || text.equals(lastDialog))
		{
			return;
		}
		lastDialog = text;
		House h = House.heard(Text.sanitizeMultilineText(text));
		if (h != null)
		{
			heard = h;
			heardTick = tick;
		}
	}

	/** Each drawer and Captain Khaled where they stand, kept so the arrow can find them out of sight. */
	private void learn(SceneIndex scene, Collection<NPC> npcs)
	{
		Map<House, WorldPoint> p = places();
		boolean changed = false;
		for (House h : House.values())
		{
			for (TileObject o : scene.get(h.drawers()))
			{
				WorldPoint w = o.getWorldLocation();
				if (w != null && !w.equals(p.get(h)))
				{
					p.put(h, w);
					changed = true;
				}
			}
		}
		for (NPC n : npcs)
		{
			WorldPoint w = n.getWorldLocation();
			if (Artefacts.isKhaled(n.getId()) && w != null && Artefacts.inPort(w.getX(), w.getY())
				&& (khaledSeen == null || khaledSeen.distanceTo2D(w) > 2 || khaledSeen.getPlane() != w.getPlane()))
			{
				khaledSeen = w;
				changed = true;
			}
		}
		if (changed && save != null)
		{
			save.accept(PLACES_KEY, encodePlaces(p, khaledSeen));
		}
	}

	private static NPC nearest(Collection<NPC> npcs, WorldPoint at, boolean khaled)
	{
		NPC best = null;
		int bestD = Integer.MAX_VALUE;
		for (NPC n : npcs)
		{
			WorldPoint p = n.getWorldLocation();
			if ((khaled ? Artefacts.isKhaled(n.getId()) : Artefacts.isPatrol(n.getId())) && p != null && p.getPlane() == at.getPlane())
			{
				int d = p.distanceTo2D(at);
				if (d < bestD)
				{
					bestD = d;
					best = n;
				}
			}
		}
		return best;
	}

	/** Whether a character faces toward a tile, within about forty-five degrees. */
	static boolean facing(NPC n, WorldPoint at)
	{
		WorldPoint p = n.getWorldLocation();
		return facing(n.getOrientation(), at.getX() - p.getX(), at.getY() - p.getY());
	}

	/** @param orientation the game's: 0 south, 512 west, 1024 north, 1536 east */
	static boolean facing(int orientation, int dx, int dy)
	{
		if (dx == 0 && dy == 0)
		{
			return true;
		}
		double a = orientation * Math.PI * 2 / 2048.0;
		double fx = -Math.sin(a);
		double fy = -Math.cos(a);
		double len = Math.sqrt(dx * dx + dy * dy);
		return (fx * dx + fy * dy) / len >= Math.cos(Math.PI / 4) - 1e-9;
	}

	/**
	 * Every tick: a new task starts a trip's clock; the artefact leaving your pack ends it,
	 * delivered when Captain Khaled's experience follows, lost otherwise.
	 */
	private void settle(Client client, Pack pack, ArtefactCycle.Snapshot s, int tick, long now)
	{
		if (client.getSkillExperience(Skill.HITPOINTS) <= 0)
		{
			return;
		}
		int xp = client.getSkillExperience(Skill.THIEVING);
		int cash = pack.count(ItemID.COINS);
		if (!read)
		{
			read = true;
			prevCarrying = s.carrying;
			prevXp = xp;
			prevCoins = cash;
			if (s.house != null && runHouse == null)
			{
				// A task already under way: kept, but not timed.
				runHouse = s.house;
				runTick = -1;
				runXp = -1;
			}
			return;
		}
		if (s.house != null && !s.carrying && taskTick == tick && pendingTick < 0)
		{
			startRun(s.house, tick, xp);
		}
		if (s.house != null && !s.carrying && pendingTick < 0)
		{
			runHouse = s.house;
		}
		if (prevCarrying && !s.carrying && pendingTick < 0)
		{
			pendingTick = tick;
			pendingXp = prevXp;
			pendingCoins = prevCoins;
		}
		if (pendingTick >= 0)
		{
			int gained = xp - pendingXp;
			boolean paid = gained >= Artefacts.deliveryXp(client.getRealSkillLevel(Skill.THIEVING)) / 2;
			if (paid && cash > pendingCoins || tick - pendingTick >= SETTLE_TICKS)
			{
				int got = paid ? Math.max(0, cash - pendingCoins) : 0;
				keep(paid, now, xp, got);
				pendingTick = -1;
				if (s.house != null && !s.carrying)
				{
					// A new house already: its trip starts now.
					startRun(s.house, tick, xp);
				}
			}
		}
		prevCarrying = s.carrying;
		prevXp = xp;
		prevCoins = cash;
	}

	private void startRun(House house, int tick, int xp)
	{
		runHouse = house;
		runTick = tick;
		runXp = xp;
	}

	private void keep(boolean paid, long now, int xp, int got)
	{
		int ticks = runTick >= 0 && pendingTick >= 0 ? pendingTick - runTick : 0;
		int gained = runXp >= 0 && xp >= 0 ? Math.max(0, xp - runXp) : paid && xp >= 0 ? Math.max(0, xp - pendingXp) : 0;
		log().add(new ArtefactLog.Run(now, ticks, runHouse, paid, gained, got));
		if (save != null)
		{
			save.accept(LOG_KEY, log().encode());
		}
		if (paid)
		{
			delivered++;
			coins += got;
		}
		else
		{
			lost++;
		}
		runTick = -1;
		runXp = -1;
		runHouse = null;
		heard = null;
	}

	private static String place(ArtefactCycle.Snapshot s)
	{
		if (s.carrying)
		{
			return "Piscarilius · artefact in hand";
		}
		return "Port Piscarilius" + (s.house == null ? "" : " · " + s.house.getTitle().toLowerCase(java.util.Locale.ROOT));
	}

	/** The district, your kit, your session, a Thieving level, your houses and your trips. */
	private void dress(View view, ArtefactCycle.Snapshot s, Client client, Pack pack, WorldPoint at, List<NPC> patrols, long now)
	{
		Map<House, WorldPoint> p = places();
		List<View.District.Place> houses = new ArrayList<>();
		for (House h : House.values())
		{
			WorldPoint w = p.get(h);
			houses.add(new View.District.Place(w == null ? h.x() : w.getX(), w == null ? h.y() : w.getY(), shortName(h), h == s.house && !s.carrying, w != null));
		}
		List<View.District.Patrol> marks = new ArrayList<>();
		for (NPC n : patrols)
		{
			WorldPoint w = n.getWorldLocation();
			marks.add(new View.District.Patrol(w.getX(), w.getY(), n.getOrientation()));
		}
		String note = s.carrying ? "To Captain Khaled" : s.house == null ? "No task" : s.house.getTitle() + (s.house.upstairs() ? ", upstairs" : ", ground floor");
		view.district(new View.District("The houses", note, houses, marks,
			khaledSeen == null ? null : new int[]{khaledSeen.getX(), khaledSeen.getY()}, new int[]{at.getX(), at.getY()}, s.carrying));

		List<View.Line> lines = new ArrayList<>();
		lines.add(new View.Line("Lockpick or hair clip", s.pick ? "Carried" : "Missing", s.pick ? View.Line.Mark.DONE : View.Line.Mark.TODO));
		lines.add(new View.Line("Artefact", s.carrying ? "On foot to Khaled" : "None yet", s.carrying ? View.Line.Mark.NEXT : View.Line.Mark.IDLE));
		lines.add(new View.Line(s.teleport ? (pack.carries(ItemID.BOOK_OF_THE_DEAD) ? "Book of the Dead" : "Kharedst's memoirs") : "Teleport back",
			s.teleport ? (s.carrying ? "Not until the hand-in" : "After each hand-in") : "None", s.teleport ? View.Line.Mark.DONE : View.Line.Mark.IDLE));
		String said = s.heardOther ? "Khaled's words: " + s.house.getTitle().toLowerCase(java.util.Locale.ROOT) : s.heard ? "As Khaled said" : null;
		view.lines("Your kit", said, lines);

		int real = client.getRealSkillLevel(Skill.THIEVING);
		view.stat(new View.Stat(Session.format(delivered), "Delivered", session.perHour(delivered, now), Theme.GOLD));
		int xp = session.xp(client);
		view.stat(new View.Stat(Session.format(xp), "Thieving xp", session.perHour(xp, now), Theme.GREEN));
		view.stat(new View.Stat(Session.format(coins), "Coins", session.perHour(coins, now), Theme.GOLD_BRIGHT));
		view.stat(new View.Stat(Session.format(lost), "Lost", lost > 0 ? "caught or teleported" : "", lost > 0 ? Theme.EMBER : Theme.TEAL));

		view.goal(goalCard(client, real));
		view.pick(housesCard(s.house, s.teleport));
		ArtefactLog g = log();
		if (g.count() > 0)
		{
			view.supply(historyCard(g, real));
		}
	}

	static String shortName(House h)
	{
		switch (h)
		{
			case NORTH:
				return "N";
			case SOUTH_EAST:
				return "SE";
			case SOUTH:
				return "S";
			case SOUTH_WEST:
				return "SW";
			case WEST:
				return "W";
			default:
				return "NW";
		}
	}

	/** A Thieving level in artefacts and time at your pace, with the diary task they count toward. */
	private View.GoalCard goalCard(Client client, int real)
	{
		int xp = client.getSkillExperience(Skill.THIEVING);
		int target = goalLevel > real ? goalLevel : Math.min(99, real + 1);
		long need = Experience.getXpForLevel(Math.max(2, target));
		boolean done = real >= 99 || xp >= need;
		long left = Math.max(0, need - xp);
		ArtefactLog g = log();
		int each = Artefacts.xpEach(real);
		int artefacts = ArtefactLog.artefactsFor(left, real);
		double hours = g.hoursFor(left);
		String note = done ? "Reached." : Session.format(artefacts) + (artefacts == 1 ? " artefact" : " artefacts") + " at " + Session.format(each)
			+ " xp each" + (hours > 0 ? ", about " + MixologyPilot.clockHours(hours) + " at your pace" : "; the time shows after your first trip");
		List<View.Line> pieces = new ArrayList<>();
		boolean diary = client.getVarbitValue(DIARY_HARD) > 0 || g.delivered() > 0;
		pieces.add(new View.Line("Hard diary: deliver an artefact", diary ? "Done" : "One hand-in", diary ? View.Line.Mark.DONE : View.Line.Mark.TODO));
		pieces.add(new View.Line("Khaled's pay, kept", Session.format(g.coins()) + " coins", View.Line.Mark.IDLE));
		return new View.GoalCard(View.Activity.ARTEFACTS, "Thieving " + target, done ? 1 : xp, done ? 1 : need, "xp", note, pieces, done);
	}

	/** The six houses: your own average trip to each beside the wiki's, the task's lit. */
	private View.PickCard housesCard(House current, boolean teleport)
	{
		ArtefactLog g = log();
		List<View.Pick> picks = new ArrayList<>();
		double longest = 1;
		for (House h : House.values())
		{
			longest = Math.max(longest, Math.max(g.typical(h) * 0.6, wikiSeconds(h, teleport)));
		}
		for (House h : House.values())
		{
			int wiki = wikiSeconds(h, teleport);
			int mine = g.typical(h);
			ArtefactLog.Run best = g.quickest(h);
			String figure = mine > 0 ? StormPilot.clock(mine) : seconds(wiki);
			String detail = mine > 0 ? "Wiki " + seconds(wiki) + " · best " + StormPilot.clock(best.ticks) + " · " + g.timed(h)
				+ (g.timed(h) == 1 ? " trip" : " trips") : "The wiki's average" + (h.upstairs() ? "; upstairs" : "; ground floor");
			picks.add(new View.Pick(h.getTitle(), figure, detail, true, h == current, (mine > 0 ? mine * 0.6 : wiki) / longest, false));
		}
		return new View.PickCard("Your houses", teleport ? "with a teleport back" : "round trip",
			"Your average where you have timed one, else the wiki's. Captain Khaled picks the house, not you.", picks);
	}

	private static int wikiSeconds(House h, boolean teleport)
	{
		return teleport ? h.wikiSecondsTeleport() : h.wikiSeconds();
	}

	/** "1:05". */
	static String seconds(int s)
	{
		return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60;
	}

	/** Your last trips, the quickest, and artefacts, experience and coins an hour across them. */
	private static View.Supply historyCard(ArtefactLog g, int level)
	{
		List<View.Line> lines = new ArrayList<>();
		List<ArtefactLog.Run> all = g.all();
		ArtefactLog.Run best = g.quickest(null);
		for (int i = all.size() - 1; i >= 0 && i >= all.size() - 5; i--)
		{
			ArtefactLog.Run r = all.get(i);
			String what = (r.house == null ? "Artefact" : r.house.getTitle()) + (r.ticks > 0 ? " · " + StormPilot.clock(r.ticks) : "");
			lines.add(new View.Line(what, r.delivered ? Session.format(r.xp) + " xp · " + Session.format(r.coins) + " gp" : "Lost",
				r == best ? View.Line.Mark.DONE : r.delivered ? View.Line.Mark.IDLE : View.Line.Mark.TODO));
		}
		String headline = best == null ? null : "Quickest: " + best.house.getTitle().toLowerCase(java.util.Locale.ROOT) + " in " + StormPilot.clock(best.ticks);
		double n = g.perHour();
		String detail = n < 0 ? null : String.format(java.util.Locale.ROOT, "%.0f an hour: %s Thieving xp and %s coins an hour at your pace (the wiki's walking pace at %d: %s xp)",
			n, Session.format(Math.round(g.xpPerHour())), Session.format(Math.round(g.coinsPerHour())), level,
			Session.format(Artefacts.wikiXpPerHour(level, false)));
		String note = g.lost() > 0 ? g.lost() + " of " + g.count() + " lost on the way" : null;
		return new View.Supply(g.delivered() + " delivered", lines, headline, detail, false, note).titled("Your artefacts");
	}

	/** Brief for the panel away from the port. */
	View.Brief brief()
	{
		ArtefactLog g = log();
		String status = null;
		ArtefactLog.Run last = g.last();
		if (last != null)
		{
			status = "Last trip: " + (last.house == null ? "" : last.house.getTitle().toLowerCase(java.util.Locale.ROOT) + ", ")
				+ (last.delivered ? Session.format(last.xp) + " Thieving xp and " + Session.format(last.coins) + " coins" : "lost on the way")
				+ (last.ticks > 0 ? " in " + StormPilot.clock(last.ticks) : "");
		}
		View.Brief b = new View.Brief(View.Activity.ARTEFACTS, status,
			"In Port Piscarilius, from 49 Thieving. Bring a lockpick or hair clip; Captain Khaled is in the foodhall on the east side.");
		double hour = g.perHour();
		if (hour >= 0)
		{
			b.best(Math.round(hour) + " artefacts, " + Session.format(Math.round(g.xpPerHour())) + " xp and "
				+ Session.format(Math.round(g.coinsPerHour())) + " coins an hour over " + g.count() + (g.count() == 1 ? " trip." : " trips."));
		}
		return b;
	}

	/** "1;N:x,y,p;...;K:x,y,p": each house's drawers by name, and Captain Khaled. */
	static String encodePlaces(Map<House, WorldPoint> drawers, WorldPoint khaled)
	{
		StringBuilder b = new StringBuilder("1");
		for (Map.Entry<House, WorldPoint> e : drawers.entrySet())
		{
			WorldPoint w = e.getValue();
			b.append(';').append(e.getKey().name()).append(':').append(w.getX()).append(',').append(w.getY()).append(',').append(w.getPlane());
		}
		if (khaled != null)
		{
			b.append(";KHALED:").append(khaled.getX()).append(',').append(khaled.getY()).append(',').append(khaled.getPlane());
		}
		return b.toString();
	}

	/** Reads a save into the map as far as it can, and returns Captain Khaled's place, or null. */
	static WorldPoint decodePlaces(String text, Map<House, WorldPoint> into)
	{
		WorldPoint khaled = null;
		if (text == null)
		{
			return null;
		}
		String[] parts = text.split(";");
		if (parts.length == 0 || !"1".equals(parts[0].trim()))
		{
			return null;
		}
		for (int i = 1; i < parts.length; i++)
		{
			int colon = parts[i].indexOf(':');
			if (colon <= 0)
			{
				continue;
			}
			String[] f = parts[i].substring(colon + 1).split(",");
			if (f.length != 3)
			{
				continue;
			}
			try
			{
				int x = Integer.parseInt(f[0].trim());
				int y = Integer.parseInt(f[1].trim());
				int plane = Integer.parseInt(f[2].trim());
				if (!Artefacts.inPort(x, y) || plane < 0 || plane > 3)
				{
					continue;
				}
				String name = parts[i].substring(0, colon).trim();
				WorldPoint w = new WorldPoint(x, y, plane);
				if ("KHALED".equals(name))
				{
					khaled = w;
				}
				else
				{
					into.put(House.valueOf(name), w);
				}
			}
			catch (IllegalArgumentException e)
			{
				// Left out.
			}
		}
		return khaled;
	}
}
