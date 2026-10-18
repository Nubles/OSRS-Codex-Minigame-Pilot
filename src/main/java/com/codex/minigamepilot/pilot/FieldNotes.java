package com.codex.minigamepilot.pilot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;

/**
 * Field notes: an optional, local record of what the Pilot saw and decided, one JSON object a
 * line, written to .runelite/codex-minigame-pilot. It exists so the guidance can be checked
 * against a real session: which objects and varbits the game showed, which step was chosen,
 * where a target could not be found, and how each round went. Nothing is sent anywhere.
 */
@Slf4j
public final class FieldNotes
{
	/** A notes file stops growing past this, so a forgotten setting cannot fill a disk. */
	private static final long LIMIT = 25L * 1024 * 1024;

	private final File dir;
	private final Gson gson;
	private final ExecutorService writer = Executors.newSingleThreadExecutor(r ->
	{
		Thread t = new Thread(r, "codex-minigame-pilot-notes");
		t.setDaemon(true);
		return t;
	});
	private BufferedWriter out;
	private File file;
	private long written;
	private volatile boolean active;

	public FieldNotes(File dir, Gson gson)
	{
		this.dir = dir;
		this.gson = gson;
	}

	/** Opens a new notes file. Safe to call when already open. */
	public synchronized void open(String version)
	{
		if (active)
		{
			return;
		}
		active = true;
		String name = "field-notes-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".jsonl";
		File target = new File(dir, name);
		writer.execute(() ->
		{
			try
			{
				Files.createDirectories(dir.toPath());
				BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
					Files.newOutputStream(target.toPath(), StandardOpenOption.CREATE, StandardOpenOption.APPEND), StandardCharsets.UTF_8));
				synchronized (this)
				{
					out = w;
					file = target;
					written = 0;
				}
			}
			catch (IOException e)
			{
				log.warn("Could not open field notes at {}", target, e);
			}
		});
		JsonObject start = new JsonObject();
		start.addProperty("plugin", "codex-minigame-pilot");
		start.addProperty("version", version);
		start.addProperty("java", System.getProperty("java.version"));
		note("start", start);
	}

	public boolean isOpen()
	{
		return active;
	}

	/** The file being written, once it is open, or null. */
	public synchronized File file()
	{
		return file;
	}

	/** Adds a line. The time and the kind of note lead every line. */
	public void note(String kind, JsonObject body)
	{
		if (!active)
		{
			return;
		}
		JsonObject line = new JsonObject();
		line.addProperty("ms", System.currentTimeMillis());
		line.addProperty("k", kind);
		for (String key : body.keySet())
		{
			line.add(key, body.get(key));
		}
		String text = gson.toJson(line);
		writer.execute(() -> write(text));
	}

	/** Turns an object with plain fields, such as a planner's snapshot, into a note body. */
	public JsonObject tree(Object o)
	{
		return o == null ? new JsonObject() : gson.toJsonTree(o).getAsJsonObject();
	}

	/** Finishes the file. Safe to call when already closed. */
	public void close()
	{
		active = false;
		writer.execute(() ->
		{
			synchronized (this)
			{
				if (out == null)
				{
					return;
				}
				try
				{
					out.close();
				}
				catch (IOException e)
				{
					log.warn("Could not close field notes", e);
				}
				out = null;
				file = null;
			}
		});
	}

	public void shutdown()
	{
		close();
		writer.shutdown();
	}

	private synchronized void write(String text)
	{
		if (out == null || written > LIMIT)
		{
			return;
		}
		try
		{
			out.write(text);
			out.newLine();
			out.flush();
			written += text.length() + 1;
		}
		catch (IOException e)
		{
			log.warn("Could not write field notes", e);
		}
	}
}
