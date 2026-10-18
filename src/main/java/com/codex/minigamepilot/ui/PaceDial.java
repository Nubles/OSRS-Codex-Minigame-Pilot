package com.codex.minigamepilot.ui;

import com.codex.minigamepilot.core.Pace;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/**
 * The last round against your best: the round's time in gilt, a chip saying how it compares,
 * and a bar with a notch where your best sits, so a slow round reads at a glance.
 */
class PaceDial extends JComponent
{
	private static final int HEIGHT = 50;

	private final View.PaceCard pace;

	PaceDial(View.PaceCard pace)
	{
		this.pace = pace;
		setOpaque(false);
		setPreferredSize(new Dimension(10, HEIGHT));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();

		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		Ui.shadowed(g, "LAST ROUND", 0, small.getAscent(), Theme.TEXT_MUTED);

		// The chip: a new best, or how far over or under the old one.
		String chip;
		Color tone;
		if (pace.isBest)
		{
			chip = "NEW BEST";
			tone = Theme.GOLD_BRIGHT;
		}
		else if (pace.againstBest > 0)
		{
			chip = "+" + seconds(pace.againstBest);
			tone = pace.againstBest <= 3 ? Theme.GOLD : Theme.EMBER;
		}
		else if (pace.againstBest < 0)
		{
			chip = "-" + seconds(-pace.againstBest);
			tone = Theme.GREEN;
		}
		else
		{
			chip = null;
			tone = Theme.TEXT_MUTED;
		}
		if (chip != null)
		{
			int cw = small.stringWidth(chip) + 10;
			int ch = small.getHeight() + 2;
			RoundRectangle2D pill = new RoundRectangle2D.Float(w - cw - 0.5f, 0.5f, cw, ch, ch, ch);
			g.setColor(Theme.alpha(tone, 40));
			g.fill(pill);
			g.setColor(Theme.alpha(tone, 170));
			g.draw(pill);
			Ui.shadowed(g, chip, w - cw + 4, 1 + small.getAscent(), tone);
		}

		g.setFont(Theme.title());
		FontMetrics big = g.getFontMetrics();
		int timeY = small.getHeight() + 4 + big.getAscent();
		String time = Pace.clock(pace.lastTicks);
		Ui.shadowed(g, time, 0, timeY, pace.isBest ? Theme.GOLD_BRIGHT : Theme.GOLD);
		if (pace.bestTicks > 0 && !pace.isBest)
		{
			g.setFont(Theme.small());
			Ui.shadowed(g, "best " + Pace.clock(pace.bestTicks), big.stringWidth(time) + 8, timeY, Theme.TEXT_FAINT);
		}

		// The bar: this round's length, with a notch where your best sits.
		int by = HEIGHT - 9;
		int bh = 6;
		int max = Math.max(pace.lastTicks, pace.bestTicks);
		double scale = (w - 2) / (max * 1.08);
		RoundRectangle2D track = new RoundRectangle2D.Float(0.5f, by + 0.5f, w - 1, bh, bh, bh);
		g.setPaint(new GradientPaint(0, by, Theme.INK_DEEP, 0, by + bh, Theme.mix(Theme.INK_DEEP, Theme.CARD, 0.6)));
		g.fill(track);
		int fill = (int) Math.round(pace.lastTicks * scale);
		Color barTone = pace.isBest ? Theme.GOLD_BRIGHT : pace.costly ? Theme.EMBER : Theme.GOLD;
		g.setPaint(new GradientPaint(0, by, Theme.mix(barTone, Theme.TEXT, 0.25), 0, by + bh, Theme.mix(barTone, Theme.INK_DEEP, 0.3)));
		g.fill(new RoundRectangle2D.Float(1, by + 1, Math.max(bh, fill), bh - 1, bh - 1, bh - 1));
		g.setColor(Theme.STROKE_SOFT);
		g.draw(track);
		if (pace.bestTicks > 0)
		{
			float x = (float) (1 + pace.bestTicks * scale);
			g.setStroke(new BasicStroke(1.4f));
			g.setColor(Theme.GOLD_BRIGHT);
			g.drawLine(Math.round(x), by - 2, Math.round(x), by + bh + 1);
			Path2D notch = new Path2D.Float();
			notch.moveTo(x - 3, by - 5);
			notch.lineTo(x + 3, by - 5);
			notch.lineTo(x, by - 1);
			notch.closePath();
			g.fill(notch);
		}
		g.dispose();
	}

	/** Ticks as seconds, rounded to the nearest. */
	private static String seconds(int ticks)
	{
		return Math.round(ticks * 0.6) + "s";
	}
}
