package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.core.Wintertodt;
import com.codex.minigamepilot.core.Wintertodt.Piece;
import com.codex.minigamepilot.core.Wintertodt.Reward;
import com.codex.minigamepilot.core.WintertodtCycle;
import com.codex.minigamepilot.core.WintertodtCycle.Brazier;
import com.codex.minigamepilot.core.WintertodtLog;
import com.codex.minigamepilot.core.WintertodtRounds;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;

/**
 * Flies the Wintertodt: the one next step through a round (chop, fletch, feed; light, fix and
 * heal when your brazier needs it; warm up before the cold takes you), every round kept per
 * account with its points, time and rewards, and your chance at the reward you are after at your
 * own pace.
 *
 * <p>It reads the game only: the HUD's energy, warmth, points and countdown, the warmth varbit,
 * the braziers and pyromancers around you, what you carry and wear. RuneLite's own Wintertodt
 * plugin already notifies on interruptions and the round's start; nothing here repeats those.
 * Its knocks (as that plugin reads them in the chat) are only counted into the round's line.
 */
public final class WintertodtPilot
{
	static final String LOG_KEY = "wintertodt.kept";
	static final String OWNED_KEY = "wintertodt.owned";
	/** Your brazier: the nearest within this many tiles. */
	static final int MY_BRAZIER = 12;
	/** A pyromancer this close to your brazier is its own. */
	static final int PYRO_BY = 4;
	/** The reward cart this close: at the cart. */
	static final int AT_CART = 20;

	/**
	 * Knocks as RuneLite's Wintertodt plugin reads them in the chat (a cross-check: the wiki
	 * quotes none of these lines): the cold, snowfall, and a brazier shattering beside you.
	 */
	static final String[] KNOCKS = {"The cold of", "The freezing cold attack", "The brazier is broken and shrapnel"};
	/** On entering without enough warm clothing (OSRS wiki, Wintertodt). */
	static final String COLD_WIND = "You feel the cold wind strike you as you enter";
	/** A reward from the cart, as RuneLite's loot tracker reads it in the Wintertodt Camp (a cross-check). */
	static final String LOOT = "You found some loot:";

	private final Session session = new Session(Skill.FIREMAKING);
	private final WintertodtRounds rounds = new WintertodtRounds();
	private Function<String, String> load;
	private BiConsumer<String, String> save;
	private WintertodtLog log;
	private String owned;
	private boolean fletch = true;
	private int eatAt = 40;
	private Reward goal = Reward.PYROMANCER_OUTFIT;

	// Read each tick.
	private boolean inPrison;
	private int energy = -1;
	private int returnsIn = -1;
	private int points = -1;
	private int warmth = -1;

	// This session.
	private int roundsBase;
	private int sessionPoints;
	private double sessionRewards;
	/** Sure rewards earned since the cart was last searched. */
	private int owed;
	private int lastPoints;
	private boolean coldWind;

	Object lastState;

	void setStore(Function<String, String> load, BiConsumer<String, String> save)
	{
		this.load = load;
		this.save = save;
	}

	/** @param reward the reward saved for, or null for none */
	void settings(boolean fletch, int eatAt, Reward reward)
	{
		this.fletch = fletch;
		this.eatAt = Math.max(0, Math.min(100, eatAt));
		this.goal = reward;
	}

	void newAccount()
	{
		session.startOver();
		log = null;
		owned = null;
		roundsBase = rounds.rounds();
		sessionPoints = 0;
		sessionRewards = 0;
		owed = 0;
		lastPoints = 0;
		coldWind = false;
	}

	WintertodtLog log()
	{
		if (log == null)
		{
			log = WintertodtLog.decode(load == null ? null : load.apply(LOG_KEY));
		}
		return log;
	}

	static boolean isWatched(int id)
	{
		return id == Wintertodt.BRAZIER_UNLIT || id == Wintertodt.BRAZIER_BROKEN || id == Wintertodt.BRAZIER_LIT || id == Wintertodt.ROOTS
			|| id == Wintertodt.CRATE_AXE || id == Wintertodt.CRATE_KNIFE || id == Wintertodt.CRATE_HAMMER || id == Wintertodt.CRATE_TINDERBOX
			|| StormPilot.contains(Wintertodt.CART, id);
	}

	/** A game message, tags removed. */
	void chat(String message)
	{
		if (message == null)
		{
			return;
		}
		for (String k : KNOCKS)
		{
			if (message.startsWith(k))
			{
				rounds.hit();
				return;
			}
		}
		if (message.startsWith(COLD_WIND))
		{
			coldWind = true;
		}
		else if (message.startsWith(LOOT) && owed > 0)
		{
			owed--;
		}
	}

	/** Every tick, in the prison or not: the HUD, so a round is kept even while another pilot flies. */
	void watch(Client client, long now)
	{
		int tick = client.getTickCount();
		inPrison = client.getLocalPlayer() != null && client.getLocalPlayer().getWorldLocation() != null
			&& client.getLocalPlayer().getWorldLocation().getRegionID() == Wintertodt.PRISON_REGION;
		String title = text(client, Wintertodt.HUD_ENERGY);
		energy = Wintertodt.energy(title);
		returnsIn = Wintertodt.returnsIn(title);
		points = Wintertodt.points(text(client, Wintertodt.HUD_POINTS));
		warmth = inPrison ? Wintertodt.warmth(text(client, Wintertodt.HUD_WARMTH)) : -1;
		if (inPrison && warmth < 0 && energy >= 0)
		{
			// The HUD's title not read: the varbit, where 0 is taken as unread (at 0% you are dead, not playing).
			int v = client.getVarbitValue(Wintertodt.WARMTH_VARBIT);
			warmth = v > 0 ? Wintertodt.warmthOf(v) : -1;
		}
		keep(rounds.tick(tick, inPrison, inPrison ? energy : -1, inPrison ? returnsIn : -1, inPrison ? points : -1, now));
	}

	private void keep(WintertodtLog.Game g)
	{
		if (g == null)
		{
			return;
		}
		log().add(g);
		if (save != null)
		{
			save.accept(LOG_KEY, log().encode());
		}
		sessionPoints += g.points;
		sessionRewards += g.rolls();
		owed += Wintertodt.sureRolls(g.points);
		lastPoints = g.points;
	}

	/** Logged out or hopped: a round under way is lost. */
	void relogged(long now, int tick)
	{
		keep(rounds.logout(tick, now));
		inPrison = false;
	}

	private static String text(Client client, int component)
	{
		Widget w = client.getWidget(component);
		return w == null || w.isHidden() ? null : w.getText();
	}

	/** In the prison, or at the reward cart with rewards to collect. */
	boolean active(Client client, SceneIndex scene)
	{
		if (client.getLocalPlayer() == null)
		{
			return false;
		}
		if (inPrison)
		{
			return true;
		}
		return owed > 0 && cart(scene, client.getLocalPlayer().getWorldLocation()) != null;
	}

	/** The nearest reward cart within reach, or null. */
	private static TileObject cart(SceneIndex scene, WorldPoint at)
	{
		return closest(scene.get(Wintertodt.CART), at, AT_CART);
	}

	/** The pace coach's count: none, as a round is no loop to hurry. */
	int units()
	{
		return 0;
	}

	void left()
	{
	}

	Flight fly(Client client, SceneIndex scene, Collection<NPC> npcs, long now)
	{
		session.tick(client, now);
		int tick = client.getTickCount();
		Pack pack = Pack.read(client);
		learnOwned(pack);
		WorldPoint at = client.getLocalPlayer().getWorldLocation();

		// Braziers by what they show now: yours is the nearest.
		List<TileObject> lit = new ArrayList<>();
		TileObject mine = null;
		Brazier mineState = Brazier.NONE;
		for (TileObject o : scene.get(Wintertodt.BRAZIER_UNLIT, Wintertodt.BRAZIER_BROKEN, Wintertodt.BRAZIER_LIT))
		{
			Brazier b = state(client, o.getId());
			if (b == Brazier.LIT)
			{
				lit.add(o);
			}
			WorldPoint p = o.getWorldLocation();
			if (p != null && at != null && p.getPlane() == at.getPlane() && p.distanceTo2D(at) <= MY_BRAZIER
				&& (mine == null || p.distanceTo2D(at) < mine.getWorldLocation().distanceTo2D(at)))
			{
				mine = o;
				mineState = b;
			}
		}
		NPC down = null;
		if (mine != null)
		{
			for (NPC n : npcs)
			{
				if (n.getId() == Wintertodt.PYROMANCER_DOWN && n.getWorldLocation() != null
					&& n.getWorldLocation().distanceTo2D(mine.getWorldLocation()) <= PYRO_BY)
				{
					down = n;
				}
			}
		}

		WintertodtCycle.Snapshot s = new WintertodtCycle.Snapshot();
		lastState = s;
		s.inPrison = inPrison;
		s.round = energy >= 0;
		s.returnsIn = returnsIn;
		s.energy = energy;
		s.warmth = warmth;
		s.points = s.round ? points : -1;
		s.firemaking = client.getRealSkillLevel(Skill.FIREMAKING);
		s.roots = pack.count(Wintertodt.ROOT);
		s.kindling = pack.count(Wintertodt.KINDLING);
		s.free = pack.free();
		s.axe = hasAxe(client);
		s.knife = pack.carries(Wintertodt.KNIVES);
		s.lighter = pack.carries(Wintertodt.LIGHTERS) || pack.wears(Wintertodt.LIGHTERS);
		s.hammer = pack.carries(Wintertodt.HAMMERS) || pack.wears(Wintertodt.HAMMERS);
		s.doses = doses(pack);
		s.brazier = mineState;
		s.pyroDown = down != null;
		s.anyLit = !lit.isEmpty();
		s.fletch = fletch;
		s.eatAt = eatAt;
		s.cartInSight = cart(scene, at) != null;
		s.owed = owed;
		s.lastPoints = lastPoints;

		Step step = WintertodtCycle.next(s);
		if (step == null)
		{
			step = Step.waiting("Nothing owed at the cart", "Rounds of 500 points or more fill it", Step.Target.NONE);
		}
		int stage = WintertodtCycle.stage(step);
		View view = new View(true, View.Activity.WINTERTODT, step, place(s), inPrison ? WintertodtCycle.LOOP : null, stage);
		Flight out = new Flight(View.Activity.WINTERTODT, step, view);
		target(client, out, step, scene, mine, mineState, lit, down, at);
		dress(view, s, pack, client, tick, now);
		return out;
	}

	/** A brazier's state from what its object shows now. */
	private static Brazier state(Client client, int id)
	{
		int shown = id;
		ObjectComposition def = client.getObjectDefinition(id);
		if (def != null && def.getImpostorIds() != null)
		{
			ObjectComposition now = def.getImpostor();
			if (now != null)
			{
				shown = now.getId();
			}
		}
		if (shown == Wintertodt.BRAZIER_LIT)
		{
			return Brazier.LIT;
		}
		if (shown == Wintertodt.BRAZIER_BROKEN)
		{
			return Brazier.BROKEN;
		}
		return shown == Wintertodt.BRAZIER_UNLIT ? Brazier.UNLIT : Brazier.NONE;
	}

	/** Doses of rejuvenation potion carried: the (4) counts four. */
	static int doses(Pack pack)
	{
		int n = 0;
		for (int i = 0; i < Wintertodt.POTIONS.length; i++)
		{
			n += pack.count(Wintertodt.POTIONS[i]) * (4 - i);
		}
		return n;
	}

	/** An axe carried or worn, by its name ("Rune axe", "Dragon felling axe"): not a pickaxe or battleaxe. */
	static boolean hasAxe(Client client)
	{
		return axeIn(client, client.getItemContainer(InventoryID.INV)) || axeIn(client, client.getItemContainer(InventoryID.WORN));
	}

	private static boolean axeIn(Client client, ItemContainer c)
	{
		if (c == null)
		{
			return false;
		}
		for (Item i : c.getItems())
		{
			if (i.getId() <= 0)
			{
				continue;
			}
			ItemComposition def = client.getItemDefinition(i.getId());
			if (def != null && isAxe(def.getName()))
			{
				return true;
			}
		}
		return false;
	}

	static boolean isAxe(String name)
	{
		if (name == null)
		{
			return false;
		}
		String n = name.toLowerCase(Locale.ROOT).replaceAll("\\s*\\(.*\\)$", "").trim();
		return (n.endsWith(" axe") || n.equals("axe")) && !n.contains("pickaxe") && !n.contains("battleaxe");
	}

	/** The pieces and rewards carried, worn or in the bank as last seen: once seen, counted as yours. */
	private void learnOwned(Pack pack)
	{
		String o = owned();
		StringBuilder b = new StringBuilder(o);
		for (Piece p : Piece.values())
		{
			add(b, code(p), seen(pack, p.item()) || seen(pack, Wintertodt.PYROMANCER_SET));
		}
		for (Reward r : Reward.values())
		{
			if (r != Reward.PYROMANCER_OUTFIT)
			{
				add(b, code(r), seen(pack, r.items()));
			}
		}
		if (b.length() != o.length())
		{
			owned = b.toString();
			if (save != null)
			{
				save.accept(OWNED_KEY, "1;" + owned);
			}
		}
	}

	private static void add(StringBuilder b, char c, boolean seen)
	{
		if (seen && b.indexOf(String.valueOf(c)) < 0)
		{
			b.append(c);
		}
	}

	private static boolean seen(Pack pack, int... ids)
	{
		return pack.carries(ids) || pack.wears(ids) || pack.banked(ids) > 0;
	}

	private String owned()
	{
		if (owned == null)
		{
			owned = Wintertodt.decodeOwned(load == null ? null : load.apply(OWNED_KEY));
		}
		return owned;
	}

	static char code(Piece p)
	{
		return "GHRB".charAt(p.ordinal());
	}

	static char code(Reward r)
	{
		switch (r)
		{
			case WARM_GLOVES:
				return 'W';
			case BRUMA_TORCH:
				return 'T';
			case TOME_OF_FIRE:
				return 'F';
			case PHOENIX:
				return 'P';
			default:
				return '?';
		}
	}

	boolean owns(Piece p)
	{
		return owned().indexOf(code(p)) >= 0;
	}

	/** Pieces of the outfit not seen yet. */
	int piecesMissing()
	{
		int n = 0;
		for (Piece p : Piece.values())
		{
			n += owns(p) ? 0 : 1;
		}
		return n;
	}

	boolean owns(Reward r)
	{
		return r == Reward.PYROMANCER_OUTFIT ? piecesMissing() == 0 : owned().indexOf(code(r)) >= 0;
	}

	/** Lights the step's thing. */
	private void target(Client client, Flight out, Step step, SceneIndex scene, TileObject mine, Brazier mineState, List<TileObject> lit,
		NPC down, WorldPoint at)
	{
		Collection<? extends TileObject> things = null;
		switch (step.getTarget())
		{
			case BRAZIER:
				if ("Feed".equals(step.getVerb()) && mineState != Brazier.LIT)
				{
					things = lit;
				}
				else if (mine != null)
				{
					things = Collections.singletonList(mine);
				}
				break;
			case PYROMANCER:
				out.npc = down;
				return;
			case ROOTS:
				things = scene.get(Wintertodt.ROOTS);
				break;
			case TOOL_CRATE:
				things = scene.get(crate(step));
				break;
			case REWARD_CART:
				things = scene.get(Wintertodt.CART);
				break;
			default:
				return;
		}
		if (things == null || things.isEmpty())
		{
			return;
		}
		Ground ground = step.isWaiting() ? null : Ground.read(client, Collections.emptyList(), 0);
		Ground.Way<TileObject> way = ground == null ? null : ground.nearest(things, null);
		if (way != null)
		{
			out.to(way);
			return;
		}
		out.object = closest(things, at, Integer.MAX_VALUE);
	}

	private static int crate(Step step)
	{
		String t = step.getTitle();
		if (t.contains("tinderbox"))
		{
			return Wintertodt.CRATE_TINDERBOX;
		}
		if (t.contains("knife"))
		{
			return Wintertodt.CRATE_KNIFE;
		}
		if (t.contains("hammer"))
		{
			return Wintertodt.CRATE_HAMMER;
		}
		return Wintertodt.CRATE_AXE;
	}

	private static TileObject closest(Collection<? extends TileObject> things, WorldPoint at, int within)
	{
		TileObject best = null;
		int bestD = Integer.MAX_VALUE;
		for (TileObject o : things)
		{
			WorldPoint p = o.getWorldLocation();
			if (p == null || at == null || p.getPlane() != at.getPlane())
			{
				continue;
			}
			int d = p.distanceTo2D(at);
			if (d <= within && d < bestD)
			{
				bestD = d;
				best = o;
			}
		}
		return best;
	}

	private static String place(WintertodtCycle.Snapshot s)
	{
		if (!s.inPrison)
		{
			return "Wintertodt Camp";
		}
		if (!s.round)
		{
			return "Wintertodt · between rounds";
		}
		return s.energy <= 20 ? "Wintertodt · the round is ending" : "Wintertodt";
	}

	/** The round's gauges and what you hold, the session, the reward you are after, and your rounds. */
	private void dress(View view, WintertodtCycle.Snapshot s, Pack pack, Client client, int tick, long now)
	{
		if (s.inPrison && s.round)
		{
			int items = s.roots + s.kindling;
			view.gauge(new View.Gauge("Energy", s.energy, 0, 100,
				s.energy == 0 ? "Subdued" : items > 0 ? "With " + items + " held, burn them from " + (items + Wintertodt.FEED_MARGIN) + "%" : "Each brazier fed drains it",
				Theme.GOLD));
			if (s.warmth >= 0)
			{
				boolean low = s.eatAt > 0 && s.warmth <= s.eatAt;
				view.gauge(new View.Gauge("Warmth", s.warmth, 0, 100, low ? "Warm up now: at 0% you die" : coldWind ? "Fewer than four warm items worn" : "Food 4+ gives 35%, a dose 30%",
					low ? Theme.BLOOD : Theme.EMBER));
			}
			if (s.points >= 0)
			{
				int next = s.points + Wintertodt.toNextRoll(s.points);
				int held = WintertodtCycle.held(s);
				view.gauge(new View.Gauge("Points", s.points, held, next, pointsNote(s.points), s.points >= Wintertodt.MIN_POINTS ? Theme.GOLD_BRIGHT : Theme.TEAL));
			}
			view.lines("Brazier and pack", roundNote(tick), roundLines(s));
		}
		else if (s.inPrison)
		{
			view.lines("Ready for the round", s.returnsIn >= 0 ? "Back in " + s.returnsIn / 60 + ":" + String.format(Locale.ROOT, "%02d", s.returnsIn % 60) : null, toolLines(s));
		}

		if (s.inPrison && s.points >= 0 && s.round)
		{
			int extra = (int) Math.round(Wintertodt.extraChance(s.points) * 100);
			view.stat(new View.Stat(Integer.toString(Wintertodt.sureRolls(s.points)), "Rewards",
				s.points < Wintertodt.MIN_POINTS ? "From 500 points" : extra > 0 ? "+" + extra + "% of one more" : "Sure", Theme.GOLD_BRIGHT));
		}
		int done = rounds.rounds() - roundsBase;
		view.stat(new View.Stat(Integer.toString(done), "Rounds", sessionPoints > 0 ? Session.format(sessionPoints) + " points" : "", Theme.GOLD));
		view.stat(new View.Stat(String.format(Locale.ROOT, "%.1f", sessionRewards), "Rewards earned", session.perHour(Math.round(sessionRewards), now), Theme.TEAL));
		WintertodtLog g = log();
		double pph = g.pointsPerHour();
		if (pph >= 0)
		{
			view.stat(new View.Stat(Session.format(Math.round(pph)), "Points an hour", "Your pace, waits in", Theme.GOLD));
			view.stat(new View.Stat(String.format(Locale.ROOT, "%.1f", g.rewardsPerHour()), "Rewards an hour", "Over " + g.count() + (g.count() == 1 ? " round" : " rounds"), Theme.GREEN));
		}
		int xp = session.xp(client);
		view.stat(new View.Stat(Session.format(xp), "Firemaking xp", session.perHour(xp, now), Theme.EMBER));

		if (goal != null)
		{
			view.goal(goalCard(g, client.getSkillExperience(Skill.FIREMAKING) >= Wintertodt.MAX_XP));
		}
		if (g.count() > 0)
		{
			view.supply(historyCard(g));
		}
	}

	static String pointsNote(int points)
	{
		if (points < Wintertodt.MIN_POINTS)
		{
			return (Wintertodt.MIN_POINTS - points) + " to 500: no reward below it";
		}
		int sure = Wintertodt.sureRolls(points);
		return sure + " rewards · " + Wintertodt.toNextRoll(points) + " to the next";
	}

	private String roundNote(int tick)
	{
		String time = rounds.open() && !rounds.late() ? StormPilot.clock(rounds.ticks(tick)) : rounds.open() ? "Joined late" : "";
		int h = rounds.hits();
		return time + (h > 0 ? (time.isEmpty() ? "" : " · ") + h + (h == 1 ? " knock" : " knocks") : "");
	}

	private List<View.Line> roundLines(WintertodtCycle.Snapshot s)
	{
		List<View.Line> lines = new ArrayList<>();
		lines.add(new View.Line("Your brazier", s.brazier == Brazier.LIT ? "Burning" : s.brazier == Brazier.BROKEN ? "Broken: fix it" : s.brazier == Brazier.UNLIT ? "Unlit" : "None near",
			s.brazier == Brazier.LIT ? View.Line.Mark.DONE : s.brazier == Brazier.NONE ? View.Line.Mark.IDLE : View.Line.Mark.NEXT));
		if (s.pyroDown)
		{
			lines.add(new View.Line("Its Pyromancer is down", s.doses > 0 ? "Help: +75" : "Needs a rejuvenation dose", View.Line.Mark.NEXT));
		}
		if (s.roots > 0)
		{
			lines.add(new View.Line(s.roots + (s.roots == 1 ? " root" : " roots"), "+" + s.roots * Wintertodt.ROOT_POINTS + " fed" + (s.fletch && s.knife ? ", +" + s.roots * Wintertodt.KINDLING_POINTS + " as kindling" : ""),
				View.Line.Mark.TODO));
		}
		if (s.kindling > 0)
		{
			lines.add(new View.Line(s.kindling + " kindling", "+" + s.kindling * Wintertodt.KINDLING_POINTS + " fed", View.Line.Mark.TODO));
		}
		if (coldWind)
		{
			lines.add(new View.Line("Warm clothing", "Under four pieces", View.Line.Mark.TODO));
		}
		return lines;
	}

	private List<View.Line> toolLines(WintertodtCycle.Snapshot s)
	{
		List<View.Line> lines = new ArrayList<>();
		lines.add(tool("Tinderbox or bruma torch", s.lighter, true));
		lines.add(tool("Axe", s.axe, true));
		lines.add(tool("Knife", s.knife, s.fletch));
		lines.add(tool("Hammer", s.hammer, true));
		lines.add(new View.Line("Rejuvenation", s.doses > 0 ? s.doses + (s.doses == 1 ? " dose" : " doses") : "None: food warms too", s.doses > 0 ? View.Line.Mark.DONE : View.Line.Mark.IDLE));
		if (coldWind)
		{
			lines.add(new View.Line("Warm clothing", "Under four pieces", View.Line.Mark.TODO));
		}
		return lines;
	}

	private static View.Line tool(String name, boolean have, boolean wanted)
	{
		return new View.Line(name, have ? "Carried" : wanted ? "Free from the crate" : "Not fletching", have ? View.Line.Mark.DONE : wanted ? View.Line.Mark.NEXT : View.Line.Mark.IDLE);
	}

	/** The reward you are after: what you own of it, its chance a reward, and the rounds and time to it at your pace. */
	View.GoalCard goalCard(WintertodtLog g, boolean maxed)
	{
		Wintertodt.Unique u = goal.unique();
		double p = u.perReward(maxed);
		int oneIn = (int) Math.round(1 / p);
		List<View.Line> pieces = new ArrayList<>();
		boolean done = owns(goal);
		int need;
		int have;
		if (goal == Reward.PYROMANCER_OUTFIT)
		{
			for (Piece piece : Piece.values())
			{
				pieces.add(new View.Line(piece.getTitle(), owns(piece) ? "Yours" : "1 in " + oneIn + " a reward", owns(piece) ? View.Line.Mark.DONE : View.Line.Mark.TODO));
			}
			need = Piece.values().length;
			have = need - piecesMissing();
		}
		else
		{
			need = 1;
			have = done ? 1 : 0;
		}
		String note;
		if (done)
		{
			note = "You have it: choose another under Wintertodt reward.";
		}
		else
		{
			int lacking = need - have;
			double rewards = Wintertodt.expectedRewards(p, lacking);
			double perRound = g.rewardsPerRound();
			StringBuilder b = new StringBuilder();
			b.append("1 in ").append(oneIn).append(" a reward");
			if (goal == Reward.PYROMANCER_OUTFIT)
			{
				b.append(", always the piece you have least of");
			}
			b.append(". About ").append(Session.format(Math.round(rewards))).append(lacking > 1 ? " rewards for the " + lacking + " you lack" : " rewards on average");
			if (perRound > 0)
			{
				long roundsTo = Math.round(Math.ceil(rewards / perRound));
				b.append(": ").append(Session.format(roundsTo)).append(roundsTo == 1 ? " round" : " rounds");
				int typical = g.typicalRound();
				if (typical > 0)
				{
					b.append(", ").append(MixologyPilot.clockHours(roundsTo * typical * 0.6 / 3600)).append(" at your pace");
				}
			}
			b.append('.');
			if (lacking == 1)
			{
				b.append(" Even odds by ").append(Session.format(Math.round(Wintertodt.evenOdds(p)))).append(" rewards; luck, not a promise.");
			}
			note = b.toString();
			double kept = g.rewards();
			if (kept >= 1)
			{
				String by = lacking == 1
					? Math.round(Wintertodt.chanceIn(p, kept) * 100) + "% chance of it by now"
					: String.format(Locale.ROOT, "%.1f pieces expected by now", kept * p);
				pieces.add(new View.Line("Your " + Session.format(Math.round(kept)) + " rewards kept", by, View.Line.Mark.IDLE));
			}
		}
		View.GoalCard card = new View.GoalCard(View.Activity.WINTERTODT, goal.getTitle(), have, need, goal == Reward.PYROMANCER_OUTFIT ? "pieces" : "", note, pieces, done);
		card.aside("Rewards come at 500 points, one more for each 500 after");
		return card;
	}

	/** Your last rounds, the best, and points and rewards an hour across them. */
	private static View.Supply historyCard(WintertodtLog g)
	{
		List<View.Line> lines = new ArrayList<>();
		List<WintertodtLog.Game> all = g.all();
		WintertodtLog.Game best = g.best();
		for (int i = all.size() - 1; i >= 0 && i >= all.size() - 5; i--)
		{
			WintertodtLog.Game x = all.get(i);
			String what = x.subdued ? Session.format(x.points) + " pts" + (x.ticks > 0 ? " · " + StormPilot.clock(x.ticks) : "") : "Left: points lost";
			String trailing = x.subdued ? rewardsText(x.points) : "";
			lines.add(new View.Line(what, trailing, x == best ? View.Line.Mark.DONE : x.subdued && x.points >= Wintertodt.MIN_POINTS ? View.Line.Mark.IDLE : View.Line.Mark.TODO));
		}
		String headline = best == null ? null : "Best round: " + Session.format(best.points) + " points";
		double h = g.pointsPerHour();
		String detail = h < 0 ? null : Session.format(Math.round(h)) + " points and " + String.format(Locale.ROOT, "%.1f", g.rewardsPerHour())
			+ " rewards an hour over your timed rounds, the minute's wait between them in";
		return new View.Supply(g.count() == 1 ? "1 kept" : g.count() + " kept", lines, headline, detail, false, null).titled("Your rounds");
	}

	/** "3 rewards + 48%", "No reward". */
	static String rewardsText(int points)
	{
		int sure = Wintertodt.sureRolls(points);
		if (sure == 0)
		{
			return "No reward";
		}
		int extra = (int) Math.round(Wintertodt.extraChance(points) * 100);
		return sure + (sure == 1 ? " reward" : " rewards") + (extra > 0 ? " +" + extra + "%" : "");
	}

	/** Brief for the panel away from the Wintertodt. */
	View.Brief brief()
	{
		WintertodtLog g = log();
		String status = null;
		WintertodtLog.Game last = g.last();
		if (last != null)
		{
			status = last.subdued ? "Last round: " + Session.format(last.points) + " points, " + rewardsText(last.points).toLowerCase(Locale.ROOT)
				+ (last.ticks > 0 ? " in " + StormPilot.clock(last.ticks) : "") : "Last round: left before its end";
		}
		View.Brief b = new View.Brief(View.Activity.WINTERTODT, status,
			"The prison north of the Wintertodt Camp, at 50 Firemaking. Bring an axe, knife, tinderbox and hammer, or take them from the crates; 500 points for a reward"
				+ (goal == null ? "." : ", toward the " + goal.getTitle().toLowerCase(Locale.ROOT) + "."));
		double h = g.pointsPerHour();
		if (h >= 0)
		{
			b.best(Session.format(Math.round(h)) + " points and " + String.format(Locale.ROOT, "%.1f", g.rewardsPerHour()) + " rewards an hour over "
				+ g.count() + (g.count() == 1 ? " round." : " rounds."));
		}
		return b;
	}
}
