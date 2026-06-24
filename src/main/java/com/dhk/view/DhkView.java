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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.dhk.io.DisplayConfig;
import com.dhk.model.DhkModel;
import com.dhk.model.FramePlacement;
import com.dhk.model.button.Button;
import com.dhk.model.button.ButtonProperties;
import com.dhk.model.button.ThemeableButton;
import com.dhk.model.button.ThemeableToggleButton;
import com.dhk.utility.FrameUtil;
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
    private JFrame frame;
    private JPanel mainPanel;
    private JPanel displayPanel;
    private JPanel menuPanel;
    private GridBagLayout mainPanelLayout;
    private FlowLayout displayPanelLayout;
    private FlowLayout menuPanelLayout;
    private GridBagConstraints mainPanelConstraints;
    private GridBagConstraints displayPanelConstraints;
    private GridBagConstraints menuPanelConstraints;
    private JLabel selectedDisplayLabel;
    private JLabel numberOfActiveSlotsLabel;
    private JLabel applyDisplayModeHeader;
    private JLabel displayModeHeader;
    private JLabel scalingModeHeader;
    private JLabel dpiScaleHeader;
    private JLabel orientationHeader;
    private JLabel hotKeyHeader;
    private JLabel clearHotKeyHeader;
    private JLabel changeHotKeyHeader;
    private JComboBox<Integer> displayIds;
    private Map<Integer, List<Slot>> displayMap;
    private Map<Integer, JComboBox<Integer>> numberOfActiveSlotsMap;
    private JComboBox<Integer> noDisplayIdsPlaceholder;
    private JComboBox<Integer> noActiveSlotsPlaceholder;
    private Button clearAllButton;
    private ThemeableButton refreshAppButton;
    private ThemeableButton aboutButton;
    private ThemeableButton themeButton;
    private ThemeableToggleButton minimizeToTrayButton;
    private ThemeableToggleButton runOnStartupButton;
    private List<Button> buttons;
    private int previouslySelectedDisplayIndex;
    private int gridYPosForSlotInPanel;

    private static final int NUM_OF_SLOT_COMPONENTS = 9;
    private static final String[] ORIENTATION_MODES = {"Landscape", "Portrait", "iLandscape", "iPortrait"};
    private static final String[] SCALING_MODES = new String[]{"Preserved", "Stretched", "Centered"};
    private static final Integer[] DPI_SCALE_PERCENTAGES = new Integer[]{100, 125, 150, 175, 200, 225, 250, 300, 350};

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

        // The starting Y-coordinate for slots
        gridYPosForSlotInPanel = 2;
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

        // Reset view state used by component initialization
        displayMap = new HashMap<>();
        numberOfActiveSlotsMap = new HashMap<>();
        previouslySelectedDisplayIndex = desiredDisplayIndex;
        gridYPosForSlotInPanel = 2;

        // Build and populate the frame
        final JFrame newFrame = new JFrame("Display Hot Keys");
        newFrame.setResizable(false);

        initPanels();
        initComponents();

        // Scroll the content so every component stays reachable when the frame is capped to a small display; the
        // borderless, as-needed scroll bars are invisible at normal resolutions where the whole frame fits
        JScrollPane scrollPane = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        newFrame.setContentPane(scrollPane);
        newFrame.pack();

        // Cap the packed size to the display's working area (reserving room for any needed scroll bars) before
        // computing the location, so the frame fits on screen immediately and the location uses the final size
        Dimension fittedFrameSize = FrameUtil.fitToWorkingArea(newFrame.getSize(), scrollPane,
                FrameUtil.workingAreaSize(previousPlacement));

        if (!fittedFrameSize.equals(newFrame.getSize())) {
            newFrame.setSize(fittedFrameSize);
        }

        final Dimension expectedFrameSize = newFrame.getSize();
        newFrame.setLocation(FrameUtil.computeLocation(previousPlacement, expectedFrameSize));

        // Set the taskbar icon
        newFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/tray_icon.png")));

        newFrame.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mousePressedEvent) {
                getDefaultFocusComponent().requestFocusInWindow();
            }
        });

        /*
         * Make the frame visible after all components are added and the frame is packed. Showing the frame earlier can
         * cause transient artifacts (ghost copies) during repaint
         */
        newFrame.setVisible(true);

        // Re-assert the placement after the frame is shown, in case the OS positioned it elsewhere
        SwingUtilities.invokeLater(() -> FrameUtil.correctLocation(newFrame, previousPlacement));

        // Dispose of the previous frame
        if (previousFrame != null) {
            previousFrame.setVisible(false);
            previousFrame.dispose();
        }

        frame = newFrame;

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
                : FrameUtil.capturePlacement(frame);

        // Capture the currently selected display so it stays selected after the view is rebuilt
        int previousSelectedDisplayIndex = (displayIds != null) ? displayIds.getSelectedIndex() : 0;

        // Re-initialize the view, reproducing the previous frame's placement and selected display
        initView(previousPlacement, previousSelectedDisplayIndex);
    }

    /**
     * Adds slots to the main panel until the number of active slots for the given display is reached.
     *
     * @param displayIndex
     *            - The index of the display to add slots in the view for
     * @param startIndex
     *            - The index in which to start adding slots to the view
     */
    public void pushSlots(int displayIndex, int startIndex) {
        if (model.getNumOfConnectedDisplays() > 0) {
            for (int slotIndex = startIndex; slotIndex < model.getNumOfSlotsForDisplay(displayIndex); slotIndex++) {
                mainPanelConstraints.anchor = GridBagConstraints.CENTER;
                mainPanelConstraints.gridwidth = 1;
                mainPanelConstraints.gridx = 0;
                mainPanelConstraints.gridy = gridYPosForSlotInPanel;
                mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getIndicatorLabel(), mainPanelConstraints);

                mainPanelConstraints.gridx = 1;
                mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getApplyDisplayModeButton(),
                        mainPanelConstraints);

                mainPanelConstraints.gridx = 2;
                mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getDisplayModes(), mainPanelConstraints);

                mainPanelConstraints.gridx = 3;
                mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getScalingModes(), mainPanelConstraints);

                mainPanelConstraints.gridx = 4;
                mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getDpiScalePercentages(),
                        mainPanelConstraints);

                mainPanelConstraints.gridx = 5;
                mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getOrientationModes(), mainPanelConstraints);

                mainPanelConstraints.gridx = 6;
                mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getHotKey(), mainPanelConstraints);

                mainPanelConstraints.gridx = 7;
                mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getClearHotKeyButton(), mainPanelConstraints);

                mainPanelConstraints.gridx = 8;
                mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getChangeHotKeyButton(),
                        mainPanelConstraints);

                // Add the next slot to the following row in the layout
                gridYPosForSlotInPanel++;
            }
        }
    }

    /**
     * Removes the specified number of slots from the end of the main panel.
     *
     * @param numOfSlotsToRemove
     *            - The number of slots to remove from the end of the main panel
     */
    public void popSlots(int numOfSlotsToRemove) {
        int lastSlotComponentIndex = mainPanel.getComponentCount() - 1;

        for (int slotIndex = 0; slotIndex < numOfSlotsToRemove; slotIndex++) {
            for (int componentIndex = 0; componentIndex < NUM_OF_SLOT_COMPONENTS; componentIndex++) {
                mainPanel.remove(lastSlotComponentIndex);

                lastSlotComponentIndex--;
            }
        }

        // Update the Y-coordinate of the next slot to add
        gridYPosForSlotInPanel -= numOfSlotsToRemove;
    }

    /**
     * Replaces the active slots from the previously selected display with the active slots for the newly selected
     * display.
     */
    public void replaceActiveSlots() {
        int numOfPrevActiveSlotsToReplace = model.getNumOfSlotsForDisplay(previouslySelectedDisplayIndex);
        int displayIndex = displayIds.getSelectedIndex();

        for (int slotIndex = 0; slotIndex < numOfPrevActiveSlotsToReplace; slotIndex++) {
            GridBagConstraints prevIndicatorLabelConstraints = mainPanelLayout
                    .getConstraints(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getIndicatorLabel());

            mainPanel.remove(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getIndicatorLabel());
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getIndicatorLabel(),
                    prevIndicatorLabelConstraints);

            GridBagConstraints prevApplyDisplayModeButtonConstraints = mainPanelLayout.getConstraints(
                    displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getApplyDisplayModeButton());

            mainPanel.remove(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getApplyDisplayModeButton());
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getApplyDisplayModeButton(),
                    prevApplyDisplayModeButtonConstraints);

            GridBagConstraints prevDisplayModesConstraints = mainPanelLayout
                    .getConstraints(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getDisplayModes());

            mainPanel.remove(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getDisplayModes());
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getDisplayModes(), prevDisplayModesConstraints);

            GridBagConstraints prevScalingModesConstraints = mainPanelLayout
                    .getConstraints(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getScalingModes());

            mainPanel.remove(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getScalingModes());
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getScalingModes(), prevScalingModesConstraints);

            GridBagConstraints prevDpiScalePercentagesConstraints = mainPanelLayout.getConstraints(
                    displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getDpiScalePercentages());

            mainPanel.remove(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getDpiScalePercentages());
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getDpiScalePercentages(),
                    prevDpiScalePercentagesConstraints);

            GridBagConstraints prevOrientationModeConstraints = mainPanelLayout.getConstraints(
                    displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getOrientationModes());

            mainPanel.remove(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getOrientationModes());
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getOrientationModes(),
                    prevOrientationModeConstraints);

            GridBagConstraints prevHotKeyConstraints = mainPanelLayout
                    .getConstraints(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getHotKey());

            mainPanel.remove(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getHotKey());
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getHotKey(), prevHotKeyConstraints);

            GridBagConstraints prevChangeHotKeyButtonConstraints = mainPanelLayout.getConstraints(
                    displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getChangeHotKeyButton());

            mainPanel.remove(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getChangeHotKeyButton());
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getChangeHotKeyButton(),
                    prevChangeHotKeyButtonConstraints);

            GridBagConstraints prevClearHotKeyButtonConstraints = mainPanelLayout.getConstraints(
                    displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getClearHotKeyButton());

            mainPanel.remove(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getClearHotKeyButton());
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getClearHotKeyButton(),
                    prevClearHotKeyButtonConstraints);
        }
    }

    /**
     * Removes the current number of active slots combo box and adds the correct one for the selected display.
     *
     * @param displayIndex
     *            - The index of the display to show the number of active slots combo box for
     */
    public void showNumberOfActiveSlotsForDisplay(int displayIndex) {
        Component oldActiveSlots = numberOfActiveSlotsMap.get(previouslySelectedDisplayIndex);
        Component newActiveSlots = numberOfActiveSlotsMap.get(displayIndex);
        int oldActiveSlotsPanelIndex = -1;

        for (int i = 0; i < displayPanel.getComponentCount(); i++) {
            if (displayPanel.getComponent(i) == oldActiveSlots) {
                oldActiveSlotsPanelIndex = i;
                break;
            }
        }

        if (oldActiveSlotsPanelIndex != -1) {
            displayPanel.remove(oldActiveSlots);
            displayPanel.add(newActiveSlots, oldActiveSlotsPanelIndex);
            displayPanel.revalidate();
            displayPanel.repaint();
        }
    }

    /**
     * Initializes all of the panels that will hold the view components.
     */
    private void initPanels() {
        mainPanel = new JPanel();
        displayPanel = new JPanel();
        menuPanel = new JPanel();

        mainPanelLayout = new GridBagLayout();
        displayPanelLayout = new FlowLayout(FlowLayout.LEFT, 16, 8);
        menuPanelLayout = new FlowLayout(FlowLayout.RIGHT, 16, 8);

        mainPanel.setLayout(mainPanelLayout);
        displayPanel.setLayout(displayPanelLayout);
        menuPanel.setLayout(menuPanelLayout);

        mainPanelConstraints = new GridBagConstraints();
        mainPanelConstraints.fill = GridBagConstraints.NONE;
        mainPanelConstraints.insets = new Insets(8, 8, 8, 8);

        displayPanelConstraints = new GridBagConstraints();
        displayPanelConstraints.fill = GridBagConstraints.NONE;

        menuPanelConstraints = new GridBagConstraints();
        menuPanelConstraints.fill = GridBagConstraints.NONE;
    }

    /**
     * Initializes the components and the initial selection for each interactive component.
     */
    private void initComponents() {
        noDisplayIdsPlaceholder = new JComboBox<Integer>(new Integer[]{-1});
        noDisplayIdsPlaceholder.setPreferredSize(new Dimension(60, 28));
        noDisplayIdsPlaceholder.setEnabled(false);

        noActiveSlotsPlaceholder = new JComboBox<Integer>(new Integer[]{0});
        noActiveSlotsPlaceholder.setPreferredSize(new Dimension(60, 28));
        noActiveSlotsPlaceholder.setEnabled(false);

        selectedDisplayLabel = new JLabel("Selected Display :", SwingConstants.LEFT);
        selectedDisplayLabel.setPreferredSize(new Dimension(112, 28));

        displayIds = model.getNumOfConnectedDisplays() > 0
                ? new JComboBox<Integer>(generateDisplayIds())
                : noDisplayIdsPlaceholder;
        displayIds.setPreferredSize(new Dimension(60, 28));

        // Select the preserved display before the dependent components are added so the view is built for it directly
        if (model.getNumOfConnectedDisplays() > 0) {
            displayIds.setSelectedIndex(previouslySelectedDisplayIndex);
        }

        numberOfActiveSlotsLabel = new JLabel("Active Slots :", SwingConstants.LEFT);
        numberOfActiveSlotsLabel.setPreferredSize(new Dimension(82, 28));

        if (model.getNumOfConnectedDisplays() > 0) {
            for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
                JComboBox<Integer> numberOfActiveSlots = new JComboBox<Integer>(generateNumOfSlotsValues());
                numberOfActiveSlots.setPreferredSize(new Dimension(60, 28));
                numberOfActiveSlots.setSelectedItem(model.getNumOfSlotsForDisplay(displayIndex));

                numberOfActiveSlotsMap.put(displayIndex, numberOfActiveSlots);
            }
        } else {
            numberOfActiveSlotsMap.put(-1, noActiveSlotsPlaceholder);
        }

        ButtonProperties clearAllButtonProps = new ButtonProperties("Clear All Slots", new Dimension(50, 28), 0.70f,
                0.60f);
        clearAllButton = new Button("/clear_all_idle.svg", "/clear_all_hover.svg", clearAllButtonProps, true);

        ButtonProperties refreshAppButtonProps = new ButtonProperties("Refresh App", new Dimension(31, 40), 0.70f,
                0.60f);
        refreshAppButton = new ThemeableButton("/refresh_app_idle.svg", "/refresh_app_light_hover.svg",
                "/refresh_app_idle.svg", "/refresh_app_dark_hover.svg", refreshAppButtonProps, true,
                model.isDarkMode());

        ButtonProperties aboutButtonProps = new ButtonProperties("About App", new Dimension(36, 40), 0.70f, 0.60f);
        aboutButton = new ThemeableButton("/about_idle.svg", "/about_light_hover.svg", "/about_idle.svg",
                "/about_dark_hover.svg", aboutButtonProps, true, model.isDarkMode());

        ButtonProperties themeButtonProps = new ButtonProperties("Change Theme", new Dimension(40, 40), 0.70f, 0.60f);
        themeButton = new ThemeableButton("/light_mode_idle.svg", "/light_mode_hover.svg", "/dark_mode_idle.svg",
                "/dark_mode_hover.svg", themeButtonProps, true, model.isDarkMode());

        ButtonProperties minimizeToTrayButtonProps = new ButtonProperties("Minimize To Tray", new Dimension(36, 40),
                0.70f, 0.60f);
        minimizeToTrayButton = new ThemeableToggleButton("/minimize_to_tray_enabled_idle.svg",
                "/minimize_to_tray_disabled_idle.svg", "/minimize_to_tray_enabled_light_hover.svg",
                "/minimize_to_tray_disabled_light_hover.svg", "/minimize_to_tray_enabled_dark_hover.svg",
                "/minimize_to_tray_disabled_dark_hover.svg", minimizeToTrayButtonProps, true, model.isDarkMode(),
                model.isMinimizeToTray());

        ButtonProperties runOnStartupButtonProps = new ButtonProperties("Run On Startup", new Dimension(36, 40), 0.70f,
                0.60f);
        runOnStartupButton = new ThemeableToggleButton("/run_on_startup_enabled_idle.svg",
                "/run_on_startup_disabled_idle.svg", "/run_on_startup_enabled_light_hover.svg",
                "/run_on_startup_disabled_light_hover.svg", "/run_on_startup_enabled_dark_hover.svg",
                "/run_on_startup_disabled_dark_hover.svg", runOnStartupButtonProps, true, model.isDarkMode(),
                model.isRunOnStartup());

        buttons.add(clearAllButton);
        buttons.add(refreshAppButton);
        buttons.add(aboutButton);
        buttons.add(themeButton);
        buttons.add(minimizeToTrayButton);
        buttons.add(runOnStartupButton);

        applyDisplayModeHeader = new JLabel("", SwingConstants.CENTER);
        applyDisplayModeHeader.setPreferredSize(new Dimension(36, 28));
        makeLabelBold(applyDisplayModeHeader);

        displayModeHeader = new JLabel("Display Mode", SwingConstants.CENTER);
        displayModeHeader.setPreferredSize(new Dimension(220, 28));
        makeLabelBold(displayModeHeader);

        scalingModeHeader = new JLabel("Scaling Mode", SwingConstants.CENTER);
        scalingModeHeader.setPreferredSize(new Dimension(90, 28));
        makeLabelBold(scalingModeHeader);

        dpiScaleHeader = new JLabel("DPI Scale", SwingConstants.CENTER);
        dpiScaleHeader.setPreferredSize(new Dimension(90, 28));
        makeLabelBold(dpiScaleHeader);

        orientationHeader = new JLabel("Orientation", SwingConstants.CENTER);
        orientationHeader.setPreferredSize(new Dimension(90, 28));
        makeLabelBold(orientationHeader);

        hotKeyHeader = new JLabel("Hot Key", SwingConstants.CENTER);
        hotKeyHeader.setPreferredSize(new Dimension(90, 28));
        makeLabelBold(hotKeyHeader);

        clearHotKeyHeader = new JLabel("", SwingConstants.CENTER);
        clearHotKeyHeader.setPreferredSize(new Dimension(32, 28));
        makeLabelBold(clearHotKeyHeader);

        changeHotKeyHeader = new JLabel("", SwingConstants.CENTER);
        changeHotKeyHeader.setPreferredSize(new Dimension(150, 28));
        makeLabelBold(changeHotKeyHeader);

        initSlotComponents();
        addNonSlotComponents();
        pushSlots(previouslySelectedDisplayIndex, 0);
    }

    /**
     * Generates the array of connected display IDs.
     *
     * @return The array of display IDs for all actively connected displays
     */
    private Integer[] generateDisplayIds() {
        Integer[] displayIds = new Integer[model.getNumOfConnectedDisplays()];

        for (int displayId = 1; displayId <= model.getNumOfConnectedDisplays(); displayId++) {
            displayIds[displayId - 1] = displayId;
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
     * Initializes the components for each slot.
     */
    private void initSlotComponents() {
        if (model.getNumOfConnectedDisplays() > 0) {
            DisplayConfig displayConfig = model.getDisplayConfig();
            String[] displayIds = model.getDisplayIds();

            for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
                List<Slot> slots = new ArrayList<Slot>();

                for (int slotIndex = 0; slotIndex < model.getMaxNumOfSlots(); slotIndex++) {
                    int slotOrientationMode = model.getSlot(displayIndex, slotIndex).getOrientationMode();
                    boolean landscapeOrientation = slotOrientationMode == 0 || slotOrientationMode == 2;
                    DisplayMode[] displayModes = landscapeOrientation
                            ? displayConfig.getLandscapeDisplayModes(displayIds[displayIndex])
                            : displayConfig.getPortraitDisplayModes(displayIds[displayIndex]);

                    slots.add(new Slot(slotIndex, displayIndex, displayModes, SCALING_MODES, DPI_SCALE_PERCENTAGES,
                            ORIENTATION_MODES));

                    slots.get(slotIndex).getDisplayModes()
                            .setSelectedItem(model.getSlot(displayIndex, slotIndex).getDisplayMode());

                    slots.get(slotIndex).getScalingModes()
                            .setSelectedIndex(model.getSlot(displayIndex, slotIndex).getScalingMode());

                    slots.get(slotIndex).getDpiScalePercentages()
                            .setSelectedItem(model.getSlot(displayIndex, slotIndex).getDpiScalePercentage());

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
        displayPanelConstraints.gridwidth = 1;
        displayPanelConstraints.gridx = 0;
        displayPanelConstraints.gridy = 0;
        displayPanel.add(selectedDisplayLabel, displayPanelConstraints);

        displayPanelConstraints.gridx = 1;
        displayPanel.add(displayIds, displayPanelConstraints);

        displayPanelConstraints.gridx = 2;
        displayPanel.add(numberOfActiveSlotsLabel, displayPanelConstraints);

        displayPanelConstraints.gridx = 3;

        if (model.getNumOfConnectedDisplays() > 0) {
            displayPanel.add(numberOfActiveSlotsMap.get(displayIds.getSelectedIndex()), displayPanelConstraints);
        } else {
            displayPanel.add(numberOfActiveSlotsMap.get(displayIds.getSelectedItem()), displayPanelConstraints);
        }

        menuPanelConstraints.gridwidth = 1;
        menuPanelConstraints.gridx = 0;
        menuPanelConstraints.gridy = 0;
        menuPanel.add(refreshAppButton, menuPanelConstraints);

        menuPanelConstraints.gridx = 1;
        menuPanel.add(aboutButton, menuPanelConstraints);

        menuPanelConstraints.gridx = 2;
        menuPanel.add(themeButton, menuPanelConstraints);

        menuPanelConstraints.gridx = 3;
        menuPanel.add(minimizeToTrayButton, menuPanelConstraints);

        menuPanelConstraints.gridx = 4;
        menuPanel.add(runOnStartupButton, menuPanelConstraints);

        mainPanelConstraints.anchor = GridBagConstraints.WEST;
        mainPanelConstraints.gridwidth = 9;
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 0;
        mainPanel.add(displayPanel, mainPanelConstraints);

        mainPanelConstraints.anchor = GridBagConstraints.EAST;
        mainPanel.add(menuPanel, mainPanelConstraints);

        mainPanelConstraints.anchor = GridBagConstraints.CENTER;
        mainPanelConstraints.gridwidth = 1;
        mainPanelConstraints.gridy = 1;
        mainPanel.add(clearAllButton, mainPanelConstraints);

        mainPanelConstraints.gridwidth = 1;
        mainPanelConstraints.gridx = 1;
        mainPanel.add(applyDisplayModeHeader, mainPanelConstraints);

        mainPanelConstraints.gridx = 2;
        mainPanel.add(displayModeHeader, mainPanelConstraints);

        mainPanelConstraints.gridx = 3;
        mainPanel.add(scalingModeHeader, mainPanelConstraints);

        mainPanelConstraints.gridx = 4;
        mainPanel.add(dpiScaleHeader, mainPanelConstraints);

        mainPanelConstraints.gridx = 5;
        mainPanel.add(orientationHeader, mainPanelConstraints);

        mainPanelConstraints.gridx = 6;
        mainPanel.add(hotKeyHeader, mainPanelConstraints);

        mainPanelConstraints.gridx = 7;
        mainPanel.add(clearHotKeyHeader, mainPanelConstraints);

        mainPanelConstraints.gridx = 8;
        mainPanel.add(changeHotKeyHeader, mainPanelConstraints);
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
     * Gets the frame's main panel.
     *
     * @return The frame's main panel
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * Gets the display IDs combo box.
     *
     * @return The combo box for the current display ID in the view
     */
    public JComboBox<Integer> getDisplayIds() {
        return displayIds;
    }

    /**
     * Gets the previously selected display index.
     *
     * @return The previously selected display index
     */
    public int getPreviouslySelectedDisplayIndex() {
        return previouslySelectedDisplayIndex;
    }

    /**
     * Sets the previously selected display index.
     *
     * @param previouslySelectedDisplayIndex
     *            - The previously selected display index
     */
    public void setPreviouslySelectedDisplayIndex(int previouslySelectedDisplayIndex) {
        this.previouslySelectedDisplayIndex = previouslySelectedDisplayIndex;
    }

    /**
     * Gets the number of active slots combo box for the given display index.
     *
     * @param displayIndex
     *            - The index of the display to get the number of active slots combo box for
     *
     * @return The number of active slots combo box for the given display index
     */
    public JComboBox<Integer> getNumberOfActiveSlots(int displayIndex) {
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
     * Gets the refresh app button.
     *
     * @return The refresh app button
     */
    public ThemeableButton getRefreshAppButton() {
        return refreshAppButton;
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
     * Gets the minimize to tray button.
     *
     * @return The minimize to tray button
     */
    public ThemeableToggleButton getMinimizeToTrayButton() {
        return minimizeToTrayButton;
    }

    /**
     * Gets the run on startup button.
     *
     * @return The run on startup button
     */
    public ThemeableToggleButton getRunOnStartupButton() {
        return runOnStartupButton;
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