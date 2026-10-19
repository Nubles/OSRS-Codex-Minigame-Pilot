package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.BankStock;
import com.codex.minigamepilot.core.Bar;
import com.codex.minigamepilot.core.BarPick;
import com.codex.minigamepilot.core.FurnaceCycle;
import com.codex.minigamepilot.core.ItemIds;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.core.Supplies;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.IntUnaryOperator;
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
	/** Bars taken before this session began, left out of its count. */
	private int barsBefore;
	/** The last snapshot planned from, for the field notes. */
	Object lastState;
	private int loads;
	private int lastOre = -1;
	/** This session's experience as last shown, for its summary when you leave. */
	private int xpSeen;

	/** A running count of what rounds produce, for the pace coach. */
	int units()
	{
		return barsTaken;
	}

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

	/** What this session came to, for when you leave the furnace, or null when it was too short or idle. */
	com.codex.minigamepilot.core.Visit visit(long now)
	{
		if (!session.begun())
		{
			return null;
		}
		java.util.List<String> parts = new java.util.ArrayList<>();
		int bars = barsTaken - barsBefore;
		if (bars > 0)
		{
			parts.add(Session.format(bars) + (bars == 1 ? " bar" : " bars"));
		}
		if (xpSeen > 0)
		{
			parts.add(Session.format(xpSeen) + " Smithing xp");
		}
		return com.codex.minigamepilot.core.Visit.of(now, session.activeMs(), parts);
	}

	/** @param lastVisit the summary saved when you last left the furnace, or null */
	/** Logged in again or hopped: readings start afresh, ore on the belt stays on the belt. */
	/** Another account logged in: the session and its counts start over on its first tick. */
	void newAccount()
	{
		session.startOver();
	}

	void relogged()
	{
		cycle.relog();
		lastBars = -1;
		lastOre = -1;
	}

	/** @param best your fastest visit, or null */
	View.Brief brief(com.codex.minigamepilot.core.Visit lastVisit, com.codex.minigamepilot.core.BestVisit best, long now)
	{
		String status = lastVisit != null ? lastVisit.status(now)
			: session.begun() ? Session.format(barsTaken - barsBefore) + " bars this session" : null;
		return new View.Brief(View.Activity.FURNACE, status,
			"In Keldagrim. Bring coins for the coffer, ice gloves, and a coal bag for steel and up.")
			.best(best == null ? null : best.status(UNIT, "Smithing", lastVisit, now));
	}

	/** What the furnace's pace is counted in. */
	static final String UNIT = "bars";

	/** This session's pace so far, as a reading for your best visit, or null while it is too short. */
	com.codex.minigamepilot.core.BestVisit pace(long now)
	{
		return com.codex.minigamepilot.core.BestVisit.of(session.startedAt(), now, session.activeMs(), barsTaken - barsBefore, xpSeen);
	}

	/**
	 * @param choice the bar chosen in the settings, or null to work it out from what is carried
	 */
	Flight fly(Client client, SceneIndex scene, Bar choice, int cofferLow, long now)
	{
		if (session.tick(client, now))
		{
			// A new session after a break: bars from the last one would inflate its hourly rate.
			barsBefore = barsTaken;
		}
		Pack pack = Pack.read(client);
		Bar bar = choice != null ? choice : detect(client, pack);
		last = bar;

		boolean wearsCape = pack.wears(ItemIds.SMITHING_CAPE, ItemIds.SMITHING_CAPE_TRIMMED);
		coalBag.setCapacity(wearsCape ? 36 : 27);
		coalBag.tick(pack.count(Bar.COAL));

		FurnaceCycle.Snapshot s = new FurnaceCycle.Snapshot();
		lastState = s;
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

		view.stat(new View.Stat(Session.format(barsTaken - barsBefore), "Bars", session.perHour(barsTaken - barsBefore, now), Theme.GOLD));
		view.stat(new View.Stat(Session.format(loads), "Ore loads", FurnaceCycle.coalRunNext(s) ? "Next: coal" : "Next: ore", Theme.TEAL));
		int xp = session.xp(client);
		xpSeen = xp;
		view.stat(new View.Stat(Session.format(xp), "Smithing xp", session.perHour(xp, now), Theme.GREEN));
		view.stat(new View.Stat(session.elapsed(now), "Time", "At the furnace", Theme.TEXT_MUTED));
		return new Flight(View.Activity.FURNACE, step, view).to(way);
	}

	/**
	 * What each bar pays at today's prices, for the furnace as you run it: your coal bag, the ore
	 * a trip carries and, once a few trips are timed, your trip time.
	 *
	 * @param tripSeconds your usual trip this session, or 0 before one is timed
	 * @return the card, or null when prices are not loaded
	 */
	View.PickCard pick(Client client, IntUnaryOperator price, double tripSeconds)
	{
		FurnaceCycle.Snapshot s = lastState instanceof FurnaceCycle.Snapshot ? (FurnaceCycle.Snapshot) lastState : null;
		BarPick.Setup setup = new BarPick.Setup();
		setup.level = client.getRealSkillLevel(Skill.SMITHING);
		setup.coalBag = s == null || s.coalBag;
		setup.bagCapacity = s == null ? 27 : s.bagCapacity;
		setup.oreSlots = s == null ? 27 : FurnaceCycle.load(s);
		setup.tripSeconds = tripSeconds;
		setup.goldsmith = s != null && (s.goldsmithWorn || s.goldsmithCarried);
		return card(BarPick.rank(price, setup), last, tripSeconds, setup.level);
	}

	/**
	 * How far the bar's ore and the coal you have go: in the bank (as last seen by the Pilot or
	 * Bank Steward), carried, in the coal bag and waiting in the furnace. Null before any bank is seen.
	 *
	 * @param tripSeconds your usual trip this session, or 0 before one is timed
	 */
	View.Supply supply(BankStock stock, double tripSeconds, long now)
	{
		FurnaceCycle.Snapshot s = lastState instanceof FurnaceCycle.Snapshot ? (FurnaceCycle.Snapshot) lastState : null;
		if (stock == null || s == null)
		{
			return null;
		}
		Bar bar = s.bar;
		long ore = stock.count(bar.getOre()) + s.ore + s.furnaceOre;
		long coal = stock.count(Bar.COAL) + s.coal + Math.max(0, s.bagCoal) + s.furnaceCoal;
		Supplies.Furnace f = Supplies.furnace(bar, ore, coal, FurnaceCycle.load(s), s.coalBag ? s.bagCapacity : 0, tripSeconds);
		List<View.Line> lines = new ArrayList<>();
		String oreName = Character.toUpperCase(bar.oreName().charAt(0)) + bar.oreName().substring(1);
		lines.add(new View.Line(oreName, Session.format(ore), ore > 0 ? View.Line.Mark.TODO : View.Line.Mark.IDLE));
		if (bar.getCoal() > 0)
		{
			lines.add(new View.Line("Coal", Session.format(coal), coal > 0 ? View.Line.Mark.TODO : View.Line.Mark.IDLE));
		}
		String detail = f.limit();
		if (detail != null && tripSeconds <= 0 && f.getBars() > 0)
		{
			detail += ". Time a few trips and this turns into time";
		}
		else if (detail == null && tripSeconds <= 0 && f.getBars() > 0)
		{
			detail = "Time a few trips and this turns into time";
		}
		return new View.Supply(bar.getName(), lines, f.headline(), detail, f.isCoalShort() || f.getBars() == 0,
			HomesPilot.seenNote(stock, now));
	}

	/** The ranking as the panel shows it. */
	static View.PickCard card(List<BarPick.Row> rows, Bar making, double tripSeconds, int level)
	{
		if (rows.isEmpty())
		{
			return null;
		}
		double top = 0;
		for (BarPick.Row r : rows)
		{
			if (r.isUnlocked())
			{
				top = Math.max(top, r.getPerTrip());
			}
		}
		boolean timed = tripSeconds > 0;
		List<View.Pick> picks = new ArrayList<>();
		for (BarPick.Row r : rows)
		{
			String figure;
			String detail;
			if (!r.isUnlocked())
			{
				figure = signed(r.getPerBar());
				detail = "Needs Smithing " + r.getLevel();
			}
			else
			{
				figure = timed ? compact(r.getPerHour()) + "/h" : signed(r.getPerBar());
				String trips = r.getCoalTrips() <= 0 ? "no coal trips" : coalTrips(r.getCoalTrips());
				detail = timed ? signed(r.getPerBar()) + "/bar · " + trips + " · " + com.codex.minigamepilot.core.Figures.brief(Math.round(r.getXpPerHour())) + " xp"
					: "a bar · " + trips + (r.getCoalTrips() > 0 ? " a load" : "");
			}
			double share = top <= 0 ? 0 : Math.max(0, r.getPerTrip() / top);
			picks.add(new View.Pick(r.getBar().getName(), figure, detail, r.isUnlocked(), r.getBar() == making, share,
				timed ? r.getPerHour() < 0 : r.getPerBar() < 0));
		}
		String fee = "the coffer's " + Session.format(BarPick.COFFER_PER_HOUR) + " an hour"
			+ (level < BarPick.FOREMAN_FREE_LEVEL ? " and the foreman's " + Session.format(BarPick.FOREMAN_PER_HOUR) : "");
		String note = timed
			? "Grand Exchange prices, at your usual trip of " + clock(tripSeconds) + ", after " + fee + "."
			: "Grand Exchange prices, after ore and coal. Time a few trips and this turns into coins an hour, after " + fee + ".";
		return new View.PickCard(timed ? "an hour" : "a bar", note, picks);
	}

	private static String signed(long n)
	{
		return (n >= 0 ? "+" : "-") + Session.format(Math.abs(n));
	}

	private static String coalTrips(double trips)
	{
		double rounded = Math.round(trips * 2) / 2.0;
		String n = rounded == Math.floor(rounded) ? String.valueOf((long) rounded) : String.valueOf(rounded);
		return n + (rounded == 1 ? " coal trip" : " coal trips");
	}

	/** Coins with their sign, shortened past ten thousand: "+71K", "-104K", "+9,870". */
	static String compact(long n)
	{
		return com.codex.minigamepilot.core.Figures.signed(n);
	}

	private static String clock(double seconds)
	{
		long s = Math.round(seconds);
		return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60;
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
