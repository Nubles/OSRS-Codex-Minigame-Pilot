package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Contract;
import com.codex.minigamepilot.core.Home;
import com.codex.minigamepilot.core.ItemIds;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.ui.Theme;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
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
	private int points;
	private long assignedAt;

	HomesPilot(Contract saved, Consumer<Contract> save)
	{
		this.contract = saved;
		this.save = save;
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

	View.Brief brief()
	{
		String status = contract == null ? "No contract open"
			: (contract.getTier() == null ? "A" : contract.getTier().getName()) + " contract for " + contract.getHome().getName()
			+ " in " + contract.getHome().getCity().getName();
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
