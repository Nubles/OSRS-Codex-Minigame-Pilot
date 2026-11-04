package com.codex.minigamepilot.core;

import net.runelite.api.gameval.ItemID;

/**
 * The facts of Mastering Mixology, the Herblore minigame in Aldarin: its three pastes, the ten
 * potions with their levels, paste, resin and experience, the three stations, the herbs that make
 * each paste and the reward shop's prices.
 *
 * <p>Every figure is from the OSRS wiki (Mastering Mixology; Mox paste, Aga paste, Lye paste;
 * Mastering Mixology/Strategies); item ids are RuneLite's gameval names. The game numbers its
 * potions and stations in its order varbits: the potion as {@link Potion#ordinal()} + 1 and the
 * station as {@link Station#ordinal()} + 1, as the Mastering Mixology plugin by hex-agon (BSD-2)
 * reads them. That is a cross-check, not a source, so the Pilot also checks the order names
 * against the game's own order list when it is on screen.
 */
public final class Mixology
{
	/** Paste a lever adds to the vessel: each use of a reagent takes this much (wiki). */
	public static final int PASTE_PER_LEVER = 10;
	/** The hopper holds up to this much of each paste (wiki). */
	public static final int HOPPER_CAP = 3000;
	/** Resin bonus, per cent, for two and for three orders filled in one deposit (wiki). */
	public static final int BONUS_TWO = 20;
	public static final int BONUS_THREE = 40;
	/** The level from which every potion can be ordered (wiki). */
	public static final int ALL_RECIPES = 81;

	private Mixology()
	{
	}

	/** The three pastes, in the order the game's hopper lists them. */
	public enum Paste
	{
		MOX("Mox", 'M', ItemID.MM_MOX_PASTE),
		AGA("Aga", 'A', ItemID.MM_AGA_PASTE),
		LYE("Lye", 'L', ItemID.MM_LYE_PASTE);

		private final String title;
		private final char letter;
		private final int item;

		Paste(String title, char letter, int item)
		{
			this.title = title;
			this.letter = letter;
			this.item = item;
		}

		public String getTitle()
		{
			return title;
		}

		public char letter()
		{
			return letter;
		}

		/** The paste as an item, from the refiner, before it goes in the hopper. */
		public int item()
		{
			return item;
		}

		/** The paste a mixer slot varbit holds: 1 mox, 2 aga, 3 lye; null for empty or unknown. */
		public static Paste ofSlot(int value)
		{
			return value >= 1 && value <= 3 ? values()[value - 1] : null;
		}
	}

	/** The three stations and what each does to a potion (wiki). */
	public enum Station
	{
		AGITATOR("Agitator", "the agitator", "Homogenise", "homogenised"),
		RETORT("Retort", "the retort", "Concentrate", "concentrated"),
		ALEMBIC("Alembic", "the alembic", "Crystallise", "crystallised");

		private final String title;
		private final String the;
		private final String verb;
		private final String done;

		Station(String title, String the, String verb, String done)
		{
			this.title = title;
			this.the = the;
			this.verb = verb;
			this.done = done;
		}

		public String getTitle()
		{
			return title;
		}

		public String the()
		{
			return the;
		}

		public String getVerb()
		{
			return verb;
		}

		public String doneWord()
		{
			return done;
		}

		/**
		 * The order the wiki's strategies process in: crystallise, then concentrate, then
		 * homogenise. Lower goes first.
		 */
		public int rank()
		{
			switch (this)
			{
				case ALEMBIC:
					return 0;
				case RETORT:
					return 1;
				default:
					return 2;
			}
		}

		public Step.Target target()
		{
			switch (this)
			{
				case AGITATOR:
					return Step.Target.AGITATOR;
				case RETORT:
					return Step.Target.RETORT;
				default:
					return Step.Target.ALEMBIC;
			}
		}

		/** A station from an order's modifier varbit, or null. */
		public static Station ofVarbit(int value)
		{
			return value >= 1 && value <= 3 ? values()[value - 1] : null;
		}
	}

	/**
	 * The ten potions in the game's order. Code is the wiki's (its lever order); level, paste,
	 * resin ("ordered points"), experience and order weight are from the wiki's table.
	 */
	public enum Potion
	{
		MMM("Mammoth-might mix", "MMM", 60, 3, 0, 0, 20, 0, 0, 190, 5, ItemID.MM_POTION_MMM_UNFINISHED, ItemID.MM_POTION_MMM_FINISHED),
		MMA("Mystic mana amalgam", "MMA", 63, 2, 1, 0, 20, 10, 0, 215, 4, ItemID.MM_POTION_MMA_UNFINISHED, ItemID.MM_POTION_MMA_FINISHED),
		MML("Marley's moonlight", "MML", 66, 2, 0, 1, 20, 0, 10, 240, 4, ItemID.MM_POTION_MML_UNFINISHED, ItemID.MM_POTION_MML_FINISHED),
		AAA("Alco-augmentator", "AAA", 60, 0, 3, 0, 0, 20, 0, 190, 5, ItemID.MM_POTION_AAA_UNFINISHED, ItemID.MM_POTION_AAA_FINISHED),
		AAM("Azure aura mix", "AAM", 69, 1, 2, 0, 10, 20, 0, 265, 4, ItemID.MM_POTION_AAM_UNFINISHED, ItemID.MM_POTION_AAM_FINISHED),
		ALA("Aqualux amalgam", "ALA", 72, 0, 2, 1, 0, 20, 10, 290, 4, ItemID.MM_POTION_AAL_UNFINISHED, ItemID.MM_POTION_AAL_FINISHED),
		LLL("Liplack liquor", "LLL", 60, 0, 0, 3, 0, 0, 20, 190, 5, ItemID.MM_POTION_LLL_UNFINISHED, ItemID.MM_POTION_LLL_FINISHED),
		MLL("Megalite liquid", "MLL", 75, 1, 0, 2, 10, 0, 20, 315, 4, ItemID.MM_POTION_LLM_UNFINISHED, ItemID.MM_POTION_LLM_FINISHED),
		ALL("Anti-leech lotion", "ALL", 78, 0, 1, 2, 0, 10, 20, 340, 4, ItemID.MM_POTION_LLA_UNFINISHED, ItemID.MM_POTION_LLA_FINISHED),
		MAL("Mixalot", "MAL", 81, 1, 1, 1, 20, 20, 20, 365, 3, ItemID.MM_POTION_MAL_UNFINISHED, ItemID.MM_POTION_MAL_FINISHED);

		private final String title;
		private final String code;
		private final int level;
		private final int[] uses;
		private final int[] resin;
		private final int xp;
		private final int weight;
		private final int unfinished;
		private final int finished;

		Potion(String title, String code, int level, int mox, int aga, int lye, int rMox, int rAga, int rLye, int xp, int weight,
			int unfinished, int finished)
		{
			this.title = title;
			this.code = code;
			this.level = level;
			this.uses = new int[]{mox, aga, lye};
			this.resin = new int[]{rMox, rAga, rLye};
			this.xp = xp;
			this.weight = weight;
			this.unfinished = unfinished;
			this.finished = finished;
		}

		public String getTitle()
		{
			return title;
		}

		/** The wiki's three letters, such as "ALA". */
		public String code()
		{
			return code;
		}

		public int level()
		{
			return level;
		}

		/** Lever pulls of a paste the recipe takes. */
		public int uses(Paste p)
		{
			return uses[p.ordinal()];
		}

		/** Paste of a kind the potion takes from the hopper. */
		public int paste(Paste p)
		{
			return uses[p.ordinal()] * PASTE_PER_LEVER;
		}

		/** Resin of a kind an order of it pays, before any bonus. */
		public int resin(Paste p)
		{
			return resin[p.ordinal()];
		}

		public int totalResin()
		{
			return resin[0] + resin[1] + resin[2];
		}

		/** Herblore experience an ordered potion gives. */
		public int xp()
		{
			return xp;
		}

		/** How often it is ordered, against the others you can make (wiki "order weight"). */
		public int weight()
		{
			return weight;
		}

		/** The potion base from the vessel. */
		public int unfinished()
		{
			return unfinished;
		}

		/** The potion once a station has worked it. */
		public int finished()
		{
			return finished;
		}

		/** One paste three times over: mammoth-might mix, alco-augmentator, liplack liquor. */
		public boolean single()
		{
			return uses[0] == 3 || uses[1] == 3 || uses[2] == 3;
		}

		/** A potion from an order or station varbit, or null. */
		public static Potion ofVarbit(int value)
		{
			return value >= 1 && value <= values().length ? values()[value - 1] : null;
		}

		/** A potion by the name the game gives it, any case, or null. */
		public static Potion ofTitle(String name)
		{
			if (name == null)
			{
				return null;
			}
			String n = name.replaceAll("<[^>]*>", "").trim();
			for (Potion p : values())
			{
				if (p.title.equalsIgnoreCase(n))
				{
					return p;
				}
			}
			return null;
		}

		/** A potion from either of its item ids, or null. */
		public static Potion ofItem(int id)
		{
			for (Potion p : values())
			{
				if (p.unfinished == id || p.finished == id)
				{
					return p;
				}
			}
			return null;
		}
	}

	/** A herb or unfinished potion the refiner turns into paste, and how much (wiki paste pages). */
	public enum Herb
	{
		GUAM("Guam leaf", Paste.MOX, 10, ItemID.GUAM_LEAF, ItemID.GUAMVIAL),
		MARRENTILL("Marrentill", Paste.MOX, 13, ItemID.MARENTILL, ItemID.MARRENTILLVIAL),
		TARROMIN("Tarromin", Paste.MOX, 15, ItemID.TARROMIN, ItemID.TARROMINVIAL),
		HARRALANDER("Harralander", Paste.MOX, 20, ItemID.HARRALANDER, ItemID.HARRALANDERVIAL),
		RANARR("Ranarr weed", Paste.LYE, 26, ItemID.RANARR_WEED, ItemID.RANARRVIAL),
		TOADFLAX("Toadflax", Paste.LYE, 32, ItemID.TOADFLAX, ItemID.TOADFLAXVIAL),
		AVANTOE("Avantoe", Paste.LYE, 30, ItemID.AVANTOE, ItemID.AVANTOEVIAL),
		KWUARM("Kwuarm", Paste.LYE, 33, ItemID.KWUARM, ItemID.KWUARMVIAL),
		SNAPDRAGON("Snapdragon", Paste.LYE, 40, ItemID.SNAPDRAGON, ItemID.SNAPDRAGONVIAL),
		IRIT("Irit leaf", Paste.AGA, 30, ItemID.IRIT_LEAF, ItemID.IRITVIAL),
		HUASCA("Huasca", Paste.AGA, 20, ItemID.HUASCA, ItemID.HUASCAVIAL),
		CADANTINE("Cadantine", Paste.AGA, 34, ItemID.CADANTINE, ItemID.CADANTINEVIAL),
		LANTADYME("Lantadyme", Paste.AGA, 40, ItemID.LANTADYME, ItemID.LANTADYMEVIAL),
		DWARF_WEED("Dwarf weed", Paste.AGA, 42, ItemID.DWARF_WEED, ItemID.DWARFWEEDVIAL),
		TORSTOL("Torstol", Paste.AGA, 44, ItemID.TORSTOL, ItemID.TORSTOLVIAL);

		private final String title;
		private final Paste paste;
		private final int yield;
		private final int clean;
		private final int unf;

		Herb(String title, Paste paste, int yield, int clean, int unf)
		{
			this.title = title;
			this.paste = paste;
			this.yield = yield;
			this.clean = clean;
			this.unf = unf;
		}

		public String getTitle()
		{
			return title;
		}

		public Paste paste()
		{
			return paste;
		}

		/** Paste one herb, or one of its unfinished potions, gives. */
		public int yield()
		{
			return yield;
		}

		public int clean()
		{
			return clean;
		}

		public int unfinished()
		{
			return unf;
		}

		/** A herb by either of its item ids, or null. */
		public static Herb ofItem(int id)
		{
			for (Herb h : values())
			{
				if (h.clean == id || h.unf == id)
				{
					return h;
				}
			}
			return null;
		}
	}

	/** The reward shop's stock and its price in mox, aga and lye resin (wiki). */
	public enum Reward
	{
		APPRENTICE_PACK("Apprentice potion pack", 420, 70, 30, ItemID.MM_POTION_PACK_LOW),
		ADEPT_PACK("Adept potion pack", 180, 440, 70, ItemID.MM_POTION_PACK_MED),
		EXPERT_PACK("Expert potion pack", 410, 320, 480, ItemID.MM_POTION_PACK_HIGH),
		GOGGLES("Prescription goggles", 8600, 7000, 9350, ItemID.MM_ALCHEMIST_HAT),
		LABCOAT("Alchemist labcoat", 2250, 2800, 3700, ItemID.MM_ALCHEMIST_BODY),
		PANTS("Alchemist pants", 2250, 2800, 3700, ItemID.MM_ALCHEMIST_LEGS),
		GLOVES("Alchemist gloves", 2250, 2800, 3700, ItemID.MM_ALCHEMIST_GLOVES),
		REAGENT_POUCH("Reagent pouch", 13800, 11200, 15100, ItemID.MM_SECONDARY_POUCH),
		POTION_STORAGE("Potion storage", 7750, 6300, 8950, ItemID.MM_POTION_STORAGE_UNLOCK_DUMMY),
		CHUGGING_BARREL("Chugging barrel", 17250, 14000, 18600, ItemID.MM_PREPOT_DEVICE_DISASSEMBLED),
		AMULET("Alchemist's amulet", 6900, 5650, 7400, ItemID.AMULET_OF_CHEMISTRY_IMBUED_CHARGED),
		ALDARIUM("Aldarium", 80, 60, 90, ItemID.ALDARIUM);

		private final String title;
		private final int[] cost;
		private final int item;

		Reward(String title, int mox, int aga, int lye, int item)
		{
			this.title = title;
			this.cost = new int[]{mox, aga, lye};
			this.item = item;
		}

		public String getTitle()
		{
			return title;
		}

		public int cost(Paste p)
		{
			return cost[p.ordinal()];
		}

		public int item()
		{
			return item;
		}
	}

	/**
	 * Paste of a kind an order takes on average at a Herblore level: each potion you can be
	 * ordered weighed by its order weight (wiki). Tenths of paste are kept, so a balance at 75
	 * (39:35:31 by the wiki) shows.
	 */
	public static double pastePerOrder(Paste p, int level)
	{
		double sum = 0;
		double weights = 0;
		for (Potion potion : Potion.values())
		{
			if (potion.level() <= level)
			{
				sum += potion.weight() * potion.paste(p);
				weights += potion.weight();
			}
		}
		return weights == 0 ? 0 : sum / weights;
	}

	/** Resin of a kind an order pays on average at a level, with the bonus for filling all three at once. */
	public static double resinPerOrder(Paste p, int level)
	{
		double sum = 0;
		double weights = 0;
		for (Potion potion : Potion.values())
		{
			if (potion.level() <= level)
			{
				sum += potion.weight() * potion.resin(p);
				weights += potion.weight();
			}
		}
		return weights == 0 ? 0 : sum / weights * (100 + BONUS_THREE) / 100.0;
	}

	/** Resin a deposit pays for this many orders filled at once, before rounding: the wiki's 20% and 40%. */
	public static double withBonus(int resin, int orders)
	{
		int bonus = orders >= 3 ? BONUS_THREE : orders == 2 ? BONUS_TWO : 0;
		return resin * (100 + bonus) / 100.0;
	}
}
