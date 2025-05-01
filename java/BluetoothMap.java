// Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
// All rights reserved.
// Confidential and Proprietary - Qualcomm Technologies, Inc with Commercial

package android.bluetooth;

import static android.bluetooth.BluetoothUtils.getSyncTimeout;

import android.Manifest;
import android.annotation.NonNull;
import android.annotation.RequiresNoPermission;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.AttributionSource;
import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.os.IpcDataCache;
import android.os.RemoteException;
import android.util.CloseGuard;
import android.util.Log;
import android.util.Pair;

import com.android.modules.utils.SynchronousResultReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * This class provides the APIs to control the Bluetooth MAP
 * Profile.
 *
 * @hide
 */
@SystemApi
public final class BluetoothMap implements BluetoothProfile, AutoCloseable {

    private static final String TAG = "BluetoothMap";
    private static final boolean DBG = true;
    private static final boolean VDBG = false;

    private CloseGuard mCloseGuard;

    /** @hide */
    @SuppressLint("ActionValue")
    @SystemApi
    @RequiresBluetoothConnectPermission
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CONNECTION_STATE_CHANGED =
            "android.bluetooth.map.profile.action.CONNECTION_STATE_CHANGED";

    /**
     * There was an error trying to obtain the state
     *
     * @hide
     */
    public static final int STATE_ERROR = -1;

    /** @hide */
    public static final int RESULT_FAILURE = 0;
    /** @hide */
    public static final int RESULT_SUCCESS = 1;
    /**
     * Connection canceled before completion.
     *
     * @hide
     */
    public static final int RESULT_CANCELED = 2;

    private final BluetoothAdapter mAdapter;
    private final AttributionSource mAttributionSource;
    private final BluetoothProfileConnector<IBluetoothMap> mProfileConnector =
            new BluetoothProfileConnector(this, BluetoothProfile.MAP,
                    "BluetoothMap", IBluetoothMap.class.getName()) {
                @Override
                public IBluetoothMap getServiceInterface(IBinder service) {
                    return IBluetoothMap.Stub.asInterface(service);
                }
    };
