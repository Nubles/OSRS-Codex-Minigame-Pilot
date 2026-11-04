package com.codex.minigamepilot.core;

/**
 * The twelve runic altars a Guardians of the Rift portal guardian leads to.
 *
 * <p>Levels, alignment, the cell each one charges and the quest each needs are from the OSRS
 * wiki, Guardians of the Rift ("Runic altars" tables). The order is the game's rune icon order
 * (the minigame's own HUD shows the open altars with these icons, air first and blood last).
 */
public enum Altar
{
	AIR("Air", 1, Alignment.ELEMENTAL, Cell.WEAK, null),
	MIND("Mind", 2, Alignment.CATALYTIC, Cell.WEAK, null),
	WATER("Water", 5, Alignment.ELEMENTAL, Cell.MEDIUM, null),
	EARTH("Earth", 9, Alignment.ELEMENTAL, Cell.STRONG, null),
	FIRE("Fire", 14, Alignment.ELEMENTAL, Cell.OVERCHARGED, null),
	BODY("Body", 20, Alignment.CATALYTIC, Cell.WEAK, null),
	COSMIC("Cosmic", 27, Alignment.CATALYTIC, Cell.MEDIUM, "Lost City"),
	CHAOS("Chaos", 35, Alignment.CATALYTIC, Cell.MEDIUM, null),
	NATURE("Nature", 44, Alignment.CATALYTIC, Cell.STRONG, null),
	LAW("Law", 54, Alignment.CATALYTIC, Cell.STRONG, "Troll Stronghold"),
	DEATH("Death", 65, Alignment.CATALYTIC, Cell.OVERCHARGED, "Mourning's End Part II"),
	BLOOD("Blood", 77, Alignment.CATALYTIC, Cell.OVERCHARGED, "Sins of the Father");

	/** Which of the two reward points crafting here earns. */
	public enum Alignment
	{
		ELEMENTAL("elemental"),
		CATALYTIC("catalytic");

		private final String word;

		Alignment(String word)
		{
			this.word = word;
		}

		public String word()
		{
			return word;
		}

		public Alignment other()
		{
			return this == ELEMENTAL ? CATALYTIC : ELEMENTAL;
		}
	}

	/** The cell an uncharged cell becomes when crafted here; a stronger cell is worth more energy. */
	public enum Cell
	{
		WEAK("weak"),
		MEDIUM("medium"),
		STRONG("strong"),
		OVERCHARGED("overcharged");

		private final String word;

		Cell(String word)
		{
			this.word = word;
		}

		public String word()
		{
			return word;
		}
	}

	private final String rune;
	private final int level;
	private final Alignment alignment;
	private final Cell cell;
	private final String quest;

	Altar(String rune, int level, Alignment alignment, Cell cell, String quest)
	{
		this.rune = rune;
		this.level = level;
		this.alignment = alignment;
		this.cell = cell;
		this.quest = quest;
	}

	/** "Fire". */
	public String rune()
	{
		return rune;
	}

	/** The Runecraft level it asks for. */
	public int level()
	{
		return level;
	}

	public Alignment alignment()
	{
		return alignment;
	}

	public Cell cell()
	{
		return cell;
	}

	/** The quest it needs, by its name, or null. */
	public String quest()
	{
		return quest;
	}

	/** The altar shown by a rune icon, by its place in the game's rune icon order, or null. */
	public static Altar byIcon(int index)
	{
		Altar[] all = values();
		return index >= 0 && index < all.length ? all[index] : null;
	}
}
