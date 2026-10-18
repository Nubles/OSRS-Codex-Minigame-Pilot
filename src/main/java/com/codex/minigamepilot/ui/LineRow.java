package com.codex.minigamepilot.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import javax.swing.JComponent;

/** One line of a checklist: a diamond that is hollow to do, lit for next and ticked when done. */
class LineRow extends JComponent
{
	private final View.Line line;

	LineRow(View.Line line)
	{
		this.line = line;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 18));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int mid = 9;
		Path2D diamond = new Path2D.Float();
		diamond.moveTo(5, mid - 5);
		diamond.lineTo(10, mid);
		diamond.lineTo(5, mid + 5);
		diamond.lineTo(0, mid);
		diamond.closePath();

		Color text;
		switch (line.mark)
		{
			case NEXT:
				g.setColor(Theme.alpha(Theme.GOLD, 50));
				g.fillRoundRect(-2, 0, w + 2, 18, 8, 8);
				g.setColor(Theme.GOLD_BRIGHT);
				g.fill(diamond);
				text = Theme.GOLD_BRIGHT;
				break;
			case DONE:
				g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.setColor(Theme.GREEN);
				g.drawLine(1, mid, 4, mid + 3);
				g.drawLine(4, mid + 3, 10, mid - 4);
				text = Theme.TEXT_FAINT;
				break;
			case IDLE:
				g.setColor(Theme.STROKE);
				g.draw(diamond);
				text = Theme.TEXT_FAINT;
				break;
			default:
				g.setStroke(new BasicStroke(1.2f));
				g.setColor(Theme.GOLD);
				g.draw(diamond);
				text = Theme.TEXT;
				break;
		}

		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		int right = w - 4;
		if (line.trailing != null && !line.trailing.isEmpty())
		{
			int tw = fm.stringWidth(line.trailing);
			Ui.shadowed(g, line.trailing, w - tw - 4, mid + 5, line.mark == View.Line.Mark.NEXT ? Theme.GOLD : Theme.TEXT_MUTED);
			right = w - tw - 10;
		}
		Ui.shadowed(g, Ui.clip(line.text, fm, right - 16), 16, mid + 5, text);
		g.dispose();
	}
}
