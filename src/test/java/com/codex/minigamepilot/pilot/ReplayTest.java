package com.codex.minigamepilot.pilot;

import com.codex.replay.Replay;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Plays every script in {@code src/test/resources/replays} against the pilots. A script is a
 * scripted run through a minigame, tick by tick, with what the Pilot must and must never show;
 * see {@link Replay} for the commands.
 */
@RunWith(Parameterized.class)
public class ReplayTest
{
	@Parameterized.Parameters(name = "{0}")
	public static List<String> scripts()
	{
		return Replay.scripts("/replays");
	}

	@Parameterized.Parameter
	public String script;

	@Test
	public void plays()
	{
		Replay.play(script, PilotRig::new);
	}
}
