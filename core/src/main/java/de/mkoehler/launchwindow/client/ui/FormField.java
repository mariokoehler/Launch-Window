package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

import java.util.Locale;

/**
 * The design system's {@code Field}: an upper-case label (optionally with a {@link
 * HistoryNote} beside it), a text box, and an error line under the box that only appears when
 * there's an error.
 *
 * @author Mario Koehler
 */
public final class FormField extends Table {

    private final UiKit ui;
    private final TextField input;
    private final Cell<Actor> inputCell;
    private final Label error;
    private final Cell<Label> errorCell;
    private final Table labelRow;
    private final Cell<Table> labelRowCell;
    private HistoryNote historyNote;

    /**
     * Builds the field.
     *
     * @param ui          the widget kit
     * @param label       the label, upper-cased here
     * @param text        the initial text
     * @param placeholder the hint shown while the box is empty
     */
    public FormField(UiKit ui, String label, String text, String placeholder) {
        this.ui = ui;
        defaults().growX().left();
        labelRow = new Table();
        labelRow.left();
        labelRow.add(ui.label(label.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED)).left();
        labelRowCell = add(labelRow).padBottom(Theme.SPACE_1);
        row();
        input = ui.textField(text, placeholder);
        inputCell = add((Actor) input);
        row();
        error = ui.paragraph("", Theme.TextStyle.CAPTION, Theme.DANGER);
        errorCell = add((Label) null).padTop(Theme.SPACE_1);
    }

    /**
     * Adds a history note beside the label.
     *
     * @param text the note
     * @return this field
     */
    public FormField withHistoryNote(String text) {
        historyNote = new HistoryNote(ui, text);
        labelRow.add(historyNote).size(HistoryNote.ICON_SIZE).padLeft(Theme.SPACE_1);
        return this;
    }

    /**
     * Puts a control, such as a button, beside the text box, as tall as the box.
     *
     * @param action the control
     * @return this field
     */
    public FormField withAction(Actor action) {
        Table box = new Table();
        box.add(input).growX();
        box.add(action).fillY().padLeft(Theme.SPACE_2);
        inputCell.setActor(box);
        return this;
    }

    /**
     * Makes the label row at least as tall as a history-note icon, so a field without a note
     * lines its box up with a neighbouring field that has one.
     *
     * @return this field
     */
    public FormField reserveHistoryNoteSpace() {
        labelRowCell.minHeight(HistoryNote.ICON_SIZE);
        return this;
    }

    /**
     * Returns the history note, if the field has one.
     *
     * @return the note, or {@code null}
     */
    public HistoryNote historyNote() {
        return historyNote;
    }

    /**
     * Returns the text box.
     *
     * @return the text box
     */
    public TextField input() {
        return input;
    }

    /**
     * Returns the text as typed, trimmed.
     *
     * @return the text
     */
    public String text() {
        return input.getText().trim();
    }

    /**
     * Shows an error under the box and turns its border red, or clears both.
     *
     * @param message the error, or {@code null} or empty to clear it
     */
    public void setError(String message) {
        boolean hasError = message != null && !message.isEmpty();
        ui.setFieldError(input, hasError);
        if (hasError) {
            error.setText(message);
            errorCell.setActor(error);
        } else {
            errorCell.clearActor();
        }
        invalidateHierarchy();
    }

    /**
     * Returns the error currently shown.
     *
     * @return the error, or an empty string
     */
    public String error() {
        return errorCell.hasActor() ? error.getText().toString() : "";
    }
}
