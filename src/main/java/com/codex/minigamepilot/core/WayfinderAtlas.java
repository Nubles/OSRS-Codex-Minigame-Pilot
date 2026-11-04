package com.codex.minigamepilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The travel atlas Codex Wayfinder shares, read and never written: the teleports this account
 * owns, where each lands, how long its cast takes and whether it is carried, and the player's own
 * walking pace. Wayfinder saves it in the RuneLite profile as {@code codexwayfinder/atlasV1}:
 * {@code {"version":1,"savedAt":ms,"walkSecondsPerTile":0.3,"teleports":[{"name":"Falador Teleport",
 * "source":"Falador teleport","kind":"SPELL","x":2966,"y":3379,"plane":0,"seconds":4,"carried":true,
 * "timed":false}],"networks":{...},"timedTrips":0}}. Codex Autopilot, Home and Ironworks read the
 * same key. A missing, damaged or other-version atlas reads as none, and nothing needs it.
 */
public final class WayfinderAtlas
{
	public static final String GROUP = "codexwayfinder";
	public static final String KEY = "atlasV1";
	/**
	 * Seconds a tile when running: two tiles a 0.6 second game tick (OSRS wiki, "Energy": running
	 * moves the player two squares a tick). Used when the atlas gives no pace that makes sense.
	 */
	public static final double RUN_SECONDS_PER_TILE = 0.3;

	/** One teleport the account owns. */
	public static final class Teleport
	{
		private final String name;
		private final String source;
		private final int x;
		private final int y;
		private final int plane;
		private final int seconds;
		private final boolean carried;

		public Teleport(String name, String source, int x, int y, int plane, int seconds, boolean carried)
		{
			this.name = name;
			this.source = source;
			this.x = x;
			this.y = y;
			this.plane = plane;
			this.seconds = Math.max(0, seconds);
			this.carried = carried;
		}

		/** Wayfinder's label, such as "Ring of wealth: Falador Park". */
		public String getName()
		{
			return name;
		}

		/** What casts it, such as "Ring of wealth"; the name when Wayfinder gave none. */
		public String getSource()
		{
			return source == null || source.isEmpty() ? name : source;
		}

		public int getX()
		{
			return x;
		}

		public int getY()
		{
			return y;
		}

		public int getPlane()
		{
			return plane;
		}

		/** The cast itself, in seconds. */
		public int getSeconds()
		{
			return seconds;
		}

		/** In the pack or worn when Wayfinder last looked; otherwise in the bank. */
		public boolean isCarried()
		{
			return carried;
		}
	}

	private final long savedAt;
	private final double walkSecondsPerTile;
	private final List<Teleport> teleports;

	WayfinderAtlas(long savedAt, double walkSecondsPerTile, List<Teleport> teleports)
	{
		this.savedAt = savedAt;
		this.walkSecondsPerTile = walkSecondsPerTile;
		this.teleports = Collections.unmodifiableList(teleports);
	}

	/** An atlas holding these, for tests and previews. */
	public static WayfinderAtlas of(double walkSecondsPerTile, List<Teleport> teleports)
	{
		return new WayfinderAtlas(0, walkSecondsPerTile, new ArrayList<>(teleports));
	}

	/** Wayfinder's saved atlas, or null when there is none or this version cannot read it. */
	public static WayfinderAtlas parse(Gson gson, String json)
	{
		if (json == null || json.trim().isEmpty())
		{
			return null;
		}
		JsonObject o;
		try
		{
			JsonElement e = gson.fromJson(json, JsonElement.class);
			o = e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
		}
		catch (JsonParseException | IllegalStateException | NumberFormatException e)
		{
			return null;
		}
		if (o == null || whole(o.get("version"), -1) != 1)
		{
			return null;
		}
		JsonElement list = o.get("teleports");
		if (list == null || !list.isJsonArray())
		{
			return null;
		}
		List<Teleport> out = new ArrayList<>();
		for (JsonElement e : (JsonArray) list)
		{
			if (e == null || !e.isJsonObject())
			{
				continue;
			}
			JsonObject t = e.getAsJsonObject();
			String name = text(t.get("name"));
			long x = whole(t.get("x"), -1);
			long y = whole(t.get("y"), -1);
			long plane = whole(t.get("plane"), 0);
			long seconds = whole(t.get("seconds"), -1);
			if (name == null || name.isEmpty() || x <= 0 || y <= 0 || x > 20_000 || y > 20_000 || plane < 0 || plane > 3
				|| seconds < 0 || seconds > 3600)
			{
				continue;
			}
			JsonElement carried = t.get("carried");
			boolean held = carried != null && carried.isJsonPrimitive() && carried.getAsJsonPrimitive().isBoolean() && carried.getAsBoolean();
			out.add(new Teleport(name, text(t.get("source")), (int) x, (int) y, (int) plane, (int) seconds, held));
		}
		double walk = real(o.get("walkSecondsPerTile"));
		return new WayfinderAtlas(Math.max(0, whole(o.get("savedAt"), 0)), walk, out);
	}

	public long getSavedAt()
	{
		return savedAt;
	}

	/** Seconds a tile of this player's walking, as Wayfinder learned it; the running pace when it gave none that makes sense. */
	public double getWalkSecondsPerTile()
	{
		return walkSecondsPerTile >= 0.1 && walkSecondsPerTile <= 2 ? walkSecondsPerTile : RUN_SECONDS_PER_TILE;
	}

	public List<Teleport> getTeleports()
	{
		return teleports;
	}

	private static String text(JsonElement e)
	{
		return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null;
	}

	private static long whole(JsonElement e, long fallback)
	{
		if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber())
		{
			return fallback;
		}
		double d = e.getAsDouble();
		return Double.isNaN(d) || Double.isInfinite(d) || d != Math.rint(d) ? fallback : (long) d;
	}

	private static double real(JsonElement e)
	{
		if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber())
		{
			return 0;
		}
		double d = e.getAsDouble();
		return Double.isNaN(d) || Double.isInfinite(d) ? 0 : d;
	}
}
