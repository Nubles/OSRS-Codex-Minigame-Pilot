package com.codex.minigamepilot;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CodexMinigamePilotPluginRunner
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CodexMinigamePilotPlugin.class);
		RuneLite.main(args);
	}
}
