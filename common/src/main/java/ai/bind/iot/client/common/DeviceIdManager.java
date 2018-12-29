/*
 * Copyright (c) 2018 CELLA
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.bind.iot.client.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;

import ai.bind.iot.client.common.util.OSUtils;
import ai.bind.iot.client.common.util.PropertiesManager;
import androidx.annotation.NonNull;

/**
 * Created by w5e.
 */
public final class DeviceIdManager {
    private static final String KEY_DEVICE_ID = "KEY_DEVICE_ID";
    private static final String KEY_SERIAL_NO = "KEY_SERIAL_NO";
    private static final String SPS_NAME_CELLA = "ai_bind_iot_client";
    private File deviceFile = new File(Environment.getExternalStorageDirectory(), ".CELLA/device");
    private PropertiesManager propertiesManager = new PropertiesManager(deviceFile);

    public void setDeviceId(@NonNull Context context, @NonNull String deviceId) {
        SharedPreferences sps = context.getSharedPreferences(SPS_NAME_CELLA, Context.MODE_PRIVATE);
        sps.edit().putString(KEY_DEVICE_ID, deviceId).apply();
        propertiesManager.setProperty(KEY_DEVICE_ID, deviceId);
        propertiesManager.setProperty(KEY_SERIAL_NO, OSUtils.getSerialNo());
    }

    public String getDeviceId(@NonNull Context context) {
        SharedPreferences sps = context.getSharedPreferences(SPS_NAME_CELLA, Context.MODE_PRIVATE);
        String deviceId = sps.getString(KEY_DEVICE_ID, null);
        String serialNo = OSUtils.getSerialNo();
        if (TextUtils.isEmpty(deviceId)) {
            String oldSerialNo = propertiesManager.getProperty(KEY_SERIAL_NO);
            if (oldSerialNo != null && oldSerialNo.equals(serialNo))
                deviceId = propertiesManager.getProperty(KEY_DEVICE_ID);
        }
        if (TextUtils.isEmpty(deviceId)) return serialNo;
        return deviceId;
    }
}
