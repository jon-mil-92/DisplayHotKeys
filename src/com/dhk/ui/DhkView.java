package com.dhk.ui;

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import com.dhk.models.DhkModel;
import com.formdev.flatlaf.ui.FlatUIUtils;
import java.awt.Insets;
import java.util.ArrayList;

/**
 * This class defines the view for Display Hot Keys. The layout for the view components is defined here. View components
 * are initialized and arranged in a GridBag layout.
 * 
 * @author Jonathan Miller
 * @version 1.2.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class DhkView {
    private DhkModel model;
    private ArrayList<Slot> slots;
    private JFrame frame;
    private JPanel mainPanel;
    private JPanel menuPanel;
    private GridBagConstraints mainConstraints;
    private GridBagConstraints menuConstraints;
    private JLabel numberOfSlotsLabel;
    private JComboBox<Integer> numberOfSlots;
    private JLabel displayModeLabel;
    private JLabel scalingModeLabel;
    private JLabel displayScaleLabel;
    private JLabel hotKeyLabel;
    private ClearAllButton clearAllButton;
    private RunOnStartupButton runOnStartupButton;
    private MinimizeButton minimizeButton;
    private ExitButton exitButton;
    private ThemeButton themeButton;
    private boolean darkMode;
    private boolean runOnStartup;
    private int slotGridY;

    // Each slot consists of seven different components.
    private final int NUM_OF_SLOT_COMPONENTS = 7;

    /**
     * Constructor for the DhkView class.
     * 
     * @param model        - The model for the application.
     * @param darkMode     - Whether or not to start the view in dark mode.
     * @param runOnStartup - Whether or not to run the application when the user logs into Windows.
     */
    public DhkView(DhkModel model, boolean darkMode, boolean runOnStartup) {
        // Initialize the model.
        this.model = model;

        // Initialize the starting state of the UI theme.
        this.darkMode = darkMode;

        // Initialize the "run on startup" state.
        this.runOnStartup = runOnStartup;

        // Initialize the window.
        frame = new JFrame();

        // Initialize the panel that will organize the components.
        mainPanel = new JPanel();

        // Set the main panel layout to the grid bag option.
        mainPanel.setLayout(new GridBagLayout());

        // Create a constraints object to change aspects of the main panel layout.
        mainConstraints = new GridBagConstraints();

        // Set main panel layout options.
        mainConstraints.fill = GridBagConstraints.NONE;
        mainConstraints.insets = new Insets(10, 10, 10, 10);

        // Initialize the menu panel that will organize the menu buttons.
        menuPanel = new JPanel();

        // Set the menu panel layout to the grid bag option.
        menuPanel.setLayout(new GridBagLayout());

        // Create a constraints object to change aspects of the menu panel layout.
        menuConstraints = new GridBagConstraints();

        // Set menu panel layout options.
        menuConstraints.fill = GridBagConstraints.NONE;
        menuConstraints.insets = new Insets(0, 4, 0, 4);

        // Initialize the slot views array list.
        slots = new ArrayList<Slot>();

        // Add the panel to the frame.
        frame.add(mainPanel);

        // Close the application when the close button is pressed.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Hide the title bar.
        frame.setUndecorated(true);

        // Make the window visible.
        frame.setVisible(true);

        // Do not allow the user to resize the window.
        frame.setResizable(false);

        // The starting Y coordinate of the slot components.
        slotGridY = 1;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Public methods
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * This method initializes the components and the initial selection for each interactive component.
     */
    public void initComponents() {
        // An array of the number of active slots value for the corresponding combo box.
        Integer[] numOfSlots = new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8 };

        // Initialize the number of slots components.
        numberOfSlotsLabel = new JLabel("Slots :", SwingConstants.LEFT);
        numberOfSlotsLabel.setPreferredSize(new Dimension(40, 28));

        numberOfSlots = new JComboBox<Integer>(numOfSlots);
        numberOfSlots.setPreferredSize(new Dimension(55, 28));

        // Initialize the clear all button.
        clearAllButton = new ClearAllButton("/clear_all_idle.svg", "/clear_all_hover.svg");

        // Initialize the run on startup button.
        runOnStartupButton = new RunOnStartupButton(runOnStartup, "/run_on_startup_enabled_idle.svg",
                "/run_on_startup_disabled_idle.svg", "/run_on_startup_enabled_hover.svg",
                "/run_on_startup_disabled_hover.svg");

        // Initialize the theme button.
        themeButton = new ThemeButton(darkMode, "/dark_mode_idle.svg", "/dark_mode_hover.svg", "/light_mode_idle.svg",
                "/light_mode_hover.svg");

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

        displayScaleLabel = new JLabel("Display Scale", SwingConstants.CENTER);
        displayScaleLabel.setPreferredSize(new Dimension(90, 28));
        makeLabelBold(displayScaleLabel);

        hotKeyLabel = new JLabel("Hot Key", SwingConstants.CENTER);
        hotKeyLabel.setPreferredSize(new Dimension(90, 28));
        makeLabelBold(hotKeyLabel);

        // Set the initial selection for the number of slots.
        numberOfSlots.setSelectedItem(model.getNumOfSlots());

        // Initialize all components for each slot in the view.
        initSlotComponents();

        // Add the label components to the view.
        addComponents();

        // Add the initial slots followed by the exit button.
        addSlots(0);

        // Pack the view components into the frame and automatically size the window.
        frame.pack();

        // Give focus to the number of slots combo box after adding all components.
        mainPanel.getComponent(1).requestFocusInWindow();

        // Center the frame in the screen after the size of the frame is defined after packing it.
        frame.setLocationRelativeTo(null);
    }

    /**
     * This method adds slots to the view panel until the new number of slots is reached.
     * 
     * @param startIndex - The index in which to start adding slots to the view's slot array.
     */
    public void addSlots(int startIndex) {
        // For each new slot to be added...
        for (int i = startIndex; i < model.getNumOfSlots(); i++) {
            // Anchor the following slot elements to the center of the grid.
            mainConstraints.anchor = GridBagConstraints.CENTER;

            // Define where the current slot's label is located in the grid bag layout.
            mainConstraints.gridwidth = 1;
            mainConstraints.gridx = 0;
            mainConstraints.gridy = slotGridY;
            mainPanel.add(slots.get(i).getIndicatorLabel(), mainConstraints);

            // Define where the current slot's display modes combo box is located in the grid bag layout.
            mainConstraints.gridwidth = 1;
            mainConstraints.gridx = 1;
            mainConstraints.gridy = slotGridY;
            mainPanel.add(slots.get(i).getDisplayModes(), mainConstraints);

            // Define where the current slot's scaling modes combo box is located in the grid bag layout.
            mainConstraints.gridwidth = 1;
            mainConstraints.gridx = 2;
            mainConstraints.gridy = slotGridY;
            mainPanel.add(slots.get(i).getScalingModes(), mainConstraints);

            // Define where the current slot's display scales combo box is located in the grid bag layout.
            mainConstraints.gridwidth = 1;
            mainConstraints.gridx = 3;
            mainConstraints.gridy = slotGridY;
            mainPanel.add(slots.get(i).getDisplayScales(), mainConstraints);

            // Define where the current slot's hot key is located in the grid bag layout.
            mainConstraints.gridwidth = 1;
            mainConstraints.gridx = 4;
            mainConstraints.gridy = slotGridY;
            mainPanel.add(slots.get(i).getHotKey(), mainConstraints);

            // Define where the current slot's change hot key button is located in the grid bag layout.
            mainConstraints.gridwidth = 1;
            mainConstraints.gridx = 5;
            mainConstraints.gridy = slotGridY;
            mainPanel.add(slots.get(i).getChangeHotKeyButton(), mainConstraints);

            // Define where the current slot's clear hot key button is located in the grid bag layout.
            mainConstraints.gridwidth = 1;
            mainConstraints.gridx = 6;
            mainConstraints.gridy = slotGridY;
            mainPanel.add(slots.get(i).getClearHotKeyButton(), mainConstraints);

            // Add the next slot view components to the following row in the layout.
            slotGridY++;
        }
    }

    /**
     * This method removes the specified number of slots to remove from the view panel.
     * 
     * @param numOfSlotsToRemove - The number of slots to remove from the view panel.
     */
    public void removeSlots(int numOfSlotsToRemove) {
        // Set the index for the last slot component in the panel.
        int lastSlotComponent = mainPanel.getComponentCount() - 1;

        // For every slot that needs to be removed...
        for (int i = 0; i < numOfSlotsToRemove; i++) {
            // For every components of the slot...
            for (int j = 0; j < NUM_OF_SLOT_COMPONENTS; j++) {
                // Remove the slot component.
                mainPanel.remove(lastSlotComponent);

                // Update the component index for the panel.
                lastSlotComponent--;
            }
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Private methods
    // -----------------------------------------------------------------------------------------------------------------

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
     * This method initialized the components for each slot.
     */
    private void initSlotComponents() {
        // Get an array of supported display modes for the main display.
        DisplayMode[] displayModes = getSupportedDisplayModes();

        // An array of scaling mode values for the corresponding combo box.
        String[] scalingModes = new String[] { "Preserved", "Stretched", "Centered" };

        // An array of scaling percentage values for the corresponding combo box.
        Integer[] displayScales = new Integer[] { 100, 125, 150, 175, 200, 225, 250, 300, 350 };

        // For each slot in the model, add it to the view and set the selection for it.
        for (int i = 0; i < model.getMaxNumOfSlots(); i++) {
            // Initialize the new slot components.
            slots.add(new Slot(Integer.toString(i), displayModes, scalingModes, displayScales));

            // Set the new display mode for the new slot.
            slots.get(i).getDisplayModes().setSelectedItem(model.getSlot(i).getDisplayMode());

            // Set the scaling mode for the new slot.
            slots.get(i).getScalingModes().setSelectedIndex(model.getSlot(i).getScalingMode());

            // Set the display scale for the new slot.
            slots.get(i).getDisplayScales().setSelectedItem(model.getSlot(i).getDisplayScale());

            // Set the hotkey for the new slot.
            slots.get(i).getHotKey().setText(model.getSlot(i).getHotKey().getHotKeyString());
        }
    }

    /**
     * This method generates an array of unique supported display modes for the main display.
     * 
     * @return An array of unique supported display modes for the main display.
     */
    private DisplayMode[] getSupportedDisplayModes() {
        // Get the graphics device.
        GraphicsDevice graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        // Get the supported display modes for the corresponding combo box.
        DisplayMode[] displayModes = graphicsDevice.getDisplayModes();

        // Instantiate an array list that will hold unique display modes.
        ArrayList<DisplayMode> displayModeSet = new ArrayList<DisplayMode>();

        // For each display mode in the array of supported display modes, starting from the highest display mode...
        for (int i = displayModes.length - 1; i >= 0; i--) {
            // Only add unique display modes to the array list.
            if (!displayModeSet.contains(displayModes[i])) {
                displayModeSet.add(displayModes[i]);
            }
        }

        // Convert the display modes array list to an array of unique display modes.
        displayModes = displayModeSet.toArray(displayModes);

        return displayModes;
    }

    /**
     * This method adds the label components and the button components to the view panel.
     */
    private void addComponents() {
        // Anchor the number of slots label to the left of the grid.
        mainConstraints.anchor = GridBagConstraints.WEST;

        // Define where the number of slots label is located in the grid bag layout.
        mainConstraints.gridwidth = 1;
        mainConstraints.gridx = 0;
        mainConstraints.gridy = 0;
        mainPanel.add(numberOfSlotsLabel, mainConstraints);

        // Anchor the number of slots combo box to the right of the grid.
        mainConstraints.anchor = GridBagConstraints.EAST;

        // Define where the number of slots combo box is located in the grid bag layout.
        mainConstraints.gridwidth = 1;
        mainConstraints.gridx = 0;
        mainConstraints.gridy = 0;
        mainPanel.add(numberOfSlots, mainConstraints);

        // Anchor the headers to the bottom of the grid.
        mainConstraints.anchor = GridBagConstraints.SOUTH;

        // Define where the display mode header is located in the grid bag layout.
        mainConstraints.gridwidth = 1;
        mainConstraints.gridx = 1;
        mainConstraints.gridy = 0;
        mainPanel.add(displayModeLabel, mainConstraints);

        // Define where the scaling mode header is located in the grid bag layout.
        mainConstraints.gridwidth = 1;
        mainConstraints.gridx = 2;
        mainConstraints.gridy = 0;
        mainPanel.add(scalingModeLabel, mainConstraints);

        // Define where the display scale header is located in the grid bag layout.
        mainConstraints.gridwidth = 1;
        mainConstraints.gridx = 3;
        mainConstraints.gridy = 0;
        mainPanel.add(displayScaleLabel, mainConstraints);

        // Define where the hot key header is located in the grid bag layout.
        mainConstraints.gridwidth = 1;
        mainConstraints.gridx = 4;
        mainConstraints.gridy = 0;
        mainPanel.add(hotKeyLabel, mainConstraints);

        // Anchor the menu panel to the top right of the grid.
        mainConstraints.anchor = GridBagConstraints.NORTHEAST;

        // Define where the menu panel is located in the grid bag layout.
        mainConstraints.gridwidth = 2;
        mainConstraints.gridx = 5;
        mainConstraints.gridy = 0;
        mainPanel.add(menuPanel, mainConstraints);

        // Define where the run on startup button is located in the grid bag layout.
        menuConstraints.gridwidth = 1;
        menuConstraints.gridx = 0;
        menuConstraints.gridy = 0;
        menuPanel.add(runOnStartupButton, menuConstraints);

        // Define where the theme button is located in the grid bag layout.
        menuConstraints.gridwidth = 1;
        menuConstraints.gridx = 1;
        menuConstraints.gridy = 0;
        menuPanel.add(themeButton, menuConstraints);

        // Define where the clear all button is located in the grid bag layout.
        menuConstraints.gridwidth = 1;
        menuConstraints.gridx = 2;
        menuConstraints.gridy = 0;
        menuPanel.add(clearAllButton, menuConstraints);

        // Define where the minimize button is located in the grid bag layout.
        menuConstraints.gridwidth = 1;
        menuConstraints.gridx = 3;
        menuConstraints.gridy = 0;
        menuPanel.add(minimizeButton, menuConstraints);

        // Define where the exit button is located in the grid bag layout.
        menuConstraints.gridwidth = 1;
        menuConstraints.gridx = 4;
        menuConstraints.gridy = 0;
        menuPanel.add(exitButton, menuConstraints);
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
     * Getter for the frame's panel.
     * 
     * @return The frame's panel.
     */
    public JPanel getPanel() {
        return mainPanel;
    }

    /**
     * Getter for the number of slots combo box.
     * 
     * @return The combo box for the current number of slots in the view.
     */
    public JComboBox<Integer> getNumberOfSlots() {
        return numberOfSlots;
    }

    /**
     * Getter for the array list of slots.
     * 
     * @return The array list of slots.
     */
    public ArrayList<Slot> getSlots() {
        return slots;
    }

    /**
     * Getter for the specified slot.
     * 
     * @param slotIndex - The index for the slot to get.
     * @return The specified slot.
     */
    public Slot getSlot(int slotIndex) {
        return slots.get(slotIndex);
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
     * Getter for the run on startup button.
     * 
     * @return The run on startup button.
     */
    public RunOnStartupButton getRunOnStartupButton() {
        return runOnStartupButton;
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
}