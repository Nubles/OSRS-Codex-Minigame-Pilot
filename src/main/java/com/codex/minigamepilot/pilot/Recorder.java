package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Pace;
import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.ui.View;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

/**
 * Decides what goes in the field notes. Only changes are written: a new step, a target that could
 * not be found, a planner snapshot that differs from the last, a varbit or message while in a
 * minigame, a click, a finished round, and every so often the objects and characters around you
 * that the Pilot has not noted yet, each marked with whether the Pilot was tracking it.
 */
final class Recorder
{
	/** Ticks between looks around, about a minute. */
	private static final int SURVEY_EVERY = 100;
	/** How far around you a look reaches, in tiles. */
	private static final int SURVEY_RANGE = 16;
	private static final int SURVEY_CAP = 300;

	private final FieldNotes notes;
	private View.Activity activity = View.Activity.NONE;
	private String lastStep;
	private String lastMiss;
	private String lastState;
	private int sinceSurvey;
	private final Set<String> surveyed = new HashSet<>();

	Recorder(FieldNotes notes)
	{
		this.notes = notes;
	}

	boolean on()
	{
		return notes.isOpen();
	}

	View.Activity activity()
	{
		return activity;
	}

	void tick(Client client, SceneIndex scene, Flight flight, Object state, Pace.Round round)
	{
		if (!on())
		{
			return;
		}
		Player me = client.getLocalPlayer();
		WorldPoint at = me == null ? null : me.getWorldLocation();
		if (flight.activity != activity)
		{
			JsonObject b = new JsonObject();
			b.addProperty("from", activity.name());
			b.addProperty("to", flight.activity.name());
			b.add("at", point(at));
			if (at != null)
			{
				b.addProperty("region", at.getRegionID());
			}
			notes.note("activity", b);
			activity = flight.activity;
			lastStep = null;
			lastMiss = null;
			lastState = null;
			surveyed.clear();
			sinceSurvey = SURVEY_EVERY;
		}
		if (activity == View.Activity.NONE)
		{
			return;
		}

		Step step = flight.step;
		if (step != null)
		{
			String key = step.getVerb() + "|" + step.getTitle() + "|" + step.getTarget() + "|" + flight.view.loopAt()
				+ "|" + (flight.object == null ? "" : flight.object.getWorldLocation())
				+ "|" + (flight.npc == null ? "" : flight.npc.getIndex());
			if (!key.equals(lastStep))
			{
				lastStep = key;
				JsonObject b = new JsonObject();
				b.addProperty("verb", step.getVerb());
				b.addProperty("title", step.getTitle());
				b.addProperty("detail", step.getDetail());
				b.addProperty("target", step.getTarget().name());
				b.addProperty("waiting", step.isWaiting());
				b.addProperty("stage", flight.view.loopAt());
				b.add("at", point(at));
				if (flight.object != null)
				{
					JsonObject o = new JsonObject();
					o.addProperty("id", flight.object.getId());
					o.add("at", point(flight.object.getWorldLocation()));
					b.add("object", o);
				}
				if (flight.npc != null)
				{
					JsonObject n = new JsonObject();
					n.addProperty("id", flight.npc.getId());
					n.addProperty("name", flight.npc.getName());
					n.add("at", point(flight.npc.getWorldLocation()));
					b.add("npc", n);
				}
				b.addProperty("route", flight.route == null ? 0 : flight.route.size());
				if (flight.destination != null)
				{
					b.add("destination", point(flight.destination));
				}
				JsonArray items = new JsonArray();
				for (int id : step.getItems())
				{
					items.add(id);
				}
				if (items.size() > 0)
				{
					b.add("items", items);
				}
				notes.note("step", b);
			}

			// A step about something in the world that the Pilot could not find to light up.
			boolean worldTarget = step.getTarget() != Step.Target.NONE && step.getTarget() != Step.Target.ITEM;
			String miss = worldTarget && flight.object == null && flight.npc == null && flight.destination == null
				? step.getTarget() + "|" + step.getTitle() : null;
			if (!Objects.equals(miss, lastMiss))
			{
				lastMiss = miss;
				if (miss != null)
				{
					JsonObject b = new JsonObject();
					b.addProperty("target", step.getTarget().name());
					b.addProperty("title", step.getTitle());
					b.add("at", point(at));
					notes.note("miss", b);
				}
			}
		}

		if (state != null)
		{
			JsonObject tree = notes.tree(state);
			String text = tree.toString();
			if (!text.equals(lastState))
			{
				lastState = text;
				tree.addProperty("of", state.getClass().getName().replaceAll(".*\\.", ""));
				notes.note("state", tree);
			}
		}

		if (round != null)
		{
			notes.note("round", notes.tree(round));
		}

		if (++sinceSurvey >= SURVEY_EVERY && at != null)
		{
			sinceSurvey = 0;
			survey(client, scene, at);
		}
	}

	void varbit(int id, int value)
	{
		if (on() && activity != View.Activity.NONE && id >= 0)
		{
			JsonObject b = new JsonObject();
			b.addProperty("id", id);
			b.addProperty("v", value);
			notes.note("varbit", b);
		}
	}

	void chat(String type, String message)
	{
		if (on() && activity != View.Activity.NONE)
		{
			JsonObject b = new JsonObject();
			b.addProperty("type", type);
			b.addProperty("text", message);
			notes.note("chat", b);
		}
	}

	void click(String option, String target, int id, String action, int itemId)
	{
		if (on() && activity != View.Activity.NONE)
		{
			JsonObject b = new JsonObject();
			b.addProperty("option", option);
			b.addProperty("target", target);
			b.addProperty("id", id);
			b.addProperty("action", action);
			if (itemId > 0)
			{
				b.addProperty("item", itemId);
			}
			notes.note("click", b);
		}
	}

	void inventory(Map<Integer, Integer> changes)
	{
		if (on() && activity != View.Activity.NONE && !changes.isEmpty())
		{
			JsonObject b = new JsonObject();
			JsonObject c = new JsonObject();
			changes.forEach((id, n) -> c.addProperty(String.valueOf(id), n));
			b.add("changes", c);
			notes.note("inventory", b);
		}
	}

	/** Notes objects and characters nearby that have not been noted yet this visit. */
	private void survey(Client client, SceneIndex scene, WorldPoint at)
	{
		JsonArray objects = new JsonArray();
		Tile[][][] tiles = client.getTopLevelWorldView().getScene().getTiles();
		int plane = client.getTopLevelWorldView().getPlane();
		if (plane >= 0 && plane < tiles.length)
		{
			for (Tile[] row : tiles[plane])
			{
				for (Tile tile : row)
				{
					if (tile == null || objects.size() >= SURVEY_CAP)
					{
						continue;
					}
					if (tile.getWorldLocation().distanceTo(at) > SURVEY_RANGE)
					{
						continue;
					}
					for (GameObject o : tile.getGameObjects())
					{
						object(client, scene, o, objects);
					}
					object(client, scene, tile.getWallObject(), objects);
					object(client, scene, tile.getDecorativeObject(), objects);
					object(client, scene, tile.getGroundObject(), objects);
				}
			}
		}
		JsonArray npcs = new JsonArray();
		for (NPC n : client.getTopLevelWorldView().npcs())
		{
			if (n == null || n.getName() == null || n.getWorldLocation().distanceTo(at) > SURVEY_RANGE)
			{
				continue;
			}
			String key = "n" + n.getId() + "@" + n.getIndex();
			if (surveyed.add(key))
			{
				JsonObject j = new JsonObject();
				j.addProperty("id", n.getId());
				j.addProperty("name", n.getName());
				j.add("at", point(n.getWorldLocation()));
				npcs.add(j);
			}
		}
		if (objects.size() == 0 && npcs.size() == 0)
		{
			return;
		}
		JsonObject b = new JsonObject();
		b.add("at", point(at));
		b.add("objects", objects);
		b.add("npcs", npcs);
		notes.note("survey", b);
	}

	private void object(Client client, SceneIndex scene, TileObject o, JsonArray into)
	{
		if (o == null || into.size() >= SURVEY_CAP)
		{
			return;
		}
		WorldPoint where = o.getWorldLocation();
		String key = o.getId() + "@" + where;
		if (surveyed.contains(key))
		{
			return;
		}
		ObjectComposition def = client.getObjectDefinition(o.getId());
		if (def == null)
		{
			return;
		}
		int shown = o.getId();
		if (def.getImpostorIds() != null)
		{
			ObjectComposition now = def.getImpostor();
			if (now != null)
			{
				def = now;
				shown = now.getId();
			}
		}
		String name = def.getName();
		String[] actions = def.getActions();
		boolean any = false;
		if (actions != null)
		{
			for (String a : actions)
			{
				any |= a != null;
			}
		}
		if (name == null || "null".equals(name) || !any)
		{
			return;
		}
		surveyed.add(key);
		JsonObject j = new JsonObject();
		j.addProperty("id", o.getId());
		if (shown != o.getId())
		{
			j.addProperty("shows", shown);
		}
		j.addProperty("name", name);
		JsonArray acts = new JsonArray();
		for (String a : actions)
		{
			if (a != null)
			{
				acts.add(a);
			}
		}
		j.add("actions", acts);
		j.add("at", point(where));
		j.addProperty("tracked", scene.knows(o));
		into.add(j);
	}

	private static JsonArray point(WorldPoint p)
	{
		JsonArray a = new JsonArray();
		if (p != null)
		{
			a.add(p.getX());
			a.add(p.getY());
			a.add(p.getPlane());
		}
		return a;
	}
}
