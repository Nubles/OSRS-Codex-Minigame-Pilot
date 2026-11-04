package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Brewing;
import com.codex.minigamepilot.core.BrewingCycle;
import com.codex.minigamepilot.core.BrewingGames;
import com.codex.minigamepilot.core.BrewingLog;
import com.codex.minigamepilot.core.Step;
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
import java.util.function.Predicate;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;

/**
 * Trouble Brewing: every game kept per account (your team, the rum it made against the other
 * team's, the pieces of eight it paid by the game's own count, the Cooking experience it gave and
 * its length), your pieces of eight an hour at your own pace, and the time to the naval set or
 * flag you are after at Honest Jimmy's prices. In the lobby one next step (Cabin Fever, 40
 * Cooking, the reward once affordable, an empty inventory and head, a team); in a game one plain
 * line on the next link of the ingredient chain you are on.
 *
 * <p>It reads the game only and draws: no object is clicked for you, and the in-game step is
 * information on what the wiki says the ingredient needs next.
 */
public final class BrewingPilot
{
	static final String LOG_KEY = "brewing.kept";
	static final String OWNED_KEY = "brewing.owned";
	/** The lobby: within this many tiles of the Trouble Brewing lobby on the ground floor. */
	static final int LOBBY_REACH = 20;
	/** How often a quest not yet done is asked about again (ticks). */
	static final int QUEST_RECHECK = 100;

	private Function<String, String> load;
	private BiConsumer<String, String> save;
	private Predicate<Quest> questDone;
	private BrewingLog log;
	private BrewingGames games;
	private String owned;
	private Brewing.Reward goal = Brewing.Reward.NAVAL_BLUE;

	// Read each tick.
	private boolean inGame;
	private boolean waiting;
	private boolean inLobby;
	private Boolean cabinFever;
	private int questAt = -QUEST_RECHECK;

	// This session.
	private int gamesBase;
	private long sessionPieces;
	private long sessionXp;

	Object lastState;

	void setStore(Function<String, String> load, BiConsumer<String, String> save)
	{
		this.load = load;
		this.save = save;
	}

	/** Tests: whether a quest is done, as FakeGame cannot run the game's quest script. */
	void readers(Predicate<Quest> questDone)
	{
		this.questDone = questDone;
		cabinFever = null;
	}

	/** @param reward the reward saved for, or null for none */
	void settings(Brewing.Reward reward)
	{
		this.goal = reward;
	}

	void newAccount()
	{
		log = null;
		games = null;
		owned = null;
		cabinFever = null;
		questAt = -QUEST_RECHECK;
		gamesBase = 0;
		sessionPieces = 0;
		sessionXp = 0;
	}

	BrewingLog log()
	{
		if (log == null)
		{
			log = BrewingLog.decode(load == null ? null : load.apply(LOG_KEY));
		}
		return log;
	}

	private BrewingGames games()
	{
		if (games == null)
		{
			games = new BrewingGames();
			gamesBase = 0;
		}
		return games;
	}

	static boolean shown(Client client, int component)
	{
		Widget w = client.getWidget(component);
		return w != null && !w.isHidden();
	}

	static String text(Client client, int component)
	{
		Widget w = client.getWidget(component);
		return w == null || w.isHidden() ? null : w.getText();
	}

	static int number(Client client, int component)
	{
		return Brewing.number(text(client, component));
	}

	/** Every tick, wherever you are: so a game is kept even as it ends or you log out. */
	void watch(Client client, long now)
	{
		WorldPoint at = client.getLocalPlayer() == null ? null
			: WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation());
		inGame = shown(client, Brewing.HUD_TOP) || shown(client, Brewing.HUD_TIME);
		waiting = shown(client, Brewing.WAITING);
		inLobby = waiting || (at != null && !inGame && at.getPlane() == 0 && Math.abs(at.getX() - Brewing.LOBBY_X) <= LOBBY_REACH
			&& Math.abs(at.getY() - Brewing.LOBBY_Y) <= LOBBY_REACH);
		BrewingGames g = games();
		boolean over = shown(client, Brewing.OVER);
		if (!inGame && !inLobby && !over && !g.open() && !g.settling())
		{
			return;
		}
		Pack pack = Pack.read(client);
		learnOwned(pack);
		keep(g.tick(client.getTickCount(), reading(client, over), now));
	}

	private BrewingGames.Reading reading(Client client, boolean over)
	{
		BrewingGames.Reading r = new BrewingGames.Reading();
		r.inGame = inGame;
		r.over = over;
		r.team = team(client);
		if (inGame)
		{
			r.red = number(client, Brewing.HUD_RED_SCORE);
			r.blue = number(client, Brewing.HUD_BLUE_SCORE);
		}
		if (over)
		{
			r.overRed = number(client, Brewing.OVER_RED);
			r.overBlue = number(client, Brewing.OVER_BLUE);
			r.overPieces = number(client, Brewing.OVER_PIECES);
		}
		r.pieces = pieces(client);
		r.xp = client.getSkillExperience(Skill.COOKING);
		return r;
	}

	/** Pieces of eight held, the game's own count, or -1. */
	static long pieces(Client client)
	{
		int v = client.getVarpValue(Brewing.PIECES);
		return v < 0 || v > Brewing.PIECES_CAP ? -1 : v;
	}

	/** The team whose hat is on your head, or null. */
	static Brewing.Team team(Client client)
	{
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn == null)
		{
			return null;
		}
		Item head = worn.getItem(EquipmentInventorySlot.HEAD.getSlotIdx());
		return head == null ? null : Brewing.Team.byHat(head.getId());
	}

	private void learnOwned(Pack pack)
	{
		String o = owned();
		StringBuilder b = new StringBuilder(o);
		for (Brewing.Piece p : Brewing.Piece.values())
		{
			int id = p.item();
			if ((pack.carries(id) || pack.wears(id) || pack.banked(id) > 0) && b.indexOf(String.valueOf(p.code())) < 0)
			{
				b.append(p.code());
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
			if (pieceCodes().indexOf(c) >= 0 && b.indexOf(String.valueOf(c)) < 0)
			{
				b.append(c);
			}
		}
		return b.toString();
	}

	private static String pieceCodes()
	{
		StringBuilder b = new StringBuilder();
		for (Brewing.Piece p : Brewing.Piece.values())
		{
			b.append(p.code());
		}
		return b.toString();
	}

	boolean owns(Brewing.Piece p)
	{
		return owned().indexOf(p.code()) >= 0;
	}

	List<Brewing.Piece> missing(Brewing.Reward r)
	{
		List<Brewing.Piece> out = new ArrayList<>();
		for (Brewing.Piece p : r.pieces())
		{
			if (!owns(p))
			{
				out.add(p);
			}
		}
		return out;
	}

	/** Pieces of eight the reward still costs you: its pieces not yet owned; one of it when bought again and again. */
	int lacking(Brewing.Reward r)
	{
		if (r.repeat())
		{
			return r.cost();
		}
		int n = 0;
		for (Brewing.Piece p : missing(r))
		{
			n += p.cost();
		}
		return n;
	}

	/** The reward's cheapest piece still to buy, or null. */
	Brewing.Piece nextPiece(Brewing.Reward r)
	{
		Brewing.Piece best = null;
		for (Brewing.Piece p : missing(r))
		{
			if (best == null || p.cost() < best.cost())
			{
				best = p;
			}
		}
		return best;
	}

	private void keep(BrewingLog.Game g)
	{
		if (g == null)
		{
			return;
		}
		log().add(g);
		sessionPieces += g.pieces;
		sessionXp += g.xp;
		if (save != null)
		{
			save.accept(LOG_KEY, log().encode());
		}
	}

	/** Logged out or hopped: a game under way ends. */
	void relogged(Client client, long now, int tick)
	{
		if (games != null)
		{
			BrewingGames.Reading r = new BrewingGames.Reading();
			r.pieces = pieces(client);
			r.xp = client.getSkillExperience(Skill.COOKING);
			keep(games.logout(tick, r, now));
		}
		inGame = false;
		waiting = false;
	}

	/** In a game, the waiting room or the lobby. */
	boolean active(Client client)
	{
		return client.getLocalPlayer() != null && (inGame || inLobby || games().settling());
	}

	/** The pace coach's count: none, as a game is no loop to hurry. */
	int units()
	{
		return 0;
	}

	void left()
	{
	}

	private boolean cabinFever(Client client)
	{
		int tick = client.getTickCount();
		if (Boolean.TRUE.equals(cabinFever) || (cabinFever != null && tick - questAt < QUEST_RECHECK))
		{
			return cabinFever;
		}
		boolean done;
		try
		{
			done = questDone != null ? questDone.test(Quest.CABIN_FEVER) : Quest.CABIN_FEVER.getState(client) == QuestState.FINISHED;
		}
		catch (RuntimeException e)
		{
			done = false;
		}
		cabinFever = done;
		questAt = tick;
		return done;
	}

	Flight fly(Client client, Collection<NPC> npcs, long now)
	{
		int tick = client.getTickCount();
		BrewingGames g = games();
		BrewingLog l = log();
		Flight out;
		if (inGame)
		{
			BrewingCycle.Game s = gameSnapshot(client);
			lastState = s;
			Step step = BrewingCycle.game(s);
			Brewing.Team team = g.team();
			View view = new View(true, View.Activity.TROUBLE_BREWING, step,
				"Trouble Brewing · " + (team == null ? "in a game" : team.getTitle().toLowerCase(Locale.ROOT) + " team"), null, -1);
			out = new Flight(View.Activity.TROUBLE_BREWING, step, view);
			gameView(view, client, g, s, tick);
		}
		else
		{
			BrewingCycle.Lobby s = lobbySnapshot(client);
			lastState = s;
			Step step = BrewingCycle.lobby(s);
			View view = new View(true, View.Activity.TROUBLE_BREWING, step,
				"Trouble Brewing · " + (waiting ? "waiting" : "the lobby"), null, -1);
			out = new Flight(View.Activity.TROUBLE_BREWING, step, view);
			if (step.getTarget() == Step.Target.HONEST_JIMMY)
			{
				for (NPC n : npcs)
				{
					if (n.getId() == Brewing.HONEST_JIMMY)
					{
						Ground ground = Ground.read(client, Collections.emptyList(), 0);
						Ground.Way<NPC> way = ground == null ? null : ground.to(n);
						if (way != null && !way.path.isEmpty())
						{
							out.route = way.path;
						}
						out.npc = n;
						break;
					}
				}
			}
			if (waiting)
			{
				waitingView(view, client);
			}
		}
		stats(out.view, client, l, g);
		if (goal != null)
		{
			out.view.goal(goalCard(client, l));
		}
		if (!inGame)
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

	private BrewingCycle.Lobby lobbySnapshot(Client client)
	{
		Pack pack = Pack.read(client);
		BrewingCycle.Lobby s = new BrewingCycle.Lobby();
		s.cabinFever = cabinFever(client);
		s.cooking = client.getRealSkillLevel(Skill.COOKING);
		s.carried = 28 - pack.free();
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		Item head = worn == null ? null : worn.getItem(EquipmentInventorySlot.HEAD.getSlotIdx());
		s.hat = head != null && head.getId() > 0 && head.getQuantity() > 0;
		s.follower = client.getFollower() != null;
		s.waiting = waiting;
		s.waitingText = waiting ? Brewing.plain(text(client, Brewing.WAITING_TIME)) : "";
		s.pieces = pieces(client);
		s.goal = goal;
		s.goalCost = goal == null ? 0 : lacking(goal);
		s.nextPiece = goal == null || goal.repeat() ? null : nextPiece(goal);
		return s;
	}

	private BrewingCycle.Game gameSnapshot(Client client)
	{
		BrewingCycle.Game s = new BrewingCycle.Game();
		s.carried = Pack.read(client).contents();
		for (Brewing.Ingredient i : Brewing.Ingredient.values())
		{
			s.hopper[i.ordinal()] = number(client, i.hud());
		}
		for (int i = 0; i < Brewing.HUD_BOILERS.length; i++)
		{
			s.boilers[i] = number(client, Brewing.HUD_BOILERS[i]);
		}
		return s;
	}

	/** In a game: the hopper and boilers as the game's overlay counts them, and this game so far. */
	private void gameView(View view, Client client, BrewingGames g, BrewingCycle.Game s, int tick)
	{
		List<View.Line> still = new ArrayList<>();
		for (Brewing.Ingredient i : Brewing.Ingredient.values())
		{
			int n = s.hopper[i.ordinal()];
			if (n >= 0)
			{
				still.add(new View.Line(i.getTitle(), n + " · " + i.perBottle() + " a bottle", n >= i.perBottle() ? View.Line.Mark.DONE : View.Line.Mark.TODO));
			}
		}
		for (int i = 0; i < s.boilers.length; i++)
		{
			int n = s.boilers[i];
			if (n >= 0)
			{
				still.add(new View.Line("Boiler " + (i + 1), n + (n == 1 ? " log" : " logs"), n >= Brewing.BOILER_LOGS_MIN ? View.Line.Mark.DONE : View.Line.Mark.TODO));
			}
		}
		if (!still.isEmpty())
		{
			view.lines("The still", "Overlay's counts", still);
		}
		int rum = g.rum();
		int rival = g.rival();
		if (rum >= 0 && rival >= 0)
		{
			int most = Math.max(1, Math.max(rum, rival));
			Brewing.Team t = g.team();
			view.gauge(new View.Gauge(t.getTitle() + " team (yours)", rum, 0, most, rum + (rum == 1 ? " bottle" : " bottles"), t == Brewing.Team.RED ? Theme.EMBER : Theme.TEAL));
			view.gauge(new View.Gauge(t.other().getTitle() + " team", rival, 0, most, rival + (rival == 1 ? " bottle" : " bottles"), Theme.TEXT_MUTED));
		}
	}

	private void waitingView(View view, Client client)
	{
		List<View.Line> lines = new ArrayList<>();
		int red = number(client, Brewing.WAITING_RED);
		int blue = number(client, Brewing.WAITING_BLUE);
		if (blue >= 0)
		{
			lines.add(new View.Line("Blue team", Integer.toString(blue), View.Line.Mark.IDLE));
		}
		if (red >= 0)
		{
			lines.add(new View.Line("Red team", Integer.toString(red), View.Line.Mark.IDLE));
		}
		String time = Brewing.plain(text(client, Brewing.WAITING_TIME));
		if (!lines.isEmpty() || !time.isEmpty())
		{
			view.lines("Waiting", "Players signed", lines);
		}
	}

	private void stats(View view, Client client, BrewingLog l, BrewingGames g)
	{
		if (inGame)
		{
			int rum = g.rum();
			view.stat(new View.Stat(rum < 0 ? "-" : Integer.toString(rum), "Rum", rum < 0 ? "Your team" : String.format(Locale.ROOT, "%d pieces each", Brewing.PIECES_PER_RUM), Theme.GOLD_BRIGHT));
			String time = Brewing.plain(text(client, Brewing.HUD_TIME));
			view.stat(new View.Stat(time.isEmpty() ? "-" : shortTime(time), "Time left", "Game's clock", Theme.TEXT));
			int xp = g.xpGained(client.getSkillExperience(Skill.COOKING));
			if (xp > 0)
			{
				view.stat(new View.Stat(Session.format(xp), "Cooking xp", "This game", Theme.TEAL));
			}
		}
		long have = pieces(client);
		view.stat(new View.Stat(have < 0 ? "?" : exact(have), "Pieces of eight", "Held", Theme.GOLD));
		double pph = l.piecesPerHour();
		if (pph >= 0)
		{
			double sit = l.piecesPerHourSitting();
			view.stat(new View.Stat(Session.format(Math.round(sit > 0 ? sit : pph)), "Pieces an hour", sit > 0 ? "With the waits" : "In the game", Theme.GOLD));
			if (!inGame)
			{
				double xph = l.xpPerHourSitting() > 0 ? l.xpPerHourSitting() : l.xpPerHour();
				view.stat(new View.Stat(Session.format(Math.round(xph)), "Xp an hour", "Cooking", Theme.TEAL));
			}
		}
		int done = games().games() - gamesBase;
		if (done > 0)
		{
			view.stat(new View.Stat(Integer.toString(done), "Games today", Session.format(sessionPieces) + " pieces · " + Session.format(sessionXp) + " xp", Theme.EMBER));
		}
	}

	/** The overlay's time as short as it will go for a stat tile ("12 mins" stays as written). */
	private static String shortTime(String t)
	{
		return t.length() <= 9 ? t : t.substring(t.length() - 9).trim();
	}

	/** The reward you are after: pieces of eight held against its price, and the games and time to it at your pace. */
	View.GoalCard goalCard(Client client, BrewingLog l)
	{
		long have = pieces(client);
		boolean repeat = goal.repeat();
		int lack = lacking(goal);
		boolean done = !repeat && lack == 0;
		List<View.Line> pieces = new ArrayList<>();
		if (!repeat)
		{
			List<Brewing.Piece> all = goal.pieces();
			if (all.size() <= 6)
			{
				for (Brewing.Piece p : all)
				{
					boolean mine = owns(p);
					pieces.add(new View.Line(p.getTitle(), mine ? "Yours" : exact(p.cost()), mine ? View.Line.Mark.DONE : View.Line.Mark.TODO));
				}
			}
			else
			{
				int ownedN = all.size() - missing(goal).size();
				pieces.add(new View.Line("Flags and naval pieces", ownedN + " of " + all.size() + " yours", ownedN == all.size() ? View.Line.Mark.DONE : View.Line.Mark.TODO));
			}
		}
		StringBuilder b = new StringBuilder();
		long need = repeat ? goal.cost() : Math.max(lack, 1);
		long toward;
		if (done)
		{
			b.append("It is yours: choose another under Trouble Brewing reward.");
			toward = need;
		}
		else if (have < 0)
		{
			b.append("Your pieces of eight are read once you are logged in.");
			toward = 0;
		}
		else
		{
			long left = repeat ? goal.cost() - have % goal.cost() : Math.max(0, lack - have);
			toward = repeat ? have % goal.cost() : Math.min(have, lack);
			if (repeat && have >= goal.cost())
			{
				long n = have / goal.cost();
				b.append(Session.format(n)).append(" affordable now. ");
			}
			if (left <= 0)
			{
				b.append("You hold the pieces of eight: buy it from Honest Jimmy.");
			}
			else
			{
				b.append(exact(left)).append(" pieces of eight to ").append(repeat ? "the next" : "go");
				double sit = l.piecesPerHourSitting();
				double rate = sit > 0 ? sit : l.piecesPerHour();
				if (rate > 0)
				{
					b.append(": ").append(MixologyPilot.clockHours(left / rate)).append(" at your pace").append(sit > 0 ? ", waits between games counted" : " in the game");
					int per = l.averagePieces();
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
			Brewing.Piece next = repeat ? null : nextPiece(goal);
			if (next != null && have < next.cost() && goal.pieces().size() > 1)
			{
				b.append(" First the ").append(next.getTitle().toLowerCase(Locale.ROOT)).append(", ").append(exact(next.cost())).append('.');
			}
		}
		View.GoalCard card = new View.GoalCard(View.Activity.TROUBLE_BREWING, goal.getTitle(), toward, need, "pieces of eight", b.toString().trim(), pieces, done);
		card.aside(goal == Brewing.Reward.MEDIUM_CLUE ? "5,000 a map from Honest Jimmy" : goal == Brewing.Reward.EVERYTHING
			? "The shop's 39,590 less the rum and the stuff" : "At Honest Jimmy's House of Stuff");
		return card;
	}

	/** Pieces of eight an hour by your team, best first: from your own games only. */
	private static View.PickCard teamsCard(BrewingLog l)
	{
		Map<Brewing.Team, long[]> by = new LinkedHashMap<>();
		for (BrewingLog.Game g : l.all())
		{
			if (g.ticks <= 0 || g.team == null)
			{
				continue;
			}
			long[] a = by.computeIfAbsent(g.team, k -> new long[4]);
			a[0]++;
			a[1] += g.pieces;
			a[2] += "Won".equals(g.result()) ? 1 : 0;
			a[3] += g.ticks;
		}
		if (by.size() < 2)
		{
			return null;
		}
		List<Map.Entry<Brewing.Team, long[]>> rows = new ArrayList<>(by.entrySet());
		rows.sort((a, b) -> Double.compare(perHour(b.getValue()[1], b.getValue()[3]), perHour(a.getValue()[1], a.getValue()[3])));
		double top = Math.max(1, perHour(rows.get(0).getValue()[1], rows.get(0).getValue()[3]));
		BrewingLog.Game last = l.last();
		List<View.Pick> picks = new ArrayList<>();
		for (Map.Entry<Brewing.Team, long[]> e : rows)
		{
			long[] a = e.getValue();
			double ph = perHour(a[1], a[3]);
			picks.add(new View.Pick(e.getKey().getTitle() + " · " + e.getKey().leader(), Session.format(Math.round(ph)) + "/h",
				a[0] + (a[0] == 1 ? " game" : " games") + " · " + a[2] + " won", true, last != null && last.team == e.getKey(), ph / top, false));
		}
		return new View.PickCard("By team", "2 teams",
			"Pieces of eight an hour in the game from your own games, by the team you played for.", picks);
	}

	static String exact(long n)
	{
		return String.format(Locale.ROOT, "%,d", n);
	}

	private static double perHour(long amount, long ticks)
	{
		return ticks <= 0 ? 0 : amount * 3600.0 / (ticks * 0.6);
	}

	/** Your last games, the best, and your pace across them. */
	private static View.Supply historyCard(BrewingLog l)
	{
		List<View.Line> lines = new ArrayList<>();
		List<BrewingLog.Game> all = l.all();
		BrewingLog.Game best = l.best();
		for (int i = all.size() - 1; i >= 0 && i >= all.size() - 5; i--)
		{
			BrewingLog.Game x = all.get(i);
			String rum = x.rum >= 0 ? x.rum + (x.rival >= 0 ? "-" + x.rival : "") + " rum · " : "";
			String what = rum + Session.format(x.pieces) + " pieces" + (x.ticks > 0 ? " · " + StormPilot.clock(x.ticks) : "");
			String trailing = x.end != BrewingLog.End.FINISHED ? x.end.getTitle() : !x.result().isEmpty() ? x.result() : x.team == null ? "" : x.team.getTitle();
			lines.add(new View.Line(what, trailing, x == best ? View.Line.Mark.DONE : x.end == BrewingLog.End.FINISHED ? View.Line.Mark.IDLE : View.Line.Mark.TODO));
		}
		String headline = best == null ? null : "Best game: " + Session.format(best.pieces) + " pieces of eight"
			+ (best.rum >= 0 ? ", " + best.rum + " rum" : "");
		double h = l.piecesPerHour();
		String detail = null;
		if (h >= 0)
		{
			detail = Session.format(Math.round(h)) + " pieces of eight and " + Session.format(Math.round(l.xpPerHour())) + " Cooking xp an hour in the game";
			double sit = l.piecesPerHourSitting();
			if (sit > 0)
			{
				detail += "; " + Session.format(Math.round(sit)) + " and " + Session.format(Math.round(l.xpPerHourSitting())) + " with the waits between games";
			}
			if (l.scored() > 0)
			{
				detail += ". Won " + l.won() + " of " + l.scored();
			}
		}
		return new View.Supply(l.count() == 1 ? "1 kept" : l.count() + " kept", lines, headline, detail, false, null).titled("Your games");
	}

	/** Brief for the panel away from Trouble Brewing. */
	View.Brief brief()
	{
		BrewingLog l = log();
		BrewingLog.Game last = l.last();
		String status = last == null ? null : "Last game: " + Session.format(last.pieces) + " pieces of eight, " + Session.format(last.xp) + " Cooking xp"
			+ (last.rum >= 0 ? ", " + last.rum + " rum" : "");
		View.Brief b = new View.Brief(View.Activity.TROUBLE_BREWING, status,
			"On Mos Le'Harmless, after Cabin Fever and from 40 Cooking. Bank everything and join a team"
				+ (goal == null ? "." : "; saving for " + (goal.repeat() ? goal.getTitle().toLowerCase(Locale.ROOT) : "the " + goal.getTitle().toLowerCase(Locale.ROOT)) + "."));
		double h = l.piecesPerHour();
		if (h >= 0)
		{
			b.best(Session.format(Math.round(h)) + " pieces of eight and " + Session.format(Math.round(l.xpPerHour())) + " xp an hour over " + l.count() + (l.count() == 1 ? " game." : " games."));
		}
		return b;
	}
}
