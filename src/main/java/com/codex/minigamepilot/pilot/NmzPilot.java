package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Nmz;
import com.codex.minigamepilot.core.NmzCycle;
import com.codex.minigamepilot.core.NmzDreams;
import com.codex.minigamepilot.core.NmzKills;
import com.codex.minigamepilot.core.NmzLog;
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
import net.runelite.api.NPC;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

/**
 * The Nightmare Zone: every dream kept per account (its mode, the bosses in it, its points from
 * the game's own count, its time and the potions drunk), your points an hour for each set of
 * bosses you dream, the points each boss gives a kill as you have seen them, and the time to the
 * imbue or herb boxes you are after. In the lobby, one next step: the dream potion, the potions
 * you drink a dream, the coffer, Dominic Onion.
 *
 * <p>It reads the game only. Inside a dream it says nothing to do: the panel keeps the dream's
 * count, and shows absorption and overload left as plain figures, as RuneLite's own Nightmare
 * Zone and Timers plugins read them. No cue to drink, pray or fight is ever given.
 */
public final class NmzPilot
{
	static final String LOG_KEY = "nmz.kept";
	static final String KILLS_KEY = "nmz.kills";
	/** The lobby: within this many tiles of the Nightmare Zone on the ground floor. */
	static final int LOBBY_REACH = 20;

	private Function<String, String> load;
	private BiConsumer<String, String> save;
	private IntUnaryOperator price;
	private NmzLog log;
	private NmzKills kills;
	private NmzDreams dreams;
	private Nmz.Reward goal = Nmz.Reward.HERB_BOX;
	private boolean halved;

	// Read each tick.
	private boolean inArena;
	private boolean inLobby;
	private int reward = -1;
	private int rewardAtStart = -1;
	private boolean wasOpen;

	// This session.
	private int dreamsBase;
	private long sessionPoints;

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

	/** @param reward the reward saved for, or null for none; @param halved imbues at half price (Hard Combat Achievements) */
	void settings(Nmz.Reward reward, boolean halved)
	{
		this.goal = reward;
		this.halved = halved;
	}

	void newAccount()
	{
		log = null;
		kills = null;
		dreams = null;
		dreamsBase = 0;
		sessionPoints = 0;
		rewardAtStart = -1;
		wasOpen = false;
	}

	NmzLog log()
	{
		if (log == null)
		{
			log = NmzLog.decode(load == null ? null : load.apply(LOG_KEY));
		}
		return log;
	}

	NmzKills kills()
	{
		if (kills == null)
		{
			kills = NmzKills.decode(load == null ? null : load.apply(KILLS_KEY));
		}
		return kills;
	}

	private NmzDreams dreams()
	{
		if (dreams == null)
		{
			dreams = new NmzDreams(kills());
			dreamsBase = 0;
		}
		return dreams;
	}

	static boolean isWatched(int id)
	{
		if (id == Nmz.VIAL_FULL || id == Nmz.CHEST || StormPilot.contains(Nmz.COFFER, id))
		{
			return true;
		}
		for (Nmz.Potion p : Nmz.Potion.values())
		{
			if (p.barrel() == id)
			{
				return true;
			}
		}
		return false;
	}

	/** Every tick, wherever you are: so a dream is kept even as you wake or log out. */
	void watch(Client client, long now)
	{
		int tick = client.getTickCount();
		WorldPoint at = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getWorldLocation();
		inArena = at != null && Nmz.inArena(at.getRegionID(), at.getPlane());
		inLobby = at != null && at.getPlane() == 0 && Math.abs(at.getX() - Nmz.LOBBY_X) <= LOBBY_REACH && Math.abs(at.getY() - Nmz.LOBBY_Y) <= LOBBY_REACH;
		if (!inArena && !inLobby && !dreams().open())
		{
			return;
		}
		reward = client.getVarpValue(Nmz.REWARD_POINTS);
		Pack pack = Pack.read(client);
		List<NmzDreams.Seen> seen = new ArrayList<>();
		if (inArena)
		{
			for (NPC n : client.getTopLevelWorldView().npcs())
			{
				if (n != null && Nmz.boss(n.getId()))
				{
					seen.add(new NmzDreams.Seen(n.getIndex(), n.getName(), Nmz.hard(n.getId())));
				}
			}
		}
		int cash = inLobby ? client.getVarbitValue(Nmz.COFFER_THOUSANDS) : -1;
		NmzDreams d = dreams();
		keep(d.tick(tick, inArena, inArena ? client.getVarbitValue(Nmz.DREAM_POINTS) : -1, inArena ? client.getVarbitValue(Nmz.COFFER_THOUSANDS) : cash,
			doses(pack), seen, now));
		if (d.open() && !wasOpen)
		{
			rewardAtStart = reward;
		}
		wasOpen = d.open();
	}

	/** Doses carried of each potion, by ordinal. */
	static int[] doses(Pack pack)
	{
		int[] out = new int[Nmz.Potion.values().length];
		for (Nmz.Potion p : Nmz.Potion.values())
		{
			int[] ids = p.ids();
			for (int i = 0; i < ids.length; i++)
			{
				out[p.ordinal()] += pack.count(ids[i]) * (4 - i);
			}
		}
		return out;
	}

	private void keep(NmzLog.Dream d)
	{
		if (d == null)
		{
			return;
		}
		log().add(d);
		sessionPoints += d.points;
		if (save != null)
		{
			save.accept(LOG_KEY, log().encode());
			save.accept(KILLS_KEY, kills().encode());
		}
	}

	/** Logged out or hopped: a dream under way ends. */
	void relogged(long now, int tick)
	{
		if (dreams != null)
		{
			keep(dreams.logout(tick, now));
		}
		inArena = false;
		wasOpen = false;
	}

	/** In a dream, or in the lobby. */
	boolean active(Client client)
	{
		return client.getLocalPlayer() != null && (inArena || inLobby);
	}

	/** The pace coach's count: none, as a dream is no loop to hurry. */
	int units()
	{
		return 0;
	}

	void left()
	{
	}

	/** Reward points held, a dream's points counted in while they have not reached the chest yet. */
	long held()
	{
		if (reward < 0)
		{
			return 0;
		}
		NmzDreams d = dreams();
		return d.open() && reward <= rewardAtStart ? (long) reward + d.gained() : reward;
	}

	Flight fly(Client client, SceneIndex scene, Collection<NPC> npcs, long now)
	{
		int tick = client.getTickCount();
		Pack pack = Pack.read(client);
		NmzDreams d = dreams();
		NmzLog g = log();
		Flight out;
		if (inArena)
		{
			String kind = kindNow(d);
			// Not an instruction: the dream's name and count, panel only.
			Step step = Step.of("Dreaming", kind, Session.format(d.gained()) + " points this dream", Step.Target.NONE);
			View view = new View(true, View.Activity.NIGHTMARE_ZONE, step, "Nightmare Zone · in the dream", null, -1);
			out = new Flight(View.Activity.NIGHTMARE_ZONE, step, view);
			out.quiet = true;
			lastState = "dream";
			dreamView(view, client, d, tick);
		}
		else
		{
			NmzCycle.Snapshot s = snapshot(client, scene, pack, g);
			lastState = s;
			Step step = NmzCycle.next(s);
			View view = new View(true, View.Activity.NIGHTMARE_ZONE, step, "Nightmare Zone · the lobby", null, -1);
			out = new Flight(View.Activity.NIGHTMARE_ZONE, step, view);
			target(client, out, step, scene, npcs);
			lobbyView(view, s);
		}
		stats(out.view, g, d, tick, now);
		if (goal != null)
		{
			out.view.goal(goalCard(g, ironman(client)));
		}
		if (!inArena)
		{
			View.PickCard sets = setsCard(g);
			if (sets != null)
			{
				out.view.pick(sets);
			}
		}
		if (g.count() > 0)
		{
			out.view.supply(historyCard(g));
		}
		return out;
	}

	private static boolean ironman(Client client)
	{
		return client.getVarbitValue(Nmz.IRONMAN) != 0;
	}

	/** "Hard rumble", from the price paid or the bosses; "Dreaming" before either is seen. */
	private static String kindNow(NmzDreams d)
	{
		Nmz.Mode m = d.mode();
		char diff = d.difficulty();
		String dd = diff == 'H' ? "Hard" : diff == 'N' ? "Normal" : "";
		if (m == null)
		{
			return dd.isEmpty() ? "Dreaming" : dd + " dream";
		}
		return dd.isEmpty() ? m.getTitle() : dd + " " + m.getTitle().toLowerCase(Locale.ROOT);
	}

	private NmzCycle.Snapshot snapshot(Client client, SceneIndex scene, Pack pack, NmzLog g)
	{
		NmzCycle.Snapshot s = new NmzCycle.Snapshot();
		s.vialFull = !scene.get(Nmz.VIAL_FULL).isEmpty();
		s.carried = doses(pack);
		for (Nmz.Potion p : Nmz.Potion.values())
		{
			if (p.stockVarbit() >= 0)
			{
				s.barrel[p.ordinal()] = Math.max(0, Math.min(Nmz.BARREL_MAX, client.getVarbitValue(p.stockVarbit())));
			}
		}
		s.habit = g.habit();
		s.rewardPoints = reward;
		s.coffer = dreams().coffer();
		NmzLog.Dream paid = lastPaid(g);
		if (paid != null)
		{
			s.usualPrice = paid.paid;
			s.usualKind = paid.kind();
		}
		else if (g.last() != null && g.last().mode != Nmz.Mode.UNKNOWN)
		{
			s.usualKind = g.last().kind();
		}
		s.free = pack.free();
		return s;
	}

	private static NmzLog.Dream lastPaid(NmzLog g)
	{
		List<NmzLog.Dream> all = g.all();
		for (int i = all.size() - 1; i >= 0; i--)
		{
			if (all.get(i).paid > 0)
			{
				return all.get(i);
			}
		}
		return null;
	}

	private void target(Client client, Flight out, Step step, SceneIndex scene, Collection<NPC> npcs)
	{
		Collection<? extends TileObject> things;
		switch (step.getTarget())
		{
			case DOMINIC:
				for (NPC n : npcs)
				{
					if (n.getId() == Nmz.DOMINIC)
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
			case DREAM_VIAL:
				things = scene.get(Nmz.VIAL_FULL);
				break;
			case REWARDS_CHEST:
				things = scene.get(Nmz.CHEST);
				break;
			case DREAM_COFFER:
				things = scene.get(Nmz.COFFER);
				break;
			case POTION_BARREL:
				things = scene.get(barrelFor(step));
				break;
			default:
				return;
		}
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

	private static int barrelFor(Step step)
	{
		String t = step.getTitle().toLowerCase(Locale.ROOT);
		for (Nmz.Potion p : Nmz.Potion.values())
		{
			if (p.nightmare() && t.contains(p.getTitle().toLowerCase(Locale.ROOT)))
			{
				return p.barrel();
			}
		}
		return -1;
	}

	/** In a dream: absorption and overload left as figures, the bosses seen and the doses drunk. */
	private void dreamView(View view, Client client, NmzDreams d, int tick)
	{
		int absorb = client.getVarbitValue(Nmz.ABSORPTION);
		if (absorb > 0 || d.used(Nmz.Potion.ABSORPTION) > 0)
		{
			view.gauge(new View.Gauge("Absorption", Math.max(0, Math.min(Nmz.ABSORB_MAX, absorb)), 0, Nmz.ABSORB_MAX,
				"Each dose 50, up to 1,000", Theme.TEAL));
		}
		int refreshes = client.getVarbitValue(Nmz.OVERLOAD);
		if (refreshes > 0)
		{
			int seconds = Math.min(Nmz.OVERLOAD_SECONDS, refreshes * Nmz.OVERLOAD_REFRESH_TICKS * 6 / 10);
			view.gauge(new View.Gauge("Overload", seconds, 0, Nmz.OVERLOAD_SECONDS, "About " + seconds / 60 + ":" + String.format(Locale.ROOT, "%02d", seconds % 60) + " left",
				Theme.GOLD));
		}
		List<View.Line> lines = new ArrayList<>();
		NmzKills k = kills();
		char diff = d.difficulty();
		for (String b : d.bosses())
		{
			NmzKills.Boss learnt = diff == '?' ? null : k.get(b, diff);
			lines.add(new View.Line(b, learnt == null ? "Not learnt yet" : Session.format(Math.round(learnt.average())) + " a kill (" + learnt.kills + ")",
				learnt == null ? View.Line.Mark.IDLE : View.Line.Mark.DONE));
		}
		for (Nmz.Potion p : Nmz.Potion.values())
		{
			int n = d.used(p);
			if (n > 0)
			{
				lines.add(new View.Line(p.getTitle(), n + (n == 1 ? " dose" : " doses") + (p.nightmare() ? " · " + Session.format((long) n * p.points()) + " pts" : ""),
					View.Line.Mark.IDLE));
			}
		}
		if (!lines.isEmpty())
		{
			view.lines("This dream", StormPilot.clock(d.ticks(tick)), lines);
		}
	}

	/** In the lobby: the potions you drink a dream against what you carry and what is stored, and the coffer. */
	private void lobbyView(View view, NmzCycle.Snapshot s)
	{
		List<View.Line> lines = new ArrayList<>();
		for (Nmz.Potion p : Nmz.Potion.values())
		{
			int want = (int) Math.ceil(s.habit[p.ordinal()]);
			int have = s.carried[p.ordinal()];
			if (want <= 0 && have <= 0)
			{
				continue;
			}
			int stored = s.barrel[p.ordinal()];
			String trailing = have + (want > 0 ? "/" + want : "") + " carried" + (stored >= 0 ? " · " + stored + " stored" : p.nightmare() ? "" : " · bank");
			boolean ok = want <= 0 || have >= want;
			lines.add(new View.Line(p.getTitle(), trailing, ok ? View.Line.Mark.DONE : p.nightmare() ? View.Line.Mark.NEXT : View.Line.Mark.TODO));
		}
		if (s.coffer >= 0)
		{
			boolean low = s.usualPrice > 0 && s.coffer < s.usualPrice;
			lines.add(new View.Line("Dominic's coffer", Session.format(s.coffer * 1000L) + " coins", low ? View.Line.Mark.NEXT : View.Line.Mark.DONE));
		}
		if (!lines.isEmpty())
		{
			view.lines("Ready for the dream", s.usualKind, lines);
		}
	}

	private void stats(View view, NmzLog g, NmzDreams d, int tick, long now)
	{
		if (inArena)
		{
			int t = d.ticks(tick);
			view.stat(new View.Stat(Session.format(d.gained()), "Dream points", StormPilot.clock(t), Theme.GOLD_BRIGHT));
			if (t >= 100)
			{
				view.stat(new View.Stat(Session.format(Math.round(d.gained() * 3600.0 / (t * 0.6))), "Dream pace", "Points an hour", Theme.TEAL));
			}
		}
		view.stat(new View.Stat(Session.format(held()), "Reward points", inArena && reward <= rewardAtStart ? "With this dream's" : "At the chest", Theme.GOLD));
		double pph = g.pointsPerHour();
		if (pph >= 0)
		{
			view.stat(new View.Stat(Session.format(Math.round(pph)), "Points an hour", "Over " + g.count() + (g.count() == 1 ? " dream" : " dreams"), Theme.GOLD));
			double net = g.netPointsPerHour();
			view.stat(new View.Stat(Session.format(Math.round(net)), "After potions", "Potion points off", net < 0 ? Theme.BLOCKED : Theme.GREEN));
		}
		int done = dreams().dreams() - dreamsBase;
		if (done > 0)
		{
			view.stat(new View.Stat(Integer.toString(done), "Dreams today", Session.format(sessionPoints) + " points", Theme.EMBER));
		}
	}

	/** The reward you are after: points held against its price, and the dreams and time to it at your pace. */
	View.GoalCard goalCard(NmzLog g, boolean ironman)
	{
		long have = held();
		int cost = goal.cost(halved);
		List<View.Line> pieces = new ArrayList<>();
		StringBuilder b = new StringBuilder();
		boolean box = goal == Nmz.Reward.HERB_BOX;
		if (box)
		{
			double worth = Nmz.herbBoxValue(price);
			long afford = have / cost;
			if (ironman)
			{
				b.append("Ironmen cannot buy herb boxes here; the Tithe Farm sells them. ");
			}
			else if (afford > 0)
			{
				b.append(Session.format(afford)).append(afford == 1 ? " box" : " boxes").append(" affordable now, ").append(Nmz.HERB_BOX_DAILY).append(" a day at most. ");
			}
			if (worth > 0)
			{
				b.append("Each is seven grimy herbs, about ").append(Session.format(Math.round(worth))).append(" coins at today's prices");
				b.append(String.format(Locale.ROOT, " (%.2f a point)", worth / cost)).append(". ");
				pieces.add(new View.Line("A box's herbs", "About " + Session.format(Math.round(worth)) + " coins", View.Line.Mark.IDLE));
				if (afford > 0 && !ironman)
				{
					long n = Math.min(afford, Nmz.HERB_BOX_DAILY);
					pieces.add(new View.Line("Today's " + n + (n == 1 ? " box" : " boxes"), "About " + Session.format(Math.round(worth * n)) + " coins", View.Line.Mark.DONE));
				}
			}
		}
		boolean done = !box && have >= cost;
		long lacking = Math.max(0, cost - (box ? have % cost : have));
		if (done)
		{
			b.append("You have the points: imbue it at the Rewards chest.");
		}
		else
		{
			double pph = g.pointsPerHour();
			double net = g.netPointsPerHour();
			b.append(Session.format(lacking)).append(" points to ").append(box ? "the next box" : "go");
			if (pph > 0)
			{
				double rate = net > 0 ? net : pph;
				b.append(": ").append(MixologyPilot.clockHours(lacking / rate)).append(" of dreams at your pace").append(net > 0 ? ", your potions paid for" : "");
				NmzLog.Pace bestSet = bestSet(g);
				if (bestSet != null && bestSet.perHour() > pph * 1.05)
				{
					b.append("; ").append(MixologyPilot.clockHours(lacking / (bestSet.netPerHour() > 0 ? bestSet.netPerHour() : bestSet.perHour())))
						.append(" with your best set");
				}
				int perDream = averagePoints(g);
				if (perDream > 0)
				{
					long n = (lacking + perDream - 1) / perDream;
					b.append(" (about ").append(Session.format(n)).append(n == 1 ? " dream" : " dreams").append(')');
				}
				b.append('.');
			}
			else
			{
				b.append(". Dream once and the time to it follows from your pace.");
			}
		}
		if (goal.imbue())
		{
			pieces.add(new View.Line("Price", Session.format(cost) + " points" + (halved ? ", half with the Hard Combat Achievements" : ""), View.Line.Mark.IDLE));
		}
		View.GoalCard card = new View.GoalCard(View.Activity.NIGHTMARE_ZONE, box ? "Herb boxes" : goal.getTitle(), box ? have % cost : Math.min(have, cost), cost, "points",
			b.toString().trim(), pieces, done);
		card.aside(box ? "9,500 points a box at the Rewards chest" : "Imbued at the Rewards chest");
		return card;
	}

	private static int averagePoints(NmzLog g)
	{
		long p = 0;
		int n = 0;
		for (NmzLog.Dream d : g.all())
		{
			if (d.points > 0)
			{
				p += d.points;
				n++;
			}
		}
		return n == 0 ? 0 : (int) (p / n);
	}

	/** Your fastest set of bosses with at least two dreams, or null. */
	static NmzLog.Pace bestSet(NmzLog g)
	{
		for (NmzLog.Pace p : g.bySet())
		{
			if (p.dreams >= 2)
			{
				return p;
			}
		}
		return null;
	}

	/** Points an hour for each set of bosses you have dreamt, best first. */
	private View.PickCard setsCard(NmzLog g)
	{
		List<NmzLog.Pace> sets = g.bySet();
		if (sets.isEmpty())
		{
			return null;
		}
		double top = Math.max(1, sets.get(0).perHour());
		NmzLog.Dream last = g.last();
		String lastSet = last == null ? null : last.set();
		List<View.Pick> picks = new ArrayList<>();
		for (int i = 0; i < sets.size() && i < 5; i++)
		{
			NmzLog.Pace p = sets.get(i);
			String bosses = p.bosses.isEmpty() ? "Bosses not seen" : p.bosses.size() <= 2 ? String.join(", ", p.bosses)
				: p.bosses.get(0) + ", " + p.bosses.get(1) + " +" + (p.bosses.size() - 2);
			String detail = bosses + " · " + p.dreams + (p.dreams == 1 ? " dream" : " dreams") + " · " + Session.format(Math.round(p.netPerHour())) + " after potions";
			picks.add(new View.Pick(p.kind, Session.format(Math.round(p.perHour())) + "/h", detail, true, p.set.equals(lastSet), p.perHour() / top,
				p.netPerHour() < 0));
		}
		return new View.PickCard("Your sets", sets.size() + (sets.size() == 1 ? " set" : " sets"),
			"Points an hour from your own dreams, by the bosses in them. The best set depends on your stats and gear, so only yours are ranked.", picks);
	}

	/** Your last dreams, the best, and your pace across them. */
	private static View.Supply historyCard(NmzLog g)
	{
		List<View.Line> lines = new ArrayList<>();
		List<NmzLog.Dream> all = g.all();
		NmzLog.Dream best = g.best();
		for (int i = all.size() - 1; i >= 0 && i >= all.size() - 5; i--)
		{
			NmzLog.Dream x = all.get(i);
			String what = x.kind() + " · " + Session.format(x.points) + " pts" + (x.ticks > 0 ? " · " + clockLong(x.ticks) : "");
			String trailing = x.potionsLine().isEmpty() ? (x.end == NmzLog.End.LOGOUT ? "Logged out" : "") : shortPotions(x);
			lines.add(new View.Line(what, trailing, x == best ? View.Line.Mark.DONE : View.Line.Mark.IDLE));
		}
		String headline = best == null ? null : "Best dream: " + Session.format(best.points) + " points";
		double h = g.pointsPerHour();
		String detail = h < 0 ? null : Session.format(Math.round(h)) + " points an hour over your dreams, " + Session.format(Math.round(g.netPointsPerHour()))
			+ " once the points your overloads, absorptions and boosts cost are taken off";
		return new View.Supply(g.count() == 1 ? "1 kept" : g.count() + " kept", lines, headline, detail, false, null).titled("Your dreams");
	}

	/** "6 ovl · 22 abs". */
	static String shortPotions(NmzLog.Dream d)
	{
		StringBuilder b = new StringBuilder();
		for (Nmz.Potion p : Nmz.Potion.values())
		{
			int n = d.doses(p);
			if (n > 0)
			{
				b.append(b.length() == 0 ? "" : " · ").append(n).append(' ').append(p.brief());
			}
		}
		return b.toString();
	}

	/** "1:02:41", or "41:12" under the hour. */
	static String clockLong(int ticks)
	{
		int s = ticks * 6 / 10;
		return s >= 3600 ? s / 3600 + ":" + String.format(Locale.ROOT, "%02d:%02d", s / 60 % 60, s % 60) : StormPilot.clock(ticks);
	}

	/** Brief for the panel away from the Nightmare Zone. */
	View.Brief brief()
	{
		NmzLog g = log();
		NmzLog.Dream last = g.last();
		String status = last == null ? null : "Last dream: " + last.kind().toLowerCase(Locale.ROOT) + ", " + Session.format(last.points) + " points"
			+ (last.ticks > 0 ? " in " + clockLong(last.ticks) : "");
		View.Brief b = new View.Brief(View.Activity.NIGHTMARE_ZONE, status,
			"North of Yanille, after five quests. Fill Dominic's coffer and ask him for a dream" + (goal == null ? "." : "; saving for " + (goal == Nmz.Reward.HERB_BOX ? "herb boxes." : "the " + goal.getTitle() + ".")));
		double h = g.pointsPerHour();
		if (h >= 0)
		{
			b.best(Session.format(Math.round(h)) + " points an hour over " + g.count() + (g.count() == 1 ? " dream." : " dreams."));
		}
		return b;
	}
}
