package com.codex.minigamepilot.ui;

import com.codex.minigamepilot.core.Step;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * The top of the panel: the Pilot's medallion, where you are, the next action as one large gilt
 * word, and the full instruction under it. When the step changes the medallion's ring flares
 * once, so a glance at the panel shows that something new is asked.
 */
public class Helm extends JComponent
{
	private static final int MEDAL = 54;

	private String heading = "MINIGAME PILOT";
	private String place;
	private Step step;
	private String idleTitle = "Log in to begin";
	private float flare;
	private final Timer flareTimer;

	public Helm()
	{
		setOpaque(false);
		flareTimer = new Timer(30, e ->
		{
			flare = Math.max(0, flare - 0.04f);
			if (flare <= 0)
			{
				((Timer) e.getSource()).stop();
			}
			repaint();
		});
		setPreferredSize(new Dimension(10, 128));
	}

	public void update(String heading, String place, Step step, String idleTitle)
	{
		boolean changed = step != null && (this.step == null || !step.sameAs(this.step));
		this.heading = heading.toUpperCase();
		this.place = place;
		this.step = step;
		this.idleTitle = idleTitle;
		if (changed && isShowing())
		{
			flare = 1;
			flareTimer.restart();
		}
		revalidate();
		repaint();
	}

	@Override
	public Dimension getPreferredSize()
	{
		int w = Math.max(200, getWidth() > 0 ? getWidth() : PilotPanel.WIDTH + 25);
		FontMetrics bold = getFontMetrics(Theme.title());
		FontMetrics small = getFontMetrics(Theme.small());
		int text = w - 16;
		int h = 82;
		String title = step == null ? idleTitle : step.getTitle();
		h += wrap(title, bold, text, 3).size() * (bold.getHeight() - 2);
		if (step != null && !step.getDetail().isEmpty())
		{
			h += 2 + wrap(step.getDetail(), small, text, 3).size() * small.getHeight();
		}
		return new Dimension(w, h + 12);
	}

	static List<String> wrap(String text, FontMetrics fm, int width, int maxLines)
	{
		List<String> lines = new ArrayList<>();
		if (text == null || text.isEmpty())
		{
			return lines;
		}
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" "))
		{
			String next = line.length() == 0 ? word : line + " " + word;
			if (fm.stringWidth(next) > width && line.length() > 0)
			{
				lines.add(line.toString());
				line = new StringBuilder(word);
			}
			else
			{
				line = new StringBuilder(next);
			}
		}
		lines.add(line.toString());
		if (lines.size() > maxLines)
		{
			List<String> cut = new ArrayList<>(lines.subList(0, maxLines));
			cut.set(maxLines - 1, Ui.clip(cut.get(maxLines - 1) + " " + lines.get(maxLines), fm, width));
			return cut;
		}
		return lines;
	}

	/** Large letters made by doubling every pixel of the game's bold font, the way the game draws big numbers. */
	static int paintLarge(Graphics2D g, String text, int x, int top, Color tint)
	{
		Font font = Theme.title();
		FontMetrics fm = g.getFontMetrics(font);
		int w = Math.max(1, fm.stringWidth(text) + 1);
		int h = fm.getHeight() + 1;
		BufferedImage small = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D sg = small.createGraphics();
		sg.setFont(font);
		sg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		int base = fm.getAscent();
		sg.setColor(Color.BLACK);
		sg.drawString(text, 1, base + 1);
		sg.setPaint(new GradientPaint(0, 3, Theme.mix(tint, Color.WHITE, 0.35), 0, base, tint));
		sg.drawString(text, 0, base);
		sg.dispose();
		Graphics2D lg = (Graphics2D) g.create();
		lg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		lg.drawImage(small, x, top, w * 2, h * 2, null);
		lg.dispose();
		return w * 2;
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		g.setPaint(new GradientPaint(0, 0, Theme.mix(Theme.INK, Theme.GOLD_DIM, 0.22), 0, h, Theme.INK));
		g.fillRect(0, 0, w, h);

		// The medallion, lit from above, with a ring that flares when the step changes.
		int mx = 10;
		int my = 12;
		g.setPaint(new RadialGradientPaint(mx + MEDAL / 2f, my + MEDAL / 3f, MEDAL * 0.7f, new float[]{0, 1},
			new Color[]{Theme.mix(Theme.CARD_TOP, Theme.GOLD_DIM, 0.35), Theme.INK_DEEP}));
		g.fill(new Ellipse2D.Float(mx, my, MEDAL, MEDAL));
		if (flare > 0)
		{
			float r = MEDAL / 2f + 3 + (1 - flare) * 8;
			g.setStroke(new BasicStroke(2f));
			g.setColor(Theme.alpha(Theme.GOLD_BRIGHT, Math.round(200 * flare)));
			g.draw(new Ellipse2D.Float(mx + MEDAL / 2f - r, my + MEDAL / 2f - r, r * 2, r * 2));
		}
		g.setStroke(new BasicStroke(2f));
		g.setPaint(new GradientPaint(mx, my, Theme.GOLD_BRIGHT, mx, my + MEDAL, Theme.GOLD_DIM));
		g.draw(new Ellipse2D.Float(mx + 1, my + 1, MEDAL - 2, MEDAL - 2));
		g.setStroke(new BasicStroke(1f));
		g.setColor(Theme.alpha(Theme.GOLD, 70));
		g.draw(new Ellipse2D.Float(mx + 5, my + 5, MEDAL - 10, MEDAL - 10));
		Sigil.paint(g, mx + 11, my + 11, MEDAL - 22);

		int tx = mx + MEDAL + 12;
		int room = w - tx - 8;
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(Ui.clip(heading, small, room), tx, 22);
		if (place != null)
		{
			g.setColor(Theme.TEXT_FAINT);
			g.drawString(Ui.clip(place, small, room), tx, 70);
		}
		String verb = step == null ? "Ready" : step.getVerb();
		Color tint = step == null ? Theme.GOLD_DIM : step.isWaiting() ? Theme.TEAL : Theme.GOLD;
		paintLarge(g, verb, tx, 26, tint);

		// The instruction in full, then the line of why.
		int y = 82;
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		String title = step == null ? idleTitle : step.getTitle();
		for (String line : wrap(title, bold, w - 16, 3))
		{
			y += bold.getHeight() - 2;
			Ui.shadowed(g, line, 8, y, step == null ? Theme.TEXT_MUTED : Theme.TEXT);
		}
		if (step != null && !step.getDetail().isEmpty())
		{
			g.setFont(Theme.small());
			y += 2;
			for (String line : wrap(step.getDetail(), small, w - 16, 3))
			{
				y += small.getHeight();
				Ui.shadowed(g, line, 8, y, step.isWaiting() ? Theme.mix(Theme.TEAL, Theme.TEXT_MUTED, 0.4) : Theme.TEXT_MUTED);
			}
		}

		// The gilt rule under the helm, with a diamond at its middle.
		int ry = h - 2;
		g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.GOLD, 0), w / 2f, 0, Theme.alpha(Theme.GOLD, 150), true));
		g.drawLine(0, ry, w, ry);
		Path2D diamond = new Path2D.Float();
		diamond.moveTo(w / 2f, ry - 3);
		diamond.lineTo(w / 2f + 3, ry);
		diamond.lineTo(w / 2f, ry + 3);
		diamond.lineTo(w / 2f - 3, ry);
		diamond.closePath();
		g.setColor(Theme.GOLD);
		g.fill(diamond);
		g.dispose();
	}
}
