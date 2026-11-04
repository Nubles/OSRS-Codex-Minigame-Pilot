package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.core.StormCycle;
import com.codex.minigamepilot.core.StormGames;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

/**
 * Flies Tempoross: the one next step through a game, its storm read off the HUD, a tether before
 * each colossal wave, and your points and permits game by game. It only reads the game (the HUD,
 * two varbits, what you carry and the objects and characters around you) and draws.
 *
 * <p>Ids are RuneLite's gameval names. The fishing spots, the spirit pool, the ammunition crates
 * and fires are characters ({@code NpcID.TEMPOROSS_*}); the shrine, totems, masts, supply crates
 * and pumps objects. Which spot is the double one ({@code TEMPOROSS_HARPOONFISH_FISHINGSPOT_SPECIAL}),
 * and the tether objects as the game spawns them ({@code TEMPOROSS_MAST_BOTTOM_*},
 * {@code TEMPOROSS_TOTEM_*}), are cross-checked with the Easy Tempoross and Tempoross Wave
 * Timer plugins; the names are RuneLite's.
 */
public final class StormPilot
{
	/** The wave's warning in the chat (OSRS wiki, Tempoross: "A colossal wave closes in..."). */
	static final String WAVE = "A colossal wave closes in";
	/**
	 * The wave has passed, tethered or not. These two wordings are as the Easy Tempoross plugin
	 * reads them (a cross-check, not a source); the tether varbit and a time limit also end it.
	 */
	static final String[] WAVE_OVER = {"the rope keeps you securely", "the wave slams into you"};
	/**
	 * Storm intensity at which the game is lost and its points with it (OSRS wiki, Tempoross: "If
	 * storm intensity hits 100% before Tempoross's energy is fully depleted, the encounter fails").
	 * The "storm is out of control" message after three energy rounds is not the end: the wiki's
	 * strategy page says the game can still be saved by loading two cannons.
	 */
	static final int WASHED_AT = 100;
	/** No wave the Pilot has been told of lasts longer than this, in ticks: its own patience, not a game fact. */
	static final int WAVE_PATIENCE = 20;
	/** A fire this close to a tile of your walk, or to where you are going, is in the way. */
	private static final int IN_THE_WAY = 1;
	/** Ticks after a game to watch the permit count for what it paid. */
	private static final int PERMIT_WATCH = 100;
	static final String GAMES_KEY = "tempoross.games";

	static final int[] SPOTS = {NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_NORTH, NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_SOUTH,
		NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_SPECIAL};
	static final int DOUBLE_SPOT = NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_SPECIAL;
	static final int SPIRIT_POOL = NpcID.TEMPOROSS_P2_FISHINGSPOT;
	static final int[] CRATES = {NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_1, NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_2,
		NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_3, NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_4};
	static final int FIRE = NpcID.TEMPOROSS_FIRE_HITBOX;
	static final int[] VICTORY = {NpcID.TEMPOROSS_INSTANCE_HOST_E_VICTORY, NpcID.TEMPOROSS_INSTANCE_HOST_W_VICTORY,
		NpcID.TEMPOROSS_INSTANCE_HOST_N_VICTORY, NpcID.TEMPOROSS_INSTANCE_HOST_S_VICTORY};

	static final int SHRINE = ObjectID.TEMPOROSS_SHRINE_FIRE;
	static final int[] TETHERS = {ObjectID.TEMPOROSS_MAST_BOTTOM_WEST, ObjectID.TEMPOROSS_MAST_BOTTOM_EAST,
		ObjectID.TEMPOROSS_TOTEM_NORTH, ObjectID.TEMPOROSS_TOTEM_SOUTH, ObjectID.TEMPOROSS_TOTEM_TETHERED,
		ObjectID.TEMPOROSS_TOTEM_UNTETHERED, ObjectID.TEMPOROSS_MAST_BOTTOM_TETHERED, ObjectID.TEMPOROSS_MAST_BOTTOM_UNTETHERED,
		ObjectID.TEMPOROSS_MAST_BOTTOM_WEST_BROKEN, ObjectID.TEMPOROSS_MAST_BOTTOM_EAST_BROKEN,
		ObjectID.TEMPOROSS_TOTEM_NORTH_BROKEN, ObjectID.TEMPOROSS_TOTEM_SOUTH_BROKEN};
	/** Damaged totems and masts: no tethering until repaired (OSRS wiki, Totem pole). */
	static final int[] BROKEN = {ObjectID.TEMPOROSS_MAST_BOTTOM_WEST_BROKEN, ObjectID.TEMPOROSS_MAST_BOTTOM_EAST_BROKEN,
		ObjectID.TEMPOROSS_TOTEM_NORTH_BROKEN, ObjectID.TEMPOROSS_TOTEM_SOUTH_BROKEN};
	static final int[] PUMPS = {ObjectID.TEMPOROSS_WATER_PUMP, ObjectID.TEMPOROSS_WATER_PUMP_2, ObjectID.TEMPOROSS_WATER_PUMP_DOCK};

	static final int[] HARPOONS = {ItemID.HARPOON, ItemID.HUNTING_BARBED_HARPOON, ItemID.DRAGON_HARPOON, ItemID.INFERNAL_HARPOON,
		ItemID.INFERNAL_HARPOON_EMPTY, ItemID.CRYSTAL_HARPOON, ItemID.CRYSTAL_HARPOON_INACTIVE, ItemID.TRAILBLAZER_HARPOON,
		ItemID.TRAILBLAZER_RELOADED_HARPOON};
	/** The whole set acts as a rope (OSRS wiki, Spirit angler outfit). */
	static final int[] ANGLER = {ItemID.SPIRIT_ANGLER_HAT, ItemID.SPIRIT_ANGLER_TOP, ItemID.SPIRIT_ANGLER_LEGS, ItemID.SPIRIT_ANGLER_BOOTS};

	private static final Pattern PERCENT = Pattern.compile("(\\d{1,3})\\s*%");
	private static final Pattern NUMBER = Pattern.compile("(\\d[\\d,]*)");

	private final Session session = new Session(Skill.FISHING);
	private Function<String, String> load;
	private BiConsumer<String, String> save;
	private StormGames games;
	private boolean cook = true;
	private int stormLoad = 80;

	/** The tick the wave was called, or -1. */
	private int waveTick = -1;
	/** The wave was called by a message the fly has not seen yet. */
	private boolean wavePending;
	private boolean wasTethered;
	/** Ticks from a wave's warning to your tether: the last, and your best this session. */
	private int lastTether = -1;
	private int bestTether = -1;
	private int waves;
	private int wavesHeld;

	/** The game under way: its first tick, most points, and whether it was won. */
	private int gameStart = -1;
	private int gameLast;
	private int gamePoints;
	private boolean won;
	private boolean washed;
	private int permitsAtStart = -1;
	/** After a game: the tick it ended and the permits before it, to read what it paid. */
	private int watchUntil = -1;
	private int watchFrom = -1;
	private boolean lastWasWork;
	private int lastStage;
	private int catches;
	private int lastFish = -1;
	Object lastState;

	void setStore(Function<String, String> load, BiConsumer<String, String> save)
	{
		this.load = load;
		this.save = save;
	}

	void setCook(boolean on)
	{
		cook = on;
	}

	void setStormLoad(int percent)
	{
		stormLoad = percent;
	}

	static boolean isWatched(int id)
	{
		return id == SHRINE || contains(TETHERS, id) || contains(PUMPS, id) || id == ObjectID.TEMPOROSS_CRATE_HARPOON
			|| id == ObjectID.TEMPOROSS_CRATE_ROPE || id == ObjectID.TEMPOROSS_CRATE_BUCKET || id == ObjectID.TEMPOROSS_CRATE_HAMMER;
	}

	static boolean contains(int[] ids, int id)
	{
		for (int i : ids)
		{
			if (i == id)
			{
				return true;
			}
		}
		return false;
	}

	private StormGames games()
	{
		if (games == null)
		{
			games = StormGames.decode(load == null ? null : load.apply(GAMES_KEY));
		}
		return games;
	}

	/** At Tempoross Cove: its HUD is up, or its crates or fishing spots are in sight. */
	boolean active(Client client)
	{
		if (client.getLocalPlayer() == null)
		{
			return false;
		}
		if (hudShown(client))
		{
			return true;
		}
		for (NPC n : client.getTopLevelWorldView().npcs())
		{
			if (n != null && (contains(CRATES, n.getId()) || contains(SPOTS, n.getId()) || n.getId() == SPIRIT_POOL))
			{
				return true;
			}
		}
		return false;
	}

	static boolean hudShown(Client client)
	{
		Widget w = client.getWidget(InterfaceID.TemporossHud.CONTENT);
		return w != null && !w.isHidden();
	}

	/** A game message, tags removed. */
	void chat(String message)
	{
		if (message == null)
		{
			return;
		}
		if (message.contains(WAVE))
		{
			wavePending = true;
			return;
		}
		for (String over : WAVE_OVER)
		{
			if (message.contains(over))
			{
				waveTick = -1;
				wavePending = false;
			}
		}
	}

	/** Out of the cove: a game under way ends here. */
	void left(int tick, long now)
	{
		endGame(tick, now);
		waveTick = -1;
		wavePending = false;
		wasTethered = false;
	}

	void newAccount()
	{
		session.startOver();
		games = null;
		gameStart = -1;
		watchUntil = -1;
		lastTether = -1;
		bestTether = -1;
		waves = 0;
		wavesHeld = 0;
		catches = 0;
		lastFish = -1;
	}

	/** A running count for the pace coach: harpoonfish caught. */
	int units()
	{
		return catches;
	}

	/** Every tick, in the cove or not: what the last game paid, once the game's count shows it. */
	void settle(Client client)
	{
		if (watchUntil < 0)
		{
			return;
		}
		int tick = client.getTickCount();
		int permits = client.getVarbitValue(VarbitID.TEMPOROSS_REWARDPERMITS);
		if (watchFrom >= 0 && permits > watchFrom)
		{
			games().correctLast(permits - watchFrom);
			persist();
			watchUntil = -1;
		}
		else if (tick > watchUntil)
		{
			watchUntil = -1;
		}
	}

	private void persist()
	{
		if (save != null)
		{
			save.accept(GAMES_KEY, games().encode());
		}
	}

	private void endGame(int tick, long now)
	{
		if (gameStart < 0)
		{
			return;
		}
		int ticks = Math.max(0, gameLast - gameStart);
		if (gamePoints > 0 || ticks > 50)
		{
			// Washed away, the points are lost (OSRS wiki); won, they pay at least the sure permits.
			int permits = won && !washed ? StormGames.sure(gamePoints) : 0;
			games().add(new StormGames.Game(now, ticks, washed ? 0 : gamePoints, permits, won && !washed));
			persist();
			watchFrom = permitsAtStart;
			watchUntil = tick + PERMIT_WATCH;
		}
		gameStart = -1;
		gamePoints = 0;
		won = false;
		washed = false;
		permitsAtStart = -1;
	}

	/** A HUD bar's percentage from its title, or -1 when not shown. */
	static int percent(Client client, int component)
	{
		Widget w = client.getWidget(component);
		if (w == null || w.isHidden() || w.getText() == null)
		{
			return -1;
		}
		Matcher m = PERCENT.matcher(w.getText());
		return m.find() ? Math.min(100, Integer.parseInt(m.group(1))) : -1;
	}

	/** This game's points: the HUD's figure, else the game's own count. */
	static int points(Client client)
	{
		Widget w = client.getWidget(InterfaceID.TemporossHud.POINTS_TEXT);
		if (w != null && !w.isHidden() && w.getText() != null)
		{
			Matcher m = NUMBER.matcher(w.getText());
			if (m.find())
			{
				try
				{
					return Integer.parseInt(m.group(1).replace(",", ""));
				}
				catch (NumberFormatException e)
				{
					// Fall back to the varbit.
				}
			}
		}
		return client.getVarbitValue(VarbitID.TEMPOROSS_CURRENT_POINTS);
	}

	Flight fly(Client client, SceneIndex scene, long now)
	{
		session.tick(client, now);
		Pack pack = Pack.read(client);
		Player me = client.getLocalPlayer();
		WorldPoint at = me.getWorldLocation();
		int tick = client.getTickCount();
		List<NPC> npcs = new ArrayList<>();
		for (NPC n : client.getTopLevelWorldView().npcs())
		{
			if (n != null)
			{
				npcs.add(n);
			}
		}

		StormCycle.Snapshot s = new StormCycle.Snapshot();
		lastState = s;
		s.inGame = hudShown(client);
		s.energy = percent(client, InterfaceID.TemporossHud.ENERGY_TITLE);
		s.essence = percent(client, InterfaceID.TemporossHud.ESSENCE_TITLE);
		s.storm = percent(client, InterfaceID.TemporossHud.STORM_INTENSITY_TITLE);
		int points = s.inGame ? points(client) : 0;

		// The game's life: started with the HUD, won when the victory host shows or essence is gone.
		if (s.inGame)
		{
			if (gameStart < 0)
			{
				gameStart = tick;
				permitsAtStart = client.getVarbitValue(VarbitID.TEMPOROSS_REWARDPERMITS);
			}
			gameLast = tick;
			gamePoints = Math.max(gamePoints, points);
			if (s.storm >= WASHED_AT && s.essence != 0)
			{
				washed = true;
			}
			if (s.essence == 0)
			{
				won = true;
			}
		}
		if (!byId(npcs, VICTORY).isEmpty())
		{
			won = true;
		}
		if (!s.inGame && gameStart >= 0)
		{
			endGame(tick, now);
		}

		// The wave: called by the chat, held by the tether varbit, over when it lets go.
		boolean tethered = client.getVarbitValue(VarbitID.TEMPOROSS_TETHERED) > 0;
		if (wavePending)
		{
			wavePending = false;
			waveTick = tick;
			waves++;
			if (tethered)
			{
				lastTether = 0;
				bestTether = 0;
				wavesHeld++;
			}
		}
		if (waveTick >= 0)
		{
			if (tethered && !wasTethered)
			{
				lastTether = tick - waveTick;
				bestTether = bestTether < 0 ? lastTether : Math.min(bestTether, lastTether);
				wavesHeld++;
			}
			else if (!tethered && wasTethered || tick - waveTick > WAVE_PATIENCE)
			{
				waveTick = -1;
			}
		}
		wasTethered = tethered;
		s.waveIncoming = waveTick >= 0;
		s.tethered = tethered;
		s.canTether = pack.carries(ItemID.ROPE) || wearsAll(pack, ANGLER);
		s.ropeCrate = !scene.get(ObjectID.TEMPOROSS_CRATE_ROPE).isEmpty();
		List<TileObject> tethers = tethers(client, scene);
		s.tetherInSight = !tethers.isEmpty();

		s.raw = pack.count(ItemID.TEMPOROSS_RAW_HARPOONFISH);
		s.cooked = pack.count(ItemID.TEMPOROSS_HARPOONFISH);
		s.crystallised = pack.count(ItemID.TEMPOROSS_CRYSTALLISED_HARPOONFISH);
		int fish = StormCycle.fish(s);
		if (lastFish >= 0 && s.raw + s.crystallised > 0 && fish > lastFish)
		{
			catches += fish - lastFish;
		}
		lastFish = fish;
		s.free = pack.free();
		s.harpoon = pack.carries(HARPOONS) || pack.wears(HARPOONS);
		List<NPC> spots = byId(npcs, SPOTS);
		List<NPC> doubles = byId(npcs, DOUBLE_SPOT);
		s.spot = !spots.isEmpty();
		s.doubleSpot = !doubles.isEmpty();
		s.poolActive = !byId(npcs, SPIRIT_POOL).isEmpty();
		s.cook = cook;
		s.stormLoad = stormLoad;

		Step step = StormCycle.next(s);
		Ground ground = Ground.read(client, Collections.emptyList(), 0);
		Flight flight = route(client, scene, ground, step, npcs, spots, doubles, tethers, at);

		// A fire on the way: douse it first (not while a wave is due: the wave puts fires out).
		if (!s.waveIncoming && !flight.route.isEmpty() && step.getTarget() != Step.Target.TETHER)
		{
			NPC fire = fireInTheWay(byId(npcs, FIRE), flight);
			if (fire != null)
			{
				String place = Coach.place(step);
				if (pack.carries(ItemID.BUCKET_WATER))
				{
					Step douse = StormCycle.douse(step, place == null ? "the next step" : place);
					Flight f = new Flight(View.Activity.TEMPOROSS, douse, null);
					Ground.Way<NPC> way = ground == null ? null : ground.nearestNpc(Collections.singletonList(fire));
					f.to(way);
					if (f.npc == null)
					{
						f.npc = fire;
					}
					step = douse;
					flight = f;
				}
				else if (pack.carries(ItemID.BUCKET_EMPTY) && !scene.get(PUMPS).isEmpty())
				{
					Step fill = StormCycle.fillBuckets(place == null ? "the next step" : place);
					step = fill;
					flight = route(client, scene, ground, fill, npcs, spots, doubles, tethers, at);
				}
			}
		}

		int stage = StormCycle.stage(step);
		if (stage < 0)
		{
			stage = lastStage;
		}
		lastStage = stage;
		View view = new View(true, View.Activity.TEMPOROSS, step, place(s), StormCycle.LOOP, stage);
		Flight out = new Flight(View.Activity.TEMPOROSS, step, view);
		out.object = flight.object;
		out.npc = flight.npc;
		out.route = flight.route;
		out.destination = flight.destination;
		dress(view, s, points, client, tick, now);
		return out;
	}

	private static boolean wearsAll(Pack pack, int[] ids)
	{
		for (int id : ids)
		{
			if (!pack.wears(id))
			{
				return false;
			}
		}
		return true;
	}

	private static List<NPC> byId(List<NPC> npcs, int... ids)
	{
		List<NPC> out = new ArrayList<>();
		for (NPC n : npcs)
		{
			if (contains(ids, n.getId()))
			{
				out.add(n);
			}
		}
		return out;
	}

	/** Totems and masts that can be tethered to now: the broken ones left out. */
	static List<TileObject> tethers(Client client, SceneIndex scene)
	{
		List<TileObject> out = new ArrayList<>();
		for (TileObject o : scene.get(TETHERS))
		{
			if (!contains(BROKEN, shownId(client, o.getId())))
			{
				out.add(o);
			}
		}
		return out;
	}

	/** What a multiloc shows now, or the id itself. */
	private static int shownId(Client client, int id)
	{
		ObjectComposition def = client.getObjectDefinition(id);
		if (def != null && def.getImpostorIds() != null)
		{
			ObjectComposition now = def.getImpostor();
			if (now != null)
			{
				return now.getId();
			}
		}
		return id;
	}

	/** The first fire next to a tile of the walk or to where it ends, or null. */
	static NPC fireInTheWay(List<NPC> fires, Flight flight)
	{
		if (fires.isEmpty())
		{
			return null;
		}
		List<WorldPoint> tiles = new ArrayList<>(flight.route);
		if (flight.npc != null)
		{
			tiles.add(flight.npc.getWorldLocation());
		}
		if (flight.object != null)
		{
			tiles.add(flight.object.getWorldLocation());
		}
		for (NPC fire : fires)
		{
			WorldPoint f = fire.getWorldLocation();
			for (WorldPoint t : tiles)
			{
				if (t.getPlane() == f.getPlane() && Math.max(Math.abs(t.getX() - f.getX()), Math.abs(t.getY() - f.getY())) <= IN_THE_WAY)
				{
					return fire;
				}
			}
		}
		return null;
	}

	private Flight route(Client client, SceneIndex scene, Ground ground, Step step, List<NPC> npcs, List<NPC> spots,
		List<NPC> doubles, List<TileObject> tethers, WorldPoint at)
	{
		Flight flight = new Flight(View.Activity.TEMPOROSS, step, null);
		Collection<? extends TileObject> things = null;
		List<NPC> who = null;
		switch (step.getTarget())
		{
			case FISHING_SPOT:
				who = doubles.isEmpty() ? spots : doubles;
				break;
			case SPIRIT_POOL:
				who = byId(npcs, SPIRIT_POOL);
				break;
			case AMMO_CRATE:
				who = byId(npcs, CRATES);
				break;
			case SHRINE:
				things = scene.get(SHRINE);
				break;
			case TETHER:
				things = tethers;
				break;
			case WATER_PUMP:
				things = scene.get(PUMPS);
				break;
			case SUPPLY_CRATE:
				things = scene.get(step.getTitle().contains("rope") ? ObjectID.TEMPOROSS_CRATE_ROPE : ObjectID.TEMPOROSS_CRATE_HARPOON);
				break;
			default:
				break;
		}
		if (who != null)
		{
			if (who.isEmpty())
			{
				return flight;
			}
			Ground.Way<NPC> way = ground == null ? null : ground.nearestNpc(who);
			if (way != null)
			{
				return flight.to(way);
			}
			NPC best = null;
			for (NPC n : who)
			{
				if (best == null || n.getWorldLocation().distanceTo(at) < best.getWorldLocation().distanceTo(at))
				{
					best = n;
				}
			}
			flight.npc = best;
			return flight;
		}
		if (things == null || things.isEmpty())
		{
			return flight;
		}
		// Holding fast: light the tether you are on, with no walk.
		Ground.Way<TileObject> way = ground == null || step.isWaiting() ? null : ground.nearest(things, null);
		if (way != null)
		{
			return flight.to(way);
		}
		TileObject best = null;
		for (TileObject o : things)
		{
			if (best == null || o.getWorldLocation().distanceTo(at) < best.getWorldLocation().distanceTo(at))
			{
				best = o;
			}
		}
		flight.object = best;
		return flight;
	}

	private static String place(StormCycle.Snapshot s)
	{
		if (!s.inGame)
		{
			return "Tempoross Cove";
		}
		if (s.poolActive)
		{
			return "Tempoross is down";
		}
		return s.waveIncoming ? "A wave is coming" : "Tempoross Cove";
	}

	/** The storm's gauges, this game's points and permits, the wave coach and your games. */
	private void dress(View view, StormCycle.Snapshot s, int points, Client client, int tick, long now)
	{
		if (s.energy >= 0)
		{
			view.gauge(new View.Gauge("Energy", s.energy, 0, 100,
				s.energy == 0 ? "Down: harpoon the spirit pool" : "Fish in the crates drain it", Theme.GOLD));
		}
		if (s.essence >= 0)
		{
			view.gauge(new View.Gauge("Essence", s.essence, 0, 100,
				s.essence == 0 ? "Subdued" : "Taken at the spirit pool", Theme.TEAL));
		}
		if (s.storm >= 0)
		{
			boolean high = s.stormLoad > 0 && s.stormLoad <= 100 && s.storm >= s.stormLoad;
			view.gauge(new View.Gauge("Storm", s.storm, 0, 100,
				high ? "Load now: 100% washes you away" : "At 100% this game's points are lost", high ? Theme.BLOOD : Theme.EMBER));
		}
		int fish = StormCycle.fish(s);
		if (s.inGame && fish > 0 && !s.poolActive)
		{
			view.gauge(new View.Gauge("Fish held", fish, 0, fish + s.free,
				s.cooked > 0 ? s.cooked + " cooked · " + StormCycle.energyOff(s) + " energy" : StormCycle.energyOff(s) + " energy when loaded", Theme.GOLD_BRIGHT));
		}

		if (s.inGame)
		{
			int sure = StormGames.sure(points);
			int chance = (int) Math.round(StormGames.chance(points) * 100);
			view.stat(new View.Stat(Session.format(points), "Points this game",
				points >= StormGames.MAX_POINTS ? "The most a game pays" : Session.format(StormGames.toNext(points)) + " to the next", Theme.GOLD_BRIGHT));
			view.stat(new View.Stat(Integer.toString(sure), "Permits so far",
				chance > 0 ? "+" + chance + "% chance" : sure == 0 ? "First at 2,000" : "If subdued", Theme.GOLD));
		}
		int held = client.getVarbitValue(VarbitID.TEMPOROSS_REWARDPERMITS);
		if (held > 0)
		{
			view.stat(new View.Stat(Integer.toString(held), "Permits to spend", "At the pool", Theme.TEAL));
		}
		StormGames g = games();
		double perHour = g.permitsPerHour();
		if (perHour >= 0)
		{
			view.stat(new View.Stat(String.format(java.util.Locale.ROOT, "%.1f", perHour), "Permits an hour",
				"Over " + g.count() + (g.count() == 1 ? " game" : " games"), Theme.GOLD));
		}
		if (waves > 0)
		{
			view.stat(new View.Stat(lastTether < 0 ? "-" : lastTether + "t", "Tether time",
				wavesHeld + " of " + waves + " held" + (bestTether >= 0 && bestTether < lastTether ? " · best " + bestTether + "t" : ""), Theme.TEAL));
		}
		view.stat(new View.Stat(Session.format(catches), "Harpoonfish", session.perHour(catches, now), Theme.GOLD));
		int xp = session.xp(client);
		view.stat(new View.Stat(Session.format(xp), "Fishing xp", session.perHour(xp, now), Theme.GREEN));

		if (g.count() > 0)
		{
			List<View.Line> lines = new ArrayList<>();
			List<StormGames.Game> all = g.all();
			for (int i = all.size() - 1; i >= 0 && i >= all.size() - 5; i--)
			{
				StormGames.Game game = all.get(i);
				lines.add(new View.Line((game.won ? "Subdued" : "Lost") + " · " + clock(game.ticks),
					Session.format(game.points) + " pts · " + game.permits + (game.permits == 1 ? " permit" : " permits"),
					game.won ? View.Line.Mark.DONE : View.Line.Mark.IDLE));
			}
			view.lines("Your games", g.count() == 1 ? "1 kept" : g.count() + " kept", lines);
		}
	}

	/** "9 permits an hour over 6 games", or null. */
	static String pace(StormGames g)
	{
		double perHour = g.permitsPerHour();
		if (perHour < 0)
		{
			return null;
		}
		return String.format(java.util.Locale.ROOT, "%.1f", perHour) + " permits an hour over " + g.count() + (g.count() == 1 ? " game" : " games");
	}

	/** "9:12" for ticks. */
	static String clock(int ticks)
	{
		int seconds = ticks * 6 / 10;
		return seconds / 60 + ":" + String.format("%02d", seconds % 60);
	}

	/** Brief for the panel away from the minigame. */
	View.Brief brief()
	{
		StormGames g = games();
		String status = null;
		StormGames.Game last = g.last();
		if (last != null)
		{
			status = "Last game: " + Session.format(last.points) + " points, " + last.permits + (last.permits == 1 ? " permit" : " permits")
				+ " in " + clock(last.ticks) + (last.won ? "" : " (lost)");
		}
		View.Brief b = new View.Brief(View.Activity.TEMPOROSS, status,
			"Tempoross Cove, from the Ruins of Unkah, at 35 Fishing. Bring a harpoon, rope, bucket and hammer, or take them from the crates.");
		String pace = pace(g);
		StormGames.Game best = g.best();
		if (pace != null && best != null)
		{
			b.best("Best game " + Session.format(best.points) + " points; " + pace + ".");
		}
		return b;
	}
}
