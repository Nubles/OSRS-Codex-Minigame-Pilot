package com.codex.minigamepilot.ui;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import javax.swing.JComponent;

/**
 * The minigame's loop as a row of waypoints joined by a gilt line that curls back to the start,
 * with the stage you are on lit. It says where this step sits in the round, and how far is left
 * before it begins again.
 */
class LoopStrip extends JComponent
{
	private static final int HEIGHT = 52;

	private final String[] stages;
	private final int at;

	LoopStrip(String[] stages, int at)
	{
		this.stages = stages;
		this.at = at;
		setOpaque(false);
		setPreferredSize(new Dimension(10, HEIGHT));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int n = stages.length;
		if (n == 0)
		{
			g.dispose();
			return;
		}
		int pad = 18;
		float span = n == 1 ? 0 : (w - pad * 2) / (float) (n - 1);
		int cy = 18;

		// The return path: a faint arc under the row from the last stage back to the first.
		Path2D back = new Path2D.Float();
		back.moveTo(pad + span * (n - 1), cy + 6);
		back.curveTo(pad + span * (n - 1), cy + 16, pad, cy + 16, pad, cy + 6);
		g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{2f, 3f}, 0f));
		g.setColor(Theme.alpha(Theme.GOLD_DIM, 120));
		g.draw(back);
		Path2D tip = new Path2D.Float();
		tip.moveTo(pad - 3, cy + 10);
		tip.lineTo(pad, cy + 6);
		tip.lineTo(pad + 3, cy + 10);
		g.setStroke(new BasicStroke(1f));
		g.draw(tip);

		// The way forward between the waypoints, gilt up to where you are.
		for (int i = 0; i < n - 1; i++)
		{
			float x0 = pad + span * i;
			float x1 = pad + span * (i + 1);
			g.setStroke(new BasicStroke(2f));
			g.setColor(i < at ? Theme.alpha(Theme.GOLD, 200) : Theme.STROKE);
			g.drawLine(Math.round(x0 + 6), cy, Math.round(x1 - 6), cy);
		}

		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		// When the names would run into each other, only the current one is written.
		boolean crowded = false;
		for (int i = 0; i < n - 1; i++)
		{
			int a = fm.stringWidth(stages[i].toUpperCase());
			int b = fm.stringWidth(stages[i + 1].toUpperCase());
			if ((a + b) / 2f + 4 > span)
			{
				crowded = true;
			}
		}
		for (int i = 0; i < n; i++)
		{
			float x = pad + span * i;
			boolean now = i == at;
			boolean past = i < at;
			if (now)
			{
				g.setColor(Theme.alpha(Theme.GOLD, 50));
				g.fill(new Ellipse2D.Float(x - 10, cy - 10, 20, 20));
			}
			float r = now ? 6 : 4.5f;
			Ellipse2D dot = new Ellipse2D.Float(x - r, cy - r, r * 2, r * 2);
			if (now)
			{
				g.setPaint(new GradientPaint(x, cy - r, Theme.GOLD_BRIGHT, x, cy + r, Theme.GOLD));
			}
			else
			{
				g.setColor(past ? Theme.GOLD_DIM : Theme.INK_DEEP);
			}
			g.fill(dot);
			g.setStroke(new BasicStroke(1.2f));
			g.setColor(now ? Theme.GOLD_BRIGHT : past ? Theme.GOLD : Theme.STROKE);
			g.draw(dot);

			if (crowded && !now)
			{
				continue;
			}
			String label = stages[i].toUpperCase();
			int lw = fm.stringWidth(label);
			int lx = Math.round(x - lw / 2f);
			lx = Math.max(0, Math.min(w - lw, lx));
			Ui.shadowed(g, label, lx, HEIGHT - 6, now ? Theme.GOLD_BRIGHT : past ? Theme.TEXT_MUTED : Theme.TEXT_FAINT);
		}
		g.dispose();
	}
}
