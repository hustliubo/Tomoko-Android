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

package ai.bind.iot.client.common.wakeup;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

/**
 * 两个进程都注册{@link WakeupBroadcastReceiver}，
 * Created by w5e.
 */
public abstract class ProcessAliveChecker {
    protected static final boolean DEBUG = false;
    protected static final String TAG = "ProcessAliveChecker";
    private boolean isChecking;
    private boolean isAlive;
    private boolean isTimeout;
    private Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    protected Context mContext;
    private static final long RESPONSE_TIMEOUT_INTERVAL = 2000;
    private WakeupBroadcastReceiver wakeupBroadcastReceiver;
    public ProcessAliveChecker(@NonNull Context context,
                               @NonNull String actionLocalRequest,
                               @NonNull String actionLocalResponse,
                               @NonNull String actionRemoteRequest,
                               @NonNull String actionRemoteResponse) {
        mContext = context.getApplicationContext();
        wakeupBroadcastReceiver =
                new WakeupBroadcastReceiver(actionLocalRequest, actionLocalResponse,
                        actionRemoteRequest, actionRemoteResponse) {
                    @Override
                    protected void onResponse() {
                        //如果未超时则说明存活
                        ProcessAliveChecker.this.onResponse(!isTimeout);
                        if (isTimeout) return;
                        isAlive = true;
                    }
                };
        wakeupBroadcastReceiver.register(mContext);
    }

    protected abstract void onResponse(boolean isAlive);

    private Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            //检测结束
            isChecking = false;
            //如果存活则不需要唤醒
            if (isAlive) {
                isAlive = false;
                return;
            }
            isTimeout = true;
            wakeup();
        }
    };

    public void check() {
        if (isChecking) return;
        isTimeout = false;
        isAlive = false;
        //检测开始
        isChecking = true;
        //发送广播并等待响应
        wakeupBroadcastReceiver.request(mContext);
        mainThreadHandler.postDelayed(timeoutRunnable, RESPONSE_TIMEOUT_INTERVAL);
    }

    public void release() {
        mainThreadHandler.removeCallbacks(timeoutRunnable);
        try {
            mContext.unregisterReceiver(wakeupBroadcastReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected abstract void wakeup();

    protected static final String ACTION_CONTROL_REQUEST = "ai.bind.iot.client.ACTION_REQUEST_CONTROL";
    protected static final String ACTION_CONTROL_RESPONSE = "ai.bind.iot.client.ACTION_RESPONSE_CONTROL";

    protected static final String ACTION_CLIENT_REQUEST = "ai.bind.iot.client.ACTION_REQUEST_CLIENT";
    protected static final String ACTION_CLIENT_RESPONSE = "ai.bind.iot.client.ACTION_RESPONSE_CLIENT";
}
