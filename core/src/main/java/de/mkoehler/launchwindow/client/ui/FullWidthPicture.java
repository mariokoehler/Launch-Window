package de.mkoehler.launchwindow.client.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;

/**
 * A picture as wide as its cell and as tall as its own proportions then need, so it's never cropped
 * or stretched. Its preferred height follows the width the cell gives it, so the table lays out a
 * second time once the width is known.
 *
 * @author Mario Koehler
 */
public final class FullWidthPicture extends Image {

    private final float aspect;

    /**
     * Wraps a picture.
     *
     * @param picture the picture
     */
    public FullWidthPicture(TextureRegionDrawable picture) {
        super(picture, Scaling.fit);
        aspect = picture.getRegion().getRegionHeight() / (float) picture.getRegion().getRegionWidth();
    }

    @Override
    public float getPrefWidth() {
        return 0f;
    }

    @Override
    public float getPrefHeight() {
        return getWidth() * aspect;
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();
        if (Math.abs(getHeight() - getPrefHeight()) > 0.5f) {
            invalidateHierarchy();
        }
    }
}
