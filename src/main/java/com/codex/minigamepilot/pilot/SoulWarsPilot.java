package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Goals;
import com.codex.minigamepilot.core.SoulWars;
import com.codex.minigamepilot.core.SoulWarsGames;
import com.codex.minigamepilot.core.SoulWarsLog;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;

/**
 * Soul Wars: every game kept per account (your team, won, lost or drawn, the zeal it earned by the
 * game's own count of your tokens, your part as the end-of-game scroll gives it, and its length),
 * your zeal an hour at your own pace, the time to the reward you choose from Nomad's shop (Lil'
 * Creator, Spoils of War, the Soul cape, an imbue, or a level bought with zeal), and the
 * experience your zeal buys at your levels.
 *
 * <p>Information only: nothing is lit in the world, no plaque is drawn in the game view, and in a
 * game the panel shows only your team, the game's clock and your tokens. No fight cue of any kind.
 */
public final class SoulWarsPilot
{
	static final String LOG_KEY = "soulwars.kept";
	static final String OWNED_KEY = "soulwars.owned";
	/** Nomad's camp: within this many tiles of it on the ground floor. */
	static final int CAMP_REACH = 32;

	private Function<String, String> load;
	private BiConsumer<String, String> save;
	private SoulWarsLog log;
	private SoulWarsGames games;
	private String owned;
	private SoulWars.Reward goal = SoulWars.Reward.LIL_CREATOR;
	private SoulWars.ZealSkill skill = SoulWars.ZealSkill.ATTACK;
	private int level;
	private int spend;
	private boolean halfImbue;

	// Read each tick.
	private boolean inGame;
	private boolean inCamp;
	private boolean over;

	// This session.
	private int gamesBase;
	private long sessionZeal;

	Object lastState;

	void setStore(Function<String, String> load, BiConsumer<String, String> save)
	{
		this.load = load;
		this.save = save;
	}

	/**
	 * @param reward the reward saved for, or null for none
	 * @param skill the skill zeal's experience is shown for
	 * @param level the level to buy to with zeal, 0 for the next
	 * @param spend the Zeal Tokens to price in experience, 0 for those you hold
	 * @param halfImbue the hard Combat Achievements rewards are claimed, so imbues cost half
	 */
	void settings(SoulWars.Reward reward, SoulWars.ZealSkill skill, int level, int spend, boolean halfImbue)
	{
		this.goal = reward;
		this.skill = skill == null ? SoulWars.ZealSkill.ATTACK : skill;
		this.level = Math.max(0, Math.min(99, level));
		this.spend = Math.max(0, spend);
		this.halfImbue = halfImbue;
	}

	void newAccount()
	{
		log = null;
		games = null;
		owned = null;
		gamesBase = 0;
		sessionZeal = 0;
	}

	SoulWarsLog log()
	{
		if (log == null)
		{
			log = SoulWarsLog.decode(load == null ? null : load.apply(LOG_KEY));
		}
		return log;
	}

	private SoulWarsGames games()
	{
		if (games == null)
		{
			games = new SoulWarsGames();
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
		return SoulWars.number(text(client, component));
	}

	/** Every tick, wherever you are: so a game is kept as it ends or you log out. */
	void watch(Client client, long now)
	{
		WorldPoint at = client.getLocalPlayer() == null ? null
			: WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation());
		inGame = shown(client, SoulWars.HUD) || (at != null && SoulWars.inArena(at.getRegionID()));
		inCamp = shown(client, SoulWars.LOBBY) || (at != null && !inGame && at.getPlane() == 0
			&& Math.abs(at.getX() - SoulWars.CAMP_X) <= CAMP_REACH && Math.abs(at.getY() - SoulWars.CAMP_Y) <= CAMP_REACH);
		over = shown(client, SoulWars.OVER);
		SoulWarsGames g = games();
		if (!inGame && !inCamp && !over && !g.open() && !g.settling())
		{
			return;
		}
		learnOwned(Pack.read(client));
		keep(g.tick(client.getTickCount(), reading(client), now));
	}

	private SoulWarsGames.Reading reading(Client client)
	{
		SoulWarsGames.Reading r = new SoulWarsGames.Reading();
		r.inGame = inGame;
		r.over = over;
		r.team = team(client);
		if (over)
		{
			r.overBlue = number(client, SoulWars.OVER_BLUE);
			r.overRed = number(client, SoulWars.OVER_RED);
			r.overZeal = number(client, SoulWars.OVER_ZEAL);
			r.part.kills = number(client, SoulWars.OVER_KILLS);
			r.part.captures = number(client, SoulWars.OVER_CAPTURES);
			r.part.damage = number(client, SoulWars.OVER_DAMAGE);
			r.part.fragments = number(client, SoulWars.OVER_FRAGMENTS);
			r.part.bones = number(client, SoulWars.OVER_BONES);
		}
		r.tokens = tokens(client);
		r.wins = varp(client, SoulWars.TOTAL_WINS);
		r.played = varp(client, SoulWars.TOTAL_GAMES);
		return r;
	}

	private static long varp(Client client, int id)
	{
		int v = client.getVarpValue(id);
		return v < 0 || v > SoulWars.TOKENS_CAP ? -1 : v;
	}

	/** Zeal Tokens held, the game's own count, or -1. */
	static long tokens(Client client)
	{
		return varp(client, SoulWars.TOKENS);
	}

	/** Your team: the cape you wear, else the team varbit; or null. */
	static SoulWars.Team team(Client client)
	{
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		Item cape = worn == null ? null : worn.getItem(EquipmentInventorySlot.CAPE.getSlotIdx());
		SoulWars.Team t = cape == null ? null : SoulWars.Team.byCape(cape.getId());
		return t != null ? t : SoulWars.Team.byVarbit(client.getVarbitValue(SoulWars.TEAM));
	}

	private void learnOwned(Pack pack)
	{
		String o = owned();
		StringBuilder b = new StringBuilder(o);
		for (SoulWars.Reward r : SoulWars.Reward.values())
		{
			int[] ids = r.items();
			if (ids.length > 0 && (pack.carries(ids) || pack.wears(ids) || pack.banked(ids) > 0) && b.indexOf(String.valueOf(r.code())) < 0)
			{
				b.append(r.code());
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
			for (SoulWars.Reward r : SoulWars.Reward.values())
			{
				if (r.code() == c && r.items().length > 0 && b.indexOf(String.valueOf(c)) < 0)
				{
					b.append(c);
				}
			}
		}
		return b.toString();
	}

	boolean owns(SoulWars.Reward r)
	{
		return r.items().length > 0 && owned().indexOf(r.code()) >= 0;
	}

	private void keep(SoulWarsLog.Game g)
	{
		if (g == null)
		{
			return;
		}
		log().add(g);
		sessionZeal += g.zeal;
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
			SoulWarsGames.Reading r = new SoulWarsGames.Reading();
			r.tokens = tokens(client);
			r.wins = varp(client, SoulWars.TOTAL_WINS);
			r.played = varp(client, SoulWars.TOTAL_GAMES);
			keep(games.logout(tick, r, now));
		}
		inGame = false;
		over = false;
	}

	/** In a game, on the scroll, or in Nomad's camp. */
	boolean active(Client client)
	{
		return client.getLocalPlayer() != null && (inGame || inCamp || over || games().settling());
	}

	/** The pace coach's count: none, as a game is no loop to hurry. */
	int units()
	{
		return 0;
	}

	void left()
	{
	}

	/** The one plain line: a requirement not met, the game you are in, the reward to buy, else to join a game. */
	static Step step(int total, int combat, boolean inGame, SoulWars.Team team, SoulWars.Reward goal, int cost, boolean owned, long tokens)
	{
		if (total > 0 && total < SoulWars.TOTAL_LEVEL)
		{
			return Step.of("Level", "Soul Wars needs a total level of " + Session.format(SoulWars.TOTAL_LEVEL),
				"Yours is " + Session.format(total) + "; it also needs combat level " + SoulWars.COMBAT_LEVEL, Step.Target.NONE);
		}
		if (combat > 0 && combat < SoulWars.COMBAT_LEVEL)
		{
			return Step.of("Level", "Soul Wars needs combat level " + SoulWars.COMBAT_LEVEL, "Yours is " + combat, Step.Target.NONE);
		}
		if (inGame)
		{
			return Step.of("Game", team == null ? "In a game" : "In a game for the " + team.getTitle().toLowerCase(Locale.ROOT) + " team",
				"Your zeal is counted as it ends: up to 60 a win, 50 a draw, 40 a loss", Step.Target.NONE);
		}
		boolean buyable = goal != null && goal != SoulWars.Reward.SKILL_LEVEL && goal != SoulWars.Reward.LIL_CREATOR && !owned;
		if (buyable && tokens >= cost && cost > 0)
		{
			return Step.of("Buy", "Buy the " + goal.getTitle() + " from Nomad",
				BrewingPilot.exact(tokens) + " Zeal Tokens held; it costs " + BrewingPilot.exact(cost), Step.Target.NONE);
		}
		return Step.of("Join", "Join a game of Soul Wars",
			"Up to 60 zeal a win, 50 a draw, 40 a loss, if you keep your activity bar up", Step.Target.NONE);
	}

	Flight fly(Client client, long now)
	{
		int tick = client.getTickCount();
		SoulWarsGames g = games();
		SoulWarsLog l = log();
		long tokens = tokens(client);
		int combat = client.getLocalPlayer() == null ? 0 : client.getLocalPlayer().getCombatLevel();
		SoulWars.Team team = inGame ? (g.team() != null ? g.team() : team(client)) : null;
		int cost = goal == null ? 0 : goal.cost(halfImbue);
		Step step = step(totalLevel(client), combat, inGame, team, goal, cost, goal != null && owns(goal), tokens);
		lastState = step.getTitle();
		String place = inGame ? (team == null ? "in a game" : team.getTitle().toLowerCase(Locale.ROOT) + " team") : over ? "the game's end" : "Nomad's camp";
		View view = new View(true, View.Activity.SOUL_WARS, step, "Soul Wars · " + place, null, -1);
		Flight out = new Flight(View.Activity.SOUL_WARS, step, view);
		// Panel only: no plaque, nothing lit, no fight cue.
		out.quiet = true;
		stats(view, client, l, g, tokens, tick);
		double share = l.share();
		if (share >= 0)
		{
			int pct = (int) Math.round(share * 100);
			view.gauge(new View.Gauge("Zeal of the most possible", pct, 0, 100, pct + "% of 60 a win, 50 a draw, 40 a loss", Theme.GOLD));
		}
		if (goal != null)
		{
			view.goal(goalCard(client, l, tokens));
		}
		if (!inGame)
		{
			buysView(view, client, tokens);
			View.PickCard results = resultsCard(l);
			if (results != null)
			{
				view.pick(results);
			}
		}
		if (l.count() > 0)
		{
			view.supply(historyCard(l));
		}
		return out;
	}

	private void stats(View view, Client client, SoulWarsLog l, SoulWarsGames g, long tokens, int tick)
	{
		if (inGame)
		{
			String time = SoulWars.plain(text(client, SoulWars.HUD_TIME));
			view.stat(new View.Stat(time.isEmpty() ? StormPilot.clock(g.ticks(tick)) : time, time.isEmpty() ? "In the game" : "Time left",
				time.isEmpty() ? "Since it began" : "Game's clock", Theme.TEXT));
		}
		view.stat(new View.Stat(tokens < 0 ? "?" : BrewingPilot.exact(tokens), "Zeal Tokens", "Held", Theme.GOLD_BRIGHT));
		double pace = l.pace();
		if (pace >= 0)
		{
			boolean sit = l.zealPerHourSitting() > 0;
			view.stat(new View.Stat(Session.format(Math.round(pace)), "Zeal an hour", sit ? "With the waits" : "In the game", Theme.GOLD));
		}
		if (!inGame && l.finished() > 0)
		{
			view.stat(new View.Stat(String.format(Locale.ROOT, "%.0f", l.averageZeal()), "Zeal a game", l.decided() == 0 ? "Played to the end"
				: "Won " + l.count(SoulWarsLog.Result.WON) + " of " + l.decided(), Theme.TEAL));
		}
		int done = g.games() - gamesBase;
		if (done > 0)
		{
			view.stat(new View.Stat(Integer.toString(done), "Games today", Session.format(sessionZeal) + " zeal", Theme.EMBER));
		}
	}

	/** Your total level, from your real levels. */
	static int totalLevel(Client client)
	{
		int n = 0;
		for (net.runelite.api.Skill s : net.runelite.api.Skill.values())
		{
			n += client.getRealSkillLevel(s);
		}
		return n;
	}

	/** Your pace, in hours, to {@code left} zeal; -1 when there is no pace yet. */
	private static double hours(SoulWarsLog l, long left)
	{
		double rate = l.pace();
		return rate > 0 ? left / rate : -1;
	}

	/** " about N games, Xh Ym at your pace." or a line on how the time will follow. */
	private static String eta(SoulWarsLog l, long left)
	{
		double h = hours(l, left);
		if (h < 0)
		{
			return ". Play a game and the time to it follows from your own pace.";
		}
		StringBuilder b = new StringBuilder(": ");
		double per = l.averageZeal();
		if (per > 0)
		{
			long n = (long) Math.ceil(left / per);
			b.append("about ").append(Session.format(n)).append(n == 1 ? " game, " : " games, ");
		}
		b.append(MixologyPilot.clockHours(h)).append(" at your pace").append(l.zealPerHourSitting() > 0 ? ", waits counted." : " in the game.");
		return b.toString();
	}

	/** The reward you chose: Zeal Tokens held against its price, and the games and time to it at your pace. */
	View.GoalCard goalCard(Client client, SoulWarsLog l, long tokens)
	{
		long have = Math.max(0, tokens);
		if (goal == SoulWars.Reward.SKILL_LEVEL)
		{
			return levelCard(client, l, tokens);
		}
		if (goal == SoulWars.Reward.LIL_CREATOR)
		{
			return petCard(l, tokens);
		}
		int cost = goal.cost(halfImbue);
		boolean done = owns(goal);
		StringBuilder b = new StringBuilder();
		long toward;
		if (done)
		{
			b.append("It is yours: choose another under Soul Wars reward.");
			toward = cost;
		}
		else if (tokens < 0)
		{
			b.append("Your Zeal Tokens are read once you are logged in.");
			toward = 0;
		}
		else if (goal.repeat())
		{
			toward = have % cost;
			if (have >= cost)
			{
				b.append(Session.format(have / cost)).append(" affordable now. ");
			}
			long left = cost - have % cost;
			b.append(BrewingPilot.exact(left)).append(" zeal to the next").append(eta(l, left));
		}
		else
		{
			toward = Math.min(have, cost);
			long left = cost - have;
			if (left <= 0)
			{
				b.append("You hold the tokens: buy it from Nomad.");
			}
			else
			{
				b.append(BrewingPilot.exact(left)).append(" zeal to go").append(eta(l, left));
			}
		}
		View.GoalCard card = new View.GoalCard(View.Activity.SOUL_WARS, goal.getTitle(), toward, cost, "Zeal Tokens", b.toString().trim(), null, done);
		String which = goal == SoulWars.Reward.SLAYER_HELMET ? "Or a black mask. " : goal == SoulWars.Reward.RING
			? "Berserker, warrior, archers, seers, tyrannical, treasonous or the gods. " : goal == SoulWars.Reward.SALVE ? "Or the salve amulet (e). " : "";
		card.aside(goal.imbue() ? which + (halfImbue ? "Half price: hard Combat Achievements rewards claimed" : "Half price once the hard Combat Achievements rewards are claimed")
			: goal == SoulWars.Reward.SPOILS ? "30 Zeal Tokens a Spoils of War, from Nomad" : "At Nomad's Soul Wars Reward Shop");
		return card;
	}

	/** Lil' Creator: not sold, a 1/400 roll on each Spoils of War, so 12,000 zeal on average, with your chance in what you hold. */
	private View.GoalCard petCard(SoulWarsLog l, long tokens)
	{
		long have = Math.max(0, tokens);
		long average = (long) SoulWars.SPOILS_COST * SoulWars.PET_RATE;
		boolean done = owns(SoulWars.Reward.LIL_CREATOR);
		StringBuilder b = new StringBuilder();
		if (done)
		{
			b.append("It is yours: choose another under Soul Wars reward.");
		}
		else if (tokens < 0)
		{
			b.append("Your Zeal Tokens are read once you are logged in.");
		}
		else
		{
			long spoils = have / SoulWars.SPOILS_COST;
			b.append("A 1/400 roll on each Spoils of War, 30 zeal each. ");
			if (spoils > 0)
			{
				b.append("Your tokens buy ").append(BrewingPilot.exact(spoils)).append(spoils == 1 ? " spoils" : " spoils")
					.append(", a ").append(percent(SoulWars.petChance(spoils))).append(" chance. ");
			}
			long half = SoulWars.spoilsFor(0.5);
			long left = Math.max(0, average - have);
			b.append("Even odds take ").append(half).append(" spoils (").append(BrewingPilot.exact(half * SoulWars.SPOILS_COST)).append(" zeal); ");
			if (left > 0)
			{
				b.append("the average 400 is ").append(BrewingPilot.exact(left)).append(" zeal away").append(eta(l, left));
			}
			else
			{
				b.append("you hold the average 400's worth.");
			}
		}
		View.GoalCard card = new View.GoalCard(View.Activity.SOUL_WARS, "Lil' Creator", done ? average : Math.min(have, average), average,
			"Zeal Tokens", b.toString().trim(), null, done);
		card.aside("A chance, not a price: 1/400 a Spoils of War");
		return card;
	}

	/** A level bought with zeal, a level at a time at each level's rate. */
	private View.GoalCard levelCard(Client client, SoulWarsLog l, long tokens)
	{
		long xp = client.getSkillExperience(skill.skill());
		int now = Goals.levelFor(xp);
		int t = level <= 0 ? Math.min(99, now + 1) : level;
		long targetXp = Goals.xpFor(t);
		long need = SoulWars.tokensTo(skill, xp, targetXp);
		String title = skill.getTitle() + " " + t + " from zeal";
		StringBuilder b = new StringBuilder();
		boolean done = xp >= targetXp;
		long have = Math.max(0, tokens);
		if (done)
		{
			b.append("Reached: set another under Zeal level.");
			need = Math.max(1, need);
		}
		else if (need < 0)
		{
			b.append("Zeal buys ").append(skill.getTitle()).append(" experience from level ").append(SoulWars.XP_LEVEL).append("; yours is ").append(now).append('.');
			need = 1;
			have = 0;
		}
		else
		{
			long left = Math.max(0, need - have);
			if (left == 0)
			{
				b.append("You hold the tokens: buy it from Nomad, a level at a time.");
			}
			else
			{
				b.append(BrewingPilot.exact(left)).append(" zeal to go").append(eta(l, left));
			}
			if (targetXp - xp > SoulWars.XP_DAILY_CAP)
			{
				b.append(" More than a day's 1,000,000 xp: zeal buys it over several days.");
			}
		}
		View.GoalCard card = new View.GoalCard(View.Activity.SOUL_WARS, title, Math.min(have, need), need, "Zeal Tokens", b.toString().trim(), null, done);
		card.aside(BrewingPilot.exact(skill.xpPerToken(now)) + " xp a token at " + now + ", bought a level at a time");
		return card;
	}

	/** What zeal buys at your levels: each skill's experience a token, and for the amount you chose. */
	private void buysView(View view, Client client, long tokens)
	{
		long amount = spend > 0 ? spend : Math.max(0, tokens);
		List<View.Line> lines = new ArrayList<>();
		boolean capped = false;
		for (SoulWars.ZealSkill s : SoulWars.ZealSkill.values())
		{
			int lvl = client.getRealSkillLevel(s.skill());
			int per = s.xpPerToken(lvl);
			capped |= (long) per * amount > SoulWars.XP_DAILY_CAP;
			String trailing = per <= 0 ? "From " + SoulWars.XP_LEVEL
				: amount > 0 ? Session.format(Math.min((long) per * amount, SoulWars.XP_DAILY_CAP)) + " xp" : per + " a token";
			String text = s.getTitle() + " " + lvl + (per > 0 ? " · " + per + " a token" : "");
			lines.add(new View.Line(text, trailing, s == skill ? View.Line.Mark.NEXT : per > 0 ? View.Line.Mark.IDLE : View.Line.Mark.TODO));
		}
		String note = amount > 0 ? BrewingPilot.exact(amount) + (spend > 0 ? " chosen" : " held") + (capped ? " · 1M a day" : "")
			: "A token at your levels";
		view.lines("What zeal buys", note, lines);
	}

	/** Zeal by how your games turned out: from your own games only. */
	private static View.PickCard resultsCard(SoulWarsLog l)
	{
		if (l.decided() < 2)
		{
			return null;
		}
		List<View.Pick> picks = new ArrayList<>();
		SoulWarsLog.Game last = l.last();
		double top = SoulWars.WIN_MAX;
		SoulWarsLog.Result[] order = {SoulWarsLog.Result.WON, SoulWarsLog.Result.DRAWN, SoulWarsLog.Result.LOST};
		int[] most = {SoulWars.WIN_MAX, SoulWars.DRAW_MAX, SoulWars.LOSS_MAX};
		for (int i = 0; i < order.length; i++)
		{
			SoulWarsLog.Result r = order[i];
			int n = l.count(r);
			if (n == 0)
			{
				continue;
			}
			double avg = Math.max(0, l.averageZeal(r));
			picks.add(new View.Pick(r.getTitle(), String.format(Locale.ROOT, "%.0f", avg),
				n + (n == 1 ? " game" : " games") + " · up to " + most[i], true, last != null && last.result == r, avg / top, false));
		}
		int won = l.count(SoulWarsLog.Result.WON);
		return new View.PickCard("By result", percent(won / (double) l.decided()) + " won",
			"Average zeal a game by how it turned out, from your own games.", picks);
	}

	/** Your last games, the best, and your pace across them. */
	private static View.Supply historyCard(SoulWarsLog l)
	{
		List<View.Line> lines = new ArrayList<>();
		List<SoulWarsLog.Game> all = l.all();
		SoulWarsLog.Game best = l.best();
		for (int i = all.size() - 1; i >= 0 && i >= all.size() - 5; i--)
		{
			SoulWarsLog.Game x = all.get(i);
			StringBuilder what = new StringBuilder();
			what.append(x.zeal).append(" zeal");
			if (!x.score().isEmpty())
			{
				what.append(" · ").append(x.score());
			}
			if (x.ticks > 0)
			{
				what.append(" · ").append(StormPilot.clock(x.ticks));
			}
			String trailing = x.end != SoulWarsLog.End.FINISHED ? x.end.getTitle() : !x.result.getTitle().isEmpty() ? x.result.getTitle()
				: x.team == null ? "" : x.team.getTitle();
			lines.add(new View.Line(what.toString(), trailing, x == best ? View.Line.Mark.DONE
				: x.result == SoulWarsLog.Result.WON ? View.Line.Mark.IDLE : View.Line.Mark.TODO));
		}
		String headline = best == null ? null : "Best game: " + best.zeal + " zeal" + (best.result.getTitle().isEmpty() ? "" : ", " + best.result.getTitle().toLowerCase(Locale.ROOT));
		double h = l.zealPerHour();
		String detail = null;
		if (h >= 0)
		{
			detail = Session.format(Math.round(h)) + " zeal an hour in the game";
			double sit = l.zealPerHourSitting();
			if (sit > 0)
			{
				detail += "; " + Session.format(Math.round(sit)) + " with the waits between games";
			}
			String part = partLine(l);
			if (part != null)
			{
				detail += ". " + part;
			}
		}
		return new View.Supply(l.count() == 1 ? "1 kept" : l.count() + " kept", lines, headline, detail, false, null).titled("Your games");
	}

	/** Your part a game on average, as the scroll counts it, or null when never read. */
	static String partLine(SoulWarsLog l)
	{
		long frag = 0;
		long dmg = 0;
		long caps = 0;
		long bones = 0;
		int n = 0;
		for (SoulWarsLog.Game g : l.all())
		{
			if (g.part.fragments < 0 && g.part.damage < 0)
			{
				continue;
			}
			frag += Math.max(0, g.part.fragments);
			dmg += Math.max(0, g.part.damage);
			caps += Math.max(0, g.part.captures);
			bones += Math.max(0, g.part.bones);
			n++;
		}
		if (n == 0)
		{
			return null;
		}
		return String.format(Locale.ROOT, "A game on average: %d fragments, %d avatar damage, %d captures, %d bones", Math.round(frag / (double) n),
			Math.round(dmg / (double) n), Math.round(caps / (double) n), Math.round(bones / (double) n));
	}

	static String percent(double share)
	{
		double p = share * 100;
		return p > 0 && p < 1 ? String.format(Locale.ROOT, "%.1f%%", p) : Math.round(p) + "%";
	}

	/** Brief for the panel away from Soul Wars. */
	View.Brief brief()
	{
		SoulWarsLog l = log();
		SoulWarsLog.Game last = l.last();
		String status = last == null ? null : "Last game: " + last.zeal + " zeal" + (last.result.getTitle().isEmpty() ? "" : ", " + last.result.getTitle().toLowerCase(Locale.ROOT))
			+ (last.ticks > 0 ? " in " + StormPilot.clock(last.ticks) : "");
		View.Brief b = new View.Brief(View.Activity.SOUL_WARS, status,
			"Through the Soul Wars portal in Edgeville or the Ferox Enclave, from a total level of 500 and combat level 40"
				+ (goal == null ? "." : "; saving for " + (goal == SoulWars.Reward.SKILL_LEVEL ? "a " + skill.getTitle() + " level" : goal.getTitle()) + "."));
		double h = l.pace();
		if (h >= 0)
		{
			b.best(Session.format(Math.round(h)) + " zeal an hour over " + l.count() + (l.count() == 1 ? " game." : " games."));
		}
		return b;
	}
}
