/*
 * The MIT License (MIT)
 *
 * Copyright © 2026 Jonathan R. Miller
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the “Software”), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.dhk.view;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;

import com.dhk.io.DisplayConfig;
import com.dhk.io.DisplayInfoOverlay;
import com.dhk.model.ActiveDisplaySettings;
import com.dhk.model.DhkModel;
import com.dhk.model.DisplayMode;
import com.dhk.model.RefreshRate;
import com.dhk.model.Resolution;
import com.dhk.utility.DisplayModeUtil;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;

/**
 * Shows the applied settings of every connected display in a translucent panel in the top-right corner of that display.
 * Every label and value reads exactly as the slot table shows it, and the panels take the current theme's look.
 *
 * @author Jonathan R. Miller
 */
public class DisplayInfoOverlayView {

    private DisplayInfoOverlay displayInfoOverlay;

    /**
     * Whether the native layer holds the Roboto fonts, which it keeps for the life of the process, so the font files
     * are only read until then.
     */
    private static boolean robotoFontsRegistered;

    /**
     * Label of each setting line, which reuses the matching slot table column header.
     */
    private static final String[] SETTING_LABELS = {DhkView.RESOLUTION_HEADER_TEXT, DhkView.REFRESH_RATE_HEADER_TEXT,
            DhkView.DPI_SCALE_HEADER_TEXT, DhkView.SCALING_MODE_HEADER_TEXT, DhkView.ORIENTATION_HEADER_TEXT};

    /**
     * Regular and bold Roboto font files, read from beside the class that installs the font.
     */
    private static final String[] ROBOTO_FONT_FILES = {"Roboto-Regular.ttf", "Roboto-Bold.ttf"};

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
     * Time the panels stay visible after the hot key is pressed.
     */
    private static final int DISPLAY_DURATION_MS = 6000;

    /**
     * Opacity of the panels, from 0 for invisible to 255 for opaque.
     */
    private static final int OVERLAY_OPACITY = 224;

    /**
     * Size of the header line font relative to the setting line font.
     */
    private static final float HEADER_FONT_SCALE = 1.15f;

    /**
     * Scale applied to the overlay's fonts and to the spacing inside its panels, which sets the size of the overlay.
     */
    private static final float OVERLAY_SCALE = 1.3f;

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
        List<Integer> anchorPoints = new ArrayList<Integer>();

        for (int displayIndex = 0; displayIndex < displayIds.length; displayIndex++) {
            ActiveDisplaySettings settings = activeDisplaySettings[displayIndex];

            // A display that lost its visible active path since the model was built has nothing to show
            if (settings == null) {
                continue;
            }

            headers.add("Display " + displayNumber(model, displayIndex));
            values.addAll(List.of(settingValues(displayConfig, displayIds[displayIndex], settings)));

            anchorPoints.add(settings.getDesktopX());
            anchorPoints.add(settings.getDesktopY());
        }

        Font font = UIManager.getFont("Label.font");
        int fontSize = scaleToOverlaySize(font.getSize());
        int headerFontSize = Math.round(fontSize * HEADER_FONT_SCALE);
        Color background = themeColor("Panel.background", Color.WHITE);
        Color foreground = themeColor("Label.foreground", Color.BLACK);
        Color border = themeColor("Component.borderColor", foreground);

        int[] overlayColors = new int[DisplayInfoOverlay.OVERLAY_COLOR_COUNT];
        overlayColors[DisplayInfoOverlay.OVERLAY_COLOR_BACKGROUND] = background.getRGB();
        overlayColors[DisplayInfoOverlay.OVERLAY_COLOR_FOREGROUND] = foreground.getRGB();
        overlayColors[DisplayInfoOverlay.OVERLAY_COLOR_BORDER] = border.getRGB();

        int[] overlayMetrics = new int[DisplayInfoOverlay.OVERLAY_METRIC_COUNT];
        overlayMetrics[DisplayInfoOverlay.OVERLAY_METRIC_FONT_SIZE] = fontSize;
        overlayMetrics[DisplayInfoOverlay.OVERLAY_METRIC_HEADER_FONT_SIZE] = headerFontSize;
        overlayMetrics[DisplayInfoOverlay.OVERLAY_METRIC_PADDING] = scaleToOverlaySize(PADDING);
        overlayMetrics[DisplayInfoOverlay.OVERLAY_METRIC_HEADER_GAP] = scaleToOverlaySize(HEADER_GAP);
        overlayMetrics[DisplayInfoOverlay.OVERLAY_METRIC_LINE_GAP] = scaleToOverlaySize(LINE_GAP);
        overlayMetrics[DisplayInfoOverlay.OVERLAY_METRIC_COLUMN_GAP] = scaleToOverlaySize(COLUMN_GAP);

        // The distance to the work area's edges stays the same at every overlay size
        overlayMetrics[DisplayInfoOverlay.OVERLAY_METRIC_MARGIN] = MARGIN;

        byte[][] fontFiles = fontFiles(font);
        boolean accepted = displayInfoOverlay.show(headers.toArray(new String[0]), SETTING_LABELS,
                values.toArray(new String[0]), anchorPoints.stream().mapToInt(Integer::intValue).toArray(),
                overlayColors, overlayMetrics, font.getFamily(), fontFiles, OVERLAY_OPACITY, DISPLAY_DURATION_MS);

        if (accepted && fontFiles != null) {
            robotoFontsRegistered = true;
        }
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

        String dpiScale = dpiScalePercentage > 0 ? dpiScalePercentage + DhkView.DPI_SCALE_SUFFIX : UNREADABLE_VALUE;

        return new String[]{displayMode.getResolution().toString(), refreshRate, dpiScale,
                modeName(DhkView.SCALING_MODES, settings.getScalingMode()),
                modeName(DhkView.ORIENTATION_MODES, orientationMode)};
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
     * Scales a font size or spacing to the overlay's size.
     *
     * @param metric
     *            - The unscaled font size or spacing
     *
     * @return The font size or spacing at the overlay's size
     */
    private int scaleToOverlaySize(int metric) {
        return Math.round(metric * OVERLAY_SCALE);
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

    /**
     * Returns the regular and bold font files for a font the system does not have installed, so the native layer can
     * draw the panels in it.
     *
     * @param font
     *            - The font the panels are drawn in
     *
     * @return The font files, or null when the font is not bundled or the native layer already registered it
     */
    private byte[][] fontFiles(Font font) {
        if (robotoFontsRegistered || !FlatRobotoFont.FAMILY.equals(font.getFamily())) {
            return null;
        }

        byte[][] fontFiles = new byte[ROBOTO_FONT_FILES.length][];

        for (int fileIndex = 0; fileIndex < ROBOTO_FONT_FILES.length; fileIndex++) {
            try (InputStream fontStream = FlatRobotoFont.class.getResourceAsStream(ROBOTO_FONT_FILES[fileIndex])) {
                if (fontStream == null) {
                    return null;
                }

                fontFiles[fileIndex] = fontStream.readAllBytes();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        return fontFiles;
    }

}
