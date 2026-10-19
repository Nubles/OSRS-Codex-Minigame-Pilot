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

/** The three pilots, the scene they share, and which one is flying this tick. */
public final class Pilots
{
	private final Client client;
	private final SceneIndex scene;
	private final MotherlodePilot motherlode = new MotherlodePilot();
	private final FurnacePilot furnace = new FurnacePilot();
	private final HomesPilot homes;
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
		this.coach = new Coach(loadBests, saveBests);
		this.recorder = new Recorder(notes);
		this.scene = new SceneIndex(client, id -> MotherlodePilot.isWatched(id) || FurnacePilot.isWatched(id) || HomesPilot.isWatched(id));
		this.homes = new HomesPilot(saved, save, load, saveKey);
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
				}
			}
		}
	}

	/** Works out this tick's guidance. Only ever reads the game. */
	public Flight fly(boolean useMotherlode, boolean useFurnace, boolean useHomes, Bar bar, int cofferLow, long now)
	{
		View.Activity activity;
		Flight flight;
		Object state = null;
		int units = 0;
		String kind = null;
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
			last = activity;
		}
		homes.settle(client.getSkillExperience(net.runelite.api.Skill.CONSTRUCTION));
		Pace.Round round = coach.tick(client, flight, units, kind);
		coach.decorate(flight.view, activity);
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
		return v;
	}
}
