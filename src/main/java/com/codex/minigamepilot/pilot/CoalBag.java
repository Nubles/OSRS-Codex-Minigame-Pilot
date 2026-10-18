package com.codex.minigamepilot.pilot;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How much coal is in the coal bag. The game only says when the bag is checked, so this follows
 * the bag's Fill and Empty options and watches the coal in the inventory move.
 */
final class CoalBag
{
	private static final Pattern CONTAINS = Pattern.compile("The coal bag (?:still )?contains (\\d+|one) pieces? of coal\\.");
	private static final Pattern EMPTY = Pattern.compile("The coal bag is (?:now )?empty\\.");

	/** What was last asked of the bag, waiting for the inventory to show it. */
	private enum Pending
	{
		NONE,
		FILL,
		EMPTY
	}

	private int coal = -1;
	private int capacity = 27;
	private Pending pending = Pending.NONE;
	private int coalBefore;
	private int wait;

	/** Coal in the bag, or -1 when unknown. */
	int coal()
	{
		return coal;
	}

	int capacity()
	{
		return capacity;
	}

	/** A smithing cape lets the bag hold 36. */
	void setCapacity(int capacity)
	{
		this.capacity = capacity;
		if (coal > capacity)
		{
			coal = capacity;
		}
	}

	void chat(String message)
	{
		Matcher m = CONTAINS.matcher(message);
		if (m.matches())
		{
			coal = "one".equals(m.group(1)) ? 1 : Integer.parseInt(m.group(1));
		}
		else if (EMPTY.matcher(message).matches())
		{
			coal = 0;
		}
	}

	/**
	 * The player chose an option on the bag.
	 *
	 * @param bankCoal coal in the open bank, or -1 when the bank is closed
	 */
	void option(String option, int inventoryCoal, int bankCoal)
	{
		if ("Fill".equalsIgnoreCase(option))
		{
			if (bankCoal >= 0)
			{
				// Filled from the bank: the inventory does not change, so count it now.
				int room = capacity - Math.max(0, coal);
				coal = Math.max(0, coal) + Math.min(room, bankCoal);
				return;
			}
			pending = Pending.FILL;
		}
		else if ("Empty".equalsIgnoreCase(option))
		{
			pending = Pending.EMPTY;
		}
		else
		{
			return;
		}
		coalBefore = inventoryCoal;
		wait = 3;
	}

	/** Once a tick, with the coal carried now. */
	void tick(int inventoryCoal)
	{
		if (pending == Pending.NONE)
		{
			return;
		}
		int moved = inventoryCoal - coalBefore;
		if (pending == Pending.EMPTY && moved > 0)
		{
			coal = Math.max(0, Math.max(0, coal) - moved);
			pending = Pending.NONE;
		}
		else if (pending == Pending.FILL && moved < 0)
		{
			coal = Math.min(capacity, Math.max(0, coal) - moved);
			pending = Pending.NONE;
		}
		else if (--wait <= 0)
		{
			pending = Pending.NONE;
		}
	}
}
