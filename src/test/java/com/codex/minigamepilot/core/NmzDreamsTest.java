package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** Following a dream from what the game shows a tick at a time. */
public class NmzDreamsTest
{
	private static final int[] NONE = new int[Nmz.Potion.values().length];

	private static int[] doses(int overload, int absorption)
	{
		int[] d = NONE.clone();
		d[Nmz.Potion.OVERLOAD.ordinal()] = overload;
		d[Nmz.Potion.ABSORPTION.ordinal()] = absorption;
		return d;
	}

	private static List<NmzDreams.Seen> bosses(NmzDreams.Seen... s)
	{
		return Arrays.asList(s);
	}

	@Test
	public void wholeDream()
	{
		NmzKills kills = new NmzKills();
		NmzDreams t = new NmzDreams(kills);
		assertNull(t.tick(1, false, -1, 100, NONE, null, 0));
		NmzDreams.Seen glod = new NmzDreams.Seen(5, "Glod", true);
		NmzDreams.Seen roc = new NmzDreams.Seen(6, "Giant Roc", true);
		assertNull(t.tick(2, true, 0, 74, doses(8, 20), bosses(glod, roc), 0));
		assertEquals(Nmz.Mode.CUSTOM, t.mode());
		assertEquals('H', t.difficulty());
		t.tick(3, true, 0, 74, doses(7, 16), bosses(glod, roc), 0);
		// Glod goes and the count rises two ticks later.
		t.tick(4, true, 0, 74, doses(7, 16), bosses(roc), 0);
		t.tick(5, true, 0, 74, doses(7, 16), bosses(roc), 0);
		t.tick(6, true, 3100, 74, doses(7, 16), bosses(roc), 0);
		assertEquals(3100, t.gained());
		assertEquals(1, kills.get("Glod", 'H').kills);
		assertEquals(3100, kills.get("Glod", 'H').points);
		NmzLog.Dream d = t.tick(10, false, -1, -1, doses(7, 16), Collections.emptyList(), 99);
		assertNotNull(d);
		assertEquals(8, d.ticks);
		assertEquals(3100, d.points);
		assertEquals(26, d.paid);
		assertEquals(Nmz.Mode.CUSTOM, d.mode);
		assertEquals('H', d.difficulty);
		assertEquals(1, d.doses(Nmz.Potion.OVERLOAD));
		assertEquals(4, d.doses(Nmz.Potion.ABSORPTION));
		assertEquals(Arrays.asList("Giant Roc", "Glod"), d.bosses);
		assertEquals(NmzLog.End.WOKE, d.end);
		assertEquals("Hard customisable rumble", d.kind());
	}

	@Test
	public void twoGoneIsNoLesson()
	{
		NmzKills kills = new NmzKills();
		NmzDreams t = new NmzDreams(kills);
		NmzDreams.Seen a = new NmzDreams.Seen(1, "Elvarg", false);
		NmzDreams.Seen b = new NmzDreams.Seen(2, "Me", false);
		t.tick(1, true, 0, -1, NONE, bosses(a, b), 0);
		t.tick(2, true, 4000, -1, NONE, bosses(), 0);
		assertEquals(0, kills.count());
		// Too long apart: the stale marks are dropped, and a later clean kill is learnt.
		NmzDreams.Seen c = new NmzDreams.Seen(3, "Tree spirit", false);
		t.tick(3, true, 4000, -1, NONE, bosses(c), 0);
		t.tick(20, true, 4000, -1, NONE, bosses(), 0);
		t.tick(21, true, 5000, -1, NONE, bosses(), 0);
		assertEquals(1, kills.count());
		assertEquals(1000, kills.get("Tree spirit", 'N').points);
	}

	@Test
	public void countThatCarriesOverIsNotThisDreams()
	{
		NmzDreams t = new NmzDreams(null);
		// The count still holds the last dream's for a tick, then begins again.
		t.tick(1, true, 52000, -1, NONE, null, 0);
		assertEquals(0, t.gained());
		t.tick(2, true, 0, -1, NONE, null, 0);
		t.tick(3, true, 800, -1, NONE, null, 0);
		assertEquals(800, t.gained());
	}

	@Test
	public void practiceAndUnknown()
	{
		NmzDreams t = new NmzDreams(null);
		t.tick(1, false, -1, 30, NONE, null, 0);
		t.tick(2, true, 0, 30, NONE, null, 0);
		NmzLog.Dream d = t.tick(50, false, -1, 30, NONE, null, 0);
		assertEquals(Nmz.Mode.PRACTICE, d.mode);
		assertEquals("Practice", d.kind());

		NmzDreams u = new NmzDreams(null);
		u.tick(2, true, 0, -1, NONE, null, 0);
		u.tick(3, true, 600, -1, NONE, null, 0);
		NmzLog.Dream e = u.logout(9, 0);
		assertEquals(Nmz.Mode.UNKNOWN, e.mode);
		assertEquals(-1, e.paid);
		assertEquals(NmzLog.End.LOGOUT, e.end);
		assertEquals("Dream", e.kind());
	}

	@Test
	public void blinkIsNotADream()
	{
		NmzDreams t = new NmzDreams(null);
		t.tick(1, true, 0, -1, NONE, null, 0);
		assertNull(t.tick(2, false, -1, -1, NONE, null, 0));
		assertEquals(0, t.dreams());
	}

	@Test
	public void potionsAddedAreNotDrunk()
	{
		NmzDreams t = new NmzDreams(null);
		t.tick(1, true, 0, -1, doses(4, 0), null, 0);
		t.tick(2, true, 0, -1, doses(8, 0), null, 0);
		t.tick(3, true, 0, -1, doses(7, 0), null, 0);
		assertEquals(1, t.used(Nmz.Potion.OVERLOAD));
		assertTrue(t.open());
	}
}
