package com.codex.minigamepilot.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** The dream log: what is kept, read back, and the pace by set of bosses. */
public class NmzLogTest
{
	private static int[] doses(int overload, int absorption)
	{
		int[] d = new int[Nmz.Potion.values().length];
		d[Nmz.Potion.OVERLOAD.ordinal()] = overload;
		d[Nmz.Potion.ABSORPTION.ordinal()] = absorption;
		return d;
	}

	@Test
	public void roundTrip()
	{
		NmzLog g = new NmzLog();
		g.add(new NmzLog.Dream(1000, 3000, 60000, Nmz.Mode.CUSTOM, 'H', 26, NmzLog.End.WOKE, Arrays.asList("Elvarg", "Count Draynor"), doses(6, 20)));
		g.add(new NmzLog.Dream(2000, 1500, 9000, Nmz.Mode.UNKNOWN, '?', -1, NmzLog.End.LOGOUT, null, null));
		NmzLog back = NmzLog.decode(g.encode());
		assertEquals(g.encode(), back.encode());
		assertEquals("1;1000,3000,60000,C,H,26,w,Count Draynor|Elvarg,O6+A20;2000,1500,9000,?,?,-1,l,,", g.encode());
		assertEquals(2, back.count());
		assertEquals(29_000, back.all().get(0).potionPoints());
		assertEquals("6 overload · 20 absorption", back.all().get(0).potionsLine());
	}

	@Test
	public void paceBySet()
	{
		NmzLog g = new NmzLog();
		List<String> a = Arrays.asList("Elvarg", "Count Draynor");
		List<String> b = Arrays.asList("Me", "Tree spirit");
		// 60,000 in half an hour with 29,000 of potions; 30,000 in half an hour with none.
		g.add(new NmzLog.Dream(1, 3000, 60000, Nmz.Mode.CUSTOM, 'H', 26, NmzLog.End.WOKE, a, doses(6, 20)));
		g.add(new NmzLog.Dream(2, 3000, 30000, Nmz.Mode.CUSTOM, 'N', 22, NmzLog.End.WOKE, b, null));
		g.add(new NmzLog.Dream(3, 3000, 0, Nmz.Mode.PRACTICE, 'N', 0, NmzLog.End.WOKE, b, null));
		// Practice pays no points and is left out of the pace.
		assertEquals(90000, g.pointsPerHour(), 1e-6);
		assertEquals(61000, g.netPointsPerHour(), 1e-6);
		List<NmzLog.Pace> sets = g.bySet();
		assertEquals(2, sets.size());
		assertEquals("Hard customisable rumble", sets.get(0).kind);
		assertEquals(120000, sets.get(0).perHour(), 1e-6);
		assertEquals(62000, sets.get(0).netPerHour(), 1e-6);
		assertEquals(60000, sets.get(1).perHour(), 1e-6);
		// Habit: over the dreams that gave points.
		assertEquals(3, g.habit()[Nmz.Potion.OVERLOAD.ordinal()], 1e-9);
		assertEquals(60000, g.best().points);
	}

	@Test
	public void guards()
	{
		NmzLog.Dream d = new NmzLog.Dream(-5, NmzLog.LONGEST + 1, -3, null, 'x', 5000, null, Arrays.asList("", ";,"), new int[]{-1});
		assertEquals(0, d.endedAt);
		assertEquals(0, d.ticks);
		assertEquals(0, d.points);
		assertEquals(Nmz.Mode.UNKNOWN, d.mode);
		assertEquals('?', d.difficulty);
		assertEquals(-1, d.paid);
		assertEquals(0, d.bosses.size());
		assertEquals(-1, new NmzLog().pointsPerHour(), 0);
		assertNull(new NmzLog().best());
		assertEquals(0, NmzLog.decode("2;1,2,3").count());
		assertEquals(0, NmzLog.decode("1;1,2,3,C,H,26,w,x,Z9").count());
	}

	@Test
	public void kills()
	{
		NmzKills k = new NmzKills();
		k.kill("Elvarg", 'H', 2400);
		k.kill("Elvarg", 'H', 2600);
		k.kill("Me", 'N', 900);
		k.kill("Me", '?', 900);
		k.kill("Me", 'N', NmzKills.KILL_MAX + 1);
		assertEquals(2, k.count());
		assertEquals(2500, k.get("Elvarg", 'H').average(), 1e-9);
		assertEquals("Elvarg", k.all().get(0).name);
		NmzKills back = NmzKills.decode(k.encode());
		assertEquals(k.encode(), back.encode());
		assertEquals("1;H|Elvarg|2|5000;N|Me|1|900", k.encode());
		assertEquals(0, NmzKills.decode("1;H|Elvarg|0|5000").count());
	}
}
