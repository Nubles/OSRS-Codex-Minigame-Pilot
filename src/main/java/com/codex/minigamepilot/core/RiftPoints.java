package com.codex.minigamepilot.core;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Your Guardians of the Rift reward points, elemental and catalytic: those banked, as last told by
 * the game, and those this game's energy is on its way to.
 *
 * <p>Facts (OSRS wiki, Guardians of the Rift and Rewards Guardian): every 100 energy of a type is
 * one point of that type, a remainder being a matching chance of one more; one elemental and one
 * catalytic point are spent on each search of the Rewards Guardian, so the two are worth keeping
 * even. The game tells you your totals in a message at the end of each game and when you check
 * the Rewards Guardian. Searching says "You found some loot:" (as RuneLite's own loot tracker
 * reads it). The wording of the two totals messages is as the Guardians of the Rift Helper plugin
 * reads them (a cross-check, not a source).
 */
public final class RiftPoints
{
	private static final Pattern TOTAL_ELEMENTAL = Pattern.compile("Total elemental energy:\\D*([\\d,]+)");
	private static final Pattern TOTAL_CATALYTIC = Pattern.compile("Total catalytic energy:\\D*([\\d,]+)");
	private static final Pattern CHECK = Pattern.compile("You have ([\\d,]+) catalytic energy and ([\\d,]+) elemental energy");
	static final String LOOT = "You found some loot:";

	/** Points banked, or -1 while not known. */
	private int elemental = -1;
	private int catalytic = -1;
	/** This game's energy so far, from the game's own counters. */
	private int gameElemental;
	private int gameCatalytic;
	/** This game's energy already in the banked totals, once the end-of-game message has said them. */
	private int settledElemental;
	private int settledCatalytic;

	/** Whether the banked points are known. */
	public boolean known()
	{
		return elemental >= 0 && catalytic >= 0;
	}

	/** Elemental points: banked and what this game's energy is worth so far. */
	public double elemental()
	{
		return Math.max(0, elemental) + Math.max(0, gameElemental - settledElemental) / 100.0;
	}

	public double catalytic()
	{
		return Math.max(0, catalytic) + Math.max(0, gameCatalytic - settledCatalytic) / 100.0;
	}

	public int bankedElemental()
	{
		return elemental;
	}

	public int bankedCatalytic()
	{
		return catalytic;
	}

	public int gameElemental()
	{
		return gameElemental;
	}

	public int gameCatalytic()
	{
		return gameCatalytic;
	}

	/** This game's energy counters. A counter going down means a new game. */
	public void energy(int elementalEnergy, int catalyticEnergy)
	{
		if (elementalEnergy < gameElemental || catalyticEnergy < gameCatalytic)
		{
			settledElemental = 0;
			settledCatalytic = 0;
		}
		gameElemental = Math.max(0, elementalEnergy);
		gameCatalytic = Math.max(0, catalyticEnergy);
	}

	/**
	 * A chat or dialogue line, tags removed. Returns true when it changed the banked points.
	 */
	public boolean read(String message)
	{
		if (message == null)
		{
			return false;
		}
		Matcher e = TOTAL_ELEMENTAL.matcher(message);
		Matcher c = TOTAL_CATALYTIC.matcher(message);
		if (e.find() && c.find())
		{
			elemental = number(e.group(1));
			catalytic = number(c.group(1));
			// The totals already hold this game's points.
			settledElemental = gameElemental;
			settledCatalytic = gameCatalytic;
			return true;
		}
		Matcher k = CHECK.matcher(message);
		if (k.find())
		{
			catalytic = number(k.group(1));
			elemental = number(k.group(2));
			return true;
		}
		if (message.startsWith(LOOT) && known())
		{
			elemental = Math.max(0, elemental - 1);
			catalytic = Math.max(0, catalytic - 1);
			return true;
		}
		return false;
	}

	private static int number(String s)
	{
		try
		{
			return Integer.parseInt(s.replace(",", ""));
		}
		catch (NumberFormatException ex)
		{
			return -1;
		}
	}

	/** Searches the banked points pay for: the fewer of the two. */
	public int searches()
	{
		return known() ? Math.min(elemental, catalytic) : -1;
	}

	/** The banked points as saved for this account: "elemental:catalytic", or null when not known. */
	public String encode()
	{
		return known() ? elemental + ":" + catalytic : null;
	}

	/** Loads saved points; anything unreadable leaves them unknown. */
	public void decode(String saved)
	{
		elemental = -1;
		catalytic = -1;
		if (saved == null)
		{
			return;
		}
		String[] p = saved.split(":");
		if (p.length != 2)
		{
			return;
		}
		int e = number(p[0].trim());
		int c = number(p[1].trim());
		if (e >= 0 && c >= 0)
		{
			elemental = e;
			catalytic = c;
		}
	}

	/** A point figure: whole, or with one decimal while this game's part is still coming. */
	public static String show(double points)
	{
		if (Math.abs(points - Math.rint(points)) < 0.05)
		{
			return String.format(Locale.ROOT, "%,d", Math.round(points));
		}
		return String.format(Locale.ROOT, "%,.1f", points);
	}
}
