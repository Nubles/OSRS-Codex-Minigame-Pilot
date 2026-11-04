package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.Houses.House;
import org.junit.Test;

/** The facts of stealing valuables against the OSRS wiki's own words and table. */
public class HousesTest
{
	@Test
	public void wikiFigures()
	{
		// "requiring level 50 Thieving and completion of Children of the Sun to participate."
		assertEquals(50, Houses.LEVEL);
		// "gaining 45 Thieving experience per stolen valuable."
		assertEquals(45, Houses.XP_EACH);
		// "a one-time bonus of 14 valuables and 630 Thieving experience." (14 x 45 = 630)
		assertEquals(14, Houses.SHINE_VALUABLES);
		assertEquals(630, Houses.SHINE_XP);
		assertEquals(Houses.SHINE_XP, Houses.xpFor(Houses.SHINE_VALUABLES));
		// "The arrow will disappear after around 7 seconds"
		assertEquals(7, Houses.SHINE_SECONDS);
		// "The homeowner will return 180–190 seconds after they left the house"
		assertEquals(180, Houses.AWAY_MIN);
		assertEquals(190, Houses.AWAY_MAX);
		// "players may loot each object for up to 50-60 seconds without additional input"
		assertEquals(50, Houses.LOOT_MIN);
		assertEquals(60, Houses.LOOT_MAX);
		// "lose around 90% of the valuables stolen from that house."
		assertEquals(90, Houses.CAUGHT_LOSS_PERCENT);
		// Oriana: "for 55 coins each"; "increased to 65 for players who have earned the Gladiator rank"
		assertEquals(55, Houses.ORIANA_PAYS);
		assertEquals(65, Houses.ORIANA_PAYS_GLADIATOR);
		// "Players can also pay 15 valuables to several urchins"
		assertEquals(15, Houses.URCHIN_FEE);
		// "Every 84-99 seconds, three street urchins ... for approximately 20 seconds"
		assertEquals(84, Houses.DISTRACT_EVERY_MIN);
		assertEquals(99, Houses.DISTRACT_EVERY_MAX);
		assertEquals(20, Houses.DISTRACT_FOR);
		// Wealthy citizen: "96 xp", 85 coins, House keys "~5/85", stun "4s", damage "3"
		assertEquals(96, Houses.PICKPOCKET_XP);
		assertEquals(85, Houses.PICKPOCKET_COINS);
		assertEquals(5, Houses.KEY_CHANCE);
		assertEquals(85, Houses.KEY_IN);
		assertEquals(4, Houses.STUN_SECONDS);
		assertEquals(3, Houses.STUN_DAMAGE);
		// "about 1,600–2,300 valuables from 18–19 houses per hour"; "about 90,000–125,000 coins per hour"
		assertEquals(1600, Houses.WIKI_VALUABLES_MIN);
		assertEquals(2300, Houses.WIKI_VALUABLES_MAX);
		assertEquals(90_000, Houses.WIKI_COINS_MIN);
		assertEquals(125_000, Houses.WIKI_COINS_MAX);
	}

	/** The wiki's coins an hour are its valuables an hour at Oriana's 55: 1,600 x 55 = 88,000, 2,300 x 55 = 126,500. */
	@Test
	public void wikiCoinsAgreeWithItsValuables()
	{
		assertTrue(Math.abs(Houses.WIKI_VALUABLES_MIN * Houses.ORIANA_PAYS - Houses.WIKI_COINS_MIN) <= 2500);
		assertTrue(Math.abs(Houses.WIKI_VALUABLES_MAX * Houses.ORIANA_PAYS - Houses.WIKI_COINS_MAX) <= 2500);
	}

	/** "50 | 72,000", "60 | 80,000", "70 | 93,000", "80 | 95,000", "90 | 100,000", "99 | 105,000". */
	@Test
	public void wikiXpTable()
	{
		int[][] rows = {{50, 72_000}, {60, 80_000}, {70, 93_000}, {80, 95_000}, {90, 100_000}, {99, 105_000}};
		for (int[] r : rows)
		{
			assertEquals("level " + r[0], r[1], Houses.wikiXpPerHour(r[0]));
		}
		assertEquals(76_000, Houses.wikiXpPerHour(55));
		// Below the activity's level and past 99, the ends of the table.
		assertEquals(72_000, Houses.wikiXpPerHour(1));
		assertEquals(105_000, Houses.wikiXpPerHour(120));
	}

	@Test
	public void houses()
	{
		// "Lavinia's house, directly south-west of the bazaar"; "Caius' house, directly north-west of the western bank".
		assertEquals("Lavinia's house", House.LAVINIA.title());
		assertEquals("Caius' house", House.CAIUS.title());
		assertEquals("Victor's", House.VICTOR.possessive());
		assertEquals("directly south-west of the bazaar", House.LAVINIA.where());
		assertEquals("directly north-west of the western bank", House.CAIUS.where());
		// Victor (Civitas illa Fortis): "owns the house directly south-west of the western bank".
		assertEquals("directly south-west of the western bank", House.VICTOR.where());
		assertEquals(House.LAVINIA, House.byOwner(13312));
		assertEquals(House.VICTOR, House.byOwner(13313));
		assertEquals(House.CAIUS, House.byOwner(13314));
		assertNull(House.byOwner(13302));
	}

	@Test
	public void valuablesForALevel()
	{
		assertEquals(0, Houses.valuablesFor(0));
		assertEquals(1, Houses.valuablesFor(1));
		assertEquals(1, Houses.valuablesFor(45));
		assertEquals(2, Houses.valuablesFor(46));
		assertEquals(0, Houses.xpFor(-3));
	}

	@Test
	public void orianasRate()
	{
		assertEquals(55, Houses.rate(-1));
		assertEquals(55, Houses.rate(55));
		assertEquals(65, Houses.rate(65));
		assertEquals(55, Houses.rate(1_000_000));
	}

	@Test
	public void clock()
	{
		assertEquals("3:00", Houses.clock(180));
		assertEquals("0:07", Houses.clock(7));
		assertEquals("0:00", Houses.clock(-4));
	}
}
