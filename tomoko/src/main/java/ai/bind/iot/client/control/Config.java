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

package ai.bind.iot.client.control;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * 功能配置
 * Created by w5e.
 */
public final class Config {
    private SharedPreferences sps;
    private static final String SPS_NAME = "ai.bind.iot.client.control.config";
    private static final String KEY_KEEP_ALIVE = "KEY_KEEP_ALIVE";
    private static final String KEY_REBOOT_ENABLED = "KEY_REBOOT_ENABLED";

    public Config(@NonNull Context context) {
        sps = context.getSharedPreferences(SPS_NAME, Context.MODE_PRIVATE);
    }

    void setKeepAlive(boolean keepAlive) {
        sps.edit().putBoolean(KEY_KEEP_ALIVE, keepAlive).apply();
    }

    void setRebootEnabled(boolean rebootEnabled) {
        sps.edit().putBoolean(KEY_REBOOT_ENABLED, rebootEnabled).apply();
    }

    public boolean isKeepAlive() {
        return sps.getBoolean(KEY_KEEP_ALIVE, true);
    }

    public boolean isRebootEnabled() {
        return sps.getBoolean(KEY_REBOOT_ENABLED, true);
    }
}
