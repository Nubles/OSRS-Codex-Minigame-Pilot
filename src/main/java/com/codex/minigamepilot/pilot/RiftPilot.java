package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Altar;
import com.codex.minigamepilot.core.AltarPick;
import com.codex.minigamepilot.core.RiftCycle;
import com.codex.minigamepilot.core.RiftPoints;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

/**
 * Flies Guardians of the Rift: mine fragments, craft essence, the best open altar for your
 * levels, quests and points, then hand in. It only reads the game (its HUD, varbits, what you
 * carry and the objects around you) and draws.
 *
 * <p>Ids are RuneLite's gameval names. The temple is map region 14484, as RuneLite's own loot
 * tracker and Discord plugins have it. The HUD components that show the open altars
 * ({@code GotrHud.ELEMENTAL_PORTAL}, {@code GotrHud.CATALYTIC_PORTAL}, each a rune icon) and the
 * guardian's power ({@code GotrHud.ENERGY_TITLE}) are the ones the Guardians of the Rift Helper
 * and Optimizer plugins read (a cross-check: the names are RuneLite's).
 */
public final class RiftPilot
{
	static final int TEMPLE_REGION = 14484;
	/** Large guardian remains are past rubble that needs 56 Agility, not boostable (OSRS wiki). */
	static final int LARGE_REMAINS_AGILITY = 56;
	/** Tiles from the huge remains that count as being on their shelf. */
	private static final int SHELF = 12;
	/** Tiles from an altar that count as being inside it. */
	private static final int INSIDE = 30;
	/** How often a quest not yet done is asked about again, in ticks. */
	private static final int QUEST_RECHECK = 100;

	static final Map<Altar, Integer> GUARDIANS = new EnumMap<>(Altar.class);
	static final Map<Altar, Integer> ALTARS = new EnumMap<>(Altar.class);
	static final Map<Altar, Integer> EXITS = new EnumMap<>(Altar.class);
	static final Map<Altar, Integer> RUNES = new EnumMap<>(Altar.class);

	static
	{
		put(Altar.AIR, ObjectID.GOTR_PORTAL_AIR, ObjectID.AIR_ALTAR, ObjectID.AIRTEMPLE_EXIT_PORTAL, ItemID.AIRRUNE);
		put(Altar.MIND, ObjectID.GOTR_PORTAL_MIND, ObjectID.MIND_ALTAR, ObjectID.MINDTEMPLE_EXIT_PORTAL, ItemID.MINDRUNE);
		put(Altar.WATER, ObjectID.GOTR_PORTAL_WATER, ObjectID.WATER_ALTAR, ObjectID.WATERTEMPLE_EXIT_PORTAL, ItemID.WATERRUNE);
		put(Altar.EARTH, ObjectID.GOTR_PORTAL_EARTH, ObjectID.EARTH_ALTAR, ObjectID.EARTHTEMPLE_EXIT_PORTAL, ItemID.EARTHRUNE);
		put(Altar.FIRE, ObjectID.GOTR_PORTAL_FIRE, ObjectID.FIRE_ALTAR, ObjectID.FIRETEMPLE_EXIT_PORTAL, ItemID.FIRERUNE);
		put(Altar.BODY, ObjectID.GOTR_PORTAL_BODY, ObjectID.BODY_ALTAR, ObjectID.BODYTEMPLE_EXIT_PORTAL, ItemID.BODYRUNE);
		put(Altar.COSMIC, ObjectID.GOTR_PORTAL_COSMIC, ObjectID.COSMIC_ALTAR, ObjectID.COSMICTEMPLE_EXIT_PORTAL, ItemID.COSMICRUNE);
		put(Altar.CHAOS, ObjectID.GOTR_PORTAL_CHAOS, ObjectID.CHAOS_ALTAR, ObjectID.CHAOSTEMPLE_EXIT_PORTAL, ItemID.CHAOSRUNE);
		put(Altar.NATURE, ObjectID.GOTR_PORTAL_NATURE, ObjectID.NATURE_ALTAR, ObjectID.NATURETEMPLE_EXIT_PORTAL, ItemID.NATURERUNE);
		put(Altar.LAW, ObjectID.GOTR_PORTAL_LAW, ObjectID.LAW_ALTAR, ObjectID.LAWTEMPLE_EXIT_PORTAL, ItemID.LAWRUNE);
		put(Altar.DEATH, ObjectID.GOTR_PORTAL_DEATH, ObjectID.DEATH_ALTAR, ObjectID.DEATHTEMPLE_EXIT_PORTAL, ItemID.DEATHRUNE);
		put(Altar.BLOOD, ObjectID.GOTR_PORTAL_BLOOD, ObjectID.BLOOD_ALTAR, ObjectID.BLOODTEMPLE_EXIT_PORTAL, ItemID.BLOODRUNE);
	}

	private static void put(Altar a, int guardian, int altar, int exit, int rune)
	{
		GUARDIANS.put(a, guardian);
		ALTARS.put(a, altar);
		EXITS.put(a, exit);
		RUNES.put(a, rune);
	}

	/** Combination runes crafted here too (OSRS wiki: mist, dust, mud, smoke, steam, lava and aether). */
	static final int[] COMBINATION_RUNES = {ItemID.MISTRUNE, ItemID.DUSTRUNE, ItemID.MUDRUNE, ItemID.SMOKERUNE,
		ItemID.STEAMRUNE, ItemID.LAVARUNE, ItemID.AETHERRUNE};
	static final int[] SMALL_REMAINS = {ObjectID.GOTR_ESSENCE_TIER_1, ObjectID.GOTR_ESSENCE_TIER_1B,
		ObjectID.GOTR_ESSENCE_TIER_1_LARGE, ObjectID.GOTR_ESSENCE_TIER_1_LARGE_B};
	static final int[] CELL_TILES = {ObjectID.GOTR_CELL_TILE_INACTIVE, ObjectID.GOTR_CELL_TILE_TIER1,
		ObjectID.GOTR_CELL_TILE_TIER2, ObjectID.GOTR_CELL_TILE_TIER3, ObjectID.GOTR_CELL_TILE_TIER4};
	static final int[] RIFT_PORTALS = {ObjectID.GOTR_AGILITY_PORTAL_TOP, ObjectID.GOTR_AGILITY_PORTAL_BOTTOM_CHILD};
	static final int[] STONES = {ItemID.GOTR_GUARDIAN_STONE_ELEMENTAL, ItemID.GOTR_GUARDIAN_STONE_CATALYTIC,
		ItemID.GOTR_GUARDIAN_STONE_POLYELEMENTAL, ItemID.GOTR_GUARDIAN_STONE_POLYCATALYTIC};
	/** Guardian essence, fragments, cells and stones exist only in the minigame. */
	static final int[] RIFT_ITEMS = {ItemID.GOTR_GUARDIAN_ESSENCE, ItemID.GOTR_GUARDIAN_FRAGMENT, ItemID.GOTR_CELL_UNCHARGED,
		ItemID.GOTR_CELL_TIER1, ItemID.GOTR_CELL_TIER2, ItemID.GOTR_CELL_TIER3, ItemID.GOTR_CELL_TIER4,
		ItemID.GOTR_GUARDIAN_STONE_ELEMENTAL, ItemID.GOTR_GUARDIAN_STONE_CATALYTIC,
		ItemID.GOTR_GUARDIAN_STONE_POLYELEMENTAL, ItemID.GOTR_GUARDIAN_STONE_POLYCATALYTIC};

	/** The game's messages as the rift opens and before (as the Helper plugin reads them: a cross-check). */
	static final String RIFT_OPEN = "The rift becomes active!";
	static final String RIFT_SOON = "The rift will become active in";
	/** The guardian's power as the HUD writes it: the first percentage in its title. */
	private static final Pattern PERCENT = Pattern.compile("(\\d{1,3})\\s*%");
	/** The first huge-remains portal and the gap to each after (OSRS wiki, Guardians of the Rift/Strategies). */
	static final int FIRST_PORTAL_TICKS = 267;
	static final int PORTAL_GAP_TICKS = 233;

	/** Where the banked points are saved for this account. */
	static final String POINTS_KEY = "rift.points";

	private final Session session = new Session(Skill.RUNECRAFT);
	private final RiftPoints points = new RiftPoints();
	private Function<String, String> load;
	private BiConsumer<String, String> save;
	private boolean pointsLoaded;
	/** The message box's text last tick, so one left open is read once. */
	private String lastBox;
	private boolean started;
	private boolean crafted;
	private int trips;
	private Altar lastAltar;
	/** The tick the rift opened, or -1. */
	private int openedTick = -1;
	/** The tick the last huge-remains portal was seen to open, or -1. */
	private int portalTick = -1;
	private boolean portalWas;
	private int firstTarget = 120;
	/** Whether a quest is done, by its name; the plugin asks the game. */
	private Predicate<Quest> questDone;
	/** The sprite a HUD component shows; the plugin asks the widget. */
	private IntUnaryOperator sprite;
	private final Map<Quest, Boolean> quests = new EnumMap<>(Quest.class);
	private int questCheckedAt = -QUEST_RECHECK;
	Object lastState;

	void setStore(Function<String, String> load, BiConsumer<String, String> save)
	{
		this.load = load;
		this.save = save;
	}

	/** How quests and HUD icons are read. Package-private: tests read them from a script. */
	void readers(Predicate<Quest> questDone, IntUnaryOperator sprite)
	{
		this.questDone = questDone;
		this.sprite = sprite;
		quests.clear();
		questCheckedAt = -QUEST_RECHECK;
	}

	/** The "Fragments before the first craft" setting. */
	void setFirstTarget(int fragments)
	{
		firstTarget = Math.max(0, fragments);
	}

	static boolean isWatched(int id)
	{
		return GUARDIANS.containsValue(id) || ALTARS.containsValue(id) || EXITS.containsValue(id)
			|| contains(SMALL_REMAINS, id) || contains(CELL_TILES, id) || contains(RIFT_PORTALS, id)
			|| id == ObjectID.GOTR_ESSENCE_TIER_2 || id == ObjectID.GOTR_ESSENCE_TIER_3 || id == ObjectID.GOTR_WORKBENCH
			|| id == ObjectID.GOTR_DEPOSITCHEST || id == ObjectID.GOTR_UNCHARGED_CELLS;
	}

	private static boolean contains(int[] ids, int id)
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

	/** In the temple, or in one of its altars or on the remains' shelf with the minigame under way. */
	boolean active(Client client, SceneIndex scene)
	{
		Player me = client.getLocalPlayer();
		if (me == null)
		{
			return false;
		}
		if (me.getWorldLocation().getRegionID() == TEMPLE_REGION)
		{
			return true;
		}
		boolean playing = hudShown(client) || Pack.read(client).carries(RIFT_ITEMS);
		return playing && (altarHere(me.getWorldLocation(), scene) != null || near(me.getWorldLocation(), scene.get(ObjectID.GOTR_ESSENCE_TIER_3), SHELF) != null);
	}

	private static boolean hudShown(Client client)
	{
		Widget w = client.getWidget(InterfaceID.GotrHud.CONTENT);
		return w != null && !w.isHidden();
	}

	static Altar altarHere(WorldPoint at, SceneIndex scene)
	{
		for (Map.Entry<Altar, Integer> e : ALTARS.entrySet())
		{
			if (near(at, scene.get(e.getValue()), INSIDE) != null)
			{
				return e.getKey();
			}
		}
		return null;
	}

	private static TileObject near(WorldPoint at, Collection<TileObject> objects, int within)
	{
		for (TileObject o : objects)
		{
			if (o.getWorldLocation().getPlane() == at.getPlane() && o.getWorldLocation().distanceTo(at) <= within)
			{
				return o;
			}
		}
		return null;
	}

	/** A game message, tags removed. */
	void chat(String message)
	{
		if (message == null)
		{
			return;
		}
		if (message.startsWith(RIFT_OPEN))
		{
			started = true;
			crafted = false;
			openedTick = -2;
			portalTick = -1;
		}
		else if (message.startsWith(RIFT_SOON))
		{
			started = false;
			crafted = false;
			openedTick = -1;
		}
		loadPoints();
		String before = points.encode();
		if (points.read(message) && save != null && points.encode() != null && !points.encode().equals(before))
		{
			save.accept(POINTS_KEY, points.encode());
		}
	}

	private void loadPoints()
	{
		if (!pointsLoaded && load != null)
		{
			points.decode(load.apply(POINTS_KEY));
			pointsLoaded = true;
		}
	}

	/** Out of the minigame: the game under way is over for the Pilot. */
	void left()
	{
		started = false;
		crafted = false;
		openedTick = -1;
		portalTick = -1;
		lastAltar = null;
	}

	void newAccount()
	{
		session.startOver();
		pointsLoaded = false;
		points.decode(null);
		quests.clear();
		questCheckedAt = -QUEST_RECHECK;
		trips = 0;
		left();
	}

	/** A running count of what rounds produce, for the pace coach: altar trips. */
	int units()
	{
		return trips;
	}

	private boolean questDone(Client client, Quest q)
	{
		Boolean known = quests.get(q);
		if (Boolean.TRUE.equals(known))
		{
			return true;
		}
		int tick = client.getTickCount();
		if (known == null || tick - questCheckedAt >= QUEST_RECHECK)
		{
			boolean done;
			try
			{
				done = questDone != null ? questDone.test(q) : q.getState(client) == QuestState.FINISHED;
			}
			catch (RuntimeException e)
			{
				done = false;
			}
			quests.put(q, done);
			questCheckedAt = tick;
			return done;
		}
		return known;
	}

	private static Quest quest(String name)
	{
		for (Quest q : Quest.values())
		{
			if (q.getName().equals(name))
			{
				return q;
			}
		}
		return null;
	}

	/** The altar a HUD component shows by its rune icon, or null. */
	private Altar shown(Client client, int component, Altar.Alignment side)
	{
		Widget w = client.getWidget(component);
		if (w == null || w.isHidden())
		{
			return null;
		}
		int id = sprite != null ? sprite.applyAsInt(component) : w.getSpriteId();
		Altar a = Altar.byIcon(id - SpriteID.IconRune32x32._0);
		// Each side only ever shows its own kind; anything else is not an altar icon.
		return a != null && a.alignment() == side ? a : null;
	}

	/** The guardian's power from the HUD, 0 to 100, or -1 when it is not shown. */
	static int power(Client client)
	{
		Widget w = client.getWidget(InterfaceID.GotrHud.ENERGY_TITLE);
		if (w == null || w.isHidden() || w.getText() == null)
		{
			return -1;
		}
		Matcher m = PERCENT.matcher(w.getText());
		return m.find() ? Math.min(100, Integer.parseInt(m.group(1))) : -1;
	}

	Flight fly(Client client, SceneIndex scene, long now)
	{
		session.tick(client, now);
		loadPoints();
		Pack pack = Pack.read(client);
		Player me = client.getLocalPlayer();
		WorldPoint at = me.getWorldLocation();
		int tick = client.getTickCount();
		if (openedTick == -2)
		{
			openedTick = tick;
		}

		Widget box = client.getWidget(InterfaceID.Messagebox.TEXT);
		String boxText = box != null && !box.isHidden() ? box.getText() : null;
		if (boxText != null && !boxText.equals(lastBox))
		{
			// The Rewards Guardian's Check answers in a message box; read once while it stays open.
			chat(boxText.replace("<br>", " ").replaceAll("<[^>]*>", ""));
		}
		lastBox = boxText;
		points.energy(client.getVarbitValue(VarbitID.GOTR_ELEMENTAL_EARNED_THIS_GAME), client.getVarbitValue(VarbitID.GOTR_CATALYTIC_EARNED_THIS_GAME));
		int rc = client.getRealSkillLevel(Skill.RUNECRAFT);

		RiftCycle.Snapshot s = new RiftCycle.Snapshot();
		lastState = s;
		s.inAltar = at.getRegionID() == TEMPLE_REGION ? null : altarHere(at, scene);
		s.atHugeRemains = s.inAltar == null && near(at, scene.get(ObjectID.GOTR_ESSENCE_TIER_3), SHELF) != null;
		s.fragments = pack.count(ItemID.GOTR_GUARDIAN_FRAGMENT);
		s.essence = pack.count(ItemID.GOTR_GUARDIAN_ESSENCE);
		s.free = pack.free();
		Pouches pouches = Pouches.read(client, pack, rc);
		s.pouchStored = pouches.stored;
		s.pouchRoom = pouches.room;
		s.pouches = pouches.ids;
		s.stones = pack.count(STONES);
		s.chargedCell = pack.carries(ItemID.GOTR_CELL_TIER4) ? Altar.Cell.OVERCHARGED : pack.carries(ItemID.GOTR_CELL_TIER3) ? Altar.Cell.STRONG
			: pack.carries(ItemID.GOTR_CELL_TIER2) ? Altar.Cell.MEDIUM : pack.carries(ItemID.GOTR_CELL_TIER1) ? Altar.Cell.WEAK : null;
		s.unchargedCells = pack.count(ItemID.GOTR_CELL_UNCHARGED);
		s.runes = pack.carries(runeIds());
		s.riftPortalOpen = !scene.get(ObjectID.GOTR_AGILITY_PORTAL_TOP).isEmpty();
		s.cellTable = !scene.get(ObjectID.GOTR_UNCHARGED_CELLS).isEmpty();
		s.firstTarget = firstTarget;
		// Fragments, essence or energy this game mean the rift is open, whatever was missed of its messages.
		if (s.fragments > 0 || s.essence > 0 || s.pouchStored > 0 || points.gameElemental() > 0 || points.gameCatalytic() > 0 || s.inAltar != null)
		{
			started = true;
		}
		if (s.inAltar != null && s.inAltar != lastAltar)
		{
			crafted = true;
			trips++;
		}
		lastAltar = s.inAltar;
		s.started = started;
		s.craftedThisGame = crafted;

		if (s.riftPortalOpen && !portalWas)
		{
			portalTick = tick;
		}
		portalWas = s.riftPortalOpen;

		Altar elemental = shown(client, InterfaceID.GotrHud.ELEMENTAL_PORTAL, Altar.Alignment.ELEMENTAL);
		Altar catalytic = shown(client, InterfaceID.GotrHud.CATALYTIC_PORTAL, Altar.Alignment.CATALYTIC);
		AltarPick.Choice choice = AltarPick.choose(elemental, catalytic, rc, name ->
		{
			Quest q = quest(name);
			return q != null && questDone(client, q);
		}, points);
		s.pick = choice.altar;
		s.pickWhy = choice.why;

		Step step = RiftCycle.next(s);
		View view = new View(true, View.Activity.RIFT, step, place(s), RiftCycle.LOOP, RiftCycle.stage(step, s.inAltar != null));
		Ground ground = Ground.read(client, java.util.Collections.emptyList(), 0);
		Flight flight = route(client, scene, ground, new Flight(View.Activity.RIFT, step, view), s, at);
		int power = power(client);
		if (power >= 0)
		{
			view.gauge(new View.Gauge("Guardian's power", power, 0, 100,
				power >= 100 ? "Charged: the rift closes" : power >= 60 ? "Past 60%: more guardians and walkers" : "Rises as stones go in",
				power >= 60 ? Theme.EMBER : Theme.GOLD));
		}
		int carried = s.essence + s.pouchStored;
		int tripRoom = carried + RiftCycle.room(s);
		if (s.inAltar == null && !s.atHugeRemains && s.started)
		{
			int target = RiftCycle.target(s);
			if (carried == 0)
			{
				view.gauge(new View.Gauge("Fragments", s.fragments, 0, Math.max(1, Math.max(target, s.fragments)),
					s.fragments >= target ? "Enough: craft essence" : (target - s.fragments) + " to go", Theme.TEAL));
			}
		}
		if (tripRoom > 0 && (carried > 0 || s.inAltar != null))
		{
			view.gauge(new View.Gauge("Essence this trip", carried, 0, tripRoom,
				pouches.ids.length == 0 ? "Pack only: no pouches carried" : pouches.stored + " in pouches", Theme.GOLD));
		}
		view.pick(altarCard(choice, balanceLine()));
		balance(view);
		view.stat(new View.Stat(Integer.toString(trips), "Altar trips", session.perHour(trips, now), Theme.GOLD));
		int xp = session.xp(client);
		view.stat(new View.Stat(Session.format(xp), "Runecraft xp", session.perHour(xp, now), Theme.GREEN));
		String clock = clock(tick);
		if (clock != null)
		{
			view.stat(new View.Stat(clock, "Rift open", portalNote(tick), Theme.TEXT_MUTED));
		}
		return flight;
	}

	private static int[] runeIds()
	{
		int[] ids = new int[RUNES.size() + COMBINATION_RUNES.length];
		int i = 0;
		for (int id : RUNES.values())
		{
			ids[i++] = id;
		}
		for (int id : COMBINATION_RUNES)
		{
			ids[i++] = id;
		}
		return ids;
	}

	private Flight route(Client client, SceneIndex scene, Ground ground, Flight flight, RiftCycle.Snapshot s, WorldPoint at)
	{
		Step step = flight.step;
		Collection<? extends TileObject> things = null;
		switch (step.getTarget())
		{
			case REMAINS:
			{
				boolean large = client.getRealSkillLevel(Skill.AGILITY) >= LARGE_REMAINS_AGILITY
					&& !scene.get(ObjectID.GOTR_ESSENCE_TIER_2).isEmpty();
				things = large ? scene.get(ObjectID.GOTR_ESSENCE_TIER_2) : scene.get(SMALL_REMAINS);
				if (large && ground != null && ground.nearest(things, null) == null)
				{
					// Behind the rubble the walk cannot be drawn, but the remains can still be lit.
					TileObject o = things.iterator().next();
					flight.object = o;
					return flight;
				}
				break;
			}
			case WORKBENCH:
				things = scene.get(ObjectID.GOTR_WORKBENCH);
				break;
			case CELLS:
				things = scene.get(ObjectID.GOTR_UNCHARGED_CELLS);
				break;
			case GUARDIAN:
				things = s.pick == null ? null : scene.get(GUARDIANS.get(s.pick));
				break;
			case ALTAR:
				things = scene.get(ALTARS.get(s.inAltar));
				break;
			case EXIT:
				things = scene.get(EXITS.get(s.inAltar));
				break;
			case CELL_TILE:
				things = scene.get(CELL_TILES);
				break;
			case DEPOSIT_POOL:
				things = scene.get(ObjectID.GOTR_DEPOSITCHEST);
				break;
			case RIFT_PORTAL:
				things = scene.get(RIFT_PORTALS);
				break;
			case HUGE_REMAINS:
				things = scene.get(ObjectID.GOTR_ESSENCE_TIER_3);
				break;
			case GREAT_GUARDIAN:
			{
				List<NPC> guardians = new ArrayList<>();
				for (NPC n : client.getTopLevelWorldView().npcs())
				{
					if (n != null && n.getId() == NpcID.GOTR_GREAT_GUARDIAN)
					{
						guardians.add(n);
					}
				}
				Ground.Way<NPC> way = ground == null ? null : ground.nearestNpc(guardians);
				if (way != null)
				{
					return flight.to(way);
				}
				if (!guardians.isEmpty())
				{
					flight.npc = guardians.get(0);
				}
				return flight;
			}
			default:
				break;
		}
		if (things == null || things.isEmpty())
		{
			return flight;
		}
		Ground.Way<TileObject> way = ground == null ? null : ground.nearest(things, null);
		if (way != null)
		{
			return flight.to(way);
		}
		// Out of walking reach (across a gap, say): still light the nearest one up.
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

	private static String place(RiftCycle.Snapshot s)
	{
		if (s.inAltar != null)
		{
			return s.inAltar.rune() + " altar";
		}
		return s.atHugeRemains ? "Huge remains" : "Temple of the Eye";
	}

	/** The open altars, which is yours and why, for the panel. */
	private static View.PickCard altarCard(AltarPick.Choice choice, String note)
	{
		if (choice.open.isEmpty())
		{
			return null;
		}
		List<View.Pick> picks = new ArrayList<>();
		for (AltarPick.Option o : choice.open)
		{
			String detail = o.locked != null ? o.locked
				: o.altar.alignment().word().substring(0, 1).toUpperCase() + o.altar.alignment().word().substring(1)
				+ " · " + o.altar.cell().word() + " cell";
			picks.add(new View.Pick(o.altar.rune(), "Lv " + o.altar.level(), detail, o.locked == null, o.altar == choice.altar,
				o.altar.level() / 77.0, false));
		}
		return new View.PickCard("Open altars", choice.altar == null ? "None for you" : choice.altar.rune(), note, picks);
	}

	/**
	 * Your points of each type in whole points, as the Rewards Guardian's Check and the end of a
	 * game report them, with what this game's energy is on its way to.
	 */
	private void balance(View view)
	{
		if (!points.known())
		{
			int ge = points.gameElemental();
			int gc = points.gameCatalytic();
			view.stat(new View.Stat(ge + " / " + gc, "Energy this game", "Points unseen yet", Theme.TEAL));
			return;
		}
		int ge = points.gameElemental();
		int gc = points.gameCatalytic();
		view.stat(new View.Stat(RiftPoints.show(points.bankedElemental()), "Elemental points",
			ge > 0 ? RiftPoints.coming(ge) + " this game" : "Banked", Theme.TEAL));
		view.stat(new View.Stat(RiftPoints.show(points.bankedCatalytic()), "Catalytic points",
			gc > 0 ? RiftPoints.coming(gc) + " this game" : "Banked", Theme.GOLD_BRIGHT));
		view.stat(new View.Stat(Session.format(points.searches()), "Searches", "1 of each", Theme.GOLD));
	}

	/** Which point runs out first, said plainly, for under the open altars; null when even. */
	private String balanceLine()
	{
		if (!points.known())
		{
			return "Check your points at the Rewards Guardian and the Pilot keeps them even.";
		}
		int e = points.bankedElemental();
		int c = points.bankedCatalytic();
		String game = points.gameElemental() > 0 || points.gameCatalytic() > 0
			? " This game adds about " + points.gameElemental() / 100 + " and " + points.gameCatalytic() / 100
			+ ", each remainder a chance of one more." : "";
		if (e == c)
		{
			return "Points even: " + Session.format(e) + " searches." + game;
		}
		String low = e < c ? "Elemental" : "Catalytic";
		return low + " runs out first: " + Session.format(Math.min(e, c)) + " searches, "
			+ Session.format(Math.abs(e - c)) + " " + (e < c ? "catalytic" : "elemental") + " to spare." + game;
	}

	/** How long the rift has been open, "2:15", or null when not known. */
	private String clock(int tick)
	{
		if (openedTick < 0 || !started)
		{
			return null;
		}
		int seconds = (tick - openedTick) * 6 / 10;
		return seconds / 60 + ":" + String.format("%02d", seconds % 60);
	}

	/**
	 * When the next huge-remains portal is due: the first about 160 seconds in, then every 140
	 * (OSRS wiki, Guardians of the Rift/Strategies). Said as "about", since the wiki does.
	 */
	private String portalNote(int tick)
	{
		if (portalWas)
		{
			return "Portal open now";
		}
		int due;
		if (portalTick >= 0)
		{
			due = portalTick + PORTAL_GAP_TICKS;
		}
		else if (openedTick >= 0)
		{
			due = openedTick + FIRST_PORTAL_TICKS;
		}
		else
		{
			return "";
		}
		int left = (due - tick) * 6 / 10;
		return left > 0 ? "Portal in ~" + left + "s" : "Portal due";
	}

	/** Brief for the panel away from the minigame. */
	View.Brief brief()
	{
		loadPoints();
		String status = points.known()
			? RiftPoints.show(points.bankedElemental()) + " elemental and " + RiftPoints.show(points.bankedCatalytic()) + " catalytic points: "
			+ Session.format(points.searches()) + " searches"
			: null;
		return new View.Brief(View.Activity.RIFT, status,
			"The Temple of the Eye, after the quest of that name and 27 Runecraft. Bring a pickaxe; a chisel for guardians.");
	}

	/** Essence pouches carried: what they hold and room left. */
	static final class Pouches
	{
		final int stored;
		final int room;
		final int[] ids;

		private Pouches(int stored, int room, int[] ids)
		{
			this.stored = stored;
			this.room = room;
			this.ids = ids;
		}

		/**
		 * Capacities are the OSRS wiki's (Essence pouch, Colossal pouch): small 3, medium 6, large
		 * 9, giant 12, and colossal 8, 16, 27 or 40 at 25, 50, 75 and 85 Runecraft. A worn pouch
		 * is counted at its first worn capacity (medium 3, large 7, giant 9, colossal 35 of 40).
		 * What each holds is its varbit, as RuneLite's own Runecraft plugin reads it.
		 */
		static Pouches read(Client client, Pack pack, int rc)
		{
			int stored = 0;
			int room = 0;
			List<Integer> ids = new ArrayList<>();
			int[][] table = {
				{ItemID.RCU_POUCH_SMALL, VarbitID.SMALL_ESSENCE_POUCH, 3, -1, 0},
				{ItemID.RCU_POUCH_MEDIUM, VarbitID.MEDIUM_ESSENCE_POUCH, 6, ItemID.RCU_POUCH_MEDIUM_DEGRADE, 3},
				{ItemID.RCU_POUCH_LARGE, VarbitID.LARGE_ESSENCE_POUCH, 9, ItemID.RCU_POUCH_LARGE_DEGRADE, 7},
				{ItemID.RCU_POUCH_GIANT, VarbitID.GIANT_ESSENCE_POUCH, 12, ItemID.RCU_POUCH_GIANT_DEGRADE, 9},
				{ItemID.RCU_POUCH_COLOSSAL, VarbitID.COLOSSAL_ESSENCE_POUCH, colossal(rc), ItemID.RCU_POUCH_COLOSSAL_DEGRADE, colossal(rc) * 35 / 40},
			};
			for (int[] p : table)
			{
				int cap;
				if (pack.carries(p[0]))
				{
					cap = p[2];
					ids.add(p[0]);
				}
				else if (p[3] > 0 && pack.carries(p[3]))
				{
					cap = p[4];
					ids.add(p[3]);
				}
				else
				{
					continue;
				}
				int held = Math.max(0, client.getVarbitValue(p[1]));
				stored += held;
				room += Math.max(0, cap - held);
			}
			int[] out = new int[ids.size()];
			for (int i = 0; i < out.length; i++)
			{
				out[i] = ids.get(i);
			}
			return new Pouches(stored, room, out);
		}

		static int colossal(int rc)
		{
			return rc >= 85 ? 40 : rc >= 75 ? 27 : rc >= 50 ? 16 : 8;
		}
	}
}
