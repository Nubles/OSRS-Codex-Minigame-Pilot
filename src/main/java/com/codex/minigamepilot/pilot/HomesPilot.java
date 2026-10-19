package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.BankStock;
import com.codex.minigamepilot.core.Contract;
import com.codex.minigamepilot.core.ContractBook;
import com.codex.minigamepilot.core.Home;
import com.codex.minigamepilot.core.ItemIds;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.core.Supplies;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

/**
 * Flies Mahogany Homes: getting a contract, fetching the right planks, the way to the client's
 * house and the teleport to take, then each piece of furniture in walking order, floor by floor.
 */
public final class HomesPilot
{
	private static final Pattern ASSIGNED = Pattern.compile("(\\w+) Contract: Go see (\\w+)");
	private static final Pattern GO_SEE = Pattern.compile("[Gg]o see (\\w+)");
	private static final Pattern ON_TIER = Pattern.compile("on an? (\\w+) [Cc]ontract");
	private static final Pattern ASKED = Pattern.compile("Could I have an? (\\w+) contract");
	private static final Pattern FINISHED = Pattern.compile("You have completed [\\d,]+ contracts? with a total of [\\d,]+ points?\\.");
	/** Tiles from a client's house within which an open contract takes over the panel. */
	private static final int NEAR = 150;
	private static final String[] LOOP = {"Job", "Planks", "Go", "Fix", "Report"};
	private static final int[] PLANK_SACK_VARBITS = {
		VarbitID.PLANK_SACK_PLAIN, VarbitID.PLANK_SACK_OAK, VarbitID.PLANK_SACK_TEAK, VarbitID.PLANK_SACK_MAHOGANY
	};

	/** Teleports that land near each city's houses, by the start of the item's name. */
	private static final Map<Home.City, String[]> TELEPORTS = new HashMap<>();

	static
	{
		TELEPORTS.put(Home.City.FALADOR, new String[]{"Falador teleport", "Ring of wealth", "Skills necklace"});
		TELEPORTS.put(Home.City.VARROCK, new String[]{"Varrock teleport"});
		TELEPORTS.put(Home.City.ARDOUGNE, new String[]{"Ardougne teleport", "Ardougne cloak", "Achievement diary cape"});
		TELEPORTS.put(Home.City.HOSIDIUS, new String[]{"Hosidius teleport", "Xeric's talisman", "Kharedst's memoirs", "Book of the dead"});
	}

	private final Session session = new Session(Skill.CONSTRUCTION);
	private final Consumer<Contract> save;
	private final Map<Integer, String> objectNames = new HashMap<>();
	private Contract contract;
	private Contract.Tier asked;
	private String lastDialog;
	private int contracts;
	/** The last snapshot planned from, for the field notes. */
	Object lastState;
	/** The contract tier's planks carried, plank sack included, as of the last tick flown. */
	private int lastPlanks;
	private int points;
	private long assignedAt;

	/** Where the contract book is saved for this account. */
	static final String BOOK_KEY = "homes.book";
	/** Ticks after the hand-in message before its experience is read, so the reward has landed. */
	static final int SETTLE_TICKS = 3;
	private final Function<String, String> load;
	private final BiConsumer<String, String> saveKey;
	private ContractBook book = new ContractBook();
	/** The contract being measured: its house, the experience when work began, and what it takes. */
	private Home meterHome;
	/** Experience when the job was first seen untouched, -1 before that, -2 when first seen part done. */
	private long meterXp = -1;
	private int meterPlanks;
	private int meterSteel;
	private Contract.Tier meterTier;
	/** A handed-in contract waiting for its experience to settle: ticks left, or -1. */
	private int settleIn = -1;
	private long settleXp;
	private int settlePlanks;
	private int settleSteel;
	private Contract.Tier settleTier;
	private long settleSeconds;
	/** When the last contract was handed in, for the time a whole loop takes. */
	private long lastHandIn;

	HomesPilot(Contract saved, Consumer<Contract> save, Function<String, String> load, BiConsumer<String, String> saveKey)
	{
		this.contract = saved;
		this.save = save;
		this.load = load;
		this.saveKey = saveKey;
		reloadBook();
	}

	/** This account's contract book, after logging in or switching account. */
	void reloadBook()
	{
		book = ContractBook.decode(load == null ? null : load.apply(BOOK_KEY));
		meterHome = null;
		meterXp = -1;
		settleIn = -1;
		lastHandIn = 0;
	}

	ContractBook getBook()
	{
		return book;
	}

	/**
	 * Measures the contract open now: the first time its furniture is seen with every piece still
	 * to do, the experience and the planks and steel the job takes are noted. A job first seen part
	 * done is not measured, since what was done before would be missing.
	 */
	void meter(Contract open, int[] values, long xp)
	{
		if (open == null || open.getTier() == null)
		{
			return;
		}
		if (open.getHome() != meterHome)
		{
			meterHome = open.getHome();
			meterXp = -1;
		}
		// No experience at all means the skills have not arrived yet (just logged in): wait for them.
		if (meterXp != -1 || xp <= 0 || !Contract.known(values))
		{
			return;
		}
		int used = 0;
		for (int v : values)
		{
			if (v != 0)
			{
				used++;
			}
		}
		int left = open.remaining(values).size();
		if (left == 0)
		{
			// Every piece done: the end of a job (this one or the last), which says nothing yet.
			return;
		}
		if (left == used)
		{
			meterXp = xp;
			meterPlanks = open.planksNeeded(values);
			meterSteel = open.steelNeeded(values);
			meterTier = open.getTier();
		}
		else
		{
			meterXp = -2;
		}
	}

	/** The hand-in message: a measured contract waits a few ticks for its experience to land. */
	private void handedIn(long now)
	{
		if (contract != null && contract.getHome() == meterHome && meterXp >= 0)
		{
			settleIn = SETTLE_TICKS;
			settleXp = meterXp;
			settlePlanks = meterPlanks;
			settleSteel = meterSteel;
			settleTier = meterTier;
			settleSeconds = lastHandIn > 0 ? (now - lastHandIn) / 1000 : 0;
		}
		lastHandIn = now;
		meterHome = null;
		meterXp = -1;
	}

	/** Every tick, wherever you are: files a handed-in contract once its experience has landed. */
	void settle(long xp)
	{
		if (settleIn < 0 || --settleIn > 0)
		{
			return;
		}
		settleIn = -1;
		if (xp > 0 && book.record(settleTier, xp - settleXp, settlePlanks, settleSteel, settleSeconds) && saveKey != null)
		{
			saveKey.accept(BOOK_KEY, book.encode());
		}
	}

	Contract getContract()
	{
		return contract;
	}

	/** After a profile change, the saved contract of the account now logged in. */
	void load(Contract saved)
	{
		contract = saved;
	}

	/** A running count of what rounds produce, for the pace coach. */
	int units()
	{
		return contracts;
	}

	/** Contracts are only compared with the same house and tier. */
	String kindHint()
	{
		return contract == null ? null : contract.getHome().getName() + (contract.getTier() == null ? "" : " " + contract.getTier().getName());
	}

	static boolean isWatched(int id)
	{
		for (Home h : Home.values())
		{
			if (h.indexOf(id) >= 0 || h.isLadder(id))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether this pilot has something to say: a contractor in sight, or a contract open and the
	 * player plainly working on it (near the house, carrying its planks, or just given it). An open
	 * contract alone does not take over the panel while the player is off doing something else.
	 */
	boolean active(Client client, long now)
	{
		if (contractorNear(client) != null)
		{
			return true;
		}
		if (contract == null)
		{
			return false;
		}
		if (now - assignedAt < 15 * 60_000L)
		{
			return true;
		}
		WorldPoint at = client.getLocalPlayer().getWorldLocation();
		if (contract.getHome().distanceTo(at.getX(), at.getY()) <= NEAR)
		{
			return true;
		}
		Contract.Tier tier = contract.getTier();
		return tier != null && Pack.read(client).carries(tier.getPlank());
	}

	View.Brief brief(BankStock stock, int constructionLevel)
	{
		String status = contract == null ? "No contract open"
			: (contract.getTier() == null ? "A" : contract.getTier().getName()) + " contract for " + contract.getHome().getName()
			+ " in " + contract.getHome().getCity().getName();
		Contract.Tier tier = contract != null && contract.getTier() != null ? contract.getTier() : Contract.Tier.forLevel(constructionLevel);
		if (stock != null && tier != null)
		{
			Supplies.Homes h = Supplies.homes(tier, stock.count(tier.getPlank()), stock.count(Contract.STEEL_BAR), book.tally(tier));
			if (h.getContracts() >= 0)
			{
				status += ". In the bank: " + Character.toLowerCase(h.headline().charAt(0)) + h.headline().substring(1);
			}
		}
		return new View.Brief(View.Activity.HOMES, status,
			"Contracts from Amy in Falador, Marlo in Varrock, Ellie in East Ardougne or Angelo in Hosidius.");
	}

	void chat(String message)
	{
		String text = Text.removeTags(message);
		Matcher m = ASSIGNED.matcher(text);
		if (m.find())
		{
			assign(m.group(2), Contract.Tier.byName(m.group(1)));
		}
		if (FINISHED.matcher(text).find())
		{
			handedIn(System.currentTimeMillis());
			if (contract != null)
			{
				contracts++;
				points += contract.getTier() == null ? 0 : contract.getTier().points();
			}
			contract = null;
			save.accept(null);
		}
	}

	/** Reads the dialogue box, where contracts are handed out and reminded of. */
	private void dialog(Client client)
	{
		Widget npc = client.getWidget(InterfaceID.ChatLeft.TEXT);
		Widget me = client.getWidget(InterfaceID.ChatRight.TEXT);
		String text = npc != null && !npc.isHidden() ? npc.getText() : me != null && !me.isHidden() ? me.getText() : null;
		if (text == null || text.equals(lastDialog))
		{
			return;
		}
		lastDialog = text;
		String plain = Text.sanitizeMultilineText(text);
		Matcher ask = ASKED.matcher(plain);
		if (ask.find())
		{
			asked = Contract.Tier.byName(ask.group(1));
			return;
		}
		Matcher go = GO_SEE.matcher(plain);
		if (go.find() && plain.contains("another job"))
		{
			Matcher tier = ON_TIER.matcher(plain);
			assign(go.group(1), tier.find() ? Contract.Tier.byName(tier.group(1)) : asked);
		}
	}

	private void assign(String name, Contract.Tier tier)
	{
		Home home = Home.byName(name);
		if (home == null)
		{
			return;
		}
		if (tier == null && contract != null && contract.getHome() == home)
		{
			tier = contract.getTier();
		}
		Contract next = new Contract(home, tier);
		if (contract == null || contract.getHome() != home || contract.getTier() != tier)
		{
			if (contract == null || contract.getHome() != home)
			{
				assignedAt = System.currentTimeMillis();
			}
			contract = next;
			save.accept(next);
		}
	}

	Flight fly(Client client, SceneIndex scene, Collection<NPC> npcs, long now)
	{
		dialog(client);
		Player me = client.getLocalPlayer();
		WorldPoint at = me.getWorldLocation();
		Pack pack = Pack.read(client);

		Contract.Snapshot s = new Contract.Snapshot();
		lastState = s;
		for (int i = 0; i < Home.HOTSPOTS; i++)
		{
			s.values[i] = client.getVarbitValue(Home.HOTSPOT_VARBIT + i);
		}
		s.constructionLevel = client.getRealSkillLevel(Skill.CONSTRUCTION);
		if (contract != null && contract.getTier() == null)
		{
			Contract.Tier seen = Contract.tierFromValues(s.values);
			if (seen != null)
			{
				contract = new Contract(contract.getHome(), seen);
				save.accept(contract);
			}
		}
		Contract.Tier tier = contract == null ? null : contract.getTier();
		if (tier != null)
		{
			s.planks = pack.count(tier.getPlank());
			if (pack.carries(ItemIds.PLANK_SACK))
			{
				s.planks += client.getVarbitValue(PLANK_SACK_VARBITS[tier.ordinal()]);
			}
		}
		s.steelBars = pack.count(Contract.STEEL_BAR);
		meter(contract, s.values, client.getSkillExperience(Skill.CONSTRUCTION));
		lastPlanks = s.planks;
		Home home = contract == null ? null : contract.getHome();
		s.atHome = home != null && home.contains(at.getX(), at.getY());
		if (contract != null)
		{
			session.tick(client, now);
		}

		Step step = Contract.next(contract, s);
		Ground ground = Ground.read(client, null, 0);
		Flight flight;
		int stage;
		switch (step.getTarget())
		{
			case CONTRACTOR:
			{
				stage = 0;
				NPC who = contractorNear(client);
				flight = new Flight(View.Activity.HOMES, step, null);
				if (who != null && ground != null)
				{
					flight.to(ground.to(who));
					flight.npc = who;
				}
				break;
			}
			case CLIENT:
			{
				stage = 4;
				NPC who = clientNear(npcs, home);
				Ground.Way<NPC> way = ground == null ? null : ground.to(who);
				if (who != null && way == null && ground != null)
				{
					Step climb = climb(home, ground, scene, home.getName() + " is on the other floor");
					if (climb != null)
					{
						step = climb;
					}
				}
				flight = new Flight(View.Activity.HOMES, step, null);
				if (step.getTarget() == Step.Target.LADDER)
				{
					flight.to(ground.nearest(ladders(scene, home), null));
				}
				else
				{
					flight.to(way);
					flight.npc = who;
				}
				if (who == null && !s.atHome)
				{
					flight.destination = new WorldPoint(home.getDoorX(), home.getDoorY(), 0);
				}
				break;
			}
			case ITEM:
				stage = 1;
				flight = new Flight(View.Activity.HOMES, step, null);
				break;
			case HOME:
			{
				stage = 2;
				int teleport = teleport(client, home.getCity());
				if (teleport > 0 && home.distanceTo(at.getX(), at.getY()) > 60)
				{
					step = step.withItems(teleport).withDetail(step.getDetail() + ". Your " + client.getItemDefinition(teleport).getName() + " is the quickest way");
				}
				flight = new Flight(View.Activity.HOMES, step, null);
				WorldPoint door = new WorldPoint(home.getDoorX(), home.getDoorY(), 0);
				flight.destination = door;
				if (ground != null && at.getPlane() == 0)
				{
					flight.route = ground.toTile(door);
				}
				break;
			}
			default:
			{
				stage = 3;
				List<TileObject> todo = new ArrayList<>();
				for (TileObject o : scene.get(hotspotIds(home)))
				{
					int i = home.indexOf(o.getId());
					if (i >= 0 && Contract.needsWork(s.values[i]))
					{
						todo.add(o);
					}
				}
				Ground.Way<TileObject> way = ground == null ? null : ground.nearest(todo, null);
				if (way != null)
				{
					int value = s.values[home.indexOf(way.thing.getId())];
					String name = name(client, way.thing);
					step = Step.of(Contract.verb(value), Contract.verb(value) + " the " + name, step.getDetail(), Step.Target.HOTSPOT);
				}
				else if (!todo.isEmpty() && ground != null)
				{
					Step climb = climb(home, ground, scene, "The rest of the work is on the other floor");
					if (climb != null)
					{
						step = climb;
						way = ground.nearest(ladders(scene, home), null);
					}
				}
				flight = new Flight(View.Activity.HOMES, step, null).to(way);
				for (TileObject o : todo)
				{
					if (o != flight.object)
					{
						flight.others.add(o);
					}
				}
				break;
			}
		}

		String place = home == null ? null : home.getName() + ", " + home.getCity().getName();
		View view = new View(true, View.Activity.HOMES, step, place, LOOP, stage);
		if (contract != null && Contract.known(s.values) && tier != null)
		{
			int planks = contract.planksNeeded(s.values);
			int steel = contract.steelNeeded(s.values);
			view.gauge(new View.Gauge(tier.getName() + " planks", Math.min(s.planks, Math.max(planks, s.planks)), 0, Math.max(planks, s.planks),
				s.planks >= planks ? "Enough for the rest of the job" : (planks - s.planks) + " short", s.planks >= planks ? Theme.GREEN : Theme.EMBER));
			if (steel > 0 || s.steelBars > 0)
			{
				view.gauge(new View.Gauge("Steel bars", s.steelBars, 0, Math.max(steel, s.steelBars),
					s.steelBars >= steel ? "Enough" : (steel - s.steelBars) + " short", s.steelBars >= steel ? Theme.GREEN : Theme.EMBER));
			}
		}
		if (contract != null && Contract.known(s.values))
		{
			view.lines("Furniture", (contract.remaining(s.values).size()) + " left", furniture(client, scene, home, s.values, flight.object));
		}
		if (session.begun())
		{
			view.stat(new View.Stat(String.valueOf(contracts), "Contracts", session.perHour(contracts, now), Theme.GOLD));
			view.stat(new View.Stat(String.valueOf(points), "Points", "From finished contracts", Theme.TEAL));
			int xp = session.xp(client);
			view.stat(new View.Stat(Session.format(xp), "Con xp", session.perHour(xp, now), Theme.GREEN));
			view.stat(new View.Stat(session.elapsed(now), "Time", "On contracts", Theme.TEXT_MUTED));
		}
		Flight out = new Flight(View.Activity.HOMES, step, view);
		out.object = flight.object;
		out.npc = flight.npc;
		out.route = flight.route;
		out.destination = flight.destination;
		out.others.addAll(flight.others);
		return out;
	}

	/**
	 * What each tier costs you per experience point at today's prices, from your own finished
	 * contracts; null until one is finished or while prices are loading.
	 */
	View.PickCard costs(IntUnaryOperator price, int constructionLevel)
	{
		if (book.isEmpty() || price == null)
		{
			return null;
		}
		List<ContractBook.Row> rows = book.rank(price, constructionLevel);
		if (rows.isEmpty())
		{
			return null;
		}
		Contract.Tier current = contract != null && contract.getTier() != null ? contract.getTier() : Contract.Tier.forLevel(constructionLevel);
		return costCard(rows, current);
	}

	/** The ranking as the panel shows it. */
	static View.PickCard costCard(List<ContractBook.Row> rows, Contract.Tier current)
	{
		double cheapest = 0;
		for (ContractBook.Row r : rows)
		{
			if (r.isKnown() && r.getPerXp() > 0 && (cheapest == 0 || r.getPerXp() < cheapest))
			{
				cheapest = r.getPerXp();
			}
		}
		List<View.Pick> picks = new ArrayList<>();
		for (ContractBook.Row r : rows)
		{
			Contract.Tier t = r.getTier();
			if (!r.isKnown())
			{
				picks.add(new View.Pick(t.getName(), "", r.isUnlocked() ? "Finish one to learn what it costs" : "Needs Construction " + t.getLevel(),
					false, t == current, 0, false));
				continue;
			}
			ContractBook.Tally tally = r.getTally();
			String detail = Session.format(Math.round(tally.xpEach())) + " xp · " + compact(Math.round(r.getCoinsEach())) + " gp"
				+ (r.getXpPerHour() > 0 ? " · " + compact(Math.round(r.getXpPerHour())) + " xp/h" : "");
			double share = r.getPerXp() <= 0 ? 1 : cheapest / r.getPerXp();
			picks.add(new View.Pick(t.getName(), perXp(r.getPerXp()), detail, true, t == current, share, false));
		}
		String note = "A contract's planks and steel at today's prices, against the experience your own contracts gave."
			+ " Rates count the whole loop, contract to contract.";
		return new View.PickCard("What it costs", "coins an xp", note, picks);
	}

	/**
	 * How far the planks and steel bars you have go: in the bank (as last seen by the Pilot or Bank
	 * Steward), carried and in the plank sack. Null when no bank has been seen yet.
	 */
	View.Supply supply(BankStock stock, int constructionLevel, long now)
	{
		if (stock == null)
		{
			return null;
		}
		Contract.Tier tier = contract != null && contract.getTier() != null ? contract.getTier() : Contract.Tier.forLevel(constructionLevel);
		boolean sameTier = contract != null && contract.getTier() == tier;
		long planks = stock.count(tier.getPlank()) + (sameTier ? lastPlanks : 0);
		long steel = stock.count(Contract.STEEL_BAR);
		Supplies.Homes h = Supplies.homes(tier, planks, steel, book.tally(tier));
		List<View.Line> lines = new ArrayList<>();
		String plankName = tier.planks(2).substring(2);
		lines.add(new View.Line(Character.toUpperCase(plankName.charAt(0)) + plankName.substring(1), Session.format(planks),
			planks > 0 ? View.Line.Mark.TODO : View.Line.Mark.IDLE));
		lines.add(new View.Line("Steel bars", Session.format(steel), steel > 0 ? View.Line.Mark.TODO : View.Line.Mark.IDLE));
		String detail = h.getContracts() < 0 ? "Finish a contract and this counts the jobs they cover"
			: h.isSteelShort() ? "Steel bars run out first" : null;
		return new View.Supply(tier.getName(), lines, h.headline(), detail, h.isSteelShort() || h.getContracts() == 0, seenNote(stock, now));
	}

	/** Where and when the bank was seen: "Your bank as the Pilot saw it 12 minutes ago". */
	static String seenNote(BankStock stock, long now)
	{
		String who = stock.getSource() == BankStock.Source.BANK_STEWARD ? "as Codex Bank Steward saw it" : "as last seen";
		long minutes = stock.getSavedAt() <= 0 ? -1 : Math.max(0, (now - stock.getSavedAt()) / 60_000L);
		String when = minutes < 0 ? "" : minutes < 1 ? ", just now" : ", " + com.codex.minigamepilot.core.Goal.duration(minutes) + " ago";
		return "Your bank " + who + when + ", with what you carry.";
	}

	private static String perXp(double coins)
	{
		return String.format(Locale.ENGLISH, coins < 10 ? "%.2f gp" : "%.1f gp", coins);
	}

	private static String compact(long n)
	{
		if (Math.abs(n) >= 1_000_000)
		{
			return String.format(Locale.ENGLISH, "%.1fM", n / 1_000_000d);
		}
		if (Math.abs(n) >= 100_000)
		{
			return Math.round(n / 1000d) + "k";
		}
		if (Math.abs(n) >= 1000)
		{
			return String.format(Locale.ENGLISH, "%.1fk", n / 1000d).replace(".0k", "k");
		}
		return String.valueOf(n);
	}

	private Step climb(Home home, Ground ground, SceneIndex scene, String why)
	{
		if (ground.nearest(ladders(scene, home), null) == null)
		{
			return null;
		}
		return Step.of("Climb", "Take the ladder", why, Step.Target.LADDER);
	}

	private static List<TileObject> ladders(SceneIndex scene, Home home)
	{
		List<TileObject> out = new ArrayList<>();
		for (TileObject o : scene.get(home.getLadders()))
		{
			if (home.distanceTo(o.getWorldLocation().getX(), o.getWorldLocation().getY()) <= 2)
			{
				out.add(o);
			}
		}
		return out;
	}

	private static int[] hotspotIds(Home home)
	{
		int[] ids = new int[home.hotspotCount()];
		for (int i = 0; i < ids.length; i++)
		{
			ids[i] = home.hotspot(i).getObjectId();
		}
		return ids;
	}

	private List<View.Line> furniture(Client client, SceneIndex scene, Home home, int[] values, TileObject next)
	{
		Map<Integer, TileObject> seen = new HashMap<>();
		for (TileObject o : scene.get(hotspotIds(home)))
		{
			seen.put(o.getId(), o);
		}
		List<View.Line> lines = new ArrayList<>();
		for (int i = 0; i < home.hotspotCount(); i++)
		{
			Home.Hotspot h = home.hotspot(i);
			int v = values[i];
			if (v == 0)
			{
				continue;
			}
			TileObject o = seen.get(h.getObjectId());
			String name = o != null ? capital(name(client, o)) : objectNames.getOrDefault(h.getObjectId(), "Piece " + (i + 1));
			String cost = h.getKind().isSteel() ? "steel bar" : h.getKind().getAmount() + (h.getKind().getAmount() == 1 ? " plank" : " planks");
			View.Line.Mark mark = !Contract.needsWork(v) ? View.Line.Mark.DONE : o != null && o == next ? View.Line.Mark.NEXT : View.Line.Mark.TODO;
			String trailing = Contract.needsWork(v) ? Contract.verb(v) + ", " + cost : "Done";
			lines.add(new View.Line(name, trailing, mark));
		}
		return lines;
	}

	/** The furniture's own name, such as "wardrobe", remembered so the checklist keeps it out of sight. */
	private String name(Client client, TileObject o)
	{
		ObjectComposition def = client.getObjectDefinition(o.getId());
		if (def != null && def.getImpostorIds() != null && def.getImpostor() != null)
		{
			def = def.getImpostor();
		}
		String name = def == null ? null : def.getName();
		if (name == null || name.isEmpty() || "null".equals(name))
		{
			return objectNames.getOrDefault(o.getId(), "furniture");
		}
		objectNames.put(o.getId(), capital(name));
		return name.toLowerCase(Locale.ENGLISH);
	}

	private static String capital(String s)
	{
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	/** The best teleport carried or worn for a city, or -1. */
	private static int teleport(Client client, Home.City city)
	{
		String[] names = TELEPORTS.get(city);
		for (String want : names)
		{
			for (int container : new int[]{InventoryID.INV, InventoryID.WORN})
			{
				net.runelite.api.ItemContainer c = client.getItemContainer(container);
				if (c == null)
				{
					continue;
				}
				for (net.runelite.api.Item item : c.getItems())
				{
					if (item.getId() <= 0)
					{
						continue;
					}
					ItemComposition def = client.getItemDefinition(item.getId());
					if (def != null && def.getName() != null && def.getName().startsWith(want))
					{
						return item.getId();
					}
				}
			}
		}
		return -1;
	}

	private static NPC clientNear(Collection<NPC> npcs, Home home)
	{
		for (NPC n : npcs)
		{
			if (n != null && n.getName() != null && n.getName().equalsIgnoreCase(home.getName())
				&& home.distanceTo(n.getWorldLocation().getX(), n.getWorldLocation().getY()) <= 3)
			{
				return n;
			}
		}
		return null;
	}

	private static NPC contractorNear(Client client)
	{
		for (NPC n : client.getTopLevelWorldView().npcs())
		{
			if (n != null && n.getName() != null && Home.City.isContractor(n.getName()) && hasContractOption(n))
			{
				return n;
			}
		}
		return null;
	}

	private static boolean hasContractOption(NPC n)
	{
		if (n.getComposition() == null || n.getComposition().getActions() == null)
		{
			return false;
		}
		for (String a : n.getComposition().getActions())
		{
			if ("Contract".equalsIgnoreCase(a))
			{
				return true;
			}
		}
		return false;
	}
}
