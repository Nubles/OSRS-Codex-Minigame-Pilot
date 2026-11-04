package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.BankStock;
import com.codex.minigamepilot.core.Bar;
import com.codex.minigamepilot.core.BestVisit;
import com.codex.minigamepilot.core.Contract;
import com.codex.minigamepilot.core.Pace;
import com.codex.minigamepilot.core.Visit;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Scene;
import net.runelite.api.Tile;

/** The pilots, the scene they share, and which one is flying this tick. */
public final class Pilots
{
	private final Client client;
	private final SceneIndex scene;
	private final MotherlodePilot motherlode = new MotherlodePilot();
	private final FurnacePilot furnace = new FurnacePilot();
	private final HomesPilot homes;
	private final RiftPilot rift = new RiftPilot();
	private volatile boolean useRift = true;
	private final StormPilot storm = new StormPilot();
	private volatile boolean useStorm = true;
	private final FoundryPilot foundry = new FoundryPilot();
	private volatile boolean useFoundry = true;
	private final MixologyPilot mixology = new MixologyPilot();
	private volatile boolean useMixology = true;
	private final TotemsPilot totems = new TotemsPilot();
	private volatile boolean useTotems = true;
	private final TithePilot tithe = new TithePilot();
	private volatile boolean useTithe = true;
	private final ArtefactsPilot artefacts = new ArtefactsPilot();
	private volatile boolean useArtefacts = true;
	private final HousesPilot valuables = new HousesPilot();
	private volatile boolean useValuables = true;
	private final RoguesDenPilot den = new RoguesDenPilot();
	private volatile boolean useDen = true;
	private final AssaultPilot assault = new AssaultPilot();
	private volatile boolean useAssault = true;
	private final WintertodtPilot wintertodt = new WintertodtPilot();
	private volatile boolean useWintertodt = true;
	private final GnomeRestaurantPilot gnome = new GnomeRestaurantPilot();
	private volatile boolean useGnome = true;
	private final NmzPilot nmz = new NmzPilot();
	private volatile boolean useNmz = true;
	private final VolcanicPilot volcanic = new VolcanicPilot();
	private volatile boolean useVolcanic = true;
	private final Coach coach;
	private final Recorder recorder;
	private final IntUnaryOperator price;
	private final Supplier<BankStock> stock;
	private View.Activity last = View.Activity.NONE;
	private final Function<String, String> load;
	private final BiConsumer<String, String> saveKey;
	/** A summary of the visit just ended, for the chat, or null. */
	private String note;
	/** The visit last said in the chat for each minigame, so stepping out again says nothing new. */
	private final java.util.Map<View.Activity, Visit> said = new java.util.EnumMap<>(View.Activity.class);
	/** Where the summary of each minigame's last visit is kept: this and the minigame's name. */
	static final String VISIT_KEY = "visit.";

	/**
	 * @param loadBests the pace coach's saved bests for a minigame
	 * @param saveBests saves them again when a round beats one
	 * @param load reads a key of this account's saved settings
	 * @param saveKey writes one
	 * @param stock the supplies in your bank as last seen, or null when no bank has been seen
	 */
	public Pilots(Client client, Contract saved, Consumer<Contract> save, FieldNotes notes,
		Function<View.Activity, String> loadBests, BiConsumer<View.Activity, String> saveBests, IntUnaryOperator price,
		Function<String, String> load, BiConsumer<String, String> saveKey, Supplier<BankStock> stock)
	{
		this.client = client;
		this.load = load;
		this.saveKey = saveKey;
		this.price = price;
		this.stock = stock;
		motherlode.setPrice(price);
		motherlode.setStore(load, saveKey);
		rift.setStore(load, saveKey);
		storm.setStore(load, saveKey);
		foundry.setStore(load, saveKey);
		mixology.setStore(load, saveKey);
		mixology.setPrice(price);
		totems.setStore(load, saveKey);
		tithe.setStore(load, saveKey);
		artefacts.setStore(load, saveKey);
		valuables.setStore(load, saveKey);
		valuables.setPrice(price);
		den.setStore(load, saveKey);
		assault.setStore(load, saveKey);
		wintertodt.setStore(load, saveKey);
		gnome.setStore(load, saveKey);
		gnome.setPrice(price);
		nmz.setStore(load, saveKey);
		nmz.setPrice(price);
		volcanic.setStore(load, saveKey);
		this.coach = new Coach(loadBests, saveBests);
		this.recorder = new Recorder(notes);
		this.scene = new SceneIndex(client, id -> MotherlodePilot.isWatched(id) || FurnacePilot.isWatched(id) || HomesPilot.isWatched(id)
			|| RiftPilot.isWatched(id) || StormPilot.isWatched(id) || FoundryPilot.isWatched(id)
			|| MixologyPilot.isWatched(id) || TotemsPilot.isWatched(id) || TithePilot.isWatched(id)
			|| ArtefactsPilot.isWatched(id) || HousesPilot.isWatched(id) || RoguesDenPilot.isWatched(id)
			|| AssaultPilot.isWatched(id) || WintertodtPilot.isWatched(id) || NmzPilot.isWatched(id) || VolcanicPilot.isWatched(id));
		this.homes = new HomesPilot(saved, save, load, saveKey);
		homes.readers(null, stock);
	}

	private volatile boolean tickCoach = true;

	/** The "Tick coach" setting: the tick coach's card on the panel. */
	public void tickCoach(boolean on)
	{
		tickCoach = on;
	}

	/** Where Codex Wayfinder's shared atlas is read, for the quickest way to a Mahogany Homes client. */
	public void atlas(Supplier<com.codex.minigamepilot.core.WayfinderAtlas> atlas)
	{
		homes.readers(atlas, stock);
		gnome.setAtlas(atlas);
	}

	/** The "Gnome Restaurant" setting. */
	public void gnomeRestaurant(boolean on)
	{
		useGnome = on;
	}

	/** The "Nightmare Zone" settings: on, the reward saved for (or null), and imbues at half price. */
	public void nightmareZone(boolean on, com.codex.minigamepilot.core.Nmz.Reward reward, boolean halved)
	{
		useNmz = on;
		nmz.settings(reward, halved);
	}

	/** The "Volcanic Mine" settings: on, and the reward saved for (or null). */
	public void volcanicMine(boolean on, com.codex.minigamepilot.core.Volcanic.Reward reward)
	{
		useVolcanic = on;
		volcanic.settings(reward);
	}

	/** After a Mahogany Homes hand-in, a line when the planks left run low, once, or null. */
	public String takeLowPlanks()
	{
		return homes.takeLowNote();
	}

	/** The "Guardians of the Rift" setting. */
	public void rift(boolean on)
	{
		useRift = on;
	}

	/** The "Fragments before the first craft" setting for Guardians of the Rift. */
	public void riftFragments(int fragments)
	{
		rift.setFirstTarget(fragments);
	}

	/**
	 * How Guardians of the Rift reads quests and its HUD's rune icons; by default it asks the game.
	 * Package-private: replays set them from the script.
	 */
	void riftReaders(java.util.function.Predicate<net.runelite.api.Quest> quest, IntUnaryOperator sprite)
	{
		rift.readers(quest, sprite);
	}

	/** The "Tempoross" settings: on, cook before loading, and the storm at which to load what you hold. */
	public void tempoross(boolean on, boolean cook, int stormLoad)
	{
		useStorm = on;
		storm.setCook(cook);
		storm.setStormLoad(stormLoad);
	}

	/** The "Giants' Foundry" setting. */
	public void foundry(boolean on)
	{
		useFoundry = on;
	}

	/** The "Mastering Mixology" settings: on, which orders to make, the reward saved for (or null) and the station cues. */
	public void mixology(boolean on, com.codex.minigamepilot.core.MixCycle.Plan plan, com.codex.minigamepilot.core.Mixology.Reward reward,
		boolean cues)
	{
		useMixology = on;
		mixology.settings(plan, reward, cues);
	}

	/** The "Vale Totems" settings: on, the wood to build with (null for the best), claiming as you pass, and the reward saved for (or null). */
	public void totems(boolean on, com.codex.minigamepilot.core.Totems.Wood wood, boolean claim, com.codex.minigamepilot.core.Totems.Reward reward)
	{
		useTotems = on;
		totems.settings(wood, claim, reward);
	}

	/** The "Tithe Farm" settings: on, plants a round, and the reward saved for (or null). */
	public void tithe(boolean on, com.codex.minigamepilot.core.Tithe.Plan plan, com.codex.minigamepilot.core.Tithe.Reward reward)
	{
		useTithe = on;
		tithe.settings(plan, reward);
	}

	/** The "Stealing artefacts" settings: on, mark the patrols, and the Thieving level to work toward (0 for the next). */
	public void artefacts(boolean on, boolean patrols, int level)
	{
		useArtefacts = on;
		artefacts.settings(patrols, level);
	}

	/** The "Stealing valuables" settings: on, mark the homeowners, and the Thieving level to work toward (0 for the next). */
	public void valuables(boolean on, boolean owners, int level)
	{
		useValuables = on;
		valuables.settings(owners, level);
	}

	/** The "Rogues' Den" settings: on, and mark the maze's traps. */
	public void roguesDen(boolean on, boolean traps)
	{
		useDen = on;
		den.settings(traps);
	}

	/** The "Barbarian Assault" settings: on, and the reward saved for (or null). */
	public void barbarianAssault(boolean on, com.codex.minigamepilot.core.Assault.Reward reward)
	{
		useAssault = on;
		assault.settings(reward);
	}

	/** The "Wintertodt" settings: on, fletching, the warmth to warm up at, and the reward saved for (or null). */
	public void wintertodt(boolean on, boolean fletch, int eatAt, com.codex.minigamepilot.core.Wintertodt.Reward reward)
	{
		useWintertodt = on;
		wintertodt.settings(fletch, eatAt, reward);
	}

	/** A spot animation appeared in the scene: Mastering Mixology's station cues. */
	public void spotanim(int id)
	{
		mixology.spotanim(id, client.getTickCount());
	}

	/** How the Giants' Foundry reads its HUD's stage icons and sweet spot; by default it asks the game. Package-private: replays set them. */
	void foundryHud(FoundryPilot.Hud hud)
	{
		foundry.setHud(hud);
	}

	/** The "Upper hopper unlocked" setting for the Motherlode Mine. */
	public void upperHopper(boolean on)
	{
		motherlode.setUpperHopper(on);
	}

	public SceneIndex scene()
	{
		return scene;
	}

	public void clearScene()
	{
		scene.clear();
	}

	/** The last account that was logged in, so logging out and back in is not a new one. */
	private String account;

	/**
	 * The save profile changed. It changes to none on every logout and back on login, so only a
	 * login to a different account than the last starts every session over: back on the same
	 * account soon, the session goes on as it always has.
	 *
	 * @param profile the profile now, or null while logged out
	 */
	public void profile(String profile)
	{
		if (switched(account, profile))
		{
			motherlode.newAccount();
			furnace.newAccount();
			homes.newAccount();
			rift.newAccount();
			storm.newAccount();
			foundry.newAccount();
			mixology.newAccount();
			totems.newAccount();
			tithe.newAccount();
			artefacts.newAccount();
			valuables.newAccount();
			den.newAccount();
			assault.newAccount();
			wintertodt.newAccount();
			gnome.newAccount();
			nmz.newAccount();
			volcanic.newAccount();
			said.clear();
			unsaid.clear();
		}
		if (profile != null)
		{
			account = profile;
		}
	}

	/** Whether a login to {@code profile} is another account than {@code last}, the last one logged in. */
	static boolean switched(String last, String profile)
	{
		return profile != null && last != null && !profile.equals(last);
	}

	public void loadContract(Contract saved)
	{
		homes.load(saved);
		homes.reloadBook();
		coach.reload();
	}

	public void chat(String type, String message)
	{
		recorder.chat(type, message);
		furnace.coalBag.chat(message);
		homes.chat(message);
		rift.chat(message);
		storm.chat(message);
		foundry.chat(message);
		valuables.chat(message);
		assault.chat(message, client.getTickCount());
		wintertodt.chat(message);
		volcanic.chat(message);
		gnome.chat(message, client.getTickCount());
	}

	/** @param bankCoal coal in the open bank, or -1 when the bank is closed */
	public void coalBagOption(String option, int inventoryCoal, int bankCoal)
	{
		furnace.coalBag.option(option, inventoryCoal, bankCoal);
	}

	public void varbit(int id, int value)
	{
		recorder.varbit(id, value);
	}

	public void click(String option, String target, int id, String action, int itemId)
	{
		recorder.click(option, target, id, action, itemId);
	}

	public void inventory(Map<Integer, Integer> changes)
	{
		recorder.inventory(changes);
	}

	/** Indexes what is already loaded, for when the plugin starts while logged in. */
	public void rescan()
	{
		scene.clear();
		Scene s = client.getTopLevelWorldView().getScene();
		Tile[][][] tiles = s.getTiles();
		for (Tile[][] plane : tiles)
		{
			for (Tile[] row : plane)
			{
				for (Tile tile : row)
				{
					if (tile == null)
					{
						continue;
					}
					for (GameObject o : tile.getGameObjects())
					{
						scene.add(o);
					}
					scene.add(tile.getWallObject());
					scene.add(tile.getDecorativeObject());
					scene.add(tile.getGroundObject());
				}
			}
		}
	}

	/** The characters in the scene. */
	private List<NPC> npcs()
	{
		List<NPC> out = new ArrayList<>();
		for (NPC n : client.getTopLevelWorldView().npcs())
		{
			out.add(n);
		}
		return out;
	}

	/** Works out this tick's guidance. Only ever reads the game. */
	public Flight fly(boolean useMotherlode, boolean useFurnace, boolean useHomes, Bar bar, int cofferLow, long now)
	{
		View.Activity activity;
		Flight flight;
		Object state = null;
		int units = 0;
		String kind = null;
		// Barbarian Assault's games are followed every tick, so one is kept even while you stand elsewhere.
		if (useAssault)
		{
			assault.watch(client, now);
		}
		// A Wintertodt round is followed every tick too, so leaving the prison mid-round is seen.
		if (useWintertodt)
		{
			wintertodt.watch(client, now);
		}
		// A Gnome Restaurant order is followed every tick, so a delivery is kept wherever it is handed over.
		if (useGnome)
		{
			gnome.watch(client, now);
		}
		// A Nightmare Zone dream is followed every tick too, so waking or logging out ends it.
		if (useNmz)
		{
			nmz.watch(client, now);
		}
		// A Volcanic Mine game is followed every tick too, so coming out or logging out ends it.
		if (useVolcanic)
		{
			volcanic.watch(client, now);
		}
		if (useMotherlode && MotherlodePilot.inMine(client))
		{
			activity = View.Activity.MOTHERLODE;
			flight = motherlode.fly(client, scene, now);
			state = motherlode.lastState;
			units = motherlode.units();
		}
		else if (useFurnace && FurnacePilot.atFurnace(client))
		{
			activity = View.Activity.FURNACE;
			flight = furnace.fly(client, scene, bar, cofferLow, now);
			state = furnace.lastState;
			units = furnace.units();
		}
		else if (useHomes && homes.active(client, now))
		{
			activity = View.Activity.HOMES;
			List<NPC> npcs = new ArrayList<>();
			for (NPC n : client.getTopLevelWorldView().npcs())
			{
				npcs.add(n);
			}
			flight = homes.fly(client, scene, npcs, now);
			state = homes.lastState;
			units = homes.units();
			kind = homes.kindHint();
		}
		else if (useRift && rift.active(client, scene))
		{
			activity = View.Activity.RIFT;
			flight = rift.fly(client, scene, now);
			state = rift.lastState;
			units = rift.units();
		}
		else if (useStorm && storm.active(client))
		{
			activity = View.Activity.TEMPOROSS;
			flight = storm.fly(client, scene, now);
			state = storm.lastState;
			units = storm.units();
		}
		else if (useFoundry && foundry.active(client, scene))
		{
			activity = View.Activity.FOUNDRY;
			flight = foundry.fly(client, scene, now);
			state = foundry.lastState;
			units = foundry.units();
		}
		else if (useMixology && mixology.active(client, scene))
		{
			mixology.settle(client, now);
			activity = View.Activity.MIXOLOGY;
			flight = mixology.fly(client, scene, now);
			state = mixology.lastState;
			units = mixology.units();
		}
		else if (useTotems && totems.active(client, scene))
		{
			activity = View.Activity.TOTEMS;
			flight = totems.fly(client, scene, now);
			state = totems.lastState;
			units = totems.units();
		}
		else if (useTithe && tithe.active(client, scene))
		{
			activity = View.Activity.TITHE;
			flight = tithe.fly(client, scene, now);
			state = tithe.lastState;
			units = tithe.units();
		}
		else if (useArtefacts && artefacts.active(client, npcs()))
		{
			activity = View.Activity.ARTEFACTS;
			flight = artefacts.fly(client, scene, npcs(), now);
			state = artefacts.lastState;
			units = artefacts.units();
		}
		else if (useValuables && valuables.active(client, scene, npcs()))
		{
			activity = View.Activity.HOUSES;
			flight = valuables.fly(client, scene, npcs(), now);
			state = valuables.lastState;
			units = valuables.units();
		}
		else if (useDen && den.active(client, scene, npcs()))
		{
			activity = View.Activity.ROGUES_DEN;
			flight = den.fly(client, scene, npcs(), now);
			state = den.lastState;
			units = den.units();
		}
		else if (useWintertodt && wintertodt.active(client, scene))
		{
			activity = View.Activity.WINTERTODT;
			flight = wintertodt.fly(client, scene, npcs(), now);
			state = wintertodt.lastState;
			units = wintertodt.units();
		}
		else if (useGnome && gnome.active(client, npcs()))
		{
			activity = View.Activity.GNOME_RESTAURANT;
			flight = gnome.fly(client, npcs(), now);
			state = gnome.lastState;
			units = gnome.units();
		}
		else if (useNmz && nmz.active(client))
		{
			activity = View.Activity.NIGHTMARE_ZONE;
			flight = nmz.fly(client, scene, npcs(), now);
			state = nmz.lastState;
			units = nmz.units();
		}
		else if (useVolcanic && volcanic.active(client))
		{
			activity = View.Activity.VOLCANIC_MINE;
			flight = volcanic.fly(client, npcs(), scene, now);
			state = volcanic.lastState;
			units = volcanic.units();
		}
		else if (useAssault && assault.active(client, scene, npcs()))
		{
			activity = View.Activity.BARBARIAN_ASSAULT;
			flight = assault.fly(client, npcs(), now);
			state = assault.lastState;
			units = assault.units();
		}
		else
		{
			activity = View.Activity.NONE;
			flight = Flight.idle(idleView());
		}
		if (activity != last)
		{
			if (last == View.Activity.MOTHERLODE)
			{
				keep(last, motherlode.visit(now), motherlode.pace(now), true);
				motherlode.left();
			}
			else if (last == View.Activity.FURNACE)
			{
				keep(last, furnace.visit(now), furnace.pace(now), true);
				furnace.left();
			}
			else if (last == View.Activity.RIFT)
			{
				rift.left();
			}
			else if (last == View.Activity.TEMPOROSS)
			{
				storm.left(client.getTickCount(), now);
			}
			else if (last == View.Activity.FOUNDRY)
			{
				foundry.left();
			}
			else if (last == View.Activity.MIXOLOGY)
			{
				mixology.left();
			}
			else if (last == View.Activity.TOTEMS)
			{
				totems.left();
			}
			else if (last == View.Activity.TITHE)
			{
				tithe.left(now);
			}
			else if (last == View.Activity.ARTEFACTS)
			{
				artefacts.left();
			}
			else if (last == View.Activity.HOUSES)
			{
				valuables.left(now);
			}
			else if (last == View.Activity.ROGUES_DEN)
			{
				den.left(now);
			}
			else if (last == View.Activity.BARBARIAN_ASSAULT)
			{
				assault.left();
			}
			else if (last == View.Activity.WINTERTODT)
			{
				wintertodt.left();
			}
			else if (last == View.Activity.GNOME_RESTAURANT)
			{
				gnome.left();
			}
			else if (last == View.Activity.NIGHTMARE_ZONE)
			{
				nmz.left();
			}
			else if (last == View.Activity.VOLCANIC_MINE)
			{
				volcanic.left();
			}
			last = activity;
		}
		homes.settle(client.getSkillExperience(net.runelite.api.Skill.CONSTRUCTION), now);
		storm.settle(client);
		foundry.settle(client, now);
		Pace.Round round = coach.tick(client, flight, units, kind);
		coach.decorate(flight.view, activity);
		if (tickCoach)
		{
			coach.tickCard(flight.view, activity);
		}
		BankStock banked = stock == null ? null : stock.get();
		if (activity == View.Activity.FURNACE)
		{
			double tripSeconds = typicalTicks(coach.pace(View.Activity.FURNACE)) * 0.6;
			if (price != null)
			{
				flight.view.pick(furnace.pick(client, price, tripSeconds));
			}
			flight.view.supply(furnace.supply(banked, tripSeconds, now));
		}
		else if (activity == View.Activity.HOMES)
		{
			int level = client.getRealSkillLevel(net.runelite.api.Skill.CONSTRUCTION);
			flight.view.pick(homes.costs(price, level));
			flight.view.supply(homes.supply(banked, level, now));
		}
		recorder.tick(client, scene, flight, state, round);
		return flight;
	}

	/**
	 * Logged out or hopping. What you carry, your experience and the minigames' varbits arrive
	 * again over the first ticks after, so nothing seen before is a base for change: a full
	 * pack read as empty for a tick, then full, is no haul. The visit under way is summed up
	 * quietly; its session goes on if you are back soon.
	 */
	public void relogged(long now)
	{
		if (last == View.Activity.MOTHERLODE)
		{
			keep(last, motherlode.visit(now), motherlode.pace(now), false);
		}
		else if (last == View.Activity.FURNACE)
		{
			keep(last, furnace.visit(now), furnace.pace(now), false);
		}
		else if (last == View.Activity.RIFT)
		{
			rift.left();
		}
		else if (last == View.Activity.TEMPOROSS)
		{
			// Logged out or hopped mid-game: that game is over, and must not run on into the next.
			storm.left(client.getTickCount(), now);
		}
		else if (last == View.Activity.FOUNDRY)
		{
			foundry.left();
		}
		else if (last == View.Activity.MIXOLOGY)
		{
			mixology.left();
		}
		else if (last == View.Activity.TOTEMS)
		{
			totems.left();
		}
		else if (last == View.Activity.TITHE)
		{
			// Logged out or hopped: the farm's game is over, and its sack with it.
			tithe.left(now);
		}
		else if (last == View.Activity.ARTEFACTS)
		{
			// Logging out takes the artefact: one carried is lost.
			artefacts.relogged(now);
		}
		else if (last == View.Activity.HOUSES)
		{
			// Logging out and in leaves a house with what you hold.
			valuables.relogged(now);
		}
		else if (last == View.Activity.ROGUES_DEN)
		{
			// A run under way ends where it stood.
			den.relogged(now);
		}
		// A Barbarian Assault game under way ends where it stood, wherever you were.
		assault.relogged(now, client.getTickCount());
		// A Wintertodt round under way is lost on logging out.
		wintertodt.relogged(now, client.getTickCount());
		// A Gnome Restaurant order's clock goes on in the save; it is picked up again on login.
		gnome.relogged();
		// A Nightmare Zone dream ends on logging out.
		nmz.relogged(now, client.getTickCount());
		// A Volcanic Mine game ends on logging out.
		volcanic.relogged(client, now, client.getTickCount());
		motherlode.relogged();
		furnace.relogged();
		coach.left();
		last = View.Activity.NONE;
	}

	/**
	 * Saves a visit's summary and weighs its pace against your best visit, and holds both for
	 * the chat when it was walked out of. Package-private for tests.
	 */
	void keep(View.Activity activity, Visit visit, BestVisit pace, boolean say)
	{
		if (visit == null)
		{
			return;
		}
		if (saveKey != null)
		{
			saveKey.accept(VISIT_KEY + activity.name(), visit.encode());
		}
		String beat = null;
		String unit = activity == View.Activity.FURNACE ? FurnacePilot.UNIT : MotherlodePilot.UNIT;
		BestVisit held = best(activity);
		BestVisit.Outcome outcome = BestVisit.judge(held, pace);
		if (outcome != BestVisit.Outcome.KEPT && saveKey != null)
		{
			saveKey.accept(BEST_KEY + activity.name(), pace.encode());
			if (outcome == BestVisit.Outcome.BEATEN)
			{
				if (say)
				{
					beat = pace.beat(held, unit);
				}
				else
				{
					// Beaten at a hop or logout, where nothing is said: once the session goes on,
					// its later readings only weigh against its own, so keep what it beat.
					unsaid.put(activity, held);
				}
			}
		}
		if (say)
		{
			BestVisit from = unsaid.remove(activity);
			BestVisit record = beat == null && from != null ? best(activity) : null;
			if (record != null && record.sameSession(pace))
			{
				beat = record.beat(from, unit);
			}
		}
		// A session carries on when you step out and back in, so leaving again with nothing more
		// done would say the same counts twice.
		if (say && !visit.sameCounts(said.get(activity)))
		{
			note = activity.getTitle() + ", " + visit.line() + (beat == null ? "" : " " + beat);
			said.put(activity, visit);
		}
	}

	/** The other session's record a visit beat while nothing could be said, to say as it is walked out of. */
	private final java.util.Map<View.Activity, BestVisit> unsaid = new java.util.EnumMap<>(View.Activity.class);

	/** Where each minigame's fastest visit is kept: this and the minigame's name. */
	static final String BEST_KEY = "best.";

	/** Your fastest visit to a minigame, or null. */
	private BestVisit best(View.Activity activity)
	{
		return load == null ? null : BestVisit.decode(load.apply(BEST_KEY + activity.name()));
	}

	/** The summary of a visit just walked out of, once, or null. */
	public String takeNote()
	{
		String n = note;
		note = null;
		return n;
	}

	/** The summary saved when a minigame was last left, or null. */
	private Visit lastVisit(View.Activity activity)
	{
		return load == null ? null : Visit.decode(load.apply(VISIT_KEY + activity.name()));
	}

	/** The median of this session's timed rounds, of any kind, or 0 before there are three. */
	static int typicalTicks(Pace pace)
	{
		List<Integer> t = new ArrayList<>();
		for (Pace.Round r : pace.rounds())
		{
			if (!r.hadBreak)
			{
				t.add(r.ticks);
			}
		}
		if (t.size() < 3)
		{
			return 0;
		}
		java.util.Collections.sort(t);
		return t.get(t.size() / 2);
	}

	/** The panel away from the minigames: each one, and what is waiting there. */
	public View idleView()
	{
		View v = new View(true, View.Activity.NONE, null, null, null, -1);
		v.brief(homes.brief(stock == null ? null : stock.get(), client.getRealSkillLevel(net.runelite.api.Skill.CONSTRUCTION)));
		long now = System.currentTimeMillis();
		v.brief(motherlode.brief(lastVisit(View.Activity.MOTHERLODE), best(View.Activity.MOTHERLODE), now));
		v.brief(furnace.brief(lastVisit(View.Activity.FURNACE), best(View.Activity.FURNACE), now));
		v.brief(rift.brief());
		v.brief(storm.brief());
		v.brief(foundry.brief());
		v.brief(mixology.brief());
		v.brief(totems.brief());
		v.brief(tithe.brief());
		v.brief(artefacts.brief());
		v.brief(valuables.brief());
		v.brief(den.brief());
		v.brief(assault.brief());
		v.brief(wintertodt.brief());
		v.brief(gnome.brief());
		v.brief(nmz.brief());
		v.brief(volcanic.brief());
		return v;
	}
}
