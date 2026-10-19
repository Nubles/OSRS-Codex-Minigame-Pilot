package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.BankStock;
import com.codex.minigamepilot.core.Bar;
import com.codex.minigamepilot.core.Contract;
import com.codex.minigamepilot.core.Pace;
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
				motherlode.left();
			}
			else if (last == View.Activity.FURNACE)
			{
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
		v.brief(motherlode.brief());
		v.brief(furnace.brief());
		return v;
	}
}
