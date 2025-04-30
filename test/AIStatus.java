/*
 * Copyright 2021 HIMSA II K/S - www.himsa.com.
 * Represented by EHIMA - www.ehima.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 # SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.android.bluetooth.leaudio;

import android.app.Application;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class BluetoothProxy {
    private static BluetoothProxy INSTANCE;
    private final Application application;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAudio bluetoothLeAudio = null;
    private BluetoothLeBroadcast mBluetoothLeBroadcast = null;
    private BluetoothLeBroadcastAssistant mBluetoothLeBroadcastAssistant = null;
    private Set<BluetoothDevice> mBroadcastScanDelegatorDevices = new HashSet<>();
    private BluetoothCsipSetCoordinator bluetoothCsis = null;
    private BluetoothVolumeControl bluetoothVolumeControl = null;
    private BluetoothHapClient bluetoothHapClient = null;
    private BluetoothProfile.ServiceListener profileListener = null;
    private BluetoothHapClient.Callback hapCallback = null;
    private OnBassEventListener mBassEventListener;
    private OnLocalBroadcastEventListener mLocalBroadcastEventListener;
    private final IntentFilter adapterIntentFilter;
    private final IntentFilter bassIntentFilter;
    private IntentFilter intentFilter;
    private final ExecutorService mExecutor;

    private final Map<Integer, UUID> mGroupLocks = new HashMap<>();

    private int GROUP_NODE_ADDED = 1;
    private int GROUP_NODE_REMOVED = 2;

    private boolean mLeAudioCallbackRegistered = false;
    private BluetoothLeAudio.Callback mLeAudioCallbacks =
    new BluetoothLeAudio.Callback() {
        @Override
        public void onCodecConfigChanged(int groupId, BluetoothLeAudioCodecStatus status) {}
        @Override
        public void onGroupStatusChanged(int groupId, int groupStatus) {
            List<LeAudioDeviceStateWrapper> valid_devices = null;
            valid_devices = allLeAudioDevicesMutable.getValue().stream().filter(
                                state -> state.leAudioData.nodeStatusMutable.getValue() != null
                                        && state.leAudioData.nodeStatusMutable.getValue().first
                                                .equals(groupId))
                                .collect(Collectors.toList());
            for (LeAudioDeviceStateWrapper dev : valid_devices) {
                dev.leAudioData.groupStatusMutable.postValue(
                        new Pair<>(groupId, new Pair<>(groupStatus, 0)));
            }
        }
        @Override
        public void onGroupNodeAdded(BluetoothDevice device, int groupId) {
            Log.d("LeCB:", device.getAddress() + " group added " + groupId);
            if (device == null || groupId == BluetoothLeAudio.GROUP_ID_INVALID) {
                Log.d("LeCB:", "invalid parameter");
                return;
            }
            Optional<LeAudioDeviceStateWrapper> valid_device_opt = allLeAudioDevicesMutable
                            .getValue().stream()
                            .filter(state -> state.device.getAddress().equals(device.getAddress()))
                            .findAny();

            if (!valid_device_opt.isPresent()) {
                Log.d("LeCB:", "Device not present");
                return;
            }

            LeAudioDeviceStateWrapper valid_device = valid_device_opt.get();
            LeAudioDeviceStateWrapper.LeAudioData svc_data = valid_device.leAudioData;

            svc_data.nodeStatusMutable.postValue(new Pair<>(groupId, GROUP_NODE_ADDED));
            svc_data.groupStatusMutable.postValue(new Pair<>(groupId, new Pair<>(-1, -1)));
        }
        @Override
        public void onGroupNodeRemoved(BluetoothDevice device, int groupId) {
            if (device == null || groupId == BluetoothLeAudio.GROUP_ID_INVALID) {
                Log.d("LeCB:", "invalid parameter");
                return;
            }

            Log.d("LeCB:", device.getAddress() + " group added " + groupId);
            if (device == null || groupId == BluetoothLeAudio.GROUP_ID_INVALID) {
                Log.d("LeCB:", "invalid parameter");
                return;
            }

            Optional<LeAudioDeviceStateWrapper> valid_device_opt = allLeAudioDevicesMutable
            .getValue().stream()
            .filter(state -> state.device.getAddress().equals(device.getAddress()))
            .findAny();

            if (!valid_device_opt.isPresent()) {
                Log.d("LeCB:", "Device not present");
                return;
            }

            LeAudioDeviceStateWrapper valid_device = valid_device_opt.get();
            LeAudioDeviceStateWrapper.LeAudioData svc_data = valid_device.leAudioData;

            svc_data.nodeStatusMutable.postValue(new Pair<>(groupId, GROUP_NODE_REMOVED));
            svc_data.groupStatusMutable.postValue(new Pair<>(groupId, new Pair<>(-1, -1)));
        }
    };
