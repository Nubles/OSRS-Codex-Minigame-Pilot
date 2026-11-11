package com.codex.minigamepilot.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.codex.minigamepilot.core.Step;
import com.codex.minigamepilot.overlay.StepPlaque;
import java.awt.BorderLayout;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import org.junit.Test;

/**
 * RuneLite's overlay renderer reads the clip back after every overlay ({@code getClip().equals(clip)}):
 * a clip cleared with {@code setClip(null)} breaks it every frame. The step plaque returns the
 * clip, transform, stroke, font, composite and hints it was given as they were, and the panel's
 * cards their clip and transform.
 */
public class GraphicsStateTest
{
	private static final class State
	{
		final Shape clip;
		final AffineTransform transform;
		final Stroke stroke;
		final Font font;
		final Composite composite;
		final RenderingHints hints;

		State(Graphics2D g)
		{
			clip = g.getClip();
			transform = g.getTransform();
			stroke = g.getStroke();
			font = g.getFont();
			composite = g.getComposite();
			hints = (RenderingHints) g.getRenderingHints().clone();
		}

		void same(Graphics2D g)
		{
			assertNotNull("the clip was cleared", g.getClip());
			assertEquals(clip, g.getClip());
			assertEquals(transform, g.getTransform());
			assertEquals(stroke, g.getStroke());
			assertEquals(font, g.getFont());
			assertEquals(composite, g.getComposite());
			assertEquals(hints, g.getRenderingHints());
		}
	}

	private static Graphics2D canvas(BufferedImage img)
	{
		Graphics2D g = img.createGraphics();
		g.setClip(new Rectangle(5, 5, 300, 120));
		g.translate(3, 4);
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		return g;
	}

	@Test
	public void theStepPlaqueLeavesTheGraphicsAsItFoundThem()
	{
		BufferedImage img = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = canvas(img);
		State before = new State(g);
		StepPlaque plaque = new StepPlaque();
		plaque.show(Step.of("Deposit", "Deposit your pay-dirt in the hopper", "27 pay-dirt in, the sack fills", Step.Target.HOPPER));
		assertNotNull(plaque.render(g));
		before.same(g);
		plaque.show(Step.waiting("Wait at the bar dispenser", "The steel bars are smelting", Step.Target.DISPENSER));
		plaque.render(g);
		before.same(g);
		g.dispose();
	}

	@Test
	public void aCardWithAnAccentPutsTheClipBack()
	{
		Card card = new Card(new BorderLayout());
		card.accent(Theme.EMBER);
		card.setSize(180, 60);
		BufferedImage img = new BufferedImage(200, 80, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = canvas(img);
		Shape clip = g.getClip();
		AffineTransform transform = g.getTransform();
		// Swing sets the component's font on the graphics it paints with, so only the clip and transform are ours to keep.
		card.paint(g);
		assertNotNull("the clip was cleared", g.getClip());
		assertEquals(clip, g.getClip());
		assertEquals(transform, g.getTransform());
		// The accent is drawn inside the clip it was given: nothing left of it.
		BufferedImage blank = new BufferedImage(200, 80, BufferedImage.TYPE_INT_ARGB);
		Graphics2D b = blank.createGraphics();
		b.setClip(new Rectangle(20, 0, 100, 80));
		card.paint(b);
		for (int y = 0; y < 60; y++)
		{
			for (int x = 0; x < 20; x++)
			{
				assertEquals("painted outside the clip at " + x + "," + y, 0, blank.getRGB(x, y) >>> 24);
			}
		}
		b.dispose();
		g.dispose();
	}
}
