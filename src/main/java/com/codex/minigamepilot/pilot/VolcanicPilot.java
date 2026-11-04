package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.core.Volcanic;
import com.codex.minigamepilot.core.VolcanicCycle;
import com.codex.minigamepilot.core.VolcanicGames;
import com.codex.minigamepilot.core.VolcanicLog;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;

/**
 * The Volcanic Mine: every game kept per account (its points from the game's own count, the
 * Mining experience it gave, its length, the players in it and how it ended), your points and
 * experience an hour at your own pace, and the time to the prospector piece, tome or ore packs
 * you are after. At the entrance, one next step: Mining to 50, the reward once you hold its
 * points, numulite for the fee, Petrified Pete.
 *
 * <p>It reads the game only. Inside the mine it says nothing to do and gives no warning timed to
 * the vents or the eruption: the panel shows the game's state as the game itself shows it (time
 * left, stability, the chambers' pressure once checked, the players) as plain figures, and no
 * plaque is drawn in the game view.
 */
public final class VolcanicPilot
{
	static final String LOG_KEY = "volcanic.kept";
	static final String OWNED_KEY = "volcanic.owned";
	static final String POINTS_KEY = "volcanic.points";
	/** The entrance: within this many tiles of the Volcanic Mine on the ground floor. */
	static final int LOBBY_REACH = 15;
	/** Numulite read at the entrance this recently before going in tells whether the fee was paid. */
	static final int PAY_WINDOW = 30;
	/** The letters of the saved list of what you own: the four pieces, permanent access, the canteen, the tome. */
	static final String OWNED_CODES = "HJLBAWT";

	private Function<String, String> load;
	private BiConsumer<String, String> save;
	private VolcanicLog log;
	private VolcanicGames games;
	private String owned;
	private Volcanic.Reward goal = Volcanic.Reward.PROSPECTOR_KIT;

	// Read each tick.
	private boolean inMine;
	private boolean inLobby;
	private int lobbyNumulite = -1;
	private int lobbyNumuliteTick = -1;
	private boolean wasOpen;

	// The shop's balance as last seen: points, and when.
	private int shopPoints = -1;
	private long shopAt;
	private boolean shopLoaded;

	// This session.
	private int gamesBase;
	private long sessionPoints;
	private long sessionXp;

	Object lastState;

	void setStore(Function<String, String> load, BiConsumer<String, String> save)
	{
		this.load = load;
		this.save = save;
	}

	/** @param reward the reward saved for, or null for none */
	void settings(Volcanic.Reward reward)
	{
		this.goal = reward;
	}

	void newAccount()
	{
		log = null;
		games = null;
		owned = null;
		shopLoaded = false;
		shopPoints = -1;
		shopAt = 0;
		gamesBase = 0;
		sessionPoints = 0;
		sessionXp = 0;
		lobbyNumulite = -1;
		lobbyNumuliteTick = -1;
		wasOpen = false;
	}

	VolcanicLog log()
	{
		if (log == null)
		{
			log = VolcanicLog.decode(load == null ? null : load.apply(LOG_KEY));
		}
		return log;
	}

	private VolcanicGames games()
	{
		if (games == null)
		{
			games = new VolcanicGames();
			gamesBase = 0;
		}
		return games;
	}

	static boolean isWatched(int id)
	{
		return id == Volcanic.BANK_CHEST;
	}

	/** Every tick, wherever you are: so a game is kept even as you come out or log out. */
	void watch(Client client, long now)
	{
		int tick = client.getTickCount();
		WorldPoint at = client.getLocalPlayer() == null ? null
			: WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation());
		inMine = at != null && Volcanic.inMine(at.getRegionID());
		inLobby = at != null && !inMine && at.getPlane() == 0 && Math.abs(at.getX() - Volcanic.LOBBY_X) <= LOBBY_REACH
			&& Math.abs(at.getY() - Volcanic.LOBBY_Y) <= LOBBY_REACH;
		VolcanicGames g = games();
		if (!inMine && !inLobby && !g.open() && !g.settling())
		{
			return;
		}
		Pack pack = Pack.read(client);
		int xp = client.getSkillExperience(Skill.MINING);
		readShop(client, now);
		learnOwned(client, pack);
		int numulite = pack.count(Volcanic.NUMULITE);
		keep(g.tick(tick, inMine,
			inMine ? client.getVarbitValue(Volcanic.GAME_POINTS) : -1, xp,
			inMine ? client.getVarbitValue(Volcanic.PLAYERS) : -1,
			inMine ? client.getVarbitValue(Volcanic.STABILITY) : -1,
			inMine ? timeLeft(client) : -1,
			inMine ? client.getVarbitValue(Volcanic.PROGRESS) : -1, now));
		if (g.open() && !wasOpen)
		{
			learnAccess(tick, numulite);
		}
		wasOpen = g.open();
		if (inLobby)
		{
			lobbyNumulite = lobbyNumuliteTick >= 0 && tick - lobbyNumuliteTick <= PAY_WINDOW ? Math.max(lobbyNumulite, numulite) : numulite;
			lobbyNumuliteTick = tick;
		}
	}

	/** Going in: numulite not gone from the pack since the entrance means access paid for good. */
	private void learnAccess(int tick, int numulite)
	{
		if (lobbyNumuliteTick < 0 || tick - lobbyNumuliteTick > PAY_WINDOW || lobbyNumulite < 0)
		{
			return;
		}
		boolean paid = lobbyNumulite - numulite >= Volcanic.ENTRY_FEE;
		String o = owned();
		boolean had = o.indexOf('A') >= 0;
		if (!paid && !had)
		{
			setOwned(o + 'A');
		}
		else if (paid && had)
		{
			setOwned(o.replace("A", ""));
		}
	}

	/** The time left as the game's own overlay shows it, in seconds, or -1. */
	static int timeLeft(Client client)
	{
		Widget w = client.getWidget(Volcanic.HUD_TIME);
		return w == null || w.isHidden() ? -1 : Volcanic.seconds(w.getText());
	}

	/** While Petrified Pete's shop is open, its points line is the balance. */
	private void readShop(Client client, long now)
	{
		Widget w = client.getWidget(Volcanic.SHOP_POINTS);
		if (w == null || w.isHidden())
		{
			return;
		}
		int p = Volcanic.shopPoints(w.getText());
		if (p < 0)
		{
			return;
		}
		loadShop();
		if (p != shopPoints || now - shopAt > 60_000)
		{
			shopPoints = p;
			shopAt = now;
			if (save != null)
			{
				save.accept(POINTS_KEY, "1;" + p + ";" + now);
			}
		}
	}

	private void loadShop()
	{
		if (shopLoaded)
		{
			return;
		}
		shopLoaded = true;
		String s = load == null ? null : load.apply(POINTS_KEY);
		if (s == null)
		{
			return;
		}
		String[] f = s.split(";");
		if (f.length != 3 || !"1".equals(f[0]))
		{
			return;
		}
		try
		{
			int p = Integer.parseInt(f[1].trim());
			long at = Long.parseLong(f[2].trim());
			if (p >= 0 && p <= Volcanic.POINTS_CAP && at >= 0)
			{
				shopPoints = p;
				shopAt = at;
			}
		}
		catch (NumberFormatException e)
		{
			// A damaged save: the balance is read again at the shop.
		}
	}

	/** Points held: the shop's balance as last seen, the games since, and this game's so far. -1 when the shop has not been seen. */
	long held()
	{
		loadShop();
		if (shopPoints < 0)
		{
			return -1;
		}
		return Math.min(Volcanic.POINTS_CAP, shopPoints + log().pointsSince(shopAt) + games().gained());
	}

	private void learnOwned(Client client, Pack pack)
	{
		String o = owned();
		StringBuilder b = new StringBuilder(o);
		for (Volcanic.Piece p : Volcanic.Piece.values())
		{
			int[] ids = p.items();
			add(b, p.code(), pack.carries(ids) || pack.wears(ids) || pack.banked(ids) > 0);
		}
		add(b, 'W', client.getVarbitValue(Volcanic.UNLOCKED_WATER) > 0);
		add(b, 'T', client.getVarbitValue(Volcanic.UNLOCKED_TOME) > 0);
		if (b.length() != o.length())
		{
			setOwned(b.toString());
		}
	}

	private static void add(StringBuilder b, char c, boolean seen)
	{
		if (seen && b.indexOf(String.valueOf(c)) < 0)
		{
			b.append(c);
		}
	}

	private void setOwned(String o)
	{
		owned = o;
		if (save != null)
		{
			save.accept(OWNED_KEY, "1;" + o);
		}
	}

	String owned()
	{
		if (owned == null)
		{
			owned = decodeOwned(load == null ? null : load.apply(OWNED_KEY));
		}
		return owned;
	}

	static String decodeOwned(String text)
	{
		if (text == null || !text.startsWith("1;"))
		{
			return "";
		}
		StringBuilder b = new StringBuilder();
		for (char c : text.substring(2).toCharArray())
		{
			if (OWNED_CODES.indexOf(c) >= 0 && b.indexOf(String.valueOf(c)) < 0)
			{
				b.append(c);
			}
		}
		return b.toString();
	}

	boolean owns(Volcanic.Piece p)
	{
		return owned().indexOf(p.code()) >= 0;
	}

	/** Whether the reward is yours already (never for ore packs). */
	boolean owns(Volcanic.Reward r)
	{
		switch (r)
		{
			case PROSPECTOR_KIT:
				return missingPieces().isEmpty();
			case ASH_COVERED_TOME:
				return owned().indexOf('T') >= 0;
			case LARGE_WATER_CONTAINER:
				return owned().indexOf('W') >= 0;
			case ORE_PACK:
				return false;
			default:
				return owns(r.piece());
		}
	}

	List<Volcanic.Piece> missingPieces()
	{
		List<Volcanic.Piece> out = new ArrayList<>();
		for (Volcanic.Piece p : Volcanic.Piece.values())
		{
			if (!owns(p))
			{
				out.add(p);
			}
		}
		return out;
	}

	/** Points the reward still costs you: the kit's missing pieces, or its price; 0 when yours. */
	int lacking(Volcanic.Reward r)
	{
		if (owns(r))
		{
			return 0;
		}
		if (r == Volcanic.Reward.PROSPECTOR_KIT)
		{
			int n = 0;
			for (Volcanic.Piece p : missingPieces())
			{
				n += p.cost();
			}
			return n;
		}
		return r.cost();
	}

	/** The kit's cheapest missing piece, bought first, or null. */
	Volcanic.Piece nextPiece()
	{
		Volcanic.Piece best = null;
		for (Volcanic.Piece p : missingPieces())
		{
			if (best == null || p.cost() < best.cost())
			{
				best = p;
			}
		}
		return best;
	}

	private void keep(VolcanicLog.Game g)
	{
		if (g == null)
		{
			return;
		}
		log().add(g);
		sessionPoints += g.points;
		sessionXp += g.xp;
		if (save != null)
		{
			save.accept(LOG_KEY, log().encode());
		}
	}

	/** A game message: a death in the mine. */
	void chat(String message)
	{
		if (games != null)
		{
			games.chat(message);
		}
	}

	/** Logged out or hopped: a game under way ends. */
	void relogged(Client client, long now, int tick)
	{
		if (games != null)
		{
			keep(games.logout(tick, client.getSkillExperience(Skill.MINING), now));
		}
		inMine = false;
		wasOpen = false;
	}

	/** In the mine, or at its entrance. */
	boolean active(Client client)
	{
		return client.getLocalPlayer() != null && (inMine || inLobby);
	}

	/** The pace coach's count: none, as a game is no loop to hurry. */
	int units()
	{
		return 0;
	}

	void left()
	{
	}

	Flight fly(Client client, Collection<NPC> npcs, SceneIndex scene, long now)
	{
		int tick = client.getTickCount();
		VolcanicGames g = games();
		VolcanicLog l = log();
		int xp = client.getSkillExperience(Skill.MINING);
		Flight out;
		if (inMine)
		{
			// Not an instruction: the game's name and count, panel only.
			int left = timeLeft(client);
			String clock = left < 0 ? "" : left / 60 + ":" + String.format(Locale.ROOT, "%02d", left % 60) + " left · ";
			Step step = Step.of("Mining", g.playing() ? "Volcanic Mine game" : "Before the game", clock + Session.format(g.gained()) + " points this game", Step.Target.NONE);
			View view = new View(true, View.Activity.VOLCANIC_MINE, step, "Volcanic Mine · in the mine", null, -1);
			out = new Flight(View.Activity.VOLCANIC_MINE, step, view);
			out.quiet = true;
			lastState = "mine";
			mineView(view, client, g, tick, xp);
		}
		else
		{
			VolcanicCycle.Snapshot s = snapshot(client);
			lastState = s;
			Step step = VolcanicCycle.next(s);
			View view = new View(true, View.Activity.VOLCANIC_MINE, step, "Volcanic Mine · the entrance", null, -1);
			out = new Flight(View.Activity.VOLCANIC_MINE, step, view);
			target(client, out, step, npcs, scene);
		}
		stats(out.view, client, l, g, tick);
		if (goal != null)
		{
			out.view.goal(goalCard(l));
		}
		if (!inMine)
		{
			View.PickCard teams = teamsCard(l);
			if (teams != null)
			{
				out.view.pick(teams);
			}
		}
		if (l.count() > 0)
		{
			out.view.supply(historyCard(l));
		}
		return out;
	}

	private VolcanicCycle.Snapshot snapshot(Client client)
	{
		Pack pack = Pack.read(client);
		VolcanicCycle.Snapshot s = new VolcanicCycle.Snapshot();
		s.mining = client.getRealSkillLevel(Skill.MINING);
		s.numulite = pack.count(Volcanic.NUMULITE);
		s.permanent = owned().indexOf('A') >= 0;
		s.points = held();
		s.goal = goal;
		s.goalCost = goal == null ? 0 : lacking(goal);
		s.nextPiece = goal == Volcanic.Reward.PROSPECTOR_KIT ? nextPiece() : null;
		return s;
	}

	private void target(Client client, Flight out, Step step, Collection<NPC> npcs, SceneIndex scene)
	{
		if (step.getTarget() == Step.Target.PETRIFIED_PETE)
		{
			for (NPC n : npcs)
			{
				if (n.getId() == Volcanic.PETE)
				{
					Ground ground = Ground.read(client, Collections.emptyList(), 0);
					Ground.Way<NPC> way = ground == null ? null : ground.to(n);
					if (way != null && !way.path.isEmpty())
					{
						out.route = way.path;
					}
					out.npc = n;
					return;
				}
			}
			return;
		}
		if (step.getTarget() == Step.Target.VOLCANO_BANK)
		{
			Collection<? extends TileObject> things = scene.get(Volcanic.BANK_CHEST);
			if (things == null || things.isEmpty())
			{
				return;
			}
			Ground ground = Ground.read(client, Collections.emptyList(), 0);
			Ground.Way<TileObject> way = ground == null ? null : ground.nearest(things, null);
			if (way != null)
			{
				out.to(way);
				return;
			}
			out.object = things.iterator().next();
		}
	}

	/** In the mine: the game's state as the game shows it, as plain figures. */
	private void mineView(View view, Client client, VolcanicGames g, int tick, int xp)
	{
		int stability = client.getVarbitValue(Volcanic.STABILITY);
		if (stability >= 0 && stability <= 100)
		{
			view.gauge(new View.Gauge("Stability", stability, 0, 100, stability + "%, as the game shows it", Theme.TEAL));
		}
		List<View.Line> lines = new ArrayList<>();
		String[] names = {"Chamber A", "Chamber B", "Chamber C"};
		for (int i = 0; i < Volcanic.VENTS.length; i++)
		{
			int v = client.getVarbitValue(Volcanic.VENTS[i]);
			lines.add(new View.Line(names[i], v >= 0 && v <= Volcanic.VENT_MAX ? v + "% pressure" : "Not checked yet", View.Line.Mark.IDLE));
		}
		int players = client.getVarbitValue(Volcanic.PLAYERS);
		if (players > 0)
		{
			lines.add(new View.Line("Players", Integer.toString(players), View.Line.Mark.IDLE));
		}
		int x = g.xpGained(xp);
		if (x > 0)
		{
			lines.add(new View.Line("Mining experience", Session.format(x) + " this game", View.Line.Mark.IDLE));
		}
		view.lines("This game", StormPilot.clock(g.ticks(tick)) + " in the mine", lines);
	}

	private void stats(View view, Client client, VolcanicLog l, VolcanicGames g, int tick)
	{
		if (inMine)
		{
			int left = timeLeft(client);
			view.stat(new View.Stat(Session.format(g.gained()), "Game points", StormPilot.clock(g.ticks(tick)) + " in the mine", Theme.GOLD_BRIGHT));
			view.stat(new View.Stat(left < 0 ? "-" : left / 60 + ":" + String.format(Locale.ROOT, "%02d", left % 60), "Time left", "Game's clock", Theme.TEXT));
		}
		long have = held();
		view.stat(new View.Stat(have < 0 ? "?" : exact(have), "Points", have < 0 ? "Open his shop" : inMine ? "With this game" : "Shop and games", Theme.GOLD));
		double pph = l.pointsPerHour();
		if (pph >= 0)
		{
			double sit = l.pointsPerHourSitting();
			view.stat(new View.Stat(Session.format(Math.round(sit > 0 ? sit : pph)), "Points an hour", sit > 0 ? "With the waits" : "In the mine", Theme.GOLD));
			double xph = l.xpPerHourSitting() > 0 ? l.xpPerHourSitting() : l.xpPerHour();
			view.stat(new View.Stat(Session.format(Math.round(xph)), "Xp an hour", "Mining", Theme.TEAL));
		}
		int done = games().games() - gamesBase;
		if (done > 0)
		{
			view.stat(new View.Stat(Integer.toString(done), "Games today", Session.format(sessionPoints) + " points · " + Session.format(sessionXp) + " xp", Theme.EMBER));
		}
	}

	/** The reward you are after: points held against its price, and the games and time to it at your pace. */
	View.GoalCard goalCard(VolcanicLog l)
	{
		long have = held();
		boolean kit = goal == Volcanic.Reward.PROSPECTOR_KIT;
		boolean repeat = goal.repeat();
		int lack = lacking(goal);
		boolean done = lack == 0;
		List<View.Line> pieces = new ArrayList<>();
		if (kit || goal.piece() != null)
		{
			for (Volcanic.Piece p : kit ? java.util.Arrays.asList(Volcanic.Piece.values()) : Collections.singletonList(goal.piece()))
			{
				boolean mine = owns(p);
				pieces.add(new View.Line(p.getTitle(), mine ? "Yours" : exact(p.cost()) + " points", mine ? View.Line.Mark.DONE : View.Line.Mark.TODO));
			}
		}
		StringBuilder b = new StringBuilder();
		long need = repeat ? goal.cost() : Math.max(lack, 1);
		long toward;
		if (done)
		{
			b.append("It is yours: choose another under Volcanic Mine reward.");
			toward = need;
		}
		else if (have < 0)
		{
			b.append("Open Petrified Pete's shop once and the Pilot reads your points from it; then the time to it follows from your games.");
			toward = 0;
		}
		else
		{
			long left = repeat ? goal.cost() - have % goal.cost() : Math.max(0, lack - have);
			toward = repeat ? have % goal.cost() : Math.min(have, lack);
			if (repeat && have >= goal.cost())
			{
				b.append(Session.format(have / goal.cost())).append(have / goal.cost() == 1 ? " pack" : " packs").append(" affordable now. ");
			}
			if (left <= 0)
			{
				b.append("You hold the points: buy it from Petrified Pete.");
			}
			else
			{
				b.append(exact(left)).append(" points to ").append(repeat ? "the next pack" : "go");
				double sit = l.pointsPerHourSitting();
				double rate = sit > 0 ? sit : l.pointsPerHour();
				if (rate > 0)
				{
					b.append(": ").append(MixologyPilot.clockHours(left / rate)).append(" at your pace").append(sit > 0 ? ", waits between games counted" : " in the mine");
					int per = l.averagePoints();
					if (per > 0)
					{
						long n = (left + per - 1) / per;
						b.append(" (about ").append(Session.format(n)).append(n == 1 ? " game" : " games").append(')');
					}
					b.append('.');
				}
				else
				{
					b.append(". Play a game and the time to it follows from your pace.");
				}
			}
			if (kit)
			{
				Volcanic.Piece next = nextPiece();
				if (next != null && have < next.cost())
				{
					b.append(" First the ").append(next.getTitle().toLowerCase(Locale.ROOT)).append(", ").append(exact(next.cost())).append('.');
				}
			}
		}
		String title = kit ? "Prospector kit" : goal.getTitle();
		View.GoalCard card = new View.GoalCard(View.Activity.VOLCANIC_MINE, title, toward, need, "points", b.toString().trim(), pieces, done);
		card.aside(kit ? "The full kit gives 2.5% more Mining experience" : goal == Volcanic.Reward.ORE_PACK ? "4,000 points a pack at Petrified Pete's shop" : "At Petrified Pete's shop");
		return card;
	}

	/** Points and experience an hour by the players in your games, best first: yours alone, no community figures. */
	private static View.PickCard teamsCard(VolcanicLog l)
	{
		Map<Integer, long[]> by = new LinkedHashMap<>();
		for (VolcanicLog.Game g : l.all())
		{
			if (g.ticks <= 0 || g.players <= 0)
			{
				continue;
			}
			long[] a = by.computeIfAbsent(g.players, k -> new long[4]);
			a[0]++;
			a[1] += g.points;
			a[2] += g.xp;
			a[3] += g.ticks;
		}
		if (by.size() < 2)
		{
			return null;
		}
		List<Map.Entry<Integer, long[]>> rows = new ArrayList<>(by.entrySet());
		rows.sort((a, b) -> Double.compare(perHour(b.getValue()[1], b.getValue()[3]), perHour(a.getValue()[1], a.getValue()[3])));
		double top = Math.max(1, perHour(rows.get(0).getValue()[1], rows.get(0).getValue()[3]));
		VolcanicLog.Game last = l.last();
		List<View.Pick> picks = new ArrayList<>();
		for (int i = 0; i < rows.size() && i < 5; i++)
		{
			int n = rows.get(i).getKey();
			long[] a = rows.get(i).getValue();
			double ph = perHour(a[1], a[3]);
			picks.add(new View.Pick(n == 1 ? "Solo" : n + " players", Session.format(Math.round(ph)) + "/h",
				a[0] + (a[0] == 1 ? " game" : " games") + " · " + Session.format(Math.round(perHour(a[2], a[3]))) + " xp/h",
				true, last != null && last.players == n, ph / top, false));
		}
		return new View.PickCard("By team", rows.size() + " sizes",
			"Points an hour in the mine from your own games, by how many played. Teams change the mine, so only yours are ranked.", picks);
	}

	/** Points written in full, as the shop writes them: "18,500". */
	static String exact(long n)
	{
		return String.format(Locale.ROOT, "%,d", n);
	}

	private static double perHour(long amount, long ticks)
	{
		return ticks <= 0 ? 0 : amount * 3600.0 / (ticks * 0.6);
	}

	/** Your last games, the best, and your pace across them. */
	private static View.Supply historyCard(VolcanicLog l)
	{
		List<View.Line> lines = new ArrayList<>();
		List<VolcanicLog.Game> all = l.all();
		VolcanicLog.Game best = l.best();
		for (int i = all.size() - 1; i >= 0 && i >= all.size() - 5; i--)
		{
			VolcanicLog.Game x = all.get(i);
			String what = Session.format(x.points) + " pts · " + Session.format(x.xp) + " xp" + (x.ticks > 0 ? " · " + StormPilot.clock(x.ticks) : "");
			String trailing = x.end == VolcanicLog.End.LEFT ? x.team() : x.end.getTitle();
			lines.add(new View.Line(what, trailing, x == best ? View.Line.Mark.DONE : x.end == VolcanicLog.End.LEFT ? View.Line.Mark.IDLE : View.Line.Mark.TODO));
		}
		String headline = best == null ? null : "Best game: " + Session.format(best.points) + " points" + (best.team().isEmpty() ? "" : ", " + best.team().toLowerCase(Locale.ROOT));
		double h = l.pointsPerHour();
		String detail = null;
		if (h >= 0)
		{
			detail = Session.format(Math.round(h)) + " points and " + Session.format(Math.round(l.xpPerHour())) + " Mining xp an hour in the mine";
			double sit = l.pointsPerHourSitting();
			if (sit > 0)
			{
				detail += "; " + Session.format(Math.round(sit)) + " and " + Session.format(Math.round(l.xpPerHourSitting())) + " with the waits between games";
			}
			int deaths = l.deaths();
			if (deaths > 0)
			{
				detail += ". " + deaths + (deaths == 1 ? " game" : " games") + " ended in a death";
			}
		}
		return new View.Supply(l.count() == 1 ? "1 kept" : l.count() + " kept", lines, headline, detail, false, null).titled("Your games");
	}

	/** Brief for the panel away from the Volcanic Mine. */
	View.Brief brief()
	{
		VolcanicLog l = log();
		VolcanicLog.Game last = l.last();
		String status = last == null ? null : "Last game: " + Session.format(last.points) + " points, " + Session.format(last.xp) + " Mining xp"
			+ (last.ticks > 0 ? " in " + StormPilot.clock(last.ticks) : "");
		View.Brief b = new View.Brief(View.Activity.VOLCANIC_MINE, status,
			"Under the Fossil Island Volcano, from 50 Mining. Pay Petrified Pete 30 numulite and go in"
				+ (goal == null ? "." : "; saving for " + (goal == Volcanic.Reward.PROSPECTOR_KIT ? "the prospector kit." : goal == Volcanic.Reward.ORE_PACK ? "ore packs." : "the " + goal.getTitle().toLowerCase(Locale.ROOT) + ".")));
		double h = l.pointsPerHour();
		if (h >= 0)
		{
			b.best(Session.format(Math.round(h)) + " points and " + Session.format(Math.round(l.xpPerHour())) + " xp an hour over " + l.count() + (l.count() == 1 ? " game." : " games."));
		}
		return b;
	}
}
