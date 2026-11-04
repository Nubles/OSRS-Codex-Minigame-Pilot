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
import net.runelite.api.Skill;
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
		Skill skill = skill(flight.activity);
		// Every account has some Hitpoints experience: none means the skills have not arrived yet.
		int xp = skill == null || client.getSkillExperience(Skill.HITPOINTS) <= 0 ? -1 : client.getSkillExperience(skill);
		Pace.Round done = pace.tick(flight.view.loopAt(), leg, place(step), still, step.isWaiting(), moved, route, units, kindHint, xp);
		// Every fair round can set a stage best even when the round as a whole was slower.
		if (done != null && !done.hadBreak)
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

	/** The skill a minigame trains. */
	static Skill skill(View.Activity activity)
	{
		switch (activity)
		{
			case MOTHERLODE:
				return Skill.MINING;
			case FURNACE:
				return Skill.SMITHING;
			case HOMES:
				return Skill.CONSTRUCTION;
			case RIFT:
				return Skill.RUNECRAFT;
			case TEMPOROSS:
				return Skill.FISHING;
			case FOUNDRY:
				return Skill.SMITHING;
			case MIXOLOGY:
				return Skill.HERBLORE;
			case TOTEMS:
				return Skill.FLETCHING;
			case TITHE:
				return Skill.FARMING;
			case ARTEFACTS:
			case HOUSES:
				return Skill.THIEVING;
			case ROGUES_DEN:
				return Skill.AGILITY;
			default:
				return null;
		}
	}

	/**
	 * Adds the tick coach's card: the last loop in ticks against your best and the floor of its
	 * walks, its biggest leak, loops and experience an hour, and the loops as a histogram.
	 */
	void tickCard(View view, View.Activity activity)
	{
		Pace p = paces.get(activity);
		Pace.Round last = p == null ? null : p.last();
		if (last == null)
		{
			return;
		}
		view.ticks(card(p, last, view.loopNames(), skill(activity)));
	}

	/** The tick coach's card for the last round of a pace. Package-private for tests. */
	static View.TickCard card(Pace p, Pace.Round last, String[] names, Skill skill)
	{
		com.codex.minigamepilot.core.Loops loops = com.codex.minigamepilot.core.Loops.of(p.rounds());
		Pace.Leak leak = last.leak();
		String text = last.hadBreak ? "A break in this loop, so it is left out" : leak.text(names);
		String xpLabel = skill == null ? "xp" : skill.getName() + " xp";
		int best = p.best(last.kind);
		return new View.TickCard(last.ticks, best, last.footTicks(), last.floorTicks(), text,
			leak.kind != Pace.Leak.Kind.NONE, loops.perHour(), loops.xpPerHour(), xpLabel, loops.count(),
			loops.histogram(last, best)).first(last.bestBefore == 0 && !last.hadBreak);
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
		View.PaceCard card = new View.PaceCard(last.ticks, best, last.isBest(),
			last.bestBefore > 0 && !last.hadBreak ? last.ticks - last.bestBefore : 0,
			last.advice(), last.costly(), tail);
		String[] names = view.loopNames();
		if (names != null && !last.hadBreak)
		{
			card.splits(splits(last, names), splitNote(last, names));
		}
		view.pace(card);
	}

	/** The round's stages in loop order, those it spent time in. */
	static java.util.List<View.Split> splits(Pace.Round round, String[] names)
	{
		java.util.List<View.Split> list = new java.util.ArrayList<>();
		int slow = round.slowestStage();
		for (int s = 0; s < names.length; s++)
		{
			if (round.stageTicks(s) > 0)
			{
				list.add(new View.Split(names[s], round.stageTicks(s), round.stageBest(s), s == slow));
			}
		}
		return list;
	}

	/** "Bank took 0:14, 0:05 over your best", or null when no stage lost much. */
	static String splitNote(Pace.Round round, String[] names)
	{
		int slow = round.slowestStage();
		if (slow < 0 || slow >= names.length)
		{
			return null;
		}
		int ticks = round.stageTicks(slow);
		int over = ticks - round.stageBest(slow);
		return names[slow] + " took " + Pace.clock(ticks) + ", " + Pace.clock(over) + " over your best for it";
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
			case REMAINS:
				return "the remains";
			case WORKBENCH:
				return "the workbench";
			case CELLS:
				return "the cell table";
			case GUARDIAN:
				return "the portal guardian";
			case ALTAR:
				return "the altar";
			case EXIT:
				return "the altar's portal";
			case GREAT_GUARDIAN:
				return "the Great Guardian";
			case CELL_TILE:
				return "the cell tile";
			case DEPOSIT_POOL:
				return "the deposit pool";
			case RIFT_PORTAL:
				return "the portal";
			case HUGE_REMAINS:
				return "the huge remains";
			case FISHING_SPOT:
				return "the fishing spot";
			case SHRINE:
				return "the shrine";
			case AMMO_CRATE:
				return "the ammunition crate";
			case TETHER:
				return "the totem";
			case FIRE:
				return "the fire";
			case SPIRIT_POOL:
				return "the spirit pool";
			case SUPPLY_CRATE:
				return "the crate";
			case WATER_PUMP:
				return "the pump";
			case KOVAC:
				return "Kovac";
			case MOULD_JIG:
				return "the mould jig";
			case CRUCIBLE:
				return "the crucible";
			case BANK_CHEST:
				return "the bank chest";
			case TRIP_HAMMER:
				return "the trip hammer";
			case GRINDSTONE:
				return "the grindstone";
			case POLISHING_WHEEL:
				return "the polishing wheel";
			case LAVA_POOL:
				return "the lava pool";
			case WATERFALL:
				return "the waterfall";
			case PREFORM_STORAGE:
				return "the preform storage";
			case LEVER:
				return "the levers";
			case MIXING_VESSEL:
				return "the vessel";
			case AGITATOR:
				return "the agitator";
			case RETORT:
				return "the retort";
			case ALEMBIC:
				return "the alembic";
			case CONVEYOR:
				return "the conveyor belt";
			case REFINER:
				return "the refiner";
			case DIGWEED:
				return "the digweed";
			case TOTEM:
				return "the totem";
			case OFFERINGS:
				return "the offerings";
			case PLOT:
				return "the patch";
			case SEED_TABLE:
				return "the seed table";
			case WATER_BARREL:
				return "the water barrel";
			case FARM_DOOR:
				return "the farm door";
			case KHALED:
				return "Captain Khaled";
			case DRAWERS:
				return "the drawers";
			case HOUSE:
				return "the house";
			case CITIZEN:
				return "the citizen";
			case LOCKED_DOOR:
				return "the locked door";
			case LOOT:
				return "the chest";
			case WINDOW:
				return "the window";
			case ORIANA:
				return "Oriana";
			case URCHIN:
				return "the urchin";
			case DOORWAY:
				return "the doorway";
			case BANKER:
				return "Emerald Benedict";
			case OBSTACLE:
				return "the obstacle";
			case GUARD:
				return "the guard";
			case WALL_SAFE:
				return "the wall safe";
			case COMMANDER:
				return "Commander Connad";
			case ROOTS:
				return "the bruma roots";
			case BRAZIER:
				return "the brazier";
			case PYROMANCER:
				return "the Pyromancer";
			case TOOL_CRATE:
				return "the crate";
			case REWARD_CART:
				return "the reward cart";
			default:
				return null;
		}
	}
}
