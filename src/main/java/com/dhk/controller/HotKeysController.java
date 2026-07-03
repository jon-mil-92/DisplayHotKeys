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

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.dhk.io.DisplayConfig;
import com.dhk.io.KeyText;
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

import lc.kra.system.keyboard.event.GlobalKeyEvent;
import lc.kra.system.keyboard.event.GlobalKeyListener;

/**
 * Controls the hot keys for the application. Listeners are added to the Change Hot Key buttons to enable the
 * functionality of changing hot keys. This class is also responsible for triggering hot key events once a hot key press
 * is detected.
 *
 * @author Jonathan R. Miller
 */
public class HotKeysController implements IController, GlobalKeyListener {

    private DhkView view;
    private DhkModel model;
    private DhkController controller;
    private SettingsManager settingsMgr;
    private DisplayConfig displayConfig;
    private SetDisplay setDisplay;
    private HotKey hotKeyBackup;
    private Timer idleTimer;
    private Timer releaseMessageTimer;
    private AppRefresher appRefresher;
    private int currentKeyCount;
    private int maxNumOfSlots;
    private boolean showReleaseMessage;
    private boolean anyHotKeySubset;
    private volatile boolean anyHotKeyChanging;
    private Set<Integer> activeKeyCodes;

    private static final String CHANGE_HOT_KEY_TEXT = "Change Hot Key";
    private static final String PRESS_HOT_KEY_TEXT = "Press Hot Key";
    private static final String RELEASE_TO_SET_TEXT = "Release To Set";
    private static final String NO_SUBSETS_TEXT = "No Subsets";
    private static final String HOT_KEY_SET_TEXT = "Hot Key Set";
    private static final String HOT_KEY_NOT_SET_TEXT = "Hot Key Not Set";
    private static final int IDLE_INPUT_TIMEOUT = 2500;
    private static final int RELEASE_MESSAGE_TIMEOUT = 1500;
    private static final int MAX_KEY_COUNT = 3;
    private static final int REINIT_DELAY_MS = 400;

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
     */
    public HotKeysController(DhkModel model, DhkView view, DhkController controller, SettingsManager settingsMgr) {
        this.model = model;
        this.view = view;
        this.controller = controller;
        this.settingsMgr = settingsMgr;
    }

    @Override
    public void initController() {
        currentKeyCount = 0;
        maxNumOfSlots = settingsMgr.getMaxNumOfSlots();
        setDisplay = new SetDisplay();
        showReleaseMessage = false;
        anyHotKeySubset = false;
        anyHotKeyChanging = false;
        activeKeyCodes = new HashSet<>();
        rebuildActiveKeyCodes();
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
    }

    @Override
    public void keyPressed(GlobalKeyEvent keyEvent) {
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
                                        // Update the Change Hot Key button text to notify the user to release keys
                                        view.getSlot(displayIndex, slotIndex).getChangeHotKeyButton()
                                                .setText(RELEASE_TO_SET_TEXT);

                                        anyHotKeySubset = false;
                                    }
                                }

                                // If no hot key is being changed, and a hot key is pressed but not held down
                                if (!anyHotKeyChanging
                                        && model.getSlot(displayIndex, slotIndex).getHotKey().isHotKeyPressed()
                                        && !model.getSlot(displayIndex, slotIndex).getHotKey().isHotKeyHeldDown()) {
                                    /*
                                     * Defer applying settings until after we finish scanning all slots to ensure
                                     * multiple displays using the same hot key are all applied
                                     */
                                    displayToSlotMap.put(displayIndex, slotIndex);
                                }
                            }
                        }
                    }
                }

                // Capture the frame placement before any display reconfiguration relocates the window
                FramePlacement placement = displayToSlotMap.isEmpty()
                        ? null
                        : FrameUtil.capturePlacement(view.getFrame());

                // Apply settings for all collected slots
                boolean displaySettingsApplied = false;

                for (Entry<Integer, Integer> displayToSlot : displayToSlotMap.entrySet()) {
                    displaySettingsApplied |= setDisplaySettings(displayToSlot.getKey(), displayToSlot.getValue());
                }

                // Re-initialize the app once, after every targeted display has been updated
                if (displaySettingsApplied) {
                    scheduleReInit(placement);
                }
            }
        });
    }

    @Override
    public void keyReleased(GlobalKeyEvent keyEvent) {
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
     * Lightweight check to determine whether the given key code is relevant to any hot key (either already set or
     * currently being changed). This method performs minimal work and avoids allocations so it can be called directly
     * from the global key thread.
     *
     * @param eventKeyCode
     *            - The key code for the key event that we are checking
     * @return
     */
    private boolean isKeyEventRelevant(int eventKeyCode) {
        // If any hot key is in the process of being changed we must handle all key events
        if (anyHotKeyChanging) {
            return true;
        }

        // Otherwise, use a cached set for O(1) membership checks
        return activeKeyCodes != null && activeKeyCodes.contains(eventKeyCode);
    }

    /**
     * Rebuild the cached set of active key codes from the model. Call this whenever hot keys change.
     */
    public void rebuildActiveKeyCodes() {
        if (activeKeyCodes == null) {
            activeKeyCodes = new HashSet<>();
        }

        activeKeyCodes.clear();

        for (int displayIndex = 0; displayIndex < model.getNumOfConnectedDisplays(); displayIndex++) {
            for (int slotIndex = 0; slotIndex < maxNumOfSlots; slotIndex++) {
                List<Key> keys = model.getSlot(displayIndex, slotIndex).getHotKey().getKeys();

                for (int k = 0; k < keys.size(); k++) {
                    activeKeyCodes.add(keys.get(k).getKey());
                }
            }
        }
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
            // Reset the key counter so a new hot key starts fresh, even if a prior change ended without finalizing
            currentKeyCount = 0;

            hotKeyBackup = new HotKey(new ArrayList<Key>());
            hotKeyBackup.getKeys().addAll(model.getSlot(displayIndex, slotIndex).getHotKey().getKeys());

            model.getSlot(displayIndex, slotIndex).getHotKey().setKeys(new ArrayList<Key>());
            view.getSlot(displayIndex, slotIndex).getChangeHotKeyButton().setText(PRESS_HOT_KEY_TEXT);

            disableComponents();

            model.getSlot(displayIndex, slotIndex).getHotKey().setChangingHotKey(true);
            anyHotKeyChanging = true;

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
                FrameUtil.refreshFrame(view.getFrame());

                // Update cached active key codes
                if (activeKeyCodes == null) {
                    activeKeyCodes = new HashSet<>();
                }

                activeKeyCodes.add(pressedKey.getKey());

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
     * Sets the display settings if the display is connected. Does not re-initialize the app; the caller is responsible
     * for triggering a single rebuild after all targeted displays have been updated.
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
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getBitDepth(),
                    model.getSlot(displayIndex, slotIndex).getDisplayMode().getRefreshRate(),
                    model.getSlot(displayIndex, slotIndex).getScalingMode(),
                    model.getSlot(displayIndex, slotIndex).getDpiScalePercentage());

            return true;
        }

        return false;
    }

    /**
     * Schedules a single, deferred re-initialization of the app to prevent window corruption after a display mode is
     * applied. Applying a display mode reconfigures the display asynchronously, so the rebuild is delayed briefly to
     * let the new geometry settle; otherwise the rebuilt frame is placed against stale display bounds and jumps up and
     * to the left. The Timer fires once on the EDT.
     *
     * @param placement
     *            - The frame placement captured before applying the display settings, reproduced after
     *            re-initialization because the OS will have moved the existing frame during the reconfiguration
     */
    private void scheduleReInit(FramePlacement placement) {
        Timer reInitTimer = new Timer(REINIT_DELAY_MS, e -> appRefresher.reInitApp(placement));
        reInitTimer.setRepeats(false);
        reInitTimer.start();
    }

    /**
     * Leaves the "changing hot key" state for the specified slot's hot key.
     *
     * @param slotIndex
     *            - The slot index for the hot key that is leaving the "changing hot key" state
     */
    private void leaveChangingHotKeyState(int slotIndex) {
        currentKeyCount = 0;

        if (idleTimer != null) {
            idleTimer.stop();
            idleTimer = null;
        }

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

            FrameUtil.refreshFrame(view.getFrame());
        } else {
            view.getSlot(selectedDisplayIndex, slotIndex).getChangeHotKeyButton().setText(HOT_KEY_SET_TEXT);
        }

        model.getSlot(selectedDisplayIndex, slotIndex).getHotKey().setChangingHotKey(false);
        anyHotKeyChanging = false;

        settingsMgr.saveIniSlotHotKey(displayId, slotId, model.getSlot(selectedDisplayIndex, slotIndex).getHotKey());

        // Hot key definitions may have changed; rebuild the membership cache
        rebuildActiveKeyCodes();

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
                view.getSlot(displayIndex, slotIndex).getDisplayModes().setEnabled(false);
                view.getSlot(displayIndex, slotIndex).getScalingModes().setEnabled(false);
                view.getSlot(displayIndex, slotIndex).getDpiScalePercentages().setEnabled(false);
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
                view.getSlot(displayIndex, slotIndex).getDisplayModes().setEnabled(true);
                view.getSlot(displayIndex, slotIndex).getScalingModes().setEnabled(true);
                view.getSlot(displayIndex, slotIndex).getDpiScalePercentages().setEnabled(true);
                view.getSlot(displayIndex, slotIndex).getOrientationModes().setEnabled(true);
                view.getSlot(displayIndex, slotIndex).getChangeHotKeyButton().setEnabled(true);

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

}