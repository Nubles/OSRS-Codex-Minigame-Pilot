package com.codex.minigamepilot.core;

import java.util.Objects;

/**
 * The one thing to do next: a short verb for the game view, a sentence for the panel, a line of
 * why, and what in the world to light up. Planners hand these out; the plugin finds the target.
 */
public final class Step
{
	/** What in the world the step is about. The plugin turns this into an object, a character or an item. */
	public enum Target
	{
		NONE,
		// Motherlode Mine
		VEIN,
		HOPPER,
		SACK,
		BANK,
		STRUT,
		ROCKFALL,
		LADDER,
		// Blast Furnace
		BELT,
		DISPENSER,
		COFFER,
		/** An item in the inventory, worn, or in the open bank. */
		ITEM,
		// Mahogany Homes
		CONTRACTOR,
		CLIENT,
		HOTSPOT,
		HOME
	}

	private final String verb;
	private final String title;
	private final String detail;
	private final Target target;
	private final int[] items;
	private final boolean waiting;

	private Step(String verb, String title, String detail, Target target, int[] items, boolean waiting)
	{
		this.verb = verb;
		this.title = title;
		this.detail = detail;
		this.target = target;
		this.items = items == null ? new int[0] : items;
		this.waiting = waiting;
	}

	public static Step of(String verb, String title, String detail, Target target)
	{
		return new Step(verb, title, detail, target, null, false);
	}

	/** A step about items: withdraw them, equip them, use them. */
	public static Step items(String verb, String title, String detail, int... items)
	{
		return new Step(verb, title, detail, Target.ITEM, items, false);
	}

	/** Nothing to click: the game is doing the work. The target is where to wait. */
	public static Step waiting(String title, String detail, Target target)
	{
		return new Step("Wait", title, detail, target, null, true);
	}

	/** The same step with a different line of why. */
	public Step withDetail(String newDetail)
	{
		return new Step(verb, title, newDetail, target, items, waiting);
	}

	/** The same step with items to light up too, such as the teleport to use on the way. */
	public Step withItems(int... newItems)
	{
		return new Step(verb, title, detail, target, newItems, waiting);
	}

	/** A short word for the label over the target, such as "Deposit". */
	public String getVerb()
	{
		return verb;
	}

	/** The sentence the panel and plaque lead with. */
	public String getTitle()
	{
		return title;
	}

	/** Why, or how much: a quieter line under the title. May be empty. */
	public String getDetail()
	{
		return detail == null ? "" : detail;
	}

	public Target getTarget()
	{
		return target;
	}

	public int[] getItems()
	{
		return items.clone();
	}

	public boolean hasItem(int id)
	{
		for (int i : items)
		{
			if (i == id)
			{
				return true;
			}
		}
		return false;
	}

	public boolean isWaiting()
	{
		return waiting;
	}

	/** Two steps are the same instruction when they ask for the same action on the same thing. */
	public boolean sameAs(Step other)
	{
		return other != null && target == other.target && Objects.equals(title, other.title);
	}

	@Override
	public String toString()
	{
		return verb + ": " + title + (detail == null || detail.isEmpty() ? "" : " (" + detail + ")");
	}
}
