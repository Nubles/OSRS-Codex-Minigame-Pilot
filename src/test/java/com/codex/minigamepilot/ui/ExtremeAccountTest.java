package com.codex.minigamepilot.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.codex.minigamepilot.core.Figures;
import com.codex.minigamepilot.core.Goal;
import com.codex.minigamepilot.core.Goals;
import com.codex.minigamepilot.core.RewardRoad;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * A brand-new account (no xp, nothing owned, no pace) and a maxed one (200M xp, every reward
 * owned, a vast balance) through every goal and road, timed: none may throw, read a figure below
 * zero or "NaN", or take more than a few milliseconds.
 */
public class ExtremeAccountTest
{
	private static Goals.Input input(String skill, long xp, long balance, boolean ownsAll, double perHour)
	{
		Goals.Input in = new Goals.Input();
		in.have = id -> ownsAll;
		in.skill = skill;
		in.xp = xp;
		in.balance = balance;
		in.xpPerHour = perHour;
		in.currencyPerHour = perHour;
		in.currencyPerRound = 5;
		in.roundName = "expert contract";
		return in;
	}

	private static List<String> texts(Goals.Input in, String skill)
	{
		List<String> out = new ArrayList<>();
		List<Goal> goals = new ArrayList<>();
		for (Goals.Choice c : Goals.Choice.values())
		{
			goals.add(Goals.motherlode(c, input(skill, in.xp, in.balance, true, in.xpPerHour)));
			goals.add(Goals.motherlode(c, in));
			goals.add(Goals.homes(c, in));
		}
		goals.add(Goals.furnace(in));
		for (Goal g : goals)
		{
			if (g != null)
			{
				out.add(g.getTitle());
				out.add(g.describe());
				out.add(Figures.pair(g.getHave(), g.getNeed()));
			}
		}
		List<RewardRoad.Stop> stops = new ArrayList<>(RewardRoad.motherlode(in));
		stops.addAll(RewardRoad.homes(in));
		stops.addAll(RewardRoad.furnace(in));
		stops.addAll(RewardRoad.withLevel(RewardRoad.motherlode(in), skill, 99, "Every skill", in));
		for (RewardRoad.Stop s : stops)
		{
			out.add(RoadView.status(s, false));
			out.add(RoadView.tip(s));
		}
		out.add(RoadView.footnote(stops));
		return out;
	}

	@Test
	public void brandNewAndMaxedAccountsReadCleanlyAndQuickly()
	{
		Object[][] accounts = {
			{0L, -1L, 0.0},
			{0L, 0L, 0.0},
			{Goals.xpFor(99), 1_000_000L, 90_000.0},
			{200_000_000L, Long.MAX_VALUE / 4, 1e9},
		};
		for (Object[] a : accounts)
		{
			for (String skill : new String[]{"Mining", "Smithing", "Construction"})
			{
				Goals.Input in = input(skill, (Long) a[0], (Long) a[1], false, (Double) a[2]);
				for (int i = 0; i < 20; i++)
				{
					texts(in, skill);
				}
				// The quickest of ten goes, so a busy machine's pause is not taken for the code's cost.
				List<String> out = null;
				double ms = Double.MAX_VALUE;
				for (int i = 0; i < 10; i++)
				{
					long t = System.nanoTime();
					out = texts(in, skill);
					ms = Math.min(ms, (System.nanoTime() - t) / 1e6);
				}
				assertTrue(skill + " " + a[0] + ": " + ms + " ms", ms < 5);
				for (String s : out)
				{
					if (s != null)
					{
						assertFalse(s, s.contains("NaN") || s.contains("Infinity"));
						assertFalse(s, s.matches("(?s).*(^|[\\s(:])-\\d.*"));
					}
				}
			}
		}
	}
}
