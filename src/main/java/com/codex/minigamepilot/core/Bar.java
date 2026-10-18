package com.codex.minigamepilot.core;

/**
 * The bars the Blast Furnace makes, with the ore and coal each needs. Coal is per bar at the
 * Blast Furnace, which is half what an ordinary furnace takes.
 */
public enum Bar
{
	IRON("Iron", 440, 0, 2351, 951, 942),
	STEEL("Steel", 440, 1, 2353, 951, 943),
	MITHRIL("Mithril", 447, 2, 2359, 952, 944),
	ADAMANTITE("Adamantite", 449, 3, 2361, 953, 945),
	RUNITE("Runite", 451, 4, 2363, 954, 946),
	GOLD("Gold", 444, 0, 2357, 955, 947),
	SILVER("Silver", 442, 0, 2355, 956, 948);

	public static final int COAL = 453;
	/** The furnace's coal count, a varbit. */
	public static final int COAL_VARBIT = 949;

	private final String name;
	private final int ore;
	private final int coal;
	private final int bar;
	private final int oreVarbit;
	private final int barVarbit;

	Bar(String name, int ore, int coal, int bar, int oreVarbit, int barVarbit)
	{
		this.name = name;
		this.ore = ore;
		this.coal = coal;
		this.bar = bar;
		this.oreVarbit = oreVarbit;
		this.barVarbit = barVarbit;
	}

	public String getName()
	{
		return name;
	}

	/** The ore item. */
	public int getOre()
	{
		return ore;
	}

	/** Coal needed per bar. */
	public int getCoal()
	{
		return coal;
	}

	/** The bar item. */
	public int getBar()
	{
		return bar;
	}

	/** The varbit holding this ore waiting in the furnace. */
	public int getOreVarbit()
	{
		return oreVarbit;
	}

	/** The varbit holding finished bars in the dispenser. */
	public int getBarVarbit()
	{
		return barVarbit;
	}

	/** "mithril ore", "coal" and so on, for sentences. */
	public String oreName()
	{
		return name.toLowerCase() + " ore";
	}

	public String barName(int count)
	{
		return name.toLowerCase() + (count == 1 ? " bar" : " bars");
	}

	public static boolean isBar(int itemId)
	{
		for (Bar b : values())
		{
			if (b.bar == itemId)
			{
				return true;
			}
		}
		return false;
	}
}
