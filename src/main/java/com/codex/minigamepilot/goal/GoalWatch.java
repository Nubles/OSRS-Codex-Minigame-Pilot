package com.codex.minigamepilot.goal;

import com.codex.minigamepilot.core.Contract;
import com.codex.minigamepilot.core.Goal;
import com.codex.minigamepilot.core.Goals;
import com.codex.minigamepilot.core.ItemIds;
import com.codex.minigamepilot.core.NavigatorGoals;
import com.codex.minigamepilot.core.Owned;
import com.codex.minigamepilot.core.Points;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

/**
 * Keeps the goals planner's books: the nuggets and reward pieces in your bank (seen whenever it
 * is open, remembered per account), your Mahogany Homes points (read from the message after each
 * contract), and how fast you earn nuggets, points and experience in each minigame. From those
 * it adds a goal card to the panel. When Codex Navigator is installed, a level goal follows the
 * level your pinned goals need. It only reads the game, and stays out of the guidance and the
 * field notes.
 */
public final class GoalWatch
{
	static final String BANK_KEY = "goal.bank";
	/** The last points total read, as a plain number, as older versions saved it. */
	static final String POINTS_KEY = "goal.points";
	/** The points book: total, contracts, what the total means and points spent since. */
	static final String POINTS_BOOK_KEY = "goal.pointsBook";
	/** The rewards you have been seen to own, so one put away in the costume room still counts. */
	static final String OWNED_KEY = "goal.owned";
	private static final Pattern POINTS = Pattern.compile("You have completed ([\\d,]+) contracts? with a total of ([\\d,]+) points?");
	/** Time in a minigame before an hourly rate is trusted. */
	private static final int RATE_TICKS = 300;
	private static final double TICK_HOURS = 0.6 / 3600;
	/** Ticks after the reward shop closes in which a reward turning up still counts as bought. */
	static final int SHOP_GRACE = 3;

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
	private final Supplier<NavigatorGoals.Board> navigator;
	private Points points = new Points();
	/** Points rewards carried or worn on the last tick, or null when not seen yet. */
	private EnumSet<Goal.Reward> carried;
	/** What the points rewards you own cost, as of the last tick. */
	private long ownedCost;
	private boolean storageWasOpen;
	private Owned owned = new Owned();
	/** Every reward carried or worn on the last tick, or null when not seen yet. */
	private EnumSet<Goal.Reward> heldBefore;
	private boolean groupWasOpen;
	/** The game tick the contract reward shop was last seen open, or -1. */
	private int shopTick = -1;
	/** The game tick the books were last kept on, or -1. */
	private int lastTick = -1;
	private View.Activity last = View.Activity.NONE;
	private long lastXp = -1;
	private int lastNuggets = -1;

	/**
	 * @param load reads a key of this account's saved settings
	 * @param save writes one
	 * @param navigator Codex Navigator's goal board, or null when it is not installed
	 */
	public GoalWatch(Function<String, String> load, BiConsumer<String, String> save, Supplier<NavigatorGoals.Board> navigator)
	{
		this.load = load;
		this.save = save;
		this.navigator = navigator;
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
		points = Points.decode(load.apply(POINTS_BOOK_KEY));
		if (!points.known())
		{
			// Saved by an older version: just the total, nothing known about what it means.
			points = Points.decode(load.apply(POINTS_KEY));
		}
		carried = null;
		ownedCost = 0;
		storageWasOpen = false;
		shopTick = -1;
		owned = Owned.decode(load.apply(OWNED_KEY));
		heldBefore = null;
		groupWasOpen = false;
		lastTick = -1;
		rates.clear();
		last = View.Activity.NONE;
		lastXp = -1;
		lastNuggets = -1;
	}

	/**
	 * Logging in or hopping: experience and the inventory arrive over the first ticks, so what
	 * was seen before is no base for what is gained next.
	 */
	public void relog()
	{
		last = View.Activity.NONE;
		lastXp = -1;
		lastNuggets = -1;
		carried = null;
		heldBefore = null;
		lastTick = -1;
	}

	/** A skill's experience, or -1 while the skills have not arrived (no Hitpoints experience yet). */
	static long known(long xp, long hitpointsXp)
	{
		return hitpointsXp > 0 ? xp : -1;
	}

	/** Experience gained since the last tick, 0 when either reading is not known. */
	static long xpGained(long before, long now)
	{
		return before >= 0 && now > before ? now - before : 0;
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

	/**
	 * Nuggets earned between two ticks, from what you hold carried and banked together. Taking
	 * nuggets out of the bank leaves that total as it was, even when the bank is closed on the
	 * same tick, so it never counts as earning. While the bank is open nothing counts, so the
	 * first look at a bank that changed since it was last seen is not taken for a haul.
	 *
	 * @param before carried plus banked nuggets on the last tick, or -1 when not known
	 */
	static int nuggetsEarned(int before, int now, boolean bankOpen)
	{
		return before >= 0 && now > before && !bankOpen ? now - before : 0;
	}

	/** A game message: the line after a finished contract gives your points. */
	public void chat(String message)
	{
		long[] read = readPoints(message);
		if (read == null)
		{
			return;
		}
		long earned = points.message(read[0], read[1], ownedCost);
		if (earned > 0)
		{
			rate(View.Activity.HOMES).currency += earned;
		}
		savePoints();
	}

	private void savePoints()
	{
		save.accept(POINTS_BOOK_KEY, points.encode());
		save.accept(POINTS_KEY, String.valueOf(points.getTotal()));
	}

	/**
	 * The contracts and points from the message after a contract, or null when the message is not
	 * that one.
	 */
	static long[] readPoints(String message)
	{
		Matcher m = message == null ? null : POINTS.matcher(message);
		if (m == null || !m.find())
		{
			return null;
		}
		long count = parse(m.group(1).replace(",", ""));
		long total = parse(m.group(2).replace(",", ""));
		return count < 0 || total < 0 ? null : new long[]{count, total};
	}

	/**
	 * The points rewards that turned up carried since the last tick, at the reward shop, from
	 * nowhere you had them: not carried or worn a tick ago and not in the bank as last seen.
	 * Those were just bought. Anything that turns up away from the shop, or while a bank, group
	 * storage or your house's costume room is open, was only taken out of storage, even when the
	 * Pilot never saw it go in.
	 *
	 * @param before points rewards carried or worn on the last tick, or null when not seen yet
	 * @param storageOpen a bank, group storage or costume room open on this tick or the last
	 * @param atShop the contract reward shop open on this tick or one of the last few
	 */
	static List<Goal.Reward> bought(EnumSet<Goal.Reward> before, EnumSet<Goal.Reward> now, EnumSet<Goal.Reward> inInventory,
		Map<Integer, Integer> bank, boolean storageOpen, boolean atShop)
	{
		List<Goal.Reward> out = new ArrayList<>();
		if (before == null || storageOpen || !atShop)
		{
			return out;
		}
		for (Goal.Reward r : inInventory)
		{
			if (!before.contains(r) && now.contains(r) && !r.ownedBy(id -> bank.getOrDefault(id, 0) > 0))
			{
				out.add(r);
			}
		}
		return out;
	}

	/**
	 * Whether the reward shop was open on this tick or one of the last few. A shop tick from
	 * before the tick count started over (logging in again) is not recent.
	 */
	static boolean atShop(int shopTick, int tickCount)
	{
		return shopTick >= 0 && tickCount >= shopTick && tickCount - shopTick <= SHOP_GRACE;
	}

	/** Whether an interface component is open and showing. */
	private static boolean showing(Client client, int component)
	{
		Widget w = client.getWidget(component);
		return w != null && !w.isHidden();
	}

	/** A bank, group storage or the costume room in your house: places a reward can come out of. */
	static boolean storageOpen(Client client)
	{
		return com.codex.minigamepilot.pilot.FurnacePilot.bankOpen(client)
			|| showing(client, InterfaceID.SharedBank.ITEMS)
			|| showing(client, InterfaceID.PohCostumes.ITEMS);
	}

	/** The points rewards among what a container holds. */
	private static EnumSet<Goal.Reward> pointsRewards(ItemContainer c)
	{
		EnumSet<Goal.Reward> set = EnumSet.noneOf(Goal.Reward.class);
		if (c == null)
		{
			return set;
		}
		for (Goal.Reward r : Goal.Reward.values())
		{
			if (r.getCurrency() == Goal.Currency.POINTS && r.ownedBy(c::contains))
			{
				set.add(r);
			}
		}
		return set;
	}

	/** What the points rewards you have anywhere cost: all of them were bought with points. */
	static long ownedCost(Have have)
	{
		long cost = 0;
		for (Goal.Reward r : Goal.Reward.values())
		{
			if (r.getCurrency() == Goal.Currency.POINTS && r.ownedBy(have::has))
			{
				cost += r.getCost();
			}
		}
		return cost;
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
		// Carried plus banked as last seen: moving nuggets between the two changes neither. With
		// no inventory yet (just logged in) nothing is known, so the next tick is no gain on it.
		int heldNuggets = inv == null ? -1 : nuggets + bank.getOrDefault(ItemIds.GOLDEN_NUGGET, 0);
		Skill skill = skillOf(activity);
		long xp = skill == null ? -1 : known(client.getSkillExperience(skill), client.getSkillExperience(Skill.HITPOINTS));
		if (activity != View.Activity.NONE)
		{
			Rate r = rate(activity);
			r.ticks++;
			if (activity == last)
			{
				r.xp += xpGained(lastXp, xp);
			}
			if (activity == View.Activity.MOTHERLODE && last == activity)
			{
				r.currency += nuggetsEarned(lastNuggets, heldNuggets, com.codex.minigamepilot.pilot.FurnacePilot.bankOpen(client));
			}
		}
		last = activity;
		lastXp = xp;
		lastNuggets = heldNuggets;

		int tickCount = client.getTickCount();
		if (tickCount != lastTick + 1)
		{
			// Ticks went by unwatched (goals hidden, say): what was carried then tells nothing now.
			carried = null;
			storageWasOpen = false;
			shopTick = -1;
			heldBefore = null;
			groupWasOpen = false;
		}
		lastTick = tickCount;
		if (inv != null)
		{
			EnumSet<Goal.Reward> inInventory = pointsRewards(inv);
			EnumSet<Goal.Reward> now = EnumSet.copyOf(inInventory);
			now.addAll(pointsRewards(worn));
			boolean storage = storageOpen(client);
			if (showing(client, InterfaceID.ConstructionContractShop.UNIVERSE))
			{
				shopTick = tickCount;
			}
			boolean atShop = atShop(shopTick, tickCount);
			// Taking a reward out and closing the storage on one tick shows it carried with the
			// storage shut, and the bank as last seen already without it: the open storage a tick
			// ago tells.
			List<Goal.Reward> bought = bought(carried, now, inInventory, bank, storage || storageWasOpen, atShop);
			// One you were already seen to own came from somewhere you keep it, not the shop.
			bought.removeIf(owned::has);
			storageWasOpen = storage;
			remember(inv, worn, client);
			for (Goal.Reward r : bought)
			{
				points.bought(r.getCost());
			}
			if (!bought.isEmpty() && points.known())
			{
				savePoints();
			}
			carried = now;
		}

		Have have = new Have(inv, worn, bank, owned);
		ownedCost = ownedCost(have);
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
		NavigatorGoals.Target target = navigatorTarget(client, activity, choices);
		Goal goal = plan(client, activity, choices, contract, have, nuggets, target);
		if (goal != null)
		{
			View.GoalCard card = card(activity, goal, balanceKnown(activity));
			if (target != null && goal.getPieces().isEmpty())
			{
				card.aside(aside(target, skillOf(activity)));
			}
			view.goal(card);
		}
	}

	/**
	 * The level Navigator's goals need in this minigame's skill, when the level goal is left to
	 * the Pilot to pick: the next level at the Blast Furnace, or next level or next reward
	 * elsewhere (which turns to a level once every reward is owned).
	 */
	private NavigatorGoals.Target navigatorTarget(Client client, View.Activity activity, Choices choices)
	{
		Skill skill = skillOf(activity);
		if (navigator == null || skill == null)
		{
			return null;
		}
		boolean pilotPicks;
		switch (activity)
		{
			case FURNACE:
				pilotPicks = choices.furnaceLevel == 0;
				break;
			case MOTHERLODE:
				pilotPicks = picksLevel(choices.motherlode);
				break;
			case HOMES:
				pilotPicks = picksLevel(choices.homes);
				break;
			default:
				pilotPicks = false;
		}
		if (!pilotPicks)
		{
			return null;
		}
		return NavigatorGoals.target(navigator.get(), skill.getName(), Goals.levelFor(client.getSkillExperience(skill)));
	}

	private static boolean picksLevel(Goals.Choice choice)
	{
		return choice == Goals.Choice.NEXT_LEVEL || choice == Goals.Choice.NEXT_REWARD;
	}

	/** "Dragon Slayer II needs Smithing 70", from your Navigator goals. */
	static String aside(NavigatorGoals.Target target, Skill skill)
	{
		return target.getGoal() + " needs " + (skill == null ? "level" : skill.getName()) + " " + target.getLevel();
	}

	private boolean balanceKnown(View.Activity activity)
	{
		return activity != View.Activity.HOMES || points.known();
	}

	Goal plan(Client client, View.Activity activity, Choices choices, Contract contract, Have have, int nuggets, NavigatorGoals.Target target)
	{
		Skill skill = skillOf(activity);
		Goals.Input in = new Goals.Input();
		in.have = have::has;
		in.skill = skill == null ? null : skill.getName();
		in.xp = skill == null ? 0 : client.getSkillExperience(skill);
		Rate r = rate(activity);
		in.xpPerHour = r.perHour(r.xp, 1);
		if (target != null)
		{
			in.targetLevel = target.getLevel();
		}
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
				in.balance = points.balance(ownedCost);
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
				if (choices.furnaceLevel > 0)
				{
					in.targetLevel = choices.furnaceLevel;
				}
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
		if (activity == View.Activity.HOMES && balanceKnown && !goal.getPieces().isEmpty() && points.getMeaning() == Points.Meaning.UNKNOWN && ownedCost > 0)
		{
			// Until the Pilot knows whether the message's total is a balance or every point ever
			// earned, it takes the lower reading, so a reward is never lit before you can buy it.
			note += ". Rewards you own are taken off until a purchase shows whether the total is a balance";
		}
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

	/**
	 * Remembers the rewards seen carried, worn or in the bank, so one put away in the costume
	 * room still counts as owned; one that came out of group storage is not remembered.
	 */
	private void remember(ItemContainer inv, ItemContainer worn, Client client)
	{
		EnumSet<Goal.Reward> inPack = itemRewards(inv);
		EnumSet<Goal.Reward> held = EnumSet.copyOf(inPack);
		held.addAll(itemRewards(worn));
		EnumSet<Goal.Reward> appeared = EnumSet.noneOf(Goal.Reward.class);
		if (heldBefore != null)
		{
			appeared.addAll(inPack);
			appeared.removeAll(heldBefore);
		}
		EnumSet<Goal.Reward> seen = EnumSet.copyOf(held);
		for (Goal.Reward r : Goal.Reward.values())
		{
			if (r.ownedBy(id -> bank.getOrDefault(id, 0) > 0))
			{
				seen.add(r);
			}
		}
		boolean group = showing(client, InterfaceID.SharedBank.ITEMS);
		if (owned.see(seen, appeared, group || groupWasOpen))
		{
			save.accept(OWNED_KEY, owned.encode());
		}
		groupWasOpen = group;
		heldBefore = held;
	}

	/** Every reward with an item among what a container holds. */
	private static EnumSet<Goal.Reward> itemRewards(ItemContainer c)
	{
		EnumSet<Goal.Reward> set = EnumSet.noneOf(Goal.Reward.class);
		if (c == null)
		{
			return set;
		}
		for (Goal.Reward r : Goal.Reward.values())
		{
			if (r.ownedBy(c::contains))
			{
				set.add(r);
			}
		}
		return set;
	}

	/** Whether an item is carried, worn, in the bank as last seen, or a reward you were seen to own. */
	static final class Have
	{
		private final ItemContainer inv;
		private final ItemContainer worn;
		private final Map<Integer, Integer> bank;
		private final Owned owned;

		Have(ItemContainer inv, ItemContainer worn, Map<Integer, Integer> bank, Owned owned)
		{
			this.inv = inv;
			this.worn = worn;
			this.bank = bank;
			this.owned = owned;
		}

		boolean has(int id)
		{
			return inv != null && inv.contains(id) || worn != null && worn.contains(id) || bank.getOrDefault(id, 0) > 0
				|| owned != null && owned.hasItem(id);
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
