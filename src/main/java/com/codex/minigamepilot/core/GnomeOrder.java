package com.codex.minigamepilot.core;

import com.codex.minigamepilot.core.GnomeRestaurant.Customer;
import com.codex.minigamepilot.core.GnomeRestaurant.Dish;
import com.codex.minigamepilot.core.GnomeRestaurant.Ingredient;
import com.codex.minigamepilot.core.GnomeRestaurant.Stage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The one next step of a Gnome Restaurant delivery: the dish's recipe from where you stand in it
 * (the furthest stage you hold), with what you still lack for every step left, then who to take
 * it to and where, against the clock.
 *
 * <p>Plain Java over a snapshot of the game, so every turn of it can be tested. The facts are in
 * {@link GnomeRestaurant}. How they decide the step:
 * <ul>
 * <li>a dish is made in order, each stage from the one before ("Use a gianne dough on a batta tin
 * to make a raw batta", "Cook the raw batta on a range to make half baked batta", and so on);
 * holding a stage's item means every stage before it is done;</li>
 * <li>"Your Cooking level is not taken into account when getting orders": an order can ask for more
 * than your level, and the step says so rather than send you to a range that will not let you;</li>
 * <li>cut fruit comes from "using a knife on" the fruit, so a missing slice or chunk you can cut is
 * the step before a fetch;</li>
 * <li>"Easy tasks have a time limit of 6 minutes, whereas hard tasks have a limit of 11 minutes";
 * past it, "Your delivery is too late! Go back to Aluft jnr. to get another order."</li>
 * </ul>
 */
public final class GnomeOrder
{
	/** Time left is not known: the order was taken before this login and not seen being taken. */
	public static final long UNKNOWN = Long.MIN_VALUE;

	/** What the Pilot read this tick. */
	public static final class Snapshot
	{
		/** The order, or null for none (or one the Pilot does not know). */
		public Dish dish;
		public Customer customer;
		/** The game says there is an order (its varbit is set), even if the Pilot cannot name it. */
		public boolean ordered;
		/** What the pack holds, by item id. */
		public Map<Integer, Integer> held = new HashMap<>();
		/** Cooking, boosts counted ("can be boosted"). */
		public int cooking = 99;
		/** Milliseconds left, or {@link #UNKNOWN}. */
		public long msLeft = UNKNOWN;
		/** The customer is in sight. */
		public boolean customerInSight;
		/** A place to head for is known: seen there before, or on RuneLite's world map. */
		public boolean placeKnown;
		/** Codex Wayfinder's quickest way there, or null. */
		public String way;
	}

	/** The step and where the recipe stands. */
	public static final class Plan
	{
		public final Step step;
		/** The stage to do next; the stage count when the dish is made and only the delivery is left; -1 with no order. */
		public final int at;
		/** For each stage, what you lack for it once the stages before it have taken theirs. */
		public final List<Map<Ingredient, Integer>> missing;

		Plan(Step step, int at, List<Map<Ingredient, Integer>> missing)
		{
			this.step = step;
			this.at = at;
			this.missing = missing;
		}
	}

	private GnomeOrder()
	{
	}

	public static Plan next(Snapshot s)
	{
		if (s.dish == null || s.customer == null)
		{
			if (s.ordered)
			{
				return new Plan(Step.waiting("Check your delivery box", "The order is one the Pilot cannot name: the box says the food and the customer",
					Step.Target.NONE), -1, Collections.emptyList());
			}
			if (s.cooking < GnomeRestaurant.TRAINING_COOKING)
			{
				return new Plan(Step.waiting("Train Cooking to " + GnomeRestaurant.TRAINING_COOKING,
					"Aluft Gianne snr.'s training needs " + GnomeRestaurant.TRAINING_COOKING + " Cooking; you have " + s.cooking, Step.Target.NONE),
					-1, Collections.emptyList());
			}
			return new Plan(Step.of("Talk", "Ask Gianne jnr. for a delivery",
				"Easy: around the Stronghold, 6:00, 1 credit · Hard: anywhere, 11:00, 3 credits", Step.Target.GIANNE), -1, Collections.emptyList());
		}
		List<Stage> stages = s.dish.stages();
		if (s.msLeft != UNKNOWN && s.msLeft <= 0)
		{
			return new Plan(Step.of("Talk", "Back to Gianne jnr. for a new order", "Too late for " + s.customer.getTitle()
				+ ": a late delivery is turned away", Step.Target.GIANNE), -1, Collections.emptyList());
		}
		int at = reached(s);
		List<Map<Ingredient, Integer>> missing = missing(s, at);
		if (at >= stages.size())
		{
			return new Plan(deliver(s), at, missing);
		}
		if (s.cooking < s.dish.level())
		{
			return new Plan(Step.waiting(s.dish.getTitle() + " needs " + s.dish.level() + " Cooking",
				"You have " + s.cooking + ": a boost will do, or hand over one made by another player (premade ones are refused)",
				Step.Target.NONE), at, missing);
		}
		Stage stage = stages.get(at);
		Map<Ingredient, Integer> lack = missing.get(at);
		String clock = clock(s);
		if (!lack.isEmpty())
		{
			Ingredient cut = cuttable(s, lack, missing, at);
			if (cut != null)
			{
				Ingredient fruit = cut.cutFrom();
				return new Plan(Step.items("Cut", "Cut " + article(fruit.getTitle()) + " " + fruit.getTitle() + " into " + cut.cut(),
					clock + "With your knife, for: " + lower(stage.text), fruit.item(), net.runelite.api.gameval.ItemID.KNIFE), at, missing);
			}
			List<String> parts = new ArrayList<>();
			int[] ids = new int[lack.size()];
			int i = 0;
			for (Map.Entry<Ingredient, Integer> e : lack.entrySet())
			{
				parts.add(e.getKey().count(e.getValue()));
				ids[i++] = e.getKey().item();
			}
			return new Plan(Step.items("Fetch", "Fetch " + GnomeRestaurant.join(parts),
				clock + "For: " + lower(stage.text) + " · " + shop(s.dish), ids), at, missing);
		}
		int[] input = input(stages, at);
		String verb = stage.range ? "Cook" : stage.name.equals("Mix") ? "Mix" : stage.name.equals("Pour") ? "Pour" : stage.name.equals("Garnish") ? "Add"
			: stage.name.equals("Prepare") ? "Prepare" : "Use";
		int left = stages.size() - at - 1;
		String then = left == 0 ? "then to " + s.customer.getTitle() : left + (left == 1 ? " more step, " : " more steps, ") + "then to " + s.customer.getTitle();
		return new Plan(Step.items(verb, stage.text, clock + then + (stage.range ? " · the range by Hudo in the Grand Tree" : ""), input), at, missing);
	}

	/** The stage to do next: one past the furthest stage whose item you hold. */
	public static int reached(Snapshot s)
	{
		List<Stage> stages = s.dish.stages();
		for (int i = stages.size() - 1; i >= 0; i--)
		{
			if (has(s, stages.get(i).makes))
			{
				return i + 1;
			}
		}
		return 0;
	}

	/** What each stage lacks: the pack's items handed out to the stages left, in order. */
	static List<Map<Ingredient, Integer>> missing(Snapshot s, int at)
	{
		List<Stage> stages = s.dish.stages();
		Map<Integer, Integer> left = new HashMap<>(s.held);
		List<Map<Ingredient, Integer>> out = new ArrayList<>();
		for (int i = 0; i < stages.size(); i++)
		{
			Map<Ingredient, Integer> lack = new java.util.LinkedHashMap<>();
			if (i >= at)
			{
				Stage st = stages.get(i);
				for (int k = 0; k < st.uses.size(); k++)
				{
					Ingredient g = st.uses.get(k);
					int need = st.counts.get(k);
					int have = left.getOrDefault(g.item(), 0);
					int take = Math.min(have, need);
					left.put(g.item(), have - take);
					if (need > take)
					{
						lack.merge(g, need - take, Integer::sum);
					}
				}
				for (Ingredient t : st.tools)
				{
					if (s.held.getOrDefault(t.item(), 0) <= 0)
					{
						lack.put(t, 1);
					}
				}
			}
			out.add(lack);
		}
		return out;
	}

	/** A cut fruit this stage lacks that a knife and a spare fruit you hold can make, or null. */
	private static Ingredient cuttable(Snapshot s, Map<Ingredient, Integer> lack, List<Map<Ingredient, Integer>> missing, int at)
	{
		if (!has(s, net.runelite.api.gameval.ItemID.KNIFE))
		{
			return null;
		}
		for (Ingredient g : lack.keySet())
		{
			Ingredient fruit = g.cutFrom();
			if (fruit == null)
			{
				continue;
			}
			// A whole fruit the steps left do not need whole.
			int needWhole = 0;
			List<Stage> stages = s.dish.stages();
			for (Stage st : stages.subList(at, stages.size()))
			{
				for (int k = 0; k < st.uses.size(); k++)
				{
					if (st.uses.get(k) == fruit)
					{
						needWhole += st.counts.get(k);
					}
				}
			}
			int lackWhole = 0;
			for (Map<Ingredient, Integer> m : missing)
			{
				lackWhole += m.getOrDefault(fruit, 0);
			}
			int spare = s.held.getOrDefault(fruit.item(), 0) - (needWhole - lackWhole);
			if (spare > 0)
			{
				return g;
			}
		}
		return null;
	}

	/** The items a stage is done with: the one before it, else what it uses. */
	private static int[] input(List<Stage> stages, int at)
	{
		if (at > 0)
		{
			return new int[]{stages.get(at - 1).makes};
		}
		Stage st = stages.get(0);
		int[] ids = new int[st.uses.size()];
		for (int i = 0; i < ids.length; i++)
		{
			ids[i] = st.uses.get(i).item();
		}
		return ids;
	}

	private static Step deliver(Snapshot s)
	{
		Customer c = s.customer;
		StringBuilder why = new StringBuilder(clock(s));
		if (s.customerInSight)
		{
			why.append(c.getTitle()).append(" is in sight");
		}
		else
		{
			why.append(c.where());
			if (c.needs() != null)
			{
				why.append(" (").append(c.needs()).append(")");
			}
			if (!s.placeKnown)
			{
				why.append(" · not yet on your map: the Pilot marks it once you have seen ").append(c.getTitle());
			}
		}
		if (s.way != null && !s.customerInSight)
		{
			why.append(". ").append(s.way);
		}
		return Step.of("Deliver", "Deliver the " + s.dish.getTitle().toLowerCase(Locale.ROOT) + " to " + c.getTitle(), why.toString(),
			Step.Target.GNOME_CUSTOMER).withItems(s.dish.item());
	}

	/** "4:12 left · " or "" when the time is not known. */
	static String clock(Snapshot s)
	{
		return s.msLeft == UNKNOWN ? "" : GnomeRestaurant.clock(s.msLeft) + " left · ";
	}

	private static String shop(Dish d)
	{
		return d.kind() == GnomeRestaurant.Kind.COCKTAIL ? "Heckel Funch in the Grand Tree sells cocktail ingredients" : "Hudo in the Grand Tree sells gnome cooking ingredients";
	}

	private static boolean has(Snapshot s, int item)
	{
		return s.held.getOrDefault(item, 0) > 0;
	}

	private static String lower(String s)
	{
		return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
	}

	private static String article(String noun)
	{
		return "aeiou".indexOf(noun.charAt(0)) >= 0 ? "an" : "a";
	}

	/** The loop strip's names: each stage, then the delivery. */
	public static String[] loop(Dish d)
	{
		List<Stage> stages = d.stages();
		String[] out = new String[stages.size() + 1];
		for (int i = 0; i < stages.size(); i++)
		{
			out[i] = stages.get(i).name;
		}
		out[stages.size()] = "Deliver";
		return out;
	}
}
