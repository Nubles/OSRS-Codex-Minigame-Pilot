package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Bar;
import com.codex.minigamepilot.core.FurnaceCycle;
import com.codex.minigamepilot.core.ItemIds;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.Collection;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;

/** Flies the Blast Furnace: what to withdraw, when to load, when to take the bars, and coal trips worked out ahead. */
public final class FurnacePilot
{
	private static final int REGION = 7757;
	static final int[] COFFERS = {ObjectID.BLAST_FURNACE_AUTOMATA_COFFER, ObjectID.BLAST_FURNACE_AUTOMATA_COFFER_EMPTY, ObjectID.BLAST_FURNACE_AUTOMATA_COFFER_FULL};
	private static final String[] LOOP = {"Bank", "Belt", "Bars"};
	/** The order ore is recognised in when working out what is being smelted. */
	private static final Bar[] BY_VALUE = {Bar.RUNITE, Bar.ADAMANTITE, Bar.MITHRIL, Bar.GOLD, Bar.SILVER, Bar.STEEL};
	/** Coins the coffer burns an hour. */
	private static final int COFFER_PER_HOUR = 72_000;

	private final FurnaceCycle cycle = new FurnaceCycle();
	private final Session session = new Session(Skill.SMITHING);
	final CoalBag coalBag = new CoalBag();
	private Bar last;
	private int lastBars = -1;
	private int barsTaken;
	private int loads;
	private int lastOre = -1;

	static boolean isWatched(int id)
	{
		return id == ObjectID.BLAST_FURNACE_CONVEYER_BELT_CLICKABLE || id == ObjectID.BLAST_FURNACE_DISPENSER
			|| MotherlodePilot.contains(COFFERS, id);
	}

	public static boolean atFurnace(Client client)
	{
		Player me = client.getLocalPlayer();
		return me != null && me.getWorldLocation().getRegionID() == REGION;
	}

	void left()
	{
		cycle.reset();
		lastBars = -1;
		lastOre = -1;
	}

	View.Brief brief()
	{
		String status = session.begun() ? barsTaken + " bars this session" : null;
		return new View.Brief(View.Activity.FURNACE, status,
			"In Keldagrim. Bring coins for the coffer, ice gloves, and a coal bag for steel and up.");
	}

	/**
	 * @param choice the bar chosen in the settings, or null to work it out from what is carried
	 */
	Flight fly(Client client, SceneIndex scene, Bar choice, int cofferLow, long now)
	{
		session.tick(client, now);
		Pack pack = Pack.read(client);
		Bar bar = choice != null ? choice : detect(client, pack);
		last = bar;

		boolean wearsCape = pack.wears(ItemIds.SMITHING_CAPE, ItemIds.SMITHING_CAPE_TRIMMED);
		coalBag.setCapacity(wearsCape ? 36 : 27);
		coalBag.tick(pack.count(Bar.COAL));

		FurnaceCycle.Snapshot s = new FurnaceCycle.Snapshot();
		s.bar = bar;
		s.furnaceCoal = client.getVarbitValue(Bar.COAL_VARBIT);
		s.furnaceOre = client.getVarbitValue(bar.getOreVarbit());
		s.dispenserBars = client.getVarbitValue(bar.getBarVarbit());
		s.dispenserState = client.getVarbitValue(VarbitID.BLAST_FURNACE_BARS_HOT);
		s.ore = pack.count(bar.getOre());
		s.coal = pack.count(Bar.COAL);
		s.bars = 0;
		for (Bar b : Bar.values())
		{
			s.bars += pack.count(b.getBar());
		}
		s.free = pack.free();
		s.fixed = 28 - s.free - pack.slots(bar.getOre(), Bar.COAL) - barSlots(pack);
		s.coalBag = pack.carries(ItemIds.COAL_BAG, ItemIds.COAL_BAG_OPEN);
		s.bagCoal = coalBag.coal();
		s.bagCapacity = coalBag.capacity();
		s.bankOpen = bankOpen(client);
		s.iceGlovesWorn = pack.wears(ItemIds.ICE_GLOVES, ItemIds.SMITHING_UNIFORM_GLOVES_ICE);
		s.iceGlovesCarried = pack.carries(ItemIds.ICE_GLOVES, ItemIds.SMITHING_UNIFORM_GLOVES_ICE);
		s.goldsmithWorn = pack.wears(ItemIds.GOLDSMITH_GAUNTLETS) || wearsCape;
		s.goldsmithCarried = pack.carries(ItemIds.GOLDSMITH_GAUNTLETS);
		s.coffer = client.getVarbitValue(VarbitID.BLAST_FURNACE_COFFER);
		s.cofferLow = cofferLow;
		s.bankOre = pack.banked(bar.getOre());
		s.bankCoal = pack.banked(Bar.COAL);

		TileObject belt = first(scene.get(ObjectID.BLAST_FURNACE_CONVEYER_BELT_CLICKABLE));
		boolean atBelt = belt != null && client.getLocalPlayer().getWorldLocation().distanceTo(belt.getWorldLocation()) <= 3;
		int carried = s.ore + s.coal;
		if (atBelt && lastOre > 0 && s.ore == 0)
		{
			loads++;
		}
		lastOre = s.ore;
		cycle.observe(carried, s.furnaceOre + s.furnaceCoal, atBelt);
		int barsNow = s.bars;
		if (lastBars >= 0 && barsNow > lastBars && !s.bankOpen)
		{
			barsTaken += barsNow - lastBars;
		}
		lastBars = barsNow;

		Step step = cycle.next(s);
		Ground ground = Ground.read(client, null, 0);
		Ground.Way<TileObject> way = null;
		if (ground != null)
		{
			switch (step.getTarget())
			{
				case BELT:
					way = ground.to(belt);
					break;
				case DISPENSER:
					way = ground.nearest(scene.get(ObjectID.BLAST_FURNACE_DISPENSER), null);
					break;
				case COFFER:
					way = ground.nearest(scene.get(COFFERS), null);
					break;
				case BANK:
					way = ground.nearest(scene.banks(), null);
					break;
				default:
					break;
			}
		}

		View view = new View(true, View.Activity.FURNACE, step, bar.getName() + " bars", LOOP, stage(step, s));
		int need = bar.getCoal() * FurnaceCycle.load(s);
		if (bar.getCoal() > 0)
		{
			view.gauge(new View.Gauge("Coal in the furnace", s.furnaceCoal, 0, Math.max(need, s.furnaceCoal),
				s.furnaceCoal >= need ? "Enough for a full load of ore" : (need - s.furnaceCoal) + " short of a full load of ore",
				s.furnaceCoal >= need ? Theme.GREEN : Theme.EMBER));
		}
		view.gauge(new View.Gauge(bar.getName() + " ore in the furnace", s.furnaceOre, cycle.getOnBelt(), Math.max(FurnaceCycle.load(s), s.furnaceOre), "", Theme.GOLD));
		view.gauge(new View.Gauge("Bars in the dispenser", s.dispenserBars, 0, 28,
			s.dispenserState == 2 ? "Hot: ice gloves to take" : s.dispenserState == 3 ? "Cooled" : "", Theme.TEAL));
		if (s.coalBag && bar.getCoal() > 0)
		{
			int bag = Math.max(0, s.bagCoal);
			view.gauge(new View.Gauge("Coal bag", bag, 0, s.bagCapacity, s.bagCoal < 0 ? "Not checked yet" : "", Theme.GOLD_DIM));
		}
		int minutes = (int) (s.coffer * 60L / COFFER_PER_HOUR);
		view.gauge(new View.Gauge("Coffer", s.coffer, 0, Math.max(s.coffer, COFFER_PER_HOUR / 2),
			minutes >= 60 ? (minutes / 60) + "h " + (minutes % 60) + "m of smelting" : minutes + " minutes of smelting",
			s.coffer < cofferLow ? Theme.BLOOD : Theme.GOLD));

		view.stat(new View.Stat(Session.format(barsTaken), "Bars", session.perHour(barsTaken, now), Theme.GOLD));
		view.stat(new View.Stat(Session.format(loads), "Ore loads", FurnaceCycle.coalRunNext(s) ? "Next: coal" : "Next: ore", Theme.TEAL));
		int xp = session.xp(client);
		view.stat(new View.Stat(Session.format(xp), "Smithing xp", session.perHour(xp, now), Theme.GREEN));
		view.stat(new View.Stat(session.elapsed(now), "Time", "At the furnace", Theme.TEXT_MUTED));
		return new Flight(View.Activity.FURNACE, step, view).to(way);
	}

	/** The bar being made: ore carried first, then ore in the furnace, then the last one seen. */
	private Bar detect(Client client, Pack pack)
	{
		for (Bar b : BY_VALUE)
		{
			int ore = b == Bar.STEEL ? Bar.IRON.getOre() : b.getOre();
			if (pack.carries(ore))
			{
				return ironOrSteel(b, client, pack);
			}
		}
		for (Bar b : BY_VALUE)
		{
			if (client.getVarbitValue(b.getOreVarbit()) > 0)
			{
				return ironOrSteel(b, client, pack);
			}
		}
		if (last != null)
		{
			return last;
		}
		Bar most = Bar.STEEL;
		int best = 0;
		for (Bar b : BY_VALUE)
		{
			int n = pack.banked(b.getOre());
			if (n > best)
			{
				best = n;
				most = b;
			}
		}
		return ironOrSteel(most, client, pack);
	}

	/** Iron ore makes steel when there is coal to go with it, and iron when there is none. */
	private static Bar ironOrSteel(Bar b, Client client, Pack pack)
	{
		if (b != Bar.STEEL)
		{
			return b;
		}
		boolean coal = client.getVarbitValue(Bar.COAL_VARBIT) > 0 || pack.carries(Bar.COAL)
			|| pack.banked(Bar.COAL) > 0 || pack.carries(ItemIds.COAL_BAG, ItemIds.COAL_BAG_OPEN);
		return coal ? Bar.STEEL : Bar.IRON;
	}

	private static int barSlots(Pack pack)
	{
		int n = 0;
		for (Bar b : Bar.values())
		{
			n += pack.slots(b.getBar());
		}
		return n;
	}

	private static int stage(Step step, FurnaceCycle.Snapshot s)
	{
		switch (step.getTarget())
		{
			case BANK:
				return 0;
			case BELT:
				return 1;
			case DISPENSER:
				return 2;
			case ITEM:
				return s.bankOpen ? 0 : s.ore + s.coal > 0 ? 1 : 2;
			default:
				return 0;
		}
	}

	public static boolean bankOpen(Client client)
	{
		net.runelite.api.widgets.Widget items = client.getWidget(InterfaceID.Bankmain.ITEMS);
		return items != null && !items.isHidden();
	}

	private static TileObject first(Collection<TileObject> objects)
	{
		return objects.isEmpty() ? null : objects.iterator().next();
	}
}
