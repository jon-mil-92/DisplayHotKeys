/*
 * Display Hot Keys
 * Copyright © 2024-2026 Jonathan R. Miller
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of version 3 of the GNU
 * General Public License as published by the Free Software Foundation, together with the additional terms under section
 * 7 of that License in ADDITIONAL_TERMS.txt.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License and the additional terms along with this program in
 * LICENSE.txt and ADDITIONAL_TERMS.txt. If not, see <https://www.gnu.org/licenses/>.
 */
package com.dhk.view;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;

import com.dhk.io.DisplayConfig;
import com.dhk.io.DisplayInfoOverlay;
import com.dhk.localization.Localizer;
import com.dhk.model.ActiveDisplaySettings;
import com.dhk.model.DhkModel;
import com.dhk.model.DisplayMode;
import com.dhk.model.RefreshRate;
import com.dhk.model.Resolution;
import com.dhk.utility.DisplayModeUtil;

/**
 * Shows the applied settings of every connected display in a panel at the chosen position and opacity on that display.
 * Every label and value reads exactly as the slot table shows it, and the panels take the current theme's look.
 *
 * @author Jonathan R. Miller
 */
public class DisplayInfoOverlayView {

    private DisplayInfoOverlay displayInfoOverlay;

    /**
     * Message key of each setting line's label, which reuses the matching slot table column header.
     */
    private static final String[] SETTING_LABEL_KEYS = {DhkView.RESOLUTION_HEADER_KEY, DhkView.REFRESH_RATE_HEADER_KEY,
            DhkView.DPI_SCALE_HEADER_KEY, DhkView.SCALING_MODE_HEADER_KEY, DhkView.ORIENTATION_HEADER_KEY};

    /**
     * Text shown for a setting that could not be read.
     */
    private static final String UNREADABLE_VALUE = "-";

    /**
     * Largest difference at which an applied refresh rate matches one the slot table lists, since the rational Windows
     * reports can differ slightly from the table's.
     */
    private static final double REFRESH_RATE_MATCH_TOLERANCE_HZ = 0.01;

    /**
     * Size of the header line font relative to the setting line font.
     */
    private static final float HEADER_FONT_SCALE = 1.15f;

    /**
     * Unscaled space between a panel's edges and its text.
     */
    private static final int PADDING = 12;

    /**
     * Unscaled space on each side of the separator beneath the header line.
     */
    private static final int HEADER_GAP = 6;

    /**
     * Unscaled space between consecutive setting lines.
     */
    private static final int LINE_GAP = 4;

    /**
     * Unscaled space between the label column and the value column.
     */
    private static final int COLUMN_GAP = 16;

    /**
     * Unscaled space between a panel and the edges of its display's work area.
     */
    private static final int MARGIN = 16;

    /**
     * Default constructor for the {@link DisplayInfoOverlayView} class.
     */
    public DisplayInfoOverlayView() {
        displayInfoOverlay = new DisplayInfoOverlay();
    }

    /**
     * Shows a panel of the applied settings on every connected display, replacing any panels already showing. The theme
     * is read on every call, so the panels always match the current theme.
     *
     * @param model
     *            - The model holding the connected displays
     */
    public void show(DhkModel model) {
        String[] displayIds = model.getDisplayIds();
        DisplayConfig displayConfig = model.getDisplayConfig();

        if (displayIds == null || displayConfig == null) {
            return;
        }

        ActiveDisplaySettings[] activeDisplaySettings = displayConfig.getActiveDisplaySettings(displayIds);
        List<String> headers = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        List<Integer> displayBounds = new ArrayList<Integer>();

        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            ActiveDisplaySettings settings = activeDisplaySettings[displayIndex];

            // A display that lost its visible active path since the model was built has nothing to show
            if (settings == null) {
                continue;
            }

            headers.add(Localizer.format("overlay.display", displayNumber(model, displayIndex)));
            values.addAll(List.of(settingValues(displayConfig, displayIds[displayIndex], settings)));

            // A panel shows only while its display keeps these bounds, so it never lands on a display that moved
            displayBounds.addAll(List.of(settings.getDesktopX(), settings.getDesktopY(),
                    settings.getDisplayMode().getWidth(), settings.getDisplayMode().getHeight()));
        }

        // The chosen scale sizes the fonts and the spacing inside the panels, which sets the size of the overlay
        float overlayScale = model.getInfoOverlayScale() / 100f;
        Font font = UIManager.getFont("Label.font");
        float fontSize = font.getSize() * overlayScale;
        Color background = themeColor("Panel.background", Color.WHITE);
        Color foreground = themeColor("Label.foreground", Color.BLACK);
        Color border = themeColor("Component.borderColor", foreground);

        int[] overlayColors = new int[DisplayInfoOverlay.OVERLAY_COLOR_COUNT];
        overlayColors[DisplayInfoOverlay.OVERLAY_COLOR_BACKGROUND] = background.getRGB();
        overlayColors[DisplayInfoOverlay.OVERLAY_COLOR_FOREGROUND] = foreground.getRGB();
        overlayColors[DisplayInfoOverlay.OVERLAY_COLOR_BORDER] = border.getRGB();

        /*
         * The metrics stay fractional, so each is rounded only once, after it is scaled for its display. Rounding here
         * as well would make some scale steps grow the panels by more than others
         */
        float[] overlayMetrics = new float[DisplayInfoOverlay.OVERLAY_METRIC_COUNT];
        overlayMetrics[DisplayInfoOverlay.OVERLAY_METRIC_FONT_SIZE] = fontSize;
        overlayMetrics[DisplayInfoOverlay.OVERLAY_METRIC_HEADER_FONT_SIZE] = fontSize * HEADER_FONT_SCALE;
        overlayMetrics[DisplayInfoOverlay.OVERLAY_METRIC_PADDING] = PADDING * overlayScale;
        overlayMetrics[DisplayInfoOverlay.OVERLAY_METRIC_HEADER_GAP] = HEADER_GAP * overlayScale;
        overlayMetrics[DisplayInfoOverlay.OVERLAY_METRIC_LINE_GAP] = LINE_GAP * overlayScale;
        overlayMetrics[DisplayInfoOverlay.OVERLAY_METRIC_COLUMN_GAP] = COLUMN_GAP * overlayScale;

        // The distance to the work area's edges stays the same at every overlay size
        overlayMetrics[DisplayInfoOverlay.OVERLAY_METRIC_MARGIN] = MARGIN;

        int overlayOpacity = Math.round(model.getInfoOverlayOpacity() * DisplayInfoOverlay.MAX_OVERLAY_OPACITY / 100f);

        displayInfoOverlay.show(headers.toArray(new String[0]), Localizer.getTexts(SETTING_LABEL_KEYS),
                values.toArray(new String[0]), displayBounds.stream().mapToInt(Integer::intValue).toArray(),
                model.getInfoOverlayPosition(), overlayColors, overlayMetrics, font.getFamily(), overlayOpacity,
                model.getInfoOverlayDuration() * 1000);
    }

    /**
     * Hides every panel that is showing, since a display configuration change leaves their placement and values stale.
     */
    public void hide() {
        displayInfoOverlay.hide();
    }

    /**
     * Formats a display's applied settings exactly as the slot table shows them, by building the same item each slot
     * combo box holds and taking its text. The values are in the same order as the setting labels.
     *
     * @param displayConfig
     *            - The current display configuration, holding the display modes the slot table lists
     * @param displayId
     *            - The ID of the display the settings belong to
     * @param settings
     *            - The settings in effect on the display
     *
     * @return The text of each applied setting, in the same order as the setting labels
     */
    private String[] settingValues(DisplayConfig displayConfig, String displayId, ActiveDisplaySettings settings) {
        DisplayMode displayMode = settings.getDisplayMode();
        int orientationMode = settings.getOrientationMode();
        int dpiScalePercentage = settings.getDpiScalePercentage();
        String refreshRate = refreshRateText(displayConfig, displayId, orientationMode, displayMode);

        String dpiScale = dpiScalePercentage > 0
                ? dpiScalePercentage + Localizer.getText("dpiScale.suffix")
                : UNREADABLE_VALUE;

        return new String[]{displayMode.getResolution().toString(), refreshRate, dpiScale,
                modeName(DhkView.getScalingModeNames(), settings.getScalingMode()),
                modeName(DhkView.getOrientationModeNames(), orientationMode)};
    }

    /**
     * Formats an applied refresh rate as the slot table shows it. The rate is matched to the slot table's own rate for
     * the resolution, so a rational Windows reports slightly differently still reads identically.
     *
     * @param displayConfig
     *            - The current display configuration, holding the display modes the slot table lists
     * @param displayId
     *            - The ID of the display the refresh rate belongs to
     * @param orientationMode
     *            - The applied orientation mode, which selects the landscape or portrait display modes
     * @param displayMode
     *            - The applied display mode, holding the resolution and the exact refresh rate
     *
     * @return The text of the applied refresh rate
     */
    private String refreshRateText(DisplayConfig displayConfig, String displayId, int orientationMode,
            DisplayMode displayMode) {
        if (displayMode.getRefreshDenominator() == 0) {
            return UNREADABLE_VALUE;
        }

        Resolution resolution = displayMode.getResolution();
        RefreshRate appliedRefreshRate = displayMode.getRefreshRate();

        // The slot table lists the display modes of the slot's orientation, so the same choice is made here
        boolean landscapeOrientation = orientationMode == 0 || orientationMode == 2;
        DisplayMode[] displayModes = landscapeOrientation
                ? displayConfig.getLandscapeDisplayModes(displayId)
                : displayConfig.getPortraitDisplayModes(displayId);

        if (displayModes == null) {
            return appliedRefreshRate.toString();
        }

        RefreshRate matchingRefreshRate = appliedRefreshRate;
        double smallestDifference = REFRESH_RATE_MATCH_TOLERANCE_HZ;

        for (RefreshRate listedRefreshRate : DisplayModeUtil.refreshRatesForResolution(displayModes, resolution)) {
            double difference = Math.abs(listedRefreshRate.getRefreshRateHz() - appliedRefreshRate.getRefreshRateHz());

            if (difference <= smallestDifference) {
                smallestDifference = difference;
                matchingRefreshRate = listedRefreshRate;
            }
        }

        return matchingRefreshRate.toString();
    }

    /**
     * Gets the name of a mode, or the unreadable value when the mode has no name.
     *
     * @param modeNames
     *            - The name of each mode, indexed by mode
     * @param mode
     *            - The mode to name
     *
     * @return The name of the mode
     */
    private String modeName(String[] modeNames, int mode) {
        return mode >= 0 && mode < modeNames.length ? modeNames[mode] : UNREADABLE_VALUE;
    }

    /**
     * Gets the Windows Display Settings number of a connected display, falling back to a sequential number when the
     * numbers are missing, the same way the display selector labels the display.
     *
     * @param model
     *            - The model holding the connected displays
     * @param displayIndex
     *            - The index of the display to number
     *
     * @return The number of the display
     */
    private int displayNumber(DhkModel model, int displayIndex) {
        int[] displayNumbers = model.getDisplayNumbers();

        return displayNumbers != null && displayIndex < displayNumbers.length
                ? displayNumbers[displayIndex]
                : displayIndex + 1;
    }

    /**
     * Returns a color of the current theme, or the fallback color when the theme lacks it.
     *
     * @param colorKey
     *            - The key of the theme color
     * @param fallbackColor
     *            - The color to use when the theme lacks the key
     *
     * @return The theme color
     */
    private Color themeColor(String colorKey, Color fallbackColor) {
        Color color = UIManager.getColor(colorKey);

        return color != null ? color : fallbackColor;
    }

}
