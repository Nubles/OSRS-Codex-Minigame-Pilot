package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;

public class HomeWayTest
{
	/** Larry's house is at 3033-3042, 3360-3368 (Home.LARRY); a landing a few tiles south. */
	private static final WayfinderAtlas.Teleport FALADOR = new WayfinderAtlas.Teleport("Falador Teleport", "Falador teleport",
		3036, 3340, 0, 4, true);
	private static final WayfinderAtlas.Teleport RING = new WayfinderAtlas.Teleport("Ring of wealth: Falador Park", "Ring of wealth",
		3036, 3356, 0, 3, false);

	@Test
	public void noAtlasNoPlan()
	{
		assertNull(HomeWay.plan(Home.LARRY, 3200, 3400, 0, null));
	}

	@Test
	public void aCarriedTeleportBeatsALongWalk()
	{
		WayfinderAtlas a = WayfinderAtlas.of(0.3, Collections.singletonList(FALADOR));
		HomeWay.Plan p = HomeWay.plan(Home.LARRY, 3200, 3400, 0, a);
		assertEquals(FALADOR, p.getBest().getTeleport());
		assertEquals(20, p.getBest().getTiles());
		assertEquals(4 + 6, p.getBest().getSeconds());
		assertNull(p.getBanked());
		assertEquals("Falador Teleport, about 10s", p.line());
	}

	@Test
	public void nearbyWalkingWinsAndSaysNothing()
	{
		WayfinderAtlas a = WayfinderAtlas.of(0.3, Collections.singletonList(FALADOR));
		HomeWay.Plan p = HomeWay.plan(Home.LARRY, 3030, 3350, 0, a);
		assertNull(p.getBest().getTeleport());
		assertNull(p.line());
	}

	@Test
	public void aQuickerTeleportInTheBankIsNamed()
	{
		WayfinderAtlas a = WayfinderAtlas.of(0.3, Arrays.asList(FALADOR, RING));
		// From Hosidius, nothing on foot: the ring lands 4 tiles off, the spell 20.
		HomeWay.Plan p = HomeWay.plan(Home.LARRY, 1700, 3600, 0, a);
		assertEquals(FALADOR, p.getBest().getTeleport());
		assertNull("4s against 10s is not worth a bank trip", p.getBanked());
		WayfinderAtlas far = WayfinderAtlas.of(0.6, Arrays.asList(FALADOR, RING));
		p = HomeWay.plan(Home.LARRY, 1700, 3600, 0, far);
		assertEquals(RING, p.getBanked().getTeleport());
		assertTrue(p.line().endsWith("Ring of wealth in your bank: 5s"));
	}

	@Test
	public void underground_walkingIsNotWeighed()
	{
		WayfinderAtlas a = WayfinderAtlas.of(0.3, Collections.emptyList());
		HomeWay.Plan p = HomeWay.plan(Home.LARRY, 3700, 5600, 0, a);
		assertNull(p.getBest());
		assertNull(p.line());
	}

	@Test
	public void theWikisWayForEachCity()
	{
		assertEquals("a Falador teleport", HomeWay.wikiWay(Home.City.FALADOR));
		assertEquals("a Varrock teleport", HomeWay.wikiWay(Home.City.VARROCK));
		assertEquals("an Ardougne teleport", HomeWay.wikiWay(Home.City.ARDOUGNE));
		assertTrue(HomeWay.wikiWay(Home.City.HOSIDIUS).contains("Xeric's talisman"));
	}

	@Test
	public void clock()
	{
		assertEquals("45s", HomeWay.clock(45));
		assertEquals("3m 05s", HomeWay.clock(185));
	}

	@Test
	public void atlasParsesWayfindersSave()
	{
		String json = "{\"version\":1,\"savedAt\":1760000000000,\"walkSecondsPerTile\":0.45,\"teleports\":["
			+ "{\"name\":\"Falador Teleport\",\"source\":\"Falador teleport\",\"kind\":\"SPELL\",\"x\":2966,\"y\":3379,\"plane\":0,\"seconds\":4,\"carried\":true,\"timed\":false},"
			+ "{\"name\":\"bad\",\"x\":-1,\"y\":3379,\"seconds\":4},"
			+ "{\"name\":\"Ring\",\"x\":2994,\"y\":3377,\"plane\":0,\"seconds\":3}],\"networks\":{},\"timedTrips\":0}";
		WayfinderAtlas a = WayfinderAtlas.parse(RuneLiteAPI.GSON, json);
		assertNotNull(a);
		assertEquals(2, a.getTeleports().size());
		assertEquals(0.45, a.getWalkSecondsPerTile(), 1e-9);
		assertTrue(a.getTeleports().get(0).isCarried());
		assertEquals("Ring", a.getTeleports().get(1).getSource());
		assertNull(WayfinderAtlas.parse(RuneLiteAPI.GSON, "{\"version\":2,\"teleports\":[]}"));
		assertEquals(WayfinderAtlas.RUN_SECONDS_PER_TILE, WayfinderAtlas.of(0, Collections.emptyList()).getWalkSecondsPerTile(), 1e-9);
	}
}
