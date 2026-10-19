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
 * loop with your place in it, the minigame's gauges, a checklist where there is one, the pace
 * coach's verdict on the last round, and the session so far. Away from the minigames it lists the three it flies and what is waiting at each.
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

		if (!v.goals.isEmpty())
		{
			body.add(new SectionHeader(v.goals.size() == 1 ? "Goal" : "Goals", v.goals.size() == 1 ? v.goals.get(0).unit : null));
			for (View.GoalCard goal : v.goals)
			{
				body.add(goalCard(goal, v.goals.size() > 1));
			}
		}

		if (v.pick != null && !v.pick.picks.isEmpty())
		{
			body.add(new SectionHeader(v.pick.title, v.pick.trailing));
			Card card = new Card(new BorderLayout(0, 4));
			StackPanel rows = new StackPanel(2, 0);
			for (View.Pick p : v.pick.picks)
			{
				rows.add(new PickRow(p));
			}
			card.add(rows, BorderLayout.CENTER);
			card.add(Ui.wrapped(v.pick.note, Theme.small(), Theme.TEXT_FAINT, WIDTH - 30), BorderLayout.SOUTH);
			body.add(card);
		}

		if (v.supply != null && !v.supply.lines.isEmpty())
		{
			body.add(new SectionHeader("In the bank", v.supply.trailing));
			Card card = new Card(new BorderLayout());
			card.accent(v.supply.warn ? Theme.EMBER : Theme.TEAL);
			StackPanel stack = new StackPanel(4, 0);
			StackPanel lines = new StackPanel(1, 0);
			for (View.Line line : v.supply.lines)
			{
				lines.add(new LineRow(line));
			}
			stack.add(lines);
			if (v.supply.headline != null)
			{
				stack.add(Ui.wrapped(v.supply.headline, Theme.small(), Theme.TEXT, WIDTH - 30));
			}
			if (v.supply.detail != null)
			{
				stack.add(Ui.wrapped(v.supply.detail, Theme.small(), v.supply.warn ? Theme.EMBER : Theme.TEXT_MUTED, WIDTH - 30));
			}
			if (v.supply.note != null)
			{
				stack.add(Ui.wrapped(v.supply.note, Theme.small(), Theme.TEXT_FAINT, WIDTH - 30));
			}
			card.add(stack, BorderLayout.CENTER);
			body.add(card);
		}

		if (v.pace != null)
		{
			body.add(new SectionHeader("Pace"));
			Card card = new Card(new BorderLayout());
			card.accent(v.pace.isBest ? Theme.GOLD_BRIGHT : v.pace.costly ? Theme.EMBER : Theme.GREEN);
			StackPanel stack = new StackPanel(4, 0);
			stack.add(new PaceDial(v.pace));
			stack.add(Ui.wrapped(v.pace.advice, Theme.small(), v.pace.costly ? Theme.EMBER : Theme.GREEN, WIDTH - 30));
			if (v.pace.splits.size() > 1)
			{
				stack.add(new StageStrip(v.pace.splits));
				if (v.pace.splitNote != null)
				{
					stack.add(Ui.wrapped(v.pace.splitNote, Theme.small(), Theme.mix(Theme.TEXT_MUTED, Theme.EMBER, 0.5), WIDTH - 30));
				}
			}
			stack.add(Ui.wrapped(v.pace.tail, Theme.small(), Theme.TEXT_FAINT, WIDTH - 30));
			card.add(stack, BorderLayout.CENTER);
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

	/** A goal: how far along it is, how far off in rounds and time, and its pieces ticked off. */
	private static Card goalCard(View.GoalCard goal, boolean named)
	{
		Card card = new Card(new BorderLayout());
		card.accent(goal.done ? Theme.GREEN : Theme.GOLD);
		StackPanel stack = new StackPanel(4, 0);
		if (named)
		{
			stack.add(Ui.label(goal.activity.getTitle(), Theme.small(), Theme.GOLD_DIM));
		}
		int have = (int) Math.min(Integer.MAX_VALUE, goal.have);
		int need = (int) Math.min(Integer.MAX_VALUE, goal.need);
		stack.add(new GaugeBar(new View.Gauge(goal.title, have, 0, need, null, goal.done ? Theme.GREEN : Theme.progressColor(need == 0 ? 1 : have / (double) need))));
		stack.add(Ui.wrapped(goal.note, Theme.small(), goal.done ? Theme.GREEN : Theme.TEXT_MUTED, WIDTH - 30));
		if (goal.aside != null)
		{
			stack.add(Ui.wrapped(goal.aside, Theme.small(), Theme.TEAL, WIDTH - 30));
		}
		if (!goal.pieces.isEmpty())
		{
			StackPanel lines = new StackPanel(1, 0);
			for (View.Line line : goal.pieces)
			{
				lines.add(new LineRow(line));
			}
			stack.add(lines);
		}
		card.add(stack, BorderLayout.CENTER);
		return card;
	}
}
