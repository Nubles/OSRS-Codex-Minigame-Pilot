package com.codex.minigamepilot.core;

import java.util.Objects;

/**
 * The one thing to do next: a short verb for the game view, a sentence for the panel, a line of
 * why, and what in the world to light up. Planners hand these out; the plugin finds the target.
 */
public final class Step
{
	/** What in the world the step is about. The plugin turns this into an object, a character or an item. */
	public enum Target
	{
		NONE,
		// Motherlode Mine
		VEIN,
		HOPPER,
		SACK,
		BANK,
		STRUT,
		ROCKFALL,
		LADDER,
		// Blast Furnace
		BELT,
		DISPENSER,
		COFFER,
		/** An item in the inventory, worn, or in the open bank. */
		ITEM,
		// Mahogany Homes
		CONTRACTOR,
		CLIENT,
		HOTSPOT,
		HOME,
		// Guardians of the Rift
		/** Guardian parts or remains, to mine fragments from. */
		REMAINS,
		WORKBENCH,
		/** The uncharged cells table. */
		CELLS,
		/** A portal guardian: the way into its altar. */
		GUARDIAN,
		/** The runic altar itself, once inside. */
		ALTAR,
		/** The altar's portal back to the temple. */
		EXIT,
		GREAT_GUARDIAN,
		CELL_TILE,
		DEPOSIT_POOL,
		/** The portal to the huge guardian remains, or back from them. */
		RIFT_PORTAL,
		HUGE_REMAINS,
		// Tempoross
		/** A harpoonfish spot, the double one first when it is up. */
		FISHING_SPOT,
		/** The shrine on the island, where harpoonfish are cooked. */
		SHRINE,
		/** An ammunition crate by a cannon. */
		AMMO_CRATE,
		/** A totem pole or mast to tether to before a wave. */
		TETHER,
		FIRE,
		SPIRIT_POOL,
		/** A crate of harpoons, ropes, buckets or hammers. */
		SUPPLY_CRATE,
		WATER_PUMP,
		// Giants' Foundry
		KOVAC,
		MOULD_JIG,
		CRUCIBLE,
		/** The foundry's bank chest. */
		BANK_CHEST,
		TRIP_HAMMER,
		GRINDSTONE,
		POLISHING_WHEEL,
		LAVA_POOL,
		WATERFALL,
		PREFORM_STORAGE,
		// Mastering Mixology (the paste hopper is HOPPER, the lab's bank chest BANK)
		/** One of the mox, aga and lye levers, as the step's title names it. */
		LEVER,
		MIXING_VESSEL,
		AGITATOR,
		RETORT,
		ALEMBIC,
		CONVEYOR,
		REFINER,
		DIGWEED,
		// Vale Totems (the Auburnvale bank is BANK)
		/** A totem site: the totem to build, carve or decorate there. */
		TOTEM,
		/** A site's offerings, to claim. */
		OFFERINGS
	}

	private final String verb;
	private final String title;
	private final String detail;
	private final Target target;
	private final int[] items;
	private final boolean waiting;

	private Step(String verb, String title, String detail, Target target, int[] items, boolean waiting)
	{
		this.verb = verb;
		this.title = title;
		this.detail = detail;
		this.target = target;
		this.items = items == null ? new int[0] : items;
		this.waiting = waiting;
	}

	public static Step of(String verb, String title, String detail, Target target)
	{
		return new Step(verb, title, detail, target, null, false);
	}

	/** A step about items: withdraw them, equip them, use them. */
	public static Step items(String verb, String title, String detail, int... items)
	{
		return new Step(verb, title, detail, Target.ITEM, items, false);
	}

	/** Nothing to click: the game is doing the work. The target is where to wait. */
	public static Step waiting(String title, String detail, Target target)
	{
		return new Step("Wait", title, detail, target, null, true);
	}

	/** The same step with a different line of why. */
	public Step withDetail(String newDetail)
	{
		return new Step(verb, title, newDetail, target, items, waiting);
	}

	/** The same step with items to light up too, such as the teleport to use on the way. */
	public Step withItems(int... newItems)
	{
		return new Step(verb, title, detail, target, newItems, waiting);
	}

	/** The same step about a thing in the world too, such as items to put in it. */
	public Step withTarget(Target newTarget)
	{
		return new Step(verb, title, detail, newTarget, items, waiting);
	}

	/** A short word for the label over the target, such as "Deposit". */
	public String getVerb()
	{
		return verb;
	}

	/** The sentence the panel and plaque lead with. */
	public String getTitle()
	{
		return title;
	}

	/** Why, or how much: a quieter line under the title. May be empty. */
	public String getDetail()
	{
		return detail == null ? "" : detail;
	}

	public Target getTarget()
	{
		return target;
	}

	public int[] getItems()
	{
		return items.clone();
	}

	public boolean hasItem(int id)
	{
		for (int i : items)
		{
			if (i == id)
			{
				return true;
			}
		}
		return false;
	}

	public boolean isWaiting()
	{
		return waiting;
	}

	/** Two steps are the same instruction when they ask for the same action on the same thing. */
	public boolean sameAs(Step other)
	{
		return other != null && target == other.target && Objects.equals(title, other.title);
	}

	@Override
	public String toString()
	{
		return verb + ": " + title + (detail == null || detail.isEmpty() ? "" : " (" + detail + ")");
	}
}
