package com.codex.minigamepilot.goal;

import com.codex.minigamepilot.core.Contract;
import com.codex.minigamepilot.core.Goal;
import com.codex.minigamepilot.core.Goals;
import com.codex.minigamepilot.core.ItemIds;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;

/**
 * Keeps the goals planner's books: the nuggets and reward pieces in your bank (seen whenever it
 * is open, remembered per account), your Mahogany Homes points (read from the message after each
 * contract), and how fast you earn nuggets, points and experience in each minigame. From those
 * it adds a goal card to the panel. It only reads the game, and stays out of the guidance and
 * the field notes.
 */
public final class GoalWatch
{
	static final String BANK_KEY = "goal.bank";
	static final String POINTS_KEY = "goal.points";
	private static final Pattern POINTS = Pattern.compile("You have completed [\\d,]+ contracts? with a total of ([\\d,]+) points?");
	/** Time in a minigame before an hourly rate is trusted. */
	private static final int RATE_TICKS = 300;
	private static final double TICK_HOURS = 0.6 / 3600;

	/** The settings that decide what each minigame works toward. */
	public static final class Choices
	{
		final Goals.Choice motherlode;
		final Goals.Choice homes;
		/** The Smithing level to reach, 0 for the next one, or -1 for no goal. */
		final int furnaceLevel;

		/** @param furnaceLevel the Smithing level to reach, 0 for the next one, -1 for none */
		public Choices(Goals.Choice motherlode, Goals.Choice homes, int furnaceLevel)
		{
			this.motherlode = motherlode;
			this.homes = homes;
			this.furnaceLevel = furnaceLevel;
		}
	}

	/** Earnings in one minigame this session. */
	static final class Rate
	{
		int ticks;
		long xp;
		long currency;

		double perHour(long gained, int minimum)
		{
			return ticks >= RATE_TICKS && gained >= minimum ? gained / (ticks * TICK_HOURS) : 0;
		}
	}

	private static final int[] TRACKED = tracked();

	private final Function<String, String> load;
	private final BiConsumer<String, String> save;
	private final Map<Integer, Integer> bank = new HashMap<>();
	private final Map<View.Activity, Rate> rates = new EnumMap<>(View.Activity.class);
	private long points = -1;
	private View.Activity last = View.Activity.NONE;
	private long lastXp = -1;
	private int lastNuggets = -1;

	/**
	 * @param load reads a key of this account's saved settings
	 * @param save writes one
	 */
	public GoalWatch(Function<String, String> load, BiConsumer<String, String> save)
	{
		this.load = load;
		this.save = save;
	}

	private static int[] tracked()
	{
		List<Integer> ids = new ArrayList<>();
		ids.add(ItemIds.GOLDEN_NUGGET);
		for (Goal.Reward r : Goal.Reward.values())
		{
			for (int id : r.getItems())
			{
				ids.add(id);
			}
		}
		int[] out = new int[ids.size()];
		for (int i = 0; i < out.length; i++)
		{
			out[i] = ids.get(i);
		}
		return out;
	}

	/** After logging in or switching account: that account's bank and points, and a fresh session. */
	public void reload()
	{
		bank.clear();
		bank.putAll(decode(load.apply(BANK_KEY)));
		points = parse(load.apply(POINTS_KEY));
		rates.clear();
		last = View.Activity.NONE;
		lastXp = -1;
		lastNuggets = -1;
	}

	/** The bank changed while open: remember what it holds of the goal items. */
	public void bank(ItemContainer container)
	{
		if (container == null)
		{
			return;
		}
		Map<Integer, Integer> now = new HashMap<>();
		for (Item item : container.getItems())
		{
			if (isTracked(item.getId()))
			{
				now.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
		}
		if (!now.equals(bank))
		{
			bank.clear();
			bank.putAll(now);
			save.accept(BANK_KEY, encode(bank));
		}
	}

	/** A game message: the line after a finished contract gives your points. */
	public void chat(String message)
	{
		long read = readPoints(message);
		if (read < 0)
		{
			return;
		}
		if (points >= 0 && read > points)
		{
			rate(View.Activity.HOMES).currency += read - points;
		}
		points = read;
		save.accept(POINTS_KEY, String.valueOf(points));
	}

	/** Your points from the message after a contract, or -1 when the message is not that one. */
	static long readPoints(String message)
	{
		Matcher m = message == null ? null : POINTS.matcher(message);
		if (m == null || !m.find())
		{
			return -1;
		}
		return parse(m.group(1).replace(",", ""));
	}

	/**
	 * One tick: counts time, experience and nuggets earned in the minigame you are in, then adds
	 * goal cards to the view: the minigame's own when in one, every minigame's away from them.
	 */
	public void tick(Client client, View view, Choices choices, Contract contract)
	{
		View.Activity activity = view.getActivity();
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		int nuggets = inv == null ? 0 : inv.count(ItemIds.GOLDEN_NUGGET);
		Skill skill = skillOf(activity);
		long xp = skill == null ? -1 : client.getSkillExperience(skill);
		if (activity != View.Activity.NONE)
		{
			Rate r = rate(activity);
			r.ticks++;
			if (activity == last && lastXp >= 0 && xp > lastXp)
			{
				r.xp += xp - lastXp;
			}
			// Nuggets taken out of the bank are not nuggets earned.
			if (activity == View.Activity.MOTHERLODE && last == activity && lastNuggets >= 0 && nuggets > lastNuggets
				&& !com.codex.minigamepilot.pilot.FurnacePilot.bankOpen(client))
			{
				r.currency += nuggets - lastNuggets;
			}
		}
		last = activity;
		lastXp = xp;
		lastNuggets = nuggets;

		Have have = new Have(inv, worn, bank);
		if (activity == View.Activity.NONE)
		{
			for (View.Activity a : new View.Activity[]{View.Activity.MOTHERLODE, View.Activity.FURNACE, View.Activity.HOMES})
			{
				addCard(client, view, a, choices, contract, have, nuggets);
			}
		}
		else
		{
			addCard(client, view, activity, choices, contract, have, nuggets);
		}
	}

	private void addCard(Client client, View view, View.Activity activity, Choices choices, Contract contract, Have have, int nuggets)
	{
		Goal goal = plan(client, activity, choices, contract, have, nuggets);
		if (goal != null)
		{
			view.goal(card(activity, goal, balanceKnown(activity)));
		}
	}

	private boolean balanceKnown(View.Activity activity)
	{
		return activity != View.Activity.HOMES || points >= 0;
	}

	Goal plan(Client client, View.Activity activity, Choices choices, Contract contract, Have have, int nuggets)
	{
		Skill skill = skillOf(activity);
		Goals.Input in = new Goals.Input();
		in.have = have::has;
		in.skill = skill == null ? null : skill.getName();
		in.xp = skill == null ? 0 : client.getSkillExperience(skill);
		Rate r = rate(activity);
		in.xpPerHour = r.perHour(r.xp, 1);
		switch (activity)
		{
			case MOTHERLODE:
				if (client.getVarbitValue(VarbitID.MOTHERLODE_BIGGERSACK) > 0)
				{
					in.ownedOtherwise = EnumSet.of(Goal.Reward.BIGGER_SACK);
				}
				in.balance = nuggets + bank.getOrDefault(ItemIds.GOLDEN_NUGGET, 0);
				in.currencyPerHour = r.perHour(r.currency, 2);
				return Goals.motherlode(choices.motherlode, in);
			case HOMES:
				in.balance = points;
				in.currencyPerHour = r.perHour(r.currency, 2);
				Contract.Tier tier = contract != null && contract.getTier() != null ? contract.getTier()
					: Contract.Tier.forLevel(client.getRealSkillLevel(Skill.CONSTRUCTION));
				in.currencyPerRound = tier.points();
				in.roundName = tier.getName().toLowerCase() + " contract";
				return Goals.homes(choices.homes, in);
			case FURNACE:
				if (choices.furnaceLevel < 0)
				{
					return null;
				}
				in.targetLevel = choices.furnaceLevel;
				return Goals.furnace(in);
			default:
				return null;
		}
	}

	private View.GoalCard card(View.Activity activity, Goal goal, boolean balanceKnown)
	{
		List<View.Line> lines = new ArrayList<>();
		long left = goal.getHave();
		for (Goal.Piece p : goal.getPieces())
		{
			Goal.Reward reward = p.getReward();
			if (p.isOwned())
			{
				lines.add(new View.Line(reward.getName(), "owned", View.Line.Mark.DONE));
				continue;
			}
			// Lit when what you hold already pays for it, taking the pieces above first.
			boolean affordable = balanceKnown && left >= reward.getCost();
			if (affordable)
			{
				left -= reward.getCost();
			}
			lines.add(new View.Line(reward.getName(), String.valueOf(reward.getCost()),
				affordable ? View.Line.Mark.NEXT : View.Line.Mark.TODO));
		}
		String note = balanceKnown ? goal.describe()
			: "Finish a contract and the Pilot reads your points from the message. " + goal.getNeed() + " points for all of it";
		return new View.GoalCard(activity, goal.getTitle(), balanceKnown ? goal.getHave() : 0, goal.getNeed(), goal.getUnit(), note, lines,
			balanceKnown && goal.done());
	}

	private Rate rate(View.Activity activity)
	{
		return rates.computeIfAbsent(activity, a -> new Rate());
	}

	static Skill skillOf(View.Activity activity)
	{
		switch (activity)
		{
			case MOTHERLODE:
				return Skill.MINING;
			case FURNACE:
				return Skill.SMITHING;
			case HOMES:
				return Skill.CONSTRUCTION;
			default:
				return null;
		}
	}

	static boolean isTracked(int id)
	{
		for (int t : TRACKED)
		{
			if (t == id)
			{
				return true;
			}
		}
		return false;
	}

	/** Whether an item is carried, worn or in the bank as last seen. */
	static final class Have
	{
		private final ItemContainer inv;
		private final ItemContainer worn;
		private final Map<Integer, Integer> bank;

		Have(ItemContainer inv, ItemContainer worn, Map<Integer, Integer> bank)
		{
			this.inv = inv;
			this.worn = worn;
			this.bank = bank;
		}

		boolean has(int id)
		{
			return inv != null && inv.contains(id) || worn != null && worn.contains(id) || bank.getOrDefault(id, 0) > 0;
		}
	}

	static String encode(Map<Integer, Integer> counts)
	{
		StringBuilder b = new StringBuilder();
		counts.keySet().stream().sorted().forEach(id ->
		{
			if (b.length() > 0)
			{
				b.append(';');
			}
			b.append(id).append('=').append(counts.get(id));
		});
		return b.toString();
	}

	static Map<Integer, Integer> decode(String saved)
	{
		Map<Integer, Integer> map = new HashMap<>();
		if (saved == null || saved.isEmpty())
		{
			return map;
		}
		for (String part : saved.split(";"))
		{
			int eq = part.indexOf('=');
			if (eq <= 0)
			{
				continue;
			}
			try
			{
				int id = Integer.parseInt(part.substring(0, eq));
				int n = Integer.parseInt(part.substring(eq + 1));
				if (n > 0 && isTracked(id))
				{
					map.put(id, n);
				}
			}
			catch (NumberFormatException ignored)
			{
				// A damaged entry is dropped; the rest still count.
			}
		}
		return map;
	}

	private static long parse(String s)
	{
		if (s == null || s.isEmpty())
		{
			return -1;
		}
		try
		{
			return Long.parseLong(s.trim());
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}
}
