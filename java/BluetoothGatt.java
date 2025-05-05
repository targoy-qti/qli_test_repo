/*
 * Copyright (C) 2013 Google Open Source Project
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
 * 
 */

// SPDX-License-Identifier: GPL-2.0-only

package android.bluetooth;

import static android.bluetooth.BluetoothUtils.getSyncTimeout;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.RequiresNoPermission;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic.WriteType;
import android.bluetooth.annotations.RequiresBluetoothConnectPermission;
import android.bluetooth.annotations.RequiresLegacyBluetoothPermission;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.AttributionSource;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import com.android.modules.utils.SynchronousResultReceiver;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Public API for the Bluetooth GATT Profile.
 *
 * <p>This class provides Bluetooth GATT functionality to enable communication
 * with Bluetooth Smart or Smart Ready devices.
 *
 * <p>To connect to a remote peripheral device, create a {@link BluetoothGattCallback}
 * and call {@link BluetoothDevice#connectGatt} to get a instance of this class.
 * GATT capable devices can be discovered using the Bluetooth device discovery or BLE
 * scan process.
 */
public final class BluetoothGatt implements BluetoothProfile {
    private static final String TAG = "BluetoothGatt";
    private static final boolean DBG = true;
    private static final boolean VDBG = false;

    @UnsupportedAppUsage
    private IBluetoothGatt mService;
    @UnsupportedAppUsage
    private volatile BluetoothGattCallback mCallback;
    private Handler mHandler;
    @UnsupportedAppUsage
    private int mClientIf;
    private BluetoothDevice mDevice;
    @UnsupportedAppUsage
    private boolean mAutoConnect;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    private int mAuthRetryState;
    private int mConnState;
    private final Object mStateLock = new Object();
    private final Object mDeviceBusyLock = new Object();
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private Boolean mDeviceBusy = false;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private int mTransport;
    private int mPhy;
    private boolean mOpportunistic;
    private final AttributionSource mAttributionSource;

    private static final int AUTH_RETRY_STATE_IDLE = 0;
    private static final int AUTH_RETRY_STATE_NO_MITM = 1;
    private static final int AUTH_RETRY_STATE_MITM = 2;

    private static final int CONN_STATE_IDLE = 0;
    private static final int CONN_STATE_CONNECTING = 1;
    private static final int CONN_STATE_CONNECTED = 2;
    private static final int CONN_STATE_DISCONNECTING = 3;
    private static final int CONN_STATE_CLOSED = 4;

    private static final int WRITE_CHARACTERISTIC_MAX_RETRIES = 5;
    private static final int WRITE_CHARACTERISTIC_TIME_TO_WAIT = 10; // milliseconds

    private List<BluetoothGattService> mServices;

    /** A GATT operation completed successfully */
    public static final int GATT_SUCCESS = 0;

    /** GATT read operation is not permitted */
    public static final int GATT_READ_NOT_PERMITTED = 0x2;

    /** GATT write operation is not permitted */
    public static final int GATT_WRITE_NOT_PERMITTED = 0x3;

    /** Insufficient authentication for a given operation */
    public static final int GATT_INSUFFICIENT_AUTHENTICATION = 0x5;

    /** The given request is not supported */
    public static final int GATT_REQUEST_NOT_SUPPORTED = 0x6;

    /** Insufficient encryption for a given operation */
    public static final int GATT_INSUFFICIENT_ENCRYPTION = 0xf;

    /** A read or write operation was requested with an invalid offset */
    public static final int GATT_INVALID_OFFSET = 0x7;

    /** Insufficient authorization for a given operation */
    public static final int GATT_INSUFFICIENT_AUTHORIZATION = 0x8;

    /** A write operation exceeds the maximum length of the attribute */
    public static final int GATT_INVALID_ATTRIBUTE_LENGTH = 0xd;

    /** A remote device connection is congested. */
    public static final int GATT_CONNECTION_CONGESTED = 0x8f;

    /** A GATT operation failed, errors other than the above */
    public static final int GATT_FAILURE = 0x101;

    /**
     * Connection parameter update - Use the connection parameters recommended by the
     * Bluetooth SIG. This is the default value if no connection parameter update
     * is requested.
     */
    public static final int CONNECTION_PRIORITY_BALANCED = 0;

    /**
     * Connection parameter update - Request a high priority, low latency connection.
     * An application should only request high priority connection parameters to transfer large
     * amounts of data over LE quickly. Once the transfer is complete, the application should
     * request {@link BluetoothGatt#CONNECTION_PRIORITY_BALANCED} connection parameters to reduce
     * energy use.
     */
    public static final int CONNECTION_PRIORITY_HIGH = 1;

    /** Connection parameter update - Request low power, reduced data rate connection parameters. */
    public static final int CONNECTION_PRIORITY_LOW_POWER = 2;
}