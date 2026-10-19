package com.codex.minigamepilot;

import com.codex.minigamepilot.core.Goals;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

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

	@ConfigSection(name = "Minigames", description = "Which minigames the Pilot guides", position = 0)
	String MINIGAMES = "minigames";

	@ConfigSection(name = "Guidance", description = "How the next step is shown", position = 1)
	String GUIDANCE = "guidance";

	@ConfigSection(name = "Goals", description = "What each minigame is working toward: a reward from its shop or a level", position = 2)
	String GOALS = "goals";

	@ConfigSection(name = "Field notes", description = "A local record of what the Pilot saw, for checking its guidance", position = 3, closedByDefault = true)
	String NOTES = "notes";

	@ConfigItem(keyName = "motherlode", name = "Motherlode Mine", description = "Guide the Motherlode Mine loop", section = MINIGAMES, position = 0)
	default boolean motherlode()
	{
		return true;
	}

	@ConfigItem(keyName = "furnace", name = "Blast Furnace", description = "Guide the Blast Furnace loop", section = MINIGAMES, position = 1)
	default boolean furnace()
	{
		return true;
	}

	@ConfigItem(keyName = "homes", name = "Mahogany Homes", description = "Guide Mahogany Homes contracts", section = MINIGAMES, position = 2)
	default boolean homes()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bar",
		name = "Blast Furnace bar",
		description = "The bar to plan for. Work it out picks it from the ore you carry, the ore in the furnace or the ore in your bank",
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

	@ConfigItem(keyName = "plaque", name = "Step plaque", description = "Show the next step on a small plaque in the game view", section = GUIDANCE, position = 0)
	default boolean plaque()
	{
		return true;
	}

	@ConfigItem(keyName = "route", name = "Path", description = "Draw the walk to the next step as a trail on the ground", section = GUIDANCE, position = 1)
	default boolean route()
	{
		return true;
	}

	@ConfigItem(keyName = "minimap", name = "Path on minimap", description = "Draw the walk on the minimap too", section = GUIDANCE, position = 2)
	default boolean minimap()
	{
		return true;
	}

	@ConfigItem(keyName = "labels", name = "Action labels", description = "Name the action over the thing to click, such as Deposit", section = GUIDANCE, position = 3)
	default boolean labels()
	{
		return true;
	}

	@ConfigItem(keyName = "others", name = "Mark what comes after", description = "Quietly outline the next vein, and furniture still to fix", section = GUIDANCE, position = 4)
	default boolean others()
	{
		return true;
	}

	@ConfigItem(keyName = "hintArrow", name = "Hint arrow", description = "Point the game's hint arrow at a Mahogany Homes client's house", section = GUIDANCE, position = 5)
	default boolean hintArrow()
	{
		return true;
	}

	@ConfigItem(keyName = "worldMap", name = "World map marker", description = "Mark a Mahogany Homes client's house on the world map", section = GUIDANCE, position = 6)
	default boolean worldMap()
	{
		return true;
	}

	@ConfigItem(
		keyName = "motherlodeGoal",
		name = "Motherlode goal",
		description = "What to save nuggets for. Next reward goes prospector kit, coal bag, gem bag, bigger sack, then the next Mining level. Pieces you own anywhere, the bank included, are ticked off",
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
		description = "What to save points for. Next reward goes plank sack, carpenter's outfit, Amy's saw, Hosidius blueprints, then the next Construction level",
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
		description = "The Smithing level to work toward at the Blast Furnace. 0 means the next level, or the level your Codex Navigator goals need when that plugin is installed",
		section = GOALS,
		position = 2
	)
	@Range(max = 99)
	default int furnaceGoal()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "showGoals",
		name = "Show goals",
		description = "Show the goal card on the panel",
		section = GOALS,
		position = 3
	)
	default boolean showGoals()
	{
		return true;
	}

	@ConfigItem(
		keyName = "fieldNotes",
		name = "Keep field notes",
		description = "While in a minigame, write what the Pilot sees and decides (steps, objects nearby, varbits, game messages, rounds) to .runelite/codex-minigame-pilot. Stays on your computer; nothing is sent anywhere",
		section = NOTES,
		position = 0
	)
	default boolean fieldNotes()
	{
		return false;
	}
}
