package com.dhk.controllers;

import com.dhk.io.SettingsManager;
import com.dhk.io.DisplayConfig;
import com.dhk.io.KeyText;
import com.dhk.io.SetDisplay;
import com.dhk.models.DhkModel;
import com.dhk.models.HotKey;
import com.dhk.models.Key;
import com.dhk.ui.DhkView;
import com.dhk.window.FrameUpdater;
import lc.kra.system.keyboard.event.GlobalKeyEvent;
import lc.kra.system.keyboard.event.GlobalKeyListener;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This class controls the hot keys for the application. Listeners are added to the Change Hot Key buttons to enable the
 * functionality of changing hot keys. This class is also responsible for triggering hot key events once a hot key press
 * is detected.
 * 
 * @author Jonathan Miller
 * @version 1.4.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class HotKeysController implements Controller, GlobalKeyListener {
    private DhkView view;
    private DhkModel model;
    private SettingsManager settingsMgr;
    private SetDisplay setDisplay;
    private HotKey hotKeyBackup;
    private KeyText keyText;
    private Timer idleTimer;
    private Timer releaseMessageTimer;
    private FrameUpdater frameUpdater;
    private int currentKeyCount;
    private int maxNumOfSlots;
    private boolean showReleaseMessage;
    private boolean anyHotKeySubset;

    // Allow the user to be idle for 2500 ms before leaving the "changing hot key" state.
    private final static int IDLE_INPUT_TIMEOUT = 2500;

    // Let the release message display for 1500 ms before leaving the "changing hot key" state.
    private final static int RELEASE_MESSAGE_TIMEOUT = 1500;

    // Only get the first three unique keys while changing the hot key keys.
    private final int MAX_KEY_COUNT = 3;

    // Define strings for the Change Hot Key button states.
    private final String CHANGE_HOT_KEY_TEXT = "Change Hot Key";
    private final String PRESS_HOT_KEY_TEXT = "Press Hot Key";
    private final String RELEASE_TO_SET_TEXT = "Release To Set";
    private final String NO_SUBSETS_TEXT = "No Subsets";
    private final String HOT_KEY_SET_TEXT = "Hot Key Set";
    private final String HOT_KEY_NOT_SET_TEXT = "Hot Key Not Set";

    /**
     * Constructor for the HotKeysController class.
     *
     * @param model       - The model for the application.
     * @param view        - The view for the application.
     * @param settingsMgr - The settings manager for the application.
     */
    public HotKeysController(DhkModel model, DhkView view, SettingsManager settingsMgr) {
        // Get the application's model, view, and settings manager.
        this.model = model;
        this.view = view;
        this.settingsMgr = settingsMgr;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Public Methods
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * This method initializes the variables needed for the hot key controller.
     */
    @Override
    public void initController() {
        // The user is not inputting a hot key during initialization.
        currentKeyCount = 0;

        // Initialize a new frame updater object that will be used to refresh the frame once a hot key is changed.
        frameUpdater = new FrameUpdater(view);

        // Get the maximum number of slots that can be displayed in the application from the settings manager.
        maxNumOfSlots = settingsMgr.getMaxNumOfSlots();

        // Initialize the set display object that will immediately apply the new display settings.
        setDisplay = new SetDisplay();

        // Initialize the key text object to get the correct text representation of a key given a key code.
        keyText = new KeyText();

        // Initialize the show release message boolean to false because the user has not attempted to set a hot key yet.
        showReleaseMessage = false;

        // Any hot key is not a subset of another upon initialization of this controller.
        anyHotKeySubset = false;
    }

    /**
     * This method initializes the listeners for hot key input.
     */
    @Override
    public void initListeners() {
        // For each connected display...
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            // The display index for the current display to add an action listener to.
            int displayIndex = i;

            // Set the action listener for each Change Hot Key button in the view.
            for (int j = 0; j < maxNumOfSlots; j++) {
                // The index for the slot view to add an action listener to.
                int slotIndex = j;

                // Set action listeners for the Change Hot Key button presses from the view.
                view.getSlot(displayIndex, slotIndex).getChangeHotKeyButton()
                        .addActionListener(e -> slotHotKeyChangeEvent(displayIndex, slotIndex));
            }
        }
    }

    @Override
    public void cleanUp() {
    }

    @Override
    public void keyPressed(GlobalKeyEvent keyEvent) {
        // Use the AWT event dispatch thread for key press events.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // For each connected display...
                for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
                    // For each slot...
                    for (int slotIndex = 0; slotIndex < maxNumOfSlots; slotIndex++) {
                        // Set the slot's pressed hot key keys.
                        setPressedKeys(keyEvent, model.getSlot(displayIndex, slotIndex).getHotKey().getKeys());

                        // Update each hot key state every time a hot key is pressed.
                        updateHotKeyState(model.getSlot(displayIndex, slotIndex).getHotKey());

                        // If the release messsage is not displaying after settings a hot key...
                        if (!showReleaseMessage) {
                            // Only check the active slots for building a new key or setting a display mode.
                            if (slotIndex < model.getNumOfSlotsForDisplay(displayIndex)) {
                                // If the user is changing a slot's hot key...
                                if (model.getSlot(displayIndex, slotIndex).getHotKey().isChangingHotKey()) {
                                    // Build the slot's hot key from the pressed keys during the "changing hot key"
                                    // state.
                                    buildHotKey(keyEvent, model.getSlot(displayIndex, slotIndex).getHotKey(),
                                            slotIndex);

                                    // If any hot key is a subset of another hot key...
                                    if (anyHotKeySubset()) {
                                        // Notify the user that hot keys cannot be a subset of another hot key.
                                        view.getSlot(displayIndex, slotIndex).getChangeHotKeyButton()
                                                .setText(NO_SUBSETS_TEXT);

                                        anyHotKeySubset = true;
                                    }
                                    // Otherwise, keep notifying the user to release the hot key to set it.
                                    else {
                                        // Update the Change Hot Key button text to notify the user to input keys.
                                        view.getSlot(displayIndex, slotIndex).getChangeHotKeyButton()
                                                .setText(RELEASE_TO_SET_TEXT);

                                        anyHotKeySubset = false;
                                    }
                                }

                                // If no hot key is being changed and a hot key is pressed but not held down...
                                if (!changingHotKey()
                                        && model.getSlot(displayIndex, slotIndex).getHotKey().isHotKeyPressed()
                                        && !model.getSlot(displayIndex, slotIndex).getHotKey().isHotKeyHeldDown()) {
                                    // Set the display settings if the display is connected.
                                    setDisplaySettings(displayIndex, slotIndex);
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void keyReleased(GlobalKeyEvent keyEvent) {
        // Use the AWT event dispatch thread for key release events.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // For each connected display...
                for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
                    // For each slot...
                    for (int slotIndex = 0; slotIndex < maxNumOfSlots; slotIndex++) {
                        // Set the hot key's keys to not pressed.
                        setReleasedKeys(keyEvent, model.getSlot(displayIndex, slotIndex).getHotKey().getKeys());

                        // Update each hot key state every time a key is released.
                        updateHotKeyState(model.getSlot(displayIndex, slotIndex).getHotKey());

                        // If the user releases the hot key during the "changing hot key" state.
                        if (model.getSlot(displayIndex, slotIndex).getHotKey().isChangingHotKey()
                                && !model.getSlot(displayIndex, slotIndex).getHotKey().isHotKeyPressed()) {
                            // Start showing the release message text for the Change Hot Key button.
                            showReleaseMessage = true;

                            // Leave the "changing hot key" state after realeasing the hot key.
                            leaveChangingHotKeyState(slotIndex);

                            // Start the release message timer to display the release message for the given time.
                            startReleaseMessageTimer(RELEASE_MESSAGE_TIMEOUT, slotIndex);
                        }
                    }
                }
            }
        });
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Private Methods
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * This method changes the specified slot's Change Hot Key button text and updates the model's corresponding
     * boolean.
     * 
     * @param displayIndex - The index of the display to update the hot key for.
     * @param slotIndex    - The index of the slot to change the hot key for.
     */
    private void slotHotKeyChangeEvent(int displayIndex, int slotIndex) {
        // Do not allow changing multiple hot keys at the same time.
        if (!changingHotKey()) {
            // Create a new hot key for the backup of the old hot key.
            hotKeyBackup = new HotKey(new ArrayList<Key>());

            // Add all keys from the old hot key into the backup hot key.
            hotKeyBackup.getKeys().addAll(model.getSlot(displayIndex, slotIndex).getHotKey().getKeys());

            // Start a new key array list for the slot's hot key.
            model.getSlot(displayIndex, slotIndex).getHotKey().setKeys(new ArrayList<Key>());

            // Update the Change Hot Key button text to notify user to input keys.
            view.getSlot(displayIndex, slotIndex).getChangeHotKeyButton().setText(PRESS_HOT_KEY_TEXT);

            // Disable interactive view components during changing the hot key.
            disableComponents();

            // Change the slot into the "changing hot key" state.
            model.getSlot(displayIndex, slotIndex).getHotKey().setChangingHotKey(true);

            // Start the idle input timer to stop getting keys after a certain period of inactivity.
            startIdleTimer(IDLE_INPUT_TIMEOUT, slotIndex);
        }
    }

    /**
     * This method sets hot key keys as pressed if the given key event matches a key in the hot key.
     * 
     * @param keyEvent - The key event that occurred.
     * @param keys     - The array list of keys for a hot key.
     */
    private void setPressedKeys(GlobalKeyEvent keyEvent, ArrayList<Key> keys) {
        // For each key in the array list...
        for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
            // If the key event's key code matches the current key in the array list...
            if (keyEvent.getVirtualKeyCode() == keys.get(keyIndex).getKey()) {
                // Update the boolean that determines whether the key is being pressed or not.
                keys.get(keyIndex).setKeyPressed(true);
            }
        }
    }

    /**
     * This method sets hot key keys as not pressed if the given key event matches a key in the hot key.
     * 
     * @param keyEvent - The key event that occurred.
     * @param keys     - The array list of keys for a hot key.
     */
    private void setReleasedKeys(GlobalKeyEvent keyEvent, ArrayList<Key> keys) {
        // For each key in the array list...
        for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
            // If the key event's key code matches the current key in the array list...
            if (keyEvent.getVirtualKeyCode() == keys.get(keyIndex).getKey()) {
                // Update the boolean that determines whether the key is being pressed or not.
                keys.get(keyIndex).setKeyPressed(false);
            }
        }
    }

    /**
     * This method updates the state of the given hot key.
     * 
     * @param hotKey - The hot key for the keys to check.
     * @return Whether or not all keys in a hot key are pressed.
     */
    private boolean updateHotKeyState(HotKey hotKey) {
        boolean allKeysPressed = true;

        // If the hot key is not set...
        if (hotKey.getKeys().size() == 0) {
            // Then no keys are pressed.
            allKeysPressed = false;
        }

        // For each key in the array list of hot key keys...
        for (int keyIndex = 0; keyIndex < hotKey.getKeys().size(); keyIndex++) {
            // Check each key state and store the AND conditional of them all.
            allKeysPressed &= hotKey.getKeys().get(keyIndex).isKeyPressed();
        }

        // If the hot key is pressed currently, and it was pressed the last time a key event was fired...
        if (allKeysPressed && hotKey.isHotKeyPressed()) {
            // Set the slot's hot key state to "held down."
            hotKey.setHotKeyHeldDown(true);
        }
        // Else, if the hot key is pressed now but not the last time a key event was fired...
        else if (allKeysPressed && !hotKey.isHotKeyPressed()) {
            // Set the slot's hot key state to "pressed."
            hotKey.setHotKeyPressed(true);
        }
        // Otherwise, if the hot key is not currently pressed or held down...
        else {
            // Set the slot's hot key states to "not pressed" and "not held down."
            hotKey.setHotKeyPressed(false);
            hotKey.setHotKeyHeldDown(false);
        }

        return allKeysPressed;
    }

    /**
     * This method builds the new hot key that will be used to change display settings.
     * 
     * @param keyEvent  - The native key event that was fired.
     * @param hotKey    - The hot key to build.
     * @param slotIndex - The index of the slot to build the hot key for.
     */
    private void buildHotKey(GlobalKeyEvent keyEvent, HotKey hotKey, int slotIndex) {
        // Stop the idle timer because the user started setting a hot key.
        idleTimer.stop();

        // Define a variable to hold the pressed hot key.
        Key pressedKey;

        // Get the key code and text representation for the pressed key.
        int keyCode = keyEvent.getVirtualKeyCode();
        String keyCodeText = keyText.getKeyCodeText(keyCode);

        // Get the display index for the selected display from the view.
        int selectedDisplayIndex = view.getDisplayIds().getSelectedIndex();

        // If the user has not entered the max number of keys...
        if (currentKeyCount < MAX_KEY_COUNT) {
            // Create a temporary hot key for comparison in the following conditional.
            pressedKey = new Key(keyCode, keyCodeText, true);

            // Only allow unique keys to form the hot key, and ignore key code 255 due to a bug in System Hook.
            if (!hotKey.getKeys().contains(pressedKey) && !(pressedKey.getKey() == 255)) {
                // Add the new key into the hot key array list.
                hotKey.getKeys().add(pressedKey);

                // Update the hot key text in the view for the slot.
                view.getSlot(selectedDisplayIndex, slotIndex).getHotKey()
                        .setText(model.getSlot(selectedDisplayIndex, slotIndex).getHotKey().getHotKeyString());

                // Update the view's frame.
                frameUpdater.updateUI();

                // Increment the key counter.
                currentKeyCount += 1;
            }
        }
    }

    /**
     * This method checks if any hot key is a subset of another hot key.
     * 
     * @return Whether or not any hot key is a subset of another hot key.
     */
    private boolean anyHotKeySubset() {
        // The booleans to determine if there are any hot key subsets.
        boolean isHotKeySubsetInSelectedDisplay = false;
        boolean isHotKeySubsetInAnotherDisplay = false;

        // Get the display index for the selected display from the view.
        int selectedDisplayIndex = view.getDisplayIds().getSelectedIndex();

        // For each connected display...
        for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
            // For each slot in the current display...
            for (int slotIndex = 0; slotIndex < maxNumOfSlots; slotIndex++) {
                // If iterating through the slots for the selected display index...
                if (displayIndex == selectedDisplayIndex) {
                    // Check if there are any subsets in the selected display.
                    isHotKeySubsetInSelectedDisplay = isHotKeySubsetInSelectedDisplay(slotIndex);
                } else {
                    // Check if there are any subsets between displays.
                    isHotKeySubsetInAnotherDisplay = isHotKeySubsetInAnotherDisplay(slotIndex);
                }

                // If any subsets are found, return true.
                if (isHotKeySubsetInSelectedDisplay || isHotKeySubsetInAnotherDisplay) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * This method checks if the hot key for the specified slot in the selected display is a subset of another hot key
     * in the selected display.
     * 
     * @param slotIndexToCheck - The index of the slot containing the hot key to check.
     * @return Whether or not the hot key for the specified slot in the selected display is a subset of another hot key
     *         in the selected display.
     */
    private boolean isHotKeySubsetInSelectedDisplay(int slotIndexToCheck) {
        // Get the display index for the selected display from the view.
        int selectedDisplayIndex = view.getDisplayIds().getSelectedIndex();

        // Get the hot key key array list from the specified slot.
        ArrayList<Key> keys = model.getSlot(selectedDisplayIndex, slotIndexToCheck).getHotKey().getKeys();

        // Only compare hot keys that have been set.
        if (keys.size() > 0) {
            // For each slot...
            for (int slotIndex = 0; slotIndex < maxNumOfSlots; slotIndex++) {
                // Skip comparing the hot key for the given slot index to check.
                if (slotIndex != slotIndexToCheck) {
                    // Get the keys in the hot key for the current slot.
                    ArrayList<Key> currentKeys = model.getSlot(selectedDisplayIndex, slotIndex).getHotKey().getKeys();

                    // Only compare hot keys that have been set.
                    if (currentKeys.size() > 0) {
                        // If all of the keys from the given slot's hot key are in the current hot key, return true.
                        if (currentKeys.containsAll(keys)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * This method checks if the hot key for the specified slot in the selected display is a subset of another hot key
     * in another display.
     * 
     * @param slotIndexToCheck - The index of the slot containing the hot key to check.
     * @return Whether or not the hot key for the specified slot in the selected display is a subset of another hot key
     *         in another display.
     */
    private boolean isHotKeySubsetInAnotherDisplay(int slotIndexToCheck) {
        // Get the display index for the selected display from the view.
        int selectedDisplayIndex = view.getDisplayIds().getSelectedIndex();

        // Get the hot key key array list from the specified slot.
        ArrayList<Key> keysToCheck = model.getSlot(selectedDisplayIndex, slotIndexToCheck).getHotKey().getKeys();

        // Only compare hot keys that have been set.
        if (keysToCheck.size() > 0) {
            // For each connected display...
            for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
                // Only check against the hot keys from other displays.
                if (displayIndex != selectedDisplayIndex) {
                    // For each slot in the current display...
                    for (int slotIndex = 0; slotIndex < maxNumOfSlots; slotIndex++) {
                        // Get the keys in the hot key for the current slot.
                        ArrayList<Key> currentKeys = model.getSlot(displayIndex, slotIndex).getHotKey().getKeys();

                        // Only compare hot keys that have been set.
                        if (currentKeys.size() > 0) {
                            // Allow hot keys to be the same between displays so the user can change display settings
                            // for multiple displays with one hot key.
                            if (!keysToCheck.equals(currentKeys)) {
                                // If all keys from the given slot's hot key are in the current hot key, return true.
                                if (currentKeys.containsAll(keysToCheck) || keysToCheck.containsAll(currentKeys)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * This method checks to see if any slots are in the "changing hot key" state.
     * 
     * @return Whether or not any active slots are in the "changing hot key" state.
     */
    private boolean changingHotKey() {
        // Boolean to check the current slot's "changing hot key" state against.
        boolean changingHotKeys = false;

        // Get the display index for the selected display from the view.
        int selectedDisplayIndex = view.getDisplayIds().getSelectedIndex();

        // For each slot...
        for (int slotIndex = 0; slotIndex < model.getNumOfSlotsForDisplay(selectedDisplayIndex); slotIndex++) {
            // If any hot key is being changed, the boolean will return true.
            changingHotKeys |= (model.getSlot(selectedDisplayIndex, slotIndex).getHotKey().isChangingHotKey());
        }

        return changingHotKeys;
    }

    /**
     * This method sets the display settings if the display is connected.
     * 
     * @param displayIndex - The index of the display to set the display settings for.
     * @param slotIndex    - The index of the slot to set the display settings for.
     */
    private void setDisplaySettings(int displayIndex, int slotIndex) {
        // Get the current display configuration.
        DisplayConfig displayConfig = new DisplayConfig();
        displayConfig.updateDisplayIds();

        // Get the ID for the given display index.
        String displayId = model.getDisplayIds()[displayIndex];

        // If the connected displays have not changed...
        if (Arrays.equals(model.getDisplayIds(), displayConfig.getDisplayIds())) {
            // Set the display settings.
            setDisplay.applyDisplaySettings(displayId,
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getWidth(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getHeight(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getBitDepth(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getRefreshRate(),
                    model.getSlot(displayIndex, slotIndex).getScalingMode(),
                    model.getSlot(displayIndex, slotIndex).getDpiScalePercentage(),
                    model.getOrientationModeForDisplay(displayIndex));
        }
    }

    /**
     * Leave the "changing hot key" state for the specified slot's hot key.
     * 
     * @param slotIndex - The slot index for the hot key that is leaving the "changing hot key" state.
     */
    private void leaveChangingHotKeyState(int slotIndex) {
        // Reset the key counter.
        currentKeyCount = 0;

        // Get the display index for the selected display from the view.
        int selectedDisplayIndex = view.getDisplayIds().getSelectedIndex();

        // Get the ID for the given display.
        String displayId = model.getDisplayIds()[selectedDisplayIndex];

        // The ID for the slot starts at 1.
        int slotId = slotIndex + 1;

        // If the user did not type any keys before the idle timeout or any hot key is a subset of another...
        if (model.getSlot(selectedDisplayIndex, slotIndex).getHotKey().getKeys().size() == 0 || anyHotKeySubset) {
            // Only notify the user that the hot key was not set if any hot key is a subset of another.
            if (anyHotKeySubset) {
                // Update the Change Hot Key button text to notify the user that the hot key was not successfully set.
                view.getSlot(selectedDisplayIndex, slotIndex).getChangeHotKeyButton().setText(HOT_KEY_NOT_SET_TEXT);
            }

            // Restore the hot key to what it was before trying to change it.
            model.getSlot(selectedDisplayIndex, slotIndex).setHotKey(hotKeyBackup);

            // Update the view for the restored hot key.
            view.getSlot(selectedDisplayIndex, slotIndex).getHotKey()
                    .setText(model.getSlot(selectedDisplayIndex, slotIndex).getHotKey().getHotKeyString());

            // Since the hot key was restored, no hot key is a subset of another anymore.
            anyHotKeySubset = false;

            // Update the view's frame.
            frameUpdater.updateUI();
        } else {
            // Update the Change Hot Key button text to notify the user that the hot key was successfully set.
            view.getSlot(selectedDisplayIndex, slotIndex).getChangeHotKeyButton().setText(HOT_KEY_SET_TEXT);
        }

        // No longer getting input from the user, so update the "changing hot key" state for the slot's hot key.
        model.getSlot(selectedDisplayIndex, slotIndex).getHotKey().setChangingHotKey(false);

        // Save the new hot key for the specified display ID and slot ID in the settings file.
        settingsMgr.saveIniSlotHotKey(displayId, slotId, model.getSlot(selectedDisplayIndex, slotIndex).getHotKey());

        // If the release message is not showing...
        if (!showReleaseMessage) {
            // Update the Change Hot Key button text to notify the user that they can change the hot key again.
            view.getSlot(selectedDisplayIndex, slotIndex).getChangeHotKeyButton().setText(CHANGE_HOT_KEY_TEXT);

            // Re-enable the view components.
            enableComponents();
        }
    }

    /**
     * This method starts a timer to stop displaying the release message after attempting to change a hot key.
     * 
     * @param milliseconds - The timeout value.
     * @param slotIndex    - The index of the slot for the potentially changed hot key.
     */
    private void startReleaseMessageTimer(int milliseconds, int slotIndex) {
        // Create a new timer to stop displaying the release message after the specified number of milliseconds.
        releaseMessageTimer = new Timer(milliseconds, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Stop showing the release message and allow user input.
                showReleaseMessage = false;

                // Get the display index for the selected display from the view.
                int selectedDisplayIndex = view.getDisplayIds().getSelectedIndex();

                // Update the Change Hot Key button text to notify the user that they can change the hot key again.
                view.getSlot(selectedDisplayIndex, slotIndex).getChangeHotKeyButton().setText(CHANGE_HOT_KEY_TEXT);

                // Re-enable the view components.
                enableComponents();
            }
        });

        // Only execute timeout event once.
        releaseMessageTimer.setRepeats(false);

        // Start counting down the timer.
        releaseMessageTimer.start();
    }

    /**
     * This method starts a timer to leave the "changing hot key" state when the user is idle while changing a hot key.
     * 
     * @param milliseconds - The timeout value.
     * @param slotIndex    - The index of the slot for the hot key to change.
     */
    private void startIdleTimer(int milliseconds, int slotIndex) {
        // Create a new timer to leave the "changing hot key" state after the specified number of milliseconds.
        idleTimer = new Timer(milliseconds, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Leave the "changing hot key" state.
                leaveChangingHotKeyState(slotIndex);
            }
        });

        // Only execute the timeout event once.
        idleTimer.setRepeats(false);

        // Start counting down the timer.
        idleTimer.start();
    }

    /**
     * This method disables all interactive view components except the exit button component to avoid unintended
     * selection during changing the hot key.
     */
    private void disableComponents() {
        // Disable the display IDs combo box while getting user input.
        view.getDisplayIds().setEnabled(false);

        // Disable the paypal donate button while getting user input.
        view.getPaypalDonateButton().setEnabled(false);

        // Disable the theme button while getting user input.
        view.getThemeButton().setEnabled(false);

        // Disable the run on startup button while getting user input.
        view.getRunOnStartupButton().setEnabled(false);

        // Disable the refresh display modes button while getting user input.
        view.getRefreshAppButton().setEnabled(false);

        // Disable the clear all button while getting user input.
        view.getClearAllButton().setEnabled(false);

        // Disable the minimize button while getting user input.
        view.getMinimizeButton().setEnabled(false);

        // Disable the exit button while getting user input.
        view.getExitButton().setEnabled(false);

        // For each connected display...
        for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
            // Disable the number of active slots combo box for the current display index.
            view.getNumberOfActiveSlots(displayIndex).setEnabled(false);

            // Disable all interactive components for each slot in the view.
            for (int slotIndex = 0; slotIndex < model.getNumOfSlotsForDisplay(displayIndex); slotIndex++) {
                // Disable all apply display mode buttons while getting user input.
                view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton().setEnabled(false);

                // Disable all display mode combo boxes while getting user input.
                view.getSlot(displayIndex, slotIndex).getDisplayModes().setEnabled(false);

                // Disable all scaling mode combo boxes while getting user input.
                view.getSlot(displayIndex, slotIndex).getScalingModes().setEnabled(false);

                // Disable all DPI scale percentages combo boxes while getting user input.
                view.getSlot(displayIndex, slotIndex).getDpiScalePercentages().setEnabled(false);

                // Disable all Clear Hot Key buttons while getting user input.
                view.getSlot(displayIndex, slotIndex).getClearHotKeyButton().setEnabled(false);

                // Disable all Change Hot Key buttons while getting user input.
                view.getSlot(displayIndex, slotIndex).getChangeHotKeyButton().setEnabled(false);
            }
        }
    }

    /**
     * This method re-enables all interactive view components except the exit button component because it is already
     * enabled.
     */
    private void enableComponents() {
        // Enable the display IDs combo box after getting user input.
        view.getDisplayIds().setEnabled(true);

        // Enable the paypal donate button after getting user input.
        view.getPaypalDonateButton().setEnabled(true);

        // Enable the theme button after getting user input.
        view.getThemeButton().setEnabled(true);

        // Enable the run on startup button after getting user input.
        view.getRunOnStartupButton().setEnabled(true);

        // Enable the refresh display modes button after getting user input.
        view.getRefreshAppButton().setEnabled(true);

        // Enable the clear all button after getting user input.
        view.getClearAllButton().setEnabled(true);

        // Enable the minimize button after getting user input.
        view.getMinimizeButton().setEnabled(true);

        // Enable the exit button after getting user input.
        view.getExitButton().setEnabled(true);

        // For each connected display...
        for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
            // Enable the number of avtive slots combo box for the current display index.
            view.getNumberOfActiveSlots(displayIndex).setEnabled(true);

            // Enable all interactive components for each slot in the view.
            for (int slotIndex = 0; slotIndex < model.getNumOfSlotsForDisplay(displayIndex); slotIndex++) {
                // Enable all apply display mode buttons after getting user input.
                view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton().setEnabled(true);

                // Enable all display mode combo boxes after getting user input.
                view.getSlot(displayIndex, slotIndex).getDisplayModes().setEnabled(true);

                // Enable all scaling mode combo boxes after getting user input.
                view.getSlot(displayIndex, slotIndex).getScalingModes().setEnabled(true);

                // Enable all DPI scale percentages combo boxes after getting user input.
                view.getSlot(displayIndex, slotIndex).getDpiScalePercentages().setEnabled(true);

                // Enable the Clear Hot Key button after getting user input if the hot key is set.
                if (model.getSlot(displayIndex, slotIndex).getHotKey().getKeys().size() > 0) {
                    view.getSlot(displayIndex, slotIndex).getClearHotKeyButton().setEnabled(true);
                }

                // Enable all Change Hot Key buttons after getting user input.
                view.getSlot(displayIndex, slotIndex).getChangeHotKeyButton().setEnabled(true);
            }
        }
    }
}