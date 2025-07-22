package com.dhk.controller;

import com.dhk.io.SettingsManager;
import com.dhk.main.AppRefresher;
import com.dhk.model.DhkModel;
import com.dhk.model.HotKey;
import com.dhk.model.Key;
import com.dhk.view.DhkView;
import com.dhk.io.DisplayConfig;
import com.dhk.io.KeyText;
import com.dhk.io.SetDisplay;
import com.dhk.window.FrameUpdater;
import lc.kra.system.keyboard.event.GlobalKeyEvent;
import lc.kra.system.keyboard.event.GlobalKeyListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Controls the hot keys for the application. Listeners are added to the Change Hot Key buttons to enable the
 * functionality of changing hot keys. This class is also responsible for triggering hot key events once a hot key press
 * is detected.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class HotKeysController implements IController, GlobalKeyListener {

    private DhkView view;
    private DhkModel model;
    private DhkController controller;
    private SettingsManager settingsMgr;
    private SetDisplay setDisplay;
    private HotKey hotKeyBackup;
    private Timer idleTimer;
    private Timer releaseMessageTimer;
    private FrameUpdater frameUpdater;
    private AppRefresher appRefresher;
    private int currentKeyCount;
    private int maxNumOfSlots;
    private boolean showReleaseMessage;
    private boolean anyHotKeySubset;

    private final String CHANGE_HOT_KEY_TEXT = "Change Hot Key";
    private final String PRESS_HOT_KEY_TEXT = "Press Hot Key";
    private final String RELEASE_TO_SET_TEXT = "Release To Set";
    private final String NO_SUBSETS_TEXT = "No Subsets";
    private final String HOT_KEY_SET_TEXT = "Hot Key Set";
    private final String HOT_KEY_NOT_SET_TEXT = "Hot Key Not Set";
    private final int IDLE_INPUT_TIMEOUT = 2500;
    private final int RELEASE_MESSAGE_TIMEOUT = 1500;
    private final int MAX_KEY_COUNT = 3;

    /**
     * Constructor for the HotKeysController class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param controller
     *            - The controller for the application
     * @param settingsMgr
     *            - The settings manager for the application
     */
    public HotKeysController(DhkModel model, DhkView view, DhkController controller, SettingsManager settingsMgr) {
        this.model = model;
        this.view = view;
        this.controller = controller;
        this.settingsMgr = settingsMgr;
    }

    /**
     * Initializes the variables needed for the hot key controller.
     */
    @Override
    public void initController() {
        currentKeyCount = 0;
        frameUpdater = new FrameUpdater(view);
        maxNumOfSlots = settingsMgr.getMaxNumOfSlots();
        setDisplay = new SetDisplay();
        showReleaseMessage = false;
        anyHotKeySubset = false;
        appRefresher = new AppRefresher(model, view, controller, settingsMgr);
    }

    /**
     * Initializes the listeners for hot key input.
     */
    @Override
    public void initListeners() {
        for (int i = 0; i < model.getNumOfConnectedDisplays(); i++) {
            int displayIndex = i;

            // Set the action listener for each Change Hot Key button in the view
            for (int j = 0; j < maxNumOfSlots; j++) {
                int slotIndex = j;

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
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
                    for (int slotIndex = 0; slotIndex < maxNumOfSlots; slotIndex++) {
                        setPressedKeys(keyEvent, model.getSlot(displayIndex, slotIndex).getHotKey().getKeys());
                        updateHotKeyState(model.getSlot(displayIndex, slotIndex).getHotKey());

                        if (!showReleaseMessage) {
                            // Only check the active slots for building a new key or setting a display mode
                            if (slotIndex < model.getNumOfSlotsForDisplay(displayIndex)) {
                                if (model.getSlot(displayIndex, slotIndex).getHotKey().isChangingHotKey()) {
                                    buildHotKey(keyEvent, model.getSlot(displayIndex, slotIndex).getHotKey(),
                                            slotIndex);

                                    if (anyHotKeySubset()) {
                                        // Notify the user that hot keys cannot be a subset of another hot key
                                        view.getSlot(displayIndex, slotIndex).getChangeHotKeyButton()
                                                .setText(NO_SUBSETS_TEXT);

                                        anyHotKeySubset = true;
                                    } else {
                                        // Update the Change Hot Key button text to notify the user to input keys
                                        view.getSlot(displayIndex, slotIndex).getChangeHotKeyButton()
                                                .setText(RELEASE_TO_SET_TEXT);

                                        anyHotKeySubset = false;
                                    }
                                }

                                // If no hot key is being changed, and a hot key is pressed but not held down
                                if (!changingHotKey()
                                        && model.getSlot(displayIndex, slotIndex).getHotKey().isHotKeyPressed()
                                        && !model.getSlot(displayIndex, slotIndex).getHotKey().isHotKeyHeldDown()) {
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
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
                    for (int slotIndex = 0; slotIndex < maxNumOfSlots; slotIndex++) {
                        setReleasedKeys(keyEvent, model.getSlot(displayIndex, slotIndex).getHotKey().getKeys());
                        updateHotKeyState(model.getSlot(displayIndex, slotIndex).getHotKey());

                        // If the user releases the hot key during the "changing hot key" state
                        if (model.getSlot(displayIndex, slotIndex).getHotKey().isChangingHotKey()
                                && !model.getSlot(displayIndex, slotIndex).getHotKey().isHotKeyPressed()) {
                            showReleaseMessage = true;

                            leaveChangingHotKeyState(slotIndex);
                            startReleaseMessageTimer(RELEASE_MESSAGE_TIMEOUT, slotIndex);
                        }
                    }
                }
            }
        });
    }

    /**
     * Changes the specified slot's Change Hot Key button text and updates the model's corresponding boolean.
     * 
     * @param displayIndex
     *            - The index of the display to update the hot key for
     * @param slotIndex
     *            - The index of the slot to change the hot key for
     */
    private void slotHotKeyChangeEvent(int displayIndex, int slotIndex) {
        // Do not allow changing multiple hot keys at the same time
        if (!changingHotKey()) {
            hotKeyBackup = new HotKey(new ArrayList<Key>());
            hotKeyBackup.getKeys().addAll(model.getSlot(displayIndex, slotIndex).getHotKey().getKeys());

            model.getSlot(displayIndex, slotIndex).getHotKey().setKeys(new ArrayList<Key>());
            view.getSlot(displayIndex, slotIndex).getChangeHotKeyButton().setText(PRESS_HOT_KEY_TEXT);

            disableComponents();

            model.getSlot(displayIndex, slotIndex).getHotKey().setChangingHotKey(true);

            startIdleTimer(IDLE_INPUT_TIMEOUT, slotIndex);
        }
    }

    /**
     * Sets hot key keys as pressed if the given key event matches a key in the hot key.
     * 
     * @param keyEvent
     *            - The key event that occurred
     * @param keys
     *            - The array list of keys for a hot key
     */
    private void setPressedKeys(GlobalKeyEvent keyEvent, List<Key> keys) {
        for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
            if (keyEvent.getVirtualKeyCode() == keys.get(keyIndex).getKey()) {
                keys.get(keyIndex).setKeyPressed(true);
            }
        }
    }

    /**
     * Sets hot key keys as not pressed if the given key event matches a key in the hot key.
     * 
     * @param keyEvent
     *            - The key event that occurred
     * @param keys
     *            - The array list of keys for a hot key
     */
    private void setReleasedKeys(GlobalKeyEvent keyEvent, List<Key> keys) {
        for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
            if (keyEvent.getVirtualKeyCode() == keys.get(keyIndex).getKey()) {
                keys.get(keyIndex).setKeyPressed(false);
            }
        }
    }

    /**
     * Updates the state of the given hot key.
     * 
     * @param hotKey
     *            - The hot key for the keys to check
     * 
     * @return Whether or not all keys in a hot key are pressed
     */
    private boolean updateHotKeyState(HotKey hotKey) {
        boolean allKeysPressed = true;

        // If the hot key is not set
        if (hotKey.getKeys().size() == 0) {
            allKeysPressed = false;
        }

        for (int keyIndex = 0; keyIndex < hotKey.getKeys().size(); keyIndex++) {
            allKeysPressed &= hotKey.getKeys().get(keyIndex).isKeyPressed();
        }

        // If the hot key is pressed currently, and it was pressed the last time a key event was fired
        if (allKeysPressed && hotKey.isHotKeyPressed()) {
            hotKey.setHotKeyHeldDown(true);
        }
        // Else, if the hot key is pressed now but not the last time a key event was fired
        else if (allKeysPressed && !hotKey.isHotKeyPressed()) {
            hotKey.setHotKeyPressed(true);
        }
        // Otherwise, if the hot key is not currently pressed or held down
        else {
            hotKey.setHotKeyPressed(false);
            hotKey.setHotKeyHeldDown(false);
        }

        return allKeysPressed;
    }

    /**
     * Builds the new hot key that will be used to change display settings.
     * 
     * @param keyEvent
     *            - The native key event that was fired
     * @param hotKey
     *            - The hot key to build
     * @param slotIndex
     *            - The index of the slot to build the hot key for
     */
    private void buildHotKey(GlobalKeyEvent keyEvent, HotKey hotKey, int slotIndex) {
        idleTimer.stop();

        Key pressedKey;
        int keyCode = keyEvent.getVirtualKeyCode();
        String keyCodeText = KeyText.getKeyCodeText(keyCode);
        int selectedDisplayIndex = view.getDisplayIds().getSelectedIndex();

        if (currentKeyCount < MAX_KEY_COUNT) {
            pressedKey = new Key(keyCode, keyCodeText, true);

            // Only allow unique keys to form the hot key, and ignore key code 255 due to a bug in System Hook
            if (!hotKey.getKeys().contains(pressedKey) && !(pressedKey.getKey() == 255)) {
                hotKey.getKeys().add(pressedKey);

                view.getSlot(selectedDisplayIndex, slotIndex).getHotKey()
                        .setText(model.getSlot(selectedDisplayIndex, slotIndex).getHotKey().getHotKeyString());
                frameUpdater.updateUI();

                currentKeyCount += 1;
            }
        }
    }

    /**
     * Checks if any hot key is a subset of another hot key.
     * 
     * @return Whether or not any hot key is a subset of another hot key
     */
    private boolean anyHotKeySubset() {
        boolean isHotKeySubsetInSelectedDisplay = false;
        boolean isHotKeySubsetInAnotherDisplay = false;
        int selectedDisplayIndex = view.getDisplayIds().getSelectedIndex();

        for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
            for (int slotIndex = 0; slotIndex < maxNumOfSlots; slotIndex++) {
                if (displayIndex == selectedDisplayIndex) {
                    isHotKeySubsetInSelectedDisplay = isHotKeySubsetInSelectedDisplay(slotIndex);
                } else {
                    isHotKeySubsetInAnotherDisplay = isHotKeySubsetInAnotherDisplay(slotIndex);
                }

                if (isHotKeySubsetInSelectedDisplay || isHotKeySubsetInAnotherDisplay) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if the hot key for the specified slot in the selected display is a subset of another hot key in the
     * selected display.
     * 
     * @param slotIndexToCheck
     *            - The index of the slot containing the hot key to check
     * 
     * @return Whether or not the hot key for the specified slot in the selected display is a subset of another hot key
     *         in the selected display
     */
    private boolean isHotKeySubsetInSelectedDisplay(int slotIndexToCheck) {
        int selectedDisplayIndex = view.getDisplayIds().getSelectedIndex();
        List<Key> keys = model.getSlot(selectedDisplayIndex, slotIndexToCheck).getHotKey().getKeys();

        if (keys.size() > 0) {
            for (int slotIndex = 0; slotIndex < maxNumOfSlots; slotIndex++) {
                if (slotIndex != slotIndexToCheck) {
                    List<Key> currentKeys = model.getSlot(selectedDisplayIndex, slotIndex).getHotKey().getKeys();

                    if (currentKeys.size() > 0) {
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
     * Checks if the hot key for the specified slot in the selected display is a subset of another hot key in another
     * display.
     * 
     * @param slotIndexToCheck
     *            - The index of the slot containing the hot key to check
     * 
     * @return Whether or not the hot key for the specified slot in the selected display is a subset of another hot key
     *         in another display
     */
    private boolean isHotKeySubsetInAnotherDisplay(int slotIndexToCheck) {
        int selectedDisplayIndex = view.getDisplayIds().getSelectedIndex();
        List<Key> keysToCheck = model.getSlot(selectedDisplayIndex, slotIndexToCheck).getHotKey().getKeys();

        if (keysToCheck.size() > 0) {
            for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
                if (displayIndex != selectedDisplayIndex) {
                    for (int slotIndex = 0; slotIndex < maxNumOfSlots; slotIndex++) {
                        List<Key> currentKeys = model.getSlot(displayIndex, slotIndex).getHotKey().getKeys();

                        if (currentKeys.size() > 0) {
                            /*
                             * Allow hot keys to be the same between displays so the user can change display settings
                             * for multiple displays with one hot key
                             */
                            if (!keysToCheck.equals(currentKeys)) {
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
     * Checks to see if any slots are in the "changing hot key" state.
     * 
     * @return Whether or not any active slots are in the "changing hot key" state
     */
    private boolean changingHotKey() {
        boolean changingHotKeys = false;
        int selectedDisplayIndex = view.getDisplayIds().getSelectedIndex();

        for (int slotIndex = 0; slotIndex < model.getNumOfSlotsForDisplay(selectedDisplayIndex); slotIndex++) {
            changingHotKeys |= (model.getSlot(selectedDisplayIndex, slotIndex).getHotKey().isChangingHotKey());
        }

        return changingHotKeys;
    }

    /**
     * Sets the display settings if the display is connected.
     * 
     * @param displayIndex
     *            - The index of the display to set the display settings for
     * @param slotIndex
     *            - The index of the slot to set the display settings for
     */
    private void setDisplaySettings(int displayIndex, int slotIndex) {
        DisplayConfig displayConfig = new DisplayConfig();
        displayConfig.updateDisplayIds();

        String displayId = model.getDisplayIds()[displayIndex];

        // If the connected displays have not changed
        if (Arrays.equals(model.getDisplayIds(), displayConfig.getDisplayIds())) {
            setDisplay.applyDisplaySettings(displayId,
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getWidth(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getHeight(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getBitDepth(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getRefreshRate(),
                    model.getSlot(displayIndex, slotIndex).getScalingMode(),
                    model.getSlot(displayIndex, slotIndex).getDpiScalePercentage());

            // Re-initialize the app to prevent window corruption
            appRefresher.reInitApp();
        }
    }

    /**
     * Leaves the "changing hot key" state for the specified slot's hot key.
     * 
     * @param slotIndex
     *            - The slot index for the hot key that is leaving the "changing hot key" state
     */
    private void leaveChangingHotKeyState(int slotIndex) {
        currentKeyCount = 0;

        int selectedDisplayIndex = view.getDisplayIds().getSelectedIndex();
        String displayId = model.getDisplayIds()[selectedDisplayIndex];
        int slotId = slotIndex + 1;

        // If the user did not type any keys before the idle timeout or any hot key is a subset of another
        if (model.getSlot(selectedDisplayIndex, slotIndex).getHotKey().getKeys().size() == 0 || anyHotKeySubset) {
            if (anyHotKeySubset) {
                view.getSlot(selectedDisplayIndex, slotIndex).getChangeHotKeyButton().setText(HOT_KEY_NOT_SET_TEXT);
            }

            model.getSlot(selectedDisplayIndex, slotIndex).setHotKey(hotKeyBackup);
            view.getSlot(selectedDisplayIndex, slotIndex).getHotKey()
                    .setText(model.getSlot(selectedDisplayIndex, slotIndex).getHotKey().getHotKeyString());
            anyHotKeySubset = false;

            frameUpdater.updateUI();
        } else {
            view.getSlot(selectedDisplayIndex, slotIndex).getChangeHotKeyButton().setText(HOT_KEY_SET_TEXT);
        }

        model.getSlot(selectedDisplayIndex, slotIndex).getHotKey().setChangingHotKey(false);
        settingsMgr.saveIniSlotHotKey(displayId, slotId, model.getSlot(selectedDisplayIndex, slotIndex).getHotKey());

        if (!showReleaseMessage) {
            view.getSlot(selectedDisplayIndex, slotIndex).getChangeHotKeyButton().setText(CHANGE_HOT_KEY_TEXT);

            enableComponents();
        }
    }

    /**
     * Starts a timer to stop displaying the release message after attempting to change a hot key.
     * 
     * @param milliseconds
     *            - The timeout value
     * @param slotIndex
     *            - The index of the slot for the potentially changed hot key
     */
    private void startReleaseMessageTimer(int milliseconds, int slotIndex) {
        releaseMessageTimer = new Timer(milliseconds, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showReleaseMessage = false;
                int selectedDisplayIndex = view.getDisplayIds().getSelectedIndex();

                view.getSlot(selectedDisplayIndex, slotIndex).getChangeHotKeyButton().setText(CHANGE_HOT_KEY_TEXT);

                enableComponents();
            }
        });

        releaseMessageTimer.setRepeats(false);
        releaseMessageTimer.start();
    }

    /**
     * Starts a timer to leave the "changing hot key" state when the user is idle while changing a hot key.
     * 
     * @param milliseconds
     *            - The timeout value
     * @param slotIndex
     *            - The index of the slot for the hot key to change
     */
    private void startIdleTimer(int milliseconds, int slotIndex) {
        idleTimer = new Timer(milliseconds, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leaveChangingHotKeyState(slotIndex);
            }
        });

        idleTimer.setRepeats(false);
        idleTimer.start();
    }

    /**
     * Disables all interactive view components except the exit button component to avoid unintended selection during
     * changing the hot key.
     */
    private void disableComponents() {
        view.getDisplayIds().setEnabled(false);
        view.getPaypalDonateButton().setEnabled(false);
        view.getThemeButton().setEnabled(false);
        view.getMinimizeToTrayButton().setEnabled(false);
        view.getRunOnStartupButton().setEnabled(false);
        view.getRefreshAppButton().setEnabled(false);
        view.getClearAllButton().setEnabled(false);
        view.getMinimizeButton().setEnabled(false);
        view.getExitButton().setEnabled(false);

        for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
            view.getNumberOfActiveSlots(displayIndex).setEnabled(false);
            view.getOrientationModes(displayIndex).setEnabled(false);

            for (int slotIndex = 0; slotIndex < model.getNumOfSlotsForDisplay(displayIndex); slotIndex++) {
                view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton().setEnabled(false);
                view.getSlot(displayIndex, slotIndex).getDisplayModes().setEnabled(false);
                view.getSlot(displayIndex, slotIndex).getScalingModes().setEnabled(false);
                view.getSlot(displayIndex, slotIndex).getDpiScalePercentages().setEnabled(false);
                view.getSlot(displayIndex, slotIndex).getClearHotKeyButton().setEnabled(false);
                view.getSlot(displayIndex, slotIndex).getChangeHotKeyButton().setEnabled(false);
            }
        }
    }

    /**
     * Re-enables all interactive view components except the exit button component because it is already enabled.
     */
    private void enableComponents() {
        view.getDisplayIds().setEnabled(true);
        view.getPaypalDonateButton().setEnabled(true);
        view.getThemeButton().setEnabled(true);
        view.getMinimizeToTrayButton().setEnabled(true);
        view.getRunOnStartupButton().setEnabled(true);
        view.getRefreshAppButton().setEnabled(true);
        view.getClearAllButton().setEnabled(true);
        view.getMinimizeButton().setEnabled(true);
        view.getExitButton().setEnabled(true);

        for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
            view.getNumberOfActiveSlots(displayIndex).setEnabled(true);
            view.getOrientationModes(displayIndex).setEnabled(true);

            for (int slotIndex = 0; slotIndex < model.getNumOfSlotsForDisplay(displayIndex); slotIndex++) {
                view.getSlot(displayIndex, slotIndex).getApplyDisplayModeButton().setEnabled(true);
                view.getSlot(displayIndex, slotIndex).getDisplayModes().setEnabled(true);
                view.getSlot(displayIndex, slotIndex).getScalingModes().setEnabled(true);
                view.getSlot(displayIndex, slotIndex).getDpiScalePercentages().setEnabled(true);

                // Enable the Clear Hot Key button after getting user input if the hot key is set
                if (model.getSlot(displayIndex, slotIndex).getHotKey().getKeys().size() > 0) {
                    view.getSlot(displayIndex, slotIndex).getClearHotKeyButton().setEnabled(true);
                }

                view.getSlot(displayIndex, slotIndex).getChangeHotKeyButton().setEnabled(true);
            }
        }
    }

}