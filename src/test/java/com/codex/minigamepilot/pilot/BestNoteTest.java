package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.codex.minigamepilot.core.BestVisit;
import com.codex.minigamepilot.core.Visit;
import com.codex.minigamepilot.ui.View;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/** A visit that beats your best is said when you walk out, even if a hop summed it up first. */
public class BestNoteTest
{
	private static final long H = 3_600_000L;
	private static final long T = 1_760_000_000_000L;

	private final Map<String, String> saved = new HashMap<>();
	private final Pilots pilots = new Pilots(null, null, c -> { }, null, a -> null, (a, s) -> { }, id -> 0,
		saved::get, saved::put, () -> null);

	private void visit(long started, long ms, int payDirt, boolean walkedOut)
	{
		pilots.keep(View.Activity.MOTHERLODE, Visit.of(started + ms, ms, Collections.singletonList(Session.format(payDirt) + " pay-dirt")),
			BestVisit.of(started, started + ms, ms, payDirt, 0), walkedOut);
	}

	@Test
	public void aHopMidVisitStillLetsTheNewBestBeSaid()
	{
		// An old record: 1,000 pay-dirt an hour.
		visit(T, H, 1000, true);
		pilots.takeNote();
		// Days later, an hour at 1,500 an hour, then a hop: summed up quietly, the record taken.
		long s = T + 100 * H;
		visit(s, H, 1500, false);
		assertNull(pilots.takeNote());
		// Walked out an hour later, a little slower than at the hop but well past the old record.
		visit(s, 2 * H, 2900, true);
		assertEquals("Motherlode Mine, 2h 0m: 2,900 pay-dirt. A new best: 1,500 pay-dirt an hour, up from 1,000.",
			pilots.takeNote());
		// Walked out again later in the same session: nothing new to say about the record.
		visit(s, 3 * H, 4000, true);
		assertEquals("Motherlode Mine, 3h 0m: 4,000 pay-dirt.", pilots.takeNote());
	}

	@Test
	public void aRecordTakenByAnOlderSessionIsNotSaidByALaterOne()
	{
		visit(T, H, 1000, true);
		pilots.takeNote();
		// Beaten at a logout, and that session never walked out.
		visit(T + 100 * H, H, 1500, false);
		// A new session, slower than both: no word of a best.
		visit(T + 200 * H, H, 1200, true);
		assertEquals("Motherlode Mine, 1h 0m: 1,200 pay-dirt.", pilots.takeNote());
	}
}
