package com.codex.minigamepilot.core;

import com.codex.minigamepilot.core.Artefacts.House;
import net.runelite.api.gameval.ItemID;

/**
 * Works out the one next thing to do when stealing artefacts: ask Captain Khaled for a house, go
 * there, climb to the drawers (all but the north house's are upstairs), pick the lock, climb down
 * and take the artefact back to him on foot, clear of the patrols.
 *
 * <p>Facts are from the OSRS wiki (see {@link Artefacts}). It only reads; every click is yours.
 */
public final class ArtefactCycle
{
	/** The stages of a trip, for the loop strip. */
	public static final String[] LOOP = {"Task", "House", "Drawers", "Khaled"};

	private ArtefactCycle()
	{
	}

	/** Everything a decision needs, read from the game. */
	public static final class Snapshot
	{
		/** Thieving, boosts included. */
		public int level;
		/** The task varbit as read. */
		public int task;
		/** The house of the task: Captain Khaled's own words where heard, else the varbit's. Null for none. */
		public House house;
		/** Captain Khaled named a house that is not the one the varbit reads as. */
		public boolean heardOther;
		/** Captain Khaled named this house when he gave the task. */
		public boolean heard;
		public boolean carrying;
		/** A lockpick or hair clip in the pack. */
		public boolean pick;
		/** Kharedst's memoirs or the Book of the Dead in the pack. */
		public boolean teleport;
		/** The floor you are on. */
		public int plane;
		/** The house's drawers are in sight on your floor. */
		public boolean drawers;
		/** A ladder up is in sight on your floor. */
		public boolean ladderUp;
		/** A ladder down is in sight on your floor. */
		public boolean ladderDown;
		/** Patrols within a few tiles of you, and whether the nearest is facing your way. */
		public int patrolsNear;
		public int nearestPatrol = -1;
		public boolean patrolFacing;
	}

	/** A step and where it is in the trip. */
	public static final class Decision
	{
		public final Step step;
		public final int stage;

		Decision(Step step, int stage)
		{
			this.step = step;
			this.stage = stage;
		}
	}

	public static Decision next(Snapshot s)
	{
		if (s.carrying)
		{
			if (s.plane > 0)
			{
				return new Decision(Step.of("Climb", "Climb down the ladder",
					"Then on foot to Captain Khaled: a teleport or logging out loses the artefact", Step.Target.LADDER), 3);
			}
			return new Decision(Step.of("Deliver", "Take the artefact to Captain Khaled", deliverWhy(s), Step.Target.KHALED), 3);
		}
		if (s.level < Artefacts.LEVEL)
		{
			return new Decision(Step.waiting("Stealing artefacts needs 49 Thieving",
				"Yours is " + s.level + ". Captain Khaled gives houses from 49", Step.Target.NONE), 0);
		}
		if (s.task == Artefacts.TASK_FAILED || s.task == Artefacts.TASK_DELIVER)
		{
			// Failed, or the artefact was lost on the way (a teleport, a logout): he gives a new house.
			return new Decision(Step.of("Ask", "Back to Captain Khaled for a new house",
				s.task == Artefacts.TASK_FAILED ? "The artefact was confiscated; he hands out another task"
					: "No artefact in your pack; he hands out another task", Step.Target.KHALED), 0);
		}
		if (s.house == null)
		{
			return new Decision(Step.of("Ask", "Ask Captain Khaled for a house",
				"He is in the foodhall on the east side of the port" + (s.pick ? "" : ". Bring a lockpick or hair clip"), Step.Target.KHALED), 0);
		}
		if (!s.pick)
		{
			return new Decision(Step.items("Bring", "Bring a lockpick or hair clip",
				"The drawers in " + s.house.inSentence() + " need one", ItemID.LOCKPICK, ItemID.KR_HAIRCLIP), 1);
		}
		String said = s.heardOther ? " (as Captain Khaled said)" : "";
		if (s.drawers)
		{
			return new Decision(Step.of("Pick-lock", "Pick the lock of the drawers",
				"The artefact is inside: 750 Thieving xp" + said, Step.Target.DRAWERS), 2);
		}
		if (s.plane > 0)
		{
			// Upstairs, and these are not the drawers: the wrong house.
			return new Decision(Step.of("Climb", "Climb down: the drawers are in " + s.house.inSentence(),
				"Not this house" + said, Step.Target.LADDER), 1);
		}
		if (s.house.upstairs() && s.ladderUp)
		{
			return new Decision(Step.of("Climb", "Climb the ladder in " + s.house.inSentence(),
				"The drawers are upstairs" + said, Step.Target.LADDER), 1);
		}
		return new Decision(Step.of("Go", "Go to " + s.house.inSentence(),
			(s.house.upstairs() ? "Its drawers are upstairs, by the ladder" : "Its drawers are on the ground floor") + said, Step.Target.HOUSE), 1);
	}

	private static String deliverWhy(Snapshot s)
	{
		String why;
		if (s.nearestPatrol >= 0 && s.patrolFacing)
		{
			why = "A patrol " + s.nearestPatrol + (s.nearestPatrol == 1 ? " tile" : " tiles") + " off faces you: wait for it to turn";
		}
		else if (s.patrolsNear > 0)
		{
			why = "Keep clear of the " + (s.patrolsNear == 1 ? "patrol" : s.patrolsNear + " patrols") + " nearby; if caught the artefact is confiscated";
		}
		else
		{
			why = "On foot, clear of the patrols";
		}
		if (s.teleport)
		{
			why += ". No teleport until he has it";
		}
		return why;
	}
}
