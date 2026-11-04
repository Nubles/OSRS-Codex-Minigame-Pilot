package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.codex.minigamepilot.core.Barracuda;
import com.codex.minigamepilot.core.BarracudaLog;
import com.codex.minigamepilot.ui.View;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class BarracudaPilotTest
{
	private static List<BarracudaLog.Split> splits(int... ticks)
	{
		List<BarracudaLog.Split> out = new ArrayList<>();
		for (int t : ticks)
		{
			out.add(new BarracudaLog.Split(t, 3000, 3000));
		}
		return out;
	}

	private static BarracudaLog.Run ghost(int... ticks)
	{
		return new BarracudaLog.Run(1, Barracuda.Trial.TEMPOR_TANTRUM, Barracuda.Rank.SWORDFISH, ticks[ticks.length - 1], 0,
			BarracudaLog.End.FINISHED, splits(ticks));
	}

	@Test
	public void theLiveGap()
	{
		BarracudaLog.Run g = ghost(60, 120, 190);
		// Ahead at the first objective by 10 ticks.
		assertEquals(-10, BarracudaPilot.liveGap(splits(50), g, 70));
		// The clock passes the ghost's second objective before yours: at least that far behind.
		assertEquals(15, BarracudaPilot.liveGap(splits(50), g, 135));
		assertEquals(0, BarracudaPilot.liveGap(splits(50), null, 135));
		assertEquals(0, BarracudaPilot.liveGap(splits(), g, 30));
	}

	@Test
	public void stretchesAgainstTheGhost()
	{
		BarracudaLog.Run g = ghost(10, 20, 30, 40, 50, 60, 70, 80, 90, 100);
		BarracudaLog.Run r = ghost(10, 20, 30, 40, 50, 70, 80, 90, 100, 110);
		List<View.Split> s = BarracudaPilot.stretches(r, g);
		assertEquals(5, s.size());
		assertEquals("#5-6", s.get(2).name());
		assertEquals(30, s.get(2).ticks());
		assertEquals(20, s.get(2).best());
		assertEquals("Objectives #5-6 took 6.0s more than your best.", BarracudaPilot.slowNote(s));
		assertNull(BarracudaPilot.slowNote(BarracudaPilot.stretches(g, g)));
	}

	/** For the saved-key fuzzing: a damaged save decodes to whole pairs only. */
	public static void ranks(String saved)
	{
		String r = BarracudaPilot.decodeRanks(saved);
		org.junit.Assert.assertTrue(r.length() % 2 == 0 && r.length() <= 18);
	}

	@Test
	public void ranksSaved()
	{
		assertEquals("tsjk", BarracudaPilot.decodeRanks("1;tsjk"));
		assertEquals("ts", BarracudaPilot.decodeRanks("1;tstsxx"));
		assertEquals("", BarracudaPilot.decodeRanks("2;ts"));
		assertEquals("", BarracudaPilot.decodeRanks(null));
	}

	@Test
	public void theNextThingWorthDoing()
	{
		Map<String, String> store = new HashMap<>();
		BarracudaPilot p = new BarracudaPilot();
		p.setStore(store::get, store::put);
		assertEquals("Sailing to 30", p.lobbyStep(Barracuda.Trial.TEMPOR_TANTRUM, 20, p.log()).getTitle());
		assertEquals("Beat 1:48 for Swordfish", p.lobbyStep(Barracuda.Trial.TEMPOR_TANTRUM, 40, p.log()).getTitle());
		store.put(BarracudaPilot.RANKS_KEY, "1;tstktm");
		p.newAccount();
		// All of the Tantrum's ranks held, at 60 Sailing: the Jubbly Jive next.
		assertEquals("On to the Jubbly Jive", p.lobbyStep(Barracuda.Trial.TEMPOR_TANTRUM, 60, p.log()).getTitle());
		assertEquals("Next: Swordfish at the Jubbly Jive, 2:03 for the fetid key and 5,000 bonus Sailing xp.",
			p.lobbyStep(Barracuda.Trial.TEMPOR_TANTRUM, 60, p.log()).getDetail().substring("All three ranks here are yours. ".length()));
		// At 40 nothing more opens: experience.
		assertEquals("Run for experience", p.lobbyStep(Barracuda.Trial.TEMPOR_TANTRUM, 40, p.log()).getTitle());
		assertEquals(Barracuda.Rank.SWORDFISH, p.nextRank(Barracuda.Trial.GWENITH_GLIDE));
		assertEquals(Arrays.asList(Barracuda.Trial.values()).get(1), p.nextTrial(60, Barracuda.Trial.TEMPOR_TANTRUM));
	}
}
