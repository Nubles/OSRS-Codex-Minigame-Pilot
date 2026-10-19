package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

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
	 * Saved names this version does not know (a reward a newer version added), kept as they were
	 * so saving again does not forget that you own it.
	 */
	private final List<String> unknown = new ArrayList<>();
	/** What a reward's saved name looks like; anything else is damage, and dropped. */
	private static final Pattern NAME = Pattern.compile("[A-Z][A-Z0-9_]{0,47}");
	/** At most this many unknown names are kept, so damage cannot make the saved value grow. */
	private static final int KEEP_UNKNOWN = 16;

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
		for (String name : unknown)
		{
			if (b.length() > 0)
			{
				b.append(',');
			}
			b.append(name);
		}
		return b.toString();
	}

	/**
	 * Reads a saved memory. A name this version does not know is kept to be saved again, unless it
	 * is not a name at all. Group storage sightings start afresh.
	 */
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
				// A reward this version does not know counts for nothing here, but is kept for
				// the version that wrote it.
				String name = part.trim();
				if (NAME.matcher(name).matches() && !o.unknown.contains(name) && o.unknown.size() < KEEP_UNKNOWN)
				{
					o.unknown.add(name);
				}
			}
		}
		return o;
	}
}
