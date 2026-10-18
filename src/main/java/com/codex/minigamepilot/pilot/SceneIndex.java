package com.codex.minigamepilot.pilot;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;
import net.runelite.api.Client;
import net.runelite.api.ObjectComposition;
import net.runelite.api.TileObject;

/**
 * The objects in the loaded scene that a pilot cares about, kept up to date from the game's
 * spawn and despawn events so nothing has to sweep the scene every tick. Banks are found by
 * name, since every bank in the game has its own object id.
 */
public final class SceneIndex
{
	private static final Set<String> BANK_NAMES = new HashSet<>(Arrays.asList(
		"bank chest", "bank booth", "bank deposit box"));

	private final Client client;
	private final IntPredicate watched;
	private final Map<Integer, Set<TileObject>> byId = new HashMap<>();
	private final Set<TileObject> banks = new LinkedHashSet<>();
	/** Whether each object id is a bank, worked out once: a region load spawns thousands of objects. */
	private final Map<Integer, Boolean> bankIds = new HashMap<>();

	public SceneIndex(Client client, IntPredicate watched)
	{
		this.client = client;
		this.watched = watched;
	}

	/** Call from the client thread when an object appears. */
	public void add(TileObject o)
	{
		if (o == null)
		{
			return;
		}
		int id = o.getId();
		if (watched.test(id))
		{
			byId.computeIfAbsent(id, k -> new LinkedHashSet<>()).add(o);
		}
		if (isBank(id))
		{
			banks.add(o);
		}
	}

	public void remove(TileObject o)
	{
		if (o == null)
		{
			return;
		}
		Set<TileObject> set = byId.get(o.getId());
		if (set != null)
		{
			set.remove(o);
		}
		banks.remove(o);
	}

	public void clear()
	{
		byId.clear();
		banks.clear();
	}

	public Collection<TileObject> get(int id)
	{
		Set<TileObject> set = byId.get(id);
		return set == null ? Collections.emptySet() : set;
	}

	public Set<TileObject> get(int... ids)
	{
		Set<TileObject> out = new LinkedHashSet<>();
		for (int id : ids)
		{
			out.addAll(get(id));
		}
		return out;
	}

	public Set<TileObject> banks()
	{
		return banks;
	}

	private boolean isBank(int id)
	{
		return bankIds.computeIfAbsent(id, this::lookUpBank);
	}

	private boolean lookUpBank(int id)
	{
		ObjectComposition def = client.getObjectDefinition(id);
		if (def == null)
		{
			return false;
		}
		if (def.getImpostorIds() != null)
		{
			ObjectComposition now = def.getImpostor();
			if (now != null)
			{
				def = now;
			}
		}
		String name = def.getName();
		return name != null && BANK_NAMES.contains(name.toLowerCase());
	}
}
