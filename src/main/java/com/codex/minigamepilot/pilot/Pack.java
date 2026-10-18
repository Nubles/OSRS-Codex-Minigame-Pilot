package com.codex.minigamepilot.pilot;

import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;

/** What the player carries and wears, read once a tick. */
final class Pack
{
	private final Item[] inventory;
	private final Item[] worn;
	private final Item[] bank;

	private Pack(Item[] inventory, Item[] worn, Item[] bank)
	{
		this.inventory = inventory;
		this.worn = worn;
		this.bank = bank;
	}

	static Pack read(Client client)
	{
		return new Pack(items(client.getItemContainer(InventoryID.INV)), items(client.getItemContainer(InventoryID.WORN)),
			items(client.getItemContainer(InventoryID.BANK)));
	}

	private static Item[] items(ItemContainer c)
	{
		return c == null ? null : c.getItems();
	}

	/** How many of these items are carried, stacks counted in full. */
	int count(int... ids)
	{
		return count(inventory, ids);
	}

	boolean carries(int... ids)
	{
		return count(ids) > 0;
	}

	boolean wears(int... ids)
	{
		return count(worn, ids) > 0;
	}

	/** How many the bank held when last open, or -1 when it has not been seen this session. */
	int banked(int... ids)
	{
		return bank == null ? -1 : count(bank, ids);
	}

	/** Empty inventory slots. */
	int free()
	{
		if (inventory == null)
		{
			return 28;
		}
		int used = 0;
		for (Item i : inventory)
		{
			if (i.getId() > 0 && i.getQuantity() > 0)
			{
				used++;
			}
		}
		return Math.max(0, 28 - used);
	}

	/** Inventory slots holding these items. */
	int slots(int... ids)
	{
		if (inventory == null)
		{
			return 0;
		}
		int n = 0;
		for (Item i : inventory)
		{
			for (int id : ids)
			{
				if (i.getId() == id && i.getQuantity() > 0)
				{
					n++;
					break;
				}
			}
		}
		return n;
	}

	private static int count(Item[] items, int... ids)
	{
		if (items == null)
		{
			return 0;
		}
		int n = 0;
		for (Item i : items)
		{
			for (int id : ids)
			{
				if (i.getId() == id)
				{
					n += i.getQuantity();
					break;
				}
			}
		}
		return n;
	}
}
