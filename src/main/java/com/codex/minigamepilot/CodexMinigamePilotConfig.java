package com.codex.minigamepilot;

import com.codex.minigamepilot.core.Goals;
import com.codex.minigamepilot.core.MixCycle;
import com.codex.minigamepilot.core.Mixology;
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

	@ConfigItem(keyName = "hintArrow", name = "Hint arrow", description = "Point the game's hint arrow at a Mahogany Homes client's house, or at the next Vale totem site when it is out of sight", section = GUIDANCE, position = 5)
	default boolean hintArrow()
	{
		return true;
	}

	@ConfigItem(keyName = "worldMap", name = "World map marker", description = "Mark a Mahogany Homes client's house, or the next Vale totem site out of sight, on the world map", section = GUIDANCE, position = 6)
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
		keyName = "showGoals",
		name = "Show goals",
		description = "Show the goal card on the panel",
		section = GOALS,
		position = 5
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
		position = 6
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
