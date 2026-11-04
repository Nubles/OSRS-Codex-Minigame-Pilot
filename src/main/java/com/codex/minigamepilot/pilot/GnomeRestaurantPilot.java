package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.GnomeLog;
import com.codex.minigamepilot.core.GnomeOrder;
import com.codex.minigamepilot.core.GnomeRestaurant;
import com.codex.minigamepilot.core.GnomeRestaurant.Customer;
import com.codex.minigamepilot.core.GnomeRestaurant.Dish;
import com.codex.minigamepilot.core.GnomeRestaurant.Ingredient;
import com.codex.minigamepilot.core.GnomeRestaurant.Stage;
import com.codex.minigamepilot.core.HomeWay;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.core.WayfinderAtlas;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

/**
 * Flies the Gnome Restaurant: the one next step of a delivery (the dish's recipe from where you
 * stand in it, with what you lack for every step left; then who to take it to and where, with
 * Codex Wayfinder's quickest way and the time left), every delivery kept per account with its
 * time, experience and tip, and every gnome dish as a checklist against your Cooking level and
 * what you have delivered.
 *
 * <p>It reads the game only: the order and customer varbits, the pack, the characters in sight,
 * and the game's own messages on handing over a delivery, its tip and your token's credits. Where
 * a customer stands is learnt from your own play (seen in sight, or where you handed over), or
 * taken from RuneLite's world map where a customer stands at one of its points (the gliders and
 * the restaurant). The wiki gives no coordinates for them.
 */
public final class GnomeRestaurantPilot
{
	static final String LOG_KEY = "gnome.kept";
	static final String ORDER_KEY = "gnome.order";
	static final String CREDITS_KEY = "gnome.credits";
	static final String PLACES_KEY = "gnome.places";
	/** The key for Gianne jnr.'s place among the customers'. */
	static final String GIANNE_PLACE = "GIANNE";
	/** Ticks after a hand-over to wait for its tip before the delivery is kept. */
	static final int TIP_TICKS = 2;
	/** An order already set on the first tick after login was taken before it (not timed here). */
	static final int LOGIN_TICKS = 1;

	private final Session session = new Session(Skill.COOKING);
	private Function<String, String> load;
	private BiConsumer<String, String> save;
	private IntUnaryOperator price = id -> 0;
	private Supplier<WayfinderAtlas> atlas = () -> null;
	private GnomeLog log;
	private Map<String, WorldPoint> places;
	private int credits = -2;

	// The order as the game keeps it.
	private int orderV;
	private int customerV;
	private long startMs = -1;
	private int startXp = -1;
	private boolean lateKept;
	/** The first tick seen since login, or -1. */
	private int firstTick = -1;
	private long lastEnd;

	// A hand-over waiting for its tip.
	private GnomeRestaurant.HandOver handOver;
	private int handTick;
	private int handOrder;
	private int handCustomer;
	private Map<Integer, Integer> before;
	private boolean tipped;
	private boolean discarded;
	private Map<Integer, Integer> lastHeld = new HashMap<>();
	private int tick;

	// This session.
	private int delivered;
	private long tipsGp;

	Object lastState;

	void setStore(Function<String, String> load, BiConsumer<String, String> save)
	{
		this.load = load;
		this.save = save;
	}

	void setPrice(IntUnaryOperator price)
	{
		this.price = price == null ? id -> 0 : price;
	}

	void setAtlas(Supplier<WayfinderAtlas> atlas)
	{
		this.atlas = atlas == null ? () -> null : atlas;
	}

	void newAccount()
	{
		session.startOver();
		log = null;
		places = null;
		credits = -2;
		orderV = 0;
		customerV = 0;
		startMs = -1;
		startXp = -1;
		lateKept = false;
		firstTick = -1;
		lastEnd = 0;
		handOver = null;
		delivered = 0;
		tipsGp = 0;
	}

	GnomeLog log()
	{
		if (log == null)
		{
			log = GnomeLog.decode(load == null ? null : load.apply(LOG_KEY));
		}
		return log;
	}

	/** Reward-token credits as the game last said, or -1 when not heard yet. */
	int credits()
	{
		if (credits == -2)
		{
			credits = -1;
			String s = load == null ? null : load.apply(CREDITS_KEY);
			if (s != null)
			{
				try
				{
					credits = Math.max(-1, Math.min(9999, Integer.parseInt(s.trim())));
				}
				catch (NumberFormatException e)
				{
					credits = -1;
				}
			}
		}
		return credits;
	}

	Map<String, WorldPoint> places()
	{
		if (places == null)
		{
			places = decodePlaces(load == null ? null : load.apply(PLACES_KEY));
		}
		return places;
	}

	/** "1;ROMETTI:2482:3510:1;GIANNE:2449:3501:1": where each customer was last seen. Damaged entries are left out. */
	static Map<String, WorldPoint> decodePlaces(String text)
	{
		Map<String, WorldPoint> out = new LinkedHashMap<>();
		if (text == null || !text.startsWith("1;"))
		{
			return out;
		}
		for (String part : text.substring(2).split(";"))
		{
			String[] f = part.split(":");
			if (f.length != 4)
			{
				continue;
			}
			String key = f[0].trim();
			if (!GIANNE_PLACE.equals(key))
			{
				try
				{
					Customer.valueOf(key);
				}
				catch (IllegalArgumentException e)
				{
					continue;
				}
			}
			try
			{
				int x = Integer.parseInt(f[1].trim());
				int y = Integer.parseInt(f[2].trim());
				int p = Integer.parseInt(f[3].trim());
				if (x > 0 && x < 16384 && y > 0 && y < 16384 && p >= 0 && p <= 3)
				{
					out.put(key, new WorldPoint(x, y, p));
				}
			}
			catch (NumberFormatException e)
			{
				// Left out.
			}
		}
		return out;
	}

	static String encodePlaces(Map<String, WorldPoint> places)
	{
		StringBuilder b = new StringBuilder("1");
		for (Map.Entry<String, WorldPoint> e : places.entrySet())
		{
			WorldPoint p = e.getValue();
			b.append(';').append(e.getKey()).append(':').append(p.getX()).append(':').append(p.getY()).append(':').append(p.getPlane());
		}
		return b.toString();
	}

	private void learn(String key, WorldPoint p)
	{
		if (key == null || p == null)
		{
			return;
		}
		WorldPoint old = places().get(key);
		if (old != null && old.distanceTo(p) <= 2)
		{
			return;
		}
		places().put(key, p);
		if (save != null)
		{
			save.accept(PLACES_KEY, encodePlaces(places()));
		}
	}

	/** A game message, tags removed. */
	void chat(String message, int now)
	{
		if (message == null)
		{
			return;
		}
		GnomeRestaurant.HandOver h = GnomeRestaurant.handOver(message);
		if (h != null)
		{
			handOver = h;
			handTick = now;
			handOrder = orderV;
			handCustomer = customerV;
			before = new HashMap<>(lastHeld);
			tipped = false;
			return;
		}
		if (GnomeRestaurant.tip(message) != null)
		{
			tipped = true;
			return;
		}
		int c = GnomeRestaurant.credits(message);
		if (c >= 0)
		{
			credits = c;
			if (save != null)
			{
				save.accept(CREDITS_KEY, Integer.toString(c));
			}
			return;
		}
		if (message.startsWith(GnomeRestaurant.DISCARD) && handOver == null)
		{
			discarded = true;
		}
	}

	/** Every tick, active or not: the order's varbits, so a delivery is kept wherever you are. */
	void watch(Client client, long now)
	{
		tick = client.getTickCount();
		if (client.getLocalPlayer() == null)
		{
			return;
		}
		if (firstTick < 0)
		{
			firstTick = tick;
		}
		int o = client.getVarbitValue(GnomeRestaurant.ORDER_VARBIT);
		int c = client.getVarbitValue(GnomeRestaurant.CUSTOMER_VARBIT);
		int xp = client.getSkillExperience(Skill.COOKING);
		if (o != orderV || c != customerV)
		{
			if (o != 0 && c != 0)
			{
				begin(o, c, xp, now);
			}
			else if (orderV != 0 && handOver == null)
			{
				late(now, xp);
				endOrder();
			}
			orderV = o;
			customerV = c;
		}
		if (discarded && handOver == null)
		{
			late(now, xp);
			discarded = false;
		}
		if (handOver != null && tick - handTick >= TIP_TICKS)
		{
			keep(Pack.read(client).contents(), xp, now - (long) (tick - handTick) * 600);
		}
		if (o != 0 || handOver != null)
		{
			lastHeld = Pack.read(client).contents();
		}
	}

	/** An order seen: just taken (timed from now), or one from before this login (timed from the save, if it is the same). */
	private void begin(int o, int c, int xp, long now)
	{
		lateKept = false;
		long[] saved = savedOrder();
		if (saved != null && saved[0] == o && saved[1] == c)
		{
			startMs = saved[2];
			startXp = (int) saved[3];
			return;
		}
		if (tick - firstTick < LOGIN_TICKS)
		{
			// Taken before this login, and not by this Pilot: its clock is not known.
			startMs = -1;
			startXp = -1;
			return;
		}
		startMs = now;
		startXp = xp;
		if (save != null)
		{
			save.accept(ORDER_KEY, "1;" + o + "," + c + "," + now + "," + xp);
		}
	}

	/** The order this Pilot saw taken: order, customer, when and the experience then; or null. */
	private long[] savedOrder()
	{
		String s = load == null ? null : load.apply(ORDER_KEY);
		if (s == null || !s.startsWith("1;"))
		{
			return null;
		}
		String[] f = s.substring(2).split(",");
		if (f.length != 4)
		{
			return null;
		}
		try
		{
			long[] out = new long[4];
			for (int i = 0; i < 4; i++)
			{
				out[i] = Long.parseLong(f[i].trim());
			}
			return out[2] > 0 && out[3] >= 0 ? out : null;
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	private void endOrder()
	{
		startMs = -1;
		startXp = -1;
		if (save != null)
		{
			save.accept(ORDER_KEY, "");
		}
	}

	/** Ms left on the order, or {@link GnomeOrder#UNKNOWN}. */
	long msLeft(Customer customer, long now)
	{
		return startMs <= 0 || customer == null ? GnomeOrder.UNKNOWN : customer.limitMs() - (now - startMs);
	}

	/** An order that ran out of time: kept as late, once. */
	private void late(long now, int xp)
	{
		Customer cu = Customer.ofVarbit(customerV);
		if (lateKept || orderV == 0 || cu == null || startMs <= 0 || now - startMs <= cu.limitMs())
		{
			return;
		}
		lateKept = true;
		add(new GnomeLog.Delivery(now, cu.isHard(), Dish.ofVarbit(orderV), cu, now - startMs, waitBefore(), false,
			startXp >= 0 ? xp - startXp : 0, 0, null));
		lastEnd = now;
	}

	/** The hand-over and its tip: kept. */
	private void keep(Map<Integer, Integer> held, int xp, long at)
	{
		GnomeRestaurant.HandOver h = handOver;
		handOver = null;
		Customer cu = h.customer != null ? h.customer : Customer.ofVarbit(handCustomer);
		Dish dish = h.dish != null ? h.dish : Dish.ofVarbit(handOrder);
		Map<Integer, Integer> tip = new LinkedHashMap<>();
		long gp = 0;
		if (tipped && before != null)
		{
			for (Map.Entry<Integer, Integer> e : held.entrySet())
			{
				int gained = e.getValue() - before.getOrDefault(e.getKey(), 0);
				if (gained > 0)
				{
					tip.put(e.getKey(), gained);
					gp += (long) gained * Math.max(0, price.applyAsInt(e.getKey()));
				}
			}
		}
		long ms = startMs > 0 ? at - startMs : 0;
		boolean onTime = cu == null || startMs <= 0 || ms <= cu.limitMs() + 1200;
		add(new GnomeLog.Delivery(at, cu != null && cu.isHard(), dish, cu, ms, waitBefore(), onTime, startXp >= 0 ? xp - startXp : 0, gp, tip));
		if (onTime)
		{
			delivered++;
			tipsGp += gp;
		}
		lastEnd = at;
		endOrder();
	}

	private long waitBefore()
	{
		return lastEnd > 0 && startMs > lastEnd ? startMs - lastEnd : 0;
	}

	private void add(GnomeLog.Delivery d)
	{
		log().add(d);
		if (save != null)
		{
			save.accept(LOG_KEY, log().encode());
		}
	}

	/** Logged out or hopped: the order's clock goes on in the save. */
	void relogged()
	{
		firstTick = -1;
		orderV = 0;
		customerV = 0;
	}

	/** An order is held, a hand-over is waiting on its tip, or Gianne jnr. is in sight. */
	boolean active(Client client, Collection<NPC> npcs)
	{
		if (client.getLocalPlayer() == null)
		{
			return false;
		}
		if (orderV != 0 || handOver != null)
		{
			return true;
		}
		return find(npcs, GnomeRestaurant.GIANNE) != null;
	}

	int units()
	{
		return 0;
	}

	void left()
	{
	}

	private static NPC find(Collection<NPC> npcs, String name)
	{
		for (NPC n : npcs)
		{
			if (n.getName() != null && n.getName().equalsIgnoreCase(name))
			{
				return n;
			}
		}
		return null;
	}

	Flight fly(Client client, Collection<NPC> npcs, long now)
	{
		session.tick(client, now);
		Pack pack = Pack.read(client);
		WorldPoint at = client.getLocalPlayer().getWorldLocation();
		for (NPC n : npcs)
		{
			Customer c = Customer.named(n.getName());
			if (c != null)
			{
				learn(c.name(), n.getWorldLocation());
			}
			else if (n.getName() != null && n.getName().equalsIgnoreCase(GnomeRestaurant.GIANNE))
			{
				learn(GIANNE_PLACE, n.getWorldLocation());
			}
		}
		Dish dish = Dish.ofVarbit(orderV);
		Customer customer = Customer.ofVarbit(customerV);

		GnomeOrder.Snapshot s = new GnomeOrder.Snapshot();
		lastState = s;
		s.ordered = orderV != 0;
		s.dish = dish;
		s.customer = customer;
		s.held = pack.contents();
		s.cooking = client.getBoostedSkillLevel(Skill.COOKING);
		s.msLeft = msLeft(customer, now);
		NPC there = customer == null ? null : find(npcs, customer.getTitle());
		s.customerInSight = there != null;
		WorldPoint dest = there != null ? there.getWorldLocation() : customer == null ? null : placeOf(customer);
		s.placeKnown = dest != null;
		if (dest != null && there == null && at != null)
		{
			HomeWay.Plan plan = HomeWay.planTo(dest.getX(), dest.getY(), at.getX(), at.getY(), at.getPlane(), atlas.get());
			s.way = plan == null ? null : plan.line();
		}

		GnomeOrder.Plan plan = GnomeOrder.next(s);
		Step step = plan.step;
		View view = new View(true, View.Activity.GNOME_RESTAURANT, step, place(s), dish == null ? null : GnomeOrder.loop(dish),
			plan.at < 0 ? -1 : plan.at);
		Flight out = new Flight(View.Activity.GNOME_RESTAURANT, step, view);
		if (step.getTarget() == Step.Target.GNOME_CUSTOMER)
		{
			if (there != null)
			{
				aim(client, out, there);
			}
			out.destination = dest;
		}
		else if (step.getTarget() == Step.Target.GIANNE)
		{
			NPC gianne = find(npcs, GnomeRestaurant.GIANNE);
			if (gianne != null)
			{
				aim(client, out, gianne);
			}
			else
			{
				WorldPoint g = places().get(GIANNE_PLACE);
				out.destination = g != null ? g : new WorldPoint(GnomeRestaurant.RESTAURANT.x, GnomeRestaurant.RESTAURANT.y, 0);
			}
		}
		dress(view, s, plan, client, now);
		return out;
	}

	private static void aim(Client client, Flight out, NPC npc)
	{
		Ground ground = Ground.read(client, java.util.Collections.emptyList(), 0);
		Ground.Way<NPC> way = ground == null ? null : ground.to(npc);
		if (way != null)
		{
			out.to(way);
		}
		else
		{
			out.npc = npc;
		}
	}

	/** Where a customer is: where you last saw them, else RuneLite's world-map point at them, else not known. */
	WorldPoint placeOf(Customer c)
	{
		WorldPoint seen = places().get(c.name());
		if (seen != null)
		{
			return seen;
		}
		GnomeRestaurant.Anchor a = c.anchor();
		return a == null ? null : new WorldPoint(a.x, a.y, 0);
	}

	private static String place(GnomeOrder.Snapshot s)
	{
		if (s.customer == null)
		{
			return "The Grand Tree";
		}
		return s.customer.isHard() ? "Hard delivery" : "Easy delivery";
	}

	private void dress(View view, GnomeOrder.Snapshot s, GnomeOrder.Plan plan, Client client, long now)
	{
		int cr = credits();
		if (s.dish != null && s.customer != null)
		{
			long limit = s.customer.limitMs();
			if (s.msLeft != GnomeOrder.UNKNOWN)
			{
				long left = Math.max(0, s.msLeft);
				boolean tight = left < limit / 4;
				view.gauge(new View.Gauge("Seconds left", (int) ((left + 999) / 1000), 0, (int) (limit / 1000),
					left == 0 ? "Too late: back to Gianne jnr." : GnomeRestaurant.clock(left) + " of " + GnomeRestaurant.clock(limit) + " to " + s.customer.getTitle(),
					left == 0 ? Theme.BLOOD : tight ? Theme.EMBER : Theme.GOLD));
			}
			view.lines(s.dish.getTitle(), "Lv " + s.dish.level() + (s.dish.xp() > 0 ? " · " + s.dish.xp() + " xp" : ""),
				recipeLines(s, plan));
		}
		if (cr >= 0)
		{
			int foods = GnomeRestaurant.foods(cr);
			int next = Math.min(GnomeRestaurant.MAX_FOODS * GnomeRestaurant.CREDITS_PER_FOOD, (foods + 1) * GnomeRestaurant.CREDITS_PER_FOOD);
			String note = foods >= GnomeRestaurant.MAX_FOODS ? "Full: 10 free foods, the most a token holds"
				: (foods > 0 ? foods + (foods == 1 ? " free food" : " free foods") + " · " : "") + GnomeRestaurant.toNextFood(cr) + " to the next";
			int coming = s.customer == null ? 0 : s.customer.credits();
			view.gauge(new View.Gauge("Token credits", Math.min(cr, next), coming, next, note, Theme.TEAL));
		}

		view.stat(new View.Stat(Integer.toString(delivered), "Deliveries", tipsGp > 0 ? Session.format(tipsGp) + " gp in tips" : "", Theme.GOLD));
		int xp = session.xp(client);
		view.stat(new View.Stat(Session.format(xp), "Cooking xp", session.perHour(xp, now), Theme.EMBER));
		GnomeLog g = log();
		double xph = g.xpPerHour();
		if (xph >= 0)
		{
			view.stat(new View.Stat(Session.format(Math.round(xph)), "Xp an hour", "Your pace, waits in", Theme.GOLD_BRIGHT));
			view.stat(new View.Stat(Session.format(Math.round(g.gpPerHour())), "Gp an hour", "Tips, your pace",
				Theme.GREEN));
		}

		if (s.dish == null)
		{
			view.pick(checklist(g, s.cooking));
		}
		if (g.count() > 0)
		{
			view.supply(historyCard(g, client));
		}
	}

	/** Each step of the dish: done, next with what it lacks, or to come; then the delivery. */
	static List<View.Line> recipeLines(GnomeOrder.Snapshot s, GnomeOrder.Plan plan)
	{
		List<View.Line> lines = new ArrayList<>();
		List<Stage> stages = s.dish.stages();
		for (int i = 0; i < stages.size(); i++)
		{
			Stage st = stages.get(i);
			Map<Ingredient, Integer> lack = i < plan.missing.size() ? plan.missing.get(i) : java.util.Collections.emptyMap();
			boolean done = plan.at > i;
			String trailing = done ? "Done" : lack.isEmpty() ? (i == plan.at ? "Ready" : "") : "Need " + lackText(lack);
			View.Line.Mark mark = done ? View.Line.Mark.DONE : i == plan.at ? View.Line.Mark.NEXT : View.Line.Mark.TODO;
			lines.add(new View.Line(short_(st), trailing, mark));
		}
		boolean ready = plan.at >= stages.size();
		lines.add(new View.Line("Deliver to " + s.customer.getTitle(), s.msLeft == GnomeOrder.UNKNOWN ? (s.customer.isHard() ? "11:00 limit" : "6:00 limit")
			: GnomeRestaurant.clock(Math.max(0, s.msLeft)), ready ? View.Line.Mark.NEXT : View.Line.Mark.TODO));
		return lines;
	}

	/** "Prepare: 1 cheese, 1 tomato", "Bake on a range". */
	static String short_(Stage st)
	{
		if (st.range)
		{
			return st.name + " on a range";
		}
		List<String> parts = new ArrayList<>();
		for (int k = 0; k < st.uses.size(); k++)
		{
			Ingredient g = st.uses.get(k);
			if (g == Ingredient.COCKTAIL_SHAKER || g == Ingredient.COCKTAIL_GLASS)
			{
				continue;
			}
			parts.add(st.counts.get(k) == 1 ? g.getTitle() : st.counts.get(k) + " " + g.getTitle());
		}
		for (Ingredient t : st.tools)
		{
			parts.add(t.getTitle());
		}
		String what = st.name.equals("Pour") ? "Pour into a glass" : st.name;
		return parts.isEmpty() ? what : what + ": " + String.join(", ", parts);
	}

	private static String lackText(Map<Ingredient, Integer> lack)
	{
		List<String> parts = new ArrayList<>();
		for (Map.Entry<Ingredient, Integer> e : lack.entrySet())
		{
			parts.add(e.getValue() == 1 ? e.getKey().getTitle() : e.getValue() + " " + e.getKey().getTitle());
		}
		return String.join(", ", parts);
	}

	/** Every dish against your Cooking, with how many you have delivered. */
	static View.PickCard checklist(GnomeLog g, int cooking)
	{
		Map<Dish, Integer> made = g.byDish();
		List<View.Pick> picks = new ArrayList<>();
		int can = 0;
		int done = 0;
		for (GnomeRestaurant.Kind kind : GnomeRestaurant.Kind.values())
		{
			for (Dish d : Dish.values())
			{
				if (d.kind() != kind)
				{
					continue;
				}
				boolean open = cooking >= d.level();
				int n = made.getOrDefault(d, 0);
				can += open ? 1 : 0;
				done += n > 0 ? 1 : 0;
				String detail = kind.getTitle().replaceAll("s$", "") + (d.xp() > 0 ? " · " + d.xp() + " xp" : "")
					+ (n > 0 ? " · delivered " + n : open ? " · not delivered yet" : " · " + (d.level() - cooking) + (d.level() - cooking == 1 ? " level to go" : " levels to go"));
				picks.add(new View.Pick((n > 0 ? "\u2713 " : "") + d.getTitle(), "Lv " + d.level(), detail, open, false, n > 0 ? 1 : 0, false));
			}
		}
		return new View.PickCard("Gnome recipes", done + " of " + Dish.values().length + " delivered",
			"You can make " + can + " of " + Dish.values().length + " at your Cooking. Orders can ask for any of them: your level is not taken into account.", picks);
	}

	/** Your last deliveries, the quickest, and experience and tips an hour across them. */
	private static View.Supply historyCard(GnomeLog g, Client client)
	{
		List<View.Line> lines = new ArrayList<>();
		List<GnomeLog.Delivery> all = g.all();
		for (int i = all.size() - 1; i >= 0 && i >= all.size() - 5; i--)
		{
			GnomeLog.Delivery d = all.get(i);
			String what = (d.dish == null ? "Delivery" : d.dish.getTitle()) + (d.customer == null ? "" : " · " + d.customer.getTitle());
			String trailing = !d.onTime ? "Too late" : (d.ms > 0 ? GnomeRestaurant.clock(d.ms) : "") + (d.tip.isEmpty() ? "" : (d.ms > 0 ? " · " : "") + tipText(d, client));
			lines.add(new View.Line(what, trailing, !d.onTime ? View.Line.Mark.TODO : d.hard ? View.Line.Mark.DONE : View.Line.Mark.IDLE));
		}
		GnomeLog.Delivery easy = g.fastest(false);
		GnomeLog.Delivery hard = g.fastest(true);
		String headline = easy == null && hard == null ? null : "Quickest: " + (easy == null ? "" : "easy " + GnomeRestaurant.clock(easy.ms))
			+ (easy != null && hard != null ? ", " : "") + (hard == null ? "" : "hard " + GnomeRestaurant.clock(hard.ms));
		double h = g.xpPerHour();
		String detail = h < 0 ? null : Session.format(Math.round(h)) + " Cooking xp and " + Session.format(Math.round(g.gpPerHour()))
			+ " gp in tips an hour, " + String.format(Locale.ROOT, "%.1f", g.perHour()) + " deliveries, over your timed ones";
		return new View.Supply(g.onTime() + " of " + g.count() + " in time", lines, headline, detail, false, null).titled("Your deliveries");
	}

	/** "5 grand seed pods", or the first item and a count of the rest. */
	private static String tipText(GnomeLog.Delivery d, Client client)
	{
		Map.Entry<Integer, Integer> first = d.tip.entrySet().iterator().next();
		String name;
		ItemComposition def = client.getItemDefinition(first.getKey());
		name = def == null || def.getName() == null || "null".equals(def.getName()) ? "a tip" : def.getName();
		return (first.getValue() > 1 ? first.getValue() + " " : "") + name + (d.tip.size() > 1 ? " +" + (d.tip.size() - 1) : "");
	}

	/** Brief for the panel away from the restaurant. */
	View.Brief brief()
	{
		GnomeLog g = log();
		String status = null;
		GnomeLog.Delivery last = g.last();
		if (last != null)
		{
			status = "Last delivery: " + (last.dish == null ? "a dish" : last.dish.getTitle().toLowerCase(Locale.ROOT))
				+ (last.customer == null ? "" : " to " + last.customer.getTitle()) + (last.onTime ? last.ms > 0 ? " in " + GnomeRestaurant.clock(last.ms) : "" : ", too late");
		}
		int cr = credits();
		View.Brief b = new View.Brief(View.Activity.GNOME_RESTAURANT, status,
			"Gianne jnr., west on the Grand Tree's first floor, after Aluft Gianne snr.'s training at 29 Cooking. Easy orders 6 minutes around the Stronghold, hard 11 minutes anywhere"
				+ (cr >= 0 ? "; your token has " + cr + " credits." : "."));
		double h = g.xpPerHour();
		if (h >= 0)
		{
			b.best(Session.format(Math.round(h)) + " Cooking xp and " + Session.format(Math.round(g.gpPerHour())) + " gp an hour over " + g.count()
				+ (g.count() == 1 ? " delivery." : " deliveries."));
		}
		return b;
	}
}
