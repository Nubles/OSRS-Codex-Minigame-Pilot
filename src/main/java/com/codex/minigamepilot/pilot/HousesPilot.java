package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.HouseCycle;
import com.codex.minigamepilot.core.HouseLog;
import com.codex.minigamepilot.core.Houses;
import com.codex.minigamepilot.core.Houses.House;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

/**
 * Flies stealing valuables in Civitas illa Fortis: the citizen an urchin has distracted, the
 * vacant house once its owner has walked off and how long you have, its chests, wardrobes and
 * jewellery cases, the open window out before the owner is back, and Oriana for the coins. Every
 * haul is kept per account with its valuables, its jewellery at Grand Exchange prices, its
 * experience and the time of the round. It only reads the game (the chat, what you carry, the
 * doors, windows and furniture, and the characters in view) and draws.
 *
 * <p>Ids are RuneLite's gameval names ({@code VARLAMORE_THIEVING_HOUSE_*},
 * {@code VARLAMORE_WEALTHY_CITIZEN_*}, {@code VARLAMORE_THIEVING_STREET_URCHIN_*}). Two readings are
 * the Pilot's own, as the wiki gives no marker: the distracted citizen is the one an urchin is
 * interacting with (else the citizen nearest that urchin), and you are inside from the key going
 * into a Locked Door until you stand clear of the furniture around that door.
 */
public final class HousesPilot
{
	static final String LOG_KEY = "valuables.kept";
	static final String PLACES_KEY = "valuables.places";
	static final String RATE_KEY = "valuables.rate";
	/** Within this many tiles of a citizen, an owner, an urchin, Oriana or a house's door, you are at it. */
	static final int NEAR = 40;
	/** An owner within this many tiles of the door is "still near" (the Pilot's margin; the wiki gives no distance). */
	static final int OWNER_NEAR = 4;
	/** An owner within this many tiles of a door is taken to own it. */
	static final int OWNS = 8;
	/** A key gone within this many tiles of a Locked Door went into it. */
	static final int AT_DOOR = 3;
	/** Furniture within this many tiles of a door is that house's. */
	static final int HOUSE_SIZE = 12;
	/** Owners within this many tiles are marked. */
	static final int MARK = 15;
	/** Thieving experience within this many ticks (three minutes) means you are at it. */
	static final int ENGAGED = 300;
	/** Valuables arriving within this many ticks means a search is running. */
	static final int SEARCH_TICKS = 4;

	private final Session session = new Session(Skill.THIEVING);
	private Function<String, String> load;
	private BiConsumer<String, String> save;
	private IntUnaryOperator price = id -> 0;
	private boolean markOwners = true;
	private int goalLevel;
	private HouseLog log;
	/** Each house's door and Oriana, where last seen, per account. */
	private Map<House, WorldPoint> doors;
	private WorldPoint orianaSeen;
	private int rate = -1;

	// The chat, waiting for the next tick.
	private boolean heardDistract;
	private boolean heardBack;
	private boolean heardNear;
	private boolean heardShine;
	private boolean heardEscorted;
	private int distractTick = -1;
	private int backTick = -1;
	private int nearTick = -1;
	private int shineTick = -1;

	// When each Locked Door appeared where an unlocked door stood: the owner leaving.
	private final Map<WorldPoint, Integer> lockedAt = new HashMap<>();
	private Set<WorldPoint> lastUnlocked = new HashSet<>();

	// The last tick.
	private boolean read;
	private int prevKeys;
	private int prevValuables;
	private int prevCoins;
	private int prevXp;
	private WorldPoint prevAt;
	private Map<Integer, Integer> prevContents = Collections.emptyMap();
	private int gainTick = -1;
	private int seenXp = -1;
	private int xpTick = -1;

	// A house: from the key in the door to the way out.
	private boolean inside;
	private House insideHouse;
	private WorldPoint insideDoor;
	private int startTick;
	private int startValuables;
	private int startXp;
	private int mostValuables;
	private Map<Integer, Integer> startContents = Collections.emptyMap();
	private int outsideTicks;
	private int lastEndTick = -1;
	private int lastEndXp = -1;

	// This session.
	private int houses;
	private long valuables;
	private int keysGained;
	private long hauled;
	private long coins;

	Object lastState;

	HousesPilot()
	{
	}

	void setStore(Function<String, String> load, BiConsumer<String, String> save)
	{
		this.load = load;
		this.save = save;
	}

	void setPrice(IntUnaryOperator price)
	{
		this.price = price == null ? id -> 0 : price;
	}

	/** @param goalLevel the Thieving level to work toward, 0 for the next */
	void settings(boolean markOwners, int goalLevel)
	{
		this.markOwners = markOwners;
		this.goalLevel = Math.max(0, Math.min(99, goalLevel));
	}

	static boolean isWatched(int id)
	{
		return Houses.isDoor(id) || Houses.isLoot(id) || Houses.isWindow(id);
	}

	/** The game's messages about the houses and the urchins. */
	void chat(String message)
	{
		if (message == null)
		{
			return;
		}
		String m = message.trim();
		if (m.equals(Houses.DISTRACTED))
		{
			heardDistract = true;
		}
		else if (m.equals(Houses.OWNER_BACK))
		{
			heardBack = true;
		}
		else if (m.equals(Houses.STILL_NEAR))
		{
			heardNear = true;
		}
		else if (m.equals(Houses.SHINE))
		{
			heardShine = true;
		}
		else if (m.equals(Houses.ESCORTED))
		{
			heardEscorted = true;
		}
	}

	/**
	 * Inside a house, or in Civitas illa Fortis by the citizens, the houses or Oriana while at it:
	 * carrying house keys or valuables, or with Thieving experience in the last three minutes. A
	 * passer-by in the bazaar is left alone.
	 */
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
		if (inside)
		{
			return true;
		}
		int tick = client.getTickCount();
		int xp = client.getSkillExperience(Skill.THIEVING);
		if (xp > 0)
		{
			if (seenXp > 0 && xp > seenXp)
			{
				xpTick = tick;
			}
			seenXp = xp;
		}
		Pack pack = Pack.read(client);
		if (!pack.carries(Houses.KEY, Houses.VALUABLES) && (xpTick < 0 || tick - xpTick > ENGAGED))
		{
			return false;
		}
		for (NPC n : npcs)
		{
			int id = n.getId();
			WorldPoint p = n.getWorldLocation();
			if (p != null && p.getPlane() == at.getPlane() && p.distanceTo2D(at) <= NEAR
				&& (Houses.isCitizen(id) || Houses.isDistracter(id) || Houses.isTeller(id) || Houses.isOriana(id) || House.byOwner(id) != null))
			{
				return true;
			}
		}
		for (TileObject o : scene.get(Houses.DOOR_LOCKED, Houses.DOOR))
		{
			if (o.getWorldLocation().distanceTo2D(at) <= NEAR)
			{
				return true;
			}
		}
		return false;
	}

	/** Out of the city: the tick's memory goes, and a house left by walking is ended. */
	void left(long now)
	{
		if (inside && read)
		{
			end(prevValuables, prevXp, prevContents, now, startTick + 1);
		}
		read = false;
		lastEndTick = -1;
		lastEndXp = -1;
		lockedAt.clear();
		lastUnlocked = new HashSet<>();
	}

	/** Logged out or hopped: "logging out and in" leaves the house with what you hold. */
	void relogged(long now)
	{
		left(now);
		distractTick = -1;
		seenXp = -1;
		xpTick = -1;
	}

	void newAccount()
	{
		session.startOver();
		log = null;
		doors = null;
		orianaSeen = null;
		rate = -1;
		inside = false;
		insideHouse = null;
		insideDoor = null;
		houses = 0;
		valuables = 0;
		keysGained = 0;
		hauled = 0;
		coins = 0;
		distractTick = -1;
		backTick = -1;
		nearTick = -1;
		shineTick = -1;
		heardDistract = heardBack = heardNear = heardShine = heardEscorted = false;
		read = false;
		lastEndTick = -1;
		lastEndXp = -1;
		seenXp = -1;
		xpTick = -1;
		lockedAt.clear();
		lastUnlocked = new HashSet<>();
	}

	/** A running count for the pace coach: houses robbed. */
	int units()
	{
		return houses;
	}

	private HouseLog log()
	{
		if (log == null)
		{
			log = HouseLog.decode(load == null ? null : load.apply(LOG_KEY));
		}
		return log;
	}

	private Map<House, WorldPoint> doors()
	{
		if (doors == null)
		{
			doors = new EnumMap<>(House.class);
			orianaSeen = decodePlaces(load == null ? null : load.apply(PLACES_KEY), doors);
		}
		return doors;
	}

	/** What Oriana pays you for a valuable, as last seen: 55 until she has paid 65. */
	private int rate()
	{
		if (rate < 0)
		{
			rate = parseRate(load == null ? null : load.apply(RATE_KEY));
		}
		return rate;
	}

	/** A saved rate: 65 when Oriana has paid that, else the wiki's 55, whatever the save holds. */
	static int parseRate(String saved)
	{
		int v = Houses.ORIANA_PAYS;
		try
		{
			v = saved == null ? Houses.ORIANA_PAYS : Integer.parseInt(saved.trim());
		}
		catch (NumberFormatException e)
		{
			// A damaged save: the wiki's 55.
		}
		return Houses.rate(v);
	}

	Flight fly(Client client, SceneIndex scene, Collection<NPC> npcs, long now)
	{
		session.tick(client, now);
		Pack pack = Pack.read(client);
		int tick = client.getTickCount();
		WorldPoint at = client.getLocalPlayer().getWorldLocation();
		hear(tick);
		learn(scene, npcs);

		HouseCycle.Snapshot s = new HouseCycle.Snapshot();
		lastState = s;
		s.level = client.getBoostedSkillLevel(Skill.THIEVING);
		s.keys = pack.count(Houses.KEY);
		s.valuables = pack.count(Houses.VALUABLES);
		s.free = pack.free();
		s.rate = rate();
		int xp = client.getSkillExperience(Skill.THIEVING);
		int cash = pack.count(ItemID.COINS);
		Map<Integer, Integer> contents = pack.contents();

		// The doors: which are locked, since when, and whose.
		List<TileObject> locked = new ArrayList<>();
		Set<WorldPoint> unlocked = new HashSet<>();
		for (TileObject o : scene.get(Houses.DOOR_LOCKED))
		{
			locked.add(o);
		}
		for (TileObject o : scene.get(Houses.DOOR))
		{
			unlocked.add(o.getWorldLocation());
		}
		Set<WorldPoint> lockedNow = new HashSet<>();
		for (TileObject o : locked)
		{
			WorldPoint w = o.getWorldLocation();
			lockedNow.add(w);
			if (!lockedAt.containsKey(w))
			{
				// Seen to lock this tick, where an unlocked door stood: the owner is leaving now.
				lockedAt.put(w, read && near(lastUnlocked, w) ? tick : -1);
			}
		}
		lockedAt.keySet().retainAll(lockedNow);
		lastUnlocked = unlocked;
		TileObject door = nearestObject(locked, at);
		if (door != null)
		{
			s.vacant = true;
			s.doorInSight = true;
			s.vacantHouse = houseAt(door.getWorldLocation());
			Integer since = lockedAt.get(door.getWorldLocation());
			if (since != null && since >= 0)
			{
				int gone = (tick - since) * 6 / 10;
				s.leftMin = Math.max(0, Houses.AWAY_MIN - gone);
				s.leftMax = Math.max(0, Houses.AWAY_MAX - gone);
			}
		}
		if (backTick >= 0 && tick - backTick <= 50)
		{
			s.leftMin = 0;
			s.leftMax = 0;
		}

		// The owners.
		List<NPC> owners = new ArrayList<>();
		for (NPC n : npcs)
		{
			House h = House.byOwner(n.getId());
			WorldPoint p = n.getWorldLocation();
			if (h != null && p != null && p.getPlane() == at.getPlane())
			{
				owners.add(n);
				if (door != null && (s.vacantHouse == null || s.vacantHouse == h) && p.distanceTo2D(door.getWorldLocation()) <= OWNER_NEAR)
				{
					s.ownerNear = true;
				}
			}
		}
		if (nearTick >= 0 && tick - nearTick <= 5)
		{
			s.ownerNear = true;
		}

		// In and out of a house.
		if (read)
		{
			if (s.keys > prevKeys)
			{
				keysGained += s.keys - prevKeys;
			}
			if (s.valuables > prevValuables)
			{
				gainTick = tick;
				valuables += s.valuables - prevValuables;
			}
			if (!inside && s.keys < prevKeys)
			{
				TileObject d = nearestObject(locked, prevAt != null ? prevAt : at);
				if (d != null && (d.getWorldLocation().distanceTo2D(at) <= AT_DOOR || prevAt != null && d.getWorldLocation().distanceTo2D(prevAt) <= AT_DOOR))
				{
					enter(d.getWorldLocation(), tick);
				}
			}
			if (!inside && s.valuables < prevValuables && cash > prevCoins)
			{
				traded(prevValuables - s.valuables, cash - prevCoins, npcs, at);
			}
		}
		if (inside)
		{
			mostValuables = Math.max(mostValuables, s.valuables);
			boolean out = heardEscorted || !within(scene, at);
			outsideTicks = out ? outsideTicks + 1 : 0;
			if (heardEscorted || outsideTicks >= 2)
			{
				end(s.valuables, xp, contents, now, tick);
			}
		}
		heardEscorted = false;
		s.rate = rate();
		s.inside = inside;
		s.insideHouse = insideHouse;
		s.ownerBack = inside && backTick >= startTick && backTick >= 0;
		s.shine = inside && shineTick >= 0 && tick - shineTick <= Houses.SHINE_SECONDS * 10 / 6;
		s.searching = inside && gainTick >= 0 && tick - gainTick <= SEARCH_TICKS;
		if (inside)
		{
			s.vacant = false;
		}

		// The citizens and the urchins.
		List<NPC> citizens = new ArrayList<>();
		for (NPC n : npcs)
		{
			WorldPoint p = n.getWorldLocation();
			if (Houses.isCitizen(n.getId()) && p != null && p.getPlane() == at.getPlane())
			{
				citizens.add(n);
			}
			if (Houses.isOriana(n.getId()) && p != null && p.distanceTo2D(at) <= 15)
			{
				s.oriana = true;
			}
			if (Houses.isTeller(n.getId()) && p != null && p.distanceTo2D(at) <= 15)
			{
				s.teller = true;
			}
		}
		s.citizens = !citizens.isEmpty();
		NPC distracted = null;
		int distractTicks = Houses.DISTRACT_FOR * 10 / 6;
		if (distractTick >= 0)
		{
			int gone = (tick - distractTick) * 6 / 10;
			if (tick - distractTick <= distractTicks)
			{
				distracted = distracted(npcs, citizens, at);
				s.distracted = distracted != null;
				s.distractedLeft = Math.max(1, Houses.DISTRACT_FOR - gone);
			}
			if (gone <= Houses.DISTRACT_EVERY_MAX)
			{
				s.nextMin = Math.max(0, Houses.DISTRACT_EVERY_MIN - gone);
				s.nextMax = Math.max(0, Houses.DISTRACT_EVERY_MAX - gone);
			}
		}

		read = true;
		prevKeys = s.keys;
		prevValuables = s.valuables;
		prevCoins = cash;
		prevXp = xp;
		prevAt = at;
		prevContents = contents;

		HouseCycle.Decision d = HouseCycle.next(s);
		Step step = d.step;
		Ground ground = Ground.read(client, Collections.emptyList(), 0);
		Flight flight = new Flight(View.Activity.HOUSES, step, null);
		List<TileObject> windows = inside ? windows(client, scene) : Collections.emptyList();
		switch (step.getTarget())
		{
			case CITIZEN:
				toNpc(flight, ground, distracted != null ? Collections.singletonList(distracted) : citizens);
				break;
			case LOCKED_DOOR:
				if (door != null)
				{
					toObject(flight, ground, Collections.singletonList(door));
				}
				break;
			case HOUSE:
				flight.destination = s.vacantHouse == null ? null : doors().get(s.vacantHouse);
				break;
			case LOOT:
				toObject(flight, ground, loot(scene, insideDoor, at.getPlane()));
				break;
			case WINDOW:
				toObject(flight, ground, windows);
				break;
			case ORIANA:
			case URCHIN:
			{
				List<NPC> who = new ArrayList<>();
				for (NPC n : npcs)
				{
					if (step.getTarget() == Step.Target.ORIANA ? Houses.isOriana(n.getId()) : Houses.isTeller(n.getId()))
					{
						who.add(n);
					}
				}
				toNpc(flight, ground, who);
				break;
			}
			default:
				break;
		}
		View view = new View(true, View.Activity.HOUSES, step, place(s), HouseCycle.LOOP, d.stage);
		Flight out = new Flight(View.Activity.HOUSES, step, view);
		out.object = flight.object;
		out.npc = flight.npc;
		out.route = flight.route;
		out.destination = flight.destination;
		if (inside)
		{
			// The ways out, quietly, whatever the step.
			for (TileObject w : windows)
			{
				if (w != out.object)
				{
					out.others.add(w);
				}
			}
		}
		if (markOwners && (inside || s.doorInSight))
		{
			for (NPC n : owners)
			{
				if (n.getWorldLocation().distanceTo2D(at) <= MARK)
				{
					out.wary.add(n);
				}
			}
		}
		dress(view, s, client, at, owners, scene, now);
		return out;
	}

	private void hear(int tick)
	{
		if (heardDistract)
		{
			distractTick = tick;
		}
		if (heardBack)
		{
			backTick = tick;
		}
		if (heardNear)
		{
			nearTick = tick;
		}
		if (heardShine)
		{
			shineTick = tick;
		}
		heardDistract = heardBack = heardNear = heardShine = false;
	}

	private static boolean near(Set<WorldPoint> points, WorldPoint w)
	{
		for (WorldPoint p : points)
		{
			if (p.getPlane() == w.getPlane() && p.distanceTo2D(w) <= 1)
			{
				return true;
			}
		}
		return false;
	}

	private void enter(WorldPoint door, int tick)
	{
		inside = true;
		insideDoor = door;
		insideHouse = houseAt(door);
		startTick = tick;
		startValuables = prevValuables;
		startXp = prevXp;
		mostValuables = prevValuables;
		startContents = prevContents;
		outsideTicks = 0;
	}

	/** The house is left: kept with what it gave. */
	private void end(int heldValuables, int xp, Map<Integer, Integer> contents, long now, int tick)
	{
		int taken = Math.max(0, mostValuables - startValuables);
		int kept = Math.max(0, heldValuables - startValuables);
		long items = 0;
		for (Map.Entry<Integer, Integer> e : contents.entrySet())
		{
			int id = e.getKey();
			if (id == Houses.VALUABLES || id == Houses.KEY || id == ItemID.COINS)
			{
				continue;
			}
			int more = e.getValue() - startContents.getOrDefault(id, 0);
			if (more > 0)
			{
				items += (long) more * Math.max(0, price.applyAsInt(id));
			}
		}
		int ticks = Math.max(1, tick - startTick);
		int round = lastEndTick >= 0 ? tick - lastEndTick : ticks;
		int houseXp = Math.max(0, xp - startXp);
		int roundXp = lastEndXp >= 0 ? Math.max(houseXp, xp - lastEndXp) : houseXp;
		HouseLog.Haul h = new HouseLog.Haul(now, insideHouse, ticks, round, taken, kept, houseXp, roundXp, items);
		log().add(h);
		if (save != null)
		{
			save.accept(LOG_KEY, log().encode());
		}
		houses++;
		hauled += h.value(rate());
		lastEndTick = tick;
		lastEndXp = xp;
		inside = false;
		insideHouse = null;
		insideDoor = null;
		backTick = -1;
		shineTick = -1;
		gainTick = -1;
	}

	/** Valuables gone and coins come by Oriana: a trade-in, and what she pays you. */
	private void traded(int gone, int paid, Collection<NPC> npcs, WorldPoint at)
	{
		boolean byOriana = false;
		for (NPC n : npcs)
		{
			WorldPoint p = n.getWorldLocation();
			byOriana |= Houses.isOriana(n.getId()) && p != null && p.distanceTo2D(at) <= 15;
		}
		if (!byOriana || gone <= 0)
		{
			return;
		}
		coins += paid;
		if (paid % gone == 0)
		{
			int each = paid / gone;
			if (each == Houses.ORIANA_PAYS || each == Houses.ORIANA_PAYS_GLADIATOR)
			{
				rate = each;
				if (save != null)
				{
					save.accept(RATE_KEY, String.valueOf(each));
				}
			}
		}
	}

	/** Still inside: within the house's furniture (a tile's slack), or near its door when none is known. */
	private boolean within(SceneIndex scene, WorldPoint at)
	{
		if (insideDoor == null)
		{
			return false;
		}
		int x0 = Integer.MAX_VALUE;
		int y0 = Integer.MAX_VALUE;
		int x1 = Integer.MIN_VALUE;
		int y1 = Integer.MIN_VALUE;
		for (TileObject o : scene.get(Houses.LOOT))
		{
			WorldPoint w = o.getWorldLocation();
			if (Math.max(Math.abs(w.getX() - insideDoor.getX()), Math.abs(w.getY() - insideDoor.getY())) <= HOUSE_SIZE)
			{
				x0 = Math.min(x0, w.getX());
				y0 = Math.min(y0, w.getY());
				x1 = Math.max(x1, w.getX());
				y1 = Math.max(y1, w.getY());
			}
		}
		if (x0 == Integer.MAX_VALUE)
		{
			return Math.max(Math.abs(at.getX() - insideDoor.getX()), Math.abs(at.getY() - insideDoor.getY())) <= HOUSE_SIZE;
		}
		return at.getX() >= x0 - 1 && at.getX() <= x1 + 1 && at.getY() >= y0 - 1 && at.getY() <= y1 + 1;
	}

	/** The chests, wardrobes and jewellery cases of the house you are in, on your floor. */
	private static List<TileObject> loot(SceneIndex scene, WorldPoint door, int plane)
	{
		List<TileObject> out = new ArrayList<>();
		for (TileObject o : scene.get(Houses.LOOT))
		{
			WorldPoint w = o.getWorldLocation();
			if (o.getPlane() == plane && (door == null || Math.max(Math.abs(w.getX() - door.getX()), Math.abs(w.getY() - door.getY())) <= HOUSE_SIZE))
			{
				out.add(o);
			}
		}
		return out;
	}

	/** The open windows in sight: "The windows are only open while the house is vacant". */
	private List<TileObject> windows(Client client, SceneIndex scene)
	{
		List<TileObject> out = new ArrayList<>();
		for (TileObject o : scene.get(Houses.WINDOWS))
		{
			if (insideDoor != null && Math.max(Math.abs(o.getWorldLocation().getX() - insideDoor.getX()),
				Math.abs(o.getWorldLocation().getY() - insideDoor.getY())) > HOUSE_SIZE)
			{
				continue;
			}
			if (open(client, o.getId()))
			{
				out.add(o);
			}
		}
		return out;
	}

	/** A window is open as its id says, or as its multiloc shows it now; unknown counts as open. */
	static boolean open(Client client, int id)
	{
		if (id == Houses.WINDOW_OPEN)
		{
			return true;
		}
		if (id == Houses.WINDOW_CLOSED)
		{
			return false;
		}
		ObjectComposition def = client.getObjectDefinition(id);
		if (def != null && def.getImpostorIds() != null)
		{
			ObjectComposition now = def.getImpostor();
			return now == null || now.getId() != Houses.WINDOW_CLOSED;
		}
		return true;
	}

	/**
	 * The citizen being distracted: the one an urchin is interacting with, else the citizen nearest
	 * an urchin (within three tiles); of those, the nearest you.
	 */
	private static NPC distracted(Collection<NPC> npcs, List<NPC> citizens, WorldPoint at)
	{
		NPC best = null;
		int bestD = Integer.MAX_VALUE;
		for (NPC u : npcs)
		{
			if (!Houses.isDistracter(u.getId()) || u.getWorldLocation() == null)
			{
				continue;
			}
			NPC c = null;
			Actor a = u.getInteracting();
			if (a instanceof NPC && Houses.isCitizen(((NPC) a).getId()))
			{
				c = (NPC) a;
			}
			else
			{
				int cd = 4;
				for (NPC n : citizens)
				{
					int dd = n.getWorldLocation().distanceTo2D(u.getWorldLocation());
					if (dd < cd)
					{
						cd = dd;
						c = n;
					}
				}
			}
			if (c != null && c.getWorldLocation() != null)
			{
				int dd = c.getWorldLocation().distanceTo2D(at);
				if (dd < bestD)
				{
					bestD = dd;
					best = c;
				}
			}
		}
		return best;
	}

	/** Each house's door (by the owner seen by it) and Oriana, kept so the arrow can find them out of sight. */
	private void learn(SceneIndex scene, Collection<NPC> npcs)
	{
		Map<House, WorldPoint> p = doors();
		boolean changed = false;
		List<TileObject> all = new ArrayList<>(scene.get(Houses.DOOR_LOCKED, Houses.DOOR));
		for (NPC n : npcs)
		{
			House h = House.byOwner(n.getId());
			WorldPoint w = n.getWorldLocation();
			if (h == null || w == null)
			{
				continue;
			}
			TileObject d = nearestObject(all, w);
			if (d == null || d.getWorldLocation().distanceTo2D(w) > OWNS)
			{
				continue;
			}
			WorldPoint dw = d.getWorldLocation();
			House other = houseAt(dw);
			if ((other == null || other == h) && !dw.equals(p.get(h)))
			{
				p.put(h, dw);
				changed = true;
			}
		}
		// Two doors known: a third door is the third house's.
		if (p.size() == House.values().length - 1)
		{
			for (TileObject d : all)
			{
				if (houseAt(d.getWorldLocation()) == null)
				{
					for (House h : House.values())
					{
						if (!p.containsKey(h))
						{
							p.put(h, d.getWorldLocation());
							changed = true;
						}
					}
					break;
				}
			}
		}
		for (NPC n : npcs)
		{
			WorldPoint w = n.getWorldLocation();
			if (Houses.isOriana(n.getId()) && w != null && (orianaSeen == null || orianaSeen.distanceTo2D(w) > 2))
			{
				orianaSeen = w;
				changed = true;
			}
		}
		if (changed && save != null)
		{
			save.accept(PLACES_KEY, encodePlaces(p, orianaSeen));
		}
	}

	/** The house whose door is at (or by) this tile, or null. */
	private House houseAt(WorldPoint w)
	{
		for (Map.Entry<House, WorldPoint> e : doors().entrySet())
		{
			if (e.getValue().getPlane() == w.getPlane() && e.getValue().distanceTo2D(w) <= 2)
			{
				return e.getKey();
			}
		}
		return null;
	}

	private static TileObject nearestObject(Collection<TileObject> things, WorldPoint at)
	{
		TileObject best = null;
		int bestD = Integer.MAX_VALUE;
		for (TileObject o : things)
		{
			int d = o.getWorldLocation().distanceTo2D(at);
			if (d < bestD)
			{
				bestD = d;
				best = o;
			}
		}
		return best;
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

	private static void toNpc(Flight flight, Ground ground, List<NPC> who)
	{
		if (who.isEmpty())
		{
			return;
		}
		Ground.Way<NPC> way = ground == null ? null : ground.nearestNpc(who);
		if (way != null)
		{
			flight.to(way);
			return;
		}
		flight.npc = who.get(0);
	}

	private static String place(HouseCycle.Snapshot s)
	{
		if (s.inside)
		{
			return "Civitas illa Fortis · " + (s.insideHouse == null ? "inside a house" : "in " + s.insideHouse.title());
		}
		return "Civitas illa Fortis" + (s.vacant ? " · " + (s.vacantHouse == null ? "a house vacant" : s.vacantHouse.owner() + " out") : "");
	}

	/** The houses, your haul, your session, a Thieving level, the three houses and your hauls. */
	private void dress(View view, HouseCycle.Snapshot s, Client client, WorldPoint at, List<NPC> owners, SceneIndex scene, long now)
	{
		Map<House, WorldPoint> p = doors();
		House target = s.inside ? s.insideHouse : s.vacant ? s.vacantHouse : null;
		if (!p.isEmpty())
		{
			List<View.District.Place> places = new ArrayList<>();
			for (House h : House.values())
			{
				WorldPoint w = p.get(h);
				if (w != null)
				{
					places.add(new View.District.Place(w.getX(), w.getY(), h.owner(), h == target, true));
				}
			}
			List<View.District.Patrol> marks = new ArrayList<>();
			for (NPC n : owners)
			{
				// The owner who matters: the one whose house is open.
				WorldPoint w = n.getWorldLocation();
				WorldPoint home = p.get(target);
				if (target != null && House.byOwner(n.getId()) == target && home != null && home.distanceTo2D(w) <= MARK)
				{
					marks.add(new View.District.Patrol(w.getX(), w.getY(), n.getOrientation()));
				}
			}
			String note = s.inside ? (s.ownerBack ? "Owner on the way back" : "Inside" + (s.leftMin >= 0 ? ", " + Houses.clock(s.leftMin) + " left" : ""))
				: target != null ? target.owner() + " is out" : s.vacant ? "A house is vacant" : "No house seen vacant";
			view.district(new View.District("The houses", note, places, marks,
				orianaSeen == null ? null : new int[]{orianaSeen.getX(), orianaSeen.getY()}, new int[]{at.getX(), at.getY()}, s.inside)
				.named("Vacant", "Owner", "Oriana"));
		}

		List<View.Line> lines = new ArrayList<>();
		lines.add(new View.Line("House keys", s.keys == 0 ? "None: pickpocket" : String.valueOf(s.keys), s.keys > 0 ? View.Line.Mark.DONE : View.Line.Mark.TODO));
		lines.add(new View.Line("Valuables", s.valuables == 0 ? "None" : Session.format(s.valuables) + " · " + Session.format((long) s.valuables * s.rate) + " gp",
			s.valuables > 0 ? View.Line.Mark.NEXT : View.Line.Mark.IDLE));
		lines.add(new View.Line("Free slots", s.free + (s.free == 0 ? ": none to search with" : ""), s.free > 0 ? View.Line.Mark.DONE : View.Line.Mark.TODO));
		if (s.inside)
		{
			lines.add(new View.Line("Owner back", s.ownerBack ? "Now" : s.leftMin >= 0 ? "In " + Houses.clock(s.leftMin) + "-" + Houses.clock(s.leftMax) : "About 3:00 from leaving",
				s.ownerBack || s.leftMin >= 0 && s.leftMin <= HouseCycle.LEAVE_AT ? View.Line.Mark.TODO : View.Line.Mark.IDLE));
		}
		else
		{
			lines.add(new View.Line("Next distraction", s.distracted ? "Now, " + s.distractedLeft + "s left" : s.nextMin >= 0 ? "In " + s.nextMin + "-" + s.nextMax + "s" : "Listening",
				s.distracted ? View.Line.Mark.NEXT : View.Line.Mark.IDLE));
		}
		view.lines("Your haul", "Oriana pays " + s.rate + " a valuable", lines);

		view.stat(new View.Stat(Session.format(houses), "Houses", session.perHour(houses, now), Theme.GOLD));
		view.stat(new View.Stat(Session.format(valuables), "Valuables", session.perHour(valuables, now), Theme.GOLD_BRIGHT));
		int xp = session.xp(client);
		view.stat(new View.Stat(Session.format(xp), "Thieving xp", session.perHour(xp, now), Theme.GREEN));
		view.stat(new View.Stat(Session.format(hauled), "Haul value", session.perHour(hauled, now), Theme.TEAL));

		int real = client.getRealSkillLevel(Skill.THIEVING);
		view.goal(goalCard(client, real));
		view.pick(housesCard(s, scene));
		HouseLog g = log();
		if (g.count() > 0)
		{
			view.supply(historyCard(g, real, rate()));
		}
	}

	/** A Thieving level in valuables and time at your pace. */
	private View.GoalCard goalCard(Client client, int real)
	{
		int xp = client.getSkillExperience(Skill.THIEVING);
		int target = goalLevel > real ? goalLevel : Math.min(99, real + 1);
		long need = Experience.getXpForLevel(Math.max(2, target));
		boolean done = real >= 99 || xp >= need;
		long left = Math.max(0, need - xp);
		HouseLog g = log();
		long each = Houses.valuablesFor(left);
		double hours = g.hoursFor(left);
		String note = done ? "Reached." : Session.format(each) + " valuables at 45 xp each"
			+ (hours > 0 ? ", about " + MixologyPilot.clockHours(hours) + " at your pace" : "; the time shows after your first house");
		List<View.Line> pieces = new ArrayList<>();
		pieces.add(new View.Line("Valuables kept", Session.format(g.kept()), View.Line.Mark.IDLE));
		pieces.add(new View.Line("Worth at Oriana", Session.format(g.kept() * rate()) + " coins", View.Line.Mark.IDLE));
		return new View.GoalCard(View.Activity.HOUSES, "Thieving " + target, done ? 1 : xp, done ? 1 : need, "xp", note, pieces, done);
	}

	/** The three houses: vacant, home or out of sight, where the wiki puts each, and your average there. */
	private View.PickCard housesCard(HouseCycle.Snapshot s, SceneIndex scene)
	{
		HouseLog g = log();
		Map<House, WorldPoint> p = doors();
		List<View.Pick> picks = new ArrayList<>();
		int most = 1;
		for (House h : House.values())
		{
			most = Math.max(most, g.typical(h));
		}
		for (House h : House.values())
		{
			WorldPoint w = p.get(h);
			String state = "Not seen";
			boolean lit = (s.inside ? s.insideHouse : s.vacant ? s.vacantHouse : null) == h;
			if (w != null)
			{
				for (TileObject o : scene.get(Houses.DOOR_LOCKED))
				{
					state = o.getWorldLocation().distanceTo2D(w) <= 2 ? "Vacant" : state;
				}
				for (TileObject o : scene.get(Houses.DOOR))
				{
					state = o.getWorldLocation().distanceTo2D(w) <= 2 ? "Home" : state;
				}
			}
			if (lit)
			{
				state = s.inside ? "Inside" : "Vacant";
			}
			int mine = g.typical(h);
			String where = h.where();
			String detail = Character.toUpperCase(where.charAt(0)) + where.substring(1) + (mine >= 0 ? " · your average " + Session.format(mine) + " valuables over "
				+ g.visits(h) + (g.visits(h) == 1 ? " visit" : " visits") : "");
			picks.add(new View.Pick(h.title(), state, detail, true, lit, mine > 0 ? mine / (double) most : 0, false));
		}
		return new View.PickCard("Who is out", "one at a time",
			"The vacant house's door is a Locked Door; the urchins by the houses tell you which for 15 valuables.", picks);
	}

	/** Your last hauls, the best, and coins and experience an hour across them. */
	private static View.Supply historyCard(HouseLog g, int level, int rate)
	{
		List<View.Line> lines = new ArrayList<>();
		List<HouseLog.Haul> all = g.all();
		HouseLog.Haul best = g.best(rate);
		for (int i = all.size() - 1; i >= 0 && i >= all.size() - 5; i--)
		{
			HouseLog.Haul h = all.get(i);
			String what = (h.house == null ? "A house" : h.house.owner()) + (h.ticks > 0 ? " · " + StormPilot.clock(h.ticks) : "");
			String got = h.caught() ? "Caught: " + h.kept + " of " + h.taken : Session.format(h.kept) + " · " + Session.format(h.value(rate)) + " gp";
			lines.add(new View.Line(what, got, h == best ? View.Line.Mark.DONE : h.caught() ? View.Line.Mark.TODO : View.Line.Mark.IDLE));
		}
		String headline = best == null ? null : "Best: " + Session.format(best.kept) + " valuables" + (best.items > 0 ? " and " + Session.format(best.items) + " gp of finds" : "")
			+ (best.house == null ? "" : " at " + best.house.possessive());
		double gp = g.coinsPerHour(rate);
		String detail = gp < 0 ? null : String.format(Locale.ROOT, "%s coins and %s Thieving xp an hour at your pace, keys included (the wiki at %d: %s xp, %s-%s coins)",
			Session.format(Math.round(gp)), Session.format(Math.round(g.xpPerHour())), level, Session.format(Houses.wikiXpPerHour(level)),
			Session.format(Houses.WIKI_COINS_MIN), Session.format(Houses.WIKI_COINS_MAX));
		String note = g.caught() > 0 ? g.caught() + " of " + g.count() + " caught inside" : null;
		return new View.Supply(g.count() + (g.count() == 1 ? " house" : " houses"), lines, headline, detail, false, note).titled("Your hauls");
	}

	/** Brief for the panel away from the city. */
	View.Brief brief()
	{
		HouseLog g = log();
		String status = null;
		HouseLog.Haul last = g.last();
		if (last != null)
		{
			status = "Last house: " + (last.house == null ? "" : last.house.possessive() + ", ")
				+ Session.format(last.kept) + " valuables, worth " + Session.format(last.value(rate())) + " coins"
				+ (last.ticks > 0 ? " in " + StormPilot.clock(last.ticks) : "");
		}
		View.Brief b = new View.Brief(View.Activity.HOUSES, status,
			"In Civitas illa Fortis, from 50 Thieving and Children of the Sun. Pickpocket the wealthy citizens in the bazaar for house keys.");
		double gp = g.coinsPerHour(rate());
		if (gp >= 0)
		{
			b.best(Session.format(Math.round(gp)) + " coins and " + Session.format(Math.round(g.xpPerHour())) + " xp an hour over "
				+ g.count() + (g.count() == 1 ? " house." : " houses."));
		}
		return b;
	}

	/** "1;LAVINIA:x,y,p;...;ORIANA:x,y,p": each house's door by owner, and Oriana. */
	static String encodePlaces(Map<House, WorldPoint> doors, WorldPoint oriana)
	{
		StringBuilder b = new StringBuilder("1");
		for (Map.Entry<House, WorldPoint> e : doors.entrySet())
		{
			WorldPoint w = e.getValue();
			b.append(';').append(e.getKey().name()).append(':').append(w.getX()).append(',').append(w.getY()).append(',').append(w.getPlane());
		}
		if (oriana != null)
		{
			b.append(";ORIANA:").append(oriana.getX()).append(',').append(oriana.getY()).append(',').append(oriana.getPlane());
		}
		return b.toString();
	}

	/** Reads a save into the map as far as it can, and returns Oriana's place, or null. */
	static WorldPoint decodePlaces(String text, Map<House, WorldPoint> into)
	{
		WorldPoint oriana = null;
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
				// On the map: a damaged save's wild numbers are left out.
				if (x <= 0 || x >= 16384 || y <= 0 || y >= 16384 || plane < 0 || plane > 3)
				{
					continue;
				}
				String name = parts[i].substring(0, colon).trim();
				WorldPoint w = new WorldPoint(x, y, plane);
				if ("ORIANA".equals(name))
				{
					oriana = w;
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
		return oriana;
	}
}
