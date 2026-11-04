package com.codex.minigamepilot;

import com.codex.minigamepilot.core.Assault;
import com.codex.minigamepilot.core.Nmz;
import com.codex.minigamepilot.core.Volcanic;
import com.codex.minigamepilot.core.Wintertodt;
import com.codex.minigamepilot.core.Goals;
import com.codex.minigamepilot.core.MixCycle;
import com.codex.minigamepilot.core.Mixology;
import com.codex.minigamepilot.core.Tithe;
import com.codex.minigamepilot.core.Totems;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(CodexMinigamePilotConfig.GROUP)
public interface CodexMinigamePilotConfig extends Config
{
	String GROUP = "codexminigamepilot";

	/** Which bar the Blast Furnace pilot plans for. */
	enum BarChoice
	{
		AUTO("Work it out"),
		IRON("Iron"),
		STEEL("Steel"),
		MITHRIL("Mithril"),
		ADAMANTITE("Adamantite"),
		RUNITE("Runite"),
		GOLD("Gold"),
		SILVER("Silver");

		private final String label;

		BarChoice(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	/** What Motherlode nuggets are saved for. */
	enum MotherlodeGoal
	{
		NEXT_REWARD(Goals.Choice.NEXT_REWARD),
		PROSPECTOR_KIT(Goals.Choice.PROSPECTOR_KIT),
		COAL_BAG(Goals.Choice.COAL_BAG),
		GEM_BAG(Goals.Choice.GEM_BAG),
		BIGGER_SACK(Goals.Choice.BIGGER_SACK),
		EVERY_REWARD(Goals.Choice.EVERY_REWARD),
		NEXT_LEVEL(Goals.Choice.NEXT_LEVEL),
		OFF(Goals.Choice.OFF);

		private final Goals.Choice choice;

		MotherlodeGoal(Goals.Choice choice)
		{
			this.choice = choice;
		}

		public Goals.Choice choice()
		{
			return choice;
		}

		@Override
		public String toString()
		{
			return choice.toString();
		}
	}

	/** What Mahogany Homes points are saved for. */
	enum HomesGoal
	{
		NEXT_REWARD(Goals.Choice.NEXT_REWARD),
		PLANK_SACK(Goals.Choice.PLANK_SACK),
		CARPENTERS_OUTFIT(Goals.Choice.CARPENTERS_OUTFIT),
		AMYS_SAW(Goals.Choice.AMYS_SAW),
		HOSIDIUS_BLUEPRINTS(Goals.Choice.HOSIDIUS_BLUEPRINTS),
		EVERY_REWARD(Goals.Choice.EVERY_REWARD),
		NEXT_LEVEL(Goals.Choice.NEXT_LEVEL),
		OFF(Goals.Choice.OFF);

		private final Goals.Choice choice;

		HomesGoal(Goals.Choice choice)
		{
			this.choice = choice;
		}

		public Goals.Choice choice()
		{
			return choice;
		}

		@Override
		public String toString()
		{
			return choice.toString();
		}
	}

	/** The wood Vale Totems are built with. */
	enum TotemsWood
	{
		BEST(null),
		OAK(Totems.Wood.OAK),
		WILLOW(Totems.Wood.WILLOW),
		MAPLE(Totems.Wood.MAPLE),
		YEW(Totems.Wood.YEW),
		MAGIC(Totems.Wood.MAGIC),
		REDWOOD(Totems.Wood.REDWOOD);

		private final Totems.Wood wood;

		TotemsWood(Totems.Wood wood)
		{
			this.wood = wood;
		}

		/** The wood, or null for the best your level and logs allow. */
		public Totems.Wood wood()
		{
			return wood;
		}

		@Override
		public String toString()
		{
			return wood == null ? "Best you carry" : wood.getTitle();
		}
	}

	/** Plants a Tithe Farm round: the wiki's strategies. */
	enum TithePlants
	{
		SIXTEEN(Tithe.Plan.SIXTEEN),
		TWENTY(Tithe.Plan.TWENTY),
		TWENTY_THREE(Tithe.Plan.TWENTY_THREE),
		TWENTY_FIVE(Tithe.Plan.TWENTY_FIVE);

		private final Tithe.Plan plan;

		TithePlants(Tithe.Plan plan)
		{
			this.plan = plan;
		}

		public Tithe.Plan plan()
		{
			return plan;
		}

		@Override
		public String toString()
		{
			return plan.getTitle() + " (" + plan.cans() + " cans)";
		}
	}

	/** What Tithe Farm points are saved for. */
	enum TitheReward
	{
		NONE(null),
		FARMERS_OUTFIT(Tithe.Reward.FARMERS_OUTFIT),
		STRAWHAT(Tithe.Reward.STRAWHAT),
		JACKET(Tithe.Reward.JACKET),
		TROUSERS(Tithe.Reward.TROUSERS),
		BOOTS(Tithe.Reward.BOOTS),
		GRICOLLERS_CAN(Tithe.Reward.GRICOLLERS_CAN),
		SEED_BOX(Tithe.Reward.SEED_BOX),
		HERB_SACK(Tithe.Reward.HERB_SACK);

		private final Tithe.Reward reward;

		TitheReward(Tithe.Reward reward)
		{
			this.reward = reward;
		}

		/** The reward, or null for none. */
		public Tithe.Reward reward()
		{
			return reward;
		}

		@Override
		public String toString()
		{
			return reward == null ? "None" : reward.getTitle() + " (" + reward.points() + ")";
		}
	}

	/** What Barbarian Assault honour points are saved for. */
	enum AssaultReward
	{
		NONE(null),
		FIGHTER_TORSO(Assault.Reward.FIGHTER_TORSO),
		PENANCE_SKIRT(Assault.Reward.PENANCE_SKIRT),
		FIGHTER_HAT(Assault.Reward.FIGHTER_HAT),
		RANGER_HAT(Assault.Reward.RANGER_HAT),
		RUNNER_HAT(Assault.Reward.RUNNER_HAT),
		HEALER_HAT(Assault.Reward.HEALER_HAT),
		RUNNER_BOOTS(Assault.Reward.RUNNER_BOOTS),
		PENANCE_GLOVES(Assault.Reward.PENANCE_GLOVES),
		GRANITE_BODY(Assault.Reward.GRANITE_BODY),
		ROLE_LEVELS(Assault.Reward.ROLE_LEVELS);

		private final Assault.Reward reward;

		AssaultReward(Assault.Reward reward)
		{
			this.reward = reward;
		}

		/** The reward, or null for none. */
		public Assault.Reward reward()
		{
			return reward;
		}

		@Override
		public String toString()
		{
			if (reward == null)
			{
				return "None";
			}
			return reward.price() > 0 ? reward.getTitle() + " (" + reward.price() + " each role)" : reward.getTitle();
		}
	}

	/** What the Wintertodt's reward cart is played for. */
	enum WintertodtReward
	{
		NONE(null),
		PYROMANCER_OUTFIT(Wintertodt.Reward.PYROMANCER_OUTFIT),
		WARM_GLOVES(Wintertodt.Reward.WARM_GLOVES),
		BRUMA_TORCH(Wintertodt.Reward.BRUMA_TORCH),
		TOME_OF_FIRE(Wintertodt.Reward.TOME_OF_FIRE),
		PHOENIX(Wintertodt.Reward.PHOENIX);

		private final Wintertodt.Reward reward;

		WintertodtReward(Wintertodt.Reward reward)
		{
			this.reward = reward;
		}

		/** The reward, or null for none. */
		public Wintertodt.Reward reward()
		{
			return reward;
		}

		@Override
		public String toString()
		{
			return reward == null ? "None" : reward.getTitle();
		}
	}

	/** What Volcanic Mine points are saved for. */
	enum VolcanicReward
	{
		NONE(null),
		PROSPECTOR_KIT(Volcanic.Reward.PROSPECTOR_KIT),
		PROSPECTOR_HELMET(Volcanic.Reward.PROSPECTOR_HELMET),
		PROSPECTOR_JACKET(Volcanic.Reward.PROSPECTOR_JACKET),
		PROSPECTOR_LEGS(Volcanic.Reward.PROSPECTOR_LEGS),
		PROSPECTOR_BOOTS(Volcanic.Reward.PROSPECTOR_BOOTS),
		ASH_COVERED_TOME(Volcanic.Reward.ASH_COVERED_TOME),
		LARGE_WATER_CONTAINER(Volcanic.Reward.LARGE_WATER_CONTAINER),
		ORE_PACK(Volcanic.Reward.ORE_PACK);

		private final Volcanic.Reward reward;

		VolcanicReward(Volcanic.Reward reward)
		{
			this.reward = reward;
		}

		/** The reward, or null for none. */
		public Volcanic.Reward reward()
		{
			return reward;
		}

		@Override
		public String toString()
		{
			return reward == null ? "None" : reward.getTitle();
		}
	}

	/** What Nightmare Zone reward points are saved for. */
	enum NmzReward
	{
		NONE(null),
		HERB_BOX(Nmz.Reward.HERB_BOX),
		BLACK_MASK(Nmz.Reward.BLACK_MASK),
		SLAYER_HELMET(Nmz.Reward.SLAYER_HELMET),
		SALVE_AMULET(Nmz.Reward.SALVE_AMULET),
		SALVE_AMULET_E(Nmz.Reward.SALVE_AMULET_E),
		RING_OF_SUFFERING(Nmz.Reward.RING_OF_SUFFERING),
		RING_OF_THE_GODS(Nmz.Reward.RING_OF_THE_GODS),
		BERSERKER_RING(Nmz.Reward.BERSERKER_RING),
		WARRIOR_RING(Nmz.Reward.WARRIOR_RING),
		ARCHERS_RING(Nmz.Reward.ARCHERS_RING),
		SEERS_RING(Nmz.Reward.SEERS_RING),
		TYRANNICAL_RING(Nmz.Reward.TYRANNICAL_RING),
		TREASONOUS_RING(Nmz.Reward.TREASONOUS_RING),
		GRANITE_RING(Nmz.Reward.GRANITE_RING);

		private final Nmz.Reward reward;

		NmzReward(Nmz.Reward reward)
		{
			this.reward = reward;
		}

		/** The reward, or null for none. */
		public Nmz.Reward reward()
		{
			return reward;
		}

		@Override
		public String toString()
		{
			return reward == null ? "None" : reward == Nmz.Reward.HERB_BOX ? "Herb boxes" : reward.getTitle();
		}
	}

	/** What Vale Research Points are saved for. */
	enum TotemsReward
	{
		NONE(null),
		ENT_BRANCH(Totems.Reward.ENT_BRANCH),
		BOW_STRING_SPOOL(Totems.Reward.BOW_STRING_SPOOL),
		FLETCHING_KNIFE(Totems.Reward.FLETCHING_KNIFE),
		GREENMAN_MASK(Totems.Reward.GREENMAN_MASK);

		private final Totems.Reward reward;

		TotemsReward(Totems.Reward reward)
		{
			this.reward = reward;
		}

		/** The reward, or null for none. */
		public Totems.Reward reward()
		{
			return reward;
		}

		@Override
		public String toString()
		{
			return reward == null ? "None" : reward.getTitle() + " (" + reward.points() + ")";
		}
	}

	/** What Mastering Mixology resin is saved for. */
	enum MixologyReward
	{
		NONE(null),
		APPRENTICE_PACK(Mixology.Reward.APPRENTICE_PACK),
		ADEPT_PACK(Mixology.Reward.ADEPT_PACK),
		EXPERT_PACK(Mixology.Reward.EXPERT_PACK),
		GOGGLES(Mixology.Reward.GOGGLES),
		LABCOAT(Mixology.Reward.LABCOAT),
		PANTS(Mixology.Reward.PANTS),
		GLOVES(Mixology.Reward.GLOVES),
		REAGENT_POUCH(Mixology.Reward.REAGENT_POUCH),
		POTION_STORAGE(Mixology.Reward.POTION_STORAGE),
		CHUGGING_BARREL(Mixology.Reward.CHUGGING_BARREL),
		AMULET(Mixology.Reward.AMULET),
		ALDARIUM(Mixology.Reward.ALDARIUM);

		private final Mixology.Reward reward;

		MixologyReward(Mixology.Reward reward)
		{
			this.reward = reward;
		}

		/** The reward, or null for none. */
		public Mixology.Reward reward()
		{
			return reward;
		}

		@Override
		public String toString()
		{
			return reward == null ? "None" : reward.getTitle();
		}
	}

	@ConfigSection(name = "Minigames", description = "Which minigames the Pilot guides", position = 0)
	String MINIGAMES = "minigames";

	@ConfigSection(name = "Guidance", description = "How the next step is shown", position = 1)
	String GUIDANCE = "guidance";

	@ConfigSection(name = "Goals", description = "What each minigame is working toward: a reward from its shop or a level", position = 2)
	String GOALS = "goals";

	@ConfigSection(name = "Tick coach", description = "Every loop measured in ticks against your best and the floor of its walks", position = 3)
	String TICKS = "ticks";

	@ConfigSection(name = "Field notes", description = "A local record of what the Pilot saw, for checking its guidance", position = 4, closedByDefault = true)
	String NOTES = "notes";

	@ConfigItem(keyName = "motherlode", name = "Motherlode Mine", description = "Guide the Motherlode Mine loop: mine pay-dirt, deposit it, hammer broken struts, search the sack and bank", section = MINIGAMES, position = 0)
	default boolean motherlode()
	{
		return true;
	}

	@ConfigItem(keyName = "furnace", name = "Blast Furnace", description = "Guide the Blast Furnace loop: top up the coffer, load the belt, take the bars and bank them", section = MINIGAMES, position = 1)
	default boolean furnace()
	{
		return true;
	}

	@ConfigItem(keyName = "homes", name = "Mahogany Homes", description = "Guide Mahogany Homes contracts: get one, go to the house, fix up the furniture and tell the client", section = MINIGAMES, position = 2)
	default boolean homes()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bar",
		name = "Blast Furnace bar",
		description = "The bar the Blast Furnace pilot plans for, or Work it out to pick it from the ore you carry, the ore in the furnace or the ore in your bank",
		section = MINIGAMES,
		position = 3
	)
	default BarChoice bar()
	{
		return BarChoice.AUTO;
	}

	@ConfigItem(
		keyName = "cofferLow",
		name = "Top up coffer below",
		description = "Ask for the Blast Furnace coffer to be topped up when it holds fewer coins than this",
		section = MINIGAMES,
		position = 4
	)
	@Range(min = 0, max = 100_000)
	default int cofferLow()
	{
		return 2500;
	}

	@ConfigItem(
		keyName = "upperHopper",
		name = "Upper hopper unlocked",
		description = "Turn on if you have bought the Motherlode Mine's upper hopper, so it is offered straight away. The Pilot also learns it the first time you use it",
		section = MINIGAMES,
		position = 5
	)
	default boolean upperHopper()
	{
		return false;
	}

	@ConfigItem(
		keyName = "rift",
		name = "Guardians of the Rift",
		description = "Guide Guardians of the Rift: mine fragments, craft essence, enter the best open altar for your levels and points, then hand in",
		section = MINIGAMES,
		position = 6
	)
	default boolean rift()
	{
		return true;
	}

	@ConfigItem(
		keyName = "riftFragments",
		name = "Fragments before first craft",
		description = "How many guardian fragments to mine at the start of a game before crafting essence. The wiki's general strategy mines about 120",
		section = MINIGAMES,
		position = 7
	)
	@Range(min = 0, max = 500)
	default int riftFragments()
	{
		return 120;
	}

	@ConfigItem(
		keyName = "tempoross",
		name = "Tempoross",
		description = "Guide Tempoross: the double fishing spot when it is up, the shrine, the ammunition crate, a tether before each wave and the spirit pool",
		section = MINIGAMES,
		position = 8
	)
	default boolean tempoross()
	{
		return true;
	}

	@ConfigItem(
		keyName = "temporossCook",
		name = "Cook before loading",
		description = "Cook harpoonfish at the shrine before loading them: cooked ones drain 15 of Tempoross's energy, raw 10. Off loads them raw",
		section = MINIGAMES,
		position = 9
	)
	default boolean temporossCook()
	{
		return true;
	}

	@ConfigItem(
		keyName = "temporossStormLoad",
		name = "Load what you hold at storm",
		description = "Once the storm intensity reaches this percentage, load the fish you hold at once. At 100% the wave washes you away and the game's points are lost. 0 turns it off",
		section = MINIGAMES,
		position = 10
	)
	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	default int temporossStormLoad()
	{
		return 80;
	}

	@ConfigItem(
		keyName = "foundry",
		name = "Giants' Foundry",
		description = "Guide the Giants' Foundry: the best moulds you own for the commission, the crucible, the tool each stage needs at the heat it needs, and your swords kept",
		section = MINIGAMES,
		position = 11
	)
	default boolean foundry()
	{
		return true;
	}

	@ConfigItem(
		keyName = "mixology",
		name = "Mastering Mixology",
		description = "Guide Mastering Mixology: which orders your paste can fill, the levers, the vessel, the station each order names, the conveyor belt, the paste to refine next, and every delivery kept",
		section = MINIGAMES,
		position = 12
	)
	default boolean mixology()
	{
		return true;
	}

	@ConfigItem(
		keyName = "mixologyPlan",
		name = "Mixology orders to make",
		description = "Every order (the most from the 40% bonus for filling all three), or the most resin per paste (skip mammoth-might mix, alco-augmentator and liplack liquor unless a mixalot is ordered)",
		section = MINIGAMES,
		position = 13
	)
	default MixCycle.Plan mixologyPlan()
	{
		return MixCycle.Plan.ALL;
	}

	@ConfigItem(
		keyName = "mixologyCues",
		name = "Mixology station cues",
		description = "Light the agitator and the alembic the moment their quick action is live, and say to keep clicking the retort",
		section = MINIGAMES,
		position = 14
	)
	default boolean mixologyCues()
	{
		return true;
	}

	@ConfigItem(
		keyName = "valeTotems",
		name = "Vale Totems",
		description = "Guide Vale Totems: the next totem site and the logs to bring, the build, the three spirits each totem wants ringed in the carving choices, the decorations, the offerings to claim and the ent trails near you, and every totem kept",
		section = MINIGAMES,
		position = 15
	)
	default boolean valeTotems()
	{
		return true;
	}

	@ConfigItem(
		keyName = "totemsWood",
		name = "Vale totem wood",
		description = "The logs to build Vale totems with: the best your Fletching level allows of those you carry, or one wood",
		section = MINIGAMES,
		position = 16
	)
	default TotemsWood totemsWood()
	{
		return TotemsWood.BEST;
	}

	@ConfigItem(
		keyName = "totemsClaim",
		name = "Claim offerings as you pass",
		description = "Make claiming a site's offerings a step whenever you are at it with nothing else to do there; off, only when 15,000 offerings stop you building there",
		section = MINIGAMES,
		position = 17
	)
	default boolean totemsClaim()
	{
		return true;
	}

	@ConfigItem(
		keyName = "titheFarm",
		name = "Tithe Farm",
		description = "Guide the Tithe Farm: the seed your Farming grows, then each patch of your round to plant, water, harvest or clear, read from the patches, the water you carry, the fruit sack, and every sack kept",
		section = MINIGAMES,
		position = 18
	)
	default boolean titheFarm()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tithePlants",
		name = "Tithe plants a round",
		description = "How many patches a Tithe Farm round plants, as the wiki's strategies do: 16 or 20 (basic), 23 or 25. Fewer are planted when your seeds or water run short",
		section = MINIGAMES,
		position = 19
	)
	default TithePlants tithePlants()
	{
		return TithePlants.TWENTY;
	}

	@ConfigItem(
		keyName = "stealingArtefacts",
		name = "Stealing artefacts",
		description = "Guide stealing artefacts in Port Piscarilius: the house Captain Khaled names, its ladder and drawers, the artefact back to him on foot, and every trip kept with experience and coins an hour",
		section = MINIGAMES,
		position = 20
	)
	default boolean stealingArtefacts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "artefactsPatrols",
		name = "Mark patrols",
		description = "While you carry an artefact, outline the patrols near you in ember with a wedge on the ground the way each faces",
		section = MINIGAMES,
		position = 21
	)
	default boolean artefactsPatrols()
	{
		return true;
	}

	@ConfigItem(
		keyName = "stealingValuables",
		name = "Stealing valuables",
		description = "Guide stealing valuables in Civitas illa Fortis once you are at it (keys or valuables carried, or Thieving experience in the last three minutes): the citizen an urchin has distracted, the vacant house once its owner has gone and how long you have, the open window out, and every haul kept with coins and experience an hour",
		section = MINIGAMES,
		position = 22
	)
	default boolean stealingValuables()
	{
		return true;
	}

	@ConfigItem(
		keyName = "valuablesOwners",
		name = "Mark homeowners",
		description = "While you are inside a house or at its locked door, outline the homeowners near you in ember with a wedge on the ground the way each faces",
		section = MINIGAMES,
		position = 23
	)
	default boolean valuablesOwners()
	{
		return true;
	}

	@ConfigItem(
		keyName = "roguesDen",
		name = "Rogues' Den",
		description = "Guide the Rogues' Den maze in Burthorpe: in the lobby the levels, the crate to search, the bank and the doorway; in the maze the next leg of the wiki's way through and its obstacle, the right tile and the shortcut at 80 Thieving; every run kept with its time and where you were sent back, and the rogue set's time at your pace",
		section = MINIGAMES,
		position = 24
	)
	default boolean roguesDen()
	{
		return true;
	}

	@ConfigItem(
		keyName = "roguesDenTraps",
		name = "Mark maze traps",
		description = "In the Rogues' Den maze, wash the floor traps, wall traps and spinning blades near you in ember",
		section = MINIGAMES,
		position = 25
	)
	default boolean roguesDenTraps()
	{
		return true;
	}

	@ConfigItem(
		keyName = "barbarianAssault",
		name = "Barbarian Assault",
		description = "Keep every Barbarian Assault game with its waves, the role you played and their times, and the honour points it gave each role; show your points against the reward you want and the role to play next to reach it soonest. Nothing about the fight itself: RuneLite's Barbarian Assault plugin covers that",
		section = MINIGAMES,
		position = 26
	)
	default boolean barbarianAssault()
	{
		return true;
	}

	@ConfigItem(
		keyName = "wintertodt",
		name = "Wintertodt",
		description = "Guide a Wintertodt round one step at a time (chop, fletch, feed; light, fix and heal at your brazier; warm up in time), keep every round with its points and rewards, and show the reward you are after at your pace. RuneLite's Wintertodt plugin keeps its own notifications",
		section = MINIGAMES,
		position = 27
	)
	default boolean wintertodt()
	{
		return true;
	}

	@ConfigItem(
		keyName = "wintertodtFletch",
		name = "Fletch kindling",
		description = "At the Wintertodt, fletch a full pack of roots into kindling before feeding it: kindling burns for 25 points, a root for 10. Off: feed the roots as they are",
		section = MINIGAMES,
		position = 28
	)
	default boolean wintertodtFletch()
	{
		return true;
	}

	@ConfigItem(
		keyName = "wintertodtWarmth",
		name = "Warm up at",
		description = "At the Wintertodt, put eating or drinking first once your warmth falls to this percentage. At 0% you die. 0 turns it off",
		section = MINIGAMES,
		position = 29
	)
	@Range(min = 0, max = 90)
	@Units(Units.PERCENT)
	default int wintertodtWarmth()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "gnomeRestaurant",
		name = "Gnome Restaurant",
		description = "Guide a Gnome Restaurant delivery one step at a time (the dish's recipe with what you lack, then the customer marked with the time left), keep every delivery with its time and tip, and show every gnome dish as a checklist",
		section = MINIGAMES,
		position = 30
	)
	default boolean gnomeRestaurant()
	{
		return true;
	}

	@ConfigItem(
		keyName = "nightmareZone",
		name = "Nightmare Zone",
		description = "Keep every Nightmare Zone dream (mode, bosses, points, time, potions), rank your points an hour by the bosses you dream, and give the next step in the lobby. Nothing is said inside a dream",
		section = MINIGAMES,
		position = 31
	)
	default boolean nightmareZone()
	{
		return true;
	}

	@ConfigItem(
		keyName = "volcanicMine",
		name = "Volcanic Mine",
		description = "Keep every Volcanic Mine game (points, Mining experience, length, how it ended), your points and experience an hour, and the next step at the entrance. Inside the mine only the game's own figures are shown, with no cues or warnings",
		section = MINIGAMES,
		position = 32
	)
	default boolean volcanicMine()
	{
		return true;
	}

	@ConfigItem(keyName = "plaque", name = "Step plaque", description = "Show the next step on a small plaque in the game view", section = GUIDANCE, position = 0)
	default boolean plaque()
	{
		return true;
	}

	@ConfigItem(keyName = "route", name = "Path on the ground", description = "Draw the walk to the next step as a trail on the ground", section = GUIDANCE, position = 1)
	default boolean route()
	{
		return true;
	}

	@ConfigItem(keyName = "minimap", name = "Path on minimap", description = "Draw the walk to the next step on the minimap", section = GUIDANCE, position = 2)
	default boolean minimap()
	{
		return true;
	}

	@ConfigItem(keyName = "labels", name = "Action labels", description = "Name the action over the thing to click, such as Deposit", section = GUIDANCE, position = 3)
	default boolean labels()
	{
		return true;
	}

	@ConfigItem(keyName = "others", name = "Mark what comes after", description = "Quietly outline what comes after the next step: the next vein, furniture still to fix, and ent trails near you at Vale Totems", section = GUIDANCE, position = 4)
	default boolean others()
	{
		return true;
	}

	@ConfigItem(keyName = "hintArrow", name = "Hint arrow", description = "Point the game's hint arrow at a Mahogany Homes client's house, at the next Vale totem site when it is out of sight, at the house of your artefact task, or at the vacant house in Civitas illa Fortis", section = GUIDANCE, position = 5)
	default boolean hintArrow()
	{
		return true;
	}

	@ConfigItem(keyName = "worldMap", name = "World map marker", description = "Mark a Mahogany Homes client's house, the next Vale totem site out of sight, the house of your artefact task, or the vacant house in Civitas illa Fortis on the world map", section = GUIDANCE, position = 6)
	default boolean worldMap()
	{
		return true;
	}

	@ConfigItem(keyName = "visitChat", name = "Visit summary in chat", description = "When you leave the Motherlode Mine or the Blast Furnace, say in the chat what the visit came to", section = GUIDANCE, position = 7)
	default boolean visitChat()
	{
		return true;
	}

	@ConfigItem(keyName = "homesLowPlanks", name = "Low planks in chat", description = "After a Mahogany Homes contract, say in the chat when the planks left in your bank and pack cover fewer than two more", section = GUIDANCE, position = 8)
	default boolean homesLowPlanks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "motherlodeGoal",
		name = "Motherlode goal",
		description = "What to save nuggets for, with pieces you own anywhere ticked off; Next reward goes prospector kit, coal bag, gem bag, bigger sack, then the next Mining level",
		section = GOALS,
		position = 0
	)
	default MotherlodeGoal motherlodeGoal()
	{
		return MotherlodeGoal.NEXT_REWARD;
	}

	@ConfigItem(
		keyName = "homesGoal",
		name = "Mahogany Homes goal",
		description = "What to save points for; Next reward goes plank sack, carpenter's outfit, Amy's saw, Hosidius blueprints, then the next Construction level",
		section = GOALS,
		position = 1
	)
	default HomesGoal homesGoal()
	{
		return HomesGoal.NEXT_REWARD;
	}

	@ConfigItem(
		keyName = "furnaceGoal",
		name = "Blast Furnace level",
		description = "The Smithing level to work toward at the Blast Furnace, with 0 for the next level (or the level your Codex Navigator goals need)",
		section = GOALS,
		position = 2
	)
	@Range(max = 99)
	default int furnaceGoal()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "mixologyReward",
		name = "Mixology reward",
		description = "The Mastering Mixology reward your resin is saved for: the panel shows each resin against its price and the time to it at your pace",
		section = GOALS,
		position = 3
	)
	default MixologyReward mixologyReward()
	{
		return MixologyReward.NONE;
	}

	@ConfigItem(
		keyName = "totemsReward",
		name = "Vale Totems reward",
		description = "The Vale Research Exchange reward your research points are saved for: the panel shows your points (and the offerings you hold, at a hundred a point) against its price and the time to it at your pace",
		section = GOALS,
		position = 4
	)
	default TotemsReward totemsReward()
	{
		return TotemsReward.NONE;
	}

	@ConfigItem(
		keyName = "titheReward",
		name = "Tithe Farm reward",
		description = "The reward from Farmer Gricoller your Tithe Farm points are saved for: the panel shows your points against its price (less pieces you own) and the time and sacks to it at your pace",
		section = GOALS,
		position = 5
	)
	default TitheReward titheReward()
	{
		return TitheReward.NONE;
	}

	@ConfigItem(
		keyName = "artefactsLevel",
		name = "Stealing artefacts level",
		description = "The Thieving level to work toward when stealing artefacts, in artefacts and time at your pace, with 0 for the next level",
		section = GOALS,
		position = 6
	)
	@Range(max = 99)
	default int artefactsLevel()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "valuablesLevel",
		name = "Stealing valuables level",
		description = "The Thieving level to work toward when stealing valuables, in valuables and time at your pace, with 0 for the next level",
		section = GOALS,
		position = 7
	)
	@Range(max = 99)
	default int valuablesLevel()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "assaultReward",
		name = "Barbarian Assault reward",
		description = "The reward from Commander Connad your honour points are saved for (every role pays its price), or every role to level 5: the panel shows each role's points against it, the role to play next, and the games and time to it at your pace",
		section = GOALS,
		position = 8
	)
	default AssaultReward assaultReward()
	{
		return AssaultReward.FIGHTER_TORSO;
	}

	@ConfigItem(
		keyName = "wintertodtReward",
		name = "Wintertodt reward",
		description = "The reward cart prize you are after: the panel shows what you own of it, its chance a reward, and the rounds and time to it at your pace",
		section = GOALS,
		position = 9
	)
	default WintertodtReward wintertodtReward()
	{
		return WintertodtReward.PYROMANCER_OUTFIT;
	}

	@ConfigItem(
		keyName = "nmzReward",
		name = "Nightmare Zone reward",
		description = "What your reward points are for: herb boxes (with their herbs' worth at today's prices) or an imbue, with the time to it at your pace",
		section = GOALS,
		position = 12
	)
	default NmzReward nmzReward()
	{
		return NmzReward.HERB_BOX;
	}

	@ConfigItem(
		keyName = "nmzHalfImbues",
		name = "Imbues at half price",
		description = "The Hard Combat Achievements are done: Dom Onion's imbues cost half",
		section = GOALS,
		position = 13
	)
	default boolean nmzHalfImbues()
	{
		return false;
	}

	@ConfigItem(
		keyName = "volcanicReward",
		name = "Volcanic Mine reward",
		description = "What your Volcanic Mine points are for: the prospector kit or a piece of it, the ash covered tome, the large water container or ore packs, with the time to it at your pace",
		section = GOALS,
		position = 14
	)
	default VolcanicReward volcanicReward()
	{
		return VolcanicReward.PROSPECTOR_KIT;
	}

	@ConfigItem(
		keyName = "showGoals",
		name = "Show goals",
		description = "Show the goal card on the panel",
		section = GOALS,
		position = 10
	)
	default boolean showGoals()
	{
		return true;
	}

	@ConfigItem(
		keyName = "rewardRoad",
		name = "Reward road",
		description = "In each minigame, show its rewards as a road (at the Blast Furnace, the bars each Smithing level opens): what you own, what you can have now and how far off the rest is at your pace",
		section = GOALS,
		position = 11
	)
	default boolean rewardRoad()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tickCoach",
		name = "Tick coach",
		description = "On the panel, measure each loop in ticks against your best and the floor of its walks at run pace, name its biggest leak, and chart your loops with loops and experience an hour",
		section = TICKS,
		position = 0
	)
	default boolean tickCoach()
	{
		return true;
	}

	@ConfigItem(
		keyName = "fieldNotes",
		name = "Keep field notes",
		description = "While in a minigame, write what the Pilot sees and decides to .runelite/plugin-data/codex-minigame-pilot on your computer, for checking its guidance; nothing is sent anywhere",
		section = NOTES,
		position = 0
	)
	default boolean fieldNotes()
	{
		return false;
	}
}
