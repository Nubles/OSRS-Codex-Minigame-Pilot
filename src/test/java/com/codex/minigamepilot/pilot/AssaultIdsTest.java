package com.codex.minigamepilot.pilot;

import static org.junit.Assert.assertEquals;

import com.codex.minigamepilot.core.Assault;
import com.codex.minigamepilot.core.Assault.Reward;
import com.codex.minigamepilot.core.Assault.Role;
import net.runelite.api.gameval.InterfaceID;
import org.junit.Test;

/** The Barbarian Assault ids the Pilot reads, against the wiki's infoboxes and RuneLite's gameval and plugin. */
public class AssaultIdsTest
{
	@Test
	public void wikiIds()
	{
		// Commander Connad: NPC ID 1656.
		assertEquals(1656, Assault.CONNAD);
		// Fighter torso 10551 (variants 20513, 24175, 33520, 33522), Penance skirt 10555 (20515, 24176),
		// Fighter hat 10548, Penance gloves 10553, Runner boots 10552, Granite body 10564.
		assertEquals(10551, Reward.FIGHTER_TORSO.items()[0]);
		assertEquals(5, Reward.FIGHTER_TORSO.items().length);
		assertEquals(20513, Reward.FIGHTER_TORSO.items()[1]);
		assertEquals(24175, Reward.FIGHTER_TORSO.items()[2]);
		assertEquals(10555, Reward.PENANCE_SKIRT.items()[0]);
		assertEquals(20515, Reward.PENANCE_SKIRT.items()[1]);
		assertEquals(24176, Reward.PENANCE_SKIRT.items()[2]);
		assertEquals(10548, Reward.FIGHTER_HAT.items()[0]);
		assertEquals(10553, Reward.PENANCE_GLOVES.items()[0]);
		assertEquals(10552, Reward.RUNNER_BOOTS.items()[0]);
		assertEquals(10564, Reward.GRANITE_BODY.items()[0]);
		assertEquals(0, Reward.ROLE_LEVELS.items().length);
	}

	@Test
	public void varbits()
	{
		// RuneLite's gameval VarbitID: BARBASSAULT_POINTS_*_BASE 4759 to 4762, *_EXTRA 4763 to 4766,
		// BARBASSULT_ROLELEVEL_ATT 3251, _DEF 3253, _COL 3254, _HEAL 3255.
		assertEquals(4759, Role.ATTACKER.baseVarbit());
		assertEquals(4763, Role.ATTACKER.extraVarbit());
		assertEquals(4760, Role.COLLECTOR.baseVarbit());
		assertEquals(4764, Role.COLLECTOR.extraVarbit());
		assertEquals(4762, Role.DEFENDER.baseVarbit());
		assertEquals(4766, Role.DEFENDER.extraVarbit());
		assertEquals(4761, Role.HEALER.baseVarbit());
		assertEquals(4765, Role.HEALER.extraVarbit());
		assertEquals(3251, Role.ATTACKER.levelVarbit());
		assertEquals(3254, Role.COLLECTOR.levelVarbit());
		assertEquals(3253, Role.DEFENDER.levelVarbit());
		assertEquals(3255, Role.HEALER.levelVarbit());
	}

	@Test
	public void interfacesAsRuneLitesPluginReadsThem()
	{
		// RuneLite's barbarianassault Role: each role's horn text in its own BARBASSAULT_OVER_* group (485 to 488).
		assertEquals(InterfaceID.BARBASSAULT_OVER_ATT, Role.ATTACKER.horn() >>> 16);
		assertEquals(InterfaceID.BARBASSAULT_OVER_COL, Role.COLLECTOR.horn() >>> 16);
		assertEquals(InterfaceID.BARBASSAULT_OVER_DEF, Role.DEFENDER.horn() >>> 16);
		assertEquals(InterfaceID.BARBASSAULT_OVER_HEAL, Role.HEALER.horn() >>> 16);
		// The wave-complete interface, group 497.
		assertEquals(497, Assault.WAVE_COMPLETE >>> 16);
	}
}
