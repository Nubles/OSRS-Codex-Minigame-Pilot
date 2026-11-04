package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.MixCycle;
import com.codex.minigamepilot.core.MixRounds;
import com.codex.minigamepilot.core.Mixology;
import com.codex.minigamepilot.core.Mixology.Paste;
import com.codex.minigamepilot.core.Mixology.Potion;
import com.codex.minigamepilot.core.Mixology.Station;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

/**
 * Flies Mastering Mixology: which of the three orders to make with the paste in the hopper, the
 * levers for each potion, the vessel, the station each order names (with the agitator's and
 * alembic's cues as they come), the conveyor belt, the hopper kept fed and the digweed used where
 * it pays most; with your resin towards a reward and every delivery kept per account. It only
 * reads the game (varbits, varps, the orders list, what you carry, the objects and spot
 * animations around you) and draws.
 *
 * <p>Ids are RuneLite's gameval names: the {@code MM_LAB_*}, {@code MM_AVAILABLE_*} and
 * {@code MM_HERB_READY_*} varbits, the {@code MIXOLOGY_*_POINTS} resin varps, the
 * {@code MM_LAB_*} objects and the {@code VFX_MACHINERY_ALCHEMY01_*} spot animations. The
 * values of the order, station and mixer varbits (potion and station as their position + 1, a
 * slot 1 mox, 2 aga, 3 lye), that the spot animations are the stations' cues, and how long each
 * cue lasts (two ticks for the agitator, which the wiki also says, one for the alembic) are as the
 * Mastering Mixology plugin by hex-agon (BSD-2) reads them: a cross-check, not a source.
 */
public final class MixologyPilot
{
	static final int[] LEVERS = {ObjectID.MM_LAB_SWITCH_MOX, ObjectID.MM_LAB_SWITCH_AGA, ObjectID.MM_LAB_SWITCH_LYE};
	static final int VESSEL = ObjectID.MM_LAB_VESSEL;
	static final int[] STATIONS = {ObjectID.MM_LAB_MACHINE_AGITATOR, ObjectID.MM_LAB_MACHINE_RETORT, ObjectID.MM_LAB_MACHINE_ALEMBIC};
	static final int HOPPER = ObjectID.MM_LAB_HOPPER;
	static final int REFINER = ObjectID.MM_LAB_MILL;
	static final int CONVEYOR = ObjectID.MM_LAB_CONVEYOR;
	static final int[] DIGWEEDS = {ObjectID.MM_HERB_1, ObjectID.MM_HERB_2, ObjectID.MM_HERB_3, ObjectID.MM_HERB_4};
	static final int[] DIGWEED_READY = {VarbitID.MM_HERB_READY_1, VarbitID.MM_HERB_READY_2, VarbitID.MM_HERB_READY_3,
		VarbitID.MM_HERB_READY_4};
	static final int DIGWEED = ItemID.MM_LAB_SPECIAL_HERB;
	static final int[] ORDER_TYPE = {VarbitID.MM_LAB_ORDER_1_TYPE, VarbitID.MM_LAB_ORDER_2_TYPE, VarbitID.MM_LAB_ORDER_3_TYPE};
	static final int[] ORDER_STATION = {VarbitID.MM_LAB_ORDER_1_MODIFIER, VarbitID.MM_LAB_ORDER_2_MODIFIER,
		VarbitID.MM_LAB_ORDER_3_MODIFIER};
	static final int[] SLOTS = {VarbitID.MM_LAB_MIXER_SLOT_0, VarbitID.MM_LAB_MIXER_SLOT_1, VarbitID.MM_LAB_MIXER_SLOT_2};
	/** By {@link Station#ordinal()}: agitator, retort, alembic. */
	static final int[] STATION_POTION = {VarbitID.MM_LAB_AGITATOR_POTION, VarbitID.MM_LAB_RETORT_POTION,
		VarbitID.MM_LAB_ALEMBIC_POTION};
	static final int[] HOPPER_PASTE = {VarbitID.MM_AVAILABLE_MOX, VarbitID.MM_AVAILABLE_AGA, VarbitID.MM_AVAILABLE_LYE};
	/** By {@link Paste#ordinal()}. */
	static final int[] RESIN = {VarPlayerID.MIXOLOGY_MOX_POINTS, VarPlayerID.MIXOLOGY_AGA_POINTS, VarPlayerID.MIXOLOGY_LYE_POINTS};
	static final int CUE_AGITATOR = SpotanimID.VFX_MACHINERY_ALCHEMY01_AGITATOR01;
	static final int CUE_ALEMBIC = SpotanimID.VFX_MACHINERY_ALCHEMY01_ALEMBIC01;
	/** Ticks a cue stays live, the tick it shows included. */
	static final int AGITATOR_WINDOW = 2;
	static final int ALEMBIC_WINDOW = 1;
	static final String ROUNDS_KEY = "mixology.rounds";

	private final Session session = new Session(Skill.HERBLORE);
	private Function<String, String> load;
	private BiConsumer<String, String> save;
	private IntUnaryOperator price;
	private MixRounds rounds;
	private MixCycle.Plan plan = MixCycle.Plan.ALL;
	private Mixology.Reward goal;
	private boolean cues = true;

	/** The orders as last read, to see them change; and which of them are made. */
	private String ordersSeen = "";
	private final boolean[] done = new boolean[3];
	private final Potion[] lastStation = new Potion[3];
	private Station cue;
	private int cueTick = -1;
	private final int[] lastShot = {-1, -1};
	/** Resin as last read, -1 before it has arrived. */
	private final int[] lastResin = {-1, -1, -1};
	private int roundTick = -1;
	private int roundXp = -1;
	/** The most orders seen made since the last delivery: what the next one fills. */
	private int doneBefore;
	/** Most resin one delivery can pay: three mixalots with digweed and the 40% bonus is 504 of a kind. */
	static final int MOST_A_DELIVERY = 1600;
	private int delivered;
	/** The potion a digweed was last seen worth most on, for the panel. */
	Object lastState;

	void setStore(Function<String, String> load, BiConsumer<String, String> save)
	{
		this.load = load;
		this.save = save;
	}

	void setPrice(IntUnaryOperator price)
	{
		this.price = price;
	}

	void settings(MixCycle.Plan plan, Mixology.Reward goal, boolean cues)
	{
		this.plan = plan == null ? MixCycle.Plan.ALL : plan;
		this.goal = goal;
		this.cues = cues;
	}

	static boolean isWatched(int id)
	{
		return StormPilot.contains(LEVERS, id) || id == VESSEL || StormPilot.contains(STATIONS, id) || id == HOPPER || id == REFINER
			|| id == CONVEYOR || StormPilot.contains(DIGWEEDS, id);
	}

	private MixRounds rounds()
	{
		if (rounds == null)
		{
			rounds = MixRounds.decode(load == null ? null : load.apply(ROUNDS_KEY));
		}
		return rounds;
	}

	/** In the lab: its vessel or conveyor is in the loaded scene. */
	boolean active(Client client, SceneIndex scene)
	{
		return client.getLocalPlayer() != null && (!scene.get(VESSEL).isEmpty() || !scene.get(CONVEYOR).isEmpty());
	}

	/**
	 * A spot animation appeared: the agitator's or alembic's cue. Its window is counted from the
	 * first tick the Pilot works out after it, whichever side of the game tick it arrived on.
	 */
	void spotanim(int id, int tick)
	{
		if (id == CUE_AGITATOR)
		{
			cue = Station.AGITATOR;
			cueTick = -1;
		}
		else if (id == CUE_ALEMBIC)
		{
			cue = Station.ALEMBIC;
			cueTick = -1;
		}
	}

	void left()
	{
		cue = null;
		roundTick = -1;
	}

	void newAccount()
	{
		session.startOver();
		rounds = null;
		ordersSeen = "";
		java.util.Arrays.fill(done, false);
		java.util.Arrays.fill(lastStation, null);
		java.util.Arrays.fill(lastResin, -1);
		roundTick = -1;
		roundXp = -1;
		delivered = 0;
		cue = null;
	}

	/** A running count for the pace coach: deliveries made. */
	int units()
	{
		return delivered;
	}

	/**
	 * Every tick, in the lab or not: a delivery shows as the resin counts rising. Its resin, the
	 * Herblore experience since the last one, the orders it filled and the round's time are kept.
	 */
	void settle(Client client, long now)
	{
		if (client.getSkillExperience(Skill.HITPOINTS) <= 0)
		{
			// Skills and varps have not arrived: nothing read now is a change.
			return;
		}
		int tick = client.getTickCount();
		int xp = client.getSkillExperience(Skill.HERBLORE);
		int[] resin = new int[3];
		int gained = 0;
		int[] delta = new int[3];
		for (int i = 0; i < 3; i++)
		{
			resin[i] = client.getVarpValue(RESIN[i]);
			if (lastResin[i] >= 0 && resin[i] > lastResin[i])
			{
				delta[i] = resin[i] - lastResin[i];
				gained += delta[i];
			}
		}
		if (gained > MOST_A_DELIVERY || roundTick < 0)
		{
			// Counts arriving after a login, or resin earned out of sight: a new base, not a delivery.
			gained = 0;
		}
		if (gained > 0)
		{
			int ticks = roundTick >= 0 ? tick - roundTick : 0;
			int xpGained = roundXp >= 0 && xp > roundXp ? xp - roundXp : 0;
			rounds().add(new MixRounds.Round(now, ticks, delta[0], delta[1], delta[2], xpGained, doneBefore));
			if (save != null)
			{
				save.accept(ROUNDS_KEY, rounds().encode());
			}
			delivered++;
			doneBefore = 0;
			roundTick = tick;
			roundXp = xp;
		}
		System.arraycopy(resin, 0, lastResin, 0, 3);
	}

	Flight fly(Client client, SceneIndex scene, long now)
	{
		session.tick(client, now);
		Pack pack = Pack.read(client);
		WorldPoint at = client.getLocalPlayer().getWorldLocation();
		int tick = client.getTickCount();
		if (roundTick < 0)
		{
			roundTick = tick;
			roundXp = client.getSkillExperience(Skill.HERBLORE);
		}

		MixCycle.Snapshot s = new MixCycle.Snapshot();
		lastState = s;
		s.plan = plan;
		s.level = client.getRealSkillLevel(Skill.HERBLORE);
		readOrders(client, s);
		for (Paste p : Paste.values())
		{
			s.hopper[p.ordinal()] = client.getVarbitValue(HOPPER_PASTE[p.ordinal()]);
			s.pasteCarried[p.ordinal()] = pack.count(p.item());
		}
		for (Mixology.Herb h : Mixology.Herb.values())
		{
			s.herbPaste[h.paste().ordinal()] += pack.count(h.clean(), h.unfinished()) * h.yield();
		}
		for (int i = 0; i < 3; i++)
		{
			s.slots[i] = Paste.ofSlot(client.getVarbitValue(SLOTS[i]));
		}
		s.vessel = Potion.ofVarbit(client.getVarbitValue(VarbitID.MM_LAB_VESSEL_READY));
		for (Potion p : Potion.values())
		{
			s.bases[p.ordinal()] = pack.count(p.unfinished());
			s.finished[p.ordinal()] = pack.count(p.finished());
		}
		for (Station st : Station.values())
		{
			s.stations[st.ordinal()] = Potion.ofVarbit(client.getVarbitValue(STATION_POTION[st.ordinal()]));
		}
		trackDone(client, s);
		s.cue = liveCue(client, s, tick);
		s.digweed = pack.carries(DIGWEED);
		int ready = -1;
		for (int i = 0; i < DIGWEED_READY.length; i++)
		{
			if (client.getVarbitValue(DIGWEED_READY[i]) == 1)
			{
				ready = i;
				break;
			}
		}
		s.digweedReady = ready >= 0;
		s.freeSlots = pack.free();

		MixCycle.Decision d = MixCycle.next(s);
		Step step = withItems(d, s, pack);
		Ground ground = Ground.read(client, Collections.emptyList(), 0);
		Flight flight = route(scene, ground, step, d, ready, at);

		int stage = MixCycle.stage(step);
		View view = new View(true, View.Activity.MIXOLOGY, step, place(s, d), MixCycle.LOOP, Math.max(stage, 0));
		Flight out = new Flight(View.Activity.MIXOLOGY, step, view);
		out.object = flight.object;
		out.route = flight.route;
		dress(view, s, d, client, now);
		return out;
	}

	/**
	 * The three orders from their varbits; when the game's own orders list is on screen, its
	 * names stand over the varbits' (whose numbering is a cross-check only).
	 */
	private void readOrders(Client client, MixCycle.Snapshot s)
	{
		Potion[] potions = new Potion[3];
		Station[] stations = new Station[3];
		StringBuilder seen = new StringBuilder();
		for (int i = 0; i < 3; i++)
		{
			potions[i] = Potion.ofVarbit(client.getVarbitValue(ORDER_TYPE[i]));
			stations[i] = Station.ofVarbit(client.getVarbitValue(ORDER_STATION[i]));
		}
		List<Potion> listed = listed(client);
		int shown = 0;
		for (Potion p : potions)
		{
			shown += p == null ? 0 : 1;
		}
		if (listed.size() == shown)
		{
			int k = 0;
			for (int i = 0; i < 3; i++)
			{
				if (potions[i] != null)
				{
					potions[i] = listed.get(k++);
				}
			}
		}
		for (int i = 0; i < 3; i++)
		{
			seen.append(potions[i]).append(stations[i]).append(',');
		}
		String sig = seen.toString();
		if (!sig.equals(ordersSeen))
		{
			// New orders: none of them is made yet.
			java.util.Arrays.fill(done, false);
			ordersSeen = sig;
		}
		for (int i = 0; i < 3; i++)
		{
			s.orders[i] = potions[i] == null || stations[i] == null ? null : new MixCycle.Order(potions[i], stations[i], done[i]);
		}
	}

	/** The potions the game's orders list names, top to bottom; empty when it is not on screen. */
	static List<Potion> listed(Client client)
	{
		List<Potion> out = new ArrayList<>();
		Widget content = client.getWidget(InterfaceID.MmOverlay.CONTENT);
		if (content == null || content.isHidden())
		{
			return out;
		}
		for (Widget[] kids : new Widget[][]{content.getDynamicChildren(), content.getChildren()})
		{
			if (kids == null)
			{
				continue;
			}
			for (Widget w : kids)
			{
				Potion p = w == null ? null : Potion.ofTitle(w.getText());
				if (p != null)
				{
					out.add(p);
				}
			}
			if (!out.isEmpty())
			{
				break;
			}
		}
		return out;
	}

	/**
	 * A station that has just let its potion go made it for the first open order of its kind and
	 * station. Orders marked made are kept to the worked potions you still carry.
	 */
	private void trackDone(Client client, MixCycle.Snapshot s)
	{
		for (Station st : Station.values())
		{
			Potion now = s.stations[st.ordinal()];
			Potion was = lastStation[st.ordinal()];
			if (was != null && now == null)
			{
				for (int i = 0; i < 3; i++)
				{
					MixCycle.Order o = s.orders[i];
					if (o != null && !done[i] && o.potion == was && o.station == st)
					{
						done[i] = true;
						break;
					}
				}
			}
			lastStation[st.ordinal()] = now;
		}
		int[] left = s.finished.clone();
		for (int i = 0; i < 3; i++)
		{
			MixCycle.Order o = s.orders[i];
			if (o == null)
			{
				continue;
			}
			if (done[i])
			{
				if (left[o.potion.ordinal()] > 0)
				{
					left[o.potion.ordinal()]--;
				}
				else
				{
					// The potion has gone (delivered, dropped): the order is open again.
					done[i] = false;
				}
			}
		}
		for (int i = 0; i < 3; i++)
		{
			MixCycle.Order o = s.orders[i];
			if (o == null)
			{
				continue;
			}
			if (!done[i] && left[o.potion.ordinal()] > 0)
			{
				// A worked potion of its kind that the Pilot did not see leave a station (it was
				// started mid-round): the game's own count is taken as made for this order.
				left[o.potion.ordinal()]--;
				done[i] = true;
			}
			s.orders[i] = new MixCycle.Order(o.potion, o.station, done[i]);
		}
		int n = 0;
		for (boolean b : done)
		{
			n += b ? 1 : 0;
		}
		doneBefore = Math.max(doneBefore, n);
	}

	/** The cue live this tick, cleared once it is hit or its window has passed. */
	private Station liveCue(Client client, MixCycle.Snapshot s, int tick)
	{
		int[] shots = {client.getVarbitValue(VarbitID.MM_LAB_HIT_SKILLSHOT_AGITATOR),
			client.getVarbitValue(VarbitID.MM_LAB_HIT_SKILLSHOT_ALEMBIC)};
		for (int i = 0; i < 2; i++)
		{
			if (lastShot[i] >= 0 && shots[i] != lastShot[i])
			{
				// Hit: the station has taken its click.
				if (cue == (i == 0 ? Station.AGITATOR : Station.ALEMBIC))
				{
					cue = null;
				}
			}
			lastShot[i] = shots[i];
		}
		if (cue == null || !cues)
		{
			return null;
		}
		if (cueTick < 0)
		{
			cueTick = tick;
		}
		int window = cue == Station.AGITATOR ? AGITATOR_WINDOW : ALEMBIC_WINDOW;
		if (tick - cueTick >= window || s.stations[cue.ordinal()] == null)
		{
			cue = null;
			return null;
		}
		return cue;
	}

	/** The items a step is about, ringed in the pack. */
	private static Step withItems(MixCycle.Decision d, MixCycle.Snapshot s, Pack pack)
	{
		Step step = d.step;
		switch (step.getTarget())
		{
			case AGITATOR:
			case RETORT:
			case ALEMBIC:
				return d.potion != null && !step.isWaiting() && !step.getVerb().equals("Click") ? step.withItems(d.potion.unfinished()) : step;
			case CONVEYOR:
			{
				List<Integer> ids = new ArrayList<>();
				for (int i : d.planned)
				{
					ids.add(s.orders[i].potion.finished());
				}
				return step.withItems(ids.stream().mapToInt(Integer::intValue).toArray());
			}
			case REFINER:
			{
				List<Integer> ids = new ArrayList<>();
				for (Mixology.Herb h : Mixology.Herb.values())
				{
					if (pack.carries(h.clean()))
					{
						ids.add(h.clean());
					}
					if (pack.carries(h.unfinished()))
					{
						ids.add(h.unfinished());
					}
				}
				return step.withItems(ids.stream().mapToInt(Integer::intValue).toArray());
			}
			default:
				return step;
		}
	}

	private Flight route(SceneIndex scene, Ground ground, Step step, MixCycle.Decision d, int digweed, WorldPoint at)
	{
		Flight flight = new Flight(View.Activity.MIXOLOGY, step, null);
		Collection<? extends TileObject> things;
		switch (step.getTarget())
		{
			case LEVER:
				things = scene.get(LEVERS[d.lever == null ? 0 : d.lever.ordinal()]);
				break;
			case MIXING_VESSEL:
				things = scene.get(VESSEL);
				break;
			case AGITATOR:
				things = scene.get(STATIONS[Station.AGITATOR.ordinal()]);
				break;
			case RETORT:
				things = scene.get(STATIONS[Station.RETORT.ordinal()]);
				break;
			case ALEMBIC:
				things = scene.get(STATIONS[Station.ALEMBIC.ordinal()]);
				break;
			case CONVEYOR:
				things = scene.get(CONVEYOR);
				break;
			case HOPPER:
				things = scene.get(HOPPER);
				break;
			case REFINER:
				things = scene.get(REFINER);
				break;
			case DIGWEED:
				things = digweed < 0 ? Collections.emptySet() : scene.get(DIGWEEDS[digweed]);
				break;
			case BANK:
				things = scene.banks();
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

	/** "3 orders · 40% bonus", what this round makes. */
	private static String place(MixCycle.Snapshot s, MixCycle.Decision d)
	{
		int open = 0;
		for (MixCycle.Order o : s.orders)
		{
			open += o == null ? 0 : 1;
		}
		if (open == 0)
		{
			return "Mastering Mixology";
		}
		int n = d.planned.size();
		return n + " of " + open + (open == 1 ? " order" : " orders") + (n == 3 ? " · +40% resin" : n == 2 ? " · +20% resin" : "");
	}

	private static final java.awt.Color[] TONES = {Theme.TEAL, Theme.GREEN, Theme.EMBER};

	/** The orders, the hopper, the paste worth refining, your resin towards a reward, and your rounds. */
	private void dress(View view, MixCycle.Snapshot s, MixCycle.Decision d, Client client, long now)
	{
		// The hopper, each paste in orders' worth at your level.
		Paste first = runsOutFirst(s);
		for (Paste p : Paste.values())
		{
			double per = Mixology.pastePerOrder(p, s.level);
			int have = s.hopper[p.ordinal()];
			String note = per <= 0 ? "None ordered at your level"
				: Session.format(Math.round(have / per)) + " orders' worth" + (p == first ? " · out first" : "");
			view.gauge(new View.Gauge(p.getTitle(), have, Math.min(Mixology.HOPPER_CAP - have, s.pasteCarried[p.ordinal()] + s.herbPaste[p.ordinal()]),
				Mixology.HOPPER_CAP, note, TONES[p.ordinal()]));
		}

		// The orders, made, next, to make or skipped.
		List<View.Line> lines = new ArrayList<>();
		Potion about = d.potion;
		boolean nextMarked = false;
		for (int i = 0; i < 3; i++)
		{
			MixCycle.Order o = s.orders[i];
			if (o == null)
			{
				continue;
			}
			boolean planned = d.planned.contains(i);
			View.Line.Mark mark;
			String trailing = o.potion.code() + " · " + o.station.getTitle();
			if (o.done)
			{
				mark = View.Line.Mark.DONE;
			}
			else if (!planned)
			{
				mark = View.Line.Mark.IDLE;
				trailing = o.potion.level() > s.level ? "Herblore " + o.potion.level() : skipWhy(s, o);
			}
			else if (!nextMarked && o.potion == about)
			{
				mark = View.Line.Mark.NEXT;
				nextMarked = true;
			}
			else
			{
				mark = View.Line.Mark.TODO;
			}
			lines.add(new View.Line(o.potion.getTitle(), trailing, mark));
		}
		if (!lines.isEmpty())
		{
			int resin = 0;
			int xp = 0;
			for (int i : d.planned)
			{
				resin += s.orders[i].potion.totalResin();
				xp += s.orders[i].potion.xp();
			}
			view.lines("The orders", d.planned.isEmpty() ? "None to make"
				: Session.format(Math.round(Mixology.withBonus(resin, d.planned.size()))) + " resin · " + Session.format(xp) + " xp", lines);
		}

		view.pick(pasteCard(s, first));

		int[] resin = new int[3];
		for (int i = 0; i < 3; i++)
		{
			resin[i] = Math.max(0, client.getVarpValue(RESIN[i]));
		}
		if (goal != null)
		{
			view.goal(goalCard(resin));
		}

		MixRounds g = rounds();
		view.stat(new View.Stat(Session.format(delivered), "Deliveries", session.perHour(delivered, now), Theme.GOLD));
		int xp = session.xp(client);
		view.stat(new View.Stat(Session.format(xp), "Herblore xp", session.perHour(xp, now), Theme.GREEN));
		view.stat(new View.Stat(Session.format(resin[0] + resin[1] + resin[2]), "Resin",
			compact(resin[0]) + " · " + compact(resin[1]) + " · " + compact(resin[2]), Theme.GOLD_BRIGHT));
		double hour = g.resinPerHour();
		view.stat(new View.Stat(hour < 0 ? "-" : Session.format(Math.round(hour)), "Resin an hour",
			hour < 0 ? "After a timed round" : "Over " + g.count() + (g.count() == 1 ? " round" : " rounds"), Theme.TEAL));

		if (g.count() > 0)
		{
			view.supply(roundsCard(g));
		}
	}

	private static String skipWhy(MixCycle.Snapshot s, MixCycle.Order o)
	{
		for (Paste p : Paste.values())
		{
			if (s.hopper[p.ordinal()] < o.potion.paste(p))
			{
				return "short of " + p.getTitle().toLowerCase(Locale.ROOT);
			}
		}
		return "skipped";
	}

	/**
	 * The paste that runs out first at your level's orders, or null with none ordered or when all
	 * three last within an order of each other.
	 */
	static Paste runsOutFirst(MixCycle.Snapshot s)
	{
		Paste first = null;
		double least = Double.MAX_VALUE;
		double most = 0;
		for (Paste p : Paste.values())
		{
			double per = Mixology.pastePerOrder(p, s.level);
			if (per <= 0)
			{
				continue;
			}
			double orders = s.hopper[p.ordinal()] / per;
			most = Math.max(most, orders);
			if (orders < least)
			{
				least = orders;
				first = p;
			}
		}
		return most - least < 1 ? null : first;
	}

	/** What to put in the hopper: each paste in orders' worth, the herb that makes it cheapest now. */
	private View.PickCard pasteCard(MixCycle.Snapshot s, Paste first)
	{
		List<View.Pick> rows = new ArrayList<>();
		for (Paste p : Paste.values())
		{
			double per = Mixology.pastePerOrder(p, s.level);
			int have = s.hopper[p.ordinal()];
			Mixology.Herb cheap = cheapest(p);
			String detail = cheap != null ? cheap.getTitle() + ", " + gpPerPaste(cheap) + " gp a paste"
				: per <= 0 ? "Not ordered at your level" : Session.format(Math.round(per)) + " an order on average";
			rows.add(new View.Pick(p.getTitle(), Session.format(have), detail, true, p == first,
				Math.min(1, have / (double) Mixology.HOPPER_CAP), false));
		}
		String note = first == null ? "Balanced for your level's orders: refine whichever herbs are cheapest. The hopper holds up to 3,000 of each."
			: "Refine " + first.getTitle().toLowerCase(Locale.ROOT) + " next: at your level's orders it runs out first."
				+ (cheapest(first) == null ? "" : " " + cheapest(first).getTitle() + " makes it cheapest at today's prices.");
		return new View.PickCard("Paste", first == null ? "balanced" : first.getTitle() + " first", note, rows);
	}

	/** The herb that makes a paste for the fewest coins, at today's prices; null without prices. */
	Mixology.Herb cheapest(Paste p)
	{
		Mixology.Herb best = null;
		double bestCost = Double.MAX_VALUE;
		for (Mixology.Herb h : Mixology.Herb.values())
		{
			if (h.paste() != p || price == null)
			{
				continue;
			}
			int gp = price.applyAsInt(h.clean());
			if (gp <= 0)
			{
				continue;
			}
			double cost = gp / (double) h.yield();
			if (cost < bestCost)
			{
				bestCost = cost;
				best = h;
			}
		}
		return best;
	}

	private String gpPerPaste(Mixology.Herb h)
	{
		double cost = price.applyAsInt(h.clean()) / (double) h.yield();
		return cost < 10 ? String.format(Locale.ROOT, "%.1f", cost) : Session.format(Math.round(cost));
	}

	/** Your resin against the reward you are saving for, and the time it is at your pace. */
	private View.GoalCard goalCard(int[] resin)
	{
		long have = 0;
		long need = 0;
		List<View.Line> pieces = new ArrayList<>();
		boolean done = true;
		for (Paste p : Paste.values())
		{
			int cost = goal.cost(p);
			int got = resin[p.ordinal()];
			have += Math.min(got, cost);
			need += cost;
			boolean enough = got >= cost;
			done &= enough;
			pieces.add(new View.Line(p.getTitle() + " resin", Session.format(got) + " / " + Session.format(cost),
				enough ? View.Line.Mark.DONE : View.Line.Mark.TODO));
		}
		String note;
		if (done)
		{
			note = "You can buy it from Lalo now.";
		}
		else
		{
			double hours = rounds().hoursTo(goal, resin);
			Paste slow = slowest(resin);
			note = hours > 0 ? "About " + clockHours(hours) + " at your pace" + (slow == null ? "" : ", " + slow.getTitle().toLowerCase(Locale.ROOT) + " last")
				: "Its time shows after a delivery or two";
		}
		return new View.GoalCard(View.Activity.MIXOLOGY, goal.getTitle(), have, need, "resin", note, pieces, done);
	}

	/** The resin a reward waits on longest at your pace, or null without one. */
	private Paste slowest(int[] resin)
	{
		Paste slow = null;
		double longest = 0;
		for (Paste p : Paste.values())
		{
			int need = goal.cost(p) - resin[p.ordinal()];
			double h = rounds().perHour(p);
			if (need > 0 && h > 0 && need / h > longest)
			{
				longest = need / h;
				slow = p;
			}
		}
		return slow;
	}

	/** "6.2k", "940": short enough for three in a tile's note. */
	static String compact(int n)
	{
		return n < 1000 ? Integer.toString(n) : n < 100_000 ? String.format(Locale.ROOT, "%.1fk", n / 1000.0) : n / 1000 + "k";
	}

	static String clockHours(double hours)
	{
		long minutes = Math.round(hours * 60);
		return minutes < 60 ? minutes + "m" : minutes / 60 + "h " + minutes % 60 + "m";
	}

	/** Your last rounds, the best, and resin and experience an hour across them. */
	private static View.Supply roundsCard(MixRounds g)
	{
		List<View.Line> lines = new ArrayList<>();
		List<MixRounds.Round> all = g.all();
		MixRounds.Round best = g.best();
		for (int i = all.size() - 1; i >= 0 && i >= all.size() - 5; i--)
		{
			MixRounds.Round r = all.get(i);
			lines.add(new View.Line(resinLine(r) + (r.ticks > 0 ? " · " + StormPilot.clock(r.ticks) : ""),
				Session.format(r.xp) + " xp", r == best ? View.Line.Mark.DONE : View.Line.Mark.IDLE));
		}
		String headline = best == null ? null : "Best round: " + Session.format(best.resin()) + " resin in " + StormPilot.clock(best.ticks);
		double xpHour = g.xpPerHour();
		double resinHour = g.resinPerHour();
		String detail = resinHour < 0 ? null : Session.format(Math.round(resinHour)) + " resin and " + Session.format(Math.round(xpHour))
			+ " Herblore xp an hour over your timed rounds";
		return new View.Supply(g.count() == 1 ? "1 kept" : g.count() + " kept", lines, headline, detail, false, null).titled("Your rounds");
	}

	static String resinLine(MixRounds.Round r)
	{
		StringBuilder b = new StringBuilder();
		int[] n = {r.mox, r.aga, r.lye};
		for (Paste p : Paste.values())
		{
			if (n[p.ordinal()] > 0)
			{
				if (b.length() > 0)
				{
					b.append(' ');
				}
				b.append(Session.format(n[p.ordinal()])).append(p.letter());
			}
		}
		return b.length() == 0 ? "No resin" : b.toString();
	}

	/** Brief for the panel away from the minigame. */
	View.Brief brief()
	{
		MixRounds g = rounds();
		String status = null;
		MixRounds.Round last = g.last();
		if (last != null)
		{
			status = "Last delivery: " + Session.format(last.resin()) + " resin (" + resinLine(last) + "), "
				+ Session.format(last.xp) + " Herblore xp" + (last.ticks > 0 ? " in " + StormPilot.clock(last.ticks) : "");
		}
		View.Brief b = new View.Brief(View.Activity.MIXOLOGY, status,
			"In Aldarin, after Children of the Sun, from 60 Herblore. Bring clean herbs or unfinished potions for paste.");
		double hour = g.resinPerHour();
		if (hour >= 0)
		{
			b.best(Session.format(Math.round(hour)) + " resin and " + Session.format(Math.round(g.xpPerHour())) + " xp an hour over "
				+ g.count() + (g.count() == 1 ? " round." : " rounds."));
		}
		return b;
	}
}
