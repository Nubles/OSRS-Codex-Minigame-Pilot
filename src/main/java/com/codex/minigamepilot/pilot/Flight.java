package com.codex.minigamepilot.pilot;

import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.api.NPC;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

/**
 * One tick's guidance: the step, the thing in the world to light up, the tiles to walk, and what
 * the panel shows. Overlays draw it; nothing in it is ever acted on by the plugin.
 */
public final class Flight
{
	public final View.Activity activity;
	public final Step step;
	public final View view;
	/** The object the step is about, or null. */
	public TileObject object;
	/** The character the step is about, or null. */
	public NPC npc;
	/** Tiles to walk to reach it, nearest first. */
	public List<WorldPoint> route = Collections.emptyList();
	/** A place beyond the loaded scene to head for, or null. */
	public WorldPoint destination;
	/** Other objects worth a quieter mark, such as the rest of the furniture still to fix. */
	public final List<TileObject> others = new ArrayList<>();

	public Flight(View.Activity activity, Step step, View view)
	{
		this.activity = activity;
		this.step = step;
		this.view = view;
	}

	public static Flight idle(View view)
	{
		return new Flight(View.Activity.NONE, null, view);
	}

	Flight to(Ground.Way<?> way)
	{
		if (way != null)
		{
			if (way.thing instanceof TileObject)
			{
				object = (TileObject) way.thing;
			}
			else if (way.thing instanceof NPC)
			{
				npc = (NPC) way.thing;
			}
			route = way.path;
		}
		return this;
	}
}
