package com.codex.minigamepilot.core;

/**
 * The twelve Mahogany Homes clients: where each lives, the furniture their house has and in what
 * order the game's eight hotspot varbits describe it. Hotspot i of a house is varbit
 * {@link #HOTSPOT_VARBIT} + i.
 */
public enum Home
{
	// East Ardougne
	JESS("Jess", City.ARDOUGNE, "upstairs in the building south of the church", 10421,
		2611, 3290, 14, 7, 2621, 3292, true, new int[]{17026, 16685},
		h(40171, Kind.B2), h(40172, Kind.B2), h(40173, Kind.B2), h(40174, Kind.B2), h(40175, Kind.B3), h(40176, Kind.B3), h(40177, Kind.RP), h(40299, Kind.SB)),
	NOELLA("Noella", City.ARDOUGNE, "north of the market", 10419,
		2652, 3317, 15, 8, 2659, 3322, false, new int[]{17026, 16685, 15645, 15648},
		h(40156, Kind.B2), h(40157, Kind.B2), h(40158, Kind.RP), h(40159, Kind.RP), h(40160, Kind.B2), h(40161, Kind.B3), h(40162, Kind.B3), h(40163, Kind.RP)),
	ROSS("Ross", City.ARDOUGNE, "north of the church", 10420,
		2609, 3313, 11, 9, 2613, 3316, false, new int[]{16683, 16679},
		h(40164, Kind.SB), h(40165, Kind.B2), h(40166, Kind.B2), h(40167, Kind.B3), h(40168, Kind.RP), h(40169, Kind.B2), h(40170, Kind.RP)),

	// Falador
	LARRY("Larry", City.FALADOR, "north of the fountain", 10418,
		3033, 3360, 10, 9, 3038, 3364, false, new int[]{24075, 24076},
		h(40297, Kind.SB), h(40095, Kind.B2), h(40096, Kind.B2), h(40097, Kind.B3), h(40298, Kind.RP), h(40098, Kind.B3), h(40099, Kind.RP)),
	NORMAN("Norman", City.FALADOR, "south of the fountain", 3266,
		3034, 3341, 8, 8, 3038, 3344, true, new int[]{24082, 24085},
		h(40296, Kind.SB), h(40089, Kind.RP), h(40090, Kind.B3), h(40091, Kind.B3), h(40092, Kind.B2), h(40093, Kind.B2), h(40094, Kind.B2)),
	TAU("Tau", City.FALADOR, "south-east of the fountain", 10417,
		3043, 3340, 10, 11, 3047, 3345, false, new int[0],
		h(40083, Kind.SB), h(40084, Kind.B3), h(40085, Kind.B3), h(40086, Kind.B2), h(40087, Kind.B2), h(40088, Kind.B2), h(40295, Kind.RP)),

	// Hosidius
	BARBARA("Barbara", City.HOSIDIUS, "south of Hosidius, by the mill", 10424,
		1746, 3531, 10, 11, 1750, 3534, false, new int[0],
		h(40011, Kind.RP), h(40293, Kind.SB), h(40012, Kind.B3), h(40294, Kind.B2), h(40013, Kind.B2), h(40014, Kind.B1), h(40015, Kind.B1)),
	LEELA("Leela", City.HOSIDIUS, "east of the market", 10423,
		1781, 3589, 9, 8, 1785, 3592, false, new int[]{11794, 11802},
		h(40007, Kind.B2), h(40008, Kind.B2), h(40290, Kind.SB), h(40291, Kind.B3), h(40009, Kind.B3), h(40010, Kind.RP), h(40292, Kind.B2)),
	MARIAH("Mariah", City.HOSIDIUS, "west of the estate agent", 10422,
		1762, 3618, 10, 7, 1766, 3621, false, new int[]{11794, 11802},
		h(40002, Kind.B3), h(40287, Kind.SB), h(40003, Kind.B2), h(40288, Kind.B2), h(40004, Kind.B2), h(40005, Kind.B2), h(40006, Kind.B2), h(40289, Kind.RP)),

	// Varrock
	BOB("Bob", City.VARROCK, "north-east, opposite the church", 10414,
		3234, 3482, 10, 10, 3238, 3486, false, new int[]{11797, 11799},
		h(39981, Kind.B4), h(39982, Kind.RP), h(39983, Kind.B2), h(39984, Kind.B2), h(39985, Kind.B2), h(39986, Kind.B2), h(39987, Kind.B2), h(39988, Kind.B2)),
	JEFF("Jeff", City.VARROCK, "in the middle, west of the museum", 10415,
		3235, 3445, 10, 12, 3239, 3450, false, new int[]{11789, 11793},
		h(39989, Kind.B3), h(39990, Kind.B2), h(39991, Kind.B2), h(39992, Kind.B3), h(39993, Kind.B2), h(39994, Kind.B2), h(39995, Kind.RP), h(39996, Kind.B1)),
	SARAH("Sarah", City.VARROCK, "along the south wall", 10416,
		3232, 3381, 8, 7, 3235, 3384, false, new int[0],
		h(39997, Kind.B3), h(39998, Kind.B2), h(39999, Kind.B2), h(40000, Kind.B2), h(40286, Kind.SB), h(40001, Kind.B2));

	/** The first of the eight hotspot varbits. */
	public static final int HOTSPOT_VARBIT = 10554;
	public static final int HOTSPOTS = 8;

	/** Where the Mahogany Homes contractors work. */
	public enum City
	{
		FALADOR("Falador", "Amy"),
		VARROCK("Varrock", "Marlo"),
		ARDOUGNE("East Ardougne", "Ellie"),
		HOSIDIUS("Hosidius", "Angelo");

		private final String name;
		private final String contractor;

		City(String name, String contractor)
		{
			this.name = name;
			this.contractor = contractor;
		}

		public String getName()
		{
			return name;
		}

		public String getContractor()
		{
			return contractor;
		}

		public static boolean isContractor(String npcName)
		{
			for (City c : values())
			{
				if (c.contractor.equalsIgnoreCase(npcName))
				{
					return true;
				}
			}
			return false;
		}
	}

	/** What a hotspot needs. Furniture is taken out and built new; the rest is mended. */
	public enum Kind
	{
		B1(false, 1),
		B2(false, 2),
		B3(false, 3),
		B4(false, 4),
		/** Mended with a plank. */
		RP(false, 1),
		/** Mended with a steel bar. */
		SB(true, 1);

		private final boolean steel;
		private final int amount;

		Kind(boolean steel, int amount)
		{
			this.steel = steel;
			this.amount = amount;
		}

		public boolean isSteel()
		{
			return steel;
		}

		public int getAmount()
		{
			return amount;
		}

		/** Whether this hotspot is mended rather than taken out and rebuilt. */
		public boolean isRepair()
		{
			return this == RP || this == SB;
		}
	}

	/** One piece of furniture. */
	public static final class Hotspot
	{
		private final int objectId;
		private final Kind kind;

		Hotspot(int objectId, Kind kind)
		{
			this.objectId = objectId;
			this.kind = kind;
		}

		public int getObjectId()
		{
			return objectId;
		}

		public Kind getKind()
		{
			return kind;
		}
	}


	private static Hotspot h(int objectId, Kind kind)
	{
		return new Hotspot(objectId, kind);
	}

	private final String name;
	private final City city;
	private final String where;
	private final int npcId;
	private final int x;
	private final int y;
	private final int width;
	private final int height;
	private final int doorX;
	private final int doorY;
	private final boolean clientUpstairs;
	private final int[] ladders;
	private final Hotspot[] hotspots;

	Home(String name, City city, String where, int npcId, int x, int y, int width, int height,
		int doorX, int doorY, boolean clientUpstairs, int[] ladders, Hotspot... hotspots)
	{
		this.name = name;
		this.city = city;
		this.where = where;
		this.npcId = npcId;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.doorX = doorX;
		this.doorY = doorY;
		this.clientUpstairs = clientUpstairs;
		this.ladders = ladders;
		this.hotspots = hotspots;
	}

	public String getName()
	{
		return name;
	}

	public City getCity()
	{
		return city;
	}

	/** Where in the city, such as "north of the fountain". */
	public String getWhere()
	{
		return where;
	}

	public int getNpcId()
	{
		return npcId;
	}

	/** The house's walls, on the ground floor. */
	public boolean contains(int wx, int wy)
	{
		return wx >= x && wx < x + width && wy >= y && wy < y + height;
	}

	/** Distance in tiles from a point to the house, zero when inside. */
	public int distanceTo(int wx, int wy)
	{
		int dx = wx < x ? x - wx : wx >= x + width ? wx - (x + width - 1) : 0;
		int dy = wy < y ? y - wy : wy >= y + height ? wy - (y + height - 1) : 0;
		return Math.max(dx, dy);
	}

	/** A tile in the house to head for. */
	public int getDoorX()
	{
		return doorX;
	}

	public int getDoorY()
	{
		return doorY;
	}

	public boolean isClientUpstairs()
	{
		return clientUpstairs;
	}

	/** The ladders between the house's floors. */
	public int[] getLadders()
	{
		return ladders.clone();
	}

	public boolean isLadder(int objectId)
	{
		for (int l : ladders)
		{
			if (l == objectId)
			{
				return true;
			}
		}
		return false;
	}

	public int hotspotCount()
	{
		return hotspots.length;
	}

	public Hotspot hotspot(int index)
	{
		return index >= 0 && index < hotspots.length ? hotspots[index] : null;
	}

	/** The hotspot index an object belongs to in this house, or -1. */
	public int indexOf(int objectId)
	{
		for (int i = 0; i < hotspots.length; i++)
		{
			if (hotspots[i].objectId == objectId)
			{
				return i;
			}
		}
		return -1;
	}

	public static Home byName(String name)
	{
		for (Home h : values())
		{
			if (h.name.equalsIgnoreCase(name))
			{
				return h;
			}
		}
		return null;
	}
}
