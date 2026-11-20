package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A screen's entrance, played once ({@link Juice}, {@code design.md} &sect;5, Backlog, J2): one clock
 * that every part of it reads, so a rebuild in the middle (an answer from the server) picks the
 * entrance up where it was instead of starting it again. A click anywhere finishes it at once.
 * Not playing (motion off, or not the first sight), every part shows its end.
 *
 * @author Mario Koehler
 */
public final class Reveal {

    /** How long a card takes to fade in, in seconds. Unconfirmed. */
    public static final float FADE_SECONDS = 0.25f;

    private static final Pattern NUMBER = Pattern.compile("\\d[\\d,]*(?:\\.(\\d+))?");

    private final double start;
    private boolean skipped;
    private final Set<String> fired = new HashSet<>();

    private Reveal(boolean play) {
        start = play && Juice.motion() ? Juice.now() : Double.NEGATIVE_INFINITY;
        skipped = !(play && Juice.motion());
    }

    /**
     * Returns an entrance that plays from now (if motion is on).
     *
     * @return the entrance
     */
    public static Reveal play() {
        return new Reveal(true);
    }

    /**
     * Returns an entrance already over: every part shows its end.
     *
     * @return the entrance
     */
    public static Reveal none() {
        return new Reveal(false);
    }

    /**
     * Finishes the entrance at once.
     */
    public void skip() {
        skipped = true;
    }

    /**
     * Returns a capture listener that finishes the entrance on any click, for the screen's stage.
     *
     * @return the listener
     */
    public InputListener skipOnClick() {
        return new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                skip();
                return false;
            }
        };
    }

    /**
     * Returns how far a part has played, linearly: 0 before its delay, 1 once done or skipped.
     *
     * @param delay   when the part starts, in seconds from the entrance's start
     * @param seconds how long it takes
     * @return the progress, 0 to 1
     */
    public float progress(float delay, float seconds) {
        if (skipped || !Juice.motion()) {
            return 1f;
        }
        return (float) Math.min(1.0, Math.max(0.0, (Juice.now() - start - delay) / seconds));
    }

    /**
     * Returns true once, the first time it's asked after a moment of the entrance has passed, for a
     * sound on a beat; never once skipped (a skipped entrance makes no sound).
     *
     * @param key   the beat's key
     * @param delay the moment, in seconds from the start
     * @return whether the beat has just come
     */
    public boolean beat(String key, float delay) {
        if (skipped || !Juice.motion() || Juice.now() - start < delay) {
            return false;
        }
        return fired.add(key);
    }

    /**
     * Fades an actor in from its delay. The action stays on the actor, harmless once done.
     *
     * @param actor the actor
     * @param delay when it starts to fade in
     * @param <A>   the actor's type
     * @return the actor
     */
    public <A extends Actor> A fadeIn(A actor, float delay) {
        actor.getColor().a = Interpolation.pow2Out.apply(progress(delay, FADE_SECONDS));
        actor.addAction(new Action() {
            @Override
            public boolean act(float delta) {
                getActor().getColor().a = Interpolation.pow2Out.apply(progress(delay, FADE_SECONDS));
                return false;
            }
        });
        return actor;
    }

    /**
     * Counts a label's first number up from zero to its value, keeping its decimals and thousands
     * commas; text without a number stays as it is.
     *
     * @param label   the label, showing its final text
     * @param delay   when it starts
     * @param seconds how long the count takes
     * @return the label
     */
    public Label countUp(Label label, float delay, float seconds) {
        String text = label.getText().toString();
        Matcher matcher = NUMBER.matcher(text);
        if (!matcher.find()) {
            return label;
        }
        String number = matcher.group();
        int decimals = matcher.group(1) == null ? 0 : matcher.group(1).length();
        boolean commas = number.contains(",");
        double value = Double.parseDouble(number.replace(",", ""));
        String before = text.substring(0, matcher.start());
        String after = text.substring(matcher.end());
        Action count = new Action() {
            @Override
            public boolean act(float delta) {
                float progress = Interpolation.pow2Out.apply(progress(delay, seconds));
                String shown = progress >= 1f ? text
                    : before + format(value * progress, decimals, commas) + after;
                Label target = (Label) getActor();
                if (!target.textEquals(shown)) {
                    target.setText(shown);
                }
                return false;
            }
        };
        count.setActor(label);
        count.act(0f);
        label.addAction(count);
        return label;
    }

    private static String format(double value, int decimals, boolean commas) {
        String pattern = "%" + (commas ? "," : "") + "." + decimals + "f";
        return String.format(Locale.ROOT, pattern, value);
    }
}
