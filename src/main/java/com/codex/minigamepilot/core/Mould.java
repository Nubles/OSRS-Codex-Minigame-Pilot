package com.codex.minigamepilot.core;

/**
 * Every Giants' Foundry mould: its part of the sword, the Smithing level and reputation it costs
 * at Kovac's shop (none for the six of each part you start with), and its score for each style a
 * commission can ask for.
 *
 * <p>Facts: OSRS wiki, Giants' Foundry/Mould List (every score below, in its Narrow, Broad,
 * Light, Heavy, Flat, Spiked columns; a blank there is 0 here), and Giants' Foundry (the
 * purchasable moulds' levels and reputation costs; the six of each part listed at level 15 with
 * no cost are the default moulds). Cross-checked with the Easy Giants' Foundry plugin's table.
 */
public enum Mould
{
	// Fortes.                    part           level rep   N   B   L   H   F   S
	SERRATED_FORTE("Serrated Forte", Part.FORTE, 15, 0, 8, 0, 0, 8, 0, 6),
	SERPENT_RICASSO("Serpent Ricasso", Part.FORTE, 15, 0, 6, 0, 8, 0, 8, 0),
	MEDUSA_RICASSO("Medusa Ricasso", Part.FORTE, 15, 0, 0, 8, 0, 6, 8, 0),
	DISARMING_FORTE("Disarming Forte", Part.FORTE, 15, 0, 4, 0, 4, 0, 0, 4),
	GLADIUS_RICASSO("Gladius Ricasso", Part.FORTE, 15, 0, 0, 4, 0, 4, 4, 0),
	CHOPPER_FORTE("Chopper Forte", Part.FORTE, 15, 0, 0, 4, 4, 0, 4, 0),
	STILETTO_FORTE("Stiletto Forte", Part.FORTE, 50, 300, 8, 0, 10, 0, 8, 0),
	DEFENDER_BASE("Defender Base", Part.FORTE, 51, 300, 0, 8, 0, 10, 8, 0),
	JUGGERNAUT_FORTE("Juggernaut Forte", Part.FORTE, 61, 350, 0, 4, 0, 4, 0, 16),
	CHOPPER_FORTE_PLUS("Chopper Forte +1", Part.FORTE, 70, 400, 0, 3, 4, 0, 18, 0),
	SPIKER("Spiker!", Part.FORTE, 79, 450, 1, 0, 0, 2, 0, 22),
	// Blades.
	GLADIUS_EDGE("Gladius Edge", Part.BLADE, 15, 0, 6, 0, 0, 8, 8, 0),
	STILETTO_BLADE("Stiletto Blade", Part.BLADE, 15, 0, 8, 0, 6, 0, 8, 0),
	MEDUSA_BLADE("Medusa Blade", Part.BLADE, 15, 0, 0, 8, 0, 8, 6, 0),
	FISH_BLADE("Fish Blade", Part.BLADE, 15, 0, 4, 0, 4, 0, 4, 0),
	DEFENDERS_EDGE("Defenders Edge", Part.BLADE, 15, 0, 0, 4, 0, 4, 0, 4),
	SAW_BLADE("Saw Blade", Part.BLADE, 15, 0, 0, 4, 4, 0, 0, 4),
	FLAMBERGE_BLADE("Flamberge Blade", Part.BLADE, 48, 300, 8, 0, 8, 0, 0, 10),
	SERPENT_BLADE("Serpent Blade", Part.BLADE, 50, 300, 10, 0, 8, 0, 8, 0),
	CLAYMORE_BLADE("Claymore Blade", Part.BLADE, 59, 350, 0, 16, 0, 4, 4, 0),
	FLEUR_DE_BLADE("Fleur de Blade", Part.BLADE, 71, 400, 0, 4, 0, 18, 0, 1),
	CHOPPA("Choppa!", Part.BLADE, 80, 450, 0, 1, 22, 0, 2, 0),
	// Tips.
	SAW_TIP("Saw Tip", Part.TIP, 15, 0, 0, 6, 0, 8, 0, 8),
	GLADIUS_POINT("Gladius Point", Part.TIP, 15, 0, 8, 0, 0, 8, 6, 0),
	SERPENTS_FANG("Serpent's Fang", Part.TIP, 15, 0, 8, 0, 6, 0, 0, 8),
	MEDUSAS_HEAD("Medusa's Head", Part.TIP, 15, 0, 0, 4, 0, 4, 0, 4),
	CHOPPER_TIP("Chopper Tip", Part.TIP, 15, 0, 0, 4, 4, 0, 0, 4),
	PEOPLE_POKER_POINT("People Poker Point", Part.TIP, 15, 0, 4, 0, 0, 4, 4, 0),
	CORRUPTED_POINT("Corrupted Point", Part.TIP, 49, 300, 8, 0, 10, 0, 0, 8),
	DEFENDERS_TIP("Defenders Tip", Part.TIP, 52, 300, 0, 10, 0, 8, 0, 8),
	SERRATED_TIP("Serrated Tip", Part.TIP, 60, 350, 4, 0, 16, 0, 0, 4),
	NEEDLE_POINT("Needle Point", Part.TIP, 69, 400, 18, 0, 3, 0, 4, 0),
	THE_POINT("The Point!", Part.TIP, 81, 450, 0, 2, 1, 0, 22, 0);

	/** The three parts a sword's mould is set from, in the jig's order. */
	public enum Part
	{
		FORTE("Forte"),
		BLADE("Blade"),
		TIP("Tip");

		private final String title;

		Part(String title)
		{
			this.title = title;
		}

		public String getTitle()
		{
			return title;
		}
	}

	/**
	 * A word of a commission. Kovac's first word is Broad, Heavy, Light or Narrow, the second
	 * Flat, Heavy, Light or Spiked, never both Heavy nor both Light (OSRS wiki, Giants' Foundry).
	 */
	public enum Style
	{
		NARROW("Narrow"),
		BROAD("Broad"),
		LIGHT("Light"),
		HEAVY("Heavy"),
		FLAT("Flat"),
		SPIKED("Spiked");

		private final String title;

		Style(String title)
		{
			this.title = title;
		}

		public String getTitle()
		{
			return title;
		}

		/** The style a commission word's varbit holds, or null for none. */
		public static Style ofVarbit(int value)
		{
			// The Easy Giants' Foundry plugin's reading of the commission varbits: 1 Narrow,
			// 2 Light, 3 Flat, 4 Broad, 5 Heavy, 6 Spiked (a cross-check, not a source; the
			// Pilot prefers the words the mould jig itself shows when it is open).
			switch (value)
			{
				case 1:
					return NARROW;
				case 2:
					return LIGHT;
				case 3:
					return FLAT;
				case 4:
					return BROAD;
				case 5:
					return HEAVY;
				case 6:
					return SPIKED;
				default:
					return null;
			}
		}

		/** The style a word names, ignoring case, or null. */
		public static Style ofWord(String word)
		{
			if (word == null)
			{
				return null;
			}
			String w = word.trim();
			for (Style s : values())
			{
				if (s.title.equalsIgnoreCase(w))
				{
					return s;
				}
			}
			return null;
		}
	}

	/** Words Kovac's commissions start with, and end with. */
	public static final Style[] FIRST = {Style.BROAD, Style.HEAVY, Style.LIGHT, Style.NARROW};
	public static final Style[] SECOND = {Style.FLAT, Style.HEAVY, Style.LIGHT, Style.SPIKED};

	private final String title;
	private final Part part;
	private final int level;
	private final int rep;
	private final int[] scores;

	Mould(String title, Part part, int level, int rep, int narrow, int broad, int light, int heavy, int flat, int spiked)
	{
		this.title = title;
		this.part = part;
		this.level = level;
		this.rep = rep;
		this.scores = new int[]{narrow, broad, light, heavy, flat, spiked};
	}

	public String getTitle()
	{
		return title;
	}

	public Part getPart()
	{
		return part;
	}

	/** The Smithing level to buy it; 15 for a default mould. */
	public int getLevel()
	{
		return level;
	}

	/** Its price in Foundry reputation; 0 for a default mould. */
	public int getRep()
	{
		return rep;
	}

	/** One of the six you start with for its part. */
	public boolean isDefault()
	{
		return rep == 0;
	}

	public int score(Style style)
	{
		return style == null ? 0 : scores[style.ordinal()];
	}

	/** What it adds to the mould score of a commission. */
	public int score(Style first, Style second)
	{
		return score(first) + score(second);
	}

	/** The mould a jig line names, ignoring case and tags, or null. */
	public static Mould ofTitle(String text)
	{
		if (text == null)
		{
			return null;
		}
		String t = text.replaceAll("<[^>]*>", "").trim();
		for (Mould m : values())
		{
			if (m.title.equalsIgnoreCase(t))
			{
				return m;
			}
		}
		return null;
	}

	/** Whether Kovac can ask for this pair of words. */
	public static boolean canCommission(Style first, Style second)
	{
		if (first == null || second == null || first == second)
		{
			return false;
		}
		boolean a = false;
		boolean b = false;
		for (Style s : FIRST)
		{
			a |= s == first;
		}
		for (Style s : SECOND)
		{
			b |= s == second;
		}
		return a && b;
	}
}
