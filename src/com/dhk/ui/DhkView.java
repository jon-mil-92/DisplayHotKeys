package com.dhk.ui;

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import com.dhk.models.DhkModel;
import com.dhk.ui.buttons.ClearAllButton;
import com.dhk.ui.buttons.ExitButton;
import com.dhk.ui.buttons.MinimizeButton;
import com.dhk.ui.buttons.PaypalDonateButton;
import com.dhk.ui.buttons.RefreshAppButton;
import com.dhk.ui.buttons.RunOnStartupButton;
import com.dhk.ui.buttons.ThemeButton;
import com.dhk.io.DisplayConfig;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.ui.FlatUIUtils;
import java.awt.Insets;
import java.awt.Point;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class defines the view for Display Hot Keys. The layout for the view components is defined here. View components
 * are initialized and arranged in a GridBag layout.
 * 
 * @author Jonathan Miller
 * @version 1.3.2
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class DhkView {
    private DhkModel model;
    private ConcurrentHashMap<Integer, ArrayList<Slot>> displayMap;
    private ConcurrentHashMap<Integer, JComboBox<Integer>> numberOfActiveSlotsMap;
    private JFrame frame;
    private JPanel mainPanel;
    private JPanel displayPanel;
    private JPanel menuPanel;
    private GridBagLayout mainPanelLayout;
    private GridBagConstraints mainPanelConstraints;
    private GridBagConstraints displayPanelConstraints;
    private GridBagConstraints menuPanelConstraints;
    private JLabel displayIdsLabel;
    private JComboBox<Integer> displayIds;
    private JLabel numberOfActiveSlotsLabel;
    private JLabel displayModeLabel;
    private JLabel scalingModeLabel;
    private JLabel dpiScaleLabel;
    private JLabel hotKeyLabel;
    private PaypalDonateButton paypalDonateButton;
    private ThemeButton themeButton;
    private RunOnStartupButton runOnStartupButton;
    private RefreshAppButton refreshAppButton;
    private ClearAllButton clearAllButton;
    private MinimizeButton minimizeButton;
    private ExitButton exitButton;
    private boolean appLaunching;
    private int previouslySelectedDisplayIndex;
    private int gridYPositionForSlotInPanel;

    // Each slot consists of seven different components.
    private final int NUM_OF_SLOT_COMPONENTS = 7;

    /**
     * Constructor for the DhkView class.
     * 
     * @param model - The model for the application.
     */
    public DhkView(DhkModel model) {
        // Get the application's model.
        this.model = model;

        // The app is currently launching.
        appLaunching = true;

        // Disable logging for SVG icons.
        FlatSVGIcon.setLoggingEnabled(false);

        // The starting Y coordinate of the slot components.
        gridYPositionForSlotInPanel = 2;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Public methods
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * This method initializes the view of the application. It creates a new frame, sets the frame properties,
     * initializes the panels, and initializes the view components.
     * 
     * @param frameLocation - The point to spawn the view's frame at.
     */
    public void initView(Point frameLocation) {
        // Initialize the map of actively connected displays.
        displayMap = new ConcurrentHashMap<Integer, ArrayList<Slot>>();

        // Initialize the map of number of slots combo boxes for the actively connected displays.
        numberOfActiveSlotsMap = new ConcurrentHashMap<Integer, JComboBox<Integer>>();

        // The previously selected display is the primary display upon app initialization.
        previouslySelectedDisplayIndex = 0;

        // Initialize the frame of the application.
        frame = new JFrame();

        // Close the application when the close button is pressed.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Hide the title bar.
        frame.setUndecorated(true);

        // Make the window visible.
        frame.setVisible(true);

        // Do not allow the user to resize the window.
        frame.setResizable(false);

        // Initialize the panels that will organize the view components.
        initPanels();

        // Initialize the view components.
        initComponents();

        // Add the main panel to the frame.
        frame.add(mainPanel);

        // Pack the view components into the frame and automatically size the window.
        frame.pack();

        // If the app is currently launching...
        if (appLaunching) {
            // Update the "launching" state of the view.
            appLaunching = false;

            // Set the location of the frame to the center of the screen during the "launching" state.
            frame.setLocationRelativeTo(null);
        } else {
            // Set the location to the given point.
            frame.setLocation(frameLocation);
        }

        // Give focus to the display label.
        displayIdsLabel.requestFocusInWindow();
    }

    /**
     * This method re-initializes the view of the application.
     */
    public void reInitView() {
        // Get the old frame location before disposing of it.
        Point oldFrameLocation = frame.getLocation();

        // Dispose of the old frame.
        frame.dispose();

        // Re-initialize the view of the application and place the frame in the same location.
        initView(oldFrameLocation);
    }

    /**
     * This method adds slots to the main panel until the number of active slots for the given display is reached.
     * 
     * @param displayIndex - The index of the display to add slots in the view for.
     * @param startIndex   - The index in which to start adding slots to the view.
     */
    public void pushSlots(int displayIndex, int startIndex) {
        // For each new slot to be added...
        for (int slotIndex = startIndex; slotIndex < model.getNumOfSlotsForDisplay(displayIndex); slotIndex++) {
            // Anchor the following slot elements to the center of the grid.
            mainPanelConstraints.anchor = GridBagConstraints.CENTER;

            // Define where the current slot's label is located in the grid bag layout and add it to the panel.
            mainPanelConstraints.gridwidth = 1;
            mainPanelConstraints.gridx = 0;
            mainPanelConstraints.gridy = gridYPositionForSlotInPanel;
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getIndicatorLabel(), mainPanelConstraints);

            // Define where the current slot's display modes combo box is located in the grid bag layout and add it to
            // the panel.
            mainPanelConstraints.gridwidth = 1;
            mainPanelConstraints.gridx = 1;
            mainPanelConstraints.gridy = gridYPositionForSlotInPanel;
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getDisplayModes(), mainPanelConstraints);

            // Define where the current slot's scaling modes combo box is located in the grid bag layout and add it to
            // the panel.
            mainPanelConstraints.gridwidth = 1;
            mainPanelConstraints.gridx = 2;
            mainPanelConstraints.gridy = gridYPositionForSlotInPanel;
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getScalingModes(), mainPanelConstraints);

            // Define where the current slot's DPI scale percentages combo box is located in the grid bag layout and add
            // it to the panel.
            mainPanelConstraints.gridwidth = 1;
            mainPanelConstraints.gridx = 3;
            mainPanelConstraints.gridy = gridYPositionForSlotInPanel;
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getDpiScalePercentages(), mainPanelConstraints);

            // Define where the current slot's hot key is located in the grid bag layout and add it to the panel.
            mainPanelConstraints.gridwidth = 1;
            mainPanelConstraints.gridx = 4;
            mainPanelConstraints.gridy = gridYPositionForSlotInPanel;
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getHotKey(), mainPanelConstraints);

            // Define where the current slot's clear hot key button is located in the grid bag layout and add it to the
            // panel.
            mainPanelConstraints.gridwidth = 1;
            mainPanelConstraints.gridx = 5;
            mainPanelConstraints.gridy = gridYPositionForSlotInPanel;
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getClearHotKeyButton(), mainPanelConstraints);

            // Define where the current slot's change hot key button is located in the grid bag layout and add it to the
            // panel.
            mainPanelConstraints.gridwidth = 1;
            mainPanelConstraints.gridx = 6;
            mainPanelConstraints.gridy = gridYPositionForSlotInPanel;
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getChangeHotKeyButton(), mainPanelConstraints);

            // Add the next slot view components to the following row in the layout.
            gridYPositionForSlotInPanel++;
        }
    }

    /**
     * This method removes the specified number of slots from the end of the main panel.
     * 
     * @param numOfSlotsToRemove - The number of slots to remove from the end of the main panel.
     */
    public void popSlots(int numOfSlotsToRemove) {
        // Set the index for the last slot component in the panel.
        int lastSlotComponentIndex = mainPanel.getComponentCount() - 1;

        // For every slot that needs to be removed...
        for (int slotIndex = 0; slotIndex < numOfSlotsToRemove; slotIndex++) {
            // For every component of the slot...
            for (int componentIndex = 0; componentIndex < NUM_OF_SLOT_COMPONENTS; componentIndex++) {
                // Remove the slot component.
                mainPanel.remove(lastSlotComponentIndex);

                // Update the component index for the panel.
                lastSlotComponentIndex--;
            }
        }

        // Update the y-position of the next slot to add.
        gridYPositionForSlotInPanel -= numOfSlotsToRemove;
    }

    /**
     * This method replaces the active slots from the previously connected display with the active slots for the newly
     * selected display.
     */
    public void replaceActiveSlots() {
        // Get the number of slots to replace from the model.
        int numOfPrevActiveSlotsToReplace = model.getNumOfSlotsForDisplay(previouslySelectedDisplayIndex);

        // Get the currently selected display index.
        int displayIndex = displayIds.getSelectedIndex();

        // For every active slot to be replaced...
        for (int slotIndex = 0; slotIndex < numOfPrevActiveSlotsToReplace; slotIndex++) {
            // Get the constraints for the previous slot's indicator label.
            GridBagConstraints prevIndicatorLabelConstraints = mainPanelLayout
                    .getConstraints(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getIndicatorLabel());

            // Replace the previous slot's indicator label with the new slot's indicator label.
            mainPanel.remove(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getIndicatorLabel());
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getIndicatorLabel(),
                    prevIndicatorLabelConstraints);

            // Get the constraints for the previous slot's display modes combo box.
            GridBagConstraints prevDisplayModesConstraints = mainPanelLayout
                    .getConstraints(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getDisplayModes());

            // Replace the previous slot's display modes combo box with the new slot's display modes combo box.
            mainPanel.remove(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getDisplayModes());
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getDisplayModes(), prevDisplayModesConstraints);

            // Get the constraints for the previous slot's scaling modes combo box.
            GridBagConstraints prevScalingModesConstraints = mainPanelLayout
                    .getConstraints(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getScalingModes());

            // Replace the previous slot's scaling modes combo box with the new slot's scaling modes combo box.
            mainPanel.remove(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getScalingModes());
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getScalingModes(), prevScalingModesConstraints);

            // Get the constraints for the previous slot's DPI scale percentages combo box.
            GridBagConstraints prevDpiScalePercentagesConstraints = mainPanelLayout.getConstraints(
                    displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getDpiScalePercentages());

            // Replace the previous slot's DPI scale percentages with the new slot's DPI scale percentages.
            mainPanel.remove(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getDpiScalePercentages());
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getDpiScalePercentages(),
                    prevDpiScalePercentagesConstraints);

            // Get the constraints for the previous slot's hot key.
            GridBagConstraints prevHotKeyConstraints = mainPanelLayout
                    .getConstraints(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getHotKey());

            // Replace the previous slot's hot key with the new slot's hot key.
            mainPanel.remove(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getHotKey());
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getHotKey(), prevHotKeyConstraints);

            // Get the constraints for the previous slot's change hot key button.
            GridBagConstraints prevChangeHotKeyButtonConstraints = mainPanelLayout.getConstraints(
                    displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getChangeHotKeyButton());

            // Replace the previous slot's change hot key button with the new slot's change hot key button.
            mainPanel.remove(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getChangeHotKeyButton());
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getChangeHotKeyButton(),
                    prevChangeHotKeyButtonConstraints);

            // Get the constraints for the previous slot's clear hot key button.
            GridBagConstraints prevClearHotKeyButtonConstraints = mainPanelLayout.getConstraints(
                    displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getClearHotKeyButton());

            // Replace the previous slot's clear hot key button with the new slot's clear hot key button.
            mainPanel.remove(displayMap.get(previouslySelectedDisplayIndex).get(slotIndex).getClearHotKeyButton());
            mainPanel.add(displayMap.get(displayIndex).get(slotIndex).getClearHotKeyButton(),
                    prevClearHotKeyButtonConstraints);
        }
    }

    /**
     * This method removes the current number of active slots combo box and add the correct one for the selected
     * display.
     * 
     * @param displayIndex - The index of the display to show the number of active slots combo box for.
     */
    public void showNumberOfActiveSlotsForDisplay(int displayIndex) {
        // Update the number of active slots combo box for the current display.
        displayPanel.remove(3);

        // Define where the number of active slots combo box is located in the grid bag layout and add it to the panel.
        displayPanelConstraints.gridwidth = 1;
        displayPanelConstraints.gridx = 3;
        displayPanelConstraints.gridy = 0;
        displayPanel.add(numberOfActiveSlotsMap.get(displayIndex), displayPanelConstraints);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Private methods
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * This method initializes all of the panels that will hold the view components.
     */
    private void initPanels() {
        // Initialize the panel that will organize the components.
        mainPanel = new JPanel();

        // Initialize the layout manager of the main panel.
        mainPanelLayout = new GridBagLayout();

        // Set the main panel layout to the grid bag option.
        mainPanel.setLayout(mainPanelLayout);

        // Create a constraints object to change aspects of the main panel layout.
        mainPanelConstraints = new GridBagConstraints();

        // Set main panel layout options.
        mainPanelConstraints.fill = GridBagConstraints.NONE;
        mainPanelConstraints.insets = new Insets(8, 8, 8, 8);

        // Initialize the display panel that will organize the display index and number of slots view components.
        displayPanel = new JPanel();

        // Set the display panel layout to the grid bag option.
        displayPanel.setLayout(new GridBagLayout());

        // Create a constraints object to change aspects of the display panel layout.
        displayPanelConstraints = new GridBagConstraints();

        // Set display panel layout options.
        displayPanelConstraints.fill = GridBagConstraints.NONE;
        displayPanelConstraints.insets = new Insets(0, 7, 0, 7);

        // Initialize the menu panel that will organize the menu buttons.
        menuPanel = new JPanel();

        // Set the menu panel layout to the grid bag option.
        menuPanel.setLayout(new GridBagLayout());

        // Create a constraints object to change aspects of the menu panel layout.
        menuPanelConstraints = new GridBagConstraints();

        // Set menu panel layout options.
        menuPanelConstraints.fill = GridBagConstraints.NONE;
        menuPanelConstraints.insets = new Insets(0, 7, 0, 7);
    }

    /**
     * This method initializes the components and the initial selection for each interactive component.
     */
    private void initComponents() {
        // Initialize the display ID components.
        displayIdsLabel = new JLabel("Display :", SwingConstants.LEFT);
        displayIdsLabel.setPreferredSize(new Dimension(55, 28));
        displayIds = new JComboBox<Integer>(generateDisplayIds());
        displayIds.setPreferredSize(new Dimension(55, 28));

        // Initialize the number of slots components.
        numberOfActiveSlotsLabel = new JLabel("Slots :", SwingConstants.LEFT);
        numberOfActiveSlotsLabel.setPreferredSize(new Dimension(40, 28));

        // Initialize the map of number of active slots combo boxes.
        for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
            // Create a new num of active slots combo box for the current display.
            JComboBox<Integer> numberOfActiveSlots = new JComboBox<Integer>(generateNumOfSlotsValues());
            numberOfActiveSlots.setPreferredSize(new Dimension(60, 28));

            // Set the initial selection for the number of active slots combo box.
            numberOfActiveSlots.setSelectedItem(model.getNumOfSlotsForDisplay(displayIndex));

            // Add the combo box to the map of num of active slots combo boxes.
            numberOfActiveSlotsMap.put(displayIndex, numberOfActiveSlots);
        }

        // Initialize the paypal donate button.
        paypalDonateButton = new PaypalDonateButton(model.isDarkMode(), "/paypal_donate_dark_idle.svg",
                "/paypal_donate_dark_hover.svg", "/paypal_donate_light_idle.svg", "/paypal_donate_light_hover.svg");

        // Initialize the theme button.
        themeButton = new ThemeButton(model.isDarkMode(), "/dark_mode_idle.svg", "/dark_mode_hover.svg",
                "/light_mode_idle.svg", "/light_mode_hover.svg");

        // Initialize the run on startup button.
        runOnStartupButton = new RunOnStartupButton(model.isRunOnStartup(), "/run_on_startup_enabled_idle.svg",
                "/run_on_startup_disabled_idle.svg", "/run_on_startup_enabled_dark_hover.svg",
                "/run_on_startup_enabled_light_hover.svg", "/run_on_startup_disabled_dark_hover.svg",
                "/run_on_startup_disabled_light_hover.svg");

        // Initialize the refresh app button.
        refreshAppButton = new RefreshAppButton("/refresh_app_idle.svg", "/refresh_app_hover.svg");

        // Initialize the clear all button.
        clearAllButton = new ClearAllButton("/clear_all_idle.svg", "/clear_all_hover.svg");

        // Initialize the minimize button.
        minimizeButton = new MinimizeButton("/minimize_idle.svg", "/minimize_hover.svg");

        // Initialize the exit button.
        exitButton = new ExitButton("/exit_idle.svg", "/exit_hover.svg");

        // Initialize headers.
        displayModeLabel = new JLabel("Display Mode", SwingConstants.CENTER);
        displayModeLabel.setPreferredSize(new Dimension(90, 28));
        makeLabelBold(displayModeLabel);

        scalingModeLabel = new JLabel("Scaling Mode", SwingConstants.CENTER);
        scalingModeLabel.setPreferredSize(new Dimension(90, 28));
        makeLabelBold(scalingModeLabel);

        dpiScaleLabel = new JLabel("DPI Scale", SwingConstants.CENTER);
        dpiScaleLabel.setPreferredSize(new Dimension(90, 28));
        makeLabelBold(dpiScaleLabel);

        hotKeyLabel = new JLabel("Hot Key", SwingConstants.CENTER);
        hotKeyLabel.setPreferredSize(new Dimension(90, 28));
        makeLabelBold(hotKeyLabel);

        // Initialize all components for each slot in the view.
        initSlotComponents();

        // Add the label components to the view.
        addNonSlotComponents();

        // Add the initial slots followed by the exit button.
        pushSlots(0, 0);
    }

    /**
     * This method generates the array of connected display IDs.
     * 
     * @return The array of display IDs for all actively connected displays.
     */
    private Integer[] generateDisplayIds() {
        // Create an array to hold an identifier for each actively connected display.
        Integer[] displayIds = new Integer[model.getNumOfConnectedDisplays()];

        // Populate the display ID array with an identifier for each actively connected display.
        for (int displayId = 1; displayId <= model.getNumOfConnectedDisplays(); displayId++) {
            displayIds[displayId - 1] = displayId;
        }

        return displayIds;
    }

    /**
     * This method generates the array of number of active slots values.
     * 
     * @return The array of number of active slots values.
     */
    private Integer[] generateNumOfSlotsValues() {
        // An array of the number of active slots values for the corresponding combo box.
        Integer[] numberOfSlots = new Integer[model.getMaxNumOfSlots()];

        // For each number of active slots value, add it to an array of number of active slots values.
        for (int value = 1; value <= model.getMaxNumOfSlots(); value++) {
            numberOfSlots[value - 1] = value;
        }

        return numberOfSlots;
    }

    /**
     * This method makes a specified JLabel use bold font.
     */
    private void makeLabelBold(JLabel label) {
        // Make the label utilize the h4 font style, which is bold.
        label.putClientProperty("FlatLaf.styleClass", "h4");
        label.putClientProperty("FlatLaf.style", "font: $h4.font");
        label.setFont(FlatUIUtils.nonUIResource(UIManager.getFont("h4.font")));
    }

    /**
     * This method initializes the components for each slot.
     */
    private void initSlotComponents() {
        // An array of scaling mode values for the corresponding combo box.
        String[] scalingModes = new String[] { "Preserved", "Stretched", "Centered" };

        // An array of DPI scale percentage values for the corresponding combo box.
        Integer[] dpiScalePercentages = new Integer[] { 100, 125, 150, 175, 200, 225, 250, 300, 350 };

        // Get the display configuration from the model.
        DisplayConfig displayConfig = model.getDisplayConfig();

        // Get the array of display IDs from the model.
        String[] displayIds = model.getDisplayIds();

        // For each connected display...
        for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
            // Initialize a new array list of slots for the current display.
            ArrayList<Slot> slots = new ArrayList<Slot>();

            // Get an array of supported display modes for the current display.
            DisplayMode[] displayModes = displayConfig.getDisplayModes(displayIds[displayIndex]);

            // For each slot in the model, add it to the view and set the selection for it.
            for (int slotIndex = 0; slotIndex < model.getMaxNumOfSlots(); slotIndex++) {
                // Initialize the new slot components.
                slots.add(new Slot(Integer.toString(slotIndex), displayModes, scalingModes, dpiScalePercentages));

                // Set the new display mode for the new slot.
                slots.get(slotIndex).getDisplayModes()
                        .setSelectedItem(model.getSlot(displayIndex, slotIndex).getDisplayMode());

                // Set the scaling mode for the new slot.
                slots.get(slotIndex).getScalingModes()
                        .setSelectedIndex(model.getSlot(displayIndex, slotIndex).getScalingMode());

                // Set the DPI scale percentage for the new slot.
                slots.get(slotIndex).getDpiScalePercentages()
                        .setSelectedItem(model.getSlot(displayIndex, slotIndex).getDpiScalePercentage());

                // Set the hot key for the new slot.
                slots.get(slotIndex).getHotKey()
                        .setText(model.getSlot(displayIndex, slotIndex).getHotKey().getHotKeyString());
            }

            // Add slots for each actively connected display.
            displayMap.put(displayIndex, slots);
        }
    }

    /**
     * This method adds the labels and sub-panels to the main panel.
     */
    private void addNonSlotComponents() {
        // Anchor the display panel to the left of the grid.
        mainPanelConstraints.anchor = GridBagConstraints.WEST;

        // Define where the display panel is located in the grid bag layout and add it to the panel.
        mainPanelConstraints.gridwidth = 7;
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 0;
        mainPanel.add(displayPanel, mainPanelConstraints);

        // Define where the display IDs label is located in the grid bag layout and add it to the panel.
        displayPanelConstraints.gridwidth = 1;
        displayPanelConstraints.gridx = 0;
        displayPanelConstraints.gridy = 0;
        displayPanel.add(displayIdsLabel, displayPanelConstraints);

        // Define where the display ID combo box is located in the grid bag layout and add it to the panel.
        displayPanelConstraints.gridwidth = 1;
        displayPanelConstraints.gridx = 1;
        displayPanelConstraints.gridy = 0;
        displayPanel.add(displayIds, displayPanelConstraints);

        // Define where the number of active slots label is located in the grid bag layout and add it to the panel.
        displayPanelConstraints.gridwidth = 1;
        displayPanelConstraints.gridx = 2;
        displayPanelConstraints.gridy = 0;
        displayPanel.add(numberOfActiveSlotsLabel, displayPanelConstraints);

        // Define where the number of active slots combo box is located in the grid bag layout and add it to the panel.
        displayPanelConstraints.gridwidth = 1;
        displayPanelConstraints.gridx = 3;
        displayPanelConstraints.gridy = 0;
        displayPanel.add(numberOfActiveSlotsMap.get(displayIds.getSelectedIndex()), displayPanelConstraints);

        // Anchor the menu panel to the right of the grid.
        mainPanelConstraints.anchor = GridBagConstraints.EAST;

        // Define where the menu panel is located in the grid bag layout and add it to the panel.
        mainPanelConstraints.gridwidth = 7;
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 0;
        mainPanel.add(menuPanel, mainPanelConstraints);

        // Define where the paypal donate button is located in the grid bag layout and add it to the panel.
        menuPanelConstraints.gridwidth = 1;
        menuPanelConstraints.gridx = 0;
        menuPanelConstraints.gridy = 0;
        menuPanel.add(paypalDonateButton, menuPanelConstraints);

        // Define where the theme button is located in the grid bag layout and add it to the panel.
        menuPanelConstraints.gridwidth = 1;
        menuPanelConstraints.gridx = 1;
        menuPanelConstraints.gridy = 0;
        menuPanel.add(themeButton, menuPanelConstraints);

        // Define where the run on startup button is located in the grid bag layout and add it to the panel.
        menuPanelConstraints.gridwidth = 1;
        menuPanelConstraints.gridx = 2;
        menuPanelConstraints.gridy = 0;
        menuPanel.add(runOnStartupButton, menuPanelConstraints);

        // Define where the refresh app button is located in the grid bag layout and add it to the panel.
        menuPanelConstraints.gridwidth = 1;
        menuPanelConstraints.gridx = 3;
        menuPanelConstraints.gridy = 0;
        menuPanel.add(refreshAppButton, menuPanelConstraints);

        // Define where the clear all button is located in the grid bag layout and add it to the panel.
        menuPanelConstraints.gridwidth = 1;
        menuPanelConstraints.gridx = 4;
        menuPanelConstraints.gridy = 0;
        menuPanel.add(clearAllButton, menuPanelConstraints);

        // Define where the minimize button is located in the grid bag layout and add it to the panel.
        menuPanelConstraints.gridwidth = 1;
        menuPanelConstraints.gridx = 5;
        menuPanelConstraints.gridy = 0;
        menuPanel.add(minimizeButton, menuPanelConstraints);

        // Define where the exit button is located in the grid bag layout and add it to the panel.
        menuPanelConstraints.gridwidth = 1;
        menuPanelConstraints.gridx = 6;
        menuPanelConstraints.gridy = 0;
        menuPanel.add(exitButton, menuPanelConstraints);

        // Anchor the headers to the center of the grid.
        mainPanelConstraints.anchor = GridBagConstraints.CENTER;

        // Define where the display mode header is located in the grid bag layout and add it to the panel.
        mainPanelConstraints.gridwidth = 1;
        mainPanelConstraints.gridx = 1;
        mainPanelConstraints.gridy = 1;
        mainPanel.add(displayModeLabel, mainPanelConstraints);

        // Define where the scaling mode header is located in the grid bag layout and add it to the panel.
        mainPanelConstraints.gridwidth = 1;
        mainPanelConstraints.gridx = 2;
        mainPanelConstraints.gridy = 1;
        mainPanel.add(scalingModeLabel, mainPanelConstraints);

        // Define where the DPI scale header is located in the grid bag layout and add it to the panel.
        mainPanelConstraints.gridwidth = 1;
        mainPanelConstraints.gridx = 3;
        mainPanelConstraints.gridy = 1;
        mainPanel.add(dpiScaleLabel, mainPanelConstraints);

        // Define where the hot key header is located in the grid bag layout and add it to the panel.
        mainPanelConstraints.gridwidth = 1;
        mainPanelConstraints.gridx = 4;
        mainPanelConstraints.gridy = 1;
        mainPanel.add(hotKeyLabel, mainPanelConstraints);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Getters and setters
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Getter for the frame of the view.
     * 
     * @return The frame of the view.
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * Getter for the frame's main panel.
     * 
     * @return The frame's main panel.
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * Getter for the display IDs label.
     * 
     * @return The label for the display IDs combo box in the view.
     */
    public JLabel getDisplayIdsLabel() {
        return displayIdsLabel;
    }

    /**
     * Getter for the display IDs combo box.
     * 
     * @return The combo box for the current display ID in the view.
     */
    public JComboBox<Integer> getDisplayIds() {
        return displayIds;
    }

    /**
     * Getter for the previously selected display index.
     * 
     * @return The previously selected display index.
     */
    public int getPreviouslySelectedDisplayIndex() {
        return previouslySelectedDisplayIndex;
    }

    /**
     * Setter for the previously selected display index.
     * 
     * @param previouslySelectedDisplayIndex - The previously selected display index.
     */
    public void setPreviouslySelectedDisplayIndex(int previouslySelectedDisplayIndex) {
        this.previouslySelectedDisplayIndex = previouslySelectedDisplayIndex;
    }

    /**
     * Getter for the number of active slots combo box for the given display index.
     * 
     * @param displayIndex - The index of the display to get the number of active slots combo box for.
     * @return The number of active slots combo box for the given dispay index.
     */
    public JComboBox<Integer> getNumberOfActiveSlots(int displayIndex) {
        return numberOfActiveSlotsMap.get(displayIndex);
    }

    /**
     * Getter for the paypal donate button.
     * 
     * @return The paypal donate button.
     */
    public PaypalDonateButton getPaypalDonateButton() {
        return paypalDonateButton;
    }

    /**
     * Getter for the theme button.
     * 
     * @return The theme button.
     */
    public ThemeButton getThemeButton() {
        return themeButton;
    }

    /**
     * Getter for the run on startup button.
     * 
     * @return The run on startup button.
     */
    public RunOnStartupButton getRunOnStartupButton() {
        return runOnStartupButton;
    }

    /**
     * Getter for the refresh app button.
     * 
     * @return The refresh app button.
     */
    public RefreshAppButton getRefreshAppButton() {
        return refreshAppButton;
    }

    /**
     * Getter for the clear all button.
     * 
     * @return The clear all button.
     */
    public ClearAllButton getClearAllButton() {
        return clearAllButton;
    }

    /**
     * Getter for the minimize button.
     * 
     * @return The minimize button.
     */
    public MinimizeButton getMinimizeButton() {
        return minimizeButton;
    }

    /**
     * Getter for the exit button.
     * 
     * @return The exit button.
     */
    public ExitButton getExitButton() {
        return exitButton;
    }

    /**
     * Getter for the specified slot.
     * 
     * @param displayIndex - The index of the display to get the slot for.
     * @param slotIndex    - The index of the slot to get.
     * @return The specified slot.
     */
    public Slot getSlot(int displayIndex, int slotIndex) {
        return displayMap.get(displayIndex).get(slotIndex);
    }
}