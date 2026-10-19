package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.ItemIds;
import com.codex.minigamepilot.core.MotherlodeCycle;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.Collection;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;

/** Flies the Motherlode Mine: which vein, when to deposit, when to empty the sack, and the way there. */
public final class MotherlodePilot
{
	private static final Set<Integer> REGIONS = Set.of(14679, 14680, 14681, 14935, 14936, 14937, 15191, 15192, 15193);
	static final int[] VEINS = {ObjectID.MOTHERLODE_ORE_SINGLE, ObjectID.MOTHERLODE_ORE_LEFT, ObjectID.MOTHERLODE_ORE_MIDDLE, ObjectID.MOTHERLODE_ORE_RIGHT};
	static final int[] SACKS = {ObjectID.MOTHERLODE_SACK, ObjectID.MOTHERLODE_SACK_EMPTY, ObjectID.MOTHERLODE_SACK_FULL};
	static final int[] ROCKFALLS = {ObjectID.MOTHERLODE_ROCKFALL_1, ObjectID.MOTHERLODE_ROCKFALL_2};
	static final int[] LADDERS = {
		ObjectID.MOTHERLODE_LADDER_BOTTOM, ObjectID.MOTHERLODE_LADDER_TOP, ObjectID.MOTHERLODE_LADDER_BOTTOM_ACTIVE,
		ObjectID.MOTHERLODE_LADDER_BOTTOM_INACTIVE, ObjectID.MOTHERLODE_LADDER_TOP_ACTIVE, ObjectID.MOTHERLODE_LADDER_TOP_INACTIVE
	};
	/** Steps a rockfall is worth: walking around it is better unless that is a long way. */
	private static final int ROCKFALL_COST = 8;
	/** Steps charged for a vein someone else is standing at. */
	private static final int CROWDED = 12;
	private static final String[] LOOP = {"Mine", "Hopper", "Sack", "Bank"};

	private final MotherlodeCycle cycle = new MotherlodeCycle();
	private final Session session = new Session(Skill.MINING);
	private int lastSack = -1;
	private int collected;
	private int sacks;
	private final com.codex.minigamepilot.core.OreHaul haul = new com.codex.minigamepilot.core.OreHaul();
	/** Grand Exchange prices, or null when not set. */
	private java.util.function.IntUnaryOperator price;
	private boolean wasEmptying;
	/** The last snapshot planned from, for the field notes. */
	Object lastState;

	/** A running count of what rounds produce, for the pace coach. */
	int units()
	{
		return cycle.getDeposited();
	}

	static boolean isWatched(int id)
	{
		return contains(VEINS, id) || contains(SACKS, id) || contains(ROCKFALLS, id) || contains(LADDERS, id)
			|| id == ObjectID.MOTHERLODE_HOPPER || id == ObjectID.MOTHERLODE_WHEEL_STRUT_BROKEN;
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

	public static boolean inMine(Client client)
	{
		Player me = client.getLocalPlayer();
		return me != null && REGIONS.contains(me.getWorldLocation().getRegionID());
	}

	/** Called when leaving the mine, so pay-dirt in flight is not carried over into a later visit. */
	void left()
	{
		cycle.reset();
		lastSack = -1;
		haul.forget();
	}

	void setPrice(java.util.function.IntUnaryOperator price)
	{
		this.price = price;
	}

	/** Brief for the panel when the player is elsewhere. */
	View.Brief brief()
	{
		String status = session.begun() ? Session.format(cycle.getDeposited()) + " pay-dirt deposited this session" : null;
		return new View.Brief(View.Activity.MOTHERLODE, status,
			"Under Falador, through the Dwarven Mine or the Falador mining guild. Bring a pickaxe; a hammer helps.");
	}

	Flight fly(Client client, SceneIndex scene, long now)
	{
		session.tick(client, now);
		Pack pack = Pack.read(client);
		Collection<TileObject> rockfalls = scene.get(ROCKFALLS);
		Ground ground = Ground.read(client, rockfalls, ROCKFALL_COST);

		MotherlodeCycle.Snapshot s = new MotherlodeCycle.Snapshot();
		lastState = s;
		s.payDirt = pack.count(ItemIds.PAYDIRT);
		s.free = pack.free();
		s.loot = pack.count(ItemIds.MOTHERLODE_LOOT);
		s.sack = client.getVarbitValue(VarbitID.MOTHERLODE_SACK_TRANSMIT);
		s.capacity = client.getVarbitValue(VarbitID.MOTHERLODE_BIGGERSACK) == 1 ? 189 : 108;
		Collection<TileObject> struts = scene.get(ObjectID.MOTHERLODE_WHEEL_STRUT_BROKEN);
		s.brokenStruts = struts.size();
		s.hammer = pack.carries(ItemIds.HAMMER, ItemIds.IMCANDO_HAMMER) || pack.wears(ItemIds.IMCANDO_HAMMER_OFFHAND, ItemIds.IMCANDO_HAMMER);

		TileObject hopper = first(scene.get(ObjectID.MOTHERLODE_HOPPER));
		boolean nearHopper = hopper != null && client.getLocalPlayer().getWorldLocation().distanceTo(hopper.getWorldLocation()) <= 2;
		cycle.observe(s.payDirt, s.sack, nearHopper, s.brokenStruts >= 2);
		boolean paidOut = lastSack >= 0 && s.sack < lastSack;
		if (paidOut)
		{
			collected += lastSack - s.sack;
		}
		lastSack = s.sack;
		int[] ores = new int[com.codex.minigamepilot.core.OreHaul.ORES.length];
		for (int i = 0; i < ores.length; i++)
		{
			ores[i] = pack.count(com.codex.minigamepilot.core.OreHaul.ORES[i]);
		}
		haul.observe(ores, paidOut);

		Step step = cycle.next(s);
		if (wasEmptying && !cycle.isEmptying())
		{
			sacks++;
		}
		wasEmptying = cycle.isEmptying();

		Ground.Way<TileObject> way = null;
		if (ground != null)
		{
			switch (step.getTarget())
			{
				case VEIN:
					way = ground.nearest(scene.get(VEINS), v -> crowded(client, v) ? CROWDED : 0);
					break;
				case HOPPER:
					way = ground.to(hopper);
					break;
				case SACK:
					way = ground.nearest(scene.get(SACKS), null);
					break;
				case BANK:
					way = ground.nearest(scene.banks(), null);
					break;
				case STRUT:
					way = ground.nearest(struts, null);
					break;
				default:
					break;
			}
			if (way != null)
			{
				// A route through a rockfall means mining it is quicker than walking around.
				TileObject rock = Ground.crossed(way.scenePath, rockfalls);
				if (rock != null)
				{
					step = Step.of("Clear", "Mine the rockfall", "It blocks the quickest way to " + what(step.getTarget()), Step.Target.ROCKFALL);
					way = ground.to(rock);
				}
			}
			else if (step.getTarget() != Step.Target.NONE && !step.isWaiting())
			{
				Ground.Way<TileObject> ladder = ground.nearest(scene.get(LADDERS), null);
				if (ladder != null)
				{
					step = Step.of("Climb", "Climb the ladder", "The " + what(step.getTarget()) + " is on the other level", Step.Target.LADDER);
					way = ladder;
				}
			}
		}

		View view = new View(true, View.Activity.MOTHERLODE, step, level(client), LOOP, stage(step, cycle.isEmptying()));
		int room = cycle.room(s);
		view.gauge(new View.Gauge("Sack", s.sack, cycle.getInFlight(), s.capacity,
			room == 0 ? "Full: empty it into the bank" : "Room for " + room + " more", s.sack + cycle.getInFlight() >= s.capacity ? Theme.EMBER : Theme.GOLD));
		if (!cycle.isEmptying())
		{
			int load = cycle.loadSize(s);
			view.gauge(new View.Gauge("This load", s.payDirt, 0, Math.max(load, s.payDirt), load < s.free + s.payDirt ? "Sized to fill the sack" : "A full inventory", Theme.TEAL));
		}
		if (s.brokenStruts > 0)
		{
			view.gauge(new View.Gauge("Struts working", 2 - Math.min(2, s.brokenStruts), 0, 2,
				s.brokenStruts >= 2 ? "The wheel has stopped" : "One broken: the wheel still turns", s.brokenStruts >= 2 ? Theme.BLOOD : Theme.EMBER));
		}
		view.stat(new View.Stat(Session.format(cycle.getDeposited()), "Pay-dirt", session.perHour(cycle.getDeposited(), now), Theme.GOLD));
		view.stat(new View.Stat(Session.format(collected), "Ore", sacks + (sacks == 1 ? " sack emptied" : " sacks emptied"), Theme.TEAL));
		int xp = session.xp(client);
		view.stat(new View.Stat(Session.format(xp), "Mining xp", session.perHour(xp, now), Theme.GREEN));
		view.stat(new View.Stat(session.elapsed(now), "Time", "In the mine", Theme.TEXT_MUTED));
		long worth = price == null ? 0 : haul.worth(price);
		if (worth > 0)
		{
			String rate = session.perHour(worth, now);
			view.stat(new View.Stat(Session.format(worth), "Ore worth", rate.isEmpty() ? "Grand Exchange prices" : rate, Theme.GOLD_BRIGHT));
		}

		Flight flight = new Flight(View.Activity.MOTHERLODE, step, view).to(way);
		if (step.getTarget() == Step.Target.VEIN && way != null)
		{
			// The next vein along, quietly, so the eye already knows where to go after this one.
			Ground.Way<TileObject> next = ground.nearest(scene.get(VEINS), v -> v == flight.object ? 1000 : crowded(client, v) ? CROWDED : 0);
			if (next != null && next.thing != flight.object)
			{
				flight.others.add(next.thing);
			}
		}
		return flight;
	}

	private static String what(Step.Target t)
	{
		switch (t)
		{
			case VEIN:
				return "the next vein";
			case HOPPER:
				return "hopper";
			case SACK:
				return "sack";
			case BANK:
				return "bank";
			case STRUT:
				return "broken strut";
			default:
				return "next stop";
		}
	}

	private static int stage(Step step, boolean emptying)
	{
		switch (step.getTarget())
		{
			case VEIN:
			case ROCKFALL:
				return 0;
			case HOPPER:
			case STRUT:
				return emptying ? 2 : 1;
			case SACK:
				return 2;
			case BANK:
				return 3;
			default:
				return emptying ? 2 : 0;
		}
	}

	private static String level(Client client)
	{
		Player me = client.getLocalPlayer();
		if (me == null)
		{
			return null;
		}
		// The upper level sits on the same floor as the lower, only higher; the core plugin tells them apart by tile height.
		int height = net.runelite.api.Perspective.getTileHeight(client, me.getLocalLocation(), 0);
		return height < -490 ? "Upper level" : "Lower level";
	}

	private static boolean crowded(Client client, TileObject vein)
	{
		WorldPoint at = vein.getWorldLocation();
		for (Player p : client.getTopLevelWorldView().players())
		{
			if (p != null && p != client.getLocalPlayer() && p.getWorldLocation().distanceTo(at) <= 1)
			{
				return true;
			}
		}
		return false;
	}

	private static TileObject first(Collection<TileObject> objects)
	{
		return objects.isEmpty() ? null : objects.iterator().next();
	}
}
