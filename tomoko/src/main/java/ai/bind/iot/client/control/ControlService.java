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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import ai.bind.iot.client.common.Constants;
import ai.bind.iot.client.common.util.AppManager;
import ai.bind.iot.client.control.update.Pushable;
import ai.bind.iot.client.control.update.UpdateManager;
import ai.bind.iot.client.control.update.XinGePush;
import ai.bind.iot.client.control.wakeup.ClientProcessAliveChecker;

import java.util.Locale;

/**
 * 主服务
 * Created by w5e.
 */
public class ControlService extends Service {
    public static final boolean DEBUG = true;
    public static final String TAG = "Control";
    private Config mConfig;

    @Override
    public void onCreate() {
        super.onCreate();
        mConfig = new Config(this);
        mIsRebootEnabled = mConfig.isRebootEnabled();
        if (BuildConfig.PUSH_UPDATE_ENABLED) {
            initPushUpdate();
        }
        initProcessAliveKeeper();
        startForeground(NOTIFICATION_ID, createNotification());
        AppManager.runCommand("settings put global ntp_server ntp1.aliyun.com");
    }

    public static void start(@NonNull Context context) {
        Intent intent = new Intent(context, ControlService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private static final String ACTION_APP_RESTART = "1";
    private static final String ACTION_SYS_REBOOT = "2";

    private ControlBroadcastReceiver mControlBroadcastReceiver;
    private ClientProcessAliveChecker mProcessAliveChecker;
    private boolean mIsRebootEnabled = true;

    private void initProcessAliveKeeper() {
        mProcessAliveChecker = new ClientProcessAliveChecker(getApplicationContext());
        if (mControlBroadcastReceiver == null) {
            mControlBroadcastReceiver = new ControlBroadcastReceiver();
        }
        IntentFilter intentFilter = new IntentFilter(Constants.INTENT_ACTION_REBOOT);
        registerReceiver(mControlBroadcastReceiver, intentFilter);
        if (mConfig.isKeepAlive()) {
            mProcessAliveChecker.start();
        }
    }

    private void releaseProcessAliveKeeper() {
        if (mProcessAliveChecker != null) {
            mProcessAliveChecker.release();
        }
        if (mControlBroadcastReceiver != null) {
            unregisterReceiver(mControlBroadcastReceiver);
        }
    }

    private Pushable mPushable;
    private UpdateManager mUpdateManager;
    private String mPushToken;

    private void initPushUpdate() {
        mUpdateManager = new UpdateManager(this, new UpdateManager.EventListener() {
            @Override
            public void onStateChanged(int state, String msg) {
                onUpdateEvent(getStateString(state) + (msg == null ? "" : ":" + msg));
            }
        });

        mPushable = new XinGePush(this) {
            @Override
            public void onStart(String pushToken) {
                log("onStarted:" + pushToken);
                mPushToken = pushToken;
                onPushEvent(getString(R.string.event_push_state_start_success));
                onEvent(EVENT_TYPE_PUSH_TOKEN, pushToken);
            }

            @Override
            protected void executeAsync() {
                super.executeAsync();
                log("onStarting");
                onPushEvent(getString(R.string.event_push_state_starting));
            }

            @Override
            protected void onResult(boolean isSuccessful) {
                super.onResult(isSuccessful);
                if (!isSuccessful) {
                    log("onStartFailed");
                    onPushEvent(getString(R.string.event_push_state_start_failure));
                }
            }

            @Override
            public void onMessage(String msg) {
                if (msg == null) {
                    return;
                }
                onEvent(EVENT_TYPE_PUSH_MSG, msg);
                if (msg.startsWith("http")) {
                    String[] data = msg.split("\\|");
                    String url = data[0];
                    String packageName = data[2];//用于卸载旧应用
                    long versionCode = Long.parseLong(data[1]);
                    log("onMessage:" + "url=" + data[0] + ", vc=" + data[1]);
                    if (versionCode > AppManager.getVersionCode(
                            getApplicationContext(), packageName)) {
                        try {
                            mUpdateManager.update(url, packageName);
                        } catch (Exception e) {
                            e.printStackTrace();
                            onUpdateEvent(String.format(
                                    Locale.getDefault(),
                                    getString(R.string.update_failed), e));
                        }
                    } else {
                        String message = String.format(
                                Locale.getDefault(),
                                getString(R.string.update_unnecessary), versionCode);
                        log(message);
                        onUpdateEvent(message);
                    }
                } else {
                    log("onMessage:" + msg);
                    if (msg.contains("|")) {
                        String[] data = msg.split("\\|");
                        String action = data[0];
                        String packageName = data[1];
                        control(packageName, action);
                    } else {
                        control(null, msg);
                    }
                }
            }
        };
        mPushable.start();
    }

    private void releasePushUpdate() {
        if (mPushable != null) {
            mPushable.stop();
        }
        if (mUpdateManager != null) {
            mUpdateManager.release();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (BuildConfig.PUSH_UPDATE_ENABLED) {
            releasePushUpdate();
        }

        releaseProcessAliveKeeper();
        stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        if (DEBUG) {
            Log.d(TAG, "onStartCommand:" + ControlService.class.getSimpleName());
        }
        return START_STICKY;
    }

    public IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private void control(String packageName, String action) {
        log("PackageName:" + packageName + ", action:" + action);

        if (action.equals(ACTION_APP_RESTART)) {
            sendBroadcast(new Intent(Constants.INTENT_ACTION_RESTART));
        } else if (action.equals(ACTION_SYS_REBOOT)) {
            //收到推送立即执行
            reboot();
        }
    }

    private class ControlBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            if (action.equals(Constants.INTENT_ACTION_REBOOT)) {
                Toast.makeText(context, "REBOOT:" + mIsRebootEnabled, Toast.LENGTH_LONG).show();
                if (mIsRebootEnabled) {
                    reboot();
                }
            }
        }
    }

    private void reboot() {
        log("REBOOT");
        try {
            Runtime.getRuntime().exec("su -c reboot");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void log(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }

    private void onUpdateEvent(@NonNull String data) {
        onEvent(EVENT_TYPE_UPDATE_STATE, data);
    }

    private void onPushEvent(@NonNull String pushState) {
        onEvent(EVENT_TYPE_PUSH_STATE, pushState);
    }

    private void onEvent(int type, @NonNull String data) {
        Intent intent = new Intent(ACTION_CONTROL);
        intent.putExtra(EXTRA_TYPE, type);
        intent.putExtra(EXTRA_DATA, data);
        getApplicationContext().sendBroadcast(intent);
    }

    public static final String ACTION_CONTROL = "ai.bind.iot.client.control.ACTION";
    public static final String EXTRA_TYPE = "EXTRA_TYPE";
    public static final String EXTRA_DATA = "EXTRA_DATA";

    public static final int EVENT_TYPE_PUSH_STATE = 1;
    public static final int EVENT_TYPE_PUSH_TOKEN = 2;
    public static final int EVENT_TYPE_PUSH_MSG = 3;
    public static final int EVENT_TYPE_UPDATE_STATE = 4;

    public class LocalBinder extends Binder {
        public String getPushToken() {
            return ControlService.this.mPushToken;
        }

        public void setKeepAliveEnabled(boolean enabled) {
            if (enabled) {
                mProcessAliveChecker.start();
            } else {
                mProcessAliveChecker.stop();
            }
            mConfig.setKeepAlive(enabled);
        }

        public void setRebootEnabled(boolean enabled) {
            mIsRebootEnabled = enabled;
            mConfig.setRebootEnabled(mIsRebootEnabled);
        }
    }

    private String getStateString(int state) {
        int resId = 0;
        switch (state) {
            case UpdateManager.EVENT_UPDATE_STATE_DOWNLOADING:
                resId = R.string.event_update_state_downloading;
                break;
            case UpdateManager.EVENT_UPDATE_STATE_DOWNLOAD_FAILED:
                resId = R.string.event_update_state_download_failed;
                break;
            case UpdateManager.EVENT_UPDATE_STATE_DOWNLOAD_SUCCESS:
                resId = R.string.event_update_state_download_success;
                break;

            case UpdateManager.EVENT_UPDATE_STATE_UNINSTALLING:
                resId = R.string.event_update_state_uninstalling;
                break;
            case UpdateManager.EVENT_UPDATE_STATE_UNINSTALL_SUCCESS:
                resId = R.string.event_update_state_uninstall_success;
                break;
            case UpdateManager.EVENT_UPDATE_STATE_UNINSTALL_FAILED:
                resId = R.string.event_update_state_uninstall_failed;
                break;

            case UpdateManager.EVENT_UPDATE_STATE_INSTALLING:
                resId = R.string.event_update_state_installing;
                break;
            case UpdateManager.EVENT_UPDATE_STATE_INSTALL_SUCCESS:
                resId = R.string.event_update_state_install_success;
                break;
            case UpdateManager.EVENT_UPDATE_STATE_INSTALL_FAILED:
                resId = R.string.event_update_state_install_failed;
                break;

            case UpdateManager.EVENT_UPDATE_STATE_APP_STARING:
                resId = R.string.event_update_state_app_staring;
                break;
            default:
                break;
        }
        return getString(resId);
    }

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "AI_BIND_IOT_TOMOKO";
    private static final String CHANNEL_NAME = "TOMOKO";
    private NotificationChannel mChannel;

    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mChannel == null) {
                mChannel = new NotificationChannel(CHANNEL_ID,
                        CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                mChannel.setSound(null, null);
                getNotificationManager(this).createNotificationChannel(mChannel);
            }
        }
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID);

        builder.setSmallIcon(R.drawable.ic_stat_main)
                .setContentTitle("Tomoko Control Service")
                .setContentText("Running").setSound(null).setOnlyAlertOnce(true);

        return builder.build();
    }

    private static NotificationManager getNotificationManager(Context context) {
        if (context == null) {
            return null;
        }
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
