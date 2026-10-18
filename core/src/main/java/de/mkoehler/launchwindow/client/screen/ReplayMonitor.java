package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.video.VideoPlayer;
import de.mkoehler.launchwindow.client.ui.HistoryNote;
import de.mkoehler.launchwindow.client.ui.HistoryNotes;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.client.view.ReplayClips;
import de.mkoehler.launchwindow.client.view.ReplayConsole;

import java.util.Locale;

/**
 * The replay's console ({@code ui-spec.md} &sect;4.7.3, "The console", from the canvas
 * {@code Launch Window — Replay Console}, 2026-10-13): a navy mission-control console filling its
 * space. On the left the step's clip plays in a 16:9 picture under a label strip, with the plan's
 * steps as lamps below it; on the right a column of instruments: the mission clock, the status
 * gauge with its CAUTION and MASTER ALARM lamps, and three telemetry bars, all read from a
 * {@link ReplayConsole}. With no video player, or a clip that won't load, it shows the clip's still
 * (the no-signal picture), says "No signal" and the instruments go dead. Lives as long as its
 * screen, which calls {@link #update()} and {@link #setTime} every frame and {@link #dispose()} at
 * the end. Everything is drawn from flat shapes, so it stays sharp at any size.
 *
 * @author Mario Koehler
 */
public final class ReplayMonitor extends WidgetGroup {

    /** The picture's shape. */
    private static final float ASPECT = 16f / 9f;
    /** The label strip on top. */
    private static final float STRIP = 26f;
    /** The lighter lip around the picture. */
    private static final float LIP = 4f;
    /** The margin at the console's left, and between the picture and the column. */
    private static final float EDGE = 8f;
    /** The margin at the console's right and bottom. */
    private static final float MARGIN = 12f;
    /** The step-lamp row under the picture. */
    private static final float FOOT = 53f;
    /** The instrument column's least width. */
    private static final float COLUMN = 280f;
    private static final float PANEL_GAP = 10f;
    private static final float CLOCK_HEIGHT = 62f;
    private static final float STATUS_HEIGHT = 210f;
    private static final float DIAL_RADIUS = 104f;
    private static final float LAMP_HEIGHT = 18f;
    private static final float LAMP_PITCH = 22f;
    private static final float BAR_HEIGHT = 8f;
    private static final float BAR_PITCH = 26f;

    private static final Color CLEAR = new Color(0f, 0f, 0f, 0f);
    private static final Color LIP_COLOR = mix(Theme.NAVY, Theme.SURFACE_CARD, 0.12f);
    /** A panel: white at 5 % over the navy, with a white-at-18 % line. */
    private static final Color PANEL = mix(Theme.NAVY, Color.WHITE, 0.05f);
    private static final Color PANEL_LINE = mix(Theme.NAVY, Color.WHITE, 0.18f);
    private static final Color TITLE = mix(PANEL, Theme.ON_PAGE_MUTED, 0.8f);
    private static final Color TEXT_OFF = mix(PANEL, Theme.ON_PAGE_MUTED, 0.45f);
    private static final Color LEGEND = mix(PANEL, Theme.ON_PAGE_MUTED, 0.7f);
    private static final Color TICK_LONG = mix(PANEL, Theme.ON_PAGE_MUTED, 0.8f);
    private static final Color TICK_SHORT = mix(PANEL, Theme.ON_PAGE_MUTED, 0.5f);
    private static final Color TRACK = mix(PANEL, Color.WHITE, 0.06f);
    private static final Color MARK = mix(PANEL, Theme.ON_PAGE_MUTED, 0.6f);
    private static final Color NEEDLE_DEAD = mix(PANEL, Theme.ON_FILL, 0.5f);
    private static final Color CLOCK_HELD = mix(PANEL, Theme.ON_FILL, 0.55f);
    /** The gauge's ranges: Nominal, Warning, Critical; the success green itself is too dark on navy. */
    private static final Color[] RANGES = {Theme.SUCCESS_ON_PAGE, Theme.GOLD, Theme.DANGER};
    private static final Color[] RANGE_IDLE = new Color[3];
    private static final Color[] RANGE_LIT = new Color[3];
    private static final Color[] RANGE_DEAD = new Color[3];
    private static final Color[] SQUARE_DEAD = new Color[3];
    private static final Color SIGNAL_RING = mix(Theme.NAVY, Theme.GOLD_ON_PAGE, 0.4f);
    private static final Color SIGNAL_DEAD = mix(Theme.NAVY, Color.WHITE, 0.12f);
    private static final Color STEP_FLOWN = mix(Theme.NAVY, Theme.ON_FILL, 0.85f);
    private static final Color STEP_RING = mix(Theme.NAVY, Theme.GOLD, 0.6f);
    private static final Color STEP_AHEAD = mix(Theme.NAVY, Color.WHITE, 0.18f);
    private static final Color ROW_TITLE = mix(Theme.NAVY, Theme.ON_PAGE_MUTED, 0.8f);
    private static final Color ROW_MISSION = mix(Theme.NAVY, Theme.ON_PAGE_MUTED, 0.7f);

    static {
        for (int i = 0; i < RANGES.length; i++) {
            RANGE_IDLE[i] = mix(PANEL, RANGES[i], 0.55f);
            RANGE_LIT[i] = mix(PANEL, RANGES[i], 0.95f);
            RANGE_DEAD[i] = mix(PANEL, RANGES[i], 0.25f);
            SQUARE_DEAD[i] = mix(PANEL, RANGES[i], 0.4f);
        }
    }

    private final UiKit ui;
    private final VideoPlayer player;
    private final Label label;
    private final Label pausedLabel;
    private final Label loopLabel;
    private final HistoryNote quindar;
    private final Label stepTitle;
    private final Label stepCount;
    private final Label missionLabel;
    private final Label clockTitle;
    private final Label clock;
    private final Label hold;
    private final Label statusTitle;
    private final Label[] legend = new Label[3];
    private final Label caution;
    private final Label alarm;
    private final Label telemetryTitle;
    private final Label[] barNames = new Label[ReplayConsole.BARS.length];
    private Texture still;
    private String stillPath;
    private boolean video;
    private ReplayConsole console = ReplayConsole.noSignal();
    private float time;
    private boolean playing = true;
    private String clockText = "";
    private int step;
    private int planned;
    private float px;
    private float py;
    private float pw;
    private float ph;
    private float colX;
    private float colW;

    /**
     * Builds the console.
     *
     * @param ui     the widget kit
     * @param player the video player, or {@code null} for stills only
     */
    public ReplayMonitor(UiKit ui, VideoPlayer player) {
        this.ui = ui;
        this.player = player;
        label = add(ui.label("", Theme.TextStyle.EYEBROW, Theme.ON_FILL));
        pausedLabel = add(ui.label("PAUSED · NEXT STEP WAITS", Theme.TextStyle.EYEBROW, Theme.GOLD_ON_PAGE));
        // The radio loop the calls come over, with the story of their beeps (user, 2026-10-18).
        loopLabel = add(ui.label("AIR-TO-GROUND", Theme.TextStyle.EYEBROW, TITLE));
        quindar = new HistoryNote(ui, HistoryNotes.QUINDAR);
        quindar.setColor(Theme.ON_FILL);
        addActor(quindar);
        stepTitle = add(ui.label("STEP", Theme.TextStyle.LABEL, ROW_TITLE));
        stepCount = add(ui.label("", Theme.TextStyle.LABEL, Theme.ON_FILL));
        missionLabel = add(ui.label("", Theme.TextStyle.LABEL, ROW_MISSION));
        clockTitle = add(ui.label("MISSION CLOCK", Theme.TextStyle.LABEL, TITLE));
        clock = add(ui.label("", Theme.TextStyle.HEADING_LG, Color.WHITE));
        hold = add(ui.label("HOLD", Theme.TextStyle.LABEL, Color.WHITE));
        statusTitle = add(ui.label("STATUS", Theme.TextStyle.LABEL, TITLE));
        String[] ranges = {"NOMINAL", "WARNING", "CRITICAL"};
        for (int i = 0; i < legend.length; i++) {
            legend[i] = add(ui.label(ranges[i], Theme.TextStyle.LABEL, Color.WHITE));
        }
        caution = add(ui.label("CAUTION", Theme.TextStyle.LABEL, Color.WHITE));
        alarm = add(ui.label("MASTER ALARM", Theme.TextStyle.LABEL, Color.WHITE));
        telemetryTitle = add(ui.label("TELEMETRY", Theme.TextStyle.LABEL, TITLE));
        for (int i = 0; i < barNames.length; i++) {
            barNames[i] = add(ui.label(ReplayConsole.BARS[i], Theme.TextStyle.LABEL, TITLE));
        }
    }

    private Label add(Label child) {
        addActor(child);
        return child;
    }

    /**
     * Plays a step's clip from its start.
     *
     * @param clip       the clip
     * @param step       the step's name, for the label
     * @param instrument the step's instruments while the camera is live
     */
    public void play(ReplayClips.Clip clip, String step, ReplayConsole instrument) {
        video = false;
        if (player != null) {
            try {
                video = player.load(Gdx.files.internal(clip.video()));
                if (video) {
                    player.setLooping(clip.loop());
                    player.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    player.play();
                }
            } catch (Exception e) {
                Gdx.app.error("Replay", "can't play " + clip.video(), e);
                video = false;
            }
        }
        boolean live = video && clip.signal();
        label.setText(live ? "CAM 1 · " + step.toUpperCase(Locale.ROOT) : "NO SIGNAL");
        console = live ? instrument : ReplayConsole.noSignal();
        time = 0f;
        if (!video) {
            showStill(clip.still() != null ? clip.still() : ReplayClips.noSignal().still());
        }
        invalidate();
    }

    /**
     * Returns the step's instruments as the console shows them: dead while there's no signal.
     *
     * @return the instruments
     */
    public ReplayConsole console() {
        return console;
    }

    /**
     * Sets the step's place in the plan and the mission, for the lamps under the picture.
     *
     * @param index   the step, from 0
     * @param count   the steps the plan has
     * @param mission the mission's name
     */
    public void setPlan(int index, int count, String mission) {
        step = index;
        planned = count;
        stepCount.setText((index + 1) + " OF " + count);
        missionLabel.setText(mission.toUpperCase(Locale.ROOT));
        invalidate();
    }

    /**
     * Moves the instruments to a time; call once a frame.
     *
     * @param seconds      how far into the step it is
     * @param running      whether the replay moves on by itself (paused: HOLD)
     * @param missionClock the mission clock, in seconds
     */
    public void setTime(float seconds, boolean running, float missionClock) {
        time = seconds;
        playing = running;
        String text = ReplayConsole.clock(missionClock);
        if (!text.equals(clockText)) {
            clockText = text;
            clock.setText(text);
        }
    }

    /**
     * Stops the clip, for the season summary.
     */
    public void stop() {
        if (player != null && video) {
            player.stop();
        }
        video = false;
    }

    /**
     * Decodes the clip's next frame, if it's time; call once a frame.
     */
    public void update() {
        if (player != null && video) {
            player.update();
        }
    }

    @Override
    public void layout() {
        float w = getWidth();
        float h = getHeight();
        pw = Math.max(0f, Math.min(w - 3 * EDGE - 2 * LIP - MARGIN - COLUMN, (h - STRIP - 2 * LIP - FOOT) * ASPECT));
        ph = pw / ASPECT;
        px = EDGE + LIP;
        py = h - STRIP - LIP - ph;
        colX = px + pw + LIP + EDGE;
        colW = w - MARGIN - colX;

        float strip = h - STRIP / 2f - 3f;
        place(label, EDGE + 24f, strip);
        quindar.setPosition(Math.round(w - MARGIN - HistoryNote.ICON_SIZE), Math.round(strip - HistoryNote.ICON_SIZE / 2f));
        loopLabel.pack();
        float loopX = quindar.getX() - Theme.SPACE_1 - loopLabel.getWidth();
        place(loopLabel, loopX, strip);
        pausedLabel.pack();
        place(pausedLabel, loopX - 2 * Theme.SPACE_3 - pausedLabel.getWidth(), strip);
        pausedLabel.setVisible(!playing);

        float row = py - LIP - 15f;
        place(stepTitle, px, row);
        missionLabel.pack();
        place(missionLabel, px + pw - missionLabel.getWidth(), row);
        place(stepCount, lampX(planned - 1) + 20f, row);

        float clockTop = h - STRIP;
        place(clockTitle, colX + 10f, clockTop - 12f);
        place(clock, colX + 10f, clockTop - 39f);
        hold.pack();
        place(hold, colX + colW - 32f - hold.getWidth() / 2f, clockTop - 16f);

        float statusTop = clockTop - CLOCK_HEIGHT - PANEL_GAP;
        place(statusTitle, colX + 10f, statusTop - 12f);
        float spacing = (colW - 28f) / 3f;
        for (int i = 0; i < legend.length; i++) {
            place(legend[i], colX + 22f + i * spacing + 12f, statusTop - 162f);
        }
        float lampW = (colW - 30f) / 2f;
        caution.pack();
        alarm.pack();
        place(caution, colX + 10f + (lampW - caution.getWidth()) / 2f, statusTop - 189f);
        place(alarm, colX + 20f + lampW + (lampW - alarm.getWidth()) / 2f, statusTop - 189f);

        // A console too short for the gauge leaves it out (drawStatus), its labels with it.
        boolean statusFits = statusTop - STATUS_HEIGHT >= MARGIN - 1f;
        statusTitle.setVisible(statusFits);
        caution.setVisible(statusFits);
        alarm.setVisible(statusFits);
        for (Label range : legend) {
            range.setVisible(statusFits);
        }

        float telemetryTop = statusTop - STATUS_HEIGHT - PANEL_GAP;
        place(telemetryTitle, colX + 10f, telemetryTop - 12f);
        for (int i = 0; i < barNames.length; i++) {
            place(barNames[i], colX + 10f, telemetryTop - 38f - i * BAR_PITCH);
            barNames[i].setVisible(barFits(i));
        }
        telemetryTitle.setVisible(telemetryTop - MARGIN > 40f);
    }

    /** Places a label by its left edge and its vertical centre. */
    private static void place(Label child, float x, float centreY) {
        child.pack();
        child.setPosition(Math.round(x), Math.round(centreY - child.getHeight() / 2f));
    }

    private float lampX(int index) {
        float room = pw - 56f - missionLabel.getPrefWidth() - 70f;
        float pitch = planned <= 1 ? LAMP_PITCH : Math.min(LAMP_PITCH, room / (planned - 1));
        return px + 46f + index * pitch;
    }

    private float telemetryTop() {
        return getHeight() - STRIP - CLOCK_HEIGHT - PANEL_GAP - STATUS_HEIGHT - PANEL_GAP;
    }

    private boolean barFits(int bar) {
        return telemetryTop() - 34f - bar * BAR_PITCH - BAR_HEIGHT >= MARGIN + 6f;
    }

    @Override
    public float getPrefWidth() {
        return 0f;
    }

    @Override
    public float getPrefHeight() {
        return 0f;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        validate();
        Color tint = batch.getColor().cpy();
        float x = getX();
        float y = getY();
        float h = getHeight();
        batch.setColor(Color.WHITE);
        ui.solid(Theme.NAVY).draw(batch, x, y, getWidth(), h);
        drawPicture(batch, x, y);
        drawStrip(batch, x, y, h);
        drawLamps(batch, x, y);
        float clockTop = y + h - STRIP;
        drawClock(batch, x, clockTop);
        float statusTop = clockTop - CLOCK_HEIGHT - PANEL_GAP;
        drawStatus(batch, x, statusTop);
        drawTelemetry(batch, x, y, statusTop - STATUS_HEIGHT - PANEL_GAP);
        batch.setColor(tint);
        super.draw(batch, parentAlpha);
    }

    private void drawPicture(Batch batch, float x, float y) {
        ui.solid(LIP_COLOR).draw(batch, x + px - LIP, y + py - LIP, pw + 2 * LIP, ph + 2 * LIP);
        ui.solid(Theme.SURFACE_CARD).draw(batch, x + px, y + py, pw, ph);
        batch.setColor(Color.WHITE);
        Texture frame = video && player != null ? player.getTexture() : null;
        if (frame != null && player.getVideoWidth() > 0) {
            batch.draw(frame, x + px, y + py, pw, ph, 0, 0, player.getVideoWidth(), player.getVideoHeight(), false, false);
        } else if (still != null) {
            batch.draw(still, x + px, y + py, pw, ph);
        }
    }

    private void drawStrip(Batch batch, float x, float y, float h) {
        float cx = x + EDGE + 10f;
        float cy = y + h - 16f;
        if (console.live()) {
            disc(batch, cx, cy, 17, CLEAR, SIGNAL_RING, 1f);
            disc(batch, cx, cy, 10, Theme.GOLD_ON_PAGE, null, 0f);
        } else {
            disc(batch, cx, cy, 10, SIGNAL_DEAD, null, 0f);
        }
        pausedLabel.setVisible(!playing);
    }

    private void drawLamps(Batch batch, float x, float y) {
        float cy = y + py - LIP - 15f;
        for (int i = 0; i < planned; i++) {
            float cx = x + lampX(i);
            if (i < step) {
                disc(batch, cx, cy, 11, STEP_FLOWN, null, 0f);
            } else if (i == step) {
                disc(batch, cx, cy, 18, CLEAR, STEP_RING, 1.5f);
                disc(batch, cx, cy, 11, Theme.GOLD, null, 0f);
            } else {
                disc(batch, cx, cy, 11, CLEAR, STEP_AHEAD, 1.5f);
            }
        }
    }

    private void drawClock(Batch batch, float x, float top) {
        panel(batch, x + colX, top - CLOCK_HEIGHT, colW, CLOCK_HEIGHT);
        clock.setColor(playing ? Theme.ON_FILL : CLOCK_HELD);
        lamp(batch, hold, x + colX + colW - 54f, top - 7f - LAMP_HEIGHT, 44f, !playing, Theme.GOLD, Theme.NAVY);
    }

    private void drawStatus(Batch batch, float x, float top) {
        if (top - STATUS_HEIGHT < getY() + MARGIN - 1f) {
            return;
        }
        panel(batch, x + colX, top - STATUS_HEIGHT, colW, STATUS_HEIGHT);
        float cx = x + colX + colW / 2f;
        float cy = top - 140f;
        boolean live = console.live();
        ReplayConsole.Range active = console.activeRange(time);
        float[][] spans = {{0.004f, ReplayConsole.NOMINAL_END - 0.004f},
            {ReplayConsole.NOMINAL_END + 0.004f, ReplayConsole.WARNING_END - 0.004f},
            {ReplayConsole.WARNING_END + 0.004f, 0.996f}};
        for (int i = 0; i < spans.length; i++) {
            boolean lit = active != null && active.ordinal() == i;
            Color color = !live ? RANGE_DEAD[i] : lit ? RANGE_LIT[i] : RANGE_IDLE[i];
            arc(batch, cx, cy, DIAL_RADIUS, spans[i][0], spans[i][1], lit ? 10f : 8f, color);
            legend[i].setColor(lit ? Theme.ON_FILL : LEGEND);
        }
        for (int i = 0; i <= 20; i++) {
            boolean big = i % 5 == 0;
            float angle = angle(i / 20f);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float inner = big ? 83f : 90f;
            line(batch, cx + 97f * cos, cy + 97f * sin, cx + inner * cos, cy + inner * sin, big ? 2f : 1f,
                big ? TICK_LONG : TICK_SHORT, 0f);
        }
        float peak = console.peak(time);
        if (peak >= 0f) {
            float angle = angle(peak);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            line(batch, cx + 76f * cos, cy + 76f * sin, cx + 108f * cos, cy + 108f * sin, 2.5f, Theme.GOLD, 0f);
        }
        float angle = angle(console.needle(time));
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        line(batch, cx - 16f * cos, cy - 16f * sin, cx + 96f * cos, cy + 96f * sin, 3f,
            live ? Theme.ON_FILL : NEEDLE_DEAD, 0f);
        disc(batch, cx, cy, 14, Theme.NAVY, Theme.ON_FILL, 2f);
        float spacing = (colW - 28f) / 3f;
        for (int i = 0; i < RANGES.length; i++) {
            ui.solid(live ? RANGES[i] : SQUARE_DEAD[i]).draw(batch, x + colX + 22f + i * spacing, top - 166f, 8f, 8f);
        }
        float lampW = (colW - 30f) / 2f;
        lamp(batch, caution, x + colX + 10f, top - 180f - LAMP_HEIGHT, lampW, console.caution(time), Theme.GOLD,
            Theme.NAVY);
        lamp(batch, alarm, x + colX + 20f + lampW, top - 180f - LAMP_HEIGHT, lampW, console.alarm(time), Theme.DANGER,
            Color.WHITE);
    }

    private void drawTelemetry(Batch batch, float x, float y, float top) {
        float bottom = y + MARGIN;
        if (top - bottom < 40f) {
            return;
        }
        panel(batch, x + colX, bottom, colW, top - bottom);
        float barX = x + colX + 46f;
        float barW = colW - 64f;
        for (int i = 0; i < ReplayConsole.BARS.length; i++) {
            if (!barFits(i)) {
                continue;
            }
            float barY = top - 34f - i * BAR_PITCH - BAR_HEIGHT;
            ReplayConsole.BarState state = console.barState(i, time);
            switch (state) {
                case DEAD -> dashed(batch, barX, barY, barW, BAR_HEIGHT);
                case LOST -> ui.rect(CLEAR, Theme.DANGER, 1f).draw(batch, barX, barY, barW, BAR_HEIGHT);
                default -> ui.rect(TRACK, PANEL_LINE, 1f).draw(batch, barX, barY, barW, BAR_HEIGHT);
            }
            for (float mark : new float[]{ReplayConsole.BAR_LOW_MARK, ReplayConsole.BAR_HIGH_MARK}) {
                ui.solid(MARK).draw(batch, Math.round(barX + mark * barW), barY - 3f, 1f, BAR_HEIGHT + 6f);
            }
            float value = console.bar(i, time);
            if (value > 0f && state != ReplayConsole.BarState.DEAD) {
                Color fill = switch (state) {
                    case WARNING -> Theme.GOLD;
                    case CRITICAL, LOST -> Theme.DANGER;
                    default -> Theme.ON_FILL;
                };
                ui.solid(fill).draw(batch, barX, barY, value * barW, BAR_HEIGHT);
            }
        }
    }

    private void panel(Batch batch, float x, float y, float w, float h) {
        ui.rect(PANEL, PANEL_LINE, 1f).draw(batch, x, y, w, h);
    }

    private void lamp(Batch batch, Label text, float x, float y, float w, boolean lit, Color fill, Color ink) {
        if (lit) {
            ui.solid(fill).draw(batch, x, y, w, LAMP_HEIGHT);
        } else {
            ui.rect(CLEAR, PANEL_LINE, 1f).draw(batch, x, y, w, LAMP_HEIGHT);
        }
        text.setColor(lit ? ink : TEXT_OFF);
    }

    private void dashed(Batch batch, float x, float y, float w, float h) {
        Drawable dash = ui.solid(PANEL_LINE);
        for (float at = 0f; at < w; at += 8f) {
            float length = Math.min(4f, w - at);
            dash.draw(batch, x + at, y, length, 1f);
            dash.draw(batch, x + at, y + h - 1f, length, 1f);
        }
        dash.draw(batch, x, y, 1f, h);
        dash.draw(batch, x + w - 1f, y, 1f, h);
    }

    /** Draws a lamp: a circle built at twice its size, so a thin ring stays round when drawn small. */
    private void disc(Batch batch, float cx, float cy, int diameter, Color fill, Color ring, float ringWidth) {
        ui.circle(fill, ring, 2 * ringWidth, 2 * diameter).draw(batch, cx - diameter / 2f, cy - diameter / 2f,
            diameter, diameter);
    }

    /** The gauge's angle for a reading: 0 on the left, 1 on the right, over the top. */
    private static float angle(float value) {
        return (float) Math.PI * (1f - value);
    }

    private void arc(Batch batch, float cx, float cy, float radius, float from, float to, float width, Color color) {
        int segments = Math.max(2, Math.round((to - from) * 100f));
        float previous = angle(from);
        for (int i = 1; i <= segments; i++) {
            float next = angle(from + (to - from) * i / segments);
            line(batch, cx + radius * (float) Math.cos(previous), cy + radius * (float) Math.sin(previous),
                cx + radius * (float) Math.cos(next), cy + radius * (float) Math.sin(next), width, color, 0.5f);
            previous = next;
        }
    }

    /**
     * Draws an anti-aliased line with the brush; {@code extend} lengthens it at both ends, so an
     * arc's segments overlap. Colours must be opaque, or the overlaps show.
     */
    private void line(Batch batch, float x1, float y1, float x2, float y2, float width, Color color, float extend) {
        int texels = Math.round(width) + 2;
        float thick = width * texels / (texels - 2f);
        TextureRegion brush = ui.brush(texels);
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.hypot(dx, dy);
        if (length == 0f) {
            return;
        }
        float ux = dx / length;
        float uy = dy / length;
        float startX = x1 - ux * extend;
        float startY = y1 - uy * extend;
        batch.setColor(color);
        batch.draw(brush, startX, startY - thick / 2f, 0f, thick / 2f, length + 2 * extend, thick, 1f, 1f,
            (float) Math.toDegrees(Math.atan2(dy, dx)));
        // Drawables multiply by the batch's colour: leave it white for them.
        batch.setColor(Color.WHITE);
    }

    private static Color mix(Color base, Color over, float amount) {
        Color color = new Color(base).lerp(over, amount);
        color.a = 1f;
        return color;
    }

    private void showStill(String path) {
        if (path.equals(stillPath)) {
            return;
        }
        disposeStill();
        stillPath = path;
        try {
            still = new Texture(Gdx.files.internal(path));
            still.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (RuntimeException e) {
            Gdx.app.error("Replay", "no still " + path, e);
            still = null;
        }
    }

    private void disposeStill() {
        if (still != null) {
            still.dispose();
            still = null;
        }
        stillPath = null;
    }

    /**
     * Frees the player and the still.
     */
    public void dispose() {
        if (player != null) {
            player.dispose();
        }
        disposeStill();
    }
}
