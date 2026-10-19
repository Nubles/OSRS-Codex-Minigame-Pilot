package com.codex.minigamepilot.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * The supplies the minigames burn (ore, coal, planks, steel bars) as your bank last held them,
 * and when it was seen. The Pilot keeps its own copy from every look at the bank, and also reads
 * Codex Bank Steward's shared snapshot, so a fresh install knows your stock before you open the
 * bank. The newer of the two is used.
 *
 * <p>Bank Steward saves {@code codexbanksteward/bankV1} in the RuneLite profile, read here and
 * never written:
 * {@code {"version":1,"savedAt":1760000000000,"items":{"995":1234567,"2434":30},"truncated":false}}.</p>
 */
public final class BankStock
{
	public static final String STEWARD_GROUP = "codexbanksteward";
	public static final String STEWARD_KEY = "bankV1";

	/** Where a stock came from. */
	public enum Source
	{
		/** The Pilot's own look at the bank. */
		PILOT,
		/** Codex Bank Steward's shared snapshot. */
		BANK_STEWARD
	}

	/** Every item the minigames burn: each bar's ore, coal, the four planks and steel bars. */
	private static final int[] SUPPLIES = supplies();

	private final Map<Integer, Long> items;
	private final long savedAt;
	private final Source source;

	BankStock(Map<Integer, Long> items, long savedAt, Source source)
	{
		this.items = Collections.unmodifiableMap(items);
		this.savedAt = savedAt;
		this.source = source;
	}

	private static int[] supplies()
	{
		java.util.Set<Integer> ids = new java.util.TreeSet<>();
		for (Bar b : Bar.values())
		{
			ids.add(b.getOre());
		}
		ids.add(Bar.COAL);
		for (Contract.Tier t : Contract.Tier.values())
		{
			ids.add(t.getPlank());
		}
		ids.add(Contract.STEEL_BAR);
		int[] out = new int[ids.size()];
		int i = 0;
		for (int id : ids)
		{
			out[i++] = id;
		}
		return out;
	}

	public static boolean isSupply(int id)
	{
		for (int s : SUPPLIES)
		{
			if (s == id)
			{
				return true;
			}
		}
		return false;
	}

	/** The Pilot's own look at the bank: the supplies among what it holds. */
	public static BankStock seen(Map<Integer, Long> bank, long savedAt)
	{
		Map<Integer, Long> keep = new TreeMap<>();
		for (Map.Entry<Integer, Long> e : bank.entrySet())
		{
			if (e.getKey() != null && isSupply(e.getKey()) && e.getValue() != null && e.getValue() > 0)
			{
				keep.merge(e.getKey(), e.getValue(), Long::sum);
			}
		}
		return new BankStock(keep, savedAt, Source.PILOT);
	}

	/** Bank Steward's snapshot, or null when there is none or it cannot be read. */
	public static BankStock steward(Gson gson, String json)
	{
		if (json == null || json.trim().isEmpty())
		{
			return null;
		}
		JsonObject root;
		try
		{
			JsonElement e = gson.fromJson(json, JsonElement.class);
			root = e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
		}
		catch (JsonParseException | IllegalStateException | NumberFormatException e)
		{
			return null;
		}
		JsonElement list = root == null ? null : root.get("items");
		if (list == null || !list.isJsonObject())
		{
			return null;
		}
		Map<Integer, Long> keep = new TreeMap<>();
		for (Map.Entry<String, JsonElement> e : list.getAsJsonObject().entrySet())
		{
			Integer id = integer(e.getKey());
			Long qty = number(e.getValue());
			if (id != null && isSupply(id) && qty != null && qty > 0)
			{
				keep.put(id, qty);
			}
		}
		Long savedAt = number(root.get("savedAt"));
		return new BankStock(keep, savedAt == null ? 0 : savedAt, Source.BANK_STEWARD);
	}

	/** The newer of two stocks, either of which may be null. The Pilot's own wins a tie. */
	public static BankStock newest(BankStock own, BankStock steward)
	{
		if (own == null)
		{
			return steward;
		}
		if (steward == null)
		{
			return own;
		}
		return steward.savedAt > own.savedAt ? steward : own;
	}

	public long count(int id)
	{
		return items.getOrDefault(id, 0L);
	}

	/** When the bank was seen, in epoch milliseconds; 0 when not known. */
	public long getSavedAt()
	{
		return savedAt;
	}

	public Source getSource()
	{
		return source;
	}

	/** Saved as "savedAt|id=n;id=n". */
	public String encode()
	{
		StringBuilder b = new StringBuilder().append(savedAt).append('|');
		boolean first = true;
		for (Map.Entry<Integer, Long> e : new TreeMap<>(items).entrySet())
		{
			if (!first)
			{
				b.append(';');
			}
			first = false;
			b.append(e.getKey()).append('=').append(e.getValue());
		}
		return b.toString();
	}

	/** The Pilot's saved copy, or null when there is none; damaged entries are dropped. */
	public static BankStock decode(String saved)
	{
		if (saved == null || saved.isEmpty())
		{
			return null;
		}
		int bar = saved.indexOf('|');
		if (bar <= 0)
		{
			return null;
		}
		Long savedAt = parse(saved.substring(0, bar));
		if (savedAt == null || savedAt < 0)
		{
			return null;
		}
		Map<Integer, Long> keep = new TreeMap<>();
		String rest = saved.substring(bar + 1);
		for (String part : rest.isEmpty() ? new String[0] : rest.split(";"))
		{
			int eq = part.indexOf('=');
			Integer id = eq <= 0 ? null : integer(part.substring(0, eq));
			Long qty = eq <= 0 ? null : parse(part.substring(eq + 1));
			if (id != null && isSupply(id) && qty != null && qty > 0)
			{
				keep.put(id, qty);
			}
		}
		return new BankStock(keep, savedAt, Source.PILOT);
	}

	/** Whether two stocks hold the same supplies, whenever they were seen. */
	public boolean sameItems(BankStock other)
	{
		return other != null && items.equals(other.items);
	}

	private static Long number(JsonElement e)
	{
		if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber())
		{
			return null;
		}
		try
		{
			return e.getAsLong();
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
	}

	private static Integer integer(String s)
	{
		try
		{
			return Integer.parseInt(s.trim());
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	private static Long parse(String s)
	{
		try
		{
			return Long.parseLong(s.trim());
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}
}
