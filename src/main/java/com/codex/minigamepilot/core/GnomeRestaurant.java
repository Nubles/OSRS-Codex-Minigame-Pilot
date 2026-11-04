package com.codex.minigamepilot.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;

/**
 * The facts of the Gnome Restaurant: Aluft Gianne's deliveries from the Grand Tree, the gnome
 * dishes and cocktails they ask for, step by step, and the customers who order them.
 *
 * <p>Figures and wording are from the OSRS wiki (Gnome Restaurant; Gnome cooking; each dish's
 * page and the pages of its half made, unfinished and mixed stages; Reward token (Gnome
 * Restaurant); Gianne jnr.'s and the customers' dialogue transcripts; Gnome goggles, Gnome scarf,
 * Mint cake, Grand seed pod), quoted beside each. Item ids are RuneLite's gameval names, checked
 * against the wiki's infoboxes where the gameval name and the wiki's differ.
 *
 * <p>Which order and customer each value of the game's two Gnome Restaurant varbits stands for is
 * not on the wiki: it is unconfirmed, from the Gnome Restaurant plugin on the Plugin Hub
 * (MMagicala, BSD-2), re-implemented here as facts. A delivery's own message names its dish and
 * customer, so what is kept in your log is always what the game said.
 */
public final class GnomeRestaurant
{
	/** "A minimum cooking level of 29 is required in order to complete the training." */
	public static final int TRAINING_COOKING = 29;
	/** "Easy tasks have a time limit of 6 minutes, whereas hard tasks have a limit of 11 minutes." */
	public static final long EASY_MS = 6 * 60_000L;
	public static final long HARD_MS = 11 * 60_000L;
	/** Easy: "one credit toward the 12 needed for one free food item"; hard: "three credits". */
	public static final int EASY_CREDITS = 1;
	public static final int HARD_CREDITS = 3;
	public static final int CREDITS_PER_FOOD = 12;
	/** The token gives "up to 10 free items of gnome food". */
	public static final int MAX_FOODS = 10;
	/** Declining a hard job: "a 5 minute and 30 second waiting penalty". */
	public static final long HARD_DECLINE_MS = 5 * 60_000L + 30_000L;
	/** Declining an easy one: "you'll have to wait 5 minutes before I can give you another easy order." */
	public static final long EASY_DECLINE_MS = 5 * 60_000L;

	/** The order and the customer, as the game keeps them (gameval ALUFT_CURRENT_ORDER, ALUFT_CURRENT_CUSTOMER); 0 for none. */
	public static final int ORDER_VARBIT = VarbitID.ALUFT_CURRENT_ORDER;
	public static final int CUSTOMER_VARBIT = VarbitID.ALUFT_CURRENT_CUSTOMER;

	/** Gianne jnr., who hands out the deliveries, by name: "Talk to Gianne jnr." */
	public static final String GIANNE = "Gianne jnr.";

	/** The customers' game messages and lines (OSRS wiki, Transcript: Captain Bleemadge; Rometti). */
	static final String HAND_OVER = "You hand over your delivery of ";
	static final Pattern HAND_OVER_LINE = Pattern.compile("^You hand over your delivery of (?:an? |some )?(.+?) to (.+?)\\.?$");
	static final Pattern TIP_LINE = Pattern.compile("^You are given (?:an? |some )?(.+?) as a tip\\.?$");
	static final Pattern CREDITS_LINE = Pattern.compile("^You have (\\d{1,4}) credits towards your free delivery token");
	/** "You discard your delivery box as you don't need it any more." */
	public static final String DISCARD = "You discard your delivery box as you don't need it any more.";

	/** A RuneLite world-map point: where a customer stands, by the map's own data. */
	public static final class Anchor
	{
		public final int x;
		public final int y;
		/** The world-map entry it comes from. */
		public final String source;

		Anchor(int x, int y, String source)
		{
			this.x = x;
			this.y = y;
			this.source = source;
		}
	}

	/**
	 * RuneLite's world map (net.runelite.client.plugins.worldmap): MinigameLocation.GNOME_RESTAURANT
	 * and the TransportationPointLocation gnome gliders. The wiki gives no coordinates for the
	 * customers; these are RuneLite's own map points, copied because those enums are not public.
	 */
	public static final Anchor RESTAURANT = new Anchor(2436, 3502, "RuneLite world map: Gnome Restaurant");
	static final Anchor GLIDER_STRONGHOLD = new Anchor(2460, 3502, "RuneLite world map: Gnome Glider (Grand Tree)");
	static final Anchor GLIDER_WHITE_WOLF = new Anchor(2845, 3501, "RuneLite world map: Gnome Glider (White Wolf Mountain)");
	static final Anchor GLIDER_KHARID = new Anchor(3278, 3213, "RuneLite world map: Gnome Glider (Al Kharid)");
	static final Anchor GLIDER_KARAMJA = new Anchor(2971, 2974, "RuneLite world map: Gnome Glider (Karamja)");
	static final Anchor GLIDER_FELDIP = new Anchor(2540, 2969, "RuneLite world map: Gnome Glider (Feldip Hills)");

	/** A tip only some customers give (OSRS wiki, each item's page). */
	public enum Tip
	{
		/** "a 1/17 chance to obtain them as a reward from Captain Daerkin or Captain Ninto". */
		GNOME_GOGGLES("Gnome goggles", ItemID.ALUFT_GNOME_GOGGLES, "1 in 17"),
		/** "Players have a 1/17 chance of receiving this from Captain Daerkin and Captain Ninto." */
		GNOME_SCARF("Gnome scarf", ItemID.ALUFT_GNOME_SCARF, "1 in 17"),
		/** "obtained as a tip from Brambickle, Wingstone, and Penwie"; the item sources table: 23/512. */
		MINT_CAKE("Mint cake", ItemID.ALUFT_GNOME_MINT_CAKE, "23 in 512"),
		/** "1/22, but only from Manglethorp and the three exploring gnomes" (Mod Ash, quoted by the wiki); 5 pods. */
		GRAND_SEED_POD("5 grand seed pods", ItemID.ALUFT_SEED_POD, "1 in 22");

		private final String title;
		private final int item;
		private final String odds;

		Tip(String title, int item, String odds)
		{
			this.title = title;
			this.item = item;
			this.odds = odds;
		}

		public String getTitle()
		{
			return title;
		}

		public int item()
		{
			return item;
		}

		public String odds()
		{
			return odds;
		}
	}

	/**
	 * Who orders: the wiki's easy and hard order tables, with Gianne jnr.'s and the tables' word on
	 * where each is. The varbit value is unconfirmed (source: Gnome Restaurant plugin).
	 */
	public enum Customer
	{
		// Easy: "in or around the Tree Gnome Stronghold".
		TRAINER_NACKLEPEN("Trainer Nacklepen", false, 1, "The tortoise pen north of the swamp, north-west of the Grand Tree", null, null),
		KING_NARNODE("King Narnode Shareen", false, 2, "At the base of the Grand Tree", null, null),
		FEMI("Femi", false, 3, "Just outside the Stronghold's south gates", null, null),
		HECKEL_FUNCH("Heckel Funch", false, 4, "His shop on the east side of the Grand Tree, Aluft's level", null, null),
		GULLUCK("Gulluck", false, 5, "The armourer, upstairs in the Grand Tree, south side", null, null),
		ROMETTI("Rometti", false, 6, "The master tailor, north-east on the Grand Tree's second level", null, null),
		COACH("Gnome Coach", false, 7, "Walking clockwise outside the Gnome Ball field", null, null),
		WURBEL("Wurbel", false, 8, "A Gnome Ball fan in a tree south of the field", null, null),
		EEBEL("Eebel", false, 9, "A hut at the southern end of the Stronghold, east of the cave", null, null),
		DAMWIN("Damwin", false, 10, "Drinking in the bar, second tree east of the Gnome Ball field", null, null),
		SARBLE("Sarble", false, 11, "The swamp field west of the Grand Tree", null, null),
		BURKOR("Burkor", false, 12, "The guard tower south-east of the Gnome Ball field", null, null),
		MEEGLE("Meegle", false, 13, "The terrorbird pen, south-west Stronghold, west of the bridge", null, null),
		GUARD_VEMMELDO("Guard Vemmeldo", false, 14, "The bank west of the agility course", null, null),
		ERMIN("Ermin", false, 15, "Upstairs by a spinner, south-east of the agility course", null, null),
		FROONO("Froono", false, 16, "In a tree south of the bank, above the magic trees", null, null),
		BRIMSTAIL("Brimstail", false, 17, "His cave in the south-west Stronghold, east of the bridge", null, null),
		CAPTAIN_ERRDO("Captain Errdo", false, 18, "The glider on the Grand Tree's top level", GLIDER_STRONGHOLD, null),
		PERRDUR("Perrdur", false, 19, "In the restaurant with Dalila, north-west on the Grand Tree's second level", null, null),
		DALILA("Dalila", false, 20, "In the restaurant with Perrdur, north-west on the Grand Tree's second level", null, null),
		// Hard: "anywhere in Gielinor".
		HAZELMERE("Hazelmere", true, 101, "His hut on an island east of Yanille", null, null),
		GNORMADIUM_AVLAFRIM("Gnormadium Avlafrim", true, 102, "Far south in the Feldip Hills, by the glider", GLIDER_FELDIP, "One Small Favour for the Feldip glider"),
		CAPTAIN_NINTO("Captain Ninto", true, 103, "The dwarf bar beneath White Wolf Mountain", null, "Fishing Contest for the tunnel"),
		CAPTAIN_DAERKIN("Captain Daerkin", true, 104, "Top of the south-western arena in Emir's Arena", null, null),
		BRAMBICKLE("Brambickle", true, 105, "North Trollweiss Mountain, near the Ice Troll Caves' exit", null, "Troll Stronghold started"),
		WINGSTONE("Wingstone", true, 106, "Surveying the southern Kharidian Desert", null, null),
		PENWIE("Penwie", true, 107, "Exploring Karamja", null, null),
		AMBASSADOR_GIMBLEWAP("Ambassador Gimblewap", true, 108, "Upstairs in Ardougne Castle", null, null),
		AMBASSADOR_SPANFIPPLE("Ambassador Spanfipple", true, 109, "Upstairs in the White Knights' Castle, Falador", null, null),
		AMBASSADOR_FERRNOOK("Ambassador Ferrnook", true, 110, "Upstairs in Varrock Palace", null, null),
		PROFESSOR_IMBLEWYN("Professor Imblewyn", true, 111, "The Mages' Guild in Yanille", null, "66 Magic, can be boosted"),
		PROFESSOR_MANGLETHORP("Professor Manglethorp", true, 112, "Visiting the engineers in Keldagrim", null, "The Giant Dwarf"),
		PROFESSOR_ONGLEWIP("Professor Onglewip", true, 113, "The Wizards' Tower", null, null),
		GARKOR("Garkor", true, 114, "Near the king on Ape Atoll; after Monkey Madness II, Glough's old house", null, "Monkey Madness I"),
		KING_BOLREN("King Bolren", true, 115, "Inside the maze of the Tree Gnome Village", null, null),
		GLO_CARANOCK("G.L.O. Caranock", true, 116, "The Ship Yard on Karamja", null, "The Grand Tree"),
		LIEUTENANT_SCHEPBUR("Lieutenant Schepbur", true, 117, "Commanding the battle tortoises on the Khazard Battlefield", null, null),
		CAPTAIN_DALBUR("Captain Dalbur", true, 118, "The glider in Al Kharid", GLIDER_KHARID, null),
		CAPTAIN_BLEEMADGE("Captain Bleemadge", true, 119, "The glider on top of White Wolf Mountain", GLIDER_WHITE_WOLF, null),
		CAPTAIN_KLEMFOODLE("Captain Klemfoodle", true, 120, "The glider on Karamja", GLIDER_KARAMJA, null);

		private final String title;
		private final boolean hard;
		private final int varbit;
		private final String where;
		private final Anchor anchor;
		private final String needs;

		Customer(String title, boolean hard, int varbit, String where, Anchor anchor, String needs)
		{
			this.title = title;
			this.hard = hard;
			this.varbit = varbit;
			this.where = where;
			this.anchor = anchor;
			this.needs = needs;
		}

		/** The NPC's name. */
		public String getTitle()
		{
			return title;
		}

		public boolean isHard()
		{
			return hard;
		}

		public String where()
		{
			return where;
		}

		/** A RuneLite world-map point at the customer, or null where the map has none. */
		public Anchor anchor()
		{
			return anchor;
		}

		/** A quest or level the wiki recommends to reach them, or null. */
		public String needs()
		{
			return needs;
		}

		/** Tips only this customer gives. */
		public List<Tip> tips()
		{
			switch (this)
			{
				case CAPTAIN_DAERKIN:
				case CAPTAIN_NINTO:
					return Arrays.asList(Tip.GNOME_GOGGLES, Tip.GNOME_SCARF);
				case BRAMBICKLE:
				case WINGSTONE:
				case PENWIE:
					return Arrays.asList(Tip.MINT_CAKE, Tip.GRAND_SEED_POD);
				case PROFESSOR_MANGLETHORP:
					return Collections.singletonList(Tip.GRAND_SEED_POD);
				default:
					return Collections.emptyList();
			}
		}

		/** The time to deliver: six minutes easy, eleven hard. */
		public long limitMs()
		{
			return hard ? HARD_MS : EASY_MS;
		}

		public int credits()
		{
			return hard ? HARD_CREDITS : EASY_CREDITS;
		}

		/** By the game's varbit value, or null (unconfirmed mapping: Gnome Restaurant plugin). */
		public static Customer ofVarbit(int value)
		{
			for (Customer c : values())
			{
				if (c.varbit == value)
				{
					return c;
				}
			}
			return null;
		}

		public int varbit()
		{
			return varbit;
		}

		/** By the NPC's name as the game writes it, or null. */
		public static Customer named(String name)
		{
			if (name == null)
			{
				return null;
			}
			String n = name.trim();
			for (Customer c : values())
			{
				if (c.title.equalsIgnoreCase(n))
				{
					return c;
				}
			}
			// Gianne jnr. calls him "Coach", the wiki "Gnome Coach"; "G.L.O Caranock" in her lines.
			if ("Coach".equalsIgnoreCase(n))
			{
				return COACH;
			}
			return n.replace(".", "").equalsIgnoreCase("GLO Caranock") ? GLO_CARANOCK : null;
		}
	}

	/** An ingredient or tool, with the wiki's name for it. */
	public enum Ingredient
	{
		GIANNE_DOUGH(ItemID.GIANNE_DOUGH, "gianne dough", "gianne dough", null),
		BATTA_TIN(ItemID.BATTA_TIN, "batta tin", "batta tins", null),
		CRUNCHY_TRAY(ItemID.CRUNCHY_TRAY, "crunchy tray", "crunchy trays", null),
		GNOMEBOWL_MOULD(ItemID.GNOMEBOWL_MOULD, "gnomebowl mould", "gnomebowl moulds", null),
		GNOME_SPICE(ItemID.GNOME_SPICE, "gnome spice", "gnome spice", null),
		EQUA_LEAVES(ItemID.EQUA_LEAVES, "equa leaves", "equa leaves", null),
		CHEESE(ItemID.CHEESE, "cheese", "cheese", null),
		TOMATO(ItemID.TOMATO, "tomato", "tomatoes", null),
		ONION(ItemID.ONION, "onion", "onions", null),
		POTATO(ItemID.POTATO, "potato", "potatoes", null),
		CABBAGE(ItemID.CABBAGE, "cabbage", "cabbages", null),
		DWELLBERRIES(ItemID.DWELLBERRIES, "dwellberries", "dwellberries", null),
		TOADS_LEGS(ItemID.TOADS_LEGS, "toad's legs", "toad's legs", null),
		KING_WORM(ItemID.KING_WORM, "king worm", "king worms", null),
		CHOCOLATE_BAR(ItemID.CHOCOLATE_BAR, "chocolate bar", "chocolate bars", null),
		CHOCOLATE_DUST(ItemID.CHOCOLATE_DUST, "chocolate dust", "chocolate dust", null),
		POT_OF_CREAM(ItemID.POT_OF_CREAM, "pot of cream", "pots of cream", null),
		BUCKET_OF_MILK(ItemID.BUCKET_MILK, "bucket of milk", "buckets of milk", null),
		PINEAPPLE(ItemID.PINEAPPLE, "pineapple", "pineapples", null),
		ORANGE(ItemID.ORANGE, "orange", "oranges", null),
		LEMON(ItemID.LEMON, "lemon", "lemons", null),
		LIME(ItemID.LIME, "lime", "limes", null),
		PINEAPPLE_CHUNKS(ItemID.PINEAPPLE_CHUNKS, "pineapple chunks", "pineapple chunks", PINEAPPLE),
		ORANGE_CHUNKS(ItemID.ORANGE_CHUNKS, "orange chunks", "orange chunks", ORANGE),
		LEMON_CHUNKS(ItemID.LEMON_CHUNKS, "lemon chunks", "lemon chunks", LEMON),
		LIME_CHUNKS(ItemID.LIME_CHUNKS, "lime chunks", "lime chunks", LIME),
		ORANGE_SLICES(ItemID.ORANGE_SLICES, "orange slices", "orange slices", ORANGE),
		LEMON_SLICES(ItemID.LEMON_SLICES, "lemon slices", "lemon slices", LEMON),
		LIME_SLICES(ItemID.LIME_SLICES, "lime slices", "lime slices", LIME),
		VODKA(ItemID.VODKA, "vodka", "vodka", null),
		GIN(ItemID.GIN, "gin", "gin", null),
		BRANDY(ItemID.BRANDY, "brandy", "brandy", null),
		WHISKY(ItemID.WHISKY, "whisky", "whisky", null),
		COCKTAIL_SHAKER(ItemID.COCKTAIL_SHAKER, "cocktail shaker", "cocktail shakers", null),
		COCKTAIL_GLASS(ItemID.COCKTAIL_GLASS_EMPTY, "cocktail glass", "cocktail glasses", null);

		private final int item;
		private final String one;
		private final String many;
		private final Ingredient cutFrom;

		Ingredient(int item, String one, String many, Ingredient cutFrom)
		{
			this.item = item;
			this.one = one;
			this.many = many;
			this.cutFrom = cutFrom;
		}

		public int item()
		{
			return item;
		}

		/** "1 tomato", "2 tomatoes". */
		public String count(int n)
		{
			return n + " " + (n == 1 ? one : many);
		}

		public String getTitle()
		{
			return one;
		}

		/** The whole fruit it is cut from with a knife ("using a knife on a lime"), or null. */
		public Ingredient cutFrom()
		{
			return cutFrom;
		}

		/** "chunks" or "slices", for a cut fruit. */
		public String cut()
		{
			return one.endsWith("chunks") ? "chunks" : "slices";
		}
	}

	/** One step of a recipe: what it makes, what it takes, and whether it is done on a range. */
	public static final class Stage
	{
		/** A short word for the loop strip: Dough, Bake, Prepare, Garnish, Mix, Pour, Heat. */
		public final String name;
		/** The step as the panel says it. */
		public final String text;
		/** The item it makes. */
		public final int makes;
		/** What it uses up besides the item before it. */
		public final List<Ingredient> uses;
		public final List<Integer> counts;
		/** Carried but kept: gnome spice "will never run out while making Gnome foods". */
		public final List<Ingredient> tools;
		/** Cooked on a range. */
		public final boolean range;

		Stage(String name, String text, int makes, boolean range, Object... parts)
		{
			this.name = name;
			this.text = text;
			this.makes = makes;
			this.range = range;
			List<Ingredient> u = new ArrayList<>();
			List<Integer> c = new ArrayList<>();
			List<Ingredient> t = new ArrayList<>();
			for (int i = 0; i < parts.length; i++)
			{
				if (parts[i] == Ingredient.GNOME_SPICE)
				{
					t.add(Ingredient.GNOME_SPICE);
					continue;
				}
				int n = 1;
				if (parts[i] instanceof Integer)
				{
					n = (Integer) parts[i];
					i++;
				}
				u.add((Ingredient) parts[i]);
				c.add(n);
			}
			this.uses = Collections.unmodifiableList(u);
			this.counts = Collections.unmodifiableList(c);
			this.tools = Collections.unmodifiableList(t);
		}

		/** "1 cheese, 1 tomato and gnome spice", or "" when it takes nothing more. */
		public String takes()
		{
			List<String> parts = new ArrayList<>();
			for (int i = 0; i < uses.size(); i++)
			{
				parts.add(uses.get(i).count(counts.get(i)));
			}
			for (Ingredient t : tools)
			{
				parts.add(t.getTitle());
			}
			return GnomeRestaurant.join(parts);
		}
	}

	/** What a dish is baked in, or a cocktail. */
	public enum Kind
	{
		CRUNCHIES("Crunchies"),
		BATTA("Battas"),
		GNOMEBOWL("Gnomebowls"),
		COCKTAIL("Cocktails");

		private final String title;

		Kind(String title)
		{
			this.title = title;
		}

		public String getTitle()
		{
			return title;
		}
	}

	/**
	 * Every dish and cocktail an order can ask for ("The hard jobs could require you to cook any
	 * piece of gnome cuisine or cocktail"), with its Cooking level and experience from the wiki's
	 * Gnome cooking table and pages, and its steps from each page's creation section. Cocktails'
	 * experience differs between their pages' text and tables, so none is given. The varbit value
	 * is unconfirmed (source: Gnome Restaurant plugin).
	 */
	public enum Dish
	{
		FRUIT_BLAST("Fruit blast", Kind.COCKTAIL, 6, 0, 1, ItemID.FRUIT_BLAST),
		PINEAPPLE_PUNCH("Pineapple punch", Kind.COCKTAIL, 8, 0, 2, ItemID.PINEAPPLE_PUNCH),
		TOAD_CRUNCHIES("Toad crunchies", Kind.CRUNCHIES, 10, 100, 3, ItemID.TOAD_CRUNCHIES),
		SPICY_CRUNCHIES("Spicy crunchies", Kind.CRUNCHIES, 12, 102, 4, ItemID.SPICY_CRUNCHIES),
		WORM_CRUNCHIES("Worm crunchies", Kind.CRUNCHIES, 14, 104, 5, ItemID.WORM_CRUNCHIES),
		CHOCCHIP_CRUNCHIES("Chocchip crunchies", Kind.CRUNCHIES, 16, 106, 6, ItemID.CHOCCHIP_CRUNCHIES),
		WIZARD_BLIZZARD("Wizard blizzard", Kind.COCKTAIL, 18, 0, 7, ItemID.WIZARD_BLIZZARD),
		SHORT_GREEN_GUY("Short green guy", Kind.COCKTAIL, 20, 0, 8, ItemID.SGG),
		FRUIT_BATTA("Fruit batta", Kind.BATTA, 25, 150, 9, ItemID.FRUIT_BATTA),
		TOAD_BATTA("Toad batta", Kind.BATTA, 26, 152, 10, ItemID.TOAD_BATTA),
		WORM_BATTA("Worm batta", Kind.BATTA, 27, 154, 11, ItemID.WORM_BATTA),
		/** 28 by the Gnome cooking table and "requiring 28 Cooking" to bake it; its own page's text says 30. */
		VEGETABLE_BATTA("Vegetable batta", Kind.BATTA, 28, 156, 12, ItemID.VEGETABLE_BATTA),
		CHEESE_TOM_BATTA("Cheese+tom batta", Kind.BATTA, 29, 158, 13, ItemID.CHEESE_TOM_BATTA),
		WORM_HOLE("Worm hole", Kind.GNOMEBOWL, 30, 170, 14, ItemID.WORM_HOLE),
		DRUNK_DRAGON("Drunk dragon", Kind.COCKTAIL, 32, 0, 15, ItemID.DRUNK_DRAGON),
		VEG_BALL("Veg ball", Kind.GNOMEBOWL, 35, 175, 16, ItemID.VEG_BALL),
		CHOC_SATURDAY("Choc saturday", Kind.COCKTAIL, 33, 0, 17, ItemID.CHOCOLATE_SATURDAY),
		BLURBERRY_SPECIAL("Blurberry special", Kind.COCKTAIL, 37, 0, 18, ItemID.BLURBERRY_SPECIAL),
		TANGLED_TOADS_LEGS("Tangled toad's legs", Kind.GNOMEBOWL, 40, 185, 19, ItemID.TANGLED_TOADS_LEGS),
		CHOCOLATE_BOMB("Chocolate bomb", Kind.GNOMEBOWL, 42, 190, 20, ItemID.CHOCOLATE_BOMB);

		private final String title;
		private final Kind kind;
		private final int level;
		private final int xp;
		private final int varbit;
		private final int item;
		private List<Stage> stages;

		Dish(String title, Kind kind, int level, int xp, int varbit, int item)
		{
			this.title = title;
			this.kind = kind;
			this.level = level;
			this.xp = xp;
			this.varbit = varbit;
			this.item = item;
		}

		public String getTitle()
		{
			return title;
		}

		public Kind kind()
		{
			return kind;
		}

		/** The Cooking level it needs (boostable). */
		public int level()
		{
			return level;
		}

		/** Its whole recipe's experience by the wiki, or 0 where the wiki disagrees with itself (cocktails). */
		public int xp()
		{
			return xp;
		}

		/** The finished item, the one delivered. */
		public int item()
		{
			return item;
		}

		public int varbit()
		{
			return varbit;
		}

		/** Its steps in order, the last making the finished item. */
		public synchronized List<Stage> stages()
		{
			if (stages == null)
			{
				stages = Collections.unmodifiableList(Recipes.of(this));
			}
			return stages;
		}

		/** By the game's varbit value, or null (unconfirmed mapping: Gnome Restaurant plugin). */
		public static Dish ofVarbit(int value)
		{
			for (Dish d : values())
			{
				if (d.varbit == value)
				{
					return d;
				}
			}
			return null;
		}

		/** By its item name as a message writes it ("cheese+tom batta", "Choc saturday"), or null. */
		public static Dish named(String name)
		{
			if (name == null)
			{
				return null;
			}
			String n = name.trim().toLowerCase(Locale.ROOT);
			for (Dish d : values())
			{
				String t = d.title.toLowerCase(Locale.ROOT);
				if (n.equals(t) || n.equals(t + "s") || (t.endsWith("s") && n.equals(t.substring(0, t.length() - 1))))
				{
					return d;
				}
			}
			if (n.equals("chocolate saturday"))
			{
				return CHOC_SATURDAY;
			}
			if (n.equals("vegetable ball"))
			{
				return VEG_BALL;
			}
			return n.equals("cheese and tomato batta") ? CHEESE_TOM_BATTA : null;
		}
	}

	/** The recipes, step by step, as each dish's wiki page gives them. */
	static final class Recipes
	{
		private Recipes()
		{
		}

		static List<Stage> of(Dish d)
		{
			switch (d)
			{
				// Crunchies: "Use a gianne dough on a crunchy tray to make a raw crunchies", "Cook the raw crunchies
				// on a range to make half baked crunchy", prepare, cook, garnish.
				case TOAD_CRUNCHIES:
					return crunchies("toad", ItemID.ALUFT_HALF_BAKED_TOAD_CRUNCHY, ItemID.ALUFT_BAKED_TOAD_CRUNCHY, d,
						new Object[]{2, Ingredient.TOADS_LEGS, Ingredient.GNOME_SPICE}, new Object[]{Ingredient.EQUA_LEAVES});
				case SPICY_CRUNCHIES:
					return crunchies("spicy", ItemID.ALUFT_HALF_BAKED_SPICY_CRUNCHY, ItemID.ALUFT_BAKED_SPICY_CRUNCHY, d,
						new Object[]{2, Ingredient.EQUA_LEAVES, Ingredient.GNOME_SPICE}, new Object[]{Ingredient.GNOME_SPICE});
				case WORM_CRUNCHIES:
					return crunchies("worm", ItemID.ALUFT_HALF_BAKED_WORM_CRUNCHY, ItemID.ALUFT_BAKED_WORM_CRUNCHY, d,
						new Object[]{2, Ingredient.KING_WORM, Ingredient.EQUA_LEAVES, Ingredient.GNOME_SPICE}, new Object[]{Ingredient.GNOME_SPICE});
				case CHOCCHIP_CRUNCHIES:
					return crunchies("chocchip", ItemID.ALUFT_HALF_BAKED_CHOC_CHIP_CRUNCHY, ItemID.ALUFT_BAKED_CHOC_CHIP_CRUNCHY, d,
						new Object[]{2, Ingredient.CHOCOLATE_BAR, Ingredient.GNOME_SPICE}, new Object[]{Ingredient.CHOCOLATE_DUST});
				// Battas.
				case FRUIT_BATTA:
					return batta("fruit", ItemID.ALUFT_HALF_MADE_FRUIT_BATTA, ItemID.ALUFT_BAKED_FRUIT_BATTA, d,
						new Object[]{4, Ingredient.EQUA_LEAVES, Ingredient.LIME_CHUNKS, Ingredient.ORANGE_CHUNKS, Ingredient.PINEAPPLE_CHUNKS},
						new Object[]{Ingredient.GNOME_SPICE});
				case TOAD_BATTA:
					// "Cooking this will create an unfinished batta, except in the case of a toad batta".
					return batta("toad", ItemID.ALUFT_HALF_MADE_TOAD_BATTA, -1, d,
						new Object[]{Ingredient.EQUA_LEAVES, Ingredient.CHEESE, Ingredient.TOADS_LEGS, Ingredient.GNOME_SPICE}, null);
				case WORM_BATTA:
					return batta("worm", ItemID.ALUFT_HALF_MADE_WORM_BATTA, ItemID.ALUFT_BAKED_WORM_BATTA, d,
						new Object[]{Ingredient.KING_WORM, Ingredient.CHEESE, Ingredient.GNOME_SPICE}, new Object[]{Ingredient.EQUA_LEAVES});
				case VEGETABLE_BATTA:
					return batta("vegetable", ItemID.ALUFT_HALF_MADE_VEG_BATTA, ItemID.ALUFT_BAKED_VEG_BATTA, d,
						new Object[]{2, Ingredient.TOMATO, Ingredient.DWELLBERRIES, Ingredient.ONION, Ingredient.CHEESE, Ingredient.CABBAGE},
						new Object[]{Ingredient.EQUA_LEAVES});
				case CHEESE_TOM_BATTA:
					return batta("cheese+tom", ItemID.ALUFT_HALF_MADE_CHEESE_TOM_BATTA, ItemID.ALUFT_BAKED_CHEESE_TOM_BATTA, d,
						new Object[]{Ingredient.CHEESE, Ingredient.TOMATO}, new Object[]{Ingredient.EQUA_LEAVES});
				// Gnomebowls.
				case WORM_HOLE:
					return bowl("worm hole", ItemID.ALUFT_HALF_BAKED_WORM_HOLE, ItemID.ALUFT_BAKED_WORM_HOLE, d,
						new Object[]{4, Ingredient.KING_WORM, 2, Ingredient.ONION, Ingredient.GNOME_SPICE}, new Object[]{Ingredient.EQUA_LEAVES});
				case VEG_BALL:
					return bowl("veg ball", ItemID.ALUFT_HALF_BAKED_VEG_BALL, ItemID.ALUFT_BAKED_VEG_BALL, d,
						new Object[]{2, Ingredient.ONION, 2, Ingredient.POTATO, Ingredient.GNOME_SPICE}, new Object[]{Ingredient.EQUA_LEAVES});
				case TANGLED_TOADS_LEGS:
					// "It needs to be baked again to create an unfinished bowl, or the final dish when making tangled toad's legs".
					return bowl("tangled toad's legs", ItemID.ALUFT_HALF_BAKED_TANGLED_TOADS_LEGS, -1, d,
						new Object[]{4, Ingredient.TOADS_LEGS, 2, Ingredient.CHEESE, 2, Ingredient.EQUA_LEAVES, Ingredient.DWELLBERRIES, Ingredient.GNOME_SPICE},
						null);
				case CHOCOLATE_BOMB:
					return bowl("chocolate bomb", ItemID.ALUFT_HALF_BAKED_CHOC_BOMB, ItemID.ALUFT_BAKED_CHOC_BOMB, d,
						new Object[]{4, Ingredient.CHOCOLATE_BAR, Ingredient.EQUA_LEAVES}, new Object[]{2, Ingredient.POT_OF_CREAM, Ingredient.CHOCOLATE_DUST});
				// Cocktails: "Mix-cocktail" on a cocktail shaker, then "Pour".
				case FRUIT_BLAST:
					return poured("blast", ItemID.ALUFT_SHAKER_FRUIT_BLAST, d,
						new Object[]{Ingredient.PINEAPPLE, Ingredient.LEMON, Ingredient.ORANGE}, new Object[]{Ingredient.LEMON_SLICES});
				case PINEAPPLE_PUNCH:
					return poured("punch", ItemID.ALUFT_SHAKER_PINEAPPLE_PUNCH, d,
						new Object[]{2, Ingredient.PINEAPPLE, Ingredient.LEMON, Ingredient.ORANGE},
						new Object[]{Ingredient.LIME_CHUNKS, Ingredient.PINEAPPLE_CHUNKS, Ingredient.ORANGE_SLICES});
				case WIZARD_BLIZZARD:
					return poured("blizzard", ItemID.ALUFT_SHAKER_WIZZARD_BLIZZARD, d,
						new Object[]{2, Ingredient.VODKA, Ingredient.GIN, Ingredient.LIME, Ingredient.LEMON, Ingredient.ORANGE},
						new Object[]{Ingredient.PINEAPPLE_CHUNKS, Ingredient.LIME_SLICES});
				case SHORT_GREEN_GUY:
					return poured("sgg", ItemID.ALUFT_SHAKER_SGG, d,
						new Object[]{Ingredient.VODKA, 3, Ingredient.LIME}, new Object[]{Ingredient.LIME_SLICES, Ingredient.EQUA_LEAVES});
				case BLURBERRY_SPECIAL:
					return poured("special", ItemID.ALUFT_SHAKER_BLURBERRY_SPECIAL, d,
						new Object[]{Ingredient.VODKA, Ingredient.BRANDY, Ingredient.GIN, 2, Ingredient.LEMON, Ingredient.ORANGE},
						new Object[]{Ingredient.LEMON_CHUNKS, Ingredient.ORANGE_CHUNKS, Ingredient.EQUA_LEAVES, Ingredient.LIME_SLICES});
				case CHOC_SATURDAY:
				{
					// Mixed saturday (shaker) 9571, (poured) 9572, (heated) 9573; "garnished with chocolate dust and a pot of cream".
					List<Stage> s = new ArrayList<>();
					s.add(new Stage("Mix", "Mix whisky, a chocolate bar, a bucket of milk and equa leaves in a cocktail shaker",
						ItemID.ALUFT_SHAKER_CHOC_SATURDAY, false, Ingredient.WHISKY, Ingredient.CHOCOLATE_BAR, Ingredient.BUCKET_OF_MILK,
						Ingredient.EQUA_LEAVES, Ingredient.COCKTAIL_SHAKER));
					s.add(new Stage("Pour", "Pour the mixed saturday into a cocktail glass", ItemID.ALUFT_SHAKER_CHOC_SATURDAY_PREHEAT, false,
						Ingredient.COCKTAIL_GLASS));
					s.add(new Stage("Heat", "Heat the mixed saturday on a range", ItemID.ALUFT_SHAKER_CHOC_SATURDAY_HEATED, true));
					s.add(new Stage("Garnish", "Add chocolate dust and a pot of cream to finish the choc saturday", d.item, false,
						Ingredient.CHOCOLATE_DUST, Ingredient.POT_OF_CREAM));
					return s;
				}
				case DRUNK_DRAGON:
				{
					// Mixed dragon (shaker) 9574, (poured) 9575, (garnished) 9576; "heated on a range, creating a Drunk dragon".
					List<Stage> s = new ArrayList<>();
					s.add(new Stage("Mix", "Mix vodka, gin and dwellberries in a cocktail shaker", ItemID.ALUFT_SHAKER_DRUNK_DRAGON, false,
						Ingredient.VODKA, Ingredient.GIN, Ingredient.DWELLBERRIES, Ingredient.COCKTAIL_SHAKER));
					s.add(new Stage("Pour", "Pour the mixed dragon into a cocktail glass", ItemID.ALUFT_SHAKER_DRUNK_DRAGON_PREFINISH, false,
						Ingredient.COCKTAIL_GLASS));
					s.add(new Stage("Garnish", "Add pineapple chunks and a pot of cream", ItemID.ALUFT_SHAKER_DRUNK_DRAGON_PREHEAT, false,
						Ingredient.PINEAPPLE_CHUNKS, Ingredient.POT_OF_CREAM));
					s.add(new Stage("Heat", "Heat it on a range to finish the drunk dragon", d.item, true));
					return s;
				}
				default:
					throw new IllegalStateException("No recipe for " + d);
			}
		}

		private static List<Stage> crunchies(String name, int halfMade, int unfinished, Dish d, Object[] prepare, Object[] garnish)
		{
			return baked("crunchies", "crunchy tray", Ingredient.CRUNCHY_TRAY, ItemID.RAW_CRUNCHIES, "raw crunchies", ItemID.HALF_BAKED_CRUNCHY,
				"half baked crunchy", "half made crunchy (" + name + ")", halfMade, "unfinished crunchy (" + name + ")", unfinished, d, prepare, garnish);
		}

		private static List<Stage> batta(String name, int halfMade, int unfinished, Dish d, Object[] prepare, Object[] garnish)
		{
			return baked("batta", "batta tin", Ingredient.BATTA_TIN, ItemID.RAW_BATTA, "raw batta", ItemID.HALF_BAKED_BATTA,
				"half baked batta", "half made batta (" + name + ")", halfMade, "unfinished batta (" + name + ")", unfinished, d, prepare, garnish);
		}

		private static List<Stage> bowl(String name, int halfMade, int unfinished, Dish d, Object[] prepare, Object[] garnish)
		{
			return baked("gnomebowl", "gnomebowl mould", Ingredient.GNOMEBOWL_MOULD, ItemID.RAW_GNOMEBOWL, "raw gnomebowl", ItemID.HALF_BAKED_BOWL,
				"half baked bowl", "half made bowl (" + name + ")", halfMade, "unfinished bowl (" + name + ")", unfinished, d, prepare, garnish);
		}

		/**
		 * "Use a gianne dough on a batta tin to make a raw batta"; "Cook the raw batta on a range to
		 * make half baked batta"; "Prepare a half made batta (...) with ..."; "Cook the half made batta
		 * (...) on a range to make unfinished batta (...)"; "Use ... on the unfinished batta (...) to finish".
		 */
		private static List<Stage> baked(String kind, String toolName, Ingredient tool, int raw, String rawName, int halfBaked, String halfBakedName,
			String halfMadeName, int halfMade, String unfinishedName, int unfinished, Dish d, Object[] prepare, Object[] garnish)
		{
			List<Stage> s = new ArrayList<>();
			s.add(new Stage("Dough", "Use gianne dough on a " + toolName, raw, false, Ingredient.GIANNE_DOUGH, tool));
			s.add(new Stage("Bake", "Cook the " + rawName + " on a range", halfBaked, true));
			Stage prep = new Stage("Prepare", "", halfMade, false, prepare);
			s.add(new Stage("Prepare", "Prepare the " + halfBakedName + " with " + prep.takes(), halfMade, false, prepare));
			if (unfinished < 0)
			{
				s.add(new Stage("Bake", "Cook the " + halfMadeName + " on a range to finish the " + d.title.toLowerCase(Locale.ROOT), d.item, true));
				return s;
			}
			s.add(new Stage("Bake", "Cook the " + halfMadeName + " on a range", unfinished, true));
			Stage g = new Stage("Garnish", "", d.item, false, garnish);
			s.add(new Stage("Garnish", "Add " + g.takes() + " to finish the " + d.title.toLowerCase(Locale.ROOT), d.item, false, garnish));
			return s;
		}

		/** "Mix-cocktail" on a cocktail shaker, then "Pour" with the garnish and a cocktail glass in your inventory. */
		private static List<Stage> poured(String mixed, int shaker, Dish d, Object[] mix, Object[] garnish)
		{
			List<Stage> s = new ArrayList<>();
			Object[] withShaker = Arrays.copyOf(mix, mix.length + 1);
			withShaker[mix.length] = Ingredient.COCKTAIL_SHAKER;
			Stage m = new Stage("Mix", "", shaker, false, mix);
			s.add(new Stage("Mix", "Mix " + m.takes() + " in a cocktail shaker", shaker, false, withShaker));
			Object[] withGlass = Arrays.copyOf(garnish, garnish.length + 1);
			withGlass[garnish.length] = Ingredient.COCKTAIL_GLASS;
			Stage p = new Stage("Pour", "", d.item, false, garnish);
			s.add(new Stage("Pour", "Pour the mixed " + mixed + " into a cocktail glass with " + p.takes(), d.item, false, withGlass));
			return s;
		}
	}

	private GnomeRestaurant()
	{
	}

	/** "a, b and c". */
	static String join(List<String> parts)
	{
		if (parts.isEmpty())
		{
			return "";
		}
		if (parts.size() == 1)
		{
			return parts.get(0);
		}
		return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.get(parts.size() - 1);
	}

	/** A hand-over message read: the dish and customer it names (either may be null when not known). */
	public static final class HandOver
	{
		public final Dish dish;
		public final Customer customer;
		public final String dishText;
		public final String customerText;

		HandOver(String dishText, String customerText)
		{
			this.dishText = dishText;
			this.customerText = customerText;
			this.dish = Dish.named(dishText);
			this.customer = Customer.named(customerText);
		}
	}

	/** "You hand over your delivery of [a/some] [item] to [customer]", or null. */
	public static HandOver handOver(String message)
	{
		if (message == null || !message.startsWith(HAND_OVER))
		{
			return null;
		}
		Matcher m = HAND_OVER_LINE.matcher(message.trim());
		return m.matches() ? new HandOver(m.group(1), m.group(2)) : null;
	}

	/** "You are given [a/some] [reward item(s)] as a tip.": what was given, or null. */
	public static String tip(String message)
	{
		if (message == null)
		{
			return null;
		}
		Matcher m = TIP_LINE.matcher(message.trim());
		return m.matches() ? m.group(1) : null;
	}

	/** "You have [number] credits towards your free delivery token.": the credits, or -1. */
	public static int credits(String message)
	{
		if (message == null)
		{
			return -1;
		}
		Matcher m = CREDITS_LINE.matcher(message.trim());
		return m.find() ? Integer.parseInt(m.group(1)) : -1;
	}

	/** Free foods the token holds at so many credits: one for every 12, ten at most. */
	public static int foods(int credits)
	{
		return Math.max(0, Math.min(MAX_FOODS, credits / CREDITS_PER_FOOD));
	}

	/** Credits to the next free food, or 0 once the token is full. */
	public static int toNextFood(int credits)
	{
		int c = Math.max(0, credits);
		return foods(c) >= MAX_FOODS ? 0 : CREDITS_PER_FOOD - c % CREDITS_PER_FOOD;
	}

	/** "3:05" for a count of milliseconds, never negative. */
	public static String clock(long ms)
	{
		long s = Math.max(0, (ms + 999) / 1000);
		return s / 60 + ":" + String.format(Locale.ROOT, "%02d", s % 60);
	}
}
