package com.codex.minigamepilot.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * The last round cut into its stages: one engraved bar split in proportion to the time each
 * stage took, the slowest against its best lit in ember, and under each segment its name. A
 * fine tick inside a segment marks where your best for that stage would have ended it.
 */
class StageStrip extends javax.swing.JComponent
{
	private static final Color[] TONES = {Theme.GOLD, Theme.TEAL, Theme.GREEN, Theme.GOLD_DIM, Theme.TEXT_MUTED};

	private final List<View.Split> splits;

	StageStrip(List<View.Split> splits)
	{
		this.splits = splits;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 34));
		StringBuilder tip = new StringBuilder("<html>");
		for (View.Split s : splits)
		{
			tip.append(s.name).append(": ").append(com.codex.minigamepilot.core.Pace.clock(s.ticks));
			if (s.best > 0)
			{
				tip.append(" (best ").append(com.codex.minigamepilot.core.Pace.clock(s.best)).append(')');
			}
			tip.append("<br>");
		}
		setToolTipText(tip.append("</html>").toString());
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int total = 0;
		for (View.Split s : splits)
		{
			total += s.ticks;
		}
		if (total == 0)
		{
			g.dispose();
			return;
		}
		int by = 2;
		int bh = 10;
		RoundRectangle2D track = new RoundRectangle2D.Float(0.5f, by + 0.5f, w - 1, bh, bh, bh);
		g.setColor(Theme.INK_DEEP);
		g.fill(track);
		Shape old = g.getClip();
		g.clip(track);
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		double x = 1;
		double span = w - 2;
		int i = 0;
		double[] starts = new double[splits.size()];
		double[] widths = new double[splits.size()];
		for (View.Split s : splits)
		{
			double sw = span * s.ticks / total;
			starts[i] = x;
			widths[i] = sw;
			Color tone = s.slowest ? Theme.EMBER : TONES[i % TONES.length];
			g.setPaint(new GradientPaint(0, by, Theme.mix(tone, Theme.TEXT, 0.25), 0, by + bh, Theme.mix(tone, Theme.INK_DEEP, 0.3)));
			g.fillRect((int) Math.round(x), by, (int) Math.max(1, Math.round(sw) - 1), bh + 1);
			if (s.best > 0 && s.best < s.ticks)
			{
				// Where your best for this stage would have ended it.
				int bx = (int) Math.round(x + sw * s.best / (double) s.ticks);
				g.setStroke(new BasicStroke(1.2f));
				g.setColor(Theme.alpha(Theme.INK_DEEP, 200));
				g.drawLine(bx, by, bx, by + bh);
			}
			x += sw;
			i++;
		}
		g.setClip(old);
		g.setColor(Theme.STROKE_SOFT);
		g.draw(track);
		// Names under the segments that have room for them.
		for (int k = 0; k < splits.size(); k++)
		{
			View.Split s = splits.get(k);
			String label = s.name;
			if (fm.stringWidth(label) > widths[k] - 2)
			{
				label = s.name.substring(0, 1);
				if (fm.stringWidth(label) > widths[k])
				{
					continue;
				}
			}
			int lx = (int) Math.round(starts[k] + (widths[k] - fm.stringWidth(label)) / 2);
			Ui.shadowed(g, label, lx, by + bh + 4 + fm.getAscent(), s.slowest ? Theme.EMBER : Theme.TEXT_MUTED);
		}
		g.dispose();
	}
}
