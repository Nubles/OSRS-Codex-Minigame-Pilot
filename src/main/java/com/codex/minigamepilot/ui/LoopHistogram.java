package com.codex.minigamepilot.ui;

import com.codex.minigamepilot.core.Figures;
import com.codex.minigamepilot.core.Loops;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import javax.swing.JComponent;

/**
 * Every loop of the session as a histogram of loop times in ticks: engraved gold bars on a deep
 * ink well, the bar holding the last loop lit in teal and the one holding your best marked with
 * a gilt pennant, the tick range written under each end.
 */
class LoopHistogram extends JComponent
{
	private static final int HEIGHT = 64;

	private final List<Loops.Bin> bins;

	LoopHistogram(List<Loops.Bin> bins)
	{
		this.bins = bins;
		setOpaque(false);
		setPreferredSize(new Dimension(10, HEIGHT));
		StringBuilder tip = new StringBuilder("<html>");
		for (Loops.Bin b : bins)
		{
			tip.append(b.from).append('-').append(b.to - 1).append(" ticks: ").append(b.count)
				.append(b.count == 1 ? " loop" : " loops").append("<br>");
		}
		setToolTipText(tip.append("</html>").toString());
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		if (bins.isEmpty())
		{
			return;
		}
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		int labelH = fm.getHeight();
		int top = 8;
		int bottom = HEIGHT - labelH - 2;

		RoundRectangle2D well = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, bottom + 2, 8, 8);
		g.setColor(Theme.INK_DEEP);
		g.fill(well);
		g.setColor(Theme.STROKE_SOFT);
		g.draw(well);

		int most = 1;
		for (Loops.Bin b : bins)
		{
			most = Math.max(most, b.count);
		}
		int n = bins.size();
		double slot = (w - 8) / (double) n;
		int gap = n > 6 ? 2 : 3;
		for (int i = 0; i < n; i++)
		{
			Loops.Bin b = bins.get(i);
			int x = (int) Math.round(4 + i * slot) + gap / 2;
			int bw = (int) Math.max(2, Math.round(slot) - gap);
			int h = b.count == 0 ? 0 : Math.max(3, (int) Math.round((bottom - top) * b.count / (double) most));
			int y = bottom - h;
			Color tone = b.last ? Theme.TEAL : Theme.GOLD;
			if (h > 0)
			{
				g.setPaint(new GradientPaint(0, y, Theme.mix(tone, Theme.TEXT, 0.2), 0, bottom, Theme.mix(tone, Theme.INK_DEEP, 0.45)));
				g.fillRoundRect(x, y, bw, h + 2, 3, 3);
				g.setColor(Theme.alpha(Theme.TEXT, 60));
				g.drawLine(x + 1, y + 1, x + bw - 2, y + 1);
			}
			else
			{
				g.setColor(Theme.STROKE_SOFT);
				g.drawLine(x, bottom - 1, x + bw - 1, bottom - 1);
			}
			if (b.best)
			{
				// A gilt pennant over the bar your best loop falls in.
				int px = x + bw / 2;
				int py = Math.max(2, y - 7);
				Path2D flag = new Path2D.Double();
				flag.moveTo(px - 3, py);
				flag.lineTo(px + 3, py);
				flag.lineTo(px, py + 5);
				flag.closePath();
				g.setColor(Theme.GOLD_BRIGHT);
				g.fill(flag);
			}
		}
		g.setColor(Theme.alpha(Theme.GOLD_DIM, 140));
		g.drawLine(4, bottom, w - 5, bottom);

		int baseline = HEIGHT - fm.getDescent() - 1;
		String from = Figures.count(bins.get(0).from) + " t";
		String to = Figures.count(bins.get(n - 1).to - 1) + " t";
		Ui.shadowed(g, from, 2, baseline, Theme.TEXT_FAINT);
		Ui.shadowed(g, to, w - 2 - fm.stringWidth(to), baseline, Theme.TEXT_FAINT);
		String mid = "loop time";
		Ui.shadowed(g, mid, (w - fm.stringWidth(mid)) / 2, baseline, Theme.alpha(Theme.TEXT_FAINT, 160));
		g.dispose();
	}
}
