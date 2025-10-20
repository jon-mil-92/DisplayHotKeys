package com.dhk.view;

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import com.dhk.io.DisplayConfig;
import com.dhk.model.DhkModel;
import com.dhk.model.button.Button;
import com.dhk.model.button.ThemeableButton;
import com.dhk.model.button.ThemeableToggleButton;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.ui.FlatUIUtils;

/**
 * Defines the view for Display Hot Keys. The layout for the view components is defined here. View components are
 * initialized and arranged in a GridBag layout.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class DhkView {

    private DhkModel model;
    private JFrame frame;
    private JPanel mainPanel;
    private JPanel displayPanel;
    private JPanel menuPanel;
    private GridBagLayout mainPanelLayout;
    private GridBagLayout displayPanelLayout;
    private GridBagLayout menuPanelLayout;
    private GridBagConstraints mainPanelConstraints;
    private GridBagConstraints displayPanelConstraints;
    private GridBagConstraints menuPanelConstraints;
    private JLabel displayIdsLabel;
    private JLabel numberOfActiveSlotsLabel;
    private JLabel displayModeLabel;
    private JLabel scalingModeLabel;
    private JLabel dpiScaleLabel;
    private JLabel orientationLabel;
    private JLabel hotKeyLabel;
    private JComboBox<Integer> displayIds;
    private Map<Integer, List<Slot>> displayMap;
    private Map<Integer, JComboBox<Integer>> numberOfActiveSlotsMap;
    private ThemeableButton themeButton;
    private ThemeableToggleButton minimizeToTrayButton;
    private ThemeableToggleButton runOnStartupButton;
    private Button refreshAppButton;
    private Button clearAllButton;
    private Button minimizeButton;
    private Button exitButton;
    private ThemeableButton paypalDonateButton;
    private List<ThemeableButton> themeableButtons;
    private boolean appLaunching;
    private int previouslySelectedDisplayIndex;
    private int gridYPosForSlotInPanel;

    private final int NUM_OF_SLOT_COMPONENTS = 9;
    private final String[] ORIENTATION_MODES = {"Landscape", "Portrait", "iLandscape", "iPortrait"};
    private final String[] SCALING_MODES = new String[]{"Preserved", "Stretched", "Centered"};
    private final Integer[] DPI_SCALE_PERCENTAGES = new Integer[]{100, 125, 150, 175, 200, 225, 250, 300, 350};

    private final String THEME_BUTTON_TOOLTIP = "Change Theme";
    private final Dimension THEME_BUTTON_SIZE = new Dimension(50, 50);
    private final float THEME_BUTTON_IDLE_SCALE = 0.80f;
    private final float THEME_BUTTON_HELD_SCALE = 0.68f;

    private final String MINIMIZE_TO_TRAY_BUTTON_TOOLTIP = "Minimize To Tray";
    private final Dimension MINIMIZE_TO_TRAY_BUTTON_SIZE = new Dimension(48, 50);
    private final float MINIMIZE_TO_TRAY_BUTTON_IDLE_SCALE = 0.80f;
    private final float MINIMIZE_TO_TRAY_BUTTON_HELD_SCALE = 0.68f;

    private final String RUN_ON_STARTUP_BUTTON_TOOLTIP = "Run On Startup";
    private final Dimension RUN_ON_STARTUP_BUTTON_SIZE = new Dimension(48, 50);
    private final float RUN_ON_STARTUP_BUTTON_IDLE_SCALE = 0.80f;
    private final float RUN_ON_STARTUP_BUTTON_HELD_SCALE = 0.68f;

    private final String REFRESH_APP_BUTTON_TOOLTIP = "Refresh App";
    private final Dimension REFRESH_APP_BUTTON_SIZE = new Dimension(40, 50);
    private final float REFRESH_APP_BUTTON_IDLE_SCALE = 0.80f;
    private final float REFRESH_APP_BUTTON_HELD_SCALE = 0.68f;

    private final String CLEAR_ALL_BUTTON_TOOLTIP = "Clear All Slots";
    private final Dimension CLEAR_ALL_BUTTON_SIZE = new Dimension(44, 50);
    private final float CLEAR_ALL_BUTTON_IDLE_SCALE = 0.80f;
    private final float CLEAR_ALL_BUTTON_HELD_SCALE = 0.68f;

    private final String MINIMIZE_BUTTON_TOOLTIP = "Minimize App";
    private final Dimension MINIMIZE_BUTTON_SIZE = new Dimension(32, 50);
    private final float MINIMIZE_BUTTON_IDLE_SCALE = 0.80f;
    private final float MINIMIZE_BUTTON_HELD_SCALE = 0.68f;

    private final String EXIT_BUTTON_TOOLTIP = "Exit App";
    private final Dimension EXIT_BUTTON_SIZE = new Dimension(34, 50);
    private final float EXIT_BUTTON_IDLE_SCALE = 0.80f;
    private final float EXIT_BUTTON_HELD_SCALE = 0.68f;

    private final String PAYPAL_DONATE_BUTTON_TOOLTIP = "PayPal Donate";
    private final Dimension PAYPAL_DONATE_BUTTON_SIZE = new Dimension(134, 46);
    private final float PAYPAL_DONATE_BUTTON_IDLE_SCALE = 0.70f;
    private final float PAYPAL_DONATE_BUTTON_HELD_SCALE = 0.63f;

    /**
     * Constructor for the DhkView class.
     * 
     * @param model
     *            - The model for the application
     */
    public DhkView(DhkModel model) {
        this.model = model;
        appLaunching = true;
        themeableButtons = new ArrayList<>();

        // Disable logging for icons
        FlatSVGIcon.setLoggingEnabled(false);

        // The starting Y-coordinate for slots
        gridYPosForSlotInPanel = 2;
    }

    /**
     * Initializes the view of the application. It creates a new frame, sets the frame properties, initializes the
     * panels, and initializes the view components.
     * 
     * @param frameLocation
     *            - The point to spawn the view's frame at
     */
    public void initView(Point frameLocation) {
        displayMap = new HashMap<Integer, List<Slot>>();
        numberOfActiveSlotsMap = new HashMap<Integer, JComboBox<Integer>>();
        previouslySelectedDisplayIndex = 0;

        frame = new JFrame();
        frame.setUndecorated(true);
        frame.setResizable(false);
        frame.setVisible(true);

        initPanels();
        initComponents();

        frame.add(mainPanel);
        frame.pack();

        if (appLaunching) {
            appLaunching = false;

            // Set the location of the frame to the center of the screen during the "launching" state
            frame.setLocationRelativeTo(null);
        } else {
            frame.setLocation(frameLocation);

            // Force a redraw to prevent window corruption
            frame.setSize(0, 0);
        }

        // Set the taskbar icon
        frame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/tray_icon.png")));

        displayIdsLabel.requestFocusInWindow();
    }

    /**
     * Re-initializes the view of the application.
     */
    public void reInitView() {
        Point oldFrameLocation = frame.getLocation();
        frame.dispose();

        // Re-initialize the view and place the frame in the same location
        initView(oldFrameLocation);
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
        for (int slotIndex = startIndex; slotIndex < model.getNumOfSlotsForDisplay(displayIndex); slotIndex++) {
            mainPanelConstraints.anchor = GridBagConstraints.CENTER;

            mainPanelConstraints.gridwidth = 1;
            mainPanelConstraints.gridx = 0;
            mainPanelConstraints.gridy = gridYPosForSlotInPanel;
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getIndicatorLabel(), mainPanelConstraints);

            mainPanelConstraints.anchor = GridBagConstraints.WEST;

            mainPanelConstraints.gridwidth = 1;
            mainPanelConstraints.gridx = 1;
            mainPanelConstraints.gridy = gridYPosForSlotInPanel;
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getApplyDisplayModeButton(),
                    mainPanelConstraints);

            mainPanelConstraints.anchor = GridBagConstraints.EAST;

            mainPanelConstraints.gridwidth = 1;
            mainPanelConstraints.gridx = 1;
            mainPanelConstraints.gridy = gridYPosForSlotInPanel;
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getDisplayModes(), mainPanelConstraints);

            mainPanelConstraints.anchor = GridBagConstraints.CENTER;

            mainPanelConstraints.gridwidth = 1;
            mainPanelConstraints.gridx = 2;
            mainPanelConstraints.gridy = gridYPosForSlotInPanel;
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getScalingModes(), mainPanelConstraints);

            mainPanelConstraints.gridwidth = 1;
            mainPanelConstraints.gridx = 3;
            mainPanelConstraints.gridy = gridYPosForSlotInPanel;
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getDpiScalePercentages(), mainPanelConstraints);

            mainPanelConstraints.gridwidth = 1;
            mainPanelConstraints.gridx = 4;
            mainPanelConstraints.gridy = gridYPosForSlotInPanel;
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getOrientationModes(), mainPanelConstraints);

            mainPanelConstraints.gridwidth = 1;
            mainPanelConstraints.gridx = 5;
            mainPanelConstraints.gridy = gridYPosForSlotInPanel;
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getHotKey(), mainPanelConstraints);

            mainPanelConstraints.gridwidth = 1;
            mainPanelConstraints.gridx = 6;
            mainPanelConstraints.gridy = gridYPosForSlotInPanel;
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getClearHotKeyButton(), mainPanelConstraints);

            mainPanelConstraints.gridwidth = 1;
            mainPanelConstraints.gridx = 7;
            mainPanelConstraints.gridy = gridYPosForSlotInPanel;
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getChangeHotKeyButton(), mainPanelConstraints);

            // Add the next slot to the following row in the layout
            gridYPosForSlotInPanel++;
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
        GridBagConstraints prevNumberOfSlotsConstraints = displayPanelLayout
                .getConstraints(numberOfActiveSlotsMap.get(previouslySelectedDisplayIndex));

        displayPanel.remove(numberOfActiveSlotsMap.get(previouslySelectedDisplayIndex));
        displayPanel.add(numberOfActiveSlotsMap.get(displayIndex), prevNumberOfSlotsConstraints);
    }

    /**
     * Initializes all of the panels that will hold the view components.
     */
    private void initPanels() {
        mainPanel = new JPanel();
        displayPanel = new JPanel();
        menuPanel = new JPanel();
        mainPanelLayout = new GridBagLayout();
        displayPanelLayout = new GridBagLayout();
        menuPanelLayout = new GridBagLayout();

        mainPanel.setLayout(mainPanelLayout);
        displayPanel.setLayout(displayPanelLayout);
        menuPanel.setLayout(menuPanelLayout);

        mainPanelConstraints = new GridBagConstraints();
        mainPanelConstraints.fill = GridBagConstraints.NONE;
        mainPanelConstraints.insets = new Insets(8, 8, 8, 8);

        displayPanelConstraints = new GridBagConstraints();
        displayPanelConstraints.fill = GridBagConstraints.NONE;
        displayPanelConstraints.insets = new Insets(0, 7, 0, 7);

        menuPanelConstraints = new GridBagConstraints();
        menuPanelConstraints.fill = GridBagConstraints.NONE;
        menuPanelConstraints.insets = new Insets(0, 6, 0, 6);
    }

    /**
     * Initializes the components and the initial selection for each interactive component.
     */
    private void initComponents() {
        displayIdsLabel = new JLabel("Display Number :", SwingConstants.LEFT);
        displayIdsLabel.setPreferredSize(new Dimension(106, 28));

        displayIds = new JComboBox<Integer>(generateDisplayIds());
        displayIds.setPreferredSize(new Dimension(60, 28));

        numberOfActiveSlotsLabel = new JLabel("Active Slots :", SwingConstants.LEFT);
        numberOfActiveSlotsLabel.setPreferredSize(new Dimension(81, 28));

        for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
            JComboBox<Integer> numberOfActiveSlots = new JComboBox<Integer>(generateNumOfSlotsValues());
            numberOfActiveSlots.setPreferredSize(new Dimension(60, 28));
            numberOfActiveSlots.setSelectedItem(model.getNumOfSlotsForDisplay(displayIndex));

            numberOfActiveSlotsMap.put(displayIndex, numberOfActiveSlots);
        }

        paypalDonateButton = new ThemeableButton("/paypal_donate_light_idle.svg", "/paypal_donate_light_hover.svg",
                "/paypal_donate_dark_idle.svg", "/paypal_donate_dark_hover.svg", PAYPAL_DONATE_BUTTON_TOOLTIP,
                PAYPAL_DONATE_BUTTON_SIZE, PAYPAL_DONATE_BUTTON_IDLE_SCALE, PAYPAL_DONATE_BUTTON_HELD_SCALE, true,
                model.isDarkMode());

        themeableButtons.add(paypalDonateButton);

        themeButton = new ThemeableButton("/light_mode_idle.svg", "/light_mode_hover.svg", "/dark_mode_idle.svg",
                "/dark_mode_hover.svg", THEME_BUTTON_TOOLTIP, THEME_BUTTON_SIZE, THEME_BUTTON_IDLE_SCALE,
                THEME_BUTTON_HELD_SCALE, true, model.isDarkMode());

        themeableButtons.add(themeButton);

        minimizeToTrayButton = new ThemeableToggleButton("/minimize_to_tray_enabled_idle.svg",
                "/minimize_to_tray_disabled_idle.svg", "/minimize_to_tray_enabled_light_hover.svg",
                "/minimize_to_tray_disabled_light_hover.svg", "/minimize_to_tray_enabled_dark_hover.svg",
                "/minimize_to_tray_disabled_dark_hover.svg", MINIMIZE_TO_TRAY_BUTTON_TOOLTIP,
                MINIMIZE_TO_TRAY_BUTTON_SIZE, MINIMIZE_TO_TRAY_BUTTON_IDLE_SCALE, MINIMIZE_TO_TRAY_BUTTON_HELD_SCALE,
                true, model.isDarkMode(), model.isMinimizeToTray());

        themeableButtons.add(minimizeToTrayButton);

        runOnStartupButton = new ThemeableToggleButton("/run_on_startup_enabled_idle.svg",
                "/run_on_startup_disabled_idle.svg", "/run_on_startup_enabled_light_hover.svg",
                "/run_on_startup_disabled_light_hover.svg", "/run_on_startup_enabled_dark_hover.svg",
                "/run_on_startup_disabled_dark_hover.svg", RUN_ON_STARTUP_BUTTON_TOOLTIP, RUN_ON_STARTUP_BUTTON_SIZE,
                RUN_ON_STARTUP_BUTTON_IDLE_SCALE, RUN_ON_STARTUP_BUTTON_HELD_SCALE, true, model.isDarkMode(),
                model.isRunOnStartup());

        themeableButtons.add(runOnStartupButton);

        refreshAppButton = new Button("/refresh_app_idle.svg", "/refresh_app_hover.svg", REFRESH_APP_BUTTON_TOOLTIP,
                REFRESH_APP_BUTTON_SIZE, REFRESH_APP_BUTTON_IDLE_SCALE, REFRESH_APP_BUTTON_HELD_SCALE, true);

        clearAllButton = new Button("/clear_all_idle.svg", "/clear_all_hover.svg", CLEAR_ALL_BUTTON_TOOLTIP,
                CLEAR_ALL_BUTTON_SIZE, CLEAR_ALL_BUTTON_IDLE_SCALE, CLEAR_ALL_BUTTON_HELD_SCALE, true);

        minimizeButton = new Button("/minimize_idle.svg", "/minimize_hover.svg", MINIMIZE_BUTTON_TOOLTIP,
                MINIMIZE_BUTTON_SIZE, MINIMIZE_BUTTON_IDLE_SCALE, MINIMIZE_BUTTON_HELD_SCALE, true);

        exitButton = new Button("/exit_idle.svg", "/exit_hover.svg", EXIT_BUTTON_TOOLTIP, EXIT_BUTTON_SIZE,
                EXIT_BUTTON_IDLE_SCALE, EXIT_BUTTON_HELD_SCALE, true);

        displayModeLabel = new JLabel("Display Mode", SwingConstants.CENTER);
        displayModeLabel.setPreferredSize(new Dimension(256, 28));
        makeLabelBold(displayModeLabel);

        scalingModeLabel = new JLabel("Scaling Mode", SwingConstants.CENTER);
        scalingModeLabel.setPreferredSize(new Dimension(90, 28));
        makeLabelBold(scalingModeLabel);

        dpiScaleLabel = new JLabel("DPI Scale", SwingConstants.CENTER);
        dpiScaleLabel.setPreferredSize(new Dimension(90, 28));
        makeLabelBold(dpiScaleLabel);

        orientationLabel = new JLabel("Orientation", SwingConstants.CENTER);
        orientationLabel.setPreferredSize(new Dimension(90, 28));
        makeLabelBold(orientationLabel);

        hotKeyLabel = new JLabel("Hot Key", SwingConstants.CENTER);
        hotKeyLabel.setPreferredSize(new Dimension(90, 28));
        makeLabelBold(hotKeyLabel);

        initSlotComponents();
        addNonSlotComponents();
        pushSlots(0, 0);
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

    /**
     * This method adds the labels and sub-panels to the main panel.
     */
    private void addNonSlotComponents() {
        mainPanelConstraints.anchor = GridBagConstraints.WEST;

        mainPanelConstraints.gridwidth = 7;
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 0;
        mainPanel.add(displayPanel, mainPanelConstraints);

        displayPanelConstraints.gridwidth = 1;
        displayPanelConstraints.gridx = 0;
        displayPanelConstraints.gridy = 0;
        displayPanel.add(displayIdsLabel, displayPanelConstraints);

        displayPanelConstraints.gridwidth = 1;
        displayPanelConstraints.gridx = 1;
        displayPanelConstraints.gridy = 0;
        displayPanel.add(displayIds, displayPanelConstraints);

        displayPanelConstraints.gridwidth = 1;
        displayPanelConstraints.gridx = 2;
        displayPanelConstraints.gridy = 0;
        displayPanel.add(numberOfActiveSlotsLabel, displayPanelConstraints);

        displayPanelConstraints.gridwidth = 1;
        displayPanelConstraints.gridx = 3;
        displayPanelConstraints.gridy = 0;
        displayPanel.add(numberOfActiveSlotsMap.get(displayIds.getSelectedIndex()), displayPanelConstraints);

        mainPanelConstraints.anchor = GridBagConstraints.EAST;

        mainPanelConstraints.gridwidth = 8;
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 0;
        mainPanel.add(menuPanel, mainPanelConstraints);

        menuPanelConstraints.gridwidth = 1;
        menuPanelConstraints.gridx = 0;
        menuPanelConstraints.gridy = 0;
        menuPanel.add(paypalDonateButton, menuPanelConstraints);

        menuPanelConstraints.gridwidth = 1;
        menuPanelConstraints.gridx = 1;
        menuPanelConstraints.gridy = 0;
        menuPanel.add(themeButton, menuPanelConstraints);

        menuPanelConstraints.gridwidth = 1;
        menuPanelConstraints.gridx = 2;
        menuPanelConstraints.gridy = 0;
        menuPanel.add(minimizeToTrayButton, menuPanelConstraints);

        menuPanelConstraints.gridwidth = 1;
        menuPanelConstraints.gridx = 3;
        menuPanelConstraints.gridy = 0;
        menuPanel.add(runOnStartupButton, menuPanelConstraints);

        menuPanelConstraints.gridwidth = 1;
        menuPanelConstraints.gridx = 4;
        menuPanelConstraints.gridy = 0;
        menuPanel.add(refreshAppButton, menuPanelConstraints);

        menuPanelConstraints.gridwidth = 1;
        menuPanelConstraints.gridx = 5;
        menuPanelConstraints.gridy = 0;
        menuPanel.add(clearAllButton, menuPanelConstraints);

        menuPanelConstraints.gridwidth = 1;
        menuPanelConstraints.gridx = 6;
        menuPanelConstraints.gridy = 0;
        menuPanel.add(minimizeButton, menuPanelConstraints);

        menuPanelConstraints.gridwidth = 1;
        menuPanelConstraints.gridx = 7;
        menuPanelConstraints.gridy = 0;
        menuPanel.add(exitButton, menuPanelConstraints);

        mainPanelConstraints.anchor = GridBagConstraints.CENTER;

        mainPanelConstraints.gridwidth = 1;
        mainPanelConstraints.gridx = 1;
        mainPanelConstraints.gridy = 1;
        mainPanel.add(displayModeLabel, mainPanelConstraints);

        mainPanelConstraints.gridwidth = 1;
        mainPanelConstraints.gridx = 2;
        mainPanelConstraints.gridy = 1;
        mainPanel.add(scalingModeLabel, mainPanelConstraints);

        mainPanelConstraints.gridwidth = 1;
        mainPanelConstraints.gridx = 3;
        mainPanelConstraints.gridy = 1;
        mainPanel.add(dpiScaleLabel, mainPanelConstraints);

        mainPanelConstraints.gridwidth = 1;
        mainPanelConstraints.gridx = 4;
        mainPanelConstraints.gridy = 1;
        mainPanel.add(orientationLabel, mainPanelConstraints);

        mainPanelConstraints.gridwidth = 1;
        mainPanelConstraints.gridx = 5;
        mainPanelConstraints.gridy = 1;
        mainPanel.add(hotKeyLabel, mainPanelConstraints);
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
     * Gets the display IDs label.
     * 
     * @return The label for the display IDs combo box in the view
     */
    public JLabel getDisplayIdsLabel() {
        return displayIdsLabel;
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
     * Gets the refresh app button.
     * 
     * @return The refresh app button
     */
    public Button getRefreshAppButton() {
        return refreshAppButton;
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
     * Gets the minimize button.
     * 
     * @return The minimize button
     */
    public Button getMinimizeButton() {
        return minimizeButton;
    }

    /**
     * Gets the exit button.
     * 
     * @return The exit button
     */
    public Button getExitButton() {
        return exitButton;
    }

    /**
     * Gets the paypal donate button.
     * 
     * @return The paypal donate button
     */
    public ThemeableButton getPaypalDonateButton() {
        return paypalDonateButton;
    }

    /**
     * Gets a list of themeable buttons in the view.
     * 
     * @return A list of themeable buttons in the view
     */
    public List<ThemeableButton> getThemeableButtons() {
        return themeableButtons;
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