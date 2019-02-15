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

package ai.bind.iot.cloud;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import ai.bind.iot.client.common.DeviceIdManager;
import ai.bind.iot.cloud.request.BindRequestProcessor;
import ai.bind.iot.cloud.request.NotifyRequestProcessor;
import me.w5e.sdk.network.HttpClient;

/**
 * 远程服务
 * Created by w5e.
 */
public final class RemoteService {
    private static final boolean DEBUG = true;
    private static final String TAG = "RemoteService";

    private DeviceIdManager mIdManager = new DeviceIdManager();

    private HttpClient mHttpClient = new HttpClient() {
        @Override
        public void authenticate() {
        }
    };

    private BindRequestProcessor mBindRequestProcessor;
    private NotifyRequestProcessor mNotifyRequestProcessor;

    public void release() {
        if (mBindRequestProcessor != null) {
            mBindRequestProcessor.cancel();
            mBindRequestProcessor = null;
        }
        if (mNotifyRequestProcessor != null) {
            mNotifyRequestProcessor.cancel();
            mNotifyRequestProcessor = null;
        }
    }

    /**
     * 绑定推送设置
     *
     * @param pushToken   推送标识
     * @param versionCode 应用版本号
     */
    public void bindPushDevice(@NonNull Context context,
                               @NonNull String pushToken, long versionCode) {
        if (mBindRequestProcessor == null) {
            mBindRequestProcessor = new BindRequestProcessor(
                    mHttpClient, mIdManager.getDeviceId(context), pushToken, versionCode);
        }
        mBindRequestProcessor.executeAsync(new HttpClient.ResultListener<String>() {
            @Override
            public void onSuccess(String s) {
                if (DEBUG) {
                    Log.d(TAG, "绑定成功");
                }
            }

            @Override
            public void onFailure(String msg) {
                if (DEBUG) {
                    Log.d(TAG, "绑定失败：" + msg);
                }
            }
        });
    }

    /**
     * 通知应用启动，用于证明应用更新成功
     *
     * @param versionCode 应用版本号
     */
    public void notifyAppStartup(@NonNull Context context, long versionCode) {
        if (mNotifyRequestProcessor == null) {
            mNotifyRequestProcessor = new NotifyRequestProcessor(
                    mHttpClient, mIdManager.getDeviceId(context), versionCode);
        }
        mNotifyRequestProcessor.executeAsync(new HttpClient.ResultListener<String>() {
            @Override
            public void onSuccess(String s) {
                if (DEBUG) {
                    Log.d(TAG, "通知成功");
                }
            }

            @Override
            public void onFailure(String msg) {
                if (DEBUG) {
                    Log.d(TAG, "通知失败：" + msg);
                }
            }
        });
    }
}
