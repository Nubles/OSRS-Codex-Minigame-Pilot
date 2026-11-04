package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.codex.minigamepilot.core.Pace;
import com.codex.minigamepilot.ui.View;
import net.runelite.api.Skill;
import org.junit.Test;

/** The tick coach's card as the panel gets it. */
public class TickCardTest
{
	private static final String[] NAMES = {"Bank", "Belt", "Bars"};

	private static Pace.Round trip(Pace p, int walkTiles, int perTick, int idle, int xp)
	{
		Pace.Round r = null;
		for (int i = 0; i < 6; i++)
		{
			p.tick(0, "bank", "the bank", false, false, 0, -1, 0, null, xp);
		}
		for (int i = 0; i < walkTiles / perTick; i++)
		{
			p.tick(1, "belt", "the belt", false, false, perTick, walkTiles, 0, null, xp);
		}
		for (int i = 0; i < idle; i++)
		{
			p.tick(1, "belt", "the belt", true, false, 0, walkTiles, 0, null, xp);
		}
		for (int i = 0; i < 5; i++)
		{
			p.tick(2, "bars", "the dispenser", true, true, 0, -1, 0, null, xp);
		}
		return p.tick(0, "bank", "the bank", false, false, 0, -1, 28, null, xp + 1000);
	}

	@Test
	public void theCardNamesTheLeakAndTheSkill()
	{
		Pace p = new Pace();
		p.tick(0, "bank", "the bank", false, false, 0, -1, 0, null, 0);
		Pace.Round first = trip(p, 16, 2, 0, 0);
		View.TickCard c = Coach.card(p, first, NAMES, Skill.SMITHING);
		assertEquals(c.getLastTicks(), first.ticks);
		assertEquals(8, c.getFloorTicks());
		assertEquals(8, c.getFootTicks());
		assertEquals(1, c.getLoops());
		assertTrue(c.getLeak().startsWith("No leak"));

		Pace.Round walked = trip(p, 16, 1, 0, 1000);
		c = Coach.card(p, walked, NAMES, Skill.SMITHING);
		assertEquals("8 ticks lost on foot to the belt", c.getLeak());
		assertEquals(2, c.getLoops());
		assertTrue(c.getPerHour() > 0);
	}

	@Test
	public void eachMinigameCountsItsOwnSkill()
	{
		assertEquals(Skill.MINING, Coach.skill(View.Activity.MOTHERLODE));
		assertEquals(Skill.SMITHING, Coach.skill(View.Activity.FURNACE));
		assertEquals(Skill.CONSTRUCTION, Coach.skill(View.Activity.HOMES));
		assertFalse(Coach.skill(View.Activity.NONE) != null);
	}
}
