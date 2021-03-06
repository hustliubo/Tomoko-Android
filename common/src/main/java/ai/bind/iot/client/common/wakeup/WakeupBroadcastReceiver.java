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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Created by w5e.
 */
public abstract class WakeupBroadcastReceiver extends BroadcastReceiver {
    private static final boolean DEBUG = true;
    public static final String TAG = "wakeup";

    protected String mActionLocalRequest;
    protected String mActionLocalResponse;
    protected String mActionRemoteRequest;
    protected String mActionRemoteResponse;

    public WakeupBroadcastReceiver(
            @NonNull String actionLocalRequest,
            @NonNull String actionLocalResponse,
            @NonNull String actionRemoteRequest,
            @NonNull String actionRemoteResponse) {
        mActionLocalRequest = actionLocalRequest;
        mActionLocalResponse = actionLocalResponse;
        mActionRemoteRequest = actionRemoteRequest;
        mActionRemoteResponse = actionRemoteResponse;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG) {
            Log.d(TAG, intent.toString());
        }
        if (intent.getAction() == null) {
            return;
        }
        if (intent.getAction().equals(mActionLocalRequest)) {
            response(context);
        }
        if (intent.getAction().equals(mActionLocalResponse)) {
            onResponse();
        }
    }

    protected abstract void onResponse();

    /**
     * 注册等待响应
     */
    public void register(@NonNull Context context) {
        IntentFilter intentFilter = new IntentFilter(mActionLocalRequest);
        intentFilter.addAction(mActionLocalResponse);
        context.registerReceiver(this, intentFilter);
    }

    public void request(@NonNull Context context) {
        context.sendBroadcast(new Intent(mActionRemoteRequest));
    }

    private void response(@NonNull Context context) {
        context.sendBroadcast(new Intent(mActionRemoteResponse));
    }
}
