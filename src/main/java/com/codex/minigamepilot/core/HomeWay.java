package com.codex.minigamepilot.core;

/**
 * The quickest way to a client's house: walking from where you stand, or one of the teleports
 * Codex Wayfinder knows you own, cast and then walked from where it lands. Distances are straight
 * lines at your own pace from Wayfinder, so they are a floor for the trip rather than its time,
 * and a teleport is only offered when it saves real time over walking.
 *
 * <p>Without Wayfinder, the teleport the OSRS wiki's Mahogany Homes page recommends for the city
 * is named instead.
 */
public final class HomeWay
{
	/** A teleport must beat walking by at least this to be worth naming. */
	static final int SAVES_AT_LEAST = 10;
	/** Surface coordinates stop here; beyond (dungeons, instances) walking from where you stand means nothing. */
	static final int SURFACE_TOP = 4200;

	/** One way there. */
	public static final class Way
	{
		private final WayfinderAtlas.Teleport teleport;
		private final int tiles;
		private final int seconds;

		Way(WayfinderAtlas.Teleport teleport, int tiles, int seconds)
		{
			this.teleport = teleport;
			this.tiles = tiles;
			this.seconds = seconds;
		}

		/** The teleport, or null when walking is the way. */
		public WayfinderAtlas.Teleport getTeleport()
		{
			return teleport;
		}

		/** Tiles to walk, from where you stand or where the teleport lands. */
		public int getTiles()
		{
			return tiles;
		}

		/** Cast and walk, in whole seconds, at least. */
		public int getSeconds()
		{
			return seconds;
		}
	}

	/** The quickest way carried, and a quicker one left in the bank when there is one. */
	public static final class Plan
	{
		private final Way best;
		private final Way banked;

		Plan(Way best, Way banked)
		{
			this.best = best;
			this.banked = banked;
		}

		/** The quickest way with what you carry: a teleport, or walking (null when walking means nothing from here and nothing carried goes there). */
		public Way getBest()
		{
			return best;
		}

		/** A teleport you own but left in the bank that would be quicker still, or null. */
		public Way getBanked()
		{
			return banked;
		}

		/** One line for the step: "Falador tablet, about 25s. Ring of wealth in your bank: 15s", or null when walking is quickest. */
		public String line()
		{
			StringBuilder b = new StringBuilder();
			if (best != null && best.teleport != null)
			{
				b.append(capital(best.teleport.getName())).append(", about ").append(clock(best.seconds));
			}
			if (banked != null)
			{
				b.append(b.length() == 0 ? "" : ". ").append(capital(banked.teleport.getSource())).append(" in your bank: ")
					.append(clock(banked.seconds));
			}
			return b.length() == 0 ? null : b.toString();
		}
	}

	private HomeWay()
	{
	}

	/**
	 * The quickest way to a house.
	 *
	 * @param x where you stand
	 * @param plane your floor; walking is only weighed from the surface
	 * @param atlas Wayfinder's atlas, or null
	 * @return the plan, or null without an atlas
	 */
	public static Plan plan(Home home, int x, int y, int plane, WayfinderAtlas atlas)
	{
		if (home == null || atlas == null)
		{
			return null;
		}
		double pace = atlas.getWalkSecondsPerTile();
		Way walk = null;
		if (y < SURFACE_TOP && plane == 0)
		{
			int tiles = home.distanceTo(x, y);
			walk = new Way(null, tiles, (int) Math.round(tiles * pace));
		}
		Way carried = null;
		Way any = null;
		for (WayfinderAtlas.Teleport t : atlas.getTeleports())
		{
			if (t.getPlane() != 0 || t.getY() >= SURFACE_TOP)
			{
				continue;
			}
			int tiles = home.distanceTo(t.getX(), t.getY());
			Way w = new Way(t, tiles, t.getSeconds() + (int) Math.round(tiles * pace));
			if (t.isCarried() && (carried == null || w.seconds < carried.seconds))
			{
				carried = w;
			}
			if (any == null || w.seconds < any.seconds)
			{
				any = w;
			}
		}
		Way best = walk;
		if (carried != null && (walk == null || carried.seconds + SAVES_AT_LEAST <= walk.seconds))
		{
			best = carried;
		}
		Way banked = null;
		if (any != null && !any.teleport.isCarried() && (best == null || any.seconds + SAVES_AT_LEAST <= best.seconds))
		{
			banked = any;
		}
		return new Plan(best, banked);
	}

	/**
	 * The teleport the OSRS wiki's Mahogany Homes page recommends for each city: a Falador teleport,
	 * a Varrock teleport, an Ardougne teleport, and for Hosidius a charged Xeric's talisman or a
	 * house set in Hosidius.
	 */
	public static String wikiWay(Home.City city)
	{
		switch (city)
		{
			case FALADOR:
				return "a Falador teleport";
			case VARROCK:
				return "a Varrock teleport";
			case ARDOUGNE:
				return "an Ardougne teleport";
			default:
				return "a Xeric's talisman or your house in Hosidius";
		}
	}

	/** "45s" or "1m 05s". */
	public static String clock(int seconds)
	{
		int s = Math.max(0, seconds);
		return s < 60 ? s + "s" : s / 60 + "m " + String.format(java.util.Locale.ENGLISH, "%02ds", s % 60);
	}

	private static String capital(String s)
	{
		return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}
