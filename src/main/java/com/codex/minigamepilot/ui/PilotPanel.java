package com.codex.minigamepilot.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import net.runelite.client.ui.PluginPanel;

/**
 * The Pilot side panel. The helm at the top always holds the one next action. Under it, the
 * loop with your place in it, the minigame's gauges, a checklist where there is one, and the
 * session so far. Away from the minigames it lists the three it flies and what is waiting at each.
 */
public class PilotPanel extends PluginPanel
{
	static final int WIDTH = 200;

	private final Helm helm = new Helm();
	private final StackPanel body = new StackPanel(8, 8);
	private View view = View.loggedOut();

	public PilotPanel()
	{
		super(false);
		setLayout(new BorderLayout());
		setBackground(Theme.INK);
		add(helm, BorderLayout.NORTH);

		JScrollPane scroll = new JScrollPane(body);
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		add(scroll, BorderLayout.CENTER);
		rebuild();
	}

	/** Called on the Swing thread whenever anything shown changes. */
	public void update(View view)
	{
		this.view = view;
		rebuild();
	}

	private void rebuild()
	{
		View v = view;
		String idle = !v.loggedIn ? "Log in to begin" : "Head to a minigame and the Pilot takes over";
		helm.update(v.activity.getTitle(), v.place, v.step, idle);
		body.removeAll();

		if (v.loop != null && v.loop.length > 0)
		{
			Card loop = new Card(new BorderLayout());
			loop.setBorder(new javax.swing.border.EmptyBorder(6, 4, 4, 4));
			loop.add(new LoopStrip(v.loop, v.loopAt), BorderLayout.CENTER);
			body.add(loop);
		}

		if (!v.gauges.isEmpty())
		{
			body.add(new SectionHeader(v.activity == View.Activity.FURNACE ? "The furnace" : v.activity == View.Activity.HOMES ? "Materials" : "The sack"));
			Card card = new Card(new BorderLayout());
			StackPanel gauges = new StackPanel(6, 0);
			for (View.Gauge gauge : v.gauges)
			{
				gauges.add(new GaugeBar(gauge));
			}
			card.add(gauges, BorderLayout.CENTER);
			body.add(card);
		}

		if (!v.lines.isEmpty())
		{
			body.add(new SectionHeader(v.linesTitle == null ? "To do" : v.linesTitle, v.linesNote));
			Card card = new Card(new BorderLayout());
			StackPanel lines = new StackPanel(2, 0);
			for (View.Line line : v.lines)
			{
				lines.add(new LineRow(line));
			}
			card.add(lines, BorderLayout.CENTER);
			body.add(card);
		}

		if (!v.stats.isEmpty())
		{
			body.add(new SectionHeader("This session"));
			JPanel grid = new JPanel(new GridLayout(0, 2, 6, 6));
			grid.setOpaque(false);
			for (View.Stat s : v.stats)
			{
				grid.add(new StatTile(s.value, s.label, s.note, s.tone));
			}
			body.add(grid);
		}

		if (!v.briefs.isEmpty())
		{
			body.add(new SectionHeader("Minigames"));
			for (View.Brief b : v.briefs)
			{
				Card card = new Card(new BorderLayout(0, 2));
				card.accent(Theme.GOLD_DIM);
				card.add(Ui.label(b.activity.getTitle(), Theme.title(), Theme.GOLD), BorderLayout.NORTH);
				StackPanel text = new StackPanel(2, 0);
				if (b.status != null)
				{
					text.add(Ui.wrapped(b.status, Theme.small(), Theme.TEXT, WIDTH - 30));
				}
				if (b.how != null)
				{
					text.add(Ui.wrapped(b.how, Theme.small(), Theme.TEXT_MUTED, WIDTH - 30));
				}
				card.add(text, BorderLayout.CENTER);
				body.add(card);
			}
		}

		body.revalidate();
		body.repaint();
		revalidate();
		repaint();
	}
}
