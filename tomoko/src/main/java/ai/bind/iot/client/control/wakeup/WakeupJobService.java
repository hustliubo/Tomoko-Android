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

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import ai.bind.iot.client.common.util.JobServiceManager;
import ai.bind.iot.client.common.wakeup.WakeupBroadcastReceiver;
import ai.bind.iot.client.control.ControlService;
import androidx.annotation.NonNull;

/**
 * Created by w5e.
 */
public class WakeupJobService extends JobService {
    private static final boolean DEBUG = ControlService.DEBUG;
    public static final String TAG = WakeupBroadcastReceiver.TAG;
    public static final int JOB_ID_CHECKING = 1;
    private static final int MILLISECONDS_INTERVAL_CHECKING = 10000;
    private JobServiceManager jobServiceManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG)
            Log.d(TAG, "onStartCommand:" + WakeupJobService.class.getSimpleName());
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        jobServiceManager = new JobServiceManager(getApplicationContext());
        if (DEBUG)
            Log.d(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG)
            Log.d(TAG, "onDestroy");
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        //当Control服务停止时唤醒
        ControlService.start(this);
        ClientProcessAliveChecker.onCheck(getApplicationContext());
        if (DEBUG)
            Log.d(TAG, "onStartJob");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            jobFinished(params, true);
            scheduleJob(getApplicationContext(), jobServiceManager);
            return true;
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (DEBUG)
            Log.d(TAG, "onStopJob");
        return false;
    }

    static void scheduleJob(@NonNull Context context,
                            @NonNull JobServiceManager jobServiceManager) {
        ComponentName componentName = new ComponentName(context,
                WakeupJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID_CHECKING, componentName);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        builder.setPersisted(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setMinimumLatency(MILLISECONDS_INTERVAL_CHECKING);
            builder.setOverrideDeadline(MILLISECONDS_INTERVAL_CHECKING);
            builder.setMinimumLatency(MILLISECONDS_INTERVAL_CHECKING);
            builder.setBackoffCriteria(MILLISECONDS_INTERVAL_CHECKING, JobInfo.BACKOFF_POLICY_LINEAR);
        } else {
            builder.setPeriodic(MILLISECONDS_INTERVAL_CHECKING);
        }
        boolean result = jobServiceManager.scheduleJob(builder.build());
        if (DEBUG) Log.d(TAG, "ScheduleJob:" + result);
    }

}
