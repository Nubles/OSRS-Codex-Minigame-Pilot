package com.codex.minigamepilot.overlay;

import com.codex.minigamepilot.ui.Theme;
import java.awt.BasicStroke;
import java.awt.Color;

/**
 * The colours and strokes the overlays draw with, made once. The overlays draw every frame, so
 * a pulse or a fading trail picks its colour from a table here rather than making a new one.
 */
final class Ink
{
	/** Steps a pulse or a fade is cut into; more than the eye can tell apart at these sizes. */
	static final int STEPS = 16;

	static final BasicStroke THIN = new BasicStroke(1f);
	static final BasicStroke MEDIUM = new BasicStroke(1.5f);
	static final BasicStroke THICK = new BasicStroke(2f);

	static final Color OTHER_RIM = Theme.alpha(Theme.GOLD, 90);
	static final Color END_FILL = Theme.alpha(Theme.TEAL, 40);
	static final Color END_RIM = Theme.alpha(Theme.TEAL, 200);
	static final Color MINIMAP_BEAD = Theme.alpha(Theme.TEAL, 200);
	static final Color MINIMAP_SHADOW = Theme.alpha(Color.BLACK, 140);
	static final Color PLATE_FILL = Theme.alpha(Theme.INK_DEEP, 215);
	static final Color RING_RIM = Theme.alpha(Theme.GOLD_BRIGHT, 230);
	static final Color HULL_RIM = Theme.alpha(Theme.GOLD_BRIGHT, 220);

	/** The target's fill as it pulses, from 30 to 70 alpha, for gold and for teal. */
	static final Color[] GOLD_PULSE = pulse(Theme.GOLD, 30, 40);
	static final Color[] TEAL_PULSE = pulse(Theme.TEAL, 30, 40);
	/** A character's hull pulses a little softer. */
	static final Color[] HULL_PULSE = pulse(Theme.GOLD, 25, 35);
	static final Color GOLD_HOVER = Theme.alpha(Theme.GOLD, 90);
	static final Color TEAL_HOVER = Theme.alpha(Theme.TEAL, 90);
	static final Color GOLD_RIM = Theme.alpha(Theme.mix(Theme.GOLD, Color.WHITE, 0.15), 230);
	static final Color GOLD_RIM_HOVER = Theme.alpha(Theme.mix(Theme.GOLD, Color.WHITE, 0.4), 230);
	static final Color TEAL_RIM = Theme.alpha(Theme.mix(Theme.TEAL, Color.WHITE, 0.15), 230);
	static final Color TEAL_RIM_HOVER = Theme.alpha(Theme.mix(Theme.TEAL, Color.WHITE, 0.4), 230);

	/** The ground trail's beads, brightening toward the end, and their shadows; and the dimmer trail while waiting. */
	static final Color[] BEAD = new Color[STEPS + 1];
	static final Color[] BEAD_SHADOW = new Color[STEPS + 1];
	static final Color WAIT_BEAD = Theme.alpha(Theme.TEAL, 60);
	static final Color WAIT_SHADOW = Theme.alpha(Color.BLACK, 30);

	static
	{
		for (int i = 0; i <= STEPS; i++)
		{
			double t = i / (double) STEPS;
			int alpha = (int) Math.round(110 + 120 * t);
			BEAD[i] = Theme.alpha(Theme.mix(Theme.TEAL, Theme.TEXT, 0.25 * t), alpha);
			BEAD_SHADOW[i] = Theme.alpha(Color.BLACK, alpha / 2);
		}
	}

	private Ink()
	{
	}

	private static Color[] pulse(Color c, int from, int span)
	{
		Color[] out = new Color[STEPS + 1];
		for (int i = 0; i <= STEPS; i++)
		{
			out[i] = Theme.alpha(c, (int) Math.round(from + span * i / (double) STEPS));
		}
		return out;
	}

	/** The table entry for a value from 0 to 1. */
	static Color at(Color[] table, double t)
	{
		int i = (int) Math.round(Math.max(0, Math.min(1, t)) * STEPS);
		return table[i];
	}
}
