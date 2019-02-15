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

package ai.bind.iot.client.control.wakeup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;

import ai.bind.iot.client.common.Constants;
import ai.bind.iot.client.common.util.AppManager;
import ai.bind.iot.client.common.util.JobServiceManager;
import ai.bind.iot.client.common.wakeup.ProcessAliveChecker;
import ai.bind.iot.client.control.ControlService;

/**
 * 用于保持Client应用进程活跃
 * Created by w5e.
 */
public final class ClientProcessAliveChecker extends ProcessAliveChecker {
    private static final boolean DEBUG = ControlService.DEBUG;
    private static final String TAG = ControlService.TAG;
    private static final String ACTION_ON_CHECK = "ai.bind.iot.client.LOCAL_ACTION_ON_CHECK";
    private OnCheckBroadcastReceiver mLocalOnCheckReceiver;
    private JobServiceManager mJobServiceManager;
    private boolean mIsKeepAlive = true;

    public ClientProcessAliveChecker(@NonNull Context context) {
        super(context,
                ACTION_CONTROL_REQUEST,
                ACTION_CONTROL_RESPONSE,
                ACTION_CLIENT_REQUEST,
                ACTION_CLIENT_RESPONSE);
        mLocalOnCheckReceiver = new OnCheckBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(ACTION_ON_CHECK);
        mContext.registerReceiver(mLocalOnCheckReceiver, intentFilter);
        mJobServiceManager = new JobServiceManager(mContext);
    }

    public void start() {
        setKeepAlive(true);
        WakeupJobService.scheduleJob(mContext, mJobServiceManager);
    }

    public void stop() {
        setKeepAlive(false);
        mJobServiceManager.cancel(WakeupJobService.JOB_ID_CHECKING);
    }

    @Override
    public void release() {
        super.release();
        mContext.unregisterReceiver(mLocalOnCheckReceiver);
    }

    private void setKeepAlive(boolean keepAlive) {
        mIsKeepAlive = keepAlive;
    }

    @Override
    protected void onResponse(boolean isAlive) {
        if (DEBUG) {
            Log.d(TAG, "Client is alive:" + isAlive);
        }
    }

    @Override
    protected void wakeup() {
        if (!mIsKeepAlive) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "Wakeup");
        }
        AppManager.startApp(mContext, Constants.CLIENT_PACKAGE_NAME);
    }

    static void onCheck(@NonNull Context context) {
        context.sendBroadcast(new Intent(ACTION_ON_CHECK));
    }

    private class OnCheckBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) {
                return;
            }
            if (!intent.getAction().equals(ACTION_ON_CHECK)) {
                return;
            }
            check();
            if (DEBUG) {
                Log.d(TAG, "Check");
            }
        }
    }

}
