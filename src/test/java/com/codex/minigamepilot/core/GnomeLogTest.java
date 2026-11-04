package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.codex.minigamepilot.core.GnomeRestaurant.Customer;
import com.codex.minigamepilot.core.GnomeRestaurant.Dish;
import java.util.Collections;
import org.junit.Test;

/** Deliveries kept per account, and the pace they come to. */
public class GnomeLogTest
{
	@Test
	public void roundTripAndPace()
	{
		GnomeLog g = new GnomeLog();
		g.add(new GnomeLog.Delivery(1000, false, Dish.CHEESE_TOM_BATTA, Customer.ROMETTI, 120_000, 0, true, 158, 0, null));
		g.add(new GnomeLog.Delivery(2000, true, Dish.DRUNK_DRAGON, Customer.CAPTAIN_NINTO, 400_000, 80_000, true, 160, 12_000,
			Collections.singletonMap(9472, 1)));
		g.add(new GnomeLog.Delivery(3000, true, Dish.TOAD_CRUNCHIES, null, 700_000, 0, false, 40, 999, Collections.singletonMap(257, 2)));
		GnomeLog back = GnomeLog.decode(g.encode());
		assertEquals(g.encode(), back.encode());
		assertEquals(3, back.count());
		assertEquals(2, back.onTime());
		assertNull(back.last().customer);
		// A late one earns nothing.
		assertEquals(0, back.last().gp);
		assertTrue(back.last().tip.isEmpty());
		assertEquals(0, back.last().credits());
		assertEquals(3, back.all().get(1).credits());
		assertEquals(1, back.all().get(0).credits());
		// 358 xp over 1,300,000 ms.
		assertEquals(358 * 3_600_000.0 / 1_300_000, back.xpPerHour(), 1e-6);
		assertEquals(12_000 * 3_600_000.0 / 1_300_000, back.gpPerHour(), 1e-6);
		assertEquals(120_000, back.fastest(false).ms);
		assertEquals(400_000, back.fastest(true).ms);
		assertEquals(Integer.valueOf(1), back.byDish().get(Dish.DRUNK_DRAGON));
		assertNull(back.byDish().get(Dish.TOAD_CRUNCHIES));
		assertEquals(Integer.valueOf(1), back.tips().get(9472));
	}

	@Test
	public void damageIsLeftOut()
	{
		GnomeLog g = GnomeLog.decode("1;5,E,NOPE,ROMETTI,1,0,o,1,0,;6,X,-,-,1,0,o,1,0,;7,H,-,-,1,0,o,1,0,9:x;8,E,-,-,1,0,o,1,0,");
		assertEquals(1, g.count());
		assertEquals(0, GnomeLog.decode("2;5,E,-,-,1,0,o,1,0,").count());
		assertEquals(0, GnomeLog.decode(null).count());
		assertEquals(-1, new GnomeLog().xpPerHour(), 0);
	}
}
