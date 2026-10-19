package com.codex.minigamepilot.ui;

import com.codex.minigamepilot.core.Goal;
import com.codex.minigamepilot.core.RewardRoad;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;
import javax.swing.JComponent;

/**
 * The reward road: a minigame's whole shop as a column of medallions joined by a gilt thread,
 * each holding the reward's own item. Owned ones are rimmed in gold and ticked, the next is
 * ringed by how much of it you already hold, and the line beside each says what it takes, lit
 * gold when you can buy it now. The thread is solid as far as you own and dotted beyond.
 */
class RoadView extends JComponent
{
	static final int ROW = 42;
	private static final int D = 32;
	private static final BasicStroke THREAD = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final BasicStroke THREAD_AHEAD = new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{2, 4}, 0);
	private static final BasicStroke RIM = new BasicStroke(1.4f);
	private static final BasicStroke ARC = new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final BasicStroke TICK = new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	private final List<RewardRoad.Stop> stops;
	private final BufferedImage[] icons;

	/** @param icons an item's picture by its id (the panel keeps them, and repaints as they load), or null to draw the medallions bare */
	RoadView(List<RewardRoad.Stop> stops, IntFunction<BufferedImage> icons)
	{
		this.stops = stops;
		this.icons = new BufferedImage[stops.size()];
		for (int i = 0; i < stops.size(); i++)
		{
			int id = stops.get(i).getIcon();
			this.icons[i] = icons == null || id <= 0 ? null : icons.apply(id);
		}
		setOpaque(false);
		setPreferredSize(new Dimension(10, stops.size() * ROW));
		MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				int i = e.getY() / ROW;
				setToolTipText(i >= 0 && i < stops.size() ? tip(stops.get(i)) : null);
			}
		};
		addMouseMotionListener(mouse);
	}

	/** The first stop not yet owned, or -1 when the road is done. */
	static int next(List<RewardRoad.Stop> stops)
	{
		for (int i = 0; i < stops.size(); i++)
		{
			if (!stops.get(i).isOwned())
			{
				return i;
			}
		}
		return -1;
	}

	static String count(long n)
	{
		return com.codex.minigamepilot.core.Figures.count(n);
	}

	/** "1,250 nuggets", "320 points", "412K xp". */
	static String amount(RewardRoad.Stop s, long n)
	{
		Goal.Currency c = s.getCurrency();
		if (c == null)
		{
			return com.codex.minigamepilot.core.Figures.brief(n) + " xp";
		}
		return count(n) + " " + c.of(n);
	}

	/**
	 * The line under a stop's name: owned, yours now, or what it takes and when. The next stop
	 * says what is still to earn ("36 to go, 1h 43m"); later ones what they cost and when, at
	 * your pace, they are yours once the stops above are ("100 nuggets, in 6h 29m").
	 */
	static String status(RewardRoad.Stop s, boolean next)
	{
		return status(s, next, false);
	}

	/** @param compact the shorter form for a narrow row: when it comes, or what is left, alone */
	static String status(RewardRoad.Stop s, boolean next, boolean compact)
	{
		if (s.isOwned())
		{
			return s.getLevel() > 0 ? "Reached" : "Owned";
		}
		if (s.isAffordable())
		{
			return compact ? "Yours to buy now" : "Yours to buy now, " + amount(s, s.getCost());
		}
		if (!s.isKnown())
		{
			return amount(s, s.getCost());
		}
		String when = s.getMinutesAway() > 0 ? Goal.duration(s.getMinutesAway())
			: s.getRoundsAway() > 0 && s.getCurrency() != null ? count(s.getRoundsAway()) + (s.getRoundsAway() == 1 ? " contract" : " contracts")
			: null;
		if (next)
		{
			String left = (s.getCurrency() == null ? amount(s, s.getLeft()) : count(s.getLeft())) + " to go";
			return when == null || compact ? left : left + ", " + when;
		}
		if (compact && when != null)
		{
			return "In " + when;
		}
		return amount(s, s.getCost()) + (when == null ? "" : ", in " + when);
	}

	/** "Mining 72 is for Dragon Slayer II, a Codex Navigator goal.", when the road ends in one. */
	static String footnote(java.util.List<RewardRoad.Stop> stops)
	{
		for (RewardRoad.Stop s : stops)
		{
			if (s.getReason() != null && !s.isOwned())
			{
				// A level that lands on a stop of another name says which stop it is.
				String level = s.getLevelName().equals(s.getName()) ? s.getName() : s.getLevelName() + " (" + s.getName() + ")";
				return level + " is for " + s.getReason() + ", a Codex Navigator goal.";
			}
		}
		return null;
	}

	static String tip(RewardRoad.Stop s)
	{
		String tip = s.getName() + ": " + status(s, false);
		if (s.getReason() != null)
		{
			tip += ". " + s.getReason() + " needs " + (s.getLevelName().equals(s.getName()) ? "it" : s.getLevelName());
		}
		else if (!s.isOwned() && !s.isAffordable() && s.getCost() != s.getLeft())
		{
			tip += ", counting the stops above it first";
		}
		return tip;
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int next = next(stops);
		float cx = D / 2f + 1;

		// The thread first, under the medallions: solid as far as the last owned stop, dotted after.
		for (int i = 0; i + 1 < stops.size(); i++)
		{
			float y0 = i * ROW + ROW / 2f + D / 2f;
			float y1 = (i + 1) * ROW + ROW / 2f - D / 2f;
			boolean walked = stops.get(i + 1).isOwned();
			g.setStroke(walked ? THREAD : THREAD_AHEAD);
			g.setColor(walked ? Theme.GOLD : Theme.alpha(Theme.GOLD_DIM, 170));
			g.draw(new Line2D.Float(cx, y0, cx, y1));
		}

		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		for (int i = 0; i < stops.size(); i++)
		{
			RewardRoad.Stop s = stops.get(i);
			int top = i * ROW + (ROW - D) / 2;
			boolean isNext = i == next;
			Shape disc = new Ellipse2D.Float(1, top, D, D);
			Color glow = s.isOwned() ? Theme.GOLD : isNext ? (s.isAffordable() ? Theme.GOLD_BRIGHT : Theme.TEAL) : Theme.STROKE;
			g.setPaint(new RadialGradientPaint(cx, top + D * 0.65f, D * 0.7f, new float[]{0, 1},
				new Color[]{Theme.mix(Theme.CARD_TOP, glow, s.isOwned() || isNext ? 0.22 : 0.06), Theme.INK_DEEP}));
			g.fill(disc);

			BufferedImage icon = icons[i];
			if (icon != null && icon.getWidth() > 1)
			{
				Graphics2D ig = (Graphics2D) g.create();
				ig.clip(disc);
				if (!s.isOwned() && !isNext)
				{
					ig.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.55f));
				}
				ig.drawImage(icon, Math.round(cx - icon.getWidth() / 2f), top + (D - icon.getHeight()) / 2, null);
				ig.dispose();
			}
			else
			{
				// No item to show: a level, or the bigger sack. Its figure, or the Pilot's mark.
				String mark = s.getLevel() > 0 ? String.valueOf(s.getLevel()) : null;
				if (mark != null)
				{
					g.setFont(Theme.title());
					FontMetrics bold = g.getFontMetrics();
					Ui.shadowed(g, mark, Math.round(cx - bold.stringWidth(mark) / 2f), top + (D + bold.getAscent()) / 2 - 1,
						isNext ? Theme.TEAL : Theme.GOLD_DIM);
					g.setFont(Theme.small());
				}
				else
				{
					Sigil.paint(g, cx - 9, top + D / 2f - 9, 18);
				}
			}

			g.setStroke(RIM);
			g.setColor(s.isOwned() ? Theme.GOLD : isNext ? Theme.alpha(glow, 200) : Theme.alpha(Theme.STROKE, 220));
			g.draw(disc);
			if (isNext && s.getCost() > 0 && !s.isAffordable())
			{
				// How much of the next stop you already hold, as an arc round its rim.
				double held = Math.max(0, Math.min(1, (s.getCost() - s.getLeft()) / (double) s.getCost()));
				if (held > 0)
				{
					g.setStroke(ARC);
					g.setColor(Theme.GOLD_BRIGHT);
					g.draw(new Arc2D.Double(1, top, D, D, 90, -360 * held, Arc2D.OPEN));
				}
			}
			if (s.isOwned())
			{
				// A small green tick on a dark seal at the medallion's foot.
				float tx = cx + D / 2f - 9;
				float ty = top + D - 9;
				g.setColor(Theme.INK_DEEP);
				g.fill(new Ellipse2D.Float(tx - 1, ty - 1, 12, 12));
				g.setStroke(TICK);
				g.setColor(Theme.GREEN);
				g.draw(new Line2D.Float(tx + 2, ty + 5, tx + 4.5f, ty + 7.5f));
				g.draw(new Line2D.Float(tx + 4.5f, ty + 7.5f, tx + 9, ty + 2.5f));
			}

			int tx = D + 10;
			int room = w - tx - 2;
			int mid = i * ROW + ROW / 2;
			g.setFont(isNext ? Theme.title() : Theme.small());
			FontMetrics nameFm = g.getFontMetrics();
			Color nameColor = s.isOwned() ? Theme.TEXT_MUTED : isNext ? Theme.GOLD_BRIGHT : Theme.TEXT;
			Ui.shadowed(g, Ui.clip(s.getName(), nameFm, room), tx, mid - 2, nameColor);
			g.setFont(Theme.small());
			Color statusColor = s.isOwned() ? Theme.GREEN : s.isAffordable() ? Theme.GOLD : isNext ? Theme.TEXT_MUTED : Theme.TEXT_FAINT;
			g.setColor(statusColor);
			String line = status(s, isNext);
			if (small.stringWidth(line) > room)
			{
				line = status(s, isNext, true);
			}
			g.drawString(Ui.clip(line, small, room), tx, mid + small.getAscent() + 1);
		}
		g.dispose();
	}
}
