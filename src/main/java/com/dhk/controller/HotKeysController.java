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
package com.dhk.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.dhk.io.DisplayConfig;
import com.dhk.io.KeyText;
import com.dhk.io.NativeKeyEvent;
import com.dhk.io.NativeKeyListener;
import com.dhk.io.SetDisplay;
import com.dhk.io.SettingsManager;
import com.dhk.main.AppRefresher;
import com.dhk.model.DhkModel;
import com.dhk.model.FramePlacement;
import com.dhk.model.HotKey;
import com.dhk.model.Key;
import com.dhk.model.button.Button;
import com.dhk.utility.FrameUtil;
import com.dhk.view.DhkView;
import com.dhk.view.AdditionalSettingsDialog;

/**
 * Controls the hot keys for the application. Listeners are added to the Change Hot Key buttons to enable the
 * functionality of changing hot keys. This class is also responsible for triggering hot key events once a hot key press
 * is detected, which applies a slot's display settings or shows the applied settings of every display.
 *
 * @author Jonathan R. Miller
 */
public class HotKeysController implements IController, NativeKeyListener {

    private DhkView view;
    private DhkModel model;
    private DhkController controller;
    private SettingsManager settingsMgr;
    private DisplayConfig displayConfig;
    private SetDisplay setDisplay;
    private HotKey hotKeyBackup;
    private RecordingTarget recordingTarget;
    private Timer idleTimer;
    private Timer releaseMessageTimer;
    private Timer reInitTimer;
    private AppRefresher appRefresher;
    private int currentKeyCount;
    private int maxNumOfSlots;
    private boolean showReleaseMessage;
    private boolean anyHotKeySubset;
    private volatile boolean anyHotKeyChanging;
    private volatile Set<Integer> activeKeyCodes;
    private HeldKeyTracker heldKeyTracker;

    private static final String CHANGE_HOT_KEY_TEXT = "Change Hot Key";
    private static final String PRESS_HOT_KEY_TEXT = "Press Hot Key";
    private static final String RELEASE_TO_SET_TEXT = "Release To Set";
    private static final String NO_SUBSETS_TEXT = "No Subsets";
    private static final String HOT_KEY_SET_TEXT = "Hot Key Set";
    private static final String HOT_KEY_NOT_SET_TEXT = "Hot Key Not Set";
    private static final int IDLE_INPUT_TIMEOUT = 2500;
    private static final int RELEASE_MESSAGE_TIMEOUT = 1500;
    private static final int MAX_KEY_COUNT = 3;

    /**
     * Constructor for the {@link HotKeysController} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param controller
     *            - The controller for the application
     * @param settingsMgr
     *            - The settings manager for the application
     * @param heldKeyTracker
     *            - The tracker of physically held keys, shared across re-initializations so held keys are not lost
     */
    public HotKeysController(DhkModel model, DhkView view, DhkController controller, SettingsManager settingsMgr,
            HeldKeyTracker heldKeyTracker) {
        this.model = model;
        this.view = view;
        this.controller = controller;
        this.settingsMgr = settingsMgr;
        this.heldKeyTracker = heldKeyTracker;
    }

    @Override
    public void initController() {
        currentKeyCount = 0;
        maxNumOfSlots = settingsMgr.getMaxNumOfSlots();
        setDisplay = new SetDisplay();
        showReleaseMessage = false;
        anyHotKeySubset = false;
        anyHotKeyChanging = false;
        rebuildActiveKeyCodes();
        seedHeldKeyStates();
        displayConfig = settingsMgr.getDisplayConfig();
        appRefresher = new AppRefresher(model, view, controller, settingsMgr);
    }

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
        // Stop and null any timers to prevent leaked Swing Timer threads/listeners
        if (idleTimer != null) {
            idleTimer.stop();
            idleTimer = null;
        }

        if (releaseMessageTimer != null) {
            releaseMessageTimer.stop();
            releaseMessageTimer = null;
        }

        // A refresh is tearing this controller down, so a still-pending refresh it scheduled would be a duplicate
        if (reInitTimer != null) {
            reInitTimer.stop();
            reInitTimer = null;
        }

        // A recording cut short by a refresh is discarded, since the refreshed model reloads the saved hot keys
        recordingTarget = null;
        anyHotKeyChanging = false;
    }

    @Override
    public void keyPressed(NativeKeyEvent keyEvent) {
        /*
         * If this key event is not relevant to any hot key and no hot key is currently being changed, skip scheduling
         * work on the EDT entirely
         */
        if (keyEvent == null || !isKeyEventRelevant(keyEvent.getVirtualKeyCode())) {
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Map<Integer, Integer> displayToSlotMap = new HashMap<>();

                for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
                    for (int slotIndex = 0; slotIndex < maxNumOfSlots; slotIndex++) {
                        HotKey slotHotKey = model.getSlot(displayIndex, slotIndex).getHotKey();

                        setPressedKeys(keyEvent, slotHotKey.getKeys());
                        updateHotKeyState(slotHotKey);

                        // If no hot key is being changed, and an active slot's hot key is pressed but not held down
                        if (!showReleaseMessage && !anyHotKeyChanging
                                && slotIndex < model.getNumOfSlotsForDisplay(displayIndex)
                                && slotHotKey.isHotKeyPressed() && !slotHotKey.isHotKeyHeldDown()) {
                            /*
                             * Defer applying settings until after we finish scanning all slots to ensure multiple
                             * displays using the same hot key are all applied
                             */
                            displayToSlotMap.put(displayIndex, slotIndex);
                        }
                    }
                }

                HotKey displayInfoHotKey = model.getDisplayInfoHotKey();

                setPressedKeys(keyEvent, displayInfoHotKey.getKeys());
                updateHotKeyState(displayInfoHotKey);

                if (!showReleaseMessage && recordingTarget != null) {
                    buildHotKey(keyEvent);

                    if (anyHotKeyConflict()) {
                        // Notify the user that hot keys cannot be a subset of another hot key
                        recordingTarget.getChangeHotKeyButton().setText(NO_SUBSETS_TEXT);

                        anyHotKeySubset = true;
                    } else {
                        // Update the Change Hot Key button text to notify the user to release keys
                        recordingTarget.getChangeHotKeyButton().setText(RELEASE_TO_SET_TEXT);

                        anyHotKeySubset = false;
                    }
                }

                // Show the applied settings of every display once the Display Info hot key is pressed but not held down
                if (!showReleaseMessage && !anyHotKeyChanging && displayInfoHotKey.isHotKeyPressed()
                        && !displayInfoHotKey.isHotKeyHeldDown()) {
                    controller.getDisplayInfoOverlayView().show(model);
                }

                // Capture the frame placement before any display reconfiguration relocates the window
                FramePlacement placement = displayToSlotMap.isEmpty()
                        ? null
                        : FrameUtil.capturePlacement(view.getFrame());

                // Apply settings for all collected slots, capturing the arrangement first so it is reflowed once after
                boolean displaySettingsApplied = false;

                if (!displayToSlotMap.isEmpty()) {
                    // Dismiss the tray menu and display info panels, since both were placed against the old geometry
                    controller.displayConfigurationChanged();

                    String[] arrangementSnapshot = displayConfig.captureArrangement();

                    for (Entry<Integer, Integer> displayToSlot : displayToSlotMap.entrySet()) {
                        displaySettingsApplied |= setDisplaySettings(displayToSlot.getKey(), displayToSlot.getValue());
                    }

                    // Reflow once, after every targeted display has been resized, so their arrangement is preserved
                    if (displaySettingsApplied) {
                        setDisplay.preserveArrangement(arrangementSnapshot);
                    }
                }

                // Re-initialize the app once, after every targeted display has been updated
                if (displaySettingsApplied) {
                    scheduleReInit(placement);
                }
            }
        });
    }

    @Override
    public void keyReleased(NativeKeyEvent keyEvent) {
        /*
         * If this key event is not relevant to any hot key and no hot key is currently being changed, skip scheduling
         * work on the EDT entirely
         */
        if (keyEvent == null || !isKeyEventRelevant(keyEvent.getVirtualKeyCode())) {
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
                    for (int slotIndex = 0; slotIndex < maxNumOfSlots; slotIndex++) {
                        setReleasedKeys(keyEvent, model.getSlot(displayIndex, slotIndex).getHotKey().getKeys());
                        updateHotKeyState(model.getSlot(displayIndex, slotIndex).getHotKey());
                    }
                }

                setReleasedKeys(keyEvent, model.getDisplayInfoHotKey().getKeys());
                updateHotKeyState(model.getDisplayInfoHotKey());

                // If the user releases the hot key during the "changing hot key" state
                if (recordingTarget != null && !recordingTarget.getHotKey().isHotKeyPressed()) {
                    RecordingTarget releasedTarget = recordingTarget;
                    showReleaseMessage = true;

                    leaveChangingHotKeyState();
                    startReleaseMessageTimer(RELEASE_MESSAGE_TIMEOUT, releasedTarget);
                }
            }
        });
    }

    /**
     * Lightweight check to determine whether the given key code is relevant to any hot key (either already set or
     * currently being changed). This method performs minimal work and avoids allocations so it can be called directly
     * from the global key thread.
     *
     * @param eventKeyCode
     *            - The key code for the key event that we are checking
     *
     * @return Whether the key event can affect a hot key
     */
    private boolean isKeyEventRelevant(int eventKeyCode) {
        // If any hot key is in the process of being changed we must handle all key events
        if (anyHotKeyChanging) {
            return true;
        }

        // Read the published set once, since a rebuild on another thread can swap it in partway through this check
        Set<Integer> keyCodes = activeKeyCodes;

        // Otherwise, use a cached set for O(1) membership checks
        return keyCodes != null && keyCodes.contains(eventKeyCode);
    }

    /**
     * Rebuild the cached set of active key codes from the model. Call this whenever hot keys change.
     */
    public void rebuildActiveKeyCodes() {
        /*
         * The native reporting thread reads this set without locking, so it is rebuilt into a new instance and
         * published in a single assignment. Mutating the live set in place could resize it mid-read and hang that
         * thread
         */
        Set<Integer> rebuiltKeyCodes = new HashSet<>();

        for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
            for (int slotIndex = 0; slotIndex < maxNumOfSlots; slotIndex++) {
                List<Key> keys = model.getSlot(displayIndex, slotIndex).getHotKey().getKeys();

                for (int k = 0; k < keys.size(); k++) {
                    rebuiltKeyCodes.add(keys.get(k).getKey());
                }
            }
        }

        List<Key> displayInfoKeys = model.getDisplayInfoHotKey().getKeys();

        for (int k = 0; k < displayInfoKeys.size(); k++) {
            rebuiltKeyCodes.add(displayInfoKeys.get(k).getKey());
        }

        activeKeyCodes = rebuiltKeyCodes;
    }

    /**
     * Restores the physically held key states onto the freshly rebuilt model so a hot key can match again without
     * releasing every key. A fully held combo is marked held down so re-seeding never re-applies it on the next event.
     */
    private void seedHeldKeyStates() {
        for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
            for (int slotIndex = 0; slotIndex < maxNumOfSlots; slotIndex++) {
                seedHeldKeyState(model.getSlot(displayIndex, slotIndex).getHotKey());
            }
        }

        seedHeldKeyState(model.getDisplayInfoHotKey());
    }

    /**
     * Restores the physically held key states onto the given hot key, marking a fully held combo as held down so it is
     * not fired again on the next event.
     *
     * @param hotKey
     *            - The hot key to restore the held key states onto
     */
    private void seedHeldKeyState(HotKey hotKey) {
        List<Key> keys = hotKey.getKeys();
        boolean allKeysPressed = keys.size() > 0;

        for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
            boolean keyHeld = heldKeyTracker.isKeyHeld(keys.get(keyIndex).getKey());
            keys.get(keyIndex).setKeyPressed(keyHeld);
            allKeysPressed &= keyHeld;
        }

        hotKey.setHotKeyPressed(allKeysPressed);
        hotKey.setHotKeyHeldDown(allKeysPressed);
    }

    /**
     * Starts recording a new hot key for the specified slot.
     *
     * @param displayIndex
     *            - The index of the display to update the hot key for
     * @param slotIndex
     *            - The index of the slot to change the hot key for
     */
    private void slotHotKeyChangeEvent(int displayIndex, int slotIndex) {
        beginHotKeyChange(new RecordingTarget(displayIndex, slotIndex));
    }

    /**
     * Starts recording a new Display Info hot key from the given additional settings dialog, which shows the hot key
     * and holds the controls that are disabled while it is recorded.
     *
     * @param additionalSettingsDialog
     *            - The additional settings dialog the Display Info hot key is being changed from
     */
    public void displayInfoHotKeyChangeEvent(AdditionalSettingsDialog additionalSettingsDialog) {
        beginHotKeyChange(new RecordingTarget(additionalSettingsDialog));
    }

    /**
     * Starts recording a new hot key for the given target, backing up its current hot key so a recording that ends
     * without a usable hot key restores it.
     *
     * @param target
     *            - The slot or Display Info hot key to record a new hot key for
     */
    private void beginHotKeyChange(RecordingTarget target) {
        // Do not allow changing multiple hot keys at the same time
        if (recordingTarget != null || showReleaseMessage) {
            return;
        }

        // Reset the key counter so a new hot key starts fresh, even if a prior change ended without finalizing
        currentKeyCount = 0;
        recordingTarget = target;

        hotKeyBackup = new HotKey(new ArrayList<Key>());
        hotKeyBackup.getKeys().addAll(target.getHotKey().getKeys());

        target.getHotKey().setKeys(new ArrayList<Key>());
        target.getChangeHotKeyButton().setText(PRESS_HOT_KEY_TEXT);
        target.showRecording(true);
        target.disableControls();
        target.getHotKey().setChangingHotKey(true);

        // Every hot key, including the slot hot keys, is suppressed until the recording ends
        anyHotKeyChanging = true;

        startIdleTimer(IDLE_INPUT_TIMEOUT);
    }

    /**
     * Sets hot key keys as pressed if the given key event matches a key in the hot key.
     *
     * @param keyEvent
     *            - The key event that occurred
     * @param keys
     *            - The array list of keys for a hot key
     */
    private void setPressedKeys(NativeKeyEvent keyEvent, List<Key> keys) {
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
    private void setReleasedKeys(NativeKeyEvent keyEvent, List<Key> keys) {
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
     * Adds the pressed key to the hot key being recorded, unless the hot key already holds it or is full.
     *
     * @param keyEvent
     *            - The native key event that was fired
     */
    private void buildHotKey(NativeKeyEvent keyEvent) {
        if (idleTimer != null) {
            idleTimer.stop();
        }

        Key pressedKey;
        int keyCode = keyEvent.getVirtualKeyCode();
        String keyCodeText = KeyText.getKeyCodeText(keyCode);
        HotKey hotKey = recordingTarget.getHotKey();

        if (currentKeyCount < MAX_KEY_COUNT) {
            pressedKey = new Key(keyCode, keyCodeText, true);

            // Only allow unique keys to form the hot key
            if (!hotKey.getKeys().contains(pressedKey)) {
                hotKey.getKeys().add(pressedKey);

                recordingTarget.showHotKeyText();

                // Update cached active key codes, republishing a copy so the reporting thread never reads a live edit
                Set<Integer> updatedKeyCodes = (activeKeyCodes == null)
                        ? new HashSet<>()
                        : new HashSet<>(activeKeyCodes);

                updatedKeyCodes.add(pressedKey.getKey());

                activeKeyCodes = updatedKeyCodes;

                currentKeyCount += 1;
            }
        }
    }

    /**
     * Checks whether the hot key being recorded conflicts with another hot key. The Display Info hot key conflicts with
     * any slot hot key it overlaps, and a slot hot key also conflicts with the Display Info hot key it overlaps.
     *
     * @return Whether the hot key being recorded conflicts with another hot key
     */
    private boolean anyHotKeyConflict() {
        List<Key> recordedKeys = recordingTarget.getHotKey().getKeys();

        // One press must never both apply a slot and show the display info, so no overlap is allowed either way
        if (recordingTarget.isDisplayInfo()) {
            return overlapsAnySlotHotKey(recordedKeys);
        }

        return anyHotKeySubset() || overlapsHotKey(recordedKeys, model.getDisplayInfoHotKey().getKeys());
    }

    /**
     * Checks whether the given keys overlap the hot key of any slot of any display, active or not.
     *
     * @param keys
     *            - The keys to check
     *
     * @return Whether the given keys overlap any slot hot key
     */
    private boolean overlapsAnySlotHotKey(List<Key> keys) {
        for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
            for (int slotIndex = 0; slotIndex < maxNumOfSlots; slotIndex++) {
                if (overlapsHotKey(keys, model.getSlot(displayIndex, slotIndex).getHotKey().getKeys())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks whether two hot keys overlap, which they do when both are set and either holds every key of the other, so
     * an equal hot key, a subset, and a superset all overlap.
     *
     * @param keys
     *            - The keys of the first hot key
     * @param otherKeys
     *            - The keys of the second hot key
     *
     * @return Whether the two hot keys overlap
     */
    private boolean overlapsHotKey(List<Key> keys, List<Key> otherKeys) {
        return !keys.isEmpty() && !otherKeys.isEmpty() && (keys.containsAll(otherKeys) || otherKeys.containsAll(keys));
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
                            if (!isSameHotKey(keysToCheck, currentKeys)) {
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
     * Checks whether two hot keys hold the same keys, regardless of the order they were recorded in, since the same
     * combination is the same hot key however it was pressed.
     *
     * @param keys
     *            - The keys of the first hot key
     * @param otherKeys
     *            - The keys of the second hot key
     *
     * @return Whether both hot keys hold the same keys
     */
    private boolean isSameHotKey(List<Key> keys, List<Key> otherKeys) {
        return keys.size() == otherKeys.size() && keys.containsAll(otherKeys);
    }

    /**
     * Sets the display settings if the display is connected. Does not refresh the app; the caller is responsible for
     * triggering a single app refresh after all targeted displays have been updated.
     *
     * @param displayIndex
     *            - The index of the display to set the display settings for
     * @param slotIndex
     *            - The index of the slot to set the display settings for
     * @return Whether the display settings were applied (the display is still connected and unchanged)
     */
    private boolean setDisplaySettings(int displayIndex, int slotIndex) {
        displayConfig.updateConnectedDisplays();

        String displayId = model.getDisplayIds()[displayIndex];

        // If the connected displays have not changed
        if (Arrays.equals(model.getDisplayIds(), displayConfig.getDisplayIds())) {
            setDisplay.applyDisplayOrientation(displayId, model.getSlot(displayIndex, slotIndex).getOrientationMode());
            setDisplay.applyDisplaySettings(displayId,
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getWidth(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getHeight(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getRefreshNumerator(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getRefreshDenominator(),
                    model.getSlot(displayIndex, slotIndex).getDpiScalePercentage(),
                    model.getSlot(displayIndex, slotIndex).getScalingMode());

            return true;
        }

        return false;
    }

    /**
     * Schedules a single, deferred re-initialization of the app after display settings are applied. The refresh is
     * delayed so the asynchronous display reconfiguration settles first, and rapid successive applies coalesce into one
     * refresh that reproduces the placement captured before the first apply.
     *
     * @param placement
     *            - The frame placement captured before applying the display settings, reproduced after
     *            re-initialization because the OS will have moved the existing frame during the reconfiguration
     */
    private void scheduleReInit(FramePlacement placement) {
        // Restarting extends the settle delay past the latest apply; the pending timer keeps the earlier placement
        if (reInitTimer != null && reInitTimer.isRunning()) {
            reInitTimer.restart();
            return;
        }

        reInitTimer = new Timer(FrameUtil.REFRESH_DELAY_MS, e -> appRefresher.reInitApp(placement));
        reInitTimer.setRepeats(false);
        reInitTimer.start();
    }

    /**
     * Leaves the "changing hot key" state for the hot key being recorded, keeping the recorded hot key when it is
     * usable and restoring the backed up hot key otherwise, then saves whichever hot key results.
     */
    private void leaveChangingHotKeyState() {
        currentKeyCount = 0;

        if (idleTimer != null) {
            idleTimer.stop();
            idleTimer = null;
        }

        RecordingTarget target = recordingTarget;

        if (target == null) {
            return;
        }

        // If the user did not type any keys before the idle timeout or any hot key is a subset of another
        if (target.getHotKey().getKeys().size() == 0 || anyHotKeySubset) {
            if (anyHotKeySubset) {
                target.getChangeHotKeyButton().setText(HOT_KEY_NOT_SET_TEXT);
            }

            target.setHotKey(hotKeyBackup);
            target.showHotKeyText();
            anyHotKeySubset = false;
        } else {
            target.getChangeHotKeyButton().setText(HOT_KEY_SET_TEXT);
        }

        target.getHotKey().setChangingHotKey(false);
        target.showRecording(false);
        recordingTarget = null;
        anyHotKeyChanging = false;

        target.saveHotKey();

        // Hot key definitions may have changed; rebuild the membership cache
        rebuildActiveKeyCodes();

        if (!showReleaseMessage) {
            target.getChangeHotKeyButton().setText(CHANGE_HOT_KEY_TEXT);
            target.enableControls();
        }
    }

    /**
     * Starts a timer to stop displaying the release message after attempting to change a hot key.
     *
     * @param milliseconds
     *            - The timeout value
     * @param target
     *            - The slot or Display Info hot key that was potentially changed
     */
    private void startReleaseMessageTimer(int milliseconds, RecordingTarget target) {
        releaseMessageTimer = new Timer(milliseconds, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showReleaseMessage = false;

                target.getChangeHotKeyButton().setText(CHANGE_HOT_KEY_TEXT);
                target.enableControls();
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
     */
    private void startIdleTimer(int milliseconds) {
        idleTimer = new Timer(milliseconds, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leaveChangingHotKeyState();
            }
        });

        idleTimer.setRepeats(false);
        idleTimer.start();
    }

    /**
     * Disables all interactive view components to avoid unintended selection during changing the hot key.
     */
    private void disableComponents() {
        // Ensure we run on the EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::disableComponents);
            return;
        }

        if (view.getFrame() == null) {
            return;
        }

        // Prevent the window from accepting input and causing focus traversal while we toggle components
        view.getFrame().setEnabled(false);

        // Disable top-level controls
        view.getDisplayIds().setEnabled(false);

        // Disable custom buttons
        for (Button button : view.getButtons()) {
            button.setEnabled(false);
        }

        // Disable slot controls
        for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
            view.getNumberOfActiveSlots(displayIndex).setEnabled(false);

            for (int slotIndex = 0; slotIndex < model.getNumOfSlotsForDisplay(displayIndex); slotIndex++) {
                view.getSlot(displayIndex, slotIndex).getResolutions().setEnabled(false);
                view.getSlot(displayIndex, slotIndex).getRefreshRates().setEnabled(false);
                view.getSlot(displayIndex, slotIndex).getDpiScalePercentages().setEnabled(false);
                view.getSlot(displayIndex, slotIndex).getScalingModes().setEnabled(false);
                view.getSlot(displayIndex, slotIndex).getOrientationModes().setEnabled(false);
                view.getSlot(displayIndex, slotIndex).getChangeHotKeyButton().setEnabled(false);
            }
        }

        // One consolidated refresh to avoid incremental repaints
        view.getFrame().revalidate();
        view.getFrame().repaint();
    }

    /**
     * Re-enables all interactive view components.
     */
    private void enableComponents() {
        // Ensure we run on the EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::enableComponents);
            return;
        }

        // Enable top-level controls
        view.getDisplayIds().setEnabled(true);

        // Enable custom buttons
        for (Button button : view.getButtons()) {
            button.setEnabled(true);
        }

        // Enable slot controls
        for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
            view.getNumberOfActiveSlots(displayIndex).setEnabled(true);

            for (int slotIndex = 0; slotIndex < model.getNumOfSlotsForDisplay(displayIndex); slotIndex++) {
                view.getSlot(displayIndex, slotIndex).getResolutions().setEnabled(true);
                view.getSlot(displayIndex, slotIndex).getRefreshRates().setEnabled(true);
                view.getSlot(displayIndex, slotIndex).getDpiScalePercentages().setEnabled(true);
                view.getSlot(displayIndex, slotIndex).getScalingModes().setEnabled(true);
                view.getSlot(displayIndex, slotIndex).getOrientationModes().setEnabled(true);

                JButton changeHotKeyButton = view.getSlot(displayIndex, slotIndex).getChangeHotKeyButton();
                changeHotKeyButton.setEnabled(true);

                // A button re-enabled under the cursor gets no mouseEntered, so restore rollover from the pointer
                changeHotKeyButton.getModel().setRollover(changeHotKeyButton.getMousePosition() != null);

                // Enable the Clear Hot Key button only if a hot key is set for this slot
                if (model.getSlot(displayIndex, slotIndex).getHotKey().getKeys().size() > 0) {
                    view.getSlot(displayIndex, slotIndex).getClearHotKeyButton().setEnabled(true);
                } else {
                    view.getSlot(displayIndex, slotIndex).getClearHotKeyButton().setEnabled(false);
                }
            }
        }

        // Re-enable the frame last so focus changes happen after component states are stable
        if (view.getFrame() != null) {
            view.getFrame().setEnabled(true);
            view.getFrame().revalidate();
            view.getFrame().repaint();
        }
    }

    /**
     * The hot key being recorded, which is either a slot's hot key shown in the main window or the Display Info hot key
     * shown in the additional settings dialog. It hides where the hot key lives so one recording flow serves both.
     *
     * @author Jonathan R. Miller
     */
    private class RecordingTarget {

        private int displayIndex;
        private int slotIndex;
        private AdditionalSettingsDialog additionalSettingsDialog;

        /**
         * Constructor for the {@link RecordingTarget} class, targeting the hot key of a slot.
         *
         * @param displayIndex
         *            - The index of the display the slot belongs to
         * @param slotIndex
         *            - The index of the slot
         */
        RecordingTarget(int displayIndex, int slotIndex) {
            this.displayIndex = displayIndex;
            this.slotIndex = slotIndex;
        }

        /**
         * Constructor for the {@link RecordingTarget} class, targeting the Display Info hot key.
         *
         * @param additionalSettingsDialog
         *            - The additional settings dialog showing the Display Info hot key
         */
        RecordingTarget(AdditionalSettingsDialog additionalSettingsDialog) {
            this.additionalSettingsDialog = additionalSettingsDialog;
        }

        /**
         * Checks whether the target is the Display Info hot key rather than a slot's hot key.
         *
         * @return Whether the target is the Display Info hot key
         */
        boolean isDisplayInfo() {
            return additionalSettingsDialog != null;
        }

        /**
         * Gets the hot key being recorded.
         *
         * @return The hot key being recorded
         */
        HotKey getHotKey() {
            return isDisplayInfo() ? model.getDisplayInfoHotKey() : model.getSlot(displayIndex, slotIndex).getHotKey();
        }

        /**
         * Replaces the hot key being recorded, which is how a backed up hot key is restored.
         *
         * @param hotKey
         *            - The hot key to put in place
         */
        void setHotKey(HotKey hotKey) {
            if (isDisplayInfo()) {
                model.setDisplayInfoHotKey(hotKey);
            } else {
                model.getSlot(displayIndex, slotIndex).setHotKey(hotKey);
            }
        }

        /**
         * Gets the Change Hot Key button that started the recording and shows its progress.
         *
         * @return The Change Hot Key button
         */
        JButton getChangeHotKeyButton() {
            return isDisplayInfo()
                    ? additionalSettingsDialog.getChangeHotKeyButton()
                    : view.getSlot(displayIndex, slotIndex).getChangeHotKeyButton();
        }

        /**
         * Shows the hot key's current keys where the hot key is displayed.
         */
        void showHotKeyText() {
            if (isDisplayInfo()) {
                additionalSettingsDialog.setDisplayInfoHotKeyText(getHotKey().getHotKeyString());
                return;
            }

            view.getSlot(displayIndex, slotIndex).getHotKey().setText(getHotKey().getHotKeyString());
            FrameUtil.refreshFrame(view.getFrame());
        }

        /**
         * Shows whether the hot key is being recorded by outlining it in the theme's focus color while it is.
         *
         * @param recording
         *            - Whether the hot key is being recorded
         */
        void showRecording(boolean recording) {
            if (isDisplayInfo()) {
                additionalSettingsDialog.setDisplayInfoHotKeyRecording(recording);
            } else {
                view.getSlot(displayIndex, slotIndex).getHotKey().setRecording(recording);
            }
        }

        /**
         * Saves the hot key to the settings file.
         */
        void saveHotKey() {
            if (isDisplayInfo()) {
                settingsMgr.saveIniDisplayInfoHotKey(getHotKey());
            } else {
                settingsMgr.saveIniSlotHotKey(model.getDisplayIds()[displayIndex], slotIndex + 1, getHotKey());
            }
        }

        /**
         * Disables the controls around the hot key so nothing can be changed while it is recorded.
         */
        void disableControls() {
            if (isDisplayInfo()) {
                additionalSettingsDialog.setControlsEnabled(false);
            } else {
                disableComponents();
            }
        }

        /**
         * Re-enables the controls around the hot key once its recording has finished.
         */
        void enableControls() {
            if (isDisplayInfo()) {
                additionalSettingsDialog.setControlsEnabled(true);
            } else {
                enableComponents();
            }
        }

    }

}
