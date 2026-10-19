package com.codex.minigamepilot.core;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * The shop rewards you have been seen to own, remembered per account. A carpenter's outfit or a
 * prospector kit put away in your house's costume room drops out of sight of the inventory, worn
 * items and the bank, but you still own it, so once a reward has been seen carried, worn or in
 * your bank it stays owned. A reward that turned up in your pack while group storage was open
 * might be a group mate's, so it is not remembered for that session.
 */
public final class Owned
{
	private final EnumSet<Goal.Reward> owned = EnumSet.noneOf(Goal.Reward.class);
	private final EnumSet<Goal.Reward> borrowed = EnumSet.noneOf(Goal.Reward.class);

	/**
	 * One tick.
	 *
	 * @param seen rewards carried, worn or in the bank as last seen
	 * @param appeared rewards in your pack now that were not carried or worn a tick ago
	 * @param groupStorage whether group storage was open on this tick or the last
	 * @return true when a reward was remembered for the first time, so the memory wants saving
	 */
	public boolean see(Set<Goal.Reward> seen, Set<Goal.Reward> appeared, boolean groupStorage)
	{
		if (groupStorage)
		{
			for (Goal.Reward r : appeared)
			{
				if (!owned.contains(r))
				{
					borrowed.add(r);
				}
			}
		}
		boolean grew = false;
		for (Goal.Reward r : seen)
		{
			if (!borrowed.contains(r) && r.getItems().length > 0)
			{
				grew |= owned.add(r);
			}
		}
		return grew;
	}

	public boolean has(Goal.Reward r)
	{
		return owned.contains(r);
	}

	/** Whether an item is one of a reward you are remembered to own. */
	public boolean hasItem(int id)
	{
		for (Goal.Reward r : owned)
		{
			for (int item : r.getItems())
			{
				if (item == id)
				{
					return true;
				}
			}
		}
		return false;
	}

	/** Saved as reward names joined by commas. */
	public String encode()
	{
		StringBuilder b = new StringBuilder();
		for (Goal.Reward r : owned)
		{
			if (b.length() > 0)
			{
				b.append(',');
			}
			b.append(r.name());
		}
		return b.toString();
	}

	/** Reads a saved memory; unknown names are dropped. Group storage sightings start afresh. */
	public static Owned decode(String saved)
	{
		Owned o = new Owned();
		if (saved == null || saved.isEmpty())
		{
			return o;
		}
		for (String part : saved.split(","))
		{
			try
			{
				Goal.Reward r = Goal.Reward.valueOf(part.trim().toUpperCase(Locale.ENGLISH));
				if (r.getItems().length > 0)
				{
					o.owned.add(r);
				}
			}
			catch (IllegalArgumentException e)
			{
				// A reward this version does not know is left out.
			}
		}
		return o;
	}
}
