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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.dhk.io.DisplayConfig;
import com.dhk.localization.Localizer;
import com.dhk.model.DhkModel;
import com.dhk.model.DisplayMode;
import com.dhk.model.FramePlacement;
import com.dhk.model.RefreshRate;
import com.dhk.model.Resolution;
import com.dhk.model.button.Button;
import com.dhk.model.button.ButtonProperties;
import com.dhk.model.button.ThemeableButton;
import com.dhk.utility.DisplayModeUtil;
import com.dhk.utility.WindowUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.ui.FlatUIUtils;

/**
 * Defines the view for Display Hot Keys. The layout for the view components is defined here. View components are
 * initialized and arranged in a GridBag layout.
 *
 * @author Jonathan R. Miller
 */
public class DhkView implements IView {

    private DhkModel model;
    private DisplayConfig displayConfig;
    private JFrame frame;
    private JPanel mainPanel;
    private JPanel displayPanel;
    private JPanel menuPanel;
    private GridBagConstraints mainPanelConstraints;
    private JLabel selectedDisplayLabel;
    private JLabel numberOfActiveSlotsLabel;
    private JLabel clearSlotHeader;
    private JSeparator slotTableSeparator;
    private JLabel applySlotHeader;
    private JLabel resolutionHeader;
    private JLabel refreshRateHeader;
    private JLabel dpiScaleHeader;
    private JLabel scalingModeHeader;
    private JLabel orientationHeader;
    private JLabel hotKeyHeader;
    private JLabel clearHotKeyHeader;
    private JLabel changeHotKeyHeader;
    private CenteredComboBox<Integer> displayIds;
    private Map<Integer, List<Slot>> displayMap;
    private Map<Integer, CenteredComboBox<Integer>> numberOfActiveSlotsMap;
    private CenteredComboBox<Integer> noDisplayIdsPlaceholder;
    private CenteredComboBox<Integer> noActiveSlotsPlaceholder;
    private Button clearAllButton;
    private ThemeableButton additionalSettingsButton;
    private ThemeableButton aboutButton;
    private ThemeableButton themeButton;
    private List<Button> buttons;
    private int shownDisplayIndex;
    private boolean startMinimizedToTray;

    /**
     * Main panel row of the first slot, which sits below the top controls, the slot table separator, and the column
     * headers.
     */
    private static final int FIRST_SLOT_GRID_Y = 3;

    /**
     * Message key of each orientation mode's name, indexed by the orientation mode a slot stores.
     */
    private static final String[] ORIENTATION_MODE_KEYS = {"orientation.landscape", "orientation.portrait",
            "orientation.landscapeFlipped", "orientation.portraitFlipped"};

    /**
     * Message key of each scaling mode's name, indexed by the scaling mode a slot stores.
     */
    private static final String[] SCALING_MODE_KEYS = {"scalingMode.preserved", "scalingMode.stretched",
            "scalingMode.centered"};

    /**
     * Message key of the slot table's resolution column header, which the display info overlay reuses as a label.
     */
    static final String RESOLUTION_HEADER_KEY = "column.resolution";

    /**
     * Message key of the slot table's refresh rate column header, which the display info overlay reuses as a label.
     */
    static final String REFRESH_RATE_HEADER_KEY = "column.refreshRate";

    /**
     * Message key of the slot table's DPI scale column header, which the display info overlay reuses as a label.
     */
    static final String DPI_SCALE_HEADER_KEY = "column.dpiScale";

    /**
     * Message key of the slot table's scaling mode column header, which the display info overlay reuses as a label.
     */
    static final String SCALING_MODE_HEADER_KEY = "column.scalingMode";

    /**
     * Message key of the slot table's orientation column header, which the display info overlay reuses as a label.
     */
    static final String ORIENTATION_HEADER_KEY = "column.orientation";

    /**
     * Constructor for the {@link DhkView} class.
     *
     * @param model
     *            - The model for the application
     */
    public DhkView(DhkModel model) {
        this.model = model;
        buttons = new ArrayList<>();

        // Disable logging for icons
        FlatSVGIcon.setLoggingEnabled(false);
    }

    @Override
    public Component getDefaultFocusComponent() {
        return selectedDisplayLabel;
    }

    /**
     * Initializes the view of the application. It creates a new frame, sets the frame properties, initializes the
     * panels, and initializes the view components.
     *
     * @param previousPlacement
     *            - The captured placement of the previous frame, or null to center on the default screen
     * @param selectedDisplayIndex
     *            - The index of the display to select in the new view; clamped to the current number of connected
     *            displays so the selection is preserved across re-initialization even if displays were added or removed
     */
    public void initView(FramePlacement previousPlacement, int selectedDisplayIndex) {
        final JFrame previousFrame = frame;

        /*
         * Clamp the requested selection to a valid display index. When displays were removed since the previous view
         * was built the requested index may now be out of range; fall back to index 0 when no displays are connected
         */
        int desiredDisplayIndex = (model.getNumOfConnectedDisplays() > 0)
                ? Math.max(0, Math.min(selectedDisplayIndex, model.getNumOfConnectedDisplays() - 1))
                : 0;

        /*
         * Decide the tray handoff before the frame is ever shown, so a frame bound for the tray is never flashed. The
         * launch follows the minimize to tray setting, but a rebuild keeps the frame off screen whenever it was already
         * away, so it comes back exactly as the user left it even if the setting was turned off from the tray
         */
        boolean previousFrameWasAway = (previousFrame != null) && !previousFrame.isShowing();
        startMinimizedToTray = (previousFrame == null) ? model.isMinimizeToTray() : previousFrameWasAway;

        // Reset view state used by component initialization
        displayConfig = model.getDisplayConfig();
        displayMap = new HashMap<>();
        numberOfActiveSlotsMap = new HashMap<>();
        shownDisplayIndex = desiredDisplayIndex;

        // Build and populate the frame
        final JFrame newFrame = new JFrame("Display Hot Keys");
        newFrame.setResizable(false);

        initPanels();
        initComponents();

        // The content scrolls when the frame is capped to a small display, and a press on empty space clears focus
        JScrollPane scrollPane = WindowUtil.setScrollingContent(newFrame, mainPanel,
                () -> getDefaultFocusComponent().requestFocusInWindow());

        frame = newFrame;
        newFrame.pack();

        /*
         * Cap the packed size to the display's working area (reserving room for any needed scroll bars) before
         * computing the location, so the frame fits on screen immediately and the location uses the final size
         */
        final Dimension workingAreaSize = WindowUtil.workingAreaSize(previousPlacement);
        WindowUtil.capToWorkingArea(newFrame, scrollPane, workingAreaSize);

        final Dimension expectedFrameSize = newFrame.getSize();
        newFrame.setLocation(WindowUtil.computeLocation(previousPlacement, expectedFrameSize));

        // Set the taskbar icon
        newFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/tray_icon.png")));

        /*
         * Make the frame visible after all components are added and the frame is packed. Showing the frame earlier can
         * cause transient artifacts (ghost copies) during repaint. A frame bound for the tray is never shown at all,
         * since showing and then hiding it flashes an unpainted window for as long as the tray takes to come up
         */
        if (!startMinimizedToTray) {
            newFrame.setVisible(true);

            // Showing alone cannot take the foreground at launch, while a rebuild must never take it from another app
            if (previousFrame == null) {
                newFrame.toFront();
            }

            // Grow the frame to absorb any pack shortfall so scroll bars do not appear when the content fits on screen
            SwingUtilities.invokeLater(() -> WindowUtil.settleScrollBars(newFrame, scrollPane, workingAreaSize));

            /*
             * Re-assert the placement after the frame is shown, in case the OS positioned it elsewhere. Both fix-ups
             * only mean something for a frame on screen, so a frame held back for the tray corrects them when restored
             */
            SwingUtilities.invokeLater(() -> WindowUtil.correctLocation(newFrame, previousPlacement));
        }

        // Dispose of the previous frame
        if (previousFrame != null) {
            previousFrame.setVisible(false);
            previousFrame.dispose();
        }

        getDefaultFocusComponent().requestFocusInWindow();
    }

    /**
     * Re-initialize the view by creating a new frame and disposing the old one. Captures the current frame's placement
     * live and reproduces it. Suitable when the display geometry is unchanged.
     */
    public void reInitView() {
        reInitView(null);
    }

    /**
     * Re-initialize the view by creating a new frame and disposing the old one. Preserves the previous frame's
     * on-screen placement and selected display, even when the display it was on has just been reconfigured.
     *
     * @param preCapturedPlacement
     *            - A frame placement captured before a display reconfiguration, or null to capture the current frame's
     *            placement live. A pre-captured placement is required after a resolution or DPI change, because the OS
     *            moves the existing frame as part of the change, so capturing here would record the wrong position.
     */
    public void reInitView(FramePlacement preCapturedPlacement) {
        FramePlacement previousPlacement = (preCapturedPlacement != null)
                ? preCapturedPlacement
                : WindowUtil.capturePlacement(frame);

        // Capture the currently selected display so it stays selected after the view is rebuilt
        int previousSelectedDisplayIndex = (displayIds != null) ? displayIds.getSelectedIndex() : 0;

        // Re-initialize the view, reproducing the previous frame's placement and selected display
        initView(previousPlacement, previousSelectedDisplayIndex);
    }

    /**
     * Shows the given display's active slots and hides its inactive slots. Hidden slots stay in the main panel, so
     * their combo boxes keep sizing the columns and showing a slot never resizes a column.
     *
     * @param displayIndex
     *            - The index of the display whose slots are in the main panel
     */
    public void showActiveSlots(int displayIndex) {
        List<Slot> slots = displayMap.get(displayIndex);
        int numOfActiveSlots = model.getNumOfSlotsForDisplay(displayIndex);

        for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
            slots.get(slotIndex).setVisible(slotIndex < numOfActiveSlots);
        }
    }

    /**
     * Shows the given display's number of active slots combo box and slots in place of the shown display's, then
     * refreshes the frame. Nothing changes when the given display is already shown.
     *
     * @param displayIndex
     *            - The index of the display to show
     */
    public void showDisplay(int displayIndex) {
        if (displayIndex == shownDisplayIndex) {
            return;
        }

        // The number of active slots combo box is the display panel's last component, so the new one is appended
        displayPanel.remove(numberOfActiveSlotsMap.get(shownDisplayIndex));
        displayPanel.add(numberOfActiveSlotsMap.get(displayIndex));

        for (Slot shownSlot : displayMap.get(shownDisplayIndex)) {
            for (Component rowComponent : shownSlot.getRowComponents()) {
                mainPanel.remove(rowComponent);
            }
        }

        addSlots(displayIndex);
        shownDisplayIndex = displayIndex;

        WindowUtil.refreshWindow(frame);
    }

    /**
     * Initializes all of the panels that will hold the view components.
     */
    private void initPanels() {
        mainPanel = new JPanel(new GridBagLayout());
        displayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        menuPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 8));

        mainPanelConstraints = new GridBagConstraints();
        mainPanelConstraints.fill = GridBagConstraints.NONE;
        mainPanelConstraints.insets = new Insets(8, 8, 8, 8);
    }

    /**
     * Initializes the components and the initial selection for each interactive component.
     */
    private void initComponents() {
        /*
         * Every label and combo box takes the size of its own content, measured by the layout once the frame is on its
         * display, so it fits exactly in every language at every display scale
         */
        noDisplayIdsPlaceholder = new CenteredComboBox<Integer>(new Integer[]{-1});
        noDisplayIdsPlaceholder.setEnabled(false);

        noActiveSlotsPlaceholder = new CenteredComboBox<Integer>(new Integer[]{0});
        noActiveSlotsPlaceholder.setEnabled(false);

        selectedDisplayLabel = new JLabel(Localizer.getText("main.selectedDisplay"), SwingConstants.LEFT);

        displayIds = model.getNumOfConnectedDisplays() > 0
                ? new CenteredComboBox<Integer>(generateDisplayIds())
                : noDisplayIdsPlaceholder;

        // Select the preserved display before the dependent components are added so the view is built for it directly
        if (model.getNumOfConnectedDisplays() > 0) {
            displayIds.setSelectedIndex(shownDisplayIndex);
        }

        numberOfActiveSlotsLabel = new JLabel(Localizer.getText("main.activeSlots"), SwingConstants.LEFT);

        if (model.getNumOfConnectedDisplays() > 0) {
            for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
                CenteredComboBox<Integer> numberOfActiveSlots = new CenteredComboBox<Integer>(
                        generateNumOfSlotsValues());
                numberOfActiveSlots.setSelectedItem(model.getNumOfSlotsForDisplay(displayIndex));

                numberOfActiveSlotsMap.put(displayIndex, numberOfActiveSlots);
            }
        } else {
            numberOfActiveSlotsMap.put(-1, noActiveSlotsPlaceholder);
        }

        ButtonProperties clearAllButtonProps = new ButtonProperties(Localizer.getText("main.clearAllSlots"),
                new Dimension(50, 28), 0.70f, 0.60f);
        clearAllButton = new Button("/clear_all_idle.svg", "/clear_all_hover.svg", clearAllButtonProps, true);

        ButtonProperties additionalSettingsButtonProps = new ButtonProperties(
                Localizer.getText("main.additionalSettings"), new Dimension(18, 40), 0.70f, 0.60f);
        additionalSettingsButton = new ThemeableButton("/additional_settings_idle.svg",
                "/additional_settings_light_hover.svg", "/additional_settings_idle.svg",
                "/additional_settings_dark_hover.svg", additionalSettingsButtonProps, true, model.isDarkMode());

        ButtonProperties aboutButtonProps = new ButtonProperties(Localizer.getText("main.about"), new Dimension(36, 40),
                0.70f, 0.60f);
        aboutButton = new ThemeableButton("/about_idle.svg", "/about_light_hover.svg", "/about_idle.svg",
                "/about_dark_hover.svg", aboutButtonProps, true, model.isDarkMode());

        ButtonProperties themeButtonProps = new ButtonProperties(Localizer.getText("main.changeTheme"),
                new Dimension(40, 40), 0.70f, 0.60f);
        themeButton = new ThemeableButton("/light_mode_idle.svg", "/light_mode_hover.svg", "/dark_mode_idle.svg",
                "/dark_mode_hover.svg", themeButtonProps, true, model.isDarkMode());

        buttons.add(clearAllButton);
        buttons.add(additionalSettingsButton);
        buttons.add(aboutButton);
        buttons.add(themeButton);

        slotTableSeparator = new JSeparator();

        // The headers above the slot buttons are empty, so those columns take the width of their buttons
        applySlotHeader = new JLabel("", SwingConstants.CENTER);
        makeLabelBold(applySlotHeader);

        // A column is as wide as its header or its widest combo box, whose width every combo box in it shares
        resolutionHeader = new JLabel(Localizer.getText(RESOLUTION_HEADER_KEY), SwingConstants.CENTER);
        makeLabelBold(resolutionHeader);

        refreshRateHeader = new JLabel(Localizer.getText(REFRESH_RATE_HEADER_KEY), SwingConstants.CENTER);
        makeLabelBold(refreshRateHeader);

        dpiScaleHeader = new JLabel(Localizer.getText(DPI_SCALE_HEADER_KEY), SwingConstants.CENTER);
        makeLabelBold(dpiScaleHeader);

        scalingModeHeader = new JLabel(Localizer.getText(SCALING_MODE_HEADER_KEY), SwingConstants.CENTER);
        makeLabelBold(scalingModeHeader);

        orientationHeader = new JLabel(Localizer.getText(ORIENTATION_HEADER_KEY), SwingConstants.CENTER);
        makeLabelBold(orientationHeader);

        hotKeyHeader = new JLabel(Localizer.getText("column.hotKey"), SwingConstants.CENTER);
        makeLabelBold(hotKeyHeader);

        clearHotKeyHeader = new JLabel("", SwingConstants.CENTER);
        makeLabelBold(clearHotKeyHeader);

        changeHotKeyHeader = new JLabel("", SwingConstants.CENTER);
        makeLabelBold(changeHotKeyHeader);

        clearSlotHeader = new JLabel("", SwingConstants.CENTER);
        makeLabelBold(clearSlotHeader);

        initSlotComponents();
        addNonSlotComponents();

        if (model.getNumOfConnectedDisplays() > 0) {
            addSlots(shownDisplayIndex);
        }
    }

    /**
     * Generates the combo box labels for the connected displays, using each display's Windows Display Settings number
     * so the labels match Windows, including the gaps Windows leaves for disconnected displays.
     *
     * @return The Windows display number of each connected display, in connected-display order
     */
    private Integer[] generateDisplayIds() {
        int[] displayNumbers = model.getDisplayNumbers();
        Integer[] displayIds = new Integer[model.getNumOfConnectedDisplays()];

        for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
            // Fall back to a sequential label when the numbers are missing or shorter than the connected count
            displayIds[displayIndex] = (displayNumbers != null && displayIndex < displayNumbers.length)
                    ? displayNumbers[displayIndex]
                    : displayIndex + 1;
        }

        return displayIds;
    }

    /**
     * Generates the array of number of active slots values.
     *
     * @return The array of number of active slots values
     */
    private Integer[] generateNumOfSlotsValues() {
        Integer[] numberOfSlots = new Integer[model.getMaxNumOfSlots()];

        for (int value = 1; value <= model.getMaxNumOfSlots(); value++) {
            numberOfSlots[value - 1] = value;
        }

        return numberOfSlots;
    }

    /**
     * Makes a specified JLabel use bold font.
     */
    private void makeLabelBold(JLabel label) {
        label.putClientProperty("FlatLaf.styleClass", "h4");
        label.putClientProperty("FlatLaf.style", "font: $h4.font");
        label.setFont(FlatUIUtils.nonUIResource(UIManager.getFont("h4.font")));
    }

    /**
     * Gets the name of each orientation mode in the selected language.
     *
     * @return The orientation mode names, indexed by the orientation mode a slot stores
     */
    static String[] getOrientationModeNames() {
        return getTexts(ORIENTATION_MODE_KEYS);
    }

    /**
     * Gets the name of each scaling mode in the selected language.
     *
     * @return The scaling mode names, indexed by the scaling mode a slot stores
     */
    static String[] getScalingModeNames() {
        return getTexts(SCALING_MODE_KEYS);
    }

    /**
     * Looks up the messages of the given keys in the selected language.
     *
     * @param keys
     *            - The message keys to look up
     *
     * @return The messages, in the same order as their keys
     */
    private static String[] getTexts(String[] keys) {
        String[] texts = new String[keys.length];

        for (int keyIndex = 0; keyIndex < keys.length; keyIndex++) {
            texts[keyIndex] = Localizer.getText(keys[keyIndex]);
        }

        return texts;
    }

    /**
     * Initializes the components for each slot.
     */
    private void initSlotComponents() {
        if (model.getNumOfConnectedDisplays() > 0) {
            String[] displayIds = model.getDisplayIds();
            String[] scalingModeNames = getScalingModeNames();
            String[] orientationModeNames = getOrientationModeNames();

            for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
                List<Slot> slots = new ArrayList<Slot>();

                /*
                 * Every combo box in a column takes the width of the column's widest combo box, shown or hidden. Each
                 * display gets its own columns, since only one display's slots are in the main panel at a time
                 */
                List<CenteredComboBox<?>> resolutionColumn = new ArrayList<>();
                List<CenteredComboBox<?>> refreshRateColumn = new ArrayList<>();
                List<CenteredComboBox<?>> dpiScaleColumn = new ArrayList<>();
                List<CenteredComboBox<?>> scalingModeColumn = new ArrayList<>();
                List<CenteredComboBox<?>> orientationColumn = new ArrayList<>();

                for (int slotIndex = 0; slotIndex < model.getMaxNumOfSlots(); slotIndex++) {
                    int slotOrientationMode = model.getSlot(displayIndex, slotIndex).getOrientationMode();
                    boolean landscapeOrientation = slotOrientationMode == 0 || slotOrientationMode == 2;
                    DisplayMode[] displayModes = landscapeOrientation
                            ? displayConfig.getLandscapeDisplayModes(displayIds[displayIndex])
                            : displayConfig.getPortraitDisplayModes(displayIds[displayIndex]);
                    DisplayMode slotDisplayMode = model.getSlot(displayIndex, slotIndex).getDisplayMode();

                    // Offer only the DPI scale percentages Windows supports for the slot's stored resolution
                    Integer[] dpiScalePercentages = displayConfig
                            .getSupportedDpiScalePercentages(slotDisplayMode.getWidth(), slotDisplayMode.getHeight());

                    /*
                     * Split the stored display mode across the resolution and refresh rate combo boxes, populating the
                     * refresh rates from the stored resolution so only its supported rates are offered
                     */
                    Resolution slotResolution = slotDisplayMode.getResolution();

                    slots.add(new Slot(slotIndex, displayIndex, displayModes, dpiScalePercentages, scalingModeNames,
                            orientationModeNames));

                    slots.get(slotIndex).getResolutions().joinColumn(resolutionColumn);
                    slots.get(slotIndex).getRefreshRates().joinColumn(refreshRateColumn);
                    slots.get(slotIndex).getDpiScalePercentages().joinColumn(dpiScaleColumn);
                    slots.get(slotIndex).getScalingModes().joinColumn(scalingModeColumn);
                    slots.get(slotIndex).getOrientationModes().joinColumn(orientationColumn);

                    slots.get(slotIndex).getResolutions().setSelectedItem(slotResolution);

                    slots.get(slotIndex)
                            .setRefreshRates(DisplayModeUtil.refreshRatesForResolution(displayModes, slotResolution));

                    slots.get(slotIndex).getRefreshRates().setSelectedItem(slotDisplayMode.getRefreshRate());

                    slots.get(slotIndex).getDpiScalePercentages()
                            .setSelectedItem(model.getSlot(displayIndex, slotIndex).getDpiScalePercentage());

                    slots.get(slotIndex).getScalingModes()
                            .setSelectedIndex(model.getSlot(displayIndex, slotIndex).getScalingMode());

                    slots.get(slotIndex).getOrientationModes()
                            .setSelectedIndex(model.getSlot(displayIndex, slotIndex).getOrientationMode());

                    slots.get(slotIndex).getHotKey()
                            .setText(model.getSlot(displayIndex, slotIndex).getHotKey().getHotKeyString());
                }

                displayMap.put(displayIndex, slots);
            }
        }
    }

    /**
     * Adds the labels and sub-panels to the main panel.
     */
    private void addNonSlotComponents() {
        displayPanel.add(selectedDisplayLabel);
        displayPanel.add(displayIds);
        displayPanel.add(numberOfActiveSlotsLabel);

        if (model.getNumOfConnectedDisplays() > 0) {
            displayPanel.add(numberOfActiveSlotsMap.get(displayIds.getSelectedIndex()));
        } else {
            displayPanel.add(numberOfActiveSlotsMap.get(displayIds.getSelectedItem()));
        }

        menuPanel.add(additionalSettingsButton);
        menuPanel.add(aboutButton);
        menuPanel.add(themeButton);

        mainPanelConstraints.anchor = GridBagConstraints.WEST;
        mainPanelConstraints.gridwidth = 11;
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 0;
        mainPanel.add(displayPanel, mainPanelConstraints);

        mainPanelConstraints.anchor = GridBagConstraints.EAST;
        mainPanel.add(menuPanel, mainPanelConstraints);

        // Separate the top controls from the slot table, relying on the spacing of the rows around it for its gap
        mainPanelConstraints.anchor = GridBagConstraints.CENTER;
        mainPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        mainPanelConstraints.insets = new Insets(0, 8, 0, 8);
        mainPanelConstraints.gridy = 1;
        mainPanel.add(slotTableSeparator, mainPanelConstraints);

        mainPanelConstraints.fill = GridBagConstraints.NONE;
        mainPanelConstraints.insets = new Insets(8, 8, 8, 8);
        mainPanelConstraints.gridwidth = 1;
        mainPanelConstraints.gridy = 2;
        mainPanel.add(clearAllButton, mainPanelConstraints);

        mainPanelConstraints.gridx = 1;
        mainPanel.add(applySlotHeader, mainPanelConstraints);

        mainPanelConstraints.gridx = 2;
        mainPanel.add(resolutionHeader, mainPanelConstraints);

        mainPanelConstraints.gridx = 3;
        mainPanel.add(refreshRateHeader, mainPanelConstraints);

        mainPanelConstraints.gridx = 4;
        mainPanel.add(dpiScaleHeader, mainPanelConstraints);

        mainPanelConstraints.gridx = 5;
        mainPanel.add(scalingModeHeader, mainPanelConstraints);

        mainPanelConstraints.gridx = 6;
        mainPanel.add(orientationHeader, mainPanelConstraints);

        mainPanelConstraints.gridx = 7;
        mainPanel.add(hotKeyHeader, mainPanelConstraints);

        mainPanelConstraints.gridx = 8;
        mainPanel.add(clearHotKeyHeader, mainPanelConstraints);

        mainPanelConstraints.gridx = 9;
        mainPanel.add(changeHotKeyHeader, mainPanelConstraints);

        mainPanelConstraints.gridx = 10;
        mainPanel.add(clearSlotHeader, mainPanelConstraints);
    }

    /**
     * Adds every slot of the given display to the main panel, one row per slot, and shows only its active slots.
     *
     * @param displayIndex
     *            - The index of the display to add the slots of
     */
    private void addSlots(int displayIndex) {
        List<Slot> slots = displayMap.get(displayIndex);

        mainPanelConstraints.anchor = GridBagConstraints.CENTER;
        mainPanelConstraints.gridwidth = 1;

        for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
            Slot slot = slots.get(slotIndex);
            List<Component> rowComponents = slot.getRowComponents();

            mainPanelConstraints.gridy = FIRST_SLOT_GRID_Y + slotIndex;

            for (int columnIndex = 0; columnIndex < rowComponents.size(); columnIndex++) {
                Component rowComponent = rowComponents.get(columnIndex);

                // The hot key outline stretches to the height of the row, so it matches the combo boxes beside it
                mainPanelConstraints.fill = (rowComponent == slot.getHotKey())
                        ? GridBagConstraints.VERTICAL
                        : GridBagConstraints.NONE;
                mainPanelConstraints.gridx = columnIndex;
                mainPanel.add(rowComponent, mainPanelConstraints);
            }
        }

        showActiveSlots(displayIndex);
    }

    /**
     * Updates the DPI scale percentages combo box for the specified slot to reflect the percentages Windows supports
     * for the slot's currently selected resolution. This is called when a new resolution is selected so the DPI Scale
     * combo box always offers a valid set of percentages for that resolution.
     *
     * @param displayIndex
     *            - The index of the display the slot resides in
     * @param slotIndex
     *            - The index of the slot to update the DPI scale percentages for
     */
    public void updateSlotDpiScalePercentages(int displayIndex, int slotIndex) {
        Resolution selectedResolution = (Resolution) getSlot(displayIndex, slotIndex).getResolutions()
                .getSelectedItem();

        if (selectedResolution == null) {
            return;
        }

        Integer[] dpiScalePercentages = displayConfig.getSupportedDpiScalePercentages(selectedResolution.getWidth(),
                selectedResolution.getHeight());

        getSlot(displayIndex, slotIndex).setDpiScalePercentages(dpiScalePercentages);
    }

    /**
     * Updates the refresh rates combo box for the specified slot to reflect the rates the slot's currently selected
     * resolution supports. This is called when a new resolution is selected so the Refresh Rate combo box always offers
     * a valid set of rates for that resolution.
     *
     * @param displayIndex
     *            - The index of the display the slot resides in
     * @param slotIndex
     *            - The index of the slot to update the refresh rates for
     */
    public void updateSlotRefreshRates(int displayIndex, int slotIndex) {
        Resolution selectedResolution = (Resolution) getSlot(displayIndex, slotIndex).getResolutions()
                .getSelectedItem();

        if (selectedResolution == null) {
            return;
        }

        RefreshRate[] refreshRates = DisplayModeUtil.refreshRatesForResolution(
                getSlot(displayIndex, slotIndex).getSupportedDisplayModes(), selectedResolution);

        getSlot(displayIndex, slotIndex).setRefreshRates(refreshRates);
    }

    /**
     * Gets the frame of the view.
     *
     * @return The frame of the view
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * Gets whether this build left the frame hidden for the tray, which means the tray still has to be brought up.
     *
     * @return True if the frame was never shown because it starts minimized to the tray, false otherwise
     */
    public boolean isStartMinimizedToTray() {
        return startMinimizedToTray;
    }

    /**
     * Gets the display IDs combo box.
     *
     * @return The combo box for the current display ID in the view
     */
    public CenteredComboBox<Integer> getDisplayIds() {
        return displayIds;
    }

    /**
     * Gets the number of active slots combo box for the given display index.
     *
     * @param displayIndex
     *            - The index of the display to get the number of active slots combo box for
     *
     * @return The number of active slots combo box for the given display index
     */
    public CenteredComboBox<Integer> getNumberOfActiveSlots(int displayIndex) {
        return numberOfActiveSlotsMap.get(displayIndex);
    }

    /**
     * Gets the clear all button.
     *
     * @return The clear all button
     */
    public Button getClearAllButton() {
        return clearAllButton;
    }

    /**
     * Gets the additional settings button.
     *
     * @return The additional settings button
     */
    public ThemeableButton getAdditionalSettingsButton() {
        return additionalSettingsButton;
    }

    /**
     * Gets the about button.
     *
     * @return The about button
     */
    public ThemeableButton getAboutButton() {
        return aboutButton;
    }

    /**
     * Gets the theme button.
     *
     * @return The theme button
     */
    public ThemeableButton getThemeButton() {
        return themeButton;
    }

    /**
     * Gets the list of buttons used by the view, including toolbar buttons and buttons from all active slots.
     *
     * @return The list of buttons used by the view
     */
    public List<Button> getButtons() {
        List<Button> allButtons = new ArrayList<Button>();

        try {
            if (buttons != null) {
                allButtons.addAll(buttons);
            }

            if (displayMap != null && model.getNumOfConnectedDisplays() > 0) {
                for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
                    List<Slot> slots = displayMap.get(displayIndex);

                    if (slots != null) {
                        int activeSlots = model.getNumOfSlotsForDisplay(displayIndex);

                        for (int slotIndex = 0; slotIndex < activeSlots; slotIndex++) {
                            List<Button> slotButtons = slots.get(slotIndex).getButtons();

                            if (slotButtons != null) {
                                allButtons.addAll(slotButtons);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return allButtons;
    }

    /**
     * Gets the specified slot.
     *
     * @param displayIndex
     *            - The index of the display to get the slot for
     * @param slotIndex
     *            - The index of the slot to get
     *
     * @return The specified slot
     */
    public Slot getSlot(int displayIndex, int slotIndex) {
        return displayMap.get(displayIndex).get(slotIndex);
    }

}