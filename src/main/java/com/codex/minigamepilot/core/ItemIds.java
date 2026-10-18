package com.codex.minigamepilot.core;

/** Item ids the planners name. Kept here so the planners stay plain Java and testable. */
public final class ItemIds
{
	public static final int PAYDIRT = 12011;
	public static final int GOLDEN_NUGGET = 12012;
	public static final int HAMMER = 2347;
	public static final int IMCANDO_HAMMER = 25644;
	public static final int IMCANDO_HAMMER_OFFHAND = 29775;
	public static final int COAL_BAG = 12019;
	public static final int COAL_BAG_OPEN = 24480;
	public static final int ICE_GLOVES = 1580;
	public static final int SMITHING_UNIFORM_GLOVES_ICE = 27031;
	public static final int GOLDSMITH_GAUNTLETS = 776;
	public static final int SMITHING_CAPE = 9795;
	public static final int SMITHING_CAPE_TRIMMED = 9796;
	public static final int PLANK_SACK = 24882;
	public static final int COINS = 995;

	/** Ore and gems that come out of the Motherlode sack or off a vein, and go to the bank. */
	public static final int[] MOTHERLODE_LOOT = {
		453, 444, 447, 449, 451,
		1623, 1621, 1619, 1617, 1631
	};

	private ItemIds()
	{
	}

	public static boolean isMotherlodeLoot(int id)
	{
		for (int i : MOTHERLODE_LOOT)
		{
			if (i == id)
			{
				return true;
			}
		}
		return false;
	}
}
