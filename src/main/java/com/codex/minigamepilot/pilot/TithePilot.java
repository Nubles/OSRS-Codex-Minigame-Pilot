package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Figures;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.core.Tithe;
import com.codex.minigamepilot.core.Tithe.Seed;
import com.codex.minigamepilot.core.Tithe.State;
import com.codex.minigamepilot.core.TitheCycle;
import com.codex.minigamepilot.core.TitheLog;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;

/**
 * Flies the Tithe Farm: the seed your Farming grows, then each patch of your round to plant, water
 * and harvest, read from the patches themselves, with the water you carry, the fruit sack, your
 * points towards a reward and every sack kept per account. It only reads the game (the patch
 * objects, what you carry, a few varbits) and draws.
 *
 * <p>Ids are RuneLite's gameval names: the patch objects {@code HOSIDIUS_TITHE_EMPTY} and
 * {@code HOSIDIUS_TITHE_A_1_DRY} to {@code _C_4_DEAD}, the seed table, the sacks, the farm door and
 * the water barrels; the varbits {@code HOSIDIUS_TITHE_REWARDPOINTS} (RuneLite's
 * {@code TITHE_FARM_POINTS}) and {@code HOSIDIUS_TITHE_SCORE} (its {@code TITHE_FARM_SACK_AMOUNT}).
 * That {@code ZEAH_WATERINGCAN_CHARGES} counts the doses in Gricoller's can is as the Tithe Farm
 * Coach plugin by GalesongAS (BSD-2) reads it: a cross-check, not a source.
 */
public final class TithePilot
{
	static final int[] PATCHES = Tithe.patchIds();
	static final int SEED_TABLE = ObjectID.TITHE_PLANT_SEED_TABLE;
	static final int[] SACKS = {ObjectID.TITHE_SACK_OF_FRUIT_EMPTY, ObjectID.TITHE_SACK_OF_FRUIT};
	static final int DOOR = ObjectID.HOSIDIUS_TITHE_FARM_DOOR;
	/** The water barrel's ids on the wiki ("5598,29101,42073", and 5599, 8702, 8703): four stand in the farm. */
	static final int[] BARRELS = {ObjectID.WATER_BARREL1, ObjectID.WATER_BARREL2, ObjectID.FARM_WATER_BARREL1, ObjectID.FARM_WATER_BARREL2};
	static final int POINTS = VarbitID.HOSIDIUS_TITHE_REWARDPOINTS;
	static final int SACK = VarbitID.HOSIDIUS_TITHE_SCORE;
	static final int CAN_CHARGES = VarbitID.ZEAH_WATERINGCAN_CHARGES;
	static final int GRICOLLER = ItemID.ZEAH_WATERINGCAN;
	/** Within this many tiles of the seed table you are about to play. */
	static final int AT_TABLE = 12;
	static final String LOG_KEY = "tithe.kept";
	static final String ROUTE_KEY = "tithe.route";
	/** Rounds shorter than this teach no route. */
	static final int LEARN_FROM = 3;

	private final Session session = new Session(Skill.FARMING);
	private Function<String, String> load;
	private BiConsumer<String, String> save;
	private Tithe.Plan plan = Tithe.Plan.TWENTY;
	private Tithe.Reward goal;
	private TitheLog log;
	/** Your route: each patch's south-west tile relative to the field's, in the order you plant them. */
	private List<int[]> route;

	/** Each patch as last seen, by where it stands. */
	private final Map<WorldPoint, Track> tracks = new HashMap<>();
	/** This round's patches in the order you planted them. */
	private final List<WorldPoint> round = new ArrayList<>();

	/** A run: from its first seed to its sack of a hundred. */
	private int runTick = -1;
	private int runXp;
	private int runPoints;
	private Seed runSeed;
	private int lastDepositTick = -1;
	private int lastSack = -1;
	private int lastPoints = -1;
	private boolean read;
	private int sacks;
	private int fruitDeposited;
	private int pointsEarned;

	Object lastState;

	private static final class Track
	{
		State state;
		int stage;
		/** The tick its stage began, or -1 when not seen. */
		int since = -1;
	}

	TithePilot()
	{
	}

	void setStore(Function<String, String> load, BiConsumer<String, String> save)
	{
		this.load = load;
		this.save = save;
	}

	void settings(Tithe.Plan plan, Tithe.Reward goal)
	{
		this.plan = plan == null ? Tithe.Plan.TWENTY : plan;
		this.goal = goal;
	}

	static boolean isWatched(int id)
	{
		return StormPilot.contains(PATCHES, id) || id == SEED_TABLE || StormPilot.contains(SACKS, id) || id == DOOR
			|| StormPilot.contains(BARRELS, id);
	}

	/** In the farm (its patches in the scene), or by the seed table before it. */
	boolean active(Client client, SceneIndex scene)
	{
		if (client.getLocalPlayer() == null)
		{
			return false;
		}
		if (!scene.get(PATCHES).isEmpty())
		{
			return true;
		}
		WorldPoint at = client.getLocalPlayer().getWorldLocation();
		for (TileObject t : scene.get(SEED_TABLE))
		{
			WorldPoint p = t.getWorldLocation();
			if (p != null && p.getPlane() == at.getPlane() && p.distanceTo2D(at) <= AT_TABLE)
			{
				return true;
			}
		}
		return false;
	}

	/** Out of the farm: "the game and score will be completely reset", so a sack begun is kept as it stands. */
	void left(long now)
	{
		if (runTick >= 0 && lastSack > 0 && lastSack < Tithe.SACK)
		{
			keep(lastSack, now, -1, -1);
		}
		tracks.clear();
		round.clear();
		runTick = -1;
		lastSack = -1;
		lastPoints = -1;
		lastDepositTick = -1;
		read = false;
	}

	void newAccount()
	{
		session.startOver();
		log = null;
		route = null;
		sacks = 0;
		fruitDeposited = 0;
		pointsEarned = 0;
		tracks.clear();
		round.clear();
		runTick = -1;
		lastSack = -1;
		lastPoints = -1;
		lastDepositTick = -1;
		read = false;
	}

	/** A running count for the pace coach: sacks filled. */
	int units()
	{
		return sacks;
	}

	private TitheLog log()
	{
		if (log == null)
		{
			log = TitheLog.decode(load == null ? null : load.apply(LOG_KEY));
		}
		return log;
	}

	private List<int[]> route()
	{
		if (route == null)
		{
			route = decodeRoute(load == null ? null : load.apply(ROUTE_KEY));
		}
		return route;
	}

	Flight fly(Client client, SceneIndex scene, long now)
	{
		session.tick(client, now);
		Pack pack = Pack.read(client);
		int tick = client.getTickCount();
		TitheCycle.Snapshot s = new TitheCycle.Snapshot();
		lastState = s;
		s.level = client.getBoostedSkillLevel(Skill.FARMING);
		s.plan = plan;
		for (Seed seed : Seed.values())
		{
			s.seeds[seed.ordinal()] = pack.count(seed.seed());
			s.fruit += pack.count(seed.fruit());
		}
		int[] cans = new int[Tithe.CANS.length];
		for (int i = 0; i < cans.length; i++)
		{
			cans[i] = pack.count(Tithe.CANS[i]);
		}
		s.doses = Tithe.canDoses(cans);
		s.gricoller = pack.carries(GRICOLLER);
		if (s.gricoller)
		{
			s.doses += Math.max(0, Math.min(Tithe.GRICOLLER_DOSES, client.getVarbitValue(CAN_CHARGES)));
		}
		s.anyCan = s.gricoller || s.doses > 0 || pack.carries(ItemID.WATERING_CAN_0);
		s.spade = pack.carries(ItemID.SPADE);
		s.dibber = pack.carries(ItemID.DIBBER);
		s.sack = Math.max(0, client.getVarbitValue(SACK));
		s.barrel = !scene.get(BARRELS).isEmpty();

		// The patches, in a fixed order: west to east, then south to north.
		List<TileObject> patches = new ArrayList<>(scene.get(PATCHES));
		patches.sort((a, b) -> a.getWorldLocation().getX() != b.getWorldLocation().getX()
			? Integer.compare(a.getWorldLocation().getX(), b.getWorldLocation().getX())
			: Integer.compare(a.getWorldLocation().getY(), b.getWorldLocation().getY()));
		s.inFarm = !patches.isEmpty();
		Map<WorldPoint, Integer> index = new HashMap<>();
		int[][] feet = new int[patches.size()][];
		for (int i = 0; i < patches.size(); i++)
		{
			TileObject o = patches.get(i);
			index.put(o.getWorldLocation(), i);
			feet[i] = Ground.footprint(o);
			s.plots.add(watch(o, tick, s));
		}
		int ox = Integer.MAX_VALUE;
		int oy = Integer.MAX_VALUE;
		for (int[] f : feet)
		{
			ox = Math.min(ox, f[0]);
			oy = Math.min(oy, f[1]);
		}
		endRound(s, patches, feet, ox, oy);
		for (WorldPoint p : round)
		{
			Integer i = index.get(p);
			if (i != null)
			{
				s.round.add(i);
			}
		}
		LocalPoint me = client.getLocalPlayer().getLocalLocation();
		int[] you = me == null || patches.isEmpty() ? null : new int[]{me.getSceneX() - ox, me.getSceneY() - oy};
		plan(s, feet, ox, oy, you);
		settle(client, s, tick, now);

		TitheCycle.Decision d = TitheCycle.next(s);
		Step step = d.step;
		Ground ground = Ground.read(client, Collections.emptyList(), 0);
		Flight flight = route(scene, ground, step, d, patches);
		View view = new View(true, View.Activity.TITHE, step, place(s), TitheCycle.LOOP, TitheCycle.stage(step, s));
		Flight out = new Flight(View.Activity.TITHE, step, view);
		out.object = flight.object;
		out.route = flight.route;
		if (s.inFarm)
		{
			// The rest of the round still wanting you, quietly.
			for (int i : s.round)
			{
				TitheCycle.Plot p = s.plots.get(i);
				if (i != d.plot && (p.state == State.DRY || p.state == State.GROWN || p.state == State.DEAD))
				{
					out.others.add(patches.get(i));
				}
			}
		}
		dress(view, s, d, client, pack, feet, ox, oy, you, now);
		return out;
	}

	/** Reads one patch and keeps its stage's start, so the time it has left is known. */
	private TitheCycle.Plot watch(TileObject o, int tick, TitheCycle.Snapshot s)
	{
		Tithe.Look look = Tithe.look(o.getId());
		WorldPoint at = o.getWorldLocation();
		Track t = tracks.get(at);
		if (t == null)
		{
			t = new Track();
			t.state = look.state;
			t.stage = look.stage;
			tracks.put(at, t);
			if (look.state != State.EMPTY && !round.contains(at))
			{
				// Planted before the Pilot saw it: tended all the same.
				round.add(at);
			}
		}
		else if (t.state != look.state || t.stage != look.stage)
		{
			if (t.state == State.EMPTY && look.state != State.EMPTY)
			{
				t.since = tick;
				if (!round.contains(at))
				{
					round.add(at);
				}
				if (runTick < 0 && read)
				{
					startRun(tick, look.seed);
				}
			}
			else if (look.stage > t.stage)
			{
				t.since = tick;
			}
			else if (look.state == State.EMPTY)
			{
				t.since = -1;
			}
			t.state = look.state;
			t.stage = look.stage;
		}
		TitheCycle.Plot p = new TitheCycle.Plot(look.state, look.stage,
			t.since >= 0 && look.state != State.EMPTY ? Math.max(0, Tithe.STAGE_TICKS - (tick - t.since)) : -1);
		p.seed = look.seed;
		return p;
	}

	/** A round whose patches are all empty again is over; what you planted, in your order, is your route. */
	private void endRound(TitheCycle.Snapshot s, List<TileObject> patches, int[][] feet, int ox, int oy)
	{
		if (round.isEmpty())
		{
			return;
		}
		for (WorldPoint p : round)
		{
			Track t = tracks.get(p);
			if (t == null || t.state != State.EMPTY)
			{
				return;
			}
		}
		if (round.size() >= LEARN_FROM)
		{
			List<int[]> learnt = new ArrayList<>();
			for (WorldPoint p : round)
			{
				for (int i = 0; i < patches.size(); i++)
				{
					if (patches.get(i).getWorldLocation().equals(p))
					{
						learnt.add(new int[]{feet[i][0] - ox, feet[i][1] - oy});
					}
				}
			}
			route = learnt;
			if (save != null)
			{
				save.accept(ROUTE_KEY, encodeRoute(learnt));
			}
		}
		round.clear();
	}

	/**
	 * The patches to plant next: the rest of your own route where it has one, then the nearest
	 * empty patch to the last one planted (or to you), and so on.
	 */
	private void plan(TitheCycle.Snapshot s, int[][] feet, int ox, int oy, int[] you)
	{
		List<Integer> free = new ArrayList<>();
		for (int i = 0; i < s.plots.size(); i++)
		{
			if (s.plots.get(i).state == State.EMPTY && !s.round.contains(i))
			{
				free.add(i);
			}
		}
		for (int[] r : route())
		{
			for (int k = 0; k < free.size(); k++)
			{
				int i = free.get(k);
				if (feet[i][0] - ox == r[0] && feet[i][1] - oy == r[1])
				{
					s.toPlant.add(i);
					free.remove(k);
					break;
				}
			}
		}
		int[] from = s.toPlant.isEmpty() ? (s.round.isEmpty() ? you : rel(feet[s.round.get(s.round.size() - 1)], ox, oy))
			: rel(feet[s.toPlant.get(s.toPlant.size() - 1)], ox, oy);
		while (!free.isEmpty() && s.toPlant.size() < Tithe.Plan.TWENTY_FIVE.plants())
		{
			int best = 0;
			for (int k = 1; from != null && k < free.size(); k++)
			{
				if (dist(rel(feet[free.get(k)], ox, oy), from) < dist(rel(feet[free.get(best)], ox, oy), from))
				{
					best = k;
				}
			}
			int i = free.remove(best);
			s.toPlant.add(i);
			from = rel(feet[i], ox, oy);
		}
	}

	private static int[] rel(int[] f, int ox, int oy)
	{
		return new int[]{f[0] - ox, f[1] - oy};
	}

	/** Walking distance between patches, roughly: tiles apart east-west and north-south, the longer counting. */
	private static int dist(int[] a, int[] b)
	{
		return Math.max(Math.abs(a[0] - b[0]), Math.abs(a[1] - b[1])) * 2 + Math.min(Math.abs(a[0] - b[0]), Math.abs(a[1] - b[1]));
	}

	private void startRun(int tick, Seed seed)
	{
		runTick = tick;
		runSeed = seed;
		runXp = -1;
		runPoints = -1;
	}

	/**
	 * Every tick in the farm: the sack filling to a hundred (or emptying after it) ends a run, kept
	 * with its points, experience and time; a rise in your points is earned this session.
	 */
	private void settle(Client client, TitheCycle.Snapshot s, int tick, long now)
	{
		if (!s.inFarm || client.getSkillExperience(Skill.HITPOINTS) <= 0)
		{
			return;
		}
		int xp = client.getSkillExperience(Skill.FARMING);
		int points = Math.max(0, client.getVarbitValue(POINTS));
		if (runTick >= 0 && runXp < 0)
		{
			runXp = xp;
			runPoints = points;
		}
		if (read)
		{
			if (points > lastPoints)
			{
				pointsEarned += points - lastPoints;
			}
			if (s.sack > lastSack)
			{
				fruitDeposited += s.sack - lastSack;
				lastDepositTick = tick;
			}
			if (s.sack >= Tithe.SACK && lastSack < Tithe.SACK || s.sack < lastSack && lastSack < Tithe.SACK)
			{
				if (s.sack < lastSack)
				{
					fruitDeposited += Tithe.SACK - lastSack;
					lastDepositTick = tick;
				}
				keep(Tithe.SACK, now, xp, points);
				sacks++;
				// The next sack runs on from here.
				runTick = tick;
				runXp = xp;
				runPoints = points;
			}
		}
		lastSack = s.sack;
		lastPoints = points;
		read = true;
		this.xpNow = xp;
		this.pointsNow = points;
	}

	private int xpNow = -1;
	private int pointsNow = -1;

	private void keep(int fruit, long now, int xp, int points)
	{
		if (runTick < 0)
		{
			return;
		}
		int x = xp >= 0 ? xp : xpNow;
		int p = points >= 0 ? points : pointsNow;
		int end = lastDepositTick >= 0 ? lastDepositTick : runTick;
		int ticks = fruit >= Tithe.SACK || end > runTick ? end - runTick : 0;
		log().add(new TitheLog.Run(now, ticks, runSeed, fruit, runPoints >= 0 && p > runPoints ? p - runPoints : 0,
			runXp >= 0 && x > runXp ? x - runXp : 0));
		if (save != null)
		{
			save.accept(LOG_KEY, log().encode());
		}
		runTick = -1;
	}

	private static Flight route(SceneIndex scene, Ground ground, Step step, TitheCycle.Decision d, List<TileObject> patches)
	{
		Flight flight = new Flight(View.Activity.TITHE, step, null);
		Collection<? extends TileObject> things;
		switch (step.getTarget())
		{
			case PLOT:
				things = d.plot < 0 ? Collections.emptySet() : Collections.singleton(patches.get(d.plot));
				break;
			case SACK:
				things = scene.get(SACKS);
				break;
			case WATER_BARREL:
				things = scene.get(BARRELS);
				break;
			case SEED_TABLE:
				things = scene.get(SEED_TABLE);
				break;
			case FARM_DOOR:
				things = scene.get(DOOR);
				break;
			default:
				return flight;
		}
		if (things.isEmpty())
		{
			return flight;
		}
		Ground.Way<TileObject> way = ground == null ? null : ground.nearest(things, null);
		if (way != null)
		{
			return flight.to(way);
		}
		flight.object = things.iterator().next();
		return flight;
	}

	private static String place(TitheCycle.Snapshot s)
	{
		if (!s.inFarm)
		{
			return "Hosidius · the seed table";
		}
		Seed seed = TitheCycle.seedCarried(s);
		return "The farm" + (seed == null ? "" : " · " + seed.getTitle().toLowerCase(Locale.ROOT));
	}

	/** The water and the sack, the farm's patches, your kit, your session, a reward and your sacks. */
	private void dress(View view, TitheCycle.Snapshot s, TitheCycle.Decision d, Client client, Pack pack, int[][] feet, int ox, int oy,
		int[] you, long now)
	{
		int owed = TitheCycle.owedInRound(s);
		Seed planting = TitheCycle.seedCarried(s);
		int room = Math.max(0, Math.min(s.plan.plants() - s.round.size(), planting == null ? 0 : s.seeds[planting.ordinal()]));
		if (!s.inFarm || !TitheCycle.roundOn(s))
		{
			// Before a round: water for a whole one.
			room = s.plan.plants();
		}
		int need = owed + room * Tithe.WATERINGS;
		if (s.gricoller)
		{
			view.gauge(new View.Gauge("Water", s.doses, 0, Tithe.GRICOLLER_DOSES,
				"Gricoller's can · about " + Figures.count(s.doses / Tithe.WATERINGS) + " plants", s.doses < need ? Theme.EMBER : Theme.TEAL));
		}
		else
		{
			view.gauge(new View.Gauge("Water", Math.min(s.doses, Math.max(need, 1)), 0, Math.max(need, 1),
				s.doses >= need ? Figures.count(s.doses) + " doses: enough for this round"
					: Figures.count(s.doses) + " doses, " + Figures.count(need) + " to finish the round", s.doses < need ? Theme.EMBER : Theme.TEAL));
		}
		if (s.inFarm)
		{
			view.gauge(new View.Gauge("Sack", Math.min(s.sack, Tithe.SACK), Math.min(s.fruit, Tithe.SACK - Math.min(s.sack, Tithe.SACK)), Tithe.SACK,
				Tithe.sackPoints(s.sack) + " points in it" + (s.fruit > 0 ? ", " + Figures.count(s.fruit) + " fruit carried" : "")
					+ (s.sack < Tithe.SACK ? "; " + Tithe.sackPoints(Tithe.SACK) + " at 100" : ""), Theme.GOLD));
			view.field(field(s, d, feet, ox, oy, you));
		}

		List<View.Line> lines = new ArrayList<>();
		Seed best = Seed.best(s.level);
		Seed carried = TitheCycle.seedCarried(s);
		if (best != null)
		{
			int seeds = carried == null ? 0 : s.seeds[carried.ordinal()];
			lines.add(new View.Line(best.getTitle() + " seeds", carried == best ? Figures.count(seeds) + " · "
				+ (seeds >= s.plan.plants() ? seeds / s.plan.plants() + " rounds" : "short") : "Take them",
				carried == best ? View.Line.Mark.DONE : View.Line.Mark.NEXT));
		}
		lines.add(new View.Line(s.gricoller ? "Gricoller's can" : "Watering cans", Figures.count(s.doses) + " doses",
			s.doses >= s.plan.doses() || s.gricoller && s.doses >= need ? View.Line.Mark.DONE : View.Line.Mark.TODO));
		lines.add(new View.Line("Spade", s.spade ? "Carried" : "Missing", s.spade ? View.Line.Mark.DONE : View.Line.Mark.TODO));
		lines.add(new View.Line("Seed dibber", s.dibber ? "Carried" : "Unless barehanded", s.dibber ? View.Line.Mark.DONE : View.Line.Mark.IDLE));
		view.lines("Your kit", s.plan.getTitle() + " · " + s.plan.cans() + " cans", lines);

		int points = Math.max(0, client.getVarbitValue(POINTS));
		view.stat(new View.Stat(Session.format(fruitDeposited), "Fruit in sacks", session.perHour(fruitDeposited, now), Theme.GOLD));
		int xp = session.xp(client);
		view.stat(new View.Stat(Session.format(xp), "Farming xp", session.perHour(xp, now), Theme.GREEN));
		view.stat(new View.Stat(Session.format(points), "Points", pointsEarned > 0 ? "+" + Session.format(pointsEarned) + " this session" : "",
			Theme.GOLD_BRIGHT));
		view.stat(new View.Stat(Session.format(sacks), "Full sacks", "", Theme.TEAL));

		if (goal != null)
		{
			view.goal(goalCard(points, pack));
		}
		TitheLog g = log();
		if (g.count() > 0)
		{
			view.supply(historyCard(g));
		}
	}

	/** The farm's patches as they stand, for the panel's map. */
	private static View.Field field(TitheCycle.Snapshot s, TitheCycle.Decision d, int[][] feet, int ox, int oy, int[] you)
	{
		List<View.Field.Cell> cells = new ArrayList<>();
		int planned = Math.min(s.toPlant.size(), TitheCycle.roomInRound(s));
		for (int i = 0; i < s.plots.size(); i++)
		{
			TitheCycle.Plot p = s.plots.get(i);
			View.Field.Need need;
			switch (p.state)
			{
				case DRY:
					need = p.ticksLeft >= 0 && p.ticksLeft <= TitheCycle.URGENT_TICKS ? View.Field.Need.URGENT : View.Field.Need.DRY;
					break;
				case WET:
					need = View.Field.Need.WET;
					break;
				case GROWN:
					need = View.Field.Need.GROWN;
					break;
				case DEAD:
					need = View.Field.Need.DEAD;
					break;
				default:
					need = View.Field.Need.EMPTY;
			}
			int order = s.round.indexOf(i) + 1;
			int plan = s.toPlant.indexOf(i);
			cells.add(new View.Field.Cell(feet[i][0] - ox, feet[i][1] - oy, Math.max(feet[i][2], 1), need,
				order > 0 ? order : plan >= 0 && plan < planned ? s.round.size() + plan + 1 : 0, i == d.plot, plan >= 0 && plan < planned));
		}
		int growing = 0;
		for (int i : s.round)
		{
			growing += s.plots.get(i).state != State.EMPTY ? 1 : 0;
		}
		String note = s.round.isEmpty() ? "New round" : growing + " of " + s.round.size() + " growing";
		return new View.Field("The farm", note, cells, you);
	}

	/** Points against the reward saved for, less pieces already owned, with the time to it at your pace. */
	private View.GoalCard goalCard(int points, Pack pack)
	{
		int need = 0;
		List<View.Line> pieces = new ArrayList<>();
		for (int i = 0; i < goal.pieces(); i++)
		{
			int[] ids = goal.piece(i);
			boolean owned = pack.carries(ids) || pack.wears(ids) || pack.banked(ids) > 0;
			if (!owned)
			{
				need += goal.price(i);
			}
			if (goal.pieces() > 1)
			{
				String[] names = {"Strawhat", "Jacket or shirt", "Boro trousers", "Boots"};
				pieces.add(new View.Line(names[i], owned ? "Owned" : goal.price(i) + " pts", owned ? View.Line.Mark.DONE : View.Line.Mark.TODO));
			}
		}
		boolean owned = need == 0;
		long have = Math.min(need, points);
		String note;
		if (owned)
		{
			note = "You have it already.";
		}
		else if (points >= need)
		{
			note = "Buy it from Farmer Gricoller now.";
		}
		else
		{
			double hours = log().hoursFor(need - points);
			int sacksLeft = log().sacksFor(need - points);
			note = hours > 0 ? "About " + MixologyPilot.clockHours(hours) + " at your pace, " + sacksLeft + (sacksLeft == 1 ? " sack" : " sacks")
				: "About " + sacksLeft + (sacksLeft == 1 ? " full sack" : " full sacks") + "; its time shows after your first";
		}
		if (pieces.isEmpty())
		{
			pieces.add(new View.Line("Points", Session.format(points), points >= need ? View.Line.Mark.DONE : View.Line.Mark.TODO));
		}
		return new View.GoalCard(View.Activity.TITHE, goal.getTitle(), owned ? 1 : have, owned ? 1 : need, "points", note, pieces, owned || points >= need);
	}

	/** Your last sacks, the quickest, and points and experience an hour across them. */
	private static View.Supply historyCard(TitheLog g)
	{
		List<View.Line> lines = new ArrayList<>();
		List<TitheLog.Run> all = g.all();
		TitheLog.Run best = g.quickest();
		for (int i = all.size() - 1; i >= 0 && i >= all.size() - 5; i--)
		{
			TitheLog.Run r = all.get(i);
			String what = (r.seed == null ? "" : r.seed.getTitle() + " ") + (r.full() ? "sack" : r.fruit + " fruit");
			lines.add(new View.Line(what + (r.ticks > 0 ? " · " + StormPilot.clock(r.ticks) : ""), r.points + " pts",
				r == best ? View.Line.Mark.DONE : View.Line.Mark.IDLE));
		}
		String headline = best == null ? null : "Quickest sack: " + StormPilot.clock(best.ticks);
		double pts = g.pointsPerHour();
		double xp = g.xpPerHour();
		String detail = pts < 0 ? null : Session.format(Math.round(pts)) + " points and " + Session.format(Math.round(xp))
			+ " Farming xp an hour over your timed sacks";
		return new View.Supply(g.count() == 1 ? "1 kept" : g.count() + " kept", lines, headline, detail, false, null).titled("Your sacks");
	}

	/** Brief for the panel away from the farm. */
	View.Brief brief()
	{
		TitheLog g = log();
		String status = null;
		TitheLog.Run last = g.last();
		if (last != null)
		{
			status = "Last sack: " + (last.seed == null ? "" : last.seed.getTitle().toLowerCase(Locale.ROOT) + ", ") + last.points + " points, "
				+ Session.format(last.xp) + " Farming xp" + (last.ticks > 0 ? " in " + StormPilot.clock(last.ticks) : "");
		}
		View.Brief b = new View.Brief(View.Activity.TITHE, status,
			"In Hosidius, from 34 Farming. Bring a spade, a seed dibber and watering cans; take seeds from the table by the door.");
		double hour = g.pointsPerHour();
		if (hour >= 0)
		{
			b.best(Session.format(Math.round(hour)) + " points and " + Session.format(Math.round(g.xpPerHour())) + " xp an hour over "
				+ g.count() + (g.count() == 1 ? " sack." : " sacks."));
		}
		return b;
	}

	/** "x,y;x,y;..." each patch's place relative to the field, in your order. */
	static String encodeRoute(List<int[]> r)
	{
		StringBuilder b = new StringBuilder();
		for (int[] p : r)
		{
			b.append(b.length() == 0 ? "" : ";").append(p[0]).append(',').append(p[1]);
		}
		return b.toString();
	}

	static List<int[]> decodeRoute(String text)
	{
		List<int[]> out = new ArrayList<>();
		if (text == null || text.isEmpty())
		{
			return out;
		}
		for (String part : text.split(";"))
		{
			String[] f = part.split(",");
			if (f.length != 2)
			{
				continue;
			}
			try
			{
				int x = Integer.parseInt(f[0].trim());
				int y = Integer.parseInt(f[1].trim());
				if (x >= 0 && y >= 0 && x < 104 && y < 104 && out.size() < Tithe.Plan.TWENTY_FIVE.plants())
				{
					boolean dup = false;
					for (int[] o : out)
					{
						dup |= o[0] == x && o[1] == y;
					}
					if (!dup)
					{
						out.add(new int[]{x, y});
					}
				}
			}
			catch (NumberFormatException e)
			{
				// Left out.
			}
		}
		return out;
	}
}
