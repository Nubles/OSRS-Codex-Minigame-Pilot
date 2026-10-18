package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Pace;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.ui.View;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;

/**
 * Watches you play, a tick at a time, and hands the pace coach what it needs: whether you stood
 * still with something to do, how far you moved, how far the step was. Then it writes the
 * coach's verdict into the panel.
 */
final class Coach
{
	/**
	 * Interfaces that mean you are busy even though you are not moving: the bank, dialogue, the
	 * make-how-many box and the furniture menu.
	 */
	private static final int[] BUSY = {12, 193, 217, 219, 229, 231, 270, 397, 458};

	private final Map<View.Activity, Pace> paces = new EnumMap<>(View.Activity.class);
	private final Function<View.Activity, String> load;
	private final BiConsumer<View.Activity, String> save;
	private WorldPoint lastAt;
	private View.Activity lastActivity = View.Activity.NONE;

	Coach(Function<View.Activity, String> load, BiConsumer<View.Activity, String> save)
	{
		this.load = load;
		this.save = save;
	}

	/** After a profile change: the bests of the account now logged in. */
	void reload()
	{
		for (Map.Entry<View.Activity, Pace> e : paces.entrySet())
		{
			e.getValue().loadBests(load.apply(e.getKey()));
		}
	}

	Pace pace(View.Activity activity)
	{
		return paces.computeIfAbsent(activity, a ->
		{
			Pace p = new Pace();
			p.loadBests(load.apply(a));
			return p;
		});
	}

	/** One tick in a minigame. Returns the round that just finished, or null. */
	Pace.Round tick(Client client, Flight flight, int units, String kindHint)
	{
		Player me = client.getLocalPlayer();
		if (me == null || flight.step == null || flight.activity == View.Activity.NONE)
		{
			left();
			return null;
		}
		if (flight.activity != lastActivity)
		{
			left();
			lastActivity = flight.activity;
		}
		WorldPoint at = me.getWorldLocation();
		int moved = 0;
		if (lastAt != null && at.getPlane() == lastAt.getPlane())
		{
			int d = Math.max(Math.abs(at.getX() - lastAt.getX()), Math.abs(at.getY() - lastAt.getY()));
			// More than a running stride is a ladder or a teleport, not walking.
			moved = d <= 2 ? d : 0;
		}
		lastAt = at;
		boolean still = moved == 0 && me.getAnimation() == -1 && !busy(client);

		Step step = flight.step;
		WorldPoint where = flight.object != null ? flight.object.getWorldLocation()
			: flight.npc != null ? flight.npc.getWorldLocation() : flight.destination;
		String leg = step.getVerb() + "|" + step.getTarget() + "|" + where;
		int route = flight.route == null || flight.route.isEmpty() ? -1 : flight.route.size();
		Pace pace = pace(flight.activity);
		Pace.Round done = pace.tick(flight.view.loopAt(), leg, place(step), still, step.isWaiting(), moved, route, units, kindHint);
		if (done != null && done.isBest())
		{
			save.accept(flight.activity, pace.saveBests());
		}
		return done;
	}

	/** Out of the minigames: any round in progress is dropped. */
	void left()
	{
		if (lastActivity != View.Activity.NONE)
		{
			Pace p = paces.get(lastActivity);
			if (p != null)
			{
				p.reset();
			}
		}
		lastActivity = View.Activity.NONE;
		lastAt = null;
	}

	/** Adds the coach's card to the panel, once a round has been timed. */
	void decorate(View view, View.Activity activity)
	{
		Pace p = paces.get(activity);
		Pace.Round last = p == null ? null : p.last();
		if (last == null)
		{
			return;
		}
		int best = p.best(last.kind);
		int typical = p.typical(last.kind);
		int rounds = p.rounds().size();
		String tail = rounds + (rounds == 1 ? " round" : " rounds")
			+ (typical > 0 && rounds > 2 ? " · usually " + Pace.clock(typical) : "")
			+ (p.idleTotal() > 0 ? " · " + Pace.clock(p.idleTotal()) + " idle" : "");
		view.pace(new View.PaceCard(last.ticks, best, last.isBest(),
			last.bestBefore > 0 && !last.hadBreak ? last.ticks - last.bestBefore : 0,
			last.advice(), last.costly(), tail));
	}

	private static boolean busy(Client client)
	{
		for (int group : BUSY)
		{
			Widget w = client.getWidget(group, 0);
			if (w != null && !w.isHidden())
			{
				return true;
			}
		}
		return false;
	}

	/** Where a step is, as the coach says it: "Stood still 4 ticks at the bank". */
	static String place(Step step)
	{
		switch (step.getTarget())
		{
			case VEIN:
				return "a vein";
			case HOPPER:
				return "the hopper";
			case SACK:
				return "the sack";
			case BANK:
				return "the bank";
			case STRUT:
				return "the strut";
			case ROCKFALL:
				return "the rockfall";
			case LADDER:
				return "the ladder";
			case BELT:
				return "the belt";
			case DISPENSER:
				return "the dispenser";
			case COFFER:
				return "the coffer";
			case CONTRACTOR:
				return "the contractor";
			case CLIENT:
				return "the client";
			case HOTSPOT:
				return "the furniture";
			case HOME:
				return "the house";
			default:
				return null;
		}
	}
}
