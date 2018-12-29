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

package ai.bind.iot.client.control.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ai.bind.iot.client.common.util.AppManager;
import ai.bind.iot.client.control.ControlService;
import androidx.annotation.NonNull;
import me.w5e.sdk.network.HttpClient;
import me.w5e.sdk.network.request.DownloadFileProcessor;

/**
 * 升级管理器
 * Created by w5e.
 */
public class UpdateManager {
    private static final boolean DEBUG = ControlService.DEBUG;
    private static final String TAG = ControlService.TAG;
    private HttpClient httpClient;
    private File apk;
    private Context mContext;
    private PackageStateReceiver packageStateReceiver;
    private EventListener eventListener;
    private static final int TASK_COUNT = 2;//安装前需要执行的任务数，安装前需要执行下载和卸载
    private int currentTaskCount;//当前已经执行的任务数
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public UpdateManager(@NonNull Context context, @NonNull EventListener l) {
        mContext = context.getApplicationContext();
        eventListener = l;
        apk = new File(Environment.getExternalStorageDirectory(),
                "/cella/client/update/last.apk");
        packageStateReceiver = new PackageStateReceiver();
    }

    public void release() {
        if (packageStateReceiver != null)
            packageStateReceiver.unregister(mContext);
    }

    /**
     * 升级应用
     *
     * @param url         APK下载路径
     * @param packageName 应用包名，安装前会先根据这个包名来卸载应用
     * @throws IllegalArgumentException url解析失败会抛出这个异常
     */
    public void update(@NonNull String url, @NonNull String packageName) throws IllegalArgumentException {
        if (httpClient == null) {
            httpClient = new HttpClient() {
                @Override
                public void authenticate() {
                }
            };
        }
        packageStateReceiver.register(mContext);
        currentTaskCount = TASK_COUNT;
        uninstallApp(packageName);
        deleteApk();
        eventListener.onStateChanged(EVENT_UPDATE_STATE_DOWNLOADING, null);
        new ApkDownloadProcessor(httpClient, url, apk)
                .executeAsync(new HttpClient.ResultListener<File>() {
                    @Override
                    public void onSuccess(File file) {
                        eventListener.onStateChanged(EVENT_UPDATE_STATE_DOWNLOAD_SUCCESS, null);
                        installApp();
                    }

                    @Override
                    public void onFailure(String msg) {
                        log("下载失败");
                        eventListener.onStateChanged(EVENT_UPDATE_STATE_DOWNLOAD_FAILED, null);
                    }
                });
    }

    private void deleteApk() {
        if (apk != null && apk.exists()) {
            if (apk.delete()) {
                log("删除旧APK成功");
            } else {
                log("删除旧APK失败");
            }
        }
    }

    private void uninstallApp(@NonNull final String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                || !AppManager.isAppInstalled(mContext, packageName)) {
            currentTaskCount -= 1;
            return;
        }
        //静默卸载
        eventListener.onStateChanged(EVENT_UPDATE_STATE_UNINSTALLING, null);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                final String errorMsg = AppManager.uninstall(packageName);
                mainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (errorMsg != null)
                            eventListener.onStateChanged(EVENT_UPDATE_STATE_UNINSTALL_FAILED, errorMsg);
                        else
                            doAfterUninstallSuccess();
                    }
                });

            }
        });

    }

    private synchronized void installApp() {
        log("开始安装应用" + currentTaskCount + apk.exists());
        if (currentTaskCount > 1) {
            currentTaskCount -= 1;
            return;
        }
        //静默安装
        eventListener.onStateChanged(EVENT_UPDATE_STATE_INSTALLING, null);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                final String errorMsg = AppManager.install(apk.getAbsolutePath());
                mainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (errorMsg != null)
                            eventListener.onStateChanged(EVENT_UPDATE_STATE_INSTALL_FAILED, errorMsg);
                    }
                });
            }
        });
    }

    private void doAfterInstallSuccess(@NonNull String packageName) {
        //安装后删除文件
        deleteApk();
        eventListener.onStateChanged(EVENT_UPDATE_STATE_INSTALL_SUCCESS, null);
        //安装完成后移除监听
        packageStateReceiver.unregister(mContext);
        String activityName = AppManager.startApp(mContext, packageName);
        eventListener.onStateChanged(EVENT_UPDATE_STATE_APP_STARING, packageName + " " + activityName);
    }

    private void doAfterUninstallSuccess() {
        //卸载后安装新应用
        eventListener.onStateChanged(EVENT_UPDATE_STATE_UNINSTALL_SUCCESS, null);
        installApp();
    }

    /**
     * 包状态广播接收者，对包的安装或卸载事件进行监听。
     */
    public class PackageStateReceiver extends BroadcastReceiver {
        public void register(Context context) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            filter.addDataScheme("package");
            context.registerReceiver(this, filter);
        }

        public void unregister(Context context) {
            try {
                context.unregisterReceiver(this);
            } catch (Exception ignore) {
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String actionName = intent.getAction();
            if (actionName == null) return;

            Uri data = intent.getData();
            if (data == null) return;
            String packageName = data.getSchemeSpecificPart();

            if (packageName == null) return;
            if (actionName.equals(Intent.ACTION_PACKAGE_ADDED)) {
                long versionCode = AppManager.getVersionCode(context, packageName);
                if (DEBUG)
                    Log.d(TAG, String.format(Locale.getDefault(),
                            "安装应用 包名:%s, 版本号:%d", packageName, versionCode));
                doAfterInstallSuccess(packageName);
            }
//            if (actionName.equals(Intent.ACTION_PACKAGE_REMOVED)) {
//                doAfterUninstallSuccess();
//            }
        }
    }

    private void log(String msg) {
        if (ControlService.DEBUG)
            Log.d(ControlService.TAG, msg);
    }

    /**
     * 用于与ControlService解藕
     */
    public interface EventListener {
        void onStateChanged(int state, String msg);
    }

    public static final int EVENT_UPDATE_STATE_DOWNLOADING = 0;
    public static final int EVENT_UPDATE_STATE_DOWNLOAD_SUCCESS = 1;
    public static final int EVENT_UPDATE_STATE_DOWNLOAD_FAILED = 2;
    public static final int EVENT_UPDATE_STATE_UNINSTALLING = 3;
    public static final int EVENT_UPDATE_STATE_UNINSTALL_SUCCESS = 4;
    public static final int EVENT_UPDATE_STATE_UNINSTALL_FAILED = 5;
    public static final int EVENT_UPDATE_STATE_INSTALLING = 6;
    public static final int EVENT_UPDATE_STATE_INSTALL_SUCCESS = 7;
    public static final int EVENT_UPDATE_STATE_INSTALL_FAILED = 8;
    public static final int EVENT_UPDATE_STATE_APP_STARING = 9;

    private class ApkDownloadProcessor extends DownloadFileProcessor {
        /**
         * @param httpClient 端
         * @param remotePath 远程下载路径
         * @param localFile  本地文件
         */
        ApkDownloadProcessor(@NonNull HttpClient httpClient,
                             @NonNull String remotePath, @NonNull File localFile) {
            super(httpClient, remotePath, localFile, false, null);
        }
    }
}
