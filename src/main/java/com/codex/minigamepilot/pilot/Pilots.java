package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Bar;
import com.codex.minigamepilot.core.Contract;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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
	private View.Activity last = View.Activity.NONE;

	public Pilots(Client client, Contract saved, Consumer<Contract> save)
	{
		this.client = client;
		this.scene = new SceneIndex(client, id -> MotherlodePilot.isWatched(id) || FurnacePilot.isWatched(id) || HomesPilot.isWatched(id));
		this.homes = new HomesPilot(saved, save);
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
	}

	public void chat(String message)
	{
		furnace.coalBag.chat(message);
		homes.chat(message);
	}

	/** @param bankCoal coal in the open bank, or -1 when the bank is closed */
	public void coalBagOption(String option, int inventoryCoal, int bankCoal)
	{
		furnace.coalBag.option(option, inventoryCoal, bankCoal);
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
		if (useMotherlode && MotherlodePilot.inMine(client))
		{
			activity = View.Activity.MOTHERLODE;
			flight = motherlode.fly(client, scene, now);
		}
		else if (useFurnace && FurnacePilot.atFurnace(client))
		{
			activity = View.Activity.FURNACE;
			flight = furnace.fly(client, scene, bar, cofferLow, now);
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
		return flight;
	}

	/** The panel away from the minigames: each one, and what is waiting there. */
	public View idleView()
	{
		View v = new View(true, View.Activity.NONE, null, null, null, -1);
		v.brief(homes.brief());
		v.brief(motherlode.brief());
		v.brief(furnace.brief());
		return v;
	}
}
