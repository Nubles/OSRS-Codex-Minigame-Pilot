package com.codex.minigamepilot.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.codex.minigamepilot.core.Totems.Animal;
import com.codex.minigamepilot.core.Totems.Wood;
import org.junit.Test;

public class TotemCycleTest
{
	private static TotemCycle.Snapshot at(int here, int level)
	{
		TotemCycle.Snapshot s = new TotemCycle.Snapshot();
		s.here = here;
		s.level = level;
		for (TotemCycle.Site x : s.sites)
		{
			x.distance = 20 * x.number;
		}
		if (here >= 0)
		{
			s.sites[here].distance = 1;
		}
		return s;
	}

	private static void built(TotemCycle.Site x, Wood w, Animal... wanted)
	{
		x.built = true;
		x.wood = w;
		System.arraycopy(wanted, 0, x.wanted, 0, wanted.length);
	}

	@Test
	public void buildsWithTheBestLogsCarriedYourLevelAllows()
	{
		TotemCycle.Snapshot s = at(0, 70);
		s.logs[Wood.OAK.ordinal()] = 3;
		s.logs[Wood.MAGIC.ordinal()] = 3;
		s.logs[Wood.MAPLE.ordinal()] = 3;
		assertEquals("Build a maple totem", TotemCycle.next(s).step.getTitle());
		s.choice = Wood.OAK;
		assertEquals("Build an oak totem", TotemCycle.next(s).step.getTitle());
	}

	@Test
	public void carvesWhatTheSiteWantsAndNotWhatIsCarved()
	{
		TotemCycle.Snapshot s = at(2, 50);
		TotemCycle.Site x = s.sites[2];
		built(x, Wood.WILLOW, Animal.SNAKE, Animal.JAGUAR, Animal.EAGLE);
		x.carved[0] = Animal.JAGUAR;
		TotemCycle.Decision d = TotemCycle.next(s);
		assertEquals("Carve the snake", d.step.getTitle());
		assertEquals(Animal.SNAKE, d.carve);
		assertEquals(1, TotemCycle.stage(d.step));
		x.carved[1] = Animal.SNAKE;
		assertEquals("Carve the eagle", TotemCycle.next(s).step.getTitle());
		x.carved[2] = Animal.EAGLE;
		assertEquals(TotemCycle.State.DECORATE, x.state());
	}

	@Test
	public void decoratesWithItsOwnWoodOrFletchesOrBanks()
	{
		TotemCycle.Snapshot s = at(0, 70);
		TotemCycle.Site x = s.sites[0];
		built(x, Wood.YEW, Animal.BUFFALO, Animal.EAGLE, Animal.SNAKE);
		x.carvedFlag = true;
		x.decorations = 1;
		s.decorations[Wood.MAPLE.ordinal()] = 4;
		assertEquals("Bank for yew decorations", TotemCycle.next(s).step.getTitle());
		s.logs[Wood.YEW.ordinal()] = 3;
		s.knife = true;
		assertEquals("Fletch your yew logs", TotemCycle.next(s).step.getTitle());
		s.decorations[Wood.YEW.ordinal()] = 3;
		TotemCycle.Decision d = TotemCycle.next(s);
		assertEquals("Decorate the totem (1 of 4)", d.step.getTitle());
		assertEquals("Any fletched yew item; 3 more finish it", d.step.getDetail());
	}

	@Test
	public void claimsAFullSiteBeforeBuilding()
	{
		TotemCycle.Snapshot s = at(0, 30);
		s.claimAsYouPass = false;
		s.logs[Wood.OAK.ordinal()] = 1;
		s.sites[0].offerings = Totems.OFFERINGS_LIMIT;
		assertEquals("Claim the offerings", TotemCycle.next(s).step.getTitle());
		s.sites[0].offerings = 14_999;
		assertEquals("Build an oak totem", TotemCycle.next(s).step.getTitle());
	}

	@Test
	public void onToYourUsualNextSiteElseTheNearest()
	{
		TotemCycle.Snapshot s = at(0, 30);
		s.logs[Wood.OAK.ordinal()] = 8;
		TotemCycle.Site h = s.sites[0];
		built(h, Wood.OAK, Animal.BUFFALO, Animal.EAGLE, Animal.SNAKE);
		h.carvedFlag = true;
		h.decorations = 4;
		TotemCycle.Decision d = TotemCycle.next(s);
		assertEquals("Go to site 2", d.step.getTitle());
		assertEquals(1, d.site);
		s.usualNext = 4;
		assertEquals("Go to site 5", TotemCycle.next(s).step.getTitle());
		// Offerings here are claimed first, when that setting is on.
		h.offerings = 120;
		assertEquals("Claim the offerings", TotemCycle.next(s).step.getTitle());
	}

	@Test
	public void waitsWhenEveryKnownTotemStands()
	{
		TotemCycle.Snapshot s = at(0, 30);
		for (TotemCycle.Site x : s.sites)
		{
			built(x, Wood.OAK);
			x.carvedFlag = true;
			x.decorations = 4;
		}
		TotemCycle.Decision d = TotemCycle.next(s);
		assertEquals("Wait for a totem to fall", d.step.getTitle());
		assertArrayEquals(new int[]{0, 0}, TotemCycle.needs(s));
	}

	@Test
	public void whatTheRestOfTheLoopTakes()
	{
		TotemCycle.Snapshot s = at(-1, 30);
		built(s.sites[1], Wood.OAK);
		s.sites[2].built = true;
		s.sites[2].carvedFlag = true;
		s.sites[2].decorations = 3;
		// Six empty sites (6 logs, 24 decorations), one to carve (4) and one a decoration short (1).
		assertArrayEquals(new int[]{6, 29}, TotemCycle.needs(s));
		assertEquals("eagle, buffalo and snake", TotemCycle.names(new Animal[]{Animal.EAGLE, Animal.BUFFALO, Animal.SNAKE}));
	}

	@Test
	public void belowTheLevelNothingIsGuessed()
	{
		assertEquals("Vale Totems needs 20 Fletching", TotemCycle.next(at(0, 19)).step.getTitle());
	}
}
